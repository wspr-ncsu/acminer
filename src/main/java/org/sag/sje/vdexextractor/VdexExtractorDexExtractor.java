package org.sag.sje.vdexextractor;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.OatFile;
import org.jf.dexlib2.iface.MultiDexContainer;
import org.sag.common.io.FileHelpers;
import org.sag.common.logging.ILogger;
import org.sag.common.tools.LinuxOrWSLCommandBuilder;
import org.sag.sje.DexEntry.State;
import org.sag.sje.DexExtractor;
import org.sag.sje.VersionMap;

import com.google.common.io.ByteStreams;

public class VdexExtractorDexExtractor extends DexExtractor<VdexExtractorArchiveEntry> {
	
	private static final Pattern vdexExtractorOutputPattern = Pattern.compile("^.*_classes(\\d*)$",Pattern.CASE_INSENSITIVE);
	
	private Path pathToWorkingExeDir;
	private Path pathToScript;
	private LinuxOrWSLCommandBuilder cmdBuilder;

	public VdexExtractorDexExtractor(String bootClassPath, Path pathToInputDir, Path pathToSystemImgZipFile, Path pathToSystemArchivesZipFile, 
			Path pathToWorkingDir, Path pathToAndroidInfoFile, String[] locations, String[] archs, String[] apexLocations, int defaultApi, ILogger logger) {
		super(bootClassPath, pathToInputDir, pathToSystemImgZipFile, pathToSystemArchivesZipFile, pathToWorkingDir, pathToAndroidInfoFile, 
				locations, archs, apexLocations, defaultApi, logger);
		this.pathToWorkingExeDir = FileHelpers.getPath(pathToWorkingDir,"vdexExtractor");
	}

	@Override
	protected boolean runInner() {
		return true;
	}
	
	@Override
	protected boolean customPreProcess() {
		try {
			this.cmdBuilder = new LinuxOrWSLCommandBuilder();
		} catch(Throwable t) {
			logger.fatal("{}: Failed to initilize command builder.",t,cname);
			return false;
		}
		return extractExeToWorkingDir();
	}

	@Override
	protected VdexExtractorArchiveEntry getNewArchiveEntry(String name, String extension, String location, Path workingDir, Path rootPathToArchive) {
		return new VdexExtractorArchiveEntry(name, extension, location, workingDir, rootPathToArchive);
	}
	
	private boolean extractExeToWorkingDir(){
		try {
			FileHelpers.processDirectory(pathToWorkingExeDir, true, false);
		} catch(Throwable t) {
			logger.fatal("{}: Failed to create the working directory for vdexExtractor '{}'.",t,cname,pathToWorkingExeDir);
			return false;
		}
		
		Path pathsFile = FileHelpers.getPath(pathToWorkingExeDir, "paths.txt");
		try {
			try(InputStream in = getClass().getResourceAsStream("/vdexExtractor/paths.txt")) {
				try(OutputStream out = Files.newOutputStream(pathsFile)) {
					ByteStreams.copy(in, out);
				}
			}
		} catch(Throwable t) {
			logger.fatal("{}: Failed to extract the paths.txt file for vdexExtractor to the working directory at '{}'",t,cname,pathsFile);
			return false;
		}
		
		Set<String> dirs = new LinkedHashSet<>();
		Set<String> files = new LinkedHashSet<>();
		try(BufferedReader br = Files.newBufferedReader(pathsFile)) {
			String line;
			while((line = br.readLine()) != null) {
				line = line.trim();
				if(line.isEmpty() || line.startsWith("#"))
					continue;
				if(line.endsWith("/"))
					dirs.add(line);
				else
					files.add(line);
			}
		} catch(Throwable t) {
			logger.fatal("{}: Failed to parse the paths.txt file for vdexExtractor to the working directory at '{}'",t,cname,pathsFile);
			return false;
		}
		
		
		for(String dir : dirs) {
			Path path = FileHelpers.getPath(pathToWorkingExeDir, dir);
			try {
				FileHelpers.processDirectory(path, true, false);
			} catch(Throwable t) {
				logger.fatal("{}: Failed to create a working directory for vdexExtractor '{}'.",t,cname,path);
				return false;
			}	
		}
		
		for(String file : files) {
			Path path = FileHelpers.getPath(pathToWorkingExeDir, file);
			try {
				try(InputStream in = getClass().getResourceAsStream("/vdexExtractor/" + file)) {
					try(OutputStream out = Files.newOutputStream(path)) {
						ByteStreams.copy(in, out);
					}
				}
				if(path.getFileName().toString().equals("vdexExtractor")) {
					path.toFile().setExecutable(true);
				} else if(path.getFileName().toString().equals("run.sh")) {
					path.toFile().setExecutable(true);
					this.pathToScript = path;
				}
			} catch(Throwable t) {
				logger.fatal("{}: Failed to extract a vdexExtractor file to the working directory at '{}'",t,cname,path);
				return false;
			}
		}
		
		return true;
	}

	@Override
	protected boolean parseOatFile(String location, Path oatFilePath, Path workingDir, Path archiveDir, String arch, Path bootDirForArch) {
		//Use Smali to grab header information from the oat file because vdexExtractor does not give us much information
		List<String> deps = Collections.emptyList();
		int oatLevel = -1;
		int apiLevel = -1;
		String firstEntry = null;
		try {
			MultiDexContainer<? extends DexBackedDexFile> container = DexFileFactory.loadDexContainer(oatFilePath.toFile(), 
					Opcodes.forApi(defaultApi));
			
			
			List<String> temp = ((OatFile)container).getBootClassPath();
			oatLevel = ((OatFile)container).getOatVersion();
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
			
			//For oat files greater than 138 the dex files appear to no longer be
			//stored in the oat of vdex files but instead just in the original jar
			//or apk like before. This is indicated by the dex entries being empty
			//for the oat file as returned by dexlib2. We quick quit here because 
			//there is nothing to do.
			List<String> dexEntries = container.getDexEntryNames();
			if(oatLevel >= 138 && dexEntries.isEmpty()) {
				return true;
			}
			
			firstEntry = dexEntries.get(0);
			apiLevel = VersionMap.mapArtVersionToApi(oatLevel);
		} catch(Throwable t) {
			logger.fatal("{}: Failed to parse the oat/odex file header for '{}'.",t,cname,oatFilePath);
			return false;
		}
		
		//Setup commands to extract dex from vdex
		Path vdexFilePath = FileHelpers.getPath(oatFilePath.getParent(), 
				com.google.common.io.Files.getNameWithoutExtension(oatFilePath.getFileName().toString()) + ".vdex");
		List<String> cmds;
		try {
			// wsl "run.sh" -i "path.vdex" -o "path" or "run.sh" -i "path.vdex" -o "path"
			cmdBuilder.addPath(pathToScript);
			cmdBuilder.add("-i");
			cmdBuilder.addPath(vdexFilePath);
			cmdBuilder.add("-o");
			cmdBuilder.addPath(pathToWorkingExeDir);
			cmds = cmdBuilder.getCommand();
		} catch(Throwable t) {
			logger.fatal("{}: Failed to build the command to extract dex from vdex with vdexExtractor.",t,cname);
			return false;
		}
		
		//Extract dex from vdex
		try {
			ProcessBuilder pb = new ProcessBuilder(cmds);
			pb.redirectErrorStream(true);
			pb.directory(pathToWorkingExeDir.toFile());
			Process p = pb.start();
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String line;
			while((line = br.readLine()) != null) {
				sb.append("\n    ").append(line.trim());
			}
			int r = p.waitFor(); 
			if(r != 0) {
				logger.fatal("{}: VdexExtractor did not terminate normally when extracting the dex files from vdex file '{}' of "
						+ "location '{}'.\n  Command Output:{}",cname,vdexFilePath,location,sb.toString());
				return false;
			}
		} catch(Throwable t) {
			logger.fatal("{}: Unexpected error. Failed to parse the vdex file '{}' of location '{}'."
					,t,cname,vdexFilePath,location);
			return false;
		}
		
		//Locate the extracted files
		Set<Path> extractedDexFiles = null;
		Path vdexExtractorOutputDir = FileHelpers.getPath(pathToWorkingExeDir, "vdexExtractor_deodexed");
		try {
			extractedDexFiles = FileHelpers.find(vdexExtractorOutputDir, "*.dex", null, "f", null, null);
		} catch(Throwable t) {
			logger.fatal("{}: Unexpected error. Failed to locate the extracted dex files of '{}' at '{}'."
					,t,cname,vdexFilePath,vdexExtractorOutputDir);
			return false;
		}
		
		try {
			String[] parts = parseOatEntryString(firstEntry,oatLevel);
			String archiveName = parts[0];
			String archiveExtension = parts[1];
			String key = makeArchiveEntriesKey(location,archiveName,archiveExtension);
			VdexExtractorArchiveEntry archiveEntry = archiveEntries.get(key);
			if(archiveEntry == null) {
				archiveEntry = new VdexExtractorArchiveEntry(archiveName,archiveExtension,location,
						FileHelpers.getPath(workingDir, archiveName),archiveDir);
				archiveEntries.put(key, archiveEntry);
				try {
					FileHelpers.processDirectory(archiveEntry.getWorkingDir(), true, false);
				} catch(Throwable t) {
					logger.fatal("{}: Failed to create the working directory '{}' for the archive entry of oat file "
							+ "'{}' of location '{}'.",t,cname,archiveEntry.getWorkingDir(),oatFilePath,location);
				}
			}
			
			for(Path dexFilePath : extractedDexFiles) {
				String fileName = com.google.common.io.Files.getNameWithoutExtension(dexFilePath.getFileName().toString());
				Matcher m = vdexExtractorOutputPattern.matcher(fileName);
				if(!m.matches()) {
					logger.fatal("{}: Unhandled extracted dex file path format for '{}' of '{}' at '{}'.",cname,fileName,vdexFilePath,location);
					return false;
				}
				String dexName = m.group(1).trim();
				if(dexName.isEmpty())
					dexName = null;
				else
					dexName = "classes" + dexName + ".dex";
				String entry = makeOatEntryString(firstEntry, dexName, oatLevel);
				
				VdexExtractorDexEntry curEntry = archiveEntry.addDexFileData(bootClassPath, bootDirForArch, entry, dexName, 
						apiLevel, oatLevel, oatFilePath, deps);
				try {
					Files.copy(dexFilePath, curEntry.getPathToDexFile());
				} catch(Throwable t) {
					logger.fatal("{}: Failed to copy the extracted dex file at '{}' to the path '{}' for oat file '{}' of "
							+ "location '{}'.",t,cname,dexFilePath,curEntry.getPathToDexFile(),oatFilePath,location);
					curEntry.setState(State.SETUPERR);
					return false;
				}
				curEntry.setState(State.SETUPSUCC);
			}
			
			if(FileHelpers.checkRWDirectoryExists(vdexExtractorOutputDir)){
				try{
					FileHelpers.removeDirectory(vdexExtractorOutputDir);
				}catch(Throwable t){
					logger.fatal("{}: Failed to completly remove directory containing the extracted dex files at '{}'.",
							t,cname,vdexExtractorOutputDir);
					return false;
				}
			}
		} catch(Throwable t) {
			logger.fatal("{}: Unexpected error. Failed to process the dex files extracted from '{}' of location '{}'."
					,t,cname,vdexFilePath,location);
			return false;
		}
		return true;
	}

	@Override
	protected State requiredStateBeforePostProcessing() {
		return State.SETUPSUCC;
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

}
