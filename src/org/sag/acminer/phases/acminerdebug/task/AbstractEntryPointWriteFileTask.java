package org.sag.acminer.phases.acminerdebug.task;

import java.nio.file.Path;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.concurrent.IgnorableRuntimeException;
import org.sag.common.io.FileHelpers;
import org.sag.common.logging.ILogger;

import soot.SootClass;
import soot.SootMethod;

public abstract class AbstractEntryPointWriteFileTask implements WriteFileTask {
	
	protected final ILogger logger;
	protected final EntryPoint ep;
	protected final int id;
	protected final IACMinerDataAccessor dataAccessor;
	protected final String cn;
	protected final Path rootOutputDir;
	
	public AbstractEntryPointWriteFileTask(EntryPoint ep, int id, IACMinerDataAccessor dataAccessor, Path rootOutputDir, 
			String cn, ILogger logger) {
		this.logger = logger;
		this.ep = ep;
		this.id = id;
		this.dataAccessor = dataAccessor;
		this.cn = cn;
		this.rootOutputDir = rootOutputDir;
	}
	
	//Create output directories for path
	protected Path getAndCreateStubOutputDir(SootClass stub) {
		Path stubOutputDir = null;
		try {
			String[] parts = stub.toString().split("\\.");
			for(int i = 0; i < parts.length; i++){
				parts[i] = FileHelpers.replaceWhitespace(FileHelpers.cleanFileName(parts[i]));
			}
			stubOutputDir = FileHelpers.getPath(rootOutputDir,parts);
			FileHelpers.processDirectory(stubOutputDir,true,false);
		} catch(Throwable t) {
			logger.fatal("{}: Failed to process the output directory for root output directory '{}' and stub '{}'.",
					t,cn,rootOutputDir,stub);
			throw new IgnorableRuntimeException();
		}
		return stubOutputDir;
	}
	
	//Create path to output file that does not exist
	protected Path getOutputFilePath(Path stubOutputDir, SootMethod m, String uniq, String ext) {
		Path output = null;
		try{
			StringBuilder sb2 = new StringBuilder();
			String className = m.getDeclaringClass().getShortName();
			int i3 = className.lastIndexOf('$');
			if(i3 > 0 && className.length() > 1){
				className = className.substring(i3+1);
			}
			sb2.append(FileHelpers.replaceWhitespace(FileHelpers.cleanFileName(className))).append("_");
			String retType = m.getReturnType().toString();
			int i = retType.lastIndexOf('.');
			if(i > 0 && retType.length() > 1) {
				retType = retType.substring(i+1);
			}
			int i2 = retType.lastIndexOf('$');
			if(i2 > 0 && retType.length() > 1){
				retType = retType.substring(i2+1);
			}
			sb2.append(FileHelpers.replaceWhitespace(FileHelpers.cleanFileName(retType))).append("_");
			sb2.append(FileHelpers.replaceWhitespace(FileHelpers.cleanFileName(m.getName())));
			output = FileHelpers.getPath(stubOutputDir, sb2.toString());
			
			StringBuilder sb3 = new StringBuilder();
			sb3.append("_").append(uniq).append(ext);
			output = FileHelpers.getPath(sb3.insert(0, FileHelpers.trimFullFilePath(output.toString(), false, sb3.length())).toString());
		}catch(Throwable t){
			logger.fatal("{}: Failed to construct the output file for output directory '{}' and method '{}'.",
					t,cn,stubOutputDir,m);
			throw new IgnorableRuntimeException();
		}
		return output;
	}

}

