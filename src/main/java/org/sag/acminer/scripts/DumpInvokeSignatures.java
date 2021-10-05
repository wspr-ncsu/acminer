package org.sag.acminer.scripts;

import java.io.ObjectStreamException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.sag.acminer.IACMinerDataAccessor;
import org.sag.common.io.FileHelpers;
import org.sag.common.logging.ILogger;
import org.sag.common.tools.HierarchyHelpers;
import org.sag.common.tools.SortingMethods;
import org.sag.common.xstream.XStreamInOut;
import org.sag.common.xstream.XStreamInOut.XStreamInOutInterface;
import org.sag.soot.SootSort;
import org.sag.soot.xstream.SootClassContainer;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.util.Chain;

public class DumpInvokeSignatures {
	
	private String cn;
	private ILogger logger;
	private Path outDir;
	private IACMinerDataAccessor dataAccessor;

	public DumpInvokeSignatures(Path outDir, ILogger logger, IACMinerDataAccessor dataAccessor) {
		this.cn = this.getClass().getSimpleName();
		this.logger = logger;
		this.outDir = outDir;
		this.dataAccessor = dataAccessor;
	}
	
	public void run() {
		logger.info("{}: Running DumpInvokeSignatures.",cn);
		for(SootClass sc : Scene.v().getClasses().getElementsUnsorted()) {
			logger.info("{}: Processing '{}'",cn,sc);
			
			try {
				InvokeSignaturesDatabase i = new InvokeSignaturesDatabase(sc);
				
				for(SootMethod m : sc.getMethods()) {
					i.addMethod(m, HierarchyHelpers.getAllPossibleInvokeSignaturesForMethod(m,dataAccessor));
				}
			
				Path filePath;
				if(!sc.getPackageName().equals("")){
					Path dirPath;
					String[] directoryNames = sc.getPackageName().split("\\.");
					if(directoryNames.length == 1){
						dirPath = FileHelpers.getPath(outDir,directoryNames[0]);
					}else{
						dirPath = FileHelpers.getPath(outDir,directoryNames);
					}
					Files.createDirectories(dirPath);
					filePath = FileHelpers.getPath(dirPath.toString(), sc.getShortName() + ".xml");
				}else{
					filePath = FileHelpers.getPath(sc.getShortName() + ".xml");
				}
				i.writeXML(null, filePath);
			} catch(Throwable t) {
				logger.fatal("{}: Failed to process '{}'.",t,cn,sc);
			}
		}
	}
	
	@XStreamAlias("InvokeSignaturesDatabase")
	private static final class InvokeSignaturesDatabase implements XStreamInOutInterface {
		
		@XStreamAlias("Class")
		private SootClassContainer sc;
		
		@XStreamAlias("Methods")
		private ArrayList<MethodContainer> methods;
		
		@XStreamAlias("Fields")
		private ArrayList<FieldContainer> fields;
		
		private InvokeSignaturesDatabase() {}
		
		public InvokeSignaturesDatabase(SootClass sc) {
			this.sc = SootClassContainer.makeSootClassContainer(sc);
			Chain<SootField> flds = sc.getFields();
			if(!flds.isEmpty()) {
				fields = new ArrayList<>();
				for(SootField f : flds) {
					fields.add(new FieldContainer(f));
				}
			}
		}
		
		protected Object readResolve() throws ObjectStreamException {
			return this;
		}
		
		protected Object writeReplace() throws ObjectStreamException {
			if(methods != null)
				Collections.sort(methods);
			return this;
		}
		
		public void addMethod(SootMethod m, Set<String> s) {
			if(methods == null)
				methods = new ArrayList<>();
			methods.add(new MethodContainer(m,s));
		}

		@Override
		public void writeXML(String filePath, Path path) throws Exception {
			XStreamInOut.writeXML(this,filePath, path);
		}

		@Override
		public InvokeSignaturesDatabase readXML(String filePath, Path path) throws Exception {
			return XStreamInOut.readXML(this,filePath, path);
		}

		@SuppressWarnings("unused")
		public static InvokeSignaturesDatabase readXMLStatic(String filePath, Path path) throws Exception {
			return new InvokeSignaturesDatabase().readXML(filePath, path);
		}
		
		@XStreamOmitField
		private static AbstractXStreamSetup xstreamSetup = null;

		public static AbstractXStreamSetup getXStreamSetupStatic(){
			if(xstreamSetup == null)
				xstreamSetup = new XStreamSetup();
			return xstreamSetup;
		}
		
		@Override
		public AbstractXStreamSetup getXStreamSetup() {
			return getXStreamSetupStatic();
		}
		
		public static class XStreamSetup extends AbstractXStreamSetup {
			
			@Override
			public void getOutputGraph(LinkedHashSet<AbstractXStreamSetup> in) {
				if(!in.contains(this)) {
					in.add(this);
					SootClassContainer.getXStreamSetupStatic().getOutputGraph(in);
				}
			}

			@Override
			public Set<Class<?>> getAnnotatedClasses() {
				Set<Class<?>> ret = new HashSet<>();
				ret.add(MethodContainer.class);
				ret.add(InvokeSignaturesDatabase.class);
				ret.add(InvokeSig.class);
				ret.add(FieldContainer.class);
				return ret;
			}
			
			@Override
			public void setXStreamOptions(XStream xstream) {
				defaultOptionsNoRef(xstream);
			}
			
		}
	}
	
	@XStreamAlias("Field")
	private static final class FieldContainer implements Comparable<FieldContainer> {
		@XStreamAlias("Name")
		@XStreamAsAttribute
		private String field;
		@XStreamAlias("isStatic")
		@XStreamAsAttribute
		private boolean isStatic;
		@XStreamAlias("isFinal")
		@XStreamAsAttribute
		private boolean isFinal;
		@XStreamAlias("Modifier")
		@XStreamAsAttribute
		private int modifier;
		public FieldContainer(SootField field) {
			this.field = field.getSignature();
			this.isStatic = field.isStatic();
			this.isFinal = field.isFinal();
			if(field.isPrivate())
				modifier = 1;
			else if(field.isProtected())
				modifier = 2;
			else if(field.isPublic())
				modifier = 3;
			else
				modifier = 0;
		}
		@Override
		public int compareTo(FieldContainer o) {
			return SootSort.sfStringComp.compare(field, o.field);
		}
	}
	
	@XStreamAlias("Method")
	private static final class MethodContainer implements Comparable<MethodContainer> {
		
		@XStreamAlias("Name")
		@XStreamAsAttribute
		private String method;
		
		@XStreamImplicit
		private LinkedHashSet<InvokeSig> invokeSignatures;
		
		public MethodContainer(SootMethod m, Set<String> s) {
			method = m.getSignature();
			invokeSignatures = new LinkedHashSet<>();
			for(String sig: s) {
				SootMethod cur = Scene.v().grabMethod(sig);
				invokeSignatures.add(new InvokeSig(sig, cur != null && !cur.isAbstract() && !cur.isPhantom()));
			}
			invokeSignatures = SortingMethods.sortSet(invokeSignatures);
		}

		@Override
		public int compareTo(MethodContainer o) {
			return SootSort.smStringComp.compare(method, o.method);
		}
		
	}
	
	@XStreamAlias("InvokeSig")
	private static final class InvokeSig implements Comparable<InvokeSig> {
		
		@XStreamAlias("Name")
		@XStreamAsAttribute
		private String name;
		@XStreamAlias("isDefined")
		@XStreamAsAttribute
		private boolean isDefined;
		public InvokeSig(String name, boolean isDefined) {
			this.isDefined = isDefined;
			this.name = name;
		}
		@Override
		public int compareTo(InvokeSig o) {
			return SootSort.smStringComp.compare(this.name, o.name);
		}
		
	}
	
}
