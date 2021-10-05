package org.sag.acminer.database.acminer;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ACMinerQueryClient {
	
	public static void main(String[] args) {
		try {
			if(args.length > 0) {
				ACMinerQueryServer.start();
			} else {
				ACMinerQueryInterface stub = getServerStub();
				System.out.println(stub.query());
				stub.shutdown();
			}
		} catch(Throwable t) {
			t.printStackTrace(System.err);
			System.exit(-1);
		}
	}
	
	private static ACMinerQueryInterface getServerStub() throws RemoteException, NotBoundException {
		//Grabs a registry instance given the address and port of the machine with the registry running (should be the same machine as the server)
		Registry registry = LocateRegistry.getRegistry(ACMinerQueryInterface.REGISTRY_ADDRESS, ACMinerQueryInterface.REGISTRY_PORT);
		//Queries the registry for the server with the SERVER_NAME
		ACMinerQueryInterface stub = (ACMinerQueryInterface) registry.lookup(ACMinerQueryInterface.SERVER_NAME);
		return stub;
	}

}
