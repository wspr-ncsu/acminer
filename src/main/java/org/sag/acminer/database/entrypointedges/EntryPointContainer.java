package org.sag.acminer.database.entrypointedges;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.sag.acminer.database.entrypointedges.IEntryPointEdgesDatabase.Type;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.tuple.Pair;
import org.sag.common.xstream.NamedCollectionConverterWithSize;
import org.sag.common.xstream.XStreamInOut;
import org.sag.common.xstream.XStreamInOut.XStreamInOutInterface;
import org.sag.soot.xstream.SootClassContainer;
import org.sag.soot.xstream.SootMethodContainer;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("EntryPointContainer")
public class EntryPointContainer implements XStreamInOutInterface {

	@XStreamAlias("EntryPoint")
	private SootMethodContainer entryPoint;
	
	@XStreamAlias("Stub")
	private SootClassContainer stub;
	
	@XStreamAlias("EntryPointEdges")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"EntryPointEdge"},types={EntryPointEdge.class})
	private LinkedHashSet<EntryPointEdge> ieEntryPoints;
	
	//for reading in from xml only
	protected EntryPointContainer() {}
	
	protected EntryPointContainer(EntryPoint ep, Map<EntryPoint,Pair<Type,Map<SootMethod,Pair<Set<Unit>,Set<Integer>>>>> data) {
		this.entryPoint = ep.getEntryPoint() == null ? null : SootMethodContainer.makeSootMethodContainer(ep.getEntryPoint());
		this.stub = ep.getStub() == null ? null : SootClassContainer.makeSootClassContainer(ep.getStub());
		this.ieEntryPoints = new LinkedHashSet<>();
		for(EntryPoint ieep : data.keySet()) {
			ieEntryPoints.add(new EntryPointEdge(ieep, data.get(ieep)));
		}
	}
	
	@Override
	public boolean equals(Object o){
		if(this == o)
			return true;
		if(o == null || !(o instanceof EntryPointContainer))
			return false;
		EntryPointContainer other = (EntryPointContainer)o;
		return Objects.equals(entryPoint, other.entryPoint) && Objects.equals(stub, other.stub) && Objects.equals(ieEntryPoints, other.ieEntryPoints);
	}
	
	@Override
	public int hashCode(){
		int hash = 17;
		hash = 31 * hash + Objects.hashCode(entryPoint);
		hash = 31 * hash + Objects.hashCode(stub);
		hash = 31 * hash + Objects.hashCode(ieEntryPoints);
		return hash;
	}
	
	@Override
	public String toString() {
		return toString("");
	}
	
	public String toString(String spacer) {
		StringBuilder sb = new StringBuilder();
		sb.append(spacer).append("EntryPoint: ").append("{" + Objects.toString(stub) + " : " + Objects.toString(entryPoint) + "}").append("\n");
		sb.append(spacer).append("  Referenced Entry Points:\n");
		for(EntryPointEdge s : ieEntryPoints) {
			sb.append(s.toString(spacer + "    "));
		}
		return sb.toString();
	}
	
	public SootMethod getEntryPoint() {
		if(entryPoint == null)
			return null;
		return entryPoint.toSootMethod();
	}
	
	public SootClass getStub() {
		if(stub == null)
			return null;
		return stub.toSootClass();
	}
	
	public Map<EntryPoint,Pair<Type,Map<SootMethod,Pair<Set<Unit>,Set<Integer>>>>> getData() {
		Map<EntryPoint,Pair<Type,Map<SootMethod,Pair<Set<Unit>,Set<Integer>>>>> ret = new LinkedHashMap<>();
		for(EntryPointEdge ieep : ieEntryPoints) {
			ret.put(new EntryPoint(ieep.getReferencedEntryPoint(),ieep.getReferencedStub()), ieep.getData());
		}
		return ret;
	}
	
	public SootMethodContainer getEntryPointContainer() {
		return entryPoint;
	}
	
	public SootClassContainer getStubContainer() {
		return stub;
	}
	
	public Set<EntryPointEdge> getReferenceEntryPointContainers() {
		return new LinkedHashSet<>(ieEntryPoints);
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}
	
	public EntryPointContainer readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this,filePath, path);
	}
	
	public static EntryPointContainer readXMLStatic(String filePath, Path path) throws Exception {
		return new EntryPointContainer().readXML(filePath, path);
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
				SootClassContainer.getXStreamSetupStatic().getOutputGraph(in);
				EntryPointEdge.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(EntryPointContainer.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}
	
}
