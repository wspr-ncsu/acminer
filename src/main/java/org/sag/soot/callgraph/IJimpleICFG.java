package org.sag.soot.callgraph;

import heros.solver.IDESolver;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sag.acminer.database.excludedelements.IExcludeHandler;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.soot.analysis.AdvLocalDefs;
import org.sag.soot.analysis.AdvLocalUses;
import org.sag.soot.analysis.ControlDependenceGraph;
import org.sag.soot.analysis.LoopFinder.Loop;

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
import soot.jimple.toolkits.callgraph.EdgePredicate;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.toolkits.graph.UnitGraph;

public interface IJimpleICFG extends BiDiInterproceduralCFG<Unit,SootMethod> {
	
	public abstract List<EntryPoint> getEntryPoints();
	
	public abstract CallGraph getCallGraph();

	public abstract SootMethod getMethodOf(Unit u);

	public abstract List<Unit> getSuccsOf(Unit u);

	public abstract UnitGraph getOrCreateUnitGraph(SootMethod m);

	public abstract UnitGraph getOrCreateUnitGraph(Body body);

	public abstract boolean isExitStmt(Unit u);

	public abstract boolean isStartPoint(Unit u);

	public abstract boolean isFallThroughSuccessor(Unit u, Unit succ);

	public abstract boolean isBranchTarget(Unit u, Unit succ);

	public abstract List<Value> getParameterRefs(SootMethod m);

	public abstract Collection<Unit> getStartPointsOf(SootMethod m);

	public abstract boolean isCallStmt(Unit u);

	public abstract Set<Unit> allNonCallStartNodes();

	public abstract Set<Unit> allNonCallEndNodes();

	public abstract Collection<Unit> getReturnSitesOfCallAt(Unit u);

	public abstract Set<Unit> getCallsFromWithin(SootMethod m);

	public abstract List<Unit> getPredsOf(Unit u);

	/** Returns all the end points (i.e. return and throw statements) 
	 * of the method body of the given method m.
	 */
	public abstract Collection<Unit> getEndPointsOf(SootMethod m);

	public abstract List<Unit> getPredsOfCallAt(Unit u);

	public abstract boolean isReturnSite(Unit n);

	public abstract boolean isReachable(Unit u);

	public abstract ControlDependenceGraph<Unit> getOrMakeControlDependenceGraph(SootMethod m);

	public abstract ControlDependenceGraph<Unit> getOrMakeControlDependenceGraph(Body b);

	public abstract AdvLocalDefs getOrMakeLocalDefs(SootMethod m);

	public abstract AdvLocalDefs getOrMakeLocalDefs(Body b);

	public abstract AdvLocalUses getOrMakeLocalUses(SootMethod m);

	public abstract AdvLocalUses getOrMakeLocalUses(Body b);

	/** Returns the methods called from a unit but only those that have
	 * a body. This appears to be a limitation imposed because {@link IDESolver} fails
	 * to check if the methods called from a unit actually have a body and it appears
	 * to assume that all the methods returned from this call have a body. This could
	 * probably be fixed by simply checking is {@link #getStartPointsOf} returns an
	 * empty set before attempting to perform any of the call related flow functions.
	 */
	public abstract Collection<SootMethod> getCalleesOfCallAt(Unit n);

	/** This is the same as {@link #getCalleesOfCallAt(Unit)} except it 
	 * returns all methods called from a unit even if the method does
	 * not have a body.
	 */
	public abstract Collection<SootMethod> getAllCalleesOfCallAt(Unit n);

	public abstract Collection<Unit> getCallersOf(SootMethod m);

	public abstract List<IdentityStmt> getParameterDefs(SootMethod m);

	public abstract Set<DefinitionStmt> getAllFieldReadsAfter(Unit start, SootField f);

	public abstract Map<SootField, Set<DefinitionStmt>> getAllFieldReadsAfter(Unit start);

	public abstract Set<DefinitionStmt> getAllFieldReadsForMethod(SootMethod m, SootField f);

	public abstract Map<SootField, Set<DefinitionStmt>> getAllFieldReadsForMethod(SootMethod m);

	public abstract Set<DefinitionStmt> getFieldReadsForMethod(SootMethod m, SootField f);

	public abstract Map<SootField, Set<DefinitionStmt>> getFieldReadsForMethod(SootMethod m);
	
	public abstract Set<DefinitionStmt> getDefsForUsedLocals(Unit u);
	
	public abstract Map<Local,Set<DefinitionStmt>> getDefsForUsedLocalsMap(Unit u);
	
	/** For an invoke statement, retrieves all the methods that could possibly be called by
	 * said invoke statement based on the call graph and then returns all the end points
	 * of the bodies of these methods (i.e. return and throw statements).
	 */
	public abstract Set<Unit> getAllEndPointsOfCalleesOfCallAt(Unit invoke);

	/** This returns all the fields read starting at the given start unit and working
	 * upwards to the top of the methods body. This is an interprocedural analysis and
	 * as such, it includes all the field reads for any method invoked as well.
	 */
	public abstract Map<SootField, Set<DefinitionStmt>> getAllFieldReadsAt(Unit start);

	/** This returns all the fields read for the given field starting at the given start unit and working
	 * upwards to the top of the methods body. This is an interprocedural analysis and
	 * as such, it includes all the field reads for the given field for any method invoked as well.
	 */
	public abstract Set<DefinitionStmt> getAllFieldReadsAt(Unit start, SootField f);

	public abstract IBasicEdgePredicate getEdgePredicate();
	
	public abstract Set<Loop> getLoops(SootMethod source);
	
	public abstract Set<Loop> getLoops(Body body);
	
	public abstract SwitchWrapper getOrMakeSwitchWrapper(SwitchStmt stmt);
	
	public interface IBasicEdgePredicate extends EdgePredicate {
		public IExcludeHandler getExcludeHandler();
	}

}