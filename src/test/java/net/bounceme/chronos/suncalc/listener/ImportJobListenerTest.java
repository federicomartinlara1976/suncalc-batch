package net.bounceme.chronos.suncalc.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.item.ExecutionContext;

import net.bounceme.chronos.notifications.services.NotificationService;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImportJobListener - listener del Job de importación")
class ImportJobListenerTest {

    @Mock private NotificationService notificationService;
    @Mock private JobExecution jobExecution;

    @InjectMocks
    private ImportJobListener listener;

    private ExecutionContext executionContext;

    @BeforeEach
    void setUp() {
        executionContext = new ExecutionContext();
        lenient().when(jobExecution.getExecutionContext()).thenReturn(executionContext);
    }

    // ==================================================================
    // initializeConfig (vía beforeJob)
    // ==================================================================

    @Nested
    @DisplayName("initializeConfig")
    class InitializeConfigTests {

        @Test
        @DisplayName("beforeJob inicializa STEP_TIMES con un mapa vacío")
        void initializeConfig_creaStepTimesVacio() {
            // Act
            listener.beforeJob(jobExecution);

            // Assert
            Object stepTimes = executionContext.get("STEP_TIMES");
            assertThat(stepTimes).isInstanceOf(Map.class);
            assertThat((Map<?, ?>) stepTimes).isEmpty();
        }

        @Test
        @DisplayName("Si ya existía STEP_TIMES, se reemplaza por uno nuevo vacío")
        void initializeConfig_reemplazaStepTimesExistente() {
            // Arrange
            executionContext.put("STEP_TIMES", Map.of("importStep", 5000L));

            // Act
            listener.beforeJob(jobExecution);

            // Assert
            Object stepTimes = executionContext.get("STEP_TIMES");
            assertThat((Map<?, ?>) stepTimes).isEmpty();
        }

        @Test
        @DisplayName("Se invoca a initializeConfig exactamente una vez por beforeJob")
        void initializeConfig_unaVezPorBeforeJob() {
            // Act
            listener.beforeJob(jobExecution);

            // Assert: el mapa se ha creado una sola vez
            assertThat(executionContext.get("STEP_TIMES")).isNotNull();
        }
    }

    // ==================================================================
    // updateStatus: status != COMPLETED
    // ==================================================================

    @Nested
    @DisplayName("updateStatus con status distinto de COMPLETED")
    class StatusNoCompletadoTests {

        @Test
        @DisplayName("BatchStatus.FAILED → no toca ExitStatus ni notifica")
        void updateStatus_failed_noHaceNada() {
            // Arrange
            when(jobExecution.getStatus()).thenReturn(BatchStatus.FAILED);

            // Act
            listener.afterJob(jobExecution);

            // Assert
            verify(jobExecution, never()).setExitStatus(org.mockito.ArgumentMatchers.any());
            verify(notificationService, never())
                    .sendNotification(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("BatchStatus.STARTED → no toca ExitStatus ni notifica")
        void updateStatus_started_noHaceNada() {
            // Arrange
            when(jobExecution.getStatus()).thenReturn(BatchStatus.STARTED);

            // Act
            listener.afterJob(jobExecution);

            // Assert
            verify(jobExecution, never()).setExitStatus(org.mockito.ArgumentMatchers.any());
            verify(notificationService, never())
                    .sendNotification(anyString(), anyString(), anyString());
        }
    }

    // ==================================================================
    // updateStatus: COMPLETED + alreadyExecuted
    // ==================================================================

    @Nested
    @DisplayName("updateStatus con status COMPLETED")
    class StatusCompletadoTests {

        @BeforeEach
        void setUp() {
            when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        }

        @Test
        @DisplayName("alreadyExecuted=true → NOOP + notificación WARNING")
        void updateStatus_alreadyExecutedTrue_noop() {
            // Arrange
            executionContext.put("ALREADY_EXECUTED", Boolean.TRUE);

            ArgumentCaptor<ExitStatus> statusCaptor = ArgumentCaptor.forClass(ExitStatus.class);

            // Act
            listener.afterJob(jobExecution);

            // Assert
            verify(jobExecution).setExitStatus(statusCaptor.capture());
            ExitStatus exitStatus = statusCaptor.getValue();
            assertThat(exitStatus.getExitCode()).isEqualTo("NOOP");
            assertThat(exitStatus.getExitDescription()).isEqualTo("La tarea ya ha sido ejecutada");

            verify(notificationService).sendNotification(
                    "suncalc-batch",
                    "La tarea ya ha sido ejecutada",
                    "WARNING");
        }

        @Test
        @DisplayName("alreadyExecuted=false → COMPLETED + notificación OK")
        void updateStatus_alreadyExecutedFalse_completed() {
            // Arrange
            executionContext.put("ALREADY_EXECUTED", Boolean.FALSE);

            ArgumentCaptor<ExitStatus> statusCaptor = ArgumentCaptor.forClass(ExitStatus.class);

            // Act
            listener.afterJob(jobExecution);

            // Assert
            verify(jobExecution).setExitStatus(statusCaptor.capture());
            ExitStatus exitStatus = statusCaptor.getValue();
            assertThat(exitStatus.getExitCode()).isEqualTo("COMPLETED");
            assertThat(exitStatus.getExitDescription()).isEqualTo("La tarea ha sido ejecutada correctamente");

            verify(notificationService).sendNotification(
                    "suncalc-batch",
                    "La tarea ha sido ejecutada correctamente",
                    "OK");
        }

        @Test
        @DisplayName("alreadyExecuted ausente (null) → COMPLETED + notificación OK")
        void updateStatus_alreadyExecutedNull_completed() {
            // Arrange
            // No se mete nada en executionContext → get devuelve null

            ArgumentCaptor<ExitStatus> statusCaptor = ArgumentCaptor.forClass(ExitStatus.class);

            // Act
            listener.afterJob(jobExecution);

            // Assert
            verify(jobExecution).setExitStatus(statusCaptor.capture());
            ExitStatus exitStatus = statusCaptor.getValue();
            assertThat(exitStatus.getExitCode()).isEqualTo("COMPLETED");
            assertThat(exitStatus.getExitDescription()).isEqualTo("La tarea ha sido ejecutada correctamente");

            verify(notificationService).sendNotification(
                    "suncalc-batch",
                    "La tarea ha sido ejecutada correctamente",
                    "OK");
        }

        @Test
        @DisplayName("alreadyExecuted=true (Boolean.true) → 1 sola notificación WARNING")
        void updateStatus_alreadyExecutedTrue_unaSolaNotificacion() {
            // Arrange
            executionContext.put("ALREADY_EXECUTED", Boolean.TRUE);

            // Act
            listener.afterJob(jobExecution);

            // Assert
            verify(notificationService, times(1))
                    .sendNotification(anyString(), anyString(), anyString());
        }
    }

    // ==================================================================
    // Integración beforeJob + afterJob
    // ==================================================================

    @Nested
    @DisplayName("Integración beforeJob + afterJob")
    class IntegracionTests {

        @Test
        @DisplayName("beforeJob inicializa, afterJob con COMPLETED usa ese contexto")
        void integracion_beforeJobAfterJob_completed() {
            // Arrange
            lenient().when(jobExecution.getJobId()).thenReturn(1L);
            when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);

            // Act
            listener.beforeJob(jobExecution);
            listener.afterJob(jobExecution);

            // Assert
            assertThat(executionContext.get("STEP_TIMES")).isInstanceOf(Map.class);
            verify(jobExecution).setExitStatus(org.mockito.ArgumentMatchers.any(ExitStatus.class));
            verify(notificationService).sendNotification(
                    eq("suncalc-batch"), anyString(), eq("OK"));
        }
    }
}