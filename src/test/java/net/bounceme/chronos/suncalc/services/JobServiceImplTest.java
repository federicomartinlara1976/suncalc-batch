package net.bounceme.chronos.suncalc.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
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
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import net.bounceme.chronos.suncalc.model.ExecutionResult;
import net.bounceme.chronos.suncalc.services.impl.JobServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobServiceImpl - orquestación de jobs Spring Batch")
class JobServiceImplTest {

    private static final String TASK_FAILED = "La tarea ha fallado";
    private static final String JOB_NAME = "miJob";

    @Mock private ApplicationContext ctx;
    @Mock private JobLauncher jobLauncher;
    @Mock private JobExplorer jobExplorer;
    @Mock private Environment env;

    @InjectMocks private JobServiceImpl service;

    @Captor private ArgumentCaptor<JobParameters> paramsCaptor;

    // ==================================================================
    // Helpers
    // ==================================================================

    /**
     * Prepara el escenario común: ctx devuelve un Job, jobLauncher devuelve un
     * JobExecution con el ExitStatus indicado.
     */
    private void stubRun(ExitStatus exitStatus) throws Exception {
        Job job = mock(Job.class);
        JobExecution exec = mock(JobExecution.class);
        when(ctx.getBean(JOB_NAME, Job.class)).thenReturn(job);
        when(jobLauncher.run(eq(job), any(JobParameters.class))).thenReturn(exec);
        when(exec.getExitStatus()).thenReturn(exitStatus);
    }

    // ==================================================================
    // run(String)
    // ==================================================================

    @Nested
    @DisplayName("run(String)")
    class RunSoloNombre {

        @Test
        @DisplayName("ExitStatus != FAILED: devuelve ExecutionResult con la descripción del job")
        void run_nombre_exito_devuelveDescripcion() throws Exception {
            // Arrange
            ExitStatus completed = new ExitStatus("COMPLETED", "Todo ok");
            stubRun(completed);

            // Act
            ExecutionResult result = service.run(JOB_NAME);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getExitStatus()).isEqualTo(completed);
            assertThat(result.getMessage()).isEqualTo("Todo ok");

            verify(jobLauncher).run(any(Job.class), paramsCaptor.capture());
            JobParameters params = paramsCaptor.getValue();
            assertThat(params.getDate("date")).isNotNull();
        }

        @Test
        @DisplayName("ExitStatus == FAILED: devuelve ExecutionResult con mensaje TASK_FAILED")
        void run_nombre_fallo_devuelveMensajeDeTareaFallida() throws Exception {
            // Arrange
            stubRun(ExitStatus.FAILED);

            // Act
            ExecutionResult result = service.run(JOB_NAME);

            // Assert
            assertThat(result.getExitStatus()).isEqualTo(ExitStatus.FAILED);
            assertThat(result.getMessage()).isEqualTo(TASK_FAILED);

            verify(jobLauncher).run(any(Job.class), any(JobParameters.class));
        }
    }

    // ==================================================================
    // run(String, String)
    // ==================================================================

    @Nested
    @DisplayName("run(String, String)")
    class RunConFecha {

        @Test
        @DisplayName("Éxito: incluye el parámetro 'fecha' de tipo String")
        void run_fecha_exito_incluyeParametroFecha() throws Exception {
            // Arrange
            stubRun(new ExitStatus("COMPLETED", "ok"));

            // Act
            ExecutionResult result = service.run(JOB_NAME, "2024-06-15");

            // Assert
            assertThat(result).isNotNull();
            verify(jobLauncher).run(any(Job.class), paramsCaptor.capture());
            JobParameters params = paramsCaptor.getValue();
            assertThat(params.getString("fecha")).isEqualTo("2024-06-15");
            assertThat(params.getDate("date")).isNotNull();
        }

        @Test
        @DisplayName("Fallo: devuelve TASK_FAILED")
        void run_fecha_fallo_devuelveMensajeTareaFallida() throws Exception {
            // Arrange
            stubRun(ExitStatus.FAILED);

            // Act
            ExecutionResult result = service.run(JOB_NAME, "2024-06-15");

            // Assert
            assertThat(result.getExitStatus()).isEqualTo(ExitStatus.FAILED);
            assertThat(result.getMessage()).isEqualTo(TASK_FAILED);
        }
    }

    // ==================================================================
    // run(String, Integer)
    // ==================================================================

    @Nested
    @DisplayName("run(String, Integer)")
    class RunConAnio {

        @Test
        @DisplayName("Éxito: incluye el parámetro 'year' de tipo Integer")
        void run_anio_exito_incluyeParametroYear() throws Exception {
        	// Arrange
            stubRun(new ExitStatus("COMPLETED", "ok"));

            // Act
            service.run(JOB_NAME, 2024);

            // Assert
            verify(jobLauncher).run(any(Job.class), paramsCaptor.capture());
            JobParameters params = paramsCaptor.getValue();
            
            Object yearValue = params.getParameter("year").getValue();
            assertThat(yearValue)
            	.isInstanceOf(Integer.class)
            	.isEqualTo(2024);
            
            assertThat(paramsCaptor.getValue().getDate("date")).isNotNull();
        }

        @Test
        @DisplayName("Fallo: devuelve TASK_FAILED")
        void run_anio_fallo_devuelveMensajeTareaFallida() throws Exception {
            // Arrange
            stubRun(ExitStatus.FAILED);

            // Act
            ExecutionResult result = service.run(JOB_NAME, 2024);

            // Assert
            assertThat(result.getMessage()).isEqualTo(TASK_FAILED);
        }
    }

    // ==================================================================
    // run(String, Integer, Integer)
    // ==================================================================

    @Nested
    @DisplayName("run(String, Integer, Integer)")
    class RunConAnioYMes {

        @Test
        @DisplayName("Éxito: incluye 'year' y 'month' como Integer")
        void run_anioMes_exito_incluyeYearYMonth() throws Exception {
            // Arrange
            stubRun(new ExitStatus("COMPLETED", "ok"));

            // Act
            service.run(JOB_NAME, 2024, 6);

            // Assert
            verify(jobLauncher).run(any(Job.class), paramsCaptor.capture());
            JobParameters params = paramsCaptor.getValue();
            
            Object yearValue = params.getParameter("year").getValue();
            assertThat(yearValue)
            	.isInstanceOf(Integer.class)
            	.isEqualTo(2024);
            
            Object monthValue = params.getParameter("month").getValue();
            assertThat(monthValue)
            	.isInstanceOf(Integer.class)
            	.isEqualTo(6);
            
            assertThat(params.getDate("date")).isNotNull();
        }

        @Test
        @DisplayName("Fallo: devuelve TASK_FAILED")
        void run_anioMes_fallo_devuelveMensajeTareaFallida() throws Exception {
            // Arrange
            stubRun(ExitStatus.FAILED);

            // Act
            ExecutionResult result = service.run(JOB_NAME, 2024, 6);

            // Assert
            assertThat(result.getMessage()).isEqualTo(TASK_FAILED);
        }
    }

    // ==================================================================
    // getLastJobInstance
    // ==================================================================

    @Nested
    @DisplayName("getLastJobInstance")
    class GetLastJobInstanceTests {

        @Test
        @DisplayName("Lista con elementos: devuelve el primero")
        void getLastJobInstance_listaConElementos_devuelvePrimero() {
            // Arrange
            JobInstance primera = new JobInstance(1L, JOB_NAME);
            JobInstance segunda = new JobInstance(2L, JOB_NAME);
            when(jobExplorer.getJobInstances(JOB_NAME, 0, 10))
                    .thenReturn(List.of(primera, segunda));

            // Act
            JobInstance result = service.getLastJobInstance(JOB_NAME);

            // Assert
            assertThat(result).isSameAs(primera);
            verify(jobExplorer).getJobInstances(JOB_NAME, 0, 10);
        }

        @Test
        @DisplayName("Lista vacía: devuelve null")
        void getLastJobInstance_listaVacia_devuelveNull() {
            // Arrange
            when(jobExplorer.getJobInstances(JOB_NAME, 0, 10))
                    .thenReturn(Collections.emptyList());

            // Act
            JobInstance result = service.getLastJobInstance(JOB_NAME);

            // Assert
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Lista null: devuelve null (CollectionUtils.isNotEmpty)")
        void getLastJobInstance_listaNull_devuelveNull() {
            // Arrange
            when(jobExplorer.getJobInstances(JOB_NAME, 0, 10)).thenReturn(null);

            // Act
            JobInstance result = service.getLastJobInstance(JOB_NAME);

            // Assert
            assertThat(result).isNull();
        }
    }

    // ==================================================================
    // getJobNames
    // ==================================================================

    @Nested
    @DisplayName("getJobNames")
    class GetJobNamesTests {

        @Test
        @DisplayName("Delega en jobExplorer.getJobNames()")
        void getJobNames_delegaEnExplorer() {
            // Arrange
            when(jobExplorer.getJobNames()).thenReturn(List.of("jobA", "jobB"));

            // Act
            List<String> result = service.getJobNames();

            // Assert
            assertThat(result).containsExactly("jobA", "jobB");
            verify(jobExplorer).getJobNames();
        }

        @Test
        @DisplayName("Lista vacía: devuelve lista vacía")
        void getJobNames_listaVacia_devuelveVacio() {
            // Arrange
            when(jobExplorer.getJobNames()).thenReturn(Collections.emptyList());

            // Act
            List<String> result = service.getJobNames();

            // Assert
            assertThat(result).isEmpty();
        }
    }

    // ==================================================================
    // getJobScheduling
    // ==================================================================

    @Nested
    @DisplayName("getJobScheduling")
    class GetJobSchedulingTests {

        @Test
        @DisplayName("Job existente: devuelve la propiedad application.<name>.cron")
        void getJobScheduling_jobExistente_devuelveProperty() {
            // Arrange
            Job job = mock(Job.class);
            when(ctx.getBean(JOB_NAME, Job.class)).thenReturn(job);
            when(env.getProperty("application." + JOB_NAME + ".cron"))
                    .thenReturn("0 0 * * * *");

            // Act
            String result = service.getJobScheduling(JOB_NAME);

            // Assert
            assertThat(result).isEqualTo("0 0 * * * *");
            verify(env).getProperty("application." + JOB_NAME + ".cron");
        }

        @Test
        @DisplayName("Propiedad no definida: devuelve null")
        void getJobScheduling_propiedadAusente_devuelveNull() {
            // Arrange
            Job job = mock(Job.class);
            when(ctx.getBean(JOB_NAME, Job.class)).thenReturn(job);
            when(env.getProperty("application." + JOB_NAME + ".cron")).thenReturn(null);

            // Act
            String result = service.getJobScheduling(JOB_NAME);

            // Assert
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("ctx.getBean devuelve null: Assert.notNull lanza IllegalArgumentException")
        void getJobScheduling_jobNull_lanzaIllegalArgumentException() {
            // Arrange
            when(ctx.getBean(JOB_NAME, Job.class)).thenReturn(null);

            // Act / Assert
            assertThatThrownBy(() -> service.getJobScheduling(JOB_NAME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("job null");

            verify(env, never()).getProperty(any());
        }
    }

    // ==================================================================
    // getAllJobs
    // ==================================================================

    @Nested
    @DisplayName("getAllJobs")
    class GetAllJobsTests {

        @Test
        @DisplayName("Filtra solo los beans que son Job")
        void getAllJobs_filtraSoloJobs() {
            // Arrange
            Job jobA = mock(Job.class);
            Job jobB = mock(Job.class);
            when(ctx.getBeanDefinitionNames())
                    .thenReturn(new String[]{ "jobA", "noJob", "jobB", "otroBean" });
            when(ctx.getBean("jobA")).thenReturn(jobA);
            when(ctx.getBean("noJob")).thenReturn("no soy un Job");
            when(ctx.getBean("jobB")).thenReturn(jobB);
            when(ctx.getBean("otroBean")).thenReturn(42);

            // Act
            List<String> result = service.getAllJobs();

            // Assert
            assertThat(result).containsExactly("jobA", "jobB");
            verify(ctx, times(4)).getBean(any(String.class));
        }

        @Test
        @DisplayName("Sin beans: devuelve lista vacía")
        void getAllJobs_sinBeans_devuelveVacio() {
            // Arrange
            when(ctx.getBeanDefinitionNames()).thenReturn(new String[0]);

            // Act
            List<String> result = service.getAllJobs();

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Ningún bean es Job: devuelve lista vacía")
        void getAllJobs_ningunJob_devuelveVacio() {
            // Arrange
            when(ctx.getBeanDefinitionNames()).thenReturn(new String[]{ "a", "b" });
            when(ctx.getBean("a")).thenReturn("x");
            when(ctx.getBean("b")).thenReturn(123);

            // Act
            List<String> result = service.getAllJobs();

            // Assert
            assertThat(result).isEmpty();
        }
    }
}