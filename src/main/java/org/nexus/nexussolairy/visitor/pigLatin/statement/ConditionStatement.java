package org.nexus.nexussolairy.visitor.pigLatin.statement;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.nexus.nexussolairy.PigLatinParser;
import org.nexus.nexussolairy.model.enums.DataType;
import org.nexus.nexussolairy.model.enums.TypeErrorSemantic;
import org.nexus.nexussolairy.visitor.VisitorContext;
import org.nexus.nexussolairy.visitor.pigLatin.expression.ExpressionEval;

import java.util.ArrayList;
import java.util.List;

public class ConditionStatement {
    private final VisitorContext visitor;
    private final ExpressionEval expressionEval;

    public ConditionStatement(VisitorContext visitor, ExpressionEval expressionEval) {
        this.visitor = visitor;
        this.expressionEval = expressionEval;
    }

    public DataType visitIfStmt(PigLatinParser.IfStmtContext ctx) {
        if (ctx == null) return DataType.VOID;

        for (int i = 0; i < ctx.expression().size(); i++) {
            PigLatinParser.ExpressionContext condExpr = ctx.expression(i);
            DataType condType = visitor.visit(condExpr);
            if (condType != DataType.BOOLEAN && condType != DataType.ERROR) {
                visitor.reportError(condExpr.getStart().getLine(), condExpr.getStart().getCharPositionInLine(), TypeErrorSemantic.NOT_BOOLEAN, "Condicion del 'si' debe ser booleana.");
            }
        }

        List<List<PigLatinParser.StatementContext>> branchStatements = new ArrayList<>();
        List<PigLatinParser.StatementContext> currentBlock = null;

        if (ctx.children != null) {
            for (ParseTree child : ctx.children) {
                if (child instanceof TerminalNode tn) {
                    if (tn.getSymbol().getType() == PigLatinParser.LBRACE) {
                        currentBlock = new ArrayList<>();
                        branchStatements.add(currentBlock);
                    }
                } else if (child instanceof PigLatinParser.StatementContext stmt) {
                    if (currentBlock != null) {
                        currentBlock.add(stmt);
                    }
                }
            }
        }

        boolean branchExecuted = false;
        for (int i = 0; i < branchStatements.size(); i++) {
            boolean shouldExecute = false;
            if (i < ctx.expression().size()) {
                PigLatinParser.ExpressionContext condExpr = ctx.expression(i);
                Object val = expressionEval.evalExpression(condExpr);
                if (Boolean.TRUE.equals(val)) {
                    shouldExecute = true;
                }
            } else {
                shouldExecute = true;
            }

            if (shouldExecute && !branchExecuted) {
                branchExecuted = true;
                visitor.getSymbolTable().pushScope("if");
                for (PigLatinParser.StatementContext s : branchStatements.get(i)) {
                    visitor.visit(s);
                    if (visitor.isShouldBreak() || visitor.isShouldContinue() || visitor.isShouldReturn()) {
                        break;
                    }
                }
                visitor.getSymbolTable().popScope();
            }
        }

        return DataType.VOID;
    }
}
