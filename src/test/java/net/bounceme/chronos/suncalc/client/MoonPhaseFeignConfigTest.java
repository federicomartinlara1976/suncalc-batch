package net.bounceme.chronos.suncalc.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import net.bounceme.chronos.suncalc.clients.MoonPhaseFeignConfig;

@ExtendWith(MockitoExtension.class)
@DisplayName("MoonPhaseFeignConfig - configuración de Feign para inyectar token")
class MoonPhaseFeignConfigTest {

    private static final String TOKEN = "mi-token-secreto";

    @Mock
    private RequestTemplate requestTemplate;

    private MoonPhaseFeignConfig config;

    @BeforeEach
    void setUp() {
        config = new MoonPhaseFeignConfig();
        ReflectionTestUtils.setField(config, "token", TOKEN);
    }

    // ==================================================================
    // Creación del interceptor
    // ==================================================================

    @Nested
    @DisplayName("moonPhaseTokenInterceptor()")
    class InterceptorCreationTests {

        @Test
        @DisplayName("Devuelve una instancia no nula de RequestInterceptor")
        void moonPhaseTokenInterceptor_devuelveInstancia() {
            // Act
            RequestInterceptor interceptor = config.moonPhaseTokenInterceptor();

            // Assert
            assertThat(interceptor).isNotNull();
        }
    }

    // ==================================================================
    // Comportamiento del interceptor (la lambda)
    // ==================================================================

    @Nested
    @DisplayName("apply(template) en el interceptor")
    class ApplyTests {

        @Test
        @DisplayName("Inyecta el token en la query del RequestTemplate")
        void apply_inyectaTokenEnTemplate() {
            // Arrange
            RequestInterceptor interceptor = config.moonPhaseTokenInterceptor();

            // Act
            interceptor.apply(requestTemplate);

            // Assert
            verify(requestTemplate).query("token", TOKEN);
            verifyNoMoreInteractions(requestTemplate);
        }

        @Test
        @DisplayName("Funciona con múltiples invocaciones sobre distintos templates")
        void apply_multiplesInvocaciones() {
            // Arrange
            RequestInterceptor interceptor = config.moonPhaseTokenInterceptor();
            RequestTemplate otroTemplate = mock(RequestTemplate.class);

            // Act
            interceptor.apply(requestTemplate);
            interceptor.apply(otroTemplate);

            // Assert
            verify(requestTemplate).query("token", TOKEN);
            verify(otroTemplate).query("token", TOKEN);
        }
    }

    // ==================================================================
    // Casos límite (token nulo o vacío)
    // ==================================================================

    @Nested
    @DisplayName("Casos límite con el token")
    class TokenLimiteTests {

        @Test
        @DisplayName("token null → se pasa null al query (sin NPE en la config)")
        void apply_tokenNull_pasaNull() {
            // Arrange
            ReflectionTestUtils.setField(config, "token", null);
            RequestInterceptor interceptor = config.moonPhaseTokenInterceptor();

            // Act
            interceptor.apply(requestTemplate);

            // Assert
            verify(requestTemplate).query("token", (String) null);
        }

        @Test
        @DisplayName("token vacío → se pasa cadena vacía")
        void apply_tokenVacio_pasaCadenaVacia() {
            // Arrange
            ReflectionTestUtils.setField(config, "token", "");
            RequestInterceptor interceptor = config.moonPhaseTokenInterceptor();

            // Act
            interceptor.apply(requestTemplate);

            // Assert
            verify(requestTemplate).query("token", "");
        }
    }
}
