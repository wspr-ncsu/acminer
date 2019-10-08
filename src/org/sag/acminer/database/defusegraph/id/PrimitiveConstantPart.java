package org.sag.acminer.database.defusegraph.id;

import java.util.Objects;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

@XStreamAlias("PrimitiveConstantPart")
public class PrimitiveConstantPart implements Part {
	
	protected static final int dtype = 7;
	protected static final int ftype = 6;
	protected static final int ltype = 5;
	protected static final int itype = 4;
	protected static final int ctype = 3;
	protected static final int stype = 2;
	protected static final int btype = 1;
	protected static final int booltype = 0;
	
	@XStreamAlias("Number")
	@XStreamAsAttribute
	private Number n;
	@XStreamAlias("CurString")
	@XStreamAsAttribute
	private String curString;
	@XStreamAlias("Type")
	@XStreamAsAttribute
	private int type;
	
	private static int getType(Number n, boolean isBool, boolean isChar) {
		if(isBool) {
			return booltype;
		} else if(isChar) {
			return ctype;
		}else {
			if(n instanceof Double)
				return dtype;
			else if(n instanceof Float)
				return ftype;
			else if(n instanceof Long)
				return ltype;
			else if(n instanceof Integer)
				return itype;
			else if(n instanceof Short)
				return stype;
			else if(n instanceof Byte)
				return btype;
		}
		throw new RuntimeException("Error: How did we get here");
	}
	
	public PrimitiveConstantPart(Number n, boolean isBool, boolean isChar) {
		this.n = n;
		this.type = getType(n, isBool, isChar);
		if(isBool) {
			this.curString = n.intValue() == 1 ? Boolean.TRUE.toString() : Boolean.FALSE.toString();
		} else if (isChar) {
			this.curString = Character.valueOf((char)n.intValue()).toString();
		} else {
			this.curString = n.toString();
		}
	}
	
	private PrimitiveConstantPart(PrimitiveConstantPart p) {
		this.n = p.n;
		this.curString = p.curString;
		this.type = p.type;
	}

	@Override
	public String getCurString() {
		return curString;
	}

	@Override
	public String getOrgString() {
		return n.toString();
	}

	@Override
	public void setCurString(String curString) {
		this.curString = curString;
	}
	
	public String toString() {
		return getCurString();
	}
	
	public int hashCode() {
		return n.hashCode();
	}
	
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof PrimitiveConstantPart))
			return false;
		return Objects.equals(toString(), o.toString());
	}
	
	public PrimitiveConstantPart clone() {
		return new PrimitiveConstantPart(this);
	}
	
	public Number getNumber() {
		return n;
	}

}
