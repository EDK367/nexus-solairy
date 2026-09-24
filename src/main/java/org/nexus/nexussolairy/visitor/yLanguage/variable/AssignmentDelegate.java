package org.nexus.nexussolairy.visitor.yLanguage.variable;

import org.nexus.nexussolairy.YParser;
import org.nexus.nexussolairy.model.enums.DataType;
import org.nexus.nexussolairy.model.enums.SymbolKind;
import org.nexus.nexussolairy.model.enums.TypeErrorSemantic;
import org.nexus.nexussolairy.model.semantic.StructInfo;
import org.nexus.nexussolairy.model.semantic.Symbol;
import org.nexus.nexussolairy.model.semantic.TypeChecker;
import org.nexus.nexussolairy.visitor.VisitorContext;
import org.nexus.nexussolairy.visitor.yLanguage.expression.ExpressionEval;

import java.util.List;
import java.util.Map;

public class AssignmentDelegate {
    private final VisitorContext visitor;
    private final ExpressionEval expressionEval;
    private final YStructValidator structValidator;

    public AssignmentDelegate(VisitorContext visitor, ExpressionEval expressionEval) {
        this.visitor = visitor;
        this.expressionEval = expressionEval;
        this.structValidator = new YStructValidator(visitor);
    }

    @SuppressWarnings("unchecked")
    public DataType visitAssignStmt(YParser.AssignStmtContext ctx) {
        if (ctx == null) return DataType.VOID;
        int line = ctx.getStart().getLine();
        int col = ctx.getStart().getCharPositionInLine();

        DataType targetType = resolveTarget(ctx.target());

        if (ctx.INC() != null || ctx.DEC() != null) {
            if (!TypeChecker.isNumeric(targetType) && targetType != DataType.ERROR) {
                visitor.reportError(line, col, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Incremento/Decremento solo en numericos");
            }

            if (ctx.target() != null && ctx.target().ID().size() == 1 && ctx.target().LBRACK().isEmpty()) {
                String id = ctx.target().ID(0).getText();
                Symbol s = visitor.getSymbolTable().lookup(id);
                if (s != null && s.value instanceof Number num) {
                    if (s.type == DataType.FLOTANTE || s.type == DataType.DECIMALIS || num instanceof Double || num instanceof Float) {
                        double delta = ctx.INC() != null ? 1.0 : -1.0;
                        visitor.getSymbolTable().updateValue(id, num.doubleValue() + delta);
                    } else {
                        long delta = ctx.INC() != null ? 1L : -1L;
                        visitor.getSymbolTable().updateValue(id, num.longValue() + delta);
                    }
                }
            }
            return DataType.VOID;
        }

        boolean isCompound = ctx.ADD_ASSIGN() != null || ctx.SUB_ASSIGN() != null || ctx.MUL_ASSIGN() != null || ctx.DIV_ASSIGN() != null;
        if (isCompound) {
            if (!TypeChecker.isNumeric(targetType) && targetType != DataType.ERROR) {
                visitor.reportError(line, col, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Asignacion compuesta solo en tipos numericos");
            }
        }

        DataType exprType;
        if (targetType == DataType.STRUCT) {
            String targetStruct = resolveTargetStructName(ctx.target());
            boolean valid = structValidator.validateStructAssignment(targetStruct, ctx.expression(), line, col);
            exprType = valid ? DataType.STRUCT : DataType.ERROR;
        } else {
            exprType = visitor.visit(ctx.expression());
            if (!TypeChecker.isAssignable(targetType, exprType)) {
                visitor.reportError(line, col, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Asignacion invalida. Esperado: " + targetType + ", obtenido: " + exprType);
            }
        }

        Object val = expressionEval.evalExpression(ctx.expression());
        if (isCompound && ctx.target() != null && ctx.target().ID().size() == 1 && ctx.target().DOT().isEmpty() && ctx.target().LBRACK().isEmpty()) {
            String id = ctx.target().ID(0).getText();
            Symbol s = visitor.getSymbolTable().lookup(id);
            if (s != null && s.value instanceof Number curNum && val instanceof Number valNum) {
                boolean isDec = s.type == DataType.FLOTANTE || s.type == DataType.DECIMALIS || curNum instanceof Double || curNum instanceof Float || valNum instanceof Double || valNum instanceof Float;
                if (ctx.ADD_ASSIGN() != null) {
                    val = isDec ? (curNum.doubleValue() + valNum.doubleValue()) : (curNum.longValue() + valNum.longValue());
                } else if (ctx.SUB_ASSIGN() != null) {
                    val = isDec ? (curNum.doubleValue() - valNum.doubleValue()) : (curNum.longValue() - valNum.longValue());
                } else if (ctx.MUL_ASSIGN() != null) {
                    val = isDec ? (curNum.doubleValue() * valNum.doubleValue()) : (curNum.longValue() * valNum.longValue());
                } else if (ctx.DIV_ASSIGN() != null) {
                    if (valNum.doubleValue() != 0) {
                        val = isDec ? (curNum.doubleValue() / valNum.doubleValue()) : (curNum.longValue() / valNum.longValue());
                    }
                }
            }
        }

        if (ctx.target() != null && ctx.target().DOT().isEmpty() && ctx.target().LBRACK().isEmpty()) {
            String id = ctx.target().ID(0).getText();
            visitor.getSymbolTable().updateValue(id, val);
        } else if (ctx.target() != null && ctx.target().DOT().isEmpty() && !ctx.target().LBRACK().isEmpty()) {
            String id = ctx.target().ID(0).getText();
            Symbol s = visitor.getSymbolTable().lookup(id);
            if (s != null && s.value instanceof List) {
                Object idxObj = expressionEval.evalExpression(ctx.target().expression(0));
                int idx = 0;
                if (idxObj instanceof Number n) idx = n.intValue();
                List<Object> list = (List<Object>) s.value;
                if (idx >= 0 && idx < list.size()) {
                    list.set(idx, val);
                }
            }
        } else if (ctx.target() != null && !ctx.target().DOT().isEmpty() && ctx.target().LBRACK().isEmpty()) {
            String id = ctx.target().ID(0).getText();
            String field = ctx.target().ID(1).getText();
            Symbol s = visitor.getSymbolTable().lookup(id);
            if (s != null && s.value instanceof Map) {
                ((Map<String, Object>) s.value).put(field, val);
            }
        }

        return DataType.VOID;
    }

    public DataType resolveTarget(YParser.TargetContext ctx) {
        if (ctx == null) return DataType.ERROR;
        int line = ctx.getStart().getLine();
        int col = ctx.getStart().getCharPositionInLine();

        String id = ctx.ID(0).getText();
        Symbol s = visitor.getSymbolTable().lookup(id);

        if (s == null) {
            visitor.reportError(line, col, TypeErrorSemantic.UNDECLARED, "'" + id + "' no declarado");
            return DataType.ERROR;
        }

        if (s.kind == SymbolKind.FUNCTION) {
            visitor.reportError(line, col, TypeErrorSemantic.ASSIGNATION_ERROR, "'" + id + "' no es variable");
            return DataType.ERROR;
        }

        // ID
        if (ctx.DOT().isEmpty() && ctx.LBRACK().isEmpty()) {
            return s.type;
        }

        // ID[expr]+
        if (ctx.DOT().isEmpty() && !ctx.LBRACK().isEmpty()) {
            if (s.kind != SymbolKind.ARRAY && s.elementType == null) {
                visitor.reportError(line, col, TypeErrorSemantic.NOT_ARRAY, "'" + id + "' no es un arreglo");
            }
            for (YParser.ExpressionContext e : ctx.expression()) {
                DataType idx = visitor.visit(e);
                if (!TypeChecker.isInt(idx) && idx != DataType.ERROR) {
                    visitor.reportError(line, col, TypeErrorSemantic.ARRAY_INDEX_ERROR, "Indice debe ser entero");
                }
            }
            return s.elementType != null ? s.elementType : s.type;
        }

        // ID.ID+  o  ID.ID+[expr]+
        if (!ctx.DOT().isEmpty()) {
            String currentStructName = s.structTypeName != null ? s.structTypeName : s.type.getName();
            DataType t = s.type;

            for (int i = 1; i < ctx.ID().size(); i++) {
                String field = ctx.ID(i).getText();
                StructInfo structInfo = visitor.getSymbolTable().lookupStruct(currentStructName);
                if (structInfo == null) {
                    visitor.reportError(line, col, TypeErrorSemantic.NOT_STRUCT, "'" + currentStructName + "' no es una estructura");
                    return DataType.ERROR;
                }
                if (!structInfo.hasField(field)) {
                    visitor.reportError(line, col, TypeErrorSemantic.ATTRIBUTE_NOT_FOUND, "Campo '" + field + "' no existe en '" + currentStructName + "'");
                    return DataType.ERROR;
                }
                t = structInfo.getFieldType(field);
                currentStructName = structInfo.getFieldStructType(field);
            }

            if (!ctx.LBRACK().isEmpty()) {
                for (YParser.ExpressionContext e : ctx.expression()) {
                    DataType idx = visitor.visit(e);
                    if (!TypeChecker.isInt(idx) && idx != DataType.ERROR) {
                        visitor.reportError(line, col, TypeErrorSemantic.ARRAY_INDEX_ERROR, "Indice debe ser entero");
                    }
                }
            }
            return t;
        }

        return DataType.ERROR;
    }

    private String resolveTargetStructName(YParser.TargetContext ctx) {
        if (ctx == null || ctx.ID().isEmpty()) return null;
        String id = ctx.ID(0).getText();
        Symbol sym = visitor.getSymbolTable().lookup(id);
        if (sym == null) return null;
        if (ctx.DOT().isEmpty()) {
            return sym.structTypeName;
        }
        String currentStruct = sym.structTypeName;
        for (int i = 1; i < ctx.ID().size(); i++) {
            String field = ctx.ID(i).getText();
            StructInfo structInfo = visitor.getSymbolTable().lookupStruct(currentStruct);
            if (structInfo == null) return null;
            if (i == ctx.ID().size() - 1) {
                return structInfo.getFieldStructType(field);
            }
            currentStruct = structInfo.getFieldStructType(field);
        }
        return null;
    }
}
