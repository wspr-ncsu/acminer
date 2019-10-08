package org.sag.common.concurrent;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class Worker implements Runnable {
	protected static final AtomicInteger workerNumber = new AtomicInteger(0);
	protected final String name;
	protected final WorkerGroup group;
	protected final Runnable runner;
	protected volatile Throwable exception;
	protected volatile int state;
	public Worker(Runnable runner, WorkerGroup group) {
		this.group = group;
		this.runner = runner;
		this.exception = null;
		this.state = 0;
		this.name = "Worker-" + workerNumber.incrementAndGet();
	}
	public WorkerGroup getWorkerGroup() { return group; }
	public Runnable getRunner() { return runner; }
	@Override public void run() { runner.run(); }
	public void startWorker() {
		if(this.state == 0) {
			this.state = 1;
			group.lock();
		}
	}
	public void resetWorker() {
		if(this.state == 1) {
			this.state = 0;
			group.unlock(null);
		}
	}
	public void endWorker(Throwable t) {
		if(this.state == 1) {
			this.exception = t;
			this.state = t == null ? 2 : 3;
			group.unlock(this);
		}
	}
	public boolean wasRun() {
		return this.state != 0;
	}
	public boolean isRunning() {
		return this.state == 1;
	}
	public boolean exitedInError() {
		return this.state == 3;
	}
	public boolean exitedInSuccess() {
		return this.state == 2;
	}
	public boolean hasException() {
		return exception != null;
	}
	public Throwable getException() {
		return exception;
	}
	public String getName() {
		return name;
	}
	public String toString() {
		return name + "_" + Objects.toString(runner);
	}
}