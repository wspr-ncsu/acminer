package org.sag.xstream.xstreamconverters;

import java.util.Collection;
import java.util.Iterator;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.AbstractCollectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

public class CollectionConverter extends AbstractCollectionConverter {

	private final Class<?> type;

	public CollectionConverter(Mapper mapper) {
		this(mapper, null);
	}

	public CollectionConverter(Mapper mapper, Class<?> type) {
		super(mapper);
		this.type = type;
		if (type != null && !Collection.class.isAssignableFrom(type)) {
			throw new IllegalArgumentException(type + " not of type " + Collection.class);
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean canConvert(Class type) {
		return Collection.class.isAssignableFrom(type);
	}

	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
		Collection<?> collection = (Collection<?>) source;
		for (Iterator<?> iterator = collection.iterator(); iterator.hasNext();) {
			Object item = iterator.next();
			writeItem(item, context, writer);
		}
	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		Collection<Object> collection = (Collection<Object>) createCollection(context.getRequiredType());
		while (reader.hasMoreChildren()) {
			reader.moveDown();
			collection.add(readItem(reader, context, collection));
			reader.moveUp();
		}
		return collection;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected Collection<Object> createCollection(Class type) {
		return (Collection<Object>)super.createCollection(this.type != null ? this.type : type);
	}
	
}
