package org.sag.acminer.database.filter;

import java.util.HashSet;
import java.util.Set;

import org.sag.acminer.database.filter.entry.AndEntry;
import org.sag.acminer.database.filter.entry.KeepContextQueryMethodReturnValueUseEntry;
import org.sag.acminer.database.filter.entry.KeepFieldValueUseEntry;
import org.sag.acminer.database.filter.entry.KeepLoopHeaderEntry;
import org.sag.acminer.database.filter.entry.KeepMethodIsEntry;
import org.sag.acminer.database.filter.entry.KeepMethodReturnValueUseEntry;
import org.sag.acminer.database.filter.entry.KeepNumberConstantUseEntry;
import org.sag.acminer.database.filter.entry.KeepSourceMethodIsEntry;
import org.sag.acminer.database.filter.entry.KeepSourceMethodIsInContextQuerySubGraphEntry;
import org.sag.acminer.database.filter.entry.NotEntry;
import org.sag.acminer.database.filter.entry.OrEntry;
import org.sag.acminer.database.filter.matcher.FieldMatcher;
import org.sag.acminer.database.filter.matcher.Matcher;
import org.sag.acminer.database.filter.matcher.MethodMatcher;
import org.sag.acminer.database.filter.matcher.NumberMatcher;
import org.sag.acminer.database.filter.matcher.SootMatcher;
import org.sag.acminer.database.filter.matcher.StringMatcher;
import org.sag.acminer.database.filter.matcher.TypeMatcher;
import org.sag.acminer.database.filter.matcher.Matcher.Op;
import org.sag.acminer.database.filter.restrict.IsDeclaringClassOfMethodRestriction;
import org.sag.acminer.database.filter.restrict.IsFieldTypeRestriction;
import org.sag.acminer.database.filter.restrict.IsFieldUsedDirectlyInRestriction;
import org.sag.acminer.database.filter.restrict.IsInArithmeticChainRestriction;
import org.sag.acminer.database.filter.restrict.IsInArithmeticOpRestriction;
import org.sag.acminer.database.filter.restrict.IsMethodReturnTypeRestriction;
import org.sag.acminer.database.filter.restrict.IsNumberUsedRestriction;
import org.sag.acminer.database.filter.restrict.IsValueUsedInMethodCallRestriction;
import org.sag.acminer.database.filter.restrict.Restrictions;

import soot.IntType;

public class Generator {
	
	public static void main(String[] args) {
		//genContextQueriesFilter();
		genFilter();
		//genOrgFilter();
		
		/*Op<String> op = 
				Matcher.getAndOp(
						SootMatcher.getNameRegexWordsOp("\\b(flag(s)?)|(protection\\slevel)\\b"),
						SootMatcher.getClassRegexWordsOp("\\b(((uri|base)\\spermission)|((package|application)\\smanager\\sservice)|permission\\s(state|data)|"
								+ "package\\ssetting|layout\\sparams|display|(activity|application|provider|user|service|display|device)\\sinfo)|setting\\sbase\\b",0));
		String[] arr = {
"<com.android.server.pm.BasePermission: int protectionLevel>",
"<<com.android.server.pm.SettingBase: int pkgFlags>"
		};
		
		//Pattern p = Pattern.compile("^<([^:]+): [^ ]+ ([^\\(]+)\\([^>]+>$");
		Pattern p = Pattern.compile("^<([^:]+): [^ ]+ ([^>]+)>$");
		for(String sig : arr) {
			java.util.regex.Matcher m = p.matcher(sig);
			if(m.matches()) {
				String fullClassName = m.group(1);
				String methodName = m.group(2);
				String shortName = fullClassName;
				String packageName = "";
				int index = fullClassName.lastIndexOf('.');
				if (index > 0) {
					shortName = fullClassName.substring(index + 1);
					packageName = fullClassName.substring(0, index);
				}
				boolean matches = op.matches(sig,methodName,packageName,shortName,fullClassName);
				System.out.println(matches + " : " + sig);
			} else {
				throw new RuntimeException("Error parsing " + sig);
			}
		}*/
	}
	
	public static void genContextQueriesFilter() {
		OrEntry baseEntry = new OrEntry();
		
		Op<String> limit = SootMatcher.getPackageStartsWithOrOp("android.","com.android.");
		
		baseEntry.addEntry(new KeepMethodIsEntry(
				Matcher.getAndOp(limit,SootMatcher.getPackageContainOp("firewall"),SootMatcher.getNameRegexWordsOp("\\bmatch(es|ed)?\\b"))));
		baseEntry.addEntry(new KeepMethodIsEntry(
				Matcher.getAndOp(limit,SootMatcher.getClassContainOp("AppOps"),
				SootMatcher.getNameRegexWordsOp("^((note|start)\\s(proxy\\s)?op(eration)?(s)?)|((proxy\\s)?op(eration)?(s)?\\sallow)\\b"))));
		baseEntry.addEntry(new KeepMethodIsEntry(
				Matcher.getAndOp(limit,SootMatcher.getClassEqualOp("LockPatternUtils"),
				SootMatcher.getNameRegexWordsOp("\\b(password|pattern)\\sexists\\b"))));
		
		baseEntry.addEntry(new KeepMethodIsEntry(SootMatcher.getSignatureOrOp(
				"<com.android.server.am.ActivityManagerService: boolean processSanityChecksLocked(com.android.server.am.ProcessRecord)>",
				"<com.android.server.am.ActivityManagerService: boolean matchesProvider(android.net.Uri,android.content.pm.ProviderInfo)>",
				"<com.android.server.am.ActivityManagerService: boolean isSingleton(java.lang.String,android.content.pm.ApplicationInfo,java.lang.String,int)>",
				"<android.content.pm.PackageParser$Package: boolean isMatch(int)>",
				"<com.android.server.pm.PackageSetting: boolean isMatch(int)>",
				"<com.android.server.pm.PermissionsState$PermissionData: boolean isDefault()>",
				"<com.android.server.pm.PermissionsState$PermissionState: boolean isDefault()>",
				"<com.android.server.am.UserController: int unsafeConvertIncomingUserLocked(int)>",
				"<com.android.server.am.UserController: int getCurrentOrTargetUserIdLocked()>",
				"<com.android.server.job.controllers.JobStatus: boolean shouldDump(int)>",
				"<android.app.admin.DevicePolicyManager: void throwIfParentInstance(java.lang.String)>",
				"<com.android.server.notification.ManagedServices$ManagedServiceInfo: boolean enabledAndUserMatches(int)>",
				"<com.android.server.am.UriPermission: int getStrength(int)>",
				"<com.android.server.am.BatteryStatsService: boolean onlyCaller(int[])>",
				"<com.android.server.LocationManagerService: boolean doesUidHavePackage(int,java.lang.String)>",
				"<com.android.server.LocationManagerService: boolean reportLocationAccessNoThrow(int,int,java.lang.String,int)>",
				"<com.android.server.pm.UserManagerService: boolean exists(int)>",
				"<android.app.admin.DeviceAdminInfo: boolean usesPolicy(int)>",
				"<com.android.server.notification.ManagedServices$ManagedServiceInfo: boolean supportsProfiles()>"
				)));
		
		baseEntry.addEntry(new KeepMethodIsEntry(Matcher.getAndOp(limit,SootMatcher.getNameRegexWordsOp("^(check|enforce|has|ensure)\\s([a-z\\s]+\\s)?permission(s)?"))));
		baseEntry.addEntry(new KeepMethodIsEntry(Matcher.getAndOp(limit,SootMatcher.getNameRegexWordsOp("^permission\\s(to\\sop\\scode|info\\sfootprint|is\\sgranted)$"))));
		baseEntry.addEntry(new KeepMethodIsEntry(Matcher.getAndOp(limit,SootMatcher.getNameRegexWordsOp("^(find\\spermission\\stree\\slp|calculate\\scurrent\\spermission\\sfootprint\\slocked)$"))));
		baseEntry.addEntry(new KeepMethodIsEntry(Matcher.getAndOp(limit,SootMatcher.getNameRegexWordsOp("^(check|enforce|compare|verify)\\s(uid\\s)?signatures"))));
		baseEntry.addEntry(new KeepMethodIsEntry(Matcher.getAndOp(limit,SootMatcher.getNameRegexWordsOp("^(check|enforce|is|ensure)\\s([a-z\\s]+\\s)?package(s)?"))));
		baseEntry.addEntry(new KeepMethodIsEntry(Matcher.getAndOp(limit,SootMatcher.getNameRegexWordsOp("^is\\s(calling\\s)?package\\sallowed\\sto\\s[a-z\\s]+"))));
		baseEntry.addEntry(new KeepMethodIsEntry(Matcher.getAndOp(limit,SootMatcher.getNameRegexWordsOp("^(is|check|enforce)\\s([a-z\\s]+\\s)?system"))));
		baseEntry.addEntry(new KeepMethodIsEntry(Matcher.getAndOp(limit,SootMatcher.getNameRegexWordsOp("^(is|get|enforce)\\s((active|user)\\s)?admin"))));
		baseEntry.addEntry(new KeepMethodIsEntry(Matcher.getAndOp(limit,SootMatcher.getNameRegexWordsOp("^(is|has)\\s((caller\\s)?(device|profile)\\s)?owner"))));
		baseEntry.addEntry(new KeepMethodIsEntry(Matcher.getAndOp(limit,SootMatcher.getNameRegexWordsOp("^(enforce|check|ensure)\\s([a-z\\s]+\\s)?((device|profile)\\s)?owner"))));
		baseEntry.addEntry(new KeepMethodIsEntry(Matcher.getAndOp(limit,SootMatcher.getNameRegexWordsOp("^get\\scross\\sprofile\\s[a-z\\s]+\\s(disabled|enabled)"))));
		baseEntry.addEntry(new KeepMethodIsEntry(Matcher.getAndOp(limit,SootMatcher.getNameRegexWordsOp("^(check|enforce|verify)\\b"))));
		baseEntry.addEntry(new KeepMethodIsEntry(Matcher.getAndOp(limit,SootMatcher.getNameRegexWordsOp("\\bis\\s([a-z\\s+]+\\s)?(protected|isolated|permi(tted|ssion(s)?)|privileged|managed|secure|(un)?locked|restrict(ed|ion)|granted|(dis)?allowed|(en|dis)abled|(in)?visible|password|lock\\spattern|cert\\sinstaller|test\\sharness|app(lication)?(s)?|user(\\sid)?|uid|profile|security\\sviolation)"))));
		baseEntry.addEntry(new KeepMethodIsEntry(Matcher.getAndOp(limit,SootMatcher.getNameRegexWordsOp("^has\\s(access|granted|([a-z\\s]+\\s)?(restriction|privilege|permission)(s)?|[a-z\\s]+\\sinstalled)"))));
		baseEntry.addEntry(new KeepMethodIsEntry(Matcher.getAndOp(limit,SootMatcher.getNameRegexWordsOp("^get\\s([a-z\\s]+\\s)?(credentials|privilege)"))));
		baseEntry.addEntry(new KeepMethodIsEntry(Matcher.getAndOp(limit,SootMatcher.getNameRegexWordsOp("^get\\s(app\\sstart\\smode|(bluetooth|camera|screen)\\s([a-z\\s]+\\s)?(disabled|enabled)|([a-z\\s]+\\s)?resolution\\slevel|[a-z\\s]+\\sfor\\scalling\\spackage)"))));
		baseEntry.addEntry(new KeepMethodIsEntry(Matcher.getAndOp(limit,SootMatcher.getNameRegexWordsOp("\\b(can)\\s([a-z\\s]+\\s)?(read|execute|run|write|clear|modify|access|draw|display|project)\\b"))));
		baseEntry.addEntry(new KeepMethodIsEntry(Matcher.getAndOp(limit,SootMatcher.getNameRegexWordsOp("^handle\\s([a-z\\s]+\\s)?user"))));
		baseEntry.addEntry(new KeepMethodIsEntry(Matcher.getAndOp(limit,SootMatcher.getNameRegexWordsOp("\\bvalid(ate)?\\s([a-z\\s]+\\s)?(user|call|package)\\b"))));
		
		ContextQueriesDescriptorDatabase c = new ContextQueriesDescriptorDatabase(baseEntry);
		
		try {
			c.writeXML("context_queries_descriptor_db.xml", null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void genFilter() {
		AndEntry baseEntry = new AndEntry();
		
		Set<String> loopConditionalMethods = new HashSet<>();
		loopConditionalMethods.add("<java.util.Collection: int size()>");
		loopConditionalMethods.add("<java.util.Iterator: boolean hasNext()>");
		loopConditionalMethods.add("<java.util.Collection: boolean isEmpty()>");
		loopConditionalMethods.add("<java.util.Map: boolean isEmpty()>");
		loopConditionalMethods.add("<java.util.Map: int size()>");
		loopConditionalMethods.add("<android.util.SparseArray: int size()>");
		loopConditionalMethods.add("<android.util.SparseBooleanArray: int size()>");
		loopConditionalMethods.add("<android.util.SparseIntArray: int size()>");
		loopConditionalMethods.add("<java.lang.CharSequence: int length()>");
		loopConditionalMethods.add("<android.app.usage.TimeSparseArray: int size()>");
		loopConditionalMethods.add("<com.android.internal.util.ArrayUtils: boolean isEmpty(int[])>");
		loopConditionalMethods.add("<com.android.internal.util.ArrayUtils: boolean isEmpty(java.lang.Object[])>");
		loopConditionalMethods.add("<com.android.internal.util.ArrayUtils: boolean isEmpty(java.util.Collection)>");
		loopConditionalMethods.add("<android.content.res.XmlBlock$Parser: int next()>");
		loopConditionalMethods.add("<org.kxml2.io.KXmlParser: int next()>");
		loopConditionalMethods.add("<org.kxml2.io.KXmlParser: int getDepth()>");
		loopConditionalMethods.add("<android.content.res.XmlBlock$Parser: int getDepth()>");
		loopConditionalMethods.add("<java.io.InputStream: int read()>");
		loopConditionalMethods.add("<java.io.InputStream: int read(byte[])>");
		loopConditionalMethods.add("<java.io.InputStream: int read(byte[], int, int)>");
		loopConditionalMethods.add("<java.io.InputStream: long skip(long)>");
		loopConditionalMethods.add("<java.util.ListIterator: boolean hasPrevious()>");
		baseEntry.addEntry(new NotEntry(new KeepLoopHeaderEntry(true,true,loopConditionalMethods))); //Remove all loop headers
		baseEntry.addEntry(new NotEntry(new KeepSourceMethodIsEntry(SootMatcher.SootMatcherOpType.EQUAL_NAME, "<clinit>")));//Remove all cp in the static initializers
		baseEntry.addEntry(new NotEntry(new KeepSourceMethodIsEntry(SootMatcher.SootMatcherOpType.EQUAL_NAME, "<init>")));//Remove all cp in object initializers
		
		OrEntry subEntry = new OrEntry();
		baseEntry.addEntry(subEntry);
		
		//Keep all cp whose source method is a context query
		subEntry.addEntry(new KeepSourceMethodIsInContextQuerySubGraphEntry(true));
		
		IsInArithmeticChainRestriction arcRes = new IsInArithmeticChainRestriction();
		Restrictions arcResSingle = new Restrictions(arcRes);
		IsInArithmeticChainRestriction arcResConst = new IsInArithmeticChainRestriction(true);
		Restrictions arcResConstSingle = new Restrictions(arcResConst);
		
		//Keep any cp that uses a context query's return value in a arithmetic chain somehow
		subEntry.addEntry(new KeepContextQueryMethodReturnValueUseEntry(arcRes));
		
		//Uid and UserId
		subEntry.addEntry(new KeepMethodReturnValueUseEntry(
		Matcher.getOrOp(SootMatcher.getNameRegexWordsOp("\\buid|user(\\s)?(id|handle)|(current|incoming|target|owner|source|calling)\\suser|profile\\sparent\\b"),
				Matcher.getAndOp(SootMatcher.getNameRegexWordsOp("\\b(handle|id(entifier)?|equals)\\b"),
				SootMatcher.getClassRegexWordsOp("\\buser\\s(info|handle)\\b", 0))),arcResSingle));
		subEntry.addEntry(new KeepFieldValueUseEntry(
		Matcher.getOrOp(SootMatcher.getNameRegexWordsOp("\\buid|user(\\s)?(id|handle)|(current|incoming|target|owner|source|calling)\\suser\\b"),
				Matcher.getAndOp(SootMatcher.getNameRegexWordsOp("\\b(handle|id(entifier)?|equals)\\b"),
				SootMatcher.getClassRegexWordsOp("\\buser\\s(info|handle)\\b", 0))),arcResSingle));
		//For the UserId value -10000 which is supposed to indicate NULL
		subEntry.addEntry(new KeepNumberConstantUseEntry(-10000, arcResConstSingle));
		
		//Pid
		subEntry.addEntry(new KeepMethodReturnValueUseEntry(SootMatcher.getNameRegexWordsOp("\\bpid|ppid|(parent|process)(\\s)?((p)?id)\\b"),arcResSingle));
		subEntry.addEntry(new KeepFieldValueUseEntry(SootMatcher.getNameRegexWordsOp("\\bpid|ppid|(parent|process)(\\s)?((p)?id)\\b"),arcResSingle));
	
		//Gid
		subEntry.addEntry(new KeepMethodReturnValueUseEntry(SootMatcher.getNameRegexWordsOp("\\bgid|group(\\s)?(id)\\b"),arcResSingle));
		subEntry.addEntry(new KeepFieldValueUseEntry(SootMatcher.getNameRegexWordsOp("\\bgid|group(\\s)?(id)\\b"),arcResSingle));
		subEntry.addEntry(new KeepNumberConstantUseEntry(NumberMatcher.getNumberOrOp(1023,1032,9997), arcResConstSingle));
		
		//AppId
		subEntry.addEntry(new KeepMethodReturnValueUseEntry(SootMatcher.getNameRegexWordsOp("\\bapp(\\s)?(id)\\b"),arcResSingle));
		subEntry.addEntry(new KeepFieldValueUseEntry(SootMatcher.getNameRegexWordsOp("\\bapp(\\s)?(id)\\b"),arcResSingle));
		subEntry.addEntry(new KeepNumberConstantUseEntry(NumberMatcher.getNumberOrOp(1000, 1001, 2000, 1007, 1010, 1013, 1019, 1016, 1027, 1002, 1037, 1041, 1047), arcResConstSingle));
	
		//Package Name checks
		
		//Methods for accessing package names
		MethodMatcher pkgNameMMatch = new MethodMatcher(Matcher.getOrOp(
				SootMatcher.getNameRegexWordsOp("\\b((source|target|owner)\\s(package|pkg))|((package|pkg|process)\\s(name(s)?|list))|(packages\\s\\w+\\suid)|(get\\stypes\\s[\\w\\s]+\\scaller)\\b"),
				Matcher.getAndOp(SootMatcher.getClassEqualOp("ComponentName"), SootMatcher.getNameEqualOp("getClassName"))
				));
		//Fields that store packag names
		FieldMatcher pkgNameFMatch = new FieldMatcher(Matcher.getOrOp(
				SootMatcher.getNameRegexWordsOp("\\b((source|target|owner)\\s(package|pkg))|((package|pkg|process)\\s(name(s)?|list))|(packages\\s\\w+\\suid)\\b"),
				Matcher.getAndOp(SootMatcher.getClassEqualOp("ActivityInfo"), SootMatcher.getNameEqualOp("name"))
				));
		
		//Or restriction for equals package name that allows any that use a field or method listed at either equals position or the special cond.
		Restrictions equalsRes = new Restrictions(true, new IsValueUsedInMethodCallRestriction(-1,pkgNameMMatch), 
				new IsValueUsedInMethodCallRestriction(0, pkgNameMMatch), new IsValueUsedInMethodCallRestriction(-1, pkgNameFMatch), 
				new IsValueUsedInMethodCallRestriction(0, pkgNameFMatch));
		
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("equals", SootMatcher.SootMatcherOpType.EQUAL_NAME, arcRes, equalsRes));
		
		//Handles the situation where contains is called on a list created by Arrays asList which is used on an array of the three package name arrs
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("contains", SootMatcher.SootMatcherOpType.EQUAL_NAME, arcRes, 
				new IsDeclaringClassOfMethodRestriction("java.util.Collection", true), 
				new Restrictions(true, 
					new IsValueUsedInMethodCallRestriction(-1, new MethodMatcher(SootMatcher.SootMatcherOpType.SIGNATURE, "<java.util.Arrays: java.util.List asList(java.lang.Object[])>"), 
						new IsValueUsedInMethodCallRestriction(0, pkgNameFMatch),
						new IsValueUsedInMethodCallRestriction(0, pkgNameMMatch)),
					new IsValueUsedInMethodCallRestriction(-1, new FieldMatcher(SootMatcher.getNameRegexWordsOp("\\b(enabled\\sservices\\s(package\\snames|\\w+\\scurrent\\sprofiles))\\b"))),
					new IsValueUsedInMethodCallRestriction(0, pkgNameFMatch),
					new IsValueUsedInMethodCallRestriction(-1, pkgNameFMatch),
					new IsValueUsedInMethodCallRestriction(0, pkgNameMMatch),
					new IsValueUsedInMethodCallRestriction(-1, pkgNameMMatch)
				)
		));
		
		//Handles the situation where a custom android contains method is used to check if one of the package arrays contains a specific name
		subEntry.addEntry(new KeepMethodReturnValueUseEntry(
				"<com.android.internal.util.ArrayUtils: boolean contains(java.lang.Object[],java.lang.Object)>", SootMatcher.SootMatcherOpType.SIGNATURE, 
				new Restrictions(true, 
						new IsValueUsedInMethodCallRestriction(0, pkgNameFMatch), 
						new IsValueUsedInMethodCallRestriction(0, pkgNameMMatch),
						new IsValueUsedInMethodCallRestriction(1, pkgNameFMatch), 
						new IsValueUsedInMethodCallRestriction(1, pkgNameMMatch)
		)));
		
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("containsKey", SootMatcher.SootMatcherOpType.EQUAL_NAME, arcRes, 
				new IsDeclaringClassOfMethodRestriction("java.util.Map", true), new Restrictions(true, 
						new IsValueUsedInMethodCallRestriction(-1, pkgNameFMatch),
						new IsValueUsedInMethodCallRestriction(-1, pkgNameMMatch),
						new IsValueUsedInMethodCallRestriction(0, pkgNameFMatch),
						new IsValueUsedInMethodCallRestriction(0, pkgNameMMatch)
		)));
		
		//SystemProperties
		IsValueUsedInMethodCallRestriction sysPropValues = new IsValueUsedInMethodCallRestriction(0, 
				new StringMatcher(StringMatcher.getRegexOp("ro\\.(factorytest|test_harness|debuggable|secure)")));
		Op<String> mmgetop = Matcher.getAndOp(
				SootMatcher.getClassRegexWordsOp("\\bsystem\\sproperties\\b",0),SootMatcher.getNameRegexWordsOp("\\bget\\b"));
		MethodMatcher mmget = new MethodMatcher(mmgetop);
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("equals", SootMatcher.SootMatcherOpType.EQUAL_NAME, arcRes, new Restrictions(true,
				new IsValueUsedInMethodCallRestriction(-1, mmget, sysPropValues), new IsValueUsedInMethodCallRestriction(0, mmget, sysPropValues))));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("equalsIgnoreCase", SootMatcher.SootMatcherOpType.EQUAL_NAME, arcRes, new Restrictions(true,
				new IsValueUsedInMethodCallRestriction(-1, mmget, sysPropValues), new IsValueUsedInMethodCallRestriction(0, mmget, sysPropValues))));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry(mmgetop, new Restrictions(arcRes, sysPropValues)));
		
		//Intent Strings
		MethodMatcher mmis = new MethodMatcher(
				Matcher.getAndOp(SootMatcher.getClassRegexWordsOp("\\bintent\\b",0),SootMatcher.getNameRegexWordsOp("\\bget\\saction\\b")));
		StringMatcher ssis = new StringMatcher(StringMatcher.getRegexOp("^"
			+ "android\\.intent\\.action\\.(CLOSE_SYSTEM_DIALOGS|DISMISS_KEYBOARD_SHORTCUTS|MEDIA_BUTTON|MEDIA_SCANNER_SCAN_FILE|SHOW_KEYBOARD_SHORTCUTS|MASTER_CLEAR|ACTION_SHUTDOWN)|"
			+ "android\\.appwidget\\.action\\.(APPWIDGET_CONFIGURE|APPWIDGET_UPDATE)|"
			+ "android\\.location\\.HIGH_POWER_REQUEST_CHANGE|"
			+ "com\\.android\\.omadm\\.service\\.CONFIGURATION_UPDATE"
			+ "android\\.text\\.style\\.SUGGESTION_PICKED"));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("equals", SootMatcher.SootMatcherOpType.EQUAL_NAME, arcRes, new Restrictions(true,
				new Restrictions(new IsValueUsedInMethodCallRestriction(-1,mmis), new IsValueUsedInMethodCallRestriction(0, ssis)),
				new Restrictions(new IsValueUsedInMethodCallRestriction(0,mmis), new IsValueUsedInMethodCallRestriction(-1, ssis))
		)));
		
		//Permission Strings
		FieldMatcher permF = new FieldMatcher(SootMatcher.getNameRegexWordsOp("\\bpermission(s)?\\b"));
		StringMatcher permS = new StringMatcher(StringMatcher.getRegexOp("^android\\.permission(\\-group)?\\..*"));
		
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("equals", SootMatcher.SootMatcherOpType.EQUAL_NAME, arcRes, new Restrictions(true,
				new IsValueUsedInMethodCallRestriction(-1, permF),
				new IsValueUsedInMethodCallRestriction(0, permF),
				new IsValueUsedInMethodCallRestriction(-1, permS),
				new IsValueUsedInMethodCallRestriction(0, permS)
		)));
		
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("contains", SootMatcher.SootMatcherOpType.EQUAL_NAME, arcRes, 
				new IsDeclaringClassOfMethodRestriction("java.util.Collection", true), new Restrictions(true, new IsValueUsedInMethodCallRestriction(-1, 
						new MethodMatcher(SootMatcher.SootMatcherOpType.SIGNATURE, "<java.util.Arrays: java.util.List asList(java.lang.Object[])>"), 
						new IsValueUsedInMethodCallRestriction(0, permF)),
				new IsValueUsedInMethodCallRestriction(0, permF),
				new IsValueUsedInMethodCallRestriction(0, permS)
		)));
		
		subEntry.addEntry(new KeepMethodReturnValueUseEntry(
				"<com.android.internal.util.ArrayUtils: boolean contains(java.lang.Object[],java.lang.Object)>", SootMatcher.SootMatcherOpType.SIGNATURE, 
				new Restrictions(true, new IsValueUsedInMethodCallRestriction(0, permF), 
				new IsValueUsedInMethodCallRestriction(1, permF), new IsValueUsedInMethodCallRestriction(1, permS))));
		
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("containsKey", SootMatcher.SootMatcherOpType.EQUAL_NAME, arcRes, 
				new IsDeclaringClassOfMethodRestriction("java.util.Map", true), new Restrictions(true, 
						new IsValueUsedInMethodCallRestriction(0, permF),
						new IsValueUsedInMethodCallRestriction(0, permS)
		)));
		
		//Other
		subEntry.addEntry(new KeepFieldValueUseEntry(Matcher.getAndOp(
				SootMatcher.getClassRegexWordsOp("\\b(activity|provider|component|service)\\sinfo\\b",0),
				SootMatcher.getNameRegexWordsOp("\\bexported|(is\\ssystem)|(grant\\suri\\spermissions)\\b")), new Restrictions(arcRes)));
		subEntry.addEntry(new KeepFieldValueUseEntry(Matcher.getAndOp(
				SootMatcher.getClassRegexWordsOp("\\b(broadcast|process)\\srecord\\b",0),
				SootMatcher.getNameRegexWordsOp("\\bisolated|(app\\sop)\\b")), new Restrictions(arcRes)));
		subEntry.addEntry(new KeepFieldValueUseEntry(Matcher.getAndOp(
				SootMatcher.getFullClassEqualOp("android.os.Build"),
				SootMatcher.getNameRegexWordsOp("\\bis\\sdebuggable\\b")), new Restrictions(arcRes)));
		subEntry.addEntry(new KeepFieldValueUseEntry(Matcher.getAndOp(
				SootMatcher.getClassRegexWordsOp("\\b(package\\smanager\\sservice)\\b"),
				SootMatcher.getNameRegexWordsOp("\\bsafe\\smode\\b")), arcResSingle));
		
		StringMatcher sm = new StringMatcher(StringMatcher.getEqualsOrOp("system","android"));
		
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("equals", SootMatcher.SootMatcherOpType.EQUAL_NAME, arcRes, new Restrictions(
				new IsValueUsedInMethodCallRestriction(-1, sm), 
				new IsValueUsedInMethodCallRestriction(0, sm)
				)));
		
		//for methods like getCarrierPrivilegeStatus
		subEntry.addEntry(new KeepMethodReturnValueUseEntry(SootMatcher.getNameRegexWordsOp("\\bcarrier\\sprivilege\\sstatus\\b"),arcResSingle));
		
		//bundle returned data accesses from a specific method
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("getBoolean", SootMatcher.SootMatcherOpType.EQUAL_NAME, arcRes,
				new IsDeclaringClassOfMethodRestriction("android.os.Bundle", true),
				new IsValueUsedInMethodCallRestriction(-1, new MethodMatcher(Matcher.getAndOp(
						SootMatcher.getNameRegexWordsOp("\\bget\\suser\\srestrictions\\b"),SootMatcher.getClassRegexWordsOp("\\buser\\smanager\\b"))
				))
		));
		
		//Flags
		Op<String> opflags = Matcher.getAndOp(
				SootMatcher.getNameRegexWordsOp("\\b(flag(s)?)\\b"),
				SootMatcher.getClassRegexWordsOp("\\b((uri\\spermission)|((package|application)\\smanager\\sservice)|permission\\s(state|data)|"
						+ "package\\ssetting|layout\\sparams|display|(activity|application|provider|user|service|display|device)\\sinfo)\\b",0));
		
		Op<String> opMode = Matcher.getAndOp(
				SootMatcher.getNameRegexWordsOp("\\b(get\\smode)|(factory\\stest)\\b"),
				SootMatcher.getClassRegexWordsOp("\\b(activity\\smanager\\sservice)|(factory\\stest)\\b",0)
		);
		
		subEntry.addEntry(new KeepFieldValueUseEntry(opflags,arcResSingle));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry(opflags,arcResSingle));
		subEntry.addEntry(new KeepFieldValueUseEntry(opMode,arcResSingle));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry(opMode,arcResSingle));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry(Matcher.getAndOp(SootMatcher.getNameRegexWordsOp("\\bget\\s(permission|private)\\sflags"),
				SootMatcher.getClassRegexWordsOp("\\b(package\\smanager)|(permissions\\sstate)\\b")),arcResSingle));
		
		subEntry.addEntry(new KeepMethodReturnValueUseEntry(SootMatcher.getSignatureOrOp(
				"<android.accessibilityservice.AccessibilityServiceInfo: int getCapabilities()>",
				"<com.android.server.pm.PackageSettingBase: int getEnabled(int)>"),arcResSingle));
		subEntry.addEntry(new KeepFieldValueUseEntry(SootMatcher.getSignatureOrOp(
				"<com.android.server.pm.BasePermission: int type>"),arcResSingle));
		
		ControlPredicateFilterDatabase cpf = new ControlPredicateFilterDatabase(baseEntry);
		
		try {
			cpf.writeXML("control_predicate_filter_db.xml", null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void genOrgFilter() {
		AndEntry baseEntry = new AndEntry();
		
		Set<String> loopConditionalMethods = new HashSet<>();
		loopConditionalMethods.add("<java.util.Collection: int size()>");
		loopConditionalMethods.add("<java.util.Iterator: boolean hasNext()>");
		loopConditionalMethods.add("<java.util.Collection: boolean isEmpty()>");
		loopConditionalMethods.add("<java.util.Map: boolean isEmpty()>");
		loopConditionalMethods.add("<java.util.Map: int size()>");
		loopConditionalMethods.add("<android.util.SparseArray: int size()>");
		loopConditionalMethods.add("<android.util.SparseBooleanArray: int size()>");
		loopConditionalMethods.add("<android.util.SparseIntArray: int size()>");
		loopConditionalMethods.add("<java.lang.CharSequence: int length()>");
		loopConditionalMethods.add("<android.app.usage.TimeSparseArray: int size()>");
		loopConditionalMethods.add("<com.android.internal.util.ArrayUtils: boolean isEmpty(int[])>");
		loopConditionalMethods.add("<com.android.internal.util.ArrayUtils: boolean isEmpty(java.lang.Object[])>");
		loopConditionalMethods.add("<com.android.internal.util.ArrayUtils: boolean isEmpty(java.util.Collection)>");
		loopConditionalMethods.add("<android.content.res.XmlBlock$Parser: int next()>");
		loopConditionalMethods.add("<org.kxml2.io.KXmlParser: int next()>");
		loopConditionalMethods.add("<org.kxml2.io.KXmlParser: int getDepth()>");
		loopConditionalMethods.add("<android.content.res.XmlBlock$Parser: int getDepth()>");
		loopConditionalMethods.add("<java.io.InputStream: int read()>");
		loopConditionalMethods.add("<java.io.InputStream: int read(byte[])>");
		loopConditionalMethods.add("<java.io.InputStream: int read(byte[], int, int)>");
		loopConditionalMethods.add("<java.io.InputStream: long skip(long)>");
		loopConditionalMethods.add("<java.util.ListIterator: boolean hasPrevious()>");
		baseEntry.addEntry(new NotEntry(new KeepLoopHeaderEntry(true,true,loopConditionalMethods))); //Remove all loop headers
		baseEntry.addEntry(new NotEntry(new KeepSourceMethodIsEntry(SootMatcher.SootMatcherOpType.EQUAL_NAME, "<clinit>")));//Remove all cp in the static initializers
		baseEntry.addEntry(new NotEntry(new KeepSourceMethodIsEntry(SootMatcher.SootMatcherOpType.EQUAL_NAME, "<init>")));//Remove all cp in object initializers
		
		OrEntry subEntry = new OrEntry();
		baseEntry.addEntry(subEntry);
		
		//Keep all cp whose source method is a context query or a member of a context queries subgraph
		subEntry.addEntry(new KeepSourceMethodIsInContextQuerySubGraphEntry(true));
		
		IsInArithmeticChainRestriction arcRes = new IsInArithmeticChainRestriction();
		IsInArithmeticChainRestriction arcResConst = new IsInArithmeticChainRestriction(true);
		Restrictions arcResConstSingle = new Restrictions(arcResConst);
		
		//Keep any cp that uses a context query's return value in a arithmetic chain somehow
		subEntry.addEntry(new KeepContextQueryMethodReturnValueUseEntry(arcRes));
		
		//Known Uid grabbing method signatures
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<com.android.server.pm.PackageManagerService: int getPackageUid(java.lang.String,int,int)>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.app.ApplicationPackageManager: int getPackageUid(java.lang.String,int)>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.app.ApplicationPackageManager: int getPackageUidAsUser(java.lang.String,int)>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.app.ApplicationPackageManager: int getPackageUidAsUser(java.lang.String,int,int)>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		//subEntry.addEntry(new KeepMethodReturnValueUseEntry("<com.android.server.am.ActivityManagerService: int checkGrantUriPermissionLocked(int,java.lang.String,com.android.server.am.ActivityManagerService$GrantUri,int,int)>",SootMatcher.Type.SIGNATURE,arcRes));
		//subEntry.addEntry(new KeepMethodReturnValueUseEntry("<com.android.server.am.ActivityManagerService: int checkGrantUriPermission(int,java.lang.String,android.net.Uri,int,int)>",SootMatcher.Type.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<com.android.server.pm.UserManagerService: int getUidForPackage(java.lang.String)>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<com.android.server.job.controllers.JobStatus: int getUid()>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<com.android.server.job.controllers.JobStatus: int getSourceUid()>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<com.android.server.devicepolicy.DevicePolicyManagerService$Injector: int binderGetCallingUid()>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<com.android.server.devicepolicy.DevicePolicyManagerService$ActiveAdmin: int getUid()>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.os.Binder: int getCallingUid()>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.os.Process: int myUid()>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.os.Process: int getUidForPid(int)>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.os.Process: int getUidForName(java.lang.String)>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.os.UserHandle: int getUid(int,int)>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<com.android.server.wm.WindowState: int getOwningUid()>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		/* Any method whose name contains "Uid" or whose name equals "Uid" or "uid" 
		 * and whose return value is used in the conditional stmt either directly or through some arithmetic logic op and returns an integer */
		Restrictions methodIntRes = new Restrictions(arcRes, new IsMethodReturnTypeRestriction(TypeMatcher.getEqualsOrOp(IntType.v().toString(),IntType.v().boxedType().toString())));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry(MethodMatcher.getOrOp(MethodMatcher.getNameContainOp("Uid"),MethodMatcher.getNameEqualOrOp("Uid","uid")),methodIntRes));
		//Known Uid field signatures
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.ActivityStarter: int mCallingUid>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.ActivityStartInterceptor: int mCallingUid>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.ActivityManagerService$Identity: int uid>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.ProcessRecord: int uid>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.BroadcastRecord: int callingUid>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.job.controllers.JobStatus: int sourceUid>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.job.controllers.JobStatus: int callingUid>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.ProcessRecord: int uid>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.ReceiverList: int uid>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.TaskRecord: int mCallingUid>", SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.TaskRecord: int effectiveUid>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<android.content.pm.ApplicationInfo: int uid>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<android.view.DisplayInfo: int ownerUid>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.ContentProviderRecord: int uid>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.PendingIntentRecord: int uid>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.wm.WindowState: int mOwnerUid>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.tv.TvInputManagerService$SessionState: int callingUid>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.LocationManagerService$Receiver: int mUid>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		/* Any field whose name contains "Uid" or whose name equals "Uid" or "uid" 
		 * and whose value is used in the conditional stmt either directly or through some arithmetic logic op and returns an integer */
		Restrictions fieldIntRes = new Restrictions(arcRes, new IsFieldTypeRestriction(TypeMatcher.getEqualsOrOp(IntType.v().toString(),IntType.v().boxedType().toString())));
		subEntry.addEntry(new KeepFieldValueUseEntry(FieldMatcher.getOrOp(FieldMatcher.getNameContainOp("Uid"),FieldMatcher.getNameEqualOrOp("Uid","uid")),fieldIntRes));
		
		//Know UserId grabbing methods
		//subEntry.addEntry(new KeepMethodReturnValueUseEntry("<com.android.server.am.ActivityManagerService: int handleIncomingUser(int,int,int,boolean,boolean,java.lang.String,java.lang.String)>",SootMatcher.Type.SIGNATURE,arcRes));
		//subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.app.ActivityManager: int handleIncomingUser(int,int,int,boolean,boolean,java.lang.String,java.lang.String)>",SootMatcher.Type.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.app.ActivityManager: int getCurrentUser()>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.app.ContextImpl: int getUserId()>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.content.ContextWrapper: int getUserId()>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<com.android.server.pm.UserManagerService: int getUserHandle(int)>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.os.UserManager: int getUserHandle()>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.os.UserManager: int getUserHandle(int)>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		//subEntry.addEntry(new KeepMethodReturnValueUseEntry("<com.android.server.am.UserController: int handleIncomingUser(int,int,int,boolean,int,java.lang.String,java.lang.String)>",SootMatcher.Type.SIGNATURE,arcRes));
		//subEntry.addEntry(new KeepMethodReturnValueUseEntry("<com.android.server.am.UserController: int unsafeConvertIncomingUserLocked(int)>",SootMatcher.Type.SIGNATURE,arcRes));
		//subEntry.addEntry(new KeepMethodReturnValueUseEntry("<com.android.server.am.UserController: int getCurrentOrTargetUserIdLocked()>",SootMatcher.Type.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<com.android.server.am.UserController: int getCurrentUserIdLocked()>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<com.android.server.job.controllers.JobStatus: int getUserId()>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<com.android.server.job.controllers.JobStatus: int getSourceUserId()>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<com.android.server.devicepolicy.DevicePolicyManagerService: int getDeviceOwnerUserId()>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<com.android.server.devicepolicy.DevicePolicyManagerService$Injector: int userHandleGetCallingUserId()>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.app.admin.DevicePolicyManager: int getDeviceOwnerUserId()>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.app.admin.DevicePolicyManager: int myUserId()>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<com.android.server.devicepolicy.Owners: int getDeviceOwnerUserId()>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.os.UserHandle: int getCallingUserId()>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.os.UserHandle: int getUserId(int)>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.os.UserHandle: int myUserId()>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.os.UserHandle: int getIdentifier()>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<com.android.server.TextServicesManagerService$TextServicesSettings: int getCurrentUserId()>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<com.android.internal.inputmethod.InputMethodUtils$InputMethodSettings: int getCurrentUserId()>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<com.android.server.utils.ManagedApplicationService: int getUserId()>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		//Any method containing the following names and whose value is used in the conditional stmt either directly or through arithmetic logic ops and returns an integer
		subEntry.addEntry(new KeepMethodReturnValueUseEntry(MethodMatcher.getNameContainOrOp("UserId","userId","UserHandle","userHandle","IncomingUser","incomingUser","CurrentUser","currentUser","TargetUser","targetUser","SourceUser","sourceUser","OwnerUser","ownerUser","CallingUser","callingUser"),methodIntRes));
		//Known UserId field signatures
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.ActivityManagerService$GrantUri: int sourceUserId>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.ActivityStackSupervisor: int mCurrentUser>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.ActivityStack: int mCurrentUser>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.pm.ProtectedPackages: int mDeviceOwnerUserId>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.ProcessRecord: int userId>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.BroadcastRecord: int userId>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.wallpaper.WallpaperManagerService: int mCurrentUserId>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.wallpaper.WallpaperManagerService$WallpaperData: int userId>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.pm.UserManagerService: int mGlobalRestrictionOwnerUserId>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.UserController: int mCurrentUserId>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.UserController: int mTargetUserId>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.job.controllers.JobStatus: int sourceUserId>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.ProcessRecord: int userId>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.ReceiverList: int userId>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.TaskRecord: int userId>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<android.content.pm.UserInfo: int id>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<android.content.pm.UserInfo: int profileGroupId>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<android.content.pm.UserInfo: int restrictedProfileParentId>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<android.os.UserHandle: int mHandle>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.notification.ManagedServices$ManagedServiceInfo: int userid>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.TextServicesManagerService$TextServicesSettings: int mCurrentUserId>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.PendingIntentRecord$Key: int userId>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.tv.TvInputManagerService: int mCurrentUserId>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.tv.TvInputManagerService$SessionState: int userId>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.tv.TvInputManagerService$ServiceCallback: int mUserId>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.tv.TvInputManagerService$InputServiceConnection: int mUserId>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<android.media.tv.TvInputManager: int mUserId>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.wm.WindowManagerService: int mCurrentUserId>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.LocationManagerService: int mCurrentUserId>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		//Any field containing the following names and whose value is used in the conditional stmt either directly or through arithmetic logic ops and has a integer type
		subEntry.addEntry(new KeepFieldValueUseEntry(FieldMatcher.getNameContainOrOp("UserId","userId","UserHandle","userHandle","IncomingUser","incomingUser","CurrentUser","currentUser","TargetUser","targetUser","SourceUser","sourceUser","OwnerUser","ownerUser","CallingUser","callingUser"),fieldIntRes));
		//The equals of UserHandle just performs a UserId check so uses of this in a cp are important
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.os.UserHandle: boolean equals(java.lang.Object)>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		//For the UserId value -10000 which is supposed to indicate NULL
		subEntry.addEntry(new KeepNumberConstantUseEntry(-10000, arcResConstSingle));
		
		//Pid methods
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<com.android.server.devicepolicy.DevicePolicyManagerService$Injector: int binderGetCallingPid()>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.os.Binder: int getCallingPid()>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.os.Process: int myPid()>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.os.Process: int myPpid()>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.os.Process: int getParentPid(int)>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry(MethodMatcher.getOrOp(MethodMatcher.getNameContainOrOp("Pid","PID","Ppid"),MethodMatcher.getNameEqualOrOp("Pid","pid","Ppid","ppid")),methodIntRes));
		//Pid fields
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.ActivityManagerService: int MY_PID>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.ActivityManagerService$Identity: int pid>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.ActivityStartInterceptor: int mCallingPid>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.ProcessRecord: int pid>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.BroadcastRecord: int callingPid>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.ProcessRecord: int pid>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.ReceiverList: int pid>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.LocationManagerService$Receiver: int mPid>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry(FieldMatcher.getOrOp(FieldMatcher.getNameContainOrOp("Pid","PID","Ppid"),FieldMatcher.getNameEqualOrOp("Pid","pid","Ppid","ppid")),fieldIntRes));
		
		//Gid methods
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.os.Process: int getGidForName(java.lang.String)>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.os.UserHandle: int getUserGid(int)>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.os.UserHandle: int getSharedAppGid(int)>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry(MethodMatcher.getOrOp(MethodMatcher.getNameContainOrOp("Gid"),MethodMatcher.getNameEqualOrOp("Gid","gid")),methodIntRes));
		subEntry.addEntry(new KeepNumberConstantUseEntry(NumberMatcher.getNumberOrOp(1023,1032,9997), arcResConstSingle));
		
		//AppId
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.os.UserHandle: int getAppId(int)>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.os.UserHandle: int getAppIdFromSharedAppGid(int)>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<com.android.server.usage.UsageStatsService: int getAppId(java.lang.String)>",SootMatcher.SootMatcherOpType.SIGNATURE,arcRes));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.pm.PackageSetting: int appId>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes));
		subEntry.addEntry(new KeepNumberConstantUseEntry(NumberMatcher.getNumberOrOp(1000, 1001, 2000, 1007, 1010, 1013, 1019, 1016, 1027, 1002, 1037, 1041, 1047), arcResConstSingle));
		
		//Flag methods
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<com.android.server.pm.PackageManagerService: int getPrivateFlagsForUid(int)>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opAnd,new IsNumberUsedRestriction(8, 1024))));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<com.android.server.pm.PackageManagerService: int getPermissionFlags(java.lang.String,java.lang.String,int)>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opAnd)));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.app.ApplicationPackageManager: int getPermissionFlags(java.lang.String,java.lang.String,android.os.UserHandle)>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opAnd)));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<com.android.server.pm.PermissionsState$PermissionData: int getFlags(int)>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opAnd)));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<com.android.server.pm.PermissionsState$PermissionState: int getFlags()>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opAnd)));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<com.android.server.pm.PermissionsState: int getPermissionFlags(java.lang.String,int)>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opAnd)));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.os.FactoryTest int getMode()>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opEq, new IsNumberUsedRestriction(NumberMatcher.getNumberOrOp(0, 1, 2)))));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.os.FactoryTest int getMode()>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opNeq, new IsNumberUsedRestriction(NumberMatcher.getNumberOrOp(0, 1, 2)))));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.os.FactoryTest int getMode()>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opGteq, new IsNumberUsedRestriction(NumberMatcher.getNumberOrOp(0, 1, 2)))));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.os.FactoryTest int getMode()>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opLteq, new IsNumberUsedRestriction(NumberMatcher.getNumberOrOp(0, 1, 2)))));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.view.Display: int getFlags()>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opAnd,new IsNumberUsedRestriction(4, true))));
		
		//Flag fields
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.ActivityManagerService: int mFactoryTest>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opEq, new IsNumberUsedRestriction(NumberMatcher.getNumberOrOp(0, 1, 2)))));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.ActivityManagerService: int mFactoryTest>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opNeq, new IsNumberUsedRestriction(NumberMatcher.getNumberOrOp(0, 1, 2)))));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.ActivityManagerService: int mFactoryTest>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opGteq, new IsNumberUsedRestriction(NumberMatcher.getNumberOrOp(0, 1, 2)))));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.ActivityManagerService: int mFactoryTest>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opLteq, new IsNumberUsedRestriction(NumberMatcher.getNumberOrOp(0, 1, 2)))));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.pm.PackageSetting: int pkgFlags>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opAnd, new IsNumberUsedRestriction(1, 128))));
		subEntry.addEntry(new KeepFieldValueUseEntry("<android.content.pm.ActivityInfo: int flags>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opAnd, new IsNumberUsedRestriction(-2147483648, 536870912, 1073741824))));
		subEntry.addEntry(new KeepFieldValueUseEntry("<android.content.pm.ApplicationInfo: int flags>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opAnd, new IsNumberUsedRestriction(1, 2, 8, 16, 128))));
		subEntry.addEntry(new KeepFieldValueUseEntry("<android.content.pm.ApplicationInfo: int privateFlags>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opAnd, new IsNumberUsedRestriction(8, 1024))));
		subEntry.addEntry(new KeepFieldValueUseEntry("<android.content.pm.ProviderInfo: int flags>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opAnd, new IsNumberUsedRestriction(1073741824))));
		subEntry.addEntry(new KeepFieldValueUseEntry("<android.content.pm.UserInfo: int flags>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opAnd, new IsNumberUsedRestriction(2, 8))));
		subEntry.addEntry(new KeepFieldValueUseEntry("<android.content.pm.ServiceInfo: int flags>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opAnd, new IsNumberUsedRestriction(1073741824, 4, 2))));
		subEntry.addEntry(new KeepFieldValueUseEntry("<android.view.DisplayInfo: int flags>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opAnd, new IsNumberUsedRestriction(4, true))));
		subEntry.addEntry(new KeepFieldValueUseEntry("<android.view.Display: int mFlags>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opAnd, new IsNumberUsedRestriction(4, true))));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.display.DisplayDeviceInfo: int flags>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opAnd, new IsNumberUsedRestriction(16, true))));
		subEntry.addEntry(new KeepFieldValueUseEntry("<android.view.WindowManager$LayoutParams: int privateFlags>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opAnd, new IsNumberUsedRestriction(16, true))));
		
		//Special flag fields that look like ((persistableModeFlags & modeFlags) == modeFlags)
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.UriPermission: int modeFlags>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opAnd, new IsFieldUsedDirectlyInRestriction(SootMatcher.SootMatcherOpType.SIGNATURE, "<com.android.server.am.UriPermission: int persistableModeFlags>"))));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.UriPermission: int modeFlags>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opAnd, new IsFieldUsedDirectlyInRestriction(SootMatcher.SootMatcherOpType.SIGNATURE, "<com.android.server.am.UriPermission: int ownedModeFlags>"))));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.UriPermission: int modeFlags>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opAnd, new IsFieldUsedDirectlyInRestriction(SootMatcher.SootMatcherOpType.SIGNATURE, "<com.android.server.am.UriPermission: int globalModeFlags>"))));
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.UriPermission: int modeFlags>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opAnd, new IsFieldUsedDirectlyInRestriction(SootMatcher.SootMatcherOpType.SIGNATURE, "<com.android.server.am.UriPermission: int persistedModeFlags>"))));
		//handles (persistedModeFlags & ~modeFlags) == 0
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.UriPermission: int persistedModeFlags>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opAnd, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opNeg, new IsFieldUsedDirectlyInRestriction(SootMatcher.SootMatcherOpType.SIGNATURE, "<com.android.server.am.UriPermission: int modeFlags>")))));
		//modeFlags can also be compared against a mask
		subEntry.addEntry(new KeepFieldValueUseEntry("<com.android.server.am.UriPermission: int modeFlags>", SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, new IsInArithmeticOpRestriction(IsInArithmeticOpRestriction.opAnd, new IsNumberUsedRestriction(1, 2, 64))));
		
		//Package Name checks
		
		//Methods for accessing package names
		MethodMatcher pkgNameMMatch = new MethodMatcher(MethodMatcher.getSignatureOrOp(
				"<com.android.server.pm.PackageManagerService: java.lang.String[] getPackagesForUid(int)>",
				"<android.app.ApplicationPackageManager: java.lang.String[] getPackagesForUid(int)>",
				"<com.android.server.am.ActivityManagerService$PermissionController: java.lang.String[] getPackagesForUid(int)>",
				"<com.android.server.pm.PackageManagerService: java.lang.String getInstallerPackageName(java.lang.String)>",
				"<android.app.ApplicationPackageManager: java.lang.String getInstallerPackageName(java.lang.String)>",
				"<android.app.LoadedApk: java.lang.String getPackageName()>",
				"<android.app.ContextImpl: java.lang.String getPackageName()>",
				"<android.content.ContextWrapper: java.lang.String getPackageName()>",
				"<android.widget.RemoteViews$4: java.lang.String getPackageName()>",
				"<android.provider.Settings: java.lang.String getPackageNameForUid(android.content.Context,int)>",
				"<com.android.server.job.controllers.JobStatus: java.lang.String getSourcePackageName()>",
				"<com.android.server.tv.TvInputManagerService$BinderService: java.lang.String getCallingPackageName()>"));
		
		//Methods that return an array of package names - first three from above
		//Note the first three are array of names which are used like equals($p[$i]) or $p[$i].equals(...) which is handled in ValueUsedInMethodCall
		MethodMatcher pkgNameArrMatcher = new MethodMatcher(MethodMatcher.getSignatureOrOp(
				"<com.android.server.pm.PackageManagerService: java.lang.String[] getPackagesForUid(int)>",
				"<android.app.ApplicationPackageManager: java.lang.String[] getPackagesForUid(int)>",
				"<com.android.server.am.ActivityManagerService$PermissionController: java.lang.String[] getPackagesForUid(int)>"));
		
		//Fields that store packag names
		FieldMatcher pkgNameFMatch = new FieldMatcher(FieldMatcher.getSignatureOrOp(
				"<com.android.server.am.ProcessRecord: java.lang.String processName>",
				"<com.android.server.pm.ProtectedPackages: java.lang.String mDeviceOwnerPackage>",
				"<com.android.server.job.controllers.JobStatus: java.lang.String sourcePackageName>",
				"<com.android.server.am.UriPermission: java.lang.String sourcePkg>",
				"<com.android.server.am.UriPermission: java.lang.String targetPkg>"));
		
		//name and packageName should only matter if used in an equals check with getPackageName() and getClassName() of ComponentName
		Restrictions specialEq = new Restrictions(true, new Restrictions(
				new IsValueUsedInMethodCallRestriction(-1, new FieldMatcher(FieldMatcher.getSignatureOrOp(
						"<android.content.pm.ActivityInfo: java.lang.String name>",
						"<android.content.pm.ActivityInfo: java.lang.String packageName>"))),
				new IsValueUsedInMethodCallRestriction(0, new MethodMatcher(MethodMatcher.getSignatureOrOp(
						"<android.content.ComponentName: java.lang.String getPackageName()>",
						"<android.content.ComponentName: java.lang.String getClassName()>")))
				),
				new Restrictions(new IsValueUsedInMethodCallRestriction(0, new FieldMatcher(FieldMatcher.getSignatureOrOp(
						"<android.content.pm.ActivityInfo: java.lang.String name>",
						"<android.content.pm.ActivityInfo: java.lang.String packageName>"))),
				new IsValueUsedInMethodCallRestriction(-1, new MethodMatcher(MethodMatcher.getSignatureOrOp(
						"<android.content.ComponentName: java.lang.String getPackageName()>",
						"<android.content.ComponentName: java.lang.String getClassName()>")))
				)
		);
		
		//Or restriction for equals package name that allows any that use a field or method listed at either equals position or the special cond.
		Restrictions equalsRes = new Restrictions(true, new IsValueUsedInMethodCallRestriction(-1,pkgNameMMatch), 
				new IsValueUsedInMethodCallRestriction(0, pkgNameMMatch), new IsValueUsedInMethodCallRestriction(-1, pkgNameFMatch), 
				new IsValueUsedInMethodCallRestriction(0, pkgNameFMatch), specialEq);
		
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("equals", SootMatcher.SootMatcherOpType.EQUAL_NAME, arcRes, equalsRes));
		
		//Handles the situation where contains is called on a list created by Arrays asList which is used on an array of the three package name arrs
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("contains", SootMatcher.SootMatcherOpType.EQUAL_NAME, arcRes, 
				new IsDeclaringClassOfMethodRestriction("java.util.Collection", true), new Restrictions(true, new IsValueUsedInMethodCallRestriction(-1, 
				new MethodMatcher(SootMatcher.SootMatcherOpType.SIGNATURE, "<java.util.Arrays: java.util.List asList(java.lang.Object[])>"), 
				new IsValueUsedInMethodCallRestriction(0, pkgNameArrMatcher)),
				new IsValueUsedInMethodCallRestriction(-1, new FieldMatcher(FieldMatcher.getSignatureOrOp(
						"<com.android.server.notification.ManagedServices: android.util.ArraySet mEnabledServicesForCurrentProfiles>",
						"<com.android.server.notification.ManagedServices: android.util.ArraySet mEnabledServicesPackageNames>"))),
				new IsValueUsedInMethodCallRestriction(0, pkgNameFMatch),
				new IsValueUsedInMethodCallRestriction(0, pkgNameMMatch))));
		
		//Handles the situation where a custom android contains method is used to check if one of the package arrays contains a specific name
		subEntry.addEntry(new KeepMethodReturnValueUseEntry(
				"<com.android.internal.util.ArrayUtils: boolean contains(java.lang.Object[],java.lang.Object)>", SootMatcher.SootMatcherOpType.SIGNATURE, 
				new Restrictions(true, new IsValueUsedInMethodCallRestriction(0, pkgNameArrMatcher), 
				new IsValueUsedInMethodCallRestriction(1, pkgNameFMatch), new IsValueUsedInMethodCallRestriction(1, pkgNameMMatch))));
		
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("containsKey", SootMatcher.SootMatcherOpType.EQUAL_NAME, arcRes, 
				new IsDeclaringClassOfMethodRestriction("java.util.Map", true), new Restrictions(true, new IsValueUsedInMethodCallRestriction(-1, 
						new FieldMatcher(SootMatcher.SootMatcherOpType.SIGNATURE,"<com.android.server.am.ProcessRecord: android.util.ArrayMap pkgList>")), 
						new IsValueUsedInMethodCallRestriction(0, pkgNameFMatch),
						new IsValueUsedInMethodCallRestriction(0, pkgNameMMatch)
				)));
		
		//SystemProperties
		IsValueUsedInMethodCallRestriction sysPropValues = new IsValueUsedInMethodCallRestriction(0, 
				new StringMatcher(StringMatcher.getEqualsOrOp("ro.factorytest","ro.test_harness","ro.debuggable","ro.secure")));
		MethodMatcher mmget = new MethodMatcher(MethodMatcher.getSignatureOrOp("<android.os.SystemProperties: java.lang.String get(java.lang.String)>", 
				"<android.os.SystemProperties: java.lang.String get(java.lang.String,java.lang.String)>"));
		
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("equals", SootMatcher.SootMatcherOpType.EQUAL_NAME, arcRes, new Restrictions(true,
				new IsValueUsedInMethodCallRestriction(-1, mmget, sysPropValues), new IsValueUsedInMethodCallRestriction(0, mmget, sysPropValues))));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("equalsIgnoreCase", SootMatcher.SootMatcherOpType.EQUAL_NAME, arcRes, new Restrictions(true,
				new IsValueUsedInMethodCallRestriction(-1, mmget, sysPropValues), new IsValueUsedInMethodCallRestriction(0, mmget, sysPropValues))));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.os.SystemProperties: boolean getBoolean(java.lang.String,boolean)>", 
				SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, sysPropValues));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.os.SystemProperties: int getInt(java.lang.String,int)>", 
				SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, sysPropValues));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("<android.os.SystemProperties: long getLong(java.lang.String,long)>", 
				SootMatcher.SootMatcherOpType.SIGNATURE, arcRes, sysPropValues));
		
		//Intent Strings
		MethodMatcher mmis = new MethodMatcher(SootMatcher.SootMatcherOpType.SIGNATURE, "<android.content.Intent: java.lang.String getAction()>");
		StringMatcher ssis = new StringMatcher(StringMatcher.getEqualsOrOp(
				"android.appwidget.action.APPWIDGET_CONFIGURE", 
				"android.appwidget.action.APPWIDGET_UPDATE",
				"android.intent.action.CLOSE_SYSTEM_DIALOGS",
				"android.intent.action.DISMISS_KEYBOARD_SHORTCUTS",
				"android.intent.action.MEDIA_BUTTON",
				"android.intent.action.MEDIA_SCANNER_SCAN_FILE",
				"android.intent.action.SHOW_KEYBOARD_SHORTCUTS",
				"android.intent.action.MASTER_CLEAR",
				"android.location.HIGH_POWER_REQUEST_CHANGE",
				"com.android.omadm.service.CONFIGURATION_UPDATE",
				"android.text.style.SUGGESTION_PICKED",
				"android.intent.action.ACTION_SHUTDOWN"));
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("equals", SootMatcher.SootMatcherOpType.EQUAL_NAME, arcRes, new Restrictions(true,
					new Restrictions(new IsValueUsedInMethodCallRestriction(-1,mmis), new IsValueUsedInMethodCallRestriction(0, ssis)),
					new Restrictions(new IsValueUsedInMethodCallRestriction(0,mmis), new IsValueUsedInMethodCallRestriction(-1, ssis))
				)));
		
		//Permission Strings
		FieldMatcher permF = new FieldMatcher(FieldMatcher.getSignatureOrOp(
				"<android.content.pm.ActivityInfo: java.lang.String permission>",
				"<android.content.pm.ApplicationInfo: java.lang.String permission>",
				"<android.content.pm.ServiceInfo: java.lang.String permission>",
				"<com.android.server.notification.ManagedServices$Config: java.lang.String bindPermission>",
				"<com.android.server.vr.EnabledComponentsObserver: java.lang.String mServicePermission>",
				"<android.content.pm.PackageInfo: java.lang.String[] requestedPermissions>"));
		
		FieldMatcher arrPermF = new FieldMatcher(SootMatcher.SootMatcherOpType.SIGNATURE,
				"<android.content.pm.PackageInfo: java.lang.String[] requestedPermissions>");
		
		StringMatcher permS = new StringMatcher(StringMatcher.getStartsWithOrOp("android.permission.","android.permission-group."));
		
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("equals", SootMatcher.SootMatcherOpType.EQUAL_NAME, arcRes, new Restrictions(true,
				new IsValueUsedInMethodCallRestriction(-1, permF),
				new IsValueUsedInMethodCallRestriction(0, permF),
				new IsValueUsedInMethodCallRestriction(-1, permS),
				new IsValueUsedInMethodCallRestriction(0, permS)
				)));
		
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("contains", SootMatcher.SootMatcherOpType.EQUAL_NAME, arcRes, 
				new IsDeclaringClassOfMethodRestriction("java.util.Collection", true), new Restrictions(true, new IsValueUsedInMethodCallRestriction(-1, 
						new MethodMatcher(SootMatcher.SootMatcherOpType.SIGNATURE, "<java.util.Arrays: java.util.List asList(java.lang.Object[])>"), 
						new IsValueUsedInMethodCallRestriction(0, arrPermF)),
				new IsValueUsedInMethodCallRestriction(0, permF),
				new IsValueUsedInMethodCallRestriction(0, permS)
				)));
		
		subEntry.addEntry(new KeepMethodReturnValueUseEntry(
				"<com.android.internal.util.ArrayUtils: boolean contains(java.lang.Object[],java.lang.Object)>", SootMatcher.SootMatcherOpType.SIGNATURE, 
				new Restrictions(true, new IsValueUsedInMethodCallRestriction(0, arrPermF), 
				new IsValueUsedInMethodCallRestriction(1, permF), new IsValueUsedInMethodCallRestriction(1, permS))));
		
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("containsKey", SootMatcher.SootMatcherOpType.EQUAL_NAME, arcRes, 
				new IsDeclaringClassOfMethodRestriction("java.util.Map", true), new Restrictions(true, 
						new IsValueUsedInMethodCallRestriction(0, permF),
						new IsValueUsedInMethodCallRestriction(0, permS)
				)));
		
		//Other
		subEntry.addEntry(new KeepFieldValueUseEntry(FieldMatcher.getSignatureOrOp(
				"<com.android.server.am.BroadcastRecord: int appOp>",
				"<com.android.server.am.ProcessRecord: boolean isolated>",
				"<android.content.pm.ActivityInfo: boolean exported>",
				"<android.content.pm.ProviderInfo: boolean grantUriPermissions>",
				"<android.content.pm.ProviderInfo: boolean exported>",
				"<android.content.pm.ComponentInfo: boolean exported>",
				"<android.os.Build: boolean IS_DEBUGGABLE>",
				"<com.android.server.notification.ManagedServices$ManagedServiceInfo: boolean isSystem>"), new Restrictions(arcRes)));
		
		StringMatcher sm = new StringMatcher(StringMatcher.getEqualsOrOp("system","android"));
		
		subEntry.addEntry(new KeepMethodReturnValueUseEntry("equals", SootMatcher.SootMatcherOpType.EQUAL_NAME, arcRes, new Restrictions(
				new IsValueUsedInMethodCallRestriction(-1, sm), 
				new IsValueUsedInMethodCallRestriction(0, sm)
				)));
		
		ControlPredicateFilterDatabase cpf = new ControlPredicateFilterDatabase(baseEntry);
		
		try {
			cpf.writeXML("control_predicate_filter_db.xml", null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
