package org.sag.soot.graphtools.formatter;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.common.graphtools.AlEdge;
import org.sag.common.graphtools.AlNode;
import org.sag.common.graphtools.Transformer;
import org.sag.common.tuple.Pair;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class SootCGClassFormatter extends AbstractSootCGFormatter<SootClass> {

	public SootCGClassFormatter(SootClass stub, SootMethod ep, Transformer<SootClass> trans, int depth, long nodeColorIndex, Path outputPath, 
			IACMinerDataAccessor dataAccessor) {
		super(stub, ep, trans, depth, nodeColorIndex, outputPath, dataAccessor);
	}
	
	public SootCGClassFormatter(SootClass stub, SootMethod ep, Transformer<SootClass> trans, int depth, long nodeColorIndex, long nodeShapeIndex, 
			long edgeColorIndex, long nodeExtraDataIndex, Path outputPath, IACMinerDataAccessor dataAccessor) {
		super(stub,ep,trans,depth,nodeColorIndex,nodeShapeIndex,edgeColorIndex,nodeExtraDataIndex,outputPath,dataAccessor);
	}
	
	public String getType() {
		return "Soot Class Call Graph";
	}

	@Override
	public void format(){
		Queue<SootMethod> tovisit = new ArrayDeque<SootMethod>();
		Queue<Integer> depthCount = new ArrayDeque<Integer>();
		Set<SootMethod> visited = new HashSet<SootMethod>();
		Map<SootClass,AlNode> classToNode = trans.getNodeToGraphNodeMap();
		Map<Pair<SootClass,SootClass>,AlEdge> pairToEdge = trans.getEdgeToGraphEdgeMap();
		CallGraph cg = Scene.v().getCallGraph();
		tovisit.add(ep);
		depthCount.add(1);
		SootClass epClass = ep.getDeclaringClass();
		nodes.add(classToNode.get(epClass));
		while(!tovisit.isEmpty()){
			SootMethod currMeth = tovisit.poll();
			int curDepth = depthCount.poll();
			SootClass currClass = currMeth.getDeclaringClass();
			visited.add(currMeth);
			
			if(depth == 0 || curDepth < depth){
				if(isExcluded(currMeth)){
					continue;
				}
				Iterator<Edge> itEdge = cg.edgesOutOf(currMeth);
				while(itEdge.hasNext()){
					Edge e = itEdge.next();
					SootMethod tgt = e.tgt();
					SootClass tgtClass = tgt.getDeclaringClass();
					
					if(conditional(tgt)){
						continue;
					}
					
					if(visited.contains(tgt) || tovisit.contains(tgt)){
						if(!currClass.equals(tgtClass)){
							edges.add(pairToEdge.get(new Pair<SootClass,SootClass>(currClass,tgtClass)));
						}
					}else{
						if(currClass.equals(tgtClass)){
							tovisit.add(tgt);
							depthCount.add(curDepth);
						}else{
							tovisit.add(tgt);
							depthCount.add(curDepth+1);
							nodes.add(classToNode.get(tgtClass));
							edges.add(pairToEdge.get(new Pair<SootClass,SootClass>(currClass,tgtClass)));
						}
					}
						
				}
			}
		}
	}
	
}
