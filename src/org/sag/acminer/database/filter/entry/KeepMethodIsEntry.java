package org.sag.acminer.database.filter.entry;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import org.sag.acminer.database.filter.IData;
import org.sag.acminer.database.filter.IData.IMethodData;
import org.sag.acminer.database.filter.matcher.MethodMatcher;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import soot.SootMethod;

//For use with the context query filter only
@XStreamAlias("KeepMethodIs")
public class KeepMethodIsEntry extends MethodMatcher implements IEntry {
	
	public static final String name = "KeepMethodIs";
	
	public KeepMethodIsEntry(String value) {
		super(value);
	}
	
	public KeepMethodIsEntry(Op<String> op) {
		super(op);
	}
	
	public KeepMethodIsEntry(SootMatcherOpType type, String... values) {
		this(getOp(type, false, values));
	}
	
	public KeepMethodIsEntry(SootMatcherOpType type, boolean ic, String... values) {
		this(getOp(type, ic, values));
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public boolean eval(IData data) {
		return evalInner(data, null, null);
	}
	
	@Override
	public boolean evalDebug(IData data, StringBuilder sb, AtomicInteger c) {
		int curC = 0;
		if(sb != null) {
			curC = c.get();
			sb.append("Start Eval ").append(name).append(" ").append(curC).append(" ").append(genValueSig()).append("\n");
		}
		boolean ret = evalInner(data, sb, c);
		if(sb != null)
			sb.append("End Eval ").append(name).append(" ").append(curC).append(" ").append(genValueSig()).append("\nResult: ").append(ret).append("\n");
		return ret;
	}
	
	private boolean evalInner(IData data, StringBuilder sb, AtomicInteger c) {
		if(data instanceof IMethodData) {
			SootMethod m = ((IMethodData)data).getMethod();
			if(m != null)
				return matches(m);
			return matches(((IMethodData)data).getMethodSignature());
		}
		return false;
	}
	
	@Override
	public boolean equals(Object o) {
		if(super.equals(o) && o instanceof KeepMethodIsEntry) {
			return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return IEntry.Factory.genSig(name, Collections.singletonList(genValueSig()));
	}

}
