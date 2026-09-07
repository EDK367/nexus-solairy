package org.nexus.nexussolairy.service.analysis;

import org.junit.jupiter.api.Test;
import org.nexus.nexussolairy.model.enums.LanguageType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AnalysisPipelinePigTest {

    @Test
    public void testSuccessfulPigLatinAnalysis() {
        String source = """
                VARIABILES >
                esto a : numerus 10;
                esto b : numerus 20;
                MAIOR >
                esto c : numerus a + b;
                >> c;
                FINIS;
                """;

        AnalysisPipeline pipeline = new AnalysisPipeline();
        AnalysisPipeline.PipelineResult result = pipeline.analyze(source, LanguageType.PIG_LATIN);

        assertTrue(result.isValid());
        assertTrue(result.getSyntacticErrors().isEmpty());
        assertTrue(result.getSemanticErrors().isEmpty());
        assertEquals(1, result.getPrintOutput().size());
        assertEquals("30", result.getPrintOutput().get(0));
    }

    @Test
    public void testSemanticErrorPigLatin() {
        String source = """
                VARIABILES >
                esto a : numerus "hola";
                MAIOR >
                FINIS;
                """;

        AnalysisPipeline pipeline = new AnalysisPipeline();
        AnalysisPipeline.PipelineResult result = pipeline.analyze(source, LanguageType.PIG_LATIN);

        assertFalse(result.isValid());
        assertFalse(result.getSemanticErrors().isEmpty());
    }

    @Test
    public void testSyntacticErrorPigLatin() {
        String source = """
                VARIABILES >
                esto a numerus 10;
                MAIOR >
                FINIS;
                """;

        AnalysisPipeline pipeline = new AnalysisPipeline();
        AnalysisPipeline.PipelineResult result = pipeline.analyze(source, LanguageType.PIG_LATIN);

        assertFalse(result.isValid());
        assertFalse(result.getSyntacticErrors().isEmpty());
    }

    @Test
    public void testWhileLoopExecution() {
        String source = """
                VARIABILES >
                esto x : numerus 0;
                MAIOR >
                dum (x < 3) {
                    >> x;
                    x = x + 1;
                } finis;
                FINIS;
                """;

        AnalysisPipeline pipeline = new AnalysisPipeline();
        AnalysisPipeline.PipelineResult result = pipeline.analyze(source, LanguageType.PIG_LATIN);

        assertTrue(result.isValid(), () -> "Errors: " + result.getSemanticErrors());
        assertEquals(List.of("0", "1", "2"), result.getPrintOutput());
    }

    @Test
    public void testWhileLoopWithBreak() {
        String source = """
                VARIABILES >
                esto x : numerus 0;
                MAIOR >
                dum (x < 10) {
                    si (x == 2) {
                        interrumpe;
                    } finis;
                    >> x;
                    x = x + 1;
                } finis;
                FINIS;
                """;

        AnalysisPipeline pipeline = new AnalysisPipeline();
        AnalysisPipeline.PipelineResult result = pipeline.analyze(source, LanguageType.PIG_LATIN);

        assertTrue(result.isValid());
        assertEquals(List.of("0", "1"), result.getPrintOutput());
    }

    @Test
    public void testWhileLoopWithContinue() {
        String source = """
                VARIABILES >
                esto x : numerus 0;
                MAIOR >
                dum (x < 4) {
                    x = x + 1;
                    si (x == 2) {
                        perge;
                    } finis;
                    >> x;
                } finis;
                FINIS;
                """;

        AnalysisPipeline pipeline = new AnalysisPipeline();
        AnalysisPipeline.PipelineResult result = pipeline.analyze(source, LanguageType.PIG_LATIN);

        assertTrue(result.isValid());
        assertEquals(List.of("1", "3", "4"), result.getPrintOutput());
    }

    @Test
    public void testDoWhileLoopExecution() {
        String source = """
                VARIABILES >
                esto x : numerus 0;
                MAIOR >
                facere {
                    >> x;
                    x = x + 1;
                } dum (x < 3);
                FINIS;
                """;

        AnalysisPipeline pipeline = new AnalysisPipeline();
        AnalysisPipeline.PipelineResult result = pipeline.analyze(source, LanguageType.PIG_LATIN);

        assertTrue(result.isValid(), () -> "Errors: " + result.getSemanticErrors());
        assertEquals(List.of("0", "1", "2"), result.getPrintOutput());
    }

    @Test
    public void testForLoopExecution() {
        String source = """
                MAIOR >
                per (esto i : numerus 0; i < 3; i++) {
                    >> i;
                }
                FINIS;
                """;

        AnalysisPipeline pipeline = new AnalysisPipeline();
        AnalysisPipeline.PipelineResult result = pipeline.analyze(source, LanguageType.PIG_LATIN);

        assertTrue(result.isValid(), () -> "Errors: " + result.getSemanticErrors());
        assertEquals(List.of("0", "1", "2"), result.getPrintOutput());
    }

    @Test
    public void testForLoopWithBreakAndContinue() {
        String source = """
                MAIOR >
                per (esto i : numerus 0; i < 5; i++) {
                    si (i == 1) {
                        perge;
                    } finis;
                    si (i == 3) {
                        interrumpe;
                    } finis;
                    >> i;
                }
                FINIS;
                """;

        AnalysisPipeline pipeline = new AnalysisPipeline();
        AnalysisPipeline.PipelineResult result = pipeline.analyze(source, LanguageType.PIG_LATIN);

        assertTrue(result.isValid());
        assertEquals(List.of("0", "2"), result.getPrintOutput());
    }

    @Test
    public void testNestedLoops() {
        String source = """
                MAIOR >
                per (esto i : numerus 0; i < 2; i++) {
                    per (esto j : numerus 0; j < 2; j++) {
                        >> i;
                        >> j;
                    }
                }
                FINIS;
                """;

        AnalysisPipeline pipeline = new AnalysisPipeline();
        AnalysisPipeline.PipelineResult result = pipeline.analyze(source, LanguageType.PIG_LATIN);

        assertTrue(result.isValid());
        assertEquals(List.of("0", "0", "0", "1", "1", "0", "1", "1"), result.getPrintOutput());
    }

    @Test
    public void testArrayInLoop() {
        String source = """
                VARIABILES >
                series arr[3] : numerus { 10, 20, 30 };
                MAIOR >
                per (esto i : numerus 0; i < 3; i++) {
                    >> arr[i];
                }
                FINIS;
                """;

        AnalysisPipeline pipeline = new AnalysisPipeline();
        AnalysisPipeline.PipelineResult result = pipeline.analyze(source, LanguageType.PIG_LATIN);

        assertTrue(result.isValid(), () -> "Errors: " + result.getSemanticErrors());
        assertEquals(List.of("10", "20", "30"), result.getPrintOutput());
    }

    @Test
    public void testLocalVarInLoopBody() {
        String source = """
                VARIABILES >
                esto x : numerus 0;
                MAIOR >
                dum (x < 3) {
                    esto temp : numerus x + 10;
                    >> temp;
                    x = x + 1;
                } finis;
                FINIS;
                """;

        AnalysisPipeline pipeline = new AnalysisPipeline();
        AnalysisPipeline.PipelineResult result = pipeline.analyze(source, LanguageType.PIG_LATIN);

        assertTrue(result.isValid(), () -> "Errors: " + result.getSemanticErrors());
        assertEquals(List.of("10", "11", "12"), result.getPrintOutput());
    }

    @Test
    public void testForLoopDecrement() {
        String source = """
                MAIOR >
                per (esto i : numerus 3; i > 0; i--) {
                    >> i;
                }
                FINIS;
                """;

        AnalysisPipeline pipeline = new AnalysisPipeline();
        AnalysisPipeline.PipelineResult result = pipeline.analyze(source, LanguageType.PIG_LATIN);

        assertTrue(result.isValid(), () -> "Errors: " + result.getSemanticErrors());
        assertEquals(List.of("3", "2", "1"), result.getPrintOutput());
    }

    @Test
    public void testNonBooleanConditionError() {
        String source = """
                MAIOR >
                dum (10 + 20) {
                    >> 1;
                } finis;
                FINIS;
                """;

        AnalysisPipeline pipeline = new AnalysisPipeline();
        AnalysisPipeline.PipelineResult result = pipeline.analyze(source, LanguageType.PIG_LATIN);

        assertFalse(result.isValid());
        assertFalse(result.getSemanticErrors().isEmpty());
    }

    @Test
    public void testBreakOutsideLoopError() {
        String source = """
                MAIOR >
                interrumpe;
                FINIS;
                """;

        AnalysisPipeline pipeline = new AnalysisPipeline();
        AnalysisPipeline.PipelineResult result = pipeline.analyze(source, LanguageType.PIG_LATIN);

        assertFalse(result.isValid());
        assertFalse(result.getSemanticErrors().isEmpty());
    }
}
