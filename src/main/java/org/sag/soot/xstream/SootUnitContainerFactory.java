package org.sag.soot.xstream;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.sag.common.xstream.XStreamInOut;
import org.sag.common.xstream.XStreamInOut.XStreamInOutInterface.AbstractXStreamSetup;

import com.thoughtworks.xstream.XStream;

import soot.SootMethod;
import soot.Unit;

//TODO Add other types to this factory that handle the various types of units
public class SootUnitContainerFactory {
	
	private static Map<Unit, SootUnitContainer> sootUnitContainers;
	private static final Object lock = new Object();
	
	public static SootUnitContainer makeSootUnitContainer(Unit u, SootMethod source){
		synchronized(lock) {
			SootUnitContainer ret = null;
			if(sootUnitContainers == null){
				sootUnitContainers = new HashMap<>();
				ret = new SootUnitContainer(u,source);
				sootUnitContainers.put(u, ret);
			}else{
				ret = sootUnitContainers.get(u);
				if(ret == null){
					ret = new SootUnitContainer(u,source);
					sootUnitContainers.put(u, ret);
				}
			}
			return ret;
		}
	}
	
	public static void reset(){
		synchronized(lock) {
			sootUnitContainers = null;
		}
	}
	
	public static SootUnitContainer readXMLStatic(String filePath, Path path) throws Exception {
		return (SootUnitContainer) XStreamInOut.readXML(filePath, path, getXStreamSetupStatic().getXStream());
	}
	
	private static AbstractXStreamSetup xstreamSetup = null;

	public static AbstractXStreamSetup getXStreamSetupStatic(){
		if(xstreamSetup == null)
			xstreamSetup = new XStreamSetup();
		return xstreamSetup;
	}
	
	public static class XStreamSetup extends AbstractXStreamSetup {
		
		@Override
		public void getOutputGraph(LinkedHashSet<AbstractXStreamSetup> in) {
			if(!in.contains(this)) {
				in.add(this);
				SootUnitContainer.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.emptySet();
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}
}
