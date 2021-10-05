package org.sag.acminer.database.acminer;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.sag.common.xstream.XStreamInOut;
import org.sag.common.xstream.XStreamInOut.XStreamInOutInterface;
import org.sag.soot.xstream.SootUnitContainer;
import org.sag.soot.xstream.SootUnitContainerFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import soot.SootMethod;
import soot.Unit;

@XStreamAlias("SourceUnit")
public class SourceUnit implements XStreamInOutInterface {
	
	@XStreamAlias("Stmt")
	private volatile String stmt;
	
	@XStreamAlias("SootUnitContainer")
	private volatile SootUnitContainer unit;
	
	private SourceUnit() {}
	
	public SourceUnit(SootUnitContainer unit, String stmt) {
		this.unit = unit;
		this.stmt = stmt;
	}
	
	public SourceUnit(SootMethod source, Unit unit, String stmt) {
		this.unit = SootUnitContainerFactory.makeSootUnitContainer(unit, source);
		this.stmt = stmt;
	}
	
	public boolean equals(Object o){
		if(this == o)
			return true;
		if(o == null || !(o instanceof SourceUnit))
			return false;
		SourceUnit other = (SourceUnit)o;
		return Objects.equals(unit, other.unit);
	}
	
	public int hashCode(){
		int hash = 17;
		hash = 31 * hash + Objects.hashCode(unit);
		return hash;
	}
	
	public SootUnitContainer getUnit() {
		return unit;
	}
	
	public Unit getSootUnit() {
		return unit.toUnit();
	}
	
	public String getStmt() {
		return stmt;
	}

	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public SourceUnit readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}

	public static SourceUnit readXMLStatic(String filePath, Path path) throws Exception {
		return new SourceUnit().readXML(filePath, path);
	}
	
	@XStreamOmitField
	private static AbstractXStreamSetup xstreamSetup = null;

	public static AbstractXStreamSetup getXStreamSetupStatic(){
		if(xstreamSetup == null)
			xstreamSetup = new XStreamSetup();
		return xstreamSetup;
	}
	
	@Override
	public AbstractXStreamSetup getXStreamSetup() {
		return getXStreamSetupStatic();
	}
	
	public static class XStreamSetup extends AbstractXStreamSetup {
		
		@Override
		public void getOutputGraph(LinkedHashSet<AbstractXStreamSetup> in) {
			if(!in.contains(this)) {
				in.add(this);
				SootUnitContainerFactory.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(SourceUnit.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}

}
