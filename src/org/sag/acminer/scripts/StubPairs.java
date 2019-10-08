package org.sag.acminer.scripts;

import java.io.ObjectStreamException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.sag.acminer.database.acminer.Doublet;
import org.sag.acminer.database.acminer.IACMinerDatabase;
import org.sag.common.io.FileHelpers;
import org.sag.common.tools.SortingMethods;
import org.sag.soot.SootSort;
import org.sag.xstream.XStreamInOut;
import org.sag.xstream.XStreamInOut.XStreamInOutInterface;
import org.sag.xstream.xstreamconverters.NamedCollectionConverterWithSize;
import com.google.common.base.CharMatcher;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("StubPairs")
public class StubPairs implements XStreamInOutInterface {
	
	@XStreamAlias("Stub")
	@XStreamAsAttribute
	private String stub;
	@XStreamImplicit
	private ArrayList<Pair> pairs;
	
	private StubPairs() {}
	
	private StubPairs(ArrayList<Pair> pairs, String stub) {
		this.pairs = pairs;
		this.stub = stub;
	}
	
	public List<Pair> getPairs() {
		return pairs;
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public StubPairs readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}

	public static StubPairs readXMLStatic(String filePath, Path path) throws Exception {
		return new StubPairs().readXML(filePath, path);
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
				Pair.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(StubPairs.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}
	
	public static StubPairs genStubPairs(String stub, IACMinerDatabase smdb) {
		Map<String,Set<Doublet>> epToLogic = smdb.getStringValuePairs(stub);
		Map<Doublet,Pair> data = new HashMap<>();
		for(String ep : epToLogic.keySet()) {
			Set<Doublet> logic = epToLogic.get(ep);
			for(Doublet d : logic) {
				Pair temp = data.get(d);
				if(temp == null) {
					temp = new Pair(d.toString());
					data.put(d, temp);
				}
				temp.addSources(d.getSourceMethods());
				temp.addEntryPoint(ep);
			}
		}
		data = SortingMethods.sortMapKeyAscending(data);
		return new StubPairs(new ArrayList<Pair>(data.values()), stub);
	}
	
	public static void main(String[] args) {
		if(args.length < 1)
			throw new RuntimeException("Error: Not enough args");
		String op = args[0];
		if(op.equals("-d")) {
			if(args.length < 4)
				throw new RuntimeException("Error: Not enough args");
			String stubSig = args[1];
			Path inFile = FileHelpers.getPath(args[2]);
			Path outDir = FileHelpers.getPath(args[3]);
			IACMinerDatabase smdb;
			
			try {
				FileHelpers.processDirectory(outDir, true, false);
			} catch (Throwable e) {
				throw new RuntimeException("Error: Failed to process output dir '" + outDir + "'.",e);
			}
			
			try {
				smdb = IACMinerDatabase.Factory.readXML(null, inFile);
			} catch (Throwable e) {
				throw new RuntimeException("Error: Failed to read in ACMinerDatabase from '" + inFile + "'.",e);
			}
			
			StubPairs info = genStubPairs(stubSig,smdb);
			Path outFile = FileHelpers.getPath(outDir, stubSig + "_pairs.xml");
			try {
				info.writeXML(null, outFile);
			} catch(Throwable t) {
				throw new RuntimeException("Error: Failed to write to file '" + outFile + "'.",t);
			}
		} else if(op.equals("-r")) {
			if(args.length < 3)
				throw new RuntimeException("Error: Not enough args");
			Path inFile = FileHelpers.getPath(args[1]);
			Path opsFile = FileHelpers.getPath(args[2]);
			StubPairs curStubPair;
			try {
				curStubPair = StubPairs.readXMLStatic(null, inFile);
			} catch(Throwable t) {
				throw new RuntimeException("Error: Failed to read in file at '" + inFile + "'.",t);
			}
			StubPairs toRemoveStubPair;
			try {
				toRemoveStubPair = StubPairs.readXMLStatic(null, opsFile);
			} catch(Throwable t) {
				throw new RuntimeException("Error: Failed to read in file at '" + opsFile + "'.",t);
			}
			
			//write backup file before making changes
			Path backFile = FileHelpers.getPath(inFile + ".bak");
			try {
				curStubPair.writeXML(null, backFile);
			} catch(Throwable t) {
				throw new RuntimeException("Error: Failed to write backup file at '" + backFile + "'.",t);
			}
			
			List<Pair> allPairs = curStubPair.getPairs();
			List<Pair> toRemovePairs = toRemoveStubPair.getPairs();
			for(Pair toRemove : toRemovePairs) {
				List<Pair> curPairs = new ArrayList<>();
				//Start by matching the value
				String v = toRemove.value;
				if(v == null || v.equals("*")) {//If pair value is wild card then include all pairs except those that are already marked removed
					for(Pair p : allPairs) {
						if(!p.isRemoveSet())
							curPairs.add(p);
					}
				} else {//We have a specific pair value so find all pairs that match this value not already marked remove
					Doublet toMatch = new Doublet(v);
					for(Pair p : allPairs) {
						if(!p.isRemoveSet()) {
							Doublet cur = new Doublet(p.getValue());
							if(cur.equals(toMatch))
								curPairs.add(p);
						}
					}
				}
				//Then move onto the sources
				if(toRemove.getSources() != null) {//Treat null value like wild card
					//If a source entry contains a wild card then ignore all other source entries as this is saying match all
					//That is the sources list is order dependent and will remove all pairs that don't contain a specific source
					//up until it hits a wild card which says match all remaining. If a wild card is not specified then the sources
					//list must match exactly.
					boolean wildCardExit = false;
					for(String toRemoveSource : toRemove.getSources()) {
						if(toRemoveSource.equals("*")) {
							wildCardExit = true;
							break;
						} else {
							for(Iterator<Pair> it = curPairs.iterator(); it.hasNext();) {
								Pair p = it.next();
								if(!p.getSources().contains(toRemoveSource))
									it.remove();
							}
						}
					}
					if(!wildCardExit) {
						for(Iterator<Pair> it = curPairs.iterator(); it.hasNext();) {
							Pair p = it.next();
							if(p.getSources().size() != toRemove.getSources().size())
								it.remove();
						}
					}
				}
				
				if(toRemove.getEps() != null) {
					boolean wildCardExit = false;
					for(String toRemoveEp : toRemove.getEps()) {
						if(toRemoveEp.equals("*")) {
							wildCardExit = true;
							break;
						} else {
							for(Iterator<Pair> it = curPairs.iterator(); it.hasNext();) {
								Pair p = it.next();
								if(!p.getEps().contains(toRemoveEp))
									it.remove();
							}
						}
					}
					if(!wildCardExit) {
						for(Iterator<Pair> it = curPairs.iterator(); it.hasNext();) {
							Pair p = it.next();
							if(p.getEps().size() != toRemove.getEps().size())
								it.remove();
						}
					}
				}
				
				for(Pair curP : curPairs) {
					curP.setRemove(true);
					curP.setReason(toRemove.getReason() == null ? "" : toRemove.getReason());
				}
			}
			
			try {
				curStubPair.writeXML(null, inFile);
			} catch(Throwable t) {
				throw new RuntimeException("Error: Failed to write file at '" + inFile + "'.",t);
			}
		} else if(op.equals("-s")) {
			Path inFile = FileHelpers.getPath(args[1]);
			Path htmlDir = FileHelpers.getPath(args[2]);
			StubPairs curStubPair;
			try {
				curStubPair = StubPairs.readXMLStatic(null, inFile);
			} catch(Throwable t) {
				throw new RuntimeException("Error: Failed to read in file at '" + inFile + "'.",t);
			}
			List<Path> htmlFiles;
			try {
				htmlFiles = FileHelpers.getAllFilesInDirectory(htmlDir);
				for(Iterator<Path> it = htmlFiles.iterator(); it.hasNext();) {
					Path p = it.next();
					if(!p.getFileName().toString().matches("\\d{1,3}\\.html"))
						it.remove();
				}
			} catch (Throwable e) {
				throw new RuntimeException("Error: Failed to find html files in '" + htmlDir + "'.",e);
			}
			
			Set<Doublet> toRemove = new HashSet<>();
			for(Pair p : curStubPair.getPairs()) {
				if(p.isRemove())
					toRemove.add(new Doublet(p.getValue()));
			}
			
			for(Path htmlFile : htmlFiles) {
				int removed = 0;
				Document doc;
				try {
					doc = Jsoup.parse(htmlFile.toFile(), StandardCharsets.UTF_8.name());
				} catch (Throwable e) {
					throw new RuntimeException("Error: Failed to parse html file " + htmlFile + "'.",e);
				}
				Elements labels = doc.select("label:matchesOwn(^`|^\\{)");
				for(Element label : labels) {
					String text = label.text();
					int ticks = CharMatcher.is('`').countIn(text);
					Doublet dbl;
					if(ticks == 2 || (ticks == 4 && text.startsWith("{")))
						dbl = new Doublet(text);
					else if(ticks == 4)
							dbl = new Doublet("{"+text+"}");
					else
						throw new RuntimeException("Error: Unexpected number of ticks for " + text);
					if(toRemove.contains(dbl)) {
						Element input = label.previousElementSibling();
						Element div = label.nextElementSibling();
						label.remove();
						input.remove();
						div.remove();
						removed++;
					}
				}
				if(removed > 0) {
					Path outFile = FileHelpers.getPath(htmlFile.getParent(), htmlFile.getFileName().toString().replace(".html", "") + "_mod.html");
					try(PrintWriter writer = new PrintWriter(outFile.toFile(),"UTF-8")) {
						writer.write(doc.html());
						writer.flush();
					} catch(Throwable t) {
						throw new RuntimeException("Error: Failed to write to '" + outFile + "'.",t);
					}
				}
			}
		} else if(op.equals("-i")) {
			if(args.length < 2)
				throw new RuntimeException("Error: Not enough args");
			Path inFile = FileHelpers.getPath(args[1]);
			
			StubPairs curStubPair;
			try {
				curStubPair = StubPairs.readXMLStatic(null, inFile);
			} catch(Throwable t) {
				throw new RuntimeException("Error: Failed to read in file at '" + inFile + "'.",t);
			}
			
			//write backup file before making changes
			Path backFile = FileHelpers.getPath(inFile + ".bak");
			try {
				curStubPair.writeXML(null, backFile);
			} catch(Throwable t) {
				throw new RuntimeException("Error: Failed to write backup file at '" + backFile + "'.",t);
			}
			
			List<Pair> pairs = curStubPair.getPairs();
			int count = 0;
			for(Pair p : pairs) {
				if(!p.isRemoveSet()) {
					count++;
				}
			}
			int i = 1;
			//Scanner scanner = new Scanner(System.in);
			try (Scanner scanner = new Scanner(System.in)) {
				for(Pair p : pairs) {
					if(!p.isRemoveSet()) {
						Doublet d = new Doublet(p.getValue());
						Set<String> sources = p.getSources();
						Set<String> eps = p.getEps();
						System.out.println("Pair (" + i++ + "/" + count + "): " + d.toString());
						System.out.println("Sources:");
						for(String source : sources) {
							System.out.println("  " + source);
						}
						System.out.println("Entry Points:");
						for(String ep : eps) {
							System.out.println("  " + ep);
						}
						String line = null;
						boolean breakOut = false;
						while(line == null || !(line.equals("exit") || line.equals("y") || line.equals("n"))) {
							System.out.println("Is this authorization logic (y/n/exit)?: ");
							line = scanner.nextLine().trim();
							if(line.equals("exit")) {
								breakOut = true;
							} else if(line.equals("y")) {
								p.setRemove(false);
							} else if(line.equals("n")) {
								System.out.println("Reason: ");
								String line2 = scanner.nextLine().trim();
								p.setRemove(true);
								p.setReason(line2);
							} else {
								System.out.println("Unknown op string '" + line + "'");
							}
						}
						
						//Save after every change
						try {
							curStubPair.writeXML(null, inFile);
						} catch(Throwable t) {
							throw new RuntimeException("Error: Failed to write file at '" + inFile + "'.",t);
						}
						
						clearTerm();
						if(breakOut)
							break;
						
					}
				}
			} catch(Throwable t) {
				System.out.println("Error: Unexpected exception");
				t.printStackTrace();
			}
			
			try {
				curStubPair.writeXML(null, inFile);
			} catch(Throwable t) {
				throw new RuntimeException("Error: Failed to write file at '" + inFile + "'.",t);
			}
		}
	}
	
	private static void clearTerm() {
		try {
			String os = System.getProperty("os.name");
			if(os.contains("Windows")) {
				new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
			} else {
				new ProcessBuilder("clear").inheritIO().start().waitFor();
			}
		} catch(Throwable t) {}
	}
	
	@XStreamAlias("Pair")
	public static class Pair implements XStreamInOutInterface {
		
		@XStreamAsAttribute
		@XStreamAlias("Remove")
		private int remove;//-1 unset, 0 do not remove, 1 remove
		@XStreamAsAttribute
		@XStreamAlias("Value")
		private String value;
		@XStreamAlias("Sources")
		@XStreamConverter(value=NamedCollectionConverterWithSize.class,booleans={false},strings={"Source"},types={String.class})
		private LinkedHashSet<String> sources;
		@XStreamAlias("EntryPoints")
		@XStreamConverter(value=NamedCollectionConverterWithSize.class,booleans={false},strings={"EntryPoint"},types={String.class})
		private LinkedHashSet<String> eps;
		@XStreamAlias("Reason")
		private String reason;
		
		private Pair() {}
		
		public Pair(String value) {
			this.value = value;
			this.sources = new LinkedHashSet<>();
			this.eps = new LinkedHashSet<>();
			this.reason = "";
			this.remove = -1;
		}
		
		protected Object writeReplace() throws ObjectStreamException {
			sources = SortingMethods.sortSet(sources,SootSort.smStringComp);
			eps = SortingMethods.sortSet(eps,SootSort.smStringComp);
			return this;
		}
		
		public void addSources(Set<String> toAdd) {
			this.sources.addAll(toAdd);
		}
		
		public void addEntryPoint(String ep) {
			this.eps.add(ep);
		}

		public boolean isRemove() {
			return remove == 1;
		}
		
		public boolean isRemoveSet() {
			return remove != -1;
		}

		public void setRemove(boolean remove) {
			if(remove)
				this.remove = 1;
			else 
				this.remove = 0;
		}

		public String getReason() {
			return reason;
		}

		public void setReason(String reason) {
			this.reason = reason;
		}

		public String getValue() {
			return value;
		}

		public LinkedHashSet<String> getSources() {
			return sources;
		}

		public LinkedHashSet<String> getEps() {
			return eps;
		}

		@Override
		public void writeXML(String filePath, Path path) throws Exception {
			XStreamInOut.writeXML(this, filePath, path);
		}

		@Override
		public Pair readXML(String filePath, Path path) throws Exception {
			return XStreamInOut.readXML(this, filePath, path);
		}

		public static Pair readXMLStatic(String filePath, Path path) throws Exception {
			return new Pair().readXML(filePath, path);
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
				}
			}

			@Override
			public Set<Class<?>> getAnnotatedClasses() {
				return Collections.singleton(Pair.class);
			}
			
			@Override
			public void setXStreamOptions(XStream xstream) {
				defaultOptionsNoRef(xstream);
			}
			
		}
		
	}

}
