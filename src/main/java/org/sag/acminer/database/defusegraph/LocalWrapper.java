package org.sag.acminer.database.defusegraph;

import java.io.ObjectStreamException;
import java.nio.file.Path;
import java.util.Objects;

import org.sag.common.xstream.XStreamInOut;
import org.sag.main.sootinit.SootInstanceWrapper;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import soot.Local;
import soot.Scene;
import soot.SootMethod;

@XStreamAlias("LocalWrapper")
public class LocalWrapper implements ILocalWrapper {
	
	@XStreamOmitField
	private volatile Local local;
	@XStreamOmitField
	private volatile SootMethod source;
	
	@XStreamAlias("Num")
	@XStreamAsAttribute
	private volatile long num;
	@XStreamAlias("OrgString")
	@XStreamAsAttribute
	private volatile String orgString;
	@XStreamAlias("GlobalString")
	@XStreamAsAttribute
	private volatile String globalString;
	@XStreamAlias("Source")
	@XStreamAsAttribute
	private volatile String sourceSig;
	
	private LocalWrapper() {}

	public LocalWrapper(Local local, SootMethod source, long num) {
		this.local = local;
		this.num = num;
		this.orgString = local.toString();
		this.globalString = "$z{" + num + "}";
		this.source = source;
		this.sourceSig = source.getSignature();
	}
	
	protected Object readResolve() throws ObjectStreamException {
		if(!SootInstanceWrapper.v().isSootInitSet())
			throw new RuntimeException("Error: Soot needs to be initilized before loading.");
		source = Scene.v().getMethod(sourceSig);
		local = null;
		for(Local l : source.retrieveActiveBody().getLocals()) {
			if(l.toString().equals(orgString)) {
				local = l;
				break;
			}
		}
		if(local == null) 
			throw new RuntimeException("Error: Unable to find local '" + orgString + "' in '" + sourceSig + "'.");
		return this;
	}
	
	protected Object writeReplace() throws ObjectStreamException {
		return this;
	}
	
	public Local getLocal() {
		return local;
	}
	
	public long getNum() {
		return num;
	}
	
	public String getOrgString() {
		return orgString;
	}
	
	@Override
	public String toString() {
		return globalString;
	}
	
	@Override
	public int hashCode() {
		int ret = 17;
		ret = ret * 31 + Objects.hashCode(local);
		ret = ret * 31 + Objects.hashCode(source);
		ret = ret * 31 + Long.valueOf(num).hashCode();
		return ret;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == this)
			return true;
		if(o == null || !(o instanceof LocalWrapper))
			return false;
		LocalWrapper lw = (LocalWrapper)o;
		return Objects.equals(local, lw.local) && Objects.equals(source, lw.source) && num == lw.num;
	}

	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public LocalWrapper readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static LocalWrapper readXMLStatix(String filePath, Path path) throws Exception {
		return new LocalWrapper().readXML(filePath, path);
	}

	public static AbstractXStreamSetup getXStreamSetupStatic(){
		return ILocalWrapper.Factory.getXStreamSetupStatic();
	}
	
	@Override
	public AbstractXStreamSetup getXStreamSetup() {
		return getXStreamSetupStatic();
	}

	@Override
	public int compareTo(ILocalWrapper arg0) {
		if(arg0 instanceof InlineConstantLocalWrapper) {
			return -1;
		}
		return Long.compare(num, ((LocalWrapper)arg0).num);
	}
	
}
