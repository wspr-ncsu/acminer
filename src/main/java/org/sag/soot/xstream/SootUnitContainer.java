package org.sag.soot.xstream;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.sag.common.tools.SortingMethods;
import org.sag.common.xstream.XStreamInOut;
import org.sag.common.xstream.XStreamInOut.XStreamInOutInterface;
import org.sag.main.sootinit.SootInstanceWrapper;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import soot.SootMethod;
import soot.Unit;

@XStreamAlias("SootUnitContainer")
public class SootUnitContainer implements XStreamInOutInterface, Comparable<SootUnitContainer> {

	@XStreamAlias("Signature")
	@XStreamAsAttribute
	private String signature;

	@XStreamAlias("SourceMethod")
	private SootMethodContainer source;
	
	@XStreamAlias("Index")
	private int index;
	
	//for reading in from xml only
	protected SootUnitContainer(){}
	
	//Go through the factory to get a new object
	protected SootUnitContainer(Unit unit, SootMethod source) {
		if(unit == null || source == null)
			throw new IllegalArgumentException("A unit and source method must be given.");
		this.signature = unit.toString();
		this.source = SootMethodContainer.makeSootMethodContainer(source);
		
		int i = 0;
		boolean found = false;
		for(Unit u : source.retrieveActiveBody().getUnits()){
			if(unit.equals(u)){
				found = true;
				break;
			}
			i++;
		}
		if(!found)
			throw new IllegalArgumentException("The given units source method is not the given source method.");
		this.index = i;
	}
	
	//Use if a copy is needed
	public SootUnitContainer(String signature, SootMethodContainer source, int index){
		this.signature = signature;
		this.source = source;
		this.index = index;
	}
	
	public boolean equals(Object o){
		if(this == o){
			return true;
		}
		if(o == null || !(o instanceof SootUnitContainer)){
			return false;
		}
		SootUnitContainer other = (SootUnitContainer) o;
		return Objects.equals(signature, other.signature) && 
				Objects.equals(source, other.source) && 
				index == other.index;
	}
	
	public int hashCode(){
		int hash = 17;
		hash = 31 * hash + Objects.hashCode(signature);
		hash = 31 * hash + Objects.hashCode(source);
		hash = 31 * hash + index;
		return hash;
	}
	
	@Override
	public int compareTo(SootUnitContainer o) {
		int ret = source.compareTo(o.source);
		if(ret == 0)
			ret = Integer.compare(index, o.index);
		if(ret == 0)
			ret = SortingMethods.sComp.compare(signature, o.signature);
		return ret;
	}
	
	public String toString(String spacer){
		StringBuilder sb = new StringBuilder();
		sb.append(spacer).append("Unit: ").append(Objects.toString(signature)).append("\n");
		sb.append(spacer).append("  ").append("Source: ").append(Objects.toString(source.getSignature())).append("\n");
		sb.append(spacer).append("  ").append("Index: ").append(index).append("\n");
		return sb.toString();
	}
	
	@Override
	public String toString(){
		return toString("");
	}
	
	public String getSignature() {
		return signature;
	}

	public SootMethodContainer getSource() {
		return source;
	}

	public int getIndex() {
		return index;
	}
	
	public Unit toUnit(){
		if(!SootInstanceWrapper.v().isSootInitSet())
			throw new RuntimeException("Some instance of Soot must be initilized first.");
		SootMethod sourceSM = source.toSootMethod();
		int i = 0;
		Unit ret = null;
		for(Unit u : sourceSM.retrieveActiveBody().getUnits()){
			if(i == index){
				ret = u;
				break;
			}
			i++;
		}
		if(ret == null)
			throw new RuntimeException("Unable to locate the unit with index " + index + ".");
		return ret;
	}
	
	public Unit toUnitUnsafe() {
		Unit ret = null;
		SootMethod sourceSM = source.toSootMethodUnsafe();
		if(sourceSM != null) {
			int i = 0;
			for(Unit u : sourceSM.retrieveActiveBody().getUnits()){
				if(i == index){
					ret = u;
					break;
				}
				i++;
			}
		}
		return ret;
	}
	
	public static Set<Unit> resolveUnitsWithCommonSource(SootMethodContainer source, Set<SootUnitContainer> units) {
		if(!SootInstanceWrapper.v().isSootInitSet())
			throw new RuntimeException("Some instance of Soot must be initilized first.");
		SootMethod s = source.toSootMethod();
		Map<Integer, SootUnitContainer> indexToUnit = new HashMap<>();
		for(SootUnitContainer u : units) {
			indexToUnit.put(u.getIndex(), u);
		}
		Map<Integer,Unit> indexToUnit2 = new HashMap<>();
		int i = 0;
		for(Unit u : s.retrieveActiveBody().getUnits()){
			if(indexToUnit.containsKey(i)) {
				indexToUnit2.put(i, u);
				if(units.size() == indexToUnit2.size())
					break;
			}
			i++;
		}
		if(units.size() != indexToUnit2.size())
			throw new RuntimeException("Unable to resolve all given units.");
		//Preserve the order
		Set<Unit> ret = new LinkedHashSet<>();
		for(SootUnitContainer u : units) {
			ret.add(indexToUnit2.get(u.getIndex()));
		}
		return ret;
	}
	
	public static Set<Unit> resolveUnitsWithCommonSourceUnsafe(SootMethodContainer source, Set<SootUnitContainer> units) {
		Set<Unit> ret = null;
		SootMethod s = source.toSootMethodUnsafe();
		if(s != null) {
			Map<Integer, SootUnitContainer> indexToUnit = new HashMap<>();
			for(SootUnitContainer u : units) {
				indexToUnit.put(u.getIndex(), u);
			}
			Map<Integer,Unit> indexToUnit2 = new HashMap<>();
			int i = 0;
			for(Unit u : s.retrieveActiveBody().getUnits()){
				if(indexToUnit.containsKey(i)) {
					indexToUnit2.put(i, u);
					if(units.size() == indexToUnit2.size())
						break;
				}
				i++;
			}
			//Preserve the order and skip missing
			ret = new LinkedHashSet<>();
			for(SootUnitContainer u : units) {
				Unit uu = indexToUnit2.get(u.getIndex());
				if(uu != null)
					ret.add(uu);
			}
		}
		return ret;
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public SootUnitContainer readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static SootUnitContainer readXMLStatic(String filePath, Path path) throws Exception {
		return new SootUnitContainer().readXML(filePath, path);
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
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(SootUnitContainer.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}
	
}
