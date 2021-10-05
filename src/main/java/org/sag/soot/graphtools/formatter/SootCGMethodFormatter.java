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

public class SootCGMethodFormatter extends AbstractSootCGFormatter<SootMethod> {

	public SootCGMethodFormatter(SootClass stub, SootMethod ep, Transformer<SootMethod> trans, int depth, long nodeColorIndex, Path outputPath, 
			IACMinerDataAccessor dataAccessor) {
		super(stub, ep, trans, depth, nodeColorIndex, outputPath, dataAccessor);
	}
	
	public SootCGMethodFormatter(SootClass stub, SootMethod ep, Transformer<SootMethod> trans, int depth, long nodeColorIndex, long nodeShapeIndex, 
			long edgeColorIndex, long nodeExtraDataIndex, Path outputPath, IACMinerDataAccessor dataAccessor) {
		super(stub,ep,trans,depth,nodeColorIndex,nodeShapeIndex,edgeColorIndex,nodeExtraDataIndex,outputPath,dataAccessor);
	}
	
	public String getType() {
		return "Soot Method Call Graph";
	}

	@Override
	public void format() {
		Queue<SootMethod> tovisit = new ArrayDeque<SootMethod>();
		Queue<Integer> depthCount = new ArrayDeque<Integer>();
		Set<SootMethod> visited = new HashSet<SootMethod>();
		Map<SootMethod,AlNode> methodToNode = trans.getNodeToGraphNodeMap();
		Map<Pair<SootMethod,SootMethod>,AlEdge> pairToEdge = trans.getEdgeToGraphEdgeMap();
		CallGraph cg = Scene.v().getCallGraph();
		tovisit.add(ep);
		depthCount.add(1);
		nodes.add(methodToNode.get(ep));
		while(!tovisit.isEmpty()){
			SootMethod currMeth = tovisit.poll();
			int curDepth = depthCount.poll();
			visited.add(currMeth);
			
			if(depth == 0 || curDepth < depth){
				if(isExcluded(currMeth)){
					continue;
				}
				Iterator<Edge> itEdge = cg.edgesOutOf(currMeth);
				while(itEdge.hasNext()){
					Edge e = itEdge.next();
					SootMethod tgt = e.tgt();
					
					if(conditional(tgt)){
						continue;
					}
					
					edges.add(pairToEdge.get(new Pair<SootMethod,SootMethod>(currMeth,tgt)));
					if(!(currMeth.equals(tgt) || visited.contains(tgt) || tovisit.contains(tgt))){
						nodes.add(methodToNode.get(tgt));
						tovisit.add(tgt);
						depthCount.add(curDepth+1);
					}
				}
			}
		}
	}
	
}
