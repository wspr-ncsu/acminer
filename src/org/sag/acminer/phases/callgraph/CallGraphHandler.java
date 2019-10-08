package org.sag.acminer.phases.callgraph;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.common.io.FileHash;
import org.sag.main.config.PhaseConfig;
import org.sag.main.phase.AbstractPhaseHandler;
import org.sag.main.phase.IPhaseHandler;
import org.sag.sootinit.IPASootLoader;

public class CallGraphHandler extends AbstractPhaseHandler {
	
	private Path jimpleJar;
	
	public CallGraphHandler(List<IPhaseHandler> depPhases, PhaseConfig pc) {
		super(depPhases, pc);
	}
	
	@Override
	protected void initInner() {
		this.jimpleJar = dependencyFilePaths.get(0);
	}

	@Override
	protected List<FileHash> getOldDependencyFileHashes() throws Exception {
		return Collections.emptyList();
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
			CallGraphModifier mod = new CallGraphModifier((IACMinerDataAccessor)dataAccessor,logger);
			if(!mod.run()){
				logger.fatal("{}: The CallGraphModifier encountered errors during executation.",cn);
				return false;
			}
		}catch(Throwable t){
			logger.fatal("{}: Unexpected exception during the run of the CallGraphModifier.",t,cn);
			return false;
		}
		return true;
	}

	//Hardcode in forced run so that if the phase is enabled it is always run without looking at anything else
	@Override
	public boolean isForcedRun(){
		return true;
	}

}
