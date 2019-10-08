package org.sag.acminer.database.defusegraph.id;

import soot.jimple.IdentityRef;
import soot.jimple.ParameterRef;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("IdentityRefPart")
public class IdentityRefPart extends ValuePart {
	
	@XStreamOmitField
	private IdentityRef t;
	
	public IdentityRefPart(IdentityRef t, String curString) {
		super(t.toString(), curString);
		this.t = t;
	}
	
	private IdentityRefPart(IdentityRefPart p) {
		super(p.getOrgString(), p.getCurString());
		this.setIndex(p.getIndex());
		this.setValue(p.getValue());
	}
	
	public IdentityRef getValue() { 
		return t;
	}
	
	public void setValue(IdentityRef t) {
		this.t = t;
	}
	
	//hashcode is the same as super already takes care of the value in both equals and hash code
	public boolean equals(Object o) {
		if(super.equals(o))
			return o instanceof IdentityRefPart;
		return false;
	}
	
	public IdentityRefPart clone() {
		return new IdentityRefPart(this);
	}
	
	public boolean isParameterRef() {
		return t instanceof ParameterRef;
	}
	
	public ParameterRef getParameterRef() {
		if(isParameterRef())
			return (ParameterRef)t;
		return null;
	}
	
}