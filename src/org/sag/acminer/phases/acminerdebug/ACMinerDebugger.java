package org.sag.acminer.phases.acminerdebug;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.phases.acminerdebug.handler.AbstractEntryPointOutputHandler;
import org.sag.acminer.phases.acminerdebug.handler.CFGEntryPointOutputHandler;
import org.sag.acminer.phases.acminerdebug.handler.CGClassEntryPointOutputHandler;
import org.sag.acminer.phases.acminerdebug.handler.CGInacEntryPointOutputHandler;
import org.sag.acminer.phases.acminerdebug.handler.CGMethodEntryPointOutputHandler;
import org.sag.acminer.phases.acminerdebug.handler.CGSubGraphSizeEntryPointHandler;
import org.sag.acminer.phases.acminerdebug.handler.CGThrowSEEntryPointOutputHandler;
import org.sag.acminer.phases.acminerdebug.handler.DataDumpsOutputHandler;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.concurrent.CountingThreadExecutor;
import org.sag.common.concurrent.LoggingWorkerGroup;
import org.sag.common.concurrent.WorkerCountingThreadExecutor;
import org.sag.common.graphtools.GraphmlGenerator;
import org.sag.common.graphtools.AlElement.Color;
import org.sag.common.logging.ILogger;
import org.sag.main.config.Config;
import org.sag.main.phase.IPhaseHandler;
import org.sag.main.phase.IPhaseOption;
import org.sag.soot.graphtools.transformers.SootCGClassTransformer;
import org.sag.soot.graphtools.transformers.SootCGMethodTransformer;

import soot.SootClass;
import soot.SootMethod;

public class ACMinerDebugger {
	
	private final IPhaseHandler handler;
	private final ILogger mainLogger;
	private final IACMinerDataAccessor dataAccessor;
	private final String cn;
	private final Config config;
	
	public ACMinerDebugger(IACMinerDataAccessor dataAccessor, IPhaseHandler handler, ILogger mainLogger){
		this.dataAccessor = dataAccessor;
		this.handler = handler;
		this.mainLogger = mainLogger;
		this.cn = this.getClass().getSimpleName();
		this.config = dataAccessor.getConfig();
	}
	
	private boolean isOptionEnabled(String name) {
		IPhaseOption<?> allO = handler.getPhaseOptionUnchecked(ACMinerDebugHandler.all);
		if(allO == null || !allO.isEnabled()) {
			IPhaseOption<?> o = handler.getPhaseOptionUnchecked(name);
			if(o == null || !o.isEnabled())
				return false;
		}
		return true;
	}
	
	private <A> void setupColorMap(Map<A,List<Color>> colorMap, Iterable<A> nodes, Color color) {
		for(A m : nodes) {
			List<Color> colors = colorMap.get(m);
			if(colors == null) {
				colors = new ArrayList<>();
				colorMap.put(m, colors);
			}
			colors.add(color);
		}
	}
	
	public boolean run(){
		Deque<EntryPoint> eps = new ArrayDeque<>(dataAccessor.getEntryPoints());
		List<AbstractEntryPointOutputHandler> epDepHandlers = new ArrayList<>();
		GraphmlGenerator outGraphml = null;
		WorkerCountingThreadExecutor exe = new WorkerCountingThreadExecutor();
		SootCGMethodTransformer transMethod = null;
		SootCGClassTransformer transClass = null;
		boolean cqSubGraphEnabled = isOptionEnabled(ACMinerDebugHandler.optCQSubGraph);
		boolean commonSubGraphsEnabled = isOptionEnabled(ACMinerDebugHandler.commonSubgraphs);
		boolean pathsToMethodsEnabled = isOptionEnabled(ACMinerDebugHandler.pathsToMethods);
		boolean subGraphCountEnabled = isOptionEnabled(ACMinerDebugHandler.optSubGraphCount);
		boolean pathsEnabled = isOptionEnabled(ACMinerDebugHandler.paths);
		boolean cgInacEnabled = isOptionEnabled(ACMinerDebugHandler.cgInac);
		boolean cgSubGraphDataEnabled = isOptionEnabled(ACMinerDebugHandler.cgSubGraphData);
		boolean cgMethodEnabled = isOptionEnabled(ACMinerDebugHandler.cgMethod);
		boolean cgMethodLimitEnabled = isOptionEnabled(ACMinerDebugHandler.cgMethodLimit);
		boolean cgClassEnabled = isOptionEnabled(ACMinerDebugHandler.cgClass);
		boolean cgClassLimitEnabled =  isOptionEnabled(ACMinerDebugHandler.cgClassLimit);
		boolean throwSEEnabled = isOptionEnabled(ACMinerDebugHandler.cgThrowSE);
		boolean cfgEnabled = isOptionEnabled(ACMinerDebugHandler.cfg);
		boolean dataDumpsEnabled = isOptionEnabled(ACMinerDebugHandler.dataDumps);
		long cgMethodColorMapIndex = -1;
		long cgClassColorMapIndex = -1;
		boolean successOuter = true;
		
		mainLogger.info("{}: Begin generating debug output.",cn);
		
		if(cgSubGraphDataEnabled) {
			epDepHandlers.add(new CGSubGraphSizeEntryPointHandler(exe, config.getFilePath("debug_cg-subgraph-data-dir"), dataAccessor));
		}
		
		if(cgInacEnabled){
			epDepHandlers.add(new CGInacEntryPointOutputHandler(exe,config.getFilePath("debug_cg-inac-dir"),dataAccessor));
		}
		
		if(cfgEnabled || cgMethodEnabled || cgMethodLimitEnabled || cgClassEnabled || cgClassLimitEnabled ||throwSEEnabled || 
				commonSubGraphsEnabled || subGraphCountEnabled || cqSubGraphEnabled || pathsEnabled){
			outGraphml = new GraphmlGenerator(exe);
		}
		
		if(cfgEnabled)
			epDepHandlers.add(new CFGEntryPointOutputHandler(exe, outGraphml, config.getFilePath("debug_cfg-dir"),dataAccessor));
		
		if(cgMethodEnabled || cgMethodLimitEnabled || cgClassEnabled || cgClassLimitEnabled || throwSEEnabled) {
			//Compute some required sets
			Set<SootMethod> epsSootMethods = dataAccessor.getEntryPointsAsSootMethods();
			Set<SootMethod> sourceMethods = new HashSet<>();
			Set<SootMethod> targetMethods = new HashSet<>();
			
			if(cgMethodEnabled || cgMethodLimitEnabled || throwSEEnabled) {
				mainLogger.info("{}: Initlizing Soot to yEd CallGraph transformer for methods...",cn);
				transMethod = new SootCGMethodTransformer(epsSootMethods);
				transMethod.transform();
				mainLogger.info("{}: Soot to yEd CallGraph transformer for methods initilized.",cn);
			}
			
			if(cgClassEnabled || cgClassLimitEnabled) {
				mainLogger.info("{}: Initlizing Soot to yEd CallGraph transformer for classes...",cn);
				transClass = new SootCGClassTransformer(epsSootMethods);
				transClass.transform();
				mainLogger.info("{}: Soot to yEd CallGraph transformer for classes initilized.",cn);
			}
			
			if(cgMethodEnabled || cgMethodLimitEnabled) {
				mainLogger.info("{}: Initlizing the color map for CGMethod and/or CGMethodLimit...",cn);
				Map<SootMethod,List<Color>> colorMap = new HashMap<>();
				setupColorMap(colorMap,epsSootMethods,Color.GREEN);
				setupColorMap(colorMap,dataAccessor.getControlPredicatesDB().getSources(),Color.BLUE);
				setupColorMap(colorMap,sourceMethods,Color.ORANGE);
				setupColorMap(colorMap,targetMethods,Color.RED);
				mainLogger.info("{}: Color map for CGMethod and/or CGMethodLimit initilized.",cn);
				mainLogger.info("{}: Applying color map to Soot to yEd CallGraph transformer for methods...",cn);
				cgMethodColorMapIndex = transMethod.applyColorsToNodes(colorMap);
				mainLogger.info("{}: Color map applied to Soot to yEd CallGraph transformer for methods.",cn);
			}
			
			if(cgClassEnabled || cgClassLimitEnabled) {
				mainLogger.info("{}: Initlizing the color map for CGClass and/or CGClassLimit...",cn);
				Map<SootClass,List<Color>> colorMap = new HashMap<>();
				Set<SootClass> temp1 = new HashSet<>();
				Set<SootClass> sourceClasses = new HashSet<>();
				Set<SootClass> targetClasses = new HashSet<>();
				for(SootMethod m : dataAccessor.getControlPredicatesDB().getSources()) {
					temp1.add(m.getDeclaringClass());
				}
				for(SootMethod m : sourceMethods) {
					sourceClasses.add(m.getDeclaringClass());
				}
				for(SootMethod m : targetMethods) {
					targetClasses.add(m.getDeclaringClass());
				}
				setupColorMap(colorMap,dataAccessor.getEntryPointsByDeclaringClass().keySet(),Color.GREEN);
				setupColorMap(colorMap,temp1,Color.BLUE);
				setupColorMap(colorMap,sourceClasses,Color.ORANGE);
				setupColorMap(colorMap,targetClasses,Color.RED);
				mainLogger.info("{}: Color map for CGClass and/or CGClassLimit initilized.",cn);
				mainLogger.info("{}: Applying color map to Soot to yEd CallGraph transformer for classes...",cn);
				cgClassColorMapIndex = transClass.applyColorsToNodes(colorMap);
				mainLogger.info("{}: Color map applied to Soot to yEd CallGraph transformer for classes.",cn);
			}
			
			if(cgMethodEnabled)
				epDepHandlers.add(new CGMethodEntryPointOutputHandler(exe, outGraphml, config.getFilePath("debug_cg-method-dir"),
						transMethod, 0, cgMethodColorMapIndex, dataAccessor));
			if(cgMethodLimitEnabled)
				epDepHandlers.add(new CGMethodEntryPointOutputHandler(exe, outGraphml,config.getFilePath("debug_cg-method-limit-dir"),
						transMethod,(Integer)handler.getPhaseOptionUnchecked(ACMinerDebugHandler.cgMethodLimit).getValue(), cgMethodColorMapIndex, 
						dataAccessor));
			if(cgClassEnabled)
				epDepHandlers.add(new CGClassEntryPointOutputHandler(exe, outGraphml,  config.getFilePath("debug_cg-class-dir"),
						transClass, 0, cgClassColorMapIndex, dataAccessor));
			if(cgClassLimitEnabled)
				epDepHandlers.add(new CGClassEntryPointOutputHandler(exe, outGraphml,config.getFilePath("debug_cg-class-limit-dir"),
						transClass,(Integer)handler.getPhaseOptionUnchecked(ACMinerDebugHandler.cgClassLimit).getValue(), cgClassColorMapIndex, 
						dataAccessor));
			if(throwSEEnabled){
				mainLogger.info("{}: Initlizing the color map for ThrowSE...",cn);
				Map<SootMethod,Long> epToIndex = new HashMap<>();
				for(EntryPoint ep : eps) {
					Map<SootMethod,List<Color>> colorMap = new HashMap<>();
					setupColorMap(colorMap,epsSootMethods,Color.GREEN);
					setupColorMap(colorMap,dataAccessor.getContextQueriesDB().getSources(ep),Color.BLUE);
					setupColorMap(colorMap,dataAccessor.getThrowSecurityExceptionStmtsDB().getSources(ep),Color.BLUE);
					setupColorMap(colorMap,dataAccessor.getControlPredicatesDB().getSources(ep),Color.ORANGE);
					epToIndex.put(ep.getEntryPoint(), transMethod.applyColorsToNodes(colorMap));
				}
				epDepHandlers.add(new CGThrowSEEntryPointOutputHandler(exe, outGraphml, config.getFilePath("debug_cg-throw-security-exception-dir"),
						transMethod, epToIndex, dataAccessor));
			}
		}
		
		SootClass stub = null;
		List<LoggingWorkerGroup> workerGroups = new ArrayList<>();
		LoggingWorkerGroup curWorkerGroup = null;
		while(!eps.isEmpty()) {
			EntryPoint ep = eps.poll();
			if(stub == null || !stub.equals(ep.getStub())) {
				stub = ep.getStub();
				if(curWorkerGroup != null) {
					curWorkerGroup.unlockInitialLock();
					curWorkerGroup = null;
				}
				LoggingWorkerGroup g = new LoggingWorkerGroup(cn,stub.toString(),false);
				if(g.getLogger() == null) {
					mainLogger.fatal("{}: Failed to initilize local logger for '{}'. Skipping debug output of '{}'.",cn,stub,stub);
					successOuter = false;
				} else {
					curWorkerGroup = g;
					workerGroups.add(g);
				}	
			}
			if(curWorkerGroup != null){
				for(AbstractEntryPointOutputHandler h : epDepHandlers) {
					if(!h.executeWorker(ep, curWorkerGroup, curWorkerGroup.getLogger()))
						successOuter = false;
				}
			}
		}
		//Unlock the initial lock for the last group produced by the loop
		if(curWorkerGroup != null) {
			curWorkerGroup.unlockInitialLock();
			curWorkerGroup = null;
		}
		
		LoggingWorkerGroup workerDumps = null;
		if(dataDumpsEnabled){
			DataDumpsOutputHandler dataDumpsOutputHandler = new DataDumpsOutputHandler(cn,exe,
					config.getFilePath("debug_data-dumps-dir"),dataAccessor,mainLogger);
			workerDumps = dataDumpsOutputHandler.run();
		}
		
		LoggingWorkerGroup commonGroup = null;
		boolean nothingToProcess = false;
		if(commonSubGraphsEnabled) {
			try {
				Set<Set<SootMethod>> graphsToCompute = CommonSubgraphOutputHandler.loadEntryPointSets(((Path)handler.getPhaseOptionUnchecked(
						ACMinerDebugHandler.commonSubgraphs).getValue()));
				if(graphsToCompute != null && !graphsToCompute.isEmpty()) {
					CommonSubgraphOutputHandler commonSub = new CommonSubgraphOutputHandler(cn, exe, outGraphml, graphsToCompute, 
							config.getFilePath("debug_common-subgraphs-dir"), dataAccessor, mainLogger);
					commonGroup = commonSub.run();
				} else {
					nothingToProcess = true;
				}
			} catch(Throwable t) {
				mainLogger.fatal("{}: Failed to successfully start all common subgraphs tasks.",t,cn);
				successOuter = false;
			}
		}
		
		List<LoggingWorkerGroup> subgraphCountGroups = null;
		boolean nothingToProcessSubGraphCount = false;
		if(subGraphCountEnabled) {
			try {
				Map<String,Set<SootMethod>> groups = SubgraphCountingOutputHandler.loadGroups(((Path)handler.getPhaseOptionUnchecked(
						ACMinerDebugHandler.optSubGraphCount).getValue()));
				if(groups != null && !groups.isEmpty()) {
					SubgraphCountingOutputHandler subGraphCount = new SubgraphCountingOutputHandler(exe, outGraphml, groups, 
							config.getFilePath("debug_subgraph-count-dir"), dataAccessor, mainLogger);
					subgraphCountGroups = subGraphCount.run();
				} else {
					nothingToProcessSubGraphCount = true;
				}
			} catch(Throwable t) {
				mainLogger.fatal("{}: Failed to successfully start all subgraph count tasks.",t,cn);
				successOuter = false;
			}
		}
		
		List<LoggingWorkerGroup> pathsGroups = null;
		if(pathsEnabled) {
			try {
				PathsOutputHandler handler = new PathsOutputHandler(exe, config.getFilePath("debug_paths-dir"), dataAccessor, mainLogger);
				pathsGroups = handler.run();
			} catch(Throwable t) {
				mainLogger.fatal("{}: Failed to successfully start all paths tasks.",t,cn);
				successOuter = false;
			}
		}
		
		if(pathsToMethodsEnabled) {
			try {
				Map<SootClass, Set<SootMethod>> inputData = PathsToMethodsOutputHandler.loadData(((Path)handler.getPhaseOptionUnchecked(
						ACMinerDebugHandler.pathsToMethods).getValue()), dataAccessor);
				if(inputData != null && !inputData.isEmpty()) {
					PathsToMethodsOutputHandler oh = new PathsToMethodsOutputHandler(outGraphml, 
							config.getFilePath("debug_paths-to-methods-dir"), dataAccessor, inputData, mainLogger);
					if(!oh.run()) {
						mainLogger.fatal("{}: Failed to successfully output all paths to methods data.",cn);
						successOuter = false;
					}
				} else {
					mainLogger.info("{}: Skipping the generation methods to paths as not input data was provided.",cn);
				}
			} catch(Throwable t) {
				mainLogger.fatal("{}: Failed to successfully output all paths to methods data.",t,cn);
				successOuter = false;
			}
		}
		
		if(cqSubGraphEnabled) {
			CQSubGraphOutputHandler handler = new CQSubGraphOutputHandler(outGraphml, config.getFilePath("debug_cq-subgraph-dir"), 
					dataAccessor, mainLogger);
			if(!handler.run()) {
				mainLogger.fatal("{}: Failed to successfully output all context query subgraphs.",cn);
				successOuter = false;
			}
		}
		
		//Shutdown the executors
		if(exe != null && !exe.shutdownWhenFinished()){
			mainLogger.fatal(CountingThreadExecutor.computeJointErrorMsg(exe.getAndClearExceptions(), 
					"Failed to wait for the executor to terminate.", cn));
			successOuter = false;
		}
		if(outGraphml != null && !outGraphml.close()) {
			mainLogger.fatal(CountingThreadExecutor.computeJointErrorMsg(outGraphml.getExceptions(), 
					"Failed to wait for the graphml executor to terminate.", cn));
			successOuter = false;
		}
		
		for(LoggingWorkerGroup g : workerGroups) {
			if(g.shutdownNormally() && !g.hasExceptions()) {
				mainLogger.info("{}: Successfully completed all debugging tasks for Stub '{}'.",cn,g.getName());
			} else {
				mainLogger.fatal("{}: Failed to complete all debugging tasks for Stub '{}'.",cn,g.getName());
				successOuter = false;
			}
		}
		
		if(dataDumpsEnabled) {
			if(workerDumps != null && workerDumps.shutdownNormally() && !workerDumps.hasExceptions()) {
				mainLogger.info("{}: Successfully generated all debug data dumps.",cn);
			} else {
				mainLogger.fatal("{}: Failed to generate all debug data dumps.",cn);
				successOuter = false;
			}
		}
		
		if(commonSubGraphsEnabled) {
			if(nothingToProcess) {
				mainLogger.info("{}: Skipping the generation of the common subgraphs as no groups were provided.",cn);
			}else if(commonGroup != null && commonGroup.shutdownNormally() && !commonGroup.hasExceptions()) {
				mainLogger.info("{}: Successfully generated all common subgraphs.",cn);
			} else {
				mainLogger.fatal("{}: Failed to generate all common subgraphs.",cn);
				successOuter = false;
			}
		}
		
		if(subGraphCountEnabled) {
			if(nothingToProcessSubGraphCount) {
				mainLogger.info("{}: Skipping the generation of the subgraph counts as no groups were provided.",cn);
			}else {
				boolean failed = false;
				if(subgraphCountGroups != null) {
					for(LoggingWorkerGroup g : subgraphCountGroups) {
						if(!g.shutdownNormally() || g.hasExceptions()) {
							failed = true;
							break;
						}
					}
				} else {
					failed = true;
				}
				if(failed) {
					mainLogger.fatal("{}: Failed to generate all subgraph counts.",cn);
					successOuter = false;
				} else {
					mainLogger.info("{}: Successfully generated all subgraph counts.",cn);
				}
					
			}
		}
		
		if(pathsEnabled) {
			boolean failed = false;
			if(pathsGroups != null) {
				for(LoggingWorkerGroup g : pathsGroups) {
					if(!g.shutdownNormally() || g.hasExceptions()) {
						failed = true;
						break;
					}
				}
			} else {
				failed = true;
			}
			if(failed) {
				mainLogger.fatal("{}: Failed to generate all paths.",cn);
				successOuter = false;
			} else {
				mainLogger.info("{}: Successfully generated all paths.",cn);
			}
		}
		
		if(!successOuter){
			mainLogger.fatal("{}: Failed to output some debug information. Stopping Analysis!",cn);
			return false;
		}else{
			mainLogger.info("{}: Generating debug output succeeded!",cn);
			return true;
		}
	}
	
}
