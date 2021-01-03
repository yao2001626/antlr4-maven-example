package org.antlr;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.antlr.LimeParser.ActionDeclContext;
import org.antlr.LimeParser.ArrayDeclContext;
import org.antlr.LimeParser.ClassDeclContext;
import org.antlr.LimeParser.EnumDeclContext;
import org.antlr.LimeParser.FieldDeclContext;
import org.antlr.LimeParser.GuardandexprContext;
import org.antlr.LimeParser.GuardatomidContext;
import org.antlr.LimeParser.GuardatomintContext;
import org.antlr.LimeParser.GuardatomnotContext;
import org.antlr.LimeParser.GuardcompexprContext;
import org.antlr.LimeParser.GuardorexprContext;
import org.antlr.LimeParser.GuardparenContext;
import org.antlr.LimeParser.Id_eleContext;
import org.antlr.LimeParser.InitDeclContext;
import org.antlr.LimeParser.LocalDeclContext;
import org.antlr.LimeParser.MethodDeclContext;
import org.antlr.LimeParser.NewcallContext;
import org.antlr.LimeParser.ParsdefContext;
import org.antlr.LimeParser.TypeContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

public class LimeParserTreeListener extends LimeBaseListener {
    Scope currentScope;
    SymbolTable symtab;
    Stack<String> sk;
    List<String> guardAsm;

    LimeParserTreeListener(SymbolTable symtab) {
        this.symtab = symtab;
        this.currentScope = symtab.GLOBALS;
        this.sk = new Stack<String>();
        this.guardAsm = new LinkedList<String>();
    }

    // importstmt
    // : 'import' ID'(' type_list ')' (':' type)? NEWLINE;

    // classDecl returns [Scope scope]
    // : 'class' ID NEWLINE INDENT classMember* DEDENT ;
    @Override
    public void enterClassDecl(ClassDeclContext ctx) {
        ClassSymbol cs = new ClassSymbol(ctx.ID().getText());
        cs.setDefNode((ParserRuleContext) ctx);
        ctx.scope = cs;
        currentScope.define(cs);
        currentScope = cs;
    }

    @Override
    public void exitClassDecl(ClassDeclContext ctx) {
        currentScope = currentScope.getEnclosingScope();
    }

    // methodDecl returns [Scope scope]
    // : ('private')? 'method' ID parameters (':' type)? (NEWLINE INDENT 'when'
    // guard 'do')? block (DEDENT)?;
    @Override
    public void enterMethodDecl(MethodDeclContext ctx) {
        MethodSymbol ms = new MethodSymbol(ctx.ID().getText());
        ms.setDefNode((ParserRuleContext) ctx);
        ctx.scope = ms;
        currentScope.define(ms);
        currentScope = ms;
    }

    @Override
    public void exitMethodDecl(MethodDeclContext ctx) {
        currentScope = currentScope.getEnclosingScope();
    }


    @Override
    public void enterInitDecl(InitDeclContext ctx) {
        MethodSymbol ms = new MethodSymbol("init");
        ms.setDefNode((ParserRuleContext) ctx);
        ctx.scope = ms;
        currentScope.define(ms);
        currentScope = ms;
    }

    @Override
    public void exitInitDecl(InitDeclContext ctx) {
        currentScope = currentScope.getEnclosingScope();
    }

    // actionDecl returns [Scope scope]
    // : 'action' ID (NEWLINE INDENT 'when' guard 'do')? block (DEDENT)? ;
    @Override
    public void enterActionDecl(ActionDeclContext ctx) {
        ActionSymbol as = new ActionSymbol(ctx.ID().getText());
        as.setDefNode((ParserRuleContext) ctx);
        ctx.scope = as;
        currentScope.define(as);
        currentScope = as;
    }

    @Override
    public void exitActionDecl(ActionDeclContext ctx) {
        currentScope = currentScope.getEnclosingScope();
    }

    // type returns [Type typ]
    // : 'int' | 'bool' | 'void' | ID | arrayDecl | enumDecl;
    @Override
    public void exitType(TypeContext ctx) {
        Type typ = null;
        if (ctx.getText().equals("int")) {
            typ = (Type) symtab.PREDEFINED.resolve("int");
        } else if (ctx.getText().equals("bool")) {
            typ = (Type) symtab.PREDEFINED.resolve("bool");
        } else if (ctx.getText().equals("void")) {
            typ = (Type) symtab.PREDEFINED.resolve("void");
        } else if (ctx.ID() != null) {
            Symbol s = currentScope.resolve(ctx.ID().getText());
            if (s instanceof ClassSymbol) {
                typ = (Type) s;
            } else {
                System.err.printf("Error: Can't resolve the class type: %s\n", ctx.ID().getText());
            }
        } else if (ctx.arrayDecl() != null) {
            typ = (Type) ctx.arrayDecl().typ;
        } else if (ctx.enumDecl() != null) {
            typ = ctx.enumDecl().typ;
        } else {
            System.err.printf("Can't resolve the type %s\n", ctx.getText());
        }
        ctx.typ = typ;
    }

    // arrayDecl returns [Type typ]
    // :'array' 'of' ty=('int' | 'bool' | ID);
    @Override
    public void enterArrayDecl(ArrayDeclContext ctx) {
        Type arrtyp = null;
        if (ctx.ty != null) {
            if (ctx.ty.getText().equals("int")) {
                arrtyp = new ArrayType((Type) symtab.GLOBALS.resolve("int"));
            } else if (ctx.ty.getText().equals("bool")) {
                arrtyp = new ArrayType((Type) symtab.GLOBALS.resolve("bool"));
            } else {// array of Objects
                ClassSymbol cs = (ClassSymbol) symtab.GLOBALS.resolve(ctx.ID().getText());
                if (cs == null) {
                    System.err.printf("arrayDecl: type %s can't resolve\n", ctx.ID().getText());
                }
                arrtyp = new ArrayType((Type) cs);
            }
        } else {
            System.err.println("arrayDecl: type is null");
        }
        ctx.typ = arrtyp;
    }

    // enumDecl returns [Type typ]
    // : 'enum' '{' ID (',' ID)* '}';
    @Override
    public void enterEnumDecl(EnumDeclContext ctx) {
        ClassSymbol cs = (ClassSymbol) currentScope;
        EnumType et = new EnumType();
        LinkedHashSet<String> vals = new LinkedHashSet<String>();
        for (TerminalNode id : ctx.ID()) {
            cs.define(new EnumSymbol(id.getText()));
            vals.add(id.getText());
        }
        et.addAllValues(vals);
        ctx.typ = et;
    }

    // fieldDecl
    // : 'var' id_list ':' type NEWLINE ;
    @Override
    public void exitFieldDecl(FieldDeclContext ctx) {
        Type t = ctx.type().typ;
        List<Id_eleContext> idele = ctx.id_list().id_ele();
        if (currentScope instanceof ClassSymbol) {
            for (Id_eleContext x : idele) {
                FieldSymbol fs = new FieldSymbol(x.ID().getText());
                fs.setType(t);
                fs.setDefNode((ParserRuleContext) ctx);
                currentScope.define(fs);
                System.out.println("define fields: " + x.ID().getText() + " type: " + t.getName());
            }
        }
    }

    // localDecl
    // : 'var' id_list ':' type ;
    @Override
    public void exitLocalDecl(LocalDeclContext ctx) {
        Type t = ctx.type().typ;
        List<Id_eleContext> idele = ctx.id_list().id_ele();
        if (currentScope instanceof ActionSymbol || currentScope instanceof MethodSymbol) {
            for (Id_eleContext x : idele) {
                VariableSymbol vs = new VariableSymbol(x.ID().getText());
                vs.setType(t);
                vs.setDefNode((ParserRuleContext) ctx);
                currentScope.define(vs);
                System.out.println("define local vars: " + x.ID().getText() + " type: " + t.getName());
            }
        }
    }

    // parsdef
    // : id_list ':' type ;
    @Override
    public void exitParsdef(ParsdefContext ctx) {
        Type t = ctx.type().typ;
        List<Id_eleContext> idele = ctx.id_list().id_ele();
        if (currentScope instanceof MethodSymbol) {
            for (Id_eleContext x : idele) {
                VariableSymbol ps = new VariableSymbol(x.ID().getText(), t);
                ps.setType(t);
                ps.setDefNode((ParserRuleContext) ctx);
                currentScope.define(ps);
                //System.out.println("define args: " + x.ID().getText() + ctx.type().getText());
                System.out.println("define args: " + x.ID().getText() + " type: " + t.getName());
            }
        }
    }

    // atom
    // : INTEGER | True | False | Null | ID
    // | method_call | arrayCreate | arrayElement ;

    // method_call
    // : 'new' n=ID args #newcall
    @Override
    public void enterNewcall(NewcallContext ctx) {
        Symbol s = currentScope.resolve(ctx.n.getText());
        if (!(s instanceof ClassSymbol)) {
            System.err.printf("Error: new ID args: ID  (%s) should be class symbol!\n", ctx.n.getText());
        }
    }

    // guard
    // : '(' guard ')' #guardparen
    // | 'not' guard #guardatomnot
    // | guard op=( '>=' | '<=' | '>' | '<' | '=' | '!=') guard #guardcompexpr
    // | guard 'and' guard #guardandexpr
    // | guard 'or' guard #guardorexpr
    // | ID #guardatomid
    // | INTEGER #guardatomint
    // ;

    // a stack machine
    /*
     * @Override public void exitGuardcompexpr(GuardcompexprContext ctx) {
     * if(sk.size()< 2){ System.err.printf("Error: guard evl\n"); } String rvalue =
     * sk.pop(); String lvalue = sk.pop(); String op = ctx.op.getText(); switch (op)
     * { case ">=": guardAsm.add("JGE " + lvalue + rvalue); //+= "JGE " +
     * "success\n"; sk.push("reg"); break; case "<=": guardAsm.add("JLE " +
     * "success\n"); sk.push("reg"); break; case ">": guardAsm.add("JG " +
     * "success\n"); sk.push("reg"); break; case "<": guardAsm.add("JL " +
     * "success\n"); sk.push("reg"); break; case "=": guardAsm.add("JE " +
     * "success\n"); sk.push("reg"); break; case "!=": guardAsm.add("JNE " +
     * "success\n"); sk.push("reg"); break; default:
     * System.err.printf("Error: unsupported operator %s for boolean expression\n",
     * op); break; } }
     * 
     * @Override public void exitGuardatomnot(GuardatomnotContext ctx) {
     * if(sk.size()< 1){ System.err.printf("Error: guard evl\n"); } String value =
     * sk.pop(); guardAsm.add("Not " + "success\n"); sk.push("reg"); }
     * 
     * @Override public void exitGuardandexpr(GuardandexprContext ctx) {
     * if(sk.size()< 2){ System.err.printf("Error: guard evl\n"); } String rvalue =
     * sk.pop(); String lvalue = sk.pop(); guardAsm.add("And " + "success\n");
     * sk.push("reg"); }
     * 
     * @Override public void exitGuardorexpr(GuardorexprContext ctx) { if(sk.size()<
     * 2){ System.err.printf("Error: guard evl\n"); } String rvalue = sk.pop();
     * String lvalue = sk.pop(); guardAsm.add("Or " + "success\n"); sk.push("reg");
     * }
     * 
     * @Override public void enterGuardatomid(GuardatomidContext ctx) {
     * sk.push("id_" + ctx.getText()); }
     * 
     * @Override public void enterGuardatomint(GuardatomintContext ctx) {
     * sk.push("num_" + ctx.getText()); }
     */

}
