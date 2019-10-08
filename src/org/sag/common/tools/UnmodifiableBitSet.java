package org.sag.common.tools;

import java.util.BitSet;

/**
 * @author agorski
 *
 */
@SuppressWarnings("serial")
public class UnmodifiableBitSet extends BitSet{

	public UnmodifiableBitSet(){
		super();
	}
	
	public UnmodifiableBitSet(int size){
		super(size);
	}
	
	public UnmodifiableBitSet(BitSet bs){
		this(bs,bs.length());
	}
	
	public UnmodifiableBitSet(BitSet bs, int size){
		super(size);
		super.or(bs);
	}

	@Override
	public void flip(int bitIndex) {
		throw new UnsupportedOperationException("Cannot modify the UnmodifiableBitSet");
	}

	@Override
	public void flip(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException("Cannot modify the UnmodifiableBitSet");
	}

	@Override
	public void set(int bitIndex) {
		throw new UnsupportedOperationException("Cannot modify the UnmodifiableBitSet");
	}

	@Override
	public void set(int bitIndex, boolean value) {
		throw new UnsupportedOperationException("Cannot modify the UnmodifiableBitSet");
	}

	@Override
	public void set(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException("Cannot modify the UnmodifiableBitSet");
	}

	@Override
	public void set(int fromIndex, int toIndex, boolean value) {
		throw new UnsupportedOperationException("Cannot modify the UnmodifiableBitSet");
	}

	@Override
	public void clear(int bitIndex) {
		throw new UnsupportedOperationException("Cannot modify the UnmodifiableBitSet");
	}

	@Override
	public void clear(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException("Cannot modify the UnmodifiableBitSet");
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException("Cannot modify the UnmodifiableBitSet");
	}

	@Override
	public void and(BitSet set) {
		throw new UnsupportedOperationException("Cannot modify the UnmodifiableBitSet");
	}

	@Override
	public void or(BitSet set) {
		throw new UnsupportedOperationException("Cannot modify the UnmodifiableBitSet");
	}

	@Override
	public void xor(BitSet set) {
		throw new UnsupportedOperationException("Cannot modify the UnmodifiableBitSet");
	}

	@Override
	public void andNot(BitSet set) {
		throw new UnsupportedOperationException("Cannot modify the UnmodifiableBitSet");
	}
	
	/** Creates a new BitSet that is equal to the current object except
	 * it is not an UnmodifiableBitSet but a BitSet.
	 * @return a modifiable equivalent of the current object
	 */
	public BitSet makeMod(){
		BitSet bs = new BitSet(this.length());
		bs.or(this);
		return bs;
	}
	
	private void setBitInn(int i){
		super.set(i);
	}
	
	private void orBitSetInn(BitSet bs){
		super.or(bs);
	}
	
	/** Clones the current UnmodifiableBitSet, sets the ith bit to true in
	 * the new UnmodifiableBitSet, and returns the new UnmodifiableBitSet. 
	 * The current object remains unchanged.
	 * @param i index for but to set to true
	 * @return a new UnmodifiableBitSet with the ith bit set to true
	 */
	public UnmodifiableBitSet setBit(int i){
		UnmodifiableBitSet nbs = (UnmodifiableBitSet)this.clone();
		nbs.setBitInn(i);
		return nbs;
	}
	
	/** Clones the current UnmodifiableBitSet, ors the new UnmodifiableBitSet
	 * together with the argument BitSet, and returns the new UnmodifiableBitSet.
	 * The current object remains unchanged.
	 * @param bs BitSet to be ored with the current object
	 * @return a new UnmodifiableBitSet that is the union of the current object 
	 * and the argument
	 */
	public UnmodifiableBitSet orBitSet(BitSet bs){
		UnmodifiableBitSet nbs = (UnmodifiableBitSet)this.clone();
		nbs.orBitSetInn(bs);
		return nbs;
	}
	
}