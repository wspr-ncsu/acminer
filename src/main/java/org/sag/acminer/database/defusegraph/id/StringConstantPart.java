package org.sag.acminer.database.defusegraph.id;

import java.util.Objects;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("StringConstantPart")
public class StringConstantPart implements Part {
	
	@XStreamAlias("CurString")
	private String curString;
	
	public StringConstantPart(String curString) {
		this.curString = curString;
	}
	
	private StringConstantPart(StringConstantPart p) {
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
		if(o == null || !(o instanceof StringConstantPart))
			return false;
		return Objects.equals(toString(), o.toString());
	}
	
	public StringConstantPart clone() {
		return new StringConstantPart(this);
	}

}
