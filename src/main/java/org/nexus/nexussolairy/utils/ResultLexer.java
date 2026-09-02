package org.nexus.nexussolairy.utils;

import org.nexus.nexussolairy.model.lexical.LexerError;
import org.nexus.nexussolairy.model.lexical.TokenInfo;

import java.util.ArrayList;
import java.util.List;

public class ResultLexer {

    public final List<TokenInfo> tokens = new ArrayList<>();
    public final List<LexerError> errors = new ArrayList<>();

    public boolean isValid() {
        return errors.isEmpty();
    }
}
