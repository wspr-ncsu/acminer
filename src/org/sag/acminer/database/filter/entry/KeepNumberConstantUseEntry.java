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
import org.sag.acminer.database.defusegraph.InvokeConstantLeafNode;
import org.sag.acminer.database.defusegraph.StartNode;
import org.sag.acminer.database.filter.IData;
import org.sag.acminer.database.filter.IData.IDefUseGraphData;
import org.sag.acminer.database.filter.matcher.NumberMatcher;
import org.sag.acminer.database.filter.restrict.Restrictions;

import soot.ValueBox;
import soot.jimple.NumericConstant;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("KeepNumberConstantUse")
public class KeepNumberConstantUseEntry extends NumberMatcher implements IEntry {
	
	public static final String name = "KeepNumberConstantUse";
	
	@XStreamAlias("Restrictions")
	private final Restrictions restrictions;
	
	public KeepNumberConstantUseEntry(String value, Restrictions res) {
		super(value);
		this.restrictions = res;
	}
	
	public KeepNumberConstantUseEntry(Op<Number> op, Restrictions res) {
		super(op);
		this.restrictions = res;
	}
	
	/** Constructs a restriction for the following number. Assumes no mask. */
	public KeepNumberConstantUseEntry(Number num, Restrictions res) {
		this(getOp(num), res);
	}
	
	/** Short cut that makes it easier to specify single value masks for int */
	public KeepNumberConstantUseEntry(int num, boolean mask, Restrictions res) {
		this(mask ? getIntegerMaskOp(num) : getOp(num), res);
	}
	
	/** Short cut that makes it easier to specify single value masks for long */
	public KeepNumberConstantUseEntry(long num, boolean mask, Restrictions res) {
		this(mask ? getLongMaskOp(num) : getOp(num), res);
	}
	
	/** Short cut that assumes since we have a list of numbers, this is a mask op for an integer */
	public KeepNumberConstantUseEntry(Restrictions res, int... nums) {
		this(getIntegerMaskOp(nums), res);
	}
	
	/** Short cut that assumes since we have a list of numbers, this is a mask op for an long */
	public KeepNumberConstantUseEntry(Restrictions res, long... nums) {
		this(getLongMaskOp(nums), res);
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
			Set<INode> numberNodes = new HashSet<>();
			Set<INode> visited = new HashSet<>();
			Deque<INode> toVisit = new LinkedList<>();
			toVisit.push(sn);
			while(!toVisit.isEmpty()) {
				INode cur = toVisit.pop();
				if(visited.add(cur)) {
					if(!(cur instanceof InvokeConstantLeafNode)) {
						for(ValueBox vb : cur.getUnit().getUseBoxes()) {
							if(matcher(vb.getValue()))
								numberNodes.add(cur);
						}
					} else if(cur instanceof InvokeConstantLeafNode || (cur.getValue() != null && cur.getValue() instanceof NumericConstant)) {
						if(matcher(cur.getValue()))
							numberNodes.add(cur);
					}
					
					for(INode vn : vt.getChildNodes(cur)) {
						if(!visited.contains(vn))
							toVisit.push(vn);
					}
				}
			}
			if(numberNodes.isEmpty()) {
				return false;//No fields matching the description -> not a match
			} else if(restrictions == null) {
				return true;//One or more fields match the description and no restrictions -> match
			} else {
				if(c != null) c.incrementAndGet();
				if(restrictions.applyRestriction(sn, null, vt, numberNodes, sb, c).isEmpty())
					return false;
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean equals(Object o) {
		if(super.equals(o) && o instanceof KeepNumberConstantUseEntry) {
			return Objects.equals(((KeepNumberConstantUseEntry)o).restrictions, restrictions);
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
