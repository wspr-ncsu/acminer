package org.sag.acminer.phases.bindergroups;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
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

@XStreamAlias("ProxyBContainer")
public class ProxyBContainer implements XStreamInOutInterface, Comparable<ProxyBContainer> {
	
	@XStreamAlias("BinderProxy")
	private SootClassContainer binderProxy;
	
	@XStreamAlias("ProxyMethods")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"ProxyMethodBContainer"},types={ProxyMethodBContainer.class})
	private LinkedHashSet<ProxyMethodBContainer> proxyMethods;

	//for reading in from xml only
	protected ProxyBContainer(){}
		
	public ProxyBContainer(SootClass sc, Map<SootMethod,Set<Integer>> proxyMethods){
		if(sc == null)
			throw new IllegalArgumentException();
		this.binderProxy = SootClassContainer.makeSootClassContainer(sc);
		this.proxyMethods = null;
		if(proxyMethods != null && !proxyMethods.isEmpty()){
			this.proxyMethods = new LinkedHashSet<>();
			for(SootMethod m : proxyMethods.keySet()){
				this.proxyMethods.add(new ProxyMethodBContainer(m,proxyMethods.get(m)));
			}
			this.proxyMethods = SortingMethods.sortSet(this.proxyMethods);
		}
	}
	
	@Override
	public boolean equals(Object o){
		if(this == o)
			return true;
		if(o == null || !(o instanceof ProxyBContainer))
			return false;
		ProxyBContainer other = (ProxyBContainer)o;
		return Objects.equals(binderProxy, other.binderProxy);
	}
	
	@Override
	public int hashCode(){
		int hash = 17;
		hash = 31 * hash + Objects.hashCode(binderProxy);
		return hash;
	}
	
	@Override
	public int compareTo(ProxyBContainer o) {
		return this.binderProxy.compareTo(o.binderProxy);
	}
	
	public SootClassContainer getBinderProxy(){
		return binderProxy;
	}
	
	public Set<ProxyMethodBContainer> getProxyMethods(){
		if(proxyMethods == null)
			return new LinkedHashSet<>();
		return new LinkedHashSet<>(proxyMethods);
	}

	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public ProxyBContainer readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static ProxyBContainer readXMLStatic(String filePath, Path path) throws Exception {
		return new ProxyBContainer().readXML(filePath, path);
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
				ProxyMethodBContainer.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(ProxyBContainer.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}
	
}
