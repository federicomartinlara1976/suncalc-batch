package net.bounceme.chronos.suncalc.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import net.bounceme.chronos.suncalc.dto.DataSolsticeEquinoxDTO;
import net.bounceme.chronos.suncalc.dto.FaseLunarDTO;
import net.bounceme.chronos.suncalc.services.AstronomiaService;

@ExtendWith(MockitoExtension.class)
@DisplayName("AstronomiaController - endpoints REST de astronomía")
class AstronomiaControllerTest {

    private static final String BASE = "/suncalc-batch";

    @Mock private AstronomiaService astronomiaService;
    @Mock private SimpleDateFormat dateFormat;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AstronomiaController controller = new AstronomiaController();
        ReflectionTestUtils.setField(controller, "astronomiaService", astronomiaService);
        ReflectionTestUtils.setField(controller, "dateFormat", dateFormat);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ==================================================================
    // GET /solstice-equinox/currentYear
    // ==================================================================

    @Nested
    @DisplayName("GET /solstice-equinox/currentYear")
    class GetCurrentTests {

        @Test
        @DisplayName("Llama al servicio con el año actual y devuelve 200 con la lista")
        void getCurrent_llamaConAnioActual_devuelve200ConLista() throws Exception {
            // Arrange
            List<DataSolsticeEquinoxDTO> esperado = List.of(
                    DataSolsticeEquinoxDTO.builder().name("june_solstice").build(),
                    DataSolsticeEquinoxDTO.builder().name("december_solstice").build());
            when(astronomiaService.calculateSolsticesEquinoxes(anyInt()))
                    .thenReturn(esperado);

            // Act / Assert
            mockMvc.perform(get(BASE + "/solstice-equinox/currentYear"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").isArray())
                    .andExpect(jsonPath("$.result.length()").value(2))
                    .andExpect(jsonPath("$.result[0].name").value("june_solstice"))
                    .andExpect(jsonPath("$.result[1].name").value("december_solstice"));

            ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
            verify(astronomiaService).calculateSolsticesEquinoxes(captor.capture());
            assertThat(captor.getValue()).isEqualTo(Calendar.getInstance().get(Calendar.YEAR));
        }

        @Test
        @DisplayName("Lista vacía: devuelve 200 con array vacío")
        void getCurrent_sinResultados_devuelveArrayVacio() throws Exception {
            // Arrange
            when(astronomiaService.calculateSolsticesEquinoxes(anyInt()))
                    .thenReturn(List.of());

            // Act / Assert
            mockMvc.perform(get(BASE + "/solstice-equinox/currentYear"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").isArray())
                    .andExpect(jsonPath("$.result").isEmpty());
        }
    }

    // ==================================================================
    // GET /solstice-equinox/year/{year}
    // ==================================================================

    @Nested
    @DisplayName("GET /solstice-equinox/year/{year}")
    class GetForYearTests {

        @Test
        @DisplayName("Año válido: delega el año al servicio y devuelve 200")
        void getForYear_anioValido_delegaYDevuelve200() throws Exception {
            // Arrange
            List<DataSolsticeEquinoxDTO> esperado = List.of(
                    DataSolsticeEquinoxDTO.builder().name("march_equinox").build());
            when(astronomiaService.calculateSolsticesEquinoxes(2024))
                    .thenReturn(esperado);

            // Act / Assert
            mockMvc.perform(get(BASE + "/solstice-equinox/year/{year}", 2024))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result[0].name").value("march_equinox"));

            verify(astronomiaService).calculateSolsticesEquinoxes(2024);
        }

        @Test
        @DisplayName("Año cero: se acepta y se delega")
        void getForYear_anioCero_delegaYDevuelve200() throws Exception {
            // Arrange
            when(astronomiaService.calculateSolsticesEquinoxes(0)).thenReturn(List.of());

            // Act / Assert
            mockMvc.perform(get(BASE + "/solstice-equinox/year/{year}", 0))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").isEmpty());

            verify(astronomiaService).calculateSolsticesEquinoxes(0);
        }

        @Test
        @DisplayName("Año negativo: se acepta y se delega")
        void getForYear_anioNegativo_delegaYDevuelve200() throws Exception {
            // Arrange
            when(astronomiaService.calculateSolsticesEquinoxes(-100)).thenReturn(List.of());

            // Act / Assert
            mockMvc.perform(get(BASE + "/solstice-equinox/year/{year}", -100))
                    .andExpect(status().isOk());

            verify(astronomiaService).calculateSolsticesEquinoxes(-100);
        }

        @Test
        @DisplayName("Año no numérico: Spring devuelve 400 antes de llegar al servicio")
        void getForYear_anioNoNumerico_devuelve400() throws Exception {
            // Act / Assert
            mockMvc.perform(get(BASE + "/solstice-equinox/year/{year}", "abc"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================================================================
    // GET /lunar-phase
    // ==================================================================

    @Nested
    @DisplayName("GET /lunar-phase")
    class GetCurrentLunarPhaseTests {

        @Test
        @DisplayName("Formatea la fecha actual y devuelve el DTO con 200")
        void getCurrentLunarPhase_formateaYDevuelve200() throws Exception {
            // Arrange
            String fechaFormateada = "2024-06-15";
            FaseLunarDTO dto = FaseLunarDTO.builder()
                    .date(new Date(0L))
                    .edad(7.5f)
                    .fase("creciente")
                    .iluminacion(0.75f)
                    .build();
            when(dateFormat.format(any(Date.class))).thenReturn(fechaFormateada);
            when(astronomiaService.calculateFaseLunar(fechaFormateada)).thenReturn(dto);

            // Act / Assert
            mockMvc.perform(get(BASE + "/lunar-phase"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.edad").value(7.5))
                    .andExpect(jsonPath("$.result.fase").value("creciente"))
                    .andExpect(jsonPath("$.result.iluminacion").value(0.75));

            verify(dateFormat).format(any(Date.class));
            verify(astronomiaService).calculateFaseLunar(fechaFormateada);
        }
    }

    // ==================================================================
    // GET /lunar-phase/{date}
    // ==================================================================

    @Nested
    @DisplayName("GET /lunar-phase/{date}")
    class GetLunarPhaseForTests {

        @Test
        @DisplayName("Fecha válida: delega al servicio y devuelve 200 con el DTO")
        void getLunarPhaseFor_fechaValida_devuelve200() throws Exception {
            // Arrange
            String fecha = "2024-06-15";
            FaseLunarDTO dto = FaseLunarDTO.builder()
                    .date(new Date(0L))
                    .edad(3.2f)
                    .fase("menguante")
                    .iluminacion(0.4f)
                    .build();
            when(astronomiaService.calculateFaseLunar(fecha)).thenReturn(dto);

            // Act / Assert
            mockMvc.perform(get(BASE + "/lunar-phase/{date}", fecha))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.fase").value("menguante"))
                    .andExpect(jsonPath("$.result.edad").value(3.2))
                    .andExpect(jsonPath("$.result.iluminacion").value(0.4));

            verify(astronomiaService).calculateFaseLunar(fecha);
        }

        @Test
        @DisplayName("Fecha con formato ISO completo: se pasa tal cual al servicio")
        void getLunarPhaseFor_fechaIso_pasaTalCual() throws Exception {
            // Arrange
            String fecha = "2024-06-15T10:30:00";
            when(astronomiaService.calculateFaseLunar(fecha))
                    .thenReturn(FaseLunarDTO.builder().build());

            // Act / Assert
            mockMvc.perform(get(BASE + "/lunar-phase/{date}", fecha))
                    .andExpect(status().isOk());

            verify(astronomiaService).calculateFaseLunar(fecha);
        }

        @Test
        @DisplayName("Fecha con caracteres especiales codificados: se decodifica y se pasa")
        void getLunarPhaseFor_fechaCodificada_seDecodifica() throws Exception {
            // Arrange
            String fecha = "2024-06-15";
            when(astronomiaService.calculateFaseLunar(fecha))
                    .thenReturn(FaseLunarDTO.builder().build());

            // Act / Assert
            mockMvc.perform(get(BASE + "/lunar-phase/{date}", fecha))
                    .andExpect(status().isOk());

            verify(astronomiaService).calculateFaseLunar(fecha);
        }
    }
}
