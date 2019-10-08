package org.sag.main.phase;

import java.util.List;
import java.util.Map;

public interface IQuickOption {
	
	public void set(IPhaseGroup group);
	public String name(IPhaseGroup group);
	
	public static class QuickOption implements IQuickOption {
		
		private final String name;
		private final Map<String,List<String>> nameToOptionOpts;
		
		public QuickOption(String name, Map<String,List<String>> nameToOptionOpts) {
			this.name = name;
			this.nameToOptionOpts = nameToOptionOpts;
		}

		@Override
		public void set(IPhaseGroup group) {
			for(String phaseName : nameToOptionOpts.keySet()) {
				List<String> opts = nameToOptionOpts.get(phaseName);
				for(int i = 0; i < opts.size(); i = i+2) {
					group.setPhaseOptionForHandler(phaseName, opts.get(i), opts.get(i+1));
				}
			}
		}

		@Override
		public String name(IPhaseGroup group) {
			if(name.equals("default")) {
				return "--" + group.getName();
			} else {
				return "--" + group.getName() + name;
			}
		}
		
	}
	
	
}