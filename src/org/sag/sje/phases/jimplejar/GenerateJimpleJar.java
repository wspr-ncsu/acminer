package org.sag.sje.phases.jimplejar;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.sag.common.io.FileHashList;
import org.sag.common.io.FileHelpers;
import org.sag.common.io.archiver.BaseArchiver;

import soot.Printer;
import soot.Scene;
import soot.SootClass;
import soot.util.EscapedWriter;

public class GenerateJimpleJar extends BaseArchiver {
	
	private final static String systemClassJarChecksumPath = "checksum.xml";

	public GenerateJimpleJar(Path archivePath) throws Exception {
		super(archivePath, true, true);
	}
	
	public static FileHashList generateArchive(Path archivePath, Path rootPath, Path fullSourceArchivePath) throws Exception {
		List<Throwable> errs = new ArrayList<>();
		GenerateJimpleJar arch = null;
		WriteChecksumOfSystemFileTask fhTask = null;
		FileHashList ret = null;
		Path realtiveSourceArchivePath = null;
		
		archivePath = FileHelpers.getNormAndAbsPath(archivePath);
		rootPath = FileHelpers.getNormAndAbsPath(rootPath);
		fullSourceArchivePath = FileHelpers.getNormAndAbsPath(fullSourceArchivePath);
		realtiveSourceArchivePath = rootPath.relativize(fullSourceArchivePath);
		
		try{
			arch = new GenerateJimpleJar(archivePath);
			
			Iterator<SootClass> classes = Scene.v().getApplicationClasses().snapshotIterator();
			while(classes.hasNext()){
				arch.addTask(arch.new WriteJimpleClassFileTask(classes.next()));
			}
			fhTask = arch.new WriteChecksumOfSystemFileTask(fullSourceArchivePath,realtiveSourceArchivePath);
			arch.addTask(fhTask);
		}catch(Throwable t){
			errs.add(t);
		}finally{
			try{
				if(arch != null){
					arch.close();
					ret = fhTask.getFileHash();
				}
			} catch(Throwable t){
				errs.add(t);
			}
			if(arch != null)
				errs.addAll(arch.getExceptions());
		}
		
		
		if(!errs.isEmpty()){
			throwJointError(errs, "Failed to generate the jimple archive '" + archivePath +"' with a root of '" + rootPath + 
					"' from the class archive '" + fullSourceArchivePath + "'.");
		}
		
		return ret;
	}
	
	public static FileHashList getSystemClassJarChecksum(Path archivePath) throws Exception {
		FileSystem zipfs = createZipFS(archivePath, false, false);
		FileHashList ret = FileHelpers.getExistingFileHashList(zipfs.getPath(systemClassJarChecksumPath));
		zipfs.close();
		return ret;
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
				PrintWriter writerOut = new PrintWriter(new EscapedWriter(new OutputStreamWriter(Files.newOutputStream(filePath))));
				Printer.v().printTo(sc, writerOut);
				writerOut.flush();
				writerOut.close();
			} catch (Throwable t){
				throw new RuntimeException("Error running WriteClassFileTask.",t);
			}
		}
	}
	
	private class WriteChecksumOfSystemFileTask extends Task {
		private Path fullSourceArchivePath;
		private Path realtiveSourceArchivePath;
		private FileHashList fh;
		public WriteChecksumOfSystemFileTask(Path fullSourceArchivePath, Path realtiveSourceArchivePath) {
			this.fullSourceArchivePath = fullSourceArchivePath;
			this.realtiveSourceArchivePath = realtiveSourceArchivePath;
		}
		@Override
		public void run() {
			try {
				fh = FileHelpers.genFileHashList(Collections.singletonList(fullSourceArchivePath), 
						Collections.singletonList(realtiveSourceArchivePath));
				fh.writeXML(null, getZipFS().getPath(systemClassJarChecksumPath));
			} catch (Throwable t) {
				throw new RuntimeException("Error running WriteChecksumOfSystemFileTask.",t);
			}
		}
		public FileHashList getFileHash(){
			return fh;
		}
	}

}
