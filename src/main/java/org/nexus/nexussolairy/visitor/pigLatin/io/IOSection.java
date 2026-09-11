package org.nexus.nexussolairy.visitor.pigLatin.io;

import org.nexus.nexussolairy.PigLatinParser;
import org.nexus.nexussolairy.model.enums.DataType;
import org.nexus.nexussolairy.model.enums.TypeErrorSemantic;
import org.nexus.nexussolairy.model.semantic.Symbol;
import org.nexus.nexussolairy.visitor.InputProvider;
import org.nexus.nexussolairy.visitor.VisitorContext;
import org.nexus.nexussolairy.visitor.pigLatin.expression.ExpressionEval;
import org.nexus.nexussolairy.visitor.pigLatin.expression.ExpressionSection;

import java.util.List;
import java.util.function.Consumer;

public class IOSection {
    private final VisitorContext visitor;
    private final ExpressionSection expressionDelegate;
    private final ExpressionEval expressionEval;
    private final List<String> printOutput;
    private final InputProvider inputProvider;
    private final Consumer<String> livePrinter;

    public IOSection(VisitorContext visitor, ExpressionSection expressionDelegate, ExpressionEval expressionEval, List<String> printOutput, InputProvider inputProvider, Consumer<String> livePrinter) {
        this.visitor = visitor;
        this.expressionDelegate = expressionDelegate;
        this.expressionEval = expressionEval;
        this.printOutput = printOutput;
        this.inputProvider = inputProvider;
        this.livePrinter = livePrinter;
    }

    public DataType visitPrintStmt(PigLatinParser.PrintStmtContext ctx) {
        if (ctx == null) return DataType.VOID;
        StringBuilder sb = new StringBuilder();
        for (PigLatinParser.ExpressionContext expr : ctx.expression()) {
            visitor.visit(expr);
            Object val = expressionEval.evalExpression(expr);
            if (val != null) {
                sb.append(expressionEval.toDisplayString(val));
            }
        }
        String output = sb.toString();
        if (!output.isEmpty()) {
            printOutput.add(output);
            if (livePrinter != null) {
                livePrinter.accept(output);
            }
        }
        return DataType.VOID;
    }

    public DataType visitReadStmt(PigLatinParser.ReadStmtContext ctx) {
        if (ctx == null) return DataType.VOID;
        if (ctx.ID() != null) {
            String name = ctx.ID().getText();
            Symbol s = visitor.getSymbolTable().lookup(name);
            if (s == null) {
                visitor.reportError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), TypeErrorSemantic.UNDECLARED, "Variable '" + name + "' no declarada.");
            } else if (inputProvider != null) {
                String input = inputProvider.readLine();
                visitor.getSymbolTable().updateValue(name, input);
            }
        } else if (inputProvider != null) {
            inputProvider.readLine();
        }
        return DataType.VOID;
    }
}
