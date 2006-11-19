/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
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
import org.jcae.mesh.cad.*;
import org.jcae.mesh.mesher.ds.SubMesh1D;
import org.jcae.mesh.mesher.ds.MNode1D;
import org.jcae.mesh.mesher.ds.MEdge1D;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.ds.Vertex2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Collection;
import java.util.Iterator;
import gnu.trove.TObjectIntHashMap;
import org.apache.log4j.Logger;

public class BinaryWriter
{
	private static Logger logger=Logger.getLogger(BinaryWriter.class);

	public static void writeCADEdge(BCADGraphCell edge, String outDir)
	{
		CADEdge E = (CADEdge) edge.getShape();
		if (E.isDegenerated())
			return;
		try
		{
			File dir = new File(outDir);

			//create the output directory if it does not exist
			if(!dir.exists())
				dir.mkdirs();

			File nodesFile = new File(dir, "n"+edge.getId());
			if(nodesFile.exists())
				nodesFile.delete();
			File parasFile = new File(dir, "p"+edge.getId());
			if(parasFile.exists())
				parasFile.delete();
			File reffile = new File(dir, "r"+edge.getId());
			if(reffile.exists())
				reffile.delete();
			File beamsFile=new File(dir, "b"+edge.getId());
			if(beamsFile.exists())
				beamsFile.delete();
			
			SubMesh1D submesh = (SubMesh1D) edge.mesh;
			if (null == submesh)
				return;

			//save nodes
			logger.debug("begin writing "+nodesFile+" and "+parasFile);
			TObjectIntHashMap localIdx = new TObjectIntHashMap();
			DataOutputStream nodesout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(nodesFile, true)));
			DataOutputStream parasout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(parasFile, true)));
			DataOutputStream refsout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(reffile, true)));
			// Set first index to 1; a null index in localIdx is thus an error
			int i = 1;
			CADShapeBuilder factory = CADShapeBuilder.factory;
			CADGeomCurve3D curve = factory.newCurve3D((CADEdge) edge.getShape());
			for (Iterator itn = submesh.getNodesIterator(); itn.hasNext(); )
			{
				MNode1D n = (MNode1D)itn.next();
				double p = n.getParameter();
				parasout.writeDouble(p);
				double [] xyz = curve.value(p);
				for (int k = 0; k < 3; k++)
					nodesout.writeDouble(xyz[k]);
				localIdx.put(n, i);
				i++;
				CADVertex v = n.getCADVertex();
				if (null != v)
				{
					BCADGraphCell vv = edge.getGraph().getByShape(v);
					refsout.writeInt(i);
					refsout.writeInt(vv.getId());
				}
			}
			nodesout.close();
			parasout.close();
			refsout.close();
			logger.debug("end writing "+nodesFile+" and "+parasFile);

			//save beams
			logger.debug("begin writing "+beamsFile);
			DataOutputStream beamsout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(beamsFile, true)));
			Iterator ite = submesh.getEdgesIterator();
			while(ite.hasNext())
			{
				MEdge1D e = (MEdge1D)ite.next();
				MNode1D pt1 = e.getNodes1();
				MNode1D pt2 = e.getNodes2();
				beamsout.writeInt(localIdx.get(pt1));
				beamsout.writeInt(localIdx.get(pt2));
			}
			beamsout.close();
			logger.debug("end writing "+beamsFile);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public static void writeCADFace(BCADGraphCell face, String outDir)
	{
		try
		{
			File dir = new File(outDir);

			// Create the output directory if it does not exist
			if(!dir.exists())
				dir.mkdirs();

			Mesh submesh = (Mesh) face.mesh;
			if (null == submesh)
				return;

			CADFace F = (CADFace) face.getShape();
			Collection trianglelist = submesh.getTriangles();
			List nodelist = submesh.quadtree.getAllVertices(trianglelist.size() / 2);
			TObjectIntHashMap localIdx = write2dNodeReferences(outDir, face.getId(), nodelist);
			write2dCoordinates(outDir, face.getId(), nodelist, F.getGeomSurface());
			write2dTriangles(outDir, face.getId(), trianglelist, localIdx);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}

	private static TObjectIntHashMap write2dNodeReferences(String dir, int id, List nodelist)
		throws IOException, FileNotFoundException
	{
		File refFile = new File(dir, "r"+id);
		if(refFile.exists())
			refFile.delete();

		// Save references
		logger.debug("begin writing "+refFile);
		DataOutputStream refsout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(refFile, true)));

		TObjectIntHashMap localIdx = new TObjectIntHashMap(nodelist.size());
		// Set first index to 1; a null index in localIdx is thus an error
		int i = 1;
		//  Write interior nodes first
		for (Iterator itn = nodelist.iterator(); itn.hasNext(); )
		{
			Vertex n = (Vertex) itn.next();
			if (n == Vertex2D.outer)
				continue;
			int ref1d = n.getRef();
			if (0 != ref1d)
				continue;
			localIdx.put(n, i);
			i++;
		}
		//  Write boundary nodes and 1D references
		int nref = 0;
		for (Iterator itn = nodelist.iterator(); itn.hasNext(); )
		{
			Vertex n = (Vertex) itn.next();
			if (n == Vertex2D.outer)
				continue;
			int ref1d = n.getRef();
			if (0 == ref1d)
				continue;
			localIdx.put(n, i);
			i++;
			refsout.writeInt(Math.abs(ref1d));
			nref++;
		}
		refsout.close();
		return localIdx;
	}

	private static void write2dCoordinates(String dir, int id, List nodelist, CADGeomSurface surface)
		throws IOException, FileNotFoundException
	{
		File nodesFile = new File(dir, "n"+id);
		if(nodesFile.exists())
			nodesFile.delete();
		File parasFile = new File(dir, "p"+id);
		if(parasFile.exists())
			parasFile.delete();

		// Save nodes
		logger.debug("begin writing "+nodesFile+" and "+parasFile);
		DataOutputStream nodesout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(nodesFile, true)));
		DataOutputStream parasout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(parasFile, true)));
		//  Write interior nodes first, then boundary nodes
		for (int phase = 0; phase < 2; phase++)
		{
			for (Iterator itn = nodelist.iterator(); itn.hasNext(); )
			{
				Vertex n = (Vertex) itn.next();
				if (n == Vertex2D.outer)
					continue;
				int ref1d = n.getRef();
				if ((0 != ref1d && phase == 0) || (0 == ref1d && phase == 1))
					continue;
				double [] p = n.getUV();
				for (int d = 0; d < p.length; d++)
					parasout.writeDouble(p[d]);
				double [] xyz = surface.value(p[0], p[1]);
				for (int k = 0; k < 3; k++)
					nodesout.writeDouble(xyz[k]);
			}
		}
		nodesout.close();
		parasout.close();
		logger.debug("end writing "+nodesFile+" and "+parasFile);
	}

	private static void write2dTriangles(String dir, int id, Collection trianglelist, TObjectIntHashMap localIdx)
		throws IOException, FileNotFoundException
	{
		File facesFile=new File(dir, "f"+id);
		if(facesFile.exists())
			facesFile.delete();

		// Save faces
		logger.debug("begin writing "+facesFile);
		DataOutputStream facesout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(facesFile, true)));
		for (Iterator itf = trianglelist.iterator(); itf.hasNext(); )
		{
			Triangle f = (Triangle) itf.next();
			if (f.isOuter())
				continue;
			for (int j = 0; j < 3; j++)
				facesout.writeInt(localIdx.get(f.vertex[j]));
		}
		facesout.close();
	}
}
