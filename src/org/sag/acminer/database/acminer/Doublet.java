package org.sag.acminer.database.acminer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.sag.acminer.phases.acminer.ValuePair;
import org.sag.acminer.phases.acminer.dw.DataWrapper;
import org.sag.common.tools.SortingMethods;
import org.sag.common.tuple.Pair;
import org.sag.soot.xstream.SootMethodContainer;
import org.sag.soot.xstream.SootUnitContainer;
import org.sag.xstream.XStreamInOut;
import org.sag.xstream.XStreamInOut.XStreamInOutInterface;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import org.sag.xstream.xstreamconverters.NamedCollectionConverterWithSize;
import soot.SootMethod;
import soot.Unit;

@XStreamAlias("Doublet")
public class Doublet implements XStreamInOutInterface, Comparable<Doublet> {
	
	@XStreamAlias("Expression")
	@XStreamAsAttribute
	private volatile String expression;
	
	@XStreamAlias("OP1")
	private volatile String op1;
	
	@XStreamAlias("OP2")
	private volatile String op2;
	
	@XStreamAlias("Sources")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"SourceMethod"},types={SourceMethod.class})
	private volatile ArrayList<SourceMethod> sources;
	
	private Doublet() {
		this.expression = null;
		this.op1 = null;
		this.op2 = null;
		this.sources = null;
	}
	
	public Doublet(String expression) {
		this();
		Pair<String,String> data = ValuePair.parsePair(expression);
		this.op1 = data.getFirst();
		this.op2 = data.getSecond();
		this.expression = expression;
		this.sources = new ArrayList<>();
	}
	
	public Doublet(String expression, Map<SootMethodContainer,Map<String,SootUnitContainer>> sources) {
		this(expression);
		if(sources == null || sources.isEmpty())
			throw new RuntimeException("Error: Doublet " + expression + " has no source");
		for(SootMethodContainer sm : SortingMethods.sortSet(sources.keySet())) {
			this.sources.add(new SourceMethod(sm, sources.get(sm)));
		}
	}
	
	public Doublet(ValuePair vp) {
		this();

		DataWrapper op1 = vp.getOp1();
		DataWrapper op2 = vp.getOp2();
		if(op1 == null && op2 == null) { //both null error
			throw new RuntimeException("Error: Received a ValuePair with no ops");
		} else if(op1 == null && op2 != null) { //first null but second not null error
			throw new RuntimeException("Error: Received improperly formated ValuePair " + vp.toString());
		} else if(op2 == null) { //first is not null and second is null
			this.op1 = op1.toString();
		} else { //both not null
			int r = op1.compareTo(op2);
			if(r <= 0) {
				this.op1 = op1.toString();
				this.op2 = op2.toString();
			} else {
				this.op1 = op2.toString();
				this.op2 = op1.toString();
			}
		}
		this.expression = vp.toString();
		
		Map<SootMethod,Map<String,Unit>> sources = vp.getSources();
		if(sources.isEmpty()) {
			throw new RuntimeException("Error: ValuePair " + vp.toString() + " has no source");
		}
		Map<String,Unit> temp = sources.values().iterator().next();
		if(temp == null || temp.isEmpty()) {
			throw new RuntimeException("Error: ValuePair " + vp.toString() + " has no source");
		}
		this.sources = new ArrayList<>();
		for(SootMethod sm : sources.keySet()) {
			this.sources.add(new SourceMethod(sm, sources.get(sm)));
		}
	}
	
	public String getOp1() {
		return op1;
	}
	
	public String getOp2() {
		return op2;
	}
	
	public Map<SootMethodContainer,Map<String,SootUnitContainer>> getSources() {
		Map<SootMethodContainer,Map<String,SootUnitContainer>> ret = new LinkedHashMap<>();
		for(SourceMethod m : sources) {
			ret.put(m.getMethod(), m.getUnits());
		}
		return ret;
	}
	
	public Map<String,Map<String,String>> getSourcesAsStrings() {
		Map<String,Map<String,String>> ret = new LinkedHashMap<>();
		for(SourceMethod m : sources) {
			Map<String,String> temp = new LinkedHashMap<>();
			Map<String,SootUnitContainer> units = m.getUnits();
			for(String stmt : units.keySet()) {
				temp.put(stmt,units.get(stmt).getSignature());
			}
			ret.put(m.getMethod().getSignature(), temp);
		}
		return ret;
	}
	
	public Map<SootMethod,Map<String,Unit>> getSootSources() {
		Map<SootMethod,Map<String,Unit>> ret = new LinkedHashMap<>();
		for(SourceMethod m : sources) {
			ret.put(m.getSootMethod(), m.getSootUnits());
		}
		return ret;
	}
	
	public Set<String> getSourceMethods() {
		Set<String> ret = new LinkedHashSet<>();
		for(SourceMethod m : sources) {
			ret.add(m.getMethod().getSignature());
		}
		return ret;
	}
	
	public Set<SootMethodContainer> getSourceMethodContainers() {
		Set<SootMethodContainer> ret = new LinkedHashSet<>();
		for(SourceMethod m : sources) {
			ret.add(m.getMethod());
		}
		return ret;
	}
	
	public Set<SootMethod> getSootSourceMethods() {
		Set<SootMethod> ret = new LinkedHashSet<>();
		for(SourceMethod m : sources) {
			ret.add(m.getSootMethod());
		}
		return ret;
	}
	
	public int size() {
		if(op1 == null && op2 == null)
			return 0;
		else if(op1 == null || op2 == null)
			return 1;
		else
			return 2;
	}
	
	public boolean equals(Object o) {
		if(this == o) 
			return true;
		if(o == null || !(o instanceof Doublet))
			return false;
		Doublet p = (Doublet)o;
		return (Objects.equals(op1,p.op1) && Objects.equals(op2,p.op2)) 
				|| (Objects.equals(op1, p.op2) && Objects.equals(op2, p.op1));
	}
	
	public int hashCode() {
		int i = 17;
		i = i * 31 + (Objects.hashCode(op1) + Objects.hashCode(op2));
		return i;
	}
	
	public String toString() {
		return expression;
	}
	
	@Override
	public int compareTo(Doublet o) {
		int r = Integer.compare(size(), o.size());
		if(r == 0) {
			if(size() == 0) {
				r = -1;
			} else if(size() == 1) {
				r = op1.compareTo(o.op1);
			} else {
				int cmp1 = op1.compareTo(op2);
				int cmp2 = o.op1.compareTo(o.op2);
				String p11;
				String p12;
				String p21;
				String p22;
				if(cmp1 <= 0) {
					p11 = op1;
					p12 = op2;
				} else {
					p11 = op2;
					p12 = op1;
				}
				if(cmp2 <= 0) {
					p21 = o.op1;
					p22 = o.op2;
				} else {
					p21 = o.op2;
					p22 = o.op1;
				}
				r = p11.compareTo(p21);
				if(r == 0)
					r = p12.compareTo(p22);
			}
		}
		return r;
	}

	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public Doublet readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}

	public static Doublet readXMLStatic(String filePath, Path path) throws Exception {
		return new Doublet().readXML(filePath, path);
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
				SourceMethod.getXStreamSetupStatic().getOutputGraph(in);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(Doublet.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}
	
}
