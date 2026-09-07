package org.nexus.nexussolairy.patron;

import org.nexus.nexussolairy.model.enums.LanguageType;
import org.nexus.nexussolairy.service.parser.ParserService;
import org.nexus.nexussolairy.service.parser.PigLatinParserService;
import org.nexus.nexussolairy.service.parser.YParserService;
import org.nexus.nexussolairy.service.parser.ZetarianoParserService;

public class ParserFactory {

    public static ParserService create(LanguageType language) {
        return switch (language) {
            case PIG_LATIN -> new PigLatinParserService();
            case Y_LANG -> new YParserService();
            case ZETARIANO -> new ZetarianoParserService();
            default -> throw new IllegalArgumentException("Unsupported language type: " + language);
        };
    }
}
