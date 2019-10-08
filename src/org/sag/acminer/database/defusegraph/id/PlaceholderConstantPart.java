package org.sag.acminer.database.defusegraph.id;

import java.util.Objects;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("PlaceholderConstantPart")
public class PlaceholderConstantPart implements Part {
	
	@XStreamAlias("CurString")
	private String curString;
	
	public PlaceholderConstantPart(String curString) {
		this.curString = curString;
	}
	
	private PlaceholderConstantPart(PlaceholderConstantPart p) {
		this(p.getCurString());
	}
	
	public String getCurString() {
		return curString;
	}
	
	public void setCurString(String curString) {
		this.curString = curString;
	}
	
	public String getOrgString() {
		return getCurString();
	}
	
	public String toString() {
		return getCurString();
	}
	
	public int hashCode() {
		return toString().hashCode();
	}
	
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof PlaceholderConstantPart))
			return false;
		return Objects.equals(toString(), o.toString());
	}
	
	public PlaceholderConstantPart clone() {
		return new PlaceholderConstantPart(this);
	}

}
