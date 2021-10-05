package org.sag.acminer.database.excludedelements;

import java.util.Set;

import org.sag.acminer.phases.entrypoints.EntryPoint;

import com.google.common.collect.ImmutableSet;

import soot.SootClass;
import soot.SootMethod;

public class ExcludeHandler implements IExcludeHandler {
	
	private final Set<SootMethod> excludedMethods;
	private final Set<SootClass> excludedClasses;
	private final Set<SootMethod> otherExcludedMethods;
	private final SootMethod ep;
	private final SootClass stub;
	
	protected ExcludeHandler(EntryPoint ep, Set<SootMethod> excludedMethods, 
			Set<SootClass> excludedClasses, Set<SootMethod> otherExcludedMethods) {
		this.otherExcludedMethods = ImmutableSet.<SootMethod>copyOf(otherExcludedMethods);
		this.ep = ep.getEntryPoint();
		this.stub = ep.getStub();
		this.excludedClasses = ImmutableSet.<SootClass>copyOf(excludedClasses);
		this.excludedMethods = ImmutableSet.<SootMethod>copyOf(excludedMethods);
	}
	
	public SootMethod getEntryPoint() {
		return ep;
	}
	
	public SootClass getStub() {
		return stub;
	}
	
	public Set<SootClass> getExcludedClasses(){
		return excludedClasses;
	}
	
	public Set<SootMethod> getExcludedMethods() {
		return excludedMethods;
	}
	
	public Set<SootMethod> getExcludedOverrideMethods() {
		return otherExcludedMethods;
	}
	
	public boolean isExcludedMethod(SootMethod m) {
		return getExcludedMethods().contains(m);
	}
	
	public boolean isExcludedMethodWithOverride(SootMethod m) {
		return getExcludedOverrideMethods().contains(m) ? true : isExcludedMethod(m);
	}
	
	public boolean isExcludedClass(SootClass sc) {
		return getExcludedClasses().contains(sc);
	}
	
	public Set<SootMethod> getExcludedMethodsWithOverride() {
		ImmutableSet.Builder<SootMethod> b = ImmutableSet.builder();
		b.addAll(getExcludedOverrideMethods());
		b.addAll(getExcludedMethods());
		return b.build();
	}

}
