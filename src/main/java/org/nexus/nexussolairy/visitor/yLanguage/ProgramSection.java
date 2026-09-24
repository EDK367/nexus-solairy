package org.nexus.nexussolairy.visitor.yLanguage;

import org.nexus.nexussolairy.YParser;
import org.nexus.nexussolairy.model.enums.*;
import org.nexus.nexussolairy.model.semantic.StructInfo;
import org.nexus.nexussolairy.model.semantic.Symbol;
import org.nexus.nexussolairy.model.semantic.TypeChecker;
import org.nexus.nexussolairy.visitor.VisitorContext;

import java.util.ArrayList;
import java.util.List;

public class ProgramSection {
    private final VisitorContext visitor;

    public ProgramSection(VisitorContext visitor) {
        this.visitor = visitor;
    }

    public DataType visitProgram(YParser.ProgramContext ctx) {
        if (ctx == null) return DataType.VOID;
        if (ctx.structSection() != null) {
            visitor.visit(ctx.structSection());
        }
        if (ctx.funcSection() != null) {
            visitor.visit(ctx.funcSection());
        }
        return DataType.VOID;
    }

    public DataType visitStructSection(YParser.StructSectionContext ctx) {
        if (ctx == null) return DataType.VOID;
        for (YParser.StructDefContext s : ctx.structDef()) {
            visitor.visit(s);
        }
        return DataType.VOID;
    }

    public DataType visitStructDef(YParser.StructDefContext ctx) {
        if (ctx == null) return DataType.VOID;
        String name = ctx.ID().getText();
        int line = ctx.getStart().getLine();
        int col = ctx.getStart().getCharPositionInLine();

        if (visitor.getSymbolTable().structExists(name)) {
            visitor.reportError(line, col, TypeErrorSemantic.ALREADY_DECLARED, "Estructura '" + name + "' ya definida");
        }

        StructInfo ss = new StructInfo(name);
        visitor.getSymbolTable().registerStruct(ss);

        for (YParser.StructFieldContext f : ctx.structField()) {
            visitStructFieldWithInfo(f, ss);
        }

        Symbol structSym = new Symbol(name, DataType.STRUCT, SymbolKind.STRUCT, ScopeKind.GLOBAL, LanguageType.Y_LANG, null, line, col, null, null, null, null, name);
        visitor.getSymbolTable().declare(structSym);

        return DataType.VOID;
    }

    public DataType visitStructField(YParser.StructFieldContext ctx) {
        return DataType.VOID;
    }

    private void visitStructFieldWithInfo(YParser.StructFieldContext ctx, StructInfo structInfo) {
        if (ctx == null) return;
        int line = ctx.getStart().getLine();
        int col = ctx.getStart().getCharPositionInLine();

        if (ctx.type() != null) {
            DataType t = ((YVisitorImpl) visitor).visitType(ctx.type());
            String fieldName = ctx.ID(0).getText();
            if (structInfo.hasField(fieldName)) {
                visitor.reportError(line, col, TypeErrorSemantic.REDECLARACION, "Campo '" + fieldName + "' ya declarado en la estructura '" + structInfo.getName() + "'");
                return;
            }
            boolean isArray = ctx.LBRACK() != null;
            if (isArray) {
                DataType idx = visitor.visit(ctx.expression());
                if (!TypeChecker.isInt(idx) && idx != DataType.ERROR) {
                    visitor.reportError(line, col, TypeErrorSemantic.ARRAY_INDEX_ERROR, "Tamano de arreglo debe ser entero");
                }
                structInfo.addField(fieldName, t, null, true);
            } else {
                String structName = (t == DataType.STRUCT && ctx.type().ID() != null) ? ctx.type().ID().getText() : null;
                structInfo.addField(fieldName, t, structName, false);
            }
        } else {
            String structTypeName = ctx.ID(0).getText();
            String fieldName = ctx.ID(1).getText();
            if (structInfo.hasField(fieldName)) {
                visitor.reportError(line, col, TypeErrorSemantic.REDECLARACION, "Campo '" + fieldName + "' ya declarado en la estructura '" + structInfo.getName() + "'");
                return;
            }
            if (!visitor.getSymbolTable().structExists(structTypeName)) {
                visitor.reportError(line, col, TypeErrorSemantic.UNDECLARED, "Tipo no definido: '" + structTypeName + "'");
            }
            structInfo.addField(fieldName, DataType.STRUCT, structTypeName, false);
        }
    }

    public DataType visitFuncSection(YParser.FuncSectionContext ctx) {
        if (ctx == null) return DataType.VOID;

        boolean hasMain = false;
        for (YParser.FuncDefContext f : ctx.funcDef()) {
            String fName = (f.voidFunction() != null) ? f.voidFunction().ID().getText() :
                           (f.returnFunction() != null) ? f.returnFunction().ID().getText() : "";
            if ("principal".equalsIgnoreCase(fName) || "main".equalsIgnoreCase(fName)) {
                hasMain = true;
                break;
            }
        }

        for (YParser.FuncDefContext f : ctx.funcDef()) {
            visitor.visit(f);
        }

        YVisitorImpl yVisitor = (YVisitorImpl) visitor;
        if (!hasMain && !yVisitor.isImportContext()) {
            for (YParser.FuncDefContext f : ctx.funcDef()) {
                String fName = (f.voidFunction() != null) ? f.voidFunction().ID().getText() :
                               (f.returnFunction() != null) ? f.returnFunction().ID().getText() : "";
                YParser.ParameterListContext pl = (f.voidFunction() != null) ? f.voidFunction().parameterList() :
                                                  (f.returnFunction() != null) ? f.returnFunction().parameterList() : null;
                if (pl == null || pl.parameter() == null || pl.parameter().isEmpty()) {
                    yVisitor.executeFunctionCall(fName, java.util.Collections.emptyList());
                }
            }
        }

        return DataType.VOID;
    }

    public DataType visitVoidFunction(YParser.VoidFunctionContext ctx) {
        if (ctx == null) return DataType.VOID;
        defineFunction(ctx.ID().getText(), DataType.VOID, ctx.parameterList(), ctx.block(), ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), ctx);
        return DataType.VOID;
    }

    public DataType visitReturnFunction(YParser.ReturnFunctionContext ctx) {
        if (ctx == null) return DataType.VOID;
        DataType retType = ((YVisitorImpl) visitor).visitType(ctx.type());
        defineFunction(ctx.ID().getText(), retType, ctx.parameterList(), ctx.block(), ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), ctx);
        return DataType.VOID;
    }

    private void defineFunction(String name, DataType retType, YParser.ParameterListContext pl, YParser.BlockContext blk, int line, int col, Object astCtx) {
        if (visitor.getSymbolTable().getGlobalScope().lookupLocal(name) != null) {
            visitor.reportError(line, col, TypeErrorSemantic.REDECLARACION, "Funcion '" + name + "' ya definida");
        }

        List<DataType> paramTypes = new ArrayList<>();
        List<Symbol> paramSymbols = new ArrayList<>();

        if (pl != null) {
            for (YParser.ParameterContext p : pl.parameter()) {
                Symbol param = processParameter(p);
                paramTypes.add(param.type);
                paramSymbols.add(param);
            }
        }

        Symbol fs = new Symbol(name, retType, ScopeKind.GLOBAL, LanguageType.Y_LANG, null, line, col, paramTypes);
        fs.setAstContext(astCtx);
        visitor.getSymbolTable().declare(fs);

        YVisitorImpl yVisitor = (YVisitorImpl) visitor;
        Symbol prevFunc = yVisitor.getCurrentFunction();
        DataType prevRet = visitor.getCurrentFunctionReturnType();
        boolean prevInsideFunc = visitor.isInsideFunction();

        yVisitor.setCurrentFunction(fs);
        visitor.setCurrentFunctionReturnType(retType);
        visitor.setInsideFunction(true);

        boolean isMain = "principal".equalsIgnoreCase(name) || "main".equalsIgnoreCase(name);
        if (isMain) {
            visitor.setInsideMain(true);
        }

        visitor.getSymbolTable().pushScope(name);

        for (Symbol param : paramSymbols) {
            boolean ok = visitor.getSymbolTable().declare(param);
            if (!ok) {
                visitor.reportError(param.line, param.column, TypeErrorSemantic.REDECLARACION, "Parametro '" + param.name + "' ya definido");
            }
        }

        if (blk != null) {
            visitor.visit(blk);
        }

        visitor.getSymbolTable().popScope();

        if (isMain) {
            visitor.setInsideMain(false);
        }

        visitor.setShouldReturn(false);
        visitor.setShouldBreak(false);
        visitor.setShouldContinue(false);

        yVisitor.setCurrentFunction(prevFunc);
        visitor.setCurrentFunctionReturnType(prevRet);
        visitor.setInsideFunction(prevInsideFunc);
    }

    private Symbol processParameter(YParser.ParameterContext p) {
        int line = p.getStart().getLine();
        int col = p.getStart().getCharPositionInLine();

        if (p.type() != null && p.LBRACK() == null && p.LBRACE() == null) {
            DataType t = ((YVisitorImpl) visitor).visitType(p.type());
            String name = p.ID(0).getText();
            String structTypeName = (t == DataType.STRUCT && p.type().ID() != null) ? p.type().ID().getText() : null;
            return new Symbol(name, t, SymbolKind.PARAMETER, ScopeKind.LOCAL, LanguageType.Y_LANG, null, line, col, null, null, null, null, structTypeName);
        }

        if (p.LBRACK() != null && p.type() != null) {
            DataType t = ((YVisitorImpl) visitor).visitType(p.type());
            String name = p.ID(0).getText();
            return new Symbol(name, t, ScopeKind.LOCAL, LanguageType.Y_LANG,  null, line, col, (Integer) null);
        }

        if (p.LBRACE() != null && p.ID().size() >= 2) {
            String structType = p.ID(0).getText();
            String name = p.ID(1).getText();
            if (!visitor.getSymbolTable().structExists(structType)) {
                visitor.reportError(line, col, TypeErrorSemantic.UNDECLARED, "Tipo no definido: '" + structType + "'");
            }
            return new Symbol(name, structType, ScopeKind.LOCAL, LanguageType.Y_LANG, null, line, col);
        }

        return new Symbol("err", DataType.ERROR, SymbolKind.PARAMETER, ScopeKind.LOCAL, LanguageType.Y_LANG, null, line, col);
    }
}
