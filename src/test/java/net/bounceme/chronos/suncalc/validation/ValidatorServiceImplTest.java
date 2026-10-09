package net.bounceme.chronos.suncalc.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import net.bounceme.chronos.suncalc.validation.impl.ValidatorServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValidatorServiceImpl - envoltura de Bean Validation")
class ValidatorServiceImplTest {

    @Mock private ValidatorFactory validatorFactory;
    @Mock private Validator validator;

    private ValidatorServiceImpl<Object> service;
    private MockedStatic<Validation> validationStatic;

    @BeforeEach
    void setUp() {
        service = new ValidatorServiceImpl<>();
        validationStatic = mockStatic(Validation.class);
        validationStatic.when(Validation::buildDefaultValidatorFactory)
                .thenReturn(validatorFactory);
        lenient().when(validatorFactory.getValidator()).thenReturn(validator);
    }

    @AfterEach
    void tearDown() {
        validationStatic.close();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private ConstraintViolation<Object> violation() {
        return mock(ConstraintViolation.class);
    }

    // ==================================================================
    // Camino feliz: sin violaciones
    // ==================================================================

    @Nested
    @DisplayName("Sin violaciones")
    class SinViolacionesTests {

        @Test
        @DisplayName("Set vacío → no lanza nada")
        void validate_sinViolaciones_noLanza() {
            // Arrange
            Object obj = new Object();
            when(validator.validate(obj)).thenReturn(Collections.emptySet());

            // Act / Assert
            assertThatCode(() -> service.validate(obj))
                    .doesNotThrowAnyException();

            verify(validator).validate(obj);
        }

        @Test
        @DisplayName("Se delega en validator.validate(obj) con el mismo objeto")
        void validate_delegaEnValidator() {
            // Arrange
            Object obj = new Object();
            when(validator.validate(obj)).thenReturn(Collections.emptySet());

            // Act
            service.validate(obj);

            // Assert
            verify(validator).validate(obj);
            verify(validatorFactory).getValidator();
        }

        @Test
        @DisplayName("obj null → se delega igualmente (mock devuelve vacío → no lanza)")
        void validate_objNull_noLanza() {
            // Arrange
            when(validator.validate(null)).thenReturn(Collections.emptySet());

            // Act / Assert
            assertThatCode(() -> service.validate(null))
                    .doesNotThrowAnyException();

            verify(validator).validate(null);
        }
    }

    // ==================================================================
    // Con violaciones: lanza ConstraintViolationException
    // ==================================================================

    @Nested
    @DisplayName("Con violaciones")
    class ConViolacionesTests {

        @Test
        @DisplayName("1 violación → lanza ConstraintViolationException con ese set")
        void validate_unaViolacion_lanzaExcepcion() {
            // Arrange
            Object obj = new Object();
            Set<ConstraintViolation<Object>> violations = Set.of(violation());
            when(validator.validate(obj)).thenReturn(violations);

            // Act / Assert
            try {
                service.validate(obj);
                throw new AssertionError("Se esperaba ConstraintViolationException");
            } catch (ConstraintViolationException e) {
                assertThat(e.getConstraintViolations())
                        .containsExactlyElementsOf(violations);
            }
        }

        @Test
        @DisplayName("Varias violaciones → lanza con todas incluidas")
        void validate_variasViolaciones_lanzaConTodas() {
            // Arrange
            Object obj = new Object();
            Set<ConstraintViolation<Object>> violations = new HashSet<>();
            violations.add(violation());
            violations.add(violation());
            violations.add(violation());
            when(validator.validate(obj)).thenReturn(violations);

            // Act / Assert
            try {
                service.validate(obj);
                throw new AssertionError("Se esperaba ConstraintViolationException");
            } catch (ConstraintViolationException e) {
                assertThat(e.getConstraintViolations()).hasSize(3);
            }
        }

        @Test
        @DisplayName("La excepción contiene el set exacto devuelto por el validator")
        void validate_excepcionContieneSetExacto() {
            // Arrange
            Object obj = new Object();
            ConstraintViolation<Object> v = violation();
            Set<ConstraintViolation<Object>> violations = Set.of(v);
            when(validator.validate(obj)).thenReturn(violations);

            ArgumentCaptor<Set<ConstraintViolation<?>>> captor = ArgumentCaptor.forClass(Set.class);

            // Act
            try {
                service.validate(obj);
                throw new AssertionError("Se esperaba ConstraintViolationException");
            } catch (ConstraintViolationException e) {
                // Assert
                captor = ArgumentCaptor.forClass(Set.class);
                // La excepción encapsula las violaciones
                assertThat(e.getConstraintViolations()).containsExactly(v);
            }
        }
    }

    // ==================================================================
    // Comportamiento con la fábrica
    // ==================================================================

    @Nested
    @DisplayName("Comportamiento de la fábrica")
    class FactoryTests {

        @Test
        @DisplayName("Cada llamada a validate construye una nueva ValidatorFactory")
        void validate_multiplesLlamadas_reconstruyeFactory() {
            // Arrange
            Object obj = new Object();
            when(validator.validate(obj)).thenReturn(Collections.emptySet());

            // Act
            service.validate(obj);
            service.validate(obj);

            // Assert
            validationStatic.verify(Validation::buildDefaultValidatorFactory, times(2));
            verify(validatorFactory, times(2)).getValidator();
        }

        @Test
        @DisplayName("Si buildDefaultValidatorFactory lanza, se propaga")
        void validate_factoryLanza_sePropaga() {
            // Arrange
            validationStatic.when(Validation::buildDefaultValidatorFactory)
                    .thenThrow(new IllegalStateException("sin proveedor de validación"));

            // Act / Assert
            try {
                service.validate(new Object());
                throw new AssertionError("Se esperaba IllegalStateException");
            } catch (IllegalStateException e) {
                assertThat(e).hasMessage("sin proveedor de validación");
            }
        }

        @Test
        @DisplayName("Si validator.validate lanza, se propaga sin envolver")
        void validate_validatorLanza_sePropaga() {
            // Arrange
            Object obj = new Object();
            RuntimeException boom = new IllegalStateException("boom del validator");
            when(validator.validate(obj)).thenThrow(boom);

            // Act / Assert
            try {
                service.validate(obj);
                throw new AssertionError("Se esperaba IllegalStateException");
            } catch (IllegalStateException e) {
                assertThat(e).isSameAs(boom);
            }
        }
    }
}