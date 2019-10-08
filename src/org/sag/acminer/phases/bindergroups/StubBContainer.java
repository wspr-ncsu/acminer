package org.sag.acminer.phases.bindergroups;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.sag.common.tools.SortingMethods;
import org.sag.soot.xstream.SootClassContainer;
import org.sag.xstream.XStreamInOut;
import org.sag.xstream.XStreamInOut.XStreamInOutInterface;
import org.sag.xstream.xstreamconverters.NamedCollectionConverterWithSize;
import soot.SootClass;
import soot.SootMethod;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("StubBContainer")
public class StubBContainer implements XStreamInOutInterface, Comparable<StubBContainer> {
	
	@XStreamAlias("BinderStub")
	private SootClassContainer binderStub;
	
	@XStreamAlias("StubMethods")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"StubMethodBContainer"},types={StubMethodBContainer.class})
	private LinkedHashSet<StubMethodBContainer> stubMethods;
	
	@XStreamAlias("AllTransactionIds")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"TransactionId"},types={Integer.class})
	private LinkedHashSet<Integer> allIds;

	//for reading in from xml only
	protected StubBContainer(){}
		
	public StubBContainer(SootClass sc, Map<SootMethod,Set<Integer>> stubMethods, Set<Integer> allIds){
		if(sc == null)
			throw new IllegalArgumentException();
		this.binderStub = SootClassContainer.makeSootClassContainer(sc);
		this.stubMethods = null;
		this.allIds = null;
		if(stubMethods != null && !stubMethods.isEmpty()){
			this.stubMethods = new LinkedHashSet<>();
			for(SootMethod m : stubMethods.keySet()){
				this.stubMethods.add(new StubMethodBContainer(m,stubMethods.get(m)));
			}
			this.stubMethods = SortingMethods.sortSet(this.stubMethods);
		}
		if(allIds != null && !allIds.isEmpty()){
			this.allIds = SortingMethods.sortSet(allIds,SortingMethods.iComp);
		}
	}
	
	public boolean equals(Object o){
		if(this == o)
			return true;
		if(o == null || !(o instanceof StubBContainer))
			return false;
		StubBContainer other = (StubBContainer)o;
		return Objects.equals(binderStub, other.binderStub);
	}
	
	public int hashCode(){
		int hash = 17;
		hash = 31 * hash + Objects.hashCode(binderStub);
		return hash;
	}
	
	@Override
	public int compareTo(StubBContainer o) {
		return this.binderStub.compareTo(o.binderStub);
	}
	
	public SootClassContainer getBinderStub(){
		return binderStub;
	}
	
	public Set<StubMethodBContainer> getStubMethods(){
		if(stubMethods == null)
			return new LinkedHashSet<>();
		return new LinkedHashSet<>(stubMethods);
	}
	
	public Set<Integer> getAllTransactionIds(){
		if(allIds == null)
			return new LinkedHashSet<>();
		return new LinkedHashSet<>(allIds);
	}

	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public StubBContainer readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static StubBContainer readXMLStatic(String filePath, Path path) throws Exception {
		return new StubBContainer().readXML(filePath, path);
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
				StubMethodBContainer.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(StubBContainer.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}
	
}
