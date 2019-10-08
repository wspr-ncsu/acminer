package org.sag.acminer.database.filter.restrict;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.sag.acminer.database.defusegraph.DefUseGraph;
import org.sag.acminer.database.defusegraph.INode;
import org.sag.acminer.database.defusegraph.LocalWrapper;
import org.sag.acminer.database.defusegraph.StartNode;
import org.sag.acminer.database.filter.entry.IEntry;
import org.sag.acminer.database.filter.matcher.NumberMatcher;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import soot.Local;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.DefinitionStmt;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.NumericConstant;
import soot.jimple.SwitchStmt;
import soot.jimple.TableSwitchStmt;

@XStreamAlias("IsNumberUsed")
public class IsNumberUsedRestriction extends NumberMatcher implements IRestriction {
	
	public static final String name = "IsNumberUsed";

	public IsNumberUsedRestriction(Op<Number> operation) {
		super(operation);
	}
	
	public IsNumberUsedRestriction(String value) {
		super(value);
	}
	
	/** Constructs a restriction for the following number. Assumes no mask. */
	public IsNumberUsedRestriction(Number num) {
		super(getOp(num));
	}
	
	/** Short cut that makes it easier to specify single value masks for int */
	public IsNumberUsedRestriction(int num, boolean mask) {
		super(num, mask);
	}
	
	/** Short cut that makes it easier to specify single value masks for long */
	public IsNumberUsedRestriction(long num, boolean mask) {
		super(num, mask);
	}
	
	/** Short cut that assumes since we have a list of numbers, this is a mask op for an integer */
	public IsNumberUsedRestriction(int... nums) {
		super(nums);
	}
	
	/** Short cut that assumes since we have a list of numbers, this is a mask op for an long */
	public IsNumberUsedRestriction(long... nums) {
		super(nums);
	}
	
	public String getName() {
		return name;
	}

	@Override
	public Set<INode> applyRestriction(StartNode sn, LocalWrapper lw, DefUseGraph vt, Set<INode> in, StringBuilder sb, 
			AtomicInteger c, Object... objects) {
		
		int curC = 0;
		if(sb != null) {
			curC = c.get();
			sb.append("Start Restriction ").append(name).append(" ").append(curC).append(" ").append(genValueSig()).append("\n");
			sb.append("  Incomming Nodes:\n");
			for(INode n : in) {
				sb.append("    ").append(n).append("\n");
			}
		}
		
		Objects.requireNonNull(lw);
		Objects.requireNonNull(vt);
		Objects.requireNonNull(in);
		Set<INode> ret = new HashSet<>();
		Local localToIgnore = lw.getLocal();
		for(INode vn : in) {
			Unit u = vn.getUnit();
			Value v = null;
			if(vn instanceof StartNode) {
				if(u instanceof IfStmt) {
					v = ((IfStmt)u).getCondition();
				} else if(u instanceof SwitchStmt) {
					if(u instanceof LookupSwitchStmt) {
						for(IntConstant i : ((LookupSwitchStmt)u).getLookupValues()) {
							if(matches(i.value)) {
								ret.add(vn);
								break;
							}
						}
					} else if(u instanceof TableSwitchStmt) {
						for(int i = ((TableSwitchStmt)u).getLowIndex(); i < ((TableSwitchStmt)u).getHighIndex(); i++) {
							if(matches(i)) {
								ret.add(vn);
								break;
							}
						}
					}
					continue;
				}
			} else {
				v = ((DefinitionStmt)u).getRightOp();
			}
			
			for(ValueBox vb : v.getUseBoxes()) {
				Value use = vb.getValue();
				if(!localToIgnore.equals(use)) {
					if(use instanceof NumericConstant) {
						if(matcher(use)) {
							ret.add(vn);
							break;
						}
					} else if(use instanceof Local) {
						Set<INode> defs = vt.getChildLocalsToChildNodes(vn).get(use);
						if(defs != null && !defs.isEmpty()) {
							boolean found = false;
							for(INode def : defs) {
								if(matcher(def)) {
									ret.add(vn);
									found = true;
									break;
								}
							}
							if(found)
								break;
						}
					}
				}
			}
		}
		
		if(ret.isEmpty())
			ret = Collections.emptySet();
		
		if(sb != null) {
			sb.append("End Restriction ").append(name).append(" ").append(curC).append(" ").append(genValueSig()).append("\n");
			sb.append("  Outgoing Nodes:\n");
			for(INode n : ret) {
				sb.append("    ").append(n).append("\n");
			}
		}
		
		return ret;
	}
	
	@Override
	public boolean equals(Object o) {
		if(super.equals(o)) {
			return o instanceof IsNumberUsedRestriction;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return IEntry.Factory.genSig(name, Collections.singletonList(genValueSig()));
	}

}
