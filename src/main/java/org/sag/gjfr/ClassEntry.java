package org.sag.gjfr;

import java.io.ObjectStreamException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.sag.common.io.FileHelpers;
import org.sag.common.tools.SortingMethods;
import org.sag.common.xstream.XStreamInOut;
import org.sag.common.xstream.XStreamInOut.XStreamInOutInterface;
import org.sag.gjfr.RulesFileParser.Remapper;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("ClassEntry")
public class ClassEntry implements Comparable<ClassEntry> {
	
	private String className;
	private String classPath;
	private String fullClassPath;
	private String oldClassPath;
	private String jarjarRulesPath;
	private String selectedPath;
	private LinkedHashSet<PossibleEntry> possiblePaths;
	
	public ClassEntry(String className, String classPath) {
		this.className = className;
		this.classPath = classPath;
		this.fullClassPath = classPath == null ? className : classPath + "." + className;
		this.possiblePaths = new LinkedHashSet<>();
		this.selectedPath = null;
		this.jarjarRulesPath = null;
		this.oldClassPath = null;
	}
	
	public String getClassName() {
		return className;
	}
	
	public String getClassPath() {
		return classPath;
	}
	
	public String getFullClassPath() {
		return fullClassPath;
	}
	
	public String getOldClassPath() {
		return oldClassPath;
	}
	
	public String getOldFullClassPath() {
		return oldClassPath == null ? className : oldClassPath + "." + className;
	}
	
	public void setOldClassPath(String path) {
		oldClassPath = path;
	}
	
	public String getJarJarRulesPath() {
		return jarjarRulesPath;
	}
	
	public Path getJarJarRulesPathPath() {
		if(jarjarRulesPath != null)
			return FileHelpers.getPath(jarjarRulesPath);
		return null;
	}
	
	public void setJarJarRulesPath(String path) {
		this.jarjarRulesPath = path;
	}
	
	public Remapper getRenamer() {
		if(jarjarRulesPath != null) {
			return RulesFileParser.getRemapper(getJarJarRulesPathPath());
		}
		return null;
	}
	
	public String getSelectedPath() {
		return selectedPath;
	}
	
	public Path getSelectedPathPath() {
		if(selectedPath != null)
			return FileHelpers.getPath(selectedPath);
		return null;
	}
	
	public void setSelectedPath(String path) {
		this.selectedPath = path;
	}
	
	public Set<PossibleEntry> getPossiblePaths() {
		return possiblePaths;
	}
	
	public void setPossiblePaths(Set<PossibleEntry> possiblePaths) {
		this.possiblePaths = SortingMethods.sortSet(possiblePaths);
	}
	
	public void addPossiblePath(PossibleEntry possiblePath) {
		this.possiblePaths.add(possiblePath);
	}
	
	@Override
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof ClassEntry))
			return false;
		ClassEntry other = (ClassEntry)o;
		return Objects.equals(this.fullClassPath, other.fullClassPath) 
				&& Objects.equals(this.selectedPath, other.selectedPath) 
				&& Objects.equals(this.possiblePaths, other.possiblePaths)
				&& Objects.equals(oldClassPath, other.oldClassPath)
				&& Objects.equals(jarjarRulesPath, other.jarjarRulesPath);
	}
	
	@Override
	public int hashCode() {
		int i = 17;
		i = i * 31 + Objects.hashCode(fullClassPath);
		i = i * 31 + Objects.hashCode(selectedPath);
		i = i * 31 + Objects.hashCode(possiblePaths);
		i = i * 31 + Objects.hashCode(oldClassPath);
		i = i * 31 + Objects.hashCode(jarjarRulesPath);
		return i;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(fullClassPath).append("\n");
		sb.append("  ").append("Name: ").append(className).append("\n");
		sb.append("  ").append("Path: ").append(Objects.toString(classPath)).append("\n");
		sb.append("  ").append("Old Path: ").append(Objects.toString(oldClassPath)).append("\n");
		sb.append("  ").append("Jar Jar Rules File: ").append(Objects.toString(jarjarRulesPath)).append("\n");
		sb.append("  ").append("Selected Java File: ").append(Objects.toString(selectedPath)).append("\n");
		if(possiblePaths.isEmpty()) {
			sb.append("  ").append("Possible Java Files: None\n");
		} else {
			sb.append("  ").append("Possible Java Files:\n");
			for(PossibleEntry s : possiblePaths) {
				if(s != null)
					sb.append(s.toString("    ")).append("\n");
			}
		}
		return sb.toString();
	}

	@Override
	public int compareTo(ClassEntry o) {
		return Paths.get(fullClassPath).compareTo(Paths.get(o.fullClassPath));
	}
	
	//ReadResolve is always run when reading from XML even if a constructor is run first
	protected Object readResolve() throws ObjectStreamException {
		if(className != null && className.isEmpty())
			className = null;
		if(classPath != null && classPath.isEmpty())
			classPath = null;
		if(fullClassPath != null && fullClassPath.isEmpty())
			fullClassPath = null;
		if(oldClassPath != null && oldClassPath.isEmpty())
			oldClassPath = null;
		if(jarjarRulesPath != null && jarjarRulesPath.isEmpty())
			jarjarRulesPath = null;
		if(selectedPath != null && selectedPath.isEmpty())
			selectedPath = null;
		return this;
	}
	
	protected Object writeReplace() throws ObjectStreamException {
		if(className == null)
			className = "";
		if(classPath == null)
			classPath = "";
		if(fullClassPath == null)
			fullClassPath = "";
		if(oldClassPath == null)
			oldClassPath = "";
		if(jarjarRulesPath == null)
			jarjarRulesPath = "";
		if(selectedPath == null)
			selectedPath = "";
		return this;
	}
	
	public boolean hasRenamedClassPath() {
		if(oldClassPath != null && !oldClassPath.equals(classPath))
			return true;
		return false;
	}
	
	@XStreamAlias("Wrapper")
	public static final class Wrapper implements XStreamInOutInterface {
		
		private String inputDir;
		private String rootRepoDir;
		private Set<ClassEntry> entries;
		
		private Wrapper() {}
		
		public Wrapper(Set<ClassEntry> entries, String inputDir, String rootRepoDir) {
			this.entries = entries;
			this.inputDir = inputDir;
			this.rootRepoDir = rootRepoDir;
		}
		
		public Set<ClassEntry> getEntries() {
			return entries;
		}
		
		public String getInputDir() {
			return inputDir;
		}
		
		public String getRootRepoDir() {
			return rootRepoDir;
		}

		@Override
		public void writeXML(String filePath, Path path) throws Exception {
			XStreamInOut.writeXML(this, filePath, path);
		}

		@Override
		public Wrapper readXML(String filePath, Path path) throws Exception {
			return (Wrapper)XStreamInOut.readXML(this,filePath, path);
		}
		
		public static Wrapper readXMLStatic(String filePath, Path path) throws Exception {
			return new Wrapper().readXML(filePath, path);
		}
		
		public static void writeXMLStatic(Set<ClassEntry> entries, String inputDir, String rootRepoDir, 
				String filePath, Path path) throws Exception {
			new Wrapper(entries,inputDir,rootRepoDir).writeXML(filePath, path);
		}

		@XStreamOmitField
		private static AbstractXStreamSetup xstreamSetup = null;

		public static AbstractXStreamSetup getXStreamSetupStatic(){
			if(xstreamSetup == null)
				xstreamSetup = new XStreamSetup();
			return xstreamSetup;
		}
		
		@Override
		public AbstractXStreamSetup getXStreamSetup() {
			return getXStreamSetupStatic();
		}
		
		public static class XStreamSetup extends AbstractXStreamSetup {
			
			@Override
			public void getOutputGraph(LinkedHashSet<AbstractXStreamSetup> in) {
				if(!in.contains(this)) {
					in.add(this);
				}
			}

			@Override
			public Set<Class<?>> getAnnotatedClasses() {
				Set<Class<?>> ret = new HashSet<>();
				ret.add(ClassEntry.class);
				ret.add(Wrapper.class);
				ret.add(PossibleEntry.class);
				return ret;
			}
			
			@Override
			public void setXStreamOptions(XStream xstream) {
				defaultOptionsNoRef(xstream);
			}
			
		}
	}
}