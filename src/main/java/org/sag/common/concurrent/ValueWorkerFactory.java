package org.sag.common.concurrent;

import org.sag.common.concurrent.WorkerCountingThreadExecutor.WorkerFactory;

public class ValueWorkerFactory<A> extends WorkerFactory {
	@SuppressWarnings({ "unchecked" })
	@Override
	public ValueWorker<A> newWorker(Runnable r, WorkerGroup g) {
		if(r instanceof ValueRunner && g instanceof ValueWorkerGroup)
			return new ValueWorker<A>((ValueRunner<A>)r,(ValueWorkerGroup<?,A>)g);
		throw new IllegalArgumentException("Error: Expected Runnable of super type 'ValueRunner' and WorkerGroup of super type "
				+ "'ValueWorkerGroup' but got '" + r.getClass().getName() + "' and '" + g.getClass().getName() + "'.");
	}
}
