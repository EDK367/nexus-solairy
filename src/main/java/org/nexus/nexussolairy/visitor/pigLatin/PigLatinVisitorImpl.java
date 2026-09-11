package org.nexus.nexussolairy.visitor.pigLatin;

import org.antlr.v4.runtime.tree.ParseTree;
import org.nexus.nexussolairy.PigLatinParser;
import org.nexus.nexussolairy.PigLatinParserBaseVisitor;
import org.nexus.nexussolairy.YParser;
import org.nexus.nexussolairy.ZetarianoParser;
import org.nexus.nexussolairy.model.enums.DataType;
import org.nexus.nexussolairy.model.enums.LanguageType;
import org.nexus.nexussolairy.model.enums.ScopeKind;
import org.nexus.nexussolairy.model.enums.SymbolKind;
import org.nexus.nexussolairy.model.enums.TypeErrorSemantic;
import org.nexus.nexussolairy.model.semantic.*;
import org.nexus.nexussolairy.visitor.InputProvider;
import org.nexus.nexussolairy.visitor.VisitorContext;
import org.nexus.nexussolairy.visitor.pigLatin.expression.ExpressionEval;
import org.nexus.nexussolairy.visitor.pigLatin.expression.ExpressionSection;
import org.nexus.nexussolairy.visitor.pigLatin.io.IOSection;
import org.nexus.nexussolairy.visitor.pigLatin.statement.ConditionStatement;
import org.nexus.nexussolairy.visitor.pigLatin.statement.JumpStatement;
import org.nexus.nexussolairy.visitor.pigLatin.statement.LoopStatement;
import org.nexus.nexussolairy.visitor.pigLatin.variable.AssignmentDelegate;
import org.nexus.nexussolairy.visitor.pigLatin.variable.VariableSection;
import org.nexus.nexussolairy.visitor.yLanguage.YVisitorImpl;
import org.nexus.nexussolairy.visitor.zetariano.ZetarianoVisitorImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class PigLatinVisitorImpl extends PigLatinParserBaseVisitor<DataType> implements VisitorContext {

    // tabla precargada
    private final SymbolTable symbolTable;
    private DataType currentFunctionReturnType = DataType.VOID;
    private boolean insideLoop = false;
    private boolean insideMain = false;
    private boolean insideFunction = false;
    private boolean shouldBreak = false;
    private boolean shouldContinue = false;
    private boolean shouldReturn = false;
    private Object returnValue = null;
    private InputProvider inputProvider;
    private Consumer<String> livePrinter;
    private final IOSection ioDelegate;

    private final List<String> printOutput = new ArrayList<>();

    private final ExpressionSection expressionDelegate = new ExpressionSection(this);
    private final ExpressionEval expressionEval = new ExpressionEval(this);
    private final ProgramSection programDelegate = new ProgramSection(this);
    private final VariableSection variableDelegate = new VariableSection(this, expressionDelegate, expressionEval);
    private final AssignmentDelegate assignmentDelegate = new AssignmentDelegate(this, expressionEval);
    private final ConditionStatement conditionDelegate = new ConditionStatement(this, expressionEval);
    private final LoopStatement loopDelegate = new LoopStatement(this, expressionEval);
    private final JumpStatement jumpDelegate = new JumpStatement(this);

    public PigLatinVisitorImpl(SymbolTable symbolTable, InputProvider inputProvider, Consumer<String> livePrinter) {
        this.symbolTable = symbolTable != null ? symbolTable : new SymbolTable();
        this.inputProvider = inputProvider;
        this.livePrinter = livePrinter;
        this.ioDelegate = new IOSection(this, expressionDelegate, expressionEval, printOutput, inputProvider, livePrinter);
    }


    public PigLatinVisitorImpl(InputProvider inputProvider, Consumer<String> livePrinter) {
        this(new SymbolTable(), inputProvider, livePrinter);
    }

    public PigLatinVisitorImpl(InputProvider inputProvider) {
        this(new SymbolTable(), inputProvider, null);
    }

    public PigLatinVisitorImpl() {
        this(new SymbolTable(), () -> "", null);
    }

    @Override
    public DataType visit(ParseTree tree) {
        return super.visit(tree);
    }

    @Override
    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    @Override
    public boolean isInsideLoop() {
        return insideLoop;
    }

    @Override
    public void setInsideLoop(boolean value) {
        this.insideLoop = value;
    }

    @Override
    public DataType getCurrentFunctionReturnType() {
        return currentFunctionReturnType;
    }

    @Override
    public void setCurrentFunctionReturnType(DataType type) {
        this.currentFunctionReturnType = type;
    }

    @Override
    public boolean isShouldBreak() {
        return shouldBreak;
    }

    @Override
    public void setShouldBreak(boolean value) {
        this.shouldBreak = value;
    }

    @Override
    public boolean isShouldContinue() {
        return shouldContinue;
    }

    @Override
    public void setShouldContinue(boolean value) {
        this.shouldContinue = value;
    }

    @Override
    public boolean isShouldReturn() {
        return shouldReturn;
    }

    @Override
    public void setShouldReturn(boolean value) {
        this.shouldReturn = value;
    }

    @Override
    public Object getReturnValue() {
        return returnValue;
    }

    @Override
    public void setReturnValue(Object value) {
        this.returnValue = value;
    }

    @Override
    public Object executeFunctionCall(String name, List<Object> arguments) {
        Symbol funcSym = symbolTable.lookup(name);
        if (funcSym == null) return null;

        Object ast = funcSym.getAstContext();
        if (ast == null) return null;

        if (funcSym.language == LanguageType.Y_LANG) {
            YParser.BlockContext blk = null;
            YParser.ParameterListContext pl = null;
            if (ast instanceof YParser.VoidFunctionContext vfc) {
                blk = vfc.block();
                pl = vfc.parameterList();
            } else if (ast instanceof YParser.ReturnFunctionContext rfc) {
                blk = rfc.block();
                pl = rfc.parameterList();
            }
            if (blk == null) return null;

            SymbolTable funcSymbolTable = new SymbolTable(this.symbolTable.getGlobalScope());
            for (StructInfo s : this.symbolTable.getStructRegistry().values()) {
                funcSymbolTable.registerStruct(s);
            }
            for (ClassSymbol c : this.symbolTable.getClassRegistry().values()) {
                funcSymbolTable.registerClass(c);
            }

            funcSymbolTable.pushScope(name);

            if (pl != null && pl.parameter() != null) {
                int argCount = arguments != null ? arguments.size() : 0;
                for (int i = 0; i < pl.parameter().size(); i++) {
                    YParser.ParameterContext p = pl.parameter(i);
                    String pName = p.ID(p.ID().size() - 1).getText();
                    DataType pType = DataType.typeToken(p.type() != null ? p.type().getText() : "");
                    Object pVal = (i < argCount) ? arguments.get(i) : null;
                    Symbol paramSym = new Symbol(pName, pType, SymbolKind.VARIABLE, ScopeKind.LOCAL, LanguageType.Y_LANG, pVal, p.getStart().getLine(), p.getStart().getCharPositionInLine());
                    funcSymbolTable.declare(paramSym);
                }
            }

            YVisitorImpl funcVisitor = new YVisitorImpl(funcSymbolTable, this.inputProvider, this.livePrinter);
            funcVisitor.setCurrentFunction(funcSym);
            funcVisitor.setCurrentFunctionReturnType(funcSym.returnType != null ? funcSym.returnType : funcSym.type);
            funcVisitor.setInsideFunction(true);
            funcVisitor.setInsideMain(true);

            funcVisitor.visitBlock(blk);

            this.printOutput.addAll(funcVisitor.getPrintOutput());

            return funcVisitor.getReturnValue();
        }

        return null;
    }

    @Override
    public Object executeMethodCall(String varName, String methodName, List<Object> arguments) {
        Symbol varSym = symbolTable.lookup(varName);
        if (varSym == null) return null;
        if (!(varSym.value instanceof Map<?, ?>)) return null;
        @SuppressWarnings("unchecked")
        Map<String, Object> objInstance = (Map<String, Object>) varSym.value;

        String className = varSym.structTypeName;
        if (className == null) return null;

        ClassSymbol cls = symbolTable.lookupClass(className);
        if (cls == null) {
            Symbol s = symbolTable.getGlobalScope().resolve(className);
            if (s instanceof ClassSymbol cs) cls = cs;
        }
        if (cls == null) return null;

        List<Symbol> methods = cls.resolveMethod(methodName);
        if (methods == null || methods.isEmpty()) return null;

        FunctionSymbol targetMethod = null;
        int argCount = arguments != null ? arguments.size() : 0;
        for (Symbol m : methods) {
            if (m instanceof FunctionSymbol fs && fs.getParams().size() == argCount) {
                targetMethod = fs;
                break;
            }
        }
        if (targetMethod == null && methods.get(0) instanceof FunctionSymbol fs) {
            targetMethod = fs;
        }
        if (targetMethod == null) return null;

        Object ast = targetMethod.getAstContext();
        if (!(ast instanceof ZetarianoParser.MethodDeclContext mCtx)) return null;

        SymbolTable methodSymbolTable = new SymbolTable(this.symbolTable.getGlobalScope());
        for (ClassSymbol c : this.symbolTable.getClassRegistry().values()) {
            methodSymbolTable.registerClass(c);
        }
        for (StructInfo s : this.symbolTable.getStructRegistry().values()) {
            methodSymbolTable.registerStruct(s);
        }

        Scope instanceScope = new Scope("instance_" + varName, methodSymbolTable.getGlobalScope());
        for (Symbol f : cls.getFields().values()) {
            Object fVal = objInstance.containsKey(f.getName()) ? objInstance.get(f.getName()) : f.getValue();
            Symbol fSym = new Symbol(f.getName(), f.getType(), SymbolKind.VARIABLE, ScopeKind.LOCAL, LanguageType.ZETARIANO, fVal, f.line, f.column);
            instanceScope.declare(fSym);
        }
        methodSymbolTable.pushScope(instanceScope);

        methodSymbolTable.pushScope("method_" + methodName);
        for (int i = 0; i < targetMethod.getParams().size(); i++) {
            VariableSymbol param = targetMethod.getParams().get(i);
            Object pVal = (arguments != null && i < arguments.size()) ? arguments.get(i) : null;
            Symbol pSym = new Symbol(param.getName(), param.getType(), SymbolKind.VARIABLE, ScopeKind.LOCAL, LanguageType.ZETARIANO, pVal, param.line, param.column);
            methodSymbolTable.getCurrentScope().declare(pSym);
        }

        ZetarianoVisitorImpl methodVisitor = new ZetarianoVisitorImpl(methodSymbolTable, this.inputProvider, this.livePrinter);
        methodVisitor.setCurrentClass(cls);
        methodVisitor.setCurrentMethod(targetMethod);
        methodVisitor.setCurrentFunctionReturnType(targetMethod.getType());
        methodVisitor.setInsideFunction(true);

        methodVisitor.visitBlock(mCtx.block());

        for (Symbol f : instanceScope.getSymbols().values()) {
            objInstance.put(f.getName(), f.getValue());
        }

        this.printOutput.addAll(methodVisitor.getPrintOutput());

        return methodVisitor.getReturnValue();
    }

    @Override
    public void reportError(int line, int column, TypeErrorSemantic type, String message) {
        symbolTable.addError(line, column, type, message);
    }

    @Override
    public DataType resolveType(String typeText) {
        DataType primitive = DataType.typeToken(typeText);
        if (primitive != DataType.ERROR) return primitive;
        if (symbolTable.structExists(typeText)) return DataType.STRUCT;
        if (symbolTable.classExists(typeText)) return DataType.CLASS;
        return DataType.ERROR;
    }

    @Override
    public boolean isInsideMain() {
        return insideMain;
    }

    @Override
    public void setInsideMain(boolean value) {
        this.insideMain = value;
    }

    @Override
    public boolean isInsideFunction() {
        return insideFunction;
    }

    @Override
    public void setInsideFunction(boolean value) {
        this.insideFunction = value;
    }

    public boolean hasErrors() {
        return symbolTable.hasErrors();
    }

    public List<SemanticError> getErrors() {
        return symbolTable.getErrors();
    }

    public List<String> getPrintOutput() {
        return Collections.unmodifiableList(printOutput);
    }

    @Override
    public DataType visitProgram(PigLatinParser.ProgramContext ctx) {
        return programDelegate.visitProgram(ctx);
    }

    @Override
    public DataType visitImportSection(PigLatinParser.ImportSectionContext ctx) {
        return programDelegate.visitImportSection(ctx);
    }

    @Override
    public DataType visitImportStmt(PigLatinParser.ImportStmtContext ctx) {
        return programDelegate.visitImportStmt(ctx);
    }

    @Override
    public DataType visitPath(PigLatinParser.PathContext ctx) {
        return programDelegate.visitPath(ctx);
    }

    @Override
    public DataType visitMainSection(PigLatinParser.MainSectionContext ctx) {
        return programDelegate.visitMainSection(ctx);
    }

    @Override
    public DataType visitVarSection(PigLatinParser.VarSectionContext ctx) {
        return variableDelegate.visitVarSection(ctx);
    }

    @Override
    public DataType visitVarDecl(PigLatinParser.VarDeclContext ctx) {
        return variableDelegate.visitVarDecl(ctx);
    }

    @Override
    public DataType visitType(PigLatinParser.TypeContext ctx) {
        return variableDelegate.visitType(ctx);
    }

    @Override
    public DataType visitArrayInit(PigLatinParser.ArrayInitContext ctx) {
        return expressionDelegate.visitArrayInit(ctx);
    }

    @Override
    public DataType visitExpressionList(PigLatinParser.ExpressionListContext ctx) {
        return expressionDelegate.visitExpressionList(ctx);
    }

    @Override
    public DataType visitStatement(PigLatinParser.StatementContext ctx) {
        if (ctx == null) return DataType.VOID;
        if (shouldBreak || shouldContinue || shouldReturn) return DataType.VOID;
        if (ctx.varDecl() != null) return visit(ctx.varDecl());
        if (ctx.assignStmt() != null) return visit(ctx.assignStmt());
        if (ctx.ifStmt() != null) return visit(ctx.ifStmt());
        if (ctx.whileStmt() != null) return visit(ctx.whileStmt());
        if (ctx.doWhileStmt() != null) return visit(ctx.doWhileStmt());
        if (ctx.forStmt() != null) return visit(ctx.forStmt());
        if (ctx.printStmt() != null) return visit(ctx.printStmt());
        if (ctx.readStmt() != null) return visit(ctx.readStmt());
        if (ctx.breakStmt() != null) return visit(ctx.breakStmt());
        if (ctx.continueStmt() != null) return visit(ctx.continueStmt());
        if (ctx.expression() != null) {
            expressionEval.evalExpression(ctx.expression());
            return visit(ctx.expression());
        }
        return DataType.VOID;
    }

    @Override
    public DataType visitAssignStmt(PigLatinParser.AssignStmtContext ctx) {
        return assignmentDelegate.visitAssignStmt(ctx);
    }

    @Override
    public DataType visitIfStmt(PigLatinParser.IfStmtContext ctx) {
        return conditionDelegate.visitIfStmt(ctx);
    }

    @Override
    public DataType visitWhileStmt(PigLatinParser.WhileStmtContext ctx) {
        return loopDelegate.visitWhileStmt(ctx);
    }

    @Override
    public DataType visitDoWhileStmt(PigLatinParser.DoWhileStmtContext ctx) {
        return loopDelegate.visitDoWhileStmt(ctx);
    }

    @Override
    public DataType visitForStmt(PigLatinParser.ForStmtContext ctx) {
        return loopDelegate.visitForStmt(ctx);
    }

    @Override
    public DataType visitForInit(PigLatinParser.ForInitContext ctx) {
        return loopDelegate.visitForInit(ctx);
    }

    @Override
    public DataType visitForUpdate(PigLatinParser.ForUpdateContext ctx) {
        return loopDelegate.visitForUpdate(ctx);
    }

    @Override
    public DataType visitPrintStmt(PigLatinParser.PrintStmtContext ctx) {
        return ioDelegate.visitPrintStmt(ctx);
    }

    @Override
    public DataType visitReadStmt(PigLatinParser.ReadStmtContext ctx) {
        return ioDelegate.visitReadStmt(ctx);
    }

    @Override
    public DataType visitBreakStmt(PigLatinParser.BreakStmtContext ctx) {
        return jumpDelegate.visitBreakStmt(ctx);
    }

    @Override
    public DataType visitContinueStmt(PigLatinParser.ContinueStmtContext ctx) {
        return jumpDelegate.visitContinueStmt(ctx);
    }

    @Override
    public DataType visitExpression(PigLatinParser.ExpressionContext ctx) {
        return expressionDelegate.visitExpression(ctx);
    }

    @Override
    public DataType visitOrExpression(PigLatinParser.OrExpressionContext ctx) {
        return expressionDelegate.visitOrExpression(ctx);
    }

    @Override
    public DataType visitAndExpression(PigLatinParser.AndExpressionContext ctx) {
        return expressionDelegate.visitAndExpression(ctx);
    }

    @Override
    public DataType visitRelationalExpression(PigLatinParser.RelationalExpressionContext ctx) {
        return expressionDelegate.visitRelationalExpression(ctx);
    }

    @Override
    public DataType visitAdditiveExpression(PigLatinParser.AdditiveExpressionContext ctx) {
        return expressionDelegate.visitAdditiveExpression(ctx);
    }

    @Override
    public DataType visitMultiplicativeExpression(PigLatinParser.MultiplicativeExpressionContext ctx) {
        return expressionDelegate.visitMultiplicativeExpression(ctx);
    }

    @Override
    public DataType visitUnaryExpression(PigLatinParser.UnaryExpressionContext ctx) {
        return expressionDelegate.visitUnaryExpression(ctx);
    }

    @Override
    public DataType visitPostfixExpression(PigLatinParser.PostfixExpressionContext ctx) {
        return expressionDelegate.visitPostfixExpression(ctx);
    }

    @Override
    public DataType visitPrimaryExpression(PigLatinParser.PrimaryExpressionContext ctx) {
        return expressionDelegate.visitPrimaryExpression(ctx);
    }

    @Override
    public DataType visitLiteral(PigLatinParser.LiteralContext ctx) {
        return expressionDelegate.visitLiteral(ctx);
    }

    @Override
    public DataType visitStructLiteral(PigLatinParser.StructLiteralContext ctx) {
        return expressionDelegate.visitStructLiteral(ctx);
    }

    @Override
    public DataType visitArgumentList(PigLatinParser.ArgumentListContext ctx) {
        return expressionDelegate.visitArgumentList(ctx);
    }
}
