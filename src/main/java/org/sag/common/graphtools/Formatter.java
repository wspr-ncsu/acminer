package org.sag.common.graphtools;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;

public abstract class Formatter {
	
	protected final long nodeColorIndex;
	protected final long nodeShapeIndex;
	protected final long edgeColorIndex;
	protected final long nodeExtraDataIndex;
	protected final Path outputPath;
	
	public Formatter(long nodeColorIndex, long nodeShapeIndex, long edgeColorIndex, long nodeExtraDataIndex, Path outputPath){
		Objects.requireNonNull(outputPath);
		this.nodeColorIndex = nodeColorIndex;
		this.nodeShapeIndex = nodeShapeIndex;
		this.edgeColorIndex = edgeColorIndex;
		this.nodeExtraDataIndex = nodeExtraDataIndex;
		this.outputPath = outputPath;
	}

	public abstract Collection<AlNode> getNodes();
	public abstract Collection<AlEdge> getEdges();
	public abstract void format();
	public String getComment() { return "\nGraph Description:\n"; }
	
	public long getNodeColorIndex() { return nodeColorIndex; }
	public long getNodeShapeIndex() { return nodeShapeIndex; }
	public long getEdgeColorIndex() { return edgeColorIndex; }
	public long getNodeExtraDataIndex() { return nodeExtraDataIndex; }
	public Path getOutputPath() { return outputPath; }
	
}
