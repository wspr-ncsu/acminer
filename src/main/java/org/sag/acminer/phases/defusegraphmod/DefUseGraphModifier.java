package org.sag.acminer.phases.defusegraphmod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sag.acminer.database.defusegraph.DefUseGraph;
import org.sag.acminer.database.defusegraph.IFieldNode;
import org.sag.acminer.database.defusegraph.IInvokeNode;
import org.sag.acminer.database.defusegraph.INode;
import org.sag.acminer.database.defusegraph.LeafNode;
import org.sag.acminer.database.defusegraph.LocalWrapper;
import org.sag.acminer.database.defusegraph.StartNode;
import org.sag.acminer.database.defusegraph.id.ConstantPart;
import org.sag.acminer.database.defusegraph.id.Identifier;
import org.sag.acminer.database.defusegraph.id.Part;
import org.sag.acminer.phases.defusegraph.DefUseGraphMaker;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.concurrent.IgnorableRuntimeException;
import org.sag.common.concurrent.SimpleValueRunner;
import org.sag.common.concurrent.SimpleValueWorkerGroup;
import org.sag.common.concurrent.ValueRunner;
import org.sag.common.concurrent.ValueWorkerFactory;
import org.sag.common.concurrent.ValueWorkerGroup;
import org.sag.common.concurrent.WorkerCountingThreadExecutor;
import org.sag.common.logging.ILogger;
import org.sag.soot.callgraph.IJimpleICFG;

import soot.Local;
import soot.Unit;
import soot.Value;
import soot.jimple.Constant;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

public class DefUseGraphModifier {
	
	private final String name;
	private final WorkerCountingThreadExecutor exe;
	private final DefUseGraphMaker maker;
	private final boolean keepInstanceObject;
	
	public DefUseGraphModifier() {
		this(new WorkerCountingThreadExecutor(new ValueWorkerFactory<>()));
	}
	
	public DefUseGraphModifier(WorkerCountingThreadExecutor exe) {
		this.name = getClass().getSimpleName();
		this.exe = exe;
		this.maker = new DefUseGraphMaker(exe); //inits the localwrapper and inode factories
		keepInstanceObject = false;
	}
	
	public boolean shutdownWhenFinished() {
		//calls shutdownWhenFinished on the exe
		//clears the LocalWrapper, INode, and IdentifierGenerator cache
		return maker.shutdownWhenFinished();
	}
	
	public List<Throwable> getAndClearExceptions() {
		return exe.getAndClearExceptions();
	}
	
	private synchronized <O> void executeRunner(ValueRunner<O> runner, ValueWorkerGroup<?,O> g, ILogger logger) {
		try {
			exe.execute(runner,g);
		} catch(Throwable t) {
			logger.fatal("{}: Failed to execute '{}' for group '{}'.",t,name,runner.toString(),g.getName());
			throw new IgnorableRuntimeException();
		}
	}
	
	private synchronized <O> void executeRunners(Iterable<ValueRunner<O>> runners, ILogger logger) {
		for(ValueRunner<O> runner : runners) {
			try {
				exe.execute(runner);
			} catch(Throwable t) {
				logger.fatal("{}: Failed to execute '{}'.",t,name,runner.toString());
				throw new IgnorableRuntimeException();
			}
		}
	}
	
	public DefUseGraph modDefUseGraph(Set<Unit> markedData, EntryPoint ep, IJimpleICFG icfg, ILogger logger) {
		try {
			logger.fineInfo("{}: Modifying def use graph for ep '{}' with '{}' marked data entries.",name,ep,markedData.size());
			
			logger.fineInfo("{}: Building new def use graph for the makred data of ep '{}'.",name,ep);
			DefUseGraph graph = maker.makeDefUseGraphs(markedData, ep, icfg, false, logger);
			logger.fineInfo("{}: Successfully built the def use graph for the marked data of ep '{}'.",name,ep);
			
			logger.fineInfo("{}: Removing instance objects from def use graph for ep '{}'.",name,ep);
			
			Set<INode> seen = Collections.<INode>synchronizedSet(new HashSet<INode>());
			SimpleValueWorkerGroup g = new SimpleValueWorkerGroup(name,ep.toString()+"_modDefUseGraph",logger,false,false);
			for(StartNode startNode : graph.getStartNodes()) {
				executeRunner(new ModDefUseGraphRunner(ep, startNode, graph, seen, logger), g, logger);
			}
			g.unlockInitialLock();
			g.getReturnValue();//block
			
			if(g.shutdownNormally() && !g.hasExceptions()) {
				//The clone and modify method makes sure that the start nodes returned are unique for this pre-processing phase
				//Since we are cloning, there will of course be one equal to  the returned node but different in the original graph
				//However, multiple calls to the INode factory will return the same nodes for this pre-processing phase (they are just
				//different objects when compared to the original graph)
				//For good reason since we are changing the basic string of each
				List<StartNode> startNodes = new ArrayList<>();
				for(StartNode sn : graph.getStartNodes()) {
					startNodes.add((StartNode)INode.Factory.modifyNode(sn, keepInstanceObject));
				}
				
				DefUseGraph outGraph = new DefUseGraph(startNodes);
				copyDataIntoNewGraph(graph, outGraph, seen, startNodes);
				
				logger.fineInfo("{}: Successfully removed instance objects from def use graph for ep '{}'. The graph "
						+ "has been successfully modified.",name,ep);
				return outGraph;
			} else {
				logger.fatal("{}: Failed to remove the instance objects from the def use graphs for group '{}'.",name,g.getName());
				throw new IgnorableRuntimeException();
			}
		} catch(IgnorableRuntimeException t) {
			throw t;
		} catch(Throwable t) {
			logger.fatal("{}: An unexpected error occured while modifying the def use graphs for ep '{}'.",t,name,ep);
			throw new IgnorableRuntimeException();
		}
	}
	
	private void copyDataIntoNewGraph(DefUseGraph oldGraph, DefUseGraph newGraph, Set<INode> nodes, List<StartNode> newStartNodes) {
		Set<INode> newnodes = new HashSet<>();
		newnodes.addAll(newStartNodes);
		for(INode cur : nodes) {
			Map<LocalWrapper, Set<INode>> data = oldGraph.getChildLocalWrappersToChildNodes(cur);
			Local invokingObject = getInvokingObject(cur, keepInstanceObject);
			INode newCur = INode.Factory.modifyNode(cur, invokingObject == null);
			newnodes.add(newCur);
			
			for(LocalWrapper lw : data.keySet()) {
				if(invokingObject == null || !lw.getLocal().equals(invokingObject)) {
					for(INode child : data.get(lw)) {
						if(nodes.contains(child)) {
							Local ioChild = getInvokingObject(child, keepInstanceObject);
							INode newchildnode = INode.Factory.modifyNode(child, ioChild == null);
							newGraph.addChild(newCur, lw, newchildnode);
							newnodes.add(newchildnode);
						}
					}
				}
			}
		}
		for(INode newnode : newnodes) {
			if(!(newnode instanceof LeafNode) || !(newnode.getValue() instanceof Constant)) {
				//Only want inline constants for non-leaf nodes and leaf nodes that are not constants
				Identifier id = newnode.getIdentifier();
				for(int i = 0; i < id.size(); i++) {
					Part p = id.get(i);
					if(p instanceof ConstantPart) {
						newGraph.addInlineConstantNode(newnode, INode.Factory.modifyInlineConstantLeafNode(newnode, i));
					}
				}
			}
		}
	}
	
	public static Local getInvokingObject(INode cur, boolean keepInstanceObject) {
		Local ret = null;
		if(!keepInstanceObject) {
			if(cur instanceof IInvokeNode) {
				InvokeExpr ir = ((Stmt)(cur.getUnit())).getInvokeExpr();
				if(ir instanceof InstanceInvokeExpr) {
					InstanceInvokeExpr iir = ((InstanceInvokeExpr)ir);
					Value v = iir.getBase();
					if(v instanceof Local) {
						if(!iir.getMethodRef().getSubSignature().getString().equals("boolean equals(java.lang.Object)"))
							ret = (Local)v;
					}
				}
			} else if(cur instanceof IFieldNode) {
				FieldRef fr = ((Stmt)cur.getUnit()).getFieldRef();
				if(fr instanceof InstanceFieldRef) {
					Value v = ((InstanceFieldRef)fr).getBase();
					ret = v instanceof Local ? (Local)v : null;
				}
			}
		}
		return ret;
	}
	
	private class ModDefUseGraphRunner extends SimpleValueRunner {
		
		private final EntryPoint ep;
		private final INode cur;
		private final DefUseGraph graph;
		private final Set<INode> seen;
		private final ILogger logger;
		private final List<ValueRunner<Object>> runners;

		public ModDefUseGraphRunner(EntryPoint ep, INode cur, DefUseGraph graph, Set<INode> seen, ILogger logger) {
			this.ep = ep;
			this.graph = graph;
			this.cur = cur;
			this.seen = seen;
			this.logger = logger;
			this.runners = new ArrayList<>();
		}

		@Override
		public void run() {
			if(seen.add(cur)) {
				Local invokingObject = getInvokingObject(cur, keepInstanceObject);
				Map<Local, Set<INode>> data = graph.getChildLocalsToChildNodes(cur);
				for(Local l : data.keySet()) {
					if(invokingObject == null || !l.equals(invokingObject)) {
						for(INode n : data.get(l)) {
							runners.add(new ModDefUseGraphRunner(ep, n, graph, seen, logger));
						}
					}
				}
				
				if(!runners.isEmpty()) {
					executeRunners(runners, logger);
					runners.clear();
				}
			}
		}
		
	}
	
	

}
