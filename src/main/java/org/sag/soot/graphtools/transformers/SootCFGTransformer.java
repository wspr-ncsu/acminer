package org.sag.soot.graphtools.transformers;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.sag.common.graphtools.AlEdge;
import org.sag.common.graphtools.AlNode;
import org.sag.common.graphtools.Formatter;
import org.sag.common.graphtools.Transformer;
import org.sag.common.graphtools.AlElement.Color;
import org.sag.common.graphtools.AlNode.Shape;
import org.sag.common.tuple.Pair;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import soot.SootMethod;
import soot.Unit;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.util.Chain;

public class SootCFGTransformer extends Transformer<Unit> {
	
	private volatile Map<SootMethod,UnitGraph> methodsToGraphs;
	private volatile Map<SootMethod, List<AlNode>> methodToNode;
	private volatile Map<SootMethod, Map<Unit,AlNode>> methodToUnitToNode;
	private volatile Map<SootMethod, List<AlEdge>> methodToEdge;
	private volatile Map<SootMethod, Map<Pair<Unit,Unit>, AlEdge>> methodToPairToEdge;
	private volatile Set<Pair<Unit,Unit>> exceptionEdges;
	private volatile Set<Unit> heads;

	public SootCFGTransformer(Map<SootMethod,UnitGraph> methodsToGraphs){
		super();
		this.methodsToGraphs = ImmutableMap.copyOf(methodsToGraphs);
		this.methodToUnitToNode = Collections.emptyMap();
		this.methodToNode = Collections.emptyMap();
		this.methodToEdge = Collections.emptyMap();
		this.exceptionEdges = Collections.emptySet();
		this.methodToPairToEdge = Collections.emptyMap();
		this.heads = Collections.emptySet();
	}
	
	public Map<Unit, AlNode> getNodeToGraphNodeMap() {
		Map<Unit, AlNode> ret = new HashMap<>();
		for(Map<Unit,AlNode> m : methodToUnitToNode.values()) {
			ret.putAll(m);
		}
		return ret;
	}

	public Map<Pair<Unit, Unit>, AlEdge> getEdgeToGraphEdgeMap() {
		Map<Pair<Unit, Unit>, AlEdge> ret = new HashMap<>();
		for(Map<Pair<Unit, Unit>, AlEdge> m : methodToPairToEdge.values()) {
			ret.putAll(m);
		}
		return ret;
	}
	
	public Set<Pair<Unit,Unit>> getExceptionEdges() {
		return exceptionEdges;
	}
	
	public Set<Unit> getHeads(){
		return heads;
	}
	
	public Collection<Formatter> getFormatters(long nodeColorIndex, long edgeColorIndex, List<Path> outputPaths){
		List<Formatter> ret = new ArrayList<>();
		int i = 0;
		for(SootMethod m : methodsToGraphs.keySet()){
			ret.add(getFormatterUnchecked(nodeColorIndex,edgeColorIndex,outputPaths.get(i++),m));
		}
		return ret;
	}
	
	public Formatter getFormatter(long nodeColorIndex, long edgeColorIndex, Path outputPath, SootMethod m){
		if(methodsToGraphs.keySet().contains(m)){
			return getFormatterUnchecked(nodeColorIndex,edgeColorIndex,outputPath,m);
		}
		return null;
	}
	
	private Formatter getFormatterUnchecked(long nodeColorIndex, long edgeColorIndex, Path outputPath, final SootMethod m){
		return new CFGFormatter(m,methodToNode.get(m),methodToEdge.get(m),nodeColorIndex,edgeColorIndex,outputPath);
	}
	
	private static class CFGFormatter extends Formatter {
		private final Collection<AlNode> nodes;
		private final Collection<AlEdge> edges;
		private final SootMethod sm;
		public CFGFormatter(SootMethod sm, Collection<AlNode> nodes, Collection<AlEdge> edges, long nodeColorIndex, long edgeColorIndex, 
				Path outputPath) {
			super(nodeColorIndex, -1, edgeColorIndex, -1, outputPath);
			this.nodes = nodes;
			this.edges = edges;
			this.sm = sm;
		}
		@Override public Collection<AlNode> getNodes() { return nodes; }
		@Override public Collection<AlEdge> getEdges() { return edges; }
		@Override public void format() {}
		public String getComment() {
			StringBuilder sb = new StringBuilder();
			sb.append(super.getComment());
			sb.append("  Type: ").append("Soot Control Flow Graph\n");
			sb.append("  Method: ").append(sm.toString()).append("\n");
			return sb.toString();
		}
	}
	
	private String getLabel(Unit u, CustomUnitPrinter up){
		if(up.labels().containsKey(u)){
			up.unitRef( u, true );
			up.literal(": ");
		}
		if (up.references().containsKey(u)) {
			up.unitRef( u, false );
		}
		
		up.startUnit(u);
		u.toString(up);
		up.endUnit(u);
		return up.toString();
	}
	
	public void transform(){
		this.methodToUnitToNode = new HashMap<>();
		this.methodToNode = new HashMap<>();
		this.methodToEdge = new HashMap<>();
		this.exceptionEdges = new HashSet<>();
		this.methodToPairToEdge = new HashMap<>();
		this.heads = new HashSet<>();
		for(SootMethod m : methodsToGraphs.keySet()){
			resetId();
			UnitGraph unitGraph = methodsToGraphs.get(m);
			Map<Unit, AlNode> unitToNode = new HashMap<Unit,AlNode>();
			Map<Pair<Unit,Unit>, AlEdge> edgeToEdge = new HashMap<>();
			transformHelper(unitGraph,unitToNode,edgeToEdge);
			List<AlNode> nodes = new ArrayList<AlNode>(unitToNode.values());
			List<AlEdge> edges = new ArrayList<AlEdge>(edgeToEdge.values());
			Collections.sort(nodes);
			Collections.sort(edges);
			nodes = ImmutableList.copyOf(nodes);
			edges = ImmutableList.copyOf(edges);
			methodToNode.put(m, nodes);
			methodToEdge.put(m, edges);
			methodToUnitToNode.put(m, ImmutableMap.copyOf(unitToNode));
			methodToPairToEdge.put(m, ImmutableMap.copyOf(edgeToEdge));
		}
		this.methodToNode = ImmutableMap.copyOf(this.methodToNode);
		this.methodToEdge = ImmutableMap.copyOf(this.methodToEdge);
		this.methodToUnitToNode = ImmutableMap.copyOf(this.methodToUnitToNode);
		this.methodToPairToEdge = ImmutableMap.copyOf(this.methodToPairToEdge);
		this.exceptionEdges = ImmutableSet.copyOf(this.exceptionEdges);
		this.heads = ImmutableSet.copyOf(this.heads);
	}
	
	
	private void transformHelper(UnitGraph unitGraph, Map<Unit, AlNode> unitToNode, Map<Pair<Unit,Unit>, AlEdge> edgeToEdge) {
		CustomUnitPrinter up = new CustomUnitPrinter(unitGraph.getBody());
		Chain<Unit> units = unitGraph.getBody().getUnits();
		heads.addAll(unitGraph.getHeads());
		for(Unit u : units){
			AlNode node = unitToNode.get(u);
			if(node == null){
				node = new AlNode(nextId(),getLabel(u,up));
				unitToNode.put(u,node);
			}
			
			Set<Unit> nEdge;
			if(unitGraph instanceof ExceptionalUnitGraph){
				nEdge = new HashSet<Unit>(((ExceptionalUnitGraph)unitGraph).getUnexceptionalSuccsOf(u));
			}else{
				nEdge = new HashSet<Unit>(unitGraph.getSuccsOf(u));
			}
			for(Unit next : nEdge){
				if(!edgeToEdge.containsKey(new Pair<Unit,Unit>(u,next))){
					AlNode nextNode = unitToNode.get(next);
					if(nextNode == null){
						nextNode = new AlNode(nextId(),getLabel(next,up));
						unitToNode.put(next, nextNode);
					}
					edgeToEdge.put(new Pair<Unit,Unit>(u,next), new AlEdge(nextId(),node,nextNode));
				}
			}
			if(unitGraph instanceof ExceptionalUnitGraph){
				Set<Unit> eEdge = new HashSet<Unit>(((ExceptionalUnitGraph)unitGraph).getExceptionalSuccsOf(u));
				for(Unit next : eEdge){
					if(!edgeToEdge.containsKey(new Pair<Unit,Unit>(u,next))){
						AlNode nextNode = unitToNode.get(next);
						if(nextNode == null){
							nextNode = new AlNode(nextId(),getLabel(next,up));
							unitToNode.put(next, nextNode);
						}
						Pair<Unit,Unit> pEdge = new Pair<Unit,Unit>(u,next);
						edgeToEdge.put(pEdge, new AlEdge(nextId(),node,nextNode));
						exceptionEdges.add(pEdge);
					}
				}
			}
		}
	}
	
	public long applyColorsToNodes(Map<Unit,List<Color>> colorMap) {
		Objects.requireNonNull(colorMap);
		long ret = nextNodeColorIndex();
		for(Unit u : colorMap.keySet()) {
			for(Map<Unit,AlNode> unitToNode : methodToUnitToNode.values()) {
				AlNode node = unitToNode.get(u);
				if(node != null) {
					node.setColors(ret, colorMap.get(u));
				}
			}
		}
		return ret;
	}
	
	public long applyColorsToEdges(Map<Pair<Unit,Unit>,Color> colorMap) {
		Objects.requireNonNull(colorMap);
		long ret = nextEdgeColorIndex();
		for(Pair<Unit,Unit> e : colorMap.keySet()) {
			for(Map<Pair<Unit,Unit>, AlEdge> unitsToEdge : methodToPairToEdge.values()) {
				AlEdge edge = unitsToEdge.get(e);
				if(edge != null) {
					edge.setColor(ret, colorMap.get(e));
				}
			}
		}
		return ret;
	}
	
	public long applyShapesToNodes(Map<Unit,Shape> shapeMap) {
		Objects.requireNonNull(shapeMap);
		long ret = nextNodeColorIndex();
		for(Unit u : shapeMap.keySet()) {
			for(Map<Unit,AlNode> unitToNode : methodToUnitToNode.values()) {
				AlNode node = unitToNode.get(u);
				if(node != null) {
					node.setShape(ret, shapeMap.get(u));
				}
			}
		}
		return ret;
	}
	
	@Override
	public long applyExtraDataToNodes(Map<Unit, String> extraDataMap) {
		Objects.requireNonNull(extraDataMap);
		long ret = nextNodeShapeIndex();
		for(Unit u : extraDataMap.keySet()) {
			for(Map<Unit,AlNode> unitToNode : methodToUnitToNode.values()) {
				AlNode node = unitToNode.get(u);
				if(node != null)
					node.setExtraData(ret, extraDataMap.get(u));
			}
		}
		return ret;
	}

}
