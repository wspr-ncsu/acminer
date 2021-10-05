package org.sag.acminer.scripts;

import heros.solver.IDESolver;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.io.FileHelpers;
import org.sag.common.io.PrintStreamUnixEOL;
import org.sag.common.logging.ILogger;
import org.sag.common.tools.HierarchyHelpers;
import org.sag.common.tools.SortingMethods;
import org.sag.common.tuple.Pair;
import org.sag.soot.analysis.AdvLocalDefs;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import soot.Body;
import soot.Local;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.CastExpr;
import soot.jimple.ConditionExpr;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.IfStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.SwitchStmt;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.LiveLocals;

public class AnalyizeControlPredicates {

	private String cn;
	private ILogger logger;
	private IACMinerDataAccessor dataAccessor;
	private boolean enableExceptions;
	private Path cpBF;
	private Path invokeBF;
	private Path fieldBF;
	private Path otherBF;
	private Path typeBF;
	private Path cpBFAll;
	private Path invokeBFAll;
	private Path fieldBFAll;
	private Path otherBFAll;
	private Path typeBFAll;
	
	private Path cpAF;
	private Path invokeAF;
	private Path fieldAF;
	private Path otherAF;
	private Path typeAF;
	private Path cpAFAll;
	private Path invokeAFAll;
	private Path fieldAFAll;
	private Path otherAFAll;
	private Path typeAFAll;
	
	protected final Map<Unit,Body> unitToOwner;
	protected final LoadingCache<Body,AdvLocalDefs> advLocalDefs;
	protected final LoadingCache<Body,UnitGraph> bodyToUnitGraph;
	protected final LoadingCache<Unit,Map<Local,Set<DefinitionStmt>>> unitToDefsForUsedLocals;
	
	private Map<EntryPoint,Map<String,String>> epToTypeToExample;
	private Map<EntryPoint,Set<String>> epToExprs;
	private Map<EntryPoint,Set<String>> epToInvokes;
	private Map<EntryPoint,Set<String>> epToFields;
	private Map<EntryPoint,Set<String>> epToOther;
	
	public AnalyizeControlPredicates(Path outDir, IACMinerDataAccessor dataAccessor, boolean enableExceptions, ILogger logger) {
		this.cn = getClass().getSimpleName();
		this.dataAccessor = dataAccessor;
		this.logger = logger;
		this.unitToOwner = new HashMap<>();
		this.enableExceptions = enableExceptions;
		this.bodyToUnitGraph = getNewBodyUnitGraphCache();
		this.advLocalDefs = getNewLocalDefs();
		this.unitToDefsForUsedLocals = getNewUnitToDefsForUsedLocals();
		
		this.cpBF = FileHelpers.getPath(outDir, "cp_ep_before_filter.txt");
		this.invokeBF = FileHelpers.getPath(outDir, "invoke_ep_before_filter.txt");
		this.fieldBF = FileHelpers.getPath(outDir, "field_ep_before_filter.txt");
		this.otherBF = FileHelpers.getPath(outDir, "other_ep_before_filter.txt");
		this.typeBF = FileHelpers.getPath(outDir, "type_ep_before_filter.txt");
		
		this.cpBFAll = FileHelpers.getPath(outDir, "cp_before_filter.txt");
		this.invokeBFAll = FileHelpers.getPath(outDir, "invoke_before_filter.txt");
		this.fieldBFAll = FileHelpers.getPath(outDir, "field_before_filter.txt");
		this.otherBFAll = FileHelpers.getPath(outDir, "other_before_filter.txt");
		this.typeBFAll = FileHelpers.getPath(outDir, "type_before_filter.txt");
		
		this.cpAF = FileHelpers.getPath(outDir, "cp_ep_after_filter.txt");
		this.invokeAF = FileHelpers.getPath(outDir, "invoke_ep_after_filter.txt");
		this.fieldAF = FileHelpers.getPath(outDir, "field_ep_after_filter.txt");
		this.otherAF = FileHelpers.getPath(outDir, "other_ep_after_filter.txt");
		this.typeAF = FileHelpers.getPath(outDir, "type_ep_after_filter.txt");
		
		this.cpAFAll = FileHelpers.getPath(outDir, "cp_after_filter.txt");
		this.invokeAFAll = FileHelpers.getPath(outDir, "invoke_after_filter.txt");
		this.fieldAFAll = FileHelpers.getPath(outDir, "field_after_filter.txt");
		this.otherAFAll = FileHelpers.getPath(outDir, "other_after_filter.txt");
		this.typeAFAll = FileHelpers.getPath(outDir, "type_after_filter.txt");
		
		this.epToTypeToExample = new HashMap<>();
		this.epToExprs = new HashMap<>();
		this.epToInvokes = new HashMap<>();
		this.epToFields = new HashMap<>();
		this.epToOther = new HashMap<>();
	}
	
	protected LoadingCache<Body,AdvLocalDefs> getNewLocalDefs() {
		return IDESolver.DEFAULT_CACHE_BUILDER.build(new CacheLoader<Body,AdvLocalDefs>() {
			@Override
			public AdvLocalDefs load(Body b) throws Exception {
				UnitGraph g = getOrCreateUnitGraph(b);
				return new AdvLocalDefs(g,LiveLocals.Factory.newLiveLocals(g));
			}
		});
	}
	
	protected LoadingCache<Body,UnitGraph> getNewBodyUnitGraphCache() {
		return IDESolver.DEFAULT_CACHE_BUILDER.build( new CacheLoader<Body,UnitGraph>() {
			@Override
			public UnitGraph load(Body body) throws Exception {
				return enableExceptions ? new ExceptionalUnitGraph(body) : new BriefUnitGraph(body);
			}
		});
	}
	
	protected LoadingCache<Unit,Map<Local,Set<DefinitionStmt>>> getNewUnitToDefsForUsedLocals() {
		return IDESolver.DEFAULT_CACHE_BUILDER.build(new CacheLoader<Unit,Map<Local,Set<DefinitionStmt>>>() {
			@Override
			public Map<Local,Set<DefinitionStmt>> load(Unit u) throws Exception {
				//Contains all values and the values inside those values so it should contain all locals
				//If an array is being assigned a value (i.e. on the left) then its local and its index local are included in the uses
				List<ValueBox> useBoxes = u.getUseBoxes();
				SootMethod source = getMethodOf(u);
				if(!useBoxes.isEmpty() && source != null) {
					AdvLocalDefs df = getOrMakeLocalDefs(source);
					Map<Local,Set<DefinitionStmt>> defs = new HashMap<>();
					for (ValueBox vb: useBoxes) {
						Value v = vb.getValue();
						if (v instanceof Local) {
							Local l = (Local)v;
							Set<DefinitionStmt> temp = defs.get(l);
							if(temp == null) {
								temp = new HashSet<>();
								defs.put(l, temp);
							}
							temp.addAll(df.getDefsWithAliases(l,u));
						}
					}
					return defs.isEmpty() ? Collections.<Local,Set<DefinitionStmt>>emptyMap() : defs;
				} else {
					return Collections.emptyMap();
				}
			}
		});
	}
	
	public SootMethod getMethodOf(Unit u) {
		Body b = unitToOwner.get(u);
		return b == null ? null : b.getMethod();
	}
	
	public UnitGraph getOrCreateUnitGraph(SootMethod m) {
		return m.hasActiveBody() ? getOrCreateUnitGraph(m.getActiveBody()) : null;
	}

	public UnitGraph getOrCreateUnitGraph(Body body) {
		return bodyToUnitGraph.getUnchecked(body);
	}
	
	public AdvLocalDefs getOrMakeLocalDefs(SootMethod m) {
		return m.hasActiveBody() ? getOrMakeLocalDefs(m.getActiveBody()) : null;
	}
	
	public AdvLocalDefs getOrMakeLocalDefs(Body b) {
		return advLocalDefs.getUnchecked(b);
	}
	
	public Map<Local,Set<DefinitionStmt>> getDefsForUsedLocals(Unit u) {
		if(unitToOwner.containsKey(u)) {
			return unitToDefsForUsedLocals.getUnchecked(u);
		}
		return Collections.emptyMap();
	}
	
	public void run() throws FileNotFoundException {
		
		for(SootMethod source : dataAccessor.getControlPredicatesDB().getSources()) {
			Body b = source.retrieveActiveBody();
			for(Unit u : b.getUnits())
				unitToOwner.put(u, b);
		}
		
		//ControlPredicateFinder cpf = new ControlPredicateFinder();
		//IJimpleICFG icfg = new DumbICFG();
		//IControlPredicateFilterDatabase filterdb = dataAccessor.getControlPredicateFilterDB();
		Map<EntryPoint, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> data = dataAccessor.getControlPredicatesDB().getAllData();
		Map<EntryPoint, Map<SootMethod, Set<Unit>>> cps = new LinkedHashMap<>();
		for(EntryPoint ep : data.keySet()) {
			Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> d = data.get(ep);
			Map<SootMethod, Set<Unit>> cpsInner = new LinkedHashMap<>();
			for(SootMethod source : d.keySet()) {
				cpsInner.put(source, d.get(source).getFirst());
			}
			cps.put(ep, cpsInner);
		}
			
		this.epToTypeToExample = new LinkedHashMap<>();
		this.epToExprs = new LinkedHashMap<>();
		this.epToInvokes = new LinkedHashMap<>();
		this.epToFields = new LinkedHashMap<>();
		this.epToOther = new LinkedHashMap<>();
		for(EntryPoint ep : cps.keySet()) {
			genInfo(ep, cps.get(ep));
		}
		
		Set<String> allExprs = new HashSet<>();
		Set<String> allInvokes = new HashSet<>();
		Set<String> allFields = new HashSet<>();
		Set<String> allOther = new HashSet<>();
		Map<String,String> allType = new HashMap<>();
		for(EntryPoint ep: cps.keySet()) {
			allExprs.addAll(epToExprs.get(ep));
			allInvokes.addAll(epToInvokes.get(ep));
			allFields.addAll(epToFields.get(ep));
			allOther.addAll(epToOther.get(ep));
			allType.putAll(epToTypeToExample.get(ep));
		}
		allExprs = SortingMethods.sortSet(allExprs,SortingMethods.sComp);
		allInvokes = SortingMethods.sortSet(allInvokes,SortingMethods.sComp);
		allFields = SortingMethods.sortSet(allFields,SortingMethods.sComp);
		allOther = SortingMethods.sortSet(allOther,SortingMethods.sComp);
		allType = SortingMethods.sortMapKey(allType, SortingMethods.sComp);
		
		try (PrintStream ps = new PrintStreamUnixEOL(cpBF.toFile())) {
			dumpEp(ps,cps.keySet(),epToExprs);
		}
		
		try (PrintStream ps = new PrintStreamUnixEOL(invokeBF.toFile())) {
			dumpEp(ps,cps.keySet(),epToInvokes);
		}
		
		try (PrintStream ps = new PrintStreamUnixEOL(fieldBF.toFile())) {
			dumpEp(ps,cps.keySet(),epToFields);
		}
		
		try (PrintStream ps = new PrintStreamUnixEOL(otherBF.toFile())) {
			dumpEp(ps,cps.keySet(),epToOther);
		}
		
		try (PrintStream ps = new PrintStreamUnixEOL(typeBF.toFile())) {
			for(EntryPoint ep : cps.keySet()) {
				ps.println("EntryPoint: " + ep);
				Map<String, String> m = epToTypeToExample.get(ep);
				for(Map.Entry<String, String> e: m.entrySet()) {
					ps.println("  " + e.getKey() + " " + e.getValue());
				}
			}
		}
		
		
		try (PrintStream ps = new PrintStreamUnixEOL(cpBFAll.toFile())) {
			dump(ps, allExprs);
		}
		
		try (PrintStream ps = new PrintStreamUnixEOL(invokeBFAll.toFile())) {
			dump(ps,allInvokes);
		}
		
		try (PrintStream ps = new PrintStreamUnixEOL(fieldBFAll.toFile())) {
			dump(ps,allFields);
		}
		
		try (PrintStream ps = new PrintStreamUnixEOL(otherBFAll.toFile())) {
			dump(ps,allOther);
		}
		
		try (PrintStream ps = new PrintStreamUnixEOL(typeBFAll.toFile())) {
			for(Map.Entry<String, String> e: allType.entrySet()) {
				ps.println("  " + e.getKey() + " : " + e.getValue());
			}
		}
		
		this.epToTypeToExample = new LinkedHashMap<>();
		this.epToExprs = new LinkedHashMap<>();
		this.epToInvokes = new LinkedHashMap<>();
		this.epToFields = new LinkedHashMap<>();
		this.epToOther = new LinkedHashMap<>();
		for(EntryPoint ep : cps.keySet()) {
			logger.info("{}: Running filter on EP '{}'.",cn,ep);
			//genInfo(ep,cpf.applyEndControlPredicateFilter(cps.get(ep), icfg, filterdb, logger));
		}
		
		allExprs = new HashSet<>();
		allInvokes = new HashSet<>();
		allFields = new HashSet<>();
		allOther = new HashSet<>();
		allType = new HashMap<>();
		for(EntryPoint ep: cps.keySet()) {
			allExprs.addAll(epToExprs.get(ep));
			allInvokes.addAll(epToInvokes.get(ep));
			allFields.addAll(epToFields.get(ep));
			allOther.addAll(epToOther.get(ep));
			allType.putAll(epToTypeToExample.get(ep));
		}
		allExprs = SortingMethods.sortSet(allExprs,SortingMethods.sComp);
		allInvokes = SortingMethods.sortSet(allInvokes,SortingMethods.sComp);
		allFields = SortingMethods.sortSet(allFields,SortingMethods.sComp);
		allOther = SortingMethods.sortSet(allOther,SortingMethods.sComp);
		allType = SortingMethods.sortMapKey(allType, SortingMethods.sComp);
		
		try (PrintStream ps = new PrintStreamUnixEOL(cpAF.toFile())) {
			dumpEp(ps,cps.keySet(),epToExprs);
		}
		
		try (PrintStream ps = new PrintStreamUnixEOL(invokeAF.toFile())) {
			dumpEp(ps,cps.keySet(),epToInvokes);
		}
		
		try (PrintStream ps = new PrintStreamUnixEOL(fieldAF.toFile())) {
			dumpEp(ps,cps.keySet(),epToFields);
		}
		
		try (PrintStream ps = new PrintStreamUnixEOL(otherAF.toFile())) {
			dumpEp(ps,cps.keySet(),epToOther);
		}
		
		try (PrintStream ps = new PrintStreamUnixEOL(typeAF.toFile())) {
			for(EntryPoint ep : cps.keySet()) {
				ps.println("EntryPoint: " + ep);
				Map<String, String> m = epToTypeToExample.get(ep);
				for(Map.Entry<String, String> e: m.entrySet()) {
					ps.println("  " + e.getKey() + " " + e.getValue());
				}
			}
		}
		
		try (PrintStream ps = new PrintStreamUnixEOL(cpAFAll.toFile())) {
			dump(ps, allExprs);
		}
		
		try (PrintStream ps = new PrintStreamUnixEOL(invokeAFAll.toFile())) {
			dump(ps,allInvokes);
		}
		
		try (PrintStream ps = new PrintStreamUnixEOL(fieldAFAll.toFile())) {
			dump(ps,allFields);
		}
		
		try (PrintStream ps = new PrintStreamUnixEOL(otherAFAll.toFile())) {
			dump(ps,allOther);
		}
		
		try (PrintStream ps = new PrintStreamUnixEOL(typeAFAll.toFile())) {
			for(Map.Entry<String, String> e: allType.entrySet()) {
				ps.println("  " + e.getKey() + " : " + e.getValue());
			}
		}
		
	}
	
	private Set<Value> getAllValues(AdvLocalDefs localDefs, SootMethod source, Unit cur, Value v) {
		Set<Value> ret = new HashSet<>();
		if(v instanceof Local) {
			Set<DefinitionStmt> defs = localDefs.getDefsWithAliases((Local)v, cur);
			for(DefinitionStmt def : defs) {
				Value right = def.getRightOp();
				if(!(right instanceof Local) && !(right instanceof CastExpr))
					ret.add(right);
			}
		} else if(v instanceof Constant) {
			ret.add(v);
		} else {
			throw new RuntimeException();
		}
		if(ret.isEmpty()) {
			throw new RuntimeException();
		}
		return ret;
	}
	
	private void genInfo(EntryPoint ep, Map<SootMethod, Set<Unit>> d) {
		Map<String,String> typeToExample = new LinkedHashMap<>();
		Set<String> exprs = new HashSet<>();
		Set<String> invokes = new HashSet<>();
		Set<String> fields = new HashSet<>();
		Set<String> other = new HashSet<>();
		for(SootMethod source : d.keySet()) {
			Set<Unit> cps = d.get(source);
			AdvLocalDefs localDefs = getOrMakeLocalDefs(source);
			for(Unit cp : cps) {
				if(cp instanceof IfStmt) {
					ConditionExpr cond = (ConditionExpr)((IfStmt)cp).getCondition();
					Set<Value> leftValues = getAllValues(localDefs, source, cp, cond.getOp1());
					Set<Value> rightValues = getAllValues(localDefs, source, cp, cond.getOp2());
					for(Value leftV : leftValues) {
						for(Value rightV : rightValues) {
							exprs.add("if (" + leftV.toString() + " " + cond.getSymbol() + " " + rightV.toString() + ")");
							if(!typeToExample.containsKey(leftV.getClass().getSimpleName()))
								typeToExample.put("Cond Left: " + leftV.getClass().getSimpleName(), leftV.toString());
							if(!typeToExample.containsKey(rightV.getClass().getSimpleName()))
								typeToExample.put("Cond Right: " + rightV.getClass().getSimpleName(), rightV.toString());
							if(leftV instanceof InvokeExpr)
								invokes.add(((InvokeExpr)leftV).getMethodRef().getSignature());
							else if(leftV instanceof FieldRef)
								fields.add(((FieldRef)leftV).getFieldRef().getSignature());
							else
								other.add(leftV.toString());
							if(rightV instanceof InvokeExpr)
								invokes.add(((InvokeExpr)rightV).getMethodRef().getSignature());
							else if(rightV instanceof FieldRef)
								fields.add(((FieldRef)rightV).getFieldRef().getSignature());
							else
								other.add(rightV.toString());
						}
					}
				} else if(cp instanceof SwitchStmt) {
					Set<Value> keyValues = getAllValues(localDefs, source, cp, ((SwitchStmt)cp).getKey());
					for(Value key : keyValues) {
						exprs.add("switch (" + key.toString() + ")");
						if(!typeToExample.containsKey(key.getClass().getSimpleName()))
							typeToExample.put("Switch Key: " + key.getClass().getSimpleName(), key.toString());
						if(key instanceof InvokeExpr)
							invokes.add(((InvokeExpr)key).getMethodRef().getSignature());
						else if(key instanceof FieldRef)
							fields.add(((FieldRef)key).getFieldRef().getSignature());
						else
							other.add(key.toString());
					}
				} else {
					throw new RuntimeException("Error: CP '" + cp.getClass() + "' '" + cp + "' of '" 
							+ source.toString() + "' and '" + ep.toString() + "' is not an if or switch.");
				}
			}
		}
		epToTypeToExample.put(ep, SortingMethods.sortMapKey(typeToExample, SortingMethods.sComp));
		epToExprs.put(ep, SortingMethods.sortSet(exprs,SortingMethods.sComp));
		epToInvokes.put(ep, SortingMethods.sortSet(invokes,SortingMethods.sComp));
		epToFields.put(ep, SortingMethods.sortSet(fields,SortingMethods.sComp));
		epToOther.put(ep, SortingMethods.sortSet(other,SortingMethods.sComp));
	}
	
	private void dumpEp(PrintStream ps, Set<EntryPoint> eps, Map<EntryPoint,Set<String>> d) {
		for(EntryPoint ep : eps) {
			ps.println("EntryPoint: " + ep.toString() + " Size: " + d.get(ep).size());
			for(String s : d.get(ep)) {
				ps.println("  " + s);
			}
		}
	}
	
	private void dump(PrintStream ps, Set<String> set) {
		ps.println("Count: " + set.size());
		for(String s : set) {
			ps.println("  " + s);
		}
	}
	
	private boolean removeInvoke(String left, String right) {
		Matcher leftM = p.matcher(left);
		Matcher rightM = p.matcher(right);
		if(leftM.matches())
			left = leftM.group(1);
		if(rightM.matches())
			right = rightM.group(1);
		return sigs.contains(left) || sigs.contains(right);
	}
	
	/* Total unique CP considering local definitions back one level 
	 *   25433 : Total
	 *   18713 : No Null
	 *   18155 : No Null and lengthof
	 *   17047 : No Null, lengthof, size, hasNext, isEmpty
	 *   
	 *   
	 *  - android.support.v4.util.LongSparseArray: int mSize
	 *  - android.support.v4.util.SimpleArrayMap: int mSize
	 */
	
	private static final Pattern p = Pattern.compile("^[a-zA-Z0-9]+invoke\\s+(?:[^<()>.]+\\.|)(<.+:\\s+[^(]+\\([^)]*\\)>)\\(.*\\)$");
	private static final Set<String> sigs = new HashSet<>();
	
	static {
		sigs.addAll(HierarchyHelpers.getAllPossibleInvokeSignatures(Scene.v().getSootClass("java.util.Collection").getMethodByName("size")));
		sigs.addAll(HierarchyHelpers.getAllPossibleInvokeSignatures(Scene.v().getSootClass("java.util.Iterator").getMethodByName("hasNext")));
		sigs.addAll(HierarchyHelpers.getAllPossibleInvokeSignatures(Scene.v().getSootClass("java.util.Collection").getMethodByName("isEmpty")));
		sigs.addAll(HierarchyHelpers.getAllPossibleInvokeSignatures(Scene.v().getSootClass("java.util.Map").getMethodByName("isEmpty")));
		sigs.addAll(HierarchyHelpers.getAllPossibleInvokeSignatures(Scene.v().getSootClass("java.util.Map").getMethodByName("size")));
		sigs.addAll(HierarchyHelpers.getAllPossibleInvokeSignatures(Scene.v().getSootClass("android.util.SparseArray").getMethodByName("size")));
		sigs.addAll(HierarchyHelpers.getAllPossibleInvokeSignatures(Scene.v().getSootClass("android.util.SparseBooleanArray").getMethodByName("size")));
		sigs.addAll(HierarchyHelpers.getAllPossibleInvokeSignatures(Scene.v().getSootClass("android.util.SparseIntArray").getMethodByName("size")));
		sigs.add("<android.app.usage.TimeSparseArray: int size()>");
		sigs.addAll(HierarchyHelpers.getAllPossibleInvokeSignatures(Scene.v().getSootClass("java.lang.CharSequence").getMethodByName("length")));
	}
	
	public void add(Set<String> exprs, String type, String op, String left, String right) {
		type = type.trim();
		op = op.trim();
		left = left.trim();
		right = right.trim();
		if(!left.equals("null") && !right.equals("null") 
				&& !left.startsWith("lengthof") && !right.startsWith("lengthof")
				&& !left.contains(" instanceof ") && !right.contains(" instanceof ")
				&& !removeInvoke(left,right)
				) {
			exprs.add(type + left + " " + op + " " + right + ")");
		}
	}
	
	/*private final class DumbICFG implements IJimpleICFG {

		@Override
		public CallGraph getCallGraph() {
			return null;
		}

		@Override
		public SootMethod getMethodOf(Unit u) {
			return AnalyizeControlPredicates.this.getMethodOf(u);
		}

		@Override
		public List<Unit> getSuccsOf(Unit u) {
			return null;
		}

		@Override
		public UnitGraph getOrCreateUnitGraph(SootMethod m) {
			return AnalyizeControlPredicates.this.getOrCreateUnitGraph(m);
		}

		@Override
		public UnitGraph getOrCreateUnitGraph(Body body) {
			return AnalyizeControlPredicates.this.getOrCreateUnitGraph(body);
		}

		@Override
		public boolean isExitStmt(Unit u) {
			return false;
		}

		@Override
		public boolean isStartPoint(Unit u) {
			return false;
		}

		@Override
		public boolean isFallThroughSuccessor(Unit u, Unit succ) {
			return false;
		}

		@Override
		public boolean isBranchTarget(Unit u, Unit succ) {
			return false;
		}

		@Override
		public List<Value> getParameterRefs(SootMethod m) {
			return null;
		}

		@Override
		public Collection<Unit> getStartPointsOf(SootMethod m) {
			return null;
		}

		@Override
		public boolean isCallStmt(Unit u) {
			
			return false;
		}

		@Override
		public Set<Unit> allNonCallStartNodes() {
			
			return null;
		}

		@Override
		public Set<Unit> allNonCallEndNodes() {
			
			return null;
		}

		@Override
		public Collection<Unit> getReturnSitesOfCallAt(Unit u) {
			
			return null;
		}

		@Override
		public Set<Unit> getCallsFromWithin(SootMethod m) {
			
			return null;
		}

		@Override
		public List<Unit> getPredsOf(Unit u) {
			
			return null;
		}

		@Override
		public Collection<Unit> getEndPointsOf(SootMethod m) {
			
			return null;
		}

		@Override
		public List<Unit> getPredsOfCallAt(Unit u) {
			
			return null;
		}

		@Override
		public boolean isReturnSite(Unit n) {
			
			return false;
		}

		@Override
		public boolean isReachable(Unit u) {
			
			return false;
		}

		@Override
		public ControlDependenceGraph<Unit> getOrMakeControlDependenceGraph(SootMethod m) {
			
			return null;
		}

		@Override
		public ControlDependenceGraph<Unit> getOrMakeControlDependenceGraph(Body b) {
			
			return null;
		}

		@Override
		public AdvLocalDefs getOrMakeLocalDefs(SootMethod m) {
			return AnalyizeControlPredicates.this.getOrMakeLocalDefs(m);
		}

		@Override
		public AdvLocalDefs getOrMakeLocalDefs(Body b) {
			return AnalyizeControlPredicates.this.getOrMakeLocalDefs(b);
		}

		@Override
		public AdvLocalUses getOrMakeLocalUses(SootMethod m) {
			
			return null;
		}

		@Override
		public AdvLocalUses getOrMakeLocalUses(Body b) {
			
			return null;
		}

		@Override
		public Collection<SootMethod> getCalleesOfCallAt(Unit n) {
			
			return null;
		}

		@Override
		public Collection<SootMethod> getAllCalleesOfCallAt(Unit n) {
			
			return null;
		}

		@Override
		public Collection<Unit> getCallersOf(SootMethod m) {
			
			return null;
		}

		@Override
		public List<IdentityStmt> getParameterDefs(SootMethod m) {
			
			return null;
		}

		@Override
		public Set<DefinitionStmt> getAllFieldReadsAfter(Unit start, SootField f) {
			
			return null;
		}

		@Override
		public Map<SootField, Set<DefinitionStmt>> getAllFieldReadsAfter(Unit start) {
			
			return null;
		}

		@Override
		public Set<DefinitionStmt> getAllFieldReadsForMethod(SootMethod m, SootField f) {
			
			return null;
		}

		@Override
		public Map<SootField, Set<DefinitionStmt>> getAllFieldReadsForMethod(SootMethod m) {
			
			return null;
		}

		@Override
		public Set<DefinitionStmt> getFieldReadsForMethod(SootMethod m, SootField f) {
			
			return null;
		}

		@Override
		public Map<SootField, Set<DefinitionStmt>> getFieldReadsForMethod(SootMethod m) {
			
			return null;
		}

		@Override
		public Set<DefinitionStmt> getDefsForUsedLocals(Unit u) {
			
			return null;
		}

		@Override
		public Set<Unit> getAllEndPointsOfCalleesOfCallAt(Unit invoke) {
			
			return null;
		}

		@Override
		public Map<SootField, Set<DefinitionStmt>> getAllFieldReadsAt(Unit start) {
			
			return null;
		}

		@Override
		public Set<DefinitionStmt> getAllFieldReadsAt(Unit start, SootField f) {
			
			return null;
		}

		@Override
		public IBasicEdgePredicate getEdgePredicate() {
			
			return null;
		}

		@Override
		public Map<Local, Set<DefinitionStmt>> getDefsForUsedLocalsMap(Unit u) {
			return AnalyizeControlPredicates.this.getDefsForUsedLocals(u);
		}

		@Override
		public Set<Loop> getLoops(SootMethod source) {
			return null;
		}

		@Override
		public Set<Loop> getLoops(Body body) {
			return null;
		}

		@Override
		public Set<Stmt> getLoopHeaders(SootMethod source) {
			return null;
		}

		@Override
		public Set<Stmt> getLoopHeaders(Body body) {
			return null;
		}
		
	}*/
	
}
