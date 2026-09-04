package org.nexus.nexussolairy.service.parser;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.nexus.nexussolairy.YParser;
import org.nexus.nexussolairy.model.syntactic.SyntaxError;

import java.util.ArrayList;
import java.util.List;

public class YParserService implements ParserService {

    private final List<SyntaxError> errors = new ArrayList<>();

    @Override
    public List<SyntaxError> analyze(String source) {
        YIdentationLexer lexer = new YIdentationLexer(CharStreams.fromString(source));
        return analyze(lexer);
    }

    @Override
    public List<SyntaxError> analyze(Lexer lexer) {
        errors.clear();

        lexer.removeErrorListeners();

        CommonTokenStream tokens = new CommonTokenStream(lexer);

        YParser parser = new YParser(tokens);
        SyntaxErrorListener errorListener = new SyntaxErrorListener();

        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        parser.program();

        errors.addAll(errorListener.getErrors());

        return errors;

    }

    @Override
    public List<SyntaxError> getErrors() {
        return errors;
    }
}
