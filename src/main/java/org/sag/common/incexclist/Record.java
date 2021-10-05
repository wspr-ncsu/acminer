package org.sag.common.incexclist;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.sag.common.xstream.XStreamInOut;
import org.sag.common.xstream.XStreamInOut.XStreamInOutInterface;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("Record")
public abstract class Record<T> implements XStreamInOutInterface {
	
	public static final String addSymbol = "+";
	public static final String removeSymbol = "-";
	
	@XStreamAlias("Include")
	@XStreamAsAttribute
	protected boolean isInclude;
	
	//For xstream use only
	protected Record(){}
	
	protected Record(boolean isInclude){
		this.isInclude = isInclude;
	}

	public abstract boolean isMatch(T m);
	
	public abstract String getTypeId();
	public abstract String getOptions();
	public abstract String getSignature();
	
	public String getIncludeId(){
		return IncludeId.toId(isInclude());
	}
	
	public static boolean getBooleanFromIncludeId(String id){
		return IncludeId.toBoolean(id);
	}
	
	static enum IncludeId{
		INCLUDED(true,addSymbol),
		EXCLUDED(false,removeSymbol);
		
		private boolean isInclude;
		private String id;
		IncludeId(boolean isInclude, String id){
			this.isInclude = isInclude;
			this.id = id;
		}
		public String getId(){ return id; }
		public boolean isInclude(){ return isInclude; }
		public static String toId(boolean isInclude){
			for(IncludeId i : IncludeId.values()){
				if(i.isInclude == isInclude)
					return i.getId();
			}
			throw new RuntimeException("Error: Could not find id to match the given boolean.");
		}
		public static boolean toBoolean(String id){
			for(IncludeId i : IncludeId.values()){
				if(i.getId().equals(id))
					return i.isInclude();
			}
			throw new RuntimeException("Error: Could not find boolean to match the given id.");
		}
		public static IncludeId toIncludeId(boolean isInclude){
			for(IncludeId i : IncludeId.values()){
				if(i.isInclude == isInclude)
					return i;
			}
			throw new RuntimeException("Error: Could the given boolean has no related IncludeId.");
		}
		public static IncludeId toIncludeId(String id){
			for(IncludeId i : IncludeId.values()){
				if(i.getId().equals(id))
					return i;
			}
			throw new RuntimeException("Error: Could the given id has no related IncludeId.");
		}
	}
	
	public final String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(getIncludeId()).append(" ");
		sb.append(getTypeId()).append(": ");
		String options = getOptions();
		if(options != null && !options.isEmpty())
			sb.append(options).append(" ");
		sb.append(getSignature());
		return sb.toString();
	}
	
	public List<String> toStringGetBasicParts(){
		ArrayList<String> ret = new ArrayList<>();
		ret.add(this.getIncludeId());
		ret.add(this.getTypeId());
		ret.add(this.getOptions());
		ret.add(this.getSignature());
		return ret;
	}
	
	public boolean isMatchString(String s){
		throw new RuntimeException("Unsuported operation");
	}
	
	public boolean isInclude(){
		return isInclude;
	}
	
	public void resetSootResolvedData(){}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}
	
	@Override
	public Record<T> readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	@SuppressWarnings("rawtypes")
	public static Record readXMLStatic(String filePath, Path path) throws Exception {
		return (Record) XStreamInOut.readXML(filePath, path, getXStreamSetupStatic().getXStream());
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
			return Collections.singleton(Record.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}
	
}
