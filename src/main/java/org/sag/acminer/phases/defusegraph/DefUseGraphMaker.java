package org.sag.acminer.phases.defusegraph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.sag.acminer.database.defusegraph.DefUseGraph;
import org.sag.acminer.database.defusegraph.ILocalWrapper;
import org.sag.acminer.database.defusegraph.INode;
import org.sag.acminer.database.defusegraph.LeafNode;
import org.sag.acminer.database.defusegraph.StartNode;
import org.sag.acminer.database.defusegraph.id.ConstantPart;
import org.sag.acminer.database.defusegraph.id.Identifier;
import org.sag.acminer.database.defusegraph.id.IdentifierGenerator;
import org.sag.acminer.database.defusegraph.id.Part;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.concurrent.IgnorableRuntimeException;
import org.sag.common.concurrent.SimpleValueRunner;
import org.sag.common.concurrent.SimpleValueWorkerGroup;
import org.sag.common.concurrent.ValueRunner;
import org.sag.common.concurrent.ValueWorkerFactory;
import org.sag.common.concurrent.ValueWorkerGroup;
import org.sag.common.concurrent.WorkerCountingThreadExecutor;
import org.sag.common.logging.ILogger;
import org.sag.common.tools.HierarchyHelpers;
import org.sag.common.tuple.Pair;
import org.sag.soot.callgraph.IJimpleICFG;

import soot.Local;
import soot.SootField;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Unit;
import soot.Value;
import soot.jimple.CastExpr;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.ParameterRef;
import soot.jimple.Stmt;

public class DefUseGraphMaker {
	
	private final String name;
	private final WorkerCountingThreadExecutor exe;

	public DefUseGraphMaker() {
		this(new WorkerCountingThreadExecutor(new ValueWorkerFactory<>()));
	}
	
	public DefUseGraphMaker(WorkerCountingThreadExecutor exe) {
		this.name = getClass().getSimpleName();
		this.exe = exe;
		//Note it is assumed that if these methods were already called before these calls will do nothing
		//i.e. these detect if the maps storing exist and do nothing if they do
		ILocalWrapper.Factory.init();
		INode.Factory.init();
	}
	
	public boolean shutdownWhenFinished() {
		boolean ret = exe.shutdownWhenFinished();
		ILocalWrapper.Factory.reset();//Clear the cache of local wrappers when finished
		INode.Factory.reset();//Clear the cache of nodes when finished
		IdentifierGenerator.resetCache();
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
	
	//TODO Remove commented out sections
	/*public Triple<Set<String>,Set<String>,Map<String,Set<String>>> gatherData(DefUseGraph graph, SootMethod ep, 
			ILogger logger) {
		try {
			//SootField, SootMethod, String constant -> Uses
			ValueWorkerGroup<Triple<Set<String>,Set<String>,Map<String,Set<String>>>, Triple<String,String,Map<String,Set<String>>>> g = 
				new ValueWorkerGroup<Triple<Set<String>,Set<String>,Map<String,Set<String>>>, Triple<String,String,Map<String,Set<String>>>>(
						name,ep.toString()+"_GatherData",logger,false,false) {
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
			logger.fineInfo("{}: Gathering data from def use graphs for ep '{}'.",name,ep.toString());
			
			Set<INode> seen = Collections.<INode>synchronizedSet(new HashSet<INode>());
			for(StartNode startNode : graph.getStartNodes()) {
				executeRunner(new GatherDataDefUseGraphRunner(startNode, graph, seen, logger), g, logger);
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
	}*/
	
	/*private class GatherDataDefUseGraphRunner implements ValueRunner<Triple<String,String,Map<String,Set<String>>>> {

		private volatile Triple<String,String,Map<String,Set<String>>> ret;
		private final INode valueNode;
		private final DefUseGraph valueTree;
		private final Set<INode> seen;
		private final ILogger logger;
		
		public GatherDataDefUseGraphRunner(INode valueNode, DefUseGraph valueTree, Set<INode> seen, ILogger logger) {
			this.valueNode = valueNode;
			this.valueTree = valueTree;
			this.seen = seen;
			this.logger = logger;
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
						for(Set<String> values : valueTree.getStartNodesToDefStrings().values()) {
							for(String value : values) {
								if(value.contains(s))
									temp.add(value);
							}
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
					String s = valueNode.toString();
					for(Set<String> values : valueTree.getStartNodesToDefStrings().values()) {
						for(String value : values) {
							if(value.contains(s))
								temp.add(value);
						}
					}
				}
			}
			
			ret = new Triple<String,String,Map<String,Set<String>>>(fieldStr,methodStr,stringConsts);
			
			List<ValueRunner<Triple<String,String,Map<String,Set<String>>>>> runners = new ArrayList<>();
			for(INode child : valueTree.getChildNodes(valueNode)) {
				if(seen.add(child)) 
					runners.add(new GatherDataDefUseGraphRunner(child, valueTree, seen, logger));
			}
			executeRunners(runners, logger);
		}

		@Override
		public Triple<String, String, Map<String, Set<String>>> getValue() {
			return ret;
		}
		
	}*/
	
	public DefUseGraph makeDefUseGraphs(Set<Unit> seeds, EntryPoint ep, IJimpleICFG icfg, ILogger logger) {
		return makeDefUseGraphs(seeds, ep, icfg, true, logger);
	}
	
	public DefUseGraph makeDefUseGraphs(Set<Unit> seeds, EntryPoint ep, IJimpleICFG icfg, boolean includeExcluded, ILogger logger) {
		try {
			List<StartNode> startNodes = new ArrayList<>();
			DefUseGraph graph = null;
			Set<INode> seen = Collections.<INode>synchronizedSet(new HashSet<INode>());
			SimpleValueWorkerGroup g = new SimpleValueWorkerGroup(name,ep.toString()+"_makeDefUseGraph",logger,false,false);
			
			logger.fineInfo("{}: Making def use graphs for ep '{}' with '{}' seeds.",name,ep.toString(),seeds.size());
			
			for(Unit u : seeds) {
				startNodes.add(INode.Factory.getNewStart(icfg.getMethodOf(u), u));
			}
			graph = new DefUseGraph(startNodes);
			
			for(StartNode startNode : startNodes) {
				Identifier id = startNode.getIdentifier();
				for(int i = 0; i < id.size(); i++) {
					Part p = id.get(i);
					if(p instanceof ConstantPart) {
						graph.addInlineConstantNode(startNode, INode.Factory.getNewInlineConstantLeafNode(startNode, i));
					}
				}
				executeRunner(new MakeValueTreesRunner(ep, startNode, graph, seen, icfg, includeExcluded, logger), g, logger);
			}
			g.unlockInitialLock();
			g.getReturnValue();//block
			if(g.shutdownNormally() && !g.hasExceptions()) {
				logger.fineInfo("{}: Successfully constructed the def use graphs for group '{}'.",name,g.getName());
				return graph;
			} else {
				logger.fatal("{}: Failed to construct the def use graphs for group '{}'.",name,g.getName());
				throw new IgnorableRuntimeException();
			}
		} catch(IgnorableRuntimeException t) {
			throw t;
		} catch(Throwable t) {
			logger.fatal("{}: An unexpected error occured while creating the def use graphs for ep'{}'.",t,name,ep);
			throw new IgnorableRuntimeException();
		}
	}
	
	private class MakeValueTreesRunner extends SimpleValueRunner {
		
		private final EntryPoint ep;
		private final INode curValueNode;
		private final DefUseGraph graph;
		private final Set<INode> seen;
		private final IJimpleICFG icfg;
		private final ILogger logger;
		private final List<ValueRunner<Object>> runners;
		private final boolean includeExcluded;
		
		public MakeValueTreesRunner(EntryPoint ep, INode curValueNode, DefUseGraph graph, Set<INode> seen, IJimpleICFG icfg, 
				boolean includeExcluded, ILogger logger) {
			this.ep = ep;
			this.graph = graph;
			this.curValueNode = curValueNode;
			this.seen = seen;
			this.icfg = icfg;
			this.logger = logger;
			this.runners = new ArrayList<>();
			this.includeExcluded = includeExcluded;
		}

		@Override
		public void run() {
			if(!(curValueNode instanceof LeafNode)) {//ignore nodes with no locals
				Unit start = curValueNode.getUnit();
				Map<Local,Set<DefinitionStmt>> defsForUsedLocals = icfg.getDefsForUsedLocalsMap(start);
				StringBuilder sb = new StringBuilder();
				sb.append("EntryPoint: ").append(ep.toString()).append("\n");
				sb.append("  Visiting ").append(curValueNode.toString()).append("\n");
				if(!defsForUsedLocals.isEmpty()) {// ignore nodes with no defs for its locals
					/* We need a worklist loop here to handle the processing of parameter refs back to
					 * all the possible values fed into the method call for the parameter. I.e. maping 
					 * parameter to defs across methods. We can only add one local of the start unit at
					 * a time to the worklist because if two different locals in the start unit trace
					 * back to two different parameters which are passed the same local in the calling
					 * method, we would end up processing one and then skipping the other because of the
					 * loop preventer.
					 * 
					 * For each used local, try to find all defs that are not local, cast, or parm-ref 
					 * but then stop at the def and process it in another thread.
					 */
					for(Local l : defsForUsedLocals.keySet()) {
						Set<DefinitionStmt> temp = filterDefs(defsForUsedLocals.get(l));
						if(temp != null && !temp.isEmpty()) {
							Queue<Pair<Unit,Set<DefinitionStmt>>> toVisit = new ArrayDeque<>();
							Set<Pair<Unit,Local>> visited = new HashSet<>();
							toVisit.add(new Pair<Unit,Set<DefinitionStmt>>(start,temp));
							visited.add(new Pair<Unit, Local>(start,l));
							
							while(!toVisit.isEmpty()) {
								Pair<Unit,Set<DefinitionStmt>> t = toVisit.poll();
								Unit cur = t.getFirst();
								SootMethod curSource = icfg.getMethodOf(cur);
								
								for(DefinitionStmt def : t.getSecond()) {
									if(def.getRightOp() instanceof ParameterRef && !curSource.equals(ep.getEntryPoint())) {
										ParameterRef parmRef = (ParameterRef)def.getRightOp();
										Collection<Unit> callers = icfg.getCallersOf(curSource);
										if(!callers.isEmpty()) {
											boolean atLeastOne = false;
											for(Unit callerStmt : callers) {
												if(((Stmt)callerStmt).containsInvokeExpr()) {
													atLeastOne = true;
													SootMethod callerSource = icfg.getMethodOf(callerStmt);
													Value arg = ((Stmt)callerStmt).getInvokeExpr().getArg(parmRef.getIndex());
													if(arg instanceof Local) {
														if(visited.add(new Pair<Unit,Local>(callerStmt,(Local)arg))) {
															toVisit.add(new Pair<Unit,Set<DefinitionStmt>>(callerStmt,
																	filterDefs(icfg.getOrMakeLocalDefs(callerSource)
																			.getDefsWithAliases((Local)arg, callerStmt))));
														}
													} else if(arg instanceof Constant) {
														processChild(l, callerStmt, callerSource, parmRef.getIndex(),sb);
													} else {
														logger.fineInfo("{}: Unexpected type '{}' of '{}' in invoke args of '{}' of '{}.",
																name,arg.getClass(),arg,callerStmt,callerSource);
														throw new IgnorableRuntimeException();
													}
												}
											}
											if(!atLeastOne)
												processChild(l, def, curSource,sb);
										} else {
											processChild(l, def, curSource,sb);
										}
									} else {
										if(def.containsInvokeExpr()) {
											Collection<SootMethod> callees;
											if(includeExcluded)
												callees = icfg.getAllCalleesOfCallAt(def);
											else
												callees = icfg.getCalleesOfCallAt(def);
											boolean atLeastOne = false;
											for(SootMethod callee : callees) {
												SootMethodRef mr = def.getInvokeExpr().getMethodRef();
												if(!callee.isAbstract() && mr.name().equals(callee.getName()) 
														&& mr.parameterTypes().toString().equals(callee.getParameterTypes().toString())) {
													processChild(l, def, curSource, callee, sb);
													atLeastOne = true;
												}
											}
											if(!atLeastOne)
												processChild(l, def, curSource, sb);
										} else if(def.containsFieldRef()) {
											SootField field;
											try {
												field = HierarchyHelpers.resolveField(def.getFieldRef().getFieldRef());
											} catch(Throwable t2) {
												field = null;
											}
											processChild(l, def, curSource, field, sb);
										} else {
											processChild(l, def, curSource, sb);
										}
									}
								}
							}
						}
					}
				}
				logger.fineInfo("{}: {}",name,sb.toString());
			}
			
			if(!runners.isEmpty()) {
				executeRunners(runners, logger);
				runners.clear();
			}
		}
		
		private Set<DefinitionStmt> filterDefs(Set<DefinitionStmt> defs) {
			Set<DefinitionStmt> ret = new HashSet<>();
			for(DefinitionStmt def : defs) {
				Value right = def.getRightOp();
				if(!(right instanceof Local) && !(right instanceof CastExpr))
					ret.add(def);
			}
			return ret;
		}
		
		private void processChild(Local l, Unit def, SootMethod source, StringBuilder sb) {
			processChild(l, def, source, -1, null, null, sb);
		}
		
		private void processChild(Local l, Unit def, SootMethod source, int index, StringBuilder sb) {
			processChild(l, def, source, index, null, null, sb);
		}
		
		private void processChild(Local l, Unit def, SootMethod source, SootMethod target, StringBuilder sb) {
			processChild(l, def, source, -1, target, null, sb);
		}
		
		private void processChild(Local l, Unit def, SootMethod source, SootField field, StringBuilder sb) {
			processChild(l, def, source, -1, null, field, sb);
		}
		
		private void processChild(Local l, Unit def, SootMethod source, int index, SootMethod target, SootField field, StringBuilder sb) {
			INode node = INode.Factory.getNew(source, def, index, target, field);
			if(seen.add(node)) {
				sb.append("    Found new Node ").append(node.toString()).append("\n");
				if(!(node instanceof LeafNode))
					runners.add(new MakeValueTreesRunner(ep, node, graph, seen, icfg, includeExcluded, logger));
				if(!(node instanceof LeafNode) || !(node.getValue() instanceof Constant)) {
					//Only want inline constants for non-leaf nodes and leaf nodes that are not constants
					Identifier id = node.getIdentifier();
					for(int i = 0; i < id.size(); i++) {
						Part p = id.get(i);
						if(p instanceof ConstantPart) {
							graph.addInlineConstantNode(node, INode.Factory.getNewInlineConstantLeafNode(node, i));
						}
					}
				}
			}
			graph.addChild(curValueNode, l, node);
		}
		
	}
	
}
