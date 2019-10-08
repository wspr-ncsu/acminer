package org.sag.main.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.sag.main.phase.IPhaseGroup;
import org.sag.main.phase.IPhaseHandler;
import org.sag.main.phase.IQuickOption;

public final class PhaseGroupConfig {
	
	private final String name;
	private final List<String> phaseNames;
	private final Class<?> daClass;
	private volatile Config config;
	private final List<IQuickOption> options;
	
	PhaseGroupConfig(String name, List<String> phaseNames, Class<?> daClass, List<IQuickOption> options) {
		this.name = name;
		this.phaseNames = phaseNames;
		this.daClass = daClass;
		this.options = options;
	}
	
	public void setConfig(Config config) {
		this.config = config;
	}
	
	public String getPhaseGroupName() {
		return name;
	}
	
	public List<String> getPhaseNames() {
		return phaseNames;
	}
	
	public Class<?> getDataAccessorClass() {
		return daClass;
	}
	
	public List<IPhaseHandler> resolveHandlers() {
		List<IPhaseHandler> ret = new ArrayList<>();
		Map<String,IPhaseHandler> existingHandlers = new HashMap<>();
		for(String phaseName : phaseNames) {
			IPhaseHandler ph = config.getPhaseConfig(phaseName).getPhaseHandler(existingHandlers);
			existingHandlers.put(phaseName, ph);
			ret.add(ph);
		}
		return ret;
	}
	
	public Map<String,IQuickOption> resolveOptions(IPhaseGroup group) {
		Map<String,IQuickOption> ret = new LinkedHashMap<>();
		for(IQuickOption opt : options) {
			ret.put(opt.name(group),opt);
		}
		return ret;
	}

}
