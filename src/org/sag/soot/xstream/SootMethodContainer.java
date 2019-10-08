package org.sag.soot.xstream;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.sag.common.tools.SortingMethods;
import org.sag.sootinit.SootInstanceWrapper;
import org.sag.xstream.XStreamInOut;
import org.sag.xstream.XStreamInOut.XStreamInOutInterface;
import org.sag.xstream.xstreamconverters.NamedCollectionConverterWithSize;
import soot.Modifier;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("SootMethodContainer")
public final class SootMethodContainer implements XStreamInOutInterface,Comparable<SootMethodContainer> {
	
	@XStreamAlias("Signature")
	@XStreamAsAttribute
	private String signature;

	@XStreamAlias("DeclaringClass")
	private String declaringClass;
	
	@XStreamAlias("Name")
	private String name;
	
	@XStreamAlias("ReturnType")
	private String returnType;
	
	@XStreamAlias("ArgumentTypes")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"Type"},types={String.class})
	private ArrayList<String> argumentTypes;
	
	@XStreamAlias("ExceptionsThrown")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"Exception"},types={String.class})
	private ArrayList<String> exceptionsThrown;
	
	@XStreamAlias("Modifiers")
	private String modifiers;
	
	private static final ArrayList<String> listToStringList(List<?> in){
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
	private SootMethodContainer(){}
	
	private SootMethodContainer(SootMethod m) {
		if(m == null)
			throw new IllegalArgumentException("A SootMethod must be given.");
		this.signature = m.getSignature();
		this.declaringClass = m.getDeclaringClass().toString();
		this.name = m.getName();
		this.returnType = m.getReturnType().toString();
		this.argumentTypes = listToStringList(m.getParameterTypes());
		this.exceptionsThrown = listToStringList(m.getExceptions());
		this.modifiers = (Modifier.toString(m.getModifiers()) + (m.isPhantom()?" phantom":"")).trim();
		this.modifiers = modifiers.length() == 0 ? null : modifiers;
	}
	
	private SootMethodContainer(String signature, String declaringClass, String name, String returnType, List<String> argumentTypes, List<String> exceptionsThrown, String modifiers){
		this.signature = signature;
		this.declaringClass = declaringClass;
		this.name = name;
		this.returnType = returnType;
		this.argumentTypes = argumentTypes == null ? null : new ArrayList<>(argumentTypes);
		this.exceptionsThrown = exceptionsThrown == null ? null : new ArrayList<>(exceptionsThrown);
		this.modifiers = modifiers;
	}
	
	public boolean equals(Object o){
		if(this == o){
			return true;
		}
		if(o == null || !(o instanceof SootMethodContainer)){
			return false;
		}
		SootMethodContainer other = (SootMethodContainer) o;
		return Objects.equals(signature, other.signature) && 
				Objects.equals(declaringClass, other.declaringClass) && 
				Objects.equals(name,other.name) && 
				Objects.equals(returnType,other.returnType) &&
				Objects.equals(argumentTypes,other.argumentTypes) &&
				Objects.equals(exceptionsThrown,other.exceptionsThrown) &&
				Objects.equals(modifiers,other.modifiers);
				
	}
	
	public int hashCode(){
		int hash = 17;
		hash = 31 * hash + Objects.hashCode(signature);
		hash = 31 * hash + Objects.hashCode(declaringClass);
		hash = 31 * hash + Objects.hashCode(name);
		hash = 31 * hash + Objects.hashCode(returnType);
		hash = 31 * hash + Objects.hashCode(argumentTypes);
		hash = 31 * hash + Objects.hashCode(exceptionsThrown);
		hash = 31 * hash + Objects.hashCode(modifiers);
		return hash;
	}
	
	@Override
	public int compareTo(SootMethodContainer o) {
		if(o == null) {
			return 1;
		} else {			
			int ret = SortingMethods.sComp.compare(this.getDeclaringClass(), o.getDeclaringClass());
			if(ret == 0) {
				ret = SortingMethods.sComp.compare(this.getName(), o.getName());
				if(ret == 0) {
					ret = SortingMethods.sComp.compare(this.getReturnType(), o.getReturnType());
					if(ret == 0)
						return SortingMethods.sComp.compare(this.getArgumentTypes().toString(),o.getArgumentTypes().toString());
				}
			}
			return ret;
		}
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

	public String getReturnType() {
		return returnType;
	}

	public List<String> getArgumentTypes() {
		if(argumentTypes == null)
			return new ArrayList<>();
		return new ArrayList<>(argumentTypes);
	}

	public List<String> getExceptionsThrown() {
		if(exceptionsThrown == null)
			return new ArrayList<>();
		return new ArrayList<>(exceptionsThrown);
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
	
	public boolean isNative() {
		if(modifiers == null)
			return false;
		return modifiers.contains("native");
	}
	
	public boolean isSynchronized() {
		if(modifiers == null)
			return false;
		return modifiers.contains("synchronized");
	}
	
	public boolean isPhantom() {
		if(modifiers == null)
			return false;
		return modifiers.contains("phantom");
	}
	
	public SootMethod toSootMethod(){
		if(!SootInstanceWrapper.v().isAnySootInitValueSet())
			throw new RuntimeException("Some instance of Soot must be initilized first.");
		return Scene.v().getMethod(signature);
	}
	
	public SootMethod toSootMethodUnsafe() {
		return Scene.v().grabMethod(signature);
	}
	
	public SootMethod toSootMethodAllowPhantom() {
		if(Scene.v().containsMethod(getSignature())) {
			return toSootMethod();
		} else {
			SootClass dc = Scene.v().getSootClassUnsafe(getDeclaringClass());
			Type rt = Scene.v().getTypeUnsafe(returnType);
			if(dc == null || rt == null)
				throw new RuntimeException("Error: Unable to parse the declaring class or return type to a soot object");
			ArrayList<Type> parameterTypes = null;
			if(argumentTypes != null) {
				parameterTypes = new ArrayList<>();
				for(String s : argumentTypes) {
					Type ttt = Scene.v().getTypeUnsafe(s);
					if(ttt != null)
						parameterTypes.add(ttt);
				}
			}
			if((parameterTypes != null || argumentTypes != null) && 
					(parameterTypes == null || argumentTypes == null || parameterTypes.size() != this.argumentTypes.size()))
				throw new RuntimeException("Error: Unable to parse one or more argument types to a soot object.");
			SootMethod ret = Scene.v().makeMethodRef(dc, name, parameterTypes, rt, isStatic()).resolve();
			if(ret == null)
				throw new RuntimeException("Error: Unable to resolve the soot method.");
			return ret;
		}
	}

	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public SootMethodContainer readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static SootMethodContainer readXMLStatic(String filePath, Path path) throws Exception {
		return new SootMethodContainer().readXML(filePath, path);
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
			return Collections.singleton(SootMethodContainer.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}
	
	@XStreamOmitField
	private static Map<SootMethod, SootMethodContainer> sootMethodContainers;
	private static final Object lock = new Object();
	
	public static SootMethodContainer makeSootMethodContainer(SootMethod sm){
		synchronized(lock) {
			SootMethodContainer ret = null;
			if(sootMethodContainers == null){
				sootMethodContainers = new HashMap<>();
				ret = new SootMethodContainer(sm);
				sootMethodContainers.put(sm, ret);
			}else{
				ret = sootMethodContainers.get(sm);
				if(ret == null){
					ret = new SootMethodContainer(sm);
					sootMethodContainers.put(sm, ret);
				}
			}
			return ret;
		}
	}
	
	public static void reset(){
		synchronized(lock) {
			sootMethodContainers = null;
		}
	}

}
