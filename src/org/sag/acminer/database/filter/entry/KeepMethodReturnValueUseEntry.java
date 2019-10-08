package org.sag.acminer.database.filter.entry;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.sag.acminer.database.defusegraph.DefUseGraph;
import org.sag.acminer.database.defusegraph.INode;
import org.sag.acminer.database.defusegraph.StartNode;
import org.sag.acminer.database.filter.IData;
import org.sag.acminer.database.filter.IData.IDefUseGraphData;
import org.sag.acminer.database.filter.matcher.MethodMatcher;
import org.sag.acminer.database.filter.restrict.IRestriction;
import org.sag.acminer.database.filter.restrict.Restrictions;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("KeepMethodReturnValueUse")
public class KeepMethodReturnValueUseEntry extends MethodMatcher implements IEntry {

	public static final String name = "KeepMethodReturnValueUse";
	
	@XStreamAlias("Restrictions")
	private final Restrictions restrictions;
	
	public KeepMethodReturnValueUseEntry(String value, Restrictions res) {
		super(value);
		this.restrictions = res;
	}
	
	public KeepMethodReturnValueUseEntry(Op<String> op, Restrictions res) {
		super(op);
		this.restrictions = res;
	}
	
	public KeepMethodReturnValueUseEntry(SootMatcherOpType type, Restrictions res, String... values) {
		this(getOp(type, false, values), res);
	}
	
	public KeepMethodReturnValueUseEntry(SootMatcherOpType type, boolean ic, Restrictions res, String... values) {
		this(getOp(type, ic, values), res);
	}
	
	public KeepMethodReturnValueUseEntry(String value, SootMatcherOpType type, IRestriction... res) {
		this(type, (res == null || res.length == 0) ? null : new Restrictions(res), value);
	}
	
	public KeepMethodReturnValueUseEntry(String value, SootMatcherOpType type, boolean ic, IRestriction... res) {
		this(type, ic, (res == null || res.length == 0) ? null : new Restrictions(res), value);
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
		if(data instanceof IDefUseGraphData) {
			DefUseGraph vt = ((IDefUseGraphData)data).getDefUseGraph();
			StartNode sn = ((IDefUseGraphData)data).getStartNode();
			Set<INode> methodNodes = new HashSet<>();
			Set<INode> visited = new HashSet<>();
			Deque<INode> toVisit = new LinkedList<>();
			toVisit.push(sn);
			while(!toVisit.isEmpty()) {
				INode cur = toVisit.pop();
				if(visited.add(cur)) {
					if(matcher(cur))
						methodNodes.add(cur);
					for(INode vn : vt.getChildNodes(cur)) {
						if(!visited.contains(vn))
							toVisit.push(vn);
					}
				}
			}
			if(methodNodes.isEmpty()) {
				return false;//No methods matching the description -> not a match
			} else if(restrictions == null) {
				return true;//One or more methods match the description and no restrictions -> match
			} else {
				if(c != null) c.incrementAndGet();
				if(restrictions.applyRestriction(sn, null, vt, methodNodes, sb, c).isEmpty())
					return false;
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean equals(Object o) {
		if(super.equals(o) && o instanceof KeepMethodReturnValueUseEntry) {
			return Objects.equals(((KeepMethodReturnValueUseEntry)o).restrictions, restrictions);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		int i = 17;
		i = i * 31 + super.hashCode();
		i = i * 31 + Objects.hashCode(restrictions);
		return i;
	}
	
	@Override
	public String toString() {
		List<String> r = new ArrayList<>();
		r.add(genValueSig());
		if(restrictions != null) {
			r.add(restrictions.toString());
		}
		return IEntry.Factory.genSig(name, r);
	}

}
