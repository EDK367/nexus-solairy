package org.nexus.nexussolairy.model.c3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class C3DProgram {
    // instrucciones para codigo intermedio
    private final List<Quadruple> quadruples; // uso de quadruples para facilitar el asembler en un futuro
    private int tempCounter;
    private int labelCounter;

    public C3DProgram() {
        this.quadruples = new ArrayList<>();
        this.tempCounter = 0;
        this.labelCounter = 0;
    }

    public String newTemp() {
        return "t" + (tempCounter++);
    }

    public String newLabel() {
        return "L" + (labelCounter++);
    }

    public void addQuad(Quadruple quad) {
        if (quad != null) {
            quadruples.add(quad);
        }
    }

    public Quadruple emit(OpCode op, String arg1, String arg2, String result, int line) {
        return emit(op, arg1, arg2, result, line, "");
    }

    public Quadruple emit(OpCode op, String arg1, String arg2, String result, int line, String comment) {
        String a1 = arg1 != null ? arg1 : "";
        String a2 = arg2 != null ? arg2 : "";
        String res = result != null ? result : "";
        String c = comment != null ? comment : "";
        Quadruple q = new Quadruple(op, a1, a2, res, line, c);
        quadruples.add(q);
        return q;
    }

    public List<Quadruple> getQuadruples() {
        return Collections.unmodifiableList(quadruples);
    }

    public int getTempCounter() {
        return tempCounter;
    }

    public int getLabelCounter() {
        return labelCounter;
    }

    public String toCodeString() {
        StringBuilder sb = new StringBuilder();
        sb.append("// ==========================================\n");
        sb.append("// Generated Output Nexus-Solairy\n");
        sb.append("// ==========================================\n\n");
        for (int i = 0; i < quadruples.size(); i++) {
            Quadruple q = quadruples.get(i);
            sb.append(String.format("%-4d: %s\n", i, q.toString()));
        }
        return sb.toString();
    }
}
