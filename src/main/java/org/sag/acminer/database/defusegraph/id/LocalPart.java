package org.sag.acminer.database.defusegraph.id;

import soot.Local;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("LocalPart")
public class LocalPart extends ValuePart {
	
	@XStreamOmitField
	private Local c;
	
	public LocalPart(Local c, String curString) {
		super(c.toString(), curString);
		this.c = c;
	}
	
	private LocalPart(LocalPart p) {
		super(p.getOrgString(), p.getCurString());
		this.setIndex(p.getIndex());
		this.setValue(p.getValue());
	}
	
	public Local getValue() { 
		return c; 
	}
	
	public void setValue(Local l) {
		this.c = l;
	}
	
	//hashcode is the same as super already takes care of the value in both equals and hash code
	public boolean equals(Object o) {
		if(super.equals(o))
			return o instanceof LocalPart;
		return false;
	}
	
	public LocalPart clone() {
		return new LocalPart(this);
	}
	
}