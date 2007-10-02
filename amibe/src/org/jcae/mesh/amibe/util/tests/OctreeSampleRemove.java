/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2004,2005, by EADS CRC
    Copyright (C) 2007, by EADS France

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

import org.jcae.mesh.amibe.util.KdTree;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.ds.Mesh;
import java.util.Random;
import org.jcae.mesh.java3d.Viewer;

/**
 * Unit test to check removal of vertices.
 * Run
 * <pre>
 *   OctreeSampleRemove
 * </pre>
 * to display an initial <code>Octree</code> with 200 vertices.
 * Click to remove vertices.
 */
public class OctreeSampleRemove extends OctreeSample
{
	public OctreeSampleRemove(KdTree o)
	{
		super(o);
	}
	
	public static void display(Viewer view, OctreeSample t)
	{
		view.addBranchGroup(t.bgOctree());
		view.setVisible(true);
		view.addBranchGroup(t.bgVertices());
		view.setVisible(true);
	}
	
	public static void main(String args[])
	{
		boolean visu = true;
		Random rand = new Random(113L);
		double [] bbox = { 0.0, 0.0, 0.0, 1.0, 1.0, 1.0 };
		final Mesh mesh = new Mesh();
		final KdTree r = new KdTree(bbox);
		final OctreeSample t = new OctreeSample(r);
		double [] xyz = new double[3];
		for (int i = 0; i < 200; i++)
		{
			xyz[0] = rand.nextDouble();
			xyz[1] = rand.nextDouble();
			xyz[2] = rand.nextDouble();
			r.add((Vertex) mesh.createVertex(xyz));
		}
		//CheckCoordProcedure checkproc = new CheckCoordProcedure();
		//r.walk(checkproc);
		
		final Viewer view=new Viewer();
		if (visu)
		{
			display(view, t);
			view.zoomTo(); 
			view.callBack=new Runnable()
			{
				public void run()
				{
					double [] xyzPick = view.getLastClick();
					if (null != xyzPick)
					{
						Vertex vt = r.getNearVertex(mesh, (Vertex) mesh.createVertex(xyzPick));
						r.remove(vt);
						view.removeAllBranchGroup();
						display(view, t);
					}
				}
			};
		}
	}
}
