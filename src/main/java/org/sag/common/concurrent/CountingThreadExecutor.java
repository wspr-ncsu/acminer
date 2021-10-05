package org.sag.common.concurrent;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * A {@link ThreadPoolExecutor} which keeps track of the number of spawned
 * tasks to allow clients to await their completion. 
 */
public class CountingThreadExecutor extends ThreadPoolExecutor {

	protected final CountLatch numRunningTasks;
	protected volatile BlockingQueue<Throwable> errs;
	protected final boolean shutdownOnError;
	protected volatile int userSpecifiedCorePoolSize;
	
	/** Continues executing threads if an error is encountered in one of them. All errors are recorded in the
	 * errors list which can be retrieved through {@link CountingThreadExecutor#getAndClearExceptions()}.
	 */
	public CountingThreadExecutor() {
		this(false);
	}
	
	/** If shutdownOnError is set to false this behaves like {@link CountingThreadExecutor#CountingThreadExecutor()}. Otherwise,
	 * this executor will shutdown when the first thread terminates with an exception.
	 * 
	 * @param shutdownOnError
	 */
	public CountingThreadExecutor(boolean shutdownOnError) {
		this(1, Math.max(1,Runtime.getRuntime().availableProcessors()-3), 30, TimeUnit.SECONDS,shutdownOnError);
	}

	public CountingThreadExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, boolean shutdownOnError) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, new LinkedBlockingQueue<Runnable>(), new CountingThreadFactory());
		numRunningTasks = new CountLatch(0);
		errs = new LinkedBlockingQueue<>();
		this.shutdownOnError = shutdownOnError;
		this.userSpecifiedCorePoolSize = corePoolSize;
	}
	
	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		//Set the current runnable of the thread before executing it
		//Useful if the runnable is more complex and contains fields used by subclasses of this class
		((CountingThread)t).setCurrentRunnable(r);
		super.beforeExecute(t, r);
	}

	@Override
	public void execute(Runnable command) {
		try {
			numRunningTasks.increment();
			super.execute(command);
		}
		catch (RejectedExecutionException ex) {
			// If we were unable to submit the task, we may not count it!
			numRunningTasks.decrement();
			throw ex;
		}
	}
	
	/* This method is called by the thread that was executing the Runnable r and not the thread
	 * that submitted the Runnable r to this thread pool for running. When shutdownNow is called
	 * and resetAndInterrupt is run, this method will still be called by any thread that was currently
	 * executing. Anything that was still on the queue (i.e. had yet to be run) will not call this 
	 * method.
	 */
	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		if(shutdownOnError) {
			if(t == null){
				numRunningTasks.decrement();
			}else{
				errs.offer(t);
				shutdownNow();
				numRunningTasks.resetAndInterrupt();
			}
		} else {
			numRunningTasks.decrement();
			if(t != null)
				errs.offer(t);
		}
		super.afterExecute(r, t);
	}
	
	@Override
	public void setCorePoolSize(int corePoolSize) {
		this.userSpecifiedCorePoolSize = corePoolSize;
		setCorePoolSizeToRunningTaskCount();
	}
	
	protected synchronized void setCorePoolSizeToRunningTaskCount() {
		int threadCount = numRunningTasks.getCount();
		if (threadCount < userSpecifiedCorePoolSize) {
			threadCount = userSpecifiedCorePoolSize;
		} else if(threadCount > getMaximumPoolSize()){
			threadCount = getMaximumPoolSize();
		}
		super.setCorePoolSize(threadCount);
	}

	/**
	 * Awaits the completion of all spawned tasks.
	 */
	public void awaitCompletion() throws InterruptedException {
		numRunningTasks.awaitZero();
	}
	
	/**
	 * Awaits the completion of all spawned tasks.
	 */
	public void awaitCompletion(long timeout, TimeUnit unit) throws InterruptedException {
		numRunningTasks.awaitZero(timeout, unit);
	}
	
	public void clearExceptions() {
		errs.clear();
	}
	
	/** Returns the exceptions thrown during task executions (if any) and then
	 * clears the queue of Throwable. This method is safe to use even if this
	 * executor is still executing tasks.
	 */
	public List<Throwable> getAndClearExceptions() {
		List<Throwable> ret = new ArrayList<>();
		errs.drainTo(ret);
		return ret;
	}
	
	/** Initiates an orderly shutdown in which previously submitted tasks are executed, 
	 * but no new tasks will be accepted. Invocation has no additional effect if already 
	 * shut down. 
	 * 
	 * This method will wait for previously submitted tasks to complete execution by 
	 * calling {@link CountingThreadExecutor#awaitCompletion()} both before and after
	 * the call to {@link ThreadPoolExecutor#shutdown()}. Any exceptions that occur during
	 * the waiting and/or shutdown procedure will be added to the list of exceptions for 
	 * this {@link CountingThreadExecutor} and can be accessed through 
	 * {@link CountingThreadExecutor#getAndClearExceptions()}. However, a boolean is returned
	 * to indicate if the shutdown completed with out an exception being thrown.
	 */
	public boolean shutdownWhenFinished() {
		boolean ret = true;
		try {
			awaitCompletion();
		} catch(Throwable e) {
			ret = false;
			errs.offer(e);
		}
		
		try {
			super.shutdown();
		} catch(Throwable t) {
			ret = false;
			errs.offer(t);
		}
		
		try {
			awaitCompletion();
		} catch(Throwable e) {
			ret = false;
			errs.offer(e);
		}
		return ret;
	}
	
	public static void throwJointError(List<Throwable> errs, String errMsg) throws Exception {
		throw new Exception(computeJointErrorMsg(errs, errMsg));
	}
	
	public static void throwJointUnckeckedError(List<Throwable> errs, String errMsg) {
		throw new RuntimeException(computeJointErrorMsg(errs, errMsg));
	}
	
	public static String computeJointErrorMsg(List<Throwable> errs, String errMsg) {
		return computeJointErrorMsg(errs, errMsg, "Error");
	}
	
	public static String computeJointErrorMsg(List<Throwable> errs, String errMsg, String start) {
		String msg = "";
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try(PrintStream ps = new PrintStream(baos,true,"utf-8")){
			ps.println(start + ": " + errMsg + " The following exceptions were thrown in the process:");
			int i = 0;
			for(Throwable t : errs){
				ps.print("Exception ");
				ps.print(i++);
				ps.print(": ");
				t.printStackTrace(ps);
			}
			msg = new String(baos.toByteArray(), StandardCharsets.UTF_8);
		}catch(Throwable t){
			//This should never happen
		}
		return msg;
	}
	
	
	
	/**
	 * A synchronization aid similar to {@link CountDownLatch} but with the ability
	 * to also count up. This is useful to wait until a variable number of tasks
	 * have completed. {@link #awaitZero()} will block until the count reaches zero.
	 */
	protected class CountLatch {
		
		private final Sync sync;

		public CountLatch(int count) {
			this.sync = new Sync(count);
		}

		public void awaitZero() throws InterruptedException {
			sync.acquireShared(1);
		}

		public boolean awaitZero(long timeout, TimeUnit unit) throws InterruptedException {
			return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
		}

		public void increment() {
			sync.acquireNonBlocking(1);
			setCorePoolSizeToRunningTaskCount();
		}

		public void decrement() {
			sync.releaseShared(1);
			setCorePoolSizeToRunningTaskCount();
		}

		/**
		 * Resets the counter to zero. But waiting threads won't be released somehow.
		 * So this interrupts the threads so that they escape from their waiting state.
		 */
		public void resetAndInterrupt(){
			sync.reset();
			for (int i = 0; i < 3; i++) //Because it is a best effort thing, do it three times and hope for the best.
				for (Thread t : sync.getQueuedThreads())
					t.interrupt();
			sync.reset(); //Just in case a thread would've incremented the counter again.
			setCorePoolSizeToRunningTaskCount();
		}
		
		public int getCount() {
			return sync.getCount();
		}

		public String toString() {
			return super.toString() + "[Count = " + sync.getCount() + "]";
		}
	}
	
	@SuppressWarnings("serial")
	protected static final class Sync extends AbstractQueuedSynchronizer {

		Sync(int count) {
			setState(count);
		}

		int getCount() {
			return getState();
		}

		void reset() {
			setState(0);
		}

		@Override
		protected int tryAcquireShared(int acquires) {
			return (getState() == 0) ? 1 : -1;
		}

		protected int acquireNonBlocking(int acquires) {
			// increment count
			for (;;) {
				int c = getState();
				int nextc = c + 1;
				if (compareAndSetState(c, nextc))
					return 1;
			}
		}

		@Override
		protected boolean tryReleaseShared(int releases) {
			// Decrement count; signal when transition to zero
			for (;;) {
				int c = getState();
				if (c == 0)
					return false;
				int nextc = c - 1;
				if (compareAndSetState(c, nextc))
					return nextc == 0;
			}
		}
	}
	
	protected static class CountingThreadFactory implements ThreadFactory {
		private static final AtomicInteger poolNumber = new AtomicInteger(1);
		//Eat the exception because it is already recorded in afterExecute
		private static final UncaughtExceptionHandler ueh = new UncaughtExceptionHandler(){ 
			@Override public void uncaughtException(Thread t, Throwable e) {}
		};
		private final ThreadGroup group;
		private final AtomicInteger threadNumber = new AtomicInteger(1);
		private final String namePrefix;

		public CountingThreadFactory() {
			SecurityManager s = System.getSecurityManager();
			group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
			namePrefix = "CountingPool-" + poolNumber.getAndIncrement() + "_Thread-";
		}

		public Thread newThread(Runnable r) {
			Thread t = new CountingThread(group, r, namePrefix + threadNumber.getAndIncrement());
			if (t.isDaemon())
				t.setDaemon(false);
			if (t.getPriority() != Thread.NORM_PRIORITY)
				t.setPriority(Thread.NORM_PRIORITY);
			t.setUncaughtExceptionHandler(ueh);
			return t;
		}
	}
	
	//Used to store a accessible reference to the underlining runnable
	protected static class CountingThread extends Thread {
		
		protected volatile ThreadLocal<Runnable> currentRunnable;
		
		public CountingThread(ThreadGroup group, Runnable target, String name) {
			this(group, target, name, 0);
		}
		
		public CountingThread(ThreadGroup group, Runnable target, String name, long stackSize) {
			super(group, target, name);
			currentRunnable = new ThreadLocal<>();
		}
		
		public void setCurrentRunnable(Runnable r) {
			currentRunnable.set(r);
		}
		
		public Runnable getCurrentRunnable() {
			return currentRunnable.get();
		}
	}

}