package com.benandow.policyminer.controlpredicatefilter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.benandow.policyminer.controlpredicatefilter.utils.Field;
import com.benandow.policyminer.controlpredicatefilter.utils.Method;
import com.benandow.policyminer.controlpredicatefilter.utils.MethodParseUtil;

public class Filter {

	private static boolean VERBOSE = false;
	
	private static final Pattern UID_REGEX = Pattern.compile("\\buid|user(\\s)?(id|handle)|(current|incoming|target|owner|source|calling)\\suser\\b");
	
	private static final Pattern UINFOHANDLE_REGEX = Pattern.compile("\\buser\\s(info|handle)\\b");
	private static final Pattern IDHANDLE_REGEX = Pattern.compile("\\b(handle|id(entifier)?|equals)\\b");

	private static final Pattern PID_REGEX = Pattern.compile("\\bpid|ppid|(parent|process)(\\s)?((p)?id)\\b");
	private static final Pattern GID_REGEX = Pattern.compile("\\bgid|group(\\s)?(id)\\b");

	private static final Pattern APPID_REGEX = Pattern.compile("\\bapp(\\s)?(id)\\b");
	private static final Pattern PACKAGENAME_REGEX = Pattern.compile("\\b(package|pkg)\\sname\\b");

	private static final Pattern PERMISSION_REGEX = Pattern.compile("\\bpermission(s)?\\b");

	//Component exported|name
	private static final Pattern COMPINFO_REGEX = Pattern.compile("\\b(activity|provider|component|service)\\s(info|name)\\b");
	private static final Pattern EXPORTED_REGEX = Pattern.compile("\\bexported|name\\b");

	//SystemProperties
	private static final Pattern SYSPROP_REGEX = Pattern.compile("\\bsystem\\sproperties\\b");
	private static final Pattern SYSPROP_GET_REGEX = Pattern.compile("\\bget\\b");
	//	"ro.factorytest","ro.test_harness","ro.debuggable","ro.secure"
	private static final Pattern BUILD_STR_REGEX = Pattern.compile("ro\\.(factorytest|test_harness|debuggable|secure)");
	
	//Intent Actions
	private static final Pattern INTENT_REGEX = Pattern.compile("\\bintent\\b");
	private static final Pattern INTENT_ACTION_REGEX = Pattern.compile("\\bget\\saction\\b");
	
	// Flags
	private static final Pattern FLAG_PKG_PATTERN = Pattern.compile("\\b(permission(\\sstate)?|package\\ssetting|layout\\sparams|display|"
			+ "(activity|application|provider|user|service|display|device)\\sinfo"
			+ ")\\b");
	private static final Pattern FLAG_PATTERN = Pattern.compile("\\b(flag(s)?|mode|name|handle|id(entifier)?|exported|match|is\\ssystem)\\b");
	
	//------------------------------------------------------------------------------------
	// The remainder only really catch 1-2 each.. Not sure if can be more generalized
	//FactoryTest mode -- FIXME this is a single match, can we generalize?
	private static final Pattern FACTORY_TEST_REGEX = Pattern.compile("\\bfactory\\stest\\b");
	private static final Pattern FACTORY_TEST_MODE = Pattern.compile("\\bget\\smode\\b");

	// Catches URIPermission source|target Package
	private static final Pattern PKG_REGEX2 = Pattern.compile("\\b(source|target)\\spkg\\b");

	// Process record
	private static final Pattern PROCESS_REC_REGEX1 = Pattern.compile("\\b(process\\sname|isolated)\\b");
	private static final Pattern PROCESS_REC_REGEX2 = Pattern.compile("\\bprocess\\srecord\\b");

	// Protected Packages
	private static final Pattern PROT_PKG_REGEX1 = Pattern.compile("\\bprotected\\spackages\\b");
	private static final Pattern PROT_PKG_REGEX2 = Pattern.compile("\\bowner\\spackage\\b");

	//Broadcast Record
	private static final Pattern BCAST_REC_REGEX1 = Pattern.compile("\\bbroadcast\\srecord\\b");
	private static final Pattern BCAST_REC_REGEX2 = Pattern.compile("\\bapp\\sop\\b");
	
	//DEBUG
	private static final Pattern DEBUG_REGEX = Pattern.compile("\\bis\\sdebuggable\\b");
	//------------------------------------------------------------------------------------

	// Intent strings REGEX
	private static final Pattern INTENT_STR_REGEX = Pattern.compile("^"
			+ "android\\.intent\\.action\\.(CLOSE_SYSTEM_DIALOGS|DISMISS_KEYBOARD_SHORTCUTS|MEDIA_BUTTON|MEDIA_SCANNER_SCAN_FILE|SHOW_KEYBOARD_SHORTCUTS|MASTER_CLEAR|ACTION_SHUTDOWN)|"
			+ "android\\.appwidget\\.action\\.(APPWIDGET_CONFIGURE|APPWIDGET_UPDATE)|"
			+ "android\\.location\\.HIGH_POWER_REQUEST_CHANGE|"
			+ "com\\.android\\.omadm\\.service\\.CONFIGURATION_UPDATE"
			+ "android\\.text\\.style\\.SUGGESTION_PICKED");
	
	private static final Pattern ANDROID_PERM_STR = Pattern.compile("^android\\.permission(\\-group)?\\..*");
	
	//------------------------------------------------------------------------------------

	private static boolean classNameMatches(String[] c, Pattern p) {
		for (int i = 1; i < c.length; i++) {
			if (p.matcher(c[i]).find()) {
				return true;
			}
		}
		return false;
	}
	
	private static boolean checkFilter(String fomPackage, String fomName) {
		String m = MethodParseUtil.parseMethodOrField(fomName);
		String[] p = MethodParseUtil.parseFullyQualifiedClassName(fomPackage);
		if (UID_REGEX.matcher(m).find() || (classNameMatches(p, UINFOHANDLE_REGEX) && IDHANDLE_REGEX.matcher(m).find())) {
			return true;
		} else if (PID_REGEX.matcher(m).find()) {
			return true;
		} else if (GID_REGEX.matcher(m).find()) {
			return true;
		} else if (APPID_REGEX.matcher(m).find()) {
			return true;
		} else if (PACKAGENAME_REGEX.matcher(m).find()) {
			return true;
		} else if (PERMISSION_REGEX.matcher(m).find()) {
			return true;
		} else if (Filter.classNameMatches(p, COMPINFO_REGEX) && EXPORTED_REGEX.matcher(m).find()) {
			return true;
		} else if (Filter.classNameMatches(p, SYSPROP_REGEX) && SYSPROP_GET_REGEX.matcher(m).find()) {
			return true;
		} else if (Filter.classNameMatches(p, INTENT_REGEX) && INTENT_ACTION_REGEX.matcher(m).find()) {
			return true;
		} else if (Filter.classNameMatches(p, FLAG_PKG_PATTERN) && FLAG_PATTERN.matcher(m).find()) {
			return true;
		} else if (Filter.classNameMatches(p, PERMISSION_REGEX) && PKG_REGEX2.matcher(m).find()) {
			return true;
		} else if (Filter.classNameMatches(p, PROCESS_REC_REGEX2) && PROCESS_REC_REGEX1.matcher(m).find()) {
			return true;
		} else if ( (Filter.classNameMatches(p, FACTORY_TEST_REGEX) && FACTORY_TEST_MODE.matcher(m).find()) || FACTORY_TEST_REGEX.matcher(m).find()) {
			return true;
		} else if (Filter.classNameMatches(p, PROT_PKG_REGEX1) && PROT_PKG_REGEX2.matcher(m).find()) {
			return true;
		} else if (Filter.classNameMatches(p, BCAST_REC_REGEX1) && BCAST_REC_REGEX2.matcher(m).find()) {
			return true;
		} else if (fomPackage.equals("android.os.Build") && DEBUG_REGEX.matcher(m).find()) {
			return true;
		}
		return false;
	}
	
	private static boolean checkFilterInternal(String packageName, String fomName) {
		if (Filter.checkFilter(packageName, fomName)) {
			return true;
		} else if (Filter.VERBOSE) {
			System.out.println(fomName+"\t\t"+packageName);
		}
		
		return Filter.checkFilter(packageName, fomName);
	}
	
	public static List<Method> filterMethods(List<Method> methods) {
		List<Method> result = new ArrayList<Method>();
		for (Method m : methods) {
			if (Filter.checkFilterInternal(m.getmPackage(), m.getName())) {
				result.add(m);
			}
		}
		return result;
	}
	
	public static List<Field> filterFields(List<Field> fields) {
		List<Field> result = new ArrayList<Field>();
		for (Field f : fields) {
			if (Filter.checkFilterInternal(f.getmPackage(), f.getName())) {
				result.add(f);
			}
		}
		return result;
	}

}