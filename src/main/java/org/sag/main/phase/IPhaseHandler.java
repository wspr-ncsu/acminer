package org.sag.main.phase;

import java.nio.file.Path;
import java.util.List;

import org.sag.common.logging.ILogger;
import org.sag.main.AndroidInfo;
import org.sag.main.IDataAccessor;

public interface IPhaseHandler {

	/** Runs the IPhaseHandler if a run is determined to be needed. Whether or not a run is needed
	 * depends on if a forced run is enabled or if any of the IPhaseHandler's file dependencies 
	 * have changed. It returns 0 on failure, 1 on success, and 2 if a run was not required.
	 */
	public int runIfNeeded();
	
	public String getName();
	
	public List<Path> getInAndOutPaths(Path... paths);
	
	public Path getOutputFilePath();
	
	public List<Path> getOutputFilePaths();
	
	public List<Path> getDependencyFiles();
	
	public List<Path> getOtherFiles();
	
	public void init(IDataAccessor dataAccessor, AndroidInfo ai, ILogger logger);
	
	public void setPhaseOption(String optionName, String optionValue);
	
	public void enablePhase();
	
	public void enableForcedRun();
	
	public IPhaseOption<?> getPhaseOption(String optionName);
	
	public IPhaseOption<?> getPhaseOptionUnchecked(String optionName);
	
	public boolean isEnabled();
	
	public boolean isForcedRun();

	public int getLongestOptionName();

	public String getHelpDiag(String spacer, int longestOptionName);

	public List<Path> getOutputFilesForHelpDiag();

	public List<Path> getDependencyFilesForHelpDiag();

	public List<IPhaseHandler> getDepHandlers();
	
}
