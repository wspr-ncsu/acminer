package org.sag.acminer.database.defusegraph.id;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import soot.Unit;

public class IdentifierWrapper extends Identifier {
	
	private Identifier id;
	private int index;
	
	public IdentifierWrapper(Identifier id, int index) {
		this.id = id;
		this.index = index;
	}
	
	public int getIndex() {
		return index;
	}
	
	public Identifier getIdentifier() {
		return id;
	}
	
	@Override
	public int hashCode() {
		int i = 17;
		i = i * 31 + id.hashCode();
		i = i * 31 + index;
		return i;
	}

	@Override
	public boolean equals(Object o) {
		if(o == this)
			return true;
		if(o == null || !(o instanceof IdentifierWrapper))
			return false;
		IdentifierWrapper iw = (IdentifierWrapper)o;
		return Objects.equals(id, iw.id) && index == iw.index;
	}

	@Override
	public String toString() {
		return id.get(index).toString();
	}

	@Override
	public Iterator<Part> iterator() {
		return Collections.singletonList(id.get(index)).iterator();
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public boolean contains(Object o) {
		return id.get(index).equals(o);
	}

	@Override
	public Object[] toArray() {
		return Collections.singletonList(id.get(index)).toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return Collections.singletonList(id.get(index)).toArray(a);
	}

	@Override
	public boolean add(Part e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		if(c.size() == 1) {
			return contains(c.iterator().next());
		}
		return false;
	}

	@Override
	public boolean addAll(Collection<? extends Part> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(int index, Collection<? extends Part> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Part get(int index) {
		return id.get(this.index);
	}

	@Override
	public Part set(int index, Part element) {
		return id.set(this.index, element);
	}

	@Override
	public void add(int index, Part element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Part remove(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int indexOf(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int lastIndexOf(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ListIterator<Part> listIterator() {
		return Collections.singletonList(id.get(index)).listIterator();
	}

	@Override
	public ListIterator<Part> listIterator(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Part> subList(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void initSootComponents(Unit u) {
		super.initSootComponents(u);
	}
	
	@Override
	public Identifier clone() {
		return new IdentifierWrapper(id.clone(),index);
	}
	
}