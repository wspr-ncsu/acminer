package org.sag.common.concurrent;

public interface ValueRunner<A> extends Runnable {

	/** Gets the value that the runnable computed during execution. If an error occurred 
	 * during execution and execution did not complete or if execution never occurred, then
	 * the value returned is undefined.
	 */
	public A getValue();
	
}
