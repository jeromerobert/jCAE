/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2004,2005,2006, by EADS CRC
    Copyright (C) 2008, by EADS France

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

package org.jcae.mesh.amibe.patch;

import org.jcae.mesh.amibe.metrics.KdTree;

import java.util.Random;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.metrics.Metric;

/**
 * Unit test to check removal of vertices.
 * Run
 * <pre>
 *   QuadTreeSampleRemove
 * </pre>
 * to display an initial <code>QuadTree</code> with 500 vertices.
 * Click to remove vertices.
 */
public class QuadTreeSampleRemove extends QuadTreeSample
{
	private static final Logger logger=Logger.getLogger(QuadTreeSampleRemove.class.getName());
	
	public QuadTreeSampleRemove(KdTree q)
	{
		super (q);
	}
	
	public final Vertex2D getNearVertex(Metric metric, double[] uv)
	{
		return (Vertex2D) quadtree.getNearVertex(metric, uv);
	}

	public Vertex2D getNearestVertex(Metric metric, double[] uv)
	{
		return (Vertex2D) quadtree.getNearestVertex(metric, uv);
	}

	public static void display(Viewer view, QuadTreeSample r)
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
		double [] bbmin = { 0.0, 0.0 };
		double [] bbmax = { 1.0, 1.0 };
		final Mesh2D m = new Mesh2D();
		m.pushCompGeom(2);
		m.resetKdTree(bbmin, bbmax);
		final QuadTreeSampleRemove r = new QuadTreeSampleRemove(m.getKdTree());
		logger.fine("Start insertion");
		for (int i = 0; i < 500; i++)
		{
			u = rand.nextDouble();
			v = rand.nextDouble();
			Vertex2D pt = (Vertex2D) m.createVertex(u, v);
			r.quadtree.add(pt);
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
						Vertex2D picked = (Vertex2D) m.createVertex(xyz[0], xyz[1]);
						Metric metric = m.getMetric(picked);
						Vertex2D vt = r.getNearVertex(metric, xyz);
						r.quadtree.remove(vt);
						view.removeAllBranchGroup();
						display(view, r);
					}
				}
			};
		}
	}
}
