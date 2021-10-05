package org.sag.common.concurrent;

/** This is an exception that is thrown to indicate to another party that a different exception has occurred. This
 * other exception has already been handled else where in some manner. However, another party needs to know that 
 * such an exception occurred (it does not care what) to continue. For instance, we may have a situation where
 * an exception occurs in a thread and is logged in the thread. However, some control flow later may need to know
 * if an error occurred inside the thread not caring what. This is the exception thrown in such an instance.
 */
public class IgnorableRuntimeException extends RuntimeException {
	private static final long serialVersionUID = -8891975612360885831L;

	public IgnorableRuntimeException() {}
	
}
