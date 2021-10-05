package org.sag.common.io;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class PrettyPrintXMLStreamWriter implements XMLStreamWriter {

	private final XMLStreamWriter parent;
	private final PrettyXMLWhitespaceHandler handler;

	/** Create instance using default indentation (\t) and line separator (\n).
	 * 
	 * @param writer - parent writer
	 */
	public PrettyPrintXMLStreamWriter(XMLStreamWriter writer) {
		this(writer, "\t", "\n");
	}

	/** Create instance.
	 * 
	 * @param writer - parent writer
	 * @param indentation - line indentation
	 * @param newline - line separator
	 */
	public PrettyPrintXMLStreamWriter(XMLStreamWriter writer, String indentation, String newline) {
		this.parent = writer;
		this.handler = new PrettyXMLWhitespaceHandler(writer, indentation, newline);
	}
	
	public XMLStreamWriter getParent() {
		return parent;
	}
	
	@Override
	public void close() throws XMLStreamException {
		parent.close();
	}

	@Override
	public void flush() throws XMLStreamException {
		parent.flush();
	}

	@Override
	public NamespaceContext getNamespaceContext() {
		return parent.getNamespaceContext();
	}

	@Override
	public String getPrefix(String uri) throws XMLStreamException {
		return parent.getPrefix(uri);
	}

	@Override
	public Object getProperty(String name) throws IllegalArgumentException {
		return parent.getProperty(name);
	}

	@Override
	public void setDefaultNamespace(String uri) throws XMLStreamException {
		parent.setDefaultNamespace(uri);
	}

	@Override
	public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
		parent.setNamespaceContext(context);
	}

	@Override
	public void setPrefix(String prefix, String uri) throws XMLStreamException {
		parent.setPrefix(prefix, uri);
	}

	@Override
	public void writeAttribute(String prefix, String namespaceURI, String localName, String value) throws XMLStreamException {
		parent.writeAttribute(prefix, namespaceURI, localName, value);
	}

	@Override
	public void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
		parent.writeAttribute(namespaceURI, localName, value);
	}

	@Override
	public void writeAttribute(String localName, String value) throws XMLStreamException {
		parent.writeAttribute(localName, value);
	}

	@Override
	public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
		parent.writeDefaultNamespace(namespaceURI);
	}

	@Override
	public void writeDTD(String dtd) throws XMLStreamException {
		parent.writeDTD(dtd);
	}

	@Override
	public void writeEndDocument() throws XMLStreamException {
		parent.writeEndDocument();
	}

	@Override
	public void writeEntityRef(String name) throws XMLStreamException {
		parent.writeEntityRef(name);
	}

	@Override
	public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
		parent.writeNamespace(prefix, namespaceURI);
	}

	@Override
	public void writeStartDocument() throws XMLStreamException {
		handler.preStartDocument();
		parent.writeStartDocument();
		handler.postStartDocument();
	}

	@Override
	public void writeStartDocument(String version) throws XMLStreamException {
		handler.preStartDocument();
		parent.writeStartDocument(version);
		handler.postStartDocument();
	}

	@Override
	public void writeStartDocument(String encoding, String version) throws XMLStreamException {
		handler.preStartDocument();
		parent.writeStartDocument(encoding, version);
		handler.postStartDocument();
	}
	
	@Override
	public void writeStartElement(String localName) throws XMLStreamException {
		handler.preStartElement();
		parent.writeStartElement(localName);
		handler.postStartElement();
	}

	@Override
	public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
		handler.preStartElement();
		parent.writeStartElement(namespaceURI, localName);
		handler.postStartElement();
	}

	@Override
	public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
		handler.preStartElement();
		parent.writeStartElement(prefix, localName, namespaceURI);
		handler.postStartElement();
	}

	@Override
	public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
		handler.preEmptyELement();
		parent.writeEmptyElement(namespaceURI, localName);
		handler.postEmptyELement();
	}

	@Override
	public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
		handler.preEmptyELement();
		parent.writeEmptyElement(prefix, localName, namespaceURI);
		handler.postEmptyELement();
	}

	@Override
	public void writeEmptyElement(String localName) throws XMLStreamException {
		handler.preEmptyELement();
		parent.writeEmptyElement(localName);
		handler.postEmptyELement();
	}

	@Override
	public void writeEndElement() throws XMLStreamException {
		handler.preEndElement();
		parent.writeEndElement();
		handler.postEndElement();
	}

	@Override
	public void writeCData(String data) throws XMLStreamException {
		handler.preCharacters();
		parent.writeCData(data);
		handler.postCharacters();
	}

	@Override
	public void writeCharacters(String text) throws XMLStreamException {
		handler.preCharacters();
		parent.writeCharacters(text);
		handler.postCharacters();
	}

	@Override
	public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
		handler.preCharacters();
		parent.writeCharacters(text, start, len);
		handler.postCharacters();
	}
	
	@Override
	public void writeComment(String data) throws XMLStreamException {
		handler.preComment();
		parent.writeComment(data);
		handler.postComment();
	}
	
	@Override
	public void writeProcessingInstruction(String target) throws XMLStreamException {
		handler.preProcessingInstruction();
		parent.writeProcessingInstruction(target);
		handler.postProcessingInstruction();
	}
	
	@Override
	public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
		handler.preProcessingInstruction();
		parent.writeProcessingInstruction(target, data);
		handler.postProcessingInstruction();
	}
	
	/** Handler for pretty printing state and insert indentation and newline characters events.
	 */
	private static class PrettyXMLWhitespaceHandler {
		
		private static final int DEPTH_INCREMENT = 64;
		
		private final String newline;
		private final String indentation;
		private final List<String> indent;
		private final XMLStreamWriter writer;

		private int depth;
		private boolean text;
		private boolean leaf;
		private int curMaxDepth;

		/** Create whitespace handler for an {@link XMLStreamWriter}.
		 * 
		 * @param writer - stream writer
		 * @param indentation - line indentation
		 * @param newline - line separator
		 */
		PrettyXMLWhitespaceHandler(XMLStreamWriter writer, String indentation, String newline) {
			this.depth = 0;
			this.text = false;
			this.leaf = false;
			this.curMaxDepth = 0;
			this.newline = newline;
			this.indentation = indentation;
			this.indent = new ArrayList<>(DEPTH_INCREMENT);
			this.writer = writer;
			increaseIndentDepth();
		}
		
		private void increaseIndentDepth() {
			StringBuilder builder;
			int nextMaxDepth;
			
			if(willAdditionOverflow(curMaxDepth,DEPTH_INCREMENT)) {
				nextMaxDepth = Integer.MAX_VALUE;
			} else {
				nextMaxDepth = curMaxDepth + DEPTH_INCREMENT;
			}
			
			if(curMaxDepth != nextMaxDepth) {
				if(curMaxDepth == 0) {
					builder = new StringBuilder();
				} else {
					builder = new StringBuilder(indent.get(curMaxDepth-1)).append(indentation);
				}
				indent.add(builder.toString());
				
				for(int i = curMaxDepth+1; i < nextMaxDepth; i++) {
					builder.append(indentation);
					indent.add(builder.toString());
				}
				curMaxDepth = nextMaxDepth;
			}
		}

		void preStartDocument() throws XMLStreamException { preStructure(); }
		void postStartDocument() throws XMLStreamException { postComment_PI(); }
		void preComment() throws XMLStreamException { preStructure(); }
		void postComment() throws XMLStreamException { postComment_PI(); }
		void preProcessingInstruction() throws XMLStreamException { preStructure(); }
		void postProcessingInstruction() throws XMLStreamException { postComment_PI(); }
		void preStartElement() throws XMLStreamException { preStructure(); }
		void preEmptyELement() throws XMLStreamException { preStructure(); }
		void postEmptyELement() throws XMLStreamException { leaf = false; }
		void preCharacters() { text = true; }
		void postCharacters() {}

		void postStartElement() throws XMLStreamException {
			if(willAdditionOverflow(depth,1))
				throw new XMLStreamException("Error: The indention depth had reached the MAX_INTEGER value. Any increase will "
						+ "cause an integer overflow to occur.");
			depth++;
			if(depth == curMaxDepth) {
				increaseIndentDepth();
			}
			leaf = true;
		}

		void preEndElement() throws XMLStreamException {
			depth--;
			if(depth < 0)
				throw new XMLStreamException("Error: The indention depth has become negative.");
			if (text) {
				text = false;
			} else if (!leaf) {
				writer.writeCharacters(newline);
				if (depth > 0) {
					writer.writeCharacters(indent.get(depth));
				}
			}
		}

		void postEndElement() throws XMLStreamException {
			leaf = false;
			if (depth == 0) {
				writer.writeCharacters(newline);
			}
		}

		private void preStructure() throws XMLStreamException {
			if (text) {
				text = false;
			} else if (depth > 0) {
				writer.writeCharacters(newline);
				writer.writeCharacters(indent.get(depth));
			}
		}

		private void postComment_PI() throws XMLStreamException {
			leaf = false;
			if (depth == 0) {
				writer.writeCharacters(newline);
			}
		}
		
		private static boolean willAdditionOverflow(int left, int right) {
		    if (right < 0 && right != Integer.MIN_VALUE) {
		        return willSubtractionOverflow(left, -right);
		    } else {
		        return (~(left ^ right) & (left ^ (left + right))) < 0;
		    }
		}

		private static boolean willSubtractionOverflow(int left, int right) {
		    if (right < 0) {
		        return willAdditionOverflow(left, -right);
		    } else {
		        return ((left ^ right) & (left ^ (left - right))) < 0;
		    }
		}
		
	}
	
}
