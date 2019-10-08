package com.benandow.policyminer.controlpredicatefilter.utils;

import java.util.Objects;
import java.util.function.Function;

//https://stackoverflow.com/questions/18400210/java-8-where-is-trifunction-and-kin-in-java-util-function-or-what-is-the-alt

@FunctionalInterface
public interface TriFunction<X,Y,Z,R> {

	 R apply(X x, Y y, Z z);

	 default <V> TriFunction<X, Y, Z, V> andThen( Function<? super R, ? extends V> after) {
	        Objects.requireNonNull(after);
	        return (X x, Y y, Z z) -> after.apply(apply(x, y, z));
	    }
	
}