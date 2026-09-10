package org.nexus.nexussolairy.service.analysis;

import org.junit.jupiter.api.Test;
import org.nexus.nexussolairy.model.enums.LanguageType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AnalysisPipelineYTest {

    @Test
    public void testSuccessfulYAnalysis() {
        String source = """
                %estructuras
                estructura Persona:
                    cadena nombre
                    entero edad
                    flotante promedio

                %funciones
                definir saludar(cadena mensaje):
                    imprimir(mensaje)

                definir esMayor(entero edad) -> bool:
                    si (edad >= 18) entonces
                        retornar verdadero
                    contrario
                        retornar falso

                definir principal():
                    entero contador = 0
                    flotante pi = 3.14
                    bool activo = falso
                    cadena nombre = "Resistencia"
                    imprimir(nombre)
                """;

        AnalysisPipeline pipeline = new AnalysisPipeline();
        AnalysisPipeline.PipelineResult result = pipeline.analyze(source, LanguageType.Y_LANG);

        assertTrue(result.isValid(), () -> "Errors: " + result.getSemanticErrors());
        assertTrue(result.getSyntacticErrors().isEmpty());
        assertTrue(result.getSemanticErrors().isEmpty());
        assertEquals(1, result.getPrintOutput().size());
        assertEquals("Resistencia", result.getPrintOutput().get(0));
    }

    @Test
    public void testSemanticErrorTypeMismatch() {
        String source = """
                %funciones
                definir principal():
                    entero x = "hola"
                """;

        AnalysisPipeline pipeline = new AnalysisPipeline();
        AnalysisPipeline.PipelineResult result = pipeline.analyze(source, LanguageType.Y_LANG);

        assertFalse(result.isValid());
        assertFalse(result.getSemanticErrors().isEmpty());
    }

    @Test
    public void testSemanticErrorUndeclaredVariable() {
        String source = """
                %funciones
                definir principal():
                    x = 10
                """;

        AnalysisPipeline pipeline = new AnalysisPipeline();
        AnalysisPipeline.PipelineResult result = pipeline.analyze(source, LanguageType.Y_LANG);

        assertFalse(result.isValid());
        assertFalse(result.getSemanticErrors().isEmpty());
    }

    @Test
    public void testSemanticErrorNonBooleanCondition() {
        String source = """
                %funciones
                definir principal():
                    si (10 + 20) entonces
                        imprimir("hola")
                """;

        AnalysisPipeline pipeline = new AnalysisPipeline();
        AnalysisPipeline.PipelineResult result = pipeline.analyze(source, LanguageType.Y_LANG);

        assertFalse(result.isValid());
        assertFalse(result.getSemanticErrors().isEmpty());
    }

    @Test
    public void testSemanticErrorIncompatibleReturn() {
        String source = """
                %funciones
                definir obtenerNumero() -> entero:
                    retornar "no es numero"
                """;

        AnalysisPipeline pipeline = new AnalysisPipeline();
        AnalysisPipeline.PipelineResult result = pipeline.analyze(source, LanguageType.Y_LANG);

        assertFalse(result.isValid());
        assertFalse(result.getSemanticErrors().isEmpty());
    }

    @Test
    public void testSemanticErrorBreakOutsideLoop() {
        String source = """
                %funciones
                definir principal():
                    romper
                """;

        AnalysisPipeline pipeline = new AnalysisPipeline();
        AnalysisPipeline.PipelineResult result = pipeline.analyze(source, LanguageType.Y_LANG);

        assertFalse(result.isValid());
        assertFalse(result.getSemanticErrors().isEmpty());
    }

    @Test
    public void testNumericWideningAssignment() {
        String source = """
                %funciones
                definir principal():
                    entero a = 10
                    flotante b = a
                    imprimir(b)
                """;

        AnalysisPipeline pipeline = new AnalysisPipeline();
        AnalysisPipeline.PipelineResult result = pipeline.analyze(source, LanguageType.Y_LANG);

        assertTrue(result.isValid(), () -> "Errors: " + result.getSemanticErrors());
        assertTrue(result.getSemanticErrors().isEmpty());
    }

    @Test
    public void testArraySemanticValidation() {
        String source = """
                %funciones
                definir principal():
                    entero numeros[3] = {1, 2, 3}
                    imprimir(numeros[0])
                """;

        AnalysisPipeline pipeline = new AnalysisPipeline();
        AnalysisPipeline.PipelineResult result = pipeline.analyze(source, LanguageType.Y_LANG);

        assertTrue(result.isValid(), () -> "Errors: " + result.getSemanticErrors());
        assertTrue(result.getSemanticErrors().isEmpty());
    }

    @Test
    public void testInvalidArrayIndexError() {
        String source = """
                %funciones
                definir principal():
                    entero numeros[3]
                    numeros["cero"] = 5
                """;

        AnalysisPipeline pipeline = new AnalysisPipeline();
        AnalysisPipeline.PipelineResult result = pipeline.analyze(source, LanguageType.Y_LANG);

        assertFalse(result.isValid());
        assertFalse(result.getSemanticErrors().isEmpty());
    }

    @Test
    public void testWhileAndForLoopValidation() {
        String source = """
                %funciones
                definir principal():
                    entero contador = 0
                    mientras (contador < 3) hacer
                        contador++
                    para (entero i = 0; i < 3; i++):
                        imprimir(i)
                """;

        AnalysisPipeline pipeline = new AnalysisPipeline();
        AnalysisPipeline.PipelineResult result = pipeline.analyze(source, LanguageType.Y_LANG);

        assertTrue(result.isValid(), () -> "Errors: " + result.getSemanticErrors());
        assertTrue(result.getSemanticErrors().isEmpty());
    }

    @Test
    public void testStructFieldAccess() {
        String source = """
                %estructuras
                estructura Punto:
                    entero x
                    entero y

                %funciones
                definir principal():
                    Punto p = {10, 20}
                    p.x = 15
                    entero val = p.x
                    imprimir(val)
                """;

        AnalysisPipeline pipeline = new AnalysisPipeline();
        AnalysisPipeline.PipelineResult result = pipeline.analyze(source, LanguageType.Y_LANG);

        assertTrue(result.isValid(), () -> "Errors: " + result.getSemanticErrors());
        assertTrue(result.getSemanticErrors().isEmpty());
    }

    @Test
    public void testStructMissingFieldError() {
        String source = """
                %estructuras
                estructura Punto:
                    entero x

                %funciones
                definir principal():
                    Punto p = {10}
                    p.z = 99
                """;

        AnalysisPipeline pipeline = new AnalysisPipeline();
        AnalysisPipeline.PipelineResult result = pipeline.analyze(source, LanguageType.Y_LANG);

        assertFalse(result.isValid());
        assertFalse(result.getSemanticErrors().isEmpty());
    }

    @Test
    public void testFunctionCallWrongArguments() {
        String source = """
                %funciones
                definir sumar(entero a, entero b) -> entero:
                    retornar a + b

                definir principal():
                    entero res = sumar(1)
                """;

        AnalysisPipeline pipeline = new AnalysisPipeline();
        AnalysisPipeline.PipelineResult result = pipeline.analyze(source, LanguageType.Y_LANG);

        assertFalse(result.isValid());
        assertFalse(result.getSemanticErrors().isEmpty());
    }

    @Test
    public void testDuplicateFunctionError() {
        String source = """
                %funciones
                definir foo():
                    imprimir("uno")

                definir foo():
                    imprimir("dos")

                definir principal():
                    entero x = 0
                """;

        AnalysisPipeline pipeline = new AnalysisPipeline();
        AnalysisPipeline.PipelineResult result = pipeline.analyze(source, LanguageType.Y_LANG);

        assertFalse(result.isValid());
        assertFalse(result.getSemanticErrors().isEmpty());
    }

    @Test
    public void testDuplicateStructError() {
        String source = """
                %estructuras
                estructura Auto:
                    cadena marca

                estructura Auto:
                    entero velocidad

                %funciones
                definir principal():
                    entero x = 1
                """;

        AnalysisPipeline pipeline = new AnalysisPipeline();
        AnalysisPipeline.PipelineResult result = pipeline.analyze(source, LanguageType.Y_LANG);

        assertFalse(result.isValid());
        assertFalse(result.getSemanticErrors().isEmpty());
    }

    @Test
    public void testSwitchCaseTypeMismatch() {
        String source = """
                %funciones
                definir principal():
                    entero x = 2
                    elegir (x):
                        caso "dos":
                            imprimir(x)
                """;

        AnalysisPipeline pipeline = new AnalysisPipeline();
        AnalysisPipeline.PipelineResult result = pipeline.analyze(source, LanguageType.Y_LANG);

        assertFalse(result.isValid());
        assertFalse(result.getSemanticErrors().isEmpty());
    }
}
