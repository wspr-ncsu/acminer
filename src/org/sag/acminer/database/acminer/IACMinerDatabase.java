package org.sag.acminer.database.acminer;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sag.acminer.phases.acminer.ValuePairHashSet;
import org.sag.acminer.phases.acminer.ValuePairLinkedHashSet;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.io.FileHash;
import org.sag.common.io.FileHashList;
import org.sag.soot.xstream.SootClassContainer;
import org.sag.soot.xstream.SootMethodContainer;
import org.sag.xstream.XStreamInOut.XStreamInOutInterface;

import soot.SootClass;
import soot.SootMethod;

public interface IACMinerDatabase extends XStreamInOutInterface {
	
	public List<FileHash> getFileHashList();
	
	public void setFileHashList(FileHashList fhl);
	
	public void addData(SootClass stub, SootMethod ep, ValuePairHashSet subedData);
	
	public Map<SootMethod, ValuePairLinkedHashSet> getValuePairsForStub(SootClass stub);
	
	public Map<SootMethod, Set<String>> getMethodsForStub(SootClass stub);
	
	public Map<SootMethod, Set<String>> getFieldsForStub(SootClass stub);
	
	public Map<SootClass, Map<SootMethod, Set<Doublet>>> getSootValuePairs();
	
	public Map<SootClassContainer, Map<SootMethodContainer, Set<Doublet>>> getValuePairs();
	
	public Map<String, Map<String, Set<Doublet>>> getStringValuePairs();
	
	public Map<String, Set<Doublet>> getStringValuePairs(String stubSig);
	
	public Map<SootClass, Map<SootMethod, Set<String>>> getSootMethods();
	
	public Map<SootClassContainer, Map<SootMethodContainer, Set<String>>> getMethods();
	
	public Map<String, Map<String, Set<String>>> getStringMethods();
	
	public Map<SootClass, Map<SootMethod, Set<String>>> getSootFields();
	
	public Map<SootClassContainer, Map<SootMethodContainer, Set<String>>> getFields();
	
	public Map<String, Map<String, Set<String>>> getStringFields();
	
	public static final class Factory {
		
		public static IACMinerDatabase getNew(Set<EntryPoint> eps) {
			if(eps == null)
				return new EmptyACMinerDatabase();
			return new ACMinerDatabase(eps);
		}
		
		public static IACMinerDatabase readXML(String filePath, Path path) throws Exception {
			return ACMinerDatabase.readXMLStatic(filePath, path);
		}
	}

	public Map<String, Set<String>> getSourceMethodsForPairInStub(String stubSig, String pair);

	public Set<String> getSourceMethodsForPairInEntryPoint(String epSig, String pair);

	public Map<String, Set<String>> getSourceMethodsForPairInEntryPoints(Set<String> epSigs, String pair);
	
	public void clearSootResolvedData();

}
