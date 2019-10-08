package org.sag.acminer.database.filter.restrict;

import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.sag.acminer.database.defusegraph.DefUseGraph;
import org.sag.acminer.database.defusegraph.INode;
import org.sag.acminer.database.defusegraph.InvokeConstantLeafNode;
import org.sag.acminer.database.defusegraph.LocalWrapper;
import org.sag.acminer.database.defusegraph.StartNode;
import org.sag.acminer.database.filter.entry.IEntry;
import org.sag.common.tuple.Pair;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import soot.Local;
import soot.Value;
import soot.jimple.BinopExpr;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.NegExpr;
import soot.jimple.ParameterRef;

@XStreamAlias("IsInArithmeticChain")
public class IsInArithmeticChainRestriction implements IRestriction {
	
	public static final String name = "IsInArithmeticChain";
	
	@XStreamAlias("HandleConstants")
	@XStreamAsAttribute
	private final boolean handleConstants;
	
	public IsInArithmeticChainRestriction() {
		this(false);
	}
	
	public IsInArithmeticChainRestriction(boolean handleConstants) {
		this.handleConstants = handleConstants;
	}
	
	public String getName() {
		return name;
	}

	@Override
	public Set<INode> applyRestriction(StartNode sn, LocalWrapper lwIn, DefUseGraph vt, Set<INode> in, StringBuilder sb, 
			AtomicInteger c, Object...objects) {
		
		int curC = 0;
		if(sb != null) {
			curC = c.get();
			sb.append("Start Restriction ").append(name).append(" ").append(curC).append(" HandleConstants=").append(handleConstants).append("\n");
			sb.append("  Incomming Nodes:\n");
			for(INode n : in) {
				sb.append("    ").append(n).append("\n");
			}
		}
		
		Set<INode> ret = new HashSet<>();
		Set<Pair<INode,INode>> visited = new HashSet<>();
		Deque<Pair<INode,INode>> toVisit = new LinkedList<>();
		
		if(handleConstants) {
			/* Note no need to worry about InvokeConstantLeafValueNode nodes since we would essentially need to get the uses
			 * of the node and then eliminate those that are not arithmetic expressions (basically the same as below).
			 * Really need to worry about constants that are inlined in the expressions which are not arithmetic expressions. Since
			 * below skips checking of the input nodes (since we want to use this on field refs and invoke expressions when this
			 * flag is not set).
			 * Keep: Definitions of constants assigned to locals, definitions of constants that are passed in as arguments to the source method, 
			 * Binop expressions, Negop expressions, 
			 */
			Set<INode> newIns = new HashSet<>(in);
			for(INode cur : in) {
				if(!(cur instanceof InvokeConstantLeafNode) && !(cur instanceof StartNode)) { 
					//All other nodes should be DefinitionStmt
					Value v = ((DefinitionStmt)cur.getUnit()).getRightOp();
					if(!(v instanceof BinopExpr || v instanceof NegExpr || v instanceof Local || v instanceof ParameterRef || v instanceof Constant))
						newIns.remove(cur);
				}
			}
		}
		
		for(INode i : in) {
			toVisit.push(new Pair<INode,INode>(i,i));
		}
		while(!toVisit.isEmpty()) {
			Pair<INode,INode> p = toVisit.pop();
			if(visited.add(p)) {
				INode cur = p.getFirst();
				INode start = p.getSecond();
				Pair<LocalWrapper, Set<INode>> uses = vt.getUsesForDefinition(sn,cur);
				if(uses == null) {
					ret.add(start);
				} else {
					for(INode use : uses.getSecond()) {
						Pair<INode,INode> newp = null;
						if(use instanceof StartNode) {
							//Start node is not a def but will always be some kind of boolean expression
							newp = new Pair<INode,INode>(use,start);
						} else {
							//All other nodes should be DefinitionStmt
							Value v = ((DefinitionStmt)use.getUnit()).getRightOp();
							if(v instanceof BinopExpr || v instanceof NegExpr || v instanceof Local || v instanceof ParameterRef) {
								newp = new Pair<INode,INode>(use,start);
							}
						}
						if(newp != null && !visited.contains(newp))
							toVisit.add(newp);
					}
				}
			}
		}
		
		if(sb != null) {
			sb.append("End Restriction ").append(name).append(" ").append(curC).append(" HandleConstants=").append(handleConstants).append("\n");
			sb.append("  Outgoing Nodes:\n");
			for(INode n : ret) {
				sb.append("    ").append(n).append("\n");
			}
		}
		
		return ret;
	}
	
	@Override
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof IsInArithmeticChainRestriction))
			return false;
		return ((IsInArithmeticChainRestriction)o).handleConstants == handleConstants;
	}
	
	public int hashCode() {
		int i = 17;
		i = i * 31 + (handleConstants ? 1 : 0);
		return i;
	}
	
	@Override
	public String toString() {
		return IEntry.Factory.genSig(name, Collections.singletonList("HandleConstants="+Boolean.toString(handleConstants)));
	}

}
