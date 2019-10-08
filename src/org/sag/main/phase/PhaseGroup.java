package org.sag.main.phase;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sag.common.logging.ILogger;
import org.sag.main.AndroidInfo;
import org.sag.main.IDataAccessor;
import org.sag.main.config.PhaseGroupConfig;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public final class PhaseGroup implements IPhaseGroup {
	
	protected final String cn;
	private final PhaseGroupConfig pgConfig;
	protected final List<IPhaseHandler> handlers;//Order matters because some generate files that others depend on
	protected final Map<String,IQuickOption> quickOptions;
	protected ILogger logger;
	protected IDataAccessor dataAccessor;
	protected AndroidInfo ai;
	protected boolean hasBeenInit;
	protected boolean isEnabled;
	
	public PhaseGroup(PhaseGroupConfig pgConfig) {
		this.pgConfig = pgConfig;
		this.logger = null;
		this.handlers = ImmutableList.copyOf(pgConfig.resolveHandlers());
		this.dataAccessor = null;
		this.ai = null;
		this.cn = this.getClass().getSimpleName();
		this.hasBeenInit = false;
		this.isEnabled = false;
		this.quickOptions = ImmutableMap.copyOf(pgConfig.resolveOptions(this));
	}
	
	@Override
	public void init(IDataAccessor dataAccessor, AndroidInfo ai, ILogger logger) {
		this.logger = logger;
		this.dataAccessor = dataAccessor;
		this.ai = ai;
		for(IPhaseHandler h : handlers) {
			if(h.isEnabled())
				h.init(dataAccessor, ai, logger);
		}
		this.hasBeenInit = true;
	}
	
	@Override
	public Set<Path> getRequiredInputFilePaths() {
		Set<Path> inputPathLists = new LinkedHashSet<>();
		Set<Path> output = new LinkedHashSet<>();
		for(IPhaseHandler h : handlers) {
			if(h.isEnabled()) {
				inputPathLists.addAll(h.getDependencyFiles());
				output.addAll(h.getOutputFilePaths());
			}
		}
		for(Path p : output) {
			inputPathLists.remove(p);
		}
		return inputPathLists;
	}
	
	@Override
	public IPhaseHandler getHandlerByClass(Class<?> c) {
		for(IPhaseHandler h : handlers) {
			if(h.getClass().equals(c))
				return h;
		}
		//Logger may not be initialized 
		throw new RuntimeException("Error: Could not find handler '" + c.getSimpleName() + "'");
	}
	
	@Override
	public IPhaseHandler getHandlerByName(String name) {
		for(IPhaseHandler h : handlers) {
			if(h.getName().equals(name))
				return h;
		}
		//Logger may not be initialized 
		throw new RuntimeException("Error: Could not find handler '" + name + "'");
	}
	
	@Override
	public void setPhaseOptionForHandler(Class<?> c, String optionName, String optionValue) {
		getHandlerByClass(c).setPhaseOption(optionName, optionValue);
	}
	
	@Override
	public void setPhaseOptionForHandler(String handlerName, String optionName, String optionValue) {
		getHandlerByName(handlerName).setPhaseOption(optionName, optionValue);
	}
	
	@Override
	public boolean isEnabled() {
		return isEnabled;
	}
	
	@Override
	public void enable() {
		this.isEnabled = true;
	}
	
	@Override
	public String getName() {
		return pgConfig.getPhaseGroupName();
	}
	
	@Override
	public void setQuickOption(String name) {
		IQuickOption option = quickOptions.get(name);
		if(option == null)
			throw new RuntimeException("Error: The quick option '" + name + "' does not exist for phase group '" + name + "'.");
		option.set(this);
	}
	
	@Override
	public boolean run(){
		if(!isEnabled)
			return false;
		if(!hasBeenInit)// The logger does not exist unless true
			throw new RuntimeException("Error: Init has not been run for '" + getName() + "'. The phases will not be run.");
		logger.info("{}: Running the requested phases for phase group '{}'.",cn,getName());
		int totalHandlers = handlers.size();
		int success = 0;
		int notRequired = 0;
		int notEnabled = 0;
		String failure = null;
		for(IPhaseHandler handler : handlers){
			int ret;
			try{
				ret = handler.runIfNeeded();
			}catch(Throwable t){
				logger.fatal("{}: Unhandled exception when running phase '{}' of phase group '{}'.",t,cn,handler.getName(),getName());
				ret = 0;
			}
			if(ret == 0){
				failure = handler.getName();
				break;
			}else if(ret == 1){
				success++;
			}else if(ret == 2){
				notRequired++;
			}else{
				notEnabled++;
			}
		}
		if(failure != null){
			logger.fatal("{}: Failed to run phase '{}' in phase group '{}'.\n\tSuccess:      {}/{}\n\tNot Enabled:  {}/{}\n\t"
					+ "Not Required: {}/{}\n\tNot Run:      {}/{}", cn, failure, getName(), success, totalHandlers, notEnabled,
					totalHandlers, notRequired, totalHandlers, totalHandlers - success - notRequired - 1, totalHandlers);
			return false;
		}else{
			logger.info("{}: Successfully completed all phases for phase group '{}'.\n\tSuccess:      {}/{}\n\t"
					+ "Not Enabled:  {}/{}\n\tNot Required: {}/{}", cn, getName(), success, totalHandlers, notEnabled,
					totalHandlers, notRequired, totalHandlers);
			return true;
		}
	}

}
