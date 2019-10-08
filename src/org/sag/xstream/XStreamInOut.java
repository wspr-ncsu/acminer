package org.sag.xstream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import org.sag.common.tools.SortingMethods;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.FieldDictionary;
import com.thoughtworks.xstream.converters.reflection.FieldKey;
import com.thoughtworks.xstream.converters.reflection.FieldKeySorter;
import com.thoughtworks.xstream.converters.reflection.SunUnsafeReflectionProvider;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;

public class XStreamInOut {
	
	/* The following methods are used to write an object to an xml file assuming the object implements the 
	 * XStreamInOutInterface. This interface provides these methods with the information needed to write
	 * the object to a file. These methods are also designed to write an object of a known type so they
	 * are type safe.
	 */
	
	public static <A extends XStreamInOutInterface> void writeXML(A o, String filePath, Path path) throws Exception {
		if(filePath != null)
			writeXML(o, filePath);
		else if(path != null)
			writeXML(o, path);
		else
			throw new Exception("Both filePath and path cannot be null at the same time!");
	}

	public static <A extends XStreamInOutInterface> void writeXML(A o, String filePath) throws Exception {
		writeXML(o,filePath,o.getXStreamSetup().getXStream());
	}
	
	public static <A extends XStreamInOutInterface> void writeXML(A o, Path path) throws Exception {
		writeXML(o,path,o.getXStreamSetup().getXStream());
	}
	
	public static void writeXML(Object o, String filePath, XStream xstream) throws Exception {
		writeXML(o, Paths.get(filePath).toAbsolutePath(), xstream);
	}
	
	public static void writeXML(Object o, Path path, XStream xstream) throws Exception {
		writeXML(o, Files.newOutputStream(path), xstream);
	}
	
	public static void writeXML(Object o, OutputStream os, XStream xstream) throws Exception {
		Writer writer = new OutputStreamWriter(os, "UTF-8");
		writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
		char[] c = {'\t'};
		PrettyPrintWriter ppw = new PrettyPrintWriter(writer,PrettyPrintWriter.XML_1_0,c);
		xstream.marshal(o, ppw);
		ppw.flush();
		ppw.close();
	}
	
	/* The following are methods for writing the XML formated data to a string instead
	 * of a file.
	 */
	
	public static <A extends XStreamInOutInterface> String writeXMLToString(A o) throws Exception {
		return writeXMLToString(o,o.getXStreamSetup().getXStream());
	}
	
	public static String writeXMLToString(Object o, XStream xstream) throws Exception {
		try(ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			writeXML(o, os, xstream);
			return os.toString("UTF-8");
		}
	}
	
	/* The following methods are used to read an object from an xml file assuming the object implements the 
	 * XStreamInOutInterface. This interface provides these methods with the information needed to read
	 * the object to a file. These methods are also designed to read an object of a known type so they
	 * are type safe. However, to be type safe, an uninitialized version of the object being read in is
	 * required. As such, it is not possible to read in an object of an unknown type with these methods.
	 */
	
	public static <A extends XStreamInOutInterface> A readXML(A o, String filePath, Path path) throws Exception {
		if(filePath != null)
			return readXML(o, filePath);
		else if(path != null)
			return readXML(o, path);
		else
			throw new Exception("Both filePath and path cannot be null at the same time!");
	}
	
	@SuppressWarnings("unchecked")
	public static <A extends XStreamInOutInterface> A readXML(A o, String filePath) throws Exception {
		return (A)readXML(o,filePath,o.getXStreamSetup().getXStream());
	}
	
	@SuppressWarnings("unchecked")
	public static <A extends XStreamInOutInterface> A readXML(A o, Path path) throws Exception {
		return (A)readXML(o,path,o.getXStreamSetup().getXStream());
	}
	
	public static Object readXML(Object o, String filePath, XStream xstream) throws Exception {
		return readXML(o, Paths.get(filePath).toAbsolutePath(), xstream);
	}
	
	public static Object readXML(Object o, Path path, XStream xstream) throws Exception {
		return readXML(o, Files.newInputStream(path), xstream);
	}
	
	public static Object readXML(Object o, InputStream is, XStream xstream) throws Exception {
		Reader reader = new InputStreamReader(is, "UTF-8");
		o = xstream.fromXML(reader,o);
		reader.close();
		return o;
	}
	
	/* The following are methods for reading the XML formated data from a string instead
	 * of a file.
	 */
	
	@SuppressWarnings("unchecked")
	public static <A extends XStreamInOutInterface> A readXMLFromString(A o, String in) throws Exception {
		return (A)readXMLFromString(o,in,o.getXStreamSetup().getXStream());
	}
	
	public static Object readXMLFromString(Object o, String in, XStream xstream) throws Exception {
		try(ByteArrayInputStream is = new ByteArrayInputStream(in.getBytes("UTF-8"))) {
			return readXML(o, is, xstream);
		}
	}
	
	/* These methods read in an object from an xml file whose type is unknown. As these objects
	 * can be anything, all configuration information for xstream must be performed before hand.
	 * Moreover, these methods are not type safe and can return an object of any type. These may
	 * be used to read in objects who are subclasses of some parent type.
	 */
	
	public static Object readXML(String filePath, Path path, XStream xstream) throws Exception {
		if(filePath != null)
			return readXML(filePath,xstream);
		else if(path != null)
			return readXML(path,xstream);
		else
			throw new Exception("Both filePath and path cannot be null at the same time!");
	}
	
	public static Object readXML(String filePath, XStream xstream) throws Exception {
		return readXML(Paths.get(filePath).toAbsolutePath(), xstream);
	}
	
	public static Object readXML(Path path, XStream xstream) throws Exception {
		return readXML(Files.newInputStream(path), xstream);
	}
	
	public static Object readXML(InputStream is, XStream xstream) throws Exception {
		Reader reader = new InputStreamReader(is, "UTF-8");
		Object o = xstream.fromXML(reader);
		reader.close();
		return o;
	}
	
	public static Object readXMLFromString(String in, XStream xstream) throws Exception {
		try(ByteArrayInputStream is = new ByteArrayInputStream(in.getBytes("UTF-8"))) {
			return readXML(is, xstream);
		}
	}
	
	//The interface for type safe xml reading and writing
	
	public interface XStreamInOutInterface {
		public void writeXML(String filePath, Path path) throws Exception;
		public XStreamInOutInterface readXML(String filePath, Path path) throws Exception;
		public AbstractXStreamSetup getXStreamSetup();
		
		public static abstract class AbstractXStreamSetup {
			
			public abstract void getOutputGraph(LinkedHashSet<AbstractXStreamSetup> in);
			public abstract Set<Class<?>> getAnnotatedClasses();
			public abstract void setXStreamOptions(XStream xstream);
			
			public XStream getXStream(){
				XStream xstream = getXStreamObject();
				List<AbstractXStreamSetup> outputGraph = getOutputGraph();
				setAllAnnotatedClasses(xstream, outputGraph);
				setAllXStreamOptions(xstream, outputGraph);
				return xstream;
			}
			
			protected List<AbstractXStreamSetup> getOutputGraph() {
				LinkedHashSet<AbstractXStreamSetup> ret = new LinkedHashSet<>();
				getOutputGraph(ret);
				return new ArrayList<>(ret);
			}
			
			protected void setAllAnnotatedClasses(XStream xstream, List<AbstractXStreamSetup> outputGraph) {
				Set<Class<?>> annotatedClasses = new LinkedHashSet<>();
				for(AbstractXStreamSetup s : outputGraph) {
					annotatedClasses.addAll(s.getAnnotatedClasses());
				}
				xstream.processAnnotations(annotatedClasses.toArray(new Class<?>[0]));
			}
			
			protected void setAllXStreamOptions(XStream xstream, List<AbstractXStreamSetup> outputGraph) {
				ListIterator<AbstractXStreamSetup> it = outputGraph.listIterator(outputGraph.size());
				while(it.hasPrevious()) {
					it.previous().setXStreamOptions(xstream);
				}
			}
			
			protected XStream getXStreamObject() {
				return new XStream(new SunUnsafeReflectionProvider(new FieldDictionary(new FieldKeySorter() {
					//Order fields from the declaring class to the base class and in each class they are in declaration order
					@SuppressWarnings({ "rawtypes", "unchecked" })
					@Override
					public Map<FieldKey, Field> sort(final Class type, final Map keyedByFieldKey) {
						return SortingMethods.sortMapKey(keyedByFieldKey, new Comparator<FieldKey>() {
							public int compare(final FieldKey fieldKey1, final FieldKey fieldKey2) {
								int i = fieldKey2.getDepth() - fieldKey1.getDepth();
								if (i == 0) {
									i = fieldKey1.getOrder() - fieldKey2.getOrder();
								}
								return i;
							}
						});
					}
				})));
			}
			
			protected void defaultOptionsNoRef(XStream xstream) {
				xstream.setMode(XStream.NO_REFERENCES);
			}
			
			protected void defaultOptionsIdRef(XStream xstream) {
				xstream.setMode(XStream.ID_REFERENCES);
			}
			
			protected void defaultOptionsXPathRelRef(XStream xstream) {
				xstream.setMode(XStream.XPATH_RELATIVE_REFERENCES);
			}
			
		}
	}
	
}
