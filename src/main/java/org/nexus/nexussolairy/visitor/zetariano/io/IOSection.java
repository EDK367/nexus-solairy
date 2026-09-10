package org.nexus.nexussolairy.visitor.zetariano.io;

import org.nexus.nexussolairy.ZetarianoParser;
import org.nexus.nexussolairy.model.enums.DataType;
import org.nexus.nexussolairy.visitor.InputProvider;
import org.nexus.nexussolairy.visitor.zetariano.ZetarianoVisitorImpl;
import org.nexus.nexussolairy.visitor.zetariano.expression.ExpressionEval;
import org.nexus.nexussolairy.visitor.zetariano.expression.ExpressionSection;

import java.util.List;
import java.util.function.Consumer;

public class IOSection {
    private final ZetarianoVisitorImpl visitor;
    private final ExpressionSection expressionDelegate;
    private final ExpressionEval expressionEval;
    private final List<String> printOutput;
    private final InputProvider inputProvider;
    private final Consumer<String> livePrinter;

    public IOSection(ZetarianoVisitorImpl visitor, ExpressionSection expressionDelegate, ExpressionEval expressionEval,
                     List<String> printOutput, InputProvider inputProvider, Consumer<String> livePrinter) {
        this.visitor = visitor;
        this.expressionDelegate = expressionDelegate;
        this.expressionEval = expressionEval;
        this.printOutput = printOutput;
        this.inputProvider = inputProvider;
        this.livePrinter = livePrinter;
    }

    public DataType visitPrintStmt(ZetarianoParser.PrintStmtContext ctx) {
        if (ctx == null) return DataType.VOID;
        if (ctx.expression() != null) {
            expressionDelegate.visitExpression(ctx.expression());
            String text = expressionEval.evaluateExprText(ctx.expression());
            printOutput.add(text);
            if (livePrinter != null) {
                if (ctx.PRINTLN() != null) {
                    livePrinter.accept(text + "\n");
                } else {
                    livePrinter.accept(text);
                }
            }
        }
        return DataType.VOID;
    }

    public DataType visitReadStmt(ZetarianoParser.ReadStmtContext ctx) {
        if (ctx == null) return DataType.CADENA;
        if (inputProvider != null) {
            inputProvider.readLine();
        }
        return DataType.CADENA;
    }
}
