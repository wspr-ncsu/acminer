package org.sag.acminer.database.accesscontrol;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.tuple.Pair;
import org.sag.soot.xstream.SootClassContainer;
import org.sag.soot.xstream.SootMethodContainer;
import org.sag.xstream.XStreamInOut;
import org.sag.xstream.XStreamInOut.XStreamInOutInterface;
import org.sag.xstream.xstreamconverters.NamedCollectionConverterWithSize;

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
	
	@XStreamAlias("Sources")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"SourceContainer"},types={SourceContainer.class})
	private LinkedHashSet<SourceContainer> sources;
	
	@XStreamAlias("ContextQueries")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"ContextQueryContainer"},types={ContextQueryContainer.class})
	private LinkedHashSet<ContextQueryContainer> contextQueries;
	
	//for reading in from xml only
	protected EntryPointContainer() {}
	
	public EntryPointContainer(EntryPoint ep, Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> data) {
		this.entryPoint = ep.getEntryPoint() == null ? null : SootMethodContainer.makeSootMethodContainer(ep.getEntryPoint());
		this.stub = ep.getStub() == null ? null : SootClassContainer.makeSootClassContainer(ep.getStub());
		this.sources = new LinkedHashSet<>();
		for(SootMethod sm : data.keySet()) {
			sources.add(new SourceContainer(sm, data.get(sm)));
		}
		this.contextQueries = null;
	}
	
	public EntryPointContainer(EntryPoint ep, Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> data, 
			Map<SootMethod, Set<SootMethod>> contextQueries) {
		this(ep, data);
		addContextQueries(contextQueries);
	}
	
	public boolean equals(Object o){
		if(this == o)
			return true;
		if(o == null || !(o instanceof EntryPointContainer))
			return false;
		EntryPointContainer other = (EntryPointContainer)o;
		return Objects.equals(entryPoint, other.entryPoint) && Objects.equals(stub, other.stub);
	}
	
	public int hashCode(){
		int hash = 17;
		hash = 31 * hash + Objects.hashCode(entryPoint);
		hash = 31 * hash + Objects.hashCode(stub);
		return hash;
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
	
	public Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> getData() {
		Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> ret = new LinkedHashMap<>();
		for(SourceContainer source : sources) {
			ret.put(source.getSource(), source.getData());
		}
		return ret;
	}
	
	public Map<SootMethod, Set<SootMethod>> getSubGraphMethods() {
		if(contextQueries == null)
			return Collections.emptyMap();
		Map<SootMethod, Set<SootMethod>> ret = new LinkedHashMap<>();
		for(ContextQueryContainer cqc : contextQueries) {
			ret.put(cqc.getContextQuery(), cqc.getSubGraphMethods());
		}
		return ret;
	}
	
	public void addContextQueries(Map<SootMethod, Set<SootMethod>> contextQueries) {
		if(this.contextQueries == null)
			this.contextQueries = new LinkedHashSet<>();
		for(SootMethod sm : contextQueries.keySet()) {
			this.contextQueries.add(new ContextQueryContainer(sm, contextQueries.get(sm)));
		}
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
				SourceContainer.getXStreamSetupStatic().getOutputGraph(in);
				ContextQueryContainer.getXStreamSetupStatic().getOutputGraph(in);
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
