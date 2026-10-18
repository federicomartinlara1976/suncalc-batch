package net.bounceme.chronos.suncalc.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Date;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("FaseLunarDTO - DTO con Lombok")
class FaseLunarDTOTest {

    private static final Date DATE = new Date(1718409600000L);
    private static final Float EDAD = 7.5f;
    private static final String FASE = "creciente";
    private static final Float ILUMINACION = 0.75f;

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
            FaseLunarDTO dto = FaseLunarDTO.builder()
                    .date(DATE)
                    .moonAgeDays(EDAD)
                    .phaseName(FASE)
                    .illuminationPercent(ILUMINACION)
                    .build();

            // Assert
            assertThat(dto.getDate()).isSameAs(DATE);
            assertThat(dto.getMoonAgeDays()).isEqualTo(EDAD);
            assertThat(dto.getPhaseName()).isEqualTo(FASE);
            assertThat(dto.getIlluminationPercent()).isEqualTo(ILUMINACION);
        }

        @Test
        @DisplayName("Builder sin campos → todos null")
        void builder_sinCampos() {
            // Act
            FaseLunarDTO dto = FaseLunarDTO.builder().build();

            // Assert
            assertThat(dto.getDate()).isNull();
            assertThat(dto.getMoonAgeDays()).isNull();
            assertThat(dto.getPhaseName()).isNull();
            assertThat(dto.getIlluminationPercent()).isNull();
        }

        @Test
        @DisplayName("Builder parcial (solo fase) → el resto null")
        void builder_parcialSoloFase() {
            // Act
            FaseLunarDTO dto = FaseLunarDTO.builder()
                    .phaseName(FASE)
                    .build();

            // Assert
            assertThat(dto.getDate()).isNull();
            assertThat(dto.getMoonAgeDays()).isNull();
            assertThat(dto.getPhaseName()).isEqualTo(FASE);
            assertThat(dto.getIlluminationPercent()).isNull();
        }

        @Test
        @DisplayName("Builder con valores null explícitos → los campos quedan null")
        void builder_conNullsExplicitos() {
            // Act
            FaseLunarDTO dto = FaseLunarDTO.builder()
                    .date(null)
                    .moonAgeDays(null)
                    .phaseName(null)
                    .illuminationPercent(null)
                    .build();

            // Assert
            assertThat(dto.getDate()).isNull();
            assertThat(dto.getMoonAgeDays()).isNull();
            assertThat(dto.getPhaseName()).isNull();
            assertThat(dto.getIlluminationPercent()).isNull();
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
            FaseLunarDTO dto = new FaseLunarDTO();
            dto.setDate(DATE);
            dto.setMoonAgeDays(EDAD);
            dto.setPhaseName(FASE);
            dto.setIlluminationPercent(ILUMINACION);

            // Assert
            assertThat(dto.getDate()).isSameAs(DATE);
            assertThat(dto.getMoonAgeDays()).isEqualTo(EDAD);
            assertThat(dto.getPhaseName()).isEqualTo(FASE);
            assertThat(dto.getIlluminationPercent()).isEqualTo(ILUMINACION);
        }

        @Test
        @DisplayName("new sin setters → todos null")
        void noArgsSinSetters() {
            // Act
            FaseLunarDTO dto = new FaseLunarDTO();

            // Assert
            assertThat(dto.getDate()).isNull();
            assertThat(dto.getMoonAgeDays()).isNull();
            assertThat(dto.getPhaseName()).isNull();
            assertThat(dto.getIlluminationPercent()).isNull();
        }

        @Test
        @DisplayName("Setter con null → sobreescribe el valor previo")
        void setterConNullSobreescribe() {
            // Arrange
            FaseLunarDTO dto = new FaseLunarDTO();
            dto.setPhaseName(FASE);

            // Act
            dto.setPhaseName(null);

            // Assert
            assertThat(dto.getPhaseName()).isNull();
        }
    }

    // ==================================================================
    // Constructor con todos los argumentos
    // ==================================================================

    @Nested
    @DisplayName("Constructor con todos los argumentos")
    class AllArgsConstructorTests {

        @Test
        @DisplayName("Orden de parámetros: date, edad, fase, iluminacion")
        void allArgs_ordenDeParametros() {
            // Act
            FaseLunarDTO dto = new FaseLunarDTO(DATE, "", FASE, "", ILUMINACION, 0.0f, 0.0f, DATE, DATE, DATE, DATE);

            // Assert
            assertThat(dto.getDate()).isSameAs(DATE);
            assertThat(dto.getMoonAgeDays()).isEqualTo(EDAD);
            assertThat(dto.getPhaseName()).isEqualTo(FASE);
            assertThat(dto.getIlluminationPercent()).isEqualTo(ILUMINACION);
        }

        @Test
        @DisplayName("All-args con todos null → todos null")
        void allArgs_todosNull() {
            // Act
            FaseLunarDTO dto = new FaseLunarDTO(null, null, null, null, null, null, null, null, null, null, null);

            // Assert
            assertThat(dto.getDate()).isNull();
            assertThat(dto.getMoonAgeDays()).isNull();
            assertThat(dto.getPhaseName()).isNull();
            assertThat(dto.getIlluminationPercent()).isNull();
        }

        @Test
        @DisplayName("Valores límite de Float: 0.0 y negativos se almacenan tal cual")
        void allArgs_valoresFloatLimite() {
            // Act
            FaseLunarDTO dto = new FaseLunarDTO(DATE, "", FASE, "", -0.5f, 0.0f, 0.0f, DATE, DATE, DATE, DATE);

            // Assert
            assertThat(dto.getMoonAgeDays()).isEqualTo(0.0f);
            assertThat(dto.getIlluminationPercent()).isEqualTo(-0.5f);
        }
    }

    // ==================================================================
    // toString
    // ==================================================================

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("Contiene el nombre de la clase y los valores de los campos")
        void toString_contieneClaseYValores() {
            // Arrange
        	FaseLunarDTO dto = new FaseLunarDTO(DATE, "", FASE, "", ILUMINACION, 0.0f, 0.0f, DATE, DATE, DATE, DATE);

            // Act
            String result = dto.toString();

            // Assert
            assertThat(result)
                    .startsWith("FaseLunarDTO(")
                    .contains("date=")
                    .contains("moonAgeDays=" + EDAD)
                    .contains("phaseName=" + FASE)
                    .contains("illuminationPercent=" + ILUMINACION);
        }

        @Test
        @DisplayName("toString con campos null no lanza")
        void toString_camposNull_noLanza() {
            // Arrange
            FaseLunarDTO dto = new FaseLunarDTO();

            // Act / Assert
            assertThatCode(dto::toString).doesNotThrowAnyException();
            assertThat(dto.toString())
                    .contains("date=null")
                    .contains("edad=null")
                    .contains("fase=null")
                    .contains("iluminacion=null");
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
        void equals_dosInstanciasIguales_noSonEquals() {
            // Arrange
            FaseLunarDTO dto1 = new FaseLunarDTO(DATE, "", FASE, "", ILUMINACION, EDAD, 0.0f, DATE, DATE, DATE, DATE);
            FaseLunarDTO dto2 = new FaseLunarDTO(DATE, "", FASE, "", ILUMINACION, EDAD, 0.0f, DATE, DATE, DATE, DATE);
            
            // Assert
            assertThat(dto1)
            	.isNotEqualTo(dto2)
            	.isNotSameAs(dto2);
        }

        @Test
        @DisplayName("hashCode es por identidad (dos instancias → distintos)")
        void hashCode_porIdentidad() {
            // Arrange
            FaseLunarDTO dto1 = new FaseLunarDTO(DATE, "", FASE, "", ILUMINACION, EDAD, 0.0f, DATE, DATE, DATE, DATE);
            FaseLunarDTO dto2 = new FaseLunarDTO(DATE, "", FASE, "", ILUMINACION, EDAD, 0.0f, DATE, DATE, DATE, DATE);

            // Assert
            assertThat(dto1.hashCode()).isNotEqualTo(dto2.hashCode());
        }

        @Test
        @DisplayName("La misma instancia sí es igual a sí misma")
        void equals_mismaInstancia() {
            // Arrange
            FaseLunarDTO dto = new FaseLunarDTO(DATE, "", FASE, "", ILUMINACION, EDAD, 0.0f, DATE, DATE, DATE, DATE);

            // Assert
            assertThat(dto).isEqualTo(dto);
        }
    }
}