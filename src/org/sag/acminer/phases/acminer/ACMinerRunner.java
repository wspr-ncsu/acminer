package org.sag.acminer.phases.acminer;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.database.acminer.IACMinerDatabase;
import org.sag.acminer.database.defusegraph.DefUseGraph;
import org.sag.acminer.database.defusegraph.IInvokeNode;
import org.sag.acminer.database.defusegraph.INode;
import org.sag.acminer.database.defusegraph.InlineConstantLeafNode;
import org.sag.acminer.database.defusegraph.InvokeStartNode;
import org.sag.acminer.database.defusegraph.LocalWrapper;
import org.sag.acminer.database.defusegraph.StartNode;
import org.sag.acminer.database.defusegraph.id.DataWrapperPart;
import org.sag.acminer.database.defusegraph.id.IdentityRefPart;
import org.sag.acminer.database.defusegraph.id.LiteralPart;
import org.sag.acminer.database.defusegraph.id.MethodRefPart;
import org.sag.acminer.database.defusegraph.id.Part;
import org.sag.acminer.database.excludedelements.IExcludeHandler;
import org.sag.acminer.phases.acminer.dw.AllConstant;
import org.sag.acminer.phases.acminer.dw.DataWrapper;
import org.sag.acminer.phases.acminer.dw.NoneConstant;
import org.sag.acminer.phases.acminer.dw.PrimitiveConstant;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.concurrent.CountingThreadExecutor;
import org.sag.common.concurrent.IgnorableRuntimeException;
import org.sag.common.concurrent.LoggingWorkerGroup;
import org.sag.common.concurrent.Worker;
import org.sag.common.concurrent.WorkerCountingThreadExecutor;
import org.sag.common.io.FileHelpers;
import org.sag.common.logging.IDebugLogger;
import org.sag.common.logging.ILogger;
import org.sag.common.tools.HierarchyHelpers;
import org.sag.common.tools.SortingMethods;
import org.sag.common.tuple.Pair;
import org.sag.common.tuple.Quad;
import org.sag.common.tuple.Triple;
import org.sag.main.logging.DebugLogger;
import org.sag.main.phase.IPhaseHandler;
import org.sag.main.phase.IPhaseOption;
import org.sag.soot.SootSort;
import org.sag.soot.callgraph.ExcludingJimpleICFG;
import org.sag.soot.callgraph.JimpleICFG;
import org.sag.soot.callgraph.ExcludingJimpleICFG.ExcludingEdgePredicate;
import org.sag.soot.callgraph.IJimpleICFG;

import com.google.common.collect.ImmutableSet;

import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.ParameterRef;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.Stmt;

public class ACMinerRunner {
	
	//TODO this can probably be done in a better non manual way
	private final Set<String> toRemove = ImmutableSet.of(
			"<com.android.server.am.ActivityManagerService: int checkComponentPermission(java.lang.String,int,int,int,boolean)>",
			"<android.content.Context: int checkCallingOrSelfPermission(java.lang.String)>",
			"<android.content.Context: int checkPermission(java.lang.String,int,int)>",
			"<android.content.Context: int checkPermission(java.lang.String,int,int,android.os.IBinder)>",
			"<android.content.Context: int checkCallingPermission(java.lang.String)>",
			"<android.content.Context: int checkSelfPermission(java.lang.String)>",
			"<android.content.pm.PackageManager: int checkPermission(java.lang.String,java.lang.String)>",
			//"<com.android.server.am.ActivityManagerService: boolean hasUsageStatsPermission(java.lang.String)>",
			"<android.app.ActivityManager: int checkComponentPermission(java.lang.String,int,int,boolean)>",
			"<android.app.ActivityManager: int checkUidPermission(java.lang.String,int)>",
			"<com.android.server.am.ActivityManagerService: int checkCallingPermission(java.lang.String)>"
	);
	
	private final Set<String> permCheckSigs = ImmutableSet.of(
			"<com.android.server.am.ActivityManagerService: int checkPermission(java.lang.String,int,int)>",
			"<com.android.server.am.ActivityManagerService: int checkPermissionWithToken(java.lang.String,int,int,android.os.IBinder)>",
			//"<com.android.server.am.ActivityManagerService: int checkGrantUriPermission(int,java.lang.String,android.net.Uri,int,int)>",
			//"<com.android.server.am.ActivityManagerService: int checkUriPermission(android.net.Uri,int,int,int,int,android.os.IBinder)>",
			"<com.android.server.am.ActivityManagerService: int checkCallingPermission(java.lang.String)>",
			"<com.android.server.am.ActivityManagerService: void enforceCallingPermission(java.lang.String,java.lang.String)>",
			"<com.android.server.am.ActivityManagerService: int checkComponentPermission(java.lang.String,int,int,int,boolean)>",
			"<android.content.Context: int checkCallingOrSelfPermission(java.lang.String)>",
			"<android.content.Context: int checkPermission(java.lang.String,int,int)>",
			"<android.content.Context: int checkPermission(java.lang.String,int,int,android.os.IBinder)>",
			"<android.content.Context: int checkCallingPermission(java.lang.String)>",
			"<android.content.Context: int checkSelfPermission(java.lang.String)>",
			"<android.content.Context: void enforcePermission(java.lang.String,int,int,java.lang.String)>",
			"<android.content.Context: void enforceCallingPermission(java.lang.String,java.lang.String)>",
			"<android.content.Context: void enforceCallingOrSelfPermission(java.lang.String,java.lang.String)>",
			//"<android.content.Context: int checkUriPermission(android.net.Uri,int,int,int)>",
			//"<android.content.Context: int checkUriPermission(android.net.Uri,int,int,int,android.os.IBinder)>",
			//"<android.content.Context: int checkCallingUriPermission(android.net.Uri,int)>",
			//"<android.content.Context: int checkCallingOrSelfUriPermission(android.net.Uri,int)>",
			//"<android.content.Context: int checkUriPermission(android.net.Uri,java.lang.String,java.lang.String,int,int,int)>",
			//"<android.content.Context: void enforceUriPermission(android.net.Uri,int,int,int,java.lang.String)>",
			//"<android.content.Context: void enforceCallingUriPermission(android.net.Uri,int,java.lang.String)>",
			//"<android.content.Context: void enforceCallingOrSelfUriPermission(android.net.Uri,int,java.lang.String)>",
			//"<android.content.Context: void enforceUriPermission(android.net.Uri,java.lang.String,java.lang.String,int,int,int,java.lang.String)>",
			"<android.app.ActivityManager: int checkComponentPermission(java.lang.String,int,int,boolean)>",
			"<android.app.ActivityManager: int checkUidPermission(java.lang.String,int)>",
			"<android.content.pm.PackageManager: int checkPermission(java.lang.String,java.lang.String)>",
			"<com.android.server.pm.PackageManagerService: int checkPermission(java.lang.String,java.lang.String,int)>",
			"<com.android.server.pm.PackageManagerService: int checkUidPermission(java.lang.String,int)>"
			
	);
	
	private final IPhaseHandler handler;
	private final Set<String> toRemoveSigs;
	private final Set<String> permissionCheckSigs;
	private final boolean enableDebug;
	private final boolean forceDebugToConsole;
	private final ILogger mainLogger;
	private final IACMinerDataAccessor dataAccessor;
	private final String name;
	private final IACMinerDatabase database;
	private final Set<EntryPoint> eps;
	
	public ACMinerRunner(IACMinerDataAccessor dataAccessor, IPhaseHandler handler, IACMinerDatabase database, Set<EntryPoint> eps, ILogger mainLogger) {
		this.handler = handler;
		this.enableDebug = isOptionEnabled(ACMinerHandler.optEnableDebug);
		this.forceDebugToConsole = isOptionEnabled(ACMinerHandler.optDebugToConsole);
		this.dataAccessor = dataAccessor;
		this.mainLogger = mainLogger;
		this.name = getClass().getSimpleName();
		this.database = database;
		this.toRemoveSigs = resolveAllPossibleSigs(toRemove,dataAccessor);
		this.permissionCheckSigs = resolveAllPossibleSigs(permCheckSigs,dataAccessor);
		this.eps = eps;
	}
	
	private boolean isOptionEnabled(String name) {
		IPhaseOption<?> o = handler.getPhaseOptionUnchecked(name);
		if(o == null || !o.isEnabled())
			return false;
		return true;
	}
	
	private static Set<String> resolveAllPossibleSigs(Set<String> in, IACMinerDataAccessor dataAccessor) {
		Set<String> ret = new HashSet<>();
		for(String s : in) {
			String cname = Scene.v().signatureToClass(s);
	        String mname = Scene.v().signatureToSubsignature(s);
	        if(Scene.v().containsClass(cname)) {
	        	SootClass c = Scene.v().getSootClass(cname);
	        	Set<String> temp = HierarchyHelpers.getAllPossibleInvokeSignaturesForMethod(c,mname,dataAccessor);
	        	if(temp == null || temp.isEmpty())
	        		ret.add(s);
	        	else
	        		ret.addAll(temp);
	        }
		}
		return ret;
	}
	
	//TODO update the output directories so everything is going into the one in input
	public boolean run() {
		boolean successOuter = true;
		
		mainLogger.info("{}: Begin simple miner runner.",name);
		
		Path debugDir = dataAccessor.getConfig().getFilePath("debug-dir");
		try {
			FileHelpers.processDirectory(debugDir, true, false);
		} catch(Throwable t) {
			mainLogger.fatal("{}: Failed to create debug directory '{}'. Stoping analysis!",name,
					debugDir);
			successOuter = false;
		}
		
		Path dumpDir = dataAccessor.getConfig().getFilePath("debug_acminer-dump-dir");
		try {
			FileHelpers.processDirectory(dumpDir, true, false);
		} catch(Throwable t) {
			mainLogger.fatal("{}: Failed to create acminer dump directory '{}'. Stoping analysis!",name,
					dumpDir);
			successOuter = false;
		}
		
		Path acminerDir = dataAccessor.getConfig().getFilePath("acminer_acminer-dir");
		try {
			FileHelpers.processDirectory(acminerDir, true, true);
		} catch(Throwable t) {
			mainLogger.fatal("{}: Failed to create acminer db directory '{}'. Stoping analysis!",name,
					acminerDir);
			successOuter = false;
		}
		
		Path graphDir = dataAccessor.getConfig().getFilePath("debug_acminer-graph-dir");
		try {
			FileHelpers.processDirectory(graphDir, true, false);
		} catch(Throwable t) {
			mainLogger.fatal("{}: Failed to create acminer graph directory '{}'. Stoping analysis!",name,
					graphDir);
			successOuter = false;
		}
		
		if(enableDebug){
			Path acminerDebug = dataAccessor.getConfig().getFilePath("debug_acminer-dir");
			try{
				FileHelpers.processDirectory(acminerDebug, true, false);
			} catch(Throwable t){
				mainLogger.fatal("{}: Failed to process acminer debug directory "
						+ "'{}'. All marking will be skipped.",t,name,acminerDebug);
				successOuter = false;
			}
		}
		
		if(successOuter){
			if(!runMutipleAtATime())
				successOuter = false;
		}
		
		if(!successOuter){
			mainLogger.fatal("{}: The simple miner runner failed for one or more entry points. Stopping Analysis!",name);
			return false;
		}else{
			mainLogger.info("{}: The simple miner runner succeeded!",name);
			return true;
		}
	}
	
	private boolean runMutipleAtATime() {
		boolean successOuter = true;
		DebugLogger.init(enableDebug,forceDebugToConsole,false);
		WorkerCountingThreadExecutor exe = null;
		ACMiner miner = null;
		List<LoggingWorkerGroup> workerGroups = new ArrayList<>();
		try {
			exe = new WorkerCountingThreadExecutor();
			miner = new ACMiner();
			JimpleICFG baseICFG = new JimpleICFG(dataAccessor.getEntryPoints(),false);
			Map<EntryPoint, ValuePairHashSet> subData = getEpsSubData(miner, mainLogger);
			SootClass stub = null;
			LoggingWorkerGroup curWorkerGroup = null;
			Deque<EntryPoint> eps = new ArrayDeque<>(sortbycps(this.eps));
			while(!eps.isEmpty()) {
				EntryPoint ep = eps.poll();
				if(stub == null || !stub.equals(ep.getStub())) {
					stub = ep.getStub();
					if(curWorkerGroup != null) {
						curWorkerGroup.unlockInitialLock();
						curWorkerGroup = null;
					}
					ACMinerRunnableGroup g = new ACMinerRunnableGroup(name,stub.toString(),false, 
							dataAccessor.getConfig().getFilePath("debug_acminer-dump-dir"), stub, baseICFG);
					if(g.getLogger() == null) {
						mainLogger.fatal("{}: Failed to initilize local logger for '{}'. Skipping analysis of '{}'.",name,stub,stub);
						successOuter = false;
					} else {
						curWorkerGroup = g;
						workerGroups.add(g);
					}	
				}
				if(curWorkerGroup != null){
					Runnable runner = new ACMinerRunnable(ep,miner,subData,baseICFG,curWorkerGroup.getLogger());
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
		} catch(IgnorableRuntimeException t) {
			successOuter = false;
		} catch(Throwable t) {
			mainLogger.fatal("{}: An unexpected exception occured in the simple miner runner.",t,name);
			successOuter = false;
		} finally {
			//Shutdown the executors
			if(exe != null && !exe.shutdownWhenFinished()){
				mainLogger.fatal(CountingThreadExecutor.computeJointErrorMsg(exe.getAndClearExceptions(), 
						"Failed to wait for the executor to terminate.", name));
				successOuter = false;
			}
			if(miner != null && !miner.shutdownWhenFinished()) {
				mainLogger.fatal(CountingThreadExecutor.computeJointErrorMsg(miner.getAndClearExceptions(), 
						"Failed to wait for the simple miner to terminate.",name));
				successOuter = false;
			}
			
			for(LoggingWorkerGroup g : workerGroups) {
				if(g.shutdownNormally() && !g.hasExceptions()) {
					mainLogger.info("{}: Successfully completed the simple miner for Stub '{}'.",name,g.getName());
				} else {
					mainLogger.fatal("{}: Failed to complete the simple miner for Stub '{}'.",name,g.getName());
					successOuter = false;
				}
			}
			
			for(int i = 0; i < 5; i++){
				try {
					DebugLogger.end();//blocks unit all debugging io has finished and thread exits
					DebugLogger.removeEmptyDirs(dataAccessor.getConfig().getFilePath("debug_acminer-dir"),mainLogger);
					break;
				} catch (InterruptedException e1) {
					mainLogger.fatal("{}: Failed to end PMinerDebugLogger. Attempt {}.",name,e1,i);
				}
			}
		}
		return successOuter;
	}
	
	private class ACMinerRunnableGroup extends LoggingWorkerGroup {
		private volatile Path rootOutDir;
		private volatile SootClass stub;
		private volatile JimpleICFG baseICFG;
		private volatile Map<StartNode, Set<String>> graphData;
		public ACMinerRunnableGroup(String phaseName, String name, boolean shutdownOnError, Path rootOutDir, SootClass stub, JimpleICFG baseICFG) {
			super(phaseName, name, shutdownOnError);
			this.graphData = new HashMap<>();
			this.rootOutDir = rootOutDir;
			this.stub = stub;
			this.baseICFG = baseICFG;
		}
		@Override
		protected void endWorker(Worker w) {
			ACMinerRunnable runner = (ACMinerRunnable)w.getRunner();
			if(!isShutdown) {
				EntryPoint ep = runner.getEntryPoint();
				database.addData(ep.getStub(), ep.getEntryPoint(), runner.getResultsSubed());
				//Dump the def use graph for every node for each entry point for debugging purposes
				DefUseGraph graph = runner.getDefUseGraph();
				graph.resolveStartNodeToDefStrings();
				synchronized(graphData) {
					for(StartNode sn : graph.getStartNodes()) {
						Set<String> cur = graphData.get(sn);
						if(cur == null) {
							cur = new HashSet<>();
							graphData.put(sn, cur);
						}
						cur.addAll(graph.getDefStrings(sn));
					}
				}
				graph.clearStartNodesToDefStrings();
			}
			runner.clearData();
			super.endWorker(w);
		}
		@Override
		protected void endGroup() {
			try {
				ACMinerDataWriter.writeData(stub, database, graphData, rootOutDir, baseICFG, dataAccessor);
				this.rootOutDir = null;
				this.stub = null;
				this.baseICFG = null;
				this.graphData = null;
			} catch(Throwable t) {
				logger.fatal("{}: Fatal an unexpected error occured when writing data for stub '{}' of group '{}'.",t,phaseName,stub,name);
				throw new IgnorableRuntimeException();
			}
			super.endGroup();
		}
	}
	
	/* 
	 * For each start node that contains a method call that ultimately leads to the final checkUidPermission in the package manager:
	 *  1) loop through the list of all possible values for the permission check's strings and determine the source method and unit of the string
	 *  2) If the string is being used in one of the methods that ultimately lead to the final checkUidPermission then this method and source unit
	 *     are recorded in the source information
	 *  3) If they are not then we track the use of the string forward until it gets used in a method call that ultimately leads to the final checkUidPermission
	 *  
	 * Other idea, start at the top for the ep and look for the method calls that ultimately leads to the final checkUidPermission that are closest to the entry point
	 * on there specific call graph (i.e. if we encounter on of these calls, mark it and do no explore it's outgoing nodes
	 * Then for each, generate a start node to get all the possible permission string values and try to match those to the string values for each start node, if found 
	 * then add it to the sources list of all the valuepairs for that startnode
	 * 
	 * Or first part of the other idea to get our list of outer most check permission calls
	 * Then loop through the list of possible values for the check permission function and see if any are passed to these outer most calls
	 */
	private Pair<Map<StartNode, List<ValuePair>>, Map<SootMethod, Set<Unit>>> fixPermissionCheckSources(EntryPoint ep, JimpleICFG baseICFG, 
			ACMiner miner, Map<StartNode, List<ValuePair>> in, DefUseGraph inGraph, ILogger logger) {
		logger.fineInfo("{}: Begin fixing permission check sources for entry point '{}'",name,ep);
		
		//Test to see if any of our value pairs perform a permission check with a method matching at least on of the given sigs
		//If they do remove it from our input and add to a mapping of just start nodes to permission checks
		Map<StartNode,List<ValuePair>> inPermChecks = new HashMap<>();
		Map<StartNode, List<ValuePair>> out = new HashMap<>();
		for(StartNode sn : in.keySet()) {
			for(ValuePair vp : in.get(sn)) {
				if((vp.size() == 1 && isPermissionCheckSig(vp.getOp1())) || 
						(vp.size() == 2 && (isPermissionCheckSig(vp.getOp1()) || isPermissionCheckSig(vp.getOp2())))) {
					List<ValuePair> temp = inPermChecks.get(sn);
					if(temp == null) {
						temp = new ArrayList<>();
						inPermChecks.put(sn, temp);
					}
					temp.add(vp);
				} else {
					List<ValuePair> temp = out.get(sn);
					if(temp == null) {
						temp = new ArrayList<>();
						out.put(sn, temp);
					}
					temp.add(vp);
				}
			}
		}
		
		//No permission checks so no point in continuing with this process
		//Note the input data was not modified if there were no permission checks
		if(inPermChecks.isEmpty()) {
			logger.fineInfo("{}: No permission checks detected for entry point '{}'",name,ep);
			return new Pair<Map<StartNode,List<ValuePair>>, Map<SootMethod,Set<Unit>>>(in, Collections.emptyMap());
		}
		
		StringBuilder sb = new StringBuilder();
		for(StartNode sn : inPermChecks.keySet()) {
			sb.append("StartNode: Stmt - ").append(sn.toString()).append("  Source - ").append(sn.getSource()).append("\n");
			for(ValuePair vp : inPermChecks.get(sn)) {
				sb.append("  ValuePair: ").append(vp.toString()).append("\n");
				Map<SootMethod, Map<String,Unit>> sources = vp.getSources();
				for(SootMethod sm : sources.keySet()) {
					for(String s : sources.get(sm).keySet()) {
						sb.append("    Location: Stmt - ").append(s).append("  Source - ").append(sm).append("\n");
					}
				}
			}
		}
		logger.fineInfo("{}: Input permission checks for entry point '{}':\n{}",name,ep,sb.toString());
		
		//Find the edges in the call graph that lead to top permission checks
		CallGraph callGraph = baseICFG.getCallGraph();
		IExcludeHandler excludeHandler = dataAccessor.getExcludedElementsDB().createNewExcludeHandler(ep);
		ExcludingEdgePredicate pred = new ExcludingEdgePredicate(callGraph, excludeHandler);
		IJimpleICFG icfg = new ExcludingJimpleICFG(ep, baseICFG, pred);
		ArrayDeque<SootMethod> queue = new ArrayDeque<>();
		Set<SootMethod> seen = new HashSet<>();
		Map<SootMethod, Map<Unit,Set<SootMethod>>> topPermissionCheckSourceToTargets = new HashMap<>();
		queue.push(ep.getEntryPoint());
		while(!queue.isEmpty()) {
			SootMethod cur = queue.pop();
			Iterator<Edge> it = callGraph.edgesOutOf(cur);
			while(it.hasNext()) {
				Edge e = it.next();
				if(pred.want(e)) {
					SootMethod tgt = e.tgt();
					//Get rid of target methods that are excluded but not the override excluded methods
					//This way we get rid of things like test permission checks while keeping the permission checks
					//with no body because the method call is an entry point to another service
					if(!excludeHandler.isExcludedMethod(tgt)) {
						if(permissionCheckSigs.contains(tgt.toString())) {
							Map<Unit,Set<SootMethod>> unitsToTargets = topPermissionCheckSourceToTargets.get(e.src());
							if(unitsToTargets == null) {
								unitsToTargets = new HashMap<>();
								topPermissionCheckSourceToTargets.put(e.src(), unitsToTargets);
							}
							Set<SootMethod> targets = unitsToTargets.get(e.srcUnit());
							if(targets == null) {
								targets = new HashSet<>();
								unitsToTargets.put(e.srcUnit(), targets);
							}
							targets.add(e.tgt());
						} else if(seen.add(tgt)) {
							queue.push(tgt);
						}
					}
				}
			}
		}
		
		sb = new StringBuilder();
		for(SootMethod src : topPermissionCheckSourceToTargets.keySet()) {
			sb.append("  Source: ").append(src.toString()).append("\n");
			Map<Unit,Set<SootMethod>> unitsToTargets = topPermissionCheckSourceToTargets.get(src);
			for(Unit srcUnit : unitsToTargets.keySet()) {
				sb.append("    Stmt: ").append(srcUnit.toString()).append("\n");
				for(SootMethod tgt : unitsToTargets.get(srcUnit)) {
					sb.append("      Target: ").append(tgt.toString()).append("\n");
				}
			}
		}
		logger.fineInfo("{}: Found the following top permission checks edges for entry point '{}':\n{}",name,ep,sb.toString());
		
		//from the top permission check edges, determine what inner permission checks are reachable from these top permission checks
		//i.e. figure out what permission checks are contained within the subgraph of the top permission check
		//including the calling statement of the top permission checks
		Set<StartNode> markedChecksAdded = new HashSet<>();
		Map<SootMethod, Map<Unit, Set<StartNode>>> topPermCheckCallsToMarkedChecks = new HashMap<>();
		for(SootMethod srcMethod : topPermissionCheckSourceToTargets.keySet()) {
			Map<Unit,Set<SootMethod>> unitsToTargets = topPermissionCheckSourceToTargets.get(srcMethod);
			for(Unit srcUnit : unitsToTargets.keySet()) {
				boolean found = false;
				
				//Determine if the top permission check call site represents a entry in our list of permission checks
				for(StartNode sn : inPermChecks.keySet()) {
					if(srcMethod.equals(sn.getSource())) {
						ArrayDeque<INode> toVisit = new ArrayDeque<>();
						Set<INode> visited = new HashSet<>();
						toVisit.add(sn);
						while(!toVisit.isEmpty()) {
							INode cur = toVisit.poll();
							if(visited.add(cur)) {
								if(cur.getUnit().equals(srcUnit)) {
									Map<Unit, Set<StartNode>> unitsToMarkedChecks = topPermCheckCallsToMarkedChecks.get(srcMethod);
									if(unitsToMarkedChecks == null) {
										unitsToMarkedChecks = new HashMap<>();
										topPermCheckCallsToMarkedChecks.put(srcMethod, unitsToMarkedChecks);
									}
									Set<StartNode> nodes = unitsToMarkedChecks.get(srcUnit);
									if(nodes == null) {
										nodes = new HashSet<>();
										unitsToMarkedChecks.put(srcUnit, nodes);
									}
									nodes.add(sn);
									markedChecksAdded.add(sn);
									found = true;
									break;
								}
								Map<LocalWrapper, Set<INode>> map = inGraph.getChildLocalWrappersToChildNodes(cur);
								for(Set<INode> nodes : map.values())
									toVisit.addAll(nodes);
							}
						}
					}
				}
				
				//Determine if any of the marked permission checks are reachable from the sub graph of a top permission check
				for(SootMethod tgt : unitsToTargets.get(srcUnit)) {
					queue = new ArrayDeque<>();
					seen = new HashSet<>();
					queue.push(tgt);
					while(!queue.isEmpty()) {
						SootMethod cur = queue.pop();
						if(seen.add(cur)) {
							for(StartNode sn : inPermChecks.keySet()) {
								if(sn.getSource().equals(cur)) {
									Map<Unit, Set<StartNode>> unitsToMarkedChecks = topPermCheckCallsToMarkedChecks.get(srcMethod);
									if(unitsToMarkedChecks == null) {
										unitsToMarkedChecks = new HashMap<>();
										topPermCheckCallsToMarkedChecks.put(srcMethod, unitsToMarkedChecks);
									}
									Set<StartNode> nodes = unitsToMarkedChecks.get(srcUnit);
									if(nodes == null) {
										nodes = new HashSet<>();
										unitsToMarkedChecks.put(srcUnit, nodes);
									}
									nodes.add(sn);
									markedChecksAdded.add(sn);
									found = true;
								}
							}
							Iterator<Edge> it = callGraph.edgesOutOf(cur);
							while(it.hasNext()) {
								Edge e = it.next();
								if(pred.want(e))
									queue.add(e.tgt());
							}
						}
					}
				}
				
				if(!found) {
					logger.fatal("{}: Failed to find any marked permission checks that are reachable from the top permission check '{}' in '{}' for entry point '{}'",
							name,srcUnit,srcMethod,ep);
					throw new IgnorableRuntimeException();
				}
			}
		}
		
		sb = new StringBuilder();
		for(SootMethod sm : topPermCheckCallsToMarkedChecks.keySet()) {
			sb.append("Source: ").append(sm.toString()).append("\n");
			for(Unit u : topPermCheckCallsToMarkedChecks.get(sm).keySet()) {
				sb.append("  Stmt: ").append(u.toString()).append("\n");
				for(StartNode sn : topPermCheckCallsToMarkedChecks.get(sm).get(u)) {
					sb.append("    PermCheck: Stmt - ").append(sn).append("  Source - ").append(sn.getSource()).append("\n");
				}
			}
		}
		logger.fineInfo("{}: Found the following top permission check relationships for entry point '{}':\n{}",name,ep,sb.toString());
		
		if(!markedChecksAdded.equals(inPermChecks.keySet())) {
			Set<StartNode> temp = new HashSet<>(inPermChecks.keySet());
			temp.removeAll(markedChecksAdded);
			sb = new StringBuilder();
			for(StartNode sn : temp) {
				sb.append("    MissingPermCheck: Stmt - ").append(sn).append("  Source - ").append(sn.getSource()).append("\n");
			}
			logger.fatal("{}: The maping of top permission checks to marked permission checks is missing one or more marked permission check for entry point '{}':\n{}",
					name,ep,sb.toString());
			throw new IgnorableRuntimeException();
		}
		
		//Generate a def use graph to get all possible values for the first argument (i.e. the permission string)
		Set<Unit> seeds = new HashSet<>();
		for(Map<Unit,Set<StartNode>> v : topPermCheckCallsToMarkedChecks.values()) {
			seeds.addAll(v.keySet());
		}
		DefUseGraph topPermCallsGraph = miner.makeDefUseGraph(seeds, ep, icfg, logger);
		
		//Get all top invoking permission check calls to all possible values for the first argument
		Map<SootMethod, Map<Unit, Pair<StartNode,Set<ValuePair>>>> topPermCallsToFirstArgValues = new HashMap<>();
		for(StartNode sn : topPermCallsGraph.getStartNodes()) {
			if(sn instanceof InvokeStartNode) {
				Map<Unit, Pair<StartNode,Set<ValuePair>>> units = topPermCallsToFirstArgValues.get(sn.getSource());
				if(units == null) {
					units = new HashMap<>();
					topPermCallsToFirstArgValues.put(sn.getSource(), units);
				}
				units.put(sn.getUnit(), new Pair<StartNode,Set<ValuePair>>(sn,getValuesForFirstArg((InvokeStartNode)sn,topPermCallsGraph,miner,ep,logger)));
			} else {
				logger.fatal("{}: StartNode '{}' is not a invoke node!?!",name,sn.toString());
				throw new IgnorableRuntimeException();
			}
		}
		
		sb = new StringBuilder();
		for(StartNode sn : topPermCallsGraph.getStartNodes()) {
			sb.append("PermCheck: Stmt - ").append(sn.toString()).append("  Source - ").append(sn.getSource()).append("\n");
			for(ValuePair vp : topPermCallsToFirstArgValues.get(sn.getSource()).get(sn.getUnit()).getSecond()) {
				sb.append("  Value: ").append(vp.toString()).append("\n");
			}
		}
		logger.fineInfo("{}: All possible permission string values for top permission check for entry point '{}':\n{}",name,ep,sb.toString());
		
		//Get the first argument value for each value pair of the marked permission checks
		Map<StartNode, Map<ValuePair, Part>> inPermChecksFirstArg = new HashMap<>();
		for(StartNode sn : inPermChecks.keySet()) {
			for(ValuePair vp : inPermChecks.get(sn)) {
				DataWrapper dw = null;
				if(vp.size() == 1)
					dw = vp.getOp1();
				else if(vp.size() == 2)
					dw = isPermissionCheckSig(vp.getOp1()) ? vp.getOp1() : vp.getOp2();
				boolean foundMethod = false;
				Part firstArg = null;
				for(Part p : dw.getIdentifier()) {
					if(foundMethod && !(p instanceof LiteralPart)) {
						firstArg = p;
						break;
					} else if(p instanceof MethodRefPart) {
						foundMethod = true;
					}
				}
				Map<ValuePair, Part> temp1 = inPermChecksFirstArg.get(sn);
				if(temp1 == null) {
					temp1 = new HashMap<>();
					inPermChecksFirstArg.put(sn, temp1);
				}
				temp1.put(vp, firstArg);
			}
		}
		
		sb = new StringBuilder();
		for(StartNode sn : inPermChecksFirstArg.keySet()) {
			sb.append("PermCheck: Stmt - ").append(sn.toString()).append("  Source - ").append(sn.getSource()).append("\n");
			for(Part vp : inPermChecksFirstArg.get(sn).values()) {
				sb.append("  FirstArgValue: ").append(vp.toString()).append("\n");
			}
		}
		logger.fineInfo("{}: Values assigned to the first argument of each marked permission check for entry point '{}':\n{}",name,ep,sb.toString());
		
		Map<StartNode, Set<ValuePair>> resultsFound = new HashMap<>();
		for(SootMethod sourceMethod : topPermCheckCallsToMarkedChecks.keySet()) {
			Map<Unit, Set<StartNode>> unitsToMarkedChecks = topPermCheckCallsToMarkedChecks.get(sourceMethod);
			for(Unit sourceUnit : unitsToMarkedChecks.keySet()) {
				Set<StartNode> markedChecks = unitsToMarkedChecks.get(sourceUnit);
				Pair<StartNode,Set<ValuePair>> firstValues = topPermCallsToFirstArgValues.get(sourceMethod).get(sourceUnit);
				Set<String> firstValueStrings = new HashSet<>();
				for(ValuePair vp : firstValues.getSecond())
					firstValueStrings.add(vp.getOp1().toString());
				for(StartNode sn : markedChecks) {
					Map<ValuePair, Part> vpToFirstArg = inPermChecksFirstArg.get(sn);
					for(ValuePair vp : vpToFirstArg.keySet()) {
						Part p = vpToFirstArg.get(vp);
						boolean found = false;
						if(firstValueStrings.contains(p.toString())) {
							List<ValuePair> vps = inPermChecks.get(sn);
							for(int i = 0; i < vps.size(); i++) {
								if(vps.get(i).equals(vp)) {
									vps.set(i, ValuePair.make(vp,Collections.singletonMap(sourceMethod, Collections.singletonMap(firstValues.getFirst().toString(),sourceUnit))));
									found = true;
								}
							}
						}
						if(found) {
							Set<ValuePair> temp = resultsFound.get(sn);
							if(temp == null) {
								temp = new ValuePairHashSet();
								resultsFound.put(sn, temp);
							}
							temp.add(vp);
						}
					}
				}
			}
		}
		
		sb = new StringBuilder();
		for(StartNode sn : inPermChecks.keySet()) {
			Set<ValuePair> results = resultsFound.get(sn);
			if(results == null) {
				sb.append("  PermCheck: Stmt - ").append(sn).append("  Source - ").append(sn.getSource()).append("\n");
				for(ValuePair vp : inPermChecks.get(sn)) {
					sb.append("    ValuePair: ").append(vp.toString()).append("\n");
				}
			} else {
				Set<ValuePair> notFound = new ValuePairHashSet();
				for(ValuePair vp : inPermChecks.get(sn)) {
					if(!results.contains(vp))
						notFound.add(vp);
				}
				if(!notFound.isEmpty()) {
					sb.append("  PermCheck: Stmt - ").append(sn).append("  Source - ").append(sn.getSource()).append("\n");
					for(ValuePair vp : notFound) {
						sb.append("    ValuePair: ").append(vp.toString()).append("\n");
					}
				}
			}
		}
		if(sb.length() != 0) {
			logger.fatal("{}: Failed to find source locations for one or more of the marked permission checks '{}':\n{}",name,ep,sb.toString());
			throw new IgnorableRuntimeException();
		}
		
		sb = new StringBuilder();
		for(StartNode sn : inPermChecks.keySet()) {
			sb.append("StartNode: Stmt - ").append(sn.toString()).append("  Source - ").append(sn.getSource()).append("\n");
			for(ValuePair vp : inPermChecks.get(sn)) {
				sb.append("  ValuePair: ").append(vp.toString()).append("\n");
				Map<SootMethod, Map<String,Unit>> sources = vp.getSources();
				for(SootMethod sm : sources.keySet()) {
					for(String s : sources.get(sm).keySet()) {
						sb.append("    Location: Stmt - ").append(s).append("  Source - ").append(sm).append("\n");
					}
				}
			}
		}
		logger.fineInfo("{}: Output permission checks for entry point '{}':\n{}",name,ep,sb.toString());
		
		for(StartNode sn : inPermChecks.keySet()) {
			List<ValuePair> temp = out.get(sn);
			if(temp == null) {
				temp = new ArrayList<>();
				out.put(sn, temp);
			}
			temp.addAll(inPermChecks.get(sn));
		}
		
		Map<SootMethod, Set<Unit>> top = new HashMap<>();
		for(SootMethod sm : topPermissionCheckSourceToTargets.keySet()) {
			top.put(sm, topPermissionCheckSourceToTargets.get(sm).keySet());
		}
		
		Pair<Map<StartNode, List<ValuePair>>, Map<SootMethod, Set<Unit>>> ret = 
				new Pair<Map<StartNode,List<ValuePair>>, Map<SootMethod,Set<Unit>>>(out, top);
		
		return ret;
	}
	
	//Note we are assuming the first argument of a permission check is always the permission string
	//and that this permission string is always passed down starting from the top most permission check
	//call, if either of these two asumptions ever change then things will need to be updated
	private Set<ValuePair> getValuesForFirstArg(IInvokeNode node, DefUseGraph graph, ACMiner miner, EntryPoint ep, ILogger logger) {
		Value firstArg = ((Stmt)node.getUnit()).getInvokeExpr().getArg(0);
		Set<INode> values = null;
		if(firstArg instanceof Local) {
			values = graph.getChildLocalsToChildNodes(node).get(firstArg);
			if(values == null) {
				logger.fatal("{}: Could not find local '{}' in the child nodes map for node '{}'.",name,firstArg,node.toString());
				throw new IgnorableRuntimeException();
			}
		} else { // args of a invoke can only be a constant or a local
			for(InlineConstantLeafNode clw : graph.getInlineConstantNodes(node).values()) {
				if(clw.getValue().equals(firstArg)) {
					values = Collections.singleton(clw);
					break;
				}
			}
			if(values == null) {
				logger.fatal("{}: Could not find constant '{}' in the list of inline constant nodes for node '{}'.",name,firstArg,node.toString());
				throw new IgnorableRuntimeException();
			}
		}
		Map<INode,List<ValuePair>> data = miner.mineAdditionalData(ep, graph, values, dataAccessor, logger);
		Set<ValuePair> ret = new ValuePairHashSet();
		for(List<ValuePair> l : data.values()) {
			ret.addAll(l);
		}
		return ret;
	}
	
	private boolean isPermissionCheckSig(DataWrapper in) {
		for(Part p : in.getIdentifier()) {
			if(p instanceof MethodRefPart) {
				if(permissionCheckSigs.contains(((MethodRefPart)p).getMethodRef().getSignature()))
					return true;
				return false;
			}
		}
		return false;
	}
	
	private class ACMinerRunnable implements Runnable {
		private volatile EntryPoint ep;
		private volatile EntryPoint entryPoint;
		private volatile ILogger logger;
		private volatile ACMiner miner;
		private volatile Map<StartNode,List<ValuePair>> results;
		private volatile ValuePairHashSet resultsSubed;
		private volatile DefUseGraph graph;
		private volatile Map<EntryPoint, ValuePairHashSet> subData;
		private volatile JimpleICFG baseICFG;
		public ACMinerRunnable(EntryPoint ep, ACMiner miner, Map<EntryPoint, ValuePairHashSet> subData, 
				JimpleICFG baseICFG, ILogger logger) {
			this.ep = ep;
			this.entryPoint = ep;
			this.logger = logger;
			this.miner = miner;
			this.results = null;
			this.resultsSubed = null;
			this.graph = null;
			this.subData = subData;
			this.baseICFG = baseICFG;
		}
		@Override
		public void run() {
			logger.fineInfo("{}: Begin for ep '{}'.",name,ep);
			
			IDebugLogger debugLogger = DebugLogger.getNewDebugLogger(ep.getStub(), ep.getEntryPoint(), 
					dataAccessor.getConfig().getFilePath("debug_acminer-dir"));
			if(debugLogger == null){
				logger.fatal("{}: Failed to start the debug logger for ep '{}'.",name,ep);
				throw new IgnorableRuntimeException();
			}
			
			try {
				graph = dataAccessor.getDefUseGraphModDB().getDefUseGraph(ep, dataAccessor.getConfig().getFilePath("acminer_defusegraphmod-dir"));
				results = miner.mineData(ep, graph, dataAccessor, debugLogger);
				
				Pair<Map<StartNode, List<ValuePair>>, Map<SootMethod, Set<Unit>>> tempData = 
						fixPermissionCheckSources(entryPoint, baseICFG, miner, results, graph, debugLogger);
				results = tempData.getFirst();
				
				ValuePairHashSet in = new ValuePairHashSet();
				for(List<ValuePair> l : results.values())
					in.addAll(l);
				
				//Removes expressions that explode because of argument combinations
				resultsSubed = simplifyExplodedExpressions(in);
				//substitutes pre-mined data for select entry points that represent context queries
				//with the authorization checks that comprise the entry point
				//emulates the ability to mine across entry points for a select number of entry points
				resultsSubed = subInEntryPointData(resultsSubed, subData);
				//removes generic context queries where the calls themselves don't add any context to the logic
				//as the logic inside the calls is already captured
				resultsSubed = removeRedundentContextQueries(resultsSubed);
				//transforms some common context queries that return integers that might as well be boolean checks
				//into a boolean check format
				resultsSubed = simplifyExpressions(resultsSubed);
				//redo some simplification procedures because of the other changes listed above
				resultsSubed = new ValuePairHashSet(ACMiner.simplifyPairs(resultsSubed));
				//For each permission check value pair, remove all sources that are not in our list of top permission
				//check locations. Effectively, this is taking the current source data and the top permission check 
				//locations and only keeping those entries in the sources that are also in the top permission check
				//locations. Of course if the result is the empty set then no modifications are made and the current
				//value pair is returned.
				resultsSubed = finalizeSources(resultsSubed, tempData.getSecond());
				
				logger.fineInfo("{}: Succeeded for ep '{}'.",name,ep);
			} catch(IgnorableRuntimeException t) {
				throw t;
			} catch(Throwable t) {
				logger.fatal("{}: An unexpected error occured for ep '{}'.",t,name,ep);
				throw new IgnorableRuntimeException();
			} finally {
				if(debugLogger != null){
					debugLogger.close();
					debugLogger = null;
				}
			}
		}
		public ValuePairHashSet getResultsSubed() {
			return resultsSubed;
		}
		public EntryPoint getEntryPoint() {
			return ep;
		}
		public DefUseGraph getDefUseGraph() {
			return graph;
		}
		public void clearData() {
			results = null;
			resultsSubed = null;
			ep = null;
			logger = null;
			miner = null;
			graph = null;
			subData = null;
		}
	}
	
	private Set<EntryPoint> sortbycps(Set<EntryPoint> in) {
		Map<SootClass,Set<EntryPoint>> stubs = new LinkedHashMap<>();
		for(EntryPoint e : in) {
			Set<EntryPoint> cur = stubs.get(e.getStub());
			if(cur == null) {
				cur = new LinkedHashSet<>();
				stubs.put(e.getStub(), cur);
			}
			cur.add(e);
		}
		
		for(SootClass sc : stubs.keySet()) {
			stubs.put(sc, SortingMethods.sortSet(stubs.get(sc), new Comparator<EntryPoint>() {
				@Override
				public int compare(EntryPoint o1, EntryPoint o2) {
					int ep1s = 0;
					int ep2s = 0;
					ep1s += dataAccessor.getContextQueriesDB().getContextQueries(o1).size();
					ep1s += dataAccessor.getControlPredicatesDB().getUnits(o1).size();
					ep1s += dataAccessor.getEntryPointEdgesDB().getEntryPointEdges(o1).size();
					ep2s += dataAccessor.getContextQueriesDB().getContextQueries(o2).size();
					ep2s += dataAccessor.getControlPredicatesDB().getUnits(o2).size();
					ep2s += dataAccessor.getEntryPointEdgesDB().getEntryPointEdges(o2).size();
					int ret = Integer.compare(ep1s, ep2s);
					if(ret == 0)
						ret = SootSort.smComp.compare(o1.getEntryPoint(), o2.getEntryPoint());
					return ret;
				}
			}));
		}
		
		stubs = SortingMethods.sortMap(stubs, new Comparator<Entry<SootClass, Set<EntryPoint>>>() {
			@Override
			public int compare(Entry<SootClass, Set<EntryPoint>> o1, Entry<SootClass, Set<EntryPoint>> o2) {
				int ret = Integer.compare(o2.getValue().size(), o1.getValue().size());
				if(ret == 0)
					ret = SootSort.scComp.compare(o1.getKey(), o2.getKey());
				return ret;
			}
		});
		
		Set<EntryPoint> ret = new LinkedHashSet<>();
		for(Set<EntryPoint> e : stubs.values()) {
			ret.addAll(e);
		}
		return ret;
	}
	
	private ValuePairHashSet simplifyExpressions(ValuePairHashSet in) {
		ValuePairHashSet ret = new ValuePairHashSet();
		for(ValuePair vp : in) {
			if(vp.size() == 2) {
				DataWrapper op1 = vp.getOp1();
				DataWrapper op2 = vp.getOp2();
				boolean is1 = isMethodToSimplify(op1);
				boolean is2 = isMethodToSimplify(op2);
				if(op1 instanceof PrimitiveConstant && is2)
					ret.add(ValuePair.make(op2,vp));
				else if(is1 && op2 instanceof PrimitiveConstant)
					ret.add(ValuePair.make(op1,vp));
				else
					ret.add(vp);
			} else {
				ret.add(vp);
			}
		}
		return ret;
	}
	
	private boolean isMethodToSimplify(DataWrapper dw) {
		Set<String> sigs = ImmutableSet.of(
			"<com.android.server.pm.PackageManagerService: int checkUidPermission(java.lang.String,int)>",
			"<com.android.server.pm.PackageManagerService: int checkPermission(java.lang.String,java.lang.String,int)>",
			"<android.app.ApplicationPackageManager: int checkSignatures(java.lang.String,java.lang.String)>",
			"<com.android.server.pm.PackageManagerService: int checkSignatures(java.lang.String,java.lang.String)>",
			"<android.app.AppOpsManager: int checkOpNoThrow(int,int,java.lang.String)>",
			"<android.app.AppOpsManager: int noteOpNoThrow(int,int,java.lang.String)>",
			"<android.app.AppOpsManager: int permissionToOpCode(java.lang.String)>",
			"<com.android.server.am.ActivityManagerService$IntentFirewallInterface: int checkComponentPermission(java.lang.String,int,int,int,boolean)>",
			"<com.android.server.pm.PackageManagerService: int checkUidSignatures(int,int)>",
			"<com.android.server.firewall.IntentFirewall$AMSInterface: int checkComponentPermission(java.lang.String,int,int,int,boolean)>",
			"<com.android.server.AppOpsService: int noteOperation(int,int,java.lang.String)>",
			"<com.android.server.AppOpsService: int startOperation(android.os.IBinder,int,int,java.lang.String)>",
			"<com.android.server.AppOpsService: int checkOperation(int,int,java.lang.String)>",
			"<android.content.pm.PackageManager: int checkSignatures(java.lang.String,java.lang.String)>"
		);
		for(Part p : dw.getIdentifier()) {
			if(p instanceof MethodRefPart && sigs.contains(((MethodRefPart)p).getMethodRef().getSignature()))
				return true;
		}
		return false;
	}
	
	//These are assumed to be things that we have already recorded the auth logic of
	//So recording the calls themselves does us no good and generates noise
	private ValuePairHashSet removeRedundentContextQueries(ValuePairHashSet in) {
		for(Iterator<ValuePair> it = in.iterator(); it.hasNext();) {
			ValuePair vp = it.next();
			if(vp.size() == 1) {
				if(isRemoveInvoke(vp.getOp1()))
					it.remove();
			} else if(vp.size() == 2) {
				if(isRemoveInvoke(vp.getOp1()) || isRemoveInvoke(vp.getOp2()))
					it.remove();
			}
		}
		return in;
	}
	
	private boolean isRemoveInvoke(DataWrapper in) {
		for(Part p : in.getIdentifier()) {
			if(p instanceof MethodRefPart) {
				if(toRemoveSigs.contains(((MethodRefPart)p).getMethodRef().getSignature()))
					return true;
				return false;
			}
		}
		return false;
	}
	
	private Map<EntryPoint, ValuePairHashSet> getEpsSubData(ACMiner miner, ILogger logger) {
		//These don't call eachother if they do more code needs to be added
		Set<String> epsSubs = ImmutableSet.of(
				"<com.android.server.am.ActivityManagerService: int checkPermission(java.lang.String,int,int)>",
				"<com.android.server.am.ActivityManagerService: int checkPermissionWithToken(java.lang.String,int,int,android.os.IBinder)>",
				"<com.android.server.am.ActivityManagerService: int checkGrantUriPermission(int,java.lang.String,android.net.Uri,int,int)>",
				"<com.android.server.am.ActivityManagerService: int checkUriPermission(android.net.Uri,int,int,int,int,android.os.IBinder)>"
		);
		
		logger.info("{}: Collecting entry point substitution data.",name);
		try {
			Map<EntryPoint, ValuePairHashSet> ret = new HashMap<>();
			for(EntryPoint ep : dataAccessor.getEntryPoints()) {
				if(epsSubs.contains(ep.getEntryPoint().toString())) {
					logger.info("{}: Begin sub data collection for ep '{}'.",name,ep);
					IDebugLogger debugLogger = DebugLogger.getNewDebugLogger(
							ep.getStub(), ep.getEntryPoint(), dataAccessor.getConfig().getFilePath("debug_acminer-dir"));
					if(debugLogger == null){
						logger.fatal("{}: Failed to start the debug logger for ep '{}'.",name,ep);
						throw new IgnorableRuntimeException();
					}
					try {
						DefUseGraph graph = dataAccessor.getDefUseGraphModDB().getDefUseGraph(
								ep, dataAccessor.getConfig().getFilePath("acminer_defusegraphmod-dir"));
						Map<StartNode, List<ValuePair>> data = miner.mineData(ep, graph, true, dataAccessor, debugLogger);
						ValuePairHashSet temp = new ValuePairHashSet();
						for(List<ValuePair> l : data.values())
							temp.addAll(l);
						if(temp.isEmpty())
							temp = ValuePairHashSet.getEmptySet();
						ret.put(ep, temp);
						logger.info("{}: Sub data collection succeeded for ep '{}'.",name,ep);
					} catch(IgnorableRuntimeException t) {
						throw t;
					} catch(Throwable t) {
						logger.fatal("{}: An unexpected error occured in sub data collection for ep '{}'.",t,name,ep);
						throw new IgnorableRuntimeException();
					} finally {
						if(debugLogger != null){
							debugLogger.close();
							debugLogger = null;
						}
					}
				}
			}
			logger.info("{}: Succeded in collecting entry point substitution data.",name);
			if(ret.isEmpty())
				ret = Collections.emptyMap();
			return ret;
		} catch(IgnorableRuntimeException t) {
			throw t;
		} catch(Throwable t) {
			logger.fatal("{}: An unexpected error occured in collecting entry point substitution data",t);
			throw new IgnorableRuntimeException();
		}
	}
	
	private ValuePairHashSet subInEntryPointData(ValuePairHashSet in, Map<EntryPoint, ValuePairHashSet> subData) {
		ValuePairHashSet ret = new ValuePairHashSet();
		for(ValuePair vp : in) {
			if(vp.size() == 1) {
				DataWrapper op = vp.getOp1();
				List<Quad<Integer,DataWrapperPart,List<Part>,EntryPoint>> invokes = getSubDataInvokeingParts(op, subData);
				if(!invokes.isEmpty()) {
					boolean isRoot = false;
					for(Quad<Integer,DataWrapperPart,List<Part>,EntryPoint> q : invokes) {
						if(q.getFirst() == 0)
							isRoot = true;
						for(ValuePair subbedVp : performSub(q.getThird(), subData.get(q.getFourth()))) {
							ret.add(ValuePair.make(subbedVp,vp.getSources()));
						}
					}
					//Preserve the original because the invoke expressions were not the starting DataWrapper
					if(!isRoot)
						ret.add(vp);
				} else {
					ret.add(vp);
				}
			} else {
				DataWrapper op1 = vp.getOp1();
				DataWrapper op2 = vp.getOp2();
				List<Quad<Integer,DataWrapperPart,List<Part>,EntryPoint>> invokes1 = getSubDataInvokeingParts(op1, subData);
				List<Quad<Integer,DataWrapperPart,List<Part>,EntryPoint>> invokes2 = getSubDataInvokeingParts(op2, subData);
				if(invokes1.isEmpty() && invokes2.isEmpty()) {
					ret.add(vp);
				} else if(invokes1.isEmpty()) {
					boolean isRoot = false;
					for(Quad<Integer,DataWrapperPart,List<Part>,EntryPoint> q : invokes2) {
						if(q.getFirst() == 0)
							isRoot = true;
						for(ValuePair subbedVp : performSub(q.getThird(), subData.get(q.getFourth()))) {
							ret.add(ValuePair.make(subbedVp,vp.getSources()));
						}
					}
					//Preserve the original because the invoke expressions were not the starting DataWrapper
					if(!isRoot)
						ret.add(vp);
				} else if(invokes2.isEmpty()) {
					boolean isRoot = false;
					for(Quad<Integer,DataWrapperPart,List<Part>,EntryPoint> q : invokes1) {
						if(q.getFirst() == 0)
							isRoot = true;
						for(ValuePair subbedVp : performSub(q.getThird(), subData.get(q.getFourth()))) {
							ret.add(ValuePair.make(subbedVp,vp.getSources()));
						}
					}
					//Preserve the original because the invoke expressions were not the starting DataWrapper
					if(!isRoot)
						ret.add(vp);
				} else {
					boolean isRoot1 = false;
					for(Quad<Integer,DataWrapperPart,List<Part>,EntryPoint> q : invokes1) {
						if(q.getFirst() == 0)
							isRoot1 = true;
						for(ValuePair subbedVp : performSub(q.getThird(), subData.get(q.getFourth()))) {
							ret.add(ValuePair.make(subbedVp,vp.getSources()));
						}
					}
					boolean isRoot2 = false;
					for(Quad<Integer,DataWrapperPart,List<Part>,EntryPoint> q : invokes2) {
						if(q.getFirst() == 0)
							isRoot2 = true;
						for(ValuePair subbedVp : performSub(q.getThird(), subData.get(q.getFourth()))) {
							ret.add(ValuePair.make(subbedVp,vp.getSources()));
						}
					}
					//Preserve the original because the invoke expressions were not the starting DataWrapper
					if(!(isRoot1 && isRoot2))
						ret.add(vp);
				}
			}
		}
		if(ret.isEmpty())
			return ValuePairHashSet.getEmptySet();
		return ret;
	}
	
	private ValuePairHashSet performSub(List<Part> parts, ValuePairHashSet pairs) {
		if(parts.size() > 1) {
			parts.remove(0);
			ValuePairHashSet ret = new ValuePairHashSet();
			Map<Integer,List<ValuePair>> sortedPairs = findParmRefs(pairs);
			for(int i : sortedPairs.keySet()) {
				if(i == 0) {
					ret.addAll(sortedPairs.get(i));
				} else if(i == 1) {
					for(ValuePair pair : sortedPairs.get(i)) {
						ret.add(ValuePair.make(replaceParts(parts,pair.getOp1()),pair.getOp2(),pair));
					}
				} else if(i == 2) {
					for(ValuePair pair : sortedPairs.get(i)) {
						ret.add(ValuePair.make(pair.getOp1(),replaceParts(parts,pair.getOp2()),pair));
					}
				} else if(i == 3) {
					for(ValuePair pair : sortedPairs.get(i)) {
						ret.add(ValuePair.make(replaceParts(parts,pair.getOp1()),replaceParts(parts,pair.getOp2()),pair));
					}
				}
			}
			return ret;
		}
		return pairs;
	}
	
	private DataWrapper replaceParts(List<Part> parts, DataWrapper in) {
		DataWrapper out = in.clone();
		Deque<DataWrapper> stack = new ArrayDeque<>();
		stack.push(out);
		while(!stack.isEmpty()) {
			DataWrapper cur = stack.pop();
			for(int i = 0; i < cur.getIdentifier().size(); i++) {
				Part p = cur.getIdentifier().get(i);
				if(p instanceof IdentityRefPart && ((IdentityRefPart)p).isParameterRef()) {
					ParameterRef pr = ((IdentityRefPart)p).getParameterRef();
					cur.getIdentifier().set(i, parts.get(pr.getIndex()));
				} else if(p instanceof DataWrapperPart) {
					stack.push(((DataWrapperPart)p).getDataWrapper());
				}
			}
		}
		if(out.toString().equals(AllConstant.val))
			return new AllConstant();
		else if(out.toString().equals(NoneConstant.val))
			return new NoneConstant();
		else
			return out;
	}
	
	private Map<Integer,List<ValuePair>> findParmRefs(ValuePairHashSet pairs) {
		Map<Integer, List<ValuePair>> ret = new HashMap<>();
		ret.put(0, new ArrayList<ValuePair>());
		ret.put(1, new ArrayList<ValuePair>());
		ret.put(2, new ArrayList<ValuePair>());
		ret.put(3, new ArrayList<ValuePair>());
		for(ValuePair pair : pairs) {
			if(pair.size() == 1) {
				DataWrapper op = pair.getOp1();
				if(containsParmRef(op))
					ret.get(1).add(pair);
				else
					ret.get(0).add(pair);
			} else {
				DataWrapper op1 = pair.getOp1();
				DataWrapper op2 = pair.getOp2();
				boolean has1 = containsParmRef(op1);
				boolean has2 = containsParmRef(op2);
				if(has1 && has2)
					ret.get(3).add(pair);
				else if(has1)
					ret.get(1).add(pair);
				else if(has2)
					ret.get(2).add(pair);
				else
					ret.get(0).add(pair);
			}
		}
		if(ret.get(0).isEmpty())
			ret.remove(0);
		if(ret.get(1).isEmpty())
			ret.remove(1);
		if(ret.get(2).isEmpty())
			ret.remove(2);
		if(ret.get(3).isEmpty())
			ret.remove(3);
		if(ret.isEmpty())
			return Collections.emptyMap();
		return ret;
	}
	
	private boolean containsParmRef(DataWrapper op) {
		Deque<DataWrapper> stack = new ArrayDeque<>();
		stack.push(op);
		while(!stack.isEmpty()) {
			DataWrapper cur = stack.pop();
			for(Part p : cur.getIdentifier()) {
				if(p instanceof IdentityRefPart && ((IdentityRefPart)p).isParameterRef())
					return true;
				else if(p instanceof DataWrapperPart)
					stack.push(((DataWrapperPart)p).getDataWrapper());
			}
		}
		return false;
	}
	
	private List<Quad<Integer,DataWrapperPart,List<Part>,EntryPoint>> getSubDataInvokeingParts(DataWrapper op, 
			Map<EntryPoint, ValuePairHashSet> subData) {
		Map<String,EntryPoint> epsSigs = new HashMap<>();
		for(EntryPoint sm : dataAccessor.getEntryPoints()) {
			epsSigs.put(sm.getEntryPoint().toString(),sm);
		}
		List<Triple<Integer,DataWrapperPart,List<Part>>> invokes = findInvokingParts(op);
		List<Quad<Integer,DataWrapperPart,List<Part>,EntryPoint>> ret = new ArrayList<>();
		for(Triple<Integer,DataWrapperPart,List<Part>> t : invokes) {
			MethodRefPart mrp = (MethodRefPart)t.getThird().get(0);
			String methodSig = mrp.getMethodRef().toString();
			Set<EntryPoint> reachableEps;
			if(epsSigs.containsKey(methodSig)) {
				reachableEps = Collections.singleton(epsSigs.get(methodSig));
			} else {
				reachableEps = new HashSet<>();
				Set<SootMethod> temp = dataAccessor.getEntryPointsFromBinderMethod(mrp.getValue());
				if(temp != null) {
					for(EntryPoint ep : dataAccessor.getEntryPoints()) {
						for(SootMethod sm : temp) {
							if(Objects.equals(ep.getEntryPoint(),sm))
								reachableEps.add(ep);
						}
					}
				}
			}
			if(reachableEps != null && !reachableEps.isEmpty()) {
				for(EntryPoint ep : reachableEps) {
					if(subData.containsKey(ep))
						ret.add(new Quad<>(t.getFirst(), t.getSecond(), t.getThird(), ep));
				}
			}
		}
		if(ret.isEmpty())
			ret = Collections.emptyList();
		return ret;
	}
	
	private List<Triple<Integer,DataWrapperPart,List<Part>>> findInvokingParts(DataWrapper op) {
		List<Triple<Integer,DataWrapperPart,List<Part>>> ret = new ArrayList<>();
		Deque<Pair<Integer,DataWrapperPart>> stack = new ArrayDeque<>();
		stack.push(new Pair<Integer,DataWrapperPart>(0, new DataWrapperPart(op)));
		while(!stack.isEmpty()) {
			Pair<Integer, DataWrapperPart> pair = stack.pop();
			DataWrapperPart cur = pair.getSecond();
			int depth = pair.getFirst();
			boolean foundMethodRef = false;
			List<Part> invoke = new ArrayList<>();
			for(Part p : cur.getDataWrapper().getIdentifier()) {
				if(p instanceof MethodRefPart)
					foundMethodRef = true;
				else if(p instanceof DataWrapperPart)
					stack.push(new Pair<Integer, DataWrapperPart>(depth+1,((DataWrapperPart)p)));
				
				if(foundMethodRef && !(p instanceof LiteralPart))
					invoke.add(p);
			}
			if(!invoke.isEmpty())
				ret.add(new Triple<Integer, DataWrapperPart, List<Part>>(depth, cur, invoke));
		}
		if(ret.isEmpty())
			ret = Collections.emptyList();
		return ret;
	}
	
	private ValuePairHashSet simplifyExplodedExpressions(ValuePairHashSet in) {
		ValuePairHashSet ret = new ValuePairHashSet();
		for(ValuePair vp : in) {
			if(vp.size() == 1) {
				DataWrapper op = vp.getOp1();
				Pair<Boolean,DataWrapper> r = simplifyExplodedExpressions(op);
				if(r.getFirst())
					ret.add(ValuePair.make(r.getSecond(),vp));
				else
					ret.add(vp);
			} else {
				DataWrapper op1 = vp.getOp1();
				DataWrapper op2 = vp.getOp2();
				Pair<Boolean,DataWrapper> r1 = simplifyExplodedExpressions(op1);
				Pair<Boolean,DataWrapper> r2 = simplifyExplodedExpressions(op2);
				if(r1.getFirst() || r2.getFirst())
					ret.add(ValuePair.make(r1.getSecond(),r2.getSecond(),vp));
				else
					ret.add(vp);
			}
		}
		if(ret.isEmpty())
			return ValuePairHashSet.getEmptySet();
		return ret;
	}
	
	private Pair<Boolean,DataWrapper> simplifyExplodedExpressions(DataWrapper in) {
		Set<String> sigs = ImmutableSet.of(
			"<com.android.server.am.ActivityManagerService: int checkGrantUriPermissionLocked(int,java.lang.String,com.android.server.am.ActivityManagerService$GrantUri,int,int)>"
		);
		DataWrapper out = in.clone();
		DataWrapperPart allPart = new DataWrapperPart(new AllConstant());
		boolean changeOccured = false;
		Deque<DataWrapper> stack = new ArrayDeque<>();
		stack.push(out);
		while(!stack.isEmpty()) {
			DataWrapper cur = stack.pop();
			boolean match = false;
			for(int i = 0; i < cur.getIdentifier().size(); i++) {
				Part p = cur.getIdentifier().get(i);
				if(p instanceof MethodRefPart && sigs.contains(((MethodRefPart)p).getMethodRef().getSignature())) {
					match = true;
				} else if(p instanceof DataWrapperPart) {
					if(match) {
						cur.getIdentifier().set(i, allPart);
						changeOccured = true;
					} else {
						stack.push(((DataWrapperPart)p).getDataWrapper());
					}
				}
			}
		}
		if(!changeOccured)
			out = in;
		return new Pair<Boolean,DataWrapper>(changeOccured,out);
	}
	
	private ValuePairHashSet finalizeSources(ValuePairHashSet in, Map<SootMethod, Set<Unit>> sources) {
		if(!sources.isEmpty()) {
			ValuePairHashSet ret = new ValuePairHashSet();
			for(ValuePair vp : in) {
				Map<SootMethod, Map<String,Unit>> curSources = vp.getSources();
				Map<SootMethod, Map<String,Unit>> newSources = new HashMap<>();
				for(SootMethod sm : curSources.keySet()) {
					Set<Unit> inUnits = sources.get(sm);
					if(inUnits != null) {
						Map<String,Unit> curUnits = curSources.get(sm);
						for(String stmt : curUnits.keySet()) {
							Unit u = curUnits.get(stmt);
							if(inUnits.contains(u)) {
								Map<String,Unit> newUnits = newSources.get(sm);
								if(newUnits == null) {
									newUnits = new HashMap<>();
									newSources.put(sm, newUnits);
								}
								newUnits.put(stmt, u);
							}
						}
					}
				}
				if(!newSources.isEmpty()) {
					ret.add(ValuePair.make(vp.getOp1(),vp.getOp2(),newSources));
				} else {
					ret.add(vp);
				}
			}
			return ret;
		}
		return in;
	}

}
