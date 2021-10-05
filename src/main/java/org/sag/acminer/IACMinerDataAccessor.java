package org.sag.acminer;

import java.util.Map;
import java.util.Set;

import org.sag.acminer.database.accesscontrol.IAccessControlDatabase;
import org.sag.acminer.database.accesscontrol.IContextQueryDatabase;
import org.sag.acminer.database.acminer.IACMinerDatabase;
import org.sag.acminer.database.binderservices.IBinderServicesDatabase;
import org.sag.acminer.database.defusegraph.IDefUseGraphDatabase;
import org.sag.acminer.database.entrypointedges.IEntryPointEdgesDatabase;
import org.sag.acminer.database.excludedelements.IExcludedElementsDatabase;
import org.sag.acminer.phases.entrypoints.EntryPoint;
import org.sag.acminer.phases.entrypoints.EntryPointsDatabase.IntegerWrapper;
import org.sag.main.IDataAccessor;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;

public interface IACMinerDataAccessor extends IDataAccessor {

	Set<SootMethod> getEntryPointsAsSootMethods();

	Set<EntryPoint> getEntryPoints();

	Set<SootMethod> getEntryPointsForStub(SootClass stub);

	Map<SootClass, Set<SootMethod>> getBinderInterfacesAndMethods();

	Map<SootClass, Map<SootClass, Map<SootMethod, Set<Integer>>>> getBinderProxiesAndMethodsByInterface();

	Map<SootClass, Map<SootClass, Map<SootMethod, Set<Integer>>>> getBinderStubsAndMethodsByInterface();

	Map<SootClass, Map<SootMethod, Set<Integer>>> getBinderProxiesAndMethods();

	Map<SootClass, Map<SootMethod, Set<Integer>>> getBinderStubsAndMethods();

	Map<SootClass, Map<String, Set<SootMethod>>> getBinderStubMethodsToEntryPointsByInterface();

	Map<SootMethod, Set<SootMethod>> getBinderInterfaceMethodsToProxyMethods();

	Map<SootMethod, Set<SootMethod>> getBinderInterfaceMethodsToEntryPoints();

	Map<SootMethod, Set<SootMethod>> getBinderProxyMethodsToEntryPoints();

	Map<String, Set<SootMethod>> getBinderStubMethodsToEntryPoints();

	Map<String, Set<SootMethod>> getBinderStubMethodsToEntryPointsForInterface(SootClass binderInterface);

	Set<SootMethod> getBinderProxyMethodsForInterfaceMethod(SootMethod interfaceMethod);

	Set<SootMethod> getEntryPointsForBinderProxyMethod(SootMethod proxyMethod);

	Set<SootMethod> getEntryPointsFromBinderMethod(InvokeExpr ie);

	Set<String> getAllBinderGroupClasses();

	Map<SootClass, Map<SootMethod, Set<IntegerWrapper>>> getEntryPointsByStubWithTransactionId();

	Map<SootClass, Set<IntegerWrapper>> getStubsToAllTransactionIds();

	boolean isBinderGroupsDatabaseSet();

	boolean isEntryPointsDatabaseSet();

	Set<String> getAllEntryPointClasses();

	Set<String> getAllEntryPointMethods();

	boolean hasMarkedUnits(EntryPoint ep);

	boolean isMarkedUnit(EntryPoint ep, Unit u);

	void resetEntryPointsDatabaseSootData();

	Map<SootClass, Set<SootMethod>> getEntryPointsByDeclaringClass();

	IAccessControlDatabase getControlPredicatesDB();

	void setControlPredicatesDB(IAccessControlDatabase db);

	IContextQueryDatabase getContextQueriesDB();

	void setContextQueriesDB(IContextQueryDatabase db);

	IAccessControlDatabase getThrowSecurityExceptionStmtsDB();

	void setThrowSecurityExceptionStmtsDB(IAccessControlDatabase db);

	IDefUseGraphDatabase getDefUseGraphDB();

	void setDefUseGraphDB(IDefUseGraphDatabase db);

	IDefUseGraphDatabase getDefUseGraphModDB();

	void setDefUseGraphModDB(IDefUseGraphDatabase db);

	IExcludedElementsDatabase getExcludedElementsDB();

	void setExcludedElementsDB(IExcludedElementsDatabase db);

	IACMinerDatabase getACMinerDB();

	void setACMinerDB(IACMinerDatabase db);

	IEntryPointEdgesDatabase getEntryPointEdgesDB();

	void setEntryPointEdgesDB(IEntryPointEdgesDatabase db);

	IBinderServicesDatabase getBinderServiceDB();

	void setBinderServiceDB(IBinderServicesDatabase db);

}