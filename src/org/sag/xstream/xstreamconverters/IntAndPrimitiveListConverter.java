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

public class IntAndPrimitiveListConverter<B> implements Converter, XStreamNeededConverter {

	private String nodeName;
	private Class<?> nodeType;
	private String nodeLabelName;
	private String[] atrNames;
	private ConverterLookup converterLookup;
	private Mapper mapper;
	
	public IntAndPrimitiveListConverter(String nodeName, Class<?> nodeType, String nodeLabelName, String[] atrNames, XStream xstream){
		this.nodeName = nodeName;
		this.nodeType = nodeType;
		this.nodeLabelName = nodeLabelName;
		this.atrNames = atrNames;
		setXStream(xstream);
	}
	
	public IntAndPrimitiveListConverter(String nodeName, Class<?> nodeType, String[] atrNames, XStream xstream){
		this(nodeName,nodeType,null,atrNames,xstream);
	}
	
	public IntAndPrimitiveListConverter(String nodeName, Class<?> nodeType, String atrName, XStream xstream){
		this(nodeName,nodeType,dupArr(atrName,new String[1]),xstream);
	}
	
	public IntAndPrimitiveListConverter(String nodeName, Class<?> nodeType, String nodeLabelName, String atrName, XStream xstream){
		this(nodeName,nodeType,nodeLabelName,dupArr(atrName,new String[1]),xstream);
	}
	
	public IntAndPrimitiveListConverter(String nodeName, Class<?> nodeType, String nodeLabelName, String[] atrNames){
		this(nodeName,nodeType,nodeLabelName,atrNames,null);
	}
	
	public IntAndPrimitiveListConverter(String nodeName, Class<?> nodeType, String[] atrNames){
		this(nodeName,nodeType,atrNames,null);
	}
	
	public IntAndPrimitiveListConverter(String nodeName, Class<?> nodeType, String nodeLabelName, String atrName){
		this(nodeName,nodeType,nodeLabelName,atrName,null);
	}
	
	public IntAndPrimitiveListConverter(String nodeName, Class<?> nodeType, String atrName){
		this(nodeName,nodeType,atrName,(XStream)null);
	}
	
	private static <T> T[] dupArr(T o, T[] ret){
		for(int i = 0; i < ret.length; i++){
			ret[i] = o;
		}
		return ret;
	}
	
	@Override
	public boolean canConvert(@SuppressWarnings("rawtypes") Class arg0) {
		return IntAndPrimitiveList.class.isAssignableFrom(arg0);
	}
	
	private SingleValueConverter getConverter(Class<?> type){
		Converter c = converterLookup.lookupConverterForType(type);
		if (c == null || !(c instanceof SingleValueConverter)) {
			ConversionException exception = new ConversionException(c == null ? 
					"Failed to lookup converter." : "No single value converter for object.");
			exception.add("type", type.getName());
			throw exception;
		}
		return (SingleValueConverter)c;
	}

	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
		if(source != null){
			Class<?> atrType = mapper.defaultImplementationOf(Integer.class);
			Class<?> nodeType = mapper.defaultImplementationOf(this.nodeType);
			Class<?> nodeLabelType = mapper.defaultImplementationOf(String.class);
			SingleValueConverter atrConverter = getConverter(atrType);
			SingleValueConverter nodeConverter = getConverter(nodeType);
			SingleValueConverter nodeLabelConverter = getConverter(nodeLabelType);
			@SuppressWarnings("unchecked")
			IntAndPrimitiveList<B> data = (IntAndPrimitiveList<B>)source;
			
			List<Integer> intList = data.getIntList();
			if(intList != null){
				if(atrNames.length != intList.size()){
					throw new ConversionException("List of names and values have unequal length.");
				}
				for(int i = 0; i < intList.size(); i++){
					Integer in = intList.get(i);
					if(in != null){
						String atrVal = atrConverter.toString(in);
						if(atrVal == null){
							final ConversionException exception = new ConversionException("Cannot write value element as attribute");
							exception.add("attribute", atrNames[i]);
							exception.add("type", atrType.getName());
							throw exception;
						}
						writer.addAttribute(atrNames[i], atrVal);
					}
				}
			}
			
			List<B> list = data.getList();
			List<String> labels = data.getLabels();
			if(list != null){
				int count = 0;
				for(B val : data.getList()){
					writer.startNode(nodeName);
					if(nodeLabelName != null && labels != null && count < labels.size()){
						String valLabel = labels.get(count);
						if(valLabel != null){
							String nodeValLabel = nodeLabelConverter.toString(valLabel);
							if(nodeValLabel == null){
								final ConversionException exception = new ConversionException("Cannot write value element as attribute");
								exception.add("attribute", nodeLabelName);
								exception.add("type", nodeLabelType.getName());
								throw exception;
							}
							writer.addAttribute(nodeLabelName, nodeValLabel);
						}
					}
					if(val != null){
						String nodeVal = nodeConverter.toString(val);
						if(nodeVal == null){
							final ConversionException exception = new ConversionException("Cannot write value element as attribute");
							exception.add("node", nodeName);
							exception.add("type", nodeType.getName());
							throw exception;
						}
						writer.setValue(nodeVal);
					}
					count++;
					writer.endNode();
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		List<B> list = new ArrayList<B>();
		List<String> labels = new ArrayList<String>();
		List<Integer> intList = new ArrayList<Integer>();
		
		Class<?> nodeLabelType = mapper.defaultImplementationOf(String.class);
		SingleValueConverter nodeLabelConverter = getConverter(nodeLabelType);
		Class<?> atrType = mapper.defaultImplementationOf(Integer.class);
		SingleValueConverter atrConverter = getConverter(atrType);
		boolean notAllNullAtr = false;
		for(int i = 0; i < atrNames.length; i++){
			String atrValStr = reader.getAttribute(atrNames[i]);
			if(atrValStr != null){
				notAllNullAtr = true;
				Object atrVal = atrConverter.fromString(atrValStr);
				if (atrVal == null || !Integer.class.isAssignableFrom(atrVal.getClass())) {
					ConversionException exception = new ConversionException("Cannot assign object to type");
					exception.add("object type", atrVal == null ? "null" : atrVal.getClass().getName());
					exception.add("target type", Integer.class.getName());
					throw exception;
				}
				intList.add((Integer)atrVal);
			}else{
				intList.add(null);
			}
		}
			
		boolean notAllNullNode = false;
		boolean notAllNullLabels = false;
		while(reader.hasMoreChildren()){
			reader.moveDown();
			if(!reader.getNodeName().equals(nodeName)){
				throw new ConversionException("Improper node name, expected " + nodeName + " but got " + reader.getNodeName() + ".");
			}
			if(nodeLabelName != null){
				String nodeValLabel = reader.getAttribute(nodeLabelName);
				if(nodeValLabel == null){
					labels.add(null);
				}else{
					notAllNullLabels = true;
					Object nodeLabel = nodeLabelConverter.fromString(nodeValLabel);
					if (nodeLabel == null || !nodeLabelType.isAssignableFrom(nodeLabel.getClass())) {
						ConversionException exception = new ConversionException("Cannot assign object to type");
						exception.add("object type", nodeLabel == null ? "null" : nodeLabel.getClass().getName());
						exception.add("target type", nodeLabelType.getName());
						throw exception;
					}
					labels.add((String)nodeLabel);
				}
			}
			String nodeValStr = reader.getValue();
			if(nodeValStr == null || nodeValStr.equals("")){
				list.add(null);
			}else{
				notAllNullNode = true;
				Class<?> nodeType = mapper.defaultImplementationOf(this.nodeType);
				SingleValueConverter nodeConverter = getConverter(nodeType);
				Object nodeVal = nodeConverter.fromString(nodeValStr);
				if (nodeVal == null || !nodeType.isAssignableFrom(nodeVal.getClass())) {
					ConversionException exception = new ConversionException("Cannot assign object to type");
					exception.add("object type", nodeVal == null ? "null" : nodeVal.getClass().getName());
					exception.add("target type", nodeType.getName());
					throw exception;
				}
				list.add((B)nodeVal);
			}
			reader.moveUp();
		}
		
		IntAndPrimitiveList<B> ret = new IntAndPrimitiveList<B>((List<Integer>)null,null,null);
		if(intList.size() == 0 || notAllNullAtr){
			ret.setIntList(intList);
		}
		if(list.size() == 0 || notAllNullNode){
			ret.setList(list);
		}
		if(notAllNullLabels){
			ret.setLabels(labels);
		}
		
		if(ret.getIntList() == null && ret.getList() == null && ret.getLabels() == null){
			return null;
		}
		return ret;
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
	
	public final static class IntAndPrimitiveList<A>{
		
		private List<Integer> intList;
		private List<A> list;
		private List<String> labels;
		
		public IntAndPrimitiveList(Integer i, List<A> list, List<String> labels){
			this.intList = new ArrayList<Integer>();
			this.intList.add(i);
			this.list = list;
			this.labels = labels;
		}
		
		public IntAndPrimitiveList(Integer i, List<A> list){
			this(i,list,null);
		}
		
		public IntAndPrimitiveList(List<Integer> intList, List<A> list, List<String> labels){
			this.intList = intList;
			this.list = list;
			this.labels = labels;
		}
		
		public IntAndPrimitiveList(List<Integer> intList, List<A> list){
			this(intList,list,null);
		}
		
		public List<Integer> getIntList(){
			return intList;
		}
		
		public List<A> getList(){
			return list;
		}
		
		public List<String> getLabels(){
			return labels;
		}
		
		public void setList(List<A> list){
			this.list = list;
		}
		
		public void setIntList(List<Integer> intList){
			this.intList = intList;
		}
		
		public void setLabels(List<String> labels){
			this.labels = labels;
		}
		
		public boolean equals(Object o){
			if(this == o){
				return true;
			}
			if(o == null || o.getClass() != this.getClass()){
				return false;
			}
			IntAndPrimitiveList<?> op = (IntAndPrimitiveList<?>)o;
			if(this.intList.equals(op.intList) && this.list.equals(op.list) && this.labels.equals(op.labels)){
				return true;
			}
			return false;
		}
		
		public int hashCode(){
			int i = 17;
			i = i * 31 + (intList == null ? 0 : intList.hashCode());
			i = i * 31 + (list == null ? 0 : list.hashCode());
			i = i * 31 + (labels == null ? 0 : labels.hashCode());
			return i;
		}
		
	}
	
}
