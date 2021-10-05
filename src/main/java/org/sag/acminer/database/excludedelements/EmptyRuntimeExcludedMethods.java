package org.sag.acminer.database.excludedelements;

import java.util.Collections;
import java.util.Set;

import org.sag.acminer.phases.entrypoints.EntryPoint;

import soot.SootMethod;

public class EmptyRuntimeExcludedMethods implements IRuntimeExcludedMethods {

	@Override
	public Set<SootMethod> genRuntimeExcludedMethods(EntryPoint ep, Object... args) {
		return Collections.emptySet();
	}

	@Override
	public void clearSootData() {}

	@Override
	public void genSootData() {}

}
