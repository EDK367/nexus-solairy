package org.nexus.nexussolairy.visitor.zetariano.statement;

import org.nexus.nexussolairy.ZetarianoParser;
import org.nexus.nexussolairy.model.enums.DataType;
import org.nexus.nexussolairy.model.enums.TypeErrorSemantic;
import org.nexus.nexussolairy.model.semantic.Symbol;
import org.nexus.nexussolairy.model.semantic.Type;
import org.nexus.nexussolairy.model.semantic.TypeChecker;
import org.nexus.nexussolairy.model.semantic.VariableSymbol;
import org.nexus.nexussolairy.visitor.zetariano.ZetarianoVisitorImpl;
import org.nexus.nexussolairy.visitor.zetariano.expression.ExpressionEval;

public class LoopStatement {
    private final ZetarianoVisitorImpl visitor;
    private final ExpressionEval expressionEval;

    public LoopStatement(ZetarianoVisitorImpl visitor, ExpressionEval expressionEval) {
        this.visitor = visitor;
        this.expressionEval = expressionEval;
    }

    public DataType visitForStmt(ZetarianoParser.ForStmtContext ctx) {
        if (ctx == null) return DataType.VOID;
        visitor.pushScope("for");
        boolean prevLoop = visitor.isInsideLoop();
        visitor.setInsideLoop(true);
        if (ctx.forInit() != null) {
            visitForInit(ctx.forInit());
        }
        if (ctx.expression() != null) {
            Type c = visitor.getExpressionDelegate().visitExpression(ctx.expression());
            if (!TypeChecker.isBool(c) && c != Type.ERROR) {
                visitor.reportError(ctx, TypeErrorSemantic.NOT_BOOLEAN, "Condicion del for debe ser booleana");
            }
        }
        if (ctx.forUpdate() != null) {
            visitForUpdate(ctx.forUpdate());
        }
        if (ctx.statement() != null) {
            visitor.visitStatement(ctx.statement());
        }
        visitor.setInsideLoop(prevLoop);
        visitor.popScope();
        return DataType.VOID;
    }

    public DataType visitForInit(ZetarianoParser.ForInitContext ctx) {
        if (ctx == null) return DataType.VOID;
        if (ctx.type() != null) {
            Type t = visitor.getVariableDelegate().getType(ctx.type());
            String name = ctx.ID().getText();
            if (visitor.getSymbolTable().getCurrentScope().lookupLocal(name) != null) {
                visitor.reportError(ctx, TypeErrorSemantic.REDECLARACION, "Variable '" + name + "' ya declarada");
            } else {
                Object value = null;
                if (ctx.expression() != null) {
                    Type init = visitor.getExpressionDelegate().visitExpression(ctx.expression());
                    if (!TypeChecker.isAssignable(t, init)) {
                        visitor.reportError(ctx, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Inicializacion invalida en for");
                    }
                    value = visitor.getExpressionEval().evalExpression(ctx.expression());
                } else {
                    value = visitor.getExpressionEval().getDefaultValue(t);
                }
                VariableSymbol vs = new VariableSymbol(name, t, value, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
                visitor.getSymbolTable().getCurrentScope().define(vs);
            }
        } else if (ctx.ID() != null) {
            String id = ctx.ID().getText();
            Symbol s = visitor.resolveSymbol(id);
            if (s == null) {
                visitor.reportError(ctx, TypeErrorSemantic.UNDECLARED, "Variable '" + id + "' no declarada");
            } else {
                Type expr = visitor.getExpressionDelegate().visitExpression(ctx.expression());
                if (!TypeChecker.isAssignable(s.getSemanticType(), expr)) {
                    visitor.reportError(ctx, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Asignacion invalida en for");
                }
                Object val = visitor.getExpressionEval().evalExpression(ctx.expression());
                visitor.getSymbolTable().updateValue(id, val);
                if (s != null) {
                    s.value = val;
                }
            }
        }
        return DataType.VOID;
    }

    public DataType visitForUpdate(ZetarianoParser.ForUpdateContext ctx) {
        if (ctx == null) return DataType.VOID;
        Type target = visitor.getAssignmentDelegate().resolveLeftValue(ctx.leftValue());
        if (ctx.INC() != null || ctx.DEC() != null) {
            if (!TypeChecker.isNumeric(target) && target != Type.ERROR) {
                visitor.reportError(ctx, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Actualizacion del for debe ser numerica");
            }
            if (ctx.leftValue() != null && ctx.leftValue().DOT() == null && ctx.leftValue().LBRACK() == null) {
                String id = ctx.leftValue().ID(0).getText();
                Symbol s = visitor.resolveSymbol(id);
                if (s != null && s.value instanceof Number num) {
                    Object newVal;
                    if (s.type == DataType.FLOTANTE || s.type == DataType.DECIMALIS || num instanceof Double || num instanceof Float) {
                        newVal = num.doubleValue() + (ctx.INC() != null ? 1.0 : -1.0);
                    } else if (num instanceof Integer || s.type == DataType.ENTERO || s.type == DataType.NUMERUS) {
                        newVal = num.intValue() + (ctx.INC() != null ? 1 : -1);
                    } else {
                        newVal = num.longValue() + (ctx.INC() != null ? 1L : -1L);
                    }
                    visitor.getSymbolTable().updateValue(id, newVal);
                    s.value = newVal;
                }
            }
            return DataType.VOID;
        }
        if (ctx.expression() != null) {
            Type expr = visitor.getExpressionDelegate().visitExpression(ctx.expression());
            if (target != Type.ERROR && expr != Type.ERROR) {
                if (!TypeChecker.isAssignable(target, expr)) {
                    visitor.reportError(ctx, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Actualizacion invalida en for");
                }
            }
            Object val = visitor.getExpressionEval().evalExpression(ctx.expression());
            if (ctx.leftValue() != null && ctx.leftValue().DOT() == null && ctx.leftValue().LBRACK() == null) {
                String id = ctx.leftValue().ID(0).getText();
                Symbol s = visitor.resolveSymbol(id);
                visitor.getSymbolTable().updateValue(id, val);
                if (s != null) {
                    s.value = val;
                }
            }
        }
        return DataType.VOID;
    }

    public DataType visitWhileStmt(ZetarianoParser.WhileStmtContext ctx) {
        if (ctx == null) return DataType.VOID;
        boolean prevLoop = visitor.isInsideLoop();
        visitor.setInsideLoop(true);
        if (ctx.expression() != null) {
            Type c = visitor.getExpressionDelegate().visitExpression(ctx.expression());
            if (!TypeChecker.isBool(c) && c != Type.ERROR) {
                visitor.reportError(ctx, TypeErrorSemantic.NOT_BOOLEAN, "Condicion del while debe ser booleana");
            }
        }
        if (ctx.block() != null) {
            visitor.visitBlock(ctx.block());
        }
        visitor.setInsideLoop(prevLoop);
        return DataType.VOID;
    }

    public DataType visitDoWhileStmt(ZetarianoParser.DoWhileStmtContext ctx) {
        if (ctx == null) return DataType.VOID;
        boolean prevLoop = visitor.isInsideLoop();
        visitor.setInsideLoop(true);
        if (ctx.block() != null) {
            visitor.visitBlock(ctx.block());
        }
        if (ctx.expression() != null) {
            Type c = visitor.getExpressionDelegate().visitExpression(ctx.expression());
            if (!TypeChecker.isBool(c) && c != Type.ERROR) {
                visitor.reportError(ctx, TypeErrorSemantic.NOT_BOOLEAN, "Condicion del do-while debe ser booleana");
            }
        }
        visitor.setInsideLoop(prevLoop);
        return DataType.VOID;
    }
}
