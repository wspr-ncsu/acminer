package org.sag.acminer.database.filter.matcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.primitives.Ints;
import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("SootMatcher")
public class SootMatcher extends Matcher<String> {
	
	public static enum SootMatcherOpType {
		SIGNATURE("signature"),
		
		EQUAL_NAME("equal-name"),
		CONTAIN_NAME("contain-name"),
		STARTS_WITH_NAME("starts-with-name"),
		ENDS_WITH_NAME("ends-with-name"),
		REGEX_NAME("regex-name"),
		
		EQUAL_FULL_CLASS("equal-full-class"),
		CONTAIN_FULL_CLASS("contain-full-class"),
		STARTS_WITH_FULL_CLASS("starts-with-full-class"),
		ENDS_WITH_FULL_CLASS("ends-with-full-class"),
		REGEX_FULL_CLASS("regex-full-class"),
		
		EQUAL_CLASS("equal-class"),
		CONTAIN_CLASS("contain-class"),
		STARTS_WITH_CLASS("starts-with-class"),
		ENDS_WITH_CLASS("ends-with-class"),
		REGEX_CLASS("regex-class"),
		
		EQUAL_PACKAGE("equal-package"),
		CONTAIN_PACKAGE("contain-package"),
		STARTS_WITH_PACKAGE("starts-with-package"),
		ENDS_WITH_PACKAGE("ends-with-package"),
		REGEX_PACKAGE("regex-package"),
		
		REGEX_NAME_WORDS("regex-name-words"),
		REGEX_CLASS_WORDS("regex-class-words");
		private String val;
		private SootMatcherOpType(String val) { this.val = val; }
		public String toString() { return val; }
		public static SootMatcherOpType fromString(String str) {
			for(SootMatcherOpType t : SootMatcherOpType.values()) {
				if(t.val.equals(str))
					return t;
			}
			return null;
		}
	}
	
	public static final char opIgnoreCase = 'i';
	
	private static final Pattern typeP = Pattern.compile("^\\(\\s*("+SootMatcherOpType.SIGNATURE+"|"+SootMatcherOpType.EQUAL_NAME+"|"+SootMatcherOpType.CONTAIN_NAME+"|"
															+SootMatcherOpType.STARTS_WITH_NAME+"|"+SootMatcherOpType.ENDS_WITH_NAME+"|"+SootMatcherOpType.REGEX_NAME+"|"
															+SootMatcherOpType.EQUAL_FULL_CLASS+"|"+SootMatcherOpType.CONTAIN_FULL_CLASS+"|"+SootMatcherOpType.STARTS_WITH_FULL_CLASS+"|"
															+SootMatcherOpType.ENDS_WITH_FULL_CLASS+"|"+SootMatcherOpType.REGEX_FULL_CLASS+"|"+SootMatcherOpType.EQUAL_CLASS+"|"
															+SootMatcherOpType.CONTAIN_CLASS+"|"+SootMatcherOpType.STARTS_WITH_CLASS+"|"+SootMatcherOpType.ENDS_WITH_CLASS+"|"
															+SootMatcherOpType.REGEX_CLASS+"|"+SootMatcherOpType.EQUAL_PACKAGE+"|"+SootMatcherOpType.CONTAIN_PACKAGE+"|"
															+SootMatcherOpType.STARTS_WITH_PACKAGE+"|"+SootMatcherOpType.ENDS_WITH_PACKAGE+"|"+SootMatcherOpType.REGEX_PACKAGE+"|"
															+SootMatcherOpType.REGEX_NAME_WORDS+"|"+SootMatcherOpType.REGEX_CLASS_WORDS
															+")(?:_("+opIgnoreCase+")|)\\s+(.*)\\)$");
	
	public SootMatcher(Op<String> operation) {
		super(operation);
	}
	
	public SootMatcher(String value) {
		super(value);
	}
	
	public SootMatcher(SootMatcherOpType type, String... values) {
		super(getOp(type,false,values));
	}
	
	public SootMatcher(SootMatcherOpType type, boolean ic, String... values) {
		super(getOp(type,ic,values));
	}
	
	@Override
	protected Op<String> leafStringToOp(String s) {
		java.util.regex.Matcher m = typeP.matcher(s);
		if(m.matches()) {
			String type = m.group(1).trim();
			String ig = m.group(2);
			String val = m.group(3);
			if(val != null) {
				String[] args = parseLeafArgs(val.trim()).toArray(new String[0]);
				boolean ignoreCase = false;
				if(ig != null && !ig.isEmpty() && ig.length() == 1 && ig.charAt(0) == opIgnoreCase)
					ignoreCase = true;
				return getOp(SootMatcherOpType.fromString(type), ignoreCase, args);
			}
			return null;
		}
		return null;
	}
	
	@Override
	public boolean matcher(Object... objects) {
		if(objects == null || objects.length <= 4 || objects[0] == null || !(objects[0] instanceof String) 
				|| objects[1] == null || !(objects[1] instanceof String)
				|| objects[2] == null || !(objects[2] instanceof String)
				|| objects[3] == null || !(objects[3] instanceof String)
				|| objects[4] == null || !(objects[4] instanceof String))
			return false;
		return matches((String)objects[0], (String)objects[1], (String)objects[2], (String)objects[3], (String)objects[4]);
	}
	
	protected boolean matches(String signature, String name, String packageName, String className, String fullClassName) {
		Objects.requireNonNull(signature);
		Objects.requireNonNull(name);
		Objects.requireNonNull(packageName);
		Objects.requireNonNull(className);
		Objects.requireNonNull(fullClassName);
		return operation.matches(signature, name, packageName, className, fullClassName);
	}
	
	@Override
	public boolean equals(Object o) {
		if(super.equals(o)) {
			return o instanceof SootMatcher;
		}
		return false;
	}
	
	private static abstract class SootMatcherOp implements Op<String> {
		protected static void verifyArgs(int size, String... args) {
			Objects.requireNonNull(args);
			if(args.length != size)
				throw new RuntimeException("Error: Expected " + size + " arguments and got " + args.length + ".");
			for(int i = 0; i < size; i++) {
				Objects.requireNonNull(args[i]);
			}
		}
		protected abstract SootMatcherOpType getOpIdentifier();
	}
	
	private static abstract class EqualsOp extends SootMatcherOp {

		protected final String orgValue;
		protected final String value;
		protected final boolean ignoreCase;
		
		public EqualsOp(String value, boolean ignoreCase) {
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
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(opB).append(getOpIdentifier());
			if(ignoreCase)
				sb.append("_").append(opIgnoreCase);
			sb.append(' ').append(quoteIfNeeded(orgValue)).append(opE);
			return sb.toString();
		}
		
	}
	
	protected static abstract class ContainsOp extends SootMatcherOp {
		
		protected final String orgValue;
		protected final String value;
		protected final boolean ignoreCase;
		
		public ContainsOp(String value, boolean ignoreCase) {
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
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(opB).append(getOpIdentifier());
			if(ignoreCase)
				sb.append("_").append(opIgnoreCase);
			sb.append(' ').append(quoteIfNeeded(orgValue)).append(opE);
			return sb.toString();
		}
		
	}
	
	protected static abstract class StartsWithOp extends SootMatcherOp {
		
		protected final String orgValue;
		protected final String value;
		protected final boolean ignoreCase;
		
		public StartsWithOp(String value, boolean ignoreCase) {
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
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(opB).append(getOpIdentifier());
			if(ignoreCase)
				sb.append("_").append(opIgnoreCase);
			sb.append(' ').append(quoteIfNeeded(orgValue)).append(opE);
			return sb.toString();
		}
		
	}
	
	protected static abstract class EndsWithOp extends SootMatcherOp {
		
		protected final String orgValue;
		protected final String value;
		protected final boolean ignoreCase;
		
		public EndsWithOp(String value, boolean ignoreCase) {
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
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(opB).append(getOpIdentifier());
			if(ignoreCase)
				sb.append("_").append(opIgnoreCase);
			sb.append(' ').append(quoteIfNeeded(orgValue)).append(opE);
			return sb.toString();
		}
		
	}
	
	protected static abstract class RegexOp extends SootMatcherOp {
		
		protected final String value;
		protected final boolean ignoreCase;
		protected final Pattern p;
		
		public RegexOp(String value, boolean ignoreCase) {
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
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(opB).append(getOpIdentifier());
			if(ignoreCase)
				sb.append("_").append(opIgnoreCase);
			sb.append(' ').append(quoteIfNeeded(value)).append(opE);
			return sb.toString();
		}
		
	}
	
	private static class NameEqualOp extends EqualsOp {
		public NameEqualOp(String value, boolean ignoreCase) { super(value, ignoreCase); }
		@Override public boolean matches(String... args) { verifyArgs(5, args); return super.matches(args[1]); }
		@Override protected SootMatcherOpType getOpIdentifier() { return SootMatcherOpType.EQUAL_NAME; }
	}
	
	private static class NameContainOp extends ContainsOp {
		public NameContainOp(String value, boolean ignoreCase) { super(value, ignoreCase); }
		@Override public boolean matches(String... args) { verifyArgs(5, args); return super.matches(args[1]); }
		@Override protected SootMatcherOpType getOpIdentifier() { return SootMatcherOpType.CONTAIN_NAME; }
	}
	
	private static class NameStartsWithOp extends StartsWithOp {
		public NameStartsWithOp(String value, boolean ignoreCase) { super(value, ignoreCase); }
		@Override public boolean matches(String... args) { verifyArgs(5, args); return super.matches(args[1]); }
		@Override protected SootMatcherOpType getOpIdentifier() { return SootMatcherOpType.STARTS_WITH_NAME; }
	}
	
	private static class NameEndsWithOp extends EndsWithOp {
		public NameEndsWithOp(String value, boolean ignoreCase) { super(value, ignoreCase); }
		@Override public boolean matches(String... args) { verifyArgs(5, args); return super.matches(args[1]); }
		@Override protected SootMatcherOpType getOpIdentifier() { return SootMatcherOpType.ENDS_WITH_NAME; }
	}
	
	private static class NameRegexOp extends RegexOp {
		public NameRegexOp(String value, boolean ignoreCase) { super(value, ignoreCase); }
		@Override public boolean matches(String... args) { verifyArgs(5, args); return super.matches(args[1]); }
		@Override protected SootMatcherOpType getOpIdentifier() { return SootMatcherOpType.REGEX_NAME; }
	}
	
	private static class FullClassEqualOp extends EqualsOp {
		public FullClassEqualOp(String value, boolean ignoreCase) { super(value, ignoreCase); }
		@Override public boolean matches(String... args) { verifyArgs(5, args); return super.matches(args[4]); }
		@Override protected SootMatcherOpType getOpIdentifier() { return SootMatcherOpType.EQUAL_FULL_CLASS; }
	}
	
	private static class FullClassContainOp extends ContainsOp {
		public FullClassContainOp(String value, boolean ignoreCase) { super(value, ignoreCase); }
		@Override public boolean matches(String... args) { verifyArgs(5, args); return super.matches(args[4]); }
		@Override protected SootMatcherOpType getOpIdentifier() { return SootMatcherOpType.CONTAIN_FULL_CLASS; }
	}
	
	private static class FullClassStartsWithOp extends StartsWithOp {
		public FullClassStartsWithOp(String value, boolean ignoreCase) { super(value, ignoreCase); }
		@Override public boolean matches(String... args) { verifyArgs(5, args); return super.matches(args[4]); }
		@Override protected SootMatcherOpType getOpIdentifier() { return SootMatcherOpType.STARTS_WITH_FULL_CLASS; }
	}
	
	private static class FullClassEndsWithOp extends EndsWithOp {
		public FullClassEndsWithOp(String value, boolean ignoreCase) { super(value, ignoreCase); }
		@Override public boolean matches(String... args) { verifyArgs(5, args); return super.matches(args[4]); }
		@Override protected SootMatcherOpType getOpIdentifier() { return SootMatcherOpType.ENDS_WITH_FULL_CLASS; }
	}
	
	private static class FullClassRegexOp extends RegexOp {
		public FullClassRegexOp(String value, boolean ignoreCase) { super(value, ignoreCase); }
		@Override public boolean matches(String... args) { verifyArgs(5, args); return super.matches(args[4]); }
		@Override protected SootMatcherOpType getOpIdentifier() { return SootMatcherOpType.REGEX_FULL_CLASS; }
	}
	
	private static class ClassEqualOp extends EqualsOp {
		public ClassEqualOp(String value, boolean ignoreCase) { super(value, ignoreCase); }
		@Override public boolean matches(String... args) { verifyArgs(5, args); return super.matches(args[3]); }
		@Override protected SootMatcherOpType getOpIdentifier() { return SootMatcherOpType.EQUAL_CLASS; }
	}
	
	private static class ClassContainOp extends ContainsOp {
		public ClassContainOp(String value, boolean ignoreCase) { super(value, ignoreCase); }
		@Override public boolean matches(String... args) { verifyArgs(5, args); return super.matches(args[3]); }
		@Override protected SootMatcherOpType getOpIdentifier() { return SootMatcherOpType.CONTAIN_CLASS; }
	}
	
	private static class ClassStartsWithOp extends StartsWithOp {
		public ClassStartsWithOp(String value, boolean ignoreCase) { super(value, ignoreCase); }
		@Override public boolean matches(String... args) { verifyArgs(5, args); return super.matches(args[3]); }
		@Override protected SootMatcherOpType getOpIdentifier() { return SootMatcherOpType.STARTS_WITH_CLASS; }
	}
	
	private static class ClassEndsWithOp extends EndsWithOp {
		public ClassEndsWithOp(String value, boolean ignoreCase) { super(value, ignoreCase); }
		@Override public boolean matches(String... args) { verifyArgs(5, args); return super.matches(args[3]); }
		@Override protected SootMatcherOpType getOpIdentifier() { return SootMatcherOpType.ENDS_WITH_CLASS; }
	}
	
	private static class ClassRegexOp extends RegexOp {
		public ClassRegexOp(String value, boolean ignoreCase) { super(value, ignoreCase); }
		@Override public boolean matches(String... args) { verifyArgs(5, args); return super.matches(args[3]); }
		@Override protected SootMatcherOpType getOpIdentifier() { return SootMatcherOpType.REGEX_CLASS; }
	}
	
	private static class PackageEqualOp extends EqualsOp {
		public PackageEqualOp(String value, boolean ignoreCase) { super(value, ignoreCase); }
		@Override public boolean matches(String... args) { verifyArgs(5, args); return super.matches(args[2]); }
		@Override protected SootMatcherOpType getOpIdentifier() { return SootMatcherOpType.EQUAL_PACKAGE; }
	}
	
	private static class PackageContainOp extends ContainsOp {
		public PackageContainOp(String value, boolean ignoreCase) { super(value, ignoreCase); }
		@Override public boolean matches(String... args) { verifyArgs(5, args); return super.matches(args[2]); }
		@Override protected SootMatcherOpType getOpIdentifier() { return SootMatcherOpType.CONTAIN_PACKAGE; }
	}
	
	private static class PackageStartsWithOp extends StartsWithOp {
		public PackageStartsWithOp(String value, boolean ignoreCase) { super(value, ignoreCase); }
		@Override public boolean matches(String... args) { verifyArgs(5, args); return super.matches(args[2]); }
		@Override protected SootMatcherOpType getOpIdentifier() { return SootMatcherOpType.STARTS_WITH_PACKAGE; }
	}
	
	private static class PackageEndsWithOp extends EndsWithOp {
		public PackageEndsWithOp(String value, boolean ignoreCase) { super(value, ignoreCase); }
		@Override public boolean matches(String... args) { verifyArgs(5, args); return super.matches(args[2]); }
		@Override protected SootMatcherOpType getOpIdentifier() { return SootMatcherOpType.ENDS_WITH_PACKAGE; }
	}
	
	private static class PackageRegexOp extends RegexOp {
		public PackageRegexOp(String value, boolean ignoreCase) { super(value, ignoreCase); }
		@Override public boolean matches(String... args) { verifyArgs(5, args); return super.matches(args[2]); }
		@Override protected SootMatcherOpType getOpIdentifier() { return SootMatcherOpType.REGEX_PACKAGE; }
	}
	
	private static class NameRegexWordsOp extends RegexOp {
		public NameRegexWordsOp(String value) {
			super(value, false);//We just will always ignore the case by design here
		}
		@Override
		public boolean matches(String... args) {
			verifyArgs(5, args);
			String in = args[1];
			return p.matcher(splitWords(in)).find();
		}
		@Override protected SootMatcherOpType getOpIdentifier() { return SootMatcherOpType.REGEX_NAME_WORDS; }
	}
	
	//Note indexes increase from the inner most class so 0 means the inner most class and negative means match any of the inner classes
	private static class ClassRegexWordsOp extends RegexOp {
		protected final int index;
		public ClassRegexWordsOp(String value) {
			this(value, -1);
		}
		public ClassRegexWordsOp(String value, int index) {
			super(value, false);//We just will always ignore the case by design here
			this.index = index;
		}
		
		@Override
		public boolean matches(String... args) {
			verifyArgs(5, args);
			String in = args[3];
			List<String> arr = splitClassName(in);
			if(index < 0) {
				for(String s : arr) {
					if(p.matcher(s).find())
						return true;
				}
				return false;
			} else {
				if(arr.size() > index) {
					return p.matcher(arr.get(index)).find();
				}
				return false;
			}
		}
		
		@Override protected SootMatcherOpType getOpIdentifier() { return SootMatcherOpType.REGEX_CLASS_WORDS; }
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(opB).append(getOpIdentifier());
			if(ignoreCase)
				sb.append("_").append(opIgnoreCase);
			sb.append(' ').append(quoteIfNeeded(value));
			if(index >= 0)
				sb.append(' ').append(index);
			sb.append(opE);
			return sb.toString();
		}
	}
	
	private static final Pattern camelCasePattern = Pattern.compile(".+?(?:(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|$)");
	public static final String splitWords(String in) {
		String val = in;
		if(val.indexOf('_') >= 0) {
			val = Joiner.on(' ').join(Splitter.on('_').split(val)).toLowerCase().trim();
		} else {
			java.util.regex.Matcher m = camelCasePattern.matcher(val);
			StringBuilder sb = new StringBuilder();
			while(m.find())
				sb.append(m.group().toLowerCase()).append(' ');
			val = sb.toString().trim();
		}
		return val;
	}
	
	public static final List<String> splitClassName(String className) {
		List<String> ret = new ArrayList<>();
		String[] arr = className.split("\\$");
		for(int i = arr.length - 1; i >= 0; i--) {
			ret.add(splitWords(arr[i]));
		}
		return ret;
	}

	protected static class SignatureOp extends EqualsOp {
		public SignatureOp(String value) { super(value, false); }
		@Override public boolean matches(String... args) { verifyArgs(5, args); return super.matches(args[0]); }
		@Override protected SootMatcherOpType getOpIdentifier() { return SootMatcherOpType.SIGNATURE; }
	}
	
	public static SignatureOp getSignatureOp(String value) { return new SignatureOp(value); }
	public static NameRegexWordsOp getNameRegexWordsOp(String value) { return new NameRegexWordsOp(value); }
	public static ClassRegexWordsOp getClassRegexWordsOp(String value) { return new ClassRegexWordsOp(value); }
	public static ClassRegexWordsOp getClassRegexWordsOp(String value, int i) { return new ClassRegexWordsOp(value, i); }
	public static NameEqualOp getNameEqualOp(String value) { return getNameEqualOp(value, false); }
	public static NameEqualOp getNameEqualOp(String value, boolean ic) { return new NameEqualOp(value, ic); }
	public static NameContainOp getNameContainOp(String value) { return getNameContainOp(value, false); }
	public static NameContainOp getNameContainOp(String value, boolean ic) { return new NameContainOp(value, ic); }
	public static NameStartsWithOp getNameStartsWithOp(String value) { return getNameStartsWithOp(value, false); }
	public static NameStartsWithOp getNameStartsWithOp(String value, boolean ic) { return new NameStartsWithOp(value, ic); }
	public static NameEndsWithOp getNameEndsWithOp(String value) { return getNameEndsWithOp(value, false); }
	public static NameEndsWithOp getNameEndsWithOp(String value, boolean ic) { return new NameEndsWithOp(value, ic); }
	public static NameRegexOp getNameRegexOp(String value) { return getNameRegexOp(value, false); }
	public static NameRegexOp getNameRegexOp(String value, boolean ic) { return new NameRegexOp(value, ic); }
	public static FullClassEqualOp getFullClassEqualOp(String value) { return getFullClassEqualOp(value, false); }
	public static FullClassEqualOp getFullClassEqualOp(String value, boolean ic) { return new FullClassEqualOp(value, ic); }
	public static FullClassContainOp getFullClassContainOp(String value) { return getFullClassContainOp(value, false); }
	public static FullClassContainOp getFullClassContainOp(String value, boolean ic) { return new FullClassContainOp(value, ic); }
	public static FullClassStartsWithOp getFullClassStartsWithOp(String value) { return getFullClassStartsWithOp(value, false); }
	public static FullClassStartsWithOp getFullClassStartsWithOp(String value, boolean ic) { return new FullClassStartsWithOp(value, ic); }
	public static FullClassEndsWithOp getFullClassEndsWithOp(String value) { return getFullClassEndsWithOp(value, false); }
	public static FullClassEndsWithOp getFullClassEndsWithOp(String value, boolean ic) { return new FullClassEndsWithOp(value, ic); }
	public static FullClassRegexOp getFullClassRegexOp(String value) { return getFullClassRegexOp(value, false); }
	public static FullClassRegexOp getFullClassRegexOp(String value, boolean ic) { return new FullClassRegexOp(value, ic); }
	public static ClassEqualOp getClassEqualOp(String value) { return getClassEqualOp(value, false); }
	public static ClassEqualOp getClassEqualOp(String value, boolean ic) { return new ClassEqualOp(value, ic); }
	public static ClassContainOp getClassContainOp(String value) { return getClassContainOp(value, false); }
	public static ClassContainOp getClassContainOp(String value, boolean ic) { return new ClassContainOp(value, ic); }
	public static ClassStartsWithOp getClassStartsWithOp(String value) { return getClassStartsWithOp(value, false); }
	public static ClassStartsWithOp getClassStartsWithOp(String value, boolean ic) { return new ClassStartsWithOp(value, ic); }
	public static ClassEndsWithOp getClassEndsWithOp(String value) { return getClassEndsWithOp(value, false); }
	public static ClassEndsWithOp getClassEndsWithOp(String value, boolean ic) { return new ClassEndsWithOp(value, ic); }
	public static ClassRegexOp getClassRegexOp(String value) { return getClassRegexOp(value, false); }
	public static ClassRegexOp getClassRegexOp(String value, boolean ic) { return new ClassRegexOp(value, ic); }
	public static PackageEqualOp getPackageEqualOp(String value) { return getPackageEqualOp(value, false); }
	public static PackageEqualOp getPackageEqualOp(String value, boolean ic) { return new PackageEqualOp(value, ic); }
	public static PackageContainOp getPackageContainOp(String value) { return getPackageContainOp(value, false); }
	public static PackageContainOp getPackageContainOp(String value, boolean ic) { return new PackageContainOp(value, ic); }
	public static PackageStartsWithOp getPackageStartsWithOp(String value) { return getPackageStartsWithOp(value, false); }
	public static PackageStartsWithOp getPackageStartsWithOp(String value, boolean ic) { return new PackageStartsWithOp(value, ic); }
	public static PackageEndsWithOp getPackageEndsWithOp(String value) { return getPackageEndsWithOp(value, false); }
	public static PackageEndsWithOp getPackageEndsWithOp(String value, boolean ic) { return new PackageEndsWithOp(value, ic); }
	public static PackageRegexOp getPackageRegexOp(String value) { return getPackageRegexOp(value, false); }
	public static PackageRegexOp getPackageRegexOp(String value, boolean ic) { return new PackageRegexOp(value, ic); }
	
	public static Op<String> getSignatureOrOp(String... values) { return getOp(MatcherOpType.OR, SootMatcherOpType.SIGNATURE, false, values); }
	public static Op<String> getNameRegexWordsOrOp(String... values) { return getOp(MatcherOpType.OR, SootMatcherOpType.REGEX_NAME_WORDS, false, values); }
	public static Op<String> getClassRegexWordsOrOp(String... values) { return getOp(MatcherOpType.OR, SootMatcherOpType.REGEX_CLASS_WORDS, false, values); }
	public static Op<String> getNameEqualOrOp(String... values) { return getNameEqualOrOp(false,values); }
	public static Op<String> getNameEqualOrOp(boolean ic, String... values) { return getOp(MatcherOpType.OR, SootMatcherOpType.EQUAL_NAME, ic, values); }
	public static Op<String> getNameContainOrOp(String... values) { return getNameContainOrOp(false,values); }
	public static Op<String> getNameContainOrOp(boolean ic, String... values) { return getOp(MatcherOpType.OR, SootMatcherOpType.CONTAIN_NAME, ic, values); }
	public static Op<String> getNameStartsWithOrOp(String... values) { return getNameStartsWithOrOp(false,values); }
	public static Op<String> getNameStartsWithOrOp(boolean ic, String... values) { return getOp(MatcherOpType.OR, SootMatcherOpType.STARTS_WITH_NAME, ic, values); }
	public static Op<String> getNameEndsWithOrOp(String... values) { return getNameEndsWithOrOp(false, values); }
	public static Op<String> getNameEndsWithOrOp(boolean ic, String... values) { return getOp(MatcherOpType.OR, SootMatcherOpType.ENDS_WITH_NAME, ic, values); }
	public static Op<String> getNameRegexOrOp(String... values) { return getNameRegexOrOp(false, values); }
	public static Op<String> getNameRegexOrOp(boolean ic, String... values) { return getOp(MatcherOpType.OR, SootMatcherOpType.REGEX_NAME, ic, values); }
	public static Op<String> getFullClassEqualOrOp(String... values) { return getFullClassEqualOrOp(false,values); }
	public static Op<String> getFullClassEqualOrOp(boolean ic, String... values) { return getOp(MatcherOpType.OR, SootMatcherOpType.EQUAL_FULL_CLASS, ic, values); }
	public static Op<String> getFullClassContainOrOp(String... values) { return getFullClassContainOrOp(false,values); }
	public static Op<String> getFullClassContainOrOp(boolean ic, String... values) { return getOp(MatcherOpType.OR, SootMatcherOpType.CONTAIN_FULL_CLASS, ic, values); }
	public static Op<String> getFullClassStartsWithOrOp(String... values) { return getFullClassStartsWithOrOp(false,values); }
	public static Op<String> getFullClassStartsWithOrOp(boolean ic, String... values) { return getOp(MatcherOpType.OR, SootMatcherOpType.STARTS_WITH_FULL_CLASS, ic, values); }
	public static Op<String> getFullClassEndsWithOrOp(String... values) { return getFullClassEndsWithOrOp(false, values); }
	public static Op<String> getFullClassEndsWithOrOp(boolean ic, String... values) { return getOp(MatcherOpType.OR, SootMatcherOpType.ENDS_WITH_FULL_CLASS, ic, values); }
	public static Op<String> getFullClassRegexOrOp(String... values) { return getFullClassRegexOrOp(false, values); }
	public static Op<String> getFullClassRegexOrOp(boolean ic, String... values) { return getOp(MatcherOpType.OR, SootMatcherOpType.REGEX_FULL_CLASS, ic, values); }
	public static Op<String> getClassEqualOrOp(String... values) { return getClassEqualOrOp(false,values); }
	public static Op<String> getClassEqualOrOp(boolean ic, String... values) { return getOp(MatcherOpType.OR, SootMatcherOpType.EQUAL_CLASS, ic, values); }
	public static Op<String> getClassContainOrOp(String... values) { return getClassContainOrOp(false,values); }
	public static Op<String> getClassContainOrOp(boolean ic, String... values) { return getOp(MatcherOpType.OR, SootMatcherOpType.CONTAIN_CLASS, ic, values); }
	public static Op<String> getClassStartsWithOrOp(String... values) { return getClassStartsWithOrOp(false,values); }
	public static Op<String> getClassStartsWithOrOp(boolean ic, String... values) { return getOp(MatcherOpType.OR, SootMatcherOpType.STARTS_WITH_CLASS, ic, values); }
	public static Op<String> getClassEndsWithOrOp(String... values) { return getClassEndsWithOrOp(false, values); }
	public static Op<String> getClassEndsWithOrOp(boolean ic, String... values) { return getOp(MatcherOpType.OR, SootMatcherOpType.ENDS_WITH_CLASS, ic, values); }
	public static Op<String> getClassRegexOrOp(String... values) { return getClassRegexOrOp(false, values); }
	public static Op<String> getClassRegexOrOp(boolean ic, String... values) { return getOp(MatcherOpType.OR, SootMatcherOpType.REGEX_CLASS, ic, values); }
	public static Op<String> getPackageEqualOrOp(String... values) { return getPackageEqualOrOp(false,values); }
	public static Op<String> getPackageEqualOrOp(boolean ic, String... values) { return getOp(MatcherOpType.OR, SootMatcherOpType.EQUAL_PACKAGE, ic, values); }
	public static Op<String> getPackageContainOrOp(String... values) { return getPackageContainOrOp(false,values); }
	public static Op<String> getPackageContainOrOp(boolean ic, String... values) { return getOp(MatcherOpType.OR, SootMatcherOpType.CONTAIN_PACKAGE, ic, values); }
	public static Op<String> getPackageStartsWithOrOp(String... values) { return getPackageStartsWithOrOp(false,values); }
	public static Op<String> getPackageStartsWithOrOp(boolean ic, String... values) { return getOp(MatcherOpType.OR, SootMatcherOpType.STARTS_WITH_PACKAGE, ic, values); }
	public static Op<String> getPackageEndsWithOrOp(String... values) { return getPackageEndsWithOrOp(false, values); }
	public static Op<String> getPackageEndsWithOrOp(boolean ic, String... values) { return getOp(MatcherOpType.OR, SootMatcherOpType.ENDS_WITH_PACKAGE, ic, values); }
	public static Op<String> getPackageRegexOrOp(String... values) { return getPackageRegexOrOp(false, values); }
	public static Op<String> getPackageRegexOrOp(boolean ic, String... values) { return getOp(MatcherOpType.OR, SootMatcherOpType.REGEX_PACKAGE, ic, values); }
	
	public static Op<String> getSignatureAndOp(String... values) { return getOp(MatcherOpType.AND, SootMatcherOpType.SIGNATURE, false, values); }
	public static Op<String> getNameRegexWordsAndOp(String... values) { return getOp(MatcherOpType.AND, SootMatcherOpType.REGEX_NAME_WORDS, false, values); }
	public static Op<String> getClassRegexWordsAndOp(String... values) { return getOp(MatcherOpType.AND, SootMatcherOpType.REGEX_CLASS_WORDS, false, values); }
	public static Op<String> getNameEqualAndOp(String... values) { return getNameEqualAndOp(false,values); }
	public static Op<String> getNameEqualAndOp(boolean ic, String... values) { return getOp(MatcherOpType.AND, SootMatcherOpType.EQUAL_NAME, ic, values); }
	public static Op<String> getNameContainAndOp(String... values) { return getNameContainAndOp(false,values); }
	public static Op<String> getNameContainAndOp(boolean ic, String... values) { return getOp(MatcherOpType.AND, SootMatcherOpType.CONTAIN_NAME, ic, values); }
	public static Op<String> getNameStartsWithAndOp(String... values) { return getNameStartsWithAndOp(false,values); }
	public static Op<String> getNameStartsWithAndOp(boolean ic, String... values) { return getOp(MatcherOpType.AND, SootMatcherOpType.STARTS_WITH_NAME, ic, values); }
	public static Op<String> getNameEndsWithAndOp(String... values) { return getNameEndsWithAndOp(false, values); }
	public static Op<String> getNameEndsWithAndOp(boolean ic, String... values) { return getOp(MatcherOpType.AND, SootMatcherOpType.ENDS_WITH_NAME, ic, values); }
	public static Op<String> getNameRegexAndOp(String... values) { return getNameRegexAndOp(false, values); }
	public static Op<String> getNameRegexAndOp(boolean ic, String... values) { return getOp(MatcherOpType.AND, SootMatcherOpType.REGEX_NAME, ic, values); }
	public static Op<String> getFullClassEqualAndOp(String... values) { return getFullClassEqualAndOp(false,values); }
	public static Op<String> getFullClassEqualAndOp(boolean ic, String... values) { return getOp(MatcherOpType.AND, SootMatcherOpType.EQUAL_FULL_CLASS, ic, values); }
	public static Op<String> getFullClassContainAndOp(String... values) { return getFullClassContainAndOp(false,values); }
	public static Op<String> getFullClassContainAndOp(boolean ic, String... values) { return getOp(MatcherOpType.AND, SootMatcherOpType.CONTAIN_FULL_CLASS, ic, values); }
	public static Op<String> getFullClassStartsWithAndOp(String... values) { return getFullClassStartsWithAndOp(false,values); }
	public static Op<String> getFullClassStartsWithAndOp(boolean ic, String... values) { return getOp(MatcherOpType.AND, SootMatcherOpType.STARTS_WITH_FULL_CLASS, ic, values); }
	public static Op<String> getFullClassEndsWithAndOp(String... values) { return getFullClassEndsWithAndOp(false, values); }
	public static Op<String> getFullClassEndsWithAndOp(boolean ic, String... values) { return getOp(MatcherOpType.AND, SootMatcherOpType.ENDS_WITH_FULL_CLASS, ic, values); }
	public static Op<String> getFullClassRegexAndOp(String... values) { return getFullClassRegexAndOp(false, values); }
	public static Op<String> getFullClassRegexAndOp(boolean ic, String... values) { return getOp(MatcherOpType.AND, SootMatcherOpType.REGEX_FULL_CLASS, ic, values); }
	public static Op<String> getClassEqualAndOp(String... values) { return getClassEqualAndOp(false,values); }
	public static Op<String> getClassEqualAndOp(boolean ic, String... values) { return getOp(MatcherOpType.AND, SootMatcherOpType.EQUAL_CLASS, ic, values); }
	public static Op<String> getClassContainAndOp(String... values) { return getClassContainAndOp(false,values); }
	public static Op<String> getClassContainAndOp(boolean ic, String... values) { return getOp(MatcherOpType.AND, SootMatcherOpType.CONTAIN_CLASS, ic, values); }
	public static Op<String> getClassStartsWithAndOp(String... values) { return getClassStartsWithAndOp(false,values); }
	public static Op<String> getClassStartsWithAndOp(boolean ic, String... values) { return getOp(MatcherOpType.AND, SootMatcherOpType.STARTS_WITH_CLASS, ic, values); }
	public static Op<String> getClassEndsWithAndOp(String... values) { return getClassEndsWithAndOp(false, values); }
	public static Op<String> getClassEndsWithAndOp(boolean ic, String... values) { return getOp(MatcherOpType.AND, SootMatcherOpType.ENDS_WITH_CLASS, ic, values); }
	public static Op<String> getClassRegexAndOp(String... values) { return getClassRegexAndOp(false, values); }
	public static Op<String> getClassRegexAndOp(boolean ic, String... values) { return getOp(MatcherOpType.AND, SootMatcherOpType.REGEX_CLASS, ic, values); }
	public static Op<String> getPackageEqualAndOp(String... values) { return getPackageEqualAndOp(false,values); }
	public static Op<String> getPackageEqualAndOp(boolean ic, String... values) { return getOp(MatcherOpType.AND, SootMatcherOpType.EQUAL_PACKAGE, ic, values); }
	public static Op<String> getPackageContainAndOp(String... values) { return getPackageContainAndOp(false,values); }
	public static Op<String> getPackageContainAndOp(boolean ic, String... values) { return getOp(MatcherOpType.AND, SootMatcherOpType.CONTAIN_PACKAGE, ic, values); }
	public static Op<String> getPackageStartsWithAndOp(String... values) { return getPackageStartsWithAndOp(false,values); }
	public static Op<String> getPackageStartsWithAndOp(boolean ic, String... values) { return getOp(MatcherOpType.AND, SootMatcherOpType.STARTS_WITH_PACKAGE, ic, values); }
	public static Op<String> getPackageEndsWithAndOp(String... values) { return getPackageEndsWithAndOp(false, values); }
	public static Op<String> getPackageEndsWithAndOp(boolean ic, String... values) { return getOp(MatcherOpType.AND, SootMatcherOpType.ENDS_WITH_PACKAGE, ic, values); }
	public static Op<String> getPackageRegexAndOp(String... values) { return getPackageRegexAndOp(false, values); }
	public static Op<String> getPackageRegexAndOp(boolean ic, String... values) { return getOp(MatcherOpType.AND, SootMatcherOpType.REGEX_PACKAGE, ic, values); }
	
	public static Op<String> getSignatureNotOp(String value) { return getOp(MatcherOpType.NOT, SootMatcherOpType.SIGNATURE, false, value); }
	public static Op<String> getNameRegexWordsNotOp(String value) { return getOp(MatcherOpType.NOT, SootMatcherOpType.REGEX_NAME_WORDS, false, value); }
	public static Op<String> getClassRegexWordsNotOp(String value, int i) { return getOp(MatcherOpType.NOT, SootMatcherOpType.REGEX_CLASS_WORDS, false, value, ""+i); }
	public static Op<String> getClassRegexWordsNotOp(String value) { return getOp(MatcherOpType.NOT, SootMatcherOpType.REGEX_CLASS_WORDS, false, value); }
	public static Op<String> getNameEqualNotOp(String value) { return getNameEqualNotOp(false,value); }
	public static Op<String> getNameEqualNotOp(boolean ic, String value) { return getOp(MatcherOpType.NOT, SootMatcherOpType.EQUAL_NAME, ic, value); }
	public static Op<String> getNameContainNotOp(String value) { return getNameContainNotOp(false,value); }
	public static Op<String> getNameContainNotOp(boolean ic, String value) { return getOp(MatcherOpType.NOT, SootMatcherOpType.CONTAIN_NAME, ic, value); }
	public static Op<String> getNameStartsWithNotOp(String value) { return getNameStartsWithNotOp(false,value); }
	public static Op<String> getNameStartsWithNotOp(boolean ic, String value) { return getOp(MatcherOpType.NOT, SootMatcherOpType.STARTS_WITH_NAME, ic, value); }
	public static Op<String> getNameEndsWithNotOp(String value) { return getNameEndsWithNotOp(false, value); }
	public static Op<String> getNameEndsWithNotOp(boolean ic, String value) { return getOp(MatcherOpType.NOT, SootMatcherOpType.ENDS_WITH_NAME, ic, value); }
	public static Op<String> getNameRegexNotOp(String value) { return getNameRegexNotOp(false, value); }
	public static Op<String> getNameRegexNotOp(boolean ic, String value) { return getOp(MatcherOpType.NOT, SootMatcherOpType.REGEX_NAME, ic, value); }
	public static Op<String> getFullClassEqualNotOp(String value) { return getFullClassEqualNotOp(false,value); }
	public static Op<String> getFullClassEqualNotOp(boolean ic, String value) { return getOp(MatcherOpType.NOT, SootMatcherOpType.EQUAL_FULL_CLASS, ic, value); }
	public static Op<String> getFullClassContainNotOp(String value) { return getFullClassContainNotOp(false,value); }
	public static Op<String> getFullClassContainNotOp(boolean ic, String value) { return getOp(MatcherOpType.NOT, SootMatcherOpType.CONTAIN_FULL_CLASS, ic, value); }
	public static Op<String> getFullClassStartsWithNotOp(String value) { return getFullClassStartsWithNotOp(false,value); }
	public static Op<String> getFullClassStartsWithNotOp(boolean ic, String value) { return getOp(MatcherOpType.NOT, SootMatcherOpType.STARTS_WITH_FULL_CLASS, ic, value); }
	public static Op<String> getFullClassEndsWithNotOp(String value) { return getFullClassEndsWithNotOp(false, value); }
	public static Op<String> getFullClassEndsWithNotOp(boolean ic, String value) { return getOp(MatcherOpType.NOT, SootMatcherOpType.ENDS_WITH_FULL_CLASS, ic, value); }
	public static Op<String> getFullClassRegexNotOp(String value) { return getFullClassRegexNotOp(false, value); }
	public static Op<String> getFullClassRegexNotOp(boolean ic, String value) { return getOp(MatcherOpType.NOT, SootMatcherOpType.REGEX_FULL_CLASS, ic, value); }
	public static Op<String> getClassEqualNotOp(String value) { return getClassEqualNotOp(false,value); }
	public static Op<String> getClassEqualNotOp(boolean ic, String value) { return getOp(MatcherOpType.NOT, SootMatcherOpType.EQUAL_CLASS, ic, value); }
	public static Op<String> getClassContainNotOp(String value) { return getClassContainNotOp(false,value); }
	public static Op<String> getClassContainNotOp(boolean ic, String value) { return getOp(MatcherOpType.NOT, SootMatcherOpType.CONTAIN_CLASS, ic, value); }
	public static Op<String> getClassStartsWithNotOp(String value) { return getClassStartsWithNotOp(false,value); }
	public static Op<String> getClassStartsWithNotOp(boolean ic, String value) { return getOp(MatcherOpType.NOT, SootMatcherOpType.STARTS_WITH_CLASS, ic, value); }
	public static Op<String> getClassEndsWithNotOp(String value) { return getClassEndsWithNotOp(false, value); }
	public static Op<String> getClassEndsWithNotOp(boolean ic, String value) { return getOp(MatcherOpType.NOT, SootMatcherOpType.ENDS_WITH_CLASS, ic, value); }
	public static Op<String> getClassRegexNotOp(String value) { return getClassRegexNotOp(false, value); }
	public static Op<String> getClassRegexNotOp(boolean ic, String value) { return getOp(MatcherOpType.NOT, SootMatcherOpType.REGEX_CLASS, ic, value); }
	public static Op<String> getPackageEqualNotOp(String value) { return getPackageEqualNotOp(false,value); }
	public static Op<String> getPackageEqualNotOp(boolean ic, String value) { return getOp(MatcherOpType.NOT, SootMatcherOpType.EQUAL_PACKAGE, ic, value); }
	public static Op<String> getPackageContainNotOp(String value) { return getPackageContainNotOp(false,value); }
	public static Op<String> getPackageContainNotOp(boolean ic, String value) { return getOp(MatcherOpType.NOT, SootMatcherOpType.CONTAIN_PACKAGE, ic, value); }
	public static Op<String> getPackageStartsWithNotOp(String value) { return getPackageStartsWithNotOp(false,value); }
	public static Op<String> getPackageStartsWithNotOp(boolean ic, String value) { return getOp(MatcherOpType.NOT, SootMatcherOpType.STARTS_WITH_PACKAGE, ic, value); }
	public static Op<String> getPackageEndsWithNotOp(String value) { return getPackageEndsWithNotOp(false, value); }
	public static Op<String> getPackageEndsWithNotOp(boolean ic, String value) { return getOp(MatcherOpType.NOT, SootMatcherOpType.ENDS_WITH_PACKAGE, ic, value); }
	public static Op<String> getPackageRegexNotOp(String value) { return getPackageRegexNotOp(false, value); }
	public static Op<String> getPackageRegexNotOp(boolean ic, String value) { return getOp(MatcherOpType.NOT, SootMatcherOpType.REGEX_PACKAGE, ic, value); }
	
	protected static SootMatcherOp getOp(SootMatcherOpType t, boolean ic, String... values) {
		Objects.requireNonNull(values);
		Objects.requireNonNull(t);
		Objects.requireNonNull(values[0]);
		switch(t) {
			case EQUAL_NAME: return getNameEqualOp(values[0],ic);
			case CONTAIN_NAME: return getNameContainOp(values[0],ic);
			case STARTS_WITH_NAME: return getNameStartsWithOp(values[0],ic);
			case ENDS_WITH_NAME: return getNameEndsWithOp(values[0],ic);
			case REGEX_NAME: return getNameRegexOp(values[0],ic);
			case EQUAL_FULL_CLASS: return getFullClassEqualOp(values[0],ic);
			case CONTAIN_FULL_CLASS: return getFullClassContainOp(values[0],ic);
			case STARTS_WITH_FULL_CLASS: return getFullClassStartsWithOp(values[0],ic);
			case ENDS_WITH_FULL_CLASS: return getFullClassEndsWithOp(values[0],ic);
			case REGEX_FULL_CLASS: return getFullClassRegexOp(values[0],ic);
			case EQUAL_CLASS: return getClassEqualOp(values[0],ic);
			case CONTAIN_CLASS: return getClassContainOp(values[0],ic);
			case STARTS_WITH_CLASS: return getClassStartsWithOp(values[0],ic);
			case ENDS_WITH_CLASS: return getClassEndsWithOp(values[0],ic);
			case REGEX_CLASS: return getClassRegexOp(values[0],ic);
			case EQUAL_PACKAGE: return getPackageEqualOp(values[0],ic);
			case CONTAIN_PACKAGE: return getPackageContainOp(values[0],ic);
			case STARTS_WITH_PACKAGE: return getPackageStartsWithOp(values[0],ic);
			case ENDS_WITH_PACKAGE: return getPackageEndsWithOp(values[0],ic);
			case REGEX_PACKAGE: return getPackageRegexOp(values[0],ic);
			case SIGNATURE: return getSignatureOp(values[0]);
			case REGEX_NAME_WORDS: return getNameRegexWordsOp(values[0]);
			case REGEX_CLASS_WORDS: {
				if(values.length > 1) {
					Objects.requireNonNull(values[1]);
					return getClassRegexWordsOp(values[0], Integer.parseInt(values[1]));
				}
				return getClassRegexWordsOp(values[0]);
			}
			default: return null;
		}
	}
	
	private static Op<String> getOp(MatcherOpType type, SootMatcherOpType type2, boolean ic, String... values) {
		Objects.requireNonNull(values);
		List<Op<String>> ops = new ArrayList<>();
		for(int i = 0; i < values.length; i++) {
			String value = values[i];
			Objects.requireNonNull(value);
			if(type2.equals(SootMatcherOpType.REGEX_CLASS_WORDS) && i + 1 < values.length && Ints.tryParse(values[i+1]) != null)
				ops.add(getOp(type2, ic, value, values[++i]));
			else
				ops.add(getOp(type2, ic, value));
		}
		switch(type) {
			case AND: return getAndOp(ops);
			case OR: return getOrOp(ops);
			case NOT: return getNotOp(ops.get(0));
			default: return null;
		}
	}
	
}
