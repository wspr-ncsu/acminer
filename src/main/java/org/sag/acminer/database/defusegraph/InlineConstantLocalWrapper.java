package org.sag.acminer.database.defusegraph;

import java.nio.file.Path;

import org.sag.common.xstream.XStreamInOut;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import soot.Local;

@XStreamAlias("InlineConstantLocalWrapper")
public class InlineConstantLocalWrapper implements ILocalWrapper {
	
	@XStreamAlias("Num")
	@XStreamAsAttribute
	private volatile long num;
	@XStreamAlias("GlobalString")
	@XStreamAsAttribute
	private volatile String globalString;
	
	
	private InlineConstantLocalWrapper() {}

	public InlineConstantLocalWrapper(long num) {
		this.num = num;
		this.globalString = "$c{" + num + "}";
	}
	
	@Override
	public String toString() {
		return globalString;
	}
	
	@Override
	public int hashCode() {
		int ret = 17;
		ret = ret * 31 + Long.valueOf(num).hashCode();
		return ret;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == this)
			return true;
		if(o == null || !(o instanceof InlineConstantLocalWrapper))
			return false;
		InlineConstantLocalWrapper r = (InlineConstantLocalWrapper)o;
		return num == r.num;
	}

	@Override
	public int compareTo(ILocalWrapper o) {
		if(o instanceof LocalWrapper) {
			return 1;
		}
		return Long.compare(num, ((InlineConstantLocalWrapper)o).num);
	}

	@Override
	public Local getLocal() {
		return null;
	}

	@Override
	public long getNum() {
		return num;
	}

	@Override
	public String getOrgString() {
		return null;
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public InlineConstantLocalWrapper readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static InlineConstantLocalWrapper readXMLStatix(String filePath, Path path) throws Exception {
		return new InlineConstantLocalWrapper().readXML(filePath, path);
	}

	public static AbstractXStreamSetup getXStreamSetupStatic(){
		return ILocalWrapper.Factory.getXStreamSetupStatic();
	}
	
	@Override
	public AbstractXStreamSetup getXStreamSetup() {
		return getXStreamSetupStatic();
	}

}
