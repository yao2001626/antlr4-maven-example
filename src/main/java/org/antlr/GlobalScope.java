package org.antlr;

public class GlobalScope extends BaseScope {
	public GlobalScope(Scope scope) { super(scope); }
	public String getName() { return "global"; }
}
