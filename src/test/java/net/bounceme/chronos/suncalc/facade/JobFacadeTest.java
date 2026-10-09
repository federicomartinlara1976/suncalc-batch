package net.bounceme.chronos.suncalc.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import net.bounceme.chronos.suncalc.dto.JobDTO;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobFacade - publicación de JobDTO en RabbitMQ")
class JobFacadeTest {

    private static final String QUEUE_NAME = "suncalc-queue";

    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private JobFacade facade;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(facade, "queueName", QUEUE_NAME);
    }

    // ==================================================================
    // Publicación correcta
    // ==================================================================

    @Nested
    @DisplayName("Publicación correcta")
    class PublicacionTests {

        @Test
        @DisplayName("publishJob delega en convertAndSend con la cola y el JobDTO")
        void publishJob_delegaEnConvertAndSend() {
            // Arrange
            JobDTO<String> dto = new JobDTO<>();

            // Act
            facade.publishJob(dto);

            // Assert
            verify(rabbitTemplate).convertAndSend(QUEUE_NAME, dto);
        }

        @Test
        @DisplayName("El JobDTO enviado es el mismo objeto que se pasa")
        void publishJob_enviaElMismoObjeto() {
            // Arrange
            JobDTO<String> dto = new JobDTO<>();
            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);

            // Act
            facade.publishJob(dto);

            // Assert
            verify(rabbitTemplate).convertAndSend(eq(QUEUE_NAME), captor.capture());
            assertThat(captor.getValue()).isSameAs(dto);
        }

        @Test
        @DisplayName("Múltiples publicaciones → múltiples convertAndSend")
        void publishJob_multiplesPublicaciones_nLlamadas() {
            // Arrange
            JobDTO<String> dto1 = new JobDTO<>();
            JobDTO<Integer> dto2 = new JobDTO<>();
            JobDTO<Long> dto3 = new JobDTO<>();
            
            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);

            // Act
            facade.publishJob(dto1);
            facade.publishJob(dto2);
            facade.publishJob(dto3);

            // Assert
            verify(rabbitTemplate, times(3)).convertAndSend(eq(QUEUE_NAME), captor.capture());
            assertThat(captor.getAllValues()).containsExactly(dto1, dto2, dto3);
        }

        @Test
        @DisplayName("Usa el queueName inyectado, no otro")
        void publishJob_usaQueueNameInyectado() {
            // Arrange
            ReflectionTestUtils.setField(facade, "queueName", "otra-cola");
            JobDTO<String> dto = new JobDTO<>();
            
            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);

            // Act
            facade.publishJob(dto);

            // Assert
            verify(rabbitTemplate).convertAndSend("otra-cola", dto);
            verify(rabbitTemplate, times(0)).convertAndSend(eq(QUEUE_NAME), captor.capture());
        }
    }

    // ==================================================================
    // Casos límite
    // ==================================================================

    @Nested
    @DisplayName("Casos límite")
    class CasosLimiteTests {

        @Test
        @DisplayName("jobDTO null → se pasa tal cual a convertAndSend (no lanza)")
        void publishJob_jobDTONull_noLanza() {
            // Act / Assert
            assertThatCode(() -> facade.publishJob(null))
                    .doesNotThrowAnyException();

            verify(rabbitTemplate).convertAndSend(QUEUE_NAME, (Object) null);
        }

        @Test
        @DisplayName("queueName null → se pasa null a convertAndSend")
        void publishJob_queueNameNull_pasaNull() {
            // Arrange
            ReflectionTestUtils.setField(facade, "queueName", null);
            JobDTO<String> dto = new JobDTO<>();

            // Act
            facade.publishJob(dto);

            // Assert
            verify(rabbitTemplate).convertAndSend(null, dto);
        }

        @Test
        @DisplayName("RabbitTemplate lanza excepción → se propaga sin envolver")
        void publishJob_rabbitLanza_propaga() {
            // Arrange
            JobDTO<String> dto = new JobDTO<>();
            RuntimeException boom = new RuntimeException("broker caído");
            doThrow(boom).when(rabbitTemplate).convertAndSend(QUEUE_NAME, dto);

            // Act / Assert
            try {
                facade.publishJob(dto);
                throw new AssertionError("Se esperaba la excepción");
            } catch (RuntimeException e) {
                assertThat(e).isSameAs(boom);
            }
        }
    }

    // ==================================================================
    // Verificación de "no más interacciones"
    // ==================================================================

    @Nested
    @DisplayName("Sin interacciones inesperadas")
    class NoInteraccionesTests {

        @Test
        @DisplayName("publishJob no invoca otros métodos del RabbitTemplate")
        void publishJob_soloConvertAndSend() {
            // Arrange
            JobDTO<String> dto = new JobDTO<>();

            // Act
            facade.publishJob(dto);

            // Assert
            verify(rabbitTemplate).convertAndSend(QUEUE_NAME, dto);
            verifyNoMoreInteractions(rabbitTemplate);
        }
    }
}