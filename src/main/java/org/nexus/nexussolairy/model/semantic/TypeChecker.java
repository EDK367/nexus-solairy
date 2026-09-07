package org.nexus.nexussolairy.model.semantic;

import org.nexus.nexussolairy.model.enums.DataType;

public class TypeChecker {

    public static DataType getResultType(DataType left, String op, DataType right) {
        if (left == null || right == null || left == DataType.ERROR || right == DataType.ERROR) {
            return DataType.ERROR;
        }
        switch (op) {
            case "+":
                if (left == DataType.TEXTUM || right == DataType.TEXTUM) return DataType.TEXTUM;
                if (left == DataType.NUMERUS && right == DataType.NUMERUS) return DataType.NUMERUS;
                if (left == DataType.NUMERUS && right == DataType.DECIMALIS) return DataType.DECIMALIS;
                if (left == DataType.DECIMALIS && right == DataType.NUMERUS) return DataType.DECIMALIS;
                if (left == DataType.DECIMALIS && right == DataType.DECIMALIS) return DataType.DECIMALIS;
                return DataType.ERROR;
            case "-":
            case "*":
            case "/":
                if (left == DataType.NUMERUS && right == DataType.NUMERUS) return DataType.NUMERUS;
                if ((left == DataType.NUMERUS && right == DataType.DECIMALIS)
                        || (left == DataType.DECIMALIS && right == DataType.NUMERUS)
                        || (left == DataType.DECIMALIS && right == DataType.DECIMALIS)) {
                    return DataType.DECIMALIS;
                }
                return DataType.ERROR;
            case "==":
            case "!=":
                if (left == right && left != DataType.VOID) return DataType.BOOLEAN;
                if (isNumeric(left) && isNumeric(right)) return DataType.BOOLEAN;
                return DataType.ERROR;
            case "<":
            case ">":
            case "<=":
            case ">=":
                if (isNumeric(left) && isNumeric(right)) return DataType.BOOLEAN;
                return DataType.ERROR;
            case "&&":
            case "||":
                if (left == DataType.BOOLEAN && right == DataType.BOOLEAN) return DataType.BOOLEAN;
                return DataType.ERROR;
            default:
                return DataType.ERROR;
        }
    }

    public static DataType getResultType(String op, DataType operand) {
        if (operand == null || operand == DataType.ERROR) return DataType.ERROR;
        if ("!".equals(op)) {
            return (operand == DataType.BOOLEAN) ? DataType.BOOLEAN : DataType.ERROR;
        }
        if ("-".equals(op) || "+".equals(op)) {
            if (operand == DataType.NUMERUS) return DataType.NUMERUS;
            if (operand == DataType.DECIMALIS) return DataType.DECIMALIS;
            return DataType.ERROR;
        }
        return DataType.ERROR;
    }

    public static boolean isAssignable(DataType target, DataType source) {
        if (target == null || source == null || target == DataType.ERROR || source == DataType.ERROR) {
            return false;
        }
        if (target == source) return true;
        if (target == DataType.DECIMALIS && source == DataType.NUMERUS) return true;
        if (target == DataType.STRUCT && source == DataType.STRUCT) return true;
        return false;
    }

    public static boolean isNumeric(DataType t) {
        return t == DataType.NUMERUS || t == DataType.DECIMALIS;
    }

    public boolean checkAssignmentCompatibility(DataType target, DataType source) {
        return isAssignable(target, source);
    }
}
