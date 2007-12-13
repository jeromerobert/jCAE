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
 * (C) Copyright 2007, by EADS France
 */

package org.jcae.mesh.xmldata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Create a soup from a 3D mesh.
 * MeshToSoupConvert only create soup from 2D mesh, and cannot
 * be used on mesh imported to jCAE from UNV.
 * @author Jerome Robert
 *
 */
public class Mesh3dToSoupConvert
{
	public static void convert(String meshDirectory) throws XPathExpressionException, ParserConfigurationException, SAXException, IOException
	{
		XPath xpath = XPathFactory.newInstance().newXPath();
		File xmlFile3d = new File(meshDirectory, JCAEXMLData.xml3dFilename);
		Document document = XMLHelper.parseXML(xmlFile3d);
		String formatVersion = xpath.evaluate("/jcae/@version", document);
		if (formatVersion != null && formatVersion.length() > 0)
			throw new RuntimeException("File "+xmlFile3d+" has been written by a newer version of jCAE and cannot be re-read");
		String fnodes = (String) xpath.evaluate(
			"/jcae/mesh/submesh/nodes/file/@location", document,
			XPathConstants.STRING);
		String ftrias = (String) xpath.evaluate(
			"/jcae/mesh/submesh/triangles/file/@location", document,
			XPathConstants.STRING);
		FileChannel nodesChannel=new FileInputStream(
			new File(meshDirectory, fnodes)).getChannel();
		FileChannel triasChannel=new FileInputStream(
			new File(meshDirectory, ftrias)).getChannel();
		FileChannel soupChannel=new FileOutputStream(
			new File(meshDirectory, "soup")).getChannel();
		
		convert(nodesChannel, triasChannel, soupChannel);
		nodesChannel.close();
		triasChannel.close();
		soupChannel.close();
	}

	private static void convert(FileChannel nodesChannel,
		FileChannel triasChannel, FileChannel soupChannel) throws IOException
	{
		
		ByteBuffer trias=ByteBuffer.allocate(3*4);
		ByteBuffer nodes=ByteBuffer.allocate(3*8);
		
		ByteBuffer toWrite=ByteBuffer.allocate(80);
		toWrite.putInt(64,0); //groupId
		toWrite.putInt(72,0); //align on 64 bit
		
		while(triasChannel.read(trias)>0)
		{
			trias.rewind();
			for(int i=0; i<3; i++)
			{
				int nodeId=trias.getInt();
				nodesChannel.read(nodes, 3*8*nodeId);
				nodes.rewind();
				for(int j=0; j<3; j++)
					toWrite.putDouble(nodes.getDouble());
				nodes.rewind();
			}
			trias.rewind();
			toWrite.rewind();
			soupChannel.write(toWrite);
			toWrite.rewind();
		}
	}
	
	public static void main(String[] args)
	{
		String dir = "/home/jerome/JCAEProject/amibe2.dir";
		if (args.length >= 1)
			dir = args[0];
		try
		{
			Mesh3dToSoupConvert.convert(dir);
		}
		catch (Exception e)
		{	
			e.printStackTrace();
		}
	}
}
