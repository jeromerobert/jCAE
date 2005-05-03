/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2004,2005
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

package org.jcae.mesh.amibe.ds;

import org.jcae.mesh.amibe.util.QuadTree;
import org.jcae.mesh.amibe.ds.tools.*;
import org.jcae.mesh.amibe.InitialTriangulationException;
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

/**
 * Mesh data structure.
 * A mesh is composed of triangles, edges and vertices.  There are
 * many data structures for representing meshes, and we focused on
 * the following constraints:
 * <ul>
 *   <li>Memory usage must be minimal in order to perform very large
 *       meshes.</li>
 *   <li>Mesh traversal must be cheap.</li>
 *   <li>To find a triangle surrounding a given point must also be cheap.</li>
 * </ul>
 * The selected data structure is as follows:
 * <ul>
 *   <li>A mesh is composed of a quadtree and a list of {@link Triangle}.</li>
 *   <li>The quadtree ibject is described in {@link QuadTree}.</li>
 *   <li>A triangle is composed of three {@link Vertex}, pointers to
 *       adjacent triangles, and an integer which is explained below.</li>
 *   <li>A vertex contains a pointer to one of the triangles it is connected
 *       to.</li>
 * </ul>
 * Example:
 * <pre>
 *
 *                         W1
 *                      V0 ,_______________, W0
 *                        / \      2      /
 *                       /   \           /
 *                 t2   /     \    t1   /
 *                     / 2   1 \ 0    1/
 *                    /    t    \     /
 *                   /           \   /
 *                  /      0      \ /
 *              V1 '---------------` W2
 *                        t0      V2
 * </pre>
 * <p>
 *   The <code>t</code> {@link Triangle} has the following members:
 * </p>
 * <pre>
 *       Vertex [] vertex = { V0, V1, V2 };
 *       Triangle [] adj  = { t0, t1, t2 };
 *       byte [] adjPosEdge = { ??, 0, ?? };
 * </pre>
 * <p>
 *   By convention, edges are numbered so that edge <em>i</em> is the opposite 
 *   side of vertex <em>i</em>.  An edge is then fully characterized by a
 *   triangle and a local number between 0 and 2 inclusive.  Here, edge
 *   between <tt>V0</tt> and <tt>V2</tt> can be defined by <em>(t,1)</em> or
 *   <em>(t1,0)</em>.  The <code>adjPosEdge</code> member stores local
 *   numbers for adjacent edges, so that mesh traversal becomes very cheap. 
 * </p>
 * <p>
 *   <b>Note:</b>  <code>adjPosEdge</code> contains values between 0 and 2,
 *   it can then be stored with 2 bits, and in practice the three values of
 *   a triangle are stored inside a single byte, which is part of an
 *   <code>int</code>.  (Remaining three bytes are used to store data on edges)
 * </p>
 * 
 * <p>
 *   With the {@link QuadTree} structure, it is easy to find the nearest
 *   {@link Vertex} <code>V</code> from any given point <code>V0</code>.
 *   This gives a {@link Triangle} having <code>V</code> in its vertices,
 *   and we can loop around <code>V</code> to find the {@link Triangle}
 *   containing <code>V0</code>.
 * </p>
 */

public class Mesh
{
	private static Logger logger=Logger.getLogger(Mesh.class);
	private ArrayList triangleList = new ArrayList();
	public QuadTree quadtree = null;

	//  Utilities to help debugging meshes with writeMESH
	private static final double scaleX = 1.0;
	private static final double scaleY = 1.0;
	
	//  Topological face on which mesh is applied
	private CADFace face;
	
	//  The geometrical surface describing the topological face, stored for
	//  efficiebcy reason
	private CADGeomSurface surface;
	
	//  Ninimal topological edge length
	private double epsilon = 1.;
	
	private boolean accumulateEpsilon = false;
	private double accumulatedLength = 0.0;
	
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
		setGeometry(f);
	}
	
	public Mesh(QuadTree q)
	{
		quadtree = q;
		quadtree.bindMesh(this);
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
		double diagonal = Math.sqrt(
                                (bb[0] - bb[3]) * (bb[0] - bb[3]) +
                                (bb[1] - bb[4]) * (bb[1] - bb[4]) +
                                (bb[2] - bb[5]) * (bb[2] - bb[5]));
		if (Metric2D.getLength() == 0.0)
			Metric2D.setLength(diagonal);
		Double absEpsilon = new Double(System.getProperty("org.jcae.mesh.amibe.ds.Mesh.epsilon", "-1.0"));
		epsilon = absEpsilon.doubleValue();
		if (epsilon < 0)
			epsilon = Math.max(diagonal/1000.0, Metric2D.getLength() / 100.0);
		accumulateEpsilon = System.getProperty("org.jcae.mesh.amibe.ds.Mesh.cumulativeEpsilon", "false").equals("true");
		logger.debug("Bounding box diagonal: "+diagonal);
		logger.debug("Epsilon: "+epsilon);
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
	
	public static OTriangle forceBoundaryEdge(Vertex start, Vertex end, int maxIter)
	{
		assert (start != end);
		Triangle t = start.tri;
		OTriangle s = new OTriangle(t, 0);
		if (s.origin() != start)
			s.nextOTri();
		if (s.origin() != start)
			s.nextOTri();
		assert s.origin() == start : ""+start+" does not belong to "+t;
		Vertex dest = s.destination();
		int i = 0;
		while (true)
		{
			Vertex d = s.destination();
			if (d == end)
				return s;
			else if (d != Vertex.outer && start.onLeft(end, d) > 0L)
				break;
			s.nextOTriOrigin();
			i++;
			if (s.destination() == dest || i > maxIter)
				throw new InitialTriangulationException();
		}
		s.prevOTriOrigin();
		dest = s.destination();
		i = 0;
		while (true)
		{
			Vertex d = s.destination();
			if (d == end)
				return s;
			else if (d != Vertex.outer && start.onLeft(end, d) < 0L)
				break;
			s.prevOTriOrigin();
			i++;
			if (s.destination() == dest || i > maxIter)
				throw new InitialTriangulationException();
		}
		//  s has 'start' as its origin point, its destination point
		//  is to the right side of (start,end) and its apex is to the
		//  left side.
		i = 0;
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
			i++;
			if (i > maxIter)
				throw new InitialTriangulationException();
			Vertex temp = start;
			start = end;
			end = temp;
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
	
	public void initSmallEdges()
	{
		accumulatedLength = 0.0;
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
			return false;
		CADGeomCurve3D c3d = CADShapeBuilder.factory.newCurve3D(te);
		if (c3d == null)
			throw new java.lang.RuntimeException("Curve not defined on edge, but this edge is not degenrerated.  Something must be wrong.");
		double range [] = c3d.getRange();
		
		//  There seems to be a bug with OpenCascade, we had a tiny
		//  edge whose length was miscomputed, so try a workaround.
		if (Math.abs(range[1] - range[0]) < 1.e-7 * Math.max(1.0, Math.max(Math.abs(range[0]), Math.abs(range[1]))))
			return true;
		
		double edgelen = c3d.length();
		if (edgelen + accumulatedLength > epsilon)
		{
			accumulatedLength = 0.0;
			return false;
		}
		logger.info("Edge "+te+" is ignored because its length is too small: "+edgelen+" <= "+epsilon);
		if (accumulateEpsilon)
			accumulatedLength += edgelen;
		return true;
	}
	
	/**
	 * Remove degenerted edges.
	 */
	public void removeDegeneratedEdges()
	{
		logger.debug("Removing degenerated edges");
		OTriangle ot = new OTriangle();
		HashSet removedTriangles = new HashSet();
		for (Iterator it = triangleList.iterator(); it.hasNext(); )
		{
			Triangle t = (Triangle) it.next();
			if (removedTriangles.contains(t))
				continue;
			ot.bind(t);
			for (int i = 0; i < 3; i++)
			{
				ot.nextOTri();
				if (!ot.hasAttributes(OTriangle.BOUNDARY))
					continue;
				int ref1 = ot.origin().getRef();
				int ref2 = ot.destination().getRef();
				if (ref1 != -1 && ref2 != -1 && ref1 == ref2)
				{
					logger.debug("  Collapsing "+ot);
					removedTriangles.add(ot.getTri());
					ot.symOTri();
					removedTriangles.add(ot.getTri());
					ot.collapse();
					break;
				}
			}
		}
		for (Iterator it = removedTriangles.iterator(); it.hasNext(); )
		{
			Triangle t = (Triangle) it.next();
			triangleList.remove(t);
		}
	}
	
	public boolean isValid()
	{
		return isValid(true);
	}
	
	public boolean isValid(boolean constrained)
	{
		for (Iterator it = triangleList.iterator(); it.hasNext(); )
		{
			Triangle t = (Triangle) it.next();
			if (t.vertex[0] == t.vertex[1] || t.vertex[1] == t.vertex[2] || t.vertex[2] == t.vertex[0])
				return false;
			if (t.vertex[0] == Vertex.outer || t.vertex[1] == Vertex.outer || t.vertex[2] == Vertex.outer)
			{
				if (constrained && !t.isOuter())
					return false;
			}
			else if (t.vertex[0].onLeft(t.vertex[1], t.vertex[2]) <= 0L)
				return false;
		}
		return true;
	}
	
}
