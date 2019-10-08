package org.sag.acminer.database.excludedelements;

import java.util.Collections;
import java.util.Set;

import soot.SootClass;
import soot.SootMethod;

public class EmptyExcludeHandler implements IExcludeHandler {

	@Override
	public boolean isExcludedMethod(SootMethod m) {
		return false;
	}

	@Override
	public boolean isExcludedMethodWithOverride(SootMethod m) {
		return false;
	}

	@Override
	public boolean isExcludedClass(SootClass sc) {
		return false;
	}

	@Override
	public Set<SootMethod> getExcludedMethods() {
		return Collections.emptySet();
	}

	@Override
	public Set<SootMethod> getExcludedOverrideMethods() {
		return Collections.emptySet();
	}

	@Override
	public Set<SootMethod> getExcludedMethodsWithOverride() {
		return Collections.emptySet();
	}

	@Override
	public Set<SootClass> getExcludedClasses() {
		return Collections.emptySet();
	}

	@Override
	public SootMethod getEntryPoint() {
		return null;
	}

	@Override
	public SootClass getStub() {
		return null;
	}

}
