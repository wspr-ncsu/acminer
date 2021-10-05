package org.sag.acminer.phases.entrypoints;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.sag.common.tools.SortingMethods;
import org.sag.common.xstream.NamedCollectionConverterWithSize;
import org.sag.common.xstream.XStreamInOut;
import org.sag.common.xstream.XStreamInOut.XStreamInOutInterface;
import org.sag.soot.xstream.SootClassContainer;

import soot.SootClass;
import soot.SootMethod;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("StubEPContainer")
public class StubEPContainer implements XStreamInOutInterface, Iterable<EntryPointEPContainer> {
	
	@XStreamAlias("Stub")
	private SootClassContainer stub;
	
	@XStreamAlias("EntryPoints")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"EntryPointEPContainer"},types={EntryPointEPContainer.class})
	private ArrayList<EntryPointEPContainer> eps;
	
	@XStreamAlias("AllTransactionIds")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"TransactionId"},types={Integer.class})
	private LinkedHashSet<Integer> allIds;
	
	//for reading in from xml only
	protected StubEPContainer(){}
		
	public StubEPContainer(SootClass sc, Set<Integer> allIds){
		this.stub = SootClassContainer.makeSootClassContainer(sc);
		eps = null;
		this.allIds = null;
		if(allIds != null && !allIds.isEmpty()){
			this.allIds = SortingMethods.sortSet(allIds,SortingMethods.iComp);
		}
	}
	
	public boolean equals(Object o){
		if(this == o)
			return true;
		if(o == null || !(o instanceof StubEPContainer))
			return false;
		StubEPContainer other = (StubEPContainer)o;
		return Objects.equals(stub, other.stub);
	}
	
	public int hashCode(){
		int hash = 17;
		hash = 31 * hash + Objects.hashCode(stub);
		return hash;
	}
	
	public void addMethod(SootMethod m, Set<Integer> ids){
		if(eps == null)
			eps = new ArrayList<>();
		eps.add(new EntryPointEPContainer(m,ids));
	}
	
	public void addMethod(EntryPointEPContainer m){
		if(eps == null)
			eps = new ArrayList<>();
		eps.add(m);
	}
	
	public SootClassContainer getStub(){
		return stub;
	}
	
	public Set<Integer> getAllTransactionIds(){
		if(allIds == null)
			return new LinkedHashSet<>();
		return new LinkedHashSet<>(allIds);
	}
	
	public List<EntryPointEPContainer> getEntryPoints(){
		if(eps == null)
			return new ArrayList<>();
		return new ArrayList<>(eps);
	}

	@Override
	public Iterator<EntryPointEPContainer> iterator() {
		if(eps == null)
			return Collections.<EntryPointEPContainer>emptyList().iterator();
		return eps.iterator();
	}

	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public StubEPContainer readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static StubEPContainer readXMLStatic(String filePath, Path path) throws Exception {
		return new StubEPContainer().readXML(filePath, path);
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
				EntryPointEPContainer.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(StubEPContainer.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}

}
