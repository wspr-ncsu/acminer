package org.sag.acminer.database.filter.matcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.sag.acminer.database.defusegraph.INode;

import soot.Value;
import soot.jimple.StringConstant;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("StringMatcher")
public class StringMatcher extends Matcher<String> {
	
	public static enum StringMatcherOpType {
		EQUAL("equal"),
		CONTAIN("contain"),
		STARTS_WITH("starts-with"),
		ENDS_WITH("ends-with"),
		REGEX("regex");
		private String val;
		private StringMatcherOpType(String val) { this.val = val; }
		public String toString() { return val; }
		public static StringMatcherOpType fromString(String str) {
			for(StringMatcherOpType t : StringMatcherOpType.values()) {
				if(t.val.equals(str))
					return t;
			}
			return null;
		}
	}
	
	public static final char opIgnoreCase = 'i';
	private static final Pattern typeP = Pattern.compile("^\\(\\s*("+StringMatcherOpType.EQUAL+"|"
			+StringMatcherOpType.CONTAIN+"|"+StringMatcherOpType.STARTS_WITH+"|"+StringMatcherOpType.ENDS_WITH+"|"
			+StringMatcherOpType.REGEX+")(?:_("+opIgnoreCase+")|)\\s+(.*)\\)$");
	
	public StringMatcher(Op<String> operation) {
		super(operation);
	}
	
	/** Takes in a formated value string representing all the ops for this matcher and produces a matcher 
	 * based on these ops.
	 */
	public StringMatcher(String value) {
		super(value);
	}
	
	public StringMatcher(String value, StringMatcherOpType type) {
		super(getOp(value,type,false));
	}
	
	public StringMatcher(String value, StringMatcherOpType type, boolean ic) {
		super(getOp(value,type,ic));
	}
	
	@Override
	protected Op<String> leafStringToOp(String s) {
		java.util.regex.Matcher m = typeP.matcher(s);
		if(m.matches()) {
			String type = m.group(1).trim();
			String ig = m.group(2);
			String val = m.group(3);
			if(val != null) {
				val = parseLeafArgs(val.trim()).get(0);
				boolean ignoreCase = false;
				if(ig != null && !ig.isEmpty() && ig.length() == 1 && ig.charAt(0) == opIgnoreCase)
					ignoreCase = true;
				return getOp(val, StringMatcherOpType.fromString(type), ignoreCase);
			}
			return null; //Does not have a value fail
		}
		return null; //Does not match pattern fail
	}
	
	@Override
	public boolean matcher(Object... objects) {
		if(objects == null || objects.length <= 0 || objects[0] == null)
			return false;
		Object u = objects[0];
		if(u instanceof String)
			return matches((String)u);
		else if(u instanceof StringConstant)
			return matches(((StringConstant)u).value);
		else if(u instanceof INode) {
			Value v = ((INode)u).getValue();
			if(v != null && v instanceof StringConstant)
				return matches(((StringConstant)v).value);
		}
		return false;
	}
	
	protected boolean matches(String s) {
		Objects.requireNonNull(s);
		//the option string makes sure the operations in this class are only ever accessed through this method
		//Basically its an origin thing because SootMatcher has special semantics for signature
		//in that we don't want a string signature to be run on any op other than a signature op if any exist
		return operation.matches(s); 
	}
	
	@Override
	public boolean equals(Object o) {
		if(super.equals(o)) {
			return o instanceof StringMatcher;
		}
		return false;
	}
	
	private static void verifyArgs(int size, String... args) {
		Objects.requireNonNull(args);
		if(args.length != size)
			throw new RuntimeException("Error: Expected " + size + " arguments and got " + args.length + ".");
		for(int i = 0; i < size; i++) {
			Objects.requireNonNull(args[i]);
		}
	}
	
	protected static class StringEqualsOp implements Op<String> {

		protected final String orgValue;
		protected final String value;
		protected final boolean ignoreCase;
		
		public StringEqualsOp(String value, boolean ignoreCase) {
			Objects.requireNonNull(value);
			this.orgValue = value;
			if(ignoreCase)
				this.value = value.toLowerCase();
			else
				this.value = value;
			this.ignoreCase = ignoreCase;
		}
		
		@Override
		public boolean matches(String... args) {
			verifyArgs(1, args);
			String in = args[0];
			if(ignoreCase)
				in = in.toLowerCase();
			return in.equals(value);
		}
		
		protected StringMatcherOpType getOpIdentifier() { return StringMatcherOpType.EQUAL; }
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(opB).append(getOpIdentifier());
			if(ignoreCase)
				sb.append("_").append(opIgnoreCase);
			sb.append(' ').append(quoteIfNeeded(orgValue)).append(opE);
			return sb.toString();
		}
		
	}
	
	protected static class StringContainsOp implements Op<String> {
		
		protected final String orgValue;
		protected final String value;
		protected final boolean ignoreCase;
		
		public StringContainsOp(String value, boolean ignoreCase) {
			Objects.requireNonNull(value);
			this.orgValue = value;
			if(ignoreCase)
				this.value = value.toLowerCase();
			else
				this.value = value;
			this.ignoreCase = ignoreCase;
		}
		
		@Override
		public boolean matches(String... args) {
			verifyArgs(1, args);
			String in = args[0];
			if(ignoreCase)
				in = in.toLowerCase();
			return in.contains(value);
		}
		
		protected StringMatcherOpType getOpIdentifier() { return StringMatcherOpType.CONTAIN; }
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(opB).append(getOpIdentifier());
			if(ignoreCase)
				sb.append("_").append(opIgnoreCase);
			sb.append(' ').append(quoteIfNeeded(orgValue)).append(opE);
			return sb.toString();
		}
		
	}
	
	protected static class StringStartsWithOp implements Op<String> {
		
		protected final String orgValue;
		protected final String value;
		protected final boolean ignoreCase;
		
		public StringStartsWithOp(String value, boolean ignoreCase) {
			Objects.requireNonNull(value);
			this.orgValue = value;
			if(ignoreCase)
				this.value = value.toLowerCase();
			else
				this.value = value;
			this.ignoreCase = ignoreCase;
		}
		
		@Override
		public boolean matches(String... args) {
			verifyArgs(1, args);
			String in = args[0];
			if(ignoreCase)
				in = in.toLowerCase();
			return in.startsWith(value);
		}
		
		protected StringMatcherOpType getOpIdentifier() { return StringMatcherOpType.STARTS_WITH; }
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(opB).append(getOpIdentifier());
			if(ignoreCase)
				sb.append("_").append(opIgnoreCase);
			sb.append(' ').append(quoteIfNeeded(orgValue)).append(opE);
			return sb.toString();
		}
		
	}
	
	protected static class StringEndsWithOp implements Op<String> {
		
		protected final String orgValue;
		protected final String value;
		protected final boolean ignoreCase;
		
		public StringEndsWithOp(String value, boolean ignoreCase) {
			Objects.requireNonNull(value);
			this.orgValue = value;
			if(ignoreCase)
				this.value = value.toLowerCase();
			else
				this.value = value;
			this.ignoreCase = ignoreCase;
		}
		
		@Override
		public boolean matches(String... args) {
			verifyArgs(1, args);
			String in = args[0];
			if(ignoreCase)
				in = in.toLowerCase();
			return in.endsWith(value);
		}
		
		protected StringMatcherOpType getOpIdentifier() { return StringMatcherOpType.ENDS_WITH; }
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(opB).append(getOpIdentifier());
			if(ignoreCase)
				sb.append("_").append(opIgnoreCase);
			sb.append(' ').append(quoteIfNeeded(orgValue)).append(opE);
			return sb.toString();
		}
		
	}
	
	protected static class StringRegexOp implements Op<String> {
		
		protected final String value;
		protected final boolean ignoreCase;
		protected final Pattern p;
		
		public StringRegexOp(String value, boolean ignoreCase) {
			Objects.requireNonNull(value);
			if(ignoreCase)
				p = Pattern.compile(value,Pattern.CASE_INSENSITIVE);
			else
				p = Pattern.compile(value);
			this.value = value;
			this.ignoreCase = ignoreCase;
		}
		
		@Override
		public boolean matches(String... args) {
			verifyArgs(1, args);
			String in = args[0];
			return p.matcher(in).find();
		}
		
		protected StringMatcherOpType getOpIdentifier() { return StringMatcherOpType.REGEX; }
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(opB).append(getOpIdentifier());
			if(ignoreCase)
				sb.append("_").append(opIgnoreCase);
			sb.append(' ').append(quoteIfNeeded(value)).append(opE);
			return sb.toString();
		}
		
	}
	
	public static Op<String> getEqualsOp(String value) { return getEqualsOp(value, false); }
	public static Op<String> getEqualsOp(String value, boolean ic) { return new StringEqualsOp(value, ic); }
	public static Op<String> getContainsOp(String value) { return getContainsOp(value, false); }
	public static Op<String> getContainsOp(String value, boolean ic) { return new StringContainsOp(value, ic); }
	public static Op<String> getStartsWithOp(String value) { return getStartsWithOp(value, false); }
	public static Op<String> getStartsWithOp(String value, boolean ic) { return new StringStartsWithOp(value, ic); }
	public static Op<String> getEndsWithOp(String value) { return getEndsWithOp(value, false); }
	public static Op<String> getEndsWithOp(String value, boolean ic) { return new StringEndsWithOp(value, ic); }
	public static Op<String> getRegexOp(String value) { return getRegexOp(value, false); }
	public static Op<String> getRegexOp(String value, boolean ic) { return new StringRegexOp(value, ic); }
	
	public static Op<String> getEqualsOrOp(String... values) { return getEqualsOrOp(false,values); }
	public static Op<String> getEqualsOrOp(boolean ic, String... values) { return getOp(MatcherOpType.OR, StringMatcherOpType.EQUAL, ic, values); }
	public static Op<String> getContainsOrOp(String... values) { return getContainsOrOp(false,values); }
	public static Op<String> getContainsOrOp(boolean ic, String... values) { return getOp(MatcherOpType.OR, StringMatcherOpType.CONTAIN, ic, values); }
	public static Op<String> getStartsWithOrOp(String... values) { return getStartsWithOrOp(false,values); }
	public static Op<String> getStartsWithOrOp(boolean ic, String... values) { return getOp(MatcherOpType.OR, StringMatcherOpType.STARTS_WITH, ic, values); }
	public static Op<String> getEndsWithOrOp(String... values) { return getEndsWithOrOp(false, values); }
	public static Op<String> getEndsWithOrOp(boolean ic, String... values) { return getOp(MatcherOpType.OR, StringMatcherOpType.ENDS_WITH, ic, values); }
	public static Op<String> getRegexOrOp(String... values) { return getRegexOrOp(false, values); }
	public static Op<String> getRegexOrOp(boolean ic, String... values) { return getOp(MatcherOpType.OR, StringMatcherOpType.REGEX, ic, values); }
	
	public static Op<String> getEqualsAndOp(String... values) { return getEqualsAndOp(false,values); }
	public static Op<String> getEqualsAndOp(boolean ic, String... values) { return getOp(MatcherOpType.AND, StringMatcherOpType.EQUAL, ic, values); }
	public static Op<String> getContainsAndOp(String... values) { return getContainsAndOp(false,values); }
	public static Op<String> getContainsAndOp(boolean ic, String... values) { return getOp(MatcherOpType.AND, StringMatcherOpType.CONTAIN, ic, values); }
	public static Op<String> getStartsWithAndOp(String... values) { return getStartsWithAndOp(false,values); }
	public static Op<String> getStartsWithAndOp(boolean ic, String... values) { return getOp(MatcherOpType.AND, StringMatcherOpType.STARTS_WITH, ic, values); }
	public static Op<String> getEndsWithAndOp(String... values) { return getEndsWithAndOp(false, values); }
	public static Op<String> getEndsWithAndOp(boolean ic, String... values) { return getOp(MatcherOpType.AND, StringMatcherOpType.ENDS_WITH, ic, values); }
	public static Op<String> getRegexAndOp(String... values) { return getRegexAndOp(false, values); }
	public static Op<String> getRegexAndOp(boolean ic, String... values) { return getOp(MatcherOpType.AND, StringMatcherOpType.REGEX, ic, values); }
	
	public static Op<String> getEqualsNotOp(String value) { return getEqualsNotOp(false,value); }
	public static Op<String> getEqualsNotOp(boolean ic, String value) { return getOp(MatcherOpType.NOT, StringMatcherOpType.EQUAL, ic, value); }
	public static Op<String> getContainsNotOp(String value) { return getContainsNotOp(false,value); }
	public static Op<String> getContainsNotOp(boolean ic, String value) { return getOp(MatcherOpType.NOT, StringMatcherOpType.CONTAIN, ic, value); }
	public static Op<String> getStartsWithNotOp(String value) { return getStartsWithNotOp(false,value); }
	public static Op<String> getStartsWithNotOp(boolean ic, String value) { return getOp(MatcherOpType.NOT, StringMatcherOpType.STARTS_WITH, ic, value); }
	public static Op<String> getEndsWithNotOp(String value) { return getEndsWithNotOp(false, value); }
	public static Op<String> getEndsWithNotOp(boolean ic, String value) { return getOp(MatcherOpType.NOT, StringMatcherOpType.ENDS_WITH, ic, value); }
	public static Op<String> getRegexNotOp(String value) { return getRegexNotOp(false, value); }
	public static Op<String> getRegexNotOp(boolean ic, String value) { return getOp(MatcherOpType.NOT, StringMatcherOpType.REGEX, ic, value); }
	
	protected static Op<String> getOp(String value, StringMatcherOpType type, boolean ic) {
		Objects.requireNonNull(value);
		Objects.requireNonNull(type);
		switch(type) {
			case EQUAL: return getEqualsOp(value, ic);
			case CONTAIN: return getContainsOp(value, ic);
			case STARTS_WITH: return getStartsWithOp(value, ic);
			case ENDS_WITH: return getEndsWithOp(value, ic);
			case REGEX: return getRegexOp(value, ic);
			default: return null;
		}
	}
	
	private static Op<String> getOp(MatcherOpType type, StringMatcherOpType type2, boolean ic, String... values) {
		Objects.requireNonNull(values);
		Objects.requireNonNull(type2);
		List<Op<String>> ops = new ArrayList<>();
		for(String value : values) {
			Objects.requireNonNull(value);
			ops.add(getOp(value, type2, ic));
		}
		switch(type) {
			case AND: return getAndOp(ops);
			case OR: return getOrOp(ops);
			case NOT: return getNotOp(ops.get(0));
			default: return null;
		}	
	}
	
}
