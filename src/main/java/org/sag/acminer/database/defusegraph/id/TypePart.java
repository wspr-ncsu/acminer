package org.sag.acminer.database.defusegraph.id;

import java.util.Objects;

import soot.Type;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("TypePart")
public class TypePart implements Part {
	
	@XStreamOmitField
	private Type t;
	@XStreamAlias("OrgString")
	private final String orgString;
	@XStreamAlias("CurString")
	private String curString;
	
	public TypePart(Type t, String curString) {
		this.t = t;
		this.curString = curString;
		this.orgString = t.toString();
	}
	
	private TypePart(TypePart p) {
		this.t = p.t;
		this.orgString = p.orgString;
		this.curString = p.curString;
	}
	
	public Type getType() { 
		return t; 
	}
	
	public void setType(Type t) {
		this.t = t;
	}
	
	public String toString() { 
		return curString; 
	}
	
	public int hashCode() {
		int i = 17;
		i = i * 31 + Objects.hashCode(t);
		i = i * 31 + Objects.hashCode(orgString);
		i = i * 31 + Objects.hashCode(curString);
		return i;
	}
	
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof TypePart))
			return false;
		TypePart vp = (TypePart)o;
		return Objects.equals(t, vp.t) && Objects.equals(curString, vp.curString) && Objects.equals(orgString, vp.orgString);
	}
	
	public TypePart clone() {
		return new TypePart(this);
	}
	
	public String getCurString() { 
		return curString; 
	}
	
	public String getOrgString() { 
		return orgString; 
	}
	
	public void setCurString(String curString) { 
		this.curString = curString; 
	}
}