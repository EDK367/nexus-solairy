package org.nexus.nexussolairy.visitor.zetariano;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.nexus.nexussolairy.ZetarianoParser;
import org.nexus.nexussolairy.ZetarianoParserBaseVisitor;
import org.nexus.nexussolairy.model.enums.*;
import org.nexus.nexussolairy.model.semantic.*;
import org.nexus.nexussolairy.visitor.InputProvider;
import org.nexus.nexussolairy.visitor.VisitorContext;
import org.nexus.nexussolairy.visitor.zetariano.expression.ExpressionEval;
import org.nexus.nexussolairy.visitor.zetariano.expression.ExpressionSection;
import org.nexus.nexussolairy.visitor.zetariano.io.IOSection;
import org.nexus.nexussolairy.visitor.zetariano.statement.ConditionStatement;
import org.nexus.nexussolairy.visitor.zetariano.statement.JumpStatement;
import org.nexus.nexussolairy.visitor.zetariano.statement.LoopStatement;
import org.nexus.nexussolairy.visitor.zetariano.variable.AssignmentDelegate;
import org.nexus.nexussolairy.visitor.zetariano.variable.VariableSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class ZetarianoVisitorImpl extends ZetarianoParserBaseVisitor<DataType> implements VisitorContext {

    private final SymbolTable symbolTable;
    private DataType currentFunctionReturnType = DataType.VOID;
    private ClassSymbol currentClass = null;
    private FunctionSymbol currentMethod = null;
    private boolean insideLoop = false;
    private boolean insideSwitch = false;
    private boolean insideMain = false;
    private boolean insideFunction = false;
    private boolean shouldBreak = false;
    private boolean shouldContinue = false;
    private boolean shouldReturn = false;
    private Object returnValue = null;
    private String fileName = null;
    private InputProvider inputProvider;
    private Consumer<String> livePrinter;
    private final List<String> printOutput = new ArrayList<>();

    private final ExpressionSection expressionDelegate;
    private final ExpressionEval expressionEval;
    private final ProgramSection programDelegate;
    private final VariableSection variableDelegate;
    private final AssignmentDelegate assignmentDelegate;
    private final ConditionStatement conditionDelegate;
    private final LoopStatement loopDelegate;
    private final JumpStatement jumpDelegate;
    private final IOSection ioDelegate;

    public ZetarianoVisitorImpl(SymbolTable symbolTable, InputProvider inputProvider, Consumer<String> livePrinter) {
        this.symbolTable = symbolTable != null ? symbolTable : new SymbolTable();
        this.inputProvider = inputProvider;
        this.livePrinter = livePrinter;
        this.expressionDelegate = new ExpressionSection(this);
        this.expressionEval = new ExpressionEval(this);
        this.programDelegate = new ProgramSection(this);
        this.variableDelegate = new VariableSection(this);
        this.assignmentDelegate = new AssignmentDelegate(this, expressionEval);
        this.conditionDelegate = new ConditionStatement(this, expressionEval);
        this.loopDelegate = new LoopStatement(this, expressionEval);
        this.jumpDelegate = new JumpStatement(this);
        this.ioDelegate = new IOSection(this, expressionDelegate, expressionEval, printOutput, inputProvider, livePrinter);
    }

    public ZetarianoVisitorImpl(InputProvider inputProvider, Consumer<String> livePrinter) {
        this(new SymbolTable(), inputProvider, livePrinter);
    }

    public ZetarianoVisitorImpl(InputProvider inputProvider) {
        this(new SymbolTable(), inputProvider, null);
    }

    public ZetarianoVisitorImpl() {
        this(new SymbolTable(), () -> "", null);
    }

    public void check(ParseTree tree) {
        visit(tree);
    }

    public ExpressionSection getExpressionDelegate() {
        return expressionDelegate;
    }

    public ExpressionEval getExpressionEval() {
        return expressionEval;
    }

    public ProgramSection getProgramDelegate() {
        return programDelegate;
    }

    public VariableSection getVariableDelegate() {
        return variableDelegate;
    }

    public AssignmentDelegate getAssignmentDelegate() {
        return assignmentDelegate;
    }

    public ConditionStatement getConditionDelegate() {
        return conditionDelegate;
    }

    public LoopStatement getLoopDelegate() {
        return loopDelegate;
    }

    public JumpStatement getJumpDelegate() {
        return jumpDelegate;
    }

    public IOSection getIoDelegate() {
        return ioDelegate;
    }

    public InputProvider getInputProvider() {
        return inputProvider;
    }

    public Consumer<String> getLivePrinter() {
        return livePrinter;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public ClassSymbol getCurrentClass() {
        return currentClass;
    }

    public void setCurrentClass(ClassSymbol currentClass) {
        this.currentClass = currentClass;
    }

    public FunctionSymbol getCurrentMethod() {
        return currentMethod;
    }

    public void setCurrentMethod(FunctionSymbol currentMethod) {
        this.currentMethod = currentMethod;
    }

    public boolean isInsideSwitch() {
        return insideSwitch;
    }

    public void setInsideSwitch(boolean insideSwitch) {
        this.insideSwitch = insideSwitch;
    }

    public void pushScope(String name) {
        symbolTable.pushScope(name);
    }

    public void popScope() {
        symbolTable.popScope();
    }

    public Symbol resolveSymbol(String name) {
        Symbol s = symbolTable.getCurrentScope().lookup(name);
        if (s != null) return s;
        if (currentClass != null) {
            if ("this".equals(name)) {
                return new Symbol("this", DataType.CLASS, SymbolKind.VARIABLE, ScopeKind.LOCAL, LanguageType.ZETARIANO, null, 0, 0, currentClass.getName());
            }
            Symbol field = currentClass.resolveField(name);
            if (field != null) return field;
            List<Symbol> methods = currentClass.resolveMethod(name);
            if (methods != null && !methods.isEmpty()) return methods.get(0);
        }
        s = symbolTable.lookup(name);
        if (s != null) return s;
        s = symbolTable.getGlobalScope().resolve(name);
        if (s != null) return s;
        return symbolTable.lookupClass(name);
    }

    public void reportError(ParserRuleContext ctx, TypeErrorSemantic type, String message) {
        int line = ctx != null && ctx.getStart() != null ? ctx.getStart().getLine() : 1;
        int col = ctx != null && ctx.getStart() != null ? ctx.getStart().getCharPositionInLine() : 0;
        reportError(line, col, type, message);
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
        if (symbolTable.classExists(typeText) || symbolTable.getGlobalScope().resolve(typeText) != null) return DataType.STRUCT;
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

    @Override
    public DataType visitProgram(ZetarianoParser.ProgramContext ctx) {
        return programDelegate.visitProgram(ctx);
    }

    @Override
    public DataType visitClassDecl(ZetarianoParser.ClassDeclContext ctx) {
        return programDelegate.visitClassDecl(ctx);
    }

    @Override
    public DataType visitClassBody(ZetarianoParser.ClassBodyContext ctx) {
        return programDelegate.visitClassBody(ctx);
    }

    @Override
    public DataType visitFieldDecl(ZetarianoParser.FieldDeclContext ctx) {
        return programDelegate.visitFieldDecl(ctx);
    }

    @Override
    public DataType visitConstructorDecl(ZetarianoParser.ConstructorDeclContext ctx) {
        return programDelegate.visitConstructorDecl(ctx);
    }

    @Override
    public DataType visitMethodDecl(ZetarianoParser.MethodDeclContext ctx) {
        return programDelegate.visitMethodDecl(ctx);
    }

    @Override
    public DataType visitBlock(ZetarianoParser.BlockContext ctx) {
        if (ctx == null) return DataType.VOID;
        pushScope("block");
        if (ctx.statement() != null) {
            for (ZetarianoParser.StatementContext s : ctx.statement()) {
                visitStatement(s);
                if (shouldBreak || shouldContinue || shouldReturn) break;
            }
        }
        popScope();
        return DataType.VOID;
    }

    @Override
    public DataType visitStatement(ZetarianoParser.StatementContext ctx) {
        if (ctx == null) return DataType.VOID;
        if (shouldBreak || shouldContinue || shouldReturn) return DataType.VOID;
        if (ctx.varDecl() != null) return visitVarDecl(ctx.varDecl());
        if (ctx.assignStmt() != null) return visitAssignStmt(ctx.assignStmt());
        if (ctx.ifStmt() != null) return visitIfStmt(ctx.ifStmt());
        if (ctx.switchStmt() != null) return visitSwitchStmt(ctx.switchStmt());
        if (ctx.forStmt() != null) return visitForStmt(ctx.forStmt());
        if (ctx.whileStmt() != null) return visitWhileStmt(ctx.whileStmt());
        if (ctx.doWhileStmt() != null) return visitDoWhileStmt(ctx.doWhileStmt());
        if (ctx.returnStmt() != null) return visitReturnStmt(ctx.returnStmt());
        if (ctx.breakStmt() != null) return visitBreakStmt(ctx.breakStmt());
        if (ctx.continueStmt() != null) return visitContinueStmt(ctx.continueStmt());
        if (ctx.printStmt() != null) return visitPrintStmt(ctx.printStmt());
        if (ctx.readStmt() != null) return visitReadStmt(ctx.readStmt());
        if (ctx.expression() != null) return visitExpression(ctx.expression());
        if (ctx.block() != null) return visitBlock(ctx.block());
        return DataType.VOID;
    }

    @Override
    public DataType visitVarDecl(ZetarianoParser.VarDeclContext ctx) {
        return variableDelegate.visitVarDecl(ctx);
    }

    @Override
    public DataType visitType(ZetarianoParser.TypeContext ctx) {
        return variableDelegate.visitType(ctx);
    }

    @Override
    public DataType visitAssignStmt(ZetarianoParser.AssignStmtContext ctx) {
        return assignmentDelegate.visitAssignStmt(ctx);
    }

    @Override
    public DataType visitIfStmt(ZetarianoParser.IfStmtContext ctx) {
        return conditionDelegate.visitIfStmt(ctx);
    }

    @Override
    public DataType visitIfBody(ZetarianoParser.IfBodyContext ctx) {
        return conditionDelegate.visitIfBody(ctx);
    }

    @Override
    public DataType visitSwitchStmt(ZetarianoParser.SwitchStmtContext ctx) {
        return conditionDelegate.visitSwitchStmt(ctx);
    }

    @Override
    public DataType visitCaseBranch(ZetarianoParser.CaseBranchContext ctx) {
        return conditionDelegate.visitCaseBranch(ctx);
    }

    @Override
    public DataType visitDefaultBranch(ZetarianoParser.DefaultBranchContext ctx) {
        return conditionDelegate.visitDefaultBranch(ctx);
    }

    @Override
    public DataType visitForStmt(ZetarianoParser.ForStmtContext ctx) {
        return loopDelegate.visitForStmt(ctx);
    }

    @Override
    public DataType visitForInit(ZetarianoParser.ForInitContext ctx) {
        return loopDelegate.visitForInit(ctx);
    }

    @Override
    public DataType visitForUpdate(ZetarianoParser.ForUpdateContext ctx) {
        return loopDelegate.visitForUpdate(ctx);
    }

    @Override
    public DataType visitWhileStmt(ZetarianoParser.WhileStmtContext ctx) {
        return loopDelegate.visitWhileStmt(ctx);
    }

    @Override
    public DataType visitDoWhileStmt(ZetarianoParser.DoWhileStmtContext ctx) {
        return loopDelegate.visitDoWhileStmt(ctx);
    }

    @Override
    public DataType visitPrintStmt(ZetarianoParser.PrintStmtContext ctx) {
        return ioDelegate.visitPrintStmt(ctx);
    }

    @Override
    public DataType visitReadStmt(ZetarianoParser.ReadStmtContext ctx) {
        return ioDelegate.visitReadStmt(ctx);
    }

    @Override
    public DataType visitReturnStmt(ZetarianoParser.ReturnStmtContext ctx) {
        return jumpDelegate.visitReturnStmt(ctx);
    }

    @Override
    public DataType visitBreakStmt(ZetarianoParser.BreakStmtContext ctx) {
        return jumpDelegate.visitBreakStmt(ctx);
    }

    @Override
    public DataType visitContinueStmt(ZetarianoParser.ContinueStmtContext ctx) {
        return jumpDelegate.visitContinueStmt(ctx);
    }

    @Override
    public DataType visitExpression(ZetarianoParser.ExpressionContext ctx) {
        Type t = expressionDelegate.visitExpression(ctx);
        return t != null ? t.getDataType() : DataType.ERROR;
    }

    @Override
    public DataType visitConditionalExpr(ZetarianoParser.ConditionalExprContext ctx) {
        Type t = expressionDelegate.visitConditionalExpr(ctx);
        return t != null ? t.getDataType() : DataType.ERROR;
    }

    @Override
    public DataType visitOrExpr(ZetarianoParser.OrExprContext ctx) {
        Type t = expressionDelegate.visitOrExpr(ctx);
        return t != null ? t.getDataType() : DataType.ERROR;
    }

    @Override
    public DataType visitAndExpr(ZetarianoParser.AndExprContext ctx) {
        Type t = expressionDelegate.visitAndExpr(ctx);
        return t != null ? t.getDataType() : DataType.ERROR;
    }

    @Override
    public DataType visitEqExpr(ZetarianoParser.EqExprContext ctx) {
        Type t = expressionDelegate.visitEqExpr(ctx);
        return t != null ? t.getDataType() : DataType.ERROR;
    }

    @Override
    public DataType visitRelExpr(ZetarianoParser.RelExprContext ctx) {
        Type t = expressionDelegate.visitRelExpr(ctx);
        return t != null ? t.getDataType() : DataType.ERROR;
    }

    @Override
    public DataType visitAddExpr(ZetarianoParser.AddExprContext ctx) {
        Type t = expressionDelegate.visitAddExpr(ctx);
        return t != null ? t.getDataType() : DataType.ERROR;
    }

    @Override
    public DataType visitMulExpr(ZetarianoParser.MulExprContext ctx) {
        Type t = expressionDelegate.visitMulExpr(ctx);
        return t != null ? t.getDataType() : DataType.ERROR;
    }

    @Override
    public DataType visitUnaryExpr(ZetarianoParser.UnaryExprContext ctx) {
        Type t = expressionDelegate.visitUnaryExpr(ctx);
        return t != null ? t.getDataType() : DataType.ERROR;
    }

    @Override
    public DataType visitPostfixExpr(ZetarianoParser.PostfixExprContext ctx) {
        Type t = expressionDelegate.visitPostfixExpr(ctx);
        return t != null ? t.getDataType() : DataType.ERROR;
    }

    @Override
    public DataType visitPrimaryExpr(ZetarianoParser.PrimaryExprContext ctx) {
        Type t = expressionDelegate.visitPrimaryExpr(ctx);
        return t != null ? t.getDataType() : DataType.ERROR;
    }

    @Override
    public DataType visitNewExpr(ZetarianoParser.NewExprContext ctx) {
        Type t = expressionDelegate.visitNewExpr(ctx);
        return t != null ? t.getDataType() : DataType.ERROR;
    }

    @Override
    public DataType visitArrayInit(ZetarianoParser.ArrayInitContext ctx) {
        Type t = expressionDelegate.visitArrayInit(ctx);
        return t != null ? t.getDataType() : DataType.ERROR;
    }

    @Override
    public DataType visitArgList(ZetarianoParser.ArgListContext ctx) {
        Type t = expressionDelegate.visitArgList(ctx);
        return t != null ? t.getDataType() : DataType.ERROR;
    }

    @Override
    public DataType visitLiteral(ZetarianoParser.LiteralContext ctx) {
        Type t = expressionDelegate.visitLiteral(ctx);
        return t != null ? t.getDataType() : DataType.ERROR;
    }
}
