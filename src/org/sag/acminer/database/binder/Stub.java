package org.sag.acminer.database.binder;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.sag.common.tools.SortingMethods;
import org.sag.xstream.XStreamInOut;
import org.sag.xstream.XStreamInOut.XStreamInOutInterface;
import org.sag.xstream.xstreamconverters.NamedCollectionConverterWithSize;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

@XStreamAlias("Stub")
public final class Stub implements XStreamInOutInterface, Comparable<Stub> {
	
	@XStreamOmitField
	private volatile SootClass sc;
	
	@XStreamAlias("StubName")
	private volatile String name;

	@XStreamAlias("StubPlaceHolder")
	private volatile boolean placeHolder;
	
	@XStreamAlias("Interface")
	private volatile Interface iface;
	
	@XStreamAlias("Services")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"Service"},types={Service.class})
	private volatile LinkedHashSet<Service> services;
	
	@XStreamAlias("AllTransactionIds")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"TransactionId"},types={Integer.class})
	private volatile LinkedHashSet<Integer> allTransactionIds;
	
	@XStreamOmitField
	private volatile Map<SootClass,Map<SootMethod,Set<Integer>>> servicesToEntryPointsToIds;
	
	@XStreamOmitField
	private volatile boolean loaded;
	
	private Stub() {}
	
	Stub(SootClass stub, AtomicLong placeHolderCount, Set<Integer> ids, 
			Map<SootClass,Map<SootMethod,Set<Integer>>> servicesToEpsToIds) {
		Objects.requireNonNull(placeHolderCount);
		if(stub == null) {
			this.sc = null;
			this.name = "Stub_Place_Holder_" + placeHolderCount.getAndIncrement();
			this.placeHolder = true;
		} else {
			this.sc = stub;
			this.name = stub.getName();
			this.placeHolder = false;
		}
		if(ids != null && !ids.isEmpty()) {
			this.allTransactionIds = new LinkedHashSet<>(ids);
		} else {
			this.allTransactionIds = null;
		}
		if(servicesToEpsToIds != null && !servicesToEpsToIds.isEmpty()) {
			services = new LinkedHashSet<>();
			ImmutableMap.Builder<SootClass,Map<SootMethod,Set<Integer>>> b = ImmutableMap.builder();
			for(SootClass service : servicesToEpsToIds.keySet()) {
				services.add(new Service(service,placeHolderCount,this,servicesToEpsToIds.get(service)));
				Map<SootMethod,Set<Integer>> temp = servicesToEpsToIds.get(service);
				ImmutableMap.Builder<SootMethod,Set<Integer>> bb = ImmutableMap.builder();
				for(SootMethod ep : temp.keySet()) {
					bb.put(ep,ImmutableSet.copyOf(temp.get(ep)));
				}
				b.put(service,bb.build());
			}
			this.servicesToEntryPointsToIds = b.build();
		} else {
			this.services = null;
			this.servicesToEntryPointsToIds = null;
		}
		this.iface = null;
		this.loaded = true;
	}

	@Override
	public int compareTo(Stub o) {
		return SortingMethods.sComp.compare(name,o.name);
	}
	
	@Override
	public int hashCode() {
		int i = 17;
		i = i * 31 + Objects.hashCode(name);
		i = i * 31 + (placeHolder ? 0 : 1);
		i = i * 31 + Objects.hashCode(services);
		i = i * 31 + Objects.hashCode(allTransactionIds);
		return i;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == this)
			return true;
		if(o == null || !(o instanceof Stub))
			return false;
		Stub p = (Stub)o;
		return Objects.equals(name,p.name) && Objects.equals(services, p.services) 
				&& placeHolder == p.placeHolder && Objects.equals(allTransactionIds, p.allTransactionIds);
	}
	
	@Override
	public String toString() {
		return toString("");
	}
	
	public String toString(String spacer) {
		StringBuilder sb = new StringBuilder();
		sb.append(spacer).append("Stub: ").append(name);
		if(allTransactionIds != null)
			sb.append(" ").append(allTransactionIds);
		sb.append("\n");
		if(services != null) {
			for(Service m : services) {
				sb.append(m.toString(spacer + "  "));
			}
		}
		return sb.toString();
	}
	
	public void initSoot() {
		if(!loaded) {
			if(!placeHolder)
				sc = Scene.v().getSootClass(name);
			if(services != null) {
				ImmutableMap.Builder<SootClass, Map<SootMethod, Set<Integer>>> b = ImmutableMap.builder();
				for(Service m : services) {
					m.initSoot();
					b.put(m.getServiceClass(), m.getEntryPointsToTransactionIds());
				}
				servicesToEntryPointsToIds = b.build();
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
	
	public SootClass getStubClass() {
		if(!loaded)
			throw new RuntimeException("Error: The Soot objects have not been resolved.");
		if(placeHolder)
			throw new RuntimeException("Error: Cannot resolve the stub class for a placeholder.");
		return sc;
	}
	
	public String getStubName() {
		return name;
	}
	
	public Set<Service> getServices() {
		if(services == null)
			return ImmutableSet.of();
		return ImmutableSet.copyOf(services);
	}
	
	public Map<SootClass,Map<SootMethod,Set<Integer>>> getServicesToEntryPointsToTransactionIds() {
		if(!loaded)
			throw new RuntimeException("Error: The Soot objects have not been resolved.");
		if(servicesToEntryPointsToIds == null)
			return ImmutableMap.of();
		return servicesToEntryPointsToIds;
	}
	
	public Set<Integer> getAllTransactionIds() {
		if(allTransactionIds == null)
			return ImmutableSet.of();
		return ImmutableSet.copyOf(allTransactionIds);
	}
	
	void setInterface(Interface iface) {
		if(this.iface != null)
			throw new RuntimeException("Error: The interface is already set for '" + this.name + "'.");
		this.iface = iface;
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public Stub readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static Stub readXMLStatic(String filePath, Path path) throws Exception {
		return new Stub().readXML(filePath, path);
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
				Service.getXStreamSetupStatic().getOutputGraph(in);
				Interface.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(Stub.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsIdRef(xstream);
		}
		
	}

}
