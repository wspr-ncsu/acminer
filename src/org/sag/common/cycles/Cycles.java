package org.sag.common.cycles;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.sag.common.tuple.Pair;

/** A helper class for finding all the cycles in a graph.
 * This uses Johnson's algorithm for finding cycles. 
 * 
 * D.B.Johnson, Finding all the elementary circuits of a directed graph, SIAM J. Comput., 4
 * (1975), pp. 77-84.
 * Johnson - O(((V+E)C)
 */
public class Cycles {
	
	private static final class Numerator<T> implements GraphVisitor<T> {
		
		private final Map<Integer, T> indexToNode;
		private final Map<T, Integer> nodeToIndex;
		private volatile int index = 0;

		public Numerator(Map<Integer, T> indexToNode, Map<T, Integer> nodeToIndex) {
			this.indexToNode = indexToNode;
			this.nodeToIndex = nodeToIndex;
		}

		public void startVisit() {
			index = 0;
		}

		public void preVisit(T vertex) {
			if (indexToNode != null) {
				indexToNode.put(index, vertex);
			}
			if (nodeToIndex != null) {
				nodeToIndex.put(vertex, index);
			}
			index++;
		}

		public void postVisit(T vertex) {}

		public boolean isDone() {
			return false;
		}

		public void endVisit() {}

	}
	
	private static final class StronglyConnectedComponentsContext<T> {
		
		final List<Set<T>> stronglyConnectedComponents;
		volatile int index;
		final Map<T, Integer> indexMap;
		final Map<T, Integer> lowlinkMap;
		final Deque<T> stack;
		
		public StronglyConnectedComponentsContext() {
			this.index = 0;
			this.stronglyConnectedComponents = new ArrayList<>();
			this.indexMap = new HashMap<>();
			this.lowlinkMap = new HashMap<>();
			this.stack = new ArrayDeque<>();
		}
		
	}
	
	private static final class CyclesContext<T> {
		
		volatile int numCycles;
		final List<Set<T>> cycles;
		final Map<Integer, T> indexToNode;
		final Map<T, Integer> nodeToIndex;
		final Set<T> blocked;
		final Map<T, Set<T>> blockedSets;
		final Deque<T> stack;

		public CyclesContext(Graph<T> graph) {
			this.numCycles = 0;
			this.cycles =  new ArrayList<>();
			this.indexToNode = new HashMap<>();
			this.nodeToIndex = new HashMap<>();
			this.blocked = new HashSet<>();
			this.blockedSets = new HashMap<>();
			this.stack = new ArrayDeque<T>();
			GraphVisitor<T> numerator = new Numerator<T>(indexToNode, nodeToIndex);
			graph.visitDepthFirst(numerator);
			for (T t : graph.keySet()) {
				blockedSets.put(t, new HashSet<T>());
			}
		}
		
	}
	
	
	
	
	/** Takes in an adjacency list which maps nodes <V> to their adjacent nodes.
	 * Note the values in the map must be either a LinkedHashSet<V> or 
	 * a Pair<V,LinkedHashSet<V>>. Anything else will throw an error. It returns
	 * the start and end nodes of the simple cycles in the graph created from the
	 * map. If any cycle is just a single node then the start and end node are the same.
	 * @param adjLists
	 * @return The simple cycles in the graph.
	 */
	public static <V> List<Pair<V,V>> findSimpleCyclesStartEnd(Map<V, ?> adjLists) {
		Objects.requireNonNull(adjLists);
		List<Set<V>> cycles = findSimpleCycles(prepGraph(adjLists));
		List<Pair<V,V>> ret = new ArrayList<>();
		for(Set<V> cycle : cycles) {
			V start = null;
			V end = null;
			boolean first = true;
			for(V node : cycle) {
				if(first) {
					first = false;
					start = node;
				} else {
					end = node;
				}
			}
			if(start == null)
				throw new RuntimeException("Error: Got empty cycles");
			if(end == null)
				end = start;
			ret.add(new Pair<>(start,end));
		}
		return ret;
	}
	
	/** Takes in an adjacency list which maps nodes <V> to their adjacent nodes.
	 * Note the values in the map must be either a LinkedHashSet<V> or 
	 * a Pair<V,LinkedHashSet<V>>. Anything else will throw an error. It then returns
	 * the simple cycles represented by the graph created from this map.
	 * @param adjLists
	 * @return The simple cycles in the graph.
	 */
	public static <V> List<Set<V>> findSimpleCycles(Map<V, ?> adjLists) {
		Objects.requireNonNull(adjLists);
		return findSimpleCycles(prepGraph(adjLists));
	}
	
	/** Find all simple cycles in a graph by the Johnson's algorithm.
	 * @param <V> The vertex type.
	 * @param graph The graph, each node must have an adjacency list.
	 * @return A list of cycles.
	 */
	public static <V> List<Set<V>> findSimpleCycles(Graph<V> graph) {
		Objects.requireNonNull(graph);
		CyclesContext<V> ctx = new CyclesContext<V>(graph);
		return doFindSimpleCycles(ctx, graph, false);
	}
	
	/** Count the simple cycles in a graph by the Johnson's algorithm.
	 * @param <V> The vertex type.
	 * @param graph The graph, each node must have an adjacency list.
	 * @return The number of simple cycles in the graph.
	 */
	public static <V> int countSimpleCycles(Graph<V> graph) {
		Objects.requireNonNull(graph);
		CyclesContext<V> ctx = new CyclesContext<V>(graph);
		doFindSimpleCycles(ctx, graph, true);
		return ctx.numCycles;
	}
	
	@SuppressWarnings("unchecked")
	private static <V> Graph<V> prepGraph(Map<V, ?> adjLists) {
		Graph<V> g = new Graph<>();
		Set<V> adjNodes = new HashSet<>();
		//Add nodes and adj nodes to the graph
		for(V node : adjLists.keySet()) {
			Object value = adjLists.get(node);
			
			if(node == null || value == null)
				throw new RuntimeException("Error: The Nodes and AdjLists cannot be null.");
			
			LinkedHashSet<V> adjList;
			if(value instanceof Pair) {
				Object temp = ((Pair<?,?>) value).getSecond();
				if(temp != null && temp instanceof LinkedHashSet)
					adjList = (LinkedHashSet<V>)temp;
				else
					throw new RuntimeException("Error: Did not get a properly formatted map");
			} else if(value instanceof LinkedHashSet){
				adjList = (LinkedHashSet<V>)value;
			} else {
				throw new RuntimeException("Error: Did not get a properly formatted map");
			}
			g.put(node, adjList);
			adjNodes.addAll(adjList);
		}
		//Remove all nodes we already have adj lists for and then
		//add the remaining nodes to the graph with empty adj lists
		//This is because the graph must have an entry for each node
		//even if there are no adjacent nodes
		adjNodes.removeAll(g.keySet());
		LinkedHashSet<V> temp = new LinkedHashSet<>();
		for(V node : adjNodes) {
			g.put(node, temp);
		}
		return g;
	}
	
	private static <V> List<Set<V>> doFindSimpleCycles(CyclesContext<V> ctx, Graph<V> graph, boolean countOnly) {
		int startIndex = 0;
		int size = graph.size();
		while (startIndex < size) {
			Pair<Integer, Graph<V>> minSCCResult = minSCGraph(ctx, graph, startIndex, false);
			if (minSCCResult != null) {
				startIndex = minSCCResult.getFirst();
				Graph<V> scg = minSCCResult.getSecond();
				for (V v : scg.get(ctx.indexToNode.get(startIndex))) {
					ctx.blocked.remove(v);
					ctx.blockedSets.get(v).clear();
				}
				findCyclesInSCG(ctx, startIndex, startIndex, scg, countOnly);
				startIndex++;
			} else {
				break;
			}
		}

		return ctx.cycles;
	}
	
	private static <V> Pair<Integer, Graph<V>> minSCGraph(CyclesContext<V> ctxJ, Graph<V> graph, int startIndex, boolean countOnly) {
		StronglyConnectedComponentsContext<V> ctxT = new StronglyConnectedComponentsContext<V>();
		for (V v : graph.keySet()) {
			int vIndex = ctxJ.nodeToIndex.get(v);
			if (vIndex < startIndex) {
				continue;
			}
			if (!ctxT.indexMap.containsKey(v)) {
				doStronglyConnect(ctxJ, ctxT, graph, startIndex, vIndex, false);
			}
		}
		// find the SCC with minimum index
		int minIndexFound = Integer.MAX_VALUE;
		Set<V> minSCC = null;
		for (Set<V> scc : ctxT.stronglyConnectedComponents) {
			for (V v : scc) {
				int t = ctxJ.nodeToIndex.get(v);
				if (t < minIndexFound) {
					minIndexFound = t;
					minSCC = scc;
				}
			}
		}
		if (minSCC == null) {
			return null;
		}
		// build a graph for the SCC found
		Graph<V> resultGraph = new Graph<V>();
		for (V v : minSCC) {
			LinkedHashSet<V> aList = new LinkedHashSet<V>();
			resultGraph.put(v, aList);
			LinkedHashSet<V> origAList = graph.get(v);
			for (V w : minSCC) {
				if (origAList.contains(w)) {
					aList.add(w);
				}
			}
		}
		return new Pair<Integer, Graph<V>>(minIndexFound, resultGraph);
	}
	
	private static <V> void doStronglyConnect(CyclesContext<V> ctxJ, StronglyConnectedComponentsContext<V> ctx, 
			Graph<V> graph, int startIndex, int vertexIndex, boolean countOnly) {
		LinkedHashSet<V> result = null;

		V vertex = ctxJ.indexToNode.get(vertexIndex);
		ctx.indexMap.put(vertex, ctx.index);
		ctx.lowlinkMap.put(vertex, ctx.index);
		ctx.index++;
		ctx.stack.push(vertex);

		Iterator<V> it = graph.get(vertex).iterator();
		while (it.hasNext()) {
			V successor = it.next();
			int successorIndex = ctxJ.nodeToIndex.get(successor);
			if (successorIndex < startIndex) {
				continue;
			}
			if (!ctx.indexMap.containsKey(successor)) {
				doStronglyConnect(ctxJ, ctx, graph, startIndex, successorIndex, countOnly);
				ctx.lowlinkMap.put(vertex, Math.min(ctx.lowlinkMap.get(vertex), ctx.lowlinkMap.get(successor)));
			} else if (ctx.stack.contains(successor)) {
				ctx.lowlinkMap.put(vertex, Math.min(ctx.lowlinkMap.get(vertex), ctx.indexMap.get(successor)));
			}
		}
		if (ctx.lowlinkMap.get(vertex).equals(ctx.indexMap.get(vertex))) {
			if (!countOnly) {
				result = new LinkedHashSet<V>();
				V temp = null;
				do {
					temp = ctx.stack.pop();
					result.add(temp);
				} while (!vertex.equals(temp));
				if (result.size() == 1) {
					V v = result.iterator().next();
					if (graph.get(v).contains(v)) {
						ctx.stronglyConnectedComponents.add(result);
					}
				} else {
					ctx.stronglyConnectedComponents.add(result);
				}
			}
		}
	}
	
	private static <V> boolean findCyclesInSCG(CyclesContext<V> ctx, int startIndex, int vertexIndex, Graph<V> scg, boolean countOnly) {
		boolean foundCycle = false;
		V vertex = ctx.indexToNode.get(vertexIndex);
		ctx.stack.push(vertex);
		ctx.blocked.add(vertex);

		for (V successor : scg.get(vertex)) {
			int successorIndex = ctx.nodeToIndex.get(successor);
			if (successorIndex == startIndex) {
				if (!countOnly) {
					LinkedHashSet<V> cycle = new LinkedHashSet<V>();
					for(Iterator<V> it = ctx.stack.descendingIterator(); it.hasNext();)
						cycle.add(it.next());
					ctx.cycles.add(cycle);
				}
				foundCycle = true;
				ctx.numCycles++;
			} else if (!ctx.blocked.contains(successor)) {
				boolean gotCycle = findCyclesInSCG(ctx, startIndex, successorIndex, scg, countOnly);
				foundCycle = foundCycle || gotCycle;
			}
		}
		if (foundCycle) {
			unblock(ctx, vertex);
		} else {
			for (V w : scg.get(vertex)) {
				Set<V> bList = ctx.blockedSets.get(w);
				if (!bList.contains(vertex)) {
					bList.add(vertex);
				}
			}
		}
		ctx.stack.pop();
		return foundCycle;
	}

	private static <V> void unblock(CyclesContext<V> ctx, V vertex) {
		ctx.blocked.remove(vertex);
		Set<V> bSet = ctx.blockedSets.get(vertex);
		while (bSet.size() > 0) {
			V w = bSet.iterator().next();
			bSet.remove(w);
			if (ctx.blocked.contains(w)) {
				unblock(ctx, w);
			}
		}
	}

}
