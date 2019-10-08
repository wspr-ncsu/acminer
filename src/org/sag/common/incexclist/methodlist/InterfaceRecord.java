package org.sag.common.incexclist.methodlist;

import java.nio.file.Path;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.sag.common.tools.HierarchyHelpers;
import org.sag.sootinit.SootInstanceWrapper;
import org.sag.xstream.XStreamInOut;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

@XStreamAlias("InterfaceRecord")
class InterfaceRecord extends ClassRecord {
	
	@XStreamAlias("AllMethods")
	@XStreamAsAttribute
	private boolean allMethods;
	
	@XStreamOmitField
	private Set<SootMethod> methods;
	
	@XStreamOmitField
	private boolean failRes;
	
	private static final String typeId = "Interface";
	
	//For xstream use only
	protected InterfaceRecord(){}
	
	/* No modifiers = only include the methods of the current interfaces and
	 * any of their implementations in the subclasses
	 * Modifiers = include all methods so long as they match the modifier
	 * and [all] means all methods
	 */
	
	//any in the implementers of the interface that match the modifiers
	protected InterfaceRecord(boolean isInclude, String name, String modifiers) {
		super(isInclude,name,modifiers);
		allMethods = true;
		methods = null;
		failRes = false;
	}
	
	//methods in interface only
	protected InterfaceRecord(boolean isInclude, String name){
		super(isInclude,name,(BitSet)null);
		allMethods = false;
		methods = null;
		failRes = false;
	}
	
	protected static InterfaceRecord constructEntry(String includeId, String line){
		Object[] temp = parseInput(includeId, line);
		boolean isInclude = (Boolean)temp[0];
		String modifiers = (String)temp[1];
		String name = (String)temp[2];
		if(modifiers == null)
			return new InterfaceRecord(isInclude,name);
		else
			return new InterfaceRecord(isInclude,name,modifiers);
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
		if(allMethods)
			return bitSetToString(modifiers);
		return null;
	}

	@Override
	public String getSignature() {
		return name;
	}
	
	@Override
	public boolean isEntireClass(){
		if(allMethods){
			if(modifiers.cardinality() == getModifiersSize()){
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean isMatch(SootMethod m) {
		if(methods == null){
			resolveToSoot();
		}
		if(failRes){
			return false;
		}
		return methods.contains(m);
	}
	
	private void checkSootInit(){
		if(!SootInstanceWrapper.v().isAnySootInitValueSet())
			throw new RuntimeException("Error: Some instance of Soot must be initilized first.");
	}
	
	private void resolveToSoot(){
		checkSootInit();
		failRes = false;
		SootClass intface;
		try{
			if(Scene.v().containsClass(name)){
				intface = Scene.v().getSootClass(name);
				if(!allMethods){
					methods = HierarchyHelpers.getAllImplementingMethods(intface);
				}else{
					Set<SootClass> classes = HierarchyHelpers.getAllSubClassesOfInterface(intface);
					methods = new HashSet<SootMethod>();
					for(SootClass sc : classes){
						ClassRecord entry = new ClassRecord(isInclude,sc.getName(),modifiers);
						for(SootMethod m : sc.getMethods()){
							if(entry.isMatch(m)){
								methods.add(m);
							}
						}
					}
				}
			}else{
				methods = Collections.emptySet();
			}
		}catch(Throwable t){
			failRes = true;
			methods = Collections.emptySet();
			return;
		}
	}
	
	public void resetSootResolvedData(){
		methods = null;
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}
	
	@Override
	public InterfaceRecord readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static InterfaceRecord readXMLStatic(String filePath, Path path) throws Exception {
		return new InterfaceRecord().readXML(filePath, path);
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
				ClassRecord.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(InterfaceRecord.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}

}
