package org.nexus.nexussolairy.visitor.yLanguage.error;

import org.nexus.nexussolairy.model.semantic.SemanticError;
import org.nexus.nexussolairy.visitor.InputProvider;
import org.nexus.nexussolairy.visitor.yLanguage.YVisitorImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class YSemanticChecker extends YVisitorImpl {

    public YSemanticChecker(InputProvider inputProvider, Consumer<String> livePrinter) {
        super(inputProvider, livePrinter);
    }

    public YSemanticChecker(InputProvider inputProvider) {
        super(inputProvider);
    }

    public YSemanticChecker() {
        super();
    }

    @Override
    public boolean hasErrors() {
        return super.hasErrors();
    }

    @Override
    public List<SemanticError> getErrors() {
        return super.getErrors();
    }

    @Override
    public List<String> getErrorMessages() {
        List<String> list = new ArrayList<>();
        for (SemanticError err : getErrors()) {
            list.add("Linea " + err.getLine() + ": " + err.getMessage());
        }
        return list;
    }
}
