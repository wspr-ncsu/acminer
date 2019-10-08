package com.benandow.policyminer.controlpredicatefilter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.benandow.policyminer.controlpredicatefilter.utils.FilterRule;
import com.benandow.policyminer.controlpredicatefilter.utils.GenFilterXmlOutput;
import com.benandow.policyminer.controlpredicatefilter.utils.Method;
import com.benandow.policyminer.controlpredicatefilter.utils.MethodParseUtil;
import com.benandow.policyminer.controlpredicatefilter.utils.TestFileParser;

public class ContextQueryFilter {
	
	private static final boolean VERBOSE = false;	
		
	private static String[] methodSignatures = new String[] { 
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
			"<com.android.server.notification.ManagedServices$ManagedServiceInfo: boolean supportsProfiles()>",
			"<com.android.server.accounts.AccountManagerService: boolean isAccountVisibleToCaller(java.lang.String,int,int,java.lang.String)>",
			"<com.android.server.am.ActivityManagerService$AppTaskImpl: void checkCaller()>",
			"<com.android.server.devicepolicy.DevicePolicyManagerService$Injector: boolean userManagerIsSplitSystemUser()>",
			"<android.os.UserHandle: boolean isApp(int)>",
			"<com.android.server.pm.DefaultPermissionGrantPolicy: boolean isSysComponentOrPersistentPlatformSignedPrivAppLPr(android.content.pm.PackageParser$Package)>",
			"<android.app.ContextImpl: void enforce(java.lang.String,int,boolean,int,java.lang.String)>",
			"<android.content.pm.UserInfo: boolean isRestricted()>",
			"<com.android.server.fingerprint.FingerprintService$FingerprintServiceWrapper: boolean isRestricted()>",
			"<com.android.server.pm.UserRestrictionsUtils: boolean isValidRestriction(java.lang.String)>",
			"<com.android.server.am.ActivityManagerService: boolean isValidSingletonCall(int,int)>",
			"<com.android.server.TextServicesManagerService: boolean calledFromValidUser()>",
			"<com.android.server.display.DisplayManagerService$BinderService: boolean validatePackageName(int,java.lang.String)>",
			//These are exclusions
			"<android.provider.Settings$System: boolean canWrite(android.content.Context)>",
			"<com.android.server.display.DisplayManagerService$BinderService: boolean canProjectSecureVideo(android.media.projection.IMediaProjection)>",
			"<com.android.server.display.DisplayManagerService$BinderService: boolean canProjectVideo(android.media.projection.IMediaProjection)>",
			"<com.android.providers.settings.SettingsProvider: boolean isGlobalOrSecureSettingRestrictedForUser(java.lang.String,int,java.lang.String,int)>",
			"<com.android.contacts.common.util.PermissionsUtil: boolean hasAppOp(android.content.Context,java.lang.String)>",
			"<com.android.server.AppOpsService: boolean isPackageSuspendedForUser(java.lang.String,int)>",
			"<com.android.server.notification.NotificationManagerService: boolean isPackageSuspendedForUser(java.lang.String,int)>",
			"<android.content.pm.UserInfo: boolean isAdmin()>",
			"<android.os.UserHandle: boolean isOwner()>",
			"<com.android.providers.blockednumber.BlockedNumberProvider: boolean canCurrentUserBlockUsers()>",
			"<com.android.server.wifi.WifiServiceImpl: boolean isForegroundApp(java.lang.String)>",
			"<com.android.messaging.util.OsUtil: boolean hasSmsPermission()>",
			"<com.android.server.TextServicesManagerService$TextServicesMonitor: boolean isChangingPackagesOfCurrentUser()>"
	};
	
	public static void printFilter() {
		List<FilterRule> rules = new ArrayList<FilterRule>();
		
		
//		if (packageName.contains("com.android.server.firewall.") 
//				&& FWALL_REGEX.matcher(m).find() 
//				&& !(methodName.equals("matchesValue") 
//						&& packageName.equals("com.android.server.firewall.StringFilter"))) {
			
		FilterRule r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.EQUAL_PKG_RULE, "com.android.server.firewall"));
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, FWALL_REGEX.toString()));
		r.addChild(FilterRule.genNot(new FilterRule(FilterRule.SIG_RULE, "<com.android.server.firewall.StringFilter: boolean matchesValue(java.lang.String)>")));
		rules.add(r);
		
//		} else if (packageName.contains("AppOps") 
//				&& !APPOP_UNCHECKED_REGEX.matcher(m).find() 
//				&& (OP_REGEX.matcher(m).find() || APPOP_CHKPKG_REGEX.matcher(m).find())) {
		
			r = GenFilterXmlOutput.genDefaultRegexRule();
			r.addChild(new FilterRule(FilterRule.CONTAIN_CLASS_RULE, "AppOps"));
			r.addChild(FilterRule.genNot(new FilterRule(FilterRule.REGEX_RULE, APPOP_UNCHECKED_REGEX.toString())));
			r.addChild(FilterRule.genOR(
							new FilterRule(FilterRule.REGEX_RULE, OP_REGEX.toString()),
							new FilterRule(FilterRule.REGEX_RULE, APPOP_CHKPKG_REGEX.toString())
							));

			rules.add(r);
		
//		} else if (packageName.contains("LockPatternUtils") && PSWD_REGEX.matcher(m).find()) {
			r = GenFilterXmlOutput.genDefaultRegexRule();
			r.addChild(new FilterRule(FilterRule.EQUAL_CLASS_RULE, "LockPatternUtils"));
			r.addChild(new FilterRule(FilterRule.REGEX_RULE, PSWD_REGEX.toString()));
			rules.add(r);
			
//		} else if (checkSpecialCase(packageName, methodName)) {
			r = new FilterRule(FilterRule.OR);
			for (String sig : ContextQueryFilter.methodSignatures) {
				r.addChild(new FilterRule(FilterRule.SIG_RULE, sig));
			}
			rules.add(r);
		
//		} else if (PERM_CHKENFORCE_REGEX.matcher(m).find() 
//		&& !PERM_NEG_REGEX2.matcher(m).find() 
//		&& !packageName.contains("android.test.") 
//		&& !packageName.contains("com.android.future.usb.") 
//		&&  !packageName.contains("com.android.packageinstaller.permission.model.") 
//		&& !packageName.contains("MockContext") 
//		&& !packageName.contains("MockPackageManager") 
//		&& !PERM_CHECK_NEG.matcher(m).find() 
//		&& !(methodName.equals("checkAddPermission") 
//				&& packageName.equals("android.view.WindowManagerPolicy"))) {
		
			r = GenFilterXmlOutput.genDefaultRegexRule();
			r.addChild(new FilterRule(FilterRule.REGEX_RULE, PERM_CHKENFORCE_REGEX.toString()));
//			r.addChild(FilterRule.genNot(new FilterRule(FilterRule.REGEX_RULE, PERM_NEG_REGEX2.toString())));
			r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_PKG_RULE, "android.test")));
			r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_PKG_RULE, "com.android.future.usb")));
			r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_PKG_RULE, "com.android.packageinstaller.permission.model")));
			r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_CLASS_RULE, "MockContext")));
			r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_CLASS_RULE, "MockPackageManager")));		
			r.addChild(FilterRule.genNot(new FilterRule(FilterRule.REGEX_RULE, PERM_CHECK_NEG.toString())));
			r.addChild(FilterRule.genNot(new FilterRule(FilterRule.SIG_RULE, "<android.view.WindowManagerPolicy: int checkAddPermission(android.view.WindowManager$LayoutParams,int[])>")));
			rules.add(r);
			
//		} else if (PERM_REGEX3.matcher(m).find() 
//		&& !packageName.contains("MockContext") 
//		&& !packageName.contains("MockPackageManager")) {
		
			r = GenFilterXmlOutput.genDefaultRegexRule();
			r.addChild(new FilterRule(FilterRule.REGEX_RULE, PERM_REGEX3.toString()));
			r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_CLASS_RULE, "MockContext")));
			r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_CLASS_RULE, "MockPackageManager")));
			rules.add(r);		
			
			
//		} else if (PERM_REGEX4.matcher(m).find() 
//		&& !packageName.contains("MockContext") 
//		&& !packageName.contains("MockPackageManager")) {
		
		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, PERM_REGEX4.toString()));
		r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_CLASS_RULE, "MockContext")));
		r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_CLASS_RULE, "MockPackageManager")));
		rules.add(r);
		
//		} else if (SIG_REGEX1.matcher(m).find() 
//		&& !packageName.contains("MockContext")
//		&& !packageName.contains("MockPackageManager")) {
		
		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, SIG_REGEX1.toString()));
		r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_CLASS_RULE, "MockContext")));
		r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_CLASS_RULE, "MockPackageManager")));
		rules.add(r);
		
		
//		} else if (PKG_CHKENFORCE_REGEX.matcher(m).find()) {

		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, PKG_CHKENFORCE_REGEX.toString()));
		rules.add(r);
		
//		} else if (IS_CALLING_PKG_REGEX.matcher(m).find()) {
		
		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, IS_CALLING_PKG_REGEX.toString()));
		rules.add(r);		
		
//		}  else if (SYSTEM_ID_REGEX.matcher(m).find()) {
		
		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, SYSTEM_ID_REGEX.toString()));
		rules.add(r);	
		
//		} else if (ADMIN_REGEX.matcher(m).find()) {
		
		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, ADMIN_REGEX.toString()));
		rules.add(r);	
		
//		} else if (DEVPROF_OWNER_REGEX.matcher(m).find()) { 
		
		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, DEVPROF_OWNER_REGEX.toString()));
		rules.add(r);	
		
//		} else if (DEVPROF_OWNER_REGEX1.matcher(m).find()) {
		
		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, DEVPROF_OWNER_REGEX1.toString()));
		rules.add(r);
		
//		} else if (CROSSPROF_REGEX1.matcher(m).find()) {
		
		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, CROSSPROF_REGEX1.toString()));
		rules.add(r);
		
//		} else if (CHK_ENFORCE_REGEX.matcher(m).find() 
//		&& !packageName.contains("MockContext") 
//		&& !packageName.contains("MockPackageManager")) {
		
		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, CHK_ENFORCE_REGEX.toString()));
		r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_CLASS_RULE, "MockContext")));
		r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_CLASS_RULE, "MockPackageManager")));
		rules.add(r);
		
		
//		} else if (IS_REGEX.matcher(m).find() 
//		&& !packageName.contains("CredentialHelper") 
//		&& !packageName.contains("MockPackageManager") 
//		&& !packageName.contains("android.icu.text.") 
//		&& !UID_NOT_MATCH.matcher(m).find() 
//		&& !(methodName.equals("isIsolated") && packageName.equals("com.android.systemui.statusbar.phone.NotificationGroupManager"))) {

		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, IS_REGEX.toString()));
		r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_CLASS_RULE, "CredentialHelper")));
		r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_CLASS_RULE, "MockContext")));
		r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_CLASS_RULE, "MockPackageManager")));
		r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_PKG_RULE, "android.icu.text")));
		r.addChild(FilterRule.genNot(new FilterRule(FilterRule.REGEX_RULE, UID_NOT_MATCH.toString())));
		r.addChild(FilterRule.genNot(new FilterRule(FilterRule.SIG_RULE, "<com.android.systemui.statusbar.phone.NotificationGroupManager: boolean isIsolated(android.service.notification.StatusBarNotification)>")));
		rules.add(r);

//		} else if (HAS_REGEX.matcher(m).find() && !packageName.contains("RestrictedLockUtils")) {
		
		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, HAS_REGEX.toString()));
		r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_CLASS_RULE, "RestrictedLockUtils")));
		rules.add(r);
			
//		} else if (GET_REGEX.matcher(m).find() 
//		&& !packageName.contains("com.android.internal.telephony.uicc.")) {
			
		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, GET_REGEX.toString()));
		r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_PKG_RULE, "com.android.internal.telephony.uicc")));
		rules.add(r);
			
		
//		} else if (GETPROT_REGEX.matcher(m).find()) {
		
		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, GETPROT_REGEX.toString()));
		rules.add(r);
		
//		} else if (OPERS_REGEX.matcher(m).find()) {

		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, OPERS_REGEX.toString()));
		rules.add(r);
			
//		} else if (HANDLE_REGEX.matcher(m).find()) {
		
		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, HANDLE_REGEX.toString()));
		rules.add(r);
		
//		} else if (IS_ENABLED_REGEX1.matcher(m).find()) {
		
		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, IS_ENABLED_REGEX1.toString()));
		rules.add(r);
			
//		} else if (IS_ENABLED_REGEX2.matcher(m).find()) {
		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, IS_ENABLED_REGEX2.toString()));
		rules.add(r);
			
		
//		} else if (VERIFY_REGEX.matcher(m).find()) {
		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, VERIFY_REGEX.toString()));
		rules.add(r);
			
		
//		} else if (CHK_REGEX1.matcher(m).find()) {
		
		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, CHK_REGEX1.toString()));
		rules.add(r);
			
		
//		} else if (ISPROT_REGEX.matcher(m).find() 
//		&& !packageName.contains("MockPackageManager") 
//		&& !packageName.contains("android.content.pm.permission.") 
//		&& !packageName.contains("com.android.packageinstaller.permission.model.")) {

		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, ISPROT_REGEX.toString()));
		r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_CLASS_RULE, "MockPackageManager")));
		r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_PKG_RULE, "android.content.pm.permission")));
		r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_PKG_RULE, "com.android.packageinstaller.permission.model")));
		rules.add(r);
			
//		} else if (IS_ALLOWED_REGEX.matcher(m).find()) {
		
		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, IS_ALLOWED_REGEX.toString()));
		rules.add(r);
		
//		} else if (ISLOCK_REGEX.matcher(m).find()) {
		
		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, ISLOCK_REGEX.toString()));
		rules.add(r);
		
//		} else if (IS_PSSWD_SUFF_REGEX.matcher(m).find()) {
		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, IS_PSSWD_SUFF_REGEX.toString()));
		rules.add(r);
		
//		} else if (IS_APP_REGEX.matcher(m).find()) {
		
		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, IS_APP_REGEX.toString()));
		rules.add(r);
		
//		} else if (IS_UORS_REGEX.matcher(m).find() 
//		&& !packageName.contains("KeyEvent") 
//		&& !packageName.contains("ActivityThread") 
//		&& !packageName.contains("MediaRouter") 
//		&& !packageName.contains("TrustedCertificateStore") 
//		&& !packageName.contains("ZenModeFiltering") 
//		&& !packageName.contains("com.android.packageinstaller.permission.utils.")) {
				
		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, IS_UORS_REGEX.toString()));
		r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_CLASS_RULE, "KeyEvent")));
		r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_CLASS_RULE, "ActivityThread")));
		r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_CLASS_RULE, "MediaRouter$RouteCategory")));
		r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_CLASS_RULE, "TrustedCertificateStore")));
		r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_CLASS_RULE, "ZenModeFiltering")));
		r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_PKG_RULE, "com.android.packageinstaller.permission.utils")));
		rules.add(r);

//		} else if (IS_RESTRICT_REGEX.matcher(m).find()) {
		
		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, IS_RESTRICT_REGEX.toString()));
		rules.add(r);
		
//		} else if (VMAIL_REGEX.matcher(m).find() && packageName.contains("VoicemailPermissions")) {
		
//		} else if (VMAIL_REGEX.matcher(m).find() && packageName.contains("VoicemailPermissions")) {
		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, VMAIL_REGEX.toString()));
		r.addChild(new FilterRule(FilterRule.EQUAL_CLASS_RULE, "VoicemailPermissions"));
		rules.add(r);
		
//		} else if (PERMIT_RULES.matcher(m).find()) {
		
		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, PERMIT_RULES.toString()));
		rules.add(r);
		
//		} else if (KEYGUARD_SEC_REGEX.matcher(m).find() 
//		&& (!packageName.contains("WindowManagerPolicy") 
//				&& !packageName.contains("QuickActivity") 
//				&& !packageName.contains("BaseStatusBar"))) {
		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, KEYGUARD_SEC_REGEX.toString()));
		r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_CLASS_RULE, "WindowManagerPolicy")));
		r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_CLASS_RULE, "QuickActivity")));
		r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_CLASS_RULE, "BaseStatusBar")));
		rules.add(r);
			
		
//		} else if (KEYGUARD_SEC_REGEX2.matcher(m).find() &&
//			(packageName.contains("KeyguardService") || 
//				packageName.contains("KeyguardState") || 
//				packageName.contains("LockPattern"))) {
			
		
//		} else if (KEYGUARD_SEC_REGEX2.matcher(m).find() && (packageName.contains("KeyguardService") || packageName.contains("KeyguardState") || packageName.contains("LockPattern"))) {
		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(FilterRule.genOR(
				new FilterRule(FilterRule.CONTAIN_CLASS_RULE, "KeyguardService"),
				new FilterRule(FilterRule.CONTAIN_CLASS_RULE, "KeyguardState"),
				new FilterRule(FilterRule.CONTAIN_CLASS_RULE, "LockPattern")
		));
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, KEYGUARD_SEC_REGEX2.toString()));
		rules.add(r);
			

		
//		} else if (IS_PROFILE_REGEX.matcher(m).find()) {
		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, IS_PROFILE_REGEX.toString()));
		rules.add(r);
		
//		} else if (IS_PERM_REGEX.matcher(m).find() 
//		&& !packageName.contains("UriPermission") 
//		&& !packageName.contains("MockPackageManager") 
//		&& !PERM_CHECK_NEG.matcher(m).find()) {

		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, IS_PERM_REGEX.toString()));
		r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_CLASS_RULE, "UriPermission")));
		r.addChild(FilterRule.genNot(new FilterRule(FilterRule.EQUAL_CLASS_RULE, "MockPackageManager")));
		r.addChild(FilterRule.genNot(new FilterRule(FilterRule.REGEX_RULE, PERM_CHECK_NEG.toString())));
		rules.add(r);		
		
		
////		} else if (PERM_ENSURE_REGEX.matcher(m).find()) {
		r = GenFilterXmlOutput.genDefaultRegexRule();
		r.addChild(new FilterRule(FilterRule.REGEX_RULE, PERM_ENSURE_REGEX.toString()));
		rules.add(r);
		
		GenFilterXmlOutput.generateXml(new File("/Users/benandow/Desktop/newcqddb.xml"), rules);
	}
	
	
	//// ------------------------ ACTUAL REGEXES -----------------------------------

	//STARTS WITH CHECK/ENFORCE -- generic (this can almost replace PERM_CHKENFORCE_REGEX|SIG_REGEX1|PKG_CHKENFORCE_REGEX|SYSTEM_ID_REGEX|DEVPROF_OWNER_REGEX1)
	private static final Pattern CHK_ENFORCE_REGEX = Pattern.compile("^enforce\\s("
			+ "can\\smanage\\s(ca\\scerts|application\\srestrictions|device\\sadmin|installed\\skeys)|"
			+ "not\\s(isolated\\scaller|managed\\sprofile)|"
			+ "shell(\\srestriction)?|"
			+ "manage(d)?\\s(users|profile)|"
			+ "(valid\\s)?u(ser(\\sid)?|id)(\\sunlocked)?|"
			+ "policy\\saccess|carrier\\sprivilege"
			+ ")\\b");

	
	//verifyBroadcastLocked, verifySignaturesLP, verifyIncomingUid
	// BENANDOW NEW
	private static final Pattern VERIFY_REGEX = Pattern.compile("^verify\\s(broadcast\\slocked|service\\strusted|calling\\spackage|caller|signatures\\slp|(incoming|calling)\\suid|([a-z]+\\s)*permission)\\b");

	//Generic permission checks
	private static final Pattern PERM_CHKENFORCE_REGEX = Pattern.compile("^(enforce|has|check)\\s([a-z\\s]+\\s)?permission(s)?\\b");
	private static final Pattern PERM_CHECK_NEG = Pattern.compile("\\b((shortcut\\shost|has\\ssms|scan\\sresult|accessory|device|package\\srequesting)\\spermission(s)?)|and\\screate\\suri\\sdata\\b");
//	private static final Pattern PERM_NEG_REGEX2 = Pattern.compile("\\band\\screate\\suri\\sdata\\b");

	//ensureXpermission
//	ensureHardwarePermission com.android.server.tv.TvInputManagerService$ServiceCallback
//	ensureParentalControlsPermission com.android.server.tv.TvInputManagerService$BinderService
//	ensureShortcutPermission com.android.server.pm.LauncherAppsService$LauncherAppsImpl
//	ensureShortcutPermission com.android.server.pm.LauncherAppsService$LauncherAppsImpl
	private static final Pattern PERM_ENSURE_REGEX = Pattern.compile("^ensure\\s[a-z\\s]+\\spermission$");

	
	// Check/enforce signature
	private static final Pattern SIG_REGEX1 = Pattern.compile("^(check|enforce|compare|verify)\\s(uid\\s)?signatures(\\scompat)?$");
	
	//Generic package checks -- REMOVED IS
	private static final Pattern PKG_CHKENFORCE_REGEX = Pattern.compile("^("
			+ "enforce\\s([a-z\\s]+\\s)?package(s)?|"
			+ "ensure\\scall(er|ing)\\spackage|"
			+ "check\\s("
				+ "(if\\s)?package(s)?\\s(startable|name|policy\\saccess|match|belongs\\sto\\suid)|"
				+ "carrier\\sprivileges\\sfor\\spackage|"
				+ "source\\spackage\\ssame"
				+ ""
			+ ")"
			+ ")\\b");


	
	// Generic system check
	private static final Pattern SYSTEM_ID_REGEX = Pattern.compile("^(check|enforce)\\s([a-z\\s]+\\s)?system\\b");
	

	private static final Pattern ADMIN_REGEX = Pattern.compile("^("
			+ "(is|enforce)\\s(user\\sadmin|admin\\suser)|"
			+ "(get|is)\\sactive\\sadmin\\s"
			+ "("
				+ "with\\spolicy(\\sfor\\s(caller|uid|user))?|"
				+ "for\\s(caller|uid|user)|"
				+ "unchecked"
			+ ")(\\slocked)?"
			+ ")\\b");



	private static final Pattern DEVPROF_OWNER_REGEX = Pattern.compile("^("
			+ "is\\s((caller\\s)?(device|profile)\\s)owner(\\s("
				+ "managed\\ssingle\\suser\\sdevice|"
				+ "app(\\s(on\\s(any|calling)\\suser(\\sinner)?))?"
			+ "))?|"
			+ "has\\sdevice\\sowner(\\sor\\sprofile\\sowner)?"
			+ ")$");
	
	private static final Pattern DEVPROF_OWNER_REGEX1 = Pattern.compile("^("
			+ "enforce\\s(owner\\srights|can\\s(manage|set)\\s(device|profile)\\s(and\\sdevice\\s)?owner(s)?|same\\sowner)|"
			+ "check\\s(uri\\sowner\\slocked|show\\sto\\sowner\\sonly|set\\sdevice\\sowner\\spre\\scondition)|"
			+ "ensure\\sdevice\\sowner\\smanaging\\ssingle\\suser"
			+ ")\\b");

	
	//----------
	
	
	private static final Pattern IS_APP_REGEX = Pattern.compile("^(is\\s"
			+ "(caller\\s(application\\srestrictions\\smanaging\\spackage|same\\sapp)|(same|m)\\sapp)"
			+ ")\\b");

	
	// FIXME The following two need to be handled differently...
	private static final Pattern PERM_REGEX3 = Pattern.compile("^permission\\s(to\\sop\\scode|info\\sfootprint|is\\sgranted)$");
	// This one is very specific findPermissionTreeLp | calculateCurrentPermissionFootprintLocked
	private static final Pattern PERM_REGEX4 = Pattern.compile("^(find\\spermission\\stree\\slp|calculate\\scurrent\\spermission\\sfootprint\\slocked)$");
	
	// Calling package checks
	private static final Pattern IS_CALLING_PKG_REGEX = Pattern.compile("^is\\s(calling\\s)?package\\sallowed\\sto\\s[a-z\\s]+");
	
	//IS KEYGUARD SECURE
	private static final Pattern KEYGUARD_SEC_REGEX = Pattern.compile("^is\\s(keyguard|system)\\ssecure$");

	private static final Pattern KEYGUARD_SEC_REGEX2 = Pattern.compile("^is\\ssecure$"); // && KeyguardService in classname or LockPatternUtils in classname
	
	private static String AC_TERMS = "isolated|privileged|account\\smanaged";
	private static String SENS_TERMS = "cert\\sinstaller|test\\sharness|uid|security\\sviolation";
	private static final Pattern IS_REGEX = Pattern.compile(String.format("\\bis\\s([a-z\\s+]+\\s)?(%s|%s)\\b", AC_TERMS, SENS_TERMS));
	
	private static final Pattern IS_PROFILE_REGEX = Pattern.compile("^is\\s(same|current|managed)\\sprofile(\\s(group(\\slp)?|locked))?$");

		
	private static final Pattern IS_PERM_REGEX = Pattern.compile("^is\\s("
			+ "((read|write|runtime|package\\srequesting|install)\\s)?permission(s)?(\\s(user\\supdated|review\\srequired|revoked\\sby\\spolicy|key))?"
			+ ")$");

	
	//IS (DIS|EN)ABLED
	//BENANDOW NEW RULES
	private static final Pattern IS_ENABLED_REGEX1 = Pattern.compile("^is\\s((component|(([a-z]+\\s)+)provider)\\s)?enabled\\sfor\\s(current\\s)?(profile|package|user)(s)?$");
	private static final Pattern IS_ENABLED_REGEX2 = Pattern.compile("^is\\s(component|lock\\s(pattern|password))\\senabled\\b");
	// checkSignatures and NOT Mock!!!
	private static final Pattern CHK_REGEX1 = Pattern.compile("^check\\s("
			+"((audio|notification)\\s)?op(eration)?(\\sno\\sthrow)?|"
			+ "(if\\s)?caller\\s(interact\\sacross\\susers\\sfull|can\\saccess\\sscan\\sresults|is\\s(current\\suser\\sor\\sprofile|same\\sapp|provider|self\\sor\\sforeground\\suser))|"
			+ "((embedded|device\\sstats|(update\\s)?app\\sop(p(s)?|s)|app\\sswitch)\\sallowed(\\s(inner|locked))?)|"
			+ "(allow\\sbackground|restriction)\\slocked|"
			+ "and\\snote\\s(write\\ssettings|change\\snetwork\\sstate|draw\\soverlays)\\soperation|"
			+ "authority\\sgrants|"
			+ "resolution\\slevel\\sis\\ssufficient\\sfor\\s(geofence|provider)\\suse|"
			+ "peers\\smac\\saddress|"
			+ "interact\\sacross\\susers\\sfull|"
			+ "(location|policy)\\saccess"
			+ ")\\b"
		);
	
	private static final Pattern ISPROT_REGEX = Pattern.compile("^is\\s("
			+ "protected\\s(broadcast|package)|"
			+ "package\\s((data|state)\\sprotected)|"
			+ "(package\\s)?granted|"
			+ "(cross|same|compatible)\\suser(\\sid)?|" //TODO may want to move rule
			+ "listener\\spackage" // TOO specific
			+ ")\\b");	
	
	// Has patterns
	private static final Pattern HAS_REGEX = Pattern.compile("^has\\s"
				+ "("
				+ "access|granted|([a-z\\s]+\\s)?(restriction|privilege)(s)?|"
				+ "[a-z\\s]+\\sinstalled|granted\\spolicy"
				+ ")$"
			);
		
	//Get with protected resources
	private static final Pattern GETPROT_REGEX = Pattern.compile("^get\\s"
			+ "("
			+ "app\\sstart\\smode|"
			+ "(bluetooth|camera|screen)\\s([a-z\\s]+\\s)?(disabled|enabled)|"
			+ "((minimum|(caller\\s)?allowed)\\s)?resolution\\slevel|"
			+ "[a-z\\s]+\\sfor\\scalling\\spackage"
			+ ")\\b"
		);
	
	//Get patterns
	private static final Pattern GET_REGEX = Pattern.compile("^get\\s([a-z\\s]+\\s)?((do\\snot\\sask|account)\\scredentials\\s(on\\sboot)|privilege)\\b");
	
	// can <OPERATION>
	private static final Pattern OPERS_REGEX = Pattern.compile(String.format("^can\\s("
			+ "clear\\sidentity|"
			+ "draw\\soverlays|"
			+ "run\\shere|"
			+ "user\\smodify\\saccounts|"
			+ "access\\sapp\\swidget|"
			+ "read\\sphone\\s(state|number)|"
			+ "caller\\saccess\\smock"
			+ ")\\b"));


	//Get patterns
	private static final Pattern HANDLE_REGEX = Pattern.compile("^handle\\sincoming\\suser\\b");
	
	private static final Pattern IS_ALLOWED_REGEX = Pattern.compile("^is\\s(get\\stasks|mount|default)\\s(dis)?allowed\\b");

	private static final Pattern ISLOCK_REGEX = Pattern.compile("^is\\s(current\\suser\\slocked|local\\sunlocked\\suser|allowed\\sby\\s(current\\s)?user\\ssettings\\slocked)\\b");

	private static final Pattern IS_PSSWD_SUFF_REGEX = Pattern.compile("^is\\sactive\\spassword\\ssufficient$");
	
	// Is user or system Identity
	private static final Pattern IS_UORS_REGEX = Pattern.compile("^is\\s("
			+ "((updated|caller|required\\sfor)\\s)?system(\\sonly)?(\\s(app|user))?"
			+ ")$");

	//RESTRICTION REGEX
	private static final Pattern IS_RESTRICT_REGEX = Pattern.compile("^is\\s(op|user|access)\\srestricted\\b");

	private static final Pattern VMAIL_REGEX = Pattern.compile("^(check\\s)?(caller|package)\\shas\\s([a-z]+\\s)*(permission|access)$");
	
	private static final Pattern PERMIT_RULES = Pattern.compile("^(is\\s((accessibility\\sservice|input\\smethod)\\s)?permitted(\\s(by\\sadmin|shell\\sbroadcast))?|check\\sread\\saccounts\\spermitted)$");

	
	//---------------------------
	//SPECIAL RULES
	//App ops
	//	private static final Pattern APPOPS_REGEX = Pattern.compile("\\bapp\\sops\\b");
	private static final Pattern OP_REGEX = Pattern.compile("^((note|start)\\s(proxy\\s)?op(eration)?(s)?)|((proxy\\s)?op(eration)?(s)?\\sallow)\\b");
	private static final Pattern APPOP_CHKPKG_REGEX = Pattern.compile("^check\\spackage$");
	private static final Pattern APPOP_UNCHECKED_REGEX = Pattern.compile("\\bunchecked\\b"); 

	
	//Firewall
	private static final Pattern FWALL_REGEX = Pattern.compile("\\bmatch(es|ed)?$");

	//Exist pattern
	private static final Pattern PSWD_REGEX = Pattern.compile("\\b(password|pattern)\\sexists\\b");
	
	
	private static final Pattern UID_NOT_MATCH = Pattern.compile("\\b"
			+ "(blocking\\suid|"
			+ "uid\\s(idle|enumeration|(state\\s)?foreground))\\b");
	
	//Cross Profile checks (Multiuser?)
	private static final Pattern CROSSPROF_REGEX1 = Pattern.compile("^get\\scross\\sprofile\\s[a-z\\s]+\\s(disabled|enabled)\\b");

	
	private static boolean checkFilter(String methodName, String packageName) {
		String m = MethodParseUtil.parseMethodOrField(methodName);
		
		// Special rules for Firewall and AppOps
		//Handle firewall
		if (packageName.contains("com.android.server.firewall.") && FWALL_REGEX.matcher(m).find() && !(methodName.equals("matchesValue") && packageName.equals("com.android.server.firewall.StringFilter"))) { //TODO narrow down with "Filter" in classname?			
			return true;
		} else if (packageName.contains("AppOps") && !APPOP_UNCHECKED_REGEX.matcher(m).find() && (OP_REGEX.matcher(m).find() || APPOP_CHKPKG_REGEX.matcher(m).find())) {
			return true;
		} else if (packageName.contains("LockPatternUtils") && PSWD_REGEX.matcher(m).find()) {
			return true;
		} else if (checkSpecialCase(packageName, methodName)) {
			return true;
		} else if (PERM_CHKENFORCE_REGEX.matcher(m).find() && !packageName.contains("android.test.") && !packageName.contains("com.android.future.usb.") &&  !packageName.contains("com.android.packageinstaller.permission.model.") && !packageName.contains("MockContext") && !packageName.contains("MockPackageManager") && !PERM_CHECK_NEG.matcher(m).find() && !(methodName.equals("checkAddPermission") && packageName.equals("android.view.WindowManagerPolicy"))) { //
			return true;
		} else if (PERM_REGEX3.matcher(m).find() && !packageName.contains("MockContext") && !packageName.contains("MockPackageManager")) {
			return true;
		} else if (PERM_REGEX4.matcher(m).find() && !packageName.contains("MockContext") && !packageName.contains("MockPackageManager")) {
			return true;
		} else if (SIG_REGEX1.matcher(m).find() && !packageName.contains("MockContext") && !packageName.contains("MockPackageManager")) {
			return true;
		} else if (PKG_CHKENFORCE_REGEX.matcher(m).find()) {
			return true;
		} else if (IS_CALLING_PKG_REGEX.matcher(m).find()) {
			return true;
		}  else if (SYSTEM_ID_REGEX.matcher(m).find()) {
			return true;
		} else if (ADMIN_REGEX.matcher(m).find()) {
			return true;
		} else if (DEVPROF_OWNER_REGEX.matcher(m).find()) { 
			return true;
		} else if (DEVPROF_OWNER_REGEX1.matcher(m).find()) {
			return true;
		} else if (CROSSPROF_REGEX1.matcher(m).find()) {
			return true;
		} else if (CHK_ENFORCE_REGEX.matcher(m).find() && !packageName.contains("MockContext") && !packageName.contains("MockPackageManager")) {
			return true;
		} else if (IS_REGEX.matcher(m).find() && !packageName.contains("CredentialHelper") && !packageName.contains("MockPackageManager") && !packageName.contains("android.icu.text.") && !UID_NOT_MATCH.matcher(m).find() && !(methodName.equals("isIsolated") && packageName.equals("com.android.systemui.statusbar.phone.NotificationGroupManager"))) {
			return true;
		} else if (HAS_REGEX.matcher(m).find() && !packageName.contains("RestrictedLockUtils")) {
			return true;
		} else if (GET_REGEX.matcher(m).find() && !packageName.contains("com.android.internal.telephony.uicc.")) {
			return true;
		} else if (GETPROT_REGEX.matcher(m).find()) {
			return true;
		} else if (OPERS_REGEX.matcher(m).find()) {
			return true;
		} else if (HANDLE_REGEX.matcher(m).find()) {
			return true;
		} else if (IS_ENABLED_REGEX1.matcher(m).find()) {
			return true;
		} else if (IS_ENABLED_REGEX2.matcher(m).find()) {
			return true;
		} else if (VERIFY_REGEX.matcher(m).find()) {
			return true;
		} else if (CHK_REGEX1.matcher(m).find()) {
			return true;
		} else if (ISPROT_REGEX.matcher(m).find() && !packageName.contains("MockPackageManager") && !packageName.contains("android.content.pm.permission.") && !packageName.contains("com.android.packageinstaller.permission.model.")) {
			return true;
		} else if (IS_ALLOWED_REGEX.matcher(m).find()) {
			return true;
		} else if (ISLOCK_REGEX.matcher(m).find()) {
			return true;
		} else if (IS_PSSWD_SUFF_REGEX.matcher(m).find()) {
			return true;
		} else if (IS_APP_REGEX.matcher(m).find()) {
			return true;
		} else if (IS_UORS_REGEX.matcher(m).find() && !packageName.contains("KeyEvent") && !packageName.contains("ActivityThread") && !packageName.contains("MediaRouter") && !packageName.contains("TrustedCertificateStore") && !packageName.contains("ZenModeFiltering") && !packageName.contains("com.android.packageinstaller.permission.utils.")) {
			return true;
		} else if (IS_RESTRICT_REGEX.matcher(m).find()) {
			System.out.println(m);
return true;
		} else if (VMAIL_REGEX.matcher(m).find() && packageName.contains("VoicemailPermissions")) {
			return true;
		} else if (PERMIT_RULES.matcher(m).find()) {
			return true;
		} else if (KEYGUARD_SEC_REGEX.matcher(m).find() && (!packageName.contains("WindowManagerPolicy") && !packageName.contains("QuickActivity") && !packageName.contains("BaseStatusBar"))) {
			return true;
		} else if (KEYGUARD_SEC_REGEX2.matcher(m).find() && (packageName.contains("KeyguardService") || packageName.contains("KeyguardState") || packageName.contains("LockPattern"))) {
			return true;
		} else if (IS_PROFILE_REGEX.matcher(m).find()) {
			return true;
		} else if (IS_PERM_REGEX.matcher(m).find() && !packageName.contains("UriPermission") && !packageName.contains("MockPackageManager") && !PERM_CHECK_NEG.matcher(m).find()) {
			return true;
		} else if (PERM_ENSURE_REGEX.matcher(m).find()) {
			return true;
		}
		
//		System.out.println("\""+packageName + "." + methodName+"\",");
		
		return false;
	}
	
	private static boolean checkSpecialCase(String packageName, String methodName) {		
		// Parse the rulse out in the format that we're comparing them...
		String[] rules = new String[methodSignatures.length];
		for (int i = 0; i < methodSignatures.length; i++) {
			Method m = TestFileParser.parseLine(methodSignatures[i]);
			rules[i] = m.getmPackage()+"."+m.getName();			
		}
		
		String sig = packageName+"."+methodName;
		for (String rule : rules) {
			if (sig.equals(rule)) {
				return true;
			}
		}
		return false;
	}
	
	public static List<Method> filterMethods(List<Method> methods) {
		List<Method> result = new ArrayList<Method>();
		for (Method m : methods) {
			if (ContextQueryFilter.checkFilter(m.getName(), m.getmPackage())) {
				result.add(m);
			} else if (ContextQueryFilter.VERBOSE) {
				System.out.println("\t"+m.getName() + "\t\t" + m.getmPackage());
			}
		}		
		return result;
	}	
}