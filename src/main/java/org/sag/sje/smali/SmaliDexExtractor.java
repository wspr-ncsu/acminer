package org.sag.sje.smali;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.DexBackedOdexFile;
import org.jf.dexlib2.dexbacked.OatFile;
import org.jf.dexlib2.dexbacked.OatFile.OatDexFile;
import org.jf.dexlib2.iface.MultiDexContainer;
import org.jf.dexlib2.iface.MultiDexContainer.DexEntry;
import org.jf.smali.Smali;
import org.jf.smali.SmaliOptions;
import org.sag.common.io.FileHelpers;
import org.sag.common.logging.ILogger;
import org.sag.common.tools.NoExitSecurityManager;
import org.sag.common.tools.NoExitSecurityManager.ExitException;
import org.sag.main.logging.PrintStreamLoggerWrapper;
import org.sag.main.logging.SingleLineFormatter;
import org.sag.sje.DexEntry.State;
import org.sag.sje.DexExtractor;
import org.sag.sje.VersionMap;

public class SmaliDexExtractor extends DexExtractor<SmaliArchiveEntry> {
	
	public SmaliDexExtractor(String bootClassPath, Path pathToInputDir, Path pathToSystemImgZipFile, Path pathToSystemArchivesZipFile, 
			Path pathToWorkingDir, Path pathToAndroidInfoFile, String[] locations, String[] archs, String[] apexLocations, int defaultApi, ILogger logger){
		super(bootClassPath,pathToInputDir, pathToSystemImgZipFile,pathToSystemArchivesZipFile,pathToWorkingDir,pathToAndroidInfoFile,locations,
				archs,apexLocations,defaultApi,logger);
	}
	
	public boolean runInner() {
		return extractDex();
	}
	
	private boolean extractDex(){
		boolean success = true;
		logger.info("{}: Extracting the dex files from the oat/odex files.",cname);
		for(SmaliArchiveEntry archiveEntry : archiveEntries.values()){
			if(!archiveEntry.skip()){
				if(!createArchiveWorkingDir(archiveEntry)){
					success = false;
					continue;
				}
				for(SmaliDexEntry dexEntry : archiveEntry.getDexFiles()){
					if(!dexEntry.skip()){
						logger.info("{}: Extracting the dex file '{}' for archive '{}.{}' of location "
								+ "'{}' from oat/odex file '{}'.",cname,dexEntry.getDexFileName(),archiveEntry.getName(),
								archiveEntry.getExtension(),archiveEntry.getLocation(),dexEntry.getPathToOatFile());
						if(!createDexEntrySmaliWorkingDir(archiveEntry,dexEntry)){
							success = false;
							continue;
						}
						if(decompileToSmali(archiveEntry,dexEntry) && recompileToDex(archiveEntry,dexEntry)){
							logger.info("{}: Successfully extracted the dex file '{}' "
									+ "for archive '{}.{}' of location '{}' from oat/odex file '{}' at '{}'.",cname,
									dexEntry.getDexFileName(),archiveEntry.getName(),archiveEntry.getExtension(),
									archiveEntry.getLocation(),dexEntry.getPathToOatFile(),
									dexEntry.getPathToDexFile());
						}else{
							logger.fatal("{}: Failed to extract the dex file '{}' "
									+ "for archive '{}.{}' of location '{}' from oat/odex file '{}'.",cname,
									dexEntry.getDexFileName(),archiveEntry.getName(),archiveEntry.getExtension(),
									archiveEntry.getLocation(),dexEntry.getPathToOatFile());
							success = false;
						}
					}else{
						logger.info("{}: The dex file '{}' already exists in archive '{}.{}' of location '{}'."
								+ " Nothing to extract.",cname,dexEntry.getDexFileName(),archiveEntry.getName(),
								archiveEntry.getExtension(),archiveEntry.getLocation());
					}
				}
			}else{
				logger.info("{}: No oat/odex files to extract for archive '{}.{}' of location '{}'. {}",
						cname,archiveEntry.getName(),archiveEntry.getExtension(),archiveEntry.getLocation(),
						archiveEntry.getDexFiles().isEmpty() ? "The archive is empty." : 
						"The archive contains already contains its dex files.");
			}
		}
		
		if(success){
			logger.info("{}: Successfully extracted all dex files from the oat/odex files.",cname);
		}else{
			logger.fatal("{}: Failed to extract all dex files from the oat/odex files.",cname);
		}
		
		return success;
	}
	
	private boolean createArchiveWorkingDir(SmaliArchiveEntry archiveEntry){
		try{
			FileHelpers.processDirectory(archiveEntry.getWorkingDir(), true, false);
		}catch(Throwable t){
			StringBuilder sb = new StringBuilder();
			for(SmaliDexEntry dexEntry : archiveEntry.getDexFiles()){
				if(!dexEntry.skip()){
					sb.append(dexEntry.toString("  "));
					dexEntry.setState(State.SETUPERR);
				}
			}
			logger.fatal("{}: Failed to create the working directory '{}' for archive '{}.{}' of "
					+ "location '{}'. The following dex file entries will not be processed:\n{}",
					cname,archiveEntry.getWorkingDir(),archiveEntry.getName(),archiveEntry.getExtension(),
					archiveEntry.getLocation(),sb.toString());
			return false;
		}
		return true;
	}
	
	private boolean createDexEntrySmaliWorkingDir(SmaliArchiveEntry archiveEntry, SmaliDexEntry dexEntry){
		try{
			FileHelpers.processDirectory(dexEntry.getPathToSmaliDir(), true, false);
		}catch(Throwable t){
			dexEntry.setState(State.SETUPERR);
			logger.fatal("{}: Failed to create the smali output directory and extract "
					+ "a dex file for the archive '{}.{}' of "
					+ "location '{}' for the following dex file entry:\n{}",t,cname,archiveEntry.getName(),
					archiveEntry.getExtension(),archiveEntry.getLocation(),dexEntry.toString("  "));
			return false;
		}
		dexEntry.setState(State.SETUPSUCC);
		return true;
	}
	
	private boolean decompileToSmali(SmaliArchiveEntry archiveEntry, SmaliDexEntry dexEntry){
		logger.info("{}: Decompiling the dex file '{}' for archive '{}.{}' of location '{}' from the oat/odex"
				+ " file '{}' to smali.",cname,dexEntry.getDexFileName(),archiveEntry.getName(),archiveEntry.getExtension(),
				archiveEntry.getLocation(),dexEntry.getPathToOatFile());
		
		PrintStream prevOut = System.out;
		PrintStream prevErr = System.err;
		try{
			System.setOut(new PrintStreamLoggerWrapper(logger,SingleLineFormatter.BAKSMALIID));
			System.setErr(new PrintStreamLoggerWrapper(logger,SingleLineFormatter.BAKSMALIID));
			NoExitSecurityManager.setup();
			
			String[] args = {
					"x",
					dexEntry.getPathToOatFile().toString()+(dexEntry.getOatEntry()==null?"":(File.separatorChar+"\""+dexEntry.getOatEntry()+"\"")),
					"-b",
					dexEntry.getBootclasspath(),
					"-d",
					dexEntry.getPathToBootClassPathFiles().toString(),
					"-o",
					dexEntry.getPathToSmaliDir().toString(),
					"-a",
					Integer.toString(dexEntry.getApiVersion())
			};
			
			org.jf.baksmali.Main.main(args);
		}catch(Throwable t){
			if(!(t instanceof ExitException) || ((ExitException)t).status != 0){
				dexEntry.setState(State.DECOMPERR);
				logger.fatal("{}: Failed to decompile the dex file '{}' for the archive '{}.{}' of "
						+ "location '{}' for the following dex file entry:\n{}",t,cname,dexEntry.getDexFileName(),archiveEntry.getName(),
						archiveEntry.getExtension(),archiveEntry.getLocation(),dexEntry.toString("  "));
				return false;
			}
		}finally{
			try{
				System.setOut(prevOut);
				System.setErr(prevErr);
				NoExitSecurityManager.reset();
			}catch(Throwable t){}
		}
		
		dexEntry.setState(State.DECOMPSUCC);
		logger.info("{}: Successfully decompiled the dex file '{}' for archive '{}.{}' of location '{}' "
				+ "from the oat/odex file '{}' to smali in the output path '{}'.",cname,dexEntry.getDexFileName(),
				archiveEntry.getName(),archiveEntry.getExtension(),archiveEntry.getLocation(),dexEntry.getPathToOatFile(),
				dexEntry.getPathToSmaliDir());
		return true;
	}
	
	private boolean recompileToDex(SmaliArchiveEntry archiveEntry, SmaliDexEntry dexEntry){
		logger.info("{}: Recompiling the dex file '{}' for archive '{}.{}' of location '{}' from the smali files"
				+ " at '{}'.",cname,dexEntry.getDexFileName(),archiveEntry.getName(),archiveEntry.getExtension(),
				archiveEntry.getLocation(),dexEntry.getPathToSmaliDir());
		
		PrintStream prevOut = System.out;
		PrintStream prevErr = System.err;
		try{
			System.setOut(new PrintStreamLoggerWrapper(logger,SingleLineFormatter.SMALIID));
			System.setErr(new PrintStreamLoggerWrapper(logger,SingleLineFormatter.SMALIID));
			NoExitSecurityManager.setup();
			
			SmaliOptions options = new SmaliOptions();
			options.outputDexFile = dexEntry.getPathToDexFile().toString();
			options.apiLevel = dexEntry.getApiVersion();
			options.allowOdexOpcodes = false;
			options.verboseErrors = true;
			
			Smali.assemble(options, dexEntry.getPathToSmaliDir().toString());
		}catch(Throwable t){
			dexEntry.setState(State.RECOMPERR);
			logger.fatal("{}: Failed to recompile the dex file '{}' for the archive '{}.{}' of "
					+ "location '{}' for the following dex file entry:\n{}",t,cname,dexEntry.getDexFileName(),archiveEntry.getName(),
					archiveEntry.getExtension(),archiveEntry.getLocation(),dexEntry.toString("  "));
			return false;
		}finally{
			try{
				System.setOut(prevOut);
				System.setErr(prevErr);
				NoExitSecurityManager.reset();
			}catch(Throwable t){}
		}
		dexEntry.setState(State.RECOMPSUCC);
		logger.info("{}: Successfully recompiled the dex file '{}' for archive '{}.{}' of location '{}' "
				+ "from the smali files at '{}' to the dex file at '{}'.",cname,dexEntry.getDexFileName(),
				archiveEntry.getName(),archiveEntry.getExtension(),archiveEntry.getLocation(),dexEntry.getPathToSmaliDir(),
				dexEntry.getPathToDexFile());
		return true;
	}
	
	public SmaliArchiveEntry getNewArchiveEntry(String name, String extension, String location, Path workingDir, Path rootPathToArchive){
		return new SmaliArchiveEntry(name,extension,location,workingDir,rootPathToArchive);
	}
	
	@Override
	protected List<String> resolveBootClassPathFromOatFiles(Set<Path> paths, List<String> bcp) throws Exception {
		ArrayDeque<Path> toProcess = new ArrayDeque<>();
		Set<Path> seen = new HashSet<>();
		for(Iterator<String> it = bcp.iterator(); it.hasNext();) {
			String bcpEntry = it.next();
			for(Path p : paths) {
				if(p.getFileName().toString().equals(bcpEntry)) {
					toProcess.add(p);
				}
			}
		}
		
		Set<String> newbcp = new LinkedHashSet<>();
		//if we have a starting point then use its dependencies to determine order
		while(!toProcess.isEmpty()) {
			Path cur = toProcess.poll();
			if(seen.add(cur)) {
				MultiDexContainer<? extends DexBackedDexFile> container = DexFileFactory.loadDexContainer(cur.toFile(), 
						Opcodes.forApi(defaultApi));
				if(container instanceof OatFile) {
					List<String> bootClassPath = ((OatFile)container).getBootClassPath();
					newbcp.addAll(bootClassPath);
					for(String s : bootClassPath) {
						String name = com.google.common.io.Files.getNameWithoutExtension(s);
						String name2 = "boot-" + name; //because the boot oat files start with boot-jarname
						for(Path p : paths) {
							String existingOatName = com.google.common.io.Files.getNameWithoutExtension(p.toString());
							if(name.equals(existingOatName) || name2.equals(existingOatName))
								toProcess.add(p);
						}
					}
				}
			}
		}
		
		if(!bcp.isEmpty()) {
			//Ensure that the resolved boot class path only contains jar files at the end
			for(Iterator<String> it = bcp.iterator(); it.hasNext();) {
				String bcpEntry = it.next();
				if(!com.google.common.io.Files.getFileExtension(bcpEntry).equals("jar"))
					it.remove();
			}
			//Add any remaining jar files in the original boot class path to the end
			if(!bcp.isEmpty())
				newbcp.addAll(bcp);
		}
		return new ArrayList<>(newbcp);
	}

	@Override
	protected boolean parseOatFile(String location, Path oatFilePath, Path workingDir, Path outputDir, String arch, Path bootDirForArch) {
		boolean success = true;
		try {
			//Note if the file being loaded is an oat file then the opcodes passed in here do nothing and the art version of the oat file is used
			//to lookup the opcodes instead
			MultiDexContainer<? extends DexBackedDexFile> container = DexFileFactory.loadDexContainer(oatFilePath.toFile(), 
					Opcodes.forApi(defaultApi));
			
			//Find the file decencies if this is an oat file and it lists them
			List<String> deps = Collections.emptyList();
			int oatVersion = 0;
			if(container instanceof OatFile) {
				List<String> temp = ((OatFile)container).getBootClassPath();
				oatVersion = ((OatFile)container).getOatVersion();
				if(!temp.isEmpty()) {
					deps = new ArrayList<>();
					for(String s : temp) {
						String ext = com.google.common.io.Files.getFileExtension(s);
						String name = com.google.common.io.Files.getNameWithoutExtension(s);
						if(ext.equals("art")){
							deps.add(name + ".oat");
						} else {
							deps.add(name + "." + ext);
						}
					}
				}
			}
			
			for(String entry : container.getDexEntryNames()){
				DexEntry<? extends DexBackedDexFile> dexEntry = container.getEntry(entry);
				DexBackedDexFile dexFileRef = dexEntry.getDexFile();
				if(dexFileRef instanceof DexBackedOdexFile){
					String archiveName = com.google.common.io.Files.getNameWithoutExtension(oatFilePath.getFileName().toString());
					String archiveExtension = com.google.common.io.Files.getFileExtension(oatFilePath.getFileName().toString());
					SmaliArchiveEntry archiveEntry = getOrAddArchiveEntry(location,archiveName,archiveExtension,workingDir,outputDir);
					archiveEntry.addDexFileData(bootClassPath, bootDirForArch, defaultApi, 
							oatFilePath);
				}else if(dexFileRef instanceof OatDexFile){
					try{
						int apiLevel = VersionMap.mapArtVersionToApi(oatVersion);
						String[] parts = parseOatEntryString(entry,oatVersion);
						String archiveName = parts[0];
						String archiveExtension = parts[1];
						String dexName = parts[2];
						SmaliArchiveEntry archiveEntry = getOrAddArchiveEntry(location,archiveName,archiveExtension,workingDir,outputDir);
						archiveEntry.addDexFileData(bootClassPath, bootDirForArch, entry, 
								dexName, apiLevel, oatFilePath, deps);
					}catch(Throwable t){
						logger.fatal("{}: Failed to parse the dex file '{}' in oat file '{}'.",t,cname,
								dexEntry.getEntryName(),oatFilePath);
						success = false;
					}
				}else{//We ignore RawDexFile, ZipDexFile, or something new because we are deodexing
					logger.fatal("{}: Expected a oat/odex file but got a dex, zip, or something else at path '{}'.",cname,oatFilePath);
					success = false;
				}
			}
		}catch(Throwable t){
			logger.fatal("{}: Failed to parse the oat/odex file '{}'.",t,cname,oatFilePath);
			success = false;
		}
		return success;
	}
	
	private SmaliArchiveEntry getOrAddArchiveEntry(String location, String archiveName, String archiveExt, Path workingDir, Path outputDir){
		String key = makeArchiveEntriesKey(location,archiveName,archiveExt);
		SmaliArchiveEntry archiveEntry = archiveEntries.get(key);
		if(archiveEntry == null){
			archiveEntry = new SmaliArchiveEntry(archiveName,archiveExt,location,
					FileHelpers.getPath(workingDir, archiveName),outputDir);
			archiveEntries.put(key, archiveEntry);
		}
		return archiveEntry;
	}

	@Override
	protected State requiredStateBeforePostProcessing() {
		return State.RECOMPSUCC;
	}

}
