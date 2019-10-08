package org.sag.sje;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

public abstract class DexEntry {
	
	protected final String dexFileName;
	protected final int hashcode;
	
	protected Path pathToDexFile;
	protected Path pathToOatFile;
	protected String oatEntry;
	protected State state;
	protected Set<String> deps;
	
	protected DexEntry(String oatEntry, String dexFileName, Path pathToDexFile, Path pathToOatFile, List<String> deps, boolean skip){
		if(dexFileName == null)
			throw new IllegalArgumentException("Error: The dex file name must be given.");
		if(!skip && (pathToOatFile == null || pathToDexFile == null))
			throw new IllegalArgumentException("Error: The values can only be null (or negative) if skip is true.");
		this.dexFileName = dexFileName;
		this.hashcode = getHashcode();
		
		this.pathToDexFile = pathToDexFile;
		this.pathToOatFile = pathToOatFile;
		this.oatEntry = oatEntry;//null if skip, null or not null if not skip
		this.deps = deps == null ? ImmutableSet.<String>of() : ImmutableSet.<String>copyOf(deps);
		if(skip)
			this.state = State.SKIP;
		else
			this.state = State.START;
	}
	
	public boolean equals(Object o){
		if(this == o)
			return true;
		if(o == null || !(o instanceof DexEntry))
			return false;
		DexEntry other = (DexEntry)o;
		return Objects.equals(dexFileName, other.dexFileName);
	}
	
	private int getHashcode(){
		int ret = 17;
		ret = ret * 31 + Objects.hashCode(dexFileName);
		return ret;
	}
	
	public int hashCode(){
		return hashcode;
	}
	
	public String toString(String spacer){
		StringBuffer sb = new StringBuffer();
		sb.append(spacer).append("Dex Entry\n");
		sb.append(spacer).append("  Name: ").append(Objects.toString(getDexFileName())).append("\n");
		sb.append(spacer).append("  Type: ").append(Objects.toString(getTypeString())).append("\n");
		sb.append(spacer).append("  Skip: ").append(Objects.toString(skip())).append("\n");
		sb.append(spacer).append("  Oat Entry String: ").append(Objects.toString(getOatEntry())).append("\n");
		sb.append(spacer).append("  Dex File Output Path: ").append(Objects.toString(getPathToDexFile())).append("\n");
		sb.append(spacer).append("  Oat File Path: ").append(Objects.toString(getPathToOatFile())).append("\n");
		sb.append(spacer).append("  Api Version: ").append(Objects.toString(getApiVersion())).append("\n");
		sb.append(spacer).append("  Dependency List: ").append(Objects.toString(deps));
		return sb.toString();
	}
	
	public String toString(){
		return toString("");
	}
	
	public String getDexFileName() {
		return dexFileName;
	}
	
	public Path getPathToDexFile(){
		return pathToDexFile;
	}
	
	public Path getPathToOatFile(){
		return pathToOatFile;
	}
	
	public String getOatEntry() {
		return oatEntry;
	}
	
	public Set<String> getDeps() {
		return deps;
	}
	
	public void enableSkip(){
		this.state = State.SKIP;
		this.oatEntry = null;
		this.pathToOatFile = null;
		this.pathToDexFile = null;
	}
	
	public boolean skip(){
		return state.equals(State.SKIP);
	}
	
	public State getState(){
		return state;
	}
	
	public void setState(State state){
		if(state.equals(State.SKIP))
			throw new IllegalArgumentException("Error: Setting the state to skip can only be done through enableSkip().");
		this.state = state;
	}
	
	public boolean isErrorState(){
		return State.isErrorState(state);
	}

	public abstract int getApiVersion();
	protected abstract String getTypeString();
	
	public static enum State{
		START("Start"),
		SETUPSUCC("Setup_Successful"),SETUPERR("Setup_Error"),
		DECOMPSUCC("Decompile_Successful"),DECOMPERR("Decompile_Error"),
		RECOMPSUCC("Recompile_Successful"),RECOMPERR("Recompile_Error"),
		REPKGSUCC("Archive_Repackage_Successful"),REPKGERR("Archive_Repackage_Error"),
		SKIP("Skip");
		
		public String name;
		private State(String name){ this.name = name; }
		public String toString(){ return name; }
		public static boolean isErrorState(State state){
			return (state.equals(SETUPERR) || state.equals(DECOMPERR) || state.equals(RECOMPERR) || state.equals(REPKGERR));
		}
	}
	
}
