package org.sag.acminer.phases.acminerdebug.task;

import java.nio.file.Path;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.graphtools.Formatter;
import org.sag.common.graphtools.GraphmlGenerator;
import org.sag.common.graphtools.Transformer;
import org.sag.common.logging.ILogger;
import org.sag.soot.graphtools.formatter.SootCGClassFormatter;

import soot.SootClass;
import soot.SootMethod;

public class CGClassEntryPointWriteFileTask extends AbstractCGEntryPointWriteFileTask<SootClass> {
	
	protected final long nodeColorIndex;

	public CGClassEntryPointWriteFileTask(EntryPoint ep, int id, IACMinerDataAccessor dataAccessor, int depth, Path rootOutputDir, 
			GraphmlGenerator outGraphml, Transformer<SootClass> trans, long nodeColorIndex, String cn, ILogger logger) {
		super(ep, id, dataAccessor, depth, rootOutputDir, outGraphml, trans, cn, logger);
		
		this.nodeColorIndex = nodeColorIndex;
		
	}

	@Override
	protected Formatter getFormatter(SootClass stub, SootMethod entryPoint, Path output) {
		return new SootCGClassFormatter(stub,entryPoint,trans,depth,nodeColorIndex,output,dataAccessor);
	}
	
}