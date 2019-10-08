package org.sag.soot.xstream;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.sag.soot.SootSort;
import org.sag.sootinit.SootInstanceWrapper;
import org.sag.xstream.XStreamInOut;
import org.sag.xstream.XStreamInOut.XStreamInOutInterface;
import soot.Modifier;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.Type;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("SootFieldContainer")
public final class SootFieldContainer implements XStreamInOutInterface, Comparable<SootFieldContainer> {
	
	@XStreamAlias("Signature")
	@XStreamAsAttribute
	private String signature;

	@XStreamAlias("DeclaringClass")
	private String declaringClass;
	
	@XStreamAlias("Name")
	private String name;
	
	@XStreamAlias("Type")
	private String type;
	
	@XStreamAlias("Modifiers")
	private String modifiers;
	
	//for reading in from xml only
	private SootFieldContainer(){}
	
	private SootFieldContainer(SootField m) {
		if(m == null)
			throw new IllegalArgumentException("A SootField must be given.");
		this.signature = m.getSignature();
		this.declaringClass = m.getDeclaringClass().toString();
		this.name = m.getName();
		this.type = m.getType().toString();
		this.modifiers = (Modifier.toString(m.getModifiers()) + (m.isPhantom()?" phantom":"")).trim();
		this.modifiers = modifiers.length() == 0 ? null : modifiers;
	}
	
	private SootFieldContainer(String signature, String declaringClass, String name, String type, String modifiers){
		this.signature = signature;
		this.declaringClass = declaringClass;
		this.name = name;
		this.type = type;
		this.modifiers = modifiers;
	}
	
	public boolean equals(Object o){
		if(this == o){
			return true;
		}
		if(o == null || !(o instanceof SootFieldContainer)){
			return false;
		}
		SootFieldContainer other = (SootFieldContainer) o;
		return Objects.equals(signature, other.signature) && 
				Objects.equals(declaringClass, other.declaringClass) && 
				Objects.equals(name,other.name) && 
				Objects.equals(type,other.type) &&
				Objects.equals(modifiers,other.modifiers);
				
	}
	
	public int hashCode(){
		int hash = 17;
		hash = 31 * hash + Objects.hashCode(signature);
		hash = 31 * hash + Objects.hashCode(declaringClass);
		hash = 31 * hash + Objects.hashCode(name);
		hash = 31 * hash + Objects.hashCode(type);
		hash = 31 * hash + Objects.hashCode(modifiers);
		return hash;
	}
	
	@Override
	public int compareTo(SootFieldContainer o) {
		return SootSort.sfStringComp.compare(signature, o.signature);
	}
	
	public String toString(String spacer){
		StringBuilder sb = new StringBuilder();
		sb.append(spacer).append(Objects.toString(signature));
		return sb.toString();
	}
	
	@Override
	public String toString(){
		return toString("");
	}
	
	public String getSignature(){
		return signature;
	}
	
	public String getDeclaringClass() {
		return declaringClass;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public String getModifiers() {
		if(modifiers == null)
			return "";
		return modifiers;
	}

	public boolean isPublic() {
		if(modifiers == null)
			return false;
		return modifiers.contains("public");
	}
	
	public boolean isPrivate() {
		if(modifiers == null)
			return false;
		return modifiers.contains("private");
	}
	
	public boolean isProtected() {
		if(modifiers == null)
			return false;
		return modifiers.contains("protected");
	}
	
	public boolean isStatic() {
		if(modifiers == null)
			return false;
		return modifiers.contains("static");
	}
	
	public boolean isFinal() {
		if(modifiers == null)
			return false;
		return modifiers.contains("final");
	}
	
	public boolean isPhantom() {
		if(modifiers == null)
			return false;
		return modifiers.contains("phantom");
	}
	
	public SootField toSootField(){
		if(!SootInstanceWrapper.v().isAnySootInitValueSet())
			throw new RuntimeException("Some instance of Soot must be initilized first.");
		return Scene.v().getField(signature);
	}
	
	public SootField toSootFieldUnsafe(){
		return Scene.v().grabField(signature);
	}
	
	public SootField toSootFieldAllowPhantom() {
		if(Scene.v().containsField(getSignature())) {
			return toSootField();
		} else {
			SootClass dc = Scene.v().getSootClassUnsafe(getDeclaringClass());
			Type t = Scene.v().getTypeUnsafe(type);
			if(dc == null || t == null)
				throw new RuntimeException("Error: Unable to parse the declaring class or field type to a soot object.");
			SootField ret = Scene.v().makeFieldRef(dc, name, t, isStatic()).resolve();
			if(ret == null)
				throw new RuntimeException("Error: Unable to resolve the soot field.");
			return ret;
		}
	}

	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public SootFieldContainer readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static SootFieldContainer readXMLStatic(String filePath, Path path) throws Exception {
		return new SootFieldContainer().readXML(filePath, path);
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
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(SootFieldContainer.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}
	
	@XStreamOmitField
	private static Map<SootField, SootFieldContainer> sootFieldContainers;
	private static final Object lock = new Object();
	
	public static SootFieldContainer makeSootMethodContainer(SootField sf){
		synchronized(lock) {
			SootFieldContainer ret = null;
			if(sootFieldContainers == null){
				sootFieldContainers = new HashMap<>();
				ret = new SootFieldContainer(sf);
				sootFieldContainers.put(sf, ret);
			}else{
				ret = sootFieldContainers.get(sf);
				if(ret == null){
					ret = new SootFieldContainer(sf);
					sootFieldContainers.put(sf, ret);
				}
			}
			return ret;
		}
	}
	
	public static void reset(){
		synchronized(lock) {
			sootFieldContainers = null;
		}
	}

}
