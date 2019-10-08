package org.sag.sje.vdexextractor;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.sag.sje.DexEntry;

public class VdexExtractorDexEntry extends DexEntry {
	
	private int oatVer;
	private String bootclasspath;
	private Path pathToBootClassPathFiles;
	private int apiVersion;

	protected VdexExtractorDexEntry(String bootclasspath, Path pathToBootClassPathFiles, String oatEntry, String dexFileName, 
			int apiVersion, int oatVer, Path pathToOatFile, Path pathToDexFile, List<String> deps, boolean skip) {
		super(oatEntry,dexFileName,pathToDexFile,pathToOatFile,deps,skip);
		if(!skip && (bootclasspath == null || pathToBootClassPathFiles == null || apiVersion < 0 || oatVer < 0))
			throw new IllegalArgumentException("Error: The values can only be null (or negative) if skip is true.");
		
		this.oatVer = oatVer;
		this.bootclasspath = bootclasspath;
		this.pathToBootClassPathFiles = pathToBootClassPathFiles;
		this.apiVersion = apiVersion;
	}
	
	public boolean equals(Object o){
		if(super.equals(o)){
			return o instanceof VdexExtractorDexEntry;
		}
		return false;
	}
	
	public String toString(String spacer){
		StringBuffer sb = new StringBuffer();
		sb.append(super.toString(spacer));
		sb.append(spacer).append("  Oat Version: ").append(Objects.toString(getOatVersion())).append("\n");
		sb.append(spacer).append("  Bootclasspath: ").append(Objects.toString(bootclasspath)).append("\n");
		sb.append(spacer).append("  Boot Dir: ").append(Objects.toString(pathToBootClassPathFiles)).append("\n");
		return sb.toString();
	}

	@Override
	public void enableSkip() {
		super.enableSkip();
		this.oatVer = -1;
		this.bootclasspath = null;
		this.pathToBootClassPathFiles = null;
		this.apiVersion = -1;
	}
	
	public String getBootclasspath() {
		return bootclasspath;
	}
	
	public Path getPathToBootClassPathFiles() {
		return pathToBootClassPathFiles;
	}
	
	public int getOatVersion() {
		return this.oatVer;
	}

	@Override
	public int getApiVersion() {
		return apiVersion;
	}
	
	protected String getTypeString(){
		return "vdexExtractor";
	}

}
