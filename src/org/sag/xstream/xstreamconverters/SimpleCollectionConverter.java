package org.sag.xstream.xstreamconverters;

import java.util.Collection;
import java.util.LinkedList;
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

public class SimpleCollectionConverter implements Converter {

	private String nodeName;
	private String attributeName;
	@SuppressWarnings("rawtypes")
	private Class entryType;
	private ConverterLookup converterLookup;
	private Mapper mapper;
	
	public SimpleCollectionConverter(String nodeName, String attributeName, @SuppressWarnings("rawtypes") Class entryType, XStream context){
		this.nodeName = nodeName;
		this.attributeName = attributeName;
		this.entryType = entryType;
		this.converterLookup = context.getConverterLookup();
		this.mapper = context.getMapper();
	}
	
	@Override
	public boolean canConvert(@SuppressWarnings("rawtypes") Class arg0) {
		return Collection.class.isAssignableFrom(arg0);
	}
	
	private SingleValueConverter getConverter(@SuppressWarnings("rawtypes") Class type){
		Converter converter = converterLookup.lookupConverterForType(type);
		if (converter == null || !(converter instanceof SingleValueConverter)) {
			ConversionException exception = new ConversionException(converter == null ? 
					"Failed to lookup converter." : "No single value converter for object.");
			exception.add("attribute", attributeName);
			exception.add("type", type.getName());
			throw exception;
		}
		return (SingleValueConverter)converter;
	}

	@Override
	public void marshal(Object arg0, HierarchicalStreamWriter writer, MarshallingContext context) {
		@SuppressWarnings("unchecked")
		Collection<Object> list = (Collection<Object>) arg0;
		@SuppressWarnings("rawtypes")
		Class type = mapper.defaultImplementationOf(entryType);
		SingleValueConverter converter = getConverter(type);
		
		for(Object entry : list){
			writer.startNode(nodeName);
			if(entry == null){
				writer.addAttribute(attributeName, "null");
			}else{
				String strVal = converter.toString(entry);
				if(strVal == null){
					final ConversionException exception = new ConversionException("Cannot write element as attribute");
					exception.add("attribute", attributeName);
					exception.add("type", type.getName());
					throw exception;
				}
				writer.addAttribute(attributeName, strVal);
			}
			writer.endNode();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		LinkedList<Object> list = new LinkedList<Object>();
		@SuppressWarnings("rawtypes")
		Class type = mapper.defaultImplementationOf(entryType);
		SingleValueConverter converter = getConverter(type);
		
		while (reader.hasMoreChildren()) {
			reader.moveDown();
			String strVal = reader.getAttribute(attributeName);
			if(strVal.equals("null")){
				list.add(null);
			}else{
				Object value = converter.fromString(strVal);
				if (value == null || !type.isAssignableFrom(value.getClass())) {
					ConversionException exception = new ConversionException("Cannot assign object to type");
					exception.add("object type", value == null ? "null" : value.getClass().getName());
					exception.add("target type", type.getName());
					throw exception;
				}
				list.add(value);
			}
			reader.moveUp();
		}
		return list;
	}

}
