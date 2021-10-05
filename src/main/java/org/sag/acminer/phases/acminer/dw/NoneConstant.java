package org.sag.acminer.phases.acminer.dw;

import org.sag.acminer.database.defusegraph.id.Identifier;

public class NoneConstant extends DataWrapper implements Constant {
	public static final String val = "NONE";
	public NoneConstant() {
		super(Identifier.getPlaceholderConstantId(val));
	}
	private NoneConstant(NoneConstant p) {
		super(p.getIdentifier().clone());
	}
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof NoneConstant))
			return false;
		return true;
	}
	public int hashCode() {
		return val.hashCode();
	}
	public String toString() {
		return val;
	}
	public NoneConstant clone() {
		return new NoneConstant(this);
	}
}
