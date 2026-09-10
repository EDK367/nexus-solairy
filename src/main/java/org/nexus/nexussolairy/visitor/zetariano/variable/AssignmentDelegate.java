package org.nexus.nexussolairy.visitor.zetariano.variable;

import org.nexus.nexussolairy.ZetarianoParser;
import org.nexus.nexussolairy.model.enums.DataType;
import org.nexus.nexussolairy.model.enums.TypeErrorSemantic;
import org.nexus.nexussolairy.model.semantic.Symbol;
import org.nexus.nexussolairy.model.semantic.Type;
import org.nexus.nexussolairy.model.semantic.TypeChecker;
import org.nexus.nexussolairy.model.semantic.VariableSymbol;
import org.nexus.nexussolairy.visitor.zetariano.ZetarianoVisitorImpl;
import org.nexus.nexussolairy.visitor.zetariano.expression.ExpressionEval;

import java.util.List;
import java.util.Map;

public class AssignmentDelegate {
    private final ZetarianoVisitorImpl visitor;
    private final ExpressionEval expressionEval;

    public AssignmentDelegate(ZetarianoVisitorImpl visitor, ExpressionEval expressionEval) {
        this.visitor = visitor;
        this.expressionEval = expressionEval;
    }

    public DataType visitAssignStmt(ZetarianoParser.AssignStmtContext ctx) {
        if (ctx == null) return DataType.VOID;
        Type target = resolveLeftValue(ctx.leftValue());
        if (ctx.INC() != null || ctx.DEC() != null) {
            if (!TypeChecker.isNumeric(target) && target != Type.ERROR) {
                visitor.reportError(ctx, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Inc/Dec solo en numericos");
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

        Object val = null;
        if (ctx.expression() != null) {
            Type expr = visitor.getExpressionDelegate().visitExpression(ctx.expression());
            if (target != Type.ERROR && expr != Type.ERROR) {
                if (ctx.ADD_ASSIGN() != null || ctx.SUB_ASSIGN() != null || ctx.MUL_ASSIGN() != null || ctx.DIV_ASSIGN() != null) {
                    String op = ctx.ADD_ASSIGN() != null ? "+" : ctx.SUB_ASSIGN() != null ? "-" : ctx.MUL_ASSIGN() != null ? "*" : "/";
                    Type res = TypeChecker.getResultType(target, op, expr);
                    if (!TypeChecker.isAssignable(target, res)) {
                        visitor.reportError(ctx, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Asignacion invalida");
                    }
                } else {
                    if (!TypeChecker.isAssignable(target, expr)) {
                        visitor.reportError(ctx, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Asignacion invalida");
                    }
                }
            }
            val = expressionEval.evalExpression(ctx.expression());
        }

        if (ctx.leftValue() != null) {
            String id = ctx.leftValue().ID(0).getText();
            Symbol s = visitor.resolveSymbol(id);
            if (ctx.leftValue().DOT() == null && ctx.leftValue().LBRACK() == null) {
                if (ctx.ADD_ASSIGN() != null || ctx.SUB_ASSIGN() != null || ctx.MUL_ASSIGN() != null || ctx.DIV_ASSIGN() != null) {
                    if (s != null && s.value != null && val != null) {
                        String op = ctx.ADD_ASSIGN() != null ? "+" : ctx.SUB_ASSIGN() != null ? "-" : ctx.MUL_ASSIGN() != null ? "*" : "/";
                        Object newVal = expressionEval.applyBinaryOp(s.value, op, val);
                        newVal = coerceValue(s, newVal);
                        visitor.getSymbolTable().updateValue(id, newVal);
                        s.value = newVal;
                    }
                } else {
                    Object coerced = coerceValue(s, val);
                    visitor.getSymbolTable().updateValue(id, coerced);
                    if (s != null) {
                        s.value = coerced;
                    }
                }
            } else if (ctx.leftValue().DOT() != null && ctx.leftValue().LBRACK() == null) {
                String fieldName = ctx.leftValue().ID(1).getText();
                if (s != null && s.value instanceof Map map) {
                    map.put(fieldName, val);
                }
            } else if (ctx.leftValue().DOT() == null && ctx.leftValue().LBRACK() != null) {
                if (s != null && s.value instanceof List list) {
                    Object idxObj = expressionEval.evalExpression(ctx.leftValue().expression());
                    if (idxObj instanceof Number n) {
                        int idx = n.intValue();
                        if (idx >= 0 && idx < list.size()) {
                            list.set(idx, val);
                        }
                    }
                }
            }
        }
        return DataType.VOID;
    }

    public Type resolveLeftValue(ZetarianoParser.LeftValueContext ctx) {
        if (ctx == null) return Type.ERROR;
        String id = ctx.ID(0).getText();
        Symbol s = visitor.resolveSymbol(id);
        if (s == null) {
            visitor.reportError(ctx, TypeErrorSemantic.UNDECLARED, "'" + id + "' no declarado");
            return Type.ERROR;
        }

        if (ctx.DOT() == null && ctx.LBRACK() == null) {
            return s.getSemanticType();
        }

        if (ctx.DOT() == null && ctx.LBRACK() != null) {
            if (s instanceof VariableSymbol vs && !vs.isArray()) {
                visitor.reportError(ctx, TypeErrorSemantic.NOT_ARRAY, "'" + id + "' no es un arreglo");
            }
            if (ctx.expression() != null) {
                Type idx = visitor.getExpressionDelegate().visitExpression(ctx.expression());
                if (!TypeChecker.isInt(idx) && idx != Type.ERROR) {
                    visitor.reportError(ctx, TypeErrorSemantic.ARRAY_INDEX_ERROR, "Indice debe ser entero");
                }
            }
            return s.getSemanticType();
        }

        if (ctx.DOT() != null) {
            Type objType = s.getSemanticType();
            String fieldName = ctx.ID(1).getText();
            Type fieldType = visitor.getExpressionDelegate().resolveFieldAccess(objType, fieldName, ctx);
            if (ctx.LBRACK() != null && ctx.expression() != null) {
                Type idx = visitor.getExpressionDelegate().visitExpression(ctx.expression());
                if (!TypeChecker.isInt(idx) && idx != Type.ERROR) {
                    visitor.reportError(ctx, TypeErrorSemantic.ARRAY_INDEX_ERROR, "Indice debe ser entero");
                }
            }
            return fieldType;
        }
        return Type.ERROR;
    }

    private Object coerceValue(Symbol s, Object val) {
        if (val == null || s == null) return val;
        if (s.getSemanticType() != null) {
            String tn = s.getSemanticType().getName().toLowerCase();
            if (("int".equals(tn) || "entero".equals(tn) || "numerus".equals(tn)) && val instanceof Number n) {
                return n.intValue();
            }
            if (("double".equals(tn) || "decimalis".equals(tn) || "flotante".equals(tn)) && val instanceof Number n) {
                return n.doubleValue();
            }
        }
        if ((s.type == DataType.ENTERO || s.type == DataType.NUMERUS) && val instanceof Number n) {
            return n.intValue();
        }
        if ((s.type == DataType.DECIMALIS || s.type == DataType.FLOTANTE) && val instanceof Number n) {
            return n.doubleValue();
        }
        return val;
    }
}
