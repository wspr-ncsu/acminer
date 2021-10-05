package org.sag.common.graphtools;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.sag.common.concurrent.CountingThreadExecutor;
import org.sag.common.io.FileHelpers;
import org.sag.common.io.PrettyPrintXMLStreamWriter;
import org.sag.common.logging.LoggableRuntimeException;

public final class GraphmlGenerator {
	private static final String cn = GraphmlGenerator.class.getSimpleName();
	private final List<Throwable> errs;
	private volatile CountingThreadExecutor exe;
	
	public GraphmlGenerator() {
		this(new CountingThreadExecutor());
	}
	
	public GraphmlGenerator(CountingThreadExecutor exe) {
		this.exe = exe;
		this.errs = new ArrayList<>();
	}
	
	/** This closes the thread executor, waiting for all currently submitted threads to run and exit
	 * before disabling new thread submissions, recording all errors in the current thread executor
	 * to the list in this class, and then setting the executor to null.
	 */
	public boolean close() {
		if(exe != null) {
			boolean ret = exe.shutdownWhenFinished();
			if(errs != null) 
				errs.addAll(exe.getAndClearExceptions());
			exe = null;
			return ret;
		}
		return true;
	}
	
	/** Clears out the exception lists of this class and the CountingThreadExecutor. This
	 * is useful if we want to guarantee that the exceptions thrown cover a specific list
	 * of tasks.
	 */
	public void clearExceptions() {
		if(exe != null) 
			exe.clearExceptions();
		if(errs != null)
			errs.clear();
	}
	
	/** Returns a list containing the exceptions currently in the exceptions list of this
	 * class and the CountingThreadExecutor. What these exceptions cover depends on the tasks
	 * currently in the queue and how they are being waited on. Note all exceptions are cleared
	 * before the list is returned.
	 */
	public List<Throwable> getExceptions() {
		ArrayList<Throwable> ret = new ArrayList<Throwable>();
		if(errs != null) {
			ret.addAll(errs);
			errs.clear();
		}
		if(exe != null) {
			ret.addAll(exe.getAndClearExceptions());
		}
		return ret;
	}
	
	/** This will wait for any currently running tasks to complete before
	 * returning.
	 */
	public void awaitCompletion() {
		if(exe != null)
			try {
				exe.awaitCompletion();
			} catch (Throwable e) {
				if(errs != null)
					errs.add(e);
			}
	}
	
	/** This method submits graphs to be output to files and then waits on the files to be written. 
	 * Since this is a blocking method, we can accurately capture the errors thrown during the 
	 * output process. As such, the errs list is first cleared before any other operations, are performed.
	 * Then if any exception occurs, an joint exception is thrown containing all the exceptions. After this,
	 * the exceptions are again cleared.
	 * 
	 * Because of this procedure, calls to output a graph(s) and block should not be mixed with calls to 
	 * output a graph without blocking. If such a mix is required, then one must wait for the graphs being
	 * output without blocking to complete before outputting graphs with blocking.
	 */
	public void outputGraphsAndBlock(List<Formatter> formatters) {
		clearExceptions();
		try {
			outputGraphs(formatters);
		} catch(Throwable t) {
			if(errs != null)
				errs.add(t);
		} finally {
			awaitCompletion();
		}
		
		List<Throwable> exceptions = getExceptions();
		if(!exceptions.isEmpty())
			throw new LoggableRuntimeException(CountingThreadExecutor.computeJointErrorMsg(exceptions, 
					"Failed to output all graphs and block.", cn));
	}
	
	/** This method submits a graph to be output to file and then waits on the file to be written. 
	 * Since this is a blocking method, we can accurately capture the errors thrown during the 
	 * output process. As such, the errs list is first cleared before any other operations, are performed.
	 * Then if any exception occurs, an joint exception is thrown containing all the exceptions. After this,
	 * the exceptions are again cleared.
	 * 
	 * Because of this procedure, calls to output a graph(s) and block should not be mixed with calls to 
	 * output a graph without blocking. If such a mix is required, then one must wait for the graphs being
	 * output without blocking to complete before outputting graphs with blocking.
	 */
	public void outputGraphAndBlock(Formatter f) {
		clearExceptions();
		try {
			outputGraph(f);
		} catch(Throwable t) {
			if(errs != null)
				errs.add(t);
		} finally {
			awaitCompletion();
		}
		
		List<Throwable> exceptions = getExceptions();
		if(!exceptions.isEmpty())
			throw new LoggableRuntimeException(CountingThreadExecutor.computeJointErrorMsg(exceptions, 
					"Failed to output the graph to path '" + f.getOutputPath() + "' and block.", cn));
	}
	
	/** This method outputs graphs without blocking and waiting for the files to be written. As such,
	 * all exceptions thrown by this method only reflect the submission of the tasks to output the graphs.
	 * They do not cover the exceptions thrown during the file writing. To get these exceptions call
	 * {@link GraphmlGenerator#getExceptions()} after calling {@link GraphmlGenerator#awaitCompletion()}.
	 * Note if there is more than one task in the ExecutatorService, the exceptions returned after wait
	 * for all tasks to complete may reflect multiple tasks and just just the one created after calling
	 * this method. However, there is no easy way around this unless one wants to block immediately after
	 * submitting the task to output the graph (in which case one should be using 
	 * {@link GraphmlGenerator#outputGraphsAndBlock(List, List)}.
	 */
	public void outputGraphs(List<Formatter> formatters) {
		Objects.requireNonNull(formatters);
		List<Throwable> errs = new ArrayList<>();
		for(Formatter f : formatters) {
			try {
				outputGraph(f);
			} catch(Throwable t) {
				errs.add(t);
			}
		}
		if(!errs.isEmpty())
			throw new LoggableRuntimeException(CountingThreadExecutor.computeJointErrorMsg(errs, 
					"Failed to output all graphs.", cn));
	}
	
	/** This method outputs a graph without blocking and waiting for the file to be written. As such,
	 * all exceptions thrown by this method only reflect the submission of a task to output the graph.
	 * They do not cover the exceptions thrown during the file writing. To get these exceptions call
	 * {@link GraphmlGenerator#getExceptions()} after calling {@link GraphmlGenerator#awaitCompletion()}.
	 * Note if there is more than one task in the ExecutatorService, the exceptions returned after wait
	 * for all tasks to complete may reflect multiple tasks and just just the one created after calling
	 * this method. However, there is no easy way around this unless one wants to block immediately after
	 * submitting the task to output the graph (in which case one should be using 
	 * {@link GraphmlGenerator#outputGraphAndBlock(Formatter, Path)}.
	 */
	public void outputGraph(final Formatter f) {
		Objects.requireNonNull(f);
		if(exe != null)
			try {
				exe.execute(new Runnable(){
					public void run() {
						outputGraphStatic(f);
					}
				});
			} catch(Throwable t) {
				throw new LoggableRuntimeException("{}: Failed to output the graph at path '{}'.",t,cn,f.getOutputPath());
			}
	}
	
	/** This outputs the given graphs to files the slow synchronous way.
	 */
	public static void outputGraphsStatic(List<Formatter> formatters) {
		Objects.requireNonNull(formatters);
		List<Throwable> errs = new ArrayList<>();
		for(Formatter f : formatters){
			try {
				outputGraphStatic(f);
			} catch(Throwable t) {
				errs.add(t);
			}
		}
		if(!errs.isEmpty())
			throw new LoggableRuntimeException(CountingThreadExecutor.computeJointErrorMsg(errs, 
					"Failed to output all graphs.", cn));
	}
	
	private static final String NODEGID = "d1";
	private static final String EDGEGID = "d2";
	
	/** This outputs a given graph to a file and is the code used to do so by the rest of the methods
	 * in this class. However, this method is static and thus does not use the thread executor. This
	 * is useful if one wishes to use his/her own thread executor or no thread executor.
	 */
	public static void outputGraphStatic(Formatter f) {
		Objects.requireNonNull(f);
		Path out = f.getOutputPath();
		OutputStream outStream = null;
		try {
			out = checkFilePath(out);
			outStream = Files.newOutputStream(out);
			outputGraphStatic(f, outStream);
		} catch (Throwable t) {
			throw new LoggableRuntimeException("{}: Failed to output the graph to path '{}'.",t,cn,out);
		} finally {
			if(outStream != null) {
				try {
					outStream.close();
					outStream = null;
				} catch(Throwable e1) {}
			}
		}
	}
	
	//Split to make it easier to test changes to output format
	private static void outputGraphStatic(Formatter f, OutputStream outStream) throws Exception {
		Collection<AlNode> nodes = f.getNodes();
		Collection<AlEdge> edges = f.getEdges();
		XMLStreamWriter writer = null;
		try{
			writer = XMLOutputFactory.newFactory().createXMLStreamWriter(outStream, StandardCharsets.UTF_8.name());
			writer = new PrettyPrintXMLStreamWriter(writer);
			writer.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");
			writer.writeComment(f.getComment());
			
			//write the root and data imports
			writer.writeStartElement("graphml");
			writer.writeAttribute("xmlns","http://graphml.graphdrawing.org/xmlns");
			writer.writeAttribute("xmlns:java","http://www.yworks.com/xml/yfiles-common/1.0/java");
			writer.writeAttribute("xmlns:sys","http://www.yworks.com/xml/yfiles-common/markup/primitives/2.0");
			writer.writeAttribute("xmlns:x","http://www.yworks.com/xml/yfiles-common/markup/2.0");
			writer.writeAttribute("xmlns:xsi","http://www.w3.org/2001/XMLSchema-instance");
			writer.writeAttribute("xmlns:y","http://www.yworks.com/xml/graphml");
			writer.writeAttribute("xmlns:yed","http://www.yworks.com/xml/yed/3");
			writer.writeAttribute("xsi:schemaLocation","http://graphml.graphdrawing.org/xmlns http://www.yworks.com/xml/schema/graphml/1.1/ygraphml.xsd");
			
			//write the key information used by all nodes and edges
			writer.writeEmptyElement("key");
			writer.writeAttribute("for", "node");
			writer.writeAttribute("id", NODEGID);
			writer.writeAttribute("yfiles.type", "nodegraphics");
			writer.writeEmptyElement("key");
			writer.writeAttribute("for", "edge");
			writer.writeAttribute("id", EDGEGID);
			writer.writeAttribute("yfiles.type", "edgegraphics");
			
			//write the root graph header
			writer.writeStartElement("graph");
			writer.writeAttribute("id", "G");
			writer.writeAttribute("edgedefault", "directed");
			
			
			writeNodes(writer, nodes, f.getNodeColorIndex(), f.getNodeShapeIndex(), f.getNodeExtraDataIndex());
			writeEdges(writer, edges, f.getEdgeColorIndex());
			
			writer.writeEndElement();
			writer.writeEndElement();
			writer.writeEndDocument();
			writer.flush();
		} finally {
			if(writer != null) {
				try {
					writer.close();
					writer = null;
				} catch(Throwable t) {}
			}
		}
	}
	
	private static void writeEdges(XMLStreamWriter writer, Collection<AlEdge> edges, long edgeColorIndex) throws XMLStreamException{
		for(AlEdge edge : edges){
			writer.writeStartElement("edge");
			writer.writeAttribute("id", edge.getId());
			writer.writeAttribute("source", edge.getSource().getId());
			writer.writeAttribute("target", edge.getTarget().getId());
			
			writer.writeStartElement("data");
			writer.writeAttribute("key", EDGEGID);
			
			writer.writeStartElement("y:PolyLineEdge");
			
			writer.writeEmptyElement("y:LineStyle");
			writer.writeAttribute("color", edge.getColor(edgeColorIndex));
			writer.writeAttribute("type", "line");
			writer.writeAttribute("width", edge.getWeight() + "");
			
			writer.writeEmptyElement("y:Arrows");
			writer.writeAttribute("source", "none");
			writer.writeAttribute("target", "standard");
			
			writer.writeStartElement("y:EdgeLabel");
			writer.writeCharacters(edge.getLabel());
			writer.writeEndElement();
			
			writer.writeEmptyElement("y:BendStyle");
			writer.writeAttribute("smoothed", "false");
			
			writer.writeEndElement();
			writer.writeEndElement();
			writer.writeEndElement();
		}
	}
	
	private static void writeNodes(XMLStreamWriter writer, Collection<AlNode> nodes, long nodeColorIndex, 
			long nodeShapeIndex, long nodeExtraDataIndex) throws XMLStreamException {
		for(AlNode node : nodes) {
			writer.writeStartElement("node");
			writer.writeAttribute("id", node.getId());
			writer.writeAttribute("yfiles.foldertype", "group");
			
			writer.writeStartElement("data");
			writer.writeAttribute("key", NODEGID);
			
			writer.writeStartElement("y:ProxyAutoBoundsNode");
			
			writer.writeStartElement("y:Realizers");
			writer.writeAttribute("active", "0");
			
			writeGroupNode(writer, node, nodeColorIndex, nodeShapeIndex, nodeExtraDataIndex, true);
			writeGroupNode(writer, node, nodeColorIndex, nodeShapeIndex, nodeExtraDataIndex, false);
			
			writer.writeEndElement();
			writer.writeEndElement();
			writer.writeEndElement();
			
			if(node.hasExtraData(nodeExtraDataIndex)) {
				writer.writeStartElement("graph");
				writer.writeAttribute("edgedefault", "directed");
				writer.writeAttribute("id", node.getId() + ":");
				
				writer.writeStartElement("node");
				writer.writeAttribute("id", node.getId() + ":" + "n0");
				
				writer.writeStartElement("data");
				writer.writeAttribute("key", NODEGID);
				
				writer.writeStartElement("y:ShapeNode");
				
				writer.writeEmptyElement("y:Geometry");
				writer.writeAttribute("height", "30.0");
				writer.writeAttribute("width", "30.0");
				
				writer.writeEmptyElement("y:Fill");
				writer.writeAttribute("hasColor", "false");
				writer.writeAttribute("transparent", "false");
				
				writer.writeEmptyElement("y:BorderStyle");
				writer.writeAttribute("hasColor", "false");
				writer.writeAttribute("type", "line");
				writer.writeAttribute("width", "2");
				
				writer.writeStartElement("y:NodeLabel");
				writer.writeAttribute("alignment", "center");
				writer.writeAttribute("autoSizePolicy", "content");
				writer.writeAttribute("fontFamily", "Dialog");
				writer.writeAttribute("fontSize", "12");
				writer.writeAttribute("fontStyle", "plain");
				writer.writeAttribute("hasBackgroundColor", "false");
				writer.writeAttribute("hasLineColor", "false");
				writer.writeAttribute("horizontalTextPosition", "center");
				writer.writeAttribute("iconTextGap", "4");
				writer.writeAttribute("modelName", "internal");
				writer.writeAttribute("modelPosition", "c");
				writer.writeAttribute("textColor", "#000000");
				writer.writeAttribute("verticalTextPosition", "bottom");
				writer.writeAttribute("visible", "true");
				writer.writeCharacters(node.getExtraData(nodeExtraDataIndex));
				writer.writeEndElement();
				
				writer.writeEmptyElement("y:Shape");
				writer.writeAttribute("type", node.getShape(nodeShapeIndex));
				
				writer.writeEndElement();
				writer.writeEndElement();
				writer.writeEndElement();
				writer.writeEndElement();
			}
			
			writer.writeEndElement();
			
		}
	}
	
	private static void writeGroupNode(XMLStreamWriter writer, AlNode node, long nodeColorIndex, 
			long nodeShapeIndex, long nodeExtraDataIndex, boolean isOpen) throws XMLStreamException {
		writer.writeStartElement("y:GroupNode");
		
		writer.writeEmptyElement("y:Geometry");
		writer.writeAttribute("height", "30.0");
		writer.writeAttribute("width", "30.0");
		
		writer.writeEmptyElement("y:Fill");
		if(node.hasColor(nodeColorIndex))
			writer.writeAttribute("color", node.getColor(nodeColorIndex));
		if(node.hasColor2(nodeColorIndex))
			writer.writeAttribute("color2", node.getColor2(nodeColorIndex));
		if(!node.hasColor(nodeColorIndex) && !node.hasColor2(nodeColorIndex))
			writer.writeAttribute("hasColor", "false");
		writer.writeAttribute("transparent", "false");
		
		writer.writeEmptyElement("y:BorderStyle");
		writer.writeAttribute("color", "#000000");
		writer.writeAttribute("type", "line");
		writer.writeAttribute("width", "2");
		
		writer.writeStartElement("y:NodeLabel");
		writer.writeAttribute("alignment", "center");
		if(node.hasColor(nodeColorIndex) && node.hasExtraData(nodeExtraDataIndex))
			writer.writeAttribute("backgroundColor", node.getColor(nodeColorIndex));
		else
			writer.writeAttribute("hasBackgroundColor", "false");
		writer.writeAttribute("borderDistance", "0.0");
		writer.writeAttribute("bottomInset", "2");
		writer.writeAttribute("fontFamily", "Dialog");
		writer.writeAttribute("fontSize", "12");
		writer.writeAttribute("fontStyle", "plain");
		writer.writeAttribute("horizontalTextPosition", "center");
		writer.writeAttribute("iconTextGap", "4");
		writer.writeAttribute("leftInset", "22");
		writer.writeAttribute("modelName", "internal");
		if(node.hasExtraData(nodeExtraDataIndex)) {
			writer.writeAttribute("modelPosition", "t");
			writer.writeAttribute("lineColor", "#000000");
			writer.writeAttribute("autoSizePolicy", "node_width");
		} else {
			writer.writeAttribute("modelPosition", "c");
			writer.writeAttribute("hasLineColor", "false");
			writer.writeAttribute("autoSizePolicy", "content");
		}
		writer.writeAttribute("rightInset", "2");
		writer.writeAttribute("textColor", "#000000");
		writer.writeAttribute("topInset", "2");
		writer.writeAttribute("verticalTextPosition", "bottom");
		writer.writeAttribute("visible", "true");
		writer.writeCharacters(node.getLabel());
		writer.writeEndElement();
		
		writer.writeEmptyElement("y:Shape");
		writer.writeAttribute("type", node.getShape(nodeShapeIndex));
		
		writer.writeEmptyElement("y:State");
		if(isOpen)
			writer.writeAttribute("closed", "false");
		else
			writer.writeAttribute("closed", "true");
		writer.writeAttribute("closedHeight", "30.0");
		writer.writeAttribute("closedWidth", "30.0");
		writer.writeAttribute("groupDepthFillColorEnabled", "false");
		writer.writeAttribute("innerGraphDisplayEnabled", "false");
		
		writer.writeEmptyElement("y:NodeBounds");
		writer.writeAttribute("considerNodeLabelSize", "true");
		
		writer.writeEmptyElement("y:Insets");
		writer.writeAttribute("bottom", "10");
		writer.writeAttribute("bottomF", "10.0");
		writer.writeAttribute("left", "10");
		writer.writeAttribute("leftF", "10.0");
		writer.writeAttribute("right", "10");
		writer.writeAttribute("rightF", "10.0");
		writer.writeAttribute("top", "10");
		writer.writeAttribute("topF", "10.0");
		
		writer.writeEmptyElement("y:BorderInsets");
		writer.writeAttribute("bottom", "0");
		writer.writeAttribute("bottomF", "0.0");
		writer.writeAttribute("left", "0");
		writer.writeAttribute("leftF", "0.0");
		writer.writeAttribute("right", "0");
		writer.writeAttribute("rightF", "0.0");
		writer.writeAttribute("top", "0");
		writer.writeAttribute("topF", "0.0");
		
		writer.writeEndElement();
	}
	
	private static Path checkFilePath(Path path) throws Exception {
		Objects.requireNonNull(path);
		String ext = com.google.common.io.Files.getFileExtension(path.toString());
		String fileName = com.google.common.io.Files.getNameWithoutExtension(path.toString());
		Path parentDir = path.getParent();
		
		if(ext.isEmpty() || !ext.equals("graphml"))
			ext = "graphml";
		if(fileName.isEmpty())
			fileName = "out";
		if(parentDir != null) {
			path = FileHelpers.getPath(parentDir, fileName+"."+ext);
		} else {
			path = FileHelpers.getPath(fileName+"."+ext);
		}
		parentDir = path.getParent();
		
		try {
			FileHelpers.processDirectory(parentDir, true, false);
		} catch(Throwable t) {
			throw new Exception("Error: Failed to create path '" + parentDir + "' to output file.",t);
		}
		
		if(Files.exists(path)) {
			if(Files.isDirectory(path)) {
				throw new Exception("Error: The specified path '"+path+"' is a directory.");
			} else if(Files.isRegularFile(path)) {
				throw new Exception("Error: The specified path '"+path+"' is an existing file.");
			} else {
				throw new Exception("Error: The specified path '"+path+"' exists already.");
			}
		}
		
		return path;
	}
	
}
