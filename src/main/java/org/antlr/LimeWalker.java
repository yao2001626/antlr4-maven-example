package org.antlr;

import org.antlr.LimeBaseListener;
import org.antlr.LimeParser;

public class LimeWalker extends LimeBaseListener {
    public void enterR(LimeParser.CompilationUnitContext ctx) {
        System.out.println("Entering R : ");
    }

    public void exitR(LimeParser.CompilationUnitContext ctx) {
        System.out.println("Exiting R");
    }
}