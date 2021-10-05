package org.sag.sje.soot;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.sag.common.io.FileHelpers;
import org.sag.common.io.archiver.BaseArchiver;

import soot.Scene;
import soot.SootClass;
import soot.baf.BafASMBackend;

public class JimpleToClassArchiver extends BaseArchiver {
	
	private BlockingQueue<Throwable> errs;
	private Set<Path> existingPaths;

	private JimpleToClassArchiver(Path pathToArchive, boolean create) throws Exception {
		super(pathToArchive,create,create);
		errs = new LinkedBlockingQueue<>();
		existingPaths = new HashSet<>();
	}
	
	public static void writeJimpleClassesToArchive(Path pathToClassArchive, int javaVersion)
			throws Exception {
		Set<SootClass> classes = new LinkedHashSet<>(Scene.v().getApplicationClasses());
		List<Throwable> errs = new ArrayList<>();
		JimpleToClassArchiver arch = null;
		
		try{
			pathToClassArchive = FileHelpers.getNormAndAbsPath(pathToClassArchive);
			arch = new JimpleToClassArchiver(pathToClassArchive,!Files.exists(pathToClassArchive));
			
			for(SootClass sc : classes){
				arch.addTask(arch.new WriteClassFileTask(sc,javaVersion));
			}
		}catch(Throwable t){
			errs.add(t);
		}finally{
			try{
				if(arch != null)
					arch.close();
			} catch(Throwable t){
				errs.add(t);
			}
			if(arch != null){
				errs.addAll(arch.errs);
				errs.addAll(arch.getExceptions());
			}
		}
		
		if(!errs.isEmpty()){
			throwJointError(errs, "Failed to successfully write all class files for to the output archive '" 
					+ pathToClassArchive + "'.");
		}
	}
	
	private synchronized ByteArrayOutputStream writeToMemory(SootClass sc, int javaVersion) throws Exception {
		ByteArrayOutputStream ret = new ByteArrayOutputStream();
		new BafASMBackend(sc, javaVersion).generateClassFile(ret);
		ret.flush();
		return ret;
	}
	
	private boolean isWritingOrExistingFile(Path p) {
		synchronized(existingPaths) {
			if(existingPaths.contains(p)) {
				//File is either being written, was written, or existed already
				return true;
			} else if(Files.exists(p)) {
				//File already existed - add to list for quick lookup later
				existingPaths.add(p);
				return true;
			} else {
				//File does not exist and is not being written too - add to list because it is about to be written
				existingPaths.add(p);
				return false;
			}
		}
	}
	
	private class WriteClassFileTask extends Task {
		private SootClass sc;
		private int javaVersion;
		public WriteClassFileTask(SootClass sc, int javaVersion) {
			this.sc = sc;
			this.javaVersion = javaVersion;
		}
		@Override
		public void run() {
			try{
				Path filePath;
				if(!sc.getPackageName().equals("")){
					Path dirPath;
					String[] directoryNames = sc.getPackageName().split("\\.");
					if(directoryNames.length == 1){
						dirPath = getZipFS().getPath(directoryNames[0]);
					}else{
						dirPath = getZipFS().getPath(directoryNames[0],Arrays.copyOfRange(directoryNames,1,directoryNames.length));
					}
					Files.createDirectories(dirPath);
					filePath = getZipFS().getPath(dirPath.toString(), sc.getShortName() + ".class");
				}else{
					filePath = getZipFS().getPath(sc.getShortName() + ".class");
				}
				//make sure we are not writing something written by something more important in the class path
				if(!isWritingOrExistingFile(filePath)){
					boolean errOccured = false;
					ByteArrayOutputStream ba = null; //closing this does nothing
					try(OutputStream os = Files.newOutputStream(filePath)){
						ba = writeToMemory(sc,javaVersion); //write to memory in a synchronized block to avoid Soot race conditions
						ba.writeTo(os); //Output to file asynchronously for speed up
						os.flush();
					}catch(Throwable t){
						errs.offer(new RuntimeException("Error: Failed to completly write the class file for class '" 
								+ Objects.toString(sc) + "' to the archive.",t));
						errOccured = true;
					}
					//Try to remove class files that did not complete writing to avoid partial files which will not load in soot
					if(errOccured && Files.exists(filePath))
						Files.delete(filePath);
				}
			} catch (Throwable t){
				errs.offer(new RuntimeException("Error: Failed to write class '" + Objects.toString(sc) + "' to the archive.",t));
			}
		}
	}
	
}
