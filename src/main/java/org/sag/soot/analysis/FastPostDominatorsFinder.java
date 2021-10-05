package org.sag.soot.analysis;

import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.InverseGraph;

public class FastPostDominatorsFinder<N> extends FastDominatorsFinder<N> {

	public FastPostDominatorsFinder(DirectedGraph<N> graph) {
		super(new InverseGraph<N>(graph));
	}
	
}
