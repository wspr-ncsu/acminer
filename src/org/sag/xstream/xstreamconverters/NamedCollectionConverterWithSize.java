package org.sag.xstream.xstreamconverters;

import java.util.Collection;

import org.sag.common.tools.HashSetWithGet;
import org.sag.common.tools.LinkedHashSetWithGet;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.converters.extended.NamedCollectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

public class NamedCollectionConverterWithSize extends NamedCollectionConverter{

	private boolean size;
	private ConverterLookup converterLookup;
	private Class<?> containerType;
	
	public NamedCollectionConverterWithSize(Mapper mapper, String itemName, Class<?> itemType, ConverterLookup converterLookup){
		this(null,mapper,itemName,itemType,converterLookup);
	}
	
	public NamedCollectionConverterWithSize(Class<?> type, Mapper mapper, String itemName, Class<?> itemType, ConverterLookup converterLookup){
		this(type,mapper,itemName,itemType,true,converterLookup);
	}
	
	public NamedCollectionConverterWithSize(Mapper mapper, String itemName, Class<?> itemType, boolean size, ConverterLookup converterLookup){
		this(null,mapper,itemName,itemType,size,converterLookup);
	}
	
	public NamedCollectionConverterWithSize(Class<?> type, Mapper mapper, String itemName, Class<?> itemType, boolean size, ConverterLookup converterLookup){
		super(type, mapper, itemName, itemType);
		this.size = size;
		this.converterLookup = converterLookup;
		this.containerType = type;
	}
	
	public boolean canConvert(@SuppressWarnings("rawtypes") Class type){
		if(this.containerType == null && (type.equals(HashSetWithGet.class) || type.equals(LinkedHashSetWithGet.class)))
			return true;
		return super.canConvert(type);
	}
	
	protected SingleValueConverter getConverter(String name, Class<?> type){
		Converter c = converterLookup.lookupConverterForType(type);
		if (c == null || !(c instanceof SingleValueConverter)) {
			ConversionException exception = new ConversionException(c == null ? 
					"Failed to lookup converter." : "No single value converter for object.");
			exception.add("attribute", name);
			exception.add("type", type.getName());
			throw exception;
		}
		return (SingleValueConverter)c;
	}
	
	@SuppressWarnings("rawtypes")
	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context){
		Collection collection = (Collection) source;
		if(size){
			writer.addAttribute("Size", getConverter("Size",Integer.class).toString(collection.size()));
		}
		super.marshal(source, writer, context);
	}

}
