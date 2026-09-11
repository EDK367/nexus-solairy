package org.nexus.nexussolairy.visitor.zetariano.expression;

import org.antlr.v4.runtime.tree.ParseTree;
import org.nexus.nexussolairy.ZetarianoParser;
import org.nexus.nexussolairy.model.semantic.ClassSymbol;
import org.nexus.nexussolairy.model.semantic.FunctionSymbol;
import org.nexus.nexussolairy.model.semantic.Symbol;
import org.nexus.nexussolairy.model.semantic.Type;
import org.nexus.nexussolairy.visitor.zetariano.ZetarianoVisitorImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ExpressionEval {
    private final ZetarianoVisitorImpl visitor;

    public ExpressionEval(ZetarianoVisitorImpl visitor) {
        this.visitor = visitor;
    }

    public Object getDefaultValue(Type type) {
        if (type == null) return null;
        String name = type.getName().toLowerCase();
        return switch (name) {
            case "int", "entero", "numerus" -> 0;
            case "double", "decimalis", "flotante", "float" -> 0.0;
            case "boolean", "bool" -> false;
            case "char", "caracter", "littera" -> '\0';
            case "string", "cadena", "textum" -> "";
            default -> null;
        };
    }

    public Object evalExpression(ZetarianoParser.ExpressionContext ctx) {
        if (ctx == null) return null;
        return evalConditionalExpr(ctx.conditionalExpr());
    }

    public Object evalConditionalExpr(ZetarianoParser.ConditionalExprContext ctx) {
        if (ctx == null) return null;
        Object cond = evalOrExpr(ctx.orExpr());
        if (ctx.QUESTION() != null) {
            if (Boolean.TRUE.equals(cond)) {
                return evalExpression(ctx.expression(0));
            } else {
                return evalExpression(ctx.expression(1));
            }
        }
        return cond;
    }

    public Object evalOrExpr(ZetarianoParser.OrExprContext ctx) {
        if (ctx == null || ctx.andExpr().isEmpty()) return null;
        Object left = evalAndExpr(ctx.andExpr(0));
        for (int i = 1; i < ctx.andExpr().size(); i++) {
            Object right = evalAndExpr(ctx.andExpr(i));
            if (left instanceof Boolean lb && right instanceof Boolean rb) {
                left = lb || rb;
            } else {
                return null;
            }
        }
        return left;
    }

    public Object evalAndExpr(ZetarianoParser.AndExprContext ctx) {
        if (ctx == null || ctx.eqExpr().isEmpty()) return null;
        Object left = evalEqExpr(ctx.eqExpr(0));
        for (int i = 1; i < ctx.eqExpr().size(); i++) {
            Object right = evalEqExpr(ctx.eqExpr(i));
            if (left instanceof Boolean lb && right instanceof Boolean rb) {
                left = lb && rb;
            } else {
                return null;
            }
        }
        return left;
    }

    public Object evalEqExpr(ZetarianoParser.EqExprContext ctx) {
        if (ctx == null || ctx.relExpr().isEmpty()) return null;
        Object left = evalRelExpr(ctx.relExpr(0));
        for (int i = 1; i < ctx.relExpr().size(); i++) {
            Object right = evalRelExpr(ctx.relExpr(i));
            String op = ctx.getChild(2 * i - 1).getText();
            left = applyRelational(left, right, op);
        }
        return left;
    }

    public Object evalRelExpr(ZetarianoParser.RelExprContext ctx) {
        if (ctx == null || ctx.addExpr().isEmpty()) return null;
        Object left = evalAddExpr(ctx.addExpr(0));
        for (int i = 1; i < ctx.addExpr().size(); i++) {
            Object right = evalAddExpr(ctx.addExpr(i));
            String op = ctx.getChild(2 * i - 1).getText();
            left = applyRelational(left, right, op);
        }
        return left;
    }

    private Object applyRelational(Object left, Object right, String op) {
        if (left == null && right == null) {
            return "==".equals(op);
        }
        if (left == null || right == null) {
            return "!=".equals(op);
        }
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
        if (left instanceof Character lc && right instanceof Character rc) {
            return switch (op) {
                case "==" -> lc.equals(rc);
                case "!=" -> !lc.equals(rc);
                case "<" -> lc < rc;
                case ">" -> lc > rc;
                case "<=" -> lc <= rc;
                case ">=" -> lc >= rc;
                default -> null;
            };
        }
        return switch (op) {
            case "==" -> Objects.equals(left, right);
            case "!=" -> !Objects.equals(left, right);
            default -> null;
        };
    }

    public Object evalAddExpr(ZetarianoParser.AddExprContext ctx) {
        if (ctx == null || ctx.mulExpr().isEmpty()) return null;
        Object left = evalMulExpr(ctx.mulExpr(0));
        for (int i = 1; i < ctx.mulExpr().size(); i++) {
            Object right = evalMulExpr(ctx.mulExpr(i));
            String op = ctx.getChild(2 * i - 1).getText();
            left = applyBinaryOp(left, op, right);
        }
        return left;
    }

    public Object evalMulExpr(ZetarianoParser.MulExprContext ctx) {
        if (ctx == null || ctx.unaryExpr().isEmpty()) return null;
        Object left = evalUnaryExpr(ctx.unaryExpr(0));
        for (int i = 1; i < ctx.unaryExpr().size(); i++) {
            Object right = evalUnaryExpr(ctx.unaryExpr(i));
            String op = ctx.getChild(2 * i - 1).getText();
            left = applyBinaryOp(left, op, right);
        }
        return left;
    }

    public Object applyBinaryOp(Object left, String op, Object right) {
        if ("+".equals(op)) {
            if (left instanceof Map || right instanceof Map) {
                return null;
            }
            if (left instanceof String || right instanceof String) {
                return toDisplayString(left) + toDisplayString(right);
            }
        }
        if (left instanceof Number ln && right instanceof Number rn) {
            if (left instanceof Double || right instanceof Double || left instanceof Float || right instanceof Float) {
                double ld = ln.doubleValue();
                double rd = rn.doubleValue();
                return switch (op) {
                    case "+" -> ld + rd;
                    case "-" -> ld - rd;
                    case "*" -> ld * rd;
                    case "/" -> rd != 0.0 ? ld / rd : null;
                    case "%" -> rd != 0.0 ? ld % rd : null;
                    default -> null;
                };
            }
            if (left instanceof Integer && right instanceof Integer) {
                int li = ln.intValue();
                int ri = rn.intValue();
                return switch (op) {
                    case "+" -> li + ri;
                    case "-" -> li - ri;
                    case "*" -> li * ri;
                    case "/" -> ri != 0 ? li / ri : null;
                    case "%" -> ri != 0 ? li % ri : null;
                    default -> null;
                };
            }
            long ll = ln.longValue();
            long rl = rn.longValue();
            return switch (op) {
                case "+" -> ll + rl;
                case "-" -> ll - rl;
                case "*" -> ll * rl;
                case "/" -> rl != 0L ? ll / rl : null;
                case "%" -> rl != 0L ? ll % rl : null;
                default -> null;
            };
        }
        return null;
    }

    public Object evalUnaryExpr(ZetarianoParser.UnaryExprContext ctx) {
        if (ctx == null) return null;
        if (ctx.NOT() != null) {
            Object v = evalUnaryExpr(ctx.unaryExpr());
            return (v instanceof Boolean b) ? !b : null;
        }
        if (ctx.MINUS() != null) {
            Object v = evalUnaryExpr(ctx.unaryExpr());
            if (v instanceof Double d) return -d;
            if (v instanceof Integer i) return -i;
            if (v instanceof Number n) return -n.longValue();
            return null;
        }
        if (ctx.PLUS() != null) {
            return evalUnaryExpr(ctx.unaryExpr());
        }
        if (ctx.INC() != null) {
            Object v = evalUnaryExpr(ctx.unaryExpr());
            if (v instanceof Double d) return d + 1.0;
            if (v instanceof Integer i) return i + 1;
            if (v instanceof Number n) return n.longValue() + 1L;
            return null;
        }
        if (ctx.DEC() != null) {
            Object v = evalUnaryExpr(ctx.unaryExpr());
            if (v instanceof Double d) return d - 1.0;
            if (v instanceof Integer i) return i - 1;
            if (v instanceof Number n) return n.longValue() - 1L;
            return null;
        }
        return evalPostfixExpr(ctx.postfixExpr());
    }

    public Object evalPostfixExpr(ZetarianoParser.PostfixExprContext ctx) {
        if (ctx == null) return null;
        if (ctx.primaryExpr() != null) {
            return evalPrimaryExpr(ctx.primaryExpr());
        }
        Object base = evalPostfixExpr(ctx.postfixExpr());
        if (ctx.DOT() != null && ctx.LPAREN() != null) {
            String method = ctx.ID().getText();
            if (base instanceof Map map) {
                return map.get(method);
            }
            return null;
        }
        if (ctx.DOT() != null) {
            String field = ctx.ID().getText();
            if (base instanceof Map map) {
                return map.get(field);
            }
            return null;
        }
        if (ctx.LBRACK() != null) {
            if (base instanceof List list) {
                Object idxObj = evalExpression(ctx.expression());
                if (idxObj instanceof Number num) {
                    int idx = num.intValue();
                    if (idx >= 0 && idx < list.size()) {
                        return list.get(idx);
                    }
                }
            }
            return null;
        }
        if (ctx.INC() != null) {
            if (base instanceof Double d) return d + 1.0;
            if (base instanceof Integer i) return i + 1;
            if (base instanceof Number n) return n.longValue() + 1L;
            return base;
        }
        if (ctx.DEC() != null) {
            if (base instanceof Double d) return d - 1.0;
            if (base instanceof Integer i) return i - 1;
            if (base instanceof Number n) return n.longValue() - 1L;
            return base;
        }
        return base;
    }

    public Object evalPrimaryExpr(ZetarianoParser.PrimaryExprContext ctx) {
        if (ctx == null) return null;
        if (ctx.literal() != null) return evalLiteral(ctx.literal());
        if (ctx.NULL() != null) return null;
        if (ctx.READ() != null) {
            if (visitor.getInputProvider() != null) {
                return visitor.getInputProvider().readLine();
            }
            return "";
        }
        if (ctx.LPAREN() != null && ctx.expression() != null) {
            return evalExpression(ctx.expression());
        }
        if (ctx.newExpr() != null) {
            return evalNewExpr(ctx.newExpr());
        }
        if (ctx.arrayInit() != null) {
            return evalArrayInit(ctx.arrayInit());
        }

        String id = ctx.ID().getText();
        if (ctx.LPAREN() != null) {
            return null;
        }

        Symbol s = visitor.resolveSymbol(id);
        if (s != null) {
            return s.getValue();
        }
        return null;
    }

    public Object evalNewExpr(ZetarianoParser.NewExprContext ctx) {
        if (ctx == null) return null;
        Type t = visitor.getVariableDelegate().getType(ctx.type());
        if (ctx.LPAREN() != null) {
            Map<String, Object> obj = new HashMap<>();
            obj.put("__class__", t.getName());
            ClassSymbol cls = visitor.getSymbolTable().lookupClass(t.getName());
            if (cls == null) {
                Symbol s = visitor.getSymbolTable().getGlobalScope().resolve(t.getName());
                if (s instanceof ClassSymbol cs) cls = cs;
            }
            if (cls != null) {
                for (Symbol f : cls.getFields().values()) {
                    obj.put(f.getName(), f.value != null ? f.value : getDefaultValue(f.getSemanticType()));
                }
            }
            if (ctx.argList() != null && ctx.argList().expression() != null && cls != null) {
                int argCount = ctx.argList().expression().size();
                for (Symbol cSym : cls.getConstructors()) {
                    if (cSym instanceof FunctionSymbol c && c.getParams().size() == argCount) {
                        for (int i = 0; i < argCount; i++) {
                            Object argVal = evalExpression(ctx.argList().expression(i));
                            String paramName = c.getParams().get(i).getName();
                            for (String fieldName : obj.keySet()) {
                                if (fieldName.equalsIgnoreCase(paramName) || fieldName.startsWith(paramName) || paramName.startsWith(fieldName)) {
                                    obj.put(fieldName, argVal);
                                }
                            }
                        }
                    }
                }
            }
            return obj;
        } else {
            List<Object> list = new ArrayList<>();
            if (ctx.expression() != null && !ctx.expression().isEmpty()) {
                Object szObj = evalExpression(ctx.expression(0));
                int sz = 0;
                if (szObj instanceof Number n) sz = n.intValue();
                Object defVal = getDefaultValue(t);
                for (int i = 0; i < sz; i++) {
                    list.add(defVal);
                }
            }
            return list;
        }
    }

    public Object evalArrayInit(ZetarianoParser.ArrayInitContext ctx) {
        List<Object> list = new ArrayList<>();
        if (ctx.argList() != null && ctx.argList().expression() != null) {
            for (ZetarianoParser.ExpressionContext e : ctx.argList().expression()) {
                list.add(evalExpression(e));
            }
        }
        return list;
    }

    public Object evalLiteral(ZetarianoParser.LiteralContext ctx) {
        if (ctx == null) return null;
        if (ctx.NUMBER() != null) {
            try {
                return Integer.parseInt(ctx.NUMBER().getText());
            } catch (Exception e) {
                try {
                    return Long.parseLong(ctx.NUMBER().getText());
                } catch (Exception ex) {
                    return 0;
                }
            }
        }
        if (ctx.DECIMAL() != null) {
            try {
                return Double.parseDouble(ctx.DECIMAL().getText());
            } catch (Exception e) {
                return 0.0;
            }
        }
        if (ctx.STRING() != null) {
            String s = ctx.STRING().getText();
            if (s.length() >= 2 && ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")))) {
                return s.substring(1, s.length() - 1);
            }
            return s;
        }
        if (ctx.CHAR() != null) {
            String c = ctx.CHAR().getText();
            if (c.length() >= 3 && c.startsWith("'") && c.endsWith("'")) {
                return c.substring(1, 2);
            }
            return c;
        }
        if (ctx.TRUE() != null) return Boolean.TRUE;
        if (ctx.FALSE() != null) return Boolean.FALSE;
        return null;
    }

    public String evaluateExprText(ZetarianoParser.ExpressionContext ctx) {
        if (ctx == null) return "";
        Object val = evalExpression(ctx);
        if (val != null) {
            return toDisplayString(val);
        }
        return evalExprObject(ctx);
    }

    public String evalExprObject(ParseTree node) {
        if (node == null) return "";
        if (node instanceof ZetarianoParser.LiteralContext lit) {
            if (lit.STRING() != null) {
                String s = lit.STRING().getText();
                if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
                    return s.substring(1, s.length() - 1);
                }
                return s;
            }
            return lit.getText();
        }
        if (node instanceof ZetarianoParser.PrimaryExprContext pri) {
            if (pri.literal() != null) return evalExprObject(pri.literal());
            if (pri.ID() != null && pri.LPAREN() == null) {
                Symbol s = visitor.resolveSymbol(pri.ID().getText());
                if (s != null && s.getValue() != null) {
                    return String.valueOf(s.getValue());
                }
                return pri.ID().getText();
            }
            if (pri.expression() != null) return evalExprObject(pri.expression());
        }
        if (node instanceof ZetarianoParser.AddExprContext add) {
            if (add.mulExpr().size() == 1) {
                return evalExprObject(add.mulExpr(0));
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < add.mulExpr().size(); i++) {
                sb.append(evalExprObject(add.mulExpr(i)));
            }
            return sb.toString();
        }
        if (node instanceof ZetarianoParser.ExpressionContext expr) {
            return evalExprObject(expr.conditionalExpr());
        }
        if (node instanceof ZetarianoParser.ConditionalExprContext cond) {
            return evalExprObject(cond.orExpr());
        }
        if (node instanceof ZetarianoParser.OrExprContext or) {
            return evalExprObject(or.andExpr(0));
        }
        if (node instanceof ZetarianoParser.AndExprContext and) {
            return evalExprObject(and.eqExpr(0));
        }
        if (node instanceof ZetarianoParser.EqExprContext eq) {
            return evalExprObject(eq.relExpr(0));
        }
        if (node instanceof ZetarianoParser.RelExprContext rel) {
            return evalExprObject(rel.addExpr(0));
        }
        if (node instanceof ZetarianoParser.MulExprContext mul) {
            return evalExprObject(mul.unaryExpr(0));
        }
        if (node instanceof ZetarianoParser.UnaryExprContext un) {
            if (un.postfixExpr() != null) return evalExprObject(un.postfixExpr());
        }
        if (node instanceof ZetarianoParser.PostfixExprContext post) {
            if (post.primaryExpr() != null) return evalExprObject(post.primaryExpr());
        }
        return node.getText();
    }

    public String toDisplayString(Object val) {
        if (val == null) return "null";
        if (val instanceof Map map) {
            if (map.containsKey("__class__")) {
                Map<String, Object> copy = new HashMap<>(map);
                Object className = copy.remove("__class__");
                return className + copy.toString();
            }
            return map.toString();
        }
        return String.valueOf(val);
    }
}
