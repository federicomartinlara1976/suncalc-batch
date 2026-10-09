package net.bounceme.chronos.suncalc.tasklet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.test.util.ReflectionTestUtils;

import net.bounceme.chronos.suncalc.model.TimeData;
import net.bounceme.chronos.suncalc.repository.RepositoryCollectionCustom;
import net.bounceme.chronos.suncalc.repository.TimeDataRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetLastMarkTasklet - recuperar el último registro y publicarlo")
class GetLastMarkTaskletTest {

    private static final String COLLECTION = "suncalc-collection";
    private static final String PREV_TIME_DATA_KEY = "PREV_TIME_DATA";

    @Mock private TimeDataRepository timeDataRepository;
    @Mock private RepositoryCollectionCustom repositoryCollectionCustom;

    @Mock private StepContribution contribution;
    @Mock private ChunkContext chunkContext;
    @Mock private StepContext stepContext;
    @Mock private StepExecution stepExecution;
    @Mock private JobExecution jobExecution;

    @InjectMocks
    private GetLastMarkTasklet tasklet;

    private ExecutionContext executionContext;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tasklet, "collection", COLLECTION);

        executionContext = new ExecutionContext();

        // Cadena de mocks hasta el ExecutionContext (real)
        lenient().when(chunkContext.getStepContext()).thenReturn(stepContext);
        lenient().when(stepContext.getStepExecution()).thenReturn(stepExecution);
        lenient().when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        lenient().when(jobExecution.getExecutionContext()).thenReturn(executionContext);
    }

    // ==================================================================
    // Camino feliz
    // ==================================================================

    @Nested
    @DisplayName("Ejecución correcta")
    class EjecucionCorrectaTests {

        @Test
        @DisplayName("Lista con un elemento → publica ese elemento en PREV_TIME_DATA")
        void execute_unElemento_publicaEnExecutionContext() throws Exception {
            // Arrange
            TimeData only = new TimeData();
            when(timeDataRepository.findAll()).thenReturn(List.of(only));

            // Act
            RepeatStatus status = tasklet.execute(contribution, chunkContext);

            // Assert
            assertThat(status).isEqualTo(RepeatStatus.FINISHED);
            assertThat(executionContext.get(PREV_TIME_DATA_KEY)).isSameAs(only);
            verify(repositoryCollectionCustom).setCollectionName(COLLECTION);
            verify(timeDataRepository).findAll();
        }

        @Test
        @DisplayName("Lista con varios elementos → publica el último")
        void execute_variosElementos_publicaElUltimo() throws Exception {
            // Arrange
            TimeData primero = new TimeData();
            TimeData segundo = new TimeData();
            TimeData ultimo = new TimeData();
            when(timeDataRepository.findAll()).thenReturn(List.of(primero, segundo, ultimo));

            // Act
            RepeatStatus status = tasklet.execute(contribution, chunkContext);

            // Assert
            assertThat(status).isEqualTo(RepeatStatus.FINISHED);
            assertThat(executionContext.get(PREV_TIME_DATA_KEY)).isSameAs(ultimo);
            verify(repositoryCollectionCustom).setCollectionName(COLLECTION);
            verify(timeDataRepository).findAll();
            verifyNoMoreInteractions(repositoryCollectionCustom, timeDataRepository);
        }
    }

    // ==================================================================
    // Casos límite y errores
    // ==================================================================

    @Nested
    @DisplayName("Casos límite")
    class CasosLimiteTests {

        @Test
        @DisplayName("Lista vacía → IndexOutOfBoundsException (list.get(-1))")
        void execute_listaVacia_lanzaIndexOutOfBounds() {
            // Arrange
            when(timeDataRepository.findAll()).thenReturn(Collections.emptyList());

            // Act / Assert
            assertThatThrownBy(() -> tasklet.execute(contribution, chunkContext))
                    .isInstanceOf(IndexOutOfBoundsException.class);

            // No se ha escrito nada en el ExecutionContext
            assertThat(executionContext.containsKey(PREV_TIME_DATA_KEY)).isFalse();
        }

        @Test
        @DisplayName("Se invoca setCollectionName ANTES de findAll")
        void execute_ordenDeInvocaciones() throws Exception {
            // Arrange
            TimeData only = new TimeData();
            when(timeDataRepository.findAll()).thenReturn(List.of(only));

            // Act
            tasklet.execute(contribution, chunkContext);

            // Assert
            var inOrder = org.mockito.Mockito.inOrder(repositoryCollectionCustom, timeDataRepository);
            inOrder.verify(repositoryCollectionCustom).setCollectionName(COLLECTION);
            inOrder.verify(timeDataRepository).findAll();
        }
    }
}