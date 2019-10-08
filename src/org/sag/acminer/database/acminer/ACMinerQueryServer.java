package org.sag.acminer.database.acminer;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.sag.common.io.FileHelpers;

public class ACMinerQueryServer implements ACMinerQueryInterface {
	
	private static final SimpleDateFormat df = new SimpleDateFormat("yy-MM-dd HH:mm:ss.SSS");
	private final Registry registry;

	protected ACMinerQueryServer() throws RemoteException {
		//Creates a registry object on the local host that listens on the REGISTRY_PORT for incoming requests
		registry = LocateRegistry.createRegistry(REGISTRY_PORT);
		//Grabs the stub object that will be exported for this server by the RMI registry on the SERVER_PORT
		ACMinerQueryInterface stub = (ACMinerQueryInterface) UnicastRemoteObject.exportObject(this, SERVER_PORT);
		//Binds the stub server object to the SERVER_NAME in the RMI registry
		registry.rebind(SERVER_NAME, stub);
	}

	@Override
	public String query() throws RemoteException {
		return "Hellow World";
	}
	
	@Override
	public void shutdown() throws RemoteException {
		logMsg("Shutting Down Simple Miner Query Server");
		try {
			registry.unbind(SERVER_NAME);
			UnicastRemoteObject.unexportObject(this, true);
		} catch (Throwable t) {
			logMsg("Error in Shutting Down Simple Miner Query Server");
			t.printStackTrace(System.err);
			System.exit(-1);
		}
		logMsg("Successfully Shutdown Simple Miner Query Server");
	}
	
	private static void logErr(String msg) {
		System.err.println("[" + df.format(new Date()) + "] <INFO>:" + msg);
	}
	
	private static void logMsg(String msg) {
		System.out.println("[" + df.format(new Date()) + "] <INFO>:" + msg);
	}
	
	public static void start() throws IOException {
		String javaHome = System.getProperty("java.home");
		String javaPath = FileHelpers.getPath(javaHome, "bin/java").toString();
		String classPath = System.getProperty("java.class.path");
		String currentDirectory = System.getProperty("user.dir");
		String mainClass = ACMinerQueryServer.class.getName();
		List<String> args = new ArrayList<>();
		args.add(javaPath);
		args.add("-cp");
		args.add(classPath);
		args.add(mainClass);
		ProcessBuilder pb = new ProcessBuilder(args);
		pb.redirectErrorStream(true);//Redirects stderr to the stdout
		pb.redirectOutput(FileHelpers.getPath(currentDirectory, "simple_miner_query_server.log").toFile());//redirects stdout to a file in the working directory
		pb.start();//Starts the server process
	}
	
	public static void main(String[] args) {
		
		try {
			logMsg("Starting Simple Miner Query Server");
			new ACMinerQueryServer();
			logMsg("Successfully Started Simple Miner Query Server");
		} catch(Throwable t) {
			logErr("Failed to Start Simple Miner Query Server");
			t.printStackTrace(System.err);
			System.exit(-1);
		}
	}

}
