package org.sag.acminer.database.acminer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.sag.soot.xstream.SootClassContainer;
import org.sag.xstream.XStreamInOut;
import org.sag.xstream.XStreamInOut.XStreamInOutInterface;
import org.sag.xstream.xstreamconverters.NamedCollectionConverterWithSize;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import soot.SootClass;
import soot.SootMethod;

@XStreamAlias("StubContainer")
public class StubContainer implements XStreamInOutInterface {
	
	@XStreamAlias("Stub")
	private volatile SootClassContainer stub;
	
	@XStreamAlias("EntryPoints")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"EntryPoint"},types={String.class})
	private volatile ArrayList<String> entryPoints;
	
	private StubContainer() {}
	
	public StubContainer(SootClass stub, Set<SootMethod> eps) {
		this.stub = SootClassContainer.makeSootClassContainer(stub);
		this.entryPoints = new ArrayList<>();
		for(SootMethod ep : eps) {
			entryPoints.add(ep.toString());
		}
	}
	
	public SootClassContainer getStub() {
		return stub;
	}
	
	public SootClass getSootStub() {
		return stub.toSootClass();
	}
	
	public String getStringStub() {
		return stub.getSignature();
	}
	
	public Set<String> getStringEntryPoints() {
		return new LinkedHashSet<>(entryPoints);
	}

	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public StubContainer readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}

	public static StubContainer readXMLStatic(String filePath, Path path) throws Exception {
		return new StubContainer().readXML(filePath, path);
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
				SootClassContainer.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(StubContainer.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}

}
