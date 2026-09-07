package org.nexus.nexussolairy.service.analysis;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.nexus.nexussolairy.PigLatinLexer;
import org.nexus.nexussolairy.PigLatinParser;
import org.nexus.nexussolairy.model.enums.LanguageType;
import org.nexus.nexussolairy.model.semantic.SemanticError;
import org.nexus.nexussolairy.model.semantic.Symbol;
import org.nexus.nexussolairy.model.syntactic.SyntaxError;
import org.nexus.nexussolairy.patron.LexerFactory;
import org.nexus.nexussolairy.patron.ParserFactory;
import org.nexus.nexussolairy.patron.VisitorFactory;
import org.nexus.nexussolairy.service.grammar.LexerService;
import org.nexus.nexussolairy.service.parser.ParserService;
import org.nexus.nexussolairy.utils.ResultLexer;
import org.nexus.nexussolairy.visitor.InputProvider;
import org.nexus.nexussolairy.visitor.VisitorContext;
import org.nexus.nexussolairy.visitor.pigLatin.PigLatinVisitorImpl;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class AnalysisPipeline {

    public PipelineResult analyze(String source) {
        return analyze(source, LanguageType.PIG_LATIN, () -> "", null);
    }

    public PipelineResult analyze(String source, LanguageType language) {
        return analyze(source, language, () -> "", null);
    }

    public PipelineResult analyze(String source, LanguageType language, InputProvider inputProvider, Consumer<String> livePrinter) {
        PipelineResult result = new PipelineResult();

        LexerService lexer = LexerFactory.create(language);
        if (lexer == null) {
            result.setValid(false);
            result.setMessage("No se encontró analizador léxico para " + language.getDisplayName());
            return result;
        }

        ResultLexer lexical = lexer.analyze(source);
        result.setLexicalResult(lexical);
        if (!lexical.isValid()) {
            result.setValid(false);
            result.setMessage("Errores léxicos detectados.");
            return result;
        }

        ParserService parser = ParserFactory.create(language);
        if (parser == null) {
            result.setValid(false);
            result.setMessage("No se encontró analizador sintáctico para " + language.getDisplayName());
            return result;
        }

        List<SyntaxError> syntacticErrors = parser.analyze(source);
        result.setSyntacticErrors(syntacticErrors);
        if (!syntacticErrors.isEmpty()) {
            result.setValid(false);
            result.setMessage("Errores sintácticos detectados.");
            return result;
        }

        VisitorContext visitor = VisitorFactory.create(language, inputProvider, livePrinter);
        if (visitor instanceof PigLatinVisitorImpl pigVisitor) {
            PigLatinLexer plLexer = new PigLatinLexer(CharStreams.fromString(source));
            plLexer.removeErrorListeners();
            CommonTokenStream tokens = new CommonTokenStream(plLexer);
            PigLatinParser plParser = new PigLatinParser(tokens);
            plParser.removeErrorListeners();
            PigLatinParser.ProgramContext tree = plParser.program();

            pigVisitor.visit(tree);

            result.setSymbols(pigVisitor.getSymbolTable().getAllVariablesLog());
            result.setSemanticErrors(pigVisitor.getErrors());
            result.setPrintOutput(pigVisitor.getPrintOutput());

            if (pigVisitor.hasErrors()) {
                result.setValid(false);
                result.setMessage("Errores semánticos detectados.");
                return result;
            }
        }

        result.setValid(true);
        result.setMessage("Análisis completado sin errores.");
        return result;
    }

    public static class PipelineResult {
        private boolean valid;
        private String message;
        private ResultLexer lexicalResult;
        private List<SyntaxError> syntacticErrors = Collections.emptyList();
        private List<Symbol> symbols = Collections.emptyList();
        private List<SemanticError> semanticErrors = Collections.emptyList();
        private List<String> printOutput = Collections.emptyList();

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public ResultLexer getLexicalResult() {
            return lexicalResult;
        }

        public void setLexicalResult(ResultLexer lexicalResult) {
            this.lexicalResult = lexicalResult;
        }

        public List<SyntaxError> getSyntacticErrors() {
            return syntacticErrors;
        }

        public void setSyntacticErrors(List<SyntaxError> syntacticErrors) {
            this.syntacticErrors = syntacticErrors;
        }

        public List<Symbol> getSymbols() {
            return symbols;
        }

        public void setSymbols(List<Symbol> symbols) {
            this.symbols = symbols;
        }

        public List<SemanticError> getSemanticErrors() {
            return semanticErrors;
        }

        public void setSemanticErrors(List<SemanticError> semanticErrors) {
            this.semanticErrors = semanticErrors;
        }

        public List<String> getPrintOutput() {
            return printOutput;
        }

        public void setPrintOutput(List<String> printOutput) {
            this.printOutput = printOutput;
        }
    }
}
