/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2005, by EADS CRC
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

package org.jcae.viewer3d;

import org.jcae.mesh.oemm.OEMM;
import org.jcae.mesh.oemm.Storage;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.VirtualHalfEdge;
import java.util.Iterator;
import java.util.Collection;
import javax.media.j3d.Appearance;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.TriangleArray;
import javax.media.j3d.IndexedLineArray;
import javax.media.j3d.IndexedGeometryArray;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.LineAttributes;
import gnu.trove.TIntHashSet;
import org.apache.log4j.Logger;

public class OEMMViewer
{
	private static Logger logger = Logger.getLogger(OEMMViewer.class);	
	private final static float absOffsetStep = Float.parseFloat(System.getProperty("javax.media.j3d.zFactorAbs", "20.0f"));
	private final static float relOffsetStep = Float.parseFloat(System.getProperty("javax.media.j3d.zFactorRel", "2.0f"));

	
	public static BranchGroup bgOEMM(OEMM oemm, boolean onlyLeaves)
	{
		BranchGroup bg=new BranchGroup();
		
		double [] coord = oemm.getCoords(onlyLeaves);
		QuadArray quad = new QuadArray(coord.length/3, QuadArray.COORDINATES);
		quad.setCapability(QuadArray.ALLOW_FORMAT_READ);
		quad.setCapability(QuadArray.ALLOW_COUNT_READ);
		quad.setCapability(QuadArray.ALLOW_COORDINATE_READ);
		quad.setCoordinates(0, coord);
		Appearance quadApp = new Appearance();
		quadApp.setPolygonAttributes(new PolygonAttributes(PolygonAttributes.POLYGON_LINE, PolygonAttributes.CULL_NONE, 0));
		quadApp.setColoringAttributes(new ColoringAttributes(0,1,0,ColoringAttributes.SHADE_GOURAUD));
		Shape3D shapeQuad=new Shape3D(quad, quadApp);
		shapeQuad.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		bg.addChild(shapeQuad);
		return bg;
	}
	
	public static BranchGroup meshOEMM(String dir)
	{
		return meshOEMM(Storage.readOEMMStructure(dir));
	}
	
	public static BranchGroup meshOEMM(OEMM oemm)
	{
		TIntHashSet leaves = new TIntHashSet();
		for (int i = 0; i < oemm.getNumberOfLeaves(); i++)
			leaves.add(i);
		return meshOEMM(oemm, leaves);
	}
	
	public static BranchGroup meshOEMM(OEMM oemm, TIntHashSet leaves)
	{
		return meshOEMM(oemm, leaves, false);
	}

	public static BranchGroup meshOEMM(OEMM oemm, TIntHashSet leaves, boolean adjacency)
	{
		Mesh mesh = Storage.loadNodes(oemm, leaves, adjacency, true);
		return meshOEMM(mesh, adjacency);
	}

	public static BranchGroup meshOEMM(Mesh mesh, boolean adjacency)
	{
		// Mesh may be empty if all vertices are connected to
		// external vertices
		BranchGroup bg = new BranchGroup();
		if (mesh.getTriangles().isEmpty())
			return bg;
		double [] coord = meshCoord(mesh);
		TriangleArray tri = new TriangleArray(coord.length/3, TriangleArray.COORDINATES);
		tri.setCapability(TriangleArray.ALLOW_COUNT_READ);
		tri.setCapability(TriangleArray.ALLOW_FORMAT_READ);
		tri.setCapability(TriangleArray.ALLOW_REF_DATA_READ);
		tri.setCoordinates(0, coord);
		
		Appearance blackFace = new Appearance();
		PolygonAttributes pa = new PolygonAttributes(PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE, 2.0f*absOffsetStep, false, relOffsetStep);
		ColoringAttributes ca = new ColoringAttributes(0,0,0,ColoringAttributes.SHADE_FLAT);
		//pa.setCullFace(PolygonAttributes.CULL_BACK);
		blackFace.setPolygonAttributes(pa);
		blackFace.setColoringAttributes(ca);
		Shape3D shapeFill = new Shape3D(tri, blackFace);
		shapeFill.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		bg.addChild(shapeFill);
		Appearance wireFrame = new Appearance();
		PolygonAttributes pa2 = new PolygonAttributes(PolygonAttributes.POLYGON_LINE, PolygonAttributes.CULL_NONE, absOffsetStep);
		ColoringAttributes ca2 = new ColoringAttributes(0,0,1,ColoringAttributes.SHADE_GOURAUD);
		//pa2.setCullFace(PolygonAttributes.CULL_BACK);
		wireFrame.setPolygonAttributes(pa2);
		wireFrame.setColoringAttributes(ca2);

		Shape3D shapeLine = new Shape3D(tri, wireFrame);
		shapeLine.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		bg.addChild(shapeLine);
		if (!adjacency)
			return bg;

		// Free edges
		int [] beams = meshFreeEdges(mesh);
		if (beams.length <= 0)
			return bg;
		IndexedLineArray geom = new IndexedLineArray(
			coord.length/3,
			GeometryArray.COORDINATES|GeometryArray.BY_REFERENCE,
			beams.length);
		geom.setCoordinateIndices(0, beams);
		geom.setCapability(GeometryArray.ALLOW_COUNT_READ);
		geom.setCapability(GeometryArray.ALLOW_FORMAT_READ);
		geom.setCapability(GeometryArray.ALLOW_REF_DATA_READ);
		geom.setCapability(IndexedGeometryArray.ALLOW_COORDINATE_INDEX_READ);
		geom.setCoordRefDouble(coord);
		Appearance freeEdgeApp = new Appearance();
		freeEdgeApp.setLineAttributes(new LineAttributes(1,LineAttributes.PATTERN_SOLID,false));
		freeEdgeApp.setColoringAttributes(new ColoringAttributes(1.0f,0.0f,0.0f,ColoringAttributes.SHADE_FLAT));
		Shape3D shapeFreeEdges = new Shape3D(geom, freeEdgeApp);
		shapeFreeEdges.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		bg.addChild(shapeFreeEdges);
		logger.info("Number of free edges: "+(beams.length/2));
		return bg;
	}

	private static final double [] meshCoord(Mesh mesh)
	{
		
		Collection triList = mesh.getTriangles();
		boolean [] isViewable = new boolean[triList.size()];
		int nrt = 0;
		int i = 0;
		for (Iterator it = triList.iterator(); it.hasNext(); )
		{
			Triangle t = (Triangle) it.next();
			isViewable[i] = isViewable(t);
			if (isViewable[i])
				nrt++;
			i++;
		}
		double [] coord = new double[9*nrt];
		i = 0;
		for (Iterator it = triList.iterator(); it.hasNext(); )
		{
			Triangle t = (Triangle) it.next();
			if (!isViewable[i])
				continue;
			for (int j = 0; j < 3; j++)
			{
				double [] xyz = t.vertex[j].getUV();
				t.vertex[j].setLabel(3*i+j);
				for (int k = 0; k < 3; k++)
					coord[9*i+3*j+k] = xyz[k];
			}
			i++;
		}
		return coord;
	}

	private static boolean isViewable(Triangle t)
	{
		return t.isReadable() && t.vertex[0].isReadable() && t.vertex[1].isReadable() && t.vertex[2].isReadable();
	}

	private static final int [] meshFreeEdges(Mesh mesh)
	{
		Collection triList = mesh.getTriangles();
		int nrt = 0;
		for (Iterator it = triList.iterator(); it.hasNext(); )
		{
			Triangle t = (Triangle) it.next();
			if (!t.isReadable())
				continue;
			AbstractHalfEdge e = t.getAbstractHalfEdge();
			for (int j = 0; j < 3; j++)
			{
				e = e.next();
				if (!e.origin().isWritable() && !e.destination().isWritable())
					continue;
				if (e.hasAttributes(VirtualHalfEdge.BOUNDARY))
					nrt++;
			}
		}
		int [] ret = new int[2*nrt];
		int i = 0;
		for (Iterator it = triList.iterator(); it.hasNext(); )
		{
			Triangle t = (Triangle) it.next();
			if (!t.isReadable())
				continue;
			AbstractHalfEdge e = t.getAbstractHalfEdge();
			for (int j = 0; j < 3; j++)
			{
				e = e.next();
				if (!e.origin().isWritable() && !e.destination().isWritable())
					continue;
				if (e.hasAttributes(VirtualHalfEdge.BOUNDARY))
				{
					ret[2*i] = e.origin().getLabel();
					ret[2*i+1] = e.destination().getLabel();
					i++;
				}
			}
		}
		return ret;
	}

}
