package org.sag.acminer.phases.entrypoints;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sag.acminer.sootinit.BasicSootLoader;
import org.sag.common.io.FileHash;
import org.sag.common.io.FileHashList;
import org.sag.common.io.FileHelpers;
import org.sag.main.config.PhaseConfig;
import org.sag.main.phase.AbstractPhaseHandler;
import org.sag.main.phase.IPhaseHandler;

public class EntryPointsHandler extends AbstractPhaseHandler {	

	private Path jimpleJar;
	
	public EntryPointsHandler(List<IPhaseHandler> depPhases, PhaseConfig pc) {
		super(depPhases, pc);
	}
	
	@Override
	protected void initInner() {
		this.jimpleJar = dependencyFilePaths.get(0);
	}

	@Override
	protected List<FileHash> getOldDependencyFileHashes() throws Exception {
		return Collections.singletonList(EntryPointsDatabase.v().getDependencyFile());
	}

	@Override
	protected boolean isSootInitilized() {
		return BasicSootLoader.v().isSootLoaded();
	}

	@Override
	protected boolean initilizeSoot() {
		return BasicSootLoader.v().load(jimpleJar, true, ai.getJavaVersion(), logger);
	}

	@Override
	protected boolean doWork() {
		try{
			GenerateEntryPoints gen = new GenerateEntryPoints(logger);
			gen.constructAndSetNewDatabase();
			List<Path> fullPaths = dependencyFilePaths;
			List<Path> realtivePaths = new ArrayList<>(fullPaths.size());
			for(Path p : fullPaths) {
				realtivePaths.add(rootPath.relativize(p));
			}
			FileHashList fhl = FileHelpers.genFileHashList(fullPaths, realtivePaths);
			for(FileHash fh : fhl) {
				EntryPointsDatabase.v().setDependencyFile(fh);
			}
			EntryPointsDatabase.v().writeXML(null, getOutputFilePath());
		}catch(Throwable t){
			logger.fatal("Error: Failed to generate the new EntryPointsDatabase from the rootPath '{}' and the system_jimple.jar path '{}'.",
					t,rootPath.toString(),jimpleJar.toString());
			return false;
		}
		return true;
	}

	@Override
	protected void loadExistingInformation() throws Exception {
		EntryPointsDatabase.readXMLStatic(null, getOutputFilePath());
	}

}
