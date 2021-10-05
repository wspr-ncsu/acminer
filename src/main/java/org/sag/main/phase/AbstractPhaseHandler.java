package org.sag.main.phase;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.sag.common.io.FileHash;
import org.sag.common.io.FileHashList;
import org.sag.common.io.FileHelpers;
import org.sag.common.logging.ILogger;
import org.sag.common.logging.LoggerWrapperSLF4J;
import org.sag.main.AndroidInfo;
import org.sag.main.IDataAccessor;
import org.sag.main.config.PhaseConfig;
import org.sag.main.phase.IPhaseOption.*;

import com.google.common.collect.ImmutableList;

/**
 * @author agorski
 *
 */
public abstract class AbstractPhaseHandler implements IPhaseHandler {
	
	public static final String optEnabled = "enable";
	public static final String optForcedRun = "force-run";

	protected final PhaseConfig pc;
	protected final String cn;
	protected final List<IPhaseOption<?>> options;
	protected final List<IPhaseHandler> depPhases;
	
	protected Path rootPath;
	protected List<Path> outFilePaths;
	protected List<Path> dependencyFilePaths;
	protected List<Path> otherFilePaths;
	protected AndroidInfo ai;
	protected ILogger logger;
	protected IDataAccessor dataAccessor;
	
	public AbstractPhaseHandler(List<IPhaseHandler> depPhases, PhaseConfig pc) {
		this.cn = this.getClass().getSimpleName();
		this.pc = pc;
		this.depPhases = depPhases;
		this.options = new ArrayList<>();
		this.options.add(new BooleanOption(optEnabled, "Specifies if the phase '" + getName() + "' is enabled. Thus the phase may run if a run is required"
				+ " as determined by the input and output files for the phase."));
		this.options.add(new BooleanOption(optForcedRun, "If the phase '" + getName() + "' is enabled setting this option forces the phase to run even if"
				+ " the phase output files exist and no changes are detected in the required phase input files."));
		this.options.addAll(pc.getOptions().values());
		
		this.logger = null;
		this.ai = null;
		this.dataAccessor = null;
		this.rootPath = null;
		this.outFilePaths = null;
		this.dependencyFilePaths = null;
		this.otherFilePaths = null;
	}
	
	public final void init(IDataAccessor dataAccessor, AndroidInfo ai, ILogger logger) {
		if(ai == null)
			throw new IllegalArgumentException("A AndroidInfo is required.");
		this.logger = logger == null ? new LoggerWrapperSLF4J(this.getClass()) : logger;
		this.ai = ai;
		this.dataAccessor = dataAccessor;
		
		this.rootPath = pc.getRootPath();
		this.outFilePaths = pc.getOutputPaths();
		if(rootPath == null || outFilePaths == null)
			throw new IllegalArgumentException("A rootPath and outFilePaths are required.");
		for(Path p : outFilePaths) {
			if(p == null)
				throw new IllegalArgumentException("The outFilePaths cannot contain null.");
		}
		this.dependencyFilePaths = pc.getDependencyPaths();
		this.otherFilePaths = pc.getOtherPaths();
		if(isEnabled()) {
			try {
				FileHelpers.processDirectory(rootPath, true, false);
			} catch(Throwable t) {
				throw new RuntimeException("Error: Failed to process root directory '" + rootPath + "'",t);
			}
			initInner();
		}
	}
	
	public String getName() {
		return pc.getPhaseName();
	}
	
	/** A convince method for when there is only a single output file path. Note since 
	 * a output file path cannot be null, this method will return a empty path object
	 * if the list is empty.
	 */
	public Path getOutputFilePath() {
		Path p = outFilePaths.get(0);
		if(p == null)
			return Paths.get("");
		return p;
	}
	
	public List<Path> getOutputFilePaths() {
		return outFilePaths;
	}
	
	public List<Path> getInAndOutPaths(Path... paths) {
		return ImmutableList.<Path>builder().addAll(dependencyFilePaths).addAll(outFilePaths).add(paths).build();
	}
	
	public List<Path> getDependencyFiles() {
		return dependencyFilePaths;
	}
	
	public List<Path> getOtherFiles() {
		return otherFilePaths;
	}
	
	public void setPhaseOption(String optionName, String optionValue) {
		for(IPhaseOption<?> option : options) {
			if(option.getName().equals(optionName)) {
				if(option.setValueFromInput(optionValue)) {
					//For default options make sure enabling them also propagates to the 
					//dependency phases
					if(optEnabled.equals(optionName) && option.isEnabled()) {
						enablePhase();
					} else if(optForcedRun.equals(optionName) && option.isEnabled()) {
						enableForcedRun();
					}
					return;
				} else {
					throw new RuntimeException("Error: Could not set '" + optionName + "' to value '" + optionValue + "' for phase '" + getName() 
							+ "'. Please review the option description for correct formating: " + option.getDescription());
				}
			}
		}
		throw new RuntimeException("Error: The option '" + optionName + "' does not exist for phase '" + getName() + "'.");
	}
	
	public IPhaseOption<?> getPhaseOption(String optionName) {
		for(IPhaseOption<?> option : options) {
			if(option.getName().equals(optionName))
				return option;
		}
		throw new RuntimeException("Error: Unable to find option '" + optionName + "' for phase '" + getName() + "'.");
	}
	
	public IPhaseOption<?> getPhaseOptionUnchecked(String optionName) {
		for(IPhaseOption<?> option : options) {
			if(option.getName().equals(optionName))
				return option;
		}
		return null;
	}
	
	private String getEnabledOptionsAsString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[ ");
		boolean first = true;
		for(IPhaseOption<?> option : options) {
			if(option.isEnabled()) {
				if(first)
					first = false;
				else
					sb.append(" , ");
				sb.append(option.toString());
			}
		}
		sb.append(" ]");
		return sb.toString();
	}
	
	public int runIfNeeded(){
		if(isEnabled()){
			boolean runIsNeeded = false;
			logger.info("Performing run of {} if needed with the following options:\n\t{}.",cn,getEnabledOptionsAsString());
			
			try{
				runIsNeeded = isRunNeeded();
			}catch(Throwable t){
				logger.fatal("Error: Could not determine if analysis is needed for {}.",t,cn);
				return 0;
			}
			
			if(runIsNeeded){
				logger.info("A run of {} is required.",cn);
				
				if(!isSootInitilized()){
					logger.info("Soot is not initilized. Initilizing Soot for {}.",cn);
					try{
						if(!initilizeSoot()) {
							logger.fatal("Error: Failed to initilize soot for {}.",cn);
							return 0;
						}
					}catch(Throwable t){
						logger.fatal("Error: Failed to initilize soot for {}.",t,cn);
						return 0;
					}
					logger.info("Soot has been initilized successfully for {}.",cn);
				}else{
					logger.info("Soot is already initilized or is not required for {}.",cn);
				}
				
				logger.info("Performing a run of {}.",cn);
				try{
					if(!doWork()){
						logger.fatal("Error: Failed perform a run of {}.",cn);
						return 0;
					}
					//Output file may have been updated so remove any previous cache of file
					for(Path p : outFilePaths) {
						FileHelpers.removeCachedFileHash(p);
					}
				}catch(Throwable t){
					logger.fatal("Error: Unexpected exception. Failed perform a run of {}.",t,cn);
					return 0;
				}
				logger.info("A run of {} completed successfully.",cn);
				
			}else{
				logger.info("A run of {} is not required.",cn);
				return 2;
			}
			return 1;
		}
		logger.info("{} is disabled and will not be run.",cn);
		return 3;
	}
	
	public boolean isEnabled() {
		return getPhaseOption(optEnabled).isEnabled();
	}
	
	public boolean isForcedRun() {
		return getPhaseOption(optForcedRun).isEnabled();
	}
	
	public void enablePhase() {
		IPhaseOption<?> temp = getPhaseOption(optEnabled);
		if(!temp.isEnabled())
			temp.toggleIsEnabled();
		if(depPhases != null && depPhases.size() != 0) {
			for(IPhaseHandler depPhase : depPhases) {
				depPhase.enablePhase();
			}
		}
	}
	
	public void enableForcedRun() {
		IPhaseOption<?> temp = getPhaseOption(optForcedRun);
		if(!temp.isEnabled())
			temp.toggleIsEnabled();
	}
	
	/**
	 * Determines if the analysis needs to run. "forced-run" is enabled or if 
	 * the {@link #outputFilePath} does not exist then run. If {@link #outputFilePath}
	 * exists but is not a file or is not RW able then an exception is thrown.
	 * Otherwise, it compares the old dependency file list (based off of the files stored
	 * in the file hash structures) to those given in {@link #dependencyFilePaths}
	 * and if they are different then run. Finally, it compares the hash in the file hash
	 * structures that were generated in the past to those that currently exist and if all
	 * do not match then run. Otherwise, do not run.
	 */
	protected boolean isRunNeeded() throws Exception {
		//if forced or one of the output file paths does not exist then always run
		if(isForcedRun())
			return true;
		for(Path p : outFilePaths) {
			if(!Files.exists(p))
				return true;
		}
		
		//At this point we need access to the files so double check that
		for(Path p : outFilePaths) {
			FileHelpers.verifyRWFileExists(p);
		}
		
		loadExistingInformation();//Loads the existing information from file(s) if needed
		
		List<FileHash> oldDependencyFileHashes = getOldDependencyFileHashes();
		List<Path> oldDependencyFilePaths = new ArrayList<>();
		
		//generate the oldDependencyFilePaths
		for(FileHash oldFileHash : oldDependencyFileHashes){
			oldDependencyFilePaths.add(oldFileHash.getFullPath(rootPath));//from the hash information reconstruct the full path to the old Dependency file and add to set
		}
		
		//make sure there are no changes the the file dependencies
		//do this before the comparison of the file hashes so we can be sure that all old dependency files are actually needed
		//and if not some change occurred so we need to rerun the analysis anyways
		if(!dependencyFilePaths.equals(oldDependencyFilePaths))
			return true;
		
		//Since there are no changes in the file dependencies, that means the files must exist and if they don't we have a problem (i.e. an exception gets thrown)
		//Generate the file hashes of each dependency file (under this assumption) and compare the the has of each dependency file on record
		//If they all match then nothing has changed and nothing needs to be done else need to rerun analysis
		for(FileHash oldFileHash : oldDependencyFileHashes){
			if(!oldFileHash.compareHash(FileHelpers.genFileHash(oldFileHash.getFullPath(rootPath), oldFileHash.getPath())))
				return true;
		}
		
		return false;
	}
	
	/**
	 * Generate the list of dependency file hashes based off of the hashes previously
	 * generated and stored in this {@link #outputFilePath} file. How this list is 
	 * generated and what it contains are implementation dependent.
	 * @return the list of FileHashes
	 * @throws Exception upon some error reading the information from file
	 */
	protected abstract List<FileHash> getOldDependencyFileHashes() throws Exception;
	
	protected abstract void loadExistingInformation() throws Exception;
	
	protected abstract boolean isSootInitilized();
	
	protected abstract boolean initilizeSoot();
	
	protected abstract boolean doWork();
	
	protected void initInner() {}
	
	protected FileHashList makeFileHashList() throws Exception {
		return FileHelpers.genFileHashList(dependencyFilePaths, rootPath);
	}

}
