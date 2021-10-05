package org.sag.acminer.database.excludedelements;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.sag.acminer.database.FileHashDatabase;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.io.FileHash;
import org.sag.common.io.FileHashList;
import org.sag.common.xstream.XStreamInOut;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import soot.SootClass;
import soot.SootMethod;

@XStreamAlias("ExcludedElementsDatabase")
public class EmptyExcludedElementsDatabase extends FileHashDatabase implements IExcludedElementsDatabase {

	@Override
	public void resetSootResolvedData() {}

	@Override
	public void loadAllSootResolvedData() {}

	@Override
	public Set<String> getExcludedClasses() {
		return Collections.emptySet();
	}

	@Override
	public Set<String> getExcludedMethods() {
		return Collections.emptySet();
	}

	@Override
	public Set<SootClass> getSootExcludedClasses() {
		return Collections.emptySet();
	}

	@Override
	public Set<SootMethod> getSootExcludedMethods() {
		return Collections.emptySet();
	}

	@Override
	public boolean isExcludedMethod(SootMethod m) {
		return false;
	}

	@Override
	public boolean isExcludedClass(SootClass sc) {
		return false;
	}

	@Override
	public IExcludeHandler createNewExcludeHandler(EntryPoint ep, Object... args) {
		return new EmptyExcludeHandler();
	}

	@Override
	public List<FileHash> getFileHashList() {
		return Collections.emptyList();
	}

	@Override
	public void setFileHashList(FileHashList fhl) {}
	
	@Override
	public EmptyExcludedElementsDatabase readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static EmptyExcludedElementsDatabase readXMLStatic(String filePath, Path path) throws Exception {
		return new EmptyExcludedElementsDatabase().readXML(filePath, path);
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
			return Collections.singleton(EmptyExcludedElementsDatabase.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}

}
