package org.sag.acminer.phases.controlpredicatefilter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.database.accesscontrol.AccessControlDatabaseFactory;
import org.sag.acminer.database.accesscontrol.IAccessControlDatabase;
import org.sag.acminer.database.filter.ControlPredicateFilterDatabase;
import org.sag.common.io.FileHash;
import org.sag.common.io.FileHashList;
import org.sag.common.io.FileHelpers;
import org.sag.main.config.PhaseConfig;
import org.sag.main.phase.AbstractPhaseHandler;
import org.sag.main.phase.IPhaseHandler;
import org.sag.main.phase.IPhaseOption;
import org.sag.sootinit.IPASootLoader;

public class ControlPredicateFilterHandler extends AbstractPhaseHandler {

	public static final String optEnableDebug = "EnableDebug";
	public static final String optDebugToConsole = "DebugToConsole";
	
	private Path jimpleJar;
	private Path controlPredicateFilter;
	private Path controlPredicateUnFiltered;
	private volatile boolean filteredWasLoaded;
	
	public ControlPredicateFilterHandler(List<IPhaseHandler> depPhases, PhaseConfig pc) {
		super(depPhases, pc);
		this.filteredWasLoaded = false;
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
		this.controlPredicateFilter = dependencyFilePaths.get(dependencyFilePaths.size()-1);
		this.controlPredicateUnFiltered = otherFilePaths.get(0);
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
		this.filteredWasLoaded = true;
		logger.info("Control Predicates After Filter: " + db.getUnits().size());
	}

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
			if(filteredWasLoaded) {
				//An existing FilteredCPDB was loaded to check if a run was needed and it is
				//Reload the UnFilteredCPDB so the filter can be applied to it
				((IACMinerDataAccessor)dataAccessor).setControlPredicatesDB(
						AccessControlDatabaseFactory.readXmlControlPredicatesDatabase(null, controlPredicateUnFiltered));
			}
			
			//Load the control predicate filter database for use
			ControlPredicateFilterDatabase cpFilter = ControlPredicateFilterDatabase.readXMLStatic(null, controlPredicateFilter);
			ControlPredicateFilter filter = new ControlPredicateFilter(cpFilter, (IACMinerDataAccessor)dataAccessor, this, logger);
			
			if(!filter.filterControlPredicates()) {
				logger.fatal("{}: The {} encountered errors during executation.",cn,getName());
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
			logger.info("Control Predicates After Filter: " + ((IACMinerDataAccessor)dataAccessor).getControlPredicatesDB().getUnits().size());
		}catch(Throwable t){
			logger.fatal("{}: Unexpected exception during the run of the {}.",t,cn,getName());
			return false;
		}
		return true;
	}
	
}
