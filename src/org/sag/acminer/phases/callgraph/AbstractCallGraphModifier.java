package org.sag.acminer.phases.callgraph;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.common.logging.ILogger;
import soot.Scene;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public abstract class AbstractCallGraphModifier {
	
	protected final IACMinerDataAccessor dataAccessor;
	protected final ILogger mainLogger;
	protected final String cn;
	
	public AbstractCallGraphModifier(IACMinerDataAccessor dataAccessor, ILogger mainLogger){
		this.dataAccessor = dataAccessor;
		this.mainLogger = mainLogger;
		this.cn = this.getClass().getSimpleName();
	}
	
	public abstract boolean run();
	
	protected CallGraph getCurrentCallGraph() {
		try {
			return Scene.v().getCallGraph();
		} catch(Throwable t) {
			mainLogger.fatal("{}: Failed to get the current callgraph.",t,cn);
			return null;
		}
	}
	
	protected Set<Edge> getEdgesFromCallGraph(CallGraph cg) {
		Set<Edge> ret = new HashSet<>();
		for(Iterator<Edge> itE = cg.iterator(); itE.hasNext();) {
			Edge e = itE.next();
			ret.add(new Edge(e.src(), e.srcStmt(), e.tgt(), e.kind()));
		}
		return ret;
	}
	
	protected Set<Edge> removeEdgesToClinit(Set<Edge> edges, boolean setPhantom) {
		for(Iterator<Edge> it = edges.iterator(); it.hasNext();) {
			Edge e = it.next();
			SootMethod target = e.tgt();
			if(target.getName().equals("<clinit>")) {
				it.remove();
				if(setPhantom) {
					target.releaseActiveBody();
					target.setSource(null);
					target.setPhantom(true);
				}
			}
		}
		return edges;
	}
	
	protected Set<Edge> removeEdgesFromClinit(Set<Edge> edges, boolean setPhantom) {
		for(Iterator<Edge> it = edges.iterator(); it.hasNext();) {
			Edge e = it.next();
			SootMethod caller = e.src();
			if(caller.getName().equals("<clinit>")) {
				it.remove();
				if(setPhantom) {
					caller.releaseActiveBody();
					caller.setSource(null);
					caller.setPhantom(true);
				}
			}
		}
		return edges;
	}
	
	protected Set<Edge> removeExcludedEdges(Set<Edge> edges, boolean setPhantom) {
		for(Iterator<Edge> it = edges.iterator(); it.hasNext();) {
			Edge e = it.next();
			SootMethod caller = e.src();
			if(dataAccessor.getExcludedElementsDB().isExcludedMethod(caller)) {
				it.remove();
				if(setPhantom) {
					caller.releaseActiveBody();
					caller.setSource(null);
					caller.setPhantom(true);
				}
			}
		}
		return edges;
	}
	
	protected Set<Edge> adjustBinderEdges(Set<Edge> edges) {
		Set<Edge> ret = new HashSet<>();
		for(Edge e : edges) {
			Set<SootMethod> epsOfInvoke = null;
			if(e.srcStmt() != null && e.srcStmt().containsInvokeExpr())
				epsOfInvoke = dataAccessor.getEntryPointsFromBinderMethod(e.srcStmt().getInvokeExpr());
			if(epsOfInvoke == null) { //This is not an invoke that leads to an entry point so just keep the current edge
				ret.add(e);
			} else { //This is an invoke that leads to an entry point so skip all its edges and record for creation
				for(SootMethod sm : epsOfInvoke) {
					ret.add(new Edge(e.src(), e.srcStmt(), sm, e.kind()));
				}
			}
		}
		return ret;
	}
	
	protected Set<Edge> removeBinderEdges(Set<Edge> edges, boolean setPhantom) {
		for(Iterator<Edge> it = edges.iterator(); it.hasNext();) {
			Edge e = it.next();
			SootMethod target = e.tgt();
			Set<SootMethod> epsOfInvoke = null;
			if(e.srcStmt() != null && e.srcStmt().containsInvokeExpr())
				epsOfInvoke = dataAccessor.getEntryPointsFromBinderMethod(e.srcStmt().getInvokeExpr());
			if(epsOfInvoke != null) {
				it.remove();
				if(setPhantom) {
					target.releaseActiveBody();
					target.setSource(null);
					target.setPhantom(true);
				}
			}
		}
		return edges;
	}
	
	protected CallGraph makeCallGraph(Set<Edge> edges) {
		CallGraph newCallGraph = new CallGraph();
		for(Edge e : edges) {
			newCallGraph.addEdge(e);
		}
		return newCallGraph;
	}
	
	protected boolean setCallGraph(CallGraph newCallGraph) {
		try {
			Scene.v().releaseCallGraph();
			Scene.v().releaseReachableMethods();
			Scene.v().releaseActiveHierarchy();
			Scene.v().releaseFastHierarchy();
			Scene.v().releasePointsToAnalysis();
			Scene.v().releaseSideEffectAnalysis();
			Scene.v().setCallGraph(newCallGraph);
			mainLogger.info("{}: Successfully set the new callgraph.",cn);
		} catch(Throwable t) {
			mainLogger.fatal("{}: Failed to set the new callgraph.",t,cn);
			return false;
		}
		return true;
	}

}
