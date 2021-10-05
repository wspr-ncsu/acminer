package org.sag.acminer.phases.acminer;

import heros.solver.IDESolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.database.defusegraph.DefUseGraph;
import org.sag.acminer.database.defusegraph.FieldStartNode;
import org.sag.acminer.database.defusegraph.IInvokeNode;
import org.sag.acminer.database.defusegraph.ILocalWrapper;
import org.sag.acminer.database.defusegraph.INode;
import org.sag.acminer.database.defusegraph.InlineConstantLeafNode;
import org.sag.acminer.database.defusegraph.InlineConstantLocalWrapper;
import org.sag.acminer.database.defusegraph.InvokeStartNode;
import org.sag.acminer.database.defusegraph.LocalWrapper;
import org.sag.acminer.database.defusegraph.StartNode;
import org.sag.acminer.database.defusegraph.id.ConstantPart;
import org.sag.acminer.database.defusegraph.id.DataWrapperPart;
import org.sag.acminer.database.defusegraph.id.FieldRefPart;
import org.sag.acminer.database.defusegraph.id.Identifier;
import org.sag.acminer.database.defusegraph.id.LocalPart;
import org.sag.acminer.database.defusegraph.id.LocalWrapperPart;
import org.sag.acminer.database.defusegraph.id.MethodRefPart;
import org.sag.acminer.database.defusegraph.id.Part;
import org.sag.acminer.phases.acminer.dw.AllConstant;
import org.sag.acminer.phases.acminer.dw.DataWrapper;
import org.sag.acminer.phases.acminer.dw.PrimitiveConstant;
import org.sag.acminer.phases.defusegraphmod.DefUseGraphModifier;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.concurrent.IgnorableRuntimeException;
import org.sag.common.concurrent.SimpleValueRunner;
import org.sag.common.concurrent.SimpleValueWorkerGroup;
import org.sag.common.concurrent.ValueRunner;
import org.sag.common.concurrent.ValueWorkerFactory;
import org.sag.common.concurrent.ValueWorkerGroup;
import org.sag.common.concurrent.WorkerCountingThreadExecutor;
import org.sag.common.logging.ILogger;
import org.sag.common.tools.SortingMethods;
import org.sag.soot.callgraph.IJimpleICFG;
import org.sag.soot.callgraph.SwitchWrapper;

import com.google.common.cache.Cache;
import com.google.common.collect.ImmutableSet;
import com.google.common.math.IntMath;

import soot.ArrayType;
import soot.BooleanType;
import soot.PrimType;
import soot.RefType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.AnyNewExpr;
import soot.jimple.ArrayRef;
import soot.jimple.BinopExpr;
import soot.jimple.CastExpr;
import soot.jimple.Constant;
import soot.jimple.IfStmt;
import soot.jimple.InstanceOfExpr;
import soot.jimple.LengthExpr;
import soot.jimple.NegExpr;
import soot.jimple.ParameterRef;
import soot.jimple.Stmt;
import soot.jimple.SwitchStmt;

public class ACMiner {
	
	private final String name;
	private final WorkerCountingThreadExecutor exe;
	private final DefUseGraphModifier modifier;
	
	/* Output simplifications
	 * Anything not a primitive type or a string type is given the value ALL
	 * Anything with no values of the locals is given the value NO (these will likely indicate error cases in the code)
	 * When a loop is detected, the value given is ALL to stop the loop propagation
	 * 
	 * Parameter refs are assigned the ALL
	 * lengthof expressions are given the value ALL
	 * instanceof expressions are given the value ALL
	 * new, newarray, and newmultiarray expressions are all given the value ALL
	 * castexpr just pass through the data without doing anything
	 * if a locals values are a string of primitive constants and a ALL then just return the ALL
	 * android.os.Bundle method return values always result in ALL
	 * If the local has values that are all primitve constants but one which is ALL they simplify to ALL
	 * ALL[num] ArrayRefs are just replaced with ALL
	 * If a local has a value of NULL with other values then the NULL value is removed
	 * 
	 * All Pairs when one op is a null constant are removed
	 * All Pairs where both ops are a primitive constant, ALL, or NO are removed
	 * All Pairs where the values are equal are removed
	 */
	public ACMiner() {
		this.name = getClass().getSimpleName();
		this.exe = new WorkerCountingThreadExecutor(new ValueWorkerFactory<>());
		this.modifier = new DefUseGraphModifier(exe);
		DataWrapper.initCache();
	}
	
	public boolean shutdownWhenFinished() {
		//calls shutdownWhenFinished on the exe and clears the other caches
		boolean ret = modifier.shutdownWhenFinished();
		DataWrapper.clearCache();
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
	
	public DefUseGraph makeDefUseGraph(Set<Unit> seeds, EntryPoint ep, IJimpleICFG icfg, ILogger logger) {
		return modifier.modDefUseGraph(seeds, ep, icfg, logger);
	}
	
	public Map<StartNode,List<ValuePair>> mineData(EntryPoint ep, DefUseGraph graph, IACMinerDataAccessor dataAccessor, ILogger logger) {
		return mineData(ep, graph, false, dataAccessor, logger);
	}
	
	public Map<StartNode,List<ValuePair>> mineData(EntryPoint ep, DefUseGraph graph, boolean subCapture, IACMinerDataAccessor dataAccessor, 
			ILogger logger) {
		try {
			logger.debug("{}: Mining simple data for ep '{}'.",name,ep);
			
			Cache<INode,Set<DataWrapper>> cache = IDESolver.DEFAULT_CACHE_BUILDER.build();
			Map<INode,ValuePairHashSet> results = new HashMap<>();
			for(StartNode sn : graph.getStartNodes()) {
				results.put(sn, new ValuePairHashSet());
			}
			SimpleValueWorkerGroup g = new SimpleValueWorkerGroup(name,ep+"_ACMiner",logger,false,false);
			for(StartNode sn : graph.getStartNodes()) {
				executeRunner(new MinerRunner(ep, new DataNode(sn, null, null, graph, dataAccessor, subCapture), 
						graph, cache, results, subCapture, false, dataAccessor, logger), g, logger);
			}
			g.unlockInitialLock();
			g.getReturnValue();//block
			
			if(g.shutdownNormally() && !g.hasExceptions()) {
				Map<StartNode,List<ValuePair>> resultsSorted = new HashMap<>();
				for(StartNode sn : graph.getStartNodes()) {
					ValuePairHashSet vp = results.get(sn);
					if(vp == null) {
						resultsSorted.put(sn, Collections.<ValuePair>emptyList());
					} else {
						synchronized(vp) {
							List<ValuePair> temp = new ArrayList<>(vp);
							Collections.sort(temp);
							resultsSorted.put(sn, temp);
						}
					}
				}
				resultsSorted = SortingMethods.sortMapKeyAscending(resultsSorted);
				logger.debug("{}: Successfully mined data for ep '{}'.",name,ep);
				return resultsSorted;
			} else {
				logger.fatal("{}: Failed mine simple data for group '{}'.",name,g.getName());
				throw new IgnorableRuntimeException();
			}
		} catch(IgnorableRuntimeException t) {
			throw t;
		} catch(Throwable t) {
			logger.fatal("{}: An unexpected error occured while mining simple data for ep '{}'.",t,name,ep);
			throw new IgnorableRuntimeException();
		}
	}
	
	public Map<INode,List<ValuePair>> mineAdditionalData(EntryPoint ep, DefUseGraph graph, Set<INode> startNodes, IACMinerDataAccessor dataAccessor, 
			ILogger logger) {
		try {
			logger.debug("{}: Mining additional simple data for ep '{}'.",name,ep);
			
			Cache<INode,Set<DataWrapper>> cache = IDESolver.DEFAULT_CACHE_BUILDER.build();
			Map<INode,ValuePairHashSet> results = new HashMap<>();
			for(INode sn : startNodes) {
				results.put(sn, new ValuePairHashSet());
			}
			SimpleValueWorkerGroup g = new SimpleValueWorkerGroup(name,ep+"_additionalACMiner",logger,false,false);
			for(INode sn : startNodes) {
				executeRunner(new MinerRunner(ep, new DataNode(sn, null, null, graph, dataAccessor, false), 
						graph, cache, results, false, true, dataAccessor, logger), g, logger);
			}
			g.unlockInitialLock();
			g.getReturnValue();//block
			
			if(g.shutdownNormally() && !g.hasExceptions()) {
				Map<INode,List<ValuePair>> resultsSorted = new HashMap<>();
				for(INode sn : startNodes) {
					ValuePairHashSet vp = results.get(sn);
					if(vp == null) {
						resultsSorted.put(sn, Collections.<ValuePair>emptyList());
					} else {
						synchronized(vp) {
							List<ValuePair> temp = new ArrayList<>(vp);
							Collections.sort(temp);
							resultsSorted.put(sn, temp);
						}
					}
				}
				resultsSorted = SortingMethods.sortMapKeyAscending(resultsSorted);
				logger.debug("{}: Successfully mined additional data for ep '{}'.",name,ep);
				return resultsSorted;
			} else {
				logger.fatal("{}: Failed to mine additional simple data for group '{}'.",name,g.getName());
				throw new IgnorableRuntimeException();
			}
		} catch(IgnorableRuntimeException t) {
			throw t;
		} catch(Throwable t) {
			logger.fatal("{}: An unexpected error occured while mining simple data for ep '{}'.",t,name,ep);
			throw new IgnorableRuntimeException();
		}
	}
	
	private static final class DataNode {
		
		private DataNode parent;
		private LocalWrapper parentLocal;
		private INode node;
		private Map<LocalWrapper, Set<DataWrapper>> resolvedData;
		private Map<InlineConstantLocalWrapper, InlineConstantLeafNode> inlineConstantData;
		private IACMinerDataAccessor dataAccessor;
		private long count;
		private final long total;
		private final boolean subCapture;
		
		public DataNode(INode node, DataNode parent, LocalWrapper parentLocal, DefUseGraph graph, IACMinerDataAccessor dataAccessor, 
				boolean subCapture) {
			this.parent = parent;
			this.parentLocal = parentLocal;
			this.node = node;
			this.resolvedData = new HashMap<>();
			this.inlineConstantData = new HashMap<>();
			this.subCapture = subCapture;
			this.dataAccessor = dataAccessor;
			this.count = 0;
			long t = 0;
			Map<LocalWrapper, Set<INode>> children = graph.getChildLocalWrappersToChildNodes(node);
			for(LocalWrapper lw : children.keySet()) {
				resolvedData.put(lw, new HashSet<DataWrapper>());
				t += children.get(lw).size();
			}
			Map<InlineConstantLocalWrapper, InlineConstantLeafNode> inlineConstants = graph.getInlineConstantNodes(node);
			for(InlineConstantLocalWrapper lw : inlineConstants.keySet()) {
				inlineConstantData.put(lw, inlineConstants.get(lw));
			}
			this.total = t;
		}
		
		public synchronized boolean seenBefore(INode val) {
			checkClearedData();
			Objects.requireNonNull(val);
			DataNode cur = this;
			while(cur != null) {
				if(cur.getCurrentNode().equals(val))
					return true;
				cur = cur.getParent();
			}
			return false;
		}
		
		public synchronized LocalWrapper getParentLocalWrapper() {
			checkClearedData();
			return parentLocal;
		}
		
		public synchronized DataNode getParent() {
			checkClearedData();
			return parent;
		}
		
		public synchronized INode getCurrentNode() {
			checkClearedData();
			return node;
		}
		
		public synchronized boolean isStart() {
			checkClearedData();
			return parent == null;
		}
		
		public synchronized INode getStartNode() {
			checkClearedData();
			DataNode cur = this;
			while(cur != null) {
				if(cur.isStart())
					return cur.getCurrentNode();
				cur = cur.getParent();
			}
			return null;
		}
		
		//Increments the count and returns true if this is the last child node to be finished
		//This is for the child nodes only
		//They will increment this count when they are finished processing their data
		//The last one needs to reinsert the parent onto the queue to be processed
		public synchronized boolean incCount() {
			checkClearedData();
			return ++count >= total;//Increment and get the updated value of count
		}
		
		public synchronized boolean childrenFinished() {
			checkClearedData();
			return count >= total;
		}
		
		public synchronized void addResolvedData(LocalWrapper lw, DataWrapper entry) {
			checkClearedData();
			Set<DataWrapper> curData = resolvedData.get(lw);
			if(curData != null)
				curData.add(entry);
		}
		
		public synchronized void addResolvedData(LocalWrapper lw, Set<DataWrapper> data) {
			checkClearedData();
			Set<DataWrapper> curData = resolvedData.get(lw);
			if(curData != null)
				curData.addAll(data);
		}
		
		public synchronized void finalizeResults() {
			for(LocalWrapper lw : resolvedData.keySet()) {
				Set<DataWrapper> data = resolvedData.get(lw);
				boolean hasAll = false;
				boolean allOtherNumbers = true;
				boolean hasNull = false;
				boolean othersBesidesNull = false;
				for(DataWrapper dw : data) {
					if(dw.isAllValueConstant() || dw.isNoValueConstant()) {
						hasAll = true;
					} else if(!(dw instanceof PrimitiveConstant)) {
						allOtherNumbers = false;
					}
					if(dw.isNullConstant()) {
						hasNull = true;
					} else {
						othersBesidesNull = true;
					}
				}
				if(hasAll && allOtherNumbers) {
					resolvedData.put(lw, Collections.singleton(DataWrapper.getAllConstant()));
				} else if(hasNull && othersBesidesNull) {
					for(Iterator<DataWrapper> it = data.iterator(); it.hasNext();) {
						DataWrapper dw = it.next();
						if(dw.isNullConstant())
							it.remove();
					}
				}
			}
			
			//Make sure all possible ep invoke statements are just resolved to their eps for simplicity
			for(LocalWrapper lw : resolvedData.keySet()) {
				Set<DataWrapper> data = resolvedData.get(lw);
				Set<DataWrapper> newData = new HashSet<>();
				for(DataWrapper dw : data) {
					newData.addAll(replaceBinderInvokesWithEps(dw, dataAccessor));
				}
				resolvedData.put(lw, newData);
			}
		}
		
		public synchronized List<ValuePair> getResolvedStringsForStartNodes(ILogger logger) {
			checkClearedData();
			if(!(node instanceof StartNode))
				throw new RuntimeException("Error: We should only be calling this method from start nodes.");
			logger.debug("DataNode-{}: Getting start node strings for '{}':",hashCode(),node.toString());
			List<ValuePair> ret = new ArrayList<>();
			if(node instanceof InvokeStartNode || node instanceof FieldStartNode) {
				//The returned sets should just end up being a bunch of singleton sets since these are not used in an if or switch
				List<ILocalWrapper> usedLocals = getUsedLocals(node);
				if(usedLocals.isEmpty()) {
					Type t = null;
					if(node instanceof InvokeStartNode) {
						SootMethod sm = ((InvokeStartNode)node).getTarget();
						if(sm == null)
							t = ((Stmt)node.getUnit()).getInvokeExpr().getMethodRef().returnType();
						else
							t = sm.getReturnType();
					} else if(node instanceof FieldStartNode) {
						SootField sf = ((FieldStartNode)node).getField();
						if(sf == null)
							t = ((Stmt)node.getUnit()).getFieldRef().getFieldRef().type();
						else
							t = sf.getType();
					}
					ret.add(ValuePair.make(DataWrapper.getConstantOrVariable(
							node.getIdentifier().clone(), null, t, node.getSource(), node.getUnit()),
							node.getSource(),node.getUnit(),node.toString()));
				} else {
					Set<List<DataWrapper>> subGroups = getSubstitutionGroups(usedLocals);
					Set<DataWrapper> values = performSubstitution(subGroups);
					for(DataWrapper s : values) {
						ret.add(ValuePair.make(s,node.getSource(),node.getUnit(),node.toString()));
					}
				}
			} else {
				Unit unit = node.getUnit();
				List<ILocalWrapper> usedLocals = getUsedLocals(node);
				Set<List<DataWrapper>> subGroups = getSubstitutionGroups(usedLocals);
				if(unit instanceof IfStmt) {
					if(usedLocals.size() != 2)
						throw new RuntimeException("Error: The if stmt '" + unit + "' does not have exactly two substitution variables!?!");
					if(subGroups.isEmpty()) {
						ret.add(ValuePair.make(DataWrapper.getNoneConstant(), DataWrapper.getNoneConstant(), 
								node.getSource(), node.getUnit(),node.toString()));
					} else {
						for(List<DataWrapper> group : subGroups) {
							if(group.size() != 2)
								throw new RuntimeException("Error: The if stmt '" + unit 
										+ "' produced a group with a size not equals to 2 '" + group + "' !?!");
							ret.add(ValuePair.make(group.get(0), group.get(1), node.getSource(), node.getUnit(), node.toString()));
						}
					}
				} else if(unit instanceof SwitchStmt) {
					//Each group can have at most one value and 0 I guess if it is switch on a constant
					if(usedLocals.size() != 1)
						throw new RuntimeException("Error: The switch stmt '" + unit + "' has more than one used local!?!");
					if(subGroups.isEmpty()) {
						ret.add(ValuePair.make(DataWrapper.getNoneConstant(), node.getSource(), node.getUnit(), node.toString()));
					} else {
						for(List<DataWrapper> group : subGroups) {
							if(group.size() != 1)
								throw new RuntimeException("Error: The switch stmt '" + unit 
										+ "' produced a group with a size not equal to 1 '" + group + "'.");
							ret.add(ValuePair.make(group.get(0), node.getSource(), node.getUnit(), node.toString()));
						}
					}
					List<ValuePair> newRet = new ArrayList<>();
					Set<Integer> cases = new SwitchWrapper(((SwitchStmt)unit)).getCases();
					for(ValuePair vp : ret) {
						if(vp.size() != 1)
							throw new RuntimeException("Error: A pair for switch stmt '" + unit 
									+ "' has a size of '" + vp.size() + "'. Not 1.");
						for(Integer i : cases) {
							if(i != null) {//Skip the default case
								newRet.add(ValuePair.make(vp.getOp1(),PrimitiveConstant.getIntConstant(i),vp));
							}
						}
					}
					ret = newRet;
				} else {
					throw new RuntimeException("Error: Unsupported start node '" + node.toString() + "'.");
				}
			}
			
			ret = replaceBinderInvokesWithEps(ret, dataAccessor);
			ret = simplifyPairs(ret);
			
			logger.debug("DataNode-{}: Start node strings for '{}':",hashCode(),node.toString());
			for(ValuePair p : ret) {
				logger.debug("DataNode-{}:     ValuePair - '{}'",hashCode(),p);
			}
			
			clearData();
			
			if(ret.isEmpty()) 
				return Collections.emptyList();
			else if(ret.size() == 1)
				return Collections.singletonList(ret.get(0));
			else
				return ret;//No point in sorting here since it will just be joined with the other start nodes for this ep
		}
		
		public synchronized Set<DataWrapper> getResolvedStrings(EntryPoint ep, ILogger logger) {
			checkClearedData();
			if(node instanceof StartNode)
				throw new RuntimeException("Error: We should only be calling this method from the child nodes.");
			
			logger.debug("DataNode-{}: Getting resolved strings for '{}'.",hashCode(),node.toString());
			
			if(node instanceof IInvokeNode) {
				SootMethod m = ((IInvokeNode)node).getTarget();
				SootClass d;
				if(m == null)
					d = ((Stmt)node.getUnit()).getInvokeExpr().getMethodRef().declaringClass();
				else
					d = m.getDeclaringClass();
				if(d.toString().equals("android.os.Bundle")) {
					logger.debug("DataNode-{}: Resolved string is All because it is an ignored class '{}'.",hashCode(),d);
					return Collections.singleton(DataWrapper.getAllConstant());
				}
			}
			
			Set<DataWrapper> ret;
			List<ILocalWrapper> usedLocals = getUsedLocals(node);
			if(usedLocals.isEmpty()) {
				//If there is nothing to substitute just return the basic string of the node
				Value v = node.getValue();
				DataWrapper s;
				if(v != null && v instanceof ParameterRef && !(subCapture && Objects.equals(ep.getEntryPoint(), node.getSource())))
					s = DataWrapper.getAllConstant();
				else
					s = DataWrapper.getConstantOrVariable(node.getIdentifier().clone(), node.getValue(), node.getValue().getType(), 
							node.getSource(), node.getUnit());
				logger.debug("DataNode-{}: Resolved string is leaf node '{}'.",hashCode(),s);
				ret = Collections.singleton(s);
			} else {
				Set<List<DataWrapper>> subGroups = getSubstitutionGroups(usedLocals);
				logger.debug("DataNode-{}: Resolved string groups for '{}':",hashCode(),node.toString());
				for(List<DataWrapper> l : subGroups) {
					StringBuilder sb = new StringBuilder();
					sb.append("[");
					boolean first = true;
					for(DataWrapper d : l) {
						if(first)
							first = false;
						else
							sb.append(", ");
						sb.append(d.toString()).append(" ").append(d.getClass().getSimpleName());
					}
					logger.debug("DataNode-{}:     Group - '{}'",hashCode(),sb.toString());
				}
				ret = performSubstitution(subGroups);
				logger.debug("DataNode-{}: Resolved strings for '{}':",hashCode(),node.toString());
				for(DataWrapper s : ret) {
					logger.debug("DataNode-{}:     RS - '{}' '{}'",hashCode(),s,s.getClass().getSimpleName());
				}
			}
			
			clearData();
			return ret;
		}
		
		private final void checkClearedData() {
			if(resolvedData == null)
				throw new RuntimeException("Error: Attempting to access the data of a finished and cleared node.");
		}
		
		private final void clearData() {
			parent = null;
			parentLocal = null;
			node = null;
			resolvedData = null;
			inlineConstantData = null;
			dataAccessor = null;
		}
		
		private Set<DataWrapper> performSubstitution(Set<List<DataWrapper>> subGroups) {
			Set<DataWrapper> ret = new HashSet<>();
			Value v = node.getValue();
			//Local and local casting should already be removed from the def use graph
			if(v != null && v instanceof BinopExpr) {
				BinopExpr expr = (BinopExpr)v;
				for(List<DataWrapper> group : subGroups) {
					if(group.size() != 2) {
						throw new RuntimeException("Error: The size of the group is not 2 for binop expr '" + v + "'!?!");
					} else {
						ret.add(DataWrapper.getPrimitiveFromBinop(group.get(0), group.get(1), parentLocal.getLocal().getType(), expr));
					}
				}
			} else if(v != null && v instanceof NegExpr) {
				for(List<DataWrapper> group : subGroups) {
					if(group.size() != 1) {
						throw new RuntimeException("Error: The size of the group is not 1 for the neg expr '" + v + "'!?!");
					} else {
						ret.add(DataWrapper.getPrimitiveFromNegExpr(group.get(0), parentLocal.getLocal().getType()));
					}
				}
			} else if(v != null && (v instanceof LengthExpr || v instanceof InstanceOfExpr || v instanceof AnyNewExpr)) {
				ret.add(DataWrapper.getAllConstant());
			} else if(v != null && v instanceof CastExpr) {//Assumes there is only one entry in each list
				for(List<DataWrapper> group : subGroups) {
					if(group.size() != 1) {
						throw new RuntimeException("Error: The size of the group is not 1 for the cast expr '" + v + "'!?!");
					} else {
						ret.add(group.get(0));
					}
				}
			} else if(v != null && v instanceof ArrayRef) {
				Identifier id = node.getIdentifier();
				for(List<DataWrapper> group : subGroups) {
					if(group.size() != 2) {
						throw new RuntimeException("Error: The size of the group is not 2 for the array ref expr '" + v + "'!?!");
					} else {
						DataWrapper op1 = group.get(0);
						if(op1.isAllValueConstant() || op1.isNoValueConstant()) {
							ret.add(op1);
						} else {
							Identifier cur = id.clone();
							int j = 0;
							for(int i = 0; i < cur.size(); i++) {
								Part p = cur.get(i);
								if(p instanceof LocalWrapperPart || p instanceof ConstantPart) {
									cur.set(i, new DataWrapperPart(group.get(j++)));
								}
							}
							ret.add(DataWrapper.getConstantOrVariable(cur, null, v.getType(), node.getSource(), node.getUnit()));
						}
					}
				}
			} else {
				Identifier id = node.getIdentifier();
				for(List<DataWrapper> group : subGroups) {
					Identifier cur = id.clone();
					int j = 0;
					for(int i = 0; i < cur.size(); i++) {
						Part p = cur.get(i);
						if(p instanceof LocalWrapperPart || p instanceof ConstantPart) {
							cur.set(i, new DataWrapperPart(group.get(j++)));
						}
					}
					Type t;
					if(v != null) {
						t = v.getType();//Everything but the start nodes
					} else {
						if(node instanceof InvokeStartNode) {
							SootMethod sm = ((InvokeStartNode)node).getTarget();
							if(sm == null)
								t = ((Stmt)node.getUnit()).getInvokeExpr().getMethodRef().returnType();
							else
								t = sm.getReturnType();
						} else if(node instanceof FieldStartNode) {
							SootField sf = ((FieldStartNode)node).getField();
							if(sf == null)
								t = ((Stmt)node.getUnit()).getFieldRef().getFieldRef().type();
							else
								t = sf.getType();
						} else if(node instanceof StartNode && node.getUnit() instanceof IfStmt) {
							t = ((IfStmt)node.getUnit()).getCondition().getType();
						} else if(node instanceof StartNode && node.getUnit() instanceof SwitchStmt) {
							t =((SwitchStmt)node.getUnit()).getKey().getType();
						} else {
							throw new RuntimeException("Error: Could not determine the type of node '" + node + 
									"'. It does not have a value and is not a start node!?!");
						}
					}
					ret.add(DataWrapper.getConstantOrVariable(cur, null, t, node.getSource(), node.getUnit()));
				}
			}
			
			if(ret.isEmpty()) {
				return Collections.emptySet();
			} else if(ret.size() == 1) {
				return Collections.singleton(ret.iterator().next());
			} else {
				boolean hasAll = false;
				boolean allPrimCon = true;
				for(DataWrapper dw : ret) {
					if(dw instanceof AllConstant)
						hasAll = true;
					else if(!(dw instanceof PrimitiveConstant))
						allPrimCon = false;
				}
				if(hasAll && allPrimCon)
					return Collections.singleton(DataWrapper.getAllConstant());
				return ret;
			}
		}
		
		//Note what this computes is a cartesian product for all the possible values of the variables
		@SuppressWarnings({ "unchecked", "rawtypes" })
		private Set<List<DataWrapper>> getSubstitutionGroups(List in) {
			//Take care of all single substitutions quickly so as to not waste memory
			List start = new ArrayList(in);
			int maxRequiredArrays = 1;
			for(int i = 0; i < start.size(); i++) {
				Object o = start.get(i);
				if(o instanceof LocalWrapper) {
					Set<DataWrapper> data = resolvedData.get(o);
					if(data == null || data.isEmpty())
						start.set(i, DataWrapper.getNoneConstant());
					else if(data.size() == 1)
						start.set(i, data.iterator().next());
					else
						//There will still exist a local wrapper after this loop finishes
						maxRequiredArrays = IntMath.checkedMultiply(maxRequiredArrays, data.size());
				} else if(o instanceof InlineConstantLocalWrapper) {
					Constant c = inlineConstantData.get(o).getValue();
					start.set(i, DataWrapper.getConstantOrVariable(null, c, c.getType(), node.getSource(), node.getUnit()));
				}
			}
			Set<List<DataWrapper>> subGroups;
			if(maxRequiredArrays <= 1) {
				subGroups = Collections.<List<DataWrapper>>singleton(start);
			} else {
				subGroups = new HashSet<>();
				List<List<Object>> allArrays = new ArrayList<>(maxRequiredArrays);
				for(int i = 0; i < maxRequiredArrays; i++) {
					allArrays.add(new ArrayList<Object>(start));
				}
				
				int repOfValue = maxRequiredArrays;
				int repGroup = 1;
				for(int i = 0; i < start.size(); i++) {
					Object o = start.get(i);
					if(o instanceof LocalWrapper) {
						Set<DataWrapper> data = resolvedData.get(o);
						int prevRepOfValue = repOfValue;
						repOfValue = repOfValue / data.size();
						for(int g = 0; g < repGroup; g++) {
							int c = 0;
							for(DataWrapper s : data) {
								for(int r = 0; r < repOfValue; r++) {
									allArrays.get((g * prevRepOfValue) + (c * repOfValue) + r).set(i, s);
								}
								c++;
							}
						}
						repGroup = repGroup * data.size();
					}
				}
				subGroups.addAll((List)allArrays);
			}
			return subGroups;
		}
		
		private List<ILocalWrapper> getUsedLocals(INode cur) {
			List<ILocalWrapper> ret = new ArrayList<>();
			Identifier id = cur.getIdentifier();
			for(int i = 0; i < id.size(); i++) {
				Part p = id.get(i);
				if(p instanceof LocalWrapperPart) {
					ret.add(((LocalWrapperPart)p).getLocalWrapper());
				} else if(p instanceof ConstantPart) {
					for(InlineConstantLocalWrapper lw : inlineConstantData.keySet()) {
						InlineConstantLeafNode n = inlineConstantData.get(lw);
						if(n.getIndex() == i && n.getValue().equals(((ConstantPart)p).getValue())) {
							ret.add(lw);
							break;
						}
					}
				} else if(p instanceof LocalPart) {
					throw new RuntimeException("Error: There is a LocalPart in the node '" + cur.toString() + "'");
				}
			}
			return ret;
		}
	}
	
	private class MinerRunner extends SimpleValueRunner {
		private EntryPoint ep;
		private DefUseGraph graph;
		private DataNode cur;
		private ILogger logger;
		private List<ValueRunner<Object>> runners;
		private Cache<INode,Set<DataWrapper>> cache;
		private Map<INode,ValuePairHashSet> results;
		private IACMinerDataAccessor dataAccessor;
		private final boolean subCapture;
		private final boolean isAdditional;
		
		public MinerRunner(EntryPoint ep, DataNode cur, DefUseGraph graph, Cache<INode,Set<DataWrapper>> cache, 
				Map<INode,ValuePairHashSet> results, boolean subCapture, boolean isAdditional, IACMinerDataAccessor dataAccessor, ILogger logger) {
			this.ep = ep;
			this.graph = graph;
			this.cur = cur;
			this.logger = logger;
			this.runners = new ArrayList<>();
			this.cache = cache;
			this.results = results;
			this.subCapture = subCapture;
			this.dataAccessor = dataAccessor;
			this.isAdditional = isAdditional;
		}
		
		private void cleanup() {
			this.ep = null;
			this.graph = null;
			this.cur = null;
			this.logger = null;
			this.runners = null;
			this.cache = null;
			this.results = null;
			this.dataAccessor = null;
		}

		@Override
		public void run() {
			INode currentNode = cur.getCurrentNode();
			DataNode parent = cur.getParent();
			LocalWrapper parentLocalWrapper = cur.getParentLocalWrapper();
			INode startNode = cur.getStartNode();
			try {
				StringBuilder sb = new StringBuilder();
				if(!cur.childrenFinished()) {
					sb.append("Visiting First Node='").append(currentNode.toString()).append("' Parent='")
						.append(parent == null ? "NULL" : parent.getCurrentNode().toString())
						.append("' StartNode='").append(startNode.toString()).append("' EP='").append(ep).append("'\n");
					//This is our first pass of this node where we add the children to be explored
					Map<LocalWrapper, Set<INode>> childrenMap = graph.getChildLocalWrappersToChildNodes(currentNode);
					for(LocalWrapper lw : childrenMap.keySet()) {
						Set<INode> children = childrenMap.get(lw);
						if(!isAllowedType(lw.getLocal().getType())) {
							sb.append("    Local '").append(lw.toString()).append("' has denied type '").append(lw.getLocal().getType())
								.append("'. Skipping all children.\n");
							cur.addResolvedData(lw, DataWrapper.getAllConstant());
							for(INode child : children) {
								sb.append("    Denied local type. Skipping child '").append(child.toString())
									.append("' for '").append(lw.toString()).append("'\n");
								if(cur.incCount()) {
									sb.append("    The current node has no more children to explore. Re-Adding to queue for second pass\n");
									runners.add(new MinerRunner(ep, cur, graph, cache, results, subCapture, isAdditional, dataAccessor, logger));
								}
							}
						} else {
							if(children.isEmpty()) {
								//No need to worry about the count here since total only includes the child nodes
								sb.append("    No child nodes for local '").append(lw.toString()).append("'\n");
								cur.addResolvedData(lw, DataWrapper.getNoneConstant());
							} else {
								for(INode child : children) {
									Set<DataWrapper> res = null;
									synchronized(cache) {
										res = cache.getIfPresent(child);
									}
									if(res == null) {
										//Node is not cached yet so generate it's data
										if(cur.seenBefore(child)) {//Avoid cycles in the graph
											sb.append("    Cycle detected. Have seen before child node '").append(child.toString())
												.append("' for '").append(lw.toString()).append("'\n");
											cur.addResolvedData(lw, DataWrapper.getAllConstant());
											//Since this was the last child, we must re-add the node to the queue to process the results
											if(cur.incCount()) {
												sb.append("    The current node has no more children to explore. Re-Adding to queue for second pass\n");
												runners.add(new MinerRunner(ep, cur, graph, cache, results, subCapture, isAdditional, dataAccessor, logger));
											}
										} else {
											sb.append("    Adding to queue child node '").append(child.toString())
												.append("' for '").append(lw.toString()).append("'\n");
											runners.add(new MinerRunner(ep, new DataNode(child, cur, lw, graph, dataAccessor, subCapture), graph, 
													cache, results, subCapture, isAdditional, dataAccessor, logger));
										}
									} else {
										sb.append("    Already computed data for child node '").append(child.toString())
											.append("' for '").append(lw.toString()).append("'\n");
										cur.addResolvedData(lw, res);
										if(cur.incCount()) {//Since this was the last child, we must re-add the node to the queue to process the results
											sb.append("    The current node has no more children to explore. Re-Adding to queue for second pass\n");
											runners.add(new MinerRunner(ep, cur, graph, cache, results,  subCapture, isAdditional, dataAccessor, logger));
										}
									}
								}
							}
						}
					}
				} else {
					sb.append("Visiting Second Node='").append(currentNode.toString()).append("' Parent='")
						.append(parent == null ? "NULL" : parent.getCurrentNode().toString())
						.append("' StartNode='").append(startNode.toString()).append("' EP='").append(ep).append("'\n");
					cur.finalizeResults();
					if(cur.isStart()) {
						sb.append("    Computing resolved data for start node.");
						INode curNode = cur.getCurrentNode();
						ValuePairHashSet curRes = results.get(curNode);
						List<ValuePair> listRes;
						if(isAdditional) {
							listRes = new ArrayList<>();
							Set<DataWrapper> resolvedData = cur.getResolvedStrings(ep, logger);//Cannot use cur after this call
							for(DataWrapper dw : resolvedData) {
								listRes.add(ValuePair.make(dw,curNode.getSource(),curNode.getUnit(),curNode.toString()));
							}
						} else {
							listRes = cur.getResolvedStringsForStartNodes(logger);//Cannot use cur after this call
						}
						synchronized(curRes) {
							curRes.addAll(listRes);
						}
						sb.append("    Finished computing resolved data for start node.");
					} else {
						sb.append("    Computing resolved data.");
						Set<DataWrapper> resolvedData = cur.getResolvedStrings(ep, logger);//Cannot use cur after this call
						
						synchronized(cache) {
							if(cache.getIfPresent(currentNode) == null)
								cache.put(currentNode, resolvedData);
						}
						parent.addResolvedData(parentLocalWrapper, resolvedData);
						sb.append("    Finished computing resolved data.");
						if(parent.incCount()) {//Since this was the last child, we must re-add the node to the queue to process the results
							sb.append("    The parent node has no more children to explore. Re-Adding to queue for second pass\n");
							runners.add(new MinerRunner(ep, parent, graph, cache, results, subCapture, isAdditional, dataAccessor, logger));
						}
					}
				}
				
				if(!runners.isEmpty())
					executeRunners(runners, logger);
				logger.debug("{}: {}",name,sb.toString());
				cleanup();
			} catch(IgnorableRuntimeException e) {
				throw e;
			} catch(Throwable t) {
				logger.fatal("{}: Unexpected exception while processing node '{}' of startNode '{}' for ep '{}'.",t,
						name,currentNode,startNode,ep);
				throw new IgnorableRuntimeException();
			}
		}
	}
	
	private final Set<String> allowedClassTypes = ImmutableSet.<String>of("java.lang.String", "java.lang.Integer", "java.lang.Long", 
			"java.lang.Short", "java.lang.Byte", "java.lang.Float", "java.lang.Double", "java.lang.Boolean", "java.lang.Character",
			"java.math.BigDecimal", "java.math.BigInteger",
			"java.lang.Number", "java.util.concurrent.atomic.AtomicInteger", "java.util.concurrent.atomic.AtomicLong");
	
	public boolean isAllowedType(Type type) {
		if(type instanceof ArrayType) {
			return isAllowedType(((ArrayType)type).baseType);
		} else if(type instanceof PrimType) {
			return true;
		} else if(type instanceof RefType) {
			return allowedClassTypes.contains(((RefType)type).getClassName());
		} else {
			return false;
		}
	}
	
	public static List<ValuePair> simplifyBooleanChecks(List<ValuePair> pairs) {
		for(int i = 0; i < pairs.size(); i++) {
			ValuePair p = pairs.get(i);
			if(p.size() == 2) {
				DataWrapper op1 = p.getOp1();
				DataWrapper op2 = p.getOp2();
				boolean isBoolCheck1 = isBooleanCheck(op1);
				boolean isBoolCheck2 = isBooleanCheck(op2);
				if(isBoolCheck1 && !isBoolCheck2)
					pairs.set(i, ValuePair.make(op1,p));
				else if(!isBoolCheck1 && isBoolCheck2)
					pairs.set(i, ValuePair.make(op2,p));
			}
		}
		return pairs;
	}
	
	public static boolean isBooleanCheck(DataWrapper in) {
		Type boolType = BooleanType.v();
		for(Part p : in.getIdentifier()) {
			if((p instanceof MethodRefPart && ((MethodRefPart)p).getMethodRef().returnType().equals(boolType)) || 
					(p instanceof FieldRefPart && ((FieldRefPart)p).getFieldRef().type().equals(boolType)))
				return true;
		}
		return false;
	}
	
	public static List<ValuePair> removeUnwantedPairs(List<ValuePair> pairs) {
		for(Iterator<ValuePair> it = pairs.iterator(); it.hasNext();) {
			ValuePair p = it.next();
			if(p.isPairWithSameValues() || p.isPrimitiveVSPrimitveCheck() || p.isNullCheck() || p.size() == 0)
				it.remove();
		}
		return pairs;
	}
	
	//Simplifies the equals method  calls
	public static List<ValuePair> simplifyEqualsPairs(List<ValuePair> pairs) {
		for(int i = 0; i < pairs.size(); i++) {
			ValuePair p = pairs.get(i);
			if(p.size() == 1) {
				DataWrapper op = p.getOp1();
				List<DataWrapper> eqOps = getArgsOfMethodSubSig(op, "boolean equals(java.lang.Object)");
				if(eqOps != null) {
					if(eqOps.size() == 2)
						pairs.set(i, ValuePair.make(eqOps.get(0),eqOps.get(1),p));
					else
						throw new RuntimeException("Error: Found an equals method '" 
								+ p.toString() + "' with only '" + eqOps.size() + " args.");
				}
			} else if(p.size() == 2) {
				DataWrapper op1 = p.getOp1();
				DataWrapper op2 = p.getOp2();
				List<DataWrapper> eq1Ops = getArgsOfMethodSubSig(op1, "boolean equals(java.lang.Object)");
				List<DataWrapper> eq2Ops = getArgsOfMethodSubSig(op2, "boolean equals(java.lang.Object)");
				if(eq1Ops != null && eq2Ops == null) {
					if(eq1Ops.size() == 2)
						pairs.set(i, ValuePair.make(eq1Ops.get(0),eq1Ops.get(1),p));
					else
						throw new RuntimeException("Error: Found an equals method '" 
								+ p.toString() + "' with only '" + eq1Ops.size() + " args.");
				} else if(eq1Ops == null && eq2Ops != null) {
					if(eq2Ops.size() == 2)
						pairs.set(i, ValuePair.make(eq2Ops.get(0),eq2Ops.get(1),p));
					else
						throw new RuntimeException("Error: Found an equals method '" 
								+ p.toString() + "' with only '" + eq2Ops.size() + " args.");
				}
				//Either neither are equal or both are
				//Not sure if both are is even possible so just leave it alone
			}
		}
		return pairs;
	}
	
	public static List<DataWrapper> getArgsOfMethodSubSig(DataWrapper op, String subSig) {
		List<DataWrapper> ret = new ArrayList<>();
		int found = 0;
		for(Part p : op.getIdentifier()) {
			if(p instanceof DataWrapperPart) {
				ret.add(((DataWrapperPart)p).getDataWrapper());
			} else if(p instanceof MethodRefPart && ((MethodRefPart)p).getMethodRef().getSubSignature().toString().equals(subSig)) {
				found++;
			}
		}
		if(found == 1)
			return ret;
		return null;
	}
	
	public static List<ValuePair> replaceBinderInvokesWithEps(List<ValuePair> in, IACMinerDataAccessor dataAccessor) {
		List<ValuePair> ret = new ArrayList<>();
		for(ValuePair vp : in) {
			if(vp.size() == 1) {
				for(DataWrapper dw : replaceBinderInvokesWithEps(vp.getOp1(), dataAccessor)) {
					ret.add(ValuePair.make(dw,vp));
				}
			} else if(vp.size() == 2) {
				Set<DataWrapper> dws1 = replaceBinderInvokesWithEps(vp.getOp1(), dataAccessor);
				Set<DataWrapper> dws2 = replaceBinderInvokesWithEps(vp.getOp2(), dataAccessor);
				for(DataWrapper op1 : dws1) {
					for(DataWrapper op2 : dws2) {
						ret.add(ValuePair.make(op1,op2,vp));
					}
				}
			}
		}
		return ret;
	}
	
	public static Set<DataWrapper> replaceBinderInvokesWithEps(DataWrapper dw, IACMinerDataAccessor dataAccessor) {
		Set<DataWrapper> ret = new HashSet<>();
		Identifier id = dw.getIdentifier();
		boolean found = false;
		for(int i = 0; i < id.size(); i++) {
			Part p = id.get(i);
			if(p instanceof MethodRefPart) {
				Set<SootMethod> reachableEps = dataAccessor.getEntryPointsFromBinderMethod(((MethodRefPart)p).getValue());
				if(reachableEps != null && !reachableEps.isEmpty()) {
					found = true;
					for(SootMethod sm : reachableEps) {
						DataWrapper newdw = dw.clone();
						MethodRefPart mrp = (MethodRefPart)newdw.getIdentifier().get(i);
						mrp.setMethodRef(sm.makeRef());
						mrp.setCurString(sm.getSignature());
						ret.add(newdw);
					}
				}
				break;//Only one method ref part per identifier (if we are not going down each data wrapper part)
			}
		}
		if(!found)
			ret.add(dw);
		return ret;
	}
	
	public static List<ValuePair> simplifyPairs(Collection<ValuePair> in) {
		List<ValuePair> ret = new ArrayList<>(in);
		ret = simplifyEqualsPairs(ret);
		ret = simplifyBooleanChecks(ret);
		ret = removeUnwantedPairs(ret);//Must be last to get rid of anything created by the others
		return ret;
	}

}
