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
 * Swaps edges to minimize interpolation error.
 */
public class ImproveDeflection
{
	private static Logger logger=Logger.getLogger(ImproveDeflection.class);
	private SubMesh2D submesh;
	
	/** Default constructor */
	public ImproveDeflection(SubMesh2D m)
	{
		submesh = m;
	}
	
	public void compute()
	{
		computeFace(submesh);
	}
	
	public static void computeFace(SubMesh2D submesh2d)
	{
		boolean redo = true;
		logger.debug("Improving deflection");
		for(Iterator ite = submesh2d.getEdgesIterator(); ite.hasNext(); )
		{
			MEdge2D e = (MEdge2D) ite.next();
			e.setFrozen(false);
		}
		
		while (redo)
		{
			redo = false;
			HashSet meshEdges = new HashSet(submesh2d.getEdges());
			for(Iterator ite = meshEdges.iterator(); ite.hasNext(); )
			{
				MEdge2D e = (MEdge2D) ite.next();
				if (! submesh2d.getEdges().contains(e) || e.isFrozen())
					continue;
				// retrieve the faces bounding e
				HashSet trianglelist = e.getFaces();
				if (2 != trianglelist.size())
					continue;
				
				MNode2D pt1 = e.getNodes1();
				MNode2D pt2 = e.getNodes2();
				
				Iterator itf = trianglelist.iterator();
				MFace2D oldT1 = (MFace2D) itf.next();
				MFace2D oldT2 = (MFace2D) itf.next();
				MNode2D apex1 = oldT1.apex(e);
				MNode2D apex2 = oldT2.apex(e);
				
				e.setMidPoint();
				MNode2D mid = e.getMidPoint();
				e.resetMidPoint();
				
				// compute the deflection
				CADGeomSurface surf = submesh2d.getGeomSurface();
				MNode3D pt1_3 = new MNode3D(pt1, surf);
				MNode3D pt2_3 = new MNode3D(pt2, surf);
				MNode3D apex1_3 = new MNode3D(apex1, surf);
				MNode3D apex2_3 = new MNode3D(apex2, surf);
				MNode3D mid_3 = new MNode3D(mid, surf);
				
				MNode3D test1 = pt1_3.midPoint(pt2_3);
				MNode3D test2 = apex1_3.midPoint(apex2_3);
				if (mid_3.distance(test2) < mid_3.distance(test1))
				{
					//  Swap edge
					MFace2D newT1 = submesh2d.addTriangle(apex1, apex2, pt1);
					MFace2D newT2 = submesh2d.addTriangle(apex1, apex2, pt2);
					MEdge2D newEdge = submesh2d.getEdgeDefinedByNodes(apex1, apex2);
					submesh2d.rmFace(oldT1);
					submesh2d.rmFace(oldT2);
					submesh2d.getEdges().remove(e);
					newEdge.setFrozen(true);
					redo = true;
				}
			}
		}
		assert (submesh2d.isValid());
	}
}
