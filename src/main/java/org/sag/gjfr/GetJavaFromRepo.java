package org.sag.gjfr;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jboss.forge.roaster.ParserException;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.JavaUnit;
import org.jboss.forge.roaster.model.Method;
import org.jboss.forge.roaster.model.MethodHolder;
import org.sag.common.concurrent.CountingThreadExecutor;
import org.sag.common.io.FileHelpers;
import org.sag.common.io.archiver.SimpleArchiver;
import org.sag.common.logging.ILogger;
import org.sag.common.logging.LoggerWrapperSLF4J;
import org.sag.common.tools.SortingMethods;
import org.sag.common.tuple.Pair;
import org.sag.common.tuple.Triple;
import org.sag.gjfr.RulesFileParser.Remapper;
import org.sag.main.AndroidInfo;
import org.sag.main.config.Config;

public class GetJavaFromRepo {
	
	private static final String pathSep = "/"; //zipfs separator is always /
	private static final PrintStream out = System.out;
	
	private Path rootRepoPath;
	private Path jarPath;
	private AndroidInfo ai;
	private Set<ClassEntry> entries;
	private Map<String,Set<PossibleEntry>> classPathsToJavaFilePaths;
	private Set<String> frameworkPkgs;
	private Map<Path,Pair<Set<String>,List<String>>> filesToClasses;
	private ILogger logger;
	private final Config config;
	
	public GetJavaFromRepo() {
		this.rootRepoPath = null;
		this.jarPath = null;
		this.ai = null;
		this.entries = new HashSet<>();
		this.classPathsToJavaFilePaths = new HashMap<>();
		this.frameworkPkgs = new LinkedHashSet<>();
		this.filesToClasses = new HashMap<>();
		this.logger = new LoggerWrapperSLF4J(this.getClass());
		this.config = Config.getConfigFromResources(logger);
	}
	
	private void getAllOuterMostClasses() {
		Set<String> jimpleFiles;
		try {
			jimpleFiles = SimpleArchiver.findInArchive(jarPath, pathSep, "*.jimple", null, "f", null, null);
			for(String path : jimpleFiles) {
				path = path.trim();
				path = path.startsWith(pathSep) ? path.substring(1) : path;
				String className = com.google.common.io.Files.getNameWithoutExtension(path);
				if(!className.contains("$")) {
					String classPath = getParent(path,pathSep).replace(pathSep, ".");
					entries.add(new ClassEntry(className, classPath));
				}
			}
			entries = SortingMethods.sortSet(entries);
		} catch(Throwable t) {
			throw new RuntimeException("Error: Something went wrong when generating the needed classes list.",t);
		}
	}
	
	private String getParent(String path, String pathSep) {
		int index = path.lastIndexOf(pathSep);
		if(index < 0)
			return null;
		return path.substring(0, index);
	}
	
	private void getClassPathsToJavaFilePaths() {
		CountingThreadExecutor exe = null;
		final List<Throwable> errs = new ArrayList<>();
		final Set<Path> removedStubPaths = new HashSet<>();
		try {
			Set<Path> javaFilePaths = FileHelpers.find(rootRepoPath, "*.java", null, "f", null, null); //already sorted
			Set<Path> jarjarRules = FileHelpers.find(rootRepoPath, "jarjar-rules.txt", null, "f", null, null);
			final Map<Path,Remapper> remappers = new HashMap<>();
			for(Path jjrule : jarjarRules) {
				remappers.put(jjrule,RulesFileParser.getRemapper(jjrule));
			}
			
			/* Hack for when external/conscrypt has a jarjar-rules.txt that is getting applied to something in
			 * out/target/common/gen/conscrypt
			 * 
			 * Also hack for external/protobuf which does not have a jarjar-rules.txt file but apparently the one
			 * at frameworks/base/proto/jarjar-rules.txt is applied to the files in this directory and I cannot
			 * find the indication of where it is actually being applied
			 * 
			 * Also hack for external/protobuf/javanano which does not have a jarjar-rules.txt file but the one
			 * in frameworks/opt/telephony gets applied (valid for api 26 and 27)
			 */
			final Path rootExternal = FileHelpers.getPath(rootRepoPath, "external");
			final Path rootOutGen = FileHelpers.getPath(rootRepoPath, "out", "target", "common", "gen");
			final Path rootProto = FileHelpers.getPath(rootRepoPath, "frameworks", "base", "proto");
			final Path rootTelephonyProto = FileHelpers.getPath(rootRepoPath, "frameworks", "opt", "telephony");
			
			//Remove all java files found in the stubs directories as these are not the source files but stubs created for API purposes
			Set<Path> stubsDirs = new HashSet<>();
			stubsDirs.add(FileHelpers.getPath(rootRepoPath, "out", "target", "common", "obj", "JAVA_LIBRARIES", "android_stubs_current_intermediates"));
			stubsDirs.add(FileHelpers.getPath(rootRepoPath, "out", "target", "common", "obj", "JAVA_LIBRARIES", "android_system_stubs_current_intermediates"));
			stubsDirs.add(FileHelpers.getPath(rootRepoPath, "out", "target", "common", "obj", "JAVA_LIBRARIES", "android_test_stubs_current_intermediates"));
			stubsDirs.add(FileHelpers.getPath(rootRepoPath, "out", "target", "common", "obj", "JAVA_LIBRARIES", "api-stubs_intermediates"));
			stubsDirs.add(FileHelpers.getPath(rootRepoPath, "out", "target", "common", "obj", "JAVA_LIBRARIES", "system-api-stubs_intermediates"));
			stubsDirs.add(FileHelpers.getPath(rootRepoPath, "out", "target", "common", "obj", "JAVA_LIBRARIES", "test-api-stubs_intermediates"));
			for(Iterator<Path> it = javaFilePaths.iterator(); it.hasNext();) {
				Path cur = it.next();
				for(Path stubDir : stubsDirs) {
					if(cur.startsWith(stubDir)) {
						it.remove();
						break;
					}
				}
			}
			
			exe = new CountingThreadExecutor();
			for(final Path filePath : javaFilePaths) {
				exe.execute(new Runnable() {
					@Override
					public void run() {
						Set<Path> jjrules = new LinkedHashSet<>();
						for(Path jarjarRule : jarjarRules) {
							Path parent = jarjarRule.getParent();
							
							
							Path externalFromJarJar = null;
							Path outGenDir = null;
							Path externalFromFilePath = null;
							if(jarjarRule.startsWith(rootExternal))
								externalFromJarJar = rootExternal.relativize(jarjarRule);
							if(filePath.startsWith(rootOutGen))
								outGenDir = rootOutGen.relativize(filePath);
							if(filePath.startsWith(rootExternal))
								externalFromFilePath = rootExternal.relativize(filePath);
							
							if(filePath.startsWith(parent) ||
									(externalFromJarJar != null && outGenDir != null && externalFromJarJar.subpath(0, 1).equals(outGenDir.subpath(0, 1))) ||
									(externalFromFilePath != null && externalFromFilePath.subpath(0, 1).toString().equals("protobuf") && jarjarRule.startsWith(rootProto)) ||
									(externalFromFilePath != null && externalFromFilePath.subpath(0, 1).toString().equals("protobuf") && 
										externalFromFilePath.subpath(1, 2).toString().equals("javanano") && jarjarRule.startsWith(rootTelephonyProto)
									)
							) {
								jjrules.add(jarjarRule); 
								// There can be multiple jar jar rule files because they are no longer following the 
								// restriction that a jar jar rule must be in the outer most directory of the files
								// it is manipulating. 
							}
						}
						
						// A triple consists of a single fully qualified class path for the current filePath, 
						// the file path to the jar jar file used to transform it, and the original class path before
						// the jar jar rule is applied or a single fully qualified
						// class path and nulls indicating that no jarjar file was used.
						Set<Triple<String, String, String>> fullyQualifiedClassPaths = new HashSet<>();
						try (InputStream in = Files.newInputStream(filePath)) {
							JavaUnit unit = Roaster.parseUnit(in);
							
							boolean isStubClass = true;
							boolean hasAtLeastOneStubException = false;
							for(JavaType<?> t : unit.getTopLevelTypes()) {
								boolean isStubClassInner = true; //Assume stub class
								if(t instanceof MethodHolder) {
									List<? extends Method<?,?>> methods = ((MethodHolder<?>)t).getMethods();
									if(methods != null) {
										int stubMethods = 0;
										int noBodyMethods = 0;
										for(Method<?,?> m : methods) {
											String body = m.getBody();
											if(body != null && !(body = body.trim()).isEmpty()) {
												if(body.endsWith("throw new RuntimeException(\"Stub!\");")) {
													hasAtLeastOneStubException = true;
													stubMethods++;
												}
											} else {
												noBodyMethods++;
											}
										}
										if(methods.size() == noBodyMethods || methods.size() != (noBodyMethods + stubMethods) || methods.size() == 0)
											isStubClassInner = false;
									} else {
										isStubClassInner = false;
									}
								} else {
									isStubClassInner = false;
								}
								isStubClass &= isStubClassInner;
							}
							
							if(hasAtLeastOneStubException && !isStubClass) {
								out.println("Warning: Detected stub class indicator in '" + filePath + "' but decided it is not a stub class.");
							}
							if(isStubClass) {
								synchronized(removedStubPaths) {
									removedStubPaths.add(filePath);
								}
							}
							
							if(!isStubClass) {
								for(JavaType<?> t : unit.getTopLevelTypes()) {
									final String fullName = t.getQualifiedName();
									if(!jjrules.isEmpty()) {
										for(Path jjrule : jjrules) {
											String newFullName = remappers.get(jjrule).remap(fullName);
											if(!newFullName.equals(fullName))
												fullyQualifiedClassPaths.add(new Triple<String, String, String>(newFullName,jjrule.toString(),fullName));
										}
									}
									// If there are no jjrules this will just add the normal fullName
									// If there are jjrules this will add the original fullName because 
									// sometimes the old and new class paths appear at the same time
									fullyQualifiedClassPaths.add(new Triple<String, String, String>(fullName,null,null));
								}
								add(fullyQualifiedClassPaths);
							}
						} catch(Throwable t) {
							if(t instanceof ParserException) {
								out.println("Warning: Unable to parse file '" + filePath + "'.");
							} else {
								synchronized(errs) {
									errs.add(new RuntimeException("Error: Something went wrong when evaluating file '" + filePath + "'.",t));
								}
							}
						}
					}
					private void add(Set<Triple<String,String,String>> fullyQualifiedClassPaths) {
						synchronized(classPathsToJavaFilePaths) {
							for(Triple<String,String,String> p : fullyQualifiedClassPaths) {
								String classPath = p.getFirst();
								String jarjarpath = p.getSecond();
								String orgclasspath = p.getThird();
								Set<PossibleEntry> ps = classPathsToJavaFilePaths.get(classPath);
								if(ps == null) {
									ps = new HashSet<>();
									classPathsToJavaFilePaths.put(classPath, ps);
								}
								ps.add(new PossibleEntry(filePath.toString(),jarjarpath,orgclasspath));
							}
						}
					}
				});
			}
		} catch(Throwable t) {
			synchronized(errs) {
				errs.add(t);
			}
		} finally {
			boolean success = true;
			if(exe != null) {
				success = exe.shutdownWhenFinished();
			}
			List<Throwable> snapshot = new ArrayList<>();
			synchronized(errs) {
				snapshot.addAll(errs);
				if(exe != null) {
					snapshot.addAll(exe.getAndClearExceptions());
				}
			}
			if(!snapshot.isEmpty()) {
				CountingThreadExecutor.throwJointUnckeckedError(snapshot, 
						"Something went wrong when generating class path to java file paths list.");
			} else if(!success) {
				throw new RuntimeException("Error: Failed to properly close the counting thread executor.");
			}
		}
		
		//If a directory contains a stub file, there is a high probability that it is contained within a higher
		//directory that contains a whole bunch of stub files. So if we can discover this containing directory,
		//we can remove all files in it and hopefully cover those we missed because they contained no method
		//bodies that indicate them as stub files.
		Path libPath = FileHelpers.getPath(rootRepoPath, "out", "target", "common", "obj", "JAVA_LIBRARIES");
		Set<Path> stubDirs = new HashSet<>();
		for(Path removedStubFile : removedStubPaths) {
			if(removedStubFile.startsWith(libPath)) {
				Path realitivePath = libPath.relativize(removedStubFile);
				stubDirs.add(FileHelpers.getPath(libPath,realitivePath.getName(0)));
			} else {
				boolean found = false;
				Path cur = removedStubFile.getRoot();
				for(int i = 0; i < removedStubFile.getNameCount() - 1; i++) {
					cur = FileHelpers.getPath(cur, removedStubFile.getName(i));
					if(removedStubFile.getName(i).toString().contains("stub")) {
						found = true;
						break;
					}
				}
				if(found)
					stubDirs.add(cur);
			}
		}
		for(Iterator<Entry<String,Set<PossibleEntry>>> it = classPathsToJavaFilePaths.entrySet().iterator(); it.hasNext();) {
			Entry<String, Set<PossibleEntry>> e = it.next();
			Set<PossibleEntry> entries = e.getValue();
			for(Iterator<PossibleEntry> itt = entries.iterator(); itt.hasNext();) {
				PossibleEntry entry = itt.next();
				Path cur = entry.getFilePath2();
				for(Path stubDir : stubDirs) {
					if(cur.startsWith(stubDir)) {
						itt.remove();
						break;
					}
				}
			}
			if(entries.isEmpty())
				it.remove();
		}
		
		for(String s : classPathsToJavaFilePaths.keySet()) {
			classPathsToJavaFilePaths.put(s, SortingMethods.sortSet(classPathsToJavaFilePaths.get(s)));
		}
		classPathsToJavaFilePaths = SortingMethods.sortMapKey(classPathsToJavaFilePaths, SortingMethods.sComp);
	}

	private void insertPossibleSourceFiles() {
		for(ClassEntry ce : entries) {
			Set<PossibleEntry> possiblePaths = classPathsToJavaFilePaths.get(ce.getFullClassPath());
			if(possiblePaths == null || possiblePaths.isEmpty()) {
				out.println("Warning: Class '" + ce.getFullClassPath() + "' has no possible source files.");
			} else {
				ce.setPossiblePaths(possiblePaths);
				if(possiblePaths.size() == 1) {
					PossibleEntry p = possiblePaths.iterator().next();
					ce.setSelectedPath(p.getFilePath());
					if(p.getJarJarPath() != null) {
						ce.setJarJarRulesPath(p.getJarJarPath());
						ce.setOldClassPath(p.getOrgClassPath());
					}
				}
			}
		}
	}
	
	private void selectKnownSourceDirectories() {
		try {
			Set<Path> knownSourceDirectories = new HashSet<>();
			knownSourceDirectories.add(FileHelpers.getPath(rootRepoPath, "libcore", "ojluni"));
			knownSourceDirectories.add(FileHelpers.getPath(rootRepoPath, "libcore", "luni"));
			knownSourceDirectories.add(FileHelpers.getPath(rootRepoPath, "frameworks", "base"));
			knownSourceDirectories.add(FileHelpers.getPath(rootRepoPath, "out", "soong", ".intermediates", "frameworks", "base", "framework", "android_common"));
			
			int notSelectedBefore = 0;
			int selected = 0;
			for(ClassEntry ce : entries) {
				if(ce.getSelectedPath() == null) {
					notSelectedBefore++;
					Set<PossibleEntry> possiblePaths = ce.getPossiblePaths();
					int foundCount = 0;
					PossibleEntry found = null;
					for(PossibleEntry e : possiblePaths) {
						Path path = e.getFilePath2();
						for(Path known : knownSourceDirectories) {
							if(path.startsWith(known)) {
								foundCount++;
								found = e;
								break;
							}
						}
					}
					if(foundCount == 1) {
						ce.setSelectedPath(found.getFilePath());
						ce.setJarJarRulesPath(found.getJarJarPath());
						ce.setOldClassPath(found.getOrgClassPath());
						selected++;
					}
				}
			}
			
			out.println("Info: Known source directories reduced the number without selected source from " + notSelectedBefore + " to " 
					+ (notSelectedBefore - selected) + " of " + entries.size() + ".");
		} catch(Throwable t) {
			throw new RuntimeException("Error: Something went wrong in known source directories",t);
		}
	}
	
	private void guessSourceUsingJackSourceList() {
		if(AndroidInfo.isBetween(ai.getApi(), 23, 28)) {
			int notSelectedBefore = 0;
			for(ClassEntry ce : entries) {
				if(ce.getSelectedPath() == null)
					notSelectedBefore++;
			}
			Path rootLibs = FileHelpers.getPath(rootRepoPath, "out","target","common","obj","JAVA_LIBRARIES");
			for(String fwpkg : frameworkPkgs) {
				boolean exists = false;
				Path cur = FileHelpers.getPath(rootLibs, fwpkg + "_intermediates");
				if(FileHelpers.checkRWDirectoryExists(cur) || 
						(ai.getApi() >= 28 && FileHelpers.checkRWDirectoryExists(cur = FileHelpers.getPath(rootLibs, fwpkg + "lib_intermediates"))))
					exists = true;
				
				if(exists) {
					try {
						String fileName = "jack-rsc.java-source-list";
						if(ai.getApi() >= 28)
							fileName = "java-source-list";
						Path jack = FileHelpers.findFirstFile(cur, fileName, null); //Should only be one file
						if(jack != null) {
							Set<Path> paths = new LinkedHashSet<>();
							try(BufferedReader br = Files.newBufferedReader(jack)) {
								String line = null;
								while((line = br.readLine()) != null) {
									line = line.trim();
									if(!line.isEmpty())
										paths.add(FileHelpers.getPath(rootRepoPath, line));
								}
							}
							for(ClassEntry ce : entries) {
								if(ce.getSelectedPath() == null) {
									Set<PossibleEntry> possiblePaths = ce.getPossiblePaths();
									for(Path p : paths) {
										String path = p.toString();
										for(PossibleEntry e : possiblePaths) {
											if(e.getFilePath().equals(path)) {
												ce.setSelectedPath(e.getFilePath());
												ce.setJarJarRulesPath(e.getJarJarPath());
												ce.setOldClassPath(e.getOrgClassPath());
												break;
											}
										}
									}
								}
							}
						}
					} catch(Throwable t) {
						throw new RuntimeException("Error: Something went wrong when exploring '" + cur + "' for jack source list.",t);
					}
				} else {
					out.println("Warning: Could not find framework package directory '" + fwpkg + "'.");
				}
			}
			int notSelectedAfter = 0;
			for(ClassEntry ce : entries) {
				if(ce.getSelectedPath() == null)
					notSelectedAfter++;
			}
			out.println("Info: The source file lists reduced the number without selected source from " + notSelectedBefore + " to " 
					+ notSelectedAfter + " of " + entries.size() + ".");
		}
	}
	
	private void populateFilesToClassesAndSetOldClassPath() {
		for(ClassEntry ce : entries) {
			Path p = ce.getSelectedPathPath();
			if(p != null) {
				Pair<Set<String>, List<String>> data = filesToClasses.get(p);
				if(data == null) {
					data = new Pair<Set<String>, List<String>>(new HashSet<String>(), new ArrayList<String>());
					filesToClasses.put(p, data);
					try (InputStream in = Files.newInputStream(p)) {
						JavaUnit unit = Roaster.parseUnit(in);
						for(JavaType<?> t : unit.getTopLevelTypes()) {
							data.getSecond().add(t.getName());
						}
					} catch(Throwable t) {
						throw new RuntimeException("Error: Failed to parse java file '" + p + "'.",t);
					}
				}
				data.getFirst().add(ce.getClassPath());
			}
		}
		for(ClassEntry ce : entries) {
			String p = ce.getSelectedPath();
			if(p != null && ce.getOldClassPath() == null) {
				for(PossibleEntry pe : ce.getPossiblePaths()) {
					if(pe.getFilePath().equals(p)) {
						ce.setOldClassPath(pe.getOrgClassPath());
						ce.setJarJarRulesPath(pe.getJarJarPath());
						break;
					}
				}
			}
		}
	}
	
	private void getRidOfDuplicateFiles() {
		Set<ClassEntry> newEntries = new HashSet<>();
		Map<Path,List<ClassEntry>> map = new HashMap<>();
		for(ClassEntry ce : entries) {
			Path p = ce.getSelectedPathPath();
			if(p != null) { //we will ignore those ce without a selected path as these will not be output
				List<ClassEntry> set = map.get(p);
				if(set == null) {
					set = new ArrayList<>();
					map.put(p, set);
				}
				set.add(ce);
			}
		}
		
		for(Path p : map.keySet()) {
			List<ClassEntry> ces = map.get(p);
			if(ces.size() <= 1) {
				newEntries.add(ces.get(0));
			} else {
				/* There are multiple entries linking to the same file. This means either 
				 * there are multiple classes in the same file, the class path was renamed 
				 * and then both a class with the original class path and the new class path
				 * were included, or a mix of the first two (i.e. the class path was renamed
				 * for a file with multiple classes in it.
				 * 
				 * So group the ClassEntries by there class path. If there ends up being more
				 * than one ClassEntry per group this implies that there are multiple classes
				 * in a file so we probably need to keep one and discard the rest.
				 */
				Map<String,List<ClassEntry>> classPathToCe = new HashMap<>(); //ClassEntries are unique so use list instead of set
				for(ClassEntry ce : ces) {
					List<ClassEntry> temp = classPathToCe.get(ce.getClassPath());
					if(temp == null) {
						temp = new ArrayList<>();
						classPathToCe.put(ce.getClassPath(), temp);
					}
					temp.add(ce);
				}
				
				for(String classPath : classPathToCe.keySet()) {
					List<ClassEntry> group = classPathToCe.get(classPath);
					//Only one entry for this specific class path so no conflicts will be generated
					if(group.size() <= 1) { 
						newEntries.add(group.get(0));
					} else {
						//Multiple entries with the same class path pointing to the same file
						//Implies multiple top level classes per file
						Pair<Set<String>,List<String>> data = filesToClasses.get(p);
						List<String> classesInFile = data.getSecond();
						String firstTopClass = com.google.common.io.Files.getNameWithoutExtension(p.toString());
						if(!classesInFile.contains(firstTopClass))
							firstTopClass = classesInFile.get(0);
						ClassEntry entryToKeep = null;
						//Double check that all entries point to classes in the selected file
						for(ClassEntry ce : group) {
							if(!classesInFile.contains(ce.getClassName())) {
								throw new RuntimeException("Error: The selected path '" + p 
										+ "' does not contain the class '" + ce.getFullClassPath() + "'.");
							}
							if(ce.getClassName().equals(firstTopClass)) {
								//Remember that ClassEntry's are unique so there should only be one with
								//a given name per class path (i.e. per group), this this should only happen once per group
								entryToKeep = ce;
							}
						}
						
						if(entryToKeep != null) 
							newEntries.add(entryToKeep);
						else
							throw new RuntimeException("Error: The top level class named '" + firstTopClass 
									+ "' is not included in the entries linked to '" + p 
									+ "' for class path '" + classPath + "'.");
						
					}
					
				}
			}
		}
		
		out.println("Info: Removed " + (entries.size() - newEntries.size()) 
				+ " entries with mutiple top level classes in the same source file. There are now " 
				+ newEntries.size() + " entries.");
		entries = SortingMethods.sortSet(newEntries);
	}
	
	private void includeMissingStandardJavaClasses() {
		Path coreLambda = FileHelpers.getPath(rootRepoPath, "out","target","common","obj",
				"JAVA_LIBRARIES","core-lambda-stubs_intermediates","jack-rsc.java-source-list");
		try(BufferedReader br = Files.newBufferedReader(coreLambda)) {
			String line;
			while((line = br.readLine()) != null) {
				line = line.trim();
				Path p = FileHelpers.getPath(rootRepoPath,line);
				String name = com.google.common.io.Files.getNameWithoutExtension(line);
				String firstName = null;
				String classPath = null;
				boolean foundName = false;
				try (InputStream in = Files.newInputStream(p)) {
					JavaUnit unit = Roaster.parseUnit(in);
					for(JavaType<?> t : unit.getTopLevelTypes()) {
						if(firstName == null) {
							firstName = t.getName();
							classPath = t.getPackage();
						}
						if(name.equals(t.getName()))
							foundName = true;
					}
				}
				if(!foundName)
					name = firstName;
				
				ClassEntry newce = new ClassEntry(name, classPath);
				newce.setSelectedPath(p.toString());
				newce.addPossiblePath(new PossibleEntry(p.toString()));
				
				boolean alreadyExists = false;
				for(ClassEntry ce : entries) {
					if(ce.getFullClassPath().equals(newce.getFullClassPath()))
						alreadyExists = true;
				}
				if(!alreadyExists)
					entries.add(newce);
			}
		} catch(Throwable t) {
			throw new RuntimeException("Error: Failed to parse '" + coreLambda + "'.",t);
		}
		entries = SortingMethods.sortSet(entries);
	}
	
	private void makeJavaJar() {
		populateFilesToClassesAndSetOldClassPath();
		getRidOfDuplicateFiles();
		// Apparently it is only android before 8 that need this
		if(AndroidInfo.isBetween(ai.getApi(), 23, 25))
			includeMissingStandardJavaClasses();
		Map<Path,Map<String,String>> renamerToOldToNewClasses = new HashMap<>();
		for(ClassEntry ce : entries) {
			Path p = ce.getJarJarRulesPathPath();
			if(p != null) {
				Map<String,String> map = renamerToOldToNewClasses.get(p);
				if(map == null) {
					map = new HashMap<>();
					renamerToOldToNewClasses.put(p, map);
				}
				map.put(ce.getOldFullClassPath(), ce.getFullClassPath());
			}
		}
		Path systemJavaJar = config.getFilePath("gjfr_system-java-jar-file");
		try {
			if(FileHelpers.checkRWFileExists(systemJavaJar)) {
				Files.deleteIfExists(systemJavaJar);
			}
			JavaArchiver.writeToArchive(systemJavaJar, entries, renamerToOldToNewClasses);
		} catch (Throwable t) {
			throw new RuntimeException("Error: Failed to write java jar to '" 
					+ systemJavaJar + "'.",t);
		}
	}
	
	
	
	private static void runFirstStage(String inPath, String repoPath) {
		GetJavaFromRepo g = new GetJavaFromRepo();
		g.verifyAndSetInput(inPath, repoPath);
		g.getAllOuterMostClasses();
		g.getClassPathsToJavaFilePaths();
		g.insertPossibleSourceFiles();
		g.guessSourceUsingJackSourceList();
		g.selectKnownSourceDirectories();
		
		boolean canOutput = true;
		for(ClassEntry ce : g.entries) {
			if(ce.getSelectedPath() == null) {
				canOutput = false;
				break;
			}
		}
		
		if(canOutput) {
			out.println("Info: All entries have a selected source path. Outputting jar.");
			g.makeJavaJar();
		} else {
			out.println("Info: Some entries do not have a selected source path. Dumping all entries to xml. "
					+ "Please modify the xml to select a source path and rerun again with the updated data.");
			try {
				ClassEntry.Wrapper.writeXMLStatic(g.entries, g.config.getFilePath("gjfr-dir").toString(), 
						g.rootRepoPath.toString(), null, g.config.getFilePath("gjfr_system-java-xml-file"));
			} catch(Throwable t) {
				throw new RuntimeException("Error: Something went wrong when dumping to xml '" 
						+ g.config.getFilePath("gjfr_system-java-xml-file") + "'.",t);
			}
		}
	}
	
	private static void runSecondStage(String inFile) {
		GetJavaFromRepo g = new GetJavaFromRepo();
		ClassEntry.Wrapper w = null;
		try {
			w = ClassEntry.Wrapper.readXMLStatic(inFile, null);
		} catch (Throwable t) {
			throw new RuntimeException("Error: Failed to read in data from file '" + inFile + "'.",t);
		}
		g.verifyAndSetInput(w.getInputDir(), w.getRootRepoDir());
		g.entries = w.getEntries();
		int nullCount = 0;
		for(ClassEntry ce : g.entries) {
			if(ce.getSelectedPath() == null)
				nullCount++;
		}
		out.println("Info: The number without selected source is " + nullCount + " out of " + g.entries.size() + ".");
		g.makeJavaJar();
		for(ClassEntry ce : g.entries) {
			out.println(ce);
		}
	}
	
	
	
	public static void main(String[] args) {
		if(args == null || args.length <= 0) {
			out.println(errmsg);
			throw new RuntimeException("Error: No argument given.");
		}else{
			boolean isStart = false;
			boolean isComplete = false;
			String inDir = null;
			String rootPath = null;;
			String inFile = null;;
			for(int i = 0; i < args.length; i++) {
				switch(args[i]) {
					case "-h":
					case "--help":
						out.println(errmsg);
						break;
					case "-s":
						inDir = args[++i];
						rootPath = args[++i];
						isStart = true;
						break;
					case "-o":
						inFile = args[++i];
						isComplete = true;
						break;
					default:
						out.println(errmsg);
						throw new RuntimeException("Error: Invalid Input '" + args[i] +"'.");
				}
			}
			
			if(isStart && isComplete) {
				out.println(errmsg);
				throw new RuntimeException("Error: Both -s and -o cannot be used at the same time.");
			} else if(isStart) {
				runFirstStage(inDir, rootPath);
			} else if(isComplete) {
				runSecondStage(inFile);
			} else {
				out.println(errmsg);
				throw new RuntimeException("Error: Either -s or -o must be supplied.");
			}
		}
		
	}
	
	public void verifyAndSetInput(String inPath, String repoPath){
		if(inPath.length() > 0 && inPath.charAt(inPath.length()-1) == File.separatorChar){
			inPath = inPath.substring(0, inPath.length()-1);
		}
		
		if(repoPath.length() > 0 && repoPath.charAt(repoPath.length()-1) == File.separatorChar){
			repoPath = repoPath.substring(0, repoPath.length()-1);
		}
		
		try {
			rootRepoPath = FileHelpers.getPath(repoPath);
			FileHelpers.processDirectory(rootRepoPath, false, false);
		} catch(Throwable t) {
			throw new RuntimeException("Error: Could not access the root repo directory provided at '" + repoPath + "'.",t);
		}
		
		config.setFilePathEntry("work-dir", inPath);
		try{
			FileHelpers.processDirectory(config.getFilePath("work-dir"),false,false);
		} catch(Throwable t) {
			throw new RuntimeException("Error: Could not access the root directory.",t);
		}
		
		try{
			FileHelpers.processDirectory(config.getFilePath("gjfr-dir"),true,false);
		} catch(Throwable t) {
			throw new RuntimeException("Error: Could not access the gjfr directory in the root directory.",t);
		}
		
		Path androidInfo = config.getFilePath("work_android-info-file");
		try {
			FileHelpers.verifyRWFileExists(androidInfo);
		} catch(Throwable t) {
			throw new RuntimeException("Error: Could not access the AndroidInfo file at path '"
					+ androidInfo + "'.");
		}
		
		try{
			ai = AndroidInfo.readXMLStatic(null, androidInfo);
		} catch (Throwable t) {
			throw new RuntimeException("Error: Could not read in the AndroidInfo file at path '" 
					+ androidInfo + "'.",t);
		}
		
		Path p = config.getFilePath("sje_framework-pkgs-file");
		try {
			FileHelpers.verifyRWFileExists(p);
			try(BufferedReader br = Files.newBufferedReader(p)) {
				String line = null;
				while((line = br.readLine()) != null) {
					line = line.trim();
					if(!line.isEmpty()) {
						frameworkPkgs.add(line);
					}
				}
			}
		} catch(Throwable t) {
			throw new RuntimeException("Error: Unable to read in the framework packages file '" 
					+ p + "'.");
		}
	}
	
	//TODO
	private static final String errmsg = 
			  "Usage: [-h|--help] [-o <dir>] [-m [-i <dir>] [--disableNativeWrapperIndicators]\n"
			+ "       [--minerDefaults] [--minerDefaultsWithDebugging]\n"
			+ "       [--minerDefaultsWithFullDebugging] [-b [--onlyClasses <file> |\n"
			+ "       --onlyStubs <file>] [--startAtProtectedOperations] [--removeLoops]\n"
			+ "       [--removeRecursion] [--suppressStdOut]\n"
			+ "       [--enableDebugOutput [--onlyKeepDebugOutputForErrors]\n"
			+ "       [--forceDebugOutputToConsole]]] [-pp <name> <args>]]\n"
			+ "       [-p <names> <input_files> <options>]\n"
			;
	
}
