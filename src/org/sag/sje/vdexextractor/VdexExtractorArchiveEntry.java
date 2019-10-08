package org.sag.sje.vdexextractor;

import java.nio.file.Path;
import java.util.List;

import org.sag.common.io.FileHelpers;
import org.sag.sje.ArchiveEntry;

public class VdexExtractorArchiveEntry extends ArchiveEntry<VdexExtractorDexEntry> {

	public VdexExtractorArchiveEntry(String name, String extension, String location, Path workingDir, Path rootPathToArchive) {
		super(name, extension, location, workingDir, rootPathToArchive);
	}
	
	public boolean equals(Object o){
		if(super.equals(o)){
			return o instanceof VdexExtractorArchiveEntry;
		}
		return false;
	}

	@Override
	protected VdexExtractorDexEntry makeNewSkipDexEntry(String dexFileName) {
		return new VdexExtractorDexEntry(null,null,null,dexFileName,-1,-1,null,null,null,true);
	}
	
	public VdexExtractorDexEntry addDexFileData(String bootclasspath, Path pathToBootClassPathFiles, String oatEntry, 
			String dexFileName, int api, int oatVer, Path pathToOatFile, List<String> deps){
		if(bootclasspath == null || pathToBootClassPathFiles == null)
			throw new IllegalArgumentException("Error: Both the bootclasspath and pathToBootClassPathFiles cannot be null.");
		if(pathToOatFile == null)
			throw new IllegalArgumentException("Error: A path to the oat/odex file containing the dex must be given.");
		if(api < 0)
			throw new IllegalArgumentException("Error: The api level must be non negative.");
		if(oatVer < 0)
			throw new IllegalArgumentException("Error: The oat version must be non negative.");
		if(dexFileName == null)
			dexFileName = "classes.dex";
		if(dexEntries.containsKey(dexFileName))
			throw new RuntimeException("Error: The dex file '" + dexFileName + "' has already been added for the "
					+ "archive '" + name + extension + "' of /system/" + location + ".");
		Path pathToDexFile = FileHelpers.getPath(getWorkingDir(), dexFileName);
		VdexExtractorDexEntry ret = new VdexExtractorDexEntry(bootclasspath,pathToBootClassPathFiles,oatEntry,dexFileName,api,
				oatVer,pathToOatFile,pathToDexFile,deps,false);
		dexEntries.put(dexFileName, ret);
		return ret;
	}

	@Override
	protected String getTypeString() {
		return "vdexExtractor";
	}

}
