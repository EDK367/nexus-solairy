package org.nexus.nexussolairy.visitor.yLanguage.expression;

import org.nexus.nexussolairy.YParser;
import org.nexus.nexussolairy.model.enums.DataType;
import org.nexus.nexussolairy.model.enums.SymbolKind;
import org.nexus.nexussolairy.model.enums.TypeErrorSemantic;
import org.nexus.nexussolairy.model.semantic.Symbol;
import org.nexus.nexussolairy.model.semantic.TypeChecker;
import org.nexus.nexussolairy.visitor.VisitorContext;
import org.nexus.nexussolairy.visitor.yLanguage.YVisitorImpl;

import java.util.Collections;
import java.util.List;

public class ExpressionSection {
    private final VisitorContext visitor;

    public ExpressionSection(VisitorContext visitor) {
        this.visitor = visitor;
    }

    public DataType visitExpression(YParser.ExpressionContext ctx) {
        if (ctx == null) return DataType.ERROR;
        return visitor.visit(ctx.orExpression());
    }

    public DataType visitOrExpression(YParser.OrExpressionContext ctx) {
        if (ctx == null || ctx.andExpression().isEmpty()) return DataType.ERROR;
        DataType t = visitor.visit(ctx.andExpression(0));
        for (int i = 1; i < ctx.andExpression().size(); i++) {
            DataType r = visitor.visit(ctx.andExpression(i));
            t = TypeChecker.getResultType(t, "||", r);
            if (t == DataType.ERROR) {
                visitor.reportError(ctx.andExpression(i).getStart().getLine(), ctx.andExpression(i).getStart().getCharPositionInLine(), TypeErrorSemantic.NOT_COMPATIBLE, "Operacion logica || invalida");
            }
        }
        return t;
    }

    public DataType visitAndExpression(YParser.AndExpressionContext ctx) {
        if (ctx == null || ctx.relationalExpression().isEmpty()) return DataType.ERROR;
        DataType t = visitor.visit(ctx.relationalExpression(0));
        for (int i = 1; i < ctx.relationalExpression().size(); i++) {
            DataType r = visitor.visit(ctx.relationalExpression(i));
            t = TypeChecker.getResultType(t, "&&", r);
            if (t == DataType.ERROR) {
                visitor.reportError(ctx.relationalExpression(i).getStart().getLine(), ctx.relationalExpression(i).getStart().getCharPositionInLine(), TypeErrorSemantic.NOT_COMPATIBLE, "Operacion logica && invalida");
            }
        }
        return t;
    }

    public DataType visitRelationalExpression(YParser.RelationalExpressionContext ctx) {
        if (ctx == null || ctx.additiveExpression().isEmpty()) return DataType.ERROR;
        DataType t = visitor.visit(ctx.additiveExpression(0));
        for (int i = 1; i < ctx.additiveExpression().size(); i++) {
            DataType r = visitor.visit(ctx.additiveExpression(i));
            String op = ctx.getChild(2 * i - 1).getText();
            t = TypeChecker.getResultType(t, op, r);
            if (t == DataType.ERROR) {
                visitor.reportError(ctx.additiveExpression(i).getStart().getLine(), ctx.additiveExpression(i).getStart().getCharPositionInLine(), TypeErrorSemantic.NOT_COMPATIBLE, "Operacion relacional " + op + " invalida");
            }
        }
        return t;
    }

    public DataType visitAdditiveExpression(YParser.AdditiveExpressionContext ctx) {
        if (ctx == null || ctx.multiplicativeExpression().isEmpty()) return DataType.ERROR;
        DataType t = visitor.visit(ctx.multiplicativeExpression(0));
        for (int i = 1; i < ctx.multiplicativeExpression().size(); i++) {
            DataType r = visitor.visit(ctx.multiplicativeExpression(i));
            String op = ctx.getChild(2 * i - 1).getText();
            t = TypeChecker.getResultType(t, op, r);
            if (t == DataType.ERROR) {
                visitor.reportError(ctx.multiplicativeExpression(i).getStart().getLine(), ctx.multiplicativeExpression(i).getStart().getCharPositionInLine(), TypeErrorSemantic.NOT_COMPATIBLE, "Operacion aritmetica " + op + " invalida");
            }
        }
        return t;
    }

    public DataType visitMultiplicativeExpression(YParser.MultiplicativeExpressionContext ctx) {
        if (ctx == null || ctx.unaryExpression().isEmpty()) return DataType.ERROR;
        DataType t = visitor.visit(ctx.unaryExpression(0));
        for (int i = 1; i < ctx.unaryExpression().size(); i++) {
            DataType r = visitor.visit(ctx.unaryExpression(i));
            String op = ctx.getChild(2 * i - 1).getText();
            t = TypeChecker.getResultType(t, op, r);
            if (t == DataType.ERROR) {
                visitor.reportError(ctx.unaryExpression(i).getStart().getLine(), ctx.unaryExpression(i).getStart().getCharPositionInLine(), TypeErrorSemantic.NOT_COMPATIBLE, "Operacion aritmetica " + op + " invalida");
            }
        }
        return t;
    }

    public DataType visitUnaryExpression(YParser.UnaryExpressionContext ctx) {
        if (ctx == null) return DataType.ERROR;
        if (ctx.NOT() != null) {
            DataType t = visitor.visit(ctx.unaryExpression());
            DataType res = TypeChecker.getResultType("!", t);
            if (res == DataType.ERROR) {
                visitor.reportError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), TypeErrorSemantic.NOT_COMPATIBLE, "Operador ! requiere operando booleano");
            }
            return res;
        }
        if (ctx.MINUS() != null || (ctx.getChildCount() > 1 && "+".equals(ctx.getChild(0).getText()))) {
            DataType t = visitor.visit(ctx.unaryExpression());
            String op = ctx.getChild(0).getText();
            DataType res = TypeChecker.getResultType(op, t);
            if (res == DataType.ERROR) {
                visitor.reportError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), TypeErrorSemantic.NOT_COMPATIBLE, "Operador unario requiere operando numerico");
            }
            return res;
        }
        return visitor.visit(ctx.postfixExpression());
    }

    public DataType visitPostfixExpression(YParser.PostfixExpressionContext ctx) {
        if (ctx == null) return DataType.ERROR;
        DataType t = visitor.visit(ctx.primaryExpression());
        if (ctx.INC() != null || ctx.DEC() != null) {
            if (!TypeChecker.isNumeric(t) && t != DataType.ERROR) {
                visitor.reportError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), TypeErrorSemantic.INCOMPATIBLE_TYPES, "Incremento/Decremento solo en tipos numericos");
            }
        }
        return t;
    }

    public DataType visitPrimaryExpression(YParser.PrimaryExpressionContext ctx) {
        if (ctx == null) return DataType.ERROR;
        int line = ctx.getStart().getLine();
        int col = ctx.getStart().getCharPositionInLine();

        if (ctx.literal() != null) return visitor.visit(ctx.literal());
        if (ctx.LEER() != null) return DataType.CADENA;
        if (ctx.target() != null) {
            return ((YVisitorImpl) visitor).getAssignmentDelegate().resolveTarget(ctx.target());
        }
        if (ctx.LPAREN() != null && ctx.expression() != null && ctx.ID() == null) {
            return visitor.visit(ctx.expression());
        }
        if (ctx.structLiteral() != null) return visitor.visit(ctx.structLiteral());

        if (ctx.ID() != null && ctx.LPAREN() != null) {
            String name = ctx.ID().getText();
            Symbol s = visitor.getSymbolTable().lookup(name);

            if (s == null) {
                visitor.reportError(line, col, TypeErrorSemantic.FUNCTION_NOT_FOUND, "Funcion '" + name + "' no declarada");
                return DataType.ERROR;
            }
            if (s.kind != SymbolKind.FUNCTION) {
                visitor.reportError(line, col, TypeErrorSemantic.FUNCTION_ERROR, "'" + name + "' no es una funcion");
                return DataType.ERROR;
            }

            List<DataType> expected = s.paramTypes;
            List<YParser.ExpressionContext> args = (ctx.argumentList() != null) ? ctx.argumentList().expression() : Collections.emptyList();

            if (expected != null) {
                if (args.size() != expected.size()) {
                    visitor.reportError(line, col, TypeErrorSemantic.WRONG_NUMBER_OF_PARAMS, "Cantidad de argumentos invalida para '" + name + "'. Esperado: " + expected.size() + ", obtenido: " + args.size());
                } else {
                    for (int i = 0; i < args.size(); i++) {
                        DataType argT = visitor.visit(args.get(i));
                        DataType expT = expected.get(i);
                        if (!TypeChecker.isAssignable(expT, argT)) {
                            visitor.reportError(args.get(i).getStart().getLine(), args.get(i).getStart().getCharPositionInLine(), TypeErrorSemantic.INCOMPATIBLE_TYPES, "Tipo de argumento incompatible para parametro " + (i + 1) + ". Esperado: " + expT + ", obtenido: " + argT);
                        }
                    }
                }
            }

            if (ctx.argumentList() != null) {
                visitor.visit(ctx.argumentList());
            }

            return s.returnType != null ? s.returnType : s.type;
        }

        return DataType.ERROR;
    }

    public DataType visitLiteral(YParser.LiteralContext ctx) {
        if (ctx == null) return DataType.ERROR;
        if (ctx.NUMBER() != null) return DataType.ENTERO;
        if (ctx.DECIMAL() != null) return DataType.FLOTANTE;
        if (ctx.STRING() != null) return DataType.CADENA;
        if (ctx.CHAR() != null) return DataType.CARACTER;
        if (ctx.VERDADERO() != null || ctx.FALSO() != null) return DataType.BOOL;
        return DataType.ERROR;
    }

    public DataType visitStructLiteral(YParser.StructLiteralContext ctx) {
        if (ctx == null) return DataType.STRUCT;
        if (ctx.expressionList() != null) {
            visitor.visit(ctx.expressionList());
        }
        return DataType.STRUCT;
    }

    public DataType visitArrayInit(YParser.ArrayInitContext ctx) {
        if (ctx == null) return DataType.VOID;
        if (ctx.expressionList() != null) {
            visitor.visit(ctx.expressionList());
        }
        return DataType.VOID;
    }

    public DataType visitArgumentList(YParser.ArgumentListContext ctx) {
        if (ctx == null) return DataType.VOID;
        for (YParser.ExpressionContext e : ctx.expression()) {
            visitor.visit(e);
        }
        return DataType.VOID;
    }

    public DataType visitExpressionList(YParser.ExpressionListContext ctx) {
        if (ctx == null) return DataType.VOID;
        for (YParser.ExpressionContext e : ctx.expression()) {
            visitor.visit(e);
        }
        return DataType.VOID;
    }
}
