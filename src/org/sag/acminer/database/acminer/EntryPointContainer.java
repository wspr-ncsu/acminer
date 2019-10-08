package org.sag.acminer.database.acminer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.sag.acminer.phases.acminer.ValuePair;
import org.sag.acminer.phases.acminer.ValuePairLinkedHashSet;
import org.sag.soot.xstream.SootMethodContainer;
import org.sag.xstream.XStreamInOut;
import org.sag.xstream.XStreamInOut.XStreamInOutInterface;
import org.sag.xstream.xstreamconverters.NamedCollectionConverterWithSize;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import soot.SootMethod;

@XStreamAlias("EntryPointContainer")
public class EntryPointContainer implements XStreamInOutInterface {
	
	@XStreamAlias("EntryPoint")
	private volatile SootMethodContainer entryPoint;
	
	@XStreamAlias("Doublets")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"Doublet"},types={Doublet.class})
	private volatile ArrayList<Doublet> doublets;
	
	@XStreamAlias("Methods")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"Method"},types={String.class})
	private volatile ArrayList<String> methods;
	
	@XStreamAlias("Fields")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"Field"},types={String.class})
	private volatile ArrayList<String> fields;
	
	private EntryPointContainer() {}
	
	public EntryPointContainer(SootMethod entryPoint, ValuePairLinkedHashSet valuePairs, Set<String> methods, Set<String> fields) {
		this.entryPoint = SootMethodContainer.makeSootMethodContainer(entryPoint);
		if(valuePairs == null || valuePairs.isEmpty()) {
			this.doublets = null;
		} else {
			this.doublets = new ArrayList<>();
			for(ValuePair vp : valuePairs) {
				this.doublets.add(new Doublet(vp));
			}
		}
		if(methods == null || methods.isEmpty())
			this.methods = null;
		else
			this.methods = new ArrayList<>(methods);
		if(fields == null || fields.isEmpty())
			this.fields = null;
		else
			this.fields = new ArrayList<>(fields);
	}
	
	public SootMethodContainer getEntryPoint() {
		return entryPoint;
	}
	
	public SootMethod getSootEntryPoint() {
		return entryPoint.toSootMethod();
	}
	
	public String getStringEntryPoint() {
		return entryPoint.getSignature();
	}
	
	public Set<Doublet> getDoublets() {
		if(doublets == null)
			return Collections.emptySet();
		return new LinkedHashSet<>(doublets);
	}
	
	public Set<String> getMethods() {
		if(methods == null)
			return Collections.emptySet();
		return new LinkedHashSet<>(methods);
	}
	
	public Set<String> getFields() {
		if(fields == null)
			return Collections.emptySet();
		return new LinkedHashSet<>(fields);
	}

	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public EntryPointContainer readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}

	public static EntryPointContainer readXMLStatic(String filePath, Path path) throws Exception {
		return new EntryPointContainer().readXML(filePath, path);
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
				Doublet.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(EntryPointContainer.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}

}
