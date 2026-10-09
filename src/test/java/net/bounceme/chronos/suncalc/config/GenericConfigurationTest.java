package net.bounceme.chronos.suncalc.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.bounceme.chronos.suncalc.model.TimeData;
import net.bounceme.chronos.suncalc.validation.ValidatorService;
import net.bounceme.chronos.suncalc.validation.impl.ValidatorServiceImpl;
import net.bounceme.chronos.utils.calc.converters.Converter;

@DisplayName("GenericConfiguration - beans y convertidores de la aplicación")
class GenericConfigurationTest {

    private final GenericConfiguration config = new GenericConfiguration();

    // ==================================================================
    // objectMapper
    // ==================================================================

    @Nested
    @DisplayName("objectMapper()")
    class ObjectMapperTests {

        @Test
        @DisplayName("Devuelve una instancia no nula de ObjectMapper")
        void objectMapper_devuelveInstancia() {
            ObjectMapper mapper = config.objectMapper();

            assertThat(mapper).isNotNull();
        }

        @Test
        @DisplayName("Cada invocación devuelve una instancia nueva (prototype)")
        void objectMapper_prototype() {
            assertThat(config.objectMapper()).isNotSameAs(config.objectMapper());
        }

        @Test
        @DisplayName("El ObjectMapper es funcional (serializa un Map simple)")
        void objectMapper_funcional() throws Exception {
            ObjectMapper mapper = config.objectMapper();

            String json = mapper.writeValueAsString(java.util.Map.of("k", "v"));

            assertThat(json).isEqualTo("{\"k\":\"v\"}");
        }
    }

    // ==================================================================
    // SimpleDateFormat beans
    // ==================================================================

    @Nested
    @DisplayName("Beans SimpleDateFormat")
    class SimpleDateFormatsTests {

        @Test
        @DisplayName("dateFormat() usa patrón 'yyyy-MM-dd'")
        void dateFormat_patron() {
            SimpleDateFormat f = config.dateFormat();

            assertThat(f).isNotNull();
            assertThat(f.toPattern()).isEqualTo("yyyy-MM-dd");
        }

        @Test
        @DisplayName("timeFormat() usa patrón 'hh:mm:ss'")
        void timeFormat_patron() {
            SimpleDateFormat f = config.timeFormat();

            assertThat(f).isNotNull();
            assertThat(f.toPattern()).isEqualTo("hh:mm:ss");
        }

        @Test
        @DisplayName("dateTimeFormat() usa patrón 'yyyy-MM-dd hh:mm:ss'")
        void dateTimeFormat_patron() {
            SimpleDateFormat f = config.dateTimeFormat();

            assertThat(f).isNotNull();
            assertThat(f.toPattern()).isEqualTo("yyyy-MM-dd hh:mm:ss");
        }

        @Test
        @DisplayName("dateFormat() formatea correctamente una fecha conocida")
        void dateFormat_formatoCorrecto() {
            SimpleDateFormat f = config.dateFormat();
            Date date = Date.from(java.time.Instant.parse("2024-06-15T12:00:00Z"));

            // El resultado depende de la zona del sistema, pero el patrón debe coincidir
            assertThat(f.format(date)).matches("\\d{4}-\\d{2}-\\d{2}");
        }
    }

    // ==================================================================
    // timeDataValidatorService
    // ==================================================================

    @Nested
    @DisplayName("timeDataValidatorService()")
    class TimeDataValidatorServiceTests {

        @Test
        @DisplayName("Devuelve una instancia de ValidatorServiceImpl")
        void timeDataValidatorService_tipo() {
            ValidatorService<TimeData> service = config.timeDataValidatorService();

            assertThat(service).isInstanceOf(ValidatorServiceImpl.class);
        }

        @Test
        @DisplayName("Cada invocación devuelve una instancia nueva (prototype)")
        void timeDataValidatorService_prototype() {
            assertThat(config.timeDataValidatorService())
                    .isNotSameAs(config.timeDataValidatorService());
        }
    }

    // ==================================================================
    // dateConverter
    // ==================================================================

    @Nested
    @DisplayName("dateConverter()")
    class DateConverterTests {

        private final Converter<BigDecimal[], Date> converter = config.dateConverter();

        @Test
        @DisplayName("Convierte [2024, 6, 15, 14, 30, 0.0] correctamente")
        void dateConverter_mediaTarde() {
            Date result = converter.apply(new BigDecimal[]{
                    BigDecimal.valueOf(2024),
                    BigDecimal.valueOf(6),
                    BigDecimal.valueOf(15),
                    BigDecimal.valueOf(14),
                    BigDecimal.valueOf(30),
                    BigDecimal.valueOf(0.0)
            });

            LocalDateTime ldt = toLocalDateTime(result);
            assertThat(ldt.getYear()).isEqualTo(2024);
            assertThat(ldt.getMonthValue()).isEqualTo(6);
            assertThat(ldt.getDayOfMonth()).isEqualTo(15);
            assertThat(ldt.getHour()).isEqualTo(14);
            assertThat(ldt.getMinute()).isEqualTo(30);
            assertThat(ldt.getSecond()).isZero();
            assertThat(ldt.getNano()).isZero();
        }

        @Test
        @DisplayName("Mes 1 (enero) → monthValue == 1 (no hay off-by-one)")
        void dateConverter_mesEnero() {
            Date result = converter.apply(new BigDecimal[]{
                    BigDecimal.valueOf(2024),
                    BigDecimal.valueOf(1),
                    BigDecimal.valueOf(15),
                    BigDecimal.valueOf(0),
                    BigDecimal.valueOf(0),
                    BigDecimal.valueOf(0)
            });

            assertThat(toLocalDateTime(result).getMonthValue()).isEqualTo(1);
        }

        @Test
        @DisplayName("Mes 12 (diciembre) → monthValue == 12")
        void dateConverter_mesDiciembre() {
            Date result = converter.apply(new BigDecimal[]{
                    BigDecimal.valueOf(2024),
                    BigDecimal.valueOf(12),
                    BigDecimal.valueOf(31),
                    BigDecimal.valueOf(23),
                    BigDecimal.valueOf(59),
                    BigDecimal.valueOf(59)
            });

            LocalDateTime ldt = toLocalDateTime(result);
            assertThat(ldt.getMonthValue()).isEqualTo(12);
            assertThat(ldt.getDayOfMonth()).isEqualTo(31);
            assertThat(ldt.getHour()).isEqualTo(23);
            assertThat(ldt.getMinute()).isEqualTo(59);
            assertThat(ldt.getSecond()).isEqualTo(59);
        }

        @Test
        @DisplayName("Segundos con fracción .5 → 500 ms")
        void dateConverter_fraccion500ms() {
            Date result = converter.apply(new BigDecimal[]{
                    BigDecimal.valueOf(2024),
                    BigDecimal.valueOf(6),
                    BigDecimal.valueOf(15),
                    BigDecimal.valueOf(14),
                    BigDecimal.valueOf(30),
                    BigDecimal.valueOf(45.5)
            });

            LocalDateTime ldt = toLocalDateTime(result);
            assertThat(ldt.getSecond()).isEqualTo(45);
            assertThat(ldt.getNano()).isEqualTo(500_000_000);
        }

        @Test
        @DisplayName("Segundos con fracción .25 → 250 ms")
        void dateConverter_fraccion250ms() {
            Date result = converter.apply(new BigDecimal[]{
                    BigDecimal.valueOf(2024),
                    BigDecimal.valueOf(6),
                    BigDecimal.valueOf(15),
                    BigDecimal.valueOf(14),
                    BigDecimal.valueOf(30),
                    BigDecimal.valueOf(45.25)
            });

            assertThat(toLocalDateTime(result).getNano()).isEqualTo(250_000_000);
        }

        @Test
        @DisplayName("Segundos con fracción .75 → 750 ms")
        void dateConverter_fraccion750ms() {
            Date result = converter.apply(new BigDecimal[]{
                    BigDecimal.valueOf(2024),
                    BigDecimal.valueOf(6),
                    BigDecimal.valueOf(15),
                    BigDecimal.valueOf(14),
                    BigDecimal.valueOf(30),
                    BigDecimal.valueOf(45.75)
            });

            assertThat(toLocalDateTime(result).getNano()).isEqualTo(750_000_000);
        }

        @Test
        @DisplayName("Fracción .0 → 0 ms")
        void dateConverter_fraccionCero() {
            Date result = converter.apply(new BigDecimal[]{
                    BigDecimal.valueOf(2024),
                    BigDecimal.valueOf(6),
                    BigDecimal.valueOf(15),
                    BigDecimal.valueOf(14),
                    BigDecimal.valueOf(30),
                    BigDecimal.valueOf(45.0)
            });

            assertThat(toLocalDateTime(result).getNano()).isZero();
        }

        @Test
        @DisplayName("Devuelve un Converter no nulo")
        void dateConverter_noNulo() {
            assertThat(converter).isNotNull();
        }
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private static LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}