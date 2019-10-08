package org.sag.common.incexclist.methodlist;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.sag.common.incexclist.Record;
import org.sag.xstream.XStreamInOut;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import soot.Scene;
import soot.SootMethod;

@XStreamAlias("MethodSignatureRecord")
class MethodSignatureRecord extends Record<SootMethod>{
	
	@XStreamAlias("Signature")
	@XStreamAsAttribute
	private String sig;
	
	@XStreamOmitField
	private SootMethod resolvedMethod;
	@XStreamOmitField
	private boolean resolveAttempted;

	private static final String typeId = "MethodSignature";
	
	//For xstream use only
	protected MethodSignatureRecord(){
		resolvedMethod = null;
		resolveAttempted = false;
	}
	
	protected MethodSignatureRecord(boolean isInclude, String signature) {
		super(isInclude);
		sig = signature;
		resolvedMethod = null;
		resolveAttempted = false;
	}
	
	protected static MethodSignatureRecord constructEntry(String includeId, String signature){
		boolean isInclude = getBooleanFromIncludeId(includeId.trim());
		
		if(signature == null)
			throw new RuntimeException("Error: A signature must be provided.");
		
		signature = signature.trim();
		
		if(signature.isEmpty())
			throw new RuntimeException("Error: A signature must be provided.");
		
		return new MethodSignatureRecord(isInclude,signature);
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
		return sig;
	}

	@Override
	public boolean isMatch(SootMethod m) {
		//Speed up by resolving the signature to a method of the current scene if one exists once
		if(!resolveAttempted){
			resolvedMethod = Scene.v().grabMethod(sig);
			resolveAttempted = true;
		}
		//If it failed to resolve the signature to a method then it doesn't exist in the scene and thus matches no method in the scene
		if(resolvedMethod == null)
			return false;
		//Otherwise perform a quick reference comparison
		return m.equals(resolvedMethod);
	}
	
	@Override
	public void resetSootResolvedData(){
		resolveAttempted = false;
		resolvedMethod = null;
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}
	
	@Override
	public MethodSignatureRecord readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static MethodSignatureRecord readXMLStatic(String filePath, Path path) throws Exception {
		return new MethodSignatureRecord().readXML(filePath, path);
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
			return Collections.singleton(MethodSignatureRecord.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}

}
