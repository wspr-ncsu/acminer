package org.sag.sje.dextra;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
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
import org.sag.sje.DexEntry.State;
import org.sag.sje.DexExtractor;

import com.google.common.io.ByteStreams;

public class DextraDexExtractor extends DexExtractor<DextraArchiveEntry> {
	
	private static final Pattern oatVerPattern = Pattern.compile(".*OAT\\s+File\\s+\\((\\d+)\\).*",Pattern.CASE_INSENSITIVE);
	private static final Pattern dexEntryPattern = Pattern.compile(".*Dex\\s+Header\\s+.*:\\s+(\\/system\\/[^\\s]+)(?:\\s+.*$|$)",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern dexFileWrittenPattern = Pattern.compile(".*Written\\s+to\\s+(system@[^\\s]+)(?:\\s+.*$|$)",
			Pattern.CASE_INSENSITIVE);
	
	private Path pathToWorkingDextraDir;
	private Path pathToDextra;

	public DextraDexExtractor(String bootClassPath, Path pathToInputDir, Path pathToSystemImgZipFile, Path pathToSystemArchivesZipFile, 
			Path pathToWorkingDir, Path pathToAndroidInfoFile, String[] locations, String[] archs, String[] apexLocations, int defaultApi, ILogger logger) {
		super(bootClassPath, pathToInputDir, pathToSystemImgZipFile, pathToSystemArchivesZipFile, pathToWorkingDir, pathToAndroidInfoFile, 
				locations, archs, apexLocations, defaultApi, logger);
		this.pathToWorkingDextraDir = FileHelpers.getPath(pathToWorkingDir,"dextra");
		this.pathToDextra = FileHelpers.getPath(pathToWorkingDextraDir, "dextra");
	}

	@Override
	protected boolean runInner() {
		return true;
	}
	
	@Override
	protected boolean customPreProcess(){
		return extractDextraToWorkingDir();
	}

	@Override
	protected DextraArchiveEntry getNewArchiveEntry(String name, String extension, String location, Path workingDir, Path rootPathToArchive) {
		return new DextraArchiveEntry(name, extension, location, workingDir, rootPathToArchive);
	}
	
	private boolean extractDextraToWorkingDir(){
		try{
			FileHelpers.processDirectory(pathToWorkingDextraDir, true, false);
		}catch(Throwable t){
			logger.fatal("{}: Failed to create the working directory for the dextra tool.",t,cname);
			return false;
		}
		try{
			try(InputStream in = getClass().getResourceAsStream("/dextra/dextra.ELF64")){
				try(OutputStream out = Files.newOutputStream(pathToDextra)){
					ByteStreams.copy(in, out);
				}
			}
			pathToDextra.toFile().setExecutable(true);
		}catch(Throwable t){
			logger.fatal("{}: Failed to extract the dextra executable to the working directory.",t,cname);
			return false;
		}
		return true;
	}

	@Override
	protected boolean parseOatFile(String location, Path oatFilePath, Path workingDir, Path archiveDir, String arch, Path bootDirForArch) {
		try{
			ProcessBuilder pb = new ProcessBuilder(pathToDextra.toString(),"-dextract",oatFilePath.toString());
			pb.directory(pathToWorkingDextraDir.toFile());
			Process p = pb.start();
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			
			String oatVerStr = null;
			Set<String> dexEntries = new LinkedHashSet<>();
			Set<String> dexFiles = new LinkedHashSet<>();
			
			String line;
			while((line = br.readLine()) != null){
				line = line.trim();
				Matcher oatVerM = oatVerPattern.matcher(line);
				Matcher dexEntryM = dexEntryPattern.matcher(line);
				Matcher dexFileM = dexFileWrittenPattern.matcher(line);
				if(oatVerM.matches()){
					oatVerStr = oatVerM.group(1);
				}else if(dexEntryM.matches()){
					dexEntries.add(dexEntryM.group(1));
				}else if(dexFileM.matches()){
					dexFiles.add(dexFileM.group(1));
				}
			}
			int r = p.waitFor(); 
			if(r != 0){
				logger.fatal("{}: Dextra did not terminate normally when extracting the dex files from oat file '{}' of "
						+ "location '{}'.",cname,oatFilePath,location);
				return false;
			}
			
			int oatVer = 0;
			
			if(oatVerStr == null){
				logger.fatal("{}: Failed to extract an oat file version from the Dextra output of oat file '{}' of location '{}'."
						,cname,oatFilePath,location);
				return false;
			}
			try{
				oatVer = Integer.parseInt(oatVerStr);
			}catch(Throwable t){
				logger.fatal("{}: Failed to parse the oat file version string to an integer for oat file '{}' of location '{}'.",
						t,cname,oatFilePath,location);
				return false;
			}
			if(dexEntries.size() != dexFiles.size()){
				logger.fatal("{}: The number of dex entries does not match the number of extracted dex files for oat file '{}' "
						+ "of location '{}'.",cname,oatFilePath,location);
				return false;
			}
			Iterator<String> it = dexEntries.iterator();
			for(String dexFile : dexFiles){
				String dexEntry = it.next();
				Path path = FileHelpers.getPath(pathToWorkingDextraDir,dexFile);
				if(!FileHelpers.checkRWFileExists(path)){
					logger.fatal("{}: The extracted dex file could not be located at path '{}' for oat file '{}' of location '{}'.",
							cname,path,oatFilePath,location);
					return false;
				}
				
				String[] parts = parseOatEntryString(dexEntry,oatVer);
				String archiveName = parts[0];
				String archiveExtension = parts[1];
				String dexName = parts[2];
				String key = makeArchiveEntriesKey(location,archiveName,archiveExtension);
				DextraArchiveEntry archiveEntry = archiveEntries.get(key);
				if(archiveEntry == null){
					archiveEntry = new DextraArchiveEntry(archiveName,archiveExtension,location,
							FileHelpers.getPath(workingDir, archiveName),archiveDir);
					archiveEntries.put(key, archiveEntry);
					try{
						FileHelpers.processDirectory(archiveEntry.getWorkingDir(), true, false);
					}catch(Throwable t){
						logger.fatal("{}: Failed to creat the working directory '{}' for the archive entry of oat file "
								+ "'{}' of location '{}'.",t,cname,archiveEntry.getWorkingDir(),oatFilePath,location);
					}
				}
				DextraDexEntry curEntry = archiveEntry.addDexFileData(dexEntry, dexName, oatVer, oatFilePath);
				
				try{
					Files.copy(path, curEntry.getPathToDexFile());
				}catch(Throwable t){
					logger.fatal("{}: Failed to copy the extracted dex file at '{}' to the path '{}' for oat file '{}' of "
							+ "location '{}'.",t,cname,path,curEntry.getPathToDexFile(),oatFilePath,location);
					curEntry.setState(State.SETUPERR);
					return false;
				}
				curEntry.setState(State.SETUPSUCC);
			}
		}catch(Throwable t){
			logger.fatal("{}: Unexpected error. Failed to parse the oat file '{}' of location '{}'."
					,t,cname,oatFilePath,location);
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
