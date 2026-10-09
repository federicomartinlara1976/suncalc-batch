package net.bounceme.chronos.suncalc.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("JobDTO<T> - DTO genérico con un único campo 'content'")
class JobDTOTest {

    // ==================================================================
    // Estado inicial
    // ==================================================================

    @Nested
    @DisplayName("Estado inicial")
    class EstadoInicialTests {

        @Test
        @DisplayName("new JobDTO<>() → content null")
        void new_dto_contentNull() {
            // Act
            JobDTO<String> dto = new JobDTO<>();

            // Assert
            assertThat(dto.getContent()).isNull();
        }

        @Test
        @DisplayName("Existe constructor público sin argumentos")
        void constructor_publicoSinArgumentos() {
            // Act / Assert
            assertThatCode(JobDTO::new).doesNotThrowAnyException();
        }
    }

    // ==================================================================
    // getContent / setContent con distintos tipos
    // ==================================================================

    @Nested
    @DisplayName("content con distintos tipos")
    class ContentDistintosTiposTests {

        @Test
        @DisplayName("JobDTO<String>")
        void content_string() {
            // Arrange
            JobDTO<String> dto = new JobDTO<>();

            // Act
            dto.setContent("hola");

            // Assert
            assertThat(dto.getContent()).isEqualTo("hola");
        }

        @Test
        @DisplayName("JobDTO<Integer>")
        void content_integer() {
            // Arrange
            JobDTO<Integer> dto = new JobDTO<>();

            // Act
            dto.setContent(42);

            // Assert
            assertThat(dto.getContent()).isEqualTo(42);
        }

        @Test
        @DisplayName("JobDTO<TaskDTO>")
        void content_taskDTO() {
            // Arrange
            TaskDTO task = TaskDTO.builder().name("importTimes").build();
            JobDTO<TaskDTO> dto = new JobDTO<>();

            // Act
            dto.setContent(task);

            // Assert
            assertThat(dto.getContent()).isSameAs(task);
            assertThat(dto.getContent().getName()).isEqualTo("importTimes");
        }

        @Test
        @DisplayName("JobDTO<List<String>>")
        void content_lista() {
            // Arrange
            List<String> lista = List.of("a", "b", "c");
            JobDTO<List<String>> dto = new JobDTO<>();

            // Act
            dto.setContent(lista);

            // Assert
            assertThat(dto.getContent()).containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("JobDTO<?> acepta cualquier objeto")
        void content_wildcard() {
            // Arrange
            JobDTO<?> dto = new JobDTO<String>();

            // Act / Assert: el wildcard impide setContent, pero se puede leer
            assertThat(dto.getContent()).isNull();
        }
    }

    // ==================================================================
    // setContent con null / sobreescritura
    // ==================================================================

    @Nested
    @DisplayName("setContent con null y sobreescritura")
    class SetContentTests {

        @Test
        @DisplayName("setContent(null) → getContent() null")
        void setContent_null() {
            // Arrange
            JobDTO<String> dto = new JobDTO<>();
            dto.setContent("algo");

            // Act
            dto.setContent(null);

            // Assert
            assertThat(dto.getContent()).isNull();
        }

        @Test
        @DisplayName("setContent sobreescribe el valor previo")
        void setContent_sobreescribe() {
            // Arrange
            JobDTO<String> dto = new JobDTO<>();
            dto.setContent("primero");

            // Act
            dto.setContent("segundo");

            // Assert
            assertThat(dto.getContent()).isEqualTo("segundo");
        }

        @Test
        @DisplayName("Misma instancia se reutiliza con distintos tipos")
        void setContent_reutilizaInstancia() {
            // Arrange
            @SuppressWarnings({ "rawtypes", "unchecked" })
            JobDTO dto = new JobDTO();

            // Act
            dto.setContent("texto");
            Object primero = dto.getContent();

            dto.setContent(123);
            Object segundo = dto.getContent();

            // Assert
            assertThat(primero).isEqualTo("texto");
            assertThat(segundo).isEqualTo(123);
        }
    }

    // ==================================================================
    // toString
    // ==================================================================

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("Contiene el nombre de la clase y el content")
        void toString_conContent() {
            // Arrange
            JobDTO<String> dto = new JobDTO<>();
            dto.setContent("mi-contenido");

            // Act
            String result = dto.toString();

            // Assert
            assertThat(result)
                    .startsWith("JobDTO(")
                    .contains("content=mi-contenido");
        }

        @Test
        @DisplayName("toString con content null → 'content=null'")
        void toString_contentNull() {
            // Arrange
            JobDTO<String> dto = new JobDTO<>();

            // Act
            String result = dto.toString();

            // Assert
            assertThat(result).contains("content=null");
        }

        @Test
        @DisplayName("toString con content complejo no lanza")
        void toString_contentComplejo_noLanza() {
            // Arrange
            TaskDTO task = TaskDTO.builder().name("importTimes").build();
            JobDTO<TaskDTO> dto = new JobDTO<>();
            dto.setContent(task);

            // Act / Assert
            assertThatCode(dto::toString).doesNotThrowAnyException();
        }
    }

    // ==================================================================
    // equals / hashCode (documentar ausencia)
    // ==================================================================

    @Nested
    @DisplayName("equals y hashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("Sin @EqualsAndHashCode: dos instancias con mismo content NO son iguales")
        void equals_mismoContent_noSonEquals() {
            // Arrange
            JobDTO<String> dto1 = new JobDTO<>();
            JobDTO<String> dto2 = new JobDTO<>();
            dto1.setContent("x");
            dto2.setContent("x");

            // Assert
            assertThat(dto1).isNotEqualTo(dto2);
        }

        @SuppressWarnings("rawtypes")
		@Test
        @DisplayName("hashCode por identidad")
        void hashCode_porIdentidad() {
            // Arrange
            JobDTO<String> dto1 = new JobDTO<>();
            JobDTO<String> dto2 = new JobDTO();

            // Assert
            assertThat(dto1.hashCode()).isNotEqualTo(dto2.hashCode());
        }

        @Test
        @DisplayName("Misma instancia es igual a sí misma")
        void equals_mismaInstancia() {
            // Arrange
            JobDTO<String> dto = new JobDTO<>();
            dto.setContent("x");

            // Assert
            assertThat(dto).isEqualTo(dto);
        }
    }
}