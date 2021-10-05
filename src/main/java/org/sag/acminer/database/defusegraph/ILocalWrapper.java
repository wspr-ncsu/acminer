package org.sag.acminer.database.defusegraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.sag.common.xstream.XStreamInOut.XStreamInOutInterface;
import org.sag.soot.SootSort;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import soot.Body;
import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

@XStreamAlias("ILocalWrapper")
public interface ILocalWrapper extends XStreamInOutInterface, Comparable<ILocalWrapper> {
	
	public Local getLocal();
	public long getNum();
	public String getOrgString();
	public String toString();
	public int hashCode();
	public boolean equals(Object o);
	
	public static final class Factory {
		
		private static Map<SootMethod,Map<Local,LocalWrapper>> existingLocalWrappers = null;
		private static volatile AtomicLong count = null;
		
		public static void init() {
			if(existingLocalWrappers == null) {
				existingLocalWrappers = new HashMap<>();
				count = new AtomicLong();
				long c = 0;
				List<SootClass> classes = new ArrayList<>(Scene.v().getClasses());
				Collections.sort(classes,SootSort.scComp);
				for(SootClass sc : classes) {
					List<SootMethod> methods = new ArrayList<>(sc.getMethods());
					Collections.sort(methods,SootSort.smComp);
					for(SootMethod sm : methods) {
						if(sm.isConcrete()) {
							try {
								Body b = sm.retrieveActiveBody();
								Map<Local,LocalWrapper> localToLocalWrapper = new HashMap<>();
								//Locals are already sorted by soot.jimple.toolkits.scalar.LocalNameStandardizer
								for(Local l : b.getLocals())
									localToLocalWrapper.put(l, new LocalWrapper(l,sm,c++));
								if(!localToLocalWrapper.isEmpty())
									existingLocalWrappers.put(sm, localToLocalWrapper);
							} catch(Throwable t) {}
						}
					}
				}
				count.set(c);
			}
		}
		
		public static void reset() {
			existingLocalWrappers = null;
			count = null;
		}
		
		public static LocalWrapper get(Local local, SootMethod source) {
			Objects.requireNonNull(local);
			Objects.requireNonNull(source);
			if(existingLocalWrappers == null)
				return null;
			Map<Local,LocalWrapper> lToLw = existingLocalWrappers.get(source);
			if(lToLw == null)
				return null;
			return lToLw.get(local);
		}
		
		public static InlineConstantLocalWrapper get() {
			return new InlineConstantLocalWrapper(count.getAndIncrement());
		}
		
		private static final XStreamSetup xstreamSetup = new XStreamSetup();

		public static XStreamSetup getXStreamSetupStatic(){
			return xstreamSetup;
		}
		
		public static class XStreamSetup extends AbstractXStreamSetup {
			
			@Override
			public void getOutputGraph(LinkedHashSet<AbstractXStreamSetup> in) {
				if(!in.contains(this)) {
					in.add(this);
				}
			}

			@Override
			public Set<Class<?>> getAnnotatedClasses() {
				Set<Class<?>> ret = new HashSet<>();
				ret.add(LocalWrapper.class);
				ret.add(InlineConstantLocalWrapper.class);
				ret.add(ILocalWrapper.class);
				return ret;
			}
			
			@Override
			public void setXStreamOptions(XStream xstream) {
				defaultOptionsXPathRelRef(xstream);
			}
			
		}
		
	}

}
