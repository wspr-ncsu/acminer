package org.sag.common.incexclist.methodlist;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.sag.common.incexclist.Record;
import org.sag.common.xstream.XStreamInOut;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import soot.SootMethod;

@XStreamAlias("MethodNameRecord")
class MethodNameRecord extends Record<SootMethod>{

	@XStreamAlias("Name")
	@XStreamAsAttribute
	protected String name;
	
	private static final String typeId = "MethodName";
	
	//For xstream use only
	protected MethodNameRecord(){}
	
	protected MethodNameRecord(boolean isInclude, String name) {
		super(isInclude);
		this.name = name;
	}
	
	protected static MethodNameRecord constructEntry(String includeId, String name){
		boolean isInclude = getBooleanFromIncludeId(includeId.trim());
		
		if(name == null)
			throw new RuntimeException("Error: A method name must be provided.");
		
		name = name.trim();
		
		if(name.isEmpty())
			throw new RuntimeException("Error: A method name must be provided.");
		
		return new MethodNameRecord(isInclude,name);
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
	public boolean isMatch(SootMethod m) {
		if(m.getName().equals(name)){
			return true;
		}
		return false;
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}
	
	@Override
	public MethodNameRecord readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static MethodNameRecord readXMLStatic(String filePath, Path path) throws Exception {
		return new MethodNameRecord().readXML(filePath, path);
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
			return Collections.singleton(MethodNameRecord.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}

}
