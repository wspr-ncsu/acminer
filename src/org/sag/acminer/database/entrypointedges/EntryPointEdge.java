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
import org.sag.soot.xstream.SootClassContainer;
import org.sag.soot.xstream.SootMethodContainer;
import org.sag.xstream.XStreamInOut;
import org.sag.xstream.XStreamInOut.XStreamInOutInterface;
import org.sag.xstream.xstreamconverters.NamedCollectionConverterWithSize;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;

@XStreamAlias("EntryPointEdge")
public class EntryPointEdge implements XStreamInOutInterface {
	
	@XStreamAlias("ReferencedEntryPoint")
	private SootMethodContainer ep;
	@XStreamAlias("ReferencedStub")
	private SootClassContainer stub;
	@XStreamAlias("Type")
	private String type;
	@XStreamAlias("Sources")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"SourceContainer"},types={SourceContainer.class})
	private LinkedHashSet<SourceContainer> sources;
	
	//for reading in from xml only
	protected EntryPointEdge() {}
	
	protected EntryPointEdge(EntryPoint ep, Pair<Type,Map<SootMethod,Pair<Set<Unit>,Set<Integer>>>> data) {
		this.ep = ep.getEntryPoint() == null ? null : SootMethodContainer.makeSootMethodContainer(ep.getEntryPoint());
		this.stub = ep.getStub() == null ? null : SootClassContainer.makeSootClassContainer(ep.getStub());
		this.type = data.getFirst().toString();
		this.sources = new LinkedHashSet<>();
		for(SootMethod source : data.getSecond().keySet()) {
			sources.add(new SourceContainer(source, data.getSecond().get(source)));
		}
	}
	
	@Override
	public boolean equals(Object o){
		if(this == o)
			return true;
		if(o == null || !(o instanceof EntryPointEdge))
			return false;
		EntryPointEdge other = (EntryPointEdge)o;
		return Objects.equals(ep, other.ep) && Objects.equals(stub, other.stub) && Objects.equals(type, other.type) && Objects.equals(sources, other.sources);
	}
	
	@Override
	public int hashCode(){
		int hash = 17;
		hash = 31 * hash + Objects.hashCode(ep);
		hash = 31 * hash + Objects.hashCode(stub);
		hash = 31 * hash + Objects.hashCode(type);
		hash = 31 * hash + Objects.hashCode(sources);
		return hash;
	}
	
	@Override
	public String toString() {
		return toString("");
	}
	
	public String toString(String spacer) {
		StringBuilder sb = new StringBuilder();
		sb.append(spacer).append("Referenced Entry Point: ").append("{" + Objects.toString(stub) + " : " + Objects.toString(ep) + "}").append("\n");
		sb.append(spacer).append("  Type: ").append(type).append("\n");
		sb.append(spacer).append("  Sources:\n");
		for(SourceContainer s : sources) {
			sb.append(s.toString(spacer + "    "));
		}
		return sb.toString();
	}
	
	public SootMethod getReferencedEntryPoint() {
		if(ep == null)
			return null;
		return ep.toSootMethod();
	}
	
	public SootClass getReferencedStub() {
		if(stub == null)
			return null;
		return stub.toSootClass();
	}
	
	public Pair<Type,Map<SootMethod,Pair<Set<Unit>,Set<Integer>>>> getData() {
		Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> ret = new LinkedHashMap<>();
		for(SourceContainer source : sources) {
			ret.put(source.getSource(), source.getData());
		}
		return new Pair<>(Type.fromString(type),ret);
	}
	
	public SootMethodContainer getReferencedEntryPointContainer() {
		return ep;
	}
	
	public SootClassContainer getReferencedStubContainer() {
		return stub;
	}
	
	public Type getType() {
		return Type.fromString(type);
	}
	
	public Set<SourceContainer> getSourceContainers() {
		return new LinkedHashSet<>(sources);
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}
	
	public EntryPointEdge readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this,filePath, path);
	}
	
	public static EntryPointEdge readXMLStatic(String filePath, Path path) throws Exception {
		return new EntryPointEdge().readXML(filePath, path);
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
				SourceContainer.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(EntryPointEdge.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}

}
