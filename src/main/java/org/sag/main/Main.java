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

}
