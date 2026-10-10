package net.bounceme.chronos.suncalc.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import net.bounceme.chronos.suncalc.model.Differences;
import net.bounceme.chronos.suncalc.model.TimeData;

@DisplayName("SuncalcHelper - utilidades de cálculo astronómico")
class SuncalcHelperTest {

    // ==================================================================
    // subtractDays
    // ==================================================================

    @Nested
    @DisplayName("subtractDays")
    class SubtractDaysTests {

        @Test
        @DisplayName("Resta 1 día correctamente")
        void subtractDays_unDia() {
            Date base = Date.from(Instant.parse("2024-06-15T12:00:00Z"));

            Date result = SuncalcHelper.subtractDays(base, 1);

            LocalDate resultDate = result.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            assertThat(resultDate).isEqualTo(LocalDate.of(2024, 6, 14));
        }

        @Test
        @DisplayName("Resta 0 días devuelve el mismo instante")
        void subtractDays_ceroDias() {
            Date base = Date.from(Instant.parse("2024-06-15T12:00:00Z"));

            Date result = SuncalcHelper.subtractDays(base, 0);

            assertThat(result.toInstant()).isEqualTo(base.toInstant());
        }

        @Test
        @DisplayName("Resta N días (30)")
        void subtractDays_treintaDias() {
            Date base = Date.from(Instant.parse("2024-06-30T12:00:00Z"));

            Date result = SuncalcHelper.subtractDays(base, 30);

            LocalDate resultDate = result.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            assertThat(resultDate).isEqualTo(LocalDate.of(2024, 5, 31));
        }

        @Test
        @DisplayName("Número negativo → suma días (comportamiento simétrico)")
        void subtractDays_numeroNegativoSuma() {
            Date base = Date.from(Instant.parse("2024-06-15T12:00:00Z"));

            Date result = SuncalcHelper.subtractDays(base, -1);

            LocalDate resultDate = result.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            assertThat(resultDate).isEqualTo(LocalDate.of(2024, 6, 16));
        }
    }

    // ==================================================================
    // round (HALF_EVEN)
    // ==================================================================

    @Nested
    @DisplayName("round con HALF_EVEN")
    class RoundTests {

        @Test
        @DisplayName("0.5 → 0 (empate va al par más cercano)")
        void round_ceroComaCinco() {
            assertThat(SuncalcHelper.round(0.5, 0)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("1.5 → 2")
        void round_unoComaCinco() {
            assertThat(SuncalcHelper.round(1.5, 0)).isEqualTo(2.0);
        }

        @Test
        @DisplayName("2.5 → 2 (empate va al par más cercano)")
        void round_dosComaCinco() {
            assertThat(SuncalcHelper.round(2.5, 0)).isEqualTo(2.0);
        }

        @Test
        @DisplayName("3.5 → 4")
        void round_tresComaCinco() {
            assertThat(SuncalcHelper.round(3.5, 0)).isEqualTo(4.0);
        }

        @Test
        @DisplayName("Negativo -2.5 → -2 (HALF_EVEN también en negativos)")
        void round_negativoDosComaCinco() {
            assertThat(SuncalcHelper.round(-2.5, 0)).isEqualTo(-2.0);
        }

        @Test
        @DisplayName("Scale 2: 1.235 → 1.24")
        void round_scaleDos() {
            assertThat(SuncalcHelper.round(1.235, 2)).isEqualTo(1.24);
        }

        @Test
        @DisplayName("Scale 2 con empate: 1.225 → 1.22 (par más cercano)")
        void round_scaleDosEmpate() {
            assertThat(SuncalcHelper.round(1.225, 2)).isEqualTo(1.22);
        }

        @Test
        @DisplayName("Cero se mantiene cero")
        void round_cero() {
            assertThat(SuncalcHelper.round(0.0, 2)).isEqualTo(0.0);
        }
    }

    // ==================================================================
    // convertToLocalTimeViaInstant
    // ==================================================================

    @Nested
    @DisplayName("convertToLocalTimeViaInstant")
    class ConvertToLocalTimeTests {

        @Test
        @DisplayName("Extrae solo la componente horaria del Date")
        void convertToLocalTime_extraeHora() {
            // 2024-06-15T14:30:00Z → en la zona del sistema
            Date date = Date.from(Instant.parse("2024-06-15T14:30:00Z"));

            LocalTime result = SuncalcHelper.convertToLocalTimeViaInstant(date);

            // Extraemos la hora esperada aplicando la misma conversión
            LocalTime expected = date.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Solo devuelve la hora (sin fecha)")
        void convertToLocalTime_soloHora() {
            Date date = Date.from(Instant.parse("2024-06-15T08:15:45Z"));

            LocalTime result = SuncalcHelper.convertToLocalTimeViaInstant(date);

            assertThat(result).isNotNull();
            assertThat(result.getHour()).isBetween(0, 23);
            assertThat(result.getMinute()).isBetween(0, 59);
        }
    }

    // ==================================================================
    // obtenerDia / obtenerMes / obtenerAnio
    // ==================================================================

    @Nested
    @DisplayName("obtenerDia / obtenerMes / obtenerAnio")
    class ObtencionComponentesTests {

        @Test
        @DisplayName("obtenerDia devuelve el día del mes")
        void obtenerDia_devuelveDia() {
            Date date = Date.from(Instant.parse("2024-06-15T12:00:00Z"));

            Integer result = SuncalcHelper.obtenerDia(date);

            assertThat(result).isEqualTo(15);
        }

        @Test
        @DisplayName("obtenerMes devuelve el mes (1-12)")
        void obtenerMes_devuelveMes() {
            Date date = Date.from(Instant.parse("2024-06-15T12:00:00Z"));

            Integer result = SuncalcHelper.obtenerMes(date);

            assertThat(result).isEqualTo(6);
        }

        @Test
        @DisplayName("obtenerAnio devuelve el año")
        void obtenerAnio_devuelveAnio() {
            Date date = Date.from(Instant.parse("2024-06-15T12:00:00Z"));

            Integer result = SuncalcHelper.obtenerAnio(date);

            assertThat(result).isEqualTo(2024);
        }
    }

    // ==================================================================
    // createDifferences
    // ==================================================================

    @Nested
    @DisplayName("createDifferences")
    class CreateDifferencesTests {

        private TimeData timeData(String id, Date dawn, Date sunrise, Date culmination, Date sunset, Date dusk) {
            TimeData td = new TimeData();
            td.setId(id);
            td.setDawn(dawn);
            td.setSunrise(sunrise);
            td.setCulmination(culmination);
            td.setSunset(sunset);
            td.setDusk(dusk);
            return td;
        }

        @Test
        @DisplayName("nextData null → Optional.empty")
        void createDifferences_nextDataNull_empty() {
            TimeData prev = timeData("prev", new Date(), new Date(), new Date(), new Date(), new Date());

            Optional<Differences> result = SuncalcHelper.createDifferences(null, prev);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("nextData != null y prevData != null → Optional.of con id y lastDate")
        void createDifferences_ambosValidos() {
            Date d = new Date();
            TimeData next = timeData("2024-06-15", d, d, d, d, d);
            TimeData prev = timeData("2024-06-14", d, d, d, d, d);

            Optional<Differences> result = SuncalcHelper.createDifferences(next, prev);

            assertThat(result).isPresent();
            Differences diff = result.get();
            assertThat(diff.getId()).isEqualTo("2024-06-15 - 2024-06-14");
            assertThat(diff.getLastDate()).isEqualTo("2024-06-15");
        }

        @Test
        @DisplayName("nextData válido, prevData null → NullPointerException")
        void createDifferences_prevDataNull_lanzaNPE() {
            Date d = new Date();
            TimeData next = timeData("2024-06-15", d, d, d, d, d);

            assertThatThrownBy(() -> SuncalcHelper.createDifferences(next, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Fechas null en ambos → diferencias 0L (protegidas por calculateDifference)")
        void createDifferences_fechasNull_diferenciasCero() {
            TimeData next = timeData("n", null, null, null, null, null);
            TimeData prev = timeData("p", null, null, null, null, null);

            Optional<Differences> result = SuncalcHelper.createDifferences(next, prev);

            assertThat(result).isPresent();
            Differences diff = result.get();
            assertThat(diff.getDawn()).isZero();
            assertThat(diff.getSunrise()).isZero();
            assertThat(diff.getCulmination()).isZero();
            assertThat(diff.getSunset()).isZero();
            assertThat(diff.getDusk()).isZero();
        }

        @Test
        @DisplayName("Fechas válidas → diferencias en segundos (valor absoluto positivo)")
        void createDifferences_fechasValidas_calculaSegundos() {
            Date nextSunrise = Date.from(Instant.parse("2024-06-15T06:45:00Z"));
            Date prevSunrise = Date.from(Instant.parse("2024-06-14T06:30:00Z"));

            TimeData next = timeData("n", null, nextSunrise, null, null, null);
            TimeData prev = timeData("p", null, prevSunrise, null, null, null);

            Optional<Differences> result = SuncalcHelper.createDifferences(next, prev);

            assertThat(result).isPresent();
            // 15 minutos = 900 segundos (o -900 según el orden de Duration.between)
            assertThat(Math.abs(result.get().getSunrise())).isEqualTo(900L);
        }
        
        @Test
        @DisplayName("nextData con dawn válido, prevData con dawn null → diferencia 0L sin NPE")
        void createDifferences_nextDawnValidoPrevDawnNull_diferenciaCero() {
            // Arrange: next tiene dawn, prev no
            Date d = new Date();
            TimeData next = timeData("2024-06-15", d, null, null, null, null);
            TimeData prev = timeData("2024-06-14", null, null, null, null, null);

            // Act
            Optional<Differences> result = SuncalcHelper.createDifferences(next, prev);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getDawn()).isZero();
            // El resto de campos: next tiene null y prev tiene null → 0L por el otro camino
            assertThat(result.get().getSunrise()).isZero();
            assertThat(result.get().getCulmination()).isZero();
            assertThat(result.get().getSunset()).isZero();
            assertThat(result.get().getDusk()).isZero();
            // id y lastDate sí se setean porque nextData no es null
            assertThat(result.get().getId()).isEqualTo("2024-06-15 - 2024-06-14");
            assertThat(result.get().getLastDate()).isEqualTo("2024-06-15");
        }
    }

    // ==================================================================
    // getDiasDelMes
    // ==================================================================

    @Nested
    @DisplayName("getDiasDelMes")
    class GetDiasDelMesTests {

        @Test
        @DisplayName("Meses de 31 días → 31")
        void getDiasDelMes_31Dias() {
            for (int mes : new int[]{ 1, 3, 5, 7, 8, 10, 12 }) {
                assertThat(SuncalcHelper.getDiasDelMes(mes, 2024))
                        .as("Mes %d debe tener 31 días", mes)
                        .isEqualTo(31);
            }
        }

        @Test
        @DisplayName("Meses de 30 días → 30")
        void getDiasDelMes_30Dias() {
            for (int mes : new int[]{ 4, 6, 9, 11 }) {
                assertThat(SuncalcHelper.getDiasDelMes(mes, 2024))
                        .as("Mes %d debe tener 30 días", mes)
                        .isEqualTo(30);
            }
        }

        @Test
        @DisplayName("Febrero no bisiesto → 28")
        void getDiasDelMes_febreroNoBisiesto() {
            assertThat(SuncalcHelper.getDiasDelMes(2, 2023)).isEqualTo(28);
        }

        @Test
        @DisplayName("Febrero bisiesto (divisible por 4, no por 100) → 29")
        void getDiasDelMes_febreroBisiesto() {
            assertThat(SuncalcHelper.getDiasDelMes(2, 2024)).isEqualTo(29);
        }

        @Test
        @DisplayName("Febrero bisiesto (divisible por 400) → 29")
        void getDiasDelMes_febreroBisiestoPor400() {
            assertThat(SuncalcHelper.getDiasDelMes(2, 2000)).isEqualTo(29);
        }

        @Test
        @DisplayName("Febrero no bisiesto (divisible por 100, no por 400) → 28")
        void getDiasDelMes_febreroNoBisiestoPor100() {
            assertThat(SuncalcHelper.getDiasDelMes(2, 1900)).isEqualTo(28);
        }

        @Test
        @DisplayName("Mes inválido (0) → IllegalArgumentException")
        void getDiasDelMes_mesCero_lanzaIAE() {
            assertThatThrownBy(() -> SuncalcHelper.getDiasDelMes(0, 2024))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Mes inválido")
                    .hasMessageContaining("0");
        }

        @Test
        @DisplayName("Mes inválido (13) → IllegalArgumentException")
        void getDiasDelMes_mesTrece_lanzaIAE() {
            assertThatThrownBy(() -> SuncalcHelper.getDiasDelMes(13, 2024))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Mes inválido");
        }

        @Test
        @DisplayName("Mes negativo (-1) → IllegalArgumentException")
        void getDiasDelMes_mesNegativo_lanzaIAE() {
            assertThatThrownBy(() -> SuncalcHelper.getDiasDelMes(-1, 2024))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Mes inválido");
        }
    }

    // ==================================================================
    // normalize
    // ==================================================================

    @Nested
    @DisplayName("normalize")
    class NormalizeTests {

        @Test
        @DisplayName("Número de un dígito → añade '0' delante")
        void normalize_unDigito() {
            assertThat(SuncalcHelper.normalize(0)).isEqualTo("00");
            assertThat(SuncalcHelper.normalize(5)).isEqualTo("05");
            assertThat(SuncalcHelper.normalize(9)).isEqualTo("09");
        }

        @Test
        @DisplayName("Número de dos dígitos → sin cambios")
        void normalize_dosDigitos() {
            assertThat(SuncalcHelper.normalize(10)).isEqualTo("10");
            assertThat(SuncalcHelper.normalize(42)).isEqualTo("42");
            assertThat(SuncalcHelper.normalize(99)).isEqualTo("99");
        }

        @Test
        @DisplayName("Número de tres o más dígitos → sin cambios")
        void normalize_tresDigitos() {
            assertThat(SuncalcHelper.normalize(100)).isEqualTo("100");
            assertThat(SuncalcHelper.normalize(9999)).isEqualTo("9999");
        }
    }
}