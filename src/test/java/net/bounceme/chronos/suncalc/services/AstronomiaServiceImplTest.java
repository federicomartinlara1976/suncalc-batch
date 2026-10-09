package net.bounceme.chronos.suncalc.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.bounceme.chronos.suncalc.dto.DataSolsticeEquinoxDTO;
import net.bounceme.chronos.suncalc.dto.FaseLunarDTO;
import net.bounceme.chronos.suncalc.services.impl.AstronomiaServiceImpl;
import net.bounceme.chronos.utils.calc.converters.Converter;

@ExtendWith(MockitoExtension.class)
@DisplayName("AstronomiaServiceImpl - cobertura de servicios astronómicos")
class AstronomiaServiceImplTest {

    private static final String HOUR_ZONE = "Europe/Madrid";

    @Mock
    private CalcService calcService;

    @Mock
    private SimpleDateFormat dateFormat;

    @Mock
    private Converter<BigDecimal[], Date> dateConverter;

    @InjectMocks
    private AstronomiaServiceImpl service;

    @Captor
    private ArgumentCaptor<String> cmdCaptor;

    // ==================================================================
    // calculateSolsticesEquinoxes
    // ==================================================================

    @Nested
    @DisplayName("calculateSolsticesEquinoxes")
    class CalculateSolsticesEquinoxesTests {

        @Test
        @DisplayName("Año válido: devuelve 4 DTOs en el orden SOLSTICES + EQUINOXES")
        void calculateSolsticesEquinoxes_yearValido_devuelveCuatroElementosEnOrden() {
            // Arrange
            Integer year = 2024;
            BigDecimal[] utcArray = { BigDecimal.valueOf(2460463.5) };
            BigDecimal[] localArray = { BigDecimal.valueOf(2460463.6) };
            Date utcDate = new Date(1718409600000L);
            Date localDate = new Date(1718409601000L);

            when(calcService.getArray("utc")).thenReturn(utcArray);
            when(calcService.getArray("local")).thenReturn(localArray);
            when(dateConverter.apply(utcArray)).thenReturn(utcDate);
            when(dateConverter.apply(localArray)).thenReturn(localDate);

            // Act
            List<DataSolsticeEquinoxDTO> result = service.calculateSolsticesEquinoxes(year);

            // Assert
            assertThat(result).hasSize(4);
            assertThat(result)
                    .extracting(DataSolsticeEquinoxDTO::getName)
                    .containsExactly(
                            "june_solstice",
                            "december_solstice",
                            "march_equinox",
                            "september_equinox");
            assertThat(result).allSatisfy(dto -> {
                assertThat(dto.getUtcDate()).isEqualTo(utcDate);
                assertThat(dto.getLocalDate()).isEqualTo(localDate);
            });

            verify(calcService, times(4)).execute(anyString());
            verify(calcService, times(4)).getArray("utc");
            verify(calcService, times(4)).getArray("local");
        }

        @Test
        @DisplayName("Año válido: genera los comandos exactos con HOUR_ZONE y orden correcto")
        void calculateSolsticesEquinoxes_yearValido_generaComandosEsperados() {
            // Arrange
            Integer year = 2024;
            BigDecimal[] array = { BigDecimal.ONE };
            Date date = new Date(0L);

            when(calcService.getArray(anyString())).thenReturn(array);
            when(dateConverter.apply(array)).thenReturn(date);

            // Act
            service.calculateSolsticesEquinoxes(year);

            // Assert
            verify(calcService, times(4)).execute(cmdCaptor.capture());
            assertThat(cmdCaptor.getAllValues()).containsExactly(
                    "[JDE, utc, local, off] = equinoccio_solsticio(2024, 'june_solstice', '" + HOUR_ZONE + "')",
                    "[JDE, utc, local, off] = equinoccio_solsticio(2024, 'december_solstice', '" + HOUR_ZONE + "')",
                    "[JDE, utc, local, off] = equinoccio_solsticio(2024, 'march_equinox', '" + HOUR_ZONE + "')",
                    "[JDE, utc, local, off] = equinoccio_solsticio(2024, 'september_equinox', '" + HOUR_ZONE + "')");

            verifyNoMoreInteractions(calcService);
        }

        @Test
        @DisplayName("Año null: el formateo con %d lanza excepción antes de invocar a CalcService")
        void calculateSolsticesEquinoxes_yearNull_lanzaExcepcion() {
            // Arrange
            Integer year = null;

            // Act / Assert
            assertThatThrownBy(() -> service.calculateSolsticesEquinoxes(year))
                    .isInstanceOf(Exception.class);

            // No se llega a invocar a las dependencias
            verify(calcService, times(0)).execute(anyString());
        }
    }

    // ==================================================================
    // calculateFaseLunar
    // ==================================================================

    @Nested
    @DisplayName("calculateFaseLunar")
    class CalculateFaseLunarTests {

        @Test
        @DisplayName("Fecha válida: construye el DTO con edad, fase e iluminación")
        void calculateFaseLunar_fechaValida_devuelveDTOCompleto() throws ParseException {
            // Arrange
            String fecha = "2024-06-15";
            Date parsedDate = new Date(1718409600000L);

            when(dateFormat.parse(fecha)).thenReturn(parsedDate);
            when(calcService.getScalar("edad")).thenReturn(BigDecimal.valueOf(7.5));
            when(calcService.getString("fase")).thenReturn("creciente");
            when(calcService.getScalar("iluminacion")).thenReturn(BigDecimal.valueOf(0.75));

            // Act
            FaseLunarDTO result = service.calculateFaseLunar(fecha);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getDate()).isEqualTo(parsedDate);
            assertThat(result.getEdad()).isEqualTo(7.5f);
            assertThat(result.getFase()).isEqualTo("creciente");
            assertThat(result.getIluminacion()).isEqualTo(0.75f);

            verify(calcService, times(1)).execute(cmdCaptor.capture());
            assertThat(cmdCaptor.getValue())
                    .isEqualTo("[edad, fase, iluminacion] = fase_lunar(" + fecha + ")");

            verify(calcService).getScalar("edad");
            verify(calcService).getString("fase");
            verify(calcService).getScalar("iluminacion");
            verify(dateFormat).parse(fecha);
        }

        @Test
        @DisplayName("Fecha inválida: propaga ParseException (vía @SneakyThrows)")
        void calculateFaseLunar_fechaInvalida_propagaParseException() throws ParseException {
            // Arrange
            String fecha = "no-es-una-fecha";
            when(dateFormat.parse(fecha)).thenThrow(new ParseException("Formato inválido", 0));

            // Act / Assert
            assertThatThrownBy(() -> service.calculateFaseLunar(fecha))
                    .isInstanceOf(ParseException.class)
                    .hasMessageContaining("Formato inválido");

            // El parseo ocurre después de execute, así que este sí se invocó
            verify(calcService).execute(anyString());
        }
    }
}
