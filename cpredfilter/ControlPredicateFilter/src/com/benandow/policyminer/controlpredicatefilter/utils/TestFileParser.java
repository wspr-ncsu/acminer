package com.benandow.policyminer.controlpredicatefilter.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestFileParser {

	private static boolean VERBOSE_OUTPUT = false;
	
	private static final Pattern methodRegex = Pattern.compile("^<(?<packageName>[a-zA-Z0-9_\\-\\$\\.<>\\[\\]]+):\\s+(?<returnType>[a-zA-Z0-9\\$_\\.<>\\[\\]]+)\\s+(?<methodName>[a-zA-Z0-9'\\$\\-_]+)\\((?<args>.*)\\)>$");
	
	private static final Pattern fieldRegex = Pattern.compile("^<(?<packageName>[a-zA-Z0-9_\\-\\$\\.<>\\[\\]]+):\\s+(?<fieldType>[a-zA-Z0-9\\$_\\.<>\\[\\]]+)\\s+(?<fieldName>[a-zA-Z0-9'_\\$\\-]+)>$");
	
	
	// -------------------- Parsers for parsing specific lines in files --------------------
	
	private static TriFunction<String, List<Method>, List<Field>, Boolean> lineParser1 = new TriFunction<String, List<Method>, List<Field>, Boolean> () {
		@Override
		public Boolean apply(String line, List<Method> methods, List<Field> fields) {
			//Remove comments
			line = line.replaceAll(">[\\s]*//.*$", ">");
			line = line.replaceAll(">\\s_-_\\sno\\sresolution[\\s]*$", ">").trim();

			Matcher methodMatch = methodRegex.matcher(line);
			if (methodMatch.matches()) {
				Method m = new Method(methodMatch.group("packageName"), methodMatch.group("returnType"), methodMatch.group("methodName"), methodMatch.group("args"));
				methods.add(m);
				return true;
			}
			
			Matcher fieldMatch = fieldRegex.matcher(line);
			if (fieldMatch.matches()) {
				Field f = new Field(fieldMatch.group("packageName"), fieldMatch.group("fieldType"), fieldMatch.group("fieldName"));
				fields.add(f);
				return true;
			}
			
			if (VERBOSE_OUTPUT) {
				System.err.println("Error parsing " + line);
			}
			return false;
		}
	};
	
	
	// -------------------- Methods for parsing initial filter info and context query files --------------------
	
	public static void readContextQueries(File file, List<Method> methods) throws IOException {
		parseFile(file, 
				new Function<String,Boolean>(){
					@Override
					public Boolean apply(String t) {
						return !t.startsWith("+") || t.length() < 1;
					}
				},
				new Function<String,String>(){
					public String apply(String t) {
						String res = t.replaceAll("^\\+\\s+MethodSignature\\s+", "");
						if (!res.startsWith("<")) {
							System.err.println("Error parsing method signature " + t);
							System.exit(1);
						}
						return res;
					}
				},
				lineParser1, methods, null);
	}
	
	public static void readFilterInfo(File file, List<Method> methods, List<Field> fields) throws IOException {
		parseFile(file, 
				new Function<String,Boolean>() {
					@Override
					public Boolean apply(String t) {
						return !t.startsWith("<") || t.length() < 1;
					}
				},
				new Function<String,String>() {
					@Override
					public String apply(String t) {
						String res = t.replaceAll("^\\+\\s+MethodSignature\\s+", "");
						if (!res.startsWith("<")) {
							System.err.println("Error parsing method signature " + t);
							System.exit(1);
						}
						return res;					}
				},
				lineParser1, methods, fields);
	}
	
	// -------------------- Methods for parsing VT test files --------------------
	
	public static void readVtMethods(File file, List<Method> methods) throws IOException {
		parseFile(file, 
				new Function<String,Boolean>() {
					@Override
					public Boolean apply(String t) {
						return !t.startsWith("Method: <");
					}
				},
				new Function<String,String>() {
					@Override
					public String apply(String t) {
						String res = t.replaceAll("^Method:\\s+", "");
						if (!res.startsWith("<")) {
							System.err.println("Error parsing method signature " + t);
							System.exit(1);
						}
						return res;	
					}
				}, TestFileParser.lineParser1, methods, null);
	}
	
	public static Method parseLine(String line) {
		return parseLineInternal(line,
		new Function<String,Boolean>() {
			@Override
			public Boolean apply(String t) {
				return !t.startsWith("<");
			}
		},
		new Function<String,String>() {
			@Override
			public String apply(String t) {
				String res = t.replaceAll("^Method:\\s+", "");
				if (!res.startsWith("<")) {
					System.err.println("Error parsing method signature " + t);
					System.exit(1);
				}
				return res;	
			}
		}, TestFileParser.lineParser1);
	}
	
	private static Method parseLineInternal(String line, Function<String,Boolean> conditionalCB,
			Function<String, String> processTxtCB, TriFunction<String, List<Method>, List<Field>, Boolean> lineParserCB) {
		List<Method> methods = new ArrayList<Method>();
		String result = line.trim();
		if (conditionalCB.apply(result)) {
			return null;
		}
		result = processTxtCB.apply(result);
		lineParserCB.apply(result, methods, null);
		return methods.get(0);
	}
	
	public static void readMethods(File file, List<Method> methods) throws IOException {
		parseFile(file, 
				new Function<String,Boolean>() {
					@Override
					public Boolean apply(String t) {
						return !t.startsWith("<");
					}
				},
				new Function<String,String>() {
					@Override
					public String apply(String t) {
						String res = t.replaceAll("^\\s+", "");
						if (!res.startsWith("<")) {
							System.err.println("Error parsing method signature " + t);
							System.exit(1);
						}
						return res;	
					}
				}, TestFileParser.lineParser1, methods, null);
	}
	
	public static void readVtStringConsts(File file, List<Method> methods, List<Field> fields) throws IOException {
		parseFile(file, 
				new Function<String,Boolean>() {
					@Override
					public Boolean apply(String t) {
						return !t.startsWith("StringConst:") || t.length() < 1;
					}
				},
				new Function<String,String>() {
					@Override
					public String apply(String t) {
						String res = t.replaceAll("^StrongConst:\\s+", "");
						if (!res.startsWith("\"")) {
							System.err.println("Error parsing method signature " + t);
							System.exit(1);
						}
						return res;
					}
				},
				new TriFunction<String, List<Method>, List<Field>, Boolean>() {
					@Override
					public Boolean apply(String line, List<Method> methods, List<Field> fields) {
						System.out.println(line);
						return true;
					} 
				}, methods, fields);
	}
	
	public static void readVtFields(File file, List<Field> fields) throws IOException {
		parseFile(file, 
				new Function<String,Boolean>() {
					@Override
					public Boolean apply(String t) {
						return !t.startsWith("Field: <");
					}
				},
				new Function<String,String>() {
					@Override
					public String apply(String t) {
						String res = t.replaceAll("^Field:\\s+", "");
						if (!res.startsWith("<")) {
							System.err.println("Error parsing method signature " + t);
							System.exit(1);
						}
						return res;	
					}
				}, TestFileParser.lineParser1, null, fields);
	}
	
	
	public static void readVtUses(File file, List<Method> methods, List<Field> fields) throws IOException {
		// Not implemented yet
	}
	
	// -------------------- Generic parser / file reader --------------------
	
	private static void parseFile(File file, Function<String,Boolean> conditionalCB,
			Function<String, String> processTxtCB, TriFunction<String, List<Method>, List<Field>, Boolean> lineParserCB, List<Method> methods, List<Field> fields) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line;
		while ((line = reader.readLine()) != null) {
			String result = line.trim();
			if (conditionalCB.apply(result)) {
				continue;
			}
			result = processTxtCB.apply(result);
			lineParserCB.apply(result, methods, fields);
		}
		reader.close();
	}	
}
