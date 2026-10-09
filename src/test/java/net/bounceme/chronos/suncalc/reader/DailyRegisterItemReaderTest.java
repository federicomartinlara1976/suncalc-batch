package net.bounceme.chronos.suncalc.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ExecutionContext;

import net.bounceme.chronos.suncalc.model.TimeData;
import net.bounceme.chronos.suncalc.support.processor.DocumentProcessor;

@ExtendWith(MockitoExtension.class)
@DisplayName("DailyRegisterItemReader - lector de un único registro diario")
class DailyRegisterItemReaderTest {

    @Mock
    private DocumentProcessor documentProcessor;

    @InjectMocks
    private DailyRegisterItemReader reader;

    // ==================================================================
    // initialize() vía open()
    // ==================================================================

    @Nested
    @DisplayName("initialize() vía open()")
    class InitializeTests {

        @Test
        @DisplayName("Añade a 'records' el resultado de documentProcessor.process()")
        void initialize_aniadeResultadoDeProcess() {
            // Arrange
            TimeData processed = new TimeData();
            when(documentProcessor.process()).thenReturn(processed);

            // Act
            reader.open(new ExecutionContext());

            // Assert
            assertThat(reader.records).containsExactly(processed);
            verify(documentProcessor).process();
        }

        @Test
        @DisplayName("Cada llamada a open() añade un nuevo elemento (no reemplaza)")
        void initialize_variasLlamadas_acumulaResultados() {
            // Arrange
            TimeData first = new TimeData();
            TimeData second = new TimeData();
            when(documentProcessor.process()).thenReturn(first, second);

            // Act
            reader.open(new ExecutionContext());
            reader.open(new ExecutionContext());

            // Assert
            assertThat(reader.records).containsExactly(first, second);
            verify(documentProcessor, times(2)).process();
        }

        @Test
        @DisplayName("Si process() devuelve null, se añade null a 'records'")
        void initialize_processNull_aniadeNull() {
            // Arrange
            when(documentProcessor.process()).thenReturn(null);

            // Act
            reader.open(new ExecutionContext());

            // Assert
            assertThat(reader.records).hasSize(1).containsNull();
        }

        @Test
        @DisplayName("Si process() lanza excepción, se propaga (vía @SneakyThrows)")
        void initialize_processLanzaExcepcion_sePropaga() {
            // Arrange
            RuntimeException boom = new RuntimeException("fallo del processor");
            when(documentProcessor.process()).thenThrow(boom);

            // Act / Assert
            assertThatThrownBy(() -> reader.open(new ExecutionContext()))
                    .isSameAs(boom);

            // records queda vacío (no se llegó a añadir nada)
            assertThat(reader.records).isEmpty();
        }

        @Test
        @DisplayName("No se invoca a process() si no se abre el stream")
        void initialize_noSeInvocaSinOpen() {
            // Act
            // (nada: no llamamos a open)

            // Assert
            verify(documentProcessor, times(0)).process();
            assertThat(reader.records).isEmpty();
        }
    }

    // ==================================================================
    // Integración con read() heredado
    // ==================================================================

    @Nested
    @DisplayName("Integración con read() heredado")
    class IntegracionConReadTests {

        @Test
        @DisplayName("Tras open(), read() devuelve el registro procesado la primera vez")
        void read_trasOpen_devuelveElRegistro() {
            // Arrange
            TimeData processed = new TimeData();
            when(documentProcessor.process()).thenReturn(processed);
            reader.open(new ExecutionContext());

            // Act
            TimeData first = reader.read();
            TimeData second = reader.read();

            // Assert
            assertThat(first).isSameAs(processed);
            assertThat(second).isNull();
        }

        @Test
        @DisplayName("Sin open() previo, read() devuelve null (records vacío)")
        void read_sinOpen_devuelveNull() {
            // Act
            TimeData result = reader.read();

            // Assert
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Tras agotar el único registro, la siguiente pasada vuelve a leerlo")
        void read_autoReset_releeElMismoRegistro() {
            // Arrange
            TimeData processed = new TimeData();
            when(documentProcessor.process()).thenReturn(processed);
            reader.open(new ExecutionContext());

            // Act
            assertThat(reader.read()).isSameAs(processed); // 1ª lectura
            assertThat(reader.read()).isNull();            // agota + reset
            TimeData segundaPasada = reader.read();        // vuelve a empezar

            // Assert
            assertThat(segundaPasada).isSameAs(processed);
        }
    }
}