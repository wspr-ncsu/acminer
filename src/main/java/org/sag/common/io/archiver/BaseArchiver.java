package org.sag.common.io.archiver;

import java.io.Closeable;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sag.common.concurrent.CountingThreadExecutor;
import org.sag.common.io.FileHelpers;
import org.sag.common.io.zipfs.ZipFileSystemProvider;

public class BaseArchiver implements Closeable {
	
	protected final static ZipFileSystemProvider zipfsProvider = new ZipFileSystemProvider();

	protected CountingThreadExecutor exe;
	protected FileSystem zipfs;
	protected List<Throwable> exceptions;
	
	protected BaseArchiver(){}
	
	/** 
	 * 
	 * @param filePath
	 * @param fileName
	 * @param create Creates the file represented by filePath/fileName if it does not exist. If it does exist it is overwritten.
	 * @param useTempFile
	 * @throws Exception 
	 */
	public BaseArchiver(Path archivePath, boolean create, boolean useTempFile) throws Exception {
		exceptions = new ArrayList<Throwable>();
		zipfs = createZipFS(archivePath,create,useTempFile);
		exe = new CountingThreadExecutor();
	}
	
	protected static FileSystem createZipFS(Path archivePath, boolean create, boolean useTempFile) throws Exception {
		if(archivePath == null)
			throw new IllegalArgumentException("Error: Please provide a path to the existing archive or the archive to be "
					+ "created.");
		
		Path fileNamePath = archivePath.getFileName();
		if(fileNamePath == null)
			throw new IllegalArgumentException("Error: Please provide a path to the existing archive or the archive to be "
					+ "created with an non-empty file name.");
		
		String fileNameString = fileNamePath.toString();
		int typeSep = fileNameString.lastIndexOf('.');
		if(typeSep < 0 || typeSep >= fileNameString.length()-1){
			throw new IllegalArgumentException("Error: Please provide a path to the existing archive or the archive to be "
					+ "created with a file name extension of zip or jar.");
		}else{
			String type = fileNameString.substring(typeSep);
			if(!type.equals(".zip") && !type.equals(".jar") && !type.equals(".apk") && !type.equals(".apex")){
				throw new IllegalArgumentException("Error: Expected file type of '.zip', '.jar', '.apk', or '.apex' but got '" + type + 
						"'.");
			}
		}
		
		archivePath = FileHelpers.getNormAndAbsPath(archivePath);
		
		//create the directories to file if needed and create is enabled
		//otherwise this just checks to make sure the directories to the file exist
		FileHelpers.processDirectory(archivePath.getParent(), create, false);
		
		Map<String, String> env = new HashMap<>();
		if(create){
			//create is enabled so we will be overwriting an existing file 
			//so delete the old one is it exists
			Files.deleteIfExists(archivePath);
			env.put("create", "true");
		}else{
			//create is disabled so just verify that the file exists
			FileHelpers.verifyRWFileExists(archivePath);
		}
		if(useTempFile) 
			env.put("useTempFile", "true");
		
		String startStr = "file:";
		if(System.getProperty("os.name").toLowerCase().contains("windows"))
			startStr = startStr + "/";
		
		//We use this custom version because there were bugs in the java 8 implementation of zipfs
		//return zipfsProvider.newFileSystem(new URI("jar",startStr+archivePath.toString(),null), env);
		
		//This is how zipfs in standard java is accessed
		//return FileSystems.newFileSystem(new URI("jar",startStr+archivePath.toString(),null), env);
		return FileSystems.newFileSystem(new URI("zipfs",startStr+archivePath.toString(),null), env);
	}

	@Override
	public void close() {
		boolean failWait = false;
		boolean failClose = false;
		if(exe != null){
			failWait = !exe.shutdownWhenFinished();
			if(exceptions != null) exceptions.addAll(exe.getAndClearExceptions());
			exe = null;
		}
		
		try {
			if(zipfs != null)
				zipfs.close();
		} catch (Throwable e) {
			if(exceptions != null) exceptions.add(e);
			failClose = true;
		}
		zipfs = null;
		
		if(failWait || failClose){
			throw new RuntimeException("Error: To get a complete list of the exceptions please call getExceptions(). "
					+ "Close failed for the following general reason(s):\n" 
					+ (failWait ? "Failed to wait for the completion of the remaining tasks and shutdown the ExecutorService.\n" : "") 
					+ (failClose ? "Failed to close the zip file.\n" : ""));
		}
	}
	
	public List<Throwable> getExceptions() {
		ArrayList<Throwable> ret = new ArrayList<Throwable>();
		if(exceptions != null) ret.addAll(exceptions);
		if(exe != null) ret.addAll(exe.getAndClearExceptions());
		return ret;
	}
	
	public void addTask(Task t){
		if(exe != null) exe.execute(t);
	}
	
	public abstract class Task implements Runnable {
		public Task() {}
		public BaseArchiver getArchiver() { return BaseArchiver.this; }
		public FileSystem getZipFS(){ return BaseArchiver.this.zipfs; }
	}

	protected static void throwJointError(List<Throwable> errs, String errMsg) throws Exception {
		CountingThreadExecutor.throwJointError(errs, errMsg);
	}
	
}
