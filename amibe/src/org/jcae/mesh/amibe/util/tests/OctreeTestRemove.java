/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2004,2005, by EADS CRC

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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */

package org.jcae.mesh.amibe.util.tests;

import org.apache.log4j.Logger;
import org.jcae.mesh.amibe.util.OctreeTest;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.ds.Mesh;
import java.util.Random;
import org.jcae.mesh.java3d.Viewer;

/**
 * Unit test to check removal of vertices.
 * Run
 * <pre>
 *   OctreeTestRemove
 * </pre>
 * to display an initial <code>Octree</code> with 200 vertices.
 * Click to remove vertices.
 */
public class OctreeTestRemove extends OctreeTest
{
	private static Logger logger=Logger.getLogger(OctreeTestRemove.class);	
	
	public OctreeTestRemove(double [] umin, double [] umax)
	{
		super (umin, umax);
	}
	
	public static void display(Viewer view, OctreeTest r)
	{
		view.addBranchGroup(r.bgOctree());
		view.setVisible(true);
		view.addBranchGroup(r.bgVertices());
		view.setVisible(true);
	}
	
	public static void main(String args[])
	{
		boolean visu = true;
		Random rand = new Random(113L);
		double [] umin = { 0.0, 0.0, 0.0 };
		double [] umax = { 1.0, 1.0, 1.0 };
		final Mesh mesh = new Mesh();
		final OctreeTest r = new OctreeTest(umin, umax);
		logger.debug("Start insertion");
		double [] xyz = new double[3];
		for (int i = 0; i < 200; i++)
		{
			xyz[0] = rand.nextDouble();
			xyz[1] = rand.nextDouble();
			xyz[2] = rand.nextDouble();
			r.add((Vertex) mesh.factory.createVertex(xyz));
		}
		//CheckCoordProcedure checkproc = new CheckCoordProcedure();
		//r.walk(checkproc);
		
		final Viewer view=new Viewer();
		if (visu)
		{
			display(view, r);
			view.zoomTo(); 
			view.callBack=new Runnable()
			{
				public void run()
				{
					double [] xyzPick = view.getLastClick();
					if (null != xyzPick)
					{
						Vertex vt = r.getNearVertex(mesh, (Vertex) mesh.factory.createVertex(xyzPick));
						r.remove(vt);
						view.removeAllBranchGroup();
						display(view, r);
					}
				}
			};
		}
	}
}
