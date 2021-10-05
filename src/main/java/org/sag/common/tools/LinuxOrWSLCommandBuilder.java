package org.sag.common.tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.sag.common.io.FileHelpers;

public final class LinuxOrWSLCommandBuilder {
	
	private int osid;
	private List<String> commands;
	
	public LinuxOrWSLCommandBuilder() throws Exception {
		osid = getOS();
		commands = new ArrayList<>();
		if(osid == 1) {
			testWSLExists();
			add("wsl");
		} else if(osid == 0) {
			throw new Exception("Unsupported operating system.");
		}
	}
	
	private static int getOS() {
		String os = System.getProperty("os.name").toLowerCase();
		if(os.startsWith("win")) {
			return 1;
		} else if(os.contains("linux")) {
			return 2;
		} else {
			return 0;
		}
	}
	
	private static String getWSLPath(Path path) throws Exception {
		String ret = path.toString().replaceAll("\\\\", "\\\\\\\\");
		try {
			ProcessBuilder pb = new ProcessBuilder("wsl", "wslpath", "-a", "\"" + ret + "\"");
			Process p = pb.start();
			
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while((line = br.readLine()) != null) {
				ret = line.trim();
			}
			
			int r = p.waitFor();
			if(r != 0 || ret.isEmpty() || !ret.startsWith("/")){
				throw new Exception("Failed to translate path '" + path + "' to a wsl path.");
			}
		} catch(Throwable t) {
			throw new Exception("Failed to translate path '" + path + "' to a wsl path.",t);
		}
		return ret;
	}
	
	public static void main(String[] args) throws Exception {
		LinuxOrWSLCommandBuilder cmdbuilder = new LinuxOrWSLCommandBuilder();
		cmdbuilder.addPath(FileHelpers.getPath("D:\\New folder\\extfstools\\bin\\ext2rd"));
		cmdbuilder.addPath(FileHelpers.getPath("D:\\New folder\\apex_payload2.img"));
		cmdbuilder.add("javalib/:" + cmdbuilder.transformPath(FileHelpers.getPath("D:\\New folder\\jar")));
		
		ProcessBuilder pb = new ProcessBuilder(cmdbuilder.getCommand());
		pb.redirectErrorStream(true);
		Process p = pb.start();
		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		StringBuilder sb = new StringBuilder();
		String line;
		while((line = br.readLine()) != null) {
			sb.append("\n    ").append(line.trim());
		}
		String msg = sb.toString();
		int r = p.waitFor();
		if(msg.trim().equals("exportdir: path not found")) {
			//Not an error if jarlib folder is not found in some apex img files
			System.out.println("No jarlib folder in img file");
		} else if(r != 0 || !msg.trim().isEmpty()) {
			System.out.println(msg);
		}
		
		/*ProcessBuilder pb = new ProcessBuilder(cmdbuilder.getCommand());
		Process p = pb.start();
		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		BufferedReader br2 = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		String line;
		while((line = br.readLine()) != null) {
			System.out.println("++" + line.trim());
		}
		String line2;
		while((line2 = br2.readLine()) != null) {
			System.out.println("--" + line2.trim());
		}
		int r = p.waitFor(); 
		if(r != 0) {
			System.out.println("Fail");
		}*/
	}
	
	private static void testWSLExists() throws Exception {
		try {
			ProcessBuilder pb = new ProcessBuilder("wsl", "true");
			Process p = pb.start();
			int r = p.waitFor();
			if(r != 0) {
				throw new Exception("Did not successfully execute the command 'wsl true'. Does wsl not exist on Windows?");
			}
		} catch(Throwable t) {
			throw new Exception("Failed to execute the command 'wsl true' on Windows.",t);
		}
	}
	
	public boolean add(String part) {
		return commands.add(part);
	}
	
	public String transformPath(Path path) throws Exception {
		String processedPath = path.toString();
		if(osid == 1)
			processedPath = getWSLPath(path);
		//Java's ProcessBuilder apparently passes strings in quotes to the command line when a space is detected
		//so no need to escape or quote file paths here
		return processedPath;
	}
	
	public boolean addPath(Path path) throws Exception {
		return add(transformPath(path));
	}
	
	public List<String> getCommand() {
		List<String> ret = new ArrayList<>(commands);
		commands.clear();
		if(osid == 1)
			commands.add("wsl");
		return ret;
	}

}
