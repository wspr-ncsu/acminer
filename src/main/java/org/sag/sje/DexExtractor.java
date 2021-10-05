package org.sag.sje;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sag.common.io.FileHelpers;
import org.sag.common.io.MakeEmptyJar;
import org.sag.common.io.archiver.SimpleArchiver;
import org.sag.common.logging.ILogger;
import org.sag.common.tools.LinuxOrWSLCommandBuilder;
import org.sag.common.tools.SortingMethods;
import org.sag.common.tuple.Pair;
import org.sag.sje.DexEntry.State;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;

public abstract class DexExtractor<T extends ArchiveEntry<? extends DexEntry>> {
	
	protected final String bootClassPath;
	protected List<String> resolvedBootClassPath;
	protected Path pathToSystemArchivesZipFile;//output - overwrite on write
	protected Path pathToSystemImgZipFile;//input
	protected Path pathToAndroidInfoFile;//input
	protected Path pathToInputDir;
	protected Path pathToWorkingDir;//working
	protected Path pathToWorkingSystemDir;//working
	protected Path[] pathToWorkingSystemLocationDirs;//working
	protected Path pathToWorkingDexDir;//working
	protected Path[] pathToWorkingDexLocationDirs;//working
	protected Path pathToWorkingArchiveDir;//working
	protected Path[] pathToWorkingArchiveLocationDirs;//working
	protected Path pathToWorkingApexDir;//working
	protected Path[] pathToWorkingApexLocationDirs;//working
	protected Path pathToWorkingApexSystemDir;//working
	protected Path pathToWorkingApexImgDir;//working
	protected Path pathToWorkingApexJarDir;//working
	protected Path pathToWorkingExtfsTool;//working
	protected String[] apexLocations;
	protected Map<String,Path> archToBootDir;
	protected String[] locations;//position 0 should always contain the framework dir
	protected List<String> archs;
	protected List<Integer> existingLocationsIndex;
	protected Map<String,T> archiveEntries;
	protected int defaultApi;
	protected ILogger logger;
	protected String cname;
	
	protected DexExtractor(String bootClassPath, Path pathToInputDir, Path pathToSystemImgZipFile, Path pathToSystemArchivesZipFile, 
			Path pathToWorkingDir, Path pathToAndroidInfoFile, String[] locations, String[] archs, String[] apexLocations, int defaultApi, ILogger logger){
		if(pathToInputDir == null || pathToSystemImgZipFile == null || pathToWorkingDir == null || pathToAndroidInfoFile == null ||
				locations == null || locations.length == 0 || logger == null || bootClassPath == null || archs == null || 
				archs.length == 0 || defaultApi < 0 || apexLocations == null || apexLocations.length == 0)
			throw new IllegalArgumentException("Error: All arguments must be non-null.");
		for(int i = 0; i < locations.length; i++){
			if(locations[i] == null)
				throw new IllegalArgumentException("Error: All locations entries must be non-null.");
		}
		this.pathToSystemArchivesZipFile = pathToSystemArchivesZipFile;
		this.pathToAndroidInfoFile = pathToAndroidInfoFile;
		this.pathToInputDir = pathToInputDir;
		this.pathToSystemImgZipFile = pathToSystemImgZipFile;
		this.pathToWorkingDir = pathToWorkingDir;
		this.pathToWorkingSystemDir = FileHelpers.getPath(pathToWorkingDir, "system");
		this.pathToWorkingDexDir = FileHelpers.getPath(pathToWorkingDir, "dex");
		this.pathToWorkingArchiveDir = FileHelpers.getPath(pathToWorkingDir, "archive");
		this.locations = locations;
		this.apexLocations = apexLocations;
		this.archs = Arrays.asList(archs);
		this.logger = logger;
		this.existingLocationsIndex = new ArrayList<>();
		this.pathToWorkingSystemLocationDirs = new Path[locations.length];
		this.pathToWorkingDexLocationDirs = new Path[locations.length];
		this.pathToWorkingArchiveLocationDirs = new Path[locations.length];
		for(int i = 0; i < this.locations.length; i++){
			this.pathToWorkingSystemLocationDirs[i] = FileHelpers.getPath(this.pathToWorkingSystemDir, this.locations[i]);
			this.pathToWorkingDexLocationDirs[i] = FileHelpers.getPath(this.pathToWorkingDexDir, this.locations[i]);
			this.pathToWorkingArchiveLocationDirs[i] = FileHelpers.getPath(this.pathToWorkingArchiveDir, this.locations[i]);
		}
		this.pathToWorkingApexDir = FileHelpers.getPath(this.pathToWorkingDir, "apex");
		this.pathToWorkingApexSystemDir = FileHelpers.getPath(pathToWorkingApexDir, "system");
		this.pathToWorkingApexImgDir = FileHelpers.getPath(pathToWorkingApexDir, "img");
		this.pathToWorkingApexJarDir = FileHelpers.getPath(pathToWorkingApexDir, "jar");
		this.pathToWorkingExtfsTool = FileHelpers.getPath(pathToWorkingApexDir, "ext2rd");
		this.pathToWorkingApexLocationDirs = new Path[apexLocations.length];
		for(int i = 0; i < this.apexLocations.length; i++) {
			this.pathToWorkingApexLocationDirs[i] = FileHelpers.getPath(this.pathToWorkingApexSystemDir, this.apexLocations[i]);
		}
		
		this.archiveEntries = new LinkedHashMap<>();
		this.bootClassPath = bootClassPath;
		this.archToBootDir = new LinkedHashMap<>();
		this.defaultApi = defaultApi;
		this.cname = this.getClass().getSimpleName();
		this.resolvedBootClassPath = resolveBootClassPath(null);
	}
	
	public boolean run() {
		logger.info("{}: Begin the pre-process procedures for extracting dex.",cname);
		if(!findExistingLocations() || !cleanupAndInitWorkingDir() || !extractLocationsFromSystemImg() || !handleApex() 
				|| !findArchName() || !customPreProcess()) {
			logger.fatal("{}: Failed to complete all pre-process procedures for extracting dex.",cname);
			removeWorkingDir();
			return false;
		}
		logger.info("{}: Successfully completed the pre-process procedures for extracting dex.",cname);
		logger.info("{}: Initializing the archive/dex information.",cname);
		if(!initArchiveAndDexInfo()) {
			StringBuilder sb = new StringBuilder();
			for(T archiveEntry : archiveEntries.values()) {
				sb.append(archiveEntry.toString());
			}
			logger.fatal("{}: Failed to complete the archive/dex initialization process. The following incomplete "
					+ "information was gathered:\n{}",cname,sb.toString());
			removeWorkingDir();
			return false;
		}
		logger.info("{}: Successfully initialized the archive/dex information.",cname);
		boolean ret = runInner();
		if(!ret) {
			removeWorkingDir();
			return false;
		}
		logger.info("{}: Begin the post-process procedured after extracting dex.",cname);
		if(!packageArchives() || !archiveArchives()) {
			logger.fatal("{}: Failed to complete all post-process procedures after extracting dex.",cname);
			removeWorkingDir();
			return false;
		}
		logger.info("{}: Successfully completed the post-process procedures after extracting dex.",cname);
		
		if(!removeWorkingDir())
			return false;
		
		return true;
	}
	
	public boolean archiveArchives(){
		try{
			//This overwrites the zip if it exists and creates it if it does not (i.e. no need to delete beforehand)
			List<Path> fullPaths = new ArrayList<>(2);
			List<Path> realtivePaths = new ArrayList<>(2);
			fullPaths.add(pathToSystemImgZipFile);
			fullPaths.add(pathToAndroidInfoFile);
			realtivePaths.add(pathToInputDir.relativize(pathToSystemImgZipFile));
			realtivePaths.add(pathToInputDir.relativize(pathToAndroidInfoFile));
			SimpleArchiver.createArchive(pathToSystemArchivesZipFile,pathToWorkingArchiveDir,fullPaths,realtivePaths);
		}catch(Throwable t){
			logger.fatal("{}: Failed to create the system archive zip file at '{}'.",t,cname,pathToSystemArchivesZipFile);
			return false;
		}
		return true;
	}
	
	public boolean removeWorkingDir(){
		if(FileHelpers.checkRWDirectoryExists(pathToWorkingDir)){
			try{
				FileHelpers.removeDirectory(pathToWorkingDir);
			}catch(Throwable t){
				logger.fatal("{}: Failed to completly remove the working directory at '{}'.",t,cname,pathToWorkingDir);
				return false;
			}
		}
		return true;
	}
	
	private boolean findExistingLocations(){
		try{
			Set<String> res = SimpleArchiver.findInArchive(pathToSystemImgZipFile, "/", null, "/"+locations[0], "d", null, null);
			if(res.isEmpty()){
				logger.fatal("{}: Failed to find the required directory '/{}' in the system image archive at path '{}'.",cname,locations[0],
						pathToSystemImgZipFile);
				return false;
			}
			existingLocationsIndex.add(0);
		}catch(Throwable t){
			logger.fatal("{}: Failed to find the required directory '/{}' in the system image archive at path '{}'.",t,cname,locations[0],
					pathToSystemImgZipFile);
			return false;
		}
		
		for(int i = 1; i < locations.length; i++){
			try{
				Set<String> res = SimpleArchiver.findInArchive(pathToSystemImgZipFile, "/", null, "/"+locations[i], "d", null, null);
				if(!res.isEmpty())
					existingLocationsIndex.add(i);
			}catch(Throwable t){
				logger.fatal("{}: An error occured when finding the directory '/{}' in the system image archive at path '{}'.",
						t,cname,locations[i],pathToSystemImgZipFile);
				return false;
			}
		}
		return true;
	}
	
	private boolean cleanupAndInitWorkingDir(){
		try{
			try{
				FileHelpers.processDirectory(pathToWorkingDir, true, true);
			}catch(Throwable t){
				logger.fatal("{}: Failed to create an empty R/W accessable working directory at '{}'.",t,cname,pathToWorkingDir);
				return false;
			}
			for(Path p : getWorkingSubDirs()){
				try{
					FileHelpers.processDirectory(p, true, false);
				}catch(Throwable t){
					logger.fatal("{}: Failed to create an empty R/W accessable working sub-directory '{}'.",t,cname,p);
					return false;
				}
			}
		}catch(Throwable t){
			logger.fatal("{}: An unexpected error occured during the cleanup and initilization of the working directory at '{}'.",t,cname,
					pathToWorkingDir);
			return false;
		}
		return true;
	}
	
	private List<Path> getWorkingSubDirs(){
		List<Path> ret = new ArrayList<>();
		ret.add(pathToWorkingSystemDir);
		ret.add(pathToWorkingDexDir);
		ret.add(pathToWorkingArchiveDir);
		for(Integer i : existingLocationsIndex){
			ret.add(pathToWorkingSystemLocationDirs[i]);
			ret.add(pathToWorkingDexLocationDirs[i]);
			ret.add(pathToWorkingArchiveLocationDirs[i]);
		}
		return ret;
	}
	
	private boolean extractLocationsFromSystemImg(){
		Set<Pair<Path,Path>> srcDestMap = new LinkedHashSet<>();
		for(Integer i : existingLocationsIndex){
			srcDestMap.add(new Pair<>(Paths.get(locations[i]),pathToWorkingSystemLocationDirs[i]));
		}
		try{
			SimpleArchiver.extractFromArchive(pathToSystemImgZipFile, srcDestMap);
		}catch(Throwable t){
			logger.fatal("{}: Failed to extract all existing locations '{}' from the system image archive at path '{}'.",t,cname,
					srcDestMap,pathToSystemImgZipFile);
			return false;
		}
		return true;
	}
	
	private boolean handleApex() {
		if(defaultApi >= 29) {
			try{
				try {
					FileHelpers.processDirectory(pathToWorkingApexDir, true, false);
				} catch(Throwable t) {
					logger.fatal("{}: Failed to create an empty R/W accessable working sub-directory at '{}'.",t,cname,pathToWorkingApexDir);
					return false;
				}
				try {
					FileHelpers.processDirectory(pathToWorkingApexSystemDir, true, false);
				} catch(Throwable t) {
					logger.fatal("{}: Failed to create an empty R/W accessable working sub-directory at '{}'.",t,cname,pathToWorkingApexSystemDir);
					return false;
				}
				try {
					FileHelpers.processDirectory(pathToWorkingApexImgDir, true, false);
				} catch(Throwable t) {
					logger.fatal("{}: Failed to create an empty R/W accessable working sub-directory at '{}'.",t,cname,pathToWorkingApexImgDir);
					return false;
				}
				try {
					FileHelpers.processDirectory(pathToWorkingApexJarDir, true, false);
				} catch(Throwable t) {
					logger.fatal("{}: Failed to create an empty R/W accessable working sub-directory at '{}'.",t,cname,pathToWorkingApexJarDir);
					return false;
				}
				Set<Pair<Path,Path>> srcDestMap = new LinkedHashSet<>();
				for(int i = 0; i < apexLocations.length; i++) {
					String location = apexLocations[i];
					Path p = pathToWorkingApexLocationDirs[i];
					try {
						FileHelpers.processDirectory(p, true, false);
					} catch(Throwable t) {
						logger.fatal("{}: Failed to create an empty R/W accessable working sub-directory '{}' in '{}'.",t,cname,p,pathToWorkingApexSystemDir);
						return false;
					}
					srcDestMap.add(new Pair<>(Paths.get(location),p));
				}
				try {
					SimpleArchiver.extractFromArchive(pathToSystemImgZipFile, srcDestMap);
				} catch(Throwable t) {
					logger.fatal("{}: Failed to extract all existing apex locations '{}' from the system image archive at path '{}'.",t,cname,
							srcDestMap,pathToSystemImgZipFile);
					return false;
				}
				Set<Path> apexFiles = null;
				try {
					apexFiles = SortingMethods.sortSet(FileHelpers.find(pathToWorkingApexSystemDir, "*.apex", null, "f", null, null));
				} catch(Throwable t) {
					logger.fatal("{}: Failed to perform a find all '*.apex' files in the path '{}'.",t,cname,pathToWorkingApexSystemDir);
					return false;
				}
				
				Set<Path> imgFiles = new HashSet<>();
				for(Path apexFile : apexFiles) {
					Set<String> apexPayload = null;
					try {
						apexPayload = SimpleArchiver.findInArchive(apexFile, "/", "apex_payload.img", null, "f", null, null);
					} catch(Throwable t) {
						logger.fatal("{}: Failed to perform a find for 'apex_payload.img' file in the archive '{}'.",t,cname,apexFile);
						return false;
					}
					if(apexPayload != null && !apexPayload.isEmpty()) {
						String newFileName = com.google.common.io.Files.getNameWithoutExtension(apexFile.toString()) + ".img";
						Path imgFile = FileHelpers.getPath(pathToWorkingApexImgDir, newFileName);
						srcDestMap = Collections.singleton(new Pair<>(Paths.get(apexPayload.iterator().next()),imgFile));
						try {
							SimpleArchiver.extractFromArchive(apexFile, srcDestMap);
						} catch(Throwable t) {
							logger.fatal("{}: Failed to extract the 'apex_payload.img' from '{}'.",t,cname,apexFile);
							return false;
						}
						imgFiles.add(imgFile);
					}
				}
				
				try {
					try(InputStream in = getClass().getResourceAsStream("/extfstools/bin/ext2rd")) {
						try(OutputStream out = Files.newOutputStream(pathToWorkingExtfsTool)) {
							ByteStreams.copy(in, out);
						}
					}
					pathToWorkingExtfsTool.toFile().setExecutable(true);
				} catch(Throwable t) {
					logger.fatal("{}: Failed to extract the ext2rd executable to the working directory.",t,cname);
					return false;
				}
				
				LinuxOrWSLCommandBuilder cmdbuilder;
				try {
					cmdbuilder = new LinuxOrWSLCommandBuilder();
				} catch(Throwable t) {
					logger.fatal("{}: Failed to initilize command builder.",t,cname);
					return false;
				}
				for(Path imgFile : imgFiles) {
					List<String> cmds;
					try {
						// wsl "run.sh" -i "path.vdex" -o "path" or "run.sh" -i "path.vdex" -o "path"
						cmdbuilder.addPath(pathToWorkingExtfsTool);
						cmdbuilder.addPath(imgFile);
						cmdbuilder.add("javalib/:" + cmdbuilder.transformPath(pathToWorkingApexJarDir));
						cmds = cmdbuilder.getCommand();
					} catch(Throwable t) {
						logger.fatal("{}: Failed to build the command to extrace jars from the img files using ext2rd.",t,cname);
						return false;
					}
					
					//Error messages are written to both stderr and stdout. The return value is always 0 except when requesting the
					//help dialog. If everything functions correctly then no output occurs.
					try {
						ProcessBuilder pb = new ProcessBuilder(cmds);
						pb.directory(pathToWorkingApexDir.toFile());
						pb.redirectErrorStream(true);
						Process p = pb.start();
						BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
						StringBuilder sb = new StringBuilder();
						String line;
						while((line = br.readLine()) != null) {
							sb.append("\n    ").append(line.trim());
						}
						String msg = sb.toString();
						int r = p.waitFor();
						if(msg.trim().equals("exportdir: path not found")) {
							//Not an error if jarlib folder is not found in some apex img files
							logger.info("{}: No jarlib folder in img file '{}'",cname,imgFile);
						} else if(r != 0 || !msg.trim().isEmpty()) {
							logger.fatal("{}: ext2rd did not terminate normally when extracting the jar files from img file '{}'.\n  Command Output:{}",
									cname,imgFile,msg);
							return false;
						}
					} catch(Throwable t) {
						logger.fatal("{}: Unexpected error. Failed to parse the img file '{}'.",t,cname,imgFile);
						return false;
					}
				}
				
				Set<Path> jarFiles = null;
				try {
					jarFiles = SortingMethods.sortSet(FileHelpers.find(pathToWorkingApexJarDir, "*.jar", null, "f", null, null));
				} catch(Throwable t) {
					logger.fatal("{}: Failed to perform a find all '*.jar' files in the path '{}'.",t,cname,pathToWorkingApexJarDir);
					return false;
				}
				
				for(Path jarFile : jarFiles) {
					Path destJarFile = FileHelpers.getPath(pathToWorkingSystemLocationDirs[0],jarFile.getFileName());
					try {
						Files.copy(jarFile, destJarFile, StandardCopyOption.REPLACE_EXISTING);
					} catch(Throwable t) {
						logger.fatal("{}: Failed to copy jar file '{}' to '{}'.",t,cname,jarFile,destJarFile);
						return false;
					}
				}
				
			} catch(Throwable t) {
				logger.fatal("{}: An unexpected error occured when processing apex files.",t,cname);
				return false;
			}
		}
		return true;
	}
	
	public boolean findArchName(){
		String[] bcp = bootClassPath.split(":");
		Map<String,Set<Path>> archToPaths = new HashMap<>();
		
		//Grab all possible paths to the boot class files and determine there arch dir
		for(String bcpEntry : bcp){
			Set<Path> paths;
			try{
				paths = FileHelpers.find(pathToWorkingSystemLocationDirs[0],bcpEntry,null,"f",null,null);
			}catch(Throwable t){
				logger.fatal("{}: An error occured while looking for boot class path file '{}' in '{}'.",
						t,cname,bcpEntry,pathToWorkingSystemLocationDirs[0]);
				return false;
			}
			//Skip over empty search results as we are looking for all possible paths
			//If some searches return empty we might still be able to make this work
			//Otherwise, populate our arch to boot class path containing arch directories map
			if(paths != null && !paths.isEmpty()) {
				for(Path p : paths) {
					try {
						Path parent = p.getParent();
						if(parent != null) {
							Path archNamePath = parent.getFileName();
							if(archNamePath != null) {
								String archName = archNamePath.toString();
								Set<Path> temp = archToPaths.get(archName);
								if(temp == null) {
									temp = new HashSet<>();
									archToPaths.put(archName, temp);
								}
								temp.add(parent);
							} else {
								logger.fatal("{}: Failed to find the directory of '{}' from path '{}'. The Path of the parents name is empty???",
										cname,bcpEntry,p);
								return false;
							}
						} else {
							logger.fatal("{}: Failed to find the directory of '{}' from path '{}'. Is it in the root directory???",
									cname,bcpEntry,p);
							return false;
						}
					} catch(Throwable t) {
						logger.fatal("{}: An unexpected error occured when trying to find the directory of '{}' from path '{}'.",cname,bcpEntry,p);
						return false;
					}
				}
			}
		}
		
		if(archToPaths.isEmpty()) {
			logger.fatal("{}: Failed to find any of the boot class path files or their containing directories in '{}' for boot class path '{}'.",
					cname,pathToWorkingSystemLocationDirs[0],bootClassPath);
			return false;
		}
		
		//If the above produces multiple Path objects for a single arch, reduce them to a single Path object
		//That is test all Path objects for an arch to see if they point to the same element and if they do keep the first Path object
		//Otherwise, we have a conflict which we can't handle so we error and report it
		Map<String,Path> archToSinglePath = new HashMap<>();
		for(String arch : archToPaths.keySet()) {
			Path cur = null;
			boolean first = true;
			for(Path p : archToPaths.get(arch)) {
				if(first) {
					first = false;
					cur = p;
				} else {
					try {
						if(!Files.isSameFile(cur, p)) {
							logger.fatal("{}: Conflicting paths for arch '{}'. The paths '{}' and '{}' do not point to the same element.",
									cname,arch,cur,p);
							return false;
						}
					} catch(Throwable t) {
						logger.fatal("{}: An unexpected error occured when determining if path '{}' and '{}' point to the same element.",
								cname,cur,p);
						return false;
					}
				}
			}
			archToSinglePath.put(arch, cur);
		}
		archToSinglePath = SortingMethods.sortMapKey(archToSinglePath, SortingMethods.sComp);
		
		//Find any previously unknown archs and add them to our arch list in front (alphabetically if there is more than one)
		List<String> newArchs = new ArrayList<>();
		for(String arch : archToSinglePath.keySet()) {
			if(!archs.contains(arch))
				newArchs.add(arch);
		}
		newArchs.addAll(archs);
		archs = newArchs;
		
		//Add the archs and their path to our map in the same order as the archs list
		for(String arch : archs) {
			Path p = archToSinglePath.get(arch);
			if(p != null)
				archToBootDir.put(arch, p);
		}
		
		return true;
	}
	
	protected boolean initArchiveAndDexInfo(){
		boolean success = true;
		try{
			//Split the passed in boot class path so it can be processed later if it contains oat files
			for(String s : bootClassPath.split(":")) {
				resolvedBootClassPath.add(s.trim());
			}
			//discover the boot class path dex files which are inside oat files
			if(!parseOatFiles()){
				logger.fatal("{}: Failed to parse all the oat files and extract all dex and archive information"
						+ " for the core android libraries (i.e. those on the bootclasspath).",cname);
				success = false;
			}
			for(Integer i : existingLocationsIndex){
				boolean ret;
				if(i == 0){//discover the remaining framework dex files in the odex/oat files or just their archives
					ret = discoverDexFiles(locations[i],pathToWorkingSystemLocationDirs[i],pathToWorkingDexLocationDirs[i],
							pathToWorkingArchiveDir);
				}else{//discover the app/priv-app related dex files in odex/oat file or in their apk's
					ret = discoverAppDexFiles(locations[i],pathToWorkingSystemLocationDirs[i],pathToWorkingDexLocationDirs[i],
							pathToWorkingArchiveDir);
				}
				if(!ret){
					logger.fatal("{}: Failed to discover all existing dex files and archives and parse information"
							+ " about dex and archive files that need extracting for '/system/{}'.",cname,locations[i]);
					success = false;
				}
			}
		}catch(Throwable t){
			logger.fatal("{}: An unexpected error occured during the archive and dex info initlization "
					+ "procedure for the system.",t,cname);
			success = false;
		}
		return success;
	}
	
	//Loop through the directories in the app/priv-app folders looking for dex files
	protected boolean discoverAppDexFiles(String location, Path systemDir, Path workingDir, Path outputDir) {
		boolean success = true;
		try(DirectoryStream<Path> dsApp = Files.newDirectoryStream(systemDir)){
			Set<Path> paths = SortingMethods.sortSet(ImmutableSet.<Path>copyOf(dsApp));//Make sure this is ordered
			for(Path p : paths){
				if(FileHelpers.checkRWDirectoryExists(p)){
					if(!discoverDexFiles(location,p,workingDir,outputDir)){
						logger.fatal("{}: An exception occured when discovering and parsing all oat/odex/dex "
								+ "files in the directory '{}' for location '{}'.",cname,p,location);
						success = false;
					}
				}
			}
		}catch(Throwable t){
			logger.fatal("{}: An exception occured when traversing the system directory '{}' for location '{}'."
					,t,cname,systemDir,location);
			success = false;
		}
		return success;
	}
	
	//discover dex files in archive dir either existing or in oat/odex and copy any existing archives to the output dir
	protected boolean discoverDexFiles(String location, Path archiveDir, Path workingDir, Path outputDir) {
		boolean success = true;
		//Locate the odex files in the arch directory and process them if any
		if(FileHelpers.checkRWDirectoryExists(archiveDir)){
			if(!parseOdexFiles(location, archiveDir, workingDir, outputDir)){
				logger.fatal("{}: The odex file parsing procedure failed for location '{}' with "
						+ "archive directory.",cname,location,archiveDir);
				success = false;
			}
		}
		try{
			//Order the paths
			Set<Path> archiveFilePaths = SortingMethods.sortSet(FileHelpers.find(archiveDir, "{*.jar,*.apk}", null, "f", null, null));
			for(Path archiveFilePath : archiveFilePaths){
				try{
					String archiveNameExt = archiveFilePath.getFileName().toString();
					
					//Grab the existing archive entry
					String key = makeArchiveEntriesKey(location,archiveNameExt);
					T archiveEntry = archiveEntries.get(key);
					if(archiveEntry == null) {
						//If the existing archive entry does not exist then add it
						//May or may not end up with dex entries depending on below process but add for completeness
						String archiveName = com.google.common.io.Files.getNameWithoutExtension(archiveNameExt);
						String archiveExtension = com.google.common.io.Files.getFileExtension(archiveNameExt);
						archiveEntry = getNewArchiveEntry(archiveName,archiveExtension,location,FileHelpers.getPath(workingDir, archiveName),
								outputDir);
						archiveEntries.put(key, archiveEntry);
					}
					
					//Look for dex files in the archive or throw an exception if something goes wrong
					Set<String> dexFilesInArchive = null;
					try{
						dexFilesInArchive = SimpleArchiver.findInArchive(archiveFilePath, "/", "*.dex", null, "f", null, null);
					}catch(Throwable t){
						throw new Exception("Error: Failed to perform a find for '.dex' files in the archive '" + archiveFilePath + "'.",t);
					}
					
					if(!dexFilesInArchive.isEmpty()){
						for(String dexFile : dexFilesInArchive){
							//Get File Name of dex (we are assuming all dex is in the root of the archive file)
							dexFile = dexFile.trim();
							dexFile = com.google.common.io.Files.getNameWithoutExtension(dexFile) + "." +
									com.google.common.io.Files.getFileExtension(dexFile);
							//Mark existing dex entry in the archive entry as skip as we have the dex file already
							//Or if the dex entry does not exist add it and mark it as skip for completeness
							archiveEntry.markDexEntryAsSkip(dexFile);
						}
					}
					
					//Error if there are conflicting archives (i.e. archives with the same name)
					if(Files.exists(archiveEntry.getPathToArchive())) {
						throw new Exception("Error: Trying to copy '" + archiveFilePath + "' to '" + archiveEntry.getPathToArchive() 
							+ "' but the path already contains a existing file system element. There may be one or more archives with"
							+ " the same name for location '" + location + "' and directory '" + archiveDir + "'.");
					}
					
					//Copy the existing archive to the output dir whether it contains dex files or not
					Files.copy(archiveFilePath, archiveEntry.getPathToArchive(), StandardCopyOption.REPLACE_EXISTING);
				}catch(Throwable t){
					logger.fatal("{}: An error occured when searching for and recording information about "
							+ "dex in the existing archive '{}' for location '{}'.",t,cname,archiveFilePath,location);
					success = false;
				}
			}
		}catch(Throwable t){
			logger.fatal("{}: An error occured when searching for archives with existing dex files in "
					+ "directory '{}' for location '{}'.",t,cname,archiveDir,location);
			success = false;
		}
		return success;
	}
	
	public Set<T> getArchiveEntries(){
		return new LinkedHashSet<T>(archiveEntries.values());
	}
	
	protected List<String> resolveBootClassPath(Set<Path> paths) {
		List<String> ret = new ArrayList<>();
		for(String s : bootClassPath.split(":")) {
			ret.add(s.trim());
		}
		if(paths != null && !paths.isEmpty()) {
			try {
				ret = resolveBootClassPathFromOatFiles(paths, ret);
			} catch(Throwable t) {
				throw new RuntimeException(t);
			}
		}
		return ret;
	}
	
	protected boolean parseOatFiles() {
		boolean success = true;
		String location = locations[0];
		Path rootDir = pathToWorkingSystemLocationDirs[0];
		Path workingDir = pathToWorkingDexLocationDirs[0];
		Path outputDir = pathToWorkingArchiveDir;
		try {
			Set<Path> oatFilePathsUnsorted = FileHelpers.find(rootDir, "*.oat", null, "f", null, null);
			resolvedBootClassPath = resolveBootClassPath(oatFilePathsUnsorted);
			Map<String,Set<Path>> archToSortedOatPaths = sortOatFilePaths(oatFilePathsUnsorted);
			if(archToSortedOatPaths == null) {
				logger.fatal("{}: Something went wrong when sorting the oat files in '{}'.",cname,rootDir);
				success = false;
			} else {
				String arch = archToBootDir.keySet().iterator().next();
				Path bootDirForArch = archToBootDir.get(arch);
				int size = 0;
				String members = null;
				boolean first = true;
				boolean sameElements = true;
				for(Set<Path> p : archToSortedOatPaths.values()) {
					size += p.size();
					if(first) {
						first = false;
						List<String> groupMembers = new ArrayList<>();
						for(Path l : p)
							groupMembers.add(l.getFileName().toString());
						Collections.sort(groupMembers);
						members = groupMembers.toString();
					} else {
						List<String> groupMembers = new ArrayList<>();
						for(Path l : p)
							groupMembers.add(l.getFileName().toString());
						Collections.sort(groupMembers);
						if(!groupMembers.toString().equals(members))
							sameElements = false;
					}
				}
				if(size != oatFilePathsUnsorted.size()) {
					logger.fatal("{}: Something went wrong when sorting the oat files in '{}'. Did not return with the same number of elements.",
							cname,rootDir);
					success = false;
				} else if(archToSortedOatPaths.containsKey("__unknown__")) {
					StringBuilder sb = new StringBuilder();
					for(Path p : archToSortedOatPaths.get("__unknown__")) {
						sb.append("\t").append(p.toString()).append("\n");
					}
					logger.fatal("{}: There are oat files not located in an arch directory for '{}'???\n{}",cname,rootDir,sb.toString());
					success = false;
				} else if(!sameElements) {
					StringBuilder sb = new StringBuilder();
					for(String archName : archToSortedOatPaths.keySet()) {
						sb.append("\t Arch: ").append(archName).append("\n");
						for(Path p : archToSortedOatPaths.get(archName)) {
							sb.append("\t\t").append(p.toString()).append("\n");
						}
					}
					logger.fatal("{}: Not all arch groups have the same oat files for '{}'???\n{}",cname,rootDir,sb.toString());
					success = false;
					
					
				} else if(arch == null || bootDirForArch == null) {
					logger.fatal("{}: Unable to determine the boot directory for '{}'.",cname,rootDir);
					success = false;
				} else if(!archToSortedOatPaths.containsKey(arch)) {
					logger.fatal("{}: The main arch '{}' does not have a group of oat files for '{}'???\n{}",cname,arch,rootDir);
					success = false;
				} else {
					//Because in Android 9 google decided to put the vdex files for the boot.oat files in the a different directory from 
					//the boot.oat files unlike all other oat files, we need to copy the vdex files into the same directory as the boot.oat
					//files so the tools can find them.
					Set<Path> vdexFilePaths = null;
					if(defaultApi >= 28) {
						for(Set<Path> sortedOatPaths : archToSortedOatPaths.values()) {
							for(Path oatFilePath : sortedOatPaths) {
								String vdexFile = com.google.common.io.Files.getNameWithoutExtension(oatFilePath.getFileName().toString()) + ".vdex";
								Path vdexFilePath = FileHelpers.getPath(oatFilePath.getParent(), vdexFile);
								if(!FileHelpers.checkRWFileExists(vdexFilePath)) {
									//vdex file does not exist at the default path, then we need to locate it
									Path foundVdexFile = null;
									if(vdexFilePaths == null)
										vdexFilePaths = FileHelpers.find(rootDir, "*.vdex", null, "f", null, null);
									for(Path existingVdexFilePath : vdexFilePaths) {
										if(existingVdexFilePath.getFileName().toString().equals(vdexFile)) {
											if(foundVdexFile != null) {
												logger.fatal("{}: Found mutiple vdex files for oat file '{}' ['{}', '{}']",cname,
														oatFilePath,foundVdexFile,existingVdexFilePath);
												success = false;
											}
											foundVdexFile = existingVdexFilePath;
										}
									}
									if(foundVdexFile == null) {
										logger.fatal("{}: Unable to find vdex file for oat file '{}'",cname,oatFilePath);
										success = false;
									}
									
									try {
										Files.copy(foundVdexFile, vdexFilePath, StandardCopyOption.REPLACE_EXISTING);
									} catch(Throwable t) {
										logger.fatal("{}: Failed to copy vdex file '{}' to '{}'",t,cname,foundVdexFile,vdexFilePath);
										success = false;
									}
								}
							}
						}
					}
					
					if(success) {
						for(Path oatFilePath : archToSortedOatPaths.get(arch)) {
							if(!parseOatFile(location,oatFilePath,workingDir,outputDir, arch, bootDirForArch))
								success = false;
						}
					}
				}
			}
		} catch(Throwable t) {
			logger.fatal("{}: Unexpected error when parsing the oat files in '{}'.",t,cname,rootDir);
			success = false;
		}
		return success;
	}
	
	/** Groups the oat files by archs. Then sorts the oat files in each arch group according to the boot class path. Oat files not dependent
	 * on any of the files in the boot class path of an arch are added to the end of the list of paths in alphabetical order. Oat files not
	 * located in any of the arch root directories are added to a group "__unknown__" in alphabetical order.
	 */
	protected Map<String,Set<Path>> sortOatFilePaths(Set<Path> oatFilesIn) {
		try {
		
			Set<Path> oatFiles = new LinkedHashSet<>(oatFilesIn);
			Map<String,Set<Path>> ret = new LinkedHashMap<>();
			for(String arch : archToBootDir.keySet()) {
				Path archPath = archToBootDir.get(arch);
				Set<Path> paths = new LinkedHashSet<>();
				
				for(Iterator<Path> it = oatFiles.iterator(); it.hasNext();) {
					Path p = it.next();
					Path parent = p.getParent();
					if(Files.isSameFile(parent, archPath)) {
						paths.add(p);
						it.remove();
					}
				}
				
				if(!paths.isEmpty()) {
					paths = sortOatPathsByDependency(paths);
					ret.put(arch, paths);
				}
			}
			
			//At the end they should all be contained within the ret map, if oatFiles still contains entries
			//then this means some oat files do not belong in any arch directory which is likely some error
			//we leave the actual decision on what to do with this up to a higher method
			//All unknown archs are stored under the key "__unknown__" at the end of the map in alphabetical order
			if(!oatFiles.isEmpty())
				ret.put("__unknown__", SortingMethods.sortSet(oatFiles));
			
			return ret;
		
		} catch(Throwable t) {
			logger.fatal("{}: Unexpected error occured when ordering the oat files.",t,cname);
			return null;
		}
	}
	
	protected final Set<Path> sortOatPathsByDependency(Set<Path> paths) throws Exception {
		Set<Path> newPaths = new LinkedHashSet<>();
		//Ensure that oat files passed in as a class path are listed first
		String[] bcp = bootClassPath.split(":");
		for(String bcpEntry : bcp) {
			for(Iterator<Path> it = paths.iterator(); it.hasNext();) {
				Path p = it.next();
				if(p.getFileName().toString().equals(bcpEntry)) {
					newPaths.add(p);
					it.remove();
				}
			}
		}
		
		//Resolve the order of the rest of the oat files from the already resolved boot class path
		for(String entry : resolvedBootClassPath) {
			String name = com.google.common.io.Files.getNameWithoutExtension(entry);
			String name2 = "boot-" + name; //because the boot oat files start with boot-jarname
			for(Iterator<Path> it = paths.iterator(); it.hasNext();) {
				Path p = it.next();
				String existingOatName = com.google.common.io.Files.getNameWithoutExtension(p.toString());
				if(name.equals(existingOatName) || name2.equals(existingOatName)) {
					newPaths.add(p);
					it.remove();
				}
			}
		}
		
		//If there are any leftover oat files then just add them to the end
		if(!paths.isEmpty())
			newPaths.addAll(SortingMethods.sortSet(paths));
		return newPaths;
	}
	
	//handles all exceptions internally
	protected boolean parseOdexFiles(String location, Path rootDir, Path workingDir, Path outputDir) {
		boolean success = true;
		try{
			Set<Path> odexFilePathsUnsorted = FileHelpers.find(rootDir, "*.odex", null, "f", null, null);
			Map<String,Map<String,Path>> sortedOdexFiles = sortOdexFilePaths(odexFilePathsUnsorted);
			if(sortedOdexFiles == null) {
				logger.fatal("{}: Something went wrong when sorting the odex files in '{}'.",cname,rootDir);
				success = false;
			} else {
				int size = 0;
				for(Map<String,Path> maps : sortedOdexFiles.values()) {
					size += maps.size();
				}
				if(size != odexFilePathsUnsorted.size()) {
					logger.fatal("{}: Something went wrong when sorting the odex files in '{}'. Did not return with the same number of elements.",
							cname,rootDir);
					success = false;
				} else {
					//Note we already check to make sure there are no unknown arch's in the sort method and throw and error there
					//So at this point everything should be find
					for(Map<String,Path> archToPaths : sortedOdexFiles.values()) {
						for(String arch : archs) {
							Path bootDirForArch = archToBootDir.get(arch);
							Path p = archToPaths.get(arch);
							if(p != null) {
								if(bootDirForArch == null) {
									logger.fatal("{}: Unable to determine the boot directory for '{}'.",cname,p);
									success = false;
								} else if(!parseOatFile(location,p,workingDir,outputDir, arch, bootDirForArch)) {
									success = false;
								}
								break;
							}
						}
					}
				}
			}
			
		}catch(Throwable t){
			logger.fatal("{}: Failed to parse the oat/odex files in '{}'.",t,cname,rootDir);
			success = false;
		}
		return success;
	}
	
	protected Map<String,Map<String,Path>> sortOdexFilePaths(Set<Path> odexFilesIn) {
		try {
			Set<Path> odexFiles = SortingMethods.sortSet(odexFilesIn);
			Map<String,Map<String,Path>> ret = new LinkedHashMap<>();
			//Group the paths to odex files first by name, then by arch
			for(Path p : odexFiles) {
				String fileName = p.getFileName().toString();
				String archName = p.getParent().getFileName().toString();
				Map<String,Path> archToPaths = ret.get(fileName);
				
				//If this is an unknown arch at this point, it means we do not have a boot.oat file for this arch
				//and thus the odex file cannot be turned back to dex, this will cause a problem later to we report it here
				if(!archs.contains(archName)) {
					logger.fatal("{}: Arch '{}' is not a recongized arch name for odex file '{}'.",cname,archName,p);
					return null;
				}
				
				if(archToPaths == null) {
					archToPaths = new LinkedHashMap<>();
					ret.put(fileName, archToPaths);
				}
				Path j = archToPaths.get(archName);
				if(j != null) {
					//Allow multiple paths to the same file, but different files with the same name and arch is a conflict
					//we cannot handle so we error
					if(!Files.isSameFile(p, j)) {
						logger.fatal("{}: Paths '{}' and '{}' do not point to the same file for name '{}' and arch '{}'.",
								cname,p,j,fileName,archName);
						return null;
					}
					//They are the same file so keep the first path discovered
				} else {
					archToPaths.put(archName, p);
				}
			}
			
			//Sort the returning data alphabetically by file name and then by arch according to our know arch list order
			for(String fileName : ret.keySet()) {
				Map<String,Path> map = ret.get(fileName);
				Map<String,Path> newMap = new LinkedHashMap<>();
				for(String arch : archs) {
					Path p = map.get(arch);
					if(p != null)
						newMap.put(arch, p);
				}
				ret.put(fileName, newMap);
			}
			ret = SortingMethods.sortMapKey(ret, SortingMethods.sComp);
			
			return ret;
		} catch(Throwable t) {
			logger.fatal("{}: Unexpected error occured when ordering the odex files.",t,cname);
			return null;
		}
	}
	
	protected boolean packageArchives(){
		boolean success = true;
		logger.info("{}: Packaging all dex files into their archives.",cname);
		for(T archiveEntry : archiveEntries.values()){
			if(!archiveEntry.skip()){
				logger.info("{}: Adding dex files back into the archive '{}.{}' of location '{}'.",cname,
						archiveEntry.getName(),archiveEntry.getExtension(),archiveEntry.getLocation());
				Set<Pair<Path,Path>> fromToMap = new LinkedHashSet<>();
				for(DexEntry dexEntry : archiveEntry.getDexFiles()){
					if(!dexEntry.skip()){
						if(!dexEntry.isErrorState() && dexEntry.getState().equals(requiredStateBeforePostProcessing())){
							if(FileHelpers.checkRWFileExists(dexEntry.getPathToDexFile())){
								fromToMap.add(new Pair<Path,Path>(dexEntry.getPathToDexFile(),
										Paths.get(dexEntry.getDexFileName())));
							}else{
								logger.fatal("{}: The dex file '{}' for archive '{}.{}' of location '{}' "
										+ "could not be found at path '{}'. Something went wrong.",cname,dexEntry.getDexFileName(),
										archiveEntry.getName(),archiveEntry.getExtension(),archiveEntry.getLocation(),
										dexEntry.getPathToDexFile());
								success = false;
								dexEntry.setState(State.REPKGERR);
							}
						}else{
							logger.fatal("{}: The dex entry is in state '{}' indicating an error occured. "
									+ "The dex file '{}' for archive '{}.{}' of location '{}' will not be added to the archive.",
									cname,dexEntry.getState(),dexEntry.getDexFileName(),archiveEntry.getName(),
									archiveEntry.getExtension(),archiveEntry.getLocation());
							success = false;
							if(!dexEntry.isErrorState())
								dexEntry.setState(State.REPKGERR);
						}
					}else{
						logger.info("{}: The dex file '{}' already exists in archive '{}.{}' of location '{}'.",
								cname,dexEntry.getDexFileName(),archiveEntry.getName(),archiveEntry.getExtension(),
								archiveEntry.getLocation());
					}
				}
				if(!fromToMap.isEmpty()){
					if(addFilesToArchive(archiveEntry,fromToMap)){
						for(DexEntry dexEntry : archiveEntry.getDexFiles()){
							if(!dexEntry.isErrorState() && !dexEntry.skip())
								dexEntry.setState(State.REPKGSUCC);
						}
					}else{
						for(DexEntry dexEntry : archiveEntry.getDexFiles()){
							if(!dexEntry.isErrorState() && !dexEntry.skip())
								dexEntry.setState(State.REPKGERR);
						}
						success = false;
					}
					StringBuilder sb = new StringBuilder();
					for(DexEntry dexEntry : archiveEntry.getDexFiles()){
						sb.append("  ").append(dexEntry.getDexFileName()).append(" : ").append(dexEntry.getState().toString());
						sb.append("\n");
					}
					logger.info("{}: The state of archive '{}.{}' of location '{}' after attempting the "
							+ "adding procedure:\n{}",cname,archiveEntry.getName(),archiveEntry.getExtension(),
							archiveEntry.getLocation(),sb.toString());
				}else{
					logger.fatal("{}: The archive entry '{}.{}' of location '{}' has no writable dex files"
							+ " due to one or more errors. Nothing can be done.",cname,archiveEntry.getName(),
							archiveEntry.getExtension(),archiveEntry.getLocation());
					for(DexEntry dexEntry : archiveEntry.getDexFiles()){
						if(!dexEntry.isErrorState() && !dexEntry.skip())
							dexEntry.setState(State.REPKGERR);
					}
					success = false;
				}
			}else{
				logger.info("{}: The archive '{}.{}' of location '{}' {}. There is nothing to be done.",cname,
						archiveEntry.getName(),archiveEntry.getExtension(),archiveEntry.getLocation(),
						archiveEntry.getDexFiles().isEmpty() ? "is empty" : "already contains its dex files");
			}
		}
		
		if(success){
			logger.info("{}: Successfully packaged all dex files into their archives.",cname);
		}else{
			logger.fatal("{}: Failed to package all dex files into their archives.",cname);
		}
		
		return success;
	}
	
	private boolean addFilesToArchive(T archiveEntry, Set<Pair<Path,Path>> fromToMap){
		Path archive = archiveEntry.getPathToArchive();
		if(!FileHelpers.checkRWFileExists(archive)) {
			try {
				MakeEmptyJar.writeEmptyJar(archive);
			} catch(Throwable t) {
				logger.fatal("{}: Failed to successfully create empty archive '{}.{}' at path '{}'.",t,cname,
						archiveEntry.getName(),archiveEntry.getExtension(),archive);
				return false;
			}
		}
		try{
			SimpleArchiver.addToArchive(archive, fromToMap, !FileHelpers.checkRWFileExists(archive));
		}catch(Throwable t){
			logger.fatal("{}: Failed to successfully add all dex files to the archive '{}.{}' at path '{}'.",t,cname,
					archiveEntry.getName(),archiveEntry.getExtension(),archive);
			return false;
		}
		return true;
	}
	
	protected static final String makeArchiveEntriesKey(String location, String archiveName, String archiveExtension){
		return makeArchiveEntriesKey(location,archiveName + "." + archiveExtension);
	}
	
	protected static final String makeArchiveEntriesKey(String location, String archiveNameExt){
		return location + "/" + archiveNameExt;
	}
	
	protected static String[] parseOatEntryString(String oatEntry, int oatVersion) throws Exception {
		String splitChar;
		if(oatVersion >= 130) {
			splitChar = "!";
		} else {
			splitChar = ":";
		}
		String[] ret = new String[3];
		String[] parts = oatEntry.split(splitChar);
		ret[0] = null;//archiveName
		ret[1] = null;//archiveExtension
		ret[2] = null;//dexName
		if(parts.length == 2){
			ret[2] = parts[1].trim();
			if(ret[2].length() <= 4)
				throw new Exception("Error: The dex file name should be longer than 4 characters for oat entry '" + oatEntry + "'.");
			if(!ret[2].endsWith(".dex"))
				throw new Exception("Error: The dex file name '" + ret[2] + "' does not end with '.dex' for oat entry '" + oatEntry + "'.");
		}
		if(parts.length == 1 || parts.length == 2){
			parts[0] = parts[0].trim();
			ret[0] = com.google.common.io.Files.getNameWithoutExtension(parts[0]);
			ret[1] = com.google.common.io.Files.getFileExtension(parts[0]);
			if(ret[0] == null || ret[0].isEmpty())
				throw new Exception("Error: Failed to parse the archive name from '" + parts[0] + "' for oat entry '" + oatEntry + "'.");
			if(ret[1] == null)
				ret[1] = "";
		}else{
			throw new Exception("Error: Unexpected oat entry format '" + oatEntry + "'.");
		}
		return ret;
	}
	
	protected static String makeOatEntryString(String firstEntry, String dexName, int oatVersion) {
		String splitChar;
		if(oatVersion >= 130) {
			splitChar = "!";
		} else {
			splitChar = ":";
		}
		if(dexName == null)
			return firstEntry;
		return firstEntry + splitChar + dexName;
	}
	
	public List<String> getResolvedBootClassPath() {
		return resolvedBootClassPath;
	}
	
	protected boolean customPreProcess() { return true; } 
	
	protected abstract boolean runInner();
	protected abstract boolean parseOatFile(String location, Path oatFilePath, Path workingDir, Path outputDir, String arch, Path bootDirForArch);
	protected abstract T getNewArchiveEntry(String name, String extension, String location, Path workingDir, Path rootPathToArchive);
	protected abstract State requiredStateBeforePostProcessing();
	protected abstract List<String> resolveBootClassPathFromOatFiles(Set<Path> paths, List<String> bcp) throws Exception;
	
}
