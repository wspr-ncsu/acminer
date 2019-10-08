package org.sag.common.io.walker;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;

import org.sag.common.io.FileHelpers;

public class MTDeleteDirectoryTask extends MTDirectoryWalkTask {

	private static final long serialVersionUID = 4807539011833596678L;

	public MTDeleteDirectoryTask(Path dir) {
		super(dir);
	}
	
	public MTDeleteDirectoryTask(Path dir, MTDeleteDirectoryTask org){
		super(dir, org);
	}
	
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		Objects.requireNonNull(file);
		Objects.requireNonNull(attrs);
		MTDeleteFileVisitTask a = new MTDeleteFileVisitTask(file, attrs, errs);
		a.fork();
		actions.add(a);
		return FileVisitResult.CONTINUE;
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
		}else{
			errs.offer(new Exception("Error: Could not delete non-empty directory '" + dir + "'."));
		}
	}
	
	@Override
	public MTDeleteDirectoryTask getNewThreadedDirectoryWalk(Path dir){
		Objects.requireNonNull(dir);
		return new MTDeleteDirectoryTask(dir,this);
	}
	
	public static class MTDeleteFileVisitTask extends MTFileVisitTask {
		
		private static final long serialVersionUID = -2116280147815325621L;

		public MTDeleteFileVisitTask(Path file, BasicFileAttributes attrs, BlockingQueue<Throwable> errs) {
			super(file, attrs, errs);
		}

		@Override
		protected void computeInner() throws Exception {
			FileHelpers.verifyRWFileExists(file);
			Files.delete(file);
		}
		
	}

}
