package org.sag.acminer.database.filter;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.database.defusegraph.DefUseGraph;
import org.sag.acminer.database.defusegraph.StartNode;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.soot.callgraph.IJimpleICFG;

import soot.SootMethod;
import soot.jimple.Stmt;

public interface IData {
	
	public interface IStmtData extends IData {
		public Stmt getStmt();
		public SootMethod getSource();
	}
	
	public interface IJimpleICFGData extends IData {
		public IJimpleICFG getJimpleICFG();
	}
	
	public interface IDefUseGraphData extends IData {
		public StartNode getStartNode();
		public DefUseGraph getDefUseGraph();
	}
	
	public interface IDataAccessorData extends IData {
		public IACMinerDataAccessor getDataAccessor();
	}
	
	public interface IEntryPointData extends IData {
		public EntryPoint getEntryPoint();
	}
	
	public interface IMethodData extends IData {
		public SootMethod getMethod();
		public String getMethodSignature();
	}
	
}