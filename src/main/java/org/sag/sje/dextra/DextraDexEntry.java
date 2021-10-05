package org.sag.sje.dextra;

import java.nio.file.Path;
import java.util.Objects;

import org.sag.sje.VersionMap;
import org.sag.sje.DexEntry;

public class DextraDexEntry extends DexEntry {
	
	private int oatVer;

	protected DextraDexEntry(String oatEntry, String dexFileName, int oatVer, Path pathToOatFile, Path pathToDexFile, boolean skip) {
		//TODO add deps support
		super(oatEntry,dexFileName,pathToDexFile,pathToOatFile,null,skip);
		this.oatVer = oatVer;
	}
	
	public boolean equals(Object o){
		if(super.equals(o)){
			return o instanceof DextraDexEntry;
		}
		return false;
	}
	
	public String toString(String spacer){
		StringBuffer sb = new StringBuffer();
		sb.append(super.toString(spacer));
		sb.append(spacer).append("  Bootclasspath: ").append(Objects.toString(getOatVersion())).append("\n");
		return sb.toString();
	}

	@Override
	public void enableSkip() {
		super.enableSkip();
		this.oatVer = -1;
	}
	
	public int getOatVersion() {
		return this.oatVer;
	}

	@Override
	public int getApiVersion() {
		if(this.oatVer == -1)
			return -1;
		return VersionMap.mapArtVersionToApi(oatVer);
	}
	
	protected String getTypeString(){
		return "Dextra";
	}

}
