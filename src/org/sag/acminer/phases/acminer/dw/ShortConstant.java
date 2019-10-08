package org.sag.acminer.phases.acminer.dw;

import org.sag.acminer.database.defusegraph.id.Identifier;

public class ShortConstant extends PrimitiveConstant {
	
	private final short val;
	
	public ShortConstant(Identifier id, short val) {
		super(id, stype);
		this.val = val;
	}
	
	private ShortConstant(ShortConstant p) {
		this(p.getIdentifier().clone(), p.val);
	}
	
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof ShortConstant))
			return false;
		return val == ((ShortConstant)o).val;
	}
	
	public int hashCode() {
		return (int)val;
	}
	
	public ShortConstant clone() {
		return new ShortConstant(this);
	}
	
	public String toString() {
		return "" + val;
	}
	
	public int getInt() {
		return (int)val;
	}
	
	public long getLong() {
		return (long)val;
	}
	
	public short getShort() {
		return val;
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
		if((int)val == 1)
			return true;
		else if((int)val == 0)
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
