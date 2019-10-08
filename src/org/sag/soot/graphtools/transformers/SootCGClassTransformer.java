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
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class SootCGClassTransformer extends Transformer<SootClass> {

	private volatile Map<SootClass, AlNode> classToNode;
	private volatile Map<Pair<SootClass,SootClass>, AlEdge> pairToEdge;
	private final CallGraph cg;
	private final boolean skipSingleEdgeLoopBacks;
	private final Set<SootMethod> eps;
	
	/** Uses the default CallGraph object in Scene and sets skipSingleEdgeLoopBacks to true. */
	public SootCGClassTransformer(Set<SootMethod> eps) {
		this(Scene.v().getCallGraph(),eps);
	}
	
	/** Sets skipSingleEdgeLoopBacks to true. */
	public SootCGClassTransformer(CallGraph cg, Set<SootMethod> eps) {
		this(cg,eps,true);
	}
	
	public SootCGClassTransformer(CallGraph cg, Set<SootMethod> eps, boolean skipSingleEdgeLoopBacks) {
		super();
		Objects.requireNonNull(cg);
		Objects.requireNonNull(eps);
		classToNode = Collections.emptyMap();
		pairToEdge = Collections.emptyMap();
		this.cg = cg;
		this.skipSingleEdgeLoopBacks = skipSingleEdgeLoopBacks;
		this.eps = eps;
	}
	
	public Map<SootClass, AlNode> getNodeToGraphNodeMap(){
		return classToNode;
	}
	
	public Map<Pair<SootClass,SootClass>, AlEdge> getEdgeToGraphEdgeMap(){
		return pairToEdge;
	}
	
	public void transform() {
		this.classToNode = new HashMap<>();
		this.pairToEdge = new HashMap<>();
		for(Edge e : cg) {
			SootClass src = e.src().getDeclaringClass();
			SootClass tgt = e.tgt().getDeclaringClass();
			
			//Skip edges that point back to the same class for simplification reasons
			if(skipSingleEdgeLoopBacks && src.equals(tgt))
				continue;
			
			Pair<SootClass,SootClass> edge = new Pair<>(src,tgt);
			AlEdge graphEdge = pairToEdge.get(edge);
			if(graphEdge == null) {
				AlNode srcGraphNode = classToNode.get(src);
				if(srcGraphNode == null) {
					srcGraphNode = new AlNode(nextId(),src.getName());
					classToNode.put(src, srcGraphNode);
				}
				AlNode tgtGraphNode;
				if(src.equals(tgt)) { //If equal we only need to lookup and create once
					tgtGraphNode = srcGraphNode;
				} else {
					tgtGraphNode = classToNode.get(tgt);
					if(tgtGraphNode == null) {
						tgtGraphNode = new AlNode(nextId(),tgt.getName());
						classToNode.put(tgt, tgtGraphNode);
					}
				}
				graphEdge = new AlEdge(nextId(),srcGraphNode,tgtGraphNode);
				pairToEdge.put(edge, graphEdge);
			} else {
				graphEdge.incWeight();
			}
		}
		//Class of eps may have no outgoing edges so loop over eps to make sure they all have a AlNode assigned to them
		for(SootMethod ep : eps) {
			SootClass sc = ep.getDeclaringClass();
			if(!classToNode.containsKey(sc))
				classToNode.put(sc, new AlNode(nextId(),sc.getName()));
		}
		classToNode = ImmutableMap.copyOf(classToNode);
		pairToEdge = ImmutableMap.copyOf(pairToEdge);
	}
	
	public long applyColorsToNodes(Map<SootClass,List<Color>> colorMap) {
		Objects.requireNonNull(colorMap);
		long ret = nextNodeColorIndex();
		for(SootClass sc : colorMap.keySet()) {
			AlNode node = classToNode.get(sc);
			if(node != null) {
				node.setColors(ret, colorMap.get(sc));
			}
		}
		return ret;
	}
	
	public long applyColorsToEdges(Map<Pair<SootClass,SootClass>,Color> colorMap) {
		Objects.requireNonNull(colorMap);
		long ret = nextEdgeColorIndex();
		for(Pair<SootClass,SootClass> e : colorMap.keySet()) {
			AlEdge edge = pairToEdge.get(e);
			if(edge != null) {
				edge.setColor(ret, colorMap.get(e));
			}
		}
		return ret;
	}
	
	public long applyShapesToNodes(Map<SootClass,Shape> shapeMap) {
		Objects.requireNonNull(shapeMap);
		long ret = nextNodeShapeIndex();
		for(SootClass sc : shapeMap.keySet()) {
			AlNode node = classToNode.get(sc);
			if(node != null) {
				node.setShape(ret, shapeMap.get(sc));
			}
		}
		return ret;
	}
	
	@Override
	public long applyExtraDataToNodes(Map<SootClass, String> extraDataMap) {
		Objects.requireNonNull(extraDataMap);
		long ret = nextNodeShapeIndex();
		for(SootClass sc : extraDataMap.keySet()) {
			AlNode node = classToNode.get(sc);
			if(node != null)
				node.setExtraData(ret, extraDataMap.get(sc));
		}
		return ret;
	}
	
}
