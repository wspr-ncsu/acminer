package org.sag.acminer.phases.bindergroups;

import java.io.ObjectStreamException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.sag.common.io.FileHash;
import org.sag.common.tools.SortingMethods;
import org.sag.common.xstream.NamedCollectionConverterWithSize;
import org.sag.common.xstream.XStreamInOut;
import org.sag.common.xstream.XStreamInOut.XStreamInOutInterface;
import org.sag.main.sootinit.SootInstanceWrapper;
import org.sag.soot.SootSort;
import org.sag.soot.xstream.SootMethodContainer;

import soot.SootClass;
import soot.SootMethod;
import soot.jimple.InvokeExpr;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/** 
 * Note this database may not contain all the stubs listed in the EntryPointsDatabase as it only includes those
 * that are subclasses of a interface that extends the IInterface. It is possible but rare for a class to just 
 * extend the Binder class and override onTransact method without implementing a subinterface of IInterface. 
 * These instances show up in the EntryPointsDatabase but not this database because they do not belong to a 
 * BinderGroup. Also note that this database may contain stubs that are not listed in the EntryPointsDatabse
 * when the stub is empty (i.e. contains no entry points). We include these in this database for completeness
 * but they are not included in the EntryPointsDatabase because they do not contain any entry points. Finally,
 * for completeness, we include the IBinder, BinderProxy, Binder group in this database for completeness even
 * though technically IBinder does not extend IInterface. None of these should contain any methods so it should
 * not be an issue.
 * 
 * @author agorski
 *
 */
@XStreamAlias("BinderGroupsDatabase")
public class BinderGroupsDatabase implements XStreamInOutInterface {
	
	//begin singleton methods
	
	@XStreamOmitField
	private static volatile BinderGroupsDatabase singleton = null;
	
	public static BinderGroupsDatabase v(){
		if(!isSet())
			throw new RuntimeException("The BinderGroupsDatabase was never initilized.");
		return singleton;
	}
	
	public static boolean isSet(){
		return singleton != null;
	}
	
	public static void setDatabase(BinderGroupsDatabase database){
		singleton = database;
	}
	
	public static void resetDatabase(){
		singleton = null;
	}
	
	/** One of two ways to create a BinderGroupsDatabase. The other is {@link #readXMLStatic(String, Path)}.
	 * This is only to be called from the class {@link GenerateBinderGroups} which generates the required data
	 * and then initializes this database. This method will override the singleton instance of BinderGroupsDatabase 
	 * with this newly created object as there should only be one instance of this class.
	 */
	protected static BinderGroupsDatabase getNewBinderGroupsDatabase(Map<SootClass,Set<SootMethod>> binderInterfacesToMethods, 
			Map<SootClass,Map<SootClass,Map<SootMethod,Set<Integer>>>> binderInterfacesToProxiesToMethods,
			Map<SootClass,Map<SootClass,Map<SootMethod,Set<Integer>>>> binderInterfacesToStubsToMethods,
			Map<SootClass,Map<SootClass,Set<Integer>>> binderInterfacesToStubsToAllTransactionIds){
		
		BinderGroupsDatabase ret = new BinderGroupsDatabase(binderInterfacesToMethods,binderInterfacesToProxiesToMethods,
				binderInterfacesToStubsToMethods,binderInterfacesToStubsToAllTransactionIds);
		setDatabase(ret);
		return ret;
	}
	
	//end singleton methods
	
	//begin instance methods
	
	@XStreamAlias("DependencyFiles")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"DependencyFile"},types={FileHash.class})
	private volatile LinkedHashSet<FileHash> dependencyFiles;
	
	@XStreamImplicit
	private volatile Set<GroupBContainer> binderGroups;
	
	@XStreamOmitField
	private volatile Map<SootClass,Set<SootMethod>> sootResolvedBinderInterfacesAndMethods;
	@XStreamOmitField
	private volatile Map<String,Set<String>> binderInterfacesAndMethods;
	
	@XStreamOmitField
	private volatile Map<SootClass,Map<SootClass,Map<SootMethod,Set<Integer>>>> sootResolvedBinderProxiesAndMethodsByInterface;
	@XStreamOmitField
	private volatile Map<String,Map<String,Map<String,Set<Integer>>>> binderProxiesAndMethodsByInterface;
	
	@XStreamOmitField
	private volatile Map<SootClass,Map<SootClass,Map<SootMethod,Set<Integer>>>> sootResolvedBinderStubsAndMethodsByInterface;
	@XStreamOmitField
	private volatile Map<String,Map<String,Map<String,Set<Integer>>>> binderStubsAndMethodsByInterface;
	
	@XStreamOmitField
	private volatile Map<SootClass,Map<SootMethod,Set<Integer>>> sootResolvedBinderProxiesAndMethods;
	@XStreamOmitField
	private volatile Map<String,Map<String,Set<Integer>>> binderProxiesAndMethods;
	
	@XStreamOmitField
	private volatile Map<SootClass,Map<SootMethod,Set<Integer>>> sootResolvedBinderStubsAndMethods;
	@XStreamOmitField
	private volatile Map<String,Map<String,Set<Integer>>> binderStubsAndMethods;
	
	@XStreamOmitField
	private volatile Map<SootMethod,Set<SootMethod>> sootResolvedBinderProxyMethodsToEntryPoints;
	
	@XStreamOmitField
	private volatile Map<SootClass,Map<SootMethod,Set<SootMethod>>> sootResolvedBinderProxyMethodsToEntryPointsByInterface;
	
	@XStreamOmitField
	private volatile Map<SootMethod,Set<SootMethod>> sootResolvedBinderInterfaceMethodsToProxyMethods;
	
	@XStreamOmitField
	private volatile Map<SootClass,Map<SootMethod,Set<SootMethod>>> sootResolvedBinderInterfaceMethodsToProxyMethodsByInterface;
	
	@XStreamOmitField
	private volatile Map<SootMethod,Set<SootMethod>> sootResolvedBinderInterfaceMethodsToEntryPoints;
	
	@XStreamOmitField
	private volatile Map<SootClass,Map<SootMethod,Set<SootMethod>>> sootResolvedBinderInterfaceMethodsToEntryPointsByInterface;
	
	@XStreamOmitField
	private volatile Map<String,SootMethod> signaturesToInterfaceMethods;
	
	@XStreamOmitField
	private volatile Map<String,SootMethod> signaturesToProxyMethods;
	
	@XStreamOmitField
	private volatile Map<String,Set<SootMethod>> sootResolvedBinderStubMethodsToEntryPoints;
	
	@XStreamOmitField
	private volatile Map<SootClass,Map<String,Set<SootMethod>>> sootResolvedBinderStubMethodsToEntryPointsByInterface;
	
	@XStreamOmitField
	private volatile Set<String> allBinderGroupClasses;
	
	//for use with xstream on read in only
	private BinderGroupsDatabase(){}
	
	private BinderGroupsDatabase(Map<SootClass,Set<SootMethod>> binderInterfacesToMethods, 
			Map<SootClass,Map<SootClass,Map<SootMethod,Set<Integer>>>> binderInterfacesToProxiesToMethods,
			Map<SootClass,Map<SootClass,Map<SootMethod,Set<Integer>>>> binderInterfacesToStubsToMethods,
			Map<SootClass,Map<SootClass,Set<Integer>>> binderInterfacesToStubsToAllTransactionIds){
		
		if(binderInterfacesToMethods == null || binderInterfacesToProxiesToMethods == null || binderInterfacesToStubsToMethods == null || 
				binderInterfacesToStubsToAllTransactionIds == null)
			throw new IllegalArgumentException();
		binderGroups = new LinkedHashSet<>();
		for(SootClass iisc : binderInterfacesToMethods.keySet()){
			binderGroups.add(new GroupBContainer(iisc, binderInterfacesToMethods.get(iisc),binderInterfacesToProxiesToMethods.get(iisc),
					binderInterfacesToStubsToMethods.get(iisc),binderInterfacesToStubsToAllTransactionIds.get(iisc)));
		}
		this.binderGroups = SortingMethods.sortSet(this.binderGroups);
		initNonSootData();
	}
	
	//ReadResolve is always run when reading from XML even if a constructor is run first
	private Object readResolve() throws ObjectStreamException {
		initNonSootData();
		return this;
	}
	
	private Object writeReplace() throws ObjectStreamException {
		return this;
	}
	
	public boolean equals(Object o){
		if(this == o){
			return true;
		}
		if(o == null || !(o instanceof BinderGroupsDatabase)){
			return false;
		}
		BinderGroupsDatabase other = (BinderGroupsDatabase) o;
		return Objects.equals(dependencyFiles, other.dependencyFiles) && Objects.equals(binderGroups, other.binderGroups);
	}
	
	public int hashCode(){
		int hash = 17;
		hash = 31 * hash + Objects.hashCode(binderGroups);
		hash = 31 * hash + Objects.hashCode(dependencyFiles);
		return hash;
	}
	
	public void addDependencyFile(FileHash dependencyFile){
		if(this.dependencyFiles == null)
			this.dependencyFiles = new LinkedHashSet<>();
		this.dependencyFiles.add(dependencyFile);
	}
	
	public Set<FileHash> getDependencyFiles(){
		return dependencyFiles;
	}
	
	public void initNonSootData(){
		binderInterfacesAndMethods = new HashMap<>();
		binderProxiesAndMethodsByInterface = new HashMap<>();
		binderStubsAndMethodsByInterface = new HashMap<>();
		binderProxiesAndMethods = new HashMap<>();
		binderStubsAndMethods = new HashMap<>();
		allBinderGroupClasses = new HashSet<>();
		for(GroupBContainer binderGroup : this.binderGroups){
			InterfaceBContainer binderInterface = binderGroup.getBinderInterface();
			Set<String> interfaceMethods = new HashSet<>();
			for(SootMethodContainer interfaceMethod : binderInterface.getInterfaceMethods()){
				interfaceMethods.add(interfaceMethod.getSignature());
			}
			binderInterfacesAndMethods.put(binderInterface.getBinderInterface().getSignature(), ImmutableSet.copyOf(
					SortingMethods.sortSet(interfaceMethods,SootSort.smStringComp)));
			allBinderGroupClasses.add(binderInterface.getBinderInterface().getSignature());
			
			Map<String,Map<String,Set<Integer>>> binderProxies = new HashMap<>();
			for(ProxyBContainer binderProxy : binderGroup.getBinderProxies()){
				Map<String,Set<Integer>> proxyMethods = new HashMap<>();
				for(ProxyMethodBContainer proxyMethod : binderProxy.getProxyMethods()){
					proxyMethods.put(proxyMethod.getProxyMethod().getSignature(), ImmutableSet.copyOf(
							SortingMethods.sortSet(proxyMethod.getTransactionIds(),SortingMethods.iComp)));
				}
				binderProxies.put(binderProxy.getBinderProxy().getSignature(), ImmutableMap.copyOf(
						SortingMethods.sortMapKey(proxyMethods,SootSort.smStringComp)));
				allBinderGroupClasses.add(binderProxy.getBinderProxy().getSignature());
			}
			binderProxiesAndMethodsByInterface.put(binderInterface.getBinderInterface().getSignature(), ImmutableMap.copyOf(
					SortingMethods.sortMapKey(binderProxies, SortingMethods.sComp)));
			binderProxiesAndMethods.putAll(binderProxies);
			
			Map<String,Map<String,Set<Integer>>> binderStubs = new HashMap<>();
			for(StubBContainer binderStub : binderGroup.getBinderStubs()){
				Map<String,Set<Integer>> stubMethods = new HashMap<>();
				for(StubMethodBContainer stubMethod : binderStub.getStubMethods()){
					stubMethods.put(stubMethod.getStubMethod().getSignature(), ImmutableSet.copyOf(
							SortingMethods.sortSet(stubMethod.getTransactionIds(),SortingMethods.iComp)));
				}
				binderStubs.put(binderStub.getBinderStub().getSignature(), ImmutableMap.copyOf(
						SortingMethods.sortMapKey(stubMethods,SootSort.smStringComp)));
				allBinderGroupClasses.add(binderStub.getBinderStub().getSignature());
			}
			binderStubsAndMethodsByInterface.put(binderInterface.getBinderInterface().getSignature(), ImmutableMap.copyOf(
					SortingMethods.sortMapKey(binderStubs, SortingMethods.sComp)));
			binderStubsAndMethods.putAll(binderStubs);
		}
		binderInterfacesAndMethods = ImmutableMap.copyOf(SortingMethods.sortMapKey(binderInterfacesAndMethods, SortingMethods.sComp));
		binderProxiesAndMethodsByInterface = ImmutableMap.copyOf(
				SortingMethods.sortMapKey(binderProxiesAndMethodsByInterface,SortingMethods.sComp));
		binderStubsAndMethodsByInterface = ImmutableMap.copyOf(
				SortingMethods.sortMapKey(binderStubsAndMethodsByInterface,SortingMethods.sComp));
		binderProxiesAndMethods = ImmutableMap.copyOf(SortingMethods.sortMapKey(binderProxiesAndMethods, SortingMethods.sComp));
		binderStubsAndMethods = ImmutableMap.copyOf(SortingMethods.sortMapKey(binderStubsAndMethods, SortingMethods.sComp));
		allBinderGroupClasses = ImmutableSet.copyOf(SortingMethods.sortSet(allBinderGroupClasses, SortingMethods.sComp));
	}
	
	public Set<String> getAllBinderGroupClasses() {
		return allBinderGroupClasses;
	}
	
	public Map<String, Set<String>> getBinderInterfacesAndMethods() {
		return binderInterfacesAndMethods;
	}

	public Map<String, Map<String, Map<String, Set<Integer>>>> getBinderProxiesAndMethodsByInterface() {
		return binderProxiesAndMethodsByInterface;
	}

	public Map<String, Map<String, Map<String, Set<Integer>>>> getBinderStubsAndMethodsByInterface() {
		return binderStubsAndMethodsByInterface;
	}

	public Map<String, Map<String, Set<Integer>>> getBinderProxiesAndMethods() {
		return binderProxiesAndMethods;
	}

	public Map<String, Map<String, Set<Integer>>> getBinderStubsAndMethods() {
		return binderStubsAndMethods;
	}

	public void resetSootResolvedData(){
		sootResolvedBinderInterfacesAndMethods = null;
		sootResolvedBinderProxiesAndMethodsByInterface = null;
		sootResolvedBinderStubsAndMethodsByInterface = null;
		sootResolvedBinderProxiesAndMethods = null;
		sootResolvedBinderStubsAndMethods = null;
		sootResolvedBinderProxyMethodsToEntryPoints = null;
		sootResolvedBinderProxyMethodsToEntryPointsByInterface = null;
		sootResolvedBinderInterfaceMethodsToProxyMethods = null;
		sootResolvedBinderInterfaceMethodsToProxyMethodsByInterface = null;
		sootResolvedBinderInterfaceMethodsToEntryPoints = null;
		sootResolvedBinderInterfaceMethodsToEntryPointsByInterface = null;
		signaturesToInterfaceMethods = null;
		signaturesToProxyMethods = null;
		sootResolvedBinderStubMethodsToEntryPoints = null;
		sootResolvedBinderStubMethodsToEntryPointsByInterface = null;
	}
	
	private void checkSootInit(){
		if(!SootInstanceWrapper.v().isSootInitSet())
			throw new RuntimeException("Error: Some instance of Soot must be initilized first.");
	}
	
	public Map<SootClass,Set<SootMethod>> getSootResolvedBinderInterfacesAndMethods(){
		if(sootResolvedBinderInterfacesAndMethods == null){
			checkSootInit();
			sootResolvedBinderInterfacesAndMethods = new HashMap<>();
			for(GroupBContainer binderGroup : this.binderGroups){
				InterfaceBContainer binderInterface = binderGroup.getBinderInterface();
				Set<SootMethod> temp = new HashSet<>();
				for(SootMethodContainer smCont : binderInterface.getInterfaceMethods()){
					temp.add(smCont.toSootMethod());
				}
				temp = ImmutableSet.copyOf(SortingMethods.sortSet(temp,SootSort.smComp));
				sootResolvedBinderInterfacesAndMethods.put(binderInterface.getBinderInterface().toSootClass(), temp);
			}
			sootResolvedBinderInterfacesAndMethods = ImmutableMap.copyOf(SortingMethods.sortMapKey(sootResolvedBinderInterfacesAndMethods, 
					SootSort.scComp));
		}
		return sootResolvedBinderInterfacesAndMethods;
	}
	
	public Map<SootClass,Map<SootClass,Map<SootMethod,Set<Integer>>>> getSootResolvedBinderProxiesAndMethodsByInterface(){
		if(sootResolvedBinderProxiesAndMethodsByInterface == null){
			checkSootInit();
			sootResolvedBinderProxiesAndMethodsByInterface = new HashMap<>();
			for(GroupBContainer binderGroup : this.binderGroups){
				Map<SootClass,Map<SootMethod,Set<Integer>>> temp2 = new HashMap<>();
				for(ProxyBContainer binderProxy : binderGroup.getBinderProxies()){
					Map<SootMethod,Set<Integer>> temp = new HashMap<>();
					for(ProxyMethodBContainer proxyMethod : binderProxy.getProxyMethods()){
						temp.put(proxyMethod.getProxyMethod().toSootMethod(),ImmutableSet.copyOf(SortingMethods.sortSet(
								proxyMethod.getTransactionIds(),SortingMethods.iComp)));
					}
					temp2.put(binderProxy.getBinderProxy().toSootClass(), ImmutableMap.copyOf(SortingMethods.sortMapKey(
							temp, SootSort.smComp)));
				}
				sootResolvedBinderProxiesAndMethodsByInterface.put(binderGroup.getBinderInterface().getBinderInterface().toSootClass(), 
						ImmutableMap.copyOf(SortingMethods.sortMapKey(temp2, SootSort.scComp)));
			}
			sootResolvedBinderProxiesAndMethodsByInterface = ImmutableMap.copyOf(SortingMethods.sortMapKey(
					sootResolvedBinderProxiesAndMethodsByInterface,SootSort.scComp));
		}
		return sootResolvedBinderProxiesAndMethodsByInterface;
	}
	
	public Map<SootClass,Map<SootClass,Map<SootMethod,Set<Integer>>>> getSootResolvedBinderStubsAndMethodsByInterface(){
		if(sootResolvedBinderStubsAndMethodsByInterface == null){
			checkSootInit();
			sootResolvedBinderStubsAndMethodsByInterface = new HashMap<>();
			for(GroupBContainer binderGroup : this.binderGroups){
				Map<SootClass,Map<SootMethod,Set<Integer>>> temp2 = new HashMap<>();
				for(StubBContainer binderStub : binderGroup.getBinderStubs()){
					Map<SootMethod,Set<Integer>> temp = new HashMap<>();
					for(StubMethodBContainer stubMethod : binderStub.getStubMethods()){
						temp.put(stubMethod.getStubMethod().toSootMethod(),ImmutableSet.copyOf(SortingMethods.sortSet(
								stubMethod.getTransactionIds(),SortingMethods.iComp)));
					}
					temp2.put(binderStub.getBinderStub().toSootClass(), ImmutableMap.copyOf(SortingMethods.sortMapKey(
							temp, SootSort.smComp)));
				}
				sootResolvedBinderStubsAndMethodsByInterface.put(binderGroup.getBinderInterface().getBinderInterface().toSootClass(), 
						ImmutableMap.copyOf(SortingMethods.sortMapKey(temp2, SootSort.scComp)));
			}
			sootResolvedBinderStubsAndMethodsByInterface = ImmutableMap.copyOf(SortingMethods.sortMapKey(
					sootResolvedBinderStubsAndMethodsByInterface,SootSort.scComp));
		}
		return sootResolvedBinderStubsAndMethodsByInterface;
	}
	
	public Map<SootClass,Map<SootMethod,Set<Integer>>> getSootResolvedBinderProxiesAndMethods(){
		if(sootResolvedBinderProxiesAndMethods == null){
			checkSootInit();
			sootResolvedBinderProxiesAndMethods = new HashMap<>();
			for(GroupBContainer binderGroup : this.binderGroups){
				//Each proxy is unique even without the interface because they are all required to implement the unique interface
				for(ProxyBContainer binderProxy : binderGroup.getBinderProxies()){
					Map<SootMethod,Set<Integer>> temp = new HashMap<>();
					for(ProxyMethodBContainer proxyMethod : binderProxy.getProxyMethods()){
						temp.put(proxyMethod.getProxyMethod().toSootMethod(),ImmutableSet.copyOf(SortingMethods.sortSet(
								proxyMethod.getTransactionIds(),SortingMethods.iComp)));
					}
					sootResolvedBinderProxiesAndMethods.put(binderProxy.getBinderProxy().toSootClass(), ImmutableMap.copyOf(
							SortingMethods.sortMapKey(temp, SootSort.smComp)));
				}
			}
			sootResolvedBinderProxiesAndMethods = ImmutableMap.copyOf(SortingMethods.sortMapKey(
					sootResolvedBinderProxiesAndMethods,SootSort.scComp));
		}
		return sootResolvedBinderProxiesAndMethods;
	}
	
	//Stub -> Map(Entry point -> Set(Transaction ids))
	public Map<SootClass,Map<SootMethod,Set<Integer>>> getSootResolvedBinderStubsAndMethods(){
		if(sootResolvedBinderStubsAndMethods == null){
			checkSootInit();
			sootResolvedBinderStubsAndMethods = new HashMap<>();
			for(GroupBContainer binderGroup : this.binderGroups){
				//Each stub is unique even without the interface because they are all required to implement the unique interface
				for(StubBContainer binderStub : binderGroup.getBinderStubs()){
					Map<SootMethod,Set<Integer>> temp = new HashMap<>();
					for(StubMethodBContainer stubMethod : binderStub.getStubMethods()){
						temp.put(stubMethod.getStubMethod().toSootMethod(),ImmutableSet.copyOf(SortingMethods.sortSet(
								stubMethod.getTransactionIds(),SortingMethods.iComp)));
					}
					sootResolvedBinderStubsAndMethods.put(binderStub.getBinderStub().toSootClass(), ImmutableMap.copyOf(
							SortingMethods.sortMapKey(temp, SootSort.smComp)));
				}
			}
			sootResolvedBinderStubsAndMethods = ImmutableMap.copyOf(SortingMethods.sortMapKey(
					sootResolvedBinderStubsAndMethods,SootSort.scComp));
		}
		return sootResolvedBinderStubsAndMethods;
	}
	
	/* Each proxy class is unique because it must implement a unique interface. Each proxy method comes directly
	 * from a unique proxy class so the methods themselves are unique. Each stub class is unique because it must
	 * implement a unique interface. All stub methods come from sub classes of a unique Stub class which means 
	 * that each Stub method is unique. Even if they were not, a proxy method will always have the same 
	 * transaction ids as they come directly from the method itself and you cannot get duplicate stub methods
	 * without going through the same stub class which always produces the same transaction ids.
	 */
	public Map<SootMethod,Set<SootMethod>> getSootResolvedBinderProxyMethodsToEntryPoints(){
		if(sootResolvedBinderProxyMethodsToEntryPoints == null){
			checkSootInit();
			sootResolvedBinderProxyMethodsToEntryPoints = new HashMap<>();
			for(GroupBContainer binderGroup : this.binderGroups){
				sootResolvedBinderProxyMethodsToEntryPoints.putAll(getProxyMethodsToEntryPointsForGroup(binderGroup));
			}
			sootResolvedBinderProxyMethodsToEntryPoints = ImmutableMap.copyOf(SortingMethods.sortMapKey(
					sootResolvedBinderProxyMethodsToEntryPoints,SootSort.smComp));
		}
		return sootResolvedBinderProxyMethodsToEntryPoints;
	}
	
	/** Returns null if proxyMethod is not an proxy method, an empty set if the proxy method
	 * resolves to no entry points, and the set of entry points the proxy method resolved to otherwise.
	 * Note the set returned is an instance of an ImmutableSet and cannot be modified.
	 */
	public Set<SootMethod> getSootResolvedEntryPointsForBinderProxyMethod(SootMethod proxyMethod){
		return getSootResolvedBinderProxyMethodsToEntryPoints().get(proxyMethod);
	}
	
	public Map<SootClass,Map<SootMethod,Set<SootMethod>>> getSootResolvedBinderProxyMethodsToEntryPointsByInterface(){
		if(sootResolvedBinderProxyMethodsToEntryPointsByInterface == null){
			checkSootInit();
			sootResolvedBinderProxyMethodsToEntryPointsByInterface = new HashMap<>();
			for(GroupBContainer binderGroup : this.binderGroups){
				sootResolvedBinderProxyMethodsToEntryPointsByInterface.put(binderGroup.getBinderInterface().getBinderInterface().toSootClass(), 
						getProxyMethodsToEntryPointsForGroup(binderGroup));
			}
			sootResolvedBinderProxyMethodsToEntryPointsByInterface = ImmutableMap.copyOf(
					SortingMethods.sortMapKey(sootResolvedBinderProxyMethodsToEntryPointsByInterface,SootSort.scComp));
		}
		return sootResolvedBinderProxyMethodsToEntryPointsByInterface;
	}
	
	public Map<SootMethod,Set<SootMethod>> getSootResolvedBinderProxyMethodsToEntryPointsForInterface(SootClass binderInterface){
		return getSootResolvedBinderProxyMethodsToEntryPointsByInterface().get(binderInterface);
	}
	
	private Map<SootMethod,Set<SootMethod>> getProxyMethodsToEntryPointsForGroup(GroupBContainer binderGroup){
		Map<SootMethod,Set<SootMethod>> ret = new HashMap<>();
		Map<SootClass, Map<Integer,Set<SootMethod>>> idsToStubMethods = new HashMap<>();
		Map<SootClass,Set<Integer>> allPossibleTransactionIds = new HashMap<>();
		//Get ids to stub methods map for group
		for(StubBContainer binderStub : binderGroup.getBinderStubs()){
			SootClass stub = binderStub.getBinderStub().toSootClass();
			allPossibleTransactionIds.put(stub, binderStub.getAllTransactionIds());
			HashMap<Integer,Set<SootMethod>> temp = new HashMap<>();
			for(StubMethodBContainer stubMethod : binderStub.getStubMethods()){
				SootMethod sm = stubMethod.getStubMethod().toSootMethod();
				for(Integer i : stubMethod.getTransactionIds()){
					Set<SootMethod> temp2 = temp.get(i);
					if(temp2 == null || temp2.isEmpty()){
						temp2 = new HashSet<>();
						temp.put(i, temp2);
					}
					temp2.add(sm);
				}
			}
			idsToStubMethods.put(stub, temp);
		}
		//Match each proxy id to a stub id and construct map
		for(ProxyBContainer binderProxy : binderGroup.getBinderProxies()){
			for(ProxyMethodBContainer proxyMethod : binderProxy.getProxyMethods()){
				Set<SootMethod> temp = new HashSet<>();
				for(Integer i : proxyMethod.getTransactionIds()){
					for(SootClass stub : idsToStubMethods.keySet()){
						Map<Integer,Set<SootMethod>> idsToSM = idsToStubMethods.get(stub);
						if(idsToSM.containsKey(i)){//If there exists an id with entry points in the stub just add them
							temp.addAll(idsToSM.get(i));
						}else if(!allPossibleTransactionIds.get(stub).contains(i)){//No stub id with entry points matches that for proxy and the 
							//id is not within our set of possible ids for the stub so default will be triggered
							if(idsToSM.containsKey(null))//Check if default has entry points and if so add them
								temp.addAll(idsToSM.get(null));
						}
						//Otherwise nothing added because there are no entry points
					}
				}
				ret.put(proxyMethod.getProxyMethod().toSootMethod(), ImmutableSet.copyOf(SortingMethods.sortSet(temp,SootSort.smComp)));
			}
		}
		return ImmutableMap.copyOf(SortingMethods.sortMapKey(ret,SootSort.smComp));
	}
	
	public Map<SootMethod,Set<SootMethod>> getSootResolvedBinderInterfaceMethodsToProxyMethods(){
		if(sootResolvedBinderInterfaceMethodsToProxyMethods == null){
			checkSootInit();
			sootResolvedBinderInterfaceMethodsToProxyMethods = new HashMap<>();
			for(GroupBContainer binderGroup : this.binderGroups){
				sootResolvedBinderInterfaceMethodsToProxyMethods.putAll(getInterfaceMethodsToProxyMethodsForGroup(binderGroup));
			}
			sootResolvedBinderInterfaceMethodsToProxyMethods = ImmutableMap.copyOf(SortingMethods.sortMapKey(
					sootResolvedBinderInterfaceMethodsToProxyMethods,SootSort.smComp));
		}
		return sootResolvedBinderInterfaceMethodsToProxyMethods;
	}
	
	/** Returns null if interfaceMethod is not an interface method, an empty set if the interface method
	 * resolves to no proxy methods, and the set of proxy methods the interface method resolved to otherwise.
	 * Note the set returned is an instance of an ImmutableSet and cannot be modified.
	 */
	public Set<SootMethod> getSootResolvedBinderProxyMethodsForInterfaceMethod(SootMethod interfaceMethod){
		return getSootResolvedBinderInterfaceMethodsToProxyMethods().get(interfaceMethod);
	}
	
	public Map<SootClass,Map<SootMethod,Set<SootMethod>>> getSootResolvedBinderInterfaceMethodsToProxyMethodsByInterface(){
		if(sootResolvedBinderInterfaceMethodsToProxyMethodsByInterface == null){
			checkSootInit();
			sootResolvedBinderInterfaceMethodsToProxyMethodsByInterface = new HashMap<>();
			for(GroupBContainer binderGroup : this.binderGroups){
				sootResolvedBinderInterfaceMethodsToProxyMethodsByInterface.put(
						binderGroup.getBinderInterface().getBinderInterface().toSootClass(), 
						getInterfaceMethodsToProxyMethodsForGroup(binderGroup));
			}
			sootResolvedBinderInterfaceMethodsToProxyMethodsByInterface = ImmutableMap.copyOf(
					SortingMethods.sortMapKey(sootResolvedBinderInterfaceMethodsToProxyMethodsByInterface,SootSort.scComp));
		}
		return sootResolvedBinderInterfaceMethodsToProxyMethodsByInterface;
	}
	
	public Map<SootMethod,Set<SootMethod>> getSootResolvedBinderInterfaceMethodsToProxyMethodsForInterface(SootClass binderInterface){
		return getSootResolvedBinderInterfaceMethodsToProxyMethodsByInterface().get(binderInterface);
	}
	
	private Map<SootMethod,Set<SootMethod>> getInterfaceMethodsToProxyMethodsForGroup(GroupBContainer binderGroup){
		Map<SootMethod,Set<SootMethod>> ret = new HashMap<>();
		Set<SootMethod> proxyMethods = new HashSet<>();
		//Convert proxy methods to soot methods, class does not matter
		for(ProxyBContainer binderProxy : binderGroup.getBinderProxies()){
			for(ProxyMethodBContainer proxyMethod : binderProxy.getProxyMethods()){
				proxyMethods.add(proxyMethod.getProxyMethod().toSootMethod());
			}
		}
		//Each proxy method that matches the sub signature of the interface method is a target for the interface method
		for(SootMethodContainer smCont : binderGroup.getBinderInterface().getInterfaceMethods()){
			SootMethod sm = smCont.toSootMethod();
			String subSigOfI = sm.getSubSignature();
			Set<SootMethod> temp = new HashSet<>();
			for(SootMethod proxyMethod : proxyMethods){
				if(subSigOfI.equals(proxyMethod.getSubSignature()))
					temp.add(proxyMethod);
			}
			ret.put(sm, ImmutableSet.copyOf(SortingMethods.sortSet(temp,SootSort.smComp)));
		}
		return ImmutableMap.copyOf(SortingMethods.sortMapKey(ret,SootSort.smComp));
	}
	
	public Map<SootMethod,Set<SootMethod>> getSootResolvedBinderInterfaceMethodsToEntryPoints(){
		if(sootResolvedBinderInterfaceMethodsToEntryPoints == null){
			checkSootInit();
			sootResolvedBinderInterfaceMethodsToEntryPoints = new HashMap<>();
			for(GroupBContainer binderGroup : this.binderGroups){
				sootResolvedBinderInterfaceMethodsToEntryPoints.putAll(getInterfaceMethodsToEntryPointsForGroup(binderGroup));
			}
			sootResolvedBinderInterfaceMethodsToEntryPoints = ImmutableMap.copyOf(SortingMethods.sortMapKey(
					sootResolvedBinderInterfaceMethodsToEntryPoints,SootSort.smComp));
		}
		return sootResolvedBinderInterfaceMethodsToEntryPoints;
	}
	
	/** Returns null if interfaceMethod is not an interface method, an empty set if the interface method
	 * resolves to no entry points, and the set of entry points the interface method resolved to otherwise.
	 * Note the set returned is an instance of an ImmutableSet and cannot be modified.
	 */
	public Set<SootMethod> getSootResolvedEntryPointsForBinderInterfaceMethod(SootMethod interfaceMethod){
		return getSootResolvedBinderInterfaceMethodsToEntryPoints().get(interfaceMethod);
	}
	
	public Map<SootClass,Map<SootMethod,Set<SootMethod>>> getSootResolvedBinderInterfaceMethodsToEntryPointsByInterface(){
		if(sootResolvedBinderInterfaceMethodsToEntryPointsByInterface == null){
			checkSootInit();
			sootResolvedBinderInterfaceMethodsToEntryPointsByInterface = new HashMap<>();
			for(GroupBContainer binderGroup : this.binderGroups){
				sootResolvedBinderInterfaceMethodsToEntryPointsByInterface.put(
						binderGroup.getBinderInterface().getBinderInterface().toSootClass(), 
						getInterfaceMethodsToEntryPointsForGroup(binderGroup));
			}
			sootResolvedBinderInterfaceMethodsToEntryPointsByInterface = ImmutableMap.copyOf(
					SortingMethods.sortMapKey(sootResolvedBinderInterfaceMethodsToEntryPointsByInterface,SootSort.scComp));
		}
		return sootResolvedBinderInterfaceMethodsToEntryPointsByInterface;
	}
	
	public Map<SootMethod,Set<SootMethod>> getSootResolvedBinderInterfaceMethodsToEntryPointsForInterface(SootClass binderInterface){
		return getSootResolvedBinderInterfaceMethodsToEntryPointsByInterface().get(binderInterface);
	}
	
	private Map<SootMethod,Set<SootMethod>> getInterfaceMethodsToEntryPointsForGroup(GroupBContainer binderGroup){
		Map<SootMethod,Set<SootMethod>> ret = new HashMap<>();
		Map<SootMethod,Set<SootMethod>> interfaceToProxy = getInterfaceMethodsToProxyMethodsForGroup(binderGroup);
		Map<SootMethod,Set<SootMethod>> proxyToStub = getProxyMethodsToEntryPointsForGroup(binderGroup);
		for(SootMethod interfaceMethod : interfaceToProxy.keySet()){
			Set<SootMethod> temp = new HashSet<>();
			for(SootMethod proxyMethod : interfaceToProxy.get(interfaceMethod)){
				if(proxyToStub.containsKey(proxyMethod))
					temp.addAll(proxyToStub.get(proxyMethod));
			}
			ret.put(interfaceMethod, ImmutableSet.copyOf(SortingMethods.sortSet(temp,SootSort.smComp)));
		}
		return ImmutableMap.copyOf(SortingMethods.sortMapKey(ret, SootSort.smComp));
	}
	
	public Map<String,Set<SootMethod>> getSootResolvedBinderStubMethodsToEntryPoints(){
		if(sootResolvedBinderStubMethodsToEntryPoints == null){
			checkSootInit();
			sootResolvedBinderStubMethodsToEntryPoints = new HashMap<>();
			for(GroupBContainer binderGroup : this.binderGroups){
				sootResolvedBinderStubMethodsToEntryPoints.putAll(getStubMethodsToEntryPointsForGroup(binderGroup));
			}
			sootResolvedBinderStubMethodsToEntryPoints = ImmutableMap.copyOf(SortingMethods.sortMapKey(
					sootResolvedBinderStubMethodsToEntryPoints,SootSort.smStringComp));
		}
		return sootResolvedBinderStubMethodsToEntryPoints;
	}
	
	/** Returns null if sig does not represent a stub method, an empty set if the stub method represented by sig
	 * resolves to no entry points, and the set of entry points the stub method resolved to otherwise.
	 * Note the set returned is an instance of an ImmutableSet and cannot be modified.
	 */
	public Set<SootMethod> getSootResolvedEntryPointsForBinderStubMethod(String sig){
		return getSootResolvedBinderStubMethodsToEntryPoints().get(sig);
	}
	
	public Map<SootClass,Map<String,Set<SootMethod>>> getSootResolvedBinderStubMethodsToEntryPointsByInterface(){
		if(sootResolvedBinderStubMethodsToEntryPointsByInterface == null){
			checkSootInit();
			sootResolvedBinderStubMethodsToEntryPointsByInterface = new HashMap<>();
			for(GroupBContainer binderGroup : this.binderGroups){
				sootResolvedBinderStubMethodsToEntryPointsByInterface.put(
						binderGroup.getBinderInterface().getBinderInterface().toSootClass(), 
						getStubMethodsToEntryPointsForGroup(binderGroup));
			}
			sootResolvedBinderStubMethodsToEntryPointsByInterface = ImmutableMap.copyOf(
					SortingMethods.sortMapKey(sootResolvedBinderStubMethodsToEntryPointsByInterface,SootSort.scComp));
		}
		return sootResolvedBinderStubMethodsToEntryPointsByInterface;
	}
	
	public Map<String,Set<SootMethod>> getSootResolvedBinderStubMethodsToEntryPointsForInterface(SootClass binderInterface){
		return getSootResolvedBinderStubMethodsToEntryPointsByInterface().get(binderInterface);
	}
	
	private Map<String,Set<SootMethod>> getStubMethodsToEntryPointsForGroup(GroupBContainer binderGroup){
		Map<String,Set<SootMethod>> ret = new HashMap<>();
		for(StubBContainer binderStub : binderGroup.getBinderStubs()){
			SootClass stub = binderStub.getBinderStub().toSootClass();
			/* Extract method sub signature from each entry point and append it to the stub class name to create a
			 * signature for a stub method (as all entry points methods in the subclasses of a stub class are 
			 * called on the current object from the stub class). We store these as strings instead of SootMethods
			 * because the methods are almost all defined as abstract and thus do not actually exist in the stub 
			 * classes (i.e. a SootMethod for these signatures does not technically exist even if it is referenced
			 * in invoke expressions).
			 */
			for(StubMethodBContainer entryPoint : binderStub.getStubMethods()){
				SootMethod ep = entryPoint.getStubMethod().toSootMethod();
				String sig = SootMethod.getSignature(stub, ep.getName(), ep.getParameterTypes(), ep.getReturnType());
				Set<SootMethod> temp = ret.get(sig);
				if(temp == null || temp.isEmpty()){
					temp = new HashSet<>();
					ret.put(sig, temp);
				}
				temp.add(ep);
			}
			/* Include those methods defined in the interface of a stub but who do not resolve to any entry points
			 * as these are still stub methods (but are not covered in the above stub method signature assembly because
			 * no resolvable entry point existed for these methods). Note we are still missing the stub methods not 
			 * defined in the interface with no resolvable entry points. However, as these methods can only be found
			 * in manually written stub classes (i.e. those not generated from a aidl file), have not resolvable
			 * entry points, and can only be called outside of the normal binder group (i.e. not through the interface
			 * type) we should not need to worry about then. Thus they can be ignored.
			 */
			for(SootMethodContainer interfaceMethod : binderGroup.getBinderInterface().getInterfaceMethods()){
				SootMethod sm = interfaceMethod.toSootMethod();
				String sig = SootMethod.getSignature(stub, sm.getName(), sm.getParameterTypes(), sm.getReturnType());
				if(!ret.containsKey(sig)){
					ret.put(sig, Collections.<SootMethod>emptySet());
				}
			}
		}
		
		for(String s : ret.keySet()){
			ret.put(s, ImmutableSet.copyOf(SortingMethods.sortSet(ret.get(s),SootSort.smComp)));
		}
		return ImmutableMap.copyOf(SortingMethods.sortMapKey(ret,SootSort.smStringComp));
	}
	
	private SootMethod getSootMethodOfInterfaceMethodSignature(String sig){
		if(signaturesToInterfaceMethods == null){
			signaturesToInterfaceMethods = new HashMap<>();
			Map<SootMethod,Set<SootMethod>> interfaceToStub = getSootResolvedBinderInterfaceMethodsToEntryPoints();
			for(SootMethod sm : interfaceToStub.keySet()){
				signaturesToInterfaceMethods.put(sm.getSignature(), sm);
			}
		}
		return signaturesToInterfaceMethods.get(sig);
	}
	
	private SootMethod getSootMethodOfProxyMethodSignature(String sig){
		if(signaturesToProxyMethods == null){
			signaturesToProxyMethods = new HashMap<>();
			Map<SootMethod,Set<SootMethod>> proxyToStub = getSootResolvedBinderProxyMethodsToEntryPoints();
			for(SootMethod sm : proxyToStub.keySet()){
				signaturesToProxyMethods.put(sm.getSignature(), sm);
			}
		}
		return signaturesToProxyMethods.get(sig);
	}
	
	/** See {@link #getSootResolvedEntryPointsFromBinderMethod(String)}. */
	public Set<SootMethod> getSootResolvedEntryPointsFromBinderMethod(InvokeExpr ie){
		return getSootResolvedEntryPointsFromBinderMethod(ie.getMethodRef().getSignature());
	}
	
	/** Returns null if not a interface, proxy, or stub method (i.e. binder method). Returns an empty set
	 * if the signature represents one of the three but there are no entry points resolvable from the 
	 * binder method. Returns a set set of SootMethods containing the entry points reachable from the 
	 * binder method represented by the signature otherwise. Note all sets returned by this method
	 * are members of the ImmutableSet class and thus cannot be modified.
	 */
	public Set<SootMethod> getSootResolvedEntryPointsFromBinderMethod(String sig){
		Set<SootMethod> ret = null;
		SootMethod sm = null;
		//Check if sig represents an interface method and if so lookup entry points using interface method
		sm = getSootMethodOfInterfaceMethodSignature(sig);
		if(sm != null){
			ret = getSootResolvedEntryPointsForBinderInterfaceMethod(sm);
		}
		//If not an interface method check if sig represents a proxy method and if so lookup entry points using proxy method
		if(ret == null || ret.isEmpty()){
			sm = getSootMethodOfProxyMethodSignature(sig);
			if(sm != null){
				ret = getSootResolvedEntryPointsForBinderProxyMethod(sm);
			}
		}
		//If not an interface or proxy method check if sig represents a stub method by looking for entry of it in stub methods to entry points map
		if(ret == null || ret.isEmpty()){
			ret = getSootResolvedEntryPointsForBinderStubMethod(sig);
		}
		return ret;
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}
	
	@Override
	public BinderGroupsDatabase readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	/** One of two ways to generate and BinderGroupsDatabase. The other is {@link #getNewBinderGroupsDatabase(Map, Map)}.
	 * This will read in an existing BinderGroupsDatabase from file using xstream and set the singleton variable
	 * to this BinderGroupsDatabase (overwriting the old variable if any). Note only one of the arguments must
	 * be given to read in the file.
	 */
	public static BinderGroupsDatabase readXMLStatic(String filePath, Path path) throws Exception {
		BinderGroupsDatabase newDatabase = new BinderGroupsDatabase().readXML(filePath, path);
		setDatabase(newDatabase);
		return newDatabase;
	}
	
	@XStreamOmitField
	private static AbstractXStreamSetup xstreamSetup = null;

	public static AbstractXStreamSetup getXStreamSetupStatic(){
		if(xstreamSetup == null)
			xstreamSetup = new XStreamSetup();
		return xstreamSetup;
	}
	
	@Override
	public AbstractXStreamSetup getXStreamSetup() {
		return getXStreamSetupStatic();
	}
	
	public static class XStreamSetup extends AbstractXStreamSetup {
		
		@Override
		public void getOutputGraph(LinkedHashSet<AbstractXStreamSetup> in) {
			if(!in.contains(this)) {
				in.add(this);
				GroupBContainer.getXStreamSetupStatic().getOutputGraph(in);
				FileHash.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(BinderGroupsDatabase.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}
}
