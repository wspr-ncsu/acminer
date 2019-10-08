package org.sag.sje.soot;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Method;
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
import org.sag.common.logging.ILogger;
import org.sag.main.logging.CentralLogger;
import org.sag.sootinit.BasicSootLoader;

import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.dava.DavaPrinter;
import soot.dava.toolkits.base.misc.PackageNamer;
import soot.options.Options;
import soot.tagkit.InnerClassTagAggregator;
import soot.toolkits.scalar.ConstantInitializerToTagTransformer;

public class JimpleToJavaArchiver extends BaseArchiver {
	
	private BlockingQueue<Throwable> errs;
	private Set<Path> existingPaths;
	
	public static void main(String[] args) {
		ILogger logger = CentralLogger.getLogger("JimpleToJavaArchiver");
		try {
			BasicSootLoader.v().load(FileHelpers.getPath(args[0]), true, Integer.parseInt(args[2]), logger);
			writeJimpleClassesToArchive(FileHelpers.getPath(args[1]));
		} catch (Throwable e) {
			logger.fatal("Error: Something went wrong",e);
		}
	}

	private JimpleToJavaArchiver(Path pathToArchive, boolean create) throws Exception {
		super(pathToArchive,create,create);
		errs = new LinkedBlockingQueue<>();
		existingPaths = new HashSet<>();
	}
	
	private static void setupDava() throws Exception {
		Options.v().set_output_format(Options.output_format_dava);
		//PhaseOptions.v().setPhaseOption("db", "source-is-javac:false");
		// Create tags from all values we only have in code assignments now
		for (SootClass sc : Scene.v().getApplicationClasses()) {
			if (Options.v().validate())
				sc.validate();
			if (!sc.isPhantom())
				ConstantInitializerToTagTransformer.v().transformClass(sc, true);
		}
		
		//preProcessDAVA()
		//ThrowFinder.v().find();
		PackageNamer.v().fixNames();
		
		PackManager.v().runBodyPacks();
		
		//handleInnerClasses()
		InnerClassTagAggregator agg = InnerClassTagAggregator.v();
		agg.internalTransform("", null);
		
		Method postProcessDAVA = PackManager.v().getClass().getDeclaredMethod("postProcessDAVA");
		postProcessDAVA.setAccessible(true);
		postProcessDAVA.invoke(PackManager.v());
	}
	
	public static void writeJimpleClassesToArchive(Path pathToJimpleArchive) throws Exception {
		setupDava();
		
		Set<SootClass> classes = new LinkedHashSet<>(Scene.v().getApplicationClasses());
		
		List<Throwable> errs = new ArrayList<>();
		JimpleToJavaArchiver arch = null;
		
		try{
			pathToJimpleArchive = FileHelpers.getNormAndAbsPath(pathToJimpleArchive);
			arch = new JimpleToJavaArchiver(pathToJimpleArchive,!Files.exists(pathToJimpleArchive));
			
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
			throwJointError(errs, "Failed to successfully write all java class files for to the output output archive '" 
					+ pathToJimpleArchive + "'.");
		}
	}
	
	private synchronized ByteArrayOutputStream writeToMemory(SootClass sc) {
		ByteArrayOutputStream ret = new ByteArrayOutputStream();
		PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(ret));
		DavaPrinter.v().printTo(sc, writerOut);
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
						errs.offer(new RuntimeException("Error: Failed to completly write the java file for class '" 
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
