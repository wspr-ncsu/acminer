package org.sag.common.incexclist.classlist;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.sag.common.incexclist.Record;
import org.sag.common.tools.HierarchyHelpers;
import org.sag.common.xstream.XStreamInOut;

import soot.Scene;
import soot.SootClass;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("SuperClassRecord")
class SuperClassRecord extends Record<SootClass>{

	@XStreamAlias("Name")
	@XStreamAsAttribute
	private String name;
	
	@XStreamOmitField
	private Set<SootClass> classes;
	
	@XStreamOmitField
	private boolean failRes;
	
	private static final String typeId = "SuperClass";
	
	//For xstream use only
	protected SuperClassRecord(){}
	
	protected SuperClassRecord(boolean isInclude, String name) {
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
	
	private void resolveToSoot(){
		failRes = false;
		SootClass intface;
		try{
			if(Scene.v().containsClass(name)){
				intface = Scene.v().getSootClass(name);
				classes = HierarchyHelpers.getAllSubClasses(intface);
			}else{
				classes = Collections.emptySet();
			}
		}catch(Throwable t){
			failRes = true;
			classes = Collections.emptySet();
			return;
		}
	}

	@Override
	public boolean isMatch(SootClass m) {
		if(classes == null){
			resolveToSoot();
		}
		if(failRes){
			return false;
		}
		return classes.contains(m);
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}
	
	@Override
	public SuperClassRecord readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static SuperClassRecord readXMLStatic(String filePath, Path path) throws Exception {
		return new SuperClassRecord().readXML(filePath, path);
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
			return Collections.singleton(SuperClassRecord.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}
	
}
