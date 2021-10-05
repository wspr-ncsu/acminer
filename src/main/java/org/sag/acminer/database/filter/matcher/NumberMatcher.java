package org.sag.acminer.database.filter.matcher;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.sag.acminer.database.defusegraph.INode;

import soot.Value;
import soot.jimple.DoubleConstant;
import soot.jimple.FloatConstant;
import soot.jimple.IntConstant;
import soot.jimple.LongConstant;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("NumberMatcher")
public class NumberMatcher extends Matcher<Number> {
	
	public static enum NumberMatcherOpType {
		INT(Integer.class.getSimpleName().toLowerCase()),
		LONG(Long.class.getSimpleName().toLowerCase()),
		BYTE(Byte.class.getSimpleName().toLowerCase()),
		SHORT(Short.class.getSimpleName().toLowerCase()),
		BIG_INTEGER(BigInteger.class.getSimpleName().toLowerCase()),
		ATOMIC_INTEGER(AtomicInteger.class.getSimpleName().toLowerCase()),
		ATOMIC_LONG(AtomicLong.class.getSimpleName().toLowerCase()),
		DOUBLE(Double.class.getSimpleName().toLowerCase()),
		FLOAT(Float.class.getSimpleName().toLowerCase()),
		BIG_DECIMAL(BigDecimal.class.getSimpleName().toLowerCase()),
		INTEGER_MASK(Integer.class.getSimpleName().toLowerCase()+"-mask"),
		LONG_MASK(Long.class.getSimpleName().toLowerCase()+"-mask");
		private String val;
		private NumberMatcherOpType(String val) { this.val = val; }
		public String toString() { return val; }
		public static NumberMatcherOpType fromString(String str) {
			for(NumberMatcherOpType t : NumberMatcherOpType.values()) {
				if(t.val.equals(str))
					return t;
			}
			return null;
		}
	}
	
	private static final Pattern typeP = Pattern.compile("^\\(\\s*("+NumberMatcherOpType.INT+"|"+NumberMatcherOpType.LONG+"|"
			+NumberMatcherOpType.BYTE+"|"+NumberMatcherOpType.SHORT+"|"+NumberMatcherOpType.BIG_INTEGER+"|"
			+NumberMatcherOpType.ATOMIC_INTEGER+"|"+NumberMatcherOpType.ATOMIC_LONG+"|"+NumberMatcherOpType.DOUBLE+"|"
			+NumberMatcherOpType.FLOAT+"|"+NumberMatcherOpType.BIG_DECIMAL+"|"+NumberMatcherOpType.INTEGER_MASK+"|"
			+NumberMatcherOpType.LONG_MASK+")\\s+(.*)\\)$");
	
	public NumberMatcher(String value) {
		super(value);
	}
	
	public NumberMatcher(Op<Number> operation) {
		super(operation);
	}
	
	/** Constructs a restriction for the following number. Assumes no mask. */
	public NumberMatcher(Number num) {
		super(getOp(num));
	}
	
	/** Short cut that makes it easier to specify single value masks for int */
	public NumberMatcher(int num, boolean mask) {
		super(mask ? getIntegerMaskOp(num) : getOp(num));
	}
	
	/** Short cut that makes it easier to specify single value masks for long */
	public NumberMatcher(long num, boolean mask) {
		super(mask ? getLongMaskOp(num) : getOp(num));
	}
	
	/** Short cut that assumes since we have a list of numbers, this is a mask op for an integer */
	public NumberMatcher(int... nums) {
		super(getIntegerMaskOp(nums));
	}
	
	/** Short cut that assumes since we have a list of numbers, this is a mask op for an long */
	public NumberMatcher(long... nums) {
		super(getLongMaskOp(nums));
	}
	
	@Override
	protected Op<Number> leafStringToOp(String s) {
		java.util.regex.Matcher m = typeP.matcher(s);
		if(m.matches()) {
			String type = m.group(1).trim();
			String val = m.group(2).trim();
			if(val != null) {
				val = parseLeafArgs(val.trim()).get(0);
				NumberMatcherOpType t = NumberMatcherOpType.fromString(type);
				switch(t) {
					case INT: return new IntegerOp(Integer.parseInt(val));
					case LONG: return new LongOp(Long.parseLong(val));
					case BYTE: return new ByteOp(Byte.parseByte(val));
					case SHORT: return new ShortOp(Short.parseShort(val));
					case BIG_INTEGER: return new BigIntegerOp(new BigInteger(val));
					case ATOMIC_INTEGER: return new AtomicIntegerOp(new AtomicInteger(Integer.parseInt(val)));
					case ATOMIC_LONG: return new AtomicLongOp(new AtomicLong(Long.parseLong(val)));
					case DOUBLE: return new DoubleOp(Double.parseDouble(val));
					case FLOAT: return new FloatOp(Float.parseFloat(val));
					case BIG_DECIMAL: return new BigDecimalOp(new BigDecimal(val));
					case INTEGER_MASK: return new IntegerMaskOp(val);
					case LONG_MASK: return new LongMaskOp(val);
					default: return null;
				}
			}
			return null;
		}
		return null;
	}
	
	@Override
	public boolean matcher(Object... objects) {
		if(objects == null || objects.length <= 0 || objects[0] == null || !(objects[0] instanceof Number))
			return false;
		Object u = objects[0];
		if(u instanceof Number)
			return matches((Number)u);
		else if(u instanceof IntConstant)
			return matches(((IntConstant)u).value);
		else if(u instanceof LongConstant)
			return matches(((LongConstant)u).value);
		else if(u instanceof DoubleConstant)
			return matches(((DoubleConstant)u).value);
		else if(u instanceof FloatConstant)
			return matches(((FloatConstant)u).value);
		else if(u instanceof INode) {
			Value v = ((INode)u).getValue();
			if(v != null) {
				if(v instanceof IntConstant)
					return matches(((IntConstant)v).value);
				else if(v instanceof LongConstant)
					return matches(((LongConstant)v).value);
				else if(v instanceof DoubleConstant)
					return matches(((DoubleConstant)v).value);
				else if(v instanceof FloatConstant)
					return matches(((FloatConstant)v).value);
			}
		}
		return false;
	}
	
	protected boolean matches(Number n) {
		Objects.requireNonNull(n);
		return operation.matches(n); 
	}
	
	@Override
	public boolean equals(Object o) {
		if(super.equals(o)) {
			return o instanceof NumberMatcher;
		}
		return false;
	}
	
	private static void verifyArgs(int size, Number... args) {
		Objects.requireNonNull(args);
		if(args.length != size)
			throw new RuntimeException("Error: Expected " + size + " arguments and got " + args.length + ".");
		for(int i = 0; i < size; i++) {
			Objects.requireNonNull(args[i]);
		}
	}
	
	private static abstract class NumberOp implements Op<Number> {
		protected Number num;
		public NumberOp(Number num) {
			Objects.requireNonNull(num);
			this.num = num;
		}
		public abstract boolean matchInner(Number in);
		@Override
		public boolean matches(Number... args) {
			verifyArgs(1,args);
			Number in = args[0];
			return matchInner(in);
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(opB).append(num.getClass().getSimpleName().toLowerCase());
			sb.append(' ').append(quoteIfNeeded(num.toString())).append(opE);
			return sb.toString();
		}
	}
	
	private static class IntegerOp extends NumberOp {
		public IntegerOp(Number num) { super(num); }
		@Override
		public boolean matchInner(Number in) { return num.equals(in); }
	}
	private static class LongOp extends NumberOp {
		public LongOp(Number num) { super(num); }
		@Override
		public boolean matchInner(Number in) { return num.equals(in); }
	}
	private static class ByteOp extends NumberOp {
		public ByteOp(Number num) { super(num); }
		@Override
		public boolean matchInner(Number in) { return num.equals(in); }
	}
	private static class ShortOp extends NumberOp {
		public ShortOp(Number num) { super(num); }
		@Override
		public boolean matchInner(Number in) { return num.equals(in); }
	}
	private static class BigIntegerOp extends NumberOp {
		public BigIntegerOp(Number num) { super(num); }
		@Override
		public boolean matchInner(Number in) { return num.equals(in); }
	}
	private static class AtomicIntegerOp extends NumberOp {
		public AtomicIntegerOp(Number num) { super(num); }
		@Override
		public boolean matchInner(Number in) { return ((AtomicInteger)in).get() == ((AtomicInteger)num).get(); }
	}
	private static class AtomicLongOp extends NumberOp {
		public AtomicLongOp(Number num) { super(num); }
		@Override
		public boolean matchInner(Number in) { return ((AtomicLong)in).get() == ((AtomicLong)num).get(); }
	}
	private static class DoubleOp extends NumberOp {
		public DoubleOp(Number num) { super(num); }
		@Override
		public boolean matchInner(Number in) { 
			double d1 = ((Double)num).doubleValue();
			double d2 = ((Double)in).doubleValue();
			return d1 == d2 ? true : Double.NaN == d1 && Double.NaN == d2;
		}
	}
	private static class FloatOp extends NumberOp {
		public FloatOp(Number num) { super(num); }
		@Override
		public boolean matchInner(Number in) { 
			float f1 = ((Float)num).floatValue();
			float f2 = ((Float)in).floatValue();
			return f1 == f2 ? true : Float.NaN == f1 && Float.NaN == f2;
		}
	}
	private static class BigDecimalOp extends NumberOp {
		public BigDecimalOp(Number num) { super(num); }
		@Override
		public boolean matchInner(Number in) { return ((BigDecimal)in).compareTo(((BigDecimal)num)) == 0; }
	}
	private static class IntegerMaskOp implements Op<Number> {
		private final String value;
		private final int num;
		public IntegerMaskOp(String value) {
			Objects.requireNonNull(value);
			this.value = value.trim();
			int mask = 0;
			for(String s : value.split("\\|")) {
				mask |= Integer.parseInt(s.trim());
			}
			this.num = mask;
		}
		
		public IntegerMaskOp(int... opts) {
			Objects.requireNonNull(opts);
			StringBuilder sb = new StringBuilder();
			int mask = 0;
			boolean first = true;
			for(int i = 0; i < opts.length; i++) {
				if(first)
					first = false;
				else
					sb.append('|');
				sb.append(opts[i]);
				mask |= opts[i];
			}
			this.value = sb.toString();
			this.num = mask;
		}
		
		@Override
		public boolean matches(Number... args) {
			verifyArgs(1, args);
			Number in = args[0];
			if(!Integer.class.equals(in.getClass()))
				return false;
			return (num & ((Integer)in).intValue()) != 0;
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(opB).append(NumberMatcherOpType.INTEGER_MASK);
			sb.append(' ').append(quoteIfNeeded(value)).append(opE);
			return sb.toString();
		}
	}
	private static class LongMaskOp implements Op<Number> {
		private final String value;
		private final long num;
		public LongMaskOp(String value) {
			Objects.requireNonNull(value);
			this.value = value.trim();
			long mask = 0;
			for(String s : value.split("\\|")) {
				mask |= Long.parseLong(s.trim());
			}
			this.num = mask;
		}
		
		public LongMaskOp(long... opts) {
			Objects.requireNonNull(opts);
			StringBuilder sb = new StringBuilder();
			long mask = 0;
			boolean first = true;
			for(int i = 0; i < opts.length; i++) {
				if(first)
					first = false;
				else
					sb.append('|');
				sb.append(opts[i]);
				mask |= opts[i];
			}
			this.value = sb.toString();
			this.num = mask;
		}
		
		@Override
		public boolean matches(Number... args) {
			verifyArgs(1, args);
			Number in = args[0];
			if(!Long.class.equals(in.getClass()))
				return false;
			return (num & ((Long)in).longValue()) != 0;
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(opB).append(NumberMatcherOpType.LONG_MASK);
			sb.append(' ').append(quoteIfNeeded(value)).append(opE);
			return sb.toString();
		}
	}
	
	public static Op<Number> getNumberOp(Number value) { return getOp(value); }
	public static Op<Number> getNumberOrOp(Number... values) { return getNumberOp(MatcherOpType.OR, values); }
	public static Op<Number> getNumberAndOp(Number... values) { return getNumberOp(MatcherOpType.AND, values); }
	public static Op<Number> getNumberNotOp(Number value) { return getNumberOp(MatcherOpType.NOT, value); }
	public static Op<Number> getIntegerMaskOp(int... values) { return new IntegerMaskOp(values); }
	public static Op<Number> getLongMaskOp(long... values) { return new LongMaskOp(values); }
	
	protected static Op<Number> getOp(Number num) {
		if(num instanceof Integer) {
			return new IntegerOp(num);
		} else if(num instanceof Long) {
			return new LongOp(num);
		} else if(num instanceof Byte) {
			return new ByteOp(num);
		} else if(num instanceof Short) {
			return new ShortOp(num);
		} else if(num instanceof BigInteger) {
			return new BigIntegerOp(num);
		} else if(num instanceof AtomicInteger) {
			return new AtomicIntegerOp(num);
		} else if(num instanceof AtomicLong) {
			return new AtomicLongOp(num);
		} else if(num instanceof Double) {
			return new DoubleOp(num);
		} else if(num instanceof Float) {
			return new FloatOp(num);
		} else if(num instanceof BigDecimal) {
			return new BigDecimalOp(num);
		} else {
			return null;
		}
	}
	
	private static Op<Number> getNumberOp(MatcherOpType type, Number... values) {
		Objects.requireNonNull(values);
		List<Op<Number>> ops = new ArrayList<>();
		for(Number num : values) {
			Objects.requireNonNull(num);
			ops.add(getOp(num));
		}
		switch(type) {
			case AND: return getAndOp(ops);
			case OR: return getOrOp(ops);
			case NOT: return getNotOp(ops.get(0));
			default: return null;
		}	
	}
	
}
