package org.sag.acminer;

import java.util.Map;
import java.util.Set;

import org.sag.acminer.database.accesscontrol.AccessControlDatabaseFactory;
import org.sag.acminer.database.accesscontrol.IAccessControlDatabase;
import org.sag.acminer.database.accesscontrol.IContextQueryDatabase;
import org.sag.acminer.database.acminer.IACMinerDatabase;
import org.sag.acminer.database.binderservices.IBinderServicesDatabase;
import org.sag.acminer.database.defusegraph.IDefUseGraphDatabase;
import org.sag.acminer.database.entrypointedges.IEntryPointEdgesDatabase;
import org.sag.acminer.database.excludedelements.IExcludedElementsDatabase;
import org.sag.acminer.phases.bindergroups.BinderGroupsDatabase;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.acminer.phases.entrypoints.EntryPointsDatabase;
import org.sag.acminer.phases.entrypoints.EntryPointsDatabase.IntegerWrapper;
import org.sag.main.BaseDataAccessor;
import org.sag.main.config.Config;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;

public class ACMinerDataAccessor extends BaseDataAccessor implements IACMinerDataAccessor {
	
	protected volatile IAccessControlDatabase controlPredicateDatabase;
	protected volatile IContextQueryDatabase contextQueryDatabase;
	protected volatile IAccessControlDatabase throwSecurityExceptionStmtsDatabase;
	protected volatile IDefUseGraphDatabase defUseGraphDatabase;
	protected volatile IDefUseGraphDatabase defUseGraphModDatabase;
	protected volatile IExcludedElementsDatabase excludedElementsDatabase;
	protected volatile IACMinerDatabase aCMinerDatabase;
	protected volatile IEntryPointEdgesDatabase entryPointEdgesDatabase;
	protected volatile IBinderServicesDatabase binderServiceDatabase;

	public ACMinerDataAccessor(Config config) {
		super(config);
		controlPredicateDatabase = AccessControlDatabaseFactory.getNewControlPredicatesDatabase(true);
		contextQueryDatabase = AccessControlDatabaseFactory.getNewContextQueriesDatabase(true);
		throwSecurityExceptionStmtsDatabase = AccessControlDatabaseFactory.getNewThrowSecurityExceptionStmtsDatabase(true);
		defUseGraphDatabase = IDefUseGraphDatabase.Factory.getNew(true);
		defUseGraphModDatabase = IDefUseGraphDatabase.Factory.getNew(true);
		excludedElementsDatabase = IExcludedElementsDatabase.Factory.getNew(null,null,null);
		aCMinerDatabase = IACMinerDatabase.Factory.getNew(null);
		entryPointEdgesDatabase = IEntryPointEdgesDatabase.Factory.getNew(true);
		binderServiceDatabase = IBinderServicesDatabase.Factory.getNew(true);
	}
	
	@Override
	public Set<SootMethod> getEntryPointsAsSootMethods(){
		synchronized(EntryPointsDatabase.v()) {
			return EntryPointsDatabase.v().getEntryPointsAsSootMethods();
		}
	}
	
	@Override
	public Set<EntryPoint> getEntryPoints(){
		synchronized(EntryPointsDatabase.v()) {
			return EntryPointsDatabase.v().getEntryPoints();
		}
	}
	
	@Override
	public Set<SootMethod> getEntryPointsForStub(SootClass stub) {
		synchronized(EntryPointsDatabase.v()) {
			return EntryPointsDatabase.v().getSootResolvedEntryPointsForStub(stub);
		}
	}
	
	@Override
	public Map<SootClass, Set<SootMethod>> getBinderInterfacesAndMethods() {
		synchronized(BinderGroupsDatabase.v()) {
			return BinderGroupsDatabase.v().getSootResolvedBinderInterfacesAndMethods();
		}
	}

	@Override
	public Map<SootClass, Map<SootClass, Map<SootMethod, Set<Integer>>>> getBinderProxiesAndMethodsByInterface() {
		synchronized(BinderGroupsDatabase.v()) {
			return BinderGroupsDatabase.v().getSootResolvedBinderProxiesAndMethodsByInterface();
		}
	}

	@Override
	public Map<SootClass, Map<SootClass, Map<SootMethod, Set<Integer>>>> getBinderStubsAndMethodsByInterface() {
		synchronized(BinderGroupsDatabase.v()) {
			return BinderGroupsDatabase.v().getSootResolvedBinderStubsAndMethodsByInterface();
		}
	}

	@Override
	public Map<SootClass, Map<SootMethod, Set<Integer>>> getBinderProxiesAndMethods() {
		synchronized(BinderGroupsDatabase.v()) {
			return BinderGroupsDatabase.v().getSootResolvedBinderProxiesAndMethods();
		}	
	}

	@Override
	public Map<SootClass, Map<SootMethod, Set<Integer>>> getBinderStubsAndMethods() {
		synchronized(BinderGroupsDatabase.v()) {
			return BinderGroupsDatabase.v().getSootResolvedBinderStubsAndMethods();
		}
	}

	@Override
	public Map<SootClass, Map<String, Set<SootMethod>>> getBinderStubMethodsToEntryPointsByInterface() {
		synchronized(BinderGroupsDatabase.v()) {
			return BinderGroupsDatabase.v().getSootResolvedBinderStubMethodsToEntryPointsByInterface();
		}
	}

	@Override
	public Map<SootMethod, Set<SootMethod>> getBinderInterfaceMethodsToProxyMethods() {
		synchronized(BinderGroupsDatabase.v()) {
			return BinderGroupsDatabase.v().getSootResolvedBinderInterfaceMethodsToProxyMethods();
		}
	}

	@Override
	public Map<SootMethod, Set<SootMethod>> getBinderInterfaceMethodsToEntryPoints() {
		synchronized(BinderGroupsDatabase.v()) {
			return BinderGroupsDatabase.v().getSootResolvedBinderInterfaceMethodsToEntryPoints();
		}
	}

	@Override
	public Map<SootMethod, Set<SootMethod>> getBinderProxyMethodsToEntryPoints() {
		synchronized(BinderGroupsDatabase.v()) {
			return BinderGroupsDatabase.v().getSootResolvedBinderProxyMethodsToEntryPoints();
		}
	}

	@Override
	public Map<String, Set<SootMethod>> getBinderStubMethodsToEntryPoints() {
		synchronized(BinderGroupsDatabase.v()) {
			return BinderGroupsDatabase.v().getSootResolvedBinderStubMethodsToEntryPoints();
		}
	}

	@Override
	public Map<String, Set<SootMethod>> getBinderStubMethodsToEntryPointsForInterface(SootClass binderInterface) {
		synchronized(BinderGroupsDatabase.v()) {
			return BinderGroupsDatabase.v().getSootResolvedBinderStubMethodsToEntryPointsForInterface(binderInterface);
		}
	}

	@Override
	public Set<SootMethod> getBinderProxyMethodsForInterfaceMethod(SootMethod interfaceMethod) {
		synchronized(BinderGroupsDatabase.v()) {
			return BinderGroupsDatabase.v().getSootResolvedBinderProxyMethodsForInterfaceMethod(interfaceMethod);
		}
	}

	@Override
	public Set<SootMethod> getEntryPointsForBinderProxyMethod(SootMethod proxyMethod) {
		synchronized(BinderGroupsDatabase.v()) {
			return BinderGroupsDatabase.v().getSootResolvedEntryPointsForBinderProxyMethod(proxyMethod);
		}
	}
	
	@Override
	public Set<SootMethod> getEntryPointsFromBinderMethod(InvokeExpr ie) {
		synchronized(BinderGroupsDatabase.v()) {
			return BinderGroupsDatabase.v().getSootResolvedEntryPointsFromBinderMethod(ie);
		}
	}
	
	@Override
	public Set<String> getAllBinderGroupClasses() {
		synchronized(BinderGroupsDatabase.v()) {
			return BinderGroupsDatabase.v().getAllBinderGroupClasses();
		}
	}
	
	@Override
	public Map<SootClass, Map<SootMethod, Set<IntegerWrapper>>> getEntryPointsByStubWithTransactionId() {
		synchronized(EntryPointsDatabase.v()) {
			return EntryPointsDatabase.v().getSootResolvedEntryPointsByStubWithTransactionId();
		}
	}

	@Override
	public Map<SootClass, Set<IntegerWrapper>> getStubsToAllTransactionIds() {
		synchronized(EntryPointsDatabase.v()) {
			return EntryPointsDatabase.v().getSootResolvedStubsToAllTransactionIds();
		}
	}
	
	@Override
	public boolean isBinderGroupsDatabaseSet() {
		rwlock.readLock().lock();
		try {
			return BinderGroupsDatabase.isSet();
		} finally {
			rwlock.readLock().unlock();
		}
	}

	@Override
	public boolean isEntryPointsDatabaseSet() {
		rwlock.readLock().lock();
		try {
			return EntryPointsDatabase.isSet();
		} finally {
			rwlock.readLock().unlock();
		}
	}
	
	@Override
	public Set<String> getAllEntryPointClasses() {
		synchronized(EntryPointsDatabase.v()) {
			return EntryPointsDatabase.v().getAllEpClasses();
		}
	}
	
	@Override
	public Set<String> getAllEntryPointMethods() {
		synchronized(EntryPointsDatabase.v()) {
			return EntryPointsDatabase.v().getAllEpMethods();
		}
	}
	
	@Override
	protected void resetAllSootDataLocked(boolean resetSootInstance) {
		if(EntryPointsDatabase.isSet())
			synchronized(EntryPointsDatabase.v()) {
				EntryPointsDatabase.v().resetSootResolvedData();
			}
		if(BinderGroupsDatabase.isSet())
			synchronized(BinderGroupsDatabase.v()) {
				BinderGroupsDatabase.v().resetSootResolvedData();
			}
		getExcludedElementsDB().resetSootResolvedData();
		getControlPredicatesDB().resetSootResolvedData();
		getContextQueriesDB().resetSootResolvedData();
		getThrowSecurityExceptionStmtsDB().resetSootResolvedData();
		getDefUseGraphDB().resetSootResolvedData();
		getDefUseGraphModDB().resetSootResolvedData();
		getEntryPointEdgesDB().clearSootResolvedData();
		getBinderServiceDB().clearSootResolvedData();
		getACMinerDB().clearSootResolvedData();
		super.resetAllSootDataLocked(resetSootInstance);
	}
	
	@Override
	protected void resetAllDatabasesAndDataLocked() {
		EntryPointsDatabase.setDatabase(null);
		BinderGroupsDatabase.setDatabase(null);
		controlPredicateDatabase = AccessControlDatabaseFactory.getNewControlPredicatesDatabase(true);
		contextQueryDatabase = AccessControlDatabaseFactory.getNewContextQueriesDatabase(true);
		throwSecurityExceptionStmtsDatabase = AccessControlDatabaseFactory.getNewThrowSecurityExceptionStmtsDatabase(true);
		defUseGraphDatabase = IDefUseGraphDatabase.Factory.getNew(true);
		defUseGraphModDatabase = IDefUseGraphDatabase.Factory.getNew(true);
		excludedElementsDatabase = IExcludedElementsDatabase.Factory.getNew(null,null,null);
		aCMinerDatabase = IACMinerDatabase.Factory.getNew(null);
		entryPointEdgesDatabase = IEntryPointEdgesDatabase.Factory.getNew(true);
		binderServiceDatabase = IBinderServicesDatabase.Factory.getNew(true);
		super.resetAllDatabasesAndDataLocked();
	}
	
	@Override
	public boolean hasMarkedUnits(EntryPoint ep) {
		return getContextQueriesDB().hasData(ep) || getControlPredicatesDB().hasData(ep) 
				|| getEntryPointEdgesDB().hasEntryPointEdges(ep);
	}

	@Override
	public boolean isMarkedUnit(EntryPoint ep, Unit u) {
		return getContextQueriesDB().contains(ep, u) || getControlPredicatesDB().contains(ep, u);
	}

	@Override
	public void resetEntryPointsDatabaseSootData() {
		synchronized(EntryPointsDatabase.v()) {
			EntryPointsDatabase.v().resetSootResolvedData();
		}
	}
	
	@Override
	public Map<SootClass, Set<SootMethod>> getEntryPointsByDeclaringClass() {
		synchronized(EntryPointsDatabase.v()) {
			return EntryPointsDatabase.v().getSootResolvedEntryPointsByDeclaringClass();
		}
	}
	
	//Start Control Predicates

	@Override
	public IAccessControlDatabase getControlPredicatesDB() {
		rwlock.readLock().lock();
		try {
			return controlPredicateDatabase;
		} finally {
			rwlock.readLock().unlock();
		}
	}

	@Override
	public void setControlPredicatesDB(IAccessControlDatabase db) {
		if(db != null) {
			if(AccessControlDatabaseFactory.isControlPredicatesDatabase(db)) {
				rwlock.writeLock().lock();
				try {
					this.controlPredicateDatabase = db;
				} finally {
					rwlock.writeLock().unlock();
				}
			}
		}
	}
	
	//End Control Predicates
	
	//Start Context Queries

	@Override
	public IContextQueryDatabase getContextQueriesDB() {
		rwlock.readLock().lock();
		try {
			return contextQueryDatabase;
		} finally {
			rwlock.readLock().unlock();
		}
	}

	@Override
	public void setContextQueriesDB(IContextQueryDatabase db) {
		if(db != null) {
			if(AccessControlDatabaseFactory.isContextQueriesDatabase(db)) {
				rwlock.writeLock().lock();
				try {
					this.contextQueryDatabase = db;
				} finally {
					rwlock.writeLock().unlock();
				}
			}
		}
	}
	
	//End Context Queries
	
	//Start Throw Security Exception Statements

	@Override
	public IAccessControlDatabase getThrowSecurityExceptionStmtsDB() {
		rwlock.readLock().lock();
		try {
			return throwSecurityExceptionStmtsDatabase;
		} finally {
			rwlock.readLock().unlock();
		}
	}

	@Override
	public void setThrowSecurityExceptionStmtsDB(IAccessControlDatabase db) {
		if(db != null) {
			if(AccessControlDatabaseFactory.isThrowSecurityExceptionStmtsDatabase(db)) {
				rwlock.writeLock().lock();
				try {
					this.throwSecurityExceptionStmtsDatabase = db;
				} finally {
					rwlock.writeLock().unlock();
				}
			}
		}
	}
	
	//End Throw Security Exception Statements
	
	//Start def use graph database
	
	@Override
	public IDefUseGraphDatabase getDefUseGraphDB() {
		rwlock.readLock().lock();
		try {
			return defUseGraphDatabase;
		} finally {
			rwlock.readLock().unlock();
		}
	}

	@Override
	public void setDefUseGraphDB(IDefUseGraphDatabase db) {
		if(db != null) {
			rwlock.writeLock().lock();
			try {
				this.defUseGraphDatabase = db;
			} finally {
				rwlock.writeLock().unlock();
			}
		}
	}
	
	//End def use graph database
	
	//Start def use graph mod database
	
	@Override
	public IDefUseGraphDatabase getDefUseGraphModDB() {
		rwlock.readLock().lock();
		try {
			return defUseGraphModDatabase;
		} finally {
			rwlock.readLock().unlock();
		}
	}

	@Override
	public void setDefUseGraphModDB(IDefUseGraphDatabase db) {
		if(db != null) {
			rwlock.writeLock().lock();
			try {
				this.defUseGraphModDatabase = db;
			} finally {
				rwlock.writeLock().unlock();
			}
		}
			
	}
	
	//End def use graph mod database
	
	//Start excluded elements database
	
	@Override
	public IExcludedElementsDatabase getExcludedElementsDB() {
		rwlock.readLock().lock();
		try {
			return excludedElementsDatabase;
		} finally {
			rwlock.readLock().unlock();
		}
	}
	
	@Override
	public void setExcludedElementsDB(IExcludedElementsDatabase db) {
		if(db != null) {
			rwlock.writeLock().lock();
			try {
				this.excludedElementsDatabase = db;
			} finally {
				rwlock.writeLock().unlock();
			}
		}
	}
	
	//End excluded elements database
	
	//Start simple miner database
	
	@Override
	public IACMinerDatabase getACMinerDB() {
		rwlock.readLock().lock();
		try {
			return aCMinerDatabase;
		} finally {
			rwlock.readLock().unlock();
		}
	}
	
	@Override
	public void setACMinerDB(IACMinerDatabase db) {
		if(db != null) {
			rwlock.writeLock().lock();
			try {
				this.aCMinerDatabase = db;
			} finally {
				rwlock.writeLock().unlock();
			}
		}
	}
	
	//End simple miner database
	
	//Start referenced entry points database
	
	@Override
	public IEntryPointEdgesDatabase getEntryPointEdgesDB() {
		rwlock.readLock().lock();
		try {
			return entryPointEdgesDatabase;
		} finally {
			rwlock.readLock().unlock();
		}
	}
	
	@Override
	public void setEntryPointEdgesDB(IEntryPointEdgesDatabase db) {
		if(db != null) {
			rwlock.writeLock().lock();
			try {
				this.entryPointEdgesDatabase = db;
			} finally {
				rwlock.writeLock().unlock();
			}
			
		}
	}
	
	//End referenced entry points database
	
	//Start structure of binder service database
	
	@Override
	public IBinderServicesDatabase getBinderServiceDB() {
		rwlock.readLock().lock();
		try {
			return binderServiceDatabase;
		} finally {
			rwlock.readLock().unlock();
		}
	}
	
	@Override
	public void setBinderServiceDB(IBinderServicesDatabase db) {
		if(db != null) {
			rwlock.writeLock().lock();
			try {
				this.binderServiceDatabase = db;
			} finally {
				rwlock.writeLock().unlock();
			}
			
		}
	}
	
	//End structure of binder service database
	
}
