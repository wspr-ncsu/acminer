package org.sag.sje.sootinit;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.sag.common.io.FileHelpers;
import org.sag.common.logging.ILogger;

import com.google.common.base.Joiner;

public class AllAppsDexSootLoader extends DexSootLoader {

	private static AllAppsDexSootLoader singleton;
	
	public static AllAppsDexSootLoader v(){
		if(singleton == null)
			singleton = new AllAppsDexSootLoader();
		return singleton;
	}
	
	public static void reset(){
		singleton = null;
	}
	
	private AllAppsDexSootLoader(){
		super(6);
	}
	
	public final boolean load(Set<Path> allAppPaths, Path pathToFrameworkJimpleJar, Set<String> classesToLoad, int apiVersion, int javaVersion, 
			ILogger logger){
		if(allAppPaths == null || allAppPaths.isEmpty() || pathToFrameworkJimpleJar == null || classesToLoad == null || classesToLoad.isEmpty() 
				|| apiVersion < 0 || logger == null)
			throw new IllegalArgumentException("Error: The arguments cannot be null, all lists must contain elements, and the api must be valid.");
		
		List<String> ins = new ArrayList<>(allAppPaths.size()+1);
		StringBuilder sb = new StringBuilder();
		for(Path p : allAppPaths) {
			if(p == null)
				throw new IllegalArgumentException("Error: The class path cannot contain null elements.");
			String path = FileHelpers.getNormAndAbsPath(p).toString();
			sb.append("  ").append(path).append("\n");
			ins.add(path);
		}
		String temp = FileHelpers.getNormAndAbsPath(pathToFrameworkJimpleJar).toString();
		sb.append("  ").append(temp).append("\n");
		ins.add(temp);
		
		String classpath = Joiner.on(File.pathSeparator).join(ins);
		logger.info("AllAppsDexSootLoader: Initilizing soot using the following apps, priv-apps, and framework jimple jar:\n{}",sb.toString());
		boolean ret = load(classpath,ins,classesToLoad,true,apiVersion,javaVersion,logger);
		getSootInstanceWrapper().setSootInit(getSootLoadKey());
		logger.info("AllAppsDexSootLoader: Soot has been initilized successfully for the following apps, priv-apps, and framework jimple jar:\n{}",
				sb.toString());
		
		return ret;
	}
	
}
