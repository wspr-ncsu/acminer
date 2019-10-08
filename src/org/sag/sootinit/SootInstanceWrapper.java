package org.sag.sootinit;

import java.util.BitSet;

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

	private final BitSet initValues;
	private G sootInstance;
	private IDataAccessor dataAccessor;
	
	private SootInstanceWrapper() {
		initValues = new BitSet(SootLoadKey.values().length);
		sootInstance = new G();
		this.dataAccessor = null;
	}
	
	/**
	 * Sets a specific soot init value indicated by the key given. If another is 
	 * already set then an exception is thrown.
	 * 
	 * @param key - the key enum representing the index of the value to set
	 */
	public void setSootInitValue(SootLoadKey key){
		if(initValues.cardinality() != 0)
			throw new RuntimeException("Error: Cannot have two soot instances at the same time. Something is wrong.");
		initValues.set(key.getValue());
	}
	
	/**
	 * Determines if the soot init value for the given key has been set.
	 * 
	 * @param key - the key enum representing the index of the value to check
	 * @return true if set false otherwise
	 */
	public boolean isSootInitValueSet(SootLoadKey key){
		return initValues.get(key.getValue());
	}
	
	/**
	 * Returns true if any soot init value has been set. Useful for determining
	 * if soot has been initialized at all when the exact type of initialization
	 * does not matter.
	 * @return true is some init value was set
	 */
	public boolean isAnySootInitValueSet(){
		return initValues.cardinality() > 0;
	}
	
	/**
	 * Unsets all the soot init values. Should be run before trying to set
	 * any of the values as only one should be set at any given time.
	 */
	public void unsetAllSootInitValues(){
		initValues.clear();
	}
	
	public void setDataAccessor(IDataAccessor dataAccessor) {
		this.dataAccessor = dataAccessor;
	}
	
	@Override
	public G getG() {
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
		sootInstance = new G();
		SootInstanceWrapper.v().unsetAllSootInitValues();
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
	
	/**
	 * Enum objects where each enum's value represents an index
	 * into the bitset that indicates if soot has been initialized
	 * in that manner.
	 * 
	 * @author agorski
	 */
	public static enum SootLoadKey {
		BASIC(0),
		IPA(1),
		FRAMEWORK_DEX(2),
		APP_DEX(3),
		DEX(4),
		ALL_APP_DEX(6),
		API(7);
		
		private final int index;
		SootLoadKey(int index) { this.index = index; }
		public int getValue() { return index; }
	}
	
	//End static fields and methods

}
