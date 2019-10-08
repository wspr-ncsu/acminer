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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.database.excludedelements.IExcludeHandler;
import org.sag.acminer.phases.acminerdebug.handler.AbstractOutputHandler;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.concurrent.IgnorableRuntimeException;
import org.sag.common.concurrent.LoggingWorkerGroup;
import org.sag.common.concurrent.WorkerCountingThreadExecutor;
import org.sag.common.graphtools.AlEdge;
import org.sag.common.graphtools.AlElement.Color;
import org.sag.common.graphtools.AlNode;
import org.sag.common.graphtools.Formatter;
import org.sag.common.graphtools.GraphmlGenerator;
import org.sag.common.io.FileHelpers;
import org.sag.common.io.PrintStreamUnixEOL;
import org.sag.common.logging.ILogger;
import org.sag.common.logging.LoggerWrapperSLF4J;
import org.sag.common.tools.SortingMethods;
import org.sag.common.tuple.Pair;
import org.sag.common.tuple.Triple;
import org.sag.soot.SootSort;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import soot.Scene;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class CommonSubgraphOutputHandler extends AbstractOutputHandler {
	
	private final List<CommonGroup> groups;
	private final Set<Set<SootMethod>> graphsToCompute;
	private final WorkerCountingThreadExecutor exe;
	private final GraphmlGenerator outGraphml;
	private final ILogger logger;
	private final String pn;
	
	private final LoadingCache<EntryPoint,Set<Edge>> callGraphEdgesForEp = CacheBuilder.newBuilder()
			.concurrencyLevel(Runtime.getRuntime().availableProcessors())
			.initialCapacity(500)
			.softValues().build(new CacheLoader<EntryPoint,Set<Edge>>(){
		@Override
		public Set<Edge> load(EntryPoint ep) throws Exception {
			return getEdgesForEp(ep);
		}
	});
	
	public CommonSubgraphOutputHandler(String pn, WorkerCountingThreadExecutor exe, GraphmlGenerator outGraphml, 
			Set<Set<SootMethod>> graphsToCompute, Path rootOutputDir, IACMinerDataAccessor dataAccessor, ILogger logger) {
		super(rootOutputDir,dataAccessor);
		Objects.requireNonNull(graphsToCompute);
		Objects.requireNonNull(exe);
		Objects.requireNonNull(pn);
		Objects.requireNonNull(outGraphml);
		this.logger = logger == null ? new LoggerWrapperSLF4J(CommonSubgraphOutputHandler.class) : logger;
		this.graphsToCompute = graphsToCompute;
		this.exe = exe;
		this.pn = pn;
		this.outGraphml = outGraphml;
		this.groups = new ArrayList<>();
		for(Set<SootMethod> eps : this.graphsToCompute) {
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
			groups.add(new CommonGroup(SortingMethods.sortSet(resolvedEps)));
		}
		//Sort groups and set their id based on the order
		Collections.sort(groups);
		int digits = digits(groups.size());
		for(int i = 0; i < groups.size(); i++) {
			groups.get(i).setId(i, digits);;
		}
	}
	
	private final static int digits(int n) {
		int len = String.valueOf(n).length();
		if(n < 0)
			return len - 1;
		else
			return len;
	}
	
	private final Set<Edge> getEdgesForEp(EntryPoint ep) {
		logger.warn("Loading the call graph for " + ep.toString());
		CallGraph cg = Scene.v().getCallGraph();
		IExcludeHandler excludeHandler = dataAccessor.getExcludedElementsDB().createNewExcludeHandler(ep);
		Set<Edge> ret = new HashSet<>();
		Queue<SootMethod> tovisit = new ArrayDeque<SootMethod>();
		HashSet<SootMethod> visited = new HashSet<SootMethod>();
		tovisit.add(ep.getEntryPoint());
		while(!tovisit.isEmpty()){
			SootMethod currMeth = tovisit.poll();
			visited.add(currMeth);
			if(excludeHandler.isExcludedMethodWithOverride(currMeth)){
				continue;
			}
			Iterator<Edge> itEdge = cg.edgesOutOf(currMeth);
			while(itEdge.hasNext()){
				Edge e = itEdge.next();
				SootMethod tgt = e.tgt();
				if(!visited.contains(tgt)){
					tovisit.add(tgt);
				}
				ret.add(e);
			}
		}
		logger.warn("Finished loading the call graph for " + ep.toString());
		return ret;
	}
	
	public LoggingWorkerGroup run() {
		try{
			FileHelpers.processDirectory(rootOutputDir,true,true);
		}catch(Throwable t){
			logger.fatal("{}: Failed to create and verify the database dump directory. Skipping the database dumps.",t,cn);
			return null;
		}
		LoggingWorkerGroup loggingGroup = new CommonLoggingWorkerGroup(groups,rootOutputDir,pn,cn,logger,false,false);
		for(CommonGroup g : groups) {
			try{
				exe.execute(g,loggingGroup);
			}catch(Throwable t){
				logger.fatal("{}: Failed to execute the task for generating common subgraphs of group '{}'.",t,cn,g.getId());
				loggingGroup.addFailedToExecuteException(t);
			}
		}
		
		loggingGroup.unlockInitialLock();
		return loggingGroup;
	}
	
	private static final class CommonLoggingWorkerGroup extends LoggingWorkerGroup {
		private final List<CommonGroup> groups;
		private final Path rootOutputDir;
		public CommonLoggingWorkerGroup(List<CommonGroup> groups, Path rootOutputDir, String phaseName, String name, ILogger logger, 
				boolean shutdownOnError, boolean closeLogger) {
			super(phaseName, name, logger, shutdownOnError, closeLogger);
			this.groups = groups;
			this.rootOutputDir = rootOutputDir;
		}
		@Override
		protected void endGroup() {
			try {
				Path output = FileHelpers.getPath(rootOutputDir, "groups.txt");
				try(PrintStreamUnixEOL out = new PrintStreamUnixEOL(Files.newOutputStream(output))) {
					for(CommonGroup g : groups) {
						out.println("Common Group: " + g.getId());
						out.println("  Entry Points: " + g.getEntryPoints().toString());
						List<Triple<Set<SootMethod>,Set<SootMethod>,Set<Edge>>> subGraphs = g.getCommonSubGraphs();
						out.println("  Sub-Graphs - " + subGraphs.size() + ":");
						int i = 0;
						for(Triple<Set<SootMethod>,Set<SootMethod>,Set<Edge>> p : subGraphs) {
							out.println("    Sub-Graph " + i++ + " Start Nodes:" + p.getFirst().toString());
						}
					}
				}
			} catch(Throwable t) {
				logger.fatal("{}: Failed to output the common groups to entry points map.");
				this.addFailedToExecuteException(t);
			}
			super.endGroup();
		}
	}
	
	private static final class CommonSubGraphFormatter extends Formatter {
		private final Triple<Set<SootMethod>,Set<SootMethod>,Set<Edge>> subGraph;
		private final Set<AlNode> alNodes;
		private final Set<AlEdge> alEdges;
		private volatile long id;
		private final String epsS;
		public CommonSubGraphFormatter(String epsS, Triple<Set<SootMethod>,Set<SootMethod>,Set<Edge>> subGraph, Path outputPath) {
			super(0,-1,-1,-1,outputPath);
			this.subGraph = subGraph;
			this.alNodes = new TreeSet<>();
			this.alEdges = new TreeSet<>();
			this.id = 0;
			this.epsS = epsS;
		}
		public Collection<AlNode> getNodes() { return alNodes; }
		public Collection<AlEdge> getEdges() { return alEdges; }
		public void format() {
			Set<SootMethod> startNodes = subGraph.getFirst();
			Set<SootMethod> nodes = subGraph.getSecond();
			Set<Edge> edges = subGraph.getThird();
			Map<SootMethod,AlNode> nton = new HashMap<>();
			for(SootMethod n : nodes) {
				AlNode alNode = new AlNode(id++,n.getSignature());
				alNodes.add(alNode);
				nton.put(n, alNode);
				if(startNodes.contains(n))
					alNode.setColors(nodeColorIndex, Collections.singletonList(Color.GREEN));
			}
			for(Edge e : edges) {
				alEdges.add(new AlEdge(id++,nton.get(e.src()),nton.get(e.tgt())));
			}
		}
		public String getComment() {
			StringBuilder sb = new StringBuilder();
			sb.append(super.getComment());
			sb.append("  Type: ").append("Common SubGraph\n");
			sb.append("  Entry Points: ").append(epsS).append("\n");
			sb.append("  Start Nodes: ").append(subGraph.getFirst().toString()).append("\n");
			return sb.toString();
		}
	}
	
	private final class CommonGroup implements Runnable,Comparable<CommonGroup> {

		private volatile String id;
		private final Set<EntryPoint> eps;
		//Start Nodes, All Graph Nodes, All Graph Edges
		private final List<Triple<Set<SootMethod>,Set<SootMethod>,Set<Edge>>> commonSubGraphs;
		
		public CommonGroup(Set<EntryPoint> eps) {
			this.eps = eps;
			this.commonSubGraphs = new ArrayList<>();
			this.id = "";
		}
		
		@Override
		public boolean equals(Object o) {
			if(o == this)
				return true;
			if(o == null || !(o instanceof CommonGroup))
				return false;
			return Objects.equals(eps,((CommonGroup)o).eps);
		}
		
		@Override
		public int hashCode() {
			int i = 17;
			i = i * 31 + Objects.hashCode(eps);
			return i;
		}
		
		@Override
		public int compareTo(CommonGroup o) {
			int size = Math.min(eps.size(), o.eps.size());
			Iterator<EntryPoint> fIt = eps.iterator();
			Iterator<EntryPoint> sIt = o.eps.iterator();
			for(int i = 0; i < size; i++) {
				EntryPoint f = fIt.next();
				EntryPoint s = sIt.next();
				int ret = f.compareTo(s);
				if(ret != 0)
					return ret;
			}
			return Integer.compare(eps.size(), o.eps.size());
		}
		
		public void setId(int id, int digits) {
			this.id = "G"+padNum(id,digits);
		}
		
		public String getId() {
			return id;
		}
		
		public Set<EntryPoint> getEntryPoints() {
			return eps;
		}
		
		public List<Triple<Set<SootMethod>,Set<SootMethod>,Set<Edge>>> getCommonSubGraphs() {
			return commonSubGraphs;
		}
		
		private String padNum(int n, int digits) {
			return String.format("%0"+digits+"d", n);
		}
		
		@Override
		public void run() {
			try {
				Map<SootMethod,Set<Edge>> epToCallGraphEdges = new HashMap<>();
				for(EntryPoint ep : eps) {
					epToCallGraphEdges.put(ep.getEntryPoint(), callGraphEdgesForEp.get(ep));
				}
				generateCommonSubgraphs(epToCallGraphEdges);
				epToCallGraphEdges = null;
			} catch(Throwable t) {
				logger.fatal("{}: Failed to generate the list of common subgraphs and their start nodes for the entry points '{}'.",t,cn,
						eps.toString());
				throw new IgnorableRuntimeException();
			}
			if(commonSubGraphs.isEmpty()) {
				logger.info("{}: No common subgraphs found for the entry points '{}'.",cn,eps);
			} else {
				Path groupOutDir = getAndCreateGroupOutputDir();
				int uniqId = 1;
				for(Triple<Set<SootMethod>,Set<SootMethod>,Set<Edge>> p : commonSubGraphs) {
					Set<SootMethod> startNodes = p.getFirst();
					SootMethod m;
					if(startNodes.isEmpty()) {
						logger.info("{}: A subgraph contains no start nodes for the entry points '{}'.",cn,eps);
						m = eps.iterator().next().getEntryPoint();
					} else {
						m = p.getFirst().iterator().next();
					}
					Path output = getOutputFilePath(groupOutDir,m,uniqId++ + "",".graphml");
					Formatter f = getFormatter(eps,p,output);
					runGraphOutputTask(p.getFirst(),eps,f);
				}
			}
		}
		
		private void runGraphOutputTask(Set<SootMethod> startNodes, Set<EntryPoint> eps, Formatter formatter) {
			try {
				outGraphml.outputGraph(formatter);
			} catch(Throwable t) {
				logger.fatal("{}: Failed to create a new task to handle the writing of the subgraph with starting nodes '{}' "
						+ "generated from entry points '{}' for group '{}'.",t,cn,startNodes,eps,getId());
				throw new IgnorableRuntimeException();
			}
			logger.fineInfo("{}: Successfully created a new task to handle the writing of the subgraph with starting nodes '{}' "
					+ "generated from entry points '{}' for group '{}'. If no error occurs when writing the file this call graph was "
					+ "output successfully.",cn,startNodes,eps,getId());
		}
		
		private Formatter getFormatter(Set<EntryPoint> eps, Triple<Set<SootMethod>,Set<SootMethod>,Set<Edge>> subGraph, Path output) {
			try {
				CommonSubGraphFormatter f = new CommonSubGraphFormatter(eps.toString(), subGraph, output);
				f.format();
				return f;
			} catch(Throwable t) {
				logger.fatal("{}: Failed to retrieve the formatter for the subgraph with starting nodes '{}' generated from entry "
						+ "points '{}' for group '{}'.",t,cn,subGraph.getFirst(),eps,getId());
				throw new IgnorableRuntimeException();
			}
		}
		
		//Create output directories for path
		private Path getAndCreateGroupOutputDir() {
			Path groupOutputDir = null;
			try {
				groupOutputDir = FileHelpers.getPath(rootOutputDir,id);
				FileHelpers.processDirectory(groupOutputDir,true,false);
			} catch(Throwable t) {
				logger.fatal("{}: Failed to process the output directory for root output directory '{}' and group '{}'.",
						t,cn,rootOutputDir,id);
				throw new IgnorableRuntimeException();
			}
			return groupOutputDir;
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
						t,cn,stubOutputDir,m);
				throw new IgnorableRuntimeException();
			}
			return output;
		}
		
		/* Determine the start nodes of a sub graph by traversing the call graph of each entry point and recording the first node
		 * at which a edge of the a entry point's call graph leads to a node in the sub graph for each path of all the entry points' 
		 * call graphs. Note this uses a Breadth-first search traversal.
		 */
		private List<SootMethod> getCommonStartNodes(Map<SootMethod,Set<Edge>> epToCallGraphEdges, Set<SootMethod> commonNodes) {
			logger.warn("Finding all start nodes for subgraphs of " + epToCallGraphEdges.keySet().toString());
			Set<Edge> fullCallGraphEdges = new HashSet<>();
			for(Set<Edge> edges : epToCallGraphEdges.values()) {
				fullCallGraphEdges.addAll(edges);
			}
			CallGraph cg = Scene.v().getCallGraph();
			List<SootMethod> commonStartNodes = new ArrayList<>();
			Queue<SootMethod> tovisit = new ArrayDeque<SootMethod>();
			Set<SootMethod> visited = new HashSet<SootMethod>();
			tovisit.addAll(epToCallGraphEdges.keySet());
			while(!tovisit.isEmpty()){
				SootMethod currMeth = tovisit.poll();
				if(visited.add(currMeth)) {
					if(commonNodes.contains(currMeth)) {
						commonStartNodes.add(currMeth);
					} else {
						Iterator<Edge> itEdge = cg.edgesOutOf(currMeth);
						while(itEdge.hasNext()){
							Edge e = itEdge.next();
							SootMethod tgt = e.tgt();
							if(fullCallGraphEdges.contains(e) && !visited.contains(tgt)) {
								tovisit.add(tgt);
							}
						}
					}
				}
			}
			return commonStartNodes;
		}
		
		private List<Pair<Set<SootMethod>,Set<Edge>>> getCommonSubGraphs(Map<SootMethod,Set<Edge>> epToCallGraphEdges, 
				Set<SootMethod> commonNodes, Map<SootMethod,List<Edge>> nodesToOutgoingEdges, Map<SootMethod,List<Edge>> nodesToIncommingEdges) {
			logger.warn("Finding subgraphs for " + epToCallGraphEdges.keySet().toString());
			//Separate the set of common edges into disjoint sets of common edges
			List<Pair<Set<SootMethod>,Set<Edge>>> graphs = new ArrayList<>();
			Set<SootMethod> commonNodesCpy = new HashSet<>(commonNodes);
			while(!commonNodesCpy.isEmpty()) {
				SootMethod cur = commonNodesCpy.iterator().next();
				Set<SootMethod> nodes = new HashSet<>();
				Set<Edge> edges = new HashSet<>();
				graphs.add(new Pair<Set<SootMethod>,Set<Edge>>(nodes,edges));
				
				Queue<SootMethod> tovisit = new ArrayDeque<SootMethod>();
				HashSet<SootMethod> visited = new HashSet<SootMethod>();
				tovisit.add(cur);
				while(!tovisit.isEmpty()) {
					SootMethod curNode = tovisit.poll();
					if(visited.add(curNode)) {
						nodes.add(curNode);
						commonNodesCpy.remove(curNode);
						List<Edge> edgesOut = nodesToOutgoingEdges.get(curNode);
						List<Edge> edgesIn = nodesToIncommingEdges.get(curNode);
						if(edgesOut != null) {
							edges.addAll(edgesOut);
							for(Edge e : edgesOut) {
								tovisit.add(e.tgt());
							}
						}
						if(edgesIn != null) {
							edges.addAll(edgesIn);
							for(Edge e : edgesIn) {
								tovisit.add(e.src());
							}
						}
					}
				}
			}
			logger.warn("Found " + graphs.size() + " subgraphs for " + epToCallGraphEdges.keySet().toString());
			return graphs;
		}
		
		private void addCommonSubGraphs(Map<SootMethod,Set<Edge>> epToCallGraphEdges, List<SootMethod> commonStartNodes, 
				List<Pair<Set<SootMethod>,Set<Edge>>> graphs) {
			logger.warn("Seting start nodes for each subgraph for " + epToCallGraphEdges.keySet().toString());
			for(Pair<Set<SootMethod>,Set<Edge>> g : graphs) {
				Set<SootMethod> nodes = g.getFirst();
				Set<SootMethod> startNodes = new HashSet<>();
				for(SootMethod n : commonStartNodes) {
					if(nodes.contains(n))
						startNodes.add(n);
				}
				commonSubGraphs.add(new Triple<Set<SootMethod>,Set<SootMethod>,Set<Edge>>(
						SortingMethods.sortSet(startNodes,SootSort.smComp),nodes,g.getSecond()));
			}
		
			logger.warn("Sorting subgraphs of " + epToCallGraphEdges.keySet().toString());
			//Sort into a predictable order
			Collections.sort(commonSubGraphs,new Comparator<Triple<Set<SootMethod>,Set<SootMethod>,Set<Edge>>>(){
				@Override
				public int compare(Triple<Set<SootMethod>, Set<SootMethod>, Set<Edge>> o1, 
						Triple<Set<SootMethod> ,Set<SootMethod>, Set<Edge>> o2) {
					int ret = Integer.compare(o1.getSecond().size(),o2.getSecond().size());
					if(ret == 0) {
						if(o1.getFirst().size() > 0 && o2.getFirst().size() > 0) {
							ret = SootSort.smComp.compare(o1.getFirst().iterator().next(),o2.getFirst().iterator().next());
							if(ret == 0) {
								ret = SortingMethods.sComp.compare(o1.getFirst().toString(), o2.getFirst().toString());
							}
						} else {
							ret = Integer.compare(o1.getFirst().size(), o2.getFirst().size());
						}
					}
					return ret;
				}
			});
		}
		
		//Compute call graphs for each entry point before passing them into this function
		//Each entry point will have the exclude override list setup beforehand so no unwanted edges will be included 
		//in these separate call graphs
		private void generateCommonSubgraphs(Map<SootMethod,Set<Edge>> epToCallGraphEdges) {
			List<SootMethod> commonStartNodes = null;
			List<Pair<Set<SootMethod>,Set<Edge>>> graphs = null;
			
			{//Block off to force variables out of scope for gc
				Set<SootMethod> commonNodes = null;
				Map<SootMethod,List<Edge>> nodesToOutgoingEdges = null;
				Map<SootMethod,List<Edge>> nodesToIncommingEdges = null;
				{
					logger.warn("Construting graph for " + epToCallGraphEdges.keySet().toString());
					//Compute the common edges between all the callGraphs by intersecting the set of edges of each call graph
					Set<Edge> commonEdges = new HashSet<>();
					boolean first = true;
					for(Set<Edge> edges : epToCallGraphEdges.values()) {
						if(first) {
							first = false;
							commonEdges.addAll(edges);
						} else {
							commonEdges.retainAll(edges);
						}
					}
					nodesToOutgoingEdges = new HashMap<>();
					nodesToIncommingEdges = new HashMap<>();
					commonNodes = new HashSet<>();
					for(Edge e : commonEdges) {
						List<Edge> edges = nodesToOutgoingEdges.get(e.src());
						if(edges == null) {
							edges = new ArrayList<>();
							nodesToOutgoingEdges.put(e.src(), edges);
						}
						edges.add(e);
						List<Edge> inEdges = nodesToIncommingEdges.get(e.tgt());
						if(inEdges == null) {
							inEdges = new ArrayList<>();
							nodesToIncommingEdges.put(e.tgt(), inEdges);
						}
						inEdges.add(e);
						commonNodes.add(e.src());
						commonNodes.add(e.tgt());
					}
				}
				commonStartNodes = getCommonStartNodes(epToCallGraphEdges, commonNodes);
				graphs = getCommonSubGraphs(epToCallGraphEdges, commonNodes, nodesToOutgoingEdges, nodesToIncommingEdges);
			}
			
			if(!graphs.isEmpty()) {
				addCommonSubGraphs(epToCallGraphEdges, commonStartNodes, graphs);
			}
		}
		
	}
	
	public static final Set<Set<SootMethod>> loadEntryPointSets(Path in) throws Exception {
		Set<Set<SootMethod>> ret = new HashSet<>();
		Set<SootMethod> cur = null;
		if(!FileHelpers.checkRWFileExists(in)) {
			return null;
		}
		try(BufferedReader br = Files.newBufferedReader(in, Charset.defaultCharset())) {
			for(String line; (line = br.readLine()) != null;) {
				line = line.trim();
				if(line.startsWith("#") || line.isEmpty()) {
					continue;
				} else if(line.equalsIgnoreCase("group")) {
					if(cur != null && !cur.isEmpty())
						ret.add(cur);
					cur = new HashSet<>();
				} else {
					SootMethod sm = Scene.v().grabMethod(line);
					if(sm != null)
						cur.add(sm);
				}
			}
			if(cur != null && !cur.isEmpty())
				ret.add(cur);
		}
		if(ret.isEmpty())
			return null;
		return ret;
	}

}
