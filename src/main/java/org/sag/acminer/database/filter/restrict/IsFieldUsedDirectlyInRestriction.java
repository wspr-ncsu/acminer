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
import org.sag.acminer.database.filter.matcher.FieldMatcher;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import soot.Local;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.DefinitionStmt;
import soot.jimple.IfStmt;
import soot.jimple.SwitchStmt;

@XStreamAlias("IsFieldUsedDirectlyIn")
public class IsFieldUsedDirectlyInRestriction extends FieldMatcher implements IRestriction {
	
	public static final String name = "IsFieldUsedDirectlyIn";
	
	public IsFieldUsedDirectlyInRestriction(String value) {
		super(value);
	}

	public IsFieldUsedDirectlyInRestriction(Op<String> operation) {
		super(operation);
	}
	
	public IsFieldUsedDirectlyInRestriction(SootMatcherOpType type, String... values) {
		super(type, values);
	}
	
	public IsFieldUsedDirectlyInRestriction(SootMatcherOpType type, boolean ic, String... values) {
		super(type, ic, values);
	}
	
	public String getName() {
		return name;
	}
	
	private boolean applyResInner(DefUseGraph vt, INode vn, Value use) {
		Set<INode> defs = vt.getChildLocalsToChildNodes(vn).get(use);
		boolean found = false;
		if(defs != null && !defs.isEmpty()) {
			for(INode def : defs) { //Looking for direct or local to local usage (handled by vt) so only need one step back
				if(matcher(def)) {
					found = true;
					break;
				}
			}
		}
		return found;
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
		
		Objects.requireNonNull(vt);
		Objects.requireNonNull(in);
		Local localToIgnore = null;
		Set<INode> ret = new HashSet<>();
		if(lw != null) {
			localToIgnore = lw.getLocal();
		}
		
		for(INode vn : in) {
			Unit u = vn.getUnit();
			Value v = null;
			if(vn instanceof StartNode) {
				if(u instanceof IfStmt) {
					v = ((IfStmt)u).getCondition();
				} else if(u instanceof SwitchStmt) {
					//Switch stmt has only one local possible
					Value use = ((SwitchStmt)u).getKey();
					if(use instanceof Local && localToIgnore == null && applyResInner(vt, vn, use))
						ret.add(vn);
					continue;
				}
			} else {
				v = ((DefinitionStmt)u).getRightOp();
			}
			
			for(ValueBox vb : v.getUseBoxes()) {
				Value use = vb.getValue();
				if(use instanceof Local && (localToIgnore == null || !localToIgnore.equals(use)) && applyResInner(vt, vn, use)) {
					//fields can only be referenced by local
					ret.add(vn);
					break;
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
			return o instanceof IsFieldUsedDirectlyInRestriction;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return IEntry.Factory.genSig(name, Collections.singletonList(genValueSig()));
	}

}
