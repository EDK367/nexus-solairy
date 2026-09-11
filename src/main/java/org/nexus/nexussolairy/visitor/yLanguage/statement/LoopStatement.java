package org.nexus.nexussolairy.visitor.yLanguage.statement;

import org.nexus.nexussolairy.YParser;
import org.nexus.nexussolairy.model.enums.*;
import org.nexus.nexussolairy.model.semantic.Symbol;
import org.nexus.nexussolairy.model.semantic.TypeChecker;
import org.nexus.nexussolairy.visitor.VisitorContext;
import org.nexus.nexussolairy.visitor.yLanguage.YVisitorImpl;
import org.nexus.nexussolairy.visitor.yLanguage.expression.ExpressionEval;

public class LoopStatement {
    private final VisitorContext visitor;
    private final ExpressionEval expressionEval;

    public LoopStatement(VisitorContext visitor, ExpressionEval expressionEval) {
        this.visitor = visitor;
        this.expressionEval = expressionEval;
    }

    public DataType visitForStmt(YParser.ForStmtContext ctx) {
        if (ctx == null) return DataType.VOID;

        visitor.getSymbolTable().pushScope("for");

        if (ctx.forInit() != null) {
            visitForInit(ctx.forInit());
        }

        if (ctx.expression() != null) {
            DataType c = visitor.visit(ctx.expression());
            if (!TypeChecker.isBool(c) && c != DataType.ERROR) {
                visitor.reportError(ctx.expression().getStart().getLine(), ctx.expression().getStart().getCharPositionInLine(), TypeErrorSemantic.NOT_BOOLEAN, "Condicion del 'para' debe ser booleana");
            }
        }

        if (ctx.forUpdate() != null) {
            visitForUpdate(ctx.forUpdate());
        }

        boolean prevLoop = visitor.isInsideLoop();
        visitor.setInsideLoop(true);

        if (ctx.block() != null) {
            visitor.visit(ctx.block());
        }

        visitor.setInsideLoop(prevLoop);
        visitor.getSymbolTable().popScope();

        return DataType.VOID;
    }

    public DataType visitForInit(YParser.ForInitContext ctx) {
        if (ctx == null) return DataType.VOID;
        int line = ctx.getStart().getLine();
        int col = ctx.getStart().getCharPositionInLine();
        YVisitorImpl yVisitor = (YVisitorImpl) visitor;

        if (ctx.type() != null) {
            DataType t = yVisitor.visitType(ctx.type());
            String name = ctx.ID().getText();

            if (visitor.getSymbolTable().lookupLocal(name) != null) {
                visitor.reportError(line, col, TypeErrorSemantic.REDECLARACION, "Variable '" + name + "' ya declarada");
            } else {
                visitor.getSymbolTable().declare(new Symbol(name, t, SymbolKind.VARIABLE, ScopeKind.LOCAL, LanguageType.Y_LANG, null, line, col));
            }

            DataType init = visitor.visit(ctx.expression());
            if (!TypeChecker.isAssignable(t, init)) {
                visitor.reportError(line, col, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Inicializacion invalida en 'para'");
            }
        } else if (ctx.target() != null) {
            DataType target = yVisitor.getAssignmentDelegate().resolveTarget(ctx.target());
            DataType init = visitor.visit(ctx.expression());
            if (!TypeChecker.isAssignable(target, init)) {
                visitor.reportError(line, col, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Asignacion invalida en inicializacion de 'para'");
            }
        }

        return DataType.VOID;
    }

    public DataType visitForUpdate(YParser.ForUpdateContext ctx) {
        if (ctx == null) return DataType.VOID;
        int line = ctx.getStart().getLine();
        int col = ctx.getStart().getCharPositionInLine();
        YVisitorImpl yVisitor = (YVisitorImpl) visitor;

        if (ctx.INC() != null || ctx.DEC() != null) {
            DataType t = yVisitor.getAssignmentDelegate().resolveTarget(ctx.target());
            if (!TypeChecker.isNumeric(t) && t != DataType.ERROR) {
                visitor.reportError(line, col, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Actualizacion del 'para' debe ser numerica");
            }
            return DataType.VOID;
        }

        DataType target = yVisitor.getAssignmentDelegate().resolveTarget(ctx.target());
        DataType expr = visitor.visit(ctx.expression());
        if (!TypeChecker.isAssignable(target, expr)) {
            visitor.reportError(line, col, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Actualizacion invalida en 'para'");
        }

        return DataType.VOID;
    }

    public DataType visitWhileStmt(YParser.WhileStmtContext ctx) {
        if (ctx == null) return DataType.VOID;

        DataType c = visitor.visit(ctx.expression());
        if (!TypeChecker.isBool(c) && c != DataType.ERROR) {
            visitor.reportError(ctx.expression().getStart().getLine(), ctx.expression().getStart().getCharPositionInLine(), TypeErrorSemantic.NOT_BOOLEAN, "Condicion del 'mientras' debe ser booleana");
        }

        boolean prevLoop = visitor.isInsideLoop();
        visitor.setInsideLoop(true);

        visitor.getSymbolTable().pushScope("while");
        if (ctx.block() != null) {
            visitor.visit(ctx.block());
        }
        visitor.getSymbolTable().popScope();

        visitor.setInsideLoop(prevLoop);

        return DataType.VOID;
    }

    public DataType visitDoWhileStmt(YParser.DoWhileStmtContext ctx) {
        if (ctx == null) return DataType.VOID;

        boolean prevLoop = visitor.isInsideLoop();
        visitor.setInsideLoop(true);

        visitor.getSymbolTable().pushScope("dowhile");
        if (ctx.block() != null) {
            visitor.visit(ctx.block());
        }
        visitor.getSymbolTable().popScope();

        visitor.setInsideLoop(prevLoop);

        DataType c = visitor.visit(ctx.expression());
        if (!TypeChecker.isBool(c) && c != DataType.ERROR) {
            visitor.reportError(ctx.expression().getStart().getLine(), ctx.expression().getStart().getCharPositionInLine(), TypeErrorSemantic.NOT_BOOLEAN, "Condicion del 'mientras' debe ser booleana");
        }

        return DataType.VOID;
    }
}
