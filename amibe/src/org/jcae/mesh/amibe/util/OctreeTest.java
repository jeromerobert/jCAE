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

package org.jcae.mesh.amibe.util;

import org.jcae.mesh.amibe.ds.Vertex;
import javax.media.j3d.Appearance;
import javax.media.j3d.QuadArray;
import javax.media.j3d.PointArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.PointAttributes;

/**
 * Utility class to write unit tests for the Octree class.
 */
public class OctreeTest extends KdTree
{
	public OctreeTest(double [] umin, double [] umax)
	{
		super (3, umin, umax);
	}
	
	public OctreeTest(double [] umin, double [] umax, int bucketsize)
	{
		super (3, umin, umax, bucketsize);
	}
	
	private class CoordProcedure implements KdTreeProcedure
	{
		public final double [] coord;
		private int index;
		public CoordProcedure(int n)
		{
			coord = new double[72*n];
		}
		public final int action(Object o, int s, final int [] i0)
		{
			int [] ii = { i0[0], i0[1], i0[2] };
			double [] p = new double[3];
			double [] p2 = new double[3];
			int2double(ii, p);
			ii[0] += s;
			int2double(ii, p2);
			double ds = p2[0] - p[0];
			double offset = 0.0;
			for (int i = 0; i < 2; i++)
			{
				//  0xy
				coord[index]   = p[0];
				coord[index+1] = p[1];
				coord[index+2] = p[2]+offset;
				index += 3;
				coord[index]   = p[0]+ds;
				coord[index+1] = p[1];
				coord[index+2] = p[2]+offset;
				index += 3;
				coord[index]   = p[0]+ds;
				coord[index+1] = p[1]+ds;
				coord[index+2] = p[2]+offset;
				index += 3;
				coord[index]   = p[0];
				coord[index+1] = p[1]+ds;
				coord[index+2] = p[2]+offset;
				index += 3;
				//  0xz
				coord[index]   = p[0];
				coord[index+1] = p[1]+offset;
				coord[index+2] = p[2];
				index += 3;
				coord[index]   = p[0];
				coord[index+1] = p[1]+offset;
				coord[index+2] = p[2]+ds;
				index += 3;
				coord[index]   = p[0]+ds;
				coord[index+1] = p[1]+offset;
				coord[index+2] = p[2]+ds;
				index += 3;
				coord[index]   = p[0]+ds;
				coord[index+1] = p[1]+offset;
				coord[index+2] = p[2];
				index += 3;
				//  0yz
				coord[index]   = p[0]+offset;
				coord[index+1] = p[1];
				coord[index+2] = p[2];
				index += 3;
				coord[index]   = p[0]+offset;
				coord[index+1] = p[1]+ds;
				coord[index+2] = p[2];
				index += 3;
				coord[index]   = p[0]+offset;
				coord[index+1] = p[1]+ds;
				coord[index+2] = p[2]+ds;
				index += 3;
				coord[index]   = p[0]+offset;
				coord[index+1] = p[1];
				coord[index+2] = p[2]+ds;
				index += 3;
				offset += ds;
			}
			return KdTreeProcedure.OK;
		}
	}
	
	/*
	private class CheckCoordProcedure implements KdTreeProcedure
	{
		public final int action(Object o, int s, final int [] i0)
		{
			Cell self = (Cell) o;
			if (!self.isLeaf())
				return 0;
			
			double [] coord = new double[6];
			int [] ii = { i0[0], i0[1], i0[2] };
			double [] p = new double[3];
			int2double(ii, p);
			coord[0] = p[0];
			coord[1] = p[1];
			coord[2] = p[2];
			ii[0] += s;
			ii[1] += s;
			ii[2] += s;
			int2double(ii, p);
			coord[3] = p[0];
			coord[4] = p[1];
			coord[5] = p[2];
			for (int i = 0, n = self.count(); i < n; i++)
			{
				Vertex v = self.getVertex(i);
				p = v.getUV();
				if (p[0] < coord[0] || p[0] > coord[3] ||
					p[1] < coord[1] || p[1] > coord[4] ||
					p[2] < coord[2] || p[2] > coord[5])
				{
					System.out.println("Vertex "+v+" not in box ("+coord[0]+","+coord[1]+","+coord[2]+") : ("+coord[3]+","+coord[4]+","+coord[5]+")");
				}
			}
			return KdTreeProcedure.OK;
		}
	}
	*/
	
	private static class CoordVertProcedure implements KdTreeProcedure
	{
		public final double [] coord;
		private int index = 0;
		public CoordVertProcedure(int n)
		{
			coord = new double[3*n];
		}
		public final int action(Object o, int s, final int [] i0)
		{
			Cell self = (Cell) o;
			if (!self.isLeaf())
				return KdTreeProcedure.OK;
			for (int i = 0, n = self.count(); i < n; i++)
			{
				Vertex v = self.getVertex(i);
				double [] param = v.getUV();
				coord[index]   = param[0];
				coord[index+1] = param[1];
				coord[index+2] = param[2];
				index += 3;
			}
			return KdTreeProcedure.OK;
		}
	}
	
	public BranchGroup bgOctree()
	{
		BranchGroup bg=new BranchGroup();
		
		CoordProcedure proc = new CoordProcedure(nCells);
		walk(proc);
		QuadArray quad = new QuadArray(24*nCells, QuadArray.COORDINATES);
		quad.setCapability(QuadArray.ALLOW_FORMAT_READ);
		quad.setCapability(QuadArray.ALLOW_COUNT_READ);
		quad.setCapability(QuadArray.ALLOW_COORDINATE_READ);
		quad.setCoordinates(0, proc.coord);
		Appearance quadApp = new Appearance();
		quadApp.setPolygonAttributes(new PolygonAttributes(PolygonAttributes.POLYGON_LINE, PolygonAttributes.CULL_NONE, 0));
		quadApp.setColoringAttributes(new ColoringAttributes(0,1,0,ColoringAttributes.SHADE_GOURAUD));
		Shape3D shapeQuad=new Shape3D(quad, quadApp);
		shapeQuad.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		bg.addChild(shapeQuad);
		return bg;
	}
	
	public BranchGroup bgVertices()
	{
		BranchGroup bg=new BranchGroup();
		int nVertices = root.count();
		CoordVertProcedure vproc = new CoordVertProcedure(nVertices);
		walk(vproc);
		PointArray p = new PointArray(nVertices, PointArray.COORDINATES);
		p.setCoordinates(0, vproc.coord);
		Appearance vertApp = new Appearance();
		vertApp.setPointAttributes(new PointAttributes());
		vertApp.setColoringAttributes(new ColoringAttributes(1,1,1,ColoringAttributes.SHADE_GOURAUD));
		Shape3D shapePoint=new Shape3D(p, vertApp);
		shapePoint.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		shapePoint.setPickable(false);
		bg.addChild(shapePoint);
		return bg;
	}
	
}
