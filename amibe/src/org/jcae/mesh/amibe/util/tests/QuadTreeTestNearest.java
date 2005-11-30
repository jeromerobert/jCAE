/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2004,2005
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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */

package org.jcae.mesh.amibe.util.tests;

import org.apache.log4j.Logger;
import org.jcae.mesh.amibe.util.QuadTreeTest;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.ds.Mesh;
import java.util.Random;
import org.jcae.mesh.java3d.Viewer;

/**
 * Unit test to check detection of near vertices.
 * Run
 * <pre>
 *   QuadTreeTestNearest
 * </pre>
 * to display an initial <code>QuadTree</code> with 500 vertices.
 * When clicking at a point, a yellow segment is displayed between this point
 * and the nearest point found in the same cell, returned by
 * {@link org.jcae.mesh.amibe.util.QuadTree#getNearVertex(Vertex)}.
 * If {@link org.jcae.mesh.amibe.util.QuadTree#getNearestVertex(Vertex)}
 * finds a nearest point, a blue segment is displayed.
 */
public class QuadTreeTestNearest extends QuadTreeTest
{
	private static Logger logger=Logger.getLogger(QuadTreeTestNearest.class);	
	
	//  Dummy constructor
	public QuadTreeTestNearest(double umin, double umax, double vmin, double vmax)
	{
		super (umin, umax, vmin, vmax);
	}
	
	public static void main(String args[])
	{
		double u, v;
		boolean visu = true;
		Random rand = new Random(113L);
		final QuadTreeTest r = new QuadTreeTest(0.0, 1.0, 0.0, 1.0);
		Mesh m = new Mesh();
		m.pushCompGeom(2);
		m.setQuadTree(r);
		logger.debug("Start insertion");
		for (int i = 0; i < 500; i++)
		{
			u = rand.nextDouble();
			v = rand.nextDouble();
			Vertex pt = new Vertex(u, v);
			pt.addToQuadTree();
		}
		//CheckCoordProcedure checkproc = new CheckCoordProcedure();
		//r.walk(checkproc);
		
		final Viewer view=new Viewer();
		if (visu)
		{
			view.addBranchGroup(r.bgQuadTree());
			view.zoomTo(); 
			view.show();
			view.addBranchGroup(r.bgVertices());
			view.show();
		}
		
		for (int i = 0; i < 10; i++)
		{
			u = rand.nextDouble();
			v = rand.nextDouble();
			Vertex vt = new Vertex(u, v);
			if (visu)
			{
				view.addBranchGroup(r.segment(vt, r.getNearVertex(vt), 5.0f, 1, 1, 0));
				view.show(); 
				view.addBranchGroup(r.segment(vt, r.getNearestVertex(vt), 0.0f, 0, 1, 1));
				view.show(); 
			}
			else
			{
				r.getNearVertex(vt);
				r.getNearestVertex(vt);
			}
		}
		if (visu)
		{
			view.callBack=new Runnable()
			{
				public void run()
				{
					double [] xyz = view.getLastClick();
					if (null != xyz)
					{
						Vertex vt = new Vertex(xyz[0], xyz[1]);
						view.addBranchGroup(r.segment(vt, r.getNearVertex(vt), 5.0f, 1, 1, 0));
						view.show(); 
						view.addBranchGroup(r.segment(vt, r.getNearestVertex(vt), 0.0f, 0, 1, 1));
						view.show(); 
					}
				}
			};
			view.show(); 
		}
	}
}
