package org.sag.acminer.database.defusegraph;

import java.nio.file.Path;

import org.sag.acminer.database.defusegraph.id.Identifier;
import org.sag.xstream.XStreamInOut;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import soot.SootMethod;
import soot.Unit;
import soot.Value;

@XStreamAlias("StartNode")
public class StartNode extends Node {
	
	public StartNode(SootMethod source, Unit unit, Identifier identifier) {
		super(source, unit, identifier);
	}
	
	@Override
	public Value getValue() {
		return null;
	}

	@Override
	public StartNode readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
}
