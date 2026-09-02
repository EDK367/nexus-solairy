package org.nexus.nexussolairy.service.grammar;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.nexus.nexussolairy.model.lexical.LexerError;

import java.util.ArrayList;
import java.util.List;

public class LexicalErrorListener extends BaseErrorListener {

    private final List<LexerError> errors = new ArrayList<>();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
                            Object offendingSymbol,
                            int line,
                            int charPositionInLine,
                            String msg,
                            RecognitionException e) {
        errors.add(new LexerError(line, charPositionInLine + 1, msg));
    }

    public List<LexerError> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
