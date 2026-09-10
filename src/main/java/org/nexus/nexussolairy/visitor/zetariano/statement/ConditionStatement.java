package org.nexus.nexussolairy.visitor.zetariano.statement;

import org.nexus.nexussolairy.ZetarianoParser;
import org.nexus.nexussolairy.model.enums.DataType;
import org.nexus.nexussolairy.model.enums.TypeErrorSemantic;
import org.nexus.nexussolairy.model.semantic.Type;
import org.nexus.nexussolairy.model.semantic.TypeChecker;
import org.nexus.nexussolairy.visitor.zetariano.ZetarianoVisitorImpl;
import org.nexus.nexussolairy.visitor.zetariano.expression.ExpressionEval;

public class ConditionStatement {
    private final ZetarianoVisitorImpl visitor;
    private final ExpressionEval expressionEval;

    public ConditionStatement(ZetarianoVisitorImpl visitor, ExpressionEval expressionEval) {
        this.visitor = visitor;
        this.expressionEval = expressionEval;
    }

    public DataType visitIfStmt(ZetarianoParser.IfStmtContext ctx) {
        if (ctx == null) return DataType.VOID;
        if (ctx.expression() != null) {
            for (ZetarianoParser.ExpressionContext e : ctx.expression()) {
                Type c = visitor.getExpressionDelegate().visitExpression(e);
                if (!TypeChecker.isBool(c) && c != Type.ERROR) {
                    visitor.reportError(e, TypeErrorSemantic.NOT_BOOLEAN, "Condicion debe ser booleana");
                }
            }
        }
        if (ctx.ifBody() != null) {
            for (ZetarianoParser.IfBodyContext b : ctx.ifBody()) {
                visitIfBody(b);
            }
        }
        return DataType.VOID;
    }

    public DataType visitIfBody(ZetarianoParser.IfBodyContext ctx) {
        if (ctx == null) return DataType.VOID;
        if (ctx.block() != null) return visitor.visitBlock(ctx.block());
        if (ctx.statement() != null) return visitor.visitStatement(ctx.statement());
        return DataType.VOID;
    }

    public DataType visitSwitchStmt(ZetarianoParser.SwitchStmtContext ctx) {
        if (ctx == null) return DataType.VOID;
        Type sel = visitor.getExpressionDelegate().visitExpression(ctx.expression());
        boolean prevSwitch = visitor.isInsideSwitch();
        visitor.setInsideSwitch(true);
        if (ctx.caseBranch() != null) {
            for (ZetarianoParser.CaseBranchContext c : ctx.caseBranch()) {
                visitCaseBranch(c, sel);
            }
        }
        if (ctx.defaultBranch() != null) {
            visitDefaultBranch(ctx.defaultBranch());
        }
        visitor.setInsideSwitch(prevSwitch);
        return DataType.VOID;
    }

    public DataType visitCaseBranch(ZetarianoParser.CaseBranchContext ctx, Type sel) {
        if (ctx == null) return DataType.VOID;
        Type ct = visitor.getExpressionDelegate().visitExpression(ctx.expression());
        if (!ct.equals(sel) && !TypeChecker.isAssignable(sel, ct) && ct != Type.ERROR && sel != Type.ERROR) {
            visitor.reportError(ctx, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Caso incompatible con selector");
        }
        if (ctx.statement() != null) {
            for (ZetarianoParser.StatementContext st : ctx.statement()) {
                visitor.visitStatement(st);
            }
        }
        return DataType.VOID;
    }

    public DataType visitCaseBranch(ZetarianoParser.CaseBranchContext ctx) {
        return visitCaseBranch(ctx, Type.ERROR);
    }

    public DataType visitDefaultBranch(ZetarianoParser.DefaultBranchContext ctx) {
        if (ctx == null) return DataType.VOID;
        if (ctx.statement() != null) {
            for (ZetarianoParser.StatementContext st : ctx.statement()) {
                visitor.visitStatement(st);
            }
        }
        return DataType.VOID;
    }
}
