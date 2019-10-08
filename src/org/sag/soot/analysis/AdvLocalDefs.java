package org.sag.soot.analysis;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import org.sag.common.tuple.Pair;

import soot.Local;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.CastExpr;
import soot.jimple.DefinitionStmt;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.LiveLocals;
import soot.toolkits.scalar.SmartLocalDefs;

public class AdvLocalDefs extends SmartLocalDefs {
	
	private static final int toRemoveLocal = 1;
	private static final int toRemoveCast = 2;
	private static final int toRemoveArray = 4;
	
	public AdvLocalDefs(UnitGraph g, LiveLocals l) {
		super(g,l);
	}

	/** Get all definition statements for a Local l as used in the Unit start.
	 * Note this takes into account the state of the Local as it is used in the
	 * starting Unit and will only return definition statements for the Local if
	 * the Local as defined in that statement gets used in the starting Unit. This
	 * also takes into account aliasing of the Local l. In other words, if a definition
	 * statement for l includes on the right hand side another Local or a CastExpr, then
	 * the this method will also include definition statements for the Local used on the
	 * right hand side of these Local/CastExpr definition statements. The aliasing 
	 * processing also takes into account instances where an alias is aliased and includes
	 * these definition statements as well. All aliasing definition statements are inclued
	 * in the return value.
	 * 
	 * @param l The Local we are searching for definitions of
	 * @param start The Unit where L is used for which we wish to find definition 
	 *              statements for its use
	 * @return All definition statements for Local l as it is used in Unit start
	 */
	public Set<DefinitionStmt> getDefsWithAliases(Local l, Unit start) {
		return workerMethod(l, start, false, 0);
	}
	
	/** The same as {@link #getDefsWithAliases(Local l, Unit start)} except this removes
	 * aliasing definition statements that are Local assignments.
	 */
	public Set<DefinitionStmt> getDefsWithAliasesRemoveLocal(Local l, Unit start) {
		return workerMethod(l, start, false, toRemoveLocal);
	}
	
	/** The same as {@link #getDefsWithAliases(Local l, Unit start)} except this removes
	 * aliasing definition statements that are CastExpr.
	 */
	public Set<DefinitionStmt> getDefsWithAliasesRemoveCast(Local l, Unit start) {
		return workerMethod(l, start, false, toRemoveCast);
	}
	
	/** The same as {@link #getDefsWithAliases(Local l, Unit start)} except this removes
	 * aliasing definition statements that are Local assignments or CastExpr.
	 */
	public Set<DefinitionStmt> getDefsWithAliasesRemoveLocalAndCast(Local l, Unit start) {
		return workerMethod(l, start, false, toRemoveLocal | toRemoveCast);
	}
	
	/** The same as {@link #getDefsWithAliases(Local l, Unit start)} except this also
	 * includes ArrayRef base Locals in the aliasing process.
	 */
	public Set<DefinitionStmt> getDefsWithAliasesAndArrays(Local l, Unit start) {
		return workerMethod(l, start, true, 0);
	}
	
	/** The same as {@link #getDefsWithAliasesAndArrays(Local l, Unit start)} except this
	 * removes aliasing definition statements that are Local assignments.
	 */
	public Set<DefinitionStmt> getDefsWithAliasesAndArraysRemoveLocal(Local l, Unit start) {
		return workerMethod(l, start, true, toRemoveLocal);
	}
	
	/** The same as {@link #getDefsWithAliasesAndArrays(Local l, Unit start)} except this
	 * removes aliasing definition statements that are CastExpr.
	 */
	public Set<DefinitionStmt> getDefsWithAliasesAndArraysRemoveCast(Local l, Unit start) {
		return workerMethod(l, start, true, toRemoveCast);
	}
	
	/** The same as {@link #getDefsWithAliasesAndArrays(Local l, Unit start)} except this
	 * removes aliasing definition statements that are ArrayRef.
	 */
	public Set<DefinitionStmt> getDefsWithAliasesAndArraysRemoveArrays(Local l, Unit start) {
		return workerMethod(l, start, true, toRemoveArray);
	}
	
	/** The same as {@link #getDefsWithAliasesAndArrays(Local l, Unit start)} except this
	 * removes aliasing definition statements that are Local assignments or CastExpr.
	 */
	public Set<DefinitionStmt> getDefsWithAliasesAndArraysRemoveLocalAndCast(Local l, Unit start) {
		return workerMethod(l, start, true, toRemoveLocal | toRemoveCast);
	}
	
	/** The same as {@link #getDefsWithAliasesAndArrays(Local l, Unit start)} except this
	 * removes aliasing definition statements that are CastExpr or ArrayRef.
	 */
	public Set<DefinitionStmt> getDefsWithAliasesAndArraysRemoveCastAndArrays(Local l, Unit start) {
		return workerMethod(l, start, true, toRemoveCast | toRemoveArray);
	}
	
	/** The same as {@link #getDefsWithAliasesAndArrays(Local l, Unit start)} except this
	 * removes aliasing definition statements that are Local assignments or ArrayRef.
	 */
	public Set<DefinitionStmt> getDefsWithAliasesAndArraysRemoveLocalAndArrays(Local l, Unit start) {
		return workerMethod(l, start, true, toRemoveLocal | toRemoveArray);
	}
	
	/** The same as {@link #getDefsWithAliasesAndArrays(Local l, Unit start)} except this
	 * removes aliasing definition statements that are Local assignments, CastExpr, or ArrayRef.
	 */
	public Set<DefinitionStmt> getDefsWithAliasesAndArraysRemoveLocalAndCastAndArrays(Local l, Unit start) {
		return workerMethod(l, start, true, toRemoveLocal | toRemoveCast | toRemoveArray);
	}
	
	//No need to cache these results since we need to re run it for each unit provided and each unit will only get passed to this method at most
	//once because of restrictions in the inter-procedural analysis (if it changes we need to update this)
	private Set<DefinitionStmt> workerMethod(Local l, Unit start, boolean includeArrays, int toRemove) {
		Set<Pair<Local,Unit>> visited = new HashSet<>();
		Set<DefinitionStmt> ret = new HashSet<>();
		Queue<Pair<Local,Unit>> toVisit = new ArrayDeque<>();
		
		toVisit.add(new Pair<Local,Unit>(l,start));
		while(!toVisit.isEmpty()) { //Limit needs to be changed to whatever uses it when grabbing the defs
			Pair<Local,Unit> p = toVisit.poll();
			Local cur = p.getFirst();
			Unit curU = p.getSecond();
			if(visited.add(p)) {
				for(Unit defUnit : getDefsOfAt(cur, curU)) {
					DefinitionStmt defStmt = (DefinitionStmt)defUnit;
					Value right = defStmt.getRightOp();
					if(right instanceof Local) {
						toVisit.add(new Pair<>((Local) right, defUnit));
						if((toRemove & toRemoveLocal) == 0)
							ret.add(defStmt);
					} else if(right instanceof CastExpr) {
						Value v = ((CastExpr)right).getOp();
						if(v instanceof Local)
							toVisit.add(new Pair<>((Local)v, defUnit));
						if((toRemove & toRemoveCast) == 0)
							ret.add(defStmt);
					} else if(includeArrays && right instanceof ArrayRef) {
						Value v = ((ArrayRef)right).getBase();
						if(v instanceof Local)
							toVisit.add(new Pair<>((Local)v, defUnit));
						if((toRemove & toRemoveArray) == 0)
							ret.add(defStmt);
					} else {
						ret.add(defStmt);
					}
				}
			}
		}
		return ret;
	}
	
}
