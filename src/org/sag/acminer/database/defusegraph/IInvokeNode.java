package org.sag.acminer.database.defusegraph;

import soot.SootMethod;
import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("IInvokeNode")
public interface IInvokeNode extends INode {

	/** Returns a target SootMethod of the invoke expression contained in
	 * the unit of this ValueNode. If there are no target SootMethod's then
	 * this method will return null.
	 */
	public SootMethod getTarget();
	
}
