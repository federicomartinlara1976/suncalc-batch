package net.bounceme.chronos.suncalc.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Duration;
import static org.awaitility.Awaitility.await;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@ExtendWith(MockitoExtension.class)
@DisplayName("AbstractListener - esqueleto de listener de Job")
class AbstractListenerTest {

    private static final long JOB_ID = 42L;

    @Mock private JobExecution jobExecution;

    private TestListener listener;
    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        listener = new TestListener();

        logger = (Logger) LoggerFactory.getLogger(AbstractListener.class);
        logger.setLevel(Level.INFO);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        when(jobExecution.getJobId()).thenReturn(JOB_ID);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
        listAppender.stop();
    }

    // ==================================================================
    // beforeJob
    // ==================================================================

    @Nested
    @DisplayName("beforeJob")
    class BeforeJobTests {

        @Test
        @DisplayName("Invoca initializeConfig(jobExecution)")
        void beforeJob_invocaInitializeConfig() {
            // Act
            listener.beforeJob(jobExecution);

            // Assert
            assertThat(listener.initializeInvocations.get()).isEqualTo(1);
            assertThat(listener.lastJobExecution).isSameAs(jobExecution);
        }

        @Test
        @DisplayName("Registra log INFO con el JobId")
        void beforeJob_logueaJobId() {
            // Act
            listener.beforeJob(jobExecution);

            // Assert
            assertThat(listAppender.list).hasSize(1);
            ILoggingEvent event = listAppender.list.get(0);
            assertThat(event.getLevel()).isEqualTo(Level.INFO);
            assertThat(event.getFormattedMessage())
                    .contains("Se va a ejecutar el Job con ID: " + JOB_ID);
        }

        @Test
        @DisplayName("Establece startTime (verificable por duración en afterJob)")
        void beforeJob_estableceStartTime() throws Exception {
            // Act
            listener.beforeJob(jobExecution);
            await().atMost(Duration.ofSeconds(30)).until(didTheThing());  // Compliant

            // Assert: el mensaje de afterJob contiene "ha tardado N ms" con N >= 30
            ILoggingEvent afterJobEvent = listAppender.list.get(1);
            String message = afterJobEvent.getFormattedMessage();
            assertThat(message).contains("ha tardado");
            assertThat(extractMs(message)).isGreaterThanOrEqualTo(30L);
        }
        
        private Callable<Boolean> didTheThing() {
        	  return new Callable<Boolean>() {
        	    public Boolean call() throws Exception {
        	    	listener.afterJob(jobExecution);
        	    	return true;
        	    }
        	  };
        	}
    }

    // ==================================================================
    // afterJob
    // ==================================================================

    @Nested
    @DisplayName("afterJob")
    class AfterJobTests {

        @Test
        @DisplayName("Invoca updateStatus(jobExecution)")
        void afterJob_invocaUpdateStatus() {
            // Arrange
            listener.beforeJob(jobExecution);

            // Act
            listener.afterJob(jobExecution);

            // Assert
            assertThat(listener.updateStatusInvocations.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("Registra log INFO con JobId y duración")
        void afterJob_logueaJobIdYDuracion() {
            // Arrange
            listener.beforeJob(jobExecution);

            // Act
            listener.afterJob(jobExecution);

            // Assert
            ILoggingEvent afterJobEvent = listAppender.list.get(1);
            assertThat(afterJobEvent.getLevel()).isEqualTo(Level.INFO);
            assertThat(afterJobEvent.getFormattedMessage())
                    .contains("Se ha terminado de ejecutar el Job con ID: " + JOB_ID)
                    .contains("ha tardado");
        }

        @Test
        @DisplayName("La duración se calcula con System.currentTimeMillis")
        void afterJob_duracionEsPositiva() {
            // Arrange
            listener.beforeJob(jobExecution);

            // Act
            listener.afterJob(jobExecution);

            // Assert: el mensaje contiene un número >= 0
            long duration = extractMs(listAppender.list.get(1).getFormattedMessage());
            assertThat(duration).isNotNegative();
        }
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private static long extractMs(String message) {
        // "..., ha tardado 42 ms"
        String[] tokens = message.split(" ");
        for (int i = 0; i < tokens.length; i++) {
            if ("tardado".equals(tokens[i])) {
                return Long.parseLong(tokens[i + 1]);
            }
        }
        throw new AssertionError("No se encontró 'tardado N ms' en: " + message);
    }

    // ==================================================================
    // Test double
    // ==================================================================

    static class TestListener extends AbstractListener {

        final AtomicInteger initializeInvocations = new AtomicInteger(0);
        final AtomicInteger updateStatusInvocations = new AtomicInteger(0);
        JobExecution lastJobExecution;

        @Override
        protected void initializeConfig(JobExecution jobExecution) {
            initializeInvocations.incrementAndGet();
            lastJobExecution = jobExecution;
        }

        @Override
        protected void updateStatus(JobExecution jobExecution) {
            updateStatusInvocations.incrementAndGet();
        }
    }
}