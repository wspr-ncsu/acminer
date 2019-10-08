package org.sag.acminer.phases.acminer.dw;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.sag.acminer.database.defusegraph.id.Identifier;
import org.sag.common.tools.SortingMethods;

import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.LongType;
import soot.RefType;
import soot.ShortType;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.dexpler.typing.UntypedConstant;
import soot.dexpler.typing.UntypedIntOrFloatConstant;
import soot.dexpler.typing.UntypedLongOrDoubleConstant;
import soot.jimple.AddExpr;
import soot.jimple.AndExpr;
import soot.jimple.BinopExpr;
import soot.jimple.CmpExpr;
import soot.jimple.CmpgExpr;
import soot.jimple.CmplExpr;
import soot.jimple.DivExpr;
import soot.jimple.EqExpr;
import soot.jimple.GeExpr;
import soot.jimple.GtExpr;
import soot.jimple.LeExpr;
import soot.jimple.LtExpr;
import soot.jimple.MulExpr;
import soot.jimple.NeExpr;
import soot.jimple.OrExpr;
import soot.jimple.RemExpr;
import soot.jimple.ShlExpr;
import soot.jimple.ShrExpr;
import soot.jimple.SubExpr;
import soot.jimple.UshrExpr;
import soot.jimple.XorExpr;
import soot.jimple.toolkits.typing.fast.Integer127Type;
import soot.jimple.toolkits.typing.fast.Integer1Type;
import soot.jimple.toolkits.typing.fast.Integer32767Type;

public abstract class DataWrapper implements Comparable<DataWrapper> {
	
	private final Identifier id;
	
	public DataWrapper(Identifier id) {
		this.id = id;
	}
	
	public abstract boolean equals(Object o);
	public abstract int hashCode();
	public abstract String toString();
	public abstract DataWrapper clone();
	
	public Identifier getIdentifier() {
		return id;
	}
	
	@Override
	public int compareTo(DataWrapper o) {
		if(this instanceof PrimitiveConstant && o instanceof PrimitiveConstant)
			return Double.compare(((PrimitiveConstant)this).getDouble(), ((PrimitiveConstant)o).getDouble());
		else if(this instanceof PrimitiveConstant)
			return -1;
		else if(o instanceof PrimitiveConstant)
			return 1;
		else if((this instanceof AllConstant && o instanceof AllConstant) 
				|| (this instanceof NoneConstant && o instanceof NoneConstant)
				|| (this instanceof NullConstant && o instanceof NullConstant))
			return 0;
		else if(this instanceof AllConstant)
			return -1;
		else if(o instanceof AllConstant)
			return 1;
		else if(this instanceof NoneConstant)
			return -1;
		else if(o instanceof NoneConstant)
			return 1;
		else if(this instanceof NullConstant)
			return -1;
		else if(o instanceof NullConstant)
			return 1;
		else if(this instanceof StringConstant && o instanceof StringConstant)
			return SortingMethods.sComp.compare(this.toString(), o.toString());
		else if(this instanceof StringConstant)
			return -1;
		else if(o instanceof StringConstant)
			return 1;
		else
			return SortingMethods.sComp.compare(this.toString(), o.toString());
	}
	
	public boolean isConstant() {
		return this instanceof Constant;
	}
	
	public boolean isVariable() {
		return this instanceof Variable;
	}
	
	public boolean isExpression() {
		return this instanceof Expression;
	}
	
	public boolean isPrimitive() {
		return this instanceof Primitive;
	}
	
	public boolean isPrimitiveConstant() {
		return this instanceof PrimitiveConstant;
	}
	
	public boolean isPrimitiveVariable() {
		return this instanceof PrimitiveVariable;
	}
	
	public boolean isStringConstant() {
		return this instanceof StringConstant;
	}
	
	public boolean isStringVariable() {
		return this instanceof StringVariable;
	}
	
	public boolean isAllValueConstant() {
		return this instanceof AllConstant;
	}
	
	public boolean isNoValueConstant() {
		return this instanceof NoneConstant;
	}
	
	public boolean isNullConstant() {
		return this instanceof NullConstant;
	}
	
	private static volatile ReadWriteLock rwlock = null;
	private static volatile Map<DataWrapper,DataWrapper> cache = null;
	private static volatile DataWrapper allConstant = null;
	private static volatile DataWrapper noneConstant = null;
	private static volatile DataWrapper nullConstant = null;
	
	public static DataWrapper getAllConstant() {
		return allConstant;
	}
	
	public static DataWrapper getNoneConstant() {
		return noneConstant;
	}
	
	public static DataWrapper getNullConstant() {
		return nullConstant;
	}
	
	public static void initCache() {
		if(cache == null) {
			rwlock = new ReentrantReadWriteLock();
			cache = new HashMap<>();
			allConstant = new AllConstant();
			noneConstant = new NoneConstant();
			nullConstant = new NullConstant();
		}
	}
	
	public static void clearCache() {
		rwlock = null;
		cache = null;
		allConstant = null;
		noneConstant = null;
		nullConstant = null;
	}
	
	private static DataWrapper checkCache(DataWrapper in) {
		if(in.isAllValueConstant()) {
			return allConstant;
		} else if(in.isNoValueConstant()) {
			return noneConstant;
		} else if(in.isNullConstant()) {
			return nullConstant;
		} else {
			rwlock.readLock().lock();
			try {
				DataWrapper exist = cache.get(in);
				if(exist != null)
					return exist;
			} finally {
				rwlock.readLock().unlock();
			}
			
			rwlock.writeLock().lock();
			try {
				DataWrapper exist = cache.get(in);
				if(exist == null) {
					cache.put(in, in);
					exist = in;
				}
				return exist;
			} finally {
				rwlock.writeLock().unlock();
			}
		}
	}
	
	public static DataWrapper getConstantOrVariable(Identifier val, Value v, Type t, SootMethod source, Unit unit) {
		if(v != null && v instanceof soot.jimple.Constant) {
			DataWrapper ret;
			if(v instanceof soot.jimple.NumericConstant) {
				PrimitiveConstant p;
				if(v instanceof soot.jimple.IntConstant)
					p = PrimitiveConstant.getIntConstant(((soot.jimple.IntConstant)v).value);
				else if(v instanceof soot.jimple.LongConstant)
					p = PrimitiveConstant.getLongConstant(((soot.jimple.LongConstant)v).value);
				else if(v instanceof soot.jimple.DoubleConstant)
					p = PrimitiveConstant.getDoubleConstant(((soot.jimple.DoubleConstant)v).value);
				else if(v instanceof soot.jimple.FloatConstant)
					p = PrimitiveConstant.getFloatConstant(((soot.jimple.FloatConstant)v).value);
				else
					throw new RuntimeException("Error: The given value is of unhandled numeric constant type '" + v.getClass().toString() + "'");
				
				if(t instanceof IntType || t instanceof Integer127Type || t instanceof Integer1Type || t instanceof Integer32767Type)
					ret = p.getAsInt();
				else if(t instanceof BooleanType)
					ret = p.getAsBoolean();
				else if(t instanceof ByteType)
					ret = p.getAsByte();
				else if(t instanceof CharType)
					ret = p.getAsChar();
				else if(t instanceof DoubleType)
					ret = p.getAsDouble();
				else if(t instanceof FloatType)
					ret = p.getAsFloat();
				else if(t instanceof LongType)
					ret = p.getAsLong();
				else if(t instanceof ShortType)
					ret = p.getAsShort();
				else
					throw new RuntimeException("Error: The assignment type is not a primitive type but " + t.getClass());
			} else if(v instanceof soot.jimple.StringConstant) {
				ret = new StringConstant(((soot.jimple.StringConstant)v).value);
			} else if(v instanceof UntypedConstant) {
				PrimitiveConstant p;
				if(v instanceof UntypedIntOrFloatConstant) {
					if(t instanceof FloatType || t instanceof DoubleType)
						p = PrimitiveConstant.getDoubleConstant(((UntypedIntOrFloatConstant)v).toFloatConstant().value);
					else
						p = PrimitiveConstant.getLongConstant(((UntypedIntOrFloatConstant)v).toIntConstant().value);
				} else {
					if(t instanceof FloatType || t instanceof DoubleType)
						p = PrimitiveConstant.getDoubleConstant(((UntypedLongOrDoubleConstant)v).toDoubleConstant().value);
					else
						p = PrimitiveConstant.getLongConstant(((UntypedLongOrDoubleConstant)v).toLongConstant().value);
				}
				
				if(t instanceof IntType || t instanceof Integer127Type || t instanceof Integer1Type || t instanceof Integer32767Type)
					ret = p.getAsInt();
				else if(t instanceof BooleanType)
					ret = p.getAsBoolean();
				else if(t instanceof ByteType)
					ret = p.getAsByte();
				else if(t instanceof CharType)
					ret = p.getAsChar();
				else if(t instanceof DoubleType)
					ret = p.getAsDouble();
				else if(t instanceof FloatType)
					ret = p.getAsFloat();
				else if(t instanceof LongType)
					ret = p.getAsLong();
				else if(t instanceof ShortType)
					ret = p.getAsShort();
				else
					throw new RuntimeException("Error: The assignment type is not a primitive type but " + t.getClass());
			} else if(v instanceof soot.jimple.NullConstant) {
				ret = nullConstant;
			} else {
				if(val == null)
					val = Identifier.getValueId(v, source, unit);
				String str = val.toString();
				if(str.equals(AllConstant.val))
					ret = getAllConstant();
				else if(str.equals(NoneConstant.val))
					ret = getNoneConstant();
				else
					ret = new UnknownConstant(val);
			}
			return checkCache(ret);
		} else {
			if(val == null && v == null)
				throw new RuntimeException("Error: The value string and value arguments both cannot be null.");
			if(val == null)
				val = Identifier.getValueId(v, source, unit);
			String str = val.toString();
			if(str.equals(AllConstant.val))
				return getAllConstant();
			else if(str.equals(NoneConstant.val))
				return getNoneConstant();
			else
				return getVariable(val, t);
		}
	}
	
	public static DataWrapper getVariable(Identifier val, Type t) {
		DataWrapper p;
		if(t instanceof IntType || t instanceof Integer127Type || t instanceof Integer1Type || t instanceof Integer32767Type)
			p = PrimitiveVariable.getIntVariable(val);
		else if(t instanceof BooleanType)
			p = PrimitiveVariable.getBooleanVariable(val);
		else if(t instanceof ByteType)
			p = PrimitiveVariable.getByteVariable(val);
		else if(t instanceof CharType)
			p = PrimitiveVariable.getCharVariable(val);
		else if(t instanceof DoubleType)
			p = PrimitiveVariable.getDoubleVariable(val);
		else if(t instanceof FloatType)
			p = PrimitiveVariable.getFloatVariable(val);
		else if(t instanceof LongType)
			p = PrimitiveVariable.getLongVariable(val);
		else if(t instanceof ShortType)
			p = PrimitiveVariable.getShortVariable(val);
		else if(t instanceof RefType && ((RefType)t).getClassName().equals("java.lang.String"))
			p = new StringVariable(val);
		else
			p = new UnknownVariable(val);
		return checkCache(p);
	}
	
	public static DataWrapper getPrimitiveFromNegExpr(DataWrapper arg, Type t) {
		DataWrapper ret;
		if(arg.isAllValueConstant() || arg.isNoValueConstant()) {
			ret = arg;
		} else {
			if(!arg.isPrimitive())
				throw new RuntimeException("Error: Tried to negate a non-primitive type '" + arg.getClass().getSimpleName() + "'.");
			Primitive newP = ((Primitive)arg).negate();
			if(t instanceof IntType || t instanceof Integer127Type || t instanceof Integer1Type || t instanceof Integer32767Type)
				newP = newP.getAsInt();
			else if(t instanceof BooleanType)
				newP = newP.getAsBoolean();
			else if(t instanceof ByteType)
				newP = newP.getAsByte();
			else if(t instanceof CharType)
				newP = newP.getAsChar();
			else if(t instanceof DoubleType)
				newP = newP.getAsDouble();
			else if(t instanceof FloatType)
				newP = newP.getAsFloat();
			else if(t instanceof LongType)
				newP = newP.getAsLong();
			else if(t instanceof ShortType)
				newP = newP.getAsShort();
			else
				throw new RuntimeException("Error: The assignment type is not a primitive type but " + t.getClass());
			ret = newP;
		}
		return checkCache(ret);
	}
	
	public static DataWrapper getPrimitiveFromBinop(DataWrapper arg1, DataWrapper arg2, Type t, BinopExpr expr) {
		DataWrapper ret = null;
		if(arg1.isAllValueConstant() || arg2.isAllValueConstant()) {
			ret = arg1.isAllValueConstant() ? arg1 : arg2;
		} else if(arg1.isNoValueConstant() || arg2.isNoValueConstant()) {
			ret = arg1.isNoValueConstant() ? arg1 : arg2;
		} else if(arg1.isPrimitiveConstant() && arg2.isPrimitiveConstant()) {
			PrimitiveConstant p = (PrimitiveConstant)arg1;
			PrimitiveConstant p2 = (PrimitiveConstant)arg2;
			PrimitiveConstant newP;
			
			if(expr instanceof AddExpr)
				newP = p.add(p2);
			else if(expr instanceof AndExpr)
				newP = p.and(p2);
			else if(expr instanceof CmpExpr)
				newP = p.compareTo(p2);
			else if(expr instanceof CmpgExpr)
				newP = p.compareToG(p2);
			else if(expr instanceof CmplExpr)
				newP = p.compareToL(p2);
			else if(expr instanceof EqExpr)
				newP = p.equalTo(p2);
			else if(expr instanceof GeExpr)
				newP = p.greaterThanOrEqualTo(p2);
			else if(expr instanceof GtExpr)
				newP = p.greaterThan(p2);
			else if(expr instanceof LeExpr)
				newP = p.lessThanOrEqualTo(p2);
			else if(expr instanceof LtExpr)
				newP = p.lessThan(p2);
			else if(expr instanceof NeExpr)
				newP = p.notEqualTo(p2);
			else if(expr instanceof DivExpr)
				newP = p.divide(p2);
			else if(expr instanceof MulExpr)
				newP = p.multiply(p2);
			else if(expr instanceof OrExpr)
				newP = p.or(p2);
			else if(expr instanceof RemExpr)
				newP = p.mod(p2);
			else if(expr instanceof ShlExpr)
				newP = p.leftShift(p2);
			else if(expr instanceof ShrExpr)
				newP = p.rightShift(p2);
			else if(expr instanceof SubExpr)
				newP = p.subtract(p2);
			else if(expr instanceof UshrExpr)
				newP = p.unsignedRightShift(p2);
			else if(expr instanceof XorExpr)
				newP = p.xor(p2);
			else
				throw new RuntimeException("Error: '" + expr.getClass().getSimpleName() + "' is not a supported two constant binop.");
			
			if(t instanceof IntType || t instanceof Integer127Type || t instanceof Integer1Type || t instanceof Integer32767Type)
				newP = newP.getAsInt();
			else if(t instanceof BooleanType)
				newP = newP.getAsBoolean();
			else if(t instanceof ByteType)
				newP = newP.getAsByte();
			else if(t instanceof CharType)
				newP = newP.getAsChar();
			else if(t instanceof DoubleType)
				newP = newP.getAsDouble();
			else if(t instanceof FloatType)
				newP = newP.getAsFloat();
			else if(t instanceof LongType)
				newP = newP.getAsLong();
			else if(t instanceof ShortType)
				newP = newP.getAsShort();
			else
				throw new RuntimeException("Error: The assignment type is not a primitive type but " + t.getClass());
			ret = newP;
		} else {
			if(!arg1.isPrimitive() || !arg2.isPrimitive())
				throw new RuntimeException("Error: Tried to get an expression from a non-primitive type '" 
						+ arg1.getClass().getSimpleName() + "' or '" + arg2.getClass().getSimpleName() + "'.");
			Primitive parg1 = (Primitive)arg1;
			Primitive parg2 = (Primitive)arg2;
			int type;
			int op;
			
			if(t instanceof IntType || t instanceof Integer127Type || t instanceof Integer1Type || t instanceof Integer32767Type)
				type = Primitive.itype;
			else if(t instanceof BooleanType)
				type = Primitive.booltype;
			else if(t instanceof ByteType)
				type = Primitive.btype;
			else if(t instanceof CharType)
				type = Primitive.ctype;
			else if(t instanceof DoubleType)
				type = Primitive.dtype;
			else if(t instanceof FloatType)
				type = Primitive.ftype;
			else if(t instanceof LongType)
				type = Primitive.ltype;
			else if(t instanceof ShortType)
				type = Primitive.stype;
			else
				throw new RuntimeException("Error: The assignment type is not a primitive type but " + t.getClass());
			
			if(expr instanceof AddExpr)
				op = Primitive.addop;
			else if(expr instanceof AndExpr)
				op = Primitive.andop;
			else if(expr instanceof CmpExpr)
				op = Primitive.cmpop;
			else if(expr instanceof CmpgExpr)
				op = Primitive.cmpgop;
			else if(expr instanceof CmplExpr)
				op = Primitive.cmplop;
			else if(expr instanceof EqExpr)
				op = Primitive.eqop;
			else if(expr instanceof GeExpr)
				op = Primitive.gteop;
			else if(expr instanceof GtExpr)
				op = Primitive.gtop;
			else if(expr instanceof LeExpr)
				op = Primitive.lteop;
			else if(expr instanceof LtExpr)
				op = Primitive.ltop;
			else if(expr instanceof NeExpr)
				op = Primitive.neqop;
			else if(expr instanceof DivExpr)
				op = Primitive.divop;
			else if(expr instanceof MulExpr)
				op = Primitive.mulop;
			else if(expr instanceof OrExpr)
				op = Primitive.orop;
			else if(expr instanceof RemExpr)
				op = Primitive.modop;
			else if(expr instanceof ShlExpr)
				op = Primitive.lshop;
			else if(expr instanceof ShrExpr)
				op = Primitive.rshop;
			else if(expr instanceof SubExpr)
				op = Primitive.subop;
			else if(expr instanceof UshrExpr)
				op = Primitive.urshop;
			else if(expr instanceof XorExpr)
				op = Primitive.xorop;
			else
				throw new RuntimeException("Error: '" + expr.getClass().getSimpleName() + "' is not a supported two constant binop.");
			
			if(op == Primitive.addop || op == Primitive.subop) {
				Deque<Primitive> parts = new ArrayDeque<>();
				Deque<Expression> queue = new ArrayDeque<>();
				queue.add(new Expression(parg1, parg2, op, type, false));
				while(!queue.isEmpty()) {
					Expression cur = queue.poll();
					if(cur.isAddExpr() || cur.isSubExpr()) {
						Primitive a = cur.isNegative() ? cur.getArg1().negate() : cur.getArg1();
						Primitive b = ((cur.isNegative() && !cur.isSubExpr()) || (!cur.isNegative() && cur.isSubExpr())) ? cur.getArg2().negate() : cur.getArg2();
						if(a.isExpression())
							queue.add((Expression)a);
						else
							parts.add(a);
						if(b.isExpression())
							queue.add((Expression)b);
						else
							parts.add(b);
					} else {
						parts.add(cur);
					}
				}
				
				PrimitiveConstant c = null;
				for(Iterator<Primitive> it = parts.iterator(); it.hasNext();) {
					Primitive cur = it.next();
					if(cur.isPrimitiveConstant()) {
						if(c == null)
							c = (PrimitiveConstant)cur;
						else
							c = c.add((PrimitiveConstant)cur);
						it.remove();
					}
				}
				
				if(c != null) {
					if(c.isZero()) {
						if(c.isNegative())
							c = c.negate();
						if(parts.isEmpty())
							parts.add(c);
					} else {
						parts.addFirst(c);
					}
				}
				
				List<Primitive> temp = new ArrayList<>(parts);
				Collections.sort(temp, new Comparator<Primitive>() {
					@Override
					public int compare(Primitive o1, Primitive o2) {
						if(o1.isNegative() && !o2.isNegative()) {
							return 1;
						} else if(!o1.isNegative() && o2.isNegative()) {
							return -1;
						} else {
							//Should only be one primitive constant in the chain now
							if(o1 instanceof PrimitiveConstant) {
								return -1;
							} else if(o2 instanceof PrimitiveConstant) {
								return 1;
							} else if(o1 instanceof PrimitiveVariable && !(o2 instanceof PrimitiveVariable)) {
								return -1;
							} else if(!(o1 instanceof PrimitiveVariable) && o2 instanceof PrimitiveVariable) {
								return 1;
							} else {
								return o1.toString().compareTo(o2.toString());
							}
						}
					}
				});
				parts = new ArrayDeque<>(temp);
				
				if(parts.size() == 1) {
					ret = parts.poll();
				} else {
					boolean isSubtract = false;
					Expression e = null;
					while(!parts.isEmpty()) {
						Primitive p = parts.pollLast();
						if(e == null) {
							Primitive p2 = parts.pollLast();
							int newop;
							if(p2.isNegative()) {
								newop = Primitive.subop;
								p2 = p2.negate();
							} else {
								newop = Primitive.addop;
							}
							if(p.isNegative()) {
								isSubtract = true;
								p = p.negate();
							}
							e = new Expression(p, p2, newop, p.getHighestTypeInt(p2), false);
						} else {
							int newop = isSubtract ? Primitive.subop : Primitive.addop;
							if(p.isNegative()) {
								isSubtract = true;
								p = p.negate();
							} else {
								isSubtract = false;
							}
							e = new Expression(p, e, newop, p.getHighestTypeInt(e), false);
						}
					}
					if(isSubtract)
						e = new Expression(e.getArg1().negate(), e.getArg2(), e.getOpInt(), e.getTypeInt(), e.isNegative());
					ret = e;
				}
			} else if(op == Primitive.mulop) {
				Deque<Primitive> parts = new ArrayDeque<>();
				Deque<Expression> queue = new ArrayDeque<>();
				queue.add(new Expression(parg1, parg2, op, type, false));
				while(!queue.isEmpty()) {
					Expression cur = queue.poll();
					if(cur.isMulExpr()) {
						Primitive a = cur.getArg1();
						Primitive b = cur.getArg2();
						if(a.isExpression())
							queue.add((Expression)a);
						else
							parts.add(a);
						if(b.isExpression())
							queue.add((Expression)b);
						else
							parts.add(b);
					} else {
						parts.add(cur);
					}
				}
				
				PrimitiveConstant c = null;
				for(Iterator<Primitive> it = parts.iterator(); it.hasNext();) {
					Primitive cur = it.next();
					if(cur.isPrimitiveConstant()) {
						if(c == null)
							c = (PrimitiveConstant)cur;
						else
							c = c.multiply((PrimitiveConstant)cur);
						it.remove();
					}
				}
				
				if(c != null) {
					if(c.isZero()) {
						if(c.isNegative())
							c = c.negate();
						parts.clear();
						parts.add(c);
					} else {
						parts.addFirst(c);
					}
				}
				
				List<Primitive> temp = new ArrayList<>(parts);
				Collections.sort(temp, new Comparator<Primitive>() {
					@Override
					public int compare(Primitive o1, Primitive o2) {
						if(o1.isNegative() && !o2.isNegative()) {
							return 1;
						} else if(!o1.isNegative() && o2.isNegative()) {
							return -1;
						} else {
							//Should only be one primitive constant in the chain now
							if(o1 instanceof PrimitiveConstant) {
								return -1;
							} else if(o2 instanceof PrimitiveConstant) {
								return 1;
							} else if(o1 instanceof PrimitiveVariable && !(o2 instanceof PrimitiveVariable)) {
								return -1;
							} else if(!(o1 instanceof PrimitiveVariable) && o2 instanceof PrimitiveVariable) {
								return 1;
							} else {
								return o1.toString().compareTo(o2.toString());
							}
						}
					}
				});
				parts = new ArrayDeque<>(temp);
				
				if(parts.size() == 1) {
					ret = parts.poll();
				} else {
					Expression e = null;
					while(!parts.isEmpty()) {
						Primitive p = parts.pollLast();
						if(e == null) {
							Primitive p2 = parts.pollLast();
							e = new Expression(p, p2, Primitive.mulop, p.getHighestTypeInt(p2), false);
						} else {
							e = new Expression(p, e, Primitive.mulop, p.getHighestTypeInt(e), false);
						}
					}
					ret = e;
				}
			} else {
				boolean isp1 = parg1 instanceof PrimitiveConstant;
				boolean isp2 = parg2 instanceof PrimitiveConstant;
				boolean isz1 = isp1 && ((PrimitiveConstant)parg1).isZero();
				boolean isz2 = isp2 && ((PrimitiveConstant)parg2).isZero();
				if(op == Primitive.divop) {
					if(isp1 && isz1 && isp2 && isz2)
						ret = PrimitiveVariable.getDoubleVariable(Identifier.getLiteralId("NAN"));
					if(isp1 && isz1)
						ret = parg1;
					else if(isp2 && isz2)
						ret = PrimitiveVariable.getDoubleVariable(Identifier.getLiteralId("INFINITY"));
				} else if(op == Primitive.modop) {
					if(isp1 && isz1 && isp2 && isz2)
						ret = PrimitiveVariable.getDoubleVariable(Identifier.getLiteralId("NAN"));
					if(isp1 && isz1)
						ret = parg1;
					else if(isp2 && isz2)
						ret = PrimitiveVariable.getDoubleVariable(Identifier.getLiteralId("NAN"));
				} else if(op == Primitive.andop) {
					if((isp1 && isz1) || (isp2 && isz2))
						ret = isz1 ? parg1 : parg2;
				} else if(op == Primitive.orop) {
					if((isp1 && isz1) || (isp2 && isz2))
						ret = isz1 ? parg2 : parg1;
				} else if(op == Primitive.xorop) {
					if((isp1 && isz1) || (isp2 && isz2)) {
						ret = isz1 ? parg2 : parg1;
					} else {
						//Convert xor to and, or, not to get rid of it (A or B) and (!A or !B)
						ret = new Expression(new Expression(parg1, parg2, Primitive.orop, type, false), 
								new Expression(parg1.negate(), parg2.negate(), Primitive.orop, type, false), 
								Primitive.andop, type, false);
					}
				} else if(op == Primitive.lshop || op == Primitive.rshop || op == Primitive.urshop) {
					if((isp1 && isz1) || (isp2 && isz2))
						ret = parg1;
				}
				
				if(ret == null)
					ret = new Expression(parg1, parg2, op, type, false);
			}
		}
		return checkCache(ret);
	}

}
