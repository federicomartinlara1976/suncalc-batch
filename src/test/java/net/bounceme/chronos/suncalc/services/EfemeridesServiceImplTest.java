package net.bounceme.chronos.suncalc.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import lombok.SneakyThrows;
import net.bounceme.chronos.suncalc.clients.FaseLunarRest;
import net.bounceme.chronos.suncalc.dto.DataSolsticeEquinoxDTO;
import net.bounceme.chronos.suncalc.dto.FaseLunarDTO;
import net.bounceme.chronos.suncalc.model.MoonData;
import net.bounceme.chronos.suncalc.model.Registro;
import net.bounceme.chronos.suncalc.repository.MoonDataRepository;
import net.bounceme.chronos.suncalc.services.impl.EfemeridesServiceImpl;
import net.bounceme.chronos.suncalc.support.processor.MoonPhaseDocumentProcessor;
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
    
    @Mock
    private MoonDataRepository moonDataRepository;
    
    @Mock
    private MoonPhaseDocumentProcessor documentProcessor;

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

        private static final String DATE_STR = "2024-06-15";

        // Asegúrate de tener estos mocks en la cabecera de la clase de test:
        // @Mock private MoonDataRepository moonDataRepository;
        // @Mock private DocumentProcessor documentProcessor;

        @Test
        @DisplayName("Si existe en BD: mapea todos los campos de MoonData a FaseLunarDTO")
        void calculateFaseLunar_existeEnBd_mapeaCampos() throws ParseException {
            // Arrange
            Date parsedDate = new Date();
            MoonData moonData = MoonData.builder()
                    .weekday("Saturday")
                    .phaseName("Waxing Crescent")
                    .phaseEmoji("🌒")
                    .illuminationPercent(0.75f)
                    .moonAgeDays(7.5f)
                    .distanceKm(384400f)
                    .principalPhase("First Quarter")
                    .principalPhaseTime(new Date().toString())
                    .moonrise(new Date().toString())
                    .moonset(new Date().toString())
                    .build();

            when(moonDataRepository.findById(DATE_STR)).thenReturn(Optional.of(moonData));
            when(dateFormat.parse(DATE_STR)).thenReturn(parsedDate);

            // Act
            FaseLunarDTO result = service.calculateFaseLunar(DATE_STR);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getDate()).isEqualTo(parsedDate);
            assertThat(result.getWeekday()).isEqualTo("Saturday");
            assertThat(result.getPhaseName()).isEqualTo("Waxing Crescent");
            assertThat(result.getPhaseEmoji()).isEqualTo("🌒");
            assertThat(result.getIlluminationPercent()).isEqualTo(0.75f);
            assertThat(result.getMoonAgeDays()).isEqualTo(7.5f);
            assertThat(result.getDistanceKm()).isEqualTo(384400f);
            assertThat(result.getPrincipalPhase()).isEqualTo("First Quarter");
            assertThat(result.getPrincipalPhaseTime()).isEqualTo(moonData.getPrincipalPhaseTime());
            assertThat(result.getMoonrise()).isEqualTo(moonData.getMoonrise());
            assertThat(result.getMoonset()).isEqualTo(moonData.getMoonset());

            verify(moonDataRepository).findById(DATE_STR);
            verify(dateFormat).parse(DATE_STR);
            verify(documentProcessor, never()).process(); // No debe llamar al fallback
        }

        @Test
        @DisplayName("Si existe en BD pero ParseException: setea la fecha a null y devuelve el resto")
        void calculateFaseLunar_existeEnBdPeroFallaParseo_devuelveFechaNull() throws ParseException {
            // Arrange
            MoonData moonData = MoonData.builder().weekday("Saturday").build();
            
            when(moonDataRepository.findById(DATE_STR)).thenReturn(Optional.of(moonData));
            when(dateFormat.parse(DATE_STR)).thenThrow(new ParseException("Error", 0));

            // Act
            FaseLunarDTO result = service.calculateFaseLunar(DATE_STR);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getDate()).isNull(); // Capturado en parseDate
            assertThat(result.getWeekday()).isEqualTo("Saturday");

            verify(dateFormat).parse(DATE_STR);
            verify(documentProcessor, never()).process();
        }

        @Test
        @DisplayName("Si NO existe en BD: llama al documentProcessor y mapea su contenido a FaseLunarDTO")
        @SneakyThrows
        void calculateFaseLunar_noExisteEnBd_llamaAlDocumentProcessorYMapea() {
            // Arrange
            Date parsedDate = new Date();
            when(dateFormat.parse(DATE_STR)).thenReturn(parsedDate);

            MoonData moonData = new MoonData();
            moonData.setPhaseName("From Processor");
            moonData.setWeekday("Saturday");
            // Puedes añadir más campos aquí si quieres verificar que se copian todos
            
            Registro<MoonData> registro = new Registro<>();
            registro.setContent(moonData);
            
            // Como el procesador devuelve un Registro crudo (raw type) por el @SuppressWarnings del servicio
            @SuppressWarnings("unchecked")
            Registro<MoonData> rawRegistro = (Registro<MoonData>) (Registro) registro;
            
            when(documentProcessor.process()).thenReturn(rawRegistro);
            when(moonDataRepository.findById(DATE_STR)).thenReturn(Optional.empty());

            // Act
            FaseLunarDTO result = service.calculateFaseLunar(DATE_STR);

            // Assert
            assertThat(result).isNotNull();
            // Ahora SÍ se aplica toFaseLunarDTO, por lo que la fecha se parsea
            assertThat(result.getDate()).isEqualTo(parsedDate);
            assertThat(result.getPhaseName()).isEqualTo("From Processor");
            assertThat(result.getWeekday()).isEqualTo("Saturday");

            verify(moonDataRepository).findById(DATE_STR);
            verify(documentProcessor).process();
            
            // CORREGIDO: Antes era "never()", pero el fallback ahora sí mapea los datos
            verify(dateFormat).parse(DATE_STR); 
        }

        @Test
        @DisplayName("Si NO existe en BD y el processor lanza excepción: se propaga")
        void calculateFaseLunar_fallbackLanzaExcepcion_sePropaga() {
            // Arrange
            when(moonDataRepository.findById(DATE_STR)).thenReturn(Optional.empty());
            when(documentProcessor.process()).thenThrow(new RuntimeException("Fallo del processor"));

            // Act / Assert
            assertThatThrownBy(() -> service.calculateFaseLunar(DATE_STR))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Fallo del processor");

            verify(moonDataRepository).findById(DATE_STR);
        }
    }
}
