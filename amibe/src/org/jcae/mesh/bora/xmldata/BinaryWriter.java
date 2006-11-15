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

import org.jcae.mesh.bora.ds.*;
import org.jcae.mesh.cad.*;
import org.jcae.mesh.mesher.ds.*;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.util.Iterator;
import gnu.trove.TObjectIntHashMap;
import org.apache.log4j.Logger;

public class BinaryWriter
{
	private static Logger logger=Logger.getLogger(BinaryWriter.class);

	public static void writeCADEdge(BCADGraphCell edge, String xmlDir)
	{
		CADEdge E = (CADEdge) edge.getShape();
		if (E.isDegenerated())
			return;
		try
		{
			File dir = new File(xmlDir, "edges");

			//create the output directory if it does not exist
			if(!dir.exists())
				dir.mkdirs();

			File nodesFile = new File(dir, "n"+edge.getId());
			if(nodesFile.exists())
				nodesFile.delete();
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
			logger.debug("begin writing "+nodesFile);
			TObjectIntHashMap localIdx = new TObjectIntHashMap();
			DataOutputStream nodesout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(nodesFile, true)));
			DataOutputStream refsout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(reffile, true)));
			// Set first index to 1; a null index in localIdx is thus an error
			int i = 1;
			for (Iterator itn = submesh.getNodesIterator(); itn.hasNext(); )
			{
				MNode1D n = (MNode1D)itn.next();
				nodesout.writeDouble(n.getParameter());
				localIdx.put(n, i);
				i++;
				CADVertex v = n.getCADVertex();
				if (null != v)
				{
					BCADGraphCell vv = edge.getGraph().cadToGraphCell(v);
					refsout.writeInt(i);
					refsout.writeInt(vv.getId());
				}
			}
			// Append 3D coordinates
			CADShapeBuilder factory = CADShapeBuilder.factory;
			CADGeomCurve3D curve = factory.newCurve3D((CADEdge) edge.getShape());
			for (Iterator itn = submesh.getNodesIterator(); itn.hasNext(); )
			{
				MNode1D n = (MNode1D) itn.next();
				double [] xyz = curve.value(n.getParameter());
				for (int k = 0; k < 3; k++)
					nodesout.writeDouble(xyz[k]);
			}
			nodesout.close();
			refsout.close();
			logger.debug("end writing "+nodesFile);

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
}
