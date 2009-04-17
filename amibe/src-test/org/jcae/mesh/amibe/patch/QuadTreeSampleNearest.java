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

import org.jcae.mesh.amibe.util.KdTree;

import java.util.Random;
import java.util.logging.Logger;

/**
 * Unit test to check detection of near vertices.
 * Run
 * <pre>
 *   QuadTreeSampleNearest
 * </pre>
 * to display an initial <code>QuadTree</code> with 500 vertices.
 * When clicking at a point, a yellow segment is displayed between this point
 * and the nearest point found in the same cell, returned by
 * {@link org.jcae.mesh.amibe.util.KdTree#getNearVertex(Mesh, Vertex)}.
 * If {@link org.jcae.mesh.amibe.util.KdTree#getNearestVertex(Mesh, Vertex)}
 * finds a nearest point, a blue segment is displayed.
 */
public class QuadTreeSampleNearest extends QuadTreeSample
{
	private static Logger logger=Logger.getLogger(QuadTreeSampleNearest.class.getName());	
	
	public QuadTreeSampleNearest(KdTree q)
	{
		super (q);
	}
	
	public Vertex2D getNearVertex(Mesh2D mesh, Vertex2D n)
	{
		return (Vertex2D) quadtree.getNearVertex(mesh, n);
	}

	public Vertex2D getNearestVertex(Mesh2D mesh, Vertex2D n)
	{
		return (Vertex2D) quadtree.getNearestVertex(mesh, n);
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
		final QuadTreeSampleNearest r = new QuadTreeSampleNearest(m.getKdTree());
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
			view.addBranchGroup(r.bgQuadTree());
			view.zoomTo(); 
			view.setVisible(true);
			view.addBranchGroup(r.bgVertices());
			view.setVisible(true);
		}
		
		for (int i = 0; i < 10; i++)
		{
			u = rand.nextDouble();
			v = rand.nextDouble();
			Vertex2D vt = (Vertex2D) m.createVertex(u, v);
			if (visu)
			{
				view.addBranchGroup(r.segment(vt, r.getNearVertex(m, vt), 5.0f, 1, 1, 0));
				view.setVisible(true);
				view.addBranchGroup(r.segment(vt, r.getNearestVertex(m, vt), 0.0f, 0, 1, 1));
				view.setVisible(true);
			}
			else
			{
				r.getNearVertex(m, vt);
				r.getNearestVertex(m, vt);
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
						Vertex2D vt = (Vertex2D) m.createVertex(xyz[0], xyz[1]);
						view.addBranchGroup(r.segment(vt, r.getNearVertex(m, vt), 5.0f, 1, 1, 0));
						view.setVisible(true);
						view.addBranchGroup(r.segment(vt, r.getNearestVertex(m, vt), 0.0f, 0, 1, 1));
						view.setVisible(true);
					}
				}
			};
			view.setVisible(true);
		}
	}
}
