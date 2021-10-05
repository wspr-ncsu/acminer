package org.sag.acminer.database.filter.restrict;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.sag.acminer.database.defusegraph.DefUseGraph;
import org.sag.acminer.database.defusegraph.IFieldNode;
import org.sag.acminer.database.defusegraph.INode;
import org.sag.acminer.database.defusegraph.LocalWrapper;
import org.sag.acminer.database.defusegraph.StartNode;
import org.sag.acminer.database.filter.entry.IEntry;
import org.sag.acminer.database.filter.matcher.TypeMatcher;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import soot.SootField;
import soot.jimple.Stmt;

@XStreamAlias("IsFieldType")
public class IsFieldTypeRestriction extends TypeMatcher implements IRestriction {
	
	public static final String name = "IsFieldType";

	public IsFieldTypeRestriction(String value) {
		super(value);
	}
	
	public IsFieldTypeRestriction(Op<String> op) {
		super(op);
	}
	
	public IsFieldTypeRestriction(String value, StringMatcherOpType type) {
		super(value, type);
	}
	
	public IsFieldTypeRestriction(String value, StringMatcherOpType type, boolean ic) {
		super(value, type, ic);
	}
	
	public String getName() {
		return name;
	}

	@Override
	public Set<INode> applyRestriction(StartNode sn, LocalWrapper lw, DefUseGraph vt, Set<INode> in, StringBuilder sb, 
			AtomicInteger c, Object...objects) {
		int curC = 0;
		if(sb != null) {
			curC = c.get();
			sb.append("Start Restriction ").append(name).append(" ").append(curC).append(" ").append(genValueSig()).append("\n");
			sb.append("  Incomming Nodes:\n");
			for(INode n : in) {
				sb.append("    ").append(n).append("\n");
			}
		}
		Set<INode> ret = new HashSet<>();
		for(INode vn : in) {
			if(vn instanceof IFieldNode) {
				SootField field = ((IFieldNode)vn).getField();
				if(field == null) {
					if(matches(((Stmt)(vn.getUnit())).getFieldRef().getFieldRef().type()))
						ret.add(vn);
				} else {
					if(matches(field.getType()))
						ret.add(vn);
				}
			}
		}
		
		if(sb != null) {
			sb.append("End Restriction ").append(name).append(" ").append(curC).append(" ").append(genValueSig()).append("\n");
			sb.append("  Outgoing Nodes:\n");
			for(INode n : ret) {
				sb.append("    ").append(n).append("\n");
			}
		}
		return ret;
	}
	
	@Override
	public boolean equals(Object o) {
		if(super.equals(o)) {
			return o instanceof IsFieldTypeRestriction;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return IEntry.Factory.genSig(name, Collections.singletonList(genValueSig()));
	}

}
