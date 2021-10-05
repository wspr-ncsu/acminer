package org.sag.acminer.phases.defusegraph;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.database.defusegraph.IDefUseGraphDatabase;
import org.sag.acminer.sootinit.IPASootLoader;
import org.sag.common.io.FileHash;
import org.sag.common.io.FileHashList;
import org.sag.common.io.FileHelpers;
import org.sag.main.config.PhaseConfig;
import org.sag.main.phase.AbstractPhaseHandler;
import org.sag.main.phase.IPhaseHandler;

public class DefUseGraphHandler extends AbstractPhaseHandler {

	private Path jimpleJar;
	
	public DefUseGraphHandler(List<IPhaseHandler> depPhases, PhaseConfig pc) {
		super(depPhases, pc);
	}
	
	@Override
	protected void initInner() {
		this.jimpleJar = dependencyFilePaths.get(0);
	}

	@Override
	protected List<FileHash> getOldDependencyFileHashes() throws Exception {
		return ((IACMinerDataAccessor)dataAccessor).getDefUseGraphDB().getFileHashList();
	}

	@Override
	protected void loadExistingInformation() throws Exception {
		if(!isSootInitilized())//To read in this database soot must be initialized completely
			initilizeSoot();
		((IACMinerDataAccessor)dataAccessor).setDefUseGraphDB(IDefUseGraphDatabase.Factory.readXML(null, getOutputFilePath()));
	}

	@Override
	protected boolean isSootInitilized() {
		return IPASootLoader.v().isSootLoaded();
	}

	@Override
	protected boolean initilizeSoot() {
		return IPASootLoader.v().load(((IACMinerDataAccessor)dataAccessor), jimpleJar, ai.getJavaVersion(), logger);
	}

	@Override
	protected boolean doWork() {
		try{
			FileHelpers.processDirectory(dataAccessor.getConfig().getFilePath("acminer_defusegraph-dir"),true,true);
			DefUseGraphRunner runner = new DefUseGraphRunner(((IACMinerDataAccessor)dataAccessor),logger);
			if(!runner.run()) {
				logger.fatal("{}: The DefUseGraphRunner encountered errors during executation.",cn);
				return false;
			}
			runner = null;//Get rid of the memory immediately
			List<Path> fullPaths = dependencyFilePaths;
			List<Path> realtivePaths = new ArrayList<>(fullPaths.size());
			for(Path p : fullPaths) {
				realtivePaths.add(rootPath.relativize(p));
			}
			FileHashList fhl = FileHelpers.genFileHashList(fullPaths, realtivePaths);
			((IACMinerDataAccessor)dataAccessor).getDefUseGraphDB().setFileHashList(fhl);
			((IACMinerDataAccessor)dataAccessor).getDefUseGraphDB().writeXML(null, getOutputFilePath());
		}catch(Throwable t){
			logger.fatal("{}: Unexpected exception during the run of the DefUseGraphRunner.",t,cn);
			return false;
		}
		return true;
	}
	
}
