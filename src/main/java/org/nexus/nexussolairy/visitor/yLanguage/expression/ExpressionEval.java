package org.nexus.nexussolairy.visitor.yLanguage.expression;

import org.nexus.nexussolairy.YParser;
import org.nexus.nexussolairy.model.semantic.Symbol;
import org.nexus.nexussolairy.visitor.VisitorContext;

import java.util.List;
import java.util.Map;

public class ExpressionEval {
    private final VisitorContext visitor;

    public ExpressionEval(VisitorContext visitor) {
        this.visitor = visitor;
    }

    public Object evalExpression(YParser.ExpressionContext ctx) {
        if (ctx == null) return null;
        return evalOrExpression(ctx.orExpression());
    }

    public Object evalOrExpression(YParser.OrExpressionContext ctx) {
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

    public Object evalAndExpression(YParser.AndExpressionContext ctx) {
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

    public Object evalRelationalExpression(YParser.RelationalExpressionContext ctx) {
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

    public Object evalAdditiveExpression(YParser.AdditiveExpressionContext ctx) {
        if (ctx == null || ctx.multiplicativeExpression().isEmpty()) return null;
        Object left = evalMultiplicativeExpression(ctx.multiplicativeExpression(0));
        for (int i = 1; i < ctx.multiplicativeExpression().size(); i++) {
            Object right = evalMultiplicativeExpression(ctx.multiplicativeExpression(i));
            String op = ctx.getChild(2 * i - 1).getText();
            if ("+".equals(op)) {
                if (left instanceof Map || right instanceof Map) {
                    left = null;
                } else if (left instanceof String || right instanceof String) {
                    left = toDisplayString(left) + toDisplayString(right);
                } else if (left instanceof Number ln && right instanceof Number rn) {
                    if (left instanceof Double || right instanceof Double || left instanceof Float || right instanceof Float) {
                        left = ln.doubleValue() + rn.doubleValue();
                    } else {
                        left = ln.longValue() + rn.longValue();
                    }
                }
            } else if ("-".equals(op)) {
                if (left instanceof Number ln && right instanceof Number rn) {
                    if (left instanceof Double || right instanceof Double || left instanceof Float || right instanceof Float) {
                        left = ln.doubleValue() - rn.doubleValue();
                    } else {
                        left = ln.longValue() - rn.longValue();
                    }
                }
            }
        }
        return left;
    }

    public Object evalMultiplicativeExpression(YParser.MultiplicativeExpressionContext ctx) {
        if (ctx == null || ctx.unaryExpression().isEmpty()) return null;
        Object left = evalUnaryExpression(ctx.unaryExpression(0));
        for (int i = 1; i < ctx.unaryExpression().size(); i++) {
            Object right = evalUnaryExpression(ctx.unaryExpression(i));
            String op = ctx.getChild(2 * i - 1).getText();
            if (left instanceof Number ln && right instanceof Number rn) {
                boolean isDec = left instanceof Double || right instanceof Double || left instanceof Float || right instanceof Float;
                if ("*".equals(op)) {
                    left = isDec ? (ln.doubleValue() * rn.doubleValue()) : (ln.longValue() * rn.longValue());
                } else if ("/".equals(op)) {
                    if (rn.doubleValue() == 0.0) return null;
                    left = isDec ? (ln.doubleValue() / rn.doubleValue()) : (ln.longValue() / rn.longValue());
                } else if ("%".equals(op)) {
                    if (rn.doubleValue() == 0.0) return null;
                    left = isDec ? (ln.doubleValue() % rn.doubleValue()) : (ln.longValue() % rn.longValue());
                }
            }
        }
        return left;
    }

    public Object evalUnaryExpression(YParser.UnaryExpressionContext ctx) {
        if (ctx == null) return null;
        if (ctx.NOT() != null) {
            Object v = evalUnaryExpression(ctx.unaryExpression());
            return (v instanceof Boolean b) ? !b : null;
        }
        if (ctx.MINUS() != null) {
            Object v = evalUnaryExpression(ctx.unaryExpression());
            if (v instanceof Long l) return -l;
            if (v instanceof Integer i) return -i.longValue();
            if (v instanceof Double d) return -d;
            if (v instanceof Float f) return -f.doubleValue();
            return null;
        }
        return evalPostfixExpression(ctx.postfixExpression());
    }

    public Object evalPostfixExpression(YParser.PostfixExpressionContext ctx) {
        if (ctx == null) return null;
        if (ctx.primaryExpression() != null) return evalPrimaryExpression(ctx.primaryExpression());
        if (ctx.postfixExpression() != null) return evalPostfixExpression(ctx.postfixExpression());
        return null;
    }

    @SuppressWarnings("unchecked")
    public Object evalPrimaryExpression(YParser.PrimaryExpressionContext ctx) {
        if (ctx == null) return null;
        if (ctx.literal() != null) return evalLiteral(ctx.literal());
        if (ctx.LPAREN() != null && ctx.expression() != null && ctx.ID() == null) {
            return evalExpression(ctx.expression());
        }
        if (ctx.ID() != null && ctx.LPAREN() != null) {
            String name = ctx.ID().getText();
            List<Object> args = new java.util.ArrayList<>();
            if (ctx.argumentList() != null && ctx.argumentList().expression() != null) {
                for (YParser.ExpressionContext argExpr : ctx.argumentList().expression()) {
                    args.add(evalExpression(argExpr));
                }
            }
            return visitor.executeFunctionCall(name, args);
        }
        if (ctx.target() != null) {
            String id = ctx.target().ID(0).getText();
            Symbol s = visitor.getSymbolTable().lookup(id);
            if (s == null) return null;

            if (ctx.target().DOT().isEmpty() && ctx.target().LBRACK().isEmpty()) {
                return s.value;
            }
            if (ctx.target().DOT().isEmpty() && !ctx.target().LBRACK().isEmpty()) {
                if (s.value instanceof List<?> list) {
                    Object idxObj = evalExpression(ctx.target().expression(0));
                    if (idxObj instanceof Number num) {
                        int idx = num.intValue();
                        if (idx >= 0 && idx < list.size()) {
                            return list.get(idx);
                        }
                    }
                }
            }
            if (!ctx.target().DOT().isEmpty() && ctx.target().LBRACK().isEmpty()) {
                if (s.value instanceof Map<?, ?> map) {
                    return map.get(ctx.target().ID(1).getText());
                }
            }
        }
        return null;
    }

    public Object evalLiteral(YParser.LiteralContext ctx) {
        if (ctx == null) return null;
        if (ctx.NUMBER() != null) {
            try {
                return Long.parseLong(ctx.NUMBER().getText());
            } catch (Exception e) {
                return null;
            }
        }
        if (ctx.DECIMAL() != null) {
            try {
                return Double.parseDouble(ctx.DECIMAL().getText());
            } catch (Exception e) {
                return null;
            }
        }
        if (ctx.STRING() != null) {
            String t = ctx.STRING().getText();
            if (t.length() >= 2 && ((t.startsWith("\"") && t.endsWith("\"")) || (t.startsWith("'") && t.endsWith("'")))) {
                return t.substring(1, t.length() - 1);
            }
            return t;
        }
        if (ctx.CHAR() != null) {
            String t = ctx.CHAR().getText();
            if (t.length() >= 3 && t.startsWith("'") && t.endsWith("'")) {
                return t.substring(1, 2);
            }
            return t;
        }
        if (ctx.VERDADERO() != null) return Boolean.TRUE;
        if (ctx.FALSO() != null) return Boolean.FALSE;
        return null;
    }

    public String toDisplayString(Object val) {
        if (val == null) return "null";
        if (val instanceof Double d) {
            if (d == d.longValue()) return String.valueOf(d.longValue());
            return d.toString();
        }
        return val.toString();
    }
}
