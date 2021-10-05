package org.sag.acminer.scripts;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.reference.DexBackedMethodReference;
import org.sag.common.io.FileHelpers;
import org.sag.common.io.PrintStreamUnixEOL;
import org.sag.common.io.archiver.SimpleArchiver;
import org.sag.common.tools.SortingMethods;
import org.sag.soot.SootSort;

import soot.Scene;
import soot.dexpler.Util;

public class FindMethodInDex {
	
	public static void main(String[] args) {
		if(args.length != 3) {
			throw new RuntimeException("Error: Not enough arguments.");
		}
		Path inputDir = FileHelpers.getPath(args[0]);
		Path sinksPath = FileHelpers.getPath(args[1]);
		Path outputPath = FileHelpers.getPath(args[2]);
		Set<String> sinks;
		Map<String,Set<Path>> data = new HashMap<>();
		
		try {
			sinks = getSinks(sinksPath);
		} catch(Throwable t) {
			throw new RuntimeException("Error: Failed to read in sinks.",t);
		}
		
		try {
			List<Path> files = FileHelpers.getAllDirectoryEntries(inputDir);
			for(Path p : files) {
				if(!Files.isDirectory(p)) {
					String ext = com.google.common.io.Files.getFileExtension(p.getFileName().toString());
					if(((ext.equals("apk") || ext.equals("jar")) && !SimpleArchiver.findInArchive(p, "/", "*.dex", null, "f", null, null).isEmpty()) 
							|| ext.equals("dex")) {
						Set<String> methods = getMethodReferences(p);
						for(String sink : sinks) {
							if(methods.contains(sink)) {
								Set<Path> s = data.get(sink);
								if(s == null) {
									s = new LinkedHashSet<>();
									data.put(sink, s);
								}
								s.add(p);
							}
						}
					}
				}
			}
			for(String sink : data.keySet()) {
				data.put(sink, SortingMethods.sortSet(data.get(sink)));
			}
			data = SortingMethods.sortMapKey(data, SootSort.smStringComp);
		} catch(Throwable t) {
			throw new RuntimeException("Error: Failed to parse all files in the input directory '" + inputDir + "'.",t);
		}
		
		try {
			FileHelpers.processDirectory(outputPath.getParent(), true, false);
		} catch(Throwable t) {
			throw new RuntimeException("Error: Failed to process directories to file '" + outputPath + "'.",t);
		}
		
		try(PrintStreamUnixEOL ps = new PrintStreamUnixEOL(Files.newOutputStream(outputPath))) {
			for(String sink : data.keySet()) {
				ps.println(sink);
				for(Path p : data.get(sink)) {
					ps.println("  " + p);
				}
			}
		} catch(Throwable t) {
			throw new RuntimeException("Error: Failed to write to file '" + outputPath + "'.",t);
		}
	}
	
	public static Set<String> getSinks(Path input) throws IOException {
		Set<String> ret = new HashSet<>();
		try(BufferedReader br = Files.newBufferedReader(input)) {
			String s;
			while((s = br.readLine()) != null) {
				s = s.trim();
				if(!s.isEmpty() && !s.startsWith("//")) {
					ret.add(s);
				}
			}
			return SortingMethods.sortSet(ret,SootSort.smStringComp);
		}
	}

	public static Set<String> getMethodReferences(Path filePath) throws IOException {
		DexBackedDexFile dexFile = DexFileFactory.loadDexFile(filePath.toFile(), null);
		List<DexBackedMethodReference> dexMethods = dexFile.getMethodSection();
		Set<String> ret = new HashSet<>();
		for(DexBackedMethodReference dexMethod : dexMethods) {
			String className = Util.dottedClassName(dexMethod.getDefiningClass());
			String name = dexMethod.getName();
			String returnType = toSoot(dexMethod.getReturnType());
			List<String> parameterTypes = new ArrayList<>();
			List<String> parms = dexMethod.getParameterTypes();
			if (parms != null && !parms.isEmpty()) {
				for (String t : parms)
					parameterTypes.add(toSoot(t));
			}
			ret.add(getSignature(className,name,parameterTypes,returnType));
		}
		return SortingMethods.sortSet(ret,SootSort.smStringComp);
	}
	
	public static String getSignature(String className, String name, List<String> params, String returnType) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("<");
		buffer.append(Scene.v().quotedNameOf(className));
		buffer.append(": ");
		buffer.append(getSubSignature(name, params, returnType));
		buffer.append(">");

		return buffer.toString();
	}
	
	private static String getSubSignature(String name, List<String> params, String returnType) {
		StringBuilder buffer = new StringBuilder();

		buffer.append(returnType);//Already quoted in toSoot method where needed

		buffer.append(" ");
		buffer.append(Scene.v().quotedNameOf(name));
		buffer.append("(");

		if (params != null) {
			for (int i = 0; i < params.size(); i++) {
				buffer.append(params.get(i));//Already quoted in toSoot method where needed
				if (i < params.size() - 1) {
					buffer.append(",");
				}
			}
		}
		buffer.append(")");

		return buffer.toString();
	}
	
	private static String toSoot(String type) {
		return toSoot(type, 0);
	}
	
	private static String toSoot(String typeDescriptor, int pos) {
		String type;
		char typeDesignator = typeDescriptor.charAt(pos);
		// see https://code.google.com/p/smali/wiki/TypesMethodsAndFields
		switch (typeDesignator) {
			case 'Z': // boolean
				type = "boolean";
				break;
			case 'B': // byte
				type = "byte";
				break;
			case 'S': // short
				type = "short";
				break;
			case 'C': // char
				type = "char";
				break;
			case 'I': // int
				type = "int";
				break;
			case 'J': // long
				type = "long";
				break;
			case 'F': // float
				type = "float";
				break;
			case 'D': // double
				type = "double";
				break;
			case 'L': // object
				//Quote here because we don't want to quote anything but reftypes and we only
				//know it is a reftype here
				type = Scene.v().quotedNameOf(Util.dottedClassName(typeDescriptor));
				break;
			case 'V': // void
				type = "void";
				break;
			case '[': // array
				type = toSoot(typeDescriptor, pos + 1) + "[]";
				break;
			default:
				type = "unknown";
		}
		return type;
	}
	
}
