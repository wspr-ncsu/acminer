package org.sag.acminer.database.defusegraph;

import java.io.ObjectStreamException;
import java.nio.file.Path;
import java.util.Objects;

import org.sag.acminer.database.defusegraph.id.ConstantPart;
import org.sag.acminer.database.defusegraph.id.Identifier;
import org.sag.acminer.database.defusegraph.id.IdentifierWrapper;
import org.sag.xstream.XStreamInOut;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.Constant;

@XStreamAlias("InlineConstantLeafNode")
public class InlineConstantLeafNode extends LeafNode {
	
	@XStreamAlias("MainNode")
	private volatile INode node;
	@XStreamAlias("Index")
	private volatile int index;

	protected InlineConstantLeafNode(INode node, int index) {
		super(null, null, null);
		this.node = node;
		this.index = index;
	}
	
	@Override
	protected Object readResolve() throws ObjectStreamException {
		return this;
	}

	@Override
	protected Object writeReplace() throws ObjectStreamException {
		return this;
	}
	
	@Override
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof InlineConstantLeafNode))
			return false;
		InlineConstantLeafNode lf = (InlineConstantLeafNode)o;
		return index == lf.index && Objects.equals(node, lf.node);
	}
	
	@Override
	public int hashCode() {
		int i = 17;
		i = i * 31 + index;
		i = i * 31 + Objects.hashCode(node);
		return i;
	}
	
	@Override
	public int compareTo(INode o) {
		int ret = super.compareTo(o);
		if(ret == 0)
			ret = Integer.compare(index, ((InlineConstantLeafNode)o).index);
		return ret;
	}

	@Override
	public String toString() {
		return getValue().toString();
	}
	
	@Override
	public Constant getValue() {
		return ((ConstantPart)(node.getIdentifier().get(index))).getValue();
	}
	
	public int getIndex() {
		return index;
	}
	
	public INode getNode() {
		return node;
	}
	
	@Override
	public SootMethod getSource() {
		return node.getSource();
	}

	@Override
	public Unit getUnit() {
		return node.getUnit();
	}
	
	@Override
	public Identifier getIdentifier() {
		return new IdentifierWrapper(node.getIdentifier(), index);
	}
	
	@Override
	public InlineConstantLeafNode readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
}
