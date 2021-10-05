package org.sag.common.tools;

import java.util.Collection;
import java.util.Set;


public class LinkedHashSetWithGet<E> extends HashSetWithGet<E> implements Set<E>, Cloneable {

	public LinkedHashSetWithGet(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor, true);
	}

	public LinkedHashSetWithGet(int initialCapacity) {
		super(initialCapacity, .75f, true);
	}

	public LinkedHashSetWithGet() {
		super(16, .75f, true);
	}

	public LinkedHashSetWithGet(Collection<? extends E> c) {
		super(Math.max(2*c.size(), 11), .75f, true);
		addAll(c);
	}
}
