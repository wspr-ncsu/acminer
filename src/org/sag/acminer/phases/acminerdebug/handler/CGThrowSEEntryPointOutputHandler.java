package org.sag.acminer.phases.acminerdebug.handler;

import java.nio.file.Path;
import java.util.Map;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.phases.acminerdebug.task.CGMethodEntryPointWriteFileTask;
import org.sag.acminer.phases.acminerdebug.task.WriteFileTask;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.concurrent.WorkerCountingThreadExecutor;
import org.sag.common.graphtools.GraphmlGenerator;
import org.sag.common.graphtools.Transformer;
import org.sag.common.logging.ILogger;

import soot.SootMethod;

//Same as CGMethodOutputHandler but we make a separate class so the logger uses this class name and also keeps with the format
//of separate handlers for each type of debugging
public class CGThrowSEEntryPointOutputHandler extends CGMethodEntryPointOutputHandler {
	
	private final Map<SootMethod,Long> epToIndex;
	
	public CGThrowSEEntryPointOutputHandler(WorkerCountingThreadExecutor exe, GraphmlGenerator outGraphml, Path rootOutputDir, 
			Transformer<SootMethod> trans, Map<SootMethod,Long> epToIndex, IACMinerDataAccessor dataAccessor) {
		super(exe, outGraphml, rootOutputDir, trans, 0, 0, dataAccessor);
		this.epToIndex = epToIndex;
	}
	
	@Override
	protected WriteFileTask getNewWriteFileTask(EntryPoint ep, ILogger logger) {
		return new CGMethodEntryPointWriteFileTask(ep,idCount++,dataAccessor,depth,rootOutputDir,outGraphml,trans,epToIndex.get(ep.getEntryPoint()),cn,logger);
	}

}
