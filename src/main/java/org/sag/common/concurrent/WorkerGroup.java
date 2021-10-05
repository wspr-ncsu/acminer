package org.sag.common.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkerGroup {
	protected static final AtomicInteger workerGroupNumber = new AtomicInteger(0); 
	protected final String name;
	protected volatile AtomicInteger count;
	protected final boolean shutdownOnError;
	protected volatile boolean isShutdown;
	protected volatile int state;
	protected final List<Throwable> errs;
	
	public WorkerGroup(boolean shutdownOnError) {
		this.count = new AtomicInteger(1);
		this.isShutdown = false;
		this.shutdownOnError = shutdownOnError;
		this.name = "WorkerGroup-" + workerGroupNumber.incrementAndGet();
		this.state = 0;
		this.errs = new ArrayList<>();
	}
	
	public synchronized void lock() {
		if(!isShutdown)
			count.incrementAndGet();
	}
	
	public void unlockInitialLock() {
		unlock(null);
	}
	
	public synchronized void unlock(Worker w) {
		if(!isShutdown) {
			Throwable t = null;
			if(w != null) {
				t = w.getException();
				endWorker(w);
			}
			if(t != null)
				errs.add(t);
			if(t != null && shutdownOnError) {
				this.state = 2;
				shutdownGroupLocked();
			} else {
				if(count.decrementAndGet() <= 0) {
					this.state = 1;
					shutdownGroupLocked();
				}
			}
		}
	}
	
	public synchronized void shutdownGroup() {
		if(!isShutdown) {
			this.state = 3;
			shutdownGroupLocked();
		}
	}
	
	public synchronized void addFailedToExecuteException(Throwable t) {
		errs.add(t);
	}
	
	private void shutdownGroupLocked() {
		count.set(0);
		this.isShutdown = true;
		endGroup();
	}
	
	public boolean isActive() {
		return state == 0;
	}
	
	/** This indicates that the group was shutdown normally. If shutdownOnError == true, then this means
	 * no exception occurred during the running of the group. Otherwise, it just means the group exited
	 * cleanly. Thus it is possible that one or more exceptions may have occurred. To determine if any errors occurred in this
	 * second situation, check {@link WorkerGroup#hasExceptions()}.
	 */
	public boolean shutdownNormally() {
		return state == 1;
	}
	
	/** If shutdownOnError == true and a exception occurs that causes the group to shutdown then
	 * this returns true. Otherwise, false.
	 */
	public boolean shutdownWithError() {
		return state == 2;
	}
	
	/** This indicates that the group was shutdown by force. This probably indicates that something
	 * bad happened but it is unclear what exactly that is. 
	 */
	public boolean shutdownByForce() {
		return state == 3;
	}
	
	public synchronized boolean hasExceptions() {
		return !errs.isEmpty();
	}
	
	public synchronized List<Throwable> getExceptions() {
		return new ArrayList<>(errs);
	}
	
	protected void endWorker(Worker w) {}
	protected void endGroup() {}
	
	public String toString() {
		return name;
	}
}