package org.sag.acminer.database.accesscontrol;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.sag.common.io.FileHash;
import org.sag.common.io.FileHashList;
import org.sag.xstream.XStreamInOut;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("AbstractAccessControlDatabase")
public abstract class AbstractAccessControlDatabase implements IAccessControlDatabase {

	@XStreamOmitField
	protected final String name;
	@XStreamOmitField
	protected final String type;
	@XStreamAlias("FileHashList")
	protected volatile FileHashList fhl;
	@XStreamImplicit
	protected volatile Set<EntryPointContainer> output;
	
	protected AbstractAccessControlDatabase(String name, String type, Set<EntryPointContainer> output) {
		this.output = output;
		this.name = name;
		this.type = type;
		this.fhl = null;
	}
	
	public String getType() {
		return type;
	}
	
	public List<FileHash> getFileHashList() {
		if(fhl == null)
			return Collections.emptyList();
		return fhl;
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}
	
	@Override
	public AbstractXStreamSetup getXStreamSetup() {
		return new XStreamSetup(type,getClass());
	}
	
	public static class XStreamSetup extends AbstractXStreamSetup {
		
		protected final String type;
		protected final Class<?> clazz;
		
		public XStreamSetup(String type, Class<?> clazz) {
			this.type = type;
			this.clazz = clazz;
		}
		
		@Override
		public void getOutputGraph(LinkedHashSet<AbstractXStreamSetup> in) {
			if(!in.contains(this)) {
				in.add(this);
				EntryPointContainer.getXStreamSetupStatic().getOutputGraph(in);
				FileHashList.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(clazz);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			xstream.alias(type, clazz);
			defaultOptionsNoRef(xstream);
		}
		
	}
	
}
