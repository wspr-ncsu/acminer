package org.sag.acminer.database.defusegraph;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sag.common.tools.SortingMethods;
import org.sag.common.tuple.Pair;
import org.sag.xstream.XStreamInOut;
import org.sag.xstream.XStreamInOut.XStreamInOutInterface;
import soot.Local;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("DefUseGraph")
public class DefUseGraph implements XStreamInOutInterface {
	
	@XStreamAlias("UseNodesToDefNodes")
	private volatile Map<INode, Map<LocalWrapper, Set<INode>>> useNodesToDefs;
	
	@XStreamAlias("UseNodesToInlineConstantNodes")
	private volatile Map<INode, Map<InlineConstantLocalWrapper, InlineConstantLeafNode>> useNodesToInlineConstantNodes;
	
	@XStreamOmitField
	private volatile Map<StartNode,Set<String>> startNodesToDefStrings;
	
	//Definition IValueNode -> Set of use IValueNode and the local being defined/used
	@XStreamOmitField
	private volatile Map<StartNode, Map<INode,Pair<LocalWrapper,Set<INode>>>> defNodesToUses;
	
	@XStreamAlias("StartNodes")
	private volatile ArrayList<StartNode> starts;
	
	public DefUseGraph() {}
	
	public DefUseGraph(List<StartNode> starts) {
		this.starts = new ArrayList<>(starts);
		Collections.sort(this.starts);
		this.startNodesToDefStrings = null;
		this.defNodesToUses = null;
		this.useNodesToDefs = new HashMap<>();
		this.useNodesToInlineConstantNodes = new HashMap<>();
	}
	
	public List<StartNode> getStartNodes() {
		return starts;
	}
	
	public Map<LocalWrapper, Set<INode>> getChildLocalWrappersToChildNodes(INode source) {
		Map<LocalWrapper, Set<INode>> localToDefs = useNodesToDefs.get(source);
		if(localToDefs == null || localToDefs.isEmpty() || source instanceof LeafNode)
			return Collections.emptyMap();
		Map<LocalWrapper, Set<INode>> ret = new HashMap<>();
		
		for(LocalWrapper lw : localToDefs.keySet()) {
			Set<INode> v = localToDefs.get(lw);
			if(v == null || v.isEmpty())
				ret.put(lw, Collections.<INode>emptySet());
			else
				ret.put(lw, new HashSet<>(v));
		}
		return ret;
	}
	
	Map<LocalWrapper, Set<INode>> getChildLocalWrappersToChildNodesInner(INode source) {
		Map<LocalWrapper, Set<INode>> localToDefs = useNodesToDefs.get(source);
		if(localToDefs == null || localToDefs.isEmpty() || source instanceof LeafNode)
			return Collections.emptyMap();
		return localToDefs;
	}

	public Set<LocalWrapper> getChildLocalWrappers(INode source) {
		Map<LocalWrapper, Set<INode>> localToDefs = useNodesToDefs.get(source);
		if(localToDefs == null || localToDefs.isEmpty() || source instanceof LeafNode)
			return Collections.emptySet();
		return new HashSet<>(localToDefs.keySet());
	}
	
	public Map<Local, Set<INode>> getChildLocalsToChildNodes(INode source) {
		Map<LocalWrapper, Set<INode>> localToDefs = useNodesToDefs.get(source);
		if(localToDefs == null || localToDefs.isEmpty() || source instanceof LeafNode)
			return Collections.emptyMap();
		Map<Local, Set<INode>> ret = new HashMap<>();
		for(LocalWrapper lw : localToDefs.keySet()) {
			ret.put(lw.getLocal(), new HashSet<>(localToDefs.get(lw)));
		}
		return ret;
	}
	
	public Set<Local> getChildLocals(INode source) {
		Map<LocalWrapper, Set<INode>> localToDefs = useNodesToDefs.get(source);
		if(localToDefs == null || localToDefs.isEmpty() || source instanceof LeafNode)
			return Collections.emptySet();
		Set<Local> ret = new HashSet<>();
		for(LocalWrapper lw : localToDefs.keySet()) {
			ret.add(lw.getLocal());
		}
		return ret;
	}
	
	public Set<INode> getChildNodes(INode source) {
		Map<LocalWrapper, Set<INode>> localToDefs = useNodesToDefs.get(source);
		if(localToDefs == null || localToDefs.isEmpty() || source instanceof LeafNode)
			return Collections.emptySet();
		Set<INode> ret = new HashSet<>();
		for(Set<INode> defs : localToDefs.values()) {
			ret.addAll(defs);
		}
		return ret;
	}
	
	public Map<InlineConstantLocalWrapper, InlineConstantLeafNode> getInlineConstantNodes(INode source) {
		Map<InlineConstantLocalWrapper, InlineConstantLeafNode> localToDefs = useNodesToInlineConstantNodes.get(source);
		if(localToDefs == null || localToDefs.isEmpty())
			return Collections.emptyMap();
		Map<InlineConstantLocalWrapper, InlineConstantLeafNode> ret = new HashMap<>();
		for(InlineConstantLocalWrapper lw : localToDefs.keySet()) {
			ret.put(lw, localToDefs.get(lw));
		}
		return ret;
	}
	
	public synchronized void addChild(INode source, Local l, INode target) {
		addChild(source, LocalWrapper.Factory.get(l, source.getSource()), target);
	}
	
	public synchronized void addChild(INode source, LocalWrapper lw, INode target) {
		Map<LocalWrapper, Set<INode>> localToDefs = useNodesToDefs.get(source);
		if(localToDefs == null) {
			localToDefs = new HashMap<>();
			useNodesToDefs.put(source, localToDefs);
		}
		Set<INode> defs = localToDefs.get(lw);
		if(defs == null) {
			defs = new HashSet<>();
			localToDefs.put(lw, defs);
		}
		defs.add(target);
	}
	
	public synchronized void addInlineConstantNode(INode source, InlineConstantLeafNode target) {
		Map<InlineConstantLocalWrapper, InlineConstantLeafNode> localToDefs = useNodesToInlineConstantNodes.get(source);
		if(localToDefs == null) {
			localToDefs = new HashMap<>();
			useNodesToInlineConstantNodes.put(source, localToDefs);
		}
		localToDefs.put(ILocalWrapper.Factory.get(), target);
	}
	
	public synchronized void computeDefinitionsToUses() {
		if(defNodesToUses == null) {
			defNodesToUses = new HashMap<>();
			Set<Pair<StartNode,INode>> visited = new HashSet<>();
			Deque<Pair<StartNode,INode>> toVisit = new LinkedList<>();
			for(StartNode s : starts) {
				Map<INode, Pair<LocalWrapper, Set<INode>>> dataMap = new HashMap<>();
				dataMap.put(s, null);//Start node is not a definition statement and therefore has no uses
				defNodesToUses.put(s, dataMap);
				toVisit.add(new Pair<StartNode,INode>(s,s));
			}
			while(!toVisit.isEmpty()) {
				Pair<StartNode, INode> dataP = toVisit.pop();
				StartNode start = dataP.getFirst();
				INode cur = dataP.getSecond();//Cur node is a use and child nodes are defs
				if(visited.add(dataP)) {
					//If we visited a use before then we have already constructed these relationships
					Map<LocalWrapper, Set<INode>> map = getChildLocalWrappersToChildNodes(cur);
					for(LocalWrapper lw : map.keySet()) {
						for(INode vn : map.get(lw)) {
							Pair<LocalWrapper,Set<INode>> p = defNodesToUses.get(start).get(vn);
							if(p == null) {//No need to worry about the start node poping up because it is not a definition stmt
								p = new Pair<LocalWrapper,Set<INode>>(lw,new HashSet<INode>());
								defNodesToUses.get(start).put(vn, p);
							}
							p.getSecond().add(cur);
							//We skip defs we have visited before under use exploration because no new info will be gained
							//However we still consider the def for the def -> use map because the use may be new for this def
							Pair<StartNode,INode> newP = new Pair<StartNode,INode>(start,vn);
							if(!visited.contains(newP))
								toVisit.push(newP);
						}
					}
				}
			}
		}
	}
	
	public synchronized void resolveStartNodeToDefStrings() {
		if(this.startNodesToDefStrings == null) {
			this.startNodesToDefStrings = new HashMap<>();
			Set<Pair<StartNode,INode>> visited = new HashSet<>();
			Deque<Pair<StartNode,INode>> toVisit = new LinkedList<>();
			for(StartNode s : starts) {
				toVisit.add(new Pair<StartNode,INode>(s,s));
				this.startNodesToDefStrings.put(s, new HashSet<String>());
			}
			while(!toVisit.isEmpty()) {
				Pair<StartNode,INode> p = toVisit.pop();
				StartNode start = p.getFirst();
				INode cur = p.getSecond();
				if(visited.add(p)) {
					Map<LocalWrapper, Set<INode>> localsToValueNodes = getChildLocalWrappersToChildNodesInner(cur);
					Set<String> localDefs = this.startNodesToDefStrings.get(start);
					for(LocalWrapper lw : localsToValueNodes.keySet()) {
						String lwString = lw.toString();
						for(INode child : localsToValueNodes.get(lw)) {
							localDefs.add(lwString + " = " + child.toString());
							Pair<StartNode,INode> newP = new Pair<StartNode,INode>(start,child);
							if(!visited.contains(newP))
								toVisit.add(newP);
						}
						
					}
				}
			}
			
			for(StartNode start : starts) {
				Set<String> localDefs = this.startNodesToDefStrings.get(start);
				if(localDefs.isEmpty())
					localDefs = Collections.emptySet();
				else
					localDefs = SortingMethods.sortSet(localDefs,SortingMethods.sComp);
				this.startNodesToDefStrings.put(start, localDefs);
			}
			this.startNodesToDefStrings = SortingMethods.sortMapKeyAscending(this.startNodesToDefStrings);
		}
	}
	
	public void clearStartNodesToDefStrings() {
		this.startNodesToDefStrings = null;
	}
	
	public void clearDefToUseMap() {
		this.defNodesToUses = null;
	}
	
	public Map<StartNode, Set<String>> getStartNodesToDefStrings() {
		if(startNodesToDefStrings == null)
			resolveStartNodeToDefStrings();
		Map<StartNode, Set<String>> ret = new LinkedHashMap<>();
		for(StartNode sn : startNodesToDefStrings.keySet()) {
			ret.put(sn, new LinkedHashSet<String>(startNodesToDefStrings.get(sn)));
		}
		return ret;
	}
	
	public Set<String> getDefStrings(StartNode sn) {
		if(startNodesToDefStrings == null)
			resolveStartNodeToDefStrings();
		if(startNodesToDefStrings.containsKey(sn))
			return new LinkedHashSet<String>(startNodesToDefStrings.get(sn));
		return Collections.emptySet();
	}
	
	public Pair<LocalWrapper,Set<INode>> getUsesForDefinition(StartNode start, INode def) {
		if(defNodesToUses == null)
			computeDefinitionsToUses();
		return defNodesToUses.get(start).get(def);
	}
	
	public int hashCode() {
		return starts.hashCode();
	}
	
	public boolean equals(Object o) {
		if(this == o) 
			return true;
		if(o == null || !(o instanceof DefUseGraph))
			return false;
		return Objects.equals(this.starts, ((DefUseGraph)o).starts);
	}
	
	@Override
	public String toString() {
		return toString("");
	}
	
	public String toString(String pad) {
		if(startNodesToDefStrings == null)
			resolveStartNodeToDefStrings();
		StringBuilder sb = new StringBuilder();
		for(StartNode start : startNodesToDefStrings.keySet()) {
			sb.append(pad).append("Stmt: ").append(start.toString()).append(" Source: ").append(start.getSource()).append("\n");
			for(String s : startNodesToDefStrings.get(start)) {
				sb.append(pad).append("  Def: ").append(s).append("\n");
			}
		}
		return sb.toString();
	}
	
	private static final class DefStmtNode {
		final Long def;
		final Map<Long,List<Integer>> useToIndexes;
		final List<List<DefStmtNode>> defNodesForUses;
		boolean hasChildren;
		public DefStmtNode(Long def, List<Long> uses) {
			this.def = def;
			if(uses.isEmpty()) {
				this.defNodesForUses = Collections.emptyList();
				this.useToIndexes = Collections.emptyMap();
			} else {
				this.useToIndexes = new HashMap<>();
				this.defNodesForUses = new ArrayList<>();
				for(int i = 0; i < uses.size(); i++) {
					Long l = uses.get(i);
					List<Integer> indexes = useToIndexes.get(l);
					if(indexes == null) {
						indexes = new ArrayList<>(1);
						useToIndexes.put(l, indexes);
					}
					indexes.add(i);
					defNodesForUses.add(new ArrayList<DefStmtNode>());
				}
			}
			hasChildren = false;
		}
		public void addNode(DefStmtNode node) {
			List<Integer> indexes = useToIndexes.get(node.def);
			if(indexes != null) {
				for(Integer i : indexes) {
					defNodesForUses.get(i).add(node);
				}
				hasChildren = true;
			}
		}
	}
	
	public static Map<String,BigInteger> computeAllStatementsCount(Map<String,Set<String>> startToUses) {
		if(startToUses == null || startToUses.isEmpty())
			return Collections.emptyMap();
		Map<String,BigInteger> ret = new LinkedHashMap<>();
		for(String start : startToUses.keySet()) {
			Set<String> uses = startToUses.get(start);
			ret.put(start, computeAllStatementsCount(start,uses));
		}
		if(ret.isEmpty())
			return Collections.emptyMap();
		return ret;
	}
	
	public static BigInteger computeAllStatementsCount(String start, Set<String> definitions) {
		//Create nodes from the definition statements and initialize the graph
		//Each node represents a unique definition statement so no need to worry about unique nodes
		//Also we want there to be more than one node for a defined local since the nodes represent the definition statement
		List<DefStmtNode> nodes = new ArrayList<>();
		nodes.add(new DefStmtNode(null, getUsedLocalIndexes(start)));
		for(String s : definitions)
			nodes.add(getDefStmtNode(s));
		for(DefStmtNode cur : nodes) {
			for(DefStmtNode n : nodes) {
				n.addNode(cur);
			}
		}
		
		//Compute the reverse topological order so we can iterate over the nodes from bottom up without cycles
		Set<DefStmtNode> visited = new HashSet<>();
		Deque<DefStmtNode> revTypoSort = new LinkedList<>();
		Deque<DefStmtNode> toVisit = new LinkedList<>();
		toVisit.push(nodes.get(0));
		while(!toVisit.isEmpty()) {
			DefStmtNode cur = toVisit.pop();
			if(visited.add(cur)) {
				revTypoSort.push(cur);
				for(List<DefStmtNode> defs : cur.defNodesForUses) {
					for(DefStmtNode def : defs) {
						if(!visited.contains(def))
							toVisit.add(def);
					}
				}
			}
		}
		
		//Compute the totals for each node
		Map<DefStmtNode,BigInteger> weights = new HashMap<>();
		for(DefStmtNode curNode : revTypoSort) {
			if(!curNode.hasChildren) {
				weights.put(curNode, BigInteger.ONE);
			} else {
				BigInteger total = BigInteger.ONE;
				for(List<DefStmtNode> defs : curNode.defNodesForUses) {
					BigInteger sumTotal = BigInteger.ZERO;
					for(DefStmtNode n : defs) {
						BigInteger w = weights.get(n);
						if(w == null)
							w = BigInteger.ONE;
						sumTotal = sumTotal.add(w);
					}
					total = total.multiply(sumTotal);
				}
				weights.put(curNode, total);
			}
		}
		
		return weights.get(nodes.get(0));
	}
	
	private static final Pattern usedLocal = Pattern.compile("\\$z\\{(\\d+)\\}");
	private static List<Long> getUsedLocalIndexes(String input) {
		List<Long> ret = new ArrayList<>();
		Matcher m = usedLocal.matcher(input);
		while(m.find()) {
			ret.add(Long.parseLong(m.group(1)));
		}
		if(ret.isEmpty())
			return Collections.emptyList();
		return ret;
	}
	
	private static final Pattern defLocal = Pattern.compile("^\\$z\\{(\\d+)\\}\\s+=\\s+(.+)$");
	private static DefStmtNode getDefStmtNode(String input) {
		Matcher m = defLocal.matcher(input);
		if(m.matches()) {
			Long def = Long.parseLong(m.group(1));
			List<Long> uses = getUsedLocalIndexes(m.group(2));
			return new DefStmtNode(def,uses);
		}
		return null;
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public DefUseGraph readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static DefUseGraph readXMLStatic(String filePath, Path path) throws Exception {
		return new DefUseGraph().readXML(filePath, path);
	}

	private static final XStreamSetup xstreamSetup = new XStreamSetup();

	public static XStreamSetup getXStreamSetupStatic(){
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
				INode.Factory.getXStreamSetupStatic().getOutputGraph(in);
				ILocalWrapper.Factory.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(DefUseGraph.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsXPathRelRef(xstream);
		}
		
	}

}
