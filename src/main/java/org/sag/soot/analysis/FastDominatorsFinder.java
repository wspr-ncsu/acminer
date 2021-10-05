package org.sag.soot.analysis;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.MHGDominatorsFinder;

public class FastDominatorsFinder<N> extends MHGDominatorsFinder<N> {

	public FastDominatorsFinder(DirectedGraph<N> graph) {
		super(graph);
	}
	
	@Override
	public List<N> getDominators(N node) {
		return new ArrayList<>(getDominatorsSet(node));
	}
	
	public Set<N> getDominatorsSet(N node) {
		if(!nodeToIndex.containsKey(node))
			throw new IllegalArgumentException("Error: Node '" + node.toString() + "' does not exist in the graph.");
		LinkedHashSet<N> ret = new LinkedHashSet<>();
		BitSet bitSet = nodeToFlowSet.get(node);
        for(int i=0;i<bitSet.length();i++) {
            if(bitSet.get(i)) {
                ret.add(indexToNode.get(i));
            }
        }
        return ret;
	}

	@Override
	public N getImmediateDominator(N node) {
		// root node
		if(getGraph().getHeads().contains(node))
			return null;
		if(!nodeToIndex.containsKey(node))
			throw new IllegalArgumentException("Error: Node '" + node.toString() + "' does not exist in the graph.");
		N immediateDominator = null;
		BitSet bitSet = (BitSet) nodeToFlowSet.get(node).clone();
		bitSet.clear(nodeToIndex.get(node));
		for(int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i+1)) {
			N dominator = indexToNode.get(i);
			BitSet cur = (BitSet) bitSet.clone();
			BitSet other = nodeToFlowSet.get(dominator);
			cur.and(other);
			if(cur.equals(other))//other is equal to or a subset of cur (i.e. cur contains all of other)
				immediateDominator = dominator;
		}
		return immediateDominator;
	}

	@Override
	public boolean isDominatedBy(N node, N dominator) {
		if(!nodeToIndex.containsKey(node) || !nodeToIndex.containsKey(dominator))
			throw new IllegalArgumentException("Error: Node '" + node.toString() + "' or dominator '" + dominator.toString() + "' do not exist in the graph.");
		return nodeToFlowSet.get(node).get(nodeToIndex.get(dominator));
	}

	@Override
	public boolean isDominatedByAll(N node, Collection<N> dominators) {
		if(!nodeToIndex.containsKey(node))
			throw new IllegalArgumentException("Error: Node '" + node.toString() + "' does not exist in the graph.");
		BitSet bitSet = nodeToFlowSet.get(node);
		for(N dom : dominators) {
			Integer i = nodeToIndex.get(dom);
			if(i == null)
				throw new IllegalArgumentException("Error: Dominator '" + dom.toString() + "' does not exist in the graph.");
			if(!bitSet.get(i))
				return false;
		}
		return true;
	}
	
}