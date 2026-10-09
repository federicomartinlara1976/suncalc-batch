package net.bounceme.chronos.suncalc.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import net.bounceme.chronos.suncalc.model.TimeData;
import net.bounceme.chronos.suncalc.services.SuncalcService;

@ExtendWith(MockitoExtension.class)
@DisplayName("SuncalcController - endpoints REST (standalone MockMvc)")
class SuncalcControllerTest {

    private static final String BASE_URL = "/suncalc-batch";
    private static final String RESULT = "result";

    @Mock
    private SuncalcService suncalcService;

    @InjectMocks
    private SuncalcController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ==================================================================
    // GET /current
    // ==================================================================

    @Nested
    @DisplayName("GET /current")
    class GetCurrentTests {

        @Test
        @DisplayName("Devuelve 200 con el TimeData bajo 'result'")
        void getCurrent_devuelveOkConTimeData() throws Exception {
            // Arrange
            TimeData timeData = new TimeData();
            when(suncalcService.getCurrentTimeData()).thenReturn(timeData);

            // Act / Assert
            mockMvc.perform(get(BASE_URL + "/current"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$." + RESULT).exists());

            verify(suncalcService).getCurrentTimeData();
            verifyNoMoreInteractions(suncalcService);
        }

        @Test
        @DisplayName("Si el servicio devuelve null, responde 200 sin 'result' (Jackson omite nulls)")
        void getCurrent_servicioDevuelveNull_respondeOk() throws Exception {
            // Arrange
            when(suncalcService.getCurrentTimeData()).thenReturn(null);

            // Act / Assert
            mockMvc.perform(get(BASE_URL + "/current"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$." + RESULT).doesNotExist());
        }
    }

    // ==================================================================
    // GET /fecha
    // ==================================================================

    @Nested
    @DisplayName("GET /fecha")
    class GetByDateTests {

        @Test
        @DisplayName("Optional presente: 200 con el TimeData bajo 'result'")
        void getByDate_optionalPresente_devuelveOkConResultado() throws Exception {
            // Arrange
            TimeData timeData = new TimeData();
            when(suncalcService.getTimeDataByDate("2024-06-15"))
                    .thenReturn(Optional.of(timeData));

            // Act / Assert
            mockMvc.perform(get(BASE_URL + "/fecha").param("d", "2024-06-15"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$." + RESULT).exists());

            verify(suncalcService).getTimeDataByDate("2024-06-15");
        }

        @Test
        @DisplayName("Optional vacío: 404 sin 'result'")
        void getByDate_optionalVacio_devuelveNotFound() throws Exception {
            // Arrange
            when(suncalcService.getTimeDataByDate(anyString()))
                    .thenReturn(Optional.empty());

            // Act / Assert
            mockMvc.perform(get(BASE_URL + "/fecha").param("d", "2024-01-01"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$." + RESULT).doesNotExist());
        }

        @Test
        @DisplayName("Sin parámetro 'd': 400 Bad Request")
        void getByDate_sinParametroD_devuelveBadRequest() throws Exception {
            // Act / Assert
            mockMvc.perform(get(BASE_URL + "/fecha"))
                    .andExpect(status().isBadRequest());

            verify(suncalcService, times(0)).getTimeDataByDate(anyString());
        }

        @Test
        @DisplayName("Parámetro 'd' vacío: Spring 6.1 rechaza con 400 y no llega al servicio")
        void getByDate_parametroVacio_devuelveBadRequest() throws Exception {
            // Act / Assert
            mockMvc.perform(get(BASE_URL + "/fecha").param("d", ""))
                    .andExpect(status().isBadRequest());

            // El controlador no se invoca → el servicio no se toca
            verify(suncalcService, times(0)).getTimeDataByDate(anyString());
        }
    }

    // ==================================================================
    // GET /fechas
    // ==================================================================

    @Nested
    @DisplayName("GET /fechas")
    class GetByDatesTests {

        @Test
        @DisplayName("Lista no vacía: 200 con la lista bajo 'result'")
        void getByDates_listaNoVacia_devuelveOkConResultados() throws Exception {
            // Arrange
            List<TimeData> registros = List.of(new TimeData(), new TimeData());
            when(suncalcService.getByRangeDate("2024-01-01", "2024-12-31"))
                    .thenReturn(registros);

            // Act / Assert
            mockMvc.perform(get(BASE_URL + "/fechas")
                            .param("inicio", "2024-01-01")
                            .param("fin", "2024-12-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$." + RESULT).isArray())
                    .andExpect(jsonPath("$." + RESULT + ".length()").value(2));
        }

        @Test
        @DisplayName("Lista vacía: 404 Not Found")
        void getByDates_listaVacia_devuelveNotFound() throws Exception {
            // Arrange
            when(suncalcService.getByRangeDate(anyString(), anyString()))
                    .thenReturn(Collections.emptyList());

            // Act / Assert
            mockMvc.perform(get(BASE_URL + "/fechas")
                            .param("inicio", "2024-01-01")
                            .param("fin", "2024-12-31"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$." + RESULT).doesNotExist());
        }

        @Test
        @DisplayName("Lista null: 404 Not Found")
        void getByDates_listaNull_devuelveNotFound() throws Exception {
            // Arrange
            when(suncalcService.getByRangeDate(anyString(), anyString()))
                    .thenReturn(null);

            // Act / Assert
            mockMvc.perform(get(BASE_URL + "/fechas")
                            .param("inicio", "2024-01-01")
                            .param("fin", "2024-12-31"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Sin parámetro 'inicio': 400 Bad Request")
        void getByDates_sinInicio_devuelveBadRequest() throws Exception {
            mockMvc.perform(get(BASE_URL + "/fechas")
                            .param("fin", "2024-12-31"))
                    .andExpect(status().isBadRequest());

            verify(suncalcService, times(0)).getByRangeDate(anyString(), anyString());
        }

        @Test
        @DisplayName("Sin parámetro 'fin': 400 Bad Request")
        void getByDates_sinFin_devuelveBadRequest() throws Exception {
            mockMvc.perform(get(BASE_URL + "/fechas")
                            .param("inicio", "2024-01-01"))
                    .andExpect(status().isBadRequest());

            verify(suncalcService, times(0)).getByRangeDate(anyString(), anyString());
        }
    }
}