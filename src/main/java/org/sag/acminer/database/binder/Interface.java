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
import org.sag.soot.xstream.SootMethodContainer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

@XStreamAlias("Interface")
public final class Interface implements XStreamInOutInterface, Comparable<Interface> {
	
	@XStreamOmitField
	private volatile SootClass sc;
	
	@XStreamAlias("InterfaceName")
	private volatile String name;

	@XStreamAlias("InterfacePlaceHolder")
	private volatile boolean placeHolder;
	
	@XStreamAlias("InterfaceMethods")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"InterfaceMethod"},types={SootMethodContainer.class})
	private volatile LinkedHashSet<SootMethodContainer> interfaceMethods;
	
	@XStreamOmitField
	private volatile Set<SootMethod> methods;
	
	@XStreamAlias("Proxies")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"Proxy"},types={Proxy.class})
	private volatile LinkedHashSet<Proxy> proxies;
	
	@XStreamOmitField
	private volatile Map<SootClass,Map<SootMethod,Set<Integer>>> proxiesToProxyMethodsToIds;
	
	@XStreamAlias("Stubs")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"Stub"},types={Stub.class})
	private volatile LinkedHashSet<Stub> stubs;
	
	@XStreamOmitField
	private volatile Map<SootClass,Map<SootClass,Map<SootMethod,Set<Integer>>>> stubsToServicesToEntryPointsToIds;
	
	@XStreamOmitField
	private volatile boolean loaded;
	
	private Interface() {}
	
	Interface(SootClass iface, AtomicLong placeHolderCount, Set<SootMethod> methods, 
			Map<SootClass,Map<SootMethod,Set<Integer>>> proxiesToProxyMethodsToIds,
			Map<SootClass,Map<SootClass,Map<SootMethod,Set<Integer>>>> stubsToServicesToEntryPointsToIds, 
			Map<SootClass,Set<Integer>> stubsToAllTransactionIds,
			Map<SootClass,Stub> existingStubs) {
		Objects.requireNonNull(placeHolderCount);
		Objects.requireNonNull(existingStubs);
		if(iface == null) {
			this.sc = null;
			this.name = "Interface_Place_Holder_" + placeHolderCount.getAndIncrement();
			this.placeHolder = true;
		} else {
			this.sc = iface;
			this.name = iface.getName();
			this.placeHolder = false;
		}
		if(methods != null && !methods.isEmpty()) {
			this.interfaceMethods = new LinkedHashSet<>();
			for(SootMethod sm : methods) {
				this.interfaceMethods.add(SootMethodContainer.makeSootMethodContainer(sm));
			}
			this.methods = ImmutableSet.copyOf(methods);
		} else {
			this.interfaceMethods = null;
			this.methods = null;
		}
		if(proxiesToProxyMethodsToIds != null && !proxiesToProxyMethodsToIds.isEmpty()) {
			this.proxies = new LinkedHashSet<>();
			ImmutableMap.Builder<SootClass,Map<SootMethod,Set<Integer>>> b = ImmutableMap.builder();
			for(SootClass proxy : proxiesToProxyMethodsToIds.keySet()) {
				this.proxies.add(new Proxy(proxy,placeHolderCount,this,proxiesToProxyMethodsToIds.get(proxy)));
				Map<SootMethod,Set<Integer>> temp = proxiesToProxyMethodsToIds.get(proxy);
				ImmutableMap.Builder<SootMethod,Set<Integer>> bb = ImmutableMap.builder();
				for(SootMethod proxyMethod : temp.keySet()) {
					bb.put(proxyMethod,ImmutableSet.copyOf(temp.get(proxyMethod)));
				}
				b.put(proxy,bb.build());
			}
			this.proxiesToProxyMethodsToIds = b.build();
		} else {
			this.proxies = null;
			this.proxiesToProxyMethodsToIds = null;
		}
		if(stubsToServicesToEntryPointsToIds != null && !stubsToServicesToEntryPointsToIds.isEmpty()) {
			this.stubs = new LinkedHashSet<>();
			ImmutableMap.Builder<SootClass,Map<SootClass,Map<SootMethod,Set<Integer>>>> b = ImmutableMap.builder();
			for(SootClass stubClass : stubsToServicesToEntryPointsToIds.keySet()) {
				Stub stub = existingStubs.get(stubClass);
				if(stub == null) {
					stub = new Stub(stubClass, placeHolderCount, 
							stubsToAllTransactionIds == null ? null : stubsToAllTransactionIds.get(stubClass), 
									stubsToServicesToEntryPointsToIds.get(stubClass));
				}
				stub.setInterface(this);
				this.stubs.add(stub);
				Map<SootClass, Map<SootMethod, Set<Integer>>> temp = stubsToServicesToEntryPointsToIds.get(stubClass);
				ImmutableMap.Builder<SootClass, Map<SootMethod, Set<Integer>>> bb = ImmutableMap.builder();
				for(SootClass service : temp.keySet()) {
					Map<SootMethod, Set<Integer>> temp2 = temp.get(service);
					ImmutableMap.Builder<SootMethod, Set<Integer>> bbb = ImmutableMap.builder();
					for(SootMethod ep : temp2.keySet()) {
						bbb.put(ep, ImmutableSet.copyOf(temp2.get(ep)));
					}
					bb.put(service, bbb.build());
				}
				b.put(stubClass, bb.build());
			}
			this.stubsToServicesToEntryPointsToIds = b.build();
		} else {
			this.stubs = null;
			this.stubsToServicesToEntryPointsToIds = null;
		}
		this.loaded = true;
	}
	
	@Override
	public int compareTo(Interface o) {
		return SortingMethods.sComp.compare(name,o.name);
	}
	
	@Override
	public int hashCode() {
		int i = 17;
		i = i * 31 + Objects.hashCode(name);
		i = i * 31 + (placeHolder ? 0 : 1);
		i = i * 31 + Objects.hashCode(interfaceMethods);
		i = i * 31 + Objects.hashCode(proxies);
		i = i * 31 + Objects.hashCode(stubs);
		return i;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == this)
			return true;
		if(o == null || !(o instanceof Interface))
			return false;
		Interface p = (Interface)o;
		return Objects.equals(name,p.name) && Objects.equals(interfaceMethods, p.interfaceMethods) 
				&& placeHolder == p.placeHolder && Objects.equals(proxies, p.proxies) && Objects.equals(stubs, p.stubs);
	}
	
	@Override
	public String toString() {
		return toString("");
	}
	
	public String toString(String spacer) {
		StringBuilder sb = new StringBuilder();
		sb.append(spacer).append("Interface: ").append(name).append("\n");
		if(interfaceMethods != null) {
			sb.append(spacer).append("  Interface Methods:\n");
			for(SootMethodContainer m : interfaceMethods) {
				sb.append(spacer).append("    ").append(m.getSignature());
			}
		}
		if(proxies != null) {
			sb.append(spacer).append("  Proxies:\n");
			for(Proxy m : proxies) {
				sb.append(m.toString(spacer + "    "));
			}
		}
		if(stubs != null) {
			sb.append(spacer).append("  Stubs:\n");
			for(Stub m : stubs) {
				sb.append(m.toString(spacer + "    "));
			}
		}
		return sb.toString();
	}
	
	public void initSoot() {
		if(!loaded) {
			if(!placeHolder)
				sc = Scene.v().getSootClass(name);
			if(interfaceMethods != null) {
				ImmutableSet.Builder<SootMethod> b = ImmutableSet.builder();
				for(SootMethodContainer m : interfaceMethods) {
					b.add(m.toSootMethod());
				}
				methods = b.build();
			}
			if(proxies != null) {
				ImmutableMap.Builder<SootClass, Map<SootMethod, Set<Integer>>> b = ImmutableMap.builder();
				for(Proxy p : proxies) {
					b.put(p.getProxyClass(), p.getProxyMethodsToTransactionIds());
				}
				proxiesToProxyMethodsToIds = b.build();
			}
			if(stubs != null) {
				ImmutableMap.Builder<SootClass, Map<SootClass, Map<SootMethod, Set<Integer>>>> b = ImmutableMap.builder();
				for(Stub p : stubs) {
					b.put(p.getStubClass(), p.getServicesToEntryPointsToTransactionIds());
				}
				stubsToServicesToEntryPointsToIds = b.build();
			}
			loaded = true;
		}
	}
	
	public boolean isPlaceHolder() {
		return placeHolder;
	}
	
	public SootClass getInterfaceClass() {
		if(!loaded)
			throw new RuntimeException("Error: The Soot objects have not been resolved.");
		if(placeHolder)
			throw new RuntimeException("Error: Cannot resolve the interface class for a placeholder.");
		return sc;
	}
	
	public String getInterfaceName() {
		return name;
	}
	
	public Set<SootMethodContainer> getInterfaceMethods() {
		if(interfaceMethods == null)
			return ImmutableSet.of();
		return ImmutableSet.copyOf(interfaceMethods);
	}
	
	public Set<SootMethod> getSootInterfaceMethods() {
		if(!loaded)
			throw new RuntimeException("Error: The Soot objects have not been resolved.");
		if(methods == null)
			return ImmutableSet.of();
		return methods;
	}
	
	public Set<Proxy> getProxies() {
		if(proxies == null)
			return ImmutableSet.of();
		return ImmutableSet.copyOf(proxies);
	}
	
	public Map<SootClass,Map<SootMethod,Set<Integer>>> getProxiesToProxyMethodsToTransactionIds() {
		if(!loaded)
			throw new RuntimeException("Error: The Soot objects have not been resolved.");
		if(proxiesToProxyMethodsToIds == null)
			return ImmutableMap.of();
		return proxiesToProxyMethodsToIds;
	}
	
	public Set<Stub> getStubs() {
		if(stubs == null)
			return ImmutableSet.of();
		return ImmutableSet.copyOf(stubs);
	}
	
	public Map<SootClass,Map<SootClass,Map<SootMethod,Set<Integer>>>> getStubsToServicesToEntryPointsToIds() {
		if(!loaded)
			throw new RuntimeException("Error: The Soot objects have not been resolved.");
		if(stubsToServicesToEntryPointsToIds == null)
			return ImmutableMap.of();
		return stubsToServicesToEntryPointsToIds;
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public Interface readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static Interface readXMLStatic(String filePath, Path path) throws Exception {
		return new Interface().readXML(filePath, path);
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
				Stub.getXStreamSetupStatic().getOutputGraph(in);
				Proxy.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(Interface.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsIdRef(xstream);
		}
		
	}
	
}
