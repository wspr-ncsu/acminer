package org.sag.soot.graphtools;

import org.sag.common.graphtools.TGFGraphWriter.TGFNodeTranslator;

import soot.SootMethod;

public class TGFSootNodeTranslator implements TGFNodeTranslator<SootMethod> {

	@Override
	public String getName(SootMethod node) {
		return node.getSignature();
	}

}
