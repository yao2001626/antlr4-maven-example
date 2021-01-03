package org.antlr;

public class VariableSymbol extends BaseSymbol implements TypedSymbol{
	public VariableSymbol(String name) {
		super(name);
	}

	public VariableSymbol(String name, Type t) {
		super(name);
		super.setType(t);
	}

	@Override
	public void setType(Type type) {
		super.setType(type);
}
}
