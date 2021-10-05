package org.sag.acminer.phases.acminerdebug.handler;

import java.nio.file.Path;
import java.util.Objects;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.phases.acminerdebug.task.WriteFileTask;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.concurrent.WorkerCountingThreadExecutor;
import org.sag.common.concurrent.WorkerGroup;
import org.sag.common.logging.ILogger;
import org.sag.common.logging.LoggerWrapperSLF4J;

import soot.SootClass;

public abstract class AbstractEntryPointOutputHandler extends AbstractOutputHandler {

	protected final WorkerCountingThreadExecutor exe;
	//Used to launch tasks but not used in tasks
	protected int idCount;
	protected SootClass prevStub;
	
	public AbstractEntryPointOutputHandler(WorkerCountingThreadExecutor exe, Path rootOutputDir, IACMinerDataAccessor dataAccessor) {
		super(rootOutputDir,dataAccessor);
		Objects.requireNonNull(exe);
		this.exe = exe;
		this.idCount = 1;
		this.prevStub = null;
	}
	
	public boolean executeWorker(EntryPoint ep, WorkerGroup workerGroup, ILogger logger) {
		try {
			Objects.requireNonNull(ep);
			Objects.requireNonNull(workerGroup);
			if(logger == null)
				logger = new LoggerWrapperSLF4J(this.getClass());
			beforeRunningTask(ep,workerGroup,logger);
			exe.execute(getNewWriteFileTask(ep,logger),workerGroup);
		} catch(Throwable t) {
			onError(ep,workerGroup,logger,t);
			return false;
		}
		onSuccess(ep,workerGroup,logger);
		return true;
	}
	
	protected abstract void beforeRunningTask(EntryPoint ep, WorkerGroup workerGroup, ILogger logger);
	protected abstract WriteFileTask getNewWriteFileTask(EntryPoint ep, ILogger logger);
	protected abstract void onError(EntryPoint ep, WorkerGroup workerGroup, ILogger logger, Throwable t);
	protected abstract void onSuccess(EntryPoint ep, WorkerGroup workerGroup, ILogger logger);
}
