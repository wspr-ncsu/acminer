package org.sag.acminer.database.accesscontrol;

import java.io.ObjectStreamException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.tools.SortingMethods;
import org.sag.common.xstream.XStreamInOut;
import org.sag.soot.SootSort;

import soot.SootMethod;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("ContextQueriesDatabase")
public class ContextQueriesDatabase extends AccessControlDatabase implements IContextQueryDatabase {
	
	@XStreamOmitField
	private final ReadWriteLock rwlock;
	//Entry Point -> Context Query Method -> Set of Methods in Sub-graph of context query method
	@XStreamOmitField
	protected volatile Map<EntryPoint, Map<SootMethod, Set<SootMethod>>> cqSubgraphMethods;
	@XStreamOmitField
	private volatile boolean loaded;

	public ContextQueriesDatabase(String name, String type, boolean newDB) {
		super(name, type, newDB);
		if(newDB) {
			cqSubgraphMethods = new HashMap<>();
			loaded = true;
		} else {
			cqSubgraphMethods = null;
			loaded = false;
		}
		rwlock = new ReentrantReadWriteLock();
	}
	
	//ReadResolve is always run when reading from XML even if a constructor is run first
	protected Object readResolve() throws ObjectStreamException {
		super.readResolve();
		loadSootResolvedData();
		return this;
	}
	
	protected Object writeReplace() throws ObjectStreamException {
		super.writeReplace();
		rwlock.writeLock().lock();
		try {
			writeSootResolvedDataWLocked();
		} finally {
			rwlock.writeLock().unlock();
		}
		return this;
	}
	
	private void sortDataLocked() {
		for(EntryPoint ep : cqSubgraphMethods.keySet()) {
			Map<SootMethod, Set<SootMethod>> cqToSubGraph = cqSubgraphMethods.get(ep);
			for(SootMethod cq : cqToSubGraph.keySet()) {
				Set<SootMethod> subGraph = cqToSubGraph.get(cq);
				cqToSubGraph.put(cq, SortingMethods.sortSet(subGraph,SootSort.smComp));
			}
			cqSubgraphMethods.put(ep, SortingMethods.sortMapKey(cqToSubGraph, SootSort.smComp));
		}
		cqSubgraphMethods = SortingMethods.sortMapKeyAscending(cqSubgraphMethods);
	}
	
	@Override
	public void resetSootResolvedData() {
		super.resetSootResolvedData();
		try {
			writeSootResolvedDataWLocked();
			cqSubgraphMethods = null;
			loaded = false;
		} finally {
			rwlock.writeLock().unlock();
		}
	}
	
	@Override
	public void loadSootResolvedData() {
		super.loadSootResolvedData();
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
			for(EntryPoint ep : cqSubgraphMethods.keySet()) {
				boolean found = false;
				for(EntryPointContainer epc : output) {
					if(Objects.equals(ep.getEntryPoint(), epc.getEntryPoint()) && Objects.equals(ep.getStub(), epc.getStub())) {
						epc.addContextQueries(cqSubgraphMethods.get(ep));
						found = true;
						break;
					}
				}
				if(!found)
					throw new RuntimeException("Error: Entry Point '" + ep.toString() + "' did not already have an entry in the database!?!");
			}
		}
	}
	
	private void loadSootResolvedDataWLocked() {
		if(!loaded) {
			cqSubgraphMethods = new HashMap<>();
			//Note this will fail with an exception if the data cannot be loaded 
			//as this should always be the same or be recreated 
			for(EntryPointContainer epContainer : output) {
				cqSubgraphMethods.put(new EntryPoint(epContainer.getEntryPoint(),epContainer.getStub()),epContainer.getSubGraphMethods());
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
		super.sortData();
		rwlock.writeLock().lock();
		try {
			loadSootResolvedDataWLocked();
			sortDataLocked();
		} finally {
			rwlock.writeLock().unlock();
		}
	}
	
	@Override
	public void addContextQuerySubGraphs(EntryPoint ep, Map<SootMethod, Set<SootMethod>> dataToAdd) {
		rwlock.writeLock().lock();
		try {
			loadSootResolvedDataWLocked();
			addInner(ep,dataToAdd);
		} finally {
			rwlock.writeLock().unlock();
		}
	}
	
	@Override
	public void addAllContextQuerySubGraphs(Map<EntryPoint, Map<SootMethod, Set<SootMethod>>> dataToAdd) {
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
	
	private void addInner(EntryPoint ep, Map<SootMethod, Set<SootMethod>> dataToAdd) {
		Objects.requireNonNull(ep);
		Objects.requireNonNull(dataToAdd);
		if(!dataToAdd.isEmpty()) {
			Map<SootMethod, Set<SootMethod>> cqSubGraphs = cqSubgraphMethods.get(ep);
			if(cqSubGraphs == null) {
				cqSubGraphs = new HashMap<>();
				cqSubgraphMethods.put(ep, cqSubGraphs);
			}
			for(SootMethod cq : dataToAdd.keySet()) {
				Set<SootMethod> newData = dataToAdd.get(cq);
				Objects.requireNonNull(newData);
				
				Set<SootMethod> temp = cqSubGraphs.get(cq);
				if(temp == null) {
					temp = new HashSet<>();
					cqSubGraphs.put(cq, temp);
				}
				temp.addAll(newData);
			}
		}
	}
	
	@Override
	public Set<SootMethod> getContextQueries() {
		Set<SootMethod> ret = new HashSet<>();
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			for(Map<SootMethod, Set<SootMethod>> cqToSubGraph : cqSubgraphMethods.values()) {
				ret.addAll(cqToSubGraph.keySet());
			}
		} finally {
			rwlock.readLock().unlock();
		}
		return SortingMethods.sortSet(ret, SootSort.smComp);
	}
	
	@Override
	public Map<EntryPoint,Set<SootMethod>> getContextQueriesByEntryPoint() {
		Map<EntryPoint,Set<SootMethod>> ret = new HashMap<>();
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			for(EntryPoint ep : cqSubgraphMethods.keySet()) {
				Map<SootMethod, Set<SootMethod>> cqToSubGraph = cqSubgraphMethods.get(ep);
				ret.put(ep, SortingMethods.sortSet(new HashSet<SootMethod>(cqToSubGraph.keySet()), SootSort.smComp));
			}
		} finally {
			rwlock.readLock().unlock();
		}
		return SortingMethods.sortMapKeyAscending(ret);
	}
	
	@Override
	public Set<SootMethod> getContextQueries(EntryPoint ep) {
		Objects.requireNonNull(ep);
		Set<SootMethod> ret = new HashSet<>();
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			Map<SootMethod, Set<SootMethod>> cqToSubGraph = cqSubgraphMethods.get(ep);
			if(cqToSubGraph != null) {
				ret.addAll(cqToSubGraph.keySet());
			}
		} finally {
			rwlock.readLock().unlock();
		}
		return SortingMethods.sortSet(ret, SootSort.smComp);
	}
	
	@Override
	public Set<SootMethod> getSubGraphMethods() {
		Set<SootMethod> ret = new HashSet<>();
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			for(Map<SootMethod, Set<SootMethod>> cqToSubGraph : cqSubgraphMethods.values()) {
				for(Set<SootMethod> sgMethods : cqToSubGraph.values()) {
					ret.addAll(sgMethods);
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		return SortingMethods.sortSet(ret, SootSort.smComp);
	}
	
	@Override
	public Set<SootMethod> getSubGraphMethods(EntryPoint ep) {
		Objects.requireNonNull(ep);
		Set<SootMethod> ret = new HashSet<>();
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			Map<SootMethod, Set<SootMethod>> cqToSubGraph = cqSubgraphMethods.get(ep);
			if(cqToSubGraph != null) {
				for(Set<SootMethod> sgMethods : cqToSubGraph.values()) {
					ret.addAll(sgMethods);
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		return SortingMethods.sortSet(ret, SootSort.smComp);
	}
	
	@Override
	public Map<SootMethod, Set<SootMethod>> getContextQueriesWithSubGraphMethods() {
		Map<SootMethod, Set<SootMethod>> ret = new HashMap<>();
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			for(Map<SootMethod, Set<SootMethod>> cqToSubGraph : cqSubgraphMethods.values()) {
				for(SootMethod cq : cqToSubGraph.keySet()) {
					Set<SootMethod> temp = ret.get(cq);
					if(temp == null) {
						temp = new HashSet<>();
						ret.put(cq, temp);
					}
					temp.addAll(cqToSubGraph.get(cq));
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		for(SootMethod cq : ret.keySet()) {
			ret.put(cq, SortingMethods.sortSet(ret.get(cq),SootSort.smComp));
		}
		return SortingMethods.sortMapKey(ret, SootSort.smComp);
	}
	
	@Override
	public Map<SootMethod, Set<SootMethod>> getContextQueriesWithSubGraphMethods(EntryPoint ep) {
		Objects.requireNonNull(ep);
		Map<SootMethod, Set<SootMethod>> ret = new HashMap<>();
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			Map<SootMethod, Set<SootMethod>> cqToSubGraph = cqSubgraphMethods.get(ep);
			if(cqToSubGraph != null) {
				for(SootMethod cq : cqToSubGraph.keySet()) {
					ret.put(cq, SortingMethods.sortSet(cqToSubGraph.get(cq), SootSort.smComp));
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		return SortingMethods.sortMapKey(ret, SootSort.smComp);
	}
	
	@Override
	public Map<EntryPoint, Map<SootMethod, Set<SootMethod>>> getAllContextQueriesWithSubGraphMethods() {
		Map<EntryPoint, Map<SootMethod, Set<SootMethod>>> ret = new HashMap<>();
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			for(EntryPoint ep : cqSubgraphMethods.keySet()) {
				Map<SootMethod, Set<SootMethod>> cqToSubGraph = cqSubgraphMethods.get(ep);
				Map<SootMethod, Set<SootMethod>> temp = new HashMap<>();
				for(SootMethod sm : cqToSubGraph.keySet()){
					temp.put(sm, SortingMethods.sortSet(cqToSubGraph.get(sm),SootSort.smComp));
				}
				ret.put(ep, SortingMethods.sortMapKey(temp, SootSort.smComp));
			}
		} finally {
			rwlock.readLock().unlock();
		}
		return SortingMethods.sortMapKeyAscending(ret);
	}
	
	@Override
	public boolean isContextQuery(SootMethod sm) {
		Objects.requireNonNull(sm);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			for(Map<SootMethod, Set<SootMethod>> t1 : cqSubgraphMethods.values()) {
				if(t1.keySet().contains(sm))
					return true;
			}
			return false;
		} finally {
			rwlock.readLock().unlock();
		}
	}
	
	@Override
	public boolean isContextQuery(EntryPoint ep, SootMethod sm) {
		Objects.requireNonNull(ep);
		Objects.requireNonNull(sm);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			Map<SootMethod, Set<SootMethod>> t1 = cqSubgraphMethods.get(ep);
			if(t1 != null && t1.keySet().contains(sm))
				return true;
			return false;
		} finally {
			rwlock.readLock().unlock();
		}
	}
	
	@Override
	public boolean isSubGraphMethodOf(SootMethod cq, SootMethod sm) {
		Objects.requireNonNull(cq);
		Objects.requireNonNull(sm);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			for(Map<SootMethod, Set<SootMethod>> t1 : cqSubgraphMethods.values()) {
				Set<SootMethod> t2 = t1.get(cq);
				if(t2 != null && t2.contains(sm))
					return true;
			}
			return false;
		} finally {
			rwlock.readLock().unlock();
		}
	}
	
	@Override
	public boolean isSubGraphMethodOf(EntryPoint ep, SootMethod cq, SootMethod sm) {
		Objects.requireNonNull(ep);
		Objects.requireNonNull(cq);
		Objects.requireNonNull(sm);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			Map<SootMethod, Set<SootMethod>> t1 = cqSubgraphMethods.get(ep);
			if(t1 == null)
				return false;
			Set<SootMethod> t2 = t1.get(cq);
			if(t2 == null)
				return false;
			return t2.contains(sm);
		} finally {
			rwlock.readLock().unlock();
		}
	}
	
	@Override
	public boolean isSubGraphMethod(SootMethod sm) {
		Objects.requireNonNull(sm);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			for(Map<SootMethod, Set<SootMethod>> t1 : cqSubgraphMethods.values()) {
				for(Set<SootMethod> sgMethods : t1.values()) {
					if(sgMethods.contains(sm))
						return true;
				}
			}
			return false;
		} finally {
			rwlock.readLock().unlock();
		}
	}
	
	@Override
	public boolean isSubGraphMethod(EntryPoint ep, SootMethod sm) {
		Objects.requireNonNull(ep);
		Objects.requireNonNull(sm);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			Map<SootMethod, Set<SootMethod>> t1 = cqSubgraphMethods.get(ep);
			if(t1 == null)
				return false;
			for(Set<SootMethod> sgMethods : t1.values()) {
				if(sgMethods.contains(sm))
					return true;
			}
			return false;
		} finally {
			rwlock.readLock().unlock();
		}
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
		Map<EntryPoint,String> parts = toStringParts(spacer);
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			sb.append(parts.get(null));
			for(EntryPoint ep : parts.keySet()) {
				if(ep == null)
					continue;
				sb.append(parts.get(ep));
				toStringLocked(ep, spacer + "      ", sb);
			}
		} finally {
			rwlock.readLock().unlock();
		}
		return sb.toString();
	}
	
	@Override
	public String toString(EntryPoint ep, String spacer) {
		Objects.requireNonNull(ep);
		if(spacer == null)
			spacer = "";
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString(ep, spacer));
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			toStringLocked(ep, spacer + "    ", sb);
		} finally {
			rwlock.readLock().unlock();
		}
		return sb.toString();
	}
	
	private void toStringLocked(EntryPoint ep, String spacer, StringBuilder sb) {
		Map<SootMethod, Set<SootMethod>> cqToSubGraph = cqSubgraphMethods.get(ep);
		if(cqToSubGraph != null) {
			sb.append(spacer).append("Context Queries (Total Methods Count - ").append(cqToSubGraph.size()).append("):\n");
			for(SootMethod contextQuery : cqToSubGraph.keySet()) {
				sb.append(spacer).append("  Context Query: ").append(contextQuery.toString()).append("\n");
				sb.append(spacer).append("    Sub-Graph Methods:\n");
				for(SootMethod sm : cqToSubGraph.get(contextQuery)) {
					sb.append(spacer).append("      ").append(sm).append("\n");
				}
			}
		}
	}
	
	@Override
	public int hashCode() {
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			int i = 17;
			i = i * 31 + super.hashCode();
			i = i * 31 + Objects.hashCode(cqSubgraphMethods);
			return i;
		} finally {
			rwlock.readLock().unlock();
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if(super.equals(o)) {
			if(o == null || !(o instanceof ContextQueriesDatabase))
				return false;
			ContextQueriesDatabase other = (ContextQueriesDatabase)o;
			while(true) {
				if(rwlock.writeLock().tryLock()) {
					if(other.rwlock.writeLock().tryLock()) {
						try {
							loadSootResolvedDataWLocked();
							other.loadSootResolvedDataWLocked();
							return Objects.equals(this.cqSubgraphMethods, other.cqSubgraphMethods);
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
		return false;
	}
	
	@Override
	public ContextQueriesDatabase readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}

}
