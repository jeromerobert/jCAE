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

import org.jcae.mesh.cad.CADGeomSurface;
import org.jcae.mesh.mesher.ds.*;
import java.util.Iterator;
import java.util.HashSet;
import org.apache.log4j.Logger;

/**
 * Deletes small edges.
 */
public class CollapseEdges
{
	private static Logger logger=Logger.getLogger(CollapseEdges.class);
	private SubMesh2D submesh2d = null;
	private boolean force = true;
	
	/**
	 * Creates a <code>CollapseEdges</code> instance.
	 *
	 * @param m  the <code>SubMesh2D</code> instance to refine.
	 */
	public CollapseEdges(SubMesh2D m)
	{
		submesh2d = m;
	}
	
	/**
	 * Creates a <code>CollapseEdges</code> instance.
	 *
	 * @param m  the <code>SubMesh2D</code> instance to refine.
	 * @param force  true if edge is always collapsed, false if it is
	 *               collapsed only when improving triangle quality.
	 */
	public CollapseEdges(SubMesh2D m, boolean f)
	{
		submesh2d = m;
		force = f;
	}
	
	/**
	 * Removes all edges which are smaller than 1.
	 * Middle points of all mutable edges smaller 1 are computed, and
	 * {@link #computeFace} is called.  When an edge is removed, other
	 * edges in its vicinity are rearranged, so it is not guaranteed
	 * that all edges are collapsed.
	 *
	 * @see #computeFace
	 */
	public boolean compute()
	{
		int ne = 0;
		for(Iterator it=submesh2d.getEdgesIterator(); it.hasNext(); )
		{
			MEdge2D e = (MEdge2D) it.next();
			if (e.isMutable() && submesh2d.compGeom().length(e) < 1.0)
			{
				ne++;
				e.setMidPoint();
			}
			else
				e.resetMidPoint();
		}
		logger.debug("  Edges to collapse: "+ne);
		if (ne > 0)
		{
			ne = computeFace(submesh2d, force).size();
		}
		assert (submesh2d.isValid());
		return (ne > 0);
	}
	
	/**
	 * Removes all marked edges and computes new triangulation.
	 * By convention, caller marks edges being removed by computing their
	 * middle point (see {@link MEdge2D#setMidPoint}).  
	 *
	 * @param submesh2d  the mesh being updated.
	 * @return the set of newly created edges.
	 */
	public static HashSet computeFace(SubMesh2D submesh2d, boolean force)
	{
		//  Create a copy to iterate over
		HashSet oldedgeset = new HashSet();
		HashSet edgesToSwap = new HashSet();
		int nec = 0, neg = 0, nen = 0;
		for (Iterator it=submesh2d.getEdgesIterator(); it.hasNext(); )
		{
			MEdge2D e = (MEdge2D) it.next();
			if (null != e.getMidPoint())
				oldedgeset.add(e);
		}
		if (oldedgeset.isEmpty())
			return edgesToSwap;
		
		for (Iterator it=oldedgeset.iterator(); it.hasNext(); )
		{
			MEdge2D e = (MEdge2D) it.next();
			//  This edge might have been removed by previous rearrangements
			if (!submesh2d.getEdges().contains(e))
			{
				nen++;
				continue;
			}
			HashSet newedges = collapse(submesh2d, e, force);
			if (null != newedges && !newedges.isEmpty())
			{
				edgesToSwap.addAll(newedges);
				nec++;
			}
			else
				neg++;
		}
		logger.debug("  Edges collapsed: "+nec);
		logger.debug("  Edges no more present: "+nen);
		logger.debug("  Edges not allowed to collapse: "+neg);
		assert (submesh2d.isValid());
		return edgesToSwap;
	}
	
	private static HashSet collapse(SubMesh2D submesh2d, MEdge2D edge, boolean force)
	{
		assert (edge.isMutable());
		//  First check that edge collapse does preserve topology
		MNode2D pt1 = edge.getNodes1();
		MNode2D pt2 = edge.getNodes2();
		if (!pt1.isMutable() && !pt2.isMutable())
			return null;
		Iterator it = edge.getFacesIterator();
		CADGeomSurface surf = submesh2d.getGeomSurface();
		
		HashSet nodes1 = pt1.getNeighboursNodes();
		HashSet nodes2 = pt2.getNeighboursNodes();
		nodes1.retainAll(nodes2);
		MNode2D apex = ((MFace2D) it.next()).apex(edge);
		nodes1.remove(apex);
		apex = ((MFace2D) it.next()).apex(edge);
		nodes1.remove(apex);
		if (0 != nodes1.size())
			return null;
		
		//  Save faces connected to pt1 or pt2 in a HashSet
		HashSet facesConnected = new HashSet(pt1.getElements2D());
		facesConnected.addAll(pt2.getElements2D());

		// Retrieve the edges connected to pt1 or pt2
		HashSet edgesConnected = new HashSet();
		// Retrieve the edges not connected to pt1 or pt2
		HashSet edgesNotConnected = new HashSet();
		Iterator itfc = facesConnected.iterator();
		while(itfc.hasNext())
		{
			MFace2D f = (MFace2D) itfc.next();
			Iterator ittemp = f.getEdgesIterator();
			while (ittemp.hasNext())
			{
				MEdge2D e = (MEdge2D) ittemp.next();
				MNode2D e1 = e.getNodes1();
				MNode2D e2 = e.getNodes2();
				if (e1 != pt1 && e1 != pt2 && e2 != pt1 && e2 != pt2)
				{
					//  Do not collapse if it reduces the connectivity
					//  of a degenerated node
					if (null != pt1.getRef() && pt1.getRef().isDegenerated())
						return null;
					if (null != pt2.getRef() && pt2.getRef().isDegenerated())
						return null;
					//  Do not collapse if hull is not starred
					if (pt1.orient2d(e1, e2, surf) * pt2.orient2d(e1, e2, surf) <= 0.0)
						return null;
					edgesNotConnected.add(e);
				}
				else
					edgesConnected.add(e);
			}
		}

		// Computes new point
		MNode2D np;

		assert (pt1.isMutable() || pt2.isMutable());
		if (pt1.isMutable() && pt2.isMutable())
		{
			edge.setMidPoint();
			np = edge.getMidPoint();
			np = submesh2d.addNode(np);
		}
		else
		{
			if (!pt1.isMutable())
				np = pt1;
			else
				np = pt2;
			for (Iterator ite = edgesNotConnected.iterator(); ite.hasNext(); )
			{
				MEdge2D e = (MEdge2D) ite.next();
				//  Do not build a triangle which will not be mutable
				if (!e.isMutable())
					return null;
			}
		}
		
		// Build new faces
		HashSet ret = new HashSet();
		for (Iterator ite = edgesNotConnected.iterator(); ite.hasNext(); )
		{
			MEdge2D e = (MEdge2D) ite.next();
			submesh2d.addNode(e.getNodes1());
			submesh2d.addNode(e.getNodes2());
			submesh2d.addTriangle(e.getNodes1(),np,e.getNodes2());
			ret.add(e);
		}
		// Destroy previous faces
		submesh2d.rmFaces(facesConnected);
		return ret;
	}
	
}
