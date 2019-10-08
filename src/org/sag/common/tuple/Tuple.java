package org.sag.common.tuple;

import java.util.Objects;

public class Tuple {

	private final Object[] data;
	
	public Tuple(Object... data){
		if(data == null)
			throw new IllegalArgumentException("Error: An array must be provided.");
		this.data = data;
	}
	
	public Object get(int i){
		return data[i];
	}
	
	public int size(){
		return data.length;
	}
	
	public boolean equals(Object o){
		if(this == o)
			return true;
		if(o == null || !(o instanceof Tuple))
			return false;
		Tuple other = (Tuple)o;
		if(size() != other.size())
			return false;
		for(int i = 0; i < size(); i++){
			if(!Objects.equals(get(i), other.get(i)))
				return false;
		}
		return true;
	}
	
	public int hashCode(){
		int ret = 17;
		for(int i = 0; i < size(); i++){
			ret = ret * 31 + Objects.hashCode(get(i));
		}
		return ret;
	}
	
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append("(");
		for(int i = 0; i < size(); i++){
			if(i == size()-1)
				sb.append(Objects.toString(get(i),""));
			else
				sb.append(Objects.toString(get(i),"")).append(", ");
		}
		sb.append(")");
		return sb.toString();
	}
	
}
