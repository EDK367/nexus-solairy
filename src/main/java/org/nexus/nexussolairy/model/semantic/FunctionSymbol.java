package org.nexus.nexussolairy.model.semantic;

import org.nexus.nexussolairy.model.enums.DataType;
import org.nexus.nexussolairy.model.enums.LanguageType;
import org.nexus.nexussolairy.model.enums.ScopeKind;
import org.nexus.nexussolairy.model.enums.SymbolKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FunctionSymbol extends Symbol {

    private final List<VariableSymbol> params = new ArrayList<>();
    private Type returnSemanticType;

    public FunctionSymbol(String name, Type returnType, LanguageType language) {
        this(name, returnType, language, 0, 0);
    }

    public FunctionSymbol(String name, Type returnType, LanguageType language, int line, int column) {
        super(name, returnType != null ? returnType.getDataType() : DataType.VOID, SymbolKind.FUNCTION, ScopeKind.GLOBAL, language, null, line, column, new ArrayList<>(), returnType != null ? returnType.getDataType() : DataType.VOID, null, null, null);
        this.returnSemanticType = returnType;
    }

    public void addParam(VariableSymbol param) {
        if (param != null) {
            params.add(param);
            if (paramTypes != null) {
                paramTypes.add(param.getType());
            }
        }
    }

    public List<VariableSymbol> getParams() {
        return Collections.unmodifiableList(params);
    }

    @Override
    public Type getSemanticType() {
        return returnSemanticType != null ? returnSemanticType : super.getSemanticType();
    }

    public void setSemanticType(Type returnSemanticType) {
        this.returnSemanticType = returnSemanticType;
    }
}
