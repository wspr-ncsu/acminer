package org.sag.gjfr;

import java.io.ObjectStreamException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.sag.common.io.FileHelpers;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("PossibleEntry")
public class PossibleEntry implements Comparable<PossibleEntry> {

	private String filepath;
	private String jarjarpath;
	private String orgclasspath;
	
	public PossibleEntry(String filepath) {
		this.filepath = filepath;
		this.jarjarpath = null;
		this.orgclasspath = null;
	}
	
	public PossibleEntry(String filepath, String jarjarpath, String orgclasspath) {
		this.filepath = filepath;
		this.jarjarpath = jarjarpath;
		this.orgclasspath = orgclasspath;
	}
	
	public String getFilePath() {
		return filepath;
	}
	
	public Path getFilePath2() {
		return FileHelpers.getPath(filepath);
	}
	
	public String getJarJarPath() {
		return jarjarpath;
	}
	
	public Path getJarJarPath2() {
		return FileHelpers.getPath(jarjarpath);
	}
	
	public String getOrgClassPath() {
		return orgclasspath;
	}
	
	//ReadResolve is always run when reading from XML even if a constructor is run first
	protected Object readResolve() throws ObjectStreamException {
		if(filepath != null && filepath.isEmpty())
			filepath = null;
		if(jarjarpath != null && jarjarpath.isEmpty())
			jarjarpath = null;
		if(orgclasspath != null && orgclasspath.isEmpty())
			orgclasspath = null;
		return this;
	}
	
	protected Object writeReplace() throws ObjectStreamException {
		return this;
	}
	
	@Override
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof PossibleEntry))
			return false;
		PossibleEntry other = (PossibleEntry)o;
		return Objects.equals(this.filepath, other.filepath) 
				&& Objects.equals(this.jarjarpath, other.jarjarpath)
				&& Objects.equals(this.orgclasspath, other.orgclasspath);
	}
	
	@Override
	public int hashCode() {
		int i = 17;
		i = i * 31 + Objects.hashCode(filepath);
		i = i * 31 + Objects.hashCode(jarjarpath);
		i = i * 31 + Objects.hashCode(orgclasspath);
		return i;
	}
	
	@Override
	public String toString() {
		return toString("");
	}
	
	public String toString(String spacer) {
		StringBuilder sb = new StringBuilder();
		sb.append(spacer).append("FilePath: ").append(Objects.toString(filepath));
		if(jarjarpath != null)
			sb.append(" ").append("JarJarPath: ").append(Objects.toString(jarjarpath));
		if(orgclasspath != null)
			sb.append(" ").append("OrgClassPath: ").append(Objects.toString(orgclasspath));
		sb.append("\n");
		return sb.toString();
	}

	@Override
	public int compareTo(PossibleEntry o) {
		int ret = Paths.get(filepath).compareTo(Paths.get(o.filepath));
		if(ret == 0) {
			ret = Paths.get(jarjarpath).compareTo(Paths.get(o.jarjarpath));
			if(ret == 0)
				ret = orgclasspath.compareToIgnoreCase(o.orgclasspath);
		}
		return ret;
	}
	
}
