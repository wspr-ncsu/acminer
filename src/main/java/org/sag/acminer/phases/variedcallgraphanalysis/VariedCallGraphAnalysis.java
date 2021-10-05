package org.sag.acminer.phases.variedcallgraphanalysis;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.database.excludedelements.IExcludeHandler;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.concurrent.CountingThreadExecutor;
import org.sag.common.concurrent.IgnorableRuntimeException;
import org.sag.common.graphtools.TGFGraphWriter;
import org.sag.common.io.FileHelpers;
import org.sag.common.io.PrintStreamUnixEOL;
import org.sag.common.logging.ILogger;
import org.sag.common.tools.SortingMethods;
import org.sag.common.tuple.Pair;
import org.sag.common.tuple.Triple;
import org.sag.main.config.Config;
import org.sag.main.phase.IPhaseHandler;
import org.sag.main.phase.IPhaseOption;
import org.sag.soot.SootSort;
import org.sag.soot.callgraph.JimpleICFG.BasicEdgePredicate;
import org.sag.soot.graphtools.TGFSootEdgeTranslator;
import org.sag.soot.graphtools.TGFSootNodeTranslator;

import soot.Scene;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class VariedCallGraphAnalysis {
	
	private final IPhaseHandler handler;
	private final ILogger mainLogger;
	private final IACMinerDataAccessor dataAccessor;
	private final String cn;
	private final Config config;
	
	public VariedCallGraphAnalysis(IACMinerDataAccessor dataAccessor, IPhaseHandler handler, ILogger mainLogger){
		this.dataAccessor = dataAccessor;
		this.handler = handler;
		this.mainLogger = mainLogger;
		this.cn = this.getClass().getSimpleName();
		this.config = dataAccessor.getConfig();
	}
	
	private boolean isOptionEnabled(String name) {
		IPhaseOption<?> o = handler.getPhaseOptionUnchecked(name);
		if(o == null || !o.isEnabled())
			return false;
		return true;
	}
	
	//Create path to output file that does not exist
	private final Path getOutputFilePath(Path rootOutDir, SootMethod m, String uniq, String ext) {
		Path output = null;
		try{
			StringBuilder sb2 = new StringBuilder();
			String className = m.getDeclaringClass().getShortName();
			int i3 = className.lastIndexOf('$');
			if(i3 > 0 && className.length() > 1){
				className = className.substring(i3+1);
			}
			sb2.append(FileHelpers.replaceWhitespace(FileHelpers.cleanFileName(className))).append("_");
			String retType = m.getReturnType().toString();
			int i = retType.lastIndexOf('.');
			if(i > 0 && retType.length() > 1) {
				retType = retType.substring(i+1);
			}
			int i2 = retType.lastIndexOf('$');
			if(i2 > 0 && retType.length() > 1){
				retType = retType.substring(i2+1);
			}
			sb2.append(FileHelpers.replaceWhitespace(FileHelpers.cleanFileName(retType))).append("_");
			sb2.append(FileHelpers.replaceWhitespace(FileHelpers.cleanFileName(m.getName())));
			output = FileHelpers.getPath(rootOutDir, sb2.toString());
			
			StringBuilder sb3 = new StringBuilder();
			sb3.append("_").append(uniq).append(ext);
			output = FileHelpers.getPath(sb3.insert(0, FileHelpers.trimFullFilePath(output.toString(), false, sb3.length())).toString());
		}catch(Throwable t){
			mainLogger.fatal("{}: Failed to construct the output file for output directory '{}' and method '{}'.",
					t,cn,rootOutDir,m);
			throw new IgnorableRuntimeException();
		}
		return output;
	}
	
	private final static int digits(int n) {
		int len = String.valueOf(n).length();
		if(n < 0)
			return len - 1;
		else
			return len;
	}
	
	private final static String padNum(int n, int digits) {
		return String.format("%"+digits+"d", n);
	}
	
	public boolean run() {
		boolean reachingEnabled = isOptionEnabled(VariedCallGraphAnalysisHandler.optReachingGraphs);
		boolean dumpnative = isOptionEnabled(VariedCallGraphAnalysisHandler.optDumpNative);
		boolean successOuter = true;
		
		mainLogger.info("{}: Begin the special call graph analysis.",cn);
		
		//Something is enabled the create the main output directory
		if(reachingEnabled) {
			if(!reachingAnalysis((Path)handler.getPhaseOptionUnchecked(VariedCallGraphAnalysisHandler.optReachingGraphs).getValue()))
				successOuter = false;
		}
		if(dumpnative) {
			Path dumpFile = dataAccessor.getConfig().getFilePath("debug_eps-to-native-methods-dump-file");
			Path dumpFile2 = dataAccessor.getConfig().getFilePath("debug_native-methods-from-eps-dump-file");
			if(!dumpNative(dumpFile,dumpFile2))
				successOuter = false;
		}
		
		if(successOuter)
			mainLogger.info("{}: Successfully completed the special call graph analysis.",cn);
		else
			mainLogger.info("{}: Failed to complete one or more of the special call graph analysis.",cn);
		return successOuter;
	}
	
	private boolean dumpNative(Path outputPath, Path outputPath2) {
		CallGraph cg = Scene.v().getCallGraph();
		Set<EntryPoint> eps = dataAccessor.getEntryPoints();
		CountingThreadExecutor exe = new CountingThreadExecutor();
		final Map<SootMethod,Set<EntryPoint>> epsToNativeMethods = new HashMap<>();
		boolean successOuter = true;
		
		mainLogger.info("{}: Begin dumping native methods.",cn);
		
		try {
			for(final EntryPoint ep : eps) {
				exe.execute(new Runnable() {
					public void run() {
						try {
							SootMethod entryPoint = ep.getEntryPoint();
							Set<SootMethod> visited = new HashSet<>();
							Queue<SootMethod> toVisit = new ArrayDeque<>();
							IExcludeHandler excludeHandler = dataAccessor.getExcludedElementsDB().createNewExcludeHandler(ep);
							BasicEdgePredicate edgePred = new BasicEdgePredicate(false);//Omit reflective calls 
							Set<Edge> allowedEdges = new HashSet<>();
							Set<SootMethod> nativeMethods = new HashSet<>();
							toVisit.add(entryPoint);
							while(!toVisit.isEmpty()) {
								SootMethod cur = toVisit.poll();
								if(!excludeHandler.isExcludedMethodWithOverride(cur)) { //Omit excluded native methods and don't traverse excluded methods
									if(cur.isNative())
										nativeMethods.add(cur);
									if(visited.add(cur)) {
										Iterator<Edge> it = cg.edgesOutOf(cur);
										while(it.hasNext()) {
											Edge e = it.next();
											if(edgePred.want(e)) {
												allowedEdges.add(e);
												toVisit.add(e.tgt());
											}
										}
									}
								}
							}
							
							if(!nativeMethods.isEmpty()) {
								for(SootMethod sm : nativeMethods) {
									synchronized(epsToNativeMethods) {
										Set<EntryPoint> entryPoints = epsToNativeMethods.get(sm);
										if(entryPoints == null) {
											entryPoints = new HashSet<>();
											epsToNativeMethods.put(sm, entryPoints);
										}
										entryPoints.add(ep);
									}
								}
							}	
						} catch(IgnorableRuntimeException t) {
							throw t;
						} catch(Throwable t) {
							mainLogger.fatal("{}: An unexpected exception occured for entry point '{}' when dumping native methods.",t,cn,ep);
							throw new IgnorableRuntimeException();
						}
					}
				});
			}
		} catch(IgnorableRuntimeException t) {
			successOuter = false;
		} catch(Throwable t) {
			mainLogger.fatal("{}: An unexpected exception occured in when dumping native methods.",t,cn);
			successOuter = false;
		} finally {
			//Shutdown the executors
			if(exe != null && !exe.shutdownWhenFinished()){
				mainLogger.fatal(CountingThreadExecutor.computeJointErrorMsg(exe.getAndClearExceptions(), 
						"Failed to wait for the executor to terminate when dumping native methods.", cn));
				successOuter = false;
			}
		}
		
		if(!epsToNativeMethods.isEmpty()) {
			for(SootMethod n : epsToNativeMethods.keySet()) {
				epsToNativeMethods.put(n, SortingMethods.sortSet(epsToNativeMethods.get(n)));
			}
			Map<SootMethod,Set<EntryPoint>> epsToNativeMethodsSorted = SortingMethods.sortMapKey(epsToNativeMethods, SootSort.smComp);
			Set<SootMethod> nativeMethodsUniq = epsToNativeMethods.keySet();
			try {
				try(PrintStream ps = new PrintStreamUnixEOL(Files.newOutputStream(outputPath))) {
					for(SootMethod sm : epsToNativeMethodsSorted.keySet()) {
						ps.println(sm);
						for(EntryPoint ep : epsToNativeMethodsSorted.get(sm)) {
							ps.println("  " + ep);
						}
					}
				}
				try(PrintStream ps = new PrintStreamUnixEOL(Files.newOutputStream(outputPath2))) {
					for(SootMethod sm : nativeMethodsUniq) {
						ps.println(sm);
					}
				}
			} catch(Throwable t) {
				mainLogger.fatal("{}: Failed to dump native methods to file.",t,cn);
				successOuter = false;
			}
		}
		return successOuter;
	}
	
	private boolean reachingAnalysis(Path inputPath) {
		CallGraph cg = Scene.v().getCallGraph();
		Set<EntryPoint> eps = dataAccessor.getEntryPoints();
		CountingThreadExecutor exe = new CountingThreadExecutor();
		final Map<EntryPoint,Triple<Set<SootMethod>,Integer,Integer>> epsToSinkSubGraph = new HashMap<>();
		final Set<SootMethod> sinks = new LinkedHashSet<>();
		final Path outputDir = config.getFilePath("debug_reaching-dir");
		boolean successOuter = true;
		
		mainLogger.info("{}: Begin the reaching analysis.",cn);
		
		try {
			FileHelpers.processDirectory(outputDir, true, false);
		} catch(Throwable t) {
			mainLogger.fatal("{}: Failed to create output directory '{}'. Stoping analysis!",cn,outputDir);
			successOuter = false;
		}
		
		if(successOuter) {
			Set<SootMethod> tempSinks = new HashSet<>();
			try(BufferedReader br = Files.newBufferedReader(inputPath)) {
				String s;
				while((s = br.readLine()) != null) {
					s = s.trim();
					if(!s.isEmpty() && !s.startsWith("//")) {
						if(Scene.v().containsMethod(s)){
							SootMethod sm = Scene.v().grabMethod(s);//The getMethodUnsafe alternative to getMethod
							if(sm != null)
								tempSinks.add(sm);
						}
					}
				}
				sinks.addAll(SortingMethods.sortSet(tempSinks,SootSort.smComp));
			} catch(Throwable t) {
				mainLogger.fatal("{}: Failed to read in the sinks for the reaching analysis.",t,cn);
				successOuter = false;
			}
		}
		
		if(successOuter && !sinks.isEmpty()) {
			try {
				int i = 0;
				for(final EntryPoint ep : eps) {
					final int uniq = i++;
					exe.execute(new Runnable() {
						public void run() {
							try {
								SootMethod entryPoint = ep.getEntryPoint();
								Set<SootMethod> visited = new HashSet<>();
								Queue<SootMethod> toVisit = new ArrayDeque<>();
								IExcludeHandler excludeHandler = dataAccessor.getExcludedElementsDB().createNewExcludeHandler(ep);
								BasicEdgePredicate edgePred = new BasicEdgePredicate(false);//Omit reflective calls 
								Set<Edge> allowedEdges = new HashSet<>();
								Set<SootMethod> foundSinks = new HashSet<>();
								toVisit.add(entryPoint);
								while(!toVisit.isEmpty()) {
									SootMethod cur = toVisit.poll();
									if(sinks.contains(cur))
										foundSinks.add(cur);
									if(visited.add(cur) && !excludeHandler.isExcludedMethodWithOverride(cur)) {
										Iterator<Edge> it = cg.edgesOutOf(cur);
										while(it.hasNext()) {
											Edge e = it.next();
											if(edgePred.want(e)) {
												allowedEdges.add(e);
												toVisit.add(e.tgt());
											}
										}
									}
								}
								
								if(!foundSinks.isEmpty()) {
									visited = new HashSet<>();
									toVisit = new ArrayDeque<>();
									Set<Edge> toSinks = new HashSet<>();
									toVisit.addAll(foundSinks);
									while(!toVisit.isEmpty()) {
										SootMethod cur = toVisit.poll();
										visited.add(cur);
										Iterator<Edge> it = cg.edgesInto(cur);
										while(it.hasNext()) {
											Edge e = it.next();
											if(allowedEdges.contains(e)) {
												SootMethod src = e.src();
												toSinks.add(e);
												if(!visited.contains(src) && !toVisit.contains(src)) {
													toVisit.add(src);
												}
											}
										}
									}
									
									synchronized(epsToSinkSubGraph) {
										epsToSinkSubGraph.put(ep, new Triple<>(SortingMethods.sortSet(foundSinks,SootSort.smComp),
												visited.size(),toSinks.size()));
									}
									
									Path output = getOutputFilePath(outputDir, entryPoint, uniq+"", ".tgf");
									TGFGraphWriter<SootMethod,Edge> graphWriter = new TGFGraphWriter<>(SortingMethods.sortSet(visited,SootSort.smComp),
											SortingMethods.sortSet(toSinks,SootSort.edgeComp), new TGFSootNodeTranslator(), 
											new TGFSootEdgeTranslator());
									graphWriter.writeToFile(output);
								}
								
								mainLogger.info("{}: Successfully completed the reaching analysis for '{}'.",cn,entryPoint);
								
							} catch(IgnorableRuntimeException t) {
								throw t;
							} catch(Throwable t) {
								mainLogger.fatal("{}: An unexpected exception occured for entry point '{}' in the reaching analysis.",t,cn,
										ep.getEntryPoint());
								throw new IgnorableRuntimeException();
							}
						}
					});
				}
			} catch(IgnorableRuntimeException t) {
				successOuter = false;
			} catch(Throwable t) {
				mainLogger.fatal("{}: An unexpected exception occured in the reaching analysis.",t,cn);
				successOuter = false;
			} finally {
				//Shutdown the executors
				if(exe != null && !exe.shutdownWhenFinished()){
					mainLogger.fatal(CountingThreadExecutor.computeJointErrorMsg(exe.getAndClearExceptions(), 
							"Failed to wait for the executor to terminate during reaching analysis.", cn));
					successOuter = false;
				}
			}
		}
		
		if(successOuter) {
			if(!epsToSinkSubGraph.isEmpty()) {
				Path overviewPath = FileHelpers.getPath(outputDir, "__overview__.txt");
				int maxEdges = 0;
				int maxNodes = 0;
				Map<SootMethod,Map<SootMethod,Pair<Integer,Integer>>> map = new HashMap<>();
				for(EntryPoint ep : epsToSinkSubGraph.keySet()) {
					Triple<Set<SootMethod>,Integer,Integer> data = epsToSinkSubGraph.get(ep);
					int nodes = digits(data.getSecond());
					int edges = digits(data.getThird());
					if(nodes > maxNodes)
						maxNodes = nodes;
					if(edges > maxEdges)
						maxEdges = edges;
					for(SootMethod sink : data.getFirst()) {
						Map<SootMethod,Pair<Integer,Integer>> epsToSizes = map.get(sink);
						if(epsToSizes == null) {
							epsToSizes = new HashMap<>();
							map.put(sink, epsToSizes);
						}
						epsToSizes.put(ep.getEntryPoint(), new Pair<>(data.getSecond(),data.getThird()));
					}
				}
				
				for(SootMethod sink : map.keySet())
					map.put(sink, SortingMethods.sortMapKey(map.get(sink), SootSort.smComp));
				map = SortingMethods.sortMapKey(map, SootSort.smComp);
				
				try(PrintStreamUnixEOL ps = new PrintStreamUnixEOL(Files.newOutputStream(overviewPath))) {
					for(SootMethod sink : map.keySet()) {
						ps.println("Sink: " + sink);
						Map<SootMethod,Pair<Integer,Integer>> epsToSizes = map.get(sink);
						for(SootMethod entryPoint : epsToSizes.keySet()) {
							Pair<Integer,Integer> sizes = epsToSizes.get(entryPoint);
							ps.println("  Nodes: " + padNum(sizes.getFirst(),maxNodes) + " Edges: " + padNum(sizes.getSecond(),maxEdges) 
							+ " EntryPoint: " + entryPoint);
						}
					}
				} catch(Throwable t) {
					mainLogger.fatal("{}: Failed to write file '{}' in the reaching analysis.",t,cn,overviewPath);
					successOuter = false;
				}
			}
		}
		
		if(successOuter)
			mainLogger.info("{}: Successfully completed the reaching analysis.",cn);
		else
			mainLogger.info("{}: Failed to complete the reaching analysis.",cn);
		return successOuter;
	}

}
