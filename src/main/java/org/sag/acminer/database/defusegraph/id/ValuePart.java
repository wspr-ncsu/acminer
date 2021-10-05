package org.sag.acminer.database.defusegraph.id;

import soot.Value;

import java.util.Objects;
import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("ValuePart")
public abstract class ValuePart implements Part {
	
	@XStreamAlias("OrgString")
	private final String orgString;
	@XStreamAlias("CurString")
	private String curString;
	@XStreamAlias("Index")
	private int i;
	
	public ValuePart(String orgString, String curString) {
		this.orgString = orgString;
		this.curString = curString;
		this.i = -1;
	}
	
	public String getOrgString() { 
		return orgString; 
	}
	
	public String getCurString() { 
		return curString; 
	}
	
	public void setCurString(String curString) {
		this.curString = curString;
	}
	
	public int getIndex() { 
		return i; 
	}
	
	public void setIndex(int i) {
		this.i = i;
	}
	
	public String toString() { 
		return curString; 
	}
	
	public int hashCode() {
		int i = 17;
		i = i * 31 + Objects.hashCode(orgString);
		i = i * 31 + Objects.hashCode(curString);
		i = i * 31 + this.i;
		i = i * 31 + Objects.hashCode(getValue());
		return i;
	}
	
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof ValuePart))
			return false;
		ValuePart vp = (ValuePart)o;
		return Objects.equals(orgString, vp.orgString) && Objects.equals(curString, vp.curString) 
				&& i == vp.i && Objects.equals(getValue(), vp.getValue());
	}
	
	public abstract Value getValue();
	public abstract ValuePart clone();
	
}