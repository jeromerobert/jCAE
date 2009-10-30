/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2006, by EADS CRC
    Copyright (C) 2007,2009, by EADS France

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

package org.jcae.mesh.bora;

import org.jcae.mesh.bora.xmldata.BModelReader;
import org.jcae.mesh.bora.ds.BModel;
import org.jcae.mesh.bora.ds.BDiscretization;
import org.jcae.mesh.bora.ds.BCADGraphCell;
import org.jcae.mesh.cad.CADShapeEnum;

import org.jcae.viewer3d.bg.ViewableBG;
import org.jcae.viewer3d.View;

import javax.media.j3d.Appearance;
import javax.media.j3d.IndexedGeometryArray;
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
import java.util.Iterator;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

public class SampleBora3D
{
	private final static float absOffsetStep = Float.parseFloat(System.getProperty("javax.media.j3d.zFactorAbs", "20.0f"));
	private final static float relOffsetStep = Float.parseFloat(System.getProperty("javax.media.j3d.zFactorRel", "2.0f"));

	private static BranchGroup [] getBranchGroups(BModel model)
	{
		BCADGraphCell root = model.getGraph().getRootCell();
		// Count faces
		int nFaces = 0;
		for (Iterator<BCADGraphCell> it = root.uniqueShapesExplorer(CADShapeEnum.SOLID); it.hasNext(); )
		{
			BCADGraphCell solid = it.next();
			if (solid.getOrientation() != 0)
			{
				if (solid.getReversed() != null)
					solid = solid.getReversed();
			}
			BDiscretization d = solid.getDiscretizations().iterator().next();
			if (null == d)
				continue;
			File nodesfile = new File(model.getOutputDir(d), "n");
			if (!nodesfile.exists())
				continue;
			nFaces++;
		}
		// Count nodes and tetra
		int [] nrNodes = new int[nFaces+1];
		int [] nrTetra = new int[nFaces+1];
		nrNodes[0] = 0;
		nrTetra[0] = 0;
		nFaces = 0;
		for (Iterator<BCADGraphCell> it = root.uniqueShapesExplorer(CADShapeEnum.SOLID); it.hasNext(); )
		{
			BCADGraphCell solid = it.next();
			if (solid.getOrientation() != 0)
			{
				if (solid.getReversed() != null)
					solid = solid.getReversed();
			}
			BDiscretization d = solid.getDiscretizations().iterator().next();
			if (null == d)
				continue;
			File nodesfile = new File(model.getOutputDir(d), "n");
			if (!nodesfile.exists())
				continue;
			nrNodes[nFaces+1] = nrNodes[nFaces] + (int) nodesfile.length() / 24;
			File triasfile = new File(model.getOutputDir(d), "f");
			if (!triasfile.exists())
				continue;
			nrTetra[nFaces+1] = nrTetra[nFaces] + (int) triasfile.length() / 16;
			nFaces++;
		}

		int nVertices = nrNodes[nFaces];
		int nTetra = nrTetra[nFaces];

		double [] xyz = new double[3*nVertices];
		int [] temp = new int[4*nTetra];
		int [] trias = new int[3*4*nTetra];

		nFaces = 0;
		for (Iterator<BCADGraphCell> it = root.uniqueShapesExplorer(CADShapeEnum.SOLID); it.hasNext(); )
		{
			BCADGraphCell solid = it.next();
			if (solid.getOrientation() != 0)
			{
				if (solid.getReversed() != null)
					solid = solid.getReversed();
			}
			BDiscretization d = solid.getDiscretizations().iterator().next();
			if (null == d)
				continue;
			try
			{
				File nodesfile = new File(model.getOutputDir(d), "n");
				if (!nodesfile.exists())
					continue;
				File triasfile = new File(model.getOutputDir(d), "f");
				if (!triasfile.exists())
					continue;

				int nr = nrNodes[nFaces+1] - nrNodes[nFaces];
				FileChannel fcN = new FileInputStream(nodesfile).getChannel();
				MappedByteBuffer bbN = fcN.map(FileChannel.MapMode.READ_ONLY, 0L, fcN.size());
				DoubleBuffer nodesBuffer = bbN.asDoubleBuffer();
				nodesBuffer.get(xyz, 3*nrNodes[nFaces], 3*nr);
				fcN.close();

				nr = nrTetra[nFaces+1] - nrTetra[nFaces];
				FileChannel fcF = new FileInputStream(triasfile).getChannel();
				MappedByteBuffer bbF = fcF.map(FileChannel.MapMode.READ_ONLY, 0L, fcF.size());
				IntBuffer triasBuffer = bbF.asIntBuffer();
				triasBuffer.get(temp, 0, 4*nr);
				// Replace tetrahedra by triangles
				int offset = 12*nrTetra[nFaces];
				for (int i = 0; i < nr; i++)
				{
					// Tetrahedron 1: [012]
					for (int j = 0; j < 3; j++)
						trias[offset+12*i+j] = temp[4*i+j] + nrNodes[nFaces] - 1;
					// Tetrahedron 2: [103]
					// Tetrahedron 3: [023]
					// Tetrahedron 4: [213]
					for (int j = 0; j < 3; j++)
						trias[offset+12*i+5+3*j] = temp[4*i+3] + nrNodes[nFaces] - 1;
					trias[offset+12*i+3] = temp[4*i+1] + nrNodes[nFaces] - 1;
					trias[offset+12*i+4] = temp[4*i] + nrNodes[nFaces] - 1;
					trias[offset+12*i+6] = temp[4*i] + nrNodes[nFaces] - 1;
					trias[offset+12*i+7] = temp[4*i+2] + nrNodes[nFaces] - 1;
					trias[offset+12*i+9] = temp[4*i+2] + nrNodes[nFaces] - 1;
					trias[offset+12*i+10] = temp[4*i+1] + nrNodes[nFaces] - 1;
				}
				fcF.close();
				nFaces++;
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}

		BranchGroup [] ret = new BranchGroup[3];
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
		l.setCapability(IndexedGeometryArray.ALLOW_COORDINATE_INDEX_READ);

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

		// 3D nodes
		++iRet;
		ret[iRet] = new BranchGroup();
		PointArray p = new PointArray(nVertices, GeometryArray.COORDINATES);
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
