package org.sag.acminer.database.acminer;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sag.acminer.database.FileHashDatabase;
import org.sag.acminer.phases.acminer.ValuePairHashSet;
import org.sag.acminer.phases.acminer.ValuePairLinkedHashSet;
import org.sag.common.io.FileHash;
import org.sag.common.io.FileHashList;
import org.sag.common.xstream.XStreamInOut;
import org.sag.soot.xstream.SootClassContainer;
import org.sag.soot.xstream.SootMethodContainer;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import soot.SootClass;
import soot.SootMethod;

public class EmptyACMinerDatabase extends FileHashDatabase implements IACMinerDatabase {

	@Override
	public void addData(SootClass stub, SootMethod ep, ValuePairHashSet subedData) {}

	@Override
	public Map<SootMethod, ValuePairLinkedHashSet> getValuePairsForStub(SootClass stub) {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootMethod, Set<String>> getMethodsForStub(SootClass stub) {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootMethod, Set<String>> getFieldsForStub(SootClass stub) {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootClass, Map<SootMethod, Set<Doublet>>> getSootValuePairs() {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootClassContainer, Map<SootMethodContainer, Set<Doublet>>> getValuePairs() {
		return Collections.emptyMap();
	}

	@Override
	public Map<String, Map<String, Set<Doublet>>> getStringValuePairs() {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootClass, Map<SootMethod, Set<String>>> getSootMethods() {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootClassContainer, Map<SootMethodContainer, Set<String>>> getMethods() {
		return Collections.emptyMap();
	}

	@Override
	public Map<String, Map<String, Set<String>>> getStringMethods() {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootClass, Map<SootMethod, Set<String>>> getSootFields() {
		return Collections.emptyMap();
	}

	@Override
	public Map<SootClassContainer, Map<SootMethodContainer, Set<String>>> getFields() {
		return Collections.emptyMap();
	}

	@Override
	public Map<String, Map<String, Set<String>>> getStringFields() {
		return Collections.emptyMap();
	}
	
	public List<FileHash> getFileHashList() {
		return Collections.emptyList();
	}
	
	public void setFileHashList(FileHashList fhl) {}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public EmptyACMinerDatabase readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static EmptyACMinerDatabase readXMLStatic(String filePath, Path path) throws Exception {
		return new EmptyACMinerDatabase().readXML(filePath, path);
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
			return Collections.singleton(EmptyACMinerDatabase.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}

	@Override
	public Map<String, Set<String>> getSourceMethodsForPairInStub(String stubSig, String pair) {
		return Collections.emptyMap();
	}

	@Override
	public Set<String> getSourceMethodsForPairInEntryPoint(String epSig, String pair) {
		return Collections.emptySet();
	}

	@Override
	public Map<String, Set<String>> getSourceMethodsForPairInEntryPoints(Set<String> epSigs, String pair) {
		return Collections.emptyMap();
	}

	@Override
	public Map<String, Set<Doublet>> getStringValuePairs(String stubSig) {
		return Collections.emptyMap();
	}

	@Override
	public void clearSootResolvedData() {}

}
