package org.sag.acminer.phases.acminerdebug.handler;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.phases.acminerdebug.task.CFGEntryPointWriteFileTask;
import org.sag.acminer.phases.acminerdebug.task.WriteFileTask;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.concurrent.WorkerCountingThreadExecutor;
import org.sag.common.concurrent.WorkerGroup;
import org.sag.common.graphtools.GraphmlGenerator;
import org.sag.common.logging.ILogger;

import soot.SootMethod;

public class CFGEntryPointOutputHandler extends AbstractEntryPointOutputHandler {
	
	private final GraphmlGenerator outGraphml;
	private Set<SootMethod> visited;
	
	public CFGEntryPointOutputHandler(WorkerCountingThreadExecutor exe, GraphmlGenerator outGraphml, Path rootOutputDir, IACMinerDataAccessor dataAccessor){
		super(exe,rootOutputDir,dataAccessor);
		this.outGraphml = outGraphml;
		this.visited = Collections.synchronizedSet(new HashSet<SootMethod>());
		this.idCount = 1;
		this.prevStub = null;
	}
	
	@Override
	protected void beforeRunningTask(EntryPoint ep, WorkerGroup workerGroup, ILogger logger) {
		if(prevStub == null || !prevStub.equals(ep.getStub())){
			this.prevStub = ep.getStub();
			this.visited = Collections.synchronizedSet(new HashSet<SootMethod>());
			this.idCount = 1;
		}
	}
	
	@Override
	protected void onError(EntryPoint ep, WorkerGroup workerGroup, ILogger logger, Throwable t) {
		logger.fatal("{}: Failed to create and start the CFGs writing task for for stub '{}' and entry point '{}'.",
				t,cn,ep.getStub(),ep.getEntryPoint());
		workerGroup.addFailedToExecuteException(t);
	}
	
	@Override
	protected void onSuccess(EntryPoint ep, WorkerGroup workerGroup, ILogger logger) {}
	
	@Override
	protected WriteFileTask getNewWriteFileTask(EntryPoint ep, ILogger logger) {
		return new CFGEntryPointWriteFileTask(ep,idCount++,visited,dataAccessor,rootOutputDir,outGraphml,cn,logger);
	}

}
