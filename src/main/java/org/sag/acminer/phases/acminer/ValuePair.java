package org.sag.acminer.phases.acminer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.sag.acminer.phases.acminer.dw.DataWrapper;
import org.sag.acminer.phases.acminer.dw.NullConstant;
import org.sag.acminer.phases.acminer.dw.PrimitiveConstant;
import org.sag.common.tuple.Pair;

import soot.SootMethod;
import soot.Unit;

//The main reason we use this instead of a set is that we can have two entries that are the same which is impossible in a set
//Also sets a hard limit on the about of values allowed in the pair
public final class ValuePair implements Comparable<ValuePair>{
	
	private volatile DataWrapper op1;
	private volatile DataWrapper op2;
	private volatile SootMethod onlySourceMethod;
	private volatile Unit onlySourceUnit;
	private volatile String onlySourceStmt;
	private volatile Map<SootMethod,Map<String,Unit>> sources;
	
	private ValuePair() {
		op1 = null;
		op2 = null;
		sources = null;
		onlySourceUnit = null;
		onlySourceMethod = null;
		onlySourceStmt = null;
	}
	
	private void add(DataWrapper val) {
		if(this.op1 == null)
			this.op1 = val;
		else if(this.op2 == null)
			this.op2 = val;
		else
			throw new RuntimeException("Error: Attempted to add more than two values to a pair.");
	}
	
	//True if the sources being added represent new sources and false otherwise
	private boolean addSource(SootMethod sourceMethod, Unit sourceUnit, String sourceStmt) {
		Objects.requireNonNull(sourceMethod);
		Objects.requireNonNull(sourceUnit);
		if(sources == null && onlySourceMethod == null) {
			onlySourceMethod = sourceMethod;
			onlySourceUnit = sourceUnit;
			onlySourceStmt = sourceStmt;
			return true;
		} else if (sources == null && onlySourceMethod != null) {
			if(!onlySourceMethod.equals(sourceMethod) || !onlySourceUnit.equals(sourceUnit)) {
				sources = new HashMap<>();
				Map<String,Unit> temp = new HashMap<>();
				temp.put(onlySourceStmt, onlySourceUnit);
				sources.put(onlySourceMethod, temp);
				Map<String,Unit> cur = sources.get(sourceMethod);
				if(cur == null) {
					cur = new HashMap<>();
					sources.put(sourceMethod, cur);
				}
				cur.put(sourceStmt, sourceUnit);
				onlySourceMethod = null;
				onlySourceUnit = null;
				onlySourceStmt = null;
				return true;
			}
			return false;
		} else { //sources is not null
			Map<String,Unit> cur = sources.get(sourceMethod);
			if(cur == null) {
				cur = new HashMap<>();
				sources.put(sourceMethod, cur);
			}
			Unit unit = cur.get(sourceStmt);
			if(unit == null) {
				cur.put(sourceStmt, sourceUnit);
				return true;
			}
			return false;
		}
	}
	
	private boolean addSources(Map<SootMethod,Map<String,Unit>> s) {
		boolean modifiedSources = false;
		for(SootMethod m : s.keySet()) {
			Map<String,Unit> cur = s.get(m);
			for(String stmt : cur.keySet()) {
				Unit u = cur.get(stmt);
				if(addSource(m,u,stmt))
					modifiedSources = true;
			}
		}
		return modifiedSources;
	}
	
	public Map<SootMethod,Map<String,Unit>> getSources() {
		if(sources == null) {
			return Collections.singletonMap(onlySourceMethod, Collections.singletonMap(onlySourceStmt,onlySourceUnit));
		}
		return sources;
	}
	
	public int size() {
		if(op1 == null && op2 == null)
			return 0;
		else if(op1 == null || op2 == null)
			return 1;
		else
			return 2;
	}
	
	public boolean isPrimitiveVSPrimitveCheck() {
		if(op1 != null) {
			boolean temp = op1.isAllValueConstant() || op1.isNoValueConstant() || op1 instanceof PrimitiveConstant;
			if(op2 != null) {
				return temp && (op2.isAllValueConstant() || op2.isNoValueConstant() || op2 instanceof PrimitiveConstant);
			}
			return temp;
		}
		return false;
	}
	
	public boolean isPairWithSameValues() {
		if(op1 != null && op2 != null) {
			return op1.equals(op2);
		}
		return false;
	}
	
	public boolean isNullCheck() {
		return (op1 != null && op1 instanceof NullConstant) || (op2 != null && op2 instanceof NullConstant);
	}
	
	public DataWrapper getOp1() {
		return op1;
	}
	
	public DataWrapper getOp2() {
		return op2;
	}
	
	public boolean equals(Object o) {
		if(this == o) 
			return true;
		if(o == null || !(o instanceof ValuePair))
			return false;
		ValuePair p = (ValuePair)o;
		return (Objects.equals(op1,p.op1) && Objects.equals(op2,p.op2)) 
				|| (Objects.equals(op1, p.op2) && Objects.equals(op2, p.op1));
	}
	
	public int hashCode() {
		int i = 17;
		i = i * 31 + (Objects.hashCode(op1) + Objects.hashCode(op2));
		return i;
	}
	
	public String toString() {
		if(op1 == null && op2 == null) {
			return "";
		} else if(op1 == null) {
			return quoteString(op2.toString());
		} else if(op2 == null) {
			return quoteString(op1.toString());
		} else {
			StringBuilder sb = new StringBuilder();
			int r = op1.compareTo(op2);
			sb.append("{");
			if(r <= 0)
				sb.append(quoteString(op1.toString())).append(", ").append(quoteString(op2.toString()));
			else
				sb.append(quoteString(op2.toString())).append(", ").append(quoteString(op1.toString()));
			sb.append("}");
			return sb.toString();
		}
	}
	
	public static Pair<String,String> parsePair(String pair) {
		boolean firstSymbol = true;
		boolean inSymbol = false;
		StringBuilder sb = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();
		for(int i = 0; i < pair.length(); i++) {
			char c = pair.charAt(i);
			if(c == '`') {
				inSymbol = !inSymbol;
			} else if(!inSymbol && c == ',') {
				firstSymbol = false;
			} else if(!inSymbol && c == '{') {
				firstSymbol = true;
			} else if(!inSymbol && c == '}') {
				if(i != pair.length()-1)
					throw new RuntimeException("Error: Failed to parse pair " + pair);
				break;
			} else {
				if(inSymbol) {
					if(firstSymbol)
						sb.append(c);
					else
						sb2.append(c);
				}
			}
		}
		String op1 = sb.toString();
		String op2 = sb2.toString();
		if(op1.indexOf('`') >= 0 || op2.indexOf('`') >= 0 || op1.isEmpty())
			throw new RuntimeException("Error: Failed to parse pair " + pair);
		if(op2.isEmpty())
			op2 = null;
		return new Pair<String,String>(op1,op2);
	}
	
	@Override
	public int compareTo(ValuePair o) {
		int r = Integer.compare(size(), o.size());
		if(r == 0) {
			if(size() == 0) {
				r = -1;
			} else if(size() == 1) {
				r = op1.compareTo(o.op1);
			} else {
				int cmp1 = op1.compareTo(op2);
				int cmp2 = o.op1.compareTo(o.op2);
				DataWrapper p11;
				DataWrapper p12;
				DataWrapper p21;
				DataWrapper p22;
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
	
	public static String quoteString(String s) {
		if(s.contains("`"))
			throw new RuntimeException("Error: The string '" + s + "' contains the quoting character '`'.");
		return "`" + s + "`";
	}
	
	public static ValuePair make(DataWrapper op1) {
		return make(op1,(DataWrapper)null,(ValuePair)null);
	}
	
	public static ValuePair make(ValuePair cur) {
		return make((DataWrapper)null,(DataWrapper)null,cur);
	}
	
	public static ValuePair make(DataWrapper op1, ValuePair cur) {
		return make(op1,(DataWrapper)null,cur);
	}
	
	public static ValuePair make(DataWrapper op1, DataWrapper op2, ValuePair cur) {
		ValuePair vp = new ValuePair();
		if(op1 != null) {
			vp.add(op1);
			if(op2 != null)
				vp.add(op2);
		}
		if(cur != null) {
			if(op1 == null) {
				if(cur.size() == 1) {
					vp.add(cur.getOp1());
				} else if(cur.size() == 2) {
					vp.add(cur.getOp1());
					vp.add(cur.getOp2());
				}
			}
			vp.addSources(cur.getSources());
		}
		return vp;
	}
	
	public static ValuePair make(SootMethod sourceMethod, Unit sourceUnit, String sourceStmt) {
		return make((DataWrapper)null,(DataWrapper)null,sourceMethod,sourceUnit,sourceStmt);
	}
	
	public static ValuePair make(DataWrapper op1, SootMethod sourceMethod, Unit sourceUnit, String sourceStmt) {
		return make(op1,(DataWrapper)null,sourceMethod,sourceUnit,sourceStmt);
	}
	
	public static ValuePair make(DataWrapper op1, DataWrapper op2, SootMethod sourceMethod, Unit sourceUnit, String sourceStmt) {
		ValuePair vp = new ValuePair();
		if(op1 != null) {
			vp.add(op1);
			if(op2 != null)
				vp.add(op2);
		}
		if(sourceMethod != null && sourceUnit != null) {
			vp.addSource(sourceMethod, sourceUnit, sourceStmt);
		}
		return vp;
	}
	
	public static ValuePair make(DataWrapper op1, DataWrapper op2, Map<SootMethod,Map<String,Unit>> sources) {
		ValuePair vp = new ValuePair();
		if(op1 != null) {
			vp.add(op1);
			if(op2 != null)
				vp.add(op2);
		}
		if(sources != null) {
			vp.addSources(sources);
		}
		return vp;
	}
	
	/** Clones the given ValuePair and attempts to add the new sources to the cloned
	 * ValuePair. If the sources to add did not actually contain any new sources then
	 * the original ValuePair is returned, otherwise the cloned value pair is returned.
	 */
	public static ValuePair make(ValuePair cur, Map<SootMethod,Map<String,Unit>> sourcesToAdd) {
		ValuePair vp = make(cur);
		if(vp.addSources(sourcesToAdd))
			return vp;
		return cur;
	}
	
}