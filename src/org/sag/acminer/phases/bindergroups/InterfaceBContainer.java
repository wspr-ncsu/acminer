package org.sag.acminer.phases.bindergroups;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.sag.common.tools.SortingMethods;
import org.sag.soot.xstream.SootClassContainer;
import org.sag.soot.xstream.SootMethodContainer;
import org.sag.xstream.XStreamInOut;
import org.sag.xstream.XStreamInOut.XStreamInOutInterface;
import org.sag.xstream.xstreamconverters.NamedCollectionConverterWithSize;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import soot.SootClass;
import soot.SootMethod;

@XStreamAlias("InterfaceBContainer")
public class InterfaceBContainer implements XStreamInOutInterface, Comparable<InterfaceBContainer> {
	
	@XStreamAlias("BinderInterface")
	private SootClassContainer binderInterface;
	
	@XStreamAlias("InterfaceMethods")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"InterfaceMethod"},types={SootMethodContainer.class})
	private LinkedHashSet<SootMethodContainer> interfaceMethods;

	//for reading in from xml only
	protected InterfaceBContainer(){}
		
	public InterfaceBContainer(SootClass sc, Set<SootMethod> interfaceMethods){
		if(sc == null)
			throw new IllegalArgumentException();
		this.binderInterface = SootClassContainer.makeSootClassContainer(sc);
		this.interfaceMethods = null;
		if(interfaceMethods != null && !interfaceMethods.isEmpty()){
			this.interfaceMethods = new LinkedHashSet<>();
			for(SootMethod m : interfaceMethods){
				this.interfaceMethods.add(SootMethodContainer.makeSootMethodContainer(m));
			}
			this.interfaceMethods = SortingMethods.sortSet(this.interfaceMethods);
		}
	}
	
	@Override
	public boolean equals(Object o){
		if(this == o)
			return true;
		if(o == null || !(o instanceof InterfaceBContainer))
			return false;
		InterfaceBContainer other = (InterfaceBContainer)o;
		return Objects.equals(binderInterface, other.binderInterface);
	}
	
	@Override
	public int hashCode(){
		int hash = 17;
		hash = 31 * hash + Objects.hashCode(binderInterface);
		return hash;
	}
	
	@Override
	public int compareTo(InterfaceBContainer o) {
		return this.binderInterface.compareTo(o.binderInterface);
	}
	
	public SootClassContainer getBinderInterface(){
		return binderInterface;
	}
	
	public Set<SootMethodContainer> getInterfaceMethods(){
		if(interfaceMethods == null)
			return new LinkedHashSet<>();
		return new LinkedHashSet<>(interfaceMethods);
	}

	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public InterfaceBContainer readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static InterfaceBContainer readXMLStatic(String filePath, Path path) throws Exception {
		return new InterfaceBContainer().readXML(filePath, path);
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
				SootClassContainer.getXStreamSetupStatic().getOutputGraph(in);
				SootMethodContainer.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(InterfaceBContainer.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}

}
