package org.sag.xstream.xstreamconverters;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

public class StatsAsAttributeConverter implements Converter, XStreamNeededConverter {
	
	private String[] atrNames;
	private Class<?>[] atrTypes;
	private ConverterLookup converterLookup;
	private Mapper mapper;
	
	public StatsAsAttributeConverter(String[] atrNames, Class<?>[] atrTypes, XStream xstream){
		if(atrTypes.length != atrNames.length){
			throw new ConversionException("List of names and types have unequal length.");
		}
		this.atrNames = atrNames;
		this.atrTypes = atrTypes;
		if(xstream != null){
			this.converterLookup = xstream.getConverterLookup();
			this.mapper = xstream.getMapper();
		}else{
			this.converterLookup = null;
			this.mapper = null;
		}
	}
	
	public StatsAsAttributeConverter(String[] atrNames, Class<?> atrType, XStream xstream){
		this(atrNames,dupArr(atrType,new Class<?>[atrNames.length]),xstream);
	}
	
	public StatsAsAttributeConverter(String atrName, Class<?> atrType, XStream xstream){
		this(dupArr(atrName,new String[1]),atrType,xstream);
	}
	
	private static <T> T[] dupArr(T o, T[] ret){
		for(int i = 0; i < ret.length; i++){
			ret[i] = o;
		}
		return ret;
	}
	
	private SingleValueConverter getConverter(String atrName, Class<?> type){
		Converter c = converterLookup.lookupConverterForType(type);
		if (c == null || !(c instanceof SingleValueConverter)) {
			ConversionException exception = new ConversionException(c == null ? 
					"Failed to lookup converter." : "No single value converter for object.");
			exception.add("attribute", atrName);
			exception.add("type", type.getName());
			throw exception;
		}
		return (SingleValueConverter)c;
	}
	
	@Override
	public boolean canConvert(@SuppressWarnings("rawtypes") Class arg0) {
		return List.class.isAssignableFrom(arg0);
	}

	@Override
	public void marshal(Object arg0, HierarchicalStreamWriter writer, MarshallingContext context) {
		if(arg0 != null){
			@SuppressWarnings("unchecked")
			List<Object> list = (List<Object>)arg0;
			if(list.size() != atrNames.length){
				throw new ConversionException("Not enough attribute names for this list.");
			}
			for(int i = 0; i < list.size(); i++){
				if(list.get(i) != null){
					Class<?> type = mapper.defaultImplementationOf(atrTypes[i]);
					SingleValueConverter converter = getConverter(atrNames[i],type);
					String val = converter.toString(list.get(i));
					if(val == null){
						final ConversionException exception = new ConversionException("Cannot write value element as attribute");
						exception.add("attribute", atrNames[i]);
						exception.add("type", type.getName());
						throw exception;
					}
					writer.addAttribute(atrNames[i], val);
				}
			}
		}
	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		List<Object> ret = new ArrayList<Object>();
		boolean allNull = true;
		for(int i = 0; i < atrNames.length; i++){
			String strVal = reader.getAttribute(atrNames[i]);
			if(strVal == null){
				ret.add(null);
			}else{
				Class<?> type = mapper.defaultImplementationOf(atrTypes[i]);
				SingleValueConverter converter = getConverter(atrNames[i],type);
				Object val = converter.fromString(strVal);
				if (val == null || !atrTypes[i].isAssignableFrom(val.getClass())) {
					ConversionException exception = new ConversionException("Cannot assign object to type");
					exception.add("object type", val == null ? "null" : val.getClass().getName());
					exception.add("target type", type.getName());
					throw exception;
				}
				ret.add(val);
				allNull = false;
			}
		}
		if(!ret.isEmpty() && allNull){
			return null;
		}
		return ret;
	}

	@Override
	public void setXStream(XStream xs) {
		if(xs != null){
			this.converterLookup = xs.getConverterLookup();
			this.mapper = xs.getMapper();
		}
	}

}
