package org.sag.acminer.database.defusegraph;

import java.io.ObjectStreamException;
import java.nio.file.Path;
import java.util.Objects;

import org.sag.acminer.database.defusegraph.INode.Factory.XStreamSetup;
import org.sag.acminer.database.defusegraph.id.Identifier;
import org.sag.soot.SootSort;
import org.sag.soot.xstream.SootUnitContainer;
import org.sag.soot.xstream.SootUnitContainerFactory;
import org.sag.sootinit.SootInstanceWrapper;
import org.sag.xstream.XStreamInOut;

import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.DefinitionStmt;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("AbstractNode")
public class AbstractNode implements INode {

	@XStreamOmitField
	private SootMethod source;
	@XStreamOmitField
	private Unit unit;
	
	@XStreamAlias("Unit")
	private SootUnitContainer unitContainer;
	@XStreamAlias("Identifier")
	protected Identifier identifier;
	
	protected AbstractNode() {}
	
	public AbstractNode(SootMethod source, Unit unit, Identifier identifier) {
		this.source = source;
		this.unit = unit;
		this.unitContainer = null;
		this.identifier = identifier;
	}
	
	//ReadResolve is always run when reading from XML even if a constructor is run first
	protected Object readResolve() throws ObjectStreamException {
		if(!SootInstanceWrapper.v().isAnySootInitValueSet())
			throw new RuntimeException("Error: Soot needs to be initilized before loading.");
		source = unitContainer.getSource().toSootMethod();
		unit = unitContainer.toUnit();
		if(!unit.toString().equals(unitContainer.getSignature())) //Sanity check because local names need to be consistent
			throw new RuntimeException("Error: The read in unit signature '" + unitContainer.getSignature() + 
					"' does not match the stored unit signature '" + unit.toString() + "' of '" + source.toString() + ".");
		identifier.initSootComponents(unit);
		return this;
	}
	
	protected Object writeReplace() throws ObjectStreamException {
		if(unitContainer == null)
			unitContainer = SootUnitContainerFactory.makeSootUnitContainer(unit, source);	
		return this;
	}
	
	@Override
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null ||  !this.getClass().isInstance(o))
			return false;
		AbstractNode other = (AbstractNode)o;
		return Objects.equals(this.source, other.source) && Objects.equals(this.unit, other.unit);
	}
	
	@Override
	public String toString() {
		return getIdentifier().toString();
	}
	
	@Override
	public int hashCode() {
		int i = 17;
		i = i * 31 + Objects.hashCode(source);
		i = i * 31 + Objects.hashCode(unit);
		return i;
	}
	
	@Override
	public int compareTo(INode o) {
		int ret = SootSort.smComp.compare(this.getSource(), o.getSource());
		if(ret == 0) {
			ret = SootSort.unitComp.compare(this.getUnit(), o.getUnit());
			if(ret == 0) {
				if(this.getClass().equals(o.getClass()))
					ret = 0;
				ret = this.getClass().getName().compareTo(o.getClass().getName());
			}	
		}
		return ret;
	}
	
	@Override
	public SootMethod getSource() {
		return source;
	}

	@Override
	public Unit getUnit() {
		return unit;
	}
	
	/* All units are DefinitionStmts except the start unit because
	 * we are traversing backwards to find all possible defs and only
	 * recoding the defs in the tree. The start node has to be handled
	 * separately.
	 */
	@Override
	public Value getValue() {
		if(unit instanceof DefinitionStmt)
			return ((DefinitionStmt)unit).getRightOp();
		return null;
	}
	
	@Override
	public Identifier getIdentifier() {
		return identifier;
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public AbstractNode readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}

	public static XStreamSetup getXStreamSetupStatic(){
		return Factory.getXStreamSetupStatic();
	}
	
	@Override
	public XStreamSetup getXStreamSetup() {
		return getXStreamSetupStatic();
	}
	
}
