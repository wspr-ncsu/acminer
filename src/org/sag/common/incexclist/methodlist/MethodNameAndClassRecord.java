package org.sag.common.incexclist.methodlist;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sag.xstream.XStreamInOut;
import soot.SootMethod;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("MethodNameAndClassRecord")
class MethodNameAndClassRecord extends MethodNameRecord {
	
	@XStreamAlias("ClassName")
	@XStreamAsAttribute
	private String className;
	
	private static final String typeId = "MethodClassAndName";
	private static final Pattern p1 = Pattern.compile("^([^\\s]+)\\s+:\\s+(.+)");
	
	//For xstream use only
	protected MethodNameAndClassRecord(){}

	protected MethodNameAndClassRecord(boolean isInclude, String className, String name) {
		super(isInclude,name);
		this.className = className;
		this.name = name;
	}
	
	protected static MethodNameAndClassRecord constructEntry(String includeId, String line){
		boolean isInclude = getBooleanFromIncludeId(includeId.trim());
		
		if(line == null)
			throw new RuntimeException("Error: A method name and class name must be provided.");
		
		line = line.trim();
		
		if(line.isEmpty())
			throw new RuntimeException("Error: A method name and class name must be provided.");
		
		Matcher m = p1.matcher(line);
		if(m.matches()){
			String className = m.group(1).trim();
			String name = m.group(2).trim();
			if(className != null && name != null && !className.isEmpty() && !name.isEmpty())
				return new MethodNameAndClassRecord(isInclude,className,name);
		}
		throw new RuntimeException("Error: Failed to seperate the class name from the method name.");
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
		return className + " : " + name;
	}

	@Override
	public boolean isMatch(SootMethod m) {
		if(ClassRecord.compare(className, m.getDeclaringClass().getName()) && m.getName().equals(name)){
			return true;
		}
		return false;
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}
	
	@Override
	public MethodNameAndClassRecord readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static MethodNameAndClassRecord readXMLStatic(String filePath, Path path) throws Exception {
		return new MethodNameAndClassRecord().readXML(filePath, path);
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
				MethodNameRecord.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(MethodNameAndClassRecord.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}

}
