package org.sag.acminer.phases.variedanalysis;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.common.io.PrintStreamUnixEOL;
import org.sag.common.logging.ILogger;
import org.sag.common.tools.SortingMethods;
import org.sag.main.phase.IPhaseHandler;
import org.sag.main.phase.IPhaseOption;
import org.sag.soot.SootSort;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

public class VariedAnalysis {
	
	private final IPhaseHandler handler;
	private final ILogger mainLogger;
	private final String cn;
	private final IACMinerDataAccessor dataAccessor;
	
	public VariedAnalysis(IPhaseHandler handler, IACMinerDataAccessor dataAccessor, ILogger mainLogger){
		this.handler = handler;
		this.mainLogger = mainLogger;
		this.cn = this.getClass().getSimpleName();
		this.dataAccessor = dataAccessor;
	}
	
	private boolean isOptionEnabled(String name) {
		IPhaseOption<?> o = handler.getPhaseOptionUnchecked(name);
		if(o == null || !o.isEnabled())
			return false;
		return true;
	}
	
	//Create path to output file that does not exist
	/*private final Path getOutputFilePath(Path rootOutDir, SootMethod m, String uniq, String ext) {
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
			output = FileHelpers.getPath(rootOutDir, sb2.toString());
			
			StringBuilder sb3 = new StringBuilder();
			sb3.append("_").append(uniq).append(ext);
			output = FileHelpers.getPath(sb3.insert(0, FileHelpers.trimFullFilePath(output.toString(), false, sb3.length())).toString());
		}catch(Throwable t){
			mainLogger.fatal("{}: Failed to construct the output file for output directory '{}' and method '{}'.",
					t,cn,rootOutDir,m);
			throw new IgnorableRuntimeException();
		}
		return output;
	}
	
	private final static int digits(int n) {
		int len = String.valueOf(n).length();
		if(n < 0)
			return len - 1;
		else
			return len;
	}
	
	private final static String padNum(int n, int digits) {
		return String.format("%"+digits+"d", n);
	}*/
	
	public boolean run() {
		boolean dumpnative = isOptionEnabled(VariedAnalysisHandler.optDumpnative);
		boolean successOuter = true;
		
		mainLogger.info("{}: Begin the special no call graph analysis.",cn);
		
		if(dumpnative) {
			Path dumpFile = dataAccessor.getConfig().getFilePath("debug_native-methods-dump-file");
			if(!dumpNativeMethods(dumpFile))
				successOuter = false;
		}
		
		if(successOuter)
			mainLogger.info("{}: Successfully completed the special no call graph analysis.",cn);
		else
			mainLogger.info("{}: Failed to complete one or more of the special no call graph analysis.",cn);
		return successOuter;
	}
	
	private boolean dumpNativeMethods(Path outputPath) {
		try {
			Set<SootMethod> nativeMethods = new HashSet<>();
			for(SootClass sc : Scene.v().getClasses()) {
				for(SootMethod sm : sc.getMethods()) {
					if(sm.isNative())
						nativeMethods.add(sm);
				}
			}
			nativeMethods = SortingMethods.sortSet(nativeMethods, SootSort.smComp);
			try(PrintStream ps = new PrintStreamUnixEOL(Files.newOutputStream(outputPath))) {
				for(SootMethod sm : nativeMethods)
					ps.println(sm);
			}
		} catch(Throwable t) {
			mainLogger.fatal("{}: Failed to dump native methods to file.",t,cn);
			return false;
		}
		return true;
	}
	
}
