/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2004 Jerome Robert <jeromerobert@users.sourceforge.net>

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

package org.jcae.mesh.amibe.tests;

import org.apache.log4j.Logger;
import org.jcae.mesh.amibe.ds.*;
import org.jcae.mesh.amibe.util.QuadTree;
import org.jcae.mesh.amibe.util.QuadTreeProcedure;
import org.jcae.mesh.amibe.util.QuadTreeTest;
import org.jcae.view3d.Viewer;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Random;
import javax.media.j3d.Appearance;
import javax.media.j3d.TriangleArray;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.ColoringAttributes;

public class OTriangle3
{
	private static Logger logger=Logger.getLogger(OTriangle.class);	
	
	//  Dummy constructor
	
	public static void display(Viewer view, QuadTreeTest r, Mesh m)
	{
		view.addBranchGroup(bgMesh(m));
		view.show();
		view.addBranchGroup(r.bgQuadTree());
		view.show();
	}
	
	public static BranchGroup bgMesh(Mesh mesh)
	{
		BranchGroup bg = new BranchGroup();
		ArrayList list = mesh.getTriangles();
		
		int n = list.size();
		for (Iterator it = list.iterator(); it.hasNext(); )
		{
			Triangle t = (Triangle) it.next();
			if (t.vertex[0] == Vertex.outer ||
			    t.vertex[1] == Vertex.outer ||
			    t.vertex[2] == Vertex.outer)
				n--;
		}
		TriangleArray tri = new TriangleArray(3*n, TriangleArray.COORDINATES);
		tri.setCapability(QuadArray.ALLOW_FORMAT_READ);
		tri.setCapability(QuadArray.ALLOW_COUNT_READ);
		tri.setCapability(QuadArray.ALLOW_COORDINATE_READ);
		double [] xc = new double[9*n];
		int i = 0;
		for (Iterator it = list.iterator(); it.hasNext(); )
		{
			Triangle t = (Triangle) it.next();
			if (t.vertex[0] == Vertex.outer ||
			    t.vertex[1] == Vertex.outer ||
			    t.vertex[2] == Vertex.outer)
				continue;
			for (int j = 0; j < 3; j++)
			{
				Vertex v = t.vertex[j];
				double [] uv = v.getUV();
				xc[9*i+3*j]   = uv[0];
				xc[9*i+3*j+1] = uv[1];
				xc[9*i+3*j+2] = 0.0;
			}
			i++;
		}
		tri.setCoordinates(0, xc);
		Appearance app = new Appearance();
		app.setPolygonAttributes(new PolygonAttributes(PolygonAttributes.POLYGON_LINE, PolygonAttributes.CULL_NONE, 0));
		app.setColoringAttributes(new ColoringAttributes(0,1,1,ColoringAttributes.SHADE_GOURAUD));
		Shape3D shapeTri=new Shape3D(tri, app);
		shapeTri.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		bg.addChild(shapeTri);
		return bg;
	}
	
	public static void main(String args[])
	{
		boolean visu = true;
		double u, v;
		Random rand = new Random(113L);
		final QuadTreeTest r = new QuadTreeTest(0.0, 1.0, 0.0, 1.0);
		final Mesh m = new Mesh(r);
		u = rand.nextDouble();
		v = rand.nextDouble();
		Vertex v1 = new Vertex(u, v);
		u = rand.nextDouble();
		v = rand.nextDouble();
		Vertex v2 = new Vertex(u, v);
		u = rand.nextDouble();
		v = rand.nextDouble();
		Vertex v3 = new Vertex(u, v);
		m.bootstrap(v1, v2, v3);
		for (int i = 0; i < 100; i++)
		{
			u = rand.nextDouble();
			v = rand.nextDouble();
			Vertex vt = new Vertex(u, v);
			OTriangle ot = vt.getSurroundingOTriangle();
			ot.split3(vt);
			vt.addToQuadTree();
		}
		for (Iterator it = m.getTriangles().iterator(); it.hasNext(); )
		{
			Triangle t = (Triangle) it.next();
			OTriangle o1 = new OTriangle(t, 0);
			OTriangle o2 = new OTriangle();
			for (int i = 0; i < 3; i++)
			{
				o1.nextOTri();
				OTriangle.symOTri(o1, o2);
				Vertex a = o1.apex();
				Vertex f = o2.apex();
				if (f == Vertex.outer || a == Vertex.outer || o1.origin() == Vertex.outer || o2.origin() == Vertex.outer)
					continue;
				assert (o1.isDelaunay(f)) : o1+" "+a+" "+f;
				
				f = t.vertex[i];
				Triangle t2 = f.tri;
				assert (f == t2.vertex[0] || f == t2.vertex[1] || f == t2.vertex[2]) : f+" does not belong to "+t2;
			}
		}
		final Viewer view = new Viewer();
		if (visu)
		{
			view.callBack = new Runnable()
			{
				int countClick = 0;
				Vertex start = null;
				public void run()
				{
					double [] xyz = view.getLastClick();
					if (null != xyz)
					{
						Vertex vt = new Vertex(xyz[0], xyz[1]);
						Vertex vn = r.getNearestVertex(vt);
						double [] p = vn.param;
						if (countClick == 0)
						{
							start = vn;
							System.out.println("From "+start);
							countClick++;
						}
						else
						{
							System.out.println("To   "+vn);
							countClick = 0;
							try
							{
								m.forceBoundaryEdge(start, vn);
							}
							catch (Exception ex)
							{
								logger.warn(ex.getMessage());
								ex.printStackTrace();
							}
							view.removeAllBranchGroup();
							display(view, r, m);
						}
					}
				}
			};
			display(view, r, m);
			view.zoomTo(); 
			view.show();
		}
	}
}
