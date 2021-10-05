package org.sag.acminer.scripts;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.database.accesscontrol.AccessControlDatabaseFactory;
import org.sag.acminer.phases.bindergroups.BinderGroupsDatabase;
import org.sag.acminer.phases.controlpredicatefilter.ControlPredicateFilterHandler;
import org.sag.acminer.phases.entrypoints.EntryPointsDatabase;
import org.sag.acminer.sootinit.BasicSootLoader;
import org.sag.common.io.FileHelpers;
import org.sag.common.logging.ILogger;
import org.sag.common.logging.LoggerWrapperSLF4J;
import org.sag.main.AndroidInfo;
import org.sag.main.config.Config;
import org.sag.main.logging.CentralLogger;
import org.sag.main.phase.AbstractPhaseHandler;
import org.sag.main.phase.PhaseManager;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

public class ACMinerAnalysisHelpers {
	
	private static final String defaultIn = "input";
	
	private AndroidInfo ai;
	private ILogger logger;
	private String inPath;
	private boolean isBasicSootNeeded;
	private boolean isEpNeeded;
	private boolean isBinderGroupsNeeded;
	private boolean isCPNeeded;
	private boolean isCPFilterNeeded;
	private boolean analyizeCP;
	private boolean testCP;
	private boolean dumpInvokeSigs;
	private String methodNamePattern;
	private final String cn;
	private final Config config;
	
	private ACMinerAnalysisHelpers(){
		inPath = null;
		isBasicSootNeeded = false;
		isEpNeeded = false;
		isBinderGroupsNeeded = false;
		isCPNeeded = false;
		isCPFilterNeeded = false;
		analyizeCP = false;
		testCP = false;
		dumpInvokeSigs = false;
		logger = new LoggerWrapperSLF4J(this.getClass());
		config = Config.getConfigFromResources(logger);
		cn = getClass().getSimpleName();
		this.ai = null;
		this.methodNamePattern = null;
	}
	
	public int init(String[] args){
		try{
			logger.info("{}: Initilizing with the following arguments: {}",cn,Arrays.toString(args));
			if(parseAndSetArguments(args)){
				logger.info("{}: Verifying options set by the parsed arguments and performing further initilization procedures.",cn);
				verifyOptions(inPath);
				logger.info("{}: Options verified successfully. Initilized successfully.",cn);
				return 1;
			}
			return 2;
		}catch(Throwable t){
			logger.fatal("{}: Something went wrong when initilizing the with the following arguments: {}.\n\n{}",t,cn,
					"",args.toString());
			return 0;
		}
	}
	
	//Throws an exception on failure, returns false if help is requested, returns true otherwise
	public boolean parseAndSetArguments(String[] args){
		if(args == null || args.length <= 0){
			logger.info("{}: No arguments to parse. Skipping parsing procedure.",cn);
		}else{
			logger.info("{}: Parsing arguments and setting options...",cn);
			for(int i = 0; i < args.length; i++){
				switch(args[i]){
					case "-h":
					case "--help":
						logger.info("{}: Help dialog requested. Outputting dialog then exiting.\n\n"
								+ "Written by Sigmund A. Gorski III.\n{}",cn,"");
						return false;
					case "-i":
						this.inPath = args[++i];
						break;
					case "--analyizeCP":
						isBasicSootNeeded = true;
						isCPNeeded = true;
						isCPFilterNeeded = true;
						analyizeCP = true;
						break;
					case "--testCP":
						isBasicSootNeeded = true;
						isCPNeeded = true;
						testCP = true;
						break;
					case "--DumpInvokeSignatures":
						isBasicSootNeeded = true;
						dumpInvokeSigs = true;
						isEpNeeded = true;
						isBinderGroupsNeeded = true;
						break;
					case "--DumpMethodNamesMatch":
						isBasicSootNeeded = true;
						this.methodNamePattern = args[++i];
						break;
					default:
						throw new RuntimeException("Error: Invalid Input '" + args[i] +"'.");
				}
			}
		}
		logger.info("{}: All arguments were parsed successfully.",cn);
		return true;
	}
	
	public void verifyOptions(String inPath){
		if(inPath == null)
			this.inPath = defaultIn;
		else
			this.inPath = inPath;
		
		verifyAndSetInput(this.inPath);
	}
	
	public void verifyAndSetInput(String inPath){
		if(inPath.length() > 0 && inPath.charAt(inPath.length()-1) == File.separatorChar){
			inPath = inPath.substring(0, inPath.length()-1);
		}
		
		config.setFilePathEntry("work-dir", inPath);
		Path rootDir = config.getFilePath("work-dir");
		try{
			FileHelpers.processDirectory(rootDir,false,false);
		} catch(Throwable t) {
			throw new RuntimeException("Error: Could not access root directory.",t);
		}
		
		Path debugDir = config.getFilePath("debug-dir");
		try{
			FileHelpers.processDirectory(debugDir,true,false);
		} catch(Throwable t) {
			throw new RuntimeException("Error: Could not access debug directory.",t);
		}
		
		Path androidInfo = config.getFilePath("work_android-info-file");
		try {
			FileHelpers.verifyRWFileExists(androidInfo);
		} catch(Throwable t) {
			throw new RuntimeException("Error: Could not access the AndroidInfo file at path '"
					+ androidInfo + "'.");
		}
		
		try{
			ai = AndroidInfo.readXMLStatic(null, androidInfo);
		} catch (Throwable t) {
			throw new RuntimeException("Error: Could not read in the AndroidInfo file at path '" + androidInfo + "'.");
		}
		
		Path logDir = config.getFilePath("log-dir");
		try{
			FileHelpers.processDirectory(logDir, true, false);
		} catch(Throwable t){
			throw new RuntimeException("Error: Failed to process log directory '" + logDir + "'.",t);
		}
		CentralLogger.setLogDir(logDir);
		CentralLogger.setMainLogFile(CentralLogger.getLogPath("log"));
		CentralLogger.setAndroidInfo(ai);
	}
	
	public boolean run(){
		try {
			logger.info("{}: Running...",cn);
			IACMinerDataAccessor dataAccessor = (IACMinerDataAccessor)(config.getNewDataAccessor());
			logger.info("{}: Starting the main logger.",cn);
			
			//Setup the main logger
			ILogger mainLogger = null;
			try{
				mainLogger = CentralLogger.startLogger(this.getClass().getName(), CentralLogger.getMainLogFile(), 
						CentralLogger.getLogLevelMain(), true);
			}catch(Throwable t){
				logger.fatal("{}: Failed to start the main logger.",t,cn);
				return false;
			}
			if(mainLogger == null){
				logger.fatal("{}: Failed to start the main logger.",cn);
				return false;
			}
			logger.info("{}: Switching to main logger.",cn);
			
			try {
				if(isBasicSootNeeded && !BasicSootLoader.v().isSootLoaded()) {
					BasicSootLoader.v().load(config.getFilePath("sje_system-jimple-jar-file"),true,ai.getJavaVersion(),mainLogger);
				}
				if(isEpNeeded) {
					EntryPointsDatabase.readXMLStatic(null, config.getFilePath("acminer_entry-points-db-file"));
				}
				if(isBinderGroupsNeeded) {
					BinderGroupsDatabase.readXMLStatic(null, config.getFilePath("acminer_binder-groups-db-file"));
				}
				if(isCPNeeded) {
					dataAccessor.setControlPredicatesDB(AccessControlDatabaseFactory.readXmlControlPredicatesDatabase(null, 
							config.getFilePath("acminer_control-predicates-db-file")));
				}
				if(isCPFilterNeeded) {
					PhaseManager pm = config.getNewPhaseManager();
					pm.enablePhaseGroup("ACMiner");
					pm.setPhaseOptionForHandler("ACMiner", ControlPredicateFilterHandler.class, AbstractPhaseHandler.optEnabled, "true");
					pm.init(dataAccessor, ai, mainLogger);
					pm.run();
				}
				if(testCP) {
					dataAccessor.getControlPredicatesDB().writeXML(null, FileHelpers.getPath(config.getFilePath("debug-dir"), "CPTEST.xml"));
				}
				if(analyizeCP) {
					new AnalyizeControlPredicates(config.getFilePath("debug-dir"), dataAccessor, true, mainLogger).run();
				}
				if(dumpInvokeSigs) {
					new DumpInvokeSignatures(config.getFilePath("debug-dir"), mainLogger, dataAccessor).run();
				}
				if(methodNamePattern != null) {
					for(SootClass sc : Scene.v().getClasses()) {
						for(SootMethod sm : sc.getMethods()) {
							if(sm.getName().equals(methodNamePattern)) {
								System.out.println(sm.toString());
							}
						}
					}
				}
			} catch (Throwable t) {
				mainLogger.fatal("{}: An error occured.",t,cn);
				return false;
			}
		} catch(Throwable t) {
			logger.fatal("{}: Unexpected error.",t,cn);
			return false;
		}
		return true;
	}
	
	public static void main(String[] args) {
		ACMinerAnalysisHelpers main = new ACMinerAnalysisHelpers();
		int success = main.init(args);//Handles exceptions internally
		if(success == 1){
			main.run();//Handles exceptions internally
		}
	}
	
}
