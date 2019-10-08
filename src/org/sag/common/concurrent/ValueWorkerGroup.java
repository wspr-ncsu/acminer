package org.sag.common.concurrent;

import java.util.concurrent.atomic.AtomicInteger;

import org.sag.common.logging.ILogger;

public abstract class ValueWorkerGroup<A,B> extends LoggingWorkerGroup {
	
	protected final A ret;
	protected AtomicInteger signal;

	public ValueWorkerGroup(String phaseName, String name, boolean shutdownOnError) {
		super(phaseName, name, shutdownOnError);
		this.ret = initReturnValue();
		this.signal = new AtomicInteger(0);
	}
	
	public ValueWorkerGroup(String phaseName, String name, ILogger logger, boolean shutdownOnError, boolean closeLogger) {
		super(phaseName,name,logger,shutdownOnError,closeLogger);
		this.ret = initReturnValue();
		this.signal = new AtomicInteger(0);
	}
	
	protected abstract A initReturnValue();
	protected abstract void joinWorkerReturnValue(B value);
	protected abstract void finalizeReturnValue();
	
	/** Gets the returned value. If the group is currently still active then this method
	 * blocks the current thread until all threads in the group have exited. Otherwise, this
	 * returns whatever is assigned as the return value regardless of the exit state of the
	 * group.
	 */
	public A getReturnValue() {
		synchronized(signal) {
			while(signal.get() == 0) {
				try {
					signal.wait();
				} catch (InterruptedException e) {}//eat it
			}
		}
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void endWorker(Worker w) {
		if(!w.exitedInError()) {
			synchronized(ret) {
				joinWorkerReturnValue(((ValueWorker<B>)w).getValue());//if the worker exited in error then there should be no value
			}
		}
		super.endWorker(w);
	}
	
	@Override
	protected void endGroup() {
		synchronized(ret) {
			finalizeReturnValue();//finalize whatever we have even if there are errors
		}
		super.endGroup();
		synchronized(signal) {
			signal.incrementAndGet();
			signal.notifyAll();
		}
	}

}
