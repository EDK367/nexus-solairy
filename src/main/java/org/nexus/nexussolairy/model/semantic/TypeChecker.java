package org.nexus.nexussolairy.model.semantic;

import org.nexus.nexussolairy.model.enums.DataType;

public class TypeChecker {

    /*
    VERIFICACION DE LOS TIPOS DE DATOS PARA LOS TRES LENGUAJES
     */
    public static boolean isNumeric(DataType t) {
        if (t == null) return false;
        String n = t.getName();
        return n.equals("numerus") || n.equals("decimalis")
                || n.equals("entero") || n.equals("flotante")
                || n.equals("int") || n.equals("double");
    }

    public static boolean isNumeric(Type t) {
        if (t == null) return false;
        String n = t.getName();
        return n.equals("numerus") || n.equals("decimalis")
                || n.equals("entero") || n.equals("flotante")
                || n.equals("int") || n.equals("double");
    }

    public static boolean isString(DataType t) {
        if (t == null) return false;
        String n = t.getName();
        return n.equals("textum") || n.equals("cadena") || n.equals("string");
    }

    public static boolean isString(Type t) {
        if (t == null) return false;
        String n = t.getName();
        return n.equals("textum") || n.equals("cadena") || n.equals("string");
    }

    public static boolean isBool(DataType t) {
        if (t == null) return false;
        String n = t.getName();
        return n.equals("bool") || n.equals("boolean");
    }

    public static boolean isBool(Type t) {
        if (t == null) return false;
        String n = t.getName();
        return n.equals("bool") || n.equals("boolean");
    }

    public static boolean isInt(DataType t) {
        if (t == null) return false;
        String n = t.getName();
        return n.equals("numerus") || n.equals("entero") || n.equals("int");
    }

    public static boolean isInt(Type t) {
        if (t == null) return false;
        String n = t.getName();
        return n.equals("numerus") || n.equals("entero") || n.equals("int");
    }

    public static boolean isFloat(DataType t) {
        if (t == null) return false;
        String n = t.getName();
        return n.equals("decimalis") || n.equals("flotante") || n.equals("double") || n.equals("float");
    }

    public static boolean isFloat(Type t) {
        if (t == null) return false;
        String n = t.getName();
        return n.equals("decimalis") || n.equals("flotante") || n.equals("double") || n.equals("float");
    }

    public static DataType getWiderNumeric(DataType a, DataType b) {
        if (a == null || b == null) return DataType.ERROR;
        String an = a.getName(), bn = b.getName();
        if (an.equals("flotante") || an.equals("decimalis") || an.equals("double")
                || bn.equals("flotante") || bn.equals("decimalis") || bn.equals("double")) {
            return (a == DataType.DECIMALIS || b == DataType.DECIMALIS) ? DataType.DECIMALIS : DataType.FLOTANTE;
        }
        return (a == DataType.NUMERUS || b == DataType.NUMERUS) ? DataType.NUMERUS : DataType.ENTERO;
    }

    public static Type getWiderNumeric(Type a, Type b) {
        if (a == null || b == null) return Type.ERROR;
        String an = a.getName(), bn = b.getName();
        if (an.equals("flotante") || an.equals("decimalis") || an.equals("double")
                || bn.equals("flotante") || bn.equals("decimalis") || bn.equals("double")) {
            return (a == Type.DECIMALIS || b == Type.DECIMALIS) ? Type.DECIMALIS : Type.FLOTANTE;
        }
        return (a == Type.NUMERUS || b == Type.NUMERUS) ? Type.NUMERUS : Type.ENTERO;
    }

    public static DataType getResultType(DataType left, String op, DataType right) {
        if (left == null || right == null || left == DataType.ERROR || right == DataType.ERROR) {
            return DataType.ERROR;
        }

        switch (op) {
            case "+":
                if (left == DataType.STRUCT || right == DataType.STRUCT
                        || left == DataType.CLASS || right == DataType.CLASS
                        || left == DataType.VOID || right == DataType.VOID) {
                    return DataType.ERROR;
                }
                if (isString(left) || isString(right)) {
                    if (isString(left)) return left;
                    if (isString(right)) return right;
                    return DataType.CADENA;
                }
                if (isNumeric(left) && isNumeric(right)) {
                    return getWiderNumeric(left, right);
                }
                return DataType.ERROR;
            case "-":
            case "*":
            case "/":
                if (isNumeric(left) && isNumeric(right)) {
                    return getWiderNumeric(left, right);
                }
                return DataType.ERROR;
            case "==":
            case "!=":
                if (left.equals(right) && left != DataType.VOID) {
                    return (left == DataType.BOOL || right == DataType.BOOL) ? DataType.BOOL : DataType.BOOLEAN;
                }
                if (isNumeric(left) && isNumeric(right)) {
                    return (left == DataType.ENTERO || left == DataType.FLOTANTE || right == DataType.ENTERO || right == DataType.FLOTANTE)
                            ? DataType.BOOL : DataType.BOOLEAN;
                }
                if (isString(left) && isString(right)) {
                    return (left == DataType.CADENA || right == DataType.CADENA) ? DataType.BOOL : DataType.BOOLEAN;
                }
                if (isBool(left) && isBool(right)) {
                    return (left == DataType.BOOL || right == DataType.BOOL) ? DataType.BOOL : DataType.BOOLEAN;
                }
                return DataType.ERROR;
            case "<":
            case ">":
            case "<=":
            case ">=":
                if (isNumeric(left) && isNumeric(right)) {
                    return (left == DataType.ENTERO || left == DataType.FLOTANTE || right == DataType.ENTERO || right == DataType.FLOTANTE)
                            ? DataType.BOOL : DataType.BOOLEAN;
                }
                return DataType.ERROR;
            case "&&":
            case "||":
                if (isBool(left) && isBool(right)) {
                    return (left == DataType.BOOL || right == DataType.BOOL) ? DataType.BOOL : DataType.BOOLEAN;
                }
                return DataType.ERROR;
            default:
                return DataType.ERROR;
        }
    }

    public static Type getResultType(Type left, String op, Type right) {
        if (left == null || right == null || left == Type.ERROR || right == Type.ERROR) {
            return Type.ERROR;
        }

        switch (op) {
            case "+":
                if (left.getDataType() == DataType.STRUCT || right.getDataType() == DataType.STRUCT
                        || left.getDataType() == DataType.CLASS || right.getDataType() == DataType.CLASS
                        || left.getDataType() == DataType.VOID || right.getDataType() == DataType.VOID) {
                    return Type.ERROR;
                }
                if (isString(left) || isString(right)) return isString(left) ? left : right;
                if (isNumeric(left) && isNumeric(right)) return getWiderNumeric(left, right);
                return Type.ERROR;
            case "-":
            case "*":
            case "/":
            case "%":
                if (isNumeric(left) && isNumeric(right)) return getWiderNumeric(left, right);
                return Type.ERROR;
            case "==":
            case "!=":
                if (left.equals(right)) return Type.BOOL;
                if (isNumeric(left) && isNumeric(right)) return Type.BOOL;
                if ((left.getName().equals("null") && (right.getDataType() == DataType.STRUCT || isString(right)))
                        || (right.getName().equals("null") && (left.getDataType() == DataType.STRUCT || isString(left)))) {
                    return Type.BOOL;
                }
                if (left.getDataType() == DataType.STRUCT && right.getDataType() == DataType.STRUCT) {
                    return Type.BOOL;
                }
                return Type.ERROR;
            case "<":
            case ">":
            case "<=":
            case ">=":
                if (isNumeric(left) && isNumeric(right)) return Type.BOOL;
                return Type.ERROR;
            case "&&":
            case "||":
                if (isBool(left) && isBool(right)) return Type.BOOL;
                return Type.ERROR;
            default:
                return Type.ERROR;
        }
    }

    public static DataType getResultType(String op, DataType operand) {
        if (operand == null || operand == DataType.ERROR) return DataType.ERROR;
        if ("!".equals(op)) {
            return isBool(operand) ? ((operand == DataType.BOOL) ? DataType.BOOL : DataType.BOOLEAN) : DataType.ERROR;
        }
        if ("-".equals(op) || "+".equals(op)) {
            if (isNumeric(operand)) return operand;
            return DataType.ERROR;
        }
        return DataType.ERROR;
    }

    public static Type getResultType(String op, Type operand) {
        if (operand == null || operand == Type.ERROR) return Type.ERROR;
        if ("!".equals(op)) {
            return isBool(operand) ? Type.BOOL : Type.ERROR;
        }
        if ("-".equals(op) || "+".equals(op)) {
            if (isNumeric(operand)) return operand;
            return Type.ERROR;
        }
        return Type.ERROR;
    }

    public static boolean isAssignable(DataType target, DataType source) {
        if (target == null || source == null) return false;
        if (target == DataType.ERROR || source == DataType.ERROR) return true; // evitar cascada
        if (target == source) return true;

        String tn = target.getName();
        String sn = source.getName();

        // Coerción numérica ascendente
        if ((tn.equals("flotante") || tn.equals("decimalis") || tn.equals("double"))
                && (sn.equals("entero") || sn.equals("numerus") || sn.equals("int"))) {
            return true;
        }

        // Equivalencias entre lenguajes
        if (isInt(target) && isInt(source)) return true;
        if (isFloat(target) && isFloat(source)) return true;
        if (isString(target) && isString(source)) return true;
        if (isBool(target) && isBool(source)) return true;

        if (target == DataType.STRUCT && source == DataType.STRUCT) return true;
        if (target == DataType.CLASS && source == DataType.CLASS) return true;
        if (target == DataType.STRUCT && source == DataType.CLASS) return true;
        if (target == DataType.CLASS && source == DataType.STRUCT) return true;

        return false;
    }

    public static boolean isAssignable(Type target, Type source) {
        if (target == null || source == null) return false;
        if (target == Type.ERROR || source == Type.ERROR) return true; // evitar cascada
        if (target.equals(source)) return true;

        String tn = target.getName();
        String sn = source.getName();

        if (sn.equals("null") && (target.getDataType() == DataType.STRUCT || target.getDataType() == DataType.CLASS || isString(target))) return true;

        // Coerción numérica ascendente
        if ((tn.equals("flotante") || tn.equals("decimalis") || tn.equals("double"))
                && (sn.equals("entero") || sn.equals("numerus") || sn.equals("int"))) {
            return true;
        }

        if (isInt(target) && isInt(source)) return true;
        if (isFloat(target) && isFloat(source)) return true;
        if (isString(target) && isString(source)) return true;
        if (isBool(target) && isBool(source)) return true;

        if ((target.getDataType() == DataType.STRUCT || target.getDataType() == DataType.CLASS)
                && (source.getDataType() == DataType.STRUCT || source.getDataType() == DataType.CLASS)) return true;

        return false;
    }

    public static boolean isAssignable(DataType target, String targetStruct, DataType source, String sourceStruct) {
        if (target == DataType.STRUCT && source == DataType.STRUCT) {
            if (targetStruct == null || sourceStruct == null) return true;
            return targetStruct.equals(sourceStruct);
        }
        return isAssignable(target, source);
    }

    public boolean checkAssignmentCompatibility(DataType target, DataType source) {
        return isAssignable(target, source);
    }
}
