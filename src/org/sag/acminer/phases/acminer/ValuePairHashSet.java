package org.sag.acminer.phases.acminer;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class ValuePairHashSet extends AbstractSet<ValuePair> {
	
	private HashMap<ValuePair,ValuePair> map;
	
	public ValuePairHashSet() {
		this.map = new HashMap<>();
	}
	
	public ValuePairHashSet(Collection<ValuePair> in) {
		this();
		addAll(in);
	}
	
	protected ValuePairHashSet(boolean dummy) {
		this.map = new LinkedHashMap<>();
	}

	@Override
	public Iterator<ValuePair> iterator() {
		return map.values().iterator();
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}
	
	public boolean contains(Object o) {
		return map.containsKey(o);
	}
	
	public boolean remove(Object o) {
		return map.remove(o) != null;
	}
	
	public void clear() {
		map.clear();
	}
	
	public boolean add(ValuePair e) {
		if(e == null)
			throw new NullPointerException("Null elements are not supported");
		ValuePair cur = map.get(e);
		if(cur != null) {
			//Two ValuePair are equal iff there operators are equal (does not depend on the sources data)
			//So even with modified sources data for the map value, it should still be the same as the 
			//key with the original sources data, thus we can use the value to replace itself
			map.put(cur, ValuePair.make(cur,e.getSources()));
			return false;
		} else {
			map.put(e, e);
			return true;
		}
	}
	
	@SuppressWarnings("unchecked")
	public Object clone() {
		try {
			ValuePairHashSet newSet = (ValuePairHashSet) super.clone();
			newSet.map = (HashMap<ValuePair, ValuePair>) map.clone();
			return newSet;
		} catch (CloneNotSupportedException e) {
			throw new InternalError(e);
		}
	}
	
	/** Retrieves the ValuePair currently in the set that matches the given 
	 * value pair or returns null if no ValuePair matches the given pair.
	 */
	public ValuePair get(ValuePair in) {
		return map.get(in);
	}
	
	/** Returns the first elements in the set according to the iterator ordering
	 *  or null if the set is empty.
	 */
	public ValuePair getFirst() {
		if(map.size() == 0)
			return null;
		return iterator().next();
	}
	
	private static final ValuePairHashSet emptySet = new ValuePairHashSet();
	
	public static ValuePairHashSet getEmptySet() {
		return emptySet;
	}

}
