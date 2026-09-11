package org.nexus.nexussolairy.visitor.yLanguage.variable;

import org.nexus.nexussolairy.YParser;
import org.nexus.nexussolairy.model.enums.DataType;
import org.nexus.nexussolairy.model.enums.LanguageType;
import org.nexus.nexussolairy.model.enums.SymbolKind;
import org.nexus.nexussolairy.model.enums.TypeErrorSemantic;
import org.nexus.nexussolairy.model.semantic.StructInfo;
import org.nexus.nexussolairy.model.semantic.Symbol;
import org.nexus.nexussolairy.model.semantic.TypeChecker;
import org.nexus.nexussolairy.visitor.VisitorContext;
import org.nexus.nexussolairy.visitor.yLanguage.expression.ExpressionEval;
import org.nexus.nexussolairy.visitor.yLanguage.expression.ExpressionSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VariableSection {
    private final VisitorContext visitor;
    private final ExpressionSection expressionDelegate;
    private final ExpressionEval expressionEval;
    private final YStructValidator structValidator;

    public VariableSection(VisitorContext visitor, ExpressionSection expressionDelegate, ExpressionEval expressionEval) {
        this.visitor = visitor;
        this.expressionDelegate = expressionDelegate;
        this.expressionEval = expressionEval;
        this.structValidator = new YStructValidator(visitor);
    }

    public DataType getType(YParser.TypeContext ctx) {
        if (ctx == null) return DataType.ERROR;
        if (ctx.ENTERO() != null) return DataType.ENTERO;
        if (ctx.FLOTANTE() != null) return DataType.FLOTANTE;
        if (ctx.CADENA() != null) return DataType.CADENA;
        if (ctx.CARACTER() != null) return DataType.CARACTER;
        if (ctx.BOOL() != null) return DataType.BOOL;
        if (ctx.ID() != null) {
            String name = ctx.ID().getText();
            if (visitor.getSymbolTable().structExists(name)) {
                return DataType.STRUCT;
            }
            visitor.reportError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), TypeErrorSemantic.UNDECLARED, "Tipo no definido: '" + name + "'");
            return DataType.STRUCT;
        }
        return DataType.ERROR;
    }

    public DataType visitType(YParser.TypeContext ctx) {
        return getType(ctx);
    }

    public DataType visitVarDecl(YParser.VarDeclContext ctx) {
        if (ctx == null) return DataType.VOID;
        int line = ctx.getStart().getLine();
        int col = ctx.getStart().getCharPositionInLine();

        if (ctx.type() != null && ctx.LBRACK().isEmpty()) {
            DataType t = getType(ctx.type());
            String name = ctx.ID(0).getText();

            if (visitor.getSymbolTable().lookupLocal(name) != null) {
                visitor.reportError(line, col, TypeErrorSemantic.REDECLARACION, "Variable '" + name + "' ya declarada");
                return DataType.ERROR;
            }

            Object value = null;
            if (!ctx.expression().isEmpty()) {
                DataType init = visitor.visit(ctx.expression(0));
                if (!TypeChecker.isAssignable(t, init)) {
                    visitor.reportError(line, col, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Inicializacion invalida para '" + name + "'");
                }
                value = expressionEval.evalExpression(ctx.expression(0));
            }

            if (t == DataType.STRUCT) {
                String structTypeName = (ctx.type().ID() != null) ? ctx.type().ID().getText() : null;
                Symbol sym = new Symbol(name, structTypeName, visitor.getSymbolTable().getCurrentScopeKind(), LanguageType.Y_LANG,value, line, col);
                visitor.getSymbolTable().declare(sym);
            } else {
                Symbol sym = new Symbol(name, t, SymbolKind.VARIABLE, visitor.getSymbolTable().getCurrentScopeKind(), LanguageType.Y_LANG, value, line, col);
                visitor.getSymbolTable().declare(sym);
            }
            return DataType.VOID;
        }

        if (ctx.type() != null && !ctx.LBRACK().isEmpty()) {
            DataType t = getType(ctx.type());
            String name = ctx.ID(0).getText();

            if (visitor.getSymbolTable().lookupLocal(name) != null) {
                visitor.reportError(line, col, TypeErrorSemantic.REDECLARACION, "Arreglo '" + name + "' ya declarado");
                return DataType.ERROR;
            }

            Integer size = null;
            for (YParser.ExpressionContext e : ctx.expression()) {
                DataType idx = visitor.visit(e);
                if (!TypeChecker.isInt(idx) && idx != DataType.ERROR) {
                    visitor.reportError(line, col, TypeErrorSemantic.ARRAY_INDEX_ERROR, "Indice de arreglo debe ser entero");
                }
                Object sObj = expressionEval.evalExpression(e);
                if (size == null && sObj instanceof Number num) {
                    size = num.intValue();
                }
            }

            List<Object> values = new ArrayList<>();
            if (ctx.arrayInit() != null) {
                if (ctx.arrayInit().expressionList() != null) {
                    for (YParser.ExpressionContext elemExpr : ctx.arrayInit().expressionList().expression()) {
                        DataType elemType = visitor.visit(elemExpr);
                        if (!TypeChecker.isAssignable(t, elemType)) {
                            visitor.reportError(elemExpr.getStart().getLine(), elemExpr.getStart().getCharPositionInLine(), TypeErrorSemantic.INCOMPATIBLE_TYPES, "Elemento incompatible con arreglo de tipo " + t);
                        }
                        values.add(expressionEval.evalExpression(elemExpr));
                    }
                }
            }

            Symbol sym = new Symbol(name, t, visitor.getSymbolTable().getCurrentScopeKind(), LanguageType.Y_LANG, values, line, col, size);
            visitor.getSymbolTable().declare(sym);
            return DataType.VOID;
        }

        if (ctx.type() == null) {
            String structType = ctx.ID(0).getText();
            String name = ctx.ID(1).getText();

            if (!visitor.getSymbolTable().structExists(structType)) {
                visitor.reportError(line, col, TypeErrorSemantic.UNDECLARED, "Tipo no definido: '" + structType + "'");
            }

            if (visitor.getSymbolTable().lookupLocal(name) != null) {
                visitor.reportError(line, col, TypeErrorSemantic.REDECLARACION, "Variable '" + name + "' ya declarada");
                return DataType.ERROR;
            }

            if (ctx.structLiteral() != null) {
                StructInfo info = visitor.getSymbolTable().lookupStruct(structType);
                if (info != null) {
                    structValidator.validateStructLiteral(info, ctx.structLiteral());
                }
            }

            Symbol sym = new Symbol(name, structType, visitor.getSymbolTable().getCurrentScopeKind(), LanguageType.Y_LANG, new HashMap<String, Object>(), line, col);
            visitor.getSymbolTable().declare(sym);
            return DataType.VOID;
        }

        return DataType.VOID;
    }
}
