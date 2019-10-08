package org.sag.acminer.phases.acminerdebug;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.database.excludedelements.IExcludeHandler;
import org.sag.acminer.phases.acminerdebug.handler.AbstractOutputHandler;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.common.concurrent.IgnorableRuntimeException;
import org.sag.common.concurrent.LoggingWorkerGroup;
import org.sag.common.concurrent.WorkerCountingThreadExecutor;
import org.sag.common.io.FileHelpers;
import org.sag.common.io.PrintStreamUnixEOL;
import org.sag.common.logging.ILogger;
import org.sag.common.logging.LoggerWrapperSLF4J;
import org.sag.soot.SootSort;

import com.google.common.collect.ImmutableList;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.EdgePredicate;
import soot.jimple.toolkits.callgraph.Filter;

public class PathsOutputHandler extends AbstractOutputHandler {
	
	private final WorkerCountingThreadExecutor exe;
	private final ILogger logger;
	private final CallGraph cg;
	private final Set<SootMethod> entryPoints;

	public PathsOutputHandler(WorkerCountingThreadExecutor exe, Path rootOutputDir, IACMinerDataAccessor dataAccessor, ILogger logger) {
		super(rootOutputDir, dataAccessor);
		this.exe = exe;
		this.logger = logger == null ? new LoggerWrapperSLF4J(CommonSubgraphOutputHandler.class) : logger;
		this.cg = Scene.v().getCallGraph();
		this.entryPoints = dataAccessor.getEntryPointsAsSootMethods();
	}
	
	private synchronized void executeRunner(Runnable runner, LoggingWorkerGroup g, ILogger logger) {
		try {
			exe.execute(runner,g);
		} catch(Throwable t) {
			logger.fatal("{}: Failed to execute '{}' for group '{}'.",t,cn,runner.toString(),g.getName());
			throw new IgnorableRuntimeException();
		}
	}
	
	public List<LoggingWorkerGroup> run() {
		try {
			FileHelpers.processDirectory(rootOutputDir,true,true);
		} catch(Throwable t) {
			logger.fatal("{}: Failed to create and verify the output directory. Skipping.",t,cn);
			throw new IgnorableRuntimeException();
		}
		
		List<LoggingWorkerGroup> workerGroups = new ArrayList<>();
		try {
			SootClass stub = null;
			LoggingWorkerGroup curWorkerGroup = null;
			Deque<EntryPoint> eps = new ArrayDeque<>(dataAccessor.getEntryPoints());
			while(!eps.isEmpty()) {
				EntryPoint ep = eps.poll();
				if(stub == null || !stub.equals(ep.getStub())) {
					stub = ep.getStub();
					if(curWorkerGroup != null) {
						curWorkerGroup.unlockInitialLock();
						curWorkerGroup = null;
					}
					LoggingWorkerGroup g = new LoggingWorkerGroup(cn,stub.toString(),false);
					if(g.getLogger() == null) {
						logger.fatal("{}: Failed to initilize local logger for '{}'. Skipping '{}'.",cn,stub,stub);
					} else {
						curWorkerGroup = g;
						workerGroups.add(g);
					}	
				}
				if(curWorkerGroup != null) {
					Runner r = new Runner(ep, cg, dataAccessor.getExcludedElementsDB().createNewExcludeHandler(ep), rootOutputDir, entryPoints);
					executeRunner(r, curWorkerGroup, logger);
				}
			}
			//Unlock the initial lock for the last group produced by the loop
			if(curWorkerGroup != null) {
				curWorkerGroup.unlockInitialLock();
				curWorkerGroup = null;
			}
		} catch(Throwable t) {
			logger.fatal("{}: An unexpected exception occured.",t,cn);
			throw new IgnorableRuntimeException();
		}
		return workerGroups;
	}
	
	private static final class Runner implements Runnable {
		
		private final EntryPoint ep;
		private final CallGraph cg;
		private final Filter filter;
		private final Path rootOutputDir;
		private final Set<SootMethod> eps;
		
		public Runner(EntryPoint ep, CallGraph cg, IExcludeHandler excludeHandler, Path rootOutputDir, Set<SootMethod> eps) {
			this.ep = ep;
			this.cg = cg;
			this.filter = new FFilter(excludeHandler);
			this.rootOutputDir = rootOutputDir;
			this.eps = eps;
		}
		
		private static class FFilter extends Filter {
			public FFilter(IExcludeHandler excludeHandler) {
				super(new EdgePredicate() {
					@Override
					public boolean want(Edge e) {
						return !excludeHandler.isExcludedMethodWithOverride(e.src());
					}
				});
			}
		}
		
		@Override
		public void run() {
			try {
				List<List<SootMethod>> paths = new ArrayList<>();
				Set<SootMethod> visited = new HashSet<>();
				Queue<SootMethod> toVisit = new ArrayDeque<>();
				Queue<List<SootMethod>> pathsToVisit = new ArrayDeque<>();
				
				toVisit.add(ep.getEntryPoint());
				pathsToVisit.add(ImmutableList.of(ep.getEntryPoint()));
				while(!toVisit.isEmpty()) {
					SootMethod cur = toVisit.poll();
					List<SootMethod> path = pathsToVisit.poll();
					if(visited.add(cur)) {
						Iterator<Edge> it = filter.wrap(cg.edgesOutOf(cur));
						boolean hasEdges = false;
						while(it.hasNext()) {
							hasEdges = true;
							Edge e = it.next();
							toVisit.add(e.tgt());
							pathsToVisit.add(ImmutableList.<SootMethod>builder().addAll(path).add(e.tgt()).build());
						}
						
						if(!hasEdges) {
							paths.add(path);
						}
					}
				}
				
				Collections.sort(paths, new Comparator<List<SootMethod>>() {
					public int compare(List<SootMethod> o1, List<SootMethod> o2) {
						int size = Math.min(o1.size(), o2.size());
						for(int i = 0; i < size; i++) {
							SootMethod sm1 = o1.get(i);
							SootMethod sm2 = o2.get(i);
							int ret = SootSort.smComp.compare(sm1, sm2);
							if(ret != 0)
								return ret;
						}
						return Integer.compare(o1.size(), o2.size());
					}
				});
				
				Path outputPath = FileHelpers.getPath(rootOutputDir, FileHelpers.getHashOfString("MD5", ep.getStub().toString() + ep.getEntryPoint().toString()) 
						+ ".txt");
				try(PrintStream ps = new PrintStreamUnixEOL(Files.newOutputStream(outputPath))) {
					for(List<SootMethod> path : paths) {
						String spacer = "";
						for(SootMethod sm : path) {
							if(eps.contains(sm))
								ps.println(spacer + "{EP} " + sm);
							else 
								ps.println(spacer + sm);
							spacer += "  ";
						}
					}
				}
			} catch(Throwable t) {
				throw new RuntimeException(t);
			}
		}
	}
	
}
