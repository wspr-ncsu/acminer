package org.sag.sje.sootinit;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.sag.common.io.FileHelpers;
import org.sag.common.logging.ILogger;

import com.google.common.base.Joiner;

public class FrameworkDexSootLoader extends DexSootLoader {
	
	private static FrameworkDexSootLoader singleton;
	
	public static FrameworkDexSootLoader v(){
		if(singleton == null)
			singleton = new FrameworkDexSootLoader();
		return singleton;
	}
	
	public static void reset(){
		singleton = null;
	}
	
	private FrameworkDexSootLoader(){
		super(3);
	}
	
	public final boolean load(Set<Path> classPathElements, Set<String> classesToLoad, int apiVersion, int javaVersion, ILogger logger){
		if(classPathElements == null || classPathElements.isEmpty() || logger == null)
			throw new IllegalArgumentException("Error: The arguments cannot be null and a class path is required.");
		List<String> ins = new ArrayList<>(classPathElements.size());
		StringBuilder sb = new StringBuilder();
		for(Path p : classPathElements){
			if(p == null)
				throw new IllegalArgumentException("Error: The class path cannot contain null elements.");
			String path = FileHelpers.getNormAndAbsPath(p).toString();
			sb.append("  ").append(path).append("\n");
			ins.add(path);
		}
		String classpath = Joiner.on(File.pathSeparator).join(ins);
		
		logger.info("FrameworkDexSootLoader: Initilizing soot using the following framework archives:\n{}",sb.toString());
		boolean ret = load(classpath,ins,classesToLoad,false,apiVersion,javaVersion,logger);
		getSootInstanceWrapper().setSootInit(getSootLoadKey());
		logger.info("FrameworkDexSootLoader: Soot has been initilized successfully for the following framework archives:\n{}",sb.toString());
		return ret;
	}

}
