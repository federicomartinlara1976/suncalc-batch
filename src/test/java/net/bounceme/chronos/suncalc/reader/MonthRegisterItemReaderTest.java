package net.bounceme.chronos.suncalc.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ExecutionContext;

import net.bounceme.chronos.suncalc.model.Execution;
import net.bounceme.chronos.suncalc.model.TimeData;
import net.bounceme.chronos.suncalc.repository.ExecutionsRepository;
import net.bounceme.chronos.suncalc.support.SuncalcHelper;
import net.bounceme.chronos.suncalc.support.processor.DocumentProcessor;

@ExtendWith(MockitoExtension.class)
@DisplayName("MonthRegisterItemReader - lector por mes completo")
class MonthRegisterItemReaderTest {

    private static final String MONTH_KEY = "month";
    private static final String YEAR_KEY = "year";

    @Mock private DocumentProcessor documentProcessor;
    @Mock private ExecutionsRepository executionsRepository;
    @Mock private JobExecution jobExecution;

    @InjectMocks
    private MonthRegisterItemReader reader;

    private MockedStatic<SuncalcHelper> suncalcHelperStatic;

    @BeforeEach
    void setUp() {
        suncalcHelperStatic = mockStatic(SuncalcHelper.class);
        reader.jobExecution = jobExecution;

        // normalize: pad a 2 dígitos
        suncalcHelperStatic.when(() -> SuncalcHelper.normalize(anyInt()))
                .thenAnswer(inv -> String.format("%02d", ((Number) inv.getArgument(0)).intValue()));
    }

    @AfterEach
    void tearDown() {
        suncalcHelperStatic.close();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void stubParams(Integer year, Integer month) {
        JobParameters params = mock(JobParameters.class);
        JobParameter<Integer> yearParam  = (year  != null) ? new JobParameter<>(year,  Integer.class) : null;
        JobParameter<Integer> monthParam = (month != null) ? new JobParameter<>(month, Integer.class) : null;
        lenient().doReturn(yearParam).when(params).getParameter(YEAR_KEY);
        lenient().doReturn(monthParam).when(params).getParameter(MONTH_KEY);
        when(jobExecution.getJobParameters()).thenReturn(params);
    }

    private void stubCurrentDate(Integer year, Integer month, Integer day) {
        suncalcHelperStatic.when(() -> SuncalcHelper.obtenerAnio(any(Date.class)))
                .thenReturn(year);
        suncalcHelperStatic.when(() -> SuncalcHelper.obtenerMes(any(Date.class)))
                .thenReturn(month);
        suncalcHelperStatic.when(() -> SuncalcHelper.obtenerDia(any(Date.class)))
                .thenReturn(day);
    }

    // ==================================================================
    // Validación de parámetros
    // ==================================================================

    @Nested
    @DisplayName("Validación de parámetros")
    class ValidacionParametrosTests {

        @Test
        @DisplayName("Sin 'month' → IllegalArgumentException 'No se ha obtenido el mes'")
        void initialize_sinMonth_lanzaIAE() {
            // Arrange
            stubParams(2024, null);

            // Act / Assert
            ExecutionContext executionContext = new ExecutionContext();
            
            assertThatThrownBy(() -> reader.open(executionContext))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("No se ha obtenido el mes");

            verify(documentProcessor, never()).process(anyString());
        }

        @Test
        @DisplayName("Sin 'year' → IllegalArgumentException 'No se ha obtenido el año'")
        void initialize_sinYear_lanzaIAE() {
            // Arrange
            stubParams(null, 6);

            // Act / Assert
            ExecutionContext executionContext = new ExecutionContext();
            
            assertThatThrownBy(() -> reader.open(executionContext))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("No se ha obtenido el año");

            verify(documentProcessor, never()).process(anyString());
        }

        @Test
        @DisplayName("jobExecution null → NullPointerException")
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
    // Rama: mes y año actuales → hasta el día de hoy
    // ==================================================================

    @Nested
    @DisplayName("Mes y año actuales")
    class MesActualTests {

        @Test
        @DisplayName("mes=año actuales → diasMes = obtenerDia(now) → bucle hasta ese día")
        void initialize_mesAnioActuales_usaObtenerDia() {
            // Arrange
            int currentYear = LocalDate.now().getYear();
            int currentMonth = LocalDate.now().getMonthValue();
            stubParams(currentYear, currentMonth);
            stubCurrentDate(currentYear, currentMonth, 5); // hoy = día 5

            // Act
            reader.open(new ExecutionContext());

            // Assert
            assertThat(reader.records).hasSize(5);
            verify(documentProcessor, times(5)).process(anyString());
            verify(executionsRepository, times(5)).save(any(Execution.class));

            // La rama de "año/mes actuales" usó obtenerDia, NO getDiasDelMes
            suncalcHelperStatic.verify(() -> SuncalcHelper.obtenerDia(any(Date.class)));
            suncalcHelperStatic.verify(
                    () -> SuncalcHelper.getDiasDelMes(anyInt(), anyInt()),
                    never());
        }
    }

    // ==================================================================
    // Rama: mes o año distintos → getDiasDelMes
    // ==================================================================

    @Nested
    @DisplayName("Mes o año distintos")
    class OtroMesTests {

        @Test
        @DisplayName("Año distinto (mismo mes) → diasMes = getDiasDelMes(mes, año)")
        void initialize_anioDistinto_usaGetDiasDelMes() {
            // Arrange
            int currentMonth = LocalDate.now().getMonthValue();
            int otherYear = 1990; // nunca es el año actual
            stubParams(otherYear, currentMonth);
            stubCurrentDate(LocalDate.now().getYear(), currentMonth, 15);

            suncalcHelperStatic.when(() -> SuncalcHelper.getDiasDelMes(currentMonth, otherYear))
                    .thenReturn(30);

            // Act
            reader.open(new ExecutionContext());

            // Assert
            assertThat(reader.records).hasSize(30);
            suncalcHelperStatic.verify(
                    () -> SuncalcHelper.getDiasDelMes(currentMonth, otherYear));
        }

        @Test
        @DisplayName("Mes distinto (mismo año) → diasMes = getDiasDelMes(mes, año)")
        void initialize_mesDistinto_usaGetDiasDelMes() {
            // Arrange
            int currentYear = LocalDate.now().getYear();
            int otherMonth = (LocalDate.now().getMonthValue() % 12) + 1; // otro mes
            stubParams(currentYear, otherMonth);
            stubCurrentDate(currentYear, LocalDate.now().getMonthValue(), 10);

            suncalcHelperStatic.when(() -> SuncalcHelper.getDiasDelMes(otherMonth, currentYear))
                    .thenReturn(28);

            // Act
            reader.open(new ExecutionContext());

            // Assert
            assertThat(reader.records).hasSize(28);
            suncalcHelperStatic.verify(
                    () -> SuncalcHelper.getDiasDelMes(otherMonth, currentYear));
        }

        @Test
        @DisplayName("diasMes = 0 → bucle no itera, records vacío")
        void initialize_diasMesCero_ceroIteraciones() {
            // Arrange
            stubParams(1990, 6);
            stubCurrentDate(LocalDate.now().getYear(), LocalDate.now().getMonthValue(), 10);
            suncalcHelperStatic.when(() -> SuncalcHelper.getDiasDelMes(anyInt(), anyInt()))
                    .thenReturn(0);

            // Act
            reader.open(new ExecutionContext());

            // Assert
            assertThat(reader.records).isEmpty();
            verify(documentProcessor, never()).process(anyString());
            verify(executionsRepository, never()).save(any());
        }
    }

    // ==================================================================
    // existsById: skip vs process
    // ==================================================================

    @Nested
    @DisplayName("existsById: skip vs process")
    class ExistsByIdTests {

        private void stubSingleDay() {
            stubParams(1990, 6);
            stubCurrentDate(LocalDate.now().getYear(), LocalDate.now().getMonthValue(), 10);
            suncalcHelperStatic.when(() -> SuncalcHelper.getDiasDelMes(anyInt(), anyInt()))
                    .thenReturn(1);
        }

        @Test
        @DisplayName("existsById = true → NO se procesa ni se guarda")
        void initialize_existsTrue_skip() {
            // Arrange
            stubSingleDay();
            when(executionsRepository.existsById(anyString())).thenReturn(true);

            // Act
            reader.open(new ExecutionContext());

            // Assert
            assertThat(reader.records).isEmpty();
            verify(documentProcessor, never()).process(anyString());
            verify(executionsRepository, never()).save(any());
        }

        @Test
        @DisplayName("existsById = false → se procesa y se guarda")
        void initialize_existsFalse_procesa() {
            // Arrange
            stubSingleDay();
            when(executionsRepository.existsById(anyString())).thenReturn(false);
            when(documentProcessor.process(anyString())).thenReturn(new TimeData());

            // Act
            reader.open(new ExecutionContext());

            // Assert
            assertThat(reader.records).hasSize(1);
            verify(documentProcessor).process("1990-06-01");
            verify(executionsRepository).save(any(Execution.class));
        }

        @Test
        @DisplayName("Mezcla: día 1 existe, día 2 no → 1 process, 1 save")
        void initialize_mezclaExistencias_procesaSoloLosNuevos() {
            // Arrange
            stubParams(1990, 6);
            stubCurrentDate(LocalDate.now().getYear(), LocalDate.now().getMonthValue(), 10);
            suncalcHelperStatic.when(() -> SuncalcHelper.getDiasDelMes(anyInt(), anyInt()))
                    .thenReturn(2);
            when(executionsRepository.existsById("1990-06-01")).thenReturn(true);
            when(executionsRepository.existsById("1990-06-02")).thenReturn(false);
            when(documentProcessor.process("1990-06-02")).thenReturn(new TimeData());

            // Act
            reader.open(new ExecutionContext());

            // Assert
            assertThat(reader.records).hasSize(1);
            verify(documentProcessor, times(1)).process("1990-06-02");
            verify(executionsRepository, times(1)).save(any(Execution.class));
        }
    }

    // ==================================================================
    // Formato de sDate y persistencia
    // ==================================================================

    @Nested
    @DisplayName("Formato de sDate y persistencia")
    class FormatoYPersistenciaTests {

        @Test
        @DisplayName("sDate = 'YYYY-MM-DD' con padding vía normalize")
        void initialize_formatoFechaConPadding() {
            // Arrange
            stubParams(1990, 6);
            stubCurrentDate(LocalDate.now().getYear(), LocalDate.now().getMonthValue(), 10);
            suncalcHelperStatic.when(() -> SuncalcHelper.getDiasDelMes(anyInt(), anyInt()))
                    .thenReturn(3);
            when(documentProcessor.process(anyString())).thenReturn(new TimeData());

            ArgumentCaptor<Execution> captor = ArgumentCaptor.forClass(Execution.class);

            // Act
            reader.open(new ExecutionContext());

            // Assert
            verify(executionsRepository, times(3)).save(captor.capture());
            assertThat(captor.getAllValues())
                    .extracting(Execution::getId)
                    .containsExactly("1990-06-01", "1990-06-02", "1990-06-03");
            assertThat(captor.getAllValues())
                    .extracting(Execution::getValue)
                    .containsOnly(1);
        }
    }

    // ==================================================================
    // Reset de records
    // ==================================================================

    @Nested
    @DisplayName("Reset de 'records'")
    class ResetTests {

        @Test
        @DisplayName("initialize() vacía records antes de repoblarlo")
        void initialize_vaciaRecordsAntesDeRepoblar() {
            // Arrange
            reader.records.add(new TimeData()); // residuo previo
            stubParams(1990, 6);
            stubCurrentDate(LocalDate.now().getYear(), LocalDate.now().getMonthValue(), 10);
            suncalcHelperStatic.when(() -> SuncalcHelper.getDiasDelMes(anyInt(), anyInt()))
                    .thenReturn(1);
            when(executionsRepository.existsById(anyString())).thenReturn(false);
            when(documentProcessor.process(anyString())).thenReturn(new TimeData());

            // Act
            reader.open(new ExecutionContext());

            // Assert
            assertThat(reader.records).hasSize(1); // el residuo desaparece
        }
    }
}