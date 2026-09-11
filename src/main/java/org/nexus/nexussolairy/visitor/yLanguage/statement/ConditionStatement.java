package org.nexus.nexussolairy.visitor.yLanguage.statement;

import org.nexus.nexussolairy.YParser;
import org.nexus.nexussolairy.model.enums.DataType;
import org.nexus.nexussolairy.model.enums.TypeErrorSemantic;
import org.nexus.nexussolairy.model.semantic.TypeChecker;
import org.nexus.nexussolairy.visitor.VisitorContext;
import org.nexus.nexussolairy.visitor.yLanguage.YVisitorImpl;
import org.nexus.nexussolairy.visitor.yLanguage.expression.ExpressionEval;

public class ConditionStatement {
    private final VisitorContext visitor;
    private final ExpressionEval expressionEval;

    public ConditionStatement(VisitorContext visitor, ExpressionEval expressionEval) {
        this.visitor = visitor;
        this.expressionEval = expressionEval;
    }

    public DataType visitIfStmt(YParser.IfStmtContext ctx) {
        if (ctx == null) return DataType.VOID;

        for (YParser.ExpressionContext e : ctx.expression()) {
            DataType c = visitor.visit(e);
            if (!TypeChecker.isBool(c) && c != DataType.ERROR) {
                visitor.reportError(e.getStart().getLine(), e.getStart().getCharPositionInLine(), TypeErrorSemantic.NOT_BOOLEAN, "Condicion del 'si' debe ser booleana");
            }
        }

        if (visitor.isInsideMain()) {
            Object cond = expressionEval.evalExpression(ctx.expression(0));
            boolean executed = false;
            if (Boolean.TRUE.equals(cond)) {
                if (ctx.block(0) != null) {
                    visitor.getSymbolTable().pushScope("if");
                    visitor.visit(ctx.block(0));
                    visitor.getSymbolTable().popScope();
                }
                executed = true;
            } else {
                int exprCount = ctx.expression().size();
                for (int i = 1; i < exprCount; i++) {
                    Object elifCond = expressionEval.evalExpression(ctx.expression(i));
                    if (Boolean.TRUE.equals(elifCond)) {
                        if (i < ctx.block().size() && ctx.block(i) != null) {
                            visitor.getSymbolTable().pushScope("elif");
                            visitor.visit(ctx.block(i));
                            visitor.getSymbolTable().popScope();
                        }
                        executed = true;
                        break;
                    }
                }
            }
            if (!executed && ctx.block().size() > ctx.expression().size()) {
                YParser.BlockContext elseBlock = ctx.block(ctx.block().size() - 1);
                if (elseBlock != null) {
                    visitor.getSymbolTable().pushScope("else");
                    visitor.visit(elseBlock);
                    visitor.getSymbolTable().popScope();
                }
            }
        } else {
            for (YParser.BlockContext b : ctx.block()) {
                visitor.getSymbolTable().pushScope("if");
                visitor.visit(b);
                visitor.getSymbolTable().popScope();
            }
        }

        return DataType.VOID;
    }

    public DataType visitSwitchStmt(YParser.SwitchStmtContext ctx) {
        if (ctx == null) return DataType.VOID;

        DataType sel = visitor.visit(ctx.expression());

        YVisitorImpl yVisitor = (YVisitorImpl) visitor;
        boolean prevSwitch = yVisitor.isInsideSwitch();
        yVisitor.setInsideSwitch(true);

        for (YParser.CaseBranchContext c : ctx.caseBranch()) {
            DataType ct = visitor.visit(c.expression());
            if (!ct.equals(sel) && ct != DataType.ERROR && !TypeChecker.isAssignable(sel, ct)) {
                visitor.reportError(c.getStart().getLine(), c.getStart().getCharPositionInLine(), TypeErrorSemantic.INCOMPATIBLE_TYPES, "Tipo de caso incompatible con el selector");
            }
            visitor.getSymbolTable().pushScope("case");
            visitor.visit(c.block());
            visitor.getSymbolTable().popScope();
        }

        if (ctx.defaultBranch() != null) {
            visitor.getSymbolTable().pushScope("default");
            visitor.visit(ctx.defaultBranch().block());
            visitor.getSymbolTable().popScope();
        }

        yVisitor.setInsideSwitch(prevSwitch);
        return DataType.VOID;
    }

    public DataType visitCaseBranch(YParser.CaseBranchContext ctx) {
        if (ctx == null) return DataType.VOID;
        return visitor.visit(ctx.expression());
    }

    public DataType visitDefaultBranch(YParser.DefaultBranchContext ctx) {
        if (ctx == null) return DataType.VOID;
        return visitor.visit(ctx.block());
    }
}
