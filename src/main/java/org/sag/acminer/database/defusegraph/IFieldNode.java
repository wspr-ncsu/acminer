package org.sag.acminer.database.defusegraph;

import soot.SootField;
import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("IFieldNode")
public interface IFieldNode extends INode {

	public SootField getField();
	
}
