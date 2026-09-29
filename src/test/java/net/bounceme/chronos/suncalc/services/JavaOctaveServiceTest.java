package net.bounceme.chronos.suncalc.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ange.octave.OctaveEngine;
import dk.ange.octave.OctaveEngineFactory;
import dk.ange.octave.OctaveUtils;
import dk.ange.octave.exception.OctaveEvalException;
import dk.ange.octave.exception.OctaveIOException;
import dk.ange.octave.type.OctaveDouble;
import dk.ange.octave.type.OctaveString;
import net.bounceme.chronos.suncalc.services.impl.JavaOctaveService;
import net.bounceme.chronos.utils.calc.converters.OctaveDoubleToArray;

@ExtendWith(MockitoExtension.class)
@DisplayName("JavaOctaveService - integración con motor Octave")
class JavaOctaveServiceTest {

    private static final String SERVICE_UNAVAILABLE = "Servicio no disponible";

    private MockedConstruction<OctaveEngineFactory> factoryConstruction;
    private MockedConstruction<OctaveDoubleToArray> arrayConstruction;
    private MockedStatic<OctaveUtils> octaveUtilsStatic;

    private OctaveEngine octave;
    private OctaveDoubleToArray octaveDoubleToArray;

    @AfterEach
    void tearDown() {
        if (factoryConstruction != null && !factoryConstruction.isClosed()) {
            factoryConstruction.close();
        }
        if (arrayConstruction != null && !arrayConstruction.isClosed()) {
            arrayConstruction.close();
        }
        if (octaveUtilsStatic != null && !octaveUtilsStatic.isClosed()) {
            octaveUtilsStatic.close();
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private JavaOctaveService buildEnabledService() {
        octave = mock(OctaveEngine.class);

        OctaveString versionMock = mock(OctaveString.class);
        lenient().when(versionMock.getString()).thenReturn("1.0.0");
        lenient().when(octave.get(OctaveString.class, "astronomia_version")).thenReturn(versionMock);

        factoryConstruction = mockConstruction(OctaveEngineFactory.class,
                (mock, ctx) -> when(mock.getScriptEngine()).thenReturn(octave));
        arrayConstruction = mockConstruction(OctaveDoubleToArray.class);

        JavaOctaveService service = new JavaOctaveService();
        octaveDoubleToArray = arrayConstruction.constructed().get(0);
        return service;
    }

    private JavaOctaveService buildDisabledService() {
        factoryConstruction = mockConstruction(OctaveEngineFactory.class,
                (mock, ctx) -> when(mock.getScriptEngine())
                        .thenThrow(new OctaveIOException("boom")));
        return new JavaOctaveService();
    }

    // ==================================================================
    // Constructor
    // ==================================================================

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("Carga OK: deja el servicio habilitado y evalúa los packages")
        void constructor_cargaOk_servicioHabilitado() {
            // Arrange / Act
            JavaOctaveService service = buildEnabledService();

            // Assert
            verify(octave).eval("pkg load symbolic");
            verify(octave).eval("pkg load astronomia");
            verify(octave).eval("astronomia_version = astronomia_version()");
            // Está habilitado: podemos ejecutar sin excepción
            service.execute("1+1");
            verify(octave).eval("1+1");
        }

        @Test
        @DisplayName("Fallo en getScriptEngine: deja el servicio deshabilitado")
        void constructor_falloEnGetScriptEngine_servicioDeshabilitado() {
            // Arrange / Act
            JavaOctaveService service = buildDisabledService();

            // Assert
            assertThatThrownBy(() -> service.execute("1+1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(SERVICE_UNAVAILABLE);
        }

        @Test
        @DisplayName("Fallo en eval inicial: deja el servicio deshabilitado")
        void constructor_falloEnEvalInicial_servicioDeshabilitado() throws Exception {
            // Arrange
            octave = mock(OctaveEngine.class);
            // Las dos primeras eval van OK, la tercera (versión) revienta
            factoryConstruction = mockConstruction(OctaveEngineFactory.class,
                    (mock, ctx) -> when(mock.getScriptEngine()).thenReturn(octave));
            arrayConstruction = mockConstruction(OctaveDoubleToArray.class);
            org.mockito.Mockito.doThrow(new OctaveEvalException("pkg fail"))
                    .when(octave).eval(anyString());

            // Act
            JavaOctaveService service = new JavaOctaveService();

            // Assert
            assertThatThrownBy(() -> service.execute("1+1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(SERVICE_UNAVAILABLE);
        }
    }

    // ==================================================================
    // Operaciones básicas sobre el motor (habilitado)
    // ==================================================================

    @Nested
    @DisplayName("Operaciones sobre motor habilitado")
    class MotorHabilitadoTests {

        @Test
        @DisplayName("addPath construye el comando con addpath('%s')")
        void addPath_construyeComandoCorrecto() {
            // Arrange
            JavaOctaveService service = buildEnabledService();
            String path = "/opt/octave/scripts";

            // Act
            service.addPath(path);

            // Assert
            verify(octave).eval("addpath('/opt/octave/scripts')");
        }

        @Test
        @DisplayName("resetPath evalúa restoredefaultpath();")
        void resetPath_evaluaComandoCorrecto() {
            // Arrange
            JavaOctaveService service = buildEnabledService();

            // Act
            service.resetPath();

            // Assert
            verify(octave).eval("restoredefaultpath();");
        }

        @Test
        @DisplayName("clearEnvironment evalúa clear()")
        void clearEnvironment_evaluaComandoCorrecto() {
            // Arrange
            JavaOctaveService service = buildEnabledService();

            // Act
            service.clearEnvironment();

            // Assert
            verify(octave).eval("clear()");
        }

        @Test
        @DisplayName("execute delega directamente el comando en el motor")
        void execute_delegaEnMotor() {
            // Arrange
            JavaOctaveService service = buildEnabledService();
            String cmd = "a = 42";

            // Act
            service.execute(cmd);

            // Assert
            verify(octave).eval(cmd);
        }

        @Test
        @DisplayName("terminate cierra el motor")
        void terminate_cierraMotor() {
            // Arrange
            JavaOctaveService service = buildEnabledService();

            // Act
            service.terminate();

            // Assert
            verify(octave).close();
        }
    }

    // ==================================================================
    // passVariable
    // ==================================================================

    @Nested
    @DisplayName("passVariable")
    class PassVariableTests {

        @Test
        @DisplayName("BigDecimal: genera name=value")
        void passVariable_bigDecimal_generaAsignacion() {
            // Arrange
            JavaOctaveService service = buildEnabledService();
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

            // Act
            service.passVariable("alpha", new BigDecimal("3.14"));

            // Assert
            verify(octave, times(4)).eval(captor.capture());
            assertThat(captor.getValue()).isEqualTo("alpha=3.14");
        }

        @Test
        @DisplayName("Integer: genera name=value")
        void passVariable_integer_generaAsignacion() {
            // Arrange
            JavaOctaveService service = buildEnabledService();
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

            // Act
            service.passVariable("n", 7);

            // Assert
            verify(octave, times(4)).eval(captor.capture());
            assertThat(captor.getValue()).isEqualTo("n=7");
        }

        @Test
        @DisplayName("BigDecimal[]: pasa a través de VectorDTO.toString()")
        void passVariable_arrayBigDecimal_delegaEnVectorDTO() {
            // Arrange
            JavaOctaveService service = buildEnabledService();
            BigDecimal[] values = { BigDecimal.ONE, BigDecimal.TEN };

            // Act
            service.passVariable("v", values);

            // Assert
            verify(octave, times(4)).eval(anyString());
        }

        @Test
        @DisplayName("BigDecimal[][]: pasa a través de MatrixDTO.toString()")
        void passVariable_matrizBigDecimal_delegaEnMatrixDTO() {
            // Arrange
            JavaOctaveService service = buildEnabledService();
            BigDecimal[][] values = {
                    { BigDecimal.ONE, BigDecimal.TEN },
                    { BigDecimal.ZERO, BigDecimal.valueOf(2) }
            };

            // Act
            service.passVariable("m", values);

            // Assert
            verify(octave, times(4)).eval(anyString());
        }
    }

    // ==================================================================
    // Lectura de variables
    // ==================================================================

    @Nested
    @DisplayName("Lectura de variables")
    class LecturaTests {

        @Test
        @DisplayName("getScalar aplica escala 2 con HALF_UP")
        void getScalar_aplicaEscala2HALFUP() {
            // Arrange
            JavaOctaveService service = buildEnabledService();
            OctaveDouble doubleMock = mock(OctaveDouble.class);
            when(doubleMock.getData()).thenReturn(new double[]{ 1.235 });
            when(octave.get(OctaveDouble.class, "x")).thenReturn(doubleMock);

            // Act
            BigDecimal result = service.getScalar("x");

            // Assert
            assertThat(result).isEqualByComparingTo(new BigDecimal("1.24"));
        }

        @Test
        @DisplayName("getScalar redondea correctamente con valor negativo")
        void getScalar_valorNegativo_redondeaBien() {
            // Arrange
            JavaOctaveService service = buildEnabledService();
            OctaveDouble doubleMock = mock(OctaveDouble.class);
            when(doubleMock.getData()).thenReturn(new double[]{ -2.345 });
            when(octave.get(OctaveDouble.class, "neg")).thenReturn(doubleMock);

            // Act
            BigDecimal result = service.getScalar("neg");

            // Assert
            assertThat(result).isEqualByComparingTo(new BigDecimal("-2.35"));
        }

        @Test
        @DisplayName("getIntScalar redondea a entero con HALF_UP")
        void getIntScalar_redondeaAEntero() {
            // Arrange
            JavaOctaveService service = buildEnabledService();
            OctaveDouble doubleMock = mock(OctaveDouble.class);
            when(doubleMock.getData()).thenReturn(new double[]{ 5.5 });
            when(octave.get(OctaveDouble.class, "n")).thenReturn(doubleMock);

            // Act
            Integer result = service.getIntScalar("n");

            // Assert
            assertThat(result).isEqualTo(6);
        }

        @Test
        @DisplayName("getIntScalar redondea a la baja si la fracción es < 0.5")
        void getIntScalar_redondeaBajaSiFraccionMenor() {
            // Arrange
            JavaOctaveService service = buildEnabledService();
            OctaveDouble doubleMock = mock(OctaveDouble.class);
            when(doubleMock.getData()).thenReturn(new double[]{ 5.4 });
            when(octave.get(OctaveDouble.class, "n")).thenReturn(doubleMock);

            // Act
            Integer result = service.getIntScalar("n");

            // Assert
            assertThat(result).isEqualTo(5);
        }

        @Test
        @DisplayName("getArray delega en OctaveDoubleToArray")
        void getArray_delegaEnConverter() {
            // Arrange
            JavaOctaveService service = buildEnabledService();
            OctaveDouble doubleMock = mock(OctaveDouble.class);
            BigDecimal[] expected = { BigDecimal.ONE, BigDecimal.TEN };
            when(octave.get(OctaveDouble.class, "v")).thenReturn(doubleMock);
            when(octaveDoubleToArray.apply(doubleMock)).thenReturn(expected);

            // Act
            BigDecimal[] result = service.getArray("v");

            // Assert
            assertThat(result).containsExactly(BigDecimal.ONE, BigDecimal.TEN);
            verify(octaveDoubleToArray).apply(doubleMock);
        }

        @Test
        @DisplayName("getString devuelve el String del OctaveString")
        void getString_devuelveCadena() {
            // Arrange
            JavaOctaveService service = buildEnabledService();
            OctaveString stringMock = mock(OctaveString.class);
            when(stringMock.getString()).thenReturn("hola");
            when(octave.get(OctaveString.class, "s")).thenReturn(stringMock);

            // Act
            String result = service.getString("s");

            // Assert
            assertThat(result).isEqualTo("hola");
        }

        @Test
        @DisplayName("getVars delega en OctaveUtils.listVars y convierte a lista")
        void getVars_listaVariables() {
            // Arrange
            JavaOctaveService service = buildEnabledService();
            octaveUtilsStatic = mockStatic(OctaveUtils.class);
            octaveUtilsStatic.when(() -> OctaveUtils.listVars(octave))
                    .thenReturn(List.of("a", "b", "c"));

            // Act
            List<String> result = service.getVars();

            // Assert
            assertThat(result).containsExactly("a", "b", "c");
            octaveUtilsStatic.verify(() -> OctaveUtils.listVars(octave));
        }
    }

    // ==================================================================
    // Comportamiento con servicio deshabilitado
    // ==================================================================

    @Nested
    @DisplayName("Servicio deshabilitado")
    class ServicioDeshabilitadoTests {

        @Test
        @DisplayName("addPath lanza IllegalArgumentException")
        void addPath_deshabilitado_lanzaExcepcion() {
            // Arrange
            JavaOctaveService service = buildDisabledService();

            // Act / Assert
            assertThatThrownBy(() -> service.addPath("/tmp"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(SERVICE_UNAVAILABLE);
        }

        @Test
        @DisplayName("resetPath lanza IllegalArgumentException")
        void resetPath_deshabilitado_lanzaExcepcion() {
            // Arrange
            JavaOctaveService service = buildDisabledService();

            // Act / Assert
            assertThatThrownBy(service::resetPath)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(SERVICE_UNAVAILABLE);
        }

        @Test
        @DisplayName("clearEnvironment lanza IllegalArgumentException")
        void clearEnvironment_deshabilitado_lanzaExcepcion() {
            // Arrange
            JavaOctaveService service = buildDisabledService();

            // Act / Assert
            assertThatThrownBy(service::clearEnvironment)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(SERVICE_UNAVAILABLE);
        }

        @Test
        @DisplayName("execute lanza IllegalArgumentException")
        void execute_deshabilitado_lanzaExcepcion() {
            // Arrange
            JavaOctaveService service = buildDisabledService();

            // Act / Assert
            assertThatThrownBy(() -> service.execute("1+1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(SERVICE_UNAVAILABLE);
        }

        @Test
        @DisplayName("passVariable(BigDecimal) lanza IllegalArgumentException")
        void passVariable_bigDecimal_deshabilitado_lanzaExcepcion() {
            // Arrange
            JavaOctaveService service = buildDisabledService();

            // Act / Assert
            assertThatThrownBy(() -> service.passVariable("x", BigDecimal.ONE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(SERVICE_UNAVAILABLE);
        }

        @Test
        @DisplayName("passVariable(Integer) lanza IllegalArgumentException")
        void passVariable_integer_deshabilitado_lanzaExcepcion() {
            // Arrange
            JavaOctaveService service = buildDisabledService();

            // Act / Assert
            assertThatThrownBy(() -> service.passVariable("x", 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(SERVICE_UNAVAILABLE);
        }

        @Test
        @DisplayName("passVariable(BigDecimal[]) lanza IllegalArgumentException")
        void passVariable_array_deshabilitado_lanzaExcepcion() {
            // Arrange
            JavaOctaveService service = buildDisabledService();

            // Act / Assert
            assertThatThrownBy(() -> service.passVariable("x", new BigDecimal[]{ BigDecimal.ONE }))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(SERVICE_UNAVAILABLE);
        }

        @Test
        @DisplayName("passVariable(BigDecimal[][]) lanza IllegalArgumentException")
        void passVariable_matriz_deshabilitado_lanzaExcepcion() {
            // Arrange
            JavaOctaveService service = buildDisabledService();

            // Act / Assert
            assertThatThrownBy(() -> service.passVariable("x", new BigDecimal[][]{ { BigDecimal.ONE } }))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(SERVICE_UNAVAILABLE);
        }
    }
}