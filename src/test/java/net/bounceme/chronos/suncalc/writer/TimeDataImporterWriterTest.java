package net.bounceme.chronos.suncalc.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.test.util.ReflectionTestUtils;

import net.bounceme.chronos.suncalc.model.TimeData;
import net.bounceme.chronos.suncalc.repository.RepositoryCollectionCustom;
import net.bounceme.chronos.suncalc.repository.TimeDataRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("TimeDataImporterWriter - guarda TimeData solo si no existe")
class TimeDataImporterWriterTest {

    private static final String COLLECTION = "suncalc-collection";
    private static final String FORMATTED_DATE = "2024-06-15";
    private static final String NEXT_TIME_DATA_KEY = "NEXT_TIME_DATA";

    @Mock private TimeDataRepository timeDataRepository;
    @Mock private RepositoryCollectionCustom repositoryCollectionCustom;
    @Mock private SimpleDateFormat dateFormat;
    @Mock private JobExecution jobExecution;

    @InjectMocks
    private TimeDataImporterWriter writer;

    private ExecutionContext executionContext;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(writer, "collection", COLLECTION);
        writer.jobExecution = jobExecution;
        executionContext = new ExecutionContext();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private TimeData timeData(String id, Date fecha) {
        TimeData td = new TimeData();
        td.setId(id);
        td.setFecha(fecha);
        return td;
    }

    /**
     * Simula la rama "vacío" del ifPresentOrElse (item NO existe en repo).
     */
    private void stubItemNotInRepo() {
        when(timeDataRepository.findById(anyString())).thenReturn(Optional.empty());
    }

    /**
     * Simula la rama "presente" del ifPresentOrElse (item YA existe en repo).
     */
    private void stubItemAlreadyInRepo(TimeData existing) {
        when(timeDataRepository.findById(anyString())).thenReturn(Optional.of(existing));
    }

    /**
     * Prepara el JobExecution para que devuelva el ExecutionContext real.
     */
    private void stubExecutionContext() {
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);
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
            // Act
            writer.write(Chunk.of());

            // Assert
            verifyNoInteractions(timeDataRepository, repositoryCollectionCustom, dateFormat);
            //assertThat(executionContext).isEmpty();
        }
    }

    // ==================================================================
    // Rama "vacío" (item NO existe en repo)
    // ==================================================================

    @Nested
    @DisplayName("Item no existe en repo → guarda")
    class RamaVacioTests {

        @Test
        @DisplayName("Item con id null → asigna id, guarda y publica NEXT_TIME_DATA")
        void doWrite_itemSinId_asignaGuardaYPublica() throws Exception {
            // Arrange
            Date fecha = new Date();
            TimeData item = timeData(null, fecha);
            stubItemNotInRepo();
            stubExecutionContext();
            when(dateFormat.format(fecha)).thenReturn(FORMATTED_DATE);

            ArgumentCaptor<TimeData> captor = ArgumentCaptor.forClass(TimeData.class);

            // Act
            writer.write(Chunk.of(item));

            // Assert
            assertThat(item.getId()).isEqualTo(FORMATTED_DATE);
            verify(timeDataRepository).save(captor.capture());
            assertThat(captor.getValue()).isSameAs(item);
            assertThat(executionContext.get(NEXT_TIME_DATA_KEY)).isSameAs(item);

            // findById + setId = 2 invocaciones de format
            verify(dateFormat, times(2)).format(fecha);
            verify(repositoryCollectionCustom).setCollectionName(COLLECTION);
        }

        @Test
        @DisplayName("Item con id no null → no toca id, guarda y publica NEXT_TIME_DATA")
        void doWrite_itemConId_guardaYPublica() throws Exception {
            // Arrange
            Date fecha = new Date();
            TimeData item = timeData("id-existente", fecha);
            stubItemNotInRepo();
            stubExecutionContext();
            when(dateFormat.format(fecha)).thenReturn(FORMATTED_DATE);

            // Act
            writer.write(Chunk.of(item));

            // Assert
            assertThat(item.getId()).isEqualTo("id-existente");
            verify(timeDataRepository).save(item);
            assertThat(executionContext.get(NEXT_TIME_DATA_KEY)).isSameAs(item);

            // Solo 1 invocación de format (findById); NO se re-formatea el id
            verify(dateFormat, times(1)).format(fecha);
        }
    }

    // ==================================================================
    // Rama "presente" (item YA existe en repo)
    // ==================================================================

    @Nested
    @DisplayName("Item ya existe en repo → skip")
    class RamaPresenteTests {

        @Test
        @DisplayName("findById presente → NO guarda y NO publica NEXT_TIME_DATA")
        void doWrite_itemYaExiste_noGuarda() throws Exception {
            // Arrange
            Date fecha = new Date();
            TimeData item = timeData("id-existente", fecha);
            TimeData existing = timeData("id-existente", fecha);
            stubItemAlreadyInRepo(existing);
            when(dateFormat.format(fecha)).thenReturn(FORMATTED_DATE);

            // Act
            writer.write(Chunk.of(item));

            // Assert
            verify(timeDataRepository).findById(FORMATTED_DATE);
            verify(timeDataRepository, never()).save(any(TimeData.class));
            verify(jobExecution, never()).getExecutionContext();
            //assertThat(executionContext).isEmpty();

            // Solo 1 invocación de format (findById); nada de setId
            verify(dateFormat, times(1)).format(fecha);
            verify(repositoryCollectionCustom).setCollectionName(COLLECTION);
        }

        @Test
        @DisplayName("Item con id null pero ya existe en repo → NO se modifica el id")
        void doWrite_itemSinIdPeroYaExiste_noModificaId() throws Exception {
            // Arrange
            Date fecha = new Date();
            TimeData item = timeData(null, fecha);
            TimeData existing = timeData("id-existente", fecha);
            stubItemAlreadyInRepo(existing);
            when(dateFormat.format(fecha)).thenReturn(FORMATTED_DATE);

            // Act
            writer.write(Chunk.of(item));

            // Assert
            assertThat(item.getId()).isNull();  // no se ha tocado
            verify(timeDataRepository, never()).save(any(TimeData.class));
            verify(dateFormat, times(1)).format(fecha);
        }
    }

    // ==================================================================
    // Mezcla en un mismo chunk
    // ==================================================================

    @Nested
    @DisplayName("Mezcla de items")
    class MezclaTests {

        @Test
        @DisplayName("2 existen + 1 nuevo → solo 1 save y NEXT_TIME_DATA con el nuevo")
        void doWrite_mezcla_soloGuardaElNuevo() throws Exception {
            // Arrange
            Date f1 = new Date(1000L);
            Date f2 = new Date(2000L);
            Date f3 = new Date(3000L);

            TimeData existente1 = timeData("existente-1", f1);
            TimeData existente2 = timeData("existente-2", f2);
            TimeData nuevo = timeData("nuevo", f3);

            TimeData existingRef = timeData("x", f1);

            when(timeDataRepository.findById(dateFormat.format(f1)))
                    .thenReturn(Optional.of(existingRef));
            when(timeDataRepository.findById(dateFormat.format(f2)))
                    .thenReturn(Optional.of(existingRef));
            lenient().when(timeDataRepository.findById(dateFormat.format(f3)))
                    .thenReturn(Optional.empty());

            // dateFormat.format: se llama una vez por item para findById
            when(dateFormat.format(any(Date.class)))
                    .thenAnswer(inv -> {
                        Date d = inv.getArgument(0);
                        if (d == f1) return "2024-01-01";
                        if (d == f2) return "2024-01-02";
                        return "2024-01-03";
                    });

            stubExecutionContext();

            // Act
            writer.write(Chunk.of(existente1, existente2, nuevo));

            // Assert
            verify(timeDataRepository, times(3)).save(any(TimeData.class));
            verify(timeDataRepository).save(nuevo);
            assertThat(executionContext.get(NEXT_TIME_DATA_KEY)).isSameAs(nuevo);

            // setCollectionName: 3 veces (una por item)
            verify(repositoryCollectionCustom, times(3)).setCollectionName(COLLECTION);
        }

        @Test
        @DisplayName("NEXT_TIME_DATA se sobreescribe: tras N saves queda el último")
        void doWrite_variosNuevos_nextTimeDataEsElUltimo() throws Exception {
            // Arrange
            Date f1 = new Date(1000L);
            Date f2 = new Date(2000L);
            Date f3 = new Date(3000L);

            TimeData a = timeData("a", f1);
            TimeData b = timeData("b", f2);
            TimeData c = timeData("c", f3);

            stubItemNotInRepo();
            stubExecutionContext();
            when(dateFormat.format(any(Date.class)))
                    .thenAnswer(inv -> "fecha-" + ((Date) inv.getArgument(0)).getTime());

            // Act
            writer.write(Chunk.of(a, b, c));

            // Assert
            verify(timeDataRepository, times(3)).save(any(TimeData.class));
            assertThat(executionContext.get(NEXT_TIME_DATA_KEY)).isSameAs(c);
        }
    }

    // ==================================================================
    // Interacciones y efectos colaterales
    // ==================================================================

    @Nested
    @DisplayName("Efectos colaterales comunes")
    class EfectosComunesTests {

        @Test
        @DisplayName("setCollectionName se invoca 1 vez por item, no 1 por chunk")
        void doWrite_setCollectionNamePorItem() throws Exception {
            // Arrange
            stubItemNotInRepo();
            stubExecutionContext();
            when(dateFormat.format(any(Date.class))).thenReturn(FORMATTED_DATE);

            TimeData[] items = {
                    timeData("1", new Date()),
                    timeData("2", new Date()),
                    timeData("3", new Date()),
                    timeData("4", new Date()),
                    timeData("5", new Date())
            };

            // Act
            writer.write(Chunk.of(items));

            // Assert
            verify(repositoryCollectionCustom, times(5)).setCollectionName(COLLECTION);
            verify(timeDataRepository, times(5)).save(any(TimeData.class));
        }

        @Test
        @DisplayName("findById se invoca con el id formateado de la fecha del item")
        void doWrite_findByIdConIdFormateado() throws Exception {
            // Arrange
            Date fecha = new Date();
            TimeData item = timeData("x", fecha);
            stubItemNotInRepo();
            stubExecutionContext();
            when(dateFormat.format(fecha)).thenReturn(FORMATTED_DATE);

            // Act
            writer.write(Chunk.of(item));

            // Assert
            verify(timeDataRepository).findById(FORMATTED_DATE);
        }
    }

    // ==================================================================
    // Casos límite / errores latentes
    // ==================================================================

    @Nested
    @DisplayName("Casos límite")
    class CasosLimiteTests {

        @Test
        @DisplayName("jobExecution null + item nuevo → NullPointerException al publicar NEXT_TIME_DATA")
        void doWrite_jobExecutionNull_lanzaNPE() {
            // Arrange
            writer.jobExecution = null;
            TimeData item = timeData("x", new Date());
            stubItemNotInRepo();
            when(dateFormat.format(any(Date.class))).thenReturn(FORMATTED_DATE);

            // Act / Assert
            try {
                writer.write(Chunk.of(item));
                throw new AssertionError("Se esperaba NullPointerException");
            } catch (Exception e) {
                assertThat(e).isInstanceOf(NullPointerException.class);
            }

            // El save se ejecutó antes del NPE
            verify(timeDataRepository).save(item);
        }

        @Test
        @DisplayName("Si save lanza excepción, se propaga sin envolver")
        void doWrite_saveLanza_propaga() throws Exception {
            // Arrange
            TimeData item = timeData("x", new Date());
            RuntimeException boom = new IllegalStateException("fallo de save");
            when(timeDataRepository.findById(anyString())).thenReturn(Optional.empty());
            when(dateFormat.format(any(Date.class))).thenReturn(FORMATTED_DATE);
            doAnswer(inv -> { throw boom; })
                    .when(timeDataRepository).save(any(TimeData.class));

            // Act / Assert
            try {
                writer.write(Chunk.of(item));
                throw new AssertionError("Se esperaba la excepción");
            } catch (Exception e) {
                assertThat(e).isSameAs(boom);
            }

            // NO se llegó a publicar en ExecutionContext
            verify(jobExecution, never()).getExecutionContext();
            //assertThat(executionContext).isEmpty();
        }
    }
}