package org.sag.acminer.database.filter.entry;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import org.sag.acminer.database.filter.IData;
import org.sag.acminer.database.filter.IData.IStmtData;
import org.sag.acminer.database.filter.matcher.MethodMatcher;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("KeepSourceMethodIs")
public class KeepSourceMethodIsEntry extends MethodMatcher implements IEntry {

	public static final String name = "KeepSourceMethodIs";
	
	public KeepSourceMethodIsEntry(String value) {
		super(value);
	}
	
	public KeepSourceMethodIsEntry(Op<String> op) {
		super(op);
	}
	
	public KeepSourceMethodIsEntry(SootMatcherOpType type, String... values) {
		this(getOp(type, false, values));
	}
	
	public KeepSourceMethodIsEntry(SootMatcherOpType type, boolean ic, String... values) {
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
	
	/** Returns true if the provided statement is in a method (i.e. the source method)
	 * that matches the description given in the constructor of this object and false
	 * otherwise. This method returns false if the data provided is not an instance of
	 * IStmtData. 
	 */
	private boolean evalInner(IData data, StringBuilder sb, AtomicInteger c) {
		if(data instanceof IStmtData)
			return matches(((IStmtData)data).getSource());
		return false;
	}
	
	@Override
	public boolean equals(Object o) {
		if(super.equals(o) && o instanceof KeepSourceMethodIsEntry) {
			return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return IEntry.Factory.genSig(name, Collections.singletonList(genValueSig()));
	}

}
