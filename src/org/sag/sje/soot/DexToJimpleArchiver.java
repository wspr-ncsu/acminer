package org.sag.sje.soot;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.sag.common.io.FileHelpers;
import org.sag.common.io.archiver.BaseArchiver;

import soot.Printer;
import soot.SootClass;
import soot.util.EscapedWriter;

public class DexToJimpleArchiver extends BaseArchiver {
	
	private BlockingQueue<Throwable> errs;
	private Set<Path> existingPaths;

	private DexToJimpleArchiver(Path pathToArchive, boolean create) throws Exception {
		super(pathToArchive,create,create);
		errs = new LinkedBlockingQueue<>();
		existingPaths = new HashSet<>();
	}
	
	public static void writeJimpleClassesToArchive(Path pathToJimpleArchive, String archiveDescriptor, Set<SootClass> classes) throws Exception {
		List<Throwable> errs = new ArrayList<>();
		DexToJimpleArchiver arch = null;
		
		try{
			pathToJimpleArchive = FileHelpers.getNormAndAbsPath(pathToJimpleArchive);
			arch = new DexToJimpleArchiver(pathToJimpleArchive,!Files.exists(pathToJimpleArchive));
			
			for(SootClass sc : classes){
				arch.addTask(arch.new WriteJimpleClassFileTask(sc));
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
			throwJointError(errs, "Failed to successfully write all jimple class files for '" + archiveDescriptor 
						+ "' to the output jimple archive '" + pathToJimpleArchive + "'.");
		}
	}
	
	private synchronized ByteArrayOutputStream writeToMemory(SootClass sc) {
		ByteArrayOutputStream ret = new ByteArrayOutputStream();
		PrintWriter writerOut = new PrintWriter(new EscapedWriter(new OutputStreamWriter(ret)));
		Printer.v().printTo(sc, writerOut);
		writerOut.flush();
		writerOut.close();
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
	
	private class WriteJimpleClassFileTask extends Task {
		private SootClass sc;
		public WriteJimpleClassFileTask(SootClass sc) {
			this.sc = sc;
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
					filePath = getZipFS().getPath(dirPath.toString(), sc.getShortName() + ".jimple");
				}else{
					filePath = getZipFS().getPath(sc.getShortName() + ".jimple");
				}
				//make sure we are not writing something written by something more important in the class path
				if(!isWritingOrExistingFile(filePath)){
					boolean errOccured = false;
					ByteArrayOutputStream ba = null; //closing this does nothing
					try(OutputStream os = Files.newOutputStream(filePath)){
						ba = writeToMemory(sc); //write to memory in a synchronized block to avoid Soot race conditions
						ba.writeTo(os); //Output to file asynchronously for speed up
						os.flush();
					}catch(Throwable t){
						errs.offer(new RuntimeException("Error: Failed to completly write the jimple file for class '" 
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
