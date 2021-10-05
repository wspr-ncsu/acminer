package org.sag.acminer.phases.bindergroups;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.sag.common.tools.SortingMethods;
import org.sag.common.xstream.NamedCollectionConverterWithSize;
import org.sag.common.xstream.XStreamInOut;
import org.sag.common.xstream.XStreamInOut.XStreamInOutInterface;
import org.sag.soot.xstream.SootMethodContainer;

import soot.SootMethod;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("ProxyMethodBContainer")
public class ProxyMethodBContainer implements XStreamInOutInterface, Comparable<ProxyMethodBContainer> {

	@XStreamAlias("ProxyMethod")
	private SootMethodContainer proxyMethod;
	
	@XStreamAlias("TransactionIds")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"TransactionId"},types={Integer.class})
	private LinkedHashSet<Integer> ids;
	
	//for reading in from xml only
	protected ProxyMethodBContainer() {}
	
	public ProxyMethodBContainer(SootMethod m, Set<Integer> ids) {
		if(m == null)
			throw new IllegalArgumentException();
		this.proxyMethod = SootMethodContainer.makeSootMethodContainer(m);
		this.ids = null;
		if(ids != null && !ids.isEmpty()){
			this.ids = SortingMethods.sortSet(ids,SortingMethods.iComp);
		}
	}
	
	@Override
	public boolean equals(Object o){
		if(this == o)
			return true;
		if(o == null || !(o instanceof ProxyMethodBContainer))
			return false;
		ProxyMethodBContainer other = (ProxyMethodBContainer)o;
		return Objects.equals(proxyMethod, other.proxyMethod);
	}
	
	@Override
	public int hashCode(){
		int hash = 17;
		hash = 31 * hash + Objects.hashCode(proxyMethod);
		return hash;
	}
	
	@Override
	public int compareTo(ProxyMethodBContainer o) {
		return this.proxyMethod.compareTo(o.proxyMethod);
	}
	
	public SootMethodContainer getProxyMethod(){
		return proxyMethod;
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
	
	public ProxyMethodBContainer readXML(String filePath, Path path) throws Exception {
		return (ProxyMethodBContainer)XStreamInOut.readXML(this,filePath, path);
	}
	
	public static ProxyMethodBContainer readXMLStatic(String filePath, Path path) throws Exception {
		return new ProxyMethodBContainer().readXML(filePath, path);
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
			return Collections.singleton(ProxyMethodBContainer.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}

}
