package org.sag.common.tuple;


public final class Triple<A,B,C> extends Tuple{
	
	private static final Object[] toArray(Object first, Object second, Object third){
		Object[] data = {first, second, third};
		return data;
	}
	
	public Triple(A first, B second, C third){
		super(toArray(first, second, third));
	}
	
	@SuppressWarnings("unchecked")
	public A getFirst(){
		return (A)get(0);
	}
	
	@SuppressWarnings("unchecked")
	public B getSecond(){
		return (B)get(1);
	}
	
	@SuppressWarnings("unchecked")
	public C getThird(){
		return (C)get(2);
	}
	
}