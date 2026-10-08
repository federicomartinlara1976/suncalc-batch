package net.bounceme.chronos.suncalc.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import net.bounceme.chronos.suncalc.dto.JobDTO;
import net.bounceme.chronos.suncalc.model.ExecutionResult;
import net.bounceme.chronos.suncalc.services.JobService;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobListener - enrutado de mensajes RabbitMQ a JobService")
class JobListenerTest {

    private static final String NAME_IMPORT_TIMES = "importTimes";
    private static final String NAME_RECALCULATE_DIFFERENCES = "recalculateDifferences";
    private static final String NAME_IMPORT_BY_MONTH = "importByMonth";
    private static final String NAME_IMPORT_FROM_DATE = "importFromDate";
    private static final String NAME_RECALCULATE_BY_YEAR = "recalculateByYear";

    @Mock private JobService jobService;

    @InjectMocks
    private JobListener listener;

    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(JobListener.class);
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

    /**
     * Construye un JobDTO cuyo content es el Map indicado.
     */
    private JobDTO<?> jobDTO(Map<String, Object> content) {
        JobDTO<Map<String, Object>> dto = new JobDTO<>();
        dto.setContent(content);
        return dto;
    }

    private Map<String, Object> content(String name) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        return m;
    }

    // ==================================================================
    // case "importTimes", "recalculateDifferences"
    // ==================================================================

    @Nested
    @DisplayName("case importTimes / recalculateDifferences")
    class CaseSingleArgumentTests {

        @Test
        @DisplayName("importTimes → jobService.run(name)")
        void executeJob_importTimes_llamaRunConName() {
            // Arrange
            Map<String, Object> c = content(NAME_IMPORT_TIMES);
            ExecutionResult expected = new ExecutionResult();
            when(jobService.run(NAME_IMPORT_TIMES)).thenReturn(expected);

            // Act
            listener.executeJob(jobDTO(c));

            // Assert
            verify(jobService).run(NAME_IMPORT_TIMES);
        }

        @Test
        @DisplayName("recalculateDifferences → jobService.run(name)")
        void executeJob_recalculateDifferences_llamaRunConName() {
            // Arrange
            Map<String, Object> c = content(NAME_RECALCULATE_DIFFERENCES);
            when(jobService.run(NAME_RECALCULATE_DIFFERENCES)).thenReturn(new ExecutionResult());

            // Act
            listener.executeJob(jobDTO(c));

            // Assert
            verify(jobService).run(NAME_RECALCULATE_DIFFERENCES);
        }

        @Test
        @DisplayName("Registra un log INFO con el ExecutionResult devuelto")
        void executeJob_importTimes_logueaResultado() {
            // Arrange
            Map<String, Object> c = content(NAME_IMPORT_TIMES);
            ExecutionResult expected = new ExecutionResult();
            when(jobService.run(NAME_IMPORT_TIMES)).thenReturn(expected);

            // Act
            listener.executeJob(jobDTO(c));

            // Assert
            assertThat(listAppender.list).hasSize(1);
            ILoggingEvent event = listAppender.list.get(0);
            assertThat(event.getLevel()).isEqualTo(Level.INFO);
            // El resultado se loguea como toString (o 'null')
            assertThat(event.getFormattedMessage()).isNotNull();
        }
    }

    // ==================================================================
    // case "importByMonth"
    // ==================================================================

    @Nested
    @DisplayName("case importByMonth")
    class CaseImportByMonthTests {

        @Test
        @DisplayName("Extrae year y month, llama jobService.run(name, year, month)")
        void executeJob_importByMonth_llamaRunConYearYMonth() {
            // Arrange
            Map<String, Object> c = content(NAME_IMPORT_BY_MONTH);
            c.put("year", 2024);
            c.put("month", 6);
            when(jobService.run(NAME_IMPORT_BY_MONTH, 2024, 6)).thenReturn(new ExecutionResult());

            // Act
            listener.executeJob(jobDTO(c));

            // Assert
            verify(jobService).run(NAME_IMPORT_BY_MONTH, 2024, 6);
        }

        @Test
        @DisplayName("year y month ausentes → se pasan null al servicio (sin NPE)")
        void executeJob_importByMonth_sinYearNiMonth_pasaNull() {
            // Arrange
            Map<String, Object> c = content(NAME_IMPORT_BY_MONTH);
            when(jobService.run(eq(NAME_IMPORT_BY_MONTH), any(), any()))
                    .thenReturn(new ExecutionResult());

            // Act / Assert
            assertThatCode(() -> listener.executeJob(jobDTO(c)))
                    .doesNotThrowAnyException();

            verify(jobService).run(NAME_IMPORT_BY_MONTH, null, null);
        }
    }

    // ==================================================================
    // case "importFromDate"
    // ==================================================================

    @Nested
    @DisplayName("case importFromDate")
    class CaseImportFromDateTests {

        @Test
        @DisplayName("Extrae date y llama jobService.run(name, date)")
        void executeJob_importFromDate_llamaRunConDate() {
            // Arrange
            Map<String, Object> c = content(NAME_IMPORT_FROM_DATE);
            c.put("date", "2024-06-15");
            when(jobService.run(NAME_IMPORT_FROM_DATE, "2024-06-15"))
                    .thenReturn(new ExecutionResult());

            // Act
            listener.executeJob(jobDTO(c));

            // Assert
            verify(jobService).run(NAME_IMPORT_FROM_DATE, "2024-06-15");
        }

        @Test
        @DisplayName("date ausente → se pasa null al servicio (sin NPE)")
        void executeJob_importFromDate_sinDate_pasaNull() {
        	ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        	
            // Arrange
            Map<String, Object> c = content(NAME_IMPORT_FROM_DATE);
            when(jobService.run(eq(NAME_IMPORT_FROM_DATE), captor.capture()))
                    .thenReturn(new ExecutionResult());

            // Act / Assert
            assertThatCode(() -> listener.executeJob(jobDTO(c)))
                    .doesNotThrowAnyException();

            verify(jobService).run(NAME_IMPORT_FROM_DATE, (String) null);
        }
    }

    // ==================================================================
    // case "recalculateByYear"
    // ==================================================================

    @Nested
    @DisplayName("case recalculateByYear")
    class CaseRecalculateByYearTests {

        @Test
        @DisplayName("Extrae year y llama jobService.run(name, year)")
        void executeJob_recalculateByYear_llamaRunConYear() {
            // Arrange
            Map<String, Object> c = content(NAME_RECALCULATE_BY_YEAR);
            c.put("year", 2024);
            when(jobService.run(NAME_RECALCULATE_BY_YEAR, 2024)).thenReturn(new ExecutionResult());

            // Act
            listener.executeJob(jobDTO(c));

            // Assert
            verify(jobService).run(NAME_RECALCULATE_BY_YEAR, 2024);
        }

        @Test
        @DisplayName("year ausente → se pasa null al servicio (sin NPE)")
        void executeJob_recalculateByYear_sinYear_pasaNull() {
        	ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        	
            // Arrange
            Map<String, Object> c = content(NAME_RECALCULATE_BY_YEAR);
            lenient().when(jobService.run(eq(NAME_RECALCULATE_BY_YEAR), captor.capture()))
                    .thenReturn(new ExecutionResult());

            // Act / Assert
            assertThatCode(() -> listener.executeJob(jobDTO(c)))
                    .doesNotThrowAnyException();

            verify(jobService).run(NAME_RECALCULATE_BY_YEAR, (Integer) null);
        }
    }

    // ==================================================================
    // default
    // ==================================================================

    @Nested
    @DisplayName("case default")
    class DefaultTests {

        @Test
        @DisplayName("name desconocido → log.warn y NO se invoca jobService")
        void executeJob_nameDesconocido_logueaWarnSinLlamarAlServicio() {
            // Arrange
            Map<String, Object> c = content("tarea-desconocida");

            // Act
            listener.executeJob(jobDTO(c));

            // Assert
            verify(jobService, never()).run(anyString());
            verify(jobService, never()).run(anyString(), any(Integer.class));
            verify(jobService, never()).run(anyString(), any(String.class));
            verify(jobService, never()).run(anyString(), any(Integer.class), any(Integer.class));

            assertThat(listAppender.list)
                    .anyMatch(e -> e.getLevel() == Level.WARN
                            && e.getFormattedMessage().contains("Tarea no especificada"));
        }

        @Test
        @DisplayName("Registra log INFO con 'null' (resultado no asignado)")
        void executeJob_nameDesconocido_logueaNullAlFinal() {
            // Arrange
            Map<String, Object> c = content("tarea-desconocida");

            // Act
            listener.executeJob(jobDTO(c));

            // Assert
            assertThat(listAppender.list)
                    .anyMatch(e -> e.getLevel() == Level.INFO
                            && "null".equals(e.getFormattedMessage()));
        }
    }

    // ==================================================================
    // Casos límite / errores latentes
    // ==================================================================

    @Nested
    @DisplayName("Casos límite")
    class CasosLimiteTests {

        @Test
        @DisplayName("name null → NullPointerException en el switch")
        void executeJob_nameNull_lanzaNPE() {
            // Arrange
            Map<String, Object> c = content(null);

            // Act / Assert
            try {
                listener.executeJob(jobDTO(c));
                throw new AssertionError("Se esperaba NullPointerException");
            } catch (NullPointerException expected) {
                // OK
            }

            verify(jobService, never()).run(anyString());
        }

        @Test
        @DisplayName("content no es Map → ClassCastException")
        void executeJob_contentNoEsMap_lanzaCCE() {
            // Arrange
            JobDTO<String> dto = new JobDTO<>();
            dto.setContent("no soy un Map");

            // Act / Assert
            try {
                listener.executeJob(dto);
                throw new AssertionError("Se esperaba ClassCastException");
            } catch (ClassCastException expected) {
                // OK
            }
        }

        @Test
        @DisplayName("year con tipo distinto → ClassCastException")
        void executeJob_yearTipoIncorrecto_lanzaCCE() {
            // Arrange
            Map<String, Object> c = content(NAME_RECALCULATE_BY_YEAR);
            c.put("year", "2024"); // String en lugar de Integer

            // Act / Assert
            try {
                listener.executeJob(jobDTO(c));
                throw new AssertionError("Se esperaba ClassCastException");
            } catch (ClassCastException expected) {
                // OK
            }
        }
    }

    // ==================================================================
    // Verificación de "siempre loguea el resultado"
    // ==================================================================

    @Nested
    @DisplayName("Log final del resultado")
    class LogFinalTests {

        @Test
        @DisplayName("Tras un case con éxito, hay 1 log INFO con el resultado")
        void executeJob_caseExitoso_unSoloLog() {
            // Arrange
            Map<String, Object> c = content(NAME_IMPORT_TIMES);
            when(jobService.run(NAME_IMPORT_TIMES)).thenReturn(new ExecutionResult());

            // Act
            listener.executeJob(jobDTO(c));

            // Assert
            assertThat(listAppender.list).hasSize(1);
            assertThat(listAppender.list.get(0).getLevel()).isEqualTo(Level.INFO);
        }

        @Test
        @DisplayName("En el default hay 2 logs: WARN 'Tarea no especificada' + INFO 'null'")
        void executeJob_default_dosLogs() {
            // Arrange
            Map<String, Object> c = content("otra");

            // Act
            listener.executeJob(jobDTO(c));

            // Assert
            assertThat(listAppender.list).hasSize(2);
            assertThat(listAppender.list.get(0).getLevel()).isEqualTo(Level.WARN);
            assertThat(listAppender.list.get(1).getLevel()).isEqualTo(Level.INFO);
        }
    }
}