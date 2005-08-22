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

import org.jcae.mesh.cad.CADGeomSurface;
import org.jcae.mesh.cad.CADFace;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import gnu.trove.TIntIntHashMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.apache.xpath.CachedXPathAPI;
import org.apache.log4j.Logger;


public class MeshToSoupConvert extends JCAEXMLData
{
	private static Logger logger=Logger.getLogger(MeshToSoupConvert.class);
	private int nrTriangles = 0;
	private int nrNodes = 0;
	private String xmlDir;
	private File rawFile;
	
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
		int i;
		CachedXPathAPI xpath = new CachedXPathAPI();
		CADGeomSurface surface = F.getGeomSurface();
		surface.dinit(0);
		try
		{
			String nodesFile = xpath.selectSingleNode(documentIn,
				"/jcae/mesh/submesh/nodes/file/@location").getNodeValue();
			FileChannel fcN = new FileInputStream(xmlDir+File.separator+nodesFile).getChannel();
			MappedByteBuffer bbN = fcN.map(FileChannel.MapMode.READ_ONLY, 0L, fcN.size());
			DoubleBuffer nodesBuffer = bbN.asDoubleBuffer();
			String trianglesFile = xpath.selectSingleNode(documentIn,
				"/jcae/mesh/submesh/triangles/file/@location").getNodeValue();
			FileChannel fcT = new FileInputStream(xmlDir+File.separator+trianglesFile).getChannel();
			MappedByteBuffer bbT = fcT.map(FileChannel.MapMode.READ_ONLY, 0L, fcT.size());
			IntBuffer trianglesBuffer = bbT.asIntBuffer();
			
			Node submeshElement = xpath.selectSingleNode(documentIn,
				"/jcae/mesh/submesh");
			Node submeshNodes = xpath.selectSingleNode(submeshElement, "nodes");
			
			int numberOfNodes = Integer.parseInt(
				xpath.selectSingleNode(submeshNodes, "number/text()").getNodeValue());
			logger.debug("Reading "+numberOfNodes+" nodes");
			double [] coord = new double[3*numberOfNodes];
			for (i = 0; i < numberOfNodes; i++)
			{
				double u = nodesBuffer.get();
				double v = nodesBuffer.get();
				double [] p3 = surface.value(u, v);
				for (int j = 0; j < 3; j++)
					coord[3*i+j] = p3[j];
			}
			
			Node submeshFaces = xpath.selectSingleNode(submeshElement, "triangles");
			int numberOfFaces = Integer.parseInt(
					xpath.selectSingleNode(submeshFaces, "number/text()").getNodeValue());
			logger.debug("Reading "+numberOfFaces+" faces");
			int ind [] = new int[3];
			double [] c = new double[9];
			DataOutputStream rawOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(rawFile, true)));
			for (i=0; i < numberOfFaces; i++)
			{
				for (int j = 0; j < 3; j++)
				{
					// Local node number for this group
					int indLoc = trianglesBuffer.get();
					for (int k = 0; k < 3; k++)
						c[3*j+k] = coord[3*indLoc+k];
				}
				if (F.isOrientationForward())
				{
					for (int j = 0; j < 3; j++)
						for (int k = 0; k < 3; k++)
							rawOut.writeDouble(c[3*j+k]);
				}
				else
				{
					for (int j = 2; j >= 0; j--)
						for (int k = 0; k < 3; k++)
							rawOut.writeDouble(c[3*j+k]);
				}
				rawOut.writeInt(groupId);
			}
			logger.debug("End reading");
			nrNodes += numberOfNodes;
			nrTriangles += numberOfFaces;
			
			fcT.close();
			UNVConverter.clean(bbT);
			fcN.close();
			UNVConverter.clean(bbN);
			rawOut.close();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
}

