package org.sag.acminer.database.binderservices;

import java.io.ObjectStreamException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.sag.acminer.database.FileHashDatabase;
import org.sag.common.xstream.XStreamInOut;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("BinderServicesDatabase")
public class BinderServicesDatabase extends FileHashDatabase implements IBinderServicesDatabase {
	
	@XStreamOmitField
	private final ReadWriteLock rwlock;
	
	@XStreamOmitField
	private volatile boolean loaded;
	
	protected BinderServicesDatabase(boolean newDB) {
		if(newDB) {
			//data = new HashMap<>();
			loaded = true;
		} else {
			//data = null;
			loaded = false;
		}
		rwlock = new ReentrantReadWriteLock();
		//output = null;
	}
	
	//ReadResolve is always run when reading from XML even if a constructor is run first
	protected Object readResolve() throws ObjectStreamException {
		//We could load the soot resolved data here but we don't because we want to be able
		//to load this database from file without having soot initialized
		//We can just call loadSootResolvedData after if we wish to use this with soot
		//loadSootResolvedData();
		return this;
	}
	
	protected Object writeReplace() throws ObjectStreamException {
		rwlock.writeLock().lock();
		try {
			//writeSootResolvedDataWLocked();
		} finally {
			rwlock.writeLock().unlock();
		}
		return this;
	}
	
	@Override
	public void clearSootResolvedData() {
		rwlock.writeLock().lock();
		try {
			//writeSootResolvedDataWLocked();
			//data = null;
			loaded = false;
		} finally {
			rwlock.writeLock().unlock();
		}
	}
	
	@Override
	public void loadSootResolvedData() {
		rwlock.writeLock().lock();
		try {
			//loadSootResolvedDataWLocked();
		} finally {
			rwlock.writeLock().unlock();
		}
	}
	
	@Override
	public String toString() {
		return toString("");
	}
	
	@Override
	public String toString(String spacer) {
		StringBuilder sb = new StringBuilder();
		sb.append("Structure Of Binder Service Database:\n");
		/*for(EntryPointContainer e : getOutputData()) {
			sb.append(e.toString(spacer + "  "));
		}*/
		return sb.toString();
	}
	
	@Override
	public int hashCode() {
		int i = 17;
		//i = i * 31 + Objects.hashCode(getOutputData());
		return i;
	}
	
	@Override
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof IBinderServicesDatabase))
			return false;
		//IBinderServiceDatabase other = (IBinderServiceDatabase)o;
		//return Objects.equals(getOutputData(), other.getOutputData());
		return true;
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}
	
	@Override
	public BinderServicesDatabase readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this,filePath, path);
	}
	
	public static BinderServicesDatabase readXMLStatic(String filePath, Path path) throws Exception {
		return new BinderServicesDatabase(false).readXML(filePath, path);
	}
	
	@XStreamOmitField
	private static AbstractXStreamSetup xstreamSetup = null;

	public static AbstractXStreamSetup getXStreamSetupStatic(){
		if(xstreamSetup == null)
			xstreamSetup = new SubXStreamSetup();
		return xstreamSetup;
	}
	
	@Override
	public AbstractXStreamSetup getXStreamSetup() {
		return getXStreamSetupStatic();
	}
	
	public static class SubXStreamSetup extends XStreamSetup {
		
		@Override
		public void getOutputGraph(LinkedHashSet<AbstractXStreamSetup> in) {
			if(!in.contains(this)) {
				in.add(this);
				super.getOutputGraph(in);
				//Z3SymbolContainer.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(BinderServicesDatabase.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}

}
