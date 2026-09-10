package org.nexus.nexussolairy.model.semantic;

import org.nexus.nexussolairy.model.enums.ScopeKind;
import org.nexus.nexussolairy.model.enums.TypeErrorSemantic;

import java.util.*;

public class SymbolTable {
    private final Scope globalScope;
    private Scope currentScope;
    private final List<SemanticError> errorList = new ArrayList<>();

    private final List<Scope> closedScopes = new ArrayList<>();

    // mapa para contener los struct
    private final Map<String, StructInfo> structRegistry = new HashMap<>();

    public SymbolTable() {
        this.globalScope = new Scope("global", null);
        this.currentScope = globalScope;
    }

    public SymbolTable(Scope globalScope) {
        this.globalScope = globalScope != null ? globalScope : new Scope("global", null);
        this.currentScope = this.globalScope;
    }

    public void pushScope(String name) {
        currentScope = new Scope(name, currentScope);
    }

    // funcion para cambiar el tipo actual de scope
    public void pushScope(Scope scope) {
        if (scope != null) {
            currentScope = scope;
        }
    }

    public void popScope() {
        if (currentScope != globalScope) {
            closedScopes.add(currentScope);
            currentScope = currentScope.getParent();
        }
    }

    public Scope getCurrentScope() {
        return currentScope;
    }

    public Scope getGlobalScope() {
        return globalScope;
    }

    public boolean hasErrors() {
        return !errorList.isEmpty();
    }

    public List<SemanticError> getErrors() {
        return new ArrayList<>(errorList);
    }

    public boolean declare(Symbol symbol) {
        boolean ok = currentScope.declare(symbol);
        if (!ok) {
            addError(symbol.line, symbol.column, TypeErrorSemantic.REDECLARACION, "Se volvio a redeclarar una variable");
        }
        return ok;
    }

    public Symbol lookup(String name) {
        return currentScope.lookup(name);
    }

    public ScopeKind getCurrentScopeKind() {
        if (currentScope == globalScope) {
            return ScopeKind.GLOBAL;
        }

        return ScopeKind.LOCAL;
    }

    public Symbol lookupLocal(String name) {
        return currentScope.lookupLocal(name);
    }

    // actualizacion del valor de la varible
    public void updateValue(String name, Object value) {
        Symbol sym = lookup(name);
        if (sym != null) {
            sym.value = value;
        }
    }


    public void addError(int line, int column, TypeErrorSemantic type, String message) {
        for (SemanticError existing : errorList) {
            if (existing.getLine() == line && existing.getColumn() == column && existing.getType().equals(type.toString())) {
                return;
            }
        }

        // verificar funcionamineot
        SemanticError error = new SemanticError(line, column, type.name(), message);
        errorList.add(error);
    }

    public List<Scope> getClosedScopes() {
        return Collections.unmodifiableList(closedScopes);
    }

    public List<Symbol> getAllVariablesLog() {
        List<Symbol> all = new ArrayList<>();

        all.addAll(globalScope.getSymbols().values());

        for (Scope scope : closedScopes) {
            for (Symbol sym : scope.getSymbols().values()) {
                boolean found = false;
                for (Symbol existing : all) {
                    if (existing.name.equals(sym.name)
                            && existing.line == sym.line
                            && existing.scope == sym.scope) {
                        existing.value = sym.value;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    all.add(sym);
                }
            }
        }

        return Collections.unmodifiableList(all);
    }

    public List<Symbol> getLocalVariablesLog() {
        List<Symbol> locals = new ArrayList<>();
        for (Scope scope : closedScopes) {
            for (Symbol sym : scope.getSymbols().values()) {
                boolean found = false;
                for (Symbol existing : locals) {
                    if (existing.name.equals(sym.name)
                            && existing.line == sym.line
                            && existing.scope == sym.scope) {
                        existing.value = sym.value;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    locals.add(sym);
                }
            }
        }
        return Collections.unmodifiableList(locals);
    }

    // registros para structs
    public void registerStruct(StructInfo structInfo) {
        structRegistry.put(structInfo.getName(), structInfo);
    }

    public StructInfo lookupStruct(String name) {
        return structRegistry.get(name);
    }

    public boolean structExists(String name) {
        return structRegistry.containsKey(name);
    }

    public Map<String, StructInfo> getStructRegistry() {
        return Collections.unmodifiableMap(structRegistry);
    }

    // registros para clases
    private final Map<String, ClassSymbol> classRegistry = new HashMap<>();

    public void registerClass(ClassSymbol classSymbol) {
        if (classSymbol != null) {
            classRegistry.put(classSymbol.getName(), classSymbol);
        }
    }

    public ClassSymbol lookupClass(String name) {
        return classRegistry.get(name);
    }

    public boolean classExists(String name) {
        return classRegistry.containsKey(name);
    }

    public Map<String, ClassSymbol> getClassRegistry() {
        return Collections.unmodifiableMap(classRegistry);
    }

}
