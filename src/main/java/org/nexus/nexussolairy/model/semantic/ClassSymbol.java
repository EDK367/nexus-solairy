package org.nexus.nexussolairy.model.semantic;

import org.nexus.nexussolairy.model.enums.DataType;
import org.nexus.nexussolairy.model.enums.ScopeKind;
import org.nexus.nexussolairy.model.enums.SymbolKind;

import java.util.*;

public class ClassSymbol extends Symbol {

    private final Map<String, Symbol> fields = new LinkedHashMap<>();
    private final Map<String, List<Symbol>> methods = new LinkedHashMap<>();
    private final List<Symbol> constructors = new ArrayList<>();
    private Scope enclosingScope;
    private ClassSymbol superClass;

    public ClassSymbol(String name, Scope enclosing) {
        this(name, enclosing, 0, 0);
    }

    public ClassSymbol(String name, Scope enclosing, int line, int column) {
        super(name, DataType.STRUCT, SymbolKind.CLASS, ScopeKind.GLOBAL, null, line, column, null, null, null, null, name);
        this.enclosingScope = enclosing;
    }

    public ClassSymbol(String name) {
        this(name, null, 0, 0);
    }

    public ClassSymbol(String name, Type type, Scope enclosing) {
        this(name, enclosing, 0, 0);
    }

    public String getScopeName() {
        return getName();
    }

    public Scope getEnclosingScope() {
        return enclosingScope;
    }

    public Scope getParent() {
        return enclosingScope;
    }

    public void setEnclosingScope(Scope enclosingScope) {
        this.enclosingScope = enclosingScope;
    }

    public ClassSymbol getSuperClass() {
        return superClass;
    }

    public void setSuperClass(ClassSymbol superClass) {
        this.superClass = superClass;
    }

    public void define(Symbol sym) {
        if (sym == null) return;
        if (sym.getKind() == SymbolKind.FUNCTION) {
            if (sym.getName().equals(this.getName())) {
                constructors.add(sym);
            }
            methods.computeIfAbsent(sym.getName(), k -> new ArrayList<>()).add(sym);
        } else {
            fields.put(sym.getName(), sym);
        }
    }

    public boolean declare(Symbol sym) {
        if (sym == null) return false;
        if (sym.getKind() == SymbolKind.FUNCTION) {
            if (sym.getName().equals(this.getName())) {
                constructors.add(sym);
            }
            methods.computeIfAbsent(sym.getName(), k -> new ArrayList<>()).add(sym);
            return true;
        } else {
            if (fields.containsKey(sym.getName())) {
                return false;
            }
            fields.put(sym.getName(), sym);
            return true;
        }
    }

    public void addConstructor(Symbol c) {
        if (c != null) {
            constructors.add(c);
        }
    }

    public Symbol resolveField(String name) {
        Symbol s = fields.get(name);
        if (s != null) return s;
        if (superClass != null) {
            return superClass.resolveField(name);
        }
        return null;
    }

    public List<Symbol> resolveMethod(String name) {
        List<Symbol> ms = methods.get(name);
        if (ms != null && !ms.isEmpty()) return ms;
        if (superClass != null) {
            return superClass.resolveMethod(name);
        }
        return null;
    }

    public Symbol resolveMethod(String name, List<DataType> paramTypes) {
        List<Symbol> methodList = resolveMethod(name);
        if (methodList == null) return null;
        for (Symbol m : methodList) {
            if (paramTypes == null || paramTypes.isEmpty()) {
                if (m.getParamTypes() == null || m.getParamTypes().isEmpty()) {
                    return m;
                }
            } else if (m.getParamTypes() != null && m.getParamTypes().equals(paramTypes)) {
                return m;
            }
        }
        return null;
    }

    public List<Symbol> getConstructors() {
        return Collections.unmodifiableList(constructors);
    }

    public Symbol resolveConstructor(List<DataType> paramTypes) {
        for (Symbol c : constructors) {
            if (paramTypes == null || paramTypes.isEmpty()) {
                if (c.getParamTypes() == null || c.getParamTypes().isEmpty()) {
                    return c;
                }
            } else if (c.getParamTypes() != null && c.getParamTypes().equals(paramTypes)) {
                return c;
            }
        }
        return null;
    }

    public Symbol resolve(String name) {
        Symbol s = fields.get(name);
        if (s != null) return s;

        List<Symbol> ms = methods.get(name);
        if (ms != null && !ms.isEmpty()) return ms.get(0);

        if (superClass != null) {
            Symbol fromSuper = superClass.resolve(name);
            if (fromSuper != null) return fromSuper;
        }

        if (enclosingScope != null) {
            return enclosingScope.lookup(name);
        }
        return null;
    }

    public Symbol lookup(String name) {
        return resolve(name);
    }

    public Symbol lookupLocal(String name) {
        Symbol s = fields.get(name);
        if (s != null) return s;
        List<Symbol> ms = methods.get(name);
        if (ms != null && !ms.isEmpty()) return ms.get(0);
        return null;
    }

    public Map<String, Symbol> getFields() {
        return Collections.unmodifiableMap(fields);
    }

    public Map<String, List<Symbol>> getMethods() {
        return Collections.unmodifiableMap(methods);
    }

    public boolean hasField(String name) {
        return fields.containsKey(name) || (superClass != null && superClass.hasField(name));
    }

    public boolean hasMethod(String name) {
        return methods.containsKey(name) || (superClass != null && superClass.hasMethod(name));
    }

    public Scope asScope() {
        return new Scope(getName(), enclosingScope) {
            @Override
            public Symbol lookup(String name) {
                return ClassSymbol.this.resolve(name);
            }

            @Override
            public Symbol lookupLocal(String name) {
                return ClassSymbol.this.lookupLocal(name);
            }

            @Override
            public boolean declare(Symbol symbol) {
                return ClassSymbol.this.declare(symbol);
            }

            @Override
            public Map<String, Symbol> getSymbols() {
                Map<String, Symbol> all = new LinkedHashMap<>(fields);
                for (Map.Entry<String, List<Symbol>> entry : methods.entrySet()) {
                    if (!entry.getValue().isEmpty()) {
                        all.putIfAbsent(entry.getKey(), entry.getValue().get(0));
                    }
                }
                return all;
            }
        };
    }
}

