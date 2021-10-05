package org.sag.acminer.database.binder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.sag.acminer.database.FileHashDatabase;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

import soot.SootClass;
import soot.SootMethod;

public class BinderDatabase extends FileHashDatabase {
	
	@XStreamOmitField
	private final ReadWriteLock rwlock;
	
	@XStreamOmitField
	private volatile boolean loaded;
	
	protected BinderDatabase() {
		rwlock = new ReentrantReadWriteLock();
		loaded = false;
	}
	
	protected BinderDatabase(Map<SootClass,Set<SootMethod>> interfacesToInterfaceMethods, 
			Map<SootClass,Map<SootClass,Map<SootMethod,Set<Integer>>>> interfacesToProxiesToProxyMethodToIds,
			Map<SootClass,Map<SootClass,Map<SootClass,Map<SootMethod,Set<Integer>>>>> interfacesToStubsToServicesToEntryPointsToIds,
			Map<SootClass,Map<SootClass,Set<Integer>>> interfacesToStubsToAllTransactionIds,
			Map<SootClass,Map<SootClass,Map<SootMethod,Set<Integer>>>> stubsToServicesToEpsToTransactionIds,
			Map<SootClass,Set<Integer>> stubsToAllTransactionIds) {
		this();
		loaded = true;
		Objects.requireNonNull(interfacesToInterfaceMethods);
		Objects.requireNonNull(interfacesToProxiesToProxyMethodToIds);
		Objects.requireNonNull(interfacesToStubsToServicesToEntryPointsToIds);
		Objects.requireNonNull(interfacesToStubsToAllTransactionIds);
		Objects.requireNonNull(stubsToServicesToEpsToTransactionIds);
		Objects.requireNonNull(stubsToAllTransactionIds);
		
		Map<SootClass,Stub> stubClassToStub = new LinkedHashMap<>();
		Map<SootClass,Interface> interfaceClassToInterface = new LinkedHashMap<>();
		AtomicLong placeHolderCount = new AtomicLong();
		
		for(SootClass stubClass : stubsToServicesToEpsToTransactionIds.keySet()) {
			stubClassToStub.put(stubClass, new Stub(stubClass, placeHolderCount, 
					stubsToAllTransactionIds.get(stubClass), stubsToServicesToEpsToTransactionIds.get(stubClass)));
		}
		
		for(SootClass iface : interfacesToInterfaceMethods.keySet()) {
			interfaceClassToInterface.put(iface, new Interface(iface, placeHolderCount, interfacesToInterfaceMethods.get(iface), 
					interfacesToProxiesToProxyMethodToIds.get(iface), interfacesToStubsToServicesToEntryPointsToIds.get(iface), 
					stubsToAllTransactionIds, stubClassToStub));
		}
	}

}
