package org.sag.acminer.phases.entrypoints;

import java.util.Objects;
import org.sag.soot.SootSort;

import soot.SootClass;
import soot.SootMethod;

public final class EntryPoint implements Comparable<EntryPoint> {

	private final SootMethod entryPoint;
	private final SootClass stub;
	private final int hashCode;
	private volatile String cache;
	
	public EntryPoint(SootMethod entryPoint, SootClass stub) {
		if(entryPoint == null || stub == null)
			throw new IllegalArgumentException();
		this.entryPoint = entryPoint;
		this.stub = stub;
		this.hashCode = hashCodeGen();
	}
	
	private int hashCodeGen() {
		int i = 17;
		i = i * 31 + Objects.hashCode(entryPoint);
		i = i * 31 + Objects.hashCode(stub);
		return i;
	}
	
	@Override
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof EntryPoint))
			return false;
		EntryPoint other = (EntryPoint)o;
		return Objects.equals(entryPoint, other.entryPoint) && Objects.equals(stub, other.stub);
	}
	
	@Override
	public int hashCode() {
		return hashCode;
	}
	
	@Override
	public String toString() {
		if(cache == null)
			cache = "{" + Objects.toString(stub) + " : " + Objects.toString(entryPoint) + "}";
		return cache;
	}
	
	@Override
	public int compareTo(EntryPoint o) {
		int ret = SootSort.scComp.compare(this.stub, o.stub);
		if(ret == 0)
			ret = SootSort.smComp.compare(this.entryPoint,o.entryPoint);
		return ret;
	}
	
	public SootMethod getEntryPoint() {
		return entryPoint;
	}
	
	public SootClass getStub() {
		return stub;
	}
	
}
