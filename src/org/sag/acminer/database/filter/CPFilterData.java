package org.sag.acminer.database.filter;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.database.defusegraph.DefUseGraph;
import org.sag.acminer.database.defusegraph.StartNode;
import org.sag.acminer.database.filter.IData.*;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.soot.callgraph.IJimpleICFG;

import soot.SootMethod;
import soot.jimple.Stmt;

public class CPFilterData implements IStmtData, IJimpleICFGData, IDefUseGraphData, IDataAccessorData, IEntryPointData {

	private final EntryPoint ep;
	private final IACMinerDataAccessor dataAccessor;
	private final IJimpleICFG icfg;
	private final Stmt cp;
	private final SootMethod cpSource;
	private final StartNode sn;
	private final DefUseGraph vt;
	
	public CPFilterData(EntryPoint ep, IACMinerDataAccessor dataAccessor, IJimpleICFG icfg, Stmt cp, SootMethod cpSource, StartNode sn, DefUseGraph vt) {
		this.ep = ep;
		this.dataAccessor = dataAccessor;
		this.icfg = icfg;
		this.cp = cp;
		this.cpSource = cpSource;
		this.sn = sn;
		this.vt = vt;
	}

	@Override
	public Stmt getStmt() {
		return cp;
	}

	@Override
	public SootMethod getSource() {
		return cpSource;
	}

	@Override
	public DefUseGraph getDefUseGraph() {
		return vt;
	}
	
	@Override
	public StartNode getStartNode() {
		return sn;
	}

	@Override
	public IJimpleICFG getJimpleICFG() {
		return icfg;
	}

	@Override
	public IACMinerDataAccessor getDataAccessor() {
		return dataAccessor;
	}

	@Override
	public EntryPoint getEntryPoint() {
		return ep;
	}
	
}
