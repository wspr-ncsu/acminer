package org.sag.acminer.database.filter.matcher;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.sag.xstream.XStreamInOut.XStreamInOutInterface.AbstractXStreamSetup;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("IMatcher")
public interface IMatcher {

	public boolean matcher(Object... objects);
	public String getValue();
	
	public static final class Factory {
		
		private static final Pattern valueP = Pattern.compile("^Value\\s*=\\s*(.+)$");
		
		public static IMatcher getMatcher(String name, List<String> lines) {
			Objects.requireNonNull(name);
			Objects.requireNonNull(lines);
			if(lines.isEmpty()) {
				throw new RuntimeException("Error: A matcher requires at least a value.");
			} else {
				String v = null;
				for(String s : lines) {
					s = s.trim();
					java.util.regex.Matcher m = valueP.matcher(s);
					if(m.matches()) {
						v = m.group(1);
						break;
					}
				}
				if(v == null)
					throw new RuntimeException("Error: Unable to find a value for the matcher.");
				if(name.equals(FieldMatcher.class.getSimpleName())) {
					return new FieldMatcher(v);
				} else if(name.equals(MethodMatcher.class.getSimpleName())) {
					return new MethodMatcher(v);
				} else if(name.equals(NumberMatcher.class.getSimpleName())) {
					return new NumberMatcher(v);
				} else if(name.equals(StringMatcher.class.getSimpleName())) {
					return new StringMatcher(v);
				} else if(name.equals(TypeMatcher.class.getSimpleName())) {
					return new TypeMatcher(v);
				} else {
					throw new RuntimeException("Error: The matcher type '" + name + "' does not match a parsable matcher.");
				}
			}
		}
		
		private static AbstractXStreamSetup xstreamSetup = null;
		
		public static AbstractXStreamSetup getXStreamSetupStatic(){
			if(xstreamSetup == null)
				xstreamSetup = new XStreamSetup();
			return xstreamSetup;
		}
		
		private static class XStreamSetup extends AbstractXStreamSetup {
			
			@Override
			public void getOutputGraph(LinkedHashSet<AbstractXStreamSetup> in) {
				if(!in.contains(this)) {
					in.add(this);
				}
			}

			@Override
			public Set<Class<?>> getAnnotatedClasses() {
				Set<Class<?>> ret = new HashSet<>();
				ret.add(Matcher.class);
				ret.add(StringMatcher.class);
				ret.add(TypeMatcher.class);
				ret.add(SootMatcher.class);
				ret.add(FieldMatcher.class);
				ret.add(MethodMatcher.class);
				ret.add(NumberMatcher.class);
				ret.add(IMatcher.class);
				return ret;
			}
			
			@Override
			public void setXStreamOptions(XStream xstream) {
				defaultOptionsNoRef(xstream);
			}
			
		}
		
	}
	
}
