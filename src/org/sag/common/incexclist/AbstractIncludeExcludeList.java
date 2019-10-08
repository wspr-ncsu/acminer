package org.sag.common.incexclist;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.sag.xstream.XStreamInOut;
import org.sag.xstream.XStreamInOut.XStreamInOutInterface;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("AbstractIncludeExcludeList")
public abstract class AbstractIncludeExcludeList<T,D> implements XStreamInOutInterface, Iterable<Record<T>>{
	
	@XStreamAlias("DefaultPolicyIsIncludeAll")
	@XStreamAsAttribute
	protected boolean defaultIncludeAll;
	
	@XStreamImplicit
	protected ArrayList<Record<T>> entryList;
	
	@XStreamOmitField
	protected HashMap<T,Boolean> cache;
	
	//For use with xstream only
	public AbstractIncludeExcludeList(){
		this(false,false);
	}

	public AbstractIncludeExcludeList(boolean includeAll){
		this(includeAll,false);
	}
	
	public AbstractIncludeExcludeList(boolean includeAll, boolean enableCache){
		defaultIncludeAll = includeAll;
		entryList = new ArrayList<Record<T>>();
		if(enableCache){
			cache = new HashMap<T,Boolean>();
		}else{
			cache = null;
		}
	}
	
	protected abstract Record<T> constructEntry(D d);
	
	public void add(Record<T> r){
		entryList.add(r);
	}
	
	public void insertAfter(int index, Record<T> r){
		entryList.add(index+1, r);
	}
	
	public void add(D d){
		Record<T> e = constructEntry(d);
		if(e != null)
			add(e);
	}
	
	public void insertAfter(int index, D d){
		Record<T> e = constructEntry(d);
		if(e != null)
			insertAfter(index, e);
	}
	
	public void insertAfter(String id, D d){
		int i;
		for(i = 0; i < entryList.size(); i++){
			Record<T> e = entryList.get(i);
			if(e.toString().equals(id)){
				i++;
				break;
			}
		}
		insertAfter(i,d);
	}
	
	public boolean isIncluded(T m){
		if(cache != null && cache.containsKey(m)){
			return cache.get(m);
		}
		boolean ret = defaultIncludeAll;
		for(Record<T> e : entryList){
			if(e.isMatch(m)){
				ret = e.isInclude();
			}
		}
		if(cache != null){
			cache.put(m, ret);
		}
		return ret;
	}
	
	public boolean isExcluded(T m){
		return !isIncluded(m);
	}
	
	public boolean isIncludedString(String s){
		throw new RuntimeException("Unsuported operation");
	}
	
	public boolean isExcludedString(String s){
		return !isIncludedString(s);
	}
	
	public boolean isEmpty(){
		return entryList.isEmpty();
	}
	
	public Iterator<Record<T>> iterator(){
		return entryList.iterator();
	}
	
	public void resetSootResolvedData(){
		for(Record<T> e : entryList){
			e.resetSootResolvedData();
		}
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}
	
	@Override
	public AbstractIncludeExcludeList<T,D> readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	@SuppressWarnings("rawtypes")
	public static AbstractIncludeExcludeList readXMLStatic(String filePath, Path path) throws Exception {
		return (AbstractIncludeExcludeList) XStreamInOut.readXML(filePath, path, getXStreamSetupStatic().getXStream());
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
				Record.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(AbstractIncludeExcludeList.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}
}
