package org.sag.acminer.database.acminer;

import java.io.ObjectStreamException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sag.acminer.database.FileHashDatabase;
import org.sag.acminer.database.defusegraph.id.DataWrapperPart;
import org.sag.acminer.database.defusegraph.id.FieldRefPart;
import org.sag.acminer.database.defusegraph.id.MethodRefPart;
import org.sag.acminer.database.defusegraph.id.Part;
import org.sag.acminer.phases.acminer.ValuePair;
import org.sag.acminer.phases.acminer.ValuePairHashSet;
import org.sag.acminer.phases.acminer.ValuePairLinkedHashSet;
import org.sag.acminer.phases.acminer.dw.DataWrapper;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.io.FileHelpers;
import org.sag.common.tools.SortingMethods;
import org.sag.common.xstream.NamedCollectionConverterWithSize;
import org.sag.common.xstream.XStreamInOut;
import org.sag.soot.SootSort;
import org.sag.soot.xstream.SootClassContainer;
import org.sag.soot.xstream.SootMethodContainer;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import soot.SootClass;
import soot.SootMethod;

@XStreamAlias("ACMinerDatabase")
public class ACMinerDatabase extends FileHashDatabase implements IACMinerDatabase {
	
	@XStreamOmitField
	private volatile Map<SootClass, Map<SootMethod, ValuePairLinkedHashSet>> stubToEpToPairs;
	@XStreamOmitField
	private volatile Map<SootClass, Map<SootMethod, Set<String>>> stubToEpToMethods;
	@XStreamOmitField
	private volatile Map<SootClass, Map<SootMethod, Set<String>>> stubToEpToFields;
	
	@XStreamAlias("Stubs")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"StubContainer"},types={StubContainer.class})
	private volatile ArrayList<StubContainer> stubs;
	@XStreamAlias("EntryPoints")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"EntryPointInfo"},types={EntryPointInfo.class})
	private volatile ArrayList<EntryPointInfo> entryPoints;
	
	@XStreamOmitField
	private volatile Map<String, StubContainer> stubSigToStubCont;
	@XStreamOmitField
	private volatile Map<String, Set<String>> stubSigsToEpSigs;
	@XStreamOmitField
	private volatile Map<String, EntryPointInfo> epSigToEpInfo;
	@XStreamOmitField
	private volatile Map<String, EntryPointContainer> epDataCache;
	@XStreamOmitField
	private volatile Path dbDir; //Set when reading or writing to xml to the directory that the database file is in
	
	private ACMinerDatabase() {}
	
	public ACMinerDatabase(Set<EntryPoint> entryPoints) {
		this.stubToEpToPairs = new LinkedHashMap<>();
		this.stubToEpToMethods = new LinkedHashMap<>();
		this.stubToEpToFields = new LinkedHashMap<>();
		this.stubs = null;
		this.entryPoints = null;
		this.dbDir = dbDir;
		for(EntryPoint ep : entryPoints) {
			Map<SootMethod, ValuePairLinkedHashSet> epToPairs = this.stubToEpToPairs.get(ep.getStub());
			Map<SootMethod, Set<String>> epToMethods = this.stubToEpToMethods.get(ep.getStub());
			Map<SootMethod, Set<String>> epToFields = this.stubToEpToFields.get(ep.getStub());
			if(epToPairs == null) {
				epToPairs = new LinkedHashMap<>();
				this.stubToEpToPairs.put(ep.getStub(), epToPairs);
			}
			epToPairs.put(ep.getEntryPoint(), ValuePairLinkedHashSet.getEmptySet());
			if(epToMethods == null) {
				epToMethods = new LinkedHashMap<>();
				this.stubToEpToMethods.put(ep.getStub(), epToMethods);
			}
			epToMethods.put(ep.getEntryPoint(), Collections.emptySet());
			if(epToFields == null) {
				epToFields = new LinkedHashMap<>();
				this.stubToEpToFields.put(ep.getStub(), epToFields);
			}
			epToFields.put(ep.getEntryPoint(), Collections.emptySet());
		}
	}
	
	//ReadResolve is always run when reading from XML even if a constructor is run first
	protected Object readResolve() throws ObjectStreamException {
		if(stubs == null)
			throw new RuntimeException("Error: Reading in database with no data!?!");
		setupDataAccessors();
		return this;
	}
	
	private void setupDataAccessors() {
		stubSigToStubCont = new LinkedHashMap<>();;
		stubSigsToEpSigs = new LinkedHashMap<>();
		epSigToEpInfo = new LinkedHashMap<>();
		epDataCache = new HashMap<>();
		
		for(StubContainer stub : stubs) {
			stubSigToStubCont.put(stub.getStringStub(), stub);
			stubSigsToEpSigs.put(stub.getStringStub(), stub.getStringEntryPoints());
		}
		
		for(EntryPointInfo epi : entryPoints) {
			epSigToEpInfo.put(epi.getStringEntryPoint(), epi);
		}
	}
	
	protected Object writeReplace() throws ObjectStreamException {
		if(stubs == null)
			writeSootResolvedData();
		return this;
	}
	
	private void writeSootResolvedData() {
		stubs = new ArrayList<>();
		entryPoints = new ArrayList<>();
		for(SootClass stub : stubToEpToPairs.keySet()) {
			//only need to copy the map since we do not merge the sets but simply add to the map
			Map<SootMethod, ValuePairLinkedHashSet> epToValuePairs = stubToEpToPairs.get(stub);
			Map<SootMethod, Set<String>> epToMethods = stubToEpToMethods.get(stub);
			Map<SootMethod, Set<String>> epToFields = stubToEpToFields.get(stub);
			synchronized(epToValuePairs) {
				epToValuePairs = new LinkedHashMap<>(epToValuePairs);
			}
			synchronized(epToMethods) {
				epToMethods = new LinkedHashMap<>(epToMethods);
			}
			synchronized(epToFields) {
				epToFields = new LinkedHashMap<>(epToFields);
			}
			
			Set<SootMethod> eps = epToValuePairs.keySet();
			if(!epToMethods.keySet().equals(eps) || !epToFields.keySet().equals(eps))
				throw new RuntimeException("Error: The entry point data does not match for '" + stub + "'");
			
			this.stubs.add(new StubContainer(stub, eps));
			for(SootMethod ep : eps) {
				try {
					Path output = FileHelpers.getPath(dbDir, FileHelpers.getHashOfString("MD5", stub.toString() + ep.toString()) + ".xml");
					if(FileHelpers.checkRWFileExists(output))
						throw new RuntimeException("Error: A file name collision has occured for '" + stub.toString() + " " + ep.toString() + "' at '" + output + "'");
					EntryPointContainer epc = new EntryPointContainer(ep, epToValuePairs.get(ep), epToMethods.get(ep), epToFields.get(ep));
					epc.writeXML(null, output);
					this.entryPoints.add(new EntryPointInfo(ep, output));
				} catch(Exception e) {
					throw new RuntimeException("Error: An error occured when finalizing output for ep '" + ep + "'.",e);
				}
			}
		}
		
		setupDataAccessors();
		for(EntryPointInfo epi : entryPoints) {
			epDataCache.put(epi.getStringEntryPoint(), getEntryPointContainer(epi.getStringEntryPoint()));
		}
	}
	
	@Override
	public void clearSootResolvedData() {
		if(stubs == null)
			writeSootResolvedData();
		stubToEpToPairs = null;
		stubToEpToMethods = null;
		stubToEpToFields = null;
	}
	
	@Override
	public void addData(SootClass stub, SootMethod ep, ValuePairHashSet subedData) {
		if(stubToEpToPairs == null || stubToEpToMethods == null || stubToEpToFields == null)
			throw new RuntimeException("Error: This method cannot be called when data is read in from xml");
		ValuePairLinkedHashSet allPairs;
		if(subedData.isEmpty()) {
			allPairs = ValuePairLinkedHashSet.getEmptySet();
		} else {
			List<ValuePair> temp = new ArrayList<>(subedData);
			allPairs = new ValuePairLinkedHashSet();
			Collections.sort(temp);
			allPairs.addAll(temp);
		}
		Map<SootMethod, ValuePairLinkedHashSet> epToPairs = stubToEpToPairs.get(stub);
		if(epToPairs == null)
			throw new RuntimeException("Error: Tried to add unknown stub '" + stub + "'");
		synchronized(epToPairs) {
			epToPairs.put(ep, allPairs);
		}
		
		Set<String> toAddM = new HashSet<>();
		Set<String> toAddF = new HashSet<>();
		for(ValuePair vp : subedData) {
			Deque<DataWrapper> queue = new ArrayDeque<>();
			if(vp.size() == 1)
				queue.push(vp.getOp1());
			else if(vp.size() == 2) {
				queue.push(vp.getOp1());
				queue.push(vp.getOp2());
			}
			while(!queue.isEmpty()) {
				DataWrapper dw = queue.pop();
				for(Part p : dw.getIdentifier()) {
					if(p instanceof MethodRefPart)
						toAddM.add(((MethodRefPart)p).getMethodRef().toString());
					else if(p instanceof FieldRefPart)
						toAddF.add(((FieldRefPart)p).getFieldRef().toString());
					else if(p instanceof DataWrapperPart)
						queue.push(((DataWrapperPart)p).getDataWrapper());
				}
			}
		}
		toAddM = SortingMethods.sortSet(toAddM,SootSort.smStringComp);
		toAddF = SortingMethods.sortSet(toAddF,SootSort.sfStringComp);
		
		Map<SootMethod, Set<String>> epToMethods = stubToEpToMethods.get(stub);
		synchronized(epToMethods) {
			epToMethods.put(ep, toAddM);
		}
		
		Map<SootMethod, Set<String>> epToFields = stubToEpToFields.get(stub);
		synchronized(epToFields) {
			epToFields.put(ep, toAddF);
		}
	}
	
	/** Only to be called when outputting the data when it is generated */
	@Override
	public Map<SootMethod, ValuePairLinkedHashSet> getValuePairsForStub(SootClass stub) {
		if(stubToEpToPairs == null)
			throw new RuntimeException("Error: This method cannot be called when data is read in from xml");
		Map<SootMethod, ValuePairLinkedHashSet> epToPairs = stubToEpToPairs.get(stub);
		if(epToPairs == null)
			throw new RuntimeException("Error: Stub '" + stub.toString() + "' not found in database.");
		Map<SootMethod, ValuePairLinkedHashSet> ret = new LinkedHashMap<>();
		synchronized(epToPairs) {
			for(SootMethod sm : epToPairs.keySet()) {
				ret.put(sm, new ValuePairLinkedHashSet(epToPairs.get(sm)));
			}
		}
		return ret;
	}
	
	/** Only to be called when outputting the data when it is generated */
	@Override
	public Map<SootMethod, Set<String>> getMethodsForStub(SootClass stub) {
		if(stubToEpToMethods == null)
			throw new RuntimeException("Error: This method cannot be called when data is read in from xml");
		Map<SootMethod, Set<String>> epToMethods = stubToEpToMethods.get(stub);
		if(epToMethods == null)
			throw new RuntimeException("Error: Stub '" + stub.toString() + "' not found in database.");
		Map<SootMethod, Set<String>> ret = new LinkedHashMap<>();
		synchronized(epToMethods) {
			for(SootMethod sm : epToMethods.keySet()) {
				ret.put(sm, new LinkedHashSet<>(epToMethods.get(sm)));
			}
		}
		return ret;
	}
	
	/** Only to be called when outputting the data when it is generated */
	@Override
	public Map<SootMethod, Set<String>> getFieldsForStub(SootClass stub) {
		if(stubToEpToFields == null)
			throw new RuntimeException("Error: This method cannot be called when data is read in from xml");
		Map<SootMethod, Set<String>> epToFields = stubToEpToFields.get(stub);
		if(epToFields == null)
			throw new RuntimeException("Error: Stub '" + stub.toString() + "' not found in database.");
		Map<SootMethod, Set<String>> ret = new LinkedHashMap<>();
		synchronized(epToFields) {
			for(SootMethod sm : epToFields.keySet()) {
				ret.put(sm, new LinkedHashSet<>(epToFields.get(sm)));
			}
		}
		return ret;
	}
	
	private void checkFinalizeData() {
		if(stubs == null)
			throw new RuntimeException("Error: Finalize data has not be run yet.");
	}
	
	private EntryPointContainer getEntryPointContainer(String epSig) {
		EntryPointContainer epc;
		synchronized(epDataCache) {
			epc = epDataCache.get(epSig);
		}
		
		if(epc != null)
			return epc;
		
		EntryPointInfo epi = epSigToEpInfo.get(epSig);
		if(epi == null)
			throw new RuntimeException("Error: Cannot find ep '" + epSig + "' in the list of eps in this database.");
		epc = epi.getEntryPointContainer(dbDir);
		if(epc == null)
			throw new RuntimeException("Error: Failed to load the entry point container for '" + epSig + "'");
		
		synchronized(epDataCache) {
			EntryPointContainer temp = epDataCache.get(epSig);
			if(temp == null)
				epDataCache.put(epSig, epc);
			else
				epc = temp;
		}
		
		return epc;
	}
	
	private Map<String, EntryPointContainer> getEntryPointContainers(String stubSig) {
		Set<String> epSigs = stubSigsToEpSigs.get(stubSig);
		if(stubSigsToEpSigs == null)
			throw new RuntimeException("Error: Cannot find the stub '" + stubSig + "' in the stub list of this database.");
		Map<String, EntryPointContainer> ret = new LinkedHashMap<>();
		for(String epSig : epSigs) {
			ret.put(epSig, getEntryPointContainer(epSig));
		}
		return ret;
	}
	
	@Override
	public Map<String, Set<String>> getSourceMethodsForPairInStub(String stubSig, String pair) {
		checkFinalizeData();
		Map<String, EntryPointContainer> epcs = getEntryPointContainers(stubSig);
		Doublet toFind = new Doublet(pair);
		Map<String, Set<String>> ret = new LinkedHashMap<>();
		for(EntryPointContainer epc : epcs.values()) {
			Set<Doublet> valuePairs = epc.getDoublets();
			if(valuePairs.contains(toFind)) {
				Set<String> sources = new LinkedHashSet<>();
				for(Doublet d : valuePairs) {
					if(d.equals(toFind)) {
						sources.addAll(d.getSourcesAsStrings().keySet());
						break;
					}
				}
				ret.put(epc.getStringEntryPoint(), sources);
			}
		}
		return ret;
	}
	
	@Override
	public Set<String> getSourceMethodsForPairInEntryPoint(String epSig, String pair) {
		checkFinalizeData();
		EntryPointContainer epc = getEntryPointContainer(epSig);
		Doublet toFind = new Doublet(pair);
		Set<String> ret = new LinkedHashSet<>();
		Set<Doublet> valuePairs = epc.getDoublets();
		if(valuePairs.contains(toFind)) {
			for(Doublet d : valuePairs) {
				if(d.equals(toFind)) {
					ret.addAll(d.getSourcesAsStrings().keySet());
					break;
				}
			}
		}
		return ret;
	}
	
	@Override
	public Map<String, Set<String>> getSourceMethodsForPairInEntryPoints(Set<String> epSigs, String pair) {
		checkFinalizeData();
		Map<String, Set<String>> ret = new LinkedHashMap<>();
		for(String epSig : epSigs) {
			ret.put(epSig, getSourceMethodsForPairInEntryPoint(epSig, pair));
		}
		return ret;
	}
	
	@Override
	public Map<SootClass, Map<SootMethod, Set<Doublet>>> getSootValuePairs() {
		checkFinalizeData();
		Map<SootClass, Map<SootMethod, Set<Doublet>>> ret = new LinkedHashMap<>();
		for(StubContainer stubCont : stubs) {
			Map<SootMethod, Set<Doublet>> epToVp = new LinkedHashMap<>();
			Map<String, EntryPointContainer> epcs = getEntryPointContainers(stubCont.getStringStub());
			for(EntryPointContainer epc : epcs.values()) {
				epToVp.put(epc.getSootEntryPoint(), epc.getDoublets());
			}
			ret.put(stubCont.getSootStub(), epToVp);
		}
		return ret;
	}
	
	@Override
	public Map<SootClassContainer, Map<SootMethodContainer, Set<Doublet>>> getValuePairs() {
		checkFinalizeData();
		Map<SootClassContainer, Map<SootMethodContainer, Set<Doublet>>> ret = new LinkedHashMap<>();
		for(StubContainer stub : stubs) {
			Map<SootMethodContainer, Set<Doublet>> epToVp = new LinkedHashMap<>();
			Map<String, EntryPointContainer> epcs = getEntryPointContainers(stub.getStringStub());
			for(EntryPointContainer epc : epcs.values()) {
				epToVp.put(epc.getEntryPoint(), epc.getDoublets());
			}
			ret.put(stub.getStub(), epToVp);
		}
		return ret;
	}
	
	@Override
	public Map<String, Map<String, Set<Doublet>>> getStringValuePairs() {
		checkFinalizeData();
		Map<String, Map<String, Set<Doublet>>> ret = new LinkedHashMap<>();
		for(StubContainer stub : stubs) {
			Map<String, Set<Doublet>> epToVp = new LinkedHashMap<>();
			Map<String, EntryPointContainer> epcs = getEntryPointContainers(stub.getStringStub());
			for(EntryPointContainer epc : epcs.values()) {
				epToVp.put(epc.getStringEntryPoint(), epc.getDoublets());
			}
			ret.put(stub.getStringStub(), epToVp);
		}
		return ret;
	}
	
	@Override
	public Map<String, Set<Doublet>> getStringValuePairs(String stubSig) {
		checkFinalizeData();
		Map<String, Set<Doublet>> ret = new LinkedHashMap<>();
		Map<String, EntryPointContainer> epcs = getEntryPointContainers(stubSig);
		for(EntryPointContainer epc : epcs.values()) {
			ret.put(epc.getStringEntryPoint(), epc.getDoublets());
		}
		return ret;
	}
	
	@Override
	public Map<SootClass, Map<SootMethod, Set<String>>> getSootMethods() {
		checkFinalizeData();
		Map<SootClass, Map<SootMethod, Set<String>>> ret = new LinkedHashMap<>();
		for(StubContainer stub : stubs) {
			Map<SootMethod, Set<String>> epToMethods = new LinkedHashMap<>();
			Map<String, EntryPointContainer> epcs = getEntryPointContainers(stub.getStringStub());
			for(EntryPointContainer epc : epcs.values()) {
				epToMethods.put(epc.getSootEntryPoint(), epc.getMethods());
			}
			ret.put(stub.getSootStub(), epToMethods);
		}
		return ret;
	}
	
	@Override
	public Map<SootClassContainer, Map<SootMethodContainer, Set<String>>> getMethods() {
		checkFinalizeData();
		Map<SootClassContainer, Map<SootMethodContainer, Set<String>>> ret = new LinkedHashMap<>();
		for(StubContainer stub : stubs) {
			Map<SootMethodContainer, Set<String>> epToMethods = new LinkedHashMap<>();
			Map<String, EntryPointContainer> epcs = getEntryPointContainers(stub.getStringStub());
			for(EntryPointContainer epc : epcs.values()) {
				epToMethods.put(epc.getEntryPoint(), epc.getMethods());
			}
			ret.put(stub.getStub(), epToMethods);
		}
		return ret;
	}
	
	@Override
	public Map<String, Map<String, Set<String>>> getStringMethods() {
		checkFinalizeData();
		Map<String, Map<String, Set<String>>> ret = new LinkedHashMap<>();
		for(StubContainer stub : stubs) {
			Map<String, Set<String>> epToMethods = new LinkedHashMap<>();
			Map<String, EntryPointContainer> epcs = getEntryPointContainers(stub.getStringStub());
			for(EntryPointContainer epc : epcs.values()) {
				epToMethods.put(epc.getStringEntryPoint(), epc.getMethods());
			}
			ret.put(stub.getStringStub(), epToMethods);
		}
		return ret;
	}
	
	@Override
	public Map<SootClass, Map<SootMethod, Set<String>>> getSootFields() {
		checkFinalizeData();
		Map<SootClass, Map<SootMethod, Set<String>>> ret = new LinkedHashMap<>();
		for(StubContainer stub : stubs) {
			Map<SootMethod, Set<String>> epToFields = new LinkedHashMap<>();
			Map<String, EntryPointContainer> epcs = getEntryPointContainers(stub.getStringStub());
			for(EntryPointContainer epc : epcs.values()) {
				epToFields.put(epc.getSootEntryPoint(), epc.getFields());
			}
			ret.put(stub.getSootStub(), epToFields);
		}
		return ret;
	}
	
	@Override
	public Map<SootClassContainer, Map<SootMethodContainer, Set<String>>> getFields() {
		checkFinalizeData();
		Map<SootClassContainer, Map<SootMethodContainer, Set<String>>> ret = new LinkedHashMap<>();
		for(StubContainer stub : stubs) {
			Map<SootMethodContainer, Set<String>> epToFields = new LinkedHashMap<>();
			Map<String, EntryPointContainer> epcs = getEntryPointContainers(stub.getStringStub());
			for(EntryPointContainer epc : epcs.values()) {
				epToFields.put(epc.getEntryPoint(), epc.getFields());
			}
			ret.put(stub.getStub(), epToFields);
		}
		return ret;
	}
	
	@Override
	public Map<String, Map<String, Set<String>>> getStringFields() {
		checkFinalizeData();
		Map<String, Map<String, Set<String>>> ret = new LinkedHashMap<>();
		for(StubContainer stub : stubs) {
			Map<String, Set<String>> epToFields = new LinkedHashMap<>();
			Map<String, EntryPointContainer> epcs = getEntryPointContainers(stub.getStringStub());
			for(EntryPointContainer epc : epcs.values()) {
				epToFields.put(epc.getStringEntryPoint(), epc.getFields());
			}
			ret.put(stub.getStringStub(), epToFields);
		}
		return ret;
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		Path dbDir;
		if(path != null)
			dbDir = FileHelpers.getNormAndAbsPath(path).getParent();
		else if(filePath != null)
			dbDir = FileHelpers.getPath(filePath).getParent();
		else
			throw new Exception("Both filePath and path cannot be null at the same time!");
		this.dbDir = dbDir;
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public ACMinerDatabase readXML(String filePath, Path path) throws Exception {
		Path dbDir;
		if(path != null)
			dbDir = FileHelpers.getNormAndAbsPath(path).getParent();
		else if(filePath != null)
			dbDir = FileHelpers.getPath(filePath).getParent();
		else
			throw new Exception("Both filePath and path cannot be null at the same time!");
		ACMinerDatabase db = XStreamInOut.readXML(this, filePath, path);
		db.dbDir = dbDir;
		return db;
	}
	
	public static ACMinerDatabase readXMLStatic(String filePath, Path path) throws Exception {
		return new ACMinerDatabase().readXML(filePath, path);
	}
	
	@XStreamOmitField
	private static AbstractXStreamSetup xstreamSetup = null;
	
	public static AbstractXStreamSetup getXStreamSetupStatic(){
		if(xstreamSetup == null)
			xstreamSetup = new SubXStreamSetup();
		return xstreamSetup;
	}

	@Override
	public AbstractXStreamSetup getXStreamSetup() {
		return getXStreamSetupStatic();
	}
	
	public static class SubXStreamSetup extends XStreamSetup {
		
		@Override
		public void getOutputGraph(LinkedHashSet<AbstractXStreamSetup> in) {
			if(!in.contains(this)) {
				in.add(this);
				super.getOutputGraph(in);
				StubContainer.getXStreamSetupStatic().getOutputGraph(in);
				EntryPointInfo.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(ACMinerDatabase.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}

}
