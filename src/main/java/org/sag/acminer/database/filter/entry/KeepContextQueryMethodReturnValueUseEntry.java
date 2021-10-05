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
import org.sag.acminer.database.defusegraph.IInvokeNode;
import org.sag.acminer.database.defusegraph.INode;
import org.sag.acminer.database.defusegraph.StartNode;
import org.sag.acminer.database.filter.IData;
import org.sag.acminer.database.filter.IData.IDataAccessorData;
import org.sag.acminer.database.filter.IData.IDefUseGraphData;
import org.sag.acminer.database.filter.restrict.IRestriction;
import org.sag.acminer.database.filter.restrict.Restrictions;

import soot.SootMethod;
import soot.jimple.Stmt;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("KeepContextQueryMethodReturnValueUse")
public class KeepContextQueryMethodReturnValueUseEntry implements IEntry {

	public static final String name = "KeepContextQueryMethodReturnValueUse";
	
	@XStreamAlias("Restrictions")
	private final Restrictions restrictions;
	
	public KeepContextQueryMethodReturnValueUseEntry() {
		this((Restrictions)null);
	}
	
	public KeepContextQueryMethodReturnValueUseEntry(Restrictions res) {
		this.restrictions = res;
	}
	
	public KeepContextQueryMethodReturnValueUseEntry(IRestriction... res) {
		this((res == null || res.length == 0) ? null : new Restrictions(res));
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
			sb.append("Start Eval ").append(name).append(" ").append(curC).append("\n");
		}
		boolean ret = evalInner(data, sb, c);
		if(sb != null)
			sb.append("End Eval ").append(name).append(" ").append(curC).append("\nResult: ").append(ret).append("\n");
		return ret;
	}
	
	private boolean evalInner(IData data, StringBuilder sb, AtomicInteger c) {
		if(data instanceof IDefUseGraphData && data instanceof IDataAccessorData) {
			Set<SootMethod> contextQueries = ((IDataAccessorData)data).getDataAccessor().getContextQueriesDB().getContextQueries();
			DefUseGraph vt = ((IDefUseGraphData)data).getDefUseGraph();
			StartNode sn = ((IDefUseGraphData)data).getStartNode();
			Set<INode> methodNodes = new HashSet<>();
			Set<INode> visited = new HashSet<>();
			Deque<INode> toVisit = new LinkedList<>();
			toVisit.push(sn);
			while(!toVisit.isEmpty()) {
				INode cur = toVisit.pop();
				if(visited.add(cur)) {
					if(cur instanceof IInvokeNode) {
						SootMethod method = ((IInvokeNode)cur).getTarget();
						if(method == null) {
							String sig = ((Stmt)(((IInvokeNode)cur).getUnit())).getInvokeExpr().getMethodRef().getSignature();
							for(SootMethod sm : contextQueries) {
								if(sig.equals(sm.getSignature())) {
									methodNodes.add(cur);
									break;
								}
							}
						} else {
							if(contextQueries.contains(method))
								methodNodes.add(cur);
						}
					}
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
		if(this == o)
			return true;
		if(o == null || !(o instanceof KeepContextQueryMethodReturnValueUseEntry))
			return false;
		return Objects.equals(((KeepContextQueryMethodReturnValueUseEntry)o).restrictions, restrictions);
	}
	
	@Override
	public int hashCode() {
		int i = 17;
		i = i * 31 + Objects.hashCode(restrictions);
		return i;
	}
	
	@Override
	public String toString() {
		List<String> r = new ArrayList<>();
		if(restrictions != null) {
			r.add(restrictions.toString());
		}
		return Factory.genSig(name, r);
	}
	
}
