package org.sag.acminer.phases.bindergroups;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.sag.common.tools.SortingMethods;
import org.sag.common.xstream.NamedCollectionConverterWithSize;
import org.sag.common.xstream.XStreamInOut;
import org.sag.common.xstream.XStreamInOut.XStreamInOutInterface;
import org.sag.soot.xstream.SootMethodContainer;

import soot.SootMethod;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("StubMethodBContainer")
public class StubMethodBContainer implements XStreamInOutInterface, Comparable<StubMethodBContainer> {

	@XStreamAlias("StubMethod")
	private SootMethodContainer stubMethod;
	
	@XStreamAlias("TransactionIds")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"TransactionId"},types={Integer.class})
	private LinkedHashSet<Integer> ids;
	
	//for reading in from xml only
	protected StubMethodBContainer() {}
	
	public StubMethodBContainer(SootMethod m, Set<Integer> ids) {
		if(m == null)
			throw new IllegalArgumentException();
		this.stubMethod = SootMethodContainer.makeSootMethodContainer(m);
		this.ids = null;
		if(ids != null && !ids.isEmpty()){
			this.ids = SortingMethods.sortSet(ids,SortingMethods.iComp);
		}
	}
	
	@Override
	public boolean equals(Object o){
		if(this == o)
			return true;
		if(o == null || !(o instanceof StubMethodBContainer))
			return false;
		StubMethodBContainer other = (StubMethodBContainer)o;
		return Objects.equals(stubMethod, other.stubMethod);
	}
	
	@Override
	public int hashCode(){
		int hash = 17;
		hash = 31 * hash + Objects.hashCode(stubMethod);
		return hash;
	}
	
	@Override
	public int compareTo(StubMethodBContainer o) {
		return this.stubMethod.compareTo(o.stubMethod);
	}
	
	public SootMethodContainer getStubMethod(){
		return stubMethod;
	}
	
	public Set<Integer> getTransactionIds(){
		if(ids == null)
			return new LinkedHashSet<>();
		return new LinkedHashSet<>(ids);
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
		
	}
	
	public StubMethodBContainer readXML(String filePath, Path path) throws Exception {
		return (StubMethodBContainer)XStreamInOut.readXML(this,filePath, path);
	}
	
	public static StubMethodBContainer readXMLStatic(String filePath, Path path) throws Exception {
		return new StubMethodBContainer().readXML(filePath, path);
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
			return Collections.singleton(StubMethodBContainer.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}

}
