package net.bounceme.chronos.suncalc.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ExecutionContext;

import net.bounceme.chronos.suncalc.model.Execution;
import net.bounceme.chronos.suncalc.model.TimeData;
import net.bounceme.chronos.suncalc.repository.ExecutionsRepository;
import net.bounceme.chronos.suncalc.support.processor.DocumentProcessor;

@ExtendWith(MockitoExtension.class)
@DisplayName("FromDateRegisterItemReader - lector por rango de fechas")
class FromDateRegisterItemReaderTest {

    private static final String DATE_PARAM = "fecha";
    private static final ZoneId ZONE = ZoneId.systemDefault();

    @Mock private DocumentProcessor documentProcessor;
    @Mock private ExecutionsRepository executionsRepository;
    @Mock private SimpleDateFormat dateFormat;

    @Mock private JobExecution jobExecution;

    @InjectMocks
    private FromDateRegisterItemReader reader;

    @BeforeEach
    void setUp() {
        reader.jobExecution = jobExecution;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Simula la cadena de parámetros del job: jobExecution.getJobParameters()
     * devuelve JobParameters con "fecha" = value.
     */
    private void stubFechaParam(String value) {
        JobParameter<String> pDate = new JobParameter<>(value, String.class);
        JobParameters params = mock(JobParameters.class);
        doReturn(pDate).when(params).getParameter(DATE_PARAM);
        when(jobExecution.getJobParameters()).thenReturn(params);
    }

    /**
     * Devuelve la representación formateada (mock) de la fecha indicada,
     * delegando en un SimpleDateFormat real para que coincida con el esperado.
     */
    private String format(LocalDate d) {
        return new SimpleDateFormat("yyyy-MM-dd")
                .format(Date.from(d.atStartOfDay(ZONE).toInstant()));
    }

    // ==================================================================
    // Fecha actual y end
    // ==================================================================

    @Nested
    @DisplayName("Cálculo del rango start..end")
    class RangoTests {

        @Test
        @DisplayName("start == hoy → 1 iteración, 1 process y 1 save")
        void initialize_startIgualAHoy_unaIteracion() throws Exception {
            // Arrange
            String hoyStr = format(LocalDate.now(ZONE));
            stubFechaParam(hoyStr);
            when(dateFormat.parse(hoyStr)).thenReturn(new Date());
            when(dateFormat.format(any(Date.class))).thenReturn(hoyStr);
            when(documentProcessor.process(hoyStr)).thenReturn(new TimeData());

            // Act
            reader.open(new ExecutionContext());

            // Assert
            assertThat(reader.records).hasSize(1);
            verify(documentProcessor, times(1)).process(hoyStr);
            verify(executionsRepository, times(1)).save(any(Execution.class));
        }

        @Test
        @DisplayName("start = hoy-2 → 3 iteraciones, 3 process y 3 saves")
        void initialize_rangoDe3Dias_tresIteraciones() throws Exception {
            // Arrange
            LocalDate hoy = LocalDate.now(ZONE);
            LocalDate start = hoy.minusDays(2);
            String startStr = format(start);

            stubFechaParam(startStr);
            when(dateFormat.parse(startStr)).thenReturn(
                    Date.from(start.atStartOfDay(ZONE).toInstant()));

            // dateFormat.format se invoca una vez por iteración
            String d1 = format(start);
            String d2 = format(start.plusDays(1));
            String d3 = format(start.plusDays(2));
            when(dateFormat.format(any(Date.class)))
                    .thenReturn(d1)
                    .thenReturn(d2)
                    .thenReturn(d3);

            when(documentProcessor.process(anyString())).thenReturn(new TimeData());

            // Act
            reader.open(new ExecutionContext());

            // Assert
            assertThat(reader.records).hasSize(3);
            verify(documentProcessor).process(d1);
            verify(documentProcessor).process(d2);
            verify(documentProcessor).process(d3);
            verify(executionsRepository, times(3)).save(any(Execution.class));
        }

        @Test
        @DisplayName("start > hoy → 0 iteraciones, sin process ni save")
        void initialize_startFuturo_ceroIteraciones() throws Exception {
            // Arrange
            LocalDate futuro = LocalDate.now(ZONE).plusDays(5);
            String futuroStr = format(futuro);
            stubFechaParam(futuroStr);
            when(dateFormat.parse(futuroStr)).thenReturn(
                    Date.from(futuro.atStartOfDay(ZONE).toInstant()));

            // Act
            reader.open(new ExecutionContext());

            // Assert
            assertThat(reader.records).isEmpty();
            verify(documentProcessor, never()).process(anyString());
            verify(executionsRepository, never()).save(any());
        }
    }

    // ==================================================================
    // Parámetro "fecha"
    // ==================================================================

    @Nested
    @DisplayName("Parámetro 'fecha' del JobParameters")
    class ParametroFechaTests {

        @Test
        @DisplayName("Sin parámetro 'fecha' → Assert.notNull lanza IllegalArgumentException")
        void initialize_sinParametroFecha_lanzaIllegalArgument() {
            // Arrange
            JobParameters params = mock(JobParameters.class);
            when(params.getParameter(DATE_PARAM)).thenReturn(null);
            when(jobExecution.getJobParameters()).thenReturn(params);

            // Act / Assert
            ExecutionContext executionContext = new ExecutionContext();
            
            assertThatThrownBy(() -> reader.open(executionContext))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("No se ha obtenido la fecha");

            verify(documentProcessor, never()).process(anyString());
            verify(executionsRepository, never()).save(any());
        }

        @Test
        @DisplayName("Si jobExecution es null → NullPointerException")
        void initialize_jobExecutionNull_lanzaNPE() {
            // Arrange
            reader.jobExecution = null;

            // Act / Assert
            ExecutionContext executionContext = new ExecutionContext();
            
            assertThatThrownBy(() -> reader.open(executionContext))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // ==================================================================
    // Persistencia de Execution
    // ==================================================================

    @Nested
    @DisplayName("Persistencia del contador de Execution")
    class PersistenciaTests {

        @Test
        @DisplayName("Cada iteración guarda un Execution con id=sDate y value=1")
        void initialize_guardaExecutionsConIdYValue() throws Exception {
            // Arrange
            LocalDate hoy = LocalDate.now(ZONE);
            LocalDate start = hoy.minusDays(1);
            String startStr = format(start);
            String hoyStr = format(hoy);

            stubFechaParam(startStr);
            when(dateFormat.parse(startStr)).thenReturn(
                    Date.from(start.atStartOfDay(ZONE).toInstant()));
            when(dateFormat.format(any(Date.class)))
                    .thenReturn(startStr)
                    .thenReturn(hoyStr);
            when(documentProcessor.process(anyString())).thenReturn(new TimeData());

            ArgumentCaptor<Execution> captor = ArgumentCaptor.forClass(Execution.class);

            // Act
            reader.open(new ExecutionContext());

            // Assert
            verify(executionsRepository, times(2)).save(captor.capture());
            assertThat(captor.getAllValues())
                    .extracting(Execution::getId)
                    .containsExactly(startStr, hoyStr);
            assertThat(captor.getAllValues())
                    .extracting(Execution::getValue)
                    .containsOnly(1);
        }
    }

    // ==================================================================
    // Reset de records entre aperturas
    // ==================================================================

    @Nested
    @DisplayName("Reseteo de 'records'")
    class ResetTests {

        @Test
        @DisplayName("initialize() vacía records antes de repoblarlo")
        void initialize_vaciaRecordsAntesDeRepoblar() throws Exception {
            // Arrange
            reader.records.add(new TimeData()); // residuo previo

            String hoyStr = format(LocalDate.now(ZONE));
            stubFechaParam(hoyStr);
            when(dateFormat.parse(hoyStr)).thenReturn(new Date());
            when(dateFormat.format(any(Date.class))).thenReturn(hoyStr);
            when(documentProcessor.process(hoyStr)).thenReturn(new TimeData());

            // Act
            reader.open(new ExecutionContext());

            // Assert
            assertThat(reader.records).hasSize(1); // el residuo fue eliminado
        }
    }

    // ==================================================================
    // Excepciones de parseo
    // ==================================================================

    @Nested
    @DisplayName("ParseException")
    class ParseExceptionTests {

        @Test
        @DisplayName("dateFormat.parse lanza ParseException → @SneakyThrows la propaga")
        void initialize_parseFalla_propagaParseException() throws Exception {
            // Arrange
            String malFormada = "no-es-fecha";
            stubFechaParam(malFormada);
            when(dateFormat.parse(malFormada))
                    .thenThrow(new ParseException("formato inválido", 0));

            // Act / Assert
            assertThatThrownBy(() -> reader.open(new ExecutionContext()))
                    .isInstanceOf(ParseException.class)
                    .hasMessageContaining("formato inválido");

            verify(documentProcessor, never()).process(anyString());
            verify(executionsRepository, never()).save(any());
        }
    }
}