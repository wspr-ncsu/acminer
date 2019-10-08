package org.sag.xstream.xstreamconverters;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.MapConverter;
import com.thoughtworks.xstream.core.util.HierarchicalStreams;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

public class NamedMapConverterWithSize extends MapConverter {
	
	private final boolean size;
	private final String entryName;
	private final String keyName;
	private final Class<?> keyType;
	private final String valueName;
	private final Class<?> valueType;
	private final boolean keyAsAttribute;
	private final boolean valueAsAttribute;
	private final boolean handleValueExternally;
	private final ConverterLookup lookup;
	
	public static final int OFF_ALL = 0;
	public static final int ON_SIZE = 1;
	public static final int ON_KEYATR = 2;
	public static final int ON_VALATR = 4;
	public static final int ON_ALL = 7;
	public static final int ON_VALEXT = 8;
	
	public NamedMapConverterWithSize(Mapper mapper, String entryName, String keyName, String valueName, Class<?> keyValType, ConverterLookup lookup){
		this(null,mapper,entryName,keyName,keyValType,valueName,keyValType,ON_ALL,lookup);
	}
	
	public NamedMapConverterWithSize(Class<?> type, Mapper mapper, String entryName, String keyName, String valueName, Class<?> keyValType, ConverterLookup lookup){
		this(type,mapper,entryName,keyName,keyValType,valueName,keyValType,ON_ALL,lookup);
	}
	
	public NamedMapConverterWithSize(Class<?> type, Mapper mapper, String entryName, String keyName, String valueName, Class<?> keyValType, int boolArgs, ConverterLookup lookup){
		this(type,mapper,entryName,keyName,keyValType,valueName,keyValType,boolArgs,lookup);
	}
	
	public NamedMapConverterWithSize(Mapper mapper, String entryName, String keyName, String valueName, Class<?> keyValType, int boolArgs, ConverterLookup lookup){
		this(null,mapper,entryName,keyName,keyValType,valueName,keyValType,boolArgs,lookup);
	}
	
	public NamedMapConverterWithSize(Mapper mapper, String entryName, String keyName, Class<?> keyType, String valueName, Class<?> valueType, int boolArgs, ConverterLookup lookup){
		this(null,mapper,entryName,keyName,keyType,valueName,valueType,boolArgs,lookup);
	}
	
	public NamedMapConverterWithSize(Class<?> type, Mapper mapper, String entryName, String keyName, Class<?> keyType, String valueName, Class<?> valueType, int boolArgs, ConverterLookup lookup){
		this(type,mapper,entryName,keyName,keyType,valueName,valueType,
			(ON_KEYATR & boolArgs) == ON_KEYATR ? true : false,
			(ON_VALATR & boolArgs) == ON_VALATR ? true : false,
			(ON_SIZE & boolArgs) == ON_SIZE ? true : false,
			(ON_VALEXT & boolArgs) == ON_VALEXT ? true : false,lookup);
	}
	
	private NamedMapConverterWithSize(Class<?> type, Mapper mapper, String entryName, String keyName, Class<?> keyType,
		String valueName, Class<?> valueType, boolean keyAsAttribute, boolean valueAsAttribute, boolean size, boolean handleValueExternally, 
		ConverterLookup lookup) {
		super(mapper, type);
		this.entryName = entryName != null && entryName.length() == 0 ? null : entryName;
		this.keyName = keyName != null && keyName.length() == 0 ? null : keyName;
		this.keyType = keyType;
		this.valueName = handleValueExternally ? null : valueName != null && valueName.length() == 0 ? null : valueName;
		this.valueType = valueType;
		this.keyAsAttribute = keyAsAttribute;
		this.valueAsAttribute = handleValueExternally ? false : valueAsAttribute;
		this.lookup = lookup;
		this.size = size;
		this.handleValueExternally = handleValueExternally;

		if (this.keyType == null || this.valueType == null) {
			throw new IllegalArgumentException("Class types of key and value are mandatory");
		}
		if (this.entryName == null) {
			if (this.keyAsAttribute || this.valueAsAttribute) {
				throw new IllegalArgumentException(
					"Cannot write attributes to map entry, if map entry must be omitted");
			}
			if (this.valueName == null && !this.handleValueExternally) {
				throw new IllegalArgumentException(
					"Cannot write value as text of entry, if entry must be omitted");
			}
		}
		if (this.keyName == null) {
			throw new IllegalArgumentException("Cannot write key without name");
		}
		if (this.valueName == null && !this.handleValueExternally) {
			if (this.valueAsAttribute) {
				throw new IllegalArgumentException(
					"Cannot write value as attribute without name");
			} else if (!this.keyAsAttribute) {
				throw new IllegalArgumentException(
					"Cannot write value as text of entry, if key is also child element");
			}
		}
		if (this.keyAsAttribute && this.valueAsAttribute && this.keyName.equals(this.valueName)) {
			throw new IllegalArgumentException(
				"Cannot write key and value with same attribute name");
		}
	}
	
	private SingleValueConverter getSingleValueConverter(String name, Class<?> type){
		SingleValueConverter c = mapper().getConverterFromItemType(null, type, null);
		if(c == null) {
			Converter cc = lookup.lookupConverterForType(type);
			if(cc == null || !(cc instanceof SingleValueConverter)) {
				ConversionException exception = new ConversionException(cc == null ? 
						"Failed to lookup converter." : "No single value converter for object.");
				exception.add("attribute", Objects.toString(name));
				exception.add("type", type.getName());
				throw exception;
			}
			c = (SingleValueConverter)cc;
		}
		return c;
	}
	
	@SuppressWarnings("rawtypes")
	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
		Map map = (Map)source;
		SingleValueConverter keyConverter = null;
		SingleValueConverter valueConverter = null;
		if (keyAsAttribute)
			keyConverter = getSingleValueConverter(keyName,keyType);
		if (!handleValueExternally && (valueAsAttribute || valueName == null))
			valueConverter = getSingleValueConverter(valueName,valueType);
		
		if(size)
			writer.addAttribute("Size", getSingleValueConverter("Size",Integer.class).toString(map.size()));
		for (Iterator iterator = map.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry)iterator.next();
			Object key = entry.getKey();
			Object value = entry.getValue();
			if (entryName != null) {
				ExtendedHierarchicalStreamWriterHelper.startNode(writer, entryName, entry.getClass());
				if (keyConverter != null && key != null)
					writer.addAttribute(keyName, keyConverter.toString(key));
				if (valueName != null && valueConverter != null && value != null)
					writer.addAttribute(valueName, valueConverter.toString(value));
			}

			if (keyConverter == null)
				writeItem(keyName, keyType, key, context, writer);
			if(handleValueExternally) {
				context.convertAnother(value);
			} else {
				if (valueConverter == null)
					writeItem(valueName, valueType, value, context, writer);
				else if (valueName == null)
					writer.setValue(valueConverter.toString(value));
			}

			if (entryName != null) {
				writer.endNode();
			}
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void populateMap(HierarchicalStreamReader reader, UnmarshallingContext context,
		Map map, Map target) {
		SingleValueConverter keyConverter = null;
		SingleValueConverter valueConverter = null;
		if (keyAsAttribute) {
			keyConverter = getSingleValueConverter(keyName,keyType);
		}
		if (!handleValueExternally && (valueAsAttribute || valueName == null)) {
			valueConverter = getSingleValueConverter(keyName,valueType);
		}

		while (reader.hasMoreChildren()) {
			Object key = null;
			Object value = null;

			if (entryName != null) {
				reader.moveDown();

				if (keyConverter != null) {
					String attribute = reader.getAttribute(keyName);
					if (attribute != null) {
						key = keyConverter.fromString(attribute);
					}
				}

				if (valueAsAttribute && valueConverter != null) {
					String attribute = reader.getAttribute(valueName);
					if (attribute != null) {
						value = valueConverter.fromString(attribute);
					}
				}
			}

			if (keyConverter == null) {
				reader.moveDown();
				if (valueConverter == null
					&& !keyName.equals(valueName)
					&& reader.getNodeName().equals(valueName)) {
					value = readItem(valueType, reader, context, map);
				} else {
					key = readItem(keyType, reader, context, map);
				}
				reader.moveUp();
			}

			if(handleValueExternally) {
				context.convertAnother(map,valueType);
			} else {
				if (valueConverter == null) {
					reader.moveDown();
					if (keyConverter == null && key == null && value != null) {
						key = readItem(keyType, reader, context, map);
					} else {
						value = readItem(valueType, reader, context, map);
					}
					reader.moveUp();
				} else if (!valueAsAttribute) {
					value = reader.getValue();
				}
			}

			target.put(key, value);

			if (entryName != null) {
				reader.moveUp();
			}
		}
	}

	protected void writeItem(String name, Class<?> type, Object item, MarshallingContext context,
		HierarchicalStreamWriter writer) {
		Class<?> itemType = item == null ? Mapper.Null.class : item.getClass();
		ExtendedHierarchicalStreamWriterHelper.startNode(writer, name, itemType);
		if (!itemType.equals(type)) {
			String attributeName = mapper().aliasForSystemAttribute("class");
			if (attributeName != null) {
				writer.addAttribute(attributeName, mapper().serializedClass(itemType));
			}
		}
		if (item != null) {
			context.convertAnother(item);
		}
		writer.endNode();
	}

	protected Object readItem(Class<?> type, HierarchicalStreamReader reader,
		UnmarshallingContext context, Object current) {
		String className = HierarchicalStreams.readClassAttribute(reader, mapper());
		Class<?> itemType = className == null ? type : mapper().realClass(className);
		if (Mapper.Null.class.equals(itemType)) {
			return null;
		} else {
			return context.convertAnother(current, itemType);
		}
	}
	
}
