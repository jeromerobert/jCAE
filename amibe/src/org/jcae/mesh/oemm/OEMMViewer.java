/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2005
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

package org.jcae.mesh.oemm;

import javax.media.j3d.Appearance;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.IndexedTriangleArray;
import javax.media.j3d.TriangleArray;
import gnu.trove.TIntHashSet;

public class OEMMViewer
{
	private final static float absOffsetStep = Float.parseFloat(System.getProperty("javax.media.j3d.zFactorAbs", "20.0f"));
	private final static float relOffsetStep = Float.parseFloat(System.getProperty("javax.media.j3d.zFactorRel", "2.0f"));

	
	public static BranchGroup bgRawOEMM(RawOEMM oemm, boolean onlyLeaves)
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
	
	public static BranchGroup meshOEMM(String dir, TIntHashSet leaves)
	{
		BranchGroup bg = new BranchGroup();
		double [] coord = IndexedStorage.getMeshOEMMCoords(dir, leaves);
		TriangleArray tri = new TriangleArray(coord.length/3, TriangleArray.COORDINATES);
		tri.setCapability(TriangleArray.ALLOW_COUNT_READ);
		tri.setCapability(TriangleArray.ALLOW_FORMAT_READ);
		tri.setCapability(TriangleArray.ALLOW_REF_DATA_READ);
		tri.setCoordinates(0, coord);
		
		Appearance blackFace = new Appearance();
		PolygonAttributes pa = new PolygonAttributes(PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE, 2.0f*absOffsetStep, false, relOffsetStep);
		ColoringAttributes ca = new ColoringAttributes(0,0,0,ColoringAttributes.SHADE_FLAT);
		blackFace.setPolygonAttributes(pa);
		blackFace.setColoringAttributes(ca);
		Shape3D shapeFill = new Shape3D(tri, blackFace);
		shapeFill.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		bg.addChild(shapeFill);
		Appearance wireFrame = new Appearance();
		PolygonAttributes pa2 = new PolygonAttributes(PolygonAttributes.POLYGON_LINE, PolygonAttributes.CULL_NONE, absOffsetStep);
		ColoringAttributes ca2 = new ColoringAttributes(0,0,1,ColoringAttributes.SHADE_GOURAUD);
		wireFrame.setPolygonAttributes(pa2);
		wireFrame.setColoringAttributes(ca2);

		Shape3D shapeLine = new Shape3D(tri, wireFrame);
		shapeLine.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		bg.addChild(shapeLine);
		return bg;
	}
	
	private static IndexedTriangleArray getGeomForTriangles(float[] nodes, int [] triangles)
	{
		IndexedTriangleArray geom = new IndexedTriangleArray(
			nodes.length/3,
			TriangleArray.COORDINATES|TriangleArray.BY_REFERENCE,
			triangles.length);
		geom.setCoordinateIndices(0, triangles);
		geom.setCapability(IndexedTriangleArray.ALLOW_COUNT_READ);
		geom.setCapability(IndexedTriangleArray.ALLOW_FORMAT_READ);
		geom.setCapability(IndexedTriangleArray.ALLOW_REF_DATA_READ);
		geom.setCapability(IndexedTriangleArray.ALLOW_COORDINATE_INDEX_READ);
		geom.setCoordRefFloat(nodes);		
		return geom;
	}
	
}
