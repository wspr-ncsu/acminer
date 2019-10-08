package org.sag.acminer.phases.acminerdebug.handler;

import java.nio.file.Path;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.phases.acminerdebug.task.CGMethodEntryPointWriteFileTask;
import org.sag.acminer.phases.acminerdebug.task.WriteFileTask;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.concurrent.WorkerCountingThreadExecutor;
import org.sag.common.graphtools.GraphmlGenerator;
import org.sag.common.graphtools.Transformer;
import org.sag.common.logging.ILogger;

import soot.SootMethod;

public class CGMethodEntryPointOutputHandler extends AbstractCGOutputHandler<SootMethod> {
	
	public CGMethodEntryPointOutputHandler(WorkerCountingThreadExecutor exe, GraphmlGenerator outGraphml, Path rootOutputDir, 
			Transformer<SootMethod> trans, int depth, long nodeColorIndex, IACMinerDataAccessor dataAccessor) {
		super(exe, outGraphml, rootOutputDir, trans, depth, nodeColorIndex, dataAccessor);
	}
	
	@Override
	protected WriteFileTask getNewWriteFileTask(EntryPoint ep, ILogger logger) {
		return new CGMethodEntryPointWriteFileTask(ep,idCount++,dataAccessor,depth,rootOutputDir,outGraphml,trans,nodeColorIndex,cn,logger);
	}

}
