package org.sag.sje.smali;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.sag.sje.DexEntry;

public class SmaliDexEntry extends DexEntry {
	
	private String bootclasspath;
	private Path pathToBootClassPathFiles;
	private int apiVersion;
	private Path pathToSmaliDir;
	
	protected SmaliDexEntry(String bootclasspath, Path pathToBootClassPathFiles, String oatEntry, String dexFileName, 
			int apiVersion, Path pathToOatFile, Path pathToSmaliDir, Path pathToDexFile, List<String> deps, boolean skip){
		super(oatEntry,dexFileName,pathToDexFile,pathToOatFile,deps,skip);
		if(!skip && (bootclasspath == null || pathToBootClassPathFiles == null || apiVersion < 0 || pathToSmaliDir == null))
			throw new IllegalArgumentException("Error: The values can only be null (or negative) if skip is true.");
		
		this.bootclasspath = bootclasspath;
		this.pathToBootClassPathFiles = pathToBootClassPathFiles;
		this.apiVersion = apiVersion;
		this.pathToSmaliDir = pathToSmaliDir;
	}
	
	public boolean equals(Object o){
		if(super.equals(o)){
			return o instanceof SmaliDexEntry;
		}
		return false;
	}
	
	public String toString(String spacer){
		StringBuffer sb = new StringBuffer();
		sb.append(super.toString(spacer));
		sb.append(spacer).append("  Bootclasspath: ").append(Objects.toString(bootclasspath)).append("\n");
		sb.append(spacer).append("  Boot Dir: ").append(Objects.toString(pathToBootClassPathFiles)).append("\n");
		sb.append(spacer).append("  Smali Dir: ").append(Objects.toString(getPathToSmaliDir())).append("\n");
		return sb.toString();
	}
	
	public String getBootclasspath() {
		return bootclasspath;
	}
	
	public Path getPathToBootClassPathFiles() {
		return pathToBootClassPathFiles;
	}
	
	public int getApiVersion() {
		return apiVersion;
	}
	
	public Path getPathToSmaliDir(){
		return pathToSmaliDir;
	}
	
	public void enableSkip(){
		super.enableSkip();
		this.bootclasspath = null;
		this.pathToBootClassPathFiles = null;
		this.apiVersion = -1;
		this.pathToSmaliDir = null;
	}
	
	protected String getTypeString(){
		return "Smali";
	}
	
}
