package org.sag.acminer.database;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sag.common.io.FileHash;
import org.sag.common.io.FileHashList;
import org.sag.common.tools.SortingMethods;
import org.sag.common.xstream.XStreamInOut;
import org.sag.common.xstream.XStreamInOut.XStreamInOutInterface;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterRegistry;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.FieldDictionary;
import com.thoughtworks.xstream.converters.reflection.FieldKey;
import com.thoughtworks.xstream.converters.reflection.FieldKeySorter;
import com.thoughtworks.xstream.converters.reflection.SunUnsafeReflectionProvider;
import com.thoughtworks.xstream.core.ClassLoaderReference;
import com.thoughtworks.xstream.core.DefaultConverterLookup;
import com.thoughtworks.xstream.core.util.CompositeClassLoader;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.XppDriver;
import com.thoughtworks.xstream.mapper.MapperWrapper;

@XStreamAlias("FileHashDatabase")
public class FileHashDatabase implements XStreamInOutInterface {

	@XStreamAlias("FileHashList")
	protected volatile FileHashList fhl;
	
	public List<FileHash> getFileHashList() {
		if(fhl == null)
			return Collections.emptyList();
		return fhl;
	}
	
	public void setFileHashList(FileHashList fhl) {
		if(fhl != null)
			this.fhl = fhl;
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}

	@Override
	public FileHashDatabase readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}
	
	public static FileHashDatabase readXMLStatic(String filePath, Path path) throws Exception {
		XStreamSetup temp = new XStreamSetup() {
			
			@Override
			public XStream getXStreamObject() {
				final DefaultConverterLookup cl = new DefaultConverterLookup() {
					@Override
					public Converter lookupConverterForType(@SuppressWarnings("rawtypes") final Class type) {
						if(FileHashDatabase.class.equals(type) || FileHashDatabase.class.isInstance(type))
							return new FileHashDatabaseConverter();
						return super.lookupConverterForType(type);
					}
				};
				final ConverterRegistry cr = new ConverterRegistry() {
					@Override
					public void registerConverter(final Converter converter, final int priority) {
						cl.registerConverter(converter, priority);
					}
				};
				return new XStream(null, new XppDriver(), new ClassLoaderReference(new CompositeClassLoader()), null, cl, cr){
					@Override
					protected MapperWrapper wrapMapper(MapperWrapper next) {
						return new MapperWrapper(next) {
							@Override
							public Class<?> realClass(final String elementName) {
								try {
									return super.realClass(elementName);
								} catch(Throwable t) {
									return FileHashDatabase.class;
								}
							}
						};
					}
				};
			}

			@Override
			public void setXStreamOptions(XStream xstream) {
				super.setXStreamOptions(xstream);
				xstream.registerConverter(new FileHashDatabaseConverter(), XStream.PRIORITY_VERY_HIGH);
			}
			
		};
		
		return (FileHashDatabase)XStreamInOut.readXML(filePath,path,temp.getXStream());
	}

	@XStreamOmitField
	private static AbstractXStreamSetup xstreamSetup = null;

	public static AbstractXStreamSetup getXStreamSetupStatic(){
		if(xstreamSetup == null)
			xstreamSetup = new XStreamSetup();
		return xstreamSetup;
	}
	
	@Override
	public AbstractXStreamSetup getXStreamSetup() {
		return getXStreamSetupStatic();
	}
	
	public static class XStreamSetup extends AbstractXStreamSetup {
		
		@Override
		public XStream getXStreamObject() {
			return new XStream(new SunUnsafeReflectionProvider(new FieldDictionary(new FieldKeySorter() {
				@SuppressWarnings({ "rawtypes", "unchecked" })
				@Override
				public Map<FieldKey, Field> sort(final Class type, final Map keyedByFieldKey) {
					return SortingMethods.sortMapKey(keyedByFieldKey, new Comparator<FieldKey>() {
						public int compare(final FieldKey fieldKey1, final FieldKey fieldKey2) {
							boolean isClass1 = fieldKey1.getDeclaringClass().equals(FileHashDatabase.class);
							boolean isClass2 = fieldKey2.getDeclaringClass().equals(FileHashDatabase.class);
							if(isClass1 && isClass2) {
								return fieldKey1.getOrder() - fieldKey2.getOrder();
							} else if(isClass1) {
								return -1;
							} else if(isClass2) {
								return 1;
							} else{
								int i = fieldKey2.getDepth() - fieldKey1.getDepth();
								if (i == 0) {
									i = fieldKey1.getOrder() - fieldKey2.getOrder();
								}
								return i;
							}
						}
					});
				}
			})));
		}
		
		@Override
		public void getOutputGraph(LinkedHashSet<AbstractXStreamSetup> in) {
			if(!in.contains(this)) {
				in.add(this);
				FileHashList.getXStreamSetupStatic().getOutputGraph(in);
			}
		}
		
		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(FileHashDatabase.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}
	
	private static final class FileHashDatabaseConverter implements Converter {

		@Override
		public boolean canConvert(@SuppressWarnings("rawtypes") Class arg0) {
			return FileHashDatabase.class.isInstance(arg0) || FileHashDatabase.class.equals(arg0);
		}

		@Override
		public void marshal(Object arg0, HierarchicalStreamWriter arg1, MarshallingContext arg2) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
			FileHashDatabase ret = new FileHashDatabase();
			FileHashList fhl = null;
			while (reader.hasMoreChildren()) {
				if(fhl != null)
					break;
				reader.moveDown();
				if(reader.getNodeName().equals("FileHashList")){
					Object o = context.convertAnother(null, FileHashList.class);
					fhl = (FileHashList)o;
					break;
				}
				reader.moveUp();
			}
			
			if(fhl == null)
				throw new RuntimeException("Error: Unable to read in the FileHashDatabase.");
			
			ret.setFileHashList(fhl);
			return ret;
		}
		
	}
	
}
