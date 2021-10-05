package org.sag.acminer.phases.acminer.dw;

import org.sag.acminer.database.defusegraph.id.Identifier;

public class UnknownVariable extends DataWrapper implements Variable {
	
	public UnknownVariable(Identifier val) {
		super(val);
	}
	
	private UnknownVariable(UnknownVariable p) {
		this(p.getIdentifier().clone());
	}
	
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof UnknownVariable))
			return false;
		return getIdentifier().equals(((UnknownVariable)o).getIdentifier());
	}
	
	public int hashCode() {
		return getIdentifier().hashCode();
	}
	
	public String toString() {
		return getIdentifier().toString();
	}
	
	public UnknownVariable clone() {
		return new UnknownVariable(this);
	}

}
