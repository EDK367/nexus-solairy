package org.nexus.nexussolairy.visitor.pigLatin.error;

import org.nexus.nexussolairy.visitor.InputProvider;
import org.nexus.nexussolairy.visitor.pigLatin.PigLatinVisitorImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PigLatinSemanticChecker extends PigLatinVisitorImpl {

    public PigLatinSemanticChecker(InputProvider inputProvider, Consumer<String> livePrinter) {
        super(inputProvider, livePrinter);
    }

    public PigLatinSemanticChecker(InputProvider inputProvider) {
        super(inputProvider);
    }

    public PigLatinSemanticChecker() {
        super();
    }

    public List<String> getErrorMessages() {
        List<String> list = new ArrayList<>();
        for (var err : getErrors()) {
            list.add("Linea " + err.getLine() + ": " + err.getMessage());
        }
        return list;
    }
}
