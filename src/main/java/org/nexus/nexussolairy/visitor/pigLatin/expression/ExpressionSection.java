package org.nexus.nexussolairy.visitor.pigLatin.expression;

import org.nexus.nexussolairy.PigLatinParser;
import org.nexus.nexussolairy.model.enums.DataType;
import org.nexus.nexussolairy.model.enums.SymbolKind;
import org.nexus.nexussolairy.model.enums.TypeErrorSemantic;
import org.nexus.nexussolairy.model.semantic.ClassSymbol;
import org.nexus.nexussolairy.model.semantic.FunctionSymbol;
import org.nexus.nexussolairy.model.semantic.StructInfo;
import org.nexus.nexussolairy.model.semantic.Symbol;
import org.nexus.nexussolairy.visitor.VisitorContext;
import org.nexus.nexussolairy.model.semantic.TypeChecker;

import java.util.List;

public class ExpressionSection {
    private final VisitorContext visitor;

    public ExpressionSection(VisitorContext visitor) {
        this.visitor = visitor;
    }

    public DataType visitExpression(PigLatinParser.ExpressionContext ctx) {
        if (ctx == null) return DataType.ERROR;
        return visitor.visit(ctx.orExpression());
    }

    public DataType visitOrExpression(PigLatinParser.OrExpressionContext ctx) {
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

    public DataType visitAndExpression(PigLatinParser.AndExpressionContext ctx) {
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

    public DataType visitRelationalExpression(PigLatinParser.RelationalExpressionContext ctx) {
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

    public DataType visitAdditiveExpression(PigLatinParser.AdditiveExpressionContext ctx) {
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

    public DataType visitMultiplicativeExpression(PigLatinParser.MultiplicativeExpressionContext ctx) {
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

    public DataType visitUnaryExpression(PigLatinParser.UnaryExpressionContext ctx) {
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
            DataType res = TypeChecker.getResultType(ctx.getChild(0).getText(), t);
            if (res == DataType.ERROR) {
                visitor.reportError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), TypeErrorSemantic.NOT_COMPATIBLE, "Operador unario requiere operando numerico");
            }
            return res;
        }
        return visitor.visit(ctx.postfixExpression());
    }

    public DataType visitPostfixExpression(PigLatinParser.PostfixExpressionContext ctx) {
        if (ctx == null) return DataType.ERROR;
        if (ctx.primaryExpression() != null) {
            return visitor.visit(ctx.primaryExpression());
        }
        if (ctx.postfixExpression() != null) {
            DataType t = visitor.visit(ctx.postfixExpression());
            int line = ctx.getStart().getLine();
            int col = ctx.getStart().getCharPositionInLine();

            if (ctx.DOT() != null && ctx.LPAREN() != null) {
                String method = ctx.ID().getText();
                String targetName = ctx.postfixExpression().getText();
                Symbol s = visitor.getSymbolTable().lookup(targetName);
                String className = (s != null && s.structTypeName != null) ? s.structTypeName : targetName;
                ClassSymbol cls = visitor.getSymbolTable().lookupClass(className);
                if (cls == null) {
                    Symbol cs = visitor.getSymbolTable().getGlobalScope().resolve(className);
                    if (cs instanceof ClassSymbol c) cls = c;
                }
                if (cls == null) {
                    visitor.reportError(line, col, TypeErrorSemantic.NOT_STRUCT, "Variable '" + targetName + "' no es una clase.");
                    return DataType.ERROR;
                }
                List<Symbol> ms = cls.resolveMethod(method);
                if (ms == null || ms.isEmpty()) {
                    visitor.reportError(line, col, TypeErrorSemantic.FUNCTION_NOT_FOUND, "Metodo '" + method + "' no existe en la clase '" + className + "'.");
                    return DataType.ERROR;
                }
                int argCount = (ctx.argumentList() != null && ctx.argumentList().expression() != null) ? ctx.argumentList().expression().size() : 0;
                FunctionSymbol match = null;
                for (Symbol mSym : ms) {
                    if (mSym instanceof FunctionSymbol fs && fs.getParams() != null && fs.getParams().size() == argCount) {
                        match = fs;
                        break;
                    }
                }
                if (match == null && !ms.isEmpty() && ms.get(0) instanceof FunctionSymbol fs) {
                    match = fs;
                }
                if (match == null) {
                    visitor.reportError(line, col, TypeErrorSemantic.ARGUMENT_COUNT_MISMATCH, "Sobrecarga no encontrada para '" + method + "' con " + argCount + " argumentos.");
                    return DataType.ERROR;
                }
                if (ctx.argumentList() != null) visitor.visit(ctx.argumentList());
                return match.returnType != null ? match.returnType : (match.type != null ? match.type : DataType.VOID);
            }

            if (ctx.DOT() != null) {
                String field = ctx.ID().getText();
                String targetName = ctx.postfixExpression().getText();
                Symbol s = visitor.getSymbolTable().lookup(targetName);
                if (s != null) {
                    return resolveFieldType(s, field, line, col);
                }
                return DataType.ERROR;
            }

            if (ctx.LBRACK() != null) {
                DataType idx = visitor.visit(ctx.expression());
                if (idx != DataType.NUMERUS && idx != DataType.ERROR) {
                    visitor.reportError(line, col, TypeErrorSemantic.ARRAY_INDEX_ERROR, "Indice debe ser 'numerus'.");
                }
                return t;
            }

            if (ctx.INC() != null || ctx.DEC() != null) {
                if (t != DataType.NUMERUS && t != DataType.DECIMALIS) {
                    visitor.reportError(line, col, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Incremento/Decremento solo en tipos numericos.");
                }
            }
            return t;
        }
        return DataType.ERROR;
    }

    public DataType visitPrimaryExpression(PigLatinParser.PrimaryExpressionContext ctx) {
        if (ctx == null) return DataType.ERROR;
        if (ctx.literal() != null) return visitor.visit(ctx.literal());
        if (ctx.structLiteral() != null) return visitor.visit(ctx.structLiteral());

        if (ctx.LPAREN() != null && ctx.ID().isEmpty()) {
            return visitor.visit(ctx.expression());
        }

        if (ctx.ID().size() == 1 && ctx.LBRACK() == null && ctx.DOT() == null && ctx.LPAREN() == null) {
            String name = ctx.ID(0).getText();
            Symbol s = visitor.getSymbolTable().lookup(name);
            if (s == null) {
                visitor.reportError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), TypeErrorSemantic.UNDECLARED, "Variable '" + name + "' no declarada.");
                return DataType.ERROR;
            }
            return s.type;
        }

        if (ctx.ID().size() == 1 && ctx.LBRACK() != null && ctx.DOT() == null && ctx.LPAREN() == null) {
            String name = ctx.ID(0).getText();
            Symbol s = visitor.getSymbolTable().lookup(name);
            if (s == null) {
                visitor.reportError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), TypeErrorSemantic.UNDECLARED, "Variable '" + name + "' no declarada.");
                return DataType.ERROR;
            }
            if (s.kind != SymbolKind.ARRAY && s.elementType == null) {
                visitor.reportError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), TypeErrorSemantic.NOT_ARRAY, "'" + name + "' no es un arreglo.");
            }
            DataType idx = visitor.visit(ctx.expression());
            if (idx != DataType.NUMERUS && idx != DataType.ERROR) {
                visitor.reportError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), TypeErrorSemantic.ARRAY_INDEX_ERROR, "Indice debe ser 'numerus'.");
            }
            return s.elementType != null ? s.elementType : s.type;
        }

        if (ctx.ID().size() == 2 && ctx.LPAREN() == null && ctx.LBRACK() == null) {
            String var = ctx.ID(0).getText();
            String field = ctx.ID(1).getText();
            Symbol s = visitor.getSymbolTable().lookup(var);
            if (s == null) {
                visitor.reportError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), TypeErrorSemantic.UNDECLARED, "Variable '" + var + "' no declarada.");
                return DataType.ERROR;
            }
            return resolveFieldType(s, field, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
        }

        if (ctx.ID().size() == 2 && ctx.LBRACK() != null && ctx.LPAREN() == null) {
            String var = ctx.ID(0).getText();
            String field = ctx.ID(1).getText();
            Symbol s = visitor.getSymbolTable().lookup(var);
            if (s == null) {
                visitor.reportError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), TypeErrorSemantic.UNDECLARED, "Variable '" + var + "' no declarada.");
                return DataType.ERROR;
            }
            DataType ft = resolveFieldType(s, field, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
            DataType idx = visitor.visit(ctx.expression());
            if (idx != DataType.NUMERUS && idx != DataType.ERROR) {
                visitor.reportError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), TypeErrorSemantic.ARRAY_INDEX_ERROR, "Indice debe ser 'numerus'.");
            }
            return ft;
        }

        if (ctx.ID().size() == 1 && ctx.LPAREN() != null && ctx.DOT() == null) {
            String name = ctx.ID(0).getText();
            Symbol s = visitor.getSymbolTable().lookup(name);
            if (s == null) {
                visitor.reportError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), TypeErrorSemantic.FUNCTION_NOT_FOUND, "Funcion '" + name + "' no declarada.");
                return DataType.ERROR;
            }
            if (s.kind != SymbolKind.FUNCTION) {
                visitor.reportError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), TypeErrorSemantic.FUNCTION_NOT_FOUND, "'" + name + "' no es una funcion.");
                return DataType.ERROR;
            }
            int argCount = (ctx.argumentList() != null && ctx.argumentList().expression() != null) ? ctx.argumentList().expression().size() : 0;
            int expected = -1;
            if (s instanceof FunctionSymbol fs && fs.getParams() != null) {
                expected = fs.getParams().size();
            } else if (s.paramTypes != null) {
                expected = s.paramTypes.size();
            }
            if (expected != -1 && expected != argCount) {
                visitor.reportError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), TypeErrorSemantic.ARGUMENT_COUNT_MISMATCH, "Funcion '" + name + "' esperaba " + expected + " argumentos pero se recibieron " + argCount + ".");
                return DataType.ERROR;
            }
            if (ctx.argumentList() != null) visitor.visit(ctx.argumentList());
            return s.returnType != null ? s.returnType : (s.type != null ? s.type : DataType.VOID);
        }

        if (ctx.ID().size() == 2 && ctx.LPAREN() != null) {
            String var = ctx.ID(0).getText();
            String method = ctx.ID(1).getText();
            Symbol s = visitor.getSymbolTable().lookup(var);
            if (s == null) {
                visitor.reportError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), TypeErrorSemantic.UNDECLARED, "Variable '" + var + "' no declarada.");
                return DataType.ERROR;
            }
            String structName = s.structTypeName != null ? s.structTypeName : (s.type != null ? s.type.name().toLowerCase() : "");
            ClassSymbol cls = visitor.getSymbolTable().lookupClass(structName);
            if (cls == null) {
                Symbol cs = visitor.getSymbolTable().getGlobalScope().resolve(structName);
                if (cs instanceof ClassSymbol c) cls = c;
            }
            if (cls == null) {
                visitor.reportError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), TypeErrorSemantic.NOT_STRUCT, "Variable '" + var + "' no es una clase.");
                return DataType.ERROR;
            }
            List<Symbol> ms = cls.resolveMethod(method);
            if (ms == null || ms.isEmpty()) {
                visitor.reportError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), TypeErrorSemantic.FUNCTION_NOT_FOUND, "Metodo '" + method + "' no existe en la clase '" + structName + "'.");
                return DataType.ERROR;
            }
            int argCount = (ctx.argumentList() != null && ctx.argumentList().expression() != null) ? ctx.argumentList().expression().size() : 0;
            FunctionSymbol match = null;
            for (Symbol mSym : ms) {
                if (mSym instanceof FunctionSymbol fs && fs.getParams() != null && fs.getParams().size() == argCount) {
                    match = fs;
                    break;
                }
            }
            if (match == null && !ms.isEmpty() && ms.get(0) instanceof FunctionSymbol fs && fs.getParams() == null && argCount == 0) {
                match = fs;
            }
            if (match == null) {
                visitor.reportError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), TypeErrorSemantic.ARGUMENT_COUNT_MISMATCH, "Sobrecarga no encontrada para '" + method + "' con " + argCount + " argumentos en la clase '" + structName + "'.");
                return DataType.ERROR;
            }
            if (ctx.argumentList() != null) visitor.visit(ctx.argumentList());
            return match.returnType != null ? match.returnType : (match.type != null ? match.type : DataType.VOID);
        }

        return DataType.ERROR;
    }

    private DataType resolveFieldType(Symbol s, String field, int line, int col) {
        String structName = s.structTypeName != null ? s.structTypeName : s.type.name().toLowerCase();
        ClassSymbol cls = visitor.getSymbolTable().lookupClass(structName);
        if (cls != null) {
            Symbol f = cls.resolveField(field);
            if (f == null) {
                visitor.reportError(line, col, TypeErrorSemantic.ATTRIBUTE_NOT_FOUND, "Campo '" + field + "' no existe en la clase '" + structName + "'.");
                return DataType.ERROR;
            }
            return f.getType();
        }
        StructInfo info = visitor.getSymbolTable().lookupStruct(structName);
        if (info == null) {
            visitor.reportError(line, col, TypeErrorSemantic.NOT_STRUCT, "Tipo '" + structName + "' no es una estructura.");
            return DataType.ERROR;
        }
        if (!info.hasField(field)) {
            visitor.reportError(line, col, TypeErrorSemantic.ATTRIBUTE_NOT_FOUND, "Campo '" + field + "' no existe en '" + structName + "'.");
            return DataType.ERROR;
        }
        return info.getFieldType(field);
    }

    public DataType visitLiteral(PigLatinParser.LiteralContext ctx) {
        if (ctx.NUMBER() != null) return DataType.NUMERUS;
        if (ctx.DECIMAL() != null) return DataType.DECIMALIS;
        if (ctx.STRING() != null) return DataType.TEXTUM;
        if (ctx.CHAR() != null) return DataType.LITTERA;
        if (ctx.VERUM() != null || ctx.FALSUS() != null) return DataType.BOOLEAN;
        return DataType.ERROR;
    }

    public DataType visitStructLiteral(PigLatinParser.StructLiteralContext ctx) {
        if (ctx.expressionList() != null) visitor.visit(ctx.expressionList());
        return DataType.STRUCT;
    }

    public DataType visitArgumentList(PigLatinParser.ArgumentListContext ctx) {
        for (PigLatinParser.ExpressionContext e : ctx.expression()) visitor.visit(e);
        return DataType.VOID;
    }

    public DataType visitExpressionList(PigLatinParser.ExpressionListContext ctx) {
        for (PigLatinParser.ExpressionContext e : ctx.expression()) visitor.visit(e);
        return DataType.VOID;
    }

    public DataType visitArrayInit(PigLatinParser.ArrayInitContext ctx) {
        if (ctx.expressionList() != null) visitor.visit(ctx.expressionList());
        return DataType.VOID;
    }
}
