/*
 * Project Info:  http://jcae.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * (C) Copyright 2009, by EADS France
 */

package org.jcae.mesh.xmldata;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;


/**
 * Write data as XML using Stax API and valide using a XSD
 * @author Jerome Robert
 */
public class XMLWriter {

	private final static Logger LOGGER = Logger.getLogger(XMLWriter.class.getName());
	public final XMLStreamWriter out;
	private Validator validator;
	private Document document;
	private StreamResult streamResult;
	public XMLWriter(String filename, URL validator) throws IOException
	{
		//Use a writer else indentation do not work
		//http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6337981
		this(new StreamResult(new FileWriter(filename)), validator);
	}

	public XMLWriter(OutputStream out, URL validator) throws IOException
	{
		//Use a writer else indentation do not work
		//http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6337981
		this(new StreamResult(new OutputStreamWriter(out)), validator);
	}

	private XMLWriter(StreamResult streamResult, URL validator)
	{
		XMLStreamWriter o = null;
		try {
			this.streamResult = streamResult;
			document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			DOMResult result = new DOMResult(document);
			o = XMLOutputFactory.newInstance().createXMLStreamWriter(result);
			o.writeStartDocument();

			Schema xsd = SchemaFactory.newInstance(
				XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(validator);
			this.validator = xsd.newValidator();
		} catch (SAXException ex) {
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
		} catch (ParserConfigurationException ex) {
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
		} catch (XMLStreamException ex) {
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
		}
		out = o;
	}

	public void close() throws SAXException, IOException
	{
		try {
			out.flush();
			DOMSource source = new DOMSource(document);
			TransformerFactory tf = TransformerFactory.newInstance();
			tf.setAttribute("indent-number", 2);
			Transformer t = tf.newTransformer();
			t.setOutputProperty(OutputKeys.INDENT, "yes");
			t.transform(source, streamResult);
			streamResult.getWriter().close();
			//We validate after writting the file to be able to debug it.
			validator.validate(source);
		} catch (TransformerException ex) {
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
		} catch (XMLStreamException ex) {
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
		}
	}
}
