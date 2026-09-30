package net.bounceme.chronos.suncalc.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@DisplayName("IndexController - endpoint de estado")
class IndexControllerTest {

    private static final String APP_NAME        = "suncalc-batch";
    private static final String APP_DESCRIPTION = "Servicio de cálculo astronómico";
    private static final String INSTANCE_ID     = "suncalc-batch:8080";
    private static final Integer PORT           = 8080;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        IndexController controller = new IndexController();
        ReflectionTestUtils.setField(controller, "applicationName", APP_NAME);
        ReflectionTestUtils.setField(controller, "applicationDescription", APP_DESCRIPTION);
        ReflectionTestUtils.setField(controller, "port", PORT);
        ReflectionTestUtils.setField(controller, "instanceId", INSTANCE_ID);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ==================================================================
    // GET /status
    // ==================================================================

    @Test
    @DisplayName("Devuelve 200 con todos los campos rellenos")
    void status_devuelve200ConTodosLosCampos() throws Exception {
        // Arrange — se usan los valores reales de System.getProperty(...)
        String expectedVersion  = System.getProperty("java.version");
        String expectedPlatform = System.getProperty("os.name");

        // Act / Assert
        mockMvc.perform(get("/suncalc-batch/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationName").value(APP_NAME))
                .andExpect(jsonPath("$.description").value(APP_DESCRIPTION))
                .andExpect(jsonPath("$.version").value(expectedVersion))
                .andExpect(jsonPath("$.platform").value(expectedPlatform))
                .andExpect(jsonPath("$.instanceId").value(INSTANCE_ID))
                .andExpect(jsonPath("$.port").value(PORT))
                .andExpect(jsonPath("$.response").value("OK"));
    }

    @Test
    @DisplayName("El campo 'response' siempre vale 'OK'")
    void status_responseSiempreOk() throws Exception {
        // Act / Assert
        mockMvc.perform(get("/suncalc-batch/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("OK"));
    }

    @Test
    @DisplayName("Devuelve un Status con los 7 campos poblados (JSON crudo)")
    void status_devuelveObjetoStatusCompleto() throws Exception {
        // Arrange
        String expectedVersion  = System.getProperty("java.version");
        String expectedPlatform = System.getProperty("os.name");

        // Act
        String body = mockMvc.perform(get("/suncalc-batch/status"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert
        assertThat(body)
                .contains("\"applicationName\":\"" + APP_NAME + "\"")
                .contains("\"version\":\"" + expectedVersion + "\"")
                .contains("\"platform\":\"" + expectedPlatform + "\"")
                .contains("\"instanceId\":\"" + INSTANCE_ID + "\"")
                .contains("\"port\":" + PORT)
                .contains("\"response\":\"OK\"");
    }
}