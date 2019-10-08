package org.sag.acminer.database.binder;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.sag.soot.xstream.SootMethodContainer;
import org.sag.xstream.XStreamInOut;
import org.sag.xstream.XStreamInOut.XStreamInOutInterface;
import org.sag.xstream.xstreamconverters.NamedCollectionConverterWithSize;

import com.google.common.collect.ImmutableSet;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import soot.SootMethod;

@XStreamAlias("ProxyMethod")
public final class ProxyMethod implements XStreamInOutInterface, Comparable<ProxyMethod>{
	
	@XStreamOmitField
	private volatile SootMethod sm;

	@XStreamAlias("SootMethodContainer")
	private volatile SootMethodContainer method;
	
	@XStreamAlias("Proxy")
	private volatile Proxy proxy;
	
	@XStreamAlias("TransactionIds")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"TransactionId"},types={Integer.class})
	private volatile LinkedHashSet<Integer> transactionIds;
	
	@XStreamOmitField
	private volatile boolean loaded;
	
	private ProxyMethod() {}
	
	ProxyMethod(SootMethod proxyMethod, Proxy proxy, Set<Integer> ids) {
		Objects.requireNonNull(proxyMethod);
		Objects.requireNonNull(proxy);
		this.sm = proxyMethod;
		this.proxy = proxy;
		this.method = SootMethodContainer.makeSootMethodContainer(proxyMethod);
		if(ids != null && !ids.isEmpty()) {
			this.transactionIds = new LinkedHashSet<>(ids);
		} else {
			this.transactionIds = null;
		}
		this.loaded = true;
	}
	
	@Override
	public int compareTo(ProxyMethod o) {
		return method.compareTo(o.method);
	}
	
	@Override
	public int hashCode() {
		int i = 17;
		i = i * 31 + Objects.hashCode(method);
		i = i * 31 + Objects.hashCode(transactionIds);
		return i;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == this)
			return true;
		if(o == null || !(o instanceof ProxyMethod))
			return false;
		ProxyMethod p = (ProxyMethod)o;
		return Objects.equals(method,p.method) && Objects.equals(transactionIds, p.transactionIds);
	}
	
	@Override
	public String toString() {
		return toString("");
	}
	
	public String toString(String spacer) {
		StringBuilder sb = new StringBuilder();
		sb.append(spacer).append("Proxy Method: ").append(method.getSignature());
		if(transactionIds != null)
			sb.append(" ").append(transactionIds);
		return sb.toString();
	}
	
	public void initSoot() {
		if(!loaded) {
			sm = method.toSootMethod();
			loaded = true;
		}
	}
	
	public SootMethod getSootMethod() {
		if(!loaded)
			throw new RuntimeException("Error: The Soot objects have not been resolved.");
		return sm;
	}
	
	public SootMethodContainer getMethod() {
		return method;
	}
	
	public Proxy getProxy() {
		return proxy;
	}
	
	public Set<Integer> getTransactionIds() {
		if(transactionIds == null)
			return ImmutableSet.of();
		return ImmutableSet.copyOf(transactionIds);
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public ProxyMethod readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static ProxyMethod readXMLStatic(String filePath, Path path) throws Exception {
		return new ProxyMethod().readXML(filePath, path);
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
				Proxy.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(ProxyMethod.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsIdRef(xstream);
		}
		
	}
	
}
