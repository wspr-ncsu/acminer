package org.sag.acminer.phases.acminerdebug.task;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.database.excludedelements.IExcludeHandler;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.concurrent.IgnorableRuntimeException;
import org.sag.common.io.FileHelpers;
import org.sag.common.io.PrintStreamUnixEOL;
import org.sag.common.logging.ILogger;
import org.sag.common.tools.SortingMethods;
import org.sag.common.tuple.Pair;
import org.sag.common.tuple.Triple;
import org.sag.soot.SootSort;
import com.google.common.collect.ImmutableList;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class CGSubGraphSizeEntryPointFileTask extends AbstractEntryPointWriteFileTask {

	protected final CallGraph cg;
	
	public CGSubGraphSizeEntryPointFileTask(EntryPoint ep, int id, CallGraph cg, IACMinerDataAccessor dataAccessor, Path rootOutputDir, String cn, 
			ILogger logger) {
		super(ep, id, dataAccessor, rootOutputDir, cn, logger);
		this.cg = cg;
	}
	
	@Override
	public void run() {
		SootMethod entryPoint = ep.getEntryPoint();
		SootClass stub = ep.getStub();
		Path stubOutputDir = getAndCreateStubOutputDir(stub);
		Path outputXML = getOutputFilePath(stubOutputDir,entryPoint,id+"",".xml");
		EntryPointSubgraphDatabase data = generateCallGraphSizes(stub, entryPoint);
		try {
			data.writeXML(null, outputXML);
		} catch (Throwable e) {
			logger.fatal("{}: Failed to open the output file '{}' for writing.",e,cn,outputXML);
		}
		writeToFile(stub,entryPoint,stubOutputDir,data);
	}
	
	/** Traverses the call graph of an entry point ignoring edges out of excluded methods and cycles.
	 * That is it will only ever process a method once. It traverses using a BFS algorithm.
	 * This function keeps track of the path used to reach each method in the call graph under 
	 * the above restrictions. Note a path for some method A includes all the methods used to 
	 * reach A minus method A. For example, the entry point will have an empty path because no 
	 * method was used to reach it. In addition to the path used to reach a method, it also keeps
	 * track of the subgraph of said method. All subgraphs are in BFS order and do not include the
	 * method that is the root of the subgraph (effectively these should all be trees). 
	 */
	protected EntryPointSubgraphDatabase generateCallGraphSizes(SootClass stub, SootMethod entryPoint) {
		try {
			IExcludeHandler excludeHandler = dataAccessor.getExcludedElementsDB().createNewExcludeHandler(new EntryPoint(entryPoint,stub));
			Queue<SootMethod> tovisit = new ArrayDeque<SootMethod>();
			Queue<List<SootMethod>> paths = new ArrayDeque<>();
			LinkedHashMap<SootMethod,List<SootMethod>> visited = new LinkedHashMap<>();
			HashSet<Edge> edges = new HashSet<>();
			tovisit.add(entryPoint);
			paths.add(ImmutableList.of());
			// This is a breadth first search algorithm
			while(!tovisit.isEmpty()) {
				SootMethod currMeth = tovisit.poll();
				List<SootMethod> path = ImmutableList.<SootMethod>builder().addAll(paths.poll()).add(currMeth).build();
				visited.put(currMeth,path);
				Iterator<Edge> itEdge = cg.edgesOutOf(currMeth);
				Set<SootMethod> tgts = new HashSet<>();
				while(itEdge.hasNext()) {
					Edge e = itEdge.next();
					SootMethod tgt = e.tgt();
					// Don't add edges to excluded method
					// If we seen an edge before adding it to the set again won't affect the count
					if(!excludeHandler.isExcludedMethodWithOverride(tgt)) {
						edges.add(e);
						if(!currMeth.equals(tgt) && !visited.containsKey(tgt) && !tovisit.contains(tgt))
							tgts.add(tgt);
					}
				}
				// Make sure the breadth first search ordering is stable between runs
				for(SootMethod sm : SortingMethods.sortSet(tgts,SootSort.smComp)) {
					tovisit.add(sm);
					paths.add(path);
				}
			}
			
			List<MethodEntry> entries = new ArrayList<>();
			for(SootMethod sm : visited.keySet()) {
				List<SootMethod> path = visited.get(sm);
				Set<SootMethod> subgraph;
				int edgesCount;
				if(sm.equals(entryPoint)) {
					subgraph = visited.keySet();
					edgesCount = edges.size();
				} else {
					Pair<Set<SootMethod>,Integer> data = generateSubGraph(entryPoint, sm, excludeHandler);
					subgraph = data.getFirst();
					edgesCount = data.getSecond();
				}
				entries.add(new MethodEntry(sm, path, subgraph, edgesCount));
			}
			return new EntryPointSubgraphDatabase(stub, entryPoint, entries);
		} catch(Throwable t) {
			logger.fatal("{}: Failed to generate call graph subgraph data for stub '{}' and entry point '{}'.",t,cn,stub,entryPoint);
			throw new IgnorableRuntimeException();
		}
	}
	
	/** This traverses some subgraph of an entry point starting at the given startMethod. The traversal is the same
	 * as the one used to generate the graphml call graph for the entry point. This means it does not include any
	 * nodes or edges to excluded methods (not even as leaf nodes). This also does not include cyclic back edges to
	 * the entry point we are exploring as this would just generate a subgraph equal to that of the entry point
	 * (making it impossible to tell how the start node actually affects the graphs size). All other nodes
	 * will be considered but only ever explored once.
	 * 
	 * Returns a set ordered by bfs containing all the nodes in the subgraph starting at the given startMethod
	 * including the start method itself and a count of the number of edges in the subgraph.
	 */
	private Pair<Set<SootMethod>,Integer> generateSubGraph(SootMethod entryPoint, SootMethod startMethod, IExcludeHandler excludeHandler) {
		Queue<SootMethod> tovisit = new ArrayDeque<SootMethod>();
		LinkedHashSet<SootMethod> visited = new LinkedHashSet<>();
		HashSet<Edge> edges = new HashSet<>();
		tovisit.add(startMethod);
		while(!tovisit.isEmpty()) {
			SootMethod currMeth = tovisit.poll();
			visited.add(currMeth);
			Iterator<Edge> itEdge = cg.edgesOutOf(currMeth);
			Set<SootMethod> tgts = new HashSet<>();
			while(itEdge.hasNext()) {
				Edge e = itEdge.next();
				SootMethod tgt = e.tgt();
				if(!excludeHandler.isExcludedMethodWithOverride(tgt) && !entryPoint.equals(tgt)) {
					// Don't add edges to excluded method or edges to entry point
					// If we seen an edge before adding it to the set again won't affect the count
					edges.add(e); 
					if(!currMeth.equals(tgt) && !visited.contains(tgt) && !tovisit.contains(tgt))
						tgts.add(tgt);
				}					
			}
			// Make sure the breadth first search ordering is stable between runs
			for(SootMethod sm : SortingMethods.sortSet(tgts,SootSort.smComp))
				tovisit.add(sm);
		}
		return new Pair<Set<SootMethod>,Integer>(visited,edges.size());
	}
	
	protected void writeToFile(SootClass stub, SootMethod entryPoint, Path stubOutputDir, EntryPointSubgraphDatabase data) {
		Path outputTXTBFS = getOutputFilePath(stubOutputDir,entryPoint,id+"_bfs",".txt");
		Path outputTXTNodeDEC = getOutputFilePath(stubOutputDir,entryPoint,id+"_nodedec",".txt");
		Path outputTXTEdgeDEC = getOutputFilePath(stubOutputDir,entryPoint,id+"_edgedec",".txt");
		Path outputTXTName = getOutputFilePath(stubOutputDir,entryPoint,id+"_name",".txt");
		List<MethodEntry> bfsEntries = data.getMethodEntries();
		List<MethodEntry> decNodeEntries = new ArrayList<>(bfsEntries);
		List<MethodEntry> decEdgeEntries = new ArrayList<>(bfsEntries);
		List<MethodEntry> nameEntries = new ArrayList<>(bfsEntries);
		Collections.sort(decNodeEntries,new MethodEntry.DecNodeComp());
		Collections.sort(decEdgeEntries,new MethodEntry.DecEdgeComp());
		Collections.sort(nameEntries,new MethodEntry.NameComp());
		int depthDigits = 1; // Always start at a depth of 1
		int nodesDigits = 1; // Always should be one node
		int edgesDigits = 0; // There may be no edges
		if(!bfsEntries.isEmpty())
			depthDigits = bfsEntries.get(bfsEntries.size() - 1).getDepth();
		if(!decNodeEntries.isEmpty())
			nodesDigits = decNodeEntries.get(0).getSubgraphNodes();
		if(!decEdgeEntries.isEmpty())
			edgesDigits = decEdgeEntries.get(0).getSubgraphEdges();
		depthDigits = digits(depthDigits);
		nodesDigits = digits(nodesDigits);
		edgesDigits = digits(edgesDigits);
		
		writeFile(stub, entryPoint, outputTXTBFS, bfsEntries, depthDigits, nodesDigits, edgesDigits);
		writeFile(stub, entryPoint, outputTXTNodeDEC, decNodeEntries, depthDigits, nodesDigits, edgesDigits);
		writeFile(stub, entryPoint, outputTXTEdgeDEC, decEdgeEntries, depthDigits, nodesDigits, edgesDigits);
		writeFile(stub, entryPoint, outputTXTName, nameEntries, depthDigits, nodesDigits, edgesDigits);
		
		logger.fineInfo("{}: Succeded in outputing call graph subgraph data for stub '{}' and entry point '{}'.",cn,stub,entryPoint);
	}
	
	private final void writeFile(SootClass stub, SootMethod entryPoint, Path path, List<MethodEntry> entries, 
			int depthDigits, int nodesDigits, int edgesDigits) {
		try(PrintStream ps = new PrintStreamUnixEOL(Files.newOutputStream(path))) {
			ps.println("///// Stub: " + stub.getName() + " EntryPoint: " + entryPoint.getSignature() + " /////\n");
			for(MethodEntry e : entries) {
				if(e.getSubgraphNodes() > 5) { // Remove all leaf nodes since what we really care about are non-leaf nodes
					ps.println("Depth: " + padNum(e.getDepth(), depthDigits) + " SGNodes: " 
						+ padNum(e.getSubgraphNodes(), nodesDigits) + " SGEdges: " + padNum(e.getSubgraphEdges(), edgesDigits) 
						+ " Method: " + e.getMethod());
				}
			}
		} catch(Throwable t) {
			logger.fatal("{}: Failed to output call graph subgraph data for stub '{}' and entry point '{}' to '{}'.",t,cn,stub,entryPoint,path);
			throw new IgnorableRuntimeException();
		}
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
	
	private final static String padString(String s, int spaces) {
		return String.format("%"+spaces+"s", s);
	}
	
	public static void main(String[] args) throws Exception {
		if(args.length < 3)
			throw new RuntimeException("Not enough arguments");
		EntryPointSubgraphDatabase ver1 = EntryPointSubgraphDatabase.readXMLStatic(args[0], null);
		EntryPointSubgraphDatabase ver2 = EntryPointSubgraphDatabase.readXMLStatic(args[1], null);
		Path outputPath = FileHelpers.getPath(args[2]);
		List<MethodEntry> ver1me = new ArrayList<>(ver1.getMethodEntries());
		List<MethodEntry> ver2me = new ArrayList<>(ver2.getMethodEntries());
		Collections.sort(ver1me, new MethodEntry.DecNodeComp());
		Collections.sort(ver2me, new MethodEntry.DecNodeComp());
		HashSet<String> processed = new HashSet<>();
		ArrayList<Triple<String,MethodEntry,MethodEntry>> data = new ArrayList<>();
		for(MethodEntry e2 : ver2me) {
			MethodEntry match = null;
			for(MethodEntry e1 : ver1me) {
				if(e1.getMethod().equals(e2.getMethod())) {
					match = e1;
					break;
				}
			}
			data.add(new Triple<>(e2.getMethod(),match,e2));
			processed.add(e2.getMethod());
		}
		MethodEntry prev = null;
		for(MethodEntry e1 : ver1me) {
			if(!processed.contains(e1.getMethod())) {
				if(prev == null) {
					data.add(0, new Triple<>(e1.getMethod(),e1,null));
				} else {
					int index = -1;
					for(int i = 0; i < data.size(); i++) {
						Triple<String,MethodEntry,MethodEntry> t = data.get(i);
						if(t.getFirst().equals(prev.getMethod())) {
							index = i + 1;
						}
					}
					data.add(index,new Triple<>(e1.getMethod(),e1,null));
				}
			}
			prev = e1;
		}
		writeFile(ver2.getStub(),ver2.getEntryPoint(),outputPath,data);
	}
	
	private static final void writeFile(String stub, String entryPoint, Path path, 
			List<Triple<String,MethodEntry,MethodEntry>> entries) throws Exception {
		int maxNodeE1 = 0;
		int maxEdgeE1 = 0;
		int maxDepthE1 = 0;
		int maxNodeE2 = 0;
		int maxEdgeE2 = 0;
		int maxDepthE2 = 0;
		for(Triple<String,MethodEntry,MethodEntry> t : entries) {
			MethodEntry e1 = t.getSecond();
			MethodEntry e2 = t.getThird();
			if(e1 != null) {
				if(e1.getDepth() > maxDepthE1)
					maxDepthE1 = e1.getDepth();
				if(e1.getSubgraphNodes() > maxNodeE1)
					maxNodeE1 = e1.getSubgraphNodes();
				if(e1.getSubgraphEdges() > maxEdgeE1)
					maxEdgeE1 = e1.getSubgraphEdges();
			}
			if(e2 != null) {
				if(e2.getDepth() > maxDepthE2)
					maxDepthE2 = e2.getDepth();
				if(e2.getSubgraphNodes() > maxNodeE2)
					maxNodeE2 = e2.getSubgraphNodes();
				if(e2.getSubgraphEdges() > maxEdgeE2)
					maxEdgeE2 = e2.getSubgraphEdges();
			}
		}
		maxNodeE1 = digits(maxNodeE1);
		maxEdgeE1 = digits(maxEdgeE1);
		maxDepthE1 = digits(maxDepthE1);
		maxNodeE2 = digits(maxNodeE2);
		maxEdgeE2 = digits(maxEdgeE2);
		maxDepthE2 = digits(maxDepthE2);
		maxNodeE1 = maxNodeE1 < 3 ? 3 : maxNodeE1;
		maxEdgeE1 = maxEdgeE1 < 3 ? 3 : maxEdgeE1;
		maxDepthE1 = maxDepthE1 < 3 ? 3 : maxDepthE1;
		maxNodeE2 = maxNodeE2 < 3 ? 3 : maxNodeE2;
		maxEdgeE2 = maxEdgeE2 < 3 ? 3 : maxEdgeE2;
		maxDepthE2 = maxDepthE2 < 3 ? 3 : maxDepthE2;
		
		try(PrintStream ps = new PrintStreamUnixEOL(Files.newOutputStream(path))) {
			ps.println("///// Stub: " + stub + " EntryPoint: " + entryPoint + " /////\n");
			for(Triple<String,MethodEntry,MethodEntry> t : entries) {
				MethodEntry e1 = t.getSecond();
				MethodEntry e2 = t.getThird();
				StringBuilder sb = new StringBuilder();
				sb.append("N1: ");
				sb.append(e1 == null ? padString("NAN",maxNodeE1) : padNum(e1.getSubgraphNodes(),maxNodeE1));
				sb.append(" N2: ");
				sb.append(e2 == null ? padString("NAN",maxNodeE2) : padNum(e2.getSubgraphNodes(),maxNodeE2));
				sb.append(" E1: ");
				sb.append(e1 == null ? padString("NAN",maxEdgeE1) : padNum(e1.getSubgraphEdges(),maxEdgeE1));
				sb.append(" E2: ");
				sb.append(e2 == null ? padString("NAN",maxEdgeE2) : padNum(e2.getSubgraphEdges(),maxEdgeE2));
				/*sb.append(" Depth V1: ");
				sb.append(e1 == null ? padString("NAN",maxDepthE1) : padNum(e1.getDepth(),maxDepthE1));
				sb.append(" Depth V2: ");
				sb.append(e2 == null ? padString("NAN",maxDepthE2) : padNum(e2.getDepth(),maxDepthE2));*/
				sb.append(" Method: ").append(t.getFirst());
				ps.println(sb.toString());				
			}
		}
	}
	
}
