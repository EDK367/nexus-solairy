package org.nexus.nexussolairy.model.enums;

// clase enum temporal se modificara con los cambios
public enum DataType {
    TEXTUM(5),
    DECIMALIS(4),
    NUMERUS(3),
    LITTERA(2),
    BOOLEAN(1),
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

    // conversion de tipo primitivo a DataType
    public static DataType typeToken(String token) {
        return switch (token) {
            case "textum" -> TEXTUM;
            case "decimalis" -> DECIMALIS;
            case "numerus" -> NUMERUS;
            case "littera" -> LITTERA;
            case "bool" -> BOOLEAN;
            case "structura" -> STRUCT;
            default -> ERROR;
        };
    }

    // conversion de booleanos
    public static DataType typeTokenBool(String token) {
        return switch (token) {
            case "verum", "falsus" -> BOOLEAN;
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
