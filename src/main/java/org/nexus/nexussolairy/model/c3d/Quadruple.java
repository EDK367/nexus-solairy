package org.nexus.nexussolairy.model.c3d;

public class Quadruple {
    // clase para los quadruples del codigo intermedio
    private OpCode op;
    private String arg1;
    private String arg2;
    private String result;
    private int line;
    private String comment;

    public Quadruple(OpCode op, String arg1, String arg2, String result, int line) {
        this(op, arg1, arg2, result, line, "");
    }

    public Quadruple(OpCode op, String arg1, String arg2, String result, int line, String comment) {
        this.op = op;
        this.arg1 = arg1 != null ? arg1 : "";
        this.arg2 = arg2 != null ? arg2 : "";
        this.result = result != null ? result : "";
        this.line = line;
        this.comment = comment != null ? comment : "";
    }

    public OpCode getOp() {
        return op;
    }

    public void setOp(OpCode op) {
        this.op = op;
    }

    public String getArg1() {
        return arg1;
    }

    public void setArg1(String arg1) {
        this.arg1 = arg1;
    }

    public String getArg2() {
        return arg2;
    }

    public void setArg2(String arg2) {
        this.arg2 = arg2;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (op == OpCode.LABEL) {
            sb.append(result).append(":");
        } else if (op == OpCode.GOTO) {
            sb.append("goto ").append(result);
        } else if (op == OpCode.IF_TRUE) {
            sb.append("if (").append(arg1).append(") goto ").append(result);
        } else if (op == OpCode.IF_FALSE) {
            sb.append("ifFalse (").append(arg1).append(") goto ").append(result);
        } else if (op == OpCode.PARAM) {
            sb.append("param ").append(arg1);
        } else if (op == OpCode.CALL) {
            sb.append(result.isEmpty() ? "" : result + " = ").append("call ").append(arg1).append(", ").append(arg2);
        } else if (op == OpCode.RETURN) {
            sb.append("return ").append(arg1);
        } else if (op == OpCode.ASSIGN) {
            sb.append(result).append(" = ").append(arg1);
        } else if (op == OpCode.STACK_WRITE) {
            sb.append("stack[(int)").append(arg1).append("] = ").append(arg2);
        } else if (op == OpCode.STACK_READ) {
            sb.append(result).append(" = stack[(int)").append(arg1).append("]");
        } else if (op == OpCode.HEAP_WRITE) {
            sb.append("heap[(int)").append(arg1).append("] = ").append(arg2);
        } else if (op == OpCode.HEAP_READ) {
            sb.append(result).append(" = heap[(int)").append(arg1).append("]");
        } else if (op == OpCode.PRINT) {
            sb.append("print ").append(arg1);
        } else if (op == OpCode.PRINT_STR) {
            sb.append("print_str ").append(arg1);
        } else if (op == OpCode.READ) {
            sb.append(result).append(" = read()");
        } else if (op == OpCode.READ_STR) {
            sb.append(result).append(" = read_str()");
        } else {
            sb.append(result).append(" = ").append(arg1).append(" ").append(op.getSymbol()).append(" ").append(arg2);
        }
        if (!comment.isEmpty()) {
            sb.append(" // ").append(comment);
        }
        return sb.toString();
    }
}
