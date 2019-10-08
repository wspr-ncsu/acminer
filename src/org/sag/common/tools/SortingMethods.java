package org.sag.common.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SortingMethods {
	
	public static final Comparator<String> simpleExprStringComp = new Comparator<String>(){
		private Pattern notCheckPat = Pattern.compile("^\\(not\\s*\\(");
		private Pattern opPat = Pattern.compile("^\\(([^\\s]+)\\s+");
		@Override
		public int compare(String o1, String o2) {
			String org1 = o1;
			String org2 = o2;
			boolean isExprO1 = true;
			boolean isExprO2 = true;
			StringBuilder o1New = new StringBuilder();
			StringBuilder o2New = new StringBuilder();
			if(o1 != null){
				Matcher notCheckMO1 = notCheckPat.matcher(o1);
				if(notCheckMO1.lookingAt()){
					o1 = notCheckMO1.replaceFirst("(");
					o1New.append("~");
				}
			}
			if(o2 != null){
				Matcher notCheckMO2 = notCheckPat.matcher(o2);
				if(notCheckMO2.lookingAt()){
					o2 = notCheckMO2.replaceFirst("(");
					o2New.append("~");
				}
			}
			if(o1 != null && o1.startsWith("(")){
				Matcher opPatMO1 = opPat.matcher(o1);
				if(opPatMO1.lookingAt()){
					o1New.insert(0, opPatMO1.group(1));
					o1 = opPatMO1.replaceFirst("");
					o1 = o1.replaceFirst("[\\)]+$", "");
					String[] symbols = o1.split("\\|\\s+\\|");
					if(symbols.length != 2){
						symbols = o1.split("(\\|\\s+)|(\\s+\\|)");
						if(symbols.length != 2){
							symbols = o1.split("\\s+");
							if(symbols.length != 2){
								isExprO1 = false;
							}
						}
					}
					if(symbols.length == 2){
						List<String> symbolList = Arrays.asList(symbols);
						Collections.sort(symbolList,sComp);
						o1New.insert(0," ").insert(0, symbolList.toString());
					}
				}else{
					isExprO1 = false;
				}
			}else{
				isExprO1 = false;
			}
			if(o2 != null && o2.startsWith("(")){
				Matcher opPatMO2 = opPat.matcher(o2);
				if(opPatMO2.lookingAt()){
					o2New.insert(0, opPatMO2.group(1));
					o2 = opPatMO2.replaceFirst("");
					o2 = o2.replaceFirst("[\\)]+$", "");
					String[] symbols = o2.split("\\|\\s+\\|");
					if(symbols.length != 2){
						symbols = o2.split("(\\|\\s+)|(\\s+\\|)");
						if(symbols.length != 2){
							symbols = o2.split("\\s+");
							if(symbols.length != 2){
								isExprO2 = false;
							}
						}
					}
					if(symbols.length == 2){
						List<String> symbolList = Arrays.asList(symbols);
						Collections.sort(symbolList,sComp);
						o2New.insert(0," ").insert(0, symbolList.toString());
					}
				}else{
					isExprO2 = false;
				}
			}else{
				isExprO2 = false;
			}
			if(isExprO1 && isExprO2){
				return sComp.compare(o1New.toString(), o2New.toString());
			}else if(isExprO1){
				return 1;
			}else if(isExprO2){
				return -1;
			}else{
				return sComp.compare(org1, org2);
			}
		}
		
	};
	
	public static final Comparator<String> sComp = new Comparator<String>(){
		@Override
		public int compare(String o1, String o2){
			if(o1 == null && o2 != null){
				return -1;
			}else if(o1 != null && o2 == null){
				return 1;
			}else if(o1 == null && o2 == null){
				return 0;
			}else{
				int ret = o1.compareToIgnoreCase(o2);
				if(ret == 0)
					ret = o1.compareTo(o2);
				return ret;
			}
		}
	};
	
	public static Comparator<Integer> iComp = new Comparator<Integer>(){
		public int compare(Integer o1, Integer o2){
			if(o1 == null && o2 == null) return 0;
			if(o1 == null) return -1;
			if(o2 == null) return 1;
			return o1.compareTo(o2);
		}
	};
	
	public static Comparator<Integer> iDecendingComp = new Comparator<Integer>(){
		public int compare(Integer o1, Integer o2){
			return o2.compareTo(o1);
		}
	};
	
	public static void sortListInteger(List<Integer> l){
		Collections.sort(l,iComp);
	}

	public static void sort(List<String> l){
		Collections.sort(l,sComp);
	}
	
	public static <A extends Comparable<? super A>> LinkedHashSet<A> sortSet(Set<A> set){
		return sortSet(set,new Comparator<A>(){
			@Override
			public int compare(A o1, A o2) {
				return o1.compareTo(o2);
			}
		});
	}
	
	public static <A extends Comparable<? super A>> LinkedHashSet<A> sortSetDec(Set<A> set){
		return sortSet(set,new Comparator<A>(){
			@Override
			public int compare(A o1, A o2) {
				return o2.compareTo(o1);
			}
		});
	}
	
	public static <A extends Comparable<? super A>> LinkedHashSet<A> sortCollectionGetSet(Collection<A> col){
		return sortCollectionGetSet(col,new Comparator<A>(){
			@Override
			public int compare(A o1, A o2) {
				return o1.compareTo(o2);
			}
		});
	}
	
	public static <A extends Comparable<? super A>> LinkedHashSet<A> sortCollectionGetSetDec(Collection<A> col){
		return sortCollectionGetSet(col,new Comparator<A>(){
			@Override
			public int compare(A o1, A o2) {
				return o2.compareTo(o1);
			}
		});
	}
	
	public static <A> LinkedHashSet<A> sortCollectionGetSet(Collection<A> col, Comparator<? super A> comparator){
		ArrayList<A> l = new ArrayList<A>(col);
		Collections.sort(l,comparator);
		return new LinkedHashSet<A>(l);
	}
	
	public static <A> LinkedHashSet<A> sortSet(Set<A> set, Comparator<? super A> comparator){
		ArrayList<A> l = new ArrayList<A>(set);
		Collections.sort(l,comparator);
		return new LinkedHashSet<A>(l);
	}
	
	public static <A,B> Map<A,B> sortMap(Map<A,B> map, Comparator<Map.Entry<A,B>> comparator){
		List<Map.Entry<A,B>> list = new LinkedList<>(map.entrySet());
		Collections.sort(list,comparator);
		Map<A,B> ret = new LinkedHashMap<A,B>();
		for(Map.Entry<A,B> e : list){
			ret.put(e.getKey(), e.getValue());
		}
		return ret;
	}
	
	public static <A,T> Map<A,T> sortMapKey(Map<A,T> map, final Comparator<? super A> comparator){
		return sortMap(map,new Comparator<Map.Entry<A,T>>(){
			@Override
			public int compare(Entry<A,T> o1, Entry<A,T> o2){
				return comparator.compare(o1.getKey(), o2.getKey());
			}
		});
	}
	
	public static <A extends Comparable<? super A>,T> Map<A,T> sortMapKeyAscending(Map<A,T> map){
		return sortMap(map,new Comparator<Map.Entry<A,T>>(){
			@Override
			public int compare(Entry<A,T> o1, Entry<A,T> o2){
				return o1.getKey().compareTo(o2.getKey());
			}
		});
	}
	
	public static <A extends Comparable<? super A>,T> Map<A,T> sortMapKeyDecending(Map<A,T> map){
		return sortMap(map,new Comparator<Map.Entry<A,T>>(){
			@Override
			public int compare(Entry<A,T> o1, Entry<A,T> o2) {
				return o2.getKey().compareTo(o1.getKey());
			}
		});
	}
	
	public static <A,T> Map<A,T> sortMapValue(Map<A,T> map, final Comparator<? super T> comparator){
		return sortMap(map,new Comparator<Map.Entry<A,T>>(){
			@Override
			public int compare(Entry<A,T> o1, Entry<A,T> o2){
				return comparator.compare(o1.getValue(), o2.getValue());
			}
		});
	}
	
	public static <A,T extends Comparable<? super T>> Map<A,T> sortMapValueAscending(Map<A,T> map){
		return sortMap(map,new Comparator<Map.Entry<A,T>>(){
			@Override
			public int compare(Entry<A,T> o1, Entry<A,T> o2) {
				return o1.getValue().compareTo(o2.getValue());
			}
		});
	}
	
	public static <A,T extends Comparable<? super T>> Map<A,T> sortMapValueDecending(Map<A,T> map){
		return sortMap(map,new Comparator<Map.Entry<A,T>>(){
			@Override
			public int compare(Entry<A,T> o1, Entry<A,T> o2) {
				return o2.getValue().compareTo(o1.getValue());
			}
		});
	}
	
	public static <A> Map<A,List<Integer>> sortMapByIndexDecending(Map<A,List<Integer>> map, final int index, boolean sep){
		return sortMap(map,new Comparator<Map.Entry<A,List<Integer>>>(){
			@Override
			public int compare(Entry<A, List<Integer>> o1, Entry<A, List<Integer>> o2) {
				return iDecendingComp.compare(o1.getValue().get(index), o2.getValue().get(index));
			}
		});
	}
	
	public static <A> Map<String,A> sortMapByClassNameKeyAscending(Map<String,A> map){
		return sortMap(map,new Comparator<Map.Entry<String,A>>(){
			@Override
			public int compare(Entry<String, A> o1,Entry<String, A> o2) {
				return sComp.compare(o1.getKey(), o2.getKey());
			}
		});
	}
	
	public static <A> Map<A,AtomicInteger> sortMapByAtomicIntegerValueDecending(Map<A,AtomicInteger> map){
		return sortMap(map,new Comparator<Map.Entry<A, AtomicInteger>>(){
			@Override
			public int compare(Entry<A,AtomicInteger> o1,Entry<A,AtomicInteger> o2){
				return Integer.compare(o2.getValue().get(), o1.getValue().get());
			}
		});
	}
	
	public static Map<String,String> sortMapStringsKeyValueAscending(Map<String,String> map){
		return sortMap(map,new Comparator<Map.Entry<String,String>>(){
			@Override
			public int compare(Entry<String, String> o1,Entry<String, String> o2) {
				int r = sComp.compare(o1.getKey(), o2.getKey());
				if(r == 0){
					return sComp.compare(o1.getValue(), o2.getValue());
				}
				return r;
			}
		});
	}
	
}
