package org.sag.sje.oatdump;

import java.nio.file.Path;

import org.sag.common.io.FileHelpers;
import org.sag.sje.ArchiveEntry;

public class OatdumpArchiveEntry extends ArchiveEntry<OatdumpDexEntry> {

	public OatdumpArchiveEntry(String name, String extension, String location, Path workingDir, Path rootPathToArchive) {
		super(name, extension, location, workingDir, rootPathToArchive);
	}
	
	public boolean equals(Object o){
		if(super.equals(o)){
			return o instanceof OatdumpArchiveEntry;
		}
		return false;
	}

	@Override
	protected OatdumpDexEntry makeNewSkipDexEntry(String dexFileName) {
		return new OatdumpDexEntry(null,dexFileName,-1,null,null,true);
	}
	
	public OatdumpDexEntry addDexFileData(String oatEntry, String dexFileName, int oatVer, Path pathToOatFile){
		if(pathToOatFile == null)
			throw new IllegalArgumentException("Error: A path to the oat/odex file containing the dex must be given.");
		if(oatVer < 0)
			throw new IllegalArgumentException("Error: The oat version must be non negative.");
		if(dexFileName == null)
			dexFileName = "classes.dex";
		if(dexEntries.containsKey(dexFileName))
			throw new RuntimeException("Error: The dex file '" + dexFileName + "' has already been added for the "
					+ "archive '" + name + extension + "' of /system/" + location + ".");
		Path pathToDexFile = FileHelpers.getPath(getWorkingDir(), dexFileName);
		OatdumpDexEntry ret = new OatdumpDexEntry(oatEntry,dexFileName,oatVer,
				pathToOatFile,pathToDexFile,false);
		dexEntries.put(dexFileName, ret);
		return ret;
	}

	@Override
	protected String getTypeString() {
		return "Oatdump";
	}

}
