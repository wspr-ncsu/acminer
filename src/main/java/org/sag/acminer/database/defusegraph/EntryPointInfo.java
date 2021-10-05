package org.sag.acminer.database.defusegraph;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.io.FileHash;
import org.sag.common.io.FileHelpers;
import org.sag.common.xstream.XStreamInOut;
import org.sag.common.xstream.XStreamInOut.XStreamInOutInterface;
import org.sag.soot.xstream.SootClassContainer;
import org.sag.soot.xstream.SootMethodContainer;

import soot.SootClass;
import soot.SootMethod;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("EntryPointInfo")
public class EntryPointInfo implements XStreamInOutInterface {

	@XStreamAlias("EntryPoint")
	private SootMethodContainer entryPoint;
	@XStreamAlias("Stub")
	private SootClassContainer stub;
	@XStreamAlias("FileName")
	private String fileName;
	@XStreamAlias("FileHash")
	private FileHash fileHash;
	
	private EntryPointInfo() {}
	
	public EntryPointInfo(EntryPoint ep, Path file) throws Exception {
		this.entryPoint = ep.getEntryPoint() == null ? null : SootMethodContainer.makeSootMethodContainer(ep.getEntryPoint());
		this.stub = ep.getStub() == null ? null : SootClassContainer.makeSootClassContainer(ep.getStub());
		this.fileName = file.getFileName().toString();
		this.fileHash = FileHelpers.genFileHash(file, file.getFileName());
	}
	
	public SootMethod getEntryPoint() {
		if(entryPoint == null)
			return null;
		return entryPoint.toSootMethod();
	}
	
	public SootClass getStub() {
		if(stub == null)
			return null;
		return stub.toSootClass();
	}
	
	public boolean fileExistsAndMatches(Path dir) {
		Path full = getFilePath(dir);
		if(!FileHelpers.checkRWFileExists(full))
			return false;
		try {
			return fileHash.compareHash(FileHelpers.genFileHash(full, full.getFileName()));
		} catch(Throwable t) {
			return false;
		}
	}
	
	public Path getFilePath(Path dir) {
		return FileHelpers.getPath(dir, fileName);
	}
	
	public DefUseGraph getDefUseGraph(Path dir) {
		if(fileExistsAndMatches(dir))
			return getDefUseGraphUnchecked(dir);
		return null;
	}
	
	public DefUseGraph getDefUseGraphUnchecked(Path dir) {
		try {
			return DefUseGraph.readXMLStatic(null, getFilePath(dir));
		} catch(Throwable t) {
			return null;
		}
	}
	
	@Override
	public int hashCode() {
		int i = 17;
		i = i * 31 + Objects.hashCode(entryPoint);
		i = i * 31 + Objects.hashCode(stub);
		i = i * 31 + Objects.hashCode(fileName);
		i = i * 31 + Objects.hashCode(fileHash);
		return i;
	}
	
	@Override
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof EntryPointInfo))
			return false;
		EntryPointInfo e = (EntryPointInfo)o;
		return Objects.equals(entryPoint,e.entryPoint) && Objects.equals(stub,e.stub) && Objects.equals(fileName,e.fileName) 
				&& Objects.equals(fileHash, e.fileHash);
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public EntryPointInfo readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static EntryPointInfo readXMLStatic(String filePath, Path path) throws Exception {
		return new EntryPointInfo().readXML(filePath, path);
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
				SootMethodContainer.getXStreamSetupStatic().getOutputGraph(in);
				SootClassContainer.getXStreamSetupStatic().getOutputGraph(in);
				FileHash.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(EntryPointInfo.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsXPathRelRef(xstream);
		}
		
	}
	
}
