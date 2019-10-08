package org.sag.acminer.database.excludedelements;

import java.util.Set;

import soot.SootClass;
import soot.SootMethod;

public interface IExcludeHandler {

	public boolean isExcludedMethod(SootMethod m);
	public boolean isExcludedMethodWithOverride(SootMethod m);
	public boolean isExcludedClass(SootClass sc);
	public Set<SootMethod> getExcludedMethods();
	public Set<SootMethod> getExcludedOverrideMethods();
	public Set<SootMethod> getExcludedMethodsWithOverride();
	public Set<SootClass> getExcludedClasses();
	/** Returns the only entry point that is not excluded */
	public SootMethod getEntryPoint();
	public SootClass getStub();
	
}
