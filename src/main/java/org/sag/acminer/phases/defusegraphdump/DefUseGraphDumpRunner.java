package org.sag.acminer.phases.defusegraphdump;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.database.defusegraph.DefUseGraph;
import org.sag.acminer.database.defusegraph.StartNode;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.concurrent.CountingThreadExecutor;
import org.sag.common.concurrent.IgnorableRuntimeException;
import org.sag.common.concurrent.LoggingWorkerGroup;
import org.sag.common.concurrent.Worker;
import org.sag.common.concurrent.WorkerCountingThreadExecutor;
import org.sag.common.io.FileHelpers;
import org.sag.common.io.PrintStreamUnixEOL;
import org.sag.common.logging.ILogger;
import org.sag.common.tools.SortingMethods;
import org.sag.common.tuple.Triple;
import org.sag.soot.SootSort;
import org.sag.soot.callgraph.ExcludingJimpleICFG.ExcludingEdgePredicate;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.callgraph.CallGraph;

public class DefUseGraphDumpRunner {
	
	private final ILogger mainLogger;
	private final IACMinerDataAccessor dataAccessor;
	private final String name;
	private final Path rootDir;
	private final Map<SootClass,Path> stubToFieldOutput;
	private final Map<SootClass,Path> stubToMethodOutput;
	private final Map<SootClass,Path> stubToStringConstOutput;
	private final Map<SootClass,Path> stubToStringConstNoUsesOutput;
	private final Map<SootClass,Path> stubToValueOutput;
	private final Map<SootClass,Path> stubToStatsOutput;//single stub per entry point
	private final Map<SootClass,Path> stubToNodesOnPathsOutput;
	
	private final Map<SootClass,Path> stubToFieldOutputUq;
	private final Map<SootClass,Path> stubToMethodOutputUq;
	private final Map<SootClass,Path> stubToStringConstOutputUq;
	private final Map<SootClass,Path> stubToStringConstNoUsesOutputUq;
	private final Map<SootClass,Path> stubToValueOutputUq;
	private final Map<SootClass,Path> stubToNodesOnPathsOutputUq;
	
	private final Path gFieldOutput;
	private final Path gMethodOutput;
	private final Path gStringConstOutput;
	private final Path gStringConstNoUsesOutput;
	private final Path gValueOutput;
	private final Path gStats;//total unique counts for all stubs summed, and total unique counts per stubs
	private final Path gNodesOnPathsOutput;
	
	private final Set<String> allFields;
	private final Set<String> allMethods;
	private final Map<String,Set<String>> allStringConst;
	private final Map<String,Set<String>> allValues;
	private final Set<String> allNodesOnPaths;
	
	public DefUseGraphDumpRunner(IACMinerDataAccessor dataAccessor, Path rootDir, ILogger mainLogger) {
		this.rootDir = rootDir;
		this.dataAccessor = dataAccessor;
		this.mainLogger = mainLogger;
		this.name = getClass().getSimpleName();
		this.gFieldOutput = FileHelpers.getPath(rootDir,"_dug_fields.txt");
		this.gMethodOutput = FileHelpers.getPath(rootDir,"_dug_methods.txt");
		this.gStringConstOutput = FileHelpers.getPath(rootDir,"_dug_string_const_w_uses.txt");
		this.gStringConstNoUsesOutput = FileHelpers.getPath(rootDir,"_dug_string_const.txt");
		this.gValueOutput = FileHelpers.getPath(rootDir,"_dug_uses.txt");
		this.gStats = FileHelpers.getPath(rootDir, "_dug_stats.txt");
		this.gNodesOnPathsOutput = FileHelpers.getPath(rootDir, "_dug_methods_to_logic.txt");
		this.stubToFieldOutput = new HashMap<>();
		this.stubToMethodOutput = new HashMap<>();
		this.stubToStringConstOutput = new HashMap<>();
		this.stubToStringConstNoUsesOutput = new HashMap<>();
		this.stubToValueOutput = new HashMap<>();
		this.stubToNodesOnPathsOutput = new HashMap<>();
		this.stubToStatsOutput = new HashMap<>();
		this.stubToFieldOutputUq = new HashMap<>();
		this.stubToMethodOutputUq = new HashMap<>();
		this.stubToNodesOnPathsOutputUq = new HashMap<>();
		this.stubToStringConstOutputUq = new HashMap<>();
		this.stubToStringConstNoUsesOutputUq = new HashMap<>();
		this.stubToValueOutputUq = new HashMap<>();
		this.allFields = new HashSet<>();
		this.allMethods = new HashSet<>();
		this.allStringConst = new HashMap<>();
		this.allValues = new HashMap<>();
		this.allNodesOnPaths = new HashSet<>();
		for(EntryPoint ep : dataAccessor.getEntryPoints()) {
			SootClass stub = ep.getStub();
			if(!stubToFieldOutput.containsKey(stub)) {
				String stubString = stub.getName().replace(".", "-").replace("$", "~");
				stubToFieldOutput.put(stub, FileHelpers.getPath(rootDir,stubString+"_dug_fields.txt"));
				stubToMethodOutput.put(stub, FileHelpers.getPath(rootDir,stubString+"_dug_methods.txt"));
				stubToNodesOnPathsOutput.put(stub, FileHelpers.getPath(rootDir,stubString+"_dug_methods_to_logic.txt"));
				stubToStringConstOutput.put(stub, FileHelpers.getPath(rootDir,stubString+"_dug_string_const_w_uses.txt"));
				stubToStringConstNoUsesOutput.put(stub, FileHelpers.getPath(rootDir,stubString+"_dug_string_const.txt"));
				stubToValueOutput.put(stub, FileHelpers.getPath(rootDir,stubString+"_dug_uses.txt"));
				stubToStatsOutput.put(stub, FileHelpers.getPath(rootDir,stubString+"_dug_stats.txt"));
				stubToFieldOutputUq.put(stub, FileHelpers.getPath(rootDir,stubString+"_dug_fields_uq.txt"));
				stubToMethodOutputUq.put(stub, FileHelpers.getPath(rootDir,stubString+"_dug_methods_uq.txt"));
				stubToStringConstOutputUq.put(stub, FileHelpers.getPath(rootDir,stubString+"_dug_string_const_w_uses_uq.txt"));
				stubToStringConstNoUsesOutputUq.put(stub, FileHelpers.getPath(rootDir,stubString+"_dug_string_const_uq.txt"));
				stubToValueOutputUq.put(stub, FileHelpers.getPath(rootDir,stubString+"_dug_uses_uq.txt"));
				stubToNodesOnPathsOutputUq.put(stub, FileHelpers.getPath(rootDir, stubString+"_dug_methods_to_logic_uq.txt"));
			}
		}
	}
	
	public boolean run() {
		boolean successOuter = true;
		
		mainLogger.info("{}: Begin the def use graph dumper.",name);
		
		try {
			FileHelpers.processDirectory(rootDir, true, true);
		} catch(Throwable t) {
			mainLogger.fatal("{}: Failed to create output directory '{}'. Stoping analysis!",name,rootDir);
			successOuter = false;
		}
		
		if(successOuter){
			WorkerCountingThreadExecutor exe = null;
			DefUseGraphDataGatherer gatherer = null;
			List<LoggingWorkerGroup> workerGroups = new ArrayList<>();
			try{
				SootClass stub = null;
				LoggingWorkerGroup curWorkerGroup = null;
				Deque<EntryPoint> eps = new ArrayDeque<>(dataAccessor.getEntryPoints());
				exe = new WorkerCountingThreadExecutor();
				gatherer = new DefUseGraphDataGatherer();
				while(!eps.isEmpty()) {
					EntryPoint ep = eps.poll();
					if(stub == null || !stub.equals(ep.getStub())) {
						stub = ep.getStub();
						if(curWorkerGroup != null) {
							curWorkerGroup.unlockInitialLock();
							curWorkerGroup = null;
						}
						RunnerGroup g = new RunnerGroup(name,stub.toString(),false,stubToFieldOutputUq.get(stub),
								stubToMethodOutputUq.get(stub),stubToStringConstOutputUq.get(stub),stubToStringConstNoUsesOutputUq.get(stub),
								stubToValueOutputUq.get(stub),stubToNodesOnPathsOutputUq.get(stub));
						if(g.getLogger() == null) {
							mainLogger.fatal("{}: Failed to initilize local logger for '{}'. Skipping analysis of '{}'.",name,stub,stub);
							successOuter = false;
						} else {
							curWorkerGroup = g;
							workerGroups.add(g);
						}	
					}
					if(curWorkerGroup != null){
						Runnable runner = new Runner(ep,gatherer,curWorkerGroup.getLogger());
						try {
							exe.execute(runner, curWorkerGroup);
						} catch(Throwable t) {
							mainLogger.fatal("{}: Failed to execute '{}' for group '{}'.",name,runner.toString(),curWorkerGroup.getName());
							successOuter = false;
						}
					}
				}
				//Unlock the initial lock for the last group produced by the loop
				if(curWorkerGroup != null) {
					curWorkerGroup.unlockInitialLock();
					curWorkerGroup = null;
				}
			} catch(Throwable t) {
				mainLogger.fatal("{}: An unexpected exception occured in the def use graph dumper.",t,name);
				successOuter = false;
			} finally {
				//Shutdown the executors
				if(exe != null && !exe.shutdownWhenFinished()){
					mainLogger.fatal(CountingThreadExecutor.computeJointErrorMsg(exe.getAndClearExceptions(), 
							"Failed to wait for the executor to terminate.", name));
					successOuter = false;
				}
				if(gatherer != null && !gatherer.shutdownWhenFinished()) {
					mainLogger.fatal(CountingThreadExecutor.computeJointErrorMsg(gatherer.getAndClearExceptions(), 
							"Failed to wait for the def use graph modifier to terminate.",name));
					successOuter = false;
				}
				
				for(LoggingWorkerGroup g : workerGroups) {
					if(g.shutdownNormally() && !g.hasExceptions()) {
						mainLogger.info("{}: Successfully completed the def use graph dumper for Stub '{}'.",name,g.getName());
					} else {
						mainLogger.fatal("{}: Failed to complete the def use graph dumper for Stub '{}'.",name,g.getName());
						successOuter = false;
					}
				}
			}
		}
		
		if(successOuter) {
			try {
				writeFields(allFields,gFieldOutput,name,mainLogger);
				writeMethods(allMethods,gMethodOutput,name,mainLogger);
				writeStringConst(allStringConst,gStringConstOutput,name,mainLogger);
				writeStringConst(allStringConst.keySet(),gStringConstNoUsesOutput,name,mainLogger);
				writeValues(allValues,gValueOutput,name,mainLogger);
				writeStats("Total", allFields.size(), allMethods.size(), allStringConst.keySet().size(), 
						allValues.keySet().size(), allNodesOnPaths.size(), gStats, name, mainLogger);
				writeMethods(allNodesOnPaths,gNodesOnPathsOutput,name,mainLogger);
			} catch(IgnorableRuntimeException e) {
				successOuter = false;
			}
		}
		
		if(!successOuter){
			mainLogger.fatal("{}: The def use graph dumper failed for one or more entry points. Stopping Analysis!",name);
			return false;
		}else{
			mainLogger.info("{}: The def use graph dumper succeeded!",name);
			return true;
		}
	}
	
	private class RunnerGroup extends LoggingWorkerGroup {
		
		private final Path fieldsOut;
		private final Path methodsOut;
		private final Path stringConstOut;
		private final Path stringConstNoUsesOut;
		private final Path usesOut;
		private final Path nodesOnPathsOut;
		private volatile Set<String> fields;
		private volatile Set<String> methods;
		private volatile Map<String,Set<String>> stringConstToUses;
		private volatile Map<String,Set<String>> startToUses;
		private volatile Set<String> nodesOnPaths;

		public RunnerGroup(String phaseName, String name, boolean shutdownOnError, 
				Path fieldsOut, Path methodsOut, Path stringConstOut, Path stringConstNoUsesOut, Path usesOut, 
				Path nodesOnPathsOut) {
			super(phaseName, name, shutdownOnError);
			this.fieldsOut = fieldsOut;
			this.methodsOut = methodsOut;
			this.stringConstOut = stringConstOut;
			this.stringConstNoUsesOut = stringConstNoUsesOut;
			this.usesOut = usesOut;
			this.nodesOnPathsOut = nodesOnPathsOut;
			this.fields = new HashSet<>();
			this.methods = new HashSet<>();
			this.stringConstToUses = new HashMap<>();
			this.startToUses = new HashMap<>();
			this.nodesOnPaths = new HashSet<>();
		}
		
		@Override
		protected void endWorker(Worker w) {
			Runner runner = (Runner)w.getRunner();
			if(!isShutdown) {
				EntryPoint ep = runner.getEntryPoint();
				
				//write to the fields data
				Set<String> f = runner.getFields();
				synchronized (fields) {
					if(f != null)
						fields.addAll(f);
				}
				synchronized (allFields) {
					if(f != null)
						allFields.addAll(f);
				}
				writeFields(f,stubToFieldOutput.get(ep.getStub()),ep,name,logger);
				
				//write to the methods data
				Set<String> m = runner.getMethods();
				synchronized (methods) {
					if(m != null)
						methods.addAll(m);
				}
				synchronized (allMethods) {
					if(m != null)
						allMethods.addAll(m);
				}
				writeMethods(m,stubToMethodOutput.get(ep.getStub()),ep,name,logger);
				
				//write the methods that occur on all paths from the ep to all detected authorization logic
				Set<String> nodes = runner.getNodesOnPaths();
				synchronized (nodesOnPaths) {
					if(nodes != null)
						nodesOnPaths.addAll(nodes);
				}
				synchronized (allNodesOnPaths) {
					if(nodes != null)
						allNodesOnPaths.addAll(nodes);
				}
				writeMethods(nodes,stubToNodesOnPathsOutput.get(ep.getStub()),ep,name,logger);
				
				//write to the string constants data
				Map<String,Set<String>> stringConsts = runner.getStringConsts();
				synchronized (stringConstToUses) {
					if(stringConsts != null) {
						for(String s : stringConsts.keySet()) {
							Set<String> temp = stringConstToUses.get(s);
							if(temp == null) {
								temp = new HashSet<>();
								stringConstToUses.put(s,temp);
							}
							temp.addAll(stringConsts.get(s));
						}
					}
				}
				synchronized (allStringConst) {
					if(stringConsts != null) {
						for(String stringConst : stringConsts.keySet()) {
							Set<String> temp = allStringConst.get(stringConst);
							if(temp == null) {
								temp = new HashSet<>();
								allStringConst.put(stringConst, temp);
							}
							temp.addAll(stringConsts.get(stringConst));
						}
					}
				}
				writeStringConst(stringConsts,stubToStringConstOutput.get(ep.getStub()),ep,name,logger);
				writeStringConst(stringConsts.keySet(),stubToStringConstNoUsesOutput.get(ep.getStub()),ep,name,logger);
				
				//write the uses data
				Map<String,Set<String>> values = runner.getValues();
				synchronized (startToUses) {
					if(values != null) {
						for(String s : values.keySet()) {
							Set<String> temp = startToUses.get(s);
							if(temp == null) {
								temp = new HashSet<>();
								startToUses.put(s,temp);
							}
							temp.addAll(values.get(s));
						}
					}
				}
				synchronized (allValues) {
					if(values != null) {
						for(String s : values.keySet()) {
							Set<String> temp = allValues.get(s);
							if(temp == null) {
								temp = new HashSet<>();
								allValues.put(s, temp);
							}
							temp.addAll(values.get(s));
						}
					}
				}
				writeValues(values,stubToValueOutput.get(ep.getStub()),ep,name,logger);
				
				writeStats(ep.toString(),f.size(),m.size(),stringConsts.keySet().size(),values.keySet().size(),
						nodesOnPaths.size(),stubToStatsOutput.get(ep.getStub()),name,logger);
			}
			runner.clearData();
			super.endWorker(w);
		}
		
		@Override
		protected void endGroup() {
			long fCount = 0;
			long mCount = 0;
			long scCount = 0;
			long cpCount = 0;
			long nodesOnPathsCount = 0;
			synchronized (fields) {
				fCount = fields.size();
				writeFields(fields,fieldsOut,name,logger);
				fields = null;
			}
			synchronized (methods) {
				mCount = methods.size();
				writeMethods(methods,methodsOut,name,logger);
				methods = null;
			}
			synchronized (stringConstToUses) {
				scCount = stringConstToUses.keySet().size();
				writeStringConst(stringConstToUses, stringConstOut, name, logger);
				writeStringConst(stringConstToUses.keySet(), stringConstNoUsesOut, name, logger);
				stringConstToUses = null;
			}
			synchronized (startToUses) {
				cpCount = startToUses.keySet().size();
				writeValues(startToUses, usesOut, name, logger);
				startToUses = null;
			}
			synchronized (nodesOnPaths) {
				nodesOnPathsCount = nodesOnPaths.size();
				writeMethods(nodesOnPaths,nodesOnPathsOut,name,logger);
				nodesOnPaths = null;
			}
			writeStats(name, fCount, mCount, scCount, cpCount, nodesOnPathsCount, gStats, name, logger);
			super.endGroup();
		}
		
	}
	
	private class Runner implements Runnable {
		
		private final EntryPoint ep;
		private final ILogger logger;
		private final DefUseGraphDataGatherer gather;
		private volatile Set<String> fields;
		private volatile Set<String> methods;
		private volatile Map<String,Set<String>> stringConsts;
		private volatile Map<String,Set<String>> startToUses;
		private volatile Set<String> nodesOnPaths;
		
		public Runner(EntryPoint ep, DefUseGraphDataGatherer gather, ILogger logger) {
			this.ep = ep;
			this.logger = logger;
			this.gather = gather;
			this.fields = null;
			this.methods = null;
			this.stringConsts = null;
			this.startToUses = null;
			this.nodesOnPaths = null;
		}

		@Override
		public void run() {
			logger.fineInfo("{}: Begin dumping def use graph for ep '{}'.",name,ep);
			
			try{
				DefUseGraph graph = dataAccessor.getDefUseGraphDB().getDefUseGraph(ep, dataAccessor.getConfig().getFilePath("acminer_defusegraph-dir"));
				Set<Unit> controlPreds = dataAccessor.getControlPredicatesDB().getUnits(ep);
				Set<StartNode> startNodes = new HashSet<>();
				for(StartNode sn : graph.getStartNodes()) {
					if(controlPreds.contains(sn.getUnit()))
						startNodes.add(sn);
				}
				startNodes = SortingMethods.sortSet(startNodes);
				
				logger.fineInfo("{}: Initilizing the def use graph for '{}'.",name,ep.toString());
				graph.resolveStartNodeToDefStrings();
				logger.fineInfo("{}: Successfully initilized the def use graph for '{}'.",name,ep.toString());
				
				Triple<Set<String>, Set<String>, Map<String, Set<String>>> data = gather.gatherData(graph, startNodes, ep, logger);
				
				this.fields = data.getFirst();
				this.methods = data.getSecond();
				this.stringConsts = data.getThird();
				this.startToUses = new HashMap<>();
				Map<StartNode, Set<String>> startNodesToDefStrings = graph.getStartNodesToDefStrings();
				for(StartNode sn : startNodes) {
					String key = "Stmt: " + sn.toString() + " Source: " + sn.getSource();
					Set<String> temp = startToUses.get(key);
					if(temp == null) {
						temp = new HashSet<>();
						startToUses.put(key, temp);
					}
					if(startNodesToDefStrings.containsKey(sn))
						temp.addAll(startNodesToDefStrings.get(sn));
				}
				
				graph.clearStartNodesToDefStrings();//Free up memory now that we don't need to store these anymore
				
				Set<SootMethod> seeds = new HashSet<>(dataAccessor.getControlPredicatesDB().getSources(ep));
				seeds.addAll(dataAccessor.getContextQueriesDB().getSources(ep));
				CallGraph cg = Scene.v().getCallGraph();
				ExcludingEdgePredicate pred = new ExcludingEdgePredicate(cg, 
						dataAccessor.getExcludedElementsDB().createNewExcludeHandler(ep));
				
				this.nodesOnPaths = gather.gatherNodesOnPathsToAuthorizationLogic(ep, pred, cg, seeds, logger);
				
				logger.fineInfo("{}: Dumping the def use graph succeeded for ep '{}'.",name,ep);
			} catch(IgnorableRuntimeException t) {
				throw t;
			} catch(Throwable t) {
				logger.fatal("{}: An unexpected error occured while dumping the def use graph for ep '{}'.",t,name,ep);
				throw new IgnorableRuntimeException();
			}
		}
		
		public void clearData() {
			this.fields = null;
			this.methods = null;
			this.stringConsts = null;
			this.startToUses = null;
			this.nodesOnPaths = null;
		}
		
		public EntryPoint getEntryPoint() { return ep; }
		public Set<String> getFields() {return fields;}
		public Set<String> getMethods() {return methods;}
		public Map<String,Set<String>> getStringConsts() {return stringConsts;}
		public Map<String,Set<String>> getValues() {return startToUses;}
		public Set<String> getNodesOnPaths() {return nodesOnPaths;}
		
	}
	
	private static void writeFields(Set<String> fields, Path p, EntryPoint ep, String name, ILogger logger) {
		synchronized (p) {
			try (PrintStreamUnixEOL ps = new PrintStreamUnixEOL(Files.newOutputStream(p,StandardOpenOption.APPEND,
					StandardOpenOption.CREATE,StandardOpenOption.WRITE))) {
				ps.println("Entry Point: " + ep);
				for(String s : fields) {
					ps.println("  Field: " + s);
				}
			} catch(Throwable t) {
				logger.fatal("{}: An error occured when writing to file '{}' for '{}'.",name,p,ep);
				throw new IgnorableRuntimeException();
			}
		}
	}
	
	private static void writeFields(Set<String> fields, Path p, String name, ILogger logger) {
		try (PrintStreamUnixEOL ps = new PrintStreamUnixEOL(Files.newOutputStream(p,StandardOpenOption.APPEND,
				StandardOpenOption.CREATE,StandardOpenOption.WRITE))) {
			for(String s : SortingMethods.sortSet(fields,SootSort.sfStringComp)) {
				ps.println(s);
			}
		} catch(Throwable t) {
			logger.fatal("{}: An error occured when writing to file '{}'.",name,p);
			throw new IgnorableRuntimeException();
		}
	}
	
	private static void writeMethods(Set<String> methods, Path p, EntryPoint ep, String name, ILogger logger) {
		synchronized (p) {
			try (PrintStreamUnixEOL ps = new PrintStreamUnixEOL(Files.newOutputStream(p,StandardOpenOption.APPEND,
					StandardOpenOption.CREATE,StandardOpenOption.WRITE))) {
				ps.println("Entry Point: " + ep);
				for(String s : methods) {
					ps.println("  Method: " + s);
				}
			} catch(Throwable t) {
				logger.fatal("{}: An error occured when writing to file '{}' for '{}'.",name,p,ep);
				throw new IgnorableRuntimeException();
			}
		}
	}
	
	private static void writeMethods(Set<String> methods, Path p, String name, ILogger logger) {
		try (PrintStreamUnixEOL ps = new PrintStreamUnixEOL(Files.newOutputStream(p,StandardOpenOption.APPEND,
				StandardOpenOption.CREATE,StandardOpenOption.WRITE))) {
			for(String s : SortingMethods.sortSet(methods,SootSort.smStringComp)) {
				ps.println(s);
			}
		} catch(Throwable t) {
			logger.fatal("{}: An error occured when writing to file '{}'.",name,p);
			throw new IgnorableRuntimeException();
		}
	}
	
	private static void writeStringConst(Map<String,Set<String>> stringConsts, Path p, EntryPoint ep, String name, ILogger logger) {
		synchronized (p) {
			try (PrintStreamUnixEOL ps = new PrintStreamUnixEOL(Files.newOutputStream(p,StandardOpenOption.APPEND,
					StandardOpenOption.CREATE,StandardOpenOption.WRITE))) {
				ps.println("Entry Point: " + ep);
				for(String s : stringConsts.keySet()) {
					ps.println("  StringConst: " + s);
					for(String k : stringConsts.get(s)) {
						ps.println("    Use: " + k);
					}
				}
			} catch(Throwable t) {
				logger.fatal("{}: An error occured when writing to file '{}' for '{}'.",name,p,ep);
				throw new IgnorableRuntimeException();
			}
		}
	}
	
	private static void writeStringConst(Set<String> stringConsts, Path p, EntryPoint ep, String name, ILogger logger) {
		Set<String> temp = SortingMethods.sortSet(stringConsts,SortingMethods.sComp);
		synchronized (p) {
			try (PrintStreamUnixEOL ps = new PrintStreamUnixEOL(Files.newOutputStream(p,StandardOpenOption.APPEND,
					StandardOpenOption.CREATE,StandardOpenOption.WRITE))) {
				ps.println("Entry Point: " + ep);
				for(String s : temp) {
					ps.println("  " + s);
				}
			} catch(Throwable t) {
				logger.fatal("{}: An error occured when writing to file '{}' for '{}'.",name,p,ep);
				throw new IgnorableRuntimeException();
			}
		}
	}
	
	private static void writeStringConst(Map<String,Set<String>> stringConsts, Path p, String name, ILogger logger) {
		for(String s : stringConsts.keySet())
			stringConsts.put(s, SortingMethods.sortSet(stringConsts.get(s),SortingMethods.sComp));
		Map<String,Set<String>> temp = SortingMethods.sortMapKey(stringConsts,SortingMethods.sComp);
		try (PrintStreamUnixEOL ps = new PrintStreamUnixEOL(Files.newOutputStream(p,StandardOpenOption.APPEND,
				StandardOpenOption.CREATE,StandardOpenOption.WRITE))) {
			for(String s : temp.keySet()) {
				ps.println("StringConst: " + s);
				for(String k : temp.get(s)) {
					ps.println("  Use: " + k);
				}
			}
		} catch(Throwable t) {
			logger.fatal("{}: An error occured when writing to file '{}'.",name,p);
			throw new IgnorableRuntimeException();
		}
	}
	
	private static void writeStringConst(Set<String> stringConsts, Path p, String name, ILogger logger) {
		Set<String> temp = SortingMethods.sortSet(stringConsts,SortingMethods.sComp);
		try (PrintStreamUnixEOL ps = new PrintStreamUnixEOL(Files.newOutputStream(p,StandardOpenOption.APPEND,
				StandardOpenOption.CREATE,StandardOpenOption.WRITE))) {
			for(String s : temp) {
				ps.println(s);
			}
		} catch(Throwable t) {
			logger.fatal("{}: An error occured when writing to file '{}'.",name,p);
			throw new IgnorableRuntimeException();
		}
	}
	
	private static void writeValues(Map<String,Set<String>> startToUses, Path p, EntryPoint ep, String name, ILogger logger) {
		Map<String,BigInteger> countMap = DefUseGraph.computeAllStatementsCount(startToUses);
		BigInteger totalUses = BigInteger.ZERO;
		for(BigInteger i : countMap.values()) {
			totalUses = totalUses.add(i);
		}
		synchronized (p) {
			try (PrintStreamUnixEOL ps = new PrintStreamUnixEOL(Files.newOutputStream(p,StandardOpenOption.APPEND,
					StandardOpenOption.CREATE,StandardOpenOption.WRITE))) {
				ps.println("Entry Point: " + ep + 
						" Totals: Unique Control Predicates = " + countMap.keySet().size() + ", Unique Uses = " + totalUses.toString());
				for(String s : startToUses.keySet()) {
					ps.println("  " + s + " Count: " + countMap.get(s).toString());
					for(String l : startToUses.get(s)) {
						ps.println("    Def: " + l);
					}
				}
			} catch(Throwable t) {
				logger.fatal("{}: An error occured when writing to file '{}' for '{}'.",name,p,ep);
				throw new IgnorableRuntimeException();
			}
		}
	}
	
	private static void writeValues(Map<String,Set<String>> values, Path p, String name, ILogger logger) {
		for(String s : values.keySet())
			values.put(s, SortingMethods.sortSet(values.get(s),SortingMethods.sComp));
		Map<String,Set<String>> temp = SortingMethods.sortMapKey(values,SortingMethods.sComp);
		Map<String,BigInteger> countMap = DefUseGraph.computeAllStatementsCount(temp);
		BigInteger totalUses = BigInteger.ZERO;
		for(BigInteger i : countMap.values()) {
			totalUses = totalUses.add(i);
		}
		try (PrintStreamUnixEOL ps = new PrintStreamUnixEOL(Files.newOutputStream(p,StandardOpenOption.APPEND,
				StandardOpenOption.CREATE,StandardOpenOption.WRITE))) {
			ps.println("Totals: Unique Control Predicates = " + countMap.keySet().size() + ", Unique Uses = " + totalUses.toString());
			for(String s : temp.keySet()) {
				ps.println(s + " Count: " + countMap.get(s));
				for(String k : temp.get(s)) {
					ps.println("  Def: " + k);
				}
			}
		} catch(Throwable t) {
			logger.fatal("{}: An error occured when writing to file '{}'.",name,p);
			throw new IgnorableRuntimeException();
		}
	}
	
	private static void writeStats(String stub, long fCount, long mCount, long scCount, long cpCount, 
			long nodesOnPathsCount, Path p, String name, ILogger logger) {
		synchronized(p) {
			boolean writeHeader = FileHelpers.checkRWFileExists(p) ? false : true;
			try (PrintStreamUnixEOL ps = new PrintStreamUnixEOL(Files.newOutputStream(p,StandardOpenOption.APPEND,
					StandardOpenOption.CREATE,StandardOpenOption.WRITE))) {
				if(writeHeader) {
					ps.println("Name, Fields, Methods, Strings, Control Predicates, "
							+ "Methods On Paths To Authorization Logic");
				}
				ps.println(stub + ", " + fCount + ", " + mCount + ", " + scCount + ", " + cpCount + ", " 
						+ nodesOnPathsCount);
			} catch(Throwable t) {
				logger.fatal("{}: An error occured when writing to file '{}'.",name,p);
				throw new IgnorableRuntimeException();
			}
		}
	}

}
