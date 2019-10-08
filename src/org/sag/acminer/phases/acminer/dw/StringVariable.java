package org.sag.acminer.phases.acminer.dw;

import org.sag.acminer.database.defusegraph.id.Identifier;

public class StringVariable extends DataWrapper implements Variable {
	
	public StringVariable(Identifier val) {
		super(val);
	}
	
	private StringVariable(StringVariable p) {
		this(p.getIdentifier().clone());
	}
	
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof StringVariable))
			return false;
		return getIdentifier().equals(((StringVariable)o).getIdentifier());
	}
	
	public int hashCode() {
		return getIdentifier().hashCode();
	}
	
	public String toString() {
		return getIdentifier().toString();
	}
	
	public StringVariable clone() {
		return new StringVariable(this);
	}

}
