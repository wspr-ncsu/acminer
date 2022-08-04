package org.sag.main.config;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;

import org.sag.common.io.FileHelpers;
import org.sag.common.logging.ILogger;
import org.sag.main.IDataAccessor;
import org.sag.main.config.PhaseConfig.*;
import org.sag.main.phase.IPhaseGroup;
import org.sag.main.phase.IPhaseOption;
import org.sag.main.phase.IQuickOption;
import org.sag.main.phase.PhaseGroup;
import org.sag.main.phase.PhaseManager;
import org.sag.main.phase.IPhaseOption.*;
import org.sag.main.phase.IQuickOption.*;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ScanResult;

public final class Config {
	
	private static final String cn = Config.class.getSimpleName();
	
	private final Map<String,List<String>> keyToPathSegments;
	private final Map<String,List<String>> keyToPathOverrides;
	private final String date;
	private final Class<?> daClass;
	private final Map<String,PhaseConfig> nameToPhaseConfig;
	private final Map<String,PhaseGroupConfig> nameToPhaseGroupConfig;
	
	private Config(Map<String,List<String>> keyToPathSegments, Class<?> daClass, Map<String,PhaseConfig> nameToPhaseConfig, 
			Map<String,PhaseGroupConfig> nameToPhaseGroupConfig) {
		this.keyToPathSegments = keyToPathSegments;
		this.keyToPathOverrides = new HashMap<>();
		this.date = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
		this.daClass = daClass;
		this.nameToPhaseConfig = nameToPhaseConfig;
		this.nameToPhaseGroupConfig = nameToPhaseGroupConfig;
		for(PhaseConfig pc : this.nameToPhaseConfig.values()) {
			pc.setConfig(this);
		}
		for(PhaseGroupConfig pc : this.nameToPhaseGroupConfig.values()) {
			pc.setConfig(this);
		}
	}
	
	/*public static void main(String[] args) throws Exception {
		Config g = getConfigFromResources(new LoggerWrapperSLF4J(Config.class));
		g.setFilePathEntry("work-dir", "C:\\CS\\Documents\\Work\\Research\\woof\\aosp-9.0.0\\input");
		for(String key : g.keyToPathSegments.keySet()) {
			System.out.println(g.getFilePath(key));
		}
		IDataAccessor da = g.getNewDataAccessor();
		PhaseManager pm = g.getNewPhaseManager();
	}*/
	
	public IDataAccessor getNewDataAccessor() {
		try {
			return (IDataAccessor) daClass.getConstructor(new Class[] {Config.class}).newInstance(this);
		} catch(Throwable t) {
			throw new RuntimeException("Error: Failed to get new instance of '" + daClass + "'",t);
		}
	}
	
	public PhaseManager getNewPhaseManager() {
		List<IPhaseGroup> ret = new ArrayList<>();
		for(PhaseGroupConfig pc : nameToPhaseGroupConfig.values()) {
			ret.add(new PhaseGroup(pc));
		}
		return new PhaseManager(ImmutableList.copyOf(ret));
	}
	
	public PhaseConfig getPhaseConfig(String name) {
		Objects.requireNonNull(name);
		PhaseConfig ret = nameToPhaseConfig.get(name);
		if(ret == null)
			throw new RuntimeException("Error: No phase with the name '" + name + "' exists.");
		return ret;
	}
	
	public void setFilePathEntry(String key, String... values) {
		Objects.requireNonNull(key);
		Objects.requireNonNull(values);
		if(values.length == 0)
			throw new RuntimeException("Error: Must provide new values for entry.");
		if(values.length > 1) {
			List<String> existing = lookupFilePathValues(values[0]);
			if(existing == null || existing.isEmpty())
				throw new RuntimeException("Error: Tried to add entry referencing non existant key '" + values[0] + "'.");
		}
		keyToPathOverrides.put(key, Arrays.asList(values));
	}

	public Path getFilePath(String key) {
		return getFilePath(key, false);
	}
	
	public Path getFilePath(String key, boolean forHelpDiag) {
		Objects.requireNonNull(key);
		String k = key;
		String first = null;
		ArrayDeque<String> others = new ArrayDeque<>();
		while(k != null) {
			List<String> parts = lookupFilePathValues(k);
			if(parts == null || parts.isEmpty())
				throw new RuntimeException("Error: No path parts entry for key '" + k + "' in the config files.");
			if(parts.size() == 1) {
				first = parts.get(0);
				if(first == null || first.isEmpty())
					throw new RuntimeException("Error: Cannot have a empty path part for key '" + k + "'.");
				if(first.equals("date-time-dir"))
					first = date;
				k = null;
			} else {
				for(int i = parts.size() - 1; i >= 0 ; i--) {
					if(i == 0) {
						k = parts.get(0);
					} else {
						String temp =  parts.get(i);
						if(temp == null || temp.isEmpty())
							throw new RuntimeException("Error: Cannot have a empty path part for key '" + k + "'.");
						if(temp.equals("date-time-dir"))
							temp = date;
						others.addFirst(temp);
					}
				}
			}
		}

		// Don't include the root directory for the help dialog
		if(forHelpDiag) {
			if(others.isEmpty())
				return FileHelpers.getSimplePath(first);
			else
				return FileHelpers.getSimplePath(first, others.toArray(new String[0]));
		} else {
			if(others.isEmpty())
				return FileHelpers.getPath(first);
			else
				return FileHelpers.getPath(first,others.toArray(new String[0]));
		}
	}
	
	private List<String> lookupFilePathValues(String key) {
		List<String> parts = keyToPathOverrides.get(key);
		if(parts == null || parts.isEmpty())
			parts = keyToPathSegments.get(key);
		return parts;
	}
	
	public static final Config getConfigFromResources(ILogger logger) {
		Map<String,List<String>> keyToPathSegments = new LinkedHashMap<>();
		List<Class<?>> dataAccessorHierarchy = new ArrayList<>();
		Map<String,PhaseConfig> nameToPhaseConfig = new LinkedHashMap<>();
		Map<String,PhaseGroupConfig> nameToPhaseGroupConfig = new LinkedHashMap<>();
		if(!getFilePathsFromAllConfigs(keyToPathSegments, logger))
			throw new RuntimeException("Error: Failed to load config from resources.");
		if(!getPhasesFromResources(nameToPhaseConfig, logger))
			throw new RuntimeException("Error: Failed to load config from resources.");
		if(!getPhaseGroupsFromResources(dataAccessorHierarchy, nameToPhaseGroupConfig, logger))
			throw new RuntimeException("Error: Failed to load config from resources.");
		return new Config(ImmutableMap.copyOf(keyToPathSegments), dataAccessorHierarchy.get(dataAccessorHierarchy.size()-1), 
				ImmutableMap.copyOf(nameToPhaseConfig), ImmutableMap.copyOf(nameToPhaseGroupConfig));
	}
	
	private static final boolean getPhasesFromResources(Map<String,PhaseConfig> nameToPhaseConfig, ILogger logger) {
		try (ScanResult scanResult = new ClassGraph().whitelistPaths("config/phases").scan()) {
			for(Resource res : scanResult.getAllResources()) {
				if(!getPhaseFromConfig(res, nameToPhaseConfig, logger))
					return false;
			}
			return true;
		} catch(Throwable t) {
			logger.fatal("{}: Something went wrong when scanning for phase config files.",cn);
			return false;
		}
	}
	
	private static final boolean getPhaseFromConfig(Resource res, Map<String,PhaseConfig> nameToPhaseConfig, ILogger logger) {
		try(BufferedReader br = new BufferedReader(new InputStreamReader(res.open()))) {
			Object o = new Load(LoadSettings.builder().build()).loadFromReader(br);
			if(o == null) {
				return true;
			} else if(!(o instanceof List)) {
				logger.fatal("{}: Expected List structure but got '{}' for file '{}'.",cn,o.getClass().getSimpleName(),res.getPath());
				return false;
			}
			
			for(Object entry : (List<?>)o) {
				if(!(entry instanceof Map)) {
					logger.fatal("{}: Expected all list elements to be a Map structure but got '{}' for file '{}'.",
							cn,entry.getClass().getSimpleName(),res.getPath());
					return false;
				}
				
				String name;
				String description;
				Class<?> handler;
				List<String> depHandlerNames;
				Op rootPath;
				Op otherPaths;
				Op outPaths;
				Op depPaths;
				Map<String,IPhaseOption<?>> options;
				
				Map<?,?> map = ((Map<?,?>)entry);
				Object temp = map.get("name");
				if(temp == null || !(temp instanceof String)) {
					logger.fatal("{}: No entry or non-string entry for key 'name' in phase file '{}'.",cn,res.getPath());
					return false;
				}
				name = (String)temp;
				
				temp = map.get("description");
				if(temp == null || !(temp instanceof String)) {
					logger.fatal("{}: No entry or non-string entry for key 'description' in phase file '{}'.",cn,res.getPath());
					return false;
				}
				description = (String)temp;
				
				temp = map.get("handler");
				if(temp == null || !(temp instanceof String)) {
					logger.fatal("{}: No entry or non-string entry for key 'handler' in phase '{}' of file '{}'.",cn,name,res.getPath());
					return false;
				}
				handler = Class.forName((String)temp);
				
				temp = map.get("dependency-handlers");
				if(temp == null) {
					depHandlerNames = ImmutableList.of();
				} else if(temp instanceof List) {
					ImmutableList.Builder<String> b = ImmutableList.builder();
					for(Object obj : (List<?>)temp) {
						if(obj == null || !(obj instanceof String)) {
							logger.fatal("{}: Key 'dependency-handlers' is a list containing a non-string or null value in phase '{}' of file '{}'.",
									cn,name,res.getPath());
							return false;
						}
						b.add((String)obj);
					}
					depHandlerNames = b.build();
				} else if(temp instanceof String) {
					depHandlerNames = ImmutableList.of((String)temp);
				} else {
					logger.fatal("{}: Key 'dependency-handlers' must be a string, list containing strings, or unassigned in phase '{}' of file '{}'.",
							cn,name,res.getPath());
					return false;
				}
				
				temp = map.get("root-path");
				if(temp == null || !(temp instanceof String)) {
					logger.fatal("{}: No entry or non-string entry for key 'root-path' in phase '{}' of file '{}'.",cn,name,res.getPath());
					return false;
				}
				rootPath = new PathOp((String)temp);
				
				temp = map.get("other-paths");
				if(temp == null) {
					otherPaths = new AndOp(ImmutableList.of());
				} else {
					otherPaths = parsePhasePaths(temp, name, "other-paths", res, logger);
					if(otherPaths == null)
						return false;
				}
				
				temp = map.get("out-paths");
				if(temp == null) {
					outPaths = new AndOp(ImmutableList.of());
				} else {
					outPaths = parsePhasePaths(temp, name, "out-paths", res, logger);
					if(outPaths == null)
						return false;
				}
				
				temp = map.get("dependency-paths");
				if(temp == null) {
					depPaths = new AndOp(ImmutableList.of());
				} else {
					depPaths = parsePhasePaths(temp, name, "dependency-paths", res, logger);
					if(depPaths == null)
						return false;
				}
				
				temp = map.get("options");
				if(temp == null) {
					options = ImmutableMap.of();
				} else {
					if(!(temp instanceof Map)) {
						logger.fatal("{}: The value for key 'options' must be a map in phase '{}' of file '{}'.",cn,name,res.getPath());
						return false;
					}
					
					options = new LinkedHashMap<>();
					Map<?,?> m = ((Map<?,?>)temp);
					for(Object obj : m.keySet()) {
						if(!(obj instanceof String)) {
							logger.fatal("{}: All keys in the options map must be Strings in phase '{}' of file '{}'.",cn,name,res.getPath());
							return false;
						}
						Object t = m.get(obj);
						if(t == null || !(t instanceof List)) {
							logger.fatal("{}: All values in the options map must be Lists in phase '{}' of file '{}'.",cn,name,res.getPath());
							return false;
						}
						String optName = (String)obj;
						List<?> optVals = (List<?>)t;
						Object type = optVals.get(0);
						if(type.equals("boolean")) {
							Object des = optVals.get(1);
							if(des == null || !(des instanceof String)) {
								logger.fatal("{}: Option '{}' description must be String type in phase '{}' of file '{}'.",cn,optName,name,res.getPath());
								return false;
							}
							options.put(optName,new BooleanOption(optName,(String)des));
						} else if(type.equals("int")) {
							Object def = optVals.get(1);
							Object des = optVals.get(2);
							if(def == null || !(def instanceof Integer)) {
								logger.fatal("{}: Option '{}' default must be Integer type in phase '{}' of file '{}'.",cn,optName,name,res.getPath());
								return false;
							}
							if(des == null || !(des instanceof String)) {
								logger.fatal("{}: Option '{}' description must be String type in phase '{}' of file '{}'.",cn,optName,name,res.getPath());
								return false;
							}
							options.put(optName,new IntOption(optName,(Integer)def,(String)des));
						} else if(type.equals("path")) {
							Object def = optVals.get(1);
							Object des = optVals.get(2);
							if(def == null || !(def instanceof String)) {
								logger.fatal("{}: Option '{}' path key must be String type in phase '{}' of file '{}'.",cn,optName,name,res.getPath());
								return false;
							}
							if(des == null || !(des instanceof String)) {
								logger.fatal("{}: Option '{}' description must be String type in phase '{}' of file '{}'.",cn,optName,name,res.getPath());
								return false;
							}
							options.put(optName,new PathOption(optName,(String)def,(String)des));
						} else if(type.equals("list")) {
							Object des = optVals.get(1);
							if(des == null || !(des instanceof String)) {
								logger.fatal("{}: Option '{}' description must be String type in phase '{}' of file '{}'.",cn,optName,name,res.getPath());
								return false;
							}
							options.put(optName,new ListOption(optName,(String)des));
						} else {
							logger.fatal("{}: Unrecongized option type '{}' in phase '{}' of file '{}'.",cn,type,name,res.getPath());
							return false;
						}
					}
					options = ImmutableMap.copyOf(options);
				}
				
				PhaseConfig pc = nameToPhaseConfig.get(name);
				if(pc != null) {
					logger.fatal("{}: Duplicate entries for phase '{}' of file '{}'.",cn,name,res.getPath());
					return false;
				}
				pc = new PhaseConfig(name, description, handler, depHandlerNames, rootPath, otherPaths, outPaths, depPaths, options);
				nameToPhaseConfig.put(name, pc);
			}
		} catch(Throwable t) {
			logger.fatal("{}: Something went wrong when loading config file '{}'.",t,cn,res.getPath());
			return false;
		}
		return true;
	}
	
	private static final Op parsePhasePaths(Object in, String phaseName, String mainKey, Resource res, ILogger logger) {
		if(in == null) {
			logger.fatal("{}: Lists cannot contain null values for key '{}' in phase '{}' of file '{}'.",
					cn,mainKey,phaseName,res.getPath());
			return null;
		} else if(in instanceof List) {
			int orCount = 0;
			int andCount = 0;
			List<?> list = (List<?>)in;
			for(Object obj : list) {
				if(obj != null && obj instanceof List) {
					orCount++;
				} else if(obj != null && (obj instanceof String || obj instanceof Map)) {
					andCount++;
				} else {
					logger.fatal("{}: Lists can only contain non-null values of type List, String, and Map for key '{}' in phase '{}' of file '{}'.",
							cn,mainKey,phaseName,res.getPath());
					return null;
				}
			}
			if(list.isEmpty()) {
				return new AndOp(ImmutableList.of());
			} else if(orCount == list.size()) {
				ImmutableList.Builder<AndOp> ops = ImmutableList.builder(); 
				for(Object obj : list) {
					Op ret = parsePhasePaths(obj, phaseName, mainKey, res, logger);
					if(ret == null || !(ret instanceof AndOp))
						return null;
					ops.add((AndOp)ret);
				}
				return new OrOp(ops.build());
			} else if(andCount == list.size()) {
				ImmutableList.Builder<Op> ops = ImmutableList.builder();
				for(Object obj : list) {
					Op ret = parsePhasePaths(obj, phaseName, mainKey, res, logger);
					if(ret == null)
						return null;
					ops.add(ret);
				}
				return new AndOp(ops.build());
			} else {
				logger.fatal("{}: Lists be either a list of lists or a list of Strings and Maps for key '{}' in phase '{}' of file '{}'.",
						cn,mainKey,phaseName,res.getPath());
				return null;
			}
		} else if(in instanceof String) {
			return new PathOp((String)in);
		} else if(in instanceof Map) {
			Map<?,?> m = (Map<?,?>)in;
			if(m.size() != 1) {
				logger.fatal("{}: Expected only maps of size 1 for key '{}' in phase '{}' of file '{}'.",cn,mainKey,phaseName,res.getPath());
				return null;
			}
			Object obj;
			if((obj = m.get("lookup-other-paths")) != null && obj instanceof String) {
				return new LookupOtherPathsOp((String)obj);
			} else if((obj = m.get("lookup-dependency-paths")) != null && obj instanceof String) {
				return new LookupDependencyPathsOp((String)obj);
			} else if((obj = m.get("lookup-output-paths")) != null && obj instanceof String) {
				return new LookupOutputPathsOp((String)obj);
			} else if((obj = m.get("lookup-root-path")) != null && obj instanceof String) {
				return new LookupRootPathOp((String)obj);
			} else {
				logger.fatal("{}: Child map entry contains invalid key or a value that is not a string for the key '{}' in phase '{}' of file '{}'.",
						cn,mainKey,phaseName,res.getPath());
				return null;
			}
		} else {
			logger.fatal("{}: Unexpected type '{}' somewhere in the value for key '{}' in phase '{}' of file '{}'.",
					cn,in.getClass().getSimpleName(),mainKey,phaseName,res.getPath());
			return null;
		}
	}
	
	private static final boolean getPhaseGroupsFromResources(List<Class<?>> dataAccessorHierarchy, Map<String,PhaseGroupConfig> nameToPhaseGroupConfig, 
			ILogger logger) {
		try (ScanResult scanResult = new ClassGraph().whitelistPaths("config/phase_groups").scan()) {
			for(Resource res : scanResult.getAllResources()) {
				if(!getPhaseGroupFromConfig(res, dataAccessorHierarchy, nameToPhaseGroupConfig, logger))
					return false;
			}
			return true;
		} catch(Throwable t) {
			logger.fatal("{}: Something went wrong when scanning for phase group config files.",cn);
			return false;
		}
	}
	
	private static final boolean getPhaseGroupFromConfig(Resource res, List<Class<?>> dataAccessorHierarchy, Map<String,PhaseGroupConfig> nameToPhaseGroupConfig, 
			ILogger logger) {
		try(BufferedReader br = new BufferedReader(new InputStreamReader(res.open()))) {
			Object o = new Load(LoadSettings.builder().build()).loadFromReader(br);
			if(o == null) {
				return true;
			} else if(!(o instanceof Map)) {
				logger.fatal("{}: Expected Map structure but got '{}' for file '{}'.",cn,o.getClass().getSimpleName(),res.getPath());
				return false;
			}
			Map<?,?> map = ((Map<?,?>)o);
			
			String name;
			List<String> phaseNames;
			Class<?> daClazz;
			List<IQuickOption> options;
			String description;
			
			Object temp = map.get("data-accessor");
			if(temp == null || !(temp instanceof String)) {
				logger.fatal("{}: No entry or non-string entry for key 'data-accessor' in phase group file '{}'.",cn,res.getPath());
				return false;
			}
			String adClass = (String)temp;
			Class<?> clazz = Class.forName(adClass);
			daClazz = clazz;
			if(dataAccessorHierarchy.isEmpty()) {
				while(!clazz.getName().equals("java.lang.Object")) {
					dataAccessorHierarchy.add(0, clazz);
					clazz = clazz.getSuperclass();
				}
			} else {
				int pos = dataAccessorHierarchy.size();
				boolean changed = false;
				while(!dataAccessorHierarchy.contains(clazz)) {
					dataAccessorHierarchy.add(pos, clazz);
					changed = true;
					clazz = clazz.getSuperclass();
				}
				if(changed) {
					Class<?> superclass = null;
					for(int i = 0; i < dataAccessorHierarchy.size(); i++) {
						if(i == 0) {
							superclass = dataAccessorHierarchy.get(0);
						} else {
							if(!dataAccessorHierarchy.get(i).getSuperclass().equals(superclass)) {
								logger.fatal("{}: Data accessor class '{}' of phase group file '{}' may be extending the same class as a data accessor "
										+ "already read in.",cn,adClass,res.getPath());
								return false;
							}
							superclass = dataAccessorHierarchy.get(i);
						}
					}
				}
			}
			
			temp = map.get("name");
			if(temp == null || !(temp instanceof String)) {
				logger.fatal("{}: No entry or non-string entry for key 'name' in phase group file '{}'.",cn,res.getPath());
				return false;
			}
			name = (String)temp;
			
			temp = map.get("description");
			if(temp == null || !(temp instanceof String)) {
				logger.fatal("{}: No entry or non-string entry for key 'description' in phase group file '{}'.",cn,res.getPath());
				return false;
			}
			description = (String)temp;
			
			temp = map.get("phases");
			if(temp == null || !(temp instanceof List || temp instanceof String)) {
				logger.fatal("{}: No entry or non-string/list entry for key 'phases' in phase group file '{}'.",cn,res.getPath());
				return false;
			}
			if(temp instanceof String) {
				phaseNames = ImmutableList.of((String)temp);
			} else {
				if(((List<?>)temp).isEmpty()) {
					logger.fatal("{}: A list cannot be empty for key 'phases' in phase group file '{}'.",cn,res.getPath());
					return false;
				}
				ImmutableList.Builder<String> b = ImmutableList.builder();
				for(Object obj : (List<?>)temp) {
					if(!(obj instanceof String)) {
						logger.fatal("{}: A list must only contain Strings for key 'phases' in phase group file '{}'.",cn,res.getPath());
						return false;
					}
					b.add((String)obj);
				}
				phaseNames = b.build();
			}
			
			temp = map.get("quick-options");
			if(temp == null) {
				options = ImmutableList.of();
			} else if(!(temp instanceof Map)) {
				logger.fatal("{}: Must be a Map for key 'options' in phase group file '{}'.",cn,res.getPath());
				return false;
			} else {
				options = new ArrayList<>();
				for(Object optNameObj : ((Map<?,?>)temp).keySet()) {
					if(optNameObj == null || !(optNameObj instanceof String)) {
						logger.fatal("{}: Quick option name must be a String in phase group file '{}'.",cn,res.getPath());
						return false;
					}
					Object fields = ((Map<?,?>)temp).get(optNameObj);
					if(fields == null || !(fields instanceof Map)) {
						logger.fatal("{}: Quick option data fields must be a Map for option '{}' in phase group file '{}'.",cn,optNameObj,res.getPath());
						return false;
					}
					Object desObj = ((Map<?,?>)fields).get("description");
					if(desObj == null || !(desObj instanceof String)) {
						logger.fatal("{}: Quick option description must be a String for option '{}' in phase group file '{}'.",cn,optNameObj,res.getPath());
						return false;
					}
					Object triggerPairsObj = ((Map<?,?>)fields).get("enabled-phases");
					if(triggerPairsObj == null || !(triggerPairsObj instanceof Map)) {
						logger.fatal("{}: Quick option trigger pairs must be a Map for option '{}' in phase group file '{}'.",cn,optNameObj,res.getPath());
						return false;
					}
					Map<String,List<String>> phaseNameToTriggers = new LinkedHashMap<>();
					for(Object phaseNameObj : ((Map<?,?>)triggerPairsObj).keySet()) {
						if(phaseNameObj == null || !(phaseNameObj instanceof String)) {
							logger.fatal("{}: Quick option phase name must be a String for option '{}' in phase group file '{}'.",cn,optNameObj,res.getPath());
							return false;
						}
						Object triggersObj = ((Map<?,?>)triggerPairsObj).get(phaseNameObj);
						if(triggersObj == null || !(triggersObj instanceof List)) {
							logger.fatal("{}: Quick option triggers must be a List for option '{}' in phase group file '{}'.",cn,optNameObj,res.getPath());
							return false;
						}
						List<?> triggers = (List<?>)triggersObj;
						if(triggers.size() % 2 != 0) {
							logger.fatal("{}: Quick option triggers must have a even number of elements option '{}' in phase group file '{}'.",cn,
									optNameObj,res.getPath());
							return false;
						}
						List<String> values = new ArrayList<>();
						for(Object obj : triggers) {
							if(obj == null || !(obj instanceof String)) {
								logger.fatal("{}: Quick option triggers all be Strings for option '{}' in phase group file '{}'.",cn,optNameObj,res.getPath());
								return false;
							}
							values.add((String)obj);
						}
						phaseNameToTriggers.put((String)phaseNameObj, ImmutableList.copyOf(values));
					}
					options.add(new QuickOption((String)optNameObj,phaseNameToTriggers,(String)desObj));
				}
				options = ImmutableList.copyOf(options);
			}
			
			nameToPhaseGroupConfig.put(name, new PhaseGroupConfig(name, phaseNames, daClazz, options, description));
		} catch(Throwable t) {
			logger.fatal("{}: Something went wrong when loading config file '{}'.",t,cn,res.getPath());
			return false;
		}
		return true;
	}
	
	private static final boolean getFilePathsFromAllConfigs(Map<String,List<String>> keyToPathSegments, ILogger logger) {
		try (ScanResult scanResult = new ClassGraph().whitelistPaths("config/files").scan()) {
			for(Resource res : scanResult.getAllResources()) {
				if(!getFilePathsFromConfig(res, keyToPathSegments, logger))
					return false;
			}
			return true;
		} catch(Throwable t) {
			logger.fatal("{}: Something went wrong when scanning for file path config files.",cn);
			return false;
		}
	}
	
	private static final boolean getFilePathsFromConfig(Resource res, Map<String,List<String>> keyToPathSegments, ILogger logger) {
		try(BufferedReader br = new BufferedReader(new InputStreamReader(res.open()))) {
			Object o = new Load(LoadSettings.builder().build()).loadFromReader(br);
			if(o == null) {
				return true;
			} else if(!(o instanceof Map)) {
				logger.fatal("{}: Expected Map structure but got '{}' for file '{}'.",cn,o.getClass().getSimpleName(),res.getPath());
				return false;
			}
			for(Entry<?,?> e : ((Map<?,?>)o).entrySet()) {
				if(!(e.getKey() instanceof String)) {
					logger.fatal("{}: All keys must be strings but key '{}' is '{}' for file '{}'.",cn,e.getKey(),e.getKey().getClass().getSimpleName(),
							res.getPath());
					return false;
				}
				String key = (String)e.getKey();
				Object v = e.getValue();
				ImmutableList<String> valsList = null;
				if(v instanceof String) {
					valsList = ImmutableList.of((String)v);
				} else if(v instanceof List) {
					ImmutableList.Builder<String> b = ImmutableList.builder();
					for(Object obj : (List<?>)v) {
						if(!(obj instanceof String)) {
							logger.fatal("{}: All entries in a list must be strings but entry '{}' is '{}' for key '{}' in file '{}'.",cn,
									obj,obj.getClass().getSimpleName(),key,res.getPath());
							return false;
						}
						b.add((String)obj);
					}
					valsList = b.build();
				} else {
					logger.fatal("{}: All values must be a single string or a list of strings but value '{}' is '{}' for key '{}' in file '{}'.",cn,
							v,v.getClass().getSimpleName(),key,res.getPath());
					return false;
				}
				List<String> vals = keyToPathSegments.get(key);
				if(vals != null) {
					logger.fatal("{}: Key '{}' already has the value '{}'. Cannot set it to value '{}' from file '{}'.",cn,key,vals,v,res.getPath());
					return false;
				}
				keyToPathSegments.put(key, valsList);
			}
		} catch(Throwable t) {
			logger.fatal("{}: Something went wrong when loading config file '{}'.",t,cn,res.getPath());
			return false;
		}
		return true;
	}
	
}
