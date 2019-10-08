package org.sag.acminer.database.defusegraph.id;

import heros.solver.IDESolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import soot.AttributesUnitPrinter;
import soot.Body;
import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.SootField;
import soot.SootFieldRef;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Type;
import soot.Unit;
import soot.UnitBox;
import soot.UnitPrinter;
import soot.ValueBox;
import soot.jimple.Constant;
import soot.jimple.IdentityRef;
import soot.jimple.Jimple;
import soot.jimple.NullConstant;
import soot.util.Chain;

public class IdentifierGenerator implements UnitPrinter {
	
	private final Set<String> quotableLocals; //keywords that need to be quoted
	private final Map<Unit, String> labels; // branch targets
	private final Map<Unit, String> references; // for unit references in Phi nodes
	private volatile List<Part> parts;
	
	public IdentifierGenerator(Body b){
		this.labels = new HashMap<>();
		this.references = new HashMap<>();
		this.quotableLocals = new HashSet<>();
		this.parts = new ArrayList<>();
		init(b);
	}
	
	/** Clones the cur printer. All fields are direct references to the fields in the 
	 * cur printer except parts. This is because every other fields is immutable except
	 * parts.
	 */
	public IdentifierGenerator(IdentifierGenerator cur) {
		this.labels = cur.labels;
		this.references = cur.references;
		this.quotableLocals = cur.quotableLocals;
		this.parts = new ArrayList<>(cur.parts);
	}
	
	private void init(Body body) {
		Chain<Unit> units = body.getUnits();

		// Create statement name table
		Set<Unit> labelStmts = new HashSet<Unit>();
		Set<Unit> refStmts = new HashSet<Unit>();

		// Build labelStmts and refStmts
		for (UnitBox box : body.getAllUnitBoxes() ) {
			Unit stmt = box.getUnit();

			if (box.isBranchTarget()) {
				labelStmts.add(stmt);
			}
			else {
				refStmts.add(stmt);
			}
		}

		// left side zero padding for all labels
		// this simplifies debugging the jimple code in simple editors, as it
		// avoids the situation where a label is the prefix of another label
		final int maxDigits = 1 + (int) Math.log10(labelStmts.size());
		final String formatString = "label%0" + maxDigits + "d";

		int labelCount = 0;
		int refCount = 0;

		// Traverse the stmts and assign a label if necessary
		for ( Unit s : units ) {
			if (labelStmts.contains(s))
				labels.put(s, String.format(formatString, ++labelCount));

			if (refStmts.contains(s))
				references.put(s, Integer.toString(refCount++));
		}
		
		quotableLocals.addAll(Jimple.jimpleKeywordList());
	}
	
	protected void handleIndent() {}
	public void noIndent() {}
	public void incIndent() {}
	public void decIndent() {}
	public void setIndent(String indent) {}
	public String getIndent() { return ""; }
	public void newline() {}
	public void setPositionTagger(AttributesUnitPrinter pt) {}
	public AttributesUnitPrinter getPositionTagger() { return null; }
	public void startUnit(Unit u) {}
	public void endUnit(Unit u) {}
	public void startUnitBox(UnitBox ub) {}
	public void endUnitBox(UnitBox ub) {}
	public void startValueBox(ValueBox vb) {}
	public void endValueBox(ValueBox vb) {}
	
	public String toString() {
		return getString(parts);
	}

	public StringBuffer output() {
		StringBuffer sb = new StringBuffer();
		for(Part p : parts) {
			sb.append(p.toString());
		}
		return sb;
	}
	
	public void literal(String s) {
		LiteralPart p;
		if(parts.isEmpty() || !(parts.get(parts.size()-1) instanceof LiteralPart)) {
			p = new LiteralPart();
			parts.add(p);
		} else {
			p = (LiteralPart)(parts.get(parts.size()-1));
		}
		p.getBuffer().append(s);
	}
	
	public void local(Local l) {
		parts.add(new LocalPart(l, l.toString()));
	}

	public void constant(Constant c) {
		if(c instanceof NullConstant)
			parts.add(new ConstantPart(c, "NULL"));
		else
			parts.add(new ConstantPart(c, c.toString()));
	}
	
	public void unitRef(Unit u, boolean branchTarget){
		String label;
		if(branchTarget) {
			label = labels.get(u);
			if(label == null || "<unnamed>".equals(label))
				label = "";
		} else {
			label = references.get(u);
		}
		literal(label);
	}
	
	public void type(Type t) {
		String s = t == null ? "<null>"  : t.toString();
		if(t instanceof RefType) 
			s = Scene.v().quotedNameOf(s);
		parts.add(new TypePart(t, s));
	}
	
	public void methodRef(SootMethodRef m) {
		parts.add(new MethodRefPart(m, m.getSignature()));
	}
	
	public void methodRefResolved(SootMethod target, SootMethodRef m) {
		parts.add(new MethodRefPart(m, target.getSignature()));
	}
	
	public void fieldRef(SootFieldRef f) {
		parts.add(new FieldRefPart(f, f.getSignature()));
	}
	
	public void fieldRefResolved(SootField target, SootFieldRef f) {
		parts.add(new FieldRefPart(f, target.getSignature()));
	}
	
	public void identityRef(IdentityRef r) {
		parts.add(new IdentityRefPart(r, r.toString()));
	}
	
	public List<Part> getParts() {
		List<Part> ret = new ArrayList<>(parts);
		parts = new ArrayList<>(0);
		return ret;
	}
	
	public static String getString(List<Part> parts) {
		StringBuilder sb = new StringBuilder();
		for(Part p : parts) {
			sb.append(p.toString());
		}
		return sb.toString();
	}
	
	private static LoadingCache<Body,IdentifierGenerator> cache = null;
	
	public static void resetCache() {
		cache = null;
	}
	
	public static IdentifierGenerator getGenerator(SootMethod m) {
		return getGenerator(m.retrieveActiveBody());
	}
	
	public static IdentifierGenerator getGenerator(Body b) {
		if(cache == null) {
			cache = IDESolver.DEFAULT_CACHE_BUILDER.build( new CacheLoader<Body,IdentifierGenerator>() {
				@Override
				public IdentifierGenerator load(Body body) throws Exception {
					return new IdentifierGenerator(body);
				}
			});
		}
		return new IdentifierGenerator(cache.getUnchecked(b));
	}

}
