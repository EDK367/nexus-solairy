package org.nexus.nexussolairy.c3d;

import org.nexus.nexussolairy.YParser;
import org.nexus.nexussolairy.YParserBaseVisitor;
import org.nexus.nexussolairy.model.c3d.C3DProgram;
import org.nexus.nexussolairy.model.c3d.OpCode;
import org.nexus.nexussolairy.model.semantic.Symbol;

import java.util.ArrayDeque;
import java.util.Deque;

public class YC3DVisitor extends YParserBaseVisitor<String> {

    private final C3DProgram program;
    private final MemoryLayout memory;
    private String currentRoutine = "main";

    private final Deque<String> loopStartLabels = new ArrayDeque<>();
    private final Deque<String> loopExitLabels = new ArrayDeque<>();
    private final Deque<String> loopUpdateLabels = new ArrayDeque<>();

    private final org.nexus.nexussolairy.model.semantic.SymbolTable symbolTable;

    public YC3DVisitor(C3DProgram program, MemoryLayout memory) {
        this(program, memory, null);
    }

    public YC3DVisitor(C3DProgram program, MemoryLayout memory, org.nexus.nexussolairy.model.semantic.SymbolTable symbolTable) {
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
    public String visitProgram(YParser.ProgramContext ctx) {
        if (ctx.funcSection() != null) {
            visit(ctx.funcSection());
        }
        return null;
    }

    @Override
    public String visitFuncDef(YParser.FuncDefContext ctx) {
        if (ctx.voidFunction() != null) return visit(ctx.voidFunction());
        if (ctx.returnFunction() != null) return visit(ctx.returnFunction());
        return null;
    }

    @Override
    public String visitVoidFunction(YParser.VoidFunctionContext ctx) {
        String funcName = ctx.ID().getText();
        currentRoutine = funcName;
        int line = ctx.getStart().getLine();

        program.emit(OpCode.LABEL, "", "", funcName, line, "Función " + funcName);
        if (ctx.parameterList() != null) {
            for (YParser.ParameterContext p : ctx.parameterList().parameter()) {
                String pName = p.ID(p.ID().size() - 1).getText();
                memory.declareParam(funcName, pName);
                if (p.type() != null && isStringType(p.type().getText())) {
                    stringVars.add(pName);
                }
            }
        }
        if (ctx.block() != null) {
            visit(ctx.block());
        }
        program.emit(OpCode.RETURN, "", "", "", line);
        return null;
    }

    @Override
    public String visitReturnFunction(YParser.ReturnFunctionContext ctx) {
        String funcName = ctx.ID().getText();
        currentRoutine = funcName;
        int line = ctx.getStart().getLine();

        program.emit(OpCode.LABEL, "", "", funcName, line, "Función " + funcName);
        if (ctx.parameterList() != null) {
            for (YParser.ParameterContext p : ctx.parameterList().parameter()) {
                String pName = p.ID(p.ID().size() - 1).getText();
                memory.declareParam(funcName, pName);
                if (p.type() != null && isStringType(p.type().getText())) {
                    stringVars.add(pName);
                }
            }
        }
        if (ctx.block() != null) {
            visit(ctx.block());
        }
        program.emit(OpCode.RETURN, "", "", "", line);
        return null;
    }

    private String loadTarget(YParser.TargetContext target, int line) {
        String varName = target.ID(0).getText();
        String basePtr = program.newTemp();
        if (memory.isGlobal(varName, currentRoutine)) {
            int offset = memory.getGlobalOffset(varName);
            program.emit(OpCode.STACK_READ, String.valueOf(offset), "", basePtr, line, "Leer global " + varName);
        } else {
            int offset = memory.getLocalOffset(currentRoutine, varName);
            if (offset < 0) offset = memory.declareLocal(currentRoutine, varName);
            String pAddr = program.newTemp();
            program.emit(OpCode.ADD, "P", String.valueOf(offset), pAddr, line);
            program.emit(OpCode.STACK_READ, pAddr, "", basePtr, line, "Leer local " + varName);
        }

        if (target.DOT().isEmpty() && target.LBRACK().isEmpty()) {
            return basePtr;
        }

        if (target.DOT().isEmpty() && !target.LBRACK().isEmpty()) {
            String idxTemp = visit(target.expression(0));
            String elemAddr = program.newTemp();
            program.emit(OpCode.ADD, basePtr, idxTemp, elemAddr, line);
            String valTemp = program.newTemp();
            program.emit(OpCode.HEAP_READ, elemAddr, "", valTemp, line);
            return valTemp;
        }

        if (!target.DOT().isEmpty()) {
            int fieldOffset = 0;
            if (target.ID().size() >= 2 && symbolTable != null) {
                String fieldName = target.ID(1).getText();
                Symbol s = symbolTable.lookup(varName);
                if (s != null && s.structTypeName != null) {
                    var structInfo = symbolTable.lookupStruct(s.structTypeName);
                    if (structInfo != null) {
                        int idx = 0;
                        for (String f : structInfo.getFields().keySet()) {
                            if (f.equals(fieldName)) {
                                fieldOffset = idx;
                                break;
                            }
                            idx++;
                        }
                    }
                }
            }
            String fieldAddr = program.newTemp();
            program.emit(OpCode.ADD, basePtr, String.valueOf(fieldOffset), fieldAddr, line);
            String valTemp = program.newTemp();
            program.emit(OpCode.HEAP_READ, fieldAddr, "", valTemp, line);
            return valTemp;
        }

        return basePtr;
    }

    private void storeTarget(YParser.TargetContext target, String valTemp, int line) {
        String varName = target.ID(0).getText();

        if (target.DOT().isEmpty() && target.LBRACK().isEmpty()) {
            if (memory.isGlobal(varName, currentRoutine)) {
                int offset = memory.getGlobalOffset(varName);
                if (offset < 0) offset = memory.declareGlobal(varName);
                program.emit(OpCode.STACK_WRITE, String.valueOf(offset), valTemp, "", line);
            } else {
                int offset = memory.getLocalOffset(currentRoutine, varName);
                if (offset < 0) offset = memory.declareLocal(currentRoutine, varName);
                String pAddr = program.newTemp();
                program.emit(OpCode.ADD, "P", String.valueOf(offset), pAddr, line);
                program.emit(OpCode.STACK_WRITE, pAddr, valTemp, "", line);
            }
            return;
        }

        String basePtr = program.newTemp();
        if (memory.isGlobal(varName, currentRoutine)) {
            int offset = memory.getGlobalOffset(varName);
            program.emit(OpCode.STACK_READ, String.valueOf(offset), "", basePtr, line);
        } else {
            int offset = memory.getLocalOffset(currentRoutine, varName);
            if (offset < 0) offset = memory.declareLocal(currentRoutine, varName);
            String pAddr = program.newTemp();
            program.emit(OpCode.ADD, "P", String.valueOf(offset), pAddr, line);
            program.emit(OpCode.STACK_READ, pAddr, "", basePtr, line);
        }

        if (target.DOT().isEmpty() && !target.LBRACK().isEmpty()) {
            String idxTemp = visit(target.expression(0));
            String elemAddr = program.newTemp();
            program.emit(OpCode.ADD, basePtr, idxTemp, elemAddr, line);
            program.emit(OpCode.HEAP_WRITE, elemAddr, valTemp, "", line);
            return;
        }

        if (!target.DOT().isEmpty()) {
            int fieldOffset = 0;
            if (target.ID().size() >= 2 && symbolTable != null) {
                String fieldName = target.ID(1).getText();
                Symbol s = symbolTable.lookup(varName);
                if (s != null && s.structTypeName != null) {
                    var structInfo = symbolTable.lookupStruct(s.structTypeName);
                    if (structInfo != null) {
                        int idx = 0;
                        for (String f : structInfo.getFields().keySet()) {
                            if (f.equals(fieldName)) {
                                fieldOffset = idx;
                                break;
                            }
                            idx++;
                        }
                    }
                }
            }
            String fieldAddr = program.newTemp();
            program.emit(OpCode.ADD, basePtr, String.valueOf(fieldOffset), fieldAddr, line);
            program.emit(OpCode.HEAP_WRITE, fieldAddr, valTemp, "", line);
        }
    }

    @Override
    public String visitVarDecl(YParser.VarDeclContext ctx) {
        int line = ctx.getStart().getLine();
        String varName = (ctx.type() != null) ? ctx.ID(0).getText() : ctx.ID(1).getText();
        if (ctx.type() != null && isStringType(ctx.type().getText())) {
            stringVars.add(varName);
        }
        int offset = memory.isGlobal(varName, currentRoutine) ?
                memory.declareGlobal(varName) : memory.declareLocal(currentRoutine, varName);

        String valTemp = "0";
        if (ctx.arrayInit() != null && ctx.arrayInit().expressionList() != null) {
            String heapRef = program.newTemp();
            program.emit(OpCode.ASSIGN, "H", "", heapRef, line, "Array " + varName);
            for (YParser.ExpressionContext e : ctx.arrayInit().expressionList().expression()) {
                String elemVal = visit(e);
                program.emit(OpCode.HEAP_WRITE, "H", elemVal, "", line);
                String newH = program.newTemp();
                program.emit(OpCode.ADD, "H", "1", newH, line);
                program.emit(OpCode.ASSIGN, newH, "", "H", line);
            }
            valTemp = heapRef;
        } else if (ctx.structLiteral() != null && ctx.structLiteral().expressionList() != null) {
            String heapRef = program.newTemp();
            program.emit(OpCode.ASSIGN, "H", "", heapRef, line, "Struct " + varName);
            for (YParser.ExpressionContext e : ctx.structLiteral().expressionList().expression()) {
                String fVal = visit(e);
                program.emit(OpCode.HEAP_WRITE, "H", fVal, "", line);
                String newH = program.newTemp();
                program.emit(OpCode.ADD, "H", "1", newH, line);
                program.emit(OpCode.ASSIGN, newH, "", "H", line);
            }
            valTemp = heapRef;
        } else if (ctx.ASSIGN() != null && ctx.expression() != null && !ctx.expression().isEmpty()) {
            valTemp = visit(ctx.expression(ctx.expression().size() - 1));
        }

        if (memory.isGlobal(varName, currentRoutine)) {
            program.emit(OpCode.STACK_WRITE, String.valueOf(offset), valTemp, "", line, "Var " + varName);
        } else {
            String pAddr = program.newTemp();
            program.emit(OpCode.ADD, "P", String.valueOf(offset), pAddr, line);
            program.emit(OpCode.STACK_WRITE, pAddr, valTemp, "", line, "Var local " + varName);
        }
        return null;
    }

    @Override
    public String visitAssignStmt(YParser.AssignStmtContext ctx) {
        int line = ctx.getStart().getLine();
        if (ctx.INC() != null || ctx.DEC() != null) {
            String curVal = loadTarget(ctx.target(), line);
            String nextVal = program.newTemp();
            OpCode op = ctx.INC() != null ? OpCode.ADD : OpCode.SUB;
            program.emit(op, curVal, "1", nextVal, line);
            storeTarget(ctx.target(), nextVal, line);
            return null;
        }

        String valTemp = ctx.expression() != null ? visit(ctx.expression()) : "0";
        if (ctx.ASSIGN() != null) {
            storeTarget(ctx.target(), valTemp, line);
        } else {
            String curVal = loadTarget(ctx.target(), line);
            String nextVal = program.newTemp();
            OpCode op = ctx.ADD_ASSIGN() != null ? OpCode.ADD :
                        ctx.SUB_ASSIGN() != null ? OpCode.SUB :
                        ctx.MUL_ASSIGN() != null ? OpCode.MULT : OpCode.DIV;
            program.emit(op, curVal, valTemp, nextVal, line);
            storeTarget(ctx.target(), nextVal, line);
        }
        return null;
    }

    @Override
    public String visitForInit(YParser.ForInitContext ctx) {
        int line = ctx.getStart().getLine();
        String valTemp = visit(ctx.expression());
        if (ctx.type() != null) {
            String varName = ctx.ID().getText();
            int offset = memory.declareLocal(currentRoutine, varName);
            String pAddr = program.newTemp();
            program.emit(OpCode.ADD, "P", String.valueOf(offset), pAddr, line);
            program.emit(OpCode.STACK_WRITE, pAddr, valTemp, "", line);
        } else if (ctx.target() != null) {
            storeTarget(ctx.target(), valTemp, line);
        }
        return null;
    }

    @Override
    public String visitForUpdate(YParser.ForUpdateContext ctx) {
        int line = ctx.getStart().getLine();
        if (ctx.INC() != null || ctx.DEC() != null) {
            String curVal = loadTarget(ctx.target(), line);
            String nextVal = program.newTemp();
            OpCode op = ctx.INC() != null ? OpCode.ADD : OpCode.SUB;
            program.emit(op, curVal, "1", nextVal, line);
            storeTarget(ctx.target(), nextVal, line);
            return null;
        }
        if (ctx.expression() != null) {
            String valTemp = visit(ctx.expression());
            if (ctx.ASSIGN() != null) {
                storeTarget(ctx.target(), valTemp, line);
            } else {
                String curVal = loadTarget(ctx.target(), line);
                String nextVal = program.newTemp();
                OpCode op = ctx.ADD_ASSIGN() != null ? OpCode.ADD :
                            ctx.SUB_ASSIGN() != null ? OpCode.SUB :
                            ctx.MUL_ASSIGN() != null ? OpCode.MULT : OpCode.DIV;
                program.emit(op, curVal, valTemp, nextVal, line);
                storeTarget(ctx.target(), nextVal, line);
            }
        }
        return null;
    }

    @Override
    public String visitCallStmt(YParser.CallStmtContext ctx) {
        int line = ctx.getStart().getLine();
        if (ctx.target() == null) {
            String funcName = ctx.ID().getText();
            int frameSize = memory.getCallFrameOffset(currentRoutine);
            String tNew = program.newTemp();
            program.emit(OpCode.ADD, "P", String.valueOf(frameSize), tNew, line, "Calcular nuevo marco");

            if (ctx.argumentList() != null) {
                int pIdx = 1;
                for (YParser.ExpressionContext argExpr : ctx.argumentList().expression()) {
                    String argVal = visit(argExpr);
                    String pAddr = program.newTemp();
                    program.emit(OpCode.ADD, tNew, String.valueOf(pIdx++), pAddr, line);
                    program.emit(OpCode.STACK_WRITE, pAddr, argVal, "", line, "Param " + (pIdx - 1));
                }
            }
            program.emit(OpCode.ADD, "P", String.valueOf(frameSize), "P", line, "Avanzar P");
            program.emit(OpCode.CALL, funcName, "", "", line);
            program.emit(OpCode.SUB, "P", String.valueOf(frameSize), "P", line, "Restaurar P");
        }
        return null;
    }

    @Override
    public String visitIfStmt(YParser.IfStmtContext ctx) {
        int line = ctx.getStart().getLine();
        String labelExit = program.newLabel();

        for (int i = 0; i < ctx.expression().size(); i++) {
            String labelNext = program.newLabel();
            String condTemp = visit(ctx.expression(i));
            program.emit(OpCode.IF_FALSE, condTemp, "", labelNext, line);

            if (i < ctx.block().size()) {
                visit(ctx.block(i));
            }
            program.emit(OpCode.GOTO, "", "", labelExit, line);
            program.emit(OpCode.LABEL, "", "", labelNext, line);
        }
        if (ctx.CONTRARIO() != null && ctx.block().size() > ctx.expression().size()) {
            visit(ctx.block(ctx.block().size() - 1));
        }
        program.emit(OpCode.LABEL, "", "", labelExit, line);
        return null;
    }

    @Override
    public String visitWhileStmt(YParser.WhileStmtContext ctx) {
        int line = ctx.getStart().getLine();
        String labelStart = program.newLabel();
        String labelExit = program.newLabel();

        loopStartLabels.push(labelStart);
        loopExitLabels.push(labelExit);
        loopUpdateLabels.push(labelStart);

        program.emit(OpCode.LABEL, "", "", labelStart, line, "Inicio While Y");
        String condTemp = visit(ctx.expression());
        program.emit(OpCode.IF_FALSE, condTemp, "", labelExit, line);

        if (ctx.block() != null) {
            visit(ctx.block());
        }
        program.emit(OpCode.GOTO, "", "", labelStart, line);
        program.emit(OpCode.LABEL, "", "", labelExit, line, "Fin While Y");

        loopStartLabels.pop();
        loopExitLabels.pop();
        loopUpdateLabels.pop();
        return null;
    }

    @Override
    public String visitDoWhileStmt(YParser.DoWhileStmtContext ctx) {
        int line = ctx.getStart().getLine();
        String labelStart = program.newLabel();
        String labelCond = program.newLabel();
        String labelExit = program.newLabel();

        loopStartLabels.push(labelStart);
        loopExitLabels.push(labelExit);
        loopUpdateLabels.push(labelCond);

        program.emit(OpCode.LABEL, "", "", labelStart, line);
        if (ctx.block() != null) {
            visit(ctx.block());
        }
        program.emit(OpCode.LABEL, "", "", labelCond, line);
        String condTemp = visit(ctx.expression());
        program.emit(OpCode.IF_TRUE, condTemp, "", labelStart, line);
        program.emit(OpCode.LABEL, "", "", labelExit, line);

        loopStartLabels.pop();
        loopExitLabels.pop();
        loopUpdateLabels.pop();
        return null;
    }

    @Override
    public String visitForStmt(YParser.ForStmtContext ctx) {
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

        program.emit(OpCode.LABEL, "", "", labelStart, line);
        if (ctx.expression() != null) {
            String condTemp = visit(ctx.expression());
            program.emit(OpCode.IF_FALSE, condTemp, "", labelExit, line);
        }
        if (ctx.block() != null) {
            visit(ctx.block());
        }
        program.emit(OpCode.LABEL, "", "", labelUpdate, line);
        if (ctx.forUpdate() != null) {
            visit(ctx.forUpdate());
        }
        program.emit(OpCode.GOTO, "", "", labelStart, line);
        program.emit(OpCode.LABEL, "", "", labelExit, line);

        loopStartLabels.pop();
        loopExitLabels.pop();
        loopUpdateLabels.pop();
        return null;
    }

    @Override
    public String visitSwitchStmt(YParser.SwitchStmtContext ctx) {
        int line = ctx.getStart().getLine();
        String switchVal = visit(ctx.expression());
        String labelExit = program.newLabel();

        int caseCount = ctx.caseBranch().size();
        String[] caseLabels = new String[caseCount];
        for (int i = 0; i < caseCount; i++) {
            caseLabels[i] = program.newLabel();
            String caseVal = visit(ctx.caseBranch(i).expression());
            String cmpTemp = program.newTemp();
            program.emit(OpCode.EQ, switchVal, caseVal, cmpTemp, line);
            program.emit(OpCode.IF_TRUE, cmpTemp, "", caseLabels[i], line);
        }

        String labelDefault = ctx.defaultBranch() != null ? program.newLabel() : labelExit;
        program.emit(OpCode.GOTO, "", "", labelDefault, line);

        loopExitLabels.push(labelExit);
        for (int i = 0; i < caseCount; i++) {
            program.emit(OpCode.LABEL, "", "", caseLabels[i], line);
            visit(ctx.caseBranch(i).block());
        }

        if (ctx.defaultBranch() != null) {
            program.emit(OpCode.LABEL, "", "", labelDefault, line);
            visit(ctx.defaultBranch().block());
        }
        loopExitLabels.pop();
        program.emit(OpCode.LABEL, "", "", labelExit, line);
        return null;
    }

    @Override
    public String visitBreakStmt(YParser.BreakStmtContext ctx) {
        if (!loopExitLabels.isEmpty()) {
            program.emit(OpCode.GOTO, "", "", loopExitLabels.peek(), ctx.getStart().getLine());
        }
        return null;
    }

    @Override
    public String visitContinueStmt(YParser.ContinueStmtContext ctx) {
        if (!loopUpdateLabels.isEmpty()) {
            program.emit(OpCode.GOTO, "", "", loopUpdateLabels.peek(), ctx.getStart().getLine());
        }
        return null;
    }

    @Override
    public String visitReturnStmt(YParser.ReturnStmtContext ctx) {
        int line = ctx.getStart().getLine();
        String valTemp = ctx.expression() != null ? visit(ctx.expression()) : "";
        if (!valTemp.isEmpty()) {
            program.emit(OpCode.STACK_WRITE, "P", valTemp, "", line, "Guardar retorno");
        }
        program.emit(OpCode.RETURN, valTemp, "", "", line);
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
        for (int i = 0; i < tree.getChildCount(); i++) {
            if (isStringExpr(tree.getChild(i))) return true;
        }
        return false;
    }

    @Override
    public String visitPrintStmt(YParser.PrintStmtContext ctx) {
        int line = ctx.getStart().getLine();
        String val = visit(ctx.expression());
        if (val == null || val.isEmpty()) val = "0";
        if (isStringExpr(ctx.expression())) {
            program.emit(OpCode.PRINT_STR, val, "", "", line);
            String nlRef = createStringLiteral("\n", line);
            program.emit(OpCode.PRINT_STR, nlRef, "", "", line);
        } else {
            program.emit(OpCode.PRINT, val, "", "", line);
        }
        return null;
    }

    private String unescapeString(String s) {
        if (s == null) return "";
        return s.replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\r", "\r")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private String createStringLiteral(String str, int line) {
        str = unescapeString(str);
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
    public String visitReadStmt(YParser.ReadStmtContext ctx) {
        int line = ctx.getStart().getLine();
        String readTemp = program.newTemp();
        program.emit(OpCode.READ, "", "", readTemp, line);
        return readTemp;
    }

    @Override
    public String visitOrExpression(YParser.OrExpressionContext ctx) {
        int line = ctx.getStart().getLine();
        if (ctx.andExpression().size() == 1) return visit(ctx.andExpression(0));

        String labelTrue = program.newLabel();
        String labelEnd = program.newLabel();
        String resTemp = program.newTemp();

        for (YParser.AndExpressionContext andCtx : ctx.andExpression()) {
            String val = visit(andCtx);
            program.emit(OpCode.IF_TRUE, val, "", labelTrue, line);
        }
        program.emit(OpCode.ASSIGN, "0", "", resTemp, line);
        program.emit(OpCode.GOTO, "", "", labelEnd, line);
        program.emit(OpCode.LABEL, "", "", labelTrue, line);
        program.emit(OpCode.ASSIGN, "1", "", resTemp, line);
        program.emit(OpCode.LABEL, "", "", labelEnd, line);
        return resTemp;
    }

    @Override
    public String visitAndExpression(YParser.AndExpressionContext ctx) {
        int line = ctx.getStart().getLine();
        if (ctx.relationalExpression().size() == 1) return visit(ctx.relationalExpression(0));

        String labelFalse = program.newLabel();
        String labelEnd = program.newLabel();
        String resTemp = program.newTemp();

        for (YParser.RelationalExpressionContext relCtx : ctx.relationalExpression()) {
            String val = visit(relCtx);
            program.emit(OpCode.IF_FALSE, val, "", labelFalse, line);
        }
        program.emit(OpCode.ASSIGN, "1", "", resTemp, line);
        program.emit(OpCode.GOTO, "", "", labelEnd, line);
        program.emit(OpCode.LABEL, "", "", labelFalse, line);
        program.emit(OpCode.ASSIGN, "0", "", resTemp, line);
        program.emit(OpCode.LABEL, "", "", labelEnd, line);
        return resTemp;
    }

    @Override
    public String visitRelationalExpression(YParser.RelationalExpressionContext ctx) {
        int line = ctx.getStart().getLine();
        String left = visit(ctx.additiveExpression(0));
        for (int i = 1; i < ctx.additiveExpression().size(); i++) {
            String right = visit(ctx.additiveExpression(i));
            String opText = ctx.getChild(2 * i - 1).getText();
            OpCode op = switch (opText) {
                case "==" -> OpCode.EQ;
                case "!=" -> OpCode.NEQ;
                case "<" -> OpCode.LT;
                case "<=" -> OpCode.LE;
                case ">" -> OpCode.GT;
                case ">=" -> OpCode.GE;
                default -> OpCode.EQ;
            };
            String res = program.newTemp();
            program.emit(op, left, right, res, line);
            left = res;
        }
        return left;
    }

    @Override
    public String visitAdditiveExpression(YParser.AdditiveExpressionContext ctx) {
        int line = ctx.getStart().getLine();
        String left = visit(ctx.multiplicativeExpression(0));
        for (int i = 1; i < ctx.multiplicativeExpression().size(); i++) {
            String right = visit(ctx.multiplicativeExpression(i));
            String opText = ctx.getChild(2 * i - 1).getText();
            String res = program.newTemp();
            if ("+".equals(opText) && (isStringExpr(ctx.multiplicativeExpression(i - 1)) || isStringExpr(ctx.multiplicativeExpression(i)) || stringVars.contains(left))) {
                boolean leftIsStr = isStringExpr(ctx.multiplicativeExpression(i - 1)) || stringVars.contains(left);
                boolean rightIsStr = isStringExpr(ctx.multiplicativeExpression(i)) || stringVars.contains(right);
                String mode = (leftIsStr && !rightIsStr) ? "str_num" :
                              (!leftIsStr && rightIsStr) ? "num_str" : "str_str";
                program.emit(OpCode.CONCAT_STR, left, right, res, line, mode);
                stringVars.add(res);
            } else {
                OpCode op = "+".equals(opText) ? OpCode.ADD : OpCode.SUB;
                program.emit(op, left, right, res, line);
            }
            left = res;
        }
        return left;
    }

    @Override
    public String visitMultiplicativeExpression(YParser.MultiplicativeExpressionContext ctx) {
        int line = ctx.getStart().getLine();
        String left = visit(ctx.unaryExpression(0));
        for (int i = 1; i < ctx.unaryExpression().size(); i++) {
            String right = visit(ctx.unaryExpression(i));
            String opText = ctx.getChild(2 * i - 1).getText();
            OpCode op = switch (opText) {
                case "*" -> OpCode.MULT;
                case "/" -> OpCode.DIV;
                case "%" -> OpCode.MOD;
                default -> OpCode.MULT;
            };
            String res = program.newTemp();
            program.emit(op, left, right, res, line);
            left = res;
        }
        return left;
    }

    @Override
    public String visitUnaryExpression(YParser.UnaryExpressionContext ctx) {
        int line = ctx.getStart().getLine();
        if (ctx.postfixExpression() != null) {
            return visit(ctx.postfixExpression());
        }
        if (ctx.MINUS() != null) {
            String val = visit(ctx.unaryExpression());
            String res = program.newTemp();
            program.emit(OpCode.SUB, "0", safe(val), res, line);
            return res;
        }
        if (ctx.NOT() != null) {
            String val = visit(ctx.unaryExpression());
            String res = program.newTemp();
            program.emit(OpCode.NOT, safe(val), "", res, line);
            return res;
        }
        return "0";
    }

    private String safe(String s) {
        return (s == null || s.isEmpty()) ? "0" : s;
    }

    @Override
    public String visitPrimaryExpression(YParser.PrimaryExpressionContext ctx) {
        int line = ctx.getStart().getLine();
        if (ctx.literal() != null) return visit(ctx.literal());
        if (ctx.LEER() != null) {
            String res = program.newTemp();
            program.emit(OpCode.READ, "", "", res, line);
            return res;
        }
        if (ctx.ID() != null && ctx.LPAREN() != null) {
            String funcName = ctx.ID().getText();
            int frameSize = memory.getCallFrameOffset(currentRoutine);
            String tNew = program.newTemp();
            program.emit(OpCode.ADD, "P", String.valueOf(frameSize), tNew, line, "Calcular nuevo marco");

            if (ctx.argumentList() != null) {
                int pIdx = 1;
                for (YParser.ExpressionContext argExpr : ctx.argumentList().expression()) {
                    String argVal = visit(argExpr);
                    String pAddr = program.newTemp();
                    program.emit(OpCode.ADD, tNew, String.valueOf(pIdx++), pAddr, line);
                    program.emit(OpCode.STACK_WRITE, pAddr, argVal, "", line, "Param " + (pIdx - 1));
                }
            }
            program.emit(OpCode.ADD, "P", String.valueOf(frameSize), "P", line, "Avanzar P");
            program.emit(OpCode.CALL, funcName, "", "", line);
            program.emit(OpCode.SUB, "P", String.valueOf(frameSize), "P", line, "Restaurar P");

            String tRet = program.newTemp();
            program.emit(OpCode.STACK_READ, tNew, "", tRet, line, "Leer retorno");
            return tRet;
        }
        if (ctx.target() != null) {
            return loadTarget(ctx.target(), line);
        }
        if (ctx.expression() != null) return visit(ctx.expression());
        return "0";
    }

    @Override
    public String visitLiteral(YParser.LiteralContext ctx) {
        int line = ctx.getStart().getLine();
        if (ctx.NUMBER() != null || ctx.DECIMAL() != null) return ctx.getText();
        if (ctx.VERDADERO() != null) return "1";
        if (ctx.FALSO() != null) return "0";
        if (ctx.STRING() != null) {
            String str = ctx.STRING().getText();
            str = str.substring(1, str.length() - 1);
            str = unescapeString(str);
            String heapRef = program.newTemp();
            program.emit(OpCode.ASSIGN, "H", "", heapRef, line, "String \"" + str.replace("\n", "\\n") + "\"");
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
}
