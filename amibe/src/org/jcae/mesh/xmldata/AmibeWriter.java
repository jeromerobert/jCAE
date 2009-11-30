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
			super(name);
		}

		public void addNode(double x) throws IOException
		{
			nodeBuffer.rewind();
			nodeBuffer.putDouble(x);
			nodeBuffer.rewind();
			cnode.write(nodeBuffer);
			numberOfNodes ++;
		}

		@Override
		protected int dim() {
			return 1;
		}
	}

	public static class Dim2 extends AmibeWriter {

		public Dim2(String name) throws IOException {
			super(name);
		}

		public void addNode(double x, double y) throws IOException
		{
			nodeBuffer.rewind();
			nodeBuffer.putDouble(x);
			nodeBuffer.putDouble(y);
			nodeBuffer.rewind();
			cnode.write(nodeBuffer);
			numberOfNodes ++;
		}
		@Override
		protected int dim() {
			return 2;
		}
	}

	public static class Dim3 extends AmibeWriter {

		public Dim3(String name) throws IOException {
			super(name);
		}

		public void addNode(double x, double y, double z) throws IOException
		{
			nodeBuffer.rewind();
			nodeBuffer.putDouble(x);
			nodeBuffer.putDouble(y);
			nodeBuffer.putDouble(z);
			nodeBuffer.rewind();
			cnode.write(nodeBuffer);
			numberOfNodes ++;
		}

		@Override
		protected int dim() {
			return 3;
		}
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
	
	protected FileChannel cnode, ctria, cgroups;
	protected ByteBuffer nodeBuffer, triangleBufer;
	private XMLWriter xmlWriter;
	protected int numberOfNodes, numberOfTriangles;
	private List<Group> groups = new ArrayList<Group>();
	private long groupOffset;
	private Group currentGroup;
	private boolean checkNoGroup;
	public void addNodeRef(int n)
	{
		
	}
	public void addTriangle(int i, int j, int k) throws IOException
	{
		triangleBufer.rewind();
		triangleBufer.putInt(i);
		triangleBufer.putInt(j);
		triangleBufer.putInt(k);		
		triangleBufer.rewind();
		ctria.write(triangleBufer);
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
			cnode.close();
			ctria.close();
			cgroups.close();
		} catch (SAXException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		} catch (XMLStreamException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}

	public AmibeWriter(String path) throws IOException
	{
		try {
			new File(path).mkdirs();
			File dir3d = new File(path, "jcae" + dim() + "d.files");
			dir3d.mkdirs();
			File fnode = new File(dir3d, "nodes" + dim() + "d.bin");
			File ftria = new File(dir3d, "triangles" + dim() + "d.bin");
			File fgrp = new File(dir3d, "groups.bin");
			cnode = new RandomAccessFile(fnode, "rw").getChannel();
			ctria = new FileOutputStream(ftria).getChannel();
			cgroups = new FileOutputStream(fgrp).getChannel();
			nodeBuffer = ByteBuffer.allocate(dim() * 8);
			triangleBufer = ByteBuffer.allocate(3 * 4);
			xmlWriter = new XMLWriter(new File(path, "jcae" + dim() + "d").getPath(),
				getClass().getResource("jcae.xsd"));
			xmlWriter.out.writeStartElement("jcae");
			xmlWriter.out.writeStartElement("mesh");
		} catch (XMLStreamException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}

	protected abstract int dim();

	public void getNode(int i, double[] nc) throws IOException {
		nodeBuffer.rewind();
		cnode.read(nodeBuffer, dim() * 8 * i);
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

	private void writeSubMesh() throws IOException
	{	
		try
		{
			XMLStreamWriter o = xmlWriter.out;
			String dir = "jcae" + dim() + "d.file/";
			o.writeStartElement("submesh");
			o.writeStartElement("nodes");
			writeNumber(numberOfNodes);
			writeFile("doublestream", dir + "nodes" + dim() + "d.bin",
				numberOfNodes);
			o.writeEndElement(); //nodes
			o.writeStartElement("triangles");
			writeNumber(numberOfNodes);
			writeFile("integerstream", dir + "triangles" + dim() + "d.bin",
				numberOfNodes);
			o.writeEndElement(); //triangles
			o.writeStartElement("groups");
			int id = 1;
			if (checkNoGroup) {
				checkNoGroup();
			}
			for (Group g : groups) {
				o.writeStartElement("group");
				o.writeAttribute("id", Integer.toString(id++));
				o.writeStartElement("name");
				o.writeCharacters(g.name);
				o.writeEndElement();
				writeNumber(g.nbElement);
				writeFile("integerstream", dir + "groups.bin", g.offset);
				o.writeEndElement();
			}
			o.writeEndElement(); //groups
			o.writeEndElement(); //submesh
		} catch (XMLStreamException ex) {
			Logger.getLogger(AmibeWriter.class.getName()).log(Level.SEVERE, null,
				ex);
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
				cgroups.write(bb);
				bb.rewind();
			}
		}
	}
}
