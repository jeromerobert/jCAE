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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 *
 * @author Jerome Robert
 */
public abstract class AmibeReader extends XMLReader implements JCAEXMLData {
	
	public class Group
	{
		private String name;
		private int numberOfNodes, numberOfTrias, numberOfBeams;
		private int nodesOffset, beamsOffset, triasOffset;

		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @return the numberOfNodes
		 */
		public int getNumberOfNodes() {
			return numberOfNodes;
		}

		/**
		 * @return the numberOfTrias
		 */
		public int getNumberOfTrias() {
			return numberOfTrias;
		}

		/**
		 * @return the numberOfBeams
		 */
		public int getNumberOfBeams() {
			return numberOfBeams;
		}

		/**
		 * @return the nodesOffset
		 */
		public long getNodesOffset() {
			return nodesOffset;
		}

		/**
		 * @return the beamsOffset
		 */
		public long getBeamsOffset() {
			return beamsOffset;
		}

		/**
		 * @return the triasOffset
		 */
		public long getTriasOffset() {
			return triasOffset;
		}

		public int[] readTria3Ids() throws IOException
		{
			if (numberOfTrias == 0)
				return new int[0];
			PrimitiveFileReaderFactory pfrf = new PrimitiveFileReaderFactory();
			IntFileReader ifrG = pfrf.getIntReader(getBinFile(groupsFilename));
			int[] toReturn = new int[numberOfTrias];
			for (int i = 0; i < numberOfTrias; i++)
				toReturn[i] = ifrG.get(triasOffset+i);
			ifrG.close();
			return toReturn;
		}
		
		public int[] readTria3() throws IOException
		{
			if (numberOfTrias == 0)
				return new int[0];
			PrimitiveFileReaderFactory pfrf = new PrimitiveFileReaderFactory();
			int[] ids = readTria3Ids();
			IntFileReader ifrT = pfrf.getIntReader(getBinFile("triangles"+dim()+"d.bin"));
			int[] toReturn = new int[numberOfTrias * 3];
			for (int i = 0; i < numberOfTrias; i++)
				ifrT.get(ids[i] * 3, toReturn, i * 3, 3);
			ifrT.close();
			return toReturn;
		}

		public int[] readBeams() throws IOException
		{
			if (numberOfBeams == 0)
				return new int[0];
			PrimitiveFileReaderFactory pfrf = new PrimitiveFileReaderFactory();
			int[] ids = readBeamsIds();
			IntFileReader ifrT = pfrf.getIntReader(getBinFile("beams"+dim()+"d.bin"));
			int[] toReturn = new int[numberOfBeams * 2];
			for (int i = 0; i < numberOfBeams; i++)
				ifrT.get(ids[i] * 2, toReturn, i * 2, 2);
			ifrT.close();
			return toReturn;
		}

		public int[] readBeamsIds() throws IOException {
			if (numberOfBeams == 0)
				return new int[0];
			PrimitiveFileReaderFactory pfrf = new PrimitiveFileReaderFactory();
			IntFileReader ifrG = pfrf.getIntReader(getBinFile("bgroups.bin"));
			int[] toReturn = new int[numberOfBeams];
			for (int i = 0; i < numberOfBeams; i++)
				toReturn[i] = ifrG.get(beamsOffset+i);
			ifrG.close();
			return toReturn;
		}

		public int[] readNodesIds() throws IOException {
			if (numberOfNodes == 0)
				return new int[0];
			PrimitiveFileReaderFactory pfrf = new PrimitiveFileReaderFactory();
			IntFileReader ifrG = pfrf.getIntReader(getBinFile("nodeGroups.bin"));
			int[] toReturn = new int[numberOfNodes];
			for (int i = 0; i < numberOfNodes; i++)
				toReturn[i] = ifrG.get(nodesOffset+i);
			ifrG.close();
			return toReturn;
		}
	}

	public class SubMesh
	{
		private int subShape;
		private int numberOfNodes, numberOfTrias, numberOfBeams, numberOfReferences;
		private int nodesOffset, beamsOffset, triasOffset;
		private LinkedHashMap<String, Group> groups = new LinkedHashMap<String, Group>();

		/**
		 * @return the subShape
		 */
		public int getSubShape() {
			return subShape;
		}

		/**
		 * @return the numberOfNodes
		 */
		public int getNumberOfNodes() {
			return numberOfNodes;
		}

		/**
		 * @return the numberOfTrias
		 */
		public int getNumberOfTrias() {
			return numberOfTrias;
		}

		/**
		 * @return the numberOfBeams
		 */
		public int getNumberOfBeams() {
			return numberOfBeams;
		}

		/**
		 * @return the nodesOffset
		 */
		public long getNodesOffset() {
			return nodesOffset;
		}

		/**
		 * @return the beamsOffset
		 */
		public long getBeamsOffset() {
			return beamsOffset;
		}

		/**
		 * @return the triasOffset
		 */
		public long getTriasOffset() {
			return triasOffset;
		}

		/**
		 * @return the groups
		 */
		public List<Group> getGroups() {
			return new ArrayList<Group>(groups.values());
		}

		public Group getGroup(String id)
		{
			return groups.get(id);
		}

		public DoubleFileReader getNodes() throws IOException
		{
			File f = getBinFile("nodes"+dim()+"d.bin");
			return new PrimitiveFileReaderFactory().getDoubleReader(f);
		}

		public DoubleFileReader getNormals() throws IOException
		{
			File f = getBinFile("normals"+dim()+"d.bin");
			return new PrimitiveFileReaderFactory().getDoubleReader(f);
		}

		public IntFileReader getBeams() throws IOException
		{
			return new PrimitiveFileReaderFactory().getIntReader(
				getBinFile("beams"+dim()+"d.bin"));
		}
		
		public IntFileReader getTriangles() throws IOException
		{
			return new PrimitiveFileReaderFactory().getIntReader(
				getBinFile("triangles"+dim()+"d.bin"));
		}
		
		public float[] readNodes(int[] nodesID) throws IOException
		{
			DoubleFileReader dfr = getNodes();
			float[] toReturn = new float[nodesID.length * dim()];

			for (int i = 0; i < nodesID.length; i++)
			{
				int ii = i * dim();
				int iid = nodesID[i] * dim() + nodesOffset * dim();
				for(int j = 0; j < dim(); j++)
					toReturn[ii+j] = (float) dfr.get(iid+j);
			}
			dfr.close();

			return toReturn;
		}

		public int[] getReferences() throws IOException
		{
			int[] refs = new int[numberOfReferences];
			if(numberOfReferences > 0)
			{
				IntFileReader ifrR = new PrimitiveFileReaderFactory().getIntReader(
					getBinFile("nodes1dref.bin"));
				ifrR.get(refs);
				ifrR.close();
			}
			return refs;
		}
		
		public int getNumberOfReferences() {
			return numberOfReferences;
		}
	}

	public static class Dim1 extends AmibeReader {

		public Dim1(String path) throws SAXException, IOException {
			super(path);
		}

		@Override
		protected int dim() {
			return 1;
		}		
	}

	public static class Dim2 extends AmibeReader {
		private final String binDirectory, xmlFile;
		public Dim2(String name, int index) throws SAXException, IOException {
			super(name);
			binDirectory = "jcae2d."+ index +".files";
			xmlFile = "jcae2d."+index;
		}

		@Override
		protected int dim() {
			return 2;
		}

		@Override
		protected String binDirectory() {
			return binDirectory;
		}

		@Override
		protected String xmlFile() {
			return xmlFile;
		}
	}

	public static class Dim3 extends AmibeReader {

		public Dim3(String path) throws SAXException, IOException {
			super(path);
		}

		@Override
		protected int dim() {
			return 3;
		}
	}
	
	private static class AFile
	{
		public String location;
		public int offset;
		public String format;
	}

	private static AFile readFile(Element e)
	{
		Element f = getElement(e, "file");
		AFile r = new AFile();
		if(f != null)
		{			
			r.location = f.getAttribute("location");
			String s = f.getAttribute("offset");
			r.offset = s.isEmpty() ? 0 : Integer.parseInt(s);
			r.format = f.getAttribute("format");
		}
		return r;
	}
	private static int readInt(Element e, String tag)
	{
		Element t = getElement(e, tag);
		if(t != null)
			return Integer.parseInt(t.getTextContent());
		else
			return 0;
	}

	private String shape;
	private List<SubMesh> submeshes = new ArrayList<SubMesh>();
	private final String path;
	@Override
	protected void read(Document dom) {
		Element mesh = getElement(dom.getDocumentElement(), "mesh");
		assert mesh != null;
		shape = readFile(getElement(mesh, "shape")).location;
		for(Element e:getElements(mesh, "submesh"))
		{
			SubMesh sm = new SubMesh();
			sm.subShape = readInt(e, "subshape");
			
			Element nodes = getElement(e, "nodes");
			sm.numberOfNodes = readInt(nodes, "number");
			sm.nodesOffset = readFile(nodes).offset;
			Element references = getElement(nodes, "references");
			if(references != null)
				sm.numberOfReferences = readInt(references, "number");

			Element triangles = getElement(e, "triangles");
			if(triangles != null)
			{
				sm.numberOfTrias = readInt(triangles, "number");
				sm.triasOffset = readFile(triangles).offset;
			}

			Element beams = getElement(e, "beams");
			if(beams != null)
			{
				sm.numberOfBeams = readInt(beams, "number");
				sm.beamsOffset = readFile(beams).offset;
			}

			for(Element eg:getElements(e, "groups", "group"))
			{
				Group g = new Group();
				g.name = getElement(eg, "name").getTextContent();
				g.numberOfTrias = readInt(eg, "number");
				g.triasOffset = readFile(eg).offset;
				Element beg = getElement(eg, "beams");
				if(beg != null)
				{
					g.numberOfBeams = readInt(beg, "number");
					g.beamsOffset = readFile(beg).offset;
				}
				sm.groups.put(g.getName(), g);
			}
			for(Element eg:getElements(e, "nodeGroups", "group"))
			{
				String name = getElement(eg, "name").getTextContent();
				Group g = sm.groups.get(name);
				if(g == null)
				{
					g = new Group();
					g.name = name;
					sm.groups.put(g.getName(), g);
					g.numberOfNodes = readInt(eg, "number");
					g.nodesOffset = readFile(eg).offset;
				}
			}
			submeshes.add(sm);
		}
	}

	public List<SubMesh> getSubmeshes() {
		return Collections.unmodifiableList(submeshes);
	}

	public String getShape() {
		return shape;
	}
	@Override
	protected String getXSD() {
		return "jcae.xsd";
	}

	protected abstract int dim();
	protected String binDirectory()
	{
		return "jcae" + dim() + "d.files";
	}

	protected String xmlFile()
	{
		return "jcae" + dim() + "d";
	}

	protected File getBinFile(String name)
	{
		return new File(new File(path, binDirectory()), name);
	}
	
	public AmibeReader(String path) throws SAXException, IOException {
		this.path = path;
		read(new File(path, xmlFile()));
	}
}
