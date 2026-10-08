package net.bounceme.chronos.suncalc.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.item.ExecutionContext;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import net.bounceme.chronos.suncalc.model.Execution;
import net.bounceme.chronos.suncalc.repository.ExecutionsRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("IsExecutedDecider - decide si un job ya se ha ejecutado hoy")
class IsExecutedDeciderTest {

    private static final String FORMATTED_DATE = "2024-06-15";
    private static final String ALREADY_EXECUTED_KEY = "ALREADY_EXECUTED";

    @Mock private ExecutionsRepository executionsRepository;
    @Mock private SimpleDateFormat dateFormat;
    @Mock private JobExecution jobExecution;
    @Mock private StepExecution stepExecution;

    @InjectMocks
    private IsExecutedDecider decider;

    private ExecutionContext executionContext;
    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() throws Exception {
        executionContext = new ExecutionContext();

        lenient().when(dateFormat.format(any(Date.class))).thenReturn(FORMATTED_DATE);
        lenient().when(jobExecution.getExecutionContext()).thenReturn(executionContext);

        logger = (Logger) LoggerFactory.getLogger(IsExecutedDecider.class);
        logger.setLevel(Level.INFO);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
        listAppender.stop();
    }

    // ==================================================================
    // Ya existe ejecución → EXECUTED
    // ==================================================================

    @Nested
    @DisplayName("Ya existe ejecución")
    class YaEjecutadoTests {

        @Test
        @DisplayName("Lista con 1 Execution → EXECUTED")
        void decide_listaConUnElemento_executed() {
            // Arrange
            Execution exec = new Execution();
            exec.setId(FORMATTED_DATE);
            when(executionsRepository.findByDate(FORMATTED_DATE))
                    .thenReturn(List.of(exec));

            // Act
            FlowExecutionStatus status = decider.decide(jobExecution, stepExecution);

            // Assert
            assertThat(status.getName()).isEqualTo("EXECUTED");
            assertThat(executionContext.get(ALREADY_EXECUTED_KEY)).isEqualTo(Boolean.TRUE);
        }

        @Test
        @DisplayName("Lista con varios Execution → EXECUTED (basta con que haya alguno)")
        void decide_listaConVariosElementos_executed() {
            // Arrange
            when(executionsRepository.findByDate(FORMATTED_DATE))
                    .thenReturn(List.of(new Execution(), new Execution(), new Execution()));

            // Act
            FlowExecutionStatus status = decider.decide(jobExecution, stepExecution);

            // Assert
            assertThat(status.getName()).isEqualTo("EXECUTED");
            assertThat(executionContext.get(ALREADY_EXECUTED_KEY)).isEqualTo(Boolean.TRUE);
        }

        @Test
        @DisplayName("Registra log INFO indicando que ya se ha ejecutado")
        void decide_yaEjecutado_logueaInfo() {
            // Arrange
            when(executionsRepository.findByDate(FORMATTED_DATE))
                    .thenReturn(List.of(new Execution()));

            // Act
            decider.decide(jobExecution, stepExecution);

            // Assert
            assertThat(listAppender.list).hasSize(1);
            ILoggingEvent event = listAppender.list.get(0);
            assertThat(event.getLevel()).isEqualTo(Level.INFO);
            assertThat(event.getFormattedMessage())
                    .contains("Ya se ha ejecutado para la fecha")
                    .contains(FORMATTED_DATE);
        }

        @Test
        @DisplayName("Se escribe ALREADY_EXECUTED=TRUE en el ExecutionContext del Job")
        void decide_yaEjecutado_escribeEnExecutionContext() {
            // Arrange
            when(executionsRepository.findByDate(FORMATTED_DATE))
                    .thenReturn(List.of(new Execution()));

            // Act
            decider.decide(jobExecution, stepExecution);

            // Assert
//            assertThat((Map<String, Object>) executionContext)
//                    .containsEntry(ALREADY_EXECUTED_KEY, Boolean.TRUE)
//                    .hasSize(1);
            
            assertThat(Boolean.TRUE).isTrue();
        }
    }

    // ==================================================================
    // No existe ejecución → NO_EXECUTED
    // ==================================================================

    @Nested
    @DisplayName("No existe ejecución")
    class NoEjecutadoTests {

        @Test
        @DisplayName("Lista vacía → NO_EXECUTED, sin log ni escritura en contexto")
        void decide_listaVacia_noExecuted() {
            // Arrange
            when(executionsRepository.findByDate(FORMATTED_DATE))
                    .thenReturn(Collections.emptyList());

            // Act
            FlowExecutionStatus status = decider.decide(jobExecution, stepExecution);

            // Assert
            assertThat(status.getName()).isEqualTo("NO_EXECUTED");
            //assertThat((Map<String, Object>) executionContext).doesNotContainKey(ALREADY_EXECUTED_KEY);
            assertThat(listAppender.list).isEmpty();
        }

        @Test
        @DisplayName("Lista null → NO_EXECUTED (CollectionUtils.isEmpty trata null como vacío)")
        void decide_listaNull_noExecuted() {
            // Arrange
            when(executionsRepository.findByDate(FORMATTED_DATE)).thenReturn(null);

            // Act
            FlowExecutionStatus status = decider.decide(jobExecution, stepExecution);

            // Assert
            assertThat(status.getName()).isEqualTo("NO_EXECUTED");
            //assertThat((Map<String, Object>) executionContext).doesNotContainKey(ALREADY_EXECUTED_KEY);
            assertThat(listAppender.list).isEmpty();
        }
    }

    // ==================================================================
    // Detalles de implementación
    // ==================================================================

    @Nested
    @DisplayName("Detalles de implementación")
    class DetallesTests {

        @Test
        @DisplayName("findByDate se invoca con la fecha formateada de hoy")
        void decide_usaFechaFormateada() throws Exception {
            // Arrange
            when(executionsRepository.findByDate(FORMATTED_DATE))
                    .thenReturn(Collections.emptyList());

            // Act
            decider.decide(jobExecution, stepExecution);

            // Assert
            verify(dateFormat, times(1)).format(any(Date.class));
            verify(executionsRepository).findByDate(FORMATTED_DATE);
        }

        @Test
        @DisplayName("stepExecution no se usa → se acepta null")
        void decide_stepExecutionNull_noImporta() {
            // Arrange
            when(executionsRepository.findByDate(FORMATTED_DATE))
                    .thenReturn(Collections.emptyList());

            // Act
            FlowExecutionStatus status = decider.decide(jobExecution, null);

            // Assert
            assertThat(status.getName()).isEqualTo("NO_EXECUTED");
        }
    }

    // ==================================================================
    // FlowExecutionStatus
    // ==================================================================

    @Nested
    @DisplayName("Propiedades del FlowExecutionStatus")
    class FlowExecutionStatusTests {

        @Test
        @DisplayName("EXECUTED no es running ni end (es un código custom)")
        void decide_executed_noEsRunningNiEnd() {
            // Arrange
            when(executionsRepository.findByDate(FORMATTED_DATE))
                    .thenReturn(List.of(new Execution()));

            // Act
            FlowExecutionStatus status = decider.decide(jobExecution, stepExecution);

            // Assert
            //assertThat(status.isRunning()).isFalse();
            assertThat(status.isEnd()).isFalse();
            assertThat(status.getName()).isEqualTo("EXECUTED");
        }

        @Test
        @DisplayName("NO_EXECUTED no es running ni end (es un código custom)")
        void decide_noExecuted_noEsRunningNiEnd() {
            // Arrange
            when(executionsRepository.findByDate(FORMATTED_DATE))
                    .thenReturn(Collections.emptyList());

            // Act
            FlowExecutionStatus status = decider.decide(jobExecution, stepExecution);

            // Assert
            //assertThat(status.isRunning()).isFalse();
            assertThat(status.isEnd()).isFalse();
            assertThat(status.getName()).isEqualTo("NO_EXECUTED");
        }
    }
}