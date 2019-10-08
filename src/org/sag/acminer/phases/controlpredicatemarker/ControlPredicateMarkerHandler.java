package org.sag.acminer.phases.controlpredicatemarker;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.database.accesscontrol.AccessControlDatabaseFactory;
import org.sag.acminer.database.accesscontrol.IAccessControlDatabase;
import org.sag.common.io.FileHash;
import org.sag.common.io.FileHashList;
import org.sag.common.io.FileHelpers;
import org.sag.main.config.PhaseConfig;
import org.sag.main.phase.AbstractPhaseHandler;
import org.sag.main.phase.IPhaseHandler;
import org.sag.main.phase.IPhaseOption;
import org.sag.sootinit.IPASootLoader;

public class ControlPredicateMarkerHandler extends AbstractPhaseHandler {

	public static final String optEnableDebug = "EnableDebug";
	public static final String optDebugToConsole = "DebugToConsole";
	
	private Path jimpleJar;
	
	public ControlPredicateMarkerHandler(List<IPhaseHandler> depPhases, PhaseConfig pc) {
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
		if(isOptionEnabled("EnableDebug")) {
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
		return ((IACMinerDataAccessor)dataAccessor).getControlPredicatesDB().getFileHashList();
	}

	@Override
	protected void loadExistingInformation() throws Exception {
		if(!isSootInitilized())//To read in this database soot must be initialized completely
			initilizeSoot();
		IAccessControlDatabase db = AccessControlDatabaseFactory.readXmlControlPredicatesDatabase(null, getOutputFilePath());
		((IACMinerDataAccessor)dataAccessor).setControlPredicatesDB(db);
		logger.info("Control Predicates Before Filter: " + db.getUnits().size());
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
			ControlPredicateMarker marker = new ControlPredicateMarker(((IACMinerDataAccessor)dataAccessor),this,logger);
			if(!marker.markControlPredicates()){
				logger.fatal("{}: The ControlPredicateMarker encountered errors during executation.",cn);
				return false;
			}
			List<Path> fullPaths = dependencyFilePaths;
			List<Path> realtivePaths = new ArrayList<>(fullPaths.size());
			for(Path p : fullPaths) {
				realtivePaths.add(rootPath.relativize(p));
			}
			FileHashList fhl = FileHelpers.genFileHashList(fullPaths, realtivePaths);
			((IACMinerDataAccessor)dataAccessor).getControlPredicatesDB().setFileHashList(fhl);
			((IACMinerDataAccessor)dataAccessor).getControlPredicatesDB().writeXML(null, getOutputFilePath());
			logger.info("Control Predicates Before Filter: " + ((IACMinerDataAccessor)dataAccessor).getControlPredicatesDB().getUnits().size());
		}catch(Throwable t){
			logger.fatal("{}: Unexpected exception during the run of the ControlPredicateMarker.",t,cn);
			return false;
		}
		return true;
	}
	
}
