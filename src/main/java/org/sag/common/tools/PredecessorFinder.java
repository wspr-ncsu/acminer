package org.sag.common.tools;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.toolkits.graph.DirectedGraph;

/* Finds all possible (union) predecessor stmts instead of all required
 * predecessor statements (intersection). 
 */
public class PredecessorFinder<N> {

	protected DirectedGraph<N> graph;
	protected BitSet fullSet;
	protected List<N> heads;
	protected Map<N, BitSet> nodeToFlowSet;
	protected Map<N, Integer> nodeToIndex;
	protected Map<Integer, N> indexToNode;
	protected int lastIndex = 0;

	public PredecessorFinder(DirectedGraph<N> graph) {
		this.graph = graph;
		doAnalysis();
	}

	protected void doAnalysis() {
		heads = graph.getHeads();
		nodeToFlowSet = new HashMap<N, BitSet>();
		nodeToIndex = new HashMap<N, Integer>();
		indexToNode = new HashMap<Integer, N>();

		// build full set all set to false
		fullSet = new BitSet(graph.size());

		// set up domain for union: head nodes are only dominated by themselves,
		// other nodes are dominated by nothing
		for (Iterator<N> i = graph.iterator(); i.hasNext();) {
			N o = i.next();
			if (heads.contains(o)) {
				BitSet self = new BitSet();
				self.set(indexOf(o));
				nodeToFlowSet.put(o, self);
			} else {
				nodeToFlowSet.put(o, fullSet);
			}
		}

		boolean changed = true;
		do {
			changed = false;
			for (Iterator<N> i = graph.iterator(); i.hasNext();) {
				N o = i.next();
				if (heads.contains(o))
					continue;

				// initialize to the "neutral element" for the union
				// this clone() is fast on BitSets (opposed to on HashSets)
				BitSet predsIntersect = (BitSet) fullSet.clone();

				// union over all predecessors
				for (Iterator<N> j = graph.getPredsOf(o).iterator(); j.hasNext();) {
					BitSet predSet = nodeToFlowSet.get(j.next());
					predsIntersect.or(predSet);
				}

				BitSet oldSet = nodeToFlowSet.get(o);
				// each node dominates itself
				predsIntersect.set(indexOf(o));
				if (!predsIntersect.equals(oldSet)) {
					nodeToFlowSet.put(o, predsIntersect);
					changed = true;
				}
			}
		} while (changed);
	}

	protected int indexOf(N o) {
		Integer index = nodeToIndex.get(o);
		if (index == null) {
			index = lastIndex;
			nodeToIndex.put(o, index);
			indexToNode.put(index, o);
			lastIndex++;
		}
		return index;
	}

	public DirectedGraph<N> getGraph() {
		return graph;
	}

	public Set<N> getPreds(N node) {
		// reconstruct list of predecessors from bitset
		Set<N> result = new HashSet<N>();
		BitSet bitSet = nodeToFlowSet.get(node);
		for (int i = 0; i < bitSet.length(); i++) {
			if (bitSet.get(i)) {
				result.add(indexToNode.get(i));
			}
		}
		return result;
	}

	public boolean isPredBy(N node, N predecessor) {
		return getPreds(node).contains(predecessor);
	}

	public boolean isPredByAll(N node, Collection<N> predecessors) {
		return getPreds(node).containsAll(predecessors);
	}

}
