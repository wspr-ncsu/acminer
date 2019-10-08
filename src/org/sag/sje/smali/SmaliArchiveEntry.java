package org.sag.sje.smali;

import java.nio.file.Path;
import java.util.List;

import org.sag.common.io.FileHelpers;
import org.sag.sje.ArchiveEntry;

public class SmaliArchiveEntry extends ArchiveEntry<SmaliDexEntry> {
	
	public SmaliArchiveEntry(String name, String extension, String location, Path workingDir, Path rootPathToArchive){
		super(name,extension,location,workingDir,rootPathToArchive);
	}
	
	public boolean equals(Object o){
		if(super.equals(o)){
			return o instanceof SmaliArchiveEntry;
		}
		return false;
	}
	
	public void addDexFileData(String bootclasspath, Path pathToBootClassPathFiles, int api, Path pathToFile){
		String dexFileName;
		if(dexEntries.isEmpty()){
			dexFileName = null;
		}else{
			int i = dexEntries.size() + 1;
			dexFileName = "classes" + i + ".dex";
		}
		addDexFileData(bootclasspath,pathToBootClassPathFiles,null,dexFileName,api,pathToFile,null);
	}
	
	public void addDexFileData(String bootclasspath, Path pathToBootClassPathFiles, String oatEntry, String dexFileName, 
			int api, Path pathToFile, List<String> deps){
		if(bootclasspath == null || pathToBootClassPathFiles == null)
			throw new IllegalArgumentException("Error: Both the bootclasspath and pathToBootClassPathFiles cannot be null.");
		if(pathToFile == null)
			throw new IllegalArgumentException("Error: A path to the oat/odex file containing the dex must be given.");
		if(api < 0)
			throw new IllegalArgumentException("Error: The api level must be non negative.");
		if(dexFileName == null)
			dexFileName = "classes.dex";
		if(dexEntries.containsKey(dexFileName))
			throw new RuntimeException("Error: The dex file '" + dexFileName + "' has already been added for the "
					+ "archive '" + name + extension + "' of /system/" + location + ".");
		Path pathToSmaliDir = FileHelpers.getPath(getWorkingDir(), dexFileName.replace('.', '_') + "_smali");
		Path pathToDexFile = FileHelpers.getPath(getWorkingDir(), dexFileName);
		dexEntries.put(dexFileName, new SmaliDexEntry(bootclasspath,pathToBootClassPathFiles,oatEntry,dexFileName,api,
				pathToFile,pathToSmaliDir,pathToDexFile,deps,false));
	}
	
	protected SmaliDexEntry makeNewSkipDexEntry(String dexFileName){
		return new SmaliDexEntry(null,null,null,dexFileName,-1,null,null,null,null,true);
	}
	
	protected String getTypeString(){
		return "Smali";
	}
	
}
