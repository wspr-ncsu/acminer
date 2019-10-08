package org.sag.common.tools;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

public class HashSetWithGet<E> extends AbstractSet<E> implements Set<E>, Cloneable{

	private transient HashMap<E,E> map;

	public HashSetWithGet() {
		map = new HashMap<>();
	}

	public HashSetWithGet(Collection<? extends E> c) {
		map = new HashMap<>(Math.max((int) (c.size()/.75f) + 1, 16));
		addAll(c);
	}

	public HashSetWithGet(int initialCapacity, float loadFactor) {
		map = new HashMap<>(initialCapacity, loadFactor);
	}

	public HashSetWithGet(int initialCapacity) {
		map = new HashMap<>(initialCapacity);
	}

	HashSetWithGet(int initialCapacity, float loadFactor, boolean dummy) {
		map = new LinkedHashMap<>(initialCapacity, loadFactor);
	}

	public Iterator<E> iterator() {
		return map.keySet().iterator();
	}

	public int size() {
		return map.size();
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public boolean contains(Object o) {
		return map.containsKey(o);
	}

	public boolean add(E e) {
		return map.put(e, e) == null;
	}

	public boolean remove(Object o) {
		return map.remove(o) != null;
	}

	public void clear() {
		map.clear();
	}
	
	public E get(E e){
		return map.get(e);
	}

	@SuppressWarnings("unchecked")
	public Object clone() {
		try {
			HashSetWithGet<E> newSet = (HashSetWithGet<E>) super.clone();
			newSet.map = (HashMap<E, E>) map.clone();
			return newSet;
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}

}
