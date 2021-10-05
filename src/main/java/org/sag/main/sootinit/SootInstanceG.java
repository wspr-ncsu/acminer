package org.sag.main.sootinit;

import soot.G;
import soot.ModuleScene;
import soot.Scene;

public class SootInstanceG extends G {

	private Scene instance_soot_Scene;
	private ModuleScene instance_soot_ModuleScene;
	
	@Override
	public Scene soot_Scene() {
		if (instance_soot_Scene == null) {
	    	synchronized (this) {
	       		if (instance_soot_Scene == null)
		        	instance_soot_Scene = new SceneWrapper(g);
	       	}
	    }
	    return instance_soot_Scene;
	}
	
	@Override
	protected void release_soot_Scene() {
		instance_soot_Scene = null;
	}
	
	@Override
	public ModuleScene soot_ModuleScene() {
		if (instance_soot_ModuleScene == null) {
			synchronized (this) {
				if (instance_soot_ModuleScene == null)
					instance_soot_ModuleScene = new ModuleSceneWrapper(g);
				}
		}
		return instance_soot_ModuleScene;
	}
	
	@Override
	protected void release_soot_ModuleScene() {
		instance_soot_ModuleScene = null;
	}

}
