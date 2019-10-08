package org.sag.sootinit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sag.common.io.FileHelpers;
import org.sag.common.logging.ILogger;
import org.sag.common.tools.SortingMethods;
import org.sag.main.logging.PrintStreamLoggerWrapper;
import org.sag.main.logging.SingleLineFormatter;
import org.sag.woof.IWoofDataAccessor;
import org.sag.soot.SootSort;
import org.sag.soot.xstream.SootMethodContainer;
import org.sag.sootinit.SootInstanceWrapper.SootLoadKey;

import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.options.Options;

public class APISootLoader extends SootLoader {
	
	private static APISootLoader singleton;
	
	public static APISootLoader v() {
		if(singleton == null)
			singleton = new APISootLoader();
		return singleton;
	}
	
	public static void reset() {
		singleton = null;
	}
	
	private String in;
	private IWoofDataAccessor dataAccessor;
	private ILogger logger;

	private APISootLoader() {
		super(SootLoadKey.API);
	}
	
	@SuppressWarnings("deprecation")
	public boolean load(IWoofDataAccessor dataAccessor, Path pathToInput, int javaVersion, ILogger logger) {
		this.in = FileHelpers.getNormAndAbsPath(pathToInput).toString();
		this.dataAccessor = dataAccessor;
		this.logger = logger;
		logger.info("{}: Initilizing API Analysis Soot using '{}'.",in,cn);
		
		G.reset();
		G.v().out = new PrintStreamLoggerWrapper(logger,SingleLineFormatter.SOOTID);
		
		logger.info("{}: Retrieving the ExcludeList from the ExcludedElementsDatabase.",cn);
		List<String> excludeList = new ArrayList<>(dataAccessor.getExcludedElementsDB().getExcludedClasses());
		logger.info("{}: The exclude list has been retrieved. Resetting soot and starting {} initilization.",cn,cn);
		
		logger.info("{}: Setting Soot Options.",cn);
		setSootOptions(excludeList, javaVersion);
		logger.info("{}: Soot Options Set",cn);
		
		logger.info("{}: Loading required classes API.",cn);
		if(!loadClasses()) {
			logger.fatal("{}: Failed to load all required API classes.",cn);
			return false;
		}
		logger.info("{}: Successfully loaded all required API classes.",cn);
		
		logger.info("{}: Resolving required API methods.",cn);
		if(!resolveMethods()) {
			logger.fatal("{}: Failed to resolve all required API methods.",cn);
			return false;
		}
		logger.info("{}: Successfully resolved all required API methods.",cn);
			
		//Run only the callgraph pack because we don't need or use the others (they waste time and resources)
		logger.info("{}: Generating the callgraph.",cn);
		try {
			PackManager.v().getPack("cg").apply();
		} catch(Throwable t) {
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
		if(!addNopAsFirstStatment(logger)) {
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
		getSootInstanceWrapper().setSootInitValue(getSootLoadKey());
		
		logger.info("{}: API Analysis Soot Initilized",cn);
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
	
	protected boolean loadClasses() {
		boolean success = true;
		try {
			Set<SootMethodContainer> apiMethods = dataAccessor.getAndroidAPIDB().getOutputData();
			Set<String> excludedMethods = dataAccessor.getExcludedElementsDB().getExcludedMethods();
			
			if(apiMethods == null || apiMethods.isEmpty()) {
				logger.fatal("{}: No API classes to load.",cn);
				success = false;
			} else {
				Map<String,SootClass> resolved = new HashMap<>();
				for(SootMethodContainer sm : apiMethods) {
					if(!excludedMethods.contains(sm.getSignature()) && !resolved.containsKey(sm.getDeclaringClass())) {
						try {
							SootClass sc = Scene.v().forceResolve(sm.getDeclaringClass(), SootClass.BODIES);
							if(sc == null || sc.isPhantom()) {
								logger.fatal("{}: Failed to load API class {}.",cn,sm.getDeclaringClass());
								success = false;
							} else {
								sc.setApplicationClass();
								resolved.put(sm.getDeclaringClass(), sc);
							}
						} catch(Throwable t) {
							logger.fatal("{}: Unexpected error when loading API class {}.",t,cn,sm.getDeclaringClass());
							success = false;
						}
					}
				}
				
				if(success)
					Scene.v().loadNecessaryClasses();
			}
		} catch(Throwable t) {
			logger.fatal("{}: Unexpected error when loading API classes.",t,cn);
			success = false;
		}
		return success;
	}
	
	protected boolean resolveMethods() {
		boolean success = true;
		try {
			Set<SootMethodContainer> apiMethods = dataAccessor.getAndroidAPIDB().getOutputData();
			Set<String> excludedMethods = dataAccessor.getExcludedElementsDB().getExcludedMethods();
			
			if(apiMethods == null || apiMethods.isEmpty()) {
				logger.fatal("{}: No API methods to resolve.",cn);
				success = false;
			} else {
				Set<SootMethod> eps = new HashSet<>();
				for(SootMethodContainer smContainer : apiMethods) {
					if(!excludedMethods.contains(smContainer.getSignature())) {
						try {
							SootMethod sm = smContainer.toSootMethodUnsafe();
							if(sm == null) {
								logger.fatal("{}: Failed to resolve API method {}.",cn,smContainer.getSignature());
								success = false;
							} else if(sm.retrieveActiveBody() == null) {
								logger.fatal("{}: Failed to retrieve a body for API method {}.",cn,smContainer.getSignature());
								success = false;
							} else {
								eps.add(sm);
							}
						} catch(Throwable t) {
							logger.fatal("{}: Failed to resolve API method {}.",t,cn,smContainer.getSignature());
							success = false;
						}
					}
				}
				
				if(success)
					Scene.v().setEntryPoints(new ArrayList<SootMethod>(SortingMethods.sortSet(eps,SootSort.smComp)));
			}
		} catch(Throwable t) {
			logger.fatal("{}: Unexpected error when resolving API methods.",t,cn);
			success = false;
		}
		
		return success;
	}

}
