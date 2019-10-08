package org.sag.acminer.phases.acminer.dw;

import org.sag.acminer.database.defusegraph.id.Identifier;

public class UnknownConstant extends DataWrapper implements Constant {
	
	/* We remove any soot identifying information because holding it does not
	 * give us any additional info and we want to make this in-line with all
	 * the other constants which remove such information. The only ones that
	 * might hold such information are the variables because method and field
	 * refs are still important.
	 */
	private final String val;
	
	public UnknownConstant(Identifier val) {
		super(Identifier.getUnknownConstantId(val.toString()));
		this.val = val.toString();
	}
	
	private UnknownConstant(UnknownConstant p) {
		this(p.getIdentifier().clone());
	}
	
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof UnknownConstant))
			return false;
		return val.equals(((UnknownConstant)o).val);
	}
	
	public int hashCode() {
		return val.hashCode();
	}
	
	public String toString() {
		return val;
	}
	
	public UnknownConstant clone() {
		return new UnknownConstant(this);
	}

}
