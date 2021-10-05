package org.sag.common.tools;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class NumberOperations {

	//Has to return a BigDecimal to preserve the number of decimal places when they are 0
	public static BigDecimal round(float d, int decimalPlace) {
		return round(new BigDecimal(Float.toString(d)),decimalPlace);
	}
	
	public static BigDecimal round(double d, int decimalPlace) {
		return round(new BigDecimal(Double.toString(d)),decimalPlace);
	}
	
	public static BigDecimal round(BigDecimal bd, int decimalPlace) {
		bd = bd.setScale(decimalPlace, BigDecimal.ROUND_DOWN);
		return bd;
	}
	
	public static BigDecimal[] largestRemainderRounding(float[] vals, int decimalPlace, float sumGoal){
		if(vals == null){ return null; }
		if(vals.length == 0){ return new BigDecimal[0]; }
		BigDecimal[] ret = new BigDecimal[vals.length];
		Map<Integer,BigDecimal> remainders = new HashMap<Integer,BigDecimal>();
		BigDecimal sumTotalBD = round(0,decimalPlace);
		for(int i = 0; i < vals.length; i++){
			ret[i] = round(vals[i],decimalPlace);
			sumTotalBD = sumTotalBD.add(ret[i]);
			remainders.put(i,new BigDecimal(Float.toString(vals[i])).subtract(ret[i]));
		}
		BigDecimal rem = round(sumGoal,decimalPlace).subtract(sumTotalBD);
		BigDecimal addVal = new BigDecimal(1);
		addVal = addVal.scaleByPowerOfTen(-1 * decimalPlace);
		BigDecimal zero = new BigDecimal(0);
		remainders = SortingMethods.sortMapValueDecending(remainders);
		while(zero.max(rem) != zero){
			for(Entry<Integer,BigDecimal> e : remainders.entrySet()){
				int i = e.getKey();
				ret[i] = ret[i].add(addVal);
				rem = rem.subtract(addVal);
				if(zero.max(rem) == zero){
					break;
				}
			}
		}
		return ret;
	}
	
	//inclusive, returns negative when it is not a positive integer or when it is not within the range
	public static int getPositiveIntegerWithinRange(String s, int lower, int upper){
		return getPositiveIntegerWithinRange(s,10,lower,upper);
	}
	
	public static int getPositiveIntegerWithinRange(String s, int radix, int lower, int upper){
		if(isPositiveInteger(s,radix)){
			int val = Integer.parseInt(s, radix);
			if(val >= lower && val <= upper){
				return val;
			}
		}
		return -1;
	}
	
	public static boolean isPositiveInteger(String s){
		return isPositiveInteger(s,10);
	}
	
	public static boolean isPositiveInteger(String s, int radix){
		if(s.isEmpty()) return false;
		for(int i = 0; i < s.length(); i++) {
			if(Character.digit(s.charAt(i),radix) < 0) return false;
		}
		return true;
	}
	
	public static boolean isInteger(String s){
		return isInteger(s,10);
	}
	
	public static boolean isInteger(String s, int radix){
		if(s.isEmpty()) return false;
		for(int i = 0; i < s.length(); i++) {
			if(i == 0 && s.charAt(i) == '-') {
				if(s.length() == 1) return false;
				else continue;
			}
			if(Character.digit(s.charAt(i),radix) < 0) return false;
		}
		return true;
	}
}
