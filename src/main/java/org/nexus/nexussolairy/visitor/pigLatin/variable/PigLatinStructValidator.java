package org.nexus.nexussolairy.visitor.pigLatin.variable;

import org.nexus.nexussolairy.PigLatinParser;
import org.nexus.nexussolairy.model.enums.DataType;
import org.nexus.nexussolairy.model.enums.SymbolKind;
import org.nexus.nexussolairy.model.enums.TypeErrorSemantic;
import org.nexus.nexussolairy.model.semantic.ClassSymbol;
import org.nexus.nexussolairy.model.semantic.StructInfo;
import org.nexus.nexussolairy.model.semantic.Symbol;
import org.nexus.nexussolairy.model.semantic.SymbolTable;
import org.nexus.nexussolairy.model.semantic.TypeChecker;
import org.nexus.nexussolairy.visitor.VisitorContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PigLatinStructValidator {
    private final VisitorContext visitor;

    public PigLatinStructValidator(VisitorContext visitor) {
        this.visitor = visitor;
    }

    public static PigLatinParser.StructLiteralContext extractStructLiteral(PigLatinParser.ExpressionContext ctx) {
        if (ctx == null || ctx.orExpression() == null) return null;
        var orCtx = ctx.orExpression();
        if (orCtx.andExpression().size() != 1) return null;
        var andCtx = orCtx.andExpression(0);
        if (andCtx.relationalExpression().size() != 1) return null;
        var relCtx = andCtx.relationalExpression(0);
        if (relCtx.additiveExpression().size() != 1) return null;
        var addCtx = relCtx.additiveExpression(0);
        if (addCtx.multiplicativeExpression().size() != 1) return null;
        var mulCtx = addCtx.multiplicativeExpression(0);
        if (mulCtx.unaryExpression().size() != 1) return null;
        var unCtx = mulCtx.unaryExpression(0);
        if (unCtx.postfixExpression() == null) return null;
        var postCtx = unCtx.postfixExpression();
        if (postCtx.primaryExpression() == null) return null;
        var primCtx = postCtx.primaryExpression();
        if (primCtx.structLiteral() != null) return primCtx.structLiteral();
        if (primCtx.LPAREN() != null && primCtx.expression() != null) {
            return extractStructLiteral(primCtx.expression());
        }
        return null;
    }

    public static String resolveExpressionStructName(PigLatinParser.ExpressionContext ctx, SymbolTable symbolTable) {
        if (ctx == null || ctx.orExpression() == null) return null;
        var orCtx = ctx.orExpression();
        if (orCtx.andExpression().size() != 1) return null;
        var andCtx = orCtx.andExpression(0);
        if (andCtx.relationalExpression().size() != 1) return null;
        var relCtx = andCtx.relationalExpression(0);
        if (relCtx.additiveExpression().size() != 1) return null;
        var addCtx = relCtx.additiveExpression(0);
        if (addCtx.multiplicativeExpression().size() != 1) return null;
        var mulCtx = addCtx.multiplicativeExpression(0);
        if (mulCtx.unaryExpression().size() != 1) return null;
        var unCtx = mulCtx.unaryExpression(0);
        if (unCtx.postfixExpression() == null) return null;
        var postCtx = unCtx.postfixExpression();
        if (postCtx.primaryExpression() == null) return null;
        var primCtx = postCtx.primaryExpression();
        if (primCtx.LPAREN() != null && primCtx.expression() != null) {
            return resolveExpressionStructName(primCtx.expression(), symbolTable);
        }
        if (primCtx.ID().size() == 1 && primCtx.DOT() == null && primCtx.LBRACK() == null && primCtx.LPAREN() == null) {
            String name = primCtx.ID(0).getText();
            Symbol s = symbolTable.lookup(name);
            if (s != null && s.type == DataType.STRUCT) {
                return s.structTypeName;
            }
        }
        if (primCtx.ID().size() == 1 && primCtx.LPAREN() != null && primCtx.DOT() == null) {
            String name = primCtx.ID(0).getText();
            Symbol s = symbolTable.lookup(name);
            if (s != null && s.kind == SymbolKind.FUNCTION && s.returnType == DataType.STRUCT) {
                return s.structTypeName;
            }
        }
        if (primCtx.ID().size() >= 2 && primCtx.DOT() != null && primCtx.LPAREN() == null) {
            String varName = primCtx.ID(0).getText();
            String fieldName = primCtx.ID(1).getText();
            Symbol s = symbolTable.lookup(varName);
            if (s != null) {
                String structName = s.structTypeName != null ? s.structTypeName : s.type.name().toLowerCase();
                StructInfo info = symbolTable.lookupStruct(structName);
                if (info != null && info.hasField(fieldName)) {
                    return info.getFieldStructType(fieldName);
                }
            }
        }
        if (primCtx.ID().size() >= 2 && primCtx.DOT() != null && primCtx.LPAREN() != null) {
            String objName = primCtx.ID(0).getText();
            String methodName = primCtx.ID(1).getText();
            Symbol s = symbolTable.lookup(objName);
            if (s != null) {
                String clsName = s.structTypeName != null ? s.structTypeName : s.type.name().toLowerCase();
                ClassSymbol cls = symbolTable.lookupClass(clsName);
                if (cls != null && cls.getMethods().containsKey(methodName)) {
                    List<Symbol> mList = cls.getMethods().get(methodName);
                    if (mList != null && !mList.isEmpty()) {
                        return mList.get(0).structTypeName;
                    }
                }
            }
        }
        return null;
    }

    public boolean validateStructAssignment(String targetStructName, PigLatinParser.ExpressionContext exprCtx, int line, int col) {
        if (targetStructName == null || exprCtx == null) return true;
        StructInfo targetInfo = visitor.getSymbolTable().lookupStruct(targetStructName);
        if (targetInfo == null) {
            visitor.reportError(line, col, TypeErrorSemantic.UNDECLARED, "Tipo no definido: '" + targetStructName + "'");
            return false;
        }

        PigLatinParser.StructLiteralContext litCtx = extractStructLiteral(exprCtx);
        if (litCtx != null) {
            return validateStructLiteral(targetInfo, litCtx);
        } else {
            DataType actualType = visitor.visit(exprCtx);
            if (actualType != DataType.STRUCT && actualType != DataType.ERROR) {
                visitor.reportError(line, col, TypeErrorSemantic.INCOMPATIBLE_TYPES,
                        "Tipo incompatible. Esperado estructura '" + targetStructName + "', obtenido: " + actualType);
                return false;
            } else if (actualType == DataType.STRUCT) {
                String actualStruct = resolveExpressionStructName(exprCtx, visitor.getSymbolTable());
                if (actualStruct != null && !actualStruct.equals(targetStructName)) {
                    visitor.reportError(line, col, TypeErrorSemantic.INCOMPATIBLE_TYPES,
                            "Tipo incompatible. Esperado estructura '" + targetStructName + "', obtenido: " + actualStruct);
                    return false;
                }
            }
            return true;
        }
    }

    public boolean validateStructLiteral(StructInfo targetInfo, PigLatinParser.StructLiteralContext litCtx) {
        if (targetInfo == null || litCtx == null) return true;
        String targetStructName = targetInfo.getName();
        List<String> fieldNames = new ArrayList<>(targetInfo.getFields().keySet());
        List<PigLatinParser.ExpressionContext> valueExprs = (litCtx.expressionList() != null && litCtx.expressionList().expression() != null)
                ? litCtx.expressionList().expression()
                : Collections.emptyList();

        boolean valid = true;
        if (valueExprs.size() != fieldNames.size()) {
            visitor.reportError(litCtx.getStart().getLine(), litCtx.getStart().getCharPositionInLine(), TypeErrorSemantic.WRONG_NUMBER_OF_PARAMS,
                    "Cantidad de valores para la estructura '" + targetStructName + "' no coincide. Esperado: " + fieldNames.size() + ", obtenido: " + valueExprs.size());
            valid = false;
        }

        int checkLimit = Math.min(valueExprs.size(), fieldNames.size());
        for (int i = 0; i < checkLimit; i++) {
            String fName = fieldNames.get(i);
            DataType expectedType = targetInfo.getFieldType(fName);
            String expectedStruct = targetInfo.getFieldStructType(fName);
            PigLatinParser.ExpressionContext valExpr = valueExprs.get(i);

            if (expectedStruct != null) {
                PigLatinParser.StructLiteralContext innerLit = extractStructLiteral(valExpr);
                if (innerLit != null) {
                    StructInfo innerInfo = visitor.getSymbolTable().lookupStruct(expectedStruct);
                    if (innerInfo != null) {
                        boolean innerValid = validateStructLiteral(innerInfo, innerLit);
                        if (!innerValid) valid = false;
                    } else {
                        visitor.reportError(valExpr.getStart().getLine(), valExpr.getStart().getCharPositionInLine(), TypeErrorSemantic.UNDECLARED,
                                "Tipo no definido: '" + expectedStruct + "'");
                        valid = false;
                    }
                } else {
                    DataType actualType = visitor.visit(valExpr);
                    if (actualType != DataType.STRUCT && actualType != DataType.ERROR) {
                        visitor.reportError(valExpr.getStart().getLine(), valExpr.getStart().getCharPositionInLine(), TypeErrorSemantic.INCOMPATIBLE_TYPES,
                                "Tipo incompatible para el campo '" + fName + "'. Esperado estructura '" + expectedStruct + "', obtenido: " + actualType);
                        valid = false;
                    } else if (actualType == DataType.STRUCT) {
                        String actualStruct = resolveExpressionStructName(valExpr, visitor.getSymbolTable());
                        if (actualStruct != null && !actualStruct.equals(expectedStruct)) {
                            visitor.reportError(valExpr.getStart().getLine(), valExpr.getStart().getCharPositionInLine(), TypeErrorSemantic.INCOMPATIBLE_TYPES,
                                    "Tipo incompatible para el campo '" + fName + "'. Esperado: " + expectedStruct + ", obtenido: " + actualStruct);
                            valid = false;
                        }
                    }
                }
            } else {
                PigLatinParser.StructLiteralContext innerLit = extractStructLiteral(valExpr);
                if (innerLit != null) {
                    visitor.reportError(valExpr.getStart().getLine(), valExpr.getStart().getCharPositionInLine(), TypeErrorSemantic.INCOMPATIBLE_TYPES,
                            "Tipo incompatible para el campo '" + fName + "'. Esperado: " + expectedType + ", obtenido: STRUCT");
                    valid = false;
                } else {
                    DataType actualType = visitor.visit(valExpr);
                    if (actualType != DataType.ERROR && !TypeChecker.isAssignable(expectedType, actualType)) {
                        visitor.reportError(valExpr.getStart().getLine(), valExpr.getStart().getCharPositionInLine(), TypeErrorSemantic.INCOMPATIBLE_TYPES,
                                "Tipo incompatible para el campo '" + fName + "'. Esperado: " + expectedType + ", obtenido: " + actualType);
                        valid = false;
                    }
                }
            }
        }

        for (int i = fieldNames.size(); i < valueExprs.size(); i++) {
            PigLatinParser.ExpressionContext extraExpr = valueExprs.get(i);
            visitor.visit(extraExpr);
        }

        return valid;
    }
}
