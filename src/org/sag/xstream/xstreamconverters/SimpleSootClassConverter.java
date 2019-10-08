package org.sag.xstream.xstreamconverters;

import soot.Scene;
import soot.SootClass;

import com.thoughtworks.xstream.converters.SingleValueConverter;

public class SimpleSootClassConverter implements SingleValueConverter {

	@Override
	public boolean canConvert(@SuppressWarnings("rawtypes") Class arg0) {
		return arg0.equals(SootClass.class);
	}

	@Override
	public Object fromString(String str) {
		if(str.equals("NULL"))
			return null;
		return Scene.v().getSootClass(str);
	}

	@Override
	public String toString(Object arg0) {
		if(arg0 == null)
			return "NULL";
		return ((SootClass)arg0).getName();
	}

}
