package org.sag.acminer.database.binder;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.sag.common.tools.SortingMethods;
import org.sag.common.xstream.NamedCollectionConverterWithSize;
import org.sag.common.xstream.XStreamInOut;
import org.sag.common.xstream.XStreamInOut.XStreamInOutInterface;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

@XStreamAlias("Proxy")
public final class Proxy implements XStreamInOutInterface, Comparable<Proxy> {
	
	@XStreamOmitField
	private volatile SootClass sc;
	
	@XStreamAlias("ProxyName")
	private volatile String name;

	@XStreamAlias("ProxyPlaceHolder")
	private volatile boolean placeHolder;
	
	@XStreamAlias("Interface")
	private volatile Interface iface;
	
	@XStreamAlias("ProxyMethods")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"ProxyMethod"},types={ProxyMethod.class})
	private volatile LinkedHashSet<ProxyMethod> proxyMethods;
	
	@XStreamOmitField
	private volatile Map<SootMethod,Set<Integer>> proxyMethodsToIds;
	
	@XStreamOmitField
	private volatile boolean loaded;
	
	private Proxy() {}
	
	Proxy(SootClass proxy, AtomicLong placeHolderCount, Interface iface, Map<SootMethod,Set<Integer>> proxyMethodsToIds) {
		Objects.requireNonNull(placeHolderCount);
		Objects.requireNonNull(iface);
		if(proxy == null) {
			this.sc = null;
			this.name = "Proxy_Place_Holder_" + placeHolderCount.getAndIncrement();
			this.placeHolder = true;
		} else {
			this.sc = proxy;
			this.name = proxy.getName();
			this.placeHolder = false;
		}
		if(proxyMethodsToIds != null && !proxyMethodsToIds.isEmpty()) {
			this.proxyMethods = new LinkedHashSet<>();
			ImmutableMap.Builder<SootMethod, Set<Integer>> b = ImmutableMap.builder();
			for(SootMethod sm : proxyMethodsToIds.keySet()) {
				this.proxyMethods.add(new ProxyMethod(sm, this, proxyMethodsToIds.get(sm)));
				b.put(sm, ImmutableSet.copyOf(proxyMethodsToIds.get(sm)));
			}
			this.proxyMethodsToIds = b.build();
		} else {
			this.proxyMethods = null;
			this.proxyMethodsToIds = null;
		}
		this.iface = iface;
		this.loaded = true;
	}

	@Override
	public int compareTo(Proxy o) {
		return SortingMethods.sComp.compare(name,o.name);
	}
	
	@Override
	public int hashCode() {
		int i = 17;
		i = i * 31 + Objects.hashCode(name);
		i = i * 31 + (placeHolder ? 0 : 1);
		i = i * 31 + Objects.hashCode(proxyMethods);
		return i;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == this)
			return true;
		if(o == null || !(o instanceof Proxy))
			return false;
		Proxy p = (Proxy)o;
		return Objects.equals(name,p.name) && Objects.equals(proxyMethods, p.proxyMethods) 
				&& placeHolder == p.placeHolder;
	}
	
	@Override
	public String toString() {
		return toString("");
	}
	
	public String toString(String spacer) {
		StringBuilder sb = new StringBuilder();
		sb.append(spacer).append("Proxy: ").append(name).append("\n");
		if(proxyMethods != null) {
			for(ProxyMethod m : proxyMethods) {
				sb.append(m.toString(spacer + "  ")).append("\n");
			}
		}
		return sb.toString();
	}
	
	public void initSoot() {
		if(!loaded) {
			if(!placeHolder)
				sc = Scene.v().getSootClass(name);
			if(proxyMethods != null) {
				ImmutableMap.Builder<SootMethod, Set<Integer>> b = ImmutableMap.builder();
				for(ProxyMethod m : proxyMethods) {
					m.initSoot();
					b.put(m.getSootMethod(), m.getTransactionIds());
				}
				proxyMethodsToIds = b.build();
			}
			loaded = true;
		}
	}
	
	public Interface getInterface() {
		return iface;
	}
	
	public boolean isPlaceHolder() {
		return placeHolder;
	}
	
	public SootClass getProxyClass() {
		if(!loaded)
			throw new RuntimeException("Error: The Soot objects have not been resolved.");
		if(placeHolder)
			throw new RuntimeException("Error: Cannot resolve the proxy class for a placeholder.");
		return sc;
	}
	
	public String getProxyName() {
		return name;
	}
	
	public Set<ProxyMethod> getProxyMethods() {
		if(proxyMethods == null)
			return ImmutableSet.of();
		return ImmutableSet.copyOf(proxyMethods);
	}
	
	public Map<SootMethod,Set<Integer>> getProxyMethodsToTransactionIds() {
		if(!loaded)
			throw new RuntimeException("Error: The Soot objects have not been resolved.");
		if(proxyMethodsToIds == null)
			return ImmutableMap.of();
		return proxyMethodsToIds;
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public Proxy readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static Proxy readXMLStatic(String filePath, Path path) throws Exception {
		return new Proxy().readXML(filePath, path);
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
				ProxyMethod.getXStreamSetupStatic().getOutputGraph(in);
				Interface.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(Proxy.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsIdRef(xstream);
		}
		
	}

}
