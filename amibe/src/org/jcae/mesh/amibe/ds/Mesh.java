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
import org.jcae.mesh.amibe.metrics.Metric3D;
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
 *                      V0 +---------------. W0
 *                        / \      2      /
 *                       /   \           /
 *                 t2   /     \    t1   /
 *                     / 2   1 \ 0    1/
 *                    /    t    \     /
 *                   /           \   /
 *                  /      0      \ /
 *              V1 '---------------+ W2
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
	
	//  Triangle list
	private ArrayList triangleList = new ArrayList();

	//  Topological face on which mesh is applied
	private CADFace face;
	
	//  The geometrical surface describing the topological face, stored for
	//  efficiebcy reason
	private CADGeomSurface surface;
	
	//  Minimal topological edge length
	private double epsilon = 1.;
	
	private boolean accumulateEpsilon = false;
	
	//  Utilities to help debugging meshes with writeMESH
	private static final double scaleX = 1.0;
	private static final double scaleY = 1.0;
	
	//  Stack of methods to compute geometrical values
	private Stack compGeomStack = new Stack();
	
	//  2D/3D
	public static final int MESH_2D = 2;
	public static final int MESH_3D = 3;
	public static final int MESH_OEMM = 4;
	private int type = MESH_2D;
	
	/**
	 * Structure to fasten search of nearest vertices.
	 */
	public QuadTree quadtree = null;
	
	/**
	 * Creates an empty mesh.
	 */
	public Mesh()
	{
		Vertex.mesh = this;
		Triangle.outer = new Triangle();
		Vertex.outer = new Vertex(0.0, 0.0);
	}
	
	/**
	 * Creates an empty mesh bounded to the topological surface.
	 * This constructor also initializes tolerance values.  If length
	 * criterion is null, {@link Metric2D#setLength} is called with
	 * the diagonal length of face bounding box as argument.
	 * If property <code>org.jcae.mesh.amibe.ds.Mesh.epsilon</code> is
	 * not set, epsilon is computed as being the maximal value between
	 * length criterion by 100 and diagonal length by 1000.
	 *
	 * @param f   topological surface
	 */
	public Mesh(CADFace f)
	{
		Vertex.mesh = this;
		Triangle.outer = new Triangle();
		Vertex.outer = null;
		face = f;
		surface = face.getGeomSurface();
		double [] bb = face.boundingBox();
		double diagonal = Math.sqrt(
                                (bb[0] - bb[3]) * (bb[0] - bb[3]) +
                                (bb[1] - bb[4]) * (bb[1] - bb[4]) +
                                (bb[2] - bb[5]) * (bb[2] - bb[5]));
		if (Metric2D.getLength() == 0.0)
			Metric2D.setLength(diagonal);
		String absEpsilonProp = System.getProperty("org.jcae.mesh.amibe.ds.Mesh.epsilon");
		if (absEpsilonProp == null)
		{
			absEpsilonProp = "-1.0";
			System.setProperty("org.jcae.mesh.amibe.ds.Mesh.epsilon", absEpsilonProp);
		}
		Double absEpsilon = new Double(absEpsilonProp);
		epsilon = absEpsilon.doubleValue();
		if (epsilon < 0)
			epsilon = Math.max(diagonal/1000.0, Metric2D.getLength() / 100.0);
		String accumulateEpsilonProp = System.getProperty("org.jcae.mesh.amibe.ds.Mesh.cumulativeEpsilon");
		if (accumulateEpsilonProp == null)
		{
			accumulateEpsilonProp = "false";
			System.setProperty("org.jcae.mesh.amibe.ds.Mesh.cumulativeEpsilon", accumulateEpsilonProp);
		}
		accumulateEpsilon = accumulateEpsilonProp.equals("true");
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
	
	/**
	 * Returns the mesh type.
	 *
	 * @return the mesh type.
	 */
	public int getType()
	{
		return type;
	}
	
	/**
	 * Sets the mesh type.
	 *
	 * @param t mesh type
	 */
	public void setType(int t)
	{
		type = t;
	}
	
	/**
	 * Initialize a QuadTree with a given bounding box.
	 * @param umin  bottom-left U coordinate.
	 * @param umax  top-right U coordinate.
	 * @param vmin  bottom-left V coordinate.
	 * @param vmax  top-right V coordinate.
	 */
	public void initQuadTree(double umin, double umax, double vmin, double vmax)
	{
		quadtree = new QuadTree(umin, umax, vmin, vmax);
		quadtree.bindMesh(this);
		Vertex.outer = new Vertex((umin+umax)*0.5, (vmin+vmax)*0.5);
	}
	
	/**
	 * Returns the quadtree associated with this mesh.
	 *
	 * @return the quadtree associated with this mesh.
	 */
	public QuadTree getQuadTree()
	{
		return quadtree;
	}
	
	public void scaleTolerance(double scale)
	{
		epsilon *= scale;
	}
	
	/**
	 * Bootstraps node instertion by creating the first triangle.
	 * This initial triangle is counter-clockwise oriented, and
	 * outer triangles are constructed.
	 *
	 * @param v0  first vertex.
	 * @param v1  second vertex.
	 * @param v2  third vertex.
	 */
	public void bootstrap(Vertex v0, Vertex v1, Vertex v2)
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
		if (Vertex.outer == null)
			Vertex.outer = new Vertex(0.0, 0.0);
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
		
		Vertex.outer.setLink(adj0);
		v0.setLink(first);
		v1.setLink(first);
		v2.setLink(first);
		
		add(first);
		add(adj0);
		add(adj1);
		add(adj2);
		quadtree.add(v0);
		quadtree.add(v1);
		quadtree.add(v2);
	}
	
	/**
	 * Adds an existing triangle to triangle list.
	 * This routine is useful when meshes are read from disk but not
	 * computed by node insertion.
	 *
	 * @param t  triangle being added.
	 */
	public void add(Triangle t)
	{
		triangleList.add(t);
	}
	
	/**
	 * Removes a triangle from triangle list.
	 *
	 * @param t  triangle being removed.
	 */
	public void remove(Triangle t)
	{
		triangleList.remove(t);
	}
	
	/**
	 * Returns triangle list.
	 *
	 * @return triangle list.
	 */
	public ArrayList getTriangles()
	{
		return triangleList;
	}
	
	/**
	 * Sets triangle list.
	 *
	 * @param l triangle list
	 */
	public void setTrianglesList(ArrayList l)
	{
		triangleList = l;
	}
	
	/**
	 * Enforces an edge between tow points.
	 * This routine is used to build constrained Delaunay meshes.
	 * Intersections between existing mesh segments and the new
	 * segment are computed, then edges are swapped so that the
	 * new edge is part of the mesh.
	 *
	 * @param start    start point.
	 * @param end      end point.
	 * @param maxIter  maximal number of iterations.
	 * @return a handle to the newly created edge.
	 * @throws InitialTriangulationException  if the boundary edge cannot
	 *         be enforced.
	 */
	public static OTriangle2D forceBoundaryEdge(Vertex start, Vertex end, int maxIter)
		throws InitialTriangulationException
	{
		assert (start != end);
		Triangle t = (Triangle) start.getLink();
		OTriangle2D s = new OTriangle2D(t, 0);
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
	
	/**
	 * Sets metrics dimension.
	 * Metrics operations can be performed either on 2D or 3D Euclidien
	 * spaces.  The latter is the normal case, but the former can
	 * also be used, e.g. when retrieving boundary edges of a
	 * constrained mesh.  Argument is either 2 or 3, other values
	 *
	 * @param i  metrics dimension.
	 * @throws IllegalArgumentException  If argument is neither 2 nor 3,
	 *         this exception is raised.
	 */
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
	
	/**
	 * Resets metrics dimension.
	 *
	 * @return metrics dimension.
	 * @throws IllegalArgumentException  If argument is neither 2 nor 3,
	 *         this exception is raised.
	 */
	public Calculus popCompGeom()
	{
		//  Metrics are always reset by pushCompGeom.
		//  Only reset them here when there is a change.
		Object ret = compGeomStack.pop();
		if (compGeomStack.size() > 0 && !ret.getClass().equals(compGeomStack.peek().getClass()))
			quadtree.clearAllMetrics();
		return (Calculus) ret;
	}
	
	/**
	 * Resets metrics dimension.
	 * Checks that the found metrics dimension is identical to the one
	 * expected.
	 *
	 * @param i  expected metrics dimension.
	 * @return metrics dimension.
	 * @throws RuntimeException  If argument is different from
	 *         metrics dimension.
	 */
	public Calculus popCompGeom(int i)
		throws RuntimeException
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
	
	/**
	 * Returns metrics dimension.
	 *
	 * @return metrics dimension.
	 */
	public Calculus compGeom()
	{
		return (Calculus) compGeomStack.peek();
	}
	
	/**
	 * Checks whether a length is llower than a threshold.
	 *
	 * @param len   the length to be checked.
	 * @return <code>true</code> if this length is too small to be considered,
	 *         <code>false</code> otherwise.
	 */
	public boolean tooSmall(double len, double accumulatedLength)
	{
		if (accumulateEpsilon)
			len += accumulatedLength;
		return (len < epsilon);
	}
	
	/**
	 * Remove degenerted edges.
	 * Degenerated wdges are present in 2D mesh, and have to be
	 * removed in the 2D -&gt; 3D transformation.  Triangles and
	 * vertices must then be updated too.
	 */
	public void removeDegeneratedEdges()
	{
		logger.debug("Removing degenerated edges");
		OTriangle2D ot = new OTriangle2D();
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
				if (ref1 != 0 && ref2 != 0 && ref1 == ref2)
				{
					logger.debug("  Collapsing "+ot);
					removedTriangles.add(ot.getTri());
					ot.removeDegenerated();
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
	
	/**
	 * Build adjacency relations between triangles
	 */
	public void buildAdjacency(Vertex [] vertices, double minAngle)
	{
		//  1. For each vertex, build the list of triangles
		//     connected to this vertex.
		logger.debug("Build the list of triangles connected to each vertex");
		HashMap tVertList = new HashMap(vertices.length);
		for (int i = 0; i < vertices.length; i++)
			tVertList.put(vertices[i], new ArrayList(10));
		for (Iterator it = triangleList.iterator(); it.hasNext(); )
		{
			Triangle t = (Triangle) it.next();
			for (int i = 0; i < 3; i++)
			{
				ArrayList list = (ArrayList) tVertList.get(t.vertex[i]);
				list.add(t);
			}
		}
		//  2. For each pair of vertices, count adjacent triangles.
		//     If there is only one adjacent triangle, this edge
		//     is tagged as a boundary.
		//     If there are 2 adjacent triangles, they are connected
		//     together.  If there are more than 2 adjacent triangles,
		//     this edge is non manifold.
		logger.debug("Connect triangles");
		for (int i = 0; i < vertices.length; i++)
			checkNeighbours(vertices[i], tVertList);
		
		//  3. Find the list of vertices which are on mesh boundary
		OTriangle ot = new OTriangle();
		logger.debug("Build the list of boundary nodes");
		HashSet bndNodes = new HashSet();
		int maxLabel = 0;
		boolean [] found = new boolean[4];
		for (Iterator it = triangleList.iterator(); it.hasNext(); )
		{
			Triangle t = (Triangle) it.next();
			ot.bind(t);
			for (int i = 0; i < found.length; i++)
				found[i] = false;
			for (int i = 0; i < 3; i++)
			{
				ot.nextOTri();
				if (ot.hasAttributes(OTriangle.BOUNDARY))
				{
					found[i] = true;
					found[i+1] = true;
				}
			}
			found[0] |= found[3];
			for (int i = 0; i < 3; i++)
			{
				ot.nextOTri();
				if (found[i])
				{
					bndNodes.add(ot.origin());
					maxLabel = Math.max(maxLabel, ot.origin().getRef());
				}
			}
		}
		//  4. Build links from non-manifold vertices
		logger.debug("Compute links for non-manifold vertices");
		Vertex [] v = new Vertex[2];
		for (Iterator it = triangleList.iterator(); it.hasNext(); )
		{
			Triangle t = (Triangle) it.next();
			ot.bind(t);
			for (int i = 0; i < 3; i++)
			{
				ot.nextOTri();
				if (ot.hasAttributes(OTriangle.BOUNDARY))
				{
					v[0] = ot.origin();
					v[1] = ot.destination();
					for (int j = 0; j < 2; j++)
					{
						//if (v[j].getLink() instanceof Triangle)
						//{
						//	Triangle [] adj = ot.getAdj();
						//}
					}
				}
			}
			found[0] |= found[3];
			for (int i = 0; i < 3; i++)
			{
				ot.nextOTri();
				if (found[i])
				{
					bndNodes.add(ot.origin());
					maxLabel = Math.max(maxLabel, ot.origin().getRef());
				}
			}
		}
		//  4. If vertices are on inner boundaries and there is
		//     no ridge, change their label.
		logger.debug("Set interior nodes mutable");
		int nrJunctionPoints = 0;
		double cosMinAngle = Math.cos(Math.PI*minAngle/180.0);
		if (minAngle < 0.0)
			cosMinAngle = -2.0;
		for (int i = 0; i < vertices.length; i++)
		{
			if (bndNodes.contains(vertices[i]))
				continue;
			int label = vertices[i].getRef();
			int nrVertNeigh = vertices[i].getNeighboursNodes().size();
			int nrTriNeigh = ((ArrayList) tVertList.get(vertices[i])).size();
			if (nrVertNeigh != nrTriNeigh)
			{
				nrJunctionPoints++;
				if (label == 0)
				{
					maxLabel++;
					vertices[i].setRef(maxLabel);
				}
				continue;
			}
			if (0 != label)
			{
				//  Check for ridges
				ot.bind((Triangle) vertices[i].getLink());
				if (minAngle < 0.0 || checkRidges(vertices[i], cosMinAngle, ot))
					vertices[i].setRef(-label);
			}
		}
		if (nrJunctionPoints > 0)
			logger.info("Found "+nrJunctionPoints+" saddle points");
	}
	
	private static final void checkNeighbours(Vertex v, HashMap tVertList)
	{
		OTriangle ot = new OTriangle();
		OTriangle sym = new OTriangle();
		OTriangle ot2 = new OTriangle();
		ArrayList newTriangles = new ArrayList();
		boolean nonManifold = false;
		//  Mark adjacent triangles
		ArrayList list = (ArrayList) tVertList.get(v);
		for (Iterator it = list.iterator(); it.hasNext(); )
		{
			Triangle t = (Triangle) it.next();
			t.mark();
		}
		//  Find all adjacent triangles
		for (Iterator it = list.iterator(); it.hasNext(); )
		{
			Triangle t = (Triangle) it.next();
			ot.bind(t);
			if (ot.destination() == v)
				ot.nextOTri();
			else if (ot.apex() == v)
				ot.prevOTri();
			assert ot.origin() == v;
			if (ot.getAdj() != null)
				continue;
			Vertex v2 = ot.destination();
			ArrayList list2 = (ArrayList) tVertList.get(v2);
			int cnt = 0;
			for (Iterator it2 = list2.iterator(); it2.hasNext(); )
			{
				Triangle t2 = (Triangle) it2.next();
				if (t == t2)
					continue;
				if (t2.isMarked())
				{
					ot2.bind(t2);
					if (ot2.destination() == v2)
						ot2.nextOTri();
					else if (ot2.apex() == v2)
						ot2.prevOTri();
					if (ot2.destination() != v)
					{
						ot2.prevOTri();
						assert ot2.origin() == v;
						assert ot2.destination() == v2;
					}
					if (cnt == 0)
					{
						ot.glue(ot2);
					}
					else
					{
						//  This edge is non manifold.  Find ot2 with endpoints
						//  being v and v2.
						//  Collect all adjacent triangles into an ArrayList.
						ArrayList adj = null;
						if (cnt == 1)
						{
							OTriangle.symOTri(ot, sym);
							adj = new ArrayList();
							adj.add(t);
							adj.add(sym.tri);
							ot.setAttributes(OTriangle.NONMANIFOLD);
							sym.setAttributes(OTriangle.NONMANIFOLD);
							ot.setAdj(adj);
							sym.setAdj(adj);
						}
						else
							adj = (ArrayList) ot.getAdj();
						adj.add(t2);
						ot2.setAdj(adj);
						ot2.setAttributes(OTriangle.NONMANIFOLD);
						System.out.println("Non-manifold: "+v+"\n"+ot+"\n"+ot2);
					}
					cnt++;
				}
			}
			if (cnt == 0)
			{
				ot.setAttributes(OTriangle.BOUNDARY);
				Triangle t2 = new Triangle(Vertex.outer, v2, v);
				//  This is an outer triangle
				t2.setOuter();
				sym.bind(t2);
				assert sym.origin() == v2 && sym.destination() == v;
				sym.setAttributes(OTriangle.BOUNDARY);
				ot.glue(sym);
				newTriangles.add(t2);
				list2.add(t2);
			}
			else if (cnt > 1)
			{
				// Non manifold edge.
				// Incident triangles must be properly sorted,
				// then adjacent relations can be updated.
				ArrayList adj = (ArrayList) ot.getAdj();
				adj.add(adj.get(0));
				adj.add(adj.get(1));
				for (int i = adj.size() - 2; i > 0; i--)
				{
					Triangle t2 = (Triangle) adj.get(i);
					Triangle tprev = (Triangle) adj.get(i-1);
					Triangle tnext = (Triangle) adj.get(i+1);
					ot2.bind(t2);
					if (ot2.getAdj() != adj)
						ot2.nextOTri();
					if (ot2.getAdj() != adj)
						ot2.nextOTri();
					assert ot2.getAdj() == adj;
					Triangle [] newAdj = new Triangle[2];
					newAdj[0] = tnext;
					newAdj[1] = tprev;
					ot2.setAdj(newAdj);
				}
			}
		}
		//  Unmark adjacent triangles
		for (Iterator it = list.iterator(); it.hasNext(); )
		{
			Triangle t = (Triangle) it.next();
			t.unmark();
		}
		//  Update triangle list, if new triangles have been added
		list.addAll(newTriangles);
	}
	
	private static final boolean checkRidges(Vertex v, double cosMinAngle, OTriangle ot)
	{
		OTriangle sym = new OTriangle();
		if (ot.origin() != v)
			ot.nextOTri();
		if (ot.origin() != v)
			ot.nextOTri();
		assert ot.origin() == v;
		Vertex first = ot.destination();
		while (true)
		{
			Vertex d = ot.destination();
			if (d != Vertex.outer && 0 != d.getRef())
			{
				ot.computeNormal3D();
				double [] n1 = ot.getTempVector();
				OTriangle.symOTri(ot, sym);
				sym.computeNormal3D();
				double [] n2 = sym.getTempVector();
				double angle = Metric3D.prodSca(n1, n2);
				if (angle > -cosMinAngle)
					return false;
			}
			ot.nextOTriOrigin();
			if (ot.destination() == first)
				break;
		}
		return true;
	}
	
	public void writeUNV2D(String file)
	{
		String cr=System.getProperty("line.separator");
		PrintWriter out;
		try {
			if (file.endsWith(".gz") || file.endsWith(".GZ"))
				out = new PrintWriter(new java.util.zip.GZIPOutputStream(new FileOutputStream(file)));
			else
				out = new PrintWriter(new FileOutputStream(file));
			out.println("    -1"+cr+"  2411");
			HashSet nodeset = new HashSet();
			for(Iterator it=triangleList.iterator();it.hasNext();)
			{
				Triangle t = (Triangle) it.next();
				if (t.isOuter())
					continue;
				if (t.vertex[0] == Vertex.outer || t.vertex[1] == Vertex.outer || t.vertex[2] == Vertex.outer)
					continue;
				nodeset.add(t.vertex[0]);
				nodeset.add(t.vertex[1]);
				nodeset.add(t.vertex[2]);
			}
			int count =  0;
			HashMap labels = new HashMap(nodeset.size());
			for(Iterator it=nodeset.iterator();it.hasNext();)
			{
				Vertex node = (Vertex) it.next();
				count++;
				Integer label = new Integer(count);
				labels.put(node, label);
				double [] uv = node.getUV();
				out.println(label+"         1         1         1");
				out.println(""+uv[0]+" "+uv[1]+" 0.0");
			}
			out.println("    -1");
			out.println("    -1"+cr+"  2412");
			count =  0;
			for(Iterator it=triangleList.iterator();it.hasNext();)
			{
				Triangle t = (Triangle)it.next();
				if (t.isOuter())
					continue;
				if (t.vertex[0] == Vertex.outer || t.vertex[1] == Vertex.outer || t.vertex[2] == Vertex.outer)
					continue;
				count++;
				out.println(""+count+"        91         1         1         1         3");
				for(int i = 0; i < 3; i++)
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
		} catch (IOException e)
		{
			logger.fatal(e.toString());
			e.printStackTrace();
		}
	}
	
	/**
	 * Checks whether this mesh is valid.
	 * This routine returns <code>isValid(true)</code>.
	 *
	 * @see #isValid(boolean)
	 */
	public boolean isValid()
	{
		return isValid(true);
	}
	
	/**
	 * Checks whether this mesh is valid.
	 * This routine can be called at any stage, even before boundary
	 * edges have been enforced.  In this case, some tests must be
	 * removed because they do not make sense.
	 *
	 * @param constrained  <code>true</code> if mesh is constrained.
	 */
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
			else if (type == MESH_2D && t.vertex[0].onLeft(t.vertex[1], t.vertex[2]) <= 0L)
				return false;
		}
		return true;
	}
	
}
