package org.nexus.nexussolairy.backend.c;

import org.nexus.nexussolairy.model.c3d.C3DProgram;
import org.nexus.nexussolairy.model.c3d.OpCode;
import org.nexus.nexussolairy.model.c3d.Quadruple;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class CSourceCodeGenerator {

    public String generateC(C3DProgram program) {
        StringBuilder sb = new StringBuilder();

        sb.append("// ========================================================\n");
        sb.append("// C Code Generated from Nexus-Solairy C3D IR\n");
        sb.append("// ========================================================\n\n");
        sb.append("#include <stdio.h>\n");
        sb.append("#include <stdlib.h>\n");
        sb.append("#include <stdbool.h>\n");
        sb.append("#include <math.h>\n\n");

        sb.append("#define STACK_SIZE 30101999\n");
        sb.append("#define HEAP_SIZE  30101999\n\n");

        sb.append("double stack[STACK_SIZE];\n");
        sb.append("double heap[HEAP_SIZE];\n");
        sb.append("double P = 0;\n");
        sb.append("double H = 0;\n\n");

        // recoleccion de los temporales
        Set<String> temps = new LinkedHashSet<>();
        for (Quadruple q : program.getQuadruples()) {
            if (isTemp(q.getResult())) temps.add(q.getResult());
            if (isTemp(q.getArg1())) temps.add(q.getArg1());
            if (isTemp(q.getArg2())) temps.add(q.getArg2());
        }

        if (!temps.isEmpty()) {
            sb.append("// Declaración de temporales\n");
            sb.append("double ");
            sb.append(String.join(", ", temps));
            sb.append(";\n\n");
        }

        // rutinas declaradas
        Set<String> routines = new LinkedHashSet<>();
        for (Quadruple q : program.getQuadruples()) {
            if (q.getOp() == OpCode.CALL && q.getArg1() != null && !q.getArg1().isEmpty()) {
                routines.add(q.getArg1());
            }
            if (q.getOp() == OpCode.LABEL && q.getResult() != null && !q.getResult().isEmpty()) {
                String lbl = q.getResult();
                if (!lbl.matches("L\\d+") && !"main_entry".equals(lbl)) {
                    routines.add(lbl);
                }
            }
        }

        if (!routines.isEmpty()) {
            sb.append("// Declaración previa de funciones C\n");
            for (String r : routines) {
                sb.append("void ").append(r).append("();\n");
            }
            sb.append("\n");
        }

        // rutinas nativas
        sb.append("// Rutinas nativas de apoyo (C3D)\n");
        sb.append("void native_print_string(double ptr) {\n");
        sb.append("    int idx = (int)ptr;\n");
        sb.append("    while (idx >= 0 && idx < HEAP_SIZE && heap[idx] != -1) {\n");
        sb.append("        printf(\"%c\", (char)heap[idx]);\n");
        sb.append("        idx++;\n");
        sb.append("    }\n");
        sb.append("    fflush(stdout);\n");
        sb.append("}\n\n");

        sb.append("double native_read_double() {\n");
        sb.append("    double val = 0;\n");
        sb.append("    int r = scanf(\"%lf\", &val);\n");
        sb.append("    if (r != 1) {\n");
        sb.append("        int ch;\n");
        sb.append("        while ((ch = getchar()) != '\\n' && ch != EOF);\n");
        sb.append("        return 0;\n");
        sb.append("    }\n");
        sb.append("    return val;\n");
        sb.append("}\n\n");

        sb.append("void native_append_str_to_heap(double ptr) {\n");
        sb.append("    int idx = (int)ptr;\n");
        sb.append("    int limit = (int)H;\n");
        sb.append("    while (idx >= 0 && idx < limit && heap[idx] != -1) {\n");
        sb.append("        if ((int)H < HEAP_SIZE) heap[(int)H++] = heap[idx++];\n");
        sb.append("    }\n");
        sb.append("}\n\n");

        sb.append("void native_append_num_to_heap(double val) {\n");
        sb.append("    char buf[64];\n");
        sb.append("    if (val == (long)val) sprintf(buf, \"%ld\", (long)val);\n");
        sb.append("    else sprintf(buf, \"%g\", val);\n");
        sb.append("    for (int i = 0; buf[i]; i++) {\n");
        sb.append("        if ((int)H < HEAP_SIZE) heap[(int)H++] = (double)buf[i];\n");
        sb.append("    }\n");
        sb.append("}\n\n");

        sb.append("double native_concat_str_str(double ptr1, double ptr2) {\n");
        sb.append("    double start = H;\n");
        sb.append("    native_append_str_to_heap(ptr1);\n");
        sb.append("    native_append_str_to_heap(ptr2);\n");
        sb.append("    if ((int)H < HEAP_SIZE) heap[(int)H++] = -1;\n");
        sb.append("    return start;\n");
        sb.append("}\n\n");

        sb.append("double native_concat_str_num(double ptr1, double val2) {\n");
        sb.append("    double start = H;\n");
        sb.append("    native_append_str_to_heap(ptr1);\n");
        sb.append("    native_append_num_to_heap(val2);\n");
        sb.append("    if ((int)H < HEAP_SIZE) heap[(int)H++] = -1;\n");
        sb.append("    return start;\n");
        sb.append("}\n\n");

        sb.append("double native_concat_num_str(double val1, double ptr2) {\n");
        sb.append("    double start = H;\n");
        sb.append("    native_append_num_to_heap(val1);\n");
        sb.append("    native_append_str_to_heap(ptr2);\n");
        sb.append("    if ((int)H < HEAP_SIZE) heap[(int)H++] = -1;\n");
        sb.append("    return start;\n");
        sb.append("}\n\n");

        sb.append("double native_concat_string(double ptr1, double ptr2) {\n");
        sb.append("    return native_concat_str_str(ptr1, ptr2);\n");
        sb.append("}\n\n");

        Set<String> definedFunctions = new HashSet<>();
        boolean inMain = false;
        boolean inRoutine = false;

        for (Quadruple q : program.getQuadruples()) {
            if (q.getOp() == OpCode.LABEL && q.getResult() != null) {
                String lbl = q.getResult();
                if ("main_entry".equals(lbl)) {
                    if (inRoutine) {
                        sb.append("}\n\n");
                        inRoutine = false;
                    }
                    if (!inMain) {
                        sb.append("int main() {\n");
                        sb.append("    main_entry:;\n");
                        inMain = true;
                    } else {
                        sb.append("    main_entry:;\n");
                    }
                    continue;
                } else if (routines.contains(lbl)) {
                    if (definedFunctions.contains(lbl)) {
                        continue;
                    }
                    definedFunctions.add(lbl);
                    if (inRoutine) {
                        sb.append("}\n\n");
                    }
                    if (inMain) {
                        sb.append("\n    return 0;\n}\n\n");
                        inMain = false;
                    }
                    sb.append("void ").append(lbl).append("() {\n");
                    inRoutine = true;
                    continue;
                }
            }

            if (!inMain && !inRoutine) {
                sb.append("int main() {\n");
                inMain = true;
            }

            String translated = translateQuadToC(q, inRoutine);
            if (!translated.isEmpty()) {
                sb.append("    ").append(translated).append("\n");
            }
        }

        if (inRoutine) {
            sb.append("}\n\n");
        }
        if (inMain) {
            sb.append("\n    return 0;\n}\n");
        } else if (!definedFunctions.contains("main")) {
            sb.append("\nint main() {\n");
            if (definedFunctions.contains("principal")) {
                sb.append("    principal();\n");
            } else {
                for (String fn : definedFunctions) {
                    sb.append("    ").append(fn).append("();\n");
                }
            }
            sb.append("    return 0;\n}\n");
        }

        return sb.toString();
    }

    private boolean isTemp(String s) {
        return s != null && s.matches("t\\d+");
    }

    private String safe(String s) {
        if (s == null || s.trim().isEmpty()) return "0";
        return s;
    }

    private String translateQuadToC(Quadruple q, boolean inRoutine) {
        OpCode op = q.getOp();
        String arg1 = safe(q.getArg1());
        String arg2 = safe(q.getArg2());
        String res = safe(q.getResult());

        return switch (op) {
            case LABEL -> res + ":;";
            case GOTO -> "goto " + res + ";";
            case IF_TRUE -> "if (" + arg1 + ") goto " + res + ";";
            case IF_FALSE -> "if (!(" + arg1 + ")) goto " + res + ";";
            case ASSIGN -> res + " = " + arg1 + ";";
            case ADD -> res + " = " + arg1 + " + " + arg2 + ";";
            case SUB -> res + " = " + arg1 + " - " + arg2 + ";";
            case MULT -> res + " = " + arg1 + " * " + arg2 + ";";
            case DIV -> res + " = (" + arg2 + " != 0) ? (" + arg1 + " / " + arg2 + ") : 0;";
            case MOD -> res + " = fmod(" + arg1 + ", " + arg2 + ");";
            case EQ -> res + " = (" + arg1 + " == " + arg2 + ") ? 1 : 0;";
            case NEQ -> res + " = (" + arg1 + " != " + arg2 + ") ? 1 : 0;";
            case LT -> res + " = (" + arg1 + " < " + arg2 + ") ? 1 : 0;";
            case LE -> res + " = (" + arg1 + " <= " + arg2 + ") ? 1 : 0;";
            case GT -> res + " = (" + arg1 + " > " + arg2 + ") ? 1 : 0;";
            case GE -> res + " = (" + arg1 + " >= " + arg2 + ") ? 1 : 0;";
            case NOT -> res + " = !(" + arg1 + ");";
            case STACK_WRITE -> "stack[(int)(" + arg1 + ")] = " + arg2 + ";";
            case STACK_READ -> res.equals("0") ? "" : res + " = stack[(int)(" + arg1 + ")];";
            case HEAP_WRITE -> "heap[(int)(" + arg1 + ")] = " + arg2 + ";";
            case HEAP_READ -> res.equals("0") ? "" : res + " = heap[(int)(" + arg1 + ")];";
            case PRINT -> "printf(\"%g\\n\", " + arg1 + "); fflush(stdout);";
            case PRINT_STR -> "native_print_string(" + arg1 + ");";
            case CONCAT_STR -> {
                String mode = q.getComment();
                if ("str_num".equals(mode)) {
                    yield res + " = native_concat_str_num(" + arg1 + ", " + arg2 + ");";
                } else if ("num_str".equals(mode)) {
                    yield res + " = native_concat_num_str(" + arg1 + ", " + arg2 + ");";
                } else {
                    yield res + " = native_concat_str_str(" + arg1 + ", " + arg2 + ");";
                }
            }
            case READ -> res + " = native_read_double();";
            case PARAM -> "// param " + arg1;
            case CALL -> arg1 + "();";
            case RETURN -> inRoutine ? "return;" : "return 0;";
            default -> "// " + q.toString();
        };
    }
}
