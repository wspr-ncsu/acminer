package com.benandow.policyminer.controlpredicatefilter.utils;

import java.util.List;

public class Field extends IFieldOrMethod {
	
	public Field(String mPackage, String mFieldType, String mFieldName) {
		super();
		this.setName(mFieldName);
		this.setPackage(mPackage);
		this.setType(mFieldType);
	}
	
	public static int dumpFieldDiffs(List<Field> f1, List<Field> f2, boolean VERBOSE) {
		int count = 0;
		for (Field f : f1) {
			if (!f2.contains(f)) {
				if (VERBOSE) {
					System.out.println(f.getName()+"\t\t"+f.getmPackage());
				}
				count +=1;
			}
		}
		return count;
	}
	
}