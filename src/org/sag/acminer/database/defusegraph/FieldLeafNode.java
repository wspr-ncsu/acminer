package org.sag.acminer.database.defusegraph;

import java.io.ObjectStreamException;
import java.nio.file.Path;
import java.util.Objects;

import org.sag.acminer.database.defusegraph.id.Identifier;
import org.sag.soot.SootSort;
import org.sag.soot.xstream.SootFieldContainer;
import org.sag.xstream.XStreamInOut;

import soot.SootField;
import soot.SootMethod;
import soot.Unit;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("FieldLeafNode")
public class FieldLeafNode extends LeafNode implements IFieldNode {

	@XStreamOmitField
	private SootField field;
	
	@XStreamAlias("Field")
	private SootFieldContainer fieldContainer;

	public FieldLeafNode(SootMethod source, Unit unit, SootField field, Identifier identifier) {
		super(source, unit, identifier);
		this.field = field;
		this.fieldContainer = null;
	}

	//ReadResolve is always run when reading from XML even if a constructor is run first
	protected Object readResolve() throws ObjectStreamException {
		super.readResolve();
		if(fieldContainer != null)
			field = fieldContainer.toSootFieldAllowPhantom();
		else
			field = null;
		return this;
	}
	
	protected Object writeReplace() throws ObjectStreamException {
		super.writeReplace();
		if(fieldContainer == null && field != null)
			fieldContainer = SootFieldContainer.makeSootMethodContainer(field);
		return this;
	}
	
	@Override
	public SootField getField() {
		return field;
	}
	
	@Override
	public FieldLeafNode readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	@Override
	public boolean equals(Object o) {
		if(super.equals(o) && o instanceof FieldLeafNode) {
			return Objects.equals(field, ((FieldLeafNode)o).field);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		int i = 17;
		i = i * 31 + super.hashCode();
		i = i * 31 + Objects.hashCode(field);
		return i;
	}
	
	@Override
	public int compareTo(INode o) {
		int ret = super.compareTo(o);
		if(ret == 0)
			ret = SootSort.sfComp.compare(field, ((FieldLeafNode)o).field);
		return ret;
	}
	
}
