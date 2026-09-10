package org.nexus.nexussolairy.visitor.yLanguage;

import org.antlr.v4.runtime.tree.ParseTree;
import org.nexus.nexussolairy.YParser;
import org.nexus.nexussolairy.YParserBaseVisitor;
import org.nexus.nexussolairy.model.enums.DataType;
import org.nexus.nexussolairy.model.enums.TypeErrorSemantic;
import org.nexus.nexussolairy.model.semantic.SemanticError;
import org.nexus.nexussolairy.model.semantic.Symbol;
import org.nexus.nexussolairy.model.semantic.SymbolTable;
import org.nexus.nexussolairy.visitor.InputProvider;
import org.nexus.nexussolairy.visitor.VisitorContext;
import org.nexus.nexussolairy.visitor.yLanguage.expression.ExpressionEval;
import org.nexus.nexussolairy.visitor.yLanguage.expression.ExpressionSection;
import org.nexus.nexussolairy.visitor.yLanguage.io.IOSection;
import org.nexus.nexussolairy.visitor.yLanguage.statement.ConditionStatement;
import org.nexus.nexussolairy.visitor.yLanguage.statement.JumpStatement;
import org.nexus.nexussolairy.visitor.yLanguage.statement.LoopStatement;
import org.nexus.nexussolairy.visitor.yLanguage.variable.AssignmentDelegate;
import org.nexus.nexussolairy.visitor.yLanguage.variable.VariableSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class YVisitorImpl extends YParserBaseVisitor<DataType> implements VisitorContext {

    private final SymbolTable symbolTable = new SymbolTable();
    private DataType currentFunctionReturnType = DataType.VOID;
    private Symbol currentFunction = null;
    private boolean insideLoop = false;
    private boolean insideMain = false;
    private boolean insideFunction = false;
    private boolean insideSwitch = false;
    private boolean shouldBreak = false;
    private boolean shouldContinue = false;
    private boolean shouldReturn = false;
    private Object returnValue = null;
    private InputProvider inputProvider;
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

    public AssignmentDelegate getAssignmentDelegate() {
        return assignmentDelegate;
    }

    public VariableSection getVariableDelegate() {
        return variableDelegate;
    }

    public YVisitorImpl(InputProvider inputProvider, Consumer<String> livePrinter) {
        this.inputProvider = inputProvider;
        this.ioDelegate = new IOSection(this, expressionDelegate, expressionEval, printOutput, inputProvider, livePrinter);
    }

    public YVisitorImpl(InputProvider inputProvider) {
        this(inputProvider, null);
    }

    public YVisitorImpl() {
        this(() -> "", null);
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

    public boolean isInsideSwitch() {
        return insideSwitch;
    }

    public void setInsideSwitch(boolean value) {
        this.insideSwitch = value;
    }

    public Symbol getCurrentFunction() {
        return currentFunction;
    }

    public void setCurrentFunction(Symbol func) {
        this.currentFunction = func;
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
        return null;
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

    public List<String> getErrorMessages() {
        List<String> list = new ArrayList<>();
        for (SemanticError err : getErrors()) {
            list.add("Linea " + err.getLine() + ": " + err.getMessage());
        }
        return list;
    }

    public List<String> getPrintOutput() {
        return Collections.unmodifiableList(printOutput);
    }

    // ============================================================
    // PROGRAMA Y ESTRUCTURAS
    // ============================================================
    @Override
    public DataType visitProgram(YParser.ProgramContext ctx) {
        return programDelegate.visitProgram(ctx);
    }

    @Override
    public DataType visitStructSection(YParser.StructSectionContext ctx) {
        return programDelegate.visitStructSection(ctx);
    }

    @Override
    public DataType visitStructDef(YParser.StructDefContext ctx) {
        return programDelegate.visitStructDef(ctx);
    }

    @Override
    public DataType visitStructField(YParser.StructFieldContext ctx) {
        return programDelegate.visitStructField(ctx);
    }

    @Override
    public DataType visitFuncSection(YParser.FuncSectionContext ctx) {
        return programDelegate.visitFuncSection(ctx);
    }

    @Override
    public DataType visitVoidFunction(YParser.VoidFunctionContext ctx) {
        return programDelegate.visitVoidFunction(ctx);
    }

    @Override
    public DataType visitReturnFunction(YParser.ReturnFunctionContext ctx) {
        return programDelegate.visitReturnFunction(ctx);
    }

    // ============================================================
    // BLOQUE Y SENTENCIAS
    // ============================================================
    @Override
    public DataType visitBlock(YParser.BlockContext ctx) {
        if (ctx == null) return DataType.VOID;
        for (YParser.StatementContext s : ctx.statement()) {
            visit(s);
            if (shouldBreak || shouldContinue || shouldReturn) break;
        }
        return DataType.VOID;
    }

    @Override
    public DataType visitStatement(YParser.StatementContext ctx) {
        if (ctx == null) return DataType.VOID;
        if (shouldBreak || shouldContinue || shouldReturn) return DataType.VOID;

        if (ctx.varDecl() != null) return visit(ctx.varDecl());
        if (ctx.assignStmt() != null) return visit(ctx.assignStmt());
        if (ctx.ifStmt() != null) return visit(ctx.ifStmt());
        if (ctx.switchStmt() != null) return visit(ctx.switchStmt());
        if (ctx.forStmt() != null) return visit(ctx.forStmt());
        if (ctx.whileStmt() != null) return visit(ctx.whileStmt());
        if (ctx.doWhileStmt() != null) return visit(ctx.doWhileStmt());
        if (ctx.printStmt() != null) return visit(ctx.printStmt());
        if (ctx.readStmt() != null) return visit(ctx.readStmt());
        if (ctx.returnStmt() != null) return visit(ctx.returnStmt());
        if (ctx.breakStmt() != null) return visit(ctx.breakStmt());
        if (ctx.continueStmt() != null) return visit(ctx.continueStmt());
        if (ctx.structDef() != null) return visit(ctx.structDef());
        return DataType.VOID;
    }

    @Override
    public DataType visitVarDecl(YParser.VarDeclContext ctx) {
        return variableDelegate.visitVarDecl(ctx);
    }

    @Override
    public DataType visitType(YParser.TypeContext ctx) {
        return variableDelegate.visitType(ctx);
    }

    @Override
    public DataType visitAssignStmt(YParser.AssignStmtContext ctx) {
        return assignmentDelegate.visitAssignStmt(ctx);
    }

    // ============================================================
    // CONTROL DE FLUJO
    // ============================================================
    @Override
    public DataType visitIfStmt(YParser.IfStmtContext ctx) {
        return conditionDelegate.visitIfStmt(ctx);
    }

    @Override
    public DataType visitSwitchStmt(YParser.SwitchStmtContext ctx) {
        return conditionDelegate.visitSwitchStmt(ctx);
    }

    @Override
    public DataType visitCaseBranch(YParser.CaseBranchContext ctx) {
        return conditionDelegate.visitCaseBranch(ctx);
    }

    @Override
    public DataType visitDefaultBranch(YParser.DefaultBranchContext ctx) {
        return conditionDelegate.visitDefaultBranch(ctx);
    }

    @Override
    public DataType visitForStmt(YParser.ForStmtContext ctx) {
        return loopDelegate.visitForStmt(ctx);
    }

    @Override
    public DataType visitForInit(YParser.ForInitContext ctx) {
        return loopDelegate.visitForInit(ctx);
    }

    @Override
    public DataType visitForUpdate(YParser.ForUpdateContext ctx) {
        return loopDelegate.visitForUpdate(ctx);
    }

    @Override
    public DataType visitWhileStmt(YParser.WhileStmtContext ctx) {
        return loopDelegate.visitWhileStmt(ctx);
    }

    @Override
    public DataType visitDoWhileStmt(YParser.DoWhileStmtContext ctx) {
        return loopDelegate.visitDoWhileStmt(ctx);
    }

    @Override
    public DataType visitReturnStmt(YParser.ReturnStmtContext ctx) {
        return jumpDelegate.visitReturnStmt(ctx);
    }

    @Override
    public DataType visitBreakStmt(YParser.BreakStmtContext ctx) {
        return jumpDelegate.visitBreakStmt(ctx);
    }

    @Override
    public DataType visitContinueStmt(YParser.ContinueStmtContext ctx) {
        return jumpDelegate.visitContinueStmt(ctx);
    }

    @Override
    public DataType visitPrintStmt(YParser.PrintStmtContext ctx) {
        return ioDelegate.visitPrintStmt(ctx);
    }

    @Override
    public DataType visitReadStmt(YParser.ReadStmtContext ctx) {
        return ioDelegate.visitReadStmt(ctx);
    }

    // ============================================================
    // EXPRESIONES
    // ============================================================
    @Override
    public DataType visitExpression(YParser.ExpressionContext ctx) {
        return expressionDelegate.visitExpression(ctx);
    }

    @Override
    public DataType visitOrExpression(YParser.OrExpressionContext ctx) {
        return expressionDelegate.visitOrExpression(ctx);
    }

    @Override
    public DataType visitAndExpression(YParser.AndExpressionContext ctx) {
        return expressionDelegate.visitAndExpression(ctx);
    }

    @Override
    public DataType visitRelationalExpression(YParser.RelationalExpressionContext ctx) {
        return expressionDelegate.visitRelationalExpression(ctx);
    }

    @Override
    public DataType visitAdditiveExpression(YParser.AdditiveExpressionContext ctx) {
        return expressionDelegate.visitAdditiveExpression(ctx);
    }

    @Override
    public DataType visitMultiplicativeExpression(YParser.MultiplicativeExpressionContext ctx) {
        return expressionDelegate.visitMultiplicativeExpression(ctx);
    }

    @Override
    public DataType visitUnaryExpression(YParser.UnaryExpressionContext ctx) {
        return expressionDelegate.visitUnaryExpression(ctx);
    }

    @Override
    public DataType visitPostfixExpression(YParser.PostfixExpressionContext ctx) {
        return expressionDelegate.visitPostfixExpression(ctx);
    }

    @Override
    public DataType visitPrimaryExpression(YParser.PrimaryExpressionContext ctx) {
        return expressionDelegate.visitPrimaryExpression(ctx);
    }

    @Override
    public DataType visitLiteral(YParser.LiteralContext ctx) {
        return expressionDelegate.visitLiteral(ctx);
    }

    @Override
    public DataType visitStructLiteral(YParser.StructLiteralContext ctx) {
        return expressionDelegate.visitStructLiteral(ctx);
    }

    @Override
    public DataType visitArrayInit(YParser.ArrayInitContext ctx) {
        return expressionDelegate.visitArrayInit(ctx);
    }

    @Override
    public DataType visitArgumentList(YParser.ArgumentListContext ctx) {
        return expressionDelegate.visitArgumentList(ctx);
    }

    @Override
    public DataType visitExpressionList(YParser.ExpressionListContext ctx) {
        return expressionDelegate.visitExpressionList(ctx);
    }
}
