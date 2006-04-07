/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.
 
    Copyright (C) 2003,2004,2005
                  Jerome Robert <jeromerobert@users.sourceforge.net>
 
    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.
 
    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.
 
    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh.xmldata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.log4j.Logger;
import org.jcae.mesh.cad.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;


public class MeshToSoupConvert extends JCAEXMLData
{
	private static Logger logger=Logger.getLogger(MeshToSoupConvert.class);
	private int nrTriangles = 0;
	private int nrNodes = 0;
	private String xmlDir;
	private File rawFile;
	// Must be a multiple of 8*2, 4*3 and 8*10
	private static final int bufferSize = 15 << 12;
	private ByteBuffer bb = ByteBuffer.allocate(bufferSize);
	private IntBuffer bbI = bb.asIntBuffer();
	private DoubleBuffer bbD = bb.asDoubleBuffer();
	
	/** 
	 * @param args mesh directory, brep path
	 */
	public static void main(String[] args)
	{
		meshToSoup(args[0], CADShapeBuilder.factory.newShape(args[1]));
	}
	
	/**
	 * Compute raw 3D mesh
	 * @param xmlDir mesh directory
	 * @param shape
	 */
	public static void meshToSoup(String xmlDir, CADShape shape)
	{
		CADExplorer expF = CADShapeBuilder.factory.newExplorer();		
		int numFace=Integer.getInteger("org.jcae.mesh.Mesher.meshFace", 0).intValue();
		int minFace=Integer.getInteger("org.jcae.mesh.Mesher.minFace", 0).intValue();
		int maxFace=Integer.getInteger("org.jcae.mesh.Mesher.maxFace", 0).intValue();

		int iFace = 0;
		MeshToSoupConvert m2dTo3D = new MeshToSoupConvert(xmlDir);
		m2dTo3D.initialize("soup", false);
		iFace = 0;
		for (expF.init(shape, CADExplorer.FACE); expF.more(); expF.next())
		{
			CADFace F = (CADFace) expF.current();
			iFace++;
			if (numFace != 0 && iFace != numFace)
				continue;
			if ((minFace != 0 || maxFace != 0) && !(iFace >= minFace && iFace <= maxFace))
				continue;
			String xmlFile = "jcae2d."+iFace;
			logger.info("Importing face "+iFace);
			m2dTo3D.convert(xmlFile, iFace, F);
		}
		m2dTo3D.finish();
	}
	
	public MeshToSoupConvert (String dir)
	{
		xmlDir = dir;
	}
	
	public void initialize(String xmlOutFile, boolean writeNormal)
	{
		rawFile = new File(xmlDir, xmlOutFile);
		rawFile.delete();
	}
	
	public void finish()
	{
		logger.info("Total number of nodes: "+nrNodes);
		logger.info("Total number of triangles: "+nrTriangles);
	}
	
	public void convert(String xmlInFile, int groupId, CADFace F)
	{
		Document documentIn;
		try
		{
			documentIn = XMLHelper.parseXML(new File(xmlDir, xmlInFile));
		}
		catch(FileNotFoundException ex)
		{
			return;
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}

		XPath xpath = XPathFactory.newInstance().newXPath();
		CADGeomSurface surface = F.getGeomSurface();
		surface.dinit(0);
		try
		{
			Node submeshElement = (Node) xpath.evaluate("/jcae/mesh/submesh",
				documentIn, XPathConstants.NODE);
			Node submeshNodes = (Node) xpath.evaluate("nodes", submeshElement,
				XPathConstants.NODE);
			Node submeshFaces = (Node) xpath.evaluate("triangles",
				submeshElement, XPathConstants.NODE);
			
			int numberOfNodes = Integer.parseInt(xpath.evaluate(
				"number/text()", submeshNodes));

			logger.debug("Reading " + numberOfNodes + " nodes");
			String nodesFile = xpath.evaluate("file/@location", submeshNodes);
			FileChannel fcN = new FileInputStream(xmlDir+File.separator+nodesFile).getChannel();
			double [] coord = new double[3*numberOfNodes];
			bb.clear();
			bbD.clear();
			int index = 0;
			int remaining = numberOfNodes;
			int nf = bufferSize / 16;
			for (int nblock = (remaining * 16) / bufferSize; nblock >= 0; --nblock)
			{
				if (remaining <= 0)
					break;
				else if (remaining < nf)
					nf = remaining;
				remaining -= nf;
				bb.rewind();
				fcN.read(bb);
				bbD.rewind();
				for(int nr = 0; nr < nf; nr ++)
				{
					double u = bbD.get();
					double v = bbD.get();
					double [] p3 = surface.value(u, v);
					for (int j = 0; j < 3; j++)
						coord[3*index+j] = p3[j];
					index++;
				}
			}
			assert index == numberOfNodes;
			fcN.close();
			
			int numberOfFaces = Integer.parseInt(xpath.evaluate(
				"number/text()", submeshFaces));
			logger.debug("Reading " + numberOfFaces + " faces");
			String trianglesFile = xpath.evaluate("file/@location",
				submeshFaces);
			FileChannel fcT = new FileInputStream(xmlDir+File.separator+trianglesFile).getChannel();
			bb.clear();
			bbI.clear();
			FileChannel fcO = new FileOutputStream(rawFile, true).getChannel();
			ByteBuffer bbo = ByteBuffer.allocate(bufferSize * 80 / 12);
			DoubleBuffer bboD = bbo.asDoubleBuffer();
			int ind [] = new int[3];
			double [] c = new double[9];
			remaining = numberOfFaces;
			nf = bufferSize / 12;
			for (int nblock = (remaining * 12) / bufferSize; nblock >= 0; --nblock)
			{
				if (remaining <= 0)
					break;
				else if (remaining < nf)
					nf = remaining;
				remaining -= nf;
				bb.rewind();
				fcT.read(bb);
				bbI.rewind();
				bboD.rewind();
				for(int nr = 0; nr < nf; nr ++)
				{
					bbI.get(ind);
					for (int j = 0; j < 3; j++)
						for (int k = 0; k < 3; k++)
							c[3*j+k] = coord[3*ind[j]+k];
					if (F.isOrientationForward())
					{
						for (int j = 0; j < 3; j++)
							for (int k = 0; k < 3; k++)
								bboD.put(c[3*j+k]);
					}
					else
					{
						for (int j = 2; j >= 0; j--)
							for (int k = 0; k < 3; k++)
								bboD.put(c[3*j+k]);
					}
					//  Align om 64bit
					bbo.position(8*bboD.position());
					bbo.putInt(groupId);
					bbo.putInt(0);
					bboD.position(1+bboD.position());
				}
				bbo.flip();
				fcO.write(bbo);
			}
			fcT.close();
			fcO.close();
			logger.debug("End reading");
			nrNodes += numberOfNodes;
			nrTriangles += numberOfFaces;
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
}

