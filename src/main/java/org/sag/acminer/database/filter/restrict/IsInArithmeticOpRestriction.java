package org.sag.acminer.database.filter.restrict;

import java.io.ObjectStreamException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.sag.acminer.database.defusegraph.DefUseGraph;
import org.sag.acminer.database.defusegraph.INode;
import org.sag.acminer.database.defusegraph.LocalWrapper;
import org.sag.acminer.database.defusegraph.StartNode;
import org.sag.acminer.database.filter.entry.IEntry;
import org.sag.common.tuple.Pair;

import soot.Local;
import soot.Value;
import soot.jimple.AddExpr;
import soot.jimple.AndExpr;
import soot.jimple.CmpExpr;
import soot.jimple.CmpgExpr;
import soot.jimple.CmplExpr;
import soot.jimple.DefinitionStmt;
import soot.jimple.DivExpr;
import soot.jimple.EqExpr;
import soot.jimple.GeExpr;
import soot.jimple.GtExpr;
import soot.jimple.IfStmt;
import soot.jimple.LeExpr;
import soot.jimple.LtExpr;
import soot.jimple.MulExpr;
import soot.jimple.NeExpr;
import soot.jimple.NegExpr;
import soot.jimple.OrExpr;
import soot.jimple.ParameterRef;
import soot.jimple.RemExpr;
import soot.jimple.ShlExpr;
import soot.jimple.ShrExpr;
import soot.jimple.Stmt;
import soot.jimple.SubExpr;
import soot.jimple.SwitchStmt;
import soot.jimple.UshrExpr;
import soot.jimple.XorExpr;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("IsInArithmeticOp")
public class IsInArithmeticOpRestriction implements IRestriction {
	
	public static final String name = "IsInArithmeticOp";
	
	public static final String opAdd = "+";
	public static final String opAnd = "&";
	public static final String opCmp = "cmp";
	public static final String opCmpg = "cmpg";
	public static final String opCmpl = "cmpl";
	public static final String opEq = "==";
	public static final String opGteq = ">=";
	public static final String opLteq = "<=";
	public static final String opGt = ">";
	public static final String opLt = "<";
	public static final String opNeq = "!=";
	public static final String opDiv = "/";
	public static final String opMul = "*";
	public static final String opOr = "|";
	public static final String opRem = "rem";//This is probably mod
	public static final String opShl = "<<";
	public static final String opShr = ">>";
	public static final String opSub = "-";
	public static final String opXor = "^";
	public static final String opUshr = ">>>";
	public static final String opNeg = "neg";
	
	@XStreamAlias("Op")
	@XStreamAsAttribute
	public final String op;
	@XStreamOmitField
	private volatile int t;
	@XStreamAlias("Restrictions")
	private final Restrictions restrictions;
	
	public IsInArithmeticOpRestriction(String op) {
		this(op, (Restrictions)null);
	}
	
	public IsInArithmeticOpRestriction(String op, Restrictions res) {
		this.op = op;
		this.t = opToCode(op);
		this.restrictions = res;
	}
	
	public IsInArithmeticOpRestriction(String op, IRestriction... res) {
		this(op, (res == null || res.length == 0) ? null : new Restrictions(res));
	}
	
	public String getName() {
		return name;
	}
	
	protected Object readResolve() throws ObjectStreamException {
		this.t = opToCode(op);
		return this;
	}
	
	protected Object writeReplace() throws ObjectStreamException {
		return this;
	}

	@Override
	public Set<INode> applyRestriction(StartNode sn, LocalWrapper lwIn, DefUseGraph vt, Set<INode> in, StringBuilder sb, 
			AtomicInteger c, Object...objects) {
		
		int curC = 0;
		if(sb != null) {
			curC = c.get();
			sb.append("Start Restriction ").append(name).append(" ").append(curC).append(" Op=").append(op).append("\n");
			sb.append("  Incomming Nodes:\n");
			for(INode n : in) {
				sb.append("    ").append(n).append("\n");
			}
		}
		
		Map<INode,Pair<LocalWrapper,Set<INode>>> firstPass = new HashMap<>();
		for(INode vn : in) {
			Pair<LocalWrapper, Set<INode>> p = vt.getUsesForDefinition(sn, vn);
			if(p != null) {//Should never happen but if the node is start there is nothing to reason about
				Set<INode> seen = new HashSet<>();
				Deque<INode> uses = new ArrayDeque<>(p.getSecond());
				while(!uses.isEmpty()) {
					INode use = uses.poll();
					if(seen.add(use)) {
						Stmt stmt = (Stmt)use.getUnit();
						Value v = null;
						if(use instanceof StartNode) {
							if(stmt instanceof IfStmt) {
								v = ((IfStmt)stmt).getCondition();
							} else if(stmt instanceof SwitchStmt) { //Special handling of switch
								//Only keep the node when it is used in a switch if this restriction is allowing for a equals op
								//since switches can only perform equals operations
								if(t == 6) {
									Pair<LocalWrapper,Set<INode>> cp = firstPass.get(vn);
									if(cp == null) {
										cp = new Pair<LocalWrapper,Set<INode>>(p.getFirst(),new HashSet<INode>());
										firstPass.put(vn, cp);
									}
									cp.getSecond().add(use);
								}
								continue;
							}
						} else {
							v = ((DefinitionStmt)stmt).getRightOp();
							if(v instanceof Local || v instanceof ParameterRef) {
								//Special case where the right side is a local which should not happen because of earlier steps 
								//Basically just keeps moving back until it finds a statement that is not a local assignment
								//Then these are added to the outer use queue for exploration
								Set<INode> queueSeen = new HashSet<>();
								Deque<INode> queue = new ArrayDeque<>();
								queue.add(use);
								while(!queue.isEmpty()) {
									INode cur = queue.poll();
									if(queueSeen.add(cur)) {
										for(INode other : vt.getUsesForDefinition(sn, cur).getSecond()) {
											if(other.getUnit() instanceof Local || other.getUnit() instanceof ParameterRef)
												queue.add(other);
											else
												uses.add(other);
										}
									}
								}
								continue;
							}
						}
						
						boolean keep = false;
						switch(t) {
							case 1: keep = v instanceof AddExpr; break;
							case 2: keep = v instanceof AndExpr; break;
							case 3: keep = v instanceof CmpExpr; break;
							case 4: keep = v instanceof CmpgExpr; break;
							case 5: keep = v instanceof CmplExpr; break;
							case 6: keep = v instanceof EqExpr; break;
							case 7: keep = v instanceof GeExpr; break;
							case 8: keep = v instanceof LeExpr; break;
							case 9: keep = v instanceof GtExpr; break;
							case 10: keep = v instanceof LtExpr; break;
							case 11: keep = v instanceof NeExpr; break;
							case 12: keep = v instanceof DivExpr; break;
							case 13: keep = v instanceof MulExpr; break;
							case 14: keep = v instanceof OrExpr; break;
							case 15: keep = v instanceof RemExpr; break;
							case 16: keep = v instanceof ShlExpr; break;
							case 17: keep = v instanceof ShrExpr; break;
							case 18: keep = v instanceof SubExpr; break;
							case 19: keep = v instanceof XorExpr; break;
							case 20: keep = v instanceof UshrExpr; break;
							case 21: keep = v instanceof NegExpr; break;
						}
						if(keep) {
							Pair<LocalWrapper,Set<INode>> cp = firstPass.get(vn);
							if(cp == null) {
								cp = new Pair<LocalWrapper,Set<INode>>(p.getFirst(),new HashSet<INode>());
								firstPass.put(vn, cp);
							}
							cp.getSecond().add(use);
						}
					}
				}
			}
		}
		
		Set<INode> ret;
		if(firstPass.isEmpty()) {
			ret = Collections.emptySet(); //All failed the first pass so return nothing
		} else if(restrictions == null) {
			ret = new HashSet<>(firstPass.keySet()); //No further restrictions so just return those from the first pass
		} else {
			ret = new HashSet<>();
			for(INode vn : firstPass.keySet()) {
				if(c != null) c.incrementAndGet();
				Set<INode> uses = restrictions.applyRestriction(sn, firstPass.get(vn).getFirst(), vt, firstPass.get(vn).getSecond(), sb, c, objects);
				if(!uses.isEmpty())
					ret.add(vn);
			}
		}
		
		if(sb != null) {
			sb.append("End Restriction ").append(name).append(" ").append(curC).append(" Op=").append(op).append("\n");
			sb.append("  Outgoing Nodes:\n");
			for(INode n : ret) {
				sb.append("    ").append(n).append("\n");
			}
		}
		
		return ret;
	}
	
	protected int opToCode(String op) {
		switch(op) {
			case opAdd: return 1;
			case opAnd: return 2;
			case opCmp: return 3;
			case opCmpg: return 4;
			case opCmpl: return 5;
			case opEq: return 6;
			case opGteq: return 7;
			case opLteq: return 8;
			case opGt: return 9;
			case opLt: return 10;
			case opNeq: return 11;
			case opDiv: return 12;
			case opMul: return 13;
			case opOr: return 14;
			case opRem: return 15;
			case opShl: return 16;
			case opShr: return 17;
			case opSub: return 18;
			case opXor: return 19;
			case opUshr: return 20;
			case opNeg: return 21;
			default: throw new RuntimeException("Error: Unrecongized op '" + op + "'");
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof IsInArithmeticOpRestriction))
			return false;
		return Objects.equals(((IsInArithmeticOpRestriction)o).op,op) && Objects.equals(((IsInArithmeticOpRestriction)o).restrictions,restrictions);
	}
	
	public int hashCode() {
		int i = 17;
		i = i * 31 + Objects.hashCode(op);
		i = i * 31 + Objects.hashCode(restrictions);
		return i;
	}
	
	@Override
	public String toString() {
		List<String> r = new ArrayList<>();
		r.add("Op="+op);
		if(restrictions != null)
			r.add(restrictions.toString());
		return IEntry.Factory.genSig(name, r);
	}

}
