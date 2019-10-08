package org.sag.sje.phases.jimplejar;

import java.nio.file.Path;
import java.util.List;
import org.sag.common.io.FileHash;
import org.sag.main.config.PhaseConfig;
import org.sag.main.phase.AbstractPhaseHandler;
import org.sag.main.phase.IPhaseHandler;
import org.sag.sje.JJExtractor;
import org.sag.sootinit.BasicSootLoader;

public class JimpleJarHandler extends AbstractPhaseHandler {

	private Path sourceAndroidInfoFilePath;
	private Path sourceSystemClassJarFilePath;
	private Path sourceSystemImgZipFilePath;
	private Path pathToSystemArchivesZipOut;
	private Path pathToSystemJimpleFrameworkOnlyJarOut;
	private Path pathToSystemJimpleClassConflictsZipOut;
	private Path pathToWorkingDir;
	private Path pathToSystemClassJarOut;
	private boolean runJJExtractor;
	
	public JimpleJarHandler(List<IPhaseHandler> depPhases, PhaseConfig pc) {
		super(depPhases, pc);
		this.runJJExtractor = false;
	}
	
	@Override
	protected void initInner() {
		if(dependencyFilePaths.size() == 1) {
			runJJExtractor = false;
		} else if(dependencyFilePaths.size() == 2) {
			runJJExtractor = true;
		}
		
		List<Path> temp = otherFilePaths;
		this.sourceSystemClassJarFilePath = temp.get(4);
		this.sourceSystemImgZipFilePath = temp.get(5);
		this.sourceAndroidInfoFilePath = temp.get(6);
		
		if(sourceAndroidInfoFilePath == null || sourceSystemClassJarFilePath == null || sourceSystemImgZipFilePath == null)
			throw new IllegalArgumentException("A path to the AndroidInfo, system class jar file, and system img zip file must be given.");
		
		this.pathToSystemArchivesZipOut = temp.get(0);
		this.pathToSystemJimpleFrameworkOnlyJarOut = temp.get(1);
		this.pathToSystemJimpleClassConflictsZipOut = temp.get(2);
		this.pathToWorkingDir = temp.get(3);
		this.pathToSystemClassJarOut = temp.get(7);
	}

	@Override
	public boolean doWork() {
		try {
			if(runJJExtractor) {
				int ret = JJExtractor.run(rootPath, sourceSystemImgZipFilePath, pathToSystemArchivesZipOut, getOutputFilePath(), 
						pathToSystemJimpleFrameworkOnlyJarOut, pathToSystemJimpleClassConflictsZipOut, 
						pathToWorkingDir, sourceAndroidInfoFilePath, pathToSystemClassJarOut, false, true, false, null, false, logger);
				if(ret != 1) {
					logger.fatal("{}: Failed to generate the jimple jar at path '{}'.",cn,getOutputFilePath());
					return false;
				}
			} else {
				GenerateJimpleJar.generateArchive(getOutputFilePath(), rootPath, sourceSystemClassJarFilePath);
			}
		} catch(Throwable t) {
			logger.fatal("{}: Failed to generate the jimple jar at path '{}'.",t,cn,getOutputFilePath());
			return false;
		}
		return true;
	}

	@Override
	protected List<FileHash> getOldDependencyFileHashes() throws Exception {
		return GenerateJimpleJar.getSystemClassJarChecksum(getOutputFilePath());
	}

	@Override
	protected boolean isSootInitilized() {
		if(runJJExtractor)
			return true;
		return BasicSootLoader.v().isSootLoaded();
	}

	@Override
	protected boolean initilizeSoot() {
		return BasicSootLoader.v().load(sourceSystemClassJarFilePath, false, ai.getJavaVersion(), logger);
	}

	@Override
	protected void loadExistingInformation() throws Exception {}

}
