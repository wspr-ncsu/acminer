package org.sag.soot.graphtools.formatter;

import java.nio.file.Path;
import java.util.Collection;
import java.util.TreeSet;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.database.excludedelements.IExcludeHandler;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.graphtools.AlEdge;
import org.sag.common.graphtools.AlNode;
import org.sag.common.graphtools.Formatter;
import org.sag.common.graphtools.Transformer;

import soot.SootClass;
import soot.SootMethod;

public abstract class AbstractSootCGFormatter<A> extends Formatter {

	protected final int depth;
	protected final SootMethod ep;
	protected final SootClass stub;
	protected final Collection<AlNode> nodes;
	protected final Collection<AlEdge> edges;
	protected final Transformer<A> trans;
	protected final IACMinerDataAccessor dataAccessor;
	protected final IExcludeHandler excludeHandler;
	
	public AbstractSootCGFormatter(SootClass stub, SootMethod ep, Transformer<A> trans, int depth, long nodeColorIndex, Path outputPath, 
			IACMinerDataAccessor dataAccessor) {
		this(stub,ep,trans,depth,nodeColorIndex,-1,-1,-1,outputPath,dataAccessor);
	}
	
	public AbstractSootCGFormatter(SootClass stub, SootMethod ep, Transformer<A> trans, int depth, long nodeColorIndex, long nodeShapeIndex, 
			long edgeColorIndex, long nodeExtraDataIndex, Path outputPath, IACMinerDataAccessor dataAccessor) {
		super(nodeColorIndex,nodeShapeIndex,edgeColorIndex,nodeExtraDataIndex,outputPath);
		this.depth = depth;
		this.trans = trans;
		this.ep = ep;
		this.stub = stub;
		this.nodes = new TreeSet<AlNode>();
		this.edges = new TreeSet<AlEdge>();
		this.dataAccessor = dataAccessor;
		this.excludeHandler = dataAccessor.getExcludedElementsDB().createNewExcludeHandler(new EntryPoint(ep,stub));
	}
	
	@Override
	public Collection<AlNode> getNodes(){
		return nodes;
	}
	
	@Override
	public Collection<AlEdge> getEdges(){
		return edges;
	}
	
	@Override
	public String getComment() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.getComment());
		sb.append("  Type: ").append(getType()).append("\n");
		sb.append("  Stub: ").append(stub.toString()).append("\n");
		sb.append("  Entry Point: ").append(ep.toString()).append("\n");
		return sb.toString();
	}
	
	public abstract String getType();
	
	protected boolean conditional(SootMethod tgt){
		if(isExcluded(tgt)){
			return true;
		}
		return false;
	}
	
	protected boolean isExcluded(SootMethod tgt){
		return excludeHandler.isExcludedMethodWithOverride(tgt);
	}
	
}
