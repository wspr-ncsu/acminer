package org.sag.acminer.database.filter.matcher;

import java.io.ObjectStreamException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.sag.acminer.database.filter.entry.IEntry;
import org.sag.common.tuple.Pair;

import com.google.common.base.CharMatcher;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("Matcher")
public abstract class Matcher<A> implements IMatcher {
	
	public static enum MatcherOpType {
		AND("and"),
		OR("or"),
		NOT("not");
		private String val;
		private MatcherOpType(String val) { this.val = val; }
		public String toString() { return val; }
		public static MatcherOpType fromString(String str) {
			for(MatcherOpType t : MatcherOpType.values()) {
				if(t.val.equals(str))
					return t;
			}
			return null;
		}
	}
	
	public static final char opB = '(';
	public static final char opE = ')';
	public static final char opQ = '`';
	private static final Pattern boolP = Pattern.compile("^\\(\\s*("+MatcherOpType.AND+"|"+MatcherOpType.OR+"|"+MatcherOpType.NOT+")\\s+(.*)\\)$");

	@XStreamAlias("Value")
	@XStreamAsAttribute
	protected final String value;
	@XStreamOmitField
	protected volatile Op<A> operation;
	
	public Matcher(Op<A> operation) {
		Objects.requireNonNull(operation);
		this.value = operation.toString();
		this.operation = operation;
	}
	
	/** Sets the formated value to be whatever is passed into this as value and then proceeds to parse
	 * the formated value into the tree of ops. Note this method is generic (i.e. calls leadStringToOp)
	 * so it can handle any of the
	 * child matcher classes and there ops assuming the formatted values is passed into the correct 
	 * constructor.
	 * @param value
	 */
	public Matcher(String value) {
		Objects.requireNonNull(value);
		this.value = value;
		this.operation = opStringToOp();
	}
	
	protected Object writeReplace() throws ObjectStreamException {
		return this;
	}
	
	protected Object readResolve() throws ObjectStreamException {
		this.operation = opStringToOp();
		return this;
	}
	
	protected abstract Op<A> leafStringToOp(String s);
	
	protected final Op<A> opStringToOp() {
		Op<A> ret = null;
		Deque<Pair<BaseOp<A>,String>> queue = new ArrayDeque<>();
		queue.add(new Pair<BaseOp<A>,String>(null,value));
		while(!queue.isEmpty()) {
			Pair<BaseOp<A>,String> p = queue.poll();
			BaseOp<A> parent = p.getFirst();
			String cur = p.getSecond().trim();
			if((cur.charAt(0) != opB) || (cur.charAt(cur.length() - 1) != opE)) {
				throw new RuntimeException("Error: Improperly formated op '" + cur + "'");
			} else {
				java.util.regex.Matcher m = boolP.matcher(cur);
				if(m.matches()) {
					String type = m.group(1).trim();
					String args = m.group(2).trim();
					BaseOp<A> curOp = null;
					MatcherOpType t = MatcherOpType.fromString(type);
					if(t.equals(MatcherOpType.AND))
						curOp = new AndOp<A>();
					else if(t.equals(MatcherOpType.OR))
						curOp = new OrOp<A>();
					else if(t.equals(MatcherOpType.NOT))
						curOp = new NotOp<A>();
					else
						throw new RuntimeException("Error: Unknown type '" + type + "' for op '" + cur + "'");
					
					if(parent == null)
						ret = curOp;
					else
						parent.add(curOp);
					
					int openIndex = -1;
					int bkCt = 0;
					boolean inquote = false;
					for(int i = 0; i < args.length(); i++) {
						char c = args.charAt(i);
						if(c == opB && !inquote) {
							if(bkCt == 0) 
								openIndex = i;
							bkCt++;
						} else if(c == opQ)
							inquote = inquote ? false : true;
						else if(c == opE && !inquote) {
							if(bkCt == 0)
								throw new RuntimeException("Error: Improperly formated op '" + cur + "'");
							bkCt--;
							if(bkCt == 0) {
								queue.add(new Pair<BaseOp<A>,String>(curOp,args.substring(openIndex, i+1)));
								openIndex = -1;
							}
						}
					}
					if(bkCt != 0 || inquote || openIndex != -1)
						throw new RuntimeException("Error: Improperly formated op '" + cur + "'");
				} else {
					Op<A> curOp = leafStringToOp(cur);
					if(curOp == null)
						throw new RuntimeException("Error: Improperly formated op '" + cur + "'");
					if(parent == null)
						ret = curOp;
					else
						parent.add(curOp);
				}
			}
		}
		if(ret == null)
			throw new RuntimeException("Error: Improperly formated op '" + value + "'");
		return ret;
	}
	
	@Override
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof Matcher))
			return false;
		return value.equals(((Matcher<?>)o).value);
	}
	
	@Override
	public int hashCode() {
		int i =  17;
		i = i * 31 + value.hashCode();
		return i;
	}
	
	@Override
	public String toString() {
		return IEntry.Factory.genSig(getClass().getSimpleName(),Collections.singletonList(genValueSig()));
	}
	
	@Override
	public String getValue() {
		return value;
	}
	
	protected final String genValueSig() {
		return "Value=" + value;
	}
	
	public static interface Op<A> {
		@SuppressWarnings("unchecked")
		public boolean matches(A... args);
		public String toString();
	}
	
	private static abstract class BaseOp<A> implements Op<A> {
		public abstract void add(Op<A> op);
		public abstract void addAll(List<Op<A>> ops);
	}
	
	protected static final class AndOp<A> extends BaseOp<A> {
		
		private final List<Op<A>> ops;
		
		public AndOp() {
			this.ops = new ArrayList<>();
		}
		
		public AndOp(List<Op<A>> ops) {
			this.ops = new ArrayList<>(ops);
		}
		
		public void add(Op<A> op) {
			Objects.requireNonNull(op);
			ops.add(op);
		}
		
		public void addAll(List<Op<A>> ops) {
			Objects.requireNonNull(ops);
			ops.addAll(ops);
		}
		
		@SuppressWarnings("unchecked")
		public boolean matches(A... args) {
			if(ops == null || ops.isEmpty())
				return false;
			for(Op<A> op : ops) {
				if(!op.matches(args))
					return false;
			}
			return true;
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(opB).append(MatcherOpType.AND).append(' ');
			boolean first = true;
			for(Op<A> op : ops) {
				if(first)
					first = false;
				else
					sb.append(' ');
				sb.append(op.toString());
			}
			sb.append(opE);
			return sb.toString();
		}
		
	}
	
	protected static final class OrOp<A> extends BaseOp<A> {
		
		private final List<Op<A>> ops;
		
		public OrOp() {
			this.ops = new ArrayList<>();
		}
		
		public OrOp(List<Op<A>> ops) {
			this.ops = new ArrayList<>(ops);
		}
		
		public void add(Op<A> op) {
			Objects.requireNonNull(op);
			ops.add(op);
		}
		
		public void addAll(List<Op<A>> ops) {
			Objects.requireNonNull(ops);
			ops.addAll(ops);
		}
		
		@SuppressWarnings("unchecked")
		public boolean matches(A... args) {
			if(ops == null || ops.isEmpty())
				return false;
			for(Op<A> op : ops) {
				if(op.matches(args))
					return true;
			}
			return false;
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(opB).append(MatcherOpType.OR).append(' ');
			boolean first = true;
			for(Op<A> op : ops) {
				if(first)
					first = false;
				else
					sb.append(' ');
				sb.append(op.toString());
			}
			sb.append(opE);
			return sb.toString();
		}
		
	}
	
	protected static final class NotOp<A> extends BaseOp<A> {
		
		private volatile Op<A> op;
		
		public NotOp() {}
		
		public NotOp(Op<A> op) {
			this.op = op;
		}
		
		public void add(Op<A> op) {
			Objects.requireNonNull(op);
			this.op = op;
		}
		
		public void addAll(List<Op<A>> ops) {
			Objects.requireNonNull(ops);
			add(ops.get(0));
		}
		
		@SuppressWarnings("unchecked")
		public boolean matches(A... args) {
			if(op == null)
				return false;
			return !op.matches(args);
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(opB).append(MatcherOpType.OR).append(' ').append(op.toString()).append(opE);
			return sb.toString();
		}
		
	}
	
	public static String quoteIfNeeded(String s) {
		if(s.indexOf(opQ) != -1)
			throw new RuntimeException("Error: String '" + s + "' contains the quote char '" + opQ + "'");
		if(s.indexOf(opB) != -1 || s.indexOf(opE) != -1 || CharMatcher.WHITESPACE.matchesAnyOf(s))
			return opQ + s + opQ;
		return s;
	}
	
	public static List<String> parseLeafArgs(String in) {
		in = in.trim();//Either start with ` or a non-whitespace character and end with ` or a non-whitespace character
		if(in.isEmpty())
			return Collections.singletonList("");
		
		List<String> ret = new ArrayList<>();
		int start = 0;
		boolean inQ = false;
		for(int i = 0; i < in.length(); i++) {
			char c = in.charAt(i);
			if(c == opQ) {
				inQ = inQ ? false : true;
			} else if(!inQ && CharMatcher.WHITESPACE.matches(c)) {
				/* Previous character is the last character that is not a whitespace (i here because of the substring specs). 
				 * We trim before the removal of quotes to ensure we keep all whitespace inside of quotes but get rid of all
				 * whitespace outside of quotes.
				 */
				String s = in.substring(start, i).trim();
				if(s.length() > 0) {
					if(s.charAt(0) == opQ && s.charAt(s.length()-1) == opQ) {
						s = s.length() == 2 ? "" : s.substring(1, s.length()-1);
					} else if(s.charAt(0) == opQ || s.charAt(s.length()-1) == opQ) {
						throw new RuntimeException("Error: Quote at beginning or end but not both. '" + s + "'");
					}
					ret.add(s);
				}
				
				//Skip ahead so the index is at the last whitespace in a row
				while(i < in.length()) {
					//If the next char is a white space set the next char to be the current char
					//If it is not a white space then stop. This way the index is set at the last whitespace so the for loop i++ will
					//increment the index to the next non-whitespace char.
					if(CharMatcher.WHITESPACE.matches(in.charAt(i+1)))
						i = i + 1;
					else 
						break;
				}
				//Start will be the next char over, won't cause issues setting this here because it will not enter this code block since
				//it is not a whitespace
				start = i + 1;
			}
		}
		if(inQ)
			throw new RuntimeException("Error: Unbalanced quotes. '" + in + "'");
		
		String s = in.substring(start, in.length()).trim();//Possible because the end will never contain white spaces
		if(s.length() > 0) {
			if(s.charAt(0) == opQ && s.charAt(s.length()-1) == opQ) {
				s = s.length() == 2 ? "" : s.substring(1, s.length()-1);
			} else if(s.charAt(0) == opQ || s.charAt(s.length()-1) == opQ) {
				throw new RuntimeException("Error: Quote at beginning or end but not both. '" + s + "'");
			}
			ret.add(s);
		}
		
		if(ret.isEmpty())
			throw new RuntimeException("Error: Nothing was parsed when input was not empty!?! + '" + in + "'");
		else if(ret.size() == 1)
			ret = Collections.singletonList(ret.get(0));
		return ret;
	}
	
	@SafeVarargs
	public static <A> Op<A> getAndOp(Op<A>... ops) { return getAndOp(Arrays.asList(ops)); }
	@SafeVarargs
	public static <A> Op<A> getOrOp(Op<A>... ops) { return getOrOp(Arrays.asList(ops)); }
	public static <A> Op<A> getAndOp(List<Op<A>> ops) { return new AndOp<A>(ops); }
	public static <A> Op<A> getOrOp(List<Op<A>> ops) { return new OrOp<A>(ops); }
	public static <A> Op<A> getNotOp(Op<A> op) { return new NotOp<A>(op); }
	
	public interface IDataType {
		
	}
	
}
