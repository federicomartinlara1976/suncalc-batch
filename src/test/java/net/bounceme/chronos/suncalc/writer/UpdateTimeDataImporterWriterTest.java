package net.bounceme.chronos.suncalc.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;
import org.springframework.test.util.ReflectionTestUtils;

import net.bounceme.chronos.suncalc.model.Registro;
import net.bounceme.chronos.suncalc.model.TimeData;
import net.bounceme.chronos.suncalc.repository.RepositoryCollectionCustom;
import net.bounceme.chronos.suncalc.repository.TimeDataRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateTimeDataImporterWriter - guarda TimeData asignando id si falta")
class UpdateTimeDataImporterWriterTest {

    private static final String COLLECTION = "suncalc-collection";
    private static final String FORMATTED_DATE = "2024-06-15";

    @Mock private TimeDataRepository timeDataRepository;
    @Mock private RepositoryCollectionCustom repositoryCollectionCustom;
    @Mock private SimpleDateFormat dateFormat;

    @InjectMocks
    private UpdateTimeDataImporterWriter writer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(writer, "collection", COLLECTION);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Crea un TimeData con id y fecha controlados.
     * Si id == null, el SUT debería rellenarlo con dateFormat.format(fecha).
     */
    private TimeData timeData(String id, Date fecha) {
        TimeData td = new TimeData();
        td.setId(id);
        td.setFecha(fecha);
        return td;
    }

    // ==================================================================
    // Chunk vacío
    // ==================================================================

    @Nested
    @DisplayName("Chunk vacío")
    class ChunkVacioTests {

        @Test
        @DisplayName("No itera → no toca repositorios ni dateFormat")
        void doWrite_chunkVacio_sinInteracciones() throws Exception {
            // Arrange
            Chunk<Registro<TimeData>> chunk = Chunk.of();

            // Act
            writer.write(chunk);

            // Assert
            verifyNoInteractions(timeDataRepository, repositoryCollectionCustom, dateFormat);
        }
    }

    // ==================================================================
    // Item con id null → asignar id
    // ==================================================================

    @Nested
    @DisplayName("Item con id null")
    class ItemSinIdTests {

        @Test
        @DisplayName("Asigna id = dateFormat.format(fecha) y guarda")
        void doWrite_itemSinId_asignaIdYGuarda() throws Exception {
            // Arrange
            Date fecha = new Date();
            TimeData timeData = timeData(null, fecha);
            Registro<TimeData> item = new Registro<>(timeData);
            when(dateFormat.format(fecha)).thenReturn(FORMATTED_DATE);

            ArgumentCaptor<TimeData> captor = ArgumentCaptor.forClass(TimeData.class);

            // Act
            writer.write(Chunk.of(item));

            // Assert
            assertThat(timeData.getId()).isEqualTo(FORMATTED_DATE);

            verify(timeDataRepository).save(captor.capture());
            assertThat(captor.getValue()).isSameAs(timeData);

            verify(dateFormat).format(fecha);
            verify(repositoryCollectionCustom, times(1)).setCollectionName(COLLECTION);
        }
    }

    // ==================================================================
    // Item con id no null → no se toca
    // ==================================================================

    @Nested
    @DisplayName("Item con id no null")
    class ItemConIdTests {

        @Test
        @DisplayName("No llama a dateFormat ni modifica el id, pero guarda")
        void doWrite_itemConId_noModificaIdPeroGuarda() throws Exception {
            // Arrange
            TimeData timeData = timeData("id-existente", new Date());
            Registro<TimeData> item = new Registro<>(timeData);

            // Act
            writer.write(Chunk.of(item));

            // Assert
            assertThat(timeData.getId()).isEqualTo("id-existente");
            verify(dateFormat, never()).format(any(Date.class));
            verify(timeDataRepository).save(timeData);
            verify(repositoryCollectionCustom).setCollectionName(COLLECTION);
        }
    }

    // ==================================================================
    // Varios items: mezcla de ids
    // ==================================================================

    @Nested
    @DisplayName("Varios items")
    class VariosItemsTests {

        @Test
        @DisplayName("Mezcla de items con y sin id: solo se formatea para los que no tienen id")
        void doWrite_mezcla_formateaSoloLosSinId() throws Exception {
            // Arrange
            Date f1 = new Date(1000L);
            Date f2 = new Date(2000L);
            Date f3 = new Date(3000L);

            TimeData tsinId1 = timeData(null, f1);
            Registro<TimeData> sinId1 = new Registro<>(tsinId1);
            TimeData tconId  = timeData("ya-tiene-id", f2);
            Registro<TimeData> conId = new Registro<>(tconId);
            TimeData tsinId2 = timeData(null, f3);
            Registro<TimeData> sinId2 = new Registro<>(tsinId2);

            when(dateFormat.format(f1)).thenReturn("2024-01-01");
            when(dateFormat.format(f3)).thenReturn("2024-01-03");

            // Act
            writer.write(Chunk.of(sinId1, conId, sinId2));

            // Assert
            assertThat(tsinId1.getId()).isEqualTo("2024-01-01");
            assertThat(tconId.getId()).isEqualTo("ya-tiene-id");
            assertThat(tsinId2.getId()).isEqualTo("2024-01-03");

            // dateFormat: 2 invocaciones (solo los sin id)
            verify(dateFormat, times(2)).format(any(Date.class));
            verify(dateFormat, never()).format(f2);

            // save: 3 invocaciones
            verify(timeDataRepository, times(3)).save(any(TimeData.class));

            // setCollectionName: 1 por item, no 1 por chunk
            verify(repositoryCollectionCustom, times(3)).setCollectionName(COLLECTION);
        }

        @Test
        @DisplayName("save se invoca con cada item del chunk, en orden")
        void doWrite_savePorCadaItem_enOrden() throws Exception {
            // Arrange
            TimeData ta = timeData("a", new Date());
            Registro<TimeData> a = new Registro<>(ta);
            TimeData tb = timeData("b", new Date());
            Registro<TimeData> b = new Registro<>(tb);
            TimeData tc = timeData("c", new Date());
            Registro<TimeData> c = new Registro<>(tc);

            ArgumentCaptor<TimeData> captor = ArgumentCaptor.forClass(TimeData.class);

            // Act
            writer.write(Chunk.of(a, b, c));

            // Assert
            verify(timeDataRepository, times(3)).save(captor.capture());
            assertThat(captor.getAllValues()).containsExactly(ta, tb, tc);
        }
    }

    // ==================================================================
    // Efectos de "una llamada por item" en setCollectionName
    // ==================================================================

    @Nested
    @DisplayName("setCollectionName se invoca por item (no por chunk)")
    class SetCollectionNameTests {

        @Test
        @DisplayName("Con 1 item → 1 sola llamada a setCollectionName")
        void doWrite_unItem_unaSetCollectionName() throws Exception {
            // Act
            writer.write(Chunk.of(new Registro<>(timeData("1", new Date()))));

            // Assert
            verify(repositoryCollectionCustom, times(1)).setCollectionName(COLLECTION);
        }
    }

    // ==================================================================
    // Propagación de excepciones (@SneakyThrows)
    // ==================================================================

    @Nested
    @DisplayName("Propagación de excepciones")
    class ExcepcionesTests {

        @Test
        @DisplayName("Si timeDataRepository.save lanza RuntimeException, se propaga sin envolver")
        void doWrite_saveLanzaRuntime_propaga() {
            // Arrange
            TimeData timeData = timeData("x", new Date());
            Registro<TimeData> item = new Registro<>(timeData);
            RuntimeException boom = new IllegalStateException("fallo de save");
            when(timeDataRepository.save(timeData)).thenThrow(boom);

            // Act / Assert
            try {
                writer.write(Chunk.of(item));
                throw new AssertionError("Se esperaba la excepción");
            } catch (Exception e) {
                assertThat(e).isSameAs(boom);
            }
        }

        @Test
        @DisplayName("Si un item intermedio lanza, los anteriores ya se han guardado")
        void doWrite_falloEnSegundo_elPrimeroYaSeGuardo() throws Exception {
            // Arrange
        	TimeData ta = timeData("a", new Date());
            Registro<TimeData> a = new Registro<>(ta);
            TimeData tb = timeData("b", new Date());
            Registro<TimeData> b = new Registro<>(tb);
            TimeData tc = timeData("c", new Date());
            Registro<TimeData> c = new Registro<>(tc);

            RuntimeException boom = new IllegalStateException("b falla");
            lenient().when(timeDataRepository.save(tb)).thenThrow(boom);

            // Act
            try {
                writer.write(Chunk.of(a, b, c));
                throw new AssertionError("Se esperaba la excepción");
            } catch (Exception e) {
                assertThat(e).isSameAs(boom);
            }

            // Assert
            verify(timeDataRepository).save(ta);
            verify(timeDataRepository).save(tb);
            verify(timeDataRepository, never()).save(tc);
            verify(repositoryCollectionCustom, times(2)).setCollectionName(COLLECTION);
        }
    }
}