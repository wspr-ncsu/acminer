package org.sag.common.concurrent;

import org.sag.common.logging.ILogger;

public class SimpleValueWorkerGroup extends ValueWorkerGroup<Object,Object> {

	public SimpleValueWorkerGroup(String phaseName, String name, boolean shutdownOnError) {
		super(phaseName, name, shutdownOnError);
	}

	public SimpleValueWorkerGroup(String phaseName, String name, ILogger logger, boolean shutdownOnError, boolean closeLogger) {
		super(phaseName, name, logger, shutdownOnError, closeLogger);
	}

	@Override
	protected Object initReturnValue() {
		return new Object();
	}

	@Override
	protected void joinWorkerReturnValue(Object value) {}

	@Override
	protected void finalizeReturnValue() {}

}
