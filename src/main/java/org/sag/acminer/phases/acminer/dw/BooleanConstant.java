package org.sag.acminer.phases.acminer.dw;

import org.sag.acminer.database.defusegraph.id.Identifier;

public class BooleanConstant extends PrimitiveConstant {
	
	private final boolean val;
	
	public BooleanConstant(Identifier id, boolean val) {
		super(id, booltype);
		this.val = val;
	}
	
	private BooleanConstant(BooleanConstant p) {
		this(p.getIdentifier().clone(), p.val);
	}
	
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof BooleanConstant))
			return false;
		return val == ((BooleanConstant)o).val;
	}
	
	public int hashCode() {
		return getInt();
	}
	
	public BooleanConstant clone() {
		return new BooleanConstant(this);
	}
	
	public String toString() {
		return "" + val;
	}
	
	public int getInt() {
		return val ? 1 : 0;
	}
	
	public long getLong() {
		return (long)getInt();
	}
	
	public short getShort() {
		return (short)getInt();
	}
	
	public byte getByte() {
		return (byte)getInt();
	}
	
	public char getChar() {
		return (char)getInt();
	}
	
	public float getFloat() {
		return (float)getInt();
	}
	
	public double getDouble() {
		return (double)getInt();
	}
	
	public boolean getBoolean() {
		return val;
	}

	@Override
	public boolean isNegative() {
		return !val;
	}

	@Override
	public boolean isZero() {
		return !val;
	}

}
