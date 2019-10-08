package org.sag.common.cycles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;

/**
 * Provides implementation of graph-related algorithms.
 * <p>
 * <b>Performance Notes:</b>
 * <p>
 * &nbsp;&nbsp;&nbsp;&nbsp; Worst case performance of the implemented algorithm for enumeration of
 * the strongly connected components of a graph is O(V+E) where V is the number of vertices in the
 * graph and E is the number of edges in the graph.<br>
 * &nbsp;&nbsp;&nbsp;&nbsp; Worst case time complexity of the algorithms for finding cycles in
 * graphs is:
 * <ol>
 * <li>Tiernan - O(V.const^V)</li>
 * <li>Tarjan - O(VEC)</li>
 * <li>Johnson - O(((V+E)C)</li>
 * <li>Szwarcfiter and Lauer - O(V+EC)</li>
 * </ol>
 * where C is the number of the simple cycles in the graph.
 * <p>
 * &nbsp;&nbsp;&nbsp;&nbsp; Worst case performance is achieved only for graphs with special
 * structure, so on practical workloads an algorithm with higher worst case complexity may
 * outperform an algorithm with lower worst case complexity. Note also that "administrative costs"
 * of algorithms with better worst case performance are higher. Also higher is their memory cost
 * (which is in all cases O(V+E)).
 * <p>
 * &nbsp;&nbsp;&nbsp;&nbsp; My own workloads have typically several thousand nodes and up to ten
 * thousand simple cycles. On these workloads the algorithms score by performance (best to worst) so
 * :
 * <ol>
 * <li>Szwarcfiter and Lauer</li>
 * <li>Tarjan</li>
 * <li>Johnson</li>
 * <li>Tiernan - often by an order of magnitude slower than the rest</li>
 * </ol>
 * <p>
 * <b>Literature:</b><br>
 * <ol>
 * <li>J.C.Tiernan An Efficient Search Algorithm Find the Elementary Circuits of a Graph.,
 * Communications of the ACM, V13, 12, (1970), pp. 722 - 726.</li>
 * <li>R.Tarjan, Depth-first search and linear graph algorithms., SIAM J. Comput. 1 (1972), pp.
 * 146-160.</li>
 * <li>R. Tarjan, Enumeration of the elementary circuits of a directed graph, SIAM J. Comput., 2
 * (1973), pp. 211-216.</li>
 * <li>D.B.Johnson, Finding all the elementary circuits of a directed graph, SIAM J. Comput., 4
 * (1975), pp. 77-84.</li>
 * <li>J.L.Szwarcfiter and P.E.Lauer, Finding the elementary cycles of a directed graph in O(n + m)
 * per cycle, no. 60, Univ. of Newcastle upon Tyne, Newcastle upon Tyne, England, May 1974.
 * <li>P.Mateti and N.Deo, On algorithms for enumerating all circuits of a graph., SIAM J. Comput.,
 * 5 (1978), pp. 90-99.</li>
 * <li>L.G.Bezem and J.van Leeuwen, Enumeration in graphs., Technical report RUU-CS-87-7, University
 * of Utrecht, The Netherlands, 1987.</li></li>
 * </ol>
 */
public final class OtherCycles {
	
	private static final int EC1 = 1;
	private static final int EC2 = 2;
	private static final int EC3 = 3;
	private static final int EC4 = 4;
	private static final int EC5 = 5;
	private static final int EC6 = 6;

	private static final class Numerator<T> implements GraphVisitor<T> {
		private Map<Integer, T> iToV = new HashMap<Integer, T>();
		private Map<T, Integer> vToI = new HashMap<T, Integer>();
		private int index = 0;

		public Numerator(Map<Integer, T> iToV, Map<T, Integer> vToI) {
			this.iToV = iToV;
			this.vToI = vToI;
		}

		public void startVisit() {
			index = 0;
		}

		public void preVisit(T vertex) {
			if (iToV != null) {
				iToV.put(index, vertex);
			}
			if (vToI != null) {
				vToI.put(vertex, index);
			}
			index++;
		}

		public void postVisit(T vertex) {
		}

		public boolean isDone() {
			return false;
		}

		public void endVisit() {
		}

	}

	private static final class InDegreeCalculator<T> implements GraphVisitor<T> {
		private Graph<T> graph;
		private Map<T, Integer> inDegrees;

		public InDegreeCalculator(Graph<T> graph) {
			this.graph = graph;
		}

		public Map<T, Integer> getInDegrees() {
			return inDegrees;
		}

		public void startVisit() {
			inDegrees = new HashMap<T, Integer>();
			for (T t : graph.keySet()) {
				inDegrees.put(t, 0);
			}
		}

		public void preVisit(T vertex) {
			for (T t : graph.get(vertex)) {
				inDegrees.put(t, inDegrees.get(t) + 1);
			}
		}

		public void postVisit(T vertex) {
		}

		public boolean isDone() {
			return false;
		}

		public void endVisit() {
		}
	}

	// Data context for the Tarjan's strongly connected components algorithm.
	private static final class SCCsCtxT<T> {
		int numSCCs = 0;
		List<LinkedHashSet<T>> SCCs = new LinkedList<LinkedHashSet<T>>();
		int index = 0;
		Map<T, Integer> indexMap = new HashMap<T, Integer>();
		Map<T, Integer> lowlinkMap = new HashMap<T, Integer>();
		Stack<T> stack = new Stack<T>();
		Set<T> stackSet = new HashSet<T>();
	}

	// Data context for the Tiernan's simple cycles algorithm.
	private static final class CyclesCtxN<T> {
		int numCycles = 0;
		List<LinkedHashSet<T>> cycles = new LinkedList<LinkedHashSet<T>>();
		List<T> path = new ArrayList<T>();
		Set<T> pathSet = new HashSet<T>();
		Map<T, Set<T>> blocked = new HashMap<T, Set<T>>();
	}

	// Data context for the Tarjahn's simple cycles algorithm.
	private static final class CyclesCtxT<T> {
		int numCycles = 0;
		List<LinkedHashSet<T>> cycles = new LinkedList<LinkedHashSet<T>>();
		Set<T> marked = new HashSet<T>();
		Stack<T> markedStack = new Stack<T>();
		Stack<T> pointStack = new Stack<T>();
	}

	// Data context for the Schwarcfiter and Lauer's simple cycles algorithm.
	private static final class CyclesCtxSL<T> {
		int numCycles = 0;
		List<LinkedHashSet<T>> cycles = new LinkedList<LinkedHashSet<T>>();
		Map<Integer, T> iToV = new HashMap<Integer, T>();
		Map<T, Integer> vToI = new HashMap<T, Integer>();
		Map<T, Set<T>> bSets = new HashMap<T, Set<T>>();
		Stack<T> stack = new Stack<T>();
		Set<T> marked = new HashSet<T>();
		HashMap<T, LinkedHashSet<T>> removed = null;
		int[] position = null;
		boolean[] reach = null;
		List<T> startVertices = new ArrayList<T>();

		CyclesCtxSL(Graph<T> graph) {
			// prepare the S&L context
			removed = new HashMap<T, LinkedHashSet<T>>();
			for (T t : graph.keySet()) {
				removed.put(t, new LinkedHashSet<T>());
			}
			int size = graph.size();
			position = new int[size];
			reach = new boolean[size];
			GraphVisitor<T> numerator = new Numerator<T>(iToV, vToI);
			graph.visitDepthFirst(numerator);
			for (T t : graph.keySet()) {
				bSets.put(t, new HashSet<T>());
			}

			InDegreeCalculator<T> inDegreeCalculator = new InDegreeCalculator<T>(graph);
			graph.visitDepthFirst(inDegreeCalculator);
			Map<T, Integer> inDegrees = inDegreeCalculator.getInDegrees();
			List<LinkedHashSet<T>> sccs = findSCCs(graph);
			for (LinkedHashSet<T> scc : sccs) {
				int maxIngrade = -1;
				T startVertex = null;
				for (T t : scc) {
					int inDegree = inDegrees.get(t);
					if (inDegree > maxIngrade) {
						maxIngrade = inDegree;
						startVertex = t;
					}
				}
				startVertices.add(startVertex);
			}
		}
	}

	/**
	 * Find all non-trivial strongly connected components of a graph.
	 * 
	 * @param <V>
	 *            The vertex type.
	 * @param graph
	 *            The graph.
	 * @return A list of strongly connected components. May be empty but not null.
	 */
	public static <V> List<LinkedHashSet<V>> findSCCs(Graph<V> graph) {
		Objects.requireNonNull(graph);
		SCCsCtxT<V> ctx = new SCCsCtxT<V>();
		for (V vertex : graph.keySet()) {
			if (!ctx.indexMap.containsKey(vertex)) {
				doStronglyConnect(ctx, graph, vertex, false, false);
			}
		}
		return ctx.SCCs;
	}

	/**
	 * Count the number of non-trivial strongly connected components in a graph.
	 * 
	 * @param <V>
	 *            The vertex type.
	 * @param graph
	 *            The graph.
	 * @return The number of strongly connected components in the graph.
	 */
	public static <V> int countSCCs(Graph<V> graph) {
		Objects.requireNonNull(graph);
		SCCsCtxT<V> ctx = new SCCsCtxT<V>();
		for (V vertex : graph.keySet()) {
			if (!ctx.indexMap.containsKey(vertex)) {
				doStronglyConnect(ctx, graph, vertex, true, false);
			}
		}
		return ctx.numSCCs;
	}

	/**
	 * Find all strongly connected components of a graph. This version of the method includes in the
	 * result trivial SCCs consisting of a single node which participates in no cycles.
	 * 
	 * @param <V>
	 *            The vertex type.
	 * @param graph
	 *            The graph.
	 * @return A list of strongly connected components. May be empty but not null.
	 */
	public static <V> List<LinkedHashSet<V>> findAllSCCs(Graph<V> graph) {
		Objects.requireNonNull(graph);
		SCCsCtxT<V> ctx = new SCCsCtxT<V>();
		for (V vertex : graph.keySet()) {
			if (!ctx.indexMap.containsKey(vertex)) {
				doStronglyConnect(ctx, graph, vertex, false, true);
			}
		}
		return ctx.SCCs;
	}

	/**
	 * Count the number of non-trivial strongly connected components in a graph. This version of the
	 * method counts trivial SCCs consisting of a single node which participates in no cycles.
	 * 
	 * @param <V>
	 *            The vertex type.
	 * @param graph
	 *            The graph.
	 * @return The number of strongly connected components in the graph.
	 */
	public static <V> int countAllSCCs(Graph<V> graph) {
		Objects.requireNonNull(graph);
		SCCsCtxT<V> ctx = new SCCsCtxT<V>();
		for (V vertex : graph.keySet()) {
			if (!ctx.indexMap.containsKey(vertex)) {
				doStronglyConnect(ctx, graph, vertex, true, true);
			}
		}
		return ctx.numSCCs;
	}

	private static <V> void doStronglyConnect(SCCsCtxT<V> ctx, Graph<V> graph, V vertex, boolean countOnly, boolean returnTrivial) {
		ctx.indexMap.put(vertex, ctx.index);
		ctx.lowlinkMap.put(vertex, ctx.index);
		ctx.index++;
		ctx.stack.push(vertex);
		ctx.stackSet.add(vertex);

		LinkedHashSet<V> successors = graph.get(vertex);
		for (V successor : successors) {
			if (!ctx.indexMap.containsKey(successor)) {
				doStronglyConnect(ctx, graph, successor, countOnly, returnTrivial);
				ctx.lowlinkMap.put(vertex, Math.min(ctx.lowlinkMap.get(vertex), ctx.lowlinkMap.get(successor)));
			} else if (ctx.stackSet.contains(successor)) {
				ctx.lowlinkMap.put(vertex, Math.min(ctx.lowlinkMap.get(vertex), ctx.indexMap.get(successor)));
			}
		}
		if (ctx.lowlinkMap.get(vertex).equals(ctx.indexMap.get(vertex))) {
			LinkedHashSet<V> result = new LinkedHashSet<V>();
			V temp = null;
			do {
				temp = ctx.stack.pop();
				ctx.stackSet.remove(temp);
				result.add(temp);
			} while (!vertex.equals(temp));

			boolean isTrivial = false;
			if (result.size() == 1) {
				V v = result.iterator().next();
				if (!graph.get(v).contains(v)) {
					isTrivial = true;
				}
			}

			if (returnTrivial || !isTrivial) {
				ctx.numSCCs++;
				if (!countOnly) {
					ctx.SCCs.add(result);
				}
			}
		}
	}

	/**
	 * Find all simple cycles in a graph by the Ternian's algorithm.
	 * <p>
	 * <b>Implementation Note:</b><br>
	 * The vertex type must implement Comparable&lt;V&gt;.<br>
	 * 
	 * @param <V>
	 *            The vertex type.
	 * @param graph
	 *            The graph.
	 * @return A list of cycles.
	 */
	public static <V> List<LinkedHashSet<V>> findSimpleCyclesN(Graph<V> graph) {
		CyclesCtxN<V> ctx = new CyclesCtxN<V>();
		for (V vertex : graph.keySet()) {
			ctx.blocked.put(vertex, new HashSet<V>());
		}
		return doFindSimpleCyclesN(ctx, graph, false);
	}

	/**
	 * Count all simple cycles in a graph by the Ternian's algorithm.
	 * <p>
	 * <b>Implementation Note:</b><br>
	 * The vertex type must implement Comparable&lt;V&gt;.<br>
	 * 
	 * @param <V>
	 *            The vertex type.
	 * @param graph
	 *            The graph.
	 * @return A list of cycles.
	 */
	public static <V> int countSimpleCyclesN(Graph<V> graph) {
		CyclesCtxN<V> ctx = new CyclesCtxN<V>();
		for (V vertex : graph.keySet()) {
			ctx.blocked.put(vertex, new HashSet<V>());
		}
		doFindSimpleCyclesN(ctx, graph, true);
		return ctx.numCycles;
	}

	private static <V> List<LinkedHashSet<V>> doFindSimpleCyclesN(CyclesCtxN<V> ctx, Graph<V> graph, boolean countOnly) {
		Objects.requireNonNull(graph);
		Iterator<V> iter = graph.keySet().iterator();
		if (!iter.hasNext()) {
			return ctx.cycles;
		}

		V startOfPath = null;
		V endOfPath = null;
		V temp = null;
		int endIndex = 0;
		int state = EC1;

		loop: while (true) {
			switch (state) {
			case EC1: // initialize
				state = EC2;
				endOfPath = iter.next();
				ctx.path.add(endOfPath);
				ctx.pathSet.add(endOfPath);
				break;
			case EC2: // path extension
				state = EC3;
				for (V n : graph.get(endOfPath)) {
					@SuppressWarnings("unchecked")
					int cmp = ((Comparable<V>) n).compareTo(ctx.path.get(0));
					if ((cmp > 0) && !ctx.pathSet.contains(n) && !ctx.blocked.get(endOfPath).contains(n)) {
						ctx.path.add(n);
						ctx.pathSet.add(n);
						endOfPath = n;
						state = EC2;
						break;
					}
				}
				break;
			case EC3: // path confirmation
				state = EC4;
				startOfPath = ctx.path.get(0);
				if (graph.get(endOfPath).contains(startOfPath)) {
					if (!countOnly) {
						LinkedHashSet<V> cycle = new LinkedHashSet<V>();
						cycle.addAll(ctx.path);
						ctx.cycles.add(cycle);
					}
					++ctx.numCycles;
				}
				break;
			case EC4: // vertex closure
				state = EC5;
				if (ctx.path.size() > 1) {
					ctx.blocked.get(endOfPath).clear();
					endIndex = ctx.path.size() - 1;
					ctx.path.remove(endIndex);
					ctx.pathSet.remove(endOfPath);
					--endIndex;
					temp = endOfPath;
					endOfPath = ctx.path.get(endIndex);
					ctx.blocked.get(endOfPath).add(temp);
					state = EC2;
				}
				break;
			case EC5:
				state = EC6;
				if (iter.hasNext()) {
					state = EC2;
					ctx.path.clear();
					ctx.pathSet.clear();
					endOfPath = iter.next();
					ctx.path.add(endOfPath);
					ctx.pathSet.add(endOfPath);
					for (V vt : ctx.blocked.keySet()) {
						ctx.blocked.get(vt).clear();
					}
				}
				break;
			case EC6:
				break loop;
			}
		}

		return ctx.cycles;
	}

	/**
	 * Find all simple cycles in a graph by the Tarjan's algorithm.
	 * <p>
	 * <b>Implementation Note:</b><br>
	 * The vertex type must implement Comparable&lt;V&gt;.<br>
	 * 
	 * @param <V>
	 *            The vertex type.
	 * @param graph
	 *            The graph.
	 * @return A list of cycles.
	 */
	public static <V> List<LinkedHashSet<V>> findSimpleCyclesT(Graph<V> graph) {
		CyclesCtxT<V> ctx = new CyclesCtxT<V>();
		return doFindSimpleCyclesT(ctx, graph, false);
	}

	/**
	 * Count the simple cycles in a graph by the Tarjan's algorithm.
	 * <p>
	 * <b>Implementation Note:</b><br>
	 * The vertex type must implement Comparable&lt;V&gt;.<br>
	 * 
	 * @param <V>
	 *            The vertex type.
	 * @param graph
	 *            The graph.
	 * @return The number of simple cycles in the graph.
	 */
	public static <V> int countSimpleCyclesT(Graph<V> graph) {
		Objects.requireNonNull(graph);
		CyclesCtxT<V> ctx = new CyclesCtxT<V>();
		doFindSimpleCyclesT(ctx, graph, true);
		return ctx.numCycles;
	}

	private static <V> List<LinkedHashSet<V>> doFindSimpleCyclesT(CyclesCtxT<V> ctx, Graph<V> graph, boolean countOnly) {
		Objects.requireNonNull(graph);
		for (V start : (Set<V>) graph.keySet()) {
			backtrackT(ctx, graph, start, start, countOnly);
			while (!ctx.markedStack.isEmpty()) {
				ctx.marked.remove(ctx.markedStack.pop());
			}
		}
		return ctx.cycles;
	}

	private static <V> boolean backtrackT(CyclesCtxT<V> ctx, Graph<V> graph, V start, V vertex, boolean countOnly) {
		boolean foundCycle = false;
		ctx.pointStack.push(vertex);
		ctx.marked.add(vertex);
		ctx.markedStack.push(vertex);

		for (V currentVertex : graph.get(vertex)) {
			@SuppressWarnings("unchecked")
			int comparison = ((Comparable<V>) currentVertex).compareTo(start);
			if (comparison < 0) {
			} else if (comparison == 0) {
				foundCycle = true;
				if (!countOnly) {
					LinkedHashSet<V> cycle = new LinkedHashSet<V>();
					int cycleStart = ctx.pointStack.indexOf(start);
					int cycleEnd = ctx.pointStack.size() - 1; // pointStack.indexOf(vertexIndex);
					for (int i = cycleStart; i <= cycleEnd; i++) {
						cycle.add(ctx.pointStack.get(i));
					}
					ctx.cycles.add(cycle);
				}
				ctx.numCycles++;
			} else if (!ctx.marked.contains(currentVertex)) {
				boolean gotCycle = backtrackT(ctx, graph, start, (V) currentVertex, countOnly);
				foundCycle = foundCycle || gotCycle;
			}
		}
		if (foundCycle) {
			while (!ctx.markedStack.peek().equals(vertex)) {
				ctx.marked.remove(ctx.markedStack.pop());
			}
			ctx.marked.remove(ctx.markedStack.pop()); // vertexIndex
		}
		ctx.pointStack.pop(); // vertexIndex
		return foundCycle;
	}

	

	

	

	

	

	

	/**
	 * Find all simple cycles in a graph by the Szwarcfiter and Lauer's algorithm.
	 * 
	 * @param <V>
	 *            The vertex type.
	 * @param graph
	 *            The graph.
	 * @return A list of cycles.
	 */
	public static <V> List<LinkedHashSet<V>> findSimpleCyclesSL(Graph<V> graph) {
		Objects.requireNonNull(graph);
		CyclesCtxSL<V> ctx = new CyclesCtxSL<V>(graph);
		return doFindSimpleCyclesSL(ctx, graph, false);
	}

	/**
	 * Count the simple cycles in a graph by the Szwarcfiter and Lauer's algorithm.
	 * 
	 * @param <V>
	 *            The vertex type.
	 * @param graph
	 *            The graph.
	 * @return The number of simple cycles in the graph.
	 */
	public static <V> int countSimpleCyclesSL(Graph<V> graph) {
		Objects.requireNonNull(graph);
		CyclesCtxSL<V> ctx = new CyclesCtxSL<V>(graph);
		doFindSimpleCyclesSL(ctx, graph, true);
		return ctx.numCycles;
	}

	private static <V> List<LinkedHashSet<V>> doFindSimpleCyclesSL(CyclesCtxSL<V> ctx, Graph<V> graph, boolean countOnly) {
		for (V vertex : ctx.startVertices) {
			cycleSL(ctx, graph, ctx.vToI.get(vertex), 0, countOnly);
		}
		return ctx.cycles;
	}

	private static <V> boolean cycleSL(CyclesCtxSL<V> ctx, Graph<V> graph, int v, int q, boolean CountOnly) {
		boolean foundCycle = false;
		V vV = ctx.iToV.get(v);
		ctx.marked.add(vV);
		ctx.stack.push(vV);
		int t = ctx.stack.size();
		ctx.position[v] = t;
		if (!ctx.reach[v]) {
			q = t;
		}
		LinkedHashSet<V> avRemoved = ctx.removed.get(vV);
		LinkedHashSet<V> avList = graph.get(vV);
		Iterator<V> avIt = avList.iterator();
		while (avIt.hasNext()) {
			V wV = avIt.next();
			if (avRemoved.contains(wV)) {
				continue;
			}
			int w = ctx.vToI.get(wV);
			if (!ctx.marked.contains(wV)) {
				boolean gotCycle = cycleSL(ctx, graph, w, q, CountOnly);
				if (gotCycle) {
					foundCycle = gotCycle;
				} else {
					noCycleSL(ctx, graph, v, w);
				}
			} else if (ctx.position[w] <= q) {
				foundCycle = true;
				ctx.numCycles++;
				if (!CountOnly) {
					int vIndex = ctx.stack.indexOf(vV);
					int wIndex = ctx.stack.indexOf(wV);
					LinkedHashSet<V> cycle = new LinkedHashSet<V>();
					for (int i = wIndex; i <= vIndex; i++) {
						cycle.add(ctx.stack.elementAt(i));
					}
					ctx.cycles.add(cycle);
				}
			} else {
				noCycleSL(ctx, graph, v, w);
			}
		}
		ctx.stack.pop();
		if (foundCycle) {
			unmarkSL(ctx, graph, v);
		}
		ctx.reach[v] = true;
		ctx.position[v] = graph.size();
		return foundCycle;
	}

	private static <V> void noCycleSL(CyclesCtxSL<V> ctx, Graph<V> graph, int x, int y) {
		V xV = ctx.iToV.get(x);
		V yV = ctx.iToV.get(y);

		Set<V> by = ctx.bSets.get(yV);
		LinkedHashSet<V> axRemoved = ctx.removed.get(xV);

		by.add(xV);
		axRemoved.add(yV);
	}

	private static <V> void unmarkSL(CyclesCtxSL<V> ctx, Graph<V> graph, int x) {
		V xV = ctx.iToV.get(x);
		ctx.marked.remove(xV);
		Set<V> bx = ctx.bSets.get(xV);
		for (V yV : bx) {
			LinkedHashSet<V> ySucc = graph.get(yV);
			Set<V> ayRemoved = ctx.removed.get(yV);
			ayRemoved.remove(xV);
			if (!ySucc.contains(xV)) {
				System.err.println("ALARM!!!!");
			}
			if (ctx.marked.contains(yV)) {
				unmarkSL(ctx, graph, ctx.vToI.get(yV));
			}
		}
		bx.clear();
	}

}
