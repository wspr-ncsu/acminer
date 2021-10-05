package org.sag.acminer.database.acminer;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ACMinerQueryInterface extends Remote {
	
	public static final int REGISTRY_PORT = 6789;
	public static final String SERVER_NAME = "ACMinerQueryServer";
	public static final String REGISTRY_ADDRESS = "127.0.0.1";
	public static final int SERVER_PORT = 3487;
	
	public String query() throws RemoteException;
	public void shutdown() throws RemoteException;

}
