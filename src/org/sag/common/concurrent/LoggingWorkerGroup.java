package org.sag.common.concurrent;

import org.sag.common.logging.ILogger;
import org.sag.common.logging.LoggableRuntimeException;
import org.sag.main.logging.CentralLogger;

public class LoggingWorkerGroup extends WorkerGroup {
	
	protected final ILogger logger;
	protected final String name;
	protected final String phaseName;
	protected final boolean closeLogger;

	public LoggingWorkerGroup(String phaseName, String name, boolean shutdownOnError) {
		this(phaseName, name, getNewGroupLogger(phaseName, name), shutdownOnError, true);
	}
	
	public LoggingWorkerGroup(String phaseName, String name, ILogger logger, boolean shutdownOnError, boolean closeLogger) {
		super(shutdownOnError);
		this.logger = logger;
		this.name = name;
		this.phaseName = phaseName;
		this.closeLogger = closeLogger;
	}
	
	protected static ILogger getNewGroupLogger(String phaseName, String name) {
		String h = "--------------------------Start " + phaseName + " for " + name + "--------------------------\n";
		String t = "---------------------------End " + phaseName + " for " + name + "---------------------------\n";
		return CentralLogger.startLogger(name, CentralLogger.getLogPath(name), CentralLogger.getLogLevelOther(), h, t, true);
	}
	
	public String getName() {
		return name;
	}
	
	public ILogger getLogger() {
		return logger;
	}
	
	protected void endWorker(Worker w) {
		Throwable t = w.getException();
		if(w.exitedInError()) {
			if(t != null) {
				if(t instanceof LoggableRuntimeException) {
					((LoggableRuntimeException)t).log(logger);
				} else if(t instanceof IgnorableRuntimeException) {
					//do nothing as the error is already recorded internally
				} else {
					logger.fatal(w.toString() + " exited with the following error:",t);
				}
			} else {
				logger.fatal(w.toString() + " exited with an unknown error.");
			}
		}
	}
	
	protected void endGroup() {
		if(shutdownWithError()) {
			logger.fatal("{}",CountingThreadExecutor.computeJointErrorMsg(errs, "Group '" + name + "' encountered an error in one of its tasks "
					+ "while shutdown on error is set.",phaseName));
		} else if(shutdownByForce()) {
			logger.warn("{}: Group '{}' was shutdown by force.",phaseName,name);
			if(hasExceptions())
				logger.warn("{}",CountingThreadExecutor.computeJointErrorMsg(errs, "Group '" + name + "' -", phaseName));
		} else if(shutdownNormally()) {
			if(!hasExceptions()) {
				logger.info("{}: Successfully completed all tasks for group '{}'.",phaseName,name);
			} else {
				logger.fatal("{}: Failed to complete all tasks for group '{}'.",phaseName,name);
			}
		} else {
			logger.warn("{}: The group '{}' is still running ?!?",phaseName,name);
		}
		if(closeLogger)
			logger.close();
	}

}
