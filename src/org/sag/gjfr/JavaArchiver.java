package org.sag.gjfr;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sag.common.io.FileHelpers;
import org.sag.common.io.archiver.BaseArchiver;
import org.sag.gjfr.RulesFileParser.Remapper;

public class JavaArchiver extends BaseArchiver {
	
	private BlockingQueue<Throwable> errs;
	private Set<Path> existingPaths;

	private JavaArchiver(Path pathToArchive, boolean create) throws Exception {
		super(pathToArchive,create,create);
		errs = new LinkedBlockingQueue<>();
		existingPaths = new HashSet<>();
	}
	
	public static void writeToArchive(Path pathToArchive, Set<ClassEntry> classes,
			Map<Path,Map<String,String>> renamerToOldToNewClasses) throws Exception {
		List<Throwable> errs = new ArrayList<>();
		JavaArchiver arch = null;
		
		try{
			pathToArchive = FileHelpers.getNormAndAbsPath(pathToArchive);
			arch = new JavaArchiver(pathToArchive,!Files.exists(pathToArchive));
			
			for(ClassEntry ce : classes){
				arch.addTask(arch.new WriteFileTask(ce,renamerToOldToNewClasses));
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
			throwJointError(errs, "Failed to successfully write all java class files to the output archive '" 
					+ pathToArchive + "'.");
		}
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
	
	private class WriteFileTask extends Task {
		private ClassEntry ce;
		private Map<Path,Map<String,String>> renamerToOldToNewClasses;
		public WriteFileTask(ClassEntry ce, Map<Path,Map<String,String>> renamerToOldToNewClasses) {
			this.ce = ce;
			this.renamerToOldToNewClasses = renamerToOldToNewClasses;
		}
		@Override
		public void run() {
			try{
				Path filePath;
				if(ce.getClassPath() != null){
					Path dirPath;
					String[] directoryNames = ce.getClassPath().split("\\.");
					if(directoryNames.length == 1){
						dirPath = getZipFS().getPath(directoryNames[0]);
					}else{
						dirPath = getZipFS().getPath(directoryNames[0],Arrays.copyOfRange(directoryNames,1,directoryNames.length));
					}
					Files.createDirectories(dirPath);
					filePath = getZipFS().getPath(dirPath.toString(), ce.getClassName() + ".java");
				}else{
					filePath = getZipFS().getPath(ce.getClassName() + ".java");
				}
				//make sure we are not writing something written by something more important in the class path
				//and skip over those for which we have no source
				if(!isWritingOrExistingFile(filePath) && ce.getSelectedPath() != null){
					boolean errOccured = false;
					try(OutputStream os = Files.newOutputStream(filePath)){
						if(!ce.hasRenamedClassPath()) {
							Files.copy(ce.getSelectedPathPath(), os);
						} else {
							Path jarJarPath = ce.getJarJarRulesPathPath();
							Remapper remapper = RulesFileParser.getRemapper(jarJarPath);
							Map<String,String> oldToNewClassPaths = renamerToOldToNewClasses.get(jarJarPath);
							
							//Remap the package
							String thefile = com.google.common.io.Files.toString(ce.getSelectedPathPath().toFile(), StandardCharsets.UTF_8);
							Pattern pat = Pattern.compile("(package\\s+)([^;\\s]+)(\\s*;)");
							Matcher m = pat.matcher(thefile);
							StringBuffer sb = new StringBuffer();
							if(m.find()) {
								String before = m.group(1);
								String after = m.group(3);
								m.appendReplacement(sb, Matcher.quoteReplacement(before + ce.getClassPath() + after));
							}
							m.appendTail(sb);
							
							String bodyMatchStr = "";
							if(oldToNewClassPaths != null && !oldToNewClassPaths.isEmpty()) {
								StringBuilder patStr = new StringBuilder();
								boolean first = true;
								patStr.append("(");
								for(String s : oldToNewClassPaths.keySet()) {
									if(first) {
										patStr.append(Pattern.quote(s));
										first = false;
									} else {
										patStr.append("|").append(Pattern.quote(s));
									}
								}
								patStr.append(")");
								bodyMatchStr = "|" + patStr.toString();
							}
							
							//Remap the imports
							//Remap any instances of the classes that will be remapped by the jarjar rules
							//in the method body itself
							Pattern pat2 = Pattern.compile("(import\\s+|import\\s+static\\s+)([^;\\s]+)(\\s*;)"+bodyMatchStr);
							m = pat2.matcher(sb);
							sb = new StringBuffer();
							while(m.find()) {
								boolean fourthMatched = false;
								if(!bodyMatchStr.isEmpty()) {
									String t = m.group(4);
									if(t != null) {
										t = t.trim();
										String repl = oldToNewClassPaths.get(t).trim();
										if(repl == null || repl.isEmpty()) {
											throw new RuntimeException("Error: Failed to find new path for '" + t 
													+ "' for file '" + ce.getSelectedPath() + "'.");
										}
										m.appendReplacement(sb, Matcher.quoteReplacement(repl));
										fourthMatched = true;
									}
								}
								if(!fourthMatched) {
									String before = m.group(1);
									String cp = m.group(2);
									String after = m.group(3);
									cp = remapper.remap(cp);
									m.appendReplacement(sb, Matcher.quoteReplacement(before + cp + after));
								}
							}
							m.appendTail(sb);
							
							os.write(sb.toString().getBytes());
						}
						os.flush();
					}catch(Throwable t){
						errs.offer(new RuntimeException("Error: Failed to completly write the file for class '" 
								+ Objects.toString(ce.getFullClassPath()) + "' to the archive.",t));
						errOccured = true;
					}
					//Try to remove class files that did not complete writing to avoid partial files which will not load in soot
					if(errOccured && Files.exists(filePath))
						Files.delete(filePath);
				}
			} catch (Throwable t){
				errs.offer(new RuntimeException("Error: Failed to write class '" + Objects.toString(ce.getFullClassPath()) + "' to the archive.",t));
			}
		}
	}
	
}
