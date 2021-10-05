package org.sag.main.sootinit;

import java.util.Set;

import soot.ModuleScene;
import soot.ModuleUtil;
import soot.SootClass;
import soot.Singletons.Global;

public class ModuleSceneWrapper extends ModuleScene {

	public ModuleSceneWrapper(Global g) {
		super(g);
	}
	
	@Override
	public void loadBasicClasses() {
		addReflectionTraceClasses();
		Set<String>[] basicclasses = getBasicClassesIncludingResolveLevel();

		for (int i = SootClass.BODIES; i >= SootClass.HIERARCHY; i--) {
			for (String name : basicclasses[i]) {
				ModuleUtil.ModuleClassNameWrapper wrapper = ModuleUtil.v().makeWrapper(name);
				tryLoadClass(wrapper.getClassName(), i, wrapper.getModuleNameOptional());
			}
		}
    }

}
