package org.nexus.nexussolairy.service.analysis;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.nexus.nexussolairy.PigLatinLexer;
import org.nexus.nexussolairy.PigLatinParser;
import org.nexus.nexussolairy.YParser;
import org.nexus.nexussolairy.ZetarianoLexer;
import org.nexus.nexussolairy.ZetarianoParser;
import org.nexus.nexussolairy.backend.vm.C3DVirtualMachine;
import org.nexus.nexussolairy.c3d.MemoryLayout;
import org.nexus.nexussolairy.c3d.PigLatinC3DVisitor;
import org.nexus.nexussolairy.c3d.YC3DVisitor;
import org.nexus.nexussolairy.c3d.ZetarianoC3DVisitor;
import org.nexus.nexussolairy.model.c3d.C3DProgram;
import org.nexus.nexussolairy.model.enums.LanguageType;
import org.nexus.nexussolairy.model.enums.TypeErrorSemantic;
import org.nexus.nexussolairy.model.semantic.*;
import org.nexus.nexussolairy.model.syntactic.SyntaxError;
import org.nexus.nexussolairy.patron.LexerFactory;
import org.nexus.nexussolairy.patron.ParserFactory;
import org.nexus.nexussolairy.patron.VisitorFactory;
import org.nexus.nexussolairy.service.grammar.LexerService;
import org.nexus.nexussolairy.service.parser.ParserService;
import org.nexus.nexussolairy.service.parser.YIdentationLexer;
import org.nexus.nexussolairy.utils.ResultLexer;
import org.nexus.nexussolairy.visitor.InputProvider;
import org.nexus.nexussolairy.visitor.VisitorContext;
import org.nexus.nexussolairy.visitor.pigLatin.PigLatinVisitorImpl;
import org.nexus.nexussolairy.visitor.yLanguage.YVisitorImpl;
import org.nexus.nexussolairy.visitor.zetariano.ZetarianoVisitorImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class AnalysisPipeline {

    public PipelineResult analyze(String source) {
        return analyze(source, null, LanguageType.PIG_LATIN, () -> "", null);
    }

    public PipelineResult analyze(String source, LanguageType language) {
        return analyze(source, null, language, () -> "", null);
    }

    public PipelineResult analyze(String source, String fileName, LanguageType language) {
        return analyze(source, fileName, language, () -> "", null);
    }

    public PipelineResult analyze(String source, LanguageType language, InputProvider inputProvider, Consumer<String> livePrinter) {
        return analyze(source, null, language, inputProvider, livePrinter);
    }

    public PipelineResult analyze(String source, String fileName, LanguageType language, InputProvider inputProvider, Consumer<String> livePrinter) {
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

        String baseDirectory = resolveBaseDirectory(fileName);
        List<SemanticError> importErrors = new ArrayList<>();
        SymbolTable preloadedTable = buildPreloadedSymbolTable(source, language, baseDirectory, importErrors);

        if (!importErrors.isEmpty()) {
            result.setSemanticErrors(importErrors);
            result.setValid(false);
            result.setMessage("Errores de importacion detectados.");
            return result;
        }
        VisitorContext visitor = VisitorFactory.create(language, preloadedTable, inputProvider, livePrinter);

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
        } else if (visitor instanceof YVisitorImpl yVisitor) {
            YIdentationLexer yLexer = new YIdentationLexer(CharStreams.fromString(source));
            yLexer.removeErrorListeners();
            CommonTokenStream tokens = new CommonTokenStream(yLexer);
            YParser yParser = new YParser(tokens);
            yParser.removeErrorListeners();
            YParser.ProgramContext tree = yParser.program();
            yVisitor.visit(tree);
            result.setSymbols(yVisitor.getSymbolTable().getAllVariablesLog());
            result.setSemanticErrors(yVisitor.getErrors());
            result.setPrintOutput(yVisitor.getPrintOutput());
            if (yVisitor.hasErrors()) {
                result.setValid(false);
                result.setMessage("Errores semánticos detectados.");
                return result;
            }
        } else if (visitor instanceof ZetarianoVisitorImpl zVisitor) {
            if (fileName != null) {
                zVisitor.setFileName(fileName);
            }
            ZetarianoLexer zLexer = new ZetarianoLexer(CharStreams.fromString(source));
            zLexer.removeErrorListeners();
            CommonTokenStream tokens = new CommonTokenStream(zLexer);
            ZetarianoParser zParser = new ZetarianoParser(tokens);
            zParser.removeErrorListeners();
            ZetarianoParser.ProgramContext tree = zParser.program();
            zVisitor.visit(tree);
            result.setSymbols(zVisitor.getSymbolTable().getAllVariablesLog());
            result.setSemanticErrors(zVisitor.getErrors());
            result.setPrintOutput(zVisitor.getPrintOutput());
            if (zVisitor.hasErrors()) {
                result.setValid(false);
                result.setMessage("Errores semánticos detectados.");
                return result;
            }
        }

        // generacion de codigo intermedio
        C3DProgram c3dProg = new C3DProgram();
        MemoryLayout memory = new MemoryLayout();

        List<String> importPaths = extractImportPaths(source, language);
        ImportResolver resolver = new ImportResolver();
        java.util.Set<String> processedImports = new java.util.HashSet<>();
        for (String importPath : importPaths) {
            String filePath = resolver.resolveFilePath(importPath, baseDirectory);
            if (filePath != null && processedImports.add(filePath)) {
                try {
                    String impSource = java.nio.file.Files.readString(java.nio.file.Path.of(filePath));
                    LanguageType impLang = LanguageType.fromFileName(filePath);
                    if (impLang == LanguageType.Y_LANG) {
                        YIdentationLexer yLexer = new YIdentationLexer(CharStreams.fromString(impSource));
                        YParser yParser = new YParser(new CommonTokenStream(yLexer));
                        YC3DVisitor yC3d = new YC3DVisitor(c3dProg, memory, preloadedTable);
                        yC3d.visit(yParser.program());
                    } else if (impLang == LanguageType.ZETARIANO) {
                        ZetarianoLexer zLexer = new ZetarianoLexer(CharStreams.fromString(impSource));
                        ZetarianoParser zParser = new ZetarianoParser(new CommonTokenStream(zLexer));
                        ZetarianoC3DVisitor zC3d = new ZetarianoC3DVisitor(c3dProg, memory, preloadedTable);
                        zC3d.visit(zParser.program());
                    }
                } catch (Exception ignored) {}
            }
        }

        if (language == LanguageType.PIG_LATIN) {
            PigLatinLexer plLexer = new PigLatinLexer(CharStreams.fromString(source));
            PigLatinParser plParser = new PigLatinParser(new CommonTokenStream(plLexer));
            PigLatinC3DVisitor c3dVisitor = new PigLatinC3DVisitor(c3dProg, memory, preloadedTable);
            c3dVisitor.visit(plParser.program());
        } else if (language == LanguageType.Y_LANG) {
            YIdentationLexer yLexer = new YIdentationLexer(CharStreams.fromString(source));
            YParser yParser = new YParser(new CommonTokenStream(yLexer));
            YC3DVisitor c3dVisitor = new YC3DVisitor(c3dProg, memory, preloadedTable);
            c3dVisitor.visit(yParser.program());
        } else if (language == LanguageType.ZETARIANO) {
            ZetarianoLexer zLexer = new ZetarianoLexer(CharStreams.fromString(source));
            ZetarianoParser zParser = new ZetarianoParser(new CommonTokenStream(zLexer));
            ZetarianoC3DVisitor c3dVisitor = new ZetarianoC3DVisitor(c3dProg, memory, preloadedTable);
            c3dVisitor.visit(zParser.program());
        }

        result.setC3dProgram(c3dProg);

        C3DVirtualMachine vm = new C3DVirtualMachine();
        vm.loadProgram(c3dProg);
        result.setVirtualMachine(vm);

        result.setValid(true);
        result.setMessage("Análisis y generación C3D completados.");
        return result;
    }

    private String resolveBaseDirectory(String fileName) {
        if (fileName == null || fileName.isBlank()) return System.getProperty("user.dir");
        java.io.File f = new java.io.File(fileName);
        String parent = f.getParent();
        return parent != null ? parent : System.getProperty("user.dir");
    }

    private SymbolTable buildPreloadedSymbolTable(String source, LanguageType language,
                                                   String baseDirectory, List<SemanticError> importErrors) {
        List<String> importPaths = extractImportPaths(source, language);
        if (importPaths.isEmpty()) return new SymbolTable();
        ImportResolver resolver = new ImportResolver();
        Scope globalScope = new GlobalScope();
        List<StructInfo> allStructs = new ArrayList<>();
        List<ClassSymbol> allClasses = new ArrayList<>();
        for (String importPath : importPaths) {
            ImportResolver.ImportResult imported = resolver.resolveImport(importPath, baseDirectory);

            if (imported.isEmpty()) {
                importErrors.add(new SemanticError(1, 0, "IMPORT_ERROR",
                    "No se pudo resolver la importacion: '" + importPath + "'. " +
                    "Verifique que el archivo exista en: " + baseDirectory));
                continue;
            }
            if (imported.errors != null && !imported.errors.isEmpty()) {
                importErrors.addAll(imported.errors);
            }
            for (Symbol sym : imported.symbols) {
                globalScope.declare(sym);
            }
            allStructs.addAll(imported.structs.values());
            allClasses.addAll(imported.classes.values());
        }
        SymbolTable table = new SymbolTable(globalScope);
        for (StructInfo si : allStructs) {
            table.registerStruct(si);
        }
        for (ClassSymbol cls : allClasses) {
            table.registerClass(cls);
        }
        return table;
    }


    private List<String> extractImportPaths(String source, LanguageType language) {
        List<String> paths = new ArrayList<>();
        String[] lines = source.split("\\r?\\n");
        String keyword = switch (language) {
            case PIG_LATIN -> "import";
            case Y_LANG -> null;
            case ZETARIANO -> null;
            default -> null;
        };
        if (keyword == null) return paths;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith(keyword + " ")) {
                String path = trimmed.substring(keyword.length()).trim();
                if (path.endsWith(";")) path = path.substring(0, path.length() - 1).trim();
                if (!path.isBlank()) paths.add(path);
            }
        }
        return paths;
    }

    public static class PipelineResult {
        private boolean valid;
        private String message;
        private ResultLexer lexicalResult;
        private List<SyntaxError> syntacticErrors = Collections.emptyList();
        private List<Symbol> symbols = Collections.emptyList();
        private List<SemanticError> semanticErrors = Collections.emptyList();
        private List<String> printOutput = Collections.emptyList();
        private C3DProgram c3dProgram;
        private C3DVirtualMachine virtualMachine;

        public C3DProgram getC3dProgram() {
            return c3dProgram;
        }

        public void setC3dProgram(C3DProgram c3dProgram) {
            this.c3dProgram = c3dProgram;
        }

        public C3DVirtualMachine getVirtualMachine() {
            return virtualMachine;
        }

        public void setVirtualMachine(C3DVirtualMachine virtualMachine) {
            this.virtualMachine = virtualMachine;
        }

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
