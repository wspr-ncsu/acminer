package org.sag.soot.xstream;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.sag.common.tools.SortingMethods;
import org.sag.common.xstream.NamedCollectionConverterWithSize;
import org.sag.common.xstream.XStreamInOut;
import org.sag.common.xstream.XStreamInOut.XStreamInOutInterface;
import org.sag.main.sootinit.SootInstanceWrapper;

import soot.Modifier;
import soot.Scene;
import soot.SootClass;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("SootClassContainer")
public final class SootClassContainer implements XStreamInOutInterface, Comparable<SootClassContainer> {

	@XStreamAlias("Signature")
	@XStreamAsAttribute
	private String signature;
	
	@XStreamAlias("PackageName")
	private String packageName;
	
	@XStreamAlias("ClassName")
	private String className;
	
	@XStreamAlias("SuperClass")
	private String superClass;
	
	@XStreamAlias("OuterClass")
	private String outerClass;
	
	@XStreamAlias("Interfaces")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"Interface"},types={String.class})
	private ArrayList<String> interfaces;
	
	@XStreamAlias("Modifiers")
	private String modifiers;
	
	private static final ArrayList<String> collectionToStringList(Collection<?> in){
		if(in != null && !in.isEmpty()){
			ArrayList<String> ret = new ArrayList<>();
			for(Object o : in){
				ret.add(o.toString());
			}
			return ret;
		}
		return null;
	}
	
	//for reading in from xml only
	private SootClassContainer(){}
	
	private SootClassContainer(SootClass sc){
		if(sc == null)
			throw new IllegalArgumentException("A SootClass must be given.");
		signature = sc.getName();
		packageName = (sc.getPackageName() == null || sc.getPackageName().isEmpty()) ? null : sc.getPackageName();
		className = (sc.getShortName() == null || sc.getShortName().isEmpty()) ? null : sc.getShortName();
		if(sc.hasSuperclass()){
			String superClassName = sc.getSuperclass().getName();
			superClass = superClassName.equals("java.lang.Object") ? null : superClassName;
		}else{
			superClass = null;
		}
		outerClass = sc.hasOuterClass() ? sc.getOuterClass().toString() : null;
		interfaces = collectionToStringList(sc.getInterfaces());
		modifiers = (Modifier.toString(sc.getModifiers()) + (sc.isPhantom()?" phantom":"")).trim();
		this.modifiers = modifiers.length() == 0 ? null : modifiers;
	}
	
	private SootClassContainer(String signature, String packageName, String className, String superClass, String outerClass, List<String> interfaces, String modifiers){
		this.signature = signature;
		this.packageName = packageName;
		this.className = className;
		this.superClass = superClass;
		this.outerClass = outerClass;
		this.interfaces = interfaces == null ? null : new ArrayList<String>(interfaces);
		this.modifiers = modifiers;
	}
	
	public boolean equals(Object o){
		if(this == o){
			return true;
		}
		if(o == null || !(o instanceof SootClassContainer)){
			return false;
		}
		SootClassContainer other = (SootClassContainer) o;
		return Objects.equals(signature, other.signature) &&
				Objects.equals(packageName, other.packageName) &&
				Objects.equals(className, other.className) &&
				Objects.equals(superClass, other.superClass) &&
				Objects.equals(outerClass, other.outerClass) &&
				Objects.equals(interfaces, other.interfaces) &&
				Objects.equals(modNoPhantom(), other.modNoPhantom());
	}
	
	public int hashCode(){
		int hash = 17;
		hash = 31 * hash + Objects.hashCode(signature);
		hash = 31 * hash + Objects.hashCode(packageName);
		hash = 31 * hash + Objects.hashCode(className);
		hash = 31 * hash + Objects.hashCode(superClass);
		hash = 31 * hash + Objects.hashCode(outerClass);
		hash = 31 * hash + Objects.hashCode(interfaces);
		hash = 31 * hash + Objects.hashCode(modNoPhantom());
		return hash;
	}
	
	private String modNoPhantom() {
		if(this.modifiers == null)
			return null;
		return this.modifiers.replace("phantom", "").replaceAll("\\s+", " ").trim();
	}
	
	@Override
	public int compareTo(SootClassContainer o) {
		return SortingMethods.sComp.compare(signature, o.signature);
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
	
	public String getSignature() {
		return signature;
	}

	public String getPackageName() {
		return packageName == null ? "" : packageName;
	}

	public String getClassName() {
		return className == null ? signature : className;
	}

	public String getSuperClass(){
		if(superClass == null){
			return isInterface() ? "java.lang.Object" : null;
		}
		return superClass;
	}
	
	public String getOuterClass(){
		return outerClass;
	}
	
	public List<String> getInterfaces(){
		if(interfaces == null)
			return new ArrayList<>();
		return new ArrayList<>(interfaces);
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
	
	public boolean isInterface() {
		if(modifiers == null)
			return false;
		return modifiers.contains("interface");
	}

	public boolean isAbstract() {
		if(modifiers == null)
			return false;
		return modifiers.contains("abstract");
	}

	public boolean isFinal() {
		if(modifiers == null)
			return false;
		return modifiers.contains("final");
	}

	public boolean isStatic() {
		if(modifiers == null)
			return false;
		return modifiers.contains("static");
	}
	
	public SootClass toSootClass(){
		if(!SootInstanceWrapper.v().isSootInitSet())
			throw new RuntimeException("Some instance of Soot must be initilized first.");
		return Scene.v().getSootClass(signature);
	}
	
	public SootClass toSootClassUnsafe(){
		return Scene.v().getSootClassUnsafe(signature);
	}

	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public SootClassContainer readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static SootClassContainer readXMLStatic(String filePath, Path path) throws Exception {
		return new SootClassContainer().readXML(filePath, path);
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
			return Collections.singleton(SootClassContainer.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}
	
	@XStreamOmitField
	private static Map<SootClass, SootClassContainer> sootClassContainers;
	private static final Object lock = new Object();
	
	public static SootClassContainer makeSootClassContainer(SootClass sc){
		synchronized(lock) {
			SootClassContainer ret = null;
			if(sootClassContainers == null){
				sootClassContainers = new HashMap<>();
				ret = new SootClassContainer(sc);
				sootClassContainers.put(sc, ret);
			}else{
				ret = sootClassContainers.get(sc);
				if(ret == null){
					ret = new SootClassContainer(sc);
					sootClassContainers.put(sc, ret);
				}
			}
			return ret;
		}
	}
	
	public static void reset(){
		synchronized(lock) {
			sootClassContainers = null;
		}
	}

}
