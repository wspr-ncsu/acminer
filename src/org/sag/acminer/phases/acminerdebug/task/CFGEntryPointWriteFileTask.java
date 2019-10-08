package org.sag.acminer.phases.acminerdebug.task;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.concurrent.IgnorableRuntimeException;
import org.sag.common.graphtools.Formatter;
import org.sag.common.graphtools.GraphmlGenerator;
import org.sag.common.graphtools.AlElement.Color;
import org.sag.common.logging.ILogger;
import org.sag.common.tools.SortingMethods;
import org.sag.common.tuple.Pair;
import org.sag.soot.SootSort;
import org.sag.soot.graphtools.transformers.SootCFGTransformer;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;

import com.google.common.collect.Iterables;

public class CFGEntryPointWriteFileTask extends AbstractEntryPointWriteFileTask {
	protected final Set<SootMethod> visited;
	protected final GraphmlGenerator outGraphml;
	
	public CFGEntryPointWriteFileTask(EntryPoint ep, int id, Set<SootMethod> visited, IACMinerDataAccessor dataAccessor, Path rootOutputDir, 
			GraphmlGenerator outGraphml, String cn, ILogger logger) {
		super(ep, id, dataAccessor, rootOutputDir, cn, logger);
		this.visited = visited;
		this.outGraphml = outGraphml;
	}
	
	@Override
	public void run() {
		SootClass stub = ep.getStub();
		Map<SootMethod,UnitGraph> methodsWithAuthLogic = getMethodsWithAuthLogic(ep);
		if(!methodsWithAuthLogic.isEmpty()) {
			Path stubOutputDir = getAndCreateStubOutputDir(stub);
			SootCFGTransformer trans = getNewTransformer(ep, methodsWithAuthLogic);
			long edgesColorIndex = setEdgeColorMap(ep, trans);
			long nodesColorIndex = setNodeColorMap(ep, trans);
			List<Formatter> formatters = makeFormatters(ep, stubOutputDir, methodsWithAuthLogic, trans, edgesColorIndex, nodesColorIndex);
			runGraphOutputTask(ep,formatters);
		}
		
	}
	
	protected Map<SootMethod,UnitGraph> getMethodsWithAuthLogic(EntryPoint ep) {
		logger.fineInfo("{}: Retrieving methods with authorization logic for entry point '{}'.",cn,ep);
		Map<SootMethod,UnitGraph> methodsWithAuthLogic = null;
		try {
			methodsWithAuthLogic = new HashMap<>();
			for(SootMethod m : Iterables.concat(dataAccessor.getControlPredicatesDB().getSources(ep),
					dataAccessor.getContextQueriesDB().getSources(ep),
					dataAccessor.getThrowSecurityExceptionStmtsDB().getSources(ep))) {
				if(!methodsWithAuthLogic.containsKey(m)){
					methodsWithAuthLogic.put(m, new ExceptionalUnitGraph(m.retrieveActiveBody()));
				}
			}
			methodsWithAuthLogic = SortingMethods.sortMapKey(methodsWithAuthLogic,SootSort.smComp);
		} catch(Throwable t) {
			logger.fineInfo("{}: Failed to retrieved methods with authorization logic for entry point '{}'.",cn,ep);
			throw new IgnorableRuntimeException();
		}
		logger.fineInfo("{}: Successfully retrieved methods with authorization logic for entry point '{}'.",cn,ep);
		return methodsWithAuthLogic;
	}
	
	protected SootCFGTransformer getNewTransformer(EntryPoint ep, Map<SootMethod,UnitGraph> methodsWithAuthLogic) {
		logger.fineInfo("{}: Setting up the transformer for entry point '{}.",cn,ep);
		SootCFGTransformer trans;
		try {
			trans = new SootCFGTransformer(methodsWithAuthLogic);
			trans.transform();
		} catch(Throwable t) {
			logger.fatal("{}: Failed to setup the transformer for entry point '{}.",cn,ep);
			throw new IgnorableRuntimeException();
		}
		logger.fineInfo("{}: Successfully setup the transformer for entry point '{}.",cn,ep);
		return trans;
	}
	
	protected long setEdgeColorMap(EntryPoint ep, SootCFGTransformer trans) {
		logger.fineInfo("{}: Setting edge colors for entry point '{}.",cn,ep);
		try {
			Map<Pair<Unit,Unit>,Color> edgeColorMap = new HashMap<>();
			setupEdgeColorMap(edgeColorMap, trans.getExceptionEdges(), Color.RED);
			long ret = trans.applyColorsToEdges(edgeColorMap);
			logger.fineInfo("{}: Successfully set edge colors for entry point '{}.",cn,ep);
			return ret;
		} catch(Throwable t) {
			logger.fineInfo("{}: Failed to set edge colors for entry point '{}.",cn,ep);
			throw new IgnorableRuntimeException();
		}
	}
	
	protected long setNodeColorMap(EntryPoint ep, SootCFGTransformer trans) {
		logger.fineInfo("{}: Setting node colors for entry point '{}.",cn,ep);
		try {
			Map<Unit,List<Color>> nodeColorMap = new HashMap<>();
			setupNodeColorMap(nodeColorMap, trans.getHeads(), Color.GREEN);
			setupNodeColorMap(nodeColorMap, dataAccessor.getControlPredicatesDB().getUnits(ep), Color.BLUE);
			setupNodeColorMap(nodeColorMap, dataAccessor.getContextQueriesDB().getUnits(ep), Color.ORANGE);
			setupNodeColorMap(nodeColorMap, dataAccessor.getThrowSecurityExceptionStmtsDB().getUnits(ep), Color.RED);
			long ret = trans.applyColorsToNodes(nodeColorMap);
			logger.fineInfo("{}: Successfully set node colors for entry point '{}.",cn,ep);
			return ret;
		} catch(Throwable t) {
			logger.fineInfo("{}: Failed to set node colors for entry point '{}.",cn,ep);
			throw new IgnorableRuntimeException();
		}
	}
	
	protected List<Formatter> makeFormatters(EntryPoint ep, Path stubOutputDir, 
			Map<SootMethod,UnitGraph> methodsWithAuthLogic, SootCFGTransformer trans, long edgesColorIndex, long nodesColorIndex) {
		logger.fineInfo("{}: Initilizing the graph formatters for entry point '{}'.",cn,ep);
		List<Formatter> formatters = null;
		try {
			formatters = new ArrayList<>();
			int count = 1;
			for(SootMethod m : methodsWithAuthLogic.keySet()) {
				if(visited.add(m)) {
					Path output = getOutputFilePath(stubOutputDir, m, id + "_" + count++,".graphml");
					Formatter f = trans.getFormatter(nodesColorIndex,edgesColorIndex,output,m);
					if(f == null){
						logger.fatal("{}: Failed to retrieve a formatter for method of entry point '{}'.",cn,m,ep);
						throw new IgnorableRuntimeException();
					}
					formatters.add(f);
				}
			}
		} catch(Throwable t) {
			if(t instanceof IgnorableRuntimeException) {
				throw t;
			} else {
				logger.fatal("{}: An unexpected error occured during the formatter initilization for entry point '{}'.",cn,ep);
				throw new IgnorableRuntimeException();
			}
		}
		logger.fineInfo("{}: Successfully initilized the graph formatters for entry point '{}'.",cn,ep);
		return formatters;
	}
	
	//Create and run task that writes the graph to file
	protected void runGraphOutputTask(EntryPoint ep, List<Formatter> formatters) {
		try {
			outGraphml.outputGraphs(formatters);
		} catch(Throwable t) {
			logger.fatal("{}: Failed to create all the new tasks to handle the writing of the CFGs for entry point '{}'.",t,cn,ep);
			throw new IgnorableRuntimeException();
		}
		logger.fineInfo("{}: Successfully created all the new tasks to handle the writing of the CFGs for "
				+ "entry point '{}'. If no error occurs when writing the files the all the CFGs were "
				+ "output successfully.",cn,ep);
	}
	
	protected <A> void setupNodeColorMap(Map<A,List<Color>> colorMap, Iterable<A> nodes, Color color) {
		for(A m : nodes) {
			List<Color> colors = colorMap.get(m);
			if(colors == null) {
				colors = new ArrayList<>();
				colorMap.put(m, colors);
			}
			colors.add(color);
		}
	}
	
	protected <A> void setupEdgeColorMap(Map<A,Color> colorMap, Iterable<A> nodes, Color color) {
		for(A m : nodes) {
			colorMap.put(m,color);
		}
	}
	
}