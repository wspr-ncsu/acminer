package org.sag.acminer.database.defusegraph;

import java.nio.file.Path;

import org.sag.acminer.database.defusegraph.id.Identifier;
import org.sag.xstream.XStreamInOut;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import soot.SootMethod;
import soot.Unit;

@XStreamAlias("Node")
public class Node extends AbstractNode {
	
	public Node(SootMethod source, Unit unit, Identifier identifier) {
		super(source, unit, identifier);
	}
	
	@Override
	public Node readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
}
