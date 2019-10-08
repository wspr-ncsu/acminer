package org.sag.acminer.database.defusegraph.id;

import java.util.Objects;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("LiteralPart")
public class LiteralPart implements Part {
	
	@XStreamAlias("CurString")
	private String curString;
	@XStreamOmitField
	private StringBuilder sb;
	
	public LiteralPart() {
		this.curString = null;
		this.sb = null;
	}
	
	public LiteralPart(String curString) {
		this.curString = curString;
		this.sb = null;
	}
	
	private LiteralPart(LiteralPart p) {
		this(p.getCurString());
	}
	
	public String getCurString() {
		if(curString == null) {
			if(sb == null) {
				curString = "";
			} else {
				curString = sb.toString();
				sb = null;
			}
		}
		return curString;
	}
	
	public void setCurString(String curString) {
		this.curString = curString;
		this.sb = null;
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
		if(o == null || !(o instanceof LiteralPart))
			return false;
		return Objects.equals(toString(), o.toString());
	}
	
	public LiteralPart clone() {
		return new LiteralPart(this);
	}
	
	public StringBuilder getBuffer() {
		if(sb == null) {
			if(curString != null) {
				sb = new StringBuilder(curString);
				curString = null;
			} else {
				sb = new StringBuilder();
			}
		}
		return sb; 
	}
	
	public void setBuffer(StringBuilder sb) {
		curString = null;
		this.sb = sb; 
	}
}