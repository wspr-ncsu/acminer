package org.sag.acminer.phases.binderservices;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.database.binderservices.IBinderServicesDatabase;
import org.sag.common.io.FileHash;
import org.sag.common.io.FileHashList;
import org.sag.common.io.FileHelpers;
import org.sag.common.io.PrintStreamUnixEOL;
import org.sag.common.tuple.Quad;
import org.sag.main.config.PhaseConfig;
import org.sag.main.phase.AbstractPhaseHandler;
import org.sag.main.phase.IPhaseHandler;
import org.sag.sootinit.BasicSootLoader;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;

public class BinderServicesHandler extends AbstractPhaseHandler {
	
	private Path jimpleJar;
	
	public BinderServicesHandler(List<IPhaseHandler> depPhases, PhaseConfig pc) {
		super(depPhases, pc);
	}
	
	@Override
	protected void initInner() {
		this.jimpleJar = dependencyFilePaths.get(0);
	}
	
	@Override
	protected List<FileHash> getOldDependencyFileHashes() throws Exception {
		return ((IACMinerDataAccessor)dataAccessor).getBinderServiceDB().getFileHashList();
	}

	@Override
	protected void loadExistingInformation() throws Exception {
		((IACMinerDataAccessor)dataAccessor).setBinderServiceDB(IBinderServicesDatabase.Factory.readXML(null, getOutputFilePath()));
	}

	@Override
	protected boolean isSootInitilized() {
		return BasicSootLoader.v().isSootLoaded();
	}

	@Override
	protected boolean initilizeSoot() {
		return BasicSootLoader.v().load(jimpleJar,true,ai.getJavaVersion(),logger);
	}
	
	@Override
	protected boolean doWork() {
		try {
			//Set new databases 
			((IACMinerDataAccessor)dataAccessor).setBinderServiceDB(IBinderServicesDatabase.Factory.getNew(false));
			DiscoverBinderServices finder = new DiscoverBinderServices(((IACMinerDataAccessor)dataAccessor), logger);
			Map<SootClass,Map<SootClass,Set<Quad<String,SootMethod,Unit,Boolean>>>> data;
			if((data = finder.run()) == null) {
				logger.fatal("{}: Failed to find all instances where a service is registered with the service manager.",cn);
				return false;
			}
			
			try(PrintStreamUnixEOL ps = new PrintStreamUnixEOL(Files.newOutputStream(
					dataAccessor.getConfig().getFilePath("acminer_registered-services-temp-file")))) {
				for(SootClass stub : data.keySet()) {
					Map<SootClass,Set<Quad<String,SootMethod,Unit,Boolean>>> data2 = data.get(stub);
					for(SootClass service : data2.keySet()) {
						if(service != null) //Some stubs may not have any defined services
							ps.println(service.toString());
					}
				}
			}
			
			List<Path> fullPaths = dependencyFilePaths;
			List<Path> realtivePaths = new ArrayList<>(fullPaths.size());
			for(Path p : fullPaths) {
				realtivePaths.add(rootPath.relativize(p));
			}
			FileHashList fhl = FileHelpers.genFileHashList(fullPaths, realtivePaths);
			((IACMinerDataAccessor)dataAccessor).getBinderServiceDB().setFileHashList(fhl);
			((IACMinerDataAccessor)dataAccessor).getBinderServiceDB().writeXML(null, getOutputFilePath());
		} catch(Throwable t) {
			logger.fatal("{}: Unexpected exception when trying to find all instances where a service is registered "
					+ "with the service manager. Failed to output '{}'",t,cn,getOutputFilePath());
			return false;
		}
		return true;
	}

}
