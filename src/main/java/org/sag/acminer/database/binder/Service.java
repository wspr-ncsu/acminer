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

@XStreamAlias("Service")
public final class Service implements XStreamInOutInterface, Comparable<Service> {
	
	@XStreamOmitField
	private volatile SootClass sc;
	
	@XStreamAlias("ServiceName")
	private volatile String name;

	@XStreamAlias("ServicePlaceHolder")
	private volatile boolean placeHolder;
	
	@XStreamAlias("Stub")
	private volatile Stub stub;
	
	@XStreamAlias("EntryPoints")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"EntryPoint"},types={EntryPoint.class})
	private volatile LinkedHashSet<EntryPoint> entryPoints;
	
	@XStreamOmitField
	private volatile Map<SootMethod,Set<Integer>> entryPointsToIds;
	
	@XStreamOmitField
	private volatile boolean loaded;
	
	private Service() {}
	
	Service(SootClass service, AtomicLong placeHolderCount, Stub stub, Map<SootMethod,Set<Integer>> epsToIds) {
		Objects.requireNonNull(placeHolderCount);
		Objects.requireNonNull(stub);
		if(service == null) {
			this.sc = null;
			this.name = "Service_Place_Holder_" + placeHolderCount.getAndIncrement();
			this.placeHolder = true;
		} else {
			this.sc = service;
			this.name = service.getName();
			this.placeHolder = false;
		}
		if(epsToIds != null && !epsToIds.isEmpty()) {
			this.entryPoints = new LinkedHashSet<>();
			ImmutableMap.Builder<SootMethod, Set<Integer>> b = ImmutableMap.builder();
			for(SootMethod ep : epsToIds.keySet()) {
				this.entryPoints.add(new EntryPoint(ep,this,epsToIds.get(ep)));
				b.put(ep, ImmutableSet.copyOf(epsToIds.get(ep)));
			}
			this.entryPointsToIds = b.build();
		} else {
			this.entryPoints = null;
			this.entryPointsToIds = null;
		}
		this.stub = stub;
		this.loaded = true;
	}

	@Override
	public int compareTo(Service o) {
		return SortingMethods.sComp.compare(name, o.name);
	}
	
	@Override
	public int hashCode() {
		int i = 17;
		i = i * 31 + Objects.hashCode(name);
		i = i * 31 + (placeHolder ? 0 : 1);
		i = i * 31 + Objects.hashCode(entryPoints);
		return i;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == this)
			return true;
		if(o == null || !(o instanceof Service))
			return false;
		Service p = (Service)o;
		return Objects.equals(name,p.name) && Objects.equals(entryPoints, p.entryPoints) 
				&& placeHolder == p.placeHolder;
	}
	
	@Override
	public String toString() {
		return toString("");
	}
	
	public String toString(String spacer) {
		StringBuilder sb = new StringBuilder();
		sb.append(spacer).append("Service: ").append(name).append("\n");
		if(entryPoints != null) {
			for(EntryPoint m : entryPoints) {
				sb.append(m.toString(spacer + "  ")).append("\n");
			}
		}
		return sb.toString();
	}
	
	public void initSoot() {
		if(!loaded) {
			if(!placeHolder)
				sc = Scene.v().getSootClass(name);
			if(entryPoints != null) {
				ImmutableMap.Builder<SootMethod, Set<Integer>> b = ImmutableMap.builder();
				for(EntryPoint m : entryPoints) {
					m.initSoot();
					b.put(m.getSootMethod(), m.getTransactionIds());
				}
				entryPointsToIds = b.build();
			}
			loaded = true;
		}
	}
	
	public Stub getInterface() {
		return stub;
	}
	
	public boolean isPlaceHolder() {
		return placeHolder;
	}
	
	public SootClass getServiceClass() {
		if(!loaded)
			throw new RuntimeException("Error: The Soot objects have not been resolved.");
		if(placeHolder)
			throw new RuntimeException("Error: Cannot resolve the service class for a placeholder.");
		return sc;
	}
	
	public String getServiceName() {
		return name;
	}
	
	public Set<EntryPoint> getEntryPoints() {
		if(entryPoints == null)
			return ImmutableSet.of();
		return ImmutableSet.copyOf(entryPoints);
	}
	
	public Map<SootMethod,Set<Integer>> getEntryPointsToTransactionIds() {
		if(!loaded)
			throw new RuntimeException("Error: The Soot objects have not been resolved.");
		if(entryPointsToIds == null)
			return ImmutableMap.of();
		return entryPointsToIds;
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public Service readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static Service readXMLStatic(String filePath, Path path) throws Exception {
		return new Service().readXML(filePath, path);
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
				EntryPoint.getXStreamSetupStatic().getOutputGraph(in);
				Stub.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(Service.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsIdRef(xstream);
		}
		
	}

}
