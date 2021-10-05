package org.sag.common.incexclist.methodlist;

import java.nio.file.Path;
import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sag.common.incexclist.Record;
import org.sag.common.xstream.BitSetSingleValueConverter;
import org.sag.common.xstream.XStreamInOut;

import soot.SootClass;
import soot.SootMethod;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("ClassRecord")
class ClassRecord extends Record<SootMethod> {

	@XStreamAlias("Name")
	@XStreamAsAttribute
	protected String name;
	
	@XStreamAlias("Mods")
	@XStreamConverter(BitSetSingleValueConverter.class)
	@XStreamAsAttribute
	protected BitSet modifiers;
	
	private static final String typeId = "ClassPath";
	private static final int mSize = 8;
	private static final Pattern p1 = Pattern.compile("^\\[(.*)\\]\\s+(.+)");
	
	//For xstream use only
	protected ClassRecord(){}

	protected ClassRecord(boolean isInclude, String name){
		this(isInclude,name,"all");
	}
	
	protected ClassRecord(boolean isInclude, String name, String modifiers) {
		super(isInclude);
		this.name = name;
		this.modifiers = constructBitSet(modifiers);
	}
	
	protected ClassRecord(boolean isInclude, String name, BitSet modifiers) {
		super(isInclude);
		this.name = name;
		this.modifiers = modifiers;
	}
	
	protected static ClassRecord constructEntry(String includeId, String line){
		Object[] temp = parseInput(includeId, line);
		boolean isInclude = (Boolean)temp[0];
		String modifiers = (String)temp[1];
		String name = (String)temp[2];
		if(modifiers == null)
			return new ClassRecord(isInclude,name);
		else
			return new ClassRecord(isInclude,name,modifiers);
	}
	
	protected static Object[] parseInput(String includeId, String line){
		Object[] ret = new Object[3];
		boolean isInclude = getBooleanFromIncludeId(includeId.trim());
		
		if(line == null)
			throw new RuntimeException("Error: At least a class path must be provided.");
		
		line = line.trim();
		
		if(line.isEmpty())
			throw new RuntimeException("Error: At least a class path must be provided.");
		
		if(line.startsWith("[")){
			Matcher m = p1.matcher(line);
			if(m.matches()){
				String modifiers = m.group(1).trim();
				String name = m.group(2).trim();
				if(name != null && !name.isEmpty()){
					ret[0] = isInclude;
					ret[1] = modifiers;
					ret[2] = name;
					return ret;
				}
			}
			throw new RuntimeException("Error: Failed to seperate modifiers from name for '" + line + "'.");
		}else{
			ret[0] = isInclude;
			ret[1] = null;
			ret[2] = line;
			return ret;
		}
	}
	
	public static String getTypeIdStatic(){
		return typeId;
	}
	
	@Override
	public String getTypeId() {
		return typeId;
	}

	@Override
	public String getOptions() {
		return bitSetToString(modifiers);
	}

	@Override
	public String getSignature() {
		return name;
	}
	
	public boolean isEntireClass(){
		if(modifiers.cardinality() == mSize){
			return true;
		}
		return false;
	}

	@Override
	public boolean isMatch(SootMethod m) {
		SootClass c = m.getDeclaringClass();
		if(compare(name,c.getName())){
			if((c.isPublic() && modifiers.get(ModifierKey.CPUBLIC.getIndex()))
					|| (c.isPrivate() && modifiers.get(ModifierKey.CPRIVATE.getIndex()))
					|| (c.isProtected() && modifiers.get(ModifierKey.CPROTECTED.getIndex()))
					|| (!c.isPrivate() && !c.isPublic() && !c.isProtected() && modifiers.get(ModifierKey.CPKGPROTECTED.getIndex()))){
				if((m.isPublic() && modifiers.get(ModifierKey.PUBLIC.getIndex()))
						|| (m.isPrivate() && modifiers.get(ModifierKey.PRIVATE.getIndex()))
						|| (m.isProtected() && modifiers.get(ModifierKey.PROTECTED.getIndex()))
						|| (!m.isPrivate() && !m.isProtected() && !m.isPublic() && modifiers.get(ModifierKey.PKGPROTECTED.getIndex()))){
					return true;
				}
			}
		}
		return false;
	}
	
	protected BitSet constructBitSet(String m){
		BitSet modifiers = new BitSet(mSize);
		if(m == null || m.isEmpty())
			return modifiers;
		String[] mods = m.split(",");
		for(String s : mods){
			ModifierKey key = ModifierKey.nameToKey(s);
			if(key.equals(ModifierKey.ALL)){
				modifiers.set(0, mSize);
			}else if(key.equals(ModifierKey.MALL)){
				modifiers.set(0,ModifierKey.PKGPROTECTED.getIndex()+1);
			}else if(key.equals(ModifierKey.CALL)){
				modifiers.set(ModifierKey.CPUBLIC.getIndex(),ModifierKey.CPKGPROTECTED.getIndex()+1);
			}else{
				modifiers.set(key.getIndex());
			}
		}
		return modifiers;
	}
	
	protected String bitSetToString(BitSet modifiers){
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		if(modifiers.cardinality() == mSize){
			sb.append(ModifierKey.ALL.getName());
		}else if(modifiers.get(0, ModifierKey.PKGPROTECTED.getIndex()+1).cardinality() == 4){
			sb.append(ModifierKey.MALL.getName());
			for(int i = ModifierKey.CPUBLIC.getIndex(); i < mSize; i++){
				sb.append(modifiers.get(i) ? ModifierKey.indexToName(i) : "");
			}
		}else if(modifiers.get(ModifierKey.CPUBLIC.getIndex(),mSize).cardinality() == 4){
			for(int i = 0; i < ModifierKey.PKGPROTECTED.getIndex()+1; i++){
				sb.append(modifiers.get(i) ? ModifierKey.indexToName(i) : "");
			}
			sb.append(ModifierKey.CALL.getName());
		}else{
			for(int i = 0; i < mSize; i++){
				sb.append(modifiers.get(i) ? ModifierKey.indexToName(i) : "");
			}
		}
		int index = sb.lastIndexOf(",");
		if(index >= 0 && index == sb.length()-1){
			sb.setCharAt(index, ']');
		}else{
			sb.append("]");
		}
		return sb.toString();
	}
	
	protected int getModifiersSize(){
		return mSize;
	}
	
	protected static enum ModifierKey{
		PUBLIC(0,"public"),
		PRIVATE(1,"private"),
		PROTECTED(2,"protected"),
		PKGPROTECTED(3,"pkgprotected"),
		CPUBLIC(4,"cPublic"),
		CPRIVATE(5,"cPrivate"),
		CPROTECTED(6,"cProtected"),
		CPKGPROTECTED(7,"cPkgProtected"),
		MALL(8,"mAll"),
		CALL(9,"cAll"),
		ALL(10,"all");
		
		private String name;
		private int i;
		ModifierKey(int i,String name){
			this.i = i;
			this.name = name;
		}
		public int getIndex(){ return i; }
		public String getName(){ return name; }
		public static String indexToName(int i){
			for(ModifierKey key : ModifierKey.values()){
				if(i == key.getIndex())
					return key.getName();
			}
			throw new RuntimeException("Error: The given index is not a key for the modifiers bit set.");
		}
		public static ModifierKey indexToKey(int i){
			for(ModifierKey key : ModifierKey.values()){
				if(i == key.getIndex())
					return key;
			}
			throw new RuntimeException("Error: The given index is not a key for the modifiers bit set.");
		}
		public static int nameToIndex(String name){
			for(ModifierKey key : ModifierKey.values()){
				if(name.equals(key.getName()))
					return key.getIndex();
			}
			throw new RuntimeException("Error: The given name is not a name for a key of the modifiers bit set.");
		}
		public static ModifierKey nameToKey(String name){
			for(ModifierKey key : ModifierKey.values()){
				if(name.equals(key.getName()))
					return key;
			}
			throw new RuntimeException("Error: The given name is not a name for a key of the modifiers bit set.");
		}
		public String toString(){ return name; }
	}
	
	protected static boolean compare(String name,String className){
		if(className.equals(name) || ((name.endsWith(".*") || name.endsWith("$*")) && className.startsWith(name.substring(0, name.length() - 1)))){
			return true;
		}
		return false;
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}
	
	@Override
	public ClassRecord readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static ClassRecord readXMLStatic(String filePath, Path path) throws Exception {
		return new ClassRecord().readXML(filePath, path);
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
			return Collections.singleton(ClassRecord.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}
	
}
