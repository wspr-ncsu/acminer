package org.sag.acminer.phases.bindergroups;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.phases.entrypoints.EntryPointsDatabase.IntegerWrapper;
import org.sag.common.logging.ILogger;
import org.sag.common.tools.HierarchyHelpers;
import org.sag.common.tools.SortingMethods;
import org.sag.soot.SootSort;

import soot.Body;
import soot.FastHierarchy;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Unit;
import soot.Value;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

public final class GenerateBinderGroups {
	
	private final String iinterfaceFullClassName = "android.os.IInterface";
	private final String transactSubSignature = "boolean transact(int,android.os.Parcel,android.os.Parcel,int)";
	private final String binderFullClassName = "android.os.Binder";
	private final String iBinderFullClassName = "android.os.IBinder";
	private final String binderProxyFullClassName = "android.os.BinderProxy";
	
	private Map<SootClass,Set<SootMethod>> binderInterfacesToMethods;
	private Map<SootClass,Map<SootClass,Map<SootMethod,Set<Integer>>>> binderInterfacesToStubsToMethods;
	private Map<SootClass,Map<SootClass,Map<SootMethod,Set<Integer>>>> binderInterfacesToProxiesToMethods;
	private Map<SootClass,Map<SootClass,Set<Integer>>> binderInterfacesToStubsToAllTransactionIds;
	
	private ILogger logger;
	private IACMinerDataAccessor dataAccessor;

	public GenerateBinderGroups(IACMinerDataAccessor dataAccessor, ILogger logger) {
		this.logger = logger;
		this.binderInterfacesToMethods = new LinkedHashMap<>();
		this.binderInterfacesToStubsToMethods = new LinkedHashMap<>();
		this.binderInterfacesToProxiesToMethods = new LinkedHashMap<>();
		this.binderInterfacesToStubsToAllTransactionIds = new LinkedHashMap<>();
		this.dataAccessor = dataAccessor;
	}
	
	public BinderGroupsDatabase constructAndSetNewDatabase(){
		findBinderGroups();
		return BinderGroupsDatabase.getNewBinderGroupsDatabase(binderInterfacesToMethods, binderInterfacesToProxiesToMethods, 
				binderInterfacesToStubsToMethods,binderInterfacesToStubsToAllTransactionIds);
	}
	
	private void findBinderGroups(){
		SootClass iinterface = Scene.v().getSootClass(iinterfaceFullClassName);
		SootClass binder = Scene.v().getSootClass(binderFullClassName);
		SootClass iBinder = Scene.v().getSootClass(iBinderFullClassName);
		SootClass binderProxy = Scene.v().getSootClass(binderProxyFullClassName);
		FastHierarchy fh = Scene.v().getOrMakeFastHierarchy();
		Map<SootClass,Map<SootMethod,Set<IntegerWrapper>>> stubToEps = dataAccessor.getEntryPointsByStubWithTransactionId();
		Map<SootClass,Set<IntegerWrapper>> stubToAllIds = dataAccessor.getStubsToAllTransactionIds();
		Set<SootClass> stubsAndSubClasses = new HashSet<>();
		
		//Get all sub interfaces of IInterface (includes IInterface)
		Set<SootClass> binderInterfaces = new HashSet<>(fh.getAllSubinterfaces(iinterface));
		binderInterfaces.add(iBinder);//Manually add IBinder because it does not extend IInterface
		binderInterfaces.remove(iinterface);//remove IInterface because it is a special case
		binderInterfaces = SortingMethods.sortSet(binderInterfaces,SootSort.scComp); 
		
		//Generate a set of all stub classes and their implementations
		for(SootClass stub : stubToEps.keySet()){
			stubsAndSubClasses.addAll(HierarchyHelpers.getAllSubClasses(stub));
		}
		
		//Get all indirect and direct subclasses of Binder
		Set<SootClass> allSubclassesOfBinder = HierarchyHelpers.getAllSubClasses(binder);
		allSubclassesOfBinder.remove(binder);//Ignore binder itself because it is unimportant
		
		//Get Only the direct subclasses of Binder
		Set<SootClass> directSubClassesOfBinder = new HashSet<>(fh.getSubclassesOf(binder));
		
		for(SootClass iisc : binderInterfaces){
			//Manually include IBinder, Binder, and BinderProxy so these classes get excluded and for completeness
			if(iisc.equals(iBinder)){
				binderInterfacesToMethods.put(iBinder, Collections.<SootMethod>emptySet());
				LinkedHashMap<SootClass,Map<SootMethod,Set<Integer>>> temp = new LinkedHashMap<>();
				temp.put(binderProxy, Collections.<SootMethod,Set<Integer>>emptyMap());
				binderInterfacesToProxiesToMethods.put(iBinder, temp);
				temp = new LinkedHashMap<>();
				temp.put(binder, Collections.<SootMethod,Set<Integer>>emptyMap());
				binderInterfacesToStubsToMethods.put(iBinder, temp);
				Map<SootClass,Set<Integer>> temp2 = new LinkedHashMap<>();
				temp2.put(binder, Collections.<Integer>emptySet());
				binderInterfacesToStubsToAllTransactionIds.put(iBinder, temp2);
			}else{
				//Generate the methods of the current interface and store them in a field sorted
				generateMethodsForInterface(iisc);
				//Generate the proxy classes, methods, and transaction ids. Also read in the stub classes, entry points, and transaction ids
				//from the entry points database and add any empty stub classes found. Store all this in the appropriate fields sorted.
				generateProxyAndStubsForInterface(iisc, stubToEps, stubsAndSubClasses, allSubclassesOfBinder, directSubClassesOfBinder, 
						stubToAllIds);
			}
		}
	}
	
	private void generateProxyAndStubsForInterface(SootClass iisc, 
			Map<SootClass,Map<SootMethod,Set<IntegerWrapper>>> stubToEps, 
			Set<SootClass> stubsAndSubClasses, Set<SootClass> allSubclassesOfBinder, Set<SootClass> directSubClassesOfBinder, 
			Map<SootClass,Set<IntegerWrapper>> stubToAllIds){
		
		Map<SootClass,Map<SootMethod,Set<Integer>>> binderProxies = new HashMap<>();
		Map<SootClass,Map<SootMethod,Set<Integer>>> binderStubs = new HashMap<>();
		Map<SootClass,Set<Integer>> binderStubsToAllTransactionIds = new HashMap<>();
		
		//Separate proxies from the stub classes and their implementations
		for(SootClass subClass : HierarchyHelpers.getAllSubClassesOfInterface(iisc)){
			if(!subClass.isInterface() && !subClass.isPhantom()){//sanity check
				if(stubToEps.keySet().contains(subClass)){//Stub found before in entry points is definitely a stub now
					Map<SootMethod,Set<IntegerWrapper>> epsToId = stubToEps.get(subClass);
					HashMap<SootMethod,Set<Integer>> temp = new HashMap<>();
					for(SootMethod ep : epsToId.keySet()){
						HashSet<Integer> temp2 = new HashSet<>();
						for(IntegerWrapper i : epsToId.get(ep)){
							temp2.add(i.isNull() ? null : i.getInteger());
						}
						temp.put(ep, temp2);
					}
					binderStubs.put(subClass, temp);
					Set<Integer> allTransactionIds = new HashSet<>();
					for(IntegerWrapper i : stubToAllIds.get(subClass)){
						allTransactionIds.add(i.isNull() ? null : i.getInteger());
					}
					binderStubsToAllTransactionIds.put(subClass, SortingMethods.sortSet(allTransactionIds,SortingMethods.iComp));
				} else if(!stubsAndSubClasses.contains(subClass)){//Ignore all already found stubs and their sub classes as these are stubs and services
					if(!isUnwantedProxy(subClass)){//remove a few test proxy implementations that don't matter
						//if the sub-class extends binder indirectly or directly then it is not a proxy
						//it may be an empty stub class that was not captured because it is empty or a class that implements an empty stub class
						if(allSubclassesOfBinder.contains(subClass)){
							//if the class directly implements binder then it is a empty stub class
							//otherwise it is likely just some class that extends the empty stub class (we add these to a separate list)
							//before it has to implement onTransact as well but we have relaxed this restriction because some do not
							//implement onTransact but instead rely on the default onTransact method in Binder
							if(directSubClassesOfBinder.contains(subClass)){
								logger.info("GenerateBinderGroups: Adding new empty stub '{}' to stubs list for interface '{}'."
										+ " This was not captured in the entry point generation because its onTransact method contained"
										+ " no resolvable entry points. These are often used as place holders for stub classes"
										+ " implemented in native.",subClass,iisc);
								binderStubs.put(subClass, new HashMap<SootMethod,Set<Integer>>());
								binderStubsToAllTransactionIds.put(subClass, new HashSet<Integer>());
							}else{
								logger.info("GenerateBinderGroups: Ignoring class '{}' for interface '{}' since this class contains"
										+ " no entry points and is not a stub (i.e. it does not directly extends Binder and contain "
										+ " an onTransact method).",subClass,iisc);
							}
						}else{
							Map<SootMethod,Set<Integer>> proxyMethods = binderProxies.get(subClass);
							if(proxyMethods == null){
								proxyMethods = new HashMap<>();
								binderProxies.put(subClass, proxyMethods);
							}
							for(SootMethod m : getMethodsForProxy(subClass)){
								proxyMethods.put(m, Collections.<Integer>emptySet());
							}
						}
					}
				}
			}
		}
		
		//Remove proxy wrappers from proxy list if possible
		tryForSingleProxy(iisc,binderProxies,binderStubs);
		//Mine the transaction id from each proxy methods transact call if it exists
		generateTransactionIdsForProxyMethods(iisc,binderProxies);
		//Sort all the generated information and add it to the appropriate fields
		binderInterfacesToProxiesToMethods.put(iisc, sortProxyStubMap(binderProxies));
		binderInterfacesToStubsToMethods.put(iisc, sortProxyStubMap(binderStubs));
		binderInterfacesToStubsToAllTransactionIds.put(iisc, SortingMethods.sortMapKey(binderStubsToAllTransactionIds,SootSort.scComp));
	}
	
	private Map<SootClass,Map<SootMethod,Set<Integer>>> sortProxyStubMap(Map<SootClass,Map<SootMethod,Set<Integer>>> in){
		for(SootClass sc : in.keySet()){
			Map<SootMethod,Set<Integer>> temp = SortingMethods.sortMapKey(in.get(sc),SootSort.smComp);
			in.put(sc, temp);
			for(SootMethod sm : temp.keySet()){
				temp.put(sm, SortingMethods.sortSet(temp.get(sm),SortingMethods.iComp));
			}
		}
		in = SortingMethods.sortMapKey(in, SootSort.scComp);
		return in;
	}
	
	private void generateTransactionIdsForProxyMethods(SootClass iisc, Map<SootClass,Map<SootMethod,Set<Integer>>> binderProxies){
		for(SootClass proxy : binderProxies.keySet()){
			Map<SootMethod,Set<Integer>> proxyMethodToId = binderProxies.get(proxy);
			for(SootMethod proxyMethod : proxyMethodToId.keySet()){
				Body b = proxyMethod.retrieveActiveBody();
				HashSet<Integer> transactIds = new HashSet<>();
				for(Unit u : b.getUnits()){
					if(u instanceof Stmt && ((Stmt)u).containsInvokeExpr()){
						InvokeExpr invokeExpr = ((Stmt)u).getInvokeExpr();
						SootMethodRef methodRef = invokeExpr.getMethodRef();
						if(methodRef.getSubSignature().getString().equals(transactSubSignature)){
							Value temp = invokeExpr.getArg(0);
							if(temp instanceof IntConstant){
								transactIds.add(((IntConstant)temp).value);
							}else{
								throw new RuntimeException("Error: Expected transacts paramater 0 value to be of type IntConstant but got " 
										+ temp.getClass() + ".");
							}
						}
					}
				}
				if(transactIds.isEmpty()){
					logger.info("GenerateBinderGroups: Could not find a transact method invoke in method '{}'. No "
							+ "proxy to entry points mapping will be generated for this proxy method of interface '{}'.",proxyMethod,iisc);
				}else{
					proxyMethodToId.put(proxyMethod, transactIds);
				}
			}
		}
	}
	
	/** If there is more than one proxy, try to pick the one with the standard proxy
	 * name as this is the only way left to identify the actual proxy class. If no
	 * such class exists then keep all of them. Note we can do this because all of
	 * the additional classes identified as proxies were either wrappers for an actual
	 * proxy class, classes that did nothing, or classes that redirected the program flow
	 * elsewhere but not through a proxy or binder. In other words, ignoring them and not
	 * excluding them should have little effect on our analysis. We record these classes
	 * in a separate set just to be safe though.
	*/
	private void tryForSingleProxy(SootClass iisc, Map<SootClass,Map<SootMethod,Set<Integer>>> binderProxies, 
			Map<SootClass,Map<SootMethod,Set<Integer>>> binderStubs){
		
		if(binderProxies.size() > 1){
			HashSet<SootClass> toRemove = new HashSet<>();
			for(SootClass proxy : binderProxies.keySet()){
				boolean keep = false;
				for(SootClass stub : binderStubs.keySet()){
					if(proxy.getName().startsWith(stub.getName() + "$")){
						keep = true;
						break;
					}
				}
				if(!keep)
					toRemove.add(proxy);
			}
			if(toRemove.size() != binderProxies.keySet().size()){
				for(SootClass sc : toRemove){
					logger.info("GenerateBinderGroups: Removing class '{}' from the proxy list of '{}' because while it implements the"
							+ " interface, it is not an inner class of one of its stubs. Thus at best it is a wrapper for an actual proxy class"
							+ " and at worst it is some custom implementation.",sc,iisc);
					binderProxies.remove(sc);
				}
			}
		}
	}
	
	private boolean isUnwantedProxy(SootClass sc) {
		return sc.getName().startsWith("android.test.mock.") 
				|| sc.getName().startsWith("com.android.uiautomator.testrunner.");
	}
	
	private Set<SootMethod> getMethodsForProxy(SootClass sc){
		Set<SootMethod> ret = new HashSet<SootMethod>();
		for(SootMethod m : sc.getMethods()){
			if(!m.getSubSignature().equals("android.os.IBinder asBinder()") //remove all asBinder methods because these do not go through binder
					&& !m.getSubSignature().equals("java.lang.String getInterfaceDescriptor()") //remove all of these methods which are auto generated gets for the interface string descriptor
					&& !m.getName().equals("<init>") //remove the constructors because we only care about the other methods
					&& !m.getName().equals("<clinit>") //remove the static constructors 
					&& !m.getName().matches("^access\\$\\d{3}")){ //remove the outer class field access methods that are auto generated by the compiler
				ret.add(m);
			}
		}
		return ret;
	}
	
	private void generateMethodsForInterface(SootClass iisc){
		Set<SootMethod> ret = new HashSet<SootMethod>();
		for(SootMethod m : iisc.getMethods()){
			if(!m.getSubSignature().equals("android.os.IBinder asBinder()") //remove all asBinder methods because these do not go through binder
					&& !m.getName().equals("<clinit>")){ //remove the static constructors 
				ret.add(m);
			}
		}
		Set<SootMethod> temp = binderInterfacesToMethods.get(iisc);
		if(temp != null){
			ret.addAll(temp);
		}
		binderInterfacesToMethods.put(iisc, SortingMethods.sortSet(ret,SootSort.smComp));
	}
	
}
