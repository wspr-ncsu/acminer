package org.sag.acminer.database.binderservices;

import java.nio.file.Path;
import java.util.List;

import org.sag.common.io.FileHash;
import org.sag.common.io.FileHashList;
import org.sag.xstream.XStreamInOut.XStreamInOutInterface;

public interface IBinderServicesDatabase extends XStreamInOutInterface {
	
	void clearSootResolvedData();
	void loadSootResolvedData();
	List<FileHash> getFileHashList();
	void setFileHashList(FileHashList fhl);
	String toString();
	String toString(String spacer);
	int hashCode();
	boolean equals(Object o);
	
	public static final class Factory {
		
		public static IBinderServicesDatabase getNew(boolean isEmpty) {
			if(isEmpty)
				return new EmptyBinderServicesDatabase();
			return new BinderServicesDatabase(true);
		}
		
		public static IBinderServicesDatabase readXML(String filePath, Path path) throws Exception {
			return BinderServicesDatabase.readXMLStatic(filePath, path);
		}
		
	}

}
