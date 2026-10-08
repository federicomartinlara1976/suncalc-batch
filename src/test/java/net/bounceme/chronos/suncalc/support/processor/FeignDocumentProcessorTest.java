package net.bounceme.chronos.suncalc.support.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import net.bounceme.chronos.suncalc.clients.SunriseSunsetRest;
import net.bounceme.chronos.suncalc.model.SunriseSunsetResponse;
import net.bounceme.chronos.suncalc.model.SunriseSunsetResult;
import net.bounceme.chronos.suncalc.model.TimeData;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeignDocumentProcessor - enriquecimiento de TimeData con datos REST")
class FeignDocumentProcessorTest {

    private static final Float LAT = 40.4168f;
    private static final Float LNG = -3.7038f;
    private static final String TODAY = "today";
    private static final String OTHER_DATE = "2024-06-15";

    @Mock private SunriseSunsetRest sunriseSunsetRest;
    @Mock private SimpleDateFormat dateFormat;

    @InjectMocks
    private FeignDocumentProcessor processor;

    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(processor, "lat", LAT);
        ReflectionTestUtils.setField(processor, "lng", LNG);

        logger = (Logger) LoggerFactory.getLogger(FeignDocumentProcessor.class);
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

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Construye una respuesta con status y 5 propiedades ISO 8601.
     * Si status != "OK", los campos de Results no se leen.
     */
    private SunriseSunsetResponse response(String status, boolean withResults) {
        SunriseSunsetResponse r = new SunriseSunsetResponse();
        r.setStatus(status);
        if (withResults) {
            SunriseSunsetResult results = new SunriseSunsetResult();
            results.setCivil_twilight_begin("2024-06-15T04:30:00+02:00");
            results.setSunrise("2024-06-15T06:45:00+02:00");
            results.setSolar_noon("2024-06-15T14:00:00+02:00");
            results.setSunset("2024-06-15T21:15:00+02:00");
            results.setCivil_twilight_end("2024-06-15T23:30:00+02:00");
            r.setResults(results);
        }
        return r;
    }

    private void stubRestReturns(SunriseSunsetResponse response) {
        when(sunriseSunsetRest.detalle(any(), any(), any(), any(), anyInt(), any()))
                .thenReturn(response);
    }

    // ==================================================================
    // process() → delega en obtainData("today")
    // ==================================================================

    @Nested
    @DisplayName("process() sin argumentos")
    class ProcessSinFechaTests {

        @Test
        @DisplayName("Pasa 'today' al REST")
        void process_pasaTodayAlRest() throws Exception {
            // Arrange
            stubRestReturns(response("OK", true));

            // Act
            processor.process();

            // Assert
            verify(sunriseSunsetRest).detalle(
                    eq(LAT), eq(LNG), eq(TODAY), eq("Europe/Madrid"), eq(0), isNull());
        }

        @Test
        @DisplayName("Con 'today' NO se llama a dateFormat.parse (usa new Date)")
        void process_todayNoParseaFecha() throws Exception {
            // Arrange
            stubRestReturns(response("OK", true));

            // Act
            processor.process();

            // Assert
            verify(dateFormat, never()).parse(any(String.class));
        }

        @Test
        @DisplayName("La fecha del TimeData es 'ahora' (no la del mock dateFormat)")
        void process_today_fechaEsAhora() {
            // Arrange
            stubRestReturns(response("OK", true));
            long before = System.currentTimeMillis();

            // Act
            TimeData result = processor.process();

            // Assert
            long after = System.currentTimeMillis();
            assertThat(result.getFecha().getTime())
                    .isBetween(before, after);
        }
    }

    // ==================================================================
    // process(String) → delega en obtainData(fecha)
    // ==================================================================

    @Nested
    @DisplayName("process(String)")
    class ProcessConFechaTests {

        @Test
        @DisplayName("Pasa la fecha recibida al REST")
        void process_conFecha_pasaFechaAlRest() throws Exception {
            // Arrange
            stubRestReturns(response("OK", true));
            when(dateFormat.parse(OTHER_DATE)).thenReturn(new Date());

            // Act
            processor.process(OTHER_DATE);

            // Assert
            verify(sunriseSunsetRest).detalle(
                    eq(LAT), eq(LNG), eq(OTHER_DATE), eq("Europe/Madrid"), eq(0), isNull());
        }

        @Test
        @DisplayName("Con fecha distinta a 'today' SÍ se llama a dateFormat.parse")
        void process_conFecha_parseaFecha() throws Exception {
            // Arrange
            Date parsed = new Date(1718409600000L);
            stubRestReturns(response("OK", true));
            when(dateFormat.parse(OTHER_DATE)).thenReturn(parsed);

            // Act
            TimeData result = processor.process(OTHER_DATE);

            // Assert
            verify(dateFormat).parse(OTHER_DATE);
            assertThat(result.getFecha()).isSameAs(parsed);
        }

        @Test
        @DisplayName("ParseException de dateFormat.parse → se propaga")
        void process_parseFalla_propaga() throws Exception {
            // Arrange
            when(dateFormat.parse(OTHER_DATE))
                    .thenThrow(new ParseException("formato inválido", 0));

            // Act / Assert
            assertThatThrownBy(() -> processor.process(OTHER_DATE))
                    .isInstanceOf(ParseException.class)
                    .hasMessageContaining("formato inválido");

            verify(sunriseSunsetRest, never()).detalle(any(), any(), any(), any(), anyInt(), any());
        }
    }

    // ==================================================================
    // Respuesta OK: rellena los 5 campos
    // ==================================================================

    @Nested
    @DisplayName("Respuesta OK")
    class RespuestaOkTests {

        @Test
        @DisplayName("Setea los 5 campos y status=true")
        void process_statusOk_rellenaTodos() throws Exception {
            // Arrange
            stubRestReturns(response("OK", true));
            when(dateFormat.parse(OTHER_DATE)).thenReturn(new Date());

            // Act
            TimeData result = processor.process(OTHER_DATE);

            // Assert
            assertThat(result.getStatus()).isTrue();
            assertThat(result.getDawn()).isNotNull();
            assertThat(result.getSunrise()).isNotNull();
            assertThat(result.getCulmination()).isNotNull();
            assertThat(result.getSunset()).isNotNull();
            assertThat(result.getDusk()).isNotNull();
        }

        @Test
        @DisplayName("Los 5 campos se derivan de las 5 propiedades de Results")
        void process_statusOk_mapeaPropiedades() throws Exception {
            // Arrange
            stubRestReturns(response("OK", true));
            when(dateFormat.parse(OTHER_DATE)).thenReturn(new Date());

            // Act
            TimeData result = processor.process(OTHER_DATE);

            // Assert: misma instancia temporal que el ISO de origen
            assertThat(result.getDawn())
                    .isEqualTo(Date.from(OffsetDateTime.parse("2024-06-15T04:30:00+02:00").toInstant()));
            assertThat(result.getSunrise())
                    .isEqualTo(Date.from(OffsetDateTime.parse("2024-06-15T06:45:00+02:00").toInstant()));
            assertThat(result.getCulmination())
                    .isEqualTo(Date.from(OffsetDateTime.parse("2024-06-15T14:00:00+02:00").toInstant()));
            assertThat(result.getSunset())
                    .isEqualTo(Date.from(OffsetDateTime.parse("2024-06-15T21:15:00+02:00").toInstant()));
            assertThat(result.getDusk())
                    .isEqualTo(Date.from(OffsetDateTime.parse("2024-06-15T23:30:00+02:00").toInstant()));
        }

        @Test
        @DisplayName("Registra log DEBUG con los 5 campos")
        void process_statusOk_logueaDebug() throws Exception {
            // Arrange
            stubRestReturns(response("OK", true));
            when(dateFormat.parse(OTHER_DATE)).thenReturn(new Date());

            // Act
            processor.process(OTHER_DATE);

            // Assert
            assertThat(listAppender.list)
                    .anyMatch(e -> e.getLevel() == Level.DEBUG
                            && e.getFormattedMessage().contains("Returned data"));
        }
    }

    // ==================================================================
    // Respuesta NO OK: solo status=false
    // ==================================================================

    @Nested
    @DisplayName("Respuesta NO OK")
    class RespuestaNoOkTests {

        @Test
        @DisplayName("status != 'OK' → no rellena los 5 campos, status=false")
        void process_statusNoOk_noRellena() throws Exception {
            // Arrange
            stubRestReturns(response("INVALID_REQUEST", false));
            when(dateFormat.parse(OTHER_DATE)).thenReturn(new Date());

            // Act
            TimeData result = processor.process(OTHER_DATE);

            // Assert
            assertThat(result.getStatus()).isFalse();
            assertThat(result.getDawn()).isNull();
            assertThat(result.getSunrise()).isNull();
            assertThat(result.getCulmination()).isNull();
            assertThat(result.getSunset()).isNull();
            assertThat(result.getDusk()).isNull();
        }

        @Test
        @DisplayName("Siempre se setea la fecha, aunque status no sea OK")
        void process_statusNoOk_fechaSiempreSeteada() throws Exception {
            // Arrange
            Date parsed = new Date(1718409600000L);
            stubRestReturns(response("INVALID_REQUEST", false));
            when(dateFormat.parse(OTHER_DATE)).thenReturn(parsed);

            // Act
            TimeData result = processor.process(OTHER_DATE);

            // Assert
            assertThat(result.getFecha()).isSameAs(parsed);
        }

        @Test
        @DisplayName("status null (response.getStatus() null) → 'OK'.equals(null) es false")
        void process_statusNull_statusFalse() throws Exception {
            // Arrange
            stubRestReturns(response(null, false));
            when(dateFormat.parse(OTHER_DATE)).thenReturn(new Date());

            // Act
            TimeData result = processor.process(OTHER_DATE);

            // Assert
            assertThat(result.getStatus()).isFalse();
        }

        @Test
        @DisplayName("NO se registra log DEBUG en rama no OK")
        void process_statusNoOk_sinLog() throws Exception {
            // Arrange
            stubRestReturns(response("INVALID_REQUEST", false));
            when(dateFormat.parse(OTHER_DATE)).thenReturn(new Date());

            // Act
            processor.process(OTHER_DATE);

            // Assert
            assertThat(listAppender.list)
                    .noneMatch(e -> e.getFormattedMessage().contains("Returned data"));
        }
    }

    // ==================================================================
    // Casos límite
    // ==================================================================

    @Nested
    @DisplayName("Casos límite")
    class CasosLimiteTests {

        @Test
        @DisplayName("response null → NullPointerException en getStatus()")
        void process_responseNull_lanzaNPE() throws Exception {
            // Arrange
            when(sunriseSunsetRest.detalle(any(), any(), any(), any(), anyInt(), any()))
                    .thenReturn(null);
            when(dateFormat.parse(OTHER_DATE)).thenReturn(new Date());

            // Act / Assert
            assertThatThrownBy(() -> processor.process(OTHER_DATE))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Status OK pero Results null → NullPointerException al parsear")
        void process_resultsNull_lanzaNPE() throws Exception {
            // Arrange
            SunriseSunsetResponse r = new SunriseSunsetResponse();
            r.setStatus("OK");
            // Results no se inicializa → null
            stubRestReturns(r);
            when(dateFormat.parse(OTHER_DATE)).thenReturn(new Date());

            // Act / Assert
            assertThatThrownBy(() -> processor.process(OTHER_DATE))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Propiedad ISO inválida → DateTimeParseException")
        void process_isoInvalido_lanzaDateTimeParse() throws Exception {
            // Arrange
            SunriseSunsetResponse r = new SunriseSunsetResponse();
            r.setStatus("OK");
            SunriseSunsetResult results = new SunriseSunsetResult();
            results.setCivil_twilight_begin("no-es-una-fecha-iso");
            results.setSunrise("2024-06-15T06:45:00+02:00");
            results.setSolar_noon("2024-06-15T14:00:00+02:00");
            results.setSunset("2024-06-15T21:15:00+02:00");
            results.setCivil_twilight_end("2024-06-15T23:30:00+02:00");
            r.setResults(results);
            stubRestReturns(r);
            when(dateFormat.parse(OTHER_DATE)).thenReturn(new Date());

            // Act / Assert
            assertThatThrownBy(() -> processor.process(OTHER_DATE))
                    .isInstanceOf(java.time.format.DateTimeParseException.class);
        }
    }
}