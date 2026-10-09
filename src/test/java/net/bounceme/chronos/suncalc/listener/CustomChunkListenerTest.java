package net.bounceme.chronos.suncalc.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@DisplayName("CustomChunkListener - listener de chunks que solo registra en log")
class CustomChunkListenerTest {

    private CustomChunkListener listener;
    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        listener = new CustomChunkListener();

        logger = (Logger) LoggerFactory.getLogger(CustomChunkListener.class);
        logger.setLevel(Level.DEBUG);

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
    // beforeChunk
    // ==================================================================

    @Nested
    @DisplayName("beforeChunk")
    class BeforeChunkTests {

        @Test
        @DisplayName("Registra 'Before chunk' a nivel DEBUG")
        void beforeChunk_registraLogDebug() {
            // Act
            listener.beforeChunk(null);

            // Assert
            assertThat(listAppender.list).hasSize(1);
            ILoggingEvent event = listAppender.list.get(0);
            assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
            assertThat(event.getFormattedMessage()).isEqualTo("Before chunk");
        }

        @Test
        @DisplayName("No depende del ChunkContext (acepta null)")
        void beforeChunk_noDependeDelContext() {
            // Act / Assert
            assertThatCode(() -> listener.beforeChunk(null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Cada llamada emite un nuevo log")
        void beforeChunk_multiplesLlamadas_multiplesLogs() {
            // Act
            listener.beforeChunk(null);
            listener.beforeChunk(null);
            listener.beforeChunk(null);

            // Assert
            assertThat(listAppender.list).hasSize(3);
            assertThat(listAppender.list)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .containsOnly("Before chunk");
        }
    }

    // ==================================================================
    // afterChunk
    // ==================================================================

    @Nested
    @DisplayName("afterChunk")
    class AfterChunkTests {

        @Test
        @DisplayName("Registra 'After chunk' a nivel DEBUG")
        void afterChunk_registraLogDebug() {
            // Act
            listener.afterChunk(null);

            // Assert
            assertThat(listAppender.list).hasSize(1);
            ILoggingEvent event = listAppender.list.get(0);
            assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
            assertThat(event.getFormattedMessage()).isEqualTo("After chunk");
        }

        @Test
        @DisplayName("No depende del ChunkContext (acepta null)")
        void afterChunk_noDependeDelContext() {
            // Act / Assert
            assertThatCode(() -> listener.afterChunk(null))
                    .doesNotThrowAnyException();
        }
    }

    // ==================================================================
    // afterChunkError
    // ==================================================================

    @Nested
    @DisplayName("afterChunkError")
    class AfterChunkErrorTests {

        @Test
        @DisplayName("Registra 'After chunk: error' a nivel DEBUG")
        void afterChunkError_registraLogDebug() {
            // Act
            listener.afterChunkError(null);

            // Assert
            assertThat(listAppender.list).hasSize(1);
            ILoggingEvent event = listAppender.list.get(0);
            assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
            assertThat(event.getFormattedMessage()).isEqualTo("After chunk: error");
        }

        @Test
        @DisplayName("No depende del ChunkContext (acepta null)")
        void afterChunkError_noDependeDelContext() {
            // Act / Assert
            assertThatCode(() -> listener.afterChunkError(null))
                    .doesNotThrowAnyException();
        }
    }

    // ==================================================================
    // Comportamiento conjunto
    // ==================================================================

    @Nested
    @DisplayName("Comportamiento conjunto")
    class ConjuntoTests {

        @Test
        @DisplayName("Los 3 métodos producen mensajes distintos")
        void tresMetodos_mensajesDistintos() {
            // Act
            listener.beforeChunk(null);
            listener.afterChunk(null);
            listener.afterChunkError(null);

            // Assert
            assertThat(listAppender.list)
                    .hasSize(3)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .containsExactly("Before chunk", "After chunk", "After chunk: error");
        }

        @Test
        @DisplayName("Todos los logs están a nivel DEBUG, ninguno a INFO o superior")
        void tresMetodos_todosDebug() {
            // Act
            listener.beforeChunk(null);
            listener.afterChunk(null);
            listener.afterChunkError(null);

            // Assert
            assertThat(listAppender.list)
                    .allSatisfy(event -> assertThat(event.getLevel()).isEqualTo(Level.DEBUG));
        }
    }
}