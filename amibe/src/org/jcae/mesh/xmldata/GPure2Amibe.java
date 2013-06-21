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
 * (C) Copyright 2010, by EADS France
 */
package org.jcae.mesh.xmldata;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.xpath.XPathExpressionException;
import org.jcae.mesh.xmldata.AmibeWriter.Dim3;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/** 
 * Convert a GPure mesh to an Amibe mesh.
 */
public class GPure2Amibe extends XMLReader {
	private final String outputDir;
	private Dim3 out;
	private int nodeCounter;
	private final Set<String> groupNames = new HashSet<String>();

	public GPure2Amibe(String outputDir) {
		this.outputDir = outputDir;
	}

	@Override
	protected void read(Document dom) throws SAXException,
		XPathExpressionException,
		IOException {
		out = new AmibeWriter.Dim3(outputDir);
		out.setFixNoGroup(true);
		Element triaElement = null;
		ArrayList<Element> edgeElements = new ArrayList<Element>();
		ArrayList<Element> partitionElements = new ArrayList<Element>();
		for(Element e:getElements(dom.getDocumentElement(), "rdf:Description"))
		{
			Element typeElement = getElement(e, "GPure:type");
			String type = typeElement == null ? null : typeElement.getTextContent().trim();
			if("Data".equals(type))
				partitionElements.add(e);
			else if("Tesselation".equals(type))
			{
				if(getElement(e, "GPure:positions") == null)
					continue;
				if(getElement(e, "GPure:faces") == null)
					edgeElements.add(e);
				else
				{
					if(triaElement != null)
						throw new IllegalStateException("Cannot convert a file"+
							"containing more than one triangulated part");
					triaElement = e;
				}
			}
		}		
		if(triaElement != null)
		{
			Element positions = getElement(triaElement, "GPure:positions");
			addNodes(positions.getTextContent());
			//free some memory
			positions.setTextContent("");
			Element faces = getElement(triaElement, "GPure:faces");
			addTriangles(faces.getTextContent());
			//free some memory
			GPure:faces.setTextContent("");
		}
		writeGroups(partitionElements);
		int beamOffset = 0;
		for(Element e:edgeElements)
		{
			int nodeOffset = nodeCounter;
			Element positions = getElement(e, "GPure:positions");
			addNodes(positions.getTextContent());
			Element edges = getElement(e, "GPure:edges");
			beamOffset += addBeams(edges.getTextContent(), nodeOffset, beamOffset);
		}
		out.finish();
	}

	private void writeGroups(List<Element> partitionElements) throws IOException
	{
		// sort group alphabetically
		TreeMap<String, Element> sortedPartitions = new TreeMap<String, Element>();
		for(Element e:partitionElements)
		{
			Element dataGroup = getElement(e, "GPure:dataGroup");
			if(dataGroup != null && "partitionDataSet".equals(dataGroup.getTextContent()))
			{
				String name = getFreeName(getElement(e, "GPure:name").getTextContent());
				sortedPartitions.put(name, e);
			}
		}
		for(Entry<String, Element> e:sortedPartitions.entrySet())
		{
			out.nextGroup(e.getKey());
			addToGroup(getElement(e.getValue(), "GPure:faces").getTextContent());
		}
	}

	private String getFreeName(String root)
	{
		String toReturn;
		if(groupNames.contains(root))
		{
			int i = 1;
			String name;
			do
			{
				name = root + i;
				i++;
			}
			while(groupNames.contains(name));
			toReturn = name;
		}
		else
			toReturn = root;
		groupNames.add(toReturn);
		return toReturn;
	}

	@Override
	protected String getXSD() {
		return null;
	}

	public static void main(final String[] args) {
		try {
			new GPure2Amibe("/tmp/testamibe").read(new File("/tmp/zebra7116.tmp/zebra-out.gpure"));
		} catch (SAXException ex) {
			Logger.getLogger(GPure2Amibe.class.getName()).log(Level.SEVERE, null,
				ex);
		} catch (IOException ex) {
			Logger.getLogger(GPure2Amibe.class.getName()).log(Level.SEVERE, null,
				ex);
		}
	}

	private void addNodes(String textContent) throws IOException {
		StringTokenizer cordinatesAll = new StringTokenizer(textContent, ", \n");
		while (cordinatesAll.hasMoreTokens()) {
			out.addNode(Double.parseDouble(cordinatesAll.nextToken()),
				Double.parseDouble(cordinatesAll.nextToken()),
				Double.parseDouble(cordinatesAll.nextToken()));
			nodeCounter ++;
		}
	}

	private void addTriangles(String textContent) throws IOException {		
		StringTokenizer cordinatesAll = new StringTokenizer(textContent, " \n");
		while (cordinatesAll.hasMoreTokens()) {
			out.addTriangle(Integer.parseInt(cordinatesAll.nextToken()),
				Integer.parseInt(cordinatesAll.nextToken()),
				Integer.parseInt(cordinatesAll.nextToken()));
		}
	}

	private void addToGroup(String textContent) throws IOException {
		StringTokenizer cordinatesAll = new StringTokenizer(textContent, " \n");
		while (cordinatesAll.hasMoreTokens()) {
			out.addTriaToGroup(Integer.parseInt(cordinatesAll.nextToken()));
		}
	}

	private int addBeams(String textContent, int nodeOffset, int beamOffset) throws IOException {
		StringTokenizer cordinatesAll = new StringTokenizer(textContent, " \n");
		int id = 0;
		out.nextGroup(""+beamOffset);
		while (cordinatesAll.hasMoreTokens()) {
			out.addBeam(
				nodeOffset + Integer.parseInt(cordinatesAll.nextToken()),
				nodeOffset + Integer.parseInt(cordinatesAll.nextToken()));
			out.addBeamToGroup(beamOffset+id);
			id ++;
		}
		return id;
	}
}
