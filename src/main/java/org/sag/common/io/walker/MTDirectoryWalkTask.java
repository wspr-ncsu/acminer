package org.sag.common.io.walker;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;

public class MTDirectoryWalkTask extends MTFileTreeWalkerTask {

	private static final long serialVersionUID = 1966844353322488069L;
	
	protected final Path dir;
	protected BlockingQueue<Throwable> errs;
	protected final List<MTFileTreeWalkerTask> actions;
	protected int successJoinCount;
	
	public MTDirectoryWalkTask(Path dir){
		Objects.requireNonNull(dir);
		this.dir = dir;
		this.errs = null;
		this.actions = new ArrayList<>();
		this.successJoinCount = 0;
	}
	
	public MTDirectoryWalkTask(Path dir, MTDirectoryWalkTask org){
		this(dir);
		Objects.requireNonNull(org);
		this.errs = org.errs;
	}
	
	public void setErrsList(BlockingQueue<Throwable> errs){
		Objects.requireNonNull(errs);
		this.errs = errs;
	}
	
	public Path getDirectory(){
		return dir;
	}

	@Override
	protected void compute() {
		try{
			Files.walkFileTree(dir, new SimpleFileVisitor<Path>(){
				@Override
				public final FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					if(!dir.equals(MTDirectoryWalkTask.this.dir)){
						MTDirectoryWalkTask a = getNewThreadedDirectoryWalk(dir);
						a.fork();
						actions.add(a);
						return preVisitNewDirectory(dir, attrs);
					}else{
						return preVisitSameDirectory(dir, attrs);
					}
				}
				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
					return MTDirectoryWalkTask.this.postVisitDirectory(dir, e);
				}
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					return MTDirectoryWalkTask.this.visitFile(file, attrs);
				}
				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
					return MTDirectoryWalkTask.this.visitFileFailed(file, exc);
				}
			});
		}catch(Throwable t){
			errs.offer(new Exception("Error: Failed to completly walk the tree starting at directory '" + dir + "'.",t));
		}finally {
			for(MTFileTreeWalkerTask a : actions){
				try{
					a.join();
					successJoinCount++;
				}catch(Throwable t){
					errs.offer(new Exception("Error: Failed to wait on the thread exploring the sub-element '" + a.getIdentifier() 
							+ "' of directory '" + dir + "'.",t));
				}
			}
			try{
				if(successJoinCount == actions.size())
					finalAction(dir);
			}catch(Throwable t){
				errs.offer(new Exception("Error: Failed to perform the requested final action for directory '" + dir + "'.",t));
			}
		}
	}
	
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		Objects.requireNonNull(file);
		Objects.requireNonNull(attrs);
		return FileVisitResult.CONTINUE;
	}
	
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		Objects.requireNonNull(file);
		throw exc;
	}
	
	public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
		Objects.requireNonNull(dir);
		if(e != null)
			throw e;
		return FileVisitResult.CONTINUE;
	}
	
	/** Override to add actions after forking a new thread to handle a new directory and/or
	 * to change the return type from SKIP_SUBTREE to SKIP_SIBLINGS if it is required that
	 * postVisitDirectory not be run. By default this just returns SKIP_SUBTREE.
	 */
	public FileVisitResult preVisitNewDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		Objects.requireNonNull(dir);
		Objects.requireNonNull(attrs);
		return FileVisitResult.SKIP_SUBTREE;
	}
	
	/** Override to add actions for when the current directory is the one this RecursiveAction
	 * was created to traverse. By default this just returns CONTINUE.
	 */
	public FileVisitResult preVisitSameDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		Objects.requireNonNull(dir);
		Objects.requireNonNull(attrs);
		return FileVisitResult.CONTINUE;
	}
	
	/** This is performed after all spawned threads and their spawned threads have terminated. 
	 * The argument directory passed in is the directory of the current MultiThreadedRecursiveAction.
	 * It is useful if one needs to guarantee that all sub-actions of this action have been attempted
	 * before performing some task. By default, this does nothing. Note this should not spawn any more 
	 * threads and it will only be invoked if all sub-threads return without exception after a join().
	 */
	public void finalAction(Path dir) throws IOException {}
	
	public MTDirectoryWalkTask getNewThreadedDirectoryWalk(Path dir){
		Objects.requireNonNull(dir);
		return new MTDirectoryWalkTask(dir,this);
	}

	@Override
	public String getIdentifier() {
		return dir.toString();
	}
	
	public static abstract class MTFileVisitTask extends MTFileTreeWalkerTask {
		
		private static final long serialVersionUID = 3284019378007994857L;
		protected final Path file;
		protected final BasicFileAttributes attrs;
		protected final BlockingQueue<Throwable> errs;
		
		public MTFileVisitTask(Path file, BasicFileAttributes attrs, BlockingQueue<Throwable> errs){
			Objects.requireNonNull(file);
			Objects.requireNonNull(attrs);
			Objects.requireNonNull(errs);
			this.file = file;
			this.attrs = attrs;
			this.errs = errs;
		}

		@Override
		public String getIdentifier() {
			return file.toString();
		}

		@Override
		protected void compute() {
			try{
				computeInner();
			}catch(Throwable t){
				errs.offer(new Exception("Error: An exception occured when visiting and processing the file '" + file + "'.",t));
			}
		}
		
		protected abstract void computeInner() throws Exception;
	}

}
