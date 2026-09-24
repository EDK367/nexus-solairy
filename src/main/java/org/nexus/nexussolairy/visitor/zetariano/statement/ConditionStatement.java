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

        if (!visitor.isInsideMain()) {
            if (ctx.ifBody() != null) {
                for (ZetarianoParser.IfBodyContext b : ctx.ifBody()) {
                    visitor.pushScope("if");
                    visitIfBody(b);
                    visitor.popScope();
                }
            }
            return DataType.VOID;
        }

        boolean executed = false;
        if (ctx.expression() != null && !ctx.expression().isEmpty()) {
            Object cond = expressionEval.evalExpression(ctx.expression(0));
            if (Boolean.TRUE.equals(cond)) {
                if (!ctx.ifBody().isEmpty() && ctx.ifBody(0) != null) {
                    visitor.pushScope("if");
                    visitIfBody(ctx.ifBody(0));
                    visitor.popScope();
                }
                executed = true;
            } else {
                for (int i = 1; i < ctx.expression().size(); i++) {
                    Object elifCond = expressionEval.evalExpression(ctx.expression(i));
                    if (Boolean.TRUE.equals(elifCond)) {
                        if (i < ctx.ifBody().size() && ctx.ifBody(i) != null) {
                            visitor.pushScope("elif");
                            visitIfBody(ctx.ifBody(i));
                            visitor.popScope();
                        }
                        executed = true;
                        break;
                    }
                }
            }
        }

        if (!executed && ctx.ifBody().size() > (ctx.expression() != null ? ctx.expression().size() : 0)) {
            ZetarianoParser.IfBodyContext elseBody = ctx.ifBody(ctx.ifBody().size() - 1);
            if (elseBody != null) {
                visitor.pushScope("else");
                visitIfBody(elseBody);
                visitor.popScope();
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
                Type ct = visitor.getExpressionDelegate().visitExpression(c.expression());
                if (!ct.equals(sel) && !TypeChecker.isAssignable(sel, ct) && ct != Type.ERROR && sel != Type.ERROR) {
                    visitor.reportError(c, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Caso incompatible con selector");
                }
            }
        }

        if (!visitor.isInsideMain()) {
            if (ctx.caseBranch() != null) {
                for (ZetarianoParser.CaseBranchContext c : ctx.caseBranch()) {
                    visitor.pushScope("case");
                    if (c.statement() != null) {
                        for (ZetarianoParser.StatementContext st : c.statement()) {
                            visitor.visitStatement(st);
                        }
                    }
                    visitor.popScope();
                }
            }
            if (ctx.defaultBranch() != null && ctx.defaultBranch().statement() != null) {
                visitor.pushScope("default");
                for (ZetarianoParser.StatementContext st : ctx.defaultBranch().statement()) {
                    visitor.visitStatement(st);
                }
                visitor.popScope();
            }
            visitor.setInsideSwitch(prevSwitch);
            return DataType.VOID;
        }

        Object selVal = expressionEval.evalExpression(ctx.expression());
        boolean matched = false;
        if (ctx.caseBranch() != null) {
            for (ZetarianoParser.CaseBranchContext c : ctx.caseBranch()) {
                Object caseVal = expressionEval.evalExpression(c.expression());
                boolean matches = (selVal != null && caseVal != null &&
                    (selVal.equals(caseVal) ||
                     (selVal instanceof Number sn && caseVal instanceof Number cn && sn.doubleValue() == cn.doubleValue())));
                if (matches || matched) {
                    matched = true;
                    visitor.pushScope("case");
                    if (c.statement() != null) {
                        for (ZetarianoParser.StatementContext st : c.statement()) {
                            visitor.visitStatement(st);
                            if (visitor.isShouldBreak() || visitor.isShouldReturn()) break;
                        }
                    }
                    visitor.popScope();
                    if (visitor.isShouldBreak()) {
                        visitor.setShouldBreak(false);
                        break;
                    }
                    if (visitor.isShouldReturn()) break;
                }
            }
        }

        if (!matched && ctx.defaultBranch() != null && ctx.defaultBranch().statement() != null) {
            visitor.pushScope("default");
            for (ZetarianoParser.StatementContext st : ctx.defaultBranch().statement()) {
                visitor.visitStatement(st);
                if (visitor.isShouldBreak() || visitor.isShouldReturn()) break;
            }
            visitor.popScope();
            if (visitor.isShouldBreak()) {
                visitor.setShouldBreak(false);
            }
        }

        visitor.setInsideSwitch(prevSwitch);
        return DataType.VOID;
    }

    public DataType visitCaseBranch(ZetarianoParser.CaseBranchContext ctx) {
        if (ctx == null) return DataType.VOID;
        Type t = visitor.getExpressionDelegate().visitExpression(ctx.expression());
        return t != null ? t.getDataType() : DataType.VOID;
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
