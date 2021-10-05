package org.sag.acminer.phases.accesscontrolmarker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.database.accesscontrol.AccessControlDatabaseFactory;
import org.sag.acminer.database.entrypointedges.EntryPointEdgesDatabase;
import org.sag.acminer.database.filter.ContextQueriesDescriptorDatabase;
import org.sag.acminer.sootinit.IPASootLoader;
import org.sag.common.io.FileHash;
import org.sag.common.io.FileHashList;
import org.sag.common.io.FileHelpers;
import org.sag.common.io.PrintStreamUnixEOL;
import org.sag.common.tools.SortingMethods;
import org.sag.main.config.PhaseConfig;
import org.sag.main.phase.AbstractPhaseHandler;
import org.sag.main.phase.IPhaseHandler;
import org.sag.main.sootinit.SootInstanceWrapper;
import org.sag.soot.SootSort;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

import com.google.common.collect.ImmutableSet;

public class AccessControlMarkerHandler extends AbstractPhaseHandler {
	
	private Path jimpleJar;
	private Path contextQueriesDescriptor;
	
	public AccessControlMarkerHandler(List<IPhaseHandler> depPhases, PhaseConfig pc) {
		super(depPhases, pc);
	}
	
	@Override
	protected void initInner() {
		if(outFilePaths == null || outFilePaths.size() < 3)
			throw new IllegalArgumentException("Error: The output file paths must contain 3 entries.");
		this.jimpleJar = dependencyFilePaths.get(0);
		this.contextQueriesDescriptor = dependencyFilePaths.get(dependencyFilePaths.size()-1);
	}

	//If for some reason the required input file hashes do not match between the three files we add
	//the file hashes for all three entries that do not match to our returned list
	//This guarantees that the procedure will detect something is wrong and rerun the phase since
	//there is no way such a list will match the required input file list
	@Override
	protected List<FileHash> getOldDependencyFileHashes() throws Exception {
		List<FileHash> ret = new ArrayList<>();
		List<FileHash> a = ((IACMinerDataAccessor)dataAccessor).getContextQueriesDB().getFileHashList();
		List<FileHash> b = ((IACMinerDataAccessor)dataAccessor).getThrowSecurityExceptionStmtsDB().getFileHashList();
		List<FileHash> c = ((IACMinerDataAccessor)dataAccessor).getEntryPointEdgesDB().getFileHashList();
		if(a.size() == b.size() && a.size() == c.size()) {
			for(int i = 0 ; i < a.size(); i++) {
				FileHash aa = a.get(i);
				FileHash bb = b.get(i);
				FileHash cc = c.get(i);
				if(aa.equals(bb) && aa.equals(cc)) {
					ret.add(aa);
				} else if(aa.equals(bb)) {
					ret.add(aa);
					ret.add(cc);
				} else if(aa.equals(cc)) {
					ret.add(aa);
					ret.add(bb);
				} else {
					ret.add(aa);
					ret.add(bb);
					ret.add(cc);
				}
			}
		} else {
			ret.addAll(a);
			ret.addAll(b);
			ret.addAll(c);
		}
		return ret;
	}

	@Override
	protected void loadExistingInformation() throws Exception {
		if(!isSootInitilized())//To read in this database soot must be initialized completely
			initilizeSoot();
		List<Path> outFilePaths = getOutputFilePaths();
		((IACMinerDataAccessor)dataAccessor).setContextQueriesDB(AccessControlDatabaseFactory.readXmlContextQueriesDatabase(null, outFilePaths.get(0)));
		((IACMinerDataAccessor)dataAccessor).setThrowSecurityExceptionStmtsDB(
				AccessControlDatabaseFactory.readXmlThrowSecurityExceptionStmtsDatabase(null, outFilePaths.get(1)));
		((IACMinerDataAccessor)dataAccessor).setEntryPointEdgesDB(EntryPointEdgesDatabase.readXMLStatic(null, outFilePaths.get(2)));
	}

	@Override
	protected boolean isSootInitilized() {
		return IPASootLoader.v().isSootLoaded();
	}

	@Override
	protected boolean initilizeSoot() {
		return IPASootLoader.v().load(((IACMinerDataAccessor)dataAccessor), jimpleJar, ai.getJavaVersion(), logger);
	}

	@Override
	protected boolean doWork() {
		try{
			//AccessControlMarker finder = new AccessControlMarker(ContextQuerySignatureDatabase.readTXTStatic(contextQueriesDescriptor).getContextQueries(), dataAccessor,logger);
			//Determine all possible context queries within the entire android system
			ContextQueriesDescriptorDatabase cdb = ContextQueriesDescriptorDatabase.readXMLStatic(null, contextQueriesDescriptor);
			if(!SootInstanceWrapper.v().isSootInitSet())
				throw new RuntimeException("Error: Some instance of Soot must be initilized first.");
			Set<SootMethod> contextQueries = new HashSet<>();
			for(SootClass sc : Scene.v().getClasses()){
				for(SootMethod sm : sc.getMethods()){
					if(cdb.matches(sm)){
						contextQueries.add(sm);
					}
				}
			}
			contextQueries = ImmutableSet.copyOf(SortingMethods.sortSet(contextQueries,SootSort.smComp));
			
			try(PrintStreamUnixEOL ps = new PrintStreamUnixEOL(Files.newOutputStream(FileHelpers.getPath(this.rootPath, "methods_dump.txt")))) {
				for(SootClass sc : Scene.v().getClasses()) {
					for(SootMethod sm : sc.getMethods()) {
						ps.println(sm.toString());
					}
				}
			}
			
			try(PrintStreamUnixEOL ps = new PrintStreamUnixEOL(Files.newOutputStream(FileHelpers.getPath(this.rootPath, "context_queries_dump.txt")))) {
				for(SootMethod sm : contextQueries) {
					ps.println(sm.getSignature());
				}
			}
			
			//Mark the context queries and other things that actually occur for all entry points
			AccessControlMarker finder = new AccessControlMarker(contextQueries, ((IACMinerDataAccessor)dataAccessor), logger);
			if(!finder.run()){
				logger.fatal("{}: The AccessControlMarker encountered errors during executation.",cn);
				return false;
			}
			List<Path> outFilePaths = getOutputFilePaths();
			List<Path> fullPaths = dependencyFilePaths;
			List<Path> realtivePaths = new ArrayList<>(fullPaths.size());
			for(Path p : fullPaths) {
				realtivePaths.add(rootPath.relativize(p));
			}
			FileHashList fhl = FileHelpers.genFileHashList(fullPaths, realtivePaths);
			((IACMinerDataAccessor)dataAccessor).getContextQueriesDB().setFileHashList(fhl);
			((IACMinerDataAccessor)dataAccessor).getThrowSecurityExceptionStmtsDB().setFileHashList(fhl);
			((IACMinerDataAccessor)dataAccessor).getEntryPointEdgesDB().setFileHashList(fhl);
			((IACMinerDataAccessor)dataAccessor).getContextQueriesDB().writeXML(null, outFilePaths.get(0));
			((IACMinerDataAccessor)dataAccessor).getThrowSecurityExceptionStmtsDB().writeXML(null, outFilePaths.get(1));
			((IACMinerDataAccessor)dataAccessor).getEntryPointEdgesDB().writeXML(null, outFilePaths.get(2));
		}catch(Throwable t){
			logger.fatal("{}: Unexpected exception during the run of the AccessControlMarker.",t,cn);
			return false;
		}
		return true;
	}
	
}
