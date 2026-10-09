package net.bounceme.chronos.suncalc.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DataSolsticeEquinoxDTO - DTO con Lombok")
class DataSolsticeEquinoxDTOTest {

    private static final String NAME = "june_solstice";
    private static final Date UTC_DATE = new Date(1718409600000L);
    private static final Date LOCAL_DATE = new Date(1718409601000L);

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
            DataSolsticeEquinoxDTO dto = DataSolsticeEquinoxDTO.builder()
                    .name(NAME)
                    .utcDate(UTC_DATE)
                    .localDate(LOCAL_DATE)
                    .build();

            // Assert
            assertThat(dto.getName()).isEqualTo(NAME);
            assertThat(dto.getUtcDate()).isSameAs(UTC_DATE);
            assertThat(dto.getLocalDate()).isSameAs(LOCAL_DATE);
        }

        @Test
        @DisplayName("Builder sin campos → todos null")
        void builder_sinCampos() {
            // Act
            DataSolsticeEquinoxDTO dto = DataSolsticeEquinoxDTO.builder().build();

            // Assert
            assertThat(dto.getName()).isNull();
            assertThat(dto.getUtcDate()).isNull();
            assertThat(dto.getLocalDate()).isNull();
        }

        @Test
        @DisplayName("Builder parcial (solo name) → el resto null")
        void builder_parcialSoloName() {
            // Act
            DataSolsticeEquinoxDTO dto = DataSolsticeEquinoxDTO.builder()
                    .name(NAME)
                    .build();

            // Assert
            assertThat(dto.getName()).isEqualTo(NAME);
            assertThat(dto.getUtcDate()).isNull();
            assertThat(dto.getLocalDate()).isNull();
        }

        @Test
        @DisplayName("Builder con valores null explícitos → los campos quedan null")
        void builder_conNullsExplicitos() {
            // Act
            DataSolsticeEquinoxDTO dto = DataSolsticeEquinoxDTO.builder()
                    .name(null)
                    .utcDate(null)
                    .localDate(null)
                    .build();

            // Assert
            assertThat(dto.getName()).isNull();
            assertThat(dto.getUtcDate()).isNull();
            assertThat(dto.getLocalDate()).isNull();
        }
    }
}