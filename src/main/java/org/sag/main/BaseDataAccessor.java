package org.sag.main;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.sag.main.config.Config;
import org.sag.main.sootinit.SootInstanceWrapper;

public class BaseDataAccessor implements IDataAccessor {
	
	protected final Config config;
	protected final ReadWriteLock rwlock;
	
	public BaseDataAccessor(Config config) {
		this.config = config;
		this.rwlock = new ReentrantReadWriteLock();
		SootInstanceWrapper.v().setDataAccessor(this); //Register a newly created DataAccessor with the soot instance
	}
	
	@Override
	public Config getConfig() {
		return config;
	}
	
	@Override
	public final void resetAllSootData(boolean resetSootInstance) {
		rwlock.writeLock().lock();
		try {
			resetAllSootDataLocked(resetSootInstance);
		} finally {
			rwlock.writeLock().unlock();
		}
	}
	
	protected void resetAllSootDataLocked(boolean resetSootInstance) {
		if(resetSootInstance)
			SootInstanceWrapper.v().reset();
	}
	
	@Override
	public final void resetAllDatabasesAndData() {
		rwlock.writeLock().lock();
		try {
			resetAllSootDataLocked(true);
			resetAllDatabasesAndDataLocked();
		} finally {
			rwlock.writeLock().unlock();
		}
	}
	
	protected void resetAllDatabasesAndDataLocked() {
		
	}

}
