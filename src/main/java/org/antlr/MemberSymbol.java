package org.antlr;

public interface MemberSymbol extends Symbol {
	int getSlotNumber();		// index giving order in the aggregate (from 0)
	
}
