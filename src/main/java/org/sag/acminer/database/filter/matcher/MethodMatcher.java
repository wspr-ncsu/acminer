package org.sag.acminer.database.filter.matcher;

import java.util.regex.Pattern;

import org.sag.acminer.database.defusegraph.IInvokeNode;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import soot.SootMethod;
import soot.SootMethodRef;
import soot.jimple.Stmt;

@XStreamAlias("MethodMatcher")
public class MethodMatcher extends SootMatcher {
	
	private static final Pattern methodSigPat = Pattern.compile("^<([^:]+):\\s+([^\\s]+)\\s+([^\\(]+)\\(([^\\)]*)\\)>$");
	
	public MethodMatcher(Op<String> operation) {
		super(operation);
	}
	
	public MethodMatcher(String value) {
		super(value);
	}
	
	public MethodMatcher(SootMatcherOpType type, String... values) {
		super(type, values);
	}
	
	public MethodMatcher(SootMatcherOpType type, boolean ic, String... values) {
		super(type, ic, values);
	}
	
	@Override
	public boolean matcher(Object... objects) {
		if(objects == null || objects.length <= 0 || objects[0] == null)
			return false;
		else if(objects[0] instanceof SootMethod)
			return matches((SootMethod)objects[0]);
		else if(objects[0] instanceof SootMethodRef)
			return matches((SootMethodRef)objects[0]);
		else if(objects[0] instanceof IInvokeNode) {
			SootMethod method = ((IInvokeNode)objects[0]).getTarget();
			if(method == null)
				return matches(((Stmt)(((IInvokeNode)objects[0]).getUnit())).getInvokeExpr().getMethodRef());
			else
				return matches(method);
		}
		return false;
	}
	
	protected boolean matches(SootMethod m) {
		return matches(m.getSignature(),m.getName(),m.getDeclaringClass().getPackageName(),m.getDeclaringClass().getShortName(),
				m.getDeclaringClass().getName());
	}
	
	protected boolean matches(SootMethodRef m) {
		return matches(m.getSignature(),m.name(),m.declaringClass().getPackageName(),m.declaringClass().getShortName(),
				m.declaringClass().getName());
	}
	
	protected boolean matches(String signature) {
		java.util.regex.Matcher m = methodSigPat.matcher(signature);
		if(m.matches()) {
			String fullClassName = m.group(1);
			String name = m.group(3);
			String className = fullClassName;
			String packageName = "";

			int index = fullClassName.lastIndexOf('.');
			if (index > 0) {
				className = fullClassName.substring(index + 1);
				packageName = fullClassName.substring(0, index);
			}
			return matches(signature, name, packageName, className, fullClassName);
		}
		return false;
	}
	
	@Override
	public boolean equals(Object o) {
		if(super.equals(o)) {
			return o instanceof MethodMatcher;
		}
		return false;
	}
	
}
