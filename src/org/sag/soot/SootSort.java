package org.sag.soot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sag.common.tools.SortingMethods;

import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.callgraph.Edge;

public class SootSort {
	
	public static final Comparator<Edge> edgeComp = new Comparator<Edge>() {
		@Override
		public int compare(Edge o1, Edge o2) {
			if(o1 == null && o2 == null)
				return 0;
			else if(o1 == null && o2 != null)
				return -1;
			else if(o1 != null && o2 == null)
				return 1;
			else {
				int ret = smComp.compare(o1.src(), o2.src());
				if(ret == 0)
					ret = smComp.compare(o1.tgt(), o2.tgt());
				return ret;
			}
		}
	};

	public static final Comparator<Unit> unitComp = new Comparator<Unit>(){
		@Override
		public int compare(Unit o1, Unit o2){
			if(o1 == null && o2 == null)
				return 0;
			else if(o1 == null && o2 != null)
				return -1;
			else if(o1 != null && o2 == null)
				return 1;
			else
				return SortingMethods.sComp.compare(o1.toString(),o2.toString());
		}
	};
	
	public static final Comparator<SootField> sfComp = new Comparator<SootField>(){
		@Override
		public int compare(SootField o1, SootField o2) {
			if(o1 == null && o2 == null)
				return 0;
			else if(o1 == null && o2 != null)
				return -1;
			else if(o1 != null && o2 == null)
				return 1;
			else {
				int ret = SortingMethods.sComp.compare(o1.getDeclaringClass().getName(),o2.getDeclaringClass().getName());
				if(ret == 0){
					ret = SortingMethods.sComp.compare(o1.getName(),o2.getName());
					if(ret == 0){
						ret = SortingMethods.sComp.compare(o1.getType().toString(),o2.getType().toString());
					}
				}
				return ret;
			}
		}
	};
	
	public static final Comparator<String> sfStringComp =  new Comparator<String>(){
		private Pattern pat = Pattern.compile("^<(.+): (.+) (.+)>$");
		@Override
		public int compare(String o1, String o2){
			if(o1 != null && o1.length() > 0 && o2 != null && o2.length() > 0){
				Matcher m1 = pat.matcher(o1);
				Matcher m2 = pat.matcher(o2);
				if(m1.matches() && m2.matches()){
					String o1Class = m1.group(1);
					String o2Class = m2.group(1);
					String o1RetType =  m1.group(2);
					String o2RetType = m2.group(2);
					String o1Name = m1.group(3);
					String o2Name = m2.group(3);
					
					int c1 = SortingMethods.sComp.compare(o1Class,o2Class);
					if(c1 == 0){
						int c2 = SortingMethods.sComp.compare(o1Name,o2Name);
						if(c2 == 0){
							return SortingMethods.sComp.compare(o1RetType,o2RetType);
						}else{
							return c2;
						}
					}else{
						return c1;
					}
				}else if(m1.matches()){
					return 1;
				}else if(m2.matches()){
					return -1;
				}else{
					return SortingMethods.sComp.compare(o1,o2);
				}
			}else if(o1 != null && o1.length() > 0){
				return 1;
			}else if(o2 != null && o2.length() > 0){
				return -1;
			}else{
				return 0;
			}
		}
	};
	
	public static final Comparator<SootMethod> smComp = new Comparator<SootMethod>(){
		@Override
		public int compare(SootMethod o1, SootMethod o2) {
			if(o1 == null && o2 == null)
				return 0;
			else if(o1 == null && o2 != null)
				return -1;
			else if(o1 != null && o2 == null)
				return 1;
			else {
				int ret = SortingMethods.sComp.compare(o1.getDeclaringClass().getName(),o2.getDeclaringClass().getName());
				if(ret == 0){
					ret = SortingMethods.sComp.compare(o1.getName(),o2.getName());
					if(ret == 0){
						ret = SortingMethods.sComp.compare(o1.getReturnType().toString(),o2.getReturnType().toString());
						if(ret == 0){
							ret = SortingMethods.sComp.compare(o1.getParameterTypes().toString(),o2.getParameterTypes().toString());
						}
					}
				}
				return ret;
			}
		}
	};

	public static final Comparator<String> smStringComp = new Comparator<String>(){
		private Pattern pat = Pattern.compile("^<(.+): (.+) (.+)\\((.*)\\)>$");
		@Override
		public int compare(String o1, String o2){
			if(o1 != null && o1.length() > 0 && o2 != null && o2.length() > 0){
				Matcher m1 = pat.matcher(o1);
				Matcher m2 = pat.matcher(o2);
				if(m1.matches() && m2.matches()){
					String o1Class = m1.group(1);
					String o1RetType = m1.group(2);
					String o1Name = m1.group(3);
					String o1Args = m1.group(4);
					String o2Class = m2.group(1);
					String o2RetType = m2.group(2);
					String o2Name = m2.group(3);
					String o2Args = m2.group(4);
					
					int c1 = SortingMethods.sComp.compare(o1Class, o2Class);
					if(c1 == 0){
						int c2 = SortingMethods.sComp.compare(o1Name,o2Name);
						if(c2 == 0){
							int c3 = SortingMethods.sComp.compare(o1RetType,o2RetType);
							if(c3 == 0){
								return SortingMethods.sComp.compare(o1Args,o2Args);
							}else{
								return c3;
							}
						}else{
							return c2;
						}
					}else{
						return c1;
					}
				}else if(m1.matches()){
					return 1;
				}else if(m2.matches()){
					return -1;
				}else{
					return SortingMethods.sComp.compare(o1,o2);
				}
			}else if(o1 != null && o1.length() > 0){
				return 1;
			}else if(o2 != null && o2.length() > 0){
				return -1;
			}else{
				return 0;
			}
		}
	};

	public static final Comparator<SootClass> scComp = new Comparator<SootClass>(){
		@Override
		public int compare(SootClass o1, SootClass o2) {
			if(o1 == null && o2 == null)
				return 0;
			else if(o1 == null && o2 != null)
				return -1;
			else if(o1 != null && o2 == null)
				return 1;
			else
				return SortingMethods.sComp.compare(o1.getName(), o2.getName());
		}
	};

	public static Comparator<LinkedList<SootMethod>> pathComp = new Comparator<LinkedList<SootMethod>>(){
		@Override
		public int compare(LinkedList<SootMethod> o1, LinkedList<SootMethod> o2) {
			StringBuilder path1 = new StringBuilder();
			StringBuilder path2 = new StringBuilder();
			for(SootMethod m : o1){
				path1.append(m.getSignature());
			}
			for(SootMethod m : o2){
				path2.append(m.getSignature());
			}
			return SortingMethods.sComp.compare(path1.toString(),path2.toString());
		}
	};

	public static void sortListSootMethod(List<SootMethod> l){
		Collections.sort(l,smComp);
	}

	public static void sortListSootClass(List<SootClass> l){
		Collections.sort(l,scComp);
	}

	public static void sortListPath(List<LinkedList<SootMethod>> l){
		Collections.sort(l,pathComp);
	}

	public static <A> Map<SootMethod,A> sortMapByMethodKeyAscending(Map<SootMethod,A> map){
		return SortingMethods.sortMap(map,new Comparator<Map.Entry<SootMethod,A>>(){
			@Override
			public int compare(Entry<SootMethod, A> o1,Entry<SootMethod, A> o2) {
				return smComp.compare(o1.getKey(), o2.getKey());
			}
		});
	}

	public static List<String> sortStringMethods(Collection<String> list){
		List<String> ret = new ArrayList<String>(list);
		Collections.sort(ret,smStringComp);
		return ret;
	}

	public static <A> Map<String,A> sortMapByStringMethodKeyAscending(Map<String,A> map){
		return SortingMethods.sortMap(map,new Comparator<Map.Entry<String,A>>(){
			@Override
			public int compare(Entry<String, A> o1,Entry<String, A> o2) {
				return smStringComp.compare(o1.getKey(), o2.getKey());
			}
		});
	}

}
