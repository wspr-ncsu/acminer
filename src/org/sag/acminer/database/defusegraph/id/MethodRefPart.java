package org.sag.acminer.database.defusegraph.id;

import java.util.Objects;

import soot.SootMethodRef;
import soot.jimple.InvokeExpr;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("MethodRefPart")
public class MethodRefPart extends ValuePart {
	
	@XStreamOmitField
	private InvokeExpr ie;
	@XStreamOmitField
	private SootMethodRef t;
	
	public MethodRefPart(SootMethodRef t, String curString) {
		super(t.getSignature(), curString);
		this.t = t;
		this.ie = null;
	}
	
	private MethodRefPart(MethodRefPart p) {
		super(p.getOrgString(), p.getCurString());
		this.setIndex(p.getIndex());
		this.setValue(p.getValue());
		this.setMethodRef(p.getMethodRef());
	}
	
	public SootMethodRef getMethodRef() { 
		return t;
	}
	
	public void setMethodRef(SootMethodRef t) {
		this.t = t;
	}
	
	public InvokeExpr getValue() {
		return ie;
	}
	
	public void setValue(InvokeExpr ie) {
		this.ie = ie;
	}
	
	public int hashCode() {
		return super.hashCode() * 31 + Objects.hashCode(t);
	}
	
	public boolean equals(Object o) {
		if(super.equals(o))
			return o instanceof MethodRefPart && Objects.equals(t,((MethodRefPart)o).t);
		return false;
	}
	
	public MethodRefPart clone() {
		return new MethodRefPart(this);
	}
	
}