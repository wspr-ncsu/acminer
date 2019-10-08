package org.sag.xstream.xstreamconverters;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Objects;

import org.sag.common.tuple.Pair;
import org.sag.common.tuple.Quad;
import org.sag.common.tuple.Triple;
import org.sag.common.tuple.Tuple;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class TupleConverter implements Converter {
	
	private final String tupleLabel;
	private final Class<?> tupleType;
	private final String[] lables;
	private final Class<?>[] types;
	private final ConverterLookup lookup;
	private final boolean valuesAsAttributes;
	private final boolean noEntryForTuple;
	
	private static final <A> A[] sameElementArr(A o, int size) {
		@SuppressWarnings("unchecked")
		A[] ret = (A[]) Array.newInstance(o.getClass(), size);
		Arrays.fill(ret, o);
		return ret;
	}
	
	public TupleConverter(Class<?> tupleType, String label, Class<?> type, int size, ConverterLookup lookup) {
		this(tupleType,label,type,size,true,lookup);
	}
	
	public TupleConverter(Class<?> tupleType, String label, Class<?> type, int size, boolean valuesAsAttributes, ConverterLookup lookup) {
		this(tupleType,sameElementArr(label,size),sameElementArr(type,size),valuesAsAttributes,lookup);
	}
	
	public TupleConverter(Class<?> tupleType, String[] lables, Class<?>[] types, boolean valuesAsAttributes, ConverterLookup lookup) {
		this(null,tupleType,lables,types,true,valuesAsAttributes,lookup);
	}
	
	public TupleConverter(String tupleLabel, Class<?> tupleType, String[] lables, Class<?>[] types, boolean noEntryForTuple, 
			boolean valuesAsAttributes, ConverterLookup lookup) {
		Objects.requireNonNull(tupleType);
		Objects.requireNonNull(lables);
		Objects.requireNonNull(types);
		Objects.requireNonNull(lookup);
		if(lables.length != types.length)
			throw new IllegalArgumentException();
		if(!noEntryForTuple && tupleLabel == null)
			throw new IllegalArgumentException();
		
		this.tupleLabel = tupleLabel;
		this.lables = lables;
		this.valuesAsAttributes = valuesAsAttributes;
		this.lookup = lookup;
		this.noEntryForTuple = noEntryForTuple;
		this.types = types;
		this.tupleType = tupleType;
	}

	@Override
	public boolean canConvert(@SuppressWarnings("rawtypes") Class arg0) {
		return arg0.equals(Tuple.class) || arg0.equals(Pair.class) || arg0.equals(Quad.class) || arg0.equals(Triple.class);
	}
	
	private SingleValueConverter getSingleValueConverter(Class<?> type){
		Converter converter = lookup.lookupConverterForType(type);
		if (converter == null || !(converter instanceof SingleValueConverter)) {
			ConversionException exception = new ConversionException(converter == null ? 
					"Failed to lookup converter." : "No single value converter for object.");
			exception.add("type", type.getName());
			throw exception;
		}
		return (SingleValueConverter)converter;
	}

	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
		Tuple tuple = (Tuple)source;
		if(!noEntryForTuple)
			writer.startNode(tupleLabel);
		for(int i = 0; i < tuple.size(); i++) {
			Object o = tuple.get(i);
			if(valuesAsAttributes) {
				if(o == null)
					writer.addAttribute(lables[i], "NULL");
				else
					writer.addAttribute(lables[i], getSingleValueConverter(types[i]).toString(o));
			} else {
				writer.startNode(lables[i]);
				if(o == null)
					writer.setValue("NULL");
				else
					context.convertAnother(o);
				writer.endNode();
			}
		}
		if(!noEntryForTuple)
			writer.endNode();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		Object[] objs = new Object[lables.length];
		if(!noEntryForTuple)
			reader.moveDown();
		for(int i = 0; i < lables.length; i++) {
			if(valuesAsAttributes) {
				String strVal = reader.getAttribute(lables[i]);
				if(strVal.equals("NULL")) {
					objs[i] = null;
				} else {
					Object o = getSingleValueConverter(types[i]).fromString(strVal);
					if (o == null || !types[i].isAssignableFrom(o.getClass())) {
						ConversionException exception = new ConversionException("Cannot assign object to type");
						exception.add("object type", o == null ? "null" : o.getClass().getName());
						exception.add("target type", types[i].getName());
						throw exception;
					}
					objs[i] = o;
				}
			} else {
				reader.moveDown();
				if(reader.getValue().equals("NULL")) {
					objs[i] = null;
				} else {
					Object o = context.convertAnother(null, types[i]);
					if (o == null || !types[i].isAssignableFrom(o.getClass())) {
						ConversionException exception = new ConversionException("Cannot assign object to type");
						exception.add("object type", o == null ? "null" : o.getClass().getName());
						exception.add("target type", types[i].getName());
						throw exception;
					}
					objs[i] = o;
				}
				reader.moveUp();
			}
		}
		if(!noEntryForTuple)
			reader.moveUp();
		if(tupleType.equals(Pair.class))
			return new Pair(objs[0],objs[1]);
		else if(tupleType.equals(Triple.class))
			return new Triple(objs[0],objs[1],objs[2]);
		else if(tupleType.equals(Quad.class))
			return new Quad(objs[0],objs[1],objs[2],objs[3]);
		else
			return new Tuple(objs);
	}

}
