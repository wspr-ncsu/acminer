package org.sag.acminer.phases.acminer;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.database.acminer.IACMinerDatabase;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.io.FileHash;
import org.sag.common.io.FileHashList;
import org.sag.common.io.FileHelpers;
import org.sag.common.tools.SortingMethods;
import org.sag.main.config.PhaseConfig;
import org.sag.main.phase.AbstractPhaseHandler;
import org.sag.main.phase.IPhaseHandler;
import org.sag.main.phase.IPhaseOption;
import org.sag.sootinit.IPASootLoader;

import com.google.common.collect.ImmutableSet;

public class ACMinerHandler  extends AbstractPhaseHandler {
	
	public static final String optEnableDebug = "EnableDebug";
	public static final String optDebugToConsole = "DebugToConsole";
	public static final String optOnlyStubs = "OnlyStubs";
	public static final String optOnlyClasses = "OnlyClasses";
	
	private Path jimpleJar;
	private Set<String> onlyClasses;
	private Set<String> onlyStubs;
	
	public ACMinerHandler(List<IPhaseHandler> depPhases, PhaseConfig pc) {
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
		boolean enabledOnlyClasses = isOptionEnabled(optOnlyClasses);
		boolean enabledOnlyStubs = isOptionEnabled(ACMinerHandler.optOnlyStubs);
		if(enabledOnlyClasses && enabledOnlyStubs)
			throw new RuntimeException("Error: Please choose either '" + optOnlyClasses + "' or '" + optOnlyStubs + "'.");
		if(enabledOnlyClasses)
			onlyClasses = parseRestrictionFile((Path)getPhaseOptionUnchecked(ACMinerHandler.optOnlyClasses).getValue());
		else
			onlyClasses = null;
		if(enabledOnlyStubs)
			onlyStubs = parseRestrictionFile((Path)getPhaseOptionUnchecked(ACMinerHandler.optOnlyStubs).getValue());
		else
			onlyStubs = null;
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
		return ((IACMinerDataAccessor)dataAccessor).getACMinerDB().getFileHashList();
	}

	@Override
	protected void loadExistingInformation() throws Exception {
		//At the moment, it is not required that soot be initialized to read in this database
		//however if we are loading it here we will likely need soot to be initialized
		if(!isSootInitilized())
			initilizeSoot();
		((IACMinerDataAccessor)dataAccessor).setACMinerDB(IACMinerDatabase.Factory.readXML(null, getOutputFilePath()));
	}

	@Override
	protected boolean isSootInitilized() {
		return IPASootLoader.v().isSootLoaded();
	}

	@Override
	protected boolean initilizeSoot() {
		return IPASootLoader.v().load(((IACMinerDataAccessor)dataAccessor), jimpleJar, ai.getJavaVersion(), logger);
	}
	
	private Set<String> parseRestrictionFile(Path filePath) {
		if(FileHelpers.checkRWFileExists(filePath)) {
			Set<String> ret = new HashSet<>();
			try(BufferedReader br = Files.newBufferedReader(filePath)) {
				String l;
				while((l = br.readLine()) != null){
					l = l.trim();
					if(!l.startsWith("#")){
						ret.add(l);
					}
				}
				if(ret.isEmpty())
					return null;
				return ret;
			} catch(Throwable t) {}
		}
		return null;
	}
	
	private boolean isOnlyClass(String className){
		if(onlyClasses == null)
			return false;
		return onlyClasses.contains(className);
	}
	
	private boolean isOnlyStub(String stubName){
		if(onlyStubs == null)
			return false;
		return onlyStubs.contains(stubName);
	}
	
	private Set<EntryPoint> getEntryPointsInAnalysis() {
		Set<EntryPoint> allEntryPoints = ((IACMinerDataAccessor)dataAccessor).getEntryPoints();
		Set<EntryPoint> ret;
		
		if(onlyClasses == null && onlyStubs == null) {
			ret = allEntryPoints;
		} else {
			ret = new HashSet<>();
			for(EntryPoint e : allEntryPoints) {
				if(isOnlyStub(e.getStub().getName()) || isOnlyClass(e.getEntryPoint().getDeclaringClass().getName()))
					ret.add(e);
			}
			
			//Based on the current set of entry points in the analysis, use the referenced entry points
			//to generate a complete view of all entry points in the analysis
			Set<EntryPoint> toAdd = new HashSet<>();
			for(EntryPoint e : ret) {
				Queue<EntryPoint> toVisit = new ArrayDeque<>();
				Set<EntryPoint> visited = new HashSet<>();
				toVisit.add(e);
				while(!toVisit.isEmpty()) {
					EntryPoint cur = toVisit.poll();
					visited.add(cur);
					for(EntryPoint sm : ((IACMinerDataAccessor)dataAccessor).getEntryPointEdgesDB().getEntryPointEdges(cur)) {
						if(!visited.contains(sm) && !toVisit.contains(sm))
							toVisit.add(sm);
					}
				}
				toAdd.addAll(visited);
			}
			
			//Locate the EntryPoint object for each newly included entry point in the analysis
			for(EntryPoint e : allEntryPoints) {
				if(toAdd.contains(e))
					ret.add(e);
			}
			
			ret = ImmutableSet.copyOf(SortingMethods.sortSet(ret));
		}
		
		return ret;
	}
	
	@Override
	protected boolean doWork() {
		try {
			Set<EntryPoint> eps = getEntryPointsInAnalysis();
			IACMinerDatabase database = IACMinerDatabase.Factory.getNew(eps);
			ACMinerRunner minerRunner = new ACMinerRunner(((IACMinerDataAccessor)dataAccessor),this,database,eps,logger);
			if(!minerRunner.run()){
				logger.fatal("{}: Encountered errors during executation.",cn);
				return false;
			}
			List<Path> fullPaths = dependencyFilePaths;
			List<Path> realtivePaths = new ArrayList<>(fullPaths.size());
			for(Path p : fullPaths) {
				realtivePaths.add(rootPath.relativize(p));
			}
			FileHashList fhl = FileHelpers.genFileHashList(fullPaths, realtivePaths);
			database.setFileHashList(fhl);
			((IACMinerDataAccessor)dataAccessor).setACMinerDB(database);
			database.writeXML(null, getOutputFilePath());
		} catch(Throwable t) {
			logger.fatal("{}: Unexpected exception during the run.",t,cn);
			return false;
		}
		return true;
	}

}
