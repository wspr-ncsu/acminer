package org.sag.acminer.database.entrypointedges;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.io.FileHash;
import org.sag.common.io.FileHashList;
import org.sag.common.tuple.Pair;
import org.sag.common.xstream.XStreamInOut.XStreamInOutInterface;

import soot.SootMethod;
import soot.Unit;

public interface IEntryPointEdgesDatabase extends XStreamInOutInterface {
	
	public static enum Type {
		Internal("internal"),External("external"),Both("both");
		
		private final String s;
		
		Type(String s) {
			this.s = s;
		}
		
		public String toString() {
			return s;
		}
		
		public static Type fromString(String s) {
			switch(s) {
				case "internal": return Internal;
				case "external": return External;
				case "both": return Both;
				default: throw new RuntimeException("Error: Unrecongized referenced type.");
			}
		}
	}
	
	void clearSootResolvedData();
	void loadSootResolvedData();
	List<FileHash> getFileHashList();
	void setFileHashList(FileHashList fhl);
	String toString();
	String toString(String spacer);
	int hashCode();
	boolean equals(Object o);
	
	void sortData();
	void add(EntryPoint ep, Map<EntryPoint,Pair<Type,Map<SootMethod,Pair<Set<Unit>,Set<Integer>>>>> dataToAdd);
	void addAll(Map<EntryPoint, Map<EntryPoint,Pair<Type,Map<SootMethod,Pair<Set<Unit>,Set<Integer>>>>>> dataToAdd);
	
	boolean hasEntryPointEdges(EntryPoint ep);
	boolean hasInternalEntryPointEdges(EntryPoint ep);
	boolean hasExtenralEntryPointEdges(EntryPoint ep);
	
	boolean hasEntryPointEdgesForStmt(EntryPoint ep, Unit unit);
	boolean hasInternalEntryPointEdgesForStmt(EntryPoint ep, Unit unit);
	boolean hasExternalEntryPointEdgesForStmt(EntryPoint ep, Unit unit);
	
	boolean hasEntryPointEdgesForStmt(Unit unit);
	boolean hasInternalEntryPointEdgesForStmt(Unit unit);
	boolean hasExternalEntryPointEdgesForStmt(Unit unit);
	
	boolean hasEntryPointEdgesForSource(EntryPoint ep, SootMethod source);
	boolean hasInternalEntryPointEdgesForSource(EntryPoint ep, SootMethod source);
	boolean hasExtermalEntryPointEdgesForSource(EntryPoint ep, SootMethod source);
	
	boolean hasEntryPointEdgesForSource(SootMethod source);
	boolean hasInternalEntryPointEdgesForSource(SootMethod source);
	boolean hasExtermalEntryPointEdgesForSource(SootMethod source);
	
	boolean hasEntryPointEdge(EntryPoint ep, EntryPoint referenced);
	boolean hasInternalEntryPointEdge(EntryPoint ep, EntryPoint referenced);
	boolean hasExternalEntryPointEdge(EntryPoint ep, EntryPoint referenced);
	
	boolean hasEntryPointEdge(EntryPoint referenced);
	boolean hasInternalEntryPointEdge(EntryPoint referenced);
	boolean hasExternalEntryPointEdge(EntryPoint referenced);
	
	Set<EntryPoint> getEntryPointEdges(EntryPoint ep);
	Set<EntryPoint> getInternalEntryPointEdges(EntryPoint ep);
	Set<EntryPoint> getExternalEntryPointEdges(EntryPoint ep);
	
	Set<EntryPoint> getEntryPointEdges();
	Set<EntryPoint> getInternalEntryPointEdges();
	Set<EntryPoint> getExternalEntryPointEdges();
	
	Set<EntryPoint> getEntryPointEdgesForStmt(EntryPoint ep, Unit unit);
	Set<EntryPoint> getInternalEntryPointEdgesForStmt(EntryPoint ep, Unit unit);
	Set<EntryPoint> getExternalEntryPointEdgesForStmt(EntryPoint ep, Unit unit);
	
	Set<EntryPoint> getEntryPointEdgesForStmt(Unit unit);
	Set<EntryPoint> getInternalEntryPointEdgesForStmt(Unit unit);
	Set<EntryPoint> getExternalEntryPointEdgesForStmt(Unit unit);
	
	Set<EntryPoint> getEntryPointEdgesForSource(EntryPoint ep, SootMethod source);
	Set<EntryPoint> getInternalEntryPointEdgesForSource(EntryPoint ep, SootMethod source);
	Set<EntryPoint> getExternalEntryPointEdgesForSource(EntryPoint ep, SootMethod source);
	
	Set<EntryPoint> getEntryPointEdgesForSource(SootMethod source);
	Set<EntryPoint> getInternalEntryPointEdgesForSource(SootMethod source);
	Set<EntryPoint> getExternalEntryPointEdgesForSource(SootMethod source);
	
	Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getSourcesWithUnitsAndDepth(EntryPoint ep);
	Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getInternalSourcesWithUnitsAndDepth(EntryPoint ep);
	Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getExternalSourcesWithUnitsAndDepth(EntryPoint ep);
	
	Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getSourcesWithUnitsAndDepth();
	Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getInternalSourcesWithUnitsAndDepth();
	Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getExternalSourcesWithUnitsAndDepth();
	
	Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getSourcesWithUnitsAndDepthForReference(EntryPoint ep, EntryPoint referenced);
	Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getInternalSourcesWithUnitsAndDepthForReference(EntryPoint ep, EntryPoint referenced);
	Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getExternalSourcesWithUnitsAndDepthForReference(EntryPoint ep, EntryPoint referenced);

	Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getSourcesWithUnitsAndDepthForReference(EntryPoint referenced);
	Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getInternalSourcesWithUnitsAndDepthForReference(EntryPoint referenced);
	Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getExternalSourcesWithUnitsAndDepthForReference(EntryPoint referenced);
	
	Map<Unit, Set<Integer>> getUnitsWithDepth(EntryPoint ep);
	Map<Unit, Set<Integer>> getInternalUnitsWithDepth(EntryPoint ep);
	Map<Unit, Set<Integer>> getExternalUnitsWithDepth(EntryPoint ep);
	
	Map<Unit, Set<Integer>> getUnitsWithDepth();
	Map<Unit, Set<Integer>> getInternalUnitsWithDepth();
	Map<Unit, Set<Integer>> getExternalUnitsWithDepth();
	
	Map<Unit, Set<Integer>> getUnitsWithDepthForReference(EntryPoint ep, EntryPoint referenced);
	Map<Unit, Set<Integer>> getInternalUnitsWithDepthForReference(EntryPoint ep, EntryPoint referenced);
	Map<Unit, Set<Integer>> getExternalUnitsWithDepthForReference(EntryPoint ep, EntryPoint referenced);
	
	Map<Unit, Set<Integer>> getUnitsWithDepthForReference(EntryPoint referenced);
	Map<Unit, Set<Integer>> getInternalUnitsWithDepthForReference(EntryPoint referenced);
	Map<Unit, Set<Integer>> getExternalUnitsWithDepthForReference(EntryPoint referenced);
	
	Set<SootMethod> getSources(EntryPoint ep);
	Set<SootMethod> getInternalSources(EntryPoint ep);
	Set<SootMethod> getExternalSources(EntryPoint ep);
	
	Set<SootMethod> getSources();
	Set<SootMethod> getInternalSources();
	Set<SootMethod> getExternalSources();
	
	Set<SootMethod> getSourcesForReference(EntryPoint ep, EntryPoint referenced);
	Set<SootMethod> getInternalSourcesForReference(EntryPoint ep, EntryPoint referenced);
	Set<SootMethod> getExternalSourcesForReference(EntryPoint ep, EntryPoint referenced);
	
	Set<SootMethod> getSourcesForReference(EntryPoint referenced);
	Set<SootMethod> getInternalSourcesForReference(EntryPoint referenced);
	Set<SootMethod> getExternalSourcesForReference(EntryPoint referenced);
	
	Set<Unit> getUnits(EntryPoint ep);
	Set<Unit> getInternalUnits(EntryPoint ep);
	Set<Unit> getExternalUnits(EntryPoint ep);
	
	Set<Unit> getUnits();
	Set<Unit> getInternalUnits();
	Set<Unit> getExternalUnits();
	
	Set<Unit> getUnitsForReference(EntryPoint ep, EntryPoint referenced);
	Set<Unit> getInternalUnitsForReference(EntryPoint ep, EntryPoint referenced);
	Set<Unit> getExternalUnitsForReference(EntryPoint ep, EntryPoint referenced);
	
	Set<Unit> getUnitsForReference(EntryPoint referenced);
	Set<Unit> getInternalUnitsForReference(EntryPoint referenced);
	Set<Unit> getExternalUnitsForReference(EntryPoint referenced);
	
	Map<SootMethod, Set<Integer>> getSourcesWithDepth(EntryPoint ep);
	Map<SootMethod, Set<Integer>> getInternalSourcesWithDepth(EntryPoint ep);
	Map<SootMethod, Set<Integer>> getExternalSourcesWithDepth(EntryPoint ep);
	
	Map<SootMethod, Set<Integer>> getSourcesWithDepth();
	Map<SootMethod, Set<Integer>> getInternalSourcesWithDepth();
	Map<SootMethod, Set<Integer>> getExternalSourcesWithDepth();
	
	Map<SootMethod, Set<Integer>> getSourcesWithDepthForReference(EntryPoint ep, EntryPoint referenced);
	Map<SootMethod, Set<Integer>> getInternalSourcesWithDepthForReference(EntryPoint ep, EntryPoint referenced);
	Map<SootMethod, Set<Integer>> getExternalSourcesWithDepthForReference(EntryPoint ep, EntryPoint referenced);
	
	Map<SootMethod, Set<Integer>> getSourcesWithDepthForReference(EntryPoint referenced);
	Map<SootMethod, Set<Integer>> getInternalSourcesWithDepthForReference(EntryPoint referenced);
	Map<SootMethod, Set<Integer>> getExternalSourcesWithDepthForReference(EntryPoint referenced);
	
	Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> getData(EntryPoint ep);
	Map<EntryPoint, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> getInternalData(EntryPoint ep);
	Map<EntryPoint, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> getExternalData(EntryPoint ep);
	
	Map<EntryPoint, Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>>> getData();
	Map<EntryPoint, Map<EntryPoint, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> getInternalData();
	Map<EntryPoint, Map<EntryPoint, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> getExternalData();
	
	Set<EntryPointContainer> getOutputData();
	IEntryPointEdgesDatabase readXML(String filePath, Path path) throws Exception;
	
	public static final class Factory {
		
		public static IEntryPointEdgesDatabase getNew(boolean isEmpty) {
			if(isEmpty)
				return new EmptyEntryPointEdgesDatabase();
			return new EntryPointEdgesDatabase(true);
		}
		
		public static IEntryPointEdgesDatabase readXML(String filePath, Path path) throws Exception {
			return EntryPointEdgesDatabase.readXMLStatic(filePath, path);
		}
		
	}

}
