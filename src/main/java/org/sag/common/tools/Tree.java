package org.sag.common.tools;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class Tree<T> implements Iterable<Tree<T>.Node>{

	private Node root;
	
	private HashSet<Node> nodes;
	
	public Tree(){
		root = null;
		this.nodes = new HashSet<Node>();
	}
	
	public Node getRoot(){
		return root;
	}
	
	public void initRoot(T data){
		root = createChild(null,data);
	}
	
	public void initRoot(){
		root = createChild(null,null);
	}
	
	public boolean isEmpty(){
		return root == null;
	}
	
	public int size(){
		return nodes.size();
	}
	
	public List<Node> createChildren(Node parent, List<T> childData){
		ArrayList<Node> ret = new ArrayList<Node>();
		for(T t : childData){
			ret.add(createChild(parent,t));
		}
		return ret;
	}
	
	public Node createChild(Node parent, T childData){
		Node ret = new Node(childData,parent);
		if(parent != null)
			parent.addChild(ret);
		nodes.add(ret);
		return ret;
	}
	
	public List<Node> getNodes(){
		return new ArrayList<Node>(nodes);
	}
	
	public final class Node{
		private T data;
		private Node parent;
		private List<Node> children;
		
		protected Node(T data, Node parent, List<Node> children){
			this.data = data;
			this.parent = parent;
			if(children == null)
				this.children = null;
			else
				this.children = new ArrayList<Node>(children);
		}
		
		protected Node(T data, Node parent){
			this(data,parent,null);
		}
		
		protected Node(T data){
			this(data,null,null);
		}
		
		protected Node(){
			this(null,null,null);
		}
		
		public boolean isLeaf(){
			return children == null;
		}
		
		public T getData(){
			return data;
		}
		
		public void setData(T data){
			this.data = data;
		}
		
		public Node getParent(){
			return parent;
		}
		
		protected void setParent(Node parent){
			this.parent = parent;
		}
		
		public List<Node> getChildren(){
			if(children == null)
				return null;
			return new ArrayList<Node>(children);
		}
		
		protected void setChildren(List<Node> children){
			this.children = new ArrayList<Node>(children);
		}
		
		protected void removeChild(Node rmChild){
			if(children != null){
				for(Iterator<Node> it = children.iterator(); it.hasNext();){
					Node child = it.next();
					if(child.equals(rmChild)){
						it.remove();
					}
				}
				if(children.isEmpty()){
					children = null;
				}
			}
		}
		
		protected void addChildren(List<Node> children){
			if(children == null)
				children = new ArrayList<Node>();
			this.children.addAll(children);
		}
		
		protected void addChild(Node child){
			if(children == null)
				children = new ArrayList<Node>();
			this.children.add(child);
		}
		
		public List<Node> createChildren(List<T> childData){
			return Tree.this.createChildren(this, childData);
		}
		
		public Node createChild(T childData){
			return Tree.this.createChild(this, childData);
		}
		
		public Tree<T> getTree(){
			return Tree.this;
		}
		
		public void remove(){
			if(isLeaf()){
				if(parent == null){
					Tree.this.root = null;
				}else{
					parent.removeChild(this);
					parent = null;
				}
				nodes.remove(this);
			}else{
				throw new RuntimeException("Error: Cannot remove non-leaf node from tree.");
			}
		}
		
		public String toString(){
			if(data == null)
				return null;
			return data.toString();
		}
	}

	public static final class DFSIterator<T> implements Iterator<Tree<T>.Node>{

		private Deque<Tree<T>.Node> stack;
		private HashMap<Tree<T>.Node,Iterator<Tree<T>.Node>> childs;
		private HashMap<Tree<T>.Node,Boolean> marked;
		private Tree<T>.Node next;
		
		public DFSIterator(Tree<T> tree){
			stack = new ArrayDeque<Tree<T>.Node>();
			childs = new HashMap<Tree<T>.Node, Iterator<Tree<T>.Node>>();
			marked = new HashMap<Tree<T>.Node, Boolean>();
			if(!tree.isEmpty()){
				for(Tree<T>.Node node : tree.nodes){
					marked.put(node, false);
					if(!node.isLeaf()){
						childs.put(node, node.children.iterator());
					}else{
						childs.put(node, null);
					}
				}
				stack.push(tree.root);
				marked.put(tree.root, true);
			}
			next = null;
		}
		
		@Override
		public boolean hasNext() {
			return !stack.isEmpty();
		}

		@Override
		public Tree<T>.Node next() {
			while(hasNext()){
				Tree<T>.Node cur = stack.peek();
				if(childs.get(cur) != null && childs.get(cur).hasNext()){
					Tree<T>.Node next = childs.get(cur).next();
					if(!marked.get(next)){
						marked.put(next, true);
						stack.push(next);
					}
				}else{
					next = stack.pop();
					return next;
				}
			}
			throw new NoSuchElementException();
		}

		@Override
		public void remove() {
			if(next != null){
				if(next.isLeaf()){
					if(next.parent == null){
						next.getTree().root = null;
					}else{
						childs.get(next.parent).remove();
						if(next.parent.children.isEmpty()){
							next.parent.children = null;
							childs.put(next.parent, null);
						}
						next.parent = null;
					}
					next.getTree().nodes.remove(next);
					next = null;
				}else{
					throw new RuntimeException("Error: Cannot remove non-leaf node from tree.");
				}
			}else{
				throw new IllegalStateException();
			}
		}
	}

	@Override
	public Iterator<Node> iterator() {
		return new DFSIterator<T>(this);
	}
	
	public static <T> String stringConstructor(Tree<T> tree, String childSeperator, String startChilds, String endChilds){
		if(tree.isEmpty()){
			return "";
		}else{
			Iterator<Tree<T>.Node> it = tree.iterator();
			HashMap<Tree<T>.Node,StringBuffer> parentToStringBuffers = new HashMap<Tree<T>.Node,StringBuffer>();
			while(it.hasNext()){
				Tree<T>.Node cur = it.next();
				if(cur.isLeaf()){
					StringBuffer sb = parentToStringBuffers.get(cur.getParent());
					if(sb == null){
						sb = new StringBuffer();
						parentToStringBuffers.put(cur.getParent(), sb);
					}
					sb.append(cur.toString()).append(childSeperator);
				}else{
					StringBuffer sbCur = parentToStringBuffers.get(cur);
					int index = sbCur.lastIndexOf(childSeperator);
					if(index != -1 && index == sbCur.length()-childSeperator.length()){
						sbCur.delete(index, sbCur.length());
					}
					sbCur.append(endChilds).insert(0, startChilds).insert(0, cur.toString());
					StringBuffer sb = parentToStringBuffers.get(cur.getParent());
					if(sb == null){
						sb = new StringBuffer();
						parentToStringBuffers.put(cur.getParent(), sb);
					}
					sb.append(sbCur).append(childSeperator);
				}
			}
			StringBuffer sb = parentToStringBuffers.get(null);
			int index = sb.lastIndexOf(childSeperator);
			if(index != -1 && index == sb.length()-childSeperator.length()){
				sb.delete(index, sb.length());
			}
			return sb.toString();
		}
	}
	
}
