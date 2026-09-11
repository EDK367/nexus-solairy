package org.nexus.nexussolairy.visitor.pigLatin.variable;

import org.nexus.nexussolairy.PigLatinParser;
import org.nexus.nexussolairy.model.enums.DataType;
import org.nexus.nexussolairy.model.enums.LanguageType;
import org.nexus.nexussolairy.model.enums.SymbolKind;
import org.nexus.nexussolairy.model.enums.TypeErrorSemantic;
import org.nexus.nexussolairy.model.semantic.ClassSymbol;
import org.nexus.nexussolairy.model.semantic.FunctionSymbol;
import org.nexus.nexussolairy.model.semantic.Symbol;
import org.nexus.nexussolairy.visitor.VisitorContext;
import org.nexus.nexussolairy.model.semantic.TypeChecker;
import org.nexus.nexussolairy.visitor.pigLatin.expression.ExpressionEval;
import org.nexus.nexussolairy.visitor.pigLatin.expression.ExpressionSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VariableSection {
    private final VisitorContext visitor;
    private final ExpressionSection expressionDelegate;
    private final ExpressionEval expressionEval;
    private final TypeChecker checker = new TypeChecker();

    public VariableSection(VisitorContext visitor, ExpressionSection expressionDelegate, ExpressionEval expressionEval) {
        this.visitor = visitor;
        this.expressionDelegate = expressionDelegate;
        this.expressionEval = expressionEval;
    }

    public DataType visitVarSection(PigLatinParser.VarSectionContext ctx) {
        if (ctx == null) return DataType.VOID;
        for (PigLatinParser.VarDeclContext v : ctx.varDecl()) {
            visitor.visit(v);
        }
        return DataType.VOID;
    }

    public DataType getTypeFromContext(PigLatinParser.TypeContext ctx) {
        if (ctx == null) return DataType.ERROR;
        if (ctx.NUMERUS() != null) return DataType.NUMERUS;
        if (ctx.TEXTUM() != null) return DataType.TEXTUM;
        if (ctx.DECIMALIS() != null) return DataType.DECIMALIS;
        if (ctx.LITTERA() != null) return DataType.LITTERA;
        if (ctx.BOOL() != null) return DataType.BOOLEAN;
        if (ctx.ID() != null) {
            String name = ctx.ID().getText();
            if (visitor.getSymbolTable().structExists(name)) {
                return DataType.STRUCT;
            }
            if (visitor.getSymbolTable().classExists(name)) {
                return DataType.CLASS;
            }
            visitor.reportError(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(),
                TypeErrorSemantic.UNDECLARED, 
                "Tipo '" + name + "' no definido. Importa el archivo .y o .z correspondiente.");
            return DataType.STRUCT;
        }
        return DataType.ERROR;
    }

    public DataType visitType(PigLatinParser.TypeContext ctx) {
        return getTypeFromContext(ctx);
    }

    public DataType visitVarDecl(PigLatinParser.VarDeclContext ctx) {
        if (ctx == null) return DataType.VOID;
        int line = ctx.getStart().getLine();
        int col = ctx.getStart().getCharPositionInLine();

        if (ctx.NOVUS() != null) {
            String varName = ctx.ID(0).getText();
            String typeName = ctx.ID(1).getText();

            // buscar struct o clase externa
            boolean tipoExiste = visitor.getSymbolTable().structExists(typeName) || visitor.getSymbolTable().classExists(typeName);
            if (!tipoExiste) {
                visitor.reportError(line, col, TypeErrorSemantic.UNDECLARED, "La estructura/clase '" + typeName + "' no ha sido declarada. " + "Verifique que fue importada desde un archivo .y o .z");
            }

            if (visitor.getSymbolTable().lookupLocal(varName) != null) {
                visitor.reportError(line, col, TypeErrorSemantic.REDECLARACION, "Variable '" + varName + "' ya declarada.");
                return DataType.ERROR;
            }

            List<Object> argValues = new ArrayList<>();
            List<DataType> argTypes = new ArrayList<>();
            if (ctx.argumentList() != null && ctx.argumentList().expression() != null) {
                for (PigLatinParser.ExpressionContext expr : ctx.argumentList().expression()) {
                    DataType argType = visitor.visit(expr);
                    argTypes.add(argType);
                    Object argVal = expressionEval.evalExpression(expr);
                    argValues.add(argVal);
                }
            }

            ClassSymbol cls = visitor.getSymbolTable().lookupClass(typeName);
            if (cls != null) {
                FunctionSymbol matchingCtor = null;
                for (Symbol cSym : cls.getConstructors()) {
                    if (cSym instanceof FunctionSymbol fs && fs.getParams().size() == argValues.size()) {
                        matchingCtor = fs;
                        break;
                    }
                }
                if (!cls.getConstructors().isEmpty() && matchingCtor == null) {
                    visitor.reportError(line, col, TypeErrorSemantic.WRONG_NUMBER_OF_PARAMS, "Constructor no encontrado para " + argValues.size() + " argumentos en la clase '" + typeName + "'.");
                }

                Map<String, Object> obj = new LinkedHashMap<>();
                for (Symbol f : cls.getFields().values()) {
                    obj.put(f.getName(), f.getValue() != null ? f.getValue() : getDefaultValue(f.getType()));
                }

                if (matchingCtor != null) {
                    for (int i = 0; i < matchingCtor.getParams().size(); i++) {
                        String paramName = matchingCtor.getParams().get(i).getName();
                        Object argVal = argValues.get(i);
                        boolean mapped = false;
                        for (String fieldName : obj.keySet()) {
                            if (fieldName.equalsIgnoreCase(paramName) || paramName.toLowerCase().startsWith(fieldName.toLowerCase()) || fieldName.toLowerCase().startsWith(paramName.toLowerCase())) {
                                obj.put(fieldName, argVal);
                                mapped = true;
                                break;
                            }
                        }
                        if (!mapped && i < obj.size()) {
                            List<String> keys = new ArrayList<>(obj.keySet());
                            obj.put(keys.get(i), argVal);
                        }
                    }
                }

                Symbol sym = new Symbol(varName, DataType.CLASS, SymbolKind.VARIABLE, visitor.getSymbolTable().getCurrentScopeKind(), LanguageType.PIG_LATIN, obj, line, col, typeName);
                visitor.getSymbolTable().declare(sym);
            } else {
                Symbol sym = new Symbol(varName, typeName, visitor.getSymbolTable().getCurrentScopeKind(), LanguageType.PIG_LATIN, new HashMap<String, Object>(), line, col);
                visitor.getSymbolTable().declare(sym);
            }
            return DataType.VOID;
        }

        if (ctx.ESTO() != null) {
            String varName = ctx.ID(0).getText();
            DataType t = getTypeFromContext(ctx.type());

            if (visitor.getSymbolTable().lookupLocal(varName) != null) {
                visitor.reportError(line, col, TypeErrorSemantic.REDECLARACION, "Variable '" + varName + "' ya declarada.");
                return DataType.ERROR;
            }

            Object value = null;
            if (ctx.expression() != null) {
                DataType init = visitor.visit(ctx.expression());
                if (!TypeChecker.isAssignable(t, init)) {
                    visitor.reportError(line, col, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Inicializacion invalida para '" + varName + "'. Esperado: " + t + ", obtenido: " + init);
                }
                value = expressionEval.evalExpression(ctx.expression());
            }

            if (t == DataType.STRUCT) {
                String structTypeName = (ctx.type() != null && ctx.type().ID() != null) ? ctx.type().ID().getText() : null;
                Symbol sym = new Symbol(varName, structTypeName, visitor.getSymbolTable().getCurrentScopeKind(), LanguageType.PIG_LATIN, value, line, col);
                visitor.getSymbolTable().declare(sym);
            } else if (t == DataType.CLASS) {
                String className = (ctx.type() != null && ctx.type().ID() != null) ? ctx.type().ID().getText() : null;
                Symbol sym = new Symbol(varName, DataType.CLASS, SymbolKind.VARIABLE, visitor.getSymbolTable().getCurrentScopeKind(), LanguageType.PIG_LATIN, value, line, col, className);
                visitor.getSymbolTable().declare(sym);
            } else {
                Symbol sym = new Symbol(varName, t, SymbolKind.VARIABLE, visitor.getSymbolTable().getCurrentScopeKind(), LanguageType.PIG_LATIN, value, line, col);
                visitor.getSymbolTable().declare(sym);
            }
            return DataType.VOID;
        }

        if (ctx.SERIES() != null) {
            String varName = ctx.ID(0).getText();
            DataType elementType = getTypeFromContext(ctx.type());

            DataType idx = visitor.visit(ctx.expression());
            if (idx != DataType.NUMERUS && idx != DataType.ERROR) {
                visitor.reportError(line, col, TypeErrorSemantic.INCOMPATIBLE_TYPES, "El tamano del arreglo debe ser 'numerus'.");
            }

            Object sizeObj = expressionEval.evalExpression(ctx.expression());
            Integer size = null;
            if (sizeObj instanceof Long l) size = l.intValue();
            else if (sizeObj instanceof Integer i) size = i;
            else if (sizeObj instanceof Double d) size = d.intValue();

            if (visitor.getSymbolTable().lookupLocal(varName) != null) {
                visitor.reportError(line, col, TypeErrorSemantic.REDECLARACION, "Variable '" + varName + "' ya declarada.");
                return DataType.ERROR;
            }

            List<Object> values = new ArrayList<>();

            if (ctx.arrayInit() != null) {
                var exprList = ctx.arrayInit().expressionList().expression();

                for (var expr : exprList) {
                    DataType actualType = visitor.visit(expr);

                    Object value = expressionEval.evalExpression(expr);

                    values.add(value);

                    if (!checker.checkAssignmentCompatibility(elementType, actualType)) {
                        visitor.reportError(line, col, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Valor incompatibvle con el tipo de arreglo " + elementType);
                    }
                }
            }

            if (values.isEmpty()) {
                while (values.size() < size) {
                    values.add(null);
                }
            }


            Symbol sym = new Symbol(varName, elementType, visitor.getSymbolTable().getCurrentScopeKind(), LanguageType.PIG_LATIN, values, line, col, size);
            visitor.getSymbolTable().declare(sym);
            return DataType.VOID;
        }

        return DataType.VOID;
    }

    private Object getDefaultValue(DataType type) {
        if (type == null) return null;
        return switch (type) {
            case NUMERUS, ENTERO -> 0L;
            case DECIMALIS, FLOTANTE -> 0.0;
            case TEXTUM, CADENA -> "";
            case LITTERA, CARACTER -> ' ';
            case BOOLEAN, BOOL -> false;
            default -> null;
        };
    }
}
