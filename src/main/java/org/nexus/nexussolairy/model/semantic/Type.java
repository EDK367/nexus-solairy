package org.nexus.nexussolairy.model.semantic;

import org.nexus.nexussolairy.model.enums.DataType;

import java.util.Objects;

public class Type {
    // tipos de datos con respecto a los lenguajes
    public static final Type ENTERO = new Type(DataType.ENTERO, "entero");
    public static final Type FLOTANTE = new Type(DataType.FLOTANTE, "flotante");
    public static final Type CADENA = new Type(DataType.CADENA, "cadena");
    public static final Type CARACTER = new Type(DataType.CARACTER, "caracter");
    public static final Type BOOL = new Type(DataType.BOOL, "bool");
    public static final Type BOOLEAN = new Type(DataType.BOOLEAN, "boolean");
    public static final Type NUMERUS = new Type(DataType.NUMERUS, "numerus");
    public static final Type DECIMALIS = new Type(DataType.DECIMALIS, "decimalis");
    public static final Type TEXTUM = new Type(DataType.TEXTUM, "textum");
    public static final Type LITTERA = new Type(DataType.LITTERA, "littera");
    public static final Type STRUCT = new Type(DataType.STRUCT, "struct");
    public static final Type VOID = new Type(DataType.VOID, "void");
    public static final Type ERROR = new Type(DataType.ERROR, "error");

    private final DataType dataType;
    private final String name;

    public Type(String name) {
        this.name = name != null ? name : "unknown";
        DataType dt = DataType.typeToken(name);
        this.dataType = (dt != DataType.ERROR) ? dt : DataType.STRUCT;
    }

    public Type(DataType dataType) {
        this.dataType = dataType != null ? dataType : DataType.ERROR;
        this.name = this.dataType.getName();
    }

    public Type(DataType dataType, String name) {
        this.dataType = dataType != null ? dataType : DataType.ERROR;
        this.name = name != null ? name : this.dataType.getName();
    }

    public DataType getDataType() {
        return dataType;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Type type = (Type) o;
        if (this.dataType == DataType.STRUCT || type.dataType == DataType.STRUCT) {
            return Objects.equals(name, type.name);
        }
        return this.dataType == type.dataType || Objects.equals(name, type.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
