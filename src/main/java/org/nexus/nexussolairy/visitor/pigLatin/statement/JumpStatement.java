package org.nexus.nexussolairy.visitor.pigLatin.statement;

import org.nexus.nexussolairy.PigLatinParser;
import org.nexus.nexussolairy.model.enums.DataType;
import org.nexus.nexussolairy.model.enums.TypeErrorSemantic;
import org.nexus.nexussolairy.visitor.VisitorContext;

public class JumpStatement {
    private final VisitorContext visitor;

    public JumpStatement(VisitorContext visitor) {
        this.visitor = visitor;
    }

    public DataType visitBreakStmt(PigLatinParser.BreakStmtContext ctx) {
        if (ctx == null) return DataType.VOID;
        if (!visitor.isInsideLoop()) {
            visitor.reportError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), TypeErrorSemantic.INVALID_JUMP, "Sentencia break/interrumpe fuera de un ciclo");
        }
        visitor.setShouldBreak(true);
        return DataType.VOID;
    }

    public DataType visitContinueStmt(PigLatinParser.ContinueStmtContext ctx) {
        if (ctx == null) return DataType.VOID;
        if (!visitor.isInsideLoop()) {
            visitor.reportError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), TypeErrorSemantic.INVALID_JUMP, "Sentencia continue/perge fuera de un ciclo");
        }
        visitor.setShouldContinue(true);
        return DataType.VOID;
    }
}
