package org.sag.acminer.phases.acminerdebug;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.phases.acminerdebug.handler.AbstractOutputHandler;
import org.sag.common.concurrent.IgnorableRuntimeException;
import org.sag.common.graphtools.AlEdge;
import org.sag.common.graphtools.AlNode;
import org.sag.common.graphtools.Formatter;
import org.sag.common.graphtools.GraphmlGenerator;
import org.sag.common.graphtools.AlElement.Color;
import org.sag.common.io.FileHelpers;
import org.sag.common.io.PrintStreamUnixEOL;
import org.sag.common.logging.ILogger;
import org.sag.common.logging.LoggerWrapperSLF4J;
import org.sag.common.tools.SortingMethods;
import org.sag.common.tuple.Pair;
import org.sag.soot.SootSort;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class PathsToMethodsOutputHandler extends AbstractOutputHandler {
	
	private final ILogger logger;
	private final CallGraph cg;
	private final GraphmlGenerator outGraphml;
	private final Map<SootClass, Set<SootMethod>> input;
	
	public PathsToMethodsOutputHandler(GraphmlGenerator outGraphml, Path rootOutputDir, IACMinerDataAccessor dataAccessor, 
			Map<SootClass,Set<SootMethod>> input, ILogger logger) {
		super(rootOutputDir,dataAccessor);
		Objects.requireNonNull(outGraphml);
		this.outGraphml = outGraphml;
		this.logger = logger == null ? new LoggerWrapperSLF4J(PathsToMethodsOutputHandler.class) : logger;
		this.cg = Scene.v().getCallGraph();
		this.input = input;
	}
	
	//Create path to output file that does not exist
	private Path getOutputFilePath(Path stubOutputDir, SootMethod m, String uniq, String ext) {
		Path output = null;
		try{
			StringBuilder sb2 = new StringBuilder();
			String className = m.getDeclaringClass().getShortName();
			int i3 = className.lastIndexOf('$');
			if(i3 > 0 && className.length() > 1){
				className = className.substring(i3+1);
			}
			sb2.append(FileHelpers.replaceWhitespace(FileHelpers.cleanFileName(className))).append("_");
			String retType = m.getReturnType().toString();
			int i = retType.lastIndexOf('.');
			if(i > 0 && retType.length() > 1) {
				retType = retType.substring(i+1);
			}
			int i2 = retType.lastIndexOf('$');
			if(i2 > 0 && retType.length() > 1){
				retType = retType.substring(i2+1);
			}
			sb2.append(FileHelpers.replaceWhitespace(FileHelpers.cleanFileName(retType))).append("_");
			sb2.append(FileHelpers.replaceWhitespace(FileHelpers.cleanFileName(m.getName())));
			output = FileHelpers.getPath(stubOutputDir, sb2.toString());
			
			StringBuilder sb3 = new StringBuilder();
			sb3.append("_").append(uniq).append(ext);
			output = FileHelpers.getPath(sb3.insert(0, FileHelpers.trimFullFilePath(output.toString(), false, sb3.length())).toString());
		}catch(Throwable t){
			logger.fatal("{}: Failed to construct the output file for output directory '{}' and method '{}'.",
					t,cn,stubOutputDir,m);
			throw new IgnorableRuntimeException();
		}
		return output;
	}
	
	//Create output directories for path
	private Path getAndCreateStubOutputDir(SootClass stub) {
		Path stubOutputDir = null;
		try {
			String[] parts = stub.toString().split("\\.");
			for(int i = 0; i < parts.length; i++){
				parts[i] = FileHelpers.replaceWhitespace(FileHelpers.cleanFileName(parts[i]));
			}
			stubOutputDir = FileHelpers.getPath(rootOutputDir,parts);
			FileHelpers.processDirectory(stubOutputDir,true,false);
		} catch(Throwable t) {
			logger.fatal("{}: Failed to process the output directory for root output directory '{}' and stub '{}'.",
					t,cn,rootOutputDir,stub);
			throw new IgnorableRuntimeException();
		}
		return stubOutputDir;
	}
	
	public boolean run() {
		try{
			FileHelpers.processDirectory(rootOutputDir,true,true);
		}catch(Throwable t){
			logger.fatal("{}: Failed to create and verify the output directory. Skipping.",t,cn);
			return false;
		}
		try {
			for(SootClass stub : input.keySet()) {
				Set<SootMethod> eps = dataAccessor.getEntryPointsForStub(stub);
				int i = 0;
				Path stubOutputDir = getAndCreateStubOutputDir(stub);
				
				for(SootMethod end : input.get(stub)) {
					Set<SootMethod> nodes = new HashSet<>();
					Path outFileGraph = getOutputFilePath(stubOutputDir,end,i + "",".graphml");
					Path outFileTxt = getOutputFilePath(stubOutputDir,end,i++ + "",".txt");
					for(SootMethod ep : eps) {
						Deque<Pair<SootMethod,List<SootMethod>>> stack = new ArrayDeque<>();
						Set<SootMethod> seen = new HashSet<>();
						stack.add(new Pair<SootMethod, List<SootMethod>>(ep, Collections.<SootMethod>singletonList(ep)));
						while(!stack.isEmpty()) {
							Pair<SootMethod, List<SootMethod>> curP = stack.pop();
							SootMethod cur = curP.getFirst();
							List<SootMethod> path = curP.getSecond();
							if(cur.equals(end)) {
								nodes.addAll(path);
							} else {
								//Looking for paths to end point, excluded method will not lead there, also remove any loops
								if(!dataAccessor.getExcludedElementsDB().isExcludedMethod(cur) && seen.add(cur)) {
									Iterator<Edge> it = cg.edgesOutOf(cur);
									while(it.hasNext()) {
										SootMethod tgt = it.next().tgt();
										if(tgt.equals(end) || 
												(!dataAccessor.getExcludedElementsDB().isExcludedMethod(tgt) && !seen.contains(tgt))) {
											List<SootMethod> l = new ArrayList<>(path);
											l.add(tgt);
											stack.push(new Pair<SootMethod, List<SootMethod>>(tgt, l));
										}
									}
								}
							}
						}
					}
					
					outGraphml.outputGraph(new GraphFormatter(end, eps, nodes, cg, outFileGraph));
					Set<SootMethod> included = new HashSet<>();
					Set<SootMethod> excluded = new HashSet<>();
					for(SootMethod ep : eps) {
						if(nodes.contains(ep))
							included.add(ep);
						else
							excluded.add(ep);
					}
					SortingMethods.sortSet(included, SootSort.smComp);
					SortingMethods.sortSet(excluded, SootSort.smComp);
					try(PrintStream ps = new PrintStreamUnixEOL(Files.newOutputStream(outFileTxt))) {
						ps.println("Entry Points Calling " + end.toString());
						for(SootMethod ep : included) {
							ps.println("\t" + ep.toString());
						}
						ps.println("Entry Points Not Calling " + end.toString());
						for(SootMethod ep : excluded) {
							ps.println("\t" + ep.toString());
						}
					}
					
				}
			}
		} catch(IgnorableRuntimeException e) {
			return false;
		} catch(Throwable t) {
			logger.fatal("{}: An unexpected error occured.",t,cn);
			return false;
		}
		return true;
	}
	
	private static final class GraphFormatter extends Formatter {
		private final Set<AlNode> alNodes;
		private final Set<AlEdge> alEdges;
		public GraphFormatter(SootMethod end, Set<SootMethod> eps, Set<SootMethod> nodes, CallGraph cg, Path outputPath) {
			super(0,-1,-1,-1,outputPath);
			this.alNodes = new TreeSet<>();
			this.alEdges = new TreeSet<>();
			formatInner(nodes, eps, end, cg);
		}
		public Collection<AlNode> getNodes() { return alNodes; }
		public Collection<AlEdge> getEdges() { return alEdges; }
		public void format() {}
		private void formatInner(Set<SootMethod> nodes, Set<SootMethod> eps, SootMethod end, CallGraph cg) {
			long id = 0;
			Map<SootMethod,AlNode> nton = new HashMap<>();
			for(SootMethod node : nodes) {
				AlNode cur = new AlNode(id++,node.getSignature());
				alNodes.add(cur);
				nton.put(node, cur);
				List<Color> c = new ArrayList<>();
				if(node.equals(end))
					c.add(Color.BLUE);
				if(eps.contains(node))
					c.add(Color.GREEN);
				if(!c.isEmpty())
					cur.setColors(nodeColorIndex, c);
			}
			for(SootMethod node : nodes) {
				AlNode cur = nton.get(node);
				if(cur != null) {
					Iterator<Edge> itEdge = cg.edgesOutOf(node);
					while(itEdge.hasNext()){
						AlNode child = nton.get(itEdge.next().tgt());
						if(child != null)
							alEdges.add(new AlEdge(id++,cur,child));
					}
				}
			}
		}
	}
	
	public static final Map<SootClass,Set<SootMethod>> loadData(Path in, IACMinerDataAccessor dataAccessor) throws Exception {
		Map<SootClass,Set<SootMethod>> ret = new HashMap<>();
		SootClass stub = null;
		if(!FileHelpers.checkRWFileExists(in)) {
			return null;
		}
		try(BufferedReader br = Files.newBufferedReader(in, Charset.defaultCharset())) {
			Map<String, SootClass> stubs = new HashMap<>();
			for(SootClass sc : dataAccessor.getBinderStubsAndMethods().keySet()) {
				stubs.put(sc.toString(), sc);
			}
			for(String line; (line = br.readLine()) != null;) {
				line = line.trim();
				if(line.startsWith("#") || line.isEmpty()) {
					continue;
				} else if(line.startsWith("Stub ")) {
					String stubString = line.substring(5).trim();
					stub = stubs.get(stubString);
					if(stub == null)
						throw new RuntimeException("Error: The given stub '" + stubString + "' is not a recongized stub.");
					Set<SootMethod> temp = ret.get(stub);
					if(temp == null) {
						temp = new HashSet<>();
						ret.put(stub, temp);
					}
				} else {
					SootMethod sm = Scene.v().grabMethod(line);
					if(sm != null && ret.containsKey(stub))
						ret.get(stub).add(sm);
				}
			}
		}
		for(SootClass s : ret.keySet()) {
			ret.put(s, SortingMethods.sortSet(ret.get(s),SootSort.smComp));
		}
		ret = SortingMethods.sortMapKey(ret, SootSort.scComp);
		if(ret.isEmpty())
			return null;
		return ret;
	}

}
