/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2006, by EADS CRC

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

import org.apache.log4j.Logger;
import org.jcae.mesh.java3d.Viewer;

import org.jcae.mesh.bora.xmldata.BModelReader;
import org.jcae.mesh.bora.ds.BModel;
import org.jcae.mesh.bora.ds.BCADGraphCell;
import org.jcae.mesh.bora.ds.BCADGraph;
import org.jcae.mesh.cad.CADEdge;
import org.jcae.mesh.cad.CADGeomCurve3D;
import org.jcae.mesh.cad.CADShapeBuilder;
import org.jcae.mesh.mesher.ds.SubMesh1D;
import org.jcae.mesh.mesher.ds.MNode1D;
import javax.media.j3d.Appearance;
import javax.media.j3d.IndexedLineArray;
import javax.media.j3d.PointArray;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.PointAttributes;
import javax.media.j3d.LineAttributes;

import java.io.FileInputStream;
import java.io.File;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import java.util.Iterator;

public class Bora1D
{
	private static Logger logger=Logger.getLogger(Bora1D.class);	
	
	public static BranchGroup bgNodes3D(BModel model)
	{
		BranchGroup bg = new BranchGroup();
		BCADGraphCell root = model.getGraph().getRootCell();
		// Count edges
		int nEdges = 0;
		for (Iterator it = root.uniqueShapesExplorer(BCADGraph.DIM_EDGE); it.hasNext(); )
		{
			BCADGraphCell edge = (BCADGraphCell) it.next();
			File file = new File(model.getOutputDir()+File.separator+"edges", "n"+edge.getId());
			if (file.exists())
				nEdges++;
		}
		// Count nodes and beams on each edge
		int [] nrNodes = new int[nEdges+1];
		int [] nrBeams = new int[nEdges+1];
		nrNodes[0] = 0;
		nrBeams[0] = 0;
		nEdges = 0;
		for (Iterator it = root.uniqueShapesExplorer(BCADGraph.DIM_EDGE); it.hasNext(); )
		{
			BCADGraphCell edge = (BCADGraphCell) it.next();
			File nodesfile = new File(model.getOutputDir()+File.separator+"edges", "n"+edge.getId());
			if (!nodesfile.exists())
				continue;
			nrNodes[nEdges+1] = nrNodes[nEdges] + (int) nodesfile.length() / 32;
			File beamsfile = new File(model.getOutputDir()+File.separator+"edges", "b"+edge.getId());
			if (!beamsfile.exists())
				continue;
			nrBeams[nEdges+1] = nrBeams[nEdges] + (int) beamsfile.length() / 8;
			nEdges++;
		}

		int nVertices = nrNodes[nEdges];
		int nBeams = nrBeams[nEdges];

		double [] xv = new double[3*nVertices];
		int [] beams = new int[2*nBeams];

		nEdges = 0;
		for (Iterator it = root.uniqueShapesExplorer(BCADGraph.DIM_EDGE); it.hasNext(); )
		{
			BCADGraphCell edge = (BCADGraphCell) it.next();
			try
			{
				File nodesfile = new File(model.getOutputDir()+File.separator+"edges", "n"+edge.getId());
				if (!nodesfile.exists())
					continue;
				File beamsfile = new File(model.getOutputDir()+File.separator+"edges", "b"+edge.getId());
				if (!beamsfile.exists())
					continue;

				int nr = nrNodes[nEdges+1] - nrNodes[nEdges];
				FileChannel fcN = new FileInputStream(nodesfile).getChannel();
				MappedByteBuffer bbN = fcN.map(FileChannel.MapMode.READ_ONLY, 0L, fcN.size());
				DoubleBuffer nodesBuffer = bbN.asDoubleBuffer();
				for (int i = 0; i < nr; i++)
					nodesBuffer.get();
				nodesBuffer.get(xv, 3*nrNodes[nEdges], 3*nr);
				fcN.close();

				nr = nrBeams[nEdges+1] - nrBeams[nEdges];
				FileChannel fcB = new FileInputStream(beamsfile).getChannel();
				MappedByteBuffer bbB = fcB.map(FileChannel.MapMode.READ_ONLY, 0L, fcB.size());
				IntBuffer beamsBuffer = bbB.asIntBuffer();
				beamsBuffer.get(beams, 2*nrBeams[nEdges], 2*nr);
				// Add node offset
				for (int i = 0; i < 2*nr; i++)
					beams[2*nrBeams[nEdges]+i] += nrNodes[nEdges] - 1;
				fcB.close();
				nEdges++;
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}

		IndexedLineArray s = new IndexedLineArray(nVertices,
			GeometryArray.COORDINATES,
			beams.length);
		s.setCoordinateIndices(0, beams);
		s.setCoordinates(0, xv);
		s.setCapability(GeometryArray.ALLOW_COUNT_READ);
		s.setCapability(GeometryArray.ALLOW_FORMAT_READ);
		s.setCapability(GeometryArray.ALLOW_REF_DATA_READ);
		s.setCapability(IndexedLineArray.ALLOW_COORDINATE_INDEX_READ);

		Appearance lineApp = new Appearance();
		lineApp.setLineAttributes(new LineAttributes(1,LineAttributes.PATTERN_SOLID,false));
		lineApp.setColoringAttributes(new ColoringAttributes(0,0,1,ColoringAttributes.SHADE_FLAT));
		Shape3D shapeEdges = new Shape3D(s, lineApp);
		shapeEdges.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		shapeEdges.setPickable(false);
		bg.addChild(shapeEdges);

		PointArray p = new PointArray(nVertices, PointArray.COORDINATES);
		p.setCoordinates(0, xv);

		Appearance vertApp = new Appearance();
		vertApp.setPointAttributes(new PointAttributes());
		vertApp.setColoringAttributes(new ColoringAttributes(1,0,0,ColoringAttributes.SHADE_GOURAUD));
		Shape3D shapePoint = new Shape3D(p, vertApp);
		shapePoint.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		shapePoint.setPickable(false);
		bg.addChild(shapePoint);

		return bg;
	}

	public static BranchGroup bgNodes1D(BModel model)
	{
		BranchGroup bg = new BranchGroup();
		BCADGraphCell root = model.getGraph().getRootCell();
		int nVertices = 0;
		for (Iterator it = root.uniqueShapesExplorer(BCADGraph.DIM_EDGE); it.hasNext(); )
		{
			BCADGraphCell edge = (BCADGraphCell) it.next();
			File file = new File(model.getOutputDir()+File.separator+"edges", "n"+edge.getId());
			if (!file.exists())
				continue;
			nVertices += file.length() / 32;
		}

		PointArray p = new PointArray(nVertices, PointArray.COORDINATES);
		double [] xv = new double[3*nVertices];
		double [] x1 = new double[nVertices];
		CADShapeBuilder factory = CADShapeBuilder.factory;

		int offset = 0;
		for (Iterator it = root.uniqueShapesExplorer(BCADGraph.DIM_EDGE); it.hasNext(); )
		{
			BCADGraphCell edge = (BCADGraphCell) it.next();
			try
			{
				File file = new File(model.getOutputDir()+File.separator+"edges", "n"+edge.getId());
				if (!file.exists())
					continue;
				int nr = (int) file.length() / 32;
				FileChannel fc = new FileInputStream(file).getChannel();
				MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0L, fc.size());
				DoubleBuffer nodesBuffer = bb.asDoubleBuffer();
				nodesBuffer.get(x1, 0, nr);
				CADGeomCurve3D curve = factory.newCurve3D((CADEdge) edge.getShape());
				for (int i = 0; i < nr; i++)
				{
					double [] xyz = curve.value(x1[i]);
					xv[offset+3*i]   = xyz[0];;
					xv[offset+3*i+1] = xyz[1];;
					xv[offset+3*i+2] = xyz[2];;
				}
				offset += 3*nr;
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
		p.setCoordinates(0, xv);
		Appearance vertApp = new Appearance();
		vertApp.setPointAttributes(new PointAttributes());
		vertApp.setColoringAttributes(new ColoringAttributes(1,1,1,ColoringAttributes.SHADE_GOURAUD));
		Shape3D shapePoint=new Shape3D(p, vertApp);
		shapePoint.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		shapePoint.setPickable(false);
		bg.addChild(shapePoint);
		return bg;
	}

	public static void display(Viewer view, BModel m)
	{
		view.addBranchGroup(bgNodes1D(m));
		view.setVisible(true);
		view.addBranchGroup(bgNodes3D(m));
		view.setVisible(true);
	}
	
	public static void main(String args[])
	{
		boolean visu = true;
		// final BModel model = new BModel(args[0], args[1]);
		final BModel model = BModelReader.readObject(args[0]);
		
		final Viewer view=new Viewer();
		if (visu)
		{
			display(view, model);
			view.zoomTo(); 
			/*
			view.callBack=new Runnable()
			{
				public void run()
				{
					double [] xyz = view.getLastClick();
					if (null != xyz)
					{
						Vertex vt = new Vertex(xyz[0], xyz[1]);
						r.add(vt);
						view.removeAllBranchGroup();
						display(view, r);
					}
				}
			};
			*/
		}
	}
}
