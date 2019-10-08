package org.sag.acminer.database.accesscontrol;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.sag.common.tools.SortingMethods;
import org.sag.soot.SootSort;
import org.sag.soot.xstream.SootMethodContainer;
import org.sag.xstream.XStreamInOut;
import org.sag.xstream.XStreamInOut.XStreamInOutInterface;
import org.sag.xstream.xstreamconverters.NamedCollectionConverterWithSize;
import soot.SootMethod;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("ContextQueryContainer")
public class ContextQueryContainer implements XStreamInOutInterface {

	@XStreamAlias("ContextQuery")
	private SootMethodContainer contextQuery;
	
	@XStreamAlias("SubGraphMethods")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"SootMethodContainer"},types={SootMethodContainer.class})
	private LinkedHashSet<SootMethodContainer> subGraphMethods;
	
	//for reading in from xml only
	protected ContextQueryContainer() {}
	
	public ContextQueryContainer(SootMethod contextQuery, Set<SootMethod> subGraphMethods) {
		this.contextQuery = SootMethodContainer.makeSootMethodContainer(contextQuery);
		this.subGraphMethods = new LinkedHashSet<>();
		for(SootMethod sm : SortingMethods.sortSet(subGraphMethods,SootSort.smComp)) {
			this.subGraphMethods.add(SootMethodContainer.makeSootMethodContainer(sm));
		}
	}
	
	public boolean equals(Object o){
		if(this == o)
			return true;
		if(o == null || !(o instanceof ContextQueryContainer))
			return false;
		ContextQueryContainer other = (ContextQueryContainer)o;
		return Objects.equals(contextQuery, other.contextQuery);
	}
	
	public int hashCode(){
		int hash = 17;
		hash = 31 * hash + Objects.hashCode(contextQuery);
		return hash;
	}
	
	public SootMethod getContextQuery() {
		return contextQuery.toSootMethod();
	}
	
	public Set<SootMethod> getSubGraphMethods() {
		LinkedHashSet<SootMethod> ret = new LinkedHashSet<>();
		for(SootMethodContainer smc : subGraphMethods) {
			ret.add(smc.toSootMethod());
		}
		return ret;
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}
	
	public ContextQueryContainer readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this,filePath, path);
	}
	
	public static ContextQueryContainer readXMLStatic(String filePath, Path path) throws Exception {
		return new ContextQueryContainer().readXML(filePath, path);
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
			return Collections.singleton(ContextQueryContainer.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}
	
}
