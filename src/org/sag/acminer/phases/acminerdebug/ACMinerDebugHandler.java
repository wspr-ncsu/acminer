package org.sag.acminer.phases.acminerdebug;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.common.io.FileHash;
import org.sag.common.io.FileHelpers;
import org.sag.main.config.PhaseConfig;
import org.sag.main.phase.AbstractPhaseHandler;
import org.sag.main.phase.IPhaseHandler;
import org.sag.sootinit.IPASootLoader;

public class ACMinerDebugHandler extends AbstractPhaseHandler {
	
	public static final String optCQSubGraph = "CQSubGraph";
	public static final String optSubGraphCount = "SubgraphCount";
	public static final String commonSubgraphs = "CommonSubgraphs";
	public static final String paths = "Paths";
	public static final String pathsToMethods = "PathsToMethods";
	public static final String cfg = "CFG";
	public static final String cgMethod = "CGMethod";
	public static final String cgClass = "CGClass";
	public static final String cgThrowSE = "CGThrowSE";
	public static final String cgMethodLimit = "CGMethodLimit";
	public static final String cgClassLimit = "CGClassLimit";
	public static final String cgInac = "CGInac";
	public static final String cgSubGraphData = "CGSubGraphData";
	public static final String dataDumps = "DataDumps";
	public static final String all = "All";
	
	private Path jimpleJar;
	
	public ACMinerDebugHandler(List<IPhaseHandler> depPhases, PhaseConfig pc) {
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
		return IPASootLoader.v().load(((IACMinerDataAccessor)dataAccessor), jimpleJar, ai.getJavaVersion(), logger);
	}

	@Override
	protected boolean doWork() {
		try{
			ACMinerDebugger debugger = new ACMinerDebugger(((IACMinerDataAccessor)dataAccessor),this,logger);
			if(!debugger.run()){
				logger.fatal("{}: The ACMinerDebugger encountered errors during executation.",cn);
				return false;
			}
		}catch(Throwable t){
			logger.fatal("{}: Unexpected exception during the run of the ACMinerDebugger.",t,cn);
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
