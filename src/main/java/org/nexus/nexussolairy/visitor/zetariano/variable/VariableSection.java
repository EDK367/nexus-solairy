package org.nexus.nexussolairy.visitor.zetariano.variable;

import org.nexus.nexussolairy.ZetarianoParser;
import org.nexus.nexussolairy.model.enums.DataType;
import org.nexus.nexussolairy.model.enums.TypeErrorSemantic;
import org.nexus.nexussolairy.model.semantic.Type;
import org.nexus.nexussolairy.model.semantic.TypeChecker;
import org.nexus.nexussolairy.model.semantic.VariableSymbol;
import org.nexus.nexussolairy.visitor.zetariano.ZetarianoVisitorImpl;

public class VariableSection {
    private final ZetarianoVisitorImpl visitor;

    public VariableSection(ZetarianoVisitorImpl visitor) {
        this.visitor = visitor;
    }

    public Type getType(ZetarianoParser.TypeContext ctx) {
        if (ctx == null) return Type.ERROR;
        if (ctx.INT() != null) return Type.INT;
        if (ctx.DOUBLE() != null) return Type.DOUBLE;
        if (ctx.CHAR_TYPE() != null) return Type.CHAR_T;
        if (ctx.BOOLEAN() != null) return Type.BOOLEAN;
        if (ctx.STRING_TYPE() != null) return Type.STRING_T;
        if (ctx.ID() != null) {
            String text = ctx.ID().getText();
            if ("string".equalsIgnoreCase(text)) return Type.STRING_T;
            if ("int".equalsIgnoreCase(text)) return Type.INT;
            if ("double".equalsIgnoreCase(text)) return Type.DOUBLE;
            if ("char".equalsIgnoreCase(text)) return Type.CHAR_T;
            if ("boolean".equalsIgnoreCase(text)) return Type.BOOLEAN;
            return new Type(text);
        }
        return Type.ERROR;
    }

    public DataType visitType(ZetarianoParser.TypeContext ctx) {
        Type t = getType(ctx);
        return t != null ? t.getDataType() : DataType.ERROR;
    }

    public VariableSymbol processParam(ZetarianoParser.ParamContext ctx) {
        Type t = getType(ctx.type());
        String name = ctx.ID().getText();
        Object defVal = visitor.getExpressionEval().getDefaultValue(t);
        VariableSymbol vs = new VariableSymbol(name, t, defVal, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
        if (ctx.LBRACK() != null) {
            vs.setArray(true);
        }
        return vs;
    }

    public DataType visitVarDecl(ZetarianoParser.VarDeclContext ctx) {
        if (ctx == null) return DataType.VOID;
        Type t = getType(ctx.type());
        String name = ctx.ID().getText();
        boolean isArray = ctx.LBRACK() != null && !ctx.LBRACK().isEmpty();

        if (visitor.getSymbolTable().getCurrentScope().lookupLocal(name) != null) {
            visitor.reportError(ctx, TypeErrorSemantic.REDECLARACION, "Variable '" + name + "' ya declarada");
            return DataType.ERROR;
        }

        Object value = null;
        if (ctx.expression() != null) {
            Type init = visitor.getExpressionDelegate().visitExpression(ctx.expression());
            if (!TypeChecker.isAssignable(t, init)) {
                visitor.reportError(ctx, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Inicializacion invalida");
            }
            value = visitor.getExpressionEval().evalExpression(ctx.expression());
        } else {
            value = visitor.getExpressionEval().getDefaultValue(t);
        }

        VariableSymbol vs = new VariableSymbol(name, t, value, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
        vs.setArray(isArray);
        visitor.getSymbolTable().getCurrentScope().define(vs);
        return DataType.VOID;
    }
}
