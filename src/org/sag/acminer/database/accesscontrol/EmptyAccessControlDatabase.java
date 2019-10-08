package org.sag.acminer.database.accesscontrol;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.io.FileHashList;
import org.sag.common.tuple.Pair;
import org.sag.xstream.XStreamInOut;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import soot.SootMethod;
import soot.Unit;

@XStreamAlias("AccessControlDatabase")
public class EmptyAccessControlDatabase extends AbstractAccessControlDatabase implements IContextQueryDatabase {
	
	public EmptyAccessControlDatabase(String name, String type) {
		super(name,type,Collections.<EntryPointContainer>emptySet());
	}

	@Override
	public void sortData() {}

	@Override
	public void add(EntryPoint ep, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> dataToAdd) {}

	@Override
	public void addAll(Map<EntryPoint, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> dataToAdd) {}

	@Override
	public boolean contains(EntryPoint ep, Unit u) {
		return false;
	}

	@Override
	public boolean contains(Unit u) {
		return false;
	}

	@Override
	public Set<Unit> getUnits(EntryPoint ep) {
		return Collections.emptySet();
	}

	@Override
	public Set<Unit> getUnits() {
		return Collections.emptySet();
	}

	@Override
	public Map<Unit, Set<Integer>> getUnitsWithDepth(EntryPoint ep) {
		return Collections.emptyMap();
	}

	@Override
	public Map<Unit, Set<Integer>> getUnitsWithDepth() {
		return Collections.emptyMap();
	}

	@Override
	public Set<SootMethod> getSources(EntryPoint ep) {
		return Collections.emptySet();
	}

	@Override
	public Set<SootMethod> getSources() {
		return Collections.emptySet();
	}

	@Override
	public Map<SootMethod, Set<Integer>> getSourcesWithDepth(EntryPoint ep) {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootMethod, Set<Integer>> getSourcesWithDepth() {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getData(EntryPoint ep) {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getData() {
		return Collections.emptyMap();
	}
	
	@Override
	public String toString() {
		return toString(null);
	}

	@Override
	public String toString(String spacer) {
		StringBuilder sb = new StringBuilder();
		sb.append(spacer).append(name).append(":\n");
		return sb.toString();
	}

	@Override
	public String toString(EntryPoint ep, String spacer) {
		return "";
	}
	
	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !this.getClass().isInstance(o))
			return false;
		return true;
	}

	@Override
	public EmptyAccessControlDatabase readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}

	@Override
	public void resetSootResolvedData() {}

	@Override
	public void loadSootResolvedData() {}

	@Override
	public boolean hasData() {
		return false;
	}

	@Override
	public boolean hasData(EntryPoint ep) {
		return false;
	}

	@Override
	public void setFileHashList(FileHashList fhl) {}

	@Override
	public Map<EntryPoint, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> getAllData() {
		return Collections.emptyMap();
	}

	@Override
	public Set<SootMethod> getContextQueries() {
		return Collections.emptySet();
	}

	@Override
	public Set<SootMethod> getContextQueries(EntryPoint ep) {
		return Collections.emptySet();
	}

	@Override
	public Map<SootMethod, Set<SootMethod>> getContextQueriesWithSubGraphMethods() {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootMethod, Set<SootMethod>> getContextQueriesWithSubGraphMethods(EntryPoint ep) {
		return Collections.emptyMap();
	}

	@Override
	public Map<EntryPoint, Map<SootMethod, Set<SootMethod>>> getAllContextQueriesWithSubGraphMethods() {
		return Collections.emptyMap();
	}

	@Override
	public boolean isSubGraphMethodOf(SootMethod cq, SootMethod sm) {
		return false;
	}

	@Override
	public boolean isSubGraphMethodOf(EntryPoint ep, SootMethod cq, SootMethod sm) {
		return false;
	}

	@Override
	public boolean isSubGraphMethod(SootMethod sm) {
		return false;
	}

	@Override
	public boolean isSubGraphMethod(EntryPoint ep, SootMethod sm) {
		return false;
	}

	@Override
	public Set<SootMethod> getSubGraphMethods() {
		return Collections.emptySet();
	}

	@Override
	public Set<SootMethod> getSubGraphMethods(EntryPoint ep) {
		return Collections.emptySet();
	}

	@Override
	public void addContextQuerySubGraphs(EntryPoint ep, Map<SootMethod, Set<SootMethod>> dataToAdd) {
	
	}

	@Override
	public void addAllContextQuerySubGraphs(Map<EntryPoint, Map<SootMethod, Set<SootMethod>>> dataToAdd) {

	}

	@Override
	public boolean isContextQuery(SootMethod sm) {
		return false;
	}

	@Override
	public boolean isContextQuery(EntryPoint ep, SootMethod sm) {
		return false;
	}

	@Override
	public Map<EntryPoint, Set<SootMethod>> getContextQueriesByEntryPoint() {
		return Collections.emptyMap();
	}

}
