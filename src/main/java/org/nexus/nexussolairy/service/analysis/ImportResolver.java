package org.nexus.nexussolairy.service.analysis;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.nexus.nexussolairy.*;
import org.nexus.nexussolairy.model.enums.LanguageType;
import org.nexus.nexussolairy.model.semantic.Scope;
import org.nexus.nexussolairy.model.semantic.Symbol;
import org.nexus.nexussolairy.service.parser.YIdentationLexer;
import org.nexus.nexussolairy.visitor.pigLatin.PigLatinVisitorImpl;
import org.nexus.nexussolairy.visitor.yLanguage.YVisitorImpl;
import org.nexus.nexussolairy.visitor.zetariano.ZetarianoVisitorImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// encargado para las importaciones externas
public class ImportResolver {

    public List<Symbol> resolveImport(String importPath, String baseDirectory) {

        List<Symbol> importedSymbols = new ArrayList<>();

        String filePath = resolveFilePath(importPath, baseDirectory);

        if (filePath == null) return importedSymbols;

        String source;
        try {
            source = Files.readString(Path.of(filePath));
        } catch (IOException ex) {
            return importedSymbols;
        }

        LanguageType language = LanguageType.fromFileName(filePath);

        if (language == LanguageType.UNKNOWN) return importedSymbols;

        Scope globalScope = analyzeAndGetGlobalScope(source, filePath, language);

        if (globalScope == null) return importedSymbols;

        for (Map.Entry<String, Symbol> entry : globalScope.getSymbols().entrySet()) {
            Symbol sym = entry.getValue();
            if (sym instanceof org.nexus.nexussolairy.model.semantic.ClassSymbol cls) {
                for (Symbol field : cls.getFields().values()) {
                    importedSymbols.add(field);
                }
            } else {
                importedSymbols.add(sym);
            }
        }

        return importedSymbols;
    }

    private String resolveFilePath(String importPath, String baseDirectory) {

        if (importPath == null || importPath.isBlank()) return null;

        // por si el import esta con comillas  (no deberia pero se acepta y se limpia)
        if (importPath.startsWith("\"") && importPath.endsWith("\"")) {
            importPath = importPath.substring(1, importPath.length() - 1);
        }

        // busqueda del archivo
        Path direct = Paths.get(baseDirectory, importPath);

        if (Files.exists(direct)) return direct.toString();

        // no contiene extension es un error, pero se trata de agregar la extesion para resolver
        if (!importPath.contains(".")) {
            for (String ext : new String[]{".z", ".y"}) {
                Path candidate = Paths.get(baseDirectory, importPath + ext);

                if (Files.exists(candidate)) return candidate.toString();

            }
        }

        return null;
    }

    // analisis de cada lenguaje
    private Scope analyzeAndGetGlobalScope(String source, String filePath, LanguageType language) {
        return switch (language) {
            case PIG_LATIN -> analyzePigLatin(source);
            case ZETARIANO -> analyzeZetariano(source, filePath);
            case Y_LANG -> analyzeYLanguage(source);
            default -> null;
        };
    }

    // metodos auxiliares para los lenguajes
    private Scope analyzePigLatin(String source) {
        try {
            PigLatinLexer lexer = new PigLatinLexer(CharStreams.fromString(source));
            lexer.removeErrorListeners();
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            PigLatinParser parser = new PigLatinParser(tokens);
            parser.removeErrorListeners();
            PigLatinParser.ProgramContext tree = parser.program();
            PigLatinVisitorImpl visitor = new PigLatinVisitorImpl();
            visitor.visit(tree);
            return visitor.getSymbolTable().getGlobalScope();
        } catch (Exception e) {
            return null;
        }
    }

    private Scope analyzeYLanguage(String source) {
        try {
            YIdentationLexer lexer = new YIdentationLexer(CharStreams.fromString(source));
            lexer.removeErrorListeners();
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            YParser parser = new YParser(tokens);
            parser.removeErrorListeners();
            YParser.ProgramContext tree = parser.program();
            YVisitorImpl visitor = new YVisitorImpl();
            visitor.visit(tree);
            return visitor.getSymbolTable().getGlobalScope();
        } catch (Exception e) {
            return null;
        }
    }

    private Scope analyzeZetariano(String source, String filePath) {
        try {
            ZetarianoLexer lexer = new ZetarianoLexer(CharStreams.fromString(source));
            lexer.removeErrorListeners();
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            ZetarianoParser parser = new ZetarianoParser(tokens);
            parser.removeErrorListeners();
            ZetarianoParser.ProgramContext tree = parser.program();
            ZetarianoVisitorImpl visitor = new ZetarianoVisitorImpl();
            if (filePath != null) visitor.setFileName(filePath);
            visitor.visit(tree);
            return visitor.getSymbolTable().getGlobalScope();
        } catch (Exception e) {
            return null;
        }
    }
}
