package org.sag.sje;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.sag.common.io.FileHelpers;
import org.sag.common.tools.SortingMethods;

import com.google.common.collect.ImmutableSet;

public abstract class ArchiveEntry<T extends DexEntry> {
	
	protected final String name;
	protected final String extension;
	protected final String location;
	protected final Path workingDir;
	protected final Map<String,T> dexEntries;
	protected final int hashcode;
	
	protected Path rootPathToArchive;
	protected Path pathToArchive;
	
	public ArchiveEntry(String name, String extension, String location, Path workingDir, Path rootPathToArchive){
		if(name == null || extension == null || location == null || workingDir == null || rootPathToArchive == null)
			throw new IllegalArgumentException("Error: All arguments must be non null.");
		this.name = name;
		this.extension = extension;
		this.location = location;
		this.workingDir = workingDir;
		this.rootPathToArchive = rootPathToArchive;
		this.dexEntries = new LinkedHashMap<>();
		this.hashcode = getHashcode();
		this.pathToArchive = null;
	}
	
	public boolean equals(Object o){
		if(this == o)
			return true;
		if(o == null || !(o instanceof ArchiveEntry))
			return false;
		@SuppressWarnings("rawtypes")
		ArchiveEntry other = (ArchiveEntry)o;
		return Objects.equals(name, other.name) && Objects.equals(extension, other.extension) && 
				Objects.equals(location, other.location);
	}
	
	private int getHashcode(){
		int ret = 17;
		ret = ret * 31 + Objects.hashCode(name);
		ret = ret * 31 + Objects.hashCode(extension);
		ret = ret * 31 + Objects.hashCode(location);
		return ret;
	}
	
	public int hashCode(){
		return hashcode;
	}
	
	public String toString(String spacer){
		StringBuffer sb = new StringBuffer();
		sb.append(spacer).append("Archive Entry\n");
		sb.append(spacer).append("  Name: ").append(Objects.toString(getName())).append("\n");
		sb.append(spacer).append("  Extension: ").append(Objects.toString(getExtension())).append("\n");
		sb.append(spacer).append("  Location: ").append(Objects.toString(getLocation())).append("\n");
		sb.append(spacer).append("  Type: ").append(Objects.toString(getTypeString())).append("\n");
		sb.append(spacer).append("  Working Dir: ").append(Objects.toString(getWorkingDir())).append("\n");
		sb.append(spacer).append("  Root Archive Dir: ").append(Objects.toString(getRootPathToArchive())).append("\n");
		sb.append(spacer).append("  Archive File Output Path: ").append(Objects.toString(getPathToArchive())).append("\n");
		sb.append(spacer).append("  Skip: ").append(Objects.toString(skip())).append("\n");
		sb.append(spacer).append("  Dex Files: \n");
		for(T dexEntry : getDexFiles()){
			sb.append(dexEntry.toString(spacer + "    "));
		}
		return sb.toString();
	}
	
	public String toString(){
		return toString("");
	}
	
	public String getName() {
		return name;
	}

	public String getExtension() {
		return extension;
	}

	public String getLocation() {
		return location;
	}
	
	public Path getWorkingDir() {
		return workingDir;
	}
	
	public Path getRootPathToArchive(){
		return rootPathToArchive;
	}
	
	public void setRootPathToArchive(Path rootPathToArchive){
		this.rootPathToArchive = rootPathToArchive;
		this.pathToArchive = null;
	}
	
	public List<T> getDexFiles() {
		return new ArrayList<>(this.dexEntries.values());
	}
	
	public boolean hasDexFiles(){
		return !this.dexEntries.isEmpty();
	}
	
	public Path getPathToArchive(){
		if(this.pathToArchive == null){
			this.pathToArchive = FileHelpers.getPath(getRootPathToArchive(), getLocation(), getName() + "." + getExtension());
		}
		return this.pathToArchive;
	}
	
	public boolean skip(){
		for(T dexEntry : dexEntries.values()){
			if(!dexEntry.skip())
				return false;
		}
		return true;
	}
	
	public boolean allSkipOrErrorState(){
		for(T dexEntry : dexEntries.values()){
			if(!dexEntry.skip() && !dexEntry.isErrorState())
				return false;
		}
		return true;
	}
	
	public boolean allErrorState(){
		for(T dexEntry : dexEntries.values()){
			if(!dexEntry.isErrorState())
				return false;
		}
		return true;
	}
	
	public void markDexEntryAsSkip(String dexFileName){
		if(dexFileName == null)
			dexFileName = "classes.dex";
		T dexEntry = dexEntries.get(dexFileName);
		if(dexEntry != null){
			dexEntry.enableSkip();
		}else{
			dexEntries.put(dexFileName, makeNewSkipDexEntry(dexFileName));
		}
	}
	
	public int getApiVersion() {
		int ret = -1;
		for(T d : dexEntries.values()){
			ret = Math.max(ret,d.getApiVersion());
		}
		return ret;
	}
	
	public Set<String> getDeps() {
		if(dexEntries.isEmpty()) {
			return ImmutableSet.<String>of();
		} else if(dexEntries.size() == 1) {
			return ImmutableSet.<String>copyOf(dexEntries.values().iterator().next().getDeps());
		} else {
			Set<String> ret = new LinkedHashSet<>();
			for(T dexEntry : dexEntries.values()) {
				ret.addAll(dexEntry.getDeps());
			}
			if(ret.isEmpty()) 
				return ImmutableSet.<String>of();
			return ret;
		}
	}
	
	protected abstract T makeNewSkipDexEntry(String dexFileName);
	protected abstract String getTypeString();
	
	public static Comparator<ArchiveEntry<? extends DexEntry>> simpleComp = new Comparator<ArchiveEntry<? extends DexEntry>>(){
		@Override
		public int compare(ArchiveEntry<? extends DexEntry> o1, ArchiveEntry<? extends DexEntry> o2) {
			return SortingMethods.sComp.compare(o1.getLocation()+"/"+o1.getName(), o2.getLocation()+"/"+o2.getName());
		}
	};
	
}
