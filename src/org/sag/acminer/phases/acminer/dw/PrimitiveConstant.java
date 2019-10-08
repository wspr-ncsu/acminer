package org.sag.acminer.phases.acminer.dw;

import org.sag.acminer.database.defusegraph.id.Identifier;

public abstract class PrimitiveConstant extends Primitive implements Constant {
	
	public PrimitiveConstant(Identifier id, int type) {
		super(id, type);
	}
	
	public abstract int getInt();
	public abstract long getLong();
	public abstract short getShort();
	public abstract byte getByte();
	public abstract char getChar();
	public abstract float getFloat();
	public abstract double getDouble();
	public abstract boolean getBoolean();
	public abstract boolean isNegative();
	public abstract boolean isZero();
	
	public IntConstant getAsInt() {
		if(isInt())
			return (IntConstant)this;
		return getIntConstant(getInt());
	}
	
	public LongConstant getAsLong() {
		if(isLong())
			return (LongConstant)this;
		return getLongConstant(getLong());
	}
	
	public ShortConstant getAsShort() {
		if(isShort())
			return (ShortConstant)this;
		return getShortConstant(getShort());
	}
	
	public ByteConstant getAsByte() {
		if(isByte())
			return (ByteConstant)this;
		return getByteConstant(getByte());
	}
	
	public CharConstant getAsChar() {
		if(isChar())
			return (CharConstant)this;
		return getCharConstant(getChar());
	}
	
	public FloatConstant getAsFloat() {
		if(isFloat())
			return (FloatConstant)this;
		return getFloatConstant(getFloat());
	}
	
	public DoubleConstant getAsDouble() {
		if(isDouble())
			return (DoubleConstant)this;
		return getDoubleConstant(getDouble());
	}
	
	public BooleanConstant getAsBoolean() {
		if(isBoolean())
			return (BooleanConstant)this;
		return getBooleanConstant(getBoolean());
	}
	
	public boolean isDouble() {
		return this instanceof DoubleConstant;
	}
	
	public boolean isFloat() {
		return this instanceof FloatConstant;
	}
	
	public boolean isLong() {
		return this instanceof LongConstant;
	}
	
	public boolean isInt() {
		return this instanceof IntConstant;
	}
	
	public boolean isChar() {
		return this instanceof CharConstant;
	}
	
	public boolean isShort() {
		return this instanceof ShortConstant;
	}
	
	public boolean isByte() {
		return this instanceof ByteConstant;
	}
	
	public boolean isBoolean() {
		return this instanceof BooleanConstant;
	}
	
	public PrimitiveConstant add(PrimitiveConstant second) {
		PrimitiveConstant upperOrder = this.getTypeInt() >= second.getTypeInt() ? this : second;
		if(upperOrder instanceof DoubleConstant)
			return getDoubleConstant(this.getDouble() + second.getDouble());
		else if(upperOrder instanceof FloatConstant)
			return getFloatConstant(this.getFloat() + second.getFloat());
		else if(upperOrder instanceof LongConstant)
			return getLongConstant(this.getLong() + second.getLong());
		else if(upperOrder instanceof IntConstant || upperOrder instanceof CharConstant || upperOrder instanceof ShortConstant 
				|| upperOrder instanceof ByteConstant || upperOrder instanceof BooleanConstant)
			return getIntConstant(this.getInt() + second.getInt());
		else 
			throw new RuntimeException("Error: Unknown constant type " + upperOrder.getClass().toString());
	}
	
	public PrimitiveConstant subtract(PrimitiveConstant second) {
		PrimitiveConstant upperOrder = this.getTypeInt() >= second.getTypeInt() ? this : second;
		if(upperOrder instanceof DoubleConstant)
			return getDoubleConstant(this.getDouble() - second.getDouble());
		else if(upperOrder instanceof FloatConstant)
			return getFloatConstant(this.getFloat() - second.getFloat());
		else if(upperOrder instanceof LongConstant)
			return getLongConstant(this.getLong() - second.getLong());
		else if(upperOrder instanceof IntConstant || upperOrder instanceof CharConstant || upperOrder instanceof ShortConstant 
				|| upperOrder instanceof ByteConstant || upperOrder instanceof BooleanConstant)
			return getIntConstant(this.getInt() - second.getInt());
		else 
			throw new RuntimeException("Error: Unknown constant type " + upperOrder.getClass().toString());
	}
	
	public PrimitiveConstant multiply(PrimitiveConstant second) {
		PrimitiveConstant upperOrder = this.getTypeInt() >= second.getTypeInt() ? this : second;
		if(upperOrder instanceof DoubleConstant)
			return getDoubleConstant(this.getDouble() * second.getDouble());
		else if(upperOrder instanceof FloatConstant)
			return getFloatConstant(this.getFloat() * second.getFloat());
		else if(upperOrder instanceof LongConstant)
			return getLongConstant(this.getLong() * second.getLong());
		else if(upperOrder instanceof IntConstant || upperOrder instanceof CharConstant || upperOrder instanceof ShortConstant 
				|| upperOrder instanceof ByteConstant || upperOrder instanceof BooleanConstant)
			return getIntConstant(this.getInt() * second.getInt());
		else 
			throw new RuntimeException("Error: Unknown constant type " + upperOrder.getClass().toString());
	}
	
	public PrimitiveConstant divide(PrimitiveConstant second) {
		PrimitiveConstant upperOrder = this.getTypeInt() >= second.getTypeInt() ? this : second;
		if(upperOrder instanceof DoubleConstant)
			return getDoubleConstant(this.getDouble() / second.getDouble());
		else if(upperOrder instanceof FloatConstant)
			return getFloatConstant(this.getFloat() / second.getFloat());
		else if(upperOrder instanceof LongConstant)
			return getLongConstant(this.getLong() / second.getLong());
		else if(upperOrder instanceof IntConstant || upperOrder instanceof CharConstant || upperOrder instanceof ShortConstant 
				|| upperOrder instanceof ByteConstant || upperOrder instanceof BooleanConstant)
			return getIntConstant(this.getInt() / second.getInt());
		else 
			throw new RuntimeException("Error: Unknown constant type " + upperOrder.getClass().toString());
	}
	
	public PrimitiveConstant mod(PrimitiveConstant second) {
		PrimitiveConstant upperOrder = this.getTypeInt() >= second.getTypeInt() ? this : second;
		if(upperOrder instanceof DoubleConstant)
			return getDoubleConstant(this.getDouble() % second.getDouble());
		else if(upperOrder instanceof FloatConstant)
			return getFloatConstant(this.getFloat() % second.getFloat());
		else if(upperOrder instanceof LongConstant)
			return getLongConstant(this.getLong() % second.getLong());
		else if(upperOrder instanceof IntConstant || upperOrder instanceof CharConstant || upperOrder instanceof ShortConstant 
				|| upperOrder instanceof ByteConstant || upperOrder instanceof BooleanConstant)
			return getIntConstant(this.getInt() % second.getInt());
		else 
			throw new RuntimeException("Error: Unknown constant type " + upperOrder.getClass().toString());
	}
	
	public PrimitiveConstant and(PrimitiveConstant second) {
		PrimitiveConstant upperOrder = this.getTypeInt() >= second.getTypeInt() ? this : second;
		if(upperOrder instanceof DoubleConstant || upperOrder instanceof LongConstant)
			return getLongConstant(this.getLong() & second.getLong());
		else if(upperOrder instanceof IntConstant || upperOrder instanceof CharConstant || upperOrder instanceof ShortConstant 
				|| upperOrder instanceof ByteConstant || upperOrder instanceof BooleanConstant || upperOrder instanceof FloatConstant)
			return getIntConstant(this.getInt() & second.getInt());
		else 
			throw new RuntimeException("Error: Unknown constant type " + upperOrder.getClass().toString());
	}
	
	public PrimitiveConstant or(PrimitiveConstant second) {
		PrimitiveConstant upperOrder = this.getTypeInt() >= second.getTypeInt() ? this : second;
		if(upperOrder instanceof DoubleConstant || upperOrder instanceof LongConstant)
			return getLongConstant(this.getLong() | second.getLong());
		else if(upperOrder instanceof IntConstant || upperOrder instanceof CharConstant || upperOrder instanceof ShortConstant 
				|| upperOrder instanceof ByteConstant || upperOrder instanceof BooleanConstant || upperOrder instanceof FloatConstant)
			return getIntConstant(this.getInt() | second.getInt());
		else 
			throw new RuntimeException("Error: Unknown constant type " + upperOrder.getClass().toString());
	}
	
	public PrimitiveConstant xor(PrimitiveConstant second) {
		PrimitiveConstant upperOrder = this.getTypeInt() >= second.getTypeInt() ? this : second;
		if(upperOrder instanceof DoubleConstant || upperOrder instanceof LongConstant)
			return getLongConstant(this.getLong() ^ second.getLong());
		else if(upperOrder instanceof IntConstant || upperOrder instanceof CharConstant || upperOrder instanceof ShortConstant 
				|| upperOrder instanceof ByteConstant || upperOrder instanceof BooleanConstant || upperOrder instanceof FloatConstant)
			return getIntConstant(this.getInt() ^ second.getInt());
		else 
			throw new RuntimeException("Error: Unknown constant type " + upperOrder.getClass().toString());
	}
	
	public PrimitiveConstant leftShift(PrimitiveConstant second) {
		PrimitiveConstant upperOrder = this.getTypeInt() >= second.getTypeInt() ? this : second;
		if(upperOrder instanceof DoubleConstant || upperOrder instanceof LongConstant)
			return getLongConstant(this.getLong() << second.getLong());
		else if(upperOrder instanceof IntConstant || upperOrder instanceof CharConstant || upperOrder instanceof ShortConstant 
				|| upperOrder instanceof ByteConstant || upperOrder instanceof BooleanConstant || upperOrder instanceof FloatConstant)
			return getIntConstant(this.getInt() << second.getInt());
		else 
			throw new RuntimeException("Error: Unknown constant type " + upperOrder.getClass().toString());
	}
	
	public PrimitiveConstant rightShift(PrimitiveConstant second) {
		PrimitiveConstant upperOrder = this.getTypeInt() >= second.getTypeInt() ? this : second;
		if(upperOrder instanceof DoubleConstant || upperOrder instanceof LongConstant)
			return getLongConstant(this.getLong() >> second.getLong());
		else if(upperOrder instanceof IntConstant || upperOrder instanceof CharConstant || upperOrder instanceof ShortConstant 
				|| upperOrder instanceof ByteConstant || upperOrder instanceof BooleanConstant || upperOrder instanceof FloatConstant)
			return getIntConstant(this.getInt() >> second.getInt());
		else 
			throw new RuntimeException("Error: Unknown constant type " + upperOrder.getClass().toString());
	}
	
	public PrimitiveConstant unsignedRightShift(PrimitiveConstant second) {
		PrimitiveConstant upperOrder = this.getTypeInt() >= second.getTypeInt() ? this : second;
		if(upperOrder instanceof DoubleConstant || upperOrder instanceof LongConstant)
			return getLongConstant(this.getLong() >>> second.getLong());
		else if(upperOrder instanceof IntConstant || upperOrder instanceof CharConstant || upperOrder instanceof ShortConstant 
				|| upperOrder instanceof ByteConstant || upperOrder instanceof BooleanConstant || upperOrder instanceof FloatConstant)
			return getIntConstant(this.getInt() >>> second.getInt());
		else 
			throw new RuntimeException("Error: Unknown constant type " + upperOrder.getClass().toString());
	}
	
	public PrimitiveConstant equalTo(PrimitiveConstant second) {
		PrimitiveConstant upperOrder = this.getTypeInt() >= second.getTypeInt() ? this : second;
		if(upperOrder instanceof DoubleConstant)
			return getBooleanConstant(this.getDouble() == second.getDouble());
		else if(upperOrder instanceof FloatConstant)
			return getBooleanConstant(this.getFloat() == second.getFloat());
		else if(upperOrder instanceof LongConstant)
			return getBooleanConstant(this.getLong() == second.getLong());
		else if(upperOrder instanceof IntConstant || upperOrder instanceof CharConstant || upperOrder instanceof ShortConstant 
				|| upperOrder instanceof ByteConstant || upperOrder instanceof BooleanConstant)
			return getBooleanConstant(this.getInt() == second.getInt());
		else 
			throw new RuntimeException("Error: Unknown constant type " + upperOrder.getClass().toString());
	}
	
	public PrimitiveConstant greaterThanOrEqualTo(PrimitiveConstant second) {
		PrimitiveConstant upperOrder = this.getTypeInt() >= second.getTypeInt() ? this : second;
		if(upperOrder instanceof DoubleConstant)
			return getBooleanConstant(this.getDouble() >= second.getDouble());
		else if(upperOrder instanceof FloatConstant)
			return getBooleanConstant(this.getFloat() >= second.getFloat());
		else if(upperOrder instanceof LongConstant)
			return getBooleanConstant(this.getLong() >= second.getLong());
		else if(upperOrder instanceof IntConstant || upperOrder instanceof CharConstant || upperOrder instanceof ShortConstant 
				|| upperOrder instanceof ByteConstant || upperOrder instanceof BooleanConstant)
			return getBooleanConstant(this.getInt() >= second.getInt());
		else 
			throw new RuntimeException("Error: Unknown constant type " + upperOrder.getClass().toString());
	}
	
	public PrimitiveConstant greaterThan(PrimitiveConstant second) {
		PrimitiveConstant upperOrder = this.getTypeInt() >= second.getTypeInt() ? this : second;
		if(upperOrder instanceof DoubleConstant)
			return getBooleanConstant(this.getDouble() > second.getDouble());
		else if(upperOrder instanceof FloatConstant)
			return getBooleanConstant(this.getFloat() > second.getFloat());
		else if(upperOrder instanceof LongConstant)
			return getBooleanConstant(this.getLong() > second.getLong());
		else if(upperOrder instanceof IntConstant || upperOrder instanceof CharConstant || upperOrder instanceof ShortConstant 
				|| upperOrder instanceof ByteConstant || upperOrder instanceof BooleanConstant)
			return getBooleanConstant(this.getInt() > second.getInt());
		else 
			throw new RuntimeException("Error: Unknown constant type " + upperOrder.getClass().toString());
	}
	
	public PrimitiveConstant lessThanOrEqualTo(PrimitiveConstant second) {
		PrimitiveConstant upperOrder = this.getTypeInt() >= second.getTypeInt() ? this : second;
		if(upperOrder instanceof DoubleConstant)
			return getBooleanConstant(this.getDouble() <= second.getDouble());
		else if(upperOrder instanceof FloatConstant)
			return getBooleanConstant(this.getFloat() <= second.getFloat());
		else if(upperOrder instanceof LongConstant)
			return getBooleanConstant(this.getLong() <= second.getLong());
		else if(upperOrder instanceof IntConstant || upperOrder instanceof CharConstant || upperOrder instanceof ShortConstant 
				|| upperOrder instanceof ByteConstant || upperOrder instanceof BooleanConstant)
			return getBooleanConstant(this.getInt() <= second.getInt());
		else 
			throw new RuntimeException("Error: Unknown constant type " + upperOrder.getClass().toString());
	}
	
	public PrimitiveConstant lessThan(PrimitiveConstant second) {
		PrimitiveConstant upperOrder = this.getTypeInt() >= second.getTypeInt() ? this : second;
		if(upperOrder instanceof DoubleConstant)
			return getBooleanConstant(this.getDouble() < second.getDouble());
		else if(upperOrder instanceof FloatConstant)
			return getBooleanConstant(this.getFloat() < second.getFloat());
		else if(upperOrder instanceof LongConstant)
			return getBooleanConstant(this.getLong() < second.getLong());
		else if(upperOrder instanceof IntConstant || upperOrder instanceof CharConstant || upperOrder instanceof ShortConstant 
				|| upperOrder instanceof ByteConstant || upperOrder instanceof BooleanConstant)
			return getBooleanConstant(this.getInt() < second.getInt());
		else 
			throw new RuntimeException("Error: Unknown constant type " + upperOrder.getClass().toString());
	}
	
	public PrimitiveConstant notEqualTo(PrimitiveConstant second) {
		PrimitiveConstant upperOrder = this.getTypeInt() >= second.getTypeInt() ? this : second;
		if(upperOrder instanceof DoubleConstant)
			return getBooleanConstant(this.getDouble() != second.getDouble());
		else if(upperOrder instanceof FloatConstant)
			return getBooleanConstant(this.getFloat() != second.getFloat());
		else if(upperOrder instanceof LongConstant)
			return getBooleanConstant(this.getLong() != second.getLong());
		else if(upperOrder instanceof IntConstant || upperOrder instanceof CharConstant || upperOrder instanceof ShortConstant 
				|| upperOrder instanceof ByteConstant || upperOrder instanceof BooleanConstant)
			return getBooleanConstant(this.getInt() != second.getInt());
		else 
			throw new RuntimeException("Error: Unknown constant type " + upperOrder.getClass().toString());
	}
	
	public PrimitiveConstant compareTo(PrimitiveConstant second) {
		PrimitiveConstant upperOrder = this.getTypeInt() >= second.getTypeInt() ? this : second;
		if(upperOrder instanceof DoubleConstant) {
			double op1 = this.getDouble();
			double op2 = second.getDouble();
			return getIntConstant(op1 == op2 ? 0 : (op1 > op2 ? 1 : -1));
		} else if(upperOrder instanceof FloatConstant) {
			float op1 = this.getFloat();
			float op2 = second.getFloat();
			return getIntConstant(op1 == op2 ? 0 : (op1 > op2 ? 1 : -1));
		} else if(upperOrder instanceof LongConstant) {
			long op1 = this.getLong();
			long op2 = second.getLong();
			return getIntConstant(op1 == op2 ? 0 : (op1 > op2 ? 1 : -1));
		} else if(upperOrder instanceof IntConstant || upperOrder instanceof CharConstant || upperOrder instanceof ShortConstant 
				|| upperOrder instanceof ByteConstant || upperOrder instanceof BooleanConstant) {
			int op1 = this.getInt();
			int op2 = second.getInt();
			return getIntConstant(op1 == op2 ? 0 : (op1 > op2 ? 1 : -1));
		} else {
			throw new RuntimeException("Error: Unknown constant type " + upperOrder.getClass().toString());
		}
	}
	
	public PrimitiveConstant compareToG(PrimitiveConstant second) {
		PrimitiveConstant upperOrder = this.getTypeInt() >= second.getTypeInt() ? this : second;
		if(upperOrder instanceof DoubleConstant) {
			double val1 = this.getDouble();
			double val2 = second.getDouble();
			return getIntConstant((val1 != val1 || val2 != val2 ? 1 : (val1 == val2 ? 0 : (val1 > val2 ? 1 : -1))));
		} else if(upperOrder instanceof FloatConstant) {
			float val1 = this.getFloat();
			float val2 = second.getFloat();
			return getIntConstant((val1 != val1 || val2 != val2 ? 1 : (val1 == val2 ? 0 : (val1 > val2 ? 1 : -1))));
		} else if(upperOrder instanceof LongConstant) {
			long val1 = this.getLong();
			long val2 = second.getLong();
			return getIntConstant(val1 == val2 ? 0 : (val1 > val2 ? 1 : -1));
		} else if(upperOrder instanceof IntConstant || upperOrder instanceof CharConstant || upperOrder instanceof ShortConstant 
				|| upperOrder instanceof ByteConstant || upperOrder instanceof BooleanConstant) {
			int val1 = this.getInt();
			int val2 = second.getInt();
			return getIntConstant(val1 == val2 ? 0 : (val1 > val2 ? 1 : -1));
		} else {
			throw new RuntimeException("Error: Unknown constant type " + upperOrder.getClass().toString());
		}
	}
	
	public PrimitiveConstant compareToL(PrimitiveConstant second) {
		PrimitiveConstant upperOrder = this.getTypeInt() >= second.getTypeInt() ? this : second;
		if(upperOrder instanceof DoubleConstant) {
			double val1 = this.getDouble();
			double val2 = second.getDouble();
			return getIntConstant((val1 != val1 || val2 != val2 ? -1 : (val1 == val2 ? 0 : (val1 > val2 ? 1 : -1))));
		} else if(upperOrder instanceof FloatConstant) {
			float val1 = this.getFloat();
			float val2 = second.getFloat();
			return getIntConstant((val1 != val1 || val2 != val2 ? -1 : (val1 == val2 ? 0 : (val1 > val2 ? 1 : -1))));
		} else if(upperOrder instanceof LongConstant) {
			long val1 = this.getLong();
			long val2 = second.getLong();
			return getIntConstant(val1 == val2 ? 0 : (val1 > val2 ? 1 : -1));
		} else if(upperOrder instanceof IntConstant || upperOrder instanceof CharConstant || upperOrder instanceof ShortConstant 
				|| upperOrder instanceof ByteConstant || upperOrder instanceof BooleanConstant) {
			int val1 = this.getInt();
			int val2 = second.getInt();
			return getIntConstant(val1 == val2 ? 0 : (val1 > val2 ? 1 : -1));
		} else {
			throw new RuntimeException("Error: Unknown constant type " + upperOrder.getClass().toString());
		}
	}
	
	public PrimitiveConstant negate() {
		if(this instanceof DoubleConstant) {
			return getDoubleConstant(-this.getDouble());
		} else if(this instanceof FloatConstant) {
			return getFloatConstant(-this.getFloat());
		} else if(this instanceof LongConstant) {
			return getLongConstant(-this.getLong());
		} else if(this instanceof IntConstant || this instanceof CharConstant || this instanceof ShortConstant || this instanceof ByteConstant) {
			return getIntConstant(-this.getInt());
		} else if(this instanceof BooleanConstant) {
			return getBooleanConstant(!this.getBoolean());
		} else {
			throw new RuntimeException("Error: Unknown constant type " + getClass().toString());
		}
	}
	
	public static DoubleConstant getDoubleConstant(double f) {
		return new DoubleConstant(Identifier.getPrimitiveConstantId(f), f);
	}
	
	public static FloatConstant getFloatConstant(float f) {
		return new FloatConstant(Identifier.getPrimitiveConstantId(f), f);
	}
	
	public static IntConstant getIntConstant(int f) {
		return new IntConstant(Identifier.getPrimitiveConstantId(f), f);
	}
	
	public static LongConstant getLongConstant(long f) {
		return new LongConstant(Identifier.getPrimitiveConstantId(f), f);
	}
	
	public static ShortConstant getShortConstant(short f) {
		return new ShortConstant(Identifier.getPrimitiveConstantId(f), f);
	}
	
	public static ByteConstant getByteConstant(byte f) {
		return new ByteConstant(Identifier.getPrimitiveConstantId(f), f);
	}
	
	public static BooleanConstant getBooleanConstant(boolean f) {
		return new BooleanConstant(Identifier.getBooleanPrimitiveConstantId(f ? 1 : 0), f);
	}
	
	public static CharConstant getCharConstant(char c) {
		return new CharConstant(Identifier.getBooleanPrimitiveConstantId((int)c), c);
	}

}
