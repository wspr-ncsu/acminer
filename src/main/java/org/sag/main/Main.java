package org.sag.main;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.sag.common.io.FileHelpers;
import org.sag.common.logging.ILogger;
import org.sag.common.logging.LoggerWrapperSLF4J;
import org.sag.common.tools.TextUtils;
import org.sag.main.config.Config;
import org.sag.main.config.PhaseConfig;
import org.sag.main.logging.CentralLogger;
import org.sag.main.phase.IPhaseOption;
import org.sag.main.phase.PhaseManager;

public class Main {
	
	private ILogger logger;
	private AndroidInfo ai;
	private final Config config;
	private final IDataAccessor dataAccessor;
	private final String cn;
	private final PhaseManager pm;
	private final String helpMsg;
	
	private Main() {
		this.ai = null;
		this.logger = new LoggerWrapperSLF4J(this.getClass());
		this.cn = this.getClass().getSimpleName();
		this.config = Config.getConfigFromResources(logger);
		this.dataAccessor = config.getNewDataAccessor();
		this.pm = config.getNewPhaseManager();
		this.helpMsg = genHelpMsg();
	}
	
	public int init(String[] args) {
		try {
			logger.info("{}: Initializing with the following arguments: {}",cn,Arrays.toString(args));
			int ret = parseArgs(args);
			if(ret == 1) {
				logger.info("{}: Setting up the analysis environment.",cn);
				if(!setupEnv())
					ret = 0;
				else
					logger.info("{}: Successfully set up the analysis environment.",cn);
			}
			return ret;
		} catch(Throwable t) {
			logger.fatal("{}: Something went wrong when initializing.\n\n{}",t,cn,helpMsg);
			return 0;
		}
	}
	
	public int parseArgs(String[] args) {
		if(args == null || args.length == 0) {
			logger.fatal("{}: No arguments to parse.\n\n{}", cn,helpMsg);
			return 0;
		}
		logger.info("{}: Starting to parse arguments.", cn);
		for(int i = 0; i < args.length; i++) {
			switch(args[i]) {
				case "-h":
				case "--help":
					logger.info("{}: Help message requested.\n\n{}",cn,helpMsg);
					return 2;
				case "-i":
					String inPath = args[++i];
					if(inPath.length() > 0 && inPath.charAt(inPath.length()-1) == File.separatorChar)
						inPath = inPath.substring(0, inPath.length()-1);
					config.setFilePathEntry("work-dir", inPath);
					break;
				case "-p":
					List<String> phaseOptions = new ArrayList<>();
					try {
						for(i = i+1; i < args.length; i++) {
							if(args[i].startsWith("-"))
								break;
							phaseOptions.add(args[i]);
						}
						pm.setPhaseOptionsFromInput(phaseOptions);
						// Decrement by one because the outer loop will increment by one and we want the next index to be the current index
						i = i - 1;
					} catch(Throwable t) {
						logger.fatal("{}: Failed to parse the phase options {}.\n\n{}",t,cn,phaseOptions,helpMsg);
						return 0;
					}
					break;
				default: //Assume all other are a quick option for one of the phase groups
					try {
						pm.setQuickOptionsFromInput(args[i]);
					} catch(Throwable t) {
						logger.fatal("{}: Failed to parse the quick option {}.\n\n{}",t,cn,args[i],helpMsg);
						return 0;
					}
			}
		}
		logger.info("{}: Successfully parsed all arguments.", cn);
		return 1;
	}
	
	public boolean setupEnv() {
		Path rootDir = config.getFilePath("work-dir");
		Path androidInfo = config.getFilePath("work_android-info-file");
		Path logDir = config.getFilePath("log-dir");
		
		logger.info("{}: Setting up root input directory to '{}'.",cn,rootDir);
		try {
			FileHelpers.processDirectory(rootDir,false,false);
		} catch(Throwable t) {
			logger.fatal("{}: Could not access the root input directory at '{}'.",t,cn,rootDir);
			return false;
		}
		
		logger.info("{}: Reading in the AndroidInfo file at '{}'.",cn,androidInfo);
		try {
			FileHelpers.verifyRWFileExists(androidInfo);
		} catch(Throwable t) {
			logger.fatal("{}: Could not access the AndroidInfo file at '{}'.",t,cn,androidInfo);
			return false;
		}
		
		try {
			ai = AndroidInfo.readXMLStatic(null, androidInfo);
		} catch (Throwable t) {
			logger.fatal("{}: Failed to parse the AndroidInfo file at '{}'.",t,cn,androidInfo);
			return false;
		}
		
		logger.info("{}: Setting up the logging directory at '{}'.",cn,logDir);
		try {
			FileHelpers.processDirectory(logDir, true, true);
		} catch(Throwable t) {
			logger.fatal("{}: Failed to process the logging directory at '{}'.",t,cn,logDir);
			return false;
		}
		
		logger.info("{}: Starting the main logger.",cn);
		ILogger mainLogger = null;
		try {
			CentralLogger.setLogDir(logDir);
			CentralLogger.setMainLogFile(CentralLogger.getLogPath("MainLog"));
			CentralLogger.setAndroidInfo(ai);
			mainLogger = CentralLogger.startLogger(this.getClass().getName(), CentralLogger.getMainLogFile(), 
					CentralLogger.getLogLevelMain(), true);
		} catch(Throwable t) {
			logger.fatal("{}: Failed to start the main logger.",t,cn);
			return false;
		}
		if(mainLogger == null){
			logger.fatal("{}: Failed to start the main logger.",cn);
			return false;
		}
		logger.info("{}: Switching to main logger.",cn);
		logger.close();
		logger = mainLogger;
		logger.info("{}: Successfully switched to main logger.",cn);
		
		logger.info("{}: Initilizing all enabled phases and groups.",cn);
		try {
			pm.init(dataAccessor, ai, logger);
		} catch(Throwable t) {
			logger.fatal("{}: Failed to initilize all enabled phases and groups.",t,cn);
			return false;
		}
		
		logger.info("{}: Verifying the required input files exist.",cn);
		for(Path p : pm.getRequiredInputFilePaths()) {
			try {
				FileHelpers.verifyRWFileExists(p);
			} catch(Throwable t) {
				logger.fatal("{}: Could not access required input file '{}'.",t,cn,p);
				return false;
			}
		}
		return true;
	}
	
	public boolean run() {
		logger.info("{}: Starting the analysis.",cn);
		try {
			if(pm.run()) {
				logger.info("{}: The analysis completed successfully.",cn);
				return true;
			} else {
				logger.fatal("{}: The analysis did not complete successfully.",cn);
				return false;
			}
		} catch(Throwable t) {
			logger.fatal("{}: An unexpected error occured when running the analysis.",t,cn);
			return false;
		}
	}

	public static void main(String[] args) {
		Main main = new Main();
		int success = main.init(args);
		if(success == 1) {
			if(!main.run())
				success = 0;
		}
		
		if(success == 1 || success == 2)
			System.exit(0);
		else
			System.exit(1);
	}
	
	// TODO
	private final String genHelpMsg() {
		StringBuilder sb = new StringBuilder();
		sb.append("Usage:    ").append(TextUtils.wrap("[-h|--help] [-i <dir>] [-p <phase_group> <phase_1> " +
				"<phase_opt_1>:<value_1>,<phase_opt_2>:<value_2>,...,<phase_opt_n>:<value_n> <phase_2> " +
				"<phase_opt>:<value> ... <phase_n> <phase_opt>:<value>] [--<quick_option>]", 80,
				"\n", TextUtils.leftPad("", 10), 10, true));

		String jar = "";
		try {
			jar = "java -jar " + Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getFileName();
		} catch(Throwable throwable) {
			jar = "java -jar <jar>";
		}
		sb.append("\n\nExamples: ").append(TextUtils.wrap("[<jar> -h], [<jar> --ACMiner], [<jar> -p ACMiner ACMinerDebug enable:true,Paths:true,CGMethod:true,CGMethodLimit:5], [<jar> --ACMinerDebugWithAllOption]".replace("<jar>", jar),
				80, "\n", TextUtils.leftPad("", 10), 10, true));

		sb.append("\n\nOptions:  ");
		sb.append("\n  ").append(TextUtils.rightPad("-h|--help", 20)).append(" - ").append(TextUtils.wrap(
				"Display this help message.", 80, "\n", TextUtils.leftPad("", 25),
				25, true));
		sb.append("\n  ").append(TextUtils.rightPad("-i <dir>", 20)).append(" - ").append(TextUtils.wrap(
				"The path to the working directory. This directory should contain the input files for " +
						"whatever phases are enabled and the files should be in the appropriate directories. See the descriptions of " +
						"the phases below for specific file locations relative to this supplied working directory." +
						"This directory will also be used to write output files as indicated in the phase " +
						"descriptions below.", 80, "\n", TextUtils.leftPad("", 25),
				25, true));
		sb.append("\n  ").append(TextUtils.rightPad("-p <pg> <p> <po>:<v>", 20)).append(" - ").append(TextUtils.wrap(
				"Set the phase options for the phase groups that will run. The format of this is follows after the " +
						"indicator -p. 1) The <phase_group> (<pg>) specifies the phase group of the phases and " +
						"phase options. Phases may be used by multiple phase groups with different options in the same " +
						"run. 2) The <phase> (<p>) indicates the phase that the options are for. A phase identifier" +
						" is always followed by 3) a list of comma separated <phase_option> (<po>) and <value> (<v>)" +
						" pairs for the proceeding phase identifier. Each phase option and value pair is separated by" +
						" a colon. There can be as many phase option and value pairs in the comma separated list as needed" +
						" so long as they are for the proceeding phase identifier. The comma separated list of phase" +
						" options and value pairs should contain no spaces unless it is in the value field. Spaces in " +
						"the value field must be quoted. Multiple phase and phase option list pairs may be provided so" +
						" long as they are for the proceeding phase group. For a description of the phase groups," +
						" their phases, and their phase options see below.", 80, "\n",
				TextUtils.leftPad("", 25), 25, true));
		sb.append("\n  ").append(TextUtils.rightPad("--<quick_option>", 20)).append(" - ").append(TextUtils.wrap(
				"A means of quickly specifying commonly used phase options for a specific phase group. A quick " +
						"option id always begins with the phase group it is associated with followed by a unique " +
						"identifier that describes its purpose. Every phase group has at least one quick option (i.e. " +
						"the default) that is equal to the name of the phase group. This default is a quick way of specifying " +
						"that a complete run of the all phases in the phase group should be performed sans any " +
						"debugging phases or debugging phase options.", 80, "\n",
				TextUtils.leftPad("", 25), 25, true));

		sb.append("\n\nNote:     ").append(TextUtils.wrap("Enabling a phase in a phase group will automatically enable all " +
						"other phases in that group that the phase depends on. There is no need for the user to " +
						"individually enable each phase that needs to run. Simply enabling the last phase a user wishes to" +
						" run will enable all other required phases. The phases in each phase group listed below are " +
						"listed according to hierarchy (i.e. run order from first to last) and also indicate the phase or phases they " +
						"immediately depend on. For example, enabling the ACMiner phase of the ACMiner phase group " +
						"will enable all other phases listed above it sans any debugging phases.", 80,
				"\n", TextUtils.leftPad("", 10), 10, true));

		sb.append("\n\nNote:     ").append(TextUtils.wrap("The input files listed at the beginning of each " +
						"phase group are the minimum files a user must provide to be able to perform a full run " +
						"of the phase group. A full run is defined here as the the phases enabled by the default " +
						"quick option of a phase group (i.e. --ACMiner for the phase group ACMiner).", 80,
				"\n", TextUtils.leftPad("", 10), 10, true));

		sb.append("\n\n").append(pm.getHelpDiag(""));

		return sb.toString();
	}
	
	/*private static final String errmsg = 
		    "Usage: [-h|--help] [-o <dir>] [-m [-i <dir>] [--disableNativeWrapperIndicators]\n"
		  + "       [--minerDefaults] [--minerDefaultsWithDebugging]\n"
		  + "       [--minerDefaultsWithFullDebugging] [-b [--onlyClasses <file> |\n"
		  + "       --onlyStubs <file>] [--startAtProtectedOperations] [--removeLoops]\n"
		  + "       [--removeRecursion] [--suppressStdOut]\n"
		  + "       [--enableDebugOutput [--onlyKeepDebugOutputForErrors]\n"
		  + "       [--forceDebugOutputToConsole]]] [-pp <name> <args>]]\n"
		  + "\nGeneral Options:\n"
		  + "  -h, --help\n"
		  + "      # Print this message.\n"
		  + "  -o <dir> [default: \"./output\"]\n"
		  + "      # Set the output directory to <dir>.\n"
		  + "\nMiner Options:\n"
		  + "  -m\n"
		  + "      # Enables the Miner. The Miner must be enabled for any of the options\n"
		  + "      # below.\n"
		  + "  -i <dir> [default: \"./input\"]\n"
		  + "      # Set the input directory to <dir>. The input directory contains all the\n"
		  + "      # input files used by the Miner. Some of these files may be generated\n"
		  + "      # if they are missing or will be modified if changes are detected in their\n"
		  + "      # source files (i.e. the files in the input directory used to generate the\n"
		  + "      # files in question). At minimum the input directory must contain the\n"
		  + "      # following manually generated files from which all other files are\n"
		  + "      # input files are generated:\n"
		  + "      #   1. context_queries_db.txt - A manually defined list of context queries\n"
		  + "      #      and context queries that do not throw security exceptions.\n"
		  + "      #   2. excluded_elements_db.txt - A manually defined list of methods and\n"
		  + "      #      classes whose method bodies will be ingored during analysis. This\n"
		  + "      #      effectively makes these methods sinks in a call graph.\n"
		  + "      #   3. native_wrappers_db.txt - A manually defined list of methods and\n"
		  + "      #      classes who we consider as potential protected operations alongside\n"
		  + "      #      our general definition of potential protected operations (i.e. all\n"
		  + "      #      native methods).\n"
		  + "      #   4. system_class.jar - A jar containing all the class files of the\n"
		  + "      #      system image of the Android system.\n"
		  + "  --disableNativeWrapperIndicators\n"
		  + "      # Ingore indicators for native wrappers such that any native weapper\n"
		  + "      # with an indicator is ingored. This speeds up the loading of the native\n"
		  + "      # wrappers.\n"
		  + "  --minerDefaults\n"
		  + "      # Enables the default options for the miner phase. This is the same as\n"
		  + "      # providing the following set of options '-b --startAtProtectedOperations\n"
		  + "      # --removeLoops --removeRecursion\n"
		  + "      # --suppressStdOut'.\n"
		  + "  --minerDefaultsWithDebugging\n"
		  + "      # Enable the miner default options with the default options for debugging\n"
		  + "      # during the backwards analysis and control predicate marking. This is the\n"
		  + "      # same as providing the following set of options '--minerDefaults\n"
		  + "      # --enableDebugOutput --onlyKeepDebugOutputForErrors -pp\n"
		  + "      # ControlPredicateMarker EnableDebug:true'.\n"
		  + "  --minerDefaultsWithFullDebugging\n"
		  + "      # Enable the miner default options with the default options for debugging\n"
		  + "      # during the backwards analysis and during the pre-process phase. This\n"
		  + "      # is the same as providing the following set of options\n"
		  + "      # '--minerDefaultsWithDebugging -pp Debug enabled:true,All:5'.\n"
		  + "\nMiner Options - Special Call Graph Analysis:\n"
		  + "  --variedCallGraphAnalysis\n"
		  + "      # Enable the special call graph analysis pre-process phase and disable\n"
		  + "      # phases that occur after the CallGraph pre-process phase. This phase\n"
		  + "      # allows us to perform some special analysis on the call graph without\n"
		  + "      # requiring that we go through all the other phases after the call graph\n"
		  + "      # has been built. Basically, it is a short-cut option for enabling the\n"
		  + "      # VariedCallGraphAnalysis pre-process phase and disabling all phases\n"
		  + "      # after the CallGraph pre-process phase. Note as this is a pre-process\n"
		  + "      # phase it follows all of the options laid out below in the section\n"
		  + "      # describing pre-process phases.\n"
		  + "    Special Call Graph Analysis Options\n"
		  + "        # [Name= 'seandroid', Enable Value= 'path', Disable Value= 'false']\n"
		  + "            # Enable the seandroid special analysis taking in a path to a\n"
		  + "            # file containing the sinks for the analysis.\n"
		  + "\nMiner Options - Backwards Analysis:\n"
		  + "  -b\n"
		  + "      # Enable the backward analysis phase of the Miner that actually mines the\n"
		  + "      # constraints. The backward analysis phase of the Miner must be enabled\n"
		  + "      # for any options in this section to have any affect.\n"
		  + "  --onlyClasses <file>\n"
		  + "      # Limit the backwards analysis to the entry points found in the classes\n"
		  + "      # listed in the provided <file>\n"
		  + "  --onlyStubs <file>\n"
		  + "      # Limit the backwards analysis to all the entry points of the stub classes\n"
		  + "      # in the provided <file>\n"
		  + "  --startAtProtectedOperations\n"
		  + "      # For a given entry point with N protected operations, a backward analysis\n" 
		  + "      # reconstructing the constraint is run for all N protected operation with\n"
		  + "      # the backward analysis starting at the statement containing the protected\n"
		  + "      # operation and moving upwards to the entry point. All statements that\n"
		  + "      # occur after the protected operation are not considered in the constraint\n"
		  + "      # mining procedure. Thus each constraint is unique to each protected\n"
		  + "      # operation.\n"
		  + "  --removeLoops\n" 
		  + "      # Causes a loop to be only considered once for any given path in the\n"
		  + "      # backwards analysis phase.\n"
		  + "  --removeRecursion\n"
		  + "      # Ingores recursive calls in the backwards analysis phase. More\n"
		  + "      # specifically, for a single path if a method is already on the call stack\n"
		  + "      # then all subsiquent calls to the method are ingored.\n"
		  + "  --suppressStdOut\n"
		  + "      # For the backwards analysis, direct all output written to standard out\n"
		  + "      # and error to the null. (Cleans up the output)\n"
		  + "  --enableDebugOutput\n"
		  + "      # For the backwards analysis, this enables debugging output which is\n"
		  + "      # written to mutiple files whose file name and path are dependent\n"
		  + "      # on the protected operation being processed, the entry point being\n"
		  + "      # processed, and the number of times this protected operation has\n"
		  + "      # been seen. Note all debugging output is written to these files and\n"
		  + "      # none appears in the console output to avoid making the console output\n"
		  + "      # unreadable. If this option is not enabled then all debugging output\n"
		  + "      # is disabled.\n"
		  + "  --onlyKeepDebugOutputForErrors\n"
		  + "      # For the backwards analysis, this removes the debugging output files\n"
		  + "      # generated by the '--enableDebugOutput' unless an exception is thrown\n"
		  + "      # during the backwards analysis.\n"
		  + "  --forceDebugOutputToConsole\n"
		  + "      # For the backwards analysis, this forces the debugging output produced by\n"
		  + "      # '--enableDebugOutput' to the console. The debugging output will still\n"
		  + "      # appear in the files like normal.\n"
		  + "\nMiner Options - Pre-Processing:\n"
		  + "  -pp <name> <args>\n"
		  + "      # This sets various options for the pre-process phase name '<name>'.\n"
		  + "      # The <args> contain the actual options for the phase in the format\n"
		  + "      # 'option1:value1,option2:value2,option3:value3,...'. The available\n"
		  + "      # phases are as follows:\n"
		  + "    Pre-Process Phase Names\n"
		  + formatPhaseNames()
		  + "      # By defualt all phases have the following options:\n"
		  + "    Generic Pre-Processing Options\n"
		  + "        # Option Name - Enable Value - Disable Value\n"
		  + "        # enabled       true           false\n"
		  + "        # forced-run    true           false\n"
		  + "      # Note the 'enabled' option enables or disables the run of the\n"
		  + "      # pre-processing phase of name '<name>'. The 'forced-run' will force the\n"
		  + "      # pre-processing phase of name '<name>' to run if 'enabled' even if\n"
		  + "      # internal analysis would have determined it was not needed and prevented\n"
		  + "      # the phase from running. In addition, the phase named 'Debug' also has\n"
		  + "      # the following options available:\n"
		  + "    Debug Pre-Processing Options\n"
		  + "        # [Name= 'All', Enable Value= 'N >= 1', Disable Value= 'false']\n"
		  + "            # A shortcut to enable all the options of this catageory (i.e. those\n"
		  + "            # listed below this one) with 'N' being the limit for both\n"
		  + "            # CGMethodLimit and CGClassLimit.\n"
		  + "        # [Name= 'CFG', Enable Value= 'true', Disable Value= 'false']\n"
		  + "            # Output a CFG for each method containing at least one control\n"
		  + "            # predicate.\n"
		  + "        # [Name= 'CGMethod', Enable Value= 'true', Disable Value= 'false']\n"
		  + "            # Output a call graph for each entry point method.\n"
		  + "        # [Name= 'CGClass', Enable Value= 'true', Disable Value= 'false']\n"
		  + "            # Output a call graph for each entry point method where each node\n"
		  + "            # represents every method in a class (as oposed to each node\n"
		  + "            # representing a single method).\n"
		  + "        # [Name= 'CGMethodLimit', Enable Value= 'N >= 1', Disable Value='false']\n"
		  + "            # Output a call graph for each entry point method where the depth\n"
		  + "            # is limited to 'N', an integer value >= 1.\n"
		  + "        # [Name= 'CGClassLimit', Enable Value= 'N >= 1', Disable Value= 'false']\n"
		  + "            # Output a callgraph for each entry point method where each node\n"
		  + "            # represents every method in a class and where the depth is\n"
		  + "            # limited to 'N', an integer value >= 1.\n"
		  + "        # [Name= 'CGThrowSE', Enable Value= 'true', Disable Value= 'false']\n"
		  + "            # Output a call graph for each entry point method marking all\n"
		  + "            # methods that throw an SecurityException or that contain a context\n"
		  + "            # query.\n"
		  + "        # [Name= 'CGInac', Enable Value= 'true', Disable Value= 'false']\n"
		  + "            # Look for inaccuries in the call graph and dump them to a file.\n"
		  + "        # [Name= 'DataDumps', Enable Value= 'true', Disable Value= 'false']\n"
		  + "            # Dump various data generated by policy miner during the\n"
		  + "            # pre-process phase to a number of files.\n"
		  + "    Control Predicate Marker Options\n"
		  + "        # [Name= 'EnableDebug', Enable Value= 'true', Disable Value= 'false']\n"
		  + "            # Enabled the debugging output for the control predicate marker. The\n"
		  + "            # output is written to mutiple files whose file name and path are\n"
		  + "            # dependent on the the entry point being processed, stub being\n"
		  + "            # processed, and the number of times this exact path has been seen.\n"
		  + "            # Note all debugging output is written to these files and none \n"
		  + "            # appears in the console output to avoid making the console output \n"
		  + "            # unreadable. If this option is not enabled then all debugging\n"
		  + "            # output is disabled.\n"
		  + "        # [Name= 'DebugToConsole', Enable Value= 'true', Disable Value= 'false']\n"
		  + "            # Forces the debugging output of the control predicate marker to\n"
		  + "            # the console. The debugging output will still appear in the files\n"
		  + "            # like normal.\n"
		  ;
	
	private static final String formatPhaseNames(){
		StringBuilder sb = new StringBuilder();
		for(String s : PreProcessManager.defaultHandlerNameOptionsPaths.keySet()){
			sb.append("        # ").append(s).append("\n");
		}
		return sb.toString();
	}*/

}
