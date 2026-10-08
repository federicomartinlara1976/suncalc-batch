package net.bounceme.chronos.suncalc.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import net.bounceme.chronos.suncalc.dto.JobDTO;
import net.bounceme.chronos.suncalc.dto.TaskDTO;
import net.bounceme.chronos.suncalc.facade.JobFacade;
import net.bounceme.chronos.suncalc.services.JobService;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobController - endpoints de ejecución de tareas")
class JobControllerTest {

    private static final String BASE = "/suncalc-batch/jobs";
    private static final String IN_PROGRESS = "Tarea en ejecución";

    @Mock private JobService jobService;
    @Mock private JobFacade jobFacade;

    @Captor private ArgumentCaptor<JobDTO<TaskDTO>> jobCaptor;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        JobController controller = new JobController();
        ReflectionTestUtils.setField(controller, "jobService", jobService);
        ReflectionTestUtils.setField(controller, "jobFacade", jobFacade);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ==================================================================
    // PUT /execute
    // ==================================================================

    @Nested
    @DisplayName("PUT /execute")
    class ExecuteTaskTests {

        @Test
        @DisplayName("TaskDTO válido: publica JobDTO con el contenido y devuelve 200 con mensaje")
        void executeTask_taskValido_publicaYDevuelve200() throws Exception {
            // Arrange
            String body = """
                    {"name":"myTask"}
                    """;

            // Act / Assert
            mockMvc.perform(put(BASE + "/execute")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(IN_PROGRESS));

            verify(jobFacade).publishJob(jobCaptor.capture());
            JobDTO<TaskDTO> capturado = jobCaptor.getValue();
            assertThat(capturado).isNotNull();
            assertThat(capturado.getContent()).isInstanceOf(TaskDTO.class);
            assertThat(capturado.getContent().getName()).isEqualTo("myTask");
        }

        @Test
        @DisplayName("Cuerpo vacío: Spring devuelve 400")
        void executeTask_cuerpoVacio_devuelve400() throws Exception {
            // Act / Assert
            mockMvc.perform(put(BASE + "/execute")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(jobFacade);
        }
    }

    // ==================================================================
    // PUT /executeByMonth/{year}/{month}
    // ==================================================================

    @Nested
    @DisplayName("PUT /executeByMonth/{year}/{month}")
    class ExecuteByMonthTests {

        @Test
        @DisplayName("Construye TaskDTO con name=importByMonth, year y month; publica y devuelve 200")
        void executeByMonth_publicaTaskCorrecta() throws Exception {
            // Act / Assert
            mockMvc.perform(put(BASE + "/executeByMonth/{year}/{month}", 2024, 6))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(IN_PROGRESS));

            verify(jobFacade).publishJob(jobCaptor.capture());
            TaskDTO content = (TaskDTO) jobCaptor.getValue().getContent();
            assertThat(content.getName()).isEqualTo("importByMonth");
            assertThat(content.getYear()).isEqualTo(2024);
            assertThat(content.getMonth()).isEqualTo(6);
        }

        @Test
        @DisplayName("Mes inválido (no numérico): Spring devuelve 400")
        void executeByMonth_mesNoNumerico_devuelve400() throws Exception {
            // Act / Assert
            mockMvc.perform(put(BASE + "/executeByMonth/{year}/{month}", 2024, "abc"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(jobFacade);
        }
    }

    // ==================================================================
    // PUT /executeFromDate
    // ==================================================================

    @Nested
    @DisplayName("PUT /executeFromDate")
    class ExecuteFromDateTests {

        @Test
        @DisplayName("Fecha válida: construye TaskDTO con name=importFromDate y la fecha, publica y devuelve 200")
        void executeFromDate_fechaValida_publicaTaskCorrecta() throws Exception {
            // Arrange
            String fecha = "2024-06-15";

            // Act / Assert
            mockMvc.perform(put(BASE + "/executeFromDate")
                            .param("fecha", fecha))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(IN_PROGRESS));

            verify(jobFacade).publishJob(jobCaptor.capture());
            TaskDTO content = (TaskDTO) jobCaptor.getValue().getContent();
            assertThat(content.getName()).isEqualTo("importFromDate");
            assertThat(content.getDate()).isEqualTo(fecha);
        }

        @Test
        @DisplayName("Falta el parámetro 'fecha': Spring devuelve 400")
        void executeFromDate_sinParametro_devuelve400() throws Exception {
            // Act / Assert
            mockMvc.perform(put(BASE + "/executeFromDate"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(jobFacade);
        }
    }

    // ==================================================================
    // PUT /recalculate/{year}
    // ==================================================================

    @Nested
    @DisplayName("PUT /recalculate/{year}")
    class RecalculateTests {

        @Test
        @DisplayName("Año válido: construye TaskDTO con name=recalculateByYear y el año")
        void recalculate_anioValido_publicaTaskCorrecta() throws Exception {
            // Act / Assert
            mockMvc.perform(put(BASE + "/recalculate/{year}", 2024))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(IN_PROGRESS));

            verify(jobFacade).publishJob(jobCaptor.capture());
            TaskDTO content = (TaskDTO) jobCaptor.getValue().getContent();
            assertThat(content.getName()).isEqualTo("recalculateByYear");
            assertThat(content.getYear()).isEqualTo(2024);
        }

        @Test
        @DisplayName("Año no numérico: Spring devuelve 400")
        void recalculate_anioNoNumerico_devuelve400() throws Exception {
            // Act / Assert
            mockMvc.perform(put(BASE + "/recalculate/{year}", "xyz"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(jobFacade);
        }
    }

    // ==================================================================
    // GET /scheduling
    // ==================================================================

    @Nested
    @DisplayName("GET /scheduling")
    class SchedulingTests {

        @Test
        @DisplayName("Cron no vacío: devuelve 200 con el cron")
        void scheduling_cronNoVacio_devuelve200() throws Exception {
            // Arrange
            when(jobService.getJobScheduling("jobA")).thenReturn("0 0 * * * *");

            // Act / Assert
            mockMvc.perform(get(BASE + "/scheduling").param("name", "jobA"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.scheduling").value("0 0 * * * *"));

            verify(jobService).getJobScheduling("jobA");
        }

        @Test
        @DisplayName("Cron null: devuelve 404")
        void scheduling_cronNull_devuelve404() throws Exception {
            // Arrange
            when(jobService.getJobScheduling("jobA")).thenReturn(null);

            // Act / Assert
            mockMvc.perform(get(BASE + "/scheduling").param("name", "jobA"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Cron en blanco: devuelve 404")
        void scheduling_cronEnBlanco_devuelve404() throws Exception {
            // Arrange
            when(jobService.getJobScheduling("jobA")).thenReturn("   ");

            // Act / Assert
            mockMvc.perform(get(BASE + "/scheduling").param("name", "jobA"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Falta el parámetro 'name': Spring devuelve 400")
        void scheduling_sinParametro_devuelve400() throws Exception {
            // Act / Assert
            mockMvc.perform(get(BASE + "/scheduling"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(jobService);
        }
    }

    // ==================================================================
    // GET ""
    // ==================================================================

    @Nested
    @DisplayName("GET /")
    class GetJobsTests {

        @Test
        @DisplayName("Devuelve 200 con la lista de jobs")
        void getJobs_devuelve200ConLista() throws Exception {
            // Arrange
            when(jobService.getAllJobs()).thenReturn(List.of("jobA", "jobB"));

            // Act / Assert
            mockMvc.perform(get(BASE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jobs").isArray())
                    .andExpect(jsonPath("$.jobs.length()").value(2))
                    .andExpect(jsonPath("$.jobs[0]").value("jobA"))
                    .andExpect(jsonPath("$.jobs[1]").value("jobB"));

            verify(jobService).getAllJobs();
        }

        @Test
        @DisplayName("Sin jobs: devuelve 200 con array vacío")
        void getJobs_sinJobs_devuelveArrayVacio() throws Exception {
            // Arrange
            when(jobService.getAllJobs()).thenReturn(Collections.emptyList());

            // Act / Assert
            mockMvc.perform(get(BASE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jobs").isArray())
                    .andExpect(jsonPath("$.jobs").isEmpty());
        }
    }
}
