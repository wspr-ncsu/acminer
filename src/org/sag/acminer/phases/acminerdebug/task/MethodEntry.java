package org.sag.acminer.phases.acminerdebug.task;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.sag.soot.SootSort;
import org.sag.xstream.XStreamInOut;
import org.sag.xstream.XStreamInOut.XStreamInOutInterface;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import soot.Scene;
import soot.SootMethod;

@XStreamAlias("MethodEntry")
public class MethodEntry implements XStreamInOutInterface {

	@XStreamAlias("Method")
	private String method;
	@XStreamAlias("Depth")
	private int depth; // Count includes the this node
	@XStreamAlias("SubgraphNodes")
	private int subGraphNodes; // Count includes this node
	@XStreamAlias("SubgraphEdges")
	private int subGraphEdges;
	@XStreamAlias("Path")
	private ArrayList<String> path; // Does not include this node and null if empty
	@XStreamAlias("SubGraph")
	private ArrayList<String> subGraph; // Does not include this node and null if empty
	
	private MethodEntry() {}
	
	public MethodEntry(SootMethod method, List<SootMethod> path, Set<SootMethod> subGraph, int edgesCount) {
		this.method = method.getSignature();
		this.path = new ArrayList<>();
		for(SootMethod sm : path)
			this.path.add(sm.getSignature());
		this.depth = path.size();
		this.subGraph = new ArrayList<>();
		for(SootMethod sm : subGraph)
			this.subGraph.add(sm.getSignature());
		this.subGraphNodes = subGraph.size();
		this.subGraphEdges = edgesCount;
	}
	
	public String getMethod() {
		return method;
	}
	
	public SootMethod getSootMethod() {
		return Scene.v().getMethod(method);
	}
	
	public int getDepth() {
		return depth;
	}
	
	public int getSubgraphNodes() {
		return subGraphNodes;
	}
	
	public int getSubgraphEdges() {
		return subGraphEdges;
	}
	
	public List<String> getPath() {
		if(path == null)
			return ImmutableList.of();
		return ImmutableList.<String>copyOf(path);
	}
	
	public List<SootMethod> getSootPath() {
		if(path == null)
			return ImmutableList.of();
		Builder<SootMethod> ret = ImmutableList.builder();
		for(String s : path) {
			ret.add(Scene.v().getMethod(s));
		}
		return ret.build();
	}
	
	public List<String> getSubgraph() {
		if(subGraph == null)
			return ImmutableList.of();
		return ImmutableList.<String>copyOf(subGraph);
	}
	
	public List<SootMethod> getSootSubgraph() {
		if(subGraph == null)
			return ImmutableList.of();
		Builder<SootMethod> ret = ImmutableList.builder();
		for(String s : subGraph) {
			ret.add(Scene.v().getMethod(s));
		}
		return ret.build();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + depth;
		result = prime * result + ((method == null) ? 0 : method.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + ((subGraph == null) ? 0 : subGraph.hashCode());
		result = prime * result + subGraphEdges;
		result = prime * result + subGraphNodes;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || !(obj instanceof MethodEntry))
			return false;
		MethodEntry other = (MethodEntry) obj;
		return other.depth == depth && subGraphEdges == other.subGraphEdges && subGraphNodes == other.subGraphNodes
				&& Objects.equals(method, other.method) && Objects.equals(path, other.path) && Objects.equals(subGraph, other.subGraph);
	}

	public static class DecNodeComp implements Comparator<MethodEntry> {

		@Override
		public int compare(MethodEntry o1, MethodEntry o2) {
			int ret = Integer.compare(o2.getSubgraphNodes(), o1.getSubgraphNodes());
			if(ret == 0)
				ret = SootSort.smStringComp.compare(o1.getMethod(), o2.getMethod());
			return ret;
		}
		
	}
	
	public static class DecEdgeComp implements Comparator<MethodEntry> {

		@Override
		public int compare(MethodEntry o1, MethodEntry o2) {
			int ret = Integer.compare(o2.getSubgraphEdges(), o1.getSubgraphEdges());
			if(ret == 0)
				ret = SootSort.smStringComp.compare(o1.getMethod(), o2.getMethod());
			return ret;
		}
		
	}
	
	public static class NameComp implements Comparator<MethodEntry> {

		@Override
		public int compare(MethodEntry o1, MethodEntry o2) {
			return SootSort.smStringComp.compare(o1.getMethod(), o2.getMethod());
		}
		
	}
	
	public static class WeightedComp implements Comparator<MethodEntry> {
		private double scale;
		private int maxDepth;
		private int graphSize;
		private boolean isGraphSizeMax;
		
		public WeightedComp(List<MethodEntry> entries) {
			maxDepth = 0;
			graphSize = 0;
			for(MethodEntry e : entries) {
				if(e.getDepth() > maxDepth)
					maxDepth = e.getDepth();
				if(e.getSubgraphNodes() > graphSize)
					graphSize = e.getSubgraphNodes();
			}
			if(graphSize >= maxDepth) {
				isGraphSizeMax = true;
				scale = ((double) graphSize) / ((double) maxDepth);
			} else {
				isGraphSizeMax = false;
				scale = ((double) maxDepth) / ((double) graphSize);
			}
		}
		
		public int getMaxDepth() {
			return maxDepth;
		}
		
		public int getGraphSize() {
			return graphSize;
		}
		
		public double getScale() {
			return scale;
		}
		
		@Override
		public int compare(MethodEntry o1, MethodEntry o2) {
			int o1Depth = o1.getDepth();
			int o1Size = o1.getSubgraphNodes();
			int o2Depth = o2.getDepth();
			int o2Size = o2.getSubgraphNodes();
			double o1Val;
			double o2Val;
			if(isGraphSizeMax) {
				o1Val = (((double)o1Depth) * scale) + o1Size;
				o2Val = (((double)o2Depth) * scale) + o2Size;
			} else {
				o1Val = (((double)o1Size) * scale) + o1Depth;
				o2Val = (((double)o2Size) * scale) + o2Depth;
			}
			return Double.compare(o2Val, o1Val);
		}
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}
	
	@Override
	public MethodEntry readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static MethodEntry readXMLStatic(String filePath, Path path) throws Exception {
		return new MethodEntry().readXML(filePath, path);
	}
	
	@XStreamOmitField
	private static AbstractXStreamSetup xstreamSetup = null;

	public static AbstractXStreamSetup getXStreamSetupStatic(){
		if(xstreamSetup == null)
			xstreamSetup = new XStreamSetup();
		return xstreamSetup;
	}
	
	@Override
	public AbstractXStreamSetup getXStreamSetup() {
		return getXStreamSetupStatic();
	}
	
	public static class XStreamSetup extends AbstractXStreamSetup {
		
		@Override
		public void getOutputGraph(LinkedHashSet<AbstractXStreamSetup> in) {
			if(!in.contains(this)) {
				in.add(this);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(MethodEntry.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}
	
}
