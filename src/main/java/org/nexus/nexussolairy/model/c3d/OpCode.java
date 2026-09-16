package org.nexus.nexussolairy.model.c3d;

// enum para terminales global
public enum OpCode {
    ADD("+"),
    SUB("-"),
    MULT("*"),
    DIV("/"),
    MOD("%"),
    ASSIGN("="),
    EQ("=="),
    NEQ("!="),
    LT("<"),
    LE("<="),
    GT(">"),
    GE(">="),
    AND("&&"),
    OR("||"),
    NOT("!"),
    GOTO("goto"),
    IF_TRUE("if"),
    IF_FALSE("ifFalse"),
    LABEL("label"),
    PARAM("param"),
    CALL("call"),
    RETURN("return"),
    STACK_WRITE("stack_write"),
    STACK_READ("stack_read"),
    HEAP_WRITE("heap_write"),
    HEAP_READ("heap_read"),
    PRINT("print"),
    READ("read"),
    PRINT_STR("print_str"),
    READ_STR("read_str"),
    CONCAT_STR("concat_str"),
    CMP_STR("cmp_str"),
    NOP("nop");

    private final String symbol;

    OpCode(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
