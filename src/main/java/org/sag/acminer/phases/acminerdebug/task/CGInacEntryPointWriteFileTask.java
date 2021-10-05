package org.sag.acminer.phases.acminerdebug.task;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.database.excludedelements.IExcludeHandler;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.concurrent.IgnorableRuntimeException;
import org.sag.common.io.PrintStreamUnixEOL;
import org.sag.common.logging.ILogger;
import org.sag.common.tools.SortingMethods;
import org.sag.common.tuple.Triple;
import org.sag.soot.SootSort;

import soot.SootClass;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class CGInacEntryPointWriteFileTask extends AbstractEntryPointWriteFileTask {
	protected final CallGraph cg;
	public CGInacEntryPointWriteFileTask(EntryPoint ep, int id, CallGraph cg, IACMinerDataAccessor dataAccessor, Path rootOutputDir, String cn, 
			ILogger logger) {
		super(ep, id, dataAccessor, rootOutputDir, cn, logger);
		this.cg = cg;
	}
	@Override
	public void run() {
		SootMethod entryPoint = ep.getEntryPoint();
		SootClass stub = ep.getStub();
		Path stubOutputDir = getAndCreateStubOutputDir(stub);
		Path output = getOutputFilePath(stubOutputDir,entryPoint,id+"",".txt");
		Set<Triple<Integer,SootMethod,Map<String,Set<SootMethod>>>> inac = discoverCallGraphInaccuries(stub, entryPoint);
		PrintStream ps = getPrintStream(output);
		writeToFile(stub,entryPoint,ps,inac);
	}
	
	protected PrintStream getPrintStream(Path output) {
		try{
			return new PrintStreamUnixEOL(Files.newOutputStream(output));
		}catch(Throwable t){
			logger.fatal("{}: Failed to open the output file '{}' for writing.",t,cn,output);
			throw new IgnorableRuntimeException();
		}
	}
	
	protected Set<Triple<Integer,SootMethod,Map<String,Set<SootMethod>>>> discoverCallGraphInaccuries(SootClass stub, SootMethod entryPoint){
		try {
			IExcludeHandler excludeHandler = dataAccessor.getExcludedElementsDB().createNewExcludeHandler(new EntryPoint(entryPoint,stub));
			Queue<SootMethod> tovisit = new ArrayDeque<SootMethod>();
			Queue<Integer> depthCount = new ArrayDeque<Integer>();
			HashSet<SootMethod> visited = new HashSet<SootMethod>();
			tovisit.add(entryPoint);
			depthCount.add(1);
			Set<Triple<Integer,SootMethod,Map<String,Set<SootMethod>>>> ret = new HashSet<>();
			while(!tovisit.isEmpty()){
				SootMethod currMeth = tovisit.poll();
				int curDepth = depthCount.poll();
				visited.add(currMeth);
				//need to discover if the outgoing edges contain calls that are identical
				Iterator<Edge> itEdge = cg.edgesOutOf(currMeth);
				Map<String,Set<SootMethod>> subSigToMethods = new HashMap<>();
				while(itEdge.hasNext()){
					Edge e = itEdge.next();
					SootMethod tgt = e.tgt();
					
					if(excludeHandler.isExcludedMethodWithOverride(tgt)){
						continue;
					}
					
					if(!currMeth.equals(tgt) && !visited.contains(tgt) && !tovisit.contains(tgt)){
						tovisit.add(tgt);
						depthCount.add(curDepth+1);
					}
					Set<SootMethod> methods = subSigToMethods.get(tgt.getSubSignature());
					if(methods == null){
						methods = new HashSet<SootMethod>();
						subSigToMethods.put(tgt.getSubSignature(), methods);
					}
					methods.add(tgt);
				}
				Iterator<Entry<String,Set<SootMethod>>> it = subSigToMethods.entrySet().iterator();
				while(it.hasNext()){
					Entry<String,Set<SootMethod>> e = it.next();
					if(e.getValue().size() <= 1){
						it.remove();
					}
				}
				if(!subSigToMethods.isEmpty()){
					ret.add(new Triple<Integer,SootMethod,Map<String,Set<SootMethod>>>(curDepth,currMeth,subSigToMethods));
				}
			}
			ret = SortingMethods.sortSet(ret,new Comparator<Triple<Integer,SootMethod,Map<String,Set<SootMethod>>>>(){
				@Override
				public int compare(Triple<Integer, SootMethod, Map<String, Set<SootMethod>>> o1,
						Triple<Integer, SootMethod, Map<String, Set<SootMethod>>> o2) {
					if(o1.getFirst() > o2.getFirst()){
						return 1;
					}else if(o1.getFirst() < o2.getFirst()){
						return -1;
					}else{
						return o1.getSecond().getSignature().compareToIgnoreCase(o2.getSecond().getSignature());
					}
				}
			});
			return ret;
		} catch(Throwable t) {
			logger.fatal("{}: Failed to generate call graph inaccuracies data for stub '{}' and entry point '{}'.",t,cn,stub,entryPoint);
			throw new IgnorableRuntimeException();
		}
	}
	
	protected void writeToFile(SootClass stub, SootMethod entryPoint, PrintStream ps, 
			Set<Triple<Integer,SootMethod,Map<String,Set<SootMethod>>>> inac) {
		try{
			if(!inac.isEmpty()){
				ps.println("///// Start " + entryPoint.getSignature() + " /////\n");
				for(Triple<Integer,SootMethod,Map<String,Set<SootMethod>>> t : inac){
					ps.println("Depth: " + t.getFirst() + " SrcMethod: " + t.getSecond().getSignature() + "\n");
					ArrayList<String> subSigs = new ArrayList<String>(t.getThird().keySet());
					SortingMethods.sort(subSigs);
					for(String s : subSigs){
						ps.println("\t"+s);
						ArrayList<SootMethod> ms = new ArrayList<SootMethod>(t.getThird().get(s));
						SootSort.sortListSootMethod(ms);
						for(SootMethod m : ms){
							ps.println("\t\t"+m.getSignature());
						}
						ps.println();
					}
				}
				ps.println("///// End " + entryPoint.getSignature() + " /////\n");
			}
		}catch (Throwable t){
			logger.fatal("{}: Failed to output call graph inaccuracies data for stub '{}' and entry point '{}'.",t,cn,stub,entryPoint);
			throw new IgnorableRuntimeException();
		}finally{
			if(ps != null){
				try {
					ps.close();
				} catch(Throwable t){}
			}
		}
		logger.fineInfo("{}: Succeded in outputing call graph inaccuracies data for stub '{}' and entry point '{}'.",cn,stub,entryPoint);
	}
	
}