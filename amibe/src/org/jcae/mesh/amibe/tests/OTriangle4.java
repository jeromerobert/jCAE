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
import org.jcae.view3d.Viewer;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Random;
import javax.media.j3d.Appearance;
import javax.media.j3d.TriangleArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.ColoringAttributes;

public class OTriangle4
{
	private static Logger logger=Logger.getLogger(OTriangle.class);	
	
	//  Dummy constructor
	
	public static BranchGroup bgMesh(Mesh mesh)
	{
		BranchGroup bg=new BranchGroup();
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
		app.setColoringAttributes(new ColoringAttributes(0,1,0,ColoringAttributes.SHADE_GOURAUD));
		Shape3D shapeTri=new Shape3D(tri, app);
		shapeTri.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		bg.addChild(shapeTri);
		return bg;
	}
	
	public static void main(String args[])
	{
		boolean visu = true;
		Random rand = new Random(113L);
		Mesh m = new Mesh();
		m.initQuadTree(0.0, 1.0, 0.0, 1.0);
		Vertex v1 = new Vertex(0.3, 0.2);
		Vertex v2 = new Vertex(0.7, 0.2);
		Vertex v3 = new Vertex(0.5, 0.8);
		m.bootstrap(v1, v2, v3);
		for (int i = 0; i < 100; i++)
		{
			Vertex v = new Vertex(rand.nextDouble(), 0.2);
			OTriangle ot = v.getSurroundingOTriangle();
			ot.split3(v);
			v.addToQuadTree();
			for (Iterator it = m.getTriangles().iterator(); it.hasNext(); )
			{
				Triangle t = (Triangle) it.next();
				OTriangle o1 = new OTriangle(t, 0);
				v1 = t.vertex[0];
				v2 = t.vertex[1];
				v3 = t.vertex[2];
				if (v1 == Vertex.outer || v2 == Vertex.outer || v3 == Vertex.outer)
					continue;
				assert (v1.onLeft(v2, v3) > 0L) : t;
			}
		}
		final Viewer view = new Viewer();
		if (visu)
		{
			view.addBranchGroup(bgMesh(m));
			view.zoomTo(); 
			view.show();
		}
	}
}
