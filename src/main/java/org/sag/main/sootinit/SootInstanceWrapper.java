package org.sag.main.sootinit;

import org.sag.main.IDataAccessor;
import org.sag.soot.xstream.SootClassContainer;
import org.sag.soot.xstream.SootFieldContainer;
import org.sag.soot.xstream.SootMethodContainer;
import org.sag.soot.xstream.SootUnitContainerFactory;

import soot.G;
import soot.G.GlobalObjectGetter;

/**
 * The SootInstanceWrapper is a class used to keep track of the
 * current Soot instance and the way it was initialized.
 * 
 * @author agorski
 */
public class SootInstanceWrapper implements GlobalObjectGetter {
	
	//Begin instance fields and methods

	private volatile int sootLoadKey;
	private SootInstanceG sootInstance;
	private IDataAccessor dataAccessor;
	
	private SootInstanceWrapper() {
		sootLoadKey = 0;
		sootInstance = new SootInstanceG();
		this.dataAccessor = null;
	}
	
	/**
	 * Sets the soot init indicator to the key value given which represents whatever
	 * soot loader was used to initialize soot.  If the indicator was already set
	 * then an exception is thrown.
	 * 
	 * @param key - the key value of a soot loader as an int
	 */
	public void setSootInit(int key){
		if(sootLoadKey != 0)
			throw new RuntimeException("Error: Cannot have two soot instances at the same time. Something is wrong.");
		this.sootLoadKey = key;
	}
	
	/**
	 * Determines if the soot init indicator for the given key has been set.
	 * 
	 * @param key - the key value of a soot loader as an int
	 * @return true if set false otherwise
	 */
	public boolean isSootInitSetTo(int key){
		return this.sootLoadKey == key;
	}
	
	/**
	 * Returns true if the soot init indicator has been set to some value. Useful 
	 * for determining if soot has been initialized at all when the exact type of 
	 * initialization does not matter.
	 * @return true if the init indicator is set
	 */
	public boolean isSootInitSet(){
		return this.sootLoadKey != 0;
	}
	
	/**
	 * Unsets the soot init indicator by setting its value back to 0. This method
	 * should be called before calling setSootInitValue if setSootInitValue has
	 * been called before.
	 */
	public void unsetSootInit(){
		this.sootLoadKey = 0;
	}
	
	public void setDataAccessor(IDataAccessor dataAccessor) {
		this.dataAccessor = dataAccessor;
	}
	
	@Override
	public SootInstanceG getG() {
		return sootInstance;
	}

	/* To start a new instance of soot we call G.reset() which
	 * calls this reset function here to create a new instance 
	 * of G. So anything that we are keeping global references
	 * to that depends on Soot structures can be reset here as
	 * well.
	 */
	@Override
	public void reset() {
		sootInstance = new SootInstanceG();
		SootInstanceWrapper.v().unsetSootInit();
		SootClassContainer.reset();
		SootMethodContainer.reset();
		SootFieldContainer.reset();
		SootUnitContainerFactory.reset();
		if(dataAccessor != null)
			dataAccessor.resetAllSootData(false);
	}
	
	//End instance fields and methods
	
	//Begin static fields and methods
	
	private static SootInstanceWrapper singleton;
	
	static{
		singleton = new SootInstanceWrapper();
		G.setGlobalObjectGetter(singleton);
	}
	
	/**
	 * Gets the current singleton instance of SootInstanceWrapper. If
	 * no instance currently exists then one is created.
	 * 
	 * @return the current singleton instance
	 */
	public static SootInstanceWrapper v(){
		return singleton;
	}
	
	//End static fields and methods

}
