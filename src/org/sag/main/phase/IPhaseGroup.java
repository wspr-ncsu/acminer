package org.sag.main.phase;

import java.nio.file.Path;
import java.util.Set;

import org.sag.common.logging.ILogger;
import org.sag.main.AndroidInfo;
import org.sag.main.IDataAccessor;

public interface IPhaseGroup {

	void init(IDataAccessor dataAccessor, AndroidInfo ai, ILogger logger);

	Set<Path> getRequiredInputFilePaths();

	IPhaseHandler getHandlerByClass(Class<?> c);

	IPhaseHandler getHandlerByName(String name);

	void setPhaseOptionForHandler(Class<?> c, String optionName, String optionValue);

	void setPhaseOptionForHandler(String handlerName, String optionName, String optionValue);

	boolean run();
	
	boolean isEnabled();
	
	void enable();
	
	String getName();
	
	void setQuickOption(String name);
	
/*	public static JimpleJarHandler getJimpleJarHandler() {
		String name = "JimpleJar";
		IPathSettings pathSettings = new IPathSettings() {
			public Path getRootDirectoryPath() {
				return PMinerFilePaths.v().getInputDir();
			}
			public List<Path> getOutputFilePaths() {
				return ImmutableList.of(
					PMinerFilePaths.v().getInput_SystemJimpleJarFile()
				);
			}
			public List<Path> getDependencyFilePaths() {
				List<Path> temp = getOtherPaths();
				Path systemClassJarFilePath = temp.get(temp.size()-3);
				Path systemImgZipFilePath = temp.get(temp.size()-2);
				Path androidInfoFilePath = temp.get(temp.size()-1);
				
				
				if(FileHelpers.checkRWFileExists(systemImgZipFilePath)) {
					return ImmutableList.of(systemImgZipFilePath, androidInfoFilePath);
				} else if(FileHelpers.checkRWFileExists(systemClassJarFilePath)) {
						return ImmutableList.of(systemClassJarFilePath);
				} else {
					throw new RuntimeException("Error: The path to the system class jar file '" + systemClassJarFilePath + "' and "
						+ "the path to the system image zip file '" + systemImgZipFilePath + "' do not exist. Cannot determine"
						+ " how to proceed.");
				}
			}
			public List<Path> getOtherPaths() {
				return ImmutableList.of(
					PMinerFilePaths.v().getInput_SystemArchivesZipFile(), 
					PMinerFilePaths.v().getInput_SystemJimpleFrameworksOnlyJarFile(), 
					PMinerFilePaths.v().getInput_SystemJimpleClassConflictsZipFile(),
					PMinerFilePaths.v().getOutput_WorkingDir(),
					PMinerFilePaths.v().getInput_SystemClassJarFile(),
					PMinerFilePaths.v().getInput_SystemImgZipFile(), 
					PMinerFilePaths.v().getInput_AndroidInfoFile()
				);
			}
		};
		
		return new JimpleJarHandler(name, null, pathSettings);
	}
	
	public static EntryPointsHandler getEntryPointsHandler(JimpleJarHandler jjHandler) {
		String name = "EntryPoints";
		IPathSettings pathSettings = new IPathSettings() {
			public Path getRootDirectoryPath() {
				return PMinerFilePaths.v().getInputDir();
			}
			public List<Path> getOutputFilePaths() {
				return ImmutableList.of(
					PMinerFilePaths.v().getInput_EntryPointsDBFile()
				);
			}
			public List<Path> getDependencyFilePaths() {
				return ImmutableList.of(
					jjHandler.getOutputFilePath()
				);
			}
			public List<Path> getOtherPaths() {
				return null;
			}
		};
		return new EntryPointsHandler(name, new IPhaseHandler[] {jjHandler}, pathSettings);
	}
	
	public static BinderGroupsHandler getBinderGroupsHandler(EntryPointsHandler epHandler) {
		String name = "BinderGroups";
		IPathSettings pathSettings = new IPathSettings() {
			public Path getRootDirectoryPath() {
				return PMinerFilePaths.v().getInputDir();
			}
			public List<Path> getOutputFilePaths() {
				return ImmutableList.of(
					PMinerFilePaths.v().getInput_BinderGroupsDBFile()
				);
			}
			public List<Path> getDependencyFilePaths() {
				return epHandler.getInAndOutPaths();
			}
			public List<Path> getOtherPaths() {
				return null;
			}
		};
		return new BinderGroupsHandler(name, new IPhaseHandler[] {epHandler}, pathSettings);
	}
	
	public static BinderServicesHandler getBinderServicesHandler(BinderGroupsHandler bgHandler) {
		String name = "BinderServices";
		IPathSettings pathSettings = new IPathSettings() {
			public Path getRootDirectoryPath() {
				return PMinerFilePaths.v().getInputDir();
			}
			public List<Path> getOutputFilePaths() {
				return ImmutableList.of(
					PMinerFilePaths.v().getInput_BinderServicesDBFile()
				);
			}
			public List<Path> getDependencyFilePaths() {
				return bgHandler.getInAndOutPaths();
			}
			public List<Path> getOtherPaths() {
				return null;
			}
		};
		return new BinderServicesHandler(name, new IPhaseHandler[] {bgHandler}, pathSettings);
	}
	
	public static VariedAnalysisHandler getVariedAnalysisHandler(JimpleJarHandler jjHandler) {
		String name = "VariedAnalysis";
		IPathSettings pathSettings = new IPathSettings() {
			public Path getRootDirectoryPath() {
				return PMinerFilePaths.v().getInputDir();
			}
			public List<Path> getOutputFilePaths() {
				return ImmutableList.of();
			}
			public List<Path> getDependencyFilePaths() {
				return jjHandler.getInAndOutPaths();
			}
			public List<Path> getOtherPaths() {
				return null;
			}
		};
		return new VariedAnalysisHandler(name, new IPhaseHandler[] {jjHandler}, pathSettings);
	}
	
	public static ExcludedElementsHandler getExcludedElementsHandler(BinderServicesHandler sbsHandler) {
		String name = "ExcludedElements";
		IPathSettings pathSettings = new IPathSettings() {
			public Path getRootDirectoryPath() {
				return PMinerFilePaths.v().getInputDir();
			}
			public List<Path> getOutputFilePaths() {
				return ImmutableList.of(
					PMinerFilePaths.v().getInput_ExcludedElementsDBFile()
				);
			}
			public List<Path> getDependencyFilePaths() {
				return sbsHandler.getInAndOutPaths(
					PMinerFilePaths.v().getInput_ExcludedElementsDBTXTFile()
				);
			}
			public List<Path> getOtherPaths() {
				return null;
			}
		};
		return new ExcludedElementsHandler(name, new IPhaseHandler[] {sbsHandler}, pathSettings);
	}
	
	public static CallGraphHandler getCallGraphHandler(ExcludedElementsHandler eeHandler) {
		String name = "CallGraph";
		IPathSettings pathSettings = new IPathSettings() {
			public Path getRootDirectoryPath() {
				return PMinerFilePaths.v().getInputDir();
			}
			public List<Path> getOutputFilePaths() {
				return ImmutableList.of();
			}
			public List<Path> getDependencyFilePaths() {
				return eeHandler.getInAndOutPaths();
			}
			public List<Path> getOtherPaths() {
				return null;
			}
		};
		return new CallGraphHandler(name, new IPhaseHandler[] {eeHandler}, pathSettings);
	}
	
	public static VariedCallGraphAnalysisHandler getVariedCallGraphAnalysisHandler(CallGraphHandler cgHandler) {
		String name = "VariedCallGraphAnalysis";
		IPathSettings pathSettings = new IPathSettings() {
			public Path getRootDirectoryPath() {
				return PMinerFilePaths.v().getInputDir();
			}
			public List<Path> getOutputFilePaths() {
				return ImmutableList.of();
			}
			public List<Path> getDependencyFilePaths() {
				return cgHandler.getInAndOutPaths();
			}
			public List<Path> getOtherPaths() {
				return null;
			}
		};
		return new VariedCallGraphAnalysisHandler(name, new IPhaseHandler[] {cgHandler}, pathSettings);
	}
	
	public static AccessControlMarkerHandler getAccessControlMarkerHandler(CallGraphHandler cgHandler) {
		String name = "AccessControlMarker";
		IPathSettings pathSettings = new IPathSettings() {
			public Path getRootDirectoryPath() {
				return PMinerFilePaths.v().getInputDir();
			}
			public List<Path> getOutputFilePaths() {
				return ImmutableList.of(
					PMinerFilePaths.v().getInput_ContextQueriesDBFile(), 
					PMinerFilePaths.v().getInput_ThrowSEDBFile(),
					PMinerFilePaths.v().getInput_EntryPointEdgessDBFile()
				);
			}
			public List<Path> getDependencyFilePaths() {
				return cgHandler.getInAndOutPaths(
					PMinerFilePaths.v().getInput_ContextQueriesDescriptorDBFile()
				);
			}
			public List<Path> getOtherPaths() {
				return null;
			}
		};
		return new AccessControlMarkerHandler(name, new IPhaseHandler[] {cgHandler}, pathSettings);
	}
	
	public static ControlPredicateMarkerHandler getControlPredicateMarkerHandler(AccessControlMarkerHandler smHandler) {
		String name = "ControlPredicateMarker";
		IPathSettings pathSettings = new IPathSettings() {
			public Path getRootDirectoryPath() {
				return PMinerFilePaths.v().getInputDir();
			}
			public List<Path> getOutputFilePaths() {
				return ImmutableList.of(
					PMinerFilePaths.v().getInput_ControlPredicatesDBFile()
				);
			}
			public List<Path> getDependencyFilePaths() {
				return smHandler.getInAndOutPaths();
			}
			public List<Path> getOtherPaths() {
				return null;
			}
		};
		return new ControlPredicateMarkerHandler(name, new IPhaseHandler[] {smHandler}, pathSettings);
	}
	
	public static DefUseGraphHandler getDefUseGraphHandler(ControlPredicateMarkerHandler cpmHandler) {
		String name = "DefUseGraph";
		IPathSettings pathSettings = new IPathSettings() {
			public Path getRootDirectoryPath() {
				return PMinerFilePaths.v().getInputDir();
			}
			public List<Path> getOutputFilePaths() {
				return ImmutableList.of(
					PMinerFilePaths.v().getInput_DefUseGraphDBFile()
				);
			}
			public List<Path> getDependencyFilePaths() {
				return cpmHandler.getInAndOutPaths();
			}
			public List<Path> getOtherPaths() {
				return null;
			}
		};
		return new DefUseGraphHandler(name, new IPhaseHandler[] {cpmHandler}, pathSettings);
	}
	
	public static DefUseGraphDumpBeforeFilterHandler getDefUseGraphDumpBeforeFilterHandler(DefUseGraphHandler dugHandler) {
		String name = "DefUseGraphDumpBeforeFilter";
		IPathSettings pathSettings = new IPathSettings() {
			public Path getRootDirectoryPath() {
				return PMinerFilePaths.v().getInputDir();
			}
			public List<Path> getOutputFilePaths() {
				return ImmutableList.of(
					FileHelpers.getPath(PMinerFilePaths.v().getInput_DefUseGraphBeforeFilterDir(), "_file_hash_list_db.xml")
				);
			}
			public List<Path> getDependencyFilePaths() {
				return dugHandler.getInAndOutPaths();
			}
			public List<Path> getOtherPaths() {
				return null;
			}
		};
		return new DefUseGraphDumpBeforeFilterHandler(name, new IPhaseHandler[] {dugHandler}, pathSettings);
	}
	
	public static ControlPredicateFilterHandler getControlPredicateFilterHandler(ControlPredicateMarkerHandler cpmHandler, 
			DefUseGraphHandler dugHandler) {
		String name = "ControlPredicateFilter";
		IPathSettings pathSettings = new IPathSettings() {
			public Path getRootDirectoryPath() {
				return PMinerFilePaths.v().getInputDir();
			}
			public List<Path> getOutputFilePaths() {
				return ImmutableList.of(
					PMinerFilePaths.v().getInput_ControlPredicatesFilteredDBFile()
				);
			}
			public List<Path> getDependencyFilePaths() {
				return dugHandler.getInAndOutPaths(
					PMinerFilePaths.v().getInput_ControlPredicateFilterDBFile()
				);
			}
			public List<Path> getOtherPaths() {
				return ImmutableList.of(
					cpmHandler.getOutputFilePath()
				);
			}
		};
		return new ControlPredicateFilterHandler(name, new IPhaseHandler[] {dugHandler}, pathSettings);
	}
	
	public static DefUseGraphDumpAfterFilterHandler getDefUseGraphDumpAfterFilterHandler(ControlPredicateFilterHandler cpfHandler) {
		String name = "DefUseGraphDumpAfterFilter";
		IPathSettings pathSettings = new IPathSettings() {
			public Path getRootDirectoryPath() {
				return PMinerFilePaths.v().getInputDir();
			}
			public List<Path> getOutputFilePaths() {
				return ImmutableList.of(
					FileHelpers.getPath(PMinerFilePaths.v().getInput_DefUseGraphAfterFilterDir(), "_file_hash_list_db.xml")
				);
			}
			public List<Path> getDependencyFilePaths() {
				return cpfHandler.getInAndOutPaths();
			}
			public List<Path> getOtherPaths() {
				return null;
			}
		};
		return new DefUseGraphDumpAfterFilterHandler(name, new IPhaseHandler[] {cpfHandler}, pathSettings);
	}
	
	public static DefUseGraphModHandler getDefUseGraphModHandler(ControlPredicateFilterHandler cpfHandler) {
		String name = "DefUseGraphMod";
		IPathSettings pathSettings = new IPathSettings() {
			public Path getRootDirectoryPath() {
				return PMinerFilePaths.v().getInputDir();
			}
			public List<Path> getOutputFilePaths() {
				return ImmutableList.of(
					PMinerFilePaths.v().getInput_DefUseGraphModDBFile()
				);
			}
			public List<Path> getDependencyFilePaths() {
				return cpfHandler.getInAndOutPaths();
			}
			public List<Path> getOtherPaths() {
				return null;
			}
		};
		return new DefUseGraphModHandler(name, new IPhaseHandler[] {cpfHandler}, pathSettings);
	}
	
	public static ACMinerDebugHandler getACMinerDebugHandler(CallGraphHandler cgHandler) {
		String name = "ACMinerDebug";
		IPathSettings pathSettings = new IPathSettings() {
			public Path getRootDirectoryPath() {
				return PMinerFilePaths.v().getInputDir();
			}
			public List<Path> getOutputFilePaths() {
				return ImmutableList.of();
			}
			public List<Path> getDependencyFilePaths() {
				return cgHandler.getInAndOutPaths();
			}
			public List<Path> getOtherPaths() {
				return null;
			}
		};
		return new ACMinerDebugHandler(name, new IPhaseHandler[] {cgHandler}, pathSettings);
	}
	
	public static ACMinerHandler getACMinerHandler(DefUseGraphModHandler dugmHandler) {
		String name = "ACMiner";
		IPathSettings pathSettings = new IPathSettings() {
			public Path getRootDirectoryPath() {
				return PMinerFilePaths.v().getInputDir();
			}
			public List<Path> getOutputFilePaths() {
				return ImmutableList.of(
					PMinerFilePaths.v().getInput_ACMinerDBFile()
				);
			}
			public List<Path> getDependencyFilePaths() {
				return dugmHandler.getInAndOutPaths();
			}
			public List<Path> getOtherPaths() {
				return null;
			}
		};
		return new ACMinerHandler(name, new IPhaseHandler[] {dugmHandler}, pathSettings);
	}
	
	
	public static AndroidAPIHandler getAndroidAPIHandler(JimpleJarHandler jjHandler) {
		String name = "AndroidAPI";
		IPathSettings pathSettings = new IPathSettings() {
			public Path getRootDirectoryPath() {
				return PMinerFilePaths.v().getInputDir();
			}
			public List<Path> getOutputFilePaths() {
				return ImmutableList.of(
					PMinerFilePaths.v().getInput_AndroidAPIDBFile()
				);
			}
			public List<Path> getDependencyFilePaths() {
				return ImmutableList.of(
					jjHandler.getOutputFilePath(),
					PMinerFilePaths.v().getInput_Woof_AndroidAPIJarFile()
				);
			}
			public List<Path> getOtherPaths() {
				return null;
			}
		};
		return new AndroidAPIHandler(name, new IPhaseHandler[] {jjHandler}, pathSettings);
	}
	
	public static FileMethodsExcludedElementsHandler getFileMethodsExcludedElementsHandler(BinderServicesHandler bsHandler, MessageHandlerHandler mhHandler, 
			AndroidAPIHandler apiHandler) {
		String name = "FileMethodsExcludedElements";
		IPathSettings pathSettings = new IPathSettings() {
			public Path getRootDirectoryPath() {
				return PMinerFilePaths.v().getInputDir();
			}
			public List<Path> getOutputFilePaths() {
				return ImmutableList.of(
					PMinerFilePaths.v().getInput_Woof_FileMethodsExcludedElementsDBFile()
				);
			}
			public List<Path> getDependencyFilePaths() {
				List<Path> ret = new ArrayList<>();
				ret.addAll(apiHandler.getInAndOutPaths());
				List<Path> others = bsHandler.getInAndOutPaths();
				for(Path p : others) {
					if(!ret.contains(p))
						ret.add(p);
				}
				others = mhHandler.getInAndOutPaths();
				for(Path p : others) {
					if(!ret.contains(p))
						ret.add(p);
				}
				ret.add(PMinerFilePaths.v().getInput_Woof_FileMethodsExcludedElementsFile());
				return ImmutableList.copyOf(ret);
			}
			public List<Path> getOtherPaths() {
				return null;
			}
		};
		return new FileMethodsExcludedElementsHandler(name, new IPhaseHandler[] {apiHandler, bsHandler, mhHandler}, pathSettings);
	}

	public static FileMethodsCallGraphHandler getFileMethodsCallGraphHandler(FileMethodsExcludedElementsHandler fmeeHandler) {
		String name = "FileMethodsCallGraph";
		IPathSettings pathSettings = new IPathSettings() {
			public Path getRootDirectoryPath() {
				return PMinerFilePaths.v().getInputDir();
			}
			public List<Path> getOutputFilePaths() {
				return ImmutableList.of();
			}
			public List<Path> getDependencyFilePaths() {
				return fmeeHandler.getInAndOutPaths();
			}
			public List<Path> getOtherPaths() {
				return null;
			}
		};
		return new FileMethodsCallGraphHandler(name, new IPhaseHandler[] {fmeeHandler}, pathSettings);
	}
	
	public static FileMethodsHandler getFileMethodsHandler(FileMethodsExcludedElementsHandler fmeeHandler) {
		String name = "FileMethods";
		IPathSettings pathSettings = new IPathSettings() {
			public Path getRootDirectoryPath() {
				return PMinerFilePaths.v().getInputDir();
			}
			public List<Path> getOutputFilePaths() {
				return ImmutableList.of(
					PMinerFilePaths.v().getInput_Woof_FileMethodsDBFile(),
					PMinerFilePaths.v().getInput_Woof_FileMethodsAndroidAPIFile(),
					PMinerFilePaths.v().getInput_Woof_FileMethodsJavaAPIFile()
				);
			}
			public List<Path> getDependencyFilePaths() {
				return fmeeHandler.getInAndOutPaths(
					PMinerFilePaths.v().getInput_Woof_FileMethodsJavaAPIIndicatorFile(),
					PMinerFilePaths.v().getInput_Woof_FileMethodsNativeFile(),
					PMinerFilePaths.v().getInput_Woof_FileMethodsJavaAPIFile(),
					PMinerFilePaths.v().getInput_Woof_FileMethodsAndroidAPIFile()
				);
			}
			public List<Path> getOtherPaths() {
				return ImmutableList.of(PMinerFilePaths.v().getInput_Woof_Debug_FileMethodsDir());
			}
		};
		return new FileMethodsHandler(name, new IPhaseHandler[] {fmeeHandler}, pathSettings);
	}
	
	public static FileActionsExcludedElementsHandler getFileActionsExcludedElementsHandler(BinderServicesHandler bsHandler, MessageHandlerHandler mhHandler,
			AndroidAPIHandler apiHandler) {
		String name = "FileActionsExcludedElements";
		IPathSettings pathSettings = new IPathSettings() {
			public Path getRootDirectoryPath() {
				return PMinerFilePaths.v().getInputDir();
			}
			public List<Path> getOutputFilePaths() {
				return ImmutableList.of(
					PMinerFilePaths.v().getInput_Woof_FileActionsExcludedElementsDBFile()
				);
			}
			public List<Path> getDependencyFilePaths() {
				List<Path> ret = new ArrayList<>();
				ret.addAll(apiHandler.getInAndOutPaths());
				List<Path> others = bsHandler.getInAndOutPaths();
				for(Path p : others) {
					if(!ret.contains(p))
						ret.add(p);
				}
				others = mhHandler.getInAndOutPaths();
				for(Path p : others) {
					if(!ret.contains(p))
						ret.add(p);
				}
				ret.add(PMinerFilePaths.v().getInput_Woof_FileActionsExcludedElementsFile());
				return ImmutableList.copyOf(ret);
			}
			public List<Path> getOtherPaths() {
				return null;
			}
		};
		return new FileActionsExcludedElementsHandler(name, new IPhaseHandler[] {apiHandler, bsHandler, mhHandler}, pathSettings);
	}
	
	public static FileActionsCallGraphHandler getFileActionsCallGraphHandler(FileActionsExcludedElementsHandler faeeHandler) {
		String name = "FileActionsCallGraph";
		IPathSettings pathSettings = new IPathSettings() {
			public Path getRootDirectoryPath() {
				return PMinerFilePaths.v().getInputDir();
			}
			public List<Path> getOutputFilePaths() {
				return ImmutableList.of();
			}
			public List<Path> getDependencyFilePaths() {
				return faeeHandler.getInAndOutPaths();
			}
			public List<Path> getOtherPaths() {
				return null;
			}
		};
		return new FileActionsCallGraphHandler(name, new IPhaseHandler[] {faeeHandler}, pathSettings);
	}
	
	public static FileActionsHandler getFileMethodsHandler(FileMethodsHandler fmHandler, FileActionsCallGraphHandler facgHandler) {
		String name = "FileActions";
		IPathSettings pathSettings = new IPathSettings() {
			public Path getRootDirectoryPath() {
				return PMinerFilePaths.v().getInputDir();
			}
			public List<Path> getOutputFilePaths() {
				return ImmutableList.of(
					PMinerFilePaths.v().getInput_Woof_FileActionsDBFile()
				);
			}
			public List<Path> getDependencyFilePaths() {
				List<Path> ret = new ArrayList<>();
				ret.addAll(facgHandler.getInAndOutPaths());
				List<Path> others = fmHandler.getInAndOutPaths();
				for(Path p : others) {
					if(!ret.contains(p))
						ret.add(p);
				}
				return ImmutableList.copyOf(ret);
			}
			public List<Path> getOtherPaths() {
				return null;
			}
		};
		return new FileActionsHandler(name, new IPhaseHandler[] {fmHandler, facgHandler}, pathSettings);
	}
	
	public static MessageHandlerHandler getMessageHandlerHandler(JimpleJarHandler jjHandler) {
		String name = "MessageHandler";
		IPathSettings pathSettings = new IPathSettings() {
			public Path getRootDirectoryPath() {
				return PMinerFilePaths.v().getInputDir();
			}
			public List<Path> getOutputFilePaths() {
				return ImmutableList.of(
					PMinerFilePaths.v().getInput_Woof_MessageHandlerDBFile()
				);
			}
			public List<Path> getDependencyFilePaths() {
				return ImmutableList.of(
					jjHandler.getOutputFilePath()
				);
			}
			public List<Path> getOtherPaths() {
				return null;
			}
		};
		return new MessageHandlerHandler(name, new IPhaseHandler[] {jjHandler}, pathSettings);
	}*/
	

}