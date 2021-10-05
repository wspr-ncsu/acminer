package org.sag.acminer.database.entrypointedges;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.sag.acminer.database.FileHashDatabase;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.io.FileHash;
import org.sag.common.io.FileHashList;
import org.sag.common.tuple.Pair;
import org.sag.common.xstream.XStreamInOut;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import soot.SootMethod;
import soot.Unit;

@XStreamAlias("EntryPointEdgesDatabase")
public class EmptyEntryPointEdgesDatabase extends FileHashDatabase implements IEntryPointEdgesDatabase {
	
	@Override
	public void clearSootResolvedData() {}

	@Override
	public void loadSootResolvedData() {}
	
	@Override
	public List<FileHash> getFileHashList() {
		return Collections.emptyList();
	}

	@Override
	public void setFileHashList(FileHashList fhl) {}
	
	@Override
	public String toString() {
		return toString("");
	}
	
	@Override
	public String toString(String spacer) {
		StringBuilder sb = new StringBuilder();
		sb.append("Referenced Entry Points Database:\n");
		for(EntryPointContainer e : getOutputData()) {
			sb.append(e.toString("  "));
		}
		return sb.toString();
	}
	
	@Override
	public int hashCode() {
		int i = 17;
		i = i * 31 + Objects.hashCode(getOutputData());
		return i;
	}
	
	@Override
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof IEntryPointEdgesDatabase))
			return false;
		IEntryPointEdgesDatabase other = (IEntryPointEdgesDatabase)o;
		return Objects.equals(getOutputData(), other.getOutputData());
	}
	
	@Override
	public void sortData() {}
	
	@Override
	public void add(EntryPoint ep, Map<EntryPoint,Pair<Type,Map<SootMethod,Pair<Set<Unit>,Set<Integer>>>>> dataToAdd) {}
	
	@Override
	public void addAll(Map<EntryPoint, Map<EntryPoint,Pair<Type,Map<SootMethod,Pair<Set<Unit>,Set<Integer>>>>>> dataToAdd) {}

	@Override
	public boolean hasEntryPointEdges(EntryPoint ep) {
		return false;
	}

	@Override
	public boolean hasInternalEntryPointEdges(EntryPoint ep) {
		return false;
	}

	@Override
	public boolean hasExtenralEntryPointEdges(EntryPoint ep) {
		return false;
	}

	@Override
	public boolean hasEntryPointEdgesForStmt(EntryPoint ep, Unit unit) {
		return false;
	}

	@Override
	public boolean hasInternalEntryPointEdgesForStmt(EntryPoint ep, Unit unit) {
		return false;
	}

	@Override
	public boolean hasExternalEntryPointEdgesForStmt(EntryPoint ep, Unit unit) {
		return false;
	}

	@Override
	public boolean hasEntryPointEdgesForStmt(Unit unit) {
		return false;
	}

	@Override
	public boolean hasInternalEntryPointEdgesForStmt(Unit unit) {
		return false;
	}

	@Override
	public boolean hasExternalEntryPointEdgesForStmt(Unit unit) {
		return false;
	}

	@Override
	public boolean hasEntryPointEdgesForSource(EntryPoint ep, SootMethod source) {
		return false;
	}

	@Override
	public boolean hasInternalEntryPointEdgesForSource(EntryPoint ep, SootMethod source) {
		return false;
	}

	@Override
	public boolean hasExtermalEntryPointEdgesForSource(EntryPoint ep, SootMethod source) {
		return false;
	}

	@Override
	public boolean hasEntryPointEdgesForSource(SootMethod source) {
		return false;
	}

	@Override
	public boolean hasInternalEntryPointEdgesForSource(SootMethod source) {
		return false;
	}

	@Override
	public boolean hasExtermalEntryPointEdgesForSource(SootMethod source) {
		return false;
	}

	@Override
	public boolean hasEntryPointEdge(EntryPoint ep, EntryPoint referenced) {
		return false;
	}

	@Override
	public boolean hasInternalEntryPointEdge(EntryPoint ep, EntryPoint referenced) {
		return false;
	}

	@Override
	public boolean hasExternalEntryPointEdge(EntryPoint ep, EntryPoint referenced) {
		return false;
	}

	@Override
	public boolean hasEntryPointEdge(EntryPoint referenced) {
		return false;
	}

	@Override
	public boolean hasInternalEntryPointEdge(EntryPoint referenced) {
		return false;
	}

	@Override
	public boolean hasExternalEntryPointEdge(EntryPoint referenced) {
		return false;
	}

	@Override
	public Set<EntryPoint> getEntryPointEdges(EntryPoint ep) {
		return Collections.emptySet();
	}

	@Override
	public Set<EntryPoint> getInternalEntryPointEdges(EntryPoint ep) {
		return Collections.emptySet();
	}

	@Override
	public Set<EntryPoint> getExternalEntryPointEdges(EntryPoint ep) {
		return Collections.emptySet();
	}

	@Override
	public Set<EntryPoint> getEntryPointEdges() {
		return Collections.emptySet();
	}

	@Override
	public Set<EntryPoint> getInternalEntryPointEdges() {
		return Collections.emptySet();
	}

	@Override
	public Set<EntryPoint> getExternalEntryPointEdges() {
		return Collections.emptySet();
	}

	@Override
	public Set<EntryPoint> getEntryPointEdgesForStmt(EntryPoint ep, Unit unit) {
		return Collections.emptySet();
	}

	@Override
	public Set<EntryPoint> getInternalEntryPointEdgesForStmt(EntryPoint ep, Unit unit) {
		return Collections.emptySet();
	}

	@Override
	public Set<EntryPoint> getExternalEntryPointEdgesForStmt(EntryPoint ep, Unit unit) {
		return Collections.emptySet();
	}

	@Override
	public Set<EntryPoint> getEntryPointEdgesForStmt(Unit unit) {
		return Collections.emptySet();
	}

	@Override
	public Set<EntryPoint> getInternalEntryPointEdgesForStmt(Unit unit) {
		return Collections.emptySet();
	}

	@Override
	public Set<EntryPoint> getExternalEntryPointEdgesForStmt(Unit unit) {
		return Collections.emptySet();
	}

	@Override
	public Set<EntryPoint> getEntryPointEdgesForSource(EntryPoint ep, SootMethod source) {
		return Collections.emptySet();
	}

	@Override
	public Set<EntryPoint> getInternalEntryPointEdgesForSource(EntryPoint ep, SootMethod source) {
		return Collections.emptySet();
	}

	@Override
	public Set<EntryPoint> getExternalEntryPointEdgesForSource(EntryPoint ep, SootMethod source) {
		return Collections.emptySet();
	}

	@Override
	public Set<EntryPoint> getEntryPointEdgesForSource(SootMethod source) {
		return Collections.emptySet();
	}

	@Override
	public Set<EntryPoint> getInternalEntryPointEdgesForSource(SootMethod source) {
		return Collections.emptySet();
	}

	@Override
	public Set<EntryPoint> getExternalEntryPointEdgesForSource(SootMethod source) {
		return Collections.emptySet();
	}

	@Override
	public Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getSourcesWithUnitsAndDepth(EntryPoint ep) {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getInternalSourcesWithUnitsAndDepth(EntryPoint ep) {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getExternalSourcesWithUnitsAndDepth(EntryPoint ep) {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getSourcesWithUnitsAndDepth() {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getInternalSourcesWithUnitsAndDepth() {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getExternalSourcesWithUnitsAndDepth() {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getSourcesWithUnitsAndDepthForReference(EntryPoint ep, EntryPoint referenced) {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getInternalSourcesWithUnitsAndDepthForReference(EntryPoint ep, EntryPoint referenced) {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getExternalSourcesWithUnitsAndDepthForReference(EntryPoint ep, EntryPoint referenced) {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getSourcesWithUnitsAndDepthForReference(EntryPoint referenced) {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getInternalSourcesWithUnitsAndDepthForReference(EntryPoint referenced) {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getExternalSourcesWithUnitsAndDepthForReference(EntryPoint referenced) {
		return Collections.emptyMap();
	}

	@Override
	public Map<Unit, Set<Integer>> getUnitsWithDepth(EntryPoint ep) {
		return Collections.emptyMap();
	}

	@Override
	public Map<Unit, Set<Integer>> getInternalUnitsWithDepth(EntryPoint ep) {
		return Collections.emptyMap();
	}

	@Override
	public Map<Unit, Set<Integer>> getExternalUnitsWithDepth(EntryPoint ep) {
		return Collections.emptyMap();
	}

	@Override
	public Map<Unit, Set<Integer>> getUnitsWithDepth() {
		return Collections.emptyMap();
	}

	@Override
	public Map<Unit, Set<Integer>> getInternalUnitsWithDepth() {
		return Collections.emptyMap();
	}

	@Override
	public Map<Unit, Set<Integer>> getExternalUnitsWithDepth() {
		return Collections.emptyMap();
	}

	@Override
	public Map<Unit, Set<Integer>> getUnitsWithDepthForReference(EntryPoint ep, EntryPoint referenced) {
		return Collections.emptyMap();
	}

	@Override
	public Map<Unit, Set<Integer>> getInternalUnitsWithDepthForReference(EntryPoint ep, EntryPoint referenced) {
		return Collections.emptyMap();
	}

	@Override
	public Map<Unit, Set<Integer>> getExternalUnitsWithDepthForReference(EntryPoint ep, EntryPoint referenced) {
		return Collections.emptyMap();
	}

	@Override
	public Map<Unit, Set<Integer>> getUnitsWithDepthForReference(EntryPoint referenced) {
		return Collections.emptyMap();
	}

	@Override
	public Map<Unit, Set<Integer>> getInternalUnitsWithDepthForReference(EntryPoint referenced) {
		return Collections.emptyMap();
	}

	@Override
	public Map<Unit, Set<Integer>> getExternalUnitsWithDepthForReference(EntryPoint referenced) {
		return Collections.emptyMap();
	}

	@Override
	public Set<SootMethod> getSources(EntryPoint ep) {
		return Collections.emptySet();
	}

	@Override
	public Set<SootMethod> getInternalSources(EntryPoint ep) {
		return Collections.emptySet();
	}

	@Override
	public Set<SootMethod> getExternalSources(EntryPoint ep) {
		return Collections.emptySet();
	}

	@Override
	public Set<SootMethod> getSources() {
		return Collections.emptySet();
	}

	@Override
	public Set<SootMethod> getInternalSources() {
		return Collections.emptySet();
	}

	@Override
	public Set<SootMethod> getExternalSources() {
		return Collections.emptySet();
	}

	@Override
	public Set<SootMethod> getSourcesForReference(EntryPoint ep, EntryPoint referenced) {
		return Collections.emptySet();
	}

	@Override
	public Set<SootMethod> getInternalSourcesForReference(EntryPoint ep, EntryPoint referenced) {
		return Collections.emptySet();
	}

	@Override
	public Set<SootMethod> getExternalSourcesForReference(EntryPoint ep, EntryPoint referenced) {
		return Collections.emptySet();
	}

	@Override
	public Set<SootMethod> getSourcesForReference(EntryPoint referenced) {
		return Collections.emptySet();
	}

	@Override
	public Set<SootMethod> getInternalSourcesForReference(EntryPoint referenced) {
		return Collections.emptySet();
	}

	@Override
	public Set<SootMethod> getExternalSourcesForReference(EntryPoint referenced) {
		return Collections.emptySet();
	}

	@Override
	public Set<Unit> getUnits(EntryPoint ep) {
		return Collections.emptySet();
	}

	@Override
	public Set<Unit> getInternalUnits(EntryPoint ep) {
		return Collections.emptySet();
	}

	@Override
	public Set<Unit> getExternalUnits(EntryPoint ep) {
		return Collections.emptySet();
	}

	@Override
	public Set<Unit> getUnits() {
		return Collections.emptySet();
	}

	@Override
	public Set<Unit> getInternalUnits() {
		return Collections.emptySet();
	}

	@Override
	public Set<Unit> getExternalUnits() {
		return Collections.emptySet();
	}

	@Override
	public Set<Unit> getUnitsForReference(EntryPoint ep, EntryPoint referenced) {
		return Collections.emptySet();
	}

	@Override
	public Set<Unit> getInternalUnitsForReference(EntryPoint ep, EntryPoint referenced) {
		return Collections.emptySet();
	}

	@Override
	public Set<Unit> getExternalUnitsForReference(EntryPoint ep, EntryPoint referenced) {
		return Collections.emptySet();
	}

	@Override
	public Set<Unit> getUnitsForReference(EntryPoint referenced) {
		return Collections.emptySet();
	}

	@Override
	public Set<Unit> getInternalUnitsForReference(EntryPoint referenced) {
		return Collections.emptySet();
	}

	@Override
	public Set<Unit> getExternalUnitsForReference(EntryPoint referenced) {
		return Collections.emptySet();
	}

	@Override
	public Map<SootMethod, Set<Integer>> getSourcesWithDepth(EntryPoint ep) {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootMethod, Set<Integer>> getInternalSourcesWithDepth(EntryPoint ep) {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootMethod, Set<Integer>> getExternalSourcesWithDepth(EntryPoint ep) {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootMethod, Set<Integer>> getSourcesWithDepth() {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootMethod, Set<Integer>> getInternalSourcesWithDepth() {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootMethod, Set<Integer>> getExternalSourcesWithDepth() {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootMethod, Set<Integer>> getSourcesWithDepthForReference(EntryPoint ep, EntryPoint referenced) {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootMethod, Set<Integer>> getInternalSourcesWithDepthForReference(EntryPoint ep, EntryPoint referenced) {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootMethod, Set<Integer>> getExternalSourcesWithDepthForReference(EntryPoint ep, EntryPoint referenced) {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootMethod, Set<Integer>> getSourcesWithDepthForReference(EntryPoint referenced) {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootMethod, Set<Integer>> getInternalSourcesWithDepthForReference(EntryPoint referenced) {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootMethod, Set<Integer>> getExternalSourcesWithDepthForReference(EntryPoint referenced) {
		return Collections.emptyMap();
	}

	@Override
	public Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> getData(EntryPoint ep) {
		return Collections.emptyMap();
	}

	@Override
	public Map<EntryPoint, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> getInternalData(EntryPoint ep) {
		return Collections.emptyMap();
	}

	@Override
	public Map<EntryPoint, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> getExternalData(EntryPoint ep) {
		return Collections.emptyMap();
	}

	@Override
	public Map<EntryPoint, Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>>> getData() {
		return Collections.emptyMap();
	}

	@Override
	public Map<EntryPoint, Map<EntryPoint, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> getInternalData() {
		return Collections.emptyMap();
	}

	@Override
	public Map<EntryPoint, Map<EntryPoint, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> getExternalData() {
		return Collections.emptyMap();
	}

	@Override
	public Set<EntryPointContainer> getOutputData() {
		return Collections.emptySet();
	}
	
	@Override
	public EmptyEntryPointEdgesDatabase readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static EmptyEntryPointEdgesDatabase readXMLStatic(String filePath, Path path) throws Exception {
		return new EmptyEntryPointEdgesDatabase().readXML(filePath, path);
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@XStreamOmitField
	private static AbstractXStreamSetup xstreamSetup = null;

	public static AbstractXStreamSetup getXStreamSetupStatic(){
		if(xstreamSetup == null)
			xstreamSetup = new SubXStreamSetup();
		return xstreamSetup;
	}
	
	@Override
	public AbstractXStreamSetup getXStreamSetup() {
		return getXStreamSetupStatic();
	}
	
	public static class SubXStreamSetup extends XStreamSetup {
		
		@Override
		public void getOutputGraph(LinkedHashSet<AbstractXStreamSetup> in) {
			if(!in.contains(this)) {
				in.add(this);
				super.getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(EmptyEntryPointEdgesDatabase.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}

}
