package org.sag.acminer.database.filter;

import java.io.ObjectStreamException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.sag.acminer.database.filter.entry.IEntry;
import org.sag.common.io.FileHash;
import org.sag.common.io.FileHashList;
import org.sag.xstream.XStreamInOut;
import org.sag.xstream.XStreamInOut.XStreamInOutInterface;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("ControlPredicateFilter")
public class ControlPredicateFilterDatabase implements XStreamInOutInterface {
	
	@XStreamAlias("FileHashList")
	private volatile FileHashList fhl;
	//XStream hack so this does not get its own name
	@XStreamImplicit
	private List<IEntry> entries;
	
	private ControlPredicateFilterDatabase() {}//For use with xstream only
	
	public ControlPredicateFilterDatabase(IEntry baseEntry) {
		entries = Collections.singletonList(baseEntry);
	}
	
	//ReadResolve is always run when reading from XML even if a constructor is run first
	protected Object readResolve() throws ObjectStreamException {
		return this;
	}
	
	protected Object writeReplace() throws ObjectStreamException {
		return this;
	}
	
	public List<FileHash> getFileHashList() {
		if(fhl == null)
			return Collections.emptyList();
		return fhl;
	}
	
	public void setFileHashList(FileHashList fhl) {
		this.fhl = fhl;
	}
	
	public boolean applyFilter(IData data) {
		return entries.get(0).eval(data);
	}
	
	public boolean applyFilterDebug(IData data, StringBuilder sb) {
		return entries.get(0).evalDebug(data, sb, new AtomicInteger());
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public ControlPredicateFilterDatabase readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static ControlPredicateFilterDatabase readXMLStatic(String filePath, Path path) throws Exception {
		return new ControlPredicateFilterDatabase().readXML(filePath, path);
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
				FileHashList.getXStreamSetupStatic().getOutputGraph(in);
				IEntry.Factory.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(ControlPredicateFilterDatabase.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}

}
