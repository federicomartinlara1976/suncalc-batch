package net.bounceme.chronos.suncalc.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import net.bounceme.chronos.suncalc.clients.FaseLunarRest;
import net.bounceme.chronos.suncalc.dto.DataSolsticeEquinoxDTO;
import net.bounceme.chronos.suncalc.dto.FaseLunarDTO;
import net.bounceme.chronos.suncalc.dto.InputFaseLunarDTO;
import net.bounceme.chronos.suncalc.services.impl.EfemeridesServiceImpl;
import net.bounceme.chronos.utils.calc.converters.Converter;

@ExtendWith(MockitoExtension.class)
@DisplayName("AstronomiaServiceImpl - cobertura de servicios astronómicos")
class EfemeridesServiceImplTest {

    @Mock
    private CalcService calcService;
    
    @Mock
    private FaseLunarRest faseLunarRest; // Importa tu clase/interfaz real

    @Mock
    private SimpleDateFormat dateFormat;

    @Mock
    private Converter<BigDecimal[], Date> dateConverter;

    @InjectMocks
    private EfemeridesServiceImpl service;

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
                    "[JDE, utc, local, off] = equinoccio_solsticio(2024, 'june_solstice', 'null')",
                    "[JDE, utc, local, off] = equinoccio_solsticio(2024, 'december_solstice', 'null')",
                    "[JDE, utc, local, off] = equinoccio_solsticio(2024, 'march_equinox', 'null')",
                    "[JDE, utc, local, off] = equinoccio_solsticio(2024, 'september_equinox', 'null')");

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

        private static final String FECHA = "2024-06-15";
        private static final Float LATITUDE = 40.4168f;
        private static final Float LONGITUDE = -3.7038f;
        private static final String TIMEZONE = "Europe/Madrid";
        
        @BeforeEach
        void setUp() {
            // Inyectar las propiedades @Value en el servicio
            ReflectionTestUtils.setField(service, "latitude", LATITUDE);
            ReflectionTestUtils.setField(service, "longitude", LONGITUDE);
            ReflectionTestUtils.setField(service, "timeZone", TIMEZONE);
        }

        @Test
        @DisplayName("Fecha válida: llama al REST con el DTO correcto y devuelve el primer elemento")
        void calculateFaseLunar_fechaValida_devuelveDTOCompleto() {
            // Arrange
            FaseLunarDTO expectedDto = new FaseLunarDTO();
            expectedDto.setDate(new Date());
            expectedDto.setMoonAgeDays(7.5f);
            expectedDto.setPhaseName("creciente");
            expectedDto.setIlluminationPercent(0.75f);

            when(faseLunarRest.detalle(any(InputFaseLunarDTO.class)))
                    .thenReturn(List.of(expectedDto));

            ArgumentCaptor<InputFaseLunarDTO> captor = ArgumentCaptor.forClass(InputFaseLunarDTO.class);

            // Act
            FaseLunarDTO result = service.calculateFaseLunar(FECHA);

            // Assert
            assertThat(result).isSameAs(expectedDto);

            verify(faseLunarRest).detalle(captor.capture());
            InputFaseLunarDTO input = captor.getValue();
            
            assertThat(input.getStartDate()).isEqualTo(FECHA);
            assertThat(input.getLatitude()).isEqualTo(LATITUDE);
            assertThat(input.getLongitude()).isEqualTo(LONGITUDE);
            assertThat(input.getDays()).isEqualTo(1);
            assertThat(input.getTimezone()).isEqualTo(TIMEZONE);
        }

        @Test
        @DisplayName("Respuesta vacía del REST: lanza IndexOutOfBoundsException al hacer get(0)")
        void calculateFaseLunar_respuestaVacia_lanzaIndexOutOfBounds() {
            // Arrange
            when(faseLunarRest.detalle(any(InputFaseLunarDTO.class)))
                    .thenReturn(Collections.emptyList());

            // Act / Assert
            assertThatThrownBy(() -> service.calculateFaseLunar(FECHA))
                    .isInstanceOf(IndexOutOfBoundsException.class);
        }

        @Test
        @DisplayName("Respuesta null del REST: lanza NullPointerException")
        void calculateFaseLunar_respuestaNull_lanzaNullPointerException() {
            // Arrange
            when(faseLunarRest.detalle(any(InputFaseLunarDTO.class)))
                    .thenReturn(null);

            // Act / Assert
            assertThatThrownBy(() -> service.calculateFaseLunar(FECHA))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
