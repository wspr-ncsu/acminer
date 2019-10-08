package org.sag.sje.soot;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.sag.sje.ArchiveEntry;
import org.sag.sje.DexEntry;
import org.sag.xstream.XStreamInOut;
import org.sag.xstream.XStreamInOut.XStreamInOutInterface;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("ClassConflicts")
public final class ClassConflicts implements XStreamInOutInterface {
	
	@XStreamImplicit
	private Set<ClassEntry> classes;
	
	private ClassConflicts() {}
	
	public ClassConflicts(Map<String,Set<ArchiveEntry<? extends DexEntry>>> classesToSources){
		this.classes = new LinkedHashSet<>();
		for(String className : classesToSources.keySet()){
			Set<ArchiveEntry<? extends DexEntry>> sources = classesToSources.get(className);
			if(sources.size() > 1){
				this.classes.add(new ClassEntry(className,sources));
			}
		}
	}

	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public ClassConflicts readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static ClassConflicts readXMLStatic(String filePath, Path path) throws Exception {
		return new ClassConflicts().readXML(filePath, path);
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
			ret.add(ClassConflicts.class);
			ret.add(ClassEntry.class);
			ret.add(SourceEntry.class);
			return ret;
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}
	
	@XStreamAlias("ClassEntry")
	static class ClassEntry {
		@XStreamAlias("ClassName")
		@XStreamAsAttribute
		private String className;
		@XStreamImplicit
		private Set<SourceEntry> sources;
		public ClassEntry(String className, Set<ArchiveEntry<? extends DexEntry>> sources){
			this.className = className;
			this.sources = new LinkedHashSet<>();
			for(ArchiveEntry<? extends DexEntry> source : sources){
				this.sources.add(new SourceEntry(source));
			}
		}
		public String getClassName(){ return className; }
		public Set<SourceEntry> getSources(){ return sources; }
		@Override
		public int hashCode(){ return 527 + Objects.hashCode(className); }
		@Override
		public boolean equals(Object o){
			if(o == this)
				return true;
			if(o == null || !(o instanceof ClassEntry))
				return false;
			ClassEntry other = (ClassEntry)o;
			return Objects.equals(className, other.className);
		}
	}
	
	@XStreamAlias("SourceEntry")
	static class SourceEntry {
		@XStreamAlias("SourceName")
		@XStreamAsAttribute
		private String sourceName;
		@XStreamAlias("Extension")
		@XStreamAsAttribute
		private String extension;
		@XStreamAlias("Location")
		@XStreamAsAttribute
		private String location;
		public SourceEntry(ArchiveEntry<? extends DexEntry> source){
			this.sourceName = source.getName();
			this.extension = source.getExtension();
			this.location = source.getLocation();
		}
		public String getSourceName(){ return sourceName; }
		public String getExtension(){ return extension; }
		public String getLocation(){ return location; }
		@Override
		public int hashCode(){
			int i = 17;
			i = i * 31 + Objects.hashCode(sourceName);
			i = i * 31 + Objects.hashCode(extension);
			i = i * 31 + Objects.hashCode(location);
			return i;
		}
		@Override
		public boolean equals(Object o){
			if(o == this)
				return true;
			if(o == null || !(o instanceof SourceEntry))
				return false;
			SourceEntry other = (SourceEntry)o;
			return Objects.equals(sourceName, other.sourceName) && Objects.equals(extension, other.extension) 
					&& Objects.equals(location, other.location);
		}
	}

}
