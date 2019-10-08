package org.sag.acminer.phases.defusegraphmod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.database.defusegraph.DefUseGraph;
import org.sag.acminer.database.defusegraph.IDefUseGraphDatabase;
import org.sag.acminer.database.defusegraph.INode;
import org.sag.acminer.database.defusegraph.LocalWrapper;
import org.sag.acminer.database.defusegraph.StartNode;
import org.sag.acminer.database.excludedelements.IExcludeHandler;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.concurrent.CountingThreadExecutor;
import org.sag.common.concurrent.IgnorableRuntimeException;
import org.sag.common.concurrent.LoggingWorkerGroup;
import org.sag.common.concurrent.WorkerCountingThreadExecutor;
import org.sag.common.logging.ILogger;
import org.sag.common.tuple.Pair;
import org.sag.soot.callgraph.ExcludingJimpleICFG;
import org.sag.soot.callgraph.IJimpleICFG;
import org.sag.soot.callgraph.JimpleICFG;
import org.sag.soot.callgraph.ExcludingJimpleICFG.ExcludingEdgePredicate;

import soot.Local;
import soot.SootClass;
import soot.Unit;
import soot.Value;
import soot.VoidType;
import soot.jimple.BinopExpr;
import soot.jimple.DefinitionStmt;
import soot.jimple.NegExpr;
import soot.jimple.ParameterRef;
import soot.jimple.Stmt;

public class DefUseGraphModifierRunner {
	
	private final ILogger mainLogger;
	private final IACMinerDataAccessor dataAccessor;
	private final String name;
	
	public DefUseGraphModifierRunner(IACMinerDataAccessor dataAccessor, ILogger mainLogger){
		this.dataAccessor = dataAccessor;
		this.mainLogger = mainLogger;
		this.name = getClass().getSimpleName();
	}
	
	public boolean run() {
		boolean successOuter = true;
		
		mainLogger.info("{}: Begin the def use graph modifier runner.",name);
		
		if(successOuter){
			WorkerCountingThreadExecutor exe = null;
			DefUseGraphModifier mod = null;
			List<LoggingWorkerGroup> workerGroups = new ArrayList<>();
			//Set new control predicates database
			dataAccessor.setDefUseGraphModDB(IDefUseGraphDatabase.Factory.getNew(false));
			try{
				JimpleICFG baseICFG = new JimpleICFG(dataAccessor.getEntryPoints(),false);
				SootClass stub = null;
				LoggingWorkerGroup curWorkerGroup = null;
				Deque<EntryPoint> eps = new ArrayDeque<>(dataAccessor.getEntryPoints());
				exe = new WorkerCountingThreadExecutor();
				mod = new DefUseGraphModifier();
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
						Runnable runner = new DefUseGraphModRunner(ep,baseICFG,mod,curWorkerGroup.getLogger());
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
				mainLogger.fatal("{}: An unexpected exception occured in the def use graph modifier runner.",t,name);
				successOuter = false;
			} finally {
				//Shutdown the executors
				if(exe != null && !exe.shutdownWhenFinished()){
					mainLogger.fatal(CountingThreadExecutor.computeJointErrorMsg(exe.getAndClearExceptions(), 
							"Failed to wait for the executor to terminate.", name));
					successOuter = false;
				}
				if(mod != null && !mod.shutdownWhenFinished()) {
					mainLogger.fatal(CountingThreadExecutor.computeJointErrorMsg(mod.getAndClearExceptions(), 
							"Failed to wait for the def use graph modifier to terminate.",name));
					successOuter = false;
				}
				
				for(LoggingWorkerGroup g : workerGroups) {
					if(g.shutdownNormally() && !g.hasExceptions()) {
						mainLogger.info("{}: Successfully completed the def use graph modifier for Stub '{}'.",name,g.getName());
					} else {
						mainLogger.fatal("{}: Failed to complete the def use graph modifier for Stub '{}'.",name,g.getName());
						successOuter = false;
					}
				}
			}
		}
		
		if(!successOuter){
			mainLogger.fatal("{}: The def use graph modifier runner failed for one or more entry points. Stopping Analysis!",name);
			return false;
		}else{
			mainLogger.info("{}: The def use graph modifier runner succeeded!",name);
			return true;
		}
	}
	
	private class DefUseGraphModRunner implements Runnable {
		
		private final EntryPoint ep;
		private final JimpleICFG baseICFG;
		private final ILogger logger;
		private final DefUseGraphModifier mod;
		
		public DefUseGraphModRunner(EntryPoint ep, JimpleICFG baseICFG, DefUseGraphModifier mod, ILogger logger) {
			this.ep = ep;
			this.baseICFG = baseICFG;
			this.logger = logger;
			this.mod = mod;
		}

		@Override
		public void run() {
			IExcludeHandler excludeHandler = null;
			IJimpleICFG icfg = null;
			
			logger.fineInfo("{}: Begin modifying the def use graph for ep '{}'.",name,ep);
			
			try{
				excludeHandler = dataAccessor.getExcludedElementsDB().createNewExcludeHandler(ep);
				icfg = new ExcludingJimpleICFG(ep, baseICFG, new ExcludingEdgePredicate(baseICFG.getCallGraph(), excludeHandler));
				DefUseGraph inGraph = dataAccessor.getDefUseGraphDB().getDefUseGraph(ep, dataAccessor.getConfig().getFilePath("acminer_defusegraph-dir"));
				Set<Unit> controlPredicates = dataAccessor.getControlPredicatesDB().getUnits(ep);
				Set<Unit> contextQueries = new HashSet<>(dataAccessor.getContextQueriesDB().getUnits(ep));
				Set<Unit> markedData = new HashSet<>();
				
				//Only add context queries to the graph if 1) they have a return type of void or 2) are not used in a control predicate
				
				markedData.addAll(controlPredicates);
				for(Iterator<Unit> it = contextQueries.iterator(); it.hasNext();) {
					Unit u = it.next();
					if(((Stmt)u).containsInvokeExpr()) {
						if((((Stmt)u).getInvokeExpr().getMethodRef().returnType() instanceof VoidType)) {
							markedData.add(u);
							it.remove();
						}
					}
				}
				
				if(!contextQueries.isEmpty()) {
					//Pull out the start nodes that match our control predicates
					Map<StartNode,Set<INode>> cpStartNodes = new HashMap<>();
					for(StartNode sn : inGraph.getStartNodes()) {
						if(controlPredicates.contains(sn.getUnit()))
							cpStartNodes.put(sn,new HashSet<INode>());
					}
					//Find any context query units reachable from each start node
					for(StartNode sn : cpStartNodes.keySet()) {
						Set<INode> visited = new HashSet<>();
						Deque<INode> toVisit = new LinkedList<>();
						toVisit.push(sn);
						while(!toVisit.isEmpty()) {
							INode cur = toVisit.pop();
							if(visited.add(cur)) {
								if(contextQueries.contains(cur.getUnit()))
									cpStartNodes.get(sn).add(cur);
								for(INode vn : inGraph.getChildNodes(cur)) {
									if(!visited.contains(vn))
										toVisit.push(vn);
								}
							}
						}
					}
					//Separate out the context queries that get used in a arithmetic chain in a control predicate
					Set<Unit> contextQueriesUsedInControlPredicates = new HashSet<>();
					for(StartNode sn : cpStartNodes.keySet()) {
						Set<Pair<INode,INode>> visited = new HashSet<>();
						Deque<Pair<INode,INode>> toVisit = new LinkedList<>();
						for(INode i : cpStartNodes.get(sn)) {
							toVisit.push(new Pair<INode,INode>(i,i));
						}
						while(!toVisit.isEmpty()) {
							Pair<INode,INode> p = toVisit.pop();
							if(visited.add(p)) {
								INode cur = p.getFirst();
								INode start = p.getSecond();
								Pair<LocalWrapper, Set<INode>> uses = inGraph.getUsesForDefinition(sn,cur);
								if(uses == null) {
									contextQueriesUsedInControlPredicates.add(start.getUnit());
								} else {
									for(INode use : uses.getSecond()) {
										Pair<INode,INode> newp = null;
										if(use instanceof StartNode) {
											//Start node is not a def but will always be some kind of boolean expression
											newp = new Pair<INode,INode>(use,start);
										} else {
											//All other nodes should be DefinitionStmt
											Value v = ((DefinitionStmt)use.getUnit()).getRightOp();
											if(v instanceof BinopExpr || v instanceof NegExpr || v instanceof Local || v instanceof ParameterRef) {
												newp = new Pair<INode,INode>(use,start);
											}
										}
										if(newp != null && !visited.contains(newp))
											toVisit.add(newp);
									}
								}
							}
						}
					}
					//Keep only the context queries not used in a arithmetic chain of a control predicate
					contextQueries.removeAll(contextQueriesUsedInControlPredicates);
					//Add any remaining to our list of marked data
					markedData.addAll(contextQueries);
				}
				
				DefUseGraph defUseGraph = mod.modDefUseGraph(markedData, ep, icfg, logger);
				
				try {
					dataAccessor.getDefUseGraphModDB().writeAndAddDefUseGraph(ep, defUseGraph, 
							dataAccessor.getConfig().getFilePath("acminer_defusegraphmod-dir"));
				} catch(Throwable t) {
					logger.fatal("{}: An error occured in writing the DefUseGraph mod for '{}'.",t,name,ep);
					throw new IgnorableRuntimeException();
				}
				
				logger.fineInfo("{}: The def use graph modifier succeeded for ep '{}'.",name,ep);
			} catch(IgnorableRuntimeException t) {
				throw t;
			} catch(Throwable t) {
				logger.fatal("{}: An unexpected error occured in the def use graph modifier for ep '{}'.",t,ep);
				throw new IgnorableRuntimeException();
			}
		}
	}

}
