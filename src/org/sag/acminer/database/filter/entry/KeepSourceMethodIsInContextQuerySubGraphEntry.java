package org.sag.acminer.database.filter.entry;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import org.sag.acminer.database.accesscontrol.IContextQueryDatabase;
import org.sag.acminer.database.filter.IData;
import org.sag.acminer.database.filter.IData.IDataAccessorData;
import org.sag.acminer.database.filter.IData.IEntryPointData;
import org.sag.acminer.database.filter.IData.IStmtData;
import org.sag.acminer.phases.entrypoints.EntryPoint;

import soot.SootMethod;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

@XStreamAlias("KeepSourceMethodIsInContextQuerySubGraph")
public class KeepSourceMethodIsInContextQuerySubGraphEntry implements IEntry {

	public static final String name = "KeepSourceMethodIsInContextQuerySubGraph";
	
	@XStreamAlias("NoSubGraphs")
	@XStreamAsAttribute
	private final boolean noSubGraphs;
	
	public KeepSourceMethodIsInContextQuerySubGraphEntry(boolean noSubGraphs) {
		this.noSubGraphs = noSubGraphs;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public boolean eval(IData data) {
		return evalInner(data, null, null);
	}
	
	@Override
	public boolean evalDebug(IData data, StringBuilder sb, AtomicInteger c) {
		int curC = 0;
		if(sb != null) {
			curC = c.get();
			sb.append("Start Eval ").append(name).append(" ").append(curC).append("\n");
		}
		boolean ret = evalInner(data, sb, c);
		if(sb != null)
			sb.append("End Eval ").append(name).append(" ").append(curC).append(" ").append("\nResult: ").append(ret).append("\n");
		return ret;
	}

	/** Returns true if the provided statement is a member of a method who is either a context query
	 * or a method that is in the subgraph of a context query and false otherwise. This method returns
	 * false if the data provided is not an instance of and IEntryPointData, IDataAccessorData, and 
	 * IStmtData object.
	 */
	private boolean evalInner(IData data, StringBuilder sb, AtomicInteger c) {
		if(data instanceof IEntryPointData && data instanceof IDataAccessorData && data instanceof IStmtData) {
			IContextQueryDatabase cqdb = ((IDataAccessorData)data).getDataAccessor().getContextQueriesDB();
			EntryPoint ep = ((IEntryPointData)data).getEntryPoint();
			SootMethod sm = ((IStmtData)data).getSource();
			if(noSubGraphs)
				return cqdb.isContextQuery(ep, sm);
			return cqdb.isSubGraphMethod(ep, sm);
		}
		return false;
	}
	
	@Override
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof KeepSourceMethodIsInContextQuerySubGraphEntry))
			return false;
		return true;
	}
	
	@Override
	public int hashCode() {
		return 527 + name.hashCode();
	}
	
	@Override
	public String toString() {
		return Factory.genSig(name, Collections.<String>emptyList());
	}

}
