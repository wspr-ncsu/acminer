package org.sag.acminer.phases.acminerdebug;

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.database.excludedelements.IExcludeHandler;
import org.sag.acminer.phases.acminerdebug.handler.AbstractOutputHandler;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.concurrent.IgnorableRuntimeException;
import org.sag.common.concurrent.LoggingWorkerGroup;
import org.sag.common.concurrent.WorkerCountingThreadExecutor;
import org.sag.common.graphtools.AlEdge;
import org.sag.common.graphtools.AlNode;
import org.sag.common.graphtools.Formatter;
import org.sag.common.graphtools.GraphmlGenerator;
import org.sag.common.graphtools.AlElement.Color;
import org.sag.common.io.FileHelpers;
import org.sag.common.io.PrintStreamUnixEOL;
import org.sag.common.logging.ILogger;
import org.sag.common.logging.LoggerWrapperSLF4J;
import org.sag.common.tools.SortingMethods;
import org.sag.common.tuple.Pair;
import org.sag.common.tuple.Triple;
import org.sag.soot.SootSort;

import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class SubgraphCountingOutputHandler extends AbstractOutputHandler {
	
	
	private final Map<String,Set<EntryPoint>> groups;
	private final WorkerCountingThreadExecutor exe;
	private final ILogger logger;
	private final CallGraph cg;
	private final GraphmlGenerator outGraphml;
	
	//Assume the id has had trim run on it and eps are sorted per group
	public SubgraphCountingOutputHandler(WorkerCountingThreadExecutor exe, GraphmlGenerator outGraphml, Map<String,Set<SootMethod>> groups, 
			Path rootOutputDir, IACMinerDataAccessor dataAccessor, ILogger logger) {
		super(rootOutputDir,dataAccessor);
		Objects.requireNonNull(groups);
		Objects.requireNonNull(exe);
		Objects.requireNonNull(outGraphml);
		this.outGraphml = outGraphml;
		this.logger = logger == null ? new LoggerWrapperSLF4J(CommonSubgraphOutputHandler.class) : logger;
		this.exe = exe;
		this.groups = new LinkedHashMap<>();
		for(String id : this.groups.keySet()) {
			if(id == null)
				throw new IllegalArgumentException("Error: The group id cannot be null.");
			if(id.isEmpty())
				throw new IllegalArgumentException("Error: The group id cannot be empty.");
			Set<SootMethod> eps = groups.get(id);
			if(eps == null)
				throw new IllegalArgumentException("Error: The individual sets of entry points cannot be null.");
			if(eps.contains(null))
				throw new IllegalArgumentException("Error: The entry points cannot be null.");
			Set<EntryPoint> resolvedEps = new HashSet<>();
			for(EntryPoint ep : dataAccessor.getEntryPoints()) {
				if(eps.contains(ep.getEntryPoint()))
					resolvedEps.add(ep);
			}
			if(resolvedEps.isEmpty())
				throw new IllegalArgumentException("Error: No entry points resolved.");
			this.groups.put(id, resolvedEps);
		}
		this.cg = Scene.v().getCallGraph();
	}
	
	private synchronized void executeRunner(Runnable runner, LoggingWorkerGroup g, ILogger logger) {
		try {
			exe.execute(runner,g);
		} catch(Throwable t) {
			logger.fatal("{}: Failed to execute '{}' for group '{}'.",t,cn,runner.toString(),g.getName());
			throw new IgnorableRuntimeException();
		}
	}
	
	public List<LoggingWorkerGroup> run() {
		try{
			FileHelpers.processDirectory(rootOutputDir,true,true);
		}catch(Throwable t){
			logger.fatal("{}: Failed to create and verify the output directory. Skipping.",t,cn);
			throw new IgnorableRuntimeException();
		}
		
		List<LoggingWorkerGroup> workerGroups = new ArrayList<>();
		try {
			for(String id : groups.keySet()) {
				Set<EntryPoint> eps = groups.get(id);
				Map<SootMethod, SubGraphRunner> runners = new LinkedHashMap<>();
				for(EntryPoint ep : eps) {
					runners.put(ep.getEntryPoint(), new SubGraphRunner(ep, cg, dataAccessor, dataAccessor.getExcludedElementsDB().createNewExcludeHandler(ep)));
				}
				SubGraphWorkerGroup g = new SubGraphWorkerGroup(runners, outGraphml, rootOutputDir, cg, cn, id, logger, false, false);
				workerGroups.add(g);
				for(SubGraphRunner r : runners.values()) {
					executeRunner(r, g, logger);
				}
				g.unlockInitialLock();
			}
		} catch(Throwable t) {
			logger.fatal("{}: An unexpected exception occured.",t,cn);
			throw new IgnorableRuntimeException();
		}
		return workerGroups;
	}
	
	private static final class SubGraphWorkerGroup extends LoggingWorkerGroup {
		private final Map<SootMethod,SubGraphRunner> runners;
		private final Path rootOutputDir;
		private final CallGraph cg;
		private final GraphmlGenerator outGraphml;
		public SubGraphWorkerGroup(Map<SootMethod,SubGraphRunner> runners, GraphmlGenerator outGraphml, Path rootOutputDir, 
				CallGraph cg, String phaseName, String name, ILogger logger, boolean shutdownOnError, boolean closeLogger) {
			super(phaseName, name, logger, shutdownOnError, closeLogger);
			this.runners = runners;
			this.rootOutputDir = rootOutputDir;
			this.cg = cg;
			this.outGraphml = outGraphml;
		}
		
		@Override
		protected void endGroup() {
			try {
				Map<SootMethod,MethodData> ret = new HashMap<>();
				for(SootMethod ep : runners.keySet()) {
					Map<SootMethod, Pair<AtomicInteger, List<Integer>>> data = runners.get(ep).getValue();
					Map<SootMethod, Map<SootMethod,Set<Unit>>> cpData = runners.get(ep).getCPValue();
					if(data != null) {
						for(SootMethod sm : data.keySet()) {
							MethodData md = ret.get(sm);
							if(md == null) {
								md = new MethodData(sm);
								ret.put(sm,md);
							}
							md.updateDepths(data.get(sm).getSecond(), ep);
							md.updateFreq(data.get(sm).getFirst().get(), ep);
						}
					}
					if(cpData != null) {
						for(SootMethod sm : cpData.keySet()) {
							MethodData md = ret.get(sm);
							if(md == null) {
								logger.fatal("{}: The method '{}' exists in the CP data but not the counting data !?!",name,sm.toString());
								throw new RuntimeException();
							}
							md.updateCPData(cpData.get(sm), ep);
						}
					}
				}
			
				int maxDepth = 0;
				for(MethodData md : ret.values()) {
					for(int i : md.depths) {
						if(i > maxDepth)
							maxDepth = i;
					}
				}
				for(MethodData md : ret.values()) {
					md.computeSkewedDepthAvg(maxDepth, 4);
					md.computeMissingEps(runners.size());
					md.computeAvgCPCount();
					md.sortData();
				}
				
				outputCountingData(ret);
				outputCPData(ret);
				outputGraphs(runners.keySet(),ret,cg);
			} catch(Throwable t) {
				logger.fatal("{}: An unexpected exception occured for {} data finilization.",phaseName,name);
				this.addFailedToExecuteException(t);
			}
			
			super.endGroup();
		}
		
		private static final Comparator<Map.Entry<SootMethod,MethodData>> cpMDComp = new Comparator<Map.Entry<SootMethod,MethodData>>() {
			@Override
			public int compare(Entry<SootMethod, MethodData> o1, Entry<SootMethod, MethodData> o2) {
				//Sort largest number first
				//Since we want the methods with the largest number of CP + the ones with the highest depth + the ones that occur most often
				int ret = Integer.compare(o2.getValue().epToDepths.size(), o1.getValue().epToDepths.size());
				if(ret == 0) {
					ret = Integer.compare(o2.getValue().avgCPCount, o1.getValue().avgCPCount);
					if(ret == 0)
						ret = Integer.compare(o1.getValue().avgDepth, o2.getValue().avgDepth);
				}
				return ret;
			}
		};
		
		private static final Color getColor(int val) {
			Color c = null;
			if(val > 1200)
				c = Color.GRAY;
			else if(val <= 1200 && val > 1000)
				c = Color.RED;
			else if(val <= 1000 && val > 800)
				c = Color.ORANGE;
			else if(val <= 800 && val > 600)
				c = Color.YELLOW;
			else if(val <= 600 && val > 400)
				c = Color.PINK;
			else if(val <= 400 && val > 200)
				c = Color.BLUE;
			else if(val <= 200 && val > 0)
				c = Color.GREEN;
			else
				c = Color.WHITE;
			return c;
		}
		
		private static final class EPSubGraphFormatter extends Formatter {
			private final SootMethod ep;
			private final Map<SootMethod,MethodData> methods;
			private final CallGraph cg;
			private final Set<AlNode> alNodes;
			private final Set<AlEdge> alEdges;
			private volatile long id;
			public EPSubGraphFormatter(SootMethod ep, Map<SootMethod,MethodData> methods, CallGraph cg, Path outputPath) {
				super(0,-1,-1,-1,outputPath);
				this.ep = ep;
				this.methods = methods;
				this.cg = cg;
				this.alNodes = new TreeSet<>();
				this.alEdges = new TreeSet<>();
				this.id = 0;
			}
			public Collection<AlNode> getNodes() { return alNodes; }
			public Collection<AlEdge> getEdges() { return alEdges; }
			public void format() {
				Map<SootMethod,AlNode> nton = new HashMap<>();
				for(MethodData md : methods.values()) {
					if(md.epToCPCount.containsKey(ep)) {
						int cpCount = md.epToCPCount.get(ep);
						if(cpCount > 0) {
							AlNode alNode = new AlNode(id++,md.method.getSignature());
							alNodes.add(alNode);
							nton.put(md.method, alNode);
							alNode.setColors(nodeColorIndex, Collections.singletonList(getColor(cpCount)));
						}
					}
				}
				for(MethodData md : methods.values()) {
					AlNode cur = nton.get(md.method);
					if(cur != null) {
						Iterator<Edge> itEdge = cg.edgesOutOf(md.method);
						while(itEdge.hasNext()){
							AlNode child = nton.get(itEdge.next().tgt());
							if(child != null)
								alEdges.add(new AlEdge(id++,cur,child));
						}
					}
				}
			}
		}
		
		private static final class SubGraphFormatter extends Formatter {
			private final Map<SootMethod,MethodData> methods;
			private final CallGraph cg;
			private final Set<AlNode> alNodes;
			private final Set<AlEdge> alEdges;
			private volatile long id;
			private final int limit;
			private final boolean limitIsMax;
			public SubGraphFormatter(Map<SootMethod,MethodData> methods, CallGraph cg, Path outputPath) {
				this(methods,cg,outputPath,1,false);
			}
			public SubGraphFormatter(Map<SootMethod,MethodData> methods, CallGraph cg, Path outputPath, int limit, boolean limitIsMax) {
				super(0,-1,-1,-1,outputPath);
				this.methods = methods;
				this.cg = cg;
				this.alNodes = new TreeSet<>();
				this.alEdges = new TreeSet<>();
				this.id = 0;
				this.limit = limit;
				this.limitIsMax = limitIsMax;
			}
			private boolean keepNode(MethodData md, int epsMax) {
				if(md.epToDepths.size() >= epsMax && md.avgCPCount > 0) {
					if(limitIsMax)
						return md.avgCPCount <= limit;
					else
						return md.avgCPCount >= limit;
				}
				return false;
			}
			public Collection<AlNode> getNodes() { return alNodes; }
			public Collection<AlEdge> getEdges() { return alEdges; }
			public void format() {
				Map<SootMethod,AlNode> nton = new HashMap<>();
				int max = 0;
				for(MethodData md : methods.values()) {
					if(md.epToDepths.size() > max)
						max = md.epToDepths.size();
				}
				max = max / 2;
				for(MethodData md : methods.values()) {
					//Only include those who have CP or lead to them and who are referenced by at least 3/4 of the eps for the group
					if(keepNode(md,max)) {
						AlNode alNode = new AlNode(id++,md.method.getSignature());
						alNodes.add(alNode);
						nton.put(md.method, alNode);
						alNode.setColors(nodeColorIndex, Collections.singletonList(getColor(md.avgCPCount)));
					}
				}
				for(MethodData md : methods.values()) {
					AlNode cur = nton.get(md.method);
					if(cur != null) {
						Iterator<Edge> itEdge = cg.edgesOutOf(md.method);
						while(itEdge.hasNext()){
							AlNode child = nton.get(itEdge.next().tgt());
							if(child != null)
								alEdges.add(new AlEdge(id++,cur,child));
						}
					}
				}
			}
		}
		
		private void outputGraphs(Set<SootMethod> eps, Map<SootMethod,MethodData> data, CallGraph cg) {
			try {
				Path output = FileHelpers.getPath(rootOutputDir, "group_" + name + "_graph_lim-1_min.graphml");
				SubGraphFormatter f = new SubGraphFormatter(data, cg, output);
				f.format();
				outGraphml.outputGraph(f);
				
				output = FileHelpers.getPath(rootOutputDir, "group_" + name + "_graph_lim-400_min.graphml");
				f = new SubGraphFormatter(data, cg, output, 400, false);
				f.format();
				outGraphml.outputGraph(f);
				
				output = FileHelpers.getPath(rootOutputDir, "group_" + name + "_graph_lim-400_max.graphml");
				f = new SubGraphFormatter(data, cg, output, 400, true);
				f.format();
				outGraphml.outputGraph(f);
				
				int i = 0;
				for(SootMethod ep : eps) {
					Path o = getOutputFilePath(rootOutputDir,ep,"g_"+name+"_"+i++,".graphml");
					EPSubGraphFormatter form = new EPSubGraphFormatter(ep, data, cg, o);
					form.format();
					outGraphml.outputGraph(form);
				}
			} catch(Throwable t) {
				logger.fatal("{}: Failed to create a new task to handle the writing of the common graph.",t,name);
				throw new RuntimeException();
			}
			logger.fineInfo("{}: Successfully created a new task to handle the writing of the common graph. If no error occurs "
					+ "when writing the file this call graph was output successfully.",name);
		}
		
		//Create path to output file that does not exist
		private Path getOutputFilePath(Path stubOutputDir, SootMethod m, String uniq, String ext) {
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
				output = FileHelpers.getPath(stubOutputDir, sb2.toString());
				
				StringBuilder sb3 = new StringBuilder();
				sb3.append("_").append(uniq).append(ext);
				output = FileHelpers.getPath(sb3.insert(0, FileHelpers.trimFullFilePath(output.toString(), false, sb3.length())).toString());
			}catch(Throwable t){
				logger.fatal("{}: Failed to construct the output file for output directory '{}' and method '{}'.",
						t,name,stubOutputDir,m);
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
			return String.format("%0"+digits+"d", n);
		}
		
		private void outputCPData(Map<SootMethod,MethodData> data) {
			int maxcps = 0;
			int maxeps = 0;
			for(MethodData md : data.values()) {
				if(md.avgCPCount > maxcps)
					maxcps = md.avgCPCount;
				if(md.epToDepths.size() > maxeps)
					maxeps = md.epToDepths.size();
			}
			maxcps = digits(maxcps);
			maxeps = digits(maxeps);
			data = SortingMethods.sortMap(data, cpMDComp);
			Path output = FileHelpers.getPath(rootOutputDir, "group_" + name + "_cp_count_simple.txt");
			try(PrintStreamUnixEOL out = new PrintStreamUnixEOL(Files.newOutputStream(output))) {
				out.println("Group: " + name);
				out.println("Entry Points: ");
				for(SootMethod ep : runners.keySet()) {
					out.println("  EP: " + ep.toString());
				}
				for(MethodData md : data.values()) {
					out.println("Method: EPS='" + padNum(md.epToDepths.size(),maxeps) + "' CPS='" + padNum(md.avgCPCount,maxcps) 
							+ "' Name='" + md.method.toString() + "'");
				}
			} catch(Throwable t) {
				logger.fatal("{}: Failed to output the cp count simple data for group {}.",phaseName,name);
				this.addFailedToExecuteException(t);
			}
			output = FileHelpers.getPath(rootOutputDir, "group_" + name + "_cp_count.txt");
			try(PrintStreamUnixEOL out = new PrintStreamUnixEOL(Files.newOutputStream(output))) {
				out.println("Group: " + name);
				out.println("Entry Points: ");
				for(SootMethod ep : runners.keySet()) {
					out.println("  EP: " + ep.toString());
				}
				for(MethodData md : data.values()) {
					out.println("Method: EPS='" + padNum(md.epToDepths.size(),maxeps) + "' CPS='" + padNum(md.avgCPCount,maxcps) 
							+ "' Name='" + md.method.toString() + "'");
					Map<SootMethod,MethodData> childs = new HashMap<>();
					Iterator<Edge> itEdge = cg.edgesOutOf(md.method);
					while(itEdge.hasNext()){
						MethodData child = data.get(itEdge.next().tgt());
						if(child != null)
							childs.put(child.method,child);
					}
					childs = SortingMethods.sortMap(childs,cpMDComp);
					for(MethodData child : childs.values()) {
						out.println("  Child Method: EPS='" + padNum(child.epToDepths.size(),maxeps) + "' CPS='" + padNum(child.avgCPCount,maxcps)
								+ "' Name='" + child.method.toString() + "'");
					}
				}
			} catch(Throwable t) {
				logger.fatal("{}: Failed to output the cp count data for group {}.",phaseName,name);
				this.addFailedToExecuteException(t);
			}
		}
		
		private void outputCountingData(Map<SootMethod,MethodData> data) {
			data = SortingMethods.sortMapValueAscending(data);
			Path output = FileHelpers.getPath(rootOutputDir, "group_" + name + "_count.txt");
			try(PrintStreamUnixEOL out = new PrintStreamUnixEOL(Files.newOutputStream(output))) {
				out.println("Group: " + name);
				out.println("Entry Points: ");
				for(SootMethod ep : runners.keySet()) {
					out.println("  EP: " + ep.toString());
				}
				for(MethodData md : data.values()) {
					Set<Integer> sted = SortingMethods.sortSet(new HashSet<>(md.depths));
					out.println("Method: Name='" + md.method.toString() + "' Freq='" + md.freq + "' EPS='" + md.epToFreq.size() + "' Depths='" 
							+ sted + "'");
					//If its all the entry points there is no point in printing out the info
					if(md.epToFreq.size() != runners.size()) {
						for(SootMethod ep : md.epToFreq.keySet()) {
							int freq = md.epToFreq.get(ep);
							List<Integer> depths = md.epToDepths.get(ep);
							out.println("  EntryPoint: Name='" + ep.toString() + "' Freq='" + freq + "' Depths='" + depths + "'");
						}
					}
				}
			} catch(Throwable t) {
				logger.fatal("{}: Failed to output the data for group {}.",phaseName,name);
				this.addFailedToExecuteException(t);
			}
			output = FileHelpers.getPath(rootOutputDir, "group_" + name + "_count_simple.txt");
			try(PrintStreamUnixEOL out = new PrintStreamUnixEOL(Files.newOutputStream(output))) {
				out.println("Group: " + name);
				out.println("Entry Points: ");
				for(SootMethod ep : runners.keySet()) {
					out.println("  EP: " + ep.toString());
				}
				for(MethodData md : data.values()) {
					Set<Integer> sted = SortingMethods.sortSet(new HashSet<>(md.depths));
					out.println("Method: Name='" + md.method.toString() + "' Freq='" + md.freq + "' EPS='" + md.epToFreq.size() + "' Depths='" 
							+ sted + "'");
				}
			} catch(Throwable t) {
				logger.fatal("{}: Failed to output the simple data for group {}.",phaseName,name);
				this.addFailedToExecuteException(t);
			}
		}
		
	}
	
	private static final class MethodData implements Comparable<MethodData> {
		private final SootMethod method;
		private volatile int freq;
		private volatile int avgCPCount;
		private final List<Integer> depths;
		private volatile Map<SootMethod,Set<Unit>> cpData;
		private volatile Map<SootMethod,Integer> epToFreq;
		private volatile Map<SootMethod,List<Integer>> epToDepths;
		private volatile Map<SootMethod,Map<SootMethod,Set<Unit>>> epToCPData;
		private volatile Map<SootMethod,Integer> epToCPCount;
		private int avgDepth;
		private int missingEps;
		public MethodData(SootMethod method) {
			this.method = method;
			this.freq = 0;
			this.depths = new ArrayList<>();
			this.epToFreq = new HashMap<>();
			this.epToDepths = new HashMap<>();
			this.epToCPData = new HashMap<>();
			this.epToCPCount = new HashMap<>();
			this.cpData = new HashMap<>();
			this.avgDepth = 0;
			this.missingEps = 0;
			this.avgCPCount = 0;
		}
		public void updateFreq(int freq, SootMethod ep) {
			this.freq += freq;
			this.epToFreq.put(ep, freq);
		}
		public void updateDepths(List<Integer> depths, SootMethod ep) {
			this.depths.addAll(depths);
			epToDepths.put(ep, depths);
		}
		public void updateCPData(Map<SootMethod,Set<Unit>> data, SootMethod ep) {
			if(data != null && !data.isEmpty()) {
				Map<SootMethod,Set<Unit>> curData = epToCPData.get(ep);
				if(curData == null) {
					curData = new HashMap<>();
					epToCPData.put(ep, curData);
				}
				for(SootMethod sm : data.keySet()) {
					Set<Unit> temp = curData.get(sm);
					if(temp == null) {
						temp = new HashSet<>();
						curData.put(sm, temp);
					}
					temp.addAll(data.get(sm));
					
					Set<Unit> fff = cpData.get(sm);
					if(fff == null) {
						fff = new HashSet<>();
						cpData.put(sm, fff);
					}
					fff.addAll(data.get(sm));
				}
				Set<Unit> units = new HashSet<>();
				for(Set<Unit> values : epToCPData.get(ep).values()) {
					units.addAll(values);
				}
				epToCPCount.put(ep, units.size());
			} else {
				epToCPData.put(ep, Collections.<SootMethod,Set<Unit>>emptyMap());
				epToCPCount.put(ep, 0);
			}
		}
		public void sortData() {
			Collections.sort(depths);
			this.epToFreq = SortingMethods.sortMapValueAscending(epToFreq);
			this.epToDepths = SortingMethods.sortMapKey(epToDepths, SootSort.smComp);
			for(List<Integer> l : epToDepths.values()) {
				Collections.sort(l);
			}
			this.epToCPCount = SortingMethods.sortMapValueAscending(epToCPCount);
			for(SootMethod ep : epToCPData.keySet()) {
				Map<SootMethod,Set<Unit>> temp = epToCPData.get(ep);
				for(SootMethod sm : temp.keySet()) {
					temp.put(sm, SortingMethods.sortSet(temp.get(sm),SootSort.unitComp));
				}
				epToCPData.put(ep, SortingMethods.sortMapKey(temp,SootSort.smComp));
			}
			this.epToCPData = SortingMethods.sortMapKey(epToCPData, SootSort.smComp);
			for(SootMethod sm : cpData.keySet()) {
				cpData.put(sm, SortingMethods.sortSet(cpData.get(sm),SootSort.unitComp));
			}
			this.cpData = SortingMethods.sortMapKey(cpData, SootSort.smComp);
		}
		public void computeAvgCPCount()	{
			if(epToCPCount.size() == 0) {
				this.avgCPCount = 0;
			} else {
				int sum = 0;
				for(int i : epToCPCount.values()) {
					sum += i;
				}
				if(sum == 0)
					this.avgCPCount = 0;
				else
					this.avgCPCount = sum / epToCPCount.size();
			}
		}
		public void computeMissingEps(int epsMax) {
			this.missingEps = epsMax - epToDepths.size();
		}
		public void computeSkewedDepthAvg(int maxDepth, int numBuckets) {
			if(maxDepth <= 0) maxDepth = 1;
			if(numBuckets > maxDepth) numBuckets = maxDepth - 1;
			if(numBuckets <= 0) numBuckets = 1;
			List<Triple<Integer,Integer,List<Integer>>> buckets = new ArrayList<>();
			int prev = 0;
			for(int i = 1; i <= numBuckets; i++) {
				int start;
				int end;
				int cur = (maxDepth * i) / numBuckets;
				if(i == 1)
					start = 0;
				else
					start = prev + 1;
				if(i == numBuckets)
					end = maxDepth;
				else
					end = cur;
				prev = cur;
				buckets.add(new Triple<Integer, Integer, List<Integer>>(start, end, new ArrayList<Integer>()));
			}
			for(int i : depths) {
				boolean found = false;
				for(Triple<Integer,Integer,List<Integer>> t : buckets) {
					if(t.getFirst() <= i && t.getSecond() >= i) {
						t.getThird().add(i);
						found = true;
						break;
					}
				}
				if(!found)
					throw new RuntimeException("Error: No bucket found for depth='" + i + "' with max_depth='" 
							+ maxDepth + "' and num_buckets='" + numBuckets + "'.");
			}
			int c = 1;
			double tSum = 0;
			double bSum = 0;
			for(Triple<Integer,Integer,List<Integer>> t : buckets) {
				double w;
				if(c == 1)
					w = 1;
				else
					w = ((double)1)/((double)c);
				int tempSum = 0;
				for(int i : t.getThird())
					tempSum += i;
				tSum += ((double)tempSum) * w;
				bSum += ((double)t.getThird().size()) * w;
				c++;
			}
			avgDepth = (int)Math.round(tSum / bSum);
		}
		@Override
		public int compareTo(MethodData o) {
			return Integer.compare(missingEps+avgDepth, o.missingEps+o.avgDepth);
		}
		@Override
		public boolean equals(Object o) {
			if(this == o)
				return true;
			if(o == null || !(o instanceof MethodData))
				return false;
			return Objects.equals(method, ((MethodData)o).method);
		}
		@Override
		public int hashCode() {
			int i = 17;
			i = i * 31 + Objects.hashCode(method);
			return i;
		}
	}
	
	private static final class SubGraphRunner implements Runnable {
		private final EntryPoint ep;
		private final CallGraph cg;
		private volatile Map<SootMethod,Pair<AtomicInteger,List<Integer>>> ret;
		private volatile Map<SootMethod,Map<SootMethod,Set<Unit>>> cpRet;
		private final IACMinerDataAccessor dataAccessor;
		private final IExcludeHandler excludeHandler;
		public SubGraphRunner(EntryPoint ep, CallGraph cg, IACMinerDataAccessor dataAccessor, IExcludeHandler excludeHandler) {
			this.ep = ep;
			this.cg = cg;
			this.excludeHandler = excludeHandler;
			this.dataAccessor = dataAccessor;
			this.ret = null;
			this.cpRet = null;
		}
		@Override
		public void run() {
			this.ret = compute(ep,cg,excludeHandler);
			if(this.ret.isEmpty())
				this.ret = Collections.emptyMap();
			this.cpRet = computeCPCount(ep, cg, dataAccessor, excludeHandler);
			if(this.cpRet.isEmpty())
				this.cpRet = Collections.emptyMap();
		}
		public Map<SootMethod,Pair<AtomicInteger,List<Integer>>> getValue() {
			return ret;
		}
		public Map<SootMethod,Map<SootMethod,Set<Unit>>> getCPValue() {
			return cpRet;
		}
	}
	
	private static Map<SootMethod,Pair<AtomicInteger,List<Integer>>> compute(EntryPoint ep, CallGraph cg, IExcludeHandler excludeHandler){
		Map<SootMethod,Pair<AtomicInteger,List<Integer>>> ret = new HashMap<>();
		HashSet<SootMethod> seen = new HashSet<SootMethod>();
		Queue<SootMethod> tovisit = new ArrayDeque<SootMethod>();
		Queue<Integer> depthCount = new ArrayDeque<Integer>();
		tovisit.add(ep.getEntryPoint());
		depthCount.add(0);
		
		while(!tovisit.isEmpty()) {
			SootMethod cur = tovisit.poll();
			int depth = depthCount.poll();
			//Excluded methods don't matter subgraph wise (and would make things noisy if included)
			if(excludeHandler.isExcludedMethodWithOverride(cur))
				continue;
			
			//Increment the occurrence counter and add the current depth to the depths list
			Pair<AtomicInteger,List<Integer>> data = ret.get(cur);
			if(data == null) {
				data = new Pair<AtomicInteger,List<Integer>>(new AtomicInteger(), new ArrayList<Integer>());
				ret.put(cur, data);
			}
			data.getFirst().incrementAndGet();
			data.getSecond().add(depth);
			
			//Only traverse further if the current not has not been seen already
			if(seen.add(cur)) {
				Iterator<Edge> itEdge = cg.edgesOutOf(cur);
				while(itEdge.hasNext()){
					SootMethod sm = itEdge.next().tgt();
					tovisit.add(sm);
					depthCount.add(depth+1);
				}
			}
		}
		return ret;
	}
	
	public static Map<SootMethod,Map<SootMethod,Set<Unit>>> computeCPCount(EntryPoint ep, CallGraph cg, IACMinerDataAccessor dataAccessor, 
			IExcludeHandler excludeHandler) {
		Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> cps = dataAccessor.getControlPredicatesDB().getData(ep);
		Set<SootMethod> seen = new HashSet<>();
		Deque<SootMethod> stack = new ArrayDeque<>();
		Map<SootMethod,Map<SootMethod,Set<Unit>>> data = new HashMap<>();
		stack.push(ep.getEntryPoint());
		
		while(!stack.isEmpty()) {
			SootMethod cur = stack.peek();
			if(seen.add(cur)) {
				if(!excludeHandler.isExcludedMethodWithOverride(cur)) {
					Iterator<Edge> itEdge = cg.edgesOutOf(cur);
					while(itEdge.hasNext()){
						stack.push(itEdge.next().tgt());
					}
				}
			} else {
				stack.pop();
				Map<SootMethod,Set<Unit>> nodesCP = new HashMap<>();
				if(!excludeHandler.isExcludedMethodWithOverride(cur)) {
					Iterator<Edge> itEdge = cg.edgesOutOf(cur);
					while(itEdge.hasNext()){
						SootMethod sm = itEdge.next().tgt();
						Map<SootMethod,Set<Unit>> childData = data.get(sm);
						if(childData != null && !childData.isEmpty()) {
							for(SootMethod m : childData.keySet()) {
								Set<Unit> temp = nodesCP.get(m);
								if(temp == null) {
									temp = new HashSet<>();
									nodesCP.put(m, temp);
								}
								temp.addAll(childData.get(m));
							}
						}
					}
				}
				Pair<Set<Unit>, Set<Integer>> temp = cps.get(cur);
				if(temp != null) {
					Set<Unit> t = nodesCP.get(cur);
					if(t == null) {
						t = new HashSet<>();
						nodesCP.put(cur, t);
					}
					t.addAll(temp.getFirst());
				}
				Iterator<SootMethod> it = nodesCP.keySet().iterator();
				while(it.hasNext()) {
					SootMethod sm = it.next();
					if(nodesCP.get(sm).isEmpty())
						it.remove();
				}
				if(!nodesCP.isEmpty())
					data.put(cur, nodesCP);
			}
		}
		
		return data;
	}
	
	public static final Map<String,Set<SootMethod>> loadGroups(Path in) throws Exception {
		Map<String,Set<SootMethod>> ret = new HashMap<>();
		String curGroup = null;
		if(!FileHelpers.checkRWFileExists(in)) {
			return null;
		}
		try(BufferedReader br = Files.newBufferedReader(in, Charset.defaultCharset())) {
			for(String line; (line = br.readLine()) != null;) {
				line = line.trim();
				if(line.startsWith("#") || line.isEmpty()) {
					continue;
				} else if(line.startsWith("Group ")) {
					curGroup = line.substring(6).trim();
					Set<SootMethod> temp = ret.get(curGroup);
					if(temp == null) {
						temp = new HashSet<>();
						ret.put(curGroup, temp);
					}
				} else {
					SootMethod sm = Scene.v().grabMethod(line);
					if(sm != null && ret.containsKey(curGroup))
						ret.get(curGroup).add(sm);
				}
			}
		}
		for(String s : ret.keySet()) {
			ret.put(s, SortingMethods.sortSet(ret.get(s),SootSort.smComp));
		}
		ret = SortingMethods.sortMapKey(ret, SortingMethods.sComp);
		if(ret.isEmpty())
			return null;
		return ret;
	}

}
