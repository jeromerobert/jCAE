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
import org.jcae.mesh.cad.CADGeomSurface;
import java.util.Iterator;
import java.util.Collection;
import java.util.HashSet;
import org.apache.log4j.Logger;

public class FindEdge2DCutter
{
	private static Logger logger=Logger.getLogger(FindEdge2DCutter.class);	

	//  The SubMesh2D instance on which methods are applied
	private SubMesh2D submesh2d;
	
	/**
	 * Constructor.
	 *
	 * @param  m   the <code>SubMesh2D</code> being modified.
	 */
	public FindEdge2DCutter(SubMesh2D m)
	{
		submesh2d = m;
	}
	
	/**
	 * Retrieves an edge that cut the segment defined by its 2 end points.
	 *
	 * @param n1   one of the bounding point of the segment
	 * @param n2   the other bounding point of the segment.
	 * @return an Object array, first index is an edge and 2nd index is
	 * a triangle containing this edge.
	 * @throws RuntimeException if no cutting edge was found.
	 */
	private Object [] getEdgeTri(MNode2D n1, MNode2D n2)
	{
		MEdge2D E;
		Object [] pair_toreturn = new Object[2];
		CADGeomSurface surf = submesh2d.getGeomSurface();
		
		// For all triangles linked to n1
		Iterator itt = n1.getElements2DIterator();
		while (itt.hasNext())
		{
			MFace2D f = (MFace2D)itt.next();
			if(f.numberOfEdges()!=3)
			{
				throw new RuntimeException("Find an element with "+
					f.numberOfEdges()+" edges while triangle expected");
			}
			// Find the edge	E : N1 - N2
			Iterator it_edge = f.getEdgesIterator();
			MEdge2D e1 = (MEdge2D)it_edge.next();
			MEdge2D e2 = (MEdge2D)it_edge.next();
			MEdge2D e3 = (MEdge2D)it_edge.next();
			MNode2D e1n1 = (MNode2D)e1.getNodes1();
			MNode2D e1n2 = (MNode2D)e1.getNodes2();
			MNode2D e2n1 = (MNode2D)e2.getNodes1();
			MNode2D e2n2 = (MNode2D)e2.getNodes2();
			if ((n1.equals(e1n1)) || (n1.equals(e1n2)))
			{
				if ((n1.equals(e2n1)) || (n1.equals(e2n2)))
					E = e3;
				else
					E = e2;
			}
			else
				E = e1;
			MNode2D En1 = (MNode2D)E.getNodes1();
			MNode2D En2 = (MNode2D)E.getNodes2();
			if (En1.orient2d(n1, n2, surf) * En2.orient2d(n1, n2, surf) <= 0.0 &&
			    n1.orient2d(En1, En2, surf) * n2.orient2d(En1, En2, surf) <= 0.0)
			{
				pair_toreturn[0] = E;
				pair_toreturn[1] = f;
				return pair_toreturn;
			}
		}
		throw new RuntimeException("Method getEdgeTri failed");
	}
	
	/**
	 * Constructs a set of edges cutting the segment n1-n2
	 *
	 * @param n1   an end point,
	 * @param n2   the other end point,
	 * @return a set of edges cutting the segment n1-n2
	 */
	public Collection getCuttingEdges(MNode2D n1, MNode2D n2)
	{
		HashSet toReturn = new HashSet();
		HashSet trianglelist = new HashSet();
		logger.debug("Method getCuttingEdges "+n1+" "+n2);
		CADGeomSurface surf = submesh2d.getGeomSurface();
		Object [] res = getEdgeTri(n1, n2);
		MEdge2D cur_edge = (MEdge2D) res[0];
		MFace2D cur_tri = (MFace2D) res[1];
		assert null != cur_edge : "Unable to find cur_edge";
		assert null != cur_tri : "Unable to find cur_tri";
		while (true)
		{
			toReturn.add(cur_edge);
			// Get the adjacent triangle of the current edge
			Iterator itf = cur_edge.getFacesIterator();
			MFace2D Tvoisin = (MFace2D) itf.next();
			if (cur_tri == Tvoisin)
				Tvoisin = (MFace2D) itf.next();
			// Find the apex
			MNode2D ap = Tvoisin.apex(cur_edge);
			if (ap == null)
				throw new RuntimeException ("Unable to find apex");
			
			// Is it the last edge?
			if (ap.equals(n2))
				break;
			MNode2D p1 = cur_edge.getNodes1();
			MNode2D p2 = cur_edge.getNodes2();
			// Find the cutting edge
			if (p1.orient2d(n1, n2, surf) * ap.orient2d(n1, n2, surf) <= 0.0)
				cur_edge = submesh2d.getEdgeDefinedByNodes(p1, ap);
			else
				cur_edge = submesh2d.getEdgeDefinedByNodes(p2, ap);
			// the current triangle
			cur_tri = submesh2d.getFaceDefinedByNodes(p1, p2, ap);
			assert null != cur_tri : "Unable to find cur_tri";
		}
		return toReturn;
	}
	
}
