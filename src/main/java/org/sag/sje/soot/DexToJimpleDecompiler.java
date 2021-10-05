package org.sag.sje.soot;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.MultiDexContainer;
import org.sag.acminer.sootinit.BasicSootLoader;
import org.sag.common.concurrent.CountingThreadExecutor;
import org.sag.common.io.FileHelpers;
import org.sag.common.io.PrintStreamUnixEOL;
import org.sag.common.io.archiver.SimpleArchiver;
import org.sag.common.logging.ILogger;
import org.sag.common.tools.SortingMethods;
import org.sag.main.sootinit.SootLoader;
import org.sag.sje.ArchiveEntry;
import org.sag.sje.DexEntry;
import org.sag.sje.sootinit.AllAppsDexSootLoader;
import org.sag.sje.sootinit.AppDexSootLoader;
import org.sag.sje.sootinit.DexSootLoader;
import org.sag.sje.sootinit.FrameworkDexSootLoader;

import soot.Scene;
import soot.SootClass;
import soot.dexpler.Util;

public class DexToJimpleDecompiler {
	
	private Set<? extends ArchiveEntry<? extends DexEntry>> archiveEntries;
	private Path pathToWorkingDir;//working
	private Path pathToSystemArchivesZipFile;//input
	private Path pathToAndroidInfoFile;//input
	private Path pathToJimpleJar;//output - deleted before creating the new version if exists
	private Path pathToJimpleJarFrameworkOnly;//output - overwritten when jimple jar is copied to this path
	private Path pathToJimpleJarConflicts;//output - deleted before creating new version if exists
	private Path pathToClassesJar;//output - delete before creating new version if exists
	private Path pathToFrameworkPkgsOut;//output - overwritten text file
	private ILogger logger;
	private Set<ArchiveEntry<? extends DexEntry>> frameworkArchives;
	private Set<ArchiveEntry<? extends DexEntry>> appArchives;
	private Set<ArchiveEntry<? extends DexEntry>> emptyArchives;
	private Set<ArchiveEntry<? extends DexEntry>> errorArchives;
	private Set<ArchiveEntry<? extends DexEntry>> skippedArchives;
	private Map<String,Set<ArchiveEntry<? extends DexEntry>>> classesToSources;
	private Map<ArchiveEntry<? extends DexEntry>,Set<String>> sourcesToClasses;
	private final int defaultApiVersion;
	private final int javaVersion;
	private Path pathToInputDir;//input for checksum only
	private Path pathToSystemImgZipFile;//input for checksum only
	private final boolean allAppsSameTime;
	private List<String> resolvedBootClassPath;
	private final boolean includeApps;
	private final boolean dumpClasses;

	public DexToJimpleDecompiler(Path pathToInputDir, Path pathToSystemImgZipFile, 
			Set<? extends ArchiveEntry<? extends DexEntry>> archiveEntries, 
			Path pathToWorkingDir, Path pathToSystemArchivesZipFile, Path pathToJimpleJar, Path pathToJimpleJarFrameworkOnly, 
			Path pathToJimpleJarConflicts, Path pathToAndroidInfoFile, Path pathToClassesJar, int defaultApiVersion, int javaVersion,
			boolean allAppsSameTime, boolean includeApps, boolean dumpClasses, List<String> resolvedBootClassPath, Path pathToFrameworkPkgsOut, 
			ILogger logger) {
		Objects.requireNonNull(pathToInputDir);
		Objects.requireNonNull(pathToSystemImgZipFile);
		Objects.requireNonNull(archiveEntries);
		Objects.requireNonNull(pathToWorkingDir);
		Objects.requireNonNull(pathToSystemArchivesZipFile);
		Objects.requireNonNull(pathToJimpleJar);
		Objects.requireNonNull(pathToJimpleJarFrameworkOnly);
		Objects.requireNonNull(pathToJimpleJarConflicts);
		Objects.requireNonNull(pathToAndroidInfoFile);
		Objects.requireNonNull(resolvedBootClassPath);
		Objects.requireNonNull(pathToFrameworkPkgsOut);
		Objects.requireNonNull(logger);
		this.pathToInputDir = pathToInputDir;
		this.pathToSystemImgZipFile = pathToSystemImgZipFile;
		this.pathToAndroidInfoFile = pathToAndroidInfoFile;
		this.archiveEntries = archiveEntries;
		this.pathToWorkingDir = pathToWorkingDir;
		this.pathToSystemArchivesZipFile = pathToSystemArchivesZipFile;
		this.pathToJimpleJar = pathToJimpleJar;
		this.pathToJimpleJarFrameworkOnly = pathToJimpleJarFrameworkOnly;
		this.pathToJimpleJarConflicts = pathToJimpleJarConflicts;
		this.pathToClassesJar = pathToClassesJar;
		this.resolvedBootClassPath = resolvedBootClassPath;
		this.pathToFrameworkPkgsOut = pathToFrameworkPkgsOut;
		this.logger = logger;
		this.frameworkArchives = new LinkedHashSet<>();
		this.appArchives = new LinkedHashSet<>();
		this.emptyArchives = new LinkedHashSet<>();
		this.errorArchives = new LinkedHashSet<>();
		this.skippedArchives = new LinkedHashSet<>();
		this.classesToSources = new LinkedHashMap<>();
		this.sourcesToClasses = new LinkedHashMap<>();
		this.defaultApiVersion = defaultApiVersion;
		this.allAppsSameTime = allAppsSameTime;
		for(ArchiveEntry<? extends DexEntry> archiveEntry : this.archiveEntries){
			archiveEntry.setRootPathToArchive(this.pathToWorkingDir);
		}
		this.includeApps = includeApps;
		this.javaVersion = javaVersion;
		this.dumpClasses = dumpClasses;
	}
	
	public boolean run() {
		boolean success = true;
		logger.info("DexToJimpleDecompiler: Begin the common pre-process procedures for decompiling dex to jimple.");
		if(!extractArchives()){
			logger.fatal("DexToJimpleDecompiler: Failed to complete all common pre-process procedures for decompiling dex to jimple.");
			removeWorkingDir();
			return false;
		}
		logger.info("DexToJimpleDecompiler: Successfully completed the common pre-process procedures for decompiling dex to jimple.");
		
		logger.info("DexToJimpleDecompiler: Begin decompiling the dex in all archives to jimple and creating the jimple jar.");
		if(sortArchives() && generateClassAndSourceMappings()){
			if(!dumpFrameworkPkgs())
				success = false;
			if(!generateConflicts())
				success = false;
			if(!createJimpleJar())
				success = false;
		}else{
			success = false;
		}
		
		if(success && dumpClasses) {
			if(!makeClassJar())
				success = false;
		}
		
		if(success){
			logger.info("DexToJimpleDecompiler: Successfully decompiled all archives to jimple and created the jimple jar.");
		}else{
			logger.fatal("DexToJimpleDecompiler: An error occured while decompiling and creating the jimple jar. The classes contained within the"
					+ " jimple jar (if it exists) may not be complete or correct.");
		}
		
		if(!removeWorkingDir()){
			success = false;
		}
		
		return success;
	}
	
	private boolean removeWorkingDir(){
		if(FileHelpers.checkRWDirectoryExists(pathToWorkingDir)){
			try{
				FileHelpers.removeDirectory(pathToWorkingDir);
			}catch(Throwable t){
				logger.fatal("DexToJimpleDecompiler: Failed to completly remove the working directory at '{}'.",t,pathToWorkingDir);
				return false;
			}
		}
		return true;
	}
	
	private boolean createJimpleJar(){
		//Delete old jimple jar archive if one exists and writes checksum of source
		try {
			List<Path> fullPaths = new ArrayList<>(2);
			List<Path> realtivePaths = new ArrayList<>(2);
			fullPaths.add(pathToSystemImgZipFile);
			fullPaths.add(pathToAndroidInfoFile);
			realtivePaths.add(pathToInputDir.relativize(pathToSystemImgZipFile));
			realtivePaths.add(pathToInputDir.relativize(pathToAndroidInfoFile));
			SimpleArchiver.addSourceChecksumToArchive(pathToJimpleJar, fullPaths, realtivePaths, true);
		} catch(Throwable t) {
			logger.fatal("DexToJimpleDecompiler: Failed to write the checksum of source file '{}' and '{}' to the new archive '{}'.",t,
					pathToSystemImgZipFile,pathToAndroidInfoFile,pathToJimpleJar);
			return false;
		}
		
		boolean success = true;
		if(frameworkToJimple() == 0)
			success = false;
		if(allAppsSameTime) {
			if(!appsToJimpleAllAtOnce())
				success = false;
		} else {
			if(!appsToJimple())
				success = false;
		}
		return success;
	}
	
	private boolean extractArchives(){
		try{
			SimpleArchiver.extractArchive(pathToSystemArchivesZipFile, pathToWorkingDir);
		}catch(Throwable t){
			logger.fatal("DexToJimpleDecompiler: Failed to extract the system archive zip file at '{}' to the directory '{}'.",
					t,pathToSystemArchivesZipFile,pathToWorkingDir);
			return false;
		}
		return true;
	}
	
	private String constructStateMessage(){
		StringBuilder sb = new StringBuilder();
		sb.append("  Framework Archives:\n");
		for(ArchiveEntry<? extends DexEntry> archiveEntry : frameworkArchives){
			sb.append("    ").append(archiveEntry.getLocation()).append("/").append(archiveEntry.getName()).append(".");
			sb.append(archiveEntry.getExtension()).append(" : ").append(archiveEntry.getPathToArchive()).append("\n");
		}
		sb.append("  App Archives:\n");
		for(ArchiveEntry<? extends DexEntry> archiveEntry: appArchives){
			sb.append("    ").append(archiveEntry.getLocation()).append("/").append(archiveEntry.getName()).append(".");
			sb.append(archiveEntry.getExtension()).append(" : ").append(archiveEntry.getPathToArchive()).append("\n");
		}
		sb.append("  Skipped Archives:\n");
		for(ArchiveEntry<? extends DexEntry> archiveEntry: skippedArchives){
			sb.append("    ").append(archiveEntry.getLocation()).append("/").append(archiveEntry.getName()).append(".");
			sb.append(archiveEntry.getExtension()).append(" : ").append(archiveEntry.getPathToArchive()).append("\n");
		}
		sb.append("  Empty Archives:\n");
		for(ArchiveEntry<? extends DexEntry> archiveEntry: emptyArchives){
			sb.append("    ").append(archiveEntry.getLocation()).append("/").append(archiveEntry.getName()).append(".");
			sb.append(archiveEntry.getExtension()).append(" : ").append(archiveEntry.getPathToArchive()).append("\n");
		}
		sb.append("  Error Archives:\n");
		for(ArchiveEntry<? extends DexEntry> archiveEntry: errorArchives){
			sb.append("    ").append(archiveEntry.getLocation()).append("/").append(archiveEntry.getName()).append(".");
			sb.append(archiveEntry.getExtension()).append(" : ").append(archiveEntry.getPathToArchive()).append("\n");
		}
		return sb.toString();
	}
	
	/*private void sortArchivesFrameworkHelper(String name, ArrayDeque<ArchiveEntry<? extends DexEntry>> toProcess, 
			Set<String> processedOrToBeProcessedFiles) {
		for(Iterator<ArchiveEntry<? extends DexEntry>> it = frameworkArchives.iterator(); it.hasNext();) {
			ArchiveEntry<? extends DexEntry> archiveEntry = it.next();
			String archiveName = archiveEntry.getName()+"."+archiveEntry.getExtension();
			boolean added = false;
			if(archiveName.equals(name)) {
				toProcess.add(archiveEntry);
				it.remove();
				added = true;
			} else {
				for(DexEntry dexEntry : archiveEntry.getDexFiles()) {
					Path oatFile = dexEntry.getPathToOatFile();
					if(oatFile != null && oatFile.getFileName().toString().equals(name)) {
						toProcess.add(archiveEntry);
						it.remove();
						added = true;
						break;
					}
				}
			}
			if(added) {
				processedOrToBeProcessedFiles.add(archiveName);
				for(DexEntry dexEntry : archiveEntry.getDexFiles()) {
					Path oatFile = dexEntry.getPathToOatFile();
					if(oatFile != null) {
						processedOrToBeProcessedFiles.add(oatFile.getFileName().toString());
					}
				}
			}
		}
	}*/
	
	private boolean sortArchives() {
		try{
			logger.info("DexToJimpleDecompiler: Begin sorting the archive entries into groups.");
			for(ArchiveEntry<? extends DexEntry> archiveEntry : archiveEntries){
				if(archiveEntry.hasDexFiles() && !archiveEntry.allErrorState()){
					if(archiveEntry.getLocation().equals("framework")){//framework
						frameworkArchives.add(archiveEntry);
					}else{//app or priv-app
						if(includeApps)
							appArchives.add(archiveEntry);
						else
							skippedArchives.add(archiveEntry);
					}
				}else{//empty or the extraction process failed for all dex files in the archive
					logger.info("DexToJimpleDecompiler: The archive '{}.{}' of location '{}' at path '{}' is {}. It will not be processed.",
							archiveEntry.getName(),archiveEntry.getExtension(),archiveEntry.getLocation(),archiveEntry.getPathToArchive(),
							!archiveEntry.hasDexFiles() ? "empty" : "entirely in an error state");
					if(!archiveEntry.hasDexFiles()){
						emptyArchives.add(archiveEntry);
					}else{
						errorArchives.add(archiveEntry);
					}	
				}
			}
			
			//order all archive sets in a predictable way that is not dependent on the file system
			
			//sort the framework archives using the boot class path as a starting point
			Set<ArchiveEntry<? extends DexEntry>> sortedframeworkArchives = new LinkedHashSet<>();
			for(String entry : resolvedBootClassPath) {
				String fileName = Paths.get(entry).getFileName().toString();
				for(Iterator<ArchiveEntry<? extends DexEntry>> it = frameworkArchives.iterator(); it.hasNext();) {
					ArchiveEntry<? extends DexEntry> archiveEntry = it.next();
					String archiveName = archiveEntry.getName()+"."+archiveEntry.getExtension();
					if(archiveName.equals(fileName)) {
						it.remove();
						sortedframeworkArchives.add(archiveEntry);
					}
				}
			}
			
			
			/*ArrayDeque<ArchiveEntry<? extends DexEntry>> toProcess = new ArrayDeque<>();
			Set<String> processedOrToBeProcessedFiles = new HashSet<>();
			String[] bcp = bootClassPath.split(":");
			for(String bcpEntry : bcp) {
				sortArchivesFrameworkHelper(bcpEntry,toProcess,processedOrToBeProcessedFiles);
			}
			
			//if some were in the boot class path then process their decency trees if any
			while(!toProcess.isEmpty()) {
				ArchiveEntry<? extends DexEntry> cur = toProcess.poll();
				sortedframeworkArchives.add(cur);
				Set<String> deps = cur.getDeps();
				for(String dep : deps) {
					if(!processedOrToBeProcessedFiles.contains(dep)) {
						sortArchivesFrameworkHelper(dep,toProcess,processedOrToBeProcessedFiles);
					}
				}
			}*/
			
			if(!frameworkArchives.isEmpty()){
				//if all else fails sort whatever remains alphabetically and add to the end of our sorted list
				sortedframeworkArchives.addAll(SortingMethods.sortSet(frameworkArchives,ArchiveEntry.simpleComp));
			}
			frameworkArchives = sortedframeworkArchives;
			
			//Sort apps and the remaining alphabetically because they should only depend on the boot class path and not each other
			appArchives = SortingMethods.sortSet(appArchives,ArchiveEntry.simpleComp);
			errorArchives = SortingMethods.sortSet(errorArchives,ArchiveEntry.simpleComp);
			emptyArchives = SortingMethods.sortSet(emptyArchives,ArchiveEntry.simpleComp);
			
		}catch(Throwable t){
			logger.fatal("DexToJimpleDecompiler: Something went wrong during the sorting phase.",t);
			return false;
		}
		logger.info("DexToJimpleDecompiler: Successfully sorted the archives into the following groups:\n{}",constructStateMessage());
		return true;
	}
	
	private boolean dumpFrameworkPkgs() {
		logger.info("DexToJimpleDecompiler: Dumping the framework package names to file.");
		try(PrintStreamUnixEOL out = new PrintStreamUnixEOL(Files.newOutputStream(pathToFrameworkPkgsOut))) {
			for(ArchiveEntry<? extends DexEntry> archiveEntry : frameworkArchives) {
				out.println(archiveEntry.getName());
			}
		} catch(Throwable t) {
			logger.fatal("DexToJimpleDecompiler: Failed to dump the framework package names to the file '{}'.",t,pathToFrameworkPkgsOut);
			return false;
		}
		logger.info("DexToJimpleDecompiler: Successfully dumped the framework package names to file.");
		return true;
	}
	
	private boolean generateClassAndSourceMappings(){
		logger.info("DexToJimpleDecompiler: Begin generating source archive <-> class name mappings for all archives.");
		BlockingQueue<Throwable> errs = new LinkedBlockingQueue<>();
		try{
			CountingThreadExecutor exe = new CountingThreadExecutor();
			for(ArchiveEntry<? extends DexEntry> archiveEntry : frameworkArchives){
				for(DexEntry dexEntry : archiveEntry.getDexFiles()){
					exe.execute(new RecordSourcesAndClassesTask(archiveEntry,dexEntry,classesToSources,sourcesToClasses,errs));
				}
			}
			for(ArchiveEntry<? extends DexEntry> archiveEntry : appArchives){
				for(DexEntry dexEntry : archiveEntry.getDexFiles()){
					exe.execute(new RecordSourcesAndClassesTask(archiveEntry,dexEntry,classesToSources,sourcesToClasses,errs));
				}
			}
			if(exe != null)
				exe.shutdownWhenFinished();
			errs.addAll(exe.getAndClearExceptions());
		}catch(Throwable t){
			errs.offer(t);
		}
		if(!errs.isEmpty()){
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try(PrintStream ps = new PrintStream(baos,true,"utf-8")){
				ps.println("DexToJimpleDecompiler: Failed to successfully generate the source archive <-> class name mappings. The following "
						+ "exceptions were thrown in the process:");
				int i = 0;
				for(Throwable t : errs){
					ps.print("Exception ");
					ps.print(i++);
					ps.print(": ");
					t.printStackTrace(ps);
				}
				logger.fatal(new String(baos.toByteArray(), StandardCharsets.UTF_8));
			}catch(Throwable t){
				logger.fatal("DexToJimpleDecompiler: Something went wrong when generating the group exception log entry.",t);
			}
			return false;
		}
		
		Map<ArchiveEntry<? extends DexEntry>,Set<String>> sourcesToClassesSort = new LinkedHashMap<>();
		for(ArchiveEntry<? extends DexEntry> archiveEntry : archiveEntries){
			Set<String> classes = sourcesToClasses.get(archiveEntry);
			if(classes != null){
				sourcesToClassesSort.put(archiveEntry, SortingMethods.sortSet(classes,SortingMethods.sComp));
			}
		}
		sourcesToClasses = sourcesToClassesSort;
		
		classesToSources = SortingMethods.sortMapKey(classesToSources, SortingMethods.sComp);
		for(String className : classesToSources.keySet()){
			Set<ArchiveEntry<? extends DexEntry>> sources = classesToSources.get(className);
			Set<ArchiveEntry<? extends DexEntry>> sourcesSort = new LinkedHashSet<>();
			for(ArchiveEntry<? extends DexEntry> archiveEntry : archiveEntries){
				if(sources.contains(archiveEntry))
					sourcesSort.add(archiveEntry);
			}
			classesToSources.put(className, sourcesSort);
		}
		
		logger.info("DexToJimpleDecompiler: Successfully generated source archive <-> class name mappings for all archives.");
		return true;
	}
	
	private static class RecordSourcesAndClassesTask implements Runnable {
		private ArchiveEntry<? extends DexEntry> archiveEntry;
		private DexEntry dexEntry;
		private Map<String,Set<ArchiveEntry<? extends DexEntry>>> classesToSources;
		private Map<ArchiveEntry<? extends DexEntry>,Set<String>> sourcesToClasses;
		private BlockingQueue<Throwable> errs;
		public RecordSourcesAndClassesTask(ArchiveEntry<? extends DexEntry> archiveEntry, DexEntry dexEntry, 
				Map<String,Set<ArchiveEntry<? extends DexEntry>>> classesToSources, 
				Map<ArchiveEntry<? extends DexEntry>,Set<String>> sourcesToClasses,BlockingQueue<Throwable> errs){
			this.archiveEntry = archiveEntry;
			this.dexEntry = dexEntry;
			this.classesToSources = classesToSources;
			this.sourcesToClasses = sourcesToClasses;
			this.errs = errs;
		}
		@Override
		public void run() {
			try{
				MultiDexContainer.DexEntry<? extends DexBackedDexFile> h = DexFileFactory.loadDexEntry(archiveEntry.getPathToArchive().toFile(), dexEntry.getDexFileName(), false, 
						Opcodes.forApi(dexEntry.getApiVersion()));
				DexBackedDexFile d = h.getDexFile();
				for (ClassDef c : d.getClasses()) {
					String className = Util.dottedClassName(c.getType());
					addClassToClassesToSources(archiveEntry,className);
					addSourceToSourcesToClasses(archiveEntry,className);
				}
			}catch(Throwable t){
				errs.offer(new RuntimeException("Error: Failed to record the classes to archive relationships for archive '" 
						+ archiveEntry.getLocation() + "/" + archiveEntry.getName() + "." + archiveEntry.getExtension() + "' and dex '" 
						+ dexEntry.getDexFileName() + "'.",t));
			}
		}
		private void addClassToClassesToSources(ArchiveEntry<? extends DexEntry> archiveEntry, String className){
			synchronized (classesToSources) {
				Set<ArchiveEntry<? extends DexEntry>> sources = classesToSources.get(className);
				if(sources == null){
					sources = new LinkedHashSet<>();
					classesToSources.put(className, sources);
				}
				sources.add(archiveEntry);
			}
		}
		private void addSourceToSourcesToClasses(ArchiveEntry<? extends DexEntry> archiveEntry, String className){
			synchronized (sourcesToClasses) {
				Set<String> classes = sourcesToClasses.get(archiveEntry);
				if(classes == null){
					classes = new LinkedHashSet<>();
					sourcesToClasses.put(archiveEntry, classes);
				}
				classes.add(className);
			}
		}
	}
	
	private boolean generateConflicts(){
		logger.info("DexToJimpleDecompiler: Begin outputting conflict information.");
		
		//Compute the archive to conflict classes map from the classesToSources map
		Map<ArchiveEntry<? extends DexEntry>, Set<String>> archivesWithConflicts = new LinkedHashMap<>();
		for(String className : classesToSources.keySet()){
			Set<ArchiveEntry<? extends DexEntry>> sources = classesToSources.get(className);
			if(sources.size() > 1){
				for(ArchiveEntry<? extends DexEntry> source : sources){
					Set<String> classes = archivesWithConflicts.get(source);
					if(classes == null){
						classes = new LinkedHashSet<>();
						archivesWithConflicts.put(source, classes);
					}
					classes.add(className);
				}
			}
		}
		
		//No conflicts then do nothing
		if(archivesWithConflicts.isEmpty()){
			logger.info("DexToJimpleDecompiler: No conflicts exist. No need to output conflict information.");
			return true;
		}
		
		//Sort the archives to conflict classes map for consistent output
		Map<ArchiveEntry<? extends DexEntry>,Set<String>> archivesWithConflictsSort = new LinkedHashMap<>();
		for(ArchiveEntry<? extends DexEntry> archiveEntry : archiveEntries){
			Set<String> classes = archivesWithConflicts.get(archiveEntry);
			if(classes != null){
				archivesWithConflictsSort.put(archiveEntry, SortingMethods.sortSet(classes,SortingMethods.sComp));
			}
		}
		archivesWithConflicts = archivesWithConflictsSort;
		
		//Delete old conflicts archive if one exists and write the source checksum
		logger.info("DexToJimpleDexompiler: Writing the source checksum file to the conflict archive.");
		try {
			List<Path> fullPaths = new ArrayList<>(2);
			List<Path> realtivePaths = new ArrayList<>(2);
			fullPaths.add(pathToSystemImgZipFile);
			fullPaths.add(pathToAndroidInfoFile);
			realtivePaths.add(pathToInputDir.relativize(pathToSystemImgZipFile));
			realtivePaths.add(pathToInputDir.relativize(pathToAndroidInfoFile));
			SimpleArchiver.addSourceChecksumToArchive(pathToJimpleJarConflicts, fullPaths, realtivePaths, true);
		} catch(Throwable t) {
			logger.fatal("DexToJimpleDecompiler: Failed to write the checksum of source files '{}' and '{} to the conflict archive '{}'.",t,
					pathToSystemImgZipFile,pathToAndroidInfoFile,pathToJimpleJarConflicts);
			return false;
		}
		logger.info("DexToJimpleDecompiler: Successfully wrote thr source checksum file to the conflict archive.");
		
		//Write an xml file detailing the classes and their various conflicts
		logger.info("DexToJimpleDecompiler: Writing the class conflict list to the conflict archive.");
		try{
			ClassConflictsArchiver.writeConflictsListToArchive(pathToJimpleJarConflicts, classesToSources);
		}catch(Throwable t){
			logger.fatal("DexToJimpleDecompiler: Failed to output conflict information. An error occured when writing the class "
					+ "conflict list to the conflict archive at '{}'.",t,pathToJimpleJarConflicts);
			return false;
		}
		logger.info("DexToJimpleDecompiler: Successfully wrote the class conflict list to the conflict archive.");
		
		logger.info("DexToJimpleDecompiler: Begin outputting the conflicting classes to jimple files.");
		boolean success = true;
		for(ArchiveEntry<? extends DexEntry> archiveEntry : archivesWithConflicts.keySet()){
			Set<SootClass> classes = new LinkedHashSet<>();
			Set<String> classNames = archivesWithConflicts.get(archiveEntry);
			boolean successInner = true;
			String archiveDescriptor = archiveEntry.getLocation() + "@" + archiveEntry.getName() + "." + archiveEntry.getExtension();
			
			logger.info("DexToJimpleDecompiler: Outputting conflicting classes for archive '{}'.",archiveDescriptor);
			
			//init soot
			try{
				int apiVersion = archiveEntry.getApiVersion();
				if(!DexSootLoader.v().load(archiveEntry.getPathToArchive().toString(), classNames, 
						apiVersion < 0 ? defaultApiVersion : apiVersion, javaVersion, logger)){
					logger.fatal("DexToJimpleDecompiler: An error occured during the initilization of soot for the archive '{}'.",archiveDescriptor);
					successInner = false;
				}
			}catch(Throwable t){
				logger.fatal("DexToJimpleDecompiler: An error occured during the initilization of soot for the archive '{}'.",t,archiveDescriptor);
				successInner = false;
			}
			
			if(successInner){
				//Find a SootClass for each class name
				try{
					for(String className : classNames){
						SootClass sc = Scene.v().getSootClassUnsafe(className);
						if(sc == null || sc.isPhantom()){
							logger.fatal("DexToJimpleDecompiler: No SootClass could be found for class name '{}'.",className);
							successInner = false;
						}else{
							classes.add(sc);
						}
					}
					if(!successInner)
						logger.fatal("DexToJimpleDecompiler: Not all conflict class names refered to a defined SootClass for the archive '{}'.",
								archiveDescriptor);
				}catch(Throwable t){
					logger.fatal("DexToJimpleDecompiler: An error occured when attempting to find a SootClass for all conflict class names "
							+ "for the archive '{}'.",t,archiveDescriptor);
					successInner = false;
				}
			}
			
			boolean errorInWriting = false;
			if(successInner){
				//write the jimple to the conflict classes archive
				try{
					ClassConflictsArchiver.writeConflictsClassesToArchive(pathToJimpleJarConflicts, archiveDescriptor, classes);
				}catch(Throwable t){
					logger.fatal("DexToJimpleDecompiler: One or more of the conflict classes in the archive '{}' did not output successfully."
							,t,archiveDescriptor);
					successInner = false;
					errorInWriting = true;
				}
			}
			
			if(successInner){
				logger.info("DexToJimpleDecompiler: Successfully output all conflict classes for archive '{}'.",archiveDescriptor);
			}else{
				logger.fatal("DexToJimpleDecompiler: Failed to {} output all conflict classes for archive '{}'.",errorInWriting ? "completly" : "",
						archiveDescriptor);
				success = successInner;
			}
		}
		
		if(success){
			logger.info("DexToJimpleDecompiler: Successfully output all conflicting classes to jimple files.");
			logger.info("DexToJimpleDecompiler: All conflict information was successfully output.");
		}else{
			logger.fatal("DexToJimpleDecompiler: Failed to output all conflicting classes to jimple files.");
			logger.fatal("DexToJimpleDecompiler: Some conflict information was not output successfully.");
		}
		return success;
	}
	
	private boolean makeClassJar() {
		boolean success = true;
		
		logger.info("DexToJimpleDecompiler: Begin dumping the .jimple to .class.");
		
		try {
			if(!BasicSootLoader.v().load(pathToJimpleJar, true, javaVersion, logger)) {
				logger.fatal("DexToJimpleDecompiler: An error occured during the initilization of soot.");
				success = false;
			}
		} catch(Throwable t) {
			logger.fatal("DexToJimpleDecompiler: An error occured during the initilization of soot.",t);
			success = false;
		}
		
		if(success) {
			try{
				JimpleToClassArchiver.writeJimpleClassesToArchive(pathToClassesJar, SootLoader.javaVersionConvert(javaVersion));
			}catch(Throwable t){
				logger.fatal("DexToJimpleDecompiler: One or more classes did not get written to '{}'.",t,pathToClassesJar);
				success = false;
			}
		}
		
		if(success) {
			logger.info("DexToJimpleDecompiler: Successfully dumped all .jimple to .class.");
			return true;
		} else {
			logger.fatal("DexToJimpleDecompiler: Failed to dump all .jimple to .class.");
			return false;
		}
		
	}
	
	//0 fatal, 1 complete success, 2 some success some failure
	private int frameworkToJimple(){
		boolean success = true;
		int apiVersion = -1;
		Set<String> classNames = new LinkedHashSet<>();
		Set<Path> archivePaths = new LinkedHashSet<>();
		Set<SootClass> classes = new LinkedHashSet<>();
		
		logger.info("DexToJimpleDecompiler: Begin decompiling the framework into jimple.");
		
		//Generate the list of classes to output
		for(ArchiveEntry<? extends DexEntry> archiveEntry : frameworkArchives){
			classNames.addAll(sourcesToClasses.get(archiveEntry));
			archivePaths.add(archiveEntry.getPathToArchive());
			if(archiveEntry.getApiVersion() > apiVersion)
				apiVersion = archiveEntry.getApiVersion();
		}
		
		//Init soot
		try{
			if(!FrameworkDexSootLoader.v().load(archivePaths, classNames, apiVersion < 0 ? defaultApiVersion : apiVersion, javaVersion, 
					logger)){
				logger.fatal("DexToJimpleDecompiler: An error occured during the initilization of soot.");
				success = false;
			}
		}catch(Throwable t){
			logger.fatal("DexToJimpleDecompiler: An error occured during the initilization of soot.",t);
			success = false;
		}
		
		//Find a SootClass for each class name
		if(success) {
			try{
				for(String className : classNames){
					SootClass sc = Scene.v().getSootClassUnsafe(className);
					if(sc == null || sc.isPhantom()){
						logger.fatal("DexToJimpleDecompiler: No SootClass could be found for class name '{}'.",className);
						success = false;
					}else{
						classes.add(sc);
					}
				}
				if(!success){
					logger.fatal("DexToJimpleDecompiler: Not all class names refered to a defined SootClass for the framework.");
				}
			}catch(Throwable t){
				logger.fatal("DexToJimpleDecompiler: An error occured when attempting to find a SootClass for all class names.",t);
				success = false;
			}
		}
		
		//dump all framework classes to jimple inside the archive
		boolean errorInWriting = false;
		if(success) {
			try{
				DexToJimpleArchiver.writeJimpleClassesToArchive(pathToJimpleJar, "framework", classes);
			}catch(Throwable t){
				logger.fatal("DexToJimpleDecompiler: One or more classes did not get decompiled to jimple.",t);
				errorInWriting = true;
			}
			
			//create a frameworks only copy of the jimple jar
			try{
				if(FileHelpers.checkRWFileExists(pathToJimpleJar))
					Files.copy(pathToJimpleJar, pathToJimpleJarFrameworkOnly, StandardCopyOption.REPLACE_EXISTING);
			}catch(Throwable t){
				logger.fatal("DexToJimpleDecompiler: An error occured when creating a frameworks only copy of the jimple jar.");
				success = false;
			}
		}
		
		if(success && !errorInWriting){
			logger.info("DexToJimpleDecompiler: Successfully decompiled the framework into jimple.");
			return 1;
		}else if(success && errorInWriting){
			logger.fatal("DexToJimpleDecompiler: Failed to completly decompile the framework into jimple. Attempting to continue.");
			return 2;
		} else {
			logger.fatal("DexToJimpleDecompiler: Failed to decompile the framework into jimple.");
			return 0;
		}
	}
	
	private boolean appsToJimpleAllAtOnce() {
		boolean success = true;
		int apiVersion = -1;
		Set<String> classNames = new LinkedHashSet<>();
		Set<Path> archivePaths = new LinkedHashSet<>();
		Set<SootClass> classes = new LinkedHashSet<>();
		boolean errorInWriting = false;
		
		logger.info("DexToJimpleDecompiler: Begin decompiling all apps and priv-apps into jimple.");
		
		if(!appArchives.isEmpty()) {
		
			for(ArchiveEntry<? extends DexEntry> archiveEntry : appArchives){
				classNames.addAll(sourcesToClasses.get(archiveEntry));
				archivePaths.add(archiveEntry.getPathToArchive());
				if(archiveEntry.getApiVersion() > apiVersion)
					apiVersion = archiveEntry.getApiVersion();
			}
			
			//Init soot
			try{
				if(!AllAppsDexSootLoader.v().load(archivePaths, pathToJimpleJarFrameworkOnly, classNames, 
						apiVersion < 0 ? defaultApiVersion : apiVersion, javaVersion, logger)){
					logger.fatal("DexToJimpleDecompiler: An error occured during the initilization of soot.");
					success = false;
				}
			}catch(Throwable t){
				logger.fatal("DexToJimpleDecompiler: An error occured during the initilization of soot.",t);
				success = false;
			}
			
			//Find a SootClass for each class name
			if(success) {
				try{
					for(String className : classNames){
						SootClass sc = Scene.v().getSootClassUnsafe(className);
						if(sc == null || sc.isPhantom()){
							logger.fatal("DexToJimpleDecompiler: No SootClass could be found for class name '{}'.",className);
							success = false;
						}else{
							classes.add(sc);
						}
					}
					if(!success){
						logger.fatal("DexToJimpleDecompiler: Not all class names refered to a defined SootClass.");
					}
				}catch(Throwable t){
					logger.fatal("DexToJimpleDecompiler: An error occured when attempting to find a SootClass for all class names.",t);
					success = false;
				}
			}
			
			//dump all framework classes to jimple inside the archive
			if(success) {
				try{
					DexToJimpleArchiver.writeJimpleClassesToArchive(pathToJimpleJar, "apps and priv-apps", classes);
				}catch(Throwable t){
					logger.fatal("DexToJimpleDecompiler: One or more classes did not get decompiled to jimple.",t);
					errorInWriting = true;
					success = false;
				}
			}
			
		}
		
		if(success){
			logger.info("DexToJimpleDecompiler: Successfully decompiled all apps and priv-apps into jimple.");
		}else{
			logger.fatal("DexToJimpleDecompiler: Failed to {} decompile all apps and priv-apps into jimple.",errorInWriting ? "completly" : "");
		}
		
		return success;
	}
	
	private boolean appsToJimple(){
		boolean success = true;
		
		logger.info("DexToJimpleDecompiler: Begin decompiling the apps into jimple.");
		
		for(ArchiveEntry<? extends DexEntry> archiveEntry : appArchives){
			boolean successInner = true;
			String archiveDescriptor = archiveEntry.getLocation() + "@" + archiveEntry.getName() + "." + archiveEntry.getExtension();
			Set<String> classNames = sourcesToClasses.get(archiveEntry);
			Set<SootClass> classes = new LinkedHashSet<>();
			
			logger.info("DexToJimpleDecompiler: Decompiling the app '{}'.",archiveDescriptor);
			
			//init soot
			try{
				int apiVersion = archiveEntry.getApiVersion();
				if(!AppDexSootLoader.v().load(archiveEntry.getPathToArchive(), pathToJimpleJarFrameworkOnly, classNames,
						apiVersion < 0 ? defaultApiVersion : apiVersion, javaVersion, logger)){
					logger.fatal("DexToJimpleDecompiler: An error occured during the initilization of soot for the app '{}'.",archiveDescriptor);
					successInner = false;
				}
			}catch(Throwable t){
				logger.fatal("DexToJimpleDecompiler: An error occured during the initilization of soot for the app '{}'.",t,archiveDescriptor);
				successInner = false;
			}
			
			if(successInner){
				//Find a SootClass for each class name
				try{
					for(String className : classNames){
						SootClass sc = Scene.v().getSootClassUnsafe(className);
						if(sc == null || sc.isPhantom()){
							logger.fatal("DexToJimpleDecompiler: No SootClass could be found for class name '{}'.",className);
							successInner = false;
						}else{
							classes.add(sc);
						}
					}
					if(!successInner)
						logger.fatal("DexToJimpleDecompiler: Not all class names refered to a defined SootClass for the app '{}'.",
								archiveDescriptor);
				}catch(Throwable t){
					logger.fatal("DexToJimpleDecompiler: An error occured when attempting to find a SootClass for all class names for the app '{}'."
							,t,archiveDescriptor);
					successInner = false;
				}
			}
			
			boolean errorInWriting = false;
			if(successInner){
				//write the jimple to the jimple jar
				try{
					DexToJimpleArchiver.writeJimpleClassesToArchive(pathToJimpleJar, archiveDescriptor, classes);
				}catch(Throwable t){
					logger.fatal("DexToJimpleDecompiler: One or more of the classes in the app '{}' did not get decompiled to jimple."
							,t,archiveDescriptor);
					successInner = false;
					errorInWriting = true;
				}
			}
			
			if(successInner){
				logger.info("DexToJimpleDecompiler: Successfully decompiled the app '{}' into jimple.",archiveDescriptor);
			}else{
				logger.fatal("DexToJimpleDecompiler: Failed to {} decompile the app '{}' into jimple.",errorInWriting ? "completly" : "",
						archiveDescriptor);
				success = successInner;
			}
		}
		
		if(success){
			logger.info("DexToJimpleDecompiler: All apps were successfully decompiled to jimple.");
		}else{
			logger.fatal("DexToJimpleDecompiler: One or more apps were not successfully decompiled to jimple.");
		}
		return success;
	}
	
}
