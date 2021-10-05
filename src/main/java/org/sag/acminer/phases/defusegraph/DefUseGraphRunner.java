package org.sag.acminer.phases.defusegraph;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.database.defusegraph.DefUseGraph;
import org.sag.acminer.database.defusegraph.IDefUseGraphDatabase;
import org.sag.acminer.database.excludedelements.IExcludeHandler;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.concurrent.CountingThreadExecutor;
import org.sag.common.concurrent.IgnorableRuntimeException;
import org.sag.common.concurrent.LoggingWorkerGroup;
import org.sag.common.concurrent.Worker;
import org.sag.common.concurrent.WorkerCountingThreadExecutor;
import org.sag.common.logging.ILogger;
import org.sag.soot.callgraph.ExcludingJimpleICFG;
import org.sag.soot.callgraph.IJimpleICFG;
import org.sag.soot.callgraph.JimpleICFG;
import org.sag.soot.callgraph.ExcludingJimpleICFG.ExcludingEdgePredicate;

import soot.SootClass;


public class DefUseGraphRunner {

	private final ILogger mainLogger;
	private final IACMinerDataAccessor dataAccessor;
	private final String name;
	//TODO Remove commented out sections
	/*private final Map<SootClass,Path> stubToFieldOutput;
	private final Map<SootClass,Path> stubToMethodOutput;
	private final Map<SootClass,Path> stubToStringConstOutput;
	private final Map<SootClass,Path> stubToValueOutput;
	
	private final Map<SootClass,Path> stubToFieldOutputUq;
	private final Map<SootClass,Path> stubToMethodOutputUq;
	private final Map<SootClass,Path> stubToStringConstOutputUq;
	private final Map<SootClass,Path> stubToValueOutputUq;
	
	private final Path gFieldOutput;
	private final Path gMethodOutput;
	private final Path gStringConstOutput;
	private final Path gValueOutput;
	
	private final Set<String> allFields;
	private final Set<String> allMethods;
	private final Map<String,Set<String>> allStringConst;
	private final Map<String,Set<String>> allValues;*/

	public DefUseGraphRunner(IACMinerDataAccessor dataAccessor, ILogger mainLogger){
		this.dataAccessor = dataAccessor;
		this.mainLogger = mainLogger;
		this.name = getClass().getSimpleName();
		/*this.gFieldOutput = FileHelpers.getPath(PMinerFilePaths.v().getOutput_Miner_DefUseGraphBeforeFilterDir(),"vt_fields.txt");
		this.gMethodOutput = FileHelpers.getPath(PMinerFilePaths.v().getOutput_Miner_DefUseGraphBeforeFilterDir(),"vt_methods.txt");
		this.gStringConstOutput = FileHelpers.getPath(PMinerFilePaths.v().getOutput_Miner_DefUseGraphBeforeFilterDir(),"vt_string_const.txt");
		this.gValueOutput = FileHelpers.getPath(PMinerFilePaths.v().getOutput_Miner_DefUseGraphBeforeFilterDir(),"vt_uses.txt");
		this.stubToFieldOutput = new HashMap<>();
		this.stubToMethodOutput = new HashMap<>();
		this.stubToStringConstOutput = new HashMap<>();
		this.stubToValueOutput = new HashMap<>();
		this.stubToFieldOutputUq = new HashMap<>();
		this.stubToMethodOutputUq = new HashMap<>();
		this.stubToStringConstOutputUq = new HashMap<>();
		this.stubToValueOutputUq = new HashMap<>();
		this.allFields = new HashSet<>();
		this.allMethods = new HashSet<>();
		this.allStringConst = new HashMap<>();
		this.allValues = new HashMap<>();*/
		/*for(EntryPoint ep : dataAccessor.getEntryPoints()) {
			SootClass stub = ep.getStub();
			if(!stubToFieldOutput.containsKey(stub)) {
				String stubString = stub.getName().replace(".", "-").replace("$", "~");
				stubToFieldOutput.put(stub, FileHelpers.getPath(PMinerFilePaths.v().getOutput_Miner_DefUseGraphBeforeFilterDir(),
						stubString+"_vt_fields.txt"));
				stubToMethodOutput.put(stub, FileHelpers.getPath(PMinerFilePaths.v().getOutput_Miner_DefUseGraphBeforeFilterDir(),
						stubString+"_vt_methods.txt"));
				stubToStringConstOutput.put(stub, FileHelpers.getPath(PMinerFilePaths.v().getOutput_Miner_DefUseGraphBeforeFilterDir(),
						stubString+"_vt_string_const.txt"));
				stubToValueOutput.put(stub, FileHelpers.getPath(PMinerFilePaths.v().getOutput_Miner_DefUseGraphBeforeFilterDir(),
						stubString+"_vt_uses.txt"));
				stubToFieldOutputUq.put(stub, FileHelpers.getPath(PMinerFilePaths.v().getOutput_Miner_DefUseGraphBeforeFilterDir(),
						stubString+"_vt_fields_uq.txt"));
				stubToMethodOutputUq.put(stub, FileHelpers.getPath(PMinerFilePaths.v().getOutput_Miner_DefUseGraphBeforeFilterDir(),
						stubString+"_vt_methods_uq.txt"));
				stubToStringConstOutputUq.put(stub, FileHelpers.getPath(PMinerFilePaths.v().getOutput_Miner_DefUseGraphBeforeFilterDir(),
						stubString+"_vt_string_const_uq.txt"));
				stubToValueOutputUq.put(stub, FileHelpers.getPath(PMinerFilePaths.v().getOutput_Miner_DefUseGraphBeforeFilterDir(),
						stubString+"_vt_uses_uq.txt"));
			}
		}*/
	}
	
	public boolean run() {
		boolean successOuter = true;
		
		mainLogger.info("{}: Begin the value tree runner.",name);
		
		/*try {
			FileHelpers.processDirectory(PMinerFilePaths.v().getInput_DefUseGraphBeforeFilterDir(), true, true);
		} catch(Throwable t) {
			mainLogger.fatal("{}: Failed to create output directory '{}'. Stoping analysis!",name,
					PMinerFilePaths.v().getInput_DefUseGraphBeforeFilterDir());
			successOuter = false;
		}*/
		
		if(successOuter){
			WorkerCountingThreadExecutor exe = null;
			DefUseGraphMaker vtm = null;
			List<LoggingWorkerGroup> workerGroups = new ArrayList<>();
			//Set new control predicates database
			dataAccessor.setDefUseGraphDB(IDefUseGraphDatabase.Factory.getNew(false));
			try{
				JimpleICFG baseICFG = new JimpleICFG(dataAccessor.getEntryPoints(),false);
				SootClass stub = null;
				LoggingWorkerGroup curWorkerGroup = null;
				Deque<EntryPoint> eps = new ArrayDeque<>(dataAccessor.getEntryPoints());
				exe = new WorkerCountingThreadExecutor();
				vtm = new DefUseGraphMaker();
				while(!eps.isEmpty()) {
					EntryPoint ep = eps.poll();
					if(stub == null || !stub.equals(ep.getStub())) {
						stub = ep.getStub();
						if(curWorkerGroup != null) {
							curWorkerGroup.unlockInitialLock();
							curWorkerGroup = null;
						}
						DefUseGraphMakerGroup g = new DefUseGraphMakerGroup(name,stub.toString(),false//,stubToFieldOutputUq.get(stub),
								/*stubToMethodOutputUq.get(stub),stubToStringConstOutputUq.get(stub),stubToValueOutputUq.get(stub)*/);
						if(g.getLogger() == null) {
							mainLogger.fatal("{}: Failed to initilize local logger for '{}'. Skipping analysis of '{}'.",name,stub,stub);
							successOuter = false;
						} else {
							curWorkerGroup = g;
							workerGroups.add(g);
						}	
					}
					if(curWorkerGroup != null){
						Runnable runner = new DefUseGraphMakerRunner(ep,baseICFG,vtm,curWorkerGroup.getLogger());
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
				mainLogger.fatal("{}: An unexpected exception occured in the def use graph runner.",t,name);
				successOuter = false;
			} finally {
				//Shutdown the executors
				if(exe != null && !exe.shutdownWhenFinished()){
					mainLogger.fatal(CountingThreadExecutor.computeJointErrorMsg(exe.getAndClearExceptions(), 
							"Failed to wait for the executor to terminate.", name));
					successOuter = false;
				}
				if(vtm != null && !vtm.shutdownWhenFinished()) {
					mainLogger.fatal(CountingThreadExecutor.computeJointErrorMsg(vtm.getAndClearExceptions(), 
							"Failed to wait for the def use graph maker to terminate.",name));
					successOuter = false;
				}
				
				for(LoggingWorkerGroup g : workerGroups) {
					if(g.shutdownNormally() && !g.hasExceptions()) {
						mainLogger.info("{}: Successfully completed the def use graph runner for Stub '{}'.",name,g.getName());
					} else {
						mainLogger.fatal("{}: Failed to complete the def use graph runner for Stub '{}'.",name,g.getName());
						successOuter = false;
					}
				}
			}
		}
		
		/*if(successOuter) {
			try {
				writeFields(allFields,gFieldOutput,name,mainLogger);
				writeMethods(allMethods,gMethodOutput,name,mainLogger);
				writeStringConst(allStringConst,gStringConstOutput,name,mainLogger);
				writeValues(allValues,gValueOutput,name,mainLogger);
			} catch(IgnorableRuntimeException e) {
				successOuter = false;
			}
		}*/
		
		if(!successOuter){
			mainLogger.fatal("{}: The def use graph runner failed for one or more entry points. Stopping Analysis!",name);
			return false;
		}else{
			mainLogger.info("{}: The def use graph runner succeeded!",name);
			return true;
		}
	}
	
	/*private static void writeFields(Set<String> fields, Path p, String name, ILogger logger) {
		try (PrintStreamUnixEOL ps = new PrintStreamUnixEOL(Files.newOutputStream(p,StandardOpenOption.APPEND,
				StandardOpenOption.CREATE,StandardOpenOption.WRITE))) {
			for(String s : SortingMethods.sortSet(fields,SootSort.sfStringComp)) {
				ps.println("Field: " + s);
			}
		} catch(Throwable t) {
			logger.fatal("{}: An error occured when writing to file '{}'.",name,p);
			throw new IgnorableRuntimeException();
		}
	}
	
	private static void writeMethods(Set<String> methods, Path p, String name, ILogger logger) {
		try (PrintStreamUnixEOL ps = new PrintStreamUnixEOL(Files.newOutputStream(p,StandardOpenOption.APPEND,
				StandardOpenOption.CREATE,StandardOpenOption.WRITE))) {
			for(String s : SortingMethods.sortSet(methods,SootSort.smStringComp)) {
				ps.println("Method: " + s);
			}
		} catch(Throwable t) {
			logger.fatal("{}: An error occured when writing to file '{}'.",name,p);
			throw new IgnorableRuntimeException();
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
	}*/
	
	private static class DefUseGraphMakerGroup extends LoggingWorkerGroup {
		
		/*private final Path fieldsOut;
		private final Path methodsOut;
		private final Path stringConstOut;
		private final Path usesOut;
		private volatile Set<String> fields;
		private volatile Set<String> methods;
		private volatile Map<String,Set<String>> stringConstToUses;
		private volatile Map<String,Set<String>> startToUses;*/

		public DefUseGraphMakerGroup(String phaseName, String name, boolean shutdownOnError//, 
				/*Path fieldsOut, Path methodsOut, Path stringConstOut, Path usesOut*/) {
			super(phaseName, name, shutdownOnError);
			/*this.fieldsOut = fieldsOut;
			this.methodsOut = methodsOut;
			this.stringConstOut = stringConstOut;
			this.usesOut = usesOut;*/
			/*this.fields = new HashSet<>();
			this.methods = new HashSet<>();
			this.stringConstToUses = new HashMap<>();
			this.startToUses = new HashMap<>();*/
		}
		
		@Override
		protected void endWorker(Worker w) {
			/*DefUseGraphMakerRunner runner = (DefUseGraphMakerRunner)w.getRunner();
			if(!isShutdown) {
				synchronized (fields) {
					Set<String> f = runner.getFields();
					if(f != null)
						fields.addAll(f);
				}
				synchronized (methods) {
					Set<String> m = runner.getMethods();
					if(m != null)
						methods.addAll(m);
				}
				synchronized (stringConstToUses) {
					Map<String,Set<String>> stringConsts = runner.getStringConsts();
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
				synchronized (startToUses) {
					Map<String,Set<String>> values = runner.getValues();
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
			}
			runner.clearData();*/
			super.endWorker(w);
		}
		
		@Override
		protected void endGroup() {
			/*synchronized (fields) {
				writeFields(fields,fieldsOut,name,logger);
				fields = null;
			}
			synchronized (methods) {
				writeMethods(methods,methodsOut,name,logger);
				methods = null;
			}
			synchronized (stringConstToUses) {
				writeStringConst(stringConstToUses, stringConstOut, name, logger);
				stringConstToUses = null;
			}
			synchronized (startToUses) {
				writeValues(startToUses, usesOut, name, logger);
				startToUses = null;
			}*/
			super.endGroup();
		}
		
	}
	
	private class DefUseGraphMakerRunner implements Runnable {
		
		private final EntryPoint ep;
		private final JimpleICFG baseICFG;
		private final ILogger logger;
		private final DefUseGraphMaker vtm;
		/*private volatile Set<String> fields;
		private volatile Set<String> methods;
		private volatile Map<String,Set<String>> stringConsts;
		private volatile Map<String,Set<String>> startToUses;*/
		
		public DefUseGraphMakerRunner(EntryPoint ep, JimpleICFG baseICFG,
				DefUseGraphMaker vtm, ILogger logger) {
			this.ep = ep;
			this.baseICFG = baseICFG;
			this.logger = logger;
			this.vtm = vtm;
			/*this.fields = null;
			this.methods = null;
			this.stringConsts = null;
			this.startToUses = null;*/
		}

		@Override
		public void run() {
			IExcludeHandler excludeHandler = null;
			IJimpleICFG icfg = null;
			
			logger.fineInfo("{}: Begin making def use graphs for ep '{}'.",name,ep);
			
			try{
				excludeHandler = dataAccessor.getExcludedElementsDB().createNewExcludeHandler(ep);
				icfg = new ExcludingJimpleICFG(ep, baseICFG, new ExcludingEdgePredicate(baseICFG.getCallGraph(), excludeHandler));
				
				DefUseGraph defUseGraph = vtm.makeDefUseGraphs(dataAccessor.getControlPredicatesDB().getUnits(ep), ep, icfg, logger);
				
				try {
					dataAccessor.getDefUseGraphDB().writeAndAddDefUseGraph(ep, defUseGraph, dataAccessor.getConfig().getFilePath("acminer_defusegraph-dir"));
				} catch(Throwable t) {
					logger.fatal("{}: An error occured in writing the DefUseGraph for '{}'.",t,name,ep);
					throw new IgnorableRuntimeException();
				}
				
				/*logger.fineInfo("{}: Initilizing the def use graph for '{}'.",name,ep.toString());
				defUseGraph.resolveStartNodeToDefStrings();
				logger.fineInfo("{}: Successfully initilized the def use graph for '{}'.",name,ep.toString());
				
				Triple<Set<String>, Set<String>, Map<String, Set<String>>> data = vtm.gatherData(defUseGraph, ep, logger);
				
				this.fields = data.getFirst();
				this.methods = data.getSecond();
				this.stringConsts = data.getThird();
				this.startToUses = new HashMap<>();
				Map<StartNode, Set<String>> startNodesToDefStrings = defUseGraph.getStartNodesToDefStrings();
				for(StartNode sn : startNodesToDefStrings.keySet()) {
					String key = "Stmt: " + sn.toString() + " Source: " + sn.getSource();
					Set<String> temp = startToUses.get(key);
					if(temp == null) {
						temp = new HashSet<>();
						startToUses.put(key, temp);
					}
					temp.addAll(startNodesToDefStrings.get(sn));
				}
				
				defUseGraph.clearStartNodesToDefStrings();//Free up memory now that we don't need to store these anymore
				
				writeValues();
				writeFields();
				writeMethods();
				writeStringConst();
				
				addFields();
				addMethods();
				addStringConst();
				addValues();*/
				
				logger.fineInfo("{}: The def use graph maker succeeded for ep '{}'.",name,ep);
			} catch(IgnorableRuntimeException t) {
				throw t;
			} catch(Throwable t) {
				logger.fatal("{}: An unexpected error occured in the def use graph maker for ep '{}'.",t,ep);
				throw new IgnorableRuntimeException();
			}
		}
		
		/*public void clearData() {
			this.fields = null;
			this.methods = null;
			this.stringConsts = null;
			this.startToUses = null;
		}
		
		public Set<String> getFields() {return fields;}
		public Set<String> getMethods() {return methods;}
		public Map<String,Set<String>> getStringConsts() {return stringConsts;}
		public Map<String,Set<String>> getValues() {return startToUses;}
		
		private void addFields() {
			synchronized (allFields) {
				allFields.addAll(fields);
			}
		}
		
		private void addMethods() {
			synchronized (allMethods) {
				allMethods.addAll(methods);
			}
		}
		
		private void addStringConst() {
			synchronized (allStringConst) {
				for(String stringConst : stringConsts.keySet()) {
					Set<String> temp = allStringConst.get(stringConst);
					if(temp == null) {
						temp = new HashSet<>();
						allStringConst.put(stringConst, temp);
					}
					temp.addAll(stringConsts.get(stringConst));
				}
			}
		}*/
		
		/*private void addValues() {
			synchronized (allValues) {
				for(String s : startToUses.keySet()) {
					Set<String> temp = allValues.get(s);
					if(temp == null) {
						temp = new HashSet<>();
						allValues.put(s, temp);
					}
					temp.addAll(startToUses.get(s));
				}
			}
		}
		
		private void writeFields() {
			Path p = stubToFieldOutput.get(stub);
			synchronized (p) {
				try (PrintStreamUnixEOL ps = new PrintStreamUnixEOL(Files.newOutputStream(p,StandardOpenOption.APPEND,
						StandardOpenOption.CREATE,StandardOpenOption.WRITE))) {
					ps.println("Entry Point: " + ep.toString());
					for(String s : fields) {
						ps.println("  Field: " + s);
					}
				} catch(Throwable t) {
					logger.fatal("{}: An error occured when writing to file '{}' for '{}' of '{}'.",name,p,stub,ep);
					throw new IgnorableRuntimeException();
				}
			}
		}
		
		private void writeMethods() {
			Path p = stubToMethodOutput.get(stub);
			synchronized (p) {
				try (PrintStreamUnixEOL ps = new PrintStreamUnixEOL(Files.newOutputStream(p,StandardOpenOption.APPEND,
						StandardOpenOption.CREATE,StandardOpenOption.WRITE))) {
					ps.println("Entry Point: " + ep.toString());
					for(String s : methods) {
						ps.println("  Method: " + s);
					}
				} catch(Throwable t) {
					logger.fatal("{}: An error occured when writing to file '{}' for '{}' of '{}'.",name,p,stub,ep);
					throw new IgnorableRuntimeException();
				}
			}
		}
		
		private void writeStringConst() {
			Path p = stubToStringConstOutput.get(stub);
			synchronized (p) {
				try (PrintStreamUnixEOL ps = new PrintStreamUnixEOL(Files.newOutputStream(p,StandardOpenOption.APPEND,
						StandardOpenOption.CREATE,StandardOpenOption.WRITE))) {
					ps.println("Entry Point: " + ep.toString());
					for(String s : stringConsts.keySet()) {
						ps.println("  StringConst: " + s);
						for(String k : stringConsts.get(s)) {
							ps.println("    Use: " + k);
						}
					}
				} catch(Throwable t) {
					logger.fatal("{}: An error occured when writing to file '{}' for '{}' of '{}'.",name,p,stub,ep);
					throw new IgnorableRuntimeException();
				}
			}
		}
		
		private void writeValues() {
			Path p = stubToValueOutput.get(stub);
			Map<String,BigInteger> countMap = DefUseGraph.computeAllStatementsCount(startToUses);
			BigInteger totalUses = BigInteger.ZERO;
			for(BigInteger i : countMap.values()) {
				totalUses = totalUses.add(i);
			}
			synchronized (p) {
				try (PrintStreamUnixEOL ps = new PrintStreamUnixEOL(Files.newOutputStream(p,StandardOpenOption.APPEND,
						StandardOpenOption.CREATE,StandardOpenOption.WRITE))) {
					ps.println("Entry Point: " + ep.toString() + 
							" Totals: Unique Control Predicates = " + countMap.keySet().size() + ", Unique Uses = " + totalUses.toString());
					for(String s : startToUses.keySet()) {
						ps.println("  " + s + " Count: " + countMap.get(s).toString());
						for(String l : startToUses.get(s)) {
							ps.println("    Def: " + l);
						}
					}
				} catch(Throwable t) {
					logger.fatal("{}: An error occured when writing to file '{}' for '{}' of '{}'.",name,p,stub,ep);
					throw new IgnorableRuntimeException();
				}
			}
		}*/
		
	}

}
