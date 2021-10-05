#!/usr/bin/env python
import base64
import subprocess

#java -jar acminer_query-fat.jar -e "<com.android.server.am.ActivityManagerService: int startActivityAsUser_AOSP(android.app.IApplicationThread,java.lang.String,android.content.Intent,java.lang.String,android.os.IBinder,java.lang.String,int,int,android.app.ProfilerInfo,android.os.Bundle,int)>;" -d "AOSP;INPUT_RES/aosp-7.1.1/acminer/acminer_db/_acminer_db_.xml"

def query(entrypoint, authCheck, filepath):
	return [ e for e in subprocess.check_output(["java", "-jar", "acminer_query-fat.jar", "-e", entrypoint, "-p", base64.urlsafe_b64encode(authCheck), "-d", filepath]).split('\n') if e is not None and len(e) > 0]
