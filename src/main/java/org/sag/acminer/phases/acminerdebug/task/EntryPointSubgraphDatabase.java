package org.sag.acminer.phases.acminerdebug.task;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.sag.common.xstream.XStreamInOut;
import org.sag.common.xstream.XStreamInOut.XStreamInOutInterface;

import com.google.common.collect.ImmutableList;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

@XStreamAlias("EntryPointSubgraphDatabase")
public class EntryPointSubgraphDatabase implements XStreamInOutInterface {
	
	@XStreamAlias("Stub")
	public String stub;
	@XStreamAlias("EntryPoint")
	public String entryPoint;
	@XStreamAlias("Entries")
	public ArrayList<MethodEntry> entries;
	
	private EntryPointSubgraphDatabase() {}
	
	public EntryPointSubgraphDatabase(SootClass stub, SootMethod ep, List<MethodEntry> entries) {
		this.stub = stub.getName();
		this.entryPoint = ep.getSignature();
		if(entries == null) {
			this.entries = null;
		} else {
			this.entries = new ArrayList<>(entries);
		}
	}
	
	public String getStub() {
		return stub;
	}
	
	public SootClass getSootStub() {
		return Scene.v().getSootClass(stub);
	}
	
	public String getEntryPoint() {
		return entryPoint;
	}
	
	public SootMethod getSootEntryPoint() {
		return Scene.v().getMethod(entryPoint);
	}
	
	public List<MethodEntry> getMethodEntries() {
		return ImmutableList.<MethodEntry>copyOf(entries);
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}
	
	@Override
	public EntryPointSubgraphDatabase readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static EntryPointSubgraphDatabase readXMLStatic(String filePath, Path path) throws Exception {
		return new EntryPointSubgraphDatabase().readXML(filePath, path);
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
				MethodEntry.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(EntryPointSubgraphDatabase.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}
}