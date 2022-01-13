package org.sag.main.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.sag.common.io.FileHelpers;
import org.sag.main.phase.IPhaseHandler;
import org.sag.main.phase.IPhaseOption;

import com.google.common.collect.ImmutableList;

public final class PhaseConfig {
	
	private final String name;
	private final String description;
	private final Class<?> handler;
	private final List<String> depHandlerNames;
	private final Op rootPath;
	private final Op otherPaths;
	private final Op outPaths;
	private final Op depPaths;
	private final Map<String,IPhaseOption<?>> options;
	private volatile Config config;
	
	PhaseConfig(String name, String description, Class<?> handler, List<String> depHandlerNames, Op rootPath, Op otherPaths, Op outPaths, Op depPaths, 
			Map<String,IPhaseOption<?>> options) {
		this.name = name;
		this.description = description;
		this.handler = handler;
		this.depHandlerNames = depHandlerNames;
		this.rootPath = rootPath;
		this.otherPaths = otherPaths;
		this.outPaths = outPaths;
		this.depPaths = depPaths;
		this.options = options;
	}
	
	public void setConfig(Config config) {
		this.config = config;
		for(IPhaseOption<?> opt : options.values()) {
			opt.setConfig(config);
		}
	}
	
	public String getPhaseName() {
		return name;
	}
	
	public String getDescription() {
		return description;
	}
	
	public Path getRootPath() {
		return rootPath.resolve(config).get(0);
	}
	
	public List<Path> getDependencyPaths() {
		return depPaths.resolve(config);
	}
	
	public List<Path> getOutputPaths() {
		return outPaths.resolve(config);
	}
	
	public List<Path> getOtherPaths() {
		return otherPaths.resolve(config);
	}
	
	public List<String> getDependencyHandlerNames() {
		return depHandlerNames;
	}
	
	public Map<String,IPhaseOption<?>> getOptions() {
		return options;
	}
	
	public IPhaseHandler getPhaseHandler(Map<String,IPhaseHandler> existingHandlers) {
		List<IPhaseHandler> depHandlers = null;
		if(depHandlerNames != null && !depHandlerNames.isEmpty()) {
			depHandlers = new ArrayList<>();
			for(String depName : depHandlerNames) {
				IPhaseHandler cur = existingHandlers.get(depName);
				if(cur == null) {
					cur = config.getPhaseConfig(depName).getPhaseHandler(existingHandlers);
					existingHandlers.put(depName, cur);
				}
				depHandlers.add(cur);
			}
		}
		
		try {
			return (IPhaseHandler) handler.getConstructor(new Class[] {List.class, PhaseConfig.class}).newInstance(depHandlers, this);
		} catch(Throwable t) {
			throw new RuntimeException("Error: Failed to get new instance of '" + handler + "'",t);
		}
	}
	
	
	interface Op {
		List<Path> resolve(Config config);
		String toString();
	}
	
	static class OrOp implements Op {
		
		private final List<AndOp> ops;
		
		OrOp(List<AndOp> ops) {
			this.ops = ops;
		}
		
		public List<Path> resolve(Config config) {
			for(AndOp op : ops) {
				List<Path> cur = op.resolve(config);
				boolean allExist = true;
				for(Path p : cur) {
					if(!FileHelpers.checkRWFileExists(p)) {
						allExist = false;
						break;
					}
				}
				if(allExist)
					return cur;
			}
			throw new RuntimeException("Error: No path groups are viable for op '" + toString() + "'");
		}
		
		@Override
		public String toString() {
			List<String> ret = new ArrayList<>();
			for(AndOp op : ops) {
				ret.add(op.toString());
			}
			if(ret.size() == 0)
				return "";
			else if(ret.size() == 1)
				return ret.get(0);
			else
				return ret.toString();
		}
		
	}
	
	static class AndOp implements Op {
		
		private final List<Op> ops;
		
		AndOp(List<Op> ops) {
			this.ops = ops;
		}
		
		public List<Path> resolve(Config config) {
			List<Path> ret = new ArrayList<>();
			for(Op op : ops) {
				List<Path> cur = op.resolve(config);
				for(Path p : cur) {
					if(!ret.contains(p))
						ret.add(p);
				}
			}
			return ImmutableList.copyOf(ret);
		}
		
		@Override
		public String toString() {
			List<String> ret = new ArrayList<>();
			for(Op op : ops) {
				ret.add(op.toString());
			}
			if(ret.size() == 0)
				return "";
			else if(ret.size() == 1)
				return ret.get(0);
			else
				return ret.toString();
		}
		
	}
	
	static class LookupOtherPathsOp implements Op {
		
		private final String phaseName;
		
		LookupOtherPathsOp(String phaseName) {
			this.phaseName = phaseName;
		}
		
		public List<Path> resolve(Config config) {
			return config.getPhaseConfig(phaseName).otherPaths.resolve(config);
		}
		
		@Override
		public String toString() {
			return "other-paths: " + phaseName;
		}
		
	}
	
	static class LookupDependencyPathsOp implements Op {
		
		private final String phaseName;
		
		LookupDependencyPathsOp(String phaseName) {
			this.phaseName = phaseName;
		}
		
		public List<Path> resolve(Config config) {
			return config.getPhaseConfig(phaseName).depPaths.resolve(config);
		}
		
		@Override
		public String toString() {
			return "dependency-paths: " + phaseName;
		}
		
	}
	
	static class LookupOutputPathsOp implements Op {
		
		private final String phaseName;
		
		LookupOutputPathsOp(String phaseName) {
			this.phaseName = phaseName;
		}
		
		public List<Path> resolve(Config config) {
			return config.getPhaseConfig(phaseName).outPaths.resolve(config);
		}
		
		@Override
		public String toString() {
			return "out-paths: " + phaseName;
		}
		
	}
	
	static class LookupRootPathOp implements Op {
		
		private final String phaseName;
		
		LookupRootPathOp(String phaseName) {
			this.phaseName = phaseName;
		}
		
		public List<Path> resolve(Config config) {
			return config.getPhaseConfig(phaseName).rootPath.resolve(config);
		}
		
		@Override
		public String toString() {
			return "root-path: " + phaseName;
		}
		
	}
	
	static class PathOp implements Op {
		
		private final String pathKey;
		
		PathOp(String pathKey) {
			this.pathKey = pathKey;
		}
		
		public List<Path> resolve(Config config) {
			return ImmutableList.of(config.getFilePath(pathKey));
		}
		
		@Override
		public String toString() {
			return pathKey;
		}
		
	}

}
