package org.sag.acminer.database.filter;

import org.sag.acminer.database.filter.IData.IMethodData;

import soot.SootMethod;

//For use with the context query filter only
public class MethodData implements IMethodData {
	
	private final SootMethod m;
	private final String signature;
	
	public MethodData(SootMethod m) {
		this.m = m;
		this.signature = m.toString();
	}
	
	public MethodData(String signature) {
		this.signature = signature;
		this.m = null;
	}

	@Override
	public SootMethod getMethod() {
		return m;
	}
	
	@Override
	public String getMethodSignature() {
		return signature;
	}

}
