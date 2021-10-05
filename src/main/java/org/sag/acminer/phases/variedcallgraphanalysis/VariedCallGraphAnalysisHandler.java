package org.sag.acminer.phases.variedcallgraphanalysis;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.sootinit.IPASootLoader;
import org.sag.common.io.FileHash;
import org.sag.common.io.FileHelpers;
import org.sag.main.config.PhaseConfig;
import org.sag.main.phase.AbstractPhaseHandler;
import org.sag.main.phase.IPhaseHandler;
import org.sag.main.phase.IPhaseOption;

public class VariedCallGraphAnalysisHandler extends AbstractPhaseHandler {
	
	public static final String optReachingGraphs = "ReachingGraphs";
	public static final String optDumpNative = "DumpNative";
	
	private Path jimpleJar;
	
	public VariedCallGraphAnalysisHandler(List<IPhaseHandler> depPhases, PhaseConfig pc) {
		super(depPhases, pc);
	}
	
	private boolean isOptionEnabled(String name) {
		IPhaseOption<?> o = getPhaseOptionUnchecked(name);
		if(o == null || !o.isEnabled())
			return false;
		return true;
	}
	
	@Override
	protected void initInner() {
		this.jimpleJar = dependencyFilePaths.get(0);
		if(isOptionEnabled(optReachingGraphs) || isOptionEnabled(optDumpNative)) {
			Path debugDir = dataAccessor.getConfig().getFilePath("debug-dir");
			try {
				FileHelpers.processDirectory(debugDir, true, false);
			} catch(Throwable t) {
				throw new RuntimeException("Error: Failed to process debug directory '" + debugDir + "'",t);
			}
		}
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
			VariedCallGraphAnalysis sg = new VariedCallGraphAnalysis((IACMinerDataAccessor)dataAccessor,this,logger);
			if(!sg.run()){
				logger.fatal("{}: Encountered errors during executation.", cn);
				return false;
			}
		}catch(Throwable t){
			logger.fatal("{}: Unexpected exception.", t, cn);
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
