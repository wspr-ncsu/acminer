package org.sag.acminer.database.defusegraph;

import java.nio.file.Path;
import java.util.List;

import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.io.FileHash;
import org.sag.common.io.FileHashList;
import org.sag.common.xstream.XStreamInOut.XStreamInOutInterface;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("IDefUseGraphDatabase")
public interface IDefUseGraphDatabase extends XStreamInOutInterface {

	public void writeAndAddDefUseGraph(EntryPoint ep, DefUseGraph graph, Path outDir) throws Exception;
	public DefUseGraph getDefUseGraph(EntryPoint ep, Path dir) throws Exception;
	public IDefUseGraphDatabase readXML(String filePath, Path path) throws Exception;
	public void resetSootResolvedData();
	public void loadSootResolvedData();
	public List<FileHash> getFileHashList();
	public void setFileHashList(FileHashList fhl);
	public boolean equals(Object o);
	public int hashCode();
	public DefUseGraph getDefUseGraphUnchecked(EntryPoint ep, Path dir) throws Exception;
	
	public static final class Factory {
		public static IDefUseGraphDatabase getNew(boolean empty) {
			if(empty)
				return new EmptyDefUseGraphDatabase();
			return new DefUseGraphDatabase(true);
		}
		public static IDefUseGraphDatabase readXML(String filePath, Path path)  throws Exception {
			return new DefUseGraphDatabase(false).readXML(filePath, path);
		}
	}

	
}
