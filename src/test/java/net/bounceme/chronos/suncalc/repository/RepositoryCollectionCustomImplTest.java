package net.bounceme.chronos.suncalc.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import net.bounceme.chronos.suncalc.repository.impl.RepositoryCollectionCustomImpl;

@DisplayName("RepositoryCollectionCustomImpl - contenedor del nombre de colección")
class RepositoryCollectionCustomImplTest {

    private RepositoryCollectionCustomImpl repo;

    @BeforeEach
    void setUp() {
        repo = new RepositoryCollectionCustomImpl();
    }

    // ==================================================================
    // Estado inicial
    // ==================================================================

    @Nested
    @DisplayName("Estado inicial")
    class EstadoInicialTests {

        @Test
        @DisplayName("collectionName arranca a null")
        void estadoInicial_collectionNameNull() {
            assertThat(repo.getCollectionName()).isNull();
        }

        @Test
        @DisplayName("Existe constructor público sin argumentos")
        void constructor_publicoSinArgumentos() {
            assertThatCode(RepositoryCollectionCustomImpl::new)
                    .doesNotThrowAnyException();
        }
    }

    // ==================================================================
    // getter / setter
    // ==================================================================

    @Nested
    @DisplayName("getCollectionName / setCollectionName")
    class GetterSetterTests {

        @Test
        @DisplayName("setCollectionName + getCollectionName devuelve el mismo valor")
        void setYGet_devuelvenMismoValor() {
            // Act
            repo.setCollectionName("suncalc-collection");

            // Assert
            assertThat(repo.getCollectionName()).isEqualTo("suncalc-collection");
        }

        @Test
        @DisplayName("setCollectionName(null) deja el campo a null")
        void setNull_quedaNull() {
            // Arrange
            repo.setCollectionName("algo");

            // Act
            repo.setCollectionName(null);

            // Assert
            assertThat(repo.getCollectionName()).isNull();
        }

        @Test
        @DisplayName("setCollectionName sobreescribe el valor previo")
        void setSobrescribe() {
            // Act
            repo.setCollectionName("primero");
            repo.setCollectionName("segundo");

            // Assert
            assertThat(repo.getCollectionName()).isEqualTo("segundo");
        }

        @Test
        @DisplayName("Acepta cadena vacía")
        void setCadenaVacia() {
            // Act
            repo.setCollectionName("");

            // Assert
            assertThat(repo.getCollectionName()).isEmpty();
        }
    }

    // ==================================================================
    // Independencia entre instancias
    // ==================================================================

    @Nested
    @DisplayName("Independencia entre instancias")
    class IndependenciaTests {

        @Test
        @DisplayName("Dos instancias no comparten estado (el campo no es estático)")
        void instancias_noCompartenEstado() {
            // Arrange
            RepositoryCollectionCustomImpl a = new RepositoryCollectionCustomImpl();
            RepositoryCollectionCustomImpl b = new RepositoryCollectionCustomImpl();

            // Act
            a.setCollectionName("A");
            b.setCollectionName("B");

            // Assert
            assertThat(a.getCollectionName()).isEqualTo("A");
            assertThat(b.getCollectionName()).isEqualTo("B");
        }

        @Test
        @DisplayName("Modificar una instancia no afecta a la otra")
        void instancias_aisladas() {
            // Arrange
            RepositoryCollectionCustomImpl a = new RepositoryCollectionCustomImpl();
            RepositoryCollectionCustomImpl b = new RepositoryCollectionCustomImpl();
            a.setCollectionName("compartido-inicial");
            b.setCollectionName("compartido-inicial");

            // Act
            a.setCollectionName("modificado");

            // Assert
            assertThat(a.getCollectionName()).isEqualTo("modificado");
            assertThat(b.getCollectionName()).isEqualTo("compartido-inicial");
        }
    }

    // ==================================================================
    // Contrato de la interfaz
    // ==================================================================

    @Nested
    @DisplayName("Contrato de la interfaz")
    class ContratoInterfazTests {

        @Test
        @DisplayName("Implementa RepositoryCollectionCustom")
        void implementaInterfaz() {
            assertThat(repo)
                    .isInstanceOf(net.bounceme.chronos.suncalc.repository.RepositoryCollectionCustom.class);
        }

        @Test
        @DisplayName("Asignable a la interfaz sin cast")
        void asignableAInterfaz() {
            // Arrange
            net.bounceme.chronos.suncalc.repository.RepositoryCollectionCustom asInterface = repo;

            // Act
            asInterface.setCollectionName("via-interfaz");

            // Assert
            assertThat(repo.getCollectionName()).isEqualTo("via-interfaz");
        }
    }
}