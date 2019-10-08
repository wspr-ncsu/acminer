package org.sag.acminer.phases.acminerdebug.handler;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.database.excludedelements.IExcludeHandler;
import org.sag.acminer.phases.acminerdebug.task.WriteFileTask;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.concurrent.IgnorableRuntimeException;
import org.sag.common.concurrent.LoggingWorkerGroup;
import org.sag.common.concurrent.WorkerCountingThreadExecutor;
import org.sag.common.io.FileHelpers;
import org.sag.common.io.PrintStreamUnixEOL;
import org.sag.common.logging.ILogger;
import org.sag.common.logging.LoggerWrapperSLF4J;
import org.sag.common.tools.SortingMethods;
import org.sag.common.tuple.Pair;
import org.sag.common.tuple.Triple;
import org.sag.soot.SootSort;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class DataDumpsOutputHandler extends AbstractOutputHandler {
	
	private final ILogger logger;
	private final WorkerCountingThreadExecutor exe;
	private final String pn;

	public DataDumpsOutputHandler(String pn, WorkerCountingThreadExecutor exe, Path rootOutputDir, IACMinerDataAccessor dataAccessor, ILogger logger){
		super(rootOutputDir,dataAccessor);
		Objects.requireNonNull(exe);
		if(logger == null)
			logger = new LoggerWrapperSLF4J(this.getClass());
		this.logger = logger;
		this.exe = exe;
		this.pn = pn;
	}
	
	private final class DumpCallGraphSizes implements WriteFileTask {
		
		private final int digits(int n) {
			int len = String.valueOf(n).length();
			if(n < 0)
				return len - 1;
			else
				return len;
		}
		
		private final String padNum(int n, int digits) {
			return String.format("%"+digits+"d", n);
		}
		
		private final int roundUpToTen(int n) {
			int rmd = n % 10;
			if(rmd != 0)
				return (10 - rmd) + n;
			return n;
		}
		
		public void run() {
			logger.info("{}: Dumping the call graph sizes.",cn);
			Map<EntryPoint,Pair<Integer,Integer>> epToSize = new LinkedHashMap<>();
			CallGraph cg = Scene.v().getCallGraph();
			int maxNode = 0;
			int minNode = Integer.MAX_VALUE;
			int maxEdge = 0;
			int minEdge = Integer.MAX_VALUE;
			List<Triple<Integer,Integer,AtomicInteger>> nodeRangeList = new ArrayList<>();
			List<Triple<Integer,Integer,AtomicInteger>> edgeRangeList = new ArrayList<>();
			try {
				for(EntryPoint ep : dataAccessor.getEntryPoints()) {
					SootMethod entryPoint = ep.getEntryPoint();
					IExcludeHandler excludeHandler = dataAccessor.getExcludedElementsDB().createNewExcludeHandler(ep);
					Queue<SootMethod> tovisit = new ArrayDeque<SootMethod>();
					HashSet<SootMethod> visited = new HashSet<>();
					HashSet<Edge> edges = new HashSet<>();
					tovisit.add(entryPoint);
					while(!tovisit.isEmpty()) {
						SootMethod currMeth = tovisit.poll();
						visited.add(currMeth);
						Iterator<Edge> itEdge = cg.edgesOutOf(currMeth);
						while(itEdge.hasNext()) {
							Edge e = itEdge.next();
							SootMethod tgt = e.tgt();
							if(!excludeHandler.isExcludedMethodWithOverride(tgt)) {
								edges.add(e);
								if(!currMeth.equals(tgt) && !visited.contains(tgt) && !tovisit.contains(tgt))
									tovisit.add(tgt);
							}
						}
					}
					epToSize.put(ep, new Pair<>(visited.size(),edges.size()));
				}
				for(Pair<Integer,Integer> p : epToSize.values()) {
					int nodes = p.getFirst();
					int edges = p.getSecond();
					if(maxNode < nodes)
						maxNode = nodes;
					if(minNode > nodes)
						minNode = nodes;
					if(maxEdge < edges)
						maxEdge = edges;
					if(minEdge > edges)
						minEdge = edges;
				}
				
				int nodeDiv = roundUpToTen(maxNode+1) / 10;
				int edgeDiv = roundUpToTen(maxEdge+1) / 10;
				for(int i = 0; i < 10; i++) {
					nodeRangeList.add(new Triple<Integer, Integer, AtomicInteger>(i * nodeDiv, (i+1) * nodeDiv, new AtomicInteger(0)));
					edgeRangeList.add(new Triple<Integer, Integer, AtomicInteger>(i * edgeDiv, (i+1) * edgeDiv, new AtomicInteger(0)));
				}
				for(Pair<Integer,Integer> p : epToSize.values()) {
					for(Triple<Integer,Integer,AtomicInteger> t : nodeRangeList) {
						if(p.getFirst() >= t.getFirst() && p.getFirst() < t.getSecond()) {
							t.getThird().incrementAndGet();
							break;
						}
					}
					for(Triple<Integer,Integer,AtomicInteger> t : edgeRangeList) {
						if(p.getSecond() >= t.getFirst() && p.getSecond() < t.getSecond()) {
							t.getThird().incrementAndGet();
							break;
						}
					}
				}
			} catch(Throwable t){
				logger.fatal("{}: An error occured when generating the call graph sizes for dumping.",t,cn);
				throw new IgnorableRuntimeException();
			}
			
			try(PrintStream out = new PrintStreamUnixEOL(Files.newOutputStream(FileHelpers.getPath(rootOutputDir, "call_graph_size_dump.txt")))) {
				out.println("Max Nodes: " + maxNode);
				out.println("Min Nodes: " + minNode);
				out.println("Max Edges: " + maxEdge);
				out.println("Min Edges: " + minEdge);
				out.println("Node Ranges:");
				for(Triple<Integer,Integer,AtomicInteger> t : nodeRangeList) {
					out.println("  [" + t.getFirst() + ", " + t.getSecond() + ") = " + t.getThird().get());
				}
				out.println("Edge Ranges:");
				for(Triple<Integer,Integer,AtomicInteger> t : edgeRangeList) {
					out.println("  [" + t.getFirst() + ", " + t.getSecond() + ") = " + t.getThird().get());
				}
				int digitsNode = digits(maxNode);
				int digitsEdge = digits(maxEdge);
				SootClass stub = null;
				for(EntryPoint ep : epToSize.keySet()) {
					if(stub == null || !stub.equals(ep.getStub())) {
						stub = ep.getStub();
						out.println("\nStub: " + stub.toString());
					}
					out.println("  Nodes: " + padNum(epToSize.get(ep).getFirst(),digitsNode) 
					+ " Edges: " + padNum(epToSize.get(ep).getSecond(),digitsEdge) + " EntryPoint: " + ep.getEntryPoint().toString());
				}
			} catch(Throwable t) {
				logger.fatal("{}: An error occured when dumping the call graph sizes.",t,cn);
				throw new IgnorableRuntimeException();
			}
			logger.info("{}: Successfully dumped the call graph sizes.",cn);
		}
	}
	
	private final class DumpMarker implements WriteFileTask {
		public void run() {
			logger.info("{}: Dumping the Marker generated data.",cn);
			PrintStream out = null;
			PrintStream out2 = null;
			try{
				out = new PrintStreamUnixEOL(Files.newOutputStream(FileHelpers.getPath(rootOutputDir, "marker_dump.txt")));
				out2 = new PrintStreamUnixEOL(Files.newOutputStream(FileHelpers.getPath(rootOutputDir, "marker_dump_simple.txt")));
				Set<EntryPoint> eps = dataAccessor.getEntryPoints();
				SootClass stub = null;
				String spacer = "    ";
				
				Set<Unit> stubCPUnits = null;
				Set<Unit> stubCQUnits = null;
				Set<Unit> stubSEUnits = null;
				StringBuilder simpleSB = null;
				
				for(EntryPoint ep : eps){
					StringBuilder sb = new StringBuilder();
					SootMethod entryPoint = ep.getEntryPoint();
					if(stub == null || !stub.equals(ep.getStub())){
						if(stub != null) {
							simpleSB.insert(0,"Stub '" + stub + "':"
									+ "\n  Unique Stub Control Predicate Units: " + stubCPUnits.size() 
									+ "\n  Unique Stub Context Query Units: " + stubCQUnits.size()
									+ "\n  Unique Stub Throw SecurityException Units: " + stubSEUnits.size()
									+ "\n");
							out2.print(simpleSB.toString());
						}
						
						stubCPUnits = new HashSet<>();
						stubCQUnits = new HashSet<>();
						stubSEUnits = new HashSet<>();
						simpleSB = new StringBuilder();
						stub = ep.getStub();
						sb.append("Stub '").append(stub).append("':\n");
					}
					
					Set<Unit> epCPUnits = new HashSet<>();
					Set<Unit> epCQUnits = new HashSet<>();
					Set<Unit> epSEUnits = new HashSet<>();
					
					sb.append("  Entry Point '").append(entryPoint).append("':\n");
					Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> controlPredicateData = SootSort.sortMapByMethodKeyAscending(
							dataAccessor.getControlPredicatesDB().getData(ep));
					Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> contextQueryData = SootSort.sortMapByMethodKeyAscending(
							dataAccessor.getContextQueriesDB().getData(ep));
					Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> throwSecurityExceptionStmtData = SootSort.sortMapByMethodKeyAscending(
							dataAccessor.getThrowSecurityExceptionStmtsDB().getData(ep));
					sb.append(formatMarksDump(controlPredicateData,"Control Predicates",spacer));
					sb.append(formatMarksDump(contextQueryData, "Context Queries", spacer));
					sb.append(formatMarksDump(throwSecurityExceptionStmtData, "Throw SecurityExceptionStmtData", spacer));
					out.print(sb.toString());
					
					for(Pair<Set<Unit>, Set<Integer>> p : controlPredicateData.values()) {
						epCPUnits.addAll(p.getFirst());
						stubCPUnits.addAll(p.getFirst());
					}
					for(Pair<Set<Unit>, Set<Integer>> p : contextQueryData.values()) {
						epCQUnits.addAll(p.getFirst());
						stubCQUnits.addAll(p.getFirst());
					}
					for(Pair<Set<Unit>, Set<Integer>> p : throwSecurityExceptionStmtData.values()) {
						epSEUnits.addAll(p.getFirst());
						stubSEUnits.addAll(p.getFirst());
					}
					
					simpleSB.append("  Entry Point '").append(entryPoint).append("':\n");
					simpleSB.append("    Unique Control Predicate Units: ").append(epCPUnits.size()).append("\n");
					simpleSB.append("    Unique Context Query Units: ").append(epCQUnits.size()).append("\n");
					simpleSB.append("    Unique Throw SecurityException Units: ").append(epSEUnits.size()).append("\n");
					
				}
				
				if(stub != null) {
					simpleSB.insert(0,"Stub '" + stub + "':"
							+ "\n  Unique Stub Control Predicate Units: " + stubCPUnits.size() 
							+ "\n  Unique Stub Context Query Units: " + stubCQUnits.size()
							+ "\n  Unique Stub Throw SecurityException Units: " + stubSEUnits.size());
					out2.print(simpleSB.toString());
				}
			}catch(Throwable t){
				logger.fatal("{}: An error occured when dumping the Marker generated data.",t,cn);
				throw new IgnorableRuntimeException();
			} finally {
				if(out != null){
					try{
						out.close();
					}catch(Throwable t){}
				}
				if(out2 != null) {
					try {
						out2.close();
					} catch(Throwable t){}
				}
			}
			logger.info("{}: Successfully dumped the Marker generated data.",cn);
		}
	}
	
	private static String formatMarksDump(Map<SootMethod, Pair<Set<Unit>,Set<Integer>>> data, String name, String spacer){
		if(spacer == null)
			spacer = "";
		StringBuilder sb = new StringBuilder();
		sb.append(spacer).append(name).append(" Start:\n");
		int count = 0;
		for(SootMethod source : data.keySet()){
			Pair<Set<Unit>,Set<Integer>> p = data.get(source);
			Set<Unit> units = SortingMethods.sortSet(p.getFirst(),SootSort.unitComp);
			sb.append(spacer).append("  Source Method: ").append(source.toString()).append("\n");
			sb.append(spacer).append("    Depths: ").append(SortingMethods.sortSet(p.getSecond())).append("\n");
			sb.append(spacer).append("    Units Count: ").append(units.size()).append("\n");
			sb.append(spacer).append("    Units: \n");
			count += units.size();
			for(Unit u : units){
				sb.append(spacer).append("      ").append(u).append("\n");
			}
		}
		sb.append(spacer).append(name).append(" End - Total Units Count: ").append(count).append("\n");
		return sb.toString();
	}
	
	private final class DumpEntryPoints implements WriteFileTask {
		public void run() {
			logger.info("{}: Dumping the entry points data.",cn);
			PrintStream out = null;
			try {
				out = new PrintStreamUnixEOL(Files.newOutputStream(FileHelpers.getPath(rootOutputDir, "entry_points_dump.txt")));
				Set<EntryPoint> eps = dataAccessor.getEntryPoints();
				SootClass stub = null;
				int totalStubs = 0;
				int totalEps = 0;
				AtomicInteger epsCount = null;
				Map<SootClass,AtomicInteger> stubToNumEps = new HashMap<>();
				for(EntryPoint ep : eps){
					if(stub == null || !stub.equals(ep.getStub())){
						stub = ep.getStub();
						totalStubs++;
						epsCount = new AtomicInteger();
						stubToNumEps.put(stub, epsCount);
					}
					totalEps++;
					epsCount.incrementAndGet();
				}
				
				stub = null;
				out.println("Total # of Stubs: " + totalStubs);
				out.println("Total # of Entry Points: " + totalEps);
				for(EntryPoint ep : eps){
					StringBuilder sb = new StringBuilder();
					SootMethod entryPoint = ep.getEntryPoint();
					if(stub == null || !stub.equals(ep.getStub())){
						stub = ep.getStub();
						sb.append("Stub '").append(stub).append("':\n");
						sb.append("  Total # of Entry Points: ").append(stubToNumEps.get(stub).get()).append("\n");
					}
					sb.append("  Entry Point: ").append(entryPoint).append("\n");
					out.print(sb.toString());
				}
			} catch(Throwable t){
				logger.fatal("{}: An error occured when dumping the entry points data.",t,cn);
				throw new IgnorableRuntimeException();
			} finally {
				if(out != null){
					try{
						out.close();
					}catch(Throwable t){}
				}
			}
			logger.info("{}: Successfully dumped the entry points data.",cn);
		}
	}
	
	private final class DumpSecondaryEntryPoints implements WriteFileTask {
		public void run() {
			logger.info("{}: Dumping the secondary entry points data.",cn);
			PrintStream out = null;
			try {
				out = new PrintStreamUnixEOL(Files.newOutputStream(FileHelpers.getPath(rootOutputDir, "secondary_entry_points_dump.txt")));
				Set<EntryPoint> eps = dataAccessor.getEntryPoints();
				SootClass stub = null;
				for(EntryPoint ep : eps){
					StringBuilder sb = new StringBuilder();
					SootMethod entryPoint = ep.getEntryPoint();
					if(stub == null || !stub.equals(ep.getStub())){
						stub = ep.getStub();
						sb.append("Stub: ").append(stub).append("\n");
					}
					sb.append("  Entry Point: ").append(entryPoint).append("\n");
					Map<EntryPoint, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> iepToSourceToData = 
							dataAccessor.getEntryPointEdgesDB().getInternalData(ep);
					Map<EntryPoint, Map<SootMethod, Pair<Set<Unit>, Set<Integer>>>> eepToSourceToData = 
							dataAccessor.getEntryPointEdgesDB().getExternalData(ep);
					
					sb.append("    Internal Referenced Entry Points: ").append(iepToSourceToData.size()).append("\n");
					for(EntryPoint iep : iepToSourceToData.keySet()) {
						sb.append("      Referenced Entry Point: ").append(iep.toString()).append("\n");
						Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> sourceToData = iepToSourceToData.get(iep);
						for(SootMethod source : sourceToData.keySet()) {
							Pair<Set<Unit>, Set<Integer>> p = sourceToData.get(source);
							sb.append("        Source Method: ").append(source).append("\n");
							sb.append("          Depths: ").append(p.getSecond()).append("\n");
							sb.append("          Units:\n");
							for(Unit u : p.getFirst()) {
								sb.append("            ").append(u).append("\n");
							}
						}
					}
					sb.append("    External Referenced Entry Points: ").append(eepToSourceToData.size()).append("\n");
					for(EntryPoint eep : eepToSourceToData.keySet()) {
						sb.append("      Referenced Entry Point: ").append(eep.toString()).append("\n");
						Map<SootMethod, Pair<Set<Unit>, Set<Integer>>> sourceToData = eepToSourceToData.get(eep);
						for(SootMethod source : sourceToData.keySet()) {
							Pair<Set<Unit>, Set<Integer>> p = sourceToData.get(source);
							sb.append("        Source Method: ").append(source).append("\n");
							sb.append("          Depths: ").append(p.getSecond()).append("\n");
							sb.append("          Units:\n");
							for(Unit u : p.getFirst()) {
								sb.append("            ").append(u).append("\n");
							}
						}
					}
					out.print(sb.toString());
				}
			} catch(Throwable t){
				logger.fatal("{}: An error occured when dumping the secondary entry points data.",t,cn);
				throw new IgnorableRuntimeException();
			} finally {
				if(out != null){
					try{
						out.close();
					}catch(Throwable t){}
				}
			}
			logger.info("{}: Successfully dumped the secondary entry points data.",cn);
		}
	}
	
	private final class DumpBinderGroups implements WriteFileTask {
		public void run() {
			logger.info("{}: Dumping the binder groups data.",cn);
			PrintStream out = null;
			try {
				out = new PrintStreamUnixEOL(Files.newOutputStream(FileHelpers.getPath(rootOutputDir, "binder_groups_dump.txt")));
				
				Map<SootClass,Set<SootMethod>> interfacesAndMethods = dataAccessor.getBinderInterfacesAndMethods();
				Map<SootClass,Map<SootClass,Map<SootMethod,Set<Integer>>>> proxiesAndMethodsByInterface = 
						dataAccessor.getBinderProxiesAndMethodsByInterface();
				Map<SootClass,Map<SootClass,Map<SootMethod,Set<Integer>>>> stubsAndEntryPointsByInterface = 
						dataAccessor.getBinderStubsAndMethodsByInterface();
				Map<SootClass,Map<String,Set<SootMethod>>> stubMethodsToEntryPointsByInterface = 
						dataAccessor.getBinderStubMethodsToEntryPointsByInterface();
				
				int totalInterfaces = interfacesAndMethods.size();
				int totalProxies = dataAccessor.getBinderProxiesAndMethods().size();
				int totalStubs = dataAccessor.getBinderStubsAndMethods().size();
				int totalInterfaceMethods = dataAccessor.getBinderInterfaceMethodsToProxyMethods().size();
				int totalProxyMethods = dataAccessor.getBinderProxyMethodsToEntryPoints().size();
				int totalStubMethods = dataAccessor.getBinderStubMethodsToEntryPoints().size();
				
				out.println("Total # of Binder Interfaces: " + totalInterfaces);
				out.println("Total # of Binder Proxies: " + totalProxies);
				out.println("Total # of Binder Stubs: " + totalStubs);
				out.println("Total # of Binder Interface Methods: " + totalInterfaceMethods);
				out.println("Total # of Binder Proxy Methods: " + totalProxyMethods);
				out.println("Total # of Binder Stub Methods: " + totalStubMethods);
				for(SootClass bInterface : interfacesAndMethods.keySet()){
					StringBuilder sb = new StringBuilder();
					Set<SootMethod> interfaceMethods = interfacesAndMethods.get(bInterface);
					Map<SootClass,Map<SootMethod,Set<Integer>>> proxiesAndMethods = proxiesAndMethodsByInterface.get(bInterface);
					Map<SootClass,Map<SootMethod,Set<Integer>>> stubAndEntryPoints = stubsAndEntryPointsByInterface.get(bInterface);
					Map<String,Set<SootMethod>> stubMethodsToEntryPoints = stubMethodsToEntryPointsByInterface.get(bInterface);
					sb.append("Binder Interface '").append(bInterface).append("':\n");
					sb.append("    Total # of Binder Interface Methods: ").append(interfaceMethods.size()).append("\n");
					for(SootMethod sm : interfaceMethods){
						sb.append("    Binder Interface Method: ").append(sm).append("\n");
					}
					sb.append("  Total # of Binder Proxies for Binder Interface: ").append(proxiesAndMethods.size()).append("\n");
					for(SootClass bProxy : proxiesAndMethods.keySet()){
						Map<SootMethod,Set<Integer>> proxyMethods = proxiesAndMethods.get(bProxy);
						sb.append("    Binder Proxy '").append(bProxy).append("':\n");
						sb.append("      Total # of Binder Proxy Methods: ").append(proxyMethods.size()).append("\n");
						for(SootMethod m : proxyMethods.keySet()){
							sb.append("      Binder Proxy Method '").append(m).append("': ").append(proxyMethods.get(m)).append("\n");
						}
					}
					int totalNumOfEps = 0;
					sb.append("  Total # of Binder Stubs for Binder Interface: ").append(stubAndEntryPoints.size()).append("\n");
					for(SootClass bStub : stubAndEntryPoints.keySet()){
						Map<SootMethod,Set<Integer>> entryPointMethods = stubAndEntryPoints.get(bStub);
						totalNumOfEps += entryPointMethods.size();
						sb.append("    Binder Stub '").append(bStub).append("':\n");
						int stubMethodCount = 0;
						StringBuilder sb2 = new StringBuilder();
						for(String stubMethodSig : stubMethodsToEntryPoints.keySet()){
							if(Scene.v().signatureToClass(stubMethodSig).equals(bStub.getName())){
								Set<Integer> ids = new HashSet<>();
								for(SootMethod ep : stubMethodsToEntryPoints.get(stubMethodSig)){
									ids.addAll(entryPointMethods.get(ep));
								}
								ids = SortingMethods.sortSet(ids,SortingMethods.iComp);
								sb2.append("      Binder Stub Method '").append(stubMethodSig).append("': ").append(ids).append("\n");
								stubMethodCount++;
							}
						}
						sb.append("      Total # of Binder Stub Methods: ").append(stubMethodCount).append("\n");
						sb.append(sb2);
					}
					sb.append("  Total # of Entry Points for Binder Interface: ").append(totalNumOfEps).append("\n");
					for(SootClass bStub : stubAndEntryPoints.keySet()){
						Map<SootMethod,Set<Integer>> entryPointMethods = stubAndEntryPoints.get(bStub);
						for(SootMethod ep : entryPointMethods.keySet()){
							sb.append("    Entry Point '").append(ep).append("': ").append(entryPointMethods.get(ep)).append("\n");
						}
					}
					out.print(sb.toString());
				}
			} catch(Throwable t){
				logger.fatal("{}: An error occured when dumping the binder groups data.",t,cn);
				throw new IgnorableRuntimeException();
			} finally {
				if(out != null){
					try{
						out.close();
					}catch(Throwable t){}
				}
			}
			logger.info("{}: Successfully dumped the binder groups data.",cn);
		}
	}
	
	private final class DumpBinderResolveRelationships implements WriteFileTask {
		@Override
		public void run() {
			logger.info("{}: Dumping the binder resolve relationships.",cn);
			PrintStream out = null;
			try {
				out = new PrintStreamUnixEOL(Files.newOutputStream(FileHelpers.getPath(rootOutputDir, "binder_resolve_relationships_dump.txt")));
				
				Map<SootClass,Set<SootMethod>> interfacesAndMethods = dataAccessor.getBinderInterfacesAndMethods();
				for(SootClass bInterface : interfacesAndMethods.keySet()){
					StringBuilder sb = new StringBuilder();
					Map<String,Set<SootMethod>> stubMethodsToEntryPointsForInterface = 
							dataAccessor.getBinderStubMethodsToEntryPointsForInterface(bInterface);
					sb.append("Binder Interface '").append(bInterface).append("':\n");
					for(SootMethod bInterfaceMethod : interfacesAndMethods.get(bInterface)){
						sb.append("  Binder Interface Method: ").append(bInterfaceMethod).append("\n");
						Set<SootMethod> proxyMethods = dataAccessor.getBinderProxyMethodsForInterfaceMethod(bInterfaceMethod);
						for(SootMethod bProxyMethod : proxyMethods){
							sb.append("    Binder Proxy Method: ").append(bProxyMethod).append("\n");
							Set<SootMethod> entryPoints = dataAccessor.getEntryPointsForBinderProxyMethod(bProxyMethod);
							Map<String,Set<SootMethod>> stubsMethodsToEntryPoints = new HashMap<>();
							for(SootMethod ep : entryPoints){
								for(Map.Entry<String,Set<SootMethod>> e : stubMethodsToEntryPointsForInterface.entrySet()){
									if(e.getValue().contains(ep)){
										stubsMethodsToEntryPoints.put(e.getKey(), e.getValue());
									}
								}
							}
							for(String stubMethod : stubsMethodsToEntryPoints.keySet()){
								sb.append("      Binder Stub Method: ").append(stubMethod).append("\n");
								for(SootMethod ep : stubsMethodsToEntryPoints.get(stubMethod)){
									sb.append("      Entry Point: ").append(ep).append("\n");
								}
							}
						}
					}
				}
				
			} catch(Throwable t){
				logger.fatal("{}: An error occured when dumping the binder resolve relationships.",t,cn);
				throw new IgnorableRuntimeException();
			} finally {
				if(out != null){
					try{
						out.close();
					}catch(Throwable t){}
				}
			}
			logger.info("{}: Successfully dumped the binder resolve relationships.",cn);
		}
	}
	
	
	public LoggingWorkerGroup run(){
		try{
			FileHelpers.processDirectory(rootOutputDir,true,true);
		}catch(Throwable t){
			logger.fatal("{}: Failed to create and verify the database dump directory. Skipping the database dumps.",t,cn);
			return null;
		}
		LoggingWorkerGroup g = new LoggingWorkerGroup(pn,cn,logger,false,false);
		try {
			exe.execute(new DumpCallGraphSizes(), g);
		} catch(Throwable t) {
			logger.fatal("{}: An unexpected error occured when dumping the call graph sizes.",t,cn);
			g.addFailedToExecuteException(t);
		}
		try{
			exe.execute(new DumpEntryPoints(),g);
		}catch(Throwable t){
			logger.fatal("{}: An unexpected error occured when dumping the entry points data.",t,cn);
			g.addFailedToExecuteException(t);
		}
		try{
			exe.execute(new DumpBinderGroups(),g);
		}catch(Throwable t){
			logger.fatal("{}: An unexpected error occured when dumping the binder groups data.",t,cn);
			g.addFailedToExecuteException(t);
		}
		try{
			exe.execute(new DumpBinderResolveRelationships(), g);
		}catch(Throwable t){
			logger.fatal("{}: An unexpected error occured when dumping the binder resolve relationships.",t,cn);
			g.addFailedToExecuteException(t);
		}
		try{
			exe.execute(new DumpSecondaryEntryPoints(), g);
		}catch(Throwable t){
			logger.fatal("{}: An unexpected error occured when dumping the secondary entry points data.",t,cn);
			g.addFailedToExecuteException(t);
		}
		try{
			exe.execute(new DumpMarker(),g);
		}catch(Throwable t){
			logger.fatal("{}: An unexpected error occured when dumping the Marker generated data.",t,cn);
			g.addFailedToExecuteException(t);
		}
		g.unlockInitialLock();
		return g;
	}
	
}
