/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2005,2006, by EADS CRC
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

import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.VirtualHalfEdge;
import org.jcae.mesh.amibe.ds.Triangle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.metrics.KdTree;

/**
 * A handle to abstract edge objects for initial 2D mesh.
 * This class implements some features which are only relevant
 * to the initial 2D mesh.  In particular, boundary edges have
 * not yet been rebuilt, so we cannot check whether the OUTER
 * attribute is set but need to test if vertices are equal to
 * the infinite point instead.
 */
public class VirtualHalfEdge2D extends VirtualHalfEdge
{
	private static final Logger logger=Logger.getLogger(VirtualHalfEdge2D.class.getName());
	
	public VirtualHalfEdge2D()
	{
		super();
	}
	
	/**
	 * Create an object to handle data about a triangle.
	 *
	 * @param t  geometrical triangle.
	 * @param o  a number between 0 and 2 determining an edge.
	 */
	public VirtualHalfEdge2D(Triangle t, int o)
	{
		super(t, o);
	}
	
	/**
	 * Copy current <code>VirtualHalfEdge</code> and move it to the counterclockwise
	 * previous edge which has the same origin.
	 *
	 * @param that  already allocated <code>VirtualHalfEdge</code> where data are
	 *              copied
	 */
	/* Method never used
	private final AbstractHalfEdge prevOrigin(AbstractHalfEdge that)
	{
		return sym(that).next();
	}
	*/
	
	/**
	 * Move counterclockwise to the previous edge with same origin.
	 * @return  current instance after its transformation
	 */
	private AbstractHalfEdge prevOrigin()
	{
		return sym().next();
	}
	
	/**
	 * Copy current <code>VirtualHalfEdge</code> and move it to the counterclockwise
	 * following edge which has the same destination.
	 *
	 * @param that  already allocated <code>VirtualHalfEdge</code> where data are
	 *              copied
	 */
	/* Method never used
	private final AbstractHalfEdge nextDest(AbstractHalfEdge that)
	{
		return sym(that).prev();
	}
	*/
	
	/**
	 * Move counterclockwise to the following edge with same
	 * destination.
	 * @return  current instance after its transformation
	 */
	private AbstractHalfEdge nextDest()
	{
		return sym().prev();
	}
	
	/**
	 * Copy current <code>VirtualHalfEdge</code> and move it to the counterclockwise
	 * previous edge which has the same destination.
	 *
	 * @param that  already allocated <code>VirtualHalfEdge</code> where data are
	 *              copied
	 */
	/* Method never used
	private final AbstractHalfEdge prevDest(AbstractHalfEdge that)
	{
		return next(that).sym();
	}
	*/
	
	/**
	 * Move counterclockwise to the previous edge with same
	 * destination.
	 * @return  current instance after its transformation
	 */
	/* Method never used
	private final AbstractHalfEdge prevDest()
	{
		return next().sym();
	}
	*/
	
	/**
	 * Copy current <code>VirtualHalfEdge</code> and move it to the counterclockwise
	 * following edge which has the same apex.
	 *
	 * @param that  already allocated <code>VirtualHalfEdge</code> where data are
	 *              copied
	 */
	/* Method never used
	private final AbstractHalfEdge nextApex(AbstractHalfEdge that)
	{
		return next(that).sym().next();
	}
	*/
	
	/**
	 * Move counterclockwise to the following edge with same apex.
	 * @return  current instance after its transformation
	 */
	private AbstractHalfEdge nextApex()
	{
		return next().sym().next();
	}
	
	/*
	 * Copy an <code>VirtualHalfEdge</code> and move it to the clockwise
	 * previous edge which has the same apex.
	 *
	 * @param o     source <code>VirtualHalfEdge</code>
	 * @param that  already allocated <code>VirtualHalfEdge</code> where data are
	 *              copied
	 */
	/* Method never used
	private final AbstractHalfEdge prevApex(AbstractHalfEdge that)
	{
		return prev(that).sym().prev();
	}
	*/
	
	private AbstractHalfEdge prevApex()
	{
		return prev().sym().prev();
	}
	
	/*
	 *                         a
	 *                         ,
	 *                        /|\
	 *                       / | \
	 *              oldLeft /  |  \ oldRight
	 *                     /  v+   \
	 *                    /   / \   \
	 *                   /  /     \  \
	 *                  / /         \ \
	 *               o '---------------` d
	 *                       (this)
	 */
	/**
	 * Splits a triangle into three new triangles by inserting a vertex.
	 *
	 * Two new triangles have to be created, the last one is
	 * updated.  For efficiency reasons, no checks are performed to
	 * ensure that the vertex being inserted is contained by this
	 * triangle.  Once triangles are created, edges are swapped if
	 * they are not Delaunay.
	 *
	 * If edges are not swapped after vertex is inserted, the quality of
	 * newly created triangles has decreased, and the vertex is eventually
	 * not inserted unless the <code>force</code> argument is set to
	 * <code>true</code>.
	 *
	 * Origin and destination points must not be at infinite, which
	 * is the case when current triangle is returned by
	 * getSurroundingTriangle().  If apex is Mesh.outerVertex, then
	 * getSurroundingTriangle() ensures that v.onLeft(o,d) &gt; 0.
	 *
	 * @param v  vertex being inserted.
	 * @param modifiedTriangles  if not null, this set of triangles is updated by adding all triangles modified during this operation.
	 * @param force  if <code>false</code>, the vertex is inserted only if some edges were swapped after its insertion.  If <code>true</code>, the vertex is unconditionnally inserted.
	 * @return number of edges swapped during insertion.  If it is 0, vertex has not been inserted.
	 */
	public final int split3(Mesh2D mesh, Vertex2D v, Set<Triangle> modifiedTriangles, boolean force)
	{
		if (logger.isLoggable(Level.FINE))
			logger.fine("Split VirtualHalfEdge2D "+this+"\nat Vertex "+v);
		Triangle backup = mesh.createTriangle(tri);
		// Aliases
		VirtualHalfEdge2D oldLeft = mesh.poolVH2D[0];
		VirtualHalfEdge2D oldRight = mesh.poolVH2D[1];
		VirtualHalfEdge2D oldSymLeft = null;
		VirtualHalfEdge2D oldSymRight = null;
		
		prevOTri(this, oldLeft);         // = (aod)
		nextOTri(this, oldRight);        // = (dao)
		oldSymLeft = mesh.poolVH2D[2];
		symOTri(oldLeft, oldSymLeft);    // = (oa*)
		oldSymRight = mesh.poolVH2D[3];
		symOTri(oldRight, oldSymRight);  // = (ad*)
		//  Set vertices of newly created and current triangles
		Vertex2D o = (Vertex2D) origin();
		assert o != mesh.outerVertex;
		Vertex2D d = (Vertex2D) destination();
		assert d != mesh.outerVertex;
		Vertex2D a = (Vertex2D) apex();
		
		Triangle t1 = mesh.createTriangle(a, o, v);
		Triangle t2 = mesh.createTriangle(d, a, v);
		VirtualHalfEdge2D newLeft  = new VirtualHalfEdge2D(t1, 2);
		VirtualHalfEdge2D newRight = new VirtualHalfEdge2D(t2, 2);
		if (oldLeft.attributes != 0)
		{
			newLeft.attributes = oldLeft.attributes;
			newLeft.pushAttributes();
			oldLeft.attributes = 0;
			oldLeft.pushAttributes();
		}
		if (oldRight.attributes != 0)
		{
			newRight.attributes = oldRight.attributes;
			newRight.pushAttributes();
			oldRight.attributes = 0;
			oldRight.pushAttributes();
		}
		v.setLink(tri);
		a.setLink(newLeft.tri);
		//  Move apex of current VirtualHalfEdge2D.  As a consequence,
		//  oldLeft is now (vod) and oldRight is changed to (dvo).
		setApex(v);
		
		newLeft.glue(oldSymLeft);
		newRight.glue(oldSymRight);
		
		//  Creates 3 inner links
		newLeft.next();                  // = (ova)
		newLeft.glue(oldLeft);
		newRight.prev();                 // = (vda)
		newRight.glue(oldRight);
		newLeft.next();                  // = (vao)
		newRight.prev();                 // = (avd)
		newLeft.glue(newRight);
		
		//  Data structures have been created, search now for non-Delaunay
		//  edges.  Re-use newLeft to walk through new vertex ring.
		newLeft.next();                  // = (aov)
		Triangle newTri1 = newLeft.tri;
		Triangle newTri2 = newRight.tri;
		if (logger.isLoggable(Level.FINE))
			logger.fine("New triangles:\n"+this+"\n"+newRight+"\n"+newLeft);
		// newRight is reused
		int ret = newLeft.checkAndSwap(mesh, modifiedTriangles, false, newRight);
		if (!force && 0 == ret)
		{
			//  v has been inserted and no edges are swapped,
			//  thus global quality has been decreased.
			//  Remove v in such cases.
			tri.copy(backup);
			o.setLink(tri);
			d.setLink(tri);
			a.setLink(tri);
			nextOTri(this, oldLeft);         // = (dao)
			oldLeft.glue(oldSymRight);
			oldLeft.next();                  // = (aod)
			oldLeft.glue(oldSymLeft);
			return ret;
		}
		mesh.add(newTri1);
		mesh.add(newTri2);
		if (modifiedTriangles != null)
		{
			modifiedTriangles.add(tri);
			modifiedTriangles.add(newTri1);
			modifiedTriangles.add(newTri2);
		}
		mesh.getKdTree().add(v);
		return ret;
	}
	
	//  Called from BasicMesh to improve initial mesh
	public final int checkSmallerAndSwap(Mesh2D mesh)
	{
		//  As checkAndSwap modifies its arguments, 'this'
		//  must be protected.
		VirtualHalfEdge2D ot1 = new VirtualHalfEdge2D();
		VirtualHalfEdge2D sym = new VirtualHalfEdge2D();
		copyOTri(this, ot1);
		return ot1.checkAndSwap(mesh, null, true, sym);
	}
	
	private int checkAndSwap(Mesh2D mesh, Set<Triangle> modifiedTriangles, boolean smallerDiag, VirtualHalfEdge2D sym)
	{
		int nrSwap = 0;
		int totNrSwap = 0;
		Vertex2D v = (Vertex2D) apex();
		assert v != mesh.outerVertex;
		//  Loops around v
		Vertex2D first = (Vertex2D) origin();
		while (true)
		{
			if (canSwap(mesh, v, smallerDiag, sym))
			{
				if (modifiedTriangles != null)
				{
					modifiedTriangles.add(tri);
					modifiedTriangles.add(sym.tri);
				}
				swap(mesh);
				nrSwap++;
				totNrSwap++;
			}
			else
			{
				// This routine may be called before boundaries
				// are recreated, so VirtualHalfEdge.nextApexLoop
				// is not relevant here.
				nextApexLoopNoBoundaries(mesh);
				if (origin() == first)
				{
					// If no swap has been performed, processing is over
					if (nrSwap == 0)
						break;
					nrSwap = 0;
				}
			}
		}
		return totNrSwap;
	}
	
	private boolean canSwap(Mesh2D mesh, Vertex2D v, boolean smallerDiag, VirtualHalfEdge2D sym)
	{
		if (hasAttributes(BOUNDARY | NONMANIFOLD | OUTER))
			return false;
		Vertex2D o = (Vertex2D) origin();
		Vertex2D d = (Vertex2D) destination();
		symOTri(this, sym);
		Vertex2D a = (Vertex2D) sym.apex();
		if (o == mesh.outerVertex)
			return (v.onLeft(mesh.getKdTree(), d, a) < 0L);
		if (d == mesh.outerVertex)
			return (v.onLeft(mesh.getKdTree(), a, o) < 0L);
		if (a == mesh.outerVertex)
			return (v.onLeft(mesh.getKdTree(), o, d) == 0L);
		if (!smallerDiag)
			return !isDelaunay(mesh, a);
		return !a.isSmallerDiagonale(mesh, this);
	}
	
	private void nextApexLoopNoBoundaries(Mesh2D mesh)
	{
		if (destination() == mesh.outerVertex)
		{
			// Loop clockwise to another boundary
			// and start again from there.
			do
			{
				prevApex();
			}
			while (origin() != mesh.outerVertex);
		}
		else
			nextApex();
	}

	/**
	 * Tries to rebuild a boundary edge by swapping edges.
	 *
	 * This routine is applied to an oriented triangle, its origin
	 * is an end point of the boundary edge to rebuild.  The other end
	 * point is passed as an argument.  Current oriented triangle has
	 * been set up by calling routine so that it is the leftmost edge
	 * standing to the right of the boundary edge.
	 * A traversal between end points is performed, and intersected
	 * edges are swapped if possible.  At exit, current oriented
	 * triangle has <code>end</code> as its origin, and is the
	 * rightmost edge standing to the left of the inverted edge.
	 * This algorithm can then be called iteratively back and forth,
	 * and it is known that it is guaranteed to finish.
	 *
	 * @param end  end point of the boundary edge.
	 * @return the number of intersected edges.
	 */
	final int forceBoundaryEdge(Mesh2D mesh, Vertex2D end)
	{
		long newl, oldl;
		int count = 0;
		
		Vertex2D start = (Vertex2D) origin();
		assert start != mesh.outerVertex;
		assert end != mesh.outerVertex;
		KdTree kdTree = mesh.getKdTree();

		next();
		while (true)
		{
			count++;
			Vertex2D o = (Vertex2D) origin();
			Vertex2D d = (Vertex2D) destination();
			Vertex2D a = (Vertex2D) apex();
			assert a != mesh.outerVertex : ""+this;
			symOTri(this, mesh.poolVH2D[0]);
			mesh.poolVH2D[0].next();
			Vertex2D n = (Vertex2D) mesh.poolVH2D[0].destination();
			assert n != mesh.outerVertex : ""+mesh.poolVH2D[0];
			newl = n.onLeft(kdTree, start, end);
			oldl = a.onLeft(kdTree, start, end);
			boolean toSwap = (n != mesh.outerVertex) && (a.onLeft(kdTree, n, d) > 0L) && (a.onLeft(kdTree, o, n) > 0L) && !hasAttributes(BOUNDARY);
			if (newl > 0L)
			{
				//  o stands to the right of (start,end), d and n to the left.
				if (!toSwap)
					prevOrigin();        // = (ond)
				else if (oldl >= 0L)
				{
					//  a stands to the left of (start,end).
					swap(mesh);          // = (ona)
				}
				else if (mesh.rand.nextBoolean())
					swap(mesh);          // = (ona)
				else
					prevOrigin();        // = (ond)
			}
			else if (newl < 0L)
			{
				//  o and n stand to the right of (start,end), d to the left.
				if (!toSwap)
					nextDest();          // = (ndo)
				else if (oldl <= 0L)
				{
					//  a stands to the right of (start,end).
					swap(mesh);          // = (ona)
					next();              // = (nao)
					prevOrigin();        // = (nda)
				}
				else if (mesh.rand.nextBoolean())
				{
					swap(mesh);          // = (ona)
					next();              // = (nao)
					prevOrigin();        // = (nda)
				}
				else
					nextDest();          // = (ndo)
			}
			else
			{
				//  n is the end point.
				if (!toSwap)
					nextDest();          // = (ndo)
				else
				{
					swap(mesh);          // = (ona)
					next();              // = (nao)
					if (oldl < 0L)
						prevOrigin();// = (nda)
				}
				break;
			}
		}
		if (origin() != end)
		{
			//  A midpoint is aligned with start and end, this should
			//  never happen.
			throw new InvalidFaceException("Point "+origin()+" is aligned with "+start+" and "+end);
		}
		return count;
	}
	
	/**
	 * Checks whether an edge is Delaunay.
	 *
	 * @param apex2  apex of the symmetric edge
	 * @return <code>true</code> if edge is Delaunay, <code>false</code>
	 * otherwise.
	 */
	public final boolean isDelaunay(Mesh2D mesh, Vertex2D apex2)
	{
		if (apex2.isPseudoIsotropic(mesh))
			return isDelaunay_isotropic(mesh, apex2);
		return isDelaunay_anisotropic(mesh, apex2);
	}
	
	private boolean isDelaunay_isotropic(Mesh2D mesh, Vertex2D apex2)
	{
		assert mesh.outerVertex != origin();
		assert mesh.outerVertex != destination();
		assert mesh.outerVertex != apex();
		Vertex2D vA = (Vertex2D) origin();
		Vertex2D vB = (Vertex2D) destination();
		Vertex2D v1 = (Vertex2D) apex();
		KdTree kdTree = mesh.getKdTree();
		long tp1 = vA.onLeft(kdTree, vB, v1);
		long tp2 = vB.onLeft(kdTree, vA, apex2);
		long tp3 = apex2.onLeft(kdTree, vB, v1);
		long tp4 = v1.onLeft(kdTree, vA, apex2);
		if (Math.abs(tp3) + Math.abs(tp4) < Math.abs(tp1)+Math.abs(tp2) )
			return true;
		if (tp1 > 0L && tp2 > 0L)
		{
			if (tp3 <= 0L || tp4 <= 0L)
				return true;
		}
		return !apex2.inCircle2D(mesh, this);
	}
	
	private boolean isDelaunay_anisotropic(Mesh2D mesh, Vertex2D apex2)
	{
		assert mesh.outerVertex != origin();
		assert mesh.outerVertex != destination();
		assert mesh.outerVertex != apex();
		if (apex2 == mesh.outerVertex)
			return true;
		return !apex2.inCircle(mesh, this);
	}
	
}
