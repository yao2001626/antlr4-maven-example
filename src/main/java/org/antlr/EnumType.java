package org.antlr;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

public class EnumType implements Type {
	public String name;
	public Set<String> values;
	
	protected Type enumType;
	protected LinkedHashMap<String,Integer> table = new LinkedHashMap<String,Integer>();
	protected int index = -1; // index we have just written
	
	public EnumType(){
		name = "enum";
		values = new LinkedHashSet<String>();
	}
	
	public void addAllValues(LinkedHashSet<String> vs) {
		values.addAll(vs);
	}
	
	@Override
	public String getName() {
		return toString();
	}
	@Override
	public int getTypeIndex() {
		return -1;
	}
	@Override
	public String toString(){
		return "Enum: " + values.toString(); 
	}
}
