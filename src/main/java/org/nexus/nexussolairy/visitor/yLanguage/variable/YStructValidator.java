package org.nexus.nexussolairy.visitor.yLanguage.variable;

import org.nexus.nexussolairy.YParser;
import org.nexus.nexussolairy.model.enums.DataType;
import org.nexus.nexussolairy.model.enums.TypeErrorSemantic;
import org.nexus.nexussolairy.model.semantic.StructInfo;
import org.nexus.nexussolairy.model.semantic.Symbol;
import org.nexus.nexussolairy.model.semantic.SymbolTable;
import org.nexus.nexussolairy.model.semantic.TypeChecker;
import org.nexus.nexussolairy.visitor.VisitorContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class YStructValidator {
    private final VisitorContext visitor;

    public YStructValidator(VisitorContext visitor) {
        this.visitor = visitor;
    }

    public static YParser.StructLiteralContext extractStructLiteral(YParser.ExpressionContext ctx) {
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

    public static String resolveExpressionStructName(YParser.ExpressionContext ctx, SymbolTable symbolTable) {
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
        if (primCtx.target() != null && primCtx.target().ID().size() == 1 && primCtx.target().DOT().isEmpty() && primCtx.target().LBRACK().isEmpty()) {
            String name = primCtx.target().ID(0).getText();
            Symbol s = symbolTable.lookup(name);
            if (s != null && s.type == DataType.STRUCT) {
                return s.structTypeName;
            }
        }
        if (primCtx.target() != null && primCtx.target().ID().size() >= 2 && !primCtx.target().DOT().isEmpty()) {
            String varName = primCtx.target().ID(0).getText();
            String fieldName = primCtx.target().ID(1).getText();
            Symbol s = symbolTable.lookup(varName);
            if (s != null) {
                String structName = s.structTypeName != null ? s.structTypeName : s.type.name().toLowerCase();
                StructInfo info = symbolTable.lookupStruct(structName);
                if (info != null && info.hasField(fieldName)) {
                    return info.getFieldStructType(fieldName);
                }
            }
        }
        return null;
    }

    public boolean validateStructAssignment(String targetStructName, YParser.ExpressionContext exprCtx, int line, int col) {
        if (targetStructName == null || exprCtx == null) return true;
        StructInfo targetInfo = visitor.getSymbolTable().lookupStruct(targetStructName);
        if (targetInfo == null) {
            visitor.reportError(line, col, TypeErrorSemantic.UNDECLARED, "Tipo no definido: '" + targetStructName + "'");
            return false;
        }

        YParser.StructLiteralContext litCtx = extractStructLiteral(exprCtx);
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

    public boolean validateStructLiteral(StructInfo targetInfo, YParser.StructLiteralContext litCtx) {
        if (targetInfo == null || litCtx == null) return true;
        String targetStructName = targetInfo.getName();
        List<String> fieldNames = new ArrayList<>(targetInfo.getFields().keySet());
        List<YParser.ExpressionContext> valueExprs = (litCtx.expressionList() != null && litCtx.expressionList().expression() != null)
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
            YParser.ExpressionContext valExpr = valueExprs.get(i);

            if (expectedStruct != null) {
                YParser.StructLiteralContext innerLit = extractStructLiteral(valExpr);
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
                YParser.StructLiteralContext innerLit = extractStructLiteral(valExpr);
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
            YParser.ExpressionContext extraExpr = valueExprs.get(i);
            visitor.visit(extraExpr);
        }

        return valid;
    }
}
