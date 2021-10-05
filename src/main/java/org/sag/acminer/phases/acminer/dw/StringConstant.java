package org.sag.acminer.phases.acminer.dw;

import org.sag.acminer.database.defusegraph.id.Identifier;

public class StringConstant extends DataWrapper implements Constant {
	
	private final String val;
	
	public StringConstant(String val) {
		super(Identifier.getStringConstantId(val));
		this.val = val;
	}
	
	private StringConstant(StringConstant p) {
		super(p.getIdentifier().clone());
		this.val = p.val;
	}
	
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof StringConstant))
			return false;
		return val.equals(((StringConstant)o).val);
	}
	
	public int hashCode() {
		return val.hashCode();
	}
	
	public String toString() {
		return "\"" + val + "\"";
	}
	
	public StringConstant clone() {
		return new StringConstant(this);
	}

}
