package org.sag.acminer.phases.defusegraphdump;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.sootinit.IPASootLoader;
import org.sag.common.io.FileHash;
import org.sag.common.io.FileHashList;
import org.sag.common.io.FileHelpers;
import org.sag.main.config.PhaseConfig;
import org.sag.main.phase.AbstractPhaseHandler;
import org.sag.main.phase.IPhaseHandler;

public class DefUseGraphDumpBeforeFilterHandler  extends AbstractPhaseHandler {
	
	private Path jimpleJar;
	
	public DefUseGraphDumpBeforeFilterHandler(List<IPhaseHandler> depPhases, PhaseConfig pc) {
		super(depPhases, pc);
	}
	
	@Override
	protected void initInner() {
		this.jimpleJar = dependencyFilePaths.get(0);
		Path debugDir = dataAccessor.getConfig().getFilePath("debug-dir");
		try {
			FileHelpers.processDirectory(debugDir, true, false);
		} catch(Throwable t) {
			throw new RuntimeException("Error: Failed to process debug directory '" + debugDir + "'",t);
		}
	}
	
	@Override
	protected List<FileHash> getOldDependencyFileHashes() throws Exception {
		//Check for if this file exists occurs before this call and if it does not we call do work
		return FileHashList.readXMLStatic(null, getOutputFilePath());
	}

	@Override
	protected void loadExistingInformation() throws Exception {}

	@Override
	protected boolean isSootInitilized() {
		return IPASootLoader.v().isSootLoaded();
	}

	@Override
	protected boolean initilizeSoot() {
		return IPASootLoader.v().load((IACMinerDataAccessor)dataAccessor, jimpleJar, ai.getJavaVersion(), logger);
	}
	
	@Override
	protected boolean doWork() {
		try{
			DefUseGraphDumpRunner dumper = new DefUseGraphDumpRunner((IACMinerDataAccessor)dataAccessor, getOutputFilePath().getParent(), logger);
			if(!dumper.run()) {
				logger.fatal("{}: The {} encountered errors during executation.",cn,getName());
				return false;
			}
			List<Path> fullPaths = dependencyFilePaths;
			List<Path> realtivePaths = new ArrayList<>(fullPaths.size());
			for(Path p : fullPaths) {
				realtivePaths.add(rootPath.relativize(p));
			}
			FileHashList fhl = FileHelpers.genFileHashList(fullPaths, realtivePaths);
			fhl.writeXML(null, getOutputFilePath());
		}catch(Throwable t){
			logger.fatal("{}: Unexpected exception during the run of the {}.",t,cn,getName());
			return false;
		}
		return true;
	}

}
