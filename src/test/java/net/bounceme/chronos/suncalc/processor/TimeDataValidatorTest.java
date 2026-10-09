package net.bounceme.chronos.suncalc.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

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
import org.springframework.batch.item.validator.ValidationException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import net.bounceme.chronos.suncalc.model.TimeData;
import net.bounceme.chronos.suncalc.validation.ValidatorService;

@ExtendWith(MockitoExtension.class)
@DisplayName("TimeDataValidator - validación de TimeData con envoltura de excepción")
class TimeDataValidatorTest {

    private static final String FORMATTED_DATE = "2024-06-15";

    @Mock private SimpleDateFormat dateFormat;
    @Mock private ValidatorService<TimeData> validatorService;

    @InjectMocks
    private TimeDataValidator validator;

    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(TimeDataValidator.class);
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

    private TimeData timeDataWithDate(Date fecha) {
        TimeData td = new TimeData();
        td.setFecha(fecha);
        return td;
    }

    @SuppressWarnings("unchecked")
    private ConstraintViolation<TimeData> violationWithMessage(String message) {
        ConstraintViolation<TimeData> v = mock(ConstraintViolation.class);
        when(v.getMessage()).thenReturn(message);
        return v;
    }

    // ==================================================================
    // Camino feliz: no hay excepción
    // ==================================================================

    @Nested
    @DisplayName("Camino feliz")
    class CaminoFelizTests {

        @Test
        @DisplayName("validatorService.validate OK → no lanza y no toca dateFormat")
        void validate_sinExcepcion_noLanza() throws Exception {
            // Arrange
            TimeData value = new TimeData();
            doNothing().when(validatorService).validate(value);

            // Act
            validator.validate(value);

            // Assert
            verify(validatorService).validate(value);
            verify(dateFormat, never()).format(any(Date.class));
            assertThat(listAppender.list).isEmpty();
        }

        @Test
        @DisplayName("Se delega en validatorService con el mismo objeto")
        void validate_delegaEnValidatorService() throws Exception {
            // Arrange
            TimeData value = new TimeData();

            // Act
            validator.validate(value);

            // Assert
            verify(validatorService).validate(value);
        }
    }

    // ==================================================================
    // Rama de error: ConstraintViolationException
    // ==================================================================

    @Nested
    @DisplayName("ConstraintViolationException")
    class ConstraintViolationTests {

        @Test
        @DisplayName("Con 1 violación → lanza ValidationException con mensaje formateado y loguea 1 vez")
        void validate_unaViolacion_lanzaYLogea() throws Exception {
            // Arrange
            Date fecha = new Date();
            TimeData value = timeDataWithDate(fecha);

            ConstraintViolation<TimeData> v = violationWithMessage("campo inválido");
            ConstraintViolationException ex = new ConstraintViolationException(Set.of(v));
            doThrow(ex).when(validatorService).validate(value);
            when(dateFormat.format(fecha)).thenReturn(FORMATTED_DATE);

            // Act / Assert
            assertThatThrownBy(() -> validator.validate(value))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("El time data para la fecha [2024-06-15] no se va a procesar");

            // Log: 1 entrada con el mensaje de la violación
            assertThat(listAppender.list).hasSize(1);
            assertThat(listAppender.list.get(0).getFormattedMessage()).isEqualTo("campo inválido");
            assertThat(listAppender.list.get(0).getLevel()).isEqualTo(Level.ERROR);

            // dateFormat solo 1 vez
            verify(dateFormat, times(1)).format(fecha);
        }

        @Test
        @DisplayName("Con N violaciones → loguea N veces, pero dateFormat 1 sola vez")
        void validate_variasViolaciones_logueaTodasYFormateaUnaVez() throws Exception {
            // Arrange
            Date fecha = new Date();
            TimeData value = timeDataWithDate(fecha);

            ConstraintViolation<TimeData> v1 = violationWithMessage("error 1");
            ConstraintViolation<TimeData> v2 = violationWithMessage("error 2");
            ConstraintViolation<TimeData> v3 = violationWithMessage("error 3");
            ConstraintViolationException ex = new ConstraintViolationException(Set.of(v1, v2, v3));
            doThrow(ex).when(validatorService).validate(value);
            when(dateFormat.format(fecha)).thenReturn(FORMATTED_DATE);

            // Act / Assert
            assertThatThrownBy(() -> validator.validate(value))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("[2024-06-15]");

            assertThat(listAppender.list)
                    .hasSize(3)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .containsExactlyInAnyOrder("error 1", "error 2", "error 3");

            verify(dateFormat, times(1)).format(fecha);
        }

        @Test
        @DisplayName("Con 0 violaciones → igualmente lanza ValidationException")
        void validate_sinViolaciones_lanzaIgual() throws Exception {
            // Arrange
            Date fecha = new Date();
            TimeData value = timeDataWithDate(fecha);

            ConstraintViolationException ex = new ConstraintViolationException(Set.of());
            doThrow(ex).when(validatorService).validate(value);
            when(dateFormat.format(fecha)).thenReturn(FORMATTED_DATE);

            // Act / Assert
            assertThatThrownBy(() -> validator.validate(value))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("El time data para la fecha [2024-06-15] no se va a procesar");

            assertThat(listAppender.list).isEmpty();
            verify(dateFormat, times(1)).format(fecha);
        }

        @Test
        @DisplayName("El mensaje de ValidationException incluye la fecha formateada")
        void validate_mensajeIncluyeFechaFormateada() throws Exception {
            // Arrange
            Date fecha = new Date();
            TimeData value = timeDataWithDate(fecha);

            ConstraintViolationException ex = new ConstraintViolationException(
                    Set.of(violationWithMessage("x")));
            doThrow(ex).when(validatorService).validate(value);
            when(dateFormat.format(fecha)).thenReturn("2025-12-31");

            // Act / Assert
            assertThatThrownBy(() -> validator.validate(value))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("El time data para la fecha [2025-12-31] no se va a procesar");
        }

        @Test
        @DisplayName("Si el mensaje de la violación es null, el log no falla")
        void validate_violacionConMensajeNull_logueaSinFallar() throws Exception {
            // Arrange
            Date fecha = new Date();
            TimeData value = timeDataWithDate(fecha);

            ConstraintViolation<TimeData> v = violationWithMessage(null);
            ConstraintViolationException ex = new ConstraintViolationException(Set.of(v));
            doThrow(ex).when(validatorService).validate(value);
            when(dateFormat.format(fecha)).thenReturn(FORMATTED_DATE);

            // Act / Assert
            assertThatThrownBy(() -> validator.validate(value))
                    .isInstanceOf(ValidationException.class);

            assertThat(listAppender.list).hasSize(1);
        }
    }

    // ==================================================================
    // Casos límite
    // ==================================================================

    @Nested
    @DisplayName("Casos límite")
    class CasosLimiteTests {

        @Test
        @DisplayName("value null y validatorService no lanza → retorna sin tocar dateFormat")
        void validate_valueNullSinExcepcion_retorna() throws Exception {
            // Arrange
            doNothing().when(validatorService).validate(null);

            // Act
            validator.validate(null);

            // Assert
            verify(validatorService).validate(null);
            verify(dateFormat, never()).format(any(Date.class));
        }

        @Test
        @DisplayName("value null + ConstraintViolationException → NullPointerException al formatear")
        void validate_valueNullConExcepcion_lanzaNPE() throws Exception {
            // Arrange
            ConstraintViolationException ex = new ConstraintViolationException(
                    Set.of(violationWithMessage("x")));
            doThrow(ex).when(validatorService).validate(null);

            // Act / Assert
            assertThatThrownBy(() -> validator.validate(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("ValidationException del servicio se propaga sin envolver")
        void validate_servicioLanzaValidationException_sePropaga() throws Exception {
            // Arrange
            TimeData value = new TimeData();
            ValidationException boom = new ValidationException("ya es una ValidationException");
            doThrow(boom).when(validatorService).validate(value);

            // Act / Assert
            assertThatThrownBy(() -> validator.validate(value))
                    .isSameAs(boom);

            // No se ha tocado dateFormat (no es ConstraintViolationException)
            verify(dateFormat, never()).format(any(Date.class));
        }

        @Test
        @DisplayName("RuntimeException del servicio se propaga sin envolver")
        void validate_servicioLanzaRuntime_sePropaga() throws Exception {
            // Arrange
            TimeData value = new TimeData();
            RuntimeException boom = new IllegalStateException("boom");
            doThrow(boom).when(validatorService).validate(value);

            // Act / Assert
            assertThatThrownBy(() -> validator.validate(value))
                    .isSameAs(boom);
        }
    }
}