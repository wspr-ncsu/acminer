package org.sag.xstream.xstreamconverters;

import java.util.Iterator;
import java.util.Map;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.AbstractCollectionConverter;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

public class MapConverter extends AbstractCollectionConverter {

	private final Class<?> type;

	public MapConverter(Mapper mapper) {
		this(mapper, null);
	}

	public MapConverter(Mapper mapper, Class<?> type) {
		super(mapper);
		this.type = type;
		if (type != null && !Map.class.isAssignableFrom(type)) {
			throw new IllegalArgumentException(type + " not of type " + Map.class);
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean canConvert(Class type) {
		return Map.class.isAssignableFrom(type);
	}
	
	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
		Map<?,?> map = (Map<?,?>) source;
		String entryName = mapper().serializedClass(Map.Entry.class);
		for (Iterator<?> iterator = map.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry<?,?> entry = (Map.Entry<?,?>) iterator.next();
			ExtendedHierarchicalStreamWriterHelper.startNode(writer, entryName, entry.getClass());

			writeItem(entry.getKey(), context, writer);
			writeItem(entry.getValue(), context, writer);

			writer.endNode();
		}
	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		Map<Object,Object> map = createCollection(context.getRequiredType());
		while (reader.hasMoreChildren()) {
			reader.moveDown();
			
			reader.moveDown();
			Object key = readItem(reader, context, map);
			reader.moveUp();

			reader.moveDown();
			Object value = readItem(reader, context, map);
			reader.moveUp();
			
			map.put(key, value);
			
			reader.moveUp();
		}
		return map;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected Map<Object,Object> createCollection(Class type) {
		return (Map<Object,Object>)super.createCollection(this.type != null ? this.type : type);
	}
	
}
