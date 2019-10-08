package org.sag.acminer.database.acminer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.sag.common.tools.SortingMethods;
import org.sag.soot.SootSort;
import org.sag.soot.xstream.SootMethodContainer;
import org.sag.soot.xstream.SootUnitContainer;
import org.sag.xstream.XStreamInOut;
import org.sag.xstream.XStreamInOut.XStreamInOutInterface;
import org.sag.xstream.xstreamconverters.NamedCollectionConverterWithSize;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import soot.SootMethod;
import soot.Unit;

@XStreamAlias("SourceMethod")
public final class SourceMethod implements XStreamInOutInterface {
	
	@XStreamAlias("SootMethodContainer")
	private volatile SootMethodContainer method;
	
	@XStreamAlias("Units")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"SourceUnit"},types={SourceUnit.class})
	private volatile ArrayList<SourceUnit> units;
	
	private SourceMethod() {}
	
	public SourceMethod(SootMethodContainer method, Map<String,SootUnitContainer> units) {
		this.method = method;
		this.units = new ArrayList<>();
		units = SortingMethods.sortMapValueAscending(units);
		for(String stmt : units.keySet()) {
			this.units.add(new SourceUnit(units.get(stmt),stmt));
		}
	}
	
	public SourceMethod(SootMethod method, Map<String,Unit> units) {
		this.method = SootMethodContainer.makeSootMethodContainer(method);
		this.units = new ArrayList<>();
		Map<String,Unit> temp = SortingMethods.sortMapValue(units, SootSort.unitComp);
		for(String stmt : temp.keySet()) {
			Unit u = temp.get(stmt);
			this.units.add(new SourceUnit(method,u,stmt));
		}
	}
	
	public boolean equals(Object o){
		if(this == o)
			return true;
		if(o == null || !(o instanceof SourceMethod))
			return false;
		SourceMethod other = (SourceMethod)o;
		return Objects.equals(method, other.method) && Objects.equals(units, other.units);
	}
	
	public int hashCode(){
		int hash = 17;
		hash = 31 * hash + Objects.hashCode(method);
		hash = 31 * hash + Objects.hashCode(units);
		return hash;
	}
	
	public Map<String,SootUnitContainer> getUnits() {
		Map<String,SootUnitContainer> ret = new LinkedHashMap<>();
		for(SourceUnit uc : units) {
			ret.put(uc.getStmt(), uc.getUnit());
		}
		return ret;
	}
	
	public Map<String,Unit> getSootUnits() {
		Map<String,Unit> ret = new LinkedHashMap<>();
		for(SourceUnit uc : units) {
			ret.put(uc.getStmt(),uc.getSootUnit());
		}
		return ret;
	}
	
	public SootMethodContainer getMethod() {
		return method;
	}
	
	public SootMethod getSootMethod() {
		return method.toSootMethod();
	}

	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public SourceMethod readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}

	public static SourceMethod readXMLStatic(String filePath, Path path) throws Exception {
		return new SourceMethod().readXML(filePath, path);
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
				SootMethodContainer.getXStreamSetupStatic().getOutputGraph(in);
				SourceUnit.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(SourceMethod.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}

}
