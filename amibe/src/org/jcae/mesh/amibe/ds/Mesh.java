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

package org.jcae.mesh.amibe.ds;

import org.jcae.mesh.amibe.util.QuadTree;
import org.jcae.mesh.amibe.ds.tools.*;
import org.jcae.mesh.amibe.metrics.Metric2D;
import org.jcae.mesh.cad.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;
import org.apache.log4j.Logger;

public class Mesh
{
	private static Logger logger=Logger.getLogger(Mesh.class);
	private ArrayList triangleList = new ArrayList();
	public QuadTree quadtree = null;
	
	//  Topological face on which mesh is applied
	private CADFace face;
	
	//  The geometrical surface describing the topological face, stored for
	//  efficiebcy reason
	private CADGeomSurface surface;
	
	//  Ninimal topological edge length
	private double epsilon = 1.;
	
	//  Stack of methods to compute geometrical values
	private Stack compGeomStack = new Stack();
	
	public Mesh()
	{
		Vertex.mesh = this;
		Triangle.outer = new Triangle();
		Vertex.outer = null;
	}
	
	public Mesh(CADFace f)
	{
		Vertex.mesh = this;
		Triangle.outer = new Triangle();
		Vertex.outer = null;
		face = f;
		surface = face.getGeomSurface();
	}
	
	public Mesh(QuadTree q)
	{
		quadtree = q;
		Vertex.mesh = this;
		Triangle.outer = new Triangle();
		double [] p = q.center();
		Vertex.outer = new Vertex(p[0], p[1]);
	}
	
	public void initQuadTree(double umin, double umax, double vmin, double vmax)
	{
		quadtree = new QuadTree(umin, umax, vmin, vmax);
		quadtree.bindMesh(this);
		Vertex.outer = new Vertex((umin+umax)*0.5, (vmin+vmax)*0.5);
	}
	
	public void setGeometry(CADShape f)
	{
		face = (CADFace) f;
		surface = face.getGeomSurface();
		double [] bb = face.boundingBox();
		epsilon = Math.max(Math.sqrt(
			(bb[0] - bb[3]) * (bb[0] - bb[3]) +
			(bb[1] - bb[4]) * (bb[1] - bb[4]) +
			(bb[2] - bb[5]) * (bb[2] - bb[5])
		) / 1000.0, Metric2D.getLength() / 100.0);
	}
	
	/**
	 * Returns the topological face.
	 *
	 * @return the topological face.
	 */
	public CADShape getGeometry()
	{
		return face;
	}
	
	/**
	 * Returns the geometrical surface.
	 *
	 * @return the geometrical surface.
	 */
	public CADGeomSurface getGeomSurface()
	{
		return surface;
	}
	
	public void scaleTolerance(double scale)
	{
		epsilon *= scale;
	}
	
	public OTriangle bootstrap(Vertex v0, Vertex v1, Vertex v2)
	{
		assert quadtree != null;
		assert v0.onLeft(v1, v2) != 0L;
		if (v0.onLeft(v1, v2) < 0L)
		{
			Vertex temp = v2;
			v2 = v1;
			v1 = temp;
		}
		Triangle first = new Triangle(v0, v1, v2);
		Triangle adj0 = new Triangle(Vertex.outer, v2, v1);
		Triangle adj1 = new Triangle(Vertex.outer, v0, v2);
		Triangle adj2 = new Triangle(Vertex.outer, v1, v0);
		OTriangle ot = new OTriangle(first, 0);
		OTriangle oa0 = new OTriangle(adj0, 0);
		OTriangle oa1 = new OTriangle(adj1, 0);
		OTriangle oa2 = new OTriangle(adj2, 0);
		ot.glue(oa0);
		ot.nextOTri();
		ot.glue(oa1);
		ot.nextOTri();
		ot.glue(oa2);
		oa0.nextOTri();
		oa2.prevOTri();
		oa0.glue(oa2);
		oa0.nextOTri();
		oa1.nextOTri();
		oa0.glue(oa1);
		oa1.nextOTri();
		oa2.prevOTri();
		oa2.glue(oa1);
		
		Vertex.outer.tri = adj0;
		v0.tri = first;
		v1.tri = first;
		v2.tri = first;
		
		add(first);
		add(adj0);
		add(adj1);
		add(adj2);
		quadtree.add(v0);
		quadtree.add(v1);
		quadtree.add(v2);
		return ot;
	}
	
	public void add(Triangle t)
	{
		triangleList.add(t);
	}
	
	public ArrayList getTriangles()
	{
		return triangleList;
	}
	
	public QuadTree getQuadTree()
	{
		return quadtree;
	}
	
	public static OTriangle forceBoundaryEdge(Vertex start, Vertex end)
	{
		assert (start != end);
		Triangle t = start.tri;
		OTriangle s = new OTriangle(t, 0);
		if (s.origin() != start)
			s.nextOTri();
		if (s.origin() != start)
			s.nextOTri();
		assert s.origin() == start : ""+start+" does not belong to "+t;
		while (true)
		{
			Vertex d = s.destination();
			if (d == end)
				return s;
			else if (d != Vertex.outer && start.onLeft(end, d) > 0L)
				break;
			s.nextOTriOrigin();
		}
		while (true)
		{
			s.prevOTriOrigin();
			Vertex d = s.destination();
			if (d == end)
				return s;
			else if (d != Vertex.outer && start.onLeft(end, d) < 0L)
				break;
		}
		//  s has 'start' as its origin point, its destination point
		//  is to the right side of (start,end) and its apex is to the
		//  left side.
		while (true)
		{
			int inter = s.forceBoundaryEdge(end);
			logger.debug("Intersectionss: "+inter);
			//  s is modified by forceBoundaryEdge, it now has 'end'
			//  as its origin point, its destination point is to the
			//  right side of (end,start) and its apex is to the left
			//  side.  This algorithm can be called iteratively after
			//  exchanging 'start' and 'end', it is known to finish.
			if (s.destination() == start)
				return s;
			Vertex temp = start;
			start = end;
			end = temp;
		}
	}
	
	public void writeUNV(String file)
	{
		String cr=System.getProperty("line.separator");
		PrintWriter out;
		try {
			if (file.endsWith(".gz") || file.endsWith(".GZ"))
				out = new PrintWriter(new java.util.zip.GZIPOutputStream(new FileOutputStream(file)));
			else
				out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(file)));
			out.println("    -1"+cr+"  2411");
			HashSet nodeset = new HashSet();
			for(Iterator it=triangleList.iterator();it.hasNext();)
			{
				Triangle t = (Triangle) it.next();
				for (int i = 0; i < 3; i++)
					nodeset.add(t.vertex[i]);
			}
			
			HashMap labels = new HashMap(nodeset.size());
			double [] p;
			int count =  0;
			for(Iterator it=nodeset.iterator();it.hasNext();)
			{
				Vertex node = (Vertex) it.next();
				if (node == Vertex.outer)
					continue;
				count++;
				Integer label = new Integer(count);
				labels.put(node, label);
				out.println(label+"         1         1         1");
				p = node.getUV();
				out.println(""+p[0]+" "+p[1]+" 0.0");
			}
			out.println("    -1");
			out.println("    -1"+cr+"  2412");
			count =  0;
			for(Iterator it=triangleList.iterator();it.hasNext();)
			{
				Triangle t = (Triangle)it.next();
				if (t.vertex[0] == Vertex.outer || t.vertex[1] == Vertex.outer || t.vertex[2] == Vertex.outer)
					continue;
				count++;
				out.println(""+count+"        91         1         1         1         3");
				for (int i = 0; i < 3; i++)
				{
					Integer nodelabel =  (Integer) labels.get(t.vertex[i]);
					out.print(" "+nodelabel.intValue());
				}
				out.println("");
			}
			out.println("    -1");
			out.close();
		} catch (FileNotFoundException e)
		{
			logger.fatal(e.toString());
			e.printStackTrace();
		} catch (Exception e)
		{
			logger.fatal(e.toString());
			e.printStackTrace();
		}
	}
	
	public void pushCompGeom(int i)
	{
		if (i == 2)
			compGeomStack.push(new Calculus2D(this));
		else if (i == 3)
			compGeomStack.push(new Calculus3D(this));
		else
			throw new java.lang.IllegalArgumentException("pushCompGeom argument must be either 2 or 3, current value is: "+i);
		quadtree.clearAllMetrics();
	}
	
	public void pushCompGeom(String type)
	{
		if (type.equals("2"))
			compGeomStack.push(new Calculus2D(this));
		else if (type.equals("3"))
			compGeomStack.push(new Calculus3D(this));
		else
			throw new java.lang.IllegalArgumentException("pushCompGeom argument must be either 2 or 3, current value is: "+type);
		quadtree.clearAllMetrics();
	}
	
	public Calculus popCompGeom()
	{
		//  Metrics are always reset by pushCompGeom.
		//  Only reset them here when there is a change.
		Object ret = compGeomStack.pop();
		if (compGeomStack.size() > 0 && !ret.getClass().equals(compGeomStack.peek().getClass()))
			quadtree.clearAllMetrics();
		return (Calculus) ret;
	}
	
	public Calculus popCompGeom(int i)
	{
		Object ret = compGeomStack.pop();
		if (compGeomStack.size() > 0 && !ret.getClass().equals(compGeomStack.peek().getClass()))
			quadtree.clearAllMetrics();
		if (i == 2)
		{
			if (!(ret instanceof Calculus2D))
				throw new java.lang.RuntimeException("Internal error.  Expected value: 2, found: 3");
		}
		else if (i == 3)
		{
			if (!(ret instanceof Calculus3D))
				throw new java.lang.RuntimeException("Internal error.  Expected value: 3, found: 2");
		}
		else
			throw new java.lang.IllegalArgumentException("pushCompGeom argument must be either 2 or 3, current value is: "+i);
		return (Calculus) ret;
	}
	
	public Calculus compGeom()
	{
		return (Calculus) compGeomStack.peek();
	}
	
	/**
	 * Checks whether a topological edge is too small to be considered.
	 *
	 * @param te   the topological edge to measure.
	 * @return <code>true</code> if this edge is too small to be considered,
	 *         <code>false</code> otherwise.
	 */
	public boolean tooSmall(CADEdge te)
	{
		if (te.isDegenerated())
		{
			CADVertex [] v = te.vertices();
			return (v[0] != v[1]);
		}
		CADGeomCurve3D c3d = CADShapeBuilder.factory.newCurve3D(te);
		if (c3d == null)
			throw new java.lang.RuntimeException("Curve not defined on edge, but this edge is not degenrerated.  Something must be wrong.");
		double range [] = c3d.getRange();
		
		//  There seems to be a bug woth OpenCascade, we had a tiny
		//  edge whose length was miscomputed, so try a workaround.
		if (Math.abs(range[1] - range[0]) < 1.e-7 * Math.max(1.0, Math.max(Math.abs(range[0]), Math.abs(range[1]))))
			return true;
		
		double edgelen = c3d.length();
		if (edgelen > epsilon)
			return false;
		logger.info("Edge "+te+" is ignored because its length is too small: "+edgelen+" <= "+epsilon);
		return true;
	}
	
	public boolean isValid()
	{
		return isValid(true);
	}
	
	public boolean isValid(boolean constrained)
	{
		OTriangle ot = new OTriangle();
		for (Iterator it = triangleList.iterator(); it.hasNext(); )
		{
			Triangle t = (Triangle) it.next();
			if (t.vertex[0] == t.vertex[1] || t.vertex[1] == t.vertex[2] || t.vertex[2] == t.vertex[0])
				return false;
			if (t.vertex[0] == Vertex.outer || t.vertex[1] == Vertex.outer || t.vertex[2] == Vertex.outer)
			{
				if (!constrained)
					continue;
				ot.bind(t);
				if (!ot.hasAttributes(OTriangle.OUTER))
					return false;
			}
			else if (t.vertex[0].onLeft(t.vertex[1], t.vertex[2]) <= 0L)
				return false;
		}
		return true;
	}
	
}
