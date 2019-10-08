package org.sag.acminer.phases.acminer.dw;

import org.sag.acminer.database.defusegraph.id.Identifier;

public abstract class Primitive extends DataWrapper {
	
	protected static final int dtype = 7;
	protected static final int ftype = 6;
	protected static final int ltype = 5;
	protected static final int itype = 4;
	protected static final int ctype = 3;
	protected static final int stype = 2;
	protected static final int btype = 1;
	protected static final int booltype = 0;
	
	protected static final int mulop = 0;
	protected static final int divop = 1;
	protected static final int modop = 2;
	protected static final int addop = 3;
	protected static final int subop = 4;
	protected static final int lshop = 5;
	protected static final int rshop = 6;
	protected static final int urshop = 7;
	protected static final int lteop = 8;
	protected static final int ltop = 9;
	protected static final int gteop = 10;
	protected static final int gtop = 11;
	protected static final int eqop = 12;
	protected static final int neqop = 13;
	protected static final int andop = 14;
	protected static final int xorop = 15;
	protected static final int orop = 16;
	protected static final int cmpop = 17;
	protected static final int cmplop = 18;
	protected static final int cmpgop = 19;
	
	protected final int type;
	
	public Primitive(Identifier id, int type) {
		super(id);
		this.type = type;
	}
	
	public abstract boolean isDouble();
	public abstract boolean isFloat();
	public abstract boolean isLong();
	public abstract boolean isInt();
	public abstract boolean isChar();
	public abstract boolean isShort();
	public abstract boolean isByte();
	public abstract boolean isBoolean();
	public abstract boolean isNegative();
	public abstract Primitive negate();
	public abstract Primitive getAsInt();
	public abstract Primitive getAsLong();
	public abstract Primitive getAsShort();
	public abstract Primitive getAsByte();
	public abstract Primitive getAsChar();
	public abstract Primitive getAsFloat();
	public abstract Primitive getAsDouble();
	public abstract Primitive getAsBoolean();
	
	public int getTypeInt() {
		return type;
	}
	
	public int getHighestTypeInt(Primitive p) {
		return this.getTypeInt() >= p.getTypeInt() ? this.getTypeInt() : p.getTypeInt();
	}

}
