package org.sag.common.incexclist.methodlist;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sag.common.incexclist.AbstractIncludeExcludeList;
import org.sag.common.incexclist.Record;
import org.sag.common.incexclist.classlist.ClassIncludeExcludeList;
import org.sag.xstream.XStreamInOut;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import soot.SootMethod;

@XStreamAlias("MethodIncludeExcludeList")
public class MethodIncludeExcludeList extends AbstractIncludeExcludeList<SootMethod,String>{
	
	public static final String commentIndicator = "//";
	public static final String spacer = "  ";
	public static final String[] headers = {"Add/Remove", "Type", "Options", "Signature"};
	private static final Pattern p1 = Pattern.compile("^([\\+-])\\s+(.+)");
	private static final Pattern p2 = Pattern.compile("^([^\\s]+)\\s+(.+)");

	protected MethodIncludeExcludeList(){
		super();
	}
	
	public MethodIncludeExcludeList(boolean includeAll) {
		super(includeAll);
	}
	
	public MethodIncludeExcludeList(boolean includeAll, boolean enableCache) {
		super(includeAll,enableCache);
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
		for(Record<SootMethod> e : this){
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
	protected Record<SootMethod> constructEntry(String line){
		line = line.trim();
		if(!line.isEmpty()){
			if(!line.startsWith(commentIndicator)){
				Matcher m = p1.matcher(line);
				if(m.matches()){
					String includeId = m.group(1).trim();
					line = m.group(2).trim();
					m = p2.matcher(line);
					if(m.matches()){
						String typeid = m.group(1).trim();
						line = m.group(2).trim();
						if(ClassRecord.getTypeIdStatic().equals(typeid)){
							return ClassRecord.constructEntry(includeId, line);
						}else if(MethodSignatureRecord.getTypeIdStatic().equals(typeid)){
							return MethodSignatureRecord.constructEntry(includeId, line);
						}else if(MethodNameRecord.getTypeIdStatic().equals(typeid)){
							return MethodNameRecord.constructEntry(includeId, line);
						}else if(InterfaceRecord.getTypeIdStatic().equals(typeid)){
							return InterfaceRecord.constructEntry(includeId, line);
						}else if(MethodNameAndClassRecord.getTypeIdStatic().equals(typeid)){
							return MethodNameAndClassRecord.constructEntry(includeId, line);
						}else if(SuperClassRecord.getTypeIdStatic().equals(typeid)){
							return SuperClassRecord.constructEntry(includeId, line);
						} else if(MethodSpecialRecord.getTypeIdStatic().equals(typeid)) {
							return MethodSpecialRecord.constructEntry(includeId, line);
						}
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
	
	public boolean isListedMethodInclude(SootMethod m){
		for(Record<SootMethod> e : entryList){
			if(e instanceof MethodSignatureRecord && e.isMatch(m) && e.isInclude()){
				return true;
			}
		}
		return false;
	}
	
	public boolean isListedMethodExclude(SootMethod m){
		for(Record<SootMethod> e : entryList){
			if(e instanceof MethodSignatureRecord && e.isMatch(m) && !e.isInclude()){
				return true;
			}
		}
		return false;
	}
	
	public ClassIncludeExcludeList convertToClassIncludeExcludeList(){
		ClassIncludeExcludeList cl = new ClassIncludeExcludeList(this.defaultIncludeAll,this.cache != null);
		for(Record<SootMethod> e : entryList){
			if(e instanceof ClassRecord){
				ClassRecord ee = (ClassRecord)e;
				if(ee.isEntireClass()){
					cl.addClass(ee.isInclude(), ee.getSignature());
				}
			}else if(e instanceof InterfaceRecord){
				InterfaceRecord ee = (InterfaceRecord)e;
				if(ee.isEntireClass()){
					cl.addInterface(ee.isInclude(), ee.getSignature());
				}
			} else if(e instanceof SuperClassRecord) {
				SuperClassRecord ee = (SuperClassRecord)e;
				if(ee.isEntireClass()) {
					cl.addSuperClass(ee.isInclude(), ee.getSignature());
				}
			}
		}
		return cl;
	}
	
	public synchronized void writeTXT(Path path) throws Exception {
		try(BufferedWriter out = Files.newBufferedWriter(path, Charset.defaultCharset())){
			out.write(this.toString());
			out.flush();
		}
	}
	
	public synchronized MethodIncludeExcludeList readTXT(Path path) throws Exception {
		try(BufferedReader in = Files.newBufferedReader(path, Charset.defaultCharset())){
			String line;
			while((line = in.readLine()) != null){
				this.add(line.trim());
			}
		}
		return this;
	}
	
	public static MethodIncludeExcludeList readTXTStatic(Path path, boolean includeAll, boolean enableCache) throws Exception {
		return new MethodIncludeExcludeList(includeAll, enableCache).readTXT(path);
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}
	
	@Override
	public MethodIncludeExcludeList readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static MethodIncludeExcludeList readXMLStatic(String filePath, Path path) throws Exception {
		return new MethodIncludeExcludeList().readXML(filePath, path);
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
				AbstractIncludeExcludeList.getXStreamSetupStatic().getOutputGraph(in);
				ClassRecord.getXStreamSetupStatic().getOutputGraph(in);
				InterfaceRecord.getXStreamSetupStatic().getOutputGraph(in);
				MethodNameAndClassRecord.getXStreamSetupStatic().getOutputGraph(in);
				MethodNameRecord.getXStreamSetupStatic().getOutputGraph(in);
				MethodSignatureRecord.getXStreamSetupStatic().getOutputGraph(in);
				SuperClassRecord.getXStreamSetupStatic().getOutputGraph(in);
				MethodSpecialRecord.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(MethodIncludeExcludeList.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}
}
