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
import javax.media.j3d.Appearance;
import javax.media.j3d.TriangleArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.ColoringAttributes;

public class OTriangle1
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
		Mesh m = new Mesh();
		m.initQuadTree(0.0, 1.0, 0.0, 1.0);
		Vertex v1 = new Vertex(0.3, 0.5);
		Vertex v2 = new Vertex(0.7, 0.5);
		Vertex v3 = new Vertex(0.5, 0.8);
		Vertex v4 = new Vertex(0.5, 0.2);
		Vertex v5 = new Vertex(0.5, 0.4);
		OTriangle ot1 = m.bootstrap(v1, v2, v3);
		ot1.symOTri();
		ot1.split3(v4);
		v4.addToQuadTree();
		m.add(ot1.getTri());
		ot1.split3(v5);
		v5.addToQuadTree();
		final Viewer view = new Viewer();
		if (visu)
		{
			view.addBranchGroup(bgMesh(m));
			view.zoomTo(); 
			view.show();
		}
	}
}
