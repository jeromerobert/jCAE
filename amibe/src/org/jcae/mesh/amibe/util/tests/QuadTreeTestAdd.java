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
import org.jcae.mesh.amibe.util.QuadTreeTest;
import org.jcae.mesh.amibe.ds.Vertex;
import java.util.Random;
import org.jcae.mesh.java3d.Viewer;

/**
 * Unit test to check insertion of vertices.
 * Run
 * <pre>
 *   QuadTreeTestAdd
 * </pre>
 * to display an initial <code>QuadTree</code> with 500 vertices.
 * Click to add vertices.
 */
public class QuadTreeTestAdd extends QuadTreeTest
{
	private static Logger logger=Logger.getLogger(QuadTreeTestAdd.class);	
	
	public QuadTreeTestAdd(double umin, double umax, double vmin, double vmax)
	{
		super (umin, umax, vmin, vmax);
	}
	
	public static void display(Viewer view, QuadTreeTest r)
	{
		view.addBranchGroup(r.bgQuadTree());
		view.setVisible(true);
		view.addBranchGroup(r.bgVertices());
		view.setVisible(true);
	}
	
	public static void main(String args[])
	{
		double u, v;
		boolean visu = true;
		Random rand = new Random(113L);
		final QuadTreeTest r = new QuadTreeTest(0.0, 1.0, 0.0, 1.0);
		logger.debug("Start insertion");
		for (int i = 0; i < 500; i++)
		{
			u = rand.nextDouble();
			v = rand.nextDouble();
			r.add(new Vertex(u, v));
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
					double [] xyz = view.getLastClick();
					if (null != xyz)
					{
						Vertex vt = new Vertex(xyz[0], xyz[1]);
						r.add(vt);
						view.removeAllBranchGroup();
						display(view, r);
					}
				}
			};
		}
	}
}
