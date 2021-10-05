package org.sag.common.tools;


import java.util.ArrayList;
import java.util.List;

import soot.AnySubType;
import soot.ArrayType;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.ErroneousType;
import soot.FloatType;
import soot.IntType;
import soot.LongType;
import soot.NullType;
import soot.PrimType;
import soot.RefLikeType;
import soot.RefType;
import soot.ShortType;
import soot.StmtAddressType;
import soot.Type;
import soot.UnknownType;
import soot.VoidType;
import soot.baf.DoubleWordType;
import soot.baf.WordType;
import soot.coffi.Double2ndHalfType;
import soot.coffi.Long2ndHalfType;
import soot.coffi.UnusuableType;
import soot.jimple.toolkits.typing.fast.BottomType;
import soot.jimple.toolkits.typing.fast.Integer127Type;
import soot.jimple.toolkits.typing.fast.Integer1Type;
import soot.jimple.toolkits.typing.fast.Integer32767Type;

public class StringToType {

	public static PrimType stringToPrimType(String type){
		if(type.equals(IntType.v().toString())){
			return IntType.v();
		}else if(type.equals(LongType.v().toString())){
			return LongType.v();
		}else if(type.equals(ShortType.v().toString())){
			return ShortType.v();
		}else if(type.equals(Integer32767Type.v().toString())){
			return Integer32767Type.v();
		}else if(type.equals(Integer1Type.v().toString())){
			return Integer1Type.v();
		}else if(type.equals(Integer127Type.v().toString())){
			return Integer127Type.v();
		}else if(type.equals(FloatType.v().toString())){
			return FloatType.v();
		}else if(type.equals(DoubleType.v().toString())){
			return DoubleType.v();
		}else if(type.equals(CharType.v().toString())){
			return CharType.v();
		}else if(type.equals(ByteType.v().toString())){
			return ByteType.v();
		}else if(type.equals(BooleanType.v().toString())){
			return BooleanType.v();
		}
		return null;
	}
	
	//returns null and may throw an exception
	public static RefLikeType stringToRefType(String type){
		int index;
		if(type.equals(NullType.v().toString())){
			return NullType.v();
		}else if(type.startsWith("Any_subtype_of_")){
			RefType ref = (RefType) stringToRefType(type.replaceFirst("Any_subtype_of_", ""));
			if(ref != null){
				return AnySubType.v(ref);
			}
			return null;
		}else if((index = type.indexOf('[')) >= 0){
			String baseType = type.substring(0,index);
			String arrDim = type.substring(index,type.length());
			int dim = 0;
			Type ret;
			
			if(arrDim.length() % 2 == 0){
				for(int i = 0; i < arrDim.length(); i += 2){
					if(arrDim.charAt(i) == '[' && arrDim.charAt(i+1) == ']'){
						dim++;
					}else{
						return null;
					}
				}
			}else{
				return null;
			}
			
			if(baseType.length() > 0){
				ret = stringToPrimType(baseType);
				if(ret == null){
					ret = stringToRefType(baseType);
					if(ret == null){
						return null;
					}
				}
			}else{
				return null;
			}
			
			return ArrayType.v(ret, dim);
		}else{
			return RefType.v(type);
		}
	}
	
	//returns null and may throw an exception
	public static Type stringToType(String type){
		if(type.equals(WordType.v().toString())){
			return WordType.v();
		}else if(type.equals(VoidType.v().toString())){
			return VoidType.v();
		}else if(type.equals(UnusuableType.v().toString())){
			return UnusuableType.v();
		}else if(type.equals(UnknownType.v().toString())){
			return UnknownType.v();
		}else if(type.equals(StmtAddressType.v().toString())){
			return StmtAddressType.v();
		}else if(type.equals(Long2ndHalfType.v().toString())){
			return Long2ndHalfType.v();
		}else if(type.equals(ErroneousType.v().toString())){
			return ErroneousType.v();
		}else if(type.equals(DoubleWordType.v().toString())){
			return DoubleWordType.v();
		}else if(type.equals(Double2ndHalfType.v().toString())){
			return Double2ndHalfType.v();
		}else if(type.equals(BottomType.v().toString())){
			return BottomType.v();
		}
		Type ret = stringToPrimType(type);
		if(ret != null){
			return ret;
		}
		ret = stringToRefType(type);
		if(ret != null){
			return ret;
		}
		return null;
	}
	
	public static List<Type> stringsToTypes(List<String> types){
		ArrayList<Type> ret = new ArrayList<Type>();
		for(String s : types){
			Type t = stringToType(s);
			if(t == null){
				return null;
			}
			ret.add(t);
		}
		return ret;
	}
	
}
