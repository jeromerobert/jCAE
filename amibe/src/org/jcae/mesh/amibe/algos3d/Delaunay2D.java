/*
 * Project Info:  http://jcae.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * (C) Copyright 2013, by EADS France
 */

package org.jcae.mesh.amibe.algos3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.algos2d.Initial;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.MeshParameters;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.patch.Mesh2D;
import org.jcae.mesh.amibe.patch.Vertex2D;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.amibe.traits.TriangleTraitsBuilder;
import org.jcae.mesh.amibe.util.HashFactory;
import org.jcae.mesh.xmldata.Amibe2VTK;
import org.jcae.mesh.xmldata.MeshWriter;

// Jython script exemple to use this class
/*
#! /usr/bin/env jython
import sys
amibe_root='/home/robert/AserisFD/zebra/zebra/jcae/amibe'
sys.path.append(amibe_root+'/dist/amibe.jar')
sys.path.append('/usr/share/java/trove-3.jar')
sys.path.append(amibe_root+'/python')
from org.jcae.mesh.amibe.ds import *
from org.jcae.mesh.amibe.algos3d import *
from org.jcae.mesh.xmldata import *
m = Mesh();
d = Delaunay2D(m, Delaunay2D.Dir.Z, 1);
d.addVertex(0, 0);
d.addVertex(0, 1);
d.addVertex(1, 1);
d.addVertex(1, 0);
d.nextPolyline();
d.addVertex(0.3,0.3);
d.addVertex(0.3,0.7);
d.addVertex(0.7,0.7);
d.addVertex(0.7,0.3);
d.nextPolyline();
d.compute();
MeshWriter.writeObject3D(m, "/tmp/m.amibe", None);
Amibe2VTK("/tmp/m.amibe").write("/tmp/toto.vtp");
 */

/**
 * Wrap org.jcae.mesh.amibe.algos2d to make it work on 3D meshes
 * @author Jerome Robert
 */
public class Delaunay2D {
	private final Dir direction;
	private final int group;
	public static enum Dir {X, Y, Z};
	private List<Vertex> orderedVertices;
	private int start;
	private final Mesh mesh;
	/**
	 * Create a mesh from a a set of closed polyline.
	 * More than one polyline can be give to create holes in the created surface.
	 * The surface is aligned on a principal plane.
	 * A polyline can be given in the input mesh but in that case the polyline
	 * must be unique (no holes).
	 * @param mesh a mesh containging only one closed beam contour
	 * @param direction The direction of the plane of the surface
	 */
	public Delaunay2D(Mesh mesh, Dir direction, int group)
	{
		this.mesh = mesh;
		this.direction = direction;
		this.group = group;
	}

	/** Add a new vertex to the current polyline */
	public void addVertex(double x, double y)
	{
		if(orderedVertices == null)
			orderedVertices = new ArrayList<Vertex>(1000);
		switch(direction)
		{
		case X: orderedVertices.add(mesh.createVertex(0, x, y)); break;
		case Y: orderedVertices.add(mesh.createVertex(y, 0, x)); break;
		case Z: orderedVertices.add(mesh.createVertex(x, y, 0)); break;
		}
	}

	/** Close the current polyline */
	public void nextPolyline()
	{
		orderedVertices.add(orderedVertices.get(start));
		start = orderedVertices.size();
	}

	public void compute()
	{
		if(orderedVertices == null)
		{
			PolylineFactory polylineFactory = new PolylineFactory(mesh, -1, 0, true);
			orderedVertices = polylineFactory.get(-1).iterator().next();
			compute(orderedVertices, true);
		}
		else
			compute(orderedVertices, false);
	}

	private void compute(List<Vertex> orderedVertices, boolean closeLoop)
	{
		Vertex2D[] border = new Vertex2D[orderedVertices.size()+ (closeLoop ? 1 : 0)];
		TriangleTraitsBuilder ttb = new TriangleTraitsBuilder();
		ttb.addVirtualHalfEdge();
		MeshTraitsBuilder mtb = new MeshTraitsBuilder();
		mtb.addKdTree(2);
		mtb.add(ttb);
		Mesh2D m = new Mesh2D(mtb, new MeshParameters(), null);
		int k = 0;
		Map<Vertex2D, Vertex> v2dTov3d = HashFactory.createMap();
		Map<Vertex, Vertex2D> v3dTov2d = HashFactory.createMap();
		for(Vertex v: orderedVertices)
		{
			border[k] = v3dTov2d.get(v);
			if(border[k] == null)
			{
				double uu, vv;
				switch(direction)
				{
				case X: uu = v.getY(); vv = v.getZ(); break;
				case Y: uu = v.getZ(); vv = v.getX(); break;
				case Z: uu = v.getX(); vv = v.getY(); break;
				default:
					throw new IllegalStateException();
				}
				border[k] = (Vertex2D) m.createVertex(uu, vv);
				v2dTov3d.put(border[k], v);
				v3dTov2d.put(v, border[k]);
			}
			k++;
		}
		if(closeLoop)
			border[k] = border[0];
		new Initial(m, mtb, border, null).compute();
		Vertex[] tmp = new Vertex[3];
		for(Triangle t:m.getTriangles())
		{
			k = 0;
			if(t.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			tmp[k++] = v2dTov3d.get(t.getV0());
			tmp[k++] = v2dTov3d.get(t.getV1());
			tmp[k++] = v2dTov3d.get(t.getV2());
			Triangle t3d = mesh.createTriangle(tmp[0], tmp[1], tmp[2]);
			t3d.setGroupId(group);
			mesh.add(t3d);
			if(mesh.hasNodes())
				for(Vertex v:tmp)
					mesh.add(v);
		}
	}
}
