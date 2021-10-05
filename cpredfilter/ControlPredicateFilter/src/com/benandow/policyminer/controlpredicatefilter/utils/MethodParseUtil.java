package com.benandow.policyminer.controlpredicatefilter.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MethodParseUtil {

	private static final Pattern camelCasePattern = Pattern.compile(".+?(?:(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|$)");
	
	
	private static boolean isNumeric(String text) {
		try {
			Integer.parseInt(text);
		} catch (NullPointerException|NumberFormatException e) {
			return false;
		}
		return true;
	}
	
	public static String[] parseFullyQualifiedClassName(String name) {
		String[] splitName = name.split("\\.");
		String packageName = String.join(".", Arrays.copyOfRange(splitName, 0, splitName.length - 1));
		List<String> parsedClass = new ArrayList<String>();
		parsedClass.add(packageName);
		for (String s : splitName[splitName.length - 1].split("\\$")) {
			if (!MethodParseUtil.isNumeric(s) && s.length() > 0) {
				parsedClass.add(parseMethodOrField(s));
			}
		}
		return parsedClass.stream().toArray(String[]::new);
	}
	
	public static String parseMethodOrField(String name) {
		if (name.contains("_")) {
			return String.join(" ", name.split("_")).toLowerCase().trim();
		}
		return parseCamel(name);
	}
		
	private static String parseCamel(String name) {
		Matcher myMatcher = camelCasePattern.matcher(name);
		StringBuffer buf = new StringBuffer();
		while (myMatcher.find()) {
			buf.append(myMatcher.group(0).toLowerCase()).append(" ");
		}
		return buf.toString().trim();
	}
	
	
}
