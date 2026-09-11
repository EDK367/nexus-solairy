package org.nexus.nexussolairy.model.semantic;

import org.nexus.nexussolairy.model.enums.DataType;
import org.nexus.nexussolairy.model.enums.LanguageType;
import org.nexus.nexussolairy.model.enums.ScopeKind;
import org.nexus.nexussolairy.model.enums.SymbolKind;

import java.util.List;

public class Symbol {

    public final String name;
    public final DataType type;
    public final SymbolKind kind;
    public final ScopeKind scope;
    public final LanguageType language;
    public Object value; // valor mutable
    public final int line;
    public final int column;

    // funciones
    public final List<DataType> paramTypes;
    public final DataType returnType;

    // arrays
    public final DataType elementType;
    public final Integer arraySize;

    // struct
    public final String structTypeName;

    // constuctor para variables simples o parametros
    public Symbol(String name, DataType type, SymbolKind kind, ScopeKind scope, LanguageType language, Object value, int line, int column) {
        this(name, type, kind, scope, language, value, line, column, null, null, null, null, null);
    }

    // constructor para funciones
    public Symbol(String name, DataType returnType, ScopeKind scope, LanguageType language, Object value, int line, int column, List<DataType> paramTypes) {
        this(name, returnType, SymbolKind.FUNCTION, scope, language, value, line, column, paramTypes, returnType, null, null, null);
    }

    // constructor para arrays
    public Symbol(String name, DataType elementType, ScopeKind scope, LanguageType language, Object value, int line, int column, Integer arraySize) {
        this(name, elementType, SymbolKind.ARRAY, scope, language, value, line, column, null, null, elementType, arraySize, null);
    }

    public Symbol(String name, String structTypeName, ScopeKind scope, LanguageType language, Object value, int line, int column) {
        this(name, DataType.STRUCT, SymbolKind.VARIABLE, scope, language, value, line, column, null, null, null, null, structTypeName);
    }

    public Symbol(String name, DataType type, SymbolKind kind, ScopeKind scope, LanguageType language, Object value, int line, int column, String structTypeName) {
        this(name, type, kind, scope, language, value, line, column, null, null, null, null, structTypeName);
    }

    public Symbol(String name, DataType type, SymbolKind kind, ScopeKind scope, LanguageType language, Object value, int line, int column, List<DataType> paramTypes, DataType returnType, DataType elementType, Integer arraySize, String structTypeName) {
        this.name = name;
        this.type = type;
        this.kind = kind;
        this.scope = scope;
        this.language = language;
        this.value = value;
        this.line = line;
        this.column = column;
        this.paramTypes = paramTypes;
        this.returnType = returnType;
        this.elementType = elementType;
        this.arraySize = arraySize;
        this.structTypeName = structTypeName;
    }

    @Override
    public String toString() {
        return String.format("Nombre = '%s', Jerarquia = %s, Tipo = %s, Ambito = %s, Valor = %s Linea = %d, Columna = %d", name, type, kind, scope, value, line, column);
    }

    public String getName() {
        return name;
    }

    public DataType getType() {
        return type;
    }

    public SymbolKind getKind() {
        return kind;
    }

    public ScopeKind getScope() {
        return scope;
    }

    public LanguageType getLanguage() {
        return language;
    }

    public Object getValue() {
        return value;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public List<DataType> getParamTypes() {
        return paramTypes;
    }

    public DataType getReturnType() {
        return returnType;
    }

    public DataType getElementType() {
        return elementType;
    }

    public Integer getArraySize() {
        return arraySize;
    }


    public String getStructTypeName() { return structTypeName; }
    public Type getSemanticType() {
        if (structTypeName != null && !structTypeName.isEmpty()) {
            return new Type(structTypeName);
        }
        if (type != null) {
            return new Type(type);
        }
        return Type.ERROR;
    }

    // scoped label para saber de que lenguaje pertenece el simbolo 
    public String getScopedLabel() {
        String langSuffix = switch (language) {
            case PIG_LATIN -> "PIG";
            case Y_LANG -> "Y";
            case ZETARIANO -> "Z";
            default -> "";
        };

        return scope.name() + (langSuffix.isEmpty() ? "" : "_" + langSuffix);
    }

    private Object astContext;

    public Object getAstContext() {
        return astContext;
    }

    public void setAstContext(Object astContext) {
        this.astContext = astContext;
    }
}