package org.sag.sje.oatdump;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.sag.common.io.FileHelpers;
import org.sag.common.logging.ILogger;
import org.sag.common.tools.SortingMethods;
import org.sag.sje.DexEntry.State;
import org.sag.sje.DexExtractor;

import com.google.common.io.ByteStreams;

public class OatdumpDexExtractor extends DexExtractor<OatdumpArchiveEntry> {
	
	private Path pathToWorkingOatdumpDir;
	private Path pathToOatdump;
	private int osid;

	public OatdumpDexExtractor(String bootClassPath, Path pathToInputDir, Path pathToSystemImgZipFile, Path pathToSystemArchivesZipFile, 
			Path pathToWorkingDir, Path pathToAndroidInfoFile, String[] locations, String[] archs, int defaultApi, ILogger logger) {
		super(bootClassPath, pathToInputDir, pathToSystemImgZipFile, pathToSystemArchivesZipFile, pathToWorkingDir, pathToAndroidInfoFile, 
				locations, archs, defaultApi, logger);
		this.pathToWorkingOatdumpDir = FileHelpers.getPath(pathToWorkingDir,"oatdump");
		
		//this.pathToOatdump = FileHelpers.getPath(pathToWorkingDir, "bin", "oatdump");
		this.osid = getOS();
	}

	@Override
	protected boolean runInner() {
		return true;
	}
	
	@Override
	protected boolean customPreProcess() {
		if(osid == 0) {
			logger.fatal("{}: Unsupported operating system.",cname);
			return false;
		}
		return extractOatdumpToWorkingDir();
	}

	@Override
	protected OatdumpArchiveEntry getNewArchiveEntry(String name, String extension, String location, Path workingDir, Path rootPathToArchive) {
		return new OatdumpArchiveEntry(name, extension, location, workingDir, rootPathToArchive);
	}
	
	private boolean extractOatdumpToWorkingDir(){
		String versionDir = null;
		if(defaultApi == 28) {
			versionDir = "android_9.0.0";
		} else {
			logger.fatal("{}: Unsupported API version '{}'. No oatdump tool for this API version.",cname,defaultApi);
			return false;
		}
		
		try {
			FileHelpers.processDirectory(pathToWorkingOatdumpDir, true, false);
		} catch(Throwable t) {
			logger.fatal("{}: Failed to create the working directory for oatdump '{}'.",t,cname,pathToWorkingOatdumpDir);
			return false;
		}
		
		Path pathsFile = FileHelpers.getPath(pathToWorkingOatdumpDir, "paths.txt");
		try {
			try(InputStream in = getClass().getResourceAsStream("/oatdump/" + versionDir + "/paths.txt")) {
				try(OutputStream out = Files.newOutputStream(pathsFile)) {
					ByteStreams.copy(in, out);
				}
			}
		} catch(Throwable t) {
			logger.fatal("{}: Failed to extract the paths.txt file for oatdump to the working directory at '{}'",t,cname,pathsFile);
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
			logger.fatal("{}: Failed to parse the paths.txt file for oatdump to the working directory at '{}'",t,cname,pathsFile);
			return false;
		}
		
		
		for(String dir : dirs) {
			Path path = FileHelpers.getPath(pathToWorkingOatdumpDir, dir);
			try {
				FileHelpers.processDirectory(path, true, false);
			} catch(Throwable t) {
				logger.fatal("{}: Failed to create a working directory for oatdump '{}'.",t,cname,path);
				return false;
			}	
		}
		
		for(String file : files) {
			Path path = FileHelpers.getPath(pathToWorkingOatdumpDir, file);
			try {
				try(InputStream in = getClass().getResourceAsStream("/oatdump/" + versionDir + "/" + file)) {
					try(OutputStream out = Files.newOutputStream(path)) {
						ByteStreams.copy(in, out);
					}
				}
				if(path.getFileName().toString().equals("oatdump")) {
					path.toFile().setExecutable(true);
					this.pathToOatdump = path;
				}
			} catch(Throwable t) {
				logger.fatal("{}: Failed to extract a oatdump file to the working directory at '{}'",t,cname,path);
				return false;
			}
		}
		
		return true;
	}
	
	private int getOS() {
		String os = System.getProperty("os.name").toLowerCase();
		if(os.startsWith("win")) {
			return 1;
		} else if(os.contains("linux")) {
			return 2;
		} else {
			return 0;
		}
	}
	
	private String getWSLPath(Path path) {
		String ret = path.toString().replaceAll("\\\\", "\\\\\\\\");
		try {
			ProcessBuilder pb = new ProcessBuilder("wsl", "wslpath", "-a", ret);
			Process p = pb.start();
			
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while((line = br.readLine()) != null) {
				ret = line.trim();
			}
			
			int r = p.waitFor();
			if(r != 0 || ret.isEmpty() || !ret.startsWith("/")){
				logger.fatal("{}: Failed to translate path '{}' to a wsl path.",cname,path);
				return null;
			}
		} catch(Throwable t) {
			logger.fatal("{}: Failed to translate path '{}' to a wsl path.",t,cname,path);
			return null;
		}
		return ret;
	}
	
	private Path wslToWindowsPath(String path) {
		String ret = path;
		try {
			ProcessBuilder pb = new ProcessBuilder("wsl", "wslpath", "-w", ret);
			Process p = pb.start();
			
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while((line = br.readLine()) != null) {
				ret = line.trim();
			}
			
			int r = p.waitFor();
			if(r != 0 || ret.isEmpty()){
				logger.fatal("{}: Failed to translate path '{}' to a windows path.",cname,path);
				return null;
			}
		} catch(Throwable t) {
			logger.fatal("{}: Failed to translate path '{}' to a windows path.",t,cname,path);
			return null;
		}
		return FileHelpers.getPath(ret);
	}

	@Override
	protected boolean parseOatFile(String location, Path oatFilePath, Path workingDir, Path archiveDir, String arch, Path bootDirForArch) {
		List<String> commands = new ArrayList<>();
		if(osid == 1) { //windows
			// Test to make sure we can execute wsl
			try {
				ProcessBuilder pb = new ProcessBuilder("wsl", "true");
				Process p = pb.start();
				int r = p.waitFor();
				if(r != 0) {
					logger.fatal("{}: Did not successfully execute the command 'wsl true'. Does wsl not exist?",cname);
					return false;
				}
			} catch(Throwable t) {
				logger.fatal("{}: Failed to execute the command 'wsl true'.",t,cname);
				return false;
			}
			
			// wsl oatdump --oat-file="path" --header-only --export-dex-to="path"
			commands.add("wsl");
			commands.add(getWSLPath(pathToOatdump));
			commands.add("--oat-file=\"" + getWSLPath(oatFilePath) + "\"");
			commands.add("--header-only");
			commands.add("--export-dex-to=\"" + getWSLPath(pathToWorkingOatdumpDir) + "\"");
		} else {
			// oatdump --oat-file="path" --header-only --export-dex-to="path"
			commands.add(pathToOatdump.toString());
			commands.add("--oat-file=\"" + oatFilePath.toString() + "\"");
			commands.add("--header-only");
			commands.add("--export-dex-to=\"" + pathToWorkingOatdumpDir.toString() + "\"");
		}
		
		try {
			ProcessBuilder pb = new ProcessBuilder(commands.toArray(new String[0]));
			pb.directory(pathToWorkingOatdumpDir.toFile());
			Process p = pb.start();
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			
			String oatVerStr = null;
			String numOfDexFilesStr = null;
			Set<String> dexFiles = new LinkedHashSet<>();
			
			String line;
			while((line = br.readLine()) != null) {
				line = line.trim();
				if(line.equals("MAGIC:")) {
					line = br.readLine();
					if(line != null && line.trim().equals("oat")) {
						line = br.readLine();
						if(line != null && (line = line.trim()).matches("\\d+"))
							oatVerStr = line;
					}
				} else if(line.equals("DEX FILE COUNT:")) {
					line = br.readLine();
					if(line != null && (line = line.trim()).matches("\\d+"))
						numOfDexFilesStr = line;
				} else if(line.startsWith("Dex file exported at ")) {
					line = line.replaceFirst("Dex file exported at ", "");
					int sizeStart = line.lastIndexOf('(');
					if(sizeStart > 0 && !(line = line.substring(0, sizeStart).trim()).isEmpty())
						dexFiles.add(line);
				}
			}
			int r = p.waitFor(); 
			if(r != 0) {
				logger.fatal("{}: Oatdump did not terminate normally when extracting the dex files from oat file '{}' of "
						+ "location '{}'.",cname,oatFilePath,location);
				return false;
			}
			
			int oatVer = 0;
			if(oatVerStr == null) {
				logger.fatal("{}: Failed to extract an oat file version from the Oatdump output of oat file '{}' of location '{}'."
						,cname,oatFilePath,location);
				return false;
			}
			try {
				oatVer = Integer.parseInt(oatVerStr);
			} catch(Throwable t) {
				logger.fatal("{}: Failed to parse the oat file version string to an integer for oat file '{}' of location '{}'.",
						t,cname,oatFilePath,location);
				return false;
			}
			
			int numOfDexFiles = 0;
			if(numOfDexFilesStr == null) {
				logger.fatal("{}: Failed to extract the number of dex files from the Oatdump output of oat file '{}' of location '{}'."
						,cname,oatFilePath,location);
				return false;
			}
			try {
				numOfDexFiles = Integer.parseInt(numOfDexFilesStr);
			} catch(Throwable t) {
				logger.fatal("{}: Failed to parse the number of dex files string to an integer for oat file '{}' of location '{}'.",
						t,cname,oatFilePath,location);
				return false;
			}
			
			if(numOfDexFiles != dexFiles.size()) {
				logger.fatal("{}: The number of dex entries does not match the number of extracted dex files for oat file '{}' "
						+ "of location '{}'.",cname,oatFilePath,location);
				return false;
			}
			
			for(String dexFile : dexFiles) {
				Path dexFilePath;
				if(osid == 1) {
					dexFilePath = wslToWindowsPath(dexFile);
				} else {
					dexFilePath = FileHelpers.getPath(dexFile);
				}
				
				if(!FileHelpers.checkRWFileExists(dexFilePath)) {
					logger.fatal("{}: The extracted dex file could not be located at path '{}' for oat file '{}' of location '{}'.",
							cname,dexFilePath,oatFilePath,location);
					return false;
				}
				
				String dexEntry = dexFilePath.getFileName().toString().replaceFirst("_export\\.dex$", "");
				String[] parts = parseOatEntryString(dexEntry,oatVer);
				String archiveName = parts[0];
				String archiveExtension = parts[1];
				String dexName = parts[2];
				String key = makeArchiveEntriesKey(location,archiveName,archiveExtension);
				OatdumpArchiveEntry archiveEntry = archiveEntries.get(key);
				if(archiveEntry == null) {
					archiveEntry = new OatdumpArchiveEntry(archiveName,archiveExtension,location,
							FileHelpers.getPath(workingDir, archiveName),archiveDir);
					archiveEntries.put(key, archiveEntry);
					try {
						FileHelpers.processDirectory(archiveEntry.getWorkingDir(), true, false);
					} catch(Throwable t) {
						logger.fatal("{}: Failed to create the working directory '{}' for the archive entry of oat file "
								+ "'{}' of location '{}'.",t,cname,archiveEntry.getWorkingDir(),oatFilePath,location);
					}
				}
				OatdumpDexEntry curEntry = archiveEntry.addDexFileData(dexEntry, dexName, oatVer, oatFilePath);
				
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
		} catch(Throwable t) {
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
	protected Set<Path> sortOatPathsByDependency(Set<Path> paths) throws Exception {
		//TODO sort by boot class path and what not 
		return SortingMethods.sortSet(paths);
	}

}
