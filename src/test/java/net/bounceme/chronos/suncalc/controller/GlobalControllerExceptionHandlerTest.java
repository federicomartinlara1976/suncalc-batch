package net.bounceme.chronos.suncalc.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalControllerExceptionHandler - manejo global de excepciones")
class GlobalControllerExceptionHandlerTest {

    private static final String MENSAJE = "mensaje";

    private GlobalControllerExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalControllerExceptionHandler();
    }

    // ==================================================================
    // Invocación directa de cada handler
    // ==================================================================

    @Nested
    @DisplayName("Handlers (invocación directa)")
    class HandlersDirectos {

        // -------------------- IllegalArgumentException ----------------

        @Test
        @DisplayName("IllegalArgumentException → 500 con {mensaje}")
        void handleIllegalArgument_devuelve500ConMensaje() {
            // Arrange
            IllegalArgumentException ex = new IllegalArgumentException("argumento inválido");

            // Act
            ResponseEntity<Map<String, String>> response = handler.handleException(ex);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody())
                    .containsEntry(MENSAJE, "argumento inválido")
                    .hasSize(1);
        }

        // ---------------------- DataAccessException --------------------

        @Test
        @DisplayName("DataAccessException con causa → 500 con la causa más específica")
        void handleDataAccess_conCausa_devuelve500ConCausaMasEspecifica() {
            // Arrange
            DataAccessResourceFailureException ex = new DataAccessResourceFailureException(
                    "fallo de acceso", new RuntimeException("causa raíz"));

            // Act
            ResponseEntity<Map<String, String>> response = handler.handleException(ex);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody())
                    .containsEntry(MENSAJE, "causa raíz")
                    .hasSize(1);
        }

        @Test
        @DisplayName("DataAccessException sin causa → 500 con su propio mensaje")
        void handleDataAccess_sinCausa_devuelve500ConMensajePropio() {
            // Arrange
            DataAccessResourceFailureException ex = new DataAccessResourceFailureException("mensaje directo");

            // Act
            ResponseEntity<Map<String, String>> response = handler.handleException(ex);

            // Assert
            assertThat(response.getBody())
                    .containsEntry(MENSAJE, "mensaje directo")
                    .hasSize(1);
        }

        // -------------- MethodArgumentNotValidException ----------------

        @Test
        @DisplayName("MethodArgumentNotValidException con varios errores → 400 con mapa campo→mensaje")
        void handleMethodArgumentNotValid_variosErrores_devuelve400ConCampos() throws Exception {
            // Arrange
            BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
            bindingResult.addError(new FieldError("target", "campo1", "mensaje1"));
            bindingResult.addError(new FieldError("target", "campo2", "mensaje2"));
            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                    anyMethodParameter(), bindingResult);

            // Act
            ResponseEntity<Map<String, String>> response = handler.handleValidationException(ex);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody())
                    .containsEntry("campo1", "mensaje1")
                    .containsEntry("campo2", "mensaje2")
                    .hasSize(2);
        }

        @Test
        @DisplayName("MethodArgumentNotValidException con un solo error → 400 con mapa de un elemento")
        void handleMethodArgumentNotValid_unSoloError_devuelve400() throws Exception {
            // Arrange
            BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
            bindingResult.addError(new FieldError("target", "campo", "solo uno"));
            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                    anyMethodParameter(), bindingResult);

            // Act
            ResponseEntity<Map<String, String>> response = handler.handleValidationException(ex);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody())
                    .containsOnlyKeys("campo")
                    .containsEntry("campo", "solo uno");
        }

        // ---------------------- ParseException ------------------------

        @Test
        @DisplayName("ParseException → 400 con {mensaje}")
        void handleParse_devuelve400ConMensaje() {
            // Arrange
            ParseException ex = new ParseException("fecha inválida", 0);

            // Act
            ResponseEntity<Map<String, String>> response = handler.handleParseException(ex);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody())
                    .containsEntry(MENSAJE, "fecha inválida")
                    .hasSize(1);
        }

        // ------------ HandlerMethodValidationException ----------------

        @Test
        @DisplayName("HandlerMethodValidationException → 400 con texto prefijado")
        void handleHandlerMethodValidation_devuelve400ConPrefijo() {
            // Arrange
            HandlerMethodValidationException ex = mock(HandlerMethodValidationException.class);
            when(ex.getMessage()).thenReturn("detalle de validación");

            // Act
            ResponseEntity<String> response = handler.handleValidationException(ex);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isEqualTo("Error de validación: detalle de validación");
        }
    }

    // ==================================================================
    // Integración MVC: el advice realmente intercepta las excepciones
    // ==================================================================

    @Nested
    @DisplayName("Integración MVC (standalone MockMvc)")
    class IntegracionMvc {

        private MockMvc mockMvc;

        @BeforeEach
        void setUpMvc() {
            mockMvc = MockMvcBuilders.standaloneSetup(new DummyController())
                    .setControllerAdvice(handler)
                    .build();
        }

        @Test
        @DisplayName("/test/illegal → 500 con body {mensaje}")
        void illegal_devuelve500() throws Exception {
            mockMvc.perform(get("/test/illegal"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.mensaje").value("argumento inválido"));
        }

        @Test
        @DisplayName("/test/dataAccess → 500 con causa más específica")
        void dataAccess_devuelve500() throws Exception {
            mockMvc.perform(get("/test/dataAccess"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.mensaje").value("causa raíz"));
        }

        @Test
        @DisplayName("/test/parse → 400 con {mensaje}")
        void parse_devuelve400() throws Exception {
            mockMvc.perform(get("/test/parse"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensaje").value("fecha inválida"));
        }

        @Test
        @DisplayName("/test/handlerMethodValidation → 400 con texto plano prefijado")
        void handlerMethodValidation_devuelve400() throws Exception {
            mockMvc.perform(get("/test/handlerMethodValidation"))
                    .andExpect(status().isBadRequest())
                    .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                            .isEqualTo("Error de validación: detalle de validación"));
        }
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private static MethodParameter anyMethodParameter() throws NoSuchMethodException {
        Method method = GlobalControllerExceptionHandlerTest.class
                .getDeclaredMethod("dummyMethod", String.class);
        return new MethodParameter(method, 0);
    }

    @SuppressWarnings("unused")
    private void dummyMethod(String value) {
        // Solo se usa para construir MethodParameter en los tests
    }

    // ==================================================================
    // Dummy controller para integración MVC
    // ==================================================================

    @RestController
    static class DummyController {

        @GetMapping("/test/illegal")
        public void illegal() {
            throw new IllegalArgumentException("argumento inválido");
        }

        @GetMapping("/test/dataAccess")
        public void dataAccess() {
            throw new DataAccessResourceFailureException(
                    "fallo de acceso", new RuntimeException("causa raíz"));
        }

        @GetMapping("/test/parse")
        public void parse() throws ParseException {
            throw new ParseException("fecha inválida", 0);
        }

        @GetMapping("/test/handlerMethodValidation")
        public void handlerMethodValidation() {
            HandlerMethodValidationException mockEx = mock(HandlerMethodValidationException.class);
            when(mockEx.getMessage()).thenReturn("detalle de validación");
            throw mockEx;
        }
    }
}