package org.nexus.nexussolairy.service.analysis;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.nexus.nexussolairy.*;
import org.nexus.nexussolairy.model.enums.LanguageType;
import org.nexus.nexussolairy.model.semantic.ClassSymbol;
import org.nexus.nexussolairy.model.semantic.SemanticError;
import org.nexus.nexussolairy.model.semantic.StructInfo;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// encargado para las importaciones externas
public class ImportResolver {

    public static class ImportResult {
        public final List<Symbol> symbols;
        public final Map<String, StructInfo> structs;   // de .y
        public final Map<String, ClassSymbol> classes;  // de .z
        public final List<SemanticError> errors;

        public ImportResult(List<Symbol> symbols, Map<String, StructInfo> structs, Map<String, ClassSymbol> classes) {
            this(symbols, structs, classes, List.of());
        }

        public ImportResult(List<Symbol> symbols, Map<String, StructInfo> structs, Map<String, ClassSymbol> classes, List<SemanticError> errors) {
            this.symbols = symbols;
            this.structs = structs;
            this.classes = classes;
            this.errors = errors != null ? errors : List.of();
        }

        public boolean isEmpty() {
            return symbols.isEmpty() && structs.isEmpty() && classes.isEmpty();
        }
    }

    public ImportResult resolveImport(String importPath, String baseDirectory) {
        String filePath = resolveFilePath(importPath, baseDirectory);
        if (filePath == null) return new ImportResult(List.of(), Map.of(), Map.of());
        String source;
        try {
            source = Files.readString(Path.of(filePath));
        } catch (IOException ex) {
            return new ImportResult(List.of(), Map.of(), Map.of());
        }
        LanguageType language = LanguageType.fromFileName(filePath);
        if (language == LanguageType.UNKNOWN) return new ImportResult(List.of(), Map.of(), Map.of());
        return switch (language) {
            case PIG_LATIN -> importFromPig(source);
            case ZETARIANO -> importFromZetariano(source, filePath);
            case Y_LANG -> importFromY(source);
            default -> new ImportResult(List.of(), Map.of(), Map.of());
        };
    }

    private ImportResult importFromY(String source) {
        try {
            YIdentationLexer lexer = new YIdentationLexer(CharStreams.fromString(source));
            lexer.removeErrorListeners();
            YParser parser = new YParser(new CommonTokenStream(lexer));
            parser.removeErrorListeners();
            YVisitorImpl visitor = new YVisitorImpl();
            visitor.visit(parser.program());
            List<Symbol> symbols = new ArrayList<>(visitor.getSymbolTable().getGlobalScope().getSymbols().values());
            Map<String, StructInfo> structs = new HashMap<>(visitor.getSymbolTable().getStructRegistry());

            return new ImportResult(symbols, structs, Map.of(), visitor.getErrors());
        } catch (Exception e) {
            return new ImportResult(List.of(), Map.of(), Map.of());
        }
    }

    private ImportResult importFromZetariano(String source, String filePath) {
        try {
            ZetarianoLexer lexer = new ZetarianoLexer(CharStreams.fromString(source));
            lexer.removeErrorListeners();
            ZetarianoParser parser = new ZetarianoParser(new CommonTokenStream(lexer));
            parser.removeErrorListeners();
            ZetarianoVisitorImpl visitor = new ZetarianoVisitorImpl();
            if (filePath != null) visitor.setFileName(filePath);
            visitor.visit(parser.program());
            List<Symbol> symbols = new ArrayList<>(visitor.getSymbolTable().getGlobalScope().getSymbols().values());
            Map<String, ClassSymbol> classes = new HashMap<>(visitor.getSymbolTable().getClassRegistry());

            return new ImportResult(symbols, Map.of(), classes, visitor.getErrors());
        } catch (Exception e) {
            return new ImportResult(List.of(), Map.of(), Map.of());
        }
    }

    private ImportResult importFromPig(String source) {
        try {
            PigLatinLexer lexer = new PigLatinLexer(CharStreams.fromString(source));
            lexer.removeErrorListeners();
            PigLatinParser parser = new PigLatinParser(new CommonTokenStream(lexer));
            parser.removeErrorListeners();
            PigLatinVisitorImpl visitor = new PigLatinVisitorImpl();
            visitor.visit(parser.program());
            List<Symbol> symbols = new ArrayList<>(visitor.getSymbolTable().getGlobalScope().getSymbols().values());
            return new ImportResult(symbols, Map.of(), Map.of(), visitor.getErrors());
        } catch (Exception e) {
            return new ImportResult(List.of(), Map.of(), Map.of());
        }
    }

    public String resolveFilePath(String importPath, String baseDirectory) {

        if (importPath == null || importPath.isBlank()) return null;

        // por si el import esta con comillas  (no deberia pero se acepta y se limpia)
        if (importPath.startsWith("\"") && importPath.endsWith("\"")) {
            importPath = importPath.substring(1, importPath.length() - 1);
        }

        // conversion de archivos estilo carpeta/carpeta/file.extension
        String convertedPath = convertDotPathToFilePath(importPath);

        // busqueda del archivo
        Path direct = Paths.get(baseDirectory, convertedPath);

        if (Files.exists(direct)) return direct.toString();

        // si el path ya iba bien
        Path fallback = Paths.get(baseDirectory, importPath);
        if (Files.exists(fallback)) return fallback.toString();
        return null;
    }

    /* uso de subdirectorios
       solo toma (id . id ) esto significa que file extesion y el resto de puntos
       de derecha para izquierda se toma como carpetas
     */
    private String convertDotPathToFilePath(String path) {
        String[] extensions = {".pig", ".z", ".y"};

        for (String ext : extensions) {
            if (path.contains(ext)) {
                // quitar la extension y convertir en carpetas
                String withoutExt = path.substring(0, path.length() - ext.length());
                String withSlashes = withoutExt.replace(".", "/");
                return withSlashes + ext;
            }
        }

        // si no hay extesion reconocida
        return path.replace(".", "/");
    }
}
