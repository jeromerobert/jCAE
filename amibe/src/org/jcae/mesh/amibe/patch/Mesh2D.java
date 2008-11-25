/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2004,2005,2006, by EADS CRC
    Copyright (C) 2007,2008, by EADS France

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

package org.jcae.mesh.amibe.patch;

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.MeshParameters;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.TriangleVH;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.amibe.InitialTriangulationException;
import org.jcae.mesh.amibe.util.KdTree;
import org.jcae.mesh.amibe.util.KdTreeProcedure;
import org.jcae.mesh.cad.*;
import java.util.Stack;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * Mesh data structure for parameterized surfaces.
 * Connectivity between triangles and vertices is inherited from {@link Mesh},
 * and a {@link KdTree} instance added in order to speed up finding the
 * nearest {@link Vertex2D} <code>V</code> from any given point <code>V0</code>.
 */
public class Mesh2D extends Mesh
{
	private static Logger logger=Logger.getLogger(Mesh2D.class.getName());
	
	//  Topological face on which mesh is applied
	private transient final CADShape face;
	
	//  The geometrical surface describing the topological face, stored for
	//  efficiency reason
	private transient final CADGeomSurface surface;
	
	//  Stack of methods to compute geometrical values
	private transient final Stack<Calculus> compGeomStack = new Stack<Calculus>();
	
	//  Current top value of compGeomStack
	private transient Calculus compGeomCurrent = null;
	
	// Utility class to improve debugging output
	private static class OuterVertex2D extends Vertex2D
	{
		public OuterVertex2D(double u, double v)
		{
			super(null, u, v);
		}
		@Override
		public String toString()
		{
			return "outer";
		}
	}

	/**
	 * Sole constructor.
	 */
	public Mesh2D()
	{
		this(MeshTraitsBuilder.getDefault2D());
	}

	public Mesh2D(MeshTraitsBuilder mtb)
	{
		this(mtb, new MeshParameters(), null);
	}

	public Mesh2D(CADShape f)
	{
		this(MeshTraitsBuilder.getDefault2D(), new MeshParameters(), f);
	}

	/**
	 * Creates an empty mesh bounded to the topological surface.
	 * This constructor also initializes tolerance values.  If length
	 * criterion is null, {@link MeshParameters#setLength} is called with
	 * the diagonal length of face bounding box as argument.
	 * If {@link MeshParameters#epsilon} is not set, epsilon is computed as
	 * being the maximal value between length criterion by 100 and diagonal
	 * length by 1000.
	 *
	 * @param f   topological surface
	 */
	public Mesh2D(MeshTraitsBuilder mtb, MeshParameters mp, CADShape f)
	{
		super(mtb, mp);
		factory = new ElementPatchFactory(traitsBuilder);
		face = f;
		if (face == null)
			surface = null;
		else
			surface = ((CADFace) face).getGeomSurface();
		init();
	}

	private final void init()
	{
		outerVertex = new OuterVertex2D(0.0, 0.0);
		outerTrianglesAreConnected = true;

		double epsilon = meshParameters.getEpsilon();

		if (!(face instanceof CADFace))
			return;

		CADFace F = (CADFace) face;
		double [] bb = F.boundingBox();
		double diagonal = Math.sqrt(
		    (bb[0] - bb[3]) * (bb[0] - bb[3]) +
		    (bb[1] - bb[4]) * (bb[1] - bb[4]) +
		    (bb[2] - bb[5]) * (bb[2] - bb[5]));
		if (meshParameters.getLength() == 0.0)
			meshParameters.setLength(diagonal);
		if (epsilon < 0)
			epsilon = Math.max(diagonal/1000.0, meshParameters.getLength() / 100.0);
		meshParameters.setEpsilon(epsilon);
		logger.fine("Bounding box diagonal: "+diagonal);
		logger.fine("Epsilon: "+epsilon);
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
	 * Returns vertex list.  Note that this class does not rely on
	 * {@link MeshTraitsBuilder}, but call {@link KdTree#getAllVertices}.
	 *
	 * @return vertex list.
	 */
	@Override
	public Collection<Vertex> getNodes()
	{
		KdTree quadtree = traitsBuilder.getKdTree(traits);
		if (quadtree == null)
			return null;
		return quadtree.getAllVertices(getTriangles().size() / 2);
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
	public void bootstrap(Vertex2D v0, Vertex2D v1, Vertex2D v2)
	{
		KdTree quadtree = traitsBuilder.getKdTree(traits);
		assert quadtree != null;
		assert v0.onLeft(this, v1, v2) != 0L;
		if (v0.onLeft(this, v1, v2) < 0L)
		{
			Vertex2D temp = v2;
			v2 = v1;
			v1 = temp;
		}
		TriangleVH first = (TriangleVH) factory.createTriangle(v0, v1, v2);
		TriangleVH adj0 = (TriangleVH) factory.createTriangle(outerVertex, v2, v1);
		TriangleVH adj1 = (TriangleVH) factory.createTriangle(outerVertex, v0, v2);
		TriangleVH adj2 = (TriangleVH) factory.createTriangle(outerVertex, v1, v0);
		VirtualHalfEdge2D ot = new VirtualHalfEdge2D(first, 0);
		VirtualHalfEdge2D oa0 = new VirtualHalfEdge2D(adj0, 0);
		VirtualHalfEdge2D oa1 = new VirtualHalfEdge2D(adj1, 0);
		VirtualHalfEdge2D oa2 = new VirtualHalfEdge2D(adj2, 0);
		ot.glue(oa0);
		ot.next();
		ot.glue(oa1);
		ot.next();
		ot.glue(oa2);
		oa0.next();
		oa2.prev();
		oa0.glue(oa2);
		oa0.next();
		oa1.next();
		oa0.glue(oa1);
		oa1.next();
		oa2.prev();
		oa2.glue(oa1);
		
		outerVertex.setLink(adj0);
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
	 * Enforces an edge between two points.
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
	public VirtualHalfEdge2D forceBoundaryEdge(Vertex2D start, Vertex2D end, int maxIter)
		throws InitialTriangulationException
	{
		assert (start != end);
		TriangleVH t = (TriangleVH) start.getLink();
		VirtualHalfEdge2D s = new VirtualHalfEdge2D(t, 0);
		if (s.origin() != start)
			s.next();
		if (s.origin() != start)
			s.next();
		assert s.origin() == start : ""+start+" does not belong to "+t;
		Vertex2D dest = (Vertex2D) s.destination();
		int i = 0;
		while (true)
		{
			Vertex2D d = (Vertex2D) s.destination();
			if (d == end)
				return s;
			else if (d != outerVertex && start.onLeft(this, end, d) > 0L)
				break;
			s.nextOrigin();
			i++;
			if ((Vertex2D) s.destination() == dest || i > maxIter)
				throw new InitialTriangulationException();
		}
		s.sym();
		s.next();
		dest = (Vertex2D) s.destination();
		i = 0;
		while (true)
		{
			Vertex2D d = (Vertex2D) s.destination();
			if (d == end)
				return s;
			else if (d != outerVertex && start.onLeft(this, end, d) < 0L)
				break;
			s.sym();
			s.next();
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
			int inter = s.forceBoundaryEdge(this, end);
			logger.fine("Intersectionss: "+inter);
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
			Vertex2D temp = start;
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
			compGeomCurrent = new Calculus2D(this);
		else if (i == 3)
			compGeomCurrent = new Calculus3D(this);
		else
			throw new java.lang.IllegalArgumentException("pushCompGeom argument must be either 2 or 3, current value is: "+i);
		compGeomStack.push(compGeomCurrent);
		clearAllMetrics();
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
		Calculus ret = compGeomStack.pop();
		if (!compGeomStack.empty() && !ret.getClass().equals(compGeomStack.peek().getClass()))
			clearAllMetrics();
		if (compGeomStack.empty())
			compGeomCurrent = null;
		else
			compGeomCurrent = compGeomStack.peek();
		return ret;
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
		Calculus ret = compGeomStack.pop();
		if (!compGeomStack.empty() && !ret.getClass().equals(compGeomStack.peek().getClass()))
			clearAllMetrics();
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
		if (compGeomStack.empty())
			compGeomCurrent = null;
		else
			compGeomCurrent = compGeomStack.peek();
		return ret;
	}
	
	/**
	 * Returns metrics dimension.
	 *
	 * @return metrics dimension.
	 */
	public Calculus compGeom()
	{
		return compGeomCurrent;
	}
	
	private static class ClearAllMetricsProcedure implements KdTreeProcedure
	{
		// Add a public constructor to avoid synthetic access
		public ClearAllMetricsProcedure()
		{
		}
		public final int action(Object o, int s, final int [] i0)
		{
			KdTree.Cell self = (KdTree.Cell) o;
			if (self.isLeaf())
			{
				for (int i = 0, n = self.count(); i < n; i++)
					((Vertex2D) self.getVertex(i)).clearMetrics();
			}
			return KdTreeProcedure.OK;
		}
	}
	
	/**
	 * Remove all metrics of vertices stored in this <code>KdTree</code>.
	 */
	private void clearAllMetrics()
	{
		KdTree quadtree = traitsBuilder.getKdTree(traits);
		if (quadtree == null)
			return;
		ClearAllMetricsProcedure gproc = new ClearAllMetricsProcedure();
		quadtree.walk(gproc);
	}

	/**
	 * Returns Riemannian square distance between nodes.
	 *
	 * @param start  the start node
	 * @param end  the end node
	 * @param vm  the vertex on which metrics is evaluated
	 * @return square distance between nodes
	 */
	@Override
	public double distance2(Vertex start, Vertex end, Vertex vm)
	{
		return compGeomCurrent.distance2((Vertex2D) start, (Vertex2D) end, ((Vertex2D) vm).getMetrics(this));
	}
	
	/**
	 * Returns the 2D radius of the 3D unit ball centered at a point.
	 * This routine returns a radius such that the 2D circle centered
	 * at a given vertex will have a distance lower than 1 in 3D.
	 * This method is used by {@link KdTree#getNearestVertex}
	 *
	 * @param v  the vertex on which metrics is evaluated
	 * @return the radius in 2D space.
	 */
	@Override
	public double [] getBounds(Vertex v)
	{
		return compGeomCurrent.getBounds2D((Vertex2D) v);
	}
	
	@Override
	public boolean isValid(boolean constrained)
	{
		if (!super.isValid(constrained))
			return false;
		for (Triangle t: getTriangles())
		{
			// We can not rely on t.hasAttributes(AbstractHalfEdge.OUTER) here,
			// attributes may not have been set yet.
			if (t.vertex[0] == outerVertex || t.vertex[1] == outerVertex || t.vertex[2] == outerVertex)
					continue;
			Vertex2D tv0 = (Vertex2D) t.vertex[0];
			Vertex2D tv1 = (Vertex2D) t.vertex[1];
			Vertex2D tv2 = (Vertex2D) t.vertex[2];
			double l = tv0.onLeft(this, tv1, tv2);
			if (l <= 0L)
			{
				logger.severe("Wrong orientation: "+l+" "+t);
				return false;
			}
		}
		return true;
	}
	
}
