package org.sag.acminer.scripts;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.sag.acminer.database.acminer.Doublet;
import org.sag.common.graphtools.AlElement.Color;
import org.sag.common.io.FileHelpers;
import org.sag.common.tools.SortingMethods;
import org.sag.common.tuple.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import com.google.common.base.Splitter;

public class ACMinerGraph {
	
	private Path graphFile;
	private List<GNode> entryPoints;
	private Map<String, GNode> nodes;
	private Map<String, GEdge> edges;
	
	public ACMinerGraph(Path graphFile, List<GNode> entryPoints, Map<String,GNode> nodes, Map<String,GEdge> edges)  {
		this.graphFile = graphFile;
		this.entryPoints = entryPoints;
		this.nodes = nodes;
		this.edges = edges;
	}
	
	public List<GNode> getEntryPoints() {
		return entryPoints;
	}
	
	public Map<String,GNode> getNodes() {
		return nodes;
	}
	
	public Map<String,GEdge> getEdges() {
		return edges;
	}
	
	public Path getGraphFile() {
		return graphFile;
	}
	
	private static void processColor(String c, BitSet colors) {
		if(Color.GREEN.toString().equals(c))
			colors.set(0);//EP
		else if(Color.BLUE.toString().equals(c))
			colors.set(1);//Has logic
		else if(Color.GRAY.toString().equals(c))
			colors.set(2);//Context query
	}
	
	public static ACMinerGraph readFile(String path) throws Exception {
		Path fp = FileHelpers.getPath(path);
		FileHelpers.verifyRWFileExists(fp);
		
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(fp.toFile());
		doc.getDocumentElement().normalize();
		NodeList nList = doc.getElementsByTagName("node");
		
		Map<String,GNode> idToNode = new HashMap<>();
		Map<String,GNode> epIdToNode = new HashMap<>();
		
		for(int i = 0; i < nList.getLength(); i++) {
			Node nNode = nList.item(i);
			if(nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				String id = eElement.getAttribute("id");
				int splitId = id.indexOf(':');
				if(splitId > 0) {//Inner node
					id = id.substring(0, splitId);
					Element label = (Element) eElement.getElementsByTagName("y:NodeLabel").item(0);
					String logic = label.getTextContent();
					List<String> logics = Splitter.on("\n").trimResults().splitToList(logic);
					if(logics.size() % 2 != 0)
						throw new Exception("Error: Odd number of lines for logic!?!\n"+logic);
					List<Pair<Doublet,List<GNode>>> logicList = new ArrayList<>();
					Doublet d = null;
					List<GNode> epIds = null;
					for(int index = 0; index < logics.size(); index++) {
						String cur = logics.get(index);
						if(index % 2 == 0) {
							if(Character.isDigit(cur.charAt(0)))
								throw new Exception("Error: Expected logic value but got ep id!?!\n"+logic);
							d = new Doublet(cur);
						} else {
							if(!Character.isDigit(cur.charAt(0)))
								throw new Exception("Error: Expected ep id but got logic value!?!\n"+logic);
							epIds = new ArrayList<>();//create place holder nodes until all the actual ep nodes are created
							for(String s : Splitter.on(',').trimResults().split(cur)) {
								GNode temp = new GNode();
								temp.setEpId(s);
								epIds.add(temp);
							}
							logicList.add(new Pair<Doublet, List<GNode>>(d,epIds));
							d = null;
							epIds = null;
						}
					}
					GNode node = idToNode.get(id);
					if(node == null) {
						node = new GNode();
						node.setId(id);
						idToNode.put(id, node);
					}
					node.setLogic(logicList);
				} else {//Outer node
					Element fill = (Element) eElement.getElementsByTagName("y:Fill").item(0);
					Element label = (Element) eElement.getElementsByTagName("y:NodeLabel").item(0);
					BitSet colors = new BitSet(3);
					if(fill.hasAttribute("color"))
						processColor(fill.getAttribute("color"),colors);
					if(fill.hasAttribute("color2"))
						processColor(fill.getAttribute("color2"),colors);
					if(label.hasAttribute("backgroundColor"))
						processColor(label.getAttribute("backgroundColor"),colors);
					String name = label.getTextContent();
					String epId = null;
					if(colors.get(0)) {
						epId = name.substring(0, 4).trim();
						name = name.substring(4+1).trim();
					}
					GNode node = idToNode.get(id);
					if(node == null) {
						node = new GNode();
						node.setId(id);
						idToNode.put(id, node);
					}
					node.setBs(colors);
					node.setName(name);
					node.setEpId(epId);
					if(epId != null) {
						if(epIdToNode.containsKey(epId))
							throw new Exception("Error: Conflicting entry point id "+epId);
						epIdToNode.put(epId, node);
					}
				}
			}
		}
		
		//Verify basic data after node parsing procedure
		for(GNode node : idToNode.values()) {
			if(node.getId() == null) 
				throw new Exception("Error: Found node without id\n" + node.toString());
			if(node.getName() == null)
				throw new Exception("Error: Found node without name\n" + node.toString());
			if(node.bs == null)
				throw new Exception("Error: Found node without options\n" + node.toString());
			if((node.isEntryPoint() && node.getEpId() == null) || (node.getEpId() != null && !node.isEntryPoint()))
				throw new Exception("Error: Found node where the is ep option and ep id settings do not match\n" + node.toString());
			if((node.hasLogic() && (node.getLogic() == null || node.getLogic().isEmpty())) 
					|| (!node.hasLogic() && !(node.getLogic() == null || node.getLogic().isEmpty())))
				throw new Exception("Error: Found node where the has logic option and logic settings do not match\n" + node.toString());
		}
		
		//Verify and setup final entry point list
		List<GNode> finalEpsNodes = Arrays.asList(new GNode[epIdToNode.size()]);
		for(GNode ep : epIdToNode.values()) {
			int epid = ep.getIntEpId();
			if(epid >= finalEpsNodes.size())
				throw new Exception("Error: Epid greater than the number of eps!?!\n"+ep.toString());
			if(finalEpsNodes.get(epid) == null)
				finalEpsNodes.set(epid, ep);
			else
				throw new Exception("Error: Entry point conflict\n"+ep.toString()+finalEpsNodes.get(epid).toString());
		}
		for(int i = 0; i < finalEpsNodes.size(); i++) {
			GNode ep = finalEpsNodes.get(i);
			if(ep == null)
				throw new Exception("Error: No node for epid " + i);
		}
		
		//Replace the place holder ep nodes with the actual nodes once everything has been read in
		for(GNode node : idToNode.values()) {
			if(node.hasLogic()) {
				for(Pair<Doublet,List<GNode>> pairs : node.getLogic()) {
					List<GNode> epNodes = pairs.getSecond();
					for(int i = 0; i < epNodes.size(); i++) {
						GNode epTemp = epNodes.get(i);
						GNode epNode = epIdToNode.get(epTemp.getEpId());
						if(epNode == null)
							throw new Exception("Error: Failed to find node for ep id " + epTemp.getEpId() + "\n" + node.toString());
						epNodes.set(i, epNode);
					}
				}
			}
		}
		
		//verify the logic data after everything node related has been setup
		for(GNode node : idToNode.values()) {
			if(node.hasLogic()) {
				for(Pair<Doublet,List<GNode>> p : node.getLogic()) {
					Doublet d = p.getFirst();
					List<GNode> eps = p.getSecond();
					if(d == null)
						throw new Exception("Error: Found a null doublet\n" + node.toString());
					if(eps == null || eps.isEmpty())
						throw new Exception("Error: Found a logic entry without entry points\n" + node.toString());
					for(GNode epNode : eps) {
						if(epNode.getId() == null)
							throw new Exception("Error: Found place holder entry point entry for " 
									+ Objects.toString(epNode.getEpId()) + "\n" + node.toString());
					}
				}
			}
		}
		
		//Node ids can be non-sequential and larger than the max integer so we store them in a map sorted by their id
		Map<String,GNode> finalNodes = SortingMethods.sortMapValueAscending(idToNode);
		
		Map<String,GEdge> edges = new HashMap<>();
		NodeList eList = doc.getElementsByTagName("edge");
		for(int i = 0; i < eList.getLength(); i++) {
			Node eNode = eList.item(i);
			if(eNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) eNode;
				if(eElement.hasAttribute("id") && eElement.hasAttribute("source") && eElement.hasAttribute("target")) {
					String id = eElement.getAttribute("id");
					String sourceId = eElement.getAttribute("source");
					String targetId = eElement.getAttribute("target");
					GNode source = finalNodes.get(sourceId);
					GNode target = finalNodes.get(targetId);
					if(source != null && target != null) {
						GEdge newE = new GEdge();
						newE.setId(id);
						newE.setSourceId(sourceId);
						newE.setTargetId(targetId);
						newE.setSource(source);
						newE.setTarget(target);
						edges.put(id, newE);
						source.addOutgoingEdge(newE);
						target.addIncomingEdge(newE);
					} else {
						System.err.println("Warning: Failed to find matching source (" + sourceId + ") or target (" 
								+ targetId + ") nodes for edge (" + id + "). Ignoring...");
					}
				} else {
					DOMImplementationLS temp = (DOMImplementationLS) doc.getImplementation();
					LSSerializer ser = temp.createLSSerializer();
					ser.getDomConfig().setParameter("xml-declaration", false);
					String part = ser.writeToString(eNode);
					System.err.println("Warning: Found edge with missing source, target, or id. Ignoring...\n" + part);
				}
			}
		}
		
		Map<String,GEdge> finalEdges = SortingMethods.sortMapValueAscending(edges);
		
		return new ACMinerGraph(fp, finalEpsNodes, finalNodes, finalEdges);
		
	}
	
	public static void main(String[] args) {
		if(args.length < 1)
			throw new RuntimeException("Error: Not enough args");
		try {
			readFile(args[0]);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static final class GEdge implements Comparable<GEdge> {
		private String id;
		private long longId;
		private String sourceId;
		private String targetId;
		private GNode source;
		private GNode target;
		
		public GEdge() {
			id = sourceId = targetId = null;
			source = target = null;
			longId = -1;
		}
		
		@Override
		public int hashCode() {
			return 31 * 17 + ((id == null) ? 0 : id.hashCode());
		}

		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(obj == null || !(obj instanceof GEdge))
				return false;
			GEdge other = (GEdge) obj;
			return Objects.equals(id, other.id);
		}

		@Override
		public int compareTo(GEdge o) {
			return Long.compare(this.longId, o.longId);
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(Objects.toString(id)).append(": ").append(Objects.toString(sourceId)).append(" --> ").append(Objects.toString(targetId));
			return sb.toString();
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
			//Assumes format is e/d+
			if(id != null)
				this.longId = Long.parseLong(id.substring(1));
			else
				this.longId = -1;
		}

		public String getSourceId() {
			return sourceId;
		}

		public void setSourceId(String sourceId) {
			this.sourceId = sourceId;
		}

		public String getTargetId() {
			return targetId;
		}

		public void setTargetId(String targetId) {
			this.targetId = targetId;
		}

		public GNode getSource() {
			return source;
		}

		public void setSource(GNode source) {
			this.source = source;
		}

		public GNode getTarget() {
			return target;
		}

		public void setTarget(GNode target) {
			this.target = target;
		}

		public long getLongId() {
			return longId;
		}
		
	}
	
	public static final class GNode implements Comparable<GNode> {
		private String id;
		private long longId;
		private String name;
		private BitSet bs;
		private String epId;
		private int intEpId;
		private List<Pair<Doublet,List<GNode>>> logic;
		private Set<GEdge> incoming;
		private Set<GEdge> outgoing;
		private boolean sortedIn;
		private boolean sortedOut;
		
		public GNode() {
			id = name = epId = null;
			bs = null;
			logic = null;
			longId = intEpId = -1;
			incoming = null;
			outgoing = null;
			sortedIn = false;
			sortedOut = false;
		}
		
		@Override
		public int hashCode() {
			return 31 * 17 + ((id == null) ? 0 : id.hashCode());
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null || !(obj instanceof GNode))
				return false;
			GNode other = (GNode) obj;
			return Objects.equals(id, other.id);
		}

		public void setId(String id) {
			this.id = id;
			//Assumes format is n/d+
			if(id != null)
				this.longId = Long.parseLong(id.substring(1));
			else
				this.longId = -1;
		}
		
		public String getId() {
			return this.id;
		}
		
		public long getLongId() {
			return this.longId;
		}
		
		public void setEpId(String epId) {
			this.epId = epId;
			//Format should be 001
			if(epId != null)
				this.intEpId = Integer.parseInt(epId);
			else
				this.intEpId = -1;
		}
		
		public int getIntEpId() {
			return this.intEpId;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		
		public String getEpId() {
			return epId;
		}
		
		public void setBs(BitSet bs) {
			this.bs = bs;
		}
		
		public boolean isEntryPoint() {
			return bs.get(0);
		}
		
		public boolean hasLogic() {
			return bs.get(1);
		}
		
		public boolean isContextQuery() {
			return bs.get(2);
		}
		
		public boolean isPlain() {
			return bs.isEmpty();
		}
		
		public void setLogic(List<Pair<Doublet,List<GNode>>> logic) {
			this.logic = logic;
		}
		
		public List<Pair<Doublet,List<GNode>>> getLogic() {
			return logic;
		}
		
		public void addOutgoingEdge(GEdge edge) {
			if(edge == null)
				return;
			if(outgoing == null)
				outgoing = new HashSet<>();
			outgoing.add(edge);
			sortedOut = false;
		}
		
		public Set<GEdge> getOutgoingEdges() {
			if(!sortedOut) {
				outgoing = SortingMethods.sortSet(outgoing);
				sortedOut = true;
			}
			return outgoing;
		}
		
		public void addIncomingEdge(GEdge edge) {
			if(edge == null)
				return;
			if(incoming == null)
				incoming = new HashSet<>();
			incoming.add(edge);
			sortedIn = false;
		}
		
		public Set<GEdge> getIncomingEdges() {
			if(!sortedIn) {
				incoming = SortingMethods.sortSet(incoming);
				sortedIn = true;
			}
			return incoming;
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("Node id=").append(Objects.toString(id)).append(" Name=").append(Objects.toString(name));
			if(bs != null) {
				if(isEntryPoint())
					sb.append(" EPId=").append(Objects.toString(epId));
				if(isContextQuery())
					sb.append(" CQ");
				if(hasLogic())
					sb.append(" hasLogic");
			}
			sb.append("\n");
			if(logic != null) {
				sb.append("  Logic:\n");
				for(Pair<Doublet,List<GNode>> p : logic) {
					sb.append("    ").append(Objects.toString(p.getFirst())).append("\n");
					sb.append("      ");
					boolean first = true;
					for(GNode node : p.getSecond()) {
						if(first)
							first = false;
						else
							sb.append(", ");
						sb.append(Objects.toString(node.getEpId()));
					}
					sb.append("\n");
				}
			}
			if(incoming != null) {
				sb.append("  Incoming Edges:\n");
				for(GEdge e : incoming) {
					sb.append("    ").append(Objects.toString(e)).append("\n");
				}
			}
			if(outgoing != null) {
				sb.append("  Outgoing Edges:\n");
				for(GEdge e : outgoing) {
					sb.append("    ").append(Objects.toString(e)).append("\n");
				}
			}
			return sb.toString();
		}

		@Override
		public int compareTo(GNode o) {
			return Long.compare(this.longId, o.longId);
		}
	}

}
