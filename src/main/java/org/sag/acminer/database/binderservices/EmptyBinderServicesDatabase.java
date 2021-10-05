package org.sag.acminer.database.binderservices;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.sag.acminer.database.FileHashDatabase;
import org.sag.common.io.FileHash;
import org.sag.common.io.FileHashList;
import org.sag.common.xstream.XStreamInOut;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("BinderServicesDatabase")
public class EmptyBinderServicesDatabase extends FileHashDatabase implements IBinderServicesDatabase {
	
	@Override
	public void clearSootResolvedData() {}

	@Override
	public void loadSootResolvedData() {}
	
	@Override
	public List<FileHash> getFileHashList() {
		return Collections.emptyList();
	}

	@Override
	public void setFileHashList(FileHashList fhl) {}
	
	@Override
	public String toString() {
		return toString("");
	}
	
	@Override
	public String toString(String spacer) {
		StringBuilder sb = new StringBuilder();
		sb.append("Structure Of Binder Service Database:\n");
		return sb.toString();
	}
	
	@Override
	public int hashCode() {
		int i = 17;
		//i = i * 31 + Objects.hashCode(getOutputData());
		return i;
	}
	
	@Override
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof IBinderServicesDatabase))
			return false;
		//IBinderServiceDatabase other = (IBinderServiceDatabase)o;
		//return Objects.equals(getOutputData(), other.getOutputData());
		return true;
	}
	
	@Override
	public EmptyBinderServicesDatabase readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static EmptyBinderServicesDatabase readXMLStatic(String filePath, Path path) throws Exception {
		return new EmptyBinderServicesDatabase().readXML(filePath, path);
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@XStreamOmitField
	private static AbstractXStreamSetup xstreamSetup = null;

	public static AbstractXStreamSetup getXStreamSetupStatic(){
		if(xstreamSetup == null)
			xstreamSetup = new SubXStreamSetup();
		return xstreamSetup;
	}
	
	@Override
	public AbstractXStreamSetup getXStreamSetup() {
		return getXStreamSetupStatic();
	}
	
	public static class SubXStreamSetup extends XStreamSetup {
		
		@Override
		public void getOutputGraph(LinkedHashSet<AbstractXStreamSetup> in) {
			if(!in.contains(this)) {
				in.add(this);
				super.getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(EmptyBinderServicesDatabase.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}

}
