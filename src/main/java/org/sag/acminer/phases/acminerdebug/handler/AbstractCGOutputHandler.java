package org.sag.acminer.phases.acminerdebug.handler;

import java.nio.file.Path;
import java.util.Objects;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.concurrent.WorkerCountingThreadExecutor;
import org.sag.common.concurrent.WorkerGroup;
import org.sag.common.graphtools.GraphmlGenerator;
import org.sag.common.graphtools.Transformer;
import org.sag.common.logging.ILogger;

public abstract class AbstractCGOutputHandler<A> extends AbstractEntryPointOutputHandler {
	
	protected final GraphmlGenerator outGraphml;
	protected final Transformer<A> trans;
	protected final long nodeColorIndex;
	protected final int depth;
	
	public AbstractCGOutputHandler(WorkerCountingThreadExecutor exe, GraphmlGenerator outGraphml, Path rootOutputDir, Transformer<A> trans, 
			int depth, long nodeColorIndex, IACMinerDataAccessor dataAccessor) {
		super(exe,rootOutputDir,dataAccessor);
		Objects.requireNonNull(outGraphml);
		Objects.requireNonNull(trans);
		this.outGraphml = outGraphml;
		this.trans = trans;
		this.nodeColorIndex = nodeColorIndex;
		this.depth = depth;
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
		logger.fatal("{}: Failed to create and start the graph writing task for for stub '{}' and entry point '{}' with depth '{}'",
				t,cn,ep.getStub(),ep.getEntryPoint(),depth == 0 ? "Unbounded" : depth);
		workerGroup.addFailedToExecuteException(t);
	}
	
	@Override
	protected void onSuccess(EntryPoint ep, WorkerGroup workerGroup, ILogger logger) {}
	
}
