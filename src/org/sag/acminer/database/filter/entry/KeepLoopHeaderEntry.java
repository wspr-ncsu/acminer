package org.sag.acminer.database.filter.entry;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sag.acminer.database.defusegraph.DefUseGraph;
import org.sag.acminer.database.defusegraph.IInvokeNode;
import org.sag.acminer.database.defusegraph.INode;
import org.sag.acminer.database.defusegraph.LocalWrapper;
import org.sag.acminer.database.defusegraph.StartNode;
import org.sag.acminer.database.filter.IData;
import org.sag.acminer.database.filter.IData.IDefUseGraphData;
import org.sag.acminer.database.filter.IData.IJimpleICFGData;
import org.sag.acminer.database.filter.IData.IStmtData;
import org.sag.common.tools.HierarchyHelpers;
import org.sag.common.tools.SortingMethods;
import org.sag.soot.SootSort;
import org.sag.soot.analysis.LoopFinder.Loop;
import org.sag.xstream.xstreamconverters.NamedCollectionConverterWithSize;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Value;
import soot.jimple.BinopExpr;
import soot.jimple.LengthExpr;
import soot.jimple.Stmt;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("KeepLoopHeader")
public class KeepLoopHeaderEntry implements IEntry {
	
	public static final String name = "KeepLoopHeader";
	private static final Pattern methodSplit =  Pattern.compile("^<([^:\\s]+):\\s+(.+)>$");
	
	@XStreamAlias("MatchExitsUsingLengthOf")
	private final boolean matchExitsUsingLengthOf;
	@XStreamAlias("MatchLoopIncrement")
	private final boolean matchLoopIncrement;
	@XStreamAlias("ConditionalMethods")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"ConditionalMethod"},types={String.class})
	private final LinkedHashSet<String> loopConditionalMethods;
	@XStreamOmitField
	private volatile Set<String> loopConditionalMethodsCache;
	
	public KeepLoopHeaderEntry(boolean matchExitsUsingLengthOf, boolean matchLoopIncrement, 
			Set<String> loopConditionalMethods) {
		this.matchExitsUsingLengthOf = matchExitsUsingLengthOf;
		this.matchLoopIncrement = matchLoopIncrement;
		this.loopConditionalMethods = SortingMethods.sortSet(loopConditionalMethods,SootSort.smStringComp);
		this.loopConditionalMethodsCache = null;
	}

	@Override
	public String getName() {
		return name;
	}
	
	private boolean matchesLoopConditionalMethod(INode node) {
		if(loopConditionalMethodsCache == null) {
			synchronized(this) {
				if(loopConditionalMethodsCache == null) {
					loopConditionalMethodsCache = new HashSet<>();
					for(String val : loopConditionalMethods) {
						Matcher m = methodSplit.matcher(val);
						if(m.matches()) { //val is a signature
							String className = m.group(1);
							String subSig = m.group(2);
							SootClass sc = Scene.v().getSootClassUnsafe(className,false);
							if(sc != null) {
								loopConditionalMethodsCache.addAll(HierarchyHelpers.getAllPossibleInvokeSignatures(sc, subSig));
							}
						}
					}
				}
			}
		}
		
		if(node instanceof IInvokeNode) {
			String sig;
			SootMethod method = ((IInvokeNode)node).getTarget();
			if(method == null)
				sig = ((Stmt)(((IInvokeNode)node).getUnit())).getInvokeExpr().getMethodRef().getSignature();
			else
				sig = method.getSignature();
			return loopConditionalMethodsCache.contains(sig);
		}
		return false;
	}
	
	private boolean matchesLoopIncrement(INode node, LocalWrapper lw, DefUseGraph vt) {
		Value v = node.getValue();
		if(v instanceof BinopExpr && vt.getChildLocalWrappers(node).contains(lw)) {
			return true;
		}
		return false;
	}
	
	private boolean matchesLengthOf(INode node) {
		return node.getValue() instanceof LengthExpr;
	}

	/** Return true if the statement is a loop header and false otherwise. If the data given
	 * does not provide a statement and a JimpleICFG then this returns false.
	 */
	private boolean evalInner(IData data, StringBuilder sb, AtomicInteger c) {
		if(data instanceof IJimpleICFGData && data instanceof IStmtData) {
			Set<Loop> loops = ((IJimpleICFGData)data).getJimpleICFG().getLoops(((IStmtData)data).getSource());
			Stmt cp = ((IStmtData)data).getStmt();
			boolean isExitStmt = false;
			boolean isSimpleLoop = false;
			for(Loop l : loops) {
				if(l.getExitStmts().contains(cp)) {
					isExitStmt = true;
					isSimpleLoop = l.isSimple();
					break;
				}
			}
			if(isExitStmt) {
				if(isSimpleLoop) {
					return true;
				} else {
					if(data instanceof IDefUseGraphData) {
						DefUseGraph vt = ((IDefUseGraphData)data).getDefUseGraph();
						StartNode sn = ((IDefUseGraphData)data).getStartNode();
						Map<LocalWrapper, Set<INode>> children = vt.getChildLocalWrappersToChildNodes(sn);
						for(LocalWrapper lw : children.keySet()) {
							for(INode vn : children.get(lw)) {
								if((matchExitsUsingLengthOf && matchesLengthOf(vn))
										|| (matchLoopIncrement && matchesLoopIncrement(vn, lw, vt))
										|| matchesLoopConditionalMethod(vn)) {
									return true;
								}
							}
						}
					}
				}
			}
		}
		return false;
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
	
	@Override
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof KeepLoopHeaderEntry))
			return false;
		return true;
	}
	
	@Override
	public int hashCode() {
		return 527 + name.hashCode();
	}
	
	@Override
	public String toString() {
		return Factory.genSig(name, Collections.<String>emptyList());
	}

}
