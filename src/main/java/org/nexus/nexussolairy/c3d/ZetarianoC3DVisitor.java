package org.nexus.nexussolairy.c3d;

import org.nexus.nexussolairy.ZetarianoParser;
import org.nexus.nexussolairy.ZetarianoParserBaseVisitor;
import org.nexus.nexussolairy.model.c3d.C3DProgram;
import org.nexus.nexussolairy.model.c3d.OpCode;

import java.util.ArrayDeque;
import java.util.Deque;

public class ZetarianoC3DVisitor extends ZetarianoParserBaseVisitor<String> {

    private final C3DProgram program;
    private final MemoryLayout memory;
    private String currentClass = "";
    private String currentRoutine = "main";

    private final Deque<String> loopStartLabels = new ArrayDeque<>();
    private final Deque<String> loopExitLabels = new ArrayDeque<>();
    private final Deque<String> loopUpdateLabels = new ArrayDeque<>();

    private final java.util.Map<String, java.util.Map<String, Integer>> classFieldOffsets = new java.util.LinkedHashMap<>();

    private final org.nexus.nexussolairy.model.semantic.SymbolTable symbolTable;

    public ZetarianoC3DVisitor(C3DProgram program, MemoryLayout memory) {
        this(program, memory, null);
    }

    public ZetarianoC3DVisitor(C3DProgram program, MemoryLayout memory, org.nexus.nexussolairy.model.semantic.SymbolTable symbolTable) {
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
    public String visitClassDecl(ZetarianoParser.ClassDeclContext ctx) {
        currentClass = ctx.ID().getText();

        if (ctx.classBody() != null) {
            java.util.Map<String, Integer> fieldMap = new java.util.LinkedHashMap<>();
            int fIdx = 0;
            for (var child : ctx.classBody().children) {
                if (child instanceof ZetarianoParser.FieldDeclContext fdc) {
                    String fieldName = fdc.ID().getText();
                    fieldMap.put(fieldName, fIdx++);
                    if (fdc.type() != null && isStringType(fdc.type().getText())) {
                        stringVars.add(fieldName);
                    }
                }
            }
            classFieldOffsets.put(currentClass, fieldMap);
            visit(ctx.classBody());
        }
        return null;
    }

    private boolean isClassField(String name) {
        if (currentClass == null || currentClass.isEmpty()) return false;
        java.util.Map<String, Integer> fields = classFieldOffsets.get(currentClass);
        if (fields != null && fields.containsKey(name)) return true;
        return memory.getClassFieldOffset(currentClass, name) >= 0;
    }

    private int getFieldOffset(String className, String fieldName) {
        java.util.Map<String, Integer> fields = classFieldOffsets.get(className);
        if (fields != null && fields.containsKey(fieldName)) {
            return fields.get(fieldName);
        }
        int off = memory.getClassFieldOffset(className, fieldName);
        return off >= 0 ? off : 0;
    }

    @Override
    public String visitConstructorDecl(ZetarianoParser.ConstructorDeclContext ctx) {
        String ctorName = currentClass + "_ctor";
        currentRoutine = ctorName;
        int line = ctx.getStart().getLine();

        program.emit(OpCode.LABEL, "", "", ctorName, line, "Constructor " + currentClass);
        memory.declareParam(ctorName, "this");

        if (ctx.paramList() != null) {
            for (ZetarianoParser.ParamContext p : ctx.paramList().param()) {
                String pName = p.ID().getText();
                memory.declareParam(ctorName, pName);
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
    public String visitMethodDecl(ZetarianoParser.MethodDeclContext ctx) {
        String methodName = currentClass + "_" + ctx.ID().getText();
        currentRoutine = methodName;
        int line = ctx.getStart().getLine();

        program.emit(OpCode.LABEL, "", "", methodName, line, "Método " + methodName);
        memory.declareParam(methodName, "this");

        if (ctx.paramList() != null) {
            for (ZetarianoParser.ParamContext p : ctx.paramList().param()) {
                String pName = p.ID().getText();
                memory.declareParam(methodName, pName);
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
    public String visitVarDecl(ZetarianoParser.VarDeclContext ctx) {
        int line = ctx.getStart().getLine();
        String varName = ctx.ID().getText();
        if (ctx.type() != null && isStringType(ctx.type().getText())) {
            stringVars.add(varName);
        }
        int offset = memory.declareLocal(currentRoutine, varName);

        String valTemp = "0";
        if (ctx.expression() != null) {
            valTemp = visit(ctx.expression());
        }
        String pAddr = program.newTemp();
        program.emit(OpCode.ADD, "P", String.valueOf(offset), pAddr, line);
        program.emit(OpCode.STACK_WRITE, pAddr, valTemp, "", line, "Var local " + varName);
        return null;
    }

    @Override
    public String visitAssignStmt(ZetarianoParser.AssignStmtContext ctx) {
        int line = ctx.getStart().getLine();
        String valTemp = visit(ctx.expression());

        if (ctx.leftValue() != null && ctx.leftValue().ID().size() == 2) {
            String objName = ctx.leftValue().ID(0).getText();
            String fieldName = ctx.leftValue().ID(1).getText();
            String objPtr;
            if ("this".equals(objName)) {
                String pAddr = program.newTemp();
                program.emit(OpCode.ADD, "P", "1", pAddr, line);
                objPtr = program.newTemp();
                program.emit(OpCode.STACK_READ, pAddr, "", objPtr, line);
            } else {
                int offset = memory.getLocalOffset(currentRoutine, objName);
                if (offset < 0) offset = memory.declareLocal(currentRoutine, objName);
                String pAddr = program.newTemp();
                program.emit(OpCode.ADD, "P", String.valueOf(offset), pAddr, line);
                objPtr = program.newTemp();
                program.emit(OpCode.STACK_READ, pAddr, "", objPtr, line);
            }
            int fieldOffset = getFieldOffset(currentClass, fieldName);
            String fieldAddr = program.newTemp();
            program.emit(OpCode.ADD, objPtr, String.valueOf(fieldOffset), fieldAddr, line);
            program.emit(OpCode.HEAP_WRITE, fieldAddr, valTemp, "", line);
            return null;
        }

        String varName = ctx.leftValue().ID(0).getText();
        int localOff = memory.getLocalOffset(currentRoutine, varName);
        if (localOff < 0 && isClassField(varName)) {
            String pThis = program.newTemp();
            program.emit(OpCode.ADD, "P", "1", pThis, line);
            String thisPtr = program.newTemp();
            program.emit(OpCode.STACK_READ, pThis, "", thisPtr, line);
            int fieldOffset = getFieldOffset(currentClass, varName);
            String fieldAddr = program.newTemp();
            program.emit(OpCode.ADD, thisPtr, String.valueOf(fieldOffset), fieldAddr, line);
            program.emit(OpCode.HEAP_WRITE, fieldAddr, valTemp, "", line);
            return null;
        }

        int offset = localOff;
        if (offset < 0) offset = memory.declareLocal(currentRoutine, varName);
        String pAddr = program.newTemp();
        program.emit(OpCode.ADD, "P", String.valueOf(offset), pAddr, line);
        program.emit(OpCode.STACK_WRITE, pAddr, valTemp, "", line);
        return null;
    }

    @Override
    public String visitIfStmt(ZetarianoParser.IfStmtContext ctx) {
        int line = ctx.getStart().getLine();
        String labelExit = program.newLabel();

        for (int i = 0; i < ctx.expression().size(); i++) {
            String labelNext = program.newLabel();
            String condTemp = visit(ctx.expression(i));
            program.emit(OpCode.IF_FALSE, condTemp, "", labelNext, line);

            if (i < ctx.ifBody().size()) {
                visit(ctx.ifBody(i));
            }
            program.emit(OpCode.GOTO, "", "", labelExit, line);
            program.emit(OpCode.LABEL, "", "", labelNext, line);
        }
        if (ctx.ELSE() != null && ctx.ifBody().size() > ctx.expression().size()) {
            visit(ctx.ifBody(ctx.ifBody().size() - 1));
        }
        program.emit(OpCode.LABEL, "", "", labelExit, line);
        return null;
    }

    @Override
    public String visitWhileStmt(ZetarianoParser.WhileStmtContext ctx) {
        int line = ctx.getStart().getLine();
        String labelStart = program.newLabel();
        String labelExit = program.newLabel();

        loopStartLabels.push(labelStart);
        loopExitLabels.push(labelExit);
        loopUpdateLabels.push(labelStart);

        program.emit(OpCode.LABEL, "", "", labelStart, line);
        String condTemp = visit(ctx.expression());
        program.emit(OpCode.IF_FALSE, condTemp, "", labelExit, line);

        if (ctx.block() != null) {
            visit(ctx.block());
        }
        program.emit(OpCode.GOTO, "", "", labelStart, line);
        program.emit(OpCode.LABEL, "", "", labelExit, line);

        loopStartLabels.pop();
        loopExitLabels.pop();
        loopUpdateLabels.pop();
        return null;
    }

    @Override
    public String visitDoWhileStmt(ZetarianoParser.DoWhileStmtContext ctx) {
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
    public String visitForStmt(ZetarianoParser.ForStmtContext ctx) {
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
        if (ctx.statement() != null) {
            visit(ctx.statement());
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
    public String visitNewExpr(ZetarianoParser.NewExprContext ctx) {
        int line = ctx.getStart().getLine();
        String clsName = ctx.type().getText();
        String objRef = program.newTemp();

        program.emit(OpCode.ASSIGN, "H", "", objRef, line, "Instancia novus/new " + clsName);
        String newH = program.newTemp();
        program.emit(OpCode.ADD, "H", "8", newH, line);
        program.emit(OpCode.ASSIGN, newH, "", "H", line);

        int frameOffset = memory.getCallFrameOffset(currentRoutine);
        String tNew = program.newTemp();
        program.emit(OpCode.ADD, "P", String.valueOf(frameOffset), tNew, line);

        String thisAddr = program.newTemp();
        program.emit(OpCode.ADD, tNew, "1", thisAddr, line);
        program.emit(OpCode.STACK_WRITE, thisAddr, objRef, "", line, "Pass 'this'");

        if (ctx.argList() != null) {
            int pIdx = 2;
            for (ZetarianoParser.ExpressionContext argExpr : ctx.argList().expression()) {
                String argVal = visit(argExpr);
                String pAddr = program.newTemp();
                program.emit(OpCode.ADD, tNew, String.valueOf(pIdx++), pAddr, line);
                program.emit(OpCode.STACK_WRITE, pAddr, argVal, "", line);
            }
        }
        program.emit(OpCode.ADD, "P", String.valueOf(frameOffset), "P", line);
        program.emit(OpCode.CALL, clsName + "_ctor", "", "", line);
        program.emit(OpCode.SUB, "P", String.valueOf(frameOffset), "P", line);
        return objRef;
    }

    @Override
    public String visitReturnStmt(ZetarianoParser.ReturnStmtContext ctx) {
        int line = ctx.getStart().getLine();
        String valTemp = ctx.expression() != null ? visit(ctx.expression()) : "";
        if (!valTemp.isEmpty()) {
            program.emit(OpCode.STACK_WRITE, "P", valTemp, "", line);
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
    public String visitPrintStmt(ZetarianoParser.PrintStmtContext ctx) {
        int line = ctx.getStart().getLine();
        String val = visit(ctx.expression());
        if (val == null || val.isEmpty()) val = "0";
        if (isStringExpr(ctx.expression())) {
            program.emit(OpCode.PRINT_STR, val, "", "", line);
            if (ctx.PRINTLN() != null || ctx.PRINT() != null) {
                String nlRef = createStringLiteral("\n", line);
                program.emit(OpCode.PRINT_STR, nlRef, "", "", line);
            }
        } else {
            program.emit(OpCode.PRINT, val, "", "", line);
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
    public String visitReadStmt(ZetarianoParser.ReadStmtContext ctx) {
        int line = ctx.getStart().getLine();
        String res = program.newTemp();
        program.emit(OpCode.READ, "", "", res, line);
        return res;
    }

    @Override
    public String visitOrExpr(ZetarianoParser.OrExprContext ctx) {
        int line = ctx.getStart().getLine();
        if (ctx.andExpr().size() == 1) return visit(ctx.andExpr(0));

        String labelTrue = program.newLabel();
        String labelEnd = program.newLabel();
        String resTemp = program.newTemp();

        for (ZetarianoParser.AndExprContext andCtx : ctx.andExpr()) {
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
    public String visitAndExpr(ZetarianoParser.AndExprContext ctx) {
        int line = ctx.getStart().getLine();
        if (ctx.eqExpr().size() == 1) return visit(ctx.eqExpr(0));

        String labelFalse = program.newLabel();
        String labelEnd = program.newLabel();
        String resTemp = program.newTemp();

        for (ZetarianoParser.EqExprContext eqCtx : ctx.eqExpr()) {
            String val = visit(eqCtx);
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
    public String visitAddExpr(ZetarianoParser.AddExprContext ctx) {
        int line = ctx.getStart().getLine();
        String left = visit(ctx.mulExpr(0));
        for (int i = 1; i < ctx.mulExpr().size(); i++) {
            String right = visit(ctx.mulExpr(i));
            String opText = ctx.getChild(2 * i - 1).getText();
            String res = program.newTemp();
            if ("+".equals(opText) && (isStringExpr(ctx.mulExpr(i - 1)) || isStringExpr(ctx.mulExpr(i)))) {
                program.emit(OpCode.CONCAT_STR, left, right, res, line);
            } else {
                OpCode op = "+".equals(opText) ? OpCode.ADD : OpCode.SUB;
                program.emit(op, left, right, res, line);
            }
            left = res;
        }
        return left;
    }

    @Override
    public String visitPostfixExpr(ZetarianoParser.PostfixExprContext ctx) {
        int line = ctx.getStart().getLine();
        if (ctx.DOT() != null) {
            String targetName = ctx.postfixExpr().getText();
            String fieldName = ctx.ID().getText();
            String objPtr;
            if ("this".equals(targetName)) {
                String pAddr = program.newTemp();
                program.emit(OpCode.ADD, "P", "1", pAddr, line);
                objPtr = program.newTemp();
                program.emit(OpCode.STACK_READ, pAddr, "", objPtr, line);
            } else {
                int offset = memory.getLocalOffset(currentRoutine, targetName);
                if (offset < 0) offset = memory.declareLocal(currentRoutine, targetName);
                String pAddr = program.newTemp();
                program.emit(OpCode.ADD, "P", String.valueOf(offset), pAddr, line);
                objPtr = program.newTemp();
                program.emit(OpCode.STACK_READ, pAddr, "", objPtr, line);
            }
            int fieldOffset = getFieldOffset(currentClass, fieldName);
            String fieldAddr = program.newTemp();
            program.emit(OpCode.ADD, objPtr, String.valueOf(fieldOffset), fieldAddr, line);
            String resTemp = program.newTemp();
            program.emit(OpCode.HEAP_READ, fieldAddr, "", resTemp, line);
            return resTemp;
        }
        return visit(ctx.primaryExpr());
    }

    @Override
    public String visitMulExpr(ZetarianoParser.MulExprContext ctx) {
        int line = ctx.getStart().getLine();
        String left = visit(ctx.unaryExpr(0));
        for (int i = 1; i < ctx.unaryExpr().size(); i++) {
            String right = visit(ctx.unaryExpr(i));
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
    public String visitPrimaryExpr(ZetarianoParser.PrimaryExprContext ctx) {
        int line = ctx.getStart().getLine();
        if (ctx.literal() != null) return visit(ctx.literal());
        if (ctx.NULL() != null) return "-1";
        if (ctx.newExpr() != null) return visit(ctx.newExpr());
        if (ctx.ID() != null) {
            String varName = ctx.ID().getText();
            int localOff = memory.getLocalOffset(currentRoutine, varName);
            if (localOff < 0 && isClassField(varName)) {
                String pThis = program.newTemp();
                program.emit(OpCode.ADD, "P", "1", pThis, line);
                String thisPtr = program.newTemp();
                program.emit(OpCode.STACK_READ, pThis, "", thisPtr, line);
                int fieldOffset = getFieldOffset(currentClass, varName);
                String fieldAddr = program.newTemp();
                program.emit(OpCode.ADD, thisPtr, String.valueOf(fieldOffset), fieldAddr, line);
                String res = program.newTemp();
                program.emit(OpCode.HEAP_READ, fieldAddr, "", res, line);
                return res;
            }

            int offset = localOff;
            if (offset < 0) offset = memory.declareLocal(currentRoutine, varName);
            String pAddr = program.newTemp();
            String res = program.newTemp();
            program.emit(OpCode.ADD, "P", String.valueOf(offset), pAddr, line);
            program.emit(OpCode.STACK_READ, pAddr, "", res, line);
            return res;
        }
        if (ctx.expression() != null) return visit(ctx.expression());
        return "0";
    }

    @Override
    public String visitUnaryExpr(ZetarianoParser.UnaryExprContext ctx) {
        int line = ctx.getStart().getLine();
        if (ctx.postfixExpr() != null) {
            return visit(ctx.postfixExpr());
        }
        if (ctx.MINUS() != null) {
            String val = visit(ctx.unaryExpr());
            String res = program.newTemp();
            program.emit(OpCode.SUB, "0", safe(val), res, line);
            return res;
        }
        if (ctx.PLUS() != null) {
            return visit(ctx.unaryExpr());
        }
        if (ctx.NOT() != null) {
            String val = visit(ctx.unaryExpr());
            String res = program.newTemp();
            program.emit(OpCode.NOT, safe(val), "", res, line);
            return res;
        }
        if (ctx.INC() != null || ctx.DEC() != null) {
            String val = visit(ctx.unaryExpr());
            OpCode op = ctx.INC() != null ? OpCode.ADD : OpCode.SUB;
            String res = program.newTemp();
            program.emit(op, safe(val), "1", res, line);
            return res;
        }
        return "0";
    }

    private String safe(String s) {
        return (s == null || s.isEmpty()) ? "0" : s;
    }

    @Override
    public String visitLiteral(ZetarianoParser.LiteralContext ctx) {
        int line = ctx.getStart().getLine();
        if (ctx.NUMBER() != null || ctx.DECIMAL() != null) return ctx.getText();
        if (ctx.TRUE() != null) return "1";
        if (ctx.FALSE() != null) return "0";
        if (ctx.STRING() != null) {
            String str = ctx.STRING().getText();
            str = str.substring(1, str.length() - 1);
            String heapRef = program.newTemp();
            program.emit(OpCode.ASSIGN, "H", "", heapRef, line, "String \"" + str + "\"");
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
