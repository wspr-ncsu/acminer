package org.sag.soot.analysis;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.sag.common.tools.SortingMethods;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableMap.Builder;

import soot.Body;
import soot.Unit;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DominatorsFinder;
import soot.toolkits.graph.UnitGraph;

public class LoopFinder {
	
	private final Set<Loop> loops;
	
	public LoopFinder(Body b) {
		//Note we are using a unit graph instead of a exceptional unit graph because
		//the exceptional edges muddle the results especially with exit stmts
		//For our purposes, we do not care about the exit stmts resulting from exceptions
		//or flow resulting from exceptions for that matter
		//We just want the core loop
		UnitGraph g = new BriefUnitGraph(b);
		this.loops = findLoops(g, new FastDominatorsFinder<Unit>(g), new Comp(g));
	}
	
	public Set<Loop> getLoops() {
		return loops;
	}
	
	public String toString(String spacer) {
		StringBuilder sb = new StringBuilder();
		sb.append("Loops: \n");
		int i = 0;
		for(Loop l : loops) {
			sb.append(l.toString(spacer+"  ", i++));
		}
		return sb.toString();
	}
	
	@Override
	public String toString() {
		return toString("");
	}
	
	@Override
	public int hashCode() {
		return loops.hashCode();
	}
	
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof LoopFinder))
			return false;
		LoopFinder f = (LoopFinder)o;
		return Objects.equals(loops, f.loops);
	}
	
	private Set<Loop> findLoops(UnitGraph g, DominatorsFinder<Unit> f, Comp comp) {
		Map<Unit, Set<Unit>> loops = new HashMap<>();
		
		//Find the loops
		for(Unit u : g.getBody().getUnits()) {
			List<Unit> succs = g.getSuccsOf(u);
			List<Unit> doms = f.getDominators(u);
			Set<Unit> headers = new HashSet<>();
			
			for(Unit succ : succs) {
				if(doms.contains(succ)) 
					headers.add(succ);
			}
			
			for(Unit header : headers) {
				Set<Unit> body = getLoopBody(header, u, g, comp);//Sorts by body order comp
				if(loops.containsKey(header)) {
					Set<Unit> curBody = loops.get(header);
					loops.put(header, union(curBody, body, comp));//Sorts by body order comp
				} else {
					loops.put(header, body);
				}
			}
		}
		SortingMethods.sortMapKey(loops, comp);
		
		Set<Loop> ret;
		if(!loops.isEmpty()) {
			//Discover exit and entry statements
			//Entry statements should not be in the loop body as these lead into the loop body
			ret = new LinkedHashSet<>();
			for(Unit header : loops.keySet()) {
				Set<Unit> loopBody = loops.get(header);
				Set<Unit> enters = new HashSet<>();
				Map<Unit,Set<Unit>> exitToExitTargets = new HashMap<>();
				Set<Unit> backJumps = new HashSet<>();
				
				//discover the exit statements of the loop if any (i.e. the statements that lead to stats outside of the loop)
				//also discover the exit targets that are outside of the loop body if any
				for(Unit u : loopBody) {
					for(Unit succ : g.getSuccsOf(u)) {
						if(!loopBody.contains(succ)) {
							Set<Unit> exitTargets = exitToExitTargets.get(u);
							if(exitTargets == null) {
								exitTargets = new HashSet<>();
								exitToExitTargets.put(u, exitTargets);
							}
							exitTargets.add(succ);
						}
					}
					if(exitToExitTargets.containsKey(u))
						exitToExitTargets.put(u, SortingMethods.sortSet(exitToExitTargets.get(u),comp));
				}
				exitToExitTargets = SortingMethods.sortMapKey(exitToExitTargets, comp);
				
				//discover the entry statements of the loop (i.e. the stmts that lead to the header and are not part of the loop)
				for(Unit u : g.getPredsOf(header)) {
					if(!loopBody.contains(u))
						enters.add(u);
				}
				enters = SortingMethods.sortSet(enters, comp);
				
				for(Unit pred : g.getPredsOf(header)) {
					if(loopBody.contains(pred))
						backJumps.add(pred);
				}
				backJumps = SortingMethods.sortSet(backJumps, comp);
				
				Loop l = new Loop(header, loopBody, enters, exitToExitTargets, backJumps);
				
				ret.add(l);
			}
			ret = ImmutableSet.copyOf(ret);
		} else {
			ret = ImmutableSet.of();
		}
		return ret;
	}
	
	private Set<Unit> getLoopBody(Unit header, Unit node, UnitGraph g, Comp comp) {
		Set<Unit> ret = new HashSet<>();
		Deque<Unit> stack = new ArrayDeque<>();
		
		ret.add(header);
		stack.push(node);
		while(!stack.isEmpty()) {
			Unit cur = stack.pop();
			if(ret.add(cur)) {
				for(Unit next : g.getPredsOf(cur)) 
					stack.push(next);
			}
		}
		
		ret = SortingMethods.sortSet(ret, comp);
		
		return ret;
	}
	
	private Set<Unit> union(Set<Unit> curBody, Set<Unit> body, Comp comp) {
		Set<Unit> ret = new HashSet<>();
		ret.addAll(curBody);
		ret.addAll(body);
		ret = SortingMethods.sortSet(ret, comp);
		return ret;
	}
	
	private static final class Comp implements Comparator<Unit>{
		private final Map<Unit, Integer> unitToPosition = new HashMap<>();
		public Comp(UnitGraph g) {
			int i = 0;
			for(Unit u : g.getBody().getUnits()) {
				unitToPosition.put(u, i++);
			}
		}
		@Override
		public int compare(Unit o1, Unit o2) {
			return unitToPosition.get(o1).compareTo(unitToPosition.get(o2));
		}
	}
	
	public static final class Loop {
		private final Unit header;
		private final Set<Unit> loopBody;
		private final Set<Unit> enters;
		private final Map<Unit,Set<Unit>> exitToExitTargets;
		private final Set<Unit> backJumps;
		private final Set<Unit> exits;
		private final boolean isSimple;
		private final boolean isInfinite;
		private final int hashCode;
		public Loop(Unit header, Set<Unit> loopBody, Set<Unit> enters, Map<Unit,Set<Unit>> exitToExitTargets, Set<Unit> backJumps) {
			this.header = header;
			this.loopBody = ImmutableSet.copyOf(loopBody);
			this.enters = ImmutableSet.copyOf(enters);
			this.exits = ImmutableSet.copyOf(exitToExitTargets.keySet());
			this.backJumps = ImmutableSet.copyOf(backJumps);
			Builder<Unit,Set<Unit>> b = ImmutableMap.builder();
			for(Unit exit : exitToExitTargets.keySet()) {
				b.put(exit, ImmutableSet.copyOf(exitToExitTargets.get(exit)));
			}
			this.exitToExitTargets = b.build();
			this.isSimple = exits.size() == 1;
			this.isInfinite = exits.size() == 0;
			this.hashCode = getHashCode();
		}
		public boolean isSimple() {
			return isSimple;
		}
		public boolean isInfinite() {
			return isInfinite;
		}
		/** The statement at which we enter the loop. Note headers are nodes that dominate their successors.*/
		public Unit getHead() {
			return header;
		}
		/** All statements contained within the loop. From the head to the backwards jump inclusive.*/
		public Set<Unit> getBody() {
			return loopBody;
		}
		/** These are the statements that lead to the head but are not contained within the loop body.*/
		public Set<Unit> getEntryStmts() {
			return enters;
		}
		/** These are the statements inside the loop body that lead to statements outside the loop body.*/
		public Set<Unit> getExitStmts() {
			return exits;
		}
		public Set<Unit> getBackJumps() {
			return backJumps;
		}
		/** Given an exit statement of the loop, this returns the target successor statements of this exit statement. 
		 * (i.e. the target successor statements that are outside the loop body)*/
		public Set<Unit> getExitTargets(Unit u) {
			Set<Unit> ret = exitToExitTargets.get(u);
			if(ret == null)
				ret = ImmutableSet.of();
			return ret;
		}
		/** This returns the target successor statements of all exit statements. (i.e. the target successor statements
		 * that are outside the loop body) */
		public Map<Unit, Set<Unit>> getExitTargets() {
			return exitToExitTargets;
		}
		private int getHashCode() {
			int i = 17;
			i = i * 31 + Objects.hashCode(header);
			i = i * 31 + Objects.hashCode(loopBody);
			i = i * 31 + Objects.hashCode(enters);
			i = i * 31 + Objects.hashCode(exitToExitTargets);
			i = i * 31 + Objects.hashCode(backJumps);
			return i;
		}
		@Override
	    public int hashCode() {
			return hashCode;
		}
		@Override
		public boolean equals(Object obj) {
			if(obj == this)
				return true;
			if(obj == null || !(obj instanceof Loop))
				return false;
			Loop l = (Loop)obj;
			return Objects.equals(header, l.header) && Objects.equals(loopBody, l.loopBody) && Objects.equals(enters, l.enters) 
					&& Objects.equals(exitToExitTargets, l.exitToExitTargets) && Objects.equals(backJumps, l.backJumps);
		}
		public String toString(String spacer, Integer i) {
			StringBuilder sb = new StringBuilder();
			sb.append(spacer).append("Loop ").append(Objects.toString(i, "")).append("\n");
			sb.append(spacer).append("  Head Stmt: ").append(Objects.toString(header)).append("\n");
			sb.append(spacer).append("  Back Jump Stmts: \n");
			for(Unit u : backJumps) {
				sb.append(spacer).append("    ").append(u).append("\n");
			}
			sb.append(spacer).append("  Exit Stmts: \n");
			for(Unit u : exits) {
				sb.append(spacer).append("    ").append(u).append("\n");
				sb.append(spacer).append("      Target Stmts: \n");
				for(Unit s : exitToExitTargets.get(u)) {
					sb.append(spacer).append("        ").append(s).append("\n");
				}
			}
			sb.append(spacer).append("  Entry Stmts: \n");
			for(Unit u : enters) {
				sb.append(spacer).append("    ").append(u).append("\n");
			}
			sb.append(spacer).append("  Body Stmts: \n");
			for(Unit u : loopBody) {
				sb.append(spacer).append("    ").append(u).append("\n");
			}
			return sb.toString();
		}
		public String toString() {
			return toString("",null);
		}
	}
	
}
