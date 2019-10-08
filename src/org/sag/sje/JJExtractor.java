package org.sag.sje;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

import org.sag.common.io.FileHelpers;
import org.sag.common.logging.ILogger;
import org.sag.main.AndroidInfo;
import org.sag.sje.dextra.DextraDexExtractor;
import org.sag.sje.smali.SmaliDexExtractor;
import org.sag.sje.soot.DexToJimpleDecompiler;
import org.sag.sje.vdexextractor.VdexExtractorDexExtractor;

public class JJExtractor {
	
	private ILogger logger;
	
	private JJExtractor(){
		this.logger = JJOptions.v().getMainLogger();
	}
	
	public static int run(Path pathToInputDir, Path pathToSystemImgZipIn, Path pathToSystemArchivesZipOut, Path pathToSystemJimpleJarOut, 
			Path pathToSystemJimpleFrameworkOnlyJarOut, Path pathToSystemJimpleClassConflictsZipOut,
			Path pathToWorkingDir, Path pathToAndroidInfo, Path pathToSystemClassJarOut, boolean useDextra, boolean disableAllAppsSameTime, 
			boolean disableDumpClasses, String bootClassPath, boolean includeApps, ILogger logger){
		if(useDextra)
			JJOptions.v().enableDextra();
		if(pathToInputDir != null)
			JJOptions.v().setInputDir(pathToInputDir);
		if(pathToSystemImgZipIn != null)
			JJOptions.v().setInput_SystemImgZip(pathToSystemImgZipIn);
		if(pathToSystemArchivesZipOut != null)
			JJOptions.v().setOutput_SystemArchivesZipFile(pathToSystemArchivesZipOut);
		if(pathToSystemJimpleJarOut != null)
			JJOptions.v().setOutput_SystemJimpleJarFile(pathToSystemJimpleJarOut);
		if(pathToSystemJimpleFrameworkOnlyJarOut != null)
			JJOptions.v().setOutput_SystemJimpleFrameworkOnlyJarFile(pathToSystemJimpleFrameworkOnlyJarOut);
		if(pathToSystemJimpleClassConflictsZipOut != null)
			JJOptions.v().setOutput_SystemJimpleClassConflictsZipFile(pathToSystemJimpleClassConflictsZipOut);
		if(pathToSystemClassJarOut != null)
			JJOptions.v().setOutput_SystemClassJarFile(pathToSystemClassJarOut);
		if(pathToWorkingDir != null)
			JJOptions.v().setOutput_WorkingDir(pathToWorkingDir);
		if(pathToAndroidInfo != null)
			JJOptions.v().setInput_AndroidInfo(pathToAndroidInfo);
		if(disableAllAppsSameTime)
			JJOptions.v().disableAllAppsSameTime();
		if(bootClassPath != null)
			JJOptions.v().setBootClassPath(bootClassPath);
		if(includeApps)
			JJOptions.v().enableIncludeApps();
		if(disableDumpClasses)
			JJOptions.v().disableDumpClasses();
		if(logger != null){
			JJOptions.v().setMainLogger(logger);
		}
		return new JJExtractor().runWrapper();
	}
	
	private int runWrapper() {
		AndroidInfo ai = null;
		int ret = 1;
		
		if(logger.equals(JJOptions.v().getDefaultMainLogger())) {
			logger.info("-------------------- Start JJExtractor --------------------");
		}
		
		try{
			ai = AndroidInfo.readXMLStatic(null, JJOptions.v().getInput_AndroidInfo());
		} catch (Throwable t) {
			logger.fatal("JJExtractor: Failed to read in the AndroidInfo file at path '{}'.",JJOptions.v().getInput_AndroidInfo());
			ret = 0;
		}
		
		if(ret != 0) {
			if(logger.equals(JJOptions.v().getDefaultMainLogger())) {
				logger.info(ai.toString());
			}
			
			try {
				ret = run(ai);
			} catch(Throwable t) {
				logger.fatal("JJExtractor: Unexpected error.",t);
				ret = 0;
			}
			
			if(logger.equals(JJOptions.v().getDefaultMainLogger())) {
				logger.info(ai.toString());
			}
		}
		
		if(logger.equals(JJOptions.v().getDefaultMainLogger())) {
			logger.info("-------------------- End JJExtractor --------------------");
		}
		
		return ret;
	}
	
	//0 complete failure, 1 complete success, 2 partial failure
	private int run(AndroidInfo ai){
		logger.info("JJExtractor: Decompiling all dex in the archives of system to jimple.");
		
		logger.info("JJExtractor: Initializing the extractor.");
		DexExtractor<? extends ArchiveEntry<? extends DexEntry>> extractor;
		if(ai.getApi() == 28) {
			extractor = new VdexExtractorDexExtractor(JJOptions.v().getBootClassPath(), JJOptions.v().getInputDir(), JJOptions.v().getInput_SystemImgZip(),
					JJOptions.v().getOutput_SystemArchivesZipFile(), JJOptions.v().getOutput_WorkingDir(), JJOptions.v().getInput_AndroidInfo(), 
					JJOptions.v().getLocations(), JJOptions.v().getArchs(), ai.getApi(),JJOptions.v().getMainLogger());
		} else if(JJOptions.v().isSmaliEnabled()) {
			extractor = new SmaliDexExtractor(JJOptions.v().getBootClassPath(),JJOptions.v().getInputDir(),JJOptions.v().getInput_SystemImgZip(),
					JJOptions.v().getOutput_SystemArchivesZipFile(),JJOptions.v().getOutput_WorkingDir(),JJOptions.v().getInput_AndroidInfo(),
					JJOptions.v().getLocations(),JJOptions.v().getArchs(),ai.getApi(),JJOptions.v().getMainLogger());
		} else if(JJOptions.v().isDextraEnabled()) {
			extractor = new DextraDexExtractor(JJOptions.v().getBootClassPath(),JJOptions.v().getInputDir(),JJOptions.v().getInput_SystemImgZip(),
					JJOptions.v().getOutput_SystemArchivesZipFile(),JJOptions.v().getOutput_WorkingDir(),JJOptions.v().getInput_AndroidInfo(),
					JJOptions.v().getLocations(),JJOptions.v().getArchs(),ai.getApi(),JJOptions.v().getMainLogger());
		} else {
			logger.fatal("JJExtractor: Failed to initialize the extractor. An extractor has not been selected.");
			return 0;
		}
		logger.info("JJExtractor: Successfully initialized the extractor.");
		
		boolean extractorSuccess;
		Set<? extends ArchiveEntry<? extends DexEntry>> archiveEntries = null;
		logger.info("JJExtractor: Extracting the dex containing archives of system using the specified extractor.");
		try{
			extractorSuccess = extractor.run();
			archiveEntries = extractor.getArchiveEntries();
		}catch(Throwable t){
			logger.fatal("JJExtractor: An unexpected error occured when running the extractor.",t);
			extractorSuccess = false;
		}
		
		if(extractorSuccess){
			logger.info("JJExtractor: Successfully extracted the dex containing archives of system using the specified extractor.");
		}else{
			logger.fatal("JJExtractor: Failed to extract the dex containing archives of system using the specified extractor.");
			if(archiveEntries == null || archiveEntries.isEmpty()){
				return 0;
			}else{
				logger.warn("JJExtractor: Some archive entries exist despite an error in the extractor. Attempting to covert any non-error "
						+ "archive entries to jimple.");
			}
		}
		
		logger.info("JJExtractor: Initializing the dex to jimple decompiler.");
		DexToJimpleDecompiler dexDecompiler = new DexToJimpleDecompiler(JJOptions.v().getBootClassPath(),JJOptions.v().getInputDir(),
				JJOptions.v().getInput_SystemImgZip(),archiveEntries,JJOptions.v().getOutput_WorkingDir(),
				JJOptions.v().getOutput_SystemArchivesZipFile(),JJOptions.v().getOutput_SystemJimpleJarFile(),
				JJOptions.v().getOutput_SystemJimpleFrameworkOnlyJarFile(),JJOptions.v().getOutput_SystemJimpleClassConflictsZipFile(),
				JJOptions.v().getInput_AndroidInfo(),JJOptions.v().getOutput_SystemClassJarFile(),ai.getApi(),ai.getJavaVersion(),
				JJOptions.v().getAllAppsSameTime(),JJOptions.v().getIncludeApps(),JJOptions.v().getDumpClasses(),
				JJOptions.v().getMainLogger());
		logger.info("JJExtractor: Successfully initialized the dex to jimple decompiler.");
		
		
		logger.info("JJExtractor: Decompiling all dex in the {}archives of system to jimple.",extractorSuccess?"":"non-error ");
		boolean decompilerSuccess;
		try{
			decompilerSuccess = dexDecompiler.run();
		}catch(Throwable t){
			logger.fatal("JJExtractor: An unexpected error occured when running the decompiler.",t);
			decompilerSuccess = false;
		}
		
		if(decompilerSuccess){
			logger.info("JJExtractor: Successfully decompiled all dex in the {}archives of system to jimple.",
					extractorSuccess ? "" : "non-error ");
		}else{
			logger.fatal("JJExtractor: Failed to decompile all dex in the {}archives of system to jimple.",
					extractorSuccess ? "" : "non-error ");
		}
		
		if(decompilerSuccess && extractorSuccess){
			return 1;
		}else if((extractorSuccess && !decompilerSuccess) || (!extractorSuccess && decompilerSuccess)){
			return 2;
		}else{
			return 0;
		}
	}
	
	//0 means failure, 1 means success, and 2 mean help dialog requested
	private int init(String[] args){
		try{
			logger.info("JJExtractor: Initilizing the system image Jimple Jar Extractor with the following arguments: {}",
					Arrays.toString(args));
			if(args == null || args.length <= 0){
				logger.info("JJExtractor: No arguments to parse. Skipping parsing procedure.");
			}else{
				logger.info("JJExtractor: Parsing arguments and setting options...");
				String inPath = null;
				String outPath = null;
				for(int i = 0; i < args.length; i++){
					switch(args[i]){
						case "-h":
						case "--help":
							logger.info("JJExtractor: Help dialog requested. Outputting dialog then exiting.\n\n"
									+ "System Image Jimple Jar Extractor. Written by Sigmund A. Gorski III.\n{}",errmsg);
							return 2;
						case "-o":
							outPath = args[++i];
							break;
						case "-i":
							inPath = args[++i];
							break;
						case "-s":
							JJOptions.v().enableSmali();
							break;
						case "-d":
							JJOptions.v().enableDextra();
							break;
						case "-t":
							JJOptions.v().disableAllAppsSameTime();
							break;
						case "-b":
							JJOptions.v().setBootClassPath(args[++i]);
						case "-a":
							JJOptions.v().enableIncludeApps();
						case "-c":
							JJOptions.v().disableDumpClasses();
						default:
							throw new RuntimeException("Error: Invalid Input '" + args[i] +"'.");
					}
				}
				if(outPath != null)
					JJOptions.v().setOutputDir(outPath);
				if(inPath != null)
					JJOptions.v().setInput(inPath);
			}
			logger.info("JJExtractor: All arguments were parsed successfully.");
		}catch(Throwable t){
			logger.fatal("JJExtractor: Something went wrong when initilizing the system image Jimple Jar Extractor with the "
					+ "following arguments: {}.\n\n{}",t,errmsg,args.toString());
			return 0;
		}
		
		logger.info("JJExtractor: Verifying options and performing pre-run procedures.");
		if(!verifyAndSetupOutput())
			return 0;
		if(!verifyInput())
			return 0;
		logger.info("JJExtractor: Options verified and pre-run completed successfully.");
		return 1;
	}
	
	private boolean verifyAndSetupOutput(){
		try{
			FileHelpers.processDirectory(JJOptions.v().getOutputDir(), true, false);
		} catch(Throwable t) {
			logger.fatal("JJExtractor: Could not access the system image Jimple Jar Extractor's output directory at '{}'.",t,
					JJOptions.v().getOutputDir());
			return false;
		}
		return true;
	}
	
	private boolean verifyInput(){
		//Verify that the input directory exists and is accessible
		try {
			FileHelpers.verifyRWDirectoryExists(JJOptions.v().getInputDir());
		} catch(Throwable t) {
			logger.fatal("JJExtractor: Failed to verify that the input directory exists and is accessible.",t);
			return false;
		}
		//Verify that the archive exists and we can access it
		try{
			FileHelpers.verifyRWFileExists(JJOptions.v().getInput_SystemImgZip());
		}catch(Throwable t){
			logger.fatal("JJExtractor: Failed to verify that the input system image archive exists and is accessible.",t);
			return false;
		}
		//Make sure the input system img zip is in the input dir
		if(!JJOptions.v().getInput_SystemImgZip().startsWith(JJOptions.v().getInputDir())){
			logger.fatal("JJExtractor: The system img zip path '" + JJOptions.v().getInput_SystemImgZip() + 
					"' is not a sub-path of input directory path '" + JJOptions.v().getInputDir() + "'.");
			return false;
		}
		//Verify that the AndroidInfo exists and we can access it
		try {
			FileHelpers.verifyRWFileExists(JJOptions.v().getInput_AndroidInfo());
		} catch(Throwable t) {
			logger.fatal("JJExtractor: Failed to verify that the AndroidInfo exists and is accessible.",t);
			return false;
		}
		//Make sure the AndroidInfo is in the input dir
		if(!JJOptions.v().getInput_AndroidInfo().startsWith(JJOptions.v().getInputDir())){
			logger.fatal("JJExtractor: The AndroidInfo path '" + JJOptions.v().getInput_AndroidInfo() + 
					"' is not a sub-path of input directory path '" + JJOptions.v().getInputDir() + "'.");
			return false;
		}
		return true;
	}
	
	public static void main(String[] args) {
		JJExtractor extractor = new JJExtractor();
		if(extractor.init(args) == 1){
			extractor.runWrapper();
		}
	}
	
	private static final String errmsg = 
		    "Usage: [-h|--help] [-o <dir>] [-i <dir>] [-s] [-d]\n"
		  + "\nGeneral Options:\n"
		  + "  -h, --help\n"
		  + "      # Print this message.\n"
		  + "  -o <dir> [default: \"./output\"]\n"
		  + "      # Set the output directory to <dir>.\n"
		  + "  -i <path> [default: \"./system_img.zip\"]\n"
		  + "      # Set the input <path>. The input path can be either the system_img.zip file\n"
		  + "      # or a directory containing said file. If the path is a directory, it must\n"
		  + "      # also contain the android_info.xml file. If the path is a file, its parent\n"
		  + "      # directory must contain the android_info.xml file.\n"
		  + "  -a\n"
		  + "      # Includes all apps in the 'app' and 'priv-app' folders of the input system\n"
		  + "      # image in the output 'system_jimple.jar' if there are any. By default these\n"
		  + "      # apps are not included and only the framework is output."
		  + "  -t\n"
		  + "      # Disables the loading of all apps at the same time and instead does them\n"
		  + "      # one at a time. Note '-a' must also be given for this option to have any\n"
		  + "      # value."
		  + "  -b <bootclasspath> [default: \"boot.oat\"]\n"
		  + "      # A ':' seperated list of the entries in the bootclasspath."
		  + "  -c\n"
		  + "      # Disables the dumping of class files when generating the jimple files."
		  + "\nSmali Options:\n"
		  + "  -s\n"
		  + "      # Enables the use of baksmali and smali for dex extraction and reassembling\n"
		  + "      # if such a task is required. Note baksmali and smali currently only\n"
		  + "      # Android 6. By default this option is enabled and only disabled if the use\n"
		  + "      # of dextra is requested instead.\n"
		  + "\nDextra Options:\n"
		  + "  -d\n"
		  + "      # Enables the use of dextra for dex extraction and reassembling\n"
		  + "      # if such a task is required. Note dextra supports Android 7 but has some\n"
		  + "      # bugs that still need to be worked out. By default this option is disabled\n"
		  + "      # and if enabled it will disable the use of baksmali and smali.\n"
		  ;
}
