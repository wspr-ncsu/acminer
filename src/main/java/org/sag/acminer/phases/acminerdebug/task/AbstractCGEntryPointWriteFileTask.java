package org.sag.acminer.phases.acminerdebug.task;

import java.nio.file.Path;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.concurrent.IgnorableRuntimeException;
import org.sag.common.graphtools.Formatter;
import org.sag.common.graphtools.GraphmlGenerator;
import org.sag.common.graphtools.Transformer;
import org.sag.common.logging.ILogger;

import soot.SootClass;
import soot.SootMethod;

public abstract class AbstractCGEntryPointWriteFileTask<A> extends AbstractEntryPointWriteFileTask {

	protected final int depth;
	protected final Transformer<A> trans;
	protected final GraphmlGenerator outGraphml;
	
	public AbstractCGEntryPointWriteFileTask(EntryPoint ep, int id, IACMinerDataAccessor dataAccessor, int depth, Path rootOutputDir, 
			GraphmlGenerator outGraphml, Transformer<A> trans, String cn, ILogger logger) {
		super(ep, id, dataAccessor, rootOutputDir, cn, logger);
		this.trans = trans;
		this.depth = depth;
		this.outGraphml = outGraphml;
	}
	
	@Override
	public void run() {
		SootMethod entryPoint = ep.getEntryPoint();
		SootClass stub = ep.getStub();
		Path stubOutputDir = getAndCreateStubOutputDir(stub);
		Path output = getOutputFilePath(stubOutputDir,entryPoint,id+"",".graphml");
		Formatter formatter = createAndFormatGraph(stub,entryPoint,output);
		runGraphOutputTask(stub,entryPoint,formatter);
	}
	
	protected abstract Formatter getFormatter(SootClass stub, SootMethod entryPoint, Path output);

	//Create the graph formatter and format the nodes for output
	protected Formatter createAndFormatGraph(SootClass stub, SootMethod entryPoint, Path output) {
		Formatter formatter = null;
		try {
			formatter = getFormatter(stub,entryPoint,output);
			formatter.format();
		} catch(Throwable t) {
			logger.fatal("{}: Failed to setup the formatter for stub '{}' and "
					+ "entry point '{}' with depth '{}' and output path '{}'.",t,cn,stub,entryPoint,depth == 0 ? "Unbounded" : depth,output);
			throw new IgnorableRuntimeException();
		}
		return formatter;
	}
	
	//Create and run task that writes the graph to file
	protected void runGraphOutputTask(SootClass stub, SootMethod entryPoint, Formatter formatter) {
		try {
			outGraphml.outputGraph(formatter);
		} catch(Throwable t) {
			logger.fatal("{}: Failed to create a new task to handle the writing of the call graph for stub '{}' and entry point '{}'"
					+ " with depth '{}' to path '{}'.",t,cn,stub,entryPoint,depth == 0 ? "Unbounded" : depth,formatter.getOutputPath());
			throw new IgnorableRuntimeException();
		}
		logger.fineInfo("{}: Successfully created a new task to handle the writing of the call graph for stub '{}' "
				+ "and entry point '{}' with depth '{}' at path '{}'. If no error occurs when writing the file this call graph was "
				+ "output successfully.",cn,stub,entryPoint,depth == 0 ? "Unbounded" : depth, formatter.getOutputPath());
	}
	
}
