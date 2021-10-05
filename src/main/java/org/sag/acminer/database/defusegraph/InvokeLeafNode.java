package org.sag.acminer.database.defusegraph;

import java.io.ObjectStreamException;
import java.nio.file.Path;
import java.util.Objects;

import org.sag.acminer.database.defusegraph.id.Identifier;
import org.sag.common.xstream.XStreamInOut;
import org.sag.soot.SootSort;
import org.sag.soot.xstream.SootMethodContainer;

import soot.SootMethod;
import soot.Unit;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("InvokeLeafNode")
public class InvokeLeafNode extends LeafNode implements IInvokeNode {

	@XStreamOmitField
	private SootMethod target;
	
	@XStreamAlias("Target")
	private SootMethodContainer targetContainer;

	public InvokeLeafNode(SootMethod source, Unit unit, SootMethod target, Identifier identifier) {
		super(source, unit, identifier);
		this.target = target;
		this.targetContainer = null;
	}

	//ReadResolve is always run when reading from XML even if a constructor is run first
	protected Object readResolve() throws ObjectStreamException {
		super.readResolve();
		if(targetContainer != null)
			target = targetContainer.toSootMethodAllowPhantom();
		else
			target = null;
		return this;
	}
	
	protected Object writeReplace() throws ObjectStreamException {
		super.writeReplace();
		if(targetContainer == null && target != null)
			targetContainer = SootMethodContainer.makeSootMethodContainer(target);
		return this;
	}
	
	@Override
	public SootMethod getTarget() {
		return target;
	}
	
	@Override
	public InvokeLeafNode readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	@Override
	public boolean equals(Object o) {
		if(super.equals(o) && o instanceof InvokeLeafNode) {
			return Objects.equals(target, ((InvokeLeafNode)o).target);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		int i = 17;
		i = i * 31 + super.hashCode();
		i = i * 31 + Objects.hashCode(target);
		return i;
	}
	
	@Override
	public int compareTo(INode o) {
		int ret = super.compareTo(o);
		if(ret == 0)
			ret = SootSort.smComp.compare(target, ((InvokeLeafNode)o).target);
		return ret;
	}
	
}
