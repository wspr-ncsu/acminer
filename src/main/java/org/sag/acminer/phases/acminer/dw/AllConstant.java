package org.sag.acminer.phases.acminer.dw;

import org.sag.acminer.database.defusegraph.id.Identifier;

public class AllConstant extends DataWrapper implements Constant {
	public static final String val = "ALL";
	public AllConstant() {
		super(Identifier.getPlaceholderConstantId(val));
	}
	private AllConstant(AllConstant p) {
		super(p.getIdentifier().clone());
	}
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof AllConstant))
			return false;
		return true;
	}
	public int hashCode() {
		return val.hashCode();
	}
	public String toString() {
		return val;
	}
	public AllConstant clone() {
		return new AllConstant(this);
	}
}
