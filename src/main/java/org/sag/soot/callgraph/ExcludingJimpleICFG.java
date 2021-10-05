package org.sag.soot.callgraph;

import heros.DontSynchronize;
import heros.SynchronizedBy;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import org.sag.acminer.database.excludedelements.IExcludeHandler;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.soot.analysis.AdvLocalDefs;
import org.sag.soot.analysis.AdvLocalUses;
import org.sag.soot.analysis.ControlDependenceGraph;
import org.sag.soot.analysis.LoopFinder.Loop;
import org.sag.soot.callgraph.JimpleICFG.BasicEdgePredicate;

import com.google.common.cache.LoadingCache;

import soot.Body;
import soot.Local;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.DefinitionStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.SwitchStmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.UnitGraph;

public class ExcludingJimpleICFG implements IJimpleICFG {
	
	@DontSynchronize("readonly")
	protected final JimpleICFG jimpleICFG;

	@DontSynchronize("readonly")
	protected final ExcludingEdgePredicate pred;
	
	@DontSynchronize("written by single thread; read afterwards")
	protected final Map<Unit,Body> unitToOwner;
	
	@SynchronizedBy("by use of synchronized LoadingCache class")
	protected final LoadingCache<SootMethod,Collection<Unit>> methodToCallers;
	
	@SynchronizedBy("by use of synchronized LoadingCache class")
	protected final LoadingCache<Unit,Collection<SootMethod>> unitToCallees;
	
	@SynchronizedBy("by use of synchronized LoadingCache class")
	protected final LoadingCache<Unit,Collection<SootMethod>> unitToAllCallees;
	
	@SynchronizedBy("by use of synchronized LoadingCache class")
	protected final LoadingCache<SootMethod,Map<SootField,Set<DefinitionStmt>>> methodToAllFieldReads;//CG dep
	
	@SynchronizedBy("by use of synchronized LoadingCache class")
	protected final LoadingCache<Unit,Map<SootField,Set<DefinitionStmt>>> allFieldReadsAtCache;//CG dep
	
	@SynchronizedBy("by use of synchronized LoadingCache class")
	protected final LoadingCache<Unit,Map<SootField,Set<DefinitionStmt>>> allFieldReadsAfterCache;//CG dep
	
	@SynchronizedBy("by use of synchronized LoadingCache class")
	protected final LoadingCache<Unit, Map<Local, Set<DefinitionStmt>>> unitToDefsForUsedLocals;
	
	@DontSynchronize("readonly")
	protected final EntryPoint ep;
	
	public ExcludingJimpleICFG(EntryPoint ep, Collection<EntryPoint> eps, ExcludingEdgePredicate pred) {
		this(ep, new JimpleICFG(eps,false),pred);
	}
	
	public ExcludingJimpleICFG(EntryPoint ep, JimpleICFG jimpleICFG, ExcludingEdgePredicate pred) {
		Objects.requireNonNull(ep);
		Objects.requireNonNull(jimpleICFG);
		Objects.requireNonNull(pred);
		this.jimpleICFG = jimpleICFG;
		this.pred = pred;
		this.ep = ep;
		//These three caches depend on the call graph and are thus effected by changes in the exclude list
		//So we cannot use them as in in the base class, they have to have their own caches per instance of this class
		this.methodToCallers = JimpleICFG.getNewMethodToCallersCache(this);
		this.unitToCallees = JimpleICFG.getNewUnitToCalleesCache(this);
		this.unitToAllCallees = JimpleICFG.getNewUnitToAllCalleesCache(this);
		this.methodToAllFieldReads = JimpleICFG.getNewMethodToAllFieldReads(this);
		this.allFieldReadsAtCache = JimpleICFG.getNewAllFieldReadsAtCache(this);
		this.allFieldReadsAfterCache = JimpleICFG.getNewAllFieldReadsAfterCache(this);
		this.unitToOwner = JimpleICFG.initUnitToBody(Collections.singleton(ep), pred, jimpleICFG.getCallGraph());
		this.unitToDefsForUsedLocals = JimpleICFG.getNewUnitToDefsForUsedLocals(this,jimpleICFG.advLocalDefs);
	}
	
	public EntryPoint getEntryPoint() {
		return ep;
	}
	
	@Override
	public List<EntryPoint> getEntryPoints() {
		return Collections.singletonList(ep);
	}

	@Override
	public SootMethod getMethodOf(Unit u) {
		return JimpleICFG.getMethodOf(unitToOwner, u);
	}

	@Override
	public List<Unit> getSuccsOf(Unit u) {
		return JimpleICFG.getSuccsOf(unitToOwner, jimpleICFG.bodyToUnitGraph, pred, u);
	}

	@Override
	public UnitGraph getOrCreateUnitGraph(SootMethod m) {
		return JimpleICFG.getOrCreateUnitGraph(jimpleICFG.bodyToUnitGraph, pred, m);
	}

	@Override
	public UnitGraph getOrCreateUnitGraph(Body body) {
		return JimpleICFG.getOrCreateUnitGraph(jimpleICFG.bodyToUnitGraph, pred, body);
	}

	@Override
	public boolean isExitStmt(Unit u) {
		return JimpleICFG.isExitStmt(unitToOwner, jimpleICFG.bodyToUnitGraph, pred, u);
	}

	@Override
	public boolean isStartPoint(Unit u) {
		return JimpleICFG.isStartPoint(unitToOwner, jimpleICFG.bodyToUnitGraph, pred, u);
	}

	@Override
	public boolean isFallThroughSuccessor(Unit u, Unit succ) {
		return JimpleICFG.isFallThroughSuccessor(unitToOwner, u, succ);
	}

	@Override
	public boolean isBranchTarget(Unit u, Unit succ) {
		return jimpleICFG.isBranchTarget(u, succ);
	}

	@Override
	public List<Value> getParameterRefs(SootMethod m) {
		return JimpleICFG.getParameterRefs(jimpleICFG.methodToParameterRefs, m);
	}

	@Override
	public Collection<Unit> getStartPointsOf(SootMethod m) {
		return JimpleICFG.getStartPointsOf(jimpleICFG.bodyToUnitGraph, pred, m);
	}

	@Override
	public boolean isCallStmt(Unit u) {
		return JimpleICFG.isCallStmtS(u);
	}

	@Override
	public Set<Unit> allNonCallStartNodes() {
		return JimpleICFG.allNonCallStartNodes(unitToOwner, jimpleICFG.bodyToUnitGraph, pred);
	}

	@Override
	public Set<Unit> allNonCallEndNodes() {
		return JimpleICFG.allNonCallEndNodes(unitToOwner, jimpleICFG.bodyToUnitGraph, pred);
	}

	@Override
	public Collection<Unit> getReturnSitesOfCallAt(Unit u) {
		return JimpleICFG.getReturnSitesOfCallAt(unitToOwner, jimpleICFG.bodyToUnitGraph, pred, u);
	}

	@Override
	public Set<Unit> getCallsFromWithin(SootMethod m) {
		return JimpleICFG.getCallsFromWithin(jimpleICFG.methodToCallsFromWithin, m);
	}

	@Override
	public List<Unit> getPredsOf(Unit u) {
		return JimpleICFG.getPredsOf(unitToOwner, jimpleICFG.bodyToUnitGraph, pred, u);
	}

	@Override
	public Collection<Unit> getEndPointsOf(SootMethod m) {
		return JimpleICFG.getEndPointsOf(unitToOwner, jimpleICFG.bodyToUnitGraph, pred, m);
	}

	@Override
	public List<Unit> getPredsOfCallAt(Unit u) {
		return JimpleICFG.getPredsOfCallAt(unitToOwner, jimpleICFG.bodyToUnitGraph, pred, u);
	}

	@Override
	public boolean isReturnSite(Unit n) {
		return JimpleICFG.isReturnSite(unitToOwner, jimpleICFG.bodyToUnitGraph, pred, n);
	}

	@Override
	public boolean isReachable(Unit u) {
		return JimpleICFG.isReachable(unitToOwner, u);
	}

	@Override
	public ControlDependenceGraph<Unit> getOrMakeControlDependenceGraph(SootMethod m) {
		return JimpleICFG.getOrMakeControlDependenceGraph(jimpleICFG.controlDependenceGraphs, pred, m);
	}

	@Override
	public ControlDependenceGraph<Unit> getOrMakeControlDependenceGraph(Body b) {
		return JimpleICFG.getOrMakeControlDependenceGraph(jimpleICFG.controlDependenceGraphs, pred, b);
	}

	@Override
	public AdvLocalDefs getOrMakeLocalDefs(SootMethod m) {
		return JimpleICFG.getOrMakeLocalDefs(jimpleICFG.advLocalDefs, pred, m);
	}

	@Override
	public AdvLocalDefs getOrMakeLocalDefs(Body b) {
		return JimpleICFG.getOrMakeLocalDefs(jimpleICFG.advLocalDefs, pred, b);
	}

	@Override
	public AdvLocalUses getOrMakeLocalUses(SootMethod m) {
		return JimpleICFG.getOrMakeLocalUses(jimpleICFG.advLocalUses, pred, m);
	}

	@Override
	public AdvLocalUses getOrMakeLocalUses(Body b) {
		return JimpleICFG.getOrMakeLocalUses(jimpleICFG.advLocalUses, pred, b);
	}

	@Override
	public Collection<SootMethod> getCalleesOfCallAt(Unit n) {
		return JimpleICFG.getCalleesOfCallAt(unitToCallees, n);
	}

	@Override
	public Collection<SootMethod> getAllCalleesOfCallAt(Unit n) {
		return JimpleICFG.getAllCalleesOfCallAt(unitToAllCallees, n);
	}

	@Override
	public Collection<Unit> getCallersOf(SootMethod m) {
		return JimpleICFG.getCallersOf(methodToCallers, pred, m);
	}
	
	@Override
	public List<IdentityStmt> getParameterDefs(SootMethod m) {
		return JimpleICFG.getParameterDefs(jimpleICFG.methodToParameterDefs, pred, m);
	}
	
	@Override
	public Map<SootField,Set<DefinitionStmt>> getAllFieldReadsForMethod(SootMethod m) {
		return JimpleICFG.getAllFieldReadsForMethod(methodToAllFieldReads, pred, m);
	}
	
	@Override
	public Set<DefinitionStmt> getAllFieldReadsAfter(Unit start, SootField f) {
		return JimpleICFG.getAllFieldReadsAfter(allFieldReadsAfterCache, start, f);
	}

	@Override
	public Map<SootField, Set<DefinitionStmt>> getAllFieldReadsAfter(Unit start) {
		return JimpleICFG.getAllFieldReadsAfter(allFieldReadsAfterCache, start);
	}

	@Override
	public Set<DefinitionStmt> getAllFieldReadsForMethod(SootMethod m, SootField f) {
		return JimpleICFG.getAllFieldReadsForMethod(methodToAllFieldReads, pred, m, f);
	}

	@Override
	public Set<DefinitionStmt> getFieldReadsForMethod(SootMethod m, SootField f) {
		return JimpleICFG.getFieldReadsForMethod(jimpleICFG.methodToFieldReads, pred, m, f);
	}

	@Override
	public Map<SootField, Set<DefinitionStmt>> getFieldReadsForMethod(SootMethod m) {
		return JimpleICFG.getFieldReadsForMethod(jimpleICFG.methodToFieldReads, pred, m);
	}
	
	public static class ExcludingEdgePredicate extends BasicEdgePredicate {
		protected final Set<Edge> allowedEdges;
		public ExcludingEdgePredicate(CallGraph cg, IExcludeHandler excludeHandler) {
			this(cg,excludeHandler,false);
		}
		public ExcludingEdgePredicate(CallGraph cg, IExcludeHandler excludeHandler, boolean includeReflectiveCalls) {
			super(includeReflectiveCalls,excludeHandler);
			Objects.requireNonNull(excludeHandler);
			Objects.requireNonNull(cg);
			this.allowedEdges = new HashSet<>();
			init(cg);
		}
		//We perform a forward traversal of the callgraph starting at the entry point and record all reachable edges
		//This ensures that a backwards traversal will only include edges that are accessible from a forward traversal
		//In the face of deleted edges and excluded methods, it is possible that an backwards traversal would include 
		//edges not in the forward traversal. Recording all allowed edges in a forward traversal prevents this.
		protected void init(CallGraph cg) {
			Set<SootMethod> visited = new HashSet<>();
			Queue<SootMethod> toVisit = new ArrayDeque<>();
			toVisit.add(excludeHandler.getEntryPoint());
			while(!toVisit.isEmpty()) {
				SootMethod cur = toVisit.poll();
				if(visited.add(cur) && !excludeHandler.isExcludedMethodWithOverride(cur)) {
					Iterator<Edge> it = cg.edgesOutOf(cur);
					while(it.hasNext()) {
						Edge e = it.next();
						if(super.want(e)) {
							allowedEdges.add(e);
							toVisit.add(e.tgt());
						}
					}
				}
			}
		}
		@Override
		public boolean want(Edge e) {
			return allowedEdges.contains(e);
		}
		public IExcludeHandler getExcludeHandler() {
			return excludeHandler;
		}
		public Set<Edge> getAllowedEdges() {
			return new HashSet<>(allowedEdges);
		}
	}

	@Override
	public Set<DefinitionStmt> getDefsForUsedLocals(Unit u) {
		return JimpleICFG.getDefsForUsedLocals(unitToOwner, unitToDefsForUsedLocals, u);
	}
	
	@Override
	public Map<Local,Set<DefinitionStmt>> getDefsForUsedLocalsMap(Unit u) {
		return JimpleICFG.getDefsForUsedLocalsMap(unitToOwner, unitToDefsForUsedLocals, u);
	}

	@Override
	public Set<Unit> getAllEndPointsOfCalleesOfCallAt(Unit invoke) {
		return JimpleICFG.getAllEndPointsOfCalleesOfCallAt(unitToOwner, jimpleICFG.bodyToUnitGraph, pred, unitToCallees, invoke);
	}

	@Override
	public CallGraph getCallGraph() {
		return jimpleICFG.getCallGraph();
	}

	@Override
	public Map<SootField, Set<DefinitionStmt>> getAllFieldReadsAt(Unit start) {
		return JimpleICFG.getAllFieldReadsAt(allFieldReadsAtCache, start);
	}

	@Override
	public Set<DefinitionStmt> getAllFieldReadsAt(Unit start, SootField f) {
		return JimpleICFG.getAllFieldReadsAt(allFieldReadsAtCache, start, f);
	}

	@Override
	public IBasicEdgePredicate getEdgePredicate() {
		return pred;
	}

	@Override
	public Set<Loop> getLoops(SootMethod source) {
		return JimpleICFG.getLoops(jimpleICFG.bodyToLoopsCache, pred, source);
	}

	@Override
	public Set<Loop> getLoops(Body body) {
		return JimpleICFG.getLoops(jimpleICFG.bodyToLoopsCache, pred, body);
	}

	@Override
	public SwitchWrapper getOrMakeSwitchWrapper(SwitchStmt stmt) {
		return JimpleICFG.getOrMakeSwitchWrapper(jimpleICFG.switchStmtToSwitchWrapperCache, stmt);
	}

}
