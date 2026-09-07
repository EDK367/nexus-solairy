package org.nexus.nexussolairy.service.parser;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.nexus.nexussolairy.ZetarianoLexer;
import org.nexus.nexussolairy.ZetarianoParser;
import org.nexus.nexussolairy.model.syntactic.SyntaxError;

import java.util.ArrayList;
import java.util.List;

public class ZetarianoParserService implements ParserService {

    List<SyntaxError> errors = new ArrayList<>();

    @Override
    public List<SyntaxError> analyze(String source) {
        ZetarianoLexer lexer = new ZetarianoLexer(CharStreams.fromString(source));
        return analyze(lexer);
    }

    @Override
    public List<SyntaxError> analyze(Lexer lexer) {
        errors.clear();

        lexer.removeErrorListeners();

        CommonTokenStream tokens = new CommonTokenStream(lexer);

        ZetarianoParser parser = new ZetarianoParser(tokens);
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
