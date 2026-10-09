package net.bounceme.chronos.suncalc.tasklet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.test.util.ReflectionTestUtils;

import net.bounceme.chronos.suncalc.model.Differences;
import net.bounceme.chronos.suncalc.model.TimeData;
import net.bounceme.chronos.suncalc.repository.DifferencesDataRepository;
import net.bounceme.chronos.suncalc.repository.RepositoryCollectionCustom;
import net.bounceme.chronos.suncalc.repository.TimeDataRepository;
import net.bounceme.chronos.suncalc.support.SuncalcHelper;

@ExtendWith(MockitoExtension.class)
@DisplayName("CalculateDifferenceTasklet - diferencia puntual con el día anterior")
class CalculateDifferenceTaskletTest {

    private static final String COLLECTION = "suncalc-collection";
    private static final String NEXT_TIME_DATA_KEY = "NEXT_TIME_DATA";
    private static final String ID_PREV_DATE = "2024-06-14";

    @Mock private DifferencesDataRepository differencesDataRepository;
    @Mock private TimeDataRepository timeDataRepository;
    @Mock private RepositoryCollectionCustom repositoryCollectionCustom;
    @Mock private SimpleDateFormat dateFormat;

    @Mock private StepContribution contribution;
    @Mock private ChunkContext chunkContext;
    @Mock private StepContext stepContext;

    @InjectMocks
    private CalculateDifferenceTasklet tasklet;

    private Map<String, Object> jobExecutionContext;
    private MockedStatic<SuncalcHelper> suncalcHelperStatic;

    @BeforeEach
    void setUp() {
        suncalcHelperStatic = mockStatic(SuncalcHelper.class);
        ReflectionTestUtils.setField(tasklet, "collection", COLLECTION);

        jobExecutionContext = new HashMap<>();
        when(chunkContext.getStepContext()).thenReturn(stepContext);
        when(stepContext.getJobExecutionContext()).thenReturn(jobExecutionContext);
    }

    @AfterEach
    void tearDown() {
        suncalcHelperStatic.close();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Crea un TimeData mockeado con la fecha indicada.
     */
    private TimeData timeDataWithDate(Date date) {
        TimeData td = mock(TimeData.class);
        when(td.getFecha()).thenReturn(date);
        return td;
    }

    /**
     * Prepara el NEXT_TIME_DATA en el JobExecutionContext.
     */
    private TimeData stubNextTimeData(Date fecha) {
        TimeData next = timeDataWithDate(fecha);
        jobExecutionContext.put(NEXT_TIME_DATA_KEY, next);
        return next;
    }

    // ==================================================================
    // Camino feliz: prev presente y diferencia creada
    // ==================================================================

    @Nested
    @DisplayName("Camino feliz")
    class CaminoFelizTests {

        @Test
        @DisplayName("prev encontrado y diferencia presente → guarda la diferencia")
        void execute_todoPresente_guardaDiferencia() throws Exception {
            // Arrange
            Date fecha = new Date();
            TimeData next = stubNextTimeData(fecha);
            when(dateFormat.format(any(Date.class))).thenReturn(ID_PREV_DATE);

            TimeData prev = mock(TimeData.class);
            Differences diff = mock(Differences.class);
            when(timeDataRepository.findById(ID_PREV_DATE)).thenReturn(Optional.of(prev));
            suncalcHelperStatic.when(() -> SuncalcHelper.createDifferences(next, prev))
                    .thenReturn(Optional.of(diff));

            // Act
            RepeatStatus status = tasklet.execute(contribution, chunkContext);

            // Assert
            assertThat(status).isEqualTo(RepeatStatus.FINISHED);
            verify(repositoryCollectionCustom).setCollectionName(COLLECTION);
            verify(timeDataRepository).findById(ID_PREV_DATE);
            verify(differencesDataRepository).save(diff);
            suncalcHelperStatic.verify(() -> SuncalcHelper.createDifferences(next, prev));
        }

        @Test
        @DisplayName("findById recibe la fecha del día anterior a NEXT_TIME_DATA")
        void execute_calculaDiaAnteriorCorrectamente() throws Exception {
            // Arrange
            // 2024-06-15 a las 12:00 UTC
            Date fecha = Date.from(java.time.Instant.parse("2024-06-15T12:00:00Z"));
            stubNextTimeData(fecha);
            when(dateFormat.format(any(Date.class))).thenReturn(ID_PREV_DATE);
            when(timeDataRepository.findById(anyString())).thenReturn(Optional.empty());

            ArgumentCaptor<Date> dateCaptor = ArgumentCaptor.forClass(Date.class);

            // Act
            tasklet.execute(contribution, chunkContext);

            // Assert
            verify(dateFormat).format(dateCaptor.capture());
            Date passed = dateCaptor.getValue();
            // La fecha pasada a format debe ser el día anterior a las 00:00 de la zona del sistema
            java.time.LocalDate expected = fecha.toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                    .minusDays(1);
            java.time.LocalDate actual = passed.toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
            assertThat(actual).isEqualTo(expected);
        }
    }

    // ==================================================================
    // Ramas de Optional vacío
    // ==================================================================

    @Nested
    @DisplayName("Ramas de Optional vacío")
    class OptionalVacioTests {

        @Test
        @DisplayName("prev no encontrado → no se crea diferencia ni se guarda")
        void execute_sinPrev_noGuarda() throws Exception {
            // Arrange
            Date fecha = new Date();
            stubNextTimeData(fecha);
            when(dateFormat.format(any(Date.class))).thenReturn(ID_PREV_DATE);
            when(timeDataRepository.findById(ID_PREV_DATE)).thenReturn(Optional.empty());

            // Act
            RepeatStatus status = tasklet.execute(contribution, chunkContext);

            // Assert
            assertThat(status).isEqualTo(RepeatStatus.FINISHED);
            suncalcHelperStatic.verifyNoInteractions();
            verify(differencesDataRepository, never()).save(any());
        }

        @Test
        @DisplayName("prev encontrado pero createDifferences vacío → no se guarda")
        void execute_sinDiferencia_noGuarda() throws Exception {
            // Arrange
            Date fecha = new Date();
            TimeData next = stubNextTimeData(fecha);
            when(dateFormat.format(any(Date.class))).thenReturn(ID_PREV_DATE);

            TimeData prev = mock(TimeData.class);
            when(timeDataRepository.findById(ID_PREV_DATE)).thenReturn(Optional.of(prev));
            suncalcHelperStatic.when(() -> SuncalcHelper.createDifferences(next, prev))
                    .thenReturn(Optional.empty());

            // Act
            RepeatStatus status = tasklet.execute(contribution, chunkContext);

            // Assert
            assertThat(status).isEqualTo(RepeatStatus.FINISHED);
            suncalcHelperStatic.verify(() -> SuncalcHelper.createDifferences(next, prev));
            verify(differencesDataRepository, never()).save(any());
        }
    }

    // ==================================================================
    // Casos límite / errores latentes
    // ==================================================================

    @Nested
    @DisplayName("Casos límite")
    class CasosLimiteTests {

        @Test
        @DisplayName("NEXT_TIME_DATA ausente en el ExecutionContext → NullPointerException")
        void execute_nextTimeDataAusente_lanzaNPE() {
            // Arrange
            lenient().when(dateFormat.format(any(Date.class))).thenReturn(ID_PREV_DATE);

            // Act / Assert
            assertThatThrownBy(() -> tasklet.execute(contribution, chunkContext))
                    .isInstanceOf(NullPointerException.class);

            verify(differencesDataRepository, never()).save(any());
        }

        @Test
        @DisplayName("setCollectionName se invoca ANTES de calcular la fecha")
        void execute_setCollectionNamePrimero() throws Exception {
            // Arrange
            Date fecha = new Date();
            stubNextTimeData(fecha);
            when(dateFormat.format(any(Date.class))).thenReturn(ID_PREV_DATE);
            when(timeDataRepository.findById(anyString())).thenReturn(Optional.empty());

            // Act
            tasklet.execute(contribution, chunkContext);

            // Assert
            var inOrder = org.mockito.Mockito.inOrder(
                    repositoryCollectionCustom, dateFormat, timeDataRepository);
            inOrder.verify(repositoryCollectionCustom).setCollectionName(COLLECTION);
            inOrder.verify(dateFormat).format(any(Date.class));
            inOrder.verify(timeDataRepository).findById(ID_PREV_DATE);
        }

        @Test
        @DisplayName("createDifferences recibe (nextData, prevData) en ese orden")
        void execute_ordenDeArgumentosEnCreateDifferences() throws Exception {
            // Arrange
            Date fecha = new Date();
            TimeData next = stubNextTimeData(fecha);
            when(dateFormat.format(any(Date.class))).thenReturn(ID_PREV_DATE);

            TimeData prev = mock(TimeData.class);
            Differences diff = mock(Differences.class);
            when(timeDataRepository.findById(ID_PREV_DATE)).thenReturn(Optional.of(prev));
            suncalcHelperStatic.when(() -> SuncalcHelper.createDifferences(next, prev))
                    .thenReturn(Optional.of(diff));

            // Act
            tasklet.execute(contribution, chunkContext);

            // Assert
            suncalcHelperStatic.verify(() -> SuncalcHelper.createDifferences(next, prev));
            suncalcHelperStatic.verify(
                    () -> SuncalcHelper.createDifferences(prev, next),
                    never());
        }

        @Test
        @DisplayName("save se invoca exactamente una vez cuando todo va bien")
        void execute_saveUnaVez() throws Exception {
            // Arrange
            Date fecha = new Date();
            TimeData next = stubNextTimeData(fecha);
            when(dateFormat.format(any(Date.class))).thenReturn(ID_PREV_DATE);

            TimeData prev = mock(TimeData.class);
            Differences diff = mock(Differences.class);
            when(timeDataRepository.findById(ID_PREV_DATE)).thenReturn(Optional.of(prev));
            suncalcHelperStatic.when(() -> SuncalcHelper.createDifferences(next, prev))
                    .thenReturn(Optional.of(diff));

            // Act
            tasklet.execute(contribution, chunkContext);

            // Assert
            verify(differencesDataRepository, times(1)).save(diff);
        }
    }
}
