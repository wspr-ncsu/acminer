package org.sag.common.tuple;


public final class Quad<A,B,C,D> extends Tuple{
	
	private static final Object[] toArray(Object first, Object second, Object third, Object fourth){
		Object[] data = {first, second, third, fourth};
		return data;
	}
	
	public Quad(A first, B second, C third, D fourth){
		super(toArray(first, second, third, fourth));
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
	
	@SuppressWarnings("unchecked")
	public D getFourth(){
		return (D)get(3);
	}
	
}