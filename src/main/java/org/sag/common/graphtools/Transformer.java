package org.sag.common.graphtools;

import java.util.List;
import java.util.Map;

import org.sag.common.graphtools.AlElement.Color;
import org.sag.common.graphtools.AlNode.Shape;
import org.sag.common.tuple.Pair;

public abstract class Transformer<A> {

	protected long id;
	protected long nodeColorIndex;
	protected long nodeShapeIndex;
	protected long edgeColorIndex;
	protected long nodeExtraDataIndex;
	
	public Transformer(){
		id = 0;
		nodeColorIndex = 0;
		nodeShapeIndex = 0;
		edgeColorIndex = 0;
		nodeExtraDataIndex = 0;
	}
	
	protected long nextId(){
		return id++;
	}
	
	protected long nextNodeColorIndex() {
		return nodeColorIndex++;
	}
	
	protected long nextNodeShapeIndex() {
		return nodeShapeIndex++;
	}
	
	protected long nextEdgeColorIndex() {
		return edgeColorIndex++;
	}
	
	protected long nextNodeExtraDataIndex() {
		return nodeExtraDataIndex++;
	}
	
	protected void resetId() {
		id = 0;
	}
	
	protected void reset(){
		resetId();
		nodeColorIndex = 0;
		nodeShapeIndex = 0;
		edgeColorIndex = 0;
	}
	
	public abstract void transform();
	public abstract long applyColorsToNodes(Map<A,List<Color>> colorMap);
	public abstract long applyColorsToEdges(Map<Pair<A,A>,Color> colorMap);
	public abstract long applyShapesToNodes(Map<A,Shape> shapeMap);
	public abstract long applyExtraDataToNodes(Map<A, String> extraDataMap);
	public abstract Map<A,AlNode> getNodeToGraphNodeMap();
	public abstract Map<Pair<A,A>,AlEdge> getEdgeToGraphEdgeMap();

}
