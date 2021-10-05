package org.sag.acminer.phases.entrypoints;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.sag.common.logging.ILogger;
import org.sag.common.tools.HierarchyHelpers;
import org.sag.common.tools.SortingMethods;
import org.sag.common.tuple.Pair;
import org.sag.soot.SootSort;
import org.sag.soot.analysis.AdvLocalDefs;
import org.sag.soot.analysis.AdvLocalUses;

import com.google.common.collect.ImmutableSet;

import soot.Body;
import soot.FastHierarchy;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Unit;
import soot.Value;
import soot.jimple.ConditionExpr;
import soot.jimple.DefinitionStmt;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.SwitchStmt;
import soot.jimple.TableSwitchStmt;
import soot.jimple.toolkits.pointer.LocalMustNotAliasAnalysis;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BriefBlockGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.LiveLocals;
import soot.toolkits.scalar.UnitValueBoxPair;
import soot.util.Chain;

public class GenerateEntryPoints {
	
	//Stub -> Service -> Eps -> Transaction Ids
	private Map<SootClass, Map<SootClass,Map<SootMethod,Set<Integer>>>> stubMethods;
	//Stored separately because a switch block for an id may have no entry points
	//All transaction ids for a stub, actual services will have some subset of these (maybe all but not guaranteed)
	//Stub -> All Transaction Ids
	private Map<SootClass, Set<Integer>> stubsToAllTransactionIds;
	private ILogger logger;
	
	//private final String onTransactBinderSignature = "<android.os.Binder: boolean onTransact(int,android.os.Parcel,android.os.Parcel,int)>";
	public static final String onTransactSubSignature = "boolean onTransact(int,android.os.Parcel,android.os.Parcel,int)";
	public static final String binderFullClassName = "android.os.Binder";
	public static final String iinterfaceFullClassName = "android.os.IInterface";
	
	//Fields related to methods we should skip as they are not eps
	private final String objectClass = "java.lang.Object";
	private final Set<String> objSubSigs;
	private final Set<String> skipMethods = ImmutableSet.<String>of(
			"<android.app.ActivityManagerNative: int[] readIntArray(android.os.Parcel)>",
			"<com.android.server.am.ActivityManagerService: void boostPriorityForLockedSection()>",
			"<com.android.server.am.ActivityManagerService: void resetPriorityAfterLockedSection()>"
	);

	public GenerateEntryPoints(ILogger logger) {
		this.logger = logger;
		stubMethods = new HashMap<>();
		stubsToAllTransactionIds = new HashMap<>();
		this.objSubSigs = new HashSet<>();
	}
	
	public EntryPointsDatabase constructAndSetNewDatabase(){
		findEntryPoints();
		//Temporary translation until this code is removed
		Map<SootClass,Map<SootMethod,Set<Integer>>> ret = new HashMap<>();
		for(SootClass stub : stubMethods.keySet()) {
			System.out.println("Stub: " + stub);
			Map<SootMethod,Set<Integer>> epToIds = new HashMap<>();
			Map<SootClass,Map<SootMethod,Set<Integer>>> serviceToEpsToIds = stubMethods.get(stub);
			for(SootClass service : serviceToEpsToIds.keySet()) {
				System.out.println("  Service: " + service);
				Map<SootMethod,Set<Integer>> data = serviceToEpsToIds.get(service);
				for(SootMethod ep : data.keySet()) {
					System.out.println("    EntryPoint: " + ep + " Ids=" + data.get(ep));
					Set<Integer> temp = epToIds.get(ep);
					if(temp == null) {
						temp = new HashSet<>();
						epToIds.put(ep, temp);
					}
					temp.addAll(data.get(ep));
				}
			}
			ret.put(stub, epToIds);
		}
		for(SootClass stub : ret.keySet()) {
			Map<SootMethod, Set<Integer>> epToIds = ret.get(stub);
			for(SootMethod ep : epToIds.keySet()) {
				epToIds.put(ep,SortingMethods.sortSet(epToIds.get(ep),SortingMethods.iComp));
			}
			ret.put(stub, SortingMethods.sortMapKey(epToIds,SootSort.smComp));
		}
		ret = SortingMethods.sortMapKey(ret, SootSort.scComp);
		return EntryPointsDatabase.getNewEntryPointsDatabase(ret,stubsToAllTransactionIds);
	}
	
	/**
	 * Find all classes with an onTransact function that extend Binder as these are our stub classes.
	 * Some of these classes may not have interfaces or proxies associated with them but they can still 
	 * be sent data via binder. Proxies and interfaces are a convince that make calling a service via
	 * Binder easier. To communicate with a service, all one really needs to do is have an object of 
	 * type Binder and call the transact function. The proxies and interfaces just make obtaining the
	 * Binder object and directing the service call easier. Therefore, to be sure we cover all entry
	 * points, we must locate all classes that both define an onTransact function and extend the 
	 * Binder object (which are stub classes in aidl terminology).
	 * 
	 * Note: If a class does not implement an onTransact function but extends Binder then it 
	 * technically can be accessed through Binder. However, the onTransact functionality 
	 * defaults to that of the onTransact function defined in the Binder class, which does
	 * not do anything interesting (or security related). Therefore, we are ignoring these
	 * trivial cases.
	 */
	private void findEntryPoints() {
		//init the list of methods in Object that we want to skip
		SootClass obj = Scene.v().getSootClass(objectClass);
		for(SootMethod sm : obj.getMethods()) {
			objSubSigs.add(sm.getSubSignature());
		}
		
		//get the Binder class which is the super class of all stubs
		SootClass binder = Scene.v().getSootClass(binderFullClassName);
		
		Map<SootMethod,Pair<Set<SootMethod>,Set<Integer>>> baseOnTransact = parseBaseOnTransact(binder, logger);
		
		//get a list of the binder methods 
		List<SootMethod> binderMethods = binder.getMethods();
		
		//get IInterface which all stubs and proxies should implement when using that model
		SootClass iinterface = Scene.v().getSootClass(iinterfaceFullClassName);
		
		//Get all indirect and direct subclasses of Binder
		Set<SootClass> allSubclassesOfBinder = HierarchyHelpers.getAllSubClasses(binder);
		allSubclassesOfBinder.remove(binder);//Ignore binder itself because it is unimportant
		
		//Get Only the direct subclasses of Binder
		FastHierarchy fh = Scene.v().getOrMakeFastHierarchy();
		Set<SootClass> directSubClassesOfBinder = new HashSet<>(fh.getSubclassesOf(binder));
		
		//Get all sub interfaces of IInterface (includes IInterface)
		Set<SootClass> binderInterfaces = HierarchyHelpers.getAllSubClassesOfInterface(iinterface);
		binderInterfaces.remove(iinterface);//remove IInterface because it is a special case
		
		for(SootClass sc : allSubclassesOfBinder) {
			if(!sc.isInterface() && !sc.isPhantom()) {//double check that it is not phantom or an interface
				SootMethod onTransactMethod = sc.getMethodUnsafe(onTransactSubSignature);//get the onTransact method if one exists
				Map<SootClass,Map<SootMethod, Set<Integer>>> eps = null;
				Set<Integer> allIds = new HashSet<>();
				if(onTransactMethod != null) {
					eps = getEntryPointsFromOnTransact(onTransactMethod,allIds,binderMethods,baseOnTransact,binder);
					if(eps.isEmpty() && !directSubClassesOfBinder.contains(sc)) {
						logger.info("The class {} does not directly extend binder and has no invoked/resolved entry points. "
								+ "It is not a stub.",sc.getName());
					}
				} else {
					//Could be extending Binder and overriding the methods called by the default onTransact method
					//Ignore those that don't directly extend binder because these would be our services and should 
					//already have been captured as such
					if(directSubClassesOfBinder.contains(sc)) {
						eps = handleSuperOnTransact(sc, new HashMap<>(), allIds, baseOnTransact);
						if(eps.isEmpty())
							logger.info("The class {} does not override onTransact or any method called by the Binder "
									+ "onTransact and does not directly extend binder. It is not a stub.",sc.getName());
					} else {
						logger.info("The class {} does not override onTransact and does not directly extend binder. It is"
								+ " not a stub but maybe a service.",sc.getName());
					} 
				}
				
				if(eps != null && !eps.isEmpty()) {
					for(SootClass service : eps.keySet()) {
						Map<SootMethod,Set<Integer>> temp = eps.get(service);
						for(SootMethod m : temp.keySet()) {
							temp.put(m, SortingMethods.sortSet(temp.get(m),SortingMethods.iComp));
						}
						eps.put(service, SortingMethods.sortMapKey(temp,SootSort.smComp));
					}
					stubMethods.put(sc, SortingMethods.sortMapKey(eps, SootSort.scComp));
					stubsToAllTransactionIds.put(sc,SortingMethods.sortSet(allIds,SortingMethods.iComp));
				} else if(directSubClassesOfBinder.contains(sc)) {
					stubMethods.put(sc, new LinkedHashMap<SootClass,Map<SootMethod,Set<Integer>>>());
					stubsToAllTransactionIds.put(sc,SortingMethods.sortSet(allIds,SortingMethods.iComp));
				}
			}
		}
		
		stubMethods = SortingMethods.sortMapKey(stubMethods, SootSort.scComp);
		stubsToAllTransactionIds = SortingMethods.sortMapKey(stubsToAllTransactionIds, SootSort.scComp);
	}
	
	public static Map<SootMethod,Pair<Set<SootMethod>,Set<Integer>>> parseBaseOnTransact(SootClass binder, ILogger logger) {
		SootMethod onTransactMethod = binder.getMethodUnsafe(onTransactSubSignature);
		Map<SootMethod,Pair<Set<SootMethod>,Set<Integer>>> ret = new HashMap<>();
		if(onTransactMethod != null) {
			Body body = onTransactMethod.retrieveActiveBody();
			BriefBlockGraph bbg = new BriefBlockGraph(body);
			List<Block> blockHeads = bbg.getHeads();
			List<Block> blockTails = bbg.getTails();
			if(blockHeads.size() >= 1) {
				Block cur = blockHeads.get(0);
				boolean first = true;
				while(cur != null) {
					Unit unit = null;
					int id = -1;
					Block head = null;
					SootMethod ep = null;

					if(first) {
						first = false;
						unit = cur.getTail();
					} else {
						unit = cur.getHead();
					}
					
					if(unit != null && unit instanceof IfStmt) {
						ConditionExpr condExpr = (ConditionExpr)(((IfStmt)unit).getCondition());
						Value op1 = condExpr.getOp1();
						Value op2 = condExpr.getOp2();
						if(op1 instanceof IntConstant) {
							id = ((IntConstant) op1).value;
						}else if(op2 instanceof IntConstant) {
							id = ((IntConstant) op2).value;
						} else {
							throw new RuntimeException("Error: The IfStmt does not contain an IntConstant.\n"
								+ "  IfStmt: " + unit + "\n"
								+ "  Op1 Type: " + op1.getClass().getName() + "\n"
								+ "  Op2 Type: " + op2.getClass().getName() + "\n"
								+ dumpBlock(cur, "  ", "Cur")
							);
						}
					} else {
						throw new RuntimeException("Error: Expected type of IfStmt when looking for the transaction id "
							+ "of a block.\n"
							+ "  Unit: " + unit + "\n"
							+ "  Type: " + unit.getClass().getName() + "\n"
							+ dumpBlock(cur, "  ", "Cur")
						);
					}
					
					List<Block> succs = cur.getSuccs();
					if(succs != null && succs.size() == 2) {
						Block b1 = succs.get(0);
						Block b2 = succs.get(1);
						boolean isSingleUnitIfB1 = b1.getHead() != null && b1.getTail() != null && 
								b1.getHead().equals(b1.getTail()) && b1.getHead() instanceof IfStmt;
						boolean isSingleUnitIfB2 = b2.getHead() != null && b2.getTail() != null && 
								b2.getHead().equals(b2.getTail()) && b2.getHead() instanceof IfStmt;
						boolean isTailBlockB1 = blockTails.contains(b1) && b1.getHead().equals(b1.getTail()) 
								&& b1.getHead() instanceof ReturnStmt;
						boolean isTailBlockB2 = blockTails.contains(b2) && b2.getHead().equals(b2.getTail()) 
								&& b2.getHead() instanceof ReturnStmt;
						if(isSingleUnitIfB1 && isSingleUnitIfB2) {
							throw new RuntimeException("Error: Expected only one of block's successors to contain"
								+ " only one unit which is an IfStmt (i.e. representing part of an else if chain).\n"
								+ dumpBlock(cur, "  ", "Cur") + "\n"
								+ dumpBlock(b1, "  ", "Succ1") + "\n"
								+ dumpBlock(b2, "  ", "Succ2")
							);
						} else if(isSingleUnitIfB1) {
							cur = b1;
							head = b2;
						} else if(isSingleUnitIfB2) {
							cur = b2;
							head = b1;
						} else if(isTailBlockB1 && isTailBlockB2) {
							throw new RuntimeException("Error: Expected only one of block's successors to contain"
								+ " be a tail block containing only one unit which is a ReturnStmt (i.e. the end of"
								+ " the else if chain in this case).\n"
								+ dumpBlock(cur, "  ", "Cur") + "\n"
								+ dumpBlock(b1, "  ", "Succ1") + "\n"
								+ dumpBlock(b2, "  ", "Succ2")
							);
						} else if(isTailBlockB1) {
							cur = null;
							head = b2;
						} else if(isTailBlockB2) {
							cur = null;
							head = b1;
						} else {
							throw new RuntimeException("Error: The successors do not follow the expected format"
								+ " of an else if chain ending in a single return statement.\n"
								+ dumpBlock(cur, "  ", "Cur") + "\n"
								+ dumpBlock(b1, "  ", "Succ1") + "\n"
								+ dumpBlock(b2, "  ", "Succ2")
							);
						}
					} else {
						throw new RuntimeException("Error: The block does not have exactly two successors. Does it not"
							+ " end in a IfStmt?\n"
							+ dumpBlock(cur, "  ", "Cur")
						);
					}
					
					Set<SootMethod> methodsInClass = new HashSet<>();
					Set<Block> visited = new HashSet<>();
					Queue<Block> toVisit = new ArrayDeque<>();
					toVisit.add(head);
					while(!toVisit.isEmpty()) {
						Block b = toVisit.poll();
						if(visited.add(b)) {
							for(Iterator<Unit> it = body.getUnits().iterator(b.getHead(), b.getTail()); it.hasNext();) {
								Unit u = it.next();
								if(u != null && ((Stmt)u).containsInvokeExpr()) {
									SootMethodRef ref = ((Stmt)u).getInvokeExpr().getMethodRef();
									if(ref.declaringClass().equals(binder))
										methodsInClass.add(ref.resolve());
								}
								if(u == b.getTail())
									break;
							}
							List<Block> s = b.getSuccs();
							if(s != null)
								toVisit.addAll(s);
						}
					}
					
					if(methodsInClass.size() == 0) {
						logger.warn("No invoke method found in the block tree for \n  Class: '{}'\n  Transiction Id: '{}'\n{}", binder, id, dumpBlock(head, "  ", "Head"));
						continue;
					} else if(methodsInClass.size() != 1) {
						throw new RuntimeException("Error: Did not find exactly one invoked method in the block tree that"
							+ " is invoked on " + binder + "\n"
							+ "  Transiction Id: " + id + "\n"
							+ "  Invoked Methods: " + methodsInClass + "\n"
							+ dumpBlock(head, "  ", "Head")
						);
					}
					ep = methodsInClass.iterator().next();
					
					Set<SootMethod> visitedM = new HashSet<>();
					Queue<SootMethod> toVisitM = new ArrayDeque<>();
					toVisitM.add(ep);
					while(!toVisitM.isEmpty()) {
						SootMethod sm = toVisitM.poll();
						if(visitedM.add(sm) && sm.isConcrete()) {
							for(Unit u : sm.retrieveActiveBody().getUnits()) {
								if(((Stmt)u).containsInvokeExpr()) {
									SootMethodRef ref = ((Stmt)u).getInvokeExpr().getMethodRef();
									if(ref.declaringClass().equals(binder))
										toVisitM.add(ref.resolve());
								}
							}
						}
					}
					
					Pair<Set<SootMethod>,Set<Integer>> p = ret.get(ep);
					if(p == null) {
						p = new Pair<>(new HashSet<>(), new HashSet<>());
						ret.put(ep, p);
					}
					p.getFirst().addAll(visitedM);
					p.getSecond().add(id);
				}
			} else {
				throw new RuntimeException("Error: No block heads in the method '" + onTransactSubSignature 
						+ "' of '" + binder + "'");
			}
		} else {
			throw new RuntimeException("Error: Failed to find method '" + onTransactSubSignature 
					+ "' in the class '" + binder + "'");
		}
		
		for(SootMethod ep : ret.keySet()) {
			Pair<Set<SootMethod>, Set<Integer>> p = ret.get(ep);
			ret.put(ep, new Pair<>(
					SortingMethods.sortSet(p.getFirst(),SootSort.smComp),
					SortingMethods.sortSet(p.getSecond(),SortingMethods.iComp)
			));
		}
		return SortingMethods.sortMapKey(ret, SootSort.smComp);
	}
	
	private static String dumpBlock(Block b, String spacer, String name) {
		StringBuffer strBuf = new StringBuffer();
		strBuf.append(spacer).append(name).append(" Block ").append(b.getIndexInMethod()).append(":\n");
		
		strBuf.append(spacer).append("[preds: ");
		List<Block> mPreds = b.getPreds();
		if(mPreds != null) {
	    	Iterator<Block> it = mPreds.iterator();
	    	while(it.hasNext())
	    		strBuf.append(it.next().getIndexInMethod()).append(" ");
		}
		strBuf.append("] [succs: ");
		List<Block> mSuccessors = b.getSuccs();
		if(mSuccessors != null) {
			Iterator<Block> it = mSuccessors.iterator();
			while(it.hasNext())
				strBuf.append(it.next().getIndexInMethod()).append(" ");
		}
		strBuf.append("]\n");

		Chain<Unit> methodUnits = b.getBody().getUnits();
		Iterator<Unit> basicBlockIt = methodUnits.iterator(b.getHead(), b.getTail());
		if(basicBlockIt.hasNext()) {
			Unit someUnit = (Unit) basicBlockIt.next();
			strBuf.append(spacer).append("  ").append(someUnit.toString()).append(";\n");
			while(basicBlockIt.hasNext()) {
				someUnit = (Unit) basicBlockIt.next();
				if (someUnit == b.getTail())
					break;
				strBuf.append(spacer).append("  ").append(someUnit.toString()).append(";\n");
			}
			someUnit = b.getTail();
			if (b.getTail() == null)
				strBuf.append(spacer).append("  Error: Null tail found.");
			else if (b.getHead() != b.getTail())
				strBuf.append(spacer).append("  ").append(someUnit.toString()).append(";");
		}
		return strBuf.toString();
	}
	
	private int getEntryPointsFromOnTransactAndWrappers(SootMethod onTransaceMethodOrWrapper, Map<Integer, Set<Unit>> keyToSwitchBlocks, 
			SootClass binder, Map<SootClass,Map<SootMethod,Set<Integer>>> ret) {
		Body body = onTransaceMethodOrWrapper.retrieveActiveBody();
		SootClass stubClass = onTransaceMethodOrWrapper.getDeclaringClass();
		LocalMustNotAliasAnalysis lmnaa = null;
		boolean currentClassMethodInvokesFound = false;
		boolean hasSuperOnTransact = false;
		int returnValues = 0;
		for(Unit u : body.getUnits()) {
			if(u instanceof Stmt && ((Stmt)u).containsInvokeExpr()) {
				SootMethodRef methodRef = ((Stmt)u).getInvokeExpr().getMethodRef();
				if(methodRef.declaringClass().equals(stubClass) && !skip(methodRef)) {
					if(methodRef.name().startsWith("onTransact$") && methodRef.name().endsWith("$")) {
						SootMethod onTransactEpWrapperMethod = methodRef.declaringClass().getMethodUnsafe(methodRef.getSubSignature());
						if(onTransactEpWrapperMethod != null) {
							logger.info("Found onTransact wrapper ep method '{}'. Recrusively parsing its method body.",onTransactEpWrapperMethod);
							returnValues |= getEntryPointsFromOnTransactAndWrappers(onTransactEpWrapperMethod, keyToSwitchBlocks, binder, ret);
						} else {
							throw new RuntimeException("Error: Found call to onTransact wrapper ep method '" + methodRef + "' but could not resolve it in the class '" 
									+ methodRef.declaringClass() + "'. Is it not in the same class as onTransact?!?");
						}
					} else {
						currentClassMethodInvokesFound = true;
						if(lmnaa == null)
							lmnaa = new LocalMustNotAliasAnalysis(new ExceptionalUnitGraph(body), body);
						Set<SootMethod> resolvedTargets = HierarchyHelpers.getAllPossibleInvokeTargets(lmnaa, u);
						if(resolvedTargets.size() > 0) {
							Set<Integer> transactIds = new HashSet<>();
							for(Map.Entry<Integer, Set<Unit>> e : keyToSwitchBlocks.entrySet()) {
								if(e.getValue().contains(u))
									transactIds.add(e.getKey());
							}
							boolean added = false;
							for(SootMethod m : resolvedTargets) {
								if(m.isConcrete()) { //Ignore native and phantom targets because they just cause problems later
									Map<SootMethod,Set<Integer>> epToId = ret.get(m.getDeclaringClass());
									if(epToId == null) {
										epToId = new HashMap<>();
										ret.put(m.getDeclaringClass(), epToId);
									}
									Set<Integer> temp = epToId.get(m);
									if(temp == null) {
										temp = new HashSet<>();
										epToId.put(m, temp);
									}
									temp.addAll(transactIds);
									added = true;
								}
							}
							if(!added) {
								logger.info("Could not resolve Unit '{}' to target concrete methods. No entry points generated from unit.",u);
							}
						} else if(logger != null) {
							logger.info("Could not resolve Unit '{}' to target methods. No entry points generated from unit.",u);
						}
					}
				} else if(methodRef.getSubSignature().toString().equals(onTransactSubSignature)
						&& methodRef.declaringClass().equals(binder)) {
					hasSuperOnTransact = true;
				}
			}
		}
		returnValues |= hasSuperOnTransact ? 1 : 0;
		returnValues |= currentClassMethodInvokesFound ? 2 : 0;
		return returnValues;
	}
	
	/**
	 * Goes through a methods program flow and pulls out any function
	 * calls being made to the current object as the current object is only an abstraction containing
	 * only a few simple methods (constructor, asInterface, asBinder, and onTransact) which should 
	 * not be called within the onTransact method. Therefore, we can assume that any function calls 
	 * made to the current object are being made to the class that extends this class which are our
	 * entry points. This is true for all binder interfaces defined by aidl file and should be true
	 * for all manually defined binder interfaces for java. This also maps all entry points generated
	 * to their transaction id in the stub for use when identifying what proxy calls what entry point.
	 */
	private Map<SootClass,Map<SootMethod,Set<Integer>>> getEntryPointsFromOnTransact(SootMethod onTransactMethod, 
			Set<Integer> allIds, List<SootMethod> binderMethods, 
			Map<SootMethod,Pair<Set<SootMethod>,Set<Integer>>> baseOnTransact, SootClass binder){
		Map<SootClass,Map<SootMethod,Set<Integer>>> ret = new HashMap<>();
		Map<Integer, Set<Unit>> keyToSwitchBlocks = getSwitchBlocksOfOnTransact(new ExceptionalUnitGraph(onTransactMethod.retrieveActiveBody()));
		allIds.addAll(keyToSwitchBlocks.keySet());
		int returnValues = getEntryPointsFromOnTransactAndWrappers(onTransactMethod, keyToSwitchBlocks, binder, ret);
		boolean currentClassMethodInvokesFound = (returnValues & 2) == 2;
		boolean hasSuperOnTransact = (returnValues & 1) == 1;
		
		//Remove all methods declared in the binder class as these are just noise except in cases where
		//the super onTransact method is called, one of a select number of methods in the binder class
		//are overridden in the stub, and the transaction id for these methods in the super class is not
		//used for something else in the stub onTransact
		for(Iterator<SootClass> it = ret.keySet().iterator(); it.hasNext();) {
			SootClass sc = it.next();
			Map<SootMethod,Set<Integer>> epToIds = ret.get(sc);
			epToIds.keySet().removeAll(binderMethods);
			if(epToIds.isEmpty())
				it.remove();
		}
		
		if(currentClassMethodInvokesFound) {
			if(ret.size() == 0)
				logger.info("The onTransact method's invoked entry points could not be resolved to concrete methods for class {}.",
						onTransactMethod.getDeclaringClass().getName());
		} else {
			logger.info("The onTransact method does not invoke entry points for class {}.",
					onTransactMethod.getDeclaringClass().getName());
		}
		
		//Add back in any methods in the declared in the Binder class (i.e. the super) who are called
		//in the onTransact of Binder, overridden in the current stub, and who have transition ids
		//associated with them that are not used for something else in the current stub 
		if(hasSuperOnTransact)
			ret = handleSuperOnTransact(onTransactMethod.getDeclaringClass(),ret,allIds,baseOnTransact);
			
		return ret;
	}
	
	private Map<SootClass,Map<SootMethod,Set<Integer>>> handleSuperOnTransact(SootClass start, 
			Map<SootClass,Map<SootMethod,Set<Integer>>> in, 
			Set<Integer> allIds, Map<SootMethod,Pair<Set<SootMethod>,Set<Integer>>> baseOnTransact) {
		Set<SootClass> subClasses;
		if(start.isInterface())
			subClasses = HierarchyHelpers.getAllSubClassesOfInterface(start);
		else
			subClasses = HierarchyHelpers.getAllSubClasses(start);
		Map<Integer,Map<SootClass,Set<SootMethod>>> overriddenMethods = new HashMap<>();
		for(SootClass subClass : subClasses) {
			for(SootMethod binderMethod : baseOnTransact.keySet()) {
				Pair<Set<SootMethod>,Set<Integer>> p = baseOnTransact.get(binderMethod);
				SootMethod binderSubMethod = null;
				boolean found = false;
				//The call graph of methods also in the Binder class called from on the of methods 
				//used as an entry point in Binder's onTransact. If any of these are overridden 
				//then it means the functionality of the entry point changed from doing nothing to
				//doing something possibly important. Only in this case do we want to include these
				//since in all other cases the functionality of the default entry points in Binder
				//are basically no ops.
				for(SootMethod sm : p.getFirst()) {
					SootMethod overriddenMethod = subClass.getMethodUnsafe(sm.getSubSignature());
					if(overriddenMethod != null && overriddenMethod.isConcrete()) {
						if(sm.equals(binderMethod))
							binderSubMethod = overriddenMethod;
						found = true;
					}
				}
				//A sub class can override any of the methods in an ep's call graph including the
				//ep itself. If any of these are overridden then it means the ep should be included
				//in the list of eps. The question is should it be the ep declared in the Binder 
				//class or should it be an ep declared in a sub class that overrides the Binder
				//ep. If we detect that a Binder ep has been overridden then the overriding ep is 
				//included instead of the Binder ep. Otherwise, the Binder ep is included.
				if(found) {
					for(Integer i : p.getSecond()) {
						Map<SootClass,Set<SootMethod>> temp = overriddenMethods.get(i);
						if(temp == null) {
							temp = new HashMap<>();
							overriddenMethods.put(i, temp);
						}
						Set<SootMethod> temp2 = temp.get(subClass);
						if(temp2 == null) {
							temp2 = new HashSet<>();
							temp.put(subClass,temp2);
						}
						if(binderSubMethod != null)
							temp2.add(binderSubMethod);
						else
							temp2.add(binderMethod);
					}
				}
			}
		}
		
		//If a Binder ep method is mapped to a transaction id which has already been used in
		//the start classes onTransact then this implies that the start classes onTransact
		//method has a completely separate handle for this transaction id. So we don't include
		//such Binder eps in the returned set.
		boolean addedEps = false;
		for(Integer i : overriddenMethods.keySet()) {
			if(!allIds.contains(i)) {
				Map<SootClass, Set<SootMethod>> serviceToEps = overriddenMethods.get(i);
				for(SootClass service : serviceToEps.keySet()) {
					for(SootMethod sm : serviceToEps.get(service)) {
						Map<SootMethod,Set<Integer>> epToId = in.get(service);
						if(epToId == null) {
							epToId = new HashMap<>();
							in.put(service, epToId);
						}
						Set<Integer> ids = epToId.get(sm);
						if(ids == null) {
							ids = new HashSet<>();
							epToId.put(sm, ids);
						}
						ids.add(i);
						addedEps = true;
					}
				}
				allIds.add(i);
			}
		}
		if(addedEps)
			logger.info("Added entry points from Binder to the list of entry points for {}.",
					start.getName());
		
		return in;
	}
	
	//For those methods where there is really no way of eliminating them other than manually excluding them
	private boolean skip(SootMethodRef methodRef){
		if(skipMethods.contains(methodRef.getSignature()) || objSubSigs.contains(methodRef.getSubSignature().getString())){
			return true;
		}
		return false;
	}
	
	/** Constructs and returns a map of all the integer key to switch blocks
	 * for all the entries of a switch statement in the given onTransact's body. The
	 * switch statement is the first switch statement that switches on the first
	 * argument of the onTransact method (which is the transaction id). If a switch
	 * statement cannot be identified (for example because it is not directly referencing
	 * the local assigned the ParameterRef of the first parameter) then we simply select
	 * the first switch statement encountered as the switch of the onTransact method.
	 * The iteration order is a downward BFS and dependent on the type of the 
	 * UnitGraph given. Also, the search starts at the heads given by the UnitGraph.
	 * If no switch statement exists in the method body then a empty map is returned.
	 * Otherwise, a non-empty map should always be returned as a switch statement 
	 * should always have at least a default entry and thus a default block.
	 */
	private Map<Integer,Set<Unit>> getSwitchBlocksOfOnTransact(UnitGraph g){
		AdvLocalDefs defsFinder = new AdvLocalDefs(g,LiveLocals.Factory.newLiveLocals(g));
		AdvLocalUses usesFinder = new AdvLocalUses(g, defsFinder);
		Value tranIdParam = g.getBody().getParameterRefs().get(0);
		Set<SwitchStmt> switches = new HashSet<>();
		Set<IfStmt> ifs = new HashSet<>();
		HashSet<Unit> visited = new HashSet<>();
		Queue<Unit> toVisit = new ArrayDeque<>();
		toVisit.addAll(g.getHeads());
		while(!toVisit.isEmpty()) {
			Unit cur = toVisit.poll();
			if (!visited.add(cur)) {
				continue;
			}
			if(cur instanceof DefinitionStmt) {
				DefinitionStmt def = (DefinitionStmt)cur;
				Value rightOfCur = def.getRightOp();
				if(rightOfCur.equals(tranIdParam)) {
					for(UnitValueBoxPair uuse : usesFinder.getUsesWithAliasesRemoveLocalAndCast(def)) {
						Unit use = uuse.getUnit();
						if(use instanceof SwitchStmt)
							switches.add((SwitchStmt)use);
						else if(use instanceof IfStmt)
							ifs.add((IfStmt)use);
					}
				}
			}
			toVisit.addAll(g.getSuccsOf(cur));
		}
		
		Map<Integer,Set<Unit>> ret = new HashMap<>();
		for(SwitchStmt s : switches) {
			for(Map.Entry<Integer, Set<Unit>> e : getBlocksOfSwitch(g,s,switches,ifs).entrySet()) {
				if(ret.containsKey(e.getKey())) {
					throw new RuntimeException("Error: Conflicting target blocks for transaction id " + e.getKey() 
						+ " in '" + g.getBody().getMethod() + "'");
				}
				ret.put(e.getKey(),e.getValue());
			}
		}
		for(IfStmt s : ifs) {
			for(Map.Entry<Integer, Set<Unit>> e : getBlockOfIf(g,s,switches,ifs).entrySet()) {
				if(ret.containsKey(e.getKey())) {
					throw new RuntimeException("Error: Conflicting target blocks for transaction id " + e.getKey() 
						+ " in '" + g.getBody().getMethod() + "'");
				}
				ret.put(e.getKey(),e.getValue());
			}
		}
		
		if(ret.isEmpty()) {
			logger.info("No conditional blocks found to use the transaction id argument in '{}'. The transaction id to entry point map will not be generated.", 
					g.getBody().getMethod());
			return Collections.emptyMap();
		}
		return ret;
	}
	
	private Map<Integer, Set<Unit>> getBlockOfIf(UnitGraph g, IfStmt ifStmt, Set<SwitchStmt> switches, Set<IfStmt> ifs) {
		Value v = ifStmt.getCondition();
		if(v instanceof ConditionExpr) {
			Value op1 = ((ConditionExpr)v).getOp1();
			Value op2 = ((ConditionExpr)v).getOp2();
			int transId = -1;
			if(op1 instanceof IntConstant) {
				transId = ((IntConstant)op1).value;
			} else if(op2 instanceof IntConstant) {
				transId = ((IntConstant)op2).value;
			} else {
				throw new RuntimeException("Error: Unexpected format for the if statement. For '" + ifStmt 
						+ "' of '" + g.getBody().getMethod() + "'");
			}
			
			//The if statement should not show up by itself because this was originally a single switch block.
			//So it should have a branch that leads to an entry point and a branch that leads to other identified
			//switches of ifs. The only case it does not lead to these would be if the if statement is last (i.e.
			//is the default of the switch.
			Set<Unit> foundBlock = null;
			Map<Unit,Set<Unit>> succsToBlocks = new HashMap<>();
			List<Unit> targets;
			//Ensure we only have the branching targets of an if and not the exceptional ones
			if(g instanceof ExceptionalUnitGraph) {
				targets = ((ExceptionalUnitGraph)g).getUnexceptionalSuccsOf(ifStmt);
			} else {
				targets = g.getSuccsOf(ifStmt);
			}
			for(Unit succ : targets) {
				Set<Unit> block = getBlockOfStmt(succ,g);
				
				boolean containsOthers = false;
				for(SwitchStmt s : switches) {
					if(block.contains(s))
						containsOthers = true;
				}
				for(IfStmt s : ifs) {
					if(block.contains(s))
						containsOthers = true;
				}
				if(!containsOthers) {
					if(foundBlock == null) {
						foundBlock = block;
					}
					succsToBlocks.put(succ, block);
				}
			}
			
			if(foundBlock == null) {
				throw new RuntimeException("Error: The if statement does not contain a branch without other identified switch and "
						+ "if statements. For '" + ifStmt + "' of '" + g.getBody().getMethod() + "'");
			} else {
				if(succsToBlocks.size() == 1) {
					return Collections.singletonMap(transId, addBodyOfOnTransactEpWrapperMethods(foundBlock, g.getBody().getMethod().getDeclaringClass()));
				} else {
					Set<Unit> defaultBlock = null;
					Set<Unit> otherBlock = null;
					for(Map.Entry<Unit, Set<Unit>> e : succsToBlocks.entrySet()) {
						boolean found = false;
						for(Unit u : e.getValue()) {
							if(
								(((Stmt)u).containsInvokeExpr() && ((Stmt)u).getInvokeExpr().getMethodRef().getSubSignature().toString().equals(
									"boolean onTransact(int,android.os.Parcel,android.os.Parcel,int)"))
								|| (u instanceof ReturnStmt && ((ReturnStmt)u).getOp() instanceof IntConstant && 
										((IntConstant)((ReturnStmt)u).getOp()).value == 0)
							  ) {
								found = true;
								break;
							}
						}
						if(found)
							defaultBlock = e.getValue();
						else
							otherBlock = e.getValue();
					}
					if(defaultBlock == null || otherBlock == null) {
						logger.warn("The super onTransact is called on both branchs of the if '{}'. This is not"
								+ " a valid entry point if block. Ignoring it for '{}'",ifStmt,g.getBody().getMethod());
					}
					Map<Integer, Set<Unit>> ret = new HashMap<>();
					ret.put(null, addBodyOfOnTransactEpWrapperMethods(defaultBlock, g.getBody().getMethod().getDeclaringClass()));
					ret.put(transId, addBodyOfOnTransactEpWrapperMethods(otherBlock, g.getBody().getMethod().getDeclaringClass()));
					return ret;
				}
			}
		} else {
			throw new RuntimeException("Error: If statement does not contain a conditional expression? For '" + ifStmt 
					+ "' of '" + g.getBody().getMethod() + "'");
		}
	}
	
	private Set<Unit> getBlockOfStmt(Unit start, UnitGraph g) {
		HashSet<Unit> visited = new HashSet<>();
		Queue<Unit> toVisit = new ArrayDeque<>();
		toVisit.add(start);
		while(!toVisit.isEmpty()){
			Unit cur = toVisit.poll();
			if (!visited.add(cur)) {
				continue;
			}
			toVisit.addAll(g.getSuccsOf(cur));
		}
		return visited;
	}
	
	private Set<Unit> addBodyOfOnTransactEpWrapperMethods(Set<Unit> in, SootClass onTransactClass) {
		if(in == null)
			return Collections.emptySet();
		for(Unit u : new HashSet<>(in)) {
			if(((Stmt)u).containsInvokeExpr()) {
				SootMethodRef smr = ((Stmt)u).getInvokeExpr().getMethodRef();
				if(smr.name().startsWith("onTransact$") && smr.name().endsWith("$") && smr.declaringClass().equals(onTransactClass)) {
					SootMethod sm = onTransactClass.getMethodUnsafe(smr.getSubSignature());
					if(sm == null) {
						throw new RuntimeException("Error: Could not resolve the on transact method wrapper '" + smr + "' to a method in class '" 
								+ onTransactClass + "'. Is it not in the on transact class?!?");
					} else {
						in.addAll(sm.retrieveActiveBody().getUnits());
					}
				}
			}
		}
		return in;
	}
	
	/** Constructs and returns a map of all the integer key to switch blocks of a given
	 * switch statement. A switch block is all the units of a method body that are reachable
	 * starting at a switches target unit and traversing downwards until an exit node is 
	 * reached (inclusive). switchGenKeyToTargetMap is used to get the initial map of 
	 * integer key to target units. Note this map should never be empty because the map
	 * returned by switchGenKeyToTargetMap should never be empty due to the default entry.
	 * If the default points of another switch or if statement that we have identified,
	 * the ignore the default entry as it will be handled separately.
	 */
	private Map<Integer, Set<Unit>> getBlocksOfSwitch(UnitGraph g, SwitchStmt switchStmt, Set<SwitchStmt> switches, Set<IfStmt> ifs){
		Map<Integer,Unit> keyToTargetMapOfSwitch = switchGenKeyToTargetMap(switchStmt);
		Map<Integer, Set<Unit>> ret = new HashMap<>();
		for(Map.Entry<Integer, Unit> e : keyToTargetMapOfSwitch.entrySet()){
			if(e.getKey() != null) {
				ret.put(e.getKey(), addBodyOfOnTransactEpWrapperMethods(getBlockOfStmt(e.getValue(),g), g.getBody().getMethod().getDeclaringClass()));
			} else {
				Set<Unit> block = getBlockOfStmt(e.getValue(), g);
				if(Collections.disjoint(block, switches) && Collections.disjoint(block, ifs))
					ret.put(e.getKey(), addBodyOfOnTransactEpWrapperMethods(block, g.getBody().getMethod().getDeclaringClass()));
			}
		}
		return ret;
	}
	
	/** Constructs and returns a map of all the integer key to unit targets of a 
	 * given switch statement. This map also includes the default key and target 
	 * pair which is represented by a null key entry in the map as the default 
	 * entry of a switch statement does not have a integer key. Note that as far
	 * as I can tell a switch statement will always have a default entry even if
	 * it does nothing. This method will throw an exception if the SwitchStmt is
	 * not one of the two types of switch statements.
	 */
	private Map<Integer,Unit> switchGenKeyToTargetMap(SwitchStmt switchStmt){
		HashMap<Integer,Unit> ret = new HashMap<>();
		if(switchStmt instanceof LookupSwitchStmt){
			LookupSwitchStmt u = (LookupSwitchStmt)switchStmt;
			List<IntConstant> lookupValues = u.getLookupValues();
			for(int i = 0; i < lookupValues.size(); i++){
				ret.put(lookupValues.get(i).value, u.getTarget(i));
			}
			ret.put(null, u.getDefaultTarget());
		}else if(switchStmt instanceof TableSwitchStmt){
			TableSwitchStmt u = (TableSwitchStmt)switchStmt;
			int low = u.getLowIndex();
			int high = u.getHighIndex();
			for (int i = low; i <= high; i++) {
				Unit target = u.getTarget(i - low);
				ret.put(i, target);
				//Make sure the last increment does not overflow i and result in an infinite loop
				if(i == high)
					break;
			}
			ret.put(null, u.getDefaultTarget());
		}else{
			throw new RuntimeException("Error: Unrecognized SwitchStmt type " + switchStmt.getClass().toString());
		}
		return ret;
	}

}
