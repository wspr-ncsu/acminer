package org.sag.acminer.scripts;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sag.common.io.FileHelpers;
import org.sag.common.io.PrintStreamUnixEOL;
import org.sag.common.tools.SortingMethods;
import org.sag.common.tuple.Pair;
import com.google.common.collect.ImmutableList;

public class DumpPackageInfo {
	
	private static final Pattern pkgLinePat = Pattern.compile("^package:([^=]+)=([^=]+)$");
	
	private Path out;
	
	public DumpPackageInfo(Path out) {
		this.out = out;
	}
	
	public void dumpPackageInfoToTXT() throws Exception {
		List<Pair<String,String>> pkgs = getPackages();
		for(Pair<String,String> p : pkgs) {
			dumpToTXTFile(p.getFirst(), p.getSecond());
		}
	}
	
	private List<Pair<String,String>> getPackages() throws Exception {
		List<Pair<String,String>> ret = new ArrayList<>();
		List<String> lines = runCmd("adb shell pm list packages -f",true);
		for(String line : lines) {
			Matcher m = pkgLinePat.matcher(line);
			if(m.matches()) {
				ret.add(new Pair<>(m.group(2), m.group(1)));
			}
		}
		return ret;
	}
	
	private void dumpToTXTFile(String pkgName, String pkgPath) throws Exception {
		List<String> lines = runCmd("adb shell dumpsys package " + pkgName, false);
		Path outFile = FileHelpers.getPath(out, pkgName + ".txt");
		try(PrintStreamUnixEOL pw = new PrintStreamUnixEOL(Files.newOutputStream(outFile))) {
			pw.println("pkgName: " + pkgName);
			pw.println("pkgPath: " + pkgPath);
			for(String line : lines) {
				pw.println(line.replaceAll("\\s+$", ""));
			}
		}
	}
	
	private static List<String> runCmd(String cmd, boolean trim) throws Exception {
		return runCmd(cmd.split("\\s+"), trim);
	}
	
	private static List<String> runCmd(String[] cmd, boolean trim) throws Exception {
		ProcessBuilder pb = new ProcessBuilder(cmd);
		Process p = pb.start();
		List<String> ret = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
			String line;
			while((line = br.readLine()) != null) {
				if(trim)
					ret.add(line.trim());
				else
					ret.add(line);
			}
		}
		return ret;
	}
	
	private static class PackageInfo {
		private static final Pattern pkgNamePat = Pattern.compile("^(\\s*)Package\\s+\\[([^\\]]+)\\].*$");
		private static final Pattern pkgOtherNamePat = Pattern.compile("^pkg=Package\\{[^\\s]+\\s+([^\\}\\s]+)\\}$");
		private boolean hidden = false;
		private List<String> body = null;
		private String spacer = null;
		private String pkgName = null;
		private String otherPkgName = null;
		private List<String> flags = null;
		private List<String> privateFlags = null;
		private List<String> pkgFlags = null;
		private String codePath = null;
		
		public void setHidden(boolean hidden) { this.hidden = hidden; }
		public List<String> getBody() { return body; }
		public List<String> getFlags() { return flags; }
		public List<String> getPrivateFlags() { return privateFlags; }
		public List<String> getPkgFlags() { return pkgFlags; }
		public String getCodePath() { return codePath; }
		public String getPackageName() { return pkgName; }
		public boolean isHidden() { return hidden; }
		
		public void addLine(String lineIn) {
			lineIn = lineIn.replaceAll("\\s+$", "");
			if(body == null) {
				body = new ArrayList<>();
				//First line should always be the 'Package [name] (hash):'
				Matcher m = pkgNamePat.matcher(lineIn);
				if(m.matches()) {
					spacer = m.group(1);
					pkgName = m.group(2);
				} else {
					throw new RuntimeException("Error: Failed to parse Package header for line '" + lineIn + "'.");
				}
			} else {
				String line = lineIn.trim();
				if(line.startsWith("pkg=")) {
					if(otherPkgName != null)
						throw new RuntimeException("Error: Duplicate entry '" + line + "' for " + pkgName);
					Matcher m = pkgOtherNamePat.matcher(line);
					if(m.matches()) {
						otherPkgName = m.group(1);
					} else {
						throw new RuntimeException("Error: Failed to match pattern for '" + line + "' of " + pkgName);
					}
				} else if(line.startsWith("codePath=")) {
					if(codePath != null)
						throw new RuntimeException("Error: Duplicate entry '" + line + "' for " + pkgName);
					codePath = line.substring(9);
				} else if(line.startsWith("flags=")) {
					if(flags != null)
						throw new RuntimeException("Error: Duplicate entry '" + line + "' for " + pkgName);
					line = line.substring(6);
					flags = parseFlags(line);
				} else if(line.startsWith("privateFlags=")) {
					if(privateFlags != null)
						throw new RuntimeException("Error: Duplicate entry '" + line + "' for " + pkgName);
					line = line.substring(13);
					privateFlags = parseFlags(line);
				} else if(line.startsWith("pkgFlags=")) {
					if(pkgFlags != null)
						throw new RuntimeException("Error: Duplicate entry '" + line + "' for " + pkgName);
					line = line.substring(9);
					pkgFlags = parseFlags(line);
				}
			}
			
			if(!spacer.isEmpty())
				lineIn = lineIn.replaceFirst(Pattern.quote(spacer), "");
			body.add(lineIn);
		}
		
		private List<String> parseFlags(String flags) {
			if(flags == null)
				return Collections.emptyList();
			List<String> ret = new ArrayList<>();
			for(String s : flags.split("\\s+")) {
				s = s.trim();
				if(!s.equals("[") && !s.equals("]") && !s.isEmpty())
					ret.add(s);
			}
			return ret;
		}
		
		public void verifyData() {
			if(pkgName == null)
				throw new RuntimeException("Error: The package name has not been set !?!");
			if(body == null)
				throw new RuntimeException("Error: There is no body for '" + pkgName + "'!?!");
			if(otherPkgName != null && !otherPkgName.equals(pkgName))
				throw new RuntimeException("Error: The pkg names '" + pkgName + "' and '" + otherPkgName + "' do not match.");
			if(codePath == null)
				throw new RuntimeException("Error: There is no codePath for '" + pkgName + "'!?!");
			flags = flags == null ? ImmutableList.of() : ImmutableList.copyOf(flags);
			privateFlags = privateFlags == null ? ImmutableList.of() : ImmutableList.copyOf(privateFlags);
			pkgFlags = pkgFlags == null ? ImmutableList.of() : ImmutableList.copyOf(pkgFlags);
			body = ImmutableList.copyOf(body);
		}
		
		public String getUniqueName(Map<String,AtomicInteger> counts) {
			String front = pkgName + (hidden ? "_hidden" : "");
			AtomicInteger i = counts.get(front);
			if(i == null) {
				i = new AtomicInteger();
				counts.put(front, i);
			}
			if(i.get() > 0) {
				front = front + "_" + i.get();
			}
			i.incrementAndGet();
			return front;
		}
	}
	
	private static Map<String,PackageInfo> getPackageInfo(Path in) throws Exception {
		Map<String,AtomicInteger> counts = new HashMap<>();
		Map<String,PackageInfo> data = new HashMap<>();
		List<Path> files = FileHelpers.getAllFilesInDirectory(in);
		for(Path file : files) {
			boolean hidden = false;
			PackageInfo pkg = null;
			boolean atLeastOneEntry = false;
			try(BufferedReader br = Files.newBufferedReader(file)) {
				String line;
				while((line = br.readLine()) != null) {
					if(line.startsWith("Packages:")) {
						atLeastOneEntry = handleBodyExit(pkg,hidden,data,counts) ? true : atLeastOneEntry;
						pkg = null;
						hidden = false;
					} else if(line.startsWith("Hidden system packages:")) {
						atLeastOneEntry = handleBodyExit(pkg,hidden,data,counts) ? true : atLeastOneEntry;
						pkg = null;
						hidden = true;
					} else if(line.matches("^\\s+Package\\s+.*$")) {
						pkg = new PackageInfo();
						pkg.addLine(line);
					} else if(line.trim().isEmpty()) {
						atLeastOneEntry = handleBodyExit(pkg,hidden,data,counts) ? true : atLeastOneEntry;
						pkg = null;
					} else if(pkg != null) {
						pkg.addLine(line);
					}
				}
				atLeastOneEntry = handleBodyExit(pkg,hidden,data,counts) ? true : atLeastOneEntry;
				pkg = null;
			}
			if(!atLeastOneEntry)
				System.out.println("Warning: No package info for " + file);
		}
		data = SortingMethods.sortMapKey(data, SortingMethods.sComp);
		return data;
	}
	
	private static boolean handleBodyExit(PackageInfo pkg, boolean hidden, Map<String,PackageInfo> data, Map<String,AtomicInteger> counts) {
		if(pkg != null) {
			pkg.setHidden(hidden);
			pkg.verifyData();
			String name = pkg.getUniqueName(counts);
			if(data.containsKey(name))
				throw new RuntimeException("Error: The unique name " + name + " is not unique!?!");
			data.put(name, pkg);
			return true;
		}
		return false;
	}
	
	public static void dumpPkgInfo(Map<String,PackageInfo> pkgInfo, Path out) throws Exception {
		out = FileHelpers.getPath(out, "packageinfo");
		FileHelpers.processDirectory(out, true, true);
		for(String name : pkgInfo.keySet()) {
			PackageInfo pkg = pkgInfo.get(name);
			try(PrintStream ps = new PrintStreamUnixEOL(Files.newOutputStream(FileHelpers.getPath(out, name + ".txt")))) {
				for(String line : pkg.getBody()) {
					ps.println(line);
				}
			}
		}
	}
	
	public static void dumpFlags(Map<String,PackageInfo> pkgInfo, Path out) throws Exception {
		try(PrintStream ps = new PrintStreamUnixEOL(Files.newOutputStream(FileHelpers.getPath(out, "flags_dump.txt")))) {
			for(String name : pkgInfo.keySet()) {
				PackageInfo pkg = pkgInfo.get(name);
				ps.println("Name: " + name + ":");
				ps.println("  PkgName: " + pkg.getPackageName());
				ps.println("  CodePath: " + pkg.getCodePath());
				ps.println("  Flags: " + pkg.getFlags());
				ps.println("  PrivateFlags: " + pkg.getPrivateFlags());
				ps.println("  PkgFlags: " + pkg.getPkgFlags());
				ps.println("  IsHidden: " + pkg.isHidden());
			}
		}
	}
	
	public static void findSystemFlag(Map<String,PackageInfo> pkgInfo, Path out) throws Exception {
		Map<String,PackageInfo> systemPackages = new LinkedHashMap<>();
		Map<String,PackageInfo> appPackages = new LinkedHashMap<>();
		for(String name : pkgInfo.keySet()) {
			PackageInfo pkg = pkgInfo.get(name);
			String codePath = pkg.getCodePath();
			if(codePath.startsWith("/vendor/overlay/") || codePath.startsWith("/system/framework/") || codePath.startsWith("/system/app/")
					|| codePath.startsWith("/system/priv-app/") || codePath.startsWith("/vendor/app/") || codePath.startsWith("/oem/app/")) {
				systemPackages.put(name, pkg);
			} else {
				appPackages.put(name, pkg);
			}
		}
		Map<String,PackageInfo> sysPkgSysFlags = new LinkedHashMap<>();
		Map<String,PackageInfo> sysPkgSysPkgFlags = new LinkedHashMap<>();
		Map<String,PackageInfo> sysPkgSysBoth = new LinkedHashMap<>();
		Map<String,PackageInfo> sysPkgSysNone = new LinkedHashMap<>();
		for(String name : systemPackages.keySet()) {
			PackageInfo pkg = systemPackages.get(name);
			boolean hasSysFlag = pkg.getFlags().contains("SYSTEM");
			boolean hasSysPkgFlag = pkg.getPkgFlags().contains("SYSTEM");
			if(hasSysFlag && hasSysPkgFlag) {
				sysPkgSysBoth.put(name, pkg);
			} else if(hasSysFlag) {
				sysPkgSysFlags.put(name, pkg);
			} else if(hasSysPkgFlag) {
				sysPkgSysPkgFlags.put(name, pkg);
			} else {
				sysPkgSysNone.put(name, pkg);
			}
		}
		Map<String,PackageInfo> appPkgSysFlags = new LinkedHashMap<>();
		Map<String,PackageInfo> appPkgSysPkgFlags = new LinkedHashMap<>();
		Map<String,PackageInfo> appPkgSysBoth = new LinkedHashMap<>();
		Map<String,PackageInfo> appPkgSysNone = new LinkedHashMap<>();
		for(String name : appPackages.keySet()) {
			PackageInfo pkg = appPackages.get(name);
			boolean hasSysFlag = pkg.getFlags().contains("SYSTEM");
			boolean hasSysPkgFlag = pkg.getPkgFlags().contains("SYSTEM");
			if(hasSysFlag && hasSysPkgFlag) {
				appPkgSysBoth.put(name, pkg);
			} else if(hasSysFlag) {
				appPkgSysFlags.put(name, pkg);
			} else if(hasSysPkgFlag) {
				appPkgSysPkgFlags.put(name, pkg);
			} else {
				appPkgSysNone.put(name, pkg);
			}
		}
		
		try(PrintStream ps = new PrintStreamUnixEOL(Files.newOutputStream(FileHelpers.getPath(out, "system_flags_info.txt")))) {
			ps.println("Total: " + pkgInfo.size());
			ps.println("  System Pkgs: " + systemPackages.size());
			ps.println("    Flags Has System: " + sysPkgSysFlags.size());
			ps.println("    PkgFlags Has System: " + sysPkgSysPkgFlags.size());
			ps.println("    Both Have System: " + sysPkgSysBoth.size());
			ps.println("    None Have System: " + sysPkgSysNone.size());
			ps.println("  App Pkgs: " + appPackages.size());
			ps.println("    Flags Has System: " + appPkgSysFlags.size());
			ps.println("    PkgFlags Has System: " + appPkgSysPkgFlags.size());
			ps.println("    Both Have System: " + appPkgSysBoth.size());
			ps.println("    None Have System: " + appPkgSysNone.size());
			
			ps.println("System Pkgs that have System Flag:");
			for(String name : sysPkgSysFlags.keySet())
				ps.println("  " + name);
			ps.println("System Pkgs that have System PkgFlag:");
			for(String name : sysPkgSysPkgFlags.keySet())
				ps.println("  " + name);
			ps.println("System Pkgs that have Both System Flag and PkgFlag:");
			for(String name : sysPkgSysBoth.keySet())
				ps.println("  " + name);
			ps.println("System Pkgs that have No System Flag or PkgFlag:");
			for(String name : sysPkgSysNone.keySet())
				ps.println("  " + name);
			
			ps.println("App Pkgs that have System Flag:");
			for(String name : appPkgSysFlags.keySet())
				ps.println("  " + name);
			ps.println("App Pkgs that have System PkgFlag:");
			for(String name : appPkgSysPkgFlags.keySet())
				ps.println("  " + name);
			ps.println("App Pkgs that have Both System Flag and PkgFlag:");
			for(String name : appPkgSysBoth.keySet())
				ps.println("  " + name);
			ps.println("App Pkgs that have No System Flag or PkgFlag:");
			for(String name : appPkgSysNone.keySet())
				ps.println("  " + name);
		}
	}
	
	public static void main(String[] args) {
		if(args.length <= 0) {
			throw new RuntimeException("Error: Please provide and output directory.");
		}
		//DumpPackageInfo di = new DumpPackageInfo(FileHelpers.getPath(args[0]));
		try {
			//di.dumpPackageInfoToTXT();
			Path out = FileHelpers.getPath(args[1]);
			Map<String,PackageInfo> pkgInfo = getPackageInfo(FileHelpers.getPath(args[0]));
			dumpPkgInfo(pkgInfo, out);
			dumpFlags(pkgInfo, out);
			findSystemFlag(pkgInfo, out);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
