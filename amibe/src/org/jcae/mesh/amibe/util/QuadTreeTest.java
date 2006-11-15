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

import org.apache.log4j.Logger;
import org.jcae.mesh.amibe.ds.Vertex;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.zip.GZIPOutputStream;
import javax.media.j3d.Appearance;
import javax.media.j3d.QuadArray;
import javax.media.j3d.LineArray;
import javax.media.j3d.PointArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.PointAttributes;

/**
 * Utility class to write unit tests for the QuadTree class.
 */
public class QuadTreeTest extends QuadTree
{
	private static Logger logger=Logger.getLogger(QuadTreeTest.class);	
	
	public QuadTreeTest(double umin, double umax, double vmin, double vmax)
	{
		super (umin, umax, vmin, vmax);
	}
	
	private final class CoordProcedure implements QuadTreeProcedure
	{
		public final double [] coord;
		private int index;
		public CoordProcedure(int n)
		{
			coord = new double[8*n];
		}
		public final int action(Object o, int s, int i0, int j0)
		{
			int [] ii = { i0, j0 };
			double [] p = new double[2];
			int2double(ii, p);
			coord[index]   = p[0];
			coord[index+1] = p[1];
			index += 2;
			ii[0] += s;
			int2double(ii, p);
			coord[index]   = p[0];
			coord[index+1] = p[1];
			index += 2;
			ii[1] += s;
			int2double(ii, p);
			coord[index]   = p[0];
			coord[index+1] = p[1];
			index += 2;
			ii[0] -= s;
			int2double(ii, p);
			coord[index]   = p[0];
			coord[index+1] = p[1];
			index += 2;
			return 0;
		}
	}
	
	private final class CheckCoordProcedure implements QuadTreeProcedure
	{
		public final int action(Object o, int s, int i0, int j0)
		{
			Cell self = (Cell) o;
			if (self.nItems < 0)
				return 0;
			
			double [] coord = new double[4];
			int [] ii = { i0, j0 };
			double [] p = new double[2];
			int2double(ii, p);
			coord[0] = p[0];
			coord[1] = p[1];
			ii[0] += s;
			ii[1] += s;
			int2double(ii, p);
			coord[2] = p[0];
			coord[3] = p[1];
			for (int i = 0; i < self.nItems; i++)
			{
				Vertex v = (Vertex) self.subQuad[i];
				p = v.getUV();
				if (p[0] < coord[0] || p[0] > coord[2] ||
					p[1] < coord[1] || p[1] > coord[3])
				{
					System.out.println("Vertex "+v+" not in box ("+coord[0]+","+coord[1]+") : ("+coord[2]+","+coord[3]+"_");
				}
			}
			return 0;
		}
	}
	
	private final class CoordVertProcedure implements QuadTreeProcedure
	{
		public final double [] coord;
		private int index = 0;
		public CoordVertProcedure(int n)
		{
			coord = new double[2*n];
		}
		public final int action(Object o, int s, int i0, int j0)
		{
			Cell self = (Cell) o;
			if (self.nItems < 0)
				return 0;
			for (int i = 0; i < self.nItems; i++)
			{
				Vertex v = (Vertex) self.subQuad[i];
				double [] param = v.getUV();
				coord[index] = param[0];
				coord[index+1] = param[1];
				index += 2;
			}
			return 0;
		}
	}
	
	public void writeUNV(String file)
	{
		CoordProcedure proc = new CoordProcedure(nCells);
		walk(proc);
		String cr=System.getProperty("line.separator");
		PrintWriter out;
		try {
			if (file.endsWith(".gz") || file.endsWith(".GZ"))
				out = new PrintWriter(new GZIPOutputStream(new FileOutputStream(file)));
			else
				out = new PrintWriter(new FileOutputStream(file));
			out.println("    -1"+cr+"  2411");
			for(int i = 0; i < 4*nCells; i++)
			{
				out.println((i+1)+"         1         1         1");
				out.println(""+proc.coord[2*i]+" "+proc.coord[2*i+1]+" "+0.0);
			}
			out.println("    -1");
			out.println("    -1"+cr+"  2412");
			for(int i = 0; i < nCells; i++)
			{
				out.println(""+(i+1)+"        91         1         1         1         4");
				for(int j = 0; j < 4; j++)
					out.print(" "+(4*i+j+1));
				out.println("");
			}
			out.println("    -1");
			out.close();
		} catch (FileNotFoundException e)
		{
			logger.fatal(e.toString());
			e.printStackTrace();
		} catch (IOException e)
		{
			logger.fatal(e.toString());
			e.printStackTrace();
		}
	}
	
	public BranchGroup bgQuadTree()
	{
		BranchGroup bg=new BranchGroup();
		
		CoordProcedure proc = new CoordProcedure(nCells);
		walk(proc);
		QuadArray quad = new QuadArray(4*nCells, QuadArray.COORDINATES);
		quad.setCapability(QuadArray.ALLOW_FORMAT_READ);
		quad.setCapability(QuadArray.ALLOW_COUNT_READ);
		quad.setCapability(QuadArray.ALLOW_COORDINATE_READ);
		double [] xc = new double[12*nCells];
		for (int i = 0; i < 4*nCells; i++)
		{
			xc[3*i]   = proc.coord[2*i];
			xc[3*i+1] = proc.coord[2*i+1];
			xc[3*i+2] = 0.0;
		}
		quad.setCoordinates(0, xc);
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
		int nVertices = Math.abs(root.nItems);
		CoordVertProcedure vproc = new CoordVertProcedure(nVertices);
		walk(vproc);
		PointArray p = new PointArray(nVertices, PointArray.COORDINATES);
		double [] xv = new double[3*nVertices];
		for (int i = 0; i < nVertices; i++)
		{
			xv[3*i] = vproc.coord[2*i];
			xv[3*i+1] = vproc.coord[2*i+1];
			xv[3*i+2] = 0.0;
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
	
	public BranchGroup segment(Vertex vt, Vertex vn, float depth, float r, float g, float b)
	{
		BranchGroup bg = new BranchGroup();
		LineArray line = new LineArray(2, LineArray.COORDINATES | LineArray.COLOR_3);
		double [] xcoord = new double[6];
		double [] pvt = vt.getUV();
		double [] pvn = vn.getUV();
		for (int i = 0; i < 2; i++)
		{
			xcoord[i]   = pvt[i];
			xcoord[i+3] = pvn[i];
		}
		line.setCoordinates(0, xcoord);
		float [] xcolor = { 1.0f, 1.0f, 0.0f, r, g, b };
		line.setColors(0, xcolor);
		Appearance lineApp = new Appearance();
		lineApp.setPolygonAttributes(new PolygonAttributes(PolygonAttributes.POLYGON_LINE, PolygonAttributes.CULL_NONE, depth));
		Shape3D shapeLine=new Shape3D(line, lineApp);
		shapeLine.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		shapeLine.setPickable(false);
		bg.addChild(shapeLine);
		return bg;
	}
}
