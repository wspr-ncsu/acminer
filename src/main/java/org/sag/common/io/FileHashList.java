package org.sag.common.io;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.sag.common.xstream.XStreamInOut;
import org.sag.common.xstream.XStreamInOut.XStreamInOutInterface;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("FileHashList")
public class FileHashList implements List<FileHash>, XStreamInOutInterface {
	
	@XStreamImplicit
	private List<FileHash> list;
	
	private FileHashList() {}
	
	public FileHashList(List<FileHash> list) {
		if(list == null)
			throw new IllegalArgumentException("Error: The list cannot be null.");
		this.list = list;
	}

	@Override public int size() { return list.size(); }
	@Override public boolean isEmpty() { return list.isEmpty(); }
	@Override public boolean contains(Object o) { return list.contains(o); }
	@Override public Iterator<FileHash> iterator() { return list.iterator(); }
	@Override public Object[] toArray() { return list.toArray(); }
	@Override public <T> T[] toArray(T[] a) { return list.toArray(a); }
	@Override public boolean add(FileHash e) { return list.add(e); }
	@Override public boolean remove(Object o) { return list.remove(o); }
	@Override public boolean containsAll(Collection<?> c) { return list.containsAll(c); }
	@Override public boolean addAll(Collection<? extends FileHash> c) { return list.addAll(c); }
	@Override public boolean addAll(int index, Collection<? extends FileHash> c) { return list.addAll(index, c); }
	@Override public boolean removeAll(Collection<?> c) { return list.removeAll(c); }
	@Override public boolean retainAll(Collection<?> c) { return list.retainAll(c); }
	@Override public void clear() { list.clear(); }
	@Override public FileHash get(int index) { return list.get(index); }
	@Override public FileHash set(int index, FileHash element) { return list.set(index, element); }
	@Override public void add(int index, FileHash element) { list.add(index, element); }
	@Override public FileHash remove(int index) { return list.remove(index); }
	@Override public int indexOf(Object o) { return list.indexOf(o); }
	@Override public int lastIndexOf(Object o) { return list.lastIndexOf(o); }
	@Override public ListIterator<FileHash> listIterator() { return list.listIterator(); }
	@Override public ListIterator<FileHash> listIterator(int index) { return list.listIterator(index); }
	@Override public List<FileHash> subList(int fromIndex, int toIndex) { return list.subList(fromIndex, toIndex); }
	@Override public boolean equals(Object o) { return list.equals(o); }
	@Override public int hashCode() { return list.hashCode(); }
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}
	
	@Override
	public FileHashList readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static FileHashList readXMLStatic(String filePath, Path path) throws Exception {
		return new FileHashList().readXML(filePath, path);
	}
	
	public String writeXMLToString() throws Exception {
		return XStreamInOut.writeXMLToString(this);
	}
	
	public FileHashList readXMLFromString(String in) throws Exception {
		return XStreamInOut.readXMLFromString(this, in);
	}
	
	public static FileHashList readXMLFromStringStatic(String in) throws Exception {
		return new FileHashList().readXMLFromString(in);
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
				FileHash.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(FileHashList.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}
	
}
