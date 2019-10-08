package org.sag.common.concurrent;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public class WorkerCountingThreadExecutor extends CountingThreadExecutor {
	
	protected volatile WorkerFactory workerFactory;
	//Uses a HashSet because we have to wrap it in a sync block anyways to ensure operations on it an isShutdown are atomic
	protected final Set<WorkerGroup> workerGroups;

	public WorkerCountingThreadExecutor() {
		this(new WorkerFactory());
	}
	
	public WorkerCountingThreadExecutor(WorkerFactory workerFactory) {
		super();
		this.workerFactory = workerFactory;
		this.workerGroups = new HashSet<>();
	}
	
	public WorkerCountingThreadExecutor(boolean shutdownOnError) {
		this(shutdownOnError, new WorkerFactory());
	}
	
	public WorkerCountingThreadExecutor(boolean shutdownOnError, WorkerFactory workerFactory) {
		super(shutdownOnError);
		this.workerFactory = workerFactory;
		this.workerGroups = new HashSet<>();
	}
	
	public WorkerCountingThreadExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, boolean shutdownOnError) {
		this(corePoolSize, maximumPoolSize, keepAliveTime, unit, shutdownOnError, new WorkerFactory());
	}
	
	public WorkerCountingThreadExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, boolean shutdownOnError, 
			WorkerFactory workerFactory) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, shutdownOnError);
		this.workerFactory = workerFactory;
		this.workerGroups = new HashSet<>();
	}
	
	@Override
	public boolean shutdownWhenFinished() {
		boolean ret = super.shutdownWhenFinished();
		if(shutdownOnError)
			shutdownWorkerGroups();
		return ret;
	}
	
	@Override
	public void execute(Runnable r) {
		execute((r instanceof Worker) ? (Worker)r : workerFactory.newWorker(r, this));
	}
	
	public void execute(Runnable r, WorkerGroup g) {
		execute((r instanceof Worker) ? (Worker)r : workerFactory.newWorker(r, g));
	}
	
	public void execute(Worker r) {
		try {
			r.startWorker();
			//once added can't say if someone else tries to add the same one after so we can't ever remove it
			//A group may be added while the worker is not (added -> shutdown -> execute)
			//However, once the executor is shutdown, no new group will be added
			synchronized(this) {
				if(!isShutdown())
					workerGroups.add(r.getWorkerGroup());
			}
			super.execute(r);
		} catch(RejectedExecutionException ex) {
			r.resetWorker();
			throw ex;
		}
	}
	
	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		((Worker)r).endWorker(t);
		super.afterExecute(r, t);
	}
	
	public void setWorkerFactory(WorkerFactory workerFactory) {
		if(workerFactory == null)
			throw new NullPointerException();
		this.workerFactory = workerFactory;
	}
	
	public WorkerFactory getWorkerFactory() {
		return workerFactory;
	}
	
	public synchronized Set<WorkerGroup> getWorkerGroups() {
		return new HashSet<>(workerGroups);
	}
	
	/** This method forcibly ends all worker groups that were used by the workers submitted to this
	 * executor. If shutdownOnError = false, then this will likely have no effect since all WorkerGroups
	 * should have shutdown automatically when their count reached 0. If it is true and an error occurs in
	 * one of the groups, however, only that WorkerGroup is guaranteed to end. All other groups may or may
	 * not end, depending on if there are any workers in the queue at the time of the exception. By calling
	 * this method we can end any still active WorkerGroup. Note this method is called internally by 
	 * {@link WorkerCountingThreadExecutor#shutdownWhenFinished()} if shutdownOnError is set. Note also, this
	 * method only has any effect is the executor is shutdown first. This ensures that no new groups will get
	 * added while we are trying to end them all.
	 */
	public synchronized void shutdownWorkerGroups() {
		if(isShutdown()) {
			for(WorkerGroup g : workerGroups) {
				g.shutdownGroup();
			}
		}
	}
	
	private class InnerWorkerGroup extends WorkerGroup {
		public InnerWorkerGroup() {
			super(false);
		}
		protected void endWorker(Worker w) {
			Throwable t = w.getException();
			if(w.exitedInError()) {
				if(t != null) {
					if(!(t instanceof IgnorableRuntimeException))
						errs.add(t);
				} else {
					errs.add(new RuntimeException(w.toString() + " exited with an unknown error."));
				}
			}
		}
	}
	 
	public static class WorkerFactory {
		/** Creates a new instance of a Worker using {@link WorkerCountingThreadExecutor.WorkerFactory#newWorker(Runnable, WorkerGroup)}.
		 * The {@link WorkerGroup} comes used comes from the current executing Runnable which is assumed to be a Worker instance. If it
		 * is not, this will fail with casting exceptions. The current executing Runnable will be a Worker when it is a thread inside
		 * a {@link WorkerCountingThreadExecutor} and is trying to spawn another Worker thread.
		 */
		public Worker newWorker(Runnable r, WorkerCountingThreadExecutor exe) {
			Thread curT = Thread.currentThread();
			if(curT instanceof CountingThread) {
				Runnable curR = ((CountingThread)curT).getCurrentRunnable();
				if(curR instanceof Worker) {
					return newWorker(r,((Worker)curR).getWorkerGroup());
				}
			}
			return newWorker(r,exe.new InnerWorkerGroup());
		}
		public Worker newWorker(Runnable r, WorkerGroup g) {
			return new Worker(r, g);
		}
	}
	
	public static long findPrimeNumber(int n) {
	    int count=0;
	    long a = 2;
	    while(count<n)
	    {
	        long b = 2;
	        int prime = 1;// to check if found a prime
	        while(b * b <= a)
	        {
	            if(a % b == 0)
	            {
	                prime = 0;
	                break;
	            }
	            b++;
	        }
	        if(prime > 0)
	        count++;
	        a++;
	    }
	    return (--a);
	}
	
}
