package org.sag.acminer.phases.accesscontrolmarker;

import heros.solver.IDESolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sag.acminer.database.entrypointedges.IEntryPointEdgesDatabase;
import org.sag.acminer.database.excludedelements.IExcludeHandler;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.concurrent.IgnorableRuntimeException;
import org.sag.common.concurrent.ValueRunner;
import org.sag.common.concurrent.ValueWorkerFactory;
import org.sag.common.concurrent.ValueWorkerGroup;
import org.sag.common.concurrent.WorkerCountingThreadExecutor;
import org.sag.common.logging.ILogger;
import org.sag.common.tools.SortingMethods;
import org.sag.common.tuple.Pair;
import org.sag.common.tuple.Triple;
import org.sag.soot.SootSort;
import org.sag.soot.callgraph.JimpleICFG.BasicEdgePredicate;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.ThrowStmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.Filter;
import soot.toolkits.graph.BriefUnitGraph;

public class SimpleFinder {

	private final String name;
	private final WorkerCountingThreadExecutor exe;
	private final CallGraph cg;
	private final Set<SootMethod> contextQueries;
	private final BasicEdgePredicate edgePredicate;
	
	private final LoadingCache<SootMethod,Set<Unit>> methodToSecurityExceptions = 
			IDESolver.DEFAULT_CACHE_BUILDER.build(new CacheLoader<SootMethod,Set<Unit>>() {
				@Override
				public Set<Unit> load(SootMethod source) throws Exception {
					if(source != null && source.isConcrete()) {
						Set<Unit> ret = new HashSet<>();
						List<Unit> endPoints = new BriefUnitGraph(source.retrieveActiveBody()).getTails();
						for(Unit u : endPoints){
							if(u instanceof ThrowStmt){
								ThrowStmt temp = (ThrowStmt)u;
								if(temp.getOp().getType().toString().equals("java.lang.SecurityException")){
									ret.add(temp);
								}
							}
						}
						if(ret.isEmpty())
							return Collections.emptySet();
						return ret;
					}
					return Collections.emptySet();
				}
			});
	
	private final LoadingCache<SootMethod,Set<Unit>> methodToContextQueries = 
			IDESolver.DEFAULT_CACHE_BUILDER.build(new CacheLoader<SootMethod,Set<Unit>>() {
				@Override
				public Set<Unit> load(SootMethod source) throws Exception {
					if(source != null && source.isConcrete()) {
						Set<Unit> ret = new HashSet<Unit>();
						Iterator<Edge> outE = new Filter(edgePredicate).wrap(cg.edgesOutOf(source));
						while(outE.hasNext()){
							Edge e = outE.next();
							if(contextQueries.contains(e.tgt())){
								ret.add(e.srcUnit());
							}
						}
						if(ret.isEmpty())
							return Collections.emptySet();
						return ret;
					}
					return Collections.emptySet();
				}
			});
	
	public SimpleFinder(Set<SootMethod> contextQueries) {
		this.name = getClass().getSimpleName();
		this.exe = new WorkerCountingThreadExecutor(new ValueWorkerFactory<>());
		this.cg = Scene.v().getCallGraph();
		this.contextQueries = contextQueries;
		this.edgePredicate = new BasicEdgePredicate();
	}
	
	public boolean shutdownWhenFinished() {
		return exe.shutdownWhenFinished();
	}
	
	public List<Throwable> getAndClearExceptions() {
		return exe.getAndClearExceptions();
	}
	
	public Triple<Map<SootMethod, Pair<Set<Unit>,Set<Integer>>>,Map<SootMethod, Pair<Set<Unit>,Set<Integer>>>,
			Map<EntryPoint,Pair<IEntryPointEdgesDatabase.Type,Map<SootMethod,Pair<Set<Unit>,Set<Integer>>>>>> findData(EntryPoint ep, 
			IExcludeHandler excludeHandler, Map<SootMethod,Set<EntryPoint>> allEntryPointsForStub, Map<SootMethod,Set<EntryPoint>> allEntryPointsNotForStub, 
			ILogger logger) {
		try {
			SimpleFinderGroup group = new SimpleFinderGroup(name,ep.toString(),logger);
			Set<SootMethod> seen = Collections.<SootMethod>synchronizedSet(new HashSet<SootMethod>());
			executeRunner(new SimpleFinderRunner(ep.getEntryPoint(), 0, seen, excludeHandler, allEntryPointsForStub, allEntryPointsNotForStub, logger),
					group,logger);
			group.unlockInitialLock();
			GroupVal val = group.getReturnValue();
			if(group.shutdownNormally() && !group.hasExceptions()) {
				logger.fineInfo("{}: Successfully completed data finding for group '{}'.",name,group.getName());
			} else {
				logger.fatal("{}: Failed to complete data finding for group '{}'.",name,group.getName());
				throw new IgnorableRuntimeException();
			}
			
			Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> retCQUnits;
			if(val.sourceToCQUnits.isEmpty()) {
				retCQUnits = Collections.emptyMap();
			} else {
				retCQUnits = new HashMap<>();
				for(SootMethod sm : val.sourceToCQUnits.keySet()) {
					retCQUnits.put(sm, new Pair<Set<Unit>,Set<Integer>>(SortingMethods.sortSet(val.sourceToCQUnits.get(sm),SootSort.unitComp),
							SortingMethods.sortSet(val.methodToDepth.get(sm))));
				}
				retCQUnits = SortingMethods.sortMapKey(retCQUnits, SootSort.smComp);
			}
			
			Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> retSEUnits;
			if(val.sourceToSEUnits.isEmpty()) {
				retSEUnits = Collections.emptyMap();
			} else {
				retSEUnits = new HashMap<>();
				for(SootMethod sm : val.sourceToSEUnits.keySet()) {
					retSEUnits.put(sm, new Pair<Set<Unit>,Set<Integer>>(SortingMethods.sortSet(val.sourceToSEUnits.get(sm),SootSort.unitComp),
							SortingMethods.sortSet(val.methodToDepth.get(sm))));
				}
				retSEUnits = SortingMethods.sortMapKey(retSEUnits, SootSort.smComp);
			}
			
			//Map<I/EEntryPoint,Pair<I/EId,Map<SourceMethod,Pair<Set<InvokeStmt>,Set<Depth>>>>>
			Map<EntryPoint,Pair<IEntryPointEdgesDatabase.Type,Map<SootMethod,Pair<Set<Unit>,Set<Integer>>>>> retIEEntryPoints;
			if(val.ieEntryPointsToSources.isEmpty()) {
				retIEEntryPoints = Collections.emptyMap();
			} else {
				retIEEntryPoints = new HashMap<>();
				for(EntryPoint ieep : val.ieEntryPointsToSources.keySet()) {
					Pair<IEntryPointEdgesDatabase.Type,Map<SootMethod,Set<Unit>>> isExternalAndSources = val.ieEntryPointsToSources.get(ieep);
					Map<SootMethod,Pair<Set<Unit>,Set<Integer>>> sourcesToUnitsAndDepth = new HashMap<>();
					for(SootMethod source : isExternalAndSources.getSecond().keySet()) {
						sourcesToUnitsAndDepth.put(source, new Pair<>(SortingMethods.sortSet(isExternalAndSources.getSecond().get(source), SootSort.unitComp),
								SortingMethods.sortSet(val.methodToDepth.get(source))));
					}
					sourcesToUnitsAndDepth = SortingMethods.sortMapKey(sourcesToUnitsAndDepth, SootSort.smComp);
					retIEEntryPoints.put(ieep, new Pair<>(isExternalAndSources.getFirst(),sourcesToUnitsAndDepth));
				}
				retIEEntryPoints = SortingMethods.sortMapKeyAscending(retIEEntryPoints);
			}
			
			return new Triple<>(retCQUnits,retSEUnits,retIEEntryPoints);
			
		} catch(IgnorableRuntimeException t) {
			throw t;
		} catch(Throwable t) {
			logger.fatal("{}: An unexpected error occured for ep'{}'.",t,name,ep);
			throw new IgnorableRuntimeException();
		}
	}
	
	public Map<SootMethod, Set<SootMethod>> getContextQuerySubGraphs(EntryPoint ep, Set<Unit> data, IExcludeHandler excludeHandler, 
			ILogger logger) {
		try {
			Set<SootMethod> cqs = new HashSet<>();
			for(Unit u : data) {
				Iterator<Edge> outE = new Filter(edgePredicate).wrap(cg.edgesOutOf(u));
				while(outE.hasNext()) {
					Edge e = outE.next();
					SootMethod sm = e.tgt();
					if(contextQueries.contains(sm))
						cqs.add(sm);
				}
			}
			
			ContextQuerySubGraphGroup group = new ContextQuerySubGraphGroup(name,ep.toString(),logger);
			for(SootMethod cq : cqs) {
				Set<SootMethod> seen = Collections.<SootMethod>synchronizedSet(new HashSet<SootMethod>());
				executeRunner(new ContextQuerySubGraphRunner(cq, cq, seen, excludeHandler, logger), group, logger);
			}
			group.unlockInitialLock();
			
			Map<SootMethod,Set<SootMethod>> val = group.getReturnValue();
			if(group.shutdownNormally() && !group.hasExceptions()) {
				logger.fineInfo("{}: Successfully completed context query subgraphs for group '{}'.",name,group.getName());
			} else {
				logger.fatal("{}: Failed to complete context query subgraphs for group '{}'.",name,group.getName());
				throw new IgnorableRuntimeException();
			}
			
			if(val.isEmpty()) {
				return Collections.emptyMap();
			} else {
				Map<SootMethod, Set<SootMethod>> ret = new HashMap<>();
				for(SootMethod cq : val.keySet()) {
					ret.put(cq, SortingMethods.sortSet(val.get(cq),SootSort.smComp));
				}
				ret = SortingMethods.sortMapKey(ret, SootSort.smComp);
				return ret;
			}
			
		} catch(IgnorableRuntimeException t) {
			throw t;
		} catch(Throwable t) {
			logger.fatal("{}: An unexpected error occured for ep '{}'.",t,name,ep);
			throw new IgnorableRuntimeException();
		}
	}
	
	private synchronized <O> void executeRunner(ValueRunner<O> runner, ValueWorkerGroup<?,O> g, ILogger logger) {
		try {
			exe.execute(runner,g);
		} catch(Throwable t) {
			logger.fatal("{}: Failed to execute '{}' for group '{}'.",t,name,runner.toString(),g.getName());
			throw new IgnorableRuntimeException();
		}
	}
	
	private synchronized <O> void executeRunners(Iterable<ValueRunner<O>> runners, ILogger logger) {
		for(ValueRunner<O> runner : runners) {
			try {
				exe.execute(runner);
			} catch(Throwable t) {
				logger.fatal("{}: Failed to execute '{}'.",t,name,runner.toString());
				throw new IgnorableRuntimeException();
			}
		}
	}
	
	private static final class SimpleFinderGroup extends ValueWorkerGroup<GroupVal,RetVal> {
		public SimpleFinderGroup(String name, String ep, ILogger logger) {
			super(name,ep,logger,false,false);
		}
		@Override
		protected GroupVal initReturnValue() { return new GroupVal(); }

		@Override
		protected void joinWorkerReturnValue(RetVal v) {
			if(!v.cqUnits.isEmpty())
				ret.sourceToCQUnits.put(v.source, v.cqUnits);
			if(!v.seUnits.isEmpty())
				ret.sourceToSEUnits.put(v.source, v.seUnits);
			
			if(!v.internalEntryPoints.isEmpty()) {
				for(EntryPoint iep : v.internalEntryPoints.keySet()) {
					Set<Unit> units = v.internalEntryPoints.get(iep);
					Pair<IEntryPointEdgesDatabase.Type, Map<SootMethod, Set<Unit>>> isInternalAndSources = ret.ieEntryPointsToSources.get(iep);
					if(isInternalAndSources == null) {
						isInternalAndSources = new Pair<>(IEntryPointEdgesDatabase.Type.Internal, new HashMap<>());
						ret.ieEntryPointsToSources.put(iep, isInternalAndSources);
					}
					Map<SootMethod,Set<Unit>> sourceToUnits = isInternalAndSources.getSecond();
					Set<Unit> sourceUnits = sourceToUnits.get(v.source);
					if(sourceUnits == null) {
						sourceUnits = new HashSet<>();
						sourceToUnits.put(v.source, sourceUnits);
					}
					sourceUnits.addAll(units);
				}
			}
			
			if(!v.externalEntryPoints.isEmpty()) {
				for(EntryPoint eep : v.externalEntryPoints.keySet()) {
					Set<Unit> units = v.externalEntryPoints.get(eep);
					Pair<IEntryPointEdgesDatabase.Type, Map<SootMethod, Set<Unit>>> isExternalAndSources = ret.ieEntryPointsToSources.get(eep);
					if(isExternalAndSources == null) {
						isExternalAndSources = new Pair<>(IEntryPointEdgesDatabase.Type.External, new HashMap<>());
						ret.ieEntryPointsToSources.put(eep, isExternalAndSources);
					}
					Map<SootMethod,Set<Unit>> sourceToUnits = isExternalAndSources.getSecond();
					Set<Unit> sourceUnits = sourceToUnits.get(v.source);
					if(sourceUnits == null) {
						sourceUnits = new HashSet<>();
						sourceToUnits.put(v.source, sourceUnits);
					}
					sourceUnits.addAll(units);
				}
			}
			
			Set<Integer> depths = ret.methodToDepth.get(v.source);
			if(depths == null) {
				depths = new HashSet<>();
				ret.methodToDepth.put(v.source, depths);
			}
			depths.add(v.depth);
			for(SootMethod sm : v.moreUses) {
				Set<Integer> d = ret.methodToDepth.get(sm);
				if(d == null) {
					d = new HashSet<>();
					ret.methodToDepth.put(sm, d);
				}
				d.add(v.depth+1);
			}
		}

		@Override
		protected void finalizeReturnValue() {}
	}
	
	private static final class GroupVal {
		public final Map<SootMethod,Set<Unit>> sourceToCQUnits;
		public final Map<SootMethod,Set<Unit>> sourceToSEUnits;
		//Map<I/EEntryPoint,Pair<I/EId,Map<SourceMethod,Set<InvokeStmt>>>> 
		public final Map<EntryPoint,Pair<IEntryPointEdgesDatabase.Type,Map<SootMethod,Set<Unit>>>> ieEntryPointsToSources; 
		public final Map<SootMethod,Set<Integer>> methodToDepth;
		public GroupVal() {
			sourceToCQUnits = new HashMap<>();
			sourceToSEUnits = new HashMap<>();
			methodToDepth = new HashMap<>();
			ieEntryPointsToSources = new HashMap<>();
		}
	}
	
	private static final class RetVal {
		public final SootMethod source;
		public final int depth;
		public volatile Set<Unit> cqUnits;
		public volatile Set<Unit> seUnits;
		public volatile Map<EntryPoint,Set<Unit>> internalEntryPoints;
		public volatile Map<EntryPoint,Set<Unit>> externalEntryPoints;
		public volatile Set<SootMethod> moreUses;
		public RetVal(SootMethod source, int depth) {
			this.source = source;
			this.depth = depth;
			this.cqUnits = Collections.emptySet();
			this.seUnits = Collections.emptySet();
			this.moreUses = Collections.emptySet();
			this.internalEntryPoints = Collections.emptyMap();
			this.externalEntryPoints = Collections.emptyMap();
		}
	}
	
	private final class SimpleFinderRunner implements ValueRunner<RetVal> {

		private final RetVal data;
		private final Set<SootMethod> seen;
		private final IExcludeHandler excludeHandler;
		private final ILogger logger;
		private final Map<SootMethod,Set<EntryPoint>> allEntryPointsForStub;
		private final Map<SootMethod,Set<EntryPoint>> allEntryPointsNotForStub;
		
		public SimpleFinderRunner(SootMethod cur, int depth, Set<SootMethod> seen, IExcludeHandler excludeHandler, 
				Map<SootMethod,Set<EntryPoint>> allEntryPointsForStub, 
				Map<SootMethod,Set<EntryPoint>> allEntryPointsNotForStub, ILogger logger) {
			this.data = new RetVal(cur,depth);
			this.logger = logger;
			this.seen = seen;
			this.excludeHandler = excludeHandler;
			this.allEntryPointsForStub = allEntryPointsForStub;
			this.allEntryPointsNotForStub = allEntryPointsNotForStub;
		}
		
		@Override
		public void run() {
			List<ValueRunner<RetVal>> runners = new ArrayList<>();
			// This data is not specific to an entry point so we can cache it
			// i.e. a method will always have the same context queries and the same exceptions
			// so if a method is reachable then it will be the same data for that method
			data.cqUnits = methodToContextQueries.getUnchecked(data.source);
			data.seUnits = methodToSecurityExceptions.getUnchecked(data.source);
			Iterator<Edge> itEdge = new Filter(edgePredicate).wrap(cg.edgesOutOf(data.source));
			while(itEdge.hasNext()) {
				Edge e = itEdge.next();
				SootMethod sm = e.tgt();
				
				//A SootMethod that is identified as an entry point could have multiple stubs and therefore 
				//multiple EntryPoint objects, like in the case of the common binder methods.
				//So the target method must be checked against all possible EntryPoint objects.
				if(allEntryPointsForStub.containsKey(sm)) {
					Map<EntryPoint,Set<Unit>> ieEntryPoints = data.internalEntryPoints = 
							data.internalEntryPoints.isEmpty() ? new HashMap<>() : data.internalEntryPoints;
					for(EntryPoint ep : allEntryPointsForStub.get(sm)) {
						Set<Unit> units = ieEntryPoints.get(ep);
						if(units == null) {
							units = new HashSet<>();
							ieEntryPoints.put(ep, units);
						}
						units.add(e.srcUnit());
					}
				}
				
				if(allEntryPointsNotForStub.containsKey(sm)) {
					Map<EntryPoint,Set<Unit>> ieEntryPoints = data.externalEntryPoints = 
							data.externalEntryPoints.isEmpty() ? new HashMap<>() : data.externalEntryPoints;
					for(EntryPoint ep : allEntryPointsNotForStub.get(sm)) {
						Set<Unit> units = ieEntryPoints.get(ep);
						if(units == null) {
							units = new HashSet<>();
							ieEntryPoints.put(ep, units);
						}
						units.add(e.srcUnit());
					}
				}
				
				if(!excludeHandler.isExcludedMethodWithOverride(sm)) {
					if(seen.add(sm)) {
						runners.add(new SimpleFinderRunner(sm,data.depth+1,seen,excludeHandler,allEntryPointsForStub,allEntryPointsNotForStub,logger));
					} else {
						if(data.moreUses.isEmpty())
							data.moreUses = new HashSet<>();
						data.moreUses.add(sm);
					}
				}
			}
			if(!runners.isEmpty())
				executeRunners(runners,logger);
		}

		@Override
		public RetVal getValue() {
			return data;
		}
		
	}
	
	private static final class ContextQuerySubGraphGroup extends ValueWorkerGroup<Map<SootMethod,Set<SootMethod>>,
	Pair<SootMethod,Set<SootMethod>>> {
		public ContextQuerySubGraphGroup(String name, String ep, ILogger logger) {
			super(name,ep,logger,false,false);
		}
		@Override
		protected Map<SootMethod,Set<SootMethod>> initReturnValue() { return new HashMap<>(); }

		@Override
		protected void joinWorkerReturnValue(Pair<SootMethod,Set<SootMethod>> v) {
			Set<SootMethod> temp = ret.get(v.getFirst());
			if(temp == null) {
				temp = new HashSet<>();
				ret.put(v.getFirst(), temp);
				temp.add(v.getFirst());//Make sure the context query method is part of its subgraph
			}
			temp.addAll(v.getSecond());
		}

		@Override
		protected void finalizeReturnValue() {}
	}
	
	public final class ContextQuerySubGraphRunner implements ValueRunner<Pair<SootMethod,Set<SootMethod>>> {
		
		private final SootMethod cq;
		private final SootMethod cur;
		private volatile Set<SootMethod> ret;
		private final Set<SootMethod> seen;
		private final IExcludeHandler excludeHandler;
		private final ILogger logger;
		
		public ContextQuerySubGraphRunner(SootMethod cq, SootMethod cur, Set<SootMethod> seen, IExcludeHandler excludeHandler, ILogger logger) {
			this.cq = cq;
			this.cur = cur;
			this.seen = seen;
			this.excludeHandler = excludeHandler;
			this.logger = logger;
			this.ret = Collections.emptySet();
		}
		
		@Override
		public void run() {
			List<ValueRunner<Pair<SootMethod,Set<SootMethod>>>> runners = new ArrayList<>();
			//Make sure that if the context query we start with is excluded we don't explore its body
			if(!excludeHandler.isExcludedMethodWithOverride(cur) && !cur.getName().equals("<init>") && !cur.getName().equals("<clinit>")) {
				Iterator<Edge> itEdge = new Filter(edgePredicate).wrap(cg.edgesOutOf(cur));
				while(itEdge.hasNext()) {
					Edge e = itEdge.next();
					SootMethod sm = e.tgt();
					if(seen.add(sm)) {
						if(!excludeHandler.isExcludedMethodWithOverride(sm) && !sm.getName().equals("<init>") && !sm.getName().equals("<clinit>")) {
							runners.add(new ContextQuerySubGraphRunner(cq,sm,seen,excludeHandler,logger));
							if(ret.isEmpty())
								ret = new HashSet<>();
							ret.add(sm);
						}
					}
				}
			}
			if(!runners.isEmpty())
				executeRunners(runners,logger);
		}

		@Override
		public Pair<SootMethod,Set<SootMethod>> getValue() {
			return new Pair<SootMethod,Set<SootMethod>>(cq,ret);
		}
		
	}
	
}
