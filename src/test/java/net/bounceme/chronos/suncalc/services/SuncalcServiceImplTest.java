package net.bounceme.chronos.suncalc.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import net.bounceme.chronos.suncalc.model.TimeData;
import net.bounceme.chronos.suncalc.repository.RepositoryCollectionCustom;
import net.bounceme.chronos.suncalc.repository.TimeDataRepository;
import net.bounceme.chronos.suncalc.services.impl.SuncalcServiceImpl;
import net.bounceme.chronos.suncalc.support.processor.DocumentProcessor;

@ExtendWith(MockitoExtension.class)
@DisplayName("SuncalcServiceImpl - servicio principal de cálculo solar")
class SuncalcServiceImplTest {

    private static final String COLLECTION = "times";
    private static final String FIXED_ID = "2024-06-15T10:00:00";

    @Mock private DocumentProcessor documentProcessor;
    @Mock private TimeDataRepository timeDataRepository;
    @Mock private RepositoryCollectionCustom repositoryCollectionCustom;
    @Mock private SimpleDateFormat dateFormat;

    @InjectMocks private SuncalcServiceImpl service;

    @Captor private ArgumentCaptor<Sort> sortCaptor;

    @BeforeEach
    void setUp() {
        // El campo @Value no lo resuelve @InjectMocks → se inyecta a mano
        ReflectionTestUtils.setField(service, "collection", COLLECTION);
    }

    // ==================================================================
    // getCurrentTimeData
    // ==================================================================

    @Nested
    @DisplayName("getCurrentTimeData")
    class GetCurrentTimeDataTests {

        @BeforeEach
        void setUp() {
            // Solo este @Nested usa dateFormat.format(...)
            when(dateFormat.format(any(Date.class))).thenReturn(FIXED_ID);
        }

        @Test
        @DisplayName("Documento existente: devuelve el TimeData del repositorio sin llamar al processor")
        void getCurrentTimeData_documentoExistente_devuelveExistenteSinProcesar() {
            // Arrange
            TimeData existente = TimeData.builder().id(FIXED_ID).status(true).build();
            when(timeDataRepository.findById(FIXED_ID)).thenReturn(Optional.of(existente));

            // Act
            TimeData result = service.getCurrentTimeData();

            // Assert
            assertThat(result).isSameAs(existente);
            verify(repositoryCollectionCustom).setCollectionName(COLLECTION);
            verify(timeDataRepository).findById(FIXED_ID);
            verify(documentProcessor, never()).process();
            verify(timeDataRepository, never()).save(any());
        }

        @Test
        @DisplayName("Documento ausente: crea uno nuevo procesándolo y lo devuelve con id, fecha y status")
        void getCurrentTimeData_documentoAusente_creaYDevuelveNuevo() {
            // Arrange
            TimeData procesado = TimeData.builder().build();
            when(timeDataRepository.findById(FIXED_ID)).thenReturn(Optional.empty());
            when(documentProcessor.process()).thenReturn(procesado);

            // Act
            TimeData result = service.getCurrentTimeData();

            // Assert
            assertThat(result).isSameAs(procesado);
            assertThat(result.getId()).isEqualTo(FIXED_ID);
            assertThat(result.getFecha()).isNotNull();
            assertThat(result.getStatus()).isTrue();

            verify(repositoryCollectionCustom).setCollectionName(COLLECTION);
            verify(timeDataRepository).findById(FIXED_ID);
            verify(documentProcessor).process();
            verify(dateFormat).format(any(Date.class));
        }

        @Test
        @DisplayName("El collection name se setea ANTES de consultar el repositorio")
        void getCurrentTimeData_seteaCollectionAntesDeConsultar() {
            // Arrange
            when(timeDataRepository.findById(FIXED_ID)).thenReturn(Optional.empty());
            when(documentProcessor.process()).thenReturn(TimeData.builder().build());

            // Act
            service.getCurrentTimeData();

            // Assert
            InOrder order = inOrder(repositoryCollectionCustom, timeDataRepository);
            order.verify(repositoryCollectionCustom).setCollectionName(COLLECTION);
            order.verify(timeDataRepository).findById(FIXED_ID);
        }
    }

    // ==================================================================
    // getTimeDataByDate
    // ==================================================================

    @Nested
    @DisplayName("getTimeDataByDate")
    class GetTimeDataByDateTests {

        @Test
        @DisplayName("Devuelve el Optional del repositorio y fija el collection name")
        void getTimeDataByDate_devuelveOptionalDelRepositorio() {
            // Arrange
            String fecha = "2024-06-15";
            TimeData td = TimeData.builder().id(fecha).build();
            when(timeDataRepository.findById(fecha)).thenReturn(Optional.of(td));

            // Act
            Optional<TimeData> result = service.getTimeDataByDate(fecha);

            // Assert
            assertThat(result).containsSame(td);
            verify(repositoryCollectionCustom).setCollectionName(COLLECTION);
            verify(timeDataRepository).findById(fecha);
        }

        @Test
        @DisplayName("Fecha inexistente: devuelve Optional vacío")
        void getTimeDataByDate_inexistente_devuelveOptionalVacio() {
            // Arrange
            String fecha = "1900-01-01";
            when(timeDataRepository.findById(fecha)).thenReturn(Optional.empty());

            // Act
            Optional<TimeData> result = service.getTimeDataByDate(fecha);

            // Assert
            assertThat(result).isEmpty();
            verify(repositoryCollectionCustom).setCollectionName(COLLECTION);
            verify(timeDataRepository).findById(fecha);
        }
    }

    // ==================================================================
    // getByRangeDate
    // ==================================================================

    @Nested
    @DisplayName("getByRangeDate")
    class GetByRangeDateTests {

        @Test
        @DisplayName("Pasa el Sort.by(ASC, '_id') al repositorio y devuelve la lista")
        void getByRangeDate_pasaSortAscendentePorId() {
            // Arrange
            String init = "2024-01-01";
            String end  = "2024-12-31";
            List<TimeData> esperado = List.of(
                    TimeData.builder().id("a").build(),
                    TimeData.builder().id("b").build());
            when(timeDataRepository.listRegistros(eq(init), eq(end), any(Sort.class)))
                    .thenReturn(esperado);

            // Act
            List<TimeData> result = service.getByRangeDate(init, end);

            // Assert
            assertThat(result).isEqualTo(esperado);

            verify(timeDataRepository).listRegistros(eq(init), eq(end), sortCaptor.capture());
            Sort sort = sortCaptor.getValue();
            assertThat(sort.getOrderFor("_id")).isNotNull();
            assertThat(sort.getOrderFor("_id").getDirection()).isEqualTo(Sort.Direction.ASC);
        }

        @Test
        @DisplayName("Fija el collection name antes de llamar al repositorio")
        void getByRangeDate_seteaCollectionAntesDeConsultar() {
            // Arrange
            when(timeDataRepository.listRegistros(anyString(), anyString(), any(Sort.class)))
                    .thenReturn(Collections.emptyList());

            // Act
            service.getByRangeDate("2024-01-01", "2024-12-31");

            // Assert
            InOrder order = inOrder(repositoryCollectionCustom, timeDataRepository);
            order.verify(repositoryCollectionCustom).setCollectionName(COLLECTION);
            order.verify(timeDataRepository)
                    .listRegistros(eq("2024-01-01"), eq("2024-12-31"), any(Sort.class));
        }

        @Test
        @DisplayName("Sin resultados: devuelve lista vacía")
        void getByRangeDate_sinResultados_devuelveVacio() {
            // Arrange
            when(timeDataRepository.listRegistros(anyString(), anyString(), any(Sort.class)))
                    .thenReturn(Collections.emptyList());

            // Act
            List<TimeData> result = service.getByRangeDate("2024-01-01", "2024-12-31");

            // Assert
            assertThat(result).isEmpty();
        }
    }
}