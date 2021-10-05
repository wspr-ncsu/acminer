package org.sag.acminer.database.defusegraph.id;

import java.util.Objects;

import soot.SootFieldRef;
import soot.jimple.FieldRef;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("FieldRefPart")
public class FieldRefPart extends ValuePart {
	
	@XStreamOmitField
	private FieldRef fr;
	@XStreamOmitField
	private SootFieldRef t;
	
	public FieldRefPart(SootFieldRef t, String curString) {
		super(t.getSignature(), curString);
		this.t = t;
		this.fr = null;
	}
	
	private FieldRefPart(FieldRefPart p) {
		super(p.getOrgString(), p.getCurString());
		this.setIndex(p.getIndex());
		this.setFieldRef(p.getFieldRef());
		this.setValue(p.getValue());
	}
	
	public SootFieldRef getFieldRef() { 
		return t; 
	}
	
	public void setFieldRef(SootFieldRef t) {
		this.t = t;
	}
	
	public FieldRef getValue() {
		return fr;
	}
	
	public void setValue(FieldRef fr) {
		this.fr = fr;
	}
	
	public int hashCode() {
		return super.hashCode() * 31 + Objects.hashCode(t);
	}
	
	public boolean equals(Object o) {
		if(super.equals(o))
			return o instanceof FieldRefPart && Objects.equals(t,((FieldRefPart)o).t);
		return false;
	}
	
	public FieldRefPart clone() {
		return new FieldRefPart(this);
	}
	
}