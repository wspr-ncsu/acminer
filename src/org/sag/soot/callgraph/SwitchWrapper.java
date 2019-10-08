package org.sag.soot.callgraph;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import soot.Unit;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.SwitchStmt;
import soot.jimple.TableSwitchStmt;

public class SwitchWrapper {

	/* Target -> case value int
	 * Case value int -> target
	 * Note what group the default case is in for the targets
	 */
	
	private final SwitchStmt stmt;
	private final Map<Unit,Set<Integer>> targetsToCases;
	private final Map<Integer,Unit> casesToTarget;
	
	public SwitchWrapper(SwitchStmt stmt) {
		this.stmt = stmt;
		this.targetsToCases = new LinkedHashMap<>();
		this.casesToTarget = new LinkedHashMap<>();
		
		if(stmt instanceof TableSwitchStmt) {
			TableSwitchStmt switchStmt = (TableSwitchStmt)stmt;
			for(int i = switchStmt.getLowIndex(); i < switchStmt.getHighIndex(); i++) {
				Unit target = switchStmt.getTarget(i);
				Set<Integer> vals = targetsToCases.get(target);
				if(vals == null) {
					vals = new LinkedHashSet<>();
					targetsToCases.put(target, vals);
				}
				vals.add(i);
				casesToTarget.put(i, target);
			}
		} else if(stmt instanceof LookupSwitchStmt) {
			LookupSwitchStmt switchStmt = (LookupSwitchStmt)stmt;
			for (int i = 0; i < switchStmt.getLookupValues().size(); i++) {
				int caseVal = switchStmt.getLookupValue(i);
				Unit target = switchStmt.getTarget(i);
				Set<Integer> vals = targetsToCases.get(target);
				if(vals == null) {
					vals = new LinkedHashSet<>();
					targetsToCases.put(target, vals);
				}
				vals.add(caseVal);
				casesToTarget.put(caseVal, target);
			}
		} else {
			throw new RuntimeException("Error: Unhandled switch type '" + stmt.getClass().getCanonicalName() + "'");
		}
		
		Unit defTgt = stmt.getDefaultTarget();
		Set<Integer> vals = targetsToCases.get(defTgt);
		if(vals == null) {
			vals = new LinkedHashSet<>();
			targetsToCases.put(defTgt, vals);
		}
		vals.add(null);
		casesToTarget.put(null, defTgt);
	}
	
	/** Returns the set of case values for this switch stmt that will lead to
	 * the given target. If the given unit is not a target for the switch stmt
	 * then a empty set is returned. The returned set may contain a null value
	 * if the one of the cases for the target is null. The returned set is ordered
	 * according the case ordering.
	 */
	public Set<Integer> getCasesForTarget(Unit target) {
		Objects.requireNonNull(target);
		if(targetsToCases.containsKey(target)) {
			return new LinkedHashSet<>(targetsToCases.get(target));
		}
		return Collections.emptySet();
	}
	
	/** Returns the target for the given case or null if the value given is
	 * not a valid case for the switch statement.
	 */
	public Unit getTargetForCase(Integer val) {
		return casesToTarget.get(val);
	}
	
	/** Returns a set containing all the possible case values for this switch
	 * stmt. Note the set should always contain the value null in the last 
	 * position to indicate the default case. The set is ordered according to
	 * the case order of the switch statement.
	 */
	public Set<Integer> getCases() {
		return new LinkedHashSet<>(casesToTarget.keySet());
	}
	
	/** Returns a set containing all the possible target units for this switch
	 * statement ordered according to the case order of the switch statement.
	 */
	public Set<Unit> getTargets() {
		return new LinkedHashSet<>(targetsToCases.keySet());
	}
	
	/** Returns the default target unit. This is a short cut method.
	 */
	public Unit getDefaultTarget() {
		return casesToTarget.get(null);
	}
	
	/** Return group of cases that lead to the same target where the target 
	 * also happens to be the target of the default case. Therefore, this
	 * group will contain a null entry representing the default case.
	 */
	public Set<Integer> getDefaultCaseGroup() {
		return new LinkedHashSet<>(targetsToCases.get(getDefaultTarget()));
	}
	
	public SwitchStmt getSwitchStmt() {
		return stmt;
	}
	
}
