package org.sag.acminer.phases.controlpredicatefilter;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.database.accesscontrol.AccessControlDatabaseFactory;
import org.sag.acminer.database.accesscontrol.IAccessControlDatabase;
import org.sag.acminer.database.defusegraph.DefUseGraph;
import org.sag.acminer.database.defusegraph.StartNode;
import org.sag.acminer.database.excludedelements.IExcludeHandler;
import org.sag.acminer.database.filter.ControlPredicateFilterDatabase;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.concurrent.CountingThreadExecutor;
import org.sag.common.concurrent.IgnorableRuntimeException;
import org.sag.common.concurrent.LoggingWorkerGroup;
import org.sag.common.concurrent.WorkerCountingThreadExecutor;
import org.sag.common.io.FileHelpers;
import org.sag.common.logging.IDebugLogger;
import org.sag.common.logging.ILogger;
import org.sag.common.tools.SortingMethods;
import org.sag.common.tuple.Pair;
import org.sag.main.logging.DebugLogger;
import org.sag.main.phase.IPhaseHandler;
import org.sag.main.phase.IPhaseOption;
import org.sag.soot.SootSort;
import org.sag.soot.callgraph.ExcludingJimpleICFG;
import org.sag.soot.callgraph.IJimpleICFG;
import org.sag.soot.callgraph.JimpleICFG;
import org.sag.soot.callgraph.ExcludingJimpleICFG.ExcludingEdgePredicate;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class ControlPredicateFilter {
	
	private final ControlPredicateFilterDatabase cpFilter;
	private final IPhaseHandler handler;
	private final boolean enableDebug;
	private final boolean forceDebugToConsole;
	private final ILogger mainLogger;
	private final IACMinerDataAccessor dataAccessor;
	private final String name;
	
	ControlPredicateFilter(ControlPredicateFilterDatabase cpFilter, IACMinerDataAccessor dataAccessor, IPhaseHandler handler, ILogger mainLogger) {
		this.cpFilter = cpFilter;
		this.handler = handler;
		this.enableDebug = isOptionEnabled(ControlPredicateFilterHandler.optEnableDebug);
		this.forceDebugToConsole = isOptionEnabled(ControlPredicateFilterHandler.optDebugToConsole);;
		this.dataAccessor = dataAccessor;
		this.mainLogger = mainLogger;
		this.name = getClass().getSimpleName();
	}
	
	private boolean isOptionEnabled(String name) {
		IPhaseOption<?> o = handler.getPhaseOptionUnchecked(name);
		if(o == null || !o.isEnabled())
			return false;
		return true;
	}
	
	public boolean filterControlPredicates() {
		boolean successOuter = true;
		IAccessControlDatabase newCPDB = AccessControlDatabaseFactory.getNewControlPredicatesDatabase(false);
		
		mainLogger.info("{}: Begin the control predicate filter.",name);
		
		if(enableDebug) {
			Path path = dataAccessor.getConfig().getFilePath("debug_control-predicate-filter-dir");
			try {
				FileHelpers.processDirectory(path, true, false);
			} catch(Throwable t) {
				mainLogger.fatal("{}: Failed to process PolicyMiner's miner debug directory for the control predicate filter "
						+ "'{}'. All filtering will be skipped.",t,name,path);
				successOuter = false;
			}
		}
		
		if(successOuter) {
			DebugLogger.init(enableDebug,forceDebugToConsole,false);
			WorkerCountingThreadExecutor exe = null;
			List<LoggingWorkerGroup> workerGroups = new ArrayList<>();
			ControlPredicateFilterApplicator app = null;
			try{
				JimpleICFG baseICFG = new JimpleICFG(dataAccessor.getEntryPoints(),false);
				SootClass stub = null;
				LoggingWorkerGroup curWorkerGroup = null;
				Deque<EntryPoint> eps = new ArrayDeque<>(dataAccessor.getEntryPoints());
				exe = new WorkerCountingThreadExecutor();
				app = new ControlPredicateFilterApplicator();
				while(!eps.isEmpty()) {
					EntryPoint ep = eps.poll();
					if(stub == null || !stub.equals(ep.getStub())) {
						stub = ep.getStub();
						if(curWorkerGroup != null) {
							curWorkerGroup.unlockInitialLock();
							curWorkerGroup = null;
						}
						LoggingWorkerGroup g = new LoggingWorkerGroup(name,stub.toString(),false);
						if(g.getLogger() == null) {
							mainLogger.fatal("{}: Failed to initilize local logger for '{}'. Skipping analysis of '{}'.",name,stub,stub);
							successOuter = false;
						} else {
							curWorkerGroup = g;
							workerGroups.add(g);
						}	
					}
					if(curWorkerGroup != null){
						Runnable runner = new ControlPredicateFilterRunner(ep,baseICFG,app,cpFilter,newCPDB,curWorkerGroup.getLogger());
						try {
							exe.execute(runner, curWorkerGroup);
						} catch(Throwable t) {
							mainLogger.fatal("{}: Failed to execute '{}' for group '{}'.",name,runner.toString(),curWorkerGroup.getName());
							successOuter = false;
						}
					}
				}
				//Unlock the initial lock for the last group produced by the loop
				if(curWorkerGroup != null) {
					curWorkerGroup.unlockInitialLock();
					curWorkerGroup = null;
				}
			} catch(Throwable t) {
				mainLogger.fatal("{}: An unexpected exception occured in the countrol predicate filter.",t,name);
				successOuter = false;
			} finally {
				//Shutdown the executors
				if(exe != null && !exe.shutdownWhenFinished()){
					mainLogger.fatal(CountingThreadExecutor.computeJointErrorMsg(exe.getAndClearExceptions(), 
							"Failed to wait for the executor to terminate.", name));
					successOuter = false;
				}
				if(app != null && !app.shutdownWhenFinished()) {
					mainLogger.fatal(CountingThreadExecutor.computeJointErrorMsg(app.getAndClearExceptions(), 
							"Failed to wait for the control predicated finder to terminate.",name));
					successOuter = false;
				}
				
				for(LoggingWorkerGroup g : workerGroups) {
					if(g.shutdownNormally() && !g.hasExceptions()) {
						mainLogger.info("{}: Successfully filtered all control predicates for Stub '{}'.",name,g.getName());
					} else {
						mainLogger.fatal("{}: Failed to filter all control predicates for Stub '{}'.",name,g.getName());
						successOuter = false;
					}
				}
				
				for(int i = 0; i < 5; i++){
					try {
						DebugLogger.end();//blocks unit all debugging io has finished and thread exits
						DebugLogger.removeEmptyDirs(dataAccessor.getConfig().getFilePath("debug_control-predicate-filter-dir"),mainLogger);
						break;
					} catch (InterruptedException e1) {
						mainLogger.fatal("{}: Failed to end PMinerDebugLogger. Attempt {}.",name,e1,i);
					}
				}
			}
		}
		if(!successOuter){
			mainLogger.fatal("{}: The control predicate filter failed for one or more entry points. Stopping Analysis!",name);
			return false;
		}else{
			dataAccessor.setControlPredicatesDB(newCPDB);
			mainLogger.info("{}: The control predicate filter succeeded!",name);
			return true;
		}
		
	}
	
	private class ControlPredicateFilterRunner implements Runnable {
		
		private final EntryPoint ep;
		private final JimpleICFG baseICFG;
		private final ILogger logger;
		private final ControlPredicateFilterApplicator app;
		private final ControlPredicateFilterDatabase cpFilter;
		private final IAccessControlDatabase newCPDB;
		
		public ControlPredicateFilterRunner(EntryPoint ep, JimpleICFG baseICFG,
				ControlPredicateFilterApplicator app, ControlPredicateFilterDatabase cpFilter, IAccessControlDatabase newCPDB, ILogger logger) {
			this.ep = ep;
			this.baseICFG = baseICFG;
			this.logger = logger;
			this.app = app;
			this.cpFilter = cpFilter;
			this.newCPDB = newCPDB;
		}

		@Override
		public void run() {
			IExcludeHandler excludeHandler = null;
			IDebugLogger debugLogger = null;
			IJimpleICFG icfg = null;
			
			logger.fineInfo("{}: Begin filtering control predicates for ep '{}'.",name,ep);
			
			debugLogger = DebugLogger.getNewDebugLogger(ep.getStub(), ep.getEntryPoint(), 
					dataAccessor.getConfig().getFilePath("debug_control-predicate-filter-dir"));
			if(debugLogger == null){
				logger.fatal("{}: Failed to start the debug logger for ep '{}'.",name,ep);
				throw new IgnorableRuntimeException();
			}
			
			try{
				excludeHandler = dataAccessor.getExcludedElementsDB().createNewExcludeHandler(ep);
				icfg = new ExcludingJimpleICFG(ep, baseICFG, new ExcludingEdgePredicate(baseICFG.getCallGraph(), excludeHandler));
				
				Set<Unit> cps = dataAccessor.getControlPredicatesDB().getUnits(ep);
				DefUseGraph graph = dataAccessor.getDefUseGraphDB().getDefUseGraph(ep, dataAccessor.getConfig().getFilePath("acminer_defusegraph-dir"));
				Map<Unit, StartNode> unitToStartNode = new HashMap<>();
				for(StartNode sn : graph.getStartNodes()) {
					Unit u = sn.getUnit();
					if(!cps.contains(u)) {
						logger.fatal("{}: Unit '{}' in def use graph is not a control predicate for ep '{}'!?!",name,u,ep);
						throw new IgnorableRuntimeException();
					}
					unitToStartNode.put(u, sn);
				}
				if(cps.size() != unitToStartNode.size()) {
					logger.fatal("{}: One or more control predicates is not in the def use graph for ep '{}'!?!",name,ep);
					throw new IgnorableRuntimeException();
				}
					
				Map<SootMethod, Set<Unit>> ret = app.applyFilter(ep, icfg, cpFilter, unitToStartNode, graph, dataAccessor, enableDebug, debugLogger);
				//Sort return CP
				for(SootMethod m : ret.keySet()) {
					ret.put(m, SortingMethods.sortSet(ret.get(m),SootSort.unitComp));
				}
				ret = SortingMethods.sortMapKey(ret, SootSort.smComp);
				
				//Store the return CP
				newCPDB.add(ep, computeDepth(ep,ret,excludeHandler));
				logger.fineInfo("{}: The control predicate filter succeeded for ep '{}'.",name,ep);
			} catch(IgnorableRuntimeException t) {
				throw t;
			} catch(Throwable t) {
				logger.fatal("{}: An unexpected error occured while filtering control predicates for ep '{}'.",t,ep);
				throw new IgnorableRuntimeException();
			} finally {
				if(debugLogger != null){
					debugLogger.close();
					debugLogger = null;
				}
			}
		}
		
		private Map<SootMethod,Pair<Set<Unit>,Set<Integer>>> computeDepth(EntryPoint ep, Map<SootMethod,Set<Unit>> dataIn, 
				IExcludeHandler excludeHandler){
			Map<SootMethod,Pair<Set<Unit>,Set<Integer>>> dataOut = new HashMap<>();
			for(SootMethod source : dataIn.keySet()){
				dataOut.put(source, new Pair<Set<Unit>,Set<Integer>>(new HashSet<Unit>(dataIn.get(source)),new HashSet<Integer>()));
			}
			CallGraph cg = Scene.v().getCallGraph();
			HashSet<SootMethod> visited = new HashSet<SootMethod>();
			Queue<SootMethod> tovisit = new ArrayDeque<SootMethod>();
			Queue<Integer> depthCount = new ArrayDeque<Integer>();
			tovisit.add(ep.getEntryPoint());
			depthCount.add(0);
			while(!tovisit.isEmpty()){
				SootMethod curr = tovisit.poll();
				Integer curDepth = depthCount.poll();
				if (visited.contains(curr)) {
					Pair<Set<Unit>,Set<Integer>> p = dataOut.get(curr);
					if(p != null){
						p.getSecond().add(curDepth);
					}
					continue;
				}
				if(excludeHandler.isExcludedMethodWithOverride(curr)){
					continue;
				}
				visited.add(curr);
				
				Pair<Set<Unit>,Set<Integer>> p = dataOut.get(curr);
				if(p != null){
					p.getSecond().add(curDepth);
				}
				
				Iterator<Edge> itEdge = cg.edgesOutOf(curr);
				while(itEdge.hasNext()){
					Edge e = itEdge.next();
					SootMethod sm = e.tgt();
					if(!visited.contains(sm)){
						tovisit.add(sm);
						depthCount.add(curDepth+1);
					}else{
						Pair<Set<Unit>,Set<Integer>> p2 = dataOut.get(sm);
						if(p2 != null){
							p2.getSecond().add(curDepth+1);
						}
					}
				}
			}
			return dataOut;
		}
		
	}

}
