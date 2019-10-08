package org.sag.acminer.phases.controlpredicatemarker;
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
import org.sag.acminer.database.excludedelements.IExcludeHandler;
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
import org.sag.main.config.Config;
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
import soot.jimple.IfStmt;
import soot.jimple.SwitchStmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;


public class ControlPredicateMarker {

	private final IPhaseHandler handler;
	private final boolean enableDebug;
	private final boolean forceDebugToConsole;
	private final ILogger mainLogger;
	private final IACMinerDataAccessor dataAccessor;
	private final Config config;
	private final String name;

	ControlPredicateMarker(IACMinerDataAccessor dataAccessor, IPhaseHandler handler, ILogger mainLogger){
		this.handler = handler;
		this.enableDebug = isOptionEnabled(ControlPredicateMarkerHandler.optEnableDebug);
		this.forceDebugToConsole = isOptionEnabled(ControlPredicateMarkerHandler.optDebugToConsole);
		this.dataAccessor = dataAccessor;
		this.mainLogger = mainLogger;
		this.name = getClass().getSimpleName();
		this.config = dataAccessor.getConfig();
	}
	
	private boolean isOptionEnabled(String name) {
		IPhaseOption<?> o = handler.getPhaseOptionUnchecked(name);
		if(o == null || !o.isEnabled())
			return false;
		return true;
	}
	
	public boolean markControlPredicates() {
		boolean successOuter = true;
		
		mainLogger.info("{}: Begin the control predicate marker.",name);
		
		if(enableDebug){
			Path debugDir = config.getFilePath("debug_control-predicate-marker-dir");
			try{
				FileHelpers.processDirectory(debugDir, true, false);
			} catch(Throwable t){
				mainLogger.fatal("{}: Failed to process PolicyMiner's miner debug directory for the control predicate marker "
						+ "'{}'. All marking will be skipped.",t,name,debugDir);
				successOuter = false;
			}
		}
		
		if(successOuter){
			DebugLogger.init(enableDebug,forceDebugToConsole,false);
			WorkerCountingThreadExecutor exe = null;
			ControlPredicateFinder cpf = null;
			List<LoggingWorkerGroup> workerGroups = new ArrayList<>();
			//Set new control predicates database 
			dataAccessor.setControlPredicatesDB(AccessControlDatabaseFactory.getNewControlPredicatesDatabase(false));
			try{
				JimpleICFG baseICFG = new JimpleICFG(dataAccessor.getEntryPoints(),false);
				SootClass stub = null;
				LoggingWorkerGroup curWorkerGroup = null;
				Deque<EntryPoint> eps = new ArrayDeque<>(dataAccessor.getEntryPoints());
				exe = new WorkerCountingThreadExecutor();
				cpf = new ControlPredicateFinder();
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
						Runnable runner = new ControlPredicateMarkerRunner(ep,baseICFG,cpf,curWorkerGroup.getLogger());
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
				mainLogger.fatal("{}: An unexpected exception occured in the countrol predicate marker.",t,name);
				successOuter = false;
			} finally {
				//Shutdown the executors
				if(exe != null && !exe.shutdownWhenFinished()){
					mainLogger.fatal(CountingThreadExecutor.computeJointErrorMsg(exe.getAndClearExceptions(), 
							"Failed to wait for the executor to terminate.", name));
					successOuter = false;
				}
				if(cpf != null && !cpf.shutdownWhenFinished()) {
					mainLogger.fatal(CountingThreadExecutor.computeJointErrorMsg(cpf.getAndClearExceptions(), 
							"Failed to wait for the control predicated finder to terminate.",name));
					successOuter = false;
				}
				
				for(LoggingWorkerGroup g : workerGroups) {
					if(g.shutdownNormally() && !g.hasExceptions()) {
						mainLogger.info("{}: Successfully marked all control predicates for Stub '{}'.",name,g.getName());
					} else {
						mainLogger.fatal("{}: Failed to mark all control predicates for Stub '{}'.",name,g.getName());
						successOuter = false;
					}
				}
				
				for(int i = 0; i < 5; i++){
					try {
						DebugLogger.end();//blocks unit all debugging io has finished and thread exits
						DebugLogger.removeEmptyDirs(config.getFilePath("debug_control-predicate-marker-dir"),mainLogger);
						break;
					} catch (InterruptedException e1) {
						mainLogger.fatal("{}: Failed to end PMinerDebugLogger. Attempt {}.",name,e1,i);
					}
				}
			}
		}
		if(!successOuter){
			mainLogger.fatal("{}: The control predicate marker failed for one or more entry points. Stopping Analysis!",name);
			return false;
		}else{
			mainLogger.info("{}: The control predicate marker succeeded!",name);
			return true;
		}
	}
	
	private class ControlPredicateMarkerRunner implements Runnable {
		
		private final EntryPoint entryPoint;
		private final JimpleICFG baseICFG;
		private final ILogger logger;
		private final ControlPredicateFinder cpf;
		
		public ControlPredicateMarkerRunner(EntryPoint entryPoint, JimpleICFG baseICFG, ControlPredicateFinder cpf, ILogger logger) {
			this.entryPoint = entryPoint;
			this.baseICFG = baseICFG;
			this.logger = logger;
			this.cpf = cpf;
		}

		@Override
		public void run() {
			IExcludeHandler excludeHandler = null;
			IDebugLogger debugLogger = null;
			IJimpleICFG icfg = null;
			
			logger.fineInfo("{}: Begin marking control predicates for ep '{}'.",name,entryPoint);
			
			debugLogger = DebugLogger.getNewDebugLogger(entryPoint.getStub(), entryPoint.getEntryPoint(), 
					config.getFilePath("debug_control-predicate-marker-dir"));
			if(debugLogger == null){
				logger.fatal("{}: Failed to start the debug logger for ep '{}'.",name,entryPoint);
				throw new IgnorableRuntimeException();
			}
			
			try{
				excludeHandler = dataAccessor.getExcludedElementsDB().createNewExcludeHandler(entryPoint);
				icfg = new ExcludingJimpleICFG(entryPoint, baseICFG, new ExcludingEdgePredicate(baseICFG.getCallGraph(), excludeHandler));
				
				//Find all if and switch statements that are in the subgraphs of all context queries of a entry point and add them as CP
				Map<SootMethod, Set<Unit>> ret = new HashMap<>();
				for(SootMethod m : dataAccessor.getContextQueriesDB().getSubGraphMethods(entryPoint)) {
					//Body grabbed already in the JimpleICFG init method so all that have a body will have a body
					if(m.hasActiveBody() && !excludeHandler.isExcludedMethodWithOverride(m)) {
						for(Unit u : m.getActiveBody().getUnits()) {
							if(u instanceof IfStmt || u instanceof SwitchStmt) {
								Set<Unit> temp = ret.get(m);
								if(temp == null) {
									temp = new HashSet<>();
									ret.put(m, temp);
								}
								temp.add(u);
							}
						}
					}
				}
				
				//Gather the seeds
				Set<Unit> seeds = new HashSet<Unit>();
				seeds.addAll(dataAccessor.getThrowSecurityExceptionStmtsDB().getUnits(entryPoint));
				seeds.addAll(dataAccessor.getContextQueriesDB().getUnits(entryPoint));
				
				//Find the remaining CP using the backwards analysis and small forward analysis and add them to our return CP
				Map<SootMethod, Set<Unit>>  moreRet = cpf.findControlPredicates(seeds, entryPoint, icfg, debugLogger);
				for(SootMethod m : moreRet.keySet()) {
					Set<Unit> units = moreRet.get(m);
					Set<Unit> exist = ret.get(m);
					if(exist == null) {
						exist = new HashSet<>();
						ret.put(m, exist);
					}
					exist.addAll(units);
				}
				
				//Sort return CP
				for(SootMethod m : ret.keySet()) {
					ret.put(m, SortingMethods.sortSet(ret.get(m),SootSort.unitComp));
				}
				ret = SortingMethods.sortMapKey(ret, SootSort.smComp);
				
				//Store the return CP
				dataAccessor.getControlPredicatesDB().add(entryPoint, computeDepth(entryPoint.getEntryPoint(),ret,excludeHandler));
				logger.fineInfo("{}: The control predicate marker succeeded for ep '{}'.",name,entryPoint);
			} catch(IgnorableRuntimeException t) {
				throw t;
			} catch(Throwable t) {
				logger.fatal("{}: An unexpected error occured while marking control predicates for ep '{}'.",t,entryPoint);
				throw new IgnorableRuntimeException();
			} finally {
				if(debugLogger != null){
					debugLogger.close();
					debugLogger = null;
				}
			}
		}
		
		private Map<SootMethod,Pair<Set<Unit>,Set<Integer>>> computeDepth(SootMethod ep, Map<SootMethod,Set<Unit>> dataIn, 
				IExcludeHandler excludeHandler){
			Map<SootMethod,Pair<Set<Unit>,Set<Integer>>> dataOut = new HashMap<>();
			for(SootMethod source : dataIn.keySet()){
				dataOut.put(source, new Pair<Set<Unit>,Set<Integer>>(new HashSet<Unit>(dataIn.get(source)),new HashSet<Integer>()));
			}
			CallGraph cg = Scene.v().getCallGraph();
			HashSet<SootMethod> visited = new HashSet<SootMethod>();
			Queue<SootMethod> tovisit = new ArrayDeque<SootMethod>();
			Queue<Integer> depthCount = new ArrayDeque<Integer>();
			tovisit.add(ep);
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
