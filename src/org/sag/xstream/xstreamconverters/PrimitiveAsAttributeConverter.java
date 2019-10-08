package org.sag.xstream.xstreamconverters;

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

public class PrimitiveAsAttributeConverter implements Converter, XStreamNeededConverter{
	
	private String atrName;
	private Class<?> atrType;
	private ConverterLookup converterLookup;
	private Mapper mapper;

	public PrimitiveAsAttributeConverter(String atrName, Class<?> atrType, XStream xstream){
		this.atrName = atrName;
		this.atrType = atrType;
		setXStream(xstream);
	}
	
	public PrimitiveAsAttributeConverter(String atrName, Class<?> atrType){
		this(atrName,atrType,null);
	}
	
	private SingleValueConverter getConverter(Class<?> type){
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
		return atrType.isAssignableFrom(arg0);
	}

	@Override
	public void marshal(Object arg0, HierarchicalStreamWriter writer, MarshallingContext context) {
		if(arg0 != null){
			Class<?> type = mapper.defaultImplementationOf(atrType);
			SingleValueConverter converter = getConverter(type);
			String val = converter.toString(arg0);
			if(val == null){
				final ConversionException exception = new ConversionException("Cannot write value element as attribute");
				exception.add("attribute", atrName);
				exception.add("type", type.getName());
				throw exception;
			}
			writer.addAttribute(atrName, val);
		}
	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		String strVal = reader.getAttribute(atrName);
		if(strVal == null){
			return null;
		}else{
			Class<?> type = mapper.defaultImplementationOf(atrType);
			SingleValueConverter converter = getConverter(type);
			Object val = converter.fromString(strVal);
			if (val == null || !atrType.isAssignableFrom(val.getClass())) {
				ConversionException exception = new ConversionException("Cannot assign object to type");
				exception.add("object type", val == null ? "null" : val.getClass().getName());
				exception.add("target type", type.getName());
				throw exception;
			}
			return val;
		}
	}

	@Override
	public void setXStream(XStream xs) {
		if(xs != null){
			this.converterLookup = xs.getConverterLookup();
			this.mapper = xs.getMapper();
		}else{
			this.converterLookup = null;
			this.mapper = null;
		}
	}

}
