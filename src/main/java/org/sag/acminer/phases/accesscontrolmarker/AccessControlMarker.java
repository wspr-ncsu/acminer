package org.sag.acminer.phases.accesscontrolmarker;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.database.accesscontrol.AccessControlDatabaseFactory;
import org.sag.acminer.database.entrypointedges.IEntryPointEdgesDatabase;
import org.sag.acminer.database.excludedelements.IExcludeHandler;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.concurrent.CountingThreadExecutor;
import org.sag.common.concurrent.IgnorableRuntimeException;
import org.sag.common.concurrent.LoggingWorkerGroup;
import org.sag.common.concurrent.WorkerCountingThreadExecutor;
import org.sag.common.logging.ILogger;
import org.sag.common.tuple.Pair;
import org.sag.common.tuple.Triple;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;

public class AccessControlMarker {

	private final IACMinerDataAccessor dataAccessor;
	private final String name;
	private final Set<SootMethod> contextQueries;
	private final ILogger mainLogger;
	
	
	public AccessControlMarker(Set<SootMethod> contextQueries, IACMinerDataAccessor dataAccessor, ILogger mainLogger){
		this.name = getClass().getSimpleName();
		this.contextQueries = contextQueries;
		this.mainLogger = mainLogger;
		this.dataAccessor = dataAccessor;
		
		
	}
	
	public boolean run() {
		boolean successOuter = true;
		WorkerCountingThreadExecutor exe = null;
		SimpleFinder sf = null;
		List<LoggingWorkerGroup> workerGroups = new ArrayList<>();
		
		mainLogger.info("{}: Begin the simple marker.",name);
		
		//Set new databases 
		dataAccessor.setContextQueriesDB(AccessControlDatabaseFactory.getNewContextQueriesDatabase(false));
		dataAccessor.setThrowSecurityExceptionStmtsDB(AccessControlDatabaseFactory.getNewThrowSecurityExceptionStmtsDatabase(false));
		dataAccessor.setEntryPointEdgesDB(IEntryPointEdgesDatabase.Factory.getNew(false));
		
		try {
			SootClass stub = null;
			LoggingWorkerGroup curWorkerGroup = null;
			Deque<EntryPoint> eps = new ArrayDeque<>(dataAccessor.getEntryPoints());
			exe = new WorkerCountingThreadExecutor();
			sf = new SimpleFinder(contextQueries);
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
					Runnable runner = new AccessControlMarkerRunner(ep,sf,curWorkerGroup.getLogger());
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
			mainLogger.fatal("{}: An unexpected exception occured.",t,name);
			successOuter = false;
		} finally {
			//Shutdown the executors
			if(exe != null && !exe.shutdownWhenFinished()){
				mainLogger.fatal(CountingThreadExecutor.computeJointErrorMsg(exe.getAndClearExceptions(), 
						"Failed to wait for the executor to terminate.", name));
				successOuter = false;
			}
			
			if(sf != null && !sf.shutdownWhenFinished()) {
				mainLogger.fatal(CountingThreadExecutor.computeJointErrorMsg(sf.getAndClearExceptions(), 
						"Failed to wait for the simple finder to terminate.",name));
				successOuter = false;
			}
			
			for(LoggingWorkerGroup g : workerGroups) {
				if(g.shutdownNormally() && !g.hasExceptions()) {
					mainLogger.info("{}: Success for Stub '{}'.",name,g.getName());
				} else {
					mainLogger.fatal("{}: Failed for Stub '{}'.",name,g.getName());
					successOuter = false;
				}
			}
		}
		if(!successOuter){
			mainLogger.fatal("{}: Failed for one or more entry points. Stopping Analysis!",name);
			return false;
		}else{
			mainLogger.info("{}: Success!",name);
			return true;
		}
		
	}
	
	private class AccessControlMarkerRunner implements Runnable {
		private final EntryPoint entryPoint;
		private final SimpleFinder sf;
		private final ILogger logger;
		
		public AccessControlMarkerRunner(EntryPoint ep, SimpleFinder sf, ILogger logger) {
			this.entryPoint = ep;
			this.sf = sf;
			this.logger = logger;
		}
		
		@Override
		public void run() {
			logger.fineInfo("{}: Begin for ep '{}'.",name,entryPoint);
			try{
				IExcludeHandler excludeHandler = dataAccessor.getExcludedElementsDB().createNewExcludeHandler(entryPoint);
				Map<SootMethod,Set<EntryPoint>> allEntryPointsNotForStub = new HashMap<>();
				Map<SootMethod,Set<EntryPoint>> allEntryPointsForStub = new HashMap<>();
				for(EntryPoint e : dataAccessor.getEntryPoints()) {
					if(e.getEntryPoint() != null) {
						if(Objects.equals(e.getStub(), entryPoint.getStub())) {
							Set<EntryPoint> temp = allEntryPointsForStub.get(e.getEntryPoint());
							if(temp == null) {
								temp = new HashSet<>();
								allEntryPointsForStub.put(e.getEntryPoint(), temp);
							}
							temp.add(e);
						} else {
							Set<EntryPoint> temp = allEntryPointsNotForStub.get(e.getEntryPoint());
							if(temp == null) {
								temp = new HashSet<>();
								allEntryPointsNotForStub.put(e.getEntryPoint(), temp);
							}
							temp.add(e);
						}
					}
				}
				
				Triple<Map<SootMethod, Pair<Set<Unit>,Set<Integer>>>,Map<SootMethod, Pair<Set<Unit>,Set<Integer>>>,
						Map<EntryPoint,Pair<IEntryPointEdgesDatabase.Type,Map<SootMethod,Pair<Set<Unit>,Set<Integer>>>>>> ret =
						sf.findData(entryPoint, excludeHandler, allEntryPointsForStub, allEntryPointsNotForStub, logger);
				dataAccessor.getContextQueriesDB().add(entryPoint, ret.getFirst());
				dataAccessor.getThrowSecurityExceptionStmtsDB().add(entryPoint, ret.getSecond());
				dataAccessor.getEntryPointEdgesDB().add(entryPoint, ret.getThird());
				
				Set<Unit> cqUnits = new HashSet<>();
				for(Pair<Set<Unit>,Set<Integer>> d : ret.getFirst().values()) {
					cqUnits.addAll(d.getFirst());
				}
				Map<SootMethod,Set<SootMethod>> val2 = sf.getContextQuerySubGraphs(entryPoint, cqUnits, excludeHandler, logger);
				dataAccessor.getContextQueriesDB().addContextQuerySubGraphs(entryPoint, val2);
				
				
				logger.fineInfo("{}: Success for ep '{}'.",name,entryPoint);
			} catch(IgnorableRuntimeException t) {
				throw t;
			} catch(Throwable t) {
				mainLogger.fatal("{}: An unexpected error occured for ep '{}'.",t,entryPoint);
				logger.fatal("{}: An unexpected error occured for ep '{}'.",t,entryPoint);
				throw new IgnorableRuntimeException();
			}
		}
	}

}
