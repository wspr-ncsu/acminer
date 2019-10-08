package org.sag.acminer.database.entrypointedges;

import java.io.ObjectStreamException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.sag.acminer.database.FileHashDatabase;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.tools.SortingMethods;
import org.sag.common.tuple.Pair;
import org.sag.soot.SootSort;
import org.sag.xstream.XStreamInOut;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import soot.SootMethod;
import soot.Unit;

@XStreamAlias("EntryPointEdgesDatabase")
public class EntryPointEdgesDatabase extends FileHashDatabase implements IEntryPointEdgesDatabase {
	
	@XStreamOmitField
	private final ReadWriteLock rwlock;
	//Map<EntryPoint,Map<I/EEntryPoint,Pair<I/EId,Map<SourceMethod,Pair<Set<InvokeStmt>,Set<Depth>>>>>>
	@XStreamOmitField
	private volatile Map<EntryPoint,Map<EntryPoint,Pair<Type,Map<SootMethod,Pair<Set<Unit>,Set<Integer>>>>>> data;
	@XStreamOmitField
	private volatile boolean loaded;
	@XStreamImplicit
	protected volatile Set<EntryPointContainer> output;

	protected EntryPointEdgesDatabase(boolean newDB) {
		if(newDB) {
			data = new HashMap<>();
			loaded = true;
		} else {
			data = null;
			loaded = false;
		}
		rwlock = new ReentrantReadWriteLock();
		output = null;
	}
	
	//ReadResolve is always run when reading from XML even if a constructor is run first
	protected Object readResolve() throws ObjectStreamException {
		//We could load the soot resolved data here but we don't because we want to be able
		//to load this database from file without having soot initialized
		//We can just call loadSootResolvedData after if we wish to use this with soot
		//loadSootResolvedData();
		return this;
	}
	
	protected Object writeReplace() throws ObjectStreamException {
		rwlock.writeLock().lock();
		try {
			writeSootResolvedDataWLocked();
		} finally {
			rwlock.writeLock().unlock();
		}
		return this;
	}
	
	private void sortDataLocked() {
		for(EntryPoint ep : data.keySet()) {
			Map<EntryPoint,Pair<Type,Map<SootMethod,Pair<Set<Unit>,Set<Integer>>>>> ieepToData = data.get(ep);
			for(EntryPoint ieep : ieepToData.keySet()) {
				Pair<Type,Map<SootMethod,Pair<Set<Unit>,Set<Integer>>>> isExternalAndSources = ieepToData.get(ieep);
				Map<SootMethod,Pair<Set<Unit>,Set<Integer>>> sourceToUnitsAndDepth = isExternalAndSources.getSecond();
				for(SootMethod source : sourceToUnitsAndDepth.keySet()) {
					Pair<Set<Unit>,Set<Integer>> unitsAndDepth = sourceToUnitsAndDepth.get(source);
					unitsAndDepth = new Pair<>(SortingMethods.sortSet(unitsAndDepth.getFirst(),SootSort.unitComp),
							SortingMethods.sortSet(unitsAndDepth.getSecond()));
					sourceToUnitsAndDepth.put(source, unitsAndDepth);
				}
				sourceToUnitsAndDepth = SortingMethods.sortMapKey(sourceToUnitsAndDepth, SootSort.smComp);
				isExternalAndSources = new Pair<>(isExternalAndSources.getFirst(),sourceToUnitsAndDepth);
				ieepToData.put(ieep, isExternalAndSources);
			}
			ieepToData = SortingMethods.sortMapKeyAscending(ieepToData);
			data.put(ep, ieepToData);
		}
		data = SortingMethods.sortMapKeyAscending(data);
	}
	
	@Override
	public void clearSootResolvedData() {
		rwlock.writeLock().lock();
		try {
			writeSootResolvedDataWLocked();
			data = null;
			loaded = false;
		} finally {
			rwlock.writeLock().unlock();
		}
	}
	
	@Override
	public void loadSootResolvedData() {
		rwlock.writeLock().lock();
		try {
			loadSootResolvedDataWLocked();
		} finally {
			rwlock.writeLock().unlock();
		}
	}
	
	private void writeSootResolvedDataWLocked() {
		if(loaded) {
			sortDataLocked();
			output = new LinkedHashSet<>();
			for(EntryPoint ep : data.keySet())
				output.add(new EntryPointContainer(ep,data.get(ep)));
		}
	}
	
	private void loadSootResolvedDataWLocked() {
		if(!loaded) {
			data = new HashMap<>();
			//Note this will fail with an exception if the data cannot be loaded 
			//as this should always be the same or be recreated 
			for(EntryPointContainer epContainer : output) {
				data.put(new EntryPoint(epContainer.getEntryPoint(),epContainer.getStub()),epContainer.getData());
			}
			sortDataLocked();
			loaded = true;
		}
	}
	
	private void loadSootResolvedDataRLocked() {
		if(!loaded) {
			// Must release read lock before acquiring write lock
			rwlock.readLock().unlock();
			rwlock.writeLock().lock();
			try {
				loadSootResolvedDataWLocked();
				rwlock.readLock().lock(); // Downgrade by acquiring read lock before releasing write lock
			} finally {
				rwlock.writeLock().unlock(); // Unlock write, still hold read
			}
		}
	}
	
	@Override
	public String toString() {
		return toString("");
	}
	
	@Override
	public String toString(String spacer) {
		StringBuilder sb = new StringBuilder();
		sb.append(spacer).append("Referenced Entry Points Database:\n");
		for(EntryPointContainer e : getOutputData()) {
			sb.append(e.toString(spacer + "  "));
		}
		return sb.toString();
	}
	
	@Override
	public int hashCode() {
		int i = 17;
		i = i * 31 + Objects.hashCode(getOutputData());
		return i;
	}
	
	@Override
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof IEntryPointEdgesDatabase))
			return false;
		IEntryPointEdgesDatabase other = (IEntryPointEdgesDatabase)o;
		return Objects.equals(getOutputData(), other.getOutputData());
	}
	
	@Override
	public void sortData() {
		rwlock.writeLock().lock();
		try {
			loadSootResolvedDataWLocked();
			sortDataLocked();
		} finally {
			rwlock.writeLock().unlock();
		}
	}
	
	@Override
	public void add(EntryPoint ep, Map<EntryPoint,Pair<Type,Map<SootMethod,Pair<Set<Unit>,Set<Integer>>>>> dataToAdd) {
		rwlock.writeLock().lock();
		try {
			loadSootResolvedDataWLocked();
			addInner(ep,dataToAdd);
		} finally {
			rwlock.writeLock().unlock();
		}
	}
	
	@Override
	public void addAll(Map<EntryPoint, Map<EntryPoint,Pair<Type,Map<SootMethod,Pair<Set<Unit>,Set<Integer>>>>>> dataToAdd) {
		rwlock.writeLock().lock();
		try {
			loadSootResolvedDataWLocked();
			for(EntryPoint ep : dataToAdd.keySet()) {
				addInner(ep, dataToAdd.get(ep));
			}
		} finally {
			rwlock.writeLock().unlock();
		}
	}
	
	private void addInner(EntryPoint ep, Map<EntryPoint,Pair<Type,Map<SootMethod,Pair<Set<Unit>,Set<Integer>>>>> dataToAdd) {
		Objects.requireNonNull(ep);
		Objects.requireNonNull(dataToAdd);
		if(!dataToAdd.isEmpty()) {
			Map<EntryPoint,Pair<Type,Map<SootMethod,Pair<Set<Unit>,Set<Integer>>>>> ieepsToData = data.get(ep);
			if(ieepsToData == null) {
				ieepsToData = new HashMap<>();
				data.put(ep, ieepsToData);
			}
			for(EntryPoint ieep : dataToAdd.keySet()) {
				Pair<Type,Map<SootMethod,Pair<Set<Unit>,Set<Integer>>>> newData = dataToAdd.get(ieep);
				Pair<Type,Map<SootMethod,Pair<Set<Unit>,Set<Integer>>>> curData = ieepsToData.get(ieep);
				
				Objects.requireNonNull(newData);
				Objects.requireNonNull(newData.getFirst());
				Objects.requireNonNull(newData.getSecond());
				if(curData == null) {
					curData = new Pair<>(newData.getFirst(), new HashMap<>());
					ieepsToData.put(ieep, curData);
				}
				
				Map<SootMethod,Pair<Set<Unit>,Set<Integer>>> newSourceToUnitAndDepth = newData.getSecond();
				Map<SootMethod,Pair<Set<Unit>,Set<Integer>>> curSourceToUnitAndDepth = curData.getSecond();
				for(SootMethod source : newSourceToUnitAndDepth.keySet()) {
					Pair<Set<Unit>,Set<Integer>> newUnitAndDepth = newSourceToUnitAndDepth.get(source);
					Pair<Set<Unit>,Set<Integer>> curUnitAndDepth = curSourceToUnitAndDepth.get(source);
					
					Objects.requireNonNull(newUnitAndDepth);
					Objects.requireNonNull(newUnitAndDepth.getFirst());
					Objects.requireNonNull(newUnitAndDepth.getSecond());
					if(curUnitAndDepth == null) {
						curUnitAndDepth = new Pair<>(new HashSet<>(), new HashSet<>());
						curSourceToUnitAndDepth.put(source, curUnitAndDepth);
					}
					curUnitAndDepth.getFirst().addAll(newUnitAndDepth.getFirst());
					curUnitAndDepth.getSecond().addAll(newUnitAndDepth.getSecond());
				}
			}
		}
	}
	
	@Override
	public boolean hasEntryPointEdges(EntryPoint ep) {
		return hasEntryPointEdgesLocal(ep, Type.Both);
	}
	
	@Override
	public boolean hasInternalEntryPointEdges(EntryPoint ep) {
		return hasEntryPointEdgesLocal(ep, Type.Internal);
	}
	
	@Override
	public boolean hasExtenralEntryPointEdges(EntryPoint ep) {
		return hasEntryPointEdgesLocal(ep, Type.External);
	}
	
	private boolean hasEntryPointEdgesLocal(EntryPoint ep, Type type) {
		Objects.requireNonNull(ep);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			if(type.equals(Type.Both)) {
				return data.containsKey(ep);
			} else {
				Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> refToData = data.get(ep);
				if(refToData != null) {
					for(Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData : refToData.values()) {
						if(isExternalToData.getFirst().equals(type))
							return true;
					}
				}
				return true;
			}
		} finally {
			rwlock.readLock().unlock();
		}
	}
	
	@Override
	public boolean hasEntryPointEdgesForStmt(EntryPoint ep, Unit unit) {
		return hasEntryPointEdgesForStmtLocal(ep, unit, Type.Both);
	}
	
	@Override
	public boolean hasInternalEntryPointEdgesForStmt(EntryPoint ep, Unit unit) {
		return hasEntryPointEdgesForStmtLocal(ep, unit, Type.Internal);
	}
	
	@Override
	public boolean hasExternalEntryPointEdgesForStmt(EntryPoint ep, Unit unit) {
		return hasEntryPointEdgesForStmtLocal(ep, unit, Type.External);
	}
	
	private boolean hasEntryPointEdgesForStmtLocal(EntryPoint ep, Unit unit, Type type) {
		Objects.requireNonNull(ep);
		Objects.requireNonNull(unit);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> refToData = data.get(ep);
			if(refToData != null) {
				for(Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData : refToData.values()) {
					if(type.equals(Type.Both) || type.equals(isExternalToData.getFirst())) {
						for(Pair<Set<Unit>, Set<Integer>> unitsAndDepth : isExternalToData.getSecond().values()) {
							if(unitsAndDepth.getFirst().contains(unit))
								return true;
						}
					}
				}
			}
			return false;
		} finally {
			rwlock.readLock().unlock();
		}
	}
	
	public boolean hasEntryPointEdgesForStmt(Unit unit) {
		return hasEntryPointEdgesForStmtLocal(unit, Type.Both);
	}
	
	public boolean hasInternalEntryPointEdgesForStmt(Unit unit) {
		return hasEntryPointEdgesForStmtLocal(unit, Type.Internal);
	}
	
	public boolean hasExternalEntryPointEdgesForStmt(Unit unit) {
		return hasEntryPointEdgesForStmtLocal(unit, Type.External);
	}
	
	private boolean hasEntryPointEdgesForStmtLocal(Unit unit, Type type) {
		Objects.requireNonNull(unit);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			for(Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> refToData : data.values()) {
				for(Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData : refToData.values()) {
					if(type.equals(Type.Both) || type.equals(isExternalToData.getFirst())) {
						for(Pair<Set<Unit>, Set<Integer>> unitsAndDepth : isExternalToData.getSecond().values()) {
							if(unitsAndDepth.getFirst().contains(unit))
								return true;
						}
					}
				}
			}
			return false;
		} finally {
			rwlock.readLock().unlock();
		}
	}
	
	@Override
	public boolean hasEntryPointEdgesForSource(EntryPoint ep, SootMethod source) {
		return hasEntryPointEdgesForSourceLocal(ep, source, Type.Both);
	}
	
	@Override
	public boolean hasInternalEntryPointEdgesForSource(EntryPoint ep, SootMethod source) {
		return hasEntryPointEdgesForSourceLocal(ep, source, Type.Internal);
	}
	
	@Override
	public boolean hasExtermalEntryPointEdgesForSource(EntryPoint ep, SootMethod source) {
		return hasEntryPointEdgesForSourceLocal(ep, source, Type.External);
	}
	
	private boolean hasEntryPointEdgesForSourceLocal(EntryPoint ep, SootMethod source, Type type) {
		Objects.requireNonNull(ep);
		Objects.requireNonNull(source);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> refToData = data.get(ep);
			if(refToData != null) {
				for(Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData : refToData.values()) {
					if((type.equals(Type.Both) || type.equals(isExternalToData.getFirst())) 
							&& isExternalToData.getSecond().containsKey(source))
						return true;
				}
			}
			return false;
		} finally {
			rwlock.readLock().unlock();
		}
	}
	
	@Override
	public boolean hasEntryPointEdgesForSource(SootMethod source) {
		return hasEntryPointEdgesForSourceLocal(source, Type.Both);
	}
	
	@Override
	public boolean hasInternalEntryPointEdgesForSource(SootMethod source) {
		return hasEntryPointEdgesForSourceLocal(source, Type.Internal);
	}
	
	@Override
	public boolean hasExtermalEntryPointEdgesForSource(SootMethod source) {
		return hasEntryPointEdgesForSourceLocal(source, Type.External);
	}
	
	private boolean hasEntryPointEdgesForSourceLocal(SootMethod source, Type type) {
		Objects.requireNonNull(source);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			for(Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> refToData : data.values()) {
				for(Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData : refToData.values()) {
					if((type.equals(Type.Both) || type.equals(isExternalToData.getFirst())) 
							&& isExternalToData.getSecond().containsKey(source))
						return true;
				}
			}
			return false;
		} finally {
			rwlock.readLock().unlock();
		}
	}
	
	@Override
	public boolean hasEntryPointEdge(EntryPoint ep, EntryPoint referenced) {
		return hasEntryPointEdgeLocal(ep, referenced, Type.Both);
	}
	
	@Override
	public boolean hasInternalEntryPointEdge(EntryPoint ep, EntryPoint referenced) {
		return hasEntryPointEdgeLocal(ep, referenced, Type.Internal);
	}
	
	@Override
	public boolean hasExternalEntryPointEdge(EntryPoint ep, EntryPoint referenced) {
		return hasEntryPointEdgeLocal(ep, referenced, Type.External);
	}
	
	private boolean hasEntryPointEdgeLocal(EntryPoint ep, EntryPoint referenced, Type type) {
		Objects.requireNonNull(ep);
		Objects.requireNonNull(referenced);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> refToData = data.get(ep);
			if(refToData != null) {
				if(type.equals(Type.Both)) {
					return refToData.containsKey(referenced);
				} else {
					Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData = refToData.get(referenced);
					return isExternalToData != null ? isExternalToData.getFirst().equals(type) : false;
				}
			}
			return false;
		} finally {
			rwlock.readLock().unlock();
		}
	}
	
	@Override
	public boolean hasEntryPointEdge(EntryPoint referenced) {
		return hasEntryPointEdgeLocal(referenced, Type.Both);
	}
	
	@Override
	public boolean hasInternalEntryPointEdge(EntryPoint referenced) {
		return hasEntryPointEdgeLocal(referenced, Type.Internal);
	}
	
	@Override
	public boolean hasExternalEntryPointEdge(EntryPoint referenced) {
		return hasEntryPointEdgeLocal(referenced, Type.External);
	}
	
	private boolean hasEntryPointEdgeLocal(EntryPoint referenced, Type type) {
		Objects.requireNonNull(referenced);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			for(Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> refToData : data.values()) {
				Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData = refToData.get(referenced);
				if(isExternalToData != null) {
					if(type.equals(Type.Both)) {
						return true;
					} else if(isExternalToData.getFirst().equals(type)) {
						return true;
					}
				}
			}
			return false;
		} finally {
			rwlock.readLock().unlock();
		}
	}
	
	@Override
	public Set<EntryPoint> getEntryPointEdges(EntryPoint ep) {
		return getEntryPointEdgesLocal(ep, Type.Both);
	}
	
	@Override
	public Set<EntryPoint> getInternalEntryPointEdges(EntryPoint ep) {
		return getEntryPointEdgesLocal(ep, Type.Internal);
	}
	
	@Override
	public Set<EntryPoint> getExternalEntryPointEdges(EntryPoint ep) {
		return getEntryPointEdgesLocal(ep, Type.External);
	}
	
	private Set<EntryPoint> getEntryPointEdgesLocal(EntryPoint ep, Type type) {
		Objects.requireNonNull(ep);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		Set<EntryPoint> ret = new HashSet<>();
		try {
			Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> refToData = data.get(ep);
			if(refToData != null) {
				if(type.equals(Type.Both)) {
					ret.addAll(refToData.keySet());
				} else {
					for(EntryPoint reference : refToData.keySet()) {
						if(refToData.get(reference).getFirst().equals(type))
							ret.add(reference);
					}
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		return SortingMethods.sortSet(ret);
	}
	
	@Override
	public Set<EntryPoint> getEntryPointEdges() {
		return getEntryPointEdgesLocal(Type.Both);
	}
	
	@Override
	public Set<EntryPoint> getInternalEntryPointEdges() {
		return getEntryPointEdgesLocal(Type.Internal);
	}
	
	@Override
	public Set<EntryPoint> getExternalEntryPointEdges() {
		return getEntryPointEdgesLocal(Type.External);
	}
	
	private Set<EntryPoint> getEntryPointEdgesLocal(Type type) {
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		Set<EntryPoint> ret = new HashSet<>();
		try {
			for(Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> refToData : data.values()) {
				if(type.equals(Type.Both)) {
					ret.addAll(refToData.keySet());
				} else {
					for(EntryPoint reference : refToData.keySet()) {
						if(refToData.get(reference).getFirst().equals(type))
							ret.add(reference);
					}
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		return SortingMethods.sortSet(ret);
	}
	
	@Override
	public Set<EntryPoint> getEntryPointEdgesForStmt(EntryPoint ep, Unit unit) {
		return getEntryPointEdgesForStmtLocal(ep, unit, Type.Both);
	}
	
	@Override
	public Set<EntryPoint> getInternalEntryPointEdgesForStmt(EntryPoint ep, Unit unit) {
		return getEntryPointEdgesForStmtLocal(ep, unit, Type.Internal);
	}
	
	@Override
	public Set<EntryPoint> getExternalEntryPointEdgesForStmt(EntryPoint ep, Unit unit) {
		return getEntryPointEdgesForStmtLocal(ep, unit, Type.External);
	}
	
	private Set<EntryPoint> getEntryPointEdgesForStmtLocal(EntryPoint ep, Unit unit, Type type) {
		Objects.requireNonNull(ep);
		Objects.requireNonNull(unit);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		Set<EntryPoint> ret = new HashSet<>();
		try {
			Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> retToData = data.get(ep);
			if(retToData != null) {
				for(EntryPoint ieep : retToData.keySet()) {
					Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData = retToData.get(ieep);
					if(type.equals(Type.Both) || type.equals(isExternalToData.getFirst())) {
						for(Pair<Set<Unit>, Set<Integer>> unitsAndDepth : isExternalToData.getSecond().values()) {
							if(unitsAndDepth.getFirst().contains(unit)) {
								ret.add(ieep);
								break;
							}
						}
					}
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		return SortingMethods.sortSet(ret);
	}
	
	@Override
	public Set<EntryPoint> getEntryPointEdgesForStmt(Unit unit) {
		return getEntryPointEdgesForStmtLocal(unit, Type.Both);
	}
	
	@Override
	public Set<EntryPoint> getInternalEntryPointEdgesForStmt(Unit unit) {
		return getEntryPointEdgesForStmtLocal(unit, Type.Internal);
	}
	
	@Override
	public Set<EntryPoint> getExternalEntryPointEdgesForStmt(Unit unit) {
		return getEntryPointEdgesForStmtLocal(unit, Type.External);
	}
	
	private Set<EntryPoint> getEntryPointEdgesForStmtLocal(Unit unit, Type type) {
		Objects.requireNonNull(unit);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		Set<EntryPoint> ret = new HashSet<>();
		try {
			for(Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> retToData : data.values()) {
				for(EntryPoint ieep : retToData.keySet()) {
					Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData = retToData.get(ieep);
					if(type.equals(Type.Both) || type.equals(isExternalToData.getFirst())) {
						for(Pair<Set<Unit>, Set<Integer>> unitsAndDepth : isExternalToData.getSecond().values()) {
							if(unitsAndDepth.getFirst().contains(unit)) {
								ret.add(ieep);
								break;
							}
						}
					}
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		return SortingMethods.sortSet(ret);
	}
	
	@Override
	public Set<EntryPoint> getEntryPointEdgesForSource(EntryPoint ep, SootMethod source) {
		return getEntryPointEdgesForSourceLocal(ep, source, Type.Both);
	}
	
	@Override
	public Set<EntryPoint> getInternalEntryPointEdgesForSource(EntryPoint ep, SootMethod source) {
		return getEntryPointEdgesForSourceLocal(ep, source, Type.Internal);
	}
	
	@Override
	public Set<EntryPoint> getExternalEntryPointEdgesForSource(EntryPoint ep, SootMethod source) {
		return getEntryPointEdgesForSourceLocal(ep, source, Type.External);
	}

	private Set<EntryPoint> getEntryPointEdgesForSourceLocal(EntryPoint ep, SootMethod source, Type type) {
		Objects.requireNonNull(ep);
		Objects.requireNonNull(source);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		Set<EntryPoint> ret = new HashSet<>();
		try {
			Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> retToData = data.get(ep);
			if(retToData != null) {
				for(EntryPoint ieep : retToData.keySet()) {
					Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData = retToData.get(ieep);
					if((type.equals(Type.Both) || type.equals(isExternalToData.getFirst())) 
							&& isExternalToData.getSecond().containsKey(source))
						ret.add(ieep);
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		return SortingMethods.sortSet(ret);
	}
	
	@Override
	public Set<EntryPoint> getEntryPointEdgesForSource(SootMethod source) {
		return getEntryPointEdgesForSourceLocal(source, Type.Both);
	}
	
	@Override
	public Set<EntryPoint> getInternalEntryPointEdgesForSource(SootMethod source) {
		return getEntryPointEdgesForSourceLocal(source, Type.Internal);
	}
	
	@Override
	public Set<EntryPoint> getExternalEntryPointEdgesForSource(SootMethod source) {
		return getEntryPointEdgesForSourceLocal(source, Type.External);
	}

	private Set<EntryPoint> getEntryPointEdgesForSourceLocal(SootMethod source, Type type) {
		Objects.requireNonNull(source);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		Set<EntryPoint> ret = new HashSet<>();
		try {
			for(Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> retToData : data.values()) {
				for(EntryPoint ieep : retToData.keySet()) {
					Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData = retToData.get(ieep);
					if((type.equals(Type.Both) || type.equals(isExternalToData.getFirst())) 
							&& isExternalToData.getSecond().containsKey(source))
						ret.add(ieep);
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		return SortingMethods.sortSet(ret);
	}
	
	@Override
	public Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getSourcesWithUnitsAndDepth(EntryPoint ep) {
		return getSourcesWithUnitsAndDepthLocal(ep, Type.Both);
	}
	
	@Override
	public Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getInternalSourcesWithUnitsAndDepth(EntryPoint ep) {
		return getSourcesWithUnitsAndDepthLocal(ep, Type.Internal);
	}
	
	@Override
	public Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getExternalSourcesWithUnitsAndDepth(EntryPoint ep) {
		return getSourcesWithUnitsAndDepthLocal(ep, Type.External);
	}
	
	private Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getSourcesWithUnitsAndDepthLocal(EntryPoint ep, Type type) {
		Objects.requireNonNull(ep);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> ret = new HashMap<>();
		try {
			Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> retToData = data.get(ep);
			if(retToData != null) {
				for(Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData : retToData.values()) {
					if(type.equals(Type.Both) || type.equals(isExternalToData.getFirst())) {
						Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> sourcesToData = isExternalToData.getSecond();
						for(SootMethod source : sourcesToData.keySet()) {
							Pair<Set<Unit>, Set<Integer>> toAdd = sourcesToData.get(source);
							Pair<Set<Unit>, Set<Integer>> p = ret.get(source);
							if(p == null) {
								p = new Pair<Set<Unit>, Set<Integer>>(new HashSet<>(), new HashSet<>());
								ret.put(source, p);
							}
							p.getFirst().addAll(toAdd.getFirst());
							p.getSecond().addAll(toAdd.getSecond());
						}
					}
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		for(SootMethod source : ret.keySet()) {
			Pair<Set<Unit>, Set<Integer>> p = ret.get(source);
			ret.put(source, new Pair<>(SortingMethods.sortSet(p.getFirst(),SootSort.unitComp),SortingMethods.sortSet(p.getSecond())));
		}
		return SortingMethods.sortMapKey(ret, SootSort.smComp);
	}
	
	@Override
	public Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getSourcesWithUnitsAndDepth() {
		return getSourcesWithUnitsAndDepthLocal(Type.Both);
	}
	
	@Override
	public Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getInternalSourcesWithUnitsAndDepth() {
		return getSourcesWithUnitsAndDepthLocal(Type.Internal);
	}
	
	@Override
	public Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getExternalSourcesWithUnitsAndDepth() {
		return getSourcesWithUnitsAndDepthLocal(Type.External);
	}
	
	private Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getSourcesWithUnitsAndDepthLocal(Type type) {
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> ret = new HashMap<>();
		try {
			for(Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> retToData : data.values()) {
				for(Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData : retToData.values()) {
					if(type.equals(Type.Both) || type.equals(isExternalToData.getFirst())) {
						Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> sourcesToData = isExternalToData.getSecond();
						for(SootMethod source : sourcesToData.keySet()) {
							Pair<Set<Unit>, Set<Integer>> toAdd = sourcesToData.get(source);
							Pair<Set<Unit>, Set<Integer>> p = ret.get(source);
							if(p == null) {
								p = new Pair<Set<Unit>, Set<Integer>>(new HashSet<>(), new HashSet<>());
								ret.put(source, p);
							}
							p.getFirst().addAll(toAdd.getFirst());
							p.getSecond().addAll(toAdd.getSecond());
						}
					}
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		for(SootMethod source : ret.keySet()) {
			Pair<Set<Unit>, Set<Integer>> p = ret.get(source);
			ret.put(source, new Pair<>(SortingMethods.sortSet(p.getFirst(),SootSort.unitComp),SortingMethods.sortSet(p.getSecond())));
		}
		return SortingMethods.sortMapKey(ret, SootSort.smComp);
	}
	
	@Override
	public Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getSourcesWithUnitsAndDepthForReference(EntryPoint ep, EntryPoint referenced) {
		return getSourcesWithUnitsAndDepthForReferenceLocal(ep, referenced, Type.Both);
	}
	
	@Override
	public Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getInternalSourcesWithUnitsAndDepthForReference(EntryPoint ep, EntryPoint referenced) {
		return getSourcesWithUnitsAndDepthForReferenceLocal(ep, referenced, Type.Internal);
	}
	
	@Override
	public Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getExternalSourcesWithUnitsAndDepthForReference(EntryPoint ep, EntryPoint referenced) {
		return getSourcesWithUnitsAndDepthForReferenceLocal(ep, referenced, Type.External);
	}
	
	private Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getSourcesWithUnitsAndDepthForReferenceLocal(EntryPoint ep, EntryPoint referenced, Type type) {
		Objects.requireNonNull(ep);
		Objects.requireNonNull(referenced);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> ret = new HashMap<>();
		try {
			Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> refToData = data.get(ep);
			if(refToData != null) {
				Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData = refToData.get(referenced);
				if(isExternalToData != null && (type.equals(Type.Both) || type.equals(isExternalToData.getFirst()))) {
					Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> sourcesToData = isExternalToData.getSecond();
					for(SootMethod source : sourcesToData.keySet()) {
						Pair<Set<Unit>, Set<Integer>> toAdd = sourcesToData.get(source);
						Pair<Set<Unit>, Set<Integer>> p = ret.get(source);
						if(p == null) {
							p = new Pair<Set<Unit>, Set<Integer>>(new HashSet<>(), new HashSet<>());
							ret.put(source, p);
						}
						p.getFirst().addAll(toAdd.getFirst());
						p.getSecond().addAll(toAdd.getSecond());
					}
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		for(SootMethod source : ret.keySet()) {
			Pair<Set<Unit>, Set<Integer>> p = ret.get(source);
			ret.put(source, new Pair<>(SortingMethods.sortSet(p.getFirst(),SootSort.unitComp),SortingMethods.sortSet(p.getSecond())));
		}
		return SortingMethods.sortMapKey(ret, SootSort.smComp);
	}
	
	@Override
	public Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getSourcesWithUnitsAndDepthForReference(EntryPoint referenced) {
		return getSourcesWithUnitsAndDepthForReferenceLocal(referenced, Type.Both);
	}
	
	@Override
	public Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getInternalSourcesWithUnitsAndDepthForReference(EntryPoint referenced) {
		return getSourcesWithUnitsAndDepthForReferenceLocal(referenced, Type.Internal);
	}
	
	@Override
	public Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getExternalSourcesWithUnitsAndDepthForReference(EntryPoint referenced) {
		return getSourcesWithUnitsAndDepthForReferenceLocal(referenced, Type.External);
	}
	
	private Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> getSourcesWithUnitsAndDepthForReferenceLocal(EntryPoint referenced, Type type) {
		Objects.requireNonNull(referenced);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> ret = new HashMap<>();
		try {
			for(Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> refToData : data.values()) {
				Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData = refToData.get(referenced);
				if(isExternalToData != null && (type.equals(Type.Both) || type.equals(isExternalToData.getFirst()))) {
					Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> sourcesToData = isExternalToData.getSecond();
					for(SootMethod source : sourcesToData.keySet()) {
						Pair<Set<Unit>, Set<Integer>> toAdd = sourcesToData.get(source);
						Pair<Set<Unit>, Set<Integer>> p = ret.get(source);
						if(p == null) {
							p = new Pair<Set<Unit>, Set<Integer>>(new HashSet<>(), new HashSet<>());
							ret.put(source, p);
						}
						p.getFirst().addAll(toAdd.getFirst());
						p.getSecond().addAll(toAdd.getSecond());
					}
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		for(SootMethod source : ret.keySet()) {
			Pair<Set<Unit>, Set<Integer>> p = ret.get(source);
			ret.put(source, new Pair<>(SortingMethods.sortSet(p.getFirst(),SootSort.unitComp),SortingMethods.sortSet(p.getSecond())));
		}
		return SortingMethods.sortMapKey(ret, SootSort.smComp);
	}
	
	@Override
	public Map<Unit, Set<Integer>> getUnitsWithDepth(EntryPoint ep) {
		return getUnitsWithDepthLocal(ep, Type.Both);
	}
	
	@Override
	public Map<Unit, Set<Integer>> getInternalUnitsWithDepth(EntryPoint ep) {
		return getUnitsWithDepthLocal(ep, Type.Internal);
	}
	
	@Override
	public Map<Unit, Set<Integer>> getExternalUnitsWithDepth(EntryPoint ep) {
		return getUnitsWithDepthLocal(ep, Type.External);
	}
	
	private Map<Unit, Set<Integer>> getUnitsWithDepthLocal(EntryPoint ep, Type type) {
		Objects.requireNonNull(ep);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		Map<Unit, Set<Integer>> ret = new HashMap<>();
		try {
			Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> refToData = data.get(ep);
			if(refToData != null) {
				for(Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData : refToData.values()) {
					if(type.equals(Type.Both) || type.equals(isExternalToData.getFirst())) {
						Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> sourcesToData = isExternalToData.getSecond();
						for(Pair<Set<Unit>, Set<Integer>> unitsAndDepths : sourcesToData.values()) {
							for(Unit u : unitsAndDepths.getFirst()) {
								Set<Integer> depths = ret.get(u);
								if(depths == null) {
									depths = new HashSet<Integer>();
									ret.put(u, depths);
								}
								depths.addAll(unitsAndDepths.getSecond());
							}
						}
					}
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		for(Unit u : ret.keySet()) {
			Set<Integer> depths = ret.get(u);
			ret.put(u, SortingMethods.sortSet(depths));
		}
		return SortingMethods.sortMapKey(ret,SootSort.unitComp);
	}
	
	@Override
	public Map<Unit, Set<Integer>> getUnitsWithDepth() {
		return getUnitsWithDepthLocal(Type.Both);
	}
	
	@Override
	public Map<Unit, Set<Integer>> getInternalUnitsWithDepth() {
		return getUnitsWithDepthLocal(Type.Internal);
	}
	
	@Override
	public Map<Unit, Set<Integer>> getExternalUnitsWithDepth() {
		return getUnitsWithDepthLocal(Type.External);
	}
	
	private Map<Unit, Set<Integer>> getUnitsWithDepthLocal(Type type) {
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		Map<Unit, Set<Integer>> ret = new HashMap<>();
		try {
			for(Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> refToData : data.values()) {
				for(Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData : refToData.values()) {
					if(type.equals(Type.Both) || type.equals(isExternalToData.getFirst())) {
						Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> sourcesToData = isExternalToData.getSecond();
						for(Pair<Set<Unit>, Set<Integer>> unitsAndDepths : sourcesToData.values()) {
							for(Unit u : unitsAndDepths.getFirst()) {
								Set<Integer> depths = ret.get(u);
								if(depths == null) {
									depths = new HashSet<Integer>();
									ret.put(u, depths);
								}
								depths.addAll(unitsAndDepths.getSecond());
							}
						}
					}
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		for(Unit u : ret.keySet()) {
			Set<Integer> depths = ret.get(u);
			ret.put(u, SortingMethods.sortSet(depths));
		}
		return SortingMethods.sortMapKey(ret,SootSort.unitComp);
	}
	
	@Override
	public Map<Unit, Set<Integer>> getUnitsWithDepthForReference(EntryPoint ep, EntryPoint referenced) {
		return getUnitsWithDepthForReferenceLocal(ep, referenced, Type.Both);
	}
	
	@Override
	public Map<Unit, Set<Integer>> getInternalUnitsWithDepthForReference(EntryPoint ep, EntryPoint referenced) {
		return getUnitsWithDepthForReferenceLocal(ep, referenced, Type.Internal);
	}
	
	@Override
	public Map<Unit, Set<Integer>> getExternalUnitsWithDepthForReference(EntryPoint ep, EntryPoint referenced) {
		return getUnitsWithDepthForReferenceLocal(ep, referenced, Type.External);
	}
	
	private Map<Unit, Set<Integer>> getUnitsWithDepthForReferenceLocal(EntryPoint ep, EntryPoint referenced, Type type) {
		Objects.requireNonNull(ep);
		Objects.requireNonNull(referenced);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		Map<Unit, Set<Integer>> ret = new HashMap<>();
		try {
			Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> refToData = data.get(ep);
			if(refToData != null) {
				Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData = refToData.get(referenced);
				if(isExternalToData != null && (type.equals(Type.Both) || type.equals(isExternalToData.getFirst()))) {
					Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> sourcesToData = isExternalToData.getSecond();
					for(Pair<Set<Unit>, Set<Integer>> unitsAndDepths : sourcesToData.values()) {
						for(Unit u : unitsAndDepths.getFirst()) {
							Set<Integer> depths = ret.get(u);
							if(depths == null) {
								depths = new HashSet<Integer>();
								ret.put(u, depths);
							}
							depths.addAll(unitsAndDepths.getSecond());
						}
					}
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		for(Unit u : ret.keySet()) {
			Set<Integer> depths = ret.get(u);
			ret.put(u, SortingMethods.sortSet(depths));
		}
		return SortingMethods.sortMapKey(ret,SootSort.unitComp);
	}
	
	@Override
	public Map<Unit, Set<Integer>> getUnitsWithDepthForReference(EntryPoint referenced) {
		return getUnitsWithDepthForReferenceLocal(referenced, Type.Both);
	}
	
	@Override
	public Map<Unit, Set<Integer>> getInternalUnitsWithDepthForReference(EntryPoint referenced) {
		return getUnitsWithDepthForReferenceLocal(referenced, Type.Internal);
	}
	
	@Override
	public Map<Unit, Set<Integer>> getExternalUnitsWithDepthForReference(EntryPoint referenced) {
		return getUnitsWithDepthForReferenceLocal(referenced, Type.External);
	}
	
	private Map<Unit, Set<Integer>> getUnitsWithDepthForReferenceLocal(EntryPoint referenced, Type type) {
		Objects.requireNonNull(referenced);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		Map<Unit, Set<Integer>> ret = new HashMap<>();
		try {
			for(Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> refToData : data.values()) {
				Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData = refToData.get(referenced);
				if(isExternalToData != null && (type.equals(Type.Both) || type.equals(isExternalToData.getFirst()))) {
					Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> sourcesToData = isExternalToData.getSecond();
					for(Pair<Set<Unit>, Set<Integer>> unitsAndDepths : sourcesToData.values()) {
						for(Unit u : unitsAndDepths.getFirst()) {
							Set<Integer> depths = ret.get(u);
							if(depths == null) {
								depths = new HashSet<Integer>();
								ret.put(u, depths);
							}
							depths.addAll(unitsAndDepths.getSecond());
						}
					}
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		for(Unit u : ret.keySet()) {
			Set<Integer> depths = ret.get(u);
			ret.put(u, SortingMethods.sortSet(depths));
		}
		return SortingMethods.sortMapKey(ret,SootSort.unitComp);
	}
	
	@Override
	public Set<SootMethod> getSources(EntryPoint ep) {
		return getSourcesLocal(ep, Type.Both);
	}
	
	@Override
	public Set<SootMethod> getInternalSources(EntryPoint ep) {
		return getSourcesLocal(ep, Type.Internal);
	}
	
	@Override
	public Set<SootMethod> getExternalSources(EntryPoint ep) {
		return getSourcesLocal(ep, Type.External);
	}
	
	private Set<SootMethod> getSourcesLocal(EntryPoint ep, Type type) {
		Objects.requireNonNull(ep);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		Set<SootMethod> ret = new HashSet<>();
		try {
			Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> retToData = data.get(ep);
			if(retToData != null) {
				for(Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData : retToData.values()) {
					if(type.equals(Type.Both) || type.equals(isExternalToData.getFirst()))
						ret.addAll(isExternalToData.getSecond().keySet());
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		return SortingMethods.sortSet(ret, SootSort.smComp);
	}
	
	@Override
	public Set<SootMethod> getSources() {
		return getSourcesLocal(Type.Both);
	}
	
	@Override
	public Set<SootMethod> getInternalSources() {
		return getSourcesLocal(Type.Internal);
	}
	
	@Override
	public Set<SootMethod> getExternalSources() {
		return getSourcesLocal(Type.External);
	}
	
	private Set<SootMethod> getSourcesLocal(Type type) {
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		Set<SootMethod> ret = new HashSet<>();
		try {
			for(Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> retToData : data.values()) {
				for(Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData : retToData.values()) {
					if(type.equals(Type.Both) || type.equals(isExternalToData.getFirst()))
						ret.addAll(isExternalToData.getSecond().keySet());
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		return SortingMethods.sortSet(ret, SootSort.smComp);
	}
	
	@Override
	public Set<SootMethod> getSourcesForReference(EntryPoint ep, EntryPoint referenced) {
		return getSourcesForReferenceLocal(ep, referenced, Type.Both);
	}
	
	@Override
	public Set<SootMethod> getInternalSourcesForReference(EntryPoint ep, EntryPoint referenced) {
		return getSourcesForReferenceLocal(ep, referenced, Type.Internal);
	}
	
	@Override
	public Set<SootMethod> getExternalSourcesForReference(EntryPoint ep, EntryPoint referenced) {
		return getSourcesForReferenceLocal(ep, referenced, Type.External);
	}
	
	private Set<SootMethod> getSourcesForReferenceLocal(EntryPoint ep, EntryPoint referenced, Type type) {
		Objects.requireNonNull(ep);
		Objects.requireNonNull(referenced);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		Set<SootMethod> ret = new HashSet<>();
		try {
			Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> refToData = data.get(ep);
			if(refToData != null) {
				Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData = refToData.get(referenced);
				if(isExternalToData != null && (type.equals(Type.Both) || type.equals(isExternalToData.getFirst()))) {
					ret.addAll(isExternalToData.getSecond().keySet());
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		return SortingMethods.sortSet(ret, SootSort.smComp);
	}
	
	@Override
	public Set<SootMethod> getSourcesForReference(EntryPoint referenced) {
		return getSourcesForReferenceLocal(referenced, Type.Both);
	}
	
	@Override
	public Set<SootMethod> getInternalSourcesForReference(EntryPoint referenced) {
		return getSourcesForReferenceLocal(referenced, Type.Internal);
	}
	
	@Override
	public Set<SootMethod> getExternalSourcesForReference(EntryPoint referenced) {
		return getSourcesForReferenceLocal(referenced, Type.External);
	}
	
	private Set<SootMethod> getSourcesForReferenceLocal(EntryPoint referenced, Type type) {
		Objects.requireNonNull(referenced);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		Set<SootMethod> ret = new HashSet<>();
		try {
			for(Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> refToData : data.values()) {
				Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData = refToData.get(referenced);
				if(isExternalToData != null && (type.equals(Type.Both) || type.equals(isExternalToData.getFirst()))) {
					ret.addAll(isExternalToData.getSecond().keySet());
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		return SortingMethods.sortSet(ret, SootSort.smComp);
	}
	
	@Override
	public Set<Unit> getUnits(EntryPoint ep) {
		return getUnitsLocal(ep, Type.Both);
	}

	@Override
	public Set<Unit> getInternalUnits(EntryPoint ep) {
		return getUnitsLocal(ep, Type.Internal);
	}

	@Override
	public Set<Unit> getExternalUnits(EntryPoint ep) {
		return getUnitsLocal(ep, Type.External);
	}
	
	private Set<Unit> getUnitsLocal(EntryPoint ep, Type type) {
		Objects.requireNonNull(ep);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		Set<Unit> ret = new HashSet<>();
		try {
			Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> refToData = data.get(ep);
			if(refToData != null) {
				for(Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData : refToData.values()) {
					if(type.equals(Type.Both) || type.equals(isExternalToData.getFirst())) {
						Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> sourcesToData = isExternalToData.getSecond();
						for(Pair<Set<Unit>, Set<Integer>> unitsAndDepths : sourcesToData.values()) {
							ret.addAll(unitsAndDepths.getFirst());
						}
					}
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		return SortingMethods.sortSet(ret,SootSort.unitComp);
	}

	@Override
	public Set<Unit> getUnits() {
		return getUnitsLocal(Type.Both);
	}

	@Override
	public Set<Unit> getInternalUnits() {
		return getUnitsLocal(Type.Internal);
	}

	@Override
	public Set<Unit> getExternalUnits() {
		return getUnitsLocal(Type.External);
	}
	
	private Set<Unit> getUnitsLocal(Type type) {
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		Set<Unit> ret = new HashSet<>();
		try {
			for(Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> refToData : data.values()) {
				for(Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData : refToData.values()) {
					if(type.equals(Type.Both) || type.equals(isExternalToData.getFirst())) {
						Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> sourcesToData = isExternalToData.getSecond();
						for(Pair<Set<Unit>, Set<Integer>> unitsAndDepths : sourcesToData.values()) {
							ret.addAll(unitsAndDepths.getFirst());
						}
					}
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		return SortingMethods.sortSet(ret,SootSort.unitComp);
	}

	@Override
	public Set<Unit> getUnitsForReference(EntryPoint ep, EntryPoint referenced) {
		return getUnitsForReferenceLocal(ep, referenced, Type.Both);
	}

	@Override
	public Set<Unit> getInternalUnitsForReference(EntryPoint ep, EntryPoint referenced) {
		return getUnitsForReferenceLocal(ep, referenced, Type.Internal);
	}

	@Override
	public Set<Unit> getExternalUnitsForReference(EntryPoint ep, EntryPoint referenced) {
		return getUnitsForReferenceLocal(ep, referenced, Type.External);
	}
	
	private Set<Unit> getUnitsForReferenceLocal(EntryPoint ep, EntryPoint referenced, Type type) {
		Objects.requireNonNull(ep);
		Objects.requireNonNull(referenced);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		Set<Unit> ret = new HashSet<>();
		try {
			Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> refToData = data.get(ep);
			if(refToData != null) {
				Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData = refToData.get(referenced);
				if(isExternalToData != null && (type.equals(Type.Both) || type.equals(isExternalToData.getFirst()))) {
					Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> sourcesToData = isExternalToData.getSecond();
					for(Pair<Set<Unit>, Set<Integer>> unitsAndDepths : sourcesToData.values()) {
						ret.addAll(unitsAndDepths.getFirst());
					}
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		return SortingMethods.sortSet(ret,SootSort.unitComp);
	}

	@Override
	public Set<Unit> getUnitsForReference(EntryPoint referenced) {
		return getUnitsForReferenceLocal(referenced, Type.Both);
	}

	@Override
	public Set<Unit> getInternalUnitsForReference(EntryPoint referenced) {
		return getUnitsForReferenceLocal(referenced, Type.Internal);
	}

	@Override
	public Set<Unit> getExternalUnitsForReference(EntryPoint referenced) {
		return getUnitsForReferenceLocal(referenced, Type.External);
	}
	
	private Set<Unit> getUnitsForReferenceLocal(EntryPoint referenced, Type type) {
		Objects.requireNonNull(referenced);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		Set<Unit> ret = new HashSet<>();
		try {
			for(Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> refToData : data.values()) {
				Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData = refToData.get(referenced);
				if(isExternalToData != null && (type.equals(Type.Both) || type.equals(isExternalToData.getFirst()))) {
					Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> sourcesToData = isExternalToData.getSecond();
					for(Pair<Set<Unit>, Set<Integer>> unitsAndDepths : sourcesToData.values()) {
						ret.addAll(unitsAndDepths.getFirst());
					}
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		return SortingMethods.sortSet(ret,SootSort.unitComp);
	}
	
	public Map<SootMethod, Set<Integer>> getSourcesWithDepth(EntryPoint ep) {
		return getSourcesWithDepthLocal(ep, Type.Both);
	}
	
	public Map<SootMethod, Set<Integer>> getInternalSourcesWithDepth(EntryPoint ep) {
		return getSourcesWithDepthLocal(ep, Type.Internal);
	}
	
	public Map<SootMethod, Set<Integer>> getExternalSourcesWithDepth(EntryPoint ep) {
		return getSourcesWithDepthLocal(ep, Type.External);
	}
	
	private Map<SootMethod, Set<Integer>> getSourcesWithDepthLocal(EntryPoint ep, Type type) {
		Objects.requireNonNull(ep);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		Map<SootMethod, Set<Integer>> ret = new HashMap<>();
		try {
			Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> retToData = data.get(ep);
			if(retToData != null) {
				for(Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData : retToData.values()) {
					if(type.equals(Type.Both) || type.equals(isExternalToData.getFirst())) {
						Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> sourcesToData = isExternalToData.getSecond();
						for(SootMethod source : sourcesToData.keySet()){
							Pair<Set<Unit>,Set<Integer>> p = sourcesToData.get(source);
							Set<Integer> depths = ret.get(source);
							if(depths == null){
								depths = new HashSet<Integer>();
								ret.put(source, depths);
							}
							depths.addAll(p.getSecond());
						}
					}
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		for(SootMethod m : ret.keySet()) {
			Set<Integer> depths = ret.get(m);
			ret.put(m, SortingMethods.sortSet(depths));
		}
		return SortingMethods.sortMapKey(ret, SootSort.smComp);
	}
	
	public Map<SootMethod, Set<Integer>> getSourcesWithDepth() {
		return getSourcesWithDepthLocal(Type.Both);
	}
	
	public Map<SootMethod, Set<Integer>> getInternalSourcesWithDepth() {
		return getSourcesWithDepthLocal(Type.Internal);
	}
	
	public Map<SootMethod, Set<Integer>> getExternalSourcesWithDepth() {
		return getSourcesWithDepthLocal(Type.External);
	}
	
	private Map<SootMethod, Set<Integer>> getSourcesWithDepthLocal(Type type) {
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		Map<SootMethod, Set<Integer>> ret = new HashMap<>();
		try {
			for(Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> retToData : data.values()) {
				for(Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData : retToData.values()) {
					if(type.equals(Type.Both) || type.equals(isExternalToData.getFirst())) {
						Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> sourcesToData = isExternalToData.getSecond();
						for(SootMethod source : sourcesToData.keySet()){
							Pair<Set<Unit>,Set<Integer>> p = sourcesToData.get(source);
							Set<Integer> depths = ret.get(source);
							if(depths == null){
								depths = new HashSet<Integer>();
								ret.put(source, depths);
							}
							depths.addAll(p.getSecond());
						}
					}
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		for(SootMethod m : ret.keySet()) {
			Set<Integer> depths = ret.get(m);
			ret.put(m, SortingMethods.sortSet(depths));
		}
		return SortingMethods.sortMapKey(ret, SootSort.smComp);
	}
	
	public Map<SootMethod, Set<Integer>> getSourcesWithDepthForReference(EntryPoint ep, EntryPoint referenced) {
		return getSourcesWithDepthForReferenceLocal(ep, referenced, Type.Both);
	}
	
	public Map<SootMethod, Set<Integer>> getInternalSourcesWithDepthForReference(EntryPoint ep, EntryPoint referenced) {
		return getSourcesWithDepthForReferenceLocal(ep, referenced, Type.Internal);
	}
	
	public Map<SootMethod, Set<Integer>> getExternalSourcesWithDepthForReference(EntryPoint ep, EntryPoint referenced) {
		return getSourcesWithDepthForReferenceLocal(ep, referenced, Type.External);
	}
	
	private Map<SootMethod, Set<Integer>> getSourcesWithDepthForReferenceLocal(EntryPoint ep, EntryPoint referenced, Type type) {
		Objects.requireNonNull(ep);
		Objects.requireNonNull(referenced);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		Map<SootMethod, Set<Integer>> ret = new HashMap<>();
		try {
			Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> retToData = data.get(ep);
			if(retToData != null) {
				Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData = retToData.get(referenced);
				if(isExternalToData != null && (type.equals(Type.Both) || type.equals(isExternalToData.getFirst()))) {
					Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> sourcesToData = isExternalToData.getSecond();
					for(SootMethod source : sourcesToData.keySet()){
						Pair<Set<Unit>,Set<Integer>> p = sourcesToData.get(source);
						Set<Integer> depths = ret.get(source);
						if(depths == null){
							depths = new HashSet<Integer>();
							ret.put(source, depths);
						}
						depths.addAll(p.getSecond());
					}
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		for(SootMethod m : ret.keySet()) {
			Set<Integer> depths = ret.get(m);
			ret.put(m, SortingMethods.sortSet(depths));
		}
		return SortingMethods.sortMapKey(ret, SootSort.smComp);
	}
	
	public Map<SootMethod, Set<Integer>> getSourcesWithDepthForReference(EntryPoint referenced) {
		return getSourcesWithDepthForReferenceLocal(referenced, Type.Both);
	}
	
	public Map<SootMethod, Set<Integer>> getInternalSourcesWithDepthForReference(EntryPoint referenced) {
		return getSourcesWithDepthForReferenceLocal(referenced, Type.Internal);
	}
	
	public Map<SootMethod, Set<Integer>> getExternalSourcesWithDepthForReference(EntryPoint referenced) {
		return getSourcesWithDepthForReferenceLocal(referenced, Type.External);
	}
	
	private Map<SootMethod, Set<Integer>> getSourcesWithDepthForReferenceLocal(EntryPoint referenced, Type type) {
		Objects.requireNonNull(referenced);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		Map<SootMethod, Set<Integer>> ret = new HashMap<>();
		try {
			for(Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> retToData : data.values()) {
				Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData = retToData.get(referenced);
				if(isExternalToData != null && (type.equals(Type.Both) || type.equals(isExternalToData.getFirst()))) {
					Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> sourcesToData = isExternalToData.getSecond();
					for(SootMethod source : sourcesToData.keySet()){
						Pair<Set<Unit>,Set<Integer>> p = sourcesToData.get(source);
						Set<Integer> depths = ret.get(source);
						if(depths == null){
							depths = new HashSet<Integer>();
							ret.put(source, depths);
						}
						depths.addAll(p.getSecond());
					}
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		for(SootMethod m : ret.keySet()) {
			Set<Integer> depths = ret.get(m);
			ret.put(m, SortingMethods.sortSet(depths));
		}
		return SortingMethods.sortMapKey(ret, SootSort.smComp);
	}
	
	@Override
	public Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> getData(EntryPoint ep) {
		return getDataLocal(ep, Type.Both);
	}
	
	@Override
	public Map<EntryPoint, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> getInternalData(EntryPoint ep) {
		Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> temp = getDataLocal(ep, Type.Internal);
		Map<EntryPoint, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> ret = new LinkedHashMap<>();
		for(EntryPoint reference : temp.keySet()) {
			ret.put(reference, temp.get(reference).getSecond());
		}
		return ret;
	}
	
	@Override
	public Map<EntryPoint, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> getExternalData(EntryPoint ep) {
		Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> temp = getDataLocal(ep, Type.External);
		Map<EntryPoint, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> ret = new LinkedHashMap<>();
		for(EntryPoint reference : temp.keySet()) {
			ret.put(reference, temp.get(reference).getSecond());
		}
		return ret;
	}
	
	private Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> getDataLocal(EntryPoint ep, Type type) {
		Objects.requireNonNull(ep);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> ret = new HashMap<>();
		try {
			Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> refToData = data.get(ep);
			if(refToData != null) {
				for(EntryPoint referenced : refToData.keySet()) {
					Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData = refToData.get(referenced);
					if(type.equals(Type.Both) || type.equals(isExternalToData.getFirst())) {
						Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> sourcesToData = isExternalToData.getSecond();
						Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> newSourcesToData = new HashMap<>();
						for(SootMethod source : sourcesToData.keySet()) {
							Pair<Set<Unit>,Set<Integer>> p = sourcesToData.get(source);
							newSourcesToData.put(source, new Pair<>(SortingMethods.sortSet(p.getFirst(),
									SootSort.unitComp),SortingMethods.sortSet(p.getSecond())));
						}
						newSourcesToData = SortingMethods.sortMapKey(newSourcesToData, SootSort.smComp);
						ret.put(referenced,new Pair<>(isExternalToData.getFirst(),newSourcesToData));
					}	
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		return SortingMethods.sortMapKeyAscending(ret);
	}
	
	@Override
	public Map<EntryPoint, Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>>> getData() {
		return getDataLocal(Type.Both);
	}
	
	@Override
	public Map<EntryPoint, Map<EntryPoint, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> getInternalData() {
		Map<EntryPoint, Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>>> temp = getDataLocal(Type.Internal);
		Map<EntryPoint, Map<EntryPoint, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> ret = new LinkedHashMap<>();
		for(EntryPoint ep : temp.keySet()) {
			Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> refToData = temp.get(ep);
			Map<EntryPoint, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> newRet = new LinkedHashMap<>();
			for(EntryPoint reference : refToData.keySet()) {
				newRet.put(reference, refToData.get(reference).getSecond());
			}
			ret.put(ep, newRet);
		}
		return ret;
	}
	
	@Override
	public Map<EntryPoint, Map<EntryPoint, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> getExternalData() {
		Map<EntryPoint, Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>>> temp = getDataLocal(Type.External);
		Map<EntryPoint, Map<EntryPoint, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> ret = new LinkedHashMap<>();
		for(EntryPoint ep : temp.keySet()) {
			Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> refToData = temp.get(ep);
			Map<EntryPoint, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> newRet = new LinkedHashMap<>();
			for(EntryPoint reference : refToData.keySet()) {
				newRet.put(reference, refToData.get(reference).getSecond());
			}
			ret.put(ep, newRet);
		}
		return ret;
	}
	
	private Map<EntryPoint, Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>>> getDataLocal(Type type) {
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		Map<EntryPoint, Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>>> ret = new HashMap<>();
		try {
			for(EntryPoint ep : data.keySet()) {
				Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> refToData = data.get(ep);
				Map<EntryPoint, Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>>> newRefToData = new HashMap<>();
				for(EntryPoint referenced : refToData.keySet()) {
					Pair<Type, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> isExternalToData = refToData.get(referenced);
					if(type.equals(Type.Both) || type.equals(isExternalToData.getFirst())) {
						Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> sourcesToData = isExternalToData.getSecond();
						Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> newSourcesToData = new HashMap<>();
						for(SootMethod source : sourcesToData.keySet()) {
							Pair<Set<Unit>,Set<Integer>> p = sourcesToData.get(source);
							newSourcesToData.put(source, new Pair<>(SortingMethods.sortSet(p.getFirst(),
									SootSort.unitComp),SortingMethods.sortSet(p.getSecond())));
						}
						newSourcesToData = SortingMethods.sortMapKey(newSourcesToData, SootSort.smComp);
						newRefToData.put(referenced,new Pair<>(isExternalToData.getFirst(),newSourcesToData));
					}	
				}
				if(!newRefToData.isEmpty()) {
					newRefToData = SortingMethods.sortMapKeyAscending(newRefToData);
					ret.put(ep, newRefToData);
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		return SortingMethods.sortMapKeyAscending(ret);
	}
	
	@Override
	public Set<EntryPointContainer> getOutputData() {
		Set<EntryPointContainer> ret = new LinkedHashSet<>();
		rwlock.writeLock().lock(); //Prevent changes and because we may end up writing something
		try {
			//Need to write any changes to the loaded data back to output before copying
			writeSootResolvedDataWLocked(); //Will only write if loaded = true, otherwise nothing happens
			ret.addAll(output);
		} finally {
			rwlock.writeLock().unlock();
		}
		return ret;
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}
	
	@Override
	public EntryPointEdgesDatabase readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this,filePath, path);
	}
	
	public static EntryPointEdgesDatabase readXMLStatic(String filePath, Path path) throws Exception {
		return new EntryPointEdgesDatabase(false).readXML(filePath, path);
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
				EntryPointContainer.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(EntryPointEdgesDatabase.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}
	
}
