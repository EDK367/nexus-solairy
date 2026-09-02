package org.nexus.nexussolairy.patron;

import org.nexus.nexussolairy.model.enums.LanguageType;
import org.nexus.nexussolairy.service.grammar.LexerService;
import org.nexus.nexussolairy.service.grammar.PigLatinLexerService;
import org.nexus.nexussolairy.service.grammar.YLexerService;

public class LexerFactory {

    public static LexerService create(LanguageType language) {
        return switch (language) {
            case PIG_LATIN -> new PigLatinLexerService();
            case Y_LANG -> new YLexerService();
            default -> throw new IllegalArgumentException("Unsupported language type: " + language);
        };
    }
}
