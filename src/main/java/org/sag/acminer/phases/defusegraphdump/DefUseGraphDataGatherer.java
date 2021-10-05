package org.sag.acminer.phases.defusegraphdump;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.sag.acminer.database.defusegraph.DefUseGraph;
import org.sag.acminer.database.defusegraph.IFieldNode;
import org.sag.acminer.database.defusegraph.IInvokeNode;
import org.sag.acminer.database.defusegraph.INode;
import org.sag.acminer.database.defusegraph.InvokeConstantLeafNode;
import org.sag.acminer.database.defusegraph.StartNode;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.concurrent.IgnorableRuntimeException;
import org.sag.common.concurrent.ValueRunner;
import org.sag.common.concurrent.ValueWorkerFactory;
import org.sag.common.concurrent.ValueWorkerGroup;
import org.sag.common.concurrent.WorkerCountingThreadExecutor;
import org.sag.common.logging.ILogger;
import org.sag.common.tools.SortingMethods;
import org.sag.common.tuple.Triple;
import org.sag.soot.SootSort;
import org.sag.soot.callgraph.ExcludingJimpleICFG.ExcludingEdgePredicate;

import soot.SootField;
import soot.SootMethod;
import soot.Value;
import soot.ValueBox;
import soot.jimple.Constant;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class DefUseGraphDataGatherer {
	
	private final String name;
	private final WorkerCountingThreadExecutor exe;

	public DefUseGraphDataGatherer() {
		this.name = getClass().getSimpleName();
		this.exe = new WorkerCountingThreadExecutor(new ValueWorkerFactory<>());
	}
	
	public boolean shutdownWhenFinished() {
		boolean ret = exe.shutdownWhenFinished();
		return ret;
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
	
	public Set<String> gatherNodesOnPathsToAuthorizationLogic(EntryPoint ep, ExcludingEdgePredicate pred, CallGraph cg,
			Set<SootMethod> seeds, ILogger logger) {
		try {
			ValueWorkerGroup<Set<SootMethod>, SootMethod> g = new ValueWorkerGroup<Set<SootMethod>,SootMethod>(name,
					ep+"_NodesOnPaths",logger,false,false) {
						@Override
						protected Set<SootMethod> initReturnValue() {
							return new HashSet<>();
						}
						@Override
						protected void joinWorkerReturnValue(SootMethod value) {
							if(value != null) {
								ret.add(value);
							}
						}
						@Override
						protected void finalizeReturnValue() {}
			};
			logger.fineInfo("{}: Gathering the nodes on the paths to all authorization logic for ep '{}'.",name,ep);
			
			Set<SootMethod> seen = Collections.<SootMethod>synchronizedSet(new HashSet<SootMethod>());
			for(SootMethod seed : seeds) {
				executeRunner(new NodePathRunner(ep, seed, pred, cg, seen, logger), g, logger);
			}
			
			g.unlockInitialLock();
			Set<SootMethod> data = g.getReturnValue();
			if(g.shutdownNormally() && !g.hasExceptions()) {
				data = SortingMethods.sortSet(data,SootSort.smComp);
				Set<String> ret = new HashSet<>();
				for(SootMethod m : data) 
					ret.add(m.toString());
				logger.fineInfo("{}: Successfully gathered the nodes on the paths to all authorization logic"
						+ " for group '{}'.",name,g.getName());
				return ret;
			} else {
				logger.fatal("{}: Failed to gather the nodes on the paths to all authorization logic"
						+ " for group '{}'.",name,g.getName());
				throw new IgnorableRuntimeException();
			}
		} catch(IgnorableRuntimeException t) {
			throw t;
		} catch(Throwable t) {
			logger.fatal("{}: An unexpected error occured while gathering the nodes on the paths to all "
					+ "authorization logic for ep '{}'.",t,name,ep);
			throw new IgnorableRuntimeException();
		}
	}
	
	public Triple<Set<String>,Set<String>,Map<String,Set<String>>> gatherData(DefUseGraph graph, Set<StartNode> startNodes, EntryPoint ep, 
			ILogger logger) {
		try {
			//SootField, SootMethod, String constant -> Uses
			ValueWorkerGroup<Triple<Set<String>,Set<String>,Map<String,Set<String>>>, Triple<String,String,Map<String,Set<String>>>> g = 
				new ValueWorkerGroup<Triple<Set<String>,Set<String>,Map<String,Set<String>>>, Triple<String,String,Map<String,Set<String>>>>(
						name,ep+"_GatherData",logger,false,false) {
				@Override 
				protected Triple<Set<String>,Set<String>,Map<String,Set<String>>> initReturnValue() {
					return new Triple<Set<String>,Set<String>,Map<String,Set<String>>>(new HashSet<String>(), new HashSet<String>(), 
							new HashMap<String,Set<String>>());
				}
				@Override 
				protected void joinWorkerReturnValue(Triple<String,String,Map<String,Set<String>>> value) {
					if(value != null) {
						if(value.getFirst() != null) {
							ret.getFirst().add(value.getFirst());
						}
						if(value.getSecond() != null) {
							ret.getSecond().add(value.getSecond());
						}
						if(value.getThird() != null) {
							Map<String,Set<String>> third = value.getThird();
							Map<String,Set<String>> map = ret.getThird();
							for(String key : third.keySet()) {
								Set<String> temp = map.get(key);
								if(temp == null) {
									temp = new HashSet<>();
									map.put(key, temp);
								}
								temp.addAll(third.get(key));
							}
						}
					}
				}
				@Override protected void finalizeReturnValue() {}
			};
			logger.fineInfo("{}: Gathering data from def use graphs for ep '{}'.",name,ep);
			
			Set<INode> seen = Collections.<INode>synchronizedSet(new HashSet<INode>());
			for(StartNode startNode : startNodes) {
				executeRunner(new GatherDataDefUseGraphRunner(startNode, startNode, graph, seen, logger), g, logger);
			}
			
			g.unlockInitialLock();
			Triple<Set<String>, Set<String>, Map<String, Set<String>>> ret = g.getReturnValue();
			if(g.shutdownNormally() && !g.hasExceptions()) {
				Set<String> r1 = SortingMethods.sortSet(ret.getFirst(),SootSort.sfStringComp);
				Set<String> r2 = SortingMethods.sortSet(ret.getSecond(),SootSort.smStringComp);
				
				for(String s : ret.getThird().keySet()) {
					ret.getThird().put(s, SortingMethods.sortSet(ret.getThird().get(s),SortingMethods.sComp));
				}
				Map<String,Set<String>> r3 = SortingMethods.sortMapKey(ret.getThird(),SortingMethods.sComp);
				
				Triple<Set<String>,Set<String>,Map<String,Set<String>>> toRet = new Triple<>(r1,r2,r3);
				logger.fineInfo("{}: Successfully gathered data from def use graphs for group '{}'.",name,g.getName());
				return toRet;
			} else {
				logger.fatal("{}: Failed to gather data from def use graphs for group '{}'.",name,g.getName());
				throw new IgnorableRuntimeException();
			}
		} catch(IgnorableRuntimeException t) {
			throw t;
		} catch(Throwable t) {
			logger.fatal("{}: An unexpected error occured while gathering data from def use graphs for ep'{}'.",t,name,ep);
			throw new IgnorableRuntimeException();
		}
	}
	
	private class NodePathRunner implements ValueRunner<SootMethod> {
		private final EntryPoint ep;
		private final ILogger logger;
		private final SootMethod cur;
		private final ExcludingEdgePredicate pred;
		private final Set<SootMethod> seen;
		private final CallGraph cg;
		
		public NodePathRunner(EntryPoint ep, SootMethod cur, ExcludingEdgePredicate pred, CallGraph cg, 
				Set<SootMethod> seen, ILogger logger) {
			this.ep = ep;
			this.cur = cur;
			this.pred = pred;
			this.seen = seen;
			this.cg = cg;
			this.logger = logger;
		}

		@Override
		public void run() {
			List<ValueRunner<SootMethod>> runners = new ArrayList<>();
			if(seen.add(cur) && !Objects.equals(ep.getEntryPoint(), cur)) {
				for(Iterator<Edge> it = cg.edgesInto(cur); it.hasNext();) {
					Edge e = it.next();
					if(pred.want(e)) {
						runners.add(new NodePathRunner(ep, e.src(), pred, cg, seen, logger));
					}
				}
			}
			if(!runners.isEmpty()) {
				executeRunners(runners, logger);
			}
		}

		@Override
		public SootMethod getValue() {
			return cur;
		}
	}
	
	private class GatherDataDefUseGraphRunner implements ValueRunner<Triple<String,String,Map<String,Set<String>>>> {

		private volatile Triple<String,String,Map<String,Set<String>>> ret;
		private final INode valueNode;
		private final StartNode sn;
		private final DefUseGraph valueTree;
		private final Set<INode> seen;
		private final ILogger logger;
		
		public GatherDataDefUseGraphRunner(INode valueNode, StartNode sn, DefUseGraph valueTree, Set<INode> seen, 
				ILogger logger) {
			this.valueNode = valueNode;
			this.valueTree = valueTree;
			this.seen = seen;
			this.logger = logger;
			this.sn = sn;
		}
		
		//FieldRef, SootMethodRef -> all arg combinations, String constant -> Uses
		@Override
		public void run() {
			String fieldStr = null;
			String methodStr = null;
			Map<String,Set<String>> stringConsts = null;
			
			Stmt stmt = (Stmt)valueNode.getUnit();
			
			//Find field ref
			if(valueNode instanceof IFieldNode) {
				SootField field = ((IFieldNode)valueNode).getField();
				if(field == null)
					fieldStr = stmt.getFieldRef().getFieldRef().getSignature() + " _-_ no resolution";
				else
					fieldStr = field.getSignature();
			} else if(!(valueNode instanceof InvokeConstantLeafNode) && stmt.containsFieldRef()) {
				fieldStr = stmt.getFieldRef().getFieldRef().getSignature() + " _-_ not considered for resolution !?!";
			}
			
			//Find method ref and get all possible args
			if(valueNode instanceof IInvokeNode) {
				SootMethod method = ((IInvokeNode)valueNode).getTarget();
				if(method == null)
					methodStr = stmt.getInvokeExpr().getMethodRef().getSignature() + " _-_ no resolution";
				else
					methodStr = method.getSignature();
			} else if(!(valueNode instanceof InvokeConstantLeafNode) && stmt.containsInvokeExpr()) {
				methodStr = stmt.getInvokeExpr().getMethodRef().getSignature() + " _-_ not considered for resolution !?!";
			}
			
			//Get the strings that get used directly without any local
			if(!(valueNode instanceof InvokeConstantLeafNode)) {
				for(ValueBox vb : stmt.getUseBoxes()) {
					Value v = vb.getValue();
					if(v instanceof StringConstant) {
						if(stringConsts == null)
							stringConsts = new HashMap<>();
						String s = v.toString();
						Set<String> temp = stringConsts.get(s);
						if(temp == null) {
							temp = new HashSet<>();
							stringConsts.put(s,temp);
						}
						String nodeString = valueNode.toString();
						for(String value : valueTree.getDefStrings(sn)) {
							if(value.endsWith(nodeString))
								temp.add(value);
						}
					}
				}
			}
			
			if(valueNode instanceof InvokeConstantLeafNode || (valueNode.getValue() != null && valueNode.getValue() instanceof Constant)) {
				Value v = valueNode.getValue();
				if(v instanceof StringConstant) {
					if(stringConsts == null)
						stringConsts = new HashMap<>();
					String stringConst = v.toString();
					Set<String> temp = stringConsts.get(stringConst);
					if(temp == null) {
						temp = new HashSet<>();
						stringConsts.put(stringConst,temp);
					}
					String nodeString = valueNode.toString();
					for(String value : valueTree.getDefStrings(sn)) {
						if(value.endsWith(nodeString))
							temp.add(value);
					}
				}
			}
			
			ret = new Triple<String,String,Map<String,Set<String>>>(fieldStr,methodStr,stringConsts);
			
			List<ValueRunner<Triple<String,String,Map<String,Set<String>>>>> runners = new ArrayList<>();
			for(INode child : valueTree.getChildNodes(valueNode)) {
				if(seen.add(child)) 
					runners.add(new GatherDataDefUseGraphRunner(child, sn, valueTree, seen, logger));
			}
			executeRunners(runners, logger);
		}

		@Override
		public Triple<String, String, Map<String, Set<String>>> getValue() {
			return ret;
		}
		
	}

}
