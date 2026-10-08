package net.bounceme.chronos.suncalc.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@ExtendWith(MockitoExtension.class)
@DisplayName("TimeStepListener - medición de duración por step")
class TimeStepListenerTest {

    private static final String STEP_NAME = "importStep";
    private static final String STEP_TIMES_KEY = "STEP_TIMES";

    @Mock private StepExecution stepExecution;
    @Mock private JobExecution jobExecution;

    private TimeStepListener listener;
    private ExecutionContext jobExecutionContext;
    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        listener = new TimeStepListener();
        jobExecutionContext = new ExecutionContext();

        // stepExecution.getJobExecution() → jobExecution (común a casi todos los tests)
        lenient().when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        lenient().when(jobExecution.getExecutionContext()).thenReturn(jobExecutionContext);
        lenient().when(stepExecution.getStepName()).thenReturn(STEP_NAME);

        // Appender al ROOT para capturar logs del listener
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

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static long extractMs(String message) {
        String[] tokens = message.split(" ");
        for (int i = 0; i < tokens.length; i++) {
            if ("tardado".equals(tokens[i])) {
                return Long.parseLong(tokens[i + 1]);
            }
        }
        throw new AssertionError("No se encontró 'tardado N ms' en: " + message);
    }

    // ==================================================================
    // beforeStep
    // ==================================================================

    @Nested
    @DisplayName("beforeStep")
    class BeforeStepTests {

        @Test
        @DisplayName("Registra log INFO con el nombre del step")
        void beforeStep_logueaNombreDelStep() {
            // Act
            listener.beforeStep(stepExecution);

            // Assert
            assertThat(listAppender.list).hasSize(1);
            ILoggingEvent event = listAppender.list.get(0);
            assertThat(event.getLevel()).isEqualTo(Level.INFO);
            assertThat(event.getFormattedMessage())
                    .contains("Se va a ejecutar el step con nombre: " + STEP_NAME);
        }

        @Test
        @DisplayName("Establece startTime (verificable por duración en afterStep)")
        void beforeStep_estableceStartTime() throws Exception {
            // Act
            listener.beforeStep(stepExecution);
            Thread.sleep(30);

            // Preparamos STEP_TIMES para que afterStep pueda acumular
            jobExecutionContext.put(STEP_TIMES_KEY, new HashMap<String, Long>());

            ExitStatus status = listener.afterStep(stepExecution);

            // Assert
            assertThat(status).isEqualTo(ExitStatus.COMPLETED);
            String afterLog = listAppender.list.get(1).getFormattedMessage();
            assertThat(afterLog).contains("ha tardado");
            assertThat(extractMs(afterLog)).isGreaterThanOrEqualTo(30L);
        }
    }

    // ==================================================================
    // afterStep
    // ==================================================================

    @Nested
    @DisplayName("afterStep")
    class AfterStepTests {

        @Test
        @DisplayName("Siempre retorna ExitStatus.COMPLETED")
        void afterStep_siempreCompleted() {
            // Arrange
            listener.beforeStep(stepExecution);

            // Act
            ExitStatus status = listener.afterStep(stepExecution);

            // Assert
            assertThat(status).isEqualTo(ExitStatus.COMPLETED);
        }

        @Test
        @DisplayName("Registra log INFO con stepName y duración")
        void afterStep_logueaNombreYDuracion() {
            // Arrange
            listener.beforeStep(stepExecution);

            // Act
            listener.afterStep(stepExecution);

            // Assert
            ILoggingEvent afterLog = listAppender.list.get(1);
            assertThat(afterLog.getLevel()).isEqualTo(Level.INFO);
            assertThat(afterLog.getFormattedMessage())
                    .contains("Se ha terminado de ejecutar el step con nombre: " + STEP_NAME)
                    .contains("ha tardado");
        }
    }

    // ==================================================================
    // Acumulación en STEP_TIMES
    // ==================================================================

    @Nested
    @DisplayName("Acumulación en STEP_TIMES")
    class StepTimesTests {

        @Test
        @DisplayName("STEP_TIMES presente → añade stepName con la duración")
        void afterStep_stepTimesPresente_acumula() {
            // Arrange
            Map<String, Long> stepTimes = new HashMap<>();
            jobExecutionContext.put(STEP_TIMES_KEY, stepTimes);
            listener.beforeStep(stepExecution);

            // Act
            listener.afterStep(stepExecution);

            // Assert
            assertThat(stepTimes).containsKey(STEP_NAME);
            assertThat(stepTimes.get(STEP_NAME)).isGreaterThanOrEqualTo(0L);
        }

        @Test
        @DisplayName("STEP_TIMES presente con datos previos → preserva los anteriores")
        void afterStep_stepTimesConDatosPreservaLosAnteriores() {
            // Arrange
            Map<String, Long> stepTimes = new HashMap<>();
            stepTimes.put("otroStep", 999L);
            jobExecutionContext.put(STEP_TIMES_KEY, stepTimes);
            listener.beforeStep(stepExecution);

            // Act
            listener.afterStep(stepExecution);

            // Assert
            assertThat(stepTimes)
                    .containsEntry("otroStep", 999L)
                    .containsKey(STEP_NAME);
            assertThat(stepTimes).hasSize(2);
        }

        @Test
        @DisplayName("STEP_TIMES ausente → no falla y no acumula nada")
        void afterStep_stepTimesAusente_noAcumula() {
            // Arrange
            listener.beforeStep(stepExecution);

            // Act
            ExitStatus status = listener.afterStep(stepExecution);

            // Assert
            assertThat(status).isEqualTo(ExitStatus.COMPLETED);
            assertThat(jobExecutionContext.containsKey(STEP_TIMES_KEY)).isFalse();
        }

        @Test
        @DisplayName("STEP_TIMES existe pero es null → no acumula, no lanza")
        void afterStep_stepTimesNull_noLanza() {
            // Arrange
            jobExecutionContext.put(STEP_TIMES_KEY, null);
            listener.beforeStep(stepExecution);

            // Act
            ExitStatus status = listener.afterStep(stepExecution);

            // Assert
            assertThat(status).isEqualTo(ExitStatus.COMPLETED);
            assertThat(jobExecutionContext.get(STEP_TIMES_KEY)).isNull();
        }

        @Test
        @DisplayName("Acumula sobrescribiendo si ya existía el stepName")
        void afterStep_stepYaExistente_sobrescribeDuracion() {
            // Arrange
            Map<String, Long> stepTimes = new HashMap<>();
            stepTimes.put(STEP_NAME, 999L);
            jobExecutionContext.put(STEP_TIMES_KEY, stepTimes);
            listener.beforeStep(stepExecution);

            // Act
            listener.afterStep(stepExecution);

            // Assert
            assertThat(stepTimes.get(STEP_NAME)).isLessThan(999L);
        }
    }

    // ==================================================================
    // Casos límite
    // ==================================================================

    @Nested
    @DisplayName("Casos límite")
    class CasosLimiteTests {

        @Test
        @DisplayName("afterStep sin beforeStep previo → NullPointerException")
        void afterStep_sinBeforeStep_lanzaNPE() {
            // Arrange
            // NO llamamos a beforeStep, por lo que startTime == null

            // Act / Assert
            try {
                listener.afterStep(stepExecution);
                throw new AssertionError("Se esperaba NullPointerException");
            } catch (NullPointerException expected) {
                // OK
            }
        }

        @Test
        @DisplayName("STEP_TIMES con tipo incorrecto → ClassCastException")
        void afterStep_stepTimesTipoIncorrecto_lanzaCCE() {
            // Arrange
            jobExecutionContext.put(STEP_TIMES_KEY, "no soy un Map");
            listener.beforeStep(stepExecution);

            // Act / Assert
            try {
                listener.afterStep(stepExecution);
                throw new AssertionError("Se esperaba ClassCastException");
            } catch (ClassCastException expected) {
                // OK
            }
        }
    }

    // ==================================================================
    // Integración beforeStep + afterStep
    // ==================================================================

    @Nested
    @DisplayName("Integración")
    class IntegracionTests {

        @Test
        @DisplayName("Flujo completo: log, duración medida y acumulada")
        void integracion_flujoCompleto() throws Exception {
            // Arrange
            Map<String, Long> stepTimes = new HashMap<>();
            jobExecutionContext.put(STEP_TIMES_KEY, stepTimes);

            // Act
            listener.beforeStep(stepExecution);
            Thread.sleep(10);
            ExitStatus status = listener.afterStep(stepExecution);

            // Assert
            assertThat(status).isEqualTo(ExitStatus.COMPLETED);
            assertThat(stepTimes.get(STEP_NAME)).isGreaterThanOrEqualTo(10L);
            assertThat(listAppender.list).hasSize(2); // before + after
        }
    }
}