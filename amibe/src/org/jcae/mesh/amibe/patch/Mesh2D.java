/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2004,2005,2006, by EADS CRC

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
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.AbstractTriangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.ds.AbstractVertex;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.amibe.InitialTriangulationException;
import org.jcae.mesh.amibe.metrics.Metric2D;
import org.jcae.mesh.amibe.util.KdTree;
import org.jcae.mesh.amibe.util.KdTreeProcedure;
import org.jcae.mesh.cad.*;
import java.util.HashSet;
import java.util.Stack;
import java.util.Collection;
import org.apache.log4j.Logger;

/**
 * Mesh data structure for parameterized surfaces.
 * Connectivity between triangles and vertices is inherited from {@link Mesh},
 * and a {@link KdTree} instance added in order to speed up finding the
 * nearest {@link Vertex2D} <code>V</code> from any given point <code>V0</code>.
 */
public class Mesh2D extends Mesh
{
	private static Logger logger=Logger.getLogger(Mesh2D.class);
	
	//  Topological face on which mesh is applied
	private transient final CADShape face;
	
	//  The geometrical surface describing the topological face, stored for
	//  efficiency reason
	private transient final CADGeomSurface surface;
	
	//  Stack of methods to compute geometrical values
	private transient final Stack<Calculus> compGeomStack = new Stack<Calculus>();
	
	/**
	 * Structure to fasten search of nearest vertices.
	 */
	private transient KdTree quadtree = null;
	
	// Utility class to improve debugging output
	private static class OuterVertex2D extends Vertex2D
	{
		public OuterVertex2D()
		{
			super();
		}
		public OuterVertex2D(double u, double v)
		{
			super(u, v);
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
		super();
		MeshTraitsBuilder mtb = new MeshTraitsBuilder();
		factory = new ElementPatchFactory(mtb);
		face = null;
		surface = null;
		init();
	}

	public Mesh2D(MeshTraitsBuilder mtb)
	{
		super(mtb);
		factory = new ElementPatchFactory(mtb);
		face = null;
		surface = null;
		init();
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
	public Mesh2D(MeshTraitsBuilder mtb, CADShape f)
	{
		super(mtb);
		factory = new ElementPatchFactory(mtb);
		face = f;
		surface = ((CADFace) face).getGeomSurface();
		init();
	}

	public Mesh2D(CADShape f)
	{
		super();
		MeshTraitsBuilder mtb = new MeshTraitsBuilder();
		factory = new ElementPatchFactory(mtb);
		face = f;
		surface = ((CADFace) face).getGeomSurface();
		init();
	}

	private final void init()
	{
		outerVertex = new OuterVertex2D();
		String accumulateEpsilonProp = System.getProperty("org.jcae.mesh.amibe.ds.Mesh.cumulativeEpsilon");
		if (accumulateEpsilonProp == null)
		{
			accumulateEpsilonProp = "false";
			System.setProperty("org.jcae.mesh.amibe.ds.Mesh.cumulativeEpsilon", accumulateEpsilonProp);
		}
		accumulateEpsilon = accumulateEpsilonProp.equals("true");
		String absEpsilonProp = System.getProperty("org.jcae.mesh.amibe.ds.Mesh.epsilon");
		if (absEpsilonProp == null)
		{
			absEpsilonProp = "-1.0";
			System.setProperty("org.jcae.mesh.amibe.ds.Mesh.epsilon", absEpsilonProp);
		}
		Double absEpsilon = new Double(absEpsilonProp);
		epsilon = absEpsilon.doubleValue();

		if (!(face instanceof CADFace))
		{
			if (epsilon < 0.0)
				epsilon = 0.0;
			return;
		}

		CADFace F = (CADFace) face;
		double [] bb = F.boundingBox();
		double diagonal = Math.sqrt(
		    (bb[0] - bb[3]) * (bb[0] - bb[3]) +
		    (bb[1] - bb[4]) * (bb[1] - bb[4]) +
		    (bb[2] - bb[5]) * (bb[2] - bb[5]));
		if (Metric2D.getLength() == 0.0)
			Metric2D.setLength(diagonal);
		if (epsilon < 0)
			epsilon = Math.max(diagonal/1000.0, Metric2D.getLength() / 100.0);
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
	 * Initialize a KdTree with a given bounding box.
	 * @param bbmin  coordinates of bottom-left vertex
	 * @param bbmax  coordinates of top-right vertex
	 */
	public void initQuadTree(double [] bbmin, double [] bbmax)
	{
		quadtree = new KdTree(2, bbmin, bbmax);
		outerVertex = new OuterVertex2D((bbmin[0]+bbmax[0])*0.5, (bbmin[1]+bbmax[1])*0.5);
	}
	
	/**
	 * Returns the quadtree associated with this mesh.
	 *
	 * @return the quadtree associated with this mesh.
	 */
	public KdTree getQuadTree()
	{
		return quadtree;
	}
	
	/**
	 * Sets the quadtree associated with this mesh.
	 *
	 * @param q  the quadtree associated with this mesh.
	 */
	public void setQuadTree(KdTree q)
	{
		quadtree = q;
	}
	
	/**
	 * Returns vertex list.  Note that this class does not rely on
	 * {@link MeshTraitsBuilder}, but call {@link KdTree#getAllVertices}.
	 *
	 * @return vertex list.
	 */
	@Override
	public Collection<AbstractVertex> getNodes()
	{
		if (quadtree == null)
			return null;
		return quadtree.getAllVertices(triangleList.size() / 2);
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
		assert quadtree != null;
		assert v0.onLeft(this, v1, v2) != 0L;
		if (v0.onLeft(this, v1, v2) < 0L)
		{
			Vertex2D temp = v2;
			v2 = v1;
			v1 = temp;
		}
		Triangle first = (Triangle) factory.createTriangle(v0, v1, v2);
		Triangle adj0 = (Triangle) factory.createTriangle(outerVertex, v2, v1);
		Triangle adj1 = (Triangle) factory.createTriangle(outerVertex, v0, v2);
		Triangle adj2 = (Triangle) factory.createTriangle(outerVertex, v1, v0);
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
		Triangle t = (Triangle) start.getLink();
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
		s.prevOrigin();
		dest = (Vertex2D) s.destination();
		i = 0;
		while (true)
		{
			Vertex2D d = (Vertex2D) s.destination();
			if (d == end)
				return s;
			else if (d != outerVertex && start.onLeft(this, end, d) < 0L)
				break;
			s.prevOrigin();
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
			compGeomStack.push(new Calculus2D(this));
		else if (i == 3)
			compGeomStack.push(new Calculus3D(this));
		else
			throw new java.lang.IllegalArgumentException("pushCompGeom argument must be either 2 or 3, current value is: "+i);
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
		if (compGeomStack.size() > 0 && !ret.getClass().equals(compGeomStack.peek().getClass()))
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
		return ret;
	}
	
	/**
	 * Returns metrics dimension.
	 *
	 * @return metrics dimension.
	 */
	public Calculus compGeom()
	{
		if (compGeomStack.empty())
			return null;
		return compGeomStack.peek();
	}
	
	private static class ClearAllMetricsProcedure implements KdTreeProcedure
	{
		public final int action(Object o, int s, final int [] i0)
		{
			KdTree.Cell self = (KdTree.Cell) o;
			if (self.isLeaf())
			{
				for (int i = 0, n = self.count(); i < n; i++)
					((Vertex2D) self.getVertex(i)).clearMetrics();
			}
			return 0;
		}
	}
	
	/**
	 * Remove all metrics of vertices stored in this <code>KdTree</code>.
	 */
	private void clearAllMetrics()
	{
		if (quadtree == null)
			return;
		ClearAllMetricsProcedure gproc = new ClearAllMetricsProcedure();
		quadtree.walk(gproc);
	}

	@Override
	public double distance2(Vertex start, Vertex end, Vertex vm)
	{
		double ret = distance(start, end, vm);
		return ret*ret;
	}
	
	/**
	 * Returns the Riemannian distance between nodes.
	 *
	 * @param start  the start node
	 * @param end  the end node
	 * @param vm  the vertex on which metrics is evaluated
	 * @return the distance between nodes
	 */
	@Override
	public double distance(Vertex start, Vertex end, Vertex vm)
	{
		return compGeom().distance((Vertex2D) start, (Vertex2D) end, (Vertex2D) vm);
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
	public double radius2d(Vertex v)
	{
		return compGeom().radius2d((Vertex2D) v);
	}
	
	/**
	 * Remove degenerted edges.
	 * Degenerated edges are present in 2D mesh, and have to be
	 * removed in the 2D -&gt; 3D transformation.  Triangles and
	 * vertices must then be updated too.
	 */
	public void removeDegeneratedEdges()
	{
		logger.debug("Removing degenerated edges");
		VirtualHalfEdge2D ot = new VirtualHalfEdge2D();
		HashSet<AbstractTriangle> removedTriangles = new HashSet<AbstractTriangle>();
		for (AbstractTriangle at: triangleList)
		{
			if (removedTriangles.contains(at))
				continue;
			Triangle t = (Triangle) at;
			if (t.isOuter())
				continue;
			ot.bind(t);
			for (int i = 0; i < 3; i++)
			{
				ot.next();
				if (!ot.hasAttributes(AbstractHalfEdge.BOUNDARY))
					continue;
				int ref1 = ot.origin().getRef();
				int ref2 = ot.destination().getRef();
				if (ref1 != 0 && ref2 != 0 && ref1 == ref2)
				{
					if (logger.isDebugEnabled())
						logger.debug("  Collapsing "+ot);
					removedTriangles.add(ot.getTri());
					ot.removeDegenerated(this);
					break;
				}
			}
		}
		for (AbstractTriangle at: removedTriangles)
			triangleList.remove(at);
	}
	
	@Override
	public boolean isValid(boolean constrained)
	{
		if (!super.isValid(constrained))
			return false;
		for (AbstractTriangle t: triangleList)
		{
			// We can not rely on t.isOuter() here, attributes
			// may not have been set yet.
			if (t.vertex[0] == outerVertex || t.vertex[1] == outerVertex || t.vertex[2] == outerVertex)
					continue;
			Vertex2D tv0 = (Vertex2D) t.vertex[0];
			Vertex2D tv1 = (Vertex2D) t.vertex[1];
			Vertex2D tv2 = (Vertex2D) t.vertex[2];
			double l = tv0.onLeft(this, tv1, tv2);
			if (l <= 0L)
			{
				logger.error("Wrong orientation: "+l+" "+t);
				return false;
			}
		}
		return true;
	}
	
}
