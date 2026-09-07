package org.nexus.nexussolairy.visitor.pigLatin.statement;

import org.nexus.nexussolairy.PigLatinParser;
import org.nexus.nexussolairy.model.enums.DataType;
import org.nexus.nexussolairy.model.enums.SymbolKind;
import org.nexus.nexussolairy.model.enums.TypeErrorSemantic;
import org.nexus.nexussolairy.model.semantic.Symbol;
import org.nexus.nexussolairy.visitor.VisitorContext;
import org.nexus.nexussolairy.model.semantic.TypeChecker;
import org.nexus.nexussolairy.visitor.pigLatin.expression.ExpressionEval;

public class LoopStatement {
    private final VisitorContext visitor;
    private final ExpressionEval expressionEval;

    private static final int MAX_ITERATIONS = 100_000;

    public LoopStatement(VisitorContext visitor, ExpressionEval expressionEval) {
        this.visitor = visitor;
        this.expressionEval = expressionEval;
    }

    public DataType visitWhileStmt(PigLatinParser.WhileStmtContext ctx) {
        if (ctx == null) return DataType.VOID;

        DataType condType = visitor.visit(ctx.expression());
        if (condType != DataType.BOOLEAN && condType != DataType.ERROR) {
            visitor.reportError(ctx.expression().getStart().getLine(), ctx.expression().getStart().getCharPositionInLine(), TypeErrorSemantic.NOT_BOOLEAN, "Condicion del 'dum' debe ser booleana.");
            return DataType.VOID;
        }
        if (condType == DataType.ERROR) {
            return DataType.VOID;
        }

        boolean prevLoop = visitor.isInsideLoop();
        visitor.setInsideLoop(true);

        int iterations = 0;
        while (true) {
            Object condVal = expressionEval.evalExpression(ctx.expression());
            if (!Boolean.TRUE.equals(condVal)) {
                break;
            }

            if (++iterations > MAX_ITERATIONS) {
                visitor.reportError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(),
                        TypeErrorSemantic.UNDEFINED_ERROR, "Bucle infinito detectado en 'dum' (límite de iteraciones excedido).");
                break;
            }

            visitor.getSymbolTable().pushScope("while");
            for (PigLatinParser.StatementContext s : ctx.statement()) {
                visitor.visit(s);
                if (visitor.isShouldBreak() || visitor.isShouldContinue() || visitor.isShouldReturn()) {
                    break;
                }
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

        visitor.setInsideLoop(prevLoop);
        return DataType.VOID;
    }

    public DataType visitDoWhileStmt(PigLatinParser.DoWhileStmtContext ctx) {
        if (ctx == null) return DataType.VOID;

        boolean prevLoop = visitor.isInsideLoop();
        visitor.setInsideLoop(true);

        int iterations = 0;
        while (true) {
            if (++iterations > MAX_ITERATIONS) {
                visitor.reportError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(),
                        TypeErrorSemantic.UNDEFINED_ERROR, "Bucle infinito detectado en 'facere...dum' (límite de iteraciones excedido).");
                break;
            }

            visitor.getSymbolTable().pushScope("dowhile");
            for (PigLatinParser.StatementContext s : ctx.statement()) {
                visitor.visit(s);
                if (visitor.isShouldBreak() || visitor.isShouldContinue() || visitor.isShouldReturn()) {
                    break;
                }
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

            if (iterations == 1) {
                DataType condType = visitor.visit(ctx.expression());
                if (condType != DataType.BOOLEAN && condType != DataType.ERROR) {
                    visitor.reportError(ctx.expression().getStart().getLine(), ctx.expression().getStart().getCharPositionInLine(), TypeErrorSemantic.NOT_BOOLEAN, "Condicion del 'dum' debe ser booleana.");
                    break;
                }
                if (condType == DataType.ERROR) {
                    break;
                }
            }

            Object condVal = expressionEval.evalExpression(ctx.expression());
            if (!Boolean.TRUE.equals(condVal)) {
                break;
            }
        }

        visitor.setInsideLoop(prevLoop);
        return DataType.VOID;
    }

    public DataType visitForStmt(PigLatinParser.ForStmtContext ctx) {
        if (ctx == null) return DataType.VOID;

        visitor.getSymbolTable().pushScope("for");

        if (ctx.forInit() != null) {
            visitor.visit(ctx.forInit());
        }

        if (ctx.expression() != null) {
            DataType condType = visitor.visit(ctx.expression());
            if (condType != DataType.BOOLEAN && condType != DataType.ERROR) {
                visitor.reportError(ctx.expression().getStart().getLine(), ctx.expression().getStart().getCharPositionInLine(), TypeErrorSemantic.NOT_BOOLEAN, "Condicion del 'per' debe ser booleana.");
                visitor.getSymbolTable().popScope();
                return DataType.VOID;
            }
            if (condType == DataType.ERROR) {
                visitor.getSymbolTable().popScope();
                return DataType.VOID;
            }
        }

        boolean prevLoop = visitor.isInsideLoop();
        visitor.setInsideLoop(true);

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
                        TypeErrorSemantic.UNDEFINED_ERROR, "Bucle infinito detectado en 'per' (límite de iteraciones excedido).");
                break;
            }

            visitor.getSymbolTable().pushScope("for-body");
            for (PigLatinParser.StatementContext s : ctx.statement()) {
                visitor.visit(s);
                if (visitor.isShouldBreak() || visitor.isShouldContinue() || visitor.isShouldReturn()) {
                    break;
                }
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
                visitor.visit(ctx.forUpdate());
            }
        }

        visitor.setInsideLoop(prevLoop);
        visitor.getSymbolTable().popScope();

        return DataType.VOID;
    }

    public DataType visitForInit(PigLatinParser.ForInitContext ctx) {
        if (ctx == null) return DataType.VOID;

        if (ctx.ESTO() != null) {
            String name = ctx.ID().getText();
            DataType t = getTypeFromContext(ctx.type());
            int line = ctx.getStart().getLine();
            int col = ctx.getStart().getCharPositionInLine();

            Object val = null;
            if (ctx.expression() != null) {
                DataType init = visitor.visit(ctx.expression());
                if (!TypeChecker.isAssignable(t, init)) {
                    visitor.reportError(line, col, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Inicializacion invalida en 'per'.");
                }
                val = expressionEval.evalExpression(ctx.expression());
            }

            if (t == DataType.STRUCT) {
                String structTypeName = (ctx.type() != null && ctx.type().ID() != null) ? ctx.type().ID().getText() : null;
                Symbol s = new Symbol(name, structTypeName, visitor.getSymbolTable().getCurrentScopeKind(), val, line, col);
                visitor.getSymbolTable().declare(s);
            } else {
                Symbol s = new Symbol(name, t, SymbolKind.VARIABLE, visitor.getSymbolTable().getCurrentScopeKind(), val, line, col);
                visitor.getSymbolTable().declare(s);
            }
        } else if (ctx.ID() != null && ctx.expression() != null) {
            String id = ctx.ID().getText();
            Symbol sym = visitor.getSymbolTable().lookup(id);
            int line = ctx.getStart().getLine();
            int col = ctx.getStart().getCharPositionInLine();
            DataType exprType = visitor.visit(ctx.expression());
            if (sym == null) {
                visitor.reportError(line, col, TypeErrorSemantic.UNDECLARED, "Variable '" + id + "' no declarada.");
            } else {
                if (!TypeChecker.isAssignable(sym.type, exprType)) {
                    visitor.reportError(line, col, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Asignacion invalida en 'per'.");
                }
                Object val = expressionEval.evalExpression(ctx.expression());
                visitor.getSymbolTable().updateValue(id, val);
            }
        }
        return DataType.VOID;
    }

    public DataType visitForUpdate(PigLatinParser.ForUpdateContext ctx) {
        if (ctx == null) return DataType.VOID;

        if (ctx.ID() != null) {
            String name = ctx.ID().getText();
            Symbol s = visitor.getSymbolTable().lookup(name);
            if (s == null) {
                visitor.reportError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), TypeErrorSemantic.UNDECLARED, "Variable '" + name + "' no declarada.");
            } else if (ctx.INC() != null || ctx.DEC() != null) {
                if (s.type != DataType.NUMERUS && s.type != DataType.DECIMALIS) {
                    visitor.reportError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), TypeErrorSemantic.INCOMPATIBLE_TYPES, "Incremento/Decremento solo en tipos numericos.");
                }
                if (s.value instanceof Number num) {
                    if (s.type == DataType.DECIMALIS || num instanceof Double || num instanceof Float) {
                        double val = num.doubleValue() + (ctx.INC() != null ? 1.0 : -1.0);
                        visitor.getSymbolTable().updateValue(name, val);
                    } else {
                        long val = num.longValue() + (ctx.INC() != null ? 1L : -1L);
                        visitor.getSymbolTable().updateValue(name, val);
                    }
                } else if (s.value == null) {
                    if (s.type == DataType.DECIMALIS) {
                        visitor.getSymbolTable().updateValue(name, ctx.INC() != null ? 1.0 : -1.0);
                    } else {
                        visitor.getSymbolTable().updateValue(name, ctx.INC() != null ? 1L : -1L);
                    }
                }
            } else if (ctx.ASSIGN() != null && ctx.expression() != null) {
                DataType exprType = visitor.visit(ctx.expression());
                if (!TypeChecker.isAssignable(s.type, exprType)) {
                    visitor.reportError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), TypeErrorSemantic.INCOMPATIBLE_TYPES, "Asignacion invalida en actualizacion de 'per'.");
                }
                Object val = expressionEval.evalExpression(ctx.expression());
                visitor.getSymbolTable().updateValue(name, val);
            }
        }
        return DataType.VOID;
    }

    private DataType getTypeFromContext(PigLatinParser.TypeContext ctx) {
        if (ctx == null) return DataType.ERROR;
        if (ctx.NUMERUS() != null) return DataType.NUMERUS;
        if (ctx.TEXTUM() != null) return DataType.TEXTUM;
        if (ctx.DECIMALIS() != null) return DataType.DECIMALIS;
        if (ctx.LITTERA() != null) return DataType.LITTERA;
        if (ctx.BOOL() != null) return DataType.BOOLEAN;
        if (ctx.ID() != null) {
            String name = ctx.ID().getText();
            if (visitor.getSymbolTable().structExists(name)) return DataType.STRUCT;
            visitor.reportError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), TypeErrorSemantic.UNDECLARED, "Tipo no definido: '" + name + "'");
            return DataType.STRUCT;
        }
        return DataType.ERROR;
    }
}
