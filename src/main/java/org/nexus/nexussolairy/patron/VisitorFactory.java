package org.nexus.nexussolairy.patron;

import org.nexus.nexussolairy.model.enums.LanguageType;
import org.nexus.nexussolairy.visitor.InputProvider;
import org.nexus.nexussolairy.visitor.VisitorContext;
import org.nexus.nexussolairy.visitor.pigLatin.PigLatinVisitorImpl;

import java.util.function.Consumer;

public class VisitorFactory {

    public static VisitorContext create(LanguageType language) {
        return create(language, () -> "", null);
    }

    public static VisitorContext create(LanguageType language, InputProvider inputProvider, Consumer<String> livePrinter) {
        if (language == null) return null;
        return switch (language) {
            case PIG_LATIN -> new PigLatinVisitorImpl(inputProvider, livePrinter);
            default -> null;
        };
    }
}
