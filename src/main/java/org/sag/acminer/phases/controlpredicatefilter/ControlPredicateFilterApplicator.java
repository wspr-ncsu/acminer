package org.sag.acminer.phases.controlpredicatefilter;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.database.defusegraph.DefUseGraph;
import org.sag.acminer.database.defusegraph.StartNode;
import org.sag.acminer.database.filter.CPFilterData;
import org.sag.acminer.database.filter.ControlPredicateFilterDatabase;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.concurrent.IgnorableRuntimeException;
import org.sag.common.concurrent.ValueRunner;
import org.sag.common.concurrent.ValueWorkerFactory;
import org.sag.common.concurrent.ValueWorkerGroup;
import org.sag.common.concurrent.WorkerCountingThreadExecutor;
import org.sag.common.logging.ILogger;
import org.sag.common.tools.SortingMethods;
import org.sag.common.tuple.Pair;
import org.sag.soot.SootSort;
import org.sag.soot.callgraph.IJimpleICFG;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;

public class ControlPredicateFilterApplicator {
	
	private final String name;
	private final WorkerCountingThreadExecutor exe;

	public ControlPredicateFilterApplicator() {
		this.name = getClass().getSimpleName();
		this.exe = new WorkerCountingThreadExecutor(new ValueWorkerFactory<>());
	}
	
	public boolean shutdownWhenFinished() {
		return exe.shutdownWhenFinished();
	}
	
	public List<Throwable> getAndClearExceptions() {
		return exe.getAndClearExceptions();
	}
	
	private synchronized <O> void executeRunner(ValueRunner<O> runner, ValueWorkerGroup<?,O> g, ILogger logger) {
		try {
			exe.execute(runner,g);
		} catch(Throwable t) {
			logger.fatal("{}: Failed to execute '{}' for group '{}'.",t,name,runner.toString(),g.getName());
			throw new IgnorableRuntimeException();
		}
	}
	
	public Map<SootMethod, Set<Unit>> applyFilter(EntryPoint ep, IJimpleICFG icfg, ControlPredicateFilterDatabase cpFilter, 
			Map<Unit, StartNode> cps, DefUseGraph graph, IACMinerDataAccessor dataAccessor, boolean debug, ILogger logger) {
		try {
			cps = SortingMethods.sortMapValueAscending(cps);
			
			StringBuilder sb = new StringBuilder();
			for(StartNode sn : cps.values()) {
				sb.append("  ").append("Stmt: ").append(sn.toString()).append(" Source: ")
				.append(sn.getSource()).append("\n");
			}
			 
			logger.debug("{}: Filtering control predicates for ep '{}'. Input:{}\n",name,ep,sb.toString());
			
			Map<SootMethod,Set<Unit>> ret;
			if(!cps.isEmpty()) {
				ValueWorkerGroup<Map<Unit,Boolean>,Pair<Unit,Boolean>> g = new ValueWorkerGroup<Map<Unit,Boolean>,Pair<Unit,Boolean>>(name,ep+"_filter",logger,false,false) {
					@Override protected Map<Unit,Boolean> initReturnValue() {return new HashMap<>();}
					@Override protected void joinWorkerReturnValue(Pair<Unit,Boolean> value) {if(value != null) ret.put(value.getFirst(),value.getSecond());}
					@Override protected void finalizeReturnValue() {}
				};
				
				graph.computeDefinitionsToUses();
				
				for(Unit cp : cps.keySet()) {
					StartNode sn = cps.get(cp);
					SootMethod cpSource = sn.getSource();
					CPFilterData cpFilterData = new CPFilterData(ep, dataAccessor, icfg, (Stmt)cp, cpSource, sn, graph);
					executeRunner(new ApplyFilterRunner(cpFilterData, cpFilter, debug, logger), g, logger);
				}
				g.unlockInitialLock();
				Map<Unit,Boolean> results = g.getReturnValue();
				Map<Unit, StartNode> newCPS;
				if(results.isEmpty()) {
					ret = Collections.emptyMap();
					newCPS = Collections.emptyMap();
				} else {
					ret = new HashMap<>();
					newCPS = new HashMap<>();
					for(Unit u : results.keySet()) {
						if(results.get(u)) {
							SootMethod source = icfg.getMethodOf(u);
							Set<Unit> s = ret.get(source);
							if(s == null) {
								s = new HashSet<>();
								ret.put(source, s);
							}
							s.add(u);
							newCPS.put(u, cps.get(u));
						}
					}
					if(ret.isEmpty()) {
						ret = Collections.emptyMap();
						newCPS = Collections.emptyMap();
					} else {
						for(SootMethod m : ret.keySet()) {
							ret.put(m, SortingMethods.sortSet(ret.get(m),SootSort.unitComp));
						}
						ret = SortingMethods.sortMapKey(ret, SootSort.smComp);
						newCPS = SortingMethods.sortMapValueAscending(newCPS);
					}
				}
				
				if(g.shutdownNormally() && !g.hasExceptions()) {
					sb = new StringBuilder();
					for(StartNode sn : newCPS.values()) {
						sb.append("  ").append("Stmt: ").append(sn.toString()).append(" Source: ")
						.append(sn.getSource()).append("\n");
					}
					
					logger.debug("{}: Successfully filtered control predicates for ep '{}'. Output:{}\n",name,ep,sb.toString());
				} else {
					logger.fatal("{}: Failed to filter control predicates for ep '{}'.",name,ep);
					throw new IgnorableRuntimeException();
				}
			} else {
				ret = Collections.emptyMap();
			}
			return ret;
		} catch(IgnorableRuntimeException t) {
			throw t;
		} catch(Throwable t) {
			logger.fatal("{}: An unexpected error occured while filtering control predicates for ep '{}'.",t,name,ep);
			throw new IgnorableRuntimeException();
		} finally {
			graph.clearDefToUseMap();
		}
	}
	
	private class ApplyFilterRunner implements ValueRunner<Pair<Unit, Boolean>> {
		
		private volatile Pair<Unit, Boolean> ret;
		private final CPFilterData cpFilterData;
		private final ControlPredicateFilterDatabase cpFilter;
		private final ILogger logger;
		private final boolean debug;

		public ApplyFilterRunner(CPFilterData cpFilterData, ControlPredicateFilterDatabase cpFilter, boolean debug, ILogger logger) {
			this.cpFilterData = cpFilterData;
			this.cpFilter = cpFilter;
			this.logger = logger;
			this.ret = null;
			this.debug = debug;
		}

		@Override
		public void run() {
			if(debug) {
				StringBuilder sb = new StringBuilder();
				ret = new Pair<Unit, Boolean>(cpFilterData.getStmt(), cpFilter.applyFilterDebug(cpFilterData, sb));
				logger.debug("{}: Applying filter to '{}' of '{}':\n{}",name,
						cpFilterData.getStartNode().toString(), cpFilterData.getSource(),sb.toString());
			} else {
				ret = new Pair<Unit, Boolean>(cpFilterData.getStmt(), cpFilter.applyFilter(cpFilterData));
			}
		}

		@Override
		public Pair<Unit, Boolean> getValue() {
			return ret;
		}
	}
	
}
