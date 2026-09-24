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
    private static final int MAX_ITERATIONS = 100_000;

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

        boolean prevLoop = visitor.isInsideLoop();
        visitor.setInsideLoop(true);

        if (visitor.isInsideMain()) {
            int iterations = 0;
            while (true) {
                if (ctx.expression() != null) {
                    Object condVal = expressionEval.evalExpression(ctx.expression());
                    if (!Boolean.TRUE.equals(condVal)) {
                        break;
                    }
                }

                if (++iterations > MAX_ITERATIONS) {
                    visitor.reportError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(),
                            TypeErrorSemantic.UNDEFINED_ERROR, "Bucle infinito detectado en 'para' (limite excedido)");
                    break;
                }

                visitor.getSymbolTable().pushScope("for-body");
                if (ctx.block() != null) {
                    visitor.visit(ctx.block());
                }
                visitor.getSymbolTable().popScope();

                if (visitor.isShouldBreak()) {
                    visitor.setShouldBreak(false);
                    break;
                }
                if (visitor.isShouldContinue()) {
                    visitor.setShouldContinue(false);
                }
                if (visitor.isShouldReturn()) {
                    break;
                }

                if (ctx.forUpdate() != null) {
                    visitForUpdate(ctx.forUpdate());
                }
            }
        } else {
            if (ctx.forUpdate() != null) {
                visitForUpdate(ctx.forUpdate());
            }
            if (ctx.block() != null) {
                visitor.visit(ctx.block());
            }
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

            Object val = null;
            if (ctx.expression() != null) {
                DataType init = visitor.visit(ctx.expression());
                if (!TypeChecker.isAssignable(t, init)) {
                    visitor.reportError(line, col, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Inicializacion invalida en 'para'");
                }
                val = expressionEval.evalExpression(ctx.expression());
            }

            if (visitor.getSymbolTable().lookupLocal(name) != null) {
                visitor.reportError(line, col, TypeErrorSemantic.REDECLARACION, "Variable '" + name + "' ya declarada");
            } else {
                visitor.getSymbolTable().declare(new Symbol(name, t, SymbolKind.VARIABLE, ScopeKind.LOCAL, LanguageType.Y_LANG, val, line, col));
            }
        } else if (ctx.target() != null) {
            DataType target = yVisitor.getAssignmentDelegate().resolveTarget(ctx.target());
            DataType init = visitor.visit(ctx.expression());
            if (!TypeChecker.isAssignable(target, init)) {
                visitor.reportError(line, col, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Asignacion invalida en inicializacion de 'para'");
            }
            Object val = expressionEval.evalExpression(ctx.expression());
            if (ctx.target().ID().size() == 1 && ctx.target().DOT().isEmpty() && ctx.target().LBRACK().isEmpty()) {
                String id = ctx.target().ID(0).getText();
                visitor.getSymbolTable().updateValue(id, val);
                Symbol s = visitor.getSymbolTable().lookup(id);
                if (s != null) s.value = val;
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
            if (ctx.target() != null && ctx.target().ID().size() == 1 && ctx.target().DOT().isEmpty() && ctx.target().LBRACK().isEmpty()) {
                String id = ctx.target().ID(0).getText();
                Symbol s = visitor.getSymbolTable().lookup(id);
                if (s != null && s.value instanceof Number num) {
                    Object newVal;
                    if (s.type == DataType.FLOTANTE || s.type == DataType.DECIMALIS || num instanceof Double || num instanceof Float) {
                        double delta = ctx.INC() != null ? 1.0 : -1.0;
                        newVal = num.doubleValue() + delta;
                    } else {
                        long delta = ctx.INC() != null ? 1L : -1L;
                        newVal = num.longValue() + delta;
                    }
                    visitor.getSymbolTable().updateValue(id, newVal);
                    s.value = newVal;
                } else if (s != null && s.value == null) {
                    Object newVal = (s.type == DataType.FLOTANTE || s.type == DataType.DECIMALIS) ? (ctx.INC() != null ? 1.0 : -1.0) : (ctx.INC() != null ? 1L : -1L);
                    visitor.getSymbolTable().updateValue(id, newVal);
                    s.value = newVal;
                }
            }
            return DataType.VOID;
        }

        boolean isCompound = ctx.ADD_ASSIGN() != null || ctx.SUB_ASSIGN() != null || ctx.MUL_ASSIGN() != null || ctx.DIV_ASSIGN() != null;
        DataType target = yVisitor.getAssignmentDelegate().resolveTarget(ctx.target());
        if (isCompound) {
            if (!TypeChecker.isNumeric(target) && target != DataType.ERROR) {
                visitor.reportError(line, col, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Actualizacion compuesta debe ser numerica");
            }
        }
        DataType expr = visitor.visit(ctx.expression());
        if (!TypeChecker.isAssignable(target, expr)) {
            visitor.reportError(line, col, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Actualizacion invalida en 'para'");
        }
        Object val = expressionEval.evalExpression(ctx.expression());
        if (ctx.target() != null && ctx.target().ID().size() == 1 && ctx.target().DOT().isEmpty() && ctx.target().LBRACK().isEmpty()) {
            String id = ctx.target().ID(0).getText();
            Symbol s = visitor.getSymbolTable().lookup(id);
            if (isCompound && s != null && s.value instanceof Number curNum && val instanceof Number valNum) {
                boolean isDec = s.type == DataType.FLOTANTE || s.type == DataType.DECIMALIS || curNum instanceof Double || curNum instanceof Float || valNum instanceof Double || valNum instanceof Float;
                if (ctx.ADD_ASSIGN() != null) {
                    val = isDec ? (curNum.doubleValue() + valNum.doubleValue()) : (curNum.longValue() + valNum.longValue());
                } else if (ctx.SUB_ASSIGN() != null) {
                    val = isDec ? (curNum.doubleValue() - valNum.doubleValue()) : (curNum.longValue() - valNum.longValue());
                } else if (ctx.MUL_ASSIGN() != null) {
                    val = isDec ? (curNum.doubleValue() * valNum.doubleValue()) : (curNum.longValue() * valNum.longValue());
                } else if (ctx.DIV_ASSIGN() != null) {
                    if (valNum.doubleValue() != 0) {
                        val = isDec ? (curNum.doubleValue() / valNum.doubleValue()) : (curNum.longValue() / valNum.longValue());
                    }
                }
            }
            visitor.getSymbolTable().updateValue(id, val);
            if (s != null) s.value = val;
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

        if (visitor.isInsideMain()) {
            int iterations = 0;
            while (true) {
                Object condVal = expressionEval.evalExpression(ctx.expression());
                if (!Boolean.TRUE.equals(condVal)) {
                    break;
                }

                if (++iterations > MAX_ITERATIONS) {
                    visitor.reportError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(),
                            TypeErrorSemantic.UNDEFINED_ERROR, "Bucle infinito detectado en 'mientras' (limite excedido)");
                    break;
                }

                visitor.getSymbolTable().pushScope("while");
                if (ctx.block() != null) {
                    visitor.visit(ctx.block());
                }
                visitor.getSymbolTable().popScope();

                if (visitor.isShouldBreak()) {
                    visitor.setShouldBreak(false);
                    break;
                }
                if (visitor.isShouldContinue()) {
                    visitor.setShouldContinue(false);
                }
                if (visitor.isShouldReturn()) {
                    break;
                }
            }
        } else {
            visitor.getSymbolTable().pushScope("while");
            if (ctx.block() != null) {
                visitor.visit(ctx.block());
            }
            visitor.getSymbolTable().popScope();
        }

        visitor.setInsideLoop(prevLoop);

        return DataType.VOID;
    }

    public DataType visitDoWhileStmt(YParser.DoWhileStmtContext ctx) {
        if (ctx == null) return DataType.VOID;

        boolean prevLoop = visitor.isInsideLoop();
        visitor.setInsideLoop(true);

        if (visitor.isInsideMain()) {
            int iterations = 0;
            while (true) {
                if (++iterations > MAX_ITERATIONS) {
                    visitor.reportError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(),
                            TypeErrorSemantic.UNDEFINED_ERROR, "Bucle infinito detectado en 'hacer...mientras' (limite excedido)");
                    break;
                }

                visitor.getSymbolTable().pushScope("dowhile");
                if (ctx.block() != null) {
                    visitor.visit(ctx.block());
                }
                visitor.getSymbolTable().popScope();

                if (visitor.isShouldBreak()) {
                    visitor.setShouldBreak(false);
                    break;
                }
                if (visitor.isShouldContinue()) {
                    visitor.setShouldContinue(false);
                }
                if (visitor.isShouldReturn()) {
                    break;
                }

                Object condVal = expressionEval.evalExpression(ctx.expression());
                if (!Boolean.TRUE.equals(condVal)) {
                    break;
                }
            }
        } else {
            visitor.getSymbolTable().pushScope("dowhile");
            if (ctx.block() != null) {
                visitor.visit(ctx.block());
            }
            visitor.getSymbolTable().popScope();
        }

        DataType c = visitor.visit(ctx.expression());
        if (!TypeChecker.isBool(c) && c != DataType.ERROR) {
            visitor.reportError(ctx.expression().getStart().getLine(), ctx.expression().getStart().getCharPositionInLine(), TypeErrorSemantic.NOT_BOOLEAN, "Condicion del 'mientras' debe ser booleana");
        }

        visitor.setInsideLoop(prevLoop);

        return DataType.VOID;
    }
}
