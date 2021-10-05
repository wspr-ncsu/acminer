package org.sag.acminer.phases.acminer.dw;

import org.sag.acminer.database.defusegraph.id.Identifier;
import org.sag.acminer.database.defusegraph.id.LiteralPart;

public class Expression extends Primitive {
	
	private final Primitive arg1;
	private final Primitive arg2;
	private final int op;
	private final boolean isNegative;
	
	public Expression(Primitive arg1, Primitive arg2, int op, int type, boolean isNegative) {
		super(constructId(arg1, arg2, op, isNegative), type);
		this.arg1 = arg1;
		this.arg2 = arg2;
		this.op = op;
		this.isNegative = isNegative;
	}
	
	private Expression(Expression e) {
		super(e.getIdentifier().clone(), e.type);
		this.arg1 = e.arg1;
		this.arg2 = e.arg2;
		this.op = e.op;
		this.isNegative = e.isNegative;
	}

	@Override
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof Expression))
			return false;
		Expression e = (Expression)o;
		return arg1.equals(e.arg1) && arg2.equals(e.arg2) && op == e.op && type == e.type && isNegative == e.isNegative;
	}

	@Override
	public int hashCode() {
		int i = 17;
		i = i * 31 + arg1.hashCode();
		i = i * 31 + arg2.hashCode();
		i = i * 31 + op;
		i = i * 31 + type;
		i = i * 31 + (isNegative ? 1 : 0);
		return i;
	}
	
	public Expression clone() {
		return new Expression(this);
	}
	
	private static Identifier constructId(Primitive arg1, Primitive arg2, int op, boolean isNegative) {
		Identifier id = Identifier.combineIds(arg1.getIdentifier(), arg2.getIdentifier(), getOpString(op));
		if(isNegative) {
			id.add(0, new LiteralPart("(neg "));
			id.add(new LiteralPart(")"));
		}
		return id;
	}
	
	private static String getOpString(int op) {
		String o;
		if(op == addop)
			o = " + ";
		else if(op == andop)
			o = " & ";
		else if(op == cmpop)
			o = " cmp ";
		else if(op == cmpgop)
			o = " cmpg ";
		else if(op == cmplop)
			o = " cmpl ";
		else if(op == eqop)
			o = " == ";
		else if(op == gteop)
			o = " >= ";
		else if(op == gtop)
			o = " > ";
		else if(op == lteop)
			o = " <= ";
		else if(op == ltop)
			o = " < ";
		else if(op == neqop)
			o = " != ";
		else if(op == divop)
			o = " / ";
		else if(op == mulop)
			o = " * ";
		else if(op == orop)
			o = " | ";
		else if(op == modop)
			o = " % ";
		else if(op == lshop)
			o = " << ";
		else if(op == rshop)
			o = " >> ";
		else if(op == subop)
			o = " - ";
		else if(op == urshop)
			o = " >>> ";
		else
			o = " ^ ";
		return o;
	}

	@Override
	public String toString() {
		return getIdentifier().toString();
	}
	
	public Primitive getArg1() {
		return arg1;
	}
	
	public Primitive getArg2() {
		return arg2;
	}
	
	public int getOpInt() {
		return op;
	}
	
	public boolean isAddExpr() {
		return op == addop;
	}
	public boolean isAndExpr() {
		return op == andop;
	}
	public boolean isCmpExpr() {
		return op == cmpop;
	}
	public boolean isCmpgExpr() {
		return op == cmpgop;
	}
	public boolean isCmplExpr() {
		return op == cmplop;
	}
	public boolean isEqExpr() {
		return op == eqop;
	}
	public boolean isGeExpr() {
		return op == gteop;
	}
	public boolean isGtExpr() {
		return op == gtop;
	}
	public boolean isLeExpr() {
		return op == lteop;
	}
	public boolean isLtExpr() {
		return op == ltop;
	}
	public boolean isNeExpr() {
		return op == neqop;
	}
	public boolean isDivExpr() {
		return op == divop;
	}
	public boolean isMulExpr() {
		return op == mulop;
	}
	public boolean isOrExpr() {
		return op == orop;
	}
	public boolean isModExpr() {
		return op == modop;
	}
	public boolean isLshExpr() {
		return op == lshop;
	}
	public boolean isRshExpr() {
		return op == rshop;
	}
	public boolean isSubExpr() {
		return op == subop;
	}
	public boolean isUrshExpr() {
		return op == urshop;
	}
	public boolean isXorExpr() {
		return op == xorop;
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

	@Override
	public boolean isNegative() {
		return isNegative;
	}

	@Override
	public Expression negate() {
		Primitive arg1 = this.arg1;
		Primitive arg2 = this.arg2;
		int type = this.type; 
		int op = this.op;
		boolean isNegative = this.isNegative;
		if(isNegative) {
			return new Expression(arg1, arg2, op, type, !isNegative);
		} else {
			if(isAddExpr()) {
				op = subop;
				arg1 = arg1.negate();
			} else if(isSubExpr()) {
				op = addop;
				arg1 = arg1.negate();
			} else if(isAddExpr()) {
				op = orop;
				arg1 = arg1.negate();
				arg2 = arg2.negate();
			} else if(isOrExpr()) {
				op = andop;
				arg1 = arg1.negate();
				arg2 = arg2.negate();
			} else if(isMulExpr()) {
				arg1 = arg1.negate();
				arg2 = arg2.negate();
			} else if(isDivExpr()) {
				arg1 = arg1.negate();
			} else if(isEqExpr()) {
				op = neqop;
			} else if(isNeExpr()) {
				op = eqop;
			} else if(isGtExpr()) {
				op = lteop;
			} else if(isGeExpr()) {
				op = ltop;
			} else if(isLtExpr()) {
				op = gteop;
			} else if(isLeExpr()) {
				op = gtop;
			} else {
				/* Not sure how to negative cmp, cmpg, cmpl, mod, lsh, rsh, and ursh.
				 * Xor should not occur because it is translated to and, or, not in
				 * DataWrapper so it should be handled.
				 */
				isNegative = !isNegative;
			}
			return new Expression(arg1, arg2, op, type, isNegative);
		}
	}
	
	public Expression getAsInt() {
		if(isInt())
			return this;
		return new Expression(arg1, arg2, op, itype, isNegative);
	}
	
	public Expression getAsLong() {
		if(isLong())
			return this;
		return new Expression(arg1, arg2, op, ltype, isNegative);
	}
	
	public Expression getAsShort() {
		if(isShort())
			return this;
		return new Expression(arg1, arg2, op, stype, isNegative);
	}
	
	public Expression getAsByte() {
		if(isByte())
			return this;
		return new Expression(arg1, arg2, op, btype, isNegative);
	}
	
	public Expression getAsChar() {
		if(isChar())
			return this;
		return new Expression(arg1, arg2, op, ctype, isNegative);
	}
	
	public Expression getAsFloat() {
		if(isFloat())
			return this;
		return new Expression(arg1, arg2, op, ftype, isNegative);
	}
	
	public Expression getAsDouble() {
		if(isDouble())
			return this;
		return new Expression(arg1, arg2, op, dtype, isNegative);
	}
	
	public Expression getAsBoolean() {
		if(isBoolean())
			return this;
		return new Expression(arg1, arg2, op, booltype, isNegative);
	}

}
