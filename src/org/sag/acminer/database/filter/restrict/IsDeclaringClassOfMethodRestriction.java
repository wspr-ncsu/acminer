package org.sag.acminer.database.filter.restrict;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.sag.acminer.database.defusegraph.DefUseGraph;
import org.sag.acminer.database.defusegraph.IFieldNode;
import org.sag.acminer.database.defusegraph.IInvokeNode;
import org.sag.acminer.database.defusegraph.INode;
import org.sag.acminer.database.defusegraph.LocalWrapper;
import org.sag.acminer.database.defusegraph.StartNode;
import org.sag.acminer.database.filter.entry.IEntry;
import org.sag.common.tools.HierarchyHelpers;

import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.jimple.Stmt;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("IsDeclaringClassOfMethod")
public class IsDeclaringClassOfMethodRestriction implements IRestriction {
	
	public static final String name = "IsDeclaringClassOfMethod";
	
	@XStreamAlias("DeclaringClass")
	@XStreamAsAttribute
	private final String declaringClass;
	@XStreamAlias("IncludeSubClasses")
	@XStreamAsAttribute
	private final boolean includeSubClasses;
	@XStreamOmitField
	private volatile Set<String> classes;
	
	public IsDeclaringClassOfMethodRestriction(String declaringClass, boolean includeSubClasses) {
		this.declaringClass = declaringClass;
		this.includeSubClasses = includeSubClasses;
		this.classes = null;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	private synchronized void loadSootData() {
		if(classes == null) {
			SootClass start = Scene.v().getSootClassUnsafe(declaringClass);
			if(start == null) {
				classes = Collections.emptySet();
			} else {
				classes = new HashSet<>();
				classes.add(start.toString());
				if(includeSubClasses) {
					Set<SootClass> r;
					if(start.isInterface()) {
						r = HierarchyHelpers.getAllSubClassesOfInterface(start);
						r.addAll(Scene.v().getFastHierarchy().getAllSubinterfaces(start));
					} else {
						r = HierarchyHelpers.getAllSubClasses(start);
					}
					for(SootClass sc : r) {
						classes.add(sc.toString());
					}
				}
			}
		}
	}

	@Override
	public Set<INode> applyRestriction(StartNode sn, LocalWrapper lw, DefUseGraph vt, Set<INode> in, StringBuilder sb, 
			AtomicInteger c, Object... objects) {
		int curC = 0;
		if(sb != null) {
			curC = c.get();
			sb.append("Start Restriction ").append(name).append(" ").append(curC).append(" DeclaringClass=").append(declaringClass)
				.append(" IncludeSubClasses=").append(includeSubClasses).append("\n");
			sb.append("  Incomming Nodes:\n");
			for(INode n : in) {
				sb.append("    ").append(n).append("\n");
			}
		}
		if(classes == null)
			loadSootData();
		Set<INode> ret = new HashSet<>();
		for(INode i : in) {
			if(i instanceof IFieldNode) {
				SootField field = ((IFieldNode)i).getField();
				if(field == null) {
					if(classes.contains(((Stmt)(((IFieldNode)i).getUnit())).getFieldRef().getFieldRef().declaringClass().toString()))
						ret.add(i);
				} else {
					if(classes.contains(field.getDeclaringClass().toString()))
						ret.add(i);
				}
			} else if(i instanceof IInvokeNode) {
				SootMethod method = ((IInvokeNode)i).getTarget();
				if(method == null) {
					if(classes.contains(((Stmt)(((IInvokeNode)i).getUnit())).getInvokeExpr().getMethodRef().declaringClass().toString()))
						ret.add(i);
				} else {
					if(classes.contains(method.getDeclaringClass().toString()))
						ret.add(i);
				}
			}
		}
		if(ret.isEmpty())
			ret = Collections.emptySet();
		
		if(sb != null) {
			sb.append("End Restriction ").append(name).append(" ").append(curC).append(" DeclaringClass=").append(declaringClass)
				.append(" IncludeSubClasses=").append(includeSubClasses).append("\n");
			sb.append("  Outgoing Nodes:\n");
			for(INode n : ret) {
				sb.append("    ").append(n).append("\n");
			}
		}
		return ret;
	}
	
	@Override
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof IsDeclaringClassOfMethodRestriction))
			return false;
		return Objects.equals(((IsDeclaringClassOfMethodRestriction)o).declaringClass,declaringClass) 
				&& ((IsDeclaringClassOfMethodRestriction)o).includeSubClasses == includeSubClasses;
	}
	
	public int hashCode() {
		int i = 17;
		i = i * 31 + Objects.hashCode(declaringClass);
		i = i * 31 + (includeSubClasses ? 1 : 0);
		return i;
	}
	
	@Override
	public String toString() {
		List<String> r = new ArrayList<>();
		r.add("DeclaringClass="+declaringClass);
		r.add("IncludeSubClasses="+Boolean.toString(includeSubClasses));
		return IEntry.Factory.genSig(name, r);
	}

}
