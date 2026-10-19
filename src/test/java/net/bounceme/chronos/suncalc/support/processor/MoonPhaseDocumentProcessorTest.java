package net.bounceme.chronos.suncalc.support.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.ParseException;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import lombok.SneakyThrows;
import net.bounceme.chronos.suncalc.clients.FaseLunarRest;
import net.bounceme.chronos.suncalc.dto.FaseLunarDTO;
import net.bounceme.chronos.suncalc.dto.InputFaseLunarDTO;
import net.bounceme.chronos.suncalc.model.MoonData;
import net.bounceme.chronos.suncalc.model.Registro;

@ExtendWith(MockitoExtension.class)
@DisplayName("MoonPhaseDocumentProcessor - procesamiento de datos lunares")
class MoonPhaseDocumentProcessorTest {

    private static final String TODAY = "today";
    private static final String OTHER_DATE = "2024-06-15";
    private static final Float LATITUDE = 40.4168f;
    private static final Float LONGITUDE = -3.7038f;
    private static final String TIMEZONE = "Europe/Madrid";

    @Mock private FaseLunarRest faseLunarRest;
    @Mock private SimpleDateFormat dateFormat;

    @InjectMocks
    private MoonPhaseDocumentProcessor processor;

    @BeforeEach
    void setUp() {
        // Inyección de las propiedades heredadas de BaseDocumentProcessor
        ReflectionTestUtils.setField(processor, "latitude", LATITUDE);
        ReflectionTestUtils.setField(processor, "longitude", LONGITUDE);
        ReflectionTestUtils.setField(processor, "timeZone", TIMEZONE);
        ReflectionTestUtils.setField(processor, "dateFormat", dateFormat);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private FaseLunarDTO createMockResponse() {
        FaseLunarDTO dto = new FaseLunarDTO();
        dto.setDate(new Date());
        dto.setWeekday("Saturday");
        dto.setPhaseName("Waxing Crescent");
        dto.setPhaseEmoji("🌒");
        dto.setIlluminationPercent(0.75f);
        dto.setMoonAgeDays(7.5f);
        dto.setDistanceKm(384400f);
        dto.setPrincipalPhase("First Quarter");
        dto.setPrincipalPhaseTime("");
        dto.setMoonrise("2026-10-19 16:13");
        dto.setMoonset("2026-10-19 01:06");
        return dto;
    }

    // ==================================================================
    // process()
    // ==================================================================

    @Nested
    @DisplayName("process() sin argumentos")
    class ProcessSinFechaTests {

        @Test
        @DisplayName("Pasa 'today' al REST y no llama a parse")
        @SneakyThrows
        void process_pasaTodayAlRest() {
            // Arrange
            when(faseLunarRest.detalle(any(InputFaseLunarDTO.class)))
                    .thenReturn(List.of(createMockResponse()));

            ArgumentCaptor<InputFaseLunarDTO> captor = ArgumentCaptor.forClass(InputFaseLunarDTO.class);

            // Act
            processor.process();

            // Assert
            verify(faseLunarRest).detalle(captor.capture());
            assertThat(captor.getValue().getStartDate()).isEqualTo(TODAY);
            verify(dateFormat, never()).parse(anyString());
        }
    }

    // ==================================================================
    // process(String)
    // ==================================================================

    @Nested
    @DisplayName("process(String)")
    class ProcessConFechaTests {

        @Test
        @DisplayName("Pasa la fecha al REST y parsea la fecha")
        void process_conFecha_pasaFechaAlRest() throws ParseException {
            // Arrange
            when(faseLunarRest.detalle(any(InputFaseLunarDTO.class)))
                    .thenReturn(List.of(createMockResponse()));
            when(dateFormat.parse(OTHER_DATE)).thenReturn(new Date());

            ArgumentCaptor<InputFaseLunarDTO> captor = ArgumentCaptor.forClass(InputFaseLunarDTO.class);

            // Act
            processor.process(OTHER_DATE);

            // Assert
            verify(faseLunarRest).detalle(captor.capture());
            assertThat(captor.getValue().getStartDate()).isEqualTo(OTHER_DATE);
            verify(dateFormat).parse(OTHER_DATE);
        }

        @Test
        @DisplayName("ParseException al parsear la fecha → se propaga")
        void process_parseFalla_propaga() throws ParseException {
            // Arrange
            when(dateFormat.parse(OTHER_DATE)).thenThrow(new ParseException("Error", 0));

            // Act / Assert
            assertThatThrownBy(() -> processor.process(OTHER_DATE))
                    .isInstanceOf(ParseException.class);

            verify(faseLunarRest, never()).detalle(any());
        }
    }

    // ==================================================================
    // obtainData: respuesta no vacía (camino feliz)
    // ==================================================================

    @Nested
    @DisplayName("Respuesta no vacía")
    class RespuestaNoVaciaTests {

        @Test
        @DisplayName("Mapea todos los campos y setea status=true")
        void process_respuestaOk_mapeaCamposYStatusTrue() throws ParseException {
            // Arrange
            FaseLunarDTO mockDto = createMockResponse();
            when(faseLunarRest.detalle(any(InputFaseLunarDTO.class)))
                    .thenReturn(List.of(mockDto));
            when(dateFormat.parse(OTHER_DATE)).thenReturn(new Date());
            
            // Para el id del MoonData
            when(dateFormat.format(mockDto.getDate())).thenReturn("2024-06-15");

            // Act
            Registro<MoonData> result = processor.process(OTHER_DATE);

            // Assert
            assertThat(result).isNotNull();
            MoonData data = result.getContent();
            
            assertThat(data.getStatus()).isTrue();
            assertThat(data.getId()).isEqualTo("2024-06-15");
            assertThat(data.getWeekday()).isEqualTo("Saturday");
            assertThat(data.getPhaseName()).isEqualTo("Waxing Crescent");
            assertThat(data.getPhaseEmoji()).isEqualTo("🌒");
            assertThat(data.getIlluminationPercent()).isEqualTo(0.75f);
            assertThat(data.getMoonAgeDays()).isEqualTo(7.5f);
            assertThat(data.getDistanceKm()).isEqualTo(384400f);
            assertThat(data.getPrincipalPhase()).isEqualTo("First Quarter");
            assertThat(data.getPrincipalPhaseTime()).isEqualTo(mockDto.getPrincipalPhaseTime());
            assertThat(data.getMoonrise()).isEqualTo(mockDto.getMoonrise());
            assertThat(data.getMoonset()).isEqualTo(mockDto.getMoonset());
        }
    }

    // ==================================================================
    // obtainData: respuesta vacía o null
    // ==================================================================

    @Nested
    @DisplayName("Respuesta vacía o null")
    class RespuestaVaciaTests {

        @Test
        @DisplayName("Lista vacía → status=false y campos de datos null")
        void process_listaVacia_statusFalse() throws ParseException {
            // Arrange
            when(faseLunarRest.detalle(any(InputFaseLunarDTO.class)))
                    .thenReturn(Collections.emptyList());
            when(dateFormat.parse(OTHER_DATE)).thenReturn(new Date());

            // Act
            Registro<MoonData> result = processor.process(OTHER_DATE);

            // Assert
            MoonData data = result.getContent();
            assertThat(data.getStatus()).isFalse();
            assertThat(data.getId()).isNull();
            assertThat(data.getPhaseName()).isNull();
            // Fecha SÍ se setea porque se hace antes del if
            assertThat(data.getFecha()).isNotNull();
        }

        @Test
        @DisplayName("Lista null → status=false")
        void process_listaNull_statusFalse() throws ParseException {
            // Arrange
            when(faseLunarRest.detalle(any(InputFaseLunarDTO.class))).thenReturn(null);
            when(dateFormat.parse(OTHER_DATE)).thenReturn(new Date());

            // Act
            Registro<MoonData> result = processor.process(OTHER_DATE);

            // Assert
            MoonData data = result.getContent();
            assertThat(data.getStatus()).isFalse();
            assertThat(data.getId()).isNull();
        }
    }

    // ==================================================================
    // Comprobación del InputFaseLunarDTO (Builder)
    // ==================================================================

    @Nested
    @DisplayName("Construcción del InputFaseLunarDTO")
    class InputDtoTests {

        @Test
        @DisplayName("Se construye con las coordenadas, días y timezone correctos")
        void process_builderCreaInputCorrecto() throws ParseException {
            // Arrange
            when(faseLunarRest.detalle(any(InputFaseLunarDTO.class)))
                    .thenReturn(Collections.emptyList());
            when(dateFormat.parse(OTHER_DATE)).thenReturn(new Date());

            ArgumentCaptor<InputFaseLunarDTO> captor = ArgumentCaptor.forClass(InputFaseLunarDTO.class);

            // Act
            processor.process(OTHER_DATE);

            // Assert
            verify(faseLunarRest).detalle(captor.capture());
            InputFaseLunarDTO input = captor.getValue();

            assertThat(input.getStartDate()).isEqualTo(OTHER_DATE);
            assertThat(input.getLatitude()).isEqualTo(LATITUDE);
            assertThat(input.getLongitude()).isEqualTo(LONGITUDE);
            assertThat(input.getDays()).isEqualTo(1);
            assertThat(input.getTimezone()).isEqualTo(TIMEZONE);
        }
    }
}
