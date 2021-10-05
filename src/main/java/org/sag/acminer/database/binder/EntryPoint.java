package org.sag.acminer.database.binder;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.sag.common.xstream.NamedCollectionConverterWithSize;
import org.sag.common.xstream.XStreamInOut;
import org.sag.common.xstream.XStreamInOut.XStreamInOutInterface;
import org.sag.soot.xstream.SootMethodContainer;

import com.google.common.collect.ImmutableSet;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import soot.SootMethod;

@XStreamAlias("EntryPoint")
public final class EntryPoint implements XStreamInOutInterface, Comparable<EntryPoint> {
	
	@XStreamOmitField
	private volatile SootMethod sm;

	@XStreamAlias("SootMethodContainer")
	private volatile SootMethodContainer method;
	
	@XStreamAlias("Service")
	private volatile Service service;
	
	@XStreamAlias("TransactionIds")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"TransactionId"},types={Integer.class})
	private volatile LinkedHashSet<Integer> transactionIds;
	
	@XStreamOmitField
	private volatile boolean loaded;
	
	private EntryPoint() {}
	
	EntryPoint(SootMethod ep, Service service, Set<Integer> ids) {
		Objects.requireNonNull(ep);
		Objects.requireNonNull(service);
		this.sm = ep;
		this.method = SootMethodContainer.makeSootMethodContainer(ep);
		this.service = service;
		if(ids != null && !ids.isEmpty()) {
			this.transactionIds = new LinkedHashSet<>(ids);
		} else {
			this.transactionIds = null;
		}
		this.loaded = true;
	}

	@Override
	public int compareTo(EntryPoint o) {
		return method.compareTo(o.method);
	}
	
	@Override
	public int hashCode() {
		int i = 17;
		i = i * 31 + Objects.hashCode(method);
		i = i * 31 + Objects.hashCode(transactionIds);
		return i;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == this)
			return true;
		if(o == null || !(o instanceof EntryPoint))
			return false;
		EntryPoint p = (EntryPoint)o;
		return Objects.equals(method,p.method) && Objects.equals(transactionIds, p.transactionIds);
	}
	
	@Override
	public String toString() {
		return toString("");
	}
	
	public String toString(String spacer) {
		StringBuilder sb = new StringBuilder();
		sb.append(spacer).append("Entry Point: ").append(method.getSignature());
		if(transactionIds != null)
			sb.append(" ").append(transactionIds);
		return sb.toString();
	}
	
	public void initSoot() {
		if(!loaded) {
			sm = method.toSootMethod();
			loaded = true;
		}
	}
	
	public SootMethod getSootMethod() {
		if(!loaded)
			throw new RuntimeException("Error: The Soot objects have not been resolved.");
		return sm;
	}
	
	public SootMethodContainer getMethod() {
		return method;
	}
	
	public Service getService() {
		return service;
	}
	
	public Set<Integer> getTransactionIds() {
		if(transactionIds == null)
			return ImmutableSet.of();
		return ImmutableSet.copyOf(transactionIds);
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public EntryPoint readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static EntryPoint readXMLStatic(String filePath, Path path) throws Exception {
		return new EntryPoint().readXML(filePath, path);
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
				Service.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(EntryPoint.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsIdRef(xstream);
		}
		
	}
	
}
