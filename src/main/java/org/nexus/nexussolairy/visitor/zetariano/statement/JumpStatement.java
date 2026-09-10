package org.nexus.nexussolairy.visitor.zetariano.statement;

import org.nexus.nexussolairy.ZetarianoParser;
import org.nexus.nexussolairy.model.enums.DataType;
import org.nexus.nexussolairy.model.enums.TypeErrorSemantic;
import org.nexus.nexussolairy.model.semantic.FunctionSymbol;
import org.nexus.nexussolairy.model.semantic.Type;
import org.nexus.nexussolairy.model.semantic.TypeChecker;
import org.nexus.nexussolairy.visitor.zetariano.ZetarianoVisitorImpl;

public class JumpStatement {
    private final ZetarianoVisitorImpl visitor;

    public JumpStatement(ZetarianoVisitorImpl visitor) {
        this.visitor = visitor;
    }

    public DataType visitReturnStmt(ZetarianoParser.ReturnStmtContext ctx) {
        if (ctx == null) return DataType.VOID;
        FunctionSymbol currentMethod = visitor.getCurrentMethod();
        if (currentMethod == null) {
            visitor.reportError(ctx, TypeErrorSemantic.INVALID_JUMP, "return fuera de metodo");
            return DataType.VOID;
        }
        if (visitor.getCurrentClass() != null && currentMethod.getName().equals(visitor.getCurrentClass().getName())) {
            if (ctx.expression() != null) {
                visitor.reportError(ctx, TypeErrorSemantic.INCOMPATIBLE_TYPES, "El constructor no puede retornar un valor");
            }
            return DataType.VOID;
        }
        Type expected = currentMethod.getSemanticType();
        if (ctx.expression() != null) {
            Type t = visitor.getExpressionDelegate().visitExpression(ctx.expression());
            if (expected == Type.VOID) {
                visitor.reportError(ctx, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Metodo void no debe retornar un valor");
            } else if (!TypeChecker.isAssignable(expected, t) && t != Type.ERROR) {
                visitor.reportError(ctx, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Tipo de retorno incompatible. Esperado: " + expected);
            }
        } else {
            if (expected != Type.VOID) {
                visitor.reportError(ctx, TypeErrorSemantic.INCOMPATIBLE_TYPES, "El metodo debe retornar un valor");
            }
        }
        visitor.setShouldReturn(true);
        return DataType.VOID;
    }

    public DataType visitBreakStmt(ZetarianoParser.BreakStmtContext ctx) {
        if (ctx == null) return DataType.VOID;
        if (!visitor.isInsideLoop() && !visitor.isInsideSwitch()) {
            visitor.reportError(ctx, TypeErrorSemantic.INVALID_JUMP, "break fuera de ciclo o switch");
        }
        visitor.setShouldBreak(true);
        return DataType.VOID;
    }

    public DataType visitContinueStmt(ZetarianoParser.ContinueStmtContext ctx) {
        if (ctx == null) return DataType.VOID;
        if (!visitor.isInsideLoop()) {
            visitor.reportError(ctx, TypeErrorSemantic.INVALID_JUMP, "continue fuera de ciclo");
        }
        visitor.setShouldContinue(true);
        return DataType.VOID;
    }
}
