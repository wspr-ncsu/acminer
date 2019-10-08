package org.sag.acminer.database.excludedelements;

import java.util.Set;

import org.sag.acminer.phases.entrypoints.EntryPoint;

import soot.SootMethod;

public interface IRuntimeExcludedMethods {
	
	public Set<SootMethod> genRuntimeExcludedMethods(EntryPoint entryPoint, Object... args);
	public void clearSootData();
	public void genSootData();

}
