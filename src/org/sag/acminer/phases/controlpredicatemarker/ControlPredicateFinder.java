package org.sag.acminer.phases.controlpredicatemarker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.concurrent.IgnorableRuntimeException;
import org.sag.common.concurrent.ValueRunner;
import org.sag.common.concurrent.ValueWorkerFactory;
import org.sag.common.concurrent.ValueWorkerGroup;
import org.sag.common.concurrent.WorkerCountingThreadExecutor;
import org.sag.common.logging.ILogger;
import org.sag.common.tools.SortingMethods;
import org.sag.common.tuple.Tuple;
import org.sag.soot.SootSort;
import org.sag.soot.callgraph.IJimpleICFG;

import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.DefinitionStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.SwitchStmt;
import soot.jimple.ThisRef;
import soot.jimple.internal.VariableBox;
import soot.toolkits.scalar.UnitValueBoxPair;

public class ControlPredicateFinder {

	private final String name;
	private final WorkerCountingThreadExecutor exe;
	private final String pad;
	
	public ControlPredicateFinder() {
		this.name = getClass().getSimpleName();
		this.exe = new WorkerCountingThreadExecutor(new ValueWorkerFactory<>());
		this.pad = genPad(24+9+name.length()+4);
	}
	
	public boolean shutdownWhenFinished() {
		return exe.shutdownWhenFinished();
	}
	
	public List<Throwable> getAndClearExceptions() {
		return exe.getAndClearExceptions();
	}
	
	public Map<SootMethod,Set<Unit>> findControlPredicates(Set<Unit> seeds, EntryPoint entryPoint, IJimpleICFG icfg, ILogger logger) {
		try {
			Set<Unit> allSeeds = new HashSet<>(seeds);
			Set<DefinitionStmt> defSeeds = new HashSet<>();
			Set<Unit> results = new HashSet<>();
			
			logger.debug("{}: Finding control predicates for ep '{}' with seeds:\n{}",name,entryPoint,formatCollection(pad, 
					seeds.iterator()));
			
			for(Unit u : seeds) {
				if(u instanceof DefinitionStmt)
					defSeeds.add((DefinitionStmt)u);
			}
			
			//perform the forward analysis
			if(!defSeeds.isEmpty()) {
				ValueWorkerGroup<Set<Unit>,Unit> gForward = new ValueWorkerGroup<Set<Unit>,Unit>(name,entryPoint.toString()+"_fwd",logger,false,false) {
					@Override protected Set<Unit> initReturnValue() {return new HashSet<>();}
					@Override protected void joinWorkerReturnValue(Unit value) {if(value != null) ret.add(value);}
					@Override protected void finalizeReturnValue() {}
				};
				Set<DataFwd> seen = Collections.<DataFwd>synchronizedSet(new HashSet<DataFwd>());
				logger.debug("{}: Starting the forward control predicate finder for group '{}' with the seeds:\n{}",name,gForward.getName(),
						formatCollection(pad,defSeeds.iterator()));
				for(DefinitionStmt u : defSeeds) {
					SootMethod uM = icfg.getMethodOf(u);
					DataFwd d = new DataFwd((Local)null,u,uM);
					executeRunner(new ForwardControlPredicateRunner(d, seen, icfg, logger),gForward,logger);
				}
				gForward.unlockInitialLock();
				Set<Unit> forwardResults = gForward.getReturnValue();
				logger.debug("{}: Forward control predicates found for group '{}':\n{}",name,gForward.getName(),
						formatCollection(pad, forwardResults.iterator()));
				if(gForward.shutdownNormally() && !gForward.hasExceptions()) {
					allSeeds.addAll(forwardResults);
					results.addAll(forwardResults);
					logger.debug("{}: Successfully completed the forward control predicate finder for group '{}'.",name,gForward.getName());
				} else {
					logger.fatal("{}: Failed to complete the forward control predicate finder for group '{}'.",name,gForward.getName());
					throw new IgnorableRuntimeException();
				}
			}
			
			//perform the backwards analysis using all the original seeds plus those generated in the forward analysis
			if(!allSeeds.isEmpty()) {
				ValueWorkerGroup<Set<Unit>,Unit> gBkwd = new ValueWorkerGroup<Set<Unit>,Unit>(name,entryPoint.toString()+"_bkwd",logger,false,false) {
					@Override protected Set<Unit> initReturnValue() {return new HashSet<>();}
					@Override protected void joinWorkerReturnValue(Unit value) {if(value != null) ret.add(value);}
					@Override protected void finalizeReturnValue() {}
				};
				Set<DataBkwd> seen = Collections.<DataBkwd>synchronizedSet(new HashSet<DataBkwd>());
				logger.debug("{}: Starting the backward control predicate finder for group '{}' with the seeds:\n{}",name,gBkwd.getName(),
						formatCollection(pad,allSeeds.iterator()));
				for(Unit u : allSeeds) {
					SootMethod uM = icfg.getMethodOf(u);
					DataBkwd d = new DataBkwd(u,uM,true);
					executeRunner(new BackwardControlPredicateRunner(d, seen, icfg, logger),gBkwd,logger);
				}
				gBkwd.unlockInitialLock();
				Set<Unit> bkwdResults = gBkwd.getReturnValue();
				logger.debug("{}: Backward control predicates found for group '{}':\n{}",name,gBkwd.getName(),
						formatCollection(pad, bkwdResults.iterator()));
				if(gBkwd.shutdownNormally() && !gBkwd.hasExceptions()) {
					results.addAll(bkwdResults);
					logger.debug("{}: Successfully completed the backward control predicate finder for group '{}'.",name,gBkwd.getName());
				} else {
					logger.fatal("{}: Failed to complete the backward control predicate finder for group '{}'.",name,gBkwd.getName());
					throw new IgnorableRuntimeException();
				}
			}
			
			Map<SootMethod,Set<Unit>> ret;
			if(results.isEmpty()) {
				ret = Collections.emptyMap();
			} else {
				ret = new HashMap<>();
				for(Unit u : results) {
					SootMethod source = icfg.getMethodOf(u);
					Set<Unit> s = ret.get(source);
					if(s == null) {
						s = new HashSet<>();
						ret.put(source, s);
					}
					s.add(u);
				}
				for(SootMethod m : ret.keySet()) {
					ret.put(m, SortingMethods.sortSet(ret.get(m),SootSort.unitComp));
				}
				ret = SortingMethods.sortMapKey(ret, SootSort.smComp);
			}
			
			logger.debug("{}: Control predicates for ep '{}':\n{}",name,entryPoint,formatMap(pad, ret));
			
			return ret;
		} catch(IgnorableRuntimeException t) {
			throw t;
		} catch(Throwable t) {
			logger.fatal("{}: An unexpected error occured while finding control predicates for ep '{}'.",t,name,entryPoint);
			throw new IgnorableRuntimeException();
		}
	}
	
	/*private Set<Value> getAllValues(AdvLocalDefs localDefs, SootMethod source, Unit cur, Value v, ILogger logger) {
		Set<Value> ret = new HashSet<>();
		if(v instanceof Local) {
			Set<DefinitionStmt> leftDefs = localDefs.getDefsWithAliases((Local)v, cur);
			for(DefinitionStmt def : leftDefs) {
				Value right = def.getRightOp();
				if(!(right instanceof Local) && !(right instanceof CastExpr))
					ret.add(right);
			}
		} else if(v instanceof Constant) {
			ret.add(v);
		} else {
			logger.fatal("{}: Unhandled type '{}' of value '{}' for Unit '{}' of source method '{}'.",
					name,v.getClass().getSimpleName(),v,cur,source);
			throw new IgnorableRuntimeException();
		}
		if(ret.isEmpty()) {
			logger.fatal("{}: No values for type '{}' of value '{}' for Unit '{}' of source method '{}'!?!",
					name,v.getClass().getSimpleName(),v,cur,source);
			throw new IgnorableRuntimeException();
		}
		return ret;
	}*/
	
	/*private boolean isRemovableValue(Value v, IControlPredicateFilterDatabase filterdb) {
		if(filterdb.isRemoveNullSet() && v instanceof NullConstant)
			return true;
		if(filterdb.isRemoveInstanceofSet() && v instanceof InstanceOfExpr)
			return true;
		if(filterdb.isRemoveLengthofSet() && v instanceof LengthExpr)
			return true;
		if(v instanceof InvokeExpr && filterdb.isInvokeSignature(((InvokeExpr)v).getMethodRef().getSignature()))
			return true;
		return false;
	}*/
	
	/*public Map<SootMethod,Set<Unit>> applyEndControlPredicateFilter(Map<SootMethod,Set<Unit>> cps, IJimpleICFG icfg, 
			IControlPredicateFilterDatabase filterdb, ILogger logger) {
		LoopFinder loopFinder = new LoopFinder();
		Map<SootMethod,Set<Unit>> ret = new HashMap<>();
		for(SootMethod source : cps.keySet()) {
			AdvLocalDefs localDefs = icfg.getOrMakeLocalDefs(source);
			Set<Stmt> loopHeaders = new HashSet<>();
			
			if(filterdb.isRemoveLoopHeaderSet()) {
				Set<Loop> loops = loopFinder.getLoops(icfg.getOrCreateUnitGraph(source));
				for(Loop l : loops) {
					loopHeaders.add(l.getHead());
				}
			}
			for(Unit cur : cps.get(source)) {
				boolean keepCP = true;
				if(cur instanceof IfStmt) {
					boolean keepCPInner = false; //if true then both values are not removable
					ConditionExpr cond = (ConditionExpr)(((IfStmt)cur).getCondition());
					Set<Value> leftValues = getAllValues(localDefs, source, cur, cond.getOp1(), logger);
					Set<Value> rightValues = getAllValues(localDefs, source, cur, cond.getOp2(), logger);
					String nonRemovableReason = null;
					for(Value leftV : leftValues) {
						for(Value rightV : rightValues) {
							if(!isRemovableValue(leftV, filterdb) && !isRemovableValue(rightV, filterdb)) {
								nonRemovableReason = "if (" + leftV + " " + cond.getSymbol() + " " + rightV + ")";
								keepCPInner = true;
								break;
							}
						}
						if(keepCPInner)
							break;
					}
					boolean isLoopHeader = loopHeaders.contains(cur);
					if(!keepCPInner || isLoopHeader) {
						logger.debug("{}: RemovableCP='{}' AllRemovable='{}' LoopHeader='{}'",name,cur,!keepCPInner,isLoopHeader);
						keepCP = false;
					} else {
						logger.debug("{}: NonRemovableCP='{}' because '{}'",name,cur,nonRemovableReason);
					}
				} else if(cur instanceof SwitchStmt) {
					Set<Value> keyValues = getAllValues(localDefs, source, cur, ((SwitchStmt)cur).getKey(), logger);
					boolean keepCPInner = false;
					String nonRemovableReason = null;
					for(Value keyV : keyValues) {
						if(!isRemovableValue(keyV, filterdb)) {
							nonRemovableReason = "switch (" + keyV + ")";
							keepCPInner = true;
							break;
						}
					}
					boolean isLoopHeader = loopHeaders.contains(cur);
					if(!keepCPInner || isLoopHeader) {
						logger.debug("{}: RemovableCP='{}' AllRemovable='{}' LoopHeader='{}'",name,cur,!keepCPInner,isLoopHeader);
						keepCP = false;
					} else {
						logger.debug("{}: NonRemovableCP='{}' because '{}'",name,cur,nonRemovableReason);
					}
				} else {
					logger.fatal("{}: The stmt '{}' of '{}' is not a if or switch but a '{}'!?!", name,cur,source,cur.getClass().getSimpleName());
					throw new IgnorableRuntimeException();
				}
				
				if(keepCP) {
					Set<Unit> temp = ret.get(source);
					if(temp == null) {
						temp = new HashSet<>();
						ret.put(source, temp);
					}
					temp.add(cur);
				}
			}
		}
		
		for(SootMethod m : ret.keySet()) {
			ret.put(m, SortingMethods.sortSet(ret.get(m),SootSort.unitComp));
		}
		ret = SortingMethods.sortMapKey(ret, SootSort.smComp);
		return ret;
	}*/
	
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
	
	private static final <A> String formatCollection(String pad, Iterator<A> it) {
		StringBuilder sb = new StringBuilder();
		while(it.hasNext()) {
			sb.append(pad).append(it.next()).append("\n");
		}
		return sb.toString();
	}
	
	private static final <A,B extends Iterable<C>,C> String formatMap(String pad, Map<A,B> map) {
		StringBuilder sb = new StringBuilder();
		for(A a : map.keySet()) {
			sb.append(pad).append(a).append("\n");
			Iterable<C> it = map.get(a);
			sb.append(formatCollection(pad+"  ",it.iterator()));
		}
		return sb.toString();
	}
	
	private static final String genPad(int len) {
		return String.format("%1$"+len+"s"," ");
	}
	
	private static final class DataFwd extends Tuple {
		private final int hashCode;
		public DataFwd(Local cause, Unit cur, SootMethod curMethod) {
			this((Object)cause, cur, curMethod);
		}
		private DataFwd(Object... objects) {
			super(objects);
			this.hashCode = super.hashCode();
		}
		public Local getCause(){return (Local)get(0);}
		public Unit getCurUnit(){return (Unit)get(1);}
		public SootMethod getCurMethod(){return (SootMethod)get(2);}
		@Override public int hashCode() {return hashCode;}
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("Local='").append(getCause()).append("' | Cur='").append(getCurUnit())
				.append("' | CurMethod='").append(getCurMethod()).append("'");
			return sb.toString();
		}
	}
	
	private static final class DataBkwd extends Tuple {
		private final int hashCode;
		public DataBkwd(Unit cur, SootMethod curMethod) {
			this((Object)cur, curMethod, false);
		}
		public DataBkwd(Unit cur, SootMethod curMethod, boolean geneppath) {
			this((Object)cur, curMethod, geneppath);
		}
		private DataBkwd(Object... objects) {
			super(objects);
			this.hashCode = super.hashCode();
		}
		public Unit getCurUnit(){return (Unit)get(0);}
		public SootMethod getCurMethod(){return (SootMethod)get(1);}
		public boolean isGenEpPath(){return (Boolean)get(2);}
		@Override public int hashCode() {return hashCode;}
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("Cur='").append(getCurUnit()).append("' | CurMethod='").append(getCurMethod())
				.append("' | GenEpPath='").append(isGenEpPath()).append("'");
			return sb.toString();
		}
	}
	
	private class ForwardControlPredicateRunner implements ValueRunner<Unit> {
		
		private final DataFwd d;
		private final Set<DataFwd> seen;
		private volatile Unit ret;
		private final IJimpleICFG icfg;
		private final ILogger logger;

		public ForwardControlPredicateRunner(DataFwd d, Set<DataFwd> seen, IJimpleICFG icfg, ILogger logger) {
			this.d = d;
			this.seen = seen;
			this.ret = null;
			this.icfg = icfg;
			this.logger = logger;
		}

		@Override
		public void run() {
			List<ValueRunner<Unit>> runners = new ArrayList<>();
			StringBuilder sb = new StringBuilder();
			Unit cur = d.getCurUnit();
			SootMethod curMethod = d.getCurMethod();
			Local cause = d.getCause();
			sb.append("Visiting ").append(d.toString()).append("\n");
			if(cur instanceof DefinitionStmt) {
				DefinitionStmt def = (DefinitionStmt)cur;
				//aliasing is take care of in this loop since those DefStmts are included in the list here
				//Get uses of defined local on left which contains the use of a tracked local on right
				for(UnitValueBoxPair usePair : icfg.getOrMakeLocalUses(curMethod).getUsesOf(def)) {
					DataFwd newd = new DataFwd((Local)usePair.getValueBox().getValue(),usePair.getUnit(),curMethod);
					if(seen.add(newd)) {
						sb.append(pad).append("   Local Use '").append(usePair.getValueBox().getValue()).append("'. Adding '")
							.append(usePair.getUnit()).append("' of '").append(curMethod).append("'.\n");
						runners.add(new ForwardControlPredicateRunner(newd,seen,icfg,logger));
					}
				}
				//VariableBox returned -> field ref is on left, RValueBox returned -> field res is on right
				//Check if local is being written to a field and if so all reads of said field afterwards are considered
				//Note we start at the source method to capture all the field reads from there downwards
				//We are not concerned with capturing the field reads of the entire ep as this is a discovery forward traversal
				/*if(def.containsFieldRef() && def.getFieldRefBox() instanceof VariableBox) {
					for(DefinitionStmt fieldRead : icfg.getAllFieldReadsForMethod(startMethod, def.getFieldRef().getField())) {
						Data newd = new Data(startUnit,startMethod,fieldRead,icfg.getMethodOf(fieldRead),true);
						if(seen.add(newd)) {
							sb.append(pad).append("   Field Read '").append(fieldRead).append("' of '").append(newd.getCurMethod())
								.append("'. Adding due to field write.\n");
							executeRunner(new ForwardControlPredicateRunner(newd,seen,markedClasses,icfg,logger),logger);
						}
					}
				}*/
				
				//VariableBox returned -> array ref is on left, RValueBox returned -> array ref is on right
				//Handling assigning an tracked local to an array entry
				//Arrays assign contain ArrayRef on the left side not local so the general use generator will miss them
				//Have to lookup actual def of the array and then retrieve the uses of that def after the tracked local is assigned
				if(def.containsArrayRef() && def.getArrayRefBox() instanceof VariableBox) {
					Local arrLocal = (Local)def.getArrayRef().getBase();
					for(DefinitionStmt arrDef : icfg.getOrMakeLocalDefs(curMethod).getDefsWithAliases(arrLocal, def)) {
						for(UnitValueBoxPair usePair : icfg.getOrMakeLocalUses(curMethod).getUsesOf(arrDef)) {
							DataFwd newd = new DataFwd((Local)usePair.getValueBox().getValue(),usePair.getUnit(),curMethod);
							if(seen.add(newd)) {
								sb.append(pad).append("   Local Array Use '").append(usePair.getValueBox().getValue()).append("'. Adding '")
									.append(usePair.getUnit()).append("' of '").append(curMethod).append("'.\n");
								runners.add(new ForwardControlPredicateRunner(newd,seen,icfg,logger));
							}
						}
					}
				}
			} else if(cur instanceof IfStmt || cur instanceof SwitchStmt) {
				sb.append(pad).append("   New CP '").append(cur).append("'\n");
				ret = cur;
			}
			
			//Assume any local used in a stmt reachable from the first def is likely to be important auth-logic wise
			//Therefore, we assume the further use of any of these locals is likely to be important later as well
			//So we need to find all their defs and the defs of any other local used in their def
			//Node arrays local and index local when writing to an array appear in the use boxes
			//So if array is written or read it will should up here
			//Only backtrack if the type being defined is a primitive, string, or a type known to be auth-logic related (or an array of those)
			//This avoids the issue where we are backtracking for unimportant types (like this which would lead to everything)
			/*for(DefinitionStmt def : icfg.getDefsForUsedLocals(cur)) {
				if(isAllowedType(def.getLeftOp().getType())) {
					Data newd = new Data(def,curMethod,true);
					if(seen.add(newd)) {
						sb.append(pad).append("   Local Def '").append((Local)def.getLeftOp()).append("'. Adding '")
							.append(def).append("' of '").append(curMethod).append("'.\n");
						runners.add(new ForwardControlPredicateRunner(newd,seen,markedClasses,icfg,logger));
					}
					//The definition stmt is a field read so this field is likely important
					//Therefore we need to find all other reads of the field and explore them
					//Note we start at the source method to capture all the field reads from there downwards
					//We are not concerned with capturing the field reads of the entire ep as this is a discovery forward traversal
					if(def.containsFieldRef() && !(def.getFieldRefBox() instanceof VariableBox)) {
						for(DefinitionStmt fieldRead : icfg.getAllFieldReadsForMethod(startMethod, def.getFieldRef().getField())) {
							Data newd2 = new Data(startUnit,startMethod,fieldRead,icfg.getMethodOf(fieldRead),true);
							if(seen.add(newd2)) {
								sb.append(pad).append("     Field Read '").append(fieldRead).append("' of '").append(newd2.getCurMethod())
								.append("'. Adding due to field read.\n");
								executeRunner(new ForwardControlPredicateRunner(newd2,seen,markedClasses,icfg,logger),logger);
							}
						}
					}
				}
			}*/
			
			if(((Stmt)cur).containsInvokeExpr() && cause != null) {
				InvokeExpr ie = ((Stmt)cur).getInvokeExpr();
				List<List<IdentityStmt>> parmDefs = new ArrayList<>();
				for(SootMethod sm : icfg.getCalleesOfCallAt(cur)) {
					if(!sm.getName().equals("<clinit>"))
						parmDefs.add(icfg.getParameterDefs(sm));
				}
				for(int i = 0; i < ie.getArgCount(); i++) {
					if(ie.getArg(i).equals(cause)) {
						for(List<IdentityStmt> parmDef : parmDefs) {
							IdentityStmt parmD = parmDef.get(i);//Could be null if the parm is never used
							if(parmD != null) {
								DataFwd newd = new DataFwd((Local)null,parmD,icfg.getMethodOf(parmD));
								if(seen.add(newd)) {
									sb.append(pad).append("   Invoke Parm '").append(parmD).append("' of '").append(newd.getCurMethod())
										.append("'. Adding...\n");
									runners.add(new ForwardControlPredicateRunner(newd,seen,icfg,logger));
								}
							}
						}
					}
				}
				//If we are tracking a local representing an object then anything done to that object needs to be explored
				if(ie instanceof InstanceInvokeExpr) {
					InstanceInvokeExpr iie = (InstanceInvokeExpr)ie;
					if(iie.getBase().equals(cause)) {
						for(SootMethod sm : icfg.getCalleesOfCallAt(cur)) {
							if(!sm.getName().equals("<clinit>")) {
								for(Unit u : sm.getActiveBody().getUnits()) {
									if(u instanceof DefinitionStmt && ((DefinitionStmt)u).getRightOp() instanceof ThisRef) {
										DataFwd newd = new DataFwd((Local)null,u,icfg.getMethodOf(u));
										if(seen.add(newd)) {
											sb.append(pad).append("   Invoke This '").append(u).append("' of '").append(newd.getCurMethod())
												.append("'. Adding....\n");
											runners.add(new ForwardControlPredicateRunner(newd,seen,icfg,logger));
										}
									}
								}
							}
						}
					}
				}
			}
			
			//Invoke expressions need to be visited per local as the use analysis in a method is dependent on the starting point
			//Since we are tracking all locals used with the local that brought us here, then we need to look at all parm and this defs
			/*if(((Stmt)cur).containsInvokeExpr()) {
				InvokeExpr ie = ((Stmt)cur).getInvokeExpr();
				if(!ie.getMethodRef().name().equals("<clinit>")) {
					boolean isInstanceInvoke =  ie instanceof InstanceInvokeExpr;
					for(SootMethod sm : icfg.getCalleesOfCallAt(cur)) {
						for(IdentityStmt parmDef : icfg.getParameterDefs(sm)) {
							if(parmDef != null) {//Could be null if the parm is never used
								Data newd = new Data(parmDef,icfg.getMethodOf(parmDef),true);
								if(seen.add(newd)) {
									sb.append(pad).append("   Invoke Parm '").append(parmDef).append("' of '").append(newd.getCurMethod())
										.append("'. Adding...\n");
									runners.add(new ForwardControlPredicateRunner(newd,seen,markedClasses,icfg,logger));
								}
							}
						}
						//If we are tracking a local representing an object then anything done to that object needs to be explored
						if(isInstanceInvoke) {
							for(Unit u : sm.getActiveBody().getUnits()) {
								if(u instanceof DefinitionStmt && ((DefinitionStmt)u).getRightOp() instanceof ThisRef) {
									Data newd = new Data(u,icfg.getMethodOf(u),true);
									if(seen.add(newd)) {
										sb.append(pad).append("     Invoke This '").append(u).append("' of '").append(newd.getCurMethod())
											.append("'. Adding....\n");
										runners.add(new ForwardControlPredicateRunner(newd,seen,markedClasses,icfg,logger));
									}
								}
							}
						}
					}
				}
			}*/
			if(!runners.isEmpty())
				executeRunners(runners,logger);
			logger.debug("{}: {}",name,sb.toString());
		}

		@Override
		public Unit getValue() {
			return ret;
		}
		
		/*private boolean isAllowedType(Type t) {
			if(t instanceof ArrayType)
				t = ((ArrayType)t).baseType;
			if(t instanceof PrimType)
				return true;
			if(t instanceof RefType) {
				String cn = ((RefType)t).getClassName();
				return markedClasses.contains(cn) || cn.equals("java.lang.String");
			}
			return false;
		}*/
		
	}
	
	private class BackwardControlPredicateRunner implements ValueRunner<Unit> {
		
		private final DataBkwd d;
		private final Set<DataBkwd> seen;
		private volatile Unit ret;
		private final IJimpleICFG icfg;
		private final ILogger logger;

		public BackwardControlPredicateRunner(DataBkwd d, Set<DataBkwd> seen, IJimpleICFG icfg, ILogger logger) {
			this.d = d;
			this.seen = seen;
			this.ret = null;
			this.icfg = icfg;
			this.logger = logger;
		}

		@Override
		public void run() {
			List<ValueRunner<Unit>> runners = new ArrayList<>();
			StringBuilder sb = new StringBuilder();
			Unit cur = d.getCurUnit();
			SootMethod curMethod = d.getCurMethod();
			
			sb.append("Visiting ").append(d.toString()).append("\n");
			
			//This is either a start stmt or a invoke stmt that is leads to the start stmt from the ep
			//Either way we need to move up the callgraph one method in the backwards traversal to the ep
			//Should stop at the ep as this should have no incoming edges (minus recursion)
			if(d.isGenEpPath()) {
				for(Unit callerStmt : icfg.getCallersOf(curMethod)) {
					DataBkwd newd = new DataBkwd(callerStmt,icfg.getMethodOf(callerStmt),true);
					if(seen.add(newd)) {
						sb.append(pad).append("   Backtracking ").append(newd.toString()).append("\n");
						runners.add(new BackwardControlPredicateRunner(newd, seen, icfg, logger));
					}
				}
			}
			
			//Regardless of what the statement is we need to generate its branch control dependencies and add them to the worklist
			for(Unit u : icfg.getOrMakeControlDependenceGraph(curMethod).getIteratedControlDependencies(cur)) {
				DataBkwd newd = new DataBkwd(u,curMethod);
				if(seen.add(newd)) {
					sb.append(pad).append("   CtrlDep ").append(newd.toString()).append("\n");
					runners.add(new BackwardControlPredicateRunner(newd, seen, icfg, logger));
				}
			}
			
			//Regardless of the statement, we need to find the definitions for all the locals used and add them to the worklist
			for(DefinitionStmt def : icfg.getDefsForUsedLocals(cur)) {
				DataBkwd newd = new DataBkwd(def,curMethod);
				if(seen.add(newd)) {
					sb.append(pad).append("   DefStmt ").append(newd.toString()).append("\n");
					runners.add(new BackwardControlPredicateRunner(newd, seen, icfg, logger));
				}
				//The definition stmt is a field read so this field is likely important
				//Therefore we need to find all other reads of the field from the start to the entry point and explore them
				/*if(def.containsFieldRef() && !(def.getFieldRefBox() instanceof VariableBox)) {
					for(DefinitionStmt fieldRead : icfg.getAllFieldReadsAt(startUnit, def.getFieldRef().getField())) {
						Data newd2 = new Data(startUnit,startMethod,fieldRead,icfg.getMethodOf(fieldRead));
						if(seen.add(newd2)) {
							sb.append(pad).append("     FieldRead ").append(newd2.toString()).append("\n");
							executeRunner(new BackwardControlPredicateRunner(newd2, seen, icfg, logger),logger);
						}
					}
				}*/
			}
			
			//Note the only way this can be triggered is if the start stmt happens to be a write of a local
			//Otherwise we will never encounter these since we are only ever dealing with definitions not uses
			/*if(cur instanceof DefinitionStmt) {
				//VariableBox returned -> field ref is on left, RValueBox returned -> field res is on right
				//Check if local is being written to a field and if so all reads are likely important
				//Therefore we need to find all other reads of the field from the start to the entry point and explore them
				DefinitionStmt def = (DefinitionStmt)cur;
				if(def.containsFieldRef() && def.getFieldRefBox() instanceof VariableBox) {
					for(DefinitionStmt fieldRead : icfg.getAllFieldReadsAt(startUnit, def.getFieldRef().getField())) {
						Data newd = new Data(startUnit,startMethod,fieldRead,icfg.getMethodOf(fieldRead));
						if(seen.add(newd)) {
							sb.append(pad).append("   FieldReadOfWrite ").append(newd.toString()).append("\n");
							executeRunner(new BackwardControlPredicateRunner(newd, seen, icfg, logger),logger);
						}
					}
				}
			}*/
			
			//If it contains an invoke expression, we need to explore all possible invoked methods starting at their returns
			//If the invoke is for a method we are returning from as part of the backwards traversal to the ep, then we don't 
			//want to explore it again
			if(((Stmt)cur).containsInvokeExpr() && !d.isGenEpPath()) {
				Set<SootMethod> seenMethods = new HashSet<>();
				for(Unit u : icfg.getAllEndPointsOfCalleesOfCallAt(cur)) {
					SootMethod methodOfU = icfg.getMethodOf(u);
					if(!methodOfU.getName().equals("<clinit>")) {
						DataBkwd newd = new DataBkwd(u,methodOfU);
						if(seen.add(newd)) {
							sb.append(pad).append("   CallExit ").append(newd.toString()).append("\n");
							runners.add(new BackwardControlPredicateRunner(newd, seen, icfg, logger));
						}
						//We will be entering a new method, all methods called from this method need to be looked at also
						SootMethod uM = icfg.getMethodOf(u);
						if(seenMethods.add(uM)) {
							for(Unit invoke : icfg.getCallsFromWithin(uM)) {
								DataBkwd newd2 = new DataBkwd(invoke,uM);
								if(seen.add(newd2)) {
									sb.append(pad).append("   InvokeWithinCall ").append(newd2.toString()).append("\n");
									runners.add(new BackwardControlPredicateRunner(newd2, seen, icfg, logger));
								}
							}
						}
					}
				}
			}
			
			//if statement is a branch record it
			if (cur instanceof IfStmt || cur instanceof SwitchStmt) {
				sb.append(pad).append("   New CP '").append(cur).append("'\n");
				ret = cur;	
			}
			if(!runners.isEmpty())
				executeRunners(runners,logger);
			logger.debug("{}: {}",name,sb.toString());
		}

		@Override
		public Unit getValue() {
			return ret;
		}
	}
	
}
