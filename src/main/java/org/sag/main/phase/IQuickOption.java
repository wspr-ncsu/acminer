package org.sag.main.phase;

import com.google.common.collect.ImmutableList;
import org.sag.common.tools.TextUtils;

import java.util.List;
import java.util.Map;

public interface IQuickOption {
	
	public void set(IPhaseGroup group);
	public String name(IPhaseGroup group);
	public String getDescription();

	public String getHelpDiag(String spacer, int longestName, IPhaseGroup group);

	public List<String> getAffectedPhaseNames();
	
	public static class QuickOption implements IQuickOption {
		
		private final String name;
		private final Map<String,List<String>> nameToOptionOpts;
		private final String description;
		
		public QuickOption(String name, Map<String,List<String>> nameToOptionOpts, String description) {
			this.name = name;
			this.nameToOptionOpts = nameToOptionOpts;
			this.description = description;
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
		public List<String> getAffectedPhaseNames() {
			return ImmutableList.copyOf(nameToOptionOpts.keySet());
		}

		@Override
		public String name(IPhaseGroup group) {
			if(name.equals("default")) {
				return "--" + group.getName();
			} else {
				return "--" + group.getName() + name;
			}
		}
		
		@Override
		public String getDescription() {
			return description;
		}

		@Override
		public String getHelpDiag(String spacer, int longestName, IPhaseGroup group) {
			int headLength = spacer.length() + longestName + 3;
			StringBuilder sb = new StringBuilder();
			sb.append(" = (-p ").append(group.getName());
			for(String phaseName : nameToOptionOpts.keySet()) {
				List<String> opts = nameToOptionOpts.get(phaseName);
				sb.append(" ").append(phaseName).append(" ");
				boolean first = true;
				for(int i = 0; i < opts.size(); i = i+2) {
					if(first)
						first = false;
					else
						sb.append(",");
					sb.append(opts.get(i)).append(":").append(opts.get(i+1));
				}
			}
			sb.append(")");
			return spacer + TextUtils.rightPad(name(group), longestName) + " - " + TextUtils.wrap(getDescription()
							+ sb.toString(),80, "\n", TextUtils.leftPad("", headLength),
					headLength, true);
		}
		
	}
	
	
}