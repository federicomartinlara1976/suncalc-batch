package net.bounceme.chronos.suncalc.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.file.FlatFileParseException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@DisplayName("CustomSkipListener - registro de errores en skip de read/write/process")
class CustomSkipListenerTest {

    private CustomSkipListener listener;
    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        listener = new CustomSkipListener();

        logger = (Logger) LoggerFactory.getLogger(CustomSkipListener.class);
        logger.setLevel(Level.ERROR);

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

    private String lastLoggedMessage() {
        assertThat(listAppender.list).isNotEmpty();
        return listAppender.list.get(listAppender.list.size() - 1).getFormattedMessage();
    }

    // ==================================================================
    // onSkipInRead
    // ==================================================================

    @Nested
    @DisplayName("onSkipInRead")
    class OnSkipInReadTests {

        @Test
        @DisplayName("FlatFileParseException → mensaje con línea y entrada")
        void onSkipInRead_flatFileParseException_mensajeConLineaYEntrada() {
            // Arrange
            FlatFileParseException ex = new FlatFileParseException(
                    "Error de parseo", "linea con formato incorrecto", 42);

            // Act
            listener.onSkipInRead(ex);

            // Assert
            assertThat(listAppender.list).hasSize(1);
            ILoggingEvent event = listAppender.list.get(0);
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(event.getFormattedMessage())
                    .isEqualTo("ERROR en LECTURA: Linea 42: Error de formato para la siguiente entrada: linea con formato incorrecto");
        }

        @Test
        @DisplayName("Excepción no FlatFileParseException → mensaje con getMessage()")
        void onSkipInRead_otraExcepcion_mensajeConGetMessage() {
            // Arrange
            RuntimeException ex = new RuntimeException("algo ha fallado leyendo");

            // Act
            listener.onSkipInRead(ex);

            // Assert
            assertThat(listAppender.list).hasSize(1);
            ILoggingEvent event = listAppender.list.get(0);
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(event.getFormattedMessage())
                    .isEqualTo("ERROR en LECTURA: algo ha fallado leyendo");
        }

        @Test
        @DisplayName("Excepción no FlatFileParseException con getMessage() null → 'null' literal sin NPE")
        void onSkipInRead_getMessageNull_noLanza() {
            // Arrange
            RuntimeException ex = new RuntimeException((String) null);

            // Act
            listener.onSkipInRead(ex);

            // Assert
            assertThat(lastLoggedMessage()).isEqualTo("ERROR en LECTURA: null");
        }

        @Test
        @DisplayName("Throwable null → NullPointerException en t.getMessage()")
        void onSkipInRead_null_lanzaNPE() {
            // Act / Assert
            assertThatCode(() -> listener.onSkipInRead(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("FlatFileParseException con input vacío → mensaje con hueco final")
        void onSkipInRead_flatFileParseConInputVacio_mensajeConHuecoFinal() {
            // Arrange
            FlatFileParseException ex = new FlatFileParseException(
                    "Error", "", 1);

            // Act
            listener.onSkipInRead(ex);

            // Assert
            assertThat(lastLoggedMessage())
                    .isEqualTo("ERROR en LECTURA: Linea 1: Error de formato para la siguiente entrada: ");
        }
    }

    // ==================================================================
    // onSkipInWrite
    // ==================================================================

    @Nested
    @DisplayName("onSkipInWrite")
    class OnSkipInWriteTests {

        @Test
        @DisplayName("Registra 'ERROR en ESCRITURA: ' + mensaje de la excepción")
        void onSkipInWrite_registraLog() {
            // Arrange
            RuntimeException ex = new RuntimeException("no se pudo escribir");

            // Act
            listener.onSkipInWrite("item-irrelevante", ex);

            // Assert
            assertThat(listAppender.list).hasSize(1);
            ILoggingEvent event = listAppender.list.get(0);
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(event.getFormattedMessage())
                    .isEqualTo("ERROR en ESCRITURA: no se pudo escribir");
        }

        @Test
        @DisplayName("item null no importa: el parámetro no se usa")
        void onSkipInWrite_itemNull_noImporta() {
            // Arrange
            RuntimeException ex = new RuntimeException("error");

            // Act / Assert
            assertThatCode(() -> listener.onSkipInWrite(null, ex))
                    .doesNotThrowAnyException();
            assertThat(lastLoggedMessage()).isEqualTo("ERROR en ESCRITURA: error");
        }

        @Test
        @DisplayName("Excepción con getMessage() null → 'null' literal sin NPE")
        void onSkipInWrite_getMessageNull_noLanza() {
            // Arrange
            RuntimeException ex = new RuntimeException((String) null);

            // Act
            listener.onSkipInWrite("item", ex);

            // Assert
            assertThat(lastLoggedMessage()).isEqualTo("ERROR en ESCRITURA: null");
        }

        @Test
        @DisplayName("Throwable null → NullPointerException en t.getMessage()")
        void onSkipInWrite_null_lanzaNPE() {
            // Act / Assert
            assertThatCode(() -> listener.onSkipInWrite("item", null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // ==================================================================
    // onSkipInProcess
    // ==================================================================

    @Nested
    @DisplayName("onSkipInProcess")
    class OnSkipInProcessTests {

        @Test
        @DisplayName("Registra 'ERROR en PROCESADO: ' + mensaje de la excepción")
        void onSkipInProcess_registraLog() {
            // Arrange
            RuntimeException ex = new RuntimeException("no se pudo procesar");

            // Act
            listener.onSkipInProcess("item-irrelevante", ex);

            // Assert
            assertThat(listAppender.list).hasSize(1);
            ILoggingEvent event = listAppender.list.get(0);
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(event.getFormattedMessage())
                    .isEqualTo("ERROR en PROCESADO: no se pudo procesar");
        }

        @Test
        @DisplayName("item null no importa: el parámetro no se usa")
        void onSkipInProcess_itemNull_noImporta() {
            // Arrange
            RuntimeException ex = new RuntimeException("error");

            // Act / Assert
            assertThatCode(() -> listener.onSkipInProcess(null, ex))
                    .doesNotThrowAnyException();
            assertThat(lastLoggedMessage()).isEqualTo("ERROR en PROCESADO: error");
        }

        @Test
        @DisplayName("Excepción con getMessage() null → 'null' literal sin NPE")
        void onSkipInProcess_getMessageNull_noLanza() {
            // Arrange
            RuntimeException ex = new RuntimeException((String) null);

            // Act
            listener.onSkipInProcess("item", ex);

            // Assert
            assertThat(lastLoggedMessage()).isEqualTo("ERROR en PROCESADO: null");
        }

        @Test
        @DisplayName("Throwable null → NullPointerException en t.getMessage()")
        void onSkipInProcess_null_lanzaNPE() {
            // Act / Assert
            assertThatCode(() -> listener.onSkipInProcess("item", null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // ==================================================================
    // Comportamiento conjunto
    // ==================================================================

    @Nested
    @DisplayName("Comportamiento conjunto")
    class ConjuntoTests {

        @Test
        @DisplayName("Los 3 métodos emiten logs independientes en orden de invocación")
        void tresMetodos_logsIndependientes() {
            // Act
            listener.onSkipInRead(new RuntimeException("r"));
            listener.onSkipInWrite("i", new RuntimeException("w"));
            listener.onSkipInProcess("i", new RuntimeException("p"));

            // Assert
            assertThat(listAppender.list)
                    .hasSize(3)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .containsExactly(
                            "ERROR en LECTURA: r",
                            "ERROR en ESCRITURA: w",
                            "ERROR en PROCESADO: p");
        }

        @Test
        @DisplayName("Todos los logs van a nivel ERROR")
        void tresMetodos_todosError() {
            // Act
            listener.onSkipInRead(new RuntimeException("r"));
            listener.onSkipInWrite("i", new RuntimeException("w"));
            listener.onSkipInProcess("i", new RuntimeException("p"));

            // Assert
            assertThat(listAppender.list)
                    .allSatisfy(event -> assertThat(event.getLevel()).isEqualTo(Level.ERROR));
        }
    }
}