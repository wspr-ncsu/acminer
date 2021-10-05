package org.sag.common.io.walker;

import java.util.concurrent.RecursiveAction;

public abstract class MTFileTreeWalkerTask extends RecursiveAction {

	private static final long serialVersionUID = -5457577199343858850L;

	public abstract String getIdentifier();
	
}
