/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2005
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

import java.util.Random;
import org.apache.log4j.Logger;

/**
 * A handle to abstract edge objects for initial 2D mesh.
 * This class implements some features which are only relevant
 * to the initial 2D mesh.  In particular, boundary edges have
 * not yet been rebuilt, so we cannot check whether the OUTER
 * attribute is set but need to test if vertices are equal to
 * the infinite point instead.
 */
public class OTriangle2D extends OTriangle
{
	private static Logger logger = Logger.getLogger(OTriangle2D.class);
	private static final Random rand = new Random(139L);
	private double [] tempD = new double[3];
	private double [] tempD1 = new double[3];
	private double [] tempD2 = new double[3];
	private static final OTriangle2D otVoid = new OTriangle2D();
	private static OTriangle2D [] work = new OTriangle2D[4];
	static {
		for (int i = 0; i < 4; i++)
			work[i] = new OTriangle2D();
	}
	
	public OTriangle2D()
	{
		super();
	}
	
	/**
	 * Create an object to handle data about a triangle.
	 *
	 * @param t  geometrical triangle.
	 * @param o  a number between 0 and 2 determining an edge.
	 */
	public OTriangle2D(Triangle t, int o)
	{
		super(t, o);
	}
	
	/**
	 * Collapse an edge and update adjacency relations.
	 * Its start and end points must have the same location.
	 */
	public final void removeDegenerated()
	{
		Vertex o = origin();
		Vertex d = destination();
		assert o.getRef() != 0 && d.getRef() != 0 && o.getRef() ==  d.getRef();
		
		//  Replace o by d in all triangles
		copyOTri(this, work[0]);
		do
		{
			work[0].nextOTriOriginLoop();
			for (int i = 0; i < 3; i++)
			{
				if (work[0].tri.vertex[i] == o)
				{
					work[0].tri.vertex[i] = d;
					break;
				}
			}
		}
		while (work[0].destination() != d);
		o.removeFromQuadTree();
		
		//  Glue triangles
		nextOTri(this, work[0]);
		prevOTri(this, work[1]);
		work[0].symOTri();
		work[1].symOTri();
		work[0].glue(work[1]);
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
	 * getSurroundingTriangle().  If apex is Vertex.outer, then
	 * getSurroundingTriangle() ensures that v.onLeft(o,d) &gt; 0.
	 *
	 * @param v  the vertex being inserted.
	 * @param force  if <code>false</code>, the vertex is inserted only if some edges were swapped after its insertion.  If <code>true</code>, the vertex is unconditionnally inserted.
	 * @return <code>true</code> if vertex was successfully added, <code>false</code> otherwise.
	 */
	public final boolean split3(Vertex v, boolean force)
	{
		if (logger.isDebugEnabled())
			logger.debug("Split OTriangle2D "+this+"\nat Vertex "+v);
		int savedAdjPos = tri.adjPos;
		// Aliases
		OTriangle2D oldLeft = work[0];
		OTriangle2D oldRight = work[1];
		OTriangle2D oldSymLeft = null;
		OTriangle2D oldSymRight = null;
		
		prevOTri(this, oldLeft);         // = (aod)
		nextOTri(this, oldRight);        // = (dao)
		oldSymLeft = work[2];
		symOTri(oldLeft, oldSymLeft);    // = (oa*)
		oldSymRight = work[3];
		symOTri(oldRight, oldSymRight);  // = (ad*)
		//  Set vertices of newly created and current triangles
		Vertex o = origin();
		assert o != Vertex.outer;
		Vertex d = destination();
		assert d != Vertex.outer;
		Vertex a = apex();
		
		OTriangle2D newLeft  = new OTriangle2D(new Triangle(a, o, v), 2);
		OTriangle2D newRight = new OTriangle2D(new Triangle(d, a, v), 2);
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
		Triangle iniTri = tri;
		v.setLink(tri);
		a.setLink(newLeft.tri);
		//  Move apex of current OTriangle2D.  As a consequence,
		//  oldLeft is now (vod) and oldRight is changed to (dvo).
		setApex(v);
		
		newLeft.glue(oldSymLeft);
		newRight.glue(oldSymRight);
		
		//  Creates 3 inner links
		newLeft.nextOTri();              // = (ova)
		newLeft.glue(oldLeft);
		newRight.prevOTri();             // = (vda)
		newRight.glue(oldRight);
		newLeft.nextOTri();              // = (vao)
		newRight.prevOTri();             // = (avd)
		newLeft.glue(newRight);
		
		//  Data structures have been created, search now for non-Delaunay
		//  edges.  Re-use newLeft to walk through new vertex ring.
		newLeft.nextOTri();              // = (aov)
		Triangle newTri1 = newLeft.tri;
		Triangle newTri2 = newRight.tri;
		if (logger.isDebugEnabled())
			logger.debug("New triangles:\n"+this+"\n"+newRight+"\n"+newLeft);
		if (force)
			newLeft.CheckAndSwap(false);
		else if (0 == newLeft.CheckAndSwap(false))
		{
			//  v has been inserted and no edges are swapped,
			//  thus global quality has been decreased.
			//  Remove v in such cases.
			o.setLink(iniTri);
			d.setLink(iniTri);
			a.setLink(iniTri);
			setApex(a);
			tri.adjPos = savedAdjPos;
			nextOTri(this, oldLeft);         // = (dao)
			oldLeft.glue(oldSymRight);
			oldLeft.nextOTri();              // = (aod)
			oldLeft.glue(oldSymLeft);
			return false;
		}
		newTri1.addToMesh();
		newTri2.addToMesh();
		return true;
	}
	
	//  Called from BasicMesh to improve initial mesh
	public int checkSmallerAndSwap()
	{
		//  As CheckAndSwap modifies its arguments, 'this'
		//  must be protected.
		OTriangle2D ot1 = new OTriangle2D();
		copyOTri(this, ot1);
		return ot1.CheckAndSwap(true);
	}
	
	private int CheckAndSwap(boolean smallerDiag)
	{
		int nrSwap = 0;
		int totNrSwap = 0;
		Vertex v = apex();
		assert v != Vertex.outer;
		OTriangle2D sym = new OTriangle2D();
		//  Loops around v
		Vertex first = origin();
		while (true)
		{
			if (hasAttributes(BOUNDARY) || hasAttributes(NONMANIFOLD) || hasAttributes(OUTER))
			{
				nextOTriApexLoop();
				if (origin() == first)
				{
					if (nrSwap == 0)
						break;
					nrSwap = 0;
				}
			}
			else
			{
				Vertex o = origin();
				Vertex d = destination();
				symOTri(this, sym);
				Vertex a = sym.apex();
				boolean toSwap = false;
				if (o == Vertex.outer)
					toSwap = (v.onLeft(d, a) < 0L);
				else if (d == Vertex.outer)
					toSwap = (v.onLeft(a, o) < 0L);
				else if (a == Vertex.outer)
					toSwap = (v.onLeft(o, d) == 0L);
				else if (isMutable())
				{
					if (!smallerDiag)
						toSwap = !isDelaunay(a);
					else
						toSwap = !a.isSmallerDiagonale(this);
				}
				if (toSwap)
				{
					swap();
					nrSwap++;
					totNrSwap++;
				}
				else
				{
					nextOTriApexLoop();
					if (origin() == first)
					{
						if (nrSwap == 0)
							break;
						nrSwap = 0;
					}
				}
			}
		}
		return totNrSwap;
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
	public final int forceBoundaryEdge(Vertex end)
	{
		long newl, oldl;
		int count = 0;
		
		Vertex start = origin();
		assert start != Vertex.outer;
		assert end != Vertex.outer;

		nextOTri();
		while (true)
		{
			count++;
			Vertex o = origin();
			Vertex d = destination();
			Vertex a = apex();
			assert a != Vertex.outer : ""+this;
			symOTri(this, work[0]);
			work[0].nextOTri();
			Vertex n = work[0].destination();
			assert n != Vertex.outer : ""+work[0];
			newl = n.onLeft(start, end);
			oldl = a.onLeft(start, end);
			boolean toSwap = (n != Vertex.outer) && (a.onLeft(n, d) > 0L) && (a.onLeft(o, n) > 0L) && !hasAttributes(BOUNDARY);
			if (newl > 0L)
			{
				//  o stands to the right of (start,end), d and n to the left.
				if (!toSwap)
					prevOTriOrigin();    // = (ond)
				else if (oldl >= 0L)
				{
					//  a stands to the left of (start,end).
					swap();              // = (ona)
				}
				else if (rand.nextBoolean())
					swap();              // = (ona)
				else
					prevOTriOrigin();    // = (ond)
			}
			else if (newl < 0L)
			{
				//  o and n stand to the right of (start,end), d to the left.
				if (!toSwap)
					nextOTriDest();      // = (ndo)
				else if (oldl <= 0L)
				{
					//  a stands to the right of (start,end).
					swap();              // = (ona)
					nextOTri();          // = (nao)
					prevOTriOrigin();    // = (nda)
				}
				else if (rand.nextBoolean())
				{
					swap();              // = (ona)
					nextOTri();          // = (nao)
					prevOTriOrigin();    // = (nda)
				}
				else
					nextOTriDest();      // = (ndo)
			}
			else
			{
				//  n is the end point.
				if (!toSwap)
					nextOTriDest();      // = (ndo)
				else
				{
					swap();              // = (ona)
					nextOTri();          // = (nao)
					if (oldl < 0L)
						prevOTriOrigin();// = (nda)
				}
				break;
			}
		}
		if (origin() != end)
		{
			//  A midpoint is aligned with start and end, this should
			//  never happen.
			throw new RuntimeException("Point "+origin()+" is aligned with "+start+" and "+end);
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
	public final boolean isDelaunay(Vertex apex2)
	{
		if (apex2.isPseudoIsotropic())
			return isDelaunay_isotropic(apex2);
		return isDelaunay_anisotropic(apex2);
	}
	
	private final boolean isDelaunay_isotropic(Vertex apex2)
	{
		assert Vertex.outer != origin();
		assert Vertex.outer != destination();
		assert Vertex.outer != apex();
		Vertex vA = origin();
		Vertex vB = destination();
		Vertex v1 = apex();
		long tp1 = vA.onLeft(vB, v1);
		long tp2 = vB.onLeft(vA, apex2);
		long tp3 = apex2.onLeft(vB, v1);
		long tp4 = v1.onLeft(vA, apex2);
		if (Math.abs(tp3) + Math.abs(tp4) < Math.abs(tp1)+Math.abs(tp2) )
			return true;
		if (tp1 > 0L && tp2 > 0L)
		{
			if (tp3 <= 0L || tp4 <= 0L)
				return true;
		}
		return !apex2.inCircleTest2(this);
	}
	
	private final boolean isDelaunay_anisotropic(Vertex apex2)
	{
		assert Vertex.outer != origin();
		assert Vertex.outer != destination();
		assert Vertex.outer != apex();
		if (apex2 == Vertex.outer)
			return true;
		return !apex2.inCircleTest3(this);
	}
	
}
