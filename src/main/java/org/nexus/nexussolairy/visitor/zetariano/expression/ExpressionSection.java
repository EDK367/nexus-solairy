package org.nexus.nexussolairy.visitor.zetariano.expression;

import org.antlr.v4.runtime.ParserRuleContext;
import org.nexus.nexussolairy.ZetarianoParser;
import org.nexus.nexussolairy.model.enums.TypeErrorSemantic;
import org.nexus.nexussolairy.model.semantic.ClassSymbol;
import org.nexus.nexussolairy.model.semantic.FunctionSymbol;
import org.nexus.nexussolairy.model.semantic.Symbol;
import org.nexus.nexussolairy.model.semantic.Type;
import org.nexus.nexussolairy.model.semantic.TypeChecker;
import org.nexus.nexussolairy.visitor.zetariano.ZetarianoVisitorImpl;

import java.util.ArrayList;
import java.util.List;

public class ExpressionSection {
    private final ZetarianoVisitorImpl visitor;

    public ExpressionSection(ZetarianoVisitorImpl visitor) {
        this.visitor = visitor;
    }

    public Type visitExpression(ZetarianoParser.ExpressionContext ctx) {
        if (ctx == null) return Type.ERROR;
        return visitConditionalExpr(ctx.conditionalExpr());
    }

    public Type visitConditionalExpr(ZetarianoParser.ConditionalExprContext ctx) {
        if (ctx == null) return Type.ERROR;
        Type cond = visitOrExpr(ctx.orExpr());
        if (ctx.QUESTION() != null) {
            if (!TypeChecker.isBool(cond) && cond != Type.ERROR) {
                visitor.reportError(ctx, TypeErrorSemantic.NOT_BOOLEAN, "Ternario requiere booleano");
            }
            Type t1 = visitExpression(ctx.expression(0));
            Type t2 = visitExpression(ctx.expression(1));
            if (!t1.equals(t2) && !TypeChecker.isAssignable(t1, t2) && !TypeChecker.isAssignable(t2, t1) && t1 != Type.ERROR && t2 != Type.ERROR) {
                visitor.reportError(ctx, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Tipos del ternario incompatibles");
            }
            return t1;
        }
        return cond;
    }

    public Type visitOrExpr(ZetarianoParser.OrExprContext ctx) {
        if (ctx == null || ctx.andExpr().isEmpty()) return Type.ERROR;
        Type t = visitAndExpr(ctx.andExpr(0));
        for (int i = 1; i < ctx.andExpr().size(); i++) {
            t = TypeChecker.getResultType(t, "||", visitAndExpr(ctx.andExpr(i)));
        }
        return t;
    }

    public Type visitAndExpr(ZetarianoParser.AndExprContext ctx) {
        if (ctx == null || ctx.eqExpr().isEmpty()) return Type.ERROR;
        Type t = visitEqExpr(ctx.eqExpr(0));
        for (int i = 1; i < ctx.eqExpr().size(); i++) {
            t = TypeChecker.getResultType(t, "&&", visitEqExpr(ctx.eqExpr(i)));
        }
        return t;
    }

    public Type visitEqExpr(ZetarianoParser.EqExprContext ctx) {
        if (ctx == null || ctx.relExpr().isEmpty()) return Type.ERROR;
        Type t = visitRelExpr(ctx.relExpr(0));
        for (int i = 1; i < ctx.relExpr().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            t = TypeChecker.getResultType(t, op, visitRelExpr(ctx.relExpr(i)));
        }
        return t;
    }

    public Type visitRelExpr(ZetarianoParser.RelExprContext ctx) {
        if (ctx == null || ctx.addExpr().isEmpty()) return Type.ERROR;
        Type t = visitAddExpr(ctx.addExpr(0));
        for (int i = 1; i < ctx.addExpr().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            t = TypeChecker.getResultType(t, op, visitAddExpr(ctx.addExpr(i)));
        }
        return t;
    }

    public Type visitAddExpr(ZetarianoParser.AddExprContext ctx) {
        if (ctx == null || ctx.mulExpr().isEmpty()) return Type.ERROR;
        Type t = visitMulExpr(ctx.mulExpr(0));
        for (int i = 1; i < ctx.mulExpr().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            t = TypeChecker.getResultType(t, op, visitMulExpr(ctx.mulExpr(i)));
        }
        return t;
    }

    public Type visitMulExpr(ZetarianoParser.MulExprContext ctx) {
        if (ctx == null || ctx.unaryExpr().isEmpty()) return Type.ERROR;
        Type t = visitUnaryExpr(ctx.unaryExpr(0));
        for (int i = 1; i < ctx.unaryExpr().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            t = TypeChecker.getResultType(t, op, visitUnaryExpr(ctx.unaryExpr(i)));
        }
        return t;
    }

    public Type visitUnaryExpr(ZetarianoParser.UnaryExprContext ctx) {
        if (ctx == null) return Type.ERROR;
        if (ctx.NOT() != null) {
            return TypeChecker.getResultType("!", visitUnaryExpr(ctx.unaryExpr()));
        }
        if (ctx.MINUS() != null || ctx.PLUS() != null) {
            return TypeChecker.getResultType(ctx.getChild(0).getText(), visitUnaryExpr(ctx.unaryExpr()));
        }
        if (ctx.INC() != null || ctx.DEC() != null) {
            Type t = visitUnaryExpr(ctx.unaryExpr());
            if (!TypeChecker.isNumeric(t) && t != Type.ERROR) {
                visitor.reportError(ctx, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Inc/Dec unario solo en numericos");
            }
            return t;
        }
        return visitPostfixExpr(ctx.postfixExpr());
    }

    public Type visitPostfixExpr(ZetarianoParser.PostfixExprContext ctx) {
        if (ctx == null) return Type.ERROR;
        if (ctx.primaryExpr() != null) {
            return visitPrimaryExpr(ctx.primaryExpr());
        }
        Type base = visitPostfixExpr(ctx.postfixExpr());
        if (ctx.DOT() != null && ctx.LPAREN() != null) {
            String method = ctx.ID().getText();
            return resolveMethodCall(base, method, ctx.argList(), ctx);
        }
        if (ctx.DOT() != null) {
            String field = ctx.ID().getText();
            return resolveFieldAccess(base, field, ctx);
        }
        if (ctx.LBRACK() != null) {
            Type idx = visitExpression(ctx.expression());
            if (!TypeChecker.isInt(idx) && idx != Type.ERROR) {
                visitor.reportError(ctx, TypeErrorSemantic.ARRAY_INDEX_ERROR, "Indice debe ser entero");
            }
            return base;
        }
        if (ctx.INC() != null || ctx.DEC() != null) {
            if (!TypeChecker.isNumeric(base) && base != Type.ERROR) {
                visitor.reportError(ctx, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Inc/Dec solo en numericos");
            }
            return base;
        }
        return base;
    }

    public Type visitPrimaryExpr(ZetarianoParser.PrimaryExprContext ctx) {
        if (ctx == null) return Type.ERROR;
        if (ctx.literal() != null) return visitLiteral(ctx.literal());
        if (ctx.NULL() != null) return new Type("null");
        if (ctx.READ() != null) {
            if (visitor.getInputProvider() != null) {
                visitor.getInputProvider().readLine();
            }
            return Type.STRING_T;
        }
        if (ctx.LPAREN() != null && ctx.expression() != null) return visitExpression(ctx.expression());
        if (ctx.newExpr() != null) return visitNewExpr(ctx.newExpr());
        if (ctx.arrayInit() != null) return visitArrayInit(ctx.arrayInit());

        String id = ctx.ID().getText();

        if (ctx.LPAREN() != null) {
            ClassSymbol currentClass = visitor.getCurrentClass();
            if (currentClass != null) {
                List<Symbol> ms = currentClass.resolveMethod(id);
                if (ms != null && !ms.isEmpty()) {
                    int argCount = (ctx.argList() != null && ctx.argList().expression() != null) ? ctx.argList().expression().size() : 0;
                    List<Type> argTypes = new ArrayList<>();
                    if (ctx.argList() != null && ctx.argList().expression() != null) {
                        for (var e : ctx.argList().expression()) {
                            argTypes.add(visitExpression(e));
                        }
                    }
                    FunctionSymbol match = null;
                    for (Symbol mSym : ms) {
                        if (mSym instanceof FunctionSymbol m && m.getParams().size() == argCount) {
                            boolean typesMatch = true;
                            for (int i = 0; i < argCount; i++) {
                                if (!TypeChecker.isAssignable(m.getParams().get(i).getSemanticType(), argTypes.get(i))) {
                                    typesMatch = false;
                                    break;
                                }
                            }
                            if (typesMatch) {
                                match = m;
                                break;
                            }
                        }
                    }
                    if (match == null) {
                        for (Symbol mSym : ms) {
                            if (mSym instanceof FunctionSymbol m && m.getParams().size() == argCount) {
                                match = m;
                                break;
                            }
                        }
                    }
                    if (match == null) {
                        visitor.reportError(ctx, TypeErrorSemantic.ARGUMENT_COUNT_MISMATCH, "Sobrecarga no encontrada para '" + id + "' con " + argCount + " argumentos");
                        return Type.ERROR;
                    }
                    return match.getSemanticType();
                }
            }
            Symbol s = visitor.resolveSymbol(id);
            if (s instanceof FunctionSymbol f) {
                int argCount = (ctx.argList() != null && ctx.argList().expression() != null) ? ctx.argList().expression().size() : 0;
                if (f.getParams().size() != argCount) {
                    visitor.reportError(ctx, TypeErrorSemantic.ARGUMENT_COUNT_MISMATCH, "Args incorrectos para '" + id + "'");
                }
                if (ctx.argList() != null && ctx.argList().expression() != null) {
                    for (var e : ctx.argList().expression()) {
                        visitExpression(e);
                    }
                }
                return f.getSemanticType();
            }
            visitor.reportError(ctx, TypeErrorSemantic.FUNCTION_NOT_FOUND, "Metodo o funcion '" + id + "' no declarado");
            return Type.ERROR;
        }

        Symbol s = visitor.resolveSymbol(id);
        if (s == null) {
            visitor.reportError(ctx, TypeErrorSemantic.UNDECLARED, "Variable '" + id + "' no declarada");
            return Type.ERROR;
        }
        return s.getSemanticType();
    }

    public Type visitNewExpr(ZetarianoParser.NewExprContext ctx) {
        if (ctx == null) return Type.ERROR;
        Type t = visitor.getVariableDelegate().getType(ctx.type());
        if (ctx.LPAREN() != null) {
            Symbol cls = visitor.getSymbolTable().getGlobalScope().resolve(t.getName());
            if (cls == null) {
                cls = visitor.getSymbolTable().lookupClass(t.getName());
            }
            if (!(cls instanceof ClassSymbol classSymbol)) {
                visitor.reportError(ctx, TypeErrorSemantic.NOT_STRUCT, "No es una clase: " + t.getName());
                return Type.ERROR;
            }
            int argCount = (ctx.argList() != null && ctx.argList().expression() != null) ? ctx.argList().expression().size() : 0;
            List<Type> argTypes = new ArrayList<>();
            if (ctx.argList() != null && ctx.argList().expression() != null) {
                for (var e : ctx.argList().expression()) {
                    argTypes.add(visitExpression(e));
                }
            }
            boolean found = false;
            for (Symbol cSym : classSymbol.getConstructors()) {
                if (cSym instanceof FunctionSymbol c && c.getParams().size() == argCount) {
                    boolean match = true;
                    for (int i = 0; i < argCount; i++) {
                        if (!TypeChecker.isAssignable(c.getParams().get(i).getSemanticType(), argTypes.get(i))) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                for (Symbol cSym : classSymbol.getConstructors()) {
                    if (cSym instanceof FunctionSymbol c && c.getParams().size() == argCount) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                visitor.reportError(ctx, TypeErrorSemantic.WRONG_NUMBER_OF_PARAMS, "Constructor no encontrado para " + argCount + " args en clase " + t.getName());
            }
            return t;
        } else {
            if (ctx.expression() != null) {
                for (ZetarianoParser.ExpressionContext e : ctx.expression()) {
                    Type sz = visitExpression(e);
                    if (!TypeChecker.isInt(sz) && sz != Type.ERROR) {
                        visitor.reportError(ctx, TypeErrorSemantic.ARRAY_INDEX_ERROR, "Tamano de array debe ser entero");
                    }
                }
            }
            return t;
        }
    }

    public Type visitArrayInit(ZetarianoParser.ArrayInitContext ctx) {
        if (ctx == null) return Type.VOID;
        Type elemType = null;
        if (ctx.argList() != null && ctx.argList().expression() != null) {
            for (ZetarianoParser.ExpressionContext e : ctx.argList().expression()) {
                Type t = visitExpression(e);
                if (elemType == null) {
                    elemType = t;
                }
            }
        }
        return elemType != null ? elemType : Type.VOID;
    }

    public Type visitArgList(ZetarianoParser.ArgListContext ctx) {
        if (ctx == null) return Type.VOID;
        if (ctx.expression() != null) {
            for (ZetarianoParser.ExpressionContext e : ctx.expression()) {
                visitExpression(e);
            }
        }
        return Type.VOID;
    }

    public Type visitLiteral(ZetarianoParser.LiteralContext ctx) {
        if (ctx == null) return Type.ERROR;
        if (ctx.NUMBER() != null) return Type.INT;
        if (ctx.DECIMAL() != null) return Type.DOUBLE;
        if (ctx.STRING() != null) return Type.STRING_T;
        if (ctx.CHAR() != null) return Type.CHAR_T;
        if (ctx.TRUE() != null || ctx.FALSE() != null) return Type.BOOLEAN;
        return Type.ERROR;
    }

    public Type resolveFieldAccess(Type objType, String field, ParserRuleContext ctx) {
        if (objType == null || objType == Type.ERROR) return Type.ERROR;
        Symbol s = visitor.getSymbolTable().getGlobalScope().resolve(objType.getName());
        if (s == null) {
            s = visitor.getSymbolTable().lookupClass(objType.getName());
        }
        if (!(s instanceof ClassSymbol cls)) {
            visitor.reportError(ctx, TypeErrorSemantic.NOT_STRUCT, "'" + objType.getName() + "' no es una clase");
            return Type.ERROR;
        }
        Symbol f = cls.resolveField(field);
        if (f == null) {
            visitor.reportError(ctx, TypeErrorSemantic.ATTRIBUTE_NOT_FOUND, "Campo '" + field + "' no existe");
            return Type.ERROR;
        }
        return f.getSemanticType();
    }

    public Type resolveMethodCall(Type objType, String method, ZetarianoParser.ArgListContext argsCtx, ParserRuleContext ctx) {
        if (objType == null || objType == Type.ERROR) return Type.ERROR;
        Symbol s = visitor.getSymbolTable().getGlobalScope().resolve(objType.getName());
        if (s == null) {
            s = visitor.getSymbolTable().lookupClass(objType.getName());
        }
        if (!(s instanceof ClassSymbol cls)) {
            visitor.reportError(ctx, TypeErrorSemantic.NOT_STRUCT, "'" + objType.getName() + "' no es una clase");
            return Type.ERROR;
        }
        List<Symbol> ms = cls.resolveMethod(method);
        if (ms == null || ms.isEmpty()) {
            visitor.reportError(ctx, TypeErrorSemantic.FUNCTION_NOT_FOUND, "Metodo '" + method + "' no existe en '" + objType.getName() + "'");
            return Type.ERROR;
        }
        int argCount = (argsCtx != null && argsCtx.expression() != null) ? argsCtx.expression().size() : 0;
        List<Type> argTypes = new ArrayList<>();
        if (argsCtx != null && argsCtx.expression() != null) {
            for (ZetarianoParser.ExpressionContext e : argsCtx.expression()) {
                argTypes.add(visitExpression(e));
            }
        }
        FunctionSymbol match = null;
        for (Symbol mSym : ms) {
            if (mSym instanceof FunctionSymbol m && m.getParams().size() == argCount) {
                boolean typesMatch = true;
                for (int i = 0; i < argCount; i++) {
                    if (!TypeChecker.isAssignable(m.getParams().get(i).getSemanticType(), argTypes.get(i))) {
                        typesMatch = false;
                        break;
                    }
                }
                if (typesMatch) {
                    match = m;
                    break;
                }
            }
        }
        if (match == null) {
            for (Symbol mSym : ms) {
                if (mSym instanceof FunctionSymbol m && m.getParams().size() == argCount) {
                    match = m;
                    break;
                }
            }
        }
        if (match == null) {
            visitor.reportError(ctx, TypeErrorSemantic.ARGUMENT_COUNT_MISMATCH, "No hay sobrecarga de '" + method + "' con " + argCount + " argumentos");
            return Type.ERROR;
        }
        return match.getSemanticType();
    }
}
