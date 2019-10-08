#!/usr/bin/env python

IGNORE_ARGS = True

def detectMethod(name): #FIXME this is dumb, but does it seems to work out....
	return name.startswith('<') and '>' in name and '(' in name

def getFirstArgInternal(argStr):
	# Extract permission check
	beginSplit = 0
	endSplit = 0
	for idc in range(0 , len(argStr)):
		if argStr[idc] == '"': # Begin string constant
			beginSplit = idc + 1
			for jdc in range(idc + 1, len(argStr)):
				if argStr[jdc] == '"': # End string constant
					endSplit = jdc
					break
#			print "\tSTRVAL  = ", argStr[beginSplit:endSplit]
			return ( '"' + argStr[beginSplit:endSplit] + '"', endSplit)
		elif argStr[idc] == '<':
			beginSplit = idc
			mdepth = 1
			for jdc in range(idc + 1, len(argStr)):
				if argStr[jdc] == '>': # End string constant
					mdepth -= 1
				if mdepth <= 0:
					endSplit = jdc + 1
					break
				endSplit = jdc
			# Now let's get the rest of the string (i.e., args)

			if argStr[endSplit] == '(':
				mdepth = 1	
				for i in range(endSplit + 1, len(argStr)):
					if argStr[i] == '(':
						mdepth += 1
					elif argStr[i] == ')':
						mdepth -= 1
					if mdepth == 0:
						endSplit = i + 1
						break

#			print "\tSTRVAL  = ", argStr[beginSplit:endSplit], endSplit, "\t\t", argStr[endSplit + 1:]
			return (argStr[beginSplit:endSplit], endSplit)
		elif argStr[idc:idc+3] == 'ALL':
			return ('ALL', idc+3)
		elif argStr[idc] >= '0' and argStr[idc] <= '9':
			# Get number...
			numStr = argStr[idc]	
			for nid in range(idc + 1, len(argStr)):
				if argStr[nid] < '0' or argStr[nid] > '9':
					break
				numStr += argStr[nid]
				endSplit = nid + 1
			return (numStr, endSplit)
	return (None, None)

def getFirstArg(argStr):
	return getFirstArgInternal(argStr)[0]

def getSecondArg(argStr):
	arg1,end_arg1 = getFirstArgInternal(argStr)
	return getFirstArgInternal(argStr[end_arg1 + 1:])[0]

def getFourthAndFifthArgs(argStr):
	arg1,end_arg1 = getFirstArgInternal(argStr.strip())	# First
	arg2,end_arg2 = getFirstArgInternal(argStr[end_arg1 + 1:].strip()) #Second
	arg3,end_arg3 = getFirstArgInternal(argStr[end_arg2 + 1:].strip()) #Third
	arg4,end_arg4 = getFirstArgInternal(argStr[end_arg3 + 1:].strip()) # Fourth
	arg5,end_arg5 = getFirstArgInternal(argStr[end_arg4 + 1:]) # Fifth
	return (arg4, arg5)


def getFourthArg(argStr):
	arg1,end_arg1 = getFirstArgInternal(argStr.strip())	# First
	arg2,end_arg2 = getFirstArgInternal(argStr[end_arg1 + 1:].strip()) #Second
	arg3,end_arg3 = getFirstArgInternal(argStr[end_arg2 + 1:].strip()) #Third
	arg4,end_arg4 = getFirstArgInternal(argStr[end_arg3 + 1:].strip()) # Fourth
	return arg4

def getFifthArg(argStr):
	arg1,end_arg1 = getFirstArgInternal(argStr.strip())	# First
	arg2,end_arg2 = getFirstArgInternal(argStr[end_arg1 + 1:].strip()) #Second
	arg3,end_arg3 = getFirstArgInternal(argStr[end_arg2 + 1:].strip()) #Third
	arg4,end_arg4 = getFirstArgInternal(argStr[end_arg3 + 1:].strip()) # Fourth
	arg5,end_arg5 = getFirstArgInternal(argStr[end_arg4 + 1:]) # Fifth
	return arg5

def getEigthArg(argStr):
	arg1,end_arg1 = getFirstArgInternal(argStr.strip())	# First
	arg2,end_arg2 = getFirstArgInternal(argStr[end_arg1 + 1:].strip()) #Second
	arg3,end_arg3 = getFirstArgInternal(argStr[end_arg2 + 1:].strip()) #Third
	arg4,end_arg4 = getFirstArgInternal(argStr[end_arg3 + 1:].strip()) # Fourth
	arg5,end_arg5 = getFirstArgInternal(argStr[end_arg4 + 1:]) # Fifth

	arg6,end_arg6 = getFirstArgInternal(argStr[end_arg5 + 1:]) # Sixth
	arg7,end_arg7 = getFirstArgInternal(argStr[end_arg6 + 1:]) # Seventh
	arg8,end_arg8 = getFirstArgInternal(argStr[end_arg7 + 1:]) # Eighth

	return arg8


def parseMethod(name):
	startIndex = name.index('>') + 1
	endIndex = 0
	parenDepth = 0
	for i in range(startIndex, len(name)):
		if name[i] == '(':
			parenDepth += 1
		elif name[i] == ')':
			parenDepth -= 1
		if parenDepth == 0:
			endIndex = i + 1
			break


	if name[:startIndex] in ['<com.android.server.pm.PackageManagerService: int checkUidPermission(java.lang.String,int)>',
							 '<com.android.server.pm.PackageManagerService: int checkPermission(java.lang.String,java.lang.String,int)>',
							 '<com.android.server.vr.VrManagerService: boolean isPermissionUserUpdated(java.lang.String,java.lang.String,int)>',
							 '<android.app.ApplicationPackageManager: int getPermissionFlags(java.lang.String,java.lang.String,android.os.UserHandle)>',
							 '<android.os.SystemProperties: java.lang.String get(java.lang.String)>',
							 '<com.android.server.pm.UserManagerService: boolean hasUserRestriction(java.lang.String,int)>',
							 '<android.os.Process: int[] getPids(java.lang.String,int[])>',
							 '<android.os.SystemProperties: java.lang.String get(java.lang.String,java.lang.String)>'
							]:
		# 1st arg
		arg1 = getFirstArg(name[startIndex + 1 : endIndex])
		return name[:startIndex] + '(' + arg1 +', ...)' + name[endIndex:]
#		return (name[:startIndex] + '(' + firstArg +')' + name[endIndex:], name[startIndex:endIndex])
	elif name[:startIndex] in [
							  '<android.provider.Settings$Secure: boolean isLocationProviderEnabledForUser(android.content.ContentResolver,java.lang.String,int)>',
							  '<android.os.SystemProperties: java.lang.String get(java.lang.String)>'
								]:
		# 2nd arg
		arg2 = getSecondArg(name[startIndex + 1 : endIndex])
		return name[:startIndex] + '(..., ' + arg2 +' ,...)' + name[endIndex:]
	
	elif name[:startIndex] in [
								'<com.android.server.am.ActivityManagerService: boolean checkAuthorityGrants(int,android.content.pm.ProviderInfo,int,boolean)>',
								]:
		arg4 = getFourthArg(name[startIndex + 1 : endIndex])
		return name[:startIndex] + '(..., '  + arg4 +', ...)' + name[endIndex:]

	elif name[:startIndex] in [
								'<com.android.server.am.ActivityStackSupervisor: int getComponentRestrictionForCallingPackage(android.content.pm.ActivityInfo,java.lang.String,int,int,boolean)>'
								]:
		arg5 = getFifthArg(name[startIndex + 1 : endIndex])
		return name[:startIndex] + '(..., '  + arg5 +', ...)' + name[endIndex:]

	elif name[:startIndex] in [
								'<com.android.server.am.UserController: int handleIncomingUser(int,int,int,boolean,int,java.lang.String,java.lang.String)>',
								]:
		# Args 4 and 5
		arg4,arg5 = getFourthAndFifthArgs(name[startIndex + 1 : endIndex])
		return name[:startIndex] + '(..., ' + arg4 + ', ' + arg5 +', ...)' + name[endIndex:]
	elif name[:startIndex] in [
								'<com.android.server.am.ActivityStackSupervisor: boolean checkStartAnyActivityPermission(android.content.Intent,android.content.pm.ActivityInfo,java.lang.String,int,int,int,java.lang.String,boolean,com.android.server.am.ProcessRecord,com.android.server.am.ActivityRecord,com.android.server.am.ActivityStack,android.app.ActivityOptions)>',
								]:
		arg8 = getEigthArg(name[startIndex + 1 : endIndex])
		return name[:startIndex] + '(..., '  + arg8 +', ...)' + name[endIndex:]


	return name[:startIndex]  + "(...)" + name[endIndex:]
#	return (name[:startIndex] + name[endIndex:], name[startIndex:endIndex])


def getSimplifiedValInner(cp):
	return cp if not detectMethod(cp) else parseMethod(cp)

def getSimplifiedValStr(cp):
	if type(cp) == str:
		return getSimplifiedValInner(cp)
	return [getSimplifiedValInner(cp[0]), getSimplifiedValInner(cp[1])]


def genControlPredStr(v):
	return '`' +  v + '`' if type(v) == str else '{%s}' % ( ','.join( [ '`' + i + '`' for i in v ] ), )

def simplifyPredicate(cp):
	if not IGNORE_ARGS:
		return cp

	# First let's split the control predicate by `
	cp = [ v for v in cp.split('`') if len(v) > 0 and v != ',' ]

	if len(cp) == 1:
		cp = cp[0]

	strippedCntrPr = getSimplifiedValStr(cp)
	return genControlPredStr(strippedCntrPr)



if __name__ == '__main__':
	print "Starting Test"

	TEST_CASES = [
			'`<com.android.server.pm.PackageManagerService: int checkUidPermission(java.lang.String,int)>(<android.content.pm.PathPermission: java.lang.String getWritePermission()>(), <com.android.server.pm.PackageManagerService: int getPackageUid(java.lang.String,int,int)>(<com.android.server.am.ActivityRecord: java.lang.String packageName>, 268435456, <android.os.UserHandle: int getUserId(int)>(<android.content.pm.ApplicationInfo: int uid>)))`',
			'`<com.android.server.pm.PackageManagerService: int checkUidPermission(java.lang.String,int)>("android.permission.INTERACT_ACROSS_USERS_FULL", <android.content.pm.ApplicationInfo: int uid>)`',
		'`0`,`<android.content.pm.ApplicationInfo: int flags> & 8`',
		'`<android.os.UserHandle: int getUid(int,int)>(<com.android.server.am.ProcessRecord: int userId>, -1)`,`<android.util.SparseArray: int keyAt(int)>(-2 + <android.util.SparseArray: int size()>())[0]`',
		'`<com.android.server.am.ProcessRecord: int userId>`,`<com.android.server.am.UserController: int handleIncomingUser(int,int,int,boolean,int,java.lang.String,java.lang.String)>(-1, 1000, -1, 1, 0, "broadcast", NULL)`',
		'`<android.provider.Settings$Secure: boolean isLocationProviderEnabledForUser(android.content.ContentResolver,java.lang.String,int)>(ALL, "gps", -2)`'
	]

	for tc in TEST_CASES:
		print simplifyPredicate(tc) + '\n\n'
