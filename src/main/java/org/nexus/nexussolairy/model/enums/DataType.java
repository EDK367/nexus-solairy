package org.nexus.nexussolairy.model.enums;

// clase enum para la jerarquia y los valores
public enum DataType {
    TEXTUM(5),
    CADENA(5),
    DECIMALIS(4),
    FLOTANTE(4),
    NUMERUS(3),
    ENTERO(3),
    LITTERA(2),
    CARACTER(2),
    BOOLEAN(1),
    BOOL(1),
    STRUCT(0),
    VOID(0),
    UNKNOWN(0),
    ERROR(-1);

    private final int hierarchy;

    DataType(int hierarchy) {
        this.hierarchy = hierarchy;
    }

    public int getHierarchy() {
        return hierarchy;
    }

    public String getName() {
        return name().toLowerCase();
    }

    // conversion de tipo primitivo a DataType
    public static DataType typeToken(String token) {
        if (token == null) return ERROR;
        return switch (token.toLowerCase()) {
            case "textum" -> TEXTUM;
            case "cadena", "string" -> CADENA;
            case "decimalis", "double" -> DECIMALIS;
            case "flotante", "float" -> FLOTANTE;
            case "numerus" -> NUMERUS;
            case "entero", "int" -> ENTERO;
            case "littera" -> LITTERA;
            case "caracter", "char" -> CARACTER;
            case "bool" -> BOOL;
            case "boolean" -> BOOLEAN;
            case "structura", "estructura" -> STRUCT;
            case "void" -> VOID;
            default -> ERROR;
        };
    }

    // conversion de booleanos
    public static DataType typeTokenBool(String token) {
        if (token == null) return ERROR;
        return switch (token.toLowerCase()) {
            case "verum", "falsus", "true", "false" -> BOOLEAN;
            case "verdadero", "falso" -> BOOL;
            default -> ERROR;
        };
    }

    // valor jerarquico
    public static DataType hierarchyType(DataType a, DataType b) {
        if (a == ERROR || b == ERROR) return ERROR;
        if (a == VOID) return a;
        if (b == VOID) return b;
        return a.getHierarchy() > b.getHierarchy() ? a : b;
    }

    // compatibilidad de asignacion
    public boolean isAssignableValue(DataType otherValue) {
        if (this == ERROR || otherValue == ERROR) return false;
        if (this == otherValue) return true;
        if (this == STRUCT && otherValue == STRUCT) return true;
        if (this == BOOLEAN || otherValue == BOOLEAN) return false;

        return this.hierarchy >= otherValue.hierarchy;
    }

    public boolean isReturnCompatible(DataType otherValue) {
        return this == otherValue || (this == VOID && otherValue == VOID) || (this == STRUCT && otherValue == STRUCT);
    }
}
