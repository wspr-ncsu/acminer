package org.sag.soot.analysis;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.DominatorNode;
import soot.toolkits.graph.DominatorTree;

/** <p>This class computes the control dependence graph using the Dominance Frontier
 * algorithm provided by Cytron. From Cytron's paper, we know that given a CFG in 
 * which there exists two nodes X and Y,
 * the following statement is true. Y is control dependent on X in the CFG iff X is in
 * the Dominance Frontier set of Y (i.e. DF(Y)) for the reverse of the CFG (i.e. RCFG).
 * This means the set of nodes that Y is control dependent on (i.e. CD(Y)) = DF(Y).
 * Therefore, to compute the control dependencies of all nodes in a CFG, we simply need
 * to compute the Dominance Frontier of all nodes in the RCFG.</p>
 * 
 * <p>This class was modified from Soot's CytronDominanceFrontier class which generically
 * computes the Dominance Frontier of all nodes in any given graph, to compute only 
 * the Control Dependence Graph. Thus, we hard-coded in the use of a post-dominators 
 * finder when computing the DominatorTree. This means the algorithm is using 
 * a RCFG when computing the Dominance Frontier of all nodes in the graph, and thus the
 * relationship CD(Y) = DF(Y) holds.</p>
 * 
 * <p>To retrieve the CD(Y) set from this class, simply call one of the two methods 
 * {@link #getControlDependencies(Object)} or {@link #getControlDependencies(Object, Filter)},
 * where the Object type argument would be node Y. The first method will return the complete
 * CD(Y) set while the second method will return a subset of CD(Y) that includes only the nodes
 * in CD(Y) that are permitted by the given {@link Filter}.</p>
 * 
 * <p>It is important to note that the DF(Y) and thus the CD(Y) only returns the immediate nodes
 * in the CFG that Y is control dependent on. In other words, it returns only the nodes Y is 
 * control dependent on but not the set of nodes that all nodes in CD(Y) are control dependent on.
 * For example, if CD(Y) is the set {A,B}, the nodes A and B may have their own control
 * dependencies which are not included in CD(Y). To get the set CD+(Y) that includes the all the
 * nodes in the CFG that Y is control dependent on, we simply have to traverse the control dependence 
 * graph and include all nodes reachable from Y. To retrieve the CD+(Y) set from this class, simply
 * call one of the two methods {@link #getIteratedControlDependencies(Object)} or 
 * {@link #getIteratedControlDependencies(Object, Filter)} where the Object type argument would be 
 * node Y. The first method will return the complete CD+(Y) set while the second method will return 
 * a subset of CD+(Y) that includes only the nodes in CD+(Y) that are permitted by the given 
 * {@link Filter}.</p>
 * 
 * <p>To get the set of nodes that are control dependent on some node X do the following:
 * <pre>
 * Set ret; //the set of nodes that are control dependent on X
 * for(N n : CFG) {
 *     if(X is in the getControlDependencies(n))
 *         ret.add(n);
 * </pre>
 * </p>
 *
 * @author Sigmund A. Gorski III
 * @see <a
 * href="http://citeseer.nj.nec.com/cytron91efficiently.html">Efficiently
 * Computing Static Single Assignment Form and the Control Dependence
 * Graph</a>
 */
public class ControlDependenceGraph<N> {
	protected final BlankFilter<N> blankFilter;
	protected final DirectedGraph<N> graph;
	protected final FastPostDominatorsFinder<N> dominatorsFinder;
	protected final DominatorTree<N> dt;
	//Mapping Y -> DF(Y) which gives us the nodes Y is immediately control dependent on
	protected final Map<DominatorNode<N>, Set<DominatorNode<N>>> nodeToControlDependencies;
	//Mapping Y -> DF+(Y) which gives us the nodes Y is control dependent on
	protected final Map<DominatorNode<N>, Set<DominatorNode<N>>> nodeToIteratedControlDependencies;
	
	public ControlDependenceGraph(DirectedGraph<N> graph) {
		Objects.requireNonNull(graph);
		this.blankFilter = new BlankFilter<N>();
		this.graph = graph;
		this.dominatorsFinder = new FastPostDominatorsFinder<N>(graph);
		this.dt = new DominatorTree<N>(dominatorsFinder);
		nodeToControlDependencies = new HashMap<>();
		nodeToIteratedControlDependencies = new HashMap<>();
		build();
		
		for(N gode : dt.getGraph()) {
			DominatorNode<N> dode = dt.getDode(gode);
			Set<DominatorNode<N>> controlDep = nodeToControlDependencies.get(dode);
			if(controlDep == null)
				throw new RuntimeException("Error: Failed to find control dependicies for node '" + dode.getGode().toString() + "'.");
			else
				controlDep = ImmutableSet.copyOf(controlDep);
			nodeToControlDependencies.put(dode, controlDep);
			Set<DominatorNode<N>> iteratedControlDep = nodeToIteratedControlDependencies.get(dode);
			if(iteratedControlDep == null)
				throw new RuntimeException("Error: Failed to find iterated control dependicies for node '" + dode.getGode().toString() + "'.");
			else
				iteratedControlDep = ImmutableSet.copyOf(iteratedControlDep);
			nodeToIteratedControlDependencies.put(dode, iteratedControlDep);
		}
	}
	
	/** Returns the CFG used by this class. */
	public DirectedGraph<N> getGraph() {
		return graph;
	}
	
	/** Returns the post-dominators finder used by this class. */
	public FastPostDominatorsFinder<N> getDominatorsFinder() {
		return dominatorsFinder;
	}
	
	/** Returns the dominator tree used by this class. */
	public DominatorTree<N> getDominatorsTree() {
		return dt;
	}
	
	/** Given a node X, this returns the set of all nodes in the CFG that X is control
	 * dependent on (i.e. CD+(X)). This method will return null if the given node X
	 * is not a member of the CFG or if some other error occurs.
	 */
	public Set<N> getIteratedControlDependencies(N node) {
		return getIteratedControlDependencies(node,blankFilter);
	}
	
	/** Given a node X, this returns the set of all nodes in the CFG that X is control
	 * dependent on that are permitted by the given {@link Filter} (i.e. A the subset of
	 * CD+(X)). This method will return null if the given node X is not a member of the 
	 * CFG or if some other error occurs.
	 */
	public Set<N> getIteratedControlDependencies(N node, Filter<N> f) {
		try {
			Set<N> ret = new LinkedHashSet<>();
			for(DominatorNode<N> dnode : nodeToIteratedControlDependencies.get(dt.getDode(node))) {
				N cur = dnode.getGode();
				if(f.includeNode(cur))
					ret.add(cur);
			}
			return ret;
		} catch(Throwable t) {
			return null;
		}
	}
	
	/** Given a node X, this returns the set of nodes that X is control dependent on
	 * (i.e. CD(X)). This method will return null if the given node X is not a member
	 * of the CFG of if some other error occurs.
	 */
	public Set<N> getControlDependencies(N node) {
		return getControlDependencies(node,blankFilter);
	}
	
	/** Given a node X, this returns the set of nodes that X is control dependent on that
	 * are permitted by the given {@link Filter} (i.e. A the subset of CD(X)). This 
	 * method will return if the given node X is not a member of the CFG of if some other
	 * error occurs.
	 */
	public Set<N> getControlDependencies(N node, Filter<N> f) {
		try {
			Set<N> ret = new LinkedHashSet<>();
			for(DominatorNode<N> dnode : nodeToControlDependencies.get(dt.getDode(node))) {
				N cur = dnode.getGode();
				if(f.includeNode(cur))
					ret.add(cur);
			}
			return ret;
		} catch(Throwable t) {
			return null;
		}
	}
	
	/** This is responsible for build the control dependence graph using the 
	 * Dominance frontier algorithm provided by Cytron. It does this in three steps:
	 * <ol>
	 *     <li>It computes the reverse topological ordering of the nodes in the CFG as
	 *         this ordering is required to compute the Dominance Frontier of each node
	 *         in the CFG. This is done in {@link #computeReverseTopologicalOrder()}.</li>
	 *     <li>It computes the Dominance Frontier of each node in the graph according to
	 *         Cytron's algorithm (remember CD(X) = DF(X)). This is done in {@link #buildDominanceFrontier(Collection)}
	 *         where the input is the reverse topological ordering of the nodes in the CFG.</li>
	 *     <li>It computes the iterated dominance frontier of each node in the graph. In other
	 *         words, for all nodes in the CFG it computes their control dependence for the 
	 *         entire CFG. This is done in {@link #buildIteratedDominanceFrontier(Collection)} 
	 *         where the input is the set of nodes in the graph.
	 * </ol>
	 */
	protected void build() {
		Collection<DominatorNode<N>> revTypoSort = computeReverseTopologicalOrder();
		buildDominanceFrontier(revTypoSort);
		buildIteratedDominanceFrontier(revTypoSort);
	}
	
	protected Collection<DominatorNode<N>> computeReverseTopologicalOrder() {
		Set<DominatorNode<N>> visited = new HashSet<>();
		Deque<DominatorNode<N>> revTypoSort = new LinkedList<>();
		Deque<DominatorNode<N>> toVisit = new LinkedList<>();
		for (DominatorNode<N> head : dt.getHeads()) {
			toVisit.push(head);
		}
		while(!toVisit.isEmpty()) {
			DominatorNode<N> cur = toVisit.pop();
			if(!visited.contains(cur)) {
				visited.add(cur);
				revTypoSort.push(cur);
				for(DominatorNode<N> child : dt.getChildrenOf(cur)) {
					if(!visited.contains(child))
						toVisit.push(child);
				}
			}
		}
		return revTypoSort;
	}
	
	/** Computes the iterated dominance frontier for all nodes in the CFG. For our purposes,
	 * we will refer to this as the CD+(X) as this algorithm is not exactly the same as the 
	 * one used to compute the iterated dominance frontier. However, as we only need the 
	 * set of all nodes in the CFG that X is control dependent on, this algorithm is sufficient.
	 * For all nodes in the CFG, this algorithm traverses the control dependence graph, starting 
	 * at the current node and searching outward in a BFS manner. 
	 */
	protected void buildIteratedDominanceFrontier(Collection<DominatorNode<N>> nodes) {
		for(DominatorNode<N> node : nodes) {
			Queue<DominatorNode<N>> toVisit = new ArrayDeque<DominatorNode<N>>();
			Set<DominatorNode<N>> visited = new HashSet<>();
			Set<DominatorNode<N>> iteratedControlDep = new LinkedHashSet<>();
			toVisit.add(node);
			while(!toVisit.isEmpty()) {
				DominatorNode<N> cur = toVisit.poll();
				if(!visited.contains(cur)) {
					visited.add(cur);
					for(DominatorNode<N> y : nodeToControlDependencies.get(cur)) {
						iteratedControlDep.add(y);
						toVisit.add(y);
					}
				}
			}
			nodeToIteratedControlDependencies.put(node, iteratedControlDep);
		}
	}
	
	/** Calculates dominance frontier for all nodes in the graph using algorithm of Cytron et al., TOPLAS Oct. 91:
	 * <pre>
	 * for each X in a bottom-up traversal of the dominator tree do
	 *
	 *	  DF(X) < - null
	 *	  for each Y in Succ(X) do
	 *		if (idom(Y)!=X) then DF(X) <- DF(X) U Y
	 *	  end
	 *	  for each Z in {idom(z) = X} do
	 *		for each Y in DF(Z) do
	 *			  if (idom(Y)!=X) then DF(X) <- DF(X) U Y
	 *		end
	 *	  end
	 * </pre>
	 */
	protected void buildDominanceFrontier(Collection<DominatorNode<N>> revTypoSort) {
		for(DominatorNode<N> node : revTypoSort) {
			Set<DominatorNode<N>> dominanceFrontier = new LinkedHashSet<>();
			//local
			for(DominatorNode<N> succ : dt.getSuccsOf(node)) {
				if(!dt.isImmediateDominatorOf(node, succ))
					dominanceFrontier.add(succ);
			}
			//up
			for(DominatorNode<N> child : dt.getChildrenOf(node)) {
				for(DominatorNode<N> childFront : nodeToControlDependencies.get(child)) {
					if(!dt.isImmediateDominatorOf(node, childFront))
						dominanceFrontier.add(childFront);
				}
			}
			nodeToControlDependencies.put(node, dominanceFrontier);
		}
	}
	
	/** This is a simple class that can be used to filter the results returned by the 
	 * get control dependents methods. It allows one to define the nodes that should be 
	 * included in the sets returned by these methods. Simply put, if the {@link #includeNode(Object)}
	 * method returns true then the node will be included in the control dependents set and if it returns
	 * false then the node will not be included.
	 * 
	 * @author Sigmund A. Gorski III
	 */
	public static interface Filter<N> {
		public boolean includeNode(N node);
	}
	
	private static final class BlankFilter<N> implements Filter<N> {
		@Override
		public boolean includeNode(N node) {
			return true;
		}
	}
}

