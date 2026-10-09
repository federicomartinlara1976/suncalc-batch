package net.bounceme.chronos.suncalc.tasklet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import net.bounceme.chronos.suncalc.model.Differences;
import net.bounceme.chronos.suncalc.model.TimeData;
import net.bounceme.chronos.suncalc.repository.DifferencesDataRepository;
import net.bounceme.chronos.suncalc.repository.RepositoryCollectionCustom;
import net.bounceme.chronos.suncalc.repository.TimeDataRepository;
import net.bounceme.chronos.suncalc.support.SuncalcHelper;

@ExtendWith(MockitoExtension.class)
@DisplayName("CalculateDifferencesExecutionTasklet - cálculo de diferencias entre registros")
class CalculateDifferencesExecutionTaskletTest {

    private static final String COLLECTION = "suncalc-collection";
    private static final Integer YEAR = 2024;
    private static final Integer OTHER_YEAR = 2023;

    private static final String S_DESDE = "2024-01-01";
    private static final String S_DESDE_OTHER = "2023-01-01";
    private static final String S_HASTA_END_OTHER = "2023-12-31";
    private static final String S_HASTA_NOW = "2024-06-15";
    private static final String S_PREV = "2023-12-31";

    @Mock private TimeDataRepository timeDataRepository;
    @Mock private DifferencesDataRepository differencesDataRepository;
    @Mock private RepositoryCollectionCustom repositoryCollectionCustom;
    @Mock private SimpleDateFormat dateFormat;

    @Mock private StepContribution contribution;
    @Mock private ChunkContext chunkContext;
    @Mock private StepContext stepContext;

    @InjectMocks
    private CalculateDifferencesExecutionTasklet tasklet;

    private MockedStatic<SuncalcHelper> suncalcHelperStatic;

    @BeforeEach
    void setUp() {
        suncalcHelperStatic = mockStatic(SuncalcHelper.class);
        ReflectionTestUtils.setField(tasklet, "collection", COLLECTION);
    }

    @AfterEach
    void tearDown() {
        suncalcHelperStatic.close();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void stubJobParameters(Integer year) {
        Map<String, Object> params = new HashMap<>();
        params.put("year", year);
        when(chunkContext.getStepContext()).thenReturn(stepContext);
        when(stepContext.getJobParameters()).thenReturn(params);
    }

    private void stubObtenerAnio(Integer anio) {
        suncalcHelperStatic.when(() -> SuncalcHelper.obtenerAnio(any(Date.class)))
                .thenReturn(anio);
    }

    private void stubListRegistros(List<TimeData> times) {
        when(timeDataRepository.listRegistros(anyString(), anyString(), any(Sort.class)))
                .thenReturn(times);
    }

    private static TimeData newTimeData() {
        return new TimeData();
    }

    // ==================================================================
    // Rama sHasta: año actual vs año distinto
    // ==================================================================

    @Nested
    @DisplayName("Cálculo de sHasta")
    class CalculoSHastaTests {

        @Test
        @DisplayName("Año actual: sHasta = fecha formateada de hoy")
        void execute_anioActual_sHastaEsFechaActual() throws Exception {
            // Arrange
            stubJobParameters(YEAR);
            stubObtenerAnio(YEAR);
            when(dateFormat.parse(S_DESDE)).thenReturn(new Date());
            when(dateFormat.format(any(Date.class)))
                    .thenReturn(S_HASTA_NOW)  // 1ª llamada: sHasta
                    .thenReturn(S_PREV);       // 2ª llamada: sPrev
            stubListRegistros(Collections.emptyList());
            when(timeDataRepository.findById(anyString())).thenReturn(Optional.empty());

            // Act
            RepeatStatus status = tasklet.execute(contribution, chunkContext);

            // Assert
            assertThat(status).isEqualTo(RepeatStatus.FINISHED);
            verify(timeDataRepository).listRegistros(eq(S_DESDE), eq(S_HASTA_NOW), any(Sort.class));
        }

        @Test
        @DisplayName("Año distinto al actual: sHasta = 31-12 del año")
        void execute_anioNoActual_sHastaEsFinDeAnio() throws Exception {
            // Arrange
            stubJobParameters(OTHER_YEAR);
            stubObtenerAnio(YEAR);
            when(dateFormat.parse(S_DESDE_OTHER)).thenReturn(new Date());
            when(dateFormat.format(any(Date.class))).thenReturn(S_PREV); // solo sPrev
            stubListRegistros(Collections.emptyList());
            when(timeDataRepository.findById(anyString())).thenReturn(Optional.empty());

            // Act
            tasklet.execute(contribution, chunkContext);

            // Assert
            verify(timeDataRepository).listRegistros(
                    eq(S_DESDE_OTHER),
                    eq(S_HASTA_END_OTHER),
                    any(Sort.class));
        }

        @Test
        @DisplayName("listRegistros se invoca siempre ordenando por _id ASC")
        void execute_ordenacionPorIdAscendente() throws Exception {
            // Arrange
            stubJobParameters(YEAR);
            stubObtenerAnio(YEAR);
            when(dateFormat.parse(S_DESDE)).thenReturn(new Date());
            when(dateFormat.format(any(Date.class)))
                    .thenReturn(S_HASTA_NOW)
                    .thenReturn(S_PREV);
            stubListRegistros(Collections.emptyList());
            when(timeDataRepository.findById(anyString())).thenReturn(Optional.empty());

            ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);

            // Act
            tasklet.execute(contribution, chunkContext);

            // Assert
            verify(timeDataRepository).listRegistros(anyString(), anyString(), sortCaptor.capture());
            Sort.Order order = sortCaptor.getValue().getOrderFor("_id");
            assertThat(order).isNotNull();
            assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
        }
    }

    // ==================================================================
    // Sección de frontera (día anterior al 01-01)
    // ==================================================================

    @Nested
    @DisplayName("Sección de frontera (prev day)")
    class FronteraTests {

        private void commonStubs() throws ParseException {
            stubJobParameters(YEAR);
            stubObtenerAnio(YEAR);
            when(dateFormat.parse(S_DESDE)).thenReturn(new Date());
            when(dateFormat.format(any(Date.class)))
                    .thenReturn(S_HASTA_NOW)
                    .thenReturn(S_PREV);
            stubListRegistros(Collections.emptyList());
        }

        @Test
        @DisplayName("prev, current y diferencia presentes → guarda la diferencia")
        void frontera_todoPresente_guardaDiferencia() throws Exception {
            // Arrange
            commonStubs();
            TimeData prev = newTimeData();
            TimeData curr = newTimeData();
            Differences diff = mock(Differences.class);
            when(timeDataRepository.findById(S_PREV)).thenReturn(Optional.of(prev));
            when(timeDataRepository.findById(S_DESDE)).thenReturn(Optional.of(curr));
            suncalcHelperStatic.when(() -> SuncalcHelper.createDifferences(curr, prev))
                    .thenReturn(Optional.of(diff));

            // Act
            tasklet.execute(contribution, chunkContext);

            // Assert
            verify(differencesDataRepository).save(diff);
            verify(differencesDataRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("prev ausente → no se consulta current ni se guarda")
        void frontera_sinPrev_noGuarda() throws Exception {
            // Arrange
            commonStubs();
            when(timeDataRepository.findById(S_PREV)).thenReturn(Optional.empty());

            // Act
            tasklet.execute(contribution, chunkContext);

            // Assert
            verify(timeDataRepository, never()).findById(S_DESDE);
            verify(differencesDataRepository, never()).save(any());
        }

        @Test
        @DisplayName("prev presente pero current ausente → no se calcula diferencia")
        void frontera_sinCurrent_noGuarda() throws Exception {
            // Arrange
            commonStubs();
            when(timeDataRepository.findById(S_PREV)).thenReturn(Optional.of(newTimeData()));
            when(timeDataRepository.findById(S_DESDE)).thenReturn(Optional.empty());

            // Act
            tasklet.execute(contribution, chunkContext);

            // Assert
            //suncalcHelperStatic.verifyNoInteractions();
            verify(differencesDataRepository, never()).save(any());
        }

        @Test
        @DisplayName("prev y current presentes pero createDifferences devuelve Optional.empty → no guarda")
        void frontera_sinDiferencia_noGuarda() throws Exception {
            // Arrange
            commonStubs();
            TimeData prev = newTimeData();
            TimeData curr = newTimeData();
            when(timeDataRepository.findById(S_PREV)).thenReturn(Optional.of(prev));
            when(timeDataRepository.findById(S_DESDE)).thenReturn(Optional.of(curr));
            suncalcHelperStatic.when(() -> SuncalcHelper.createDifferences(curr, prev))
                    .thenReturn(Optional.empty());

            // Act
            tasklet.execute(contribution, chunkContext);

            // Assert
            verify(differencesDataRepository, never()).save(any());
        }
    }

    // ==================================================================
    // Bucle de diferencias
    // ==================================================================

    @Nested
    @DisplayName("Bucle sobre la lista de TimeData")
    class BucleTests {

        private void commonStubs(List<TimeData> times) throws ParseException {
            stubJobParameters(YEAR);
            stubObtenerAnio(YEAR);
            when(dateFormat.parse(S_DESDE)).thenReturn(new Date());
            when(dateFormat.format(any(Date.class)))
                    .thenReturn(S_HASTA_NOW)
                    .thenReturn(S_PREV);
            stubListRegistros(times);
            // Frontera: skip
            when(timeDataRepository.findById(anyString())).thenReturn(Optional.empty());
        }

        @Test
        @DisplayName("Lista vacía → no itera, no guarda")
        void bucle_listaVacia_noGuarda() throws Exception {
            // Arrange
            commonStubs(Collections.emptyList());

            // Act
            tasklet.execute(contribution, chunkContext);

            // Assert
            verify(differencesDataRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lista con 1 elemento → no itera, no guarda")
        void bucle_unElemento_noGuarda() throws Exception {
            // Arrange
            commonStubs(List.of(newTimeData()));

            // Act
            tasklet.execute(contribution, chunkContext);

            // Assert
            verify(differencesDataRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lista con 2 elementos y diferencia presente → 1 save")
        void bucle_dosElementos_guardaUnaVez() throws Exception {
            // Arrange
            commonStubs(List.of(newTimeData(), newTimeData()));
            Differences diff = mock(Differences.class);
            suncalcHelperStatic.when(() -> SuncalcHelper.createDifferences(any(), any()))
                    .thenReturn(Optional.of(diff));

            // Act
            tasklet.execute(contribution, chunkContext);

            // Assert
            verify(differencesDataRepository, times(1)).save(diff);
            suncalcHelperStatic.verify(() -> SuncalcHelper.createDifferences(any(), any()), times(1));
        }

        @Test
        @DisplayName("Lista con 2 elementos y Optional.empty → 0 saves")
        void bucle_dosElementos_sinDiferencia_noGuarda() throws Exception {
            // Arrange
            commonStubs(List.of(newTimeData(), newTimeData()));
            suncalcHelperStatic.when(() -> SuncalcHelper.createDifferences(any(), any()))
                    .thenReturn(Optional.empty());

            // Act
            tasklet.execute(contribution, chunkContext);

            // Assert
            verify(differencesDataRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lista con 3 elementos → 2 saves (n-1 iteraciones)")
        void bucle_tresElementos_dosSaves() throws Exception {
            // Arrange
            commonStubs(List.of(newTimeData(), newTimeData(), newTimeData()));
            Differences diff = mock(Differences.class);
            suncalcHelperStatic.when(() -> SuncalcHelper.createDifferences(any(), any()))
                    .thenReturn(Optional.of(diff));

            // Act
            tasklet.execute(contribution, chunkContext);

            // Assert
            verify(differencesDataRepository, times(2)).save(diff);
            suncalcHelperStatic.verify(() -> SuncalcHelper.createDifferences(any(), any()), times(2));
        }
    }

    // ==================================================================
    // Efectos colaterales comunes
    // ==================================================================

    @Nested
    @DisplayName("Efectos colaterales comunes")
    class EfectosComunesTests {

        @Test
        @DisplayName("Siempre se invoca setCollectionName con el valor inyectado")
        void execute_seteaCollectionName() throws Exception {
            // Arrange
            stubJobParameters(YEAR);
            stubObtenerAnio(YEAR);
            when(dateFormat.parse(S_DESDE)).thenReturn(new Date());
            when(dateFormat.format(any(Date.class)))
                    .thenReturn(S_HASTA_NOW)
                    .thenReturn(S_PREV);
            stubListRegistros(Collections.emptyList());
            when(timeDataRepository.findById(anyString())).thenReturn(Optional.empty());

            // Act
            tasklet.execute(contribution, chunkContext);

            // Assert
            verify(repositoryCollectionCustom).setCollectionName(COLLECTION);
        }
    }
}
