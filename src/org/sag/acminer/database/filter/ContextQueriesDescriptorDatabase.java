package org.sag.acminer.database.filter;

import java.io.BufferedReader;
import java.io.ObjectStreamException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.sag.acminer.database.filter.entry.IEntry;
import org.sag.acminer.database.filter.matcher.SootMatcher;
import org.sag.common.io.FileHash;
import org.sag.common.io.FileHashList;
import org.sag.common.io.FileHelpers;
import org.sag.common.tools.SortingMethods;
import org.sag.soot.SootSort;
import org.sag.xstream.XStreamInOut;
import org.sag.xstream.XStreamInOut.XStreamInOutInterface;
import soot.SootMethod;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("ContextQueriesDescriptorDatabase")
public class ContextQueriesDescriptorDatabase implements XStreamInOutInterface {
	
	@XStreamAlias("FileHashList")
	private volatile FileHashList fhl;
	//XStream hack so this does not get its own name
	@XStreamImplicit
	private List<IEntry> entries;
	
	private ContextQueriesDescriptorDatabase() {}//For use with xstream only
	
	public ContextQueriesDescriptorDatabase(IEntry baseEntry) {
		entries = Collections.singletonList(baseEntry);
	}
	
	//ReadResolve is always run when reading from XML even if a constructor is run first
	protected Object readResolve() throws ObjectStreamException {
		return this;
	}
	
	protected Object writeReplace() throws ObjectStreamException {
		return this;
	}
	
	public List<FileHash> getFileHashList() {
		if(fhl == null)
			return Collections.emptyList();
		return fhl;
	}
	
	public void setFileHashList(FileHashList fhl) {
		this.fhl = fhl;
	}
	
	public boolean matches(SootMethod m) {
		return matches(new MethodData(m));
	}
	
	protected boolean matches(String signature) {
		return matches(new MethodData(signature));
	}
	
	protected boolean matchesDebug(String signature, StringBuilder sb) {
		return matchesDebug(new MethodData(signature), sb); 
	}
	
	public boolean matches(IData data) {
		return entries.get(0).eval(data);
	}
	
	public boolean matchesDebug(IData data, StringBuilder sb) {
		return entries.get(0).evalDebug(data, sb, new AtomicInteger());
	}

	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public ContextQueriesDescriptorDatabase readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static ContextQueriesDescriptorDatabase readXMLStatic(String filePath, Path path) throws Exception {
		return new ContextQueriesDescriptorDatabase().readXML(filePath, path);
	}

	@XStreamOmitField
	private static AbstractXStreamSetup xstreamSetup = null;

	public static AbstractXStreamSetup getXStreamSetupStatic(){
		if(xstreamSetup == null)
			xstreamSetup = new XStreamSetup();
		return xstreamSetup;
	}
	
	@Override
	public AbstractXStreamSetup getXStreamSetup() {
		return getXStreamSetupStatic();
	}
	
	public static class XStreamSetup extends AbstractXStreamSetup {
		
		@Override
		public void getOutputGraph(LinkedHashSet<AbstractXStreamSetup> in) {
			if(!in.contains(this)) {
				in.add(this);
				FileHashList.getXStreamSetupStatic().getOutputGraph(in);
				IEntry.Factory.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(ContextQueriesDescriptorDatabase.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}
	
	private static final Pattern debugPattern = Pattern.compile("^Start\\s+Eval\\s+KeepMethodIs\\s+\\d+\\s+Value=(.+)$");
	private static final Pattern methodSigPat = Pattern.compile("^<([^:]+):\\s+([^\\s]+)\\s+([^\\(]+)\\(([^\\)]*)\\)>$");
	
	public static void main(String[] args) {
		if(args.length < 4) {
			throw new RuntimeException("Error: Did not provide 4 arguments '" + args.toString() + "'");
		}
		ContextQueriesDescriptorDatabase cdb;
		try {
			cdb = ContextQueriesDescriptorDatabase.readXMLStatic(args[0], null);
		} catch (Throwable t) {
			throw new RuntimeException("Error: Failed to read in the context query database at '" + args[0] + "'",t);
		}
		
		Set<String> allMethods = new HashSet<>();
		try(BufferedReader br = Files.newBufferedReader(FileHelpers.getPath(args[1]))) {
			String line;
			while((line = br.readLine()) != null) {
				allMethods.add(line.trim());
			}
		} catch(Throwable t) {
			throw new RuntimeException("Error: Failed to read in the all the methods from file '" + args[1] + "'",t);
		}
		allMethods = SortingMethods.sortSet(allMethods,SootSort.smStringComp);
		
		Set<String> existingContextQueries = new HashSet<>();
		try(BufferedReader br = Files.newBufferedReader(FileHelpers.getPath(args[2]))) {
			String line;
			while((line = br.readLine()) != null) {
				existingContextQueries.add(line.trim());
			}
		} catch(Throwable t) {
			throw new RuntimeException("Error: Failed to read in the the existing context queries from file '" + args[2] + "'",t);
		}
		existingContextQueries = SortingMethods.sortSet(existingContextQueries,SootSort.smStringComp);
		
		Set<String> newContextQueries = new HashSet<>();
		try(BufferedReader br = Files.newBufferedReader(FileHelpers.getPath(args[3]))) {
			String line;
			while((line = br.readLine()) != null) {
				newContextQueries.add(line.trim());
			}
		} catch(Throwable t) {
			throw new RuntimeException("Error: Failed to read in the the new context queries from file '" + args[3] + "'",t);
		}
		newContextQueries = SortingMethods.sortSet(newContextQueries,SootSort.smStringComp);
		
		Set<String> expectedMatchedMethods = new LinkedHashSet<>();
		Map<String,List<String>> unexpectedMatchedMethods = new LinkedHashMap<>();
		Set<String> expectedRejectedMethods = new LinkedHashSet<>();
		Set<String> unexpectedRejectedMethods = new LinkedHashSet<>();
		int sizeUnexpectedMatchedMaethods = 0;
		
		for(String methodSig : allMethods) {
			StringBuilder sb = new StringBuilder();
			boolean ret = cdb.matchesDebug(methodSig, sb);
			if(ret) {
				if(existingContextQueries.contains(methodSig) || newContextQueries.contains(methodSig)) {
					expectedMatchedMethods.add(methodSig);
				} else {
					try {
						BufferedReader br = new BufferedReader(new StringReader(sb.toString()));
						String line;
						String matchingPat = null;;
						while((line = br.readLine()) != null) {
							if(line.startsWith("Start Eval KeepMethodIs")) {
								line = line.trim();
								java.util.regex.Matcher m = debugPattern.matcher(line);
								if(m.matches()) {
									String pat = m.group(1).trim();
									line = br.readLine();
									if(line != null) {
										line = br.readLine();
										if(line != null) {
											line = line.trim();
											if(line.startsWith("Result: true")) {
												matchingPat = pat;
												break;
											}
										} else {
											throw new RuntimeException("Error: No third line for starting debug output '" + pat + "'");
										}
									} else {
										throw new RuntimeException("Error: No second line for starting debug output '" + pat + "'");
									}
								} else {
									throw new RuntimeException("Error: Debug output '" + line + "' is in unexpected format.");
								}
							}
						}
						if(matchingPat == null)
							throw new RuntimeException("Error: Failed to find the pattern that matches '" + methodSig + "'");
						
						List<String> sigsMatching = unexpectedMatchedMethods.get(matchingPat);
						if(sigsMatching == null) {
							sigsMatching = new ArrayList<>();
							unexpectedMatchedMethods.put(matchingPat, sigsMatching);
						}
						sigsMatching.add(methodSig);
						sizeUnexpectedMatchedMaethods++;
					} catch(Exception e) {
						throw new RuntimeException(e);
					}
				}
			} else {
				if(existingContextQueries.contains(methodSig) || newContextQueries.contains(methodSig)) {
					unexpectedRejectedMethods.add(methodSig);
				} else {
					expectedRejectedMethods.add(methodSig);
				}
			}
		}
		unexpectedMatchedMethods = SortingMethods.sortMapKey(unexpectedMatchedMethods, SortingMethods.sComp);
		
		Set<String> allKnownCq = new HashSet<>(existingContextQueries);
		allKnownCq.addAll(newContextQueries);
		allKnownCq.removeAll(allMethods);
		
		System.out.println("Stats: ");
		System.out.println("  Expected Matches: " + expectedMatchedMethods.size());
		System.out.println("  Unexpected Matches: " + sizeUnexpectedMatchedMaethods);
		System.out.println("  Expected Rejects: " + expectedRejectedMethods.size());
		System.out.println("  Unexpected Rejects: " + unexpectedRejectedMethods.size());
		System.out.println("  Context Queries Not in All Methods List: " + allKnownCq.size());
		if(!unexpectedMatchedMethods.isEmpty()) {
			System.out.println();
			System.out.println("Unexpected Matches:");
			for(String sigPat : unexpectedMatchedMethods.keySet()) {
				System.out.println("  " + sigPat);
				for(String methodSig : unexpectedMatchedMethods.get(sigPat)) {
					java.util.regex.Matcher m = methodSigPat.matcher(methodSig);
					if(m.matches()) {
						System.out.println("    " + SootMatcher.splitWords(m.group(3)) + "  " + methodSig);
					} else {
						throw new RuntimeException("Error: Failed to match string method sig for '" + methodSig + "'");
					}
				}
			}
		}
		if(!unexpectedRejectedMethods.isEmpty()) {
			System.out.println();
			System.out.println("Unexpected Rejects:");
			for(String methodSig : unexpectedRejectedMethods) {
				java.util.regex.Matcher m = methodSigPat.matcher(methodSig);
				if(m.matches()) {
					System.out.println("  " + SootMatcher.splitWords(m.group(3)) + "  " + methodSig);
				} else {
					throw new RuntimeException("Error: Failed to match string method sig for '" + methodSig + "'");
				}
			}
		}
		if(!allKnownCq.isEmpty()) {
			System.out.println();
			System.out.println("Context Queries Not in All Methods List:");
			for(String methodSig : allKnownCq) {
				java.util.regex.Matcher m = methodSigPat.matcher(methodSig);
				if(m.matches()) {
					System.out.println("  " + SootMatcher.splitWords(m.group(3)) + "  " + methodSig);
				} else {
					throw new RuntimeException("Error: Failed to match string method sig for '" + methodSig + "'");
				}
			}
		}
		
		/*if(newContextQueries.removeAll(existingContextQueries)) {
			newContextQueries = SortingMethods.sortSet(newContextQueries,SootSort.smStringComp);
			try(PrintStreamUnixEOL ps = new PrintStreamUnixEOL(Files.newOutputStream(FileHelpers.getPath(args[3])))) {
				for(String s : newContextQueries) {
					ps.println(s);
				}
			} catch(Throwable t) {
				throw new RuntimeException(t);
			}
		}*/
		
	}

}
