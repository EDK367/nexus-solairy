package org.nexus.nexussolairy.visitor;

import org.antlr.v4.runtime.tree.ParseTree;
import org.nexus.nexussolairy.model.enums.DataType;
import org.nexus.nexussolairy.model.enums.TypeErrorSemantic;
import org.nexus.nexussolairy.model.semantic.SymbolTable;

import java.util.List;

public interface VisitorContext {
    DataType visit(ParseTree tree);
    SymbolTable getSymbolTable();
    boolean isInsideLoop();
    void setInsideLoop(boolean value);
    DataType getCurrentFunctionReturnType();
    void setCurrentFunctionReturnType(DataType type);

    boolean isShouldBreak();
    void setShouldBreak(boolean value);

    boolean isShouldContinue();
    void setShouldContinue(boolean value);

    boolean isShouldReturn();
    void setShouldReturn(boolean value);

    Object getReturnValue();
    void setReturnValue(Object value);

    Object executeFunctionCall(String name, List<Object> arguments);

    default Object executeMethodCall(String varName, String methodName, List<Object> arguments) {
        return null;
    }

    void reportError(int line, int column, TypeErrorSemantic type, String message);

    DataType resolveType(String typeText);

    boolean isInsideMain();
    void setInsideMain(boolean value);

    boolean isInsideFunction();
    void setInsideFunction(boolean value);
}
