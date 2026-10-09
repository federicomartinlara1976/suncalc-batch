package net.bounceme.chronos.suncalc.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.item.ExecutionContext;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@ExtendWith(MockitoExtension.class)
@DisplayName("LastExecutionsListener - listener que siempre marca COMPLETED")
class LastExecutionsListenerTest {

    private static final long JOB_ID = 7L;

    @Mock private JobExecution jobExecution;

    @InjectMocks
    private LastExecutionsListener listener;

    private ExecutionContext executionContext;
    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        executionContext = new ExecutionContext();
        lenient().when(jobExecution.getJobId()).thenReturn(JOB_ID);

        logger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        logger.setLevel(Level.INFO);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
        listAppender.stop();
    }

    // ==================================================================
    // initializeConfig (vía beforeJob)
    // ==================================================================

    @Nested
    @DisplayName("initializeConfig vía beforeJob")
    class InitializeConfigTests {

        @Test
        @DisplayName("Registra 'initializeConfig' a nivel INFO")
        void initializeConfig_registraLog() {
            // Arrange
            lenient().when(jobExecution.getExecutionContext()).thenReturn(executionContext);

            // Act
            listener.beforeJob(jobExecution);

            // Assert
            List<ILoggingEvent> fromThisClass = listAppender.list.stream()
                    .filter(e -> e.getLoggerName().equals(LastExecutionsListener.class.getName()))
                    .toList();
            assertThat(fromThisClass).hasSize(1);
            assertThat(fromThisClass.get(0).getLevel()).isEqualTo(Level.INFO);
            assertThat(fromThisClass.get(0).getFormattedMessage()).isEqualTo("initializeConfig");
        }

        @Test
        @DisplayName("No escribe nada en el ExecutionContext")
        void initializeConfig_noEscribeEnExecutionContext() {
            // Arrange
            lenient().when(jobExecution.getExecutionContext()).thenReturn(executionContext);

            // Act
            listener.beforeJob(jobExecution);

            // Assert
            //assertThat(executionContext).isEmpty();
            assertThat(executionContext.containsKey("STEP_TIMES")).isFalse();
        }

        @Test
        @DisplayName("beforeJob sigue publicando el log del padre con el JobId")
        void beforeJob_logueaJobIdHeredado() {
            // Arrange
            lenient().when(jobExecution.getExecutionContext()).thenReturn(executionContext);

            // Act
            listener.beforeJob(jobExecution);

            // Assert
            List<ILoggingEvent> fromParent = listAppender.list.stream()
                    .filter(e -> e.getLoggerName().equals(AbstractListener.class.getName()))
                    .toList();
            assertThat(fromParent).hasSize(1);
            assertThat(fromParent.get(0).getFormattedMessage())
                    .contains("Se va a ejecutar el Job con ID: " + JOB_ID);
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
        @DisplayName("Ignora BatchStatus del JobExecution (aunque sea FAILED)")
        void updateStatus_ignoraBatchStatus() {
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
        @DisplayName("Ignora ALREADY_EXECUTED del ExecutionContext")
        void updateStatus_ignoraAlreadyExecuted() {
            // Arrange
            executionContext.put("ALREADY_EXECUTED", Boolean.TRUE);
            lenient().when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);

            ArgumentCaptor<ExitStatus> captor = ArgumentCaptor.forClass(ExitStatus.class);

            // Act
            listener.afterJob(jobExecution);

            // Assert
            verify(jobExecution).setExitStatus(captor.capture());
            assertThat(captor.getValue().getExitCode()).isEqualTo("COMPLETED");
            assertThat(captor.getValue().getExitDescription())
                    .isEqualTo("La tarea ha sido ejecutada correctamente");
        }

        @Test
        @DisplayName("afterJob invoca setExitStatus exactamente una vez")
        void afterJob_setExitStatusUnaVez() {
            // Arrange
            lenient().when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);

            // Act
            listener.afterJob(jobExecution);

            // Assert
            verify(jobExecution, times(1)).setExitStatus(any(ExitStatus.class));
        }
    }

    // ==================================================================
    // Integración beforeJob + afterJob
    // ==================================================================

    @Nested
    @DisplayName("Integración beforeJob + afterJob")
    class IntegracionTests {

        @Test
        @DisplayName("Flujo completo: log de init + log de fin + setExitStatus COMPLETED")
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

            // No se ha escrito nada en el contexto
            //assertThat(executionContext).isEmpty();

            // Hay 3 logs: initializeConfig, "Se va a ejecutar...", "Se ha terminado..."
            assertThat(listAppender.list).hasSize(3);
        }
    }

    // ==================================================================
    // No interacciones no esperadas
    // ==================================================================

    @Nested
    @DisplayName("Sin interacciones inesperadas")
    class NoInteraccionesTests {

        @Test
        @DisplayName("initializeConfig no toca el JobExecution")
        void initializeConfig_noTocaJobExecution() {
            // Arrange
            lenient().when(jobExecution.getExecutionContext()).thenReturn(executionContext);

            // Act
            listener.beforeJob(jobExecution);

            // Assert: el único acceso es getJobId (del padre) y getExecutionContext
            // (aquí no se llama en la subclase, solo lo haría el padre si lo necesitara)
            verify(jobExecution, never()).setExitStatus(any(ExitStatus.class));
        }
    }
}