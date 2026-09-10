package org.nexus.nexussolairy.model.semantic;

import org.nexus.nexussolairy.model.enums.DataType;
import org.nexus.nexussolairy.model.enums.ScopeKind;
import org.nexus.nexussolairy.model.enums.SymbolKind;

public class VariableSymbol extends Symbol {

    private boolean isArray;
    private Type semanticType;

    public VariableSymbol(String name, Type type) {
        this(name, type, null, 0, 0);
    }

    public VariableSymbol(String name, Type type, int line, int column) {
        this(name, type, null, line, column);
    }

    public VariableSymbol(String name, Type type, Object value, int line, int column) {
        super(name, type != null ? type.getDataType() : DataType.ERROR, SymbolKind.VARIABLE, ScopeKind.LOCAL, value, line, column, null, null, null, null, (type != null && type.getDataType() == DataType.STRUCT) ? type.getName() : null);
        this.semanticType = type;
        this.value = value;
    }

    public VariableSymbol(String name, Type type, SymbolKind kind, ScopeKind scope, int line, int column) {
        this(name, type, kind, scope, null, line, column);
    }

    public VariableSymbol(String name, Type type, SymbolKind kind, ScopeKind scope, Object value, int line, int column) {
        super(name, type != null ? type.getDataType() : DataType.ERROR, kind, scope, value, line, column, null, null, null, null, (type != null && type.getDataType() == DataType.STRUCT) ? type.getName() : null);
        this.semanticType = type;
        this.value = value;
    }

    public boolean isArray() {
        return isArray;
    }

    public void setArray(boolean array) {
        this.isArray = array;
    }

    @Override
    public SymbolKind getKind() {
        return isArray ? SymbolKind.ARRAY : super.getKind();
    }

    @Override
    public Type getSemanticType() {
        return semanticType != null ? semanticType : super.getSemanticType();
    }

    public void setSemanticType(Type semanticType) {
        this.semanticType = semanticType;
    }
}
