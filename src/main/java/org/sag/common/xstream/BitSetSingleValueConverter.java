package org.sag.common.xstream;

import java.util.BitSet;
import com.thoughtworks.xstream.converters.SingleValueConverter;

public class BitSetSingleValueConverter implements SingleValueConverter{

	@Override
	public boolean canConvert(@SuppressWarnings("rawtypes") Class clazz) {
		return BitSet.class.isAssignableFrom(clazz);
	}

	@Override
	public Object fromString(String arg0) {
		BitSet b = new BitSet();
		
		if(arg0.isEmpty())
			return b;
		
		String[] strings = arg0.split(",");
		for(int i = 0; i < strings.length; i++){
			b.set(i, strings[i].equals("1") ? true : false);
		}
		return b;
	}

	@Override
	public String toString(Object arg0) {
		BitSet collection = (BitSet) arg0;
		StringBuffer sb = new StringBuffer();
		boolean first = true;
		for(int i = 0; i < collection.length(); i++){
			if(!first)
				sb.append(",");
			else
				first = false;
			sb.append(collection.get(i) ? "1" : "0");
		}
		return sb.toString();
	}
	
}
