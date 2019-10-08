package org.sag.acminer.phases.binderservices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.logging.ILogger;
import org.sag.common.tools.HierarchyHelpers;
import org.sag.common.tools.SortingMethods;
import org.sag.common.tuple.Quad;
import org.sag.soot.SootSort;
import org.sag.soot.analysis.AdvLocalDefs;
import org.sag.soot.analysis.AdvLocalUses;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import heros.solver.IDESolver;
import soot.AnySubType;
import soot.ArrayType;
import soot.BooleanType;
import soot.Local;
import soot.NullType;
import soot.PrimType;
import soot.RefLikeType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootFieldRef;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.dexpler.typing.UntypedConstant;
import soot.RefType;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.ClassConstant;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.MethodHandle;
import soot.jimple.MethodType;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.NullConstant;
import soot.jimple.NumericConstant;
import soot.jimple.ParameterRef;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.ThisRef;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.LiveLocals;
import soot.toolkits.scalar.UnitValueBoxPair;

public class DiscoverBinderServices {
	
	private final LoadingCache<SootMethod,UnitGraph> methodToUnitGraph = 
		IDESolver.DEFAULT_CACHE_BUILDER.build( new CacheLoader<SootMethod,UnitGraph>() {
		@Override
		public UnitGraph load(SootMethod sm) throws Exception {
			return new ExceptionalUnitGraph(sm.retrieveActiveBody());
		}
	});
	
	private final LoadingCache<UnitGraph,AdvLocalDefs> unitGraphToLocalDefs = 
		IDESolver.DEFAULT_CACHE_BUILDER.build( new CacheLoader<UnitGraph,AdvLocalDefs>() {
		@Override
		public AdvLocalDefs load(UnitGraph g) throws Exception {
			return new AdvLocalDefs(g,LiveLocals.Factory.newLiveLocals(g));
		}
	});	
	
	private final LoadingCache<SootClass,Set<SootClass>> classToSubClasses = 
		IDESolver.DEFAULT_CACHE_BUILDER.build( new CacheLoader<SootClass,Set<SootClass>>() {
		@Override
		public Set<SootClass> load(SootClass sc) throws Exception {
			if(sc.isInterface())
				return HierarchyHelpers.getAllSubClassesOfInterface(sc);
			else
				return HierarchyHelpers.getAllSubClasses(sc);
		}
	});
	
	private final LoadingCache<UnitGraph,AdvLocalUses> unitGraphToLocalUses = 
		IDESolver.DEFAULT_CACHE_BUILDER.build(new CacheLoader<UnitGraph,AdvLocalUses>() {
		@Override
		public AdvLocalUses load(UnitGraph g) throws Exception {
			return new AdvLocalUses(g,unitGraphToLocalDefs.getUnchecked(g));
		}
	});
	
	private final Set<SootClass> iinterfaceSubInterfaces;
	private final Set<SootClass> iinterfaceSubClasses;
	private final String binderFullClassName = "android.os.Binder";
	private final String iBinderFullClassName = "android.os.IBinder";
	private final String iinterfaceFullClassName = "android.os.IInterface";
	private final SootClass binder;
	private final SootClass ibinder;
	
	private ILogger logger;
	private IACMinerDataAccessor dataAccessor;

	public DiscoverBinderServices(IACMinerDataAccessor dataAccessor, ILogger logger) {
		this.logger = logger;
		this.dataAccessor = dataAccessor;
		SootClass iinterface = Scene.v().getSootClassUnsafe(iinterfaceFullClassName, false);
		this.iinterfaceSubInterfaces = Scene.v().getOrMakeFastHierarchy().getAllSubinterfaces(iinterface);
		this.iinterfaceSubClasses = HierarchyHelpers.getAllSubClassesOfInterface(iinterface);
		this.binder = Scene.v().getSootClassUnsafe(binderFullClassName, false);
		this.ibinder = Scene.v().getSootClassUnsafe(iBinderFullClassName, false);
	}
	
	/* The string value could be a constant, static/final/inited once field value (hopefully inlined), or
	 * the value of a class name ??
	 * 
	 * this, asBinder() which is this, a field that is hopefully only inited in one place or
	 * at least with one type, a new object created in the calling method, 
	 * 
	 * the default policy when a boolean is not provided is to deny isolated processes (i.e. false)
	 * 
	 * Apparently PacManager takes in a generic name and IBinder object as arguments. Backtracking, we find
	 * that this is a ServiceConnection object which goes through an extremely long and convoluted callgraph to
	 * end up saying that the object could be any IBinder object. However, from the code below it we find that
	 * it always registers under the name "com.android.net.IProxyService" and that the it gets used as a 
	 * IProcyService right after registering it with the ServiceManager (in the same method). So we can safely
	 * assume in this instance it must be some child of the com.android.net.IProxyService interface which is
	 * good enough for us.
	 *  
	 *  http://androidxref.com/8.1.0_r33/xref/system/sepolicy/private/service_contexts
	 */
	private List<String> tempAddSystemServiceSigs = ImmutableList.of(
		"<android.os.ServiceManager: void addService(java.lang.String,android.os.IBinder)>", //false
		"<android.os.ServiceManager: void addService(java.lang.String,android.os.IBinder,boolean)>",
		"<android.os.IServiceManager: void addService(java.lang.String,android.os.IBinder,boolean)>",
		"<android.os.ServiceManagerProxy: void addService(java.lang.String,android.os.IBinder,boolean)>",
		"<android.os.ServiceManagerNative: void addService(java.lang.String,android.os.IBinder,boolean)>",
		"<com.android.server.SystemService: void publishBinderService(java.lang.String,android.os.IBinder)>", //false
		"<com.android.server.SystemService: void publishBinderService(java.lang.String,android.os.IBinder,boolean)>"
	);
	
	//Cast or used as part of ICameraService.Stub.asInterface
	private List<String> tempGetSystemServiceSigs = ImmutableList.of(
		"<android.os.ServiceManager: android.os.IBinder getService(java.lang.String)>",
		"<android.os.ServiceManager: android.os.IBinder getServiceOrThrow(java.lang.String)>",
		"<android.os.IServiceManager: android.os.IBinder getService(java.lang.String)>",
		"<android.os.ServiceManagerProxy: android.os.IBinder getService(java.lang.String)>",
		"<android.os.ServiceManagerNative: android.os.IBinder getService(java.lang.String)>",
		"<com.android.server.SystemService: android.os.IBinder getBinderService(java.lang.String)>",
		"<com.android.server.wifi.FrameworkFacade: android.os.IBinder getService(java.lang.String)>"
	);
	
	/* - Except for in one case, the object registered as a local service are not instances of IBinder but are instead some
	 *   custom internal classes that get registered with a map in the LocalServices class. All the methods in the LocalServices
	 *   class are static. This means LocalServices is simply a means for objects (i.e. other services) in the same process
	 *   to easily access the functionality of another service.
	 * - As none of these are used as IBinder objects (or even instances of IBinder except for one), objects registered as 
	 *   LocalServices should not occur in our binder groups.
	 * - 
	 */
	private List<String> tempAddLocalServiceSigs = ImmutableList.of(
		"<com.android.server.LocalServices: void addService(java.lang.Class,java.lang.Object)>",
		"<com.android.server.SystemService: void publishLocalService(java.lang.Class,java.lang.Object)>"
	);
	
	public Map<SootClass,Map<SootClass,Set<Quad<String,SootMethod,Unit,Boolean>>>> run() {
		//Stub -> Service -> Set<[Name, Source, RegStmt, AllowIsolated]>
		//Stub should not be null but Service could be because there may be stubs without any implementing service
		Map<SootClass,Map<SootClass,Set<Quad<String,SootMethod,Unit,Boolean>>>> ret = new HashMap<>();
		Map<SootMethod,Set<Stmt>> sourceToInvokeStmt = getInvokeStmtsOfSigs(resolveMethodsGivenSignatures(tempAddSystemServiceSigs));
		Map<SootMethod,Set<Stmt>> sourceToServiceGetStmts = null;
		
		for(SootMethod sm : sourceToInvokeStmt.keySet()) {
			AdvLocalDefs defsFinder = unitGraphToLocalDefs.getUnchecked(methodToUnitGraph.getUnchecked(sm));
			for(Stmt u : sourceToInvokeStmt.get(sm)) {
				InvokeExpr ie = u.getInvokeExpr();
				List<Value> args = ie.getArgs();
				List<Type> parmTypes = ie.getMethodRef().parameterTypes();
				int nameIndex = -1;
				int binderIndex = -1;
				int boolIndex = -1;
				for(int i = 0; i < parmTypes.size(); i++) {
					if(parmTypes.get(i).toString().equals("java.lang.String")) {
						if(nameIndex != -1)
							throw new RuntimeException("Error: The method ref '" + ie.getMethodRef() 
								+ "' has more than one string type. Unable to determine the name argument.");
						nameIndex = i;
					} else if(parmTypes.get(i).toString().equals(iBinderFullClassName)) {
						if(binderIndex != -1)
							throw new RuntimeException("Error: The method ref '" + ie.getMethodRef() 
								+ "' has more than one IBinder type. Unable to determine the binder object argument.");
						binderIndex = i;
					} else if(parmTypes.get(i) instanceof BooleanType) {
						if(boolIndex != -1)
							throw new RuntimeException("Error: The method ref '" + ie.getMethodRef() 
								+ "' has more than one boolean type. Unable to determine the allow isolated argument.");
						boolIndex = i;
					}
				}
				if(nameIndex == -1)
					throw new RuntimeException("Error: The method ref '" + ie.getMethodRef() 
						+ "' does not have a string type. Unable to determine the name argument.");
				if(binderIndex == -1)
					throw new RuntimeException("Error: The method ref '" + ie.getMethodRef() 
						+ "' does not have a IBinder type. Unable to determine the binder object argument.");
				
				Value nameArg = args.get(nameIndex);
				String name;
				if(nameArg instanceof StringConstant) {
					name = ((StringConstant)nameArg).value;
				} else {
					throw new RuntimeException("Error: Unhandled value type '" + nameArg.getClass().toString() 
						+ "' for '" + nameArg.toString() + "' when evaluating the name argument.");
				}
				
				boolean allowIsolated;
				if(boolIndex != -1) {
					Value boolArg = args.get(boolIndex);
					if(boolArg instanceof IntConstant) {
						int i = ((IntConstant)boolArg).value;
						if(i == 0)
							allowIsolated = false;
						else if(i == 1)
							allowIsolated = true;
						else
							throw new RuntimeException("Error: Unhandled value '" + i + "' when translating the int constant"
									+ " to boolean for the boolean argument.");
					} else {
						throw new RuntimeException("Error: Unhandled value type '" + boolArg.getClass().toString() 
								+ "' for '" + boolArg.toString() + "' when evaluating the boolean argument.");
					}
				} else {
					allowIsolated = false;
				}
				
				Local l = (Local)args.get(binderIndex);//Objects are always locals as method args
				Set<DefinitionStmt> defs = defsFinder.getDefsWithAliasesAndArraysRemoveLocalAndCastAndArrays(l, u);
				Set<SootClass> servicesRegistered = new HashSet<>();
				for(DefinitionStmt def : defs) {
					Value right = def.getRightOp();
					/* Possible values for the definition of a local: ParameterRef, ThisRef, CaughtExceptionRef, 
					 * ArrayRef, FieldRef, Local, Constant, NewExpr, NewMultiArrayExpr, NewArrayExpr, CastExpr,
					 * InstanceOfExpr, InvokeExpr, BinopExpr, and UnopExpr.
					 * This will be an object of IBinder type so CaughtExceptionRef, Constant, BinopExpr, UnopExpr, 
					 * NewMultiArrayExpr, NewArrayExpr, and InstanceOfExpr are out.
					 * The method used to retrieve the definition stmts also removes all Local, ArrayRef, and
					 * CastExpr.
					 * This leaves ParameterRef, ThisRef, FieldRef, NewExpr, and InvokeExpr.
					 */
					if(right instanceof ParameterRef) {
						//We do not know who calls the source method so all we have to go on is the type of the
						//parameter (which could just be IBinder). We attempt to improve the accuracy below.
						servicesRegistered.add(((RefType)(((ParameterRef)right).getType())).getSootClass());
					} else if(right instanceof ThisRef) {
						//Should always be a RefType and The class will always resolve since it comes from this
						//If it is this it should be the proper type
						servicesRegistered.add(((RefType)(((ThisRef)right).getType())).getSootClass());
					} else if(right instanceof FieldRef) {
						//If the local is assigned a value from a field then the type could be what the field is 
						//declared as or what is assigned to it, so we try to back track to all of the possible
						//assignment stmts of the field and get their types as well
						SootField binderField = resolveFieldRef((FieldRef)right,sm,def);
						if(binderField.isPublic()) {
							servicesRegistered.addAll(findTypesFromFieldAssignments(binderField, Scene.v().getClasses()));
						} else if(binderField.isProtected()) {
							SootClass binderClass = binderField.getDeclaringClass();
							String pkg = binderClass.getPackageName();
							Set<SootClass> subClasses;
							if(binderClass.isInterface()) {
								subClasses = HierarchyHelpers.getAllSubClassesOfInterface(binderClass);
							} else {
								subClasses = HierarchyHelpers.getAllSubClasses(binderClass);
							}
							List<SootClass> temp = new ArrayList<>();
							for(SootClass sc : Scene.v().getClasses()) {
								if(sc.getPackageName().equals(pkg) || subClasses.contains(sc) || binderClass.equals(sc))
									temp.add(sc);
							}
							servicesRegistered.addAll(findTypesFromFieldAssignments(binderField, temp));
						} else if(binderField.isPrivate()) {
							servicesRegistered.addAll(findTypesFromFieldAssignments(binderField, 
								Collections.singleton(binderField.getDeclaringClass())));
						} else {
							SootClass binderClass = binderField.getDeclaringClass();
							String pkg = binderClass.getPackageName();
							List<SootClass> temp = new ArrayList<>();
							for(SootClass sc : Scene.v().getClasses()) {
								if(sc.getPackageName().equals(pkg) || binderClass.equals(sc))
									temp.add(sc);
							}
							servicesRegistered.addAll(findTypesFromFieldAssignments(binderField, temp));
						}
					} else if(right instanceof NewExpr) {
						//The local is assigned a object from a new expression so it is obvious what the type is
						servicesRegistered.add(((NewExpr)right).getBaseType().getSootClass());
					} else if(right instanceof InvokeExpr) {
						//We do not really want to try to determine the types actually returned from the called method since
						//we are doing this analysis without a call graph so we will rely only on the return type (which could
						//be IBinder). We attempt to improve the accuracy below.
						SootMethodRef ref = ((InvokeExpr)right).getMethodRef();
						if(ref.getSubSignature().toString().equals("android.os.IBinder asBinder()")) {
							servicesRegistered.addAll(getServicesFromBinderClass(ref.declaringClass()));
						} else {
							servicesRegistered.add(((RefType)(ref.returnType())).getSootClass());
						}
					} else {
						throw new RuntimeException("Error: Unhandled type '" + right.getClass().getName() + "' for the right hand side of"
							+ " the DefinitionStmts of the IBinder argument used in a registration statement.\n"
							+ "  AddStmt: " + u + "\n"
							+ "  Local: " + l + "\n"
							+ "  Def: " + def + "\n"
							+ "  Source: " + sm
						);
					}
					
					//Remove any instances of java.lang.Object that were added because reflection
					//is used instead of new. Replace them with IBinder only if Object was the
					//only class in our list.
					if(servicesRegistered.remove(Scene.v().getSootClass("java.lang.Object"))) {
						if(servicesRegistered.isEmpty())
							servicesRegistered.add(ibinder);
					}
					
					//Remove any unwanted interface/abstract types that have concrete subclasses in the set
					//since what we want is actually the concrete services if the exist
					if(servicesRegistered.size() > 1) {
						servicesRegistered = removeUnwantedNonConcrete(servicesRegistered);
					}
					
					//If the type IBinder still exists in the set then that means we were unable to 
					//determine the type based on given information when it was registered
					//Maybe the type given when the name is used will tell us the possible services 
					//registered for that name
					//Note if IBinder still exists in the set after the step above to remove
					//unwanted non-concrete services then it should be the only element in the
					//set as it is the parent of all stub classes. Therefore, we want to remove
					//it from the set as this is our last attempt to resolve it to an actual
					//concrete type. Keeping it will cause noise in our analysis so we should
					//instead generate an error that will need to be fixed in newer versions
					//of the code.
					if(servicesRegistered.remove(ibinder)) {
						if(sourceToServiceGetStmts == null)
							sourceToServiceGetStmts = getInvokeStmtsOfSigs(resolveMethodsGivenSignatures(tempGetSystemServiceSigs));
						servicesRegistered.addAll(getServiceFromServiceGetStmts(name, sourceToServiceGetStmts));
					}
					
					Map<SootClass,Set<SootClass>> stubToServices = createStubToServicesMap(servicesRegistered);
					
					if(stubToServices.isEmpty()) {
						throw new RuntimeException("Error: Unable to find the service being registered.\n"
							+ "  AddStmt: " + u + "\n"
							+ "  Local: " + l + "\n"
							+ "  Source: " + sm
						);
					} else if(stubToServices.containsKey(binder)) {
						throw new RuntimeException("Error: '" + binder + "' cannot be a stub.\n"
							+ "  AddStmt: " + u + "\n"
							+ "  Local: " + l + "\n"
							+ "  Source: " + sm + "\n"
							+ "  StubToServices: " + stubToServices
						);
					} else if(stubToServices.containsKey(ibinder)) {
						throw new RuntimeException("Error: '" + ibinder + "' cannot be a stub.\n"
							+ "  AddStmt: " + u + "\n"
							+ "  Local: " + l + "\n"
							+ "  Source: " + sm + "\n"
							+ "  StubToServices: " + stubToServices
						);
					}
					
					for(SootClass stub : stubToServices.keySet()) {
						SootClass toThrow = null;
						if(stubToServices.get(stub).contains(binder))
							toThrow = binder;
						else if(stubToServices.get(stub).contains(ibinder))
							toThrow = ibinder;
						if(toThrow != null)
							throw new RuntimeException("Error: '" + toThrow + "' cannot be a service for stub '" + stub + "'\n"
								+ "  AddStmt: " + u + "\n"
								+ "  Local: " + l + "\n"
								+ "  Source: " + sm + "\n"
								+ "  StubToServices: " + stubToServices
							);
						
						Map<SootClass, Set<Quad<String, SootMethod, Unit, Boolean>>> servicesToData = ret.get(stub);
						if(servicesToData == null) {
							servicesToData = new HashMap<>();
							ret.put(stub, servicesToData);
						}
						Set<SootClass> services = stubToServices.get(stub);
						if(services.isEmpty()) {
							//Can have stubs without any implementing services
							Set<Quad<String, SootMethod, Unit, Boolean>> q = servicesToData.get(null);
							if(q == null) {
								q = new HashSet<>();
								servicesToData.put(null, q);
							}
							q.add(new Quad<>(name,sm,u,allowIsolated));
						} else {
							for(SootClass service : services) {
								Set<Quad<String, SootMethod, Unit, Boolean>> q = servicesToData.get(service);
								if(q == null) {
									q = new HashSet<>();
									servicesToData.put(service, q);
								}
								q.add(new Quad<>(name,sm,u,allowIsolated));
							}
						}
					}
				}
			}
		}
		
		return ret;
	}
	
	/*private Map<SootClass,Set<SootClass>> createStubToServicesMap(Set<SootClass> in) {
		Set<SootClass> processed = new HashSet<>();
		Map<SootClass,Set<SootClass>> ret = new HashMap<>();
		Set<EntryPoint> eps = dataAccessor.getEntryPoints();
		for(SootClass sc : in) {
			Set<SootClass> subClassesOfInput = classToSubClasses.getUnchecked(sc);
			for(EntryPoint ep : eps) {
				SootClass service = ep.getEntryPoint().getDeclaringClass();
				if(service.equals(binder))
					service = ep.getStub();
				if(subClassesOfInput.contains(service)) {
					Set<SootClass> services = ret.get(ep.getStub());
					if(services == null) {
						services = new HashSet<>();
						ret.put(ep.getStub(), services);
					}
					services.add(service);
					processed.add(sc);
				}
			}
		}
		
		//Remove instances where Binder methods caused both a Stub and Service pair to be
		//added and a Service and Service pair (where the Service is in the Service and Service
		//pair isthe same as the Service in the Stub and Service pair).
		if(ret.size() >= 2) {
			Set<SootClass> stubIsService = new HashSet<>();
			Set<SootClass> stubIsNormal = new HashSet<>();
			for(SootClass stub : ret.keySet()) {
				Set<SootClass> services = ret.get(stub);
				if(services.size() == 1 && services.contains(stub))
					stubIsService.add(stub);
				else
					stubIsNormal.add(stub);
			}
			for(SootClass stub : stubIsNormal) {
				Set<SootClass> services = ret.get(stub);
				for(SootClass service : stubIsService) {
					if(services.contains(service))
						ret.remove(service);
				}
			}
		}
		
		if(!processed.equals(in)) {
			in.removeAll(processed);
			throw new RuntimeException("Error: Failed to completly create stubs to services map for input classes:\n"
				+ "  Unprocessed: " + in
			);
		}
		
		return ret;
	}*/
	
	private Map<SootClass,Set<SootClass>> createStubToServicesMap(Set<SootClass> in) {
		Set<SootClass> processed = new HashSet<>();
		Map<SootClass,Set<SootClass>> ret = new HashMap<>();
		Map<SootClass, Map<SootClass, Map<SootMethod, Set<Integer>>>> map = dataAccessor.getBinderStubsAndMethodsByInterface();
		Set<EntryPoint> eps = dataAccessor.getEntryPoints();
		for(SootClass sc : in) {
			Set<SootClass> subClassesOfInput = classToSubClasses.getUnchecked(sc);
			for(SootClass iinterface : map.keySet()) {
				Map<SootClass, Map<SootMethod, Set<Integer>>> stubToEps = map.get(iinterface);
				for(SootClass stub : stubToEps.keySet()) {
					if(iinterface.equals(sc) || subClassesOfInput.contains(stub)) {
						Set<SootClass> services = ret.get(stub);
						if(services == null) {
							services = new HashSet<>();
							ret.put(stub, services);
						}
						for(SootMethod ep : stubToEps.get(stub).keySet()) {
							SootClass service = ep.getDeclaringClass();
							if(service.equals(binder))
								services.add(stub);
							else
								services.add(service);
						}
						processed.add(sc);
					} else {
						for(SootMethod ep : stubToEps.get(stub).keySet()) {
							SootClass service = ep.getDeclaringClass();
							if(service.equals(binder))
								service = stub;
							if(subClassesOfInput.contains(service)) {
								Set<SootClass> services = ret.get(stub);
								if(services == null) {
									services = new HashSet<>();
									ret.put(stub, services);
								}
								services.add(service);
								processed.add(sc);
							}
						}
					}
				}
			}
			//Because BinderGroups and EntryPoints do not always overlap
			if(ret.isEmpty()) {
				for(EntryPoint ep : eps) {
					SootClass service = ep.getEntryPoint().getDeclaringClass();
					if(service.equals(binder))
						service = ep.getStub();
					if(subClassesOfInput.contains(service)) {
						Set<SootClass> services = ret.get(ep.getStub());
						if(services == null) {
							services = new HashSet<>();
							ret.put(ep.getStub(), services);
						}
						services.add(service);
						processed.add(sc);
					}
				}
			}
		}
		//Remove instances where Binder methods caused both a Stub and Service pair to be
		//added and a Service and Service pair (where the Service is in the Service and Service
		//pair isthe same as the Service in the Stub and Service pair).
		if(ret.size() >= 2) {
			Set<SootClass> stubIsService = new HashSet<>();
			Set<SootClass> stubIsNormal = new HashSet<>();
			for(SootClass stub : ret.keySet()) {
				Set<SootClass> services = ret.get(stub);
				if(services.size() == 1 && services.contains(stub))
					stubIsService.add(stub);
				else
					stubIsNormal.add(stub);
			}
			for(SootClass stub : stubIsNormal) {
				Set<SootClass> services = ret.get(stub);
				for(SootClass service : stubIsService) {
					if(services.contains(service))
						ret.remove(service);
				}
			}
		}
		
		if(!processed.equals(in)) {
			in.removeAll(processed);
			throw new RuntimeException("Error: Failed to completly create stubs to services map for input classes:\n"
				+ "  Unprocessed: " + in
			);
		}
		
		return ret;
	}
	
	private Set<SootClass> getServiceFromServiceGetStmts(String name, Map<SootMethod,Set<Stmt>> sourceToServiceGetStmts) {
		Set<SootClass> ret = new HashSet<>();
		for(SootMethod source : sourceToServiceGetStmts.keySet()) {
			for(Stmt invokeStmt : sourceToServiceGetStmts.get(source)) {
				InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();
				Value serviceNameArg = invokeExpr.getArg(0);
				String serviceName = null;
				if(serviceNameArg instanceof StringConstant) {
					serviceName = ((StringConstant)serviceNameArg).value;
				} else if(serviceNameArg instanceof Local) {
					AdvLocalDefs localDefs = unitGraphToLocalDefs.getUnchecked(methodToUnitGraph.getUnchecked(source));
					Set<DefinitionStmt> defs = localDefs.getDefsWithAliasesAndArraysRemoveLocalAndCastAndArrays((Local)serviceNameArg, invokeStmt);
					for(DefinitionStmt def : defs) {
						if(def.getRightOp() instanceof StringConstant) {
							String temp = ((StringConstant)def.getRightOp()).value;
							serviceName = temp.equals(name) ? temp : null;
						}
					}
				} else {
					throw new RuntimeException("Error: Unhandled value type '" + serviceNameArg.getClass().getName()
						+ "' when attempting to resolve service names in service get methods.\n"
						+ "  ServiceNameArg: " + serviceNameArg + "\n"
						+ "  InvokeStmt: " + invokeStmt + "\n"
						+ "  Source: " + source
					);
				}
				
				//Only instances where the return value of the get service methods is used will tell us anything
				//so we only process those
				if(serviceName != null && serviceName.equals(name) && invokeStmt instanceof DefinitionStmt) {
					//By definition expressions can only be assigned to locals
					AdvLocalUses localUses = unitGraphToLocalUses.getUnchecked(methodToUnitGraph.getUnchecked(source));
					Set<UnitValueBoxPair> uses = localUses.getUsesWithAliasesRemoveLocal((DefinitionStmt)invokeStmt);
					for(UnitValueBoxPair uu : uses) {
						Unit u = uu.getUnit();
						if(u instanceof DefinitionStmt && ((DefinitionStmt)u).getRightOp() instanceof CastExpr) {
							SootClass sc = ((RefType)((CastExpr)(((DefinitionStmt)u).getRightOp())).getCastType()).getSootClass();
							if(iinterfaceSubInterfaces.contains(sc) || iinterfaceSubClasses.contains(sc))
								ret.add(sc);
						} else if(((Stmt)u).containsInvokeExpr()) {
							InvokeExpr ie = ((Stmt)u).getInvokeExpr();
							SootMethodRef ref = ie.getMethodRef();
							if(ref.name().equals("asInterface") && ref.parameterTypes().size() == 1 
									&& ref.parameterType(0).toString().equals(iBinderFullClassName)) {
								Type returnType = ref.returnType();
								if(returnType instanceof RefType) {
									SootClass sc = ((RefType)returnType).getSootClass();
									if(iinterfaceSubInterfaces.contains(sc) || iinterfaceSubClasses.contains(sc))
										ret.add(sc);
								}
							}
						}
					}
				}
			}
		}
		return ret;
	}
	
	private Set<String> resolveMethodsGivenSignatures(List<String> sigs) {
		Set<String> ret = new HashSet<>();
		for(String s : sigs) {
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
		return SortingMethods.sortSet(ret,SootSort.smStringComp);
	}
	
	private static Map<SootMethod,Set<Stmt>> getInvokeStmtsOfSigs(Set<String> methodSigs) {
		Set<SootClass> hierarchyOfServiceManager = 
				HierarchyHelpers.getAllSubClassesOfInterface(Scene.v().getSootClass("android.os.IServiceManager"));
		Map<SootMethod,Set<Stmt>> sourceToInvokeStmt = new HashMap<>();
		for(SootClass sc : Scene.v().getClasses()) {
			//To remove the onTransact method in the native portion of the IServiceManager
			//Did not just exclude the ServiceManagerNative because they seem to be trying to eliminate 
			//all manually defined aidl generated code
			if(!hierarchyOfServiceManager.contains(sc)) {
				for(SootMethod sm : sc.getMethods()) {
					if(sm.isConcrete() && !methodSigs.contains(sm.getSignature())) {
						for(Unit u : sm.retrieveActiveBody().getUnits()) {
							if(((Stmt)u).containsInvokeExpr()) {
								InvokeExpr ir = ((Stmt)u).getInvokeExpr();
								if(methodSigs.contains(ir.getMethodRef().toString())) {
									Set<Stmt> invokeStmts = sourceToInvokeStmt.get(sm);
									if(invokeStmts == null) {
										invokeStmts = new HashSet<>();
										sourceToInvokeStmt.put(sm, invokeStmts);
									}
									invokeStmts.add((Stmt)u);
								}
							}
						}
					}
				}
			}
		}
		for(SootMethod sm : sourceToInvokeStmt.keySet()) {
			sourceToInvokeStmt.put(sm, SortingMethods.sortSet(sourceToInvokeStmt.get(sm),SootSort.unitComp));
		}
		return SortingMethods.sortMapKey(sourceToInvokeStmt, SootSort.smComp);
	}
	
	private Set<SootClass> getServicesFromBinderClass(SootClass binderClass) {
		Set<SootClass> ret = new HashSet<>();
		Map<SootClass, Map<SootClass, Map<SootMethod, Set<Integer>>>> map = dataAccessor.getBinderStubsAndMethodsByInterface();
		if(map.keySet().contains(binderClass)) {//The binder class is the Interface
			Map<SootClass, Map<SootMethod, Set<Integer>>> stubsToMethods = map.get(binderClass);
			//The SootClass here is the actual Stub class in the Interface, Proxy, Stub triple and not the service
			//so we get the service classes from the methods (which are the actual entry points (i.e. methods in the
			//services).
			//Could be any of the services for the given interface so we include them all
			for(Map<SootMethod, Set<Integer>> methods : stubsToMethods.values()) {
				for(SootMethod ep : methods.keySet()) {
					//Ignore the binder methods because if we have an interface leading to stubs we should have other eps
					//that define the actual service
					if(!ep.getDeclaringClass().equals(binder))
						ret.add(ep.getDeclaringClass());
				}
			}
		} else {
			//The binder class may be the stub
			boolean found = false;
			for(Map<SootClass, Map<SootMethod, Set<Integer>>> stubsToMethods : map.values()) {
				if(stubsToMethods.containsKey(binderClass)) {
					//Only include the services for a the given stub
					for(SootMethod ep : stubsToMethods.get(binderClass).keySet()) {
						ret.add(ep.getDeclaringClass());
					}
					found = true;
				}
			}
			if(!found) {
				//The last possibility is the given class is a proxy
				Map<SootMethod,Set<SootMethod>> proxyToEp = dataAccessor.getBinderProxyMethodsToEntryPoints();
				for(SootMethod proxy : proxyToEp.keySet()) {
					if(proxy.getDeclaringClass().equals(binderClass)) {
						for(SootMethod ep : proxyToEp.get(proxy)) {
							ret.add(ep.getDeclaringClass());
						}
					}
				}
			}
		}
		
		//If we are unable to determine any services for the given binder class then just return the class
		//so we don't lose data
		if(ret.isEmpty())
			ret.add(binderClass);
		return ret;
	}
	
	private Set<SootClass> findTypesFromFieldAssignments(SootField field, Collection<SootClass> toSearch) {
		Set<SootClass> ret = new HashSet<>();
		for(SootClass sc : toSearch) {
			for(SootMethod sootMethod : sc.getMethods()) {
				if(sootMethod.isConcrete()) {
					for(Unit unit : sootMethod.retrieveActiveBody().getUnits()) {
						if(unit instanceof AssignStmt && ((AssignStmt)unit).containsFieldRef() 
								&& ((AssignStmt)unit).getLeftOp() instanceof FieldRef) {
							FieldRef v = (FieldRef)((AssignStmt)unit).getLeftOp();
							SootFieldRef ref = v.getFieldRef();
							if(ref.name().equals(field.getName()) && field.equals(resolveFieldRef(v, sootMethod, unit)))
								ret.addAll(getPossibleTypesForFieldAssignment(v,((AssignStmt)unit).getRightOp(),unit,sootMethod));
						}
					}
				}
			}
		}
		return ret;
	}
	
	private Set<SootClass> getPossibleTypesForFieldAssignment(FieldRef left, Value right, Unit u, SootMethod source) {
		if(right instanceof Local) {
			return getPossibleTypesForRefLikeTypeLocal((Local)right, u, source);
		} else {
			Type type;
			if(right instanceof ClassConstant || right instanceof MethodHandle 
					|| right instanceof MethodType || right instanceof StringConstant)
				type = right.getType();
			else if(right instanceof NumericConstant)
				type = ((PrimType)(right.getType())).boxedType();
			else //UntypedConstant and NullConstant
				type = left.getFieldRef().type();
			
			type = refLikeTypeToRefType(type);
			if(!(type instanceof RefLikeType))
				throw new RuntimeException("Error: '" + type.getClass().getName() + "' is not a RefLikeType when determining type for RefLikeType"
					+ " constant.\n"
					+ "  Value: " + right + "\n"
					+ "  Unit: " + u + "\n"
					+ "  Source: " + source
				);
			else if(type instanceof NullType)
				throw new RuntimeException("Error: Cannot determine type from a NullType RefLikeType!?!.\n"
					+ "  Value: " + right + "\n"
					+ "  Unit: " + u + "\n"
					+ "  Source: " + source
				);
			return Collections.singleton(((RefType)type).getSootClass());
		}
	}
	
	/* Given a local that is assumed to be a RefLikeType, return the classes that best fit the base type of the local
	 * that will actually declared at runtime given the information only available in the source method (i.e. this is not
	 * Interprocedural). This will return a set containing one to many classes or throw an exception. As the classes returned
	 * are the best fit for a local, it will attempt to remove from the returned set any non-concrete classes who have at least 
	 * one child class in the set as well. However, any concrete class could potentially represent the class that is actually
	 * used during runtime. Moreover, if no subclass for a non-concrete class exists then the non-concrete class will remain
	 * because it also represents a best fit result.
	 */
	private Set<SootClass> getPossibleTypesForRefLikeTypeLocal(Local l, Unit u, SootMethod source) {
		Set<SootClass> possibleTypes = new HashSet<>();
		AdvLocalDefs defsFinder = unitGraphToLocalDefs.getUnchecked(methodToUnitGraph.getUnchecked(source));
		Set<DefinitionStmt> defs = defsFinder.getDefsWithAliasesAndArraysRemoveLocalAndCastAndArrays(l, u);
		
		//Local must be a RefLikeType, otherwise it is unclear what we are processing
		if(!(l.getType() instanceof RefLikeType))
			throw new RuntimeException("Error: Attempted to process non-RefLikeType '" + l.getType().getClass().getName() + "'\n"
				+ "  Local: " + l + "\n"
				+ "  Unit: " + u + "\n"
				+ "  Source: " + source
			);
		
		//Add the type of the local just in case we cannot find anything better
		//ArrayType and AnySubType should be resolved to base types leaving only RefType and NullType
		//If NullType we simply ignore the local's type because it does not give us any extra information
		//If the returned type is not a RefLikeType this is an error
		Type localType = refLikeTypeToRefType(l.getType());
		if(!(localType instanceof RefLikeType))
			throw new RuntimeException("Error: '" + localType.getClass().getName() + "' is not a RefLikeType when determining "
				+ "type for initial local.\n"
				+ "  Local: " + l + "\n"
				+ "  Unit: " + u + "\n"
				+ "  Source: " + source
			);
		else if(localType instanceof RefType)
			possibleTypes.add(((RefType)localType).getSootClass());
		
		for(DefinitionStmt def : defs) {
			Value right = def.getRightOp();
			Type type;
			if(right instanceof ParameterRef)
				type = ((ParameterRef)right).getType();
			else if(right instanceof ThisRef)
				type = ((ThisRef)right).getType();
			else if(right instanceof CaughtExceptionRef)
				type = ((CaughtExceptionRef)right).getType();
			else if(right instanceof ArrayRef)
				type = ((ArrayRef)right).getBase().getType();
			else if(right instanceof FieldRef)
				type = ((FieldRef)right).getFieldRef().type();
			else if(right instanceof Local)
				type = ((Local)right).getType();
			else if (right instanceof InvokeExpr)
				type = ((InvokeExpr)right).getMethodRef().returnType();
			else if(right instanceof NewExpr)
				type = ((NewExpr)right).getType();
			else if(right instanceof NewArrayExpr)
				type = ((NewArrayExpr)right).getBaseType();
			else if(right instanceof NewMultiArrayExpr)
				type = ((NewMultiArrayExpr)right).getBaseType().baseType;
			else if(right instanceof ClassConstant || right instanceof MethodHandle 
					|| right instanceof MethodType || right instanceof StringConstant)
				type = right.getType();
			else if(right instanceof NumericConstant)
				type = ((PrimType)(right.getType())).boxedType();
			else if(right instanceof UntypedConstant || right instanceof NullConstant)
				type = localType;
			else 
				throw new RuntimeException("Error: Unhandled right op of '" + right.getClass().getName() 
						+ "' for definition stmt '" + def + "' of unit '" + u + "' in method '" + source + "'.");
			
			type = refLikeTypeToRefType(type);
			if(!(type instanceof RefLikeType))
				throw new RuntimeException("Error: '" + type.getClass().getName() + "' is not a RefLikeType when determining type for RefLikeType"
					+ " definition statement.\n"
					+ "  DefStmt: " + def + "\n"
					+ "  Local: " + l + "\n"
					+ "  Unit: " + u + "\n"
					+ "  Source: " + source
				);
			else if(type instanceof NullType)
				throw new RuntimeException("Error: Cannot determine type from a NullType RefLikeType!?!.\n"
					+ "  DefStmt: " + def + "\n"
					+ "  Local: " + l + "\n"
					+ "  Unit: " + u + "\n"
					+ "  Source: " + source
				);
			else
				possibleTypes.add(((RefType)type).getSootClass());
		}
		
		//Remove any non-concrete classes where at least one implementation of the concrete exists in our final set
		//because we are trying to find the best fit type being assigned to a local
		Set<SootClass> ret = removeUnwantedNonConcrete(possibleTypes);
		if(ret.isEmpty())
			throw new RuntimeException("Error: Failed to resolve even one class for the given local!?!\n"
				+ "  Local: " + l + "\n"
				+ "  Unit: " + u + "\n"
				+ "  Source: " + source
			);
		
		return ret;
	}
	
	//Remove any non-concrete classes where at least one implementation of the concrete exists in our final set
	//because we are trying to find the best fit type being assigned to a local
	private Set<SootClass> removeUnwantedNonConcrete(Set<SootClass> in) {
		List<SootClass> nonConcrete = new ArrayList<>();
		List<SootClass> concrete = new ArrayList<>();
		Set<SootClass> ret = new HashSet<>();
		for(SootClass sc : in) {
			if(sc.isConcrete())
				concrete.add(sc);
			else
				nonConcrete.add(sc);
		}
		if(nonConcrete.isEmpty()) {
			ret = in;
		} else {
			for(Iterator<SootClass> it = nonConcrete.iterator(); it.hasNext();) {
				SootClass sc = it.next();
				Set<SootClass> subClasses = classToSubClasses.getUnchecked(sc);
				for(SootClass c : concrete) {
					if(subClasses.contains(c)) {
						it.remove();
						break;
					}
				}
			}
			ret = new HashSet<>(concrete);
			ret.addAll(nonConcrete);
		}
		return ret;
	}
	
	/* Want to make sure type ends up being a RefType
	 * RefLikeTypes can be ArrayType, AnySubType, NullType, and RefType where the first two store as their absolute base
	 * should store RefTypes
	 * So keep searching until we end up with a RefType, NullType (error), or non RefLikeType (error)
	 * We are under the assumption that the type passed in is assumed to be a RefLikeType so if we ultimately end up
	 * with a PrimType then return the boxed version of that PrimType as this is a RefType
	 */
	private Type refLikeTypeToRefType(Type type) {
		while(!(type instanceof RefType || type instanceof NullType) && (type instanceof RefLikeType)) {
			type = (type instanceof ArrayType) ? ((ArrayType)type).baseType : ((AnySubType)type).getBase();
		}
		if(type instanceof PrimType)
			type = ((PrimType)type).boxedType();
		return type;
	}
	
	private SootField resolveFieldRef(FieldRef ref, SootMethod sm, Unit u) {
		if(ref instanceof StaticFieldRef) {
			return ref.getField();
		} else {
			InstanceFieldRef iref = (InstanceFieldRef)ref;
			Set<SootClass> possibleTypes = getPossibleTypesForRefLikeTypeLocal((Local)iref.getBase(), u, sm);
			SootClass finalType;
			if(possibleTypes.size() == 1) {
				finalType = possibleTypes.iterator().next();
			} else {
				possibleTypes = SortingMethods.sortSet(possibleTypes,new Comparator<SootClass>() {
					public int compare(SootClass sc1, SootClass sc2) {
						if(classToSubClasses.getUnchecked(sc1).contains(sc2)) {
							return -1;
						} else if(classToSubClasses.getUnchecked(sc2).contains(sc1)) {
							return 1;
						} else {
							throw new RuntimeException("Error: Unable to determine final type because neither '" + sc1 
									+ "' nor '" + sc2 + "' is a subclass of the other when resolving field ref '" 
									+ ref + "' in method '" + sm + "'.");
						}
					}
				});
				finalType = possibleTypes.iterator().next();
			}
			return Scene.v().makeFieldRef(finalType, ref.getFieldRef().name(), 
					ref.getFieldRef().type(), ref.getFieldRef().isStatic()).resolve();
		}
	}

}
