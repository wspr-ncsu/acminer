package org.sag.acminer.database.acminer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sag.common.io.FileHelpers;
import org.sag.common.tools.SortingMethods;
import org.sag.soot.SootSort;

//import sun.misc.Unsafe;

public class ACMinerQuery {
	
	private static final Pattern methodSigPat = Pattern.compile("^<([^:]+):\\s+([^\\s]+)\\s+([^\\(]+)\\(([^\\)]*)\\)>$");
	
	private boolean hasNoKeys;
	private Map<String, IACMinerDatabase> keyToDatabases;
	private Map<String, Set<String>> keyToEps;
	private String pair;
	private boolean dumpStats;
	
	public ACMinerQuery() {
		this.pair = null;
		this.keyToDatabases = new HashMap<>();
		this.keyToEps = new HashMap<>();
		this.hasNoKeys = false;
		this.dumpStats = false;
	}
	
	public boolean parseAndSetup(String[] args){
		try {
			if(args == null || args.length <= 0) {
				return handleError("No arguments provided.");
			} else {
				Map<String,String> databaseNameToFile = new HashMap<>();
				List<String> eps = new ArrayList<>();
				for(int i = 0; i < args.length; i++) {
					switch(args[i]) {
						case "-h":
						case "--help":
							System.out.println(errmsg);
							return false;
						case "-e":
							String[] splits = args[++i].split(";");
							for(String s : splits)
								eps.add(s.trim());
							break;
						case "-p":
							this.pair = new String(Base64.getUrlDecoder().decode(args[++i]), StandardCharsets.UTF_8);
							break;
						case "-d":
							String in = args[++i];
							int index = in.indexOf(';');
							if(index < 0) {
								hasNoKeys = true;
								databaseNameToFile.put("", in.trim());
							} else if(index == 0) {
								hasNoKeys = true;
								databaseNameToFile.put("", in.substring(1).trim());
							} else {
								databaseNameToFile.put(in.substring(0, index).trim(), in.substring(index + 1).trim());
							}
							break;
						case "--stats":
							dumpStats = true;
							break;
						default:
							return handleError("Unrecognized argument '" + args[i] + "'.");
					}
				}
				
				if(this.pair == null && !dumpStats)
					return handleError("No authorization check provided.");
				if(eps.isEmpty() && !dumpStats)
					return handleError("No entry points provided.");
				if(databaseNameToFile.isEmpty())
					return handleError("No databases provided.");
				if(hasNoKeys && databaseNameToFile.size() != 1)
					return handleError("Expected only a single database.");
				
				for(String key : databaseNameToFile.keySet()) {
					String sPath = databaseNameToFile.get(key);
					Path p = FileHelpers.getPath(sPath);
					if(!FileHelpers.checkRWFileExists(p)) 
						return handleError("Failed to located database file '" + p.toString() + "'.");
					
					IACMinerDatabase d = null;
					try {
						d = IACMinerDatabase.Factory.readXML(null, p);
					} catch(Throwable t) {
						return handleError("Failed to load database file '" + p.toString() + "'.",t);
					}
					keyToDatabases.put(key, d);
					keyToEps.put(key, new HashSet<>());
				}
				
				if(!dumpStats) {
					for(String ep : eps) {
						Matcher m = methodSigPat.matcher(ep);
						if(m.matches()) {
							if(hasNoKeys) {
								keyToEps.get("").add(ep);
							} else {
								String className = m.group(1);
								String retType = m.group(2);
								String methodName = m.group(3);
								String parms = m.group(4);
								
								int index = methodName.lastIndexOf('_');
								String name;
								String key;
								if(index > 0) {
									key = methodName.substring(index + 1);
									name = methodName.substring(0, index);
								} else {
									return handleError("Could not determine the key for entry point '" + ep + "'.");
								}
								
								String newEp = "<" + className + ": " + retType + " " + name + "(" + parms + ")>";
								
								Set<String> temp = keyToEps.get(key);
								if(temp == null) {
									return handleError("No matching database for key '" + key + "for entry point '" + ep + "'.");
								}
								temp.add(newEp);
							}
						} else {
							return handleError("Failed to parse entry point '" + ep + "'.");
						}
					}
				}
				
				keyToDatabases = SortingMethods.sortMapKey(keyToDatabases,SortingMethods.sComp);
				
				if(!dumpStats) {
					for(String key : keyToEps.keySet()) {
						keyToEps.put(key,SortingMethods.sortSet(keyToEps.get(key),SootSort.smStringComp));
					}
					keyToEps = SortingMethods.sortMapKey(keyToEps, SortingMethods.sComp);
				}
				
				return true;
			}
		} catch(Throwable t) {
			return handleError("Unexpected error when parsing input arguments and importing data.",t);
		}
	}
	
	private String dumpStatsForDatabase(IACMinerDatabase db, String spacer) {
		Map<String, Map<String, Set<Doublet>>> stubToEPToPairs = db.getStringValuePairs();
		int stubsCount = stubToEPToPairs.size();
		int stubsWLogicCount = 0;
		int epsCount = 0;
		int epsWLogicCount = 0;
		for(String stub : stubToEPToPairs.keySet()) {
			boolean stubHasLogic = false;
			Map<String, Set<Doublet>> epToPairs = stubToEPToPairs.get(stub);
			epsCount += epToPairs.size();
			for(String ep : epToPairs.keySet()) {
				Set<Doublet> pairs = epToPairs.get(ep);
				if(!pairs.isEmpty()) {
					epsWLogicCount++;
					stubHasLogic = true;
				}
			}
			if(stubHasLogic)
				stubsWLogicCount++;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(spacer).append("Stubs With Logic: ").append(stubsWLogicCount).append("/").append(stubsCount).append("\n");
		sb.append(spacer).append("Entry Points With Logic: ").append(epsWLogicCount).append("/").append(epsCount);
		return sb.toString();
	}
	
	public boolean runQuery() {
		try {
			if(dumpStats) {
				if(hasNoKeys) {
					System.out.println(dumpStatsForDatabase(keyToDatabases.get(""),""));
				} else {
					for(String key : keyToDatabases.keySet()) {
						System.out.println(key);
						System.out.println(dumpStatsForDatabase(keyToDatabases.get(key),"  "));
					}
				}
				return true;
			} else {
				Map<String,Set<String>> sourcesToKeys = new HashMap<>();
				for(String key : keyToEps.keySet()) {
					Set<String> eps = keyToEps.get(key);
					if(!eps.isEmpty()) {
						IACMinerDatabase db = keyToDatabases.get(key);
						for(String ep : eps) {
							Set<String> sources = db.getSourceMethodsForPairInEntryPoint(ep, pair);
							for(String source : sources) {
								Set<String> keys = sourcesToKeys.get(source);
								if(keys == null) {
									keys = new HashSet<>();
									sourcesToKeys.put(source, keys);
								}
								keys.add(key);
							}
						}
					}
				}
				
				for(String source : sourcesToKeys.keySet()) {
					sourcesToKeys.put(source, SortingMethods.sortSet(sourcesToKeys.get(source),SortingMethods.sComp));
				}
				sourcesToKeys = SortingMethods.sortMapKey(sourcesToKeys, SootSort.smStringComp);
				
				StringBuilder sb = new StringBuilder();
				if(hasNoKeys) {
					for(String source : sourcesToKeys.keySet())
						sb.append(source).append("\n");
				} else {
					for(String source : sourcesToKeys.keySet()) {
						Set<String> keys = sourcesToKeys.get(source);
						Matcher m = methodSigPat.matcher(source);
						if(m.matches()) {
							String className = m.group(1);
							String retType = m.group(2);
							String methodName = m.group(3);
							String parms = m.group(4);
							sb.append("<").append(className).append(": ").append(retType).append(" ").append(methodName).append("_");
							boolean first = true;
							for(String key : keys) {
								if(first) {
									sb.append(key);
									first = false;
								} else {
									sb.append("|").append(key);
								}
							}
							sb.append("(").append(parms).append(")>").append("\n");
						} else {
							return handleError("Source method '" + source + "' did not parse correctly!?!");
						}
					}
				}
				System.out.print(sb.toString());
				return true;
			}
		} catch(Throwable t) {
			return handleError("Unexpected error when running the query.",t);
		}
	}
	
	public static void main(String[] args) {
		//disableWarning();
		ACMinerQuery o = new ACMinerQuery();
		if(o.parseAndSetup(args)) {
			if(o.runQuery())
				System.exit(0);
			else 
				System.exit(1);
		}
	}
	
	private static boolean handleError(String msg) {
		return handleError(msg,null);
	}
	
	private static boolean handleError(String msg, Throwable t) {
		StringBuilder sb = new StringBuilder();
		sb.append("Error: ").append(msg).append("\n");
		if(t != null) {
			StringWriter st = new StringWriter();
			t.printStackTrace(new PrintWriter(st));
			sb.append(st.toString()).append("\n");
		}
		sb.append(errmsg);
		System.out.println(sb.toString());
		return false;
	}
	
	//Disable the Illegal reflective access warning generated in java 9
	/*private static void disableWarning() {
	    try {
	        Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
	        theUnsafe.setAccessible(true);
	        Unsafe u = (Unsafe) theUnsafe.get(null);

	        Class<?> cls = Class.forName("jdk.internal.module.IllegalAccessLogger");
	        Field logger = cls.getDeclaredField("logger");
	        u.putObjectVolatile(cls, u.staticFieldOffset(logger), null);
	    } catch (Exception e) {
	        // ignore
	    }
	}*/
	
	private static final String errmsg = 
		    "Usage: [-h|--help] [-e <entry_points>] [-p <authorization_check>]\n"
		  + "       [-d <database_file>]\n [--stats]"
		  + "\nDescription:\n"
		  + "  Queries a given database or databases to find for the given entry\n"
		  + "  point(s) the source methods of the given authorization checks.\n"
		  + "  The output is written to standard out where each line contains a\n"
		  + "  single unique source method. If no source methods are found then\n"
		  + "  only a single new line character should be printed. If an error\n"
		  + "  occurs then a message starting with 'Error: ' will be printed,\n"
		  + "  ending with this message. Note on error the exit code is set to\n"
		  + "  1, otherwise it is always 0."
		  + "\nExample Input Arguments:\n"
		  + "  Ex 1:\n"
		  + "    -e \"<com.android.server.accounts.AccountManagerService: void hasFeatures(android.accounts.IAccountManagerResponse,android.accounts.Account,java.lang.String[],java.lang.String)>;"
		  + "<com.android.server.accounts.AccountManagerService: android.accounts.Account[] getAccountsAsUser(java.lang.String,int,java.lang.String)>;"
		  + "<com.android.server.accounts.AccountManagerService: void getAccountsByFeatures(android.accounts.IAccountManagerResponse,java.lang.String,java.lang.String[],java.lang.String)>\" "
		  + "-p \"YDxjb20uYW5kcm9pZC5zZXJ2ZXIucG0uUGFja2FnZU1hbmFnZXJTZXJ2aWNlOiBpbnQgY2hlY2tVaWRQZXJtaXNzaW9uKGphdmEubGFuZy5TdHJpbmcsaW50KT4oQUxMLCA8YW5kcm9pZC5vcy5CaW5kZXI6IGludCBnZXRDYWxsaW5nVWlkKCk-KCkpYA==\" "
		  + "-d \"..../acminer_db/_acminer_db_.xml\"\n"
		  + "  Ex 2:\n"
		  + "    -e \"<com.android.server.accounts.AccountManagerService: void hasFeatures_AOSP(android.accounts.IAccountManagerResponse,android.accounts.Account,java.lang.String[],java.lang.String)>;"
		  + "<com.android.server.accounts.AccountManagerService: android.accounts.Account[] getAccountsAsUser_HTC(java.lang.String,int,java.lang.String)>;"
		  + "<com.android.server.accounts.AccountManagerService: void getAccountsByFeatures_SONY(android.accounts.IAccountManagerResponse,java.lang.String,java.lang.String[],java.lang.String)>\" "
		  + "-p \"YDxjb20uYW5kcm9pZC5zZXJ2ZXIucG0uUGFja2FnZU1hbmFnZXJTZXJ2aWNlOiBpbnQgY2hlY2tVaWRQZXJtaXNzaW9uKGphdmEubGFuZy5TdHJpbmcsaW50KT4oQUxMLCA8YW5kcm9pZC5vcy5CaW5kZXI6IGludCBnZXRDYWxsaW5nVWlkKCk-KCkpYA==\" "
		  + "-d \"AOSP;..../acminer_db/_acminer_db_.xml\" -d \"HTC;..../acminer_db/_acminer_db_.xml\" -d \"SONY;..../acminer_db/_acminer_db_.xml\"\n"
		  + "\nOptions:\n"
		  + "  -h, --help\n"
		  + "      # Print this message.\n"
		  + "  -e <entry_points>\n"
		  + "      # Sets the entry points to be queried for the authorization check\n"
		  + "      # Accepts a ';' seperated list of entry points in the soot signature\n"
		  + "      # format. (i.e. ep1;ep2;ep3;...;epn)\n"
		  + "  -p <authorization_check>\n"
		  + "      # Sets the authorization check to search for in the database. Accepts\n"
		  + "      # a Base64 encoded string using the URL and Filename safe type Base64\n"
		  + "      # encoding scheme. This string must be in Base64 as the actual\n"
		  + "      # authorization check may contain any chararacter many of which are\n"
		  + "      # not command line friendly.\n"
		  + "  -d <database_file>\n"
		  + "      # Adds a database file to the list of possible database files to query.\n"
		  + "      # Input must be in the format 'KEY;DATABASE_FILE_PATH' where the key is\n"
		  + "      # the unique identifier for the database used to identify entry points\n"
		  + "      # belonging to the database. The key value is only needed in the case\n"
		  + "      # where mutiple database files are to be queried. If a single database\n"
		  + "      # file is to be queried then the provided string format may be\n"
		  + "      # 'DATABASE_FILE_PATH' under the asumption that none of the entry\n"
		  + "      # points have been modified to include a KEY value in the method name.\n"
		  + "      # Note 'DATABASE_FILE_PATH' can only be used when a single database\n"
		  + "      # file is given. If mutiple database files are given then you must use\n"
		  + "      # the 'KEY;DATABASE_FILE_PATH' format. Also note the KEY cannot be the\n"
		  + "      # empty string.\n"
		  + "  --stats\n"
		  + "      # Prints out some stats on each database file."
		  ;
	
}
