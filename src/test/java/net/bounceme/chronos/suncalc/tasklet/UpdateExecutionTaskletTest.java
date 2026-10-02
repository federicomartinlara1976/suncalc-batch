package net.bounceme.chronos.suncalc.tasklet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;

import net.bounceme.chronos.suncalc.model.Execution;
import net.bounceme.chronos.suncalc.repository.ExecutionsRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateExecutionTasklet - alta o incremento del contador de ejecuciones")
class UpdateExecutionTaskletTest {

    private static final String FORMATTED_DATE = "2024-06-15";
    private static final String STEP_NAME = "importStep";
    private static final String STEP_TIMES_KEY = "STEP_TIMES";

    @Mock private ExecutionsRepository executionsRepository;
    @Mock private SimpleDateFormat dateFormat;

    @Mock private StepContribution contribution;
    @Mock private ChunkContext chunkContext;
    @Mock private StepContext stepContext;

    @InjectMocks
    private UpdateExecutionTasklet tasklet;

    private Map<String, Object> jobExecutionContext;

    @BeforeEach
    void setUp() throws Exception {
        // El formateo de fecha es estable durante todo el test
        when(dateFormat.format(any(Date.class))).thenReturn(FORMATTED_DATE);

        // JobExecutionContext real (es un Map<String,Object>)
        jobExecutionContext = new HashMap<>();
        lenient().when(chunkContext.getStepContext()).thenReturn(stepContext);
        lenient().when(stepContext.getJobExecutionContext()).thenReturn(jobExecutionContext);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void stubStepTimes(Long duration) {
        jobExecutionContext.put(STEP_TIMES_KEY, Map.of(STEP_NAME, duration));
    }

    // ==================================================================
    // Rama: no existen ejecuciones previas → crear una nueva
    // ==================================================================

    @Nested
    @DisplayName("Sin ejecuciones previas (crea una nueva)")
    class RamaCreacionTests {

        @Test
        @DisplayName("Lista vacía → crea Execution con id=fecha, value=1 y executionTime del STEP_TIMES")
        void execute_listaVacia_creaNuevaEjecucion() throws Exception {
            // Arrange
            when(executionsRepository.findByDate(FORMATTED_DATE))
                    .thenReturn(Collections.emptyList());
            stubStepTimes(5000L);

            ArgumentCaptor<Execution> captor = ArgumentCaptor.forClass(Execution.class);

            // Act
            RepeatStatus status = tasklet.execute(contribution, chunkContext);

            // Assert
            assertThat(status).isEqualTo(RepeatStatus.FINISHED);
            verify(executionsRepository).save(captor.capture());
            Execution saved = captor.getValue();

            verify(saved).setId(FORMATTED_DATE);
            verify(saved).setValue(1);
            verify(saved).setExecutionTime(5000L);

            verify(executionsRepository).findByDate(FORMATTED_DATE);
            verify(executionsRepository, times(1)).save(any(Execution.class));
        }

        @Test
        @DisplayName("Lista null → también entra en la rama de creación (CollectionUtils.isEmpty)")
        void execute_listaNull_creaNuevaEjecucion() throws Exception {
            // Arrange
            when(executionsRepository.findByDate(FORMATTED_DATE)).thenReturn(null);
            stubStepTimes(1234L);

            // Act
            tasklet.execute(contribution, chunkContext);

            // Assert
            verify(executionsRepository).save(any(Execution.class));
        }

        @Test
        @DisplayName("STEP_TIMES presente pero sin la clave del step → executionTime = null")
        void execute_stepTimesSinClave_executionTimeNull() throws Exception {
            // Arrange
            when(executionsRepository.findByDate(FORMATTED_DATE))
                    .thenReturn(Collections.emptyList());
            // STEP_TIMES tiene otras claves, pero no 'importStep'
            jobExecutionContext.put(STEP_TIMES_KEY, Map.of("otroStep", 1L));

            ArgumentCaptor<Execution> captor = ArgumentCaptor.forClass(Execution.class);

            // Act
            tasklet.execute(contribution, chunkContext);

            // Assert
            verify(executionsRepository).save(captor.capture());
            verify(captor.getValue()).setExecutionTime(null);
        }

        @Test
        @DisplayName("STEP_TIMES ausente en el JobExecutionContext → NullPointerException")
        void execute_stepTimesAusente_lanzaNPE() {
            // Arrange
            when(executionsRepository.findByDate(FORMATTED_DATE))
                    .thenReturn(Collections.emptyList());
            // NO se rellena STEP_TIMES

            // Act / Assert
            assertThatThrownBy(() -> tasklet.execute(contribution, chunkContext))
                    .isInstanceOf(NullPointerException.class);

            verify(executionsRepository, never()).save(any());
        }
    }

    // ==================================================================
    // Rama: ya existen ejecuciones → incrementar contador
    // ==================================================================

    @Nested
    @DisplayName("Con ejecuciones previas (incrementa contador)")
    class RamaIncrementoTests {

        @Test
        @DisplayName("Lista con un Execution → incrementa value en 1 y guarda")
        void execute_unaEjecucion_incrementaValor() throws Exception {
            // Arrange
            Execution existing = mock(Execution.class);
            when(existing.getValue()).thenReturn(7);
            when(executionsRepository.findByDate(FORMATTED_DATE))
                    .thenReturn(List.of(existing));

            // Act
            RepeatStatus status = tasklet.execute(contribution, chunkContext);

            // Assert
            assertThat(status).isEqualTo(RepeatStatus.FINISHED);
            verify(existing).setValue(8);
            verify(existing, never()).setId(anyString());
            verify(existing, never()).setExecutionTime(any());
            verify(executionsRepository).save(existing);
        }

        @Test
        @DisplayName("Lista con varios Execution → usa el primero (índice 0)")
        void execute_variasEjecuciones_usaElPrimero() throws Exception {
            // Arrange
            Execution primero = mock(Execution.class);
            Execution segundo = mock(Execution.class);
            when(primero.getValue()).thenReturn(1);
            when(segundo.getValue()).thenReturn(99);
            when(executionsRepository.findByDate(FORMATTED_DATE))
                    .thenReturn(List.of(primero, segundo));

            // Act
            tasklet.execute(contribution, chunkContext);

            // Assert
            verify(primero).setValue(2);
            verify(segundo, never()).setValue(any());
            verify(segundo, never()).setId(anyString());
            verify(executionsRepository).save(primero);
            verify(executionsRepository, never()).save(segundo);
        }

        @Test
        @DisplayName("value null en el Execution → NullPointerException al sumar")
        void execute_valueNull_lanzaNPE() {
            // Arrange
            Execution existing = mock(Execution.class);
            when(existing.getValue()).thenReturn(null);
            when(executionsRepository.findByDate(FORMATTED_DATE))
                    .thenReturn(List.of(existing));

            // Act / Assert
            assertThatThrownBy(() -> tasklet.execute(contribution, chunkContext))
                    .isInstanceOf(NullPointerException.class);

            verify(executionsRepository, never()).save(any());
        }
    }

    // ==================================================================
    // Interacción común entre ramas
    // ==================================================================

    @Nested
    @DisplayName("Interacciones comunes")
    class InteraccionesComunesTests {

        @Test
        @DisplayName("findByDate se invoca siempre con la fecha formateada")
        void execute_siempreConsultaFindByDate() throws Exception {
            // Arrange
            when(executionsRepository.findByDate(FORMATTED_DATE))
                    .thenReturn(Collections.emptyList());
            stubStepTimes(1L);

            // Act
            tasklet.execute(contribution, chunkContext);

            // Assert
            verify(executionsRepository).findByDate(FORMATTED_DATE);
            //verifyNoMoreInteractions(executionsRepository);
            // findByDate + save únicamente
        }

        @Test
        @DisplayName("dateFormat.format se invoca al menos una vez (para buscar)")
        void execute_formateaFecha() throws Exception {
            // Arrange
            when(executionsRepository.findByDate(FORMATTED_DATE))
                    .thenReturn(Collections.emptyList());
            stubStepTimes(1L);

            // Act
            tasklet.execute(contribution, chunkContext);

            // Assert
            verify(dateFormat, times(2)).format(any(Date.class)); // buscar + setId
        }
    }
}
