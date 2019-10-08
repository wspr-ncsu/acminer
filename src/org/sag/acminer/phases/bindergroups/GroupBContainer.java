package org.sag.acminer.phases.bindergroups;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.sag.common.tools.SortingMethods;
import org.sag.xstream.XStreamInOut;
import org.sag.xstream.XStreamInOut.XStreamInOutInterface;
import org.sag.xstream.xstreamconverters.NamedCollectionConverterWithSize;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import soot.SootClass;
import soot.SootMethod;

@XStreamAlias("GroupBContainer")
public final class GroupBContainer implements XStreamInOutInterface, Comparable<GroupBContainer> {

	@XStreamAlias("InterfaceBContainer")
	private InterfaceBContainer binderInterface;
	
	@XStreamAlias("BinderProxies")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"ProxyBContainer"},types={ProxyBContainer.class})
	private LinkedHashSet<ProxyBContainer> binderProxies;
	
	@XStreamAlias("BinderStubs")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"StubBContainer"},types={StubBContainer.class})
	private LinkedHashSet<StubBContainer> binderStubs;
	
	//for reading in from xml only
	protected GroupBContainer(){}
	
	public GroupBContainer(SootClass iisc, Set<SootMethod> iiscMethods, Map<SootClass,Map<SootMethod,Set<Integer>>> binderProxies,
			Map<SootClass,Map<SootMethod,Set<Integer>>> binderStubs, Map<SootClass,Set<Integer>> binderStubsToAllTransactionIds){
		
		if(iisc == null)
			throw new IllegalArgumentException();
		if(binderStubsToAllTransactionIds == null)
			binderStubsToAllTransactionIds = Collections.emptyMap();
		this.binderInterface = new InterfaceBContainer(iisc,iiscMethods);
		this.binderProxies = null;
		this.binderStubs = null;
		if(binderProxies != null && !binderProxies.isEmpty()){
			this.binderProxies = new LinkedHashSet<>();
			for(SootClass proxy : binderProxies.keySet()){
				this.binderProxies.add(new ProxyBContainer(proxy, binderProxies.get(proxy)));
			}
			this.binderProxies = SortingMethods.sortSet(this.binderProxies);
		}
		if(binderStubs != null && !binderStubs.isEmpty()){
			this.binderStubs = new LinkedHashSet<>();
			for(SootClass stub : binderStubs.keySet()){
				this.binderStubs.add(new StubBContainer(stub, binderStubs.get(stub), binderStubsToAllTransactionIds.get(stub)));
			}
			this.binderStubs = SortingMethods.sortSet(this.binderStubs);
		}
	}
	
	@Override
	public boolean equals(Object o){
		if(this == o)
			return true;
		if(o == null || !(o instanceof GroupBContainer))
			return false;
		GroupBContainer other = (GroupBContainer)o;
		return Objects.equals(binderInterface, other.binderInterface);
	}
	
	@Override
	public int hashCode(){
		int hash = 17;
		hash = 31 * hash + Objects.hashCode(binderInterface);
		return hash;
	}
	
	@Override
	public int compareTo(GroupBContainer o) {
		return this.binderInterface.compareTo(o.binderInterface);
	}
	
	public InterfaceBContainer getBinderInterface(){
		return binderInterface;
	}
	
	public Set<ProxyBContainer> getBinderProxies(){
		if(binderProxies == null)
			return new LinkedHashSet<>();
		return new LinkedHashSet<>(binderProxies);
	}
	
	public Set<StubBContainer> getBinderStubs(){
		if(binderStubs == null)
			return new LinkedHashSet<>();
		return new LinkedHashSet<>(binderStubs);
	}

	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public GroupBContainer readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static GroupBContainer readXMLStatic(String filePath, Path path) throws Exception {
		return new GroupBContainer().readXML(filePath, path);
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
				InterfaceBContainer.getXStreamSetupStatic().getOutputGraph(in);
				ProxyBContainer.getXStreamSetupStatic().getOutputGraph(in);
				StubBContainer.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(GroupBContainer.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}

}
