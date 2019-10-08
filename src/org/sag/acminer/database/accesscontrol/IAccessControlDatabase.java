package org.sag.acminer.database.accesscontrol;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.io.FileHash;
import org.sag.common.io.FileHashList;
import org.sag.common.tuple.Pair;
import org.sag.xstream.XStreamInOut.XStreamInOutInterface;

import soot.SootMethod;
import soot.Unit;

public interface IAccessControlDatabase extends XStreamInOutInterface {
	
	public List<FileHash> getFileHashList();
	
	public void setFileHashList(FileHashList fhl);
	
	public String getType();

	public void sortData();

	public void add(EntryPoint ep, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> dataToAdd);

	public void addAll(Map<EntryPoint, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> dataToAdd);

	public boolean contains(EntryPoint ep, Unit u);

	public boolean contains(Unit u);

	public Set<Unit> getUnits(EntryPoint ep);

	public Set<Unit> getUnits();

	public Map<Unit, Set<Integer>> getUnitsWithDepth(EntryPoint ep);

	public Map<Unit, Set<Integer>> getUnitsWithDepth();

	public Set<SootMethod> getSources(EntryPoint ep);

	public Set<SootMethod> getSources();

	public Map<SootMethod, Set<Integer>> getSourcesWithDepth(EntryPoint ep);

	public Map<SootMethod, Set<Integer>> getSourcesWithDepth();

	public Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getData(EntryPoint ep);

	public Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getData();
	
	public Map<EntryPoint, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> getAllData();

	public abstract String toString();

	public String toString(String spacer);

	public String toString(EntryPoint ep, String spacer);

	public abstract int hashCode();

	public abstract boolean equals(Object o);
	
	public abstract IAccessControlDatabase readXML(String filePath, Path path) throws Exception;
	
	/** Clears out the soot data map and dumps the data to the output structure so it can be 
	 * read back in if needed. Note this does not write the data to file. This puts the data
	 * structure into a state as if the data were just read from a file and readResolve had
	 * not run yet.
	 */
	public abstract void resetSootResolvedData();
	
	/** Loads the data from the output structure into soot resolved data structures, overwriting
	 * any pre-existing soot resolved data. Note this method should only be called after 
	 * {@link #resetSootResolvedData()} once soot has been reset to the appropriate state. It is
	 * not required to be called when reading the data in from file as this process automatically
	 * loads the soot data assuming soot it is the required state.
	 */
	public abstract void loadSootResolvedData();
	
	public abstract boolean hasData();
	
	public abstract boolean hasData(EntryPoint ep);

}