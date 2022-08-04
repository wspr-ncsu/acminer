package org.sag.main.phase;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.sag.common.logging.ILogger;
import org.sag.common.tools.TextUtils;
import org.sag.main.AndroidInfo;
import org.sag.main.IDataAccessor;

import com.google.common.base.Splitter;

import javax.xml.soap.Text;

public class PhaseManager {
	
	private final String cn;
	private final List<IPhaseGroup> groups;
	private ILogger logger;
	private boolean hasBeenInit;
	private IDataAccessor dataAccessor;
	
	public PhaseManager(List<IPhaseGroup> groups) {
		this.groups = groups;
		this.cn = this.getClass().getSimpleName();
		this.hasBeenInit = false;
		this.dataAccessor = null;
	}
	
	public void init(IDataAccessor dataAccessor, AndroidInfo ai, ILogger logger) {
		this.logger = logger;
		this.dataAccessor = dataAccessor;
		for(IPhaseGroup g : groups) {
			if(g.isEnabled()) {
				g.init(dataAccessor, ai, logger);
			}
		}
		hasBeenInit = true;
	}
	
	public void enablePhaseGroup(String name) {
		for(IPhaseGroup g : groups) {
			if(g.getName().equals(name)) {
				g.enable();
				return;
			}
		}
		throw new RuntimeException("Error: The phase group '" + name + "' does not exist.");
	}
	
	public void setPhaseOptionForHandler(String name, Class<?> c, String optionName, String optionValue) {
		for(IPhaseGroup g : groups) {
			if(g.getName().equals(name)) {
				g.setPhaseOptionForHandler(c, optionName, optionValue);
				g.enable(); // Since options have been set for the group, it should be enabled
				return;
			}
		}
		throw new RuntimeException("Error: The phase group '" + name + "' does not exist.");
	}
	
	public void setPhaseOptionForHandler(String name, String handlerName, String optionName, String optionValue) {
		for(IPhaseGroup g : groups) {
			if(g.getName().equals(name)) {
				g.setPhaseOptionForHandler(handlerName, optionName, optionValue);
				g.enable(); // Since options have been set for the group, it should be enabled
				return;
			}
		}
		throw new RuntimeException("Error: The phase group '" + name + "' does not exist.");
	}
	
	public void setPhaseOptionsFromInput(List<String> phaseOptions) {
		if(phaseOptions.size() < 3)
			throw new RuntimeException("Error: The phase group name, phase name, and phase options list must be provided at minimum.");
		String phaseGroup = phaseOptions.remove(0);
		if((phaseOptions.size() % 2) != 0)
			throw new RuntimeException("Error: A phase name must always be followed by a phase options list.");
		String phaseName = null;
		for(int i = 0; i < phaseOptions.size(); i++) {
			if(i % 2 == 0) {
				phaseName = phaseOptions.get(i);
			} else {
				String[] options = phaseOptions.get(i).split(",");
				for(String option : options){
					List<String> op = Splitter.on(':').limit(2).trimResults().splitToList(option);
					if(op.size() != 2 || op.get(0).isEmpty() || op.get(1).isEmpty())
						throw new RuntimeException("Error: Could not parse the phase options '" + phaseOptions.get(i) 
							+ "' for phase '" + phaseName + "' of phase group '" + phaseGroup + "'.");
					setPhaseOptionForHandler(phaseGroup, phaseName, op.get(0), op.get(1));
				}
			}
		}
	}
	
	public void setQuickOptionsFromInput(String option) {
		for(IPhaseGroup g : groups) {
			if(option.startsWith("--" + g.getName())) {
				g.setQuickOption(option);
				g.enable(); // Since options have been set for the group, it should be enabled
				return;
			}
		}
		throw new RuntimeException("Error: Unable to locate a phase group containing the phase quick '" + option + "'");
	}
	
	public Set<Path> getRequiredInputFilePaths() {
		Set<Path> ret = new LinkedHashSet<>();
		for(IPhaseGroup g : groups) {
			ret.addAll(g.getRequiredInputFilePaths());
		}
		return ret;
	}
	
	public boolean run() {
		if(!hasBeenInit)// The logger does not exist unless true
			throw new RuntimeException("Error: Init has not been run for " + cn + ". The phase groups will not be run.");
		logger.info("{}: Running the requested phase groups.",cn);
		List<String> success = new ArrayList<>();
		String failed = null;
		List<String> notEnabled = new ArrayList<>();
		List<String> remaining = new ArrayList<>();
		for(IPhaseGroup g : groups) {
			if(g.isEnabled()) {
				if(failed == null) {
					try {
						// Reset all databases, data, and soot so we start off with a fresh analysis environment
						dataAccessor.resetAllDatabasesAndData();
						if(g.run()) {
							success.add(g.getName());
						} else {
							failed = g.getName();
						}
					} catch(Throwable t) {
						logger.fatal("{}: Unhandled exception when running phase group {}.",t,cn,g.getName());
						failed = g.getName();
					}
				} else {
					remaining.add(g.getName());
				}
			} else {
				notEnabled.add(g.getName());
			}
		}
		
		if(failed != null) {
			logger.fatal("{}: Failed to run phase group  '{}'.\n\tSuccess:     {}\n\tNot Enabled: {}\n\tNot Run:     {}",
					cn, failed, success, notEnabled, remaining);
			return false;
		} else {
			logger.info("{}: Successfully ran all phase groups.\n\tSuccess:     {}\n\tNot Enabled: {}", cn, success, notEnabled);
			return true;
		}
	}

	public String getHelpDiag(String spacer) {
		StringBuilder sb = new StringBuilder();
		if(!groups.isEmpty()) {
			String header = "[Phase Groups]";
			int leftHeader = (80 - header.length() - spacer.length()) / 2;
			int rightHeader = 80 - header.length() - leftHeader - spacer.length();
			sb.append(spacer).append(TextUtils.leftPad("", leftHeader, "+")).append(header).append(TextUtils.leftPad("", rightHeader, '+'));
			for (IPhaseGroup g : groups) {
				sb.append("\n\n").append(g.getHelpDiag(spacer + "  "));
			}
			sb.append("\n\n").append(spacer).append(TextUtils.leftPad("",80-spacer.length(),"+"));
		}
		return sb.toString();
	}

}
