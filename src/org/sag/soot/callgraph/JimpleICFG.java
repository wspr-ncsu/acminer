package org.sag.soot.callgraph;

import heros.DontSynchronize;
import heros.SynchronizedBy;
import heros.solver.IDESolver;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.sag.acminer.database.excludedelements.IExcludeHandler;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.soot.analysis.AdvLocalDefs;
import org.sag.soot.analysis.AdvLocalUses;
import org.sag.soot.analysis.ControlDependenceGraph;
import org.sag.soot.analysis.LoopFinder;
import org.sag.soot.analysis.LoopFinder.Loop;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;

import soot.Body;
import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.UnitBox;
import soot.Value;
import soot.ValueBox;
import soot.jimple.DefinitionStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.ParameterRef;
import soot.jimple.Stmt;
import soot.jimple.SwitchStmt;
import soot.jimple.internal.VariableBox;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.Filter;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.LiveLocals;

public class JimpleICFG implements IJimpleICFG {
	
	@DontSynchronize("readonly")
	protected volatile IBasicEdgePredicate pred;
	
	@DontSynchronize("readonly")
	protected final CallGraph cg;
	
	@DontSynchronize("written by single thread; read afterwards")
	protected volatile List<EntryPoint> eps;
	
	@DontSynchronize("readonly")
	protected final boolean enableExceptions;
	
	@DontSynchronize("written by single thread; read afterwards")
	protected volatile Map<Unit,Body> unitToOwner;
	
	@SynchronizedBy("by use of synchronized LoadingCache class")
	protected final LoadingCache<Body,UnitGraph> bodyToUnitGraph;
	
	@SynchronizedBy("by use of synchronized LoadingCache class")
	protected final LoadingCache<SootMethod,List<Value>> methodToParameterRefs;

	@SynchronizedBy("by use of synchronized LoadingCache class")
	protected final LoadingCache<SootMethod,Set<Unit>> methodToCallsFromWithin;
	
	@SynchronizedBy("by use of synchronized LoadingCache class")
	protected final LoadingCache<Body,ControlDependenceGraph<Unit>> controlDependenceGraphs;
	
	@SynchronizedBy("by use of synchronized LoadingCache class")
	protected final LoadingCache<Body,AdvLocalDefs> advLocalDefs;
	
	@SynchronizedBy("by use of synchronized LoadingCache class")
	protected final LoadingCache<Body,AdvLocalUses> advLocalUses;
	
	@SynchronizedBy("by use of synchronized LoadingCache class")
	protected volatile LoadingCache<SootMethod,Collection<Unit>> methodToCallers;//CG dep
	
	@SynchronizedBy("by use of synchronized LoadingCache class")
	protected volatile LoadingCache<Unit,Collection<SootMethod>> unitToCallees;//CG dep
	
	@SynchronizedBy("by use of synchronized LoadingCache class")
	protected volatile LoadingCache<Unit,Collection<SootMethod>> unitToAllCallees;//CG dep
	
	@SynchronizedBy("by use of synchronized LoadingCache class")
	protected final LoadingCache<SootMethod,List<IdentityStmt>> methodToParameterDefs;
	
	@SynchronizedBy("by use of synchronized LoadingCache class")
	protected final LoadingCache<SootMethod,Map<SootField,Set<DefinitionStmt>>> methodToFieldReads;
	
	@SynchronizedBy("by use of synchronized LoadingCache class")
	protected volatile LoadingCache<SootMethod,Map<SootField,Set<DefinitionStmt>>> methodToAllFieldReads;//CG dep
	
	@SynchronizedBy("by use of synchronized LoadingCache class")
	protected volatile LoadingCache<Unit,Map<SootField,Set<DefinitionStmt>>> allFieldReadsAtCache;//CG dep
	
	@SynchronizedBy("by use of synchronized LoadingCache class")
	protected volatile LoadingCache<Unit,Map<SootField,Set<DefinitionStmt>>> allFieldReadsAfterCache;//CG dep
	
	@SynchronizedBy("by use of synchronized LoadingCache class")
	protected volatile LoadingCache<Unit,Map<Local,Set<DefinitionStmt>>> unitToDefsForUsedLocals;
	
	@SynchronizedBy("by use of synchronized LoadingCache class")
	protected final LoadingCache<Body,Set<Loop>> bodyToLoopsCache;
	
	@SynchronizedBy("by use of synchronized LoadingCache class")
	protected final LoadingCache<SwitchStmt,SwitchWrapper> switchStmtToSwitchWrapperCache;
	
	public JimpleICFG(Collection<EntryPoint> eps) {
		this(eps,new BasicEdgePredicate());
	}
	
	//Pass in null pred to disable CG caching
	public JimpleICFG(Collection<EntryPoint> eps, IBasicEdgePredicate pred) {
		this(eps,pred,true);
	}

	public JimpleICFG(Collection<EntryPoint> eps, IBasicEdgePredicate pred, boolean enableExceptions) {
		this(enableExceptions);
		this.eps = ImmutableList.copyOf(eps);
		this.pred = pred;
		this.methodToCallers = getNewMethodToCallersCache(this);
		this.unitToCallees = getNewUnitToCalleesCache(this);
		this.unitToAllCallees = getNewUnitToAllCalleesCache(this);
		this.methodToAllFieldReads = getNewMethodToAllFieldReads(this);
		this.allFieldReadsAtCache = getNewAllFieldReadsAtCache(this);
		this.allFieldReadsAfterCache = getNewAllFieldReadsAfterCache(this);
		this.unitToDefsForUsedLocals = getNewUnitToDefsForUsedLocals(this,this.advLocalDefs);
		this.unitToOwner = initUnitToBody(eps, pred, cg);
	}
	
	//Base constructor for the fields whose data is not affected by the call graph
	private JimpleICFG(boolean enableExceptions) {
		this.enableExceptions = enableExceptions;
		this.cg = Scene.v().getCallGraph();
		this.bodyToUnitGraph = getNewBodyUnitGraphCache(enableExceptions);//Must be created first because of its use below
		this.advLocalDefs = getNewLocalDefs(this.bodyToUnitGraph); //The this because of dependencies
		this.advLocalUses = getNewLocalUses(this.bodyToUnitGraph, this.advLocalDefs);
		this.controlDependenceGraphs = getNewControlDependenceGraphs(this.bodyToUnitGraph);
		this.bodyToLoopsCache = getNewBodyToLoopsCache();
		this.methodToParameterRefs = getNewMethodToParameterRefsCache();
		this.methodToCallsFromWithin = getNewMethodToCallsFromWithinCache();
		this.methodToParameterDefs = getNewMethodToParameterDefsCache();
		this.methodToFieldReads = getNewMethodToFieldReads();
		this.switchStmtToSwitchWrapperCache = getNewSwitchStmtToSwitchWrapperCache();
	}
	
	/** Special constructor whose should only be called in the case where this 
	 * ICFG is going to be wrapped by many other ICFG whose call graph views differ.
	 * The fields who are affected by changes in the call graph are all set to null.
	 * 
	 * @param eps - entry points for the overall call graph like the others
	 * @param bla - unused arg (just to separate it from other constructors)
	 */
	public JimpleICFG(Collection<EntryPoint> eps, boolean bla) {
		this(true);
		this.eps = null;
		this.pred = null;
		this.methodToCallers = null;
		this.unitToCallees = null;
		this.unitToAllCallees = null;
		this.methodToAllFieldReads = null;
		this.allFieldReadsAtCache = null;
		this.allFieldReadsAfterCache = null;
		this.unitToOwner = null;
		this.unitToDefsForUsedLocals = null;
		initUnitToBody(eps, pred, cg);//Run this but don't assign it so the bodies are resolved but the memory is not wasted
	}
	
	protected static Map<Unit,Body> initUnitToBody(Collection<EntryPoint> eps, IBasicEdgePredicate pred, CallGraph cg) {
		Map<Unit,Body> ret = new HashMap<>();
		Set<SootMethod> seen = new HashSet<>();
		//Init unit to body and retrieve body for all methods mentioned in CG
		for(Edge e : cg) {
			SootMethod src = e.src();
			SootMethod tgt = e.tgt();
			if(seen.add(src))
				initUnitToBody(ret, pred, src);
			if(seen.add(tgt))
				initUnitToBody(ret, pred, tgt);
		}
		//Init unit to body for eps as some may have no outgoing or incoming edges
		for(EntryPoint ep : eps) {
			if(ep.getEntryPoint() != null && seen.add(ep.getEntryPoint()))
				initUnitToBody(ret, pred, ep.getEntryPoint());
		}
		return ret;
	}
	
	private static void initUnitToBody(Map<Unit,Body> unitToOwner, IBasicEdgePredicate pred, SootMethod m) {
		if(m.isConcrete() && (pred == null || !pred.getExcludeHandler().isExcludedMethodWithOverride(m))) {
			Body b = m.retrieveActiveBody();
			for(Unit u : b.getUnits())
				unitToOwner.put(u, b);
		}
	}
	
	protected static LoadingCache<Body,UnitGraph> getNewBodyUnitGraphCache(final boolean enableExceptions) {
		return IDESolver.DEFAULT_CACHE_BUILDER.build( new CacheLoader<Body,UnitGraph>() {
			@Override
			public UnitGraph load(Body body) throws Exception {
				return enableExceptions ? new ExceptionalUnitGraph(body) : new BriefUnitGraph(body);
			}
		});
	}
	
	protected static LoadingCache<SootMethod,List<Value>> getNewMethodToParameterRefsCache() {
		return IDESolver.DEFAULT_CACHE_BUILDER.build( new CacheLoader<SootMethod,List<Value>>() {
			@Override
			public List<Value> load(SootMethod m) throws Exception {
				if(m.hasActiveBody()) {
					return Collections.unmodifiableList(m.getActiveBody().getParameterRefs());
				} else {
					//getParameterRefs returns a fixed size list as long as the number of parameters 
					//with null set for each parameter that does not have a reference in the body
					Value[] res = new Value[m.getParameterCount()];
					Arrays.fill(res, null);
					return Collections.unmodifiableList(Arrays.asList(res));
				}
			}
		});
	}
	
	protected static LoadingCache<SwitchStmt,SwitchWrapper> getNewSwitchStmtToSwitchWrapperCache() {
		return IDESolver.DEFAULT_CACHE_BUILDER.build(new CacheLoader<SwitchStmt, SwitchWrapper> () {
			@Override
			public SwitchWrapper load(SwitchStmt key) throws Exception {
				return new SwitchWrapper(key);
			}
		});
	}
	
	protected static LoadingCache<SootMethod,List<IdentityStmt>> getNewMethodToParameterDefsCache() {
		return IDESolver.DEFAULT_CACHE_BUILDER.build( new CacheLoader<SootMethod,List<IdentityStmt>>() {
			@Override
			public List<IdentityStmt> load(SootMethod m) throws Exception {
				IdentityStmt[] res = new IdentityStmt[m.getParameterCount()];
				Arrays.fill(res, null);
				if(m.hasActiveBody()) {
			        for (Unit s : m.getActiveBody().getUnits()) {
			            if (s instanceof IdentityStmt) {
							Value rightOp = ((IdentityStmt)s).getRightOp();
							if (rightOp instanceof ParameterRef) {
								ParameterRef parameterRef = (ParameterRef) rightOp;
								if(res[parameterRef.getIndex()] != null)
									throw new RuntimeException("Error: Mutiple parameter defintions for parameter '" 
											+ parameterRef.getIndex() + "' of method '" + m + "'.");
								res[parameterRef.getIndex()] = (IdentityStmt)s;
							}
						}
			        }
				}
		        return Collections.unmodifiableList(Arrays.asList(res));
			}
		});
	}
	
	protected static LoadingCache<SootMethod,Set<Unit>> getNewMethodToCallsFromWithinCache() {
		return IDESolver.DEFAULT_CACHE_BUILDER.build( new CacheLoader<SootMethod,Set<Unit>>() {
			@Override
			public Set<Unit> load(SootMethod m) throws Exception {
				Set<Unit> res = null;
				if(m.hasActiveBody()) {
					for(Unit u: m.getActiveBody().getUnits()) {
						if(isCallStmtS(u)) {
							if (res == null)
								res = new LinkedHashSet<Unit>();
							res.add(u);
						}
					}
				}
				return res == null ? Collections.<Unit>emptySet() : Collections.unmodifiableSet(res);
			}
		});
	}
	
	protected static LoadingCache<Body,ControlDependenceGraph<Unit>> getNewControlDependenceGraphs(final LoadingCache<Body,UnitGraph> bodyToUnitGraph) {
		return IDESolver.DEFAULT_CACHE_BUILDER.build(new CacheLoader<Body,ControlDependenceGraph<Unit>>() {
			@Override
			public ControlDependenceGraph<Unit> load(Body b) throws Exception {
				return new ControlDependenceGraph<Unit>(getOrCreateUnitGraph(bodyToUnitGraph, null, b));
			}
		});
	}
	
	protected static LoadingCache<Body,AdvLocalDefs> getNewLocalDefs(final LoadingCache<Body,UnitGraph> bodyToUnitGraph) {
		return IDESolver.DEFAULT_CACHE_BUILDER.build(new CacheLoader<Body,AdvLocalDefs>() {
			@Override
			public AdvLocalDefs load(Body b) throws Exception {
				UnitGraph g = getOrCreateUnitGraph(bodyToUnitGraph, null, b);
				return new AdvLocalDefs(g,LiveLocals.Factory.newLiveLocals(g));
			}
		});
	}
	
	protected static LoadingCache<Body,AdvLocalUses> getNewLocalUses(final LoadingCache<Body,UnitGraph> bodyToUnitGraph, 
			final LoadingCache<Body,AdvLocalDefs> advLocalDefs) {
		return IDESolver.DEFAULT_CACHE_BUILDER.build(new CacheLoader<Body,AdvLocalUses>() {
			@Override
			public AdvLocalUses load(Body b) throws Exception {
				return new AdvLocalUses(getOrCreateUnitGraph(bodyToUnitGraph, null, b),getOrMakeLocalDefs(advLocalDefs, null, b));
			}
		});
	}
	
	protected static LoadingCache<SootMethod,Collection<Unit>> getNewMethodToCallersCache(final IJimpleICFG icfg) {
		if(icfg == null)
			return null;
		return IDESolver.DEFAULT_CACHE_BUILDER.build( new CacheLoader<SootMethod,Collection<Unit>>() {
			@Override
			public Collection<Unit> load(SootMethod m) throws Exception {
				ArrayList<Unit> res = new ArrayList<>();
				//only retain callers that are explicit call sites or Thread.start()
				Iterator<Edge> edgeIter = new Filter(icfg.getEdgePredicate()).wrap(icfg.getCallGraph().edgesInto(m));					
				while(edgeIter.hasNext())
					res.add(edgeIter.next().srcUnit());
				if(res.isEmpty())
					return Collections.emptyList();
				res.trimToSize();
				return Collections.unmodifiableList(res);
			}
		});
	}
	
	//Only callee methods that are not excluded and that have bodies
	protected static LoadingCache<Unit,Collection<SootMethod>> getNewUnitToCalleesCache(final IJimpleICFG icfg) {
		if(icfg == null)
			return null;
		return IDESolver.DEFAULT_CACHE_BUILDER.build( new CacheLoader<Unit,Collection<SootMethod>>() {
			@Override
			public Collection<SootMethod> load(Unit u) throws Exception {
				ArrayList<SootMethod> res = null;
				IBasicEdgePredicate edgePred = icfg.getEdgePredicate();
				//only retain callers that are explicit call sites or Thread.start()
				Iterator<Edge> edgeIter = new Filter(edgePred).wrap(icfg.getCallGraph().edgesOutOf(u));					
				while(edgeIter.hasNext()) {
					Edge edge = edgeIter.next();
					SootMethod m = edge.getTgt().method();
					if(m.hasActiveBody() && !edgePred.getExcludeHandler().isExcludedMethodWithOverride(m)) {
						if (res == null)
							res = new ArrayList<>();
						res.add(m);
					}
				}
				if(res == null)
					return Collections.emptySet();
				res.trimToSize();
				return Collections.unmodifiableList(res);
			}
		});
	}
	
	//All callee methods even if they are excluded or have no body
	protected static LoadingCache<Unit,Collection<SootMethod>> getNewUnitToAllCalleesCache(final IJimpleICFG icfg) {
		if(icfg == null)
			return null;
		return IDESolver.DEFAULT_CACHE_BUILDER.build( new CacheLoader<Unit,Collection<SootMethod>>() {
			@Override
			public Collection<SootMethod> load(Unit u) throws Exception {
				ArrayList<SootMethod> res = new ArrayList<>();
				//only retain callers that are explicit call sites or Thread.start()
				Iterator<Edge> edgeIter = new Filter(icfg.getEdgePredicate()).wrap(icfg.getCallGraph().edgesOutOf(u));					
				while(edgeIter.hasNext())
					res.add(edgeIter.next().getTgt().method());
				if(res.isEmpty())
					return Collections.emptySet();
				res.trimToSize();
				return Collections.unmodifiableList(res);
			}
		});
	}
	
	protected static LoadingCache<SootMethod,Map<SootField,Set<DefinitionStmt>>> getNewMethodToFieldReads() {
		return IDESolver.DEFAULT_CACHE_BUILDER.build( new CacheLoader<SootMethod,Map<SootField,Set<DefinitionStmt>>>() {
			@Override
			public Map<SootField,Set<DefinitionStmt>> load(SootMethod m) throws Exception {
				Map<SootField,Set<DefinitionStmt>> ret = new HashMap<>();
				if(m.hasActiveBody()) {
					for(Unit u : m.getActiveBody().getUnits()) {
						if(u instanceof DefinitionStmt) {
							DefinitionStmt def = (DefinitionStmt)u;
							//VariableBox returned -> field ref is on left, RValueBox returned -> field res is on right
							if(def.containsFieldRef() && !(def.getFieldRefBox() instanceof VariableBox)) {
								SootField field = def.getFieldRef().getField();
								Set<DefinitionStmt> temp = ret.get(field);
								if(temp == null) {
									temp = new HashSet<>();
									ret.put(field, temp);
								}
								temp.add(def);
							}
						}
					}
					for(SootField f : ret.keySet()) {
						ret.put(f, Collections.unmodifiableSet(ret.get(f)));
					}
				}
				return ret.isEmpty() ? Collections.<SootField,Set<DefinitionStmt>>emptyMap() : 
					Collections.<SootField,Set<DefinitionStmt>>unmodifiableMap(ret);
			}
		});
	}
	
	protected static LoadingCache<SootMethod,Map<SootField,Set<DefinitionStmt>>> getNewMethodToAllFieldReads(final IJimpleICFG icfg) {
		if(icfg == null)
			return null;
		return IDESolver.DEFAULT_CACHE_BUILDER.build( new CacheLoader<SootMethod,Map<SootField,Set<DefinitionStmt>>>() {
			@Override
			public Map<SootField,Set<DefinitionStmt>> load(SootMethod m) throws Exception {
				Map<SootField,Set<DefinitionStmt>> ret = new HashMap<>();
				IBasicEdgePredicate edgePred = icfg.getEdgePredicate();
				if(m.hasActiveBody()) {
					Set<SootMethod> visited = new HashSet<>();
					Queue<SootMethod> toVisit = new ArrayDeque<>();
					toVisit.add(m);
					while(!toVisit.isEmpty()) {
						SootMethod cur = toVisit.poll();
						if(visited.add(cur) && !edgePred.getExcludeHandler().isExcludedMethodWithOverride(cur)) {
							Map<SootField,Set<DefinitionStmt>> localFieldReads = icfg.getFieldReadsForMethod(cur);
							for(SootField f : localFieldReads.keySet()) {
								Set<DefinitionStmt> temp = ret.get(f);
								if(temp == null) {
									temp = new HashSet<>();
									ret.put(f, temp);
								}
								temp.addAll(localFieldReads.get(f));
							}
							Iterator<Edge> it = new Filter(edgePred).wrap(icfg.getCallGraph().edgesOutOf(cur));
							while(it.hasNext())
								toVisit.add(it.next().tgt());
						}
					}
					for(SootField f : ret.keySet()) {
						ret.put(f, Collections.unmodifiableSet(ret.get(f)));
					}
				}
				return ret.isEmpty() ? Collections.<SootField,Set<DefinitionStmt>>emptyMap() : 
					Collections.<SootField,Set<DefinitionStmt>>unmodifiableMap(ret);
			}
		});
	}
	
	protected static LoadingCache<Unit,Map<SootField,Set<DefinitionStmt>>> getNewAllFieldReadsAtCache(final IJimpleICFG icfg) {
		if(icfg == null)
			return null;
		return IDESolver.DEFAULT_CACHE_BUILDER.build( new CacheLoader<Unit,Map<SootField,Set<DefinitionStmt>>>() {
			@Override
			public Map<SootField,Set<DefinitionStmt>> load(Unit start) throws Exception {
				return getAllFieldReads(icfg,start,false);
			}
		});
	}
	
	protected static LoadingCache<Unit,Map<SootField,Set<DefinitionStmt>>> getNewAllFieldReadsAfterCache(final IJimpleICFG icfg) {
		if(icfg == null)
			return null;
		return IDESolver.DEFAULT_CACHE_BUILDER.build( new CacheLoader<Unit,Map<SootField,Set<DefinitionStmt>>>() {
			@Override
			public Map<SootField,Set<DefinitionStmt>> load(Unit start) throws Exception {
				return getAllFieldReads(icfg,start,true);
			}
		});
	}
	
	protected static Map<SootField,Set<DefinitionStmt>> getAllFieldReads(IJimpleICFG icfg, Unit start, boolean forward) {
		Map<SootField,Set<DefinitionStmt>> ret = new HashMap<>();
		SootMethod startM = icfg.getMethodOf(start);
		UnitGraph g = icfg.getOrCreateUnitGraph(startM);
		//We only grab the field reads for the current method because for any method invoked 
		//we need to include all of its field reads in a interprocedural manner and we already 
		//have a method for that. Of course if this is backwards, we need all the reads for the 
		Map<SootField,Set<DefinitionStmt>> startReads = icfg.getFieldReadsForMethod(startM);
		Map<DefinitionStmt,SootField> allReadsRev = new HashMap<>();
		for(SootField f : startReads.keySet()) {
			for(DefinitionStmt def : startReads.get(f)) {
				allReadsRev.put(def, f);
			}
		}
		
		Queue<Unit> toVisit = new ArrayDeque<>();
		Set<Unit> visited = new HashSet<>();
		
		if(forward)
			toVisit.addAll(g.getSuccsOf(start));
		else
			toVisit.add(start);
		while(!toVisit.isEmpty()) {
			Unit cur = toVisit.poll();
			if(visited.add(cur)) {
				if(!forward && icfg.isStartPoint(cur)) {
					//Need to move up the callgraph when we reach the head of a method
					for(Unit callerStmt : icfg.getCallersOf(icfg.getMethodOf(cur))) {
						//Need the field reads for the method we moved up into
						Map<SootField,Set<DefinitionStmt>> reads = icfg.getFieldReadsForMethod(icfg.getMethodOf(callerStmt));
						for(SootField f : reads.keySet()) {
							for(DefinitionStmt def : reads.get(f)) {
								allReadsRev.put(def, f);
							}
						}
						//skip over the call site in the caller so we don't include all the field reads since these should be partial
						toVisit.addAll(icfg.getPredsOfCallAt(callerStmt));
					}
				}
				if(allReadsRev.keySet().contains(cur)) {
					SootField f = allReadsRev.get(cur);
					Set<DefinitionStmt> defs = ret.get(f);
					if(defs == null) {
						defs = new HashSet<>();
						ret.put(f, defs);
					}
					defs.add((DefinitionStmt)cur);
				}
				if(((Stmt)cur).containsInvokeExpr()) {
					for(SootMethod m : icfg.getCalleesOfCallAt(cur)) {
						Map<SootField,Set<DefinitionStmt>> otherMMap = icfg.getAllFieldReadsForMethod(m);
						for(SootField f : otherMMap.keySet()) {
							Set<DefinitionStmt> defs = ret.get(f);
							if(defs == null) {
								defs = new HashSet<>();
								ret.put(f, defs);
							}
							defs.addAll(otherMMap.get(f));
						}
					}
				}
				if(forward)
					toVisit.addAll(g.getSuccsOf(cur));
				else
					toVisit.addAll(g.getPredsOf(cur));	
			}
		}
		for(SootField f : ret.keySet()) {
			ret.put(f, Collections.unmodifiableSet(ret.get(f)));
		}
		return ret.isEmpty() ? Collections.<SootField,Set<DefinitionStmt>>emptyMap() : 
			Collections.<SootField,Set<DefinitionStmt>>unmodifiableMap(ret);
	}
	
	protected static LoadingCache<Unit,Map<Local,Set<DefinitionStmt>>> getNewUnitToDefsForUsedLocals(final IJimpleICFG icfg, 
			final LoadingCache<Body,AdvLocalDefs> advLocalDefs) {
		return IDESolver.DEFAULT_CACHE_BUILDER.build(new CacheLoader<Unit,Map<Local,Set<DefinitionStmt>>>() {
			@Override
			public Map<Local,Set<DefinitionStmt>> load(Unit u) throws Exception {
				//Contains all values and the values inside those values so it should contain all locals
				//If an array is being assigned a value (i.e. on the left) then its local and its index local are included in the uses
				List<ValueBox> useBoxes = u.getUseBoxes();
				SootMethod source = icfg.getMethodOf(u);
				if(!useBoxes.isEmpty() && source != null) {
					AdvLocalDefs df = getOrMakeLocalDefs(advLocalDefs, null, source);
					Map<Local,Set<DefinitionStmt>> defs = new HashMap<>();
					for (ValueBox vb : useBoxes) {
						Value v = vb.getValue();
						if (v instanceof Local) {
							Set<DefinitionStmt> temp = df.getDefsWithAliases((Local)v,u);
							if(temp.isEmpty())
								defs.put((Local)v, Collections.<DefinitionStmt>emptySet());
							else
								defs.put((Local)v, temp);
						}
					}
					return defs.isEmpty() ? Collections.<Local,Set<DefinitionStmt>>emptyMap() : defs;
				} else {
					return Collections.emptyMap();
				}
			}
		});
	}
	
	protected static LoadingCache<Body,Set<Loop>> getNewBodyToLoopsCache() {
		return IDESolver.DEFAULT_CACHE_BUILDER.build(new CacheLoader<Body,Set<Loop>>() {
			@Override
			public Set<Loop> load(Body body) throws Exception {
				return Collections.unmodifiableSet(new LoopFinder(body).getLoops());
			}
		});
	}
	
	@Override
	public List<EntryPoint> getEntryPoints() {
		return eps;
	}
	
	@Override
	public IBasicEdgePredicate getEdgePredicate() {
		return pred;
	}
	
	@Override
	public CallGraph getCallGraph() {
		return cg;
	}
	
	@Override public SootMethod getMethodOf(Unit u) { return getMethodOf(unitToOwner, u); }
	
	protected static final SootMethod getMethodOf(Map<Unit, Body> unitToOwner, Unit u) {
		Body b = unitToOwner.get(u);
		return b == null ? null : b.getMethod();
	}

	@Override public List<Unit> getSuccsOf(Unit u) { return getSuccsOf(unitToOwner, bodyToUnitGraph, pred, u); }
	
	protected static final List<Unit> getSuccsOf(Map<Unit, Body> unitToOwner, LoadingCache<Body, UnitGraph> bodyToUnitGraph, IBasicEdgePredicate pred, Unit u) {
		Body body = unitToOwner.get(u);
		if (body == null)
			return Collections.emptyList();
		DirectedGraph<Unit> unitGraph = getOrCreateUnitGraph(bodyToUnitGraph, pred, body);
		return unitGraph.getSuccsOf(u);
	}

	@Override public UnitGraph getOrCreateUnitGraph(SootMethod m) { return getOrCreateUnitGraph(bodyToUnitGraph, pred, m); }
	
	protected static final UnitGraph getOrCreateUnitGraph(LoadingCache<Body, UnitGraph> bodyToUnitGraph, IBasicEdgePredicate pred, SootMethod m) {
		return m.hasActiveBody() ? getOrCreateUnitGraph(bodyToUnitGraph, pred, m.getActiveBody()) : null;
	}

	@Override public UnitGraph getOrCreateUnitGraph(Body body) { return getOrCreateUnitGraph(bodyToUnitGraph, pred, body); }
	
	protected static final UnitGraph getOrCreateUnitGraph(LoadingCache<Body, UnitGraph> bodyToUnitGraph, IBasicEdgePredicate pred, Body body) {
		return (pred == null || !pred.getExcludeHandler().isExcludedMethodWithOverride(body.getMethod())) ? bodyToUnitGraph.getUnchecked(body) : null;
	}
	
	@Override public boolean isExitStmt(Unit u) { return isExitStmt(unitToOwner, bodyToUnitGraph, pred, u); }
	
	protected static final boolean isExitStmt(Map<Unit, Body> unitToOwner, LoadingCache<Body, UnitGraph> bodyToUnitGraph, IBasicEdgePredicate pred, Unit u) {
		Body body = unitToOwner.get(u);
		if(body == null) return false;
		DirectedGraph<Unit> unitGraph = getOrCreateUnitGraph(bodyToUnitGraph, pred, body);
		return unitGraph.getTails().contains(u);
	}

	@Override public boolean isStartPoint(Unit u) { return isStartPoint(unitToOwner, bodyToUnitGraph, pred, u); }
	
	protected static final boolean isStartPoint(Map<Unit, Body> unitToOwner, LoadingCache<Body, UnitGraph> bodyToUnitGraph, IBasicEdgePredicate pred, Unit u) {
		Body body = unitToOwner.get(u);
		if(body == null) return false;
		DirectedGraph<Unit> unitGraph = getOrCreateUnitGraph(bodyToUnitGraph, pred, body);		
		return unitGraph.getHeads().contains(u);
	}

	@Override public boolean isFallThroughSuccessor(Unit u, Unit succ) { return isFallThroughSuccessor(unitToOwner, u, succ); }
	
	protected static final boolean isFallThroughSuccessor(Map<Unit, Body> unitToOwner, Unit u, Unit succ) {
		if(!u.fallsThrough()) return false;
		Body body = unitToOwner.get(u);
		if(body == null) return false;
		return body.getUnits().getSuccOf(u) == succ;
	}

	@Override public boolean isBranchTarget(Unit u, Unit succ) {
		if(!u.branches()) return false;
		for (UnitBox ub : u.getUnitBoxes()) {
			if(ub.getUnit()==succ) return true;
		}
		return false;
	}

	@Override public List<Value> getParameterRefs(SootMethod m) { return getParameterRefs(methodToParameterRefs, m); }
	
	protected static final List<Value> getParameterRefs(LoadingCache<SootMethod, List<Value>> methodToParameterRefs, SootMethod m) {
		return methodToParameterRefs.getUnchecked(m);
	}
	
	@Override public Collection<Unit> getStartPointsOf(SootMethod m) { return getStartPointsOf(bodyToUnitGraph, pred, m); }
	
	protected static final Collection<Unit> getStartPointsOf(LoadingCache<Body, UnitGraph> bodyToUnitGraph, IBasicEdgePredicate pred, SootMethod m) {
		if(m.hasActiveBody() && (pred == null || !pred.getExcludeHandler().isExcludedMethodWithOverride(m))) {
			Body body = m.getActiveBody();
			DirectedGraph<Unit> unitGraph = getOrCreateUnitGraph(bodyToUnitGraph, pred, body);
			return unitGraph.getHeads();
		}
		return Collections.emptySet();
	}

	@Override public boolean isCallStmt(Unit u) { return isCallStmtS(u); }
	
	protected static final boolean isCallStmtS(Unit u) { return ((Stmt)u).containsInvokeExpr(); }

	@Override public Set<Unit> allNonCallStartNodes() { return allNonCallStartNodes(unitToOwner, bodyToUnitGraph, pred); }
	
	protected static final Set<Unit> allNonCallStartNodes(Map<Unit, Body> unitToOwner, LoadingCache<Body, UnitGraph> bodyToUnitGraph, IBasicEdgePredicate pred) {
		Set<Unit> res = new LinkedHashSet<Unit>(unitToOwner.keySet());
		for (Iterator<Unit> iter = res.iterator(); iter.hasNext();) {
			Unit u = iter.next();
			if(isStartPoint(unitToOwner, bodyToUnitGraph, pred, u) || isCallStmtS(u)) iter.remove();
		}
		return res;
	}
	
	@Override public Set<Unit> allNonCallEndNodes() { return allNonCallEndNodes(unitToOwner, bodyToUnitGraph, pred); }
	
	protected static final Set<Unit> allNonCallEndNodes(Map<Unit, Body> unitToOwner, LoadingCache<Body, UnitGraph> bodyToUnitGraph, IBasicEdgePredicate pred) {
		Set<Unit> res = new LinkedHashSet<Unit>(unitToOwner.keySet());
		for (Iterator<Unit> iter = res.iterator(); iter.hasNext();) {
			Unit u = iter.next();
			if(isExitStmt(unitToOwner, bodyToUnitGraph, pred, u) || isCallStmtS(u)) iter.remove();
		}
		return res;
	}

	@Override public Collection<Unit> getReturnSitesOfCallAt(Unit u) { return getReturnSitesOfCallAt(unitToOwner, bodyToUnitGraph, pred, u); }
	
	protected static final Collection<Unit> getReturnSitesOfCallAt(Map<Unit, Body> unitToOwner, LoadingCache<Body, UnitGraph> bodyToUnitGraph, 
			IBasicEdgePredicate pred, Unit u) { 
		return getSuccsOf(unitToOwner, bodyToUnitGraph, pred, u);
	}

	@Override public Set<Unit> getCallsFromWithin(SootMethod m) { return getCallsFromWithin(methodToCallsFromWithin, m); }
	
	protected static final Set<Unit> getCallsFromWithin(LoadingCache<SootMethod, Set<Unit>> methodToCallsFromWithin, SootMethod m) {
		return methodToCallsFromWithin.getUnchecked(m);		
	}
	
	@Override public List<Unit> getPredsOf(Unit u) { return getPredsOf(unitToOwner, bodyToUnitGraph, pred, u); }
	
	protected static final List<Unit> getPredsOf(Map<Unit, Body> unitToOwner, LoadingCache<Body, UnitGraph> bodyToUnitGraph, IBasicEdgePredicate pred, Unit u) {
		Body body = unitToOwner.get(u);
		if(body == null) return Collections.emptyList();
		DirectedGraph<Unit> unitGraph = getOrCreateUnitGraph(bodyToUnitGraph, pred, body);
		return unitGraph.getPredsOf(u);
	}

	@Override public Collection<Unit> getEndPointsOf(SootMethod m) { return getEndPointsOf(unitToOwner, bodyToUnitGraph, pred, m); }
	
	protected static final Collection<Unit> getEndPointsOf(Map<Unit, Body> unitToOwner, LoadingCache<Body, UnitGraph> bodyToUnitGraph, 
			IBasicEdgePredicate pred, SootMethod m) {
		if(m.hasActiveBody() && (pred == null || !pred.getExcludeHandler().isExcludedMethodWithOverride(m))) {
			Body body = m.getActiveBody();
			DirectedGraph<Unit> unitGraph = getOrCreateUnitGraph(bodyToUnitGraph, pred, body);
			return unitGraph.getTails();
		}
		return Collections.emptySet();
	}
	
	@Override
	public Set<Unit> getAllEndPointsOfCalleesOfCallAt(Unit invoke) {
		return getAllEndPointsOfCalleesOfCallAt(unitToOwner, bodyToUnitGraph, pred, unitToCallees, invoke);
	}
	
	protected static final Set<Unit> getAllEndPointsOfCalleesOfCallAt(Map<Unit, Body> unitToOwner, LoadingCache<Body, UnitGraph> bodyToUnitGraph, 
			IBasicEdgePredicate pred, LoadingCache<Unit, Collection<SootMethod>> unitToCallees, Unit invoke) {
		Set<Unit> ret = new HashSet<>();
		for (SootMethod sm: getCalleesOfCallAt(unitToCallees, invoke)) {
			ret.addAll(getEndPointsOf(unitToOwner, bodyToUnitGraph, pred, sm));
		}
		return ret.isEmpty() ? Collections.<Unit>emptySet() : ret;
	}
	
	@Override public List<Unit> getPredsOfCallAt(Unit u) { return getPredsOfCallAt(unitToOwner, bodyToUnitGraph, pred, u); }
	
	protected static final List<Unit> getPredsOfCallAt(Map<Unit, Body> unitToOwner, LoadingCache<Body, UnitGraph> bodyToUnitGraph, 
			IBasicEdgePredicate pred, Unit u) {
		return getPredsOf(unitToOwner, bodyToUnitGraph, pred, u);
	}
	
	@Override public boolean isReturnSite(Unit n) { return isReturnSite(unitToOwner, bodyToUnitGraph, pred, n); }
	
	protected static final boolean isReturnSite(Map<Unit, Body> unitToOwner, LoadingCache<Body, UnitGraph> bodyToUnitGraph, 
			IBasicEdgePredicate pred, Unit n) {
		for (Unit p : getPredsOf(unitToOwner, bodyToUnitGraph, pred, n))
			if (isCallStmtS(p))
				return true;
		return false;
	}
	
	@Override public boolean isReachable(Unit u) { return isReachable(unitToOwner, u); }
	
	protected static final boolean isReachable(Map<Unit, Body> unitToOwner, Unit u) { return unitToOwner.containsKey(u); }
	
	@Override
	public ControlDependenceGraph<Unit> getOrMakeControlDependenceGraph(SootMethod m) { return getOrMakeControlDependenceGraph(controlDependenceGraphs, pred, m); }
	
	protected static final ControlDependenceGraph<Unit> getOrMakeControlDependenceGraph(LoadingCache<Body, ControlDependenceGraph<Unit>> controlDependenceGraphs, 
			IBasicEdgePredicate pred, SootMethod m) {
		return m.hasActiveBody() ? getOrMakeControlDependenceGraph(controlDependenceGraphs, pred, m.getActiveBody()) : null;
	}
	
	@Override
	public ControlDependenceGraph<Unit> getOrMakeControlDependenceGraph(Body b) { return getOrMakeControlDependenceGraph(controlDependenceGraphs, pred, b); }
	
	protected static final ControlDependenceGraph<Unit> getOrMakeControlDependenceGraph(LoadingCache<Body, ControlDependenceGraph<Unit>> controlDependenceGraphs, 
			IBasicEdgePredicate pred, Body b) {
		return (pred == null || !pred.getExcludeHandler().isExcludedMethodWithOverride(b.getMethod())) ? controlDependenceGraphs.getUnchecked(b) : null;
	}
	
	@Override public AdvLocalDefs getOrMakeLocalDefs(SootMethod m) { return getOrMakeLocalDefs(advLocalDefs, pred, m); }
	
	protected static final AdvLocalDefs getOrMakeLocalDefs(LoadingCache<Body, AdvLocalDefs> advLocalDefs, IBasicEdgePredicate pred, SootMethod m) {
		return m.hasActiveBody() ? getOrMakeLocalDefs(advLocalDefs, pred, m.getActiveBody()) : null;
	}
	
	@Override public AdvLocalDefs getOrMakeLocalDefs(Body b) { return getOrMakeLocalDefs(advLocalDefs, pred, b); }
	
	protected static final AdvLocalDefs getOrMakeLocalDefs(LoadingCache<Body, AdvLocalDefs> advLocalDefs, IBasicEdgePredicate pred, Body b) {
		return (pred == null || !pred.getExcludeHandler().isExcludedMethodWithOverride(b.getMethod())) ? advLocalDefs.getUnchecked(b) : null;
	}
	
	@Override public AdvLocalUses getOrMakeLocalUses(SootMethod m) { return getOrMakeLocalUses(advLocalUses, pred, m); }
	
	protected static final AdvLocalUses getOrMakeLocalUses(LoadingCache<Body, AdvLocalUses> advLocalUses, IBasicEdgePredicate pred, SootMethod m) {
		return m.hasActiveBody() ? getOrMakeLocalUses(advLocalUses, pred, m.getActiveBody()) : null;
	}
	
	@Override public AdvLocalUses getOrMakeLocalUses(Body b) { return getOrMakeLocalUses(advLocalUses, pred, b); }
	
	protected static final AdvLocalUses getOrMakeLocalUses(LoadingCache<Body, AdvLocalUses> advLocalUses, IBasicEdgePredicate pred, Body b) {
		return (pred == null || !pred.getExcludeHandler().isExcludedMethodWithOverride(b.getMethod())) ? advLocalUses.getUnchecked(b) : null ;
	}
	
	/** Returns the methods called from a unit but only those that have
	 * a body. This appears to be a limitation imposed because {@link IDESolver} fails
	 * to check if the methods called from a unit actually have a body and it appears
	 * to assume that all the methods returned from this call have a body. This could
	 * probably be fixed by simply checking is {@link #getStartPointsOf} returns an
	 * empty set before attempting to perform any of the call related flow functions.
	 */
	@Override public Collection<SootMethod> getCalleesOfCallAt(Unit n) { return getCalleesOfCallAt(unitToCallees, n); }
	
	protected static final Collection<SootMethod> getCalleesOfCallAt(LoadingCache<Unit, Collection<SootMethod>> unitToCallees, Unit n) {
		return unitToCallees.getUnchecked(n);
	}
	
	/** This is the same as {@link #getCalleesOfCallAt(Unit)} except it 
	 * returns all methods called from a unit even if the method does
	 * not have a body.
	 */
	@Override public Collection<SootMethod> getAllCalleesOfCallAt(Unit n) { return getAllCalleesOfCallAt(unitToAllCallees, n); }
	
	protected static final Collection<SootMethod> getAllCalleesOfCallAt(LoadingCache<Unit, Collection<SootMethod>> unitToAllCallees, Unit n) {
		return unitToAllCallees.getUnchecked(n);
	}

	@Override public Collection<Unit> getCallersOf(SootMethod m) { return getCallersOf(methodToCallers, pred, m); }
	
	protected static final Collection<Unit> getCallersOf(LoadingCache<SootMethod, Collection<Unit>> methodToCallers, IBasicEdgePredicate pred, SootMethod m) {
		return (m.hasActiveBody() && (pred == null || !pred.getExcludeHandler().isExcludedMethodWithOverride(m))) ? methodToCallers.getUnchecked(m) : null;
	}
	
	@Override
	public List<IdentityStmt> getParameterDefs(SootMethod m) {
		return getParameterDefs(methodToParameterDefs, pred, m);
	}
	
	protected static final List<IdentityStmt> getParameterDefs(LoadingCache<SootMethod, List<IdentityStmt>> methodToParameterDefs, IBasicEdgePredicate pred, 
			SootMethod m) {
		return (m.hasActiveBody() && (pred == null || !pred.getExcludeHandler().isExcludedMethodWithOverride(m))) ? methodToParameterDefs.getUnchecked(m) : null;
	}
	
	@Override public Map<SootField,Set<DefinitionStmt>> getFieldReadsForMethod(SootMethod m) { return getFieldReadsForMethod(methodToFieldReads, pred, m); }
	
	protected static final Map<SootField,Set<DefinitionStmt>> getFieldReadsForMethod(
			LoadingCache<SootMethod, Map<SootField, Set<DefinitionStmt>>> methodToFieldReads, IBasicEdgePredicate pred, SootMethod m) {
		return (m.hasActiveBody() && (pred == null || !pred.getExcludeHandler().isExcludedMethodWithOverride(m))) ? methodToFieldReads.getUnchecked(m) : null;
	}
	
	@Override public Set<DefinitionStmt> getFieldReadsForMethod(SootMethod m, SootField f) { return getFieldReadsForMethod(methodToFieldReads, pred, m, f); }
	
	protected static final Set<DefinitionStmt> getFieldReadsForMethod(LoadingCache<SootMethod, Map<SootField, Set<DefinitionStmt>>> methodToFieldReads, 
			IBasicEdgePredicate pred, SootMethod m, SootField f) {
		Set<DefinitionStmt> ret = getFieldReadsForMethod(methodToFieldReads, pred, m).get(f);
		return ret == null ? Collections.<DefinitionStmt>emptySet() : ret;
	}
	
	@Override
	public Map<SootField,Set<DefinitionStmt>> getAllFieldReadsForMethod(SootMethod m) { return getAllFieldReadsForMethod(methodToAllFieldReads, pred, m); }
	
	protected static final Map<SootField,Set<DefinitionStmt>> getAllFieldReadsForMethod(
			LoadingCache<SootMethod, Map<SootField, Set<DefinitionStmt>>> methodToAllFieldReads, IBasicEdgePredicate pred, SootMethod m) {
		return (m.hasActiveBody() && (pred == null || !pred.getExcludeHandler().isExcludedMethodWithOverride(m))) ? methodToAllFieldReads.getUnchecked(m) : null;
	}
	
	@Override
	public Set<DefinitionStmt> getAllFieldReadsForMethod(SootMethod m, SootField f) { return getAllFieldReadsForMethod(methodToAllFieldReads, pred, m, f); }
	
	protected static final Set<DefinitionStmt> getAllFieldReadsForMethod(
			LoadingCache<SootMethod, Map<SootField, Set<DefinitionStmt>>> methodToAllFieldReads, IBasicEdgePredicate pred, SootMethod m, SootField f) {
		Set<DefinitionStmt> ret = getAllFieldReadsForMethod(methodToAllFieldReads, pred, m).get(f);
		return ret == null ? Collections.<DefinitionStmt>emptySet() : ret;
	}
	
	@Override public Map<SootField,Set<DefinitionStmt>> getAllFieldReadsAt(Unit start) { return getAllFieldReadsAt(allFieldReadsAtCache, start); }
	
	protected static final Map<SootField,Set<DefinitionStmt>> getAllFieldReadsAt(LoadingCache<Unit, Map<SootField, Set<DefinitionStmt>>> allFieldReadsAtCache, 
			Unit start) {
		return allFieldReadsAtCache.getUnchecked(start);
	}
	
	@Override public Set<DefinitionStmt> getAllFieldReadsAt(Unit start, SootField f) { return getAllFieldReadsAt(allFieldReadsAtCache, start, f); }
	
	protected static final Set<DefinitionStmt> getAllFieldReadsAt(LoadingCache<Unit, Map<SootField, Set<DefinitionStmt>>> allFieldReadsAtCache, 
			Unit start, SootField f) {
		Set<DefinitionStmt> set = getAllFieldReadsAt(allFieldReadsAtCache, start).get(f);
		return set == null ? Collections.<DefinitionStmt>emptySet() : set;
	}
	
	@Override public Map<SootField,Set<DefinitionStmt>> getAllFieldReadsAfter(Unit start) { return getAllFieldReadsAfter(allFieldReadsAfterCache, start); }
	
	protected static final Map<SootField,Set<DefinitionStmt>> getAllFieldReadsAfter(
			LoadingCache<Unit, Map<SootField, Set<DefinitionStmt>>> allFieldReadsAfterCache, Unit start) {
		return allFieldReadsAfterCache.getUnchecked(start);
	}
	
	@Override public Set<DefinitionStmt> getAllFieldReadsAfter(Unit start, SootField f) { return getAllFieldReadsAfter(allFieldReadsAfterCache, start, f); }
	
	protected static final Set<DefinitionStmt> getAllFieldReadsAfter(LoadingCache<Unit, Map<SootField, Set<DefinitionStmt>>> allFieldReadsAfterCache, 
			Unit start, SootField f) {
		Set<DefinitionStmt> set = getAllFieldReadsAfter(allFieldReadsAfterCache, start).get(f);
		return set == null ? Collections.<DefinitionStmt>emptySet() : set;
	}
	
	@Override public Set<DefinitionStmt> getDefsForUsedLocals(Unit u) { return getDefsForUsedLocals(unitToOwner, unitToDefsForUsedLocals, u); }
	
	protected static final Set<DefinitionStmt> getDefsForUsedLocals(Map<Unit, Body> unitToOwner, 
			LoadingCache<Unit, Map<Local, Set<DefinitionStmt>>> unitToDefsForUsedLocals, Unit u) {
		if(unitToOwner.containsKey(u)) {
			Map<Local,Set<DefinitionStmt>> map = unitToDefsForUsedLocals.getUnchecked(u);
			Set<DefinitionStmt> ret = new HashSet<>();
			for(Set<DefinitionStmt> defs : map.values())
				ret.addAll(defs);
			return ret.isEmpty() ? Collections.<DefinitionStmt>emptySet() : ret;
		}
		return Collections.emptySet();
	}
	
	@Override public Map<Local,Set<DefinitionStmt>> getDefsForUsedLocalsMap(Unit u) { return getDefsForUsedLocalsMap(unitToOwner, unitToDefsForUsedLocals, u); }
	
	protected static final Map<Local,Set<DefinitionStmt>> getDefsForUsedLocalsMap(Map<Unit, Body> unitToOwner, 
			LoadingCache<Unit, Map<Local, Set<DefinitionStmt>>> unitToDefsForUsedLocals, Unit u) {
		if(unitToOwner.containsKey(u)) {
			return unitToDefsForUsedLocals.getUnchecked(u);
		}
		return Collections.emptyMap();
	}
	
	@Override public Set<Loop> getLoops(SootMethod source) { return getLoops(bodyToLoopsCache, pred, source); }
	
	protected static final Set<Loop> getLoops(LoadingCache<Body, Set<Loop>> bodyToLoopsCache, IBasicEdgePredicate pred, SootMethod source) {
		return source.hasActiveBody()  ? getLoops(bodyToLoopsCache, pred, source.getActiveBody()) : null;
	}
	
	@Override public Set<Loop> getLoops(Body body) { return getLoops(bodyToLoopsCache, pred, body); }
	
	protected static final Set<Loop> getLoops(LoadingCache<Body, Set<Loop>> bodyToLoopsCache, IBasicEdgePredicate pred, Body body) {
		return (body != null && (pred == null || !pred.getExcludeHandler().isExcludedMethodWithOverride(body.getMethod()))) ? 
				bodyToLoopsCache.getUnchecked(body) : null;
	}
	
	@Override public SwitchWrapper getOrMakeSwitchWrapper(SwitchStmt stmt) { return getOrMakeSwitchWrapper(switchStmtToSwitchWrapperCache, stmt); }
	
	protected static final SwitchWrapper getOrMakeSwitchWrapper(LoadingCache<SwitchStmt, SwitchWrapper> stmtToWrapper, SwitchStmt stmt) {
		return stmtToWrapper.getUnchecked(stmt);
	}

	public static class BasicEdgePredicate implements IBasicEdgePredicate {
		protected final boolean includeReflectiveCalls;
		protected final IExcludeHandler excludeHandler;
		public BasicEdgePredicate() {
			this(false);
		}
		public BasicEdgePredicate(boolean includeReflectiveCalls) {
			this(includeReflectiveCalls,new EmptyExcludeHandler());
		}
		protected BasicEdgePredicate(boolean includeReflectiveCalls, IExcludeHandler excludeHandler) {
			this.includeReflectiveCalls = includeReflectiveCalls;
			this.excludeHandler = excludeHandler;
		}
		@Override
		public boolean want(Edge e) {
			return e.kind().isExplicit() || e.kind().isThread() || e.kind().isExecutor()
					|| e.kind().isAsyncTask() || e.kind().isClinit() || e.kind().isPrivileged()
					|| (includeReflectiveCalls && e.kind().isReflection());
		}
		public IExcludeHandler getExcludeHandler() {
			return excludeHandler;
		}
	}
	
	private static final class EmptyExcludeHandler implements IExcludeHandler {
		@Override public boolean isExcludedMethod(SootMethod m) {return false;}
		@Override public boolean isExcludedMethodWithOverride(SootMethod m) {return false;}
		@Override public boolean isExcludedClass(SootClass sc) {return false;}
		@Override public Set<SootMethod> getExcludedMethods() {return Collections.emptySet();}
		@Override public Set<SootMethod> getExcludedOverrideMethods() {return Collections.emptySet();}
		@Override public Set<SootMethod> getExcludedMethodsWithOverride() {return Collections.emptySet();}
		@Override public Set<SootClass> getExcludedClasses() {return Collections.emptySet();}
		@Override public SootMethod getEntryPoint() {return null;}
		@Override public SootClass getStub() {return null;}
		
	}

}
