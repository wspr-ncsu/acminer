package org.sag.acminer.phases.acminer.dw;

import org.sag.acminer.database.defusegraph.id.Identifier;

public class IntConstant extends PrimitiveConstant {
	
	private final int val;
	
	public IntConstant(Identifier id, int val) {
		super(id, itype);
		this.val = val;
	}
	
	private IntConstant(IntConstant p) {
		this(p.getIdentifier().clone(), p.val);
	}
	
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof IntConstant))
			return false;
		return val == ((IntConstant)o).val;
	}
	
	public int hashCode() {
		return val;
	}
	
	public IntConstant clone() {
		return new IntConstant(this);
	}
	
	public String toString() {
		return "" + val;
	}
	
	public int getInt() {
		return val;
	}
	
	public long getLong() {
		return (long)val;
	}
	
	public short getShort() {
		return (short)val;
	}
	
	public byte getByte() {
		return (byte)val;
	}
	
	public char getChar() {
		return (char)val;
	}
	
	public float getFloat() {
		return (float)val;
	}
	
	public double getDouble() {
		return (double)val;
	}
	
	public boolean getBoolean() {
		if(val == 1)
			return true;
		else if(val == 0)
			return false;
		else
			throw new RuntimeException("Error: Tried to convert a num not equal to 0 or 1 to a boolean.");
	}
	
	@Override
	public boolean isNegative() {
		return val < 0;
	}
	
	@Override
	public boolean isZero() {
		return val == 0;
	}

}
