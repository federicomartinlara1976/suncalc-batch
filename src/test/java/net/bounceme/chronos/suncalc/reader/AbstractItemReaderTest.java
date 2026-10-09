package net.bounceme.chronos.suncalc.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

import net.bounceme.chronos.suncalc.model.TimeData;

@ExtendWith(MockitoExtension.class)
@DisplayName("AbstractItemReader - comportamiento común de lectores")
class AbstractItemReaderTest {

    @Mock private StepExecution stepExecution;
    @Mock private JobExecution jobExecution;

    private TestItemReader reader;

    @BeforeEach
    void setUp() {
        reader = new TestItemReader();
    }

    // ==================================================================
    // Estado inicial
    // ==================================================================

    @Nested
    @DisplayName("Estado inicial")
    class EstadoInicialTests {

        @Test
        @DisplayName("records arranca vacío y no nulo")
        void estadoInicial_recordsVacio() {
            assertThat(reader.records).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("jobExecution arranca a null hasta que se llama beforeStep")
        void estadoInicial_jobExecutionNull() {
            assertThat(reader.jobExecution).isNull();
        }

        @Test
        @DisplayName("read() sobre records vacío devuelve null sin explotar")
        void estadoInicial_readVacio_devuelveNull() {
            // Act
            TimeData result = reader.read();

            // Assert
            assertThat(result).isNull();
        }
    }

    // ==================================================================
    // beforeStep
    // ==================================================================

    @Nested
    @DisplayName("beforeStep")
    class BeforeStepTests {

        @Test
        @DisplayName("Asigna jobExecution a partir de stepExecution.getJobExecution()")
        void beforeStep_asignaJobExecution() {
            // Arrange
            when(stepExecution.getJobExecution()).thenReturn(jobExecution);

            // Act
            reader.beforeStep(stepExecution);

            // Assert
            assertThat(reader.jobExecution).isSameAs(jobExecution);
            verify(stepExecution).getJobExecution();
        }

        @Test
        @DisplayName("Si stepExecution.getJobExecution() es null, jobExecution queda null")
        void beforeStep_jobExecutionNull() {
            // Arrange
            when(stepExecution.getJobExecution()).thenReturn(null);

            // Act
            reader.beforeStep(stepExecution);

            // Assert
            assertThat(reader.jobExecution).isNull();
        }
    }

    // ==================================================================
    // open
    // ==================================================================

    @Nested
    @DisplayName("open")
    class OpenTests {

        @Test
        @DisplayName("Invoca initialize() exactamente una vez")
        void open_invocaInitialize() {
            // Arrange
            ExecutionContext executionContext = new ExecutionContext();

            // Act
            reader.open(executionContext);

            // Assert
            assertThat(reader.initializeInvocations).isEqualTo(1);
        }

        @Test
        @DisplayName("Cada llamada a open incrementa el contador de initialize()")
        void open_multiplesLlamadas_incrementaContador() {
            // Arrange
            ExecutionContext executionContext = new ExecutionContext();

            // Act
            reader.open(executionContext);
            reader.open(executionContext);
            reader.open(executionContext);

            // Assert
            assertThat(reader.initializeInvocations).isEqualTo(3);
        }

        @Test
        @DisplayName("No se invoca initialize() hasta que se abre el stream")
        void open_noInvocadoAntesDeOpen() {
            assertThat(reader.initializeInvocations).isZero();
        }
    }

    // ==================================================================
    // read
    // ==================================================================

    @Nested
    @DisplayName("read")
    class ReadTests {

        @Test
        @DisplayName("Con 1 registro: primera lectura lo devuelve, segunda devuelve null")
        void read_unRegistro_devuelveUnaVezYNull() {
            // Arrange
            TimeData only = new TimeData();
            reader.records.add(only);

            // Act
            TimeData first = reader.read();
            TimeData second = reader.read();

            // Assert
            assertThat(first).isSameAs(only);
            assertThat(second).isNull();
        }

        @Test
        @DisplayName("Con N registros: los devuelve en orden de la lista")
        void read_variosRegistros_devuelveEnOrden() {
            // Arrange
            TimeData a = new TimeData();
            TimeData b = new TimeData();
            TimeData c = new TimeData();
            reader.records.addAll(List.of(a, b, c));

            // Act / Assert
            assertThat(reader.read()).isSameAs(a);
            assertThat(reader.read()).isSameAs(b);
            assertThat(reader.read()).isSameAs(c);
        }

        @Test
        @DisplayName("Tras agotar la lista: read() devuelve null y RESETEA el índice")
        void read_alAgotar_devuelveNullYResetea() {
            // Arrange
            TimeData a = new TimeData();
            TimeData b = new TimeData();
            reader.records.addAll(List.of(a, b));

            // Act
            reader.read(); // a
            reader.read(); // b
            TimeData exhausted = reader.read(); // null + reset

            // Assert
            assertThat(exhausted).isNull();
        }

        @Test
        @DisplayName("Tras agotar, la siguiente lectura vuelve a empezar desde el principio")
        void read_alAgotarVuelveAEmpezarDesdeElPrincipio() {
            // Arrange
            TimeData a = new TimeData();
            TimeData b = new TimeData();
            reader.records.addAll(List.of(a, b));

            // Act: primera pasada
            reader.read(); // a
            reader.read(); // b
            assertThat(reader.read()).isNull(); // reset

            // Segunda pasada: debe empezar de nuevo por 'a'
            TimeData primeroSegundaPasada = reader.read();

            // Assert
            assertThat(primeroSegundaPasada).isSameAs(a);
        }

        @Test
        @DisplayName("Con records vacío, cada read() devuelve null y el índice sigue en 0")
        void read_vacioSiempreDevuelveNull() {
            // Act / Assert
            assertThat(reader.read()).isNull();
            assertThat(reader.read()).isNull();
            assertThat(reader.read()).isNull();
        }

        @Test
        @DisplayName("Si se añaden registros tras un read() vacío, se leen correctamente")
        void read_registrosAniadidosTrasLecturaVacia() {
            // Arrange
            assertThat(reader.read()).isNull();
            TimeData late = new TimeData();
            reader.records.add(late);

            // Act
            TimeData result = reader.read();

            // Assert
            assertThat(result).isSameAs(late);
        }
    }

    // ==================================================================
    // Test double
    // ==================================================================

    /**
     * Implementación mínima de {@link AbstractItemReader} que:
     * - expone records (ya es protected) para poder poblarla desde el test.
     * - cuenta cuántas veces se ha invocado initialize().
     */
    static class TestItemReader extends AbstractItemReader {

        int initializeInvocations = 0;

        @Override
        protected void initialize() {
            initializeInvocations++;
        }
    }
}
