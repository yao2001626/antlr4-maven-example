package org.antlr;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.io.File;

import org.antlr.LimeLexer;
import org.antlr.LimeParser;

public class Lime {
    // private static boolean makeDot = false;
    public static void main(String [] args) throws Exception
    {

        ANTLRInputStream antlrInputStream = new ANTLRInputStream("hello world");

        LimeLexer lexer = new LimeLexer(antlrInputStream);

        CommonTokenStream tokens = new CommonTokenStream( lexer );
        LimeParser parser = new LimeParser( tokens );
        ParseTree tree = parser.compilationUnit();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk( new LimeWalker(), tree );
    }
}