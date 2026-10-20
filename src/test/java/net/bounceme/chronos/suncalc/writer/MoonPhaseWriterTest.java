package net.bounceme.chronos.suncalc.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;

import net.bounceme.chronos.suncalc.model.MoonData;
import net.bounceme.chronos.suncalc.model.Registro;
import net.bounceme.chronos.suncalc.repository.MoonDataRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("MoonPhaseWriter - guarda MoonData solo si no existe")
class MoonPhaseWriterTest {

    private static final String FORMATTED_DATE = "2024-06-15";
    private static final String NEXT_TIME_DATA_KEY = "NEXT_TIME_DATA";

    @Mock private MoonDataRepository moonDataRepository;
    @Mock private SimpleDateFormat dateFormat;
    @Mock private StepExecution stepExecution;
    @Mock private JobExecution jobExecution;

    @InjectMocks
    private MoonPhaseWriter writer;

    private ExecutionContext executionContext;

    @BeforeEach
    void setUp() {
        writer.jobExecution = jobExecution;
        executionContext = new ExecutionContext();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Registro<MoonData> createMoonData(String id, Date fecha) {
        MoonData md = new MoonData();
        md.setId(id);
        md.setFecha(fecha);
        return new Registro<>(md);
    }

    // ==================================================================
    // beforeStep
    // ==================================================================

    @Nested
    @DisplayName("beforeStep")
    class BeforeStepTests {

        @Test
        @DisplayName("Asigna jobExecution a partir de stepExecution")
        void beforeStep_asignaJobExecution() {
            // Arrange
            when(stepExecution.getJobExecution()).thenReturn(jobExecution);

            // Act
            writer.beforeStep(stepExecution);

            // Assert
            assertThat(writer.jobExecution).isSameAs(jobExecution);
            verify(stepExecution).getJobExecution();
        }
    }

    // ==================================================================
    // write: Chunk vacío
    // ==================================================================

    @Nested
    @DisplayName("write: Chunk vacío")
    class ChunkVacioTests {

        @Test
        @DisplayName("No itera, no toca repositorios ni ExecutionContext")
        void write_chunkVacio_sinInteracciones() throws Exception {
            // Act
            writer.write(Chunk.of());

            // Assert
            verifyNoInteractions(moonDataRepository, dateFormat, jobExecution);
        }
    }

    // ==================================================================
    // write: Item ya existe en BD
    // ==================================================================

    @Nested
    @DisplayName("write: Item ya existe en BD")
    class ItemExistenteTests {

        @Test
        @DisplayName("findById presente → NO guarda y NO publica en ExecutionContext")
        void write_itemExistente_noGuarda() throws Exception {
            // Arrange
            Date fecha = new Date();
            Registro<MoonData> item = createMoonData("id-existente", fecha);
            when(dateFormat.format(fecha)).thenReturn(FORMATTED_DATE);
            when(moonDataRepository.findById(FORMATTED_DATE))
                    .thenReturn(Optional.of(new MoonData()));

            // Act
            writer.write(Chunk.of(item));

            // Assert
            verify(moonDataRepository).findById(FORMATTED_DATE);
            verify(moonDataRepository, never()).save(any(MoonData.class));
            verify(jobExecution, never()).getExecutionContext();
        }
    }

    // ==================================================================
    // write: Item nuevo (no existe en BD)
    // ==================================================================

    @Nested
    @DisplayName("write: Item nuevo")
    class ItemNuevoTests {

        @Test
        @DisplayName("id null → asigna id, guarda y publica en ExecutionContext")
        void write_itemNuevoSinId_asignaGuardaYPublica() throws Exception {
            // Arrange
            Date fecha = new Date();
            Registro<MoonData> item = createMoonData(null, fecha);
            when(dateFormat.format(fecha)).thenReturn(FORMATTED_DATE);
            when(moonDataRepository.findById(FORMATTED_DATE)).thenReturn(Optional.empty());
            when(jobExecution.getExecutionContext()).thenReturn(executionContext);

            // Act
            writer.write(Chunk.of(item));

            // Assert
            assertThat(item.getContent().getId()).isEqualTo(FORMATTED_DATE);
            verify(moonDataRepository).save(item.getContent());
            assertThat(executionContext.get(NEXT_TIME_DATA_KEY)).isSameAs(item);
            
            // format llamado 2 veces: 1 para findById y 1 para setId
            verify(dateFormat, times(2)).format(fecha);
        }

        @Test
        @DisplayName("id no null → no modifica id, guarda y publica en ExecutionContext")
        void write_itemNuevoConId_guardaYPublica() throws Exception {
            // Arrange
            Date fecha = new Date();
            Registro<MoonData> item = createMoonData("id-predefinido", fecha);
            when(dateFormat.format(fecha)).thenReturn(FORMATTED_DATE);
            when(moonDataRepository.findById(FORMATTED_DATE)).thenReturn(Optional.empty());
            when(jobExecution.getExecutionContext()).thenReturn(executionContext);

            // Act
            writer.write(Chunk.of(item));

            // Assert
            assertThat(item.getContent().getId()).isEqualTo("id-predefinido");
            verify(moonDataRepository).save(item.getContent());
            assertThat(executionContext.get(NEXT_TIME_DATA_KEY)).isSameAs(item);
            
            // format llamado 1 sola vez: solo para findById
            verify(dateFormat, times(1)).format(fecha);
        }
    }

    // ==================================================================
    // write: Múltiples items
    // ==================================================================

    @Nested
    @DisplayName("write: Múltiples items")
    class MultiplesItemsTests {

        @Test
        @DisplayName("Sobrescribe NEXT_TIME_DATA con el último item guardado")
        void write_multiplesItems_sobrescribeNextTimeData() throws Exception {
            // Arrange
            Date f1 = new Date(1000L);
            Date f2 = new Date(2000L);
            Date f3 = new Date(3000L);
            
            Registro<MoonData> item1 = createMoonData("1", f1);
            Registro<MoonData> item2 = createMoonData("2", f2); // existente
            Registro<MoonData> item3 = createMoonData("3", f3); // último guardado

            when(dateFormat.format(f1)).thenReturn("2024-01-01");
            when(dateFormat.format(f2)).thenReturn("2024-01-02");
            when(dateFormat.format(f3)).thenReturn("2024-01-03");

            when(moonDataRepository.findById("2024-01-01")).thenReturn(Optional.empty());
            when(moonDataRepository.findById("2024-01-02")).thenReturn(Optional.of(new MoonData()));
            when(moonDataRepository.findById("2024-01-03")).thenReturn(Optional.empty());

            when(jobExecution.getExecutionContext()).thenReturn(executionContext);

            // Act
            writer.write(Chunk.of(item1, item2, item3));

            // Assert
            verify(moonDataRepository).save(item1.getContent());
            verify(moonDataRepository, never()).save(item2.getContent());
            verify(moonDataRepository).save(item3.getContent());
            
            // NEXT_TIME_DATA debe ser item3, no item1
            assertThat(executionContext.get(NEXT_TIME_DATA_KEY)).isSameAs(item3);
        }
    }

    // ==================================================================
    // Casos límite / Excepciones
    // ==================================================================

    @Nested
    @DisplayName("Casos límite y excepciones")
    class ExcepcionesTests {

        @Test
        @DisplayName("jobExecution null al intentar publicar en ExecutionContext → NPE")
        void write_jobExecutionNull_lanzaNPE() {
            // Arrange
            writer.jobExecution = null;
            Registro<MoonData> item = createMoonData("x", new Date());
            when(dateFormat.format(any(Date.class))).thenReturn(FORMATTED_DATE);
            when(moonDataRepository.findById(FORMATTED_DATE)).thenReturn(Optional.empty());

            // Act / Assert
            // Extraemos el Chunk fuera de la lambda
            Chunk<Registro<MoonData>> chunk = Chunk.of(item);
            
            assertThatThrownBy(() -> writer.write(chunk))
                    .isInstanceOf(NullPointerException.class);

            // El guardado ocurre antes del NPE
            verify(moonDataRepository).save(item.getContent());
        }

        @Test
        @DisplayName("Si save lanza excepción, se propaga")
        void write_saveLanza_propaga() throws Exception {
            // Arrange
            Date fecha = new Date();
            Registro<MoonData> item = createMoonData("x", fecha);
            RuntimeException boom = new IllegalStateException("Fallo de guardado");

            when(dateFormat.format(fecha)).thenReturn(FORMATTED_DATE);
            when(moonDataRepository.findById(FORMATTED_DATE)).thenReturn(Optional.empty());
            doAnswer(inv -> { throw boom; }).when(moonDataRepository).save(any(MoonData.class));

            // Act / Assert
            try {
                writer.write(Chunk.of(item));
                throw new AssertionError("Se esperaba una excepción");
            } catch (Exception e) {
                assertThat(e).isSameAs(boom);
            }

            // No llega a publicar en el ExecutionContext
            verify(jobExecution, never()).getExecutionContext();
        }
    }
}