package org.sag.acminer.phases.entrypoints;

import java.io.ObjectStreamException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.sag.common.io.FileHash;
import org.sag.common.tools.SortingMethods;
import org.sag.soot.SootSort;
import org.sag.sootinit.SootInstanceWrapper;
import org.sag.xstream.XStreamInOut;
import org.sag.xstream.XStreamInOut.XStreamInOutInterface;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import soot.SootClass;
import soot.SootMethod;

@XStreamAlias("EntryPointsDatabase")
public class EntryPointsDatabase implements XStreamInOutInterface {
	
	//begin singleton methods
	
	@XStreamOmitField
	private static volatile EntryPointsDatabase singleton = null;
	
	public static EntryPointsDatabase v(){
		if(!isSet())
			throw new RuntimeException("The EntryPointsDatabase was never initilized.");
		return singleton;
	}
	
	public static boolean isSet(){
		return singleton != null;
	}
	
	public static void setDatabase(EntryPointsDatabase database){
		singleton = database;
	}
	
	public static void resetDatabase(){
		singleton = null;
	}
	
	/**
	 * One of two ways to generate and EntryPointsDatabase. The other is {@link #readXMLStatic(String, Path)}. 
	 * This will generate a new database based off of the input given and return said database. It will also
	 * set the singleton variable (overwriting whatever is there) as this becomes the new only instance
	 * of this class.
	 * 
	 * @param stubMethods - The stub to entry points map. These are the normal entry points generated from
	 * onTransact.
	 * @return
	 */
	protected static EntryPointsDatabase getNewEntryPointsDatabase(Map<SootClass,Map<SootMethod,Set<Integer>>> stubMethods,
			Map<SootClass, Set<Integer>> stubsToAllTransactionIds){
		EntryPointsDatabase ret = new EntryPointsDatabase(stubMethods,stubsToAllTransactionIds);
		setDatabase(ret);
		return ret;
	}
	
	//end singleton methods
	
	//begin instance methods
	
	@XStreamAlias("DependencyFile")
	private volatile FileHash dependencyFile;
	
	@XStreamImplicit
	private volatile Set<StubEPContainer> stubs;
	
	@XStreamOmitField
	private volatile Set<String> stubsList;
	
	@XStreamOmitField
	private volatile Set<String> epClasses;
	
	@XStreamOmitField
	private volatile Map<String, Set<String>> stubToEpClasses;
	
	@XStreamOmitField
	private volatile Map<String, String> epClassToStub;
	
	@XStreamOmitField
	private volatile Set<String> epMethods;
	
	@XStreamOmitField
	private volatile Map<String,Set<String>> stubToEpMethods;
	
	@XStreamOmitField
	private volatile Map<String,Set<String>> classToEpMethods;
	
	@XStreamOmitField
	private volatile ImmutableMap<SootClass,ImmutableSet<SootMethod>> sootResolvedEntryPointsByStub;
	
	@XStreamOmitField
	private volatile Map<SootClass,Map<SootMethod,Set<IntegerWrapper>>> sootResolvedEntryPointsByStubWithId;
	
	@XStreamOmitField
	private volatile ImmutableMap<SootClass,ImmutableMap<IntegerWrapper,ImmutableSet<SootMethod>>> sootResolvedEntryPointsByIdAndStub;
	
	@XStreamOmitField
	private volatile Map<SootClass,Set<SootMethod>> sootResolvedEntryPointsByDeclaringClass;
	
	@XStreamOmitField
	private volatile ImmutableMap<SootClass,ImmutableMap<SootClass,ImmutableSet<SootMethod>>> sootResolvedEntryPointsByStubAndDeclaringClass;
	
	@XStreamOmitField
	private volatile Map<SootClass,Set<IntegerWrapper>> sootResolvedStubsToAllTransactionIds;
	
	@XStreamOmitField
	private volatile Set<EntryPoint> sootResolvedEntryPointsInAnalysis;
	
	@XStreamOmitField
	private volatile Set<SootMethod> sootResolvedEntryPointsInAnalysisAsSootMethods;
	
	@XStreamOmitField
	private volatile Set<EntryPoint> sootResolvedEntryPoints;
	
	@XStreamOmitField
	private volatile Set<SootMethod> sootResolvedEntryPointsAsSootMethods;

	//for use with xstream on read in only
	private EntryPointsDatabase(){
		stubs = null;
		stubsList = null;
		epClasses = null;
		stubToEpClasses = null;
		epClassToStub = null;
		epMethods = null;
		stubToEpMethods = null;
		classToEpMethods = null;
		dependencyFile = null;
		sootResolvedEntryPointsByStub = null;
		sootResolvedEntryPointsByStubWithId = null;
		sootResolvedEntryPointsByIdAndStub = null;
		sootResolvedEntryPointsByDeclaringClass = null;
		sootResolvedEntryPointsByStubAndDeclaringClass = null;
		sootResolvedStubsToAllTransactionIds = null;
		sootResolvedEntryPointsInAnalysis = null;
		sootResolvedEntryPointsInAnalysisAsSootMethods = null;
		sootResolvedEntryPoints = null;
		sootResolvedEntryPointsAsSootMethods = null;
		
	}
	
	//Initialize a new EntryPointsDatabase based off of newly generated data not from a xml file
	//Assumes the input is already sorted which it is if it comes from GenerateEntryPoints which it should
	private EntryPointsDatabase(Map<SootClass,Map<SootMethod,Set<Integer>>> stubMethods, Map<SootClass,Set<Integer>> stubsToAllTransactionIds){
		this();
		stubs = new LinkedHashSet<StubEPContainer>();
		initStubList(stubMethods,stubsToAllTransactionIds);
		generateClassInfo();
		generateMethodInfo();
	}
	
	//ReadResolve is always run when reading from XML even if a constructor is run first
	private Object readResolve() throws ObjectStreamException {
		generateClassInfo();
		generateMethodInfo();
		return this;
	}
	
	private Object writeReplace() throws ObjectStreamException {
		return this;
	}
	
	public boolean equals(Object o){
		if(this == o){
			return true;
		}
		if(o == null || !(o instanceof EntryPointsDatabase)){
			return false;
		}
		EntryPointsDatabase ep2 = (EntryPointsDatabase) o;
		return Objects.equals(dependencyFile, ep2.dependencyFile) && Objects.equals(stubs, ep2.stubs);
	}
	
	public int hashCode(){
		int hash = 17;
		hash = 31 * hash + Objects.hashCode(stubs);
		hash = 31 * hash + Objects.hashCode(dependencyFile);
		return hash;
	}
	
	public void setDependencyFile(FileHash dependencyFile){
		this.dependencyFile = dependencyFile;
	}
	
	public FileHash getDependencyFile(){
		return dependencyFile;
	}
	
	public void initStubList(Map<SootClass,Map<SootMethod,Set<Integer>>> stubMethods, Map<SootClass,Set<Integer>> stubsToAllTransactionIds){
		for(SootClass stub : stubMethods.keySet()){
			StubEPContainer nStub = new StubEPContainer(stub,stubsToAllTransactionIds.get(stub));
			Map<SootMethod,Set<Integer>> methodsForStub = stubMethods.get(stub);
			if(methodsForStub != null){
				for(Map.Entry<SootMethod, Set<Integer>> e : methodsForStub.entrySet()){
					nStub.addMethod(e.getKey(),e.getValue());
				}
			}
			if(methodsForStub != null){
				stubs.add(nStub);
			}
		}
	}
	
	private void generateClassInfo(){
		stubsList = new LinkedHashSet<String>();
		epClasses = new LinkedHashSet<String>();
		stubToEpClasses = new HashMap<String,Set<String>>();
		epClassToStub = new HashMap<String,String>();
		
		for(StubEPContainer stub : stubs){
			stubsList.add(stub.getStub().getSignature());
			LinkedHashSet<String> epTemp = new LinkedHashSet<String>();
			for(Iterator<EntryPointEPContainer> it = stub.iterator(); it.hasNext();){
				EntryPointEPContainer m = it.next();
				epClasses.add(m.getEntryPoint().getDeclaringClass());
				epTemp.add(m.getEntryPoint().getDeclaringClass());
				epClassToStub.put(m.getEntryPoint().getDeclaringClass(), stub.getStub().getSignature());
			}
			if(!epTemp.isEmpty()){
				stubToEpClasses.put(stub.getStub().getSignature(), epTemp);
			}
		}
	}

	private void generateMethodInfo() {
		stubToEpMethods = new HashMap<String,Set<String>>();
		classToEpMethods = new HashMap<String,Set<String>>();
		Set<String> epM = new LinkedHashSet<String>();
		for(StubEPContainer stub : stubs){
			Set<String> epTemp = new LinkedHashSet<String>();
			for(Iterator<EntryPointEPContainer> it = stub.iterator(); it.hasNext();){
				EntryPointEPContainer m = it.next();
				epM.add(m.getEntryPoint().getSignature());
				epTemp.add(m.getEntryPoint().getSignature());
				Set<String> epMethods = classToEpMethods.get(m.getEntryPoint().getDeclaringClass());
				if(epMethods == null){
					epMethods = new LinkedHashSet<String>();
					classToEpMethods.put(m.getEntryPoint().getDeclaringClass(), epMethods);
				}
				epMethods.add(m.getEntryPoint().getSignature());
			}
			if(!epTemp.isEmpty()){
				stubToEpMethods.put(stub.getStub().getSignature(), epTemp);
			}
		}
		epMethods = epM;
	}
	
	public Set<String> stubToEpMethod(String stub){
		Set<String> ret = stubToEpMethods.get(stub);
		if(ret == null){
			return Collections.emptySet();
		}
		return ret;
	}
	
	public Set<String> classToEpMethod(String cl){
		Set<String> ret = classToEpMethods.get(cl);
		if(ret == null){
			return Collections.emptySet();
		}
		return ret;
	}
	
	public Map<String,Set<String>> getAllEpMethodsByStub(){
		return stubToEpMethods;
	}
	
	public Set<String> getAllStubs(){
		return stubsList;
	}
	
	public Set<String> getAllEpMethods(){
		return epMethods;
	}
	
	public Set<String> getAllEpClasses(){
		return epClasses;
	}
	
	public String epMethodToStub(SootMethod m){
		String ret = epClassToStub.get(m.getDeclaringClass().getName());
		if(ret == null){
			return "";
		}
		return ret;
	}
	
	public String epMethodToStub(String sig){
		int index = sig.indexOf( ":" );
		String ret = "";
		if( index >= 0 ){
			ret = epClassToStub.get(sig.substring(1,index));
			if(ret == null){
				ret = "";
			}
		}
		return ret;
	}
	
	public String epClassToStub(String epClass){
		String ret = epClassToStub.get(epClass);
		if(ret == null){
			return "";
		}
		return ret;
	}
	
	public Set<String> stubToEpClasses(String stub){
		Set<String> ret = stubToEpClasses.get(stub);
		if(ret == null){
			return Collections.emptySet();
		}
		return ret;
	}
	
	public Map<String,Set<String>> getAllEpClassesByStub(){
		return stubToEpClasses;
	}
	
	public final static class IntegerWrapper implements Comparable<IntegerWrapper> {
		private Integer i;
		public IntegerWrapper(Integer i){ this.i = i; }
		public boolean isNull(){ return i == null; }
		public int getInt(){ return i; };
		public Integer getInteger(){ return i; }
		public int hashCode(){ return Objects.hashCode(i); }
		public boolean equals(Object o){
			if(o == this) return true;
			if(o == null || !(o instanceof IntegerWrapper)) return false;
			return Objects.equals(this.i, ((IntegerWrapper)o).i);
		}
		public int compareTo(IntegerWrapper o){
			if(o == null) return 1;
			if(this.isNull()){
				if(o.isNull()){
					return 0;
				}else{
					return -1;
				}
			}else{
				if(o.isNull()){
					return 1;
				}else{
					return i.compareTo(o.i);
				}
			}
		}
	}
	
	private Map<Integer,IntegerWrapper> intToIntWrap = null;
	
	private IntegerWrapper getIntegerWrapper(Integer i){
		if(intToIntWrap == null) intToIntWrap = new HashMap<>();
		IntegerWrapper ret = intToIntWrap.get(i);
		if(ret == null){
			ret = new IntegerWrapper(i);
			intToIntWrap.put(i, ret);
		}
		return ret;
	}
	
	public void resetSootResolvedData(){
		intToIntWrap = null;
		sootResolvedEntryPointsByStub = null;
		sootResolvedEntryPointsByStubWithId = null;
		sootResolvedEntryPointsByIdAndStub = null;
		sootResolvedEntryPointsByDeclaringClass = null;
		sootResolvedEntryPointsByStubAndDeclaringClass = null;
		sootResolvedStubsToAllTransactionIds = null;
		sootResolvedEntryPointsInAnalysis = null;
		sootResolvedEntryPointsInAnalysisAsSootMethods = null;
		sootResolvedEntryPoints = null;
		sootResolvedEntryPointsAsSootMethods = null;
	}
	
	private void checkSootInit(){
		if(!SootInstanceWrapper.v().isAnySootInitValueSet())
			throw new RuntimeException("Error: Some instance of Soot must be initilized first.");
	}
	
	public Set<EntryPoint> getEntryPoints(){
		if(sootResolvedEntryPoints == null){
			checkSootInit();
			Map<SootClass,Map<SootMethod,Set<IntegerWrapper>>> allEntryPoints = getSootResolvedEntryPointsByStubWithTransactionId();
			sootResolvedEntryPoints = new HashSet<>();
			
			for(SootClass stub : allEntryPoints.keySet()){
				Map<SootMethod,Set<IntegerWrapper>> eps = allEntryPoints.get(stub);
				for(SootMethod ep : eps.keySet()){
					sootResolvedEntryPoints.add(new EntryPoint(ep,stub));
				}
			}
			sootResolvedEntryPoints = ImmutableSet.copyOf(SortingMethods.sortSet(sootResolvedEntryPoints));
		}
		return sootResolvedEntryPoints;
	}
	
	public Set<SootMethod> getEntryPointsAsSootMethods(){
		if(sootResolvedEntryPointsAsSootMethods == null){
			//Use the iteration order of getEntryPoints() which is sorted by Stub then entry point
			sootResolvedEntryPointsAsSootMethods = new LinkedHashSet<>();
			for(EntryPoint ep : getEntryPoints()){
				sootResolvedEntryPointsAsSootMethods.add(ep.getEntryPoint());
			}
			sootResolvedEntryPointsAsSootMethods = ImmutableSet.copyOf(sootResolvedEntryPointsAsSootMethods);
		}
		return sootResolvedEntryPointsAsSootMethods;
	}
	
	public Map<SootClass,Set<IntegerWrapper>> getSootResolvedStubsToAllTransactionIds(){
		if(sootResolvedStubsToAllTransactionIds == null){
			if(!SootInstanceWrapper.v().isAnySootInitValueSet())
				throw new RuntimeException("Error: Some instance of Soot must be initilized first.");
			sootResolvedStubsToAllTransactionIds = new HashMap<>();
			for(StubEPContainer stub : stubs){
				Set<IntegerWrapper> temp = new HashSet<>();
				for(Integer i : stub.getAllTransactionIds()){
					temp.add(getIntegerWrapper(i));
				}
				sootResolvedStubsToAllTransactionIds.put(stub.getStub().toSootClass(), ImmutableSet.copyOf(SortingMethods.sortSet(temp)));
			}
			sootResolvedStubsToAllTransactionIds = ImmutableMap.copyOf(SortingMethods.sortMapKey(
					sootResolvedStubsToAllTransactionIds, SootSort.scComp));
		}
		return sootResolvedStubsToAllTransactionIds;
	}
	
	public boolean isTransactionIdForStub(SootClass stub, Integer id){
		Set<IntegerWrapper> temp = getSootResolvedStubsToAllTransactionIds().get(stub);
		if(temp != null){
			return temp.contains(getIntegerWrapper(id));
		}
		return false;
	}
	
	public ImmutableMap<SootClass,ImmutableSet<SootMethod>> getSootResolvedEntryPointsByStub(){
		if(sootResolvedEntryPointsByStub == null){
			if(!SootInstanceWrapper.v().isAnySootInitValueSet())
				throw new RuntimeException("Error: Some instance of Soot must be initilized first.");
			Map<SootClass,ImmutableSet<SootMethod>> ret = new HashMap<>();
			for(StubEPContainer s : stubs){
				Set<SootMethod> temp = new HashSet<>();
				for(EntryPointEPContainer ep : s){
					temp.add(ep.getEntryPoint().toSootMethod());
				}
				ret.put(s.getStub().toSootClass(), ImmutableSet.copyOf(SortingMethods.sortSet(temp,SootSort.smComp)));
			}
			sootResolvedEntryPointsByStub = ImmutableMap.copyOf(SortingMethods.sortMapKey(ret, SootSort.scComp));
		}
		return sootResolvedEntryPointsByStub;
	}
	
	public Map<SootClass,Map<SootMethod,Set<IntegerWrapper>>> getSootResolvedEntryPointsByStubWithTransactionId(){
		if(sootResolvedEntryPointsByStubWithId == null){
			if(!SootInstanceWrapper.v().isAnySootInitValueSet())
				throw new RuntimeException("Error: Some instance of Soot must be initilized first.");
			Map<SootClass,Map<SootMethod,Set<IntegerWrapper>>> ret = new HashMap<>();
			for(StubEPContainer s : stubs){
				Map<SootMethod,Set<IntegerWrapper>> temp = new HashMap<>();
				for(EntryPointEPContainer ep : s){
					HashSet<IntegerWrapper> tranIds = new HashSet<>();
					for(Integer i : ep.getTransactionIds()){
						tranIds.add(getIntegerWrapper(i));
					}
					temp.put(ep.getEntryPoint().toSootMethod(),ImmutableSet.copyOf(SortingMethods.sortSet(tranIds)));
				}
				ret.put(s.getStub().toSootClass(), ImmutableMap.copyOf(SortingMethods.sortMapKey(temp,SootSort.smComp)));
			}
			sootResolvedEntryPointsByStubWithId = ImmutableMap.copyOf(SortingMethods.sortMapKey(ret, SootSort.scComp));
		}
		return sootResolvedEntryPointsByStubWithId;
	}
	
	public ImmutableMap<SootClass,ImmutableMap<IntegerWrapper,ImmutableSet<SootMethod>>> getSootResolvedEntryPointsByTransactionIdAndStub(){
		if(sootResolvedEntryPointsByIdAndStub == null){
			if(!SootInstanceWrapper.v().isAnySootInitValueSet())
				throw new RuntimeException("Error: Some instance of Soot must be initilized first.");
			Map<SootClass,ImmutableMap<IntegerWrapper,ImmutableSet<SootMethod>>> ret = new HashMap<>();
			for(StubEPContainer s : stubs){
				Map<IntegerWrapper,Set<SootMethod>> temp = new HashMap<>();
				for(EntryPointEPContainer ep : s){
					for(Integer id : ep.getTransactionIds()){
						Set<SootMethod> temp2 = temp.get(getIntegerWrapper(id));
						if(temp2 == null){
							temp2 = new HashSet<>();
							temp.put(getIntegerWrapper(id), temp2);
						}
						temp2.add(ep.getEntryPoint().toSootMethod());
					}
				}
				temp = SortingMethods.sortMapKeyAscending(temp);
				ImmutableMap.Builder<IntegerWrapper, ImmutableSet<SootMethod>> b = ImmutableMap.builder();
				for(IntegerWrapper i : temp.keySet()){
					b.put(i, ImmutableSet.copyOf(SortingMethods.sortSet(temp.get(i),SootSort.smComp)));
				}
				ret.put(s.getStub().toSootClass(), b.build());
			}
			sootResolvedEntryPointsByIdAndStub = ImmutableMap.copyOf(SortingMethods.sortMapKey(ret, SootSort.scComp));
		}
		return sootResolvedEntryPointsByIdAndStub;
	}
	
	public Map<SootClass, Set<SootMethod>> getSootResolvedEntryPointsByDeclaringClass(){
		if(sootResolvedEntryPointsByDeclaringClass == null){
			if(!SootInstanceWrapper.v().isAnySootInitValueSet())
				throw new RuntimeException("Error: Some instance of Soot must be initilized first.");
			Map<SootClass,Set<SootMethod>> ret = new HashMap<>();
			for(StubEPContainer s : stubs){
				for(EntryPointEPContainer ep : s){
					SootMethod m = ep.getEntryPoint().toSootMethod();
					Set<SootMethod> temp = ret.get(m.getDeclaringClass());
					if(temp == null){
						temp = new HashSet<>();
						ret.put(m.getDeclaringClass(), temp);
					}
					temp.add(m);
				}
			}
			ret = SortingMethods.sortMapKey(ret, SootSort.scComp);
			ImmutableMap.Builder<SootClass, Set<SootMethod>> b = ImmutableMap.<SootClass,Set<SootMethod>>builder();
			for(SootClass sc : ret.keySet()){
				b.put(sc, ImmutableSet.copyOf(SortingMethods.sortSet(ret.get(sc),SootSort.smComp)));
			}
			sootResolvedEntryPointsByDeclaringClass = b.build();
		}
		return sootResolvedEntryPointsByDeclaringClass;
	}
	
	public ImmutableMap<SootClass,ImmutableMap<SootClass,ImmutableSet<SootMethod>>> getSootResolvedEntryPointsByStubAndDeclaringClass(){
		if(sootResolvedEntryPointsByStubAndDeclaringClass == null){
			if(!SootInstanceWrapper.v().isAnySootInitValueSet())
				throw new RuntimeException("Error: Some instance of Soot must be initilized first.");
			Map<SootClass, ImmutableMap<SootClass,ImmutableSet<SootMethod>>> ret = new HashMap<>();
			for(StubEPContainer s : stubs){
				Map<SootClass, Set<SootMethod>> ret2 = new HashMap<SootClass, Set<SootMethod>>();
				for(EntryPointEPContainer ep : s){
					SootMethod m = ep.getEntryPoint().toSootMethod();
					Set<SootMethod> temp = ret2.get(m.getDeclaringClass());
					if(temp == null){
						temp = new HashSet<>();
						ret2.put(m.getDeclaringClass(), temp);
					}
					temp.add(m);
				}
				ret2 = SortingMethods.sortMapKey(ret2, SootSort.scComp);
				ImmutableMap.Builder<SootClass, ImmutableSet<SootMethod>> b = ImmutableMap.<SootClass,ImmutableSet<SootMethod>>builder();
				for(SootClass sc : ret2.keySet()){
					b.put(sc, ImmutableSet.copyOf(SortingMethods.sortSet(ret2.get(sc),SootSort.smComp)));
				}
				ret.put(s.getStub().toSootClass(), b.build());
			}
			sootResolvedEntryPointsByStubAndDeclaringClass = ImmutableMap.copyOf(ret);
		}
		return sootResolvedEntryPointsByStubAndDeclaringClass;
	}
	
	public Set<SootMethod> getSootResolvedEntryPointsForStub(String stub){
		for(StubEPContainer s : stubs){
			if(s.getStub().getSignature().equals(stub)){
				return getSootResolvedEntryPointsForStub(s);
			}
		}
		return ImmutableSet.of();
	}
	
	public Set<SootMethod> getSootResolvedEntryPointsForStub(StubEPContainer stub) {
		if(!SootInstanceWrapper.v().isAnySootInitValueSet())
			throw new RuntimeException("Error: Some instance of Soot must be initilized first.");
		return getSootResolvedEntryPointsForStub(stub.getStub().toSootClass());
	}
	
	public Set<SootMethod> getSootResolvedEntryPointsForStub(SootClass stub){
		return getSootResolvedEntryPointsByStub().get(stub);
	}
	
	public ImmutableMap<SootClass,ImmutableSet<SootMethod>> getSootResolvedEntryPointsByDeclaringClassGivenStub(String stub) {
		for(StubEPContainer s : stubs){
			if(s.getStub().getSignature().equals(stub)){
				return getSootResolvedEntryPointsByDeclaringClassGivenStub(s);
			}
		}
		return ImmutableMap.<SootClass,ImmutableSet<SootMethod>>of();
	}
	
	public ImmutableMap<SootClass,ImmutableSet<SootMethod>> getSootResolvedEntryPointsByDeclaringClassGivenStub(StubEPContainer stub) {
		if(!SootInstanceWrapper.v().isAnySootInitValueSet())
			throw new RuntimeException("Error: Some instance of Soot must be initilized first.");
		return getSootResolvedEntryPointsByDeclaringClassGivenStub(stub.getStub().toSootClass());
	}
	
	public ImmutableMap<SootClass,ImmutableSet<SootMethod>> getSootResolvedEntryPointsByDeclaringClassGivenStub(SootClass stub) {
		return getSootResolvedEntryPointsByStubAndDeclaringClass().get(stub);
	}
	
	public Set<IntegerWrapper> getTransactionIds(SootClass stub, SootMethod ep){
		return getSootResolvedEntryPointsByStubWithTransactionId().get(stub).get(ep);
	}
	
	public Map<SootMethod,Set<IntegerWrapper>> getTransactionIdsByEntryPoints(SootClass stub){
		return getSootResolvedEntryPointsByStubWithTransactionId().get(stub);
	}
	
	public ImmutableMap<IntegerWrapper,ImmutableSet<SootMethod>> getEntryPointsByTransactionId(SootClass stub){
		return getSootResolvedEntryPointsByTransactionIdAndStub().get(stub);
	}
	
	public Set<SootMethod> getEntryPointsOfTransactionId(SootClass stub, Integer i){
		return getSootResolvedEntryPointsByTransactionIdAndStub().get(stub).get(getIntegerWrapper(i));
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}
	
	@Override
	public EntryPointsDatabase readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	/**
	 * One of two way to generate and EntryPointsDatabase. The other is {@link #getNewEntryPointsDatabase(Map, Map)}.
	 * This will read in an existing EntryPointsDatabase from file using xstream and set the singleton variable
	 * to this EntryPointsDatabase (overwriting the old variable if any). Note only one of the arguments must
	 * be given to read in the file.
	 * 
	 * @param filePath - String representation of the path to the file to be read in
	 * @param path - Path representation of the path to the file to be read in
	 * @return The EntryPointsDatabase
	 * @throws Exception Any exception that might be thrown during the read in procedure.
	 */
	public static EntryPointsDatabase readXMLStatic(String filePath, Path path) throws Exception {
		EntryPointsDatabase newDatabase = new EntryPointsDatabase().readXML(filePath, path);
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
				StubEPContainer.getXStreamSetupStatic().getOutputGraph(in);
				FileHash.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(EntryPointsDatabase.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}
	
}
