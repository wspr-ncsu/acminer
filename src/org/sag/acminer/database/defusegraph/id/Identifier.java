package org.sag.acminer.database.defusegraph.id;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.sag.acminer.database.defusegraph.InvokeConstantLeafNode;
import org.sag.acminer.database.defusegraph.LocalWrapper;
import org.sag.acminer.phases.acminer.dw.DataWrapper;
import org.sag.xstream.XStreamInOut.XStreamInOutInterface.AbstractXStreamSetup;
import org.sag.xstream.xstreamconverters.NamedCollectionConverterWithSize;
import soot.Local;
import soot.Scene;
import soot.SootField;
import soot.SootFieldRef;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.ConditionExpr;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.IdentityRef;
import soot.jimple.IfStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.SwitchStmt;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("Identifier")
public class Identifier implements List<Part> {
	
	@XStreamAlias("Parts")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"Part"},types={Part.class})
	private ArrayList<Part> parts;
	
	@XStreamOmitField
	private boolean inited;
	
	protected Identifier() { this.inited = false; }
	
	private Identifier(List<Part> parts) {
		this.parts = new ArrayList<>(parts);
		this.inited = true;
	}
	
	public int hashCode() {
		return toString().hashCode();
	}
	
	public boolean equals(Object o) {
		if(o == this) 
			return true;
		if(o == null || !(o instanceof Identifier))
			return false;
		Identifier id = (Identifier)o;
		return this.toString().equals(id.toString());
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(Part p : parts) {
			sb.append(p.toString());
		}
		return sb.toString();
	}
	
	public Identifier clone() {
		List<Part> list = new ArrayList<>();
		for(Part p : parts) {
			list.add(p.clone());
		}
		return new Identifier(list);
	}
	
	@Override
	public Iterator<Part> iterator() {
		return parts.iterator();
	}
	
	public int size() {
		return parts.size();
	}
	
	@Override
	public boolean isEmpty() {
		return parts.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return parts.contains(o);
	}

	@Override
	public Object[] toArray() {
		return parts.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return parts.toArray(a);
	}

	@Override
	public boolean add(Part e) {
		return parts.add(e);
	}

	@Override
	public boolean remove(Object o) {
		return parts.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return parts.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends Part> c) {
		return parts.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends Part> c) {
		return parts.addAll(index, c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return parts.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return parts.retainAll(c);
	}

	@Override
	public void clear() {
		parts.clear();
	}

	@Override
	public Part get(int index) {
		return parts.get(index);
	}

	@Override
	public Part set(int index, Part element) {
		return parts.set(index, element);
	}

	@Override
	public void add(int index, Part element) {
		parts.add(index, element);
	}

	@Override
	public Part remove(int index) {
		return parts.remove(index);
	}

	@Override
	public int indexOf(Object o) {
		return parts.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return parts.lastIndexOf(o);
	}

	@Override
	public ListIterator<Part> listIterator() {
		return parts.listIterator();
	}

	@Override
	public ListIterator<Part> listIterator(int index) {
		return parts.listIterator(index);
	}

	@Override
	public List<Part> subList(int fromIndex, int toIndex) {
		return parts.subList(fromIndex, toIndex);
	}
	
	public void initSootComponents(Unit u) {
		if(!inited) {
			List<Value> uses = getOrderedUseValues(u);
			for(Part p : parts) {
				//Add the use index and set any additional values
				boolean success = true;
				if(p instanceof ValuePart) {
					Value v = uses.get(((ValuePart)p).getIndex());
					if(p instanceof MethodRefPart) {
						MethodRefPart part = (MethodRefPart)p;
						if(v instanceof InvokeExpr) {
							InvokeExpr ir = (InvokeExpr)v;
							SootMethodRef r = ir.getMethodRef();
							if(r.getSignature().equals(part.getOrgString())) {
								part.setMethodRef(r);
								part.setValue(ir);
							} else {
								success = false;
							}
						} else {
							success = false;
						}
					} else if(p instanceof FieldRefPart) {
						FieldRefPart part = (FieldRefPart)p;
						if(v instanceof FieldRef) {
							FieldRef fr = (FieldRef)v;
							SootFieldRef r = fr.getFieldRef();
							if(r.getSignature().equals(part.getOrgString())) {
								part.setFieldRef(r);
								part.setValue(fr);
							} else {
								success = false;
							}
						} else {
							success = false;
						}
					} else if(p instanceof ConstantPart) {
						ConstantPart part = (ConstantPart)p;
						if(v instanceof Constant) {
							if(part.getOrgString().equals(v.toString()))
								part.setValue((Constant)v);
							else
								success = false;
						} else {
							success = false;
						}
					} else if(p instanceof IdentityRefPart) {
						IdentityRefPart part = (IdentityRefPart)p;
						if(v instanceof IdentityRef) {
							if(part.getOrgString().equals(v.toString()))
								part.setValue((IdentityRef)v);
							else
								success = false;
						} else {
							success = false;
						}
					} else if(p instanceof LocalPart) {
						LocalPart part = (LocalPart)p;
						if(v instanceof Local) {
							if(part.getOrgString().equals(v.toString()))
								part.setValue((Local)v);
							else
								success = false;
						} else {
							success = false;
						}
					}
					if(!success)
						throw new RuntimeException("Error: Failed to successfully resolve soot components for part '" 
								+ p.toString() + "' org string '" + p.getOrgString() + " and value at it's index '" + v.toString() + "'.");
				} else if(p instanceof TypePart) {
					TypePart part = (TypePart)p;
					part.setType(Scene.v().getType(part.getOrgString()));
				}
			}
			this.toString();//Init the string cache
			inited = true;
		}
	}
	
	public static Identifier getInvokeExprId(Unit unit, SootMethod source, SootMethod target, boolean keepInstanceObject) {
		Stmt stmt = (Stmt)unit;
		if(stmt.containsInvokeExpr() && target != null) {
			IdentifierGenerator gen = IdentifierGenerator.getGenerator(source);
			InvokeExpr ie = stmt.getInvokeExpr();
			if(keepInstanceObject && ie instanceof InstanceInvokeExpr) {
				((InstanceInvokeExpr)ie).getBase().toString(gen);
				gen.literal(".");
			}
			gen.methodRefResolved(target, ie.getMethodRef());
			gen.literal("(");
			boolean first = true;
			for(Value v : ie.getArgs()) {
				if(!first)
					gen.literal(", ");
				else
					first = false;
				v.toString(gen);
			}
			gen.literal(")");
			return new Identifier(finalizeParts(unit, source, gen.getParts()));
		} else {
			return getUnitId(unit, source, keepInstanceObject);
		}
	}
	
	public static Identifier getFieldId(Unit unit, SootMethod source, SootField field, boolean keepInstanceObject) {
		Stmt stmt = (Stmt)unit;
		if(stmt.containsFieldRef() && field != null) {
			IdentifierGenerator gen = IdentifierGenerator.getGenerator(source);
			FieldRef fr = stmt.getFieldRef();
			if(keepInstanceObject && fr instanceof InstanceFieldRef) {
				((InstanceFieldRef)fr).getBase().toString(gen);
				gen.literal(".");
			}
			gen.fieldRefResolved(field, fr.getFieldRef());
			return new Identifier(finalizeParts(unit, source, gen.getParts()));
		} else {
			return getUnitId(unit, source, keepInstanceObject);
		}
	}
	
	public static Identifier getUnitId(Unit unit, SootMethod source, boolean keepInstanceObject) {
		Stmt stmt = (Stmt)unit;
		IdentifierGenerator gen = IdentifierGenerator.getGenerator(source);
		if(stmt.containsInvokeExpr()) {
			InvokeExpr ie = stmt.getInvokeExpr();
			if(keepInstanceObject && ie instanceof InstanceInvokeExpr) {
				((InstanceInvokeExpr)ie).getBase().toString(gen);
				gen.literal(".");
			}
			gen.methodRef(ie.getMethodRef());
			gen.literal("(");
			boolean first = true;
			for(Value v : ie.getArgs()) {
				if(!first)
					gen.literal(", ");
				else
					first = false;
				v.toString(gen);
			}
			gen.literal(")");
		} else if(stmt.containsFieldRef()) {
			FieldRef fr = stmt.getFieldRef();
			if(keepInstanceObject && fr instanceof InstanceFieldRef) {
				((InstanceFieldRef)fr).getBase().toString(gen);
				gen.literal(".");
			}
			gen.fieldRef(fr.getFieldRef());
		} else if(stmt instanceof IfStmt) {
			ConditionExpr cond = (ConditionExpr)((IfStmt)stmt).getCondition();
			gen.literal("if(");
			cond.getOp1().toString(gen);
			gen.literal(" ");
			gen.literal(cond.getSymbol());
			gen.literal(" ");
			cond.getOp2().toString(gen);
			gen.literal(")");
		} else if(stmt instanceof SwitchStmt){
			gen.literal("switch(");
			((SwitchStmt)stmt).getKey().toString(gen);
			gen.literal(")");
		} else {
			if(stmt instanceof DefinitionStmt) {
				((DefinitionStmt)stmt).getRightOp().toString(gen);
			} else {
				stmt.toString(gen);
			}
		}
		return new Identifier(finalizeParts(unit, source, gen.getParts()));
	}
	
	public static Identifier getInvokeConstantId(Unit unit, SootMethod source, int index) {
		IdentifierGenerator gen = IdentifierGenerator.getGenerator(source);
		Constant c = InvokeConstantLeafNode.valueFromUnit(source, unit, index);
		gen.constant(c);
		return new Identifier(finalizeParts(unit, source, gen.getParts()));
	}
	
	public static Identifier getValueId(Value v, SootMethod source, Unit unit) {
		IdentifierGenerator gen = IdentifierGenerator.getGenerator(source);
		v.toString(gen);
		return new Identifier(finalizeParts(unit, source, gen.getParts()));
	}
	
	public static Identifier getLiteralId(String val) {
		return new Identifier(Collections.<Part>singletonList(new LiteralPart(val)));
	}
	
	/** Combines two identifiers using the given string as a separator. Note after this
	 * is run the given indexes for the ValuePart will not really mean anything as they
	 * are for a specific unit and this combines two units essentially. It should only
	 * be used where these indexes don't matter anymore.
	 */
	public static Identifier combineIds(Identifier id1, Identifier id2, String sep) {
		ArrayList<Part> temp = new ArrayList<>();
		temp.addAll(id1);
		temp.add(new LiteralPart(sep));
		temp.addAll(id2);
		return new Identifier(temp);
	}
	
	public static Identifier getDataWrapperId(DataWrapper in) {
		return new Identifier(Collections.<Part>singletonList(new DataWrapperPart(in)));
	}
	
	public static Identifier getPrimitiveConstantId(Number n) {
		return new Identifier(Collections.<Part>singletonList(new PrimitiveConstantPart(n, false, false)));
	}
	
	public static Identifier getBooleanPrimitiveConstantId(Number n) {
		return new Identifier(Collections.<Part>singletonList(new PrimitiveConstantPart(n, true, false)));
	}
	
	public static Identifier getCharacterPrimitiveConstantId(Number n) {
		return new Identifier(Collections.<Part>singletonList(new PrimitiveConstantPart(n, false, true)));
	}
	
	public static Identifier getStringConstantId(String in) {
		return new Identifier(Collections.<Part>singletonList(new StringConstantPart(in)));
	}
	
	public static Identifier getPlaceholderConstantId(String in) {
		return new Identifier(Collections.<Part>singletonList(new PlaceholderConstantPart(in)));
	}
	
	public static Identifier getUnknownConstantId(String in) {
		return new Identifier(Collections.<Part>singletonList(new UnknownConstantPart(in)));
	}
	
	private static List<Part> finalizeParts(Unit u, SootMethod source, List<Part> parts) {
		List<Value> uses = getOrderedUseValues(u);
		for(Part p : parts) {
			//Add the use index and set any additional values
			if(p instanceof ValuePart) {
				boolean found = false;
				if(p instanceof MethodRefPart) {
					MethodRefPart mp = ((MethodRefPart)p);
					for(int i = 0; i < uses.size(); i++) {
						Value v = uses.get(i);
						if(v instanceof InvokeExpr && mp.getMethodRef().equals(((InvokeExpr)v).getMethodRef())) {
							mp.setIndex(i);
							mp.setValue((InvokeExpr)v);
							found = true;
							break;
						}
					}
				} else if(p instanceof FieldRefPart) {
					FieldRefPart fp = ((FieldRefPart)p);
					for(int i = 0; i < uses.size(); i++) {
						Value v = uses.get(i);
						if(v instanceof FieldRef && fp.getFieldRef().equals(((FieldRef)v).getFieldRef())) {
							fp.setIndex(i);
							fp.setValue((FieldRef)v);
							found = true;
							break;
						}
					}
				} else {
					Value cur = ((ValuePart)p).getValue();
					for(int i = 0; i < uses.size(); i++) {
						if(cur.equals(uses.get(i))) {
							found = true;
							((ValuePart)p).setIndex(i);
							break;
						}
					}
				}
				
				if(!found)
					throw new RuntimeException("Error: Unable to find index for value part '" + p.toString() + "' a '" 
							+ p.getClass().getSimpleName() + "'.");
			} else if(p instanceof LiteralPart) {
				p.toString();//force the buffer into the string value
			}
		}
		
		//Transform all local parts into local wrapper parts
		for(int i = 0; i < parts.size(); i++) {
			Part p = parts.get(i);
			if(p instanceof LocalPart) {
				LocalWrapper lw = LocalWrapper.Factory.get(((LocalPart)p).getValue(), source);
				LocalWrapperPart part = new LocalWrapperPart(lw, lw.toString());
				part.setIndex(((LocalPart)p).getIndex());
				parts.set(i, part);
			}
		}
		
		return parts;
	}
	
	private static List<Value> getOrderedUseValues(Unit u) {
		List<Value> list = new ArrayList<>();
		Set<Value> seen = new HashSet<>();
		for(ValueBox vb : u.getUseBoxes()) {
			Value v = vb.getValue();
			if(seen.add(v))
				list.add(v);
		}
		Collections.sort(list, new  Comparator<Value>() {
			@Override
			public int compare(Value o1, Value o2) {
				int r = o1.getClass().toString().compareTo(o2.getClass().toString());
				if(r == 0)
					r = o1.toString().compareTo(o2.toString());
				return r;
			}
		});
		return list;
	}
	
	private static final XStreamSetup xstreamSetup = new XStreamSetup();

	public static AbstractXStreamSetup getXStreamSetupStatic(){
		return xstreamSetup;
	}
	
	public static class XStreamSetup extends AbstractXStreamSetup {
		
		@Override
		public void getOutputGraph(LinkedHashSet<AbstractXStreamSetup> in) {
			if(!in.contains(this)) {
				in.add(this);
				LocalWrapper.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			Set<Class<?>> ret = new HashSet<>();
			ret.add(ConstantPart.class);
			ret.add(FieldRefPart.class);
			ret.add(IdentityRefPart.class);
			ret.add(LiteralPart.class);
			ret.add(LocalPart.class);
			ret.add(LocalWrapperPart.class);
			ret.add(MethodRefPart.class);
			ret.add(Part.class);
			ret.add(TypePart.class);
			ret.add(ValuePart.class);
			ret.add(Identifier.class);
			return ret;
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsXPathRelRef(xstream);
		}
		
	}

}
