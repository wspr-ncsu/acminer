package org.sag.acminer.database.excludedelements;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.incexclist.methodlist.MethodIncludeExcludeList;
import org.sag.common.io.FileHash;
import org.sag.common.io.FileHashList;
import org.sag.common.tools.SortingMethods;
import org.sag.soot.SootSort;
import org.sag.sootinit.SootInstanceWrapper;
import org.sag.xstream.XStreamInOut.XStreamInOutInterface;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

public interface IExcludedElementsDatabase extends XStreamInOutInterface {

	void resetSootResolvedData();
	void loadAllSootResolvedData();
	Set<String> getExcludedClasses();
	Set<String> getExcludedMethods();
	Set<SootClass> getSootExcludedClasses();
	Set<SootMethod> getSootExcludedMethods();
	boolean isExcludedMethod(SootMethod m);
	boolean isExcludedClass(SootClass sc);
	IExcludeHandler createNewExcludeHandler(EntryPoint ep, Object... args);
	IExcludedElementsDatabase readXML(String filePath, Path path) throws Exception;
	List<FileHash> getFileHashList();
	void setFileHashList(FileHashList fhl);
	
	public static final class Factory {
		
		public static IExcludedElementsDatabase getNew(Set<String> excludedClasses, Set<String> excludedMethods,
				IRuntimeExcludedMethods runtimeExcludedMethods) {
			if(excludedClasses == null || excludedMethods == null)
				return new EmptyExcludedElementsDatabase();
			return new ExcludedElementsDatabase(excludedClasses, excludedMethods, runtimeExcludedMethods);
		}
		
		public static IExcludedElementsDatabase readXML(String filePath, Path path,
				IRuntimeExcludedMethods runtimeExcludedMethods) throws Exception {
			return ExcludedElementsDatabase.readXMLStatic(filePath, path, runtimeExcludedMethods);
		}
		
		public static IExcludedElementsDatabase readTXT(Path path) throws Exception {
			return readTXT(path, null, null, null);
		}
		
		public static IExcludedElementsDatabase readTXT(Path path, Set<String> otherClasses, Set<String> otherMethods,
				IRuntimeExcludedMethods runtimeExcludedMethods) throws Exception {
			if(!SootInstanceWrapper.v().isAnySootInitValueSet())
				throw new RuntimeException("Error: Some instance of Soot must be initilized first.");
			
			Set<String> classes = (otherClasses == null) ? new HashSet<>() : new HashSet<>(otherClasses);
			Set<String> methods = (otherMethods == null) ? new HashSet<>() : new HashSet<>(otherMethods);
			
			MethodIncludeExcludeList mList = MethodIncludeExcludeList.readTXTStatic(path, false, false);
			
			for(SootClass sc : Scene.v().getClasses()) {
				boolean allExcluded = true;
				for(SootMethod sm : sc.getMethods()) {
					if(!mList.isIncluded(sm)) {
						allExcluded = false;
					} else {
						methods.add(sm.getSignature());
					}
				}
				if(allExcluded) {
					classes.add(sc.getName());
				}
			}
			
			classes = SortingMethods.sortSet(classes, SortingMethods.sComp);
			methods = SortingMethods.sortSet(methods, SootSort.smStringComp);
			
			return getNew(classes, methods, runtimeExcludedMethods);
		}
	}
	
}
