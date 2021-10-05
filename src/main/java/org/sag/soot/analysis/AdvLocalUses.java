package org.sag.soot.analysis;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import soot.Local;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.CastExpr;
import soot.jimple.DefinitionStmt;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.LocalDefs;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.toolkits.scalar.UnitValueBoxPair;

public class AdvLocalUses extends SimpleLocalUses {
	
	private static final int toRemoveLocal = 1;
	private static final int toRemoveCast = 2;
	private static final int toRemoveArray = 4;

	public AdvLocalUses(UnitGraph unitGraph, LocalDefs localDefs) {
		super(unitGraph, localDefs);
	}
	
	/** Get all uses of a local l defined in the given definition statement. Note this
	 * takes into account the state of the local, so if it is redefined at some point
	 * the uses of the local in that state would not be included. This will also take into
	 * account aliasing of the local. So if the uses of the defined local l include other
	 * definition statements where l is being assigned to another local or where l is
	 * being type cast and then assigned to another local, the uses of these locals will
	 * also be included in the final set. The aliasing process is done in a recursive 
	 * manner on each local discovered to be an alias of the original local or one of
	 * its aliases.
	 * 
	 * @param start The definition statement that defines the local we are searching for
	 *              uses of.
	 * @return All uses of the local defined in the definition statement and the uses of
	 *         any of its aliases
	 */
	public Set<UnitValueBoxPair> getUsesWithAliases(DefinitionStmt start) {
		return workerMethod(start, false, 0);
	}
	
	/** The same as {@link #getUsesWithAliases(Local l, Unit start)} except this removes
	 * aliasing statements that are Local assignments.
	 */
	public Set<UnitValueBoxPair> getUsesWithAliasesRemoveLocal(DefinitionStmt start) {
		return workerMethod(start, false, toRemoveLocal);
	}
	
	/** The same as {@link #getUsesWithAliases(Local l, Unit start)} except this removes
	 * aliasing statements that are CastExpr.
	 */
	public Set<UnitValueBoxPair> getUsesWithAliasesRemoveCast(DefinitionStmt start) {
		return workerMethod(start, false, toRemoveCast);
	}
	
	/** The same as {@link #getUsesWithAliases(Local l, Unit start)} except this removes
	 * aliasing statements that are Local assignments or CastExpr.
	 */
	public Set<UnitValueBoxPair> getUsesWithAliasesRemoveLocalAndCast(DefinitionStmt start) {
		return workerMethod(start, false, toRemoveLocal | toRemoveCast);
	}
	
	/** The same as {@link #getUsesWithAliases(Local l, Unit start)} except this also
	 * includes ArrayRef base Locals in the aliasing process.
	 */
	public Set<UnitValueBoxPair> getUsesWithAliasesAndArrays(DefinitionStmt start) {
		return workerMethod(start, true, 0);
	}
	
	/** The same as {@link #getUsesWithAliasesAndArrays(Local l, Unit start)} except this
	 * removes aliasing statements that are Local assignments.
	 */
	public Set<UnitValueBoxPair> getUsesWithAliasesAndArraysRemoveLocal(DefinitionStmt start) {
		return workerMethod(start, true, toRemoveLocal);
	}
	
	/** The same as {@link #getUsesWithAliasesAndArrays(Local l, Unit start)} except this
	 * removes aliasing statements that are CastExpr.
	 */
	public Set<UnitValueBoxPair> getUsesWithAliasesAndArraysRemoveCast(DefinitionStmt start) {
		return workerMethod(start, true, toRemoveCast);
	}
	
	/** The same as {@link #getUsesWithAliasesAndArrays(Local l, Unit start)} except this
	 * removes aliasing statements that are ArrayRef.
	 */
	public Set<UnitValueBoxPair> getUsesWithAliasesAndArraysRemoveArrays(DefinitionStmt start) {
		return workerMethod(start, true, toRemoveArray);
	}
	
	/** The same as {@link #getUsesWithAliasesAndArrays(Local l, Unit start)} except this
	 * removes aliasing statements that are Local assignments or CastExpr.
	 */
	public Set<UnitValueBoxPair> getUsesWithAliasesAndArraysRemoveLocalAndCast(DefinitionStmt start) {
		return workerMethod(start, true, toRemoveLocal | toRemoveCast);
	}
	
	/** The same as {@link #getUsesWithAliasesAndArrays(Local l, Unit start)} except this
	 * removes aliasing statements that are CastExpr or ArrayRef.
	 */
	public Set<UnitValueBoxPair> getUsesWithAliasesAndArraysRemoveCastAndArrays(DefinitionStmt start) {
		return workerMethod(start, true, toRemoveCast | toRemoveArray);
	}
	
	/** The same as {@link #getUsesWithAliasesAndArrays(Local l, Unit start)} except this
	 * removes aliasing statements that are Local assignments or ArrayRef.
	 */
	public Set<UnitValueBoxPair> getUsesWithAliasesAndArraysRemoveLocalAndArrays(DefinitionStmt start) {
		return workerMethod(start, true, toRemoveLocal | toRemoveArray);
	}
	
	/** The same as {@link #getUsesWithAliasesAndArrays(Local l, Unit start)} except this
	 * removes aliasing statements that are Local assignments, CastExpr, or ArrayRef.
	 */
	public Set<UnitValueBoxPair> getUsesWithAliasesAndArraysRemoveLocalAndCastAndArrays(DefinitionStmt start) {
		return workerMethod(start, true, toRemoveLocal | toRemoveCast | toRemoveArray);
	}
	
	private Set<UnitValueBoxPair> workerMethod(DefinitionStmt start, boolean includeArrays, int toRemove) {
		Set<DefinitionStmt> visited = new HashSet<>();
		Set<UnitValueBoxPair> ret = new HashSet<>();
		Queue<DefinitionStmt> toVisit = new ArrayDeque<>();
		
		toVisit.add(start);
		while(!toVisit.isEmpty()) {
			DefinitionStmt cur = toVisit.poll();
			if(visited.add(cur)) {
				//Unit is the unit where the local from the left of the DefinitionStmt is used
				//Value is the local used in unit (i.e. the left of the DefinitionStmt)
				for(UnitValueBoxPair usePair : getUsesOf(cur)) {
					Unit use = usePair.getUnit();
					Local l = (Local)(usePair.getValueBox().getValue());
					
					if(use instanceof DefinitionStmt) {
						DefinitionStmt def = (DefinitionStmt)use;
						Value right = def.getRightOp();
						Value left = def.getLeftOp();
						if(left instanceof Local) {
							if(right instanceof Local) {
								toVisit.add(def);
								if((toRemove & toRemoveLocal) == 0)
									ret.add(usePair);
							} else if(right instanceof CastExpr) {
								toVisit.add(def);
								if((toRemove & toRemoveCast) == 0)
									ret.add(usePair);
							} else if(includeArrays && right instanceof ArrayRef && ((ArrayRef)right).getBase().equals(l)) {
								toVisit.add(def);
								if((toRemove & toRemoveArray) == 0)
									ret.add(usePair);
							} else {
								ret.add(usePair);
							}
						} else {
							ret.add(usePair);
						}
					} else {
						ret.add(usePair);
					}
				}
			}
		}
		return ret;
	}

}
