package org.nexus.nexussolairy.visitor.zetariano.error;

import org.nexus.nexussolairy.model.semantic.SemanticError;
import org.nexus.nexussolairy.visitor.InputProvider;
import org.nexus.nexussolairy.visitor.zetariano.ZetarianoVisitorImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ZetarianoSemanticChecker extends ZetarianoVisitorImpl {

    public ZetarianoSemanticChecker(InputProvider inputProvider, Consumer<String> livePrinter) {
        super(inputProvider, livePrinter);
    }

    public ZetarianoSemanticChecker(InputProvider inputProvider) {
        super(inputProvider);
    }

    public ZetarianoSemanticChecker() {
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
