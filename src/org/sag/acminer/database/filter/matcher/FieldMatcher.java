package org.sag.acminer.database.filter.matcher;

import soot.SootField;
import soot.SootFieldRef;
import soot.jimple.Stmt;

import org.sag.acminer.database.defusegraph.IFieldNode;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("FieldMatcher")
public class FieldMatcher extends SootMatcher {
	
	public FieldMatcher(Op<String> operation) {
		super(operation);
	}
	
	public FieldMatcher(String value) {
		super(value);
	}
	
	public FieldMatcher(SootMatcherOpType type, String... values) {
		super(type, values);
	}
	
	public FieldMatcher(SootMatcherOpType type, boolean ic, String... values) {
		super(type, ic, values);
	}
	
	@Override
	public boolean matcher(Object... objects) {
		if(objects == null || objects.length <= 0 || objects[0] == null)
			return false;
		else if(objects[0] instanceof SootField)
			return matches((SootField)objects[0]);
		else if(objects[0] instanceof SootFieldRef)
			return matches((SootFieldRef)objects[0]);
		else if(objects[0] instanceof IFieldNode) {
			SootField field = ((IFieldNode)objects[0]).getField();
			if(field == null)
				return matches(((Stmt)(((IFieldNode)objects[0]).getUnit())).getFieldRef().getFieldRef());
			else
				return matches(field);
		}
		return false;
	}
	
	protected boolean matches(SootField f) {
		return matches(f.getSignature(),f.getName(),f.getDeclaringClass().getPackageName(),f.getDeclaringClass().getShortName(),
				f.getDeclaringClass().getName());
	}
	
	protected boolean matches(SootFieldRef f) {
		return matches(f.getSignature(),f.name(),f.declaringClass().getPackageName(),f.declaringClass().getShortName(),
				f.declaringClass().getName());
	}
	
	@Override
	public boolean equals(Object o) {
		if(super.equals(o)) {
			return o instanceof FieldMatcher;
		}
		return false;
	}
	
}
