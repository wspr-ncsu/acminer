package org.sag.common.incexclist.methodlist;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sag.acminer.database.filter.matcher.IMatcher;
import org.sag.acminer.database.filter.matcher.MethodMatcher;
import org.sag.common.incexclist.Record;
import org.sag.common.tools.HierarchyHelpers;
import org.sag.sootinit.SootInstanceWrapper;
import org.sag.xstream.XStreamInOut;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("MethodSpecialRecord")
public class MethodSpecialRecord extends Record<SootMethod> {
	
	private static final String typeId = "MethodSpecial";
	private static final Pattern p1 = Pattern.compile("(?:^IncludeSubClasses=([^\\s]+)\\s+|^)(?:ClassName=([^\\s]+)\\s+|)Method=(.+)$");
	
	@XStreamAlias("IncludeSubClasses")
	@XStreamAsAttribute
	private volatile Boolean includeSubClasses;
	
	@XStreamAlias("ClassName")
	@XStreamAsAttribute
	private volatile String className;
	
	@XStreamAlias("MethodMatcher")
	private volatile MethodMatcher methodMatcher;
	
	@XStreamOmitField
	private volatile boolean resolveAttempted;
	@XStreamOmitField
	private volatile Set<SootMethod> cache;
	
	protected MethodSpecialRecord() {}
	
	protected MethodSpecialRecord(MethodMatcher mm) {
		this(false, false, null, mm);
	}
	
	protected MethodSpecialRecord(boolean isInclude, MethodMatcher mm) {
		this(isInclude, false, null, mm);
	}
	
	protected MethodSpecialRecord(boolean isInclude, boolean includeSubClasses, String className, MethodMatcher mm) {
		super(isInclude);
		Objects.requireNonNull(mm);
		this.className = className;
		this.methodMatcher = mm;
		if(includeSubClasses)
			this.includeSubClasses = true;
		else
			this.includeSubClasses = null;
		this.resolveAttempted = false;
		this.cache = null;
	}
	
	protected static MethodSpecialRecord constructEntry(String includeId, String line) {
		boolean isInclude = getBooleanFromIncludeId(includeId.trim());
		boolean includeSubClasses = false;
		String className = null;
		MethodMatcher mm = null;
		
		if(line == null)
			throw new RuntimeException("Error: A no data provided.");
		
		line = line.trim();
		
		Matcher m = p1.matcher(line);
		if(m.matches()) {
			String b1 = m.group(1);
			String b2 = m.group(2);
			String b3 = m.group(3);
			if(b1 != null && !b1.isEmpty())
				includeSubClasses = Boolean.parseBoolean(b1);
			if(b2 != null && !b2.isEmpty())
				className = b2;
			if(b3 != null && !b3.isEmpty())
				mm = new MethodMatcher(b3);
			else
				throw new RuntimeException("Error: At least a method description must be given for '" + line + "'.");
			return new MethodSpecialRecord(isInclude, includeSubClasses, className, mm);
		}
		throw new RuntimeException("Error: Failed to parse the data provided for '" + line + "'.");
	}

	@Override
	public boolean isMatch(SootMethod m) {
		if(!resolveAttempted)
			resolveSootData();
		return cache.contains(m);
	}
	
	public static String getTypeIdStatic(){
		return typeId;
	}

	@Override
	public String getTypeId() {
		return typeId;
	}

	@Override
	public String getOptions() {
		return null;
	}

	@Override
	public String getSignature() {
		return (includeSubClasses == null ? "" : "IncludeSubClasses=" + includeSubClasses + " ") 
				+ (className == null ? "" : "ClassName=" + className + " ") + "Method=" + methodMatcher.getValue(); 
	}
	
	private synchronized void resolveSootData() {
		if(!resolveAttempted) {
			if(!SootInstanceWrapper.v().isAnySootInitValueSet())
				throw new RuntimeException("Error: Soot must be initilized.");
			this.cache = new HashSet<>();
			Set<SootClass> expC = new HashSet<>();
			if(className != null) {
				for(SootClass sc : Scene.v().getClasses()) {
					if(ClassRecord.compare(className, sc.getName())) {
						if(includeSubClasses != null && includeSubClasses) {
							if(sc.isInterface())
								expC.addAll(HierarchyHelpers.getAllSubClassesOfInterface(sc));
							else
								expC.addAll(HierarchyHelpers.getAllSubClasses(sc));
						}
						expC.add(sc);
					}
				}
			} else {
				expC.addAll(Scene.v().getClasses());
			}
			
			for(SootClass sc : expC) {
				for(SootMethod sm : sc.getMethods()) {
					if(methodMatcher.matcher(sm))
						cache.add(sm);
				}
			}
			if(cache.isEmpty())
				cache = Collections.emptySet();
			resolveAttempted = true;
		}
	}
	
	@Override
	public synchronized void resetSootResolvedData(){
		this.cache = null;
		this.resolveAttempted = false;
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}
	
	@Override
	public MethodSpecialRecord readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static MethodSpecialRecord readXMLStatic(String filePath, Path path) throws Exception {
		return new MethodSpecialRecord().readXML(filePath, path);
	}
	
	@XStreamOmitField
	private static AbstractXStreamSetup xstreamSetup = null;

	public static AbstractXStreamSetup getXStreamSetupStatic(){
		if(xstreamSetup == null)
			xstreamSetup = new XStreamSetup();
		return xstreamSetup;
	}
	
	@Override
	public AbstractXStreamSetup getXStreamSetup() {
		return getXStreamSetupStatic();
	}
	
	public static class XStreamSetup extends AbstractXStreamSetup {
		
		@Override
		public void getOutputGraph(LinkedHashSet<AbstractXStreamSetup> in) {
			if(!in.contains(this)) {
				in.add(this);
				Record.getXStreamSetupStatic().getOutputGraph(in);
				IMatcher.Factory.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(MethodSpecialRecord.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}

}
