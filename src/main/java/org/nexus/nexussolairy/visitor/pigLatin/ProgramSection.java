package org.nexus.nexussolairy.visitor.pigLatin;

import org.nexus.nexussolairy.PigLatinParser;
import org.nexus.nexussolairy.model.enums.DataType;
import org.nexus.nexussolairy.visitor.VisitorContext;

public class ProgramSection {
    private final VisitorContext visitor;

    public ProgramSection(VisitorContext visitor) {
        this.visitor = visitor;
    }

    public DataType visitProgram(PigLatinParser.ProgramContext ctx) {
        if (ctx == null) return DataType.VOID;
        if (ctx.importSection() != null) visitor.visit(ctx.importSection());
        if (ctx.varSection() != null) visitor.visit(ctx.varSection());
        if (ctx.mainSection() != null) visitor.visit(ctx.mainSection());
        return DataType.VOID;
    }

    public DataType visitImportSection(PigLatinParser.ImportSectionContext ctx) {
        if (ctx == null) return DataType.VOID;
        for (PigLatinParser.ImportStmtContext imp : ctx.importStmt()) {
            visitor.visit(imp);
        }
        return DataType.VOID;
    }

    public DataType visitImportStmt(PigLatinParser.ImportStmtContext ctx) {
        if (ctx == null) return DataType.VOID;
        if (ctx.path() != null) {
            visitor.visit(ctx.path());
        }
        return DataType.VOID;
    }

    public DataType visitPath(PigLatinParser.PathContext ctx) {
        return DataType.VOID;
    }

    public DataType visitMainSection(PigLatinParser.MainSectionContext ctx) {
        if (ctx == null) return DataType.VOID;
        visitor.setInsideMain(true);
        visitor.getSymbolTable().pushScope("main");
        for (PigLatinParser.StatementContext statement : ctx.statement()) {
            visitor.visit(statement);
            if (visitor.isShouldBreak() || visitor.isShouldReturn()) break;
        }
        visitor.getSymbolTable().popScope();
        visitor.setInsideMain(false);
        return DataType.VOID;
    }
}
