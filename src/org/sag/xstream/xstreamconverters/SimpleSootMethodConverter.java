package org.sag.xstream.xstreamconverters;

import soot.Scene;
import soot.SootMethod;

import com.thoughtworks.xstream.converters.SingleValueConverter;

public class SimpleSootMethodConverter implements SingleValueConverter {

	@Override
	public boolean canConvert(@SuppressWarnings("rawtypes") Class arg0) {
		return arg0.equals(SootMethod.class);
	}

	@Override
	public Object fromString(String str) {
		if(str.equals("NULL"))
			return null;
		return Scene.v().getMethod(str);
	}

	@Override
	public String toString(Object value) {
		if(value == null)
			return "NULL";
		return ((SootMethod)value).getSignature();
	}

}
