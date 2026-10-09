package net.bounceme.chronos.suncalc.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@DisplayName("TaskDTO - DTO serializable con validación @NotEmpty en name")
class TaskDTOTest {

    private static final String NAME = "importTimes";
    private static final Integer YEAR = 2024;
    private static final Integer MONTH = 6;
    private static final String DATE = "2024-06-15";

    // ==================================================================
    // Builder
    // ==================================================================

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("Builder con todos los campos → todos accesibles")
        void builder_todosLosCampos() {
            // Act
            TaskDTO dto = TaskDTO.builder()
                    .name(NAME)
                    .year(YEAR)
                    .month(MONTH)
                    .date(DATE)
                    .build();

            // Assert
            assertThat(dto.getName()).isEqualTo(NAME);
            assertThat(dto.getYear()).isEqualTo(YEAR);
            assertThat(dto.getMonth()).isEqualTo(MONTH);
            assertThat(dto.getDate()).isEqualTo(DATE);
        }

        @Test
        @DisplayName("Builder sin campos → todos null")
        void builder_sinCampos() {
            // Act
            TaskDTO dto = TaskDTO.builder().build();

            // Assert
            assertThat(dto.getName()).isNull();
            assertThat(dto.getYear()).isNull();
            assertThat(dto.getMonth()).isNull();
            assertThat(dto.getDate()).isNull();
        }

        @Test
        @DisplayName("Builder parcial (solo name) → el resto null")
        void builder_parcialSoloName() {
            // Act
            TaskDTO dto = TaskDTO.builder()
                    .name(NAME)
                    .build();

            // Assert
            assertThat(dto.getName()).isEqualTo(NAME);
            assertThat(dto.getYear()).isNull();
            assertThat(dto.getMonth()).isNull();
            assertThat(dto.getDate()).isNull();
        }

        @Test
        @DisplayName("Builder con valores null explícitos → los campos quedan null")
        void builder_conNullsExplicitos() {
            // Act
            TaskDTO dto = TaskDTO.builder()
                    .name(null)
                    .year(null)
                    .month(null)
                    .date(null)
                    .build();

            // Assert
            assertThat(dto.getName()).isNull();
            assertThat(dto.getYear()).isNull();
            assertThat(dto.getMonth()).isNull();
            assertThat(dto.getDate()).isNull();
        }
    }

    // ==================================================================
    // Constructor sin argumentos + setters
    // ==================================================================

    @Nested
    @DisplayName("Constructor sin argumentos + setters")
    class NoArgsYSettersTests {

        @Test
        @DisplayName("new + setters → todos accesibles")
        void noArgsYSetters() {
            // Act
            TaskDTO dto = new TaskDTO();
            dto.setName(NAME);
            dto.setYear(YEAR);
            dto.setMonth(MONTH);
            dto.setDate(DATE);

            // Assert
            assertThat(dto.getName()).isEqualTo(NAME);
            assertThat(dto.getYear()).isEqualTo(YEAR);
            assertThat(dto.getMonth()).isEqualTo(MONTH);
            assertThat(dto.getDate()).isEqualTo(DATE);
        }

        @Test
        @DisplayName("new sin setters → todos null")
        void noArgsSinSetters() {
            // Act
            TaskDTO dto = new TaskDTO();

            // Assert
            assertThat(dto.getName()).isNull();
            assertThat(dto.getYear()).isNull();
            assertThat(dto.getMonth()).isNull();
            assertThat(dto.getDate()).isNull();
        }

        @Test
        @DisplayName("Setter con null sobreescribe el valor previo")
        void setterConNullSobreescribe() {
            // Arrange
            TaskDTO dto = new TaskDTO();
            dto.setName(NAME);

            // Act
            dto.setName(null);

            // Assert
            assertThat(dto.getName()).isNull();
        }
    }

    // ==================================================================
    // Constructor con todos los argumentos
    // ==================================================================

    @Nested
    @DisplayName("Constructor con todos los argumentos")
    class AllArgsConstructorTests {

        @Test
        @DisplayName("Orden de parámetros: name, year, month, date")
        void allArgs_ordenDeParametros() {
            // Act
            TaskDTO dto = new TaskDTO(NAME, YEAR, MONTH, DATE);

            // Assert
            assertThat(dto.getName()).isEqualTo(NAME);
            assertThat(dto.getYear()).isEqualTo(YEAR);
            assertThat(dto.getMonth()).isEqualTo(MONTH);
            assertThat(dto.getDate()).isEqualTo(DATE);
        }

        @Test
        @DisplayName("All-args con todos null → todos null")
        void allArgs_todosNull() {
            // Act
            TaskDTO dto = new TaskDTO(null, null, null, null);

            // Assert
            assertThat(dto.getName()).isNull();
            assertThat(dto.getYear()).isNull();
            assertThat(dto.getMonth()).isNull();
            assertThat(dto.getDate()).isNull();
        }
    }

    // ==================================================================
    // Validación con @NotEmpty
    // ==================================================================

    @Nested
    @DisplayName("Validación @NotEmpty sobre name")
    class ValidacionTests {

        private final Validator validator;

        ValidacionTests() {
            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            this.validator = factory.getValidator();
        }

        @Test
        @DisplayName("name no vacío → sin violaciones")
        void validacion_nameOk_sinViolaciones() {
            // Arrange
            TaskDTO dto = TaskDTO.builder().name(NAME).build();

            // Act
            Set<ConstraintViolation<TaskDTO>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("name null → 1 violación con el mensaje configurado")
        void validacion_nameNull_unaViolacion() {
            // Arrange
            TaskDTO dto = TaskDTO.builder().name(null).build();

            // Act
            Set<ConstraintViolation<TaskDTO>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).hasSize(1);
            ConstraintViolation<TaskDTO> v = violations.iterator().next();
            assertThat(v.getPropertyPath().toString()).hasToString("name");
            assertThat(v.getMessage()).isEqualTo("no puede estar vacío");
        }

        @Test
        @DisplayName("name vacío ('') → 1 violación")
        void validacion_nameVacio_unaViolacion() {
            // Arrange
            TaskDTO dto = TaskDTO.builder().name("").build();

            // Act
            Set<ConstraintViolation<TaskDTO>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).hasSize(1);
        }

        @Test
        @DisplayName("name solo con espacios → @NotEmpty NO lo considera vacío (no hay @NotBlank)")
        void validacion_nameEspacios_sinViolacion() {
            // Arrange
            TaskDTO dto = TaskDTO.builder().name("   ").build();

            // Act
            Set<ConstraintViolation<TaskDTO>> violations = validator.validate(dto);

            // Assert: @NotEmpty solo rechaza null y cadena vacía
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Los demás campos no se validan (year, month, date pueden ser null)")
        void validacion_otrosCamposNoValidados() {
            // Arrange
            TaskDTO dto = TaskDTO.builder()
                    .name(NAME)
                    .year(null)
                    .month(null)
                    .date(null)
                    .build();

            // Act
            Set<ConstraintViolation<TaskDTO>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).isEmpty();
        }
    }

    // ==================================================================
    // Serializable
    // ==================================================================

    @Nested
    @DisplayName("Serialización")
    class SerializableTests {

        @Test
        @DisplayName("Round-trip: serialize + deserialize mantiene los valores")
        void serializable_roundTrip() throws Exception {
            // Arrange
            TaskDTO original = TaskDTO.builder()
                    .name(NAME)
                    .year(YEAR)
                    .month(MONTH)
                    .date(DATE)
                    .build();

            // Act
            byte[] bytes;
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(original);
                bytes = baos.toByteArray();
            }

            TaskDTO deserialized;
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                 ObjectInputStream ois = new ObjectInputStream(bais)) {
                deserialized = (TaskDTO) ois.readObject();
            }

            // Assert: no hay equals → comparo campo a campo
            assertThat(deserialized).isNotSameAs(original);
            assertThat(deserialized.getName()).isEqualTo(NAME);
            assertThat(deserialized.getYear()).isEqualTo(YEAR);
            assertThat(deserialized.getMonth()).isEqualTo(MONTH);
            assertThat(deserialized.getDate()).isEqualTo(DATE);
        }

        @Test
        @DisplayName("Round-trip con todos los campos null")
        void serializable_camposNull() throws Exception {
            // Arrange
            TaskDTO original = new TaskDTO();

            // Act
            byte[] bytes;
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(original);
                bytes = baos.toByteArray();
            }

            TaskDTO deserialized;
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                 ObjectInputStream ois = new ObjectInputStream(bais)) {
                deserialized = (TaskDTO) ois.readObject();
            }

            // Assert
            assertThat(deserialized.getName()).isNull();
            assertThat(deserialized.getYear()).isNull();
            assertThat(deserialized.getMonth()).isNull();
            assertThat(deserialized.getDate()).isNull();
        }

        @Test
        @DisplayName("TaskDTO es instancia de Serializable")
        void serializable_esSerializable() {
            assertThat(new TaskDTO()).isInstanceOf(java.io.Serializable.class);
        }
    }

    // ==================================================================
    // toString
    // ==================================================================

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("Contiene el nombre de la clase y los valores")
        void toString_conValores() {
            // Arrange
            TaskDTO dto = new TaskDTO(NAME, YEAR, MONTH, DATE);

            // Act
            String result = dto.toString();

            // Assert
            assertThat(result)
                    .startsWith("TaskDTO(")
                    .contains("name=" + NAME)
                    .contains("year=" + YEAR)
                    .contains("month=" + MONTH)
                    .contains("date=" + DATE);
        }

        @Test
        @DisplayName("toString con campos null no lanza")
        void toString_camposNull_noLanza() {
            // Arrange
            TaskDTO dto = new TaskDTO();

            // Act / Assert
            assertThatCode(dto::toString).doesNotThrowAnyException();
            assertThat(dto.toString()).contains("name=null");
        }
    }

    // ==================================================================
    // equals / hashCode (documentar ausencia)
    // ==================================================================

    @Nested
    @DisplayName("equals y hashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("Sin @EqualsAndHashCode: dos instancias con mismos valores NO son iguales")
        void equals_mismosValores_noSonEquals() {
            // Arrange
            TaskDTO dto1 = new TaskDTO(NAME, YEAR, MONTH, DATE);
            TaskDTO dto2 = new TaskDTO(NAME, YEAR, MONTH, DATE);

            // Assert
            assertThat(dto1).isNotEqualTo(dto2);
        }

        @Test
        @DisplayName("hashCode por identidad")
        void hashCode_porIdentidad() {
            // Arrange
            TaskDTO dto1 = new TaskDTO(NAME, YEAR, MONTH, DATE);
            TaskDTO dto2 = new TaskDTO(NAME, YEAR, MONTH, DATE);

            // Assert
            assertThat(dto1.hashCode()).isNotEqualTo(dto2.hashCode());
        }

        @Test
        @DisplayName("Misma instancia es igual a sí misma")
        void equals_mismaInstancia() {
            // Arrange
            TaskDTO dto = new TaskDTO(NAME, YEAR, MONTH, DATE);

            // Assert
            assertThat(dto).isEqualTo(dto);
        }
    }
}