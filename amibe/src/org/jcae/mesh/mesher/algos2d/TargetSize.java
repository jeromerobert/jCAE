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
 * Cuts long edges and removes small edges.
 *
 * @see CollapseEdges
 * @see CutEdges
 */

public class TargetSize
{
	private static Logger logger=Logger.getLogger(TargetSize.class);
	private SubMesh2D submesh2d;
	private int maxiter = 20;
	
	/**
	 * Creates a <code>TargetSize</code> instance.
	 *
	 * @param m  the <code>SubMesh2D</code> instance to refine.
	 */
	public TargetSize(SubMesh2D m)
	{
		submesh2d = m;
	}
	
	/**
	 * Creates a <code>TargetSize</code> instance.
	 *
	 * @param m  the <code>SubMesh2D</code> instance to refine.
	 * @param n  the number of iterations.
	 */
	public TargetSize(SubMesh2D m, int n)
	{
		submesh2d = m;
		maxiter = n;
	}
	
	/**
	 * Iteratively collapses and refines edges.
	 *
	 * @see CollapseEdges
	 * @see CutEdges
	 */
	public void compute()
	{
		double minlen = 1.0 / Math.sqrt(2.);
		double maxlen = 1.0 * Math.sqrt(2.);
		boolean redo = false;
		submesh2d.checkInvertedTriangles = false;
		int ne = 0;
		int niter = 0;
		do {
			redo = false;
			if (logger.isDebugEnabled())
				logger.debug("Current number of nodes: "+submesh2d.getNodes().size());
			ne = 0;
			for(Iterator it=submesh2d.getEdgesIterator(); it.hasNext(); )
			{
				MEdge2D e = (MEdge2D) it.next();
				if (e.isMutable() && submesh2d.compGeom().length(e) < minlen)
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
				HashSet edgesToCheck = CollapseEdges.computeFace(submesh2d, false);
				if (!edgesToCheck.isEmpty())
				{
					redo = true;
					for (Iterator it = edgesToCheck.iterator(); it.hasNext(); )
					{
						MEdge2D e = (MEdge2D) it.next();
						if (submesh2d.getEdges().contains(e) && e.isMutable())
							submesh2d.flipEdge(e);
					}
				}
			}
			ne = 0;
			for(Iterator it=submesh2d.getEdgesIterator(); it.hasNext(); )
			{
				MEdge2D e = (MEdge2D) it.next();
				if (e.isMutable() && submesh2d.compGeom().length(e) > maxlen)
				{
					ne++;
					e.setMidPoint();
				}
				else
					e.resetMidPoint();
			}
			logger.debug("  Edges to refine: "+ne);
			if (ne > 0)
			{
				HashSet edgesToCheck = CutEdges.computeFace(submesh2d);
				if (!edgesToCheck.isEmpty())
				{
					redo = true;
					for (Iterator it = edgesToCheck.iterator(); it.hasNext(); )
					{
						MEdge2D e = (MEdge2D) it.next();
						if (submesh2d.getEdges().contains(e) && e.isMutable())
							submesh2d.flipEdge(e);
					}
				}
			}
			niter++;
		} while (redo && niter < maxiter);
		assert(submesh2d.isValid());
	}
}
