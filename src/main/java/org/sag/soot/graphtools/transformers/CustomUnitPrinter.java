package org.sag.soot.graphtools.transformers;

import soot.AttributesUnitPrinter;
import soot.Body;
import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.SootFieldRef;
import soot.SootMethodRef;
import soot.Type;
import soot.Unit;
import soot.UnitBox;
import soot.UnitPrinter;
import soot.ValueBox;
import soot.jimple.*;
import soot.util.*;

import java.util.*;

/**
* Partial default UnitPrinter implementation.
*/
public class CustomUnitPrinter implements UnitPrinter {

	private StringBuffer output = new StringBuffer();
	private AttributesUnitPrinter pt;
	private HashSet<String> quotableLocals;

	/** branch targets **/
	private Map<Unit, String> labels;
	/** for unit references in Phi nodes **/
	private Map<Unit, String> references;
	
	public CustomUnitPrinter(Body b){
		createLabelMaps(b);
	}

	public void setPositionTagger( AttributesUnitPrinter pt ) {
		this.pt = pt;
		pt.setUnitPrinter( this );
	}

	public AttributesUnitPrinter getPositionTagger() {
		return pt;
	}

	public void startUnit( Unit u ) {
		handleIndent();
		if( pt != null ) pt.startUnit( u );
	}

	public void endUnit( Unit u ) {
		if( pt != null ) pt.endUnit( u );
	}

	public void startUnitBox( UnitBox ub ) {
		handleIndent();
	}

	public void endUnitBox( UnitBox ub ) {}

	public void startValueBox( ValueBox vb ) {
		handleIndent();
		if( pt != null ) pt.startValueBox( vb );
	}

	public void endValueBox( ValueBox vb ) {
		if( pt != null ) pt.endValueBox( vb );
	}

	public void noIndent() {
	}

	public void incIndent() {
	}

	public void decIndent() {
	}

	public void setIndent(String indent) {
	}

	public String getIndent() {
		return "";
	}

	public void newline() {
	}

	public void local( Local l ) {
		handleIndent();
		if( quotableLocals == null )
			initializeQuotableLocals();
		if( quotableLocals.contains(l.getName()) )
			output.append ( "'" + l.getName() + "'");
		else
			output.append( l.getName() );
	}

	public void constant( Constant c ) {
		handleIndent();
		output.append( c.toString() );
	}

	public String toString() {
		String ret = output.toString();
		output = new StringBuffer();
		return ret;
	}

	public StringBuffer output() {
		return output;
	}

	protected void handleIndent() {
	}

	protected void initializeQuotableLocals() {
		quotableLocals = new HashSet<String>();
		quotableLocals.addAll (Jimple.jimpleKeywordList());
	}

	public Map<Unit, String> labels() {
		return labels;
	}

	public Map<Unit, String> references() {
		return references;
	}

	public void unitRef(Unit u, boolean branchTarget){
		if(branchTarget){
			String label = labels.get(u);
			if(label == null || "<unnamed>".equals(label)){
				label = "";
			}
			output.append(label);
		}else{
			String ref = references.get(u);
			output.append(ref);
		}
	}

	private void createLabelMaps(Body body) {
		Chain<Unit> units = body.getUnits();

		labels = new HashMap<Unit, String>(units.size() * 2 + 1, 0.7f);
		references = new HashMap<Unit, String>(units.size() * 2 + 1, 0.7f);

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
	}

	public void type( Type t ) {
		handleIndent();
		String s = t==null ? "<null>"  : t.toString();
		if( t instanceof RefType ) s = Scene.v().quotedNameOf(s);
		output.append( s );
	}
	public void methodRef( SootMethodRef m ) {
		handleIndent();
		output.append( m.getSignature() );
	}
	public void fieldRef( SootFieldRef f ) {
		handleIndent();
		output.append(f.getSignature());
	}
	public void identityRef( IdentityRef r ) {
		handleIndent();
		if( r instanceof ThisRef ) {
			literal("@this: ");
			type(r.getType());
		} else if( r instanceof ParameterRef ) {
			ParameterRef pr = (ParameterRef) r;
			literal("@parameter"+pr.getIndex()+": ");
			type(r.getType());
		} else if( r instanceof CaughtExceptionRef ) {
			literal("@caughtexception");
		} else throw new RuntimeException();
	}
	public void literal( String s ) {
		handleIndent();
		output.append( s );
	}
}

