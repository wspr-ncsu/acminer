package org.sag.acminer.database.defusegraph;

import java.io.ObjectStreamException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.sag.acminer.database.FileHashDatabase;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.io.FileHelpers;
import org.sag.common.tools.SortingMethods;
import org.sag.xstream.XStreamInOut;
import org.sag.xstream.xstreamconverters.NamedCollectionConverterWithSize;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("DefUseGraphDatabase")
public class DefUseGraphDatabase extends FileHashDatabase implements IDefUseGraphDatabase {
	
	@XStreamAlias("EntryPoints")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"EntryPointInfo"},types={EntryPointInfo.class})
	private ArrayList<EntryPointInfo> info;
	
	@XStreamOmitField
	private Map<EntryPoint, EntryPointInfo> data;

	public DefUseGraphDatabase(boolean isNew) {
		if(isNew)
			this.data = new HashMap<>();
		else
			this.data = null;
		this.info = null;
		this.fhl = null;
	}
	
	//ReadResolve is always run when reading from XML even if a constructor is run first
	protected Object readResolve() throws ObjectStreamException {
		loadSootResolvedData();
		return this;
	}
	
	protected Object writeReplace() throws ObjectStreamException {
		writeSootResolvedData();
		return this;
	}
	
	//Assumes sorted, non null, and non empty
	@Override
	public void writeAndAddDefUseGraph(EntryPoint ep, DefUseGraph graph, Path outDir) throws Exception {
		Objects.requireNonNull(ep);
		Objects.requireNonNull(graph);
		loadSootResolvedData();
		Path output = FileHelpers.getPath(outDir, FileHelpers.getHashOfString("MD5", ep.toString()) + ".xml");
		if(FileHelpers.checkRWFileExists(output))
			throw new Exception("Error: A file name collision has occured for '" + ep.toString() + "' at '" + output.toString() + "'");
		graph.writeXML(null, output);
		synchronized(this) {
			data.put(ep, new EntryPointInfo(ep, output));
		}
	}
	
	@Override
	public DefUseGraph getDefUseGraph(EntryPoint ep, Path dir) throws Exception {
		loadSootResolvedData();
		EntryPointInfo epi;
		synchronized(this) {
			epi = data.get(ep);
		}
		DefUseGraph ret = epi.getDefUseGraph(dir);
		if(ret == null)
			throw new Exception("Error: Failed to load the DefUseGraph for entry point '" + ep + "'");
		return ret;
	}
	
	@Override
	public DefUseGraph getDefUseGraphUnchecked(EntryPoint ep, Path dir) throws Exception {
		loadSootResolvedData();
		EntryPointInfo epi;
		synchronized(this) {
			epi = data.get(ep);
		}
		DefUseGraph ret = epi.getDefUseGraphUnchecked(dir);
		if(ret == null)
			throw new Exception("Error: Failed to load the DefUseGraph for entry point '" + ep + "'");
		return ret;
	}
	
	@Override
	public synchronized int hashCode() {
		loadSootResolvedData();
		return data.hashCode();
	}
	
	//This will need locking if ever called in a thread which is unlikely
	@Override
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof DefUseGraphDatabase))
			return false;
		DefUseGraphDatabase other = (DefUseGraphDatabase)o;
		loadSootResolvedData();
		other.loadSootResolvedData();
		return Objects.equals(data, other.data);
	}

	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public DefUseGraphDatabase readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static DefUseGraphDatabase readXMLStatic(String filePath, Path path) throws Exception {
		return new DefUseGraphDatabase(true).readXML(filePath, path);
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
				EntryPointInfo.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(DefUseGraphDatabase.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsXPathRelRef(xstream);
		}
		
	}
	
	private void writeSootResolvedData() {
		if(data != null) {
			info = new ArrayList<>();
			Map<EntryPoint, EntryPointInfo> sorted = SortingMethods.sortMapKeyAscending(data);
			info.addAll(sorted.values());
		}
	}

	@Override
	public synchronized void resetSootResolvedData() {
		writeSootResolvedData();
		data = null;
	}
	
	@Override 
	public synchronized void loadSootResolvedData() {
		if(data == null) {
			this.data = new HashMap<>();
			for(EntryPointInfo epC : info) {
				data.put(new EntryPoint(epC.getEntryPoint(), epC.getStub()), epC);
			}
		}
	}

}
