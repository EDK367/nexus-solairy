package org.nexus.nexussolairy.patron;

import org.nexus.nexussolairy.model.enums.LanguageType;
import org.nexus.nexussolairy.service.grammar.LexerService;
import org.nexus.nexussolairy.service.grammar.PigLatinLexerService;

public class LexerFactory {

    public static LexerService create(LanguageType language) {
        return switch (language) {
            case PIG_LATIN -> new PigLatinLexerService();
            default -> throw new IllegalArgumentException("Unsupported language type: " + language);
        };
    }
}
