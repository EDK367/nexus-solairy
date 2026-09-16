package org.nexus.nexussolairy.c3d;

import org.antlr.v4.runtime.tree.ParseTree;
import org.nexus.nexussolairy.PigLatinParser;
import org.nexus.nexussolairy.PigLatinParserBaseVisitor;
import org.nexus.nexussolairy.model.c3d.C3DProgram;
import org.nexus.nexussolairy.model.c3d.OpCode;

import java.util.ArrayDeque;
import java.util.Deque;

public class PigLatinC3DVisitor extends PigLatinParserBaseVisitor<String> {

    private final C3DProgram program;
    private final MemoryLayout memory;
    private String currentRoutine = "MAIOR";

    private final Deque<String> loopStartLabels = new ArrayDeque<>();
    private final Deque<String> loopExitLabels = new ArrayDeque<>();
    private final Deque<String> loopUpdateLabels = new ArrayDeque<>();

    private final org.nexus.nexussolairy.model.semantic.SymbolTable symbolTable;

    public PigLatinC3DVisitor(C3DProgram program, MemoryLayout memory) {
        this(program, memory, null);
    }

    public PigLatinC3DVisitor(C3DProgram program, MemoryLayout memory, org.nexus.nexussolairy.model.semantic.SymbolTable symbolTable) {
        this.program = program;
        this.memory = memory;
        this.symbolTable = symbolTable;
    }

    public C3DProgram getProgram() {
        return program;
    }

    public MemoryLayout getMemory() {
        return memory;
    }

    @Override
    public String visitProgram(PigLatinParser.ProgramContext ctx) {
        currentRoutine = "MAIOR";
        program.emit(OpCode.LABEL, "", "", "main_entry", ctx.getStart().getLine(), "Sección MAIOR");
        if (ctx.varSection() != null) {
            visit(ctx.varSection());
        }
        if (ctx.mainSection() != null) {
            visit(ctx.mainSection());
        }
        return null;
    }

    @Override
    public String visitMainSection(PigLatinParser.MainSectionContext ctx) {
        currentRoutine = "MAIOR";
        if (ctx.statement() != null) {
            for (PigLatinParser.StatementContext stmt : ctx.statement()) {
                visit(stmt);
            }
        }
        return null;
    }

    @Override
    public String visitVarDecl(PigLatinParser.VarDeclContext ctx) {
        int line = ctx.getStart().getLine();
        String varName = ctx.ID(0).getText();

        if (ctx.SERIES() != null) {
            String sizeTemp = visit(ctx.expression());
            int offset = memory.isGlobal(varName, currentRoutine) ?
                    memory.declareGlobal(varName) : memory.declareLocal(currentRoutine, varName);

            String heapRef = program.newTemp();
            program.emit(OpCode.ASSIGN, "H", "", heapRef, line, "Base arreglo " + varName);
            program.emit(OpCode.HEAP_WRITE, "H", sizeTemp, "", line, "Guardar tamaño");
            
            String newH = program.newTemp();
            program.emit(OpCode.ADD, "H", "1", newH, line);
            program.emit(OpCode.ADD, newH, sizeTemp, "H", line, "Avanzar Heap");

            if (memory.isGlobal(varName, currentRoutine)) {
                program.emit(OpCode.STACK_WRITE, String.valueOf(offset), heapRef, "", line);
            } else {
                String pAddr = program.newTemp();
                program.emit(OpCode.ADD, "P", String.valueOf(offset), pAddr, line);
                program.emit(OpCode.STACK_WRITE, pAddr, heapRef, "", line);
            }
            return null;
        }

        if (ctx.type() != null) {
            String tName = ctx.type().getText();
            if (isStringType(tName)) {
                stringVars.add(varName);
            } else {
                varClasses.put(varName, tName);
            }
        }

        int offset = memory.isGlobal(varName, currentRoutine) ?
                memory.declareGlobal(varName) : memory.declareLocal(currentRoutine, varName);

        String valTemp = "0";
        if (ctx.expression() != null) {
            valTemp = visit(ctx.expression());
            if (valTemp == null || valTemp.isEmpty()) valTemp = "0";
        } else if (ctx.NOVUS() != null) {
            String clsName = ctx.ID(1).getText();
            varClasses.put(varName, clsName);
            valTemp = allocateAndConstruct(clsName, ctx.argumentList(), line);
        }

        if (memory.isGlobal(varName, currentRoutine)) {
            program.emit(OpCode.STACK_WRITE, String.valueOf(offset), valTemp, "", line, "Var global " + varName);
        } else {
            String pAddr = program.newTemp();
            program.emit(OpCode.ADD, "P", String.valueOf(offset), pAddr, line);
            program.emit(OpCode.STACK_WRITE, pAddr, valTemp, "", line, "Var local " + varName);
        }
        return null;
    }

    private String allocateObject(String clsName, int line) {
        String objRef = program.newTemp();
        program.emit(OpCode.ASSIGN, "H", "", objRef, line, "Instancia novus " + clsName);
        String newH = program.newTemp();
        program.emit(OpCode.ADD, "H", "8", newH, line);
        program.emit(OpCode.ASSIGN, newH, "", "H", line);
        return objRef;
    }

    private String allocateAndConstruct(String clsName, PigLatinParser.ArgumentListContext argList, int line) {
        String objRef = allocateObject(clsName, line);

        java.util.List<String> argTemps = new java.util.ArrayList<>();
        if (argList != null && argList.expression() != null) {
            for (PigLatinParser.ExpressionContext argExpr : argList.expression()) {
                argTemps.add(visit(argExpr));
            }
        }

        int frameOffset = memory.getCallFrameOffset(currentRoutine);
        String tNew = program.newTemp();
        program.emit(OpCode.ADD, "P", String.valueOf(frameOffset), tNew, line, "Nuevo P para ctor");

        String thisAddr = program.newTemp();
        program.emit(OpCode.ADD, tNew, "1", thisAddr, line);
        program.emit(OpCode.STACK_WRITE, thisAddr, safe(objRef), "", line, "Pass 'this'");

        int pIdx = 2;
        for (String arg : argTemps) {
            String pAddr = program.newTemp();
            program.emit(OpCode.ADD, tNew, String.valueOf(pIdx++), pAddr, line);
            program.emit(OpCode.STACK_WRITE, pAddr, safe(arg), "", line, "Ctor param " + (pIdx - 1));
        }

        program.emit(OpCode.ADD, "P", String.valueOf(frameOffset), "P", line, "Avanzar P");
        program.emit(OpCode.CALL, clsName + "_ctor", "", "", line);
        program.emit(OpCode.SUB, "P", String.valueOf(frameOffset), "P", line, "Restaurar P");

        return objRef;
    }

    @Override
    public String visitAssignStmt(PigLatinParser.AssignStmtContext ctx) {
        int line = ctx.getStart().getLine();
        String valTemp = visit(ctx.expression(ctx.expression().size() - 1));

        if (ctx.DOT() != null) {
            String objName = ctx.ID(0).getText();
            String objPtr = loadVar(objName, line);
            int fieldOffset = 0;
            String fieldAddr = program.newTemp();
            program.emit(OpCode.ADD, objPtr, String.valueOf(fieldOffset), fieldAddr, line);
            program.emit(OpCode.HEAP_WRITE, fieldAddr, valTemp, "", line);
        } else if (ctx.LBRACK() != null) {
            String arrName = ctx.ID(0).getText();
            String arrPtr = loadVar(arrName, line);
            String idxTemp = visit(ctx.expression(0));
            String elemAddr = program.newTemp();
            program.emit(OpCode.ADD, arrPtr, "1", elemAddr, line);
            program.emit(OpCode.ADD, elemAddr, idxTemp, elemAddr, line);
            program.emit(OpCode.HEAP_WRITE, elemAddr, valTemp, "", line);
        } else {
            String varName = ctx.ID(0).getText();
            storeVar(varName, valTemp, line);
        }
        return null;
    }

    private String loadVar(String varName, int line) {
        String res = program.newTemp();
        if (memory.isGlobal(varName, currentRoutine)) {
            int offset = memory.getGlobalOffset(varName);
            program.emit(OpCode.STACK_READ, String.valueOf(offset), "", res, line, "Leer global " + varName);
        } else {
            int offset = memory.getLocalOffset(currentRoutine, varName);
            if (offset < 0) offset = memory.declareLocal(currentRoutine, varName);
            String pAddr = program.newTemp();
            program.emit(OpCode.ADD, "P", String.valueOf(offset), pAddr, line);
            program.emit(OpCode.STACK_READ, pAddr, "", res, line, "Leer local " + varName);
        }
        return res;
    }

    private void storeVar(String varName, String valTemp, int line) {
        if (memory.isGlobal(varName, currentRoutine)) {
            int offset = memory.getGlobalOffset(varName);
            if (offset < 0) offset = memory.declareGlobal(varName);
            program.emit(OpCode.STACK_WRITE, String.valueOf(offset), valTemp, "", line, "Escribir global " + varName);
        } else {
            int offset = memory.getLocalOffset(currentRoutine, varName);
            if (offset < 0) offset = memory.declareLocal(currentRoutine, varName);
            String pAddr = program.newTemp();
            program.emit(OpCode.ADD, "P", String.valueOf(offset), pAddr, line);
            program.emit(OpCode.STACK_WRITE, pAddr, valTemp, "", line, "Escribir local " + varName);
        }
    }

    @Override
    public String visitIfStmt(PigLatinParser.IfStmtContext ctx) {
        int line = ctx.getStart().getLine();
        String labelExit = program.newLabel();
        
        java.util.List<java.util.List<PigLatinParser.StatementContext>> branchStatements = new java.util.ArrayList<>();
        java.util.List<PigLatinParser.StatementContext> currentBlock = null;

        if (ctx.children != null) {
            for (ParseTree child : ctx.children) {
                if (child instanceof org.antlr.v4.runtime.tree.TerminalNode tn) {
                    if (tn.getSymbol().getType() == PigLatinParser.LBRACE) {
                        currentBlock = new java.util.ArrayList<>();
                        branchStatements.add(currentBlock);
                    }
                } else if (child instanceof PigLatinParser.StatementContext stmt) {
                    if (currentBlock != null) {
                        currentBlock.add(stmt);
                    }
                }
            }
        }

        for (int i = 0; i < branchStatements.size(); i++) {
            if (i < ctx.expression().size()) {
                String labelNext = program.newLabel();
                String condTemp = visit(ctx.expression(i));
                program.emit(OpCode.IF_FALSE, condTemp, "", labelNext, line);

                for (PigLatinParser.StatementContext stmt : branchStatements.get(i)) {
                    visit(stmt);
                }
                program.emit(OpCode.GOTO, "", "", labelExit, line);
                program.emit(OpCode.LABEL, "", "", labelNext, line);
            } else {
                for (PigLatinParser.StatementContext stmt : branchStatements.get(i)) {
                    visit(stmt);
                }
            }
        }
        program.emit(OpCode.LABEL, "", "", labelExit, line);
        return null;
    }

    @Override
    public String visitWhileStmt(PigLatinParser.WhileStmtContext ctx) {
        int line = ctx.getStart().getLine();
        String labelStart = program.newLabel();
        String labelExit = program.newLabel();

        loopStartLabels.push(labelStart);
        loopExitLabels.push(labelExit);
        loopUpdateLabels.push(labelStart);

        program.emit(OpCode.LABEL, "", "", labelStart, line, "Inicio While");
        String condTemp = visit(ctx.expression());
        program.emit(OpCode.IF_FALSE, condTemp, "", labelExit, line);

        if (ctx.statement() != null) {
            for (PigLatinParser.StatementContext stmt : ctx.statement()) {
                visit(stmt);
            }
        }
        program.emit(OpCode.GOTO, "", "", labelStart, line);
        program.emit(OpCode.LABEL, "", "", labelExit, line, "Fin While");

        loopStartLabels.pop();
        loopExitLabels.pop();
        loopUpdateLabels.pop();
        return null;
    }

    @Override
    public String visitDoWhileStmt(PigLatinParser.DoWhileStmtContext ctx) {
        int line = ctx.getStart().getLine();
        String labelStart = program.newLabel();
        String labelCond = program.newLabel();
        String labelExit = program.newLabel();

        loopStartLabels.push(labelStart);
        loopExitLabels.push(labelExit);
        loopUpdateLabels.push(labelCond);

        program.emit(OpCode.LABEL, "", "", labelStart, line, "Inicio Do-While");
        if (ctx.statement() != null) {
            for (PigLatinParser.StatementContext stmt : ctx.statement()) {
                visit(stmt);
            }
        }
        program.emit(OpCode.LABEL, "", "", labelCond, line);
        String condTemp = visit(ctx.expression());
        program.emit(OpCode.IF_TRUE, condTemp, "", labelStart, line);
        program.emit(OpCode.LABEL, "", "", labelExit, line, "Fin Do-While");

        loopStartLabels.pop();
        loopExitLabels.pop();
        loopUpdateLabels.pop();
        return null;
    }

    @Override
    public String visitForStmt(PigLatinParser.ForStmtContext ctx) {
        int line = ctx.getStart().getLine();
        if (ctx.forInit() != null) {
            visit(ctx.forInit());
        }

        String labelStart = program.newLabel();
        String labelUpdate = program.newLabel();
        String labelExit = program.newLabel();

        loopStartLabels.push(labelStart);
        loopExitLabels.push(labelExit);
        loopUpdateLabels.push(labelUpdate);

        program.emit(OpCode.LABEL, "", "", labelStart, line, "Inicio For");

        if (ctx.expression() != null) {
            String condTemp = visit(ctx.expression());
            program.emit(OpCode.IF_FALSE, condTemp, "", labelExit, line);
        }

        if (ctx.statement() != null) {
            for (PigLatinParser.StatementContext stmt : ctx.statement()) {
                visit(stmt);
            }
        }

        program.emit(OpCode.LABEL, "", "", labelUpdate, line);
        if (ctx.forUpdate() != null) {
            visit(ctx.forUpdate());
        }
        program.emit(OpCode.GOTO, "", "", labelStart, line);
        program.emit(OpCode.LABEL, "", "", labelExit, line, "Fin For");

        loopStartLabels.pop();
        loopExitLabels.pop();
        loopUpdateLabels.pop();
        return null;
    }

    @Override
    public String visitBreakStmt(PigLatinParser.BreakStmtContext ctx) {
        if (!loopExitLabels.isEmpty()) {
            program.emit(OpCode.GOTO, "", "", loopExitLabels.peek(), ctx.getStart().getLine(), "break");
        }
        return null;
    }

    @Override
    public String visitContinueStmt(PigLatinParser.ContinueStmtContext ctx) {
        if (!loopUpdateLabels.isEmpty()) {
            program.emit(OpCode.GOTO, "", "", loopUpdateLabels.peek(), ctx.getStart().getLine(), "continue");
        }
        return null;
    }

    private final java.util.Set<String> stringVars = new java.util.HashSet<>();

    private boolean isStringType(String typeName) {
        if (typeName == null) return false;
        String t = typeName.toLowerCase();
        return t.equals("textum") || t.equals("cadena") || t.equals("string");
    }

    private boolean isStringExpr(org.antlr.v4.runtime.tree.ParseTree tree) {
        if (tree == null) return false;
        String text = tree.getText();
        if (text.startsWith("\"") && text.endsWith("\"")) return true;
        if (stringVars.contains(text)) return true;
        if (tree instanceof PigLatinParser.ExpressionContext expr) {
            PigLatinParser.PostfixExpressionContext post = getPostfix(expr);
            if (post != null) {
                if (post.DOT() != null && post.LPAREN() != null) {
                    return isStringMethod(post.postfixExpression().getText(), post.ID().getText());
                }
                if (post.primaryExpression() != null) {
                    var pri = post.primaryExpression();
                    if (pri.ID().size() == 2 && pri.LPAREN() != null) {
                        return isStringMethod(pri.ID(0).getText(), pri.ID(1).getText());
                    }
                    if (pri.ID().size() == 1 && pri.LPAREN() != null) {
                        return isStringFunc(pri.ID(0).getText());
                    }
                }
            }
        }
        for (int i = 0; i < tree.getChildCount(); i++) {
            var child = tree.getChild(i);
            if (child instanceof PigLatinParser.ArgumentListContext) continue;
            if (isStringExpr(child)) return true;
        }
        return false;
    }

    private boolean isVoidCall(PigLatinParser.ExpressionContext expr) {
        if (expr == null) return false;
        PigLatinParser.PostfixExpressionContext post = getPostfix(expr);
        if (post == null) return false;

        if (post.DOT() != null && post.LPAREN() != null) {
            String objName = post.postfixExpression().getText();
            String methodName = post.ID().getText();
            return isVoidMethod(objName, methodName);
        }

        if (post.primaryExpression() != null) {
            PigLatinParser.PrimaryExpressionContext pri = post.primaryExpression();
            if (pri.ID().size() == 2 && pri.LPAREN() != null) {
                String objName = pri.ID(0).getText();
                String methodName = pri.ID(1).getText();
                return isVoidMethod(objName, methodName);
            }
            if (pri.ID().size() == 1 && pri.LPAREN() != null) {
                String funcName = pri.ID(0).getText();
                return isVoidFunc(funcName);
            }
        }
        return false;
    }

    private PigLatinParser.PostfixExpressionContext getPostfix(PigLatinParser.ExpressionContext expr) {
        try {
            if (expr == null || expr.orExpression() == null) return null;
            var orExpr = expr.orExpression();
            if (orExpr.andExpression() != null && orExpr.andExpression().size() == 1) {
                var andExpr = orExpr.andExpression(0);
                if (andExpr.relationalExpression() != null && andExpr.relationalExpression().size() == 1) {
                    var relExpr = andExpr.relationalExpression(0);
                    if (relExpr.additiveExpression() != null && relExpr.additiveExpression().size() == 1) {
                        var addExpr = relExpr.additiveExpression(0);
                        if (addExpr.multiplicativeExpression() != null && addExpr.multiplicativeExpression().size() == 1) {
                            var mulExpr = addExpr.multiplicativeExpression(0);
                            if (mulExpr.unaryExpression() != null && mulExpr.unaryExpression().size() == 1) {
                                var unExpr = mulExpr.unaryExpression(0);
                                if (unExpr.postfixExpression() != null) {
                                    return unExpr.postfixExpression();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private boolean isVoidFunc(String funcName) {
        if (symbolTable != null) {
            org.nexus.nexussolairy.model.semantic.Symbol s = symbolTable.lookup(funcName);
            if (s instanceof org.nexus.nexussolairy.model.semantic.FunctionSymbol fs) {
                return fs.getType() == org.nexus.nexussolairy.model.enums.DataType.VOID || (fs.getSemanticType() != null && fs.getSemanticType().getDataType() == org.nexus.nexussolairy.model.enums.DataType.VOID);
            }
            if (s != null) {
                return s.getType() == org.nexus.nexussolairy.model.enums.DataType.VOID;
            }
        }
        return false;
    }

    private boolean isVoidMethod(String objName, String methodName) {
        String clsName = varClasses.getOrDefault(objName, objName);
        if (symbolTable != null) {
            org.nexus.nexussolairy.model.semantic.ClassSymbol cs = symbolTable.lookupClass(clsName);
            if (cs == null) {
                org.nexus.nexussolairy.model.semantic.Symbol s = symbolTable.lookup(objName);
                if (s != null && s.getStructTypeName() != null) {
                    cs = symbolTable.lookupClass(s.getStructTypeName());
                }
            }
            if (cs != null) {
                java.util.List<org.nexus.nexussolairy.model.semantic.Symbol> ms = cs.resolveMethod(methodName);
                if (ms != null && !ms.isEmpty()) {
                    org.nexus.nexussolairy.model.semantic.Symbol m = ms.get(0);
                    return m.getType() == org.nexus.nexussolairy.model.enums.DataType.VOID || (m.getSemanticType() != null && m.getSemanticType().getDataType() == org.nexus.nexussolairy.model.enums.DataType.VOID);
                }
            }
        }
        return false;
    }

    private boolean isStringFunc(String funcName) {
        if (symbolTable != null) {
            org.nexus.nexussolairy.model.semantic.Symbol s = symbolTable.lookup(funcName);
            if (s != null) {
                return s.getType() == org.nexus.nexussolairy.model.enums.DataType.CADENA || s.getType() == org.nexus.nexussolairy.model.enums.DataType.TEXTUM;
            }
        }
        return false;
    }

    private boolean isStringMethod(String objName, String methodName) {
        String clsName = varClasses.getOrDefault(objName, objName);
        if (symbolTable != null) {
            org.nexus.nexussolairy.model.semantic.ClassSymbol cs = symbolTable.lookupClass(clsName);
            if (cs == null) {
                org.nexus.nexussolairy.model.semantic.Symbol s = symbolTable.lookup(objName);
                if (s != null && s.getStructTypeName() != null) {
                    cs = symbolTable.lookupClass(s.getStructTypeName());
                }
            }
            if (cs != null) {
                java.util.List<org.nexus.nexussolairy.model.semantic.Symbol> ms = cs.resolveMethod(methodName);
                if (ms != null && !ms.isEmpty()) {
                    org.nexus.nexussolairy.model.semantic.Symbol m = ms.get(0);
                    return m.getType() == org.nexus.nexussolairy.model.enums.DataType.CADENA || m.getType() == org.nexus.nexussolairy.model.enums.DataType.TEXTUM;
                }
            }
        }
        return false;
    }

    @Override
    public String visitPrintStmt(PigLatinParser.PrintStmtContext ctx) {
        int line = ctx.getStart().getLine();
        boolean lastWasString = false;
        for (PigLatinParser.ExpressionContext expr : ctx.expression()) {
            if (isVoidCall(expr)) {
                visit(expr);
                continue;
            }
            String val = visit(expr);
            if (val == null || val.isEmpty()) val = "0";
            if (isStringExpr(expr)) {
                program.emit(OpCode.PRINT_STR, val, "", "", line);
                lastWasString = true;
            } else {
                program.emit(OpCode.PRINT, val, "", "", line);
                lastWasString = false;
            }
        }
        if (lastWasString) {
            String nlRef = createStringLiteral("\n", line);
            program.emit(OpCode.PRINT_STR, nlRef, "", "", line);
        }
        return null;
    }

    private String createStringLiteral(String str, int line) {
        String heapRef = program.newTemp();
        program.emit(OpCode.ASSIGN, "H", "", heapRef, line, "String literal");
        for (char c : str.toCharArray()) {
            program.emit(OpCode.HEAP_WRITE, "H", String.valueOf((int) c), "", line);
            String newH = program.newTemp();
            program.emit(OpCode.ADD, "H", "1", newH, line);
            program.emit(OpCode.ASSIGN, newH, "", "H", line);
        }
        program.emit(OpCode.HEAP_WRITE, "H", "-1", "", line);
        String newH = program.newTemp();
        program.emit(OpCode.ADD, "H", "1", newH, line);
        program.emit(OpCode.ASSIGN, newH, "", "H", line);
        return heapRef;
    }

    @Override
    public String visitReadStmt(PigLatinParser.ReadStmtContext ctx) {
        int line = ctx.getStart().getLine();
        String readTemp = program.newTemp();
        program.emit(OpCode.READ, "", "", readTemp, line);
        if (ctx.ID() != null) {
            storeVar(ctx.ID().getText(), readTemp, line);
        }
        return null;
    }

    @Override
    public String visitOrExpression(PigLatinParser.OrExpressionContext ctx) {
        int line = ctx.getStart().getLine();
        if (ctx.andExpression().size() == 1) {
            return visit(ctx.andExpression(0));
        }
        String labelTrue = program.newLabel();
        String labelEnd = program.newLabel();
        String resTemp = program.newTemp();

        for (PigLatinParser.AndExpressionContext andCtx : ctx.andExpression()) {
            String andVal = visit(andCtx);
            program.emit(OpCode.IF_TRUE, andVal, "", labelTrue, line);
        }
        program.emit(OpCode.ASSIGN, "0", "", resTemp, line);
        program.emit(OpCode.GOTO, "", "", labelEnd, line);
        program.emit(OpCode.LABEL, "", "", labelTrue, line);
        program.emit(OpCode.ASSIGN, "1", "", resTemp, line);
        program.emit(OpCode.LABEL, "", "", labelEnd, line);
        return resTemp;
    }

    @Override
    public String visitAndExpression(PigLatinParser.AndExpressionContext ctx) {
        int line = ctx.getStart().getLine();
        if (ctx.relationalExpression().size() == 1) {
            return visit(ctx.relationalExpression(0));
        }
        String labelFalse = program.newLabel();
        String labelEnd = program.newLabel();
        String resTemp = program.newTemp();

        for (PigLatinParser.RelationalExpressionContext relCtx : ctx.relationalExpression()) {
            String relVal = visit(relCtx);
            program.emit(OpCode.IF_FALSE, relVal, "", labelFalse, line);
        }
        program.emit(OpCode.ASSIGN, "1", "", resTemp, line);
        program.emit(OpCode.GOTO, "", "", labelEnd, line);
        program.emit(OpCode.LABEL, "", "", labelFalse, line);
        program.emit(OpCode.ASSIGN, "0", "", resTemp, line);
        program.emit(OpCode.LABEL, "", "", labelEnd, line);
        return resTemp;
    }

    @Override
    public String visitRelationalExpression(PigLatinParser.RelationalExpressionContext ctx) {
        int line = ctx.getStart().getLine();
        String left = visit(ctx.additiveExpression(0));
        for (int i = 1; i < ctx.additiveExpression().size(); i++) {
            String right = visit(ctx.additiveExpression(i));
            String opText = ctx.getChild(2 * i - 1).getText();
            OpCode op = parseOp(opText);
            String resTemp = program.newTemp();
            program.emit(op, left, right, resTemp, line);
            left = resTemp;
        }
        return left;
    }

    @Override
    public String visitAdditiveExpression(PigLatinParser.AdditiveExpressionContext ctx) {
        int line = ctx.getStart().getLine();
        String left = visit(ctx.multiplicativeExpression(0));
        for (int i = 1; i < ctx.multiplicativeExpression().size(); i++) {
            String right = visit(ctx.multiplicativeExpression(i));
            String opText = ctx.getChild(2 * i - 1).getText();
            OpCode op = "+".equals(opText) ? OpCode.ADD : OpCode.SUB;
            String resTemp = program.newTemp();
            program.emit(op, left, right, resTemp, line);
            left = resTemp;
        }
        return left;
    }

    @Override
    public String visitMultiplicativeExpression(PigLatinParser.MultiplicativeExpressionContext ctx) {
        int line = ctx.getStart().getLine();
        String left = visit(ctx.unaryExpression(0));
        for (int i = 1; i < ctx.unaryExpression().size(); i++) {
            String right = visit(ctx.unaryExpression(i));
            String opText = ctx.getChild(2 * i - 1).getText();
            OpCode op = "*".equals(opText) ? OpCode.MULT : OpCode.DIV;
            String resTemp = program.newTemp();
            program.emit(op, left, right, resTemp, line);
            left = resTemp;
        }
        return left;
    }

    @Override
    public String visitUnaryExpression(PigLatinParser.UnaryExpressionContext ctx) {
        int line = ctx.getStart().getLine();
        if (ctx.NOT() != null) {
            String val = visit(ctx.unaryExpression());
            String resTemp = program.newTemp();
            program.emit(OpCode.NOT, val, "", resTemp, line);
            return resTemp;
        }
        if (ctx.MINUS() != null) {
            String val = visit(ctx.unaryExpression());
            String resTemp = program.newTemp();
            program.emit(OpCode.SUB, "0", val, resTemp, line);
            return resTemp;
        }
        return visit(ctx.postfixExpression());
    }

    @Override
    public String visitForInit(PigLatinParser.ForInitContext ctx) {
        int line = ctx.getStart().getLine();
        if (ctx.ID() != null && ctx.expression() != null) {
            String varName = ctx.ID().getText();
            String valTemp = visit(ctx.expression());
            if (valTemp == null || valTemp.isEmpty()) valTemp = "0";
            storeVar(varName, valTemp, line);
        }
        return null;
    }

    @Override
    public String visitForUpdate(PigLatinParser.ForUpdateContext ctx) {
        int line = ctx.getStart().getLine();
        if (ctx.ID() != null) {
            String varName = ctx.ID().getText();
            String curVal = loadVar(varName, line);
            if (ctx.INC() != null || ctx.DEC() != null) {
                OpCode op = ctx.INC() != null ? OpCode.ADD : OpCode.SUB;
                String resTemp = program.newTemp();
                program.emit(op, curVal, "1", resTemp, line);
                storeVar(varName, resTemp, line);
                return resTemp;
            } else if (ctx.expression() != null) {
                String valTemp = visit(ctx.expression());
                if (valTemp == null || valTemp.isEmpty()) valTemp = "0";
                storeVar(varName, valTemp, line);
                return valTemp;
            }
        }
        return null;
    }

    @Override
    public String visitPostfixExpression(PigLatinParser.PostfixExpressionContext ctx) {
        int line = ctx.getStart().getLine();
        if (ctx.primaryExpression() != null) {
            return visit(ctx.primaryExpression());
        }
        if (ctx.postfixExpression() != null) {
            String val = visit(ctx.postfixExpression());
            if (ctx.INC() != null || ctx.DEC() != null) {
                OpCode op = ctx.INC() != null ? OpCode.ADD : OpCode.SUB;
                String resTemp = program.newTemp();
                program.emit(op, safe(val), "1", resTemp, line);
                String varName = getVarNameFromPostfix(ctx.postfixExpression());
                if (varName != null) {
                    storeVar(varName, resTemp, line);
                }
                return resTemp;
            }
            if (ctx.DOT() != null && ctx.LPAREN() != null) {
                String targetName = ctx.postfixExpression().getText();
                String methodName = ctx.ID().getText();
                return emitMethodCallC3D(targetName, methodName, ctx.argumentList(), line);
            }
            if (ctx.DOT() != null) {
                String targetName = ctx.postfixExpression().getText();
                String objPtr = loadVar(targetName, line);
                String resTemp = program.newTemp();
                program.emit(OpCode.HEAP_READ, objPtr, "0", resTemp, line);
                return resTemp;
            }
            return val;
        }
        return "0";
    }

    private String getVarNameFromPostfix(PigLatinParser.PostfixExpressionContext ctx) {
        if (ctx == null) return null;
        if (ctx.primaryExpression() != null) {
            if (!ctx.primaryExpression().ID().isEmpty()) {
                return ctx.primaryExpression().ID(0).getText();
            }
        }
        if (ctx.postfixExpression() != null) {
            return getVarNameFromPostfix(ctx.postfixExpression());
        }
        return null;
    }

    @Override
    public String visitPrimaryExpression(PigLatinParser.PrimaryExpressionContext ctx) {
        int line = ctx.getStart().getLine();
        if (ctx.literal() != null) {
            return visit(ctx.literal());
        }
        if (ctx.ID().size() == 1 && ctx.DOT() == null && ctx.LBRACK() == null && ctx.LPAREN() == null) {
            return loadVar(ctx.ID(0).getText(), line);
        }
        if (ctx.ID().size() == 1 && ctx.LPAREN() != null) {
            String funcName = ctx.ID(0).getText();
            return emitFunctionCallC3D(funcName, ctx.argumentList(), line);
        }
        if (ctx.ID().size() == 2 && ctx.LPAREN() != null) {
            String objName = ctx.ID(0).getText();
            String methodName = ctx.ID(1).getText();
            return emitMethodCallC3D(objName, methodName, ctx.argumentList(), line);
        }
        if (ctx.ID().size() == 2 && ctx.LPAREN() == null) {
            String objName = ctx.ID(0).getText();
            String objPtr = loadVar(objName, line);
            String resTemp = program.newTemp();
            program.emit(OpCode.HEAP_READ, objPtr, "0", resTemp, line);
            return resTemp;
        }
        if (ctx.NOVUS() != null) {
            String clsName = ctx.ID(0).getText();
            return allocateObject(clsName, line);
        }
        if (ctx.expression() != null) {
            return visit(ctx.expression());
        }
        return "0";
    }

    private String emitFunctionCallC3D(String funcName, PigLatinParser.ArgumentListContext argList, int line) {
        java.util.List<String> argTemps = new java.util.ArrayList<>();
        if (argList != null && argList.expression() != null) {
            for (PigLatinParser.ExpressionContext argExpr : argList.expression()) {
                argTemps.add(visit(argExpr));
            }
        }

        int frameOffset = memory.getCallFrameOffset(currentRoutine);
        String tNew = program.newTemp();
        program.emit(OpCode.ADD, "P", String.valueOf(frameOffset), tNew, line, "Nuevo P");

        int pIdx = 1;
        for (String arg : argTemps) {
            String pAddr = program.newTemp();
            program.emit(OpCode.ADD, tNew, String.valueOf(pIdx++), pAddr, line);
            program.emit(OpCode.STACK_WRITE, pAddr, safe(arg), "", line, "Param " + (pIdx - 1));
        }

        program.emit(OpCode.ADD, "P", String.valueOf(frameOffset), "P", line, "Avanzar P");
        program.emit(OpCode.CALL, funcName, "", "", line);
        program.emit(OpCode.SUB, "P", String.valueOf(frameOffset), "P", line, "Restaurar P");

        String tRet = program.newTemp();
        program.emit(OpCode.STACK_READ, tNew, "", tRet, line, "Leer retorno");
        return tRet;
    }

    private final java.util.Map<String, String> varClasses = new java.util.HashMap<>();

    private String emitMethodCallC3D(String objName, String methodName, PigLatinParser.ArgumentListContext argList, int line) {
        String clsName = varClasses.getOrDefault(objName, objName);
        String objPtr = loadVar(objName, line);

        java.util.List<String> argTemps = new java.util.ArrayList<>();
        if (argList != null && argList.expression() != null) {
            for (PigLatinParser.ExpressionContext argExpr : argList.expression()) {
                argTemps.add(visit(argExpr));
            }
        }

        int frameOffset = memory.getCallFrameOffset(currentRoutine);
        String tNew = program.newTemp();
        program.emit(OpCode.ADD, "P", String.valueOf(frameOffset), tNew, line, "Nuevo P");

        String thisAddr = program.newTemp();
        program.emit(OpCode.ADD, tNew, "1", thisAddr, line);
        program.emit(OpCode.STACK_WRITE, thisAddr, safe(objPtr), "", line, "Pass 'this'");

        int pIdx = 2;
        for (String arg : argTemps) {
            String pAddr = program.newTemp();
            program.emit(OpCode.ADD, tNew, String.valueOf(pIdx++), pAddr, line);
            program.emit(OpCode.STACK_WRITE, pAddr, safe(arg), "", line, "Param " + (pIdx - 1));
        }

        program.emit(OpCode.ADD, "P", String.valueOf(frameOffset), "P", line, "Avanzar P");
        program.emit(OpCode.CALL, clsName + "_" + methodName, "", "", line);
        program.emit(OpCode.SUB, "P", String.valueOf(frameOffset), "P", line, "Restaurar P");

        String tRet = program.newTemp();
        program.emit(OpCode.STACK_READ, tNew, "", tRet, line, "Leer retorno");
        return tRet;
    }

    private String safe(String s) {
        return (s == null || s.isEmpty()) ? "0" : s;
    }

    @Override
    public String visitLiteral(PigLatinParser.LiteralContext ctx) {
        int line = ctx.getStart().getLine();
        if (ctx.NUMBER() != null || ctx.DECIMAL() != null) {
            return ctx.getText();
        }
        if (ctx.VERUM() != null) return "1";
        if (ctx.FALSUS() != null) return "0";
        if (ctx.STRING() != null) {
            String str = ctx.STRING().getText();
            str = str.substring(1, str.length() - 1);
            String heapRef = program.newTemp();
            program.emit(OpCode.ASSIGN, "H", "", heapRef, line, "Literal cadena \"" + str + "\"");
            for (char c : str.toCharArray()) {
                program.emit(OpCode.HEAP_WRITE, "H", String.valueOf((int) c), "", line);
                String newH = program.newTemp();
                program.emit(OpCode.ADD, "H", "1", newH, line);
                program.emit(OpCode.ASSIGN, newH, "", "H", line);
            }
            program.emit(OpCode.HEAP_WRITE, "H", "-1", "", line);
            String newH = program.newTemp();
            program.emit(OpCode.ADD, "H", "1", newH, line);
            program.emit(OpCode.ASSIGN, newH, "", "H", line);
            return heapRef;
        }
        return "0";
    }

    private OpCode parseOp(String op) {
        return switch (op) {
            case "==" -> OpCode.EQ;
            case "!=" -> OpCode.NEQ;
            case "<" -> OpCode.LT;
            case "<=" -> OpCode.LE;
            case ">" -> OpCode.GT;
            case ">=" -> OpCode.GE;
            default -> OpCode.EQ;
        };
    }
}
