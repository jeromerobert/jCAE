/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.
 
    Copyright (C) 2006, by EADS CRC
 
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

package org.jcae.mesh.bora.xmldata;

import org.jcae.mesh.bora.ds.BCADGraphCell;
import org.jcae.mesh.bora.ds.BCADGraph;
import org.jcae.mesh.bora.ds.BModel;
import org.jcae.mesh.xmldata.UNVConverter;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import java.io.File;
import java.io.FileInputStream;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import gnu.trove.TIntObjectHashMap;
import org.apache.log4j.Logger;


public class BinaryReader
{
	private static Logger logger=Logger.getLogger(BinaryReader.class);
	
	/**
	 * Create a Mesh instance from an XML file.
	 * @param xmlDir       directory containing XML files
	 * @param xmlFile      basename of the main XML file
	 * @param F            yopological surface
	 */
	public static Mesh readObject(BCADGraphCell root)
	{
		return readObject(root, false);
	}
	public static Mesh readObject(BCADGraphCell root, boolean buildAdj)
	{
		Mesh mesh = new Mesh();
		mesh.setType(Mesh.MESH_3D);
		BModel model = root.getGraph().getModel();
		TIntObjectHashMap vertMap = new TIntObjectHashMap();
		for (Iterator it = root.uniqueShapesExplorer(BCADGraph.DIM_FACE); it.hasNext(); )
		{
			BCADGraphCell s = (BCADGraphCell) it.next();
			String nfile = model.getOutputDir()+File.separator+model.get2dDir()+File.separator+"n"+s.getId();
			try
			{
				File nodesFile = new File(model.getOutputDir()+File.separator+model.get2dDir(), "n"+s.getId());
				File refFile = new File(model.getOutputDir()+File.separator+model.get2dDir(), "r"+s.getId());
				File trianglesFile = new File(model.getOutputDir()+File.separator+model.get2dDir(), "f"+s.getId());
	
				FileChannel fcR = new FileInputStream(refFile).getChannel();
				MappedByteBuffer bbR = fcR.map(FileChannel.MapMode.READ_ONLY, 0L, fcR.size());
				IntBuffer refsBuffer = bbR.asIntBuffer();
				FileChannel fcN = new FileInputStream(nodesFile).getChannel();
				MappedByteBuffer bbN = fcN.map(FileChannel.MapMode.READ_ONLY, 0L, fcN.size());
				DoubleBuffer nodesBuffer = bbN.asDoubleBuffer();
				
				FileChannel fcT = new FileInputStream(trianglesFile).getChannel();
				MappedByteBuffer bbT = fcT.map(FileChannel.MapMode.READ_ONLY, 0L, fcT.size());
				IntBuffer trianglesBuffer = bbT.asIntBuffer();
	
				int numberOfReferences = (int) refFile.length() / 4;
				int [] refs = new int[numberOfReferences];
				refsBuffer.get(refs);
				fcR.close();
				
				int numberOfNodes = (int) nodesFile.length() / 24;
				Vertex [] nodelist = new Vertex[numberOfNodes];
				int label;
				double [] coord = new double[3];
				logger.debug("Reading "+numberOfNodes+" nodes");
				double [] bbmin = new double[3];
				double [] bbmax = new double[3];
				for (int j = 0; j < 3; j++)
				{
					bbmin[j] = Double.MAX_VALUE;
					bbmax[j] = Double.MIN_VALUE;
				}
				for (int i=0; i < numberOfNodes; i++)
				{
					nodesBuffer.get(coord);
					nodelist[i] = mesh.newVertex(coord[0], coord[1], coord[2]);
					if (i < numberOfNodes - numberOfReferences)
						label = 0;
					else
					{
						label = refs[i+numberOfReferences-numberOfNodes];
						Object o = vertMap.get(label);
						if (o == null)
							vertMap.put(label, nodelist[i]);
						else
							nodelist[i] = (Vertex) o;
					}
					nodelist[i].setRef(label);
					for (int j = 0; j < 3; j++)
					{
						if (coord[j] > bbmax[j])
							bbmax[j] = coord[j];
						if (coord[j] < bbmin[j])
							bbmin[j] = coord[j];
					}
				} 
				fcN.close();
				UNVConverter.clean(bbN);
				
				int numberOfTriangles = (int) trianglesFile.length() / 12;
				logger.debug("Reading "+numberOfTriangles+" elements");
				Triangle [] facelist = new Triangle[numberOfTriangles];
				for (int i=0; i < numberOfTriangles; i++)
				{
					Vertex pt1 = nodelist[trianglesBuffer.get()-1];
					Vertex pt2 = nodelist[trianglesBuffer.get()-1];
					Vertex pt3 = nodelist[trianglesBuffer.get()-1];
					facelist[i] = new Triangle(pt1, pt2, pt3);
					mesh.add(facelist[i]);
					pt1.setLink(facelist[i]);
					pt2.setLink(facelist[i]);
					pt3.setLink(facelist[i]);
				}
				fcT.close();
				UNVConverter.clean(bbT);
			}
			catch(Exception ex)
			{
				ex.printStackTrace();
				throw new RuntimeException(ex);
			}
			logger.debug("end reading "+nfile);
		}
		return mesh;
	}
}

