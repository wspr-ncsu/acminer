package com.benandow.policyminer.controlpredicatefilter.utils;

import java.io.File;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class GenFilterXmlOutput {

	//TODO write the actual XML so it escapes correctly...
	
	public static FilterRule genDefaultRegexRule() {
		FilterRule rule = new FilterRule(FilterRule.AND);
		//Gen default...
		//(or (starts-with-package android.) (starts-with-package com.android.))
		FilterRule def = new FilterRule(FilterRule.OR);
		def.addChild(new FilterRule(FilterRule.START_PKG_RULE, "android."));
		def.addChild(new FilterRule(FilterRule.START_PKG_RULE, "com.android."));
		rule.addChild(def);
		return rule;
	}
	
	
	public static void generateXml(File outputXmlName, List<FilterRule> rules) {
		//Open XML Writer
		//<?xml version="1.0" encoding="UTF-8" standalone="no"?>

		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();	
			// root elements
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("ContextQueriesDescriptorDatabase");
			doc.appendChild(rootElement);
			
			// staff elements
			Element orEl = doc.createElement("Or");
			rootElement.appendChild(orEl);
			
			for (FilterRule r : rules) {
				Element kmi = doc.createElement("KeepMethodIs");
				StringBuffer text = new StringBuffer();
				writeRule(text, r);
				kmi.setAttribute("Value", text.toString());
				orEl.appendChild(kmi);
			}
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(outputXmlName);
			
			transformer.transform(source, result);

		} catch (ParserConfigurationException|TransformerException e) {
			
		}
	}
	
	private static void writeRule(StringBuffer buf, FilterRule rules) {
		if (rules.relation != null) {
			buf.append("(").append(rules.relation).append(" ");
			for (int i = 0; i < rules.children.size(); i++) {
				FilterRule r = rules.children.get(i);
				writeRule(buf, r);
				if (i < rules.children.size() - 1 ) {
					buf.append(" ");
				}
			}
			buf.append(")");
		} else {
			buf.append("(").append(rules.ruleType).append(" ");
			if (rules.ruleType.equals(FilterRule.REGEX_RULE) || rules.ruleType.equals(FilterRule.SIG_RULE)) {
				buf.append("`");
				buf.append(rules.rule);
				buf.append("`");
			} else {
				buf.append(rules.rule);
			}
			buf.append(")");
		}
	}
	
	
}
