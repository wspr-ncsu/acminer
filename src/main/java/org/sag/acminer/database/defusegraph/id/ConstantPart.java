package org.sag.acminer.database.defusegraph.id;

import soot.jimple.Constant;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("ConstantPart")
public class ConstantPart extends ValuePart {
	
	@XStreamOmitField
	private Constant c;
	
	public ConstantPart(Constant c, String curString) {
		super(c.toString(), curString);
		this.c = c;
	}
	
	private ConstantPart(ConstantPart p) {
		super(p.getOrgString(), p.getCurString());
		this.setIndex(p.getIndex());
		this.setValue(p.getValue());
	}
	
	public Constant getValue() { 
		return c; 
	}
	
	public void setValue(Constant c) {
		this.c = c;
	}
	
	//hashcode is the same as super already takes care of the value in both equals and hash code
	public boolean equals(Object o) {
		if(super.equals(o))
			return o instanceof ConstantPart;
		return false;
	}
	
	public ConstantPart clone() {
		return new ConstantPart(this);
	}
	
}