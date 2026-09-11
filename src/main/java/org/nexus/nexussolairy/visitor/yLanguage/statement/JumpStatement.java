package org.nexus.nexussolairy.visitor.yLanguage.statement;

import org.nexus.nexussolairy.YParser;
import org.nexus.nexussolairy.model.enums.DataType;
import org.nexus.nexussolairy.model.enums.TypeErrorSemantic;
import org.nexus.nexussolairy.model.semantic.TypeChecker;
import org.nexus.nexussolairy.visitor.VisitorContext;
import org.nexus.nexussolairy.visitor.yLanguage.YVisitorImpl;

public class JumpStatement {
    private final VisitorContext visitor;

    public JumpStatement(VisitorContext visitor) {
        this.visitor = visitor;
    }

    public DataType visitReturnStmt(YParser.ReturnStmtContext ctx) {
        if (ctx == null) return DataType.VOID;
        int line = ctx.getStart().getLine();
        int col = ctx.getStart().getCharPositionInLine();
        YVisitorImpl yVisitor = (YVisitorImpl) visitor;

        if (!visitor.isInsideFunction() || yVisitor.getCurrentFunction() == null) {
            visitor.reportError(line, col, TypeErrorSemantic.INVALID_JUMP, "'retornar' fuera de funcion");
            return DataType.VOID;
        }

        DataType expected = visitor.getCurrentFunctionReturnType();
        if (ctx.expression() != null) {
            DataType actual = visitor.visit(ctx.expression());
            if (!TypeChecker.isAssignable(expected, actual)) {
                visitor.reportError(line, col, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Tipo de retorno incompatible. Esperado: " + expected + ", obtenido: " + actual);
            }
            Object val = yVisitor.getExpressionEval().evalExpression(ctx.expression());
            visitor.setReturnValue(val);
        } else {
            if (expected != DataType.VOID) {
                visitor.reportError(line, col, TypeErrorSemantic.INCOMPATIBLE_TYPES, "La funcion debe retornar un valor");
            }
            visitor.setReturnValue(null);
        }

        visitor.setShouldReturn(true);
        return DataType.VOID;
    }

    public DataType visitBreakStmt(YParser.BreakStmtContext ctx) {
        if (ctx == null) return DataType.VOID;
        int line = ctx.getStart().getLine();
        int col = ctx.getStart().getCharPositionInLine();
        YVisitorImpl yVisitor = (YVisitorImpl) visitor;

        if (!visitor.isInsideLoop() && !yVisitor.isInsideSwitch()) {
            visitor.reportError(line, col, TypeErrorSemantic.INVALID_JUMP, "Sentencia romper fuera de un ciclo o elegir");
        }
        visitor.setShouldBreak(true);
        return DataType.VOID;
    }

    public DataType visitContinueStmt(YParser.ContinueStmtContext ctx) {
        if (ctx == null) return DataType.VOID;
        int line = ctx.getStart().getLine();
        int col = ctx.getStart().getCharPositionInLine();

        if (!visitor.isInsideLoop()) {
            visitor.reportError(line, col, TypeErrorSemantic.INVALID_JUMP, "Sentencia continuar fuera de un ciclo");
        }
        visitor.setShouldContinue(true);
        return DataType.VOID;
    }
}
