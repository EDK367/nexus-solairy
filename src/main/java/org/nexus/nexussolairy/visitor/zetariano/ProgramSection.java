package org.nexus.nexussolairy.visitor.zetariano;

import org.nexus.nexussolairy.ZetarianoParser;
import org.nexus.nexussolairy.model.enums.DataType;
import org.nexus.nexussolairy.model.enums.LanguageType;
import org.nexus.nexussolairy.model.enums.TypeErrorSemantic;
import org.nexus.nexussolairy.model.semantic.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProgramSection {
    private final ZetarianoVisitorImpl visitor;
    private boolean registered = false;

    public ProgramSection(ZetarianoVisitorImpl visitor) {
        this.visitor = visitor;
    }

    public DataType visitProgram(ZetarianoParser.ProgramContext ctx) {
        if (ctx == null) return DataType.VOID;
        if (!registered) {
            registered = true;
            for (ZetarianoParser.ClassDeclContext c : ctx.classDecl()) {
                registerClass(c);
            }
            for (ZetarianoParser.ClassDeclContext c : ctx.classDecl()) {
                registerMembers(c);
            }
        }
        for (ZetarianoParser.ClassDeclContext c : ctx.classDecl()) {
            visitClassDecl(c);
        }
        return DataType.VOID;
    }

    public void registerClass(ZetarianoParser.ClassDeclContext ctx) {
        if (ctx == null) return;
        String name = ctx.ID().getText();
        if (visitor.getSymbolTable().getGlobalScope().resolve(name) != null || visitor.getSymbolTable().lookupClass(name) != null) {
            visitor.reportError(ctx, TypeErrorSemantic.REDECLARACION, "Clase '" + name + "' ya definida");
            return;
        }
        ClassSymbol cls = new ClassSymbol(name, visitor.getSymbolTable().getGlobalScope(), ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
        visitor.getSymbolTable().getGlobalScope().define(cls);
        visitor.getSymbolTable().registerClass(cls);
    }

    public void registerMembers(ZetarianoParser.ClassDeclContext ctx) {
        if (ctx == null) return;
        String name = ctx.ID().getText();
        Symbol s = visitor.getSymbolTable().getGlobalScope().resolve(name);
        if (!(s instanceof ClassSymbol cls)) return;

        ZetarianoParser.ClassBodyContext body = ctx.classBody();
        if (body != null) {
            for (ZetarianoParser.FieldDeclContext f : body.fieldDecl()) {
                registerField(cls, f);
            }
            for (ZetarianoParser.ConstructorDeclContext c : body.constructorDecl()) {
                registerConstructor(cls, c);
            }
            for (ZetarianoParser.MethodDeclContext m : body.methodDecl()) {
                registerMethod(cls, m);
            }
        }
    }

    private void registerField(ClassSymbol cls, ZetarianoParser.FieldDeclContext ctx) {
        Type t = visitor.getVariableDelegate().getType(ctx.type());
        String name = ctx.ID().getText();
        if (cls.hasField(name)) {
            visitor.reportError(ctx, TypeErrorSemantic.REDECLARACION, "Campo '" + name + "' ya declarado en la clase '" + cls.getName() + "'");
            return;
        }
        Object value = null;
        if (ctx.expression() != null) {
            value = visitor.getExpressionEval().evalExpression(ctx.expression());
        } else {
            value = visitor.getExpressionEval().getDefaultValue(t);
        }
        VariableSymbol vs = new VariableSymbol(name, t, value, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
        if (ctx.LBRACK() != null && !ctx.LBRACK().isEmpty()) {
            vs.setArray(true);
        }
        cls.define(vs);
    }

    private void registerConstructor(ClassSymbol cls, ZetarianoParser.ConstructorDeclContext ctx) {
        String ctorName = ctx.ID().getText();
        if (!ctorName.equals(cls.getName())) {
            visitor.reportError(ctx, TypeErrorSemantic.DECLARATION_ERROR, "El constructor '" + ctorName + "' debe tener el mismo nombre que la clase '" + cls.getName() + "'");
            return;
        }
        FunctionSymbol fs = new FunctionSymbol(cls.getName(), new Type(cls.getName()), LanguageType.ZETARIANO, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
        if (ctx.paramList() != null) {
            for (ZetarianoParser.ParamContext p : ctx.paramList().param()) {
                fs.addParam(visitor.getVariableDelegate().processParam(p));
            }
        }
        fs.setAstContext(ctx);
        cls.addConstructor(fs);
    }

    private void registerMethod(ClassSymbol cls, ZetarianoParser.MethodDeclContext ctx) {
        Type ret = (ctx.VOID() != null) ? Type.VOID : visitor.getVariableDelegate().getType(ctx.type());
        String name = ctx.ID().getText();
        FunctionSymbol fs = new FunctionSymbol(name, ret, LanguageType.ZETARIANO, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
        if (ctx.paramList() != null) {
            for (ZetarianoParser.ParamContext p : ctx.paramList().param()) {
                fs.addParam(visitor.getVariableDelegate().processParam(p));
            }
        }
        fs.setAstContext(ctx);
        cls.define(fs);
    }

    public DataType visitClassDecl(ZetarianoParser.ClassDeclContext ctx) {
        if (ctx == null) return DataType.VOID;
        String name = ctx.ID().getText();
        Symbol s = visitor.getSymbolTable().getGlobalScope().resolve(name);
        ClassSymbol cls = (s instanceof ClassSymbol cs) ? cs : visitor.getSymbolTable().lookupClass(name);
        visitor.setCurrentClass(cls);

        String fileName = visitor.getFileName();
        if (fileName != null && !fileName.isEmpty()) {
            String baseName = fileName;
            int lastSlash = Math.max(baseName.lastIndexOf('/'), baseName.lastIndexOf('\\'));
            if (lastSlash >= 0) {
                baseName = baseName.substring(lastSlash + 1);
            }
            if (baseName.endsWith(".z")) {
                baseName = baseName.substring(0, baseName.length() - 2);
            }
            if (!baseName.equals(name)) {
                visitor.reportError(ctx, TypeErrorSemantic.DECLARATION_ERROR, "El nombre de la clase '" + name + "' debe coincidir con el nombre del archivo '" + baseName + "'");
            }
        }

        visitor.pushScope(name);
        if (cls != null) {
            for (Symbol fieldSym : cls.getFields().values()) {
                VariableSymbol copy = new VariableSymbol(fieldSym.getName(), fieldSym.getSemanticType(), fieldSym.value, fieldSym.getLine(), fieldSym.getColumn());
                if (fieldSym instanceof VariableSymbol vs && vs.isArray()) {
                    copy.setArray(true);
                }
                visitor.getSymbolTable().getCurrentScope().define(copy);
            }
        }

        ZetarianoParser.ClassBodyContext body = ctx.classBody();
        if (body != null) {
            visitClassBody(body);
        }

        visitor.popScope();
        visitor.setCurrentClass(null);
        return DataType.VOID;
    }

    public DataType visitClassBody(ZetarianoParser.ClassBodyContext body) {
        if (body == null) return DataType.VOID;
        for (ZetarianoParser.FieldDeclContext f : body.fieldDecl()) {
            visitFieldDecl(f);
        }
        for (ZetarianoParser.ConstructorDeclContext c : body.constructorDecl()) {
            visitConstructorDecl(c);
        }
        for (ZetarianoParser.MethodDeclContext m : body.methodDecl()) {
            visitMethodDecl(m);
        }
        return DataType.VOID;
    }

    public DataType visitFieldDecl(ZetarianoParser.FieldDeclContext ctx) {
        if (ctx == null) return DataType.VOID;
        Type t = visitor.getVariableDelegate().getType(ctx.type());
        String name = ctx.ID().getText();
        if (ctx.expression() != null) {
            Type init = visitor.getExpressionDelegate().visitExpression(ctx.expression());
            if (!TypeChecker.isAssignable(t, init)) {
                visitor.reportError(ctx, TypeErrorSemantic.INCOMPATIBLE_TYPES, "Inicializacion invalida");
            }
            Object value = visitor.getExpressionEval().evalExpression(ctx.expression());
            Symbol s = visitor.getSymbolTable().getCurrentScope().lookupLocal(name);
            if (s != null) {
                s.value = value;
            }
        }
        return DataType.VOID;
    }

    public DataType visitConstructorDecl(ZetarianoParser.ConstructorDeclContext ctx) {
        if (ctx == null) return DataType.VOID;
        String ctorName = ctx.ID().getText();
        ClassSymbol currentClass = visitor.getCurrentClass();
        if (currentClass != null && !ctorName.equals(currentClass.getName())) {
            visitor.reportError(ctx, TypeErrorSemantic.DECLARATION_ERROR, "El constructor '" + ctorName + "' debe tener el mismo nombre que la clase '" + currentClass.getName() + "'");
            return DataType.VOID;
        }
        visitor.setCurrentMethod(findConstructor(ctx));
        visitor.pushScope(ctorName + "_constructor");
        if (ctx.paramList() != null) {
            for (ZetarianoParser.ParamContext p : ctx.paramList().param()) {
                VariableSymbol vs = visitor.getVariableDelegate().processParam(p);
                visitor.getSymbolTable().getCurrentScope().define(vs);
            }
        }
        if (ctx.block() != null) {
            visitor.visitBlock(ctx.block());
        }
        visitor.popScope();
        visitor.setCurrentMethod(null);
        return DataType.VOID;
    }

    public DataType visitMethodDecl(ZetarianoParser.MethodDeclContext ctx) {
        if (ctx == null) return DataType.VOID;
        visitor.setCurrentMethod(findMethod(ctx));
        String name = ctx.ID().getText();
        boolean prevInsideFunction = visitor.isInsideFunction();
        boolean prevInsideMain = visitor.isInsideMain();
        visitor.setInsideFunction(true);
        if ("main".equalsIgnoreCase(name) || "principal".equalsIgnoreCase(name)) {
            visitor.setInsideMain(true);
        }

        visitor.pushScope(name);
        if (ctx.paramList() != null) {
            for (ZetarianoParser.ParamContext p : ctx.paramList().param()) {
                VariableSymbol vs = visitor.getVariableDelegate().processParam(p);
                visitor.getSymbolTable().getCurrentScope().define(vs);
            }
        }
        if (ctx.block() != null) {
            visitor.visitBlock(ctx.block());
        }
        visitor.popScope();
        visitor.setCurrentMethod(null);
        visitor.setInsideFunction(prevInsideFunction);
        visitor.setInsideMain(prevInsideMain);
        return DataType.VOID;
    }

    private FunctionSymbol findConstructor(ZetarianoParser.ConstructorDeclContext ctx) {
        ClassSymbol currentClass = visitor.getCurrentClass();
        if (currentClass == null) return null;
        int argCount = (ctx.paramList() != null) ? ctx.paramList().param().size() : 0;
        List<Type> paramTypes = new ArrayList<>();
        if (ctx.paramList() != null) {
            for (ZetarianoParser.ParamContext p : ctx.paramList().param()) {
                paramTypes.add(visitor.getVariableDelegate().getType(p.type()));
            }
        }
        for (Symbol s : currentClass.getConstructors()) {
            if (s instanceof FunctionSymbol c && c.getParams().size() == argCount) {
                boolean match = true;
                for (int i = 0; i < argCount; i++) {
                    if (!Objects.equals(c.getParams().get(i).getSemanticType(), paramTypes.get(i))) {
                        match = false;
                        break;
                    }
                }
                if (match) return c;
            }
        }
        for (Symbol s : currentClass.getConstructors()) {
            if (s instanceof FunctionSymbol c && c.getParams().size() == argCount) {
                return c;
            }
        }
        return null;
    }

    private FunctionSymbol findMethod(ZetarianoParser.MethodDeclContext ctx) {
        ClassSymbol currentClass = visitor.getCurrentClass();
        if (currentClass == null) return null;
        String name = ctx.ID().getText();
        int argCount = (ctx.paramList() != null) ? ctx.paramList().param().size() : 0;
        List<Type> paramTypes = new ArrayList<>();
        if (ctx.paramList() != null) {
            for (ZetarianoParser.ParamContext p : ctx.paramList().param()) {
                paramTypes.add(visitor.getVariableDelegate().getType(p.type()));
            }
        }
        List<Symbol> ms = currentClass.resolveMethod(name);
        if (ms != null) {
            for (Symbol s : ms) {
                if (s instanceof FunctionSymbol m && m.getParams().size() == argCount) {
                    boolean match = true;
                    for (int i = 0; i < argCount; i++) {
                        if (!Objects.equals(m.getParams().get(i).getSemanticType(), paramTypes.get(i))) {
                            match = false;
                            break;
                        }
                    }
                    if (match) return m;
                }
            }
            for (Symbol s : ms) {
                if (s instanceof FunctionSymbol m && m.getParams().size() == argCount) {
                    return m;
                }
            }
        }
        return null;
    }
}
