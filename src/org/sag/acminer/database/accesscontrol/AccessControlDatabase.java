package org.sag.acminer.database.accesscontrol;

import java.io.ObjectStreamException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.io.FileHashList;
import org.sag.common.tools.SortingMethods;
import org.sag.common.tuple.Pair;
import org.sag.soot.SootSort;
import org.sag.xstream.XStreamInOut;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import soot.SootMethod;
import soot.Unit;

@XStreamAlias("AccessControlDatabase")
public class AccessControlDatabase extends AbstractAccessControlDatabase {
	
	@XStreamOmitField
	private final ReadWriteLock rwlock;
	//Entry Point -> Source Method of units -> units + depths of source method
	@XStreamOmitField
	private volatile Map<EntryPoint, Map<SootMethod, Pair<Set<Unit>,Set<Integer>>>> data;
	@XStreamOmitField
	private volatile boolean loaded;
	
	/* All data is loaded when the classes is loaded from xml. If the loaded data is
	 * reset, the data is written to the load structure first. Then is is reloaded
	 * whenever an operation is performed.
	 */
	public AccessControlDatabase(String name, String type, boolean newDB) {
		super(name,type,null);
		if(newDB) {
			data = new HashMap<>();
			loaded = true;
		} else {
			data = null;
			loaded = false;
		}
		rwlock = new ReentrantReadWriteLock();
	}
	
	//ReadResolve is always run when reading from XML even if a constructor is run first
	protected Object readResolve() throws ObjectStreamException {
		loadSootResolvedData();
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
			Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> sourceToUnit = data.get(ep);
			for(SootMethod source : sourceToUnit.keySet()) {
				Pair<Set<Unit>,Set<Integer>> units = sourceToUnit.get(source);
				units = new Pair<Set<Unit>,Set<Integer>>(SortingMethods.sortSet(units.getFirst(),SootSort.unitComp),
						SortingMethods.sortSet(units.getSecond()));
				sourceToUnit.put(source, units);
			}
			sourceToUnit = SortingMethods.sortMapKey(sourceToUnit, SootSort.smComp);
			data.put(ep, sourceToUnit);
		}
		data = SortingMethods.sortMapKeyAscending(data);
	}
	
	@Override
	public void resetSootResolvedData() {
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
			for(EntryPoint ep : data.keySet()) {
				Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> sourceToUnit = data.get(ep);
				output.add(new EntryPointContainer(ep,sourceToUnit));
			}
		}
	}
	
	private void loadSootResolvedDataWLocked() {
		if(!loaded) {
			data = new HashMap<>();
			//Note this will fail with an exception if the data cannot be loaded 
			//as this should always be the same or be recreated 
			for(EntryPointContainer epContainer : output) {
				data.put(new EntryPoint(epContainer.getEntryPoint(), epContainer.getStub()),epContainer.getData());
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
	public void add(EntryPoint ep, Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> dataToAdd){
		rwlock.writeLock().lock();
		try {
			loadSootResolvedDataWLocked();
			addInner(ep,dataToAdd);
		} finally {
			rwlock.writeLock().unlock();
		}
	}
	
	@Override
	public void addAll(Map<EntryPoint, Map<SootMethod, Pair<Set<Unit>,Set<Integer>>>> dataToAdd) {
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
	
	private void addInner(EntryPoint ep, Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> dataToAdd) {
		Objects.requireNonNull(ep);
		Objects.requireNonNull(dataToAdd);
		if(!dataToAdd.isEmpty()) {
			Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> sourceToUnitMap = data.get(ep);
			if(sourceToUnitMap == null) {
				sourceToUnitMap = new HashMap<>();
				data.put(ep, sourceToUnitMap);
			}
			for(SootMethod source : dataToAdd.keySet()) {
				Pair<Set<Unit>,Set<Integer>> newData = dataToAdd.get(source);
				Pair<Set<Unit>,Set<Integer>> curData = sourceToUnitMap.get(source);
				
				Objects.requireNonNull(newData);
				Objects.requireNonNull(newData.getFirst());
				Objects.requireNonNull(newData.getSecond());
				if(curData == null){
					curData = new Pair<Set<Unit>,Set<Integer>>(new HashSet<Unit>(),new HashSet<Integer>());
					sourceToUnitMap.put(source, curData);
				}
				curData.getFirst().addAll(newData.getFirst());
				curData.getSecond().addAll(newData.getSecond());
			}
		}
	}
	
	@Override
	public boolean contains(EntryPoint ep, Unit u) {
		Objects.requireNonNull(ep);
		Objects.requireNonNull(u);
		
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> sourceToUnit = data.get(ep);
			if(sourceToUnit != null) {
				for(Pair<Set<Unit>,Set<Integer>> p : sourceToUnit.values()){
					if(p.getFirst().contains(u)){
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
	public boolean contains(Unit u) {
		Objects.requireNonNull(u);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			for(Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> sourceToUnit : data.values()) {
				for(Pair<Set<Unit>,Set<Integer>> p : sourceToUnit.values()){
					if(p.getFirst().contains(u)){
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
	public Set<Unit> getUnits(EntryPoint ep) {
		Objects.requireNonNull(ep);
		Set<Unit> ret = new HashSet<>();
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> sourceToUnit = data.get(ep);
			if(sourceToUnit != null) {
				for(Pair<Set<Unit>,Set<Integer>> p : sourceToUnit.values()){
					ret.addAll(p.getFirst());
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		return SortingMethods.sortSet(ret, SootSort.unitComp);
	}
	
	@Override
	public Set<Unit> getUnits() {
		Set<Unit> ret = new HashSet<>();
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			for(Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> sourceToUnit : data.values()) {
				for(Pair<Set<Unit>,Set<Integer>> p : sourceToUnit.values()){
					ret.addAll(p.getFirst());
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		return SortingMethods.sortSet(ret, SootSort.unitComp);
	}
	
	@Override
	public Map<Unit,Set<Integer>> getUnitsWithDepth(EntryPoint ep){
		Objects.requireNonNull(ep);
		Map<Unit, Set<Integer>> ret = new HashMap<>();
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> sourceToUnit = data.get(ep);
			if(sourceToUnit != null) {
				for(Pair<Set<Unit>,Set<Integer>> p : sourceToUnit.values()){
					for(Unit u : p.getFirst()){
						Set<Integer> depths = ret.get(u);
						if(depths == null){
							depths = new HashSet<Integer>();
							ret.put(u, depths);
						}
						depths.addAll(p.getSecond());
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
	public Map<Unit,Set<Integer>> getUnitsWithDepth(){
		Map<Unit, Set<Integer>> ret = new HashMap<>();
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			for(Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> sourceToUnit : data.values()) {
				for(Pair<Set<Unit>,Set<Integer>> p : sourceToUnit.values()){
					for(Unit u : p.getFirst()){
						Set<Integer> depths = ret.get(u);
						if(depths == null){
							depths = new HashSet<Integer>();
							ret.put(u, depths);
						}
						depths.addAll(p.getSecond());
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
		Objects.requireNonNull(ep);
		Set<SootMethod> ret = new HashSet<>();
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> sourceToUnit = data.get(ep);
			if(sourceToUnit != null) 
				ret.addAll(sourceToUnit.keySet());
		} finally {
			rwlock.readLock().unlock();
		}
		return SortingMethods.sortSet(ret,SootSort.smComp);
	}
	
	@Override
	public Set<SootMethod> getSources() {
		Set<SootMethod> ret = new HashSet<>();
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			for(Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> sourceToUnit : data.values()) {
				ret.addAll(sourceToUnit.keySet());
			}
		} finally {
			rwlock.readLock().unlock();
		}
		return SortingMethods.sortSet(ret,SootSort.smComp);
	}
	
	@Override
	public Map<SootMethod,Set<Integer>> getSourcesWithDepth(EntryPoint ep){
		Objects.requireNonNull(ep);
		Map<SootMethod,Set<Integer>> ret = new HashMap<>();
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> sourceToUnit = data.get(ep);
			if(sourceToUnit != null) {
				for(SootMethod source : sourceToUnit.keySet()){
					Pair<Set<Unit>,Set<Integer>> p = sourceToUnit.get(source);
					Set<Integer> depths = ret.get(source);
					if(depths == null){
						depths = new HashSet<Integer>();
						ret.put(source, depths);
					}
					depths.addAll(p.getSecond());
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
	public Map<SootMethod,Set<Integer>> getSourcesWithDepth() {
		Map<SootMethod,Set<Integer>> ret = new HashMap<>();
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			for(Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> sourceToUnit : data.values()) {
				for(SootMethod source : sourceToUnit.keySet()){
					Pair<Set<Unit>,Set<Integer>> p = sourceToUnit.get(source);
					Set<Integer> depths = ret.get(source);
					if(depths == null){
						depths = new HashSet<Integer>();
						ret.put(source, depths);
					}
					depths.addAll(p.getSecond());
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
	public Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> getData(EntryPoint ep){
		Objects.requireNonNull(ep);
		Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> ret = new HashMap<>();
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> sourceToUnit = data.get(ep);
			if(sourceToUnit != null) {
				for(SootMethod sm : sourceToUnit.keySet()){
					Pair<Set<Unit>,Set<Integer>> val2 = sourceToUnit.get(sm);	
					ret.put(sm, new Pair<Set<Unit>,Set<Integer>>(SortingMethods.sortSet(val2.getFirst(),SootSort.unitComp), 
							SortingMethods.sortSet(val2.getSecond())));
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		return SortingMethods.sortMapKey(ret, SootSort.smComp);
	}
	
	@Override
	public Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> getData(){
		Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> ret = new HashMap<>();
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			for(Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> sourceToUnit : data.values()) {
				for(SootMethod sm : sourceToUnit.keySet()){
					Pair<Set<Unit>,Set<Integer>> val2 = sourceToUnit.get(sm);
					Pair<Set<Unit>,Set<Integer>> cur = ret.get(sm);
					if(cur == null) {
						cur = new Pair<Set<Unit>,Set<Integer>>(new HashSet<Unit>(),new HashSet<Integer>());
						ret.put(sm, cur);
					}
					cur.getFirst().addAll(val2.getFirst());
					cur.getSecond().addAll(val2.getSecond());
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		for(SootMethod sm : ret.keySet()) {
			Pair<Set<Unit>,Set<Integer>> cur = ret.get(sm);
			ret.put(sm, new Pair<Set<Unit>,Set<Integer>>(SortingMethods.sortSet(cur.getFirst(),SootSort.unitComp),SortingMethods.sortSet(cur.getSecond())));
		}
		return SortingMethods.sortMapKey(ret, SootSort.smComp);
	}
	
	@Override
	public String toString() {
		return toString(null);
	}
	
	@Override
	public String toString(String spacer) {
		if(spacer == null)
			spacer = "";
		StringBuilder sb = new StringBuilder();
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			sb.append(spacer).append(name).append(":\n");
			for(EntryPoint ep : data.keySet()) {
				toStringLocked(ep,"  " + spacer,sb);
			}
		} finally {
			rwlock.readLock().unlock();
		}
		return sb.toString();
	}
	
	protected Map<EntryPoint,String> toStringParts(String spacer) {
		if(spacer == null)
			spacer = "";
		Map<EntryPoint,String> ret = new LinkedHashMap<>(data.size()+1);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			ret.put(null, spacer + name + ":\n");
			for(EntryPoint ep : data.keySet()) {
				StringBuilder sb = new StringBuilder();
				toStringLocked(ep,"  " + spacer,sb);
				ret.put(ep, sb.toString());
			}
		} finally {
			rwlock.readLock().unlock();
		}
		return ret;
	}
	
	@Override
	public String toString(EntryPoint ep, String spacer) {
		Objects.requireNonNull(ep);
		if(spacer == null)
			spacer = "";
		StringBuilder sb = new StringBuilder();
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			toStringLocked(ep, spacer, sb);
		} finally {
			rwlock.readLock().unlock();
		}
		return sb.toString();
	}
	
	private void toStringLocked(EntryPoint ep, String spacer, StringBuilder sb) {
		Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> sourceToUnit = data.get(ep);
		if(sourceToUnit != null) {
			int count = 0;
			int insertIndex = 0;
			
			sb.append(spacer).append("Entry Point (Total Units Count - ): ");
			insertIndex = sb.lastIndexOf(")");
			sb.append(ep.toString()).append("\n");
			for(SootMethod source : sourceToUnit.keySet()){
				Pair<Set<Unit>,Set<Integer>> p = sourceToUnit.get(source);
				Set<Unit> units = SortingMethods.sortSet(p.getFirst(),SootSort.unitComp);
				sb.append(spacer).append("  Source Method: ").append(source.toString()).append("\n");
				sb.append(spacer).append("    Depths: ").append(SortingMethods.sortSet(p.getSecond())).append("\n");
				sb.append(spacer).append("    Units Count: ").append(units.size()).append("\n");
				sb.append(spacer).append("    Units: \n");
				count += units.size();
				for(Unit u : units){
					sb.append(spacer).append("      ").append(u).append("\n");
				}
			}
			sb.insert(insertIndex, count);
		}
	}
	
	@Override
	public int hashCode() {
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			int i = 17;
			i = i * 31 + Objects.hashCode(type);
			i = i * 31 + Objects.hashCode(data);
			return i;
		} finally {
			rwlock.readLock().unlock();
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof AccessControlDatabase))
			return false;
		AccessControlDatabase other = (AccessControlDatabase)o;
		if(!type.equals(other.type))
			return false;
		while(true) {
			if(rwlock.writeLock().tryLock()) {
				if(other.rwlock.writeLock().tryLock()) {
					try {
						loadSootResolvedDataWLocked();
						other.loadSootResolvedDataWLocked();
						return Objects.equals(this.data, other.data);
					} finally {
						other.rwlock.writeLock().unlock();
						rwlock.writeLock().unlock();
					}
				} else {
					rwlock.writeLock().unlock();
				}
			}
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {}
		}
	}
	
	@Override
	public AccessControlDatabase readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}

	@Override
	public boolean hasData() {
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			return !data.isEmpty();
		} finally {
			rwlock.readLock().unlock();
		}
	}

	@Override
	public boolean hasData(EntryPoint ep) {
		Objects.requireNonNull(ep);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> sourceToUnit = data.get(ep);
			if(sourceToUnit != null) 
				return !sourceToUnit.isEmpty();
			return false;
		} finally {
			rwlock.readLock().unlock();
		}
	}

	@Override
	public void setFileHashList(FileHashList fhl) {
		this.fhl = fhl;
	}

	@Override
	public Map<EntryPoint, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> getAllData() {
		Map<EntryPoint, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> ret = new HashMap<>();
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			for(EntryPoint ep : data.keySet()) {
				Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> sourceToUnit = data.get(ep);
				Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> temp = new HashMap<>();
				for(SootMethod sm : sourceToUnit.keySet()){
					Pair<Set<Unit>,Set<Integer>> val2 = sourceToUnit.get(sm);	
					temp.put(sm, new Pair<Set<Unit>,Set<Integer>>(SortingMethods.sortSet(val2.getFirst(),SootSort.unitComp), 
							SortingMethods.sortSet(val2.getSecond())));
				}
				ret.put(ep, SortingMethods.sortMapKey(temp, SootSort.smComp));
			}
		} finally {
			rwlock.readLock().unlock();
		}
		return SortingMethods.sortMapKeyAscending(ret);
	}
	
}
