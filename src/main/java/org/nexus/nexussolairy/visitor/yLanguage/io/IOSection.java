package org.nexus.nexussolairy.visitor.yLanguage.io;

import org.nexus.nexussolairy.YParser;
import org.nexus.nexussolairy.model.enums.DataType;
import org.nexus.nexussolairy.visitor.InputProvider;
import org.nexus.nexussolairy.visitor.VisitorContext;
import org.nexus.nexussolairy.visitor.yLanguage.expression.ExpressionEval;
import org.nexus.nexussolairy.visitor.yLanguage.expression.ExpressionSection;

import java.util.List;
import java.util.function.Consumer;

public class IOSection {
    private final VisitorContext visitor;
    private final ExpressionSection expressionDelegate;
    private final ExpressionEval expressionEval;
    private final List<String> printOutput;
    private final InputProvider inputProvider;
    private final Consumer<String> livePrinter;

    public IOSection(VisitorContext visitor, ExpressionSection expressionDelegate, ExpressionEval expressionEval,
                     List<String> printOutput, InputProvider inputProvider, Consumer<String> livePrinter) {
        this.visitor = visitor;
        this.expressionDelegate = expressionDelegate;
        this.expressionEval = expressionEval;
        this.printOutput = printOutput;
        this.inputProvider = inputProvider;
        this.livePrinter = livePrinter;
    }

    public DataType visitPrintStmt(YParser.PrintStmtContext ctx) {
        if (ctx == null) return DataType.VOID;
        if (ctx.expression() != null) {
            visitor.visit(ctx.expression());
            if (visitor.isInsideMain()) {
                Object val = expressionEval.evalExpression(ctx.expression());
                String output = expressionEval.toDisplayString(val);
                printOutput.add(output);
                if (livePrinter != null) {
                    livePrinter.accept(output);
                }
            }
        }
        return DataType.VOID;
    }

    public DataType visitReadStmt(YParser.ReadStmtContext ctx) {
        if (ctx == null) return DataType.CADENA;
        if (inputProvider != null) {
            inputProvider.readLine();
        }
        return DataType.CADENA;
    }
}
