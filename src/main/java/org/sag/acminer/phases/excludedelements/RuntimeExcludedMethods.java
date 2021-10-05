package org.sag.acminer.phases.excludedelements;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.database.excludedelements.IRuntimeExcludedMethods;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.tools.HierarchyHelpers;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

public class RuntimeExcludedMethods implements IRuntimeExcludedMethods {
	
	private final ReadWriteLock rwlock;
	private final IACMinerDataAccessor dataAccessor;
	private volatile List<SootMethod> callBackMethods;
	private volatile Set<SootClass> callBackClasses;
	private volatile Set<SootClass> binderClasses;
	private volatile SootClass binderClass;
	private volatile Set<SootMethod> binderMethods;
	private volatile Set<SootMethod> shellCommandMethods;
	
	public RuntimeExcludedMethods(IACMinerDataAccessor dataAccessor) {
		this.dataAccessor = dataAccessor;
		this.rwlock = new ReentrantReadWriteLock();
	}

	@Override
	public Set<SootMethod> genRuntimeExcludedMethods(EntryPoint entryPoint, Object... args) {
		SootMethod ep = entryPoint.getEntryPoint();
		SootClass stub = entryPoint.getStub();
		Set<SootMethod> runtimeExcludedMethods = new HashSet<SootMethod>(dataAccessor.getEntryPointsAsSootMethods());
		runtimeExcludedMethods.remove(ep);
		
		rwlock.readLock().lock();
		try {
			genSootDataRLocked();
			
			//Attempt to clean up the analysis of binder entry points by excluding all implemented binder methods except those 
			//implemented by subclasses of the stub, all onTransact methods, and all methods in the Binder class
			if(Scene.v().containsClass("android.os.Binder")) {
				if(binderClasses.contains(stub) && !stub.equals(binderClass) && ep.getDeclaringClass().equals(binderClass)) {
					Set<SootClass> stubSubs = HierarchyHelpers.getAllSubClasses(stub);
					for(SootMethod sm : binderMethods) {
						if(!stubSubs.contains(sm.getDeclaringClass()))
							runtimeExcludedMethods.add(sm);
					}
					if(Scene.v().containsClass("android.os.ShellCommand")) {
						Set<SootMethod> allowedMethods = new HashSet<>();
						for(SootMethod onCommand : shellCommandMethods) {
							SootClass onCommandClass = onCommand.getDeclaringClass();
							if((onCommandClass.isInnerClass() && stubSubs.contains(onCommandClass.getOuterClass())) 
									|| (onCommandClass.isInnerClass() && hasMatchingOuterClass(onCommandClass.getOuterClass(), stubSubs))) {
								allowedMethods.add(onCommand);
							}
						}
						if(allowedMethods.isEmpty()) {
							for(SootMethod onCommand : shellCommandMethods) {
								SootClass onCommandClass = onCommand.getDeclaringClass();
								if(hasMatchingPackage(onCommandClass, stubSubs))
									allowedMethods.add(onCommand);
							}
						}
						if(!allowedMethods.isEmpty()) {
							for(SootMethod onCommand : shellCommandMethods) {
								if(!allowedMethods.contains(onCommand))
									runtimeExcludedMethods.add(onCommand);
							}
						}
					}
				}
			}
			
			//try to get rid of callgraph explosion when dealing with Callback entry point classes
			if(Scene.v().containsClass("com.android.internal.os.HandlerCaller$Callback")) {
				SootClass epClass = ep.getDeclaringClass();
				if(callBackClasses.contains(epClass)) {
					for(SootClass sc : callBackClasses) {
						if(!epClass.equals(sc)) {
							for(SootMethod m : callBackMethods) {
								SootMethod e = sc.getMethodUnsafe(m.getSubSignature());
								if(e != null)
									runtimeExcludedMethods.add(e);
							}
						}
					}
				}
				
				//hack to make the callback handler go to the right place for this ep
				if(Scene.v().containsClass("com.android.printspooler.PrintSpoolerService$PrintSpooler")) {
					if(epClass.getName().equals("com.android.printspooler.PrintSpoolerService$PrintSpooler")) {
						for(SootClass sc : callBackClasses) {
							if(!sc.getName().equals("com.android.printspooler.PrintSpoolerService$HandlerCallerCallback")) {
								for(SootMethod m : callBackMethods) {
									SootMethod e = sc.getMethodUnsafe(m.getSubSignature());
									if(e != null)
										runtimeExcludedMethods.add(e);
								}
							}
						}
					}
				}
			}
		} finally {
			rwlock.readLock().unlock();
		}
		
		return runtimeExcludedMethods;
	}
	
	private boolean hasMatchingOuterClass(SootClass outerClass, Set<SootClass> stubSubs) {
		for(SootClass stubSub : stubSubs) {
			if(stubSub.isInnerClass() && stubSub.getOuterClass().equals(outerClass))
				return true;
		}
		return false;
	}
	
	private boolean hasMatchingPackage(SootClass sc , Set<SootClass> stubSubs) {
		for(SootClass stubSub : stubSubs) {
			if(stubSub.getPackageName().equals(sc.getPackageName()))
				return true;
		}
		return false;
	}
	
	private void genSootDataRLocked() {
		if(callBackMethods == null) {
			// Must release read lock before acquiring write lock
			rwlock.readLock().unlock();
			rwlock.writeLock().lock();
			try {
				genSootDataWLocked();
				rwlock.readLock().lock(); // Downgrade by acquiring read lock before releasing write lock
			} finally {
				rwlock.writeLock().unlock(); // Unlock write, still hold read
			}
		}
	}
	
	private void genSootDataWLocked() {
		if(callBackMethods == null) {
			if(Scene.v().containsClass("com.android.internal.os.HandlerCaller$Callback")) {
				SootClass callback = Scene.v().getSootClassUnsafe("com.android.internal.os.HandlerCaller$Callback",false);
				callBackMethods = callback.getMethods();
				callBackClasses = HierarchyHelpers.getAllSubClassesOfInterface(callback);
			} else {
				callBackMethods = Collections.emptyList();
				callBackClasses = Collections.emptySet();
			}
			
			if(Scene.v().containsClass("android.os.Binder")) {
				binderClass = Scene.v().getSootClassUnsafe("android.os.Binder", false);
				binderClasses = HierarchyHelpers.getAllSubClasses(binderClass);
				binderMethods = HierarchyHelpers.getAllImplementingMethods(binderClass);
				//All implemented binder methods minus those in the binder classes and onTransact methods
				for(SootMethod sm : binderClass.getMethods()) {
					binderMethods.remove(sm);
				}
				for(Iterator<SootMethod> it = binderMethods.iterator(); it.hasNext();) {
					if(it.next().getName().equals("onTransact"))
						it.remove();
				}
			} else {
				binderClasses = Collections.emptySet();
				binderClass = null;
				binderMethods = Collections.emptySet();
			}
			
			if(Scene.v().containsClass("android.os.ShellCommand")) {
				shellCommandMethods = new HashSet<>();
				for(SootMethod sm : HierarchyHelpers.getAllImplementingMethods(Scene.v().getSootClassUnsafe("android.os.ShellCommand", false))) {
					if(sm.getSubSignature().equals("int onCommand(java.lang.String)"))
						shellCommandMethods.add(sm);
				}
			} else {
				shellCommandMethods = Collections.emptySet();
			}
		}
	}
	
	@Override
	public void genSootData() {
		if(callBackMethods == null) {
			rwlock.writeLock().lock();
			try {
				genSootDataWLocked();
			} finally {
				rwlock.writeLock().unlock();
			}
		}
	}
	
	@Override
	public void clearSootData() {
		rwlock.writeLock().lock();
		try {
			callBackMethods = null;
			callBackClasses = null;
			binderClasses = null;
			binderClass = null;
			binderMethods = null;
			shellCommandMethods = null;
		} finally {
			rwlock.writeLock().unlock();
		}
	}

}
