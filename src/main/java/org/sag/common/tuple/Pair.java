package org.sag.common.tuple;


public final class Pair<A,B> extends Tuple {
	
	private static final Object[] toArray(Object first, Object second){
		Object[] data = {first, second};
		return data;
	}
	
	public Pair(A first, B second){
		super(toArray(first, second));
	}
	
	@SuppressWarnings("unchecked")
	public A getFirst(){
		return (A)get(0);
	}
	
	@SuppressWarnings("unchecked")
	public B getSecond(){
		return (B)get(1);
	}
	
}