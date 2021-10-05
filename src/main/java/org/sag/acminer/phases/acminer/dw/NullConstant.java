package org.sag.acminer.phases.acminer.dw;

import org.sag.acminer.database.defusegraph.id.Identifier;

public class NullConstant extends DataWrapper implements Constant {
	public static final String val = "NULL";
	public NullConstant() {
		super(Identifier.getPlaceholderConstantId(val));
	}
	private NullConstant(NullConstant p) {
		super(p.getIdentifier().clone());
	}
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof NullConstant))
			return false;
		return true;
	}
	public int hashCode() {
		return val.hashCode();
	}
	public String toString() {
		return val;
	}
	public NullConstant clone() {
		return new NullConstant(this);
	}
}
