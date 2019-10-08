package org.sag.acminer.phases.acminerdebug;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.database.accesscontrol.IContextQueryDatabase;
import org.sag.acminer.phases.acminerdebug.handler.AbstractOutputHandler;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.concurrent.IgnorableRuntimeException;
import org.sag.common.graphtools.AlEdge;
import org.sag.common.graphtools.AlElement.Color;
import org.sag.common.graphtools.AlNode;
import org.sag.common.graphtools.Formatter;
import org.sag.common.graphtools.GraphmlGenerator;
import org.sag.common.io.FileHelpers;
import org.sag.common.logging.ILogger;
import org.sag.common.logging.LoggerWrapperSLF4J;

import soot.Scene;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class CQSubGraphOutputHandler extends AbstractOutputHandler {

	private final ILogger logger;
	private final CallGraph cg;
	private final GraphmlGenerator outGraphml;
	
	public CQSubGraphOutputHandler(GraphmlGenerator outGraphml, Path rootOutputDir, IACMinerDataAccessor dataAccessor, ILogger logger) {
		super(rootOutputDir,dataAccessor);
		Objects.requireNonNull(outGraphml);
		this.outGraphml = outGraphml;
		this.logger = logger == null ? new LoggerWrapperSLF4J(CQSubGraphOutputHandler.class) : logger;
		this.cg = Scene.v().getCallGraph();
	}
	
	public boolean run() {
		try{
			FileHelpers.processDirectory(rootOutputDir,true,true);
		}catch(Throwable t){
			logger.fatal("{}: Failed to create and verify the output directory. Skipping.",t,cn);
			return false;
		}
		try {
			IContextQueryDatabase cqdb = dataAccessor.getContextQueriesDB();
			Map<EntryPoint, Map<SootMethod, Set<SootMethod>>> data = cqdb.getAllContextQueriesWithSubGraphMethods();
			Map<SootMethod,Set<SootMethod>> cqToGraph = new HashMap<>();
			for(EntryPoint ep : data.keySet()) {
				Map<SootMethod, Set<SootMethod>> datai = data.get(ep);
				for(SootMethod cq : datai.keySet()) {
					Set<SootMethod> temp = cqToGraph.get(cq);
					if(temp == null) {
						temp = new HashSet<>();
						cqToGraph.put(cq, temp);
					}
					temp.addAll(datai.get(cq));
				}
			}
			
			int i = 0;
			for(SootMethod cq : cqToGraph.keySet()) {
				Set<SootMethod> subGraph = cqToGraph.get(cq);
				Path o = getOutputFilePath(rootOutputDir,cq,i++ + "",".graphml");
				GraphFormatter form = new GraphFormatter(cq, subGraph, cg, o);
				form.format();
				outGraphml.outputGraph(form);
			}
		} catch(IgnorableRuntimeException e) {
			return false;
		} catch(Throwable t) {
			logger.fatal("{}: An unexpected error occured.",t,cn);
			return false;
		}
		return true;
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
	
	private static final class GraphFormatter extends Formatter {
		private final SootMethod cq;
		private final Set<SootMethod> subGraph;
		private final CallGraph cg;
		private final Set<AlNode> alNodes;
		private final Set<AlEdge> alEdges;
		private volatile long id;
		public GraphFormatter(SootMethod cq, Set<SootMethod> subGraph, CallGraph cg, Path outputPath) {
			super(0,-1,-1,-1,outputPath);
			this.cg = cg;
			this.alNodes = new TreeSet<>();
			this.alEdges = new TreeSet<>();
			this.id = 0;
			this.cq = cq;
			this.subGraph = subGraph;
			this.subGraph.add(cq);
		}
		public Collection<AlNode> getNodes() { return alNodes; }
		public Collection<AlEdge> getEdges() { return alEdges; }
		public void format() {
			Map<SootMethod,AlNode> nton = new HashMap<>();
			for(SootMethod node : subGraph) {
				AlNode cur = new AlNode(id++,node.getSignature());
				alNodes.add(cur);
				nton.put(node, cur);
				if(node.equals(cq))
					cur.setColors(nodeColorIndex, Collections.singletonList(Color.GREEN));
			}
			for(SootMethod node : subGraph) {
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
	
}
