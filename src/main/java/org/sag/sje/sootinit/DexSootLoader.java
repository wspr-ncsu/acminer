package org.sag.sje.sootinit;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.sag.common.logging.ILogger;
import org.sag.main.logging.PrintStreamLoggerWrapper;
import org.sag.main.logging.SingleLineFormatter;
import org.sag.main.sootinit.SootLoader;

import soot.G;
import soot.Scene;
import soot.options.Options;

public class DexSootLoader extends SootLoader {
	
private static DexSootLoader singleton;
	
	public static DexSootLoader v(){
		if(singleton == null)
			singleton = new DexSootLoader();
		return singleton;
	}
	
	public static void reset(){
		singleton = null;
	}
	
	private DexSootLoader(){
		this(5);
	}
	
	protected DexSootLoader(int key) {
		super(key);
	}
	
	public final boolean load(String archivePath, Set<String> classesToLoad, int apiVersion, int javaVersion, ILogger logger){
		logger.info("DexSootLoader: Initilizing soot for using archive containing dex at '{}'.",archivePath);
		boolean ret = load(archivePath,Collections.singletonList(archivePath),classesToLoad,false,apiVersion,javaVersion,logger);
		getSootInstanceWrapper().setSootInit(getSootLoadKey());
		logger.info("DexSootLoader: Soot had been initilized successfully using the archive containing dex at '{}'.",archivePath);
		return ret;
	}

	@SuppressWarnings("deprecation")
	protected final boolean load(String classpath, List<String> ins, Set<String> classesToLoad, boolean useJimple, int apiVersion, 
			int javaVersion, ILogger logger){
		G.reset();
		G.v().out = new PrintStreamLoggerWrapper(logger,SingleLineFormatter.SOOTID);
		
		logger.info("DexSootLoader: Setting Soot options.");
		Options.v().set_java_version(javaVersionConvert(javaVersion));
		Options.v().set_allow_phantom_refs(true);
		Options.v().set_allow_phantom_elms(true);
		Options.v().set_prepend_classpath(false);
		//Sorts the locals into a stable order to ensure stable output across runs
		Options.v().setPhaseOption("jb","stabilize-local-names:true");
		//Locates the jimple files first by attempting file name is full class path + class name search.
		//Then splits the class path by '.' and treats these as directories and the 
		//final string in the split (i.e. the class name) as the file name and attempts
		//to find the jimple file this way.
		if(useJimple)
			Options.v().set_permissive_resolving(true);
		Options.v().set_android_api_version(apiVersion);
		Options.v().set_soot_classpath(classpath);
		Options.v().set_process_dir(ins);
		Options.v().classes().addAll(classesToLoad);
		Options.v().set_whole_program(true);
		Options.v().set_include_all(true);
		Options.v().set_src_prec(Options.src_prec_apk);
		Options.v().set_process_multiple_dex(true);
		Options.v().set_search_dex_in_archives(true);
		
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
		
		logger.info("DexSootLoader: Soot Options set. Loading classes.");
		Scene.v().loadNecessaryClasses();
		
		logger.info("{}: Running jb pack and fixing types.",cn);
		if(!runJBPackAndFixTypes(true, logger)) {
			logger.fatal("{}: Failed to run jb pack and fix types",cn);
			return false;
		}
		logger.info("{}: Successfully ran jb pack and fixed types",cn);
		
		return true;
	}
	
}
