package org.sag.soot.graphtools;

import org.sag.common.graphtools.TGFGraphWriter.TGFEdgeTranslator;

import soot.SootMethod;
import soot.jimple.toolkits.callgraph.Edge;

public class TGFSootEdgeTranslator implements TGFEdgeTranslator<SootMethod,Edge> {

	@Override
	public SootMethod getSrc(Edge edge) {
		return edge.src();
	}

	@Override
	public SootMethod getTgt(Edge edge) {
		return edge.tgt();
	}

	@Override
	public String getText(Edge edge) {
		return null;
	}

}
