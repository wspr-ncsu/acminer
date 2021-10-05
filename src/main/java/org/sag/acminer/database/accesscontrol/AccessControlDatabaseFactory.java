package org.sag.acminer.database.accesscontrol;

import java.nio.file.Path;
import java.util.List;

import org.sag.common.tuple.Pair;

import com.google.common.collect.ImmutableList;

public class AccessControlDatabaseFactory {

	private static final List<Pair<String,String>> names = ImmutableList.of(
			new Pair<String,String>("Control Predicates","ControlPredicatesDatabase"),
			new Pair<String,String>("Context Queries","ContextQueriesDatabase"),
			new Pair<String,String>("Throw Security Exception Stmts","ThrowSecurityExceptionStmtsDatabase")
			);
	
	private static IAccessControlDatabase getNewDatabase(Pair<String,String> p, boolean empty, boolean newDB) {
		if(empty)
			return new EmptyAccessControlDatabase(p.getFirst(),p.getSecond());
		else
			return new AccessControlDatabase(p.getFirst(),p.getSecond(),newDB);
	}
	
	public static IAccessControlDatabase getNewControlPredicatesDatabase(boolean empty) {
		return getNewControlPredicatesDatabase(empty,true);
	}
	
	public static IContextQueryDatabase getNewContextQueriesDatabase(boolean empty) {
		return getNewContextQueriesDatabase(empty, true);
	}
	
	public static IAccessControlDatabase getNewThrowSecurityExceptionStmtsDatabase(boolean empty) {
		return getNewThrowSecurityExceptionStmtsDatabase(empty,true);
	}
	
	private static IAccessControlDatabase getNewControlPredicatesDatabase(boolean empty, boolean newDB) {
		return getNewDatabase(names.get(0), empty, newDB);
	}
	
	private static IContextQueryDatabase getNewContextQueriesDatabase(boolean empty, boolean newDB) {
		if(empty)
			return new EmptyAccessControlDatabase(names.get(1).getFirst(),names.get(1).getSecond());
		return new ContextQueriesDatabase(names.get(1).getFirst(),names.get(1).getSecond(),newDB);
	}
	
	private static IAccessControlDatabase getNewThrowSecurityExceptionStmtsDatabase(boolean empty, boolean newDB) {
		return getNewDatabase(names.get(2), empty, newDB);
	}
	
	private static IAccessControlDatabase readXml(IAccessControlDatabase database, String filePath, Path path) throws Exception {
		return database.readXML(filePath, path);
	}
	
	private static IContextQueryDatabase readXml(IContextQueryDatabase database, String filePath, Path path) throws Exception {
		return database.readXML(filePath, path);
	}
	
	public static IAccessControlDatabase readXmlControlPredicatesDatabase(String filePath, Path path) throws Exception {
		return readXml(getNewControlPredicatesDatabase(false,false),filePath,path);
	}
	
	public static IContextQueryDatabase readXmlContextQueriesDatabase(String filePath, Path path) throws Exception {
		return readXml(getNewContextQueriesDatabase(false,false),filePath,path);
	}
	
	public static IAccessControlDatabase readXmlThrowSecurityExceptionStmtsDatabase(String filePath, Path path) throws Exception {
		return readXml(getNewThrowSecurityExceptionStmtsDatabase(false,false),filePath,path);
	}
	
	private static boolean isType(IAccessControlDatabase db, String type) {
		return db.getType().equals(type);
	}
	
	public static boolean isControlPredicatesDatabase(IAccessControlDatabase db) {
		return isType(db,names.get(0).getSecond());
	}
	
	public static boolean isContextQueriesDatabase(IAccessControlDatabase db) {
		return isType(db,names.get(1).getSecond());
	}
	
	public static boolean isThrowSecurityExceptionStmtsDatabase(IAccessControlDatabase db) {
		return isType(db,names.get(2).getSecond());
	}
	
}
