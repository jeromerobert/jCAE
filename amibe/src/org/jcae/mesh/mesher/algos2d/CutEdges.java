/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2003 Jerome Robert <jeromerobert@users.sourceforge.net>

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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh.mesher.algos2d;

import org.jcae.mesh.mesher.ds.*;
import java.util.Iterator;
import java.util.HashSet;
import org.apache.log4j.Logger;

/**
 * Abstract class to refine edges.  When refining edges, care must be taken
 * to ensure mesh validity.  This is a 2=step process:
 * <ol>
 *   <li>Compute middle points for all edges being refined.</li>
 *   <li>For all triangles, check if its edges are marked, and if
 *       so computes a new triangulation.</li>
 * </ol>
 * This class implements the {@link #computeFace second step},
 * derived classes must implement the {@link #compute first one}.
 */

public abstract class CutEdges
{
	private static Logger logger=Logger.getLogger(CutEdges.class);
	
	/** 
	 * Set edges to be cut.
	 * Derived classes must call {@link MEdge2D#setMidPoint} for each edge
	 * to be cut.
	 */
	public abstract void compute();
	
	/** 
	 * Cut marked edges.
	 * For all triangles in the current mesh, checks if its edges have been
	 * marked, and performs a new triangulation based on which edges are being
	 * cut.
	 *
	 * @param submesh2d  the mesh being updated.
	 * @return the set of newly created edges.
	 */
	public static HashSet computeFace(SubMesh2D submesh2d)
	{
		assert (null != submesh2d);
		HashSet newedges = new HashSet();
		HashSet faceset = submesh2d.getFaces();
		//  Create a copy to iterate over
		HashSet oldfaceset = new HashSet(faceset);
		for (Iterator it=oldfaceset.iterator(); it.hasNext(); )
		{
			MFace2D face = (MFace2D) it.next();
			MEdge2D [] e = new MEdge2D[3];
			int count = 0, first = 0, last = 2;
			for (Iterator ite=face.getEdgesIterator(); ite.hasNext(); )
			{
				MEdge2D edge = (MEdge2D) ite.next();
				if (null != edge.getMidPoint())
				{
					count++;
					e[first] = edge;
					first++;
				} else {
					e[last] = edge;
					last--;
				}
			}
			if (1 == count)
				cutOneEdge(submesh2d, face, e, newedges);
			else if (2 == count)
				cutTwoEdges(submesh2d, face, e, newedges);
			else if (3 == count)
				cutThreeEdges(submesh2d, face, e, newedges);
			else
			{
				assert (0 == count);
			}
		}
		assert (submesh2d.isValid());
		return newedges;
	}
	
	private static void cutOneEdge(SubMesh2D submesh2d, MFace2D face, MEdge2D [] e, HashSet newedges)
	{
		//  Cut e[0]
		assert (e[0].isMutable());
		MNode2D pt1 = e[0].getNodes1();
		MNode2D pt2 = e[0].getNodes2();
		MNode2D mid = e[0].getMidPoint();
		submesh2d.addNode(mid);
		MNode2D apex;
		if (e[1].getNodes1() == pt1 || e[1].getNodes1() == pt2)
			apex = e[1].getNodes2();
		else
			apex = e[1].getNodes1();
		if (e[1].getNodes1() == pt1 || e[1].getNodes2() == pt1)
		{
			MEdge2D tmp = e[1];
			e[1] = e[2];
			e[2] = tmp;
		}
		/* Now
		     e[0] = (pt1, pt2)
		     e[1] = (pt2, apex)
		     e[2] = (apex, pt1)
		 */
		MEdge2D ne1 = submesh2d.addEdgeIfNotDefined(pt1, mid);
		MEdge2D ne2 = submesh2d.addEdgeIfNotDefined(pt2, mid);
		//  This is an interior edge, it could not exist previously
		MEdge2D ne3 = new MEdge2D(apex, mid);
		submesh2d.addEdge(ne3);
		MFace2D nt1 = new MFace2D(ne1,ne3,e[2]);
		MFace2D nt2 = new MFace2D(ne2,ne3,e[1]);
		submesh2d.addFace(nt1);
		submesh2d.addFace(nt2);
		assert (nt1.isValid()) : nt1;
		assert (nt2.isValid()) : nt2;
		submesh2d.rmFace(face);
		newedges.add(ne1);
		newedges.add(ne2);
		newedges.add(ne3);
	}
	
	private static void cutTwoEdges(SubMesh2D submesh2d, MFace2D face, MEdge2D [] e, HashSet newedges)
	{
		//  Cut e[0] and e[1], keep e[2]
		assert (e[0].isMutable());
		assert (e[1].isMutable());
		MNode2D mid1 = e[0].getMidPoint();
		MNode2D mid2 = e[1].getMidPoint();
		submesh2d.addNode(mid1);
		submesh2d.addNode(mid2);
		MNode2D pt1, pt2, pt3;
		/*  Build pt1, pt2, pt3 so that
		     e[0] = (pt1, pt2)
		     e[1] = (pt2, pt3)
		     e[2] = (pt3, pt1)
		 */
		if (e[0].getNodes1() == e[1].getNodes1())
		{
			pt1 = e[0].getNodes2();
			pt2 = e[0].getNodes1();
			pt3 = e[1].getNodes2();
		}
		else if (e[0].getNodes1() == e[1].getNodes2())
		{
			pt1 = e[0].getNodes2();
			pt2 = e[0].getNodes1();
			pt3 = e[1].getNodes1();
		}
		else if (e[0].getNodes2() == e[1].getNodes1())
		{
			pt1 = e[0].getNodes1();
			pt2 = e[0].getNodes2();
			pt3 = e[1].getNodes2();
		}
		else
		{
			assert (e[0].getNodes2() == e[1].getNodes2());
			pt1 = e[0].getNodes1();
			pt2 = e[0].getNodes2();
			pt3 = e[1].getNodes1();
		}
		//  Construct new edges
		MEdge2D ne1 = submesh2d.addEdgeIfNotDefined(pt1, mid1);
		MEdge2D ne2 = submesh2d.addEdgeIfNotDefined(mid1, pt2);
		MEdge2D ne3 = submesh2d.addEdgeIfNotDefined(pt2, mid2);
		MEdge2D ne4 = submesh2d.addEdgeIfNotDefined(mid2, pt3);
		MEdge2D ne5 = new MEdge2D(mid1, mid2);
		submesh2d.addEdge(ne5);
		MFace2D nt1 = new MFace2D(ne2,ne3,ne5);

		MEdge2D ne6 = new MEdge2D(mid1, pt3);
		MEdge2D ne7 = new MEdge2D(mid2, pt1);
		MFace2D nt2, nt3, nt4, nt5;
		nt2 = new MFace2D(e[2],ne7,ne4);
		nt3 = new MFace2D(ne1,ne5,ne7);
		nt4 = new MFace2D(ne4,ne6,ne5);
		nt5 = new MFace2D(ne1,ne6,e[2]);
		if (Math.min(submesh2d.compGeom().quality(nt4), submesh2d.compGeom().quality(nt5)) <
		    Math.min(submesh2d.compGeom().quality(nt2), submesh2d.compGeom().quality(nt3)))
		{
			ne6 = ne7;
			nt4.unlink();
			nt5.unlink();
		}
		else
		{
			nt2.unlink();
			nt3.unlink();
			nt2 = nt4;
			nt3 = nt5;
		}
		submesh2d.addEdge(ne6);
		submesh2d.addFace(nt1);
		submesh2d.addFace(nt2);
		submesh2d.addFace(nt3);
		assert (nt1.isValid()) : nt1;
		assert (nt2.isValid()) : nt2;
		assert (nt3.isValid()) : nt3;
		submesh2d.rmFace(face);
	}
	
	private static void cutThreeEdges(SubMesh2D submesh2d, MFace2D face, MEdge2D [] e, HashSet newedges)
	{
		//  Cut all edges => replace f by 4 triangles
		assert (e[0].isMutable());
		assert (e[1].isMutable());
		assert (e[2].isMutable());
		MNode2D mid1 = e[0].getMidPoint();
		MNode2D mid2 = e[1].getMidPoint();
		MNode2D mid3 = e[2].getMidPoint();
		submesh2d.addNode(mid1);
		submesh2d.addNode(mid2);
		submesh2d.addNode(mid3);
		MNode2D pt1, pt2, pt3;
		/*  Build pt1, pt2, pt3 so that
		     e[0] = (pt1, pt2)
		     e[1] = (pt2, pt3)
		     e[2] = (pt3, pt1)
		 */
		if (e[0].getNodes1() == e[1].getNodes1())
		{
			pt1 = e[0].getNodes2();
			pt2 = e[0].getNodes1();
			pt3 = e[1].getNodes2();
		}
		else if (e[0].getNodes1() == e[1].getNodes2())
		{
			pt1 = e[0].getNodes2();
			pt2 = e[0].getNodes1();
			pt3 = e[1].getNodes1();
		}
		else if (e[0].getNodes2() == e[1].getNodes1())
		{
			pt1 = e[0].getNodes1();
			pt2 = e[0].getNodes2();
			pt3 = e[1].getNodes2();
		}
		else
		{
			assert (e[0].getNodes2() == e[1].getNodes2());
			pt1 = e[0].getNodes1();
			pt2 = e[0].getNodes2();
			pt3 = e[1].getNodes1();
		}
		//  Construct new edges
		MEdge2D ne1 = submesh2d.addEdgeIfNotDefined(pt1, mid1);
		MEdge2D ne2 = new MEdge2D(mid1, mid3);
		submesh2d.addEdge(ne2);
		MEdge2D ne3 = submesh2d.addEdgeIfNotDefined(mid3, pt1);
		MEdge2D ne4 = submesh2d.addEdgeIfNotDefined(pt2, mid2);
		MEdge2D ne5 = new MEdge2D(mid2, mid1);
		submesh2d.addEdge(ne5);
		MEdge2D ne6 = submesh2d.addEdgeIfNotDefined(mid1, pt2);
		MEdge2D ne7 = submesh2d.addEdgeIfNotDefined(pt3, mid3);
		MEdge2D ne8 = new MEdge2D(mid3, mid2);
		submesh2d.addEdge(ne8);
		MEdge2D ne9 = submesh2d.addEdgeIfNotDefined(mid2, pt3);
		
		MFace2D nt1 = new MFace2D(ne1,ne2,ne3);
		MFace2D nt2 = new MFace2D(ne4,ne5,ne6);
		MFace2D nt3 = new MFace2D(ne7,ne8,ne9);
		MFace2D nt4 = new MFace2D(ne2,ne8,ne5);
		submesh2d.addFace(nt1);
		submesh2d.addFace(nt2);
		submesh2d.addFace(nt3);
		submesh2d.addFace(nt4);
		assert (nt1.isValid()) : nt1;
		assert (nt2.isValid()) : nt2;
		assert (nt3.isValid()) : nt3;
		assert (nt4.isValid()) : nt4;
		submesh2d.rmFace(face);
		newedges.add(ne1);
		newedges.add(ne2);
		newedges.add(ne3);
		newedges.add(ne4);
		newedges.add(ne5);
		newedges.add(ne6);
		newedges.add(ne7);
		newedges.add(ne8);
		newedges.add(ne9);
	}
}
