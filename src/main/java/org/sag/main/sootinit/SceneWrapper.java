package org.sag.main.sootinit;

import soot.Scene;
import soot.SootClass;
import soot.Singletons.Global;

public class SceneWrapper extends Scene {

	public SceneWrapper(Global g) {
		super(g);
	}
	
	/* Revert back to before commit ff05e7bda7953b4fc71e48bafef07f6997115de6.
	 * That commit added a unnecessary runtime exception that prevented basic
	 * classes from being phantom. In some cases, (i.e. where we are loading 
	 * parts of full system libraries, we do not care about the basic classes
	 * being phantom or what it might do if there is no exception hierarchy.
	 */
	public void loadBasicClasses() {
		addReflectionTraceClasses();

		for (int i = SootClass.BODIES; i >= SootClass.HIERARCHY; i--) {
			for (String name : basicclasses[i]) {
				tryLoadClass(name, i);
			}
		}
	}

}
