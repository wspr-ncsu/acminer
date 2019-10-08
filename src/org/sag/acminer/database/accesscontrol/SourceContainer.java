package org.sag.acminer.database.accesscontrol;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.sag.common.tuple.Pair;
import org.sag.soot.xstream.SootMethodContainer;
import org.sag.soot.xstream.SootUnitContainer;
import org.sag.soot.xstream.SootUnitContainerFactory;
import org.sag.xstream.XStreamInOut;
import org.sag.xstream.XStreamInOut.XStreamInOutInterface;
import org.sag.xstream.xstreamconverters.NamedCollectionConverterWithSize;
import soot.SootMethod;
import soot.Unit;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("SourceContainer")
public class SourceContainer implements XStreamInOutInterface {

	@XStreamAlias("Source")
	private SootMethodContainer source;
	
	@XStreamAlias("Depths")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"Depth"},types={Integer.class})
	private LinkedHashSet<Integer> depths;
	
	@XStreamAlias("Units")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"SootUnitContainer"},types={SootUnitContainer.class})
	private LinkedHashSet<SootUnitContainer> units;
	
	//for reading in from xml only
	protected SourceContainer() {}
	
	public SourceContainer(SootMethod source, Pair<Set<Unit>,Set<Integer>> data) {
		this.source = SootMethodContainer.makeSootMethodContainer(source);
		this.depths = new LinkedHashSet<>(data.getSecond());
		this.units = new LinkedHashSet<>();
		for(Unit u : data.getFirst()) {
			units.add(SootUnitContainerFactory.makeSootUnitContainer(u, source));
		}
	}
	
	public boolean equals(Object o){
		if(this == o)
			return true;
		if(o == null || !(o instanceof SourceContainer))
			return false;
		SourceContainer other = (SourceContainer)o;
		return Objects.equals(source, other.source);
	}
	
	public int hashCode(){
		int hash = 17;
		hash = 31 * hash + Objects.hashCode(source);
		return hash;
	}
	
	public SootMethod getSource() {
		return source.toSootMethod();
	}
	
	public Pair<Set<Unit>, Set<Integer>> getData() {
		return new Pair<Set<Unit>, Set<Integer>>(SootUnitContainer.resolveUnitsWithCommonSource(source, units), new LinkedHashSet<>(depths));
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}
	
	public SourceContainer readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this,filePath, path);
	}
	
	public static SourceContainer readXMLStatic(String filePath, Path path) throws Exception {
		return new SourceContainer().readXML(filePath, path);
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
				SootUnitContainerFactory.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(SourceContainer.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}
	
}
