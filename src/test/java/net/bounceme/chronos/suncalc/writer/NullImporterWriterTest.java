package net.bounceme.chronos.suncalc.writer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import net.bounceme.chronos.suncalc.model.TimeData;

@DisplayName("NullImporterWriter - writer no-op que solo registra en log")
class NullImporterWriterTest {

    private NullImporterWriter writer;
    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        writer = new NullImporterWriter();

        logger = (Logger) LoggerFactory.getLogger(NullImporterWriter.class);
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

    private List<ILoggingEvent> eventosLog() {
        return listAppender.list;
    }

    // ==================================================================
    // write: iteración del Chunk
    // ==================================================================

    @Nested
    @DisplayName("write: iteración del Chunk")
    class WriteTests {

        @Test
        @DisplayName("Chunk vacío → no se registra ningún log")
        void write_chunkVacio_sinLogs() throws Exception {
            // Arrange
            Chunk<TimeData> chunk = Chunk.of();

            // Act
            writer.write(chunk);

            // Assert
            assertThat(eventosLog()).isEmpty();
        }

        @Test
        @DisplayName("Chunk con 1 elemento → 1 log con el item")
        void write_unElemento_unLog() throws Exception {
            // Arrange
            TimeData item = new TimeData();
            Chunk<TimeData> chunk = Chunk.of(item);

            // Act
            writer.write(chunk);

            // Assert
            assertThat(eventosLog()).hasSize(1);
            ILoggingEvent event = eventosLog().get(0);
            assertThat(event.getLevel()).isEqualTo(Level.INFO);
            assertThat(event.getFormattedMessage()).startsWith("Writing ");
            assertThat(event.getArgumentArray()).containsExactly(item);
        }

        @Test
        @DisplayName("Chunk con N elementos → N logs, uno por item, en orden")
        void write_variosElementos_nLogsEnOrden() throws Exception {
            // Arrange
            TimeData a = new TimeData();
            TimeData b = new TimeData();
            TimeData c = new TimeData();
            Chunk<TimeData> chunk = Chunk.of(a, b, c);

            // Act
            writer.write(chunk);

            // Assert
            assertThat(eventosLog()).hasSize(3);
            assertThat(eventosLog())
                    .extracting(e -> ((ILoggingEvent) e).getArgumentArray()[0])
                    .containsExactly(a, b, c);
        }

        @Test
        @DisplayName("Múltiples llamadas a write acumulan logs")
        void write_multiplesLlamadas_acumulaLogs() throws Exception {
            // Act
            writer.write(Chunk.of(new TimeData()));
            writer.write(Chunk.of(new TimeData(), new TimeData()));
            writer.write(Chunk.of());

            // Assert
            assertThat(eventosLog()).hasSize(3);
        }

        @Test
        @DisplayName("Item null dentro del Chunk → se loguea sin lanzar NPE")
        void write_itemNull_logueaSinNPE() throws Exception {
            // Arrange
            Chunk<TimeData> chunk = Chunk.of((TimeData) null);

            // Act
            writer.write(chunk);

            // Assert
            assertThat(eventosLog()).hasSize(1);
            assertThat(eventosLog().get(0).getFormattedMessage()).startsWith("Writing ");
        }
    }

    // ==================================================================
    // synchronized
    // ==================================================================

    @Nested
    @DisplayName("write es synchronized")
    class SynchronizedTests {

        @Test
        @DisplayName("Concurrentemente no se producen excepciones ni se pierden logs")
        void write_concurrente_noPierdeLogs() throws Exception {
            // Arrange
            int hilos = 4;
            int chunksPorHilo = 25;
            ExecutorService pool = Executors.newFixedThreadPool(hilos);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(hilos);

            // Act
            for (int i = 0; i < hilos; i++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        for (int j = 0; j < chunksPorHilo; j++) {
                            writer.write(Chunk.of(new TimeData()));
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            boolean terminated = done.await(10, TimeUnit.SECONDS);
            pool.shutdownNow();

            // Assert
            assertThat(terminated).isTrue();
            assertThat(eventosLog()).hasSize(hilos * chunksPorHilo);
        }
    }
}