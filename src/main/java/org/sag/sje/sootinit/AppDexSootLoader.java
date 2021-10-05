package org.sag.sje.sootinit;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.sag.common.io.FileHelpers;
import org.sag.common.logging.ILogger;

public class AppDexSootLoader extends DexSootLoader {

	private static AppDexSootLoader singleton;
	
	public static AppDexSootLoader v(){
		if(singleton == null)
			singleton = new AppDexSootLoader();
		return singleton;
	}
	
	public static void reset(){
		singleton = null;
	}
	
	private AppDexSootLoader(){
		super(4);
	}
	
	public final boolean load(Path pathToApp, Path pathToFrameworkJimpleJar, Set<String> classesToLoad, int apiVersion, int javaVersion, 
			ILogger logger){
		String pathToAppStr = FileHelpers.getNormAndAbsPath(pathToApp).toString(); 
		String pathToFrameworkJimpleStr = FileHelpers.getNormAndAbsPath(pathToFrameworkJimpleJar).toString();
		String classpath = pathToAppStr + File.pathSeparator + pathToFrameworkJimpleStr;
		List<String> ins = new ArrayList<>();
		ins.add(pathToAppStr);
		ins.add(pathToFrameworkJimpleStr);
		
		logger.info("AppDexSootLoader: Initilizing soot for the app at '{}' using the framework at '{}'.",pathToAppStr,pathToFrameworkJimpleStr);
		boolean ret = load(classpath,ins,classesToLoad,true,apiVersion,javaVersion,logger);
		getSootInstanceWrapper().setSootInit(getSootLoadKey());
		logger.info("AppDexSootLoader: Soot had been initilized successfully for the app at '{}' using the framework at '{}'.",pathToAppStr,
				pathToFrameworkJimpleStr);
		return ret;
	}
	
}
