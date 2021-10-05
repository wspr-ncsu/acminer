package com.benandow.policyminer.controlpredicatefilter.utils;

import java.util.List;

public class Method extends IFieldOrMethod {

	private String mArgs;

	public Method(String mPackage, String mReturnType, String mMethodName, String mArgs) {
		super();
		this.setPackage(mPackage);
		this.setType(mReturnType);
		this.setName(mMethodName);
		this.mArgs = mArgs;
	}

	public String getmArgs() {
		return mArgs;
	}

	public void setmArgs(String mArgs) {
		this.mArgs = mArgs;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((mArgs == null) ? 0 : mArgs.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Method other = (Method) obj;
		if (mArgs == null) {
			if (other.mArgs != null)
				return false;
		} else if (!mArgs.equals(other.mArgs))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Method [" + super.toString() + ", mArgs=" + mArgs + "]";
	}

	public static int dumpMethodDiffs(List<Method> m1, List<Method> m2, boolean VERBOSE) {
		int count = 0;
		for (Method m : m1) {
			if (!m2.contains(m)) {
				if (VERBOSE) {
					System.out.println(m.getName()+" "+m.getmPackage());
				}
				count +=1;
			}
		}
		return count;
	}
	
	public static int dumpMethodDiffs2(List<Method> m1, List<Method> m2, boolean VERBOSE) {
		int count = 0;
		for (Method m : m1) {
			if (!m2.contains(m) && !( m.getName().contains("Permission") || m.getName().contains("Uid") || m.getName().contains("noteOp") || m.getName().contains("noteProxyOp")|| m.getName().contains("enforce"))) {
				if (VERBOSE) {
					System.out.println("\t"+m.getName()+"\t\t"+m.getmPackage());
				}
				count +=1;
			}
		}
		return count;
	}
}