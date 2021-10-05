package org.sag.acminer.phases.acminer;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.database.acminer.Doublet;
import org.sag.acminer.database.acminer.IACMinerDatabase;
import org.sag.acminer.database.defusegraph.StartNode;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.graphtools.AlEdge;
import org.sag.common.graphtools.AlElement;
import org.sag.common.graphtools.AlNode;
import org.sag.common.graphtools.Formatter;
import org.sag.common.graphtools.GraphmlGenerator;
import org.sag.common.graphtools.AlElement.Color;
import org.sag.common.io.FileHelpers;
import org.sag.common.io.PrintStreamUnixEOL;
import org.sag.common.tools.SortingMethods;
import org.sag.common.tuple.Pair;
import org.sag.soot.SootSort;
import org.sag.soot.callgraph.JimpleICFG;
import org.sag.soot.callgraph.ExcludingJimpleICFG.ExcludingEdgePredicate;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class ACMinerDataWriter {
	
	public static void writeData(IACMinerDatabase database, Path rootOutDir) throws Exception {
		Map<String, Map<String, Set<Doublet>>> stubToEpToValuePair = database.getStringValuePairs();
		Map<String, Map<String, Set<String>>> stubToEpToMethods = database.getStringMethods();
		Map<String, Map<String, Set<String>>> stubToEpToFields = database.getStringFields();
		
		for(String stub : stubToEpToValuePair.keySet()) {
			Map<String, Set<Doublet>> epToValuePair = stubToEpToValuePair.get(stub);
			Map<String, Set<String>> epToMethods = stubToEpToMethods.get(stub);
			Map<String, Set<String>> epToFields = stubToEpToFields.get(stub);
			List<Path> outPaths = getOutputPaths(stub,rootOutDir);
			
			Map<String, Set<String>> stringEpToPairs = new LinkedHashMap<>();
			for(String ep : epToValuePair.keySet()) {
				Set<String> temp = new LinkedHashSet<>();
				for(Doublet vp : epToValuePair.get(ep)) {
					temp.add(vp.toString());
				}
				stringEpToPairs.put(ep, temp);
			}
			writeCSV(stringEpToPairs, outPaths.get(0));
			
			Map<String, Map<String, Map<String, Set<String>>>> epToSourceMethodToSourceUnitToPairs = new LinkedHashMap<>();
			for(String ep : epToValuePair.keySet()) {
				Map<String, Map<String, Set<String>>> sourceMethodToSourceUnitToPairs = new LinkedHashMap<>();
				for(Doublet vp : epToValuePair.get(ep)) {
					Map<String, Map<String, String>> sources = vp.getSourcesAsStrings();
					for(String source : sources.keySet()) {
						Map<String, Set<String>> sourceUnitToPairs = sourceMethodToSourceUnitToPairs.get(source);
						if(sourceUnitToPairs == null) {
							sourceUnitToPairs = new LinkedHashMap<>();
							sourceMethodToSourceUnitToPairs.put(source, sourceUnitToPairs);
						}
						for(String stmt : sources.get(source).keySet()) {
							Set<String> pairs = sourceUnitToPairs.get(stmt);
							if(pairs == null) {
								pairs = new LinkedHashSet<>();
								sourceUnitToPairs.put(stmt, pairs);
							}
							pairs.add(vp.toString());
						}
					}
				}
				for(String sourceMethod : sourceMethodToSourceUnitToPairs.keySet()) {
					sourceMethodToSourceUnitToPairs.put(sourceMethod, SortingMethods.sortMapKey(sourceMethodToSourceUnitToPairs.get(sourceMethod),SortingMethods.sComp));
				}
				epToSourceMethodToSourceUnitToPairs.put(ep.toString(), SortingMethods.sortMapKey(sourceMethodToSourceUnitToPairs,SootSort.smStringComp));
			}
			writeEpUnitPairs(epToSourceMethodToSourceUnitToPairs,outPaths.get(1));
			
			writeEpPairs(stringEpToPairs,outPaths.get(2));
			
			Map<String, Map<String, Set<Doublet>>> sourceMethodToSourceUnitToPairs = new LinkedHashMap<>();
			for(String ep : epToValuePair.keySet()) {
				for(Doublet vp : epToValuePair.get(ep)) {
					Map<String, Map<String, String>> sources = vp.getSourcesAsStrings();
					for(String source : sources.keySet()) {
						Map<String, Set<Doublet>> sourceUnitToPairs = sourceMethodToSourceUnitToPairs.get(source);
						if(sourceUnitToPairs == null) {
							sourceUnitToPairs = new LinkedHashMap<>();
							sourceMethodToSourceUnitToPairs.put(source, sourceUnitToPairs);
						}
						for(String stmt : sources.get(source).keySet()) {
							Set<Doublet> pairs = sourceUnitToPairs.get(stmt);
							if(pairs == null) {
								pairs = new LinkedHashSet<>();
								sourceUnitToPairs.put(stmt, pairs);
							}
							pairs.add(vp);
						}
					}
				}
			}
			Map<String, Map<String, Set<String>>> sourceMethodToSourceUnitToPairsSorted = new HashMap<>();
			for(String sourceMethod : sourceMethodToSourceUnitToPairs.keySet()) {
				Map<String, Set<Doublet>> map = sourceMethodToSourceUnitToPairs.get(sourceMethod);
				Map<String, Set<String>> temp = new HashMap<>();
				for(String sourceUnit : map.keySet()) {
					Set<Doublet> sorted = SortingMethods.sortSet(map.get(sourceUnit));
					Set<String> stringSorted = new LinkedHashSet<>();
					for(Doublet vp : sorted)
						stringSorted.add(vp.toString());
					temp.put(sourceUnit, stringSorted);
				}
				temp = SortingMethods.sortMapKey(temp, SortingMethods.sComp);
				sourceMethodToSourceUnitToPairsSorted.put(sourceMethod, temp);
			}
			sourceMethodToSourceUnitToPairsSorted = SortingMethods.sortMapKey(sourceMethodToSourceUnitToPairsSorted,SootSort.smStringComp);
			writeUnitPairs(sourceMethodToSourceUnitToPairsSorted, outPaths.get(3));
			
			Set<Doublet> pairs = new LinkedHashSet<>();
			for(String ep : epToValuePair.keySet()) {
				for(Doublet vp : epToValuePair.get(ep)) {
					pairs.add(vp);
				}
			}
			Set<Doublet> sortedPairs = SortingMethods.sortSet(pairs);
			Set<String> stringSortedPairs = new LinkedHashSet<>();
			for(Doublet vp : sortedPairs) {
				stringSortedPairs.add(vp.toString());
			}
			writePairs(stringSortedPairs, outPaths.get(4));
			
			Set<String> methods = new HashSet<>();
			for(Set<String> m : epToMethods.values())
				methods.addAll(m);
			methods = SortingMethods.sortSet(methods,SootSort.smStringComp);
			writeMethods(methods,outPaths.get(6));
			
			Set<String> fields = new HashSet<>();
			for(Set<String> f : epToFields.values())
				fields.addAll(f);
			fields = SortingMethods.sortSet(fields,SootSort.sfStringComp);
			writeFields(fields,outPaths.get(7));
			
			writeEpMethods(epToMethods, outPaths.get(8));
			
			writeEpFields(epToFields, outPaths.get(9));
		}
	}
	
	public static void writeData(SootClass stub, IACMinerDatabase database, Map<StartNode, Set<String>> graphData, Path rootOutDir, 
			JimpleICFG baseICFG, IACMinerDataAccessor dataAccessor) throws Exception {
		Map<SootMethod, ValuePairLinkedHashSet> epToPairs = database.getValuePairsForStub(stub);
		Map<SootMethod, Set<String>> epToMethods = database.getMethodsForStub(stub);
		Map<SootMethod, Set<String>> epToFields = database.getFieldsForStub(stub);
		List<Path> outPaths = getOutputPaths(stub.toString(),rootOutDir);
		
		Map<String, Set<String>> stringEpToPairs = new LinkedHashMap<>();
		for(SootMethod ep : epToPairs.keySet()) {
			Set<String> temp = new LinkedHashSet<>();
			for(ValuePair vp : epToPairs.get(ep)) {
				temp.add(vp.toString());
			}
			stringEpToPairs.put(ep.toString(), temp);
		}
		writeCSV(stringEpToPairs, outPaths.get(0));
		
		Map<String, Map<String, Map<String, Set<String>>>> epToSourceMethodToSourceUnitToPairs = new LinkedHashMap<>();
		for(SootMethod ep : epToPairs.keySet()) {
			Map<String, Map<String, Set<String>>> sourceMethodToSourceUnitToPairs = new LinkedHashMap<>();
			for(ValuePair vp : epToPairs.get(ep)) {
				Map<SootMethod, Map<String, Unit>> sources = vp.getSources();
				for(SootMethod source : sources.keySet()) {
					Map<String, Set<String>> sourceUnitToPairs = sourceMethodToSourceUnitToPairs.get(source.toString());
					if(sourceUnitToPairs == null) {
						sourceUnitToPairs = new LinkedHashMap<>();
						sourceMethodToSourceUnitToPairs.put(source.toString(), sourceUnitToPairs);
					}
					for(String stmt : sources.get(source).keySet()) {
						Set<String> pairs = sourceUnitToPairs.get(stmt);
						if(pairs == null) {
							pairs = new LinkedHashSet<>();
							sourceUnitToPairs.put(stmt, pairs);
						}
						pairs.add(vp.toString());
					}
				}
			}
			for(String sourceMethod : sourceMethodToSourceUnitToPairs.keySet()) {
				sourceMethodToSourceUnitToPairs.put(sourceMethod, SortingMethods.sortMapKey(sourceMethodToSourceUnitToPairs.get(sourceMethod),SortingMethods.sComp));
			}
			epToSourceMethodToSourceUnitToPairs.put(ep.toString(), SortingMethods.sortMapKey(sourceMethodToSourceUnitToPairs,SootSort.smStringComp));
		}
		writeEpUnitPairs(epToSourceMethodToSourceUnitToPairs,outPaths.get(1));
		
		writeEpPairs(stringEpToPairs,outPaths.get(2));
		
		Map<String, Map<String, ValuePairLinkedHashSet>> sourceMethodToSourceUnitToPairs = new LinkedHashMap<>();
		for(SootMethod ep : epToPairs.keySet()) {
			for(ValuePair vp : epToPairs.get(ep)) {
				Map<SootMethod, Map<String, Unit>> sources = vp.getSources();
				for(SootMethod source : sources.keySet()) {
					Map<String, ValuePairLinkedHashSet> sourceUnitToPairs = sourceMethodToSourceUnitToPairs.get(source.toString());
					if(sourceUnitToPairs == null) {
						sourceUnitToPairs = new LinkedHashMap<>();
						sourceMethodToSourceUnitToPairs.put(source.toString(), sourceUnitToPairs);
					}
					for(String stmt : sources.get(source).keySet()) {
						ValuePairLinkedHashSet pairs = sourceUnitToPairs.get(stmt);
						if(pairs == null) {
							pairs = new ValuePairLinkedHashSet();
							sourceUnitToPairs.put(stmt, pairs);
						}
						pairs.add(vp);
					}
				}
			}
		}
		Map<String, Map<String, Set<String>>> sourceMethodToSourceUnitToPairsSorted = new HashMap<>();
		for(String sourceMethod : sourceMethodToSourceUnitToPairs.keySet()) {
			Map<String, ValuePairLinkedHashSet> map = sourceMethodToSourceUnitToPairs.get(sourceMethod);
			Map<String, Set<String>> temp = new HashMap<>();
			for(String sourceUnit : map.keySet()) {
				Set<ValuePair> sorted = SortingMethods.sortSet(map.get(sourceUnit));
				Set<String> stringSorted = new LinkedHashSet<>();
				for(ValuePair vp : sorted)
					stringSorted.add(vp.toString());
				temp.put(sourceUnit, stringSorted);
			}
			temp = SortingMethods.sortMapKey(temp, SortingMethods.sComp);
			sourceMethodToSourceUnitToPairsSorted.put(sourceMethod, temp);
		}
		sourceMethodToSourceUnitToPairsSorted = SortingMethods.sortMapKey(sourceMethodToSourceUnitToPairsSorted,SootSort.smStringComp);
		writeUnitPairs(sourceMethodToSourceUnitToPairsSorted, outPaths.get(3));
		
		ValuePairLinkedHashSet pairs = new ValuePairLinkedHashSet();
		for(SootMethod ep : epToPairs.keySet()) {
			for(ValuePair vp : epToPairs.get(ep)) {
				pairs.add(vp);
			}
		}
		Set<ValuePair> sortedPairs = SortingMethods.sortSet(pairs);
		Set<String> stringSortedPairs = new LinkedHashSet<>();
		for(ValuePair vp : sortedPairs) {
			stringSortedPairs.add(vp.toString());
		}
		writePairs(stringSortedPairs, outPaths.get(4));
		
		writeGraph(graphData, outPaths.get(5));
		
		Set<String> methods = new HashSet<>();
		for(Set<String> m : epToMethods.values())
			methods.addAll(m);
		methods = SortingMethods.sortSet(methods,SootSort.smStringComp);
		writeMethods(methods,outPaths.get(6));
		
		Set<String> fields = new HashSet<>();
		for(Set<String> f : epToFields.values())
			fields.addAll(f);
		fields = SortingMethods.sortSet(fields,SootSort.sfStringComp);
		writeFields(fields,outPaths.get(7));
		
		Map<String, Set<String>> stringEpToMethods = new LinkedHashMap<>();
		for(SootMethod ep : epToMethods.keySet()) {
			stringEpToMethods.put(ep.toString(), epToMethods.get(ep));
		}
		writeEpMethods(stringEpToMethods, outPaths.get(8));
		
		Map<String, Set<String>> stringEpToFields = new LinkedHashMap<>();
		for(SootMethod ep : epToFields.keySet()) {
			stringEpToFields.put(ep.toString(), epToFields.get(ep));
		}
		writeEpFields(stringEpToFields, outPaths.get(9));
		
		writeLogicGraph(stub, epToPairs, baseICFG, dataAccessor);
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
	
	private static void writeLogicGraph(SootClass stub, Map<SootMethod, ValuePairLinkedHashSet> epToPairs, JimpleICFG baseICFG, 
			IACMinerDataAccessor dataAccessor) {
		Path out = getGraphOutputPath(stub.toString(), dataAccessor.getConfig().getFilePath("debug_acminer-graph-dir"));
		CallGraph callGraph = baseICFG.getCallGraph();
		Map<SootMethod,Set<SootMethod>> epToSources = new LinkedHashMap<>();
		for(SootMethod ep : epToPairs.keySet()) {
			Set<SootMethod> sourceMethods = new LinkedHashSet<>();
			for(ValuePair vp : epToPairs.get(ep)) {
				sourceMethods.addAll(vp.getSources().keySet());
			}
			epToSources.put(ep, SortingMethods.sortSet(sourceMethods,SootSort.smComp));
		}
		epToSources = SortingMethods.sortMapKey(epToSources, SootSort.smComp);
		
		long nodeid = 0;
		long edgeid = 0;
		int epcount = 0;
		Map<SootMethod, Integer> epToIndex = new HashMap<>();
		Map<SootMethod, Pair<AlNode, BitSet>> nodes = new HashMap<>();
		Map<SootMethod, Map<SootMethod, AlEdge>> edges = new HashMap<>();
		//create nodes for the entry points so we can get a stable and correct count
		int digits = digits(epToSources.keySet().size());
		for(SootMethod ep : epToSources.keySet()) {
			epToIndex.put(ep, epcount);
			nodes.put(ep, new Pair<AlNode, BitSet>(new AlNode(nodeid++, padNum(epcount++,digits) + " : " + ep.toString()), new BitSet()));
		}
		for(SootMethod ep : epToSources.keySet()) {
			//Start at the all context queries and source method for an entry point and traverse upwards in the call graph
			//using the restricted view of the call graph that the ExcludingEdgePredicate provides. This way the entry
			//point will be at the top of all traversals and all excluded nodes will not appear. Record all reachable nodes
			//and edges to get a complete view of all the paths that lead to authorization logic.
			Set<SootMethod> sources = epToSources.get(ep);
			Set<SootMethod> contextQueries = dataAccessor.getContextQueriesDB().getContextQueries(new EntryPoint(ep,stub));
			ExcludingEdgePredicate pred = new ExcludingEdgePredicate(callGraph, dataAccessor.getExcludedElementsDB().createNewExcludeHandler(new EntryPoint(ep,stub)));
			Set<SootMethod> sinks = new HashSet<>();
			sinks.addAll(sources);
			sinks.addAll(contextQueries);
			Set<Edge> reachableEdges = new HashSet<>();
			Set<SootMethod> reachableNodes = new HashSet<>();
			reachableNodes.addAll(sinks);
			reachableNodes.add(ep);
			ArrayDeque<SootMethod> queue = new ArrayDeque<>(sinks);
			Set<SootMethod> seen = new HashSet<>();
			while(!queue.isEmpty()) {
				SootMethod cur = queue.pop();
				if(!cur.equals(ep) && seen.add(cur)) {
					Iterator<Edge> it = callGraph.edgesInto(cur);
					while(it.hasNext()) {
						Edge e = it.next();
						if(pred.want(e)) {
							reachableEdges.add(e);
							reachableNodes.add(e.src());
							reachableNodes.add(e.tgt());
							queue.push(e.src());
						}
					}
				}
			}
			//Given the reachable nodes create a node for each first as they are needed in the
			//creation of the edges. Make sure to mark the colors possible based on the situation
			//but leave color selection to later (after all entry points have had the chance to
			//set these fields) as we have 3 possibilities and 2 color options.
			for(SootMethod rn : reachableNodes) {
				Pair<AlNode, BitSet> p = nodes.get(rn);
				if(p == null) {
					p = new Pair<AlNode, BitSet>(new AlNode(nodeid++, rn.toString()), new BitSet());
					nodes.put(rn, p);
				}
				BitSet bs = p.getSecond();
				if(rn.equals(ep))
					bs.set(0);
				if(sources.contains(rn))
					bs.set(1);
				if(contextQueries.contains(rn))
					bs.set(2);
			}
			//Create the edges making special care to ensure we do not have any duplicate edges
			//(i.e. edges with the same source and target).
			for(Edge e : reachableEdges) {
				SootMethod srcM = e.src();
				SootMethod tgtM = e.tgt();
				Map<SootMethod, AlEdge> tgtToEdge = edges.get(srcM);
				if(tgtToEdge == null) {
					tgtToEdge = new HashMap<>();
					edges.put(srcM, tgtToEdge);
				}
				AlEdge edge = tgtToEdge.get(tgtM);
				if(edge == null) {
					edge = new AlEdge(edgeid++, nodes.get(srcM).getFirst(), nodes.get(tgtM).getFirst());
					tgtToEdge.put(tgtM, edge);
				} else {
					edge.incWeight();
				}
			}
		}
		
		//Organize the mined authorization logic by source method with the indexes of each
		//entry point referencing the value pair in that source attached to it and make sure
		//everything is sorted as this will be in the output
		Map<SootMethod, Map<ValuePair, Set<Integer>>> sourceToPairToEpIndex = new HashMap<>();
		for(SootMethod ep : epToPairs.keySet()) {
			Integer index = epToIndex.get(ep);
			for(ValuePair vp : epToPairs.get(ep)) {
				Map<SootMethod, Map<String, Unit>> sources = vp.getSources();
				for(SootMethod source : sources.keySet()) {
					Map<ValuePair, Set<Integer>> pairToEpIndex = sourceToPairToEpIndex.get(source);
					if(pairToEpIndex == null) {
						pairToEpIndex = new HashMap<>();
						sourceToPairToEpIndex.put(source, pairToEpIndex);
					}
					Set<Integer> epIndex = pairToEpIndex.get(vp);
					if(epIndex == null) {
						epIndex = new HashSet<>();
						pairToEpIndex.put(vp, epIndex);
					}
					epIndex.add(index);
				}
			}
		}
		for(SootMethod source : sourceToPairToEpIndex.keySet()) {
			Map<ValuePair, Set<Integer>> pairToEpIndex = sourceToPairToEpIndex.get(source);
			for(ValuePair vp : pairToEpIndex.keySet()) {
				pairToEpIndex.put(vp, SortingMethods.sortSet(pairToEpIndex.get(vp)));
			}
			sourceToPairToEpIndex.put(source, SortingMethods.sortMapKeyAscending(pairToEpIndex));
		}
		SortingMethods.sortMapKey(sourceToPairToEpIndex, SootSort.smComp);
		
		//Set the extra data for the nodes using the value pairs of each source method
		//note each value pair also includes an index referencing what entry points the
		//value pair originates from
		for(SootMethod source : sourceToPairToEpIndex.keySet()) {
			StringBuilder sb = new StringBuilder();
			boolean fs = true;
			Map<ValuePair, Set<Integer>> pairToEpIndex = sourceToPairToEpIndex.get(source);
			for(ValuePair vp : pairToEpIndex.keySet()) {
				if(fs)
					fs = false;
				else
					sb.append("\n");
				sb.append(vp.toString()).append("\n    ");
				boolean first = true;
				for(Integer i : pairToEpIndex.get(vp)) {
					if(first)
						first = false;
					else
						sb.append(", ");
					sb.append(padNum(i,digits));
				}
				
			}
			nodes.get(source).getFirst().setExtraData(0,sb.toString());
		}
		
		List<AlEdge> finalEdges = new ArrayList<>();
		List<AlNode> finalNodes = new ArrayList<>();
		//Set the colors for the nodes
		for(Pair<AlNode, BitSet> p : nodes.values()) {
			BitSet b = p.getSecond();
			List<Color> colors = new ArrayList<>();
			if(b.get(0)) 
				colors.add(AlElement.Color.GREEN);
			if(b.get(1))
				colors.add(AlElement.Color.BLUE);
			if(b.get(2) && !(b.get(0) && b.get(1)))
				colors.add(AlElement.Color.GRAY);
			if(colors.isEmpty())
				colors.add(AlElement.Color.NOCOLOR);
			p.getFirst().setColors(0, colors);
			finalNodes.add(p.getFirst());
		}
		for(Map<SootMethod, AlEdge> temp : edges.values()) {
			for(AlEdge e : temp.values()) {
				finalEdges.add(e);
			}
		}
		
		Formatter f = new Formatter(0,0,0,0,out) {
			@Override
			public Collection<AlNode> getNodes() {
				return finalNodes;
			}
			@Override
			public Collection<AlEdge> getEdges() {
				return finalEdges;
			}
			@Override
			public void format() {}
			@Override
			public String getComment() {
				StringBuilder sb = new StringBuilder();
				sb.append("\nGraph Description:\n");
				sb.append("  Type: Call Graph Exclusively Containing All Captured Authorization Logic And The Paths Leading To Them For A Stub\n");
				sb.append("  Stub: ").append(stub.toString()).append("\n");
				sb.append("  Entry Points:\n");
				for(SootMethod ep : epToPairs.keySet()) {
					sb.append("    ").append(ep).append("\n");
				}
				return sb.toString();
			}
		};
		GraphmlGenerator.outputGraphStatic(f);
	}
	
	private static Path getGraphOutputPath(String stub, Path rootOutDir) {
		String[] parts = stub.toString().split("\\.");
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for(int i = 0; i < parts.length; i++) {
			if(first)
				first = false;
			else
				sb.append(".");
			sb.append(FileHelpers.replaceWhitespace(FileHelpers.cleanFileName(parts[i])));
		}
		sb.append(".graphml");
		return FileHelpers.getPath(rootOutDir, sb.toString());
	}
	
	private static List<Path> getOutputPaths(String stub, Path rootOutDir) throws Exception {
		String[] parts = stub.toString().split("\\.");
		String[] n = new String[parts.length-1];
		String stubName = null;
		for(int i = 0; i < parts.length; i++){
			if(i == parts.length-1) 
				stubName = FileHelpers.replaceWhitespace(FileHelpers.cleanFileName(parts[i]));
			else
				n[i] = FileHelpers.replaceWhitespace(FileHelpers.cleanFileName(parts[i]));
		}
		Path outDir;
		if(n.length > 0) {
			outDir = FileHelpers.getPath(rootOutDir,n);
			FileHelpers.processDirectory(outDir,true,false);
		} else {
			outDir = rootOutDir;
		}
		List<Path> ret = new ArrayList<>();
		ret.add(FileHelpers.getPath(outDir, stubName + ".csv"));//outCSVFile
		ret.add(FileHelpers.getPath(outDir, stubName + "_ep_unit_pairs.txt"));//outEpNodePairs
		ret.add(FileHelpers.getPath(outDir, stubName + "_ep_pairs.txt"));//outEpPairs
		ret.add(FileHelpers.getPath(outDir, stubName + "_unit_pairs.txt"));//outNodePairs
		ret.add(FileHelpers.getPath(outDir, stubName + "_pairs.txt"));//outPairs
		ret.add(FileHelpers.getPath(outDir, stubName + "_graph_dump.txt"));//outGraph
		ret.add(FileHelpers.getPath(outDir, stubName + "_methods.txt"));//outMethods
		ret.add(FileHelpers.getPath(outDir, stubName + "_fields.txt"));//outFields
		ret.add(FileHelpers.getPath(outDir, stubName + "_ep_methods.txt"));
		ret.add(FileHelpers.getPath(outDir, stubName + "_ep_fields.txt"));
		return ret;
	}
	
	private static void writeCSV(Map<String, Set<String>> epToPairs, Path path) throws Exception {
		try(PrintStream ps = new PrintStreamUnixEOL(Files.newOutputStream(path))) {
			for(String ep : epToPairs.keySet()) {
				ps.print(ValuePair.quoteString(ep));
				for(String vp : epToPairs.get(ep)) {
					ps.print("; "+vp);
					ps.flush();
				}
				ps.println();
			}
		}
	}
	
	private static void writeEpUnitPairs(Map<String, Map<String, Map<String, Set<String>>>> epToSourceMethodToSourceUnitToPairs, Path path) throws Exception {
		try(PrintStream ps = new PrintStreamUnixEOL(Files.newOutputStream(path))) {
			for(String ep : epToSourceMethodToSourceUnitToPairs.keySet()) {
				ps.println(ep.toString());
				Map<String, Map<String, Set<String>>> sourceMethodToSourceUnitToPairs = epToSourceMethodToSourceUnitToPairs.get(ep);
				for(String sourceMethod : sourceMethodToSourceUnitToPairs.keySet()) {
					Map<String, Set<String>> sourceUnitToPairs = sourceMethodToSourceUnitToPairs.get(sourceMethod);
					for(String sourceUnit : sourceUnitToPairs.keySet()) {
						ps.println("  Stmt: " + sourceUnit + " Source: " + sourceMethod);
						for(String pair : sourceUnitToPairs.get(sourceUnit)) {
							ps.println("    " + pair);
						}
					}
				}
			}
		}
	}
	
	private static void writeEpPairs(Map<String, Set<String>> epToPairs, Path path) throws Exception {
		try(PrintStream ps = new PrintStreamUnixEOL(Files.newOutputStream(path))) {
			for(String ep : epToPairs.keySet()) {
				ps.println(ep);
				for(String pair : epToPairs.get(ep)) {
					ps.println("  " + pair);
				}
			}
		}
	}
	
	private static void writeUnitPairs(Map<String, Map<String, Set<String>>> sourceMethodToSourceUnitToPairs, Path path) throws Exception {
		try(PrintStream ps = new PrintStreamUnixEOL(Files.newOutputStream(path))) {
			for(String sourceMethod : sourceMethodToSourceUnitToPairs.keySet()) {
				Map<String, Set<String>> sourceUnitToPairs = sourceMethodToSourceUnitToPairs.get(sourceMethod);
				for(String sourceUnit : sourceUnitToPairs.keySet()) {
					ps.println("Stmt: " + sourceUnit + " Source: " + sourceMethod);
					for(String pair : sourceUnitToPairs.get(sourceUnit)) {
						ps.println("  " + pair);
					}
				}
			}
		}
	}
	
	private static void writePairs(Set<String> pairs, Path path) throws Exception {
		try(PrintStream ps = new PrintStreamUnixEOL(Files.newOutputStream(path))) {
			for(String s : pairs) {
				ps.println(s);
			}
		}
	}
	
	private static void writeGraph(Map<StartNode, Set<String>> graphData, Path path) throws Exception {
		Map<StartNode, List<String>> sorted = new HashMap<>();
		for(StartNode sn : graphData.keySet()) {
			List<String> s = new ArrayList<>(graphData.get(sn));
			Collections.sort(s);
			sorted.put(sn,s);
		}
		sorted = SortingMethods.sortMapKeyAscending(sorted);
		try(PrintStream ps = new PrintStreamUnixEOL(Files.newOutputStream(path))) {
			for(StartNode sn : sorted.keySet()) {
				ps.println("Stmt: " + sn.toString() + " Source: " + sn.getSource().toString());
				for(String s : sorted.get(sn)) {
					ps.println("  Def: " + s);
				}
			}
		}
	}
	
	private static void writeMethods(Set<String> methods, Path path) throws Exception {
		try(PrintStream ps = new PrintStreamUnixEOL(Files.newOutputStream(path))) {
			for(String s : methods) {
				ps.println(s);
			}
		}
	}
	
	private static void writeFields(Set<String> fields, Path path) throws Exception {
		try(PrintStream ps = new PrintStreamUnixEOL(Files.newOutputStream(path))) {
			for(String s : fields) {
				ps.println(s);
			}
		}
	}
	
	private static void writeEpMethods(Map<String, Set<String>> epToMethods, Path path) throws Exception {
		try(PrintStream ps = new PrintStreamUnixEOL(Files.newOutputStream(path))) {
			for(String ep : epToMethods.keySet()) {
				ps.println(ep);
				for(String method : epToMethods.get(ep)) {
					ps.println("  " + method);
				}
			}
		}
	}
	
	private static void writeEpFields(Map<String, Set<String>> epToFields, Path path) throws Exception {
		try(PrintStream ps = new PrintStreamUnixEOL(Files.newOutputStream(path))) {
			for(String ep : epToFields.keySet()) {
				ps.println(ep);
				for(String field : epToFields.get(ep)) {
					ps.println("  " + field);
				}
			}
		}
	}

}
