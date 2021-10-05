package org.sag.common.incexclist.classlist;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sag.common.incexclist.AbstractIncludeExcludeList;
import org.sag.common.incexclist.Record;
import org.sag.common.xstream.XStreamInOut;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import soot.SootClass;

@XStreamAlias("ClassIncludeExcludeList")
public class ClassIncludeExcludeList extends AbstractIncludeExcludeList<SootClass,String>{

	@XStreamOmitField
	private HashMap<String,Boolean> stringCache;
	
	public static final String commentIndicator = "//";
	public static final String spacer = "  ";
	public static final String[] headers = {"Add/Remove", "Type", "Options", "Signature"};
	private static final Pattern p1 = Pattern.compile("^([\\+-])\\s+([^\\s]+)\\s+(.+)");
	
	public ClassIncludeExcludeList(){
		this(false);
	}
	
	public ClassIncludeExcludeList(boolean includeAll) {
		this(includeAll,false);
	}
	
	public ClassIncludeExcludeList(boolean includeAll, boolean enableCache) {
		super(includeAll,enableCache);
		if(enableCache){
			stringCache = new HashMap<String,Boolean>();
		}else{
			stringCache = null;
		}
	}
	
	public static String padRightToMax(String s, int max){
		if(s == null)
			s = "";
		if(s.length() == max)
			return s;
		else if(s.length() > max)
			throw new RuntimeException("Error: The given string '" + s + "' is longer than the maximum length '" + max + "'.");
		else
			return String.format("%1$-" + max + "s", s);
	}
	
	@Override
	public String toString(){
		int[] max = new int[headers.length];
		for(int i = 0; i < max.length; i++){
			max[i] = headers[i].length();
		}
		List<List<String>> parts = new ArrayList<>();
		for(Record<SootClass> e : this){
			List<String> line = e.toStringGetBasicParts();
			for(int i = 0; i < line.size(); i++){
				if(line.get(i) != null){
					max[i] = (max[i] < line.get(i).length() ? line.get(i).length() : max[i]);
				}else{
					line.set(i, "");
				}
			}
			parts.add(line);
		}
		StringBuilder sb = new StringBuilder();
		sb.append(commentIndicator).append(spacer).append(padRightToMax(headers[0],max[0]))
			.append(spacer).append(padRightToMax(headers[1], max[1]))
			.append(spacer).append(padRightToMax(headers[2],max[2]))
			.append(spacer).append(headers[3])//No need to pad to the right on the last entry
			.append("\n\n");
		for(List<String> line : parts){
			sb.append(padRightToMax(null,commentIndicator.length()));
			for(int i = 0; i < line.size(); i++){
				if(i == line.size()-1)
					sb.append(spacer).append(line.get(i));//Don't pad on the last entry 
				else
					sb.append(spacer).append(padRightToMax(line.get(i), max[i]));
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	@Override
	protected Record<SootClass> constructEntry(String line) {
		line = line.trim();
		if(!line.isEmpty()){
			if(!line.startsWith(commentIndicator)){
				Matcher m = p1.matcher(line);
				if(m.matches()){
					String includeId = m.group(1).trim();
					String typeid = m.group(2).trim();
					line = m.group(3).trim();
					boolean isInclude = Record.getBooleanFromIncludeId(includeId.trim());
					if(InterfaceRecord.getTypeIdStatic().equals(typeid)){
						return new InterfaceRecord(isInclude,line);
					}else if(SuperClassRecord.getTypeIdStatic().equals(typeid)){
						return new SuperClassRecord(isInclude,line);
					}else if (ClassRecord.getTypeIdStatic().equals(typeid)) {
						return new ClassRecord(isInclude,line);
					}
				}
				throw new RuntimeException("Error: Unrecongized format. Could not parse entry line.");
			}else{
				return null;
			}
		}else{
			return null;
		}
		
	}
	
	public void addClass(boolean isInclude, String name){
		entryList.add(new ClassRecord(isInclude,name));
	}
	
	public void addInterface(boolean isInclude, String name){
		entryList.add(new InterfaceRecord(isInclude,name));
	}
	
	public void addSuperClass(boolean isInclude, String name){
		entryList.add(new SuperClassRecord(isInclude,name));
	}
	
	@Override
	public boolean isIncludedString(String s){
		if(stringCache != null && stringCache.containsKey(s)){
			return stringCache.get(s);
		}
		boolean ret = defaultIncludeAll;
		for(Record<SootClass> e : entryList){
			if(e.isMatchString(s)){
				ret = e.isInclude();
			}
		}
		if(stringCache != null){
			stringCache.put(s, ret);
		}
		return ret;
	}
	
	public void writeTXT(Path path) throws Exception {
		BufferedWriter out = null;
		try{
			out = Files.newBufferedWriter(path, Charset.defaultCharset());
			out.write(this.toString());
			out.flush();
			out.close();
			out = null;
		}finally{
			if(out != null){
				try{ out.close(); } catch (IOException e) {}
			}
		}
	}
	
	public ClassIncludeExcludeList readTXT(Path path) throws Exception {
		BufferedReader in = null;
		try{
			in = Files.newBufferedReader(path, Charset.defaultCharset());
			String line;
			while((line = in.readLine()) != null){
				this.add(line.trim());
			}
			in.close();
			in = null;
			return this;
		}finally{
			if(in != null){
				try { in.close(); }catch (IOException e) {}
			}
		}
	}
	
	public static ClassIncludeExcludeList readTXTStatic(Path path) throws Exception {
		return new ClassIncludeExcludeList().readTXT(path);
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}
	
	@Override
	public ClassIncludeExcludeList readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static ClassIncludeExcludeList readXMLStatic(String filePath, Path path) throws Exception {
		return new ClassIncludeExcludeList().readXML(filePath, path);
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
				AbstractIncludeExcludeList.getXStreamSetupStatic().getOutputGraph(in);
				Record.getXStreamSetupStatic().getOutputGraph(in);
				ClassRecord.getXStreamSetupStatic().getOutputGraph(in);
				InterfaceRecord.getXStreamSetupStatic().getOutputGraph(in);
				SuperClassRecord.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(ClassIncludeExcludeList.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}

}
