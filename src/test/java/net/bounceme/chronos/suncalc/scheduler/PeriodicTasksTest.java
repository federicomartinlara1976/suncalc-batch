package net.bounceme.chronos.suncalc.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.bounceme.chronos.suncalc.dto.JobDTO;
import net.bounceme.chronos.suncalc.dto.TaskDTO;
import net.bounceme.chronos.suncalc.facade.JobFacade;

@ExtendWith(MockitoExtension.class)
@DisplayName("PeriodicTasks - scheduler que publica tareas periódicas")
class PeriodicTasksTest {

    private static final String TASK_NAME = "importTimes";

    @Mock private JobFacade jobFacade;

    @InjectMocks
    private PeriodicTasks periodicTasks;

    // ==================================================================
    // importTimesTask
    // ==================================================================

    @Nested
    @DisplayName("importTimesTask")
    class ImportTimesTaskTests {

        @Test
        @DisplayName("Publica un JobDTO con TaskDTO.name='importTimes'")
        void importTimesTask_publicaJobDTO() {
            // Arrange
            @SuppressWarnings("rawtypes")
            ArgumentCaptor<JobDTO> captor = ArgumentCaptor.forClass(JobDTO.class);

            // Act
            periodicTasks.importTimesTask();

            // Assert
            verify(jobFacade).publishJob(captor.capture());
            JobDTO<?> dto = captor.getValue();

            assertThat(dto).isNotNull();
            assertThat(dto.getContent()).isInstanceOf(TaskDTO.class);

            TaskDTO content = (TaskDTO) dto.getContent();
            assertThat(content.getName()).isEqualTo(TASK_NAME);
        }

        @Test
        @DisplayName("El JobDTO publicado es una instancia nueva en cada invocación")
        void importTimesTask_instanciaNuevaCadaVez() {
            // Arrange
            @SuppressWarnings("rawtypes")
            ArgumentCaptor<JobDTO> captor = ArgumentCaptor.forClass(JobDTO.class);

            // Act
            periodicTasks.importTimesTask();
            periodicTasks.importTimesTask();

            // Assert
            verify(jobFacade, times(2)).publishJob(captor.capture());
            JobDTO<?> first = captor.getAllValues().get(0);
            JobDTO<?> second = captor.getAllValues().get(1);

            assertThat(first).isNotSameAs(second);
            assertThat(first.getContent()).isNotSameAs(second.getContent());
        }

        @Test
        @DisplayName("Solo invoca publishJob (no hay otras interacciones)")
        void importTimesTask_soloPublishJob() {
            // Act
            periodicTasks.importTimesTask();

            // Assert
            verify(jobFacade).publishJob(org.mockito.ArgumentMatchers.any());
            verifyNoMoreInteractions(jobFacade);
        }

        @Test
        @DisplayName("Content no null y con nombre esperado")
        void importTimesTask_contentNoNull() {
            // Arrange
            @SuppressWarnings("rawtypes")
            ArgumentCaptor<JobDTO> captor = ArgumentCaptor.forClass(JobDTO.class);

            // Act
            periodicTasks.importTimesTask();

            // Assert
            verify(jobFacade).publishJob(captor.capture());
            assertThat(captor.getValue().getContent()).isNotNull();
        }
    }
}