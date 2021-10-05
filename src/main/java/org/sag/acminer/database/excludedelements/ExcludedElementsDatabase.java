package org.sag.acminer.database.excludedelements;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.sag.acminer.database.FileHashDatabase;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.tools.SortingMethods;
import org.sag.common.xstream.NamedCollectionConverterWithSize;
import org.sag.common.xstream.XStreamInOut;
import org.sag.main.sootinit.SootInstanceWrapper;
import org.sag.soot.SootSort;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

import com.google.common.collect.ImmutableSet;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("ExcludedElementsDatabase")
public class ExcludedElementsDatabase extends FileHashDatabase implements IExcludedElementsDatabase {
	
	@XStreamAlias("ExcludedClasses")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"ClassName"},types={String.class})
	private final LinkedHashSet<String> excludedClasses;
	@XStreamAlias("ExcludedMethods")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"MethodSignature"},types={String.class})
	private final LinkedHashSet<String> excludedMethods;
	
	@XStreamOmitField
	private volatile Set<SootClass> resolvedClasses;
	@XStreamOmitField
	private volatile Set<SootMethod> resolvedMethods;
	@XStreamOmitField
	private final IRuntimeExcludedMethods runtimeExcludedMethods;
	@XStreamOmitField
	private final ReadWriteLock rwlock;
	
	private ExcludedElementsDatabase(IRuntimeExcludedMethods runtimeExcludedMethods) {
		this(null,null,runtimeExcludedMethods);
	}
	
	public ExcludedElementsDatabase(Set<String> excludedClasses, Set<String> excludedMethods,
			IRuntimeExcludedMethods runtimeExcludedMethods) {
		this.excludedClasses = excludedClasses == null ? null : new LinkedHashSet<>(excludedClasses);
		this.excludedMethods = excludedMethods == null ? null : new LinkedHashSet<>(excludedMethods);
		this.runtimeExcludedMethods = runtimeExcludedMethods == null ? new EmptyRuntimeExcludedMethods() : runtimeExcludedMethods;
		this.rwlock = new ReentrantReadWriteLock();
		resetSootResolvedData();
	}
	
	public void resetSootResolvedData() {
		rwlock.writeLock().lock();
		try {
			resolvedClasses = null;
			resolvedMethods = null;
			runtimeExcludedMethods.clearSootData();
		} finally {
			rwlock.writeLock().unlock();
		}
	}
	
	public void loadAllSootResolvedData() {
		rwlock.writeLock().lock();
		try {
			loadSootResolvedDataWLocked();
			runtimeExcludedMethods.genSootData();
		} finally {
			rwlock.writeLock().unlock();
		}
	}
	
	public Set<String> getExcludedClasses() {
		rwlock.readLock().lock();
		try {
			return ImmutableSet.copyOf(excludedClasses);
		} finally {
			rwlock.readLock().unlock();
		}
	}
	
	public Set<String> getExcludedMethods() {
		rwlock.readLock().lock();
		try {
			return ImmutableSet.copyOf(excludedMethods);
		} finally {
			rwlock.readLock().unlock();
		}
	}
	
	public Set<SootClass> getSootExcludedClasses() {
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			return resolvedClasses;
		} finally {
			rwlock.readLock().unlock();
		}
	}
	
	public Set<SootMethod> getSootExcludedMethods() {
		rwlock.readLock().lock();
		loadSootResolvedDataRLocked();
		try {
			return resolvedMethods;
		} finally {
			rwlock.readLock().unlock();
		}
	}
	
	private void loadSootResolvedDataWLocked() {
		if(resolvedMethods == null) {
			if(!SootInstanceWrapper.v().isSootInitSet())
				throw new RuntimeException("Error: Some instance of Soot must be initilized first.");
			
			Set<SootClass> resClasses = new HashSet<>();
			for(String cName : excludedClasses) {
				if(Scene.v().containsClass(cName)) {
					SootClass sc = Scene.v().getSootClassUnsafe(cName);//returns null on fail
					if(sc != null)
						resClasses.add(sc);
				}
			}
			this.resolvedClasses = ImmutableSet.copyOf(SortingMethods.sortSet(resClasses,SootSort.scComp));
			
			Set<SootMethod> resMethods = new HashSet<>();
			for(String mSig : excludedMethods) {
				if(Scene.v().containsMethod(mSig)) {
					SootMethod sm = Scene.v().grabMethod(mSig);
					if(sm != null)
						resMethods.add(sm);
				}
			}
			this.resolvedMethods = ImmutableSet.copyOf(SortingMethods.sortSet(resMethods,SootSort.smComp));
		}
	}
	
	private void loadSootResolvedDataRLocked() {
		if(resolvedMethods == null) {
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
	
	public boolean isExcludedMethod(SootMethod m){
		return getSootExcludedMethods().contains(m);
	}
	
	public boolean isExcludedClass(SootClass sc){
		return getSootExcludedClasses().contains(sc);
	}
	
	public ExcludeHandler createNewExcludeHandler(EntryPoint ep, Object... args) {
		return new ExcludeHandler(ep, getSootExcludedMethods(), getSootExcludedClasses(), runtimeExcludedMethods.genRuntimeExcludedMethods(ep));
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		rwlock.writeLock().lock();
		try {
			XStreamInOut.writeXML(this, filePath, path);
		} finally {
			rwlock.writeLock().unlock();
		}
	}
	
	@Override
	public ExcludedElementsDatabase readXML(String filePath, Path path) throws Exception {
		rwlock.writeLock().lock();
		try {
			return XStreamInOut.readXML(this, filePath, path);
		} finally {
			rwlock.writeLock().unlock();
		}
	}
	
	public static ExcludedElementsDatabase readXMLStatic(String filePath, Path path, 
			IRuntimeExcludedMethods runtimeExcludedMethods) throws Exception {
		return new ExcludedElementsDatabase(runtimeExcludedMethods).readXML(filePath, path);
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
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(ExcludedElementsDatabase.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}
	
}
