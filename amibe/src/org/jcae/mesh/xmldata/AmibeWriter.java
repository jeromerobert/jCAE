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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.xml.sax.SAXException;

/**
 *
 * @author Jerome Robert
 */
public abstract class AmibeWriter {

	private final static Logger LOGGER = Logger.getLogger(AmibeWriter.class.getName());
	public static class Dim1 extends AmibeWriter {

		public Dim1(String name) throws IOException {
			init(name);
		}

		public void addNode(double x) throws IOException
		{
			nodeBuffer.rewind();
			nodeBuffer.putDouble(x);
			nodeBuffer.rewind();
			nodeChan.write(nodeBuffer);
			numberOfNodes ++;
		}

		@Override
		protected int dim() {
			return 1;
		}
	}

	public static class Dim2 extends AmibeWriter {
		private final String binDirectory, xmlFile;
		public Dim2(String name, int index) throws IOException {			
			binDirectory = "jcae2d."+ index +".files";
			xmlFile = "jcae2d."+index;
			init(name);
		}

		public void addNode(double x, double y) throws IOException
		{
			nodeBuffer.rewind();
			nodeBuffer.putDouble(x);
			nodeBuffer.putDouble(y);
			nodeBuffer.rewind();
			nodeChan.write(nodeBuffer);
			numberOfNodes ++;
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

	public static class Dim3 extends AmibeWriter {

		public Dim3(String name) throws IOException {
			init(name);
		}

		public void addNode(double x, double y, double z) throws IOException
		{
			nodeBuffer.rewind();
			nodeBuffer.putDouble(x);
			nodeBuffer.putDouble(y);
			nodeBuffer.putDouble(z);
			nodeBuffer.rewind();
			nodeChan.write(nodeBuffer);
			numberOfNodes ++;
		}

		@Override
		protected int dim() {
			return 3;
		}

		@Override
		public void addNodeRef(int n) {}
	}

	/**
	 * Contains informations about groups, which will be written to the
	 * XML file
	 */
	private static class Group
	{
		String name;
		long offset;
		int nbElement;
	}
	
	protected FileChannel nodeChan, triaChan, groupChan, refChan;
	protected ByteBuffer nodeBuffer, triaBuffer, refBuffer;
	private XMLWriter xmlWriter;
	protected int numberOfNodes, numberOfTriangles, numberOfRef;
	private List<Group> groups = new ArrayList<Group>();
	private long groupOffset;
	private Group currentGroup;
	private boolean checkNoGroup;
	private String shape;
	private boolean shapeWritten;
	private int subShape;
	private boolean haveSubShape;

	/** Set the subShape */
	public void setSubShape(int i)
	{
		haveSubShape = true;
		subShape = i;
	}
	public void setShape(String shape)
	{
		if(shapeWritten)
			//only one shape by file
			throw new IllegalStateException();
		this.shape = shape;
	}
	public void addNodeRef(int n) throws IOException
	{
		refBuffer.rewind();
		refBuffer.putInt(0, n);
		refChan.write(refBuffer);
		numberOfRef++;
	}

	public void addNode(double[] coords) throws IOException
	{
		assert coords.length == dim();
		nodeBuffer.rewind();
		for(int i = 0; i<coords.length; i++)
			nodeBuffer.putDouble(i*8, coords[i]);
		nodeChan.write(nodeBuffer);
		numberOfNodes ++;

	}
	public void addTriangle(int i, int j, int k) throws IOException
	{
		triaBuffer.rewind();
		triaBuffer.putInt(i);
		triaBuffer.putInt(j);
		triaBuffer.putInt(k);
		triaBuffer.rewind();
		triaChan.write(triaBuffer);
		numberOfTriangles ++;
	}

	public void addTriangle(int[] indices) throws IOException
	{
		triaBuffer.rewind();
		triaBuffer.putInt(0, indices[0]);
		triaBuffer.putInt(4, indices[1]);
		triaBuffer.putInt(8, indices[2]);
		triaChan.write(triaBuffer);
		numberOfTriangles ++;
	}

	public void addBeam(int i, int j)
	{

	}

	public void nextSubMesh() throws IOException
	{
		writeSubMesh();
		numberOfNodes = 0;
		numberOfTriangles = 0;
		groups.clear();
		groupOffset = 0;
		numberOfRef = 0;
		haveSubShape = false;
	}

	public void nextGroup(String name)
	{
		Group g=new Group();
		g.name = name;
		g.offset= groupOffset;
		groups.add(g);
		currentGroup = g;
	}

	public void addElementToGroup(int id)
	{
		groupOffset ++;
		currentGroup.nbElement ++;
	}
	
	public void finish() throws IOException
	{
		try {
			writeSubMesh();
			xmlWriter.out.writeEndElement();
			xmlWriter.out.writeEndElement();
			xmlWriter.close();
			nodeChan.close();
			triaChan.close();
			groupChan.close();
			if(refChan != null)
				refChan.close();
		} catch (SAXException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		} catch (XMLStreamException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}

	private String nodeFName, triaFName, refFName;
	protected final void init(String path) throws IOException
	{
		try {
			new File(path).mkdirs();
			nodeFName = "nodes" + dim() + "d.bin";
			triaFName = "triangles" + dim() + "d.bin";
			File dir3d = new File(path, binDirectory());
			dir3d.mkdirs();
			File fnode = new File(dir3d, nodeFName );
			File ftria = new File(dir3d, triaFName );
			File fgrp = new File(dir3d, "groups.bin");
			if(hasRef())
			{
				refFName = "nodes1dref.bin";
				File ref = new File(dir3d, refFName);
				refChan = new FileOutputStream(ref).getChannel();
			}
			nodeChan = new RandomAccessFile(fnode, "rw").getChannel();
			triaChan = new FileOutputStream(ftria).getChannel();
			groupChan = new FileOutputStream(fgrp).getChannel();
			nodeBuffer = ByteBuffer.allocate(dim() * 8);
			triaBuffer = ByteBuffer.allocate(3 * 4);
			refBuffer = ByteBuffer.allocate(4);
			xmlWriter = new XMLWriter(new File(path, xmlFile()).getPath(),
				getClass().getResource("jcae.xsd"));
			xmlWriter.out.writeStartElement("jcae");
			xmlWriter.out.writeStartElement("mesh");
		} catch (XMLStreamException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
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
	
	public void getNode(int i, double[] nc) throws IOException {
		nodeBuffer.rewind();
		nodeChan.read(nodeBuffer, dim() * 8 * i);
		nodeBuffer.rewind();
		nodeBuffer.asDoubleBuffer().get(nc);
	}

	private void writeNumber(int i) throws XMLStreamException
	{
		XMLStreamWriter o = xmlWriter.out;
		o.writeStartElement("number");
		o.writeCharacters(Integer.toString(i));
		o.writeEndElement();
	}

	private void writeFile(String format, String location, long offset) throws XMLStreamException
	{
		XMLStreamWriter o = xmlWriter.out;
		o.writeStartElement("file");
		o.writeAttribute("format", format);
		o.writeAttribute("location", location);
		if(offset != 0)
			o.writeAttribute("offset", Long.toString(offset));
		o.writeEndElement();
	}

	private boolean hasRef()
	{
		return dim() < 3;
	}

	private void writeSubMesh() throws IOException
	{	
		try
		{
			XMLStreamWriter o = xmlWriter.out;
			if(shape != null && !shapeWritten)
			{
				o.writeStartElement("shape");
				writeFile("brep", shape, 0);
				o.writeEndElement();
				shapeWritten = true;
			}
			
			String dir = binDirectory()+"/";
			o.writeStartElement("submesh");

			if(haveSubShape)
			{
				o.writeStartElement("subshape");
				o.writeCharacters(Integer.toString(subShape));
				o.writeEndElement();
			}
			o.writeStartElement("nodes");
			writeNumber(numberOfNodes);
			writeFile("doublestream", dir + nodeFName, 0);
			if(hasRef())
			{
				o.writeStartElement("references");
				writeNumber(numberOfRef);
				writeFile("integerstream", dir + refFName, 0);
				o.writeEndElement();
			}
			o.writeEndElement(); //nodes

			o.writeStartElement("triangles");
			writeNumber(numberOfTriangles);
			writeFile("integerstream", dir + triaFName, 0);
			o.writeEndElement(); //triangles
			writeGroups();
			o.writeEndElement(); //submesh
		} catch (XMLStreamException ex) {
			Logger.getLogger(AmibeWriter.class.getName()).log(Level.SEVERE, null,
				ex);
		}
	}

	private void writeGroups() throws XMLStreamException, IOException
	{
		if (checkNoGroup) {
			checkNoGroup();
		}
		if(!groups.isEmpty())
		{
			XMLStreamWriter o = xmlWriter.out;
			o.writeStartElement("groups");
			int id = 1;
			for (Group g : groups) {
				o.writeStartElement("group");
				o.writeAttribute("id", Integer.toString(id++));
				o.writeStartElement("name");
				o.writeCharacters(g.name);
				o.writeEndElement();
				writeNumber(g.nbElement);
				writeFile("integerstream", binDirectory() + "/groups.bin", g.offset);
				o.writeEndElement();
			}
			o.writeEndElement(); //groups
		}
	}

	public void setFixNoGroup(boolean b)
	{
		checkNoGroup = b;
	}
	/**
	 * If do not contains any groups, create one with all
	 * elements
	 */
	private void checkNoGroup() throws IOException
	{
		if(groups.size()==0)
		{
			Group g=new Group();
			g.name="EXT";
			g.nbElement=numberOfTriangles;
			g.offset=0;
			groups.add(g);
			ByteBuffer bb=ByteBuffer.allocate(4);
			for(int i=0; i<numberOfTriangles; i++)
			{
				bb.putInt(i);
				bb.rewind();
				groupChan.write(bb);
				bb.rewind();
			}
		}
	}
}
