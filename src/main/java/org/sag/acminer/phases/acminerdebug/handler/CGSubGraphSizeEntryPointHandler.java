package org.sag.acminer.phases.acminerdebug.handler;

import java.nio.file.Path;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.phases.acminerdebug.task.CGSubGraphSizeEntryPointFileTask;
import org.sag.acminer.phases.acminerdebug.task.WriteFileTask;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.concurrent.WorkerCountingThreadExecutor;
import org.sag.common.concurrent.WorkerGroup;
import org.sag.common.logging.ILogger;

import soot.Scene;
import soot.jimple.toolkits.callgraph.CallGraph;

public class CGSubGraphSizeEntryPointHandler extends AbstractEntryPointOutputHandler {
	
	private final CallGraph cg;

	public CGSubGraphSizeEntryPointHandler(WorkerCountingThreadExecutor exe, Path rootOutputDir, IACMinerDataAccessor dataAccessor){
		super(exe,rootOutputDir,dataAccessor);
		this.cg = Scene.v().getCallGraph();
	}
	
	@Override
	protected void beforeRunningTask(EntryPoint ep, WorkerGroup workerGroup, ILogger logger) {
		if(prevStub == null || !prevStub.equals(ep.getStub())){
			this.prevStub = ep.getStub();
			this.idCount = 1;
		}
	}
	
	@Override
	protected void onError(EntryPoint ep, WorkerGroup workerGroup, ILogger logger, Throwable t) {
		logger.fatal("{}: Failed to create and start the call graph subgraph writing task for for stub '{}' and entry point '{}'.",
				t,cn,ep.getStub(),ep.getEntryPoint());
		workerGroup.addFailedToExecuteException(t);
	}
	
	@Override
	protected void onSuccess(EntryPoint ep, WorkerGroup workerGroup, ILogger logger) {}
	
	@Override
	protected WriteFileTask getNewWriteFileTask(EntryPoint ep, ILogger logger) {
		return new CGSubGraphSizeEntryPointFileTask(ep,idCount++,cg,dataAccessor,rootOutputDir,cn,logger);
	}

}
