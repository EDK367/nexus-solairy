package org.nexus.nexussolairy.visitor.pigLatin.variable;

import org.nexus.nexussolairy.PigLatinParser;
import org.nexus.nexussolairy.model.enums.DataType;
import org.nexus.nexussolairy.model.enums.SymbolKind;
import org.nexus.nexussolairy.model.enums.TypeErrorSemantic;
import org.nexus.nexussolairy.model.semantic.ClassSymbol;
import org.nexus.nexussolairy.model.semantic.StructInfo;
import org.nexus.nexussolairy.model.semantic.Symbol;
import org.nexus.nexussolairy.visitor.VisitorContext;
import org.nexus.nexussolairy.model.semantic.TypeChecker;
import org.nexus.nexussolairy.visitor.pigLatin.expression.ExpressionEval;

import java.util.List;
import java.util.Map;

public class AssignmentDelegate {
    private final VisitorContext visitor;
    private final ExpressionEval expressionEval;

    public AssignmentDelegate(VisitorContext visitor, ExpressionEval expressionEval) {
        this.visitor = visitor;
        this.expressionEval = expressionEval;
    }

    @SuppressWarnings("unchecked")
    public DataType visitAssignStmt(PigLatinParser.AssignStmtContext ctx) {
        if (ctx == null) return DataType.VOID;
        int line = ctx.getStart().getLine();
        int col = ctx.getStart().getCharPositionInLine();

        DataType targetType = resolveAssignTarget(ctx);
        DataType exprType;
        PigLatinParser.ExpressionContext valueExpr;

        if (ctx.DOT() == null && ctx.LBRACK() == null) {
            valueExpr = ctx.expression(0);
        } else if (ctx.DOT() == null && ctx.LBRACK() != null) {
            valueExpr = ctx.expression(1);
        } else if (ctx.DOT() != null && ctx.LBRACK() == null) {
            valueExpr = ctx.expression(0);
        } else {
            valueExpr = ctx.expression(1);
        }

        exprType = visitor.visit(valueExpr);

        if (targetType != null && targetType != DataType.ERROR && exprType != DataType.ERROR) {
            if (!TypeChecker.isAssignable(targetType, exprType)) {
                visitor.reportError(line, col, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Asignacion invalida. Esperado: " + targetType + ", obtenido: " + exprType);
            }
        }

        Object val = expressionEval.evalExpression(valueExpr);

        if (ctx.DOT() == null && ctx.LBRACK() == null) {
            String id = ctx.ID(0).getText();
            visitor.getSymbolTable().updateValue(id, val);
        } else if (ctx.DOT() == null && ctx.LBRACK() != null) {
            String id = ctx.ID(0).getText();
            Symbol sym = visitor.getSymbolTable().lookup(id);
            if (sym != null && sym.value instanceof List) {
                Object idxObj = expressionEval.evalExpression(ctx.expression(0));
                int idx = 0;
                if (idxObj instanceof Long l) idx = l.intValue();
                else if (idxObj instanceof Integer i) idx = i;
                else if (idxObj instanceof Double d) idx = d.intValue();
                List<Object> list = (List<Object>) sym.value;
                if (idx >= 0 && idx < list.size()) {
                    list.set(idx, val);
                }
            }
        } else if (ctx.DOT() != null && ctx.LBRACK() == null) {
            String id = ctx.ID(0).getText();
            String field = ctx.ID(1).getText();
            Symbol sym = visitor.getSymbolTable().lookup(id);
            if (sym != null && sym.value instanceof Map) {
                ((Map<String, Object>) sym.value).put(field, val);
            }
        } else if (ctx.DOT() != null && ctx.LBRACK() != null) {
            String id = ctx.ID(0).getText();
            String field = ctx.ID(1).getText();
            Symbol sym = visitor.getSymbolTable().lookup(id);
            if (sym != null && sym.value instanceof Map) {
                Object fieldVal = ((Map<String, Object>) sym.value).get(field);
                if (fieldVal instanceof List) {
                    Object idxObj = expressionEval.evalExpression(ctx.expression(0));
                    int idx = 0;
                    if (idxObj instanceof Long l) idx = l.intValue();
                    else if (idxObj instanceof Integer i) idx = i;
                    else if (idxObj instanceof Double d) idx = d.intValue();
                    List<Object> list = (List<Object>) fieldVal;
                    if (idx >= 0 && idx < list.size()) {
                        list.set(idx, val);
                    }
                }
            }
        }

        return DataType.VOID;
    }

    private DataType resolveAssignTarget(PigLatinParser.AssignStmtContext ctx) {
        int line = ctx.getStart().getLine();
        int col = ctx.getStart().getCharPositionInLine();
        String id = ctx.ID(0).getText();
        Symbol sym = visitor.getSymbolTable().lookup(id);

        if (sym == null) {
            visitor.reportError(line, col, TypeErrorSemantic.UNDECLARED, "Variable '" + id + "' no declarada.");
            return DataType.ERROR;
        }

        if (sym.kind == SymbolKind.FUNCTION) {
            visitor.reportError(line, col, TypeErrorSemantic.ASSIGNATION_ERROR, "'" + id + "' no es una variable.");
            return DataType.ERROR;
        }

        if (ctx.DOT() == null && ctx.LBRACK() == null) {
            return sym.type;
        }

        if (ctx.DOT() == null && ctx.LBRACK() != null) {
            if (sym.kind != SymbolKind.ARRAY && sym.elementType == null) {
                visitor.reportError(line, col, TypeErrorSemantic.NOT_ARRAY, "'" + id + "' no es un arreglo.");
            }
            DataType idx = visitor.visit(ctx.expression(0));
            if (idx != DataType.NUMERUS && idx != DataType.ERROR) {
                visitor.reportError(line, col, TypeErrorSemantic.ARRAY_INDEX_ERROR, "Indice debe ser 'numerus'.");
            }
            return sym.elementType != null ? sym.elementType : sym.type;
        }

        if (ctx.DOT() != null && ctx.LBRACK() == null) {
            String field = ctx.ID(1).getText();
            return resolveFieldType(sym, field, line, col);
        }

        if (ctx.DOT() != null && ctx.LBRACK() != null) {
            String field = ctx.ID(1).getText();
            DataType ft = resolveFieldType(sym, field, line, col);
            DataType idx = visitor.visit(ctx.expression(0));
            if (idx != DataType.NUMERUS && idx != DataType.ERROR) {
                visitor.reportError(line, col, TypeErrorSemantic.ARRAY_INDEX_ERROR, "Indice debe ser 'numerus'.");
            }
            return ft;
        }

        return DataType.ERROR;
    }

    private DataType resolveFieldType(Symbol sym, String field, int line, int col) {
        String typeName = sym.structTypeName != null ? sym.structTypeName : sym.type.name().toLowerCase();
        ClassSymbol cls = visitor.getSymbolTable().lookupClass(typeName);
        if (cls != null) {
            Symbol f = cls.resolveField(field);
            if (f == null) {
                visitor.reportError(line, col, TypeErrorSemantic.ATTRIBUTE_NOT_FOUND, "Campo '" + field + "' no existe en la clase '" + typeName + "'.");
                return DataType.ERROR;
            }
            return f.getType();
        }
        StructInfo info = visitor.getSymbolTable().lookupStruct(typeName);
        if (info == null) {
            visitor.reportError(line, col, TypeErrorSemantic.NOT_STRUCT, "Tipo '" + typeName + "' no es una estructura o no esta declarada.");
            return DataType.ERROR;
        }
        if (!info.hasField(field)) {
            visitor.reportError(line, col, TypeErrorSemantic.ATTRIBUTE_NOT_FOUND, "Campo '" + field + "' no existe en '" + typeName + "'.");
            return DataType.ERROR;
        }
        return info.getFieldType(field);
    }
}
