package org.sag.acminer.database.filter.matcher;

import org.sag.acminer.database.defusegraph.INode;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import soot.Type;
import soot.Value;

@XStreamAlias("TypeMatcher")
public class TypeMatcher extends StringMatcher {
	
	public TypeMatcher(Op<String> operation) {
		super(operation);
	}
	
	public TypeMatcher(String value) {
		super(value);
	}
	
	public TypeMatcher(String value, StringMatcherOpType type) {
		super(value, type);
	}
	
	public TypeMatcher(String value, StringMatcherOpType type, boolean ic) {
		super(value, type, ic);
	}
	
	@Override
	public boolean matcher(Object... objects) {
		if(objects == null || objects.length <= 0 || objects[0] == null)
			return false;
		else if(objects[0] instanceof Type)
			return matches((Type)objects[0]);
		else if(objects[0] instanceof String)
			return matches((String)objects[0]);
		else if(objects[0] instanceof INode) {
			Value v = ((INode)objects[0]).getValue();
			if(v != null)
				return matches(v.getType());
		}
		return false;
	}
	
	protected boolean matches(Type t) {
		return super.matches(t.toString());
	}
	
	@Override
	public boolean equals(Object o) {
		if(super.equals(o)) {
			return o instanceof TypeMatcher;
		}
		return false;
	}

}
