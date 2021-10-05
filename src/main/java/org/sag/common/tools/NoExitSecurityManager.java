package org.sag.common.tools;

import java.security.Permission;

public class NoExitSecurityManager extends SecurityManager {
	
	private static boolean isSetup = false;
	private static SecurityManager prev = null;

	@Override
	public void checkPermission(Permission perm) {}
	@Override
	public void checkPermission(Permission perm, Object context) {}
	@Override
	public void checkExit(int status) {
		throw new ExitException(status);
	}
	
	public static class ExitException extends SecurityException {
		private static final long serialVersionUID = -5071134039976514046L;
		public final int status;
		public ExitException(int status) {
			super("Attempted to exit with status: " + status);
			this.status = status;
		}
	}
	
	public static void setup() {
		prev = System.getSecurityManager();
		isSetup = true;
		System.setSecurityManager(new NoExitSecurityManager());
	}

	public static void reset() {
		if(isSetup)
			System.setSecurityManager(prev);
	}
	
}
