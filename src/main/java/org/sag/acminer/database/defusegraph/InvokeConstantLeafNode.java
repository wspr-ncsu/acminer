package org.sag.acminer.database.defusegraph;

import java.nio.file.Path;
import java.util.Objects;

import org.sag.acminer.database.defusegraph.id.Identifier;
import org.sag.common.xstream.XStreamInOut;

import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.Constant;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("InvokeConstantLeafNode")
public class InvokeConstantLeafNode extends LeafNode {

	@XStreamAlias("Index")
	private int index;

	public InvokeConstantLeafNode(SootMethod source, Unit unit, int index, Identifier identifier) {
		super(source, unit, identifier);
		this.index = index;
	}
	
	@Override
	public Constant getValue() {
		return valueFromUnit(getSource(),getUnit(),index);
	}
	
	public int getIndex() {
		return index;
	}
	
	protected static String getBasicString(SootMethod source, Unit unit, int index, boolean calledFromMod) {
		Constant c = valueFromUnit(source,unit,index);
		if(calledFromMod && c instanceof NullConstant)
			return org.sag.acminer.phases.acminer.dw.NullConstant.val;
		return c.toString();
	}
	
	public static Constant valueFromUnit(SootMethod source, Unit unit, int index) {
		Value v = ((Stmt)unit).getInvokeExpr().getArg(index);
		if(!(v instanceof Constant))
			throw new RuntimeException("Error: '" + v.toString() + "' type '" + v.getClass().getSimpleName() 
					+ "' of unit '" + unit + "' of '" + source + "' for index '" + index + "' is not a constant!?!");
		return (Constant)v;
	}
	
	@Override
	public InvokeConstantLeafNode readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	@Override
	public boolean equals(Object o) {
		if(super.equals(o) && o instanceof InvokeConstantLeafNode) {
			return index == ((InvokeConstantLeafNode)o).index;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		int i = 17;
		i = i * 31 + super.hashCode();
		i = i * 31 + Objects.hashCode(index);
		return i;
	}
	
	@Override
	public int compareTo(INode o) {
		int ret = super.compareTo(o);
		if(ret == 0)
			ret = Integer.compare(index, ((InvokeConstantLeafNode)o).index);
		return ret;
	}
	
}
