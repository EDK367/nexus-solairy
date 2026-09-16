package org.nexus.nexussolairy.backend.vm;

import org.nexus.nexussolairy.model.c3d.C3DProgram;
import org.nexus.nexussolairy.model.c3d.OpCode;
import org.nexus.nexussolairy.model.c3d.Quadruple;

import java.util.*;
import java.util.function.Consumer;

public class C3DVirtualMachine {

    public static final int MEMORY_SIZE = 30101999;

    private final double[] stack = new double[100000];
    private final double[] heap = new double[100000];
    private double P = 0;
    private double H = 0;

    private final Map<String, Double> temporals = new HashMap<>();
    private final Map<String, Integer> labelMap = new HashMap<>();
    private final Deque<Integer> returnStack = new ArrayDeque<>();

    private List<Quadruple> quadruples = Collections.emptyList();
    private int pc = 0;
    private boolean halted = true;

    private Consumer<String> consoleOutput;

    public C3DVirtualMachine() {
    }

    public void setConsoleOutput(Consumer<String> consoleOutput) {
        this.consoleOutput = consoleOutput;
    }

    public void loadProgram(C3DProgram program) {
        reset();
        if (program != null) {
            this.quadruples = program.getQuadruples();
            this.labelMap.clear();
            for (int i = 0; i < quadruples.size(); i++) {
                Quadruple q = quadruples.get(i);
                if (q.getOp() == OpCode.LABEL) {
                    labelMap.put(q.getResult(), i);
                }
            }
            if (labelMap.containsKey("main_entry")) {
                pc = labelMap.get("main_entry");
            } else if (labelMap.containsKey("principal")) {
                pc = labelMap.get("principal");
            } else if (labelMap.containsKey("main")) {
                pc = labelMap.get("main");
            }
            this.halted = quadruples.isEmpty();
        }
    }

    public void reset() {
        Arrays.fill(stack, 0);
        Arrays.fill(heap, 0);
        P = 0;
        H = 0;
        temporals.clear();
        labelMap.clear();
        returnStack.clear();
        pc = 0;
        halted = true;
    }

    public boolean isHalted() {
        return halted || pc < 0 || pc >= quadruples.size();
    }

    public int getPc() {
        return pc;
    }

    public double getP() {
        return P;
    }

    public double getH() {
        return H;
    }

    public double[] getStack() {
        return stack;
    }

    public double[] getHeap() {
        return heap;
    }

    public Map<String, Double> getTemporals() {
        return Collections.unmodifiableMap(temporals);
    }

    public List<Quadruple> getQuadruples() {
        return quadruples;
    }

    public boolean step() {
        if (isHalted()) return false;

        Quadruple q = quadruples.get(pc);
        int currentLine = pc;
        pc++; // Por defecto avanzar a la siguiente

        executeQuad(q);

        if (pc >= quadruples.size()) {
            halted = true;
        }
        return !isHalted();
    }

    public void runAll() {
        while (!isHalted()) {
            step();
        }
    }

    private void executeQuad(Quadruple q) {
        OpCode op = q.getOp();
        String arg1 = q.getArg1();
        String arg2 = q.getArg2();
        String res = q.getResult();

        switch (op) {
            case LABEL -> { /* NOP */ }
            case GOTO -> jumpToLabel(res);
            case IF_TRUE -> {
                if (evalVal(arg1) != 0) jumpToLabel(res);
            }
            case IF_FALSE -> {
                if (evalVal(arg1) == 0) jumpToLabel(res);
            }
            case ASSIGN -> setVal(res, evalVal(arg1));
            case ADD -> setVal(res, evalVal(arg1) + evalVal(arg2));
            case SUB -> setVal(res, evalVal(arg1) - evalVal(arg2));
            case MULT -> setVal(res, evalVal(arg1) * evalVal(arg2));
            case DIV -> {
                double divisor = evalVal(arg2);
                setVal(res, divisor != 0 ? evalVal(arg1) / divisor : 0);
            }
            case MOD -> {
                double divisor = evalVal(arg2);
                setVal(res, divisor != 0 ? evalVal(arg1) % divisor : 0);
            }
            case EQ -> setVal(res, evalVal(arg1) == evalVal(arg2) ? 1 : 0);
            case NEQ -> setVal(res, evalVal(arg1) != evalVal(arg2) ? 1 : 0);
            case LT -> setVal(res, evalVal(arg1) < evalVal(arg2) ? 1 : 0);
            case LE -> setVal(res, evalVal(arg1) <= evalVal(arg2) ? 1 : 0);
            case GT -> setVal(res, evalVal(arg1) > evalVal(arg2) ? 1 : 0);
            case GE -> setVal(res, evalVal(arg1) >= evalVal(arg2) ? 1 : 0);
            case NOT -> setVal(res, evalVal(arg1) == 0 ? 1 : 0);
            case STACK_WRITE -> {
                int addr = (int) evalVal(arg1);
                if (addr >= 0 && addr < stack.length) stack[addr] = evalVal(arg2);
            }
            case STACK_READ -> {
                int addr = (int) evalVal(arg1);
                double val = (addr >= 0 && addr < stack.length) ? stack[addr] : 0;
                setVal(res, val);
            }
            case HEAP_WRITE -> {
                int addr = (int) evalVal(arg1);
                if (addr >= 0 && addr < heap.length) heap[addr] = evalVal(arg2);
            }
            case HEAP_READ -> {
                int addr = (int) evalVal(arg1);
                double val = (addr >= 0 && addr < heap.length) ? heap[addr] : 0;
                setVal(res, val);
            }
            case CALL -> {
                returnStack.push(pc);
                jumpToLabel(arg1);
            }
            case RETURN -> {
                if (!returnStack.isEmpty()) {
                    pc = returnStack.pop();
                } else {
                    halted = true;
                }
            }
            case PRINT -> {
                double val = evalVal(arg1);
                String msg = (val == (long) val) ? String.valueOf((long) val) : String.valueOf(val);
                if (consoleOutput != null) consoleOutput.accept(msg);
            }
            case PRINT_STR -> {
                int ptr = (int) evalVal(arg1);
                StringBuilder sb = new StringBuilder();
                while (ptr >= 0 && ptr < heap.length && heap[ptr] != -1) {
                    sb.append((char) heap[ptr]);
                    ptr++;
                }
                if (consoleOutput != null) consoleOutput.accept(sb.toString());
            }
        }
    }

    private void jumpToLabel(String label) {
        Integer targetPc = labelMap.get(label);
        if (targetPc != null) {
            pc = targetPc;
        }
    }

    private double evalVal(String expr) {
        if (expr == null || expr.isEmpty()) return 0;
        if ("P".equals(expr)) return P;
        if ("H".equals(expr)) return H;
        if (temporals.containsKey(expr)) return temporals.get(expr);
        try {
            return Double.parseDouble(expr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void setVal(String target, double val) {
        if (target == null || target.isEmpty()) return;
        if ("P".equals(target)) {
            P = val;
        } else if ("H".equals(target)) {
            H = val;
        } else {
            temporals.put(target, val);
        }
    }
}
