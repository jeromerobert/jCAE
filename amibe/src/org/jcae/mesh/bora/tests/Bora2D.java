/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2006, by EADS CRC
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

package org.jcae.mesh.bora.tests;

import org.jcae.mesh.bora.xmldata.BModelReader;
import org.jcae.mesh.bora.ds.BModel;
import org.jcae.mesh.bora.ds.BDiscretization;
import org.jcae.mesh.bora.ds.BCADGraphCell;
import org.jcae.mesh.cad.CADFace;
import org.jcae.mesh.cad.CADGeomSurface;
import org.jcae.mesh.cad.CADShapeEnum;
import javax.media.j3d.Appearance;
import javax.media.j3d.IndexedTriangleArray;
import javax.media.j3d.PointArray;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.PointAttributes;
import javax.media.j3d.PolygonAttributes;

import java.io.FileInputStream;
import java.io.File;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import org.jcae.viewer3d.bg.ViewableBG;
import org.jcae.viewer3d.View;

import java.util.Iterator;

public class Bora2D
{
	private final static float absOffsetStep = Float.parseFloat(System.getProperty("javax.media.j3d.zFactorAbs", "20.0f"));
	private final static float relOffsetStep = Float.parseFloat(System.getProperty("javax.media.j3d.zFactorRel", "2.0f"));

	public static BranchGroup [] getBranchGroups(BModel model)
	{
		BCADGraphCell root = model.getGraph().getRootCell();
		// Count faces
		int nFaces = 0;
		for (Iterator<BCADGraphCell> it = root.uniqueShapesExplorer(CADShapeEnum.FACE); it.hasNext(); )
		{
			BCADGraphCell face = it.next();
			if (face.getOrientation() != 0)
			{
				if (face.getReversed() != null)
					face = face.getReversed();
			}
			BDiscretization d = face.discretizationIterator().next();
			if (null == d)
				continue;
			File nodesfile = new File(model.getOutputDir(d)+File.separator+"2d", "n"+face.getId());
			if (!nodesfile.exists())
				continue;
			File parasfile = new File(model.getOutputDir(d)+File.separator+"2d", "p"+face.getId());
			if (!parasfile.exists())
				continue;
			nFaces++;
		}
		// Count nodes and trias on each face
		int [] nrNodes = new int[nFaces+1];
		int [] nrTria = new int[nFaces+1];
		nrNodes[0] = 0;
		nrTria[0] = 0;
		nFaces = 0;
		for (Iterator<BCADGraphCell> it = root.uniqueShapesExplorer(CADShapeEnum.FACE); it.hasNext(); )
		{
			BCADGraphCell face = it.next();
			if (face.getOrientation() != 0)
			{
				if (face.getReversed() != null)
					face = face.getReversed();
			}
			BDiscretization d = face.discretizationIterator().next();
			if (null == d)
				continue;
			File nodesfile = new File(model.getOutputDir(d)+File.separator+"2d", "n"+face.getId());
			if (!nodesfile.exists())
				continue;
			File parasfile = new File(model.getOutputDir(d)+File.separator+"2d", "p"+face.getId());
			if (!parasfile.exists())
				continue;
			nrNodes[nFaces+1] = nrNodes[nFaces] + (int) nodesfile.length() / 24;
			File triasfile = new File(model.getOutputDir(d)+File.separator+"2d", "f"+face.getId());
			if (!triasfile.exists())
				continue;
			nrTria[nFaces+1] = nrTria[nFaces] + (int) triasfile.length() / 12;
			nFaces++;
		}

		int nVertices = nrNodes[nFaces];
		int nTrias = nrTria[nFaces];

		double [] xyz = new double[3*nVertices];
		double [] x2 = new double[2*nVertices];
		double [] x3d2 = new double[3*nVertices];
		int [] trias = new int[3*nTrias];

		nFaces = 0;
		for (Iterator<BCADGraphCell> it = root.uniqueShapesExplorer(CADShapeEnum.FACE); it.hasNext(); )
		{
			BCADGraphCell face = it.next();
			if (face.getOrientation() != 0)
			{
				if (face.getReversed() != null)
					face = face.getReversed();
			}
			BDiscretization d = face.discretizationIterator().next();
			if (null == d)
				continue;
			CADFace F = (CADFace) face.getShape();
			try
			{
				File nodesfile = new File(model.getOutputDir(d)+File.separator+"2d", "n"+face.getId());
				if (!nodesfile.exists())
					continue;
				File parasfile = new File(model.getOutputDir(d)+File.separator+"2d", "p"+face.getId());
				if (!parasfile.exists())
					continue;
				File triasfile = new File(model.getOutputDir(d)+File.separator+"2d", "f"+face.getId());
				if (!triasfile.exists())
					continue;

				int nr = nrNodes[nFaces+1] - nrNodes[nFaces];
				FileChannel fcP = new FileInputStream(parasfile).getChannel();
				MappedByteBuffer bbP = fcP.map(FileChannel.MapMode.READ_ONLY, 0L, fcP.size());
				DoubleBuffer parasBuffer = bbP.asDoubleBuffer();
				parasBuffer.get(x2, 0, 2*nr);
				fcP.close();
				CADGeomSurface surface = F.getGeomSurface();
				for (int i = 0; i < nr; i++)
				{
					double [] x3 = surface.value(x2[2*i], x2[2*i+1]);
					x3d2[3*nrNodes[nFaces]+3*i]   = x3[0];
					x3d2[3*nrNodes[nFaces]+3*i+1] = x3[1];
					x3d2[3*nrNodes[nFaces]+3*i+2] = x3[2];
				}
				FileChannel fcN = new FileInputStream(nodesfile).getChannel();
				MappedByteBuffer bbN = fcN.map(FileChannel.MapMode.READ_ONLY, 0L, fcN.size());
				DoubleBuffer nodesBuffer = bbN.asDoubleBuffer();
				nodesBuffer.get(xyz, 3*nrNodes[nFaces], 3*nr);
				fcN.close();

				nr = nrTria[nFaces+1] - nrTria[nFaces];
				FileChannel fcF = new FileInputStream(triasfile).getChannel();
				MappedByteBuffer bbF = fcF.map(FileChannel.MapMode.READ_ONLY, 0L, fcF.size());
				IntBuffer triasBuffer = bbF.asIntBuffer();
				triasBuffer.get(trias, 3*nrTria[nFaces], 3*nr);
				// Add node offset
				for (int i = 0; i < 3*nr; i++)
					trias[3*nrTria[nFaces]+i] += nrNodes[nFaces] - 1;
				fcF.close();
				nFaces++;
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}

		BranchGroup [] ret = new BranchGroup[4];
		PointAttributes pa = new PointAttributes();
		pa.setPointSize(4.0f);
		int iRet = -1;

		// 3D edges
		++iRet;
		ret[iRet] = new BranchGroup();
		IndexedTriangleArray l = new IndexedTriangleArray(nVertices,
			GeometryArray.COORDINATES,
			trias.length);
		l.setCoordinateIndices(0, trias);
		l.setCoordinates(0, xyz);
		l.setCapability(GeometryArray.ALLOW_COUNT_READ);
		l.setCapability(GeometryArray.ALLOW_FORMAT_READ);
		l.setCapability(GeometryArray.ALLOW_REF_DATA_READ);
		l.setCapability(IndexedTriangleArray.ALLOW_COORDINATE_INDEX_READ);

		Appearance triaApp = new Appearance();
		triaApp.setPolygonAttributes(new PolygonAttributes(PolygonAttributes.POLYGON_LINE, PolygonAttributes.CULL_NONE, 0));
		triaApp.setColoringAttributes(new ColoringAttributes(1f,0f,0f,ColoringAttributes.SHADE_FLAT));

		Shape3D shapeTrias = new Shape3D(l, triaApp);
		shapeTrias.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		shapeTrias.setPickable(false);
		ret[iRet].addChild(shapeTrias);

		// Black faces
		++iRet;
		ret[iRet] = new BranchGroup();
		Appearance htriApp = new Appearance();
		htriApp.setPolygonAttributes(new PolygonAttributes(PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE, 2.0f*absOffsetStep, false, relOffsetStep));
		htriApp.setColoringAttributes(new ColoringAttributes(0.1f,0.1f,0.3f,ColoringAttributes.SHADE_FLAT));

		Shape3D hiddenTrias = new Shape3D(l, htriApp);
		hiddenTrias.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		hiddenTrias.setPickable(false);
		ret[iRet].addChild(hiddenTrias);

		// 2D nodes
		++iRet;
		ret[iRet] = new BranchGroup();
		PointArray p2 = new PointArray(nVertices, PointArray.COORDINATES);
		p2.setCoordinates(0, x3d2);
		Appearance vert2App = new Appearance();
		vert2App.setPointAttributes(pa);
		vert2App.setColoringAttributes(new ColoringAttributes(0,1,0,ColoringAttributes.SHADE_GOURAUD));
		Shape3D shapePoint2=new Shape3D(p2, vert2App);
		shapePoint2.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		shapePoint2.setPickable(false);
		ret[iRet].addChild(shapePoint2);

		// 3D nodes
		++iRet;
		ret[iRet] = new BranchGroup();
		PointArray p = new PointArray(nVertices, PointArray.COORDINATES);
		p.setCoordinates(0, xyz);

		Appearance vertApp = new Appearance();
		vertApp.setPointAttributes(pa);
		vertApp.setColoringAttributes(new ColoringAttributes(1f,1f,0f,ColoringAttributes.SHADE_GOURAUD));
		Shape3D shapePoint = new Shape3D(p, vertApp);
		shapePoint.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		shapePoint.setPickable(false);
		ret[iRet].addChild(shapePoint);

		return ret;
	}

	public static void main(String args[])
	{
		final BModel model = BModelReader.readObject(args[0]);
		JFrame feFrame = new JFrame("Bora Demo");
		final View view = new View(feFrame);
		feFrame.setSize(800,600);
		feFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		final BranchGroup [] bgList = getBranchGroups(model);
		final ViewableBG [] viewList = new ViewableBG[bgList.length];
		// bgList:
		//   0: triangles
		//   1: triangles without hidden faces
		//   2: 2D nodes on the 3D surface
		//   3: 3D nodes
		final boolean [] active = new boolean[bgList.length];
		for (int i = 0; i < bgList.length; i++)
		{
			active[i] = true;
			viewList[i] = new ViewableBG(bgList[i]);
		}
		for (int i = 0; i < bgList.length; i++)
			if (active[i])
				view.add(viewList[i]);
		view.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event)
			{
				char k = event.getKeyChar();
				if ('2' == k)
					active[2] = !active[2];
				else if ('3' == k)
					active[3] = !active[3];
				else if ('e' == k)
					active[0] = !active[0];
				else if ('f' == k)
					active[1] = !active[1];
				else if ('q' == k)
					System.exit(0);
				else
					return;
				for (int i = 0; i < bgList.length; i++)
				{
					view.remove(viewList[i]);
					if (active[i])
						view.add(viewList[i]);
				}
			}
		});
		view.fitAll(); 
		feFrame.getContentPane().add(view);
		feFrame.setVisible(true);
	}
}
