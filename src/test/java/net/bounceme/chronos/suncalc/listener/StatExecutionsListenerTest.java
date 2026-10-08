package net.bounceme.chronos.suncalc.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.item.ExecutionContext;

import net.bounceme.chronos.suncalc.model.Execution;

@ExtendWith(MockitoExtension.class)
@DisplayName("StatExecutionsListener - listener que inicializa EXECUTIONS y marca COMPLETED")
class StatExecutionsListenerTest {

    private static final String EXECUTIONS_KEY = "EXECUTIONS";

    @Mock private JobExecution jobExecution;

    @InjectMocks
    private StatExecutionsListener listener;

    private ExecutionContext executionContext;

    @BeforeEach
    void setUp() {
        executionContext = new ExecutionContext();
        lenient().when(jobExecution.getJobId()).thenReturn(1L);
    }

    // ==================================================================
    // initializeConfig (vía beforeJob)
    // ==================================================================

    @Nested
    @DisplayName("initializeConfig vía beforeJob")
    class InitializeConfigTests {

        @Test
        @DisplayName("Publica EXECUTIONS como List vacía en el ExecutionContext")
        void initializeConfig_creaListaVacia() {
            // Arrange
            lenient().when(jobExecution.getExecutionContext()).thenReturn(executionContext);

            // Act
            listener.beforeJob(jobExecution);

            // Assert
            Object value = executionContext.get(EXECUTIONS_KEY);
            assertThat(value).isInstanceOf(List.class);
            assertThat((List<?>) value).isEmpty();
        }

        @Test
        @DisplayName("EXECUTIONS es una lista modificable (ArrayList)")
        void initializeConfig_listaEsModificable() {
            // Arrange
            lenient().when(jobExecution.getExecutionContext()).thenReturn(executionContext);

            // Act
            listener.beforeJob(jobExecution);

            // Assert
            @SuppressWarnings("unchecked")
            List<Execution> executions = (List<Execution>) executionContext.get(EXECUTIONS_KEY);
            executions.add(new Execution()); // no debe lanzar UnsupportedOperationException
            assertThat(executions).hasSize(1);
        }

        @Test
        @DisplayName("Si ya existía EXECUTIONS, se reemplaza por una lista nueva vacía")
        void initializeConfig_reemplazaListaExistente() {
            // Arrange
            List<Execution> previa = new ArrayList<>();
            previa.add(new Execution());
            executionContext.put(EXECUTIONS_KEY, previa);
            lenient().when(jobExecution.getExecutionContext()).thenReturn(executionContext);

            // Act
            listener.beforeJob(jobExecution);

            // Assert
            Object value = executionContext.get(EXECUTIONS_KEY);
            assertThat(value).isNotSameAs(previa);
            assertThat((List<?>) value).isEmpty();
        }

        @Test
        @DisplayName("No publica la clave STEP_TIMES (no es responsabilidad de esta subclase)")
        void initializeConfig_noPublicaStepTimes() {
            // Arrange
            lenient().when(jobExecution.getExecutionContext()).thenReturn(executionContext);

            // Act
            listener.beforeJob(jobExecution);

            // Assert
            assertThat(executionContext.containsKey("STEP_TIMES")).isFalse();
            assertThat(executionContext.containsKey("ALREADY_EXECUTED")).isFalse();
        }

        @Test
        @DisplayName("No toca el ExitStatus en beforeJob")
        void initializeConfig_noTocaExitStatus() {
            // Arrange
            lenient().when(jobExecution.getExecutionContext()).thenReturn(executionContext);

            // Act
            listener.beforeJob(jobExecution);

            // Assert
            verify(jobExecution, never()).setExitStatus(any(ExitStatus.class));
        }
    }

    // ==================================================================
    // updateStatus (vía afterJob)
    // ==================================================================

    @Nested
    @DisplayName("updateStatus vía afterJob")
    class UpdateStatusTests {

        @Test
        @DisplayName("Siempre publica ExitStatus COMPLETED con descripción 'correctamente'")
        void updateStatus_siempreCompleted() {
            // Arrange
            lenient().when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);

            ArgumentCaptor<ExitStatus> captor = ArgumentCaptor.forClass(ExitStatus.class);

            // Act
            listener.afterJob(jobExecution);

            // Assert
            verify(jobExecution).setExitStatus(captor.capture());
            ExitStatus exitStatus = captor.getValue();
            assertThat(exitStatus.getExitCode()).isEqualTo("COMPLETED");
            assertThat(exitStatus.getExitDescription()).isEqualTo("La tarea ha sido ejecutada correctamente");
        }

        @Test
        @DisplayName("Ignora BatchStatus FAILED (siempre marca COMPLETED)")
        void updateStatus_ignoraBatchStatusFailed() {
            // Arrange
            lenient().when(jobExecution.getStatus()).thenReturn(BatchStatus.FAILED);

            ArgumentCaptor<ExitStatus> captor = ArgumentCaptor.forClass(ExitStatus.class);

            // Act
            listener.afterJob(jobExecution);

            // Assert
            verify(jobExecution).setExitStatus(captor.capture());
            assertThat(captor.getValue().getExitCode()).isEqualTo("COMPLETED");
        }

        @Test
        @DisplayName("Ignora BatchStatus STOPPED (siempre marca COMPLETED)")
        void updateStatus_ignoraBatchStatusStopped() {
            // Arrange
            lenient().when(jobExecution.getStatus()).thenReturn(BatchStatus.STOPPED);

            ArgumentCaptor<ExitStatus> captor = ArgumentCaptor.forClass(ExitStatus.class);

            // Act
            listener.afterJob(jobExecution);

            // Assert
            verify(jobExecution).setExitStatus(captor.capture());
            assertThat(captor.getValue().getExitCode()).isEqualTo("COMPLETED");
        }

        @Test
        @DisplayName("setExitStatus se invoca exactamente una vez por afterJob")
        void updateStatus_setExitStatusUnaVez() {
            // Arrange
            lenient().when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);

            // Act
            listener.afterJob(jobExecution);

            // Assert
            verify(jobExecution, times(1)).setExitStatus(any(ExitStatus.class));
        }

        @Test
        @DisplayName("afterJob no lee el ExecutionContext (no depende de EXECUTIONS)")
        void updateStatus_noLeeExecutionContext() {
            // Arrange
            lenient().when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);

            // Act
            listener.afterJob(jobExecution);

            // Assert: getExecutionContext no se invoca en afterJob
            verify(jobExecution, never()).getExecutionContext();
        }
    }

    // ==================================================================
    // Integración beforeJob + afterJob
    // ==================================================================

    @Nested
    @DisplayName("Integración beforeJob + afterJob")
    class IntegracionTests {

        @Test
        @DisplayName("Flujo completo: EXECUTIONS inicializado + ExitStatus COMPLETED")
        void integracion_flujoCompleto() {
            // Arrange
            lenient().when(jobExecution.getExecutionContext()).thenReturn(executionContext);
            lenient().when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);

            ArgumentCaptor<ExitStatus> captor = ArgumentCaptor.forClass(ExitStatus.class);

            // Act
            listener.beforeJob(jobExecution);
            listener.afterJob(jobExecution);

            // Assert
            verify(jobExecution).setExitStatus(captor.capture());
            assertThat(captor.getValue().getExitCode()).isEqualTo("COMPLETED");

            // EXECUTIONS sigue presente y vacío
            assertThat(executionContext.containsKey(EXECUTIONS_KEY)).isTrue();
            assertThat((List<?>) executionContext.get(EXECUTIONS_KEY)).isEmpty();
        }

        @Test
        @DisplayName("Si un tercero añade Execution a EXECUTIONS entre beforeJob y afterJob, se preserva")
        void integracion_executionsPreservado() {
            // Arrange
            lenient().when(jobExecution.getExecutionContext()).thenReturn(executionContext);
            lenient().when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);

            // Act
            listener.beforeJob(jobExecution);

            @SuppressWarnings("unchecked")
            List<Execution> executions = (List<Execution>) executionContext.get(EXECUTIONS_KEY);
            executions.add(new Execution());
            executions.add(new Execution());

            listener.afterJob(jobExecution);

            // Assert
            assertThat((List<?>) executionContext.get(EXECUTIONS_KEY)).hasSize(2);
        }
    }
}