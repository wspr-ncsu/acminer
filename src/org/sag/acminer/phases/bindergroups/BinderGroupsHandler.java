package org.sag.acminer.phases.bindergroups;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.common.io.FileHash;
import org.sag.common.io.FileHashList;
import org.sag.common.io.FileHelpers;
import org.sag.main.config.PhaseConfig;
import org.sag.main.phase.AbstractPhaseHandler;
import org.sag.main.phase.IPhaseHandler;
import org.sag.sootinit.BasicSootLoader;

public class BinderGroupsHandler extends AbstractPhaseHandler {
	
	private Path jimpleJar;
	
	public BinderGroupsHandler(List<IPhaseHandler> depPhases, PhaseConfig pc) {
		super(depPhases, pc);
	}
	
	@Override
	protected void initInner() {
		this.jimpleJar = dependencyFilePaths.get(0);
	}

	@Override
	protected List<FileHash> getOldDependencyFileHashes() throws Exception {
		return new ArrayList<>(BinderGroupsDatabase.v().getDependencyFiles());
	}

	@Override
	protected void loadExistingInformation() throws Exception {
		BinderGroupsDatabase.readXMLStatic(null, getOutputFilePath());
	}

	@Override
	protected boolean isSootInitilized() {
		return BasicSootLoader.v().isSootLoaded();
	}

	@Override
	protected boolean initilizeSoot() {
		return BasicSootLoader.v().load(jimpleJar,true,ai.getJavaVersion(),logger);
	}

	@Override
	protected boolean doWork() {
		try{
			GenerateBinderGroups gen = new GenerateBinderGroups((IACMinerDataAccessor)dataAccessor, logger);
			gen.constructAndSetNewDatabase();
			List<Path> fullPaths = dependencyFilePaths;
			List<Path> realtivePaths = new ArrayList<>(fullPaths.size());
			for(Path p : fullPaths) {
				realtivePaths.add(rootPath.relativize(p));
			}
			FileHashList fhl = FileHelpers.genFileHashList(fullPaths, realtivePaths);
			for(FileHash fh : fhl) {
				BinderGroupsDatabase.v().addDependencyFile(fh);
			}
			BinderGroupsDatabase.v().writeXML(null, getOutputFilePath());
		}catch(Throwable t){
			logger.fatal("Error: Failed to generate the new BinderGroupsDatabase from the rootPath '{}' and '{}'.",t,
					rootPath.toString(),dependencyFilePaths);
			return false;
		}
		return true;
	}

}
