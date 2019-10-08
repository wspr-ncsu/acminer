package org.sag.common.incexclist.classlist;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.sag.common.incexclist.Record;
import org.sag.xstream.XStreamInOut;
import soot.SootClass;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("ClassRecord")
class ClassRecord extends Record<SootClass>{

	@XStreamAlias("Name")
	@XStreamAsAttribute
	private String name;
	
	private static final String typeId = "ClassPath";
	
	//For xstream use only
	protected ClassRecord(){}

	protected ClassRecord(boolean isInclude, String name) {
		super(isInclude);
		this.name = name;
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
		return name;
	}

	@Override
	public boolean isMatch(SootClass m) {
		return compare(name,m.getName());
	}
	
	@Override
	public boolean isMatchString(String s){
		return compare(name,s);
	}
	
	static boolean compare(String name,String className){
		if(className.equals(name) || ((name.endsWith(".*") || name.endsWith("$*")) && className.startsWith(name.substring(0, name.length() - 1)))){
			return true;
		}
		return false;
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}
	
	@Override
	public ClassRecord readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static ClassRecord readXMLStatic(String filePath, Path path) throws Exception {
		return new ClassRecord().readXML(filePath, path);
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
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(ClassRecord.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}
	
}
