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

package org.jcae.mesh.mesher.ds.tools;

import org.jcae.mesh.mesher.ds.*;
import org.jcae.mesh.mesher.metrics.Metric2D;
import org.jcae.mesh.cad.CADGeomSurface;
import java.util.Iterator;
import java.util.HashSet;
import org.apache.log4j.Logger;

public class Edge2DSwapper
{
	private static Logger logger=Logger.getLogger(Edge2DSwapper.class);	

	//  The SubMesh2D instance on which methods are applied
	private SubMesh2D submesh2d;
	
	/**
	 * Constructor.
	 *
	 * @param  m   the <code>SubMesh2D</code> being modified.
	 */
	public Edge2DSwapper(SubMesh2D m)
	{
		submesh2d = m;
	}
	
	/**
	 * Flips an edge if it locally improves mesh quality.
	 *
	 * @param  e          the edge to flip,
	 * @param  oldT       n array containing old triangles,
	 * @param  forceFlip  <code>true</code> if edge is always flipped,
	 *         <code>false</code> if it is flipped only when triangles have
	 *         better qualities.
	 * @return the two triangles bounding this edge.
	 */
	public MFace2D [] swap(MEdge2D e, MFace2D [] oldT, boolean forceFlip)
	{
		if (oldT.length != 2)
			throw new java.lang.RuntimeException("Invalid argument");
		
		CADGeomSurface surf = submesh2d.getGeomSurface();
		MNode2D apex1 = oldT[0].apex(e);
		assert null != apex1 : "swap: apex1 not found";
		MNode2D apex2 = oldT[1].apex(e);
		assert null != apex2 : "swap: apex2 not found";
		
		MNode2D e_p1 = e.getNodes1();
		MNode2D e_p2 = e.getNodes2();
		// Do not swap if polygon is not convex
		if (e_p1.orient2d(apex1, apex2, surf) * e_p2.orient2d(apex1, apex2, surf) >= 0.0)
			return null;
		if (!forceFlip)
		{
			if (!apex2.inCircle(e_p1, e_p2, apex1, surf))
				return null;
		}
		
		int before = submesh2d.getEdges().size();
		MFace2D [] newT = new MFace2D[2];
		newT[0] = submesh2d.addTriangle(apex1, e_p1, apex2);
		newT[1] = submesh2d.addTriangle(apex2, e_p2, apex1);
		
		oldT[0].unlink();
		oldT[1].unlink();
		submesh2d.getEdges().remove(e);
		submesh2d.getFaces().remove(oldT[0]);
		submesh2d.getFaces().remove(oldT[1]);
		if (logger.isDebugEnabled())
			logger.debug("Flip edge "+e+" to "+submesh2d.getEdgeDefinedByNodes(apex1, apex2));
		assert submesh2d.getEdges().size() == before : ""+before+" != "+submesh2d.getEdges().size();
		assert submesh2d.isValid();
		return newT;
	}
	
	/**
	 * Flips an edge if it locally improves mesh quality.
	 *
	 * @param  e          the edge to flip,
	 * @return <code>true</code> if edge was successfully flipped,
	 *         <code>false</code>  othersie.
	 */
	public boolean swap(MEdge2D e)
	{
		assert 2 == e.getFaces().size() : e;
		MFace2D [] oldT = new MFace2D[2];
		Iterator it = e.getFacesIterator();
		oldT[0] = (MFace2D) it.next();
		oldT[1] = (MFace2D) it.next();
		boolean res = (null != swap(e, oldT, false));
		assert submesh2d.isValid();
		return res;
	}
	
	/**
	 * Recursibelu flips a set of edges.  Whenever an edge is flipped,
	 * its neighbours are also checked to see if they can be flipped too.
	 *
	 * @param  edges      the set of edge to flip,
	 * @return the number of flipped edges.
	 */
	public int swapAll(HashSet edges)
	{
		int res = 0;
		HashSet seenEdges = new HashSet();
		while (!edges.isEmpty())
		{
			MEdge2D e = (MEdge2D) edges.iterator().next();
			logger.debug("Checking edge "+e);
			edges.remove(e);
			seenEdges.add(e);
			if (!submesh2d.getEdges().contains(e))
				continue;
			HashSet trianglelist = e.getFaces();
			if (2 > trianglelist.size())
				continue;
			MFace2D [] oldT = new MFace2D[2];
			System.arraycopy(trianglelist.toArray(), 0, oldT, 0, 2);;
			//  Building the list of edges which will have to be checked
			//  can easily be done here, so here we go even if it is
			//  useless when edge is not flipped.
			HashSet edgesToCheck = new HashSet();
			edgesToCheck.addAll(oldT[0].getEdges());
			edgesToCheck.addAll(oldT[1].getEdges());
			edgesToCheck.remove(e);
			
			MFace2D [] newT = swap(e, oldT, false);
			if (null == newT)
				continue;
			logger.debug("Edge flipped:");
			res++;
			// Check edges
			for (Iterator it = edgesToCheck.iterator(); it.hasNext(); )
			{
				MEdge2D newedge = (MEdge2D) it.next();
				if (!seenEdges.contains(newedge))
					edges.add(newedge);
			}
		}
		assert submesh2d.isValid();
		return res;
	}
}
