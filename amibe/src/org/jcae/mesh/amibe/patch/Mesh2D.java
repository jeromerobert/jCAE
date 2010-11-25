/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2004,2005,2006, by EADS CRC
    Copyright (C) 2007,2008,2009,2010, by EADS France

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
import org.jcae.mesh.amibe.metrics.KdTree;
import org.jcae.mesh.amibe.metrics.KdTreeProcedure;
import org.jcae.mesh.amibe.metrics.Location;
import org.jcae.mesh.cad.CADFace;
import org.jcae.mesh.cad.CADGeomSurface;
import org.jcae.mesh.cad.CADShape;

import java.util.Stack;
import java.util.Collection;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Mesh data structure for parameterized surfaces.
 * Connectivity between triangles and vertices is inherited from {@link Mesh},
 * and a {@link KdTree} instance added in order to speed up finding the
 * nearest {@link Vertex2D} <code>V</code> from any given point <code>V0</code>.
 */
public class Mesh2D extends Mesh
{
	private static final long serialVersionUID = -2970368204713902058L;
	private static final Logger logger=Logger.getLogger(Mesh2D.class.getName());
	
	//  Topological face on which mesh is applied
	private transient final CADShape face;
	
	//  The geometrical surface describing the topological face, stored for
	//  efficiency reason
	private transient final CADGeomSurface surface;
	
	//  Stack of methods to compute geometrical values
	private transient final Stack<Integer> compGeomStack = new Stack<Integer>();
	
	//  Current top value of compGeomStack
	private transient int compGeomCurrent;

	// 2D euclidian metric
	private transient final EuclidianMetric2D euclidian_metric2d = new EuclidianMetric2D();

	// Few methods in Vertex2D and VirtualHalfEdge2D require a pseudo-random generator.
	// Define it in Mesh2D so that this generator gives the same value when
	// performing several meshes in the same run.
	transient final Random rand = new Random(139L);

	private static final double delta_max = 0.5;
	private static final int level_max = 10;
	private static final Integer [] intArray = new Integer[level_max+1];
	private static final boolean accurateDistance;

	static {
		String accurateDistanceProp = System.getProperty("org.jcae.mesh.amibe.patch.Mesh2D.accurateDistance");
		if (accurateDistanceProp == null)
		{
			accurateDistanceProp = "false";
			System.setProperty("org.jcae.mesh.amibe.patch.Mesh2D.accurateDistance", accurateDistanceProp);
		}
		accurateDistance = accurateDistanceProp.equals("true");
		for (int i = 0; i <= level_max; i++)
			intArray[i] = Integer.valueOf(i);
	}

	// Some algorithms make heavy use of VirtualHalfEdge2D, create a pool
	protected final VirtualHalfEdge2D [] poolVH2D = new VirtualHalfEdge2D[4];

	// Utility class to improve debugging output
	private static class OuterVertex2D extends Vertex2D
	{
		private static final long serialVersionUID = 3873055411695847841L;

		private OuterVertex2D(double u, double v)
		{
			super(null, u, v);
		}
		@Override
		public final String toString()
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

	private Mesh2D(MeshTraitsBuilder mtb)
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
		{
			surface = ((CADFace) face).getGeomSurface();
			surface.dinit(2);

		}
		init();
	}

	private void init()
	{
		outerVertex = new OuterVertex2D(0.0, 0.0);
		outerTrianglesAreConnected = true;

		for (int i = 0; i < 4; i++)
			poolVH2D[i] = new VirtualHalfEdge2D();

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
	public final CADShape getGeometry()
	{
		return face;
	}
	
	/**
	 * Returns the geometrical surface.
	 *
	 * @return the geometrical surface.
	 */
	public final CADGeomSurface getGeomSurface()
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
	public final Collection<Vertex> getNodes()
	{
		KdTree<Vertex> quadtree = traitsBuilder.getKdTree(traits);
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
	public final void bootstrap(Vertex2D v0, Vertex2D v1, Vertex2D v2)
	{
		KdTree<Vertex2D> quadtree = traitsBuilder.getKdTree(traits);
		assert quadtree != null;
		assert v0.onLeft(quadtree, v1, v2) != 0L;
		if (v0.onLeft(quadtree, v1, v2) < 0L)
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
	public final VirtualHalfEdge2D forceBoundaryEdge(Vertex2D start, Vertex2D end, int maxIter)
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
		KdTree<Vertex2D> quadtree = traitsBuilder.getKdTree(traits);
		int i = 0;
		while (true)
		{
			Vertex2D d = (Vertex2D) s.destination();
			if (d == end)
				return s;
			else if (d != outerVertex && start.onLeft(quadtree, end, d) > 0L)
				break;
			s.nextOrigin();
			i++;
			if (s.destination() == dest || i > maxIter)
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
			else if (d != outerVertex && start.onLeft(quadtree, end, d) < 0L)
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
	public final void pushCompGeom(int i)
	{
		if (i != 2 && i != 3)
			throw new java.lang.IllegalArgumentException("pushCompGeom argument must be either 2 or 3, current value is: "+i);
		compGeomCurrent = Integer.valueOf(i);
		compGeomStack.push(compGeomCurrent);
		clearAllMetrics();
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
	public final void popCompGeom(int i)
		throws RuntimeException
	{
		Integer ret = compGeomStack.pop();
		if (!compGeomStack.empty() && !ret.equals(compGeomStack.peek()))
			clearAllMetrics();
		if (ret.intValue() != i)
			throw new java.lang.RuntimeException("Internal error.  Expected value: "+i+", found: "+ret);
		if (compGeomStack.empty())
			compGeomCurrent = 0;
		else
			compGeomCurrent = compGeomStack.peek().intValue();
	}
	
	private static class ClearAllMetricsProcedure implements KdTreeProcedure
	{
		// Add a public constructor to avoid synthetic access
		public ClearAllMetricsProcedure()
		{
		}
		public final int action(Object o, int s, final int [] i0)
		{
			KdTree<Vertex2D>.Cell self = (KdTree.Cell) o;
			if (self.isLeaf())
			{
				for (int i = 0, n = self.count(); i < n; i++)
					self.getVertex(i).metric = null;
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

	@Override
	public final Metric2D getMetric(Location pt)
	{
		Vertex2D v2 = (Vertex2D) pt;
		Metric2D m2 = v2.metric;
		if (null == m2)
		{
			if (compGeomCurrent == 2)
				m2 = euclidian_metric2d;
			else
			{
				double uv[] = pt.getUV();
				surface.setParameter(uv[0], uv[1]);
				m2 = new MetricOnSurface(surface, meshParameters);
			}
			v2.metric = m2;
		}
		return m2;
	}

	public final void moveVertex(Vertex2D vertex, double u, double v)
	{
		vertex.metric = null;
		vertex.moveTo(u, v);
	}

	/**
	 * Move a vertex to the 2D centroid of a triangle.
	 *
	 * @param vertex vertex
	 * @param t triangle
	 */
	public final void moveVertexToCentroid(Vertex2D vertex, Triangle t)
	{
		double x = 0.0, y = 0.0;
		for (Vertex v : t.vertex)
		{
			double [] p = v.getUV();
			x += p[0];
			y += p[1];
		}
		x /= t.vertex.length;
		y /= t.vertex.length;
		moveVertex(vertex, x, y);
	}

	/**
	 * Returns the Riemannian distance between nodes.
	 * This distance is computed with metrics on start and end points,
	 * and the maximal distance is returned.
	 *
	 * @param start  the start node
	 * @param end  the end node
	 * @return the distance between nodes
	 **/
	public final double interpolatedDistance(Vertex2D start, Vertex2D end)
	{
		if (compGeomCurrent == 2)
			return Math.sqrt(euclidian_metric2d.distance2(start.getUV(), end.getUV()));

		Metric2D ms = getMetric(start);
		Metric2D me = getMetric(end);
		double [] ps = start.getUV();
		double [] pe = end.getUV();
		double l1 = Math.sqrt(ms.distance2(ps, pe));
		double l2 = Math.sqrt(me.distance2(ps, pe));
		double lmax = Math.max(l1, l2);
		if (!accurateDistance || Math.abs(l1 - l2) < delta_max * lmax)
			return lmax;

		Stack<Vertex2D> v = new Stack<Vertex2D>();
		Stack<Integer> l = new Stack<Integer>();
		Vertex2D mid = Vertex2D.middle(start, end);
		l.push(intArray[level_max]);
		v.push(end);
		v.push(mid);
		l.push(intArray[level_max]);
		v.push(mid);
		v.push(start);
		double ret = 0.0;
		int level = level_max;
		while (v.size() > 0)
		{
			Vertex2D pt1 = v.pop();
			Vertex2D pt2 = v.pop();
			level = l.pop().intValue();
			ms = getMetric(pt1);
			me = getMetric(pt2);
			ps = pt1.getUV();
			pe = pt2.getUV();
			l1 = Math.sqrt(ms.distance2(ps, pe));
			l2 = Math.sqrt(me.distance2(ps, pe));
			lmax = Math.max(l1, l2);
			if (Math.abs(l1 - l2) < delta_max * lmax || level == 0)
				ret += lmax;
			else
			{
				level--;
				mid = Vertex2D.middle(pt1, pt2);
				l.push(intArray[level]);
				v.push(pt2);
				v.push(mid);
				l.push(intArray[level]);
				v.push(mid);
				v.push(pt1);
			}
		}
		return ret;
	}
	
	@Override
	public final boolean isValid(boolean constrained)
	{
		if (!super.isValid(constrained))
			return false;
		KdTree<Vertex2D> quadtree = traitsBuilder.getKdTree(traits);

		for (Triangle t: getTriangles())
		{
			// We can not rely on t.hasAttributes(AbstractHalfEdge.OUTER) here,
			// attributes may not have been set yet.
			if (t.vertex[0] == outerVertex || t.vertex[1] == outerVertex || t.vertex[2] == outerVertex)
					continue;
			Vertex2D tv0 = (Vertex2D) t.vertex[0];
			Vertex2D tv1 = (Vertex2D) t.vertex[1];
			Vertex2D tv2 = (Vertex2D) t.vertex[2];
			double l = tv0.onLeft(quadtree, tv1, tv2);
			if (l <= 0L)
			{
				logger.severe("Wrong orientation: "+l+" "+t);
				return false;
			}
		}
		return true;
	}
	
}
