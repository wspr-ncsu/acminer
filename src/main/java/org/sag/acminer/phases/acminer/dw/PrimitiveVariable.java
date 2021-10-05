package org.sag.acminer.phases.acminer.dw;

import org.sag.acminer.database.defusegraph.id.Identifier;
import org.sag.acminer.database.defusegraph.id.LiteralPart;

public class PrimitiveVariable extends Primitive implements Variable {
	
	private final boolean isNegative;
	
	public PrimitiveVariable(Identifier val, int type, boolean isNegative) {
		super(constructId(val, isNegative), type);
		this.isNegative = isNegative;
	}
	
	private PrimitiveVariable(PrimitiveVariable p) {
		this(p.getIdentifier().clone(), p.getTypeInt(), p.isNegative);
	}
	
	private static Identifier constructId(Identifier val, boolean isNegative) {
		val = val.clone();
		if(isNegative) {
			if(val.size() > 1 && val.get(0) instanceof LiteralPart && val.get(0).toString().equals("(neg ")) {
				if(!(val.get(val.size() - 1) instanceof LiteralPart) || !val.get(val.size() - 1).toString().equals(")"))
					throw new RuntimeException("Error: Improperly formated neg primitive variable " + val.toString());
				val.remove(0);
				val.remove(val.size() - 1);
			} else {
				LiteralPart p = new LiteralPart();
				p.setCurString("(neg ");
				LiteralPart p2 = new LiteralPart();
				p2.setCurString(")");
				val.add(0, p);
				val.add(p2);
			}
		}
		return val;
	}
	
	@Override
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof PrimitiveVariable))
			return false;
		return getIdentifier().equals(((PrimitiveVariable)o).getIdentifier()) 
				&& type == ((PrimitiveVariable)o).type && isNegative == ((PrimitiveVariable)o).isNegative;
	}
	
	@Override
	public int hashCode() {
		return 31 * (17 * 31 + getIdentifier().hashCode()) + type + (isNegative ? 1 : 0);
	}
	
	public PrimitiveVariable clone() {
		return new PrimitiveVariable(this);
	}
	
	@Override
	public String toString() {
		return getIdentifier().toString();
	}
	
	public PrimitiveVariable getAsInt() {
		if(isInt())
			return this;
		return new PrimitiveVariable(getIdentifier(), itype, isNegative);
	}
	
	public PrimitiveVariable getAsLong() {
		if(isLong())
			return this;
		return new PrimitiveVariable(getIdentifier(), ltype, isNegative);
	}
	
	public PrimitiveVariable getAsShort() {
		if(isShort())
			return this;
		return new PrimitiveVariable(getIdentifier(), stype, isNegative);
	}
	
	public PrimitiveVariable getAsByte() {
		if(isByte())
			return this;
		return new PrimitiveVariable(getIdentifier(), btype, isNegative);
	}
	
	public PrimitiveVariable getAsChar() {
		if(isChar())
			return this;
		return new PrimitiveVariable(getIdentifier(), ctype, isNegative);
	}
	
	public PrimitiveVariable getAsFloat() {
		if(isFloat())
			return this;
		return new PrimitiveVariable(getIdentifier(), ftype, isNegative);
	}
	
	public PrimitiveVariable getAsDouble() {
		if(isDouble())
			return this;
		return new PrimitiveVariable(getIdentifier(), dtype, isNegative);
	}
	
	public PrimitiveVariable getAsBoolean() {
		if(isBoolean())
			return this;
		return new PrimitiveVariable(getIdentifier(), booltype, isNegative);
	}
	
	public static PrimitiveVariable getDoubleVariable(Identifier val) {
		return new PrimitiveVariable(val, dtype, false);
	}
	
	public static PrimitiveVariable getFloatVariable(Identifier val) {
		return new PrimitiveVariable(val, ftype, false);
	}
	
	public static PrimitiveVariable getLongVariable(Identifier val) {
		return new PrimitiveVariable(val, ltype, false);
	}
	
	public static PrimitiveVariable getIntVariable(Identifier val) {
		return new PrimitiveVariable(val, itype, false);
	}
	
	public static PrimitiveVariable getCharVariable(Identifier val) {
		return new PrimitiveVariable(val, ctype, false);
	}
	
	public static PrimitiveVariable getShortVariable(Identifier val) {
		return new PrimitiveVariable(val, stype, false);
	}
	
	public static PrimitiveVariable getByteVariable(Identifier val) {
		return new PrimitiveVariable(val, btype, false);
	}
	
	public static PrimitiveVariable getBooleanVariable(Identifier val) {
		return new PrimitiveVariable(val, booltype, false);
	}

	@Override
	public boolean isDouble() {
		return type == dtype;
	}

	@Override
	public boolean isFloat() {
		return type == ftype;
	}

	@Override
	public boolean isLong() {
		return type == ltype;
	}

	@Override
	public boolean isInt() {
		return type == itype;
	}

	@Override
	public boolean isChar() {
		return type == ctype;
	}

	@Override
	public boolean isShort() {
		return type == stype;
	}

	@Override
	public boolean isByte() {
		return type == btype;
	}

	@Override
	public boolean isBoolean() {
		return type == booltype;
	}
	
	public boolean isNegative() {
		return isNegative;
	}
	
	public PrimitiveVariable negate() {
		return new PrimitiveVariable(getIdentifier(), type, !isNegative);
	}

}
