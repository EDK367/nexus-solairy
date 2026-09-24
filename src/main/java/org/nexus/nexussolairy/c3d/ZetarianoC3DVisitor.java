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
    private final java.util.Map<String, String> varClasses = new java.util.HashMap<>();

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
                    if (fdc.ID() == null) continue;
                    String fieldName = fdc.ID().getText();
                    fieldMap.put(fieldName, fIdx++);
                    if (fdc.type() != null) {
                        String fType = fdc.type().getText();
                        if (isStringType(fType)) {
                            stringVars.add(fieldName);
                        } else {
                            varClasses.put(fieldName, fType);
                        }
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
        if (className != null) {
            java.util.Map<String, Integer> fields = classFieldOffsets.get(className);
            if (fields != null && fields.containsKey(fieldName)) {
                return fields.get(fieldName);
            }
            int off = memory.getClassFieldOffset(className, fieldName);
            if (off >= 0) return off;
        }
        int off = memory.getClassFieldOffset(currentClass, fieldName);
        return off >= 0 ? off : 0;
    }

    private String resolveTargetClass(String targetName, String methodName) {
        if ("this".equals(targetName)) {
            return currentClass;
        }
        if (targetName != null) {
            String cls = varClasses.get(targetName);
            if (cls != null && !cls.isEmpty()) {
                return cls;
            }
            if (symbolTable != null) {
                org.nexus.nexussolairy.model.semantic.Symbol s = symbolTable.lookup(targetName);
                if (s != null && s.getStructTypeName() != null) {
                    return s.getStructTypeName();
                }
                if (currentClass != null && !currentClass.isEmpty()) {
                    org.nexus.nexussolairy.model.semantic.ClassSymbol curCs = symbolTable.lookupClass(currentClass);
                    if (curCs != null) {
                        org.nexus.nexussolairy.model.semantic.Symbol f = curCs.resolveField(targetName);
                        if (f != null && f.getSemanticType() != null) {
                            return f.getSemanticType().getName();
                        }
                    }
                }
            }
        }
        if (methodName != null && symbolTable != null) {
            for (org.nexus.nexussolairy.model.semantic.ClassSymbol cs : symbolTable.getClassRegistry().values()) {
                if (cs.resolveMethod(methodName) != null && !cs.resolveMethod(methodName).isEmpty()) {
                    return cs.getName();
                }
            }
        }
        return (currentClass != null && !currentClass.isEmpty()) ? currentClass : targetName;
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
                if (p.type() != null) {
                    String pType = p.type().getText();
                    if (isStringType(pType)) {
                        stringVars.add(pName);
                    } else {
                        varClasses.put(pName, pType);
                    }
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
                if (p.type() != null) {
                    String pType = p.type().getText();
                    if (isStringType(pType)) {
                        stringVars.add(pName);
                    } else {
                        varClasses.put(pName, pType);
                    }
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
        if (ctx.type() != null) {
            String vType = ctx.type().getText();
            if (isStringType(vType)) {
                stringVars.add(varName);
            } else {
                varClasses.put(varName, vType);
            }
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

    private String loadLeftValue(ZetarianoParser.LeftValueContext ctx, int line) {
        if (ctx == null) return "0";
        if (ctx.DOT() != null && ctx.ID().size() == 2) {
            String objName = ctx.ID(0).getText();
            String fieldName = ctx.ID(1).getText();
            String objPtr;
            if ("this".equals(objName)) {
                String pAddr = program.newTemp();
                program.emit(OpCode.ADD, "P", "1", pAddr, line);
                objPtr = program.newTemp();
                program.emit(OpCode.STACK_READ, pAddr, "", objPtr, line);
            } else {
                int localOff = memory.getLocalOffset(currentRoutine, objName);
                if (localOff < 0 && isClassField(objName)) {
                    String pThis = program.newTemp();
                    program.emit(OpCode.ADD, "P", "1", pThis, line);
                    String thisPtr = program.newTemp();
                    program.emit(OpCode.STACK_READ, pThis, "", thisPtr, line);
                    int off = getFieldOffset(currentClass, objName);
                    String fAddr = program.newTemp();
                    program.emit(OpCode.ADD, thisPtr, String.valueOf(off), fAddr, line);
                    objPtr = program.newTemp();
                    program.emit(OpCode.HEAP_READ, fAddr, "", objPtr, line);
                } else {
                    int offset = localOff;
                    if (offset < 0) offset = memory.declareLocal(currentRoutine, objName);
                    String pAddr = program.newTemp();
                    program.emit(OpCode.ADD, "P", String.valueOf(offset), pAddr, line);
                    objPtr = program.newTemp();
                    program.emit(OpCode.STACK_READ, pAddr, "", objPtr, line);
                }
            }
            String targetClass = resolveTargetClass(objName, null);
            int fieldOffset = getFieldOffset(targetClass, fieldName);
            String fieldAddr = program.newTemp();
            program.emit(OpCode.ADD, objPtr, String.valueOf(fieldOffset), fieldAddr, line);
            String res = program.newTemp();
            program.emit(OpCode.HEAP_READ, fieldAddr, "", res, line);
            return res;
        }

        if (ctx.LBRACK() != null && ctx.expression() != null) {
            String arrName = ctx.ID(0).getText();
            int offset = memory.getLocalOffset(currentRoutine, arrName);
            if (offset < 0) offset = memory.declareLocal(currentRoutine, arrName);
            String pAddr = program.newTemp();
            program.emit(OpCode.ADD, "P", String.valueOf(offset), pAddr, line);
            String arrPtr = program.newTemp();
            program.emit(OpCode.STACK_READ, pAddr, "", arrPtr, line);
            String idxTemp = visit(ctx.expression());
            String elemAddr = program.newTemp();
            program.emit(OpCode.ADD, arrPtr, idxTemp, elemAddr, line);
            String res = program.newTemp();
            program.emit(OpCode.HEAP_READ, elemAddr, "", res, line);
            return res;
        }

        String varName = ctx.ID(0).getText();
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

    private void storeLeftValue(ZetarianoParser.LeftValueContext ctx, String valTemp, int line) {
        if (ctx == null) return;
        if (ctx.DOT() != null && ctx.ID().size() == 2) {
            String objName = ctx.ID(0).getText();
            String fieldName = ctx.ID(1).getText();
            String objPtr;
            if ("this".equals(objName)) {
                String pAddr = program.newTemp();
                program.emit(OpCode.ADD, "P", "1", pAddr, line);
                objPtr = program.newTemp();
                program.emit(OpCode.STACK_READ, pAddr, "", objPtr, line);
            } else {
                int localOff = memory.getLocalOffset(currentRoutine, objName);
                if (localOff < 0 && isClassField(objName)) {
                    String pThis = program.newTemp();
                    program.emit(OpCode.ADD, "P", "1", pThis, line);
                    String thisPtr = program.newTemp();
                    program.emit(OpCode.STACK_READ, pThis, "", thisPtr, line);
                    int off = getFieldOffset(currentClass, objName);
                    String fAddr = program.newTemp();
                    program.emit(OpCode.ADD, thisPtr, String.valueOf(off), fAddr, line);
                    objPtr = program.newTemp();
                    program.emit(OpCode.HEAP_READ, fAddr, "", objPtr, line);
                } else {
                    int offset = localOff;
                    if (offset < 0) offset = memory.declareLocal(currentRoutine, objName);
                    String pAddr = program.newTemp();
                    program.emit(OpCode.ADD, "P", String.valueOf(offset), pAddr, line);
                    objPtr = program.newTemp();
                    program.emit(OpCode.STACK_READ, pAddr, "", objPtr, line);
                }
            }
            String targetClass = resolveTargetClass(objName, null);
            int fieldOffset = getFieldOffset(targetClass, fieldName);
            String fieldAddr = program.newTemp();
            program.emit(OpCode.ADD, objPtr, String.valueOf(fieldOffset), fieldAddr, line);
            program.emit(OpCode.HEAP_WRITE, fieldAddr, valTemp, "", line);
            return;
        }

        if (ctx.LBRACK() != null && ctx.expression() != null) {
            String arrName = ctx.ID(0).getText();
            int offset = memory.getLocalOffset(currentRoutine, arrName);
            if (offset < 0) offset = memory.declareLocal(currentRoutine, arrName);
            String pAddr = program.newTemp();
            program.emit(OpCode.ADD, "P", String.valueOf(offset), pAddr, line);
            String arrPtr = program.newTemp();
            program.emit(OpCode.STACK_READ, pAddr, "", arrPtr, line);
            String idxTemp = visit(ctx.expression());
            String elemAddr = program.newTemp();
            program.emit(OpCode.ADD, arrPtr, idxTemp, elemAddr, line);
            program.emit(OpCode.HEAP_WRITE, elemAddr, valTemp, "", line);
            return;
        }

        String varName = ctx.ID(0).getText();
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
            return;
        }

        int offset = localOff;
        if (offset < 0) offset = memory.declareLocal(currentRoutine, varName);
        String pAddr = program.newTemp();
        program.emit(OpCode.ADD, "P", String.valueOf(offset), pAddr, line);
        program.emit(OpCode.STACK_WRITE, pAddr, valTemp, "", line);
    }

    @Override
    public String visitAssignStmt(ZetarianoParser.AssignStmtContext ctx) {
        int line = ctx.getStart().getLine();
        if (ctx.INC() != null || ctx.DEC() != null) {
            String curVal = loadLeftValue(ctx.leftValue(), line);
            String nextVal = program.newTemp();
            OpCode op = ctx.INC() != null ? OpCode.ADD : OpCode.SUB;
            program.emit(op, curVal, "1", nextVal, line);
            storeLeftValue(ctx.leftValue(), nextVal, line);
            return null;
        }

        String valTemp = visit(ctx.expression());
        if (ctx.ADD_ASSIGN() != null || ctx.SUB_ASSIGN() != null || ctx.MUL_ASSIGN() != null || ctx.DIV_ASSIGN() != null) {
            String curVal = loadLeftValue(ctx.leftValue(), line);
            String nextVal = program.newTemp();
            OpCode op = ctx.ADD_ASSIGN() != null ? OpCode.ADD :
                        ctx.SUB_ASSIGN() != null ? OpCode.SUB :
                        ctx.MUL_ASSIGN() != null ? OpCode.MULT : OpCode.DIV;
            program.emit(op, curVal, valTemp, nextVal, line);
            storeLeftValue(ctx.leftValue(), nextVal, line);
            return null;
        }

        storeLeftValue(ctx.leftValue(), valTemp, line);
        return null;
    }

    @Override
    public String visitForInit(ZetarianoParser.ForInitContext ctx) {
        int line = ctx.getStart().getLine();
        if (ctx.type() != null) {
            String varName = ctx.ID().getText();
            int offset = memory.declareLocal(currentRoutine, varName);
            String valTemp = "0";
            if (ctx.expression() != null) {
                valTemp = visit(ctx.expression());
            }
            String pAddr = program.newTemp();
            program.emit(OpCode.ADD, "P", String.valueOf(offset), pAddr, line);
            program.emit(OpCode.STACK_WRITE, pAddr, valTemp, "", line);
        } else if (ctx.ID() != null) {
            String varName = ctx.ID().getText();
            String valTemp = visit(ctx.expression());
            int offset = memory.getLocalOffset(currentRoutine, varName);
            if (offset < 0) offset = memory.declareLocal(currentRoutine, varName);
            String pAddr = program.newTemp();
            program.emit(OpCode.ADD, "P", String.valueOf(offset), pAddr, line);
            program.emit(OpCode.STACK_WRITE, pAddr, valTemp, "", line);
        }
        return null;
    }

    @Override
    public String visitForUpdate(ZetarianoParser.ForUpdateContext ctx) {
        int line = ctx.getStart().getLine();
        if (ctx.INC() != null || ctx.DEC() != null) {
            String curVal = loadLeftValue(ctx.leftValue(), line);
            String nextVal = program.newTemp();
            OpCode op = ctx.INC() != null ? OpCode.ADD : OpCode.SUB;
            program.emit(op, curVal, "1", nextVal, line);
            storeLeftValue(ctx.leftValue(), nextVal, line);
            return null;
        }

        String valTemp = visit(ctx.expression());
        if (ctx.ASSIGN() != null) {
            storeLeftValue(ctx.leftValue(), valTemp, line);
        } else {
            String curVal = loadLeftValue(ctx.leftValue(), line);
            String nextVal = program.newTemp();
            OpCode op = ctx.ADD_ASSIGN() != null ? OpCode.ADD :
                        ctx.SUB_ASSIGN() != null ? OpCode.SUB :
                        ctx.MUL_ASSIGN() != null ? OpCode.MULT : OpCode.DIV;
            program.emit(op, curVal, valTemp, nextVal, line);
            storeLeftValue(ctx.leftValue(), nextVal, line);
        }
        return null;
    }

    @Override
    public String visitBreakStmt(ZetarianoParser.BreakStmtContext ctx) {
        if (!loopExitLabels.isEmpty()) {
            program.emit(OpCode.GOTO, "", "", loopExitLabels.peek(), ctx.getStart().getLine());
        }
        return null;
    }

    @Override
    public String visitContinueStmt(ZetarianoParser.ContinueStmtContext ctx) {
        if (!loopUpdateLabels.isEmpty()) {
            program.emit(OpCode.GOTO, "", "", loopUpdateLabels.peek(), ctx.getStart().getLine());
        }
        return null;
    }

    @Override
    public String visitSwitchStmt(ZetarianoParser.SwitchStmtContext ctx) {
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
            if (ctx.caseBranch(i).statement() != null) {
                for (ZetarianoParser.StatementContext st : ctx.caseBranch(i).statement()) {
                    visit(st);
                }
            }
        }

        if (ctx.defaultBranch() != null) {
            program.emit(OpCode.LABEL, "", "", labelDefault, line);
            if (ctx.defaultBranch().statement() != null) {
                for (ZetarianoParser.StatementContext st : ctx.defaultBranch().statement()) {
                    visit(st);
                }
            }
        }
        loopExitLabels.pop();
        program.emit(OpCode.LABEL, "", "", labelExit, line);
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
    public String visitEqExpr(ZetarianoParser.EqExprContext ctx) {
        int line = ctx.getStart().getLine();
        String left = visit(ctx.relExpr(0));
        for (int i = 1; i < ctx.relExpr().size(); i++) {
            String right = visit(ctx.relExpr(i));
            String opText = ctx.getChild(2 * i - 1).getText();
            OpCode op = "==".equals(opText) ? OpCode.EQ : OpCode.NEQ;
            String res = program.newTemp();
            program.emit(op, left, right, res, line);
            left = res;
        }
        return left;
    }

    @Override
    public String visitRelExpr(ZetarianoParser.RelExprContext ctx) {
        int line = ctx.getStart().getLine();
        String left = visit(ctx.addExpr(0));
        for (int i = 1; i < ctx.addExpr().size(); i++) {
            String right = visit(ctx.addExpr(i));
            String opText = ctx.getChild(2 * i - 1).getText();
            OpCode op = switch (opText) {
                case "<" -> OpCode.LT;
                case "<=" -> OpCode.LE;
                case ">" -> OpCode.GT;
                case ">=" -> OpCode.GE;
                default -> OpCode.LT;
            };
            String res = program.newTemp();
            program.emit(op, left, right, res, line);
            left = res;
        }
        return left;
    }

    @Override
    public String visitAddExpr(ZetarianoParser.AddExprContext ctx) {
        int line = ctx.getStart().getLine();
        String left = visit(ctx.mulExpr(0));
        for (int i = 1; i < ctx.mulExpr().size(); i++) {
            String right = visit(ctx.mulExpr(i));
            String opText = ctx.getChild(2 * i - 1).getText();
            String res = program.newTemp();
            if ("+".equals(opText) && (isStringExpr(ctx.mulExpr(i - 1)) || isStringExpr(ctx.mulExpr(i)) || stringVars.contains(left))) {
                boolean leftIsStr = isStringExpr(ctx.mulExpr(i - 1)) || stringVars.contains(left);
                boolean rightIsStr = isStringExpr(ctx.mulExpr(i)) || stringVars.contains(right);
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
    public String visitPostfixExpr(ZetarianoParser.PostfixExprContext ctx) {
        int line = ctx.getStart().getLine();
        if (ctx.DOT() != null && ctx.LPAREN() != null) {
            String targetName = ctx.postfixExpr().getText();
            String methodName = ctx.ID().getText();
            String objPtr;
            if ("this".equals(targetName)) {
                String pAddr = program.newTemp();
                program.emit(OpCode.ADD, "P", "1", pAddr, line);
                objPtr = program.newTemp();
                program.emit(OpCode.STACK_READ, pAddr, "", objPtr, line);
            } else {
                objPtr = visit(ctx.postfixExpr());
            }
            int frameOffset = memory.getCallFrameOffset(currentRoutine);
            String tNew = program.newTemp();
            program.emit(OpCode.ADD, "P", String.valueOf(frameOffset), tNew, line);
            String thisAddr = program.newTemp();
            program.emit(OpCode.ADD, tNew, "1", thisAddr, line);
            program.emit(OpCode.STACK_WRITE, thisAddr, objPtr, "", line, "Pass this");

            if (ctx.argList() != null) {
                int pIdx = 2;
                for (ZetarianoParser.ExpressionContext argExpr : ctx.argList().expression()) {
                    String argVal = visit(argExpr);
                    String pAddr = program.newTemp();
                    program.emit(OpCode.ADD, tNew, String.valueOf(pIdx++), pAddr, line);
                    program.emit(OpCode.STACK_WRITE, pAddr, argVal, "", line);
                }
            }
            String targetClass = resolveTargetClass(targetName, methodName);
            program.emit(OpCode.ADD, "P", String.valueOf(frameOffset), "P", line);
            program.emit(OpCode.CALL, targetClass + "_" + methodName, "", "", line);
            program.emit(OpCode.SUB, "P", String.valueOf(frameOffset), "P", line);
            String tRet = program.newTemp();
            program.emit(OpCode.STACK_READ, tNew, "", tRet, line);
            return tRet;
        }
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
                objPtr = visit(ctx.postfixExpr());
            }
            String targetClass = resolveTargetClass(targetName, null);
            int fieldOffset = getFieldOffset(targetClass, fieldName);
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
        if (ctx.READ() != null) {
            String readTemp = program.newTemp();
            program.emit(OpCode.READ, "", "", readTemp, line);
            return readTemp;
        }
        if (ctx.arrayInit() != null) {
            String heapRef = program.newTemp();
            program.emit(OpCode.ASSIGN, "H", "", heapRef, line, "Array literal");
            if (ctx.arrayInit().argList() != null) {
                for (ZetarianoParser.ExpressionContext e : ctx.arrayInit().argList().expression()) {
                    String elemVal = visit(e);
                    program.emit(OpCode.HEAP_WRITE, "H", elemVal, "", line);
                    String newH = program.newTemp();
                    program.emit(OpCode.ADD, "H", "1", newH, line);
                    program.emit(OpCode.ASSIGN, newH, "", "H", line);
                }
            }
            return heapRef;
        }
        if (ctx.newExpr() != null) return visit(ctx.newExpr());
        if (ctx.ID() != null && ctx.LPAREN() != null) {
            String methodName = ctx.ID().getText();
            int frameOffset = memory.getCallFrameOffset(currentRoutine);
            String tNew = program.newTemp();
            program.emit(OpCode.ADD, "P", String.valueOf(frameOffset), tNew, line);
            String pThis = program.newTemp();
            program.emit(OpCode.ADD, "P", "1", pThis, line);
            String thisPtr = program.newTemp();
            program.emit(OpCode.STACK_READ, pThis, "", thisPtr, line);
            String thisAddr = program.newTemp();
            program.emit(OpCode.ADD, tNew, "1", thisAddr, line);
            program.emit(OpCode.STACK_WRITE, thisAddr, thisPtr, "", line);

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
            program.emit(OpCode.CALL, currentClass + "_" + methodName, "", "", line);
            program.emit(OpCode.SUB, "P", String.valueOf(frameOffset), "P", line);
            String tRet = program.newTemp();
            program.emit(OpCode.STACK_READ, tNew, "", tRet, line);
            return tRet;
        }
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
