package org.sag.acminer.sootinit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sag.common.io.FileHelpers;
import org.sag.common.logging.ILogger;
import org.sag.common.tools.SortingMethods;
import org.sag.common.tuple.Pair;
import org.sag.main.logging.PrintStreamLoggerWrapper;
import org.sag.main.logging.SingleLineFormatter;
import org.sag.main.sootinit.SootLoader;
import org.sag.acminer.IACMinerDataAccessor;
import org.sag.acminer.database.excludedelements.EmptyExcludedElementsDatabase;
import org.sag.acminer.database.excludedelements.IExcludedElementsDatabase;
import org.sag.acminer.phases.entrypoints.GenerateEntryPoints;
import org.sag.soot.SootSort;
import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.options.Options;

public class IPASootLoader extends SootLoader {
	
	private static IPASootLoader singleton;
	
	public static IPASootLoader v(){
		if(singleton == null)
			singleton = new IPASootLoader();
		return singleton;
	}
	
	public static void reset(){
		singleton = null;
	}
	
	private String in;
	private IACMinerDataAccessor dataAccessor;
	private ILogger logger;

	private IPASootLoader() {
		super(2);
	}
	
	@SuppressWarnings("deprecation")
	public boolean load(IACMinerDataAccessor dataAccessor, Path pathToInput, int javaVersion, ILogger logger){
		this.in = FileHelpers.getNormAndAbsPath(pathToInput).toString();
		this.dataAccessor = dataAccessor;
		this.logger = logger;
		logger.info("{}: Initilizing Inter-Procedural Analysis Soot using '{}'.",in,cn);
		
		G.reset();
		G.v().out = new PrintStreamLoggerWrapper(logger,SingleLineFormatter.SOOTID);
		
		logger.info("{}: Retrieving the ExcludeList from the ExcludedElementsDatabase.",cn);
		List<String> excludeList = new ArrayList<>(dataAccessor.getExcludedElementsDB().getExcludedClasses());
		logger.info("{}: The exclude list has been retrieved. Resetting soot and starting {} initilization.",cn,cn);
		
		logger.info("{}: Setting Soot Options.",cn);
		setSootOptions(excludeList,javaVersion);
		logger.info("{}: Soot Options Set",cn);
		
		logger.info("{}: Resolving all required classes to required levels.",cn);
		if(!resolveClasses()){
			logger.fatal("{}: Failed to resolve all required classes to required levels.",cn);
			return false;
		}
		logger.info("{}: Successfully resolved all required classes to required levels.",cn);
		
		logger.info("{}: Resolving all required methods.",cn);
		if(!resolveMethods()){
			logger.fatal("{}: Failed to resolve all required methods.",cn);
			return false;
		}
		logger.info("{}: Successfully resolved all required methods.",cn);
		
		logger.info("{}: Excluding all but select methods in Binder.",cn);
		if(!handleBinderClass()){
			logger.fatal("{}: Failed to exclude all but select methods in Binder.",cn);
			return false;
		}
		logger.info("{}: Successfully excluded all but select methods in Binder.",cn);
			
		//Run only the callgraph pack because we don't need or use the others (they waste time and resources)
		logger.info("{}: Generating the callgraph.",cn);
		try{
			PackManager.v().getPack("cg").apply();
		}catch(Throwable t){
			logger.fatal("{}: Failed to generate the callgraph.",t,cn);
			return false;
		}
		logger.info("{}: Successfully generated the callgraph.",cn);
		
		//Note we do this before loading the call graph because I do not wish to change
		//the method body drastically after the call graph is built
		logger.info("{}: Running jb pack and fixing types.",cn);
		if(!runJBPackAndFixTypes(false, logger)) {
			logger.fatal("{}: Failed to run jb pack and fix types",cn);
			return false;
		}
		logger.info("{}: Successfully ran jb pack and fixed types",cn);
		
		//Add a nop as first statement of all methods (TODO: remove this when the bug is fixed in heros)
		//This effectively loads all method bodies which is not required but does speed things up later
		logger.info("{}: Adding a nop as the first statement for all methods.",cn);
		if(!addNopAsFirstStatment(logger)){
			logger.fatal("{}: Failed to add a nop as the first statment for all methods.",cn);
			return false;
		}
		logger.info("{}: Successfully added a nop as the first statement for all methods.",cn);
		
		logger.info("{}: Removing all constant casts from the code and replacing them with just the constants.",cn);
		if(!eleminateNonLocalCasts(logger)) {
			logger.fatal("{}: Failed to remove all constant casts from all methods.",cn);
			return false;
		}
		logger.info("{}: Successfully removed all constant casts from the code and replacing them with just the constants.",cn);
		
		logger.info("{}: Setting {} as initilized as the all other tasks require an initilized soot instance.",cn,cn);
		getSootInstanceWrapper().setSootInit(getSootLoadKey());
		
		//Reset and reload the soot specific excluded data because we have a new scene
		IExcludedElementsDatabase exdb = dataAccessor.getExcludedElementsDB();
		if(!(exdb instanceof EmptyExcludedElementsDatabase)){
			logger.info("{}: Reloading the Soot specific data of the ExcludedElementsDatabase.",cn);
			try{
				exdb.resetSootResolvedData();
				exdb.loadAllSootResolvedData();
			}catch(Throwable t){
				logger.fatal("{}: Failed to reload the Soot specific data of the ExcludedElementsDatabase.",t,cn);
				return false;
			}
			logger.info("{}: Successfully reloaded the Soot specific data of the ExcludedElementsDatabase.",cn);
		}else{
			logger.fatal("{}: ExcludedElementsDatabase is not set. How the hell did we get here?!?",cn);
			return false;
		}
		
		logger.info("{}: Inter-Procedural Analysis Soot Initilized",cn);
		return true;
		
	}
	
	protected void setSootOptions(List<String> excludeList, int javaVersion){
		Options.v().set_java_version(javaVersionConvert(javaVersion));
		Options.v().set_allow_phantom_refs(true);
		Options.v().set_allow_phantom_elms(true);
		
		Options.v().set_output_format(Options.output_format_jimple);
		Options.v().set_output_dir(dataAccessor.getConfig().getFilePath("debug_jimple-dump-dir").toString());
		
		//Locates the jimple files first by attempting file name is full class path + class name search.
		//Then splits the class path by '.' and treats these as directories and the 
		//final string in the split (i.e. the class name) as the file name and attempts
		//to find the jimple file this way.
		Options.v().set_permissive_resolving(true);
		
		//Sorts the locals into a stable order to ensure stable output across runs
		Options.v().setPhaseOption("jb","stabilize-local-names:true");
		
		Options.v().set_soot_classpath(in);
		//Appears to be required if we want all classes included (not just those directly referenced by our 
		//application classes i.e. ep and binder group classes)
		Options.v().set_process_dir(Arrays.asList(in));
		Options.v().set_prepend_classpath(false);
		Options.v().set_whole_program(true);
		
		Options.v().set_include_all(true);
		Options.v().set_exclude(excludeList);
		
		Options.v().set_no_bodies_for_excluded(true);
		
		//Attempt to fix the wrong static accesses that appear in dex and if the fix fails then just
		//used the incorrect jimple 
		Options.v().set_wrong_staticness(Options.wrong_staticness_fix);
		
		//When resolving a field reference to a field, if the field reference type does not match the resolved field type
		//ignore the situation (i.e. don't throw an exception)
		//The default used to just resolve the field using both the name and the type and if it could not find an exact
		//match for both, it would add the field as a phantom field (when those were enabled)
		//So this is set to ignore to prevent any exceptions from being thrown in code I cannot change
		Options.v().set_field_type_mismatches(Options.field_type_mismatches_ignore);
		
		//Unlinks the method source from the SootMethod class after they have been loaded to bodies
		//this helps to free up some memory
		Options.v().set_drop_bodies_after_load(true);
		
		//As the code we are analyzing is all technically dalvik code (even those loaded from class files)
		//we default to the dalvik throw analysis everywhere
		//Note dalvik throw analysis is just a extension of the unit throw analysis with some small changes
		//to account for changes in dalvik from the standard jvm
		Options.v().set_throw_analysis(Options.throw_analysis_dalvik);
		Options.v().set_check_init_throw_analysis(Options.check_init_throw_analysis_dalvik);
		
		//Instead of doing ExceptionalUnitGraph(body, UnitThrowAnalysis.v(), true) we set this option
		//which always sets the third argument to true when doing ExceptionUnitGraph(body)
		Options.v().set_omit_excepting_unit_edges(true);
		
		Options.v().set_src_prec(Options.src_prec_jimple);
		
		Options.v().setPhaseOption("cg.cha", "on");
		Options.v().setPhaseOption("cg","jdkver:"+javaVersion);
		//resolve invokes on abstract classes even when there are no children
		Options.v().setPhaseOption("cg","resolve-all-abstract-invokes:true");
	}
	
	protected boolean resolveClasses(){
		//Make sure that all classes in the binder groups database are at least resolved to signature
		//i.e. all Interface, Proxy, and Stub classes will exist and have their list of methods just no method bodies
		//this is so we can use the classes and methods as needed but still not look inside the bodies (i.e. they are excluded)
		if(dataAccessor.isBinderGroupsDatabaseSet()){
			logger.info("{}: Resolving all binder group classes to SIGNATURES.",cn);
			boolean success = true;
			for(String bgClassName : dataAccessor.getAllBinderGroupClasses()){
				try{
					SootClass c = Scene.v().forceResolve(bgClassName, SootClass.SIGNATURES);
					if(c == null){
						logger.fatal("{}: Failed to resolve binder group class {} to SIGNATURES.",cn,bgClassName);
						success = false;
					}else{
						c.setApplicationClass();
					}
				}catch(Throwable t){
					logger.fatal("{}: Failed to resolve binder group class {} to SIGNATURES.",t,cn,bgClassName);
					success = false;
				}
			}
			if(success){
				logger.info("{}: All binder group classes resolved to SIGNATURES successfully.",cn);
			}else{
				logger.fatal("{}: Failed to resolve some binder group classes to SIGNATURES.",cn);
				return false;
			}
		}else{
			logger.fatal("{}: The BinderGroupsDatabase is not initilized.",cn);
			return false;
		}
		
		//Resolve all entry point classes to BODIES as these are the starting points for our analysis
		if(dataAccessor.isEntryPointsDatabaseSet()){
			logger.info("{}: Resolving all entry point classes to BODIES.",cn);
			boolean success = true;
			for(String epClassName : dataAccessor.getAllEntryPointClasses()){
				try{
					SootClass c = Scene.v().forceResolve(epClassName, SootClass.BODIES);
					if(c == null || c.isPhantom()){
						logger.fatal("{}: Failed to resolve entry point class {} to BODIES.",cn,epClassName);
						success = false;
					}else{
						c.setApplicationClass();
					}
				}catch(Throwable t){
					logger.fatal("{}: Failed to resolve entry point class {} to BODIES.",t,cn,epClassName);
					success = false;
				}
			}
			if(success){
				logger.info("{}: All entry point classes resolved to BODIES successfully.",cn);
			}else{
				logger.fatal("{}: Failed to resolve some entry point classes to BODIES.",cn);
				return false;
			}
		}else{
			logger.fatal("{}: The EntryPointsDatabase is not initilized.",cn);
			return false;
		}
		
		//hack to force resolve callback classes for this entry point
		//TODO Double check if this is still needed
		/*if(epClasses.contains("com.android.printspooler.PrintSpoolerService$PrintSpooler")){
			try{
			Scene.v().forceResolve("com.android.printspooler.PrintSpoolerService", SootClass.BODIES);
			Scene.v().forceResolve("com.android.printspooler.PrintSpoolerService$HandlerCallerCallback", SootClass.BODIES);
			Scene.v().forceResolve("com.android.printspooler.PrintSpoolerService$PersistenceManager", SootClass.BODIES);
			}catch(Throwable t){}
		}*/
		Scene.v().loadNecessaryClasses();
		return true;
	}
	
	protected boolean resolveMethods(){
		//Resolve and load the bodies of all entry point methods then set the the soot entry points list all all resolved SootMethods
		if(dataAccessor.isEntryPointsDatabaseSet()){
			logger.info("{}: Resolving all entry point methods and initilizing their bodies.",cn);
			boolean success = true;
			Set<SootMethod> eps = new HashSet<>();
			
			//Double check there we have some entry point method signatures to resolve because if not this is pointless
			if(dataAccessor.getAllEntryPointMethods().isEmpty()){
				logger.fatal("{}: Failed to resolve any entry point methods or load their bodies "
						+ "because no entry point method signatures were provided.",cn);
				return false;
			}
			
			for(String epSig : dataAccessor.getAllEntryPointMethods()){
				try{
					SootMethod sm = Scene.v().getMethod(epSig);
					if(sm.retrieveActiveBody() == null){
						logger.fatal("{}: Failed to retrieve a body for entry point method {}.",cn,epSig);
					}else{
						eps.add(sm);
					}
				}catch(Throwable t){
					logger.fatal("{}: Failed to resolve entry point method {} or retrieve its body.",t,cn,epSig);
					success = false;
				}
			}
			if(success){
				logger.info("{}: Successfully resolved and initilized the bodies of all entry point methods.",cn);
				Scene.v().setEntryPoints(new ArrayList<SootMethod>(SortingMethods.sortSet(eps,SootSort.smComp)));
			}else{
				logger.fatal("{}: Failed to resolve or initilize the bodies of some entry point methods.",cn);
				return false;
			}
		}else{
			logger.fatal("{}: The EntryPointsDatabase is not initilized.",cn);
			return false;
		}
		return true;
	}
	
	protected boolean handleBinderClass() {
		try {
			SootClass binder = Scene.v().forceResolve(GenerateEntryPoints.binderFullClassName, SootClass.BODIES);
			Map<SootMethod,Pair<Set<SootMethod>,Set<Integer>>> baseOnTransact = 
					GenerateEntryPoints.parseBaseOnTransact(binder, logger);
			Set<SootMethod> binderToNotExclude = new HashSet<>();
			for(SootMethod sm : baseOnTransact.keySet()) {
				binderToNotExclude.add(sm);
				binderToNotExclude.addAll(baseOnTransact.get(sm).getFirst());
			}
			for(SootMethod sm : binder.getMethods()) {
				if(!binderToNotExclude.contains(sm)) {
					if(sm.isConcrete()) {
						sm.releaseActiveBody();
						sm.setSource(null);
						sm.setPhantom(true);
					}
				}
			}
			return true;
		} catch(Throwable t) {
			logger.fatal("{}: Unexpected exception when handeling the binder class.",t,cn);
			return false;
		}
	}

}
