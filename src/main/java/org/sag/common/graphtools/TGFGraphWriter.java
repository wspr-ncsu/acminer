package org.sag.common.graphtools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.sag.common.io.PrintStreamUnixEOL;

public class TGFGraphWriter<A,B> {
	
	private final List<Node> nodes;
	private final List<Edge> edges;

	public TGFGraphWriter(Collection<A> nodes, Collection<B> edges, 
			TGFNodeTranslator<A> nodeTranslator, TGFEdgeTranslator<A,B> edgeTranslator) {
		int id = 1;
		Map<A,Node> nodesMap = new LinkedHashMap<>();
		for(A node : nodes) {
			nodesMap.put(node, new Node(id++, nodeTranslator.getName(node)));
		}
		List<Edge> edgesList = new ArrayList<>();
		for(B edge : edges) {
			edgesList.add(new Edge(nodesMap.get(edgeTranslator.getSrc(edge)), nodesMap.get(edgeTranslator.getTgt(edge)), 
					edgeTranslator.getText(edge)));
		}
		this.nodes = new ArrayList<>(nodesMap.values());
		this.edges = edgesList;
	}
	
	public void writeToFile(Path output) throws IOException {
		try(PrintStreamUnixEOL ps = new PrintStreamUnixEOL(Files.newOutputStream(output))) {
			for(Node n : nodes) {
				ps.print(n.id);
				if(n.name != null)
					ps.println(" " + n.name);
				else
					ps.println();
			}
			ps.println("#");
			for(Edge e : edges) {
				ps.print(e.src.id + " " + e.tgt.id);
				if(e.text != null)
					ps.println(" " + e.text);
				else
					ps.println();
			}
		}
	}
	
	private static final class Node {
		public int id;
		public String name;
		
		public Node(int id, String name) {
			this.id = id;
			this.name = name;
		}
		
		@Override
		public int hashCode() {
			int i = 17;
			i = i * 31 + id;
			i = i * 31 + Objects.hashCode(name);
			return i;
		}
		
		@Override
		public boolean equals(Object o) {
			if(this == o)
				return true;
			if(o == null || !(o instanceof Node))
				return false;
			Node other = (Node)o;
			return id == other.id && Objects.equals(name, other.name);
		}
	}
	
	private static final class Edge {
		public Node src;
		public Node tgt;
		public String text;
		
		public Edge(Node src, Node tgt, String text) {
			this.src = src;
			this.tgt = tgt;
			this.text = text;
		}
		
		@Override
		public int hashCode() {
			int i = 17;
			i = i * 31 + Objects.hashCode(src);
			i = i * 31 + Objects.hashCode(tgt);
			i = i * 31 + Objects.hashCode(text);
			return i;
		}
		
		@Override
		public boolean equals(Object o) {
			if(this == o)
				return true;
			if(o == null || !(o instanceof Edge))
				return false;
			Edge other = (Edge)o;
			return Objects.equals(src, other.src) && Objects.equals(tgt, other.tgt) 
					&& Objects.equals(text, other.text);
		}
	}
	
	public static interface TGFNodeTranslator<A> {
		public String getName(A node);
	}
	
	public static interface TGFEdgeTranslator<A,B> {
		public A getSrc(B edge);
		public A getTgt(B edge);
		public String getText(B edge);
	}
	
}
