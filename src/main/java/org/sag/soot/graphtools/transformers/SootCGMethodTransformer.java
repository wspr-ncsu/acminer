package org.sag.soot.graphtools.transformers;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.sag.common.graphtools.AlEdge;
import org.sag.common.graphtools.AlNode;
import org.sag.common.graphtools.Transformer;
import org.sag.common.graphtools.AlElement.Color;
import org.sag.common.graphtools.AlNode.Shape;
import org.sag.common.tuple.Pair;
import com.google.common.collect.ImmutableMap;

import soot.Scene;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class SootCGMethodTransformer extends Transformer<SootMethod> {

	private volatile Map<SootMethod, AlNode> methodToNode;
	private volatile Map<Pair<SootMethod,SootMethod>,AlEdge> pairToEdge;
	private final CallGraph cg;
	private final boolean skipSingleEdgeLoopBacks;
	private final Set<SootMethod> eps;
	
	/** Uses the default CallGraph object in Scene and sets skipSingleEdgeLoopBacks to false. */
	public SootCGMethodTransformer(Set<SootMethod> eps) {
		this(Scene.v().getCallGraph(),eps);
	}
	
	/** Sets skipSingleEdgeLoopBacks to false. */
	public SootCGMethodTransformer(CallGraph cg, Set<SootMethod> eps) {
		this(cg,eps, false);
	}
	
	public SootCGMethodTransformer(CallGraph cg, Set<SootMethod> eps, boolean skipSingleEdgeLoopBacks) {
		super();
		methodToNode = Collections.emptyMap();
		pairToEdge = Collections.emptyMap();
		this.cg = cg;
		this.skipSingleEdgeLoopBacks = skipSingleEdgeLoopBacks;
		this.eps = eps;
	}
	
	public Map<SootMethod, AlNode> getNodeToGraphNodeMap(){
		return methodToNode;
	}
	
	public Map<Pair<SootMethod,SootMethod>, AlEdge> getEdgeToGraphEdgeMap(){
		return pairToEdge;
	}
	
	public void transform() {
		this.methodToNode = new HashMap<>();
		this.pairToEdge = new HashMap<>();
		for(Edge e : cg) {
			SootMethod src = e.src();
			SootMethod tgt = e.tgt();
			
			//Skip edges that point back to the same class for simplification reasons
			if(skipSingleEdgeLoopBacks && src.equals(tgt))
				continue;
			
			Pair<SootMethod,SootMethod> edge = new Pair<>(src,tgt);
			AlEdge graphEdge = pairToEdge.get(edge);
			if(graphEdge == null) { //GraphEdge does not exist so the GraphNodes may or may not exist
				AlNode srcGraphNode = methodToNode.get(src);
				if(srcGraphNode == null) {
					srcGraphNode = new AlNode(nextId(),src.getSignature());
					methodToNode.put(src, srcGraphNode);
				}
				AlNode tgtGraphNode;
				if(src.equals(tgt)) { //If equal we only need to lookup and create once
					tgtGraphNode = srcGraphNode;
				} else {
					tgtGraphNode = methodToNode.get(tgt);
					if(tgtGraphNode == null) {
						tgtGraphNode = new AlNode(nextId(),tgt.getSignature());
						methodToNode.put(tgt, tgtGraphNode);
					}
				}
				graphEdge = new AlEdge(nextId(),srcGraphNode,tgtGraphNode);
				pairToEdge.put(edge, graphEdge);
			} else { //GraphEdge exist so we have already created the GraphNodes
				graphEdge.incWeight();
			}
		}
		//An ep may have no edges and thus no node would be added for it
		//However since it is an ep we need the node so we loop through all and add any missing to make sure
		for(SootMethod ep : eps) {
			if(!methodToNode.containsKey(ep))
				methodToNode.put(ep, new AlNode(nextId(),ep.getSignature()));
		}
		methodToNode = ImmutableMap.copyOf(methodToNode);
		pairToEdge = ImmutableMap.copyOf(pairToEdge);
	}
	
	public long applyColorsToNodes(Map<SootMethod,List<Color>> colorMap) {
		Objects.requireNonNull(colorMap);
		long ret = nextNodeColorIndex();
		for(SootMethod m : colorMap.keySet()) {
			AlNode node = methodToNode.get(m);
			if(node != null) {
				node.setColors(ret, colorMap.get(m));
			}
		}
		return ret;
	}
	
	public long applyColorsToEdges(Map<Pair<SootMethod,SootMethod>,Color> colorMap) {
		Objects.requireNonNull(colorMap);
		long ret = nextEdgeColorIndex();
		for(Pair<SootMethod,SootMethod> e : colorMap.keySet()) {
			AlEdge edge = pairToEdge.get(e);
			if(edge != null) {
				edge.setColor(ret, colorMap.get(e));
			}
		}
		return ret;
	}
	
	public long applyShapesToNodes(Map<SootMethod,Shape> shapeMap) {
		Objects.requireNonNull(shapeMap);
		long ret = nextNodeShapeIndex();
		for(SootMethod m : shapeMap.keySet()) {
			AlNode node = methodToNode.get(m);
			if(node != null) {
				node.setShape(ret, shapeMap.get(m));
			}
		}
		return ret;
	}
	
	@Override
	public long applyExtraDataToNodes(Map<SootMethod, String> extraDataMap) {
		Objects.requireNonNull(extraDataMap);
		long ret = nextNodeShapeIndex();
		for(SootMethod m : extraDataMap.keySet()) {
			AlNode node = methodToNode.get(m);
			if(node != null)
				node.setExtraData(ret, extraDataMap.get(m));
		}
		return ret;
	}
	
}
