package org.sag.acminer.database.defusegraph;

import java.nio.file.Path;

import org.sag.acminer.database.defusegraph.id.Identifier;
import org.sag.xstream.XStreamInOut;

import soot.SootMethod;
import soot.Unit;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("LeafNode")
public class LeafNode extends AbstractNode {
	
	protected LeafNode(SootMethod source, Unit unit, Identifier identifier) {
		super(source, unit, identifier);
	}

	@Override
	public LeafNode readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}

}
