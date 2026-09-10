package org.nexus.nexussolairy.model.semantic;

import java.util.HashMap;
import java.util.Map;

// clase Scope para guardar las variables y buscarlas
// no confundir con ScopeKind que es una ayuda logica y visual
public class Scope {

    private final String name;
    private final Scope parent;
    private final Map<String, Symbol> symbols = new HashMap<>();

    public Scope(String name, Scope parent) {
        this.name = name;
        this.parent = parent;
    }

    // busqueda la variable
    // en el ambito actual o en los ambitos padres
    // si no esta en ningun lado devuelve null
    public Symbol lookup(String name) {
        Symbol symbol = symbols.get(name);

        if (symbol != null) return symbol;

        return (parent != null) ? parent.lookup(name) : null;
    }

    public Symbol lookupLocal(String name) {
        return symbols.get(name);
    }

    public boolean declare(Symbol symbol) {
        if (symbols.containsKey(symbol.name)) return false;
        //if (symbol.scope == ScopeKind.GLOBAL && parent != null) return false;
        symbols.put(symbol.name, symbol);
        return true;
    }

    public String getName() {
        return name;
    }

    public Map<String, Symbol> getSymbols() {
        return new HashMap<>(symbols);
    }

    public Scope getParent() {
        return parent;
    }

    public Symbol resolve(String name) {
        return lookup(name);
    }

    public boolean define(Symbol symbol) {
        return declare(symbol);
    }

    public String getScopeName() {
        return name;
    }

    public Scope getEnclosingScope() {
        return parent;
    }

}
