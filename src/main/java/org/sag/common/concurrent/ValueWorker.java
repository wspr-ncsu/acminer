package org.sag.common.concurrent;

public class ValueWorker<A> extends Worker {

	public ValueWorker(ValueRunner<A> runner, ValueWorkerGroup<?,A> group) {
		super(runner, group);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ValueRunner<A> getRunner() {
		return (ValueRunner<A>)super.getRunner();
	}

	public A getValue() {
		return getRunner().getValue();
	}
	
}
