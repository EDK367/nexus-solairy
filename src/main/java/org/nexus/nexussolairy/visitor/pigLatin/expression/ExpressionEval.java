package org.nexus.nexussolairy.visitor.pigLatin.expression;

import org.nexus.nexussolairy.PigLatinParser;
import org.nexus.nexussolairy.model.enums.SymbolKind;
import org.nexus.nexussolairy.model.enums.TypeErrorSemantic;
import org.nexus.nexussolairy.model.semantic.Symbol;
import org.nexus.nexussolairy.visitor.VisitorContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpressionEval {
    private final VisitorContext visitor;

    public ExpressionEval(VisitorContext visitor) {
        this.visitor = visitor;
    }

    public Object evalExpression(PigLatinParser.ExpressionContext ctx) {
        if (ctx == null) return null;
        return evalOrExpression(ctx.orExpression());
    }

    public Object evalOrExpression(PigLatinParser.OrExpressionContext ctx) {
        if (ctx == null || ctx.andExpression().isEmpty()) return null;
        Object left = evalAndExpression(ctx.andExpression(0));
        for (int i = 1; i < ctx.andExpression().size(); i++) {
            Object right = evalAndExpression(ctx.andExpression(i));
            if (left instanceof Boolean lb && right instanceof Boolean rb) {
                left = lb || rb;
            } else {
                return null;
            }
        }
        return left;
    }

    public Object evalAndExpression(PigLatinParser.AndExpressionContext ctx) {
        if (ctx == null || ctx.relationalExpression().isEmpty()) return null;
        Object left = evalRelationalExpression(ctx.relationalExpression(0));
        for (int i = 1; i < ctx.relationalExpression().size(); i++) {
            Object right = evalRelationalExpression(ctx.relationalExpression(i));
            if (left instanceof Boolean lb && right instanceof Boolean rb) {
                left = lb && rb;
            } else {
                return null;
            }
        }
        return left;
    }

    public Object evalRelationalExpression(PigLatinParser.RelationalExpressionContext ctx) {
        if (ctx == null || ctx.additiveExpression().isEmpty()) return null;
        Object left = evalAdditiveExpression(ctx.additiveExpression(0));
        for (int i = 1; i < ctx.additiveExpression().size(); i++) {
            Object right = evalAdditiveExpression(ctx.additiveExpression(i));
            String operator = ctx.getChild(2 * i - 1).getText();
            left = applyRelational(left, right, operator);
        }
        return left;
    }

    private Object applyRelational(Object left, Object right, String op) {
        if (left instanceof Number ln && right instanceof Number rn) {
            double l = ln.doubleValue();
            double r = rn.doubleValue();
            return switch (op) {
                case "==" -> l == r;
                case "!=" -> l != r;
                case "<" -> l < r;
                case ">" -> l > r;
                case "<=" -> l <= r;
                case ">=" -> l >= r;
                default -> null;
            };
        }
        if (left instanceof String ls && right instanceof String rs) {
            return switch (op) {
                case "==" -> ls.equals(rs);
                case "!=" -> !ls.equals(rs);
                default -> null;
            };
        }
        if (left instanceof Boolean lb && right instanceof Boolean rb) {
            return switch (op) {
                case "==" -> lb.equals(rb);
                case "!=" -> !lb.equals(rb);
                default -> null;
            };
        }
        return null;
    }

    public Object evalAdditiveExpression(PigLatinParser.AdditiveExpressionContext ctx) {
        if (ctx == null || ctx.multiplicativeExpression().isEmpty()) return null;
        Object left = evalMultiplicativeExpression(ctx.multiplicativeExpression(0));
        for (int i = 1; i < ctx.multiplicativeExpression().size(); i++) {
            Object right = evalMultiplicativeExpression(ctx.multiplicativeExpression(i));
            String operator = ctx.getChild(2 * i - 1).getText();
            left = applyAdditive(left, right, operator);
        }
        return left;
    }

    private Object applyAdditive(Object left, Object right, String op) {
        if ("+".equals(op)) {
            if (left instanceof String || right instanceof String) {
                return toDisplayString(left) + toDisplayString(right);
            }
            return applyNumeric(left, right, op);
        }
        if ("-".equals(op)) {
            return applyNumeric(left, right, op);
        }
        return null;
    }

    public Object evalMultiplicativeExpression(PigLatinParser.MultiplicativeExpressionContext ctx) {
        if (ctx == null || ctx.unaryExpression().isEmpty()) return null;
        Object left = evalUnaryExpression(ctx.unaryExpression(0));
        for (int i = 1; i < ctx.unaryExpression().size(); i++) {
            Object right = evalUnaryExpression(ctx.unaryExpression(i));
            String operator = ctx.getChild(2 * i - 1).getText();
            if ("/".equals(operator)) {
                if (isZero(right)) {
                    var child = ctx.unaryExpression(i);
                    int line = child.getStart().getLine();
                    int col = child.getStart().getCharPositionInLine();
                    visitor.reportError(line, col, TypeErrorSemantic.MATH_ERROR, "División por cero");
                    return null;
                }
            }
            left = applyNumeric(left, right, operator);
        }
        return left;
    }

    private boolean isZero(Object value) {
        if (value == null) return false;
        if (value instanceof Long l) return l == 0;
        if (value instanceof Integer i) return i == 0;
        if (value instanceof Double d) return d == 0.0 || Double.isNaN(d);
        if (value instanceof Float f) return f == 0.0f || Float.isNaN(f);
        if (value instanceof Number n) return n.doubleValue() == 0.0;
        return false;
    }

    private Object applyNumeric(Object left, Object right, String op) {
        if (left == null || right == null) return null;
        if (left instanceof Character c) left = (long) c;
        if (right instanceof Character c) right = (long) c;

        if (left instanceof Number ln && right instanceof Number rn) {
            if (ln instanceof Double || rn instanceof Double) {
                double l = ln.doubleValue();
                double r = rn.doubleValue();
                return switch (op) {
                    case "+" -> l + r;
                    case "-" -> l - r;
                    case "*" -> l * r;
                    case "/" -> r != 0.0 ? l / r : null;
                    default -> 0.0;
                };
            }
            long l = ln.longValue();
            long r = rn.longValue();
            return switch (op) {
                case "+" -> l + r;
                case "-" -> l - r;
                case "*" -> l * r;
                case "/" -> r != 0L ? l / r : null;
                default -> 0L;
            };
        }
        return null;
    }

    public Object evalUnaryExpression(PigLatinParser.UnaryExpressionContext ctx) {
        if (ctx == null) return null;
        if (ctx.NOT() != null) {
            Object inner = evalUnaryExpression(ctx.unaryExpression());
            if (inner instanceof Boolean b) return !b;
            return null;
        }
        if (ctx.MINUS() != null) {
            Object inner = evalUnaryExpression(ctx.unaryExpression());
            if (inner instanceof Long n) return -n;
            if (inner instanceof Integer n) return -n.longValue();
            if (inner instanceof Double n) return -n;
            return null;
        }
        return evalPostfixExpression(ctx.postfixExpression());
    }

    public Object evalPostfixExpression(PigLatinParser.PostfixExpressionContext ctx) {
        if (ctx == null) return null;
        Object value = evalPrimaryExpression(ctx.primaryExpression());
        if (ctx.INC() != null) {
            Object result = null;
            if (value instanceof Long l) result = l + 1;
            else if (value instanceof Integer i) result = (long) (i + 1);
            else if (value instanceof Double d) result = d + 1;
            else if (value == null) result = 1L;
            if (result != null) {
                saveIfVariable(ctx.primaryExpression(), result);
                return result;
            }
        }
        if (ctx.DEC() != null) {
            Object result = null;
            if (value instanceof Long l) result = l - 1;
            else if (value instanceof Integer i) result = (long) (i - 1);
            else if (value instanceof Double d) result = d - 1;
            else if (value == null) result = -1L;
            if (result != null) {
                saveIfVariable(ctx.primaryExpression(), result);
                return result;
            }
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public Object evalPrimaryExpression(PigLatinParser.PrimaryExpressionContext ctx) {
        if (ctx == null) return null;
        if (ctx.literal() != null) {
            return evalLiteral(ctx.literal());
        }
        if (ctx.ID().size() == 1 && ctx.LBRACK() == null && ctx.DOT() == null && ctx.LPAREN() == null) {
            String name = ctx.ID(0).getText();
            Symbol symbol = visitor.getSymbolTable().lookup(name);
            if (symbol == null) return null;
            return symbol.value;
        }
        if (ctx.ID().size() == 1 && ctx.LBRACK() != null && ctx.DOT() == null && ctx.LPAREN() == null) {
            return evalArrayAccess(ctx);
        }
        if (ctx.ID().size() == 2 && ctx.LPAREN() == null) {
            return evalAttributeAccess(ctx);
        }
        if (ctx.structLiteral() != null) {
            return evalStructLiteral(ctx.structLiteral());
        }
        if (ctx.LPAREN() != null && ctx.ID().isEmpty()) {
            if (ctx.expression() != null) {
                return evalExpression(ctx.expression());
            }
            return null;
        }
        if (ctx.LPAREN() != null && !ctx.ID().isEmpty()) {
            String name = ctx.ID(0).getText();
            List<Object> args = new ArrayList<>();
            if (ctx.argumentList() != null) {
                for (PigLatinParser.ExpressionContext expr : ctx.argumentList().expression()) {
                    args.add(evalExpression(expr));
                }
            }
            return visitor.executeFunctionCall(name, args);
        }
        return null;
    }

    private Object evalArrayAccess(PigLatinParser.PrimaryExpressionContext ctx) {
        String name = ctx.ID(0).getText();
        Symbol symbol = visitor.getSymbolTable().lookup(name);
        if (symbol == null || symbol.kind != SymbolKind.ARRAY) return null;
        Object indexObj = evalExpression(ctx.expression());
        int index = 0;
        if (indexObj instanceof Long l) index = l.intValue();
        else if (indexObj instanceof Integer i) index = i;
        else if (indexObj instanceof Double d) index = (int) Math.floor(d);
        else return null;

        if (symbol.value instanceof List<?> list) {
            if (index < 0 || index >= list.size()) return null;
            return list.get(index);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Object evalAttributeAccess(PigLatinParser.PrimaryExpressionContext ctx) {
        String varName = ctx.ID(0).getText();
        String attrName = ctx.ID(1).getText();
        Symbol symbol = visitor.getSymbolTable().lookup(varName);
        if (symbol == null || !(symbol.value instanceof Map)) return null;
        Map<String, Object> fields = (Map<String, Object>) symbol.value;

        if (ctx.LBRACK() != null) {
            Object arr = fields.get(attrName);
            if (!(arr instanceof List<?> list)) return null;
            Object indexObj = evalExpression(ctx.expression());
            int index = 0;
            if (indexObj instanceof Long l) index = l.intValue();
            else if (indexObj instanceof Integer i) index = i;
            else if (indexObj instanceof Double d) index = (int) Math.floor(d);
            else return null;

            if (index < 0 || index >= list.size()) return null;
            return list.get(index);
        }
        return fields.get(attrName);
    }

    private Object evalStructLiteral(PigLatinParser.StructLiteralContext ctx) {
        Map<String, Object> map = new HashMap<>();
        if (ctx.expressionList() != null) {
            int idx = 0;
            for (PigLatinParser.ExpressionContext expr : ctx.expressionList().expression()) {
                map.put("field_" + idx, evalExpression(expr));
                idx++;
            }
        }
        return map;
    }

    public Object evalLiteral(PigLatinParser.LiteralContext ctx) {
        if (ctx.NUMBER() != null) return Long.parseLong(ctx.NUMBER().getText());
        if (ctx.DECIMAL() != null) return Double.parseDouble(ctx.DECIMAL().getText());
        if (ctx.STRING() != null) {
            String raw = ctx.STRING().getText();
            return raw.substring(1, raw.length() - 1).replace("\\n", "\n").replace("\\t", "\t").replace("\\\"", "\"").replace("\\\\", "\\");
        }
        if (ctx.CHAR() != null) {
            String raw = ctx.CHAR().getText();
            return raw.charAt(1);
        }
        if (ctx.VERUM() != null) return Boolean.TRUE;
        if (ctx.FALSUS() != null) return Boolean.FALSE;
        return null;
    }

    public String toDisplayString(Object value) {
        if (value == null) return "null";
        if (value instanceof Boolean b) return b ? "verum" : "falsus";
        return value.toString();
    }

    private void saveIfVariable(PigLatinParser.PrimaryExpressionContext primary, Object newValue) {
        if (primary != null && primary.ID().size() == 1 && primary.getChildCount() == 1) {
            String name = primary.ID(0).getText();
            visitor.getSymbolTable().updateValue(name, newValue);
        }
    }
}
