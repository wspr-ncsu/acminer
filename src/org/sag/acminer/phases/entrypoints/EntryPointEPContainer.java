package org.sag.acminer.phases.entrypoints;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.sag.common.tools.SortingMethods;
import org.sag.soot.xstream.SootMethodContainer;
import org.sag.xstream.XStreamInOut;
import org.sag.xstream.XStreamInOut.XStreamInOutInterface;
import org.sag.xstream.xstreamconverters.NamedCollectionConverterWithSize;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import soot.SootMethod;

@XStreamAlias("EntryPointEPContainer")
public class EntryPointEPContainer implements XStreamInOutInterface {
	
	@XStreamAlias("EntryPoint")
	private SootMethodContainer entryPoint;
	
	@XStreamAlias("TransactionIds")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"TransactionId"},types={Integer.class})
	private LinkedHashSet<Integer> ids;
	
	//for reading in from xml only
	protected EntryPointEPContainer() {}
	
	public EntryPointEPContainer(SootMethod m, Set<Integer> ids) {
		if(m == null)
			throw new IllegalArgumentException();
		this.entryPoint = SootMethodContainer.makeSootMethodContainer(m);
		this.ids = null;
		if(ids != null && !ids.isEmpty()){
			this.ids = SortingMethods.sortSet(ids,SortingMethods.iComp);
		}
	}
	
	public boolean equals(Object o){
		if(this == o)
			return true;
		if(o == null || !(o instanceof EntryPointEPContainer))
			return false;
		EntryPointEPContainer other = (EntryPointEPContainer)o;
		return Objects.equals(entryPoint, other.entryPoint);
	}
	
	public int hashCode(){
		int hash = 17;
		hash = 31 * hash + Objects.hashCode(entryPoint);
		return hash;
	}
	
	public SootMethodContainer getEntryPoint(){
		return entryPoint;
	}
	
	public Set<Integer> getTransactionIds(){
		if(ids == null)
			return new LinkedHashSet<>();
		return new LinkedHashSet<>(ids);
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
		
	}
	
	public EntryPointEPContainer readXML(String filePath, Path path) throws Exception {
		return (EntryPointEPContainer)XStreamInOut.readXML(this,filePath, path);
	}
	
	public static EntryPointEPContainer readXMLStatic(String filePath, Path path) throws Exception {
		return new EntryPointEPContainer().readXML(filePath, path);
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
				SootMethodContainer.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(EntryPointEPContainer.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}

}
