package org.sag.acminer.database.defusegraph;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.sag.acminer.database.defusegraph.id.Identifier;
import org.sag.common.xstream.XStreamInOut.XStreamInOutInterface;
import org.sag.soot.xstream.SootFieldContainer;
import org.sag.soot.xstream.SootMethodContainer;
import org.sag.soot.xstream.SootUnitContainer;

import com.thoughtworks.xstream.XStream;

import soot.Local;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.IfStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.SwitchStmt;

public interface INode extends XStreamInOutInterface, Comparable<INode> {

	public abstract boolean equals(Object o);
	public abstract int hashCode();
	public abstract String toString();
	public abstract SootMethod getSource();
	public abstract Unit getUnit();
	public abstract Value getValue();
	public abstract Identifier getIdentifier();
	public abstract INode readXML(String filePath, Path path) throws Exception;

	public static final class Factory {
		
		private static Map<INode,INode> existingNodes = null;
		private static Map<INode,INode> nodeToCopy = null;
		
		public static void reset() {
			existingNodes = null;
			nodeToCopy = null;
		}
		
		public static void init() {
			if(existingNodes == null) {
				existingNodes = new HashMap<>();
				nodeToCopy = new HashMap<>();
			}
		}
		
		public static final InlineConstantLeafNode getNewInlineConstantLeafNode(INode cur, int index) {
			Objects.requireNonNull(cur);
			if(existingNodes == null)
				throw new RuntimeException("Error: The existing nodes list has not been initilized!");
			
			InlineConstantLeafNode ret = new InlineConstantLeafNode(cur, index);
			synchronized(existingNodes) {
				InlineConstantLeafNode r = (InlineConstantLeafNode)existingNodes.get(ret);
				if(r != null)
					ret = r;
				else
					existingNodes.put(ret, ret);
			}
			return ret;
		}
		
		public static final StartNode getNewStart(SootMethod source, Unit unit) {
			return (StartNode) getNew(source, unit, true, -1, null, null, true);
		}
		
		public static final StartNode getNewStart(SootMethod source, Unit unit, boolean keepInstanceObject) {
			return (StartNode) getNew(source, unit, true, -1, null, null, keepInstanceObject);
		}
		
		public static final INode getNew(SootMethod source, Unit unit, int index, SootMethod target, SootField field) {
			return getNew(source, unit, false, index, target, field);
		}
		
		public static final INode getNew(SootMethod source, Unit unit, boolean isStart, int index, SootMethod target, SootField field) {
			return getNew(source, unit, isStart, index, target, field, true);
		}
		
		//LocalWrapper init must be called before the use of this method
		public static final INode getNew(SootMethod source, Unit unit, boolean isStart, int index, SootMethod target, SootField field, 
				boolean keepInstanceObject) {
			Objects.requireNonNull(source);
			Objects.requireNonNull(unit);
			if(existingNodes == null)
				throw new RuntimeException("Error: The existing nodes list has not been initilized!");
			
			Set<Local> localUses = getLocalUses(unit, keepInstanceObject);
			INode ret = null;
			if(isStart || unit instanceof IfStmt || unit instanceof SwitchStmt) {
				if(((Stmt)unit).containsInvokeExpr())
					ret = new InvokeStartNode(source, unit, target, Identifier.getInvokeExprId(unit, source, target, keepInstanceObject));
				else if(((Stmt)unit).containsFieldRef())
					ret = new FieldStartNode(source, unit, field, Identifier.getFieldId(unit, source, field, keepInstanceObject));
				else
					ret = new StartNode(source, unit, Identifier.getUnitId(unit, source, keepInstanceObject));
			} else if(index >= 0) {
				ret = new InvokeConstantLeafNode(source, unit, index, Identifier.getInvokeConstantId(unit, source, index));
			} else if(unit instanceof DefinitionStmt) {
				if(localUses.isEmpty()) {
					if(((Stmt)unit).containsInvokeExpr())
						ret = new InvokeLeafNode(source, unit, target, Identifier.getInvokeExprId(unit, source, target, keepInstanceObject));
					else if(((Stmt)unit).containsFieldRef())
						ret = new FieldLeafNode(source, unit, field, Identifier.getFieldId(unit, source, field, keepInstanceObject));
					else
						ret = new LeafNode(source, unit, Identifier.getUnitId(unit, source, keepInstanceObject));
				} else {
					if(((Stmt)unit).containsInvokeExpr())
						ret = new InvokeNode(source, unit, target, Identifier.getInvokeExprId(unit, source, target, keepInstanceObject));
					else if(((Stmt)unit).containsFieldRef())
						ret = new FieldNode(source, unit, field, Identifier.getFieldId(unit, source, field, keepInstanceObject));
					else
						ret = new Node(source, unit, Identifier.getUnitId(unit, source, keepInstanceObject));
				}
			} else {
				throw new RuntimeException("Error: Unhandled unit '" + unit + "' of " + source + "'.");
			}
			
			synchronized(existingNodes) {
				INode r = existingNodes.get(ret);
				if(r != null)
					ret = r;
				else
					existingNodes.put(ret, ret);
			}
			return ret;
		}
		
		public static final InlineConstantLeafNode modifyInlineConstantLeafNode(INode cur, int index) {
			Objects.requireNonNull(cur);
			if(existingNodes == null)
				throw new RuntimeException("Error: The existing nodes list has not been initilized!");
			
			InlineConstantLeafNode ret = new InlineConstantLeafNode(cur, index);
			synchronized(nodeToCopy) {
				InlineConstantLeafNode r = (InlineConstantLeafNode)nodeToCopy.get(ret);
				if(r != null)
					ret = r;
				else
					nodeToCopy.put(ret, ret);
			}
			return ret;
		}
		
		//LocalWrapper init must be called before the use of this method
		public static INode modifyNode(INode in, boolean keepInstanceObject) {
			Objects.requireNonNull(in);
			if(nodeToCopy == null)
				throw new RuntimeException("Error: The existing copies list has not been initilized!");
			
			INode out;
			synchronized(nodeToCopy) {
				out = nodeToCopy.get(in);
			}
			if(out == null) {
				Unit unit = in.getUnit();
				SootMethod source = in.getSource();
				Set<Local> localUses = getLocalUses(unit, keepInstanceObject);
				SootMethod target = null;
				SootField field = null;
				if(in instanceof IInvokeNode)
					target = ((IInvokeNode)in).getTarget();
				if(in instanceof IFieldNode)
					field = ((IFieldNode)in).getField();
				
				if(in instanceof InvokeStartNode) {
					out = new InvokeStartNode(source, unit, target, Identifier.getInvokeExprId(unit, source, target, keepInstanceObject));
				} else if(in instanceof FieldStartNode) {
					out = new FieldStartNode(source, unit, field, Identifier.getFieldId(unit, source, field, keepInstanceObject));
				} else if(in instanceof StartNode) {
					out = new StartNode(source, unit, Identifier.getUnitId(unit, source, keepInstanceObject));
				} else if(in instanceof InvokeLeafNode) {
					out = new InvokeLeafNode(source, unit, target, Identifier.getInvokeExprId(unit, source, target, keepInstanceObject));
				} else if(in instanceof FieldLeafNode) {
					out = new FieldLeafNode(source, unit, field, Identifier.getFieldId(unit, source, field, keepInstanceObject));
				} else if(in instanceof InvokeConstantLeafNode) {
					int index = ((InvokeConstantLeafNode)in).getIndex();
					out = new InvokeConstantLeafNode(source, unit, index, Identifier.getInvokeConstantId(unit, source, index));
				} else if(in instanceof LeafNode) {
					out = new LeafNode(source, unit, Identifier.getUnitId(unit, source, keepInstanceObject));
				} else if(in instanceof InvokeNode) {
					if(localUses.isEmpty()) //If the removal of the instance object causes us to have no locals then this is now a leaf
						out = new InvokeLeafNode(source, unit, target, Identifier.getInvokeExprId(unit, source, target, keepInstanceObject));
					else
						out = new InvokeNode(source, unit, target, Identifier.getInvokeExprId(unit, source, target, keepInstanceObject));
				} else if(in instanceof FieldNode) {
					if(localUses.isEmpty()) //If the removal of the instance object causes us to have no locals then this is now a leaf
						out = new FieldLeafNode(source, unit, field, Identifier.getFieldId(unit, source, field, keepInstanceObject));
					else
						out = new FieldNode(source, unit, field, Identifier.getFieldId(unit, source, field, keepInstanceObject));
				} else if(in instanceof Node) {
					out = new Node(source, unit, Identifier.getUnitId(unit, source, keepInstanceObject));
				} else {
					throw new RuntimeException("Error: Unhandled node type '" + in.getClass().getSimpleName() + "'.");
				}
				
				synchronized(nodeToCopy) {
					INode r = nodeToCopy.get(in);
					if(r != null)
						out = r;
					else
						nodeToCopy.put(in, out);
				}
			}
			return out;
		}
		
		//Left array refs which show up as uses should not appear here as we are finding the defs of locals
		//which in the case when an array is used on the right will be the def of the index and the array itself
		private static Set<Local> getLocalUses(Unit unit, boolean keepInstanceObject) {
			Value instanceObject = null;
			if(!keepInstanceObject && ((Stmt)unit).containsInvokeExpr()) {
				InvokeExpr ie = ((Stmt)unit).getInvokeExpr();
				if(ie instanceof InstanceInvokeExpr)
					instanceObject = ((InstanceInvokeExpr)ie).getBase();
			} else if(!keepInstanceObject && ((Stmt)unit).containsFieldRef()) {
				FieldRef fr = ((Stmt)unit).getFieldRef();
				if(fr instanceof InstanceFieldRef)
					instanceObject = ((InstanceFieldRef)fr).getBase();
			}
			Set<Local> ret = new HashSet<>();
			for(ValueBox vb : unit.getUseBoxes()) {
				Value v = vb.getValue();
				if(v instanceof Local && (instanceObject == null || !v.equals(instanceObject)))
					ret.add((Local)v);
			}
			return ret;
		}
		
		private static final XStreamSetup xstreamSetup = new XStreamSetup();

		public static XStreamSetup getXStreamSetupStatic(){
			return xstreamSetup;
		}
		
		public static class XStreamSetup extends AbstractXStreamSetup {
			
			@Override
			public void getOutputGraph(LinkedHashSet<AbstractXStreamSetup> in) {
				if(!in.contains(this)) {
					in.add(this);
					SootUnitContainer.getXStreamSetupStatic().getOutputGraph(in);
					SootMethodContainer.getXStreamSetupStatic().getOutputGraph(in);
					SootFieldContainer.getXStreamSetupStatic().getOutputGraph(in);
					Identifier.getXStreamSetupStatic().getOutputGraph(in);
				}
			}

			@Override
			public Set<Class<?>> getAnnotatedClasses() {
				Set<Class<?>> ret = new HashSet<>();
				ret.add(AbstractNode.class);
				ret.add(LeafNode.class);
				ret.add(StartNode.class);
				ret.add(Node.class);
				ret.add(InvokeConstantLeafNode.class);
				ret.add(InvokeLeafNode.class);
				ret.add(InvokeNode.class);
				ret.add(FieldNode.class);
				ret.add(FieldLeafNode.class);
				ret.add(FieldStartNode.class);
				ret.add(InvokeStartNode.class);
				ret.add(InlineConstantLeafNode.class);
				ret.add(INode.class);
				ret.add(IInvokeNode.class);
				ret.add(IFieldNode.class);
				return ret;
			}
			
			@Override
			public void setXStreamOptions(XStream xstream) {
				defaultOptionsXPathRelRef(xstream);
			}
			
		}
	}
	
}
