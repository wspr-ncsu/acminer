package org.sag.acminer.database.filter.restrict;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.sag.acminer.database.defusegraph.DefUseGraph;
import org.sag.acminer.database.defusegraph.IInvokeNode;
import org.sag.acminer.database.defusegraph.INode;
import org.sag.acminer.database.defusegraph.LocalWrapper;
import org.sag.acminer.database.defusegraph.StartNode;
import org.sag.acminer.database.filter.entry.IEntry;
import org.sag.acminer.database.filter.matcher.IMatcher;

import soot.Local;
import soot.Value;
import soot.jimple.Constant;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

@XStreamAlias("IsValueUsedInMethodCall")
public class IsValueUsedInMethodCallRestriction implements IRestriction {
	
	public static final String name = "IsValueUsedInMethodCall";
	
	@XStreamAlias("Position")
	@XStreamAsAttribute
	private final int pos;
	
	@XStreamAlias("Matcher")
	private final IMatcher matcher;
	
	@XStreamAlias("Restrictions")
	private final Restrictions restrictions;
	
	public IsValueUsedInMethodCallRestriction(int pos, IMatcher matcher) {
		this(pos, matcher, (Restrictions)null);
	}
	
	public IsValueUsedInMethodCallRestriction(int pos, IMatcher matcher, IRestriction... res) {
		this(pos, matcher, (res == null || res.length == 0) ? null : new Restrictions(res));
	}
	
	public IsValueUsedInMethodCallRestriction(int pos, IMatcher matcher, Restrictions res) {
		Objects.requireNonNull(matcher);
		if(pos < -1) throw new IllegalArgumentException();
		this.pos = pos;
		this.matcher = matcher;
		this.restrictions = res;
	}
	
	public String getName() {
		return name;
	}

	@Override
	public Set<INode> applyRestriction(StartNode sn, LocalWrapper lw, DefUseGraph vt, Set<INode> in, StringBuilder sb, 
			AtomicInteger cc, Object... objects) {
		
		int curC = 0;
		if(sb != null) {
			curC = cc.get();
			sb.append("Start Restriction ").append(name).append(" ").append(curC).append(" Position=").append(pos).append(" Matcher=").append(matcher.toString()).append("\n");
			sb.append("  Incomming Nodes:\n");
			for(INode n : in) {
				sb.append("    ").append(n).append("\n");
			}
		}
		
		Objects.requireNonNull(vt);
		Objects.requireNonNull(in);
		Set<INode> ret = new HashSet<>();
		for(INode vn : in) {
			if(vn instanceof IInvokeNode) {
				Set<INode> defs = null;
				Constant c = null;
				InvokeExpr ir = ((Stmt)vn.getUnit()).getInvokeExpr();
				if(pos >= 0) {
					List<Value> args = ir.getArgs();
					if(args.size() > 0 && pos < args.size()) {
						Value v = args.get(pos);
						if(v instanceof Local) {
							Set<INode> nodes = vt.getChildLocalsToChildNodes(vn).get(v);
							if(nodes == null) {
								defs = Collections.emptySet(); 
							} else {
								//Lookup the defs of a array local if an array ref is used in a method
								defs = new HashSet<>();
								for(INode d : nodes) {
									if(((Stmt)d.getUnit()).containsArrayRef()) {
										defs.addAll(vt.getChildLocalsToChildNodes(d).get(((Stmt)d.getUnit()).getArrayRef().getBase()));
									} else {
										defs.add(d);
									}
								}
							}
						} else {//In-line constant
							c = (Constant)v;
						}
					} else {
						//The position '" + pos +"' is not valid for arguments of size '" + args.size()
						continue;
					}
				} else {
					if(ir instanceof InstanceInvokeExpr) {
						Value v = ((InstanceInvokeExpr)ir).getBase();
						if(v instanceof Local) {
							Set<INode> nodes = vt.getChildLocalsToChildNodes(vn).get(v);
							if(nodes == null) {
								defs = Collections.emptySet(); 
							} else {
								//Lookup the defs of a array local if an array ref is used in a method
								defs = new HashSet<>();
								for(INode d : nodes) {
									if(((Stmt)d.getUnit()).containsArrayRef()) {
										defs.addAll(vt.getChildLocalsToChildNodes(d).get(((Stmt)d.getUnit()).getArrayRef().getBase()));
									} else {
										defs.add(d);
									}
								}
							}
						} else {//In-line constant
							c = (Constant)v;
						}
					} else {
						//Restriction is applied to the instance object for an non-instance invoke.
						continue;
					}
				}
				
				if(c != null) {
					//Restrictions don't apply here
					if(matcher.matcher(c))
						ret.add(vn);
				} else {
					Set<INode> newDefs = new HashSet<>();
					for(INode def : defs) {
						if(matcher.matcher(def)) {
							newDefs.add(def);
						}
					}
					if(!newDefs.isEmpty()) {
						if(restrictions == null)
							ret.add(vn);
						else {
							if(cc != null) cc.incrementAndGet();
							newDefs = restrictions.applyRestriction(sn, null, vt, newDefs, sb, cc, objects);
							if(!newDefs.isEmpty())
								ret.add(vn);
						}
					}
				}
			}
		}
		
		if(ret.isEmpty())
			ret = Collections.emptySet();
		
		if(sb != null) {
			sb.append("End Restriction ").append(name).append(" ").append(curC).append(" Position=").append(pos).append(" Matcher=").append(matcher.toString()).append("\n");
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
		if(o == null || !(o instanceof IsValueUsedInMethodCallRestriction))
			return false;
		return ((IsValueUsedInMethodCallRestriction)o).pos == pos && Objects.equals(((IsValueUsedInMethodCallRestriction)o).matcher,matcher) 
				&& Objects.equals(((IsValueUsedInMethodCallRestriction)o).restrictions,restrictions);
	}
	
	public int hashCode() {
		int i = 17;
		i = i * 31 + pos;
		i = i * 31 + Objects.hashCode(matcher);
		i = i * 31 + Objects.hashCode(restrictions);
		return i;
	}
	
	@Override
	public String toString() {
		List<String> r = new ArrayList<>();
		r.add("Position="+pos);
		r.add(matcher.toString());
		if(restrictions != null)
			r.add(restrictions.toString());
		return IEntry.Factory.genSig(name, r);
	}

}
