package org.sag.common.io.walker;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.sag.common.io.FileHelpers;

public class MTDeleteEmptyDirectoryTask extends MTDirectoryWalkTask {

	private static final long serialVersionUID = -4505288071221569612L;

	public MTDeleteEmptyDirectoryTask(Path dir) {
		super(dir);
	}
	
	public MTDeleteEmptyDirectoryTask(Path dir, MTDeleteEmptyDirectoryTask org){
		super(dir, org);
	}
	
	@Override
	public void finalAction(Path dir) throws IOException {
		FileHelpers.verifyRWDirectoryExists(dir);
		boolean isEmpty;
		try(DirectoryStream<Path> ds = Files.newDirectoryStream(dir);){
			isEmpty = !ds.iterator().hasNext();
		}
		if(isEmpty){
			Files.delete(dir);
		}
	}
	
	@Override
	public MTDeleteEmptyDirectoryTask getNewThreadedDirectoryWalk(Path dir){
		Objects.requireNonNull(dir);
		return new MTDeleteEmptyDirectoryTask(dir,this);
	}

}
