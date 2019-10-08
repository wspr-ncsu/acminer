package org.sag.acminer.phases.excludedelements;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.database.excludedelements.IExcludedElementsDatabase;
import org.sag.common.io.FileHash;
import org.sag.common.io.FileHashList;
import org.sag.common.io.FileHelpers;
import org.sag.main.config.PhaseConfig;
import org.sag.main.phase.AbstractPhaseHandler;
import org.sag.main.phase.IPhaseHandler;
import org.sag.sootinit.BasicSootLoader;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

public class ExcludedElementsHandler extends AbstractPhaseHandler {
	
	protected Path excludedElementsTextFile;
	protected Path jimpleJar;
	
	public ExcludedElementsHandler(List<IPhaseHandler> depPhases, PhaseConfig pc) {
		super(depPhases, pc);
	}
	
	@Override
	protected void initInner() {
		this.jimpleJar = dependencyFilePaths.get(0);
		this.excludedElementsTextFile = dependencyFilePaths.get(dependencyFilePaths.size()-1);
	}

	@Override
	protected List<FileHash> getOldDependencyFileHashes() throws Exception {
		return ((IACMinerDataAccessor)dataAccessor).getExcludedElementsDB().getFileHashList();
	}

	@Override
	protected void loadExistingInformation() throws Exception {
		((IACMinerDataAccessor)dataAccessor).setExcludedElementsDB(IExcludedElementsDatabase.Factory.readXML(
				null, getOutputFilePath(), new RuntimeExcludedMethods((IACMinerDataAccessor)dataAccessor)));
	}

	//Soot is not required to generate this database because the generation is just reading it from a text file and outputting it to xml
	//Soot is required later when the exclude list of classes needs to be generated or when we want to determine if a method is in the exclude list
	@Override
	protected boolean isSootInitilized() {
		return BasicSootLoader.v().isSootLoaded();
	}

	@Override
	protected boolean initilizeSoot() {
		return BasicSootLoader.v().load(jimpleJar, true, ai.getJavaVersion(), logger);
	}
	
	protected IExcludedElementsDatabase genExcludedElementsDatabase(Set<String> otherClasses, Set<String> otherMethods) throws Exception {
		//Exclude all binder group classes and their methods
		Set<String> epClasses = ((IACMinerDataAccessor)dataAccessor).getAllEntryPointClasses();
		for(String bgClassName : ((IACMinerDataAccessor)dataAccessor).getAllBinderGroupClasses()){
			//Don't exclude if this class is an entry point class for some reason
			if(!epClasses.contains(bgClassName) && Scene.v().containsClass(bgClassName)){
				otherClasses.add(bgClassName);
				SootClass sc = Scene.v().getSootClassUnsafe(bgClassName);
				if(sc != null){
					for(SootMethod sm : sc.getMethods()){
						otherMethods.add(sm.getSignature());
					}
				}
			}
		}
		
		return IExcludedElementsDatabase.Factory.readTXT(excludedElementsTextFile, otherClasses, 
				otherMethods, new RuntimeExcludedMethods((IACMinerDataAccessor)dataAccessor));
	}

	@Override
	protected boolean doWork() {
		try {
			IExcludedElementsDatabase exdb = genExcludedElementsDatabase(new HashSet<>(), new HashSet<>());
			List<Path> fullPaths = dependencyFilePaths;
			List<Path> realtivePaths = new ArrayList<>(fullPaths.size());
			for(Path p : fullPaths) {
				realtivePaths.add(rootPath.relativize(p));
			}
			FileHashList fhl = FileHelpers.genFileHashList(fullPaths, realtivePaths);
			exdb.setFileHashList(fhl);
			((IACMinerDataAccessor)dataAccessor).setExcludedElementsDB(exdb);
			exdb.writeXML(null, getOutputFilePath());
		} catch(Throwable t) {
			logger.fatal("Error: Failed to generate the new ExcludedElementsDatabase from the rootPath '{}' and the "
					+ "excluded_elements_db.txt path '{}'.",t,rootPath.toString(),excludedElementsTextFile.toString());
			return false;
		}
		return true;
	}

}
